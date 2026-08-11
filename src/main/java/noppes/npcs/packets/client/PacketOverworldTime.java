package noppes.npcs.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.client.Client;
import noppes.npcs.shared.common.PacketBasic;

public class PacketOverworldTime extends PacketBasic {

    protected static int channelId;
    public long overworldTime;

    public PacketOverworldTime() { }

    public PacketOverworldTime(long overworldTimeIn) { overworldTime = overworldTimeIn; }

    @Override
    public void decode(FriendlyByteBuf buf) { overworldTime = buf.readLong(); }

    @Override
    public void encode(FriendlyByteBuf buf) { buf.writeLong(overworldTime); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() { Client.processPacket(this); }

}
