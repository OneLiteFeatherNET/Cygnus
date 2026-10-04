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
