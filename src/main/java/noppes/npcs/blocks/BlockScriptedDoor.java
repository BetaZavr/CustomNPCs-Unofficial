package noppes.npcs.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import noppes.npcs.*;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.BlockEvent;
import noppes.npcs.blocks.tiles.TileScriptedDoor;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketPlaySound;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.shared.common.CommonUtil;

import javax.annotation.Nonnull;
import java.util.Objects;

public class BlockScriptedDoor extends BlockNpcDoorInterface {

	@Override
	public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
		BlockPos blockpos1 = (state.getValue(BlockScriptedDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER) ? pos
				: pos.down();
		IBlockState iblockstate1 = pos.equals(blockpos1) ? state : world.getBlockState(blockpos1);
		if (!world.isRemote && iblockstate1.getBlock() == this) {
			TileScriptedDoor tile = (TileScriptedDoor) world.getTileEntity(pos);
			if (tile != null) { EventHooks.onScriptBlockBreak(tile); }
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
		return new TileScriptedDoor();
	}

	@Override
	@SuppressWarnings("deprecation")
	public float getBlockHardness(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos) {
		return ((TileScriptedDoor) Objects.requireNonNull(world.getTileEntity(pos))).blockHardness;
	}

	@Override
	public float getExplosionResistance(@Nonnull World world, @Nonnull BlockPos pos, Entity exploder, @Nonnull Explosion explosion) {
		return ((TileScriptedDoor) Objects.requireNonNull(world.getTileEntity(pos))).blockResistance;
	}

	@Override
	@SuppressWarnings("deprecation")
	public @Nonnull EnumBlockRenderType getRenderType(@Nonnull IBlockState state) { return EnumBlockRenderType.INVISIBLE; }

	@Override
	public void neighborChanged(@Nonnull IBlockState state, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull Block neighborBlock, @Nonnull BlockPos pos2) {
		if (state.getValue(BlockScriptedDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER) {
			BlockPos blockpos1 = pos.down();
			IBlockState iblockstate1 = worldIn.getBlockState(blockpos1);
			if (iblockstate1.getBlock() != this) {
				worldIn.setBlockToAir(pos);
			} else if (neighborBlock != this) {
				neighborChanged(iblockstate1, worldIn, blockpos1, neighborBlock, blockpos1);
			}
		} else {
			BlockPos blockpos2 = pos.up();
			IBlockState iblockstate2 = worldIn.getBlockState(blockpos2);
			if (iblockstate2.getBlock() != this) {
				worldIn.setBlockToAir(pos);
			} else {
				TileScriptedDoor tile = (TileScriptedDoor) worldIn.getTileEntity(pos);
				if (!worldIn.isRemote && tile != null) {
					EventHooks.onScriptBlockNeighborChanged(tile, pos2);
				}
				boolean flag = worldIn.isBlockPowered(pos) || worldIn.isBlockPowered(blockpos2);
				if ((flag || neighborBlock.getDefaultState().canProvidePower()) && neighborBlock != this
						&& flag != iblockstate2.getValue(BlockScriptedDoor.POWERED)) {
					worldIn.setBlockState(blockpos2, iblockstate2.withProperty(BlockScriptedDoor.POWERED, flag), 2);
					if (flag != state.getValue(BlockScriptedDoor.OPEN)) {
						toggleDoor(worldIn, pos, flag);
					}
				}
				int power = 0;
				for (EnumFacing enumfacing : EnumFacing.values()) {
					int p = worldIn.getRedstonePower(pos.offset(enumfacing), enumfacing);
					if (p > power) {
						power = p;
					}
				}
                if (tile != null) {
					tile.newPower = power;
				}
			}
		}
	}

	@Override
	public boolean onBlockActivated(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer playerIn,
									@Nonnull EnumHand hand, @Nonnull EnumFacing side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			BlockEvent.DoorToggleEvent event = new BlockEvent.DoorToggleEvent(Objects.requireNonNull(NpcAPI.Instance()).getIBlock(world, pos));
			EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.DOOR_TOGGLE, event);
			return !event.isCanceled();
		}
		else if (playerIn instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) playerIn;
			BlockPos blockpos1 = state.getValue(BlockScriptedDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER ? pos : pos.down();
			IBlockState iblockstate1 = pos.equals(blockpos1) ? state : world.getBlockState(blockpos1);
			if (iblockstate1.getBlock() != this) { return false; }
			ItemStack heldItem = player.inventory.getCurrentItem();
			if (heldItem.getItem() == CustomItems.wand ||
					heldItem.getItem() == CustomItems.scripter ||
					heldItem.getItem() == CustomBlocks.scripted_door_item) {
				if ((CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
						!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_SCRIPTER)) {
					player.sendMessage(Component.translatable("availability.permission").getParent());
					return false;
				}
				PlayerData.get(player).scriptBlockPos = blockpos1;
				SPacketGuiOpen.sendOpenGui(player, EnumGuiType.ScriptDoor, null, blockpos1);
			}
			else {
				TileScriptedDoor tile = (TileScriptedDoor) world.getTileEntity(blockpos1);
				if (tile == null ||
						EventHooks.onScriptBlockInteract(tile, player, side.getIndex(), hitX, hitY, hitZ))
				{ return false; }
				toggleDoor(world, blockpos1, iblockstate1.getValue(BlockDoor.OPEN).equals(false));
			}
		}
		return true;
	}

	@Override
	public void onBlockClicked(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer playerIn) {
		if (world.isRemote) {
			return;
		}
		IBlockState state = world.getBlockState(pos);
		BlockPos blockpos1 = (state.getValue(BlockScriptedDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER) ? pos : pos.down();
		IBlockState iblockstate1 = pos.equals(blockpos1) ? state : world.getBlockState(blockpos1);
		if (iblockstate1.getBlock() != this) {
			return;
		}
		TileScriptedDoor tile = (TileScriptedDoor) world.getTileEntity(blockpos1);
		if (tile != null) { EventHooks.onScriptBlockClicked(tile, playerIn); }
	}

	@Override
	public void onBlockHarvested(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer player) {
		BlockPos blockpos1 = (state.getValue(BlockScriptedDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER) ? pos
				: pos.down();
		IBlockState iblockstate1 = pos.equals(blockpos1) ? state : world.getBlockState(blockpos1);
		if (player.capabilities.isCreativeMode
				&& iblockstate1.getValue(BlockScriptedDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER
				&& iblockstate1.getBlock() == this) {
			world.setBlockToAir(blockpos1);
		}
	}

	@Override
	public void onEntityCollidedWithBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Entity entityIn) {
		if (world.isRemote) {
			return;
		}
		TileScriptedDoor tile = (TileScriptedDoor) world.getTileEntity(pos);
		if (tile != null) { EventHooks.onScriptBlockCollide(tile, entityIn); }
	}

	@Override
	public boolean removedByPlayer(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest) {
		if (!world.isRemote) {
			TileScriptedDoor tile = (TileScriptedDoor) world.getTileEntity(pos);
			if (tile != null && EventHooks.onScriptBlockHarvest(tile, player)) {
				return false;
			}
		}
		return super.removedByPlayer(state, world, pos, player, willHarvest);
	}

	@Override
	public void toggleDoor(@Nonnull World world, @Nonnull BlockPos pos, boolean open) {
		TileScriptedDoor tile = (TileScriptedDoor) world.getTileEntity(pos);
		if (tile != null && EventHooks.onScriptBlockDoorToggle(tile)) {
			return;
		}
		IBlockState iblockstate = world.getBlockState(pos);
		if (iblockstate.getBlock() == this) {
			BlockPos blockpos = iblockstate.getValue(HALF) == BlockDoor.EnumDoorHalf.LOWER ? pos : pos.down();
			IBlockState iblockstate1 = pos == blockpos ? iblockstate : world.getBlockState(blockpos);
			if (iblockstate1.getBlock() == this && iblockstate1.getValue(OPEN) != open) {
				world.setBlockState(blockpos, iblockstate1.withProperty(OPEN, open), 10);
				world.markBlockRangeForRenderUpdate(blockpos, pos);
				if (tile != null) {
					String sound = open ? tile.openSound : tile.closeSound;
					if (sound != null && !sound.isEmpty()) {
						Packets.sendNearby(world, pos, 32,
								new PacketPlaySound(sound, SoundCategory.NEUTRAL, pos.getX(), pos.getY(), pos.getZ(), 1.0f, 1.0f));
					} else {
						world.playEvent(null, open ? blockMaterial == Material.IRON ? 1005 : 1006 : blockMaterial == Material.IRON ? 1011 : 1012, pos, 0);
					}
				}
			}
		}
	}

}
