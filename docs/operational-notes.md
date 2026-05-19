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

