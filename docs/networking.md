# Networking

Networking is explicit, small, and direction-specific. The current packet model replaces the old fragile object serialization approach.

## Packet Interfaces

All common packets implement:

```java
app.jyu.common.network.IPacket
```

Required methods:

- `write(FriendlyByteBuf buf)`
- `boolean isCorrupt()`
- `ResourceLocation getId()`

Packet reads use `PacketHandler.readSafe(...)` to guard corrupt payloads.

## Packet Types

Current packet IDs:

```text
sophisticated_ping:ping_location_c2s
sophisticated_ping:ping_location_s2c
sophisticated_ping:update_channel_c2s
```

Files:

```text
common/network/PingLocationC2SPacket.java
common/network/PingLocationS2CPacket.java
common/network/UpdateChannelC2SPacket.java
```

## Ping Location C2S

`PingLocationC2SPacket` fields:

```java
String channel
Vec3 pos
UUID entity
int sequence
int dimension
```

The client sends this packet after raycasting a valid target. `entity` is nullable. If the target is an entity, its UUID is sent so clients can track item/player/entity movement.

## Ping Location S2C

`PingLocationS2CPacket` fields:

```java
String channel
Vec3 pos
UUID entity
int sequence
int dimension
UUID author
```

The server constructs this from the C2S packet and adds the sender UUID as `author`.

## Update Channel C2S

`UpdateChannelC2SPacket` fields:

```java
String channel
```

Clients send this when joining a server or changing channel config.

## Packet Safety

Each packet has:

- a default constructor creating a corrupt sentinel object;
- a buffer constructor for normal decoding;
- `readSafe(...)` wrapper.

If decoding throws, `PacketHandler` returns the corrupt sentinel. Server/client handlers check `isCorrupt()` and ignore or warn instead of crashing.

## Platform Network Service

Common code sends packets through:

```java
IPlatformNetworkService.INSTANCE.sendToServer(packet)
IPlatformNetworkService.INSTANCE.sendToClient(packet, serverPlayer)
```

Loader implementations decide how to register and transport packets.

### Fabric

Fabric uses Fabric API networking:

```text
ClientPlayNetworking.send(...)
ServerPlayNetworking.send(...)
ServerPlayNetworking.registerGlobalReceiver(...)
ClientPlayNetworking.registerGlobalReceiver(...)
```

### Forge

Forge baseline uses `EventNetworkChannel` per packet ID. `ForgeMain` creates channels and registers handlers. `PlatformNetworkServiceImpl` stores channel mappings and sends vanilla custom payload packets when the remote side has the channel.

### NeoForge

NeoForge generated branches should use modern custom payload APIs. Keep C2S and S2C IDs separate to avoid duplicate-payload registration crashes.

## Server Relay Rules

Server relay is implemented in `ServerCore`.

Flow:

```text
onPingLocation(server, sender, packet)
  -> reject corrupt packet
  -> apply sender rate limit
  -> enforce default-channel mode
  -> sync sender channel if needed
  -> hide target player UUID if player tracking disabled
  -> relay to eligible players
```

Eligibility:

- recipients must be on the same explicit channel;
- default-channel recipients depend on `ChannelMode`;
- `GLOBAL` allows all default-channel players;
- `TEAM_ONLY` requires same default team context;
- `DISABLED` rejects default-channel pings.

Default team context is resolved as:

1. Simple Voice Chat group if both players have a group;
2. FTB Teams non-personal team if both players have a team;
3. vanilla Minecraft team.

Voice Chat has priority over FTB Teams when both integrations are present.

## Rate Limiting

Server rate limiting uses:

```text
common/util/RateLimiter.java
```

Config fields:

```java
ServerConfig.msToRegenerate
ServerConfig.rateLimit
```

`RateLimiter.setRates(...)` is called during server init.

## Player Tracking

`ServerConfig.playerTrackingEnabled` controls whether pinging a player entity should relay that player UUID.

When disabled:

- if the target UUID belongs to a server player;
- server sends the ping position but clears `entity`;
- clients render a static location ping rather than tracking that player.

## Sequence And Replacement

Pings contain `sequence`. The client uses author plus sequence to add or replace pings. This prevents every correction tick from creating a new visual ping.

Client correction behavior is controlled by:

```java
ClientConfig.correctionPeriod
```
