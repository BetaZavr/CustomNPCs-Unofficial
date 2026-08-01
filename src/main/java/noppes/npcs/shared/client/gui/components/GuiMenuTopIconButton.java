package noppes.npcs.shared.client.gui.components;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

import javax.annotation.Nonnull;

public class GuiMenuTopIconButton extends GuiMenuTopButton {

    private static final ResourceLocation resource = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    protected final ItemStack item;

    public GuiMenuTopIconButton(IGuiInterface gui, int id, Object label, int x, int y, @Nonnull ItemStack itemIn) {
        super(gui, id, label, x, y);
        item = itemIn.isEmpty() ? new ItemStack(Blocks.DIRT) : itemIn;
        setSize(28, 28);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            renderWidget(mouseX, mouseY, partialTicks);
            if (isHovered) {
                if (listener != null && !hoverText.isEmpty()) { listener.setHoverText(hoverText); }
                else { drawHoveringText(Collections.singletonList(getMessage()), mouseX, mouseY, Minecraft.getMinecraft().fontRenderer); }
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void renderWidget(int mouseX, int mouseY, float partialTicks) {
        isHovered = false;
        if (visible) {
            isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + height;
            GlStateManager.pushMatrix();
            GlStateManager.enableDepth();
            GlStateManager.enableBlend();
            Minecraft mc = Minecraft.getMinecraft();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            mc.getTextureManager().bindTexture(resource);
            drawTexturedModalRect(getX(), getY() + (active ? 2 : 0), 0, active ? 32 : 0, 28, 28);
            GlStateManager.translate(0.0F, 0.0F, 100.0F);
            mc.getRenderItem().renderItemAndEffectIntoGUI(item, getX() + 6, getY() + 10);
            mc.getRenderItem().renderItemOverlays(mc.fontRenderer, item, getX() + 6, getY() + 10);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    protected void drawHoveringText(List<Component> list, int x, int y, FontRenderer font) {
        if (!list.isEmpty()) {
            GlStateManager.disableDepth();
            int k = 0;
            Iterator<Component> var7 = list.iterator();
            int i1;
            while(var7.hasNext()) {
                Component o = var7.next();
                i1 = font.getStringWidth(o.getFormattedText());
                if (i1 > k) { k = i1; }
            }

            int k2 = y;
            i1 = 8;
            if (list.size() > 1) { i1 += 2 + (list.size() - 1) * 10; }

            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 300.0F);
            int j1 = -267386864;
            drawGradientRect(x - 3, y - 4, x + k + 3, y - 3, j1, j1);
            drawGradientRect(x - 3, y + i1 + 3, x + k + 3, y + i1 + 4, j1, j1);
            drawGradientRect(x - 3, y - 3, x + k + 3, y + i1 + 3, j1, j1);
            drawGradientRect(x - 4, y - 3, x - 3, y + i1 + 3, j1, j1);
            drawGradientRect(x + k + 3, y - 3, x + k + 4, y + i1 + 3, j1, j1);
            int k1 = 1347420415;
            int l1 = (k1 & 16711422) >> 1 | k1 & -16777216;
            drawGradientRect(x - 3, y - 3 + 1, x - 3 + 1, y + i1 + 3 - 1, k1, l1);
            drawGradientRect(x + k + 2, y - 3 + 1, x + k + 3, y + i1 + 3 - 1, k1, l1);
            drawGradientRect(x - 3, y - 3, x + k + 3, y - 3 + 1, k1, k1);
            drawGradientRect(x - 3, y + i1 + 2, x + k + 3, y + i1 + 3, l1, l1);

            for(int i2 = 0; i2 < list.size(); ++i2) {
                Component s1 = list.get(i2);
                drawString(font, s1.getFormattedText(), x, k2, -1);
                if (i2 == 0) { k2 += 2; }
                k2 += 10;
            }

            GlStateManager.popMatrix();
            GlStateManager.enableDepth();
        }
    }

}
