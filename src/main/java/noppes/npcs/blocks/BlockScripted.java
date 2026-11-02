package noppes.npcs.blocks;

import java.util.Objects;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import noppes.npcs.*;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.shared.common.CommonUtil;

import javax.annotation.Nonnull;

public class BlockScripted extends BlockInterface {

	public static AxisAlignedBB AABB = new AxisAlignedBB(0.002, 0.002, 0.002, 0.998, 0.998, 0.998);
	public static AxisAlignedBB AABB_EMPTY = new AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

	public BlockScripted() {
		super(Material.ROCK);
		setName("npcscripted");
		setHardness(5.0f);
		setResistance(10.0f);
		setCreativeTab(CustomTabs.TOOLS);
		setSoundType(SoundType.STONE);
	}

	@Override
	public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
		if (!world.isRemote) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof TileScripted)) {
				super.breakBlock(world, pos, state);
				return;
			}
			EventHooks.onScriptBlockBreak((TileScripted) tile);
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public boolean canCreatureSpawn(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EntityLiving.SpawnPlacementType type) {
		return true;
	}

	@Override
	public boolean canEntityDestroy(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull Entity entity) {
		return super.canEntityDestroy(state, world, pos, entity);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean canProvidePower(@Nonnull IBlockState state) {
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
		return new TileScripted();
	}

	@Override
	public void fillWithRain(@Nonnull World world, @Nonnull BlockPos pos) {
		if (world.isRemote) {
			return;
		}
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			return;
		}
		EventHooks.onScriptBlockRainFill((TileScripted) tile);
	}

	@Override
	@SuppressWarnings("deprecation")
	public float getBlockHardness(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			return blockHardness;
		}
		return ((TileScripted) tile).blockHardness;
	}

	@Override
	@SuppressWarnings("deprecation")
	public @Nonnull AxisAlignedBB getBoundingBox(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
		return BlockScripted.AABB;
	}

	@Override
	@SuppressWarnings("deprecation")
	public @Nonnull AxisAlignedBB getCollisionBoundingBox(@Nonnull IBlockState blockState, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			return BlockScripted.AABB_EMPTY;
		}
		if (((TileScripted) tile).isPassable) {
			return BlockScripted.AABB_EMPTY;
		}
		return BlockScripted.AABB;
	}

	@Override
	public float getEnchantPowerBonus(@Nonnull World world, @Nonnull BlockPos pos) {
		return super.getEnchantPowerBonus(world, pos);
	}

	@Override
	public float getExplosionResistance(@Nonnull World world, @Nonnull BlockPos pos, Entity exploder, @Nonnull Explosion explosion) {
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			return super.getExplosionResistance(world, pos, exploder, explosion);
		}
		return ((TileScripted) Objects.requireNonNull(world.getTileEntity(pos))).blockResistance;
	}

	@Override
	public @Nonnull Item getItemDropped(@Nonnull IBlockState state, @Nonnull Random rand, int fortune) {
		return ItemStack.EMPTY.getItem();
	}

	@Override
	public int getLightValue(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			return super.getLightValue(state, world, pos);
		}
		return ((TileScripted) tile).lightValue;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getStrongPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			return super.getStrongPower(state, world, pos, side);
		}
		return ((TileScripted) tile).activePowering;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getWeakPower(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
		return getStrongPower(state, worldIn, pos, side);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isFullCube(@Nonnull IBlockState state) {
		return false;
	}

	@Override
	public boolean isLadder(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EntityLivingBase entity) {
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			return super.isLadder(state, world, pos, entity);
		}
		return ((TileScripted) tile).isLadder;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(@Nonnull IBlockState state) {
		return false;
	}

	@Override
	public boolean isPassable(@Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			return false;
		}
		return ((TileScripted) tile).isPassable;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void neighborChanged(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Block neighborBlock, @Nonnull BlockPos pos2) {
		if (world.isRemote) {
			return;
		}
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			super.neighborChanged(state, world, pos, neighborBlock, pos2);
			return;
		}
		EventHooks.onScriptBlockNeighborChanged((TileScripted) tile, pos2);
		int power = 0;
		for (EnumFacing enumfacing : EnumFacing.values()) {
			int p = world.getRedstonePower(pos.offset(enumfacing), enumfacing);
			if (p > power) {
				power = p;
			}
		}
		if (((TileScripted) tile).prevPower != power && ((TileScripted) tile).powering <= 0) {
			((TileScripted) tile).newPower = power;
		}
	}

	@Override
	public boolean onBlockActivated(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand, @Nonnull EnumFacing side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote && playerIn instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) playerIn;
			ItemStack currentItem = player.inventory.getCurrentItem();
			if (currentItem.getItem() != CustomItems.wand && currentItem.getItem() != CustomItems.scripter) {
				float x = (float)(hitX - (double)pos.getX());
				float y = (float)(hitY - (double)pos.getY());
				float z = (float)(hitZ - (double)pos.getZ());
				TileScripted tile = (TileScripted) world.getTileEntity(pos);
				if (tile == null) { return false; }
				return !EventHooks.onScriptBlockInteract(tile, player, side.getIndex(), x, y, z);
			}
			if ((CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
					!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_SCRIPTER)) {
				player.sendMessage(Component.translatable("availability.permission").getParent());
				return false;
			}
			PlayerData.get(player).scriptBlockPos = pos;
			SPacketGuiOpen.sendOpenGui(player, EnumGuiType.ScriptBlock, null, pos);
		}
		return true;
	}

	@Override
	public void onBlockClicked(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
		if (world.isRemote) {
			return;
		}
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			return;
		}
		EventHooks.onScriptBlockClicked((TileScripted) tile, player);
	}

	@Override
	public void onBlockExploded(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull Explosion explosion) {
		if (!world.isRemote) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof TileScripted)) {
				super.onBlockExploded(world, pos, explosion);
				return;
			}
			if (EventHooks.onScriptBlockExploded((TileScripted) tile)) {
				return;
			}
		}
		super.onBlockExploded(world, pos, explosion);
	}

	@Override
	public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase entity, @Nonnull ItemStack stack) {
		if (!world.isRemote && entity instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) entity;
			if ((!CustomNpcs.OpsOnly || CommonUtil.isOp(player)) &&
					CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_SCRIPTER)) {
				PlayerData.get((EntityPlayerMP) entity).scriptBlockPos = pos;
				SPacketGuiOpen.sendOpenGui((EntityPlayerMP) entity, EnumGuiType.ScriptBlock, null, pos);
			}
		}
	}

	@Override
	public void onEntityCollidedWithBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Entity entityIn) {
		if (world.isRemote) {
			return;
		}
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			return;
		}
		EventHooks.onScriptBlockCollide((TileScripted) tile, entityIn);
	}

	@Override
	public void onFallenUpon(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull Entity entity, float fallDistance) {
		if (world.isRemote) {
			return;
		}
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileScripted)) {
			super.onFallenUpon(world, pos, entity, fallDistance);
			return;
		}
		fallDistance = EventHooks.onScriptBlockFallenUpon((TileScripted) tile, entity, fallDistance);
		super.onFallenUpon(world, pos, entity, fallDistance);
	}

	@Override
	public boolean removedByPlayer(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest) {
		if (!world.isRemote) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof TileScripted)) {
				return super.removedByPlayer(state, world, pos, player, willHarvest);
			}
			if (EventHooks.onScriptBlockHarvest((TileScripted) tile, player)) {
				return false;
			}
		}
		return super.removedByPlayer(state, world, pos, player, willHarvest);
	}

}
