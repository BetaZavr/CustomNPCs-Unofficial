package noppes.npcs.shared.client.gui.components;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.controllers.YDEController;
import noppes.npcs.client.gui.GuiBoundarySetting;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.listeners.*;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiCustomWindowNop extends GuiBasic
        implements IComponentGui, ICustomScrollListener, ISliderListener, ITextfieldListener, ITextChangeListener {

    @SideOnly(Side.CLIENT)
    public interface OnClose {
        void onClose(GuiCustomWindowNop window);
    }

    // super
    public int id;
    protected int guiLeft;
    protected int guiTop;


    protected boolean isHovered;
    protected boolean isHeadHovered;
    protected boolean focused = false;
    public boolean active = false;
    public boolean enabled = true;
    public boolean visible = true;
    public IGuiInterface listener;

    public Component title;
    protected IComponentGui point;
    public final @Nonnull GuiButtonNop exit;
    public final @Nonnull GuiButtonNop lock;
    protected OnClose onClose = null;
    public int colorLine = new Color(0x6C00FF).getRGB();
    public Object[] objs = null;

    // Yellow Dialog Edit
    protected ClientProxy.FontContainer customFont = null;
    public boolean isLock = false;
    public boolean isYDEShow = false;
    public YDEScrollNop yde_scroll;

    public GuiCustomWindowNop(IGuiInterface gui, int idIn, int x, int y, int width, int height, Component titleIn) {
        super();
        id = idIn;
        title = titleIn;
        guiLeft = x;
        guiTop = y;
        setSize(width, height);
        setBackground("bgfilled.png");
        listener = gui;
        exit = addButton(2500, width - 11, 3, "X")
                .setSize(8, 8)
                .setTexture(ANIMATION_BUTTONS)
                .setDefBack(false)
                .setIsAnim(true)
                .setUV(232, 0, 24, 24)
                .setColor(new Color(0xFF404040).getRGB());
        exit.layerColor = new Color(0xFFFF0000).getRGB();
        lock = addButton(2501, width - 20, 3, false, 0, "", "")
                .setSize(8, 8)
                .setTexture(ANIMATION_BUTTONS_SLOTS)
                .setDefBack(false)
                .setIsAnim(true)
                .setUV(208, 0, 24, 24);
        lock.layerColor = new Color(0xFFFFFF00).getRGB();
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        if (isHovered && visible) {
            if (button.equals(exit)) { visible = false; }
            else if (button == lock) {
                isLock = !isLock;
                if (isLock) {
                    button.txrX += button.txrW;
                    lock.layerColor = new Color(0xFFA0A000).getRGB();
                }
                else {
                    button.txrX -= button.txrW;
                    lock.layerColor = new Color(0xFFFFFF00).getRGB();
                }
            }
            listener.buttonEvent(button);
        }
    }

    @Override
    public boolean mouseButtonEvent(GuiButtonNop button, int mouseButton) {
        if (isHovered && visible) { return listener.mouseButtonEvent(button, mouseButton); }
        return false;
    }

    @Override
    public void drawDefaultBackground() {
        if (drawDefaultBackground) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(guiLeft, guiTop, -1.0f);
            int right = imageWidth;
            int bottom = imageHeight;
            if (customFont != null) {
                right *= 2;
                GlStateManager.pushMatrix();
                GlStateManager.scale(0.5f * bgScale, 0.5f * bgScale, 0.5f * bgScale);
                drawRect(0, 0, right, bottom, YDEController.backColor);
                int color = (isHovered ? YDEController.hoverLineColor : YDEController.componentLineColor) & 0xFFFFFF | 0xC0000000;
                drawHorizontalLine(1, right - 2, 1, color);
                drawVerticalLine(1, 1, bottom - 2, color);
                drawVerticalLine(right - 2, 1, bottom - 2, color);
                drawHorizontalLine(1, right - 2, bottom - 2, color);
                if (title != null && !title.getFormattedText().isEmpty()) {
                    GlStateManager.translate(3.0f, 3.0f, 0.0f);
                    drawTopRect(right - 1);
                }
                GlStateManager.popMatrix();
                GlStateManager.popMatrix();

                if (title != null && !title.getFormattedText().isEmpty()) {
                    GuiButtonNop.renderString(title, guiLeft + 3, guiTop + 1,
                            guiLeft + imageWidth - 10, guiTop + 11,
                            YDEController.textColor, false, false, customFont);
                }
            }
            else {
                GlStateManager.scale(bgScale, bgScale, bgScale);
                if (background != null) {
                    minecraft.getTextureManager().bindTexture(background);
                    if (widthTexture != 0 && heightTexture != 0) {
                        int maxRow = ValueUtil.correctInt((int) Math.ceil((float) imageHeight / (float) (heightTexture - 2 * borderTexture)), 2, 10);
                        int maxCol = ValueUtil.correctInt((int) Math.ceil((float) imageWidth / (float) (widthTexture - 2 * borderTexture)), 2, 10);
                        int tileWidth = imageWidth / maxCol;
                        int tileHeight = imageHeight / maxRow;
                        int lastTileWidth = imageWidth - tileWidth * (maxCol - 1);
                        int lastTileHeight = imageHeight - tileHeight * (maxRow - 1);

                        int uOffset = (widthTexture - 2 * borderTexture - tileWidth) / 2;
                        int uMax = widthTexture - lastTileWidth;
                        int vOffset = (heightTexture - 2 * borderTexture - tileHeight) / 2;
                        int vMax = heightTexture - lastTileHeight;
                        for (int col = 0; col < maxCol; ++col) {
                            for (int row = 0; row < maxRow; ++row) {
                                drawTexturedModalRect(col * tileWidth,
                                        row * tileHeight,
                                        col == 0 ? 0 : col == maxCol - 1 ? uMax : uOffset,
                                        row == 0 ? 0 : row == maxRow - 1 ? vMax : vOffset,
                                        col == maxCol - 1 ? lastTileWidth : tileWidth,
                                        row == maxRow - 1 ? lastTileHeight : tileHeight);
                            }
                        }
                    }
                    else if (imageWidth > 256) {
                        drawTexturedModalRect(0, 0, 0, 0, 250, imageHeight);
                        drawTexturedModalRect(250, 0, 256 - (imageWidth - 250), 0, imageWidth - 250, imageHeight);
                    }
                    else { drawTexturedModalRect(0, 0, 0, 0, imageWidth, imageHeight); }
                }
                GlStateManager.translate(3.0f, 3.0f, 0.0f);
                drawTopRect(right - 3);
                GlStateManager.popMatrix();
                if (title != null && !title.getFormattedText().isEmpty()) {
                    GuiButtonNop.renderString(title, guiLeft + 4, guiTop + 2,
                            guiLeft + imageWidth - 20, guiTop + 11,
                            CustomNpcs.MainColor.getRGB() | 255 << 24, false, false, null);
                }
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        width = imageWidth;
        height = imageHeight;
        isHovered = visible && isMouseHover(mouseX, mouseY, getX(), guiTop, imageWidth, imageHeight);
        if (!isHovered) {
            for (IComponentGui c : wrapper.components) {
                if (c.isHovered()) {
                    isHovered = true;
                    break;
                }
            }
        }
        isHeadHovered = isHovered && isMouseHover(mouseX, mouseY, getX() + 3, guiTop + 3, imageWidth - 3, 8);
        if (visible) {
            wrapper.mouseX = mouseX;
            wrapper.mouseY = mouseY;
            int x = hasSubGui() ? 0 : mouseX;
            int y = hasSubGui() ? 0 : mouseY;
            int right = getX() + imageWidth;
            int bottom = guiTop + imageHeight;
            if (drawDefaultBackground) { drawDefaultBackground(); }
            for (IComponentGui component : new ArrayList<>(wrapper.components)) {
                component.render(x, y, partialTicks);
            }
            if (wrapper.subgui == null) {
                if (point != null && customFont == null) {
                    float xc = (float) right / 2.0f;
                    float yc = (float) bottom / 2.0f;
                    double dist = Math.sqrt((mouseY - yc) * (mouseY - yc) + (mouseX - xc) * (mouseX - xc));
                    double base = Math.sqrt(Math.pow(imageWidth, 2.0d) + Math.pow(imageHeight, 2.0d)) / 2.0d;
                    if (dist <= base * 2.0d) {
                        double a = -1.0d / (2.0d * base - base);
                        double b = -2.0d * a  * base;
                        float alpha = (float) (a * dist + b);
                        if (alpha < 0.0f) { alpha = 0.0f; } else if (alpha > 1.0f) { alpha = 1.0f; }
                        int[] cr = point.getCenter();
                        int color = colorLine + ((int) (alpha * 255.0f) << 24);
                        GuiBoundarySetting.drawLine(cr[0], cr[1], xc, yc, color, 2);
                    }
                }
                if (!hoverText.isEmpty() && (hoverIsGame || (CustomNpcs.ShowDescriptions && GuiBasic.showHoverText))) {
                    if (!hoverIsGame) { hoverText.add(Component.translatable("hover.alt.h")); }
                    if (listener != null) { listener.setHoverText(hoverText); }
                    else {
                        if (hoverFont == null) { drawHoveringText(toHoverText(), mouseX, mouseY, fontRenderer); }
                        else { GuiBasic.renderTooltipInternal(mouseX, ValueUtil.correctInt(mouseY, 16, height), this, hoverFont, hoverText, bgScale); }
                    }
                    hoverText.clear();
                }
            }
        }
    }

    @Override
    public int[] getCenter() { return new int[] { guiLeft + width / 2, guiTop + height / 2}; }

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
        guiLeft += addX;
        guiTop += addY;
        for (IComponentGui component : new ArrayList<>(wrapper.components)) { component.moveTo(addX, addY); }
    }

    public void transferTo(int newX, int newY) {
        moveTo(newX - guiLeft, newY - guiTop);
    }

    @Override
    public GuiCustomWindowNop setHoverTexts(Object... components) {
        hoverText.clear();
        if (components == null) { return this; }
        Util.instance.putHovers(hoverText, components);
        return this;
    }

    @Override
    public GuiCustomWindowNop setIsEnabled(boolean isEnabled) {
        active = isEnabled;
        for (IComponentGui component : new ArrayList<>(wrapper.components)) { component.setIsEnabled(isEnabled); }
        return this;
    }

    @Override
    public GuiCustomWindowNop setIsVisible(boolean isVisible) {
        visible = isVisible;
        for (IComponentGui component : new ArrayList<>(wrapper.components)) { component.setIsVisible(isVisible); }
        return this;
    }

    @Override
    public GuiCustomWindowNop setIsFocused(boolean isFocused) {
        focused = isFocused;
        for (IComponentGui component : new ArrayList<>(wrapper.components)) { component.setIsFocused(false); }
        return this;
    }

    @Override
    public GuiCustomWindowNop setSize(int width, int height) {
        imageWidth = width;
        imageHeight = height;
        return this;
    }

    @Override
    public GuiComponentType getElementType() { return GuiComponentType.EXTRA; }

    @Override
    public GuiCustomWindowNop setCustomFont(ClientProxy.FontContainer font) {
        customFont = font;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (!enabled || !visible) { return false; }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
        if (enabled && visible && isHovered) {
            boolean bo = wrapper.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
            if (!bo && !isLock && mouseButton == 0 && isHeadHovered) {
                moveTo((int) (dx), (int) (dy));
                bo = true;
            }
            return bo;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (enabled && visible && isHovered) { return wrapper.mouseReleased(mouseX, mouseY, mouseButton); }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double mouseScrolled) {
        if (enabled && visible && isHovered) {
            boolean bo = wrapper.mouseScrolled(mouseX, mouseY, mouseScrolled);
            if (!bo) {
                if (mouseScrolled > 0.0d) { focusedNextComponent(); bo = true; }
                else if (mouseScrolled < 0.0d) { focusedPrevComponent(); bo = true; }
            }
            return bo;
        }
        return false;
    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {
        if (listener instanceof ICustomScrollListener) { ((ICustomScrollListener) listener).scrollClicked(scroll); }
    }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
        if (listener instanceof ICustomScrollListener) { ((ICustomScrollListener) listener).scrollDoubleClicked(scroll); }
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) {
        if (listener instanceof ITextfieldListener) { ((ITextfieldListener) listener).unFocused(textField); }
    }

    @Override
    public void mouseDragged(GuiSliderNop slider) {
        if (listener instanceof ISliderListener) { ((ISliderListener) listener).mouseDragged(slider); }
    }

    @Override
    public void mousePressed(GuiSliderNop slider) {
        if (listener instanceof ISliderListener) { ((ISliderListener) listener).mousePressed(slider); }
    }

    @Override
    public void mouseReleased(GuiSliderNop slider) {
        if (listener instanceof ISliderListener) { ((ISliderListener) listener).mouseReleased(slider); }
    }

    @Override
    public void textUpdate(IComponentGui component, String text) {
        if (listener instanceof ITextChangeListener) { ((ITextChangeListener) listener).textUpdate(component, text); }
    }

    @Override
    public void onClose() {
        if (onClose != null) { onClose.onClose(this); }
        super.onClose();
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
    public GuiMenuSideButton addSideButton(int id, int x, int y, Object label) {
        return super.addSideButton(id, guiLeft + x, guiTop + y, label);
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
    public int getX() { return guiLeft; }

    @Override
    public int getY() { return guiTop; }

    @Override
    public boolean isHovered() { return visible && isHovered; }

    public void drawTopRect(int width) {
        float r = (colorLine >> 16) / 255.0f;
        float g = (colorLine >> 8) / 255.0f;
        float b = (colorLine & 0xFF) / 255.0f;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(0.0f, 0.0f, zLevel).color(r, g, b, 1.0f).endVertex();
        buffer.pos(0.0f, 19.0f, zLevel).color(r, g, b, 1.0f).endVertex();
        buffer.pos(width - 6.0f, 19.0f, zLevel).color(r, g, b, 0.5f).endVertex();
        buffer.pos(width - 6.0f, 0.0f, zLevel).color(r, g, b, 0.5f).endVertex();

        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public void setPoint(IComponentGui component) { point = component; }

    public void setColorLine(int color) { colorLine = color & 0x00FFFFFF; }

    public int getColorLine() { return colorLine; }

    @SuppressWarnings("unused")
    public boolean isHeadHovered() { return isHeadHovered; }

    public GuiCustomWindowNop addClose(OnClose onCloseIn) {
        onClose = onCloseIn;
        return this;
    }

    @Override
    public void tick() { wrapper.tick(); }

    @Override
    public boolean keyPressed(char typedChar, int keyCode) {
        if (enabled && visible && isHovered) {
            boolean bo = wrapper.keyPressed(typedChar, keyCode);
            if (!bo) {
                switch (keyCode) {
                    case Keyboard.KEY_TAB:
                    case Keyboard.KEY_DOWN: {
                        focusedNextComponent();
                        return true;
                    } // focused next component
                    case Keyboard.KEY_UP: {
                        focusedPrevComponent();
                        return true;
                    } // focused prev component
                }
            }
            return bo;
        }
        return false;
    }

}
