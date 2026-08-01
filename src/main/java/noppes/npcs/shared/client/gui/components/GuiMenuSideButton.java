package noppes.npcs.shared.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

public class GuiMenuSideButton extends GuiButtonNop {

    public static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/menusidebutton.png");

    // New from Unofficial (BetaZavr)
    protected boolean isRight = false;
    public int data;

    public GuiMenuSideButton(IGuiInterface gui, int id, Object label, int x, int y) {
        super(gui, id, label, x, y, clicked);
        setSize(70, 22);
    }

    @Override
    public void renderWidget(int mouseX, int mouseY, float partialTicks) {
        isHovered = false;
        if (!visible) { return; }
        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        GlStateManager.enableBlend();
        int w = width + (active ? isRight ? -2 : 2 : 0);
        isHovered = mouseX >= getX() - (isRight ? 0 : width) && mouseY >= getY() && mouseX < getX() + w - (isRight ? 0 : width) && mouseY < getY() + height;
        int state = active ? 0 : (isHovered ? 2 : 1) * 22;
        int h0 = height / 2;
        int h1 = height - h0;
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(resource);
        // background
        if (isRight) {
            state += 66;
            int i = 197 - width;
            drawTexturedModalRect(getX(), getY(), i, state, width, h0);
            drawTexturedModalRect(getX(), getY() + h0, i, (22 - h1) + state, width, h1);
            if (active || isHovered) {
                drawTexturedModalRect(getX() - 1, getY() + 1, i - 1, 1 + state, 1, h0 - 1);
                drawTexturedModalRect(getX() - 2, getY() + 2, i - 2, 2 + state, 1, h0 - 2);
                drawTexturedModalRect(getX() - 3, getY() + 3, i - 3, 3 + state, 1, h0 - 3);
                drawTexturedModalRect(getX() - 1, getY() + h0, i - 1, 22 + state - h1, 1, h1 - 1);
                drawTexturedModalRect(getX() - 2, getY() + h0, i - 2, 22 + state - h1, 1, h1 - 2);
                drawTexturedModalRect(getX() - 3, getY() + h0, i - 3, 22 + state - h1, 1, h1 - 3);
            }
        }
        else {
            drawTexturedModalRect(getX() - width, getY(), 0, state, width, h0);
            drawTexturedModalRect(getX() - width, getY() + h0, 0, (22 - h1) + state, width, h1);
            if (active || isHovered) {
                drawTexturedModalRect(getX(), getY() + 1, 194, 1 + state, 2, h0 - 1);
                drawTexturedModalRect(getX() + 2, getY() + 2, 196, 2 + state, 1, h0 - 2);
                drawTexturedModalRect(getX(), getY() + h0, 194, (22 - h1) + state, 2, h1 - 1);
                drawTexturedModalRect(getX() + 2, getY() + h0, 196, (22 - h1) + state, 2, h1 - 2);
            }
        }
        // Title
        renderString(getMessage(), getX() + (isRight ? 0 : 5 - width), getY(), getX() + (isRight ? width - 4 : - 4), getY() + height,
                getFGColor() | (int) Math.ceil(alpha * 255.0F) << 24, showShadow, true, customFont);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    // New from Unofficial (BetaZavr)
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (visible && isHovered && enabled && mouseButton == 0) {
            if (display != null && display.length != 0) { setDisplay((displayValue + 1) % display.length); }
            if (hasSound) { Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)); }
            if (onPress != null) { onPress(); }
            else if (listener != null) { listener.buttonEvent(this); }
            return true;
        }
        return enabled && super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) { return visible && enabled && isHovered; }

    @Override
    public GuiComponentType getElementType() { return GuiComponentType.SIDE_BUTTON; }

    public GuiMenuSideButton setIsRight(boolean right) {
        isRight = right;
        return this;
    }

}
