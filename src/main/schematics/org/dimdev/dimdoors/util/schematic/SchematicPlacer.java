package org.dimdev.dimdoors.util.schematic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

import net.fabricmc.loader.api.FabricLoader;
import org.dimdev.dimdoors.block.entity.RiftBlockEntity;
import org.dimdev.dimdoors.util.BlockPlacementType;

public final class SchematicPlacer {
	public static final Logger LOGGER = LogManager.getLogger();

	private SchematicPlacer() {
	}

	public static void place(Schematic schematic, StructureWorldAccess world, BlockPos origin, BlockPlacementType placementType) {
		LOGGER.debug("Placing schematic: {}", schematic.getMetadata().getName());
		for (String id : schematic.getMetadata().getRequiredMods()) {
			if (!FabricLoader.getInstance().isModLoaded(id)) {
				LOGGER.warn("Schematic \"" + schematic.getMetadata().getName() + "\" depends on mod \"" + id + "\", which is missing!");
			}
		}
		RelativeBlockSample blockSample = Schematic.getBlockSample(schematic);
		blockSample.place(origin, world, placementType, false);
	}

	public static Map<BlockPos, RiftBlockEntity> getAbsoluteRifts(Schematic schematic, BlockPos origin) {
		LOGGER.debug("Placing schematic: {}", schematic.getMetadata().getName());
		for (String id : schematic.getMetadata().getRequiredMods()) {
			if (!FabricLoader.getInstance().isModLoaded(id)) {
				LOGGER.warn("Schematic \"" + schematic.getMetadata().getName() + "\" depends on mod \"" + id + "\", which is missing!");
			}
		}
		RelativeBlockSample blockSample = Schematic.getBlockSample(schematic);
		return blockSample.getAbsoluteRifts(origin);
	}

	public static void place(Schematic schematic, ServerWorld world, Chunk chunk, BlockPos origin, BlockPlacementType placementType) {
		LOGGER.debug("Placing schematic: {}", schematic.getMetadata().getName());
		for (String id : schematic.getMetadata().getRequiredMods()) {
			if (!FabricLoader.getInstance().isModLoaded(id)) {
				LOGGER.warn("Schematic \"" + schematic.getMetadata().getName() + "\" depends on mod \"" + id + "\", which is missing!");
			}
		}
		RelativeBlockSample blockSample = Schematic.getBlockSample(schematic);
		blockSample.place(origin, world, chunk, placementType, false);
	}



	public static int[][][] getBlockData(Schematic schematic) {
		int width = schematic.getWidth();
		int height = schematic.getHeight();
		int length = schematic.getLength();
		byte[] blockDataIntArray = schematic.getBlockData().array();
		int[][][] blockData = new int[width][height][length];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				for (int z = 0; z < length; z++) {
					blockData[x][y][z] = blockDataIntArray[x + z * width + y * width * length];
				}
			}
		}
		return blockData;
	}

	public static int[][] getBiomeData(Schematic schematic) {
		int width = schematic.getWidth();
		int length = schematic.getLength();
		byte[] biomeDataArray = schematic.getBiomeData().array();
		if (biomeDataArray.length == 0) return new int[0][0];
		int[][] biomeData = new int[width][length];
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < length; z++) {
				biomeData[x][z] = biomeDataArray[x + z * width];
			}
		}
		return biomeData;
	}

	private static void placeEntities(int originX, int originY, int originZ, Schematic schematic, StructureWorldAccess world) {
		List<CompoundTag> entityTags = schematic.getEntities();
		for (CompoundTag tag : entityTags) {
			ListTag listTag = Objects.requireNonNull(tag.getList("Pos", 6), "Entity in schematic  \"" + schematic.getMetadata().getName() + "\" did not have a Pos tag!");
			SchematicPlacer.processPos(listTag, originX, originY, originZ, tag);

			EntityType<?> entityType = EntityType.fromTag(tag).orElseThrow(AssertionError::new);
			Entity e = entityType.create(world.toServerWorld());
			// TODO: fail with an exception
			if (e != null) {
				e.fromTag(tag);
				world.spawnEntityAndPassengers(e);
			}
		}
	}

	public static CompoundTag fixEntityId(CompoundTag tag) {
		if (!tag.contains("Id") && tag.contains("id")) {
			tag.putString("Id", tag.getString("id"));
		} else if (tag.contains("Id") && !tag.contains("id")) {
			tag.putString("id", tag.getString("Id"));
		}
		if (!tag.contains("Id") || !tag.contains("id")) {
			System.err.println("An unexpected error occurred parsing this entity");
			System.err.println(tag.toString());
			throw new IllegalStateException("Entity did not have an 'Id' tag, nor an 'id' tag!");
		}
		return tag;
	}

	private static void processPos(ListTag listTag, int originX, int originY, int originZ, CompoundTag tag) {
		double x = listTag.getDouble(0);
		double y = listTag.getDouble(1);
		double z = listTag.getDouble(2);
		tag.remove("Pos");
		tag.put("Pos", NbtOps.INSTANCE.createList(Stream.of(DoubleTag.of(x + originX),
				DoubleTag.of(y + originY),
				DoubleTag.of(z + originZ))));
	}
}
