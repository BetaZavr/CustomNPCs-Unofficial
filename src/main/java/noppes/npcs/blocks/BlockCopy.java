package noppes.npcs.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import noppes.npcs.CustomItems;
import noppes.npcs.blocks.tiles.TileCopy;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.server.SPacketGuiOpen;

import javax.annotation.Nonnull;

public class BlockCopy extends BlockInterface {

   public BlockCopy() { super(Properties.copy(Blocks.BARRIER).sound(SoundType.STONE)); }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player playerIn,
                                         @Nonnull InteractionHand hand, @Nonnull BlockHitResult ray) {
      if (!level.isClientSide && playerIn instanceof ServerPlayer player) {
         if (player.getInventory().getSelected().getItem() == CustomItems.wand) {
            SPacketGuiOpen.sendOpenGui(player, EnumGuiType.CopyBlock, null, pos);
         }
         return InteractionResult.SUCCESS;
      }
      return InteractionResult.PASS;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      if (!context.getLevel().isClientSide && context.getPlayer() instanceof ServerPlayer player) {
         SPacketGuiOpen.sendOpenGui(player, EnumGuiType.CopyBlock, null, context.getClickedPos());
      }
      return defaultBlockState();
   }

   @Override
   public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new TileCopy(pos, state); }

}
