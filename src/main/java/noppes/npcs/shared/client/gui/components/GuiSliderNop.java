package noppes.npcs.shared.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.mixin.client.IMouseHandlerMixin;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.util.EnumAlignText;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GuiSliderNop extends AbstractWidget implements IComponentGui {

   protected final IGuiInterface listener;
   public int id;
   /** 0.0 ... 1.0 */
   protected float sliderValue;
   protected float startValue;

   // New from Unofficial (BetaZavr)
   protected List<Component> hoverText = new ArrayList<>();
   protected ClientProxy.FontContainer customFont = null;
   protected boolean showShadow = true;
   protected boolean enabled = true;
   public boolean isDrag = false;
   public boolean isVertical;

   public GuiSliderNop(IGuiInterface parent, int idIn, int x, int y, float sliderValueIn) {
      super(x, y, 150, 20, Component.empty());
      id = idIn;
      isVertical = height > width;
      sliderValue = ValueUtil.correctFloat(sliderValueIn, 0.0f, 1.0f);
      startValue = sliderValue;
      listener = parent;
      visible = true;
      if (listener instanceof ISliderListener gui) { gui.mouseDragged(this); }
      packedFGColor = CustomNpcs.MainColor.getRGB();
   }

   public GuiSliderNop setString(Object str) {
      setMessage(Component.translatable(str == null ? "" : str.toString()));
      return this;
   }

   @SuppressWarnings("unused")
   public float getStartValue() { return startValue; }

   public float getValue() { return sliderValue; }

   public void setValue(float value) {
      value = ValueUtil.correctFloat(value, 0.0f, 1.0f);
      if (value != sliderValue) { sliderValue = value; }
   }

   protected void setSliderValue(float value) {
      value = ValueUtil.correctFloat(value, 0.0f, 1.0f);
      if (value != sliderValue) {
         sliderValue = value;
         if (listener instanceof ISliderListener parent) { parent.mouseDragged(this); }
      }
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!visible) { return; }
      super.render(graphics, mouseX, mouseY, partialTicks);
      if (isHovered && !hoverText.isEmpty() && listener != null) { listener.setHoverText(hoverText); }
      if (!isHovered && isDrag && ((IMouseHandlerMixin) Minecraft.getInstance().mouseHandler).getActiveButton() != 0) { isDrag = false; }
   }

   @Override
   public int[] getCenter() { return new int[] { getX() + width / 2, getY() + height / 2}; }

   @Override
   public List<Component> getHoversText() { return hoverText; }

   @Override
   public int getId() { return id; }

   @Override
   public boolean isEnabled() { return active; }

   @Override
   public boolean isVisible() { return visible; }

   @Override
   public void moveTo(int addX, int addY) {
      setX(getX() + addX);
      setY(getY() + addY);
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      int h0 = height / 2;
      int h1 = height - h0;
      int x = getX();
      int y = getY();
      int wp = width / 2;
      // background
      graphics.blit(WIDGETS_LOCATION, x, y, 0, 46, wp, h0); // left up
      graphics.blit(WIDGETS_LOCATION, x, y + h0, 0, 66 - h1, wp, h1); // left down
      graphics.blit(WIDGETS_LOCATION, x + wp, y, 200 - wp, 46, wp, h0); // right up
      graphics.blit(WIDGETS_LOCATION, x + wp, y + h0, 200 - wp, 66 - h1, wp, h1); // right down
      // scroll
      renderBg(graphics);
      GuiButtonNop.renderString(graphics, getMessage(), getX(), getY(), getX() + getWidth(), getY() + getHeight(),
              getFGColor() | Mth.ceil(alpha * 255.0F) << 24, showShadow, EnumAlignText.CENTER, false, customFont);
   }

   @Override
   public void onClick(double x, double y) {
      if (visible && active) {
         setSliderValue((float)(x - (double)(getX() + 4)) / (float)(width - 8));
         super.onClick(x, y);
      }
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
      if (isValidClickButton(mouseButton) && (isHovered || isDrag)) {
         isDrag = true;
         onDrag(mouseX, mouseY, dx, dy);
         return true;
      }
      return false;
   }

   @Override
   protected void onDrag(double mouseX, double mouseY, double dx, double dy) {
      setSliderValue((float) (mouseX - (double)(getX() + 4)) / (float)(width - 8));
   }

   @Override
   protected void updateWidgetNarration(@Nonnull NarrationElementOutput narrationElementOutput) {}

   @Override
   public void onRelease(double x, double y) {
      if (sliderValue != startValue) {
         super.playDownSound(Minecraft.getInstance().getSoundManager());
         if (listener instanceof ISliderListener parent) { parent.mouseReleased(this); }
         startValue = sliderValue;
      }
   }

   public void renderBg(GuiGraphics graphics) {
      if (visible) {
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
         int lvt_4_1_ = (isHovered || isFocused() ? 2 : 1) * 20;
         int x = getX() + (int)((double)sliderValue * (double)(getWidth() - 8));
         int y = getY();
         int h0 = getHeight() / 2;
         int h1 = getHeight() - h0;
         // left top side
         graphics.blit(WIDGETS_LOCATION, x, y, 0, 46 + lvt_4_1_, 4, h0);
         // left bottom side
         graphics.blit(WIDGETS_LOCATION, x, y + h0, 0, 66 + lvt_4_1_ - h1, 4, h1);
         // right top side
         graphics.blit(WIDGETS_LOCATION, x + 4, y, 196, 46 + lvt_4_1_, 4, h0);
         // right bottom side
         graphics.blit(WIDGETS_LOCATION, x + 4, y + h0, 196, 66 + lvt_4_1_ - h1, 4, h1);
      }
   }

   @Override
   public GuiSliderNop setCustomFont(ClientProxy.FontContainer font) {
      customFont = font;
      return this;
   }

   @Override
   public GuiSliderNop setIsVisible(boolean show) {
      visible = show;
      return this;
   }

   @Override
   public GuiSliderNop setIsFocused(boolean isFocused) {
      setFocused(isFocused);
      return this;
   }

   public GuiSliderNop setShowShadow(boolean show) {
      showShadow = show;
      return this;
   }

   // New from Unofficial (BetaZavr)
   @Override
   public GuiSliderNop setHoverTexts(Object... components) {
      hoverText.clear();
      if (components == null) { return this; }
      Util.instance.putHovers(hoverText, components);
      return this;
   }

   @Override
   public GuiSliderNop setIsEnabled(boolean isEnabled) {
      enabled = isEnabled;
      return this;
   }

   @Override
   public GuiSliderNop setSize(int widthIn, int heightIn) {
      width = widthIn;
      height = heightIn;
      return this;
   }

   @Override
   public GuiComponentType getElementType() { return GuiComponentType.SLIDER; }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
      if (isValidClickButton(mouseButton)) {
         onRelease(mouseX, mouseY);
         return sliderValue != startValue;
      }
      return false;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
      if (visible && enabled && isHovered && scrolled != 0.0d) {
         float step = (float) width / 100.0f;
         setSliderValue(sliderValue + (scrolled > 0.0d ? step : -step));
         return true;
      }
      return false;
   }

}
