package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketPermissionGlobal;

import java.util.List;

public class SPacketPermissionGlobalGet extends PacketServerBasic {

    protected static int channelId;

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    public static void encode(SPacketPermissionGlobalGet ignoredMsg, FriendlyByteBuf ignoredBuf) { }

    public static SPacketPermissionGlobalGet decode(FriendlyByteBuf ignoredBuf) { return new SPacketPermissionGlobalGet(); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Packets.send(player, new PacketPermissionGlobal(
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_BANK),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_FACTION),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_DIALOG),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_QUEST),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_TRANSPORT),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_PLAYERDATA),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_RECIPE),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_NATURALSPAWN),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_LINKED),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_MARKETS),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_AUCTIONS),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_MAIL),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_ELEMENTS),
                CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_DUNGEONS)
        ));
        CustomNpcs.debugData.end("Packets");
    }

}