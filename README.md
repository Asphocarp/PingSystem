<p align="center">
    <img src="./common/src/main/resources/assets/sophisticated_ping/icon.png" alt="Sophisticated Ping" width="200"/>
</p>

<h1 align="center">Sophisticated Ping</h1>

Sophisticated Ping for Minecraft. Make MC Apex Again!

## Sophisticated Ping

Default hotkey: `Mouse5` (`Forward` side mouse button)

Send signal to your team for communication.

Press hotkey again towards the Ping Point to cancel it.

## Images

A ping from teammate:

![pingFromTeammate](images/pingFromTeammate.png)

Pings at night:

![pingAtNight](images/pingAtNight.png)


## Build

- use jdk-17 (due to Minecraft 1.19.2 Fabric)
  - e.g., `"java.import.gradle.java.home": "/opt/homebrew/opt/openjdk@17"` in your vscode user settings Json
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17 
# build, run
./gradlew build
./gradlew runClient
# to show the .jar mod file
ls -la build/libs
```

## Detailed Design

Core Functionality:
- Dual Ping Types: Location pings (static world markers) and entity pings (dynamic tracking with glow effects)
- Advanced Rendering: 3D world-space boxes and 2D HUD icons with distance/owner info using MVP matrix transformations
- Smart Raycasting: 512-block range with entity prioritization and fluid detection toggle
- Audio System: 4 custom sounds + configurable audio selection

Technical Architecture:
- Multi-platform: Fabric (primary) and Forge support via Architectury
- Networking: Custom packet system with multicast distribution
- Client-Server: Server applies entity glow effects, client handles rendering and input
- Configuration: ModMenu integration with ClothConfig for extensive customization

Key Features:
- Default `Mouse5` (`Forward`) key binding with toggle functionality
- Thread-safe ping storage with automatic cleanup
- Screen projection mathematics for accurate HUD positioning
- Entity interpolation for smooth tracking of moving targets
- Configurable ping limits, colors, sizes, and timeouts

Current Status: Supports Minecraft 1.20.1, fully functional ping system with planned improvements for team systems, forge
compatibility, and minimap integration.
