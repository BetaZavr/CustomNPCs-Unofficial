package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.data.Deal;
import noppes.npcs.controllers.data.Marcet;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.common.PacketBasic;

public class PacketDealUpdate extends PacketBasic {

    protected static int channelId;
    private final int marcetID;
    private final CompoundTag dealData;

    public PacketDealUpdate(int marcetIDIn, CompoundTag dealDataIn) {
        marcetID = marcetIDIn;
        dealData = dealDataIn;
    }

    public static void encode(PacketDealUpdate msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.marcetID);
        buf.writeNbt(msg.dealData);
    }

    public static PacketDealUpdate decode(FriendlyByteBuf buf) { return new PacketDealUpdate(buf.readInt(), buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Marcet marcet = MarcetController.getInstance().getMarcet(marcetID);
        if (marcet != null) {
            Deal deal = marcet.getDeal(dealData.getInt("DealID"));
            if (deal != null) { deal.loadData(dealData); }
        }
        if (Minecraft.getInstance().screen instanceof IGuiData gui) { gui.setGuiData(new CompoundTag()); }
        CustomNpcs.debugData.end("Packets");
    }

}