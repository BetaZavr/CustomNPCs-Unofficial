package noppes.npcs.client.gui.player.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.player.GuiLog;
import org.jetbrains.annotations.NotNull;

public class InventoryTabQuests extends AbstractTab {

   public InventoryTabQuests() {
      super(2, 0, 0, new ItemStack(Items.BOOK), Component.translatable("quest.quest")
              .append(" (")
              .append(ClientProxy.QuestLog.getKey().getDisplayName())
              .append(")"));
      setFocused(false);
   }

   @Override
   public void onTabClicked() {
      if (minecraft.player != null) {
         NoppesUtil.openGUI(Minecraft.getInstance().player, new GuiLog(0));
      }
   }

   @Override
   protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) { }

}
