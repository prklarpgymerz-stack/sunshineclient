# Sunshine Client — Phase 5 Update

Fabric client-side mod source for Minecraft 26.2.

## Follow-up fixes
- EDIT HUD button moved into the sidebar header to prevent overlap with Keybinds/search.
- SPRINTING INDICATOR is now represented in the HUD editor and can be dragged to a saved position.
- Balanced keeps the earlier yellow-to-black look with fewer decorative effects; Quality gets the upgraded layered glow and animated highlight.
- Added missing Armor HUD config/toggle and a basic durability display.

## Build
Requires JDK 25 and Gradle with the Fabric Loom dependencies specified in `build.gradle` / `gradle.properties`. This source ZIP has not yet been compiled or verified in-game.


### UI profiles
- **Quality:** asymmetric hero-style Sunshine title screen, horizontal navigation dock, overview panel, expanded Right Shift workspace, richer module tiles, and layered yellow/black surfaces.
- **Balanced:** retains the lighter, simpler menu presentation.
- UI layout is inspired by modern Minecraft client menus, but uses Sunshine's own design.

Build and in-game testing are still required before treating this archive as a verified release.

## Motion Blur

The Visual category includes an experimental temporal screen-space motion blur inspired by common PvP-client blur controls. Toggle **Motion Blur** and open its module settings to tune strength from **1% to 100%**. The effect is disabled by default and stores its settings in `config/sunshine.json`. It uses a previous-frame after-image pass, so performance and renderer compatibility must be tested on the target Minecraft build before release.
