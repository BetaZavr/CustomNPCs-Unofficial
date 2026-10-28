package noppes.npcs.shared.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

import javax.annotation.Nonnull;

public class GuiMenuTopIconButton extends GuiMenuTopButton {

   private static final ResourceLocation resource = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
   protected final ItemStack item;

   public GuiMenuTopIconButton(IGuiInterface gui, int id, Object label, int x, int y, @Nonnull ItemStack itemIn) {
      super(gui, id, label, x, y);
      item = itemIn.isEmpty() ? new ItemStack(Blocks.DIRT) : itemIn;
      setSize(28, 28);
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (visible) {
         super.render(graphics, mouseX, mouseY, partialTicks);
         if (isHovered) {
            if (listener != null && !hoverText.isEmpty()) { listener.setHoverText(hoverText); }
            else { drawHoveringText(graphics, Collections.singletonList(getMessage()), mouseX, mouseY, Minecraft.getInstance().font); }
         }
      }
   }

   @Override
   public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (visible) {
         isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + height;
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         RenderSystem.setShaderTexture(0, resource);
         graphics.pose().pushPose();
         graphics.blit(resource, getX(), getY() + (active ? 2 : 0), 0, active ? 32 : 0, 28, 28);
         GuiBasic.renderStackInGUI(graphics, item, getX() + 6, getY() + 10, 100.0f, 1.0f, null);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         graphics.pose().popPose();
      }
   }

   protected void drawHoveringText(GuiGraphics graphics, List<Component> list, int x, int y, Font font) {
      if (!list.isEmpty()) {
         RenderSystem.disableDepthTest();
         int k = 0;
         Iterator<Component> var7 = list.iterator();

         int i1;
         while(var7.hasNext()) {
            Component o = var7.next();
            i1 = font.width(o);
            if (i1 > k) { k = i1; }
         }

          int k2 = y;
         i1 = 8;
         if (list.size() > 1) { i1 += 2 + (list.size() - 1) * 10; }

         graphics.pose().pushPose();
         graphics.pose().translate(0.0F, 0.0F, 300.0F);
         int j1 = -267386864;
         graphics.fillGradient(x - 3, y - 4, x + k + 3, y - 3, j1, j1);
         graphics.fillGradient(x - 3, y + i1 + 3, x + k + 3, y + i1 + 4, j1, j1);
         graphics.fillGradient(x - 3, y - 3, x + k + 3, y + i1 + 3, j1, j1);
         graphics.fillGradient(x - 4, y - 3, x - 3, y + i1 + 3, j1, j1);
         graphics.fillGradient(x + k + 3, y - 3, x + k + 4, y + i1 + 3, j1, j1);
         int k1 = 1347420415;
         int l1 = (k1 & 16711422) >> 1 | k1 & -16777216;
         graphics.fillGradient(x - 3, y - 3 + 1, x - 3 + 1, y + i1 + 3 - 1, k1, l1);
         graphics.fillGradient(x + k + 2, y - 3 + 1, x + k + 3, y + i1 + 3 - 1, k1, l1);
         graphics.fillGradient(x - 3, y - 3, x + k + 3, y - 3 + 1, k1, k1);
         graphics.fillGradient(x - 3, y + i1 + 2, x + k + 3, y + i1 + 3, l1, l1);

         for(int i2 = 0; i2 < list.size(); ++i2) {
            Component s1 = list.get(i2);
            graphics.drawString(font, s1, x, k2, -1);
            if (i2 == 0) { k2 += 2; }
            k2 += 10;
         }

         graphics.pose().popPose();
         RenderSystem.enableDepthTest();
      }
   }

}
