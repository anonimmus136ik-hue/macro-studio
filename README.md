# Macro Studio MAX 2.2.0

Client-side Fabric mod for Minecraft Java 1.21.11. This repo contains ONE current version as editable source. Old uploaded ZIP versions are removed from main.

## Install the real mod

1. Open GitHub Actions in this repository.
2. Open latest successful "Build Macro Studio MAX 2.2.0" on main.
3. Download the "MacroStudio-MAX-2.2.0" artifact ZIP and extract it.
4. Place ONLY macro-studio-2.2.0.jar into the active Minecraft instance's mods folder. Remove all previous macro-studio JARs from that same instance.
5. Enter a world. The message "Macro Studio MAX 2.2.0 загружен" confirms the new version has loaded.

Open the GUI with Right Ctrl. On MINING use POS1 LOOK / FEET, POS2 LOOK / FEET, then START, PAUSE / RESUME and STOP. LOOK uses the block captured when opening the menu.

## Build

Fabric Loom needs Java 25 to run Gradle. Compiled bytecode targets Java 21. Build using gradlew.bat build (Windows) or ./gradlew build (Linux).

## Implemented

Mining Studio with smooth camera turning, real reach/aim checks, selection, progress, pause; Recorder/Replayer, AutoTool, AutoRefill, AutoEat, AutoDeposit, Shulker Preview, HUD, keybinds and visual/performance settings.

Not implemented: AutoFish, AutoFarm, AutoBuild, A* pathfinding and 3D in-world selection.

A successful CI build confirms compilation, not successful use in Minecraft. Historical Git commits and old Actions logs remain accessible.
