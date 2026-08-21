package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.containers.ContainerCustomGui;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketCustomGuiKeyPressed extends PacketServerBasic {

    protected static int channelId;
    private final int keyId;

    public SPacketCustomGuiKeyPressed(int keyIdIn) { keyId = keyIdIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketCustomGuiKeyPressed msg, FriendlyByteBuf buf) { buf.writeInt(msg.keyId); }

    public static SPacketCustomGuiKeyPressed decode(FriendlyByteBuf buf) { return new SPacketCustomGuiKeyPressed(buf.readInt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (player.containerMenu instanceof ContainerCustomGui container) {
            EventHooks.onCustomGuiKeyPressed(iPlayer, container.activeGui, keyId);
        }
        CustomNpcs.debugData.end("Packets");
    }

}