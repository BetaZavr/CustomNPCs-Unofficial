package noppes.npcs.client.gui.script;

import net.minecraft.nbt.CompoundTag;
import noppes.npcs.controllers.data.PlayerScriptData;

public class GuiScriptPlayers extends GuiScriptInterface {

   protected final PlayerScriptData script = new PlayerScriptData(null);

   public GuiScriptPlayers() {
      super(4);
      handler = script;
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      script.load(compound);
      super.setGuiData(compound);
   }

   @Override
   public void save() {
      super.save();
      sendToServer(script.save(new CompoundTag()));
   }

}
