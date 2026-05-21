# 1.3.0

- Add optional Factions teammate recognition for default-channel ping routing.
- Pin Factions integration artifacts across the generated version matrix.
- Add Fabric metadata and contract tests so generated branches keep Factions support wired.
- Document conditional Factions runtime support for 1.20.6, 1.21.3, and 26.1.2.
- Fix generated 1.21.1 loader builds by sharing the Sable compile-only classpath with loader source compiles.
- Fix Sable Companion Maven resolution for the generated 1.21.1 build.
- Clear row-scoped Sable matrix properties during branch migration so unsupported later branches do not inherit the 1.21.1 Sable dependency.

# 1.2.5

- Add a CurseForge Upload API preflight check before release builds or uploads.
- Publish CurseForge before Modrinth/GitHub for each artifact to avoid partial public releases when CurseForge rejects credentials.
- Supersede `1.2.4`, which was aborted after the configured CurseForge credential was rejected as malformed.

# 1.2.4

- Retry Gradle builds in CI/release workflows to absorb transient Maven/plugin repository failures.
- Superseded before public completion because the configured CurseForge credential was rejected by the Upload API.

# 1.2.3

- Superseded before public completion because the first CurseForge token secret was malformed.

- Fix ping wheel cursor selection on HiDPI displays.
- Publish CurseForge through the platform deploy workflow when a CurseForge project ID is configured.
- Fix generated 1.19.3+ migration sources for the JOML matrix API.
- Add the 1.20.6 Fabric typed custom-payload override required by Fabric API 0.100+.
- Add the 1.20.6 Forge GUI and world-render overrides required by Forge 50 API changes.
- Add the 1.21.1 Forge sound registration override required by private `ResourceLocation` constructors.
- Add the 1.21.3+ Forge world-render mixin fallback after Forge removed `RenderLevelStageEvent`.
- Add the 1.21.8+ Forge EventBus 7 and networking overrides required by the `BusGroup` listener API and moved `EventNetworkChannel` API.
- Add the 1.21.11 Forge identifier rename override after `ResourceLocation` became `Identifier`.

# 1.2.2

- Reserved during CI hardening; no public release.

# 1.2.1

- Add Ping Wheel-style rendering, settings, and radial ping selection.
