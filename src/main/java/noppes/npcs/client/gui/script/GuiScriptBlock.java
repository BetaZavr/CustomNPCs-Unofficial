package noppes.npcs.client.gui.script;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import noppes.npcs.blocks.tiles.TileScripted;

public class GuiScriptBlock extends GuiScriptInterface {

   protected final TileScripted script;

   public GuiScriptBlock(BlockPos pos) {
      super(1);
      handler = script = (TileScripted) player.level().getBlockEntity(pos);
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      script.setNBT(compound);
      super.setGuiData(compound);
   }

   @Override
   public void save() {
      super.save();
      sendToServer(script.save(new CompoundTag()));
   }

}
