package noppes.npcs.blocks;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.storage.loot.LootParams.Builder;

public abstract class BlockNpcDoorInterface extends DoorBlock implements EntityBlock {

   public BlockNpcDoorInterface(Properties properties) { super(properties, BlockSetType.STONE); }

   @Override
   @SuppressWarnings("deprecation")
   public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
      super.onRemove(state, level, pos, newState, isMoving);
      level.removeBlockEntity(pos);
   }

   @Override
   @SuppressWarnings("deprecation")
   public @Nonnull List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull Builder builder) {
      return Collections.emptyList();
   }

   @Override
   public void playerDestroy(@Nonnull Level level, Player playerIn, @Nonnull BlockPos pos, @Nonnull BlockState state,
                             @Nullable BlockEntity blockEntity, @Nonnull ItemStack stack) {
      playerIn.awardStat(Stats.BLOCK_MINED.get(this));
      playerIn.causeFoodExhaustion(0.005F);
      dropResources(state, level, pos, blockEntity, playerIn, stack);
   }

}
