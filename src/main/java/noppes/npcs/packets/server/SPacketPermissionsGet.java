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

public class SPacketPermissionsGet extends PacketServerBasic {

    protected static int channelId;

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    public static void encode(SPacketPermissionsGet ignoredMsg, FriendlyByteBuf ignoredBuf) { }

    public static SPacketPermissionsGet decode(FriendlyByteBuf ignoredBuf) { return new SPacketPermissionsGet(); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_PERMISSION)) {
            permission(CustomNpcsPermissions.GLOBAL_PERMISSION.getNodeName());
            Packets.send(player, new PacketGuiClose());
        }
        else { CustomNpcsPermissions.sendTo(player); }
        CustomNpcs.debugData.end("Packets");
    }

}