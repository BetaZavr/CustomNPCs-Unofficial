package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.data.Deal;
import noppes.npcs.controllers.data.Marcet;
import noppes.npcs.controllers.data.MarcetSection;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SPacketDealDropSetSave extends PacketServerBasic {

    protected static int channelId;
    private final int marcetId;
    private final int dealId;
    private final CompoundTag data;

    public SPacketDealDropSetSave(int marcetIDIn, int dealIDIn, CompoundTag dataIn) {
        marcetId = marcetIDIn;
        dealId = dealIDIn;
        data = dataIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_MARKETS); }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketDealDropSetSave msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.marcetId);
        buf.writeInt(msg.dealId);
        buf.writeNbt(msg.data);
    }

    public static SPacketDealDropSetSave decode(FriendlyByteBuf buf) {
        return new SPacketDealDropSetSave(buf.readInt(), buf.readInt(), buf.readNbt());
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        MarcetController mData = MarcetController.getInstance();
        Deal dealIn = mData.deals.get(dealId);
        if (dealIn != null && dealIn.isCase()) {
            int pos = data.getInt("Slot");
            if (pos < 0) {
                dealIn.addCaseItem(ItemStack.EMPTY, 100.0d).load(data);
            }
            else {
                DropSet[] drops = dealIn.getCaseItems();
                if (pos < drops.length) { drops[pos].load(data); }
            }
            for (Marcet marcet : new ArrayList<>(mData.markets.values())) {
                boolean nasInMarcet = false;
                for (MarcetSection section : new ArrayList<>(marcet.sections.values())) {
                    for (Deal deal : new ArrayList<>(section.deals)) {
                        if (deal.getId() == data.getInt("DealID")) {
                            nasInMarcet = true;
                            deal.load(dealIn.saveData());
                        }
                    }
                }
                if (nasInMarcet) { marcet.updateNew(); }
            }
            mData.save();
        }
        CustomNpcs.debugData.end("Packets");
    }

}