package noppes.npcs.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.shared.common.PacketBasic;

public class PacketClearMarcets extends PacketBasic {

    protected static int channelId;

    @Override
    public void decode(FriendlyByteBuf buf) { }

    @Override
    public void encode(FriendlyByteBuf buf) { }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (!MarcetController.hasLocalServerData()) {
            MarcetController.getInstance().markets.clear();
            MarcetController.getInstance().deals.clear();
        }
        CustomNpcs.debugData.end("Packets");
    }

}
