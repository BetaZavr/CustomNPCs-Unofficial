package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.AuctionHouse;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketAuctionClose extends PacketServerBasic {

    protected static int channelId;
    private final int auctionId;

    public SPacketAuctionClose(int auctionIdIn) { auctionId = auctionIdIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketAuctionClose msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.auctionId);
    }

    public static SPacketAuctionClose decode(FriendlyByteBuf buf) { return new SPacketAuctionClose(buf.readInt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        AuctionHouse auction = AuctionController.getInstance().getAuction(auctionId);
        if (auction != null) { auction.removePlayer(player); }
        CustomNpcs.debugData.end("Packets");
    }

}