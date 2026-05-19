# Multi-loader release model

Sophisticated Ping follows the same release shape as the Ping Wheel exemplar:

- one Minecraft version per branch;
- shared implementation in `common`;
- thin loader adapters in `fabric`, `forge`, and, on supported newer branches, `neoforge`;
- feature work lands on the oldest supported maintenance branch first, then is replayed forward.

## Branches

Use `1.18.2` as the canonical oldest supported maintenance branch once it exists. Forward-port feature and bug-fix commits to the active Minecraft branches:

- `1.18.2`
- `1.19.2`
- `1.19.3`
- `1.19.4`
- `1.20.1`
- `1.20.2`
- `1.20.4`
- `1.20.6`
- `1.21.1+`

Each branch should differ only in Minecraft/loader versions, Java version, Gradle plugin requirements, loader metadata, and the minimum compatibility code needed for API changes.

## Loaders

The `1.20.1` branch builds Fabric and Forge. Do not add NeoForge to this branch. Add a `neoforge` module on branches where NeoForge is part of the target ecosystem, starting with the newer branch line.

## Releases

Artifacts are named:

```text
Sophisticated_Ping-${mod_version}-${loader}-${minecraft_version}.jar
```

Tags are named:

```text
v${mod_version}-${loader}-${minecraft_version}
```

Use `.github/workflows/platform-deploy.yml` for manual matrix releases across branches and loaders. The publish step is dry-run when platform tokens are absent.
