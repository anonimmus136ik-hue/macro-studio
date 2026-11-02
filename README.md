# Macro Studio MAX 2.3.0 — Obsidian Edition

Fabric client-side mod for **Minecraft Java 1.21.11** (official Mojang mappings). This is the editable source, not a web mockup. The project still includes original MAX 2.2 macros, binds, assisted mining, auto tools, deposit, and HUD modules.

## How to build and install

1. Open **Actions → Build Macro Studio MAX 2.3.0** on GitHub.
2. Download the latest **successful** `MacroStudio-MAX-2.3.0` artifact.
3. Extract `macro-studio-2.3.0.jar` from that ZIP.
4. Put the JAR into the Fabric 1.21.11 instance's `mods` directory. Install the matching Fabric API, and remove older Macro Studio JARs.
5. **Right Ctrl** opens the in-game menu. The text **Obsidian Edition** at the top identifies this version.

The Gradle wrapper uses Java 25 for Fabric Loom; the mod's class bytecode is Java 21.

## Implemented in this branch

### Mining / 3D selection
- Resizable territory using two points (POS1 and POS2); adjust X/Z in **Smart Route** and depth in **Mining Studio**.
- Native Minecraft 1.21.11 Gizmos: transparent 3D selection volume, highlighted current layer, point A/B boxes, current mining target.
- **Mine Up / Down** selection direction, with fixed Y layers in the digger queue.
- **Drill 3×3×2 preview** renders the 18 estimated block cells in the direction of the clicked face. This is visual only, **not an integration with any private server-side drill**.\n- **Drill Tool Assist**: save the currently held custom tool ID and choose one matching block type by looking at a block before opening the GUI. If the block matches, the miner selects that drill; on other types it chooses an ordinary correct harvesting tool from the hotbar when available. The server still decides actual mined cells.
- Mining no longer searches up to ten previously visited columns for old targets.
- Bounded horizontal A* navigation avoids known floor holes on loaded terrain; on blocked paths, the miner stops with an explicit reason rather than stepping into the void. The world is not scanned through walls for ores.
- Client movement still uses normal keys, camera turning and reach rules; no packet spoofing or anti-cheat bypass.

### Build Studio
- Hologram-only preview of a floor, perimeter walls or fill volume using selected points.
- **No automatic block placement** is implemented in this version.

### Market & resources
- Resource selection and **real inventory totals** for diamonds, emeralds, gold, iron, redstone, coal and stone including raw/ingot/ore variants from loaded inventory.
- Optional inventory resource counter HUD.
- Price controls and **manual copy** of `/market sell PRICE`, only while holding a complete stack of 64 eligible, selected resource items (not tools or other valuables). The player checks the price and submits the chat command themselves.
- No automatic selling, teleporting to warps, X-Ray, container manipulation or server-side market API integration.

### Obsidian UI
- Dark slate, emerald and muted gold accents.
- Dashboard, Mining Studio, Smart Route, Build Studio, Market/Ores, Automation, Macros, Binds, Deposit, Visual, Performance and Settings.
- Width and height customization, accessible pagination for smaller GUI resolutions, responsive native buttons.
- Preserves **Right Ctrl** as the default menu shortcut.

## Limitations and safety

The A* walker only considers **horizontal, currently loaded and walkable surfaces**, with solid floor and enough headroom. It intentionally does not bridge, fly, jump holes, teleport or mine inaccessible targets. Upward mining needs a stable reachable position (scaffolding is not created). Verify any server's rules before enabling automation. Some multiplayer servers prohibit mining bots and automatic market listings.

Save custom drill with **Smart Route → SAVE HELD AS DRILL** and choose a matching block type using **SET LOOKED BLOCK TYPE**, then enable **DRILL TOOL ASSIST**. These settings identify a tool; they do not discover private server recipes or synchronize proprietary 3×3×2 effects.\n\nThe drill footprint visualization does **not** establish which server-specific blocks a custom drill will break; that requires knowing the drill's actual item behavior and testing on a server that allows automation. This is not a compatibility claim for ReallyWorld's proprietary mechanics.

**A green GitHub build confirms compilation only.** Test the rendered outlines, Digger route safety, settings layout and Minecraft startup in an actual world before calling the mod production ready.

## Manual QA checklist

- [ ] Right Ctrl opens and closes the UI at multiple GUI scales
- [ ] POS1 / POS2, Smart Route X/Z resize and Mine Up/Down show correct bounds
- [ ] 3D selection and Active Layer appear in-world
- [ ] Drill 3x3x2 preview updates in all 6 face directions
- [ ] A* route avoids a hole; no route stops safely
- [ ] Mining progresses through one Y level at a time without stepping on air
- [ ] Build Studio Floor / Walls / Fill shows only holograms
- [ ] Market/Ores counts match the 36 inventory slots
- [ ] Full stack is required before copy; no command is sent automatically
- [ ] Stop all releases movement and attack keys
