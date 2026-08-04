# Running Cygnus without LuckPerms

## Problem

Both services load LuckPerms unconditionally:

```java
// CygnusLoader / SetupLoader
MinestomLoader.get().load().registerShutdownHook().start();
```

That creates a `data/` directory next to the process, opens an H2 database and downloads
translation bundles and relocated libraries — before anything Cygnus-specific runs. The permission
path has the same problem from the other side: `PermissionAwarePlayer.value()` calls
`LuckPermsProvider.get()`, which throws `IllegalStateException` when no LuckPerms instance is
registered.

Two consequences:

- A local run always pays the LuckPerms startup cost, even when the change under test is map, game
  or setup logic.
- No test can exercise a permission-dependent path. `:game` and `:setup` already drop
  `net.luckperms:minestom-loader` from `configurations.testRuntimeClasspath`, so any test touching
  `value()` would hit that `IllegalStateException`.

## Goal

Cygnus starts and serves players with LuckPerms absent from the class path. In that state every
permission check answers `TRUE`, so `/stop`, setup commands and staff paths stay reachable. With
LuckPerms present nothing changes.

## Detection

A new class in `common`:

```java
package net.onelitefeather.cygnus.common.permission;

public final class LuckPermsSupport {
    private static final String LOADER_CLASS = "me.lucko.luckperms.minestom.loader.MinestomLoader";
    private static final boolean PRESENT = detect();

    public static boolean isPresent();
    public static void bootstrap();
}
```

`detect()` resolves `LOADER_CLASS` through `Class.forName(name, false, classLoader)` once and caches
the result. When the class is missing it logs a single WARN line stating that LuckPerms is absent,
that every permission check now resolves to `TRUE`, and that this mode does not belong in
production.

The marker is the **loader**, not `net.luckperms.api.LuckPermsProvider`. The API class is a
`testImplementation` dependency of `common` and would therefore be present during tests, so it would
report "LuckPerms available" exactly where it is not.

## Bootstrap

`LuckPermsSupport.bootstrap()` starts LuckPerms when `isPresent()`, otherwise does nothing. Both
entry points call it instead of touching `MinestomLoader` themselves:

```java
ExtensionBootstrap bootstrap = ExtensionBootstrap.init();
LuckPermsSupport.bootstrap();
```

`common` gains `compileOnly(libs.luckperms.minestom.loader)` for this. The artifact is still shipped
by `:game` / `:setup` as `runtimeOnly` — no new runtime dependency, and the fat jars are unchanged.

The statement that names `MinestomLoader` lives in a separate private method, so class resolution
cannot happen while the entry point itself is being verified.

## Permission lookup

`PermissionAwarePlayer.value()` gains a leading branch:

```java
if (!LuckPermsSupport.isPresent()) {
    return TriState.TRUE;
}
```

Everything reading Adventure's `PermissionChecker#POINTER` follows from there without knowing about
the fallback: `StopCommand`, the CloudNet bridge extension in `:bridge`, and LuckPerms' own command
sender factory.

`TriState.FALSE` for "LuckPerms is running but has no user data for this player" stays untouched —
that is correct production behaviour and unrelated to this change.

## Local runs

The fat jar bundles the loader, so `java -jar cygnus.jar` and `./gradlew :game:run` keep running
with LuckPerms. Reaching the fallback needs a start path with a filtered class path: a `JavaExec`
task `runWithoutLuckPerms` in `:game` and `:setup` whose configuration excludes
`net.luckperms:minestom-loader`, mirroring the existing `testRuntimeClasspath` exclusion. Main class
and working directory match the `run` task the `application` plugin provides.

Unchanged: the server still resolves maps relative to the working directory, so `game/maps` or
`setup/maps` must exist or startup ends at `No maps found in the given path`.

## Tests

The fallback is active during tests without further setup — `common` never has the loader on its
class path, and `:game` / `:setup` already exclude it. That exclusion gets a comment explaining what
now depends on it.

- `LuckPermsSupportTest` (`common`): `isPresent()` is `false` during tests and `bootstrap()`
  completes without throwing. Pins the assumption every other test relies on.
- `PermissionAwarePlayerIntegrationTest` (`common`): `value("cygnus.test")` returns `TriState.TRUE`
  instead of throwing `IllegalStateException`. Not testable today. The `IntegrationTest` suffix
  follows the convention for tests that use Cyano's `Env`.
- `StopCommandTest` (`common`): add a case for a real player; the existing tests only cover the
  console sender.

## Out of scope

- A switch that forces the fallback while LuckPerms is present. Detection is by class path only.
- Any change to how permissions resolve when LuckPerms *is* loaded.
- Test coverage for the CloudNet bridge extension, which needs a CloudNet bridge to load at all.
