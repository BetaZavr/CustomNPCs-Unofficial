package noppes.npcs.client.gui.select;

import com.mojang.blaze3d.systems.RenderSystem;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.shared.common.util.LogWriter;

public class SubGuiColorSelector
        extends GuiBasic
        implements ITextfieldListener, ISliderListener {

   protected static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID , "textures/gui/color.png");

   protected final BufferedImage bufferedimage;
   protected int colorX;
   protected int colorY;
   protected GuiTextFieldNop textfield;
   protected ColorCallback callback;
   public int color;

   // New from Unofficial Betazavr
   protected boolean hoverTexture;
   protected boolean hasAlpha = false;
   protected float alpha = 1.0f;
   protected int offsetX = 0;
   protected int offsetY = 0;
   protected GuiSliderNop alphaSlider;
   protected GuiTextFieldNop alphaField;
   public Object object;

   public SubGuiColorSelector(int colorIn) {
      super();
      imageWidth = 176;
      imageHeight = 222;
      color = colorIn;
      setBackground("smallbg.png");

      if (minecraft == null) { minecraft = Minecraft.getInstance(); }

      InputStream stream = null;
      BufferedImage buffer = null;
      Resource iresource = minecraft.getResourceManager().getResource(resource).orElse(null);
      if (iresource != null) {
         try { buffer = ImageIO.read(stream = iresource.open()); }
         catch (IOException e) { LogWriter.error("Error:", e); }
         finally {
            if (stream != null) {
               try {
                  stream.close();
               } catch (IOException ignored) {
               }
            }
         }
      }
      bufferedimage = buffer;
   }

   public SubGuiColorSelector(int colorIn, ColorCallback callbackIn) {
      this(colorIn);
      callback = callbackIn;
   }

   @Override
   public boolean charTyped(char c, int i) {
      String prev = textfield.getValue();
      boolean bo = super.charTyped(c, i);
      String newText = textfield.getValue();
      if (!newText.equals(prev)) {
         try { setColor(Integer.parseInt(newText, 16)); }
         catch (NumberFormatException e) { textfield.setValue(prev); }
      }
      return bo;
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      LogWriter.info("TEST: buttonID: "+button.id+"; "+button.getClass().getSimpleName());
      if (button.id == 66) { onClose(); }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      // background
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      graphics.blit(resource, colorX, colorY, 0, 0, 120, 120);
      hoverTexture = !(mouseX < (double) colorX) && !(mouseX > (double) (colorX + 117)) && !(mouseY < (double) colorY) && !(mouseY > (double) (colorY + 117));
      if (textfield == null) { return; }
      int x = textfield.getX() + textfield.getWidth() + 4;
      int y = textfield.getY();
      int c = new Color(0xFF808080).getRGB();
      graphics.fill(x - 1, y - 1, x + 41, y + 21, c);
      c = color;
      if (bufferedimage != null && hoverTexture) {
         try {
            c = new Color(bufferedimage.getRGB((int)(mouseX - (double)guiLeft - 30.0D) * 4, (int)(mouseY - (double)guiTop - 50.0D) * 4) & new Color(0xFFFFFF).getRGB()).getRGB();
            StringBuilder str = new StringBuilder(Integer.toHexString(c));
            while (str.length() < 6) { str.insert(0, "0"); }
            while (str.length() > 6) { str.deleteCharAt(0); }
            if (!textfield.isFocused()) { textfield.setValue(str.toString()); }
         }
         catch (Exception ignored) { }
      }
      else if (!textfield.isFocused()) { textfield.setValue(getColor()); }
      if (callback != null) {
         if (hasAlpha) { c = (int) (alpha * 255.0f) << 24 | c & 0x00FFFFFF; }
         callback.preColor(c);
      }
      float alpha = (float) (c >> 24 & 255) / 255.0F;
      if (alpha == 0.0f) { c += new Color(0xFF000000).getRGB(); }
      graphics.fill(x, y, x + 40, y + 20, c);
   }

   @Override
   public void init() {
      super.init();
      guiLeft += offsetX;
      guiTop += offsetY;
      colorX = guiLeft + 30;
      colorY = guiTop + 50;
      textfield = addTextField(0, guiLeft + 31, guiTop + 20, 70, 20, getColor())
              .setHoverTexts("color.hover")
              .setColor(color)
              .setIsFocused(true)
              .setMaxStringLength(hasAlpha ? 8 : 6);
      addButton(66, guiLeft + 112, guiTop + 198, "gui.done")
              .setSize(60, 20)
              .setHoverTexts("hover.back");
      if (hasAlpha) {
         alpha = FastColor.ARGB32.alpha(color) / 255.0f;
         alphaSlider = addSlider(0, guiLeft + 30, guiTop + 173, alpha)
                 .setSize(84, 14)
                 .setHoverTexts("color.alpha");
         alphaField = addTextField(1, guiLeft + 117, guiTop + 170, 30, 20, "" + ((int) (alpha * 255.0f)))
                 .setMinMaxDefault(0, 255, ((int) (alpha * 255.0f)));
         alphaField.setHoverTexts("color.alpha");
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (bufferedimage != null && hoverTexture) {
         try {
            setColor(bufferedimage.getRGB((int)(mouseX - (double)guiLeft - 30.0D) * 4, (int)(mouseY - (double)guiTop - 50.0D) * 4) & new Color(0xFFFFFF).getRGB());
            return true;
         }
         catch (Exception ignored) { }
      }
      return super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   @Override
   public void unFocused(GuiTextFieldNop textfield) {
      if (textfield.id == 0) {
         try { setColor(Integer.parseInt(textfield.getValue(), 16)); }
         catch (NumberFormatException e) { textfield.setValue(getColor()); }
      }
      else if (textfield.id == 1) {
         alpha = textfield.getInteger() / 255.0f;
         color = textfield.getInteger() << 24 | color & 0x00FFFFFF;
         if (alphaSlider != null) { alphaSlider.setValue(alpha); }
      }
   }

   public String getColor() {
      StringBuilder str = new StringBuilder(Integer.toHexString(color));
      while (str.length() < (hasAlpha ? 8 : 6)) { str.insert(0, "0"); }
      while (str.length() > (hasAlpha ? 8 : 6)) { str.deleteCharAt(0); }
      return str.toString();
   }

   private void setColor(int colorIn) {
      color = colorIn;
      if (hasAlpha) { color = (int) (alpha * 255.0f) << 24 | color & 0x00FFFFFF; }
      textfield.setValue(getColor());
      if (callback != null) { callback.color(color); }
   }

   public interface ColorCallback {
      void color(int colorIn);
      void preColor(int colorIn);
   }

   // New from Unofficial Betazavr
   @Override
   public void mouseDragged(GuiSliderNop slider) {
      alpha = slider.getValue();
      color = (int) (alpha * 255.0f) << 24 | color & 0x00FFFFFF;
      if (alphaField != null) { alphaField.setValue("" + (int) (alpha * 255.0f)); }
   }

   @Override
   public void mousePressed(GuiSliderNop slider) { }

   @Override
   public void mouseReleased(GuiSliderNop slider) { }

   public SubGuiColorSelector setOffsetX(int posX) {
      offsetX = posX;
      return this;
   }

   public SubGuiColorSelector setOffsetY(int posY) {
      offsetY = posY;
      return this;
   }

   public SubGuiColorSelector setIsAlpha() {
      hasAlpha = true;
      return this;
   }

   public Object getObject() { return object; }

}
