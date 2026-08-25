package noppes.npcs.client.gui.script;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import noppes.npcs.blocks.tiles.TileScriptedDoor;

public class GuiScriptDoor extends GuiScriptInterface {

   protected final TileScriptedDoor script;

   public GuiScriptDoor(BlockPos pos) {
      super(5);
      handler = script = (TileScriptedDoor) player.level().getBlockEntity(pos);
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      script.setNBT(compound);
      super.setGuiData(compound);
   }

   @Override
   public void save() {
      super.save();
      sendToServer(script.getNBT(new CompoundTag()));
   }

}
