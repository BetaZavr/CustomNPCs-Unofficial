package noppes.npcs.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.*;
import noppes.npcs.blocks.tiles.TileRedstoneBlock;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.shared.common.CommonUtil;

import javax.annotation.Nonnull;

public class BlockNpcRedstone extends BlockInterface {

	public static PropertyBool ACTIVE = PropertyBool.create("active");

	public BlockNpcRedstone() {
		super(Material.ROCK);
		setName("npcredstoneblock");
		setHardness(50.0f);
		setResistance(2000.0f);
		setCreativeTab(CustomTabs.TOOLS);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean canProvidePower(@Nonnull IBlockState state) {
		return true;
	}

	@Override
	protected @Nonnull BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockNpcRedstone.ACTIVE);
	}

	@Override
	public TileEntity createNewTileEntity(@Nonnull World var1, int var2) {
		return new TileRedstoneBlock();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public @Nonnull BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.CUTOUT;
	}

	@Override
	public int getMetaFromState(@Nonnull IBlockState state) {
		return state.getValue(BlockNpcRedstone.ACTIVE) ? 1 : 0;
	}

	@Override
	public @Nonnull EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	@SuppressWarnings("deprecation")
	public @Nonnull IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(BlockNpcRedstone.ACTIVE, false);
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getStrongPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
		return isActivated(state);
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getWeakPower(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
		return isActivated(state);
	}

	public int isActivated(IBlockState state) {
		return state.getValue(BlockNpcRedstone.ACTIVE) ? 15 : 0;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isFullCube(@Nonnull IBlockState state) {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(@Nonnull IBlockState state) {
		return false;
	}

	@Override
	public boolean onBlockActivated(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer playerIn,
									@Nonnull EnumHand hand, @Nonnull EnumFacing side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote && playerIn instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) playerIn;
			Item heldItem = player.inventory.getCurrentItem().getItem();
			if (heldItem == CustomItems.wand || heldItem == CustomBlocks.waypoint_item) {
				if ((CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
						!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.EDIT_BLOCKS)) {
					player.sendMessage(Component.translatable("availability.permission").getParent());
					return false;
				}
				SPacketGuiOpen.sendOpenGui(player, EnumGuiType.RedstoneBlock, null, pos);
			}
		}
		return false;
	}

	@Override
	public void onBlockAdded(@Nonnull World par1World, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
		par1World.notifyNeighborsOfStateChange(pos, this, false);
		par1World.notifyNeighborsOfStateChange(pos.down(), this, false);
		par1World.notifyNeighborsOfStateChange(pos.up(), this, false);
		par1World.notifyNeighborsOfStateChange(pos.west(), this, false);
		par1World.notifyNeighborsOfStateChange(pos.east(), this, false);
		par1World.notifyNeighborsOfStateChange(pos.south(), this, false);
		par1World.notifyNeighborsOfStateChange(pos.north(), this, false);
	}

	@Override
	public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase entityliving, @Nonnull ItemStack item) {
		if (entityliving instanceof EntityPlayerMP && !world.isRemote) {
			SPacketGuiOpen.sendOpenGui((EntityPlayerMP) entityliving, EnumGuiType.RedstoneBlock, null, pos);
		}
	}

	@Override
	public void onBlockDestroyedByPlayer(@Nonnull World par1World, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
		onBlockAdded(par1World, pos, state);
	}

}
