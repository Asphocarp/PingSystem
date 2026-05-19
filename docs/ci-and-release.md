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

## Platform Deploy

Workflow:

```text
platform-deploy.yml
```

Manual inputs:

```text
versions
loaders
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
v1.2.1-fabric-1.19.2
```

The workflow deletes any existing GitHub release/tag for a rerun before republishing:

```bash
gh release delete "$RELEASE_TAG" --cleanup-tag --yes || true
```

## Modrinth

Publishing uses `MODRINTH_TOKEN` and `MODRINTH_PROJECT`.

Fabric releases declare Fabric API as a required dependency. Forge/NeoForge releases are currently marked beta in `publish.gradle`.

## CurseForge

`publish.gradle` supports CurseForge only if both are present:

```text
CURSEFORGE_TOKEN
CURSEFORGE_PROJECT
```

If `CURSEFORGE_PROJECT` is absent, CurseForge publishing is skipped. This keeps CurseForge optional.

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
CURSEFORGE_TOKEN optional
```

`GITHUB_TOKEN` is provided by GitHub Actions.

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

