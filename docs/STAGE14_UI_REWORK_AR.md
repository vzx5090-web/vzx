# Stage 14 — Studio-like UI Rework

## What changed
- Replaced the simplified single-screen prototype with a Studio-style workspace layout.
- Added a top title/menu strip and ribbon tabs: File, Home, Model, Avatar, UI, Script, Test, View, Plugins.
- Added a dynamic Ribbon tool strip with Select, Move, Scale, Rotate, Insert, Script, Anchor, Group, Toolbox and panel controls.
- Reorganized desktop layout into Toolbox (left), 3D Viewport (center), and Explorer + Properties/Attributes (right).
- Added an Output dock beneath the viewport.
- Added mobile panel navigation for Toolbox, Viewport, Explorer, Properties and Script.
- Rebuilt the script editor with line numbers, basic Luau syntax highlighting, synchronized scrolling, Tab indentation and basic Enter indentation.
- Added Orientation editing, rotated Part geometry, selected-object outline and XYZ transform axes.
- Added touch drag Move/Scale/Rotate tools for the currently selected physical object.
- Added local Group/Ungroup, Align-to-grid, Anchor and CanCollide commands.
- Expanded the default Explorer tree with common services and placed new Script/LocalScript/ModuleScript/GUI instances in their appropriate service containers.
- Kept the DataModel, local project import/export, limited RBXMX support and Luau native bridge.

## What is still not Roblox Studio
This application is an independent Android editor prototype inspired by Roblox Studio's interface. It is not an official Roblox product and does not contain the Roblox Engine.
- Playtest/Game simulation, Roblox physics, replication, networking and live game services are unavailable.
- Toolbox currently contains local starter insertions; it is not connected to the Creator Store.
- Avatar rigging, animation editor, plugins, advanced model/CSG and full UI engine are not implemented.
- Viewport selection still uses approximate tap selection; Explorer is the reliable selection method.
- Luau runs against a limited compatibility prelude, not the complete Roblox API.
- Touch performance and visuals still need validation on a physical Android device.

## Local validation
- JavaScript syntax: PASS.
- Stage 14 UI smoke test: PASS.
- WebGL viewport mock test: PASS, including the additional selection/gizmo line passes.
- Compatibility static contract: PASS.
- Luau literal/lifter tests: PASS.
- Official Luau VM smoke test and Android ARM64 build are run by the GitHub Actions pipeline.
