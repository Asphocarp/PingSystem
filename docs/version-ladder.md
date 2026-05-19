# Version Ladder

Sophisticated Ping uses a generated version ladder. The baseline branch contains shared implementation and migration recipes. Generated branches should be disposable outputs of the baseline plus one migration commit per Minecraft version.

## Baseline

Current baseline:

```text
1.19.2
```

The baseline contains:

- common implementation;
- Fabric and Forge baseline adapters;
- version matrix;
- migration script;
- version overrides;
- CI/release workflows.

Do feature work on `1.19.2` first.

## Version Matrix

Source of truth:

```text
ci/version-matrix.yml
```

Each version entry declares:

- `minecraft_version`
- `java_version`
- `loaders`
- loader dependencies and compatible ranges
- Gradle/plugin versions where needed
- NeoForge/NeoForm properties where needed

Query matrix:

```bash
./scripts/migrate-version.sh --baseline
./scripts/migrate-version.sh --versions
./scripts/migrate-version.sh --loaders 1.21.1
```

## Migration Script

Script:

```text
scripts/migrate-version.sh
```

Responsibilities:

1. read `ci/version-matrix.yml`;
2. update `gradle.properties`;
3. copy files from `ci/version-overrides/<target>/`;
4. process optional `.delete` file entries.

It does not invent API ports. If a branch needs source changes, encode them in `ci/version-overrides/<version>/`.

## Overrides

Override directory shape:

```text
ci/version-overrides/<minecraft-version>/
```

Files inside an override are copied over the repo root during migration.

Use overrides for:

- Gradle/plugin changes;
- settings changes;
- module inclusion/exclusion;
- loader metadata changes;
- Minecraft API differences;
- source files required only for that version.

Use `.delete` for files/directories that must be removed on that generated branch.

Example:

```text
ci/version-overrides/1.21.1/settings.gradle
ci/version-overrides/1.21.1/common/build.gradle
ci/version-overrides/1.21.1/neoforge/build.gradle
ci/version-overrides/1.21.1/.delete
```

## Generated Branch Rule

Generated branches should contain exactly one migration commit relative to the previous version branch:

```text
1.19.2 baseline
  -> 1.19.3 = baseline + migrate commit
  -> 1.19.4 = 1.19.3 + migrate commit
  -> 1.20.1 = 1.19.4 + migrate commit
```

Manual edits on generated branches are not durable. Put the fix in baseline matrix/overrides and regenerate.

## Regeneration Workflow

Workflow:

```text
.github/workflows/regenerate-version-branches.yml
```

Manual dispatch behavior:

1. checks out `1.19.2`;
2. configures Git;
3. installs Java 17, 21, and 25;
4. loops through matrix versions;
5. checks out previous branch as base;
6. runs `scripts/migrate-version.sh <version>`;
7. commits `:alien: migrate v${mod_version} to ${version}`;
8. builds declared loaders;
9. pushes with `--force-with-lease`;
10. uploads branch artifacts.

## Loader Policy

Baseline/pre-1.21:

```text
fabric, forge
```

1.21+ generated branches:

```text
fabric, forge, neoforge
```

Future/latest experimental branch:

```text
fabric, neoforge
```

Always trust `loaders` in `ci/version-matrix.yml`.

## Current Caveat

The codebase recently migrated away from the old renderer and deleted stale Java override sources. That prevents generated branches from reintroducing old `PingPoint`/`RenderHandler` code, but newer branches still need verified source overrides for the new architecture where Minecraft APIs changed.

Before claiming full matrix support, run the branch regeneration workflow or build each branch locally.

