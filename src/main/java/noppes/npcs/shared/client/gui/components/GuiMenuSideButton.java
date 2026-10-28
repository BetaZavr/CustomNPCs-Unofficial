package noppes.npcs.shared.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.util.EnumAlignText;

import javax.annotation.Nonnull;

public class GuiMenuSideButton extends GuiButtonNop {

   public static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/menusidebutton.png");

   // New from Unofficial (BetaZavr)
   protected boolean isRight = false;
   public int data;

   public GuiMenuSideButton(IGuiInterface gui, int id, Object label, int x, int y) {
      super(gui, id, label, x, y, clicked);
      setSize(70, 22);
      alignText = EnumAlignText.CENTER;
   }

   @Override
   public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (!visible) { return; }
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int w = width + (active ? isRight ? -2 : 2 : 0);
      isHovered = mouseX >= getX() - (isRight ? 0 : width) && mouseY >= getY() && mouseX < getX() + w - (isRight ? 0 : width) && mouseY < getY() + height;
      int state = active ? 0 : (isHovered ? 2 : 1) * 22;
      int h0 = height / 2;
      int h1 = height - h0;
      // background
      if (isRight) {
         state += 66;
         int i = 197 - width;
         graphics.blit(resource, getX(), getY(), i, state, width, h0);
         graphics.blit(resource, getX(), getY() + h0, i, (22 - h1) + state, width, h1);
         if (active || isHovered) {
            graphics.blit(resource, getX() - 1, getY() + 1, i - 1, 1 + state, 1, h0 - 1);
            graphics.blit(resource, getX() - 2, getY() + 2, i - 2, 2 + state, 1, h0 - 2);
            graphics.blit(resource, getX() - 3, getY() + 3, i - 3, 3 + state, 1, h0 - 3);
            graphics.blit(resource, getX() - 1, getY() + h0, i - 1, 22 + state - h1, 1, h1 - 1);
            graphics.blit(resource, getX() - 2, getY() + h0, i - 2, 22 + state - h1, 1, h1 - 2);
            graphics.blit(resource, getX() - 3, getY() + h0, i - 3, 22 + state - h1, 1, h1 - 3);
         }
      }
      else {
         graphics.blit(resource, getX() - width, getY(), 0, state, width, h0);
         graphics.blit(resource, getX() - width, getY() + h0, 0, (22 - h1) + state, width, h1);
         if (active || isHovered) {
            graphics.blit(resource, getX(), getY() + 1, 194, 1 + state, 2, h0 - 1);
            graphics.blit(resource, getX() + 2, getY() + 2, 196, 2 + state, 1, h0 - 2);
            graphics.blit(resource, getX(), getY() + h0, 194, (22 - h1) + state, 2, h1 - 1);
            graphics.blit(resource, getX() + 2, getY() + h0, 196, (22 - h1) + state, 2, h1 - 2);
         }
      }
      // Title
      renderString(graphics, getMessage(), getX() + (isRight ? 0 : 5 - width), getY(), getX() + (isRight ? width - 4 : - 4), getY() + height,
              getFGColor() | Mth.ceil(alpha * 255.0F) << 24, showShadow, alignText, false, customFont);
   }

   // New from Unofficial (BetaZavr)
   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (visible && isHovered  && isValidClickButton(mouseButton)) {
         if (listener == null || !listener.hasSubGui()) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            onPress();
         }
         return true;
      }
      return false;
   }

   @Override
   public int getFGColor() {
      if (packedFGColor != -1) { return packedFGColor; }
      else if (isHovered) { return CustomNpcs.HoverColor.getRGB(); }
      return CustomNpcs.ButtonColor.getRGB();
   }

   @Override
   public GuiComponentType getElementType() { return GuiComponentType.SIDE_BUTTON; }

   public GuiMenuSideButton setIsRight(boolean right) {
      isRight = right;
      return this;
   }

}
