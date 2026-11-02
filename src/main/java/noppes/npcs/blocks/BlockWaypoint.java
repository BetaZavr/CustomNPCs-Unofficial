package noppes.npcs.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
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
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.*;
import noppes.npcs.blocks.tiles.TileWaypoint;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.shared.common.CommonUtil;

import javax.annotation.Nonnull;

public class BlockWaypoint extends BlockInterface {

	public BlockWaypoint() {
		super(Material.IRON);
		setName("npcwaypoint");
		setSoundType(SoundType.METAL);
		setHardness(5.0f);
		setResistance(10.0f);
		setCreativeTab(CustomTabs.TOOLS);
	}

	@Override
	public TileEntity createNewTileEntity(@Nonnull World var1, int var2) {
		return new TileWaypoint();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public @Nonnull BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.CUTOUT;
	}

	@Override
	public @Nonnull EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
		return EnumBlockRenderType.MODEL;
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
				SPacketGuiOpen.sendOpenGui(player, EnumGuiType.Waypoint, null, pos);
			}
		}
		return true;
	}

	@Override
	public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase entity, @Nonnull ItemStack stack) {
		if (entity instanceof EntityPlayerMP && !world.isRemote) {
			SPacketGuiOpen.sendOpenGui((EntityPlayerMP) entity, EnumGuiType.Waypoint, null, pos);
		}
	}

}
