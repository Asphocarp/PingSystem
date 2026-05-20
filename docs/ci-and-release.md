# CI And Release

This repo has separate workflows for branch-local builds, full matrix builds, branch regeneration, and platform releases.

## Workflows

```text
.github/workflows/ci.yml
.github/workflows/all-branches-ci.yml
.github/workflows/regenerate-version-branches.yml
.github/workflows/platform-deploy.yml
```

## Branch-local CI

Workflow:

```text
ci.yml
```

Triggers:

- push to any branch;
- pull request.

Behavior:

1. extracts `java_version` from branch-local `gradle.properties`;
2. installs that JDK;
3. validates Gradle wrapper;
4. reads declared loaders from `scripts/migrate-version.sh --loaders <minecraft_version>`;
5. builds each declared loader.

It also has a branch freshness check that warns when configured branches do not contain the baseline marker.

## All Branches CI

Workflow:

```text
all-branches-ci.yml
```

Triggers:

- manual dispatch;
- weekly schedule.

Behavior:

1. checks out baseline `1.19.2`;
2. builds a matrix from `ci/version-matrix.yml`;
3. checks out each version branch;
4. sets up the declared Java version;
5. builds each declared loader;
6. uploads artifacts per version/loader.

Use this workflow to verify that published branches still build.

## Regenerate Version Branches

Workflow:

```text
regenerate-version-branches.yml
```

This workflow rewrites generated version branches from the baseline ladder. It has `contents: write` permission and pushes generated branches with `--force-with-lease`.

Use it after changes to:

- `ci/version-matrix.yml`;
- `ci/version-overrides/**`;
- shared baseline code that must be replayed forward.

Failure rule: if a generated branch fails to build, do not treat later branches as valid. Fix the baseline recipe or overrides, then rerun.

Minecraft 1.20.6 is the first generated branch in this repo that needs Fabric's typed `CustomPacketPayload` networking override while still keeping the older Forge branch layout. It also needs a Forge 50 GUI render hook override because `RenderGuiEvent` is not present in the compiled Forge API for that branch. Keep those splits in `ci/version-overrides/1.20.6/`; do not move the 1.20.6 Fabric payload wrappers or Forge GUI hook into the `1.20.1` or baseline sources.

Minecraft 1.21.1 is the first branch where Forge sources must stop calling `new ResourceLocation(namespace, path)`. Put Forge-specific registrations that need resource IDs under `ci/version-overrides/1.21.1/forge/` so later generated branches inherit the modern factory method.

Minecraft 1.21.3 and later Forge branches no longer expose the `RenderLevelStageEvent` API used by the 1.21.1 adapter. Those branches use a Forge-only `LevelRenderer` mixin under `ci/version-overrides/1.21.3/forge/` to fire the same internal world-render callback, plus a `1.21.8` mixin signature override for the newer renderer method. The same override uses `CustomizeGuiOverlayEvent.Chat` because the old `RenderGuiEvent` is not available in Forge 53. Keep the `sophisticated_ping.forge.mixins.json` config and conditional `MixinConfigs` manifest behavior together with those overrides.

Minecraft 1.21.8 and later Forge branches use Forge EventBus 7. Do not import `net.minecraftforge.eventbus.api.IEventBus` or the old `net.minecraftforge.eventbus.api.SubscribeEvent` package in those generated sources. The override under `ci/version-overrides/1.21.8/forge/` uses `FMLJavaModLoadingContext` constructor injection, stores the mod `BusGroup`, and registers listeners through per-event `BUS` fields or `getBus(BusGroup)` methods. Forge 58 also moved the `EventNetworkChannel` API to `net.minecraftforge.network` and expects channels to be created with `ChannelBuilder`, so keep the Forge network override with the EventBus 7 override. The same override uses `sender.level().getServer()` instead of `ServerPlayer#getServer()` so it survives the 1.21.10 server-player API cleanup.

## Platform Deploy

Workflow:

```text
platform-deploy.yml
```

Manual inputs:

```text
versions
loaders
publish_curseforge
```

Behavior:

1. prepares a version/loader matrix from requested inputs and declared loader support;
2. checks out each version branch;
3. builds the loader artifact;
4. uploads temporary build artifacts;
5. serially publishes releases with `publish.gradle`.

Publishing uses:

```text
me.modmuss50.mod-publish-plugin
```

## GitHub Releases

Release tags:

```text
v${mod_version}-${loader}-${minecraft_version}
```

Example:

```text
v1.2.3-fabric-1.19.2
```

The workflow deletes any existing GitHub release/tag for a rerun before republishing:

```bash
gh release delete "$RELEASE_TAG" --cleanup-tag --yes || true
```

## Modrinth

Publishing uses `MODRINTH_TOKEN` and `MODRINTH_PROJECT`.

Fabric releases declare Fabric API as a required dependency. Forge/NeoForge releases are currently marked beta in `publish.gradle`.

## CurseForge

CurseForge publishing is wired through the same `me.modmuss50.mod-publish-plugin` release path as GitHub and Modrinth. The platform deploy workflow passes:

```text
CURSEFORGE_TOKEN
CURSEFORGE_PROJECT
```

`CURSEFORGE_TOKEN` must be a GitHub Actions secret. `CURSEFORGE_PROJECT` may be either a repository variable or a secret.

CurseForge requires a numeric project ID for uploads. The upload API posts files to `/api/projects/{projectId}/upload-file`; project creation is done in the CurseForge Authors dashboard, and the project ID is shown in that project URL/dashboard. If `publish_curseforge` is true and either value is missing, the workflow fails before running `gradle publishMods` so skipped CurseForge releases are not mistaken for successful publishing.

## Release Artifacts

Expected artifact name:

```text
sophisticated_ping-${mod_version}-${loader}-${minecraft_version}.jar
```

The release workflow moves the loader output to:

```text
build/libs/${artifact_name}.jar
```

before publishing.

## Secrets

Expected GitHub Actions secrets:

```text
MODRINTH_TOKEN
CURSEFORGE_TOKEN
```

`GITHUB_TOKEN` is provided by GitHub Actions.

Expected repository variable or secret after the CurseForge project exists:

```text
CURSEFORGE_PROJECT
```

Never commit tokens or local `temp/secrets.md`.

## Release Checklist

Before publishing:

1. verify baseline builds;
2. regenerate version branches if baseline or overrides changed;
3. run all-branches CI;
4. inspect artifacts for correct names;
5. dry-run or publish one version/loader if changing workflow logic;
6. run `platform-deploy.yml` for the target matrix.

After publishing:

1. confirm GitHub releases exist;
2. confirm Modrinth files exist;
3. confirm loader/version metadata is correct;
4. test at least one Fabric, Forge, and NeoForge jar manually.
