package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.common.PacketBasic;

public class PacketMarcetData extends PacketBasic {

    protected static int channelId;
    private NBTTagCompound data;

    public PacketMarcetData() { }

    public PacketMarcetData(NBTTagCompound dataIn) { data = dataIn; }

    @Override
    public void decode(FriendlyByteBuf buf) { data = buf.readNbt(); }

    @Override
    public void encode(FriendlyByteBuf buf) { buf.writeNbt(data); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (!MarcetController.hasLocalServerData()) { MarcetController.getInstance().loadMarcet(data); }
        if (Minecraft.getMinecraft().currentScreen instanceof IGuiData) { ((IGuiData) Minecraft.getMinecraft().currentScreen).setGuiData(new NBTTagCompound()); }
        CustomNpcs.debugData.end("Packets");
    }

}