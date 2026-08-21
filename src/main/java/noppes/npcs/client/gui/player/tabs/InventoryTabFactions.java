package noppes.npcs.client.gui.player.tabs;

import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.player.GuiLog;
import org.jetbrains.annotations.NotNull;

public class InventoryTabFactions extends AbstractTab {

   public InventoryTabFactions() {
      super(1, 0, 0, new ItemStack(Items.RED_BANNER, 1), Component.translatable("menu.factions"));
      setFocused(false);
   }

   @Override
   public void onTabClicked() {
      if (minecraft.player != null) {
         NoppesUtil.openGUI(minecraft.player, new GuiLog(1));
      }
   }

   @Override
   protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) { }

}
