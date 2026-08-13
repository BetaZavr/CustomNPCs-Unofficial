package noppes.npcs.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketBasic;

public class PacketOverworldTime extends PacketBasic {

    protected static int channelId;
    private final long overworldTime;

    public PacketOverworldTime(long overworldTimeIn) { overworldTime = overworldTimeIn; }

    public static void encode(PacketOverworldTime msg, FriendlyByteBuf buf) { buf.writeLong(msg.overworldTime); }

    public static PacketOverworldTime decode(FriendlyByteBuf buf) { return new PacketOverworldTime(buf.readLong()); }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        PlayerData.get(player).questData.overworldTime = overworldTime;
        CustomNpcs.debugData.end("Packets");
    }

}
