# Rendering

Sophisticated Ping uses a Ping Wheel-style two-phase render model:

1. world render updates ping screen positions from camera/model/projection state;
2. GUI overlay render draws pings, item icons, labels, distances, and direction indicators.

This avoids the old unstable direct world-render marker pipeline.

## Main Classes

```text
common/core/PingView.java
common/core/PingManager.java
common/render/WorldRenderContext.java
common/render/OverlayRenderer.java
common/render/PingLocationRenderer.java
common/render/DirectionIndicatorRenderer.java
common/render/DrawContext.java
common/math/MathUtils.java
common/math/ScreenPos.java
```

## World Render Phase

Loader code captures the current render state and calls:

```java
CommonClient.INSTANCE.onRenderWorld(WorldRenderContext ctx)
```

`WorldRenderContext` contains:

- `modelViewMatrix`
- `projectionMatrix`
- `tickDelta`
- `camera`

`PingManager.updatePings(ctx)` updates every active `PingView`. Each `PingView` resolves its world position and calls `MathUtils.worldToScreen(...)`.

## World-to-Screen Projection

`MathUtils.worldToScreen`:

- subtracts the camera position from the world position;
- applies the model-view matrix;
- applies the projection matrix;
- divides by `w`;
- maps normalized device coordinates into GUI coordinates;
- preserves depth so behind-camera/off-screen pings can be handled by direction indicators.

The renderer does not simply clamp all pings into the viewport. Pings behind the camera or outside safe screen bounds are handled by `DirectionIndicatorRenderer`.

## GUI Overlay Phase

Loader code calls:

```java
CommonClient.INSTANCE.onRenderGUI(PoseStack poseStack, float tickDelta)
```

`OverlayRenderer.draw(...)`:

- ignores invalid game states;
- prepares safe-zone data for direction indicators;
- draws in-screen pings with `PingLocationRenderer`;
- draws off-screen/behind-camera pings with `DirectionIndicatorRenderer`.

## Ping Rendering

`PingLocationRenderer.draw(...)` renders:

- ping icon or item icon;
- distance text;
- player label/head depending on config;
- team-based coloring depending on config.

`DrawContext` centralizes lower-level GUI drawing:

- default ping icon;
- arrow icon;
- player head overlay;
- item entity icon rendering;
- text positioning/scaling.

## Item Icon Feature

When `ClientConfig.itemIconVisible` is true and the ping targets an item entity, the ping icon is replaced with that item stack's texture/model.

Relevant code:

```text
PingView        resolves target entity and stores ItemStack
PingLocationRenderer delegates icon drawing
DrawContext    renders item or default ping icon
```

This is the README/gallery feature from the Ping Wheel exemplar: "replaces the ping icon with the respective item texture (or model)."

## Direction Indicators

`DirectionIndicatorRenderer` handles pings that are:

- behind the camera;
- outside the viewport;
- outside configured safe-zone bounds.

Client config safe-zone fields:

```java
safeZoneLeft
safeZoneRight
safeZoneTop
safeZoneBottom
```

The indicator can render either:

- an arrow icon;
- the item/default ping icon, depending on config and ping state.

## Loader Hooks

### Fabric

Fabric uses mixin-backed callbacks:

```text
fabric/mixin/LevelRendererMixin.java
fabric/mixin/GuiMixin.java
fabric/event/WorldRenderCallback.java
fabric/event/GuiRenderCallback.java
```

`LevelRendererMixin` captures world render matrices. `GuiMixin` emits GUI overlay callbacks.

### Forge

Forge uses loader event adapters in:

```text
forge/platform/PlatformClientEventServiceImpl.java
```

The adapter bridges Forge client tick, login/logout, world render, and GUI render events into the common client event service.

### NeoForge

NeoForge render hooks are version-specific and live in generated branch overrides. Use the same common event service contract. For versions without a stable event matching the needed matrix data, use a loader-side LevelRenderer mixin equivalent rather than making world render a no-op.

## Visual Acceptance Checklist

- A location ping remains visually locked to the world point while the camera moves.
- A ping behind the camera shows a direction indicator instead of disappearing.
- Item entity pings show item icons when enabled.
- Item entity pings fall back to default icon when item icons are disabled.
- Distance text stays readable and does not overlap the icon excessively.
- Player labels/heads follow `PlayerInfoMode`.
- Team coloring follows `TeamColorMode`.
- Pings expire according to client duration.

