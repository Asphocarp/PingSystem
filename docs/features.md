# Features

This document describes the user-visible features of Sophisticated Ping as implemented by the current codebase.

## Summary

Sophisticated Ping lets players mark locations and entities in the world with multiplayer-visible pings. The current implementation is modeled after Ping Wheel's direct ping UX: press the ping key to mark the target under the crosshair, then the mod renders a HUD marker with distance, optional player information, item icons, team colors, and off-screen direction indicators.

The mod does **not** currently implement a radial hold-and-select ping-type menu. If that feature is added later, it should be documented as a separate interaction model.

## Default Controls

Default keybinds:

```text
Ping Location: Mouse Button 5
Open Settings: unbound
```

The ping keybinding is registered under the `Sophisticated Ping` key category.

If the ping key is bound to Minecraft's pick-block key, Sophisticated Ping only consumes the pick-block click when the normal pick-block action would not apply. This avoids stealing pick-block from creative/build workflows.

## Location Pings

Press the ping key while looking at a block or valid raycast hit to create a location ping.

Behavior:

- client raycasts from the camera direction;
- client sends the hit position to the server;
- server relays the ping to eligible players;
- clients render the ping at that world position.

Location pings are static. They remain at the original world coordinate until replaced or expired.

## Entity Pings

If the raycast hits an entity, the client includes that entity UUID in the ping packet.

Behavior:

- clients resolve the entity locally by UUID;
- if found, the ping follows the entity;
- ping position is adjusted upward by the entity bounding-box height so the marker sits above the entity;
- if the entity is not found locally, the ping falls back to the last known packet position.

Server option:

```text
playerTrackingEnabled
```

When player tracking is disabled and the target entity is a player, the server strips the entity UUID before relay. Recipients then see a static position ping instead of a player-tracking ping.

## Item Icon Pings

When an entity ping targets an item entity and item icons are enabled, the ping icon is replaced with the item's texture/model.

Setting:

```text
Item Icons
```

Config field:

```java
ClientConfig.itemIconVisible
```

This is the feature shown in Ping Wheel's gallery text: item pings can render the respective item texture or model instead of the default ping icon.

## Distance Labels

Every visible ping shows a distance label in meters.

Behavior:

- distance is computed from the camera position to the ping position;
- displayed format uses one decimal place;
- pings are sorted far-to-near before drawing, so nearer markers draw after farther markers.

Example display:

```text
12.4m
```

## Player Information

Pings carry the author UUID. Clients use that UUID to look up player info from the current connection.

Player info modes:

```text
HOLD
DISABLED
ALWAYS
COMPACT
```

Behavior:

- `HOLD`: show verbose player label while the Minecraft player-list key is held.
- `DISABLED`: do not show player info.
- `ALWAYS`: always show verbose player label.
- `COMPACT`: show compact player info near the distance label.

The settings screen explains the `HOLD` mode using the current player-list keybinding.

## Team Colors

Pings can use the author's team color.

Team color modes:

```text
FULL
DISABLED
PING_ONLY
LABELS_ONLY
```

Behavior:

- `FULL`: apply team color to ping icon and labels.
- `DISABLED`: use normal white rendering.
- `PING_ONLY`: apply team color only to the ping icon.
- `LABELS_ONLY`: apply team color only to labels.

The baseline implementation uses vanilla Minecraft teams for team context and team color.

## Direction Indicators

When a ping is off screen or behind the camera, Sophisticated Ping draws an edge-of-screen direction indicator instead of hiding the ping.

Setting:

```text
Direction Indicator
```

Config field:

```java
ClientConfig.directionIndicatorVisible
```

The indicator uses:

- an arrow icon rotated toward the ping direction;
- the ping icon or item icon;
- safe-zone boundaries so indicators avoid screen edges and HUD areas.

Safe-zone config fields:

```java
safeZoneLeft
safeZoneRight
safeZoneTop
safeZoneBottom
```

These fields are hidden from the settings screen and can be edited in the JSON config.

## Ping Scaling

Ping size scales with distance and the user-configured ping size.

Setting:

```text
Ping Size
```

Config field:

```java
ClientConfig.pingSize
```

Default:

```text
100%
```

The renderer applies a distance-based scale and then multiplies it by `pingSize / 100`.

## Ping Sound

When a ping is accepted on the client and is in the client's current dimension, the client plays a directional ping sound.

Setting:

```text
Ping Volume
```

Config field:

```java
ClientConfig.pingVolume
```

Behavior:

- `0%` disables audible ping playback;
- higher values scale the sound volume;
- sound source is `MASTER`;
- sound position is the ping world position.

## Ping Duration And Expiry

Pings expire automatically.

Setting:

```text
Ping Duration
```

Config field:

```java
ClientConfig.pingDuration
```

Behavior:

- default duration is 7 seconds;
- max duration is treated as infinite;
- expired pings are removed during world-render updates.

## Ping Distance Filter

Clients can hide pings beyond a configured distance.

Setting:

```text
Ping Distance
```

Config field:

```java
ClientConfig.pingDistance
```

Behavior:

- `0` hides all incoming pings by distance filter;
- max distance is treated as infinite;
- incoming S2C pings farther than the configured distance are ignored.

This is a client-side display filter. It does not stop the server from relaying pings.

## Ping Correction And Replacement

Pings include an author UUID and sequence number. The client stores pings by author plus sequence.

Behavior:

- if a new ping has the same author and sequence, it replaces the existing ping;
- if the correction period has elapsed, a new sequence is used;
- this allows repeated/corrective pings without flooding the HUD with duplicates.

Setting:

```text
Correction Period
```

Config field:

```java
ClientConfig.correctionPeriod
```

Max correction period is treated as infinite/manual replacement behavior.

## Remove Nearby Ping

If a ping action is queued while an existing ping is close enough to the crosshair, the client removes the existing local ping instead of sending a new ping.

Config fields:

```java
ClientConfig.removeRadius
ClientConfig.correctionPeriod
```

Behavior:

- only removable pings are considered;
- the nearest ping to screen center is selected;
- removal is local client-side state in the current implementation.

## Channels

Channels let players send and receive pings only from players in the same channel.

Client setting:

```text
Ping Channel
```

Client config fields:

```java
ClientConfig.channel
ClientConfig.serverChannels
```

Behavior:

- empty channel means "default channel";
- non-empty channel must match between sender and recipient;
- channel names are capped at `MAX_CHANNEL_LENGTH`;
- multiplayer channels can be stored per server IP.

The client sends a channel update packet when joining a server and when config changes.

## Default Channel Server Modes

Server admins can control empty-channel behavior.

Server config field:

```java
ServerConfig.defaultChannelMode
```

Modes:

```text
AUTO
DISABLED
GLOBAL
TEAM_ONLY
```

Behavior:

- `DISABLED`: players must choose a non-empty channel before pinging.
- `GLOBAL`: empty-channel players can see each other's pings globally.
- `TEAM_ONLY`: empty-channel pings only relay within the same team context.
- `AUTO`: present in config enum; behavior should be verified before documenting as a distinct policy in user-facing docs.

## Rate Limiting

The server can rate-limit ping usage per player.

Server config fields:

```java
ServerConfig.msToRegenerate
ServerConfig.rateLimit
```

Behavior:

- every player has an independent rate limiter;
- `rateLimit <= 0` disables the limit;
- `msToRegenerate` controls how quickly usages regenerate;
- disconnect clears the player's limiter state.

## Settings Screen

The shared settings screen exposes the main client-facing options.

Open it through:

- configured settings keybind;
- `/sophisticated_ping config`;
- Mod Menu on Fabric;
- Forge config screen integration.

Controls:

- Ping Volume
- Ping Duration
- Ping Distance
- Correction Period
- Item Icons
- Direction Indicator
- Player Info
- Team Color
- Ping Size
- Ping Channel

## Commands

Client command:

```text
/sophisticated_ping config
/sophisticated_ping channel
/sophisticated_ping channel <channel_name>
```

Server command:

```text
/sophisticated_ping:server default_channel
/sophisticated_ping:server player_tracking
/sophisticated_ping:server regen_time
/sophisticated_ping:server rate_limit
```

Commands return localized feedback with the Sophisticated Ping prefix.

## Distant Horizons Far Raycast

If Distant Horizons is installed and the normal raycast misses, Sophisticated Ping can attempt an async far-terrain raycast through the Distant Horizons API.

Behavior:

- DH classes are isolated behind `DistantHorizonsCompat`;
- code only calls DH after mod detection confirms `distanthorizons` is loaded;
- if DH is absent, normal pings still work and the game should start normally.

This feature needs runtime smoke testing whenever the DH API dependency or target Minecraft version changes.

## Multiplayer Behavior

In multiplayer:

- client sends ping intent to server;
- server validates and relays;
- recipients are filtered by channel/default-channel/team policy;
- clients ignore S2C packets whose channel does not match current client channel;
- sender can receive their own ping if relay rules include them.

## Not Currently Implemented

These are not currently implemented features:

- radial hold-and-select ping-type wheel;
- multiple semantic ping types such as attack/defend/look/go-here;
- persistent server-side ping storage;
- server-authoritative ping removal packets;
- non-vanilla team integrations such as FTB Teams or Simple Voice Chat groups;
- Bukkit/plugin interoperability in this repository.

