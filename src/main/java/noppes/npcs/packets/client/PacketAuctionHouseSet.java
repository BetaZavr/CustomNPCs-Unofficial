package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.player.GuiNpcAuction;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.AuctionHouse;
import noppes.npcs.shared.common.PacketBasic;

public class PacketAuctionHouseSet extends PacketBasic {

    protected static int channelId;
    protected final int auctionId;
    protected final CompoundTag compound;

    public PacketAuctionHouseSet(int auctionIdIn, CompoundTag compoundIn) {
        auctionId = auctionIdIn;
        compound = compoundIn;
    }

    public static void encode(PacketAuctionHouseSet msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.auctionId);
        buf.writeNbt(msg.compound);
    }

    public static PacketAuctionHouseSet decode(FriendlyByteBuf buf) {
        return new PacketAuctionHouseSet(buf.readInt(), buf.readNbt());
    }

    @Override
    public int getChannelId() {
        return channelId;
    }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (compound.isEmpty()) {
            AuctionController.getInstance().auctionHouses.remove(auctionId);
        }
        else {
            AuctionHouse auction = AuctionController.getInstance().getAuction(auctionId);
            if (auction == null) {
                auction = new AuctionHouse(auctionId);
            }
            auction.load(compound, true);
            auction.clientAvailabilityLot = compound.getBoolean("AvailabilityLot");
            auction.clientAvailabilityBuyout = compound.getBoolean("AvailabilityBuyout");
            AuctionController.getInstance().auctionHouses.put(auctionId, auction);
            if (Minecraft.getInstance().screen instanceof GuiNpcAuction gui) { gui.update(); }
        }
        CustomNpcs.debugData.end("Packets");
    }

}