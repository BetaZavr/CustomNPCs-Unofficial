package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.player.GuiNpcAuction;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.AuctionHouse;
import noppes.npcs.shared.common.PacketBasic;

public class PacketAuctionLotRemoved extends PacketBasic {

    protected static int channelId;
    protected final int auctionId;
    protected final int lotId;

    public PacketAuctionLotRemoved(int auctionIdIn, int lotIdIn) {
        auctionId = auctionIdIn;
        lotId = lotIdIn;
    }

    public static void encode(PacketAuctionLotRemoved msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.auctionId);
        buf.writeInt(msg.lotId);
    }

    public static PacketAuctionLotRemoved decode(FriendlyByteBuf buf) {
        return new PacketAuctionLotRemoved(buf.readInt(), buf.readInt());
    }

    @Override
    public int getChannelId() {
        return channelId;
    }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        AuctionHouse auction = AuctionController.getInstance().getAuction(auctionId);
        if (auction != null) {
            auction.removeLot(lotId);
            if (Minecraft.getInstance().screen instanceof GuiNpcAuction gui) { gui.updateLot(lotId); }
        }
        CustomNpcs.debugData.end("Packets");
    }

}