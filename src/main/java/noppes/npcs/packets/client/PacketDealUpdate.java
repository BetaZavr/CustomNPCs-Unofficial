package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.data.Deal;
import noppes.npcs.controllers.data.Marcet;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.common.PacketBasic;

public class PacketDealUpdate extends PacketBasic {

    protected static int channelId;
    private int marcetID;
    private NBTTagCompound dealData;

    public PacketDealUpdate() { }

    public PacketDealUpdate(int marcetIDIn, NBTTagCompound dealDataIn) {
        marcetID = marcetIDIn;
        dealData = dealDataIn;
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        marcetID = buf.readInt();
        dealData = buf.readNbt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(marcetID);
        buf.writeNbt(dealData);
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (!MarcetController.hasLocalServerData()) {
            Marcet marcet = MarcetController.getInstance().getMarcet(marcetID);
            if (marcet != null) {
                Deal deal = marcet.getDeal(dealData.getInteger("DealID"));
                if (deal != null) { deal.loadData(dealData); }
            }
        }
        if (Minecraft.getMinecraft().currentScreen instanceof IGuiData) { ((IGuiData) Minecraft.getMinecraft().currentScreen).setGuiData(new NBTTagCompound()); }
        CustomNpcs.debugData.end("Packets");
    }

}