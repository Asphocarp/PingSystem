# Build And Test

This document covers local verification for the current branch and generated version branches.

## Java Versions

Use the Java version declared by `gradle.properties` on the current branch:

```bash
grep '^java_version=' gradle.properties
```

Current `1.19.2` baseline:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

Known version policy:

- `1.19.2` through `1.20.4`: Java 17.
- `1.20.6` and `1.21.x`: Java 21.
- `26.1.2`: Java 25.

The source of truth is always `ci/version-matrix.yml`.

## Build Everything On Current Branch

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew -Dnet.minecraftforge.gradle.check.certs=false build
```

The Forge cert property is used because ForgeGradle certificate checking can fail in local/CI environments unrelated to mod source correctness.

## Build One Loader

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew -Dnet.minecraftforge.gradle.check.certs=false :fabric:build
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew -Dnet.minecraftforge.gradle.check.certs=false :forge:build
```

On branches that include NeoForge:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew -Dnet.minecraftforge.gradle.check.certs=false :neoforge:build
```

## Tests

Common tests live under:

```text
common/src/test/java
```

Current packet tests cover:

- C2S ping location round trip;
- S2C ping location round trip;
- channel update round trip;
- corrupt packet guard behavior.

Run common tests:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :common:test
```

## Expected Artifacts

Loader build outputs are under:

```text
fabric/build/libs/
forge/build/libs/
neoforge/build/libs/
```

Artifact naming policy:

```text
sophisticated_ping-${mod_version}-${loader}-${minecraft_version}.jar
```

There may be sources/dev jars depending on the loader task. Use the non-sources production jar for game testing.

## Local Game Testing

Minimum manual smoke test:

1. Place the loader jar in the profile's `mods` folder.
2. Start the correct Minecraft version and loader.
3. Join singleplayer and multiplayer if possible.
4. Press the ping key.
5. Verify the ping appears at the targeted world position.
6. Ping an item entity and verify item icon display when enabled.
7. Move camera behind/off-screen and verify direction indicator.
8. Verify pings expire.
9. Open settings screen and change a value.
10. Restart and verify config persists.
11. With Simple Voice Chat installed, verify default-channel pings relay inside a voice group and not outside it.
12. With FTB Teams installed, verify default-channel pings relay inside a non-personal FTB team and not outside it.
13. With both Simple Voice Chat and FTB Teams installed, verify voice groups take priority.
14. On the `1.21.1` generated branch with Sable installed, verify block pings inside sub-levels appear at global positions.

## Runtime Acceptance Checklist

Client:

- keybinds register;
- settings screen opens;
- ping hotkey triggers raycast;
- Distant Horizons absent does not crash startup;
- Simple Voice Chat, FTB Teams, and Sable absent do not crash startup;
- item pings render item icons when enabled;
- player labels follow `PlayerInfoMode`;
- direction indicators work for off-screen pings.

Server:

- corrupt packets are ignored or warned, not fatal;
- sender rate limit works;
- channel rules are enforced;
- disconnect removes channel/rate state;
- relay does not duplicate or omit expected recipients.

## Common Build Failures

### Missing Java Version

If `/usr/libexec/java_home -v 21` or `-v 25` fails, install that JDK first or use a concrete `JAVA_HOME`.

### Mappings/API Drift

Symptoms:

- `cannot find symbol` for Minecraft classes;
- method names changed across Minecraft versions;
- `ResourceLocation` constructor failures on newer branches.

Fix:

- keep baseline code compiling on `1.19.2`;
- put version-specific differences in `ci/version-overrides/<version>/`;
- regenerate the branch after changing overrides.

### Fabric Mixin Warning

Fabric may warn about locating a render method mapping. Treat warnings seriously for runtime tests, but build success means the source compiled. If the render hook fails at runtime, add a version-specific mixin override for that branch.

### NeoForge Payload Registration

NeoForge custom payload IDs must be unique per direction. Do not register C2S and S2C handlers against the same payload ID.
