package noppes.npcs.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.command.CmdHeapAnalyzer;
import noppes.npcs.shared.common.PacketBasic;

public class PacketHeapAnalyzer extends PacketBasic {

    protected static int channelId;

    public enum State { START, STOP, MANUAL }

    private final State type;
    private final int count;

    public PacketHeapAnalyzer(State typeIn, int countIn) {
        type = typeIn;
        count = countIn;
    }

    public static void encode(PacketHeapAnalyzer msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeInt(msg.count);
    }

    public static PacketHeapAnalyzer decode(FriendlyByteBuf buf) {
        return new PacketHeapAnalyzer(buf.readEnum(State.class), buf.readInt());
    }

    @Override
    public int getChannelId() {
        return channelId;
    }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        switch (type) {
            case START -> CmdHeapAnalyzer.startTracking(null, count);
            case STOP -> CmdHeapAnalyzer.stopTracking(null, count);
            case MANUAL -> CmdHeapAnalyzer.manual(null, count);
        }
        CustomNpcs.debugData.end("Packets");
    }

}