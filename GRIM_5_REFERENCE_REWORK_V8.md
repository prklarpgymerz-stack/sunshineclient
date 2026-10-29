# Grim-reference layout rework v8

This pass uses the user-provided Grim 5.0 screenshot as the concrete layout reference, while retaining Sunshine branding and a yellow/grey palette.

Changes in `SunshineScreen.java`:
- Wider workspace and narrower category sidebar proportions.
- Compact search field positioned in the top bar when Grim Skid Theme is enabled.
- Compact category labels and a FEATURES sidebar label.
- Flat, dark horizontal module rows with slim yellow enabled rails instead of outlined cards.
- Reduced row gaps and smaller settings affordances.
- Grim-themed content heading is compact and separated from the top bar.

Build target remains the existing Fabric project. This source has not been compiled or tested in Minecraft in this environment; verify with the project's Java 25 / Fabric Loom setup.
