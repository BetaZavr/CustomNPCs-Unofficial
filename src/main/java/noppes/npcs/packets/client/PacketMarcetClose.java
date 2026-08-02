package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.player.GuiNPCTrader;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.data.Marcet;
import noppes.npcs.shared.common.PacketBasic;

public class PacketMarcetClose extends PacketBasic {

    protected static int channelId;
    private int marcetID;

    public PacketMarcetClose() { }

    public PacketMarcetClose(int marcetIDIn) { marcetID = marcetIDIn; }

    @Override
    public void decode(FriendlyByteBuf buf) { marcetID = buf.readInt(); }

    @Override
    public void encode(FriendlyByteBuf buf) { buf.writeInt(marcetID); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Marcet m = MarcetController.getInstance().getMarcet(marcetID);
        if (m != null) {
            if (Minecraft.getMinecraft().currentScreen instanceof GuiNPCTrader &&
                    GuiNPCTrader.marcet != null &&
                    GuiNPCTrader.marcet.getId() == marcetID) { ((GuiNPCTrader) Minecraft.getMinecraft().currentScreen).onClose(); }
        }
        CustomNpcs.debugData.end("Packets");
    }

}