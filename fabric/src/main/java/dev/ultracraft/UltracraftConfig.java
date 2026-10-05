package dev.ultracraft;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import net.fabricmc.loader.api.FabricLoader;

/**
 * config/ultracraft.properties, written with defaults on first start (and missing settings added later). Most of it
 * is set from the Ultracraft settings screen (pause menu or Options, the "Ultracraft" button).
 */
public final class UltracraftConfig {
	/**
	 * Height ULTRAKILL renders V1's layer at, scaled up to fill Minecraft's window (0 = Minecraft's full size).
	 * Every frame crosses from one game to the other, so this is the big framerate knob; 720 matches ULTRAKILL's
	 * own chunky look and is about 4x less work than 1440p.
	 */
	public static int v1Height = 720;
	/** Become V1 as soon as ULTRAKILL is ready (no F8 needed). */
	public static boolean autoV1 = true;
	/** ULTRAKILL's enemies spawn in the dark like Minecraft's monsters (also switched on a shop's Sandbox page). */
	public static boolean ukSpawns = true;
	/** Minecraft's monsters spawn (bosses like the Ender Dragon and the Wither always do). */
	public static boolean mcMobs = true;
	/** Using a shop's screen, ULTRAKILL draws at Minecraft's full size, so SmileOS's small text is sharp. */
	public static boolean sharpShop = true;
	/** The furthest wave reached in the Cyber Grind. */
	public static int grindBest = 0;
	/** ULTRAKILL's bosses come for V1 now and then (with a warning first). */
	public static boolean bosses = true;
	/** About how many minutes of play between bosses. */
	public static int bossMinutes = 20;
	/** How long the warning gives V1 to get ready. */
	public static int bossWarnSeconds = 30;
	/** Every weapon, variant and arm without buying them (the shop still takes P for custom colours). */
	public static boolean allGear = false;
	/** V1's weapons break blocks. */
	public static boolean playerBlockDamage = true;
	/** ULTRAKILL's enemies (their shots, beams, blasts and fire) break blocks. */
	public static boolean enemyBlockDamage = true;
	/** How long ULTRAKILL's impact frames (hitstop) last, 0.1x to 3x. */
	public static float impactFrames = 1f;
	/** Starting Minecraft starts ULTRAKILL too (through Steam), and closing Minecraft closes it. */
	public static boolean launchUltrakill = true;
	/** The OP Shop: Power goes on to 1500% and blast sizes to 1500% (more levels on the Upgrades page). */
	public static boolean opShop = false;
	/** Back as Steve, ULTRAKILL's enemies stay (drawn from Steve's camera, still fighting); off: they wait, frozen. */
	public static boolean steveEnemies = true;
	/**
	 * ULTRAKILL's frame rate cap (it shares the graphics card with Minecraft); 0 = match Minecraft: Minecraft draws
	 * its world from ULTRAKILL's frames, so the view is only as smooth as ULTRAKILL's frame rate.
	 */
	public static int ukFps = 0;
	/**
	 * ULTRAKILL's own settings as Ultracraft plays it (mouse sensitivity, field of view...): applied while it runs,
	 * never written into ULTRAKILL's own settings files. Key = ULTRAKILL's pref name.
	 */
	public static final Map<String, String> ukPrefs = new TreeMap<>();
	/** ULTRAKILL's controls rebound (UcKeybindsScreen): action -> GLFW key, or -1 - mouse button. */
	public static final Map<String, Integer> ukBinds = new TreeMap<>();

	private static final String[] KEYS = {"v1Height", "autoV1", "ukSpawns", "mcMobs", "sharpShop", "grindBest", "bosses", "bossMinutes", "bossWarnSeconds",
		"allGear", "playerBlockDamage", "enemyBlockDamage", "impactFrames", "launchUltrakill", "opShop", "ukFpsCap", "steveEnemies"};

	private static final String COMMENT = "Ultracraft (most of this is on the Ultracraft settings screen): v1Height = ULTRAKILL render height (0 = full window, lower = faster);"
		+ " autoV1 = become V1 automatically; ukSpawns = ULTRAKILL's enemies spawn in the dark; mcMobs = Minecraft's monsters spawn;"
		+ " sharpShop = full resolution at a shop's screen; grindBest = best Cyber Grind wave; bosses = ULTRAKILL's bosses come now and then;"
		+ " bossMinutes = minutes of play between them; bossWarnSeconds = warning before one arrives; allGear = every weapon without buying it;"
		+ " playerBlockDamage / enemyBlockDamage = V1's / enemies' attacks break blocks; impactFrames = hitstop length (0.1 to 3);"
		+ " launchUltrakill = start ULTRAKILL with Minecraft; opShop = upgrades go to 1500%; uk.* = ULTRAKILL settings used while playing Ultracraft";

	private UltracraftConfig() {}

	/**
	 * -Dultracraft.instance=N: a second (third...) Minecraft on the same computer, paired with an ULTRAKILL started
	 * with -ucinstance N (testing multiplayer alone): its own port and shared files.
	 */
	public static int instance() {
		try {
			return Math.max(1, Integer.parseInt(System.getProperty("ultracraft.instance", "1")));
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	public static String instanceSuffix() {
		return instance() > 1 ? "_" + instance() : "";
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("ultracraft.properties");
	}

	public static void load() {
		Path file = file();
		Properties p = new Properties();
		try {
			if (Files.exists(file)) {
				try (Reader r = Files.newBufferedReader(file)) {
					p.load(r);
				}
			}
			boolean complete = true;
			for (String key : KEYS) {
				if (!p.containsKey(key)) complete = false;
			}
			v1Height = Integer.parseInt(p.getProperty("v1Height", Integer.toString(v1Height)).trim());
			autoV1 = bool(p, "autoV1", autoV1);
			ukSpawns = bool(p, "ukSpawns", ukSpawns);
			mcMobs = bool(p, "mcMobs", mcMobs);
			sharpShop = bool(p, "sharpShop", sharpShop);
			grindBest = Integer.parseInt(p.getProperty("grindBest", Integer.toString(grindBest)).trim());
			bosses = bool(p, "bosses", bosses);
			bossMinutes = Integer.parseInt(p.getProperty("bossMinutes", Integer.toString(bossMinutes)).trim());
			bossWarnSeconds = Integer.parseInt(p.getProperty("bossWarnSeconds", Integer.toString(bossWarnSeconds)).trim());
			allGear = bool(p, "allGear", allGear);
			playerBlockDamage = bool(p, "playerBlockDamage", playerBlockDamage);
			enemyBlockDamage = bool(p, "enemyBlockDamage", enemyBlockDamage);
			impactFrames = Math.max(0.1f, Math.min(3f, Float.parseFloat(p.getProperty("impactFrames", Float.toString(impactFrames)).trim())));
			launchUltrakill = bool(p, "launchUltrakill", launchUltrakill);
			opShop = bool(p, "opShop", opShop);
			steveEnemies = bool(p, "steveEnemies", steveEnemies);
			// (ukFps was a plain cap, 120 by default: ukFpsCap starts again at "match Minecraft")
			ukFps = Integer.parseInt(p.getProperty("ukFpsCap", Integer.toString(ukFps)).trim());
			if (ukFps != 0) ukFps = Math.max(30, Math.min(240, ukFps));
			ukPrefs.clear();
			for (String k : p.stringPropertyNames()) {
				if (k.startsWith("uk.")) ukPrefs.put(k.substring(3), p.getProperty(k).trim());
				if (k.startsWith("bind.")) ukBinds.put(k.substring(5), Integer.parseInt(p.getProperty(k).trim()));
			}
			if (!complete) save();
		} catch (Exception e) {
			System.err.println("[Ultracraft] config: " + e);
		}
	}

	private static boolean bool(Properties p, String key, boolean def) {
		return Boolean.parseBoolean(p.getProperty(key, Boolean.toString(def)).trim());
	}

	/** Write the settings back (the settings screen, a shop's Sandbox page, a Cyber Grind run that went further). */
	public static void save() {
		Properties p = new Properties();
		p.setProperty("v1Height", Integer.toString(v1Height));
		p.setProperty("autoV1", Boolean.toString(autoV1));
		p.setProperty("ukSpawns", Boolean.toString(ukSpawns));
		p.setProperty("mcMobs", Boolean.toString(mcMobs));
		p.setProperty("sharpShop", Boolean.toString(sharpShop));
		p.setProperty("grindBest", Integer.toString(grindBest));
		p.setProperty("bosses", Boolean.toString(bosses));
		p.setProperty("bossMinutes", Integer.toString(bossMinutes));
		p.setProperty("bossWarnSeconds", Integer.toString(bossWarnSeconds));
		p.setProperty("allGear", Boolean.toString(allGear));
		p.setProperty("playerBlockDamage", Boolean.toString(playerBlockDamage));
		p.setProperty("enemyBlockDamage", Boolean.toString(enemyBlockDamage));
		p.setProperty("impactFrames", String.format(Locale.ROOT, "%.1f", impactFrames));
		p.setProperty("launchUltrakill", Boolean.toString(launchUltrakill));
		p.setProperty("opShop", Boolean.toString(opShop));
		p.setProperty("steveEnemies", Boolean.toString(steveEnemies));
		p.setProperty("ukFpsCap", Integer.toString(ukFps));
		for (var e : ukPrefs.entrySet()) p.setProperty("uk." + e.getKey(), e.getValue());
		for (var e : ukBinds.entrySet()) p.setProperty("bind." + e.getKey(), Integer.toString(e.getValue()));
		try {
			Path file = file();
			Files.createDirectories(file.getParent());
			try (Writer w = Files.newBufferedWriter(file)) {
				p.store(w, COMMENT);
			}
		} catch (Exception e) {
			System.err.println("[Ultracraft] config: " + e);
		}
	}

	/**
	 * ULTRAKILL's frame rate cap now: the set one, or Minecraft's own limit (its frame limit, or the monitor's
	 * refresh rate with VSync).
	 */
	public static int ukFpsNow() {
		if (ukFps > 0) return ukFps;
		var mc = net.minecraft.client.Minecraft.getInstance();
		int limit = 240;
		try {
			int cap = mc.options.framerateLimit().get();
			if (cap < 260) limit = cap;
			if (mc.options.enableVsync().get() && mc.getWindow().getRefreshRate() > 0) limit = Math.min(limit, mc.getWindow().getRefreshRate());
		} catch (RuntimeException ignored) {
		}
		// Minecraft waits for each of ULTRAKILL's frames (UkFrame.waitForNext), so this is the rate both run at
		return Math.max(30, Math.min(240, limit));
	}

	/** What ULTRAKILL needs to know of these: OPTS key=value ... (sent on connecting and after every change). */
	public static void sendOpts() {
		UkLink.send(String.format(Locale.ROOT, "OPTS impact=%.2f fps=%d playerBlocks=%d enemyBlocks=%d", impactFrames, ukFpsNow(), playerBlockDamage ? 1 : 0, enemyBlockDamage ? 1 : 0));
		for (var e : ukPrefs.entrySet()) UkLink.send("UKPREF " + e.getKey() + " " + e.getValue());
		for (var e : ukBinds.entrySet()) UkLink.send("UKBIND " + e.getKey() + " " + e.getValue());
	}
}
