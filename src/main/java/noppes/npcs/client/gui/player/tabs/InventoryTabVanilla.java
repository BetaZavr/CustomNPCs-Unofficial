package noppes.npcs.client.gui.player.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class InventoryTabVanilla extends AbstractTab {

   public InventoryTabVanilla() {
      super(0, 0, 0, new ItemStack(Blocks.CRAFTING_TABLE), Component.translatable("stats.rarity.normal")
              .append(" (")
              .append(Minecraft.getInstance().options.keyInventory.getKey().getDisplayName())
              .append(")"));
      setFocused(true);
   }

   @Override
   public void onTabClicked() {
      if (minecraft.player != null) {
         minecraft.player.connection.send(new ServerboundContainerClosePacket(minecraft.player.containerMenu.containerId));
         InventoryScreen inventory = new InventoryScreen(minecraft.player);
         minecraft.setScreen(inventory);
      }
   }

   @Override
   protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) { }

}
