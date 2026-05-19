# Sophisticated Ping Documentation

This directory is the maintainer documentation for Sophisticated Ping. It describes the current multi-loader architecture, branch/version workflow, runtime systems, build/release process, and known caveats.

The current baseline branch is `1.19.2`. New code should land on the baseline first, then be regenerated or forward-ported through the Minecraft version ladder.

## Documentation Map

- [Architecture](./architecture.md): module layout, shared code boundaries, lifecycle, service-provider model.
- [Features](./features.md): user-visible ping features, settings, commands, and current non-features.
- [Rendering](./rendering.md): Ping Wheel-style projection, overlay drawing, item icons, direction indicators, and render hooks.
- [Networking](./networking.md): packet flow, channel IDs, server relay, rate limiting, and packet safety.
- [Configuration](./configuration.md): client/server JSON config, settings UI, commands, and migration behavior.
- [Build And Test](./build-and-test.md): local builds, Java versions, artifacts, unit tests, and troubleshooting.
- [Version Ladder](./version-ladder.md): matrix, generated branches, overrides, and rules for version-specific changes.
- [CI And Release](./ci-and-release.md): GitHub Actions workflows, artifacts, publishing, tags, and reruns.
- [Operational Notes](./operational-notes.md): current limitations, Distant Horizons notes, loader caveats, and maintenance checklist.
- [Multi-loader Release Model](./multiloader-release.md): concise release model summary.

## Repository Overview

Sophisticated Ping is a Minecraft ping mod with shared gameplay logic and thin loader adapters.

Active module layout:

```text
common/       shared gameplay, config, rendering, packet models, tests
fabric/       Fabric entrypoints, networking, events, mixins, Mod Menu
forge/        Forge entrypoint, networking, events, config screen registration
ci/           ordered Minecraft version matrix and generated-branch overrides
scripts/      migration helper used by CI and local branch generation
.github/      CI, branch regeneration, and release workflows
docs/         maintainer documentation
```

Planned/generated module layout on newer branches:

```text
neoforge/     NeoForge entrypoint, payload registration, events, keybinds
```

The project has intentionally moved away from the original single-loader `PingPoint` renderer design. Current common code mirrors the local Ping Wheel exemplar's stable split:

- world-render phase updates screen positions from camera-relative projection data;
- GUI overlay phase draws pings, labels, item icons, and off-screen arrows;
- server relays ping packets based on channel and team/default-channel policy;
- platform adapters expose loader differences through Java `ServiceLoader` interfaces.

## Identity Rules

Use these names consistently:

```text
Display name: Sophisticated Ping
Mod id:       sophisticated_ping
Namespace:    sophisticated_ping
Package root: app.jyu
Archive base: sophisticated_ping
```

Use `Sophisticated Ping` when human-facing spaces/capitals are allowed. Use `sophisticated_ping` for mod ids, resource namespaces, artifact names, and machine-readable identifiers.

## Current Baseline

`gradle.properties` on `1.19.2` currently declares:

```properties
mod_version=1.2.2
minecraft_version=1.19.2
java_version=17
fabric_loader_min_version=0.14.9
forge_version=43.3.0
```

The baseline includes Fabric and Forge. NeoForge starts only on newer generated branches where the version matrix declares it.

## Quick Commands

Build the current branch:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew -Dnet.minecraftforge.gradle.check.certs=false build
```

Build one loader:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew -Dnet.minecraftforge.gradle.check.certs=false :fabric:build
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew -Dnet.minecraftforge.gradle.check.certs=false :forge:build
```

List declared versions/loaders:

```bash
./scripts/migrate-version.sh --versions
./scripts/migrate-version.sh --loaders 1.19.2
```
