package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SPacketAuctionsGet extends PacketServerBasic {

    protected static int channelId;

    public SPacketAuctionsGet() { }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return new ArrayList<>(Collections.singletonList(CustomNpcsPermissions.GLOBAL_AUCTIONS)); }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketAuctionsGet ignoreMsg, FriendlyByteBuf ignoreBuf) { }

    public static SPacketAuctionsGet decode(FriendlyByteBuf ignoreBuf) { return new SPacketAuctionsGet(); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        AuctionController.getInstance().sendAllTo(player);
        CustomNpcs.debugData.end("Packets");
    }

}