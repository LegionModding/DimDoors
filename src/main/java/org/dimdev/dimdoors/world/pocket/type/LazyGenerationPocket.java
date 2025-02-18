package org.dimdev.dimdoors.world.pocket.type;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.dimdev.dimdoors.pockets.generator.LazyPocketGenerator;
import org.dimdev.dimdoors.pockets.generator.PocketGenerator;
import org.dimdev.dimdoors.world.level.ChunkLazilyGenerated;

import java.util.Map;

public class LazyGenerationPocket extends Pocket {
	public static String KEY = "lazy_gen_pocket";

	private LazyPocketGenerator generator;
	private int toBeGennedChunkCount = 0;

	public void chunkLoaded(Chunk chunk) {
		if (isDoneGenerating()) return;

		ChunkLazilyGenerated lazyGenned = ChunkLazilyGenerated.get(chunk);
		if (lazyGenned.hasBeenLazyGenned()) return;

		ChunkPos pos = chunk.getPos();
		BlockBox chunkBox = BlockBox.create(pos.getStartX(), 0, pos.getStartZ(), pos.getEndX(), chunk.getHeight() - 1, pos.getEndZ());
		if (!chunkBox.intersects(getBox())) return;

		generator.generateChunk(this, chunk);
		lazyGenned.setGenned();
		toBeGennedChunkCount--;

		if (isDoneGenerating()) {
			this.generator = null; // saving up on some ram
		}
	}

	public boolean isDoneGenerating() {
		return toBeGennedChunkCount == 0;
	}

	public void attachGenerator(LazyPocketGenerator generator) {
		this.generator = generator;
	}

	public void init() {
		BlockBox box = getBox();

		toBeGennedChunkCount = (Math.floorDiv(box.maxX, 16) - Math.floorDiv(box.minX, 16) + 1) * (Math.floorDiv(box.maxZ, 16) - Math.floorDiv(box.minZ, 16) + 1);
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		super.toTag(tag);

		if (generator != null) tag.put("generator", generator.toTag(new CompoundTag()));
		if (toBeGennedChunkCount > 0) tag.putInt("to_be_genned_chunks", toBeGennedChunkCount);

		return tag;
	}

	@Override
	public AbstractPocketType<?> getType() {
		return AbstractPocketType.LAZY_GENERATION_POCKET;
	}

	public static String getKEY() {
		return KEY;
	}

	@Override
	public Pocket fromTag(CompoundTag tag) {
		super.fromTag(tag);

		if (tag.contains("generator", NbtType.COMPOUND)) generator = (LazyPocketGenerator) PocketGenerator.deserialize(tag.getCompound("generator"));
		if (tag.contains("to_be_genned_chunks", NbtType.INT)) toBeGennedChunkCount = tag.getInt("to_be_genned_chunks");

		return this;
	}

	@Override
	public Map<BlockPos, BlockEntity> getBlockEntities() {

		return super.getBlockEntities();
	}

	public static LazyGenerationPocketBuilder<?, LazyGenerationPocket> builderLazyGenerationPocket() {
		return new LazyGenerationPocketBuilder<>(AbstractPocketType.LAZY_GENERATION_POCKET);
	}

	public static class LazyGenerationPocketBuilder<P extends LazyGenerationPocketBuilder<P, T>, T extends LazyGenerationPocket> extends PocketBuilder<P, T> {
		protected LazyGenerationPocketBuilder(AbstractPocketType<T> type) {
			super(type);
		}
	}
}
