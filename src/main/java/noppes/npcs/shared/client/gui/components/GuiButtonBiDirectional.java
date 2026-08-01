package noppes.npcs.shared.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

import javax.annotation.Nonnull;

public class GuiButtonBiDirectional extends GuiButtonNop {

   public static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/arrowbuttons.png");
   protected boolean hoverL;
   protected boolean hoverR;

   public GuiButtonBiDirectional(IGuiInterface gui, int id, int x, int y, int variant, Object ... variants) {
      super(gui, id, x, y, variant, variants);
   }

   @Override
   public void render(int mouseX, int mouseY, float partialTicks) {
      if (offsetHoverX != 0 || offsetHoverY != 0) {
         mouseX -= offsetHoverX;
         mouseY -= offsetHoverY;
      }
      isHovered = visible && mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
      if (!visible) { return; }
      hoverL = isHovered && mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width / 2 && mouseY < getY() + height;
      hoverR = isHovered && !hoverL;
      Minecraft mc = Minecraft.getMinecraft();

      GlStateManager.enableDepth();
      GlStateManager.enableBlend();
      GlStateManager.pushMatrix();
      GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
      GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
      GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
      mc.getTextureManager().bindTexture(resource);
      GlStateManager.translate(getX(), getY(), 0.0f);
      float scale = (float) height / 20.0f;
      GlStateManager.scale(1.0f, scale, 1.0f);
      drawTexturedModalRect(0, 0, 0, hoverL ? 40 : 20, 11, 20);
      drawTexturedModalRect(width - 11, 0, 11, (!isHovered || hoverL) && !hoverR ? 20 : 40, 11, 20);
      GlStateManager.popMatrix();

      int color = 0xFF000000;

      if (enabled && isFocused()) {
         drawHorizontalLine(x + 10, x + width - 11, y, 0xFFFFFFFF);
         drawHorizontalLine(x + 10, x + width - 11, y + height - 1, 0xFFFFFFFF);
         drawVerticalLine(x + 10, y, y + height - 1, 0xFFFFFFFF);
         drawVerticalLine(x + width - 11, y, y + height - 1, 0xFFFFFFFF);
      }
      else {
         drawHorizontalLine(getX() + 11, getX() + width - 12, getY(), color);
         drawHorizontalLine(getX() + 11, getX() + width - 12, getY() + getHeight() - 1, color);
      }
      @Nonnull Component label = getMessage();
      if (isHovered) {
         label = Component.literal(TextFormatting.UNDERLINE + label.getFormattedText());
      }
      renderString(label, x + 11, y, x + width - 11, y + height,
              getFGColor() | (int) Math.ceil(alpha * 255.0F) << 24, showShadow, true, customFont);
      if (isHovered && !hoverText.isEmpty()) {
         if (listener != null) { listener.setHoverText(hoverText); }
         else { drawHoveringText(hoverText, mouseX, mouseY, width, height); }
      }
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.disableBlend();
   }

   @Override
   protected void onClick(double x, double y) {
      if (display != null && display.length != 0) { setDisplay(hoverL ? displayValue - 1 : displayValue + 1); }
      if (hasSound) { Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)); }
      if (onPress != null) { onPress(); }
      else if (listener != null) { listener.buttonEvent(this); }
   }

   @Override
   protected boolean isValidClickButton(int mouseButton) { return mouseButton == 0; }

}
