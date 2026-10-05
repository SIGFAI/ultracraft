package dev.ultracraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Cyber Grind, started from a shop's screen: instead of ULTRAKILL's own arena, waves of ULTRAKILL's enemies come
 * for V1 around the shop it was started from, each wave bigger and nastier than the last. A wave is cleared when its
 * last enemy dies; the next comes a few seconds later. The run ends when V1 dies, leaves (48 blocks), turns back into
 * Steve, or presses Leave on the screen. The best wave is kept. Runs on the server thread (ULTRAKILL's messages come
 * in on the client thread and are handed over).
 */
final class CyberGrind {
	/** An enemy, what it costs out of a wave's budget, the first wave it can come in, whether it flies. */
	private record Kind(String type, int cost, int from, boolean flies) {}

	private static final List<Kind> KINDS = List.of(
		new Kind("Filth", 1, 1, false), new Kind("Stray", 2, 1, false), new Kind("Drone", 2, 2, true), new Kind("Schism", 3, 3, false),
		new Kind("Soldier", 3, 4, false), new Kind("Streetcleaner", 5, 5, false), new Kind("Mannequin", 4, 6, false), new Kind("Virtue", 6, 7, true),
		new Kind("Cerberus", 7, 8, false), new Kind("Swordsmachine", 8, 9, false), new Kind("MaliciousFace", 8, 10, true), new Kind("Stalker", 6, 12, false),
		new Kind("Mindflayer", 12, 14, false), new Kind("Gutterman", 12, 16, false), new Kind("Idol", 10, 18, false), new Kind("Power", 18, 22, false));

	private static final double LEAVE = 48.0;

	static volatile boolean running;
	static volatile int wave;
	private static Vec3 center = Vec3.ZERO;
	private static long shopId;
	private static int left;
	/** Ticks until the next wave comes (after one is cleared), or -1 while a wave is on. */
	private static int countdown = -1;
	/** Ticks since the wave started (a wave whose enemies never arrived, or wandered off, is called after a while). */
	private static int waveTicks;
	/** The player who started it (their ULTRAKILL runs the waves; the others see them and can join in). */
	private static java.util.UUID runner;
	private static net.minecraft.server.MinecraftServer server;

	private CyberGrind() {}

	static boolean isRunner(ServerPlayer sp) {
		return running && runner != null && runner.equals(sp.getUUID());
	}

	static void stopIfRunner(ServerPlayer sp, String why) {
		if (isRunner(sp)) stop(sp, why);
	}

	/** The world closed. */
	static void reset() {
		running = false;
		runner = null;
		server = null;
	}

	private static ServerPlayer runnerPlayer() {
		return server == null || runner == null ? null : server.getPlayerList().getPlayer(runner);
	}

	/** GRIND id from a shop's screen: start a run around that shop, or end the one going on. */
	static void toggle(ServerPlayer sp, long id) {
		if (running) {
			stop(sp, "left");
			return;
		}
		if (UkBosses.busy()) {
			sp.displayClientMessage(Component.literal("Not now: a boss is coming for you."), true);
			return;
		}
		BlockPos anchor = BlockPos.of(id);
		var state = sp.level().getBlockState(anchor);
		if (!state.is(UltracraftCommon.UK_SHOP)) return;
		Direction f = state.getValue(UkShopBlock.FACING), right = UkShopBlock.right(f);
		// the arena: the open ground in front of the screen
		center = new Vec3(anchor.getX() + 0.5 + 0.5 * right.getStepX() + f.getStepX() * 6, anchor.getY(), anchor.getZ() + 0.5 + 0.5 * right.getStepZ() + f.getStepZ() * 6);
		shopId = id;
		runner = sp.getUUID();
		server = sp.level().getServer();
		running = true;
		wave = 0;
		left = 0;
		countdown = 60;
		hud("THE CYBER GRIND");
		sp.displayClientMessage(Component.literal("The Cyber Grind: waves of enemies around this shop. Dying or leaving ends it."), true);
		sendState();
	}

	/** Every 10 ticks while V1 is active. */
	static void tick(ServerPlayer sp) {
		if (!running) return;
		if (sp.position().distanceTo(center) > LEAVE || !sp.level().getBlockState(BlockPos.of(shopId)).is(UltracraftCommon.UK_SHOP)) {
			stop(sp, "left");
			return;
		}
		if (countdown >= 0) {
			countdown -= 10;
			if (countdown < 0) startWave(sp);
			return;
		}
		waveTicks += 10;
		// stragglers stuck somewhere they can't reach V1: after two minutes the wave is called
		if (left <= 0 || waveTicks > 2400) cleared(sp);
	}

	private static void startWave(ServerPlayer sp) {
		wave++;
		waveTicks = 0;
		ServerLevel level = sp.level();
		RandomSource random = level.getRandom();
		// the budget grows with every wave; the biggest enemies come in later
		int budget = 4 + wave * 3 + wave * wave / 4;
		List<Kind> spawn = new ArrayList<>();
		int guard = 0;
		while (budget > 0 && spawn.size() < 4 + wave * 2 && guard++ < 200) {
			List<Kind> can = new ArrayList<>();
			for (Kind k : KINDS) if (k.from <= wave && k.cost <= budget) can.add(k);
			if (can.isEmpty()) break;
			// the newest enemies are the likeliest
			Kind k = can.get(Math.max(0, can.size() - 1 - (int) Math.floor(Math.abs(random.nextGaussian()) * can.size() / 2.0)));
			spawn.add(k);
			budget -= k.cost;
		}
		left = 0;
		for (Kind k : spawn) {
			Vec3 at = spot(level, random);
			double y = at.y + (k.flies ? 3.0 + random.nextInt(3) : 0.0);
			UcNet.send(sp, String.format(Locale.ROOT, "SPAWNAT %s %.2f %.2f %.2f 2", k.type, at.x, y, at.z));
			left++;
		}
		hud("WAVE " + wave);
		sendState();
	}

	/** Somewhere around the arena an enemy can stand: 6 to 14 blocks from its middle, on the ground. */
	private static Vec3 spot(ServerLevel level, RandomSource random) {
		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0, dist = 6.0 + random.nextDouble() * 8.0;
			int x = (int) Math.floor(center.x + Math.cos(angle) * dist), z = (int) Math.floor(center.z + Math.sin(angle) * dist);
			BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, (int) Math.floor(center.y) + 6, z);
			for (int y = m.getY(); y > center.y - 10; y--) {
				m.setY(y);
				if (!level.getBlockState(m).isFaceSturdy(level, m, Direction.UP)) continue;
				BlockPos feet = m.above();
				if (level.noCollision(new AABB(feet.getX() + 0.1, feet.getY(), feet.getZ() + 0.1, feet.getX() + 0.9, feet.getY() + 3.0, feet.getZ() + 0.9))
					&& level.getFluidState(feet).isEmpty()) {
					return new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
				}
				break;
			}
		}
		return center;
	}

	/** UKDEAD ... 1: one of the wave's enemies died. */
	static void died(ServerPlayer sp) {
		if (!isRunner(sp) || countdown >= 0) return;
		left--;
		if (left <= 0) cleared(sp);
	}

	private static void cleared(ServerPlayer sp) {
		if (countdown >= 0) return;
		// a cleared wave pays out, more the further in: experience, and P on top of the style it took
		ExperienceOrb.award(sp.level(), sp.position(), 5 + wave * 3);
		int prize = 250 * wave;
		// everyone fighting it is paid
		for (ServerPlayer o : sp.level().getServer().getPlayerList().getPlayers()) {
			if (o == sp || (UcNet.isV1(o) && o.level() == sp.level() && o.position().distanceTo(center) < LEAVE)) UkProgress.get(o).award(prize);
		}
		if (wave > UltracraftConfig.grindBest) {
			UltracraftConfig.grindBest = wave;
			UltracraftConfig.save();
		}
		UcNet.send(sp, "GRINDCLEAR");
		countdown = 100;
		hud(String.format(Locale.ROOT, "WAVE %d CLEARED  +%,d <color=#FF4343>P</color>", wave, prize));
		sendState();
	}

	/** The run is over: V1 died, left, became Steve, or pressed Leave. */
	static void stop(ServerPlayer sp, String why) {
		if (!running) return;
		ServerPlayer owner = sp != null ? sp : runnerPlayer();
		running = false;
		int reached = Math.max(0, countdown >= 0 ? wave : wave - 1);
		if (reached > UltracraftConfig.grindBest) {
			UltracraftConfig.grindBest = reached;
			UltracraftConfig.save();
		}
		if (owner != null) UcNet.send(owner, "GRINDCLEAR");
		hud("THE CYBER GRIND IS OVER: WAVE " + wave);
		if (sp != null) {
			sp.displayClientMessage(Component.literal(String.format(Locale.ROOT, "The Cyber Grind is over (%s): wave %d, best %d", why, wave, UltracraftConfig.grindBest)), false);
		}
		sendState();
		runner = null;
	}

	/** Its banner for everyone. */
	private static void hud(String text) {
		if (server != null) UcNet.sendAll(server, "GRINDHUD " + text);
	}

	private static String stateLine() {
		return "GRINDSTATE " + (running ? 1 : 0) + " " + wave + " " + UltracraftConfig.grindBest + " " + (UltracraftConfig.ukSpawns ? 1 : 0);
	}

	/** GRINDSTATE running wave best spawns, for every shop screen. */
	static void sendState() {
		if (server != null) UcNet.sendAll(server, stateLine());
	}

	static void sendState(ServerPlayer sp) {
		UcNet.send(sp, stateLine());
	}

	static void sendStateAll(net.minecraft.server.MinecraftServer s) {
		UcNet.sendAll(s, stateLine());
	}
}
