package com.macrostudio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Settings {
	/** GLFW_KEY_RIGHT_CONTROL */
	public int openKey = 345;
	public int recordKey = -1;
	public int stopKey = -1;
	public boolean recordArmed = false;
	public Map<String, Integer> macroKeys = new LinkedHashMap<>();
	public List<Bind> binds = new ArrayList<>();
	public int[] dig1 = null;
	public int[] dig2 = null;
	public int digDepth = 2;
	/** Maximum camera turn in degrees per client tick. */
	public int digTurnSpeed = 6;
    /** Preview options: no automatic server interactions. */
    public boolean selectionVisuals = true;
    public boolean showActiveLayer = true;
    public boolean showRoute = false;
    public boolean drillPreview = false;
    public boolean digUp = false;
    /** Window percentage of available GUI width, not global Minecraft GUI scale. */
    public int uiWidthPercent = 84;
    public int uiHeightPercent = 90;
    public boolean buildPreview = false;
    /** 0 floor, 1 walls, 2 fill. Preview only. */
    public int buildMode = 0;
    public boolean resourceHud = false;
    public boolean marketAssistant = false;
    public int marketPrice = 400;
    public boolean[] resourceFilter = {true, true, true, true, true, true, true};
	public boolean critSound = false;
	public boolean[] keepSlots = defaultKeep();
	public boolean noRain = false;
	public int timeMode = 0;
	public boolean brightMax = false;
	public int zoomKey = -1;
	public boolean autoSprint = false;
	public boolean hpAlarm = false;
	public int hpHearts = 3;
	public boolean hudCoords = false;
	public boolean hudArmor = false;
	public boolean hudWaypoints = false;
	public List<Waypoint> waypoints = new ArrayList<>();

	public static final class Waypoint {
		public String name = "";
		public int x;
		public int y;
		public int z;
	}

	/** Inventory slots that are kept (not put into chests): by default the hotbar (0-8). */
	public static boolean[] defaultKeep() {
		boolean[] k = new boolean[36];
		for (int i = 0; i < 9; i++) k[i] = true;
		return k;
	}
	// Convenience modules (disabled by default).
	public boolean autoTool = false;
	public boolean autoRefill = false;
	public boolean autoEat = false;
	public int eatBelow = 14;
	public boolean shulkerPreview = false;

	public boolean autoDeposit = false;
	public String fullCmd = "";
	public String backCmd = "";
	public List<int[]> chests = new ArrayList<>();
	public boolean optOn = false;
	public int optCrowd = 0;
	public int optDist = 0;
	public boolean optZone = false;
	public int[] optZ1 = null;
	public int[] optZ2 = null;

	public static final class Bind {
		public int key = -1;
		public String lines = "";
	}

	public static Settings I = new Settings();

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static Path dir() {
		return FabricLoader.getInstance().getConfigDir().resolve("macrostudio");
	}

	private static Path file() {
		return dir().resolve("settings.json");
	}

	public static void load() {
		try {
			Path f = file();
			if (Files.exists(f)) {
				Settings s = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Settings.class);
				if (s != null) {
					if (s.macroKeys == null) s.macroKeys = new LinkedHashMap<>();
					if (s.binds == null) s.binds = new ArrayList<>();
					if (s.chests == null) s.chests = new ArrayList<>();
					if (s.waypoints == null) s.waypoints = new ArrayList<>();
					if (s.keepSlots == null || s.keepSlots.length != 36) s.keepSlots = defaultKeep();
                    if (s.resourceFilter == null || s.resourceFilter.length != 7) s.resourceFilter = new boolean[] {true,true,true,true,true,true,true};
                    s.uiWidthPercent = Math.max(55, Math.min(96, s.uiWidthPercent));
                    s.uiHeightPercent = Math.max(65, Math.min(97, s.uiHeightPercent));
                    s.marketPrice = Math.max(1, Math.min(100000000, s.marketPrice));
					if (s.fullCmd == null) s.fullCmd = "";
					if (s.backCmd == null) s.backCmd = "";
					I = s;
				}
			}
		} catch (Exception e) {
			System.err.println("[MacroStudio] Could not load settings: " + e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(dir());
			Files.writeString(file(), GSON.toJson(I), StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("[MacroStudio] Could not save settings: " + e);
		}
	}

	/** Keys that belong to the mod itself and must not be recorded. */
	public static Set<Integer> reservedKeys() {
		Set<Integer> r = new HashSet<>();
		r.add(I.openKey);
		r.add(I.recordKey);
		r.add(I.stopKey);
		r.add(I.zoomKey);
		r.addAll(I.macroKeys.values());
		return r;
	}
}
