package noppes.npcs.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import noppes.npcs.containers.ContainerEntityInventory;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.GuiBasicContainer;

import javax.annotation.Nonnull;

public class GuiEntityInventory extends GuiBasicContainer<ContainerEntityInventory> {

    public GuiEntityInventory(ContainerEntityInventory container, Inventory inv, Component titleIn) {
        super(container, inv, titleIn);
        setBackground("companioninv.png");
        imageWidth = 172;
        inventoryLabelX = 167;
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);
        PoseStack matrixStack = graphics.pose();

        matrixStack.pushPose();
        for (int slotId = 0; slotId < 6; ++slotId) {
            Slot slot = menu.getSlot(slotId);
            graphics.blit(GuiBasic.RESOURCE_SLOT,
                    guiLeft + slot.x - 1, guiTop + slot.y - 1, 0, 0, 18, 18);
        }
        matrixStack.popPose();
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (menu.entity == null) { onClose(); }
        else { drawNpc(graphics, menu.entity, 50, 72, 1.0f, 0, 0, 0); }
    }

}
