package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketAuctionBid extends PacketServerBasic {

    protected static int channelId;
    private final int auctionId;
    private final long lotId;

    public SPacketAuctionBid(int auctionIdIn, long lotIdIn) {
        auctionId = auctionIdIn;
        lotId = lotIdIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketAuctionBid msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.auctionId);
        buf.writeLong(msg.lotId);
    }

    public static SPacketAuctionBid decode(FriendlyByteBuf buf) { return new SPacketAuctionBid(buf.readInt(), buf.readLong()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        AuctionController controller = AuctionController.getInstance();
        if (controller.getAuction(auctionId) != null) { controller.bid(player, auctionId, lotId); }
        CustomNpcs.debugData.end("Packets");
    }

}