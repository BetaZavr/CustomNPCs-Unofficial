package noppes.npcs.shared.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.util.EnumAlignText;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nonnull;

public class GuiCheckBoxNop extends GuiButtonNop {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");
    protected boolean selected;
    protected Component trueLabel;
    protected Component falseLabel;

    public GuiCheckBoxNop(IGuiInterface gui, int id, int x, int y, Object trueLabel, Object falseLabel, boolean select) {
        super(gui, id, x, y, 120, 14, Component.empty());
        textColor = CustomNpcs.LableColor.getRGB();
        showShadow = false;
        alignText = EnumAlignText.LEFT;
        selected = select;
        setText(trueLabel, falseLabel);
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) { return; }
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (isHovered && !hoverText.isEmpty()) { listener.setHoverText(hoverText); }
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (offsetHoverX != 0 || offsetHoverY != 0) {
            mouseX -= offsetHoverX;
            mouseY -= offsetHoverY;
        }
        isHovered = visible && mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
        if (!visible) { return; }
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        RenderSystem.enableBlend();
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate((float) getX(), (float) getY(), 0.0f);
        float s = (float) height / 20.0f;
        matrixStack.scale(s, s, 1.0f);
        graphics.blit(TEXTURE, 0, 0, isFocused() ? 20.0F : 0.0F, selected ? 20.0F : 0.0F, 20, 20, 64, 64);
        matrixStack.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        renderString(graphics, getMessage(), getX() + height + 1, getY(), getX() + width - 1, getY() + height,
                getFGColor() | Mth.ceil(alpha * 255.0F) << 24, showShadow, alignText, false, customFont);
    }

    @Override
    public int getFGColor() {
        if (packedFGColor != -1) { return packedFGColor; }
        else if (!active) { return CustomNpcs.NotEnableColor.getRGB(); }
        else if (isHovered) { return CustomNpcs.HoverColor.getRGB(); }
        return textColor;
    }

    @Override
    public @Nonnull Component getMessage() { return selected ? trueLabel : falseLabel; }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, createNarrationMessage());
        if (!active) { return; }
        narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage." + (isFocused() ? "focused" : "hovered")));
    }

    public boolean selected() { return selected; }

    public void setColor(int newTextColor, boolean isShowShadow) {
        LogWriter.info("[DEBUG] "+id+"; "+textColor);
        textColor = newTextColor;
        showShadow = isShowShadow;
    }

    public void setSelected(boolean select) { selected = select; }

    public void setText(Object trueText, Object falseText) {
        trueLabel = trueText == null ? Component.empty() :
                trueText instanceof Component component ? component :
                        Component.translatable(trueText.toString());
        falseLabel = falseText == null ||
                (falseText instanceof Component component && component.getString().isEmpty()) ||
                falseText.toString().isEmpty() ? trueLabel :
                falseText instanceof Component component ? component :
                        Component.translatable(falseText.toString());
    }

}
