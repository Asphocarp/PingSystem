<p align="center">
    <img src="./common/src/main/resources/assets/sophisticated_ping/icon.png" alt="Sophisticated Ping" width="200"/>
</p>

<h1 align="center">Sophisticated Ping</h1>

Sophisticated Ping brings fast, Apex-style ping communication to Minecraft. Mark blocks, terrain, entities, dropped items, threats, loot, and team objectives without stopping to type in chat.

Tap one key to ping what you are looking at. Hold the key to open a radial wheel and choose a more specific callout.

## Why Use It

- Point teammates to locations, enemies, danger, loot, help requests, and gathering points.
- Keep multiplayer communication fast when voice chat is unavailable or too noisy.
- See pings in-world with distance labels, player info, item icons, and off-screen arrows.
- Keep groups organized with ping channels and team-aware default-channel behavior.
- Use it across Fabric, Forge, and NeoForge versions where the release matrix supports them.

## Screenshots

A ping from a teammate:

![pingFromTeammate](images/pingFromTeammate.png)

Pings at night:

![pingAtNight](images/pingAtNight.png)

## Features

- Direct pings for blocks, terrain hits, entities, and dropped items.
- Radial ping wheel with `Location`, `Attack`, `Danger`, `Help`, `Gather`, `Defend`, `Loot`, and `Confirm`.
- Moving entity pings when the target entity is available on the client.
- Item pings that can show the dropped item's icon instead of the default ping marker.
- Distance labels, ping-type labels, optional player names, and team-color rendering.
- Off-screen direction indicators so important pings do not disappear at the screen edge.
- Directional ping sound with configurable volume.
- Client settings for ping duration, distance filter, size, item icons, player info, and team colors.
- Per-server ping channels for private group callouts.
- Server settings for default-channel behavior, player tracking, and rate limiting.
- Simple Voice Chat, FTB Teams, and vanilla team context support.
- Optional Distant Horizons far-terrain raycast fallback when Distant Horizons is installed.

Entity pings are rendered client-side. The current implementation does not apply server-side glowing effects or store pings persistently on the server.

## How It Works

Default keybinds:

```text
Ping Location: Mouse5 / Forward side mouse button
Open Settings: unbound
```

Press the ping key while looking at a block, terrain hit, entity, or dropped item to create a ping. Hold the ping key to open the radial wheel, move the cursor to a ping type, and release to send it. If no wheel sector is selected, release sends a normal `Location` ping.

Looking near an existing removable ping and pinging again removes that local ping from the current client state.

## Multiplayer Tools

Channels let players send and receive pings only from players on the same channel. The client can remember different channels per multiplayer server.

Server admins can configure whether players on the default empty channel can ping globally, only within a team context, or not at all. Team context can come from Simple Voice Chat groups, FTB Teams, or vanilla teams.

## Compatibility

Sophisticated Ping currently supports Minecraft `1.19.2` through `1.21.11`, plus `26.1.2`, through the release matrix in `ci/version-matrix.yml`.

```text
Minecraft 1.19.2 - 1.20.6:   Fabric, Forge
Minecraft 1.21.1 - 1.21.11:  Fabric, Forge, NeoForge
Minecraft 26.1.2:             Fabric, NeoForge
```

Java requirements follow the Minecraft version line: Java 17 for `1.19.2` through `1.20.4`, Java 21 for `1.20.6` through `1.21.11`, and Java 25 for `26.1.2`.

## Settings And Commands

Client commands:

```text
/sophisticated_ping config
/sophisticated_ping channel
/sophisticated_ping channel <channel_name>
```

Server commands:

```text
/sophisticated_ping:server default_channel
/sophisticated_ping:server player_tracking
/sophisticated_ping:server regen_time
/sophisticated_ping:server rate_limit
```

Client config:

```text
config/sophisticated_ping.json
```

Server config:

```text
config/sophisticated_ping.server.json
```

Fabric exposes the shared settings screen through Mod Menu when Mod Menu is installed. Forge registers the same shared settings screen through its config-screen integration.

## Build

For local development, use the Java version declared by the checked-out Minecraft version. The release matrix uses Java 17, 21, or 25 depending on the target Minecraft line, so set `JAVA_VERSION` accordingly.

Build all loaders in the checked-out source tree:

```bash
JAVA_VERSION=17
JAVA_HOME=$(/usr/libexec/java_home -v "$JAVA_VERSION") ./gradlew -Dnet.minecraftforge.gradle.check.certs=false build
```

Build one loader:

```bash
JAVA_VERSION=17
JAVA_HOME=$(/usr/libexec/java_home -v "$JAVA_VERSION") ./gradlew -Dnet.minecraftforge.gradle.check.certs=false :fabric:build
JAVA_HOME=$(/usr/libexec/java_home -v "$JAVA_VERSION") ./gradlew -Dnet.minecraftforge.gradle.check.certs=false :forge:build
# On versions that include NeoForge:
JAVA_HOME=$(/usr/libexec/java_home -v "$JAVA_VERSION") ./gradlew -Dnet.minecraftforge.gradle.check.certs=false :neoforge:build
```

Run common tests:

```bash
JAVA_VERSION=17
JAVA_HOME=$(/usr/libexec/java_home -v "$JAVA_VERSION") ./gradlew :common:test
```

Production jars are written under loader-specific build directories:

```text
fabric/build/libs/
forge/build/libs/
neoforge/build/libs/
```

## Documentation

Maintainer documentation lives in [`docs/`](./docs/README.md):

- [`docs/features.md`](./docs/features.md): implemented user-visible behavior.
- [`docs/configuration.md`](./docs/configuration.md): client/server config and commands.
- [`docs/architecture.md`](./docs/architecture.md): common code, loader adapters, and service boundaries.
- [`docs/build-and-test.md`](./docs/build-and-test.md): local build, test, artifact, and runtime checks.
- [`docs/version-ladder.md`](./docs/version-ladder.md): generated Minecraft version branches.
- [`docs/ci-and-release.md`](./docs/ci-and-release.md): CI and publishing workflows.
