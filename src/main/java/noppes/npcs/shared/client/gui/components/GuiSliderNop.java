package noppes.npcs.shared.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GuiSliderNop extends Gui implements IComponentGui {

    protected final IGuiInterface listener;
    public int id;
    public float sliderValue;
    public float startValue;

    // New from Unofficial (BetaZavr)
    protected List<Component> hoverText = new ArrayList<>();
    protected ClientProxy.FontContainer customFont = null;
    protected boolean showShadow = true;
    protected boolean enabled = true;
    public boolean isDrag = false;
    public boolean isVertical = false;

    // standard
    protected Component message;
    protected boolean isHovered = false;
    protected boolean focused = false;
    protected boolean visible = true;
    protected int x;
    protected int y;
    protected int width = 150;
    protected int height = 20;
    public int packedFGColor;

    public GuiSliderNop(IGuiInterface parent, int idIn, int xIn, int yIn, float sliderValueIn) {
        id = idIn;
        x = xIn;
        y = yIn;
        message = Component.empty();
        sliderValue = sliderValueIn;
        startValue = sliderValueIn;
        listener = parent;
        if (listener instanceof ISliderListener) { ((ISliderListener) listener).mouseDragged(this); }
        packedFGColor = CustomNpcs.MainColor.getRGB();
    }

    public GuiSliderNop setString(Object str) {
        setMessage(Component.translatable(str == null ? "" : str.toString()));
        return this;
    }

    public Component getMessage() { return message; }

    public void setMessage(@Nonnull Component label) { message = label; }

    public void setSliderValue(float value) {
        value = ValueUtil.correctFloat(value, 0.0f, 1.0f);
        if (value != sliderValue) {
            sliderValue = value;
            if (listener instanceof ISliderListener) { ((ISliderListener) listener).mouseDragged(this); }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            renderWidget(mouseX, mouseY, partialTicks);
            if (isHovered && !hoverText.isEmpty() && listener != null) { listener.setHoverText(hoverText); }
            if (!isHovered && isDrag && !Mouse.isButtonDown(0)) { isDrag = false; }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean isFocused() { return focused; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
        if (visible && enabled && isHovered && scrolled != 0.0d) {
            setSliderValue(sliderValue + (scrolled > 0.0d ? 0.05f : -0.05f));
            return true;
        }
        return false;
    }

    @Override
    public int[] getCenter() { return new int[] { getX() + width / 2, getY() + height / 2}; }

    @Override
    public List<Component> getHoversText() { return hoverText; }

    @Override
    public int getId() { return id; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public boolean isVisible() { return visible; }

    @Override
    public void moveTo(int addX, int addY) {
        x += addX;
        y += addY;
    }

    public void renderWidget(int mouseX, int mouseY, float partialTicks) {
        isHovered = visible && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        if (!visible) { return; }
        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        Minecraft.getMinecraft().getTextureManager().bindTexture(GuiButtonNop.WIDGETS_LOCATION);
        int h0 = height / 2;
        int h1 = height - h0;
        int x = getX();
        int y = getY();
        int wp = width / 2;
        // background
        drawTexturedModalRect(x, y, 0, 46, wp, h0); // left up
        drawTexturedModalRect(x, y + h0, 0, 66 - h1, wp, h1); // left down
        drawTexturedModalRect(x + wp, y, 200 - wp, 46, wp, h0); // right up
        drawTexturedModalRect(x + wp, y + h0, 200 - wp, 66 - h1, wp, h1); // right down
        // scroll
        drawDefaultBackground();
        GuiButtonNop.renderString(getMessage(), getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                packedFGColor | 255 << 24, showShadow, true, customFont);
        if (enabled && isFocused()) {
            drawHorizontalLine(x, x + width - 1, y, 0xFFFFFFFF);
            drawHorizontalLine(x, x + width - 1, y + height - 1, 0xFFFFFFFF);
            drawVerticalLine(x, y, y + height - 1, 0xFFFFFFFF);
            drawVerticalLine(x + width - 1, y, y + height - 1, 0xFFFFFFFF);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    @Override
    public GuiSliderNop setCustomFont(ClientProxy.FontContainer font) {
        customFont = font;
        return this;
    }

    public void onClick(double mouseX, double mouseY) {
        setSliderValue((float)(mouseX - (double)(getX() + 4)) / (float)(width - 8));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
        if (mouseButton == 0 && (isHovered || isDrag)) {
            isDrag = true;
            onDrag(mouseX, mouseY, dx, dy);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    protected void onDrag(double mouseX, double mouseY, double dx, double dy) {
        setSliderValue((float) (mouseX - (double)(getX() + 4)) / (float)(width - 8));
    }

    public void onRelease(double x, double y) {
        if (sliderValue != startValue) {
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (listener instanceof ISliderListener) { ((ISliderListener) listener).mouseReleased(this); }
            startValue = sliderValue;
        }
    }

    public void drawDefaultBackground() {
        if (visible) {
            GlStateManager.pushMatrix();
            GlStateManager.enableDepth();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            Minecraft mc = Minecraft.getMinecraft();
            mc.getTextureManager().bindTexture(GuiButtonNop.WIDGETS_LOCATION);
            int lvt_4_1_ = (isHovered ? 2 : 1) * 20;
            int x = getX() + (int)((double)sliderValue * (double)(getWidth() - 8));
            int y = getY();
            int h0 = getHeight() / 2;
            int h1 = getHeight() - h0;
            // left top side
            drawTexturedModalRect(x, y, 0, 46 + lvt_4_1_, 4, h0);
            // left bottom side
            drawTexturedModalRect(x, y + h0, 0, 66 + lvt_4_1_ - h1, 4, h1);
            // right top side
            drawTexturedModalRect(x + 4, y, 196, 46 + lvt_4_1_, 4, h0);
            // right bottom side
            drawTexturedModalRect(x + 4, y + h0, 196, 66 + lvt_4_1_ - h1, 4, h1);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public GuiSliderNop setIsVisible(boolean show) {
        visible = show;
        return this;
    }

    @Override
    public GuiSliderNop setIsFocused(boolean isFocused) {
        focused = isFocused;
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
        if (mouseButton == 0) {
            onRelease(mouseX, mouseY);
            return sliderValue != startValue;
        }
        return false;
    }

    @Override
    public boolean keyPressed(char typedChar, int keyCode) { return false; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (enabled && visible && isHovered) {
            focused = true;
            onClick(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public void tick() { }

    @Override
    public boolean isHovered() { return isHovered; }

    public int getX() { return x; }

    public void setX(int xIn) { x = xIn; }

    public int getY() { return y; }

    public void setY(int yIn) { y = yIn; }

    public int getHeight() { return height; }

    public void setHeight(int heightIn) { height = heightIn; }

    public int getWidth() { return width; }

    public void setWidth(int widthIn) { width = widthIn; }

}
