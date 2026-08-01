package noppes.npcs.shared.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

public class GuiMenuTopButton extends GuiButtonNop {

    public static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/menutopbutton.png");
    public boolean rotated = false;

    public GuiMenuTopButton(IGuiInterface gui, int id, Object label, int x, int y) {
        super(gui, id, label, x, y, null);
        active = false;
        height = 20;
        width = Minecraft.getMinecraft().fontRenderer.getStringWidth(getMessage().getFormattedText()) + 12;
    }

    @Override
    public void renderWidget(int mouseX, int mouseY, float partialTicks) {
        isHovered = false;
        if (visible) {
            GlStateManager.pushMatrix();
            GlStateManager.enableDepth();
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            GlStateManager.enableBlend();
            Minecraft mc = Minecraft.getMinecraft();
            mc.getTextureManager().bindTexture(resource);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            int h = height - (active ? 0 : 2);
            isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + h;
            int state = active ? 0 : (isHovered ? 2 : 1);
            drawTexturedModalRect(getX(), getY(), 0, state * 20, getWidth() / 2, h);
            drawTexturedModalRect(getX() + getWidth() / 2, getY(), 200 - getWidth() / 2, state * 20, getWidth() / 2, h);
            if (rotated) { GlStateManager.rotate(90.0F, 1.0f, 0.0f, 0.0f); }
            renderString(getMessage(), getX() + 2, getY(), getX() + getWidth() - 2, getY() + getHeight(),
                    getFGColor() | (int) Math.ceil(alpha * 255.0F) << 24, showShadow, true, customFont);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void onClick(double x, double y) {
        if (hasSound) { Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)); }
        if (onPress != null) { onPress.onPress(this); }
        else if (listener != null) { listener.buttonEvent(this); }
    }

    // New from Unofficial (BetaZavr)
    @Override
    public int getFGColor() {
        if (packedFGColor != 0) { return packedFGColor; }
        else if (isHovered) { return CustomNpcs.HoverColor.getRGB(); }
        return CustomNpcs.ButtonColor.getRGB();
    }

    @Override
    public GuiMenuTopButton setIsEnabled(boolean bo) {
        enabled = bo;
        return this;
    }

    @Override
    public GuiComponentType getElementType() { return GuiComponentType.TOP_BUTTON; }

}