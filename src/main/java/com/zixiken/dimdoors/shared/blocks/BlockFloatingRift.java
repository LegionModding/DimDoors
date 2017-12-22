package com.zixiken.dimdoors.shared.blocks;

import java.util.*;

import com.zixiken.dimdoors.DimDoors;
import com.zixiken.dimdoors.client.ParticleRiftEffect;
import com.zixiken.dimdoors.shared.items.ModItems;
import com.zixiken.dimdoors.shared.tileentities.TileEntityFloatingRift;
import ddutils.blocks.BlockSpecialAir;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockFloatingRift extends BlockSpecialAir implements ITileEntityProvider, IRiftProvider<TileEntityFloatingRift> {

    public static final String ID = "rift";

    public BlockFloatingRift() {
        // super();
        setRegistryName(new ResourceLocation(DimDoors.MODID, ID));
        setUnlocalizedName(ID);
        setTickRandomly(true);
        setResistance(6000000.0F); // Same as bedrock
    }

    @Override
    public TileEntityFloatingRift createNewTileEntity(World worldIn, int meta) {
        return new TileEntityFloatingRift();
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return MapColor.BLUE;
    }

    // Unregister the rift on break
    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntityFloatingRift rift = (TileEntityFloatingRift) worldIn.getTileEntity(pos);
        rift.unregister();
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return false; // TODO
    }

    @Override
    public TileEntityFloatingRift getRift(World world, BlockPos pos, IBlockState state) {
        return (TileEntityFloatingRift) world.getTileEntity(pos);
    }

    @Override public void setupRift(TileEntityFloatingRift rift) {} // No default setup

    public void dropWorldThread(World world, BlockPos pos, Random random) {
        if (!world.getBlockState(pos).equals(Blocks.AIR)) {
            ItemStack thread = new ItemStack(ModItems.WORLD_THREAD, 1);
            world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), thread));
        }
    }

    // Render rift effects
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) { // TODO
        //ArrayList<BlockPos> targets = findReachableBlocks(worldIn, pos, 2, false);
        TileEntityFloatingRift rift = (TileEntityFloatingRift) worldIn.getTileEntity(pos);

        if (0 > 0) {
            FMLClientHandler.instance().getClient().effectRenderer.addEffect(new ParticleRiftEffect.GogglesRiftEffect(
                    worldIn,
                    pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                    rand.nextGaussian() * 0.01D, rand.nextGaussian() * 0.01D, rand.nextGaussian() * 0.01D));
        }

        if (rift.shouldClose) { // Renders an opposite color effect if it is being closed by the rift remover
            FMLClientHandler.instance().getClient().effectRenderer.addEffect(new ParticleRiftEffect.ClosingRiftEffect(
                    worldIn,
                    pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                    rand.nextGaussian() * 0.01D, rand.nextGaussian() * 0.01D, rand.nextGaussian() * 0.01D));
        }
    }
}
