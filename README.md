<p align="center">
    <img src="./src/main/resources/assets/ping_system/icon.png" alt="PingSystem" width="200"/> 
</p>

<h1 align="center">PingSystem</h1>

Ping System for Minecraft. Make MC Apex Again!

## Ping System

Default hotkey: `c`

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


## Todo

- [ ] run server
- [ ] confirm ping of teammate; team system; 0.1f
- [ ] d Display the same size across different GUI scales. (see ping wheel)
- [ ] d support forge
- [ ] d show direction
- [ ] get current key for info
- [ ] 2 (hard) automatic pathfinding
- [ ] (hard-compatibility) show on minimap
- [x] (hard) ping entity
- [x] config for mod with mod menu
- [x] support 1.19.2, 1.20.1

Differentiation: 
- [ ] 1. Compatible with various styles from games like Apex Legends, Valorant, League of Legends, and Overwatch. (icon, sound, etc.)
- [ ] 2. Team system (channel), confirm, say no