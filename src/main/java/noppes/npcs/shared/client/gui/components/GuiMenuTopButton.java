package noppes.npcs.shared.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.mixin.client.IMouseHandlerMixin;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.util.EnumAlignText;

import javax.annotation.Nonnull;

public class GuiMenuTopButton extends GuiButtonNop {

   public static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/menutopbutton.png");
   public boolean rotated = false;

   public GuiMenuTopButton(IGuiInterface gui, int id, Object label, int x, int y) {
      super(gui, id, label, x, y, null);
      setFocused(false);
      height = 20;
      width = Minecraft.getInstance().font.width(getMessage()) + 12;
      alignText = EnumAlignText.CENTER;
   }

   @Override
   public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (visible) {
         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShaderTexture(0, texture != null ? texture : resource);
         int h = height - (isFocused() ? 0 : 2);
         isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + h;
         if (render != null) { render.onRender(this, graphics, mouseX, mouseY, partialTicks); }
         else {
            if (rotated) { matrixStack.mulPose(Axis.XP.rotationDegrees(90.0F)); }
            int x = getX();
            int y = getY();
            int state = getState();
            if (hasDefBack) {
               graphics.fill(x - 1, y - 1, x + width + 1, y + h + 1, 0xFF202020);
               graphics.fill(x, y, x + width, y + h, 0xFFA0A0A0);
            }
            if (layerColor != 0) {
               RenderSystem.setShaderColor((float) FastColor.ARGB32.red(layerColor) / 255.0f,
                       (float) FastColor.ARGB32.green(layerColor) / 255.0f,
                       (float) FastColor.ARGB32.blue(layerColor) / 255.0f,
                       (float) FastColor.ARGB32.alpha(layerColor) / 255.0f);
            }
            if (texture == null) {
               graphics.blit(resource, getX(), getY(), 0, state * 20, getWidth() / 2, h);
               graphics.blit(resource, getX() + getWidth() / 2, getY(), 200 - getWidth() / 2, state * 20, getWidth() / 2, h);
            }
            else {
               matrixStack.pushPose();
               if (isSimple) {
                  int w0 = width / 2;
                  int w1 = width - w0;
                  graphics.blit(texture, x, y, txrX, txrY + state * h, w0, h);
                  graphics.blit(texture, x + w0, y, txrX + txrW - w0, txrY + state * h, w1, h);
               }
               else {
                  boolean isPrefabricated = txrW == 0;
                  int tw = isPrefabricated ? 200 : txrW;
                  int th = txrH == 0 ? 20 : txrH;
                  float scaleH = (float) h / (float) th;
                  float scaleW = isPrefabricated ? scaleH : width / (float) tw;
                  matrixStack.translate(x, y, 0.0f);
                  matrixStack.scale(scaleW, scaleH, 1.0f);
                  if (isPrefabricated) {
                     tw = (int) (((float) width / 2.0f) / scaleH);
                     graphics.blit(texture, 0, 0, txrX, txrY + state * th, tw, th);
                     graphics.blit(texture, tw, 0, txrX + 200 - tw, txrY + state * th, tw, th);
                  } else {
                     graphics.blit(texture, 0, 0, txrX, txrY + state * th, tw, th);
                  }
               }
               matrixStack.popPose();
            }
         }
         renderString(graphics, getMessage(), getX() + 2, getY(), getX() + getWidth() - 2, getY() + getHeight(),
                 getFGColor() | Mth.ceil(alpha * 255.0F) << 24, showShadow, alignText, false, customFont);
         matrixStack.popPose();
      }
   }

   @Override
   public int getState() {
      boolean lbm = ((IMouseHandlerMixin) Minecraft.getInstance().mouseHandler).getActiveButton() == 0;
      if (texture == null) {
         return isFocused() ? 0 : (isHovered && active ? 2 : 1);
      }
      if (isAnim) {
         return !active ? 1 : isHoveredOrFocused() && (listener == null || !listener.hasSubGui()) ? lbm ? 3 : 2 : !isFocused() ? 1 : 0;
      }
      if (isSimple) {
         int i = 0;
         if (!isFocused() || !active) { i = 2; }
         else if (isHoveredOrFocused() && (listener == null || !listener.hasSubGui())) { i = lbm ? 2 : 1; }
         return i;
      }
      if (isHoveredOrFocused() && (listener == null || !listener.hasSubGui())) {
         return (isFocused() ? 1 : 4) + (lbm ? 1 : 0);
      }
      return !active ? 1 : isFocused() ? 0 : 3;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) { return false; }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) { return false; }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      boolean bo = !isFocused() && visible && isHovered && active;
      if (isHovered && bo) {
         if (listener != null && listener.mouseButtonEvent(this, mouseButton)) { return true; }
         if (mouseButton == 0) { onClick(mouseX, mouseY); }
      }
      return bo;
   }

   @Override
   public void onClick(double x, double y) { listener.buttonEvent(this); }

   // New from Unofficial (BetaZavr)
   @Override
   public int getFGColor() {
      if (packedFGColor != -1) { return packedFGColor; }
      else if (isHovered) { return CustomNpcs.HoverColor.getRGB(); }
      return CustomNpcs.ButtonColor.getRGB();
   }

   @Override
   public GuiMenuTopButton setIsEnabled(boolean isEnabled) {
      active = isEnabled;
      return this;
   }

   @Override
   public GuiMenuTopButton setIsFocused(boolean isFocused) {
      setFocused(isFocused);
      return this;
   }

   @Override
   public GuiComponentType getElementType() { return GuiComponentType.TOP_BUTTON; }

}
