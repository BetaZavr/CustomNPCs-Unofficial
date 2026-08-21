package noppes.npcs.blocks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.blocks.tiles.TileBorder;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class BlockBorder extends BlockInterface {

   public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

   public BlockBorder() {
      super(Properties.copy(Blocks.BARRIER).sound(SoundType.STONE));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(ROTATION);
   }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, Player player,
                                         @Nonnull InteractionHand hand, @Nonnull BlockHitResult ray) {
      ItemStack currentItem = player.getInventory().getSelected();
      if (!level.isClientSide && currentItem.getItem() == CustomItems.wand) {
         SPacketGuiOpen.sendOpenGui((ServerPlayer) player, EnumGuiType.Border, null, pos);
         return InteractionResult.SUCCESS;
      }
      return InteractionResult.PASS;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      if (context.getPlayer() != null) { return defaultBlockState().setValue(ROTATION,  Direction.fromYRot(context.getRotation()).get2DDataValue()); }
      else { return super.getStateForPlacement(context); }
   }

   @Override
   public void setPlacedBy(Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                           @Nullable LivingEntity entity, @Nonnull ItemStack item) {
      if (level.getBlockEntity(pos) instanceof TileBorder borderTile) {
         TileBorder adjacent = null;
         for (Direction facing : Direction.values()) {
            adjacent = getAdjacentTile(level, pos.relative(facing));
            if (adjacent != null) break;
         }
         if (adjacent == null) {
            for (int i = 0; i < 3; i++) {
               BlockPos tempPos = i == 0 ? pos : i == 1 ? pos.above() : pos.below();
               if (i != 0) {
                  for (int j = 0; j < 4; j++) {
                     BlockPos p = switch (j) {
                        case 1 -> tempPos.south();
                        case 2 -> tempPos.west();
                        case 3 -> tempPos.north();
                        default -> tempPos.east();
                     };
                     adjacent = getAdjacentTile(level, p);
                     if (adjacent != null) break;
                  }
                  if (adjacent != null) break;
               }
               for (int j = 0; j < 4; j++) {
                  BlockPos p = switch (j) {
                     case 1 -> tempPos.south().east();
                     case 2 -> tempPos.south().west();
                     case 3 -> tempPos.north().east();
                     default -> tempPos.north().west();
                  };
                  adjacent = getAdjacentTile(level, p);
                  if (adjacent != null) break;
               }
               if (adjacent != null) break;
            }
         }
         if (adjacent != null) {
            CompoundTag compound = new CompoundTag();
            adjacent.writeExtraNBT(compound);
            borderTile.readExtraNBT(compound);
         }
         borderTile.rotation = state.getValue(ROTATION);

         CompoundTag nbt = new CompoundTag();
         borderTile.saveAdditional(nbt);
         level.setBlock(pos, state.setValue(ROTATION, borderTile.rotation), 3);
         BlockEntity newTile = level.getBlockEntity(pos);
         if (newTile instanceof TileBorder) {
            newTile.load(nbt);
            newTile.setChanged();
         }

         if (!level.isClientSide && entity instanceof ServerPlayer player) {
            if (adjacent == null) { SPacketGuiOpen.sendOpenGui(player, EnumGuiType.Border, null, pos); }
            else {
               player.sendSystemMessage(Component.translatable("copy.settings.adjacent.block",
                       ChatFormatting.GRAY + "" + adjacent.getBlockPos().getX(),
                       ChatFormatting.GRAY + "" + adjacent.getBlockPos().getY(),
                       ChatFormatting.GRAY + "" + adjacent.getBlockPos().getZ()));
            } // Copy
         }
      }

   }

   @Override
   public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
      return RenderShape.MODEL;
   }

   @Override
   public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
      return new TileBorder(pos, state);
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
      return createTickerHelper(type, CustomBlocks.tile_border, TileBorder::tick);
   }

   private TileBorder getAdjacentTile(Level level, BlockPos pos) {
      BlockEntity tile = level.getBlockEntity(pos);
      Block block = level.getBlockState(pos).getBlock();
      if (tile instanceof TileBorder borderTile && block instanceof BlockBorder) {
         return borderTile;
      }
      return null;
   }

}
