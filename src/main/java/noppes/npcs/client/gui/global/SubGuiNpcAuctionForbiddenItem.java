package noppes.npcs.client.gui.global;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import noppes.npcs.api.handler.data.IForbiddenItem;
import noppes.npcs.containers.ContainerAuctionForbiddenItem;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketAuctionSave;
import noppes.npcs.shared.client.gui.GuiBasicContainer;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

import javax.annotation.Nonnull;

public class SubGuiNpcAuctionForbiddenItem extends GuiBasicContainer<ContainerAuctionForbiddenItem> {

    public SubGuiNpcAuctionForbiddenItem(ContainerAuctionForbiddenItem container, Inventory inv, Component ignoredTitle) {
        super(container, inv, Component.empty());
        imageWidth = 200;
        imageHeight = 166;
    }

    @Override
    public void init() {
        super.init();
        int x = guiLeft;
        int y = guiTop;
        addLabel(0, x + 5, y + 5, Component.translatable("auction.forbidden.title"));
        y += 20;
        IForbiddenItem[] forbidden = menu.auction.getForbiddenItems();
        for (int i = 0; i < forbidden.length; i++) {
            if (!forbidden[i].getItem().isEmpty()) {
                addLabel(10 + i, x + 5, y + i * 14, forbidden[i].getItem().getDisplayName());
            }
        }
        addButton(66, x + 5, y + imageHeight - 30, "gui.done").setSize(55, 20);
    }

    @Override
    public void buttonEvent(@Nonnull GuiButtonNop button) {
        if (button.id == 66) { onClose(); }
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics) {
        super.renderBackground(graphics);
    }

    @Override
    public void save() {
        Packets.sendServer(new SPacketAuctionSave(menu.auction.getId(), menu.auction.save()));
    }

}