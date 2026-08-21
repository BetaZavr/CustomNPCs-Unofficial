package noppes.npcs.blocks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.BlockEvent;
import noppes.npcs.blocks.tiles.TileScriptedDoor;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.server.SPacketGuiOpen;

import java.util.Objects;

public class BlockScriptedDoor extends BlockNpcDoorInterface {

   public BlockScriptedDoor() {
      super(Properties.copy(Blocks.IRON_DOOR).strength(5.0F, 10.0F));
   }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull ItemStack getCloneItemStack(@Nonnull BlockGetter worldIn, @Nonnull BlockPos pos, @Nonnull BlockState state) {
      return new ItemStack(CustomBlocks.scripted_door_item);
   }

   @Override
   public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new TileScriptedDoor(pos, state); }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) { return RenderShape.INVISIBLE; }

   @Override
   public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                         @Nonnull InteractionHand hand, @Nonnull BlockHitResult ray) {
      if (level.isClientSide) {
         BlockEvent.DoorToggleEvent event = new BlockEvent.DoorToggleEvent(Objects.requireNonNull(NpcAPI.Instance()).getIBlock(level, pos));
         EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.DOOR_TOGGLE, event);
         return event.isCanceled() ? InteractionResult.FAIL : InteractionResult.SUCCESS;
      }
      BlockPos blockpos1 = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
      BlockState iblockstate1 = pos.equals(blockpos1) ? state : level.getBlockState(blockpos1);
      if (iblockstate1.getBlock() == this) {
         ItemStack currentItem = player.getInventory().getSelected();
         if (currentItem.getItem() == CustomItems.wand ||
                 currentItem.getItem() == CustomItems.scripter ||
                 currentItem.getItem() == CustomBlocks.scripted_door_item) {
            PlayerData data = PlayerData.get(player);
            data.scriptBlockPos = blockpos1;
            SPacketGuiOpen.sendOpenGui((ServerPlayer) player, EnumGuiType.ScriptDoor, null, blockpos1);
            return InteractionResult.SUCCESS;
         }
         TileScriptedDoor tile = (TileScriptedDoor)level.getBlockEntity(blockpos1);
         if (tile != null) {
            Vec3 vec = ray.getLocation();
            float x = (float)(vec.x - (double)pos.getX());
            float y = (float)(vec.y - (double)pos.getY());
            float z = (float)(vec.z - (double)pos.getZ());
            if (EventHooks.onScriptBlockInteract(tile, player, ray.getDirection().get3DDataValue(), x, y, z)) { return InteractionResult.FAIL; }
            this.setOpen(player, level, iblockstate1, blockpos1, iblockstate1.getValue(DoorBlock.OPEN).equals(false));
            return InteractionResult.SUCCESS;
         }
      }
      return InteractionResult.FAIL;
   }

   @Override
   public void neighborChanged(BlockState state, @Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull Block neighborBlock,
                               @Nonnull BlockPos pos2, boolean isMoving) {
      BlockPos blockpos2;
      BlockState iblockstate2;
      if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
         blockpos2 = pos.below();
         iblockstate2 = worldIn.getBlockState(blockpos2);
         if (iblockstate2.getBlock() != this) {
            worldIn.removeBlock(pos, false);
         } else if (neighborBlock != this) {
            this.neighborChanged(iblockstate2, worldIn, blockpos2, neighborBlock, blockpos2, isMoving);
         }
      } else {
         blockpos2 = pos.above();
         iblockstate2 = worldIn.getBlockState(blockpos2);
         if (iblockstate2.getBlock() != this) {
            worldIn.removeBlock(pos, false);
         } else {
            TileScriptedDoor tile = (TileScriptedDoor)worldIn.getBlockEntity(pos);
            if (!worldIn.isClientSide && tile != null) {
               EventHooks.onScriptBlockNeighborChanged(tile, pos2);
            }

            boolean flag = worldIn.hasNeighborSignal(pos) || worldIn.hasNeighborSignal(blockpos2);
            if ((flag || neighborBlock.defaultBlockState().isSignalSource()) && neighborBlock != this && flag != iblockstate2.getValue(POWERED)) {
               worldIn.setBlock(blockpos2, iblockstate2.setValue(POWERED, flag), 2);
               if (flag != state.getValue(OPEN)) {
                  this.setOpen(null, worldIn, state, pos, flag);
               }
            }

            int power = 0;
            for (Direction direction : Direction.values()) {
               int p = worldIn.getSignal(pos.relative(direction), direction);
               if (p > power) {
                  power = p;
               }
            }
            if (tile != null) { tile.newPower = power; }
         }
      }

   }

   @Override
   public void setOpen(Entity entity, Level worldIn, @Nonnull BlockState state, @Nonnull BlockPos pos, boolean open) {
      TileScriptedDoor tile = (TileScriptedDoor)worldIn.getBlockEntity(pos);
      if (tile == null || !EventHooks.onScriptBlockDoorToggle(tile)) {
         super.setOpen(entity, worldIn, state, pos, open);
      }
   }

   @Override
   @SuppressWarnings("deprecation")
   public void attack(@Nonnull BlockState state, Level level, @Nonnull  BlockPos pos, @Nonnull Player playerIn) {
      if (!level.isClientSide) {
         BlockPos blockpos1 = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
         BlockState iblockstate1 = pos.equals(blockpos1) ? state : level.getBlockState(blockpos1);
         if (iblockstate1.getBlock() == this) {
            TileScriptedDoor tile = (TileScriptedDoor)level.getBlockEntity(blockpos1);
            if (tile != null) { EventHooks.onScriptBlockClicked(tile, playerIn); }
         }
      }
   }

   @Override
   public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
      if (state.getBlock() != newState.getBlock()) {
         BlockPos blockpos1 = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
         BlockState iblockstate1 = pos.equals(blockpos1) ? state : level.getBlockState(blockpos1);
         if (!level.isClientSide && iblockstate1.getBlock() == this) {
            TileScriptedDoor tile = (TileScriptedDoor)level.getBlockEntity(pos);
            if (tile != null) { EventHooks.onScriptBlockBreak(tile); }
         }
         super.onRemove(state, level, pos, newState, isMoving);
      }
   }

   @Override
   public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
      if (!level.isClientSide) {
         TileScriptedDoor tile = (TileScriptedDoor)level.getBlockEntity(pos);
         if (tile != null && EventHooks.onScriptBlockHarvest(tile, player)) {
            return false;
         }
      }
      return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
   }

   @Override
   @SuppressWarnings("deprecation")
   public void entityInside(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Entity entityIn) {
      if (!level.isClientSide) {
         TileScriptedDoor tile = (TileScriptedDoor)level.getBlockEntity(pos);
         if (tile != null) { EventHooks.onScriptBlockCollide(tile, entityIn); }
      }
   }

   @Override
   public void playerWillDestroy(@Nonnull Level level, BlockPos pos, BlockState state, Player player) {
      BlockPos blockpos1 = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
      BlockState iblockstate1 = pos.equals(blockpos1) ? state : level.getBlockState(blockpos1);
      if (player.getAbilities().instabuild && iblockstate1.getValue(HALF) == DoubleBlockHalf.LOWER && iblockstate1.getBlock() == this) {
         level.removeBlock(blockpos1, false);
      }

   }

   @Override
   @SuppressWarnings("deprecation")
   public float getDestroyProgress(@Nonnull BlockState state, @Nonnull Player player, BlockGetter level, @Nonnull BlockPos pos) {
      TileScriptedDoor tile = (TileScriptedDoor) level.getBlockEntity(pos);
      float f;
      if (tile == null) { f = state.getDestroySpeed(level, pos); }
      else { f = tile.blockHardness; }
      if (f == -1.0F) {
         return 0.0F;
      } else {
         int i = ForgeHooks.isCorrectToolForDrops(state, player) ? 30 : 100;
         return player.getDigSpeed(state, pos) / f / (float)i;
      }
   }

   @Override
   public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
      TileScriptedDoor tile = (TileScriptedDoor) level.getBlockEntity(pos);
      return tile != null ? tile.blockResistance : super.getExplosionResistance(state, level, pos, explosion);
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
      return createTickerHelper(type, CustomBlocks.tile_scripteddoor, TileScriptedDoor::tick);
   }

   @SuppressWarnings("unchecked")
   protected static @Nullable <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> p_152133_,
                                                                                                                     BlockEntityType<E> p_152134_,
                                                                                                                     BlockEntityTicker<? super E> p_152135_) {
      return p_152134_ == p_152133_ ? (BlockEntityTicker<A>) p_152135_ : null;
   }

}
