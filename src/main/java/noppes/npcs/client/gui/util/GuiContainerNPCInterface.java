package noppes.npcs.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.GuiBasicContainer;
import org.jetbrains.annotations.NotNull;

public abstract class GuiContainerNPCInterface<T extends AbstractContainerMenu> extends GuiBasicContainer<T> {

   public EntityNPCInterface npc;

   public GuiContainerNPCInterface(EntityNPCInterface npcIn, T cont, Inventory inv, Component ignoredTitleIn) {
      super(cont, inv, Component.empty());
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      npc = npcIn;
   }

   public void drawNpc(GuiGraphics graphics, int x, int y) {
      if (npc == null) { return; }
      wrapper.drawNpc(graphics, npc, x, y, 1.0F, 0, 0, 0, guiLeft, guiTop);
   }

   public void drawNpc(@NotNull GuiGraphics graphics, Entity entity, int x, int y, float zoomed, int rotation, int vertical, int followCursor) {
      if (entity == null) { return; }
      wrapper.drawNpc(graphics, entity, x, y, zoomed, rotation, vertical, followCursor, guiLeft, guiTop);
   }

}
