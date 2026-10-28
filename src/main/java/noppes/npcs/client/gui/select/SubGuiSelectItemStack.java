package noppes.npcs.client.gui.select;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import noppes.npcs.shared.client.gui.GuiBasic;

import java.awt.*;
import java.util.List;

public class SubGuiSelectItemStack extends GuiBasic {

    protected int hoverPos = -2;
    public final int id;
    public ItemStack stack;

    public SubGuiSelectItemStack(int idIn, ItemStack item) {
        super();
        setBackground("followerhire.png");
        imageWidth = 176;
        imageHeight = 166;
        closeOnEsc = true;

        id = idIn;
        stack = item;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        PoseStack matrixStack = graphics.pose();
        matrixStack.translate(0.0f, 0.0f, -300.0f);
        super.render(graphics, mouseX, mouseY, partialTicks);

        List<Component> list = null;
        int x = guiLeft + 79;
        int y = guiTop + 38;
        hoverPos = -2;

        matrixStack.pushPose();
        graphics.blit(RESOURCE_SLOT, x, y, 0, 0, 18, 18);
        if (isMouseHover(mouseX, mouseY, x, y, 16, 16)) {
            hoverPos = -1;
            graphics.fill(x + 1, y + 1, x + 17, y + 17, new Color(0x80FFFFFF).getRGB());
            if (stack != null && !stack.isEmpty()) {
                list = stack.getTooltipLines(player, minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
            }
        }
        if (stack != null && !stack.isEmpty()) {
            renderStackInGUI(graphics, stack, x + 1.0f, y + 1.0f, 0.0f, 1.0f, null);
        }

        for (int i = 0; i < player.getInventory().items.size(); i ++) {
            ItemStack st = player.getInventory().items.get(i);
            x = guiLeft + 7 + (i % 9) * 18;
            y = guiTop + 83 + (i / 9) * 18;
            if (i < 9) { y += 58; } else { y -= 18; }
            if (isMouseHover(mouseX, mouseY, x, y, 16, 16)) {
                hoverPos = i;
                graphics.fill(x + 1, y + 1, x + 17, y + 17, new Color(0x80FFFFFF).getRGB());
                if (!st.isEmpty()) {
                    list = stack.getTooltipLines(player, minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
                }
            }
            if (st.isEmpty()) { continue; }
            renderStackInGUI(graphics, st, x + 1.0f, y + 1.0f, 0.0f, 1.0f, null);
        }
        matrixStack.popPose();
        if (list != null && !list.isEmpty()) {
            setHoverText(list);
            drawHoverText(null);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (!hasSubGui()) {
            if (hoverPos == -1) { stack = ItemStack.EMPTY; }
            else if (hoverPos >= 0) {
                stack = player.getInventory().items.get(hoverPos);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

}
