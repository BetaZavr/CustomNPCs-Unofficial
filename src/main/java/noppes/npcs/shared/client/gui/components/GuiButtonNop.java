package noppes.npcs.shared.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.api.mixin.util.text.IStyleMixin;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class GuiButtonNop extends Gui implements IComponentGui {

    public static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");
    protected static final OnPress clicked = (button) -> button.listener.buttonEvent(button);
    protected static float level;

    protected OnRender render = null;

    public IGuiInterface listener;
    protected Component[] display;
    protected int displayValue = 0;
    public int id;

    // Normal
    protected Component message;
    protected int textColor;
    public boolean showShadow = true;

    // New from Unofficial (BetaZavr)
    protected List<Component> hoverText = new ArrayList<>();
    protected ClientProxy.FontContainer customFont = null;
    protected static final double step = 60;

    public ItemStack[] renderStacks = null;
    public ItemStack renderStack = ItemStack.EMPTY;
    public int renderStackId = -1;
    protected int ticks = 0;
    protected int wait = 0;

    public ResourceLocation texture = null;
    public int renderStackID = -1;
    public int txrX = 0;
    public int txrY = 0;
    public int txrW = 200;
    public int txrH = 20;
    public int offsetHoverX = 0;
    public int offsetHoverY = 0;

    public int layerColor;
    public boolean hasDefBack;
    public boolean isSimple = false;
    public boolean isAnim = false;
    public boolean isScissor = true;

    public boolean hasSound = true;

    // standard
    protected boolean focused = false;
    protected boolean isHovered;
    protected final OnPress onPress;
    protected float alpha = 1.0F;
    protected int width;
    protected int height;
    protected int x;
    protected int y;
    public boolean active = false;
    public boolean enabled = true;
    public boolean visible = true;
    public int packedFGColor;

    private static int customColorOf(ITextComponent component) {
        int color = ((IStyleMixin) component.getStyle()).npcs$getColor();
        if (color != 0) { return color; }
        for (ITextComponent sibling : component.getSiblings()) {
            color = customColorOf(sibling);
            if (color != 0) { return color; }
        }
        return 0;
    }

    public static void renderString(@Nonnull Component message,
                                    int left, int top, int right, int bottom, int color, boolean showShadow,
                                    boolean centered, ClientProxy.FontContainer customFont) {
        Minecraft mc = Minecraft.getMinecraft();
        int textWidth = customFont != null ? customFont.width(message) : mc.fontRenderer.getStringWidth(message.getFormattedText());
        int height = (top + bottom - 9) / 2 + 1;
        if (customFont != null) {
            textWidth++;
            height--;
        }
        int width = right - left;
        GlStateManager.pushMatrix();
        int c = customColorOf(message.getParent());
        if (c != 0) {
            int alpha = color >>> 24;
            if (color == 0 || (color & 0xFFFFFF) == 0xFFFFFF) { color = (alpha << 24) | (c & 0xFFFFFF); }
            else {
                int r1 = (color >> 16) & 0xff;
                int g1 = (color >> 8) & 0xff;
                int b1 = color & 0xff;
                int r2 = (c >> 16) & 0xff;
                int g2 = (c >> 8) & 0xff;
                int b2 = c & 0xff;
                color = (alpha << 24) | (((r1 + r2) / 2) << 16) | (((g1 + g2) / 2) << 8) | ((b1 + b2) / 2);
            }
        }
        if (textWidth > width) {
            boolean wasScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            ScaledResolution sw = new ScaledResolution(Minecraft.getMinecraft());
            double d4 = sw.getScaleFactor();
            GL11.glScissor((int) ((double) left * d4),
                    (int) ((double) mc.displayHeight - (double) bottom * d4),
                    Math.max(0, (int) ((double) (right - left) * d4)),
                    Math.max(0, (int) ((double) (bottom - top) * d4)));

            int centerX = textWidth - width;
            double d0 = (double) System.currentTimeMillis() / 1000.0;
            double d1 = Math.max((double) centerX * 0.5, 3.0);
            double d2 = Math.sin(Math.PI / 2.0d * Math.cos(Math.PI * 2.0d * d0 / d1)) / 2.0 + 0.5;
            double d3 = ValueUtil.lerp(d2, 0.0, centerX);
            if (customFont != null) { customFont.draw(message, left - (int) d3, height, color); }
            else { mc.fontRenderer.drawString(message.getFormattedText(), left - (int) d3, height, color, showShadow); }
            if (!wasScissor) { GL11.glDisable(GL11.GL_SCISSOR_TEST); }
        } // moved
        else {
            if (centered) {
                width = (left + right) / 2;
                if (customFont != null) { customFont.draw(message, width - (float) textWidth / 2.0f, height, color); }
                else { mc.fontRenderer.drawString(message.getFormattedText(), width - (float) textWidth / 2.0f, height, color, showShadow); }
            }
            else {
                if (customFont != null) { customFont.draw(message, left, height, color); }
                else { mc.fontRenderer.drawString(message.getFormattedText(), left, height, color, showShadow); }
            }
        } // in round
        GlStateManager.popMatrix();
    }

    public static void drawHoveringText(List<Component> components, int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        List<Component> lines = new ArrayList<>();
        Util.instance.putHovers(lines, components);
        List<String> textLines = new ArrayList<>();
        for (Component line : lines) { textLines.add(line.getFormattedText()); }
        net.minecraftforge.fml.client.config.GuiUtils.drawHoveringText(textLines, x, y, width, height, -1, mc.fontRenderer);
        if (!textLines.isEmpty()) {
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            int i = 0;
            for (String s : textLines) {
                int j = mc.fontRenderer.getStringWidth(s);
                if (j > i) { i = j; }
            }
            int l1 = x + 12;
            int i2 = y - 12;
            int k = 8;
            if (textLines.size() > 1) { k += 2 + (textLines.size() - 1) * 10; }
            if (l1 + i > width) { l1 -= 28 + i; }
            if (i2 + k + 6 > height) { i2 = height - k - 6; }
            level = 300.0F;
            mc.getRenderItem().zLevel = 300.0F;
            int colorS = 0xF0100010;
            int colorE = colorS;
            drawRect(l1 - 3, i2 - 4, l1 + i + 3, i2 - 3, colorS, colorE);
            drawRect(l1 - 3, i2 + k + 3, l1 + i + 3, i2 + k + 4, colorS, colorE);
            drawRect(l1 - 3, i2 - 3, l1 + i + 3, i2 + k + 3, colorS, colorE);
            drawRect(l1 - 4, i2 - 3, l1 - 3, i2 + k + 3, colorS, colorE);
            drawRect(l1 + i + 3, i2 - 3, l1 + i + 4, i2 + k + 3, colorS, colorE);
            colorS = 0x505000FF;
            colorE = 0x5028007F;
            drawRect(l1 - 3, i2 - 3 + 1, l1 - 3 + 1, i2 + k + 3 - 1, colorS, colorE);
            drawRect(l1 + i + 2, i2 - 3 + 1, l1 + i + 3, i2 + k + 3 - 1, colorS, colorE);
            drawRect(l1 - 3, i2 - 3, l1 + i + 3, i2 - 3 + 1, colorS, colorS);
            drawRect(l1 - 3, i2 + k + 2, l1 + i + 3, i2 + k + 3, colorE, colorE);
            for (int k1 = 0; k1 < textLines.size(); ++k1) {
                String s1 = textLines.get(k1);
                mc.fontRenderer.drawStringWithShadow(s1, (float)l1, (float)i2, -1);
                if (k1 == 0) { i2 += 2; }
                i2 += 10;
            }
            level = 0.0F;
            mc.getRenderItem().zLevel = 0.0F;
            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableRescaleNormal();
        }
    }

    protected static void drawRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(right, top, level).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(left, top, level).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(left, bottom, level).color(f5, f6, f7, f4).endVertex();
        bufferbuilder.pos(right, bottom, level).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public GuiButtonNop(IGuiInterface gui, int buttonId, Object label, int xIn, int yIn, OnPress onPressIn) {
        id = buttonId;
        x = xIn;
        y = yIn;
        width = 200;
        height = 20;
        onPress = onPressIn != null ? onPressIn : clicked;
        listener = gui;
        setDisplayText(label);
    }

    public GuiButtonNop(IGuiInterface gui, int buttonId, int xIn, int yIn, OnPress onPressIn, int variant, Object ... variants) {
        id = buttonId;
        x = xIn;
        y = yIn;
        width = 200;
        height = 20;
        onPress = onPressIn != null ? onPressIn : clicked;
        setDisplayText(Component.empty());
        listener = gui;
        displayValue = variant;
        setVariants(variants);
    }

    public GuiButtonNop(IGuiInterface gui, int buttonId, Object label, int x, int y, OnPress onPress, OnRender onRender) {
        this(gui, buttonId, label, x, y, onPress);
        render = onRender;
    }

    public GuiButtonNop(IGuiInterface gui, int buttonId, int x, int y, int variant, Object ... variants) {
        this(gui, buttonId, x, y, clicked, variant, variants);
    }

    protected boolean clicked(double mouseX, double mouseY) {
        return visible && enabled && (isHovered || mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height);
    }

    @Override
    public GuiButtonNop setSize(int widthIn, int heightIn) {
        width = widthIn;
        height = heightIn;
        return this;
    }

    public @Nonnull Component getMessage() {
        if (message == null) { return Component.empty(); }
        return message;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            renderWidget(mouseX, mouseY, partialTicks);
            if (isHovered && !hoverText.isEmpty()) {
                if (listener != null) { listener.setHoverText(hoverText); }
                else { drawHoveringText(hoverText, mouseX, mouseY, width, height); }
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    protected void onClick(double x, double y) {
        if (display != null && display.length != 0) { setDisplay((displayValue + 1) % display.length); }
        if (hasSound) { Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)); }
        if (onPress != null) { onPress(); }
        else if (listener != null) { listener.buttonEvent(this); }
    }

    public void onPress() { onPress.onPress(this); }

    public void renderWidget(int mouseX, int mouseY, float partialTicks) {
        if (offsetHoverX != 0 || offsetHoverY != 0) {
            mouseX -= offsetHoverX;
            mouseY -= offsetHoverY;
        }
        isHovered = visible && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        if (!visible) { return; }
        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        GlStateManager.enableBlend();
        Minecraft mc = Minecraft.getMinecraft();
        if (render != null) { render.onRender(this, mouseX, mouseY, partialTicks); }
        else {
            int state = getState();
            if (hasDefBack) {
                drawRect(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF202020);
                drawRect(x, y, x + width, y + height, 0xFFA0A0A0);
            }
            if (layerColor != 0) {
                GlStateManager.color((float) (layerColor >> 16 & 255) / 255.0f,
                        (float) (layerColor >> 8 & 255) / 255.0f,
                        (float) (layerColor & 255) / 255.0f,
                        (float) (layerColor >> 24 & 255) / 255.0f);
            }
            if (texture == null) {
                mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                int left = width / 2;
                int right = width - left;
                int v = 46 + state * 20;
                int h0 = height / 2;
                int h1 = height - h0;
                GlStateManager.pushMatrix();
                GlStateManager.translate(x, y, 0.0f);
                drawTexturedModalRect(0, 0, 0, v, left, h0); // left up
                drawTexturedModalRect(0, h0, 0, v + 20 - h1, left, h1); // left down
                drawTexturedModalRect(left, 0, 200 - right, v, right, h0); // right up
                drawTexturedModalRect(left, h0, 200 - right, v + 20 - h1, right, h1); // right down
                if (enabled && isFocused()) {
                    drawHorizontalLine(0, width - 1, 0, 0xFFFFFFFF);
                    drawHorizontalLine(0, width - 1, height - 1, 0xFFFFFFFF);
                    drawVerticalLine(0, 0, height - 1, 0xFFFFFFFF);
                    drawVerticalLine(width - 1, 0, height - 1, 0xFFFFFFFF);
                }
                GlStateManager.popMatrix();
            }
            else {
                GlStateManager.pushMatrix();
                mc.getTextureManager().bindTexture(texture);
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
                if (isSimple) {
                    int w0 = width / 2;
                    int w1 = width - w0;
                    drawTexturedModalRect(x, y, txrX, txrY + state * height, w0, height);
                    drawTexturedModalRect(x + w0, y, txrX + txrW - w0, txrY + state * height, w1, height);
                }
                else {
                    boolean isPrefabricated = txrW == 0;
                    int tw = isPrefabricated ? 200 : txrW;
                    int th = txrH == 0 ? 20 : txrH;
                    float scaleH = height / (float) th;
                    float scaleW = isPrefabricated ? scaleH : width / (float) tw;
                    GlStateManager.scale(scaleW, scaleH, 1.0f);
                    GlStateManager.translate(x / scaleW, y / scaleH, 0.0f);
                    if (isPrefabricated) {
                        tw = (int) (((float) width / 2.0f) / scaleH);
                        drawTexturedModalRect(0, 0, txrX, txrY + state * th, tw, th);
                        drawTexturedModalRect(tw, 0, txrX + 200 - tw, txrY + state * th, tw, th);
                    }
                    else { drawTexturedModalRect(0, 0, txrX, txrY + state * th, tw, th); }
                }
                GlStateManager.popMatrix();
            }
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.pushMatrix();
        @Nonnull Component label = getMessage();
        String text = label.getString();
        if (enabled && (text.equals("<") || text.equals(">") || text.equals("<<") || text.equals("<<<") || text.equals(">>") || text.equals(">>>"))) {
            int w = mc.fontRenderer.getStringWidth(text);
            float wm = width - 4 + 2 * w;
            float ox = (float) (System.currentTimeMillis() % 2000L) / 2000.0f * wm;
            if (text.equals("<") || text.equals("<<") || text.equals("<<<")) { GlStateManager.translate((width + w) / 2.0f - ox, 0.0f, 0.0f); }
            else { GlStateManager.translate((width + w) / -2.0f + ox, 0.0f, 0.0f); }
        }
        if (!enabled) { GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F); }
        int color = getFGColor() | (int) Math.ceil(alpha * 255.0F) << 24;
        if (isScissor) {
            renderString(label, x + 2, y, x + width - 2, y + height, color, showShadow, true, customFont);
        } else {
            if (customFont != null) { customFont.draw(label, getX() + 2, getY(), color); }
            else { mc.fontRenderer.drawString(label.getFormattedText(), getX() + 2.0f - mc.fontRenderer.getStringWidth(label.getFormattedText()) / 2.0f, getY(), color, showShadow); }
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.popMatrix();
        if (renderStacks != null && renderStacks.length != 0 && (listener == null || !listener.hasSubGui())) {
            renderStack = renderStacks[0];
            renderStackID = 0;
            if (renderStacks.length > 1) {
                if (wait > 0) { wait --; }
                else {
                    renderStackID = (int) Math.floor(((double) ticks % (step * (double) renderStacks.length - 1.0d)) / step);
                    renderStack = renderStacks[renderStackID];
                }
            }
            if (renderStack != null && !renderStack.isEmpty()) {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float) x + (float) width / 2.0f - 8.0f, (float) y + (float) height / 2.0f - 8.0f, 30.0F);
                mc.getRenderItem().renderItemAndEffectIntoGUI(renderStack, 0, 0);
                mc.getRenderItem().renderItemOverlays(mc.fontRenderer, renderStack, 0, 0);
                GlStateManager.popMatrix();
            }
            if (wait == 0) {
                ticks++;
                if (ticks > step * renderStacks.length) { ticks = 0; }
            }
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double mouseScrolled) {
        if (display != null && display.length != 0 && isHovered && mouseScrolled != 0.0d) {
            setDisplay(displayValue + (mouseScrolled > 0 ? 1 : -1));
            if (onPress != null) { onPress(); }
            else if (listener != null) { listener.buttonEvent(this); }
            return true;
        }
        return false;
    }

    public int getValue() { return displayValue; }

    public void setMessage(Object label) { setDisplayText(label); }

    public void setDisplayText(Object label) {
        message = label == null ? Component.empty() :
                label instanceof Component ? (Component) label :
                        label instanceof ITextComponent ? new Component((ITextComponent) label) :
                                Component.translatable(label.toString());
    }

    public GuiButtonNop setDisplay(int value) {
        if (display == null || display.length == 0) { return this; }
        displayValue = ((value % display.length) + display.length) % display.length;
        setDisplayText(display[displayValue]);
        return this;
    }

    public GuiButtonNop setDefBack(boolean bo) {
        hasDefBack = bo;
        return this;
    }

    @SuppressWarnings("unused")
    public ItemStack getRenderStack() { return renderStack; }

    @SuppressWarnings("unused")
    public int getRenderStackId() { return renderStackId; }

    @SideOnly(Side.CLIENT)
    public interface OnRender {
        void onRender(GuiButtonNop button, int mouseX, int mouseY, float partialTicks);
    }

    public int getFGColor() {
        if (packedFGColor != 0) { return packedFGColor; }
        else if (!enabled) { return CustomNpcs.NotEnableColor.getRGB(); }
        else if (isHovered) { return CustomNpcs.HoverColor.getRGB(); }
        return CustomNpcs.ButtonColor.getRGB();
    }

    @Override
    public int[] getCenter() { return new int[] { x + width / 2, y + height / 2}; }

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

    @Override
    public GuiButtonNop setHoverTexts(Object... components) {
        hoverText.clear();
        if (components == null) { return this; }
        Util.instance.putHovers(hoverText, components);
        return this;
    }

    @Override
    public GuiButtonNop setIsEnabled(boolean isEnabled) {
        enabled = isEnabled;
        return this;
    }

    @Override
    public GuiButtonNop setIsVisible(boolean isVisible) {
        visible = isVisible;
        return this;
    }

    @Override
    public GuiButtonNop setIsFocused(boolean isFocused) {
        focused = isFocused;
        return this;
    }

    @Override
    public GuiComponentType getElementType() { return GuiComponentType.BUTTON; }


    @Override
    public GuiButtonNop setCustomFont(ClientProxy.FontContainer font) {
        customFont = font;
        return this;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) { return false; }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) { return false; }

    public Component[] getVariants() { return display; }


    public GuiButtonNop setVariants(Object ... variants) {
        List<Component> lines = new ArrayList<>();
        for (Object o : variants) {
            if (o == null) { lines.add(Component.empty()); }
            else if (o instanceof Component) { lines.add(((Component) o)); }
            else if (o instanceof Iterable<?>) {
                for (Object line : ((Iterable<?>) o)) {
                    if (line == null) { lines.add(Component.empty()); }
                    else if (line instanceof Component) { lines.add(((Component) line)); }
                    else { lines.add(Component.translatable("" + line)); }
                }
            }
            else { lines.add(Component.translatable("" + o)); }
        }
        display = lines.toArray(new Component[0]);
        displayValue = display.length == 0 ? 0 : ((displayValue % display.length) + display.length) % display.length;
        if (displayValue < display.length) {
            setDisplayText(display[displayValue]);
        }
        return this;
    }

    public int getState() {
        boolean hover = isHovered && (listener == null || !listener.hasSubGui());
        boolean lbm = Mouse.isButtonDown(0) && hover;
        if (texture == null) {
            int i = 1;
            if (!enabled) { i = 0; }
            else if (hover) { i = 2; }
            return i;
        }
        if (isAnim) {
            if (!enabled) { return 1; }
            return hover ? lbm ? 3 : 2 : 0;
        }
        if (isSimple) {
            int i = 0;
            if (!enabled) { i = 2; }
            else if (hover) { i = lbm ? 2 : 1; }
            return i;
        }
        if (hover) {
            return (enabled ? 1 : 4) + (lbm ? 1 : 0);
        }
        return enabled ? 0 : 3;
    }

    public GuiButtonNop setColor(int color) {
        packedFGColor = color;
        return this;
    }

    public GuiButtonNop setStacks(ItemStack... stacks) {
        if (renderStacks != null && stacks != null) { wait = 160; }
        renderStacks = stacks;
        renderStackID = renderStacks != null ? 0 : -1;
        ticks = 0;
        return this;
    }

    public ItemStack[] getStacks() { return renderStacks; }

    public GuiButtonNop setCurrentStackPos(int pos) {
        if (renderStacks != null && pos >= 0 && pos < renderStacks.length) {
            renderStackID = pos;
            wait = 160;
            ticks = 0;
        }
        return this;
    }

    public GuiButtonNop setShowShadow(boolean shadow) {
        showShadow = shadow;
        return this;
    }

    public GuiButtonNop setTexture(ResourceLocation resource) {
        texture = resource;
        return this;
    }

    public GuiButtonNop setUV(int u, int v, int width, int height) {
        txrX = u;
        txrY = v;
        txrW = width;
        txrH = height;
        return this;
    }

    public GuiButtonNop setIsAnim(boolean bo) {
        isAnim = bo;
        isSimple = !bo;
        return this;
    }

    @SuppressWarnings("unused")
    public void offsetHover(int x, int y) {
        offsetHoverX = x;
        offsetHoverY = y;
    }

    // for 1.12.2
    public boolean isActive() { return visible && active; }

    @SideOnly(Side.CLIENT)
    public interface OnPress { void onPress(GuiButtonNop button); }

    public boolean isHoveredOrFocused() { return isHovered || focused; }

    public void setHovered(boolean bo) { isHovered = bo; }

    @Override
    public boolean isFocused() { return getElementType().isSelectable() && focused; }

    @Override
    public boolean keyPressed(char typedChar, int keyCode) {
        if (visible && enabled && focused) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) { // Enter
                onClick(0.0d, 0.0d);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (enabled && visible) {
            if (isValidClickButton(mouseButton)) {
                boolean flag = clicked(mouseX, mouseY);
                if (flag) {
                    if (mouseButton == 0) { onClick(mouseX, mouseY); }
                    if (listener != null) { listener.mouseButtonEvent(this, mouseButton); }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void tick() { }

    @Override
    public boolean isHovered() { return isHovered; }

    protected boolean isValidClickButton(int mouseButton) { return true; }

    public int getX() { return x; }

    public void setX(int xIn) { x = xIn; }

    public int getY() { return y; }

    public void setY(int yIn) { y = yIn; }

    public int getHeight() { return height; }

    public void setHeight(int heightIn) { height = heightIn; }

    public int getWidth() { return width; }

    public void setWidth(int widthIn) { width = widthIn; }

}
