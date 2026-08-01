package noppes.npcs.shared.client.gui.components;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.controllers.YDEController;
import noppes.npcs.client.gui.yellow_de.GuiYellowDialogEditor;
import noppes.npcs.client.gui.yellow_de.data.UtilYDE;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextChangeListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YDEScrollNop extends GuiBasic
        implements IComponentGui, ITextfieldListener, ITextChangeListener, ICustomScrollListener {

    public static final ResourceLocation resource = getResource("animation/yde_buttons.png");

    // scroll vars
    protected float scrollX = 0;
    protected float scrollY = 0;
    protected float maxScrollX;
    protected float maxScrollY;
    protected int tabHeight = 0;
    // bar
    protected boolean isScrollingX = false;
    protected boolean isScrollingY = false;
    // standard
    protected final GuiYellowDialogEditor listener;
    protected boolean isHovered = false;
    protected boolean focused = false;
    protected boolean enabled = true;
    protected boolean visible = true;
    // yde
    protected @Nonnull ClientProxy.FontContainer customFont = UtilYDE.FONT;
    public @Nonnull Component scrollSelect = Component.empty();
    public Availability availability;
    public IComponentGui select;
    public int tabId = -1;

    public YDEScrollNop(GuiYellowDialogEditor listenerIn, int xIn, int yIn, int widthIn, int heightIn) {
        super();
        guiLeft = xIn;
        guiTop = yIn;
        width = widthIn;
        height = heightIn;
        listener = listenerIn;
        initGui();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        labelList.clear();
        wrapper.onlyScroll = null;
        wrapper.components.clear();
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        if (isVisible()) { listener.mouseButtonEvent(this, button, 0); }
    }

    @Override
    public boolean mouseButtonEvent(GuiButtonNop button, int mouseButton) {
        if (isVisible()) { return listener.mouseButtonEvent(this, button, mouseButton); }
        return false;
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) { listener.unFocused(this, textField); }

    @Override
    public void textUpdate(IComponentGui component, String text) { listener.textUpdate(this, component, text); }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        isHovered = false;
        if (isVisible()) {
            isHovered = isMouseOver(mouseX, mouseY);
            // background
            GlStateManager.pushMatrix();
            int w = width + 1;
            int h = height + 1;
            GlStateManager.translate(guiLeft - 0.5f, guiTop - 0.5f, 0.0f);
            GlStateManager.scale(0.5f, 0.5f, 0.5f);
            w *= 2;
            h *= 2;
            int color = (isHovered ? YDEController.backHoverColor : YDEController.backColor) & 0xFFFFFF | 0x40000000;
            drawRect(0, tabHeight * 2, w, h, color);
            color = (isHovered ? YDEController.hoverLineColor : YDEController.componentLineColor) & 0xFFFFFF | 0xA0000000;
            drawHorizontalLine(0, w, 0, color);
            drawHorizontalLine(0, w, h, color);
            drawVerticalLine(0, 0, h, color);
            drawVerticalLine(w, 0, h, color);
            if (tabHeight > 0) {
                w -= 4;
                GlStateManager.translate(2.5f, (tabHeight - 4) * 2.0f + 0.5f, 0.0f);
                drawHorizontalLine(0, w, 0, color);
                drawHorizontalLine(0, w, 4, color);
            }
            GlStateManager.popMatrix();

            // draw scrolling
            double xPos = mouseX - guiLeft;
            double yPos = mouseY - guiTop;
            drawHorizontalScrollBar(isScrollingX ? 0.5f : xPos >= 1 && xPos < width - 11 && yPos >= height - 10 && yPos < height - 1 ? 0.75f : 1.0f);
            drawVerticalScrollBar(isScrollingY ? 0.5f : xPos >= width - 10 && xPos < width - 1 && yPos >= tabHeight && yPos < height + tabHeight - 10 ? 0.75f : 1.0f);

            GlStateManager.pushMatrix();
            int minX = guiLeft;
            int minY = guiTop + tabHeight;
            int maxX = minX + width - 11;
            int maxY = minY + height - tabHeight - 11;

            if (tabHeight > 0) {
                for (IComponentGui component : new ArrayList<>(wrapper.components)) {
                    if (component instanceof GuiMenuTopButton) {
                        component.render(mouseX, mouseY, partialTicks);
                    }
                }
            }
            float scale = 2.0f;
            if (listener.guiScale > 1.0f) { scale = 1.0f / listener.guiScale * 2.0f; }

            ScaledResolution sr = new ScaledResolution(minecraft);
            int factor = sr.getScaleFactor();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int) (minX * scale * factor), minecraft.displayHeight - (int) (maxY * scale * factor), (int) ((maxX - minX) * scale * factor), (int) ((maxY - minY) * scale * factor));

            GlStateManager.pushMatrix();
            wrapper.subgui = null;
            wrapper.mouseX = mouseX;
            wrapper.mouseY = mouseY;
            GlStateManager.translate(-scrollX, tabHeight + 1 - scrollY, 0.0f);
            int x, y, wComp, hComp;
            for (IComponentGui component : new ArrayList<>(wrapper.components)) {
                if (!(component instanceof GuiMenuTopButton)) {
                    int mX = (int) (mouseX + scrollX);
                    int mY = (int) (mouseY - tabHeight - 1 + scrollY);
                    x = component.getX();
                    y = component.getY();
                    wComp = component.getWidth();
                    hComp = component.getHeight();

                    if (mouseY < minY && mY > y - mouseY - minY) { mY = -1; }
                    if (mouseX < minX && mX > x - mouseX - minX) { mX = -1; }
                    if (mouseX > maxX && (x + wComp) > mX) { mX = -1; }
                    if (mouseY > maxY && (y + hComp) > mY) { mY = -1; }
                    component.render(mX, mY, partialTicks);
                }
            }
            GlStateManager.popMatrix();

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.popMatrix();

            if (maxScrollX != 0 && isScrollingX && !Mouse.isButtonDown(0)) { isScrollingX = false; }
            if (maxScrollY != 0 && isScrollingY && !Mouse.isButtonDown(0)) { isScrollingY = false; }
            if (!hoverText.isEmpty()) {
                listener.setHoverText(hoverText);
                hoverText.clear();
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
        if (visible && isHovered() && scrolled != 0.0D) {
            if (maxScrollX != 0 && (GuiScreen.isShiftKeyDown() ||
                    (maxScrollX != 0 && maxScrollY == 0) ||
                    (mouseX > guiLeft + 2 && mouseX < guiLeft + width - 12 &&
                            mouseY > guiTop + height - 10 && mouseY < guiTop + height - 1))) {
                float lineHeight = Math.max(10.0f, maxScrollX / 10.0f);
                scrollX = ValueUtil.correctFloat(scrollX + (scrolled > 0.0D ? -lineHeight : lineHeight), 0, maxScrollX);
                return true;
            }
            if (maxScrollY != 0) {
                float lineHeight = Math.max(10.0f, maxScrollY / 10.0f);
                scrollY = ValueUtil.correctFloat(scrollY + (scrolled > 0.0D ? -lineHeight : lineHeight), 0, maxScrollY);
                return true;
            }
        }
        return super.mouseScrolled(mouseX + scrollX, mouseY - tabHeight - 1 + scrollY, scrolled);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (!isVisible() || !isHovered) { return false; }
        double xPos = mouseX - guiLeft;
        double yPos = mouseY - guiTop;
        if (maxScrollX != 0) {
            isScrollingX = xPos >= 1 && xPos < width - 11 && yPos >= height - 10 && yPos < height - 1;
            if (isScrollingX) {
                int trackWidth = width - 14;
                float scrollRatio = scrollX / maxScrollX;
                int thumbWidth = Math.max(20, (int) (trackWidth - maxScrollX));
                int thumbX = (int) (scrollRatio * (trackWidth - thumbWidth));
                float f0 = guiLeft + 2.0f + thumbX;
                if (mouseX < f0) { scrollX = ValueUtil.correctFloat(scrollX - f0 + (float) mouseX, 0, maxScrollX); }
                float f1 = f0 + thumbWidth;
                if (mouseX > f1) { scrollX = ValueUtil.correctFloat(scrollX + (float) mouseX - f1, 0, maxScrollX); }
                return true;
            }
        }
        if (maxScrollY != 0) {
            isScrollingY = xPos >= width - 10 && xPos < width - 1 && yPos >= tabHeight && yPos < height - 10;
            if (isScrollingY) {
                int trackHeight = height - tabHeight - 11;
                float scrollRatio = scrollY / maxScrollY;
                int thumbHeight = Math.max(20, (int) (trackHeight - maxScrollY));
                int thumbY = (int) (scrollRatio * (trackHeight - thumbHeight));
                float f0 = guiTop + tabHeight + thumbY;
                if (mouseY < f0) { scrollY = ValueUtil.correctFloat(scrollY - f0 + (float) mouseY, 0, maxScrollY); }
                float f1 = f0 + thumbHeight;
                if (mouseY > f1) { scrollY = ValueUtil.correctFloat(scrollY + (float) mouseY - f1, 0, maxScrollY); }
                return true;
            }
        }
        if (tabHeight > 0) {
            for (IComponentGui component : new ArrayList<>(wrapper.components)) {
                if (component instanceof GuiMenuTopButton && component.mouseClicked(mouseX, mouseY, mouseButton)) { return true; }
            }
        }
        if (mouseX < guiLeft || mouseX > guiLeft + width - 11 ||
                mouseY < guiTop + tabHeight || mouseY > guiTop + height - 11) {
            return false;
        }
        return super.mouseClicked(mouseX + scrollX, mouseY - tabHeight - 1 + scrollY, mouseButton) || isVisible() && isHovered;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
        if (!isHovered) { return false; }
        if (isScrollingX && maxScrollX != 0) {
            scrollX = ValueUtil.correctFloat(scrollX + (float) dx, 0, maxScrollX);
            return true;
        }
        if (isScrollingY && maxScrollY != 0) {
            scrollY = ValueUtil.correctFloat(scrollY + (float) dy, 0, maxScrollY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return super.mouseReleased(mouseX + scrollX, mouseY - tabHeight - 1 + scrollY, mouseButton);
    }

    @Override
    public void add(IComponentGui element) {
        if (!(element instanceof GuiCustomWindowNop) &&
                !(element instanceof YDEScrollNop) &&
                !(element instanceof GuiMenuSideButton)) { super.add(element); }
    }

    @Override
    public GuiLabel addLabel(int id, int x, int y, Object label) {
        return super.addLabel(id, guiLeft + x, guiTop + y, label);
    }

    @Override
    public GuiButtonNop addButton(int id, int x, int y, Object label) {
        return super.addButton(id, guiLeft + x, guiTop + y, label);
    }

    @Override
    public GuiButtonNop addButton(int id, int x, int y, boolean isBiDirectional, int variant, Object... variants) {
        return super.addButton(id, guiLeft + x, guiTop + y, isBiDirectional, variant, variants);
    }

    @Override
    public GuiCheckBoxNop addCheckBox(int id, int x, int y, Object labelTrue, Object labelFalse, boolean selected) {
        return super.addCheckBox(id, guiLeft + x, guiTop + y, labelTrue, labelFalse, selected);
    }

    @Override
    public GuiMenuTopButton addTopButton(int id, int x, int y, Object label) {
        return super.addTopButton(id, guiLeft + x, guiTop + y, label);
    }

    @Override
    public GuiMenuTopIconButton addTopButton(int id, int x, int y, Object label, ItemStack stack) {
        return super.addTopButton(id, guiLeft + x, guiTop + y, label, stack);
    }

    @Override
    public GuiButtonYesNo addYesNo(int id, int x, int y, boolean isYes) {
        return super.addYesNo(id, guiLeft + x, guiTop + y, isYes);
    }

    @Override
    public GuiSliderNop addSlider(int id, int x, int y, float sliderValue) {
        return super.addSlider(id, guiLeft + x, guiTop + y, sliderValue);
    }

    @Override
    public GuiTextFieldNop addTextField(int id, int x, int y, int width, int height, Object value) {
        return super.addTextField(id, guiLeft + x, guiTop + y, width, height, value);
    }

    @Override
    public GuiMenuSideButton addSideButton(int id, int x, int y, Object label) { return null; }

    @Override
    public int[] getCenter() { return new int[] { guiLeft + width / 2, guiTop + (height - tabHeight) / 2}; }

    @Override
    public List<Component> getHoversText() { return new ArrayList<>(); }

    @Override
    public YDEScrollNop setCustomFont(ClientProxy.FontContainer font) {
        if (font != null) { customFont = font; }
        return this;
    }

    @Override
    public int getId() { return 0; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public boolean isVisible() { return visible && !wrapper.components.isEmpty(); }

    public boolean isHovered() { return isVisible() && isHovered; }

    @Override
    public void moveTo(int addX, int addY) {
        guiLeft += addX;
        guiTop += addY;
        for (IComponentGui component : new ArrayList<>(wrapper.components)) {
            component.moveTo(addX, addY);
        }
    }

    @Override
    public YDEScrollNop setHoverTexts(Object... components) { return this; }

    @Override
    public YDEScrollNop setIsEnabled(boolean isEnabled) {
        enabled = isEnabled;
        return this;
    }

    @Override
    public YDEScrollNop setIsVisible(boolean isVisible) {
        visible = isVisible;
        return this;
    }

    @Override
    public YDEScrollNop setIsFocused(boolean isFocused) {
        focused = isFocused;
        return this;
    }

    @Override
    public YDEScrollNop setSize(int widthIn, int heightIn) {
        height = heightIn;
        width = widthIn;
        scrollX = Math.min(scrollX, maxScrollX);
        scrollY = Math.min(scrollY, maxScrollY);
        return this;
    }

    @Override
    public GuiComponentType getElementType() { return GuiComponentType.SCROLL; }

    public boolean isMouseOver(int xPos, int yPos) {
        return xPos >= guiLeft && xPos <= guiLeft + width && yPos >= guiTop && yPos <= guiTop + height;
    }

    private void drawHorizontalScrollBar(float gray) {
        minecraft.getTextureManager().bindTexture(resource);
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0F);
        GlStateManager.translate(guiLeft + 1.0f, guiTop + height - 10.0f, 0.0f);
        GlStateManager.scale(0.5f, 0.5f, 0.5f);

        int barWidth = (width - 12) * 2;
        int maxCeil = (int) Math.ceil(barWidth / 256.0f);
        int tileWidth = barWidth / maxCeil;
        int lastTileWidth = barWidth - tileWidth * (maxCeil - 1);
        int vOffset = (216 - tileWidth) / 2;
        int vMax = 220 - lastTileWidth;
        for (int i = 0; i < maxCeil; ++i) {
            drawModalRectWithCustomSizedTexture(i * tileWidth, 0, i == 0 ? 0 : i == maxCeil - 1 ? vMax : vOffset,
                    220,
                    i == maxCeil - 1 ? lastTileWidth : tileWidth, 20, 256, 256);
        }
        if (maxScrollX > 0) {
            drawModalRectWithCustomSizedTexture(barWidth + 2, 0, 200, maxScrollY > 0 ? 80 : 100, 20, 20, 256, 256);
        }
        GlStateManager.popMatrix();

        if (maxScrollX > 0) {
            int trackWidth = width - 14;
            float scrollRatio = scrollX / maxScrollX;
            int thumbWidth = Math.max(20, (int) (trackWidth - maxScrollX));
            int thumbX = (int) (scrollRatio * (trackWidth - thumbWidth));

            GlStateManager.pushMatrix();
            GlStateManager.translate(guiLeft + 2.0f + thumbX, guiTop + height - 9.0f, 0.0f);
            GlStateManager.scale(0.5f, 0.5f, 0.5f);
            GlStateManager.color(gray, gray, gray, 1.0F);

            drawModalRectWithCustomSizedTexture(0, 0, 0, 240, thumbWidth, 16, 256, 256);
            drawModalRectWithCustomSizedTexture(thumbWidth, 0, 220 - thumbWidth, 240, thumbWidth, 16, 256, 256);

            GlStateManager.popMatrix();
        }
    }

    private void drawVerticalScrollBar(float gray) {
        minecraft.getTextureManager().bindTexture(resource);
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0F);
        GlStateManager.translate(guiLeft + width - 10.0f, guiTop + tabHeight, 0.0f);
        GlStateManager.scale(0.5f, 0.5f, 0.5f);

        int barHeight = (height - tabHeight - 11) * 2;
        int maxRow = (int) Math.ceil(barHeight / 256.0f);
        int tileHeight = barHeight / maxRow;
        int lastTileHeight = barHeight - tileHeight * (maxRow - 1);
        int vOffset = (252 - tileHeight) / 2;
        int vMax = 256 - lastTileHeight;

        for (int row = 0; row < maxRow; ++row) {
            drawModalRectWithCustomSizedTexture(0, row * tileHeight, 220,
                    row == 0 ? 0 : row == maxRow - 1 ? vMax : vOffset,
                    20, row == maxRow - 1 ? lastTileHeight : tileHeight, 256, 256);
        }
        if (maxScrollX <= 0 && maxScrollY > 0) {
            drawModalRectWithCustomSizedTexture(0, barHeight + 2, 200, 120, 20, 20, 256, 256);
        }
        GlStateManager.popMatrix();

        if (maxScrollY > 0) {
            int trackHeight = height - tabHeight - 13;
            float scrollRatio = scrollY / maxScrollY;

            int thumbHeight = Math.max(20, (int) (trackHeight - maxScrollY));
            int thumbY = (int) (scrollRatio * (trackHeight - thumbHeight));

            GlStateManager.pushMatrix();
            GlStateManager.translate(guiLeft + width - 9.0f, guiTop + tabHeight + 1.0f + thumbY, 0.0f);
            GlStateManager.scale(0.5f, 0.5f, 0.5f);
            GlStateManager.color(gray, gray, gray, 1.0F);

            drawModalRectWithCustomSizedTexture(0, 0, 240, 0, 16, thumbHeight, 256, 256);
            drawModalRectWithCustomSizedTexture(0, thumbHeight, 240, 256 - thumbHeight, 16, thumbHeight, 256, 256);

            GlStateManager.popMatrix();
        }
    }

    public void reset() {
        maxScrollX = 0.0f;
        maxScrollY = 0.0f;
        tabHeight = 0;
        int right, bottom;
        List<GuiMenuTopButton> topButtons = new ArrayList<>();
        for (IComponentGui c : wrapper.components) {
            right = c.getX() + c.getWidth() - guiLeft;
            bottom = c.getY() + c.getHeight() - guiTop;
            if (c instanceof GuiMenuTopButton) {
                topButtons.add((GuiMenuTopButton) c);
                if (tabHeight < c.getHeight() + 2) { tabHeight = c.getHeight() + 2; }
            }
            if (maxScrollX < right) { maxScrollX = right; }
            if (maxScrollY < bottom) { maxScrollY = bottom; }
        }
        if (maxScrollX - width + 11 < 0) { maxScrollX = 0; } else { maxScrollX -= width - 11; }
        if (maxScrollY - height + tabHeight + 12 < 0) { maxScrollY = 0; } else { maxScrollY -= height - tabHeight - 12; }
        if (!topButtons.isEmpty()) {
            if (topButtons.size() == 1) { topButtons.get(0).setWidth(width); }
            else {
                int x = 0;
                float tB = 0;
                Map<GuiMenuTopButton, Float> map = new HashMap<>();
                for (GuiMenuTopButton topButton : topButtons) {
                    float w = topButton.customFont != null ? topButton.customFont.width(topButton.getMessage()) : font.getStringWidth(topButton.getMessage().getString());
                    map.put(topButton, w);
                    tB += w;
                }
                int wB;
                for (GuiMenuTopButton topButton : topButtons) {
                    topButton.setX(guiLeft + x);
                    topButton.setY(guiTop);
                    if (topButtons.indexOf(topButton) == topButtons.size() - 1) { wB = width - x; }
                    else { wB = (int) (map.get(topButton) / tB * ((float) width)); }
                    topButton.setWidth(wB);
                    x += wB;
                }
            }
        }
        setSize(width, height);
    }

    public void resetRoll() {
        scrollX = 0;
        scrollY = 0;
    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) { listener.scrollClicked(scroll); }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) { listener.scrollDoubleClicked(scroll); }

    @Override
    public boolean isFocused() { return focused; }

    @Override
    public void tick() { wrapper.tick(); }

}