package noppes.npcs.shared.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GuiLabel extends Gui implements IComponentGui {

    protected int textColor = CustomNpcResourceListener.DefaultTextColor;
    protected boolean centered = false;
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
    public IGuiInterface listener;
    public boolean showShadow = false;
    public int offsetHoverX = 0;
    public int offsetHoverY = 0;

    // standard
    protected Component message = Component.empty();
    protected boolean isHovered = false;
    protected boolean visible = true;
    protected int x;
    protected int y;
    protected int width = 0;
    protected int height = 0;

    public GuiLabel(IGuiInterface gui, int idIn, Object label, int xIn, int yIn) {
        id = idIn;
        x = xIn;
        y = yIn;
        listener = gui;
        setMessage(label);
    }

    @Override
    public GuiLabel setSize(int widthIn, int ignoredHeight) {
        if (widthIn < 0) { widthIn *= -1; }
        setWidth(widthIn);
        setHeight(Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 1);
        return this;
    }


    public GuiLabel setColor(TextFormatting color) {
        textColor = Util.instance.getColorI(color.getColorIndex());
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

    public GuiLabel setCentered(boolean bo) {
        centered = bo;
        return this;
    }

    public GuiLabel setCenter(int widthIn) {
        setX(getX() + (widthIn - width) / 2);
        return this;
    }

    public boolean isFocused() { return false; }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (enabled && visible) {
            if (height <= 0) { setHeight(0); }
            renderWidget(mouseX, mouseY, partialTicks);
            if (isHovered && !hoverText.isEmpty() && listener != null) { listener.setHoverText(hoverText); }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public void renderWidget(int mouseX, int mouseY, float partialTicks) {
        if (offsetHoverX != 0 || offsetHoverY != 0) {
            mouseX -= offsetHoverX;
            mouseY -= offsetHoverY;
        }
        isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;

        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        drawBox();
        GuiButtonNop.renderString(getMessage(), getX(), getY(), getX() + width, getY() + height,
                textColor | 0xFF000000, showShadow, centered, customFont);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    @Override
    public int[] getCenter() { return new int[] { getX() + width / 2, getY() + height / 2}; }

    @Override
    public List<Component> getHoversText() { return hoverText; }

    protected void drawBox() {
        if (!labelBgEnabled) { return; }
        int i = width + border * 2;
        int j = height + border * 2;
        int k = getX() - border;
        int l = getY() - border;
        drawRect(k, l, k + i, l + j, backColor);
        drawHorizontalLine(k, k + i, l, ulColor);
        drawHorizontalLine(k, k + i, l + j, brColor);
        drawHorizontalLine(k, l, l + j, ulColor);
        drawHorizontalLine(k + i, l, l + j, brColor);
        if (borderColor != 0) { drawRect(getX() - 2, getY() - 1, getX() + width + 2, getY() + height, borderColor); }
        if (backColor != 0) { drawRect(getX() - 1, getY(), getX() + width + 1, getY() + height - 1, backColor); }
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

    @SuppressWarnings("unused")
    public void offsetHover(int x, int y) {
        offsetHoverX = x;
        offsetHoverY = y;
    }

    @Override
    public void moveTo(int addX, int addY) {
        x += addX;
        y += addY;
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
    public int getId() { return id; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public boolean isVisible() { return visible; }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) { return false; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isHovered && visible) {
            if (lastClicked + 500L > System.currentTimeMillis()) {
                lastClicked = 0L;
                return listener.doubleClicked(this);
            }
            else { lastClicked = System.currentTimeMillis(); }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) { return false; }

    @Override
    public boolean keyPressed(char typedChar, int keyCode) { return false; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) { return false; }

    @Override
    public void tick() { }

    @Override
    public boolean isHovered() { return isHovered; }

    @Override
    public GuiLabel setCustomFont(ClientProxy.FontContainer font) {
        customFont = font;
        return this;
    }

    public int getX() { return x; }

    public void setX(int xIn) { x = xIn; }

    public int getY() { return y; }

    public void setY(int yIn) { y = yIn; }

    public int getHeight() { return height; }

    public void setHeight(int ignoredHeightIn) { height = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 1; }

    public int getWidth() { return width; }

    public void setWidth(int widthIn) { width = widthIn; }

    public Component getMessage() { return message; }

    public void setMessage(Object label) {
        setMessage(label == null ? Component.empty() :
                label instanceof Component ? (Component) label :
                        label instanceof ITextComponent ? new Component((ITextComponent) label) :
                                Component.translatable(label.toString()));
    }

    public void setMessage(@Nonnull Component label) {
        message = label;
        setWidth(Minecraft.getMinecraft().fontRenderer.getStringWidth(message.getFormattedText()) + 3);
        setHeight(0);
    }

}
