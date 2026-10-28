package noppes.npcs.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.BorderController;
import noppes.npcs.shared.common.PacketBasic;

public class PacketBorderData extends PacketBasic {

    protected static int channelId;
    private final CompoundTag data;

    public PacketBorderData(CompoundTag dataIn) { data = dataIn; }

    public static void encode(PacketBorderData msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static PacketBorderData decode(FriendlyByteBuf buf) { return new PacketBorderData(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        BorderController.getInstance().loadRegion(data);
        CustomNpcs.debugData.end("Packets");
    }

}