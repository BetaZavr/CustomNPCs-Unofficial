package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketHudTimerEnd extends PacketServerBasic {

    protected static int channelId;
    private final int orientationType;
    private final int timerId;

    public SPacketHudTimerEnd(int orientationIn, int timerIn) {
        orientationType = orientationIn;
        timerId = timerIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketHudTimerEnd msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.orientationType);
        buf.writeInt(msg.timerId);
    }

    public static SPacketHudTimerEnd decode(FriendlyByteBuf buf) { return new SPacketHudTimerEnd(buf.readInt(), buf.readInt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        PlayerData data = PlayerData.get(player);
        EventHooks.onPlayerTimer(new PlayerEvent.TimerEvent(data.getIPlayer(), timerId));
        CustomNpcs.debugData.end("Packets");
    }

}
