# Configuration

Sophisticated Ping has separate client and server JSON configs. The config model is adapted from Ping Wheel and is managed by shared common classes.

## Config Files

Client config:

```text
config/sophisticated_ping.json
```

Server config:

```text
config/sophisticated_ping.server.json
```

Config handler:

```text
common/config/ConfigHandler.java
```

## Client Config

Class:

```text
common/config/ClientConfig.java
```

User-facing fields:

```java
int pingVolume
int pingDuration
int pingDistance
float correctionPeriod
boolean itemIconVisible
boolean directionIndicatorVisible
PlayerInfoMode playerInfoMode
TeamColorMode teamColorMode
int pingSize
String channel
Map<String, String> serverChannels
```

Advanced fields:

```java
int removeRadius
int raycastDistance
int safeZoneLeft
int safeZoneRight
int safeZoneTop
int safeZoneBottom
```

### Channel Storage

`ClientConfig.getChannel()` is server-aware:

- in singleplayer/no server IP, it uses `channel`;
- on multiplayer servers, it reads/writes `serverChannels[currentServerIp]`.

This lets players keep different channels per server.

### Channel Update

When client config changes, `onUpdate()` sends:

```java
new UpdateChannelC2SPacket(getChannel())
```

This keeps the server's `PLAYER_CHANNELS` map synchronized.

## Server Config

Class:

```text
common/config/ServerConfig.java
```

Fields:

```java
ChannelMode defaultChannelMode
boolean playerTrackingEnabled
int msToRegenerate
int rateLimit
```

`defaultChannelMode` controls what an empty client channel means:

- `GLOBAL`: default-channel players can see each other globally.
- `TEAM_ONLY`: default-channel players must share team context.
- `DISABLED`: clients must choose a non-empty channel.

## Settings UI

Shared screen:

```text
common/screen/SettingsScreen.java
common/screen/OptionUtils.java
```

The settings screen mirrors the Ping Wheel UX:

- ping volume slider;
- ping duration slider;
- ping distance slider;
- correction period slider;
- item icons toggle;
- direction indicator toggle;
- player info mode cycle;
- team color mode cycle;
- ping size slider;
- channel text box with synchronized default placeholder;
- Done button that saves config.

Fabric opens this via Mod Menu. Forge registers a config screen factory. The settings keybind also opens the shared screen.

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

Command builders:

```text
common/command/ClientCommandBuilder.java
common/command/ServerCommandBuilder.java
```

## Validation

`ClientConfig.validate()` truncates channels longer than `MAX_CHANNEL_LENGTH`.

Packet constructors also enforce maximum channel length when reading/writing buffers.

## Legacy Migration

Legacy migration code:

```text
common/compat/LegacyMigrationHandler.java
```

It exists to move old config/key/resource names toward the current `sophisticated_ping` namespace. Keep this code conservative. It should never crash the game if legacy files are absent or malformed.

## Localization

English localization:

```text
common/src/main/resources/assets/sophisticated_ping/lang/en_us.json
```

When adding new settings/commands, add keys here first. Use:

```java
LanguageUtils.settings("...")
LanguageUtils.of("value", "...")
LanguageUtils.command("...")
```

instead of hard-coded user-facing strings where possible.

