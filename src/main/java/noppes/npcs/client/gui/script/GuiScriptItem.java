package noppes.npcs.client.gui.script;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomItems;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;

public class GuiScriptItem extends GuiScriptInterface {

   protected final ItemScriptedWrapper item;

   public GuiScriptItem() {
      super(2);
      handler = item = new ItemScriptedWrapper(new ItemStack(CustomItems.scripted_item));
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      item.setMCNbt(compound);
      super.setGuiData(compound);
   }

   @Override
   public void save() {
      super.save();
      sendToServer(item.getMCNbt());
   }

}
