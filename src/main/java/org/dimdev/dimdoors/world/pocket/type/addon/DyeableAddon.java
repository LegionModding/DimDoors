package org.dimdev.dimdoors.world.pocket.type.addon;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.dimdev.dimdoors.DimensionalDoorsInitializer;
import org.dimdev.dimdoors.block.AncientFabricBlock;
import org.dimdev.dimdoors.block.FabricBlock;
import org.dimdev.dimdoors.block.ModBlocks;
import org.dimdev.dimdoors.util.EntityUtils;
import org.dimdev.dimdoors.world.pocket.type.Pocket;
import org.dimdev.dimdoors.world.pocket.type.PocketColor;
import org.dimdev.dimdoors.world.pocket.type.PrivatePocket;

public class DyeableAddon implements PocketAddon {
	public static Identifier ID = new Identifier("dimdoors", "dyeable");

	private static final int BLOCKS_PAINTED_PER_DYE = 1000000;

	protected PocketColor dyeColor = PocketColor.WHITE;
	private PocketColor nextDyeColor = PocketColor.NONE;
	private int count = 0;

	private static int amountOfDyeRequiredToColor(Pocket pocket) {
		int outerVolume = pocket.box.getBlockCountX() * pocket.box.getBlockCountY() * pocket.box.getBlockCountZ();
		int innerVolume = (pocket.box.getBlockCountX() - 5) * (pocket.box.getBlockCountY() - 5) * (pocket.box.getBlockCountZ() - 5);

		return Math.max((outerVolume - innerVolume) / BLOCKS_PAINTED_PER_DYE, 1);
	}

	private void repaint(Pocket pocket, DyeColor dyeColor) {
		ServerWorld serverWorld = DimensionalDoorsInitializer.getWorld(pocket.getWorld());
		BlockState innerWall = ModBlocks.fabricFromDye(dyeColor).getDefaultState();
		BlockState outerWall = ModBlocks.ancientFabricFromDye(dyeColor).getDefaultState();

		BlockPos.stream(pocket.getBox()).forEach(pos -> {
			if (serverWorld.getBlockState(pos).getBlock() instanceof AncientFabricBlock) {
				serverWorld.setBlockState(pos, outerWall);
			} else if (serverWorld.getBlockState(pos).getBlock() instanceof FabricBlock) {
				serverWorld.setBlockState(pos, innerWall);
			}
		});
	}

	public boolean addDye(Pocket pocket, Entity entity, DyeColor dyeColor) {
		PocketColor color = PocketColor.from(dyeColor);

		int maxDye = amountOfDyeRequiredToColor(pocket);

		if (this.dyeColor == color) {
			EntityUtils.chat(entity, new TranslatableText("dimdoors.pockets.dyeAlreadyAbsorbed"));
			return false;
		}

		if (this.nextDyeColor != PocketColor.NONE && this.nextDyeColor == color) {
			if (this.count + 1 > maxDye) {
				repaint(pocket, dyeColor);
				this.dyeColor = color;
				this.nextDyeColor = PocketColor.NONE;
				this.count = 0;
				EntityUtils.chat(entity, new TranslatableText("dimdoors.pocket.pocketHasBeenDyed", dyeColor));
			} else {
				this.count++;
				EntityUtils.chat(entity, new TranslatableText("dimdoors.pocket.remainingNeededDyes", this.count, maxDye, color));
			}
		} else {
			this.nextDyeColor = color;
			this.count = 1;
			EntityUtils.chat(entity, new TranslatableText("dimdoors.pocket.remainingNeededDyes", this.count, maxDye, color));
		}
		return true;
	}

	@Override
	public boolean applicable(Pocket pocket) {
		return pocket instanceof PrivatePocket;
	}

	@Override
	public PocketAddon fromTag(CompoundTag tag) {

		this.dyeColor = PocketColor.from(tag.getInt("dyeColor"));
		this.nextDyeColor = PocketColor.from(tag.getInt("nextDyeColor"));
		this.count = tag.getInt("count");

		return this;
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		PocketAddon.super.toTag(tag);

		tag.putInt("dyeColor", this.dyeColor.getId());
		tag.putInt("nextDyeColor", this.nextDyeColor.getId());
		tag.putInt("count", this.count);

		return tag;
	}

	@Override
	public PocketAddonType<? extends PocketAddon> getType() {
		return PocketAddonType.DYEABLE_ADDON;
	}

	@Override
	public Identifier getId() {
		return ID;
	}

	public interface DyeablePocketBuilder<T extends Pocket.PocketBuilder<T, ?>> extends PocketBuilderExtension<T> {
		default T dyeColor(PocketColor dyeColor) {

			this.<DyeableBuilderAddon>getAddon(ID).dyeColor = dyeColor;

			return getSelf();
		}
	}

	public static class DyeableBuilderAddon implements PocketBuilderAddon<DyeableAddon> {

		private PocketColor dyeColor = PocketColor.NONE;
		// TODO: add some Pocket#init so that we can have boolean shouldRepaintOnInit

		@Override
		public void apply(Pocket pocket) {
			DyeableAddon addon = new DyeableAddon();
			addon.dyeColor = dyeColor;
			pocket.addAddon(addon);
		}

		@Override
		public Identifier getId() {
			return ID;
		}

		@Override
		public PocketBuilderAddon<DyeableAddon> fromTag(CompoundTag tag) {
			this.dyeColor = PocketColor.from(tag.getInt("dye_color"));

			return this;
		}

		@Override
		public CompoundTag toTag(CompoundTag tag) {
			PocketBuilderAddon.super.toTag(tag);

			tag.putInt("dye_color", dyeColor.getId());

			return tag;
		}

		@Override
		public PocketAddonType<DyeableAddon> getType() {
			return PocketAddonType.DYEABLE_ADDON;
		}
	}

	public interface DyeablePocket extends AddonProvider {
		default boolean addDye(Entity entity, DyeColor dyeColor) {
			ensureIsPocket();
			if (!this.hasAddon(ID)) {
				DyeableAddon addon = new DyeableAddon();
				this.addAddon(addon);
				return addon.addDye((Pocket) this, entity, dyeColor);
			}
			return this.<DyeableAddon>getAddon(ID).addDye((Pocket) this, entity, dyeColor);
		}
	}
}
