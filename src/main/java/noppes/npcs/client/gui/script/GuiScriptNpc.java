package noppes.npcs.client.gui.script;

import net.minecraft.nbt.CompoundTag;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataScript;

public class GuiScriptNpc extends GuiScriptInterface {

   protected final DataScript script;
   private boolean inited = false;

   public GuiScriptNpc(EntityNPCInterface npc) {
      super(0);
      handler = script = npc.script;
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      script.load(compound);
      inited = true;
      super.setGuiData(compound);
   }

   @Override
   public void save() {
      super.save();
      if (inited) { sendToServer(script.save(new CompoundTag())); }
   }

}
