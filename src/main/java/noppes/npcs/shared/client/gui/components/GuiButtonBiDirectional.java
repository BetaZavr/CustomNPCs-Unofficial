package noppes.npcs.shared.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.util.GuiTooltipUtils;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

import javax.annotation.Nonnull;
import java.util.Optional;

public class GuiButtonBiDirectional extends GuiButtonNop {

   public static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/arrowbuttons.png");
   protected boolean hoverL;
   protected boolean hoverR;

   public GuiButtonBiDirectional(IGuiInterface gui, int id, int x, int y, int variant, Object ... variants) {
      super(gui, id, x, y, variant, variants);
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!visible) { return; }
      if (offsetHoverX != 0 || offsetHoverY != 0) {
         mouseX -= offsetHoverX;
         mouseY -= offsetHoverY;
      }
      isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
      hoverL = isHovered && mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width / 2 && mouseY < getY() + height;
      hoverR = isHovered && !hoverL;
      PoseStack matrixStack = graphics.pose();

      matrixStack.pushPose();
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      RenderSystem.setShaderTexture(0, resource);
      matrixStack.translate(getX(), getY(), 0.0f);
      float scale = (float) height / 20.0f;
      matrixStack.scale(1.0f, scale, 1.0f);
      graphics.blit(resource, 0, 0, 0, hoverL ? 40 : 20, 11, 20);
      graphics.blit(resource, width - 11, 0, 11, (!isHovered || hoverL) && !hoverR ? 20 : 40, 11, 20);
      matrixStack.popPose();

      int color = 0xFF000000;
      graphics.hLine(getX() + 11, getX() + width - 12, getY(), color);
      graphics.hLine(getX() + 11, getX() + width - 12, getY() + getHeight() - 1, color);
      Component mes = getMessage();
      if (isHovered) {
         MutableComponent tempMes = MutableComponent.create(mes.getContents()).withStyle(ChatFormatting.UNDERLINE);
         for (Component c : mes.getSiblings()) { tempMes.append(c); }
         mes = tempMes;
      }
      renderString(graphics, mes, getX() + 11, getY(), getX() + getWidth() - 11, getY() + getHeight(),
              getFGColor() | Mth.ceil(alpha * 255.0F) << 24, showShadow, alignText, false, customFont);
      if (isHovered && !hoverText.isEmpty()) {
         if (listener != null) { listener.setHoverText(hoverText); }
         else { GuiTooltipUtils.renderTooltip(graphics, Minecraft.getInstance().font, hoverText, Optional.empty(), mouseX, mouseY); }
      }
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (active && visible) {
         if (clicked(mouseX, mouseY)) {
            if (isValidClickButton(mouseButton) && display != null) {
               if (hoverR) {
                  displayValue++;
                  if (displayValue >= display.length) { displayValue = 0; }
               }
               if (hoverL) {
                  --displayValue;
                  if (displayValue < 0) { displayValue = display.length - 1; }
               }
               setDisplay(displayValue);
            }
            if (listener != null && !listener.hasSubGui() && listener.mouseButtonEvent(this, mouseButton) ) {
               playDownSound(Minecraft.getInstance().getSoundManager());
               return true;
            }
            if (isValidClickButton(mouseButton)) {
               playDownSound(Minecraft.getInstance().getSoundManager());
               onClick(mouseX, mouseY);
            }
            return true;
         }
      }
      return false;
   }

}
