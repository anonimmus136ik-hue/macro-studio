# Macro Studio MAX 2.3.0

Fabric client mod for **Minecraft Java 1.21.11**. Current source is in this repository, with no nested ZIP.

## Download and install

1. Open [GitHub Actions](https://github.com/anonimmus136ik-hue/macro-studio/actions) and select the newest **successful build** for main.
2. Download the **MacroStudio-MAX-2.3.0** artifact, unzip it, and place **only** the JAR macro-studio-2.3.0.jar in your active Minecraft instance mods directory.
3. Remove older macro-studio JARs from that same mods directory. Install Fabric API for 1.21.11.
4. When entering a world, look for chat confirmation "Macro Studio MAX 2.3.0 загружен".

## 3D mining selection: key 0

- Press **0** (top-row zero, not numpad) in the world to start 3D selection.
- **Right-click #1:** choose the first ground block. Horizontal Minecraft X/Z and ground height Y are saved.
- **Right-click #2:** choose the opposite horizontal corner (X/Z). This creates a rectangular floor area.
- **Right-click #3:** point at a block at your desired second Y height and right-click. This sets Y only; its X/Z are ignored.
- **Red in-world wireframe:** blocks selected for top-down mining.
- **Green in-world cells:** *candidate* walking places on the one-block outer rim; shown only if the client sees solid ground and two blocks of air above. This is a **preliminary preview**, not pathfinding or a guarantee of safety.
- Press **0** during selection to cancel without deleting the old saved area. Use **CLEAR AREA** in Mining to erase it.
- Selection right-clicks cancel vanilla block placement only while 3D selection is on.
- The 3D outline uses normal depth-tested rendering, not wallhack/ESP.

## GUI

Press **Right Ctrl**. The **MINING** tab has fixed buttons **START / PAUSE / RESUME / STOP**, **3D SELECT [0]**, manual POS1/POS2 input, true square checkbox toggles, red/green 2D map, progress, volume, turn speed, depth and safety settings.

**Mouse wheel scrolling works inside the content panel**. Long settings lists are bounded to the panel, with a scrollbar. Existing modules include Recorder, Replayer, AutoTool, AutoRefill, AutoEat, AutoDeposit, Shulker Preview, HUD and performance.

Optional safety controls: protect containers, avoid fluids/unbreakable blocks, stop on damage or low hunger, floor/gap checks and emergency stop. The mod rotates the normal client camera and only attacks where the real view ray hits.

The mod does not implement AutoBuild, AutoFish or AutoFarm and does not show fake switches for these.

## Build

- Fabric Loom runs Gradle on **Java 25**.
- Compiled Java bytecode target is **Java 21**.
- Windows: gradlew.bat clean build
- Linux: ./gradlew clean build

GitHub Actions verifies the 2.3.0 mod version and required class files. Passing CI confirms compilation, not tested operation in Minecraft. Test in a disposable single-player world first.
