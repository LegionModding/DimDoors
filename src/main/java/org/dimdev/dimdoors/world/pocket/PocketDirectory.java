package org.dimdev.dimdoors.world.pocket;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3i;
import org.dimdev.dimdoors.DimensionalDoorsInitializer;
import org.dimdev.dimdoors.util.math.GridUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.dimdev.dimdoors.world.pocket.type.AbstractPocket;
import org.dimdev.dimdoors.world.pocket.type.IdReferencePocket;
import org.dimdev.dimdoors.world.pocket.type.Pocket;

public class PocketDirectory {
	int gridSize; // Determines how much pockets in their dimension are spaced
	int privatePocketSize;
	int publicPocketSize;
	Map<Integer, AbstractPocket<?>> pockets;
	private SortedMap<Integer, Integer> nextIDMap;
	RegistryKey<World> worldKey;

	public PocketDirectory(RegistryKey<World> worldKey) {
		this.gridSize = DimensionalDoorsInitializer.getConfig().getPocketsConfig().pocketGridSize;
		this.worldKey = worldKey;
		this.nextIDMap = new TreeMap<>();
		this.pockets = new HashMap<>();
	}

	public static PocketDirectory readFromNbt(String id, CompoundTag tag) {
		PocketDirectory directory = new PocketDirectory(RegistryKey.of(Registry.DIMENSION, new Identifier(id)));
		// no need to parallelize
		directory.gridSize = tag.getInt("grid_size");
		directory.privatePocketSize = tag.getInt("private_pocket_size");
		directory.publicPocketSize = tag.getInt("public_pocket_size");
		// same thing, too short anyways
		CompoundTag nextIdMapTag = tag.getCompound("next_id_map");
		directory.nextIDMap.putAll(nextIdMapTag.getKeys().stream().collect(Collectors.toMap(Integer::parseInt, nextIdMapTag::getInt)));

		CompoundTag pocketsTag = tag.getCompound("pockets");
		directory.pockets = pocketsTag.getKeys().stream().unordered().map(key -> {
			CompoundTag pocketTag = pocketsTag.getCompound(key);
			return CompletableFuture.supplyAsync(() -> new Pair<>(Integer.parseInt(key), AbstractPocket.deserialize(pocketTag)));
		}).parallel().map(CompletableFuture::join).collect(Collectors.toConcurrentMap(Pair::getLeft, Pair::getRight));

		return directory;
	}

	public CompoundTag writeToNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("grid_size", this.gridSize);
		tag.putInt("private_pocket_size", this.privatePocketSize);
		tag.putInt("public_pocket_size", this.publicPocketSize);

		CompoundTag nextIdMapTag = new CompoundTag();
		this.nextIDMap.forEach((key, value) -> nextIdMapTag.putInt(key.toString(), value));
		tag.put("next_id_map", nextIdMapTag);

		CompoundTag pocketsTag = new CompoundTag();
		this.pockets.entrySet().parallelStream().unordered().map(entry -> CompletableFuture.supplyAsync(() -> new Pair<>(entry.getKey().toString(), entry.getValue().toTag(new CompoundTag()))))
				.map(CompletableFuture::join).forEach(pair -> pocketsTag.put(pair.getLeft(), pair.getRight()));
		tag.put("pockets", pocketsTag);

		return tag;
	}

	/**
	 * Create a new blank pocket.
	 *
	 * @return The newly created pockets
	 */
	public <T extends Pocket> T newPocket(Pocket.PocketBuilder<?, T> builder) {
		Vec3i size = builder.getExpectedSize();
		int longest = Math.max(size.getX(), size.getZ());
		longest = (longest / (gridSize * 16)) + 1;

		int base3Size = 1;
		while (longest > base3Size) {
			base3Size *= 3;
		}

		int squaredSize = base3Size * base3Size;

		int cursor = nextIDMap.headMap(base3Size+1).values().stream().mapToInt(num -> num).max().orElse(0);
		cursor = cursor - Math.floorMod(cursor, squaredSize);

		Pocket pocketAt = getPocket(cursor);
		while (pocketAt != null) {
			size = pocketAt.getSize();
			longest = Math.max(size.getX(), size.getZ());
			longest = (longest / (gridSize * 16)) + 1;

			int pocketBase3Size = 1;
			while (longest > pocketBase3Size) {
				pocketBase3Size *= 3;
			}

			cursor += Math.max(squaredSize, pocketBase3Size * pocketBase3Size);
			pocketAt = getPocket(cursor);
		}

		cursor = cursor + squaredSize - 1; // we actually want to use the last id of
		// the assigned grid space since it is in the bottom left corner

		T pocket = builder
				.id(cursor)
				.world(worldKey)
				.range(squaredSize)
				.offsetOrigin(idToCenteredPos(cursor, base3Size, builder.getExpectedSize()))
				.build();

		nextIDMap.put(base3Size, cursor + squaredSize);
		addPocket(pocket);

		IdReferencePocket.IdReferencePocketBuilder idReferenceBuilder = IdReferencePocket.builder();
		for (int i = 1; i < squaredSize; i++) {
			addPocket(idReferenceBuilder
					.id(cursor - i)
					.world(worldKey)
					.referencedId(cursor)
					.build());
		}
		return pocket;
	}

	private void addPocket(AbstractPocket<?> pocket) {
		pockets.put(pocket.getId(), pocket);
	}

	// TODO: rework this method to remove references as well
	public void removePocket(int id) {
		this.pockets.remove(id);
	}

	/**
	 * Gets the pocket that occupies the GridPos which a certain ID represents, or null if there is no pocket at that GridPos.
	 *
	 * @return The pocket which occupies the GridPos represented by that ID, or null if there was no pocket occupying that GridPos.
	 */
	public Pocket getPocket(int id) {
		AbstractPocket<?> pocket = this.pockets.get(id);
		return pocket == null ? null : pocket.getReferencedPocket();
	}

	public <P extends Pocket> P getPocket(int id, Class<P> clazz) {
		Pocket pocket = getPocket(id);
		if (clazz.isInstance(pocket)) return clazz.cast(pocket);
		return null;
	}

	public GridUtil.GridPos idToGridPos(int id) {
		return GridUtil.idToGridPos(id);
	}

	public int gridPosToID(GridUtil.GridPos pos) {
		return GridUtil.gridPosToID(pos);
	}

	/**
	 * Calculates the default BlockPos where a pocket should be based on the ID. Use this only for placing
	 * pockets, and use Pocket.getGridPos() for getting the position
	 *
	 * @param id The ID of the pocket
	 * @return The BlockPos of the pocket
	 */
	public BlockPos idToPos(int id) {
		GridUtil.GridPos pos = this.idToGridPos(id);
		return new BlockPos(pos.x * this.gridSize * 16, 0, pos.z * this.gridSize * 16);
	}

	public BlockPos idToCenteredPos(int id, int base3Size, Vec3i expectedSize) {
		GridUtil.GridPos pos = this.idToGridPos(id);
		// you actually need the "/ 2 * 16" here. "*8" would not work the same since it doesn't guarantee chunk alignment
		return new BlockPos((pos.x * this.gridSize * 16) + (base3Size * this.gridSize - expectedSize.getX() / 16) / 2 * 16, 0, (pos.z * this.gridSize * 16) + (base3Size * this.gridSize - expectedSize.getZ() / 16) / 2 * 16);
	}

	/**
	 * Calculates the ID of a pocket at a certain BlockPos.
	 *
	 * @param pos The position
	 * @return The ID of the pocket, or -1 if there is no pocket at that location
	 */
	public int posToID(BlockPos pos) {
		return this.gridPosToID(new GridUtil.GridPos(Math.floorDiv(pos.getX(), this.gridSize * 16), Math.floorDiv(pos.getZ(), this.gridSize * 16)));
	}

	public Pocket getPocketAt(BlockPos pos) { // TODO: use BlockPos
		return this.getPocket(this.posToID(pos));
	}

	public boolean isWithinPocketBounds(BlockPos pos) {
		Pocket pocket = this.getPocketAt(pos);
		return pocket != null && pocket.isInBounds(pos);
	}

	public int getGridSize() {
		return this.gridSize;
	}

	public int getPrivatePocketSize() {
		return this.privatePocketSize;
	}

	public int getPublicPocketSize() {
		return this.publicPocketSize;
	}

	public Map<Integer, AbstractPocket<?>> getPockets() {
		return this.pockets;
	}
}


