package dev.ultracraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * ULTRAKILL's enemies turning up in Minecraft's world the way its monsters do: in the dark (Minecraft's own light
 * rule), never on Peaceful, only with mob spawning on, a few at a time by difficulty, somewhere around V1 out of
 * arm's reach. Which ones depends on where: husks and machines in the Overworld (each biome with its favourites),
 * demons in the Nether, angels in the End (never the bosses: they come only as bosses). Killed, they drop experience like Minecraft's monsters, more the higher
 * V1's style rank, and sometimes a bit of what they're made of. Runs on the server thread.
 */
final class UkSpawns {
	private record Kind(String type, int weight, boolean flies, int xp) {}

	// ULTRAKILL's enemy types (EnemyType names), how often, whether they spawn in the air, their experience
	private static final List<Kind> OVERWORLD = List.of(
		new Kind("Filth", 30, false, 3), new Kind("Stray", 20, false, 5), new Kind("Schism", 12, false, 7),
		new Kind("Soldier", 10, false, 7), new Kind("Drone", 10, true, 5), new Kind("Mannequin", 4, false, 10),
		new Kind("Streetcleaner", 4, false, 12),
		new Kind("MaliciousFace", 2, true, 25), new Kind("Stalker", 1, false, 20), new Kind("Gutterman", 1, false, 30),
		new Kind("Virtue", 1, true, 25));
	private static final List<Kind> NETHER = List.of(
		new Kind("Filth", 14, false, 3), new Kind("Stray", 12, false, 5), new Kind("MaliciousFace", 10, true, 25),
		new Kind("Streetcleaner", 8, false, 12), new Kind("Soldier", 6, false, 7),
		new Kind("Idol", 2, false, 15),
		new Kind("Gutterman", 2, false, 30));
	private static final List<Kind> END = List.of(
		new Kind("Virtue", 10, true, 25), new Kind("Drone", 10, true, 5),
		new Kind("Idol", 2, false, 15), new Kind("Power", 1, false, 50));

	private static int ticks;

	private UkSpawns() {}

	/** Every couple of seconds while V1 is active. */
	static void tick(ServerPlayer sp) {
		if (!UltracraftConfig.ukSpawns) return;
		ServerLevel level = sp.level();
		if (level.getDifficulty() == Difficulty.PEACEFUL) return;
		if (!level.getGameRules().get(GameRules.SPAWN_MOBS) || !level.getGameRules().get(GameRules.SPAWN_MONSTERS)) return;
		int cap = switch (level.getDifficulty()) {
			case EASY -> 3;
			case HARD -> 7;
			default -> 5;
		};
		int near = 0;
		for (UkEnemyEntity e : UltracraftCommon.allUkEnemies()) if (!e.isRemoved() && e.level() == level && e.distanceToSqr(sp) < 64 * 64) near++;
		if (near >= cap) return;
		RandomSource random = level.getRandom();
		// about one try in three: a new enemy every few seconds while there's room, like a dark area filling up
		if (random.nextInt(3) != 0) return;
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double dist = 20.0 + random.nextDouble() * 16.0;
			int x = (int) Math.floor(sp.getX() + Math.cos(angle) * dist);
			int z = (int) Math.floor(sp.getZ() + Math.sin(angle) * dist);
			BlockPos ground = findGround(level, x, (int) Math.floor(sp.getY()) + 8, z);
			if (ground == null) continue;
			Kind kind = pick(level, ground, random);
			if (kind == null) continue;
			BlockPos feet = ground.above();
			if (!Monster.isDarkEnoughToSpawn(level, feet, random)) continue;
			// a body's room: ULTRAKILL's bigger enemies are three blocks tall
			if (!level.noCollision(new AABB(feet.getX() + 0.1, feet.getY(), feet.getZ() + 0.1, feet.getX() + 0.9, feet.getY() + 3.0, feet.getZ() + 0.9))) continue;
			if (!level.getFluidState(feet).isEmpty()) continue;
			double y = feet.getY() + (kind.flies ? 3.0 + random.nextInt(3) : 0.0);
			UcNet.send(sp, String.format(Locale.ROOT, "SPAWNAT %s %.2f %.2f %.2f 1", kind.type, feet.getX() + 0.5, y, feet.getZ() + 0.5));
			return;
		}
	}

	/** The highest standable block at or below fromY (16 blocks down at most): solid on top, two of air above. */
	private static BlockPos findGround(ServerLevel level, int x, int fromY, int z) {
		if (!level.hasChunkAt(new BlockPos(x, fromY, z))) return null;
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, fromY, z);
		for (int y = fromY; y > fromY - 16 && y > level.getMinY(); y--) {
			m.setY(y);
			if (!level.getBlockState(m).isFaceSturdy(level, m, Direction.UP)) continue;
			if (level.getBlockState(m.above()).isAir() && level.getBlockState(m.above(2)).isAir()) return m.immutable();
		}
		return null;
	}

	private static Kind pick(ServerLevel level, BlockPos pos, RandomSource random) {
		List<Kind> table;
		Holder<Biome> biome = level.getBiome(pos);
		if (level.dimension() == Level.NETHER) table = NETHER;
		else if (level.dimension() == Level.END) table = END;
		else {
			// no monsters where Minecraft has none
			if (biome.is(Biomes.MUSHROOM_FIELDS) || biome.is(Biomes.DEEP_DARK)) return null;
			table = new ArrayList<>(OVERWORLD);
			// each kind of place has its regulars
			if (biome.is(BiomeTags.IS_BADLANDS) || biome.is(Biomes.DESERT)) favour(table, "Streetcleaner", "Soldier", "MaliciousFace");
			else if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL)) favour(table, "Drone", "Virtue", "MaliciousFace");
			else if (biome.is(Biomes.DARK_FOREST) || biome.is(Biomes.PALE_GARDEN)) favour(table, "Mannequin", "Stalker", "Schism");
			else if (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP)) favour(table, "Filth", "Stray", "Mannequin");
			else if (biome.is(BiomeTags.IS_TAIGA) || biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.ICE_SPIKES)) favour(table, "Stray", "Schism", "Streetcleaner");
			else if (biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) favour(table, "Filth", "Drone", "Gutterman");
			// down in the caves, more machines
			if (pos.getY() < 40) favour(table, "Soldier", "Stalker", "Gutterman");
		}
		int total = 0;
		for (Kind k : table) total += k.weight;
		int r = random.nextInt(total);
		for (Kind k : table) {
			r -= k.weight;
			if (r < 0) return k;
		}
		return table.get(0);
	}

	private static void favour(List<Kind> table, String... types) {
		for (int i = 0; i < table.size(); i++) {
			Kind k = table.get(i);
			for (String t : types) if (k.type.equals(t)) table.set(i, new Kind(k.type, k.weight * 3 + 4, k.flies, k.xp));
		}
	}

	/** UKDEAD type x y z rank: one of ULTRAKILL's enemies died; its experience (and a little loot) drops there. */
	static void died(ServerLevel level, String type, Vec3 at, int rank) {
		int xp = 5;
		for (List<Kind> table : List.of(OVERWORLD, NETHER, END)) for (Kind k : table) if (k.type.equalsIgnoreCase(type)) xp = k.xp;
		// style pays: D is plain, every rank up adds a quarter (ULTRAKILL rank: double)
		xp = Math.round(xp * (1f + 0.25f * Math.max(0, Math.min(rank, 7))));
		ExperienceOrb.award(level, at, xp);
		RandomSource random = level.getRandom();
		if (random.nextInt(3) != 0) return;
		ItemStack drop = switch (type) {
			// husks: what's left of the damned
			case "Filth", "Stray", "Schism", "Soldier", "Stalker" -> new ItemStack(random.nextBoolean() ? Items.ROTTEN_FLESH : Items.BONE, 1 + random.nextInt(2));
			// machines run on blood, wires and steel
			case "Drone", "Streetcleaner", "Swordsmachine", "Mindflayer", "Gutterman", "Guttertank", "Turret", "V2" ->
				new ItemStack(random.nextBoolean() ? Items.IRON_NUGGET : Items.REDSTONE, 2 + random.nextInt(4));
			// demons
			case "MaliciousFace", "Cerberus", "Idol" -> new ItemStack(random.nextBoolean() ? Items.COBBLESTONE : Items.BLAZE_POWDER, 1 + random.nextInt(2));
			case "HideousMass", "Mannequin" -> new ItemStack(Items.SLIME_BALL, 1 + random.nextInt(3));
			// angels
			case "Virtue", "Power" -> new ItemStack(random.nextBoolean() ? Items.GLOWSTONE_DUST : Items.GOLD_NUGGET, 2 + random.nextInt(4));
			default -> ItemStack.EMPTY;
		};
		if (!drop.isEmpty()) level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z, drop));
	}
}
