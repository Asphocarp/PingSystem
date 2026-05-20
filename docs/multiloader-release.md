# Multi-loader Release Model

Sophisticated Ping follows a Ping Wheel-style release shape:

- one Minecraft version per branch;
- one Gradle multi-project build per branch;
- shared behavior in `common`;
- thin loader adapters in `fabric`, `forge`, and, on supported newer branches, `neoforge`;
- feature work lands on the baseline branch first;
- generated branches are recreated from the baseline plus deterministic migration commits.

## Baseline Branch

Current baseline:

```text
1.19.2
```

Do normal feature and bug-fix work on `1.19.2`. Do not hand-edit generated branches for durable fixes.

## Target Branches

The ordered branch list is stored in:

```text
ci/version-matrix.yml
```

Current matrix includes:

```text
1.19.2
1.19.3
1.19.4
1.20.1
1.20.2
1.20.4
1.20.6
1.21.1
1.21.3
1.21.4
1.21.5
1.21.8
1.21.10
1.21.11
26.1.2
```

## Loader Policy

Use the `loaders` field from the matrix as the source of truth.

General policy:

- pre-1.21 branches: Fabric and Forge;
- 1.21+ branches: Fabric, Forge, and NeoForge where declared;
- future/latest branch: Fabric and NeoForge where declared.

NeoForge should not be added to old branches where the ecosystem does not support it.

## Branch Generation

Use:

```text
.github/workflows/regenerate-version-branches.yml
```

or locally:

```bash
./scripts/migrate-version.sh <minecraft-version>
```

Each generated branch should be the previous version branch plus exactly one commit:

```text
:alien: migrate v${mod_version} to ${minecraft_version}
```

Version-specific changes belong in:

```text
ci/version-overrides/<minecraft-version>/
```

## Artifact Names

Artifacts are named:

```text
sophisticated_ping-${mod_version}-${loader}-${minecraft_version}.jar
```

## Tag Names

Release tags are named:

```text
v${mod_version}-${loader}-${minecraft_version}
```

Example:

```text
v1.2.5-fabric-1.19.2
```

## Publishing

Use:

```text
.github/workflows/platform-deploy.yml
```

The workflow:

1. checks out each requested version branch;
2. builds each requested loader if declared by the matrix;
3. uploads artifacts;
4. publishes serially through `publish.gradle`.

GitHub, Modrinth, and CurseForge are supported. CurseForge requires an existing CurseForge project ID configured as `CURSEFORGE_PROJECT` and an Upload API token configured as `CURSEFORGE_TOKEN`.

When CurseForge publishing is enabled, the workflow validates the token before building or uploading. Use the CurseForge Authors API Tokens page token, not the bcrypt-like Personal API Key. Per artifact, CurseForge publishes before Modrinth/GitHub so a CurseForge rejection does not leave new partial releases on the other platforms.
