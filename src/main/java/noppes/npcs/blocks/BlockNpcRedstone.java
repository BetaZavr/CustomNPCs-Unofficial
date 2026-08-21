package noppes.npcs.blocks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.blocks.tiles.TileRedstoneBlock;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class BlockNpcRedstone extends BlockInterface {
   public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

   public BlockNpcRedstone() {
      super(Properties.copy(Blocks.STONE).lightLevel((state) -> 12).strength(50.0F, 2000.0F));
   }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player playerIn,
                                         @Nonnull InteractionHand hand, @Nonnull BlockHitResult ray) {
      if (!level.isClientSide && playerIn instanceof ServerPlayer player) {
         if (player.getInventory().getSelected().getItem() == CustomItems.wand &&
                 CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.EDIT_BLOCKS)) {
            SPacketGuiOpen.sendOpenGui(player, EnumGuiType.RedstoneBlock, null, pos);
            return InteractionResult.SUCCESS;
         }
      }
      return InteractionResult.PASS;
   }

   @Override
   @SuppressWarnings("deprecation")
   public void onPlace(@Nonnull BlockState state, Level levelIn, @Nonnull BlockPos pos, @Nonnull BlockState stateNew, boolean bo) {
      levelIn.updateNeighborsAt(pos, this);
      levelIn.updateNeighborsAt(pos.below(), this);
      levelIn.updateNeighborsAt(pos.above(), this);
      levelIn.updateNeighborsAt(pos.west(), this);
      levelIn.updateNeighborsAt(pos.east(), this);
      levelIn.updateNeighborsAt(pos.south(), this);
      levelIn.updateNeighborsAt(pos.north(), this);
   }

   @Override
   public void setPlacedBy(Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity entity, @Nonnull ItemStack item) {
      if (!level.isClientSide && entity instanceof ServerPlayer player) {
         SPacketGuiOpen.sendOpenGui(player, EnumGuiType.RedstoneBlock, null, pos);
      }
   }

   @Override
   @SuppressWarnings("deprecation")
   public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
      onPlace(state, level, pos, state, isMoving);
   }

   @Override
   @SuppressWarnings("deprecation")
   public int getSignal(@Nonnull BlockState state, @Nonnull BlockGetter worldIn, @Nonnull BlockPos pos, @Nonnull Direction side) {
      return isActivated(state);
   }

   @Override
   @SuppressWarnings("deprecation")
   public int getDirectSignal(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull Direction side) {
      return isActivated(state);
   }

   @Override
   @SuppressWarnings("deprecation")
   public boolean isSignalSource(@Nonnull BlockState state) { return true; }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(ACTIVE);
   }

   @Override
   public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
      return new TileRedstoneBlock(pos, state);
   }

   @Override
   public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) { return RenderShape.MODEL; }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
      return createTickerHelper(type, CustomBlocks.tile_redstoneblock, TileRedstoneBlock::tick);
   }

   public int isActivated(BlockState state) { return state.getValue(ACTIVE) ? 15 : 0; }

}
