package noppes.npcs.client.gui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.network.chat.Component;
import noppes.npcs.containers.ContainerEntityInventory;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.GuiBasicContainer;

public class GuiEntityInventory extends GuiBasicContainer<ContainerEntityInventory> {

    public GuiEntityInventory(ContainerEntityInventory container) {
        super(container, Component.empty());
        setBackground("companioninv.png");
        xSize = 172;
        ySize = 167;
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        GlStateManager.pushMatrix();
        mc.getTextureManager().bindTexture(GuiBasic.RESOURCE_SLOT);
        for (int slotId = 0; slotId < 6; ++slotId) {
            Slot slot = menu.getSlot(slotId);
            drawTexturedModalRect(guiLeft + slot.xPos - 1, guiTop + slot.yPos - 1, 0, 0, 18, 18);
        }
        GlStateManager.popMatrix();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (menu.entity == null) { onClose(); }
        else { drawNpc(menu.entity, 50, 72, 1.0f, 0, 0, 0); }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

}
