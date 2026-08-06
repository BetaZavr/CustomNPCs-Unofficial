package noppes.npcs.shared.client.gui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.controllers.YDEController;
import noppes.npcs.client.gui.GuiBoundarySetting;
import noppes.npcs.client.gui.util.GuiTooltipUtils;
import noppes.npcs.mixin.client.IMouseHandlerMixin;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.listeners.*;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuiCustomWindowNop extends GuiBasic
        implements IComponentGui, ICustomScrollListener, ISliderListener, ITextfieldListener, ITextChangeListener {


    @OnlyIn(Dist.CLIENT)
    public interface OnClose {
        void onClose(GuiCustomWindowNop window);
    }

    // super
    public int id;
    protected int guiLeft;
    protected int guiTop;
    protected double[] mouseMovedPos = new double[] { -1.0d, -1.0d };

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
    public void renderBackground(@Nonnull GuiGraphics graphics) {
        if (drawDefaultBackground) {
            PoseStack matrixStack = graphics.pose();
            matrixStack.pushPose();
            matrixStack.translate(guiLeft, guiTop, -1.0f);
            int right = imageWidth;
            int bottom = imageHeight;
            if (customFont != null) {
                right *= 2;
                    matrixStack.pushPose();
                    matrixStack.scale(0.5f * bgScale, 0.5f * bgScale, 0.5f * bgScale);
                    graphics.fill(0, 0, right, bottom, YDEController.backColor);
                    int color = (isHovered ? YDEController.hoverLineColor : YDEController.componentLineColor) & 0xFFFFFF | 0xC0000000;
                    graphics.hLine(1, right - 2, 1, color);
                    graphics.vLine(1, 1, bottom - 2, color);
                    graphics.vLine(right - 2, 1, bottom - 2, color);
                    graphics.hLine(1, right - 2, bottom - 2, color);
                    if (title != null && !title.getString().isEmpty()) {
                        matrixStack.translate(3.0f, 3.0f, 0.0f);
                        drawTopRect(graphics, right - 1);
                    }
                    matrixStack.popPose();
                matrixStack.popPose();

                if (title != null && !title.getString().isEmpty()) {
                    GuiButtonNop.renderString(graphics, title, guiLeft + 3, guiTop + 1,
                            guiLeft + imageWidth - 10, guiTop + 11,
                            YDEController.textColor, false, false, customFont);
                }
            }
            else {
                matrixStack.scale(bgScale, bgScale, bgScale);
                if (background != null) {
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
                                graphics.blit(background, col * tileWidth,
                                        row * tileHeight,
                                        col == 0 ? 0 : col == maxCol - 1 ? uMax : uOffset,
                                        row == 0 ? 0 : row == maxRow - 1 ? vMax : vOffset,
                                        col == maxCol - 1 ? lastTileWidth : tileWidth,
                                        row == maxRow - 1 ? lastTileHeight : tileHeight);
                            }
                        }
                    }
                    else if (imageWidth > 256) {
                        graphics.blit(background, 0, 0, 0, 0, 250, imageHeight);
                        graphics.blit(background, 250, 0, 256 - (imageWidth - 250), 0, imageWidth - 250, imageHeight);
                    }
                    else { graphics.blit(background, 0, 0, 0, 0, imageWidth, imageHeight); }
                }
                matrixStack.translate(3.0f, 3.0f, 0.0f);
                drawTopRect(graphics, right - 3);
                matrixStack.popPose();
                if (title != null && !title.getString().isEmpty()) {
                    GuiButtonNop.renderString(graphics, title, guiLeft + 4, guiTop + 2,
                            guiLeft + imageWidth - 20, guiTop + 11,
                            CustomNpcs.MainColor.getRGB() | 255 << 24, false, false, null);
                }
            }
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        width = imageWidth;
        height = imageHeight;
        isHovered = visible && isMouseHover(mouseX, mouseY, getX(), guiTop, imageWidth, imageHeight);
        if (!isHovered) {
            for (IComponentGui c : wrapper.components) {
                if ((c instanceof AbstractWidget widget && widget.isHovered()) ||
                        (c instanceof GuiCustomScrollNop scroll && scroll.isHovered())) {
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
            if (drawDefaultBackground) { renderBackground(graphics); }
            for (IComponentGui component : new ArrayList<>(wrapper.components)) {
                if (component instanceof Renderable renderable) { renderable.render(graphics, x, y, partialTicks); }
            }
            if (wrapper.subgui == null) {
                if (point != null && customFont == null) {
                    float xc, yc;
                    int[] cr = point.getCenter();
                    if (getX() + imageWidth / 2 < cr[0]) { xc = (float) getX() + imageWidth - 4; }
                    else { xc = (float) getX() + 4; }
                    if (getY() + 12 < cr[1]) { yc = (float) getY() + 12; }
                    else { yc = (float) getY() + 4; }
                    double dist = Math.sqrt((mouseY - yc) * (mouseY - yc) + (mouseX - xc) * (mouseX - xc));
                    double base = Math.sqrt(Math.pow(imageWidth, 2.0d) + Math.pow(imageHeight, 2.0d)) / 2.0d;
                    if (dist <= base * 2.0d) {
                        double a = -1.0d / (2.0d * base - base);
                        double b = -2.0d * a  * base;
                        float alpha = (float) (a * dist + b);
                        if (alpha < 0.0f) { alpha = 0.0f; } else if (alpha > 1.0f) { alpha = 1.0f; }
                        int color = colorLine + ((int) (alpha * 255.0f) << 24);
                        GuiBoundarySetting.drawLine(graphics, cr[0], cr[1], xc, yc, color, 2);
                    }
                }
                if (!hoverText.isEmpty() && (hoverIsGame || (CustomNpcs.ShowDescriptions && GuiBasic.showHoverText))) {
                    if (!hoverIsGame) { hoverText.add(Component.translatable("hover.alt.h")); }
                    if (listener != null) { listener.setHoverText(hoverText); }
                    else {
                        if (hoverFont == null) { GuiTooltipUtils.renderTooltip(graphics, font, hoverText, Optional.empty(), mouseX, ValueUtil.correctInt(mouseY, 16, height)); }
                        else { renderTooltipInternal(graphics, mouseX, ValueUtil.correctInt(mouseY, 16, height), hoverFont, hoverText, bgScale); }
                    }
                    hoverText.clear();
                }
            }
            if (!isLock && mouseMovedPos[0] > 0.0d && mouseMovedPos[1] > 0.0d) {
                if (((IMouseHandlerMixin) Minecraft.getInstance().mouseHandler).getActiveButton() == 0) {
                    x = (int) (mouseX - mouseMovedPos[0]);
                    y = (int) (mouseY - mouseMovedPos[1]);
                    if (Math.abs(x) > 0 || Math.abs(y) > 0) {
                        mouseMovedPos[0] = mouseX;
                        mouseMovedPos[1] = mouseY;
                        moveTo(x, y);
                    }
                }
                else {
                    mouseMovedPos[0] = -1.0d;
                    mouseMovedPos[1] = -1.0d;
                }
            }
        }
    }

    @Override
    public int[] getCenter() { return new int[] { getX() + width / 2, guiTop + height / 2}; }

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
        return this;
    }

    @Override
    public GuiCustomWindowNop setIsVisible(boolean isVisible) {
        visible = isVisible;
        return this;
    }

    @Override
    public GuiCustomWindowNop setIsFocused(boolean isFocused) {
        focused = isFocused;
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
        boolean bo = wrapper.mouseClicked(mouseX, mouseY, mouseButton);
        if (!bo && !isLock && mouseButton == 0 && isHeadHovered) {
            mouseMovedPos[0] = mouseX;
            mouseMovedPos[1] = mouseY;
        }
        return bo;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
        if (enabled && visible && isHovered) {
            return wrapper.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (enabled && visible && isHovered) { return wrapper.mouseReleased(mouseX, mouseY, mouseButton); }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
        return wrapper.mouseScrolled(mouseX, mouseY, scrolled);
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
    public boolean isFocused() { return focused; }

    @Override
    public int getX() { return guiLeft; }

    @Override
    public int getY() { return guiTop; }

    public boolean isHovered() { return visible && isHovered; }

    public void drawTopRect(GuiGraphics graphics, int width) {
        float r = (colorLine >> 16) / 255.0f;
        float g = (colorLine >> 8) / 255.0f;
        float b = (colorLine & 0xFF) / 255.0f;
        VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f matrix = graphics.pose().last().pose();
        consumer.vertex(matrix, 0.0f, 0.0f, 0.0f).color(r, g, b, 1.0f).endVertex();
        consumer.vertex(matrix, 0.0f, 8.0f, 0.0f).color(r, g, b, 1.0f).endVertex();
        consumer.vertex(matrix, width - 6.0f, 8.0f, 0.0f).color(r, g, b, 0.5f).endVertex();
        consumer.vertex(matrix, width - 6.0f, 0.0f, 0.0f).color(r, g, b, 0.5f).endVertex();
        graphics.bufferSource().endBatch();
    }

    public void setPoint(IComponentGui component) { point = component; }

    public void setColorLine(int color) { colorLine = color & 0x00FFFFFF; }

    public int getColorLine() { return colorLine; }

    public boolean isHeadHovered() { return isHeadHovered; }

    public GuiCustomWindowNop addClose(OnClose onCloseIn) {
        onClose = onCloseIn;
        return this;
    }

}
