package noppes.npcs.blocks.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import noppes.npcs.CustomBlocks;

import javax.annotation.Nonnull;

public class TileMailbox extends TileNpcEntity {

   private int type = 0;

   public TileMailbox(BlockPos pos, BlockState state) { super(CustomBlocks.tile_mailbox, pos, state); }

   public TileMailbox setModel(int typeIn) {
      type = typeIn;
      return this;
   }

   public int getModel() { return type; }

   @Override
   public void load(@Nonnull CompoundTag compound) {
      super.load(compound);
      type = compound.getInt("Type");
   }

   @Override
   public void saveAdditional(@Nonnull CompoundTag compound) {
      compound.putInt("Type", type);
      super.saveAdditional(compound);
   }

}
