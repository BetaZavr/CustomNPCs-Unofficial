package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiClose;

import java.util.List;

public class SPacketPermissionsAdd extends PacketServerBasic {

    protected static int channelId;
    private final String name;
    private final String node;

    public SPacketPermissionsAdd(String nameIn, String nodeIn) {
        name = nameIn;
        node = nodeIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    public static void encode(SPacketPermissionsAdd msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name);
        buf.writeUtf(msg.node);
    }

    public static SPacketPermissionsAdd decode(FriendlyByteBuf buf) { return new SPacketPermissionsAdd(buf.readUtf(), buf.readUtf()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_PERMISSION)) {
            permission(CustomNpcsPermissions.GLOBAL_PERMISSION.getNodeName());
            Packets.send(player, new PacketGuiClose());
        }
        else { CustomNpcsPermissions.add(node, name, player); }
        CustomNpcs.debugData.end("Packets");
    }

}