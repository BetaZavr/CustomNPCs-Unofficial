package noppes.npcs.client.gui.player.tabs;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomNpcs;

import javax.annotation.Nonnull;
import java.awt.*;

public abstract class AbstractTab extends AbstractButton {

   protected static final ResourceLocation texture = new ResourceLocation(CustomNpcs.MODID, "textures/gui/tabs.png");
   protected final ItemStack renderStack;
   protected final int id;

   protected Screen screen;
   protected int guiLeft = 0;
   protected int guiTop = 0;

   // New from Unofficial (GoodBird)
   protected @Nonnull Minecraft minecraft;

   public AbstractTab(int idIn, int posX, int posY, @Nonnull ItemStack renderStackIn, @Nonnull Component hoverText) {
      super(posX, posY, 28, 32, hoverText);
      renderStack = renderStackIn;
      id = idIn;

      minecraft = Minecraft.getInstance();
   }

   public AbstractTab init(Screen screenIn) {
      int guiLeft = screenIn.width / 2;
      int guiTop = screenIn.height / 2;
      if (screenIn instanceof InventoryScreen) {
         guiLeft -= 88;
         guiTop -= 82;
      }
      setX(guiLeft + id * 28);
      setY(guiTop - 29);
      screen = screenIn;
      return this;
   }

   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
      active = false;
      if (visible) {
         int x = getX();
         if (screen instanceof InventoryScreen inv) {
            if (inv.getRecipeBookComponent().isVisible()) { x += 77; }
            active = id == 0;
         }
         else { active = id != 0; }
         RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
         int yTexPos = isFocused() ? 32 : 2;
         int ySize = isFocused() ? 32 : 29;
         int xOffset = active ? 0 : 28;
         RenderSystem.setShaderTexture(0, texture);
         graphics.blit(texture, x, getY(), xOffset, yTexPos, 28, ySize);
         graphics.pose().pushPose();
         graphics.pose().translate(0.0F, 0.0F, 30.0F);
         graphics.renderItem(renderStack, x + 6, getY() + 8);
         graphics.renderItemDecorations(minecraft.font, renderStack, x + 6, getY() + 8, null);
         graphics.pose().popPose();
         Component message = getMessage();
         if (isHovered && !message.getString().isEmpty()) {
            drawHoveringText(graphics, message, mouseX, mouseY);
         }
      }
   }

   @Override
   public boolean isFocused() { return active || super.isFocused() || isHovered; }

   @Override
   public void onClick(double mouseX, double mouseY) { onTabClicked(); }

   @Override
   public void onPress() { }

   protected void drawHoveringText(@Nonnull GuiGraphics graphics, @Nonnull Component message, int x, int y) {
      y -= 12;
      RenderSystem.disableDepthTest();
      int k = minecraft.font.width(message);
      int i1 = 8;

      graphics.pose().pushPose();
      graphics.pose().translate(0.0F, 0.0F, 300.0F);
      int color = new Color(0xF0100010).getRGB();
      graphics.fillGradient(x - 3, y - 4, x + k + 3, y - 3, color, color);
      graphics.fillGradient(x - 3, y + i1 + 3, x + k + 3, y + i1 + 4, color, color);
      graphics.fillGradient(x - 3, y - 3, x + k + 3, y + i1 + 3, color, color);
      graphics.fillGradient(x - 4, y - 3, x - 3, y + i1 + 3, color, color);
      graphics.fillGradient(x + k + 3, y - 3, x + k + 4, y + i1 + 3, color, color);
      color = new Color(0x505000FF).getRGB();
      int nextColor = (color & new Color(0xFEFEFE).getRGB()) >> 1 | (color & new Color(0xFF000000).getRGB());
      graphics.fillGradient(x - 3, y - 3 + 1, x - 3 + 1, y + i1 + 3 - 1, color, nextColor);
      graphics.fillGradient(x + k + 2, y - 3 + 1, x + k + 3, y + i1 + 3 - 1, color, nextColor);
      graphics.fillGradient(x - 3, y - 3, x + k + 3, y - 3 + 1, color, color);
      graphics.fillGradient(x - 3, y + i1 + 2, x + k + 3, y + i1 + 3, nextColor, nextColor);

      graphics.drawString(minecraft.font, message, x, y, -1);
      graphics.pose().popPose();
      RenderSystem.enableDepthTest();
   }

   public abstract void onTabClicked();

}
