package noppes.npcs.client.gui.roles;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.AuctionHouse;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketAuctionsGet;
import noppes.npcs.roles.RoleAuctioneer;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubGuiRoleAuctioneer extends GuiNPCInterface implements IGuiData, ICustomScrollListener {

    protected final Map<Component, Integer> data = new HashMap<>();
    protected GuiCustomScrollNop scroll;
    protected Component select = Component.empty();
    public int id;

    public SubGuiRoleAuctioneer(EntityNPCInterface npc) {
        super(npc);
        setBackground("menubg.png");
        imageWidth = 190;
        imageHeight = 217;

        id = ((RoleAuctioneer) npc.role).getAuctionId();
        Packets.sendServer(new SPacketAuctionsGet());
    }

    @Override
    public void init() {
        super.init();
        List<Component> list = new ArrayList<>();
        data.clear();
        for (AuctionHouse auction : AuctionController.getInstance().auctionHouses.values()) {
            if (auction != null) {
                Component name = auction.getKey();
                list.add(name);
                data.put(name, auction.getId());
                if (id == auction.getId()) { select = name; }
            }
        }
        if (scroll == null) { scroll = addScroll(0).setSize(170, 157); }
        int x = guiLeft + 12, y = guiTop + 14;
        scroll.setNormalList(list);
        if (data.containsValue(id) && !select.getString().isEmpty()) { scroll.setSelected(select); }
        add(scroll.setPos(x, y));
        addLabel(0, x + 2, y - 10, "auctioneer.select")
                .setSize(170, 12)
                .setHoverTexts("auctioneer.hover.role.list");
        addButton(66, guiLeft + 50, guiTop + 190, "gui.done")
                .setSize(90, 20)
                .setHoverTexts("hover.back");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (background != null) { // add right
            PoseStack matrixStack = graphics.pose();
            matrixStack.pushPose();
            matrixStack.translate(guiLeft, guiTop, 0.0f);
            matrixStack.scale(bgScale, bgScale, bgScale);
            graphics.blit(background, imageWidth, 0, 252, 0, 4, imageHeight);
            matrixStack.popPose();
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        if (button.id == 66) { onClose(); }
    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {
        if (scroll.getSelected().equals(select.getString()) || !data.containsKey(scroll.getNormalSelected())) { return; }
        select = scroll.getNormalSelected();
        id = data.get(scroll.getNormalSelected());
        init();
    }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) { onClose(); }

    @Override
    public void setGuiData(CompoundTag compound) { init(); }

}
