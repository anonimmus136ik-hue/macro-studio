package com.macrostudio;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/** Reads and writes recordings as JSON files. */
public final class Macros {
	private static final Gson GSON = new Gson();

	private Macros() {
	}

	private static Path dir() {
		return Settings.dir().resolve("macros");
	}

	public static String sanitize(String raw) {
		if (raw == null) return "";
		String s = raw.trim().replaceAll("[^\\p{L}\\p{N}_\\- ]", "_");
		if (s.length() > 40) s = s.substring(0, 40);
		return s.trim();
	}

	public static List<String> list() {
		List<String> out = new ArrayList<>();
		Path d = dir();
		if (!Files.isDirectory(d)) return out;
		try (Stream<Path> s = Files.list(d)) {
			s.forEach(p -> {
				String n = p.getFileName().toString();
				if (n.endsWith(".json")) out.add(n.substring(0, n.length() - 5));
			});
		} catch (IOException ignored) {
		}
		Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
		return out;
	}

	public static boolean exists(String name) {
		return Files.exists(dir().resolve(name + ".json"));
	}

	public static Macro load(String name) {
		try {
			Path f = dir().resolve(name + ".json");
			Macro m = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Macro.class);
			if (m != null) {
				m.name = name;
				if (m.frames == null) m.frames = new ArrayList<>();
			}
			return m;
		} catch (Exception e) {
			System.err.println("[MacroStudio] Could not load " + name + ": " + e);
			return null;
		}
	}

	public static void save(Macro m) {
		try {
			Files.createDirectories(dir());
			Files.writeString(dir().resolve(m.name + ".json"), GSON.toJson(m), StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("[MacroStudio] Could not save " + m.name + ": " + e);
		}
	}

	public static void delete(String name) {
		try {
			Files.deleteIfExists(dir().resolve(name + ".json"));
		} catch (IOException ignored) {
		}
		Settings.I.macroKeys.remove(name);
		Settings.save();
	}

	public static boolean rename(String from, String to) {
		String target = sanitize(to);
		if (target.isEmpty() || exists(target)) return false;
		try {
			Files.move(dir().resolve(from + ".json"), dir().resolve(target + ".json"));
		} catch (IOException e) {
			return false;
		}
		Integer key = Settings.I.macroKeys.remove(from);
		if (key != null) Settings.I.macroKeys.put(target, key);
		Settings.save();
		return true;
	}

	public static String nextAutoName() {
		int i = 1;
		while (exists("rec_" + i)) i++;
		return "rec_" + i;
	}
}
