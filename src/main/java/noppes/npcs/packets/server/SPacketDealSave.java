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
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SPacketDealSave extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag data;

    public SPacketDealSave(CompoundTag dataIn) { data = dataIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_MARKETS); }

    public static void encode(SPacketDealSave msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static SPacketDealSave decode(FriendlyByteBuf buf) { return new SPacketDealSave(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        MarcetController mData = MarcetController.getInstance();
        if (data.contains("DealID", 3)) {
            mData.loadDeal(data);
            for (Marcet marcet : new ArrayList<>(mData.markets.values())) {
                boolean nasInMarcet = false;
                for (MarcetSection section : new ArrayList<>(marcet.sections.values())) {
                    for (Deal deal : new ArrayList<>(section.deals)) {
                        if (deal.getId() == data.getInt("DealID")) {
                            nasInMarcet = true;
                            deal.load(data);
                        }
                    }
                }
                if (nasInMarcet) { marcet.updateNew(); }
            }
            mData.save();
            mData.sendToAll();
        }
        CustomNpcs.debugData.end("Packets");
    }

}