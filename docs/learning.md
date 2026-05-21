# Learning Notes

This file records project-specific lessons that were surprising, unusual, or easy to forget. Keep entries concise and practical so future agents can quickly avoid repeating the same investigation.

## Forge Risk Builds On macOS Can Fail Before Project Compilation

While validating matrix-wide Factions support, the first risk build hit a non-Factions dependency problem while configuring the Forge project on macOS:

```text
Could not find lwjgl-freetype-3.3.3-natives-macos-patch.jar
org.lwjgl:lwjgl-freetype:3.3.3
```

Gradle searched Maven Central for `lwjgl-freetype-3.3.3-natives-macos-patch.jar` and failed during Forge project configuration, before the Factions integration or common sources could be meaningfully compiled for that row.

Practical implications:

- Treat this as a generated-branch/toolchain dependency blocker, not evidence that Factions support is broken.
- Prefer static matrix wiring checks when this local macOS Forge native-resolution issue blocks compilation.
- Re-run full generated-branch loader builds in CI or an environment that resolves the Forge/LWJGL native patch artifact before claiming matrix build coverage.
