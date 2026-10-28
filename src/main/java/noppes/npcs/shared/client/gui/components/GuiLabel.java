package noppes.npcs.shared.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.util.EnumAlignText;
import noppes.npcs.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GuiLabel extends AbstractWidget implements GuiEventListener, IComponentGui {

   protected int textColor = CustomNpcResourceListener.DefaultTextColor;
   public EnumAlignText alignText = EnumAlignText.LEFT;
   protected boolean labelBgEnabled;
   protected int ulColor;
   protected int brColor;
   protected int border;

   public boolean enabled = true;
   public int id;

   // New from Unofficial (BetaZavr)
   protected List<Component> hoverText = new ArrayList<>();
   protected ClientProxy.FontContainer customFont = null;
   protected int backColor = 0;
   protected int borderColor = 0;
   protected long lastClicked = 0L;
   public final IGuiInterface listener;
   public boolean showShadow = false;
   public int offsetHoverX = 0;
   public int offsetHoverY = 0;

   public GuiLabel(IGuiInterface gui, int idIn, Object label, int x, int y) {
      super(x, y, 0, 0, Component.empty());
      id = idIn;
      listener = gui;
      setMessage(label);
   }

   @Override
   public GuiLabel setSize(int widthIn, int ignoredHeight) {
      if (widthIn < 0) { widthIn *= -1; }
      setWidth(widthIn);
      setHeight((customFont != null ? customFont.getHeight() : Minecraft.getInstance().font.lineHeight) + 2);
      return this;
   }

   public GuiLabel setTooltip(Object tooltip) {
      if (tooltip == null) { setTooltip(null); }
      else {
         MutableComponent hover = tooltip instanceof MutableComponent comp ? comp :
                 tooltip instanceof Component comp ? Component.literal(comp.getString()) : Component.translatable(tooltip.toString());
         setTooltip(Tooltip.create(hover.setStyle(Style.EMPTY.withColor(0xFFC65C))));
      }
      return this;
   }

   public GuiLabel setColor(int color) {
      textColor = color;
      return this;
   }

   public GuiLabel setBackColor(int color) {
      backColor = color;
      return this;
   }

   public GuiLabel setBorderColor(int color) {
      borderColor = color;
      return this;
   }

   public GuiLabel setAlignCenter() {
      alignText = EnumAlignText.CENTER;
      return this;
   }

   public GuiLabel setAlignLeft() {
      alignText = EnumAlignText.LEFT;
      return this;
   }

   @SuppressWarnings("unused")
   public GuiLabel setAlignRight() {
      alignText = EnumAlignText.RIGHT;
      return this;
   }

   public GuiLabel setCenter(int widthIn) {
      setX(getX() + (widthIn - width) / 2);
      return this;
   }

   public boolean isFocused() { return false; }

   @Override
   public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!enabled || !visible) { return; }
      if (height <= 0) { setHeight(Minecraft.getInstance().font.lineHeight + 1); }
      super.render(graphics, mouseX, mouseY, partialTicks);
      if (isHovered && !hoverText.isEmpty()) { listener.setHoverText(hoverText); }
   }

   @Override
   protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (offsetHoverX != 0 || offsetHoverY != 0) {
         mouseX -= offsetHoverX;
         mouseY -= offsetHoverY;
      }
      isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
      drawBox(graphics);

      GuiButtonNop.renderString(graphics, getMessage(), getX(), getY(), getX() + width, getY() + height,
              textColor, showShadow, alignText, false, customFont);
   }

   @Override
   public int[] getCenter() { return new int[] { getX() + width / 2, getY() + height / 2}; }

   @Override
   public List<Component> getHoversText() { return hoverText; }

   @Override
   protected void updateWidgetNarration(@Nonnull NarrationElementOutput narrationElementOutput) { }

   protected void drawBox(GuiGraphics graphics) {
      if (!labelBgEnabled) { return; }
      int i = width + border * 2;
      int j = height + border * 2;
      int k = getX() - border;
      int l = getY() - border;
      graphics.fill(k, l, k + i, l + j, backColor);
      graphics.hLine(k, k + i, l, ulColor);
      graphics.hLine(k, k + i, l + j, brColor);
      graphics.hLine(k, l, l + j, ulColor);
      graphics.hLine(k + i, l, l + j, brColor);
      if (borderColor != 0) {
         graphics.fill(getX() - 2, getY() - 1, getX() + width + 2, getY() + height, borderColor);
      }
      if (backColor != 0) {
         graphics.fill(getX() - 1, getY(), getX() + width + 1, getY() + height - 1, backColor);
      }
   }

   // New from Unofficial (BetaZavr)
   @Override
   public GuiLabel setIsVisible(boolean show) {
      enabled = show;
      return this;
   }

   @Override
   public GuiLabel setIsFocused(boolean isFocused) { return this; }

   @Override
   public GuiComponentType getElementType() { return GuiComponentType.LABEL; }

   public void offsetHover(int x, int y) {
      offsetHoverX = x;
      offsetHoverY = y;
   }

   @Override
   public void moveTo(int addX, int addY) {
      setX(getX() + addX);
      setY(getY() + addY);
   }

   @Override
   public GuiLabel setHoverTexts(Object... components) {
      hoverText.clear();
      if (components == null) { return this; }
      Util.instance.putHovers(hoverText, components);
      return this;
   }

   @Override
   public GuiLabel setIsEnabled(boolean isEnabled) {
      enabled = isEnabled;
      return this;
   }

   @Override
   public GuiLabel setCustomFont(ClientProxy.FontContainer font) {
      customFont = font;
      return this;
   }

   @Override
    public int getId() { return id; }

   @Override
   public boolean isEnabled() { return enabled; }

   @Override
   public boolean isVisible() { return enabled && visible; }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
      super.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
      return false;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (isHovered && visible) {
         if (lastClicked + 500L > System.currentTimeMillis()) {
            lastClicked = 0L;
            return listener.doubleClicked(this);
         }
         else { lastClicked = System.currentTimeMillis(); }
      }
      return super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
      super.mouseReleased(mouseX, mouseY, mouseButton);
      return false;
   }

   public void setMessage(Object label) {
      setMessage(label == null ? Component.empty() :
              label instanceof Component comp ? comp : Component.translatable(label.toString()));
   }

   @Override
   public void setMessage(@Nonnull Component label) {
      super.setMessage(label);
      setWidth(Minecraft.getInstance().font.width(getMessage().getString()) + 3);
      setHeight(0);
   }

   @Override
   public void setHeight(int value) {
      height = Minecraft.getInstance().font.lineHeight + 1;
   }

}
