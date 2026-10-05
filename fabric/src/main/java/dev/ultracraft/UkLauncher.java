package dev.ultracraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minecraft started from the normal launcher brings ULTRAKILL along: as soon as Minecraft is up, ULTRAKILL starts
 * through Steam with -ultracraft (its window hidden, the Sandbox loading in the background, waiting for a world), and
 * closing Minecraft closes the ULTRAKILL it started. An ULTRAKILL already running is used as it is. The "Start
 * ULTRAKILL" setting (or -Dultracraft.noLaunch, which the test launcher passes) turns this off.
 */
final class UkLauncher {
	private static final Logger LOG = LoggerFactory.getLogger("ultracraft");
	private static boolean started;

	private UkLauncher() {}

	static void register() {
		ClientLifecycleEvents.CLIENT_STARTED.register(mc -> launch());
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> close());
	}

	static Optional<ProcessHandle> running() {
		return ProcessHandle.allProcesses().filter(p -> p.info().command().map(c -> c.toLowerCase().endsWith("\\ultrakill.exe")).orElse(false)).findFirst();
	}

	private static void launch() {
		if (!UltracraftConfig.launchUltrakill || System.getProperty("ultracraft.noLaunch") != null) return;
		if (running().isPresent() || UkLink.connected) {
			LOG.info("ULTRAKILL is already running");
			return;
		}
		String steam = steamExe();
		if (steam == null) {
			LOG.warn("Steam not found: start ULTRAKILL yourself (with the UltraBridge plugin)");
			return;
		}
		try {
			new ProcessBuilder(steam, "-applaunch", "1229490", "-ultracraft", "-screen-fullscreen", "0", "-screen-width", "1280", "-screen-height", "720").start();
			started = true;
			LOG.info("starting ULTRAKILL through {}", steam);
		} catch (Exception e) {
			LOG.warn("couldn't start ULTRAKILL: {}", e.toString());
		}
	}

	private static void close() {
		if (!started) return;
		// ULTRAKILL quits itself when asked; if it doesn't answer, it's closed
		if (UkLink.connected) UkLink.send("QUIT");
		running().ifPresent(p -> {
			try {
				p.onExit().get(6, TimeUnit.SECONDS);
			} catch (Exception e) {
				LOG.info("closing ULTRAKILL");
				p.destroy();
			}
		});
	}

	/** Steam's own record of where it is (HKCU\Software\Valve\Steam SteamExe), or the usual place. */
	private static String steamExe() {
		try {
			Process p = new ProcessBuilder("reg", "query", "HKCU\\Software\\Valve\\Steam", "/v", "SteamExe").redirectErrorStream(true).start();
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = r.readLine()) != null) {
					int i = line.indexOf("REG_SZ");
					if (line.contains("SteamExe") && i >= 0) {
						String path = line.substring(i + 6).trim().replace('/', '\\');
						if (new File(path).isFile()) return path;
					}
				}
			}
		} catch (Exception ignored) {
		}
		String fallback = "C:\\Program Files (x86)\\Steam\\steam.exe";
		return new File(fallback).isFile() ? fallback : null;
	}
}
