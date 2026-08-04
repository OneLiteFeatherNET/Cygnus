# Running Cygnus without LuckPerms — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cygnus starts, serves players and passes tests with LuckPerms absent from the class path, answering every permission check with `TRUE` in that state.

**Architecture:** A single class in `common` resolves the LuckPerms loader class once and caches the result. Both service entry points route their LuckPerms bootstrap through it, and `PermissionAwarePlayer` short-circuits its lookup when it reports absent. Local runs reach that state through a Gradle task with a filtered class path.

**Tech Stack:** Java 25, Minestom, LuckPerms 5.5 API + `net.luckperms:minestom-loader`, Adventure, Gradle Kotlin DSL, JUnit 5 with Cyano (`MicrotusExtension`).

## Global Constraints

- Detection is by class path only. Do not add a system property, environment variable or config entry that forces the fallback while LuckPerms is present.
- The marker class is `me.lucko.luckperms.minestom.loader.MinestomLoader` — never `net.luckperms.api.LuckPermsProvider`, which is on the test class path of `common` and would report "available" exactly where it is not.
- Every LuckPerms dependency declaration carries `exclude(group = "net.kyori.adventure")`, matching the existing declarations in `game/build.gradle.kts` and `setup/build.gradle.kts`.
- `TriState.FALSE` for "LuckPerms is running but has no user data for this player" stays untouched.
- Fat jars keep bundling the loader. No change to `shadowJar` in either module.
- New and modified classes carry the repository's Javadoc style with `@author`, `@version` and `@since` tags. Use `@author TheMeinerLP`, `@version 1.0.0`, `@since 2.6.7`.
- Commit messages follow Conventional Commits, as the repository runs Release Please.

---

### Task 1: LuckPermsSupport in `common`

**Files:**
- Create: `common/src/main/java/net/onelitefeather/cygnus/common/permission/LuckPermsSupport.java`
- Create: `common/src/test/java/net/onelitefeather/cygnus/common/permission/LuckPermsSupportTest.java`
- Modify: `common/build.gradle.kts` (dependencies block, after the `compileOnly(libs.luckperms.api)` entry)

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `LuckPermsSupport.isPresent()` returning `boolean`, and `LuckPermsSupport.bootstrap()` returning `void`. Tasks 2 and 3 depend on both.

- [ ] **Step 1: Add the compile-only loader dependency**

In `common/build.gradle.kts`, directly below the existing `compileOnly(libs.luckperms.api) { ... }` block:

```kotlin
    // Only to compile LuckPermsSupport.bootstrap(). The artifact is shipped by :game and :setup as
    // runtimeOnly - common must not put it on any runtime class path, because its absence is exactly
    // what LuckPermsSupport detects.
    compileOnly(libs.luckperms.minestom.loader) {
        exclude(group = "net.kyori.adventure")
    }
```

- [ ] **Step 2: Write the failing test**

Create `common/src/test/java/net/onelitefeather/cygnus/common/permission/LuckPermsSupportTest.java`:

```java
package net.onelitefeather.cygnus.common.permission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Pins the assumption every other test relies on: the LuckPerms loader is kept off the test class
 * path, so Cygnus runs in its LuckPerms-free mode while tests execute.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 */
class LuckPermsSupportTest {

    @Test
    void testLoaderIsAbsentDuringTests() {
        assertFalse(LuckPermsSupport.isPresent(), "The test class path must not carry the LuckPerms loader");
    }

    @Test
    void testBootstrapIsSilentWithoutLoader() {
        assertDoesNotThrow(LuckPermsSupport::bootstrap);
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :common:test --tests 'net.onelitefeather.cygnus.common.permission.LuckPermsSupportTest'`
Expected: FAIL — compilation error, `LuckPermsSupport` does not exist.

- [ ] **Step 4: Write the implementation**

Create `common/src/main/java/net/onelitefeather/cygnus/common/permission/LuckPermsSupport.java`:

```java
package net.onelitefeather.cygnus.common.permission;

import me.lucko.luckperms.minestom.loader.MinestomLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decides whether Cygnus runs with LuckPerms.
 * <p>
 * Minestom has no plugin folder, so LuckPerms is bootstrapped from {@code main} and shipped inside
 * the fat jar. A build that leaves the loader out - the test class path and the
 * {@code runWithoutLuckPerms} task do exactly that - runs without any permission backend, and
 * {@code PermissionAwarePlayer} then grants every permission instead of failing on
 * {@code LuckPermsProvider.get()}.
 * <p>
 * Detection reads the class path once and caches the answer, so a permission check never pays for
 * it twice.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 **/
public final class LuckPermsSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(LuckPermsSupport.class);
    private static final String LOADER_CLASS = "me.lucko.luckperms.minestom.loader.MinestomLoader";
    private static final boolean PRESENT = detect();

    /**
     * Returns whether LuckPerms can be used.
     *
     * @return {@code true} if the LuckPerms loader is on the class path
     */
    public static boolean isPresent() {
        return PRESENT;
    }

    /**
     * Starts LuckPerms and registers its shutdown hook. Does nothing when LuckPerms is absent.
     */
    public static void bootstrap() {
        if (!PRESENT) {
            return;
        }
        startLuckPerms();
    }

    /**
     * Starts LuckPerms. Kept separate so resolving {@link MinestomLoader} cannot happen while
     * {@link #bootstrap()} itself is being verified.
     */
    private static void startLuckPerms() {
        MinestomLoader.get().load().registerShutdownHook().start();
    }

    /**
     * Resolves the loader class without initialising it.
     *
     * @return {@code true} if the class is available, {@code false} after logging a warning
     */
    private static boolean detect() {
        try {
            Class.forName(LOADER_CLASS, false, LuckPermsSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            LOGGER.warn("LuckPerms is not on the class path. Every permission check resolves to TRUE. "
                    + "This mode is meant for local runs and tests, never for production.");
            return false;
        }
    }

    private LuckPermsSupport() {
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :common:test --tests 'net.onelitefeather.cygnus.common.permission.LuckPermsSupportTest'`
Expected: PASS, 2 tests. The WARN line from `detect()` appears in the output.

- [ ] **Step 6: Commit**

```bash
git add common/build.gradle.kts \
        common/src/main/java/net/onelitefeather/cygnus/common/permission/LuckPermsSupport.java \
        common/src/test/java/net/onelitefeather/cygnus/common/permission/LuckPermsSupportTest.java
git commit -m "feat(common): detect whether LuckPerms is on the class path"
```

---

### Task 2: Grant every permission without LuckPerms

**Files:**
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/player/PermissionAwarePlayer.java:61-69` (the `value` method) and its class Javadoc
- Create: `common/src/test/java/net/onelitefeather/cygnus/common/player/PermissionAwarePlayerIntegrationTest.java`
- Modify: `common/src/test/java/net/onelitefeather/cygnus/common/bootstrap/StopCommandTest.java`

**Interfaces:**
- Consumes: `LuckPermsSupport.isPresent()` from Task 1.
- Produces: `PermissionAwarePlayer.value(String)` returning `TriState.TRUE` whenever LuckPerms is absent. Nothing later depends on new names.

- [ ] **Step 1: Write the failing player test**

Create `common/src/test/java/net/onelitefeather/cygnus/common/player/PermissionAwarePlayerIntegrationTest.java`:

```java
package net.onelitefeather.cygnus.common.player;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies that a player answers permission questions without LuckPerms present, which is the state
 * every test run and every {@code runWithoutLuckPerms} start is in.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 */
@ExtendWith(MicrotusExtension.class)
class PermissionAwarePlayerIntegrationTest {

    @BeforeAll
    static void setUp(Env env) {
        env.process().connection().setPlayerProvider(TestPlayer::new);
    }

    @Test
    void testPermissionIsGrantedWithoutLuckPerms(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        PermissionAwarePlayer permissionAware = assertInstanceOf(PermissionAwarePlayer.class, player);
        assertEquals(TriState.TRUE, permissionAware.value("cygnus.test"));

        env.destroyInstance(instance, true);
    }

    @Test
    void testPointerResolvesThroughTheSamePath(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        PermissionChecker checker = player.getOrDefault(
                PermissionChecker.POINTER,
                PermissionChecker.always(TriState.FALSE)
        );
        assertEquals(TriState.TRUE, checker.value("cygnus.test"));

        env.destroyInstance(instance, true);
    }

    /**
     * A player which adds nothing to {@link PermissionAwarePlayer}, so the test observes the
     * permission handling of the base class and nothing else.
     */
    private static final class TestPlayer extends PermissionAwarePlayer {

        private TestPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
            super(playerConnection, gameProfile);
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :common:test --tests 'net.onelitefeather.cygnus.common.player.PermissionAwarePlayerIntegrationTest'`
Expected: FAIL — both tests throw `IllegalStateException` from `LuckPermsProvider.get()`.

- [ ] **Step 3: Add the fallback**

In `PermissionAwarePlayer.java`, make `value` start with the new branch:

```java
    @Override
    public @NotNull TriState value(@NotNull String permission) {
        if (!LuckPermsSupport.isPresent()) {
            return TriState.TRUE;
        }
        User user = LuckPermsProvider.get().getUserManager().getUser(getUuid());
        if (user == null) {
            return TriState.FALSE;
        }
        QueryOptions queryOptions = LuckPermsProvider.get().getContextManager().getQueryOptions(this);
        return TriStates.fromLuckPerms(user.getCachedData().getPermissionData(queryOptions).checkPermission(permission));
    }
```

Add the import `net.onelitefeather.cygnus.common.permission.LuckPermsSupport`, and extend the method Javadoc's `@return` so it names the new case:

```java
    /**
     * Resolves a permission for this player through LuckPerms, honouring the contexts LuckPerms
     * has calculated for them.
     *
     * @param permission the permission node to check
     * @return {@link TriState#TRUE} when LuckPerms is absent, the value LuckPerms holds for the
     * node otherwise, or {@link TriState#FALSE} when LuckPerms has no user data for this player
     */
```

Extend the class Javadoc with a paragraph after the sentence about the dynamic pointer:

```java
 * <p>
 * Without LuckPerms on the class path every check answers {@link TriState#TRUE} instead, so local
 * runs and tests reach permission-gated paths at all. See {@code LuckPermsSupport}.
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :common:test --tests 'net.onelitefeather.cygnus.common.player.PermissionAwarePlayerIntegrationTest'`
Expected: PASS, 2 tests.

- [ ] **Step 5: Cover the command path**

`StopCommandTest` so far only proves the console sender is allowed. Add a player case. Replace the class body of `common/src/test/java/net/onelitefeather/cygnus/common/bootstrap/StopCommandTest.java` with:

```java
@ExtendWith(MicrotusExtension.class)
class StopCommandTest {

    @BeforeAll
    static void setUp(Env env) {
        env.process().connection().setPlayerProvider(TestPlayer::new);
    }

    @Test
    void testCommandName() {
        StopCommand command = new StopCommand();
        assertEquals("stop", command.getName());
    }

    @Test
    void testConsoleSenderIsAlwaysAllowed(@NotNull Env env) {
        StopCommand command = new StopCommand();
        CommandSender consoleSender = env.process().command().getConsoleSender();

        assertTrue(command.getCondition().canUse(consoleSender, "stop"));
    }

    @Test
    void testPlayerIsAllowedWithoutLuckPerms(@NotNull Env env) {
        StopCommand command = new StopCommand();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        assertTrue(command.getCondition().canUse(player, "stop"));

        env.destroyInstance(instance, true);
    }

    /**
     * A player which adds nothing to {@link PermissionAwarePlayer}, so the command sees the same
     * pointer a Cygnus player carries.
     */
    private static final class TestPlayer extends PermissionAwarePlayer {

        private TestPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
            super(playerConnection, gameProfile);
        }
    }
}
```

Add the imports this needs: `net.minestom.server.entity.Player`, `net.minestom.server.instance.Instance`, `net.minestom.server.network.player.GameProfile`, `net.minestom.server.network.player.PlayerConnection`, `net.onelitefeather.cygnus.common.player.PermissionAwarePlayer` and `org.junit.jupiter.api.BeforeAll`.

- [ ] **Step 6: Run the command tests**

Run: `./gradlew :common:test --tests 'net.onelitefeather.cygnus.common.bootstrap.StopCommandTest'`
Expected: PASS, 3 tests.

- [ ] **Step 7: Run the whole common suite**

Run: `./gradlew :common:test`
Expected: PASS. No existing test changes behaviour — none of them touched a permission path before.

- [ ] **Step 8: Commit**

```bash
git add common/src/main/java/net/onelitefeather/cygnus/common/player/PermissionAwarePlayer.java \
        common/src/test/java/net/onelitefeather/cygnus/common/player/PermissionAwarePlayerIntegrationTest.java \
        common/src/test/java/net/onelitefeather/cygnus/common/bootstrap/StopCommandTest.java
git commit -m "feat(common): grant every permission when LuckPerms is absent"
```

---

### Task 3: Route both entry points through LuckPermsSupport

**Files:**
- Modify: `game/src/main/java/net/onelitefeather/cygnus/CygnusLoader.java:3,15`
- Modify: `setup/src/main/java/net/onelitefeather/cygnus/setup/SetupLoader.java:3,16`
- Modify: `game/build.gradle.kts:38-40` and `setup/build.gradle.kts:40-42` (the `compileOnly(libs.luckperms.minestom.loader)` blocks)
- Modify: `game/build.gradle.kts:56-58` and `setup/build.gradle.kts:57-59` (the `configurations.testRuntimeClasspath` blocks)

**Interfaces:**
- Consumes: `LuckPermsSupport.bootstrap()` from Task 1.
- Produces: nothing new.

- [ ] **Step 1: Rewrite the game entry point**

In `CygnusLoader.java`, drop the `me.lucko.luckperms.minestom.loader.MinestomLoader` import, add `net.onelitefeather.cygnus.common.permission.LuckPermsSupport`, and replace the bootstrap line:

```java
        ExtensionBootstrap bootstrap = ExtensionBootstrap.init();
        LuckPermsSupport.bootstrap();
```

- [ ] **Step 2: Rewrite the setup entry point**

Apply the same two edits to `SetupLoader.java`: drop the `MinestomLoader` import, add the `LuckPermsSupport` import, replace `MinestomLoader.get().load().registerShutdownHook().start();` with `LuckPermsSupport.bootstrap();`.

- [ ] **Step 3: Drop the now-unused compile scope**

Neither module names `MinestomLoader` any more, so remove this block from both `game/build.gradle.kts` and `setup/build.gradle.kts`:

```kotlin
    compileOnly(libs.luckperms.minestom.loader) {
        exclude(group = "net.kyori.adventure")
    }
```

Keep the `runtimeOnly(libs.luckperms.minestom.loader)` block in both — that is what puts LuckPerms into the fat jars.

- [ ] **Step 4: Explain the test class path exclusion**

In both modules the exclusion now carries the LuckPerms-free test mode. Give it a comment:

```kotlin
// Keeps the loader off the test class path, which is what makes LuckPermsSupport report absent and
// every permission check answer TRUE during tests.
configurations.testRuntimeClasspath {
    exclude(group = "net.luckperms", module = "minestom-loader")
}
```

- [ ] **Step 5: Build and test everything**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL. Both entry points compile without any LuckPerms import.

- [ ] **Step 6: Verify the fat jar still ships LuckPerms**

Run: `unzip -l game/build/libs/cygnus.jar | grep -c 'me/lucko/luckperms/minestom/loader/MinestomLoader.class'`
Expected: `1`. Production behaviour is unchanged — the loader is present, so `isPresent()` is `true` there.

- [ ] **Step 7: Commit**

```bash
git add game/src/main/java/net/onelitefeather/cygnus/CygnusLoader.java \
        setup/src/main/java/net/onelitefeather/cygnus/setup/SetupLoader.java \
        game/build.gradle.kts setup/build.gradle.kts
git commit -m "refactor(bootstrap): start LuckPerms through LuckPermsSupport"
```

---

### Task 4: A local start without LuckPerms

**Files:**
- Modify: `game/build.gradle.kts` (new task registration after the existing `tasks { ... }` block)
- Modify: `setup/build.gradle.kts` (same)
- Modify: `docs/cloudnet-deployment.md` ("Running locally" section, currently the last section)

**Interfaces:**
- Consumes: the behaviour from Tasks 1-3.
- Produces: Gradle tasks `:game:runWithoutLuckPerms` and `:setup:runWithoutLuckPerms`.

- [ ] **Step 1: Register the task in `:game`**

Append to `game/build.gradle.kts`, after the `tasks { ... }` block:

```kotlin
// Local counterpart to `run`: the same entry point, but with the LuckPerms loader filtered out of
// the class path, so LuckPermsSupport reports absent and every permission check answers TRUE. No
// data/ directory, no H2 database, no library downloads.
tasks.register<JavaExec>("runWithoutLuckPerms") {
    group = "application"
    description = "Runs the game service without LuckPerms; every permission check resolves to TRUE."
    mainClass.set(application.mainClass)
    classpath = sourceSets.main.get().runtimeClasspath.filter { file ->
        !file.name.startsWith("minestom-loader-")
    }
}
```

- [ ] **Step 2: Register the task in `:setup`**

Append the same block to `setup/build.gradle.kts`, with the description changed to:

```kotlin
    description = "Runs the setup service without LuckPerms; every permission check resolves to TRUE."
```

- [ ] **Step 3: Verify the task exists**

Run: `./gradlew :game:tasks --group application`
Expected: `runWithoutLuckPerms` is listed alongside `run`.

- [ ] **Step 4: Verify the fallback is reached**

Run: `timeout 180 ./gradlew :game:runWithoutLuckPerms 2>&1 | head -40`
Expected: the WARN line `LuckPerms is not on the class path. Every permission check resolves to TRUE.` appears, and **no** LuckPerms banner (`Running on Minestom`, `Loading storage provider... [H2]`). The run then ends at `IllegalStateException: No maps found in the given path` unless the repository root contains `game/maps` — that is the pre-existing map requirement and confirms the server got past the permission bootstrap.

- [ ] **Step 5: Confirm no `data/` directory appeared**

Run: `test -d data && echo "PRESENT" || echo "ABSENT"`
Expected: `ABSENT`. LuckPerms is what creates that directory; its absence is the point of the task.

- [ ] **Step 6: Document it**

In `docs/cloudnet-deployment.md`, extend the "Running locally" section with a second paragraph after the existing one:

```markdown
To run without LuckPerms — no `data/` directory, no H2 database, no library downloads — use the
Gradle task instead:

```
./gradlew :game:runWithoutLuckPerms
./gradlew :setup:runWithoutLuckPerms
```

It filters the LuckPerms loader off the class path. Every permission check then answers `TRUE`, so
`/stop` and permission-gated commands stay reachable. The same state applies during tests, where
`configurations.testRuntimeClasspath` already excludes the loader. The fat jar is unaffected and
always runs with LuckPerms.
```

- [ ] **Step 7: Commit**

```bash
git add game/build.gradle.kts setup/build.gradle.kts docs/cloudnet-deployment.md
git commit -m "feat(build): add runWithoutLuckPerms task for local runs"
```

---

## Verification

- [ ] `./gradlew build` passes.
- [ ] `./gradlew :common:test --tests '*LuckPermsSupportTest'` passes and prints the WARN line.
- [ ] `unzip -l game/build/libs/cygnus.jar | grep -c 'MinestomLoader.class'` returns `1` — production still ships LuckPerms.
- [ ] `./gradlew :game:runWithoutLuckPerms` reaches the map check without a LuckPerms banner and leaves no `data/` directory behind.
