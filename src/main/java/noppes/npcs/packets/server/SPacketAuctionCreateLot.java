package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.AuctionLot;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;
import java.util.Objects;

public class SPacketAuctionCreateLot extends PacketServerBasic {

    protected static int channelId;
    private final int auctionId;
    private final int count;
    private final AuctionLot lot;

    public SPacketAuctionCreateLot(int auctionIdIn, AuctionLot lotIn, int countIn) {
        auctionId = auctionIdIn;
        count = countIn;
        lot = lotIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketAuctionCreateLot msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.auctionId);
        buf.writeInt(msg.count);
        buf.writeNbt(msg.lot.save());
    }

    public static SPacketAuctionCreateLot decode(FriendlyByteBuf buf) {
        int auctionIdIn = buf.readInt();
        int countIn = buf.readInt();
        AuctionLot lotIn = new AuctionLot(0L);
        lotIn.load(Objects.requireNonNull(buf.readNbt()));
        return new SPacketAuctionCreateLot(auctionIdIn, lotIn, countIn);
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        AuctionController.getInstance().createLots(player, lot, auctionId, count);
        CustomNpcs.debugData.end("Packets");
    }

}