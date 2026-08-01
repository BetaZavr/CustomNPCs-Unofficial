package noppes.npcs.shared.client.gui.components;

import net.minecraft.network.chat.Component;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

import java.awt.*;

public class GuiColorButtonNop extends GuiButtonNop {

    protected int color;

    public GuiColorButtonNop(IGuiInterface gui, int id, int x, int y, int colorIn) {
        super(gui, id, Component.empty(), x, y, null);
        setColor(colorIn);
    }

    @Override
    public void renderWidget(int mouseX, int mouseY, float partialTicks) {
        isHovered = visible && mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
        if (!visible) { return; }
        int x = getX();
        int y = getY();
        drawRect(x, y, x + width, y + height, isHovered ? 0xFFFFFFFF : new Color(0x80, 0x80, 0x80, 0xFF).getRGB());
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, color);
    }

    @Override
    public GuiColorButtonNop setColor(int colorIn) {
        color = 0xFF000000 | colorIn & 0x00FFFFFF;
        return this;
    }

}