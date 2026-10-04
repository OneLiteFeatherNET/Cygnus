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

## Hierarchy

```
cygnus.startup                      (own trace)  <-- link: first round
  cygnus.startup.config | .resourcepack | .maps | .features | .phases | .listeners

cygnus.round                        (own trace; links: startup, previous rounds from cookies)
  cygnus.phase.lobby
    cygnus.player.join              (started during the lobby, may end later)
    cygnus.action.*
    cygnus.tick.slow
      cygnus.tick.section
  cygnus.phase.waiting
    ... same children
  cygnus.phase.gamephase
    cygnus.action.* (page, creek, sanity, blackout, ...)
    cygnus.player.kick              (spectator leave)
    cygnus.tick.slow
  cygnus.phase.restart
    cygnus.player.kick

cygnus.shutdown                     (own trace)  --> link: running or last round
```

Work attaches to the phase that is running when it starts. Between two phases it attaches to the round.
A span may outlive its phase (a join started in the lobby ends in the waiting phase); it is never ended
early. With no round running (before the series started), joins, kicks and slow ticks are roots of their own
trace. The current phase is kept as a volatile snapshot in `RoundTracer`, so the tick thread and the
configuration threads read it without taking the lock.

## Spans

Player identity is always the UUID in `cygnus.player.uuid`. No address of a player is recorded.

| Span | Covers | Attributes |
| --- | --- | --- |
| `cygnus.startup` | Wiring the game in `Cygnus`. Ends before the port is bound. | |
| `cygnus.startup.config`, `.resourcepack`, `.maps`, `.features`, `.phases`, `.listeners` | Children of the startup, one per major step. | |
| `cygnus.round` | One round, from the lobby opening to the restart ending. Root of its own trace; the first round links to `cygnus.startup`. | `cygnus.round.id`, `cygnus.game.end_reason` |
| `cygnus.phase.<name>` | One phase of the round (`lobby`, `waiting`, `gamephase`, `restart`). Child of the round. | `cygnus.phase.name` |
| `cygnus.player.join` | A player from the login until the first spawn. Child of the phase running at the login; a root when no round runs. | `cygnus.player.uuid`, `cygnus.join.outcome` (`spawned`, `abandoned`) |
| `cygnus.player.kick` | A kick, including the wait for the client to drop the ResourcePack. Child of the phase running at the kick; a root when no round runs. | `cygnus.player.uuid`, `cygnus.kick.reason`, `cygnus.kick.completed_by` (`ack`, `timeout`, `immediate`) |
| `cygnus.shutdown` | The request until the server stopped, ended before the JVM is told to exit. Its own trace, linked to the running (or last) round. | |
| `cygnus.tick.slow` | A server tick that took at least the threshold, created after the fact. Child of the current phase; a root when no round runs. | `cygnus.tick.duration_ms`, `cygnus.tick.acquisition_ms`, `cygnus.tick.threshold_ms` |
| `cygnus.tick.section` | Child of a slow tick: the share of one measured service, see [Slow ticks](#slow-ticks). | `cygnus.tick.section.name`, `cygnus.tick.section.duration_ms` |

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
when. A share below 0.1 ms is left out, and a service that did not run in the tick has no child, so the lobby
tick shows `lobby-waiting` and `lobby-time` where a game tick shows the creek.

The measured services, with the name each reports as `cygnus.tick.section.name` (the constants are in
`TickSectionNames`; one name per service, never per player):

| Name | Service |
| --- | --- |
| `lobby-waiting` | The lobby's waiting action bar |
| `lobby-time` | The lobby's slide of the world time |
| `creek` | The creek's step |
| `glow-reveal` | The betrayal glow resend |
| `slender-gaze` | The slender's gaze check |
| `tunnel-vision` | The tunnel vision overlay |
| `page-glitch` | The page glitch overlay |
| `blood-splatter` | The fade of blood splatters |
| `page-proximity` | The page proximity hint |
| `ambient` | Ambient sounds and blackouts |
| `adrenaline` | The adrenaline rush |
| `slender-takeover` | The takeover countdown after the slender left |
| `stamina` | The survivors' stamina bars, added up |
| `slender-bar` | The slender's stamina bar |
| `jump-scare` | The despawn of a jump scare |

Only what is scheduled through the Minestom scheduler can be measured. Time the server spends outside of
it (entity and chunk ticking, packet handling) is not a section, so a slow tick can still have a gap between the
sum of its children and its own duration; that gap is the engine.

```properties
# config.properties
telemetry.slowTickThresholdMillis=50
```

A value below 1 is rejected and the service refuses to start.

## Tick metrics

Spans exist only for slow ticks, so a trace cannot say how long a lobby tick usually takes. Metrics are
recorded for every tick (`ServerTickMonitorEvent`) and every measured section, per phase. The javaagent exports
them over OTLP together with its own metrics; without the agent they are no-ops.

| Instrument | Kind | Unit | Attributes |
| --- | --- | --- | --- |
| `cygnus.tick.duration` | histogram | `ms` | `cygnus.phase.name` |
| `cygnus.tick.section.duration` | histogram | `ms` | `cygnus.tick.section.name`, `cygnus.phase.name` |
| `cygnus.tick.slow` | counter | `{tick}` | `cygnus.phase.name` |

- `cygnus.phase.name` is `lobby`, `waiting`, `gamephase`, `restart`, or `none` (no round or between phases).
- `cygnus.tick.section.name` is one of the names in [Slow ticks](#slow-ticks). A section is recorded for a tick
  only if it ran in it, with the total of its runs.
- `cygnus.tick.slow` counts ticks at or above `telemetry.slowTickThresholdMillis`, the same rule as the span.
- Unit is milliseconds, not the seconds the semantic conventions prefer: Minestom's budget, the threshold and the
  span attributes are in milliseconds, and a bucket at `50` reads as 50.
- Explicit buckets (ms): 0.1, 0.25, 0.5, 1, 2.5, 5, 10, 15, 20, 25, 30, 40, 50, 75, 100, 150, 200. The SDK
  default starts at 5 and would put the whole lobby in one bucket.
- The hot path allocates nothing of its own: attributes per phase and per section/phase pair are built once and
  reused, values are recorded as primitives.

### In Mimir / Grafana

OTLP to Prometheus translation appends the unit and, for counters, `_total` (checked against the existing
`jvm_gc_duration_seconds_*` and `jvm_cpu_time_seconds_total` in Mimir). Dots become underscores:

| OTel | Prometheus |
| --- | --- |
| `cygnus.tick.duration` | `cygnus_tick_duration_milliseconds_bucket` / `_sum` / `_count` |
| `cygnus.tick.section.duration` | `cygnus_tick_section_duration_milliseconds_bucket` / `_sum` / `_count` |
| `cygnus.tick.slow` | `cygnus_tick_slow_total` |
| `cygnus.phase.name` | label `cygnus_phase_name` |
| `cygnus.tick.section.name` | label `cygnus_tick_section_name` |

For the Kubernetes-FLUX dashboard `cygnus-traces`, the per-phase and sub-tick panels should query these metrics
rather than derive them from slow tick spans, which only cover the tail. For example the p99 tick per phase:

```promql
histogram_quantile(0.99, sum by (le, cygnus_phase_name) (rate(cygnus_tick_duration_milliseconds_bucket[5m])))
```

and the mean cost of a section: `sum by (cygnus_tick_section_name) (rate(..._section_duration_milliseconds_sum[5m]))
/ sum by (cygnus_tick_section_name) (rate(..._section_duration_milliseconds_count[5m]))`.

## Hunts

A hunt is the chase of one creek after one survivor. It gets a span of its own, `cygnus.action.creek.hunt`, and a
metric, so "how long are survivors hunted, and how does it end" can be answered per map.

| Outcome | Meaning |
| --- | --- |
| `caught` | The creek caught the survivor (the catch is reported just before the hunt ends). |
| `timeout` | The hunt ran out of its maximum time. |
| `gone` | The survivor died, left or turned before it ended. |
| `escaped` | The creek was sent away for good while it hunted (only one survivor left). |
| `round_end` | The creek was removed, or the round ended with the hunt still going. Hunts still open when a round ends are closed as `round_end` before the phase span ends. |

| Instrument | Kind | Unit | Attributes |
| --- | --- | --- | --- |
| `cygnus.creek.hunt.duration` | histogram | `ms` | `cygnus.creek.hunt.outcome`, `cygnus.map` |
| `cygnus.creek.hunts` | counter | `{hunt}` | `cygnus.creek.hunt.outcome`, `cygnus.map` |

Buckets (ms): 500, 1000, 2500, 5000, 7500, 10000, 15000, 20000, 30000, 45000, 60000, 90000, 120000. There is no
player attribute on the metrics (one series per player); the UUID is on the span. In Prometheus:
`cygnus_creek_hunt_duration_milliseconds_bucket` and `cygnus_creek_hunts_total`, labels `cygnus_creek_hunt_outcome`
and `cygnus_map`. A hunt that starts while no round runs has no span but is still counted.

The creek reports a hunt by the survivor only, not by itself. Two creeks hunting the same survivor are two spans,
and each end closes the oldest open one of that survivor: the count is right, which span belongs to which creek
may not be.

## Shutdown

`ServiceShutdown` ends the span before it calls `Runtime.exit`. The agent flushes its exporter from a JVM
shutdown hook, which only runs once the exit started, so a span ended earlier is flushed with the rest. If
the shutdown hangs, the 30 second watchdog calls `Runtime.halt`, which skips the hooks: that shutdown span,
and the tail of every other export, is lost.

## Actions

What happens in a round is recorded as one short span per action, a child of the phase it happened in (`cygnus.round` between two phases). A span
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
| `cygnus.action.blackout` | `cygnus.blackout.team`, `cygnus.blackout.players`, `cygnus.blackout.duration_ticks`, `cygnus.blackout.next_in_s` (the interval just rolled) |
| `cygnus.action.blackout.player` | child of a blackout, one per player hit; the player attributes only |
| `cygnus.action.sanity.threshold` | `cygnus.sanity.band.from`, `cygnus.sanity.band`, `cygnus.sanity.fear` (0 calm, 1 maximum), `cygnus.sanity.value` (1 - fear, as `/creek` shows it), `cygnus.sanity.source` (`page`, `sighting`, `caught`, `selected`, `death`, `stalk`) |
| `cygnus.action.creek.sighted`, `.selected`, `.stalk` (first step of a stalk only), `.caught` | the survivor's attributes only |
| `cygnus.action.creek.hunt` | A hunt, from the creek taking the survivor as its target until it stops. Child of the phase it started in. The survivor's attributes at the start, `cygnus.map`, `cygnus.creek.hunt.outcome` (`caught`, `timeout`, `gone`, `escaped`, `round_end`) and `cygnus.creek.hunt.duration_ms`. |

`cygnus.page.out_ms` counts from the moment the page was placed or moved on. A page that has to wait on its
own spot (no other spot is free) is counted from the moment it was hidden.

### Sanity bands

A survivor's fear is continuous, so only upward crossings of a band are traced: `calm` below 0.25,
`uneasy` from 0.25, `afraid` from 0.5, `terrified` from 0.75 and `panic` at the maximum, 1.0. The source is
what caused the jump that crossed the band. Each band is reported once per survivor and round, so a
survivor hovering around a boundary produces no flood and a round has at most four sanity spans per
survivor. The fear decaying back down is not traced (it is computed when read, not in a tick), and fear
cannot bottom out: its floor grows with the pages found and the time played.

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
