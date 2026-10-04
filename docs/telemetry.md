# Telemetry

Cygnus creates OpenTelemetry traces. It ships only the `opentelemetry-api`; the SDK and the OTLP
exporter come from the OpenTelemetry javaagent, which has to be attached to the JVM. Without the agent
every span is a no-op and nothing is exported.

## Attaching the agent

```
java -javaagent:/path/to/opentelemetry-javaagent.jar -jar cygnus.jar
```

```
OTEL_SERVICE_NAME=cygnus
OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4318
```

The API version Cygnus compiles against (see `opentelemetry` in `settings.gradle.kts`) must not exceed
what the attached agent supports. The API classes stay under their real `io.opentelemetry` package
names in `cygnus.jar`: the agent recognises `GlobalOpenTelemetry` by exact class name, so they must never be
relocated.

The instrumentation scope is `net.onelitefeather.cygnus`, versioned with the jar.

## Spans

Player identity is always the UUID in `cygnus.player.uuid`. No address of a player is recorded.

| Span | Covers | Attributes |
| --- | --- | --- |
| `cygnus.startup` | Wiring the game in `Cygnus`. Ends before the port is bound. | |
| `cygnus.startup.config`, `.resourcepack`, `.maps`, `.features`, `.phases`, `.listeners` | Children of the startup, one per major step. | |
| `cygnus.round` | One round, from the lobby opening to the restart ending. Root of its own trace. | `cygnus.round.id`, `cygnus.game.end_reason` |
| `cygnus.phase.<name>` | One phase of the round (`lobby`, `waiting`, `gamephase`, `restart`). Child of the round. | `cygnus.phase.name` |
| `cygnus.player.join` | A player from the login until the first spawn. | `cygnus.player.uuid`, `cygnus.join.outcome` (`spawned`, `abandoned`) |
| `cygnus.player.kick` | A kick, including the wait for the client to drop the ResourcePack. | `cygnus.player.uuid`, `cygnus.kick.reason`, `cygnus.kick.completed_by` (`ack`, `timeout`, `immediate`) |
| `cygnus.shutdown` | The request until the server stopped, ended before the JVM is told to exit. | |
| `cygnus.tick.slow` | A server tick that took at least the threshold, created after the fact. | `cygnus.tick.duration_ms`, `cygnus.tick.acquisition_ms`, `cygnus.tick.threshold_ms` |
| `cygnus.tick.section` | Child of a slow tick: the share of one measured service (`creek`, `slender-gaze`, `tunnel-vision`). | `cygnus.tick.section.name`, `cygnus.tick.section.duration_ms` |

A span that fails is marked `ERROR` and carries the exception.

### Events on `cygnus.round`

| Event | Attributes |
| --- | --- |
| `cygnus.game.start` | |
| `cygnus.page.found` | `cygnus.player.uuid`, `cygnus.pages.found`, `cygnus.pages.max` |
| `cygnus.player.death` | `cygnus.player.uuid`, `cygnus.player.role` (`survivor`, `slender`, `spectator`, `none`) |
| `cygnus.slender.revive` | `cygnus.player.uuid` |
| `cygnus.game.finish` | `cygnus.game.end_reason` (`TIME_OVER`, `ALL_PAGES_FOUND`, `ALL_SURVIVOR_DEAD`, `SLENDER_LEFT`, `SURVIVOR_LEFT`) |

A round that is cut short by a shutdown ends with the end reason `shutdown`.

### Events on `cygnus.player.join`

| Event | Attributes |
| --- | --- |
| `cygnus.join.configuration` | |
| `cygnus.join.resourcepack` | `cygnus.resourcepack.status` |

## Slow ticks

There is no span per tick. When a tick takes at least `telemetry.slowTickThresholdMillis` (default 50,
Minestom's tick budget), a `cygnus.tick.slow` span is created after the fact: it ends now and started as
long ago as the tick took. The services measured through `TickSections` appear as children. They all start
at the start of the tick and last as long as the service took in total, so they show who was in the tick, not
when. A share below one millisecond is left out.

```properties
# config.properties
telemetry.slowTickThresholdMillis=50
```

A value below 1 is rejected and the service refuses to start.

## Shutdown

`ServiceShutdown` ends the span before it calls `Runtime.exit`. The agent flushes its exporter from a JVM
shutdown hook, which only runs once the exit started, so a span ended earlier is flushed with the rest. If
the shutdown hangs, the 30 second watchdog calls `Runtime.halt`, which skips the hooks: that shutdown span,
and the tail of every other export, is lost.

## Actions

What happens in a round is recorded as one short span per action, a child of `cygnus.round`. A span
rather than an event on the round span, because a backend can search spans by name and attribute
("every `cygnus.action.page.found` within 5 blocks of this spot", "everything player X did"), while events
pile up on one long span and are capped per span by the SDK. Movement and anything that happens per tick are
not traced. An action outside a running round creates no span.

Every action that has a player carries `cygnus.player.uuid`, `cygnus.player.role` and the position of the
player at that moment as `cygnus.position.x/y/z` (rounded to 0.1), plus `cygnus.map` while a map is loaded.

| Span | Extra attributes |
| --- | --- |
| `cygnus.action.page.spawn` | `cygnus.page.id`, `cygnus.page.relocated`; the position is the spot of the page |
| `cygnus.action.page.found` | `cygnus.page.id`, `cygnus.page.index` (found, including this one), `cygnus.pages.max`, `cygnus.page.out_ms` (time since it appeared), `cygnus.page.spot.x/y/z`, `cygnus.distance` (finder to page) |
| `cygnus.action.page.expired` | `cygnus.page.id`, `cygnus.page.out_ms`; the position is the spot of the page |
| `cygnus.action.player.death` | for a survivor with a slender in the round: `cygnus.killer.uuid` and `cygnus.distance` to the slender |
| `cygnus.action.slender.revive` | |
| `cygnus.action.slender.stamina` | `cygnus.stamina.state` (`READY`, `DRAINING`, `REGENERATING`), which is what using the slender's ability changes |
| `cygnus.action.spectator.join` | |
| `cygnus.action.disclaimer.acknowledge`, `cygnus.action.disclaimer.decline` | |
| `cygnus.action.creek.sighted`, `.selected`, `.stalk` (first step of a stalk only), `.caught` | the survivor's attributes only |

`cygnus.page.out_ms` counts from the moment the page was placed or moved on. A page that has to wait on its
own spot (no other spot is free) is counted from the moment it was hidden.

### Not traced

- **Creek catches have no distance or line of sight.** The creek reports only the survivor's UUID to its
  witness; adding the creek's position would mean changing the creek.
- **The slender's damage** (`PlayerDamagedEvent`) happens twice a second to everyone around him while he
  drains; that is per-tick state. The kill is traced as the death.
- **Sprinting, jump scares, the ambient sounds and the page proximity chime** are continuous or purely
  cosmetic.
- **Players joining after the round started** do not exist: the login listener turns them away, so
  there is no spectator join late in a round.

## Trace cookie

At the start of a round the `traceparent` (W3C Trace Context) of the round span is stored in the client
cookie `onelitefeather:trace` of every online player. When a player joins, the cookie is requested and, if it
holds a valid `traceparent`, the join span and the running round get a span link (`cygnus.link.kind` =
`previous_round`) to it. The round gets one link per distinct trace.

The cookie comes from the client, so it is only parsed: at most 128 bytes, ASCII, accepted only if the W3C
parser yields a valid span context. The request does not block the configuration thread, and a client that
does not answer within two seconds counts as having no cookie.

A cookie lives on the client. It reaches another backend only as long as the client's connection to the
proxy persists, which is the case while Velocity moves a player between backends; once the player has
left the network it is gone.
