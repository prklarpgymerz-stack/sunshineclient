# Sunshine Client — Phase 5 update (2026-10-18)

## Changes in this source update

- Keystrokes and mouse visualizer now share the same configured widget geometry. The LMB/RMB row is placed below the actual resized SPACE bar rather than below a fixed-size widget.
- Mouse buttons now use the same height as the keystroke tiles, with vertically centered labels.
- Balanced mode uses a flat dark backdrop and disables menu hover/switch interpolation to reduce cosmetic rendering work. Quality retains the animated, gradient-backed presentation.
- Replaced the fixed crosshair-area text decoration with client-side player-entity custom nameplates. Player Nametags uses a yellow sun and gold nickname by default, and a red sun/name for `Prosek_MC`; the local player's tag is hidden in first person. Original client-side custom-name values are restored when the feature is turned off or player entities unload.

## Verification status

Source archive integrity can be checked, but this environment has Java 21 and no configured Java 25/Gradle toolchain for Minecraft 26.2. A full Gradle compile and in-game test were therefore **not performed**. The nametag implementation uses the entity custom-name API and still needs runtime validation in the target Fabric 26.2 instance.

## Follow-up update (2026-10-18)
- Moved EDIT HUD into the left sidebar header so it no longer collides with the Keybinds button or sits over the search field.
- Added a draggable SPRINTING INDICATOR preview/position to the HUD editor. Its saved X/Y values are used by the in-game indicator and can be reset with Reset Layout.
- Balanced now retains the earlier yellow-to-black visual style with fewer extra effects; Quality adds layered panel glow and a small animated highlight sweep.
- Added the missing Armor HUD configuration toggle and a simple durability percentage renderer referenced by the existing module list.
- Source-only update. Build and in-game testing still need to be run with Java 25 and the matching Fabric toolchain.


## Visual redesign follow-up
- Replaced the vanilla title screen button arrangement with a dedicated Sunshine title screen: yellow-to-black background, branded header, framed feature/status panel, and Singleplayer / Multiplayer / Settings / Quit actions.
- Expanded Quality mode into a full visual treatment for the Right Shift menu: layered amber atmosphere, glass-like panel framing, branded header/status pill, enhanced module cards, and enabled-state chips.
- Balanced mode intentionally keeps the simpler earlier Sunshine style.
- Design direction is inspired by the clean, structured client-menu feel of OneClient, while keeping Sunshine's own layout and yellow/black branding rather than copying its UI directly.
- These source changes have not been compiled or tested in Minecraft yet.


## Self-nametag fix follow-up

- Replaced the old approach of changing Player custom names every tick with a client-side renderer mixin.
- The renderer now supplies the yellow sun + nickname, or red for `Prosek_MC`, and explicitly controls visibility so the local nameplate is hidden in first-person and shown in third-person.
- This change is source-only and still requires a Java 25 / Gradle 9.5.1 build and in-game verification on Minecraft 26.2.


## Quality UI redesign follow-up
- Reworked the title screen into an asymmetric hero layout with a horizontal navigation dock to avoid a generic vertical button stack.
- Expanded the Quality Right Shift workspace and added stronger header/content separation, broad ambient lighting, and more deliberate module tiles.
- Balanced remains intentionally simpler.
- Source-only change; compilation and in-game verification are still required.


### Motion Blur addition
- Added a Visual-category Motion Blur module with an adjustable 1–100% strength setting.
- Added a temporal screen-space previous-frame blend pass and saved settings in `sunshine.json`.
- Disabled by default; requires compilation and in-game renderer/performance verification on Minecraft 26.2.
