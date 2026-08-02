package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.common.PacketBasic;

public class PacketMarcetRemove extends PacketBasic {

    protected static int channelId;
    private int marcetID;

    public PacketMarcetRemove() { }

    public PacketMarcetRemove(int marcetIDIn) { marcetID = marcetIDIn; }

    @Override
    public void decode(FriendlyByteBuf buf) { marcetID = buf.readInt(); }

    @Override
    public void encode(FriendlyByteBuf buf) { buf.writeInt(marcetID); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (!MarcetController.hasLocalServerData()) { MarcetController.getInstance().removeMarcet(marcetID); }
        if (Minecraft.getMinecraft().currentScreen instanceof IGuiData) { ((IGuiData) Minecraft.getMinecraft().currentScreen).setGuiData(new NBTTagCompound()); }
        CustomNpcs.debugData.end("Packets");
    }

}
