# CloudNet deployment

How to deploy the `game` and `setup` services as CloudNet services.

## Artifacts

| Artifact | Maven | Goes to |
|---|---|---|
| `cygnus.jar` | `cygnus-game` | service root (application file) |
| `setup.jar` | `cygnus-setup` | service root (application file) |
| `bridge.jar` | `cygnus-bridge` | `extensions/` |

## How CloudNet starts the service

```
java … -javaagent:<wrapper.jar>
       -Dservice.bind.host=<host> -Dservice.bind.port=<port>
       -cp <wrapper.jar>:<app.jar>  <Main-Class from the app manifest>
```

- The application jar is launched through `-cp` plus its manifest `Main-Class`, not through `-jar`. Both jars end
  up in one classloader, which is why no CloudNet artifact may ever be bundled into the fat jar — everything
  CloudNet-related stays `compileOnly`.
- `service.bind.host` / `service.bind.port` are always set by the node. Standalone runs fall back to
  `localhost:25565`; no system properties are needed for local testing.
- To stop a service the node writes `end` and then `stop` to stdin. `stop` triggers a clean shutdown;
  `end` is not a registered command and is ignored.

## Service directory layout

Everything except `data/` belongs in the CloudNet template. `data/` is created by LuckPerms on first start and
holds its config and H2 database — it is per service and must not be shared between services.

```
<service>/                        <service>/
├── cygnus.jar                    ├── setup.jar
├── extensions/                   ├── extensions/
│   ├── CloudNet-Bridge.jar       │   ├── CloudNet-Bridge.jar
│   └── bridge.jar                │   └── bridge.jar
├── game/maps/<map>/…             ├── setup/maps/<map>/…
└── data/                         └── data/
```

Map paths are resolved relative to the working directory and are hardcoded: `game/maps` for the game service,
`setup/maps` for the setup service. A game map needs a `region/` folder and a `map.json`; the setup service only
requires `region/`.

## Extensions

Both jars in `extensions/` are required for the CloudNet integration to be complete:

- **`CloudNet-Bridge.jar`** — CloudNet's own bridge. Provides the player manager, service info updates and
  fallback handling.
- **`bridge.jar`** — registers a permission checker that resolves through Adventure's `PermissionChecker`
  pointer, which our player implementation backs with LuckPerms. Without it CloudNet falls back to a checker that
  only inspects the Minestom permission level, which is always `0` here — maintenance bypass and task-level
  `requiredPermission` would then reject every player, staff included.

`bridge.jar` declares a dependency on `CloudNet_Bridge`. If the CloudNet bridge is missing, it is skipped with a
log message and the server still starts normally. That is also what happens when running locally without CloudNet.

The name of the application file (`cygnus.jar` / `setup.jar`) is defined by the Minestom service environment, not
by this repository. Check the environment registration if the node cannot find the application file.

## Verifying a deployment

1. Start one service and check the log. `CygnusCloudNetPermissions requires an extension called CloudNet_Bridge`
   means the CloudNet bridge is missing from `extensions/` and permission checks are not wired up.
2. Run `stop <service>` on the node. The process must exit on its own instead of being killed after a timeout —
   the log ends with `[cygnus-shutdown] Stopping Minestom server` and `[luckperms-shutdown-hook] Goodbye!`.
3. Put the task into maintenance and join with an account holding `cloudnet.bridge.maintenance`. Getting through
   confirms the whole permission chain; being kicked points back to step 1.

## Running locally

```
java -jar build/libs/cygnus.jar
```

Binds `localhost:25565`. Run it from a directory that contains `game/maps` (or `setup/maps`). `stop` on the
console shuts it down. No CloudNet, no extensions and no system properties are required.
