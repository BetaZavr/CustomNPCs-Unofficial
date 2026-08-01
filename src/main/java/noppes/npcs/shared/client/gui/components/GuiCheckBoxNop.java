package noppes.npcs.shared.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

import javax.annotation.Nonnull;

public class GuiCheckBoxNop extends GuiButtonNop {

    private static final ResourceLocation TEXTURE = new ResourceLocation(CustomNpcs.MODID, "textures/gui/checkbox.png");
    protected boolean selected;
    protected Component trueLabel;
    protected Component falseLabel;

    public GuiCheckBoxNop(IGuiInterface gui, int id, int x, int y, Object trueLabel, Object falseLabel, boolean select) {
        super(gui, id, Component.empty(), x, y, null);
        setSize(120, 14);
        textColor = CustomNpcs.LableColor.getRGB();
        showShadow = false;
        selected = select;
        setText(trueLabel, falseLabel);
    }

    @Override
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
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) getX(), (float) getY(), 0.0f);
        float s = (float) height / 80.0f;
        GlStateManager.scale(s, s, 1.0f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        drawTexturedModalRect(0, 0, isHovered ? 80 : 0, selected ? 80 : 0, 80, 80);
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        renderString(getMessage(), getX() + height + 1, getY(), getX() + width - 1, getY() + height,
                getFGColor() | (int) Math.ceil(alpha * 255.0F) << 24, showShadow, false, customFont);
    }

    @Override
    public @Nonnull Component getMessage() { return selected ? trueLabel : falseLabel; }

    @Override
    public int getFGColor() {
        if (packedFGColor != 0) { return packedFGColor; }
        else if (!enabled) { return CustomNpcs.NotEnableColor.getRGB(); }
        else if (isHovered) { return CustomNpcs.HoverColor.getRGB(); }
        return CustomNpcs.LableColor.getRGB();
    }

    @Override
    protected void onClick(double x, double y) {
        if (enabled && (listener == null || !listener.hasSubGui())) {
            if (display != null && display.length != 0) { setDisplay((displayValue + 1) % display.length); }
            if (hasSound) { Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)); }
            selected = !selected;
            if (onPress != null) { onPress.onPress(this); }
            else if (listener != null) { listener.buttonEvent(this); }
        }
    }

    @Override
    protected boolean isValidClickButton(int mouseButton) { return mouseButton == 0; }

    public boolean selected() { return selected; }

    public void setColor(int newTextColor, boolean isShowShadow) {
        textColor = newTextColor;
        packedFGColor = newTextColor;
        showShadow = isShowShadow;
    }

    public void setSelected(boolean select) { selected = select; }

    public void setText(Object trueText, Object falseText) {
        trueLabel = trueText == null ? Component.empty() :
                trueText instanceof Component ? (Component) trueText :
                        trueText instanceof ITextComponent ? new Component((ITextComponent) trueText) :
                                Component.translatable(trueText.toString());
        falseLabel = falseText == null ||
                (falseText instanceof Component && ((Component) falseText).getString().isEmpty()) ||
                (falseText instanceof ITextComponent && ((ITextComponent) falseText).getFormattedText().isEmpty()) ||
                falseText.toString().isEmpty() ? trueLabel :
                falseText instanceof Component ? (Component) falseText :
                        falseText instanceof ITextComponent ? new Component((ITextComponent) falseText) :
                        Component.translatable(falseText.toString());
    }

}
