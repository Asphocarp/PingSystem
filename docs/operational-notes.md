# Operational Notes

This document records practical caveats for maintainers and agents working in this repo.

## Do Not Confuse These Mods

The local exemplar used for this migration is:

```text
/Users/asc/repo/Minecraft-Ping-Wheel
```

It is LukenSkyne's Ping Wheel-style direct ping implementation. It includes:

- direct ping hotkey;
- item-icon pings;
- direction indicators;
- distance labels;
- player info modes;
- channels;
- settings UI.

It does not appear to include a radial hold-and-select ping-type menu in the local repository history.

If you see a "hold TAB to select ping type" feature online, verify whether it belongs to a different mod before claiming it exists in this codebase.

## Current Implementation Status

Implemented on baseline:

- shared Ping Wheel-style ping model;
- explicit packets;
- Fabric and Forge baseline adapters;
- settings UI;
- item icon ping rendering;
- direction indicators;
- server channel/rate relay;
- Distant Horizons guarded integration code.

Needs verification or future work:

- generated branch builds after the renderer migration;
- NeoForge source overrides for the new architecture;
- runtime smoke tests on all target loaders;
- Distant Horizons runtime smoke test with DH installed;
- full branch/release matrix after regeneration.

## Distant Horizons

The common build has a compile-only Distant Horizons API dependency. Runtime code must only touch DH-specific classes when the platform service confirms `distanthorizons` is loaded.

Rules:

- keep DH calls behind `ModContext.HasDistantHorizons`;
- keep DH implementation isolated in `DistantHorizonsCompat`;
- never require DH at runtime;
- test both with and without DH installed.

## Optional Team Integrations

Simple Voice Chat, FTB Teams, and Factions are compile-only integrations. Runtime code must check `ModContext.HasVoiceChat`, `ModContext.HasFTBTeams`, or `ModContext.HasFactions` before touching integration APIs.

Rules:

- keep Simple Voice Chat API access in `VoiceChatIntegration` and `VoiceChatWrapper`;
- keep FTB Teams API access in `FTBTeamsWrapper`;
- keep Factions API access in `FactionsWrapper`;
- preserve context priority: voice group, FTB team, Factions faction, vanilla team;
- test startup without optional team mods installed.

Factions is a Fabric, server-side mod. The server relay uses Factions as authoritative teammate context when it is loaded, but clients connected to dedicated servers may not show Factions in the settings placeholder because the mod does not need to be installed client-side.

### Factions Version Matrix

All matrix branches compile the guarded Factions integration, but runtime teammate recognition requires a compatible Factions server mod.

| Minecraft | Factions artifact | Runtime support note |
| --- | --- | --- |
| 1.19.2 | v2.3.1 | Upstream-listed |
| 1.19.3 | v2.3.2 | Upstream-listed |
| 1.19.4 | 2.4.0 | Upstream-listed |
| 1.20.1 | 2.5.1 | Upstream-listed |
| 1.20.2 | 2.5.2 | Upstream-listed |
| 1.20.4 | 2.6.0 | Upstream-listed |
| 1.20.6 | 2.6.1 | Conditional: upstream lists 1.20.5, artifact metadata allows >=1.20.2 |
| 1.21.1 | 2.8.0-1.21 | Upstream-listed |
| 1.21.3 | 2.6.4 | Conditional: upstream lists 1.21.2, artifact filename targets 1.21.3 and metadata allows >=1.21.2 |
| 1.21.4 | 2.7.2 | Upstream-listed |
| 1.21.5 | 2.9.0 | Upstream-listed |
| 1.21.8 | 2.9.1 | Upstream-listed |
| 1.21.10 | 2.9.2 | Upstream-listed |
| 1.21.11 | 2.9.3 | Upstream-listed |
| 26.1.2 | 2.9.4 | Conditional: upstream lists 26.1 and 26.1.1, artifact metadata allows ~26.1 |

## Sable

Sable support is version-limited. The only current Sable source override is for `1.21.1`, where Sable Companion projects sub-level block hit positions into global world space before ping packets are sent.

Rules:

- do not add Sable classes to baseline common sources;
- add Sable dependencies only in explicit version overrides with known supported coordinates;
- smoke test both with and without Sable installed on any branch that enables the override.

## ServiceLoader Failures

Symptoms:

```text
No IPlatform... implementation found
```

Likely causes:

- missing `META-INF/services/...` file in loader resources;
- service file points to a renamed/deleted implementation;
- common code loaded before loader resources are available;
- client-only service requested on dedicated server.

Fix by checking the loader module's resources and implementation classes.

## Sound Registration

Common calls:

```java
IPlatformSoundService.INSTANCE.registerSounds(List.of("ping"))
```

Loader modules must register sound events at the correct lifecycle phase. Do not directly mutate frozen registries on Forge/NeoForge after registry freeze.

## Resource Namespace

Resources must live under:

```text
assets/sophisticated_ping/
```

Do not use old namespaces:

```text
ping_system
sophisticated-ping
ping-wheel
pingwheel
```

except inside explicit legacy migration code or attribution docs.

## Common Runtime Risks

### Client-only classes on server

Common code includes client systems, but they should only be initialized from loader client entrypoints. Server code should not touch `Minecraft`, `PoseStack`, client screens, or client render classes.

### Version-specific GUI APIs

Minecraft GUI APIs move often:

- `PoseStack` versus `GuiGraphics`;
- `Option`/`CycleOption` versus `OptionInstance`;
- widget constructor changes;
- tooltip APIs.

Keep the baseline compatible with `1.19.2`; put newer GUI API differences in overrides.

### Render Hook Drift

Mixin targets and render events drift across versions. If pings compile but do not render, inspect loader render hooks first.

### Payload ID Collisions

NeoForge is strict about custom payload registration. Keep direction-specific packet IDs and do not register two payload classes under one ID.

## Manual Test Matrix

Minimum manual matrix before release:

```text
Fabric 1.19.2
Forge 1.19.2
Forge 1.20.1
NeoForge 1.21.1
One latest 1.21.x/26.x branch
```

Smoke test each:

- launch;
- join world;
- send location ping;
- send entity/item ping;
- verify item icon option;
- verify direction indicator;
- verify settings screen;
- verify multiplayer relay if possible.

## Repository Hygiene

Avoid committing:

- `run/`
- `build/`
- `.gradle/`
- local crash reports;
- local secrets;
- test profile mods;
- `.DS_Store`.

The existing repository may contain historical generated/local files; do not expand that footprint.
