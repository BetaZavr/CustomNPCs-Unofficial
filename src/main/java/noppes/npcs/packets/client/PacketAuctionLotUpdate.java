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
import noppes.npcs.controllers.data.AuctionLot;
import noppes.npcs.shared.common.PacketBasic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PacketAuctionLotUpdate extends PacketBasic {

    protected static int channelId;
    protected final int auctionId;
    protected final long lotId;
    protected final CompoundTag compound;

    public PacketAuctionLotUpdate(int auctionIdIn, long lotIdIn, CompoundTag compoundIn) {
        auctionId = auctionIdIn;
        lotId = lotIdIn;
        compound = compoundIn;
    }

    public static void encode(PacketAuctionLotUpdate msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.auctionId);
        buf.writeLong(msg.lotId);
        buf.writeNbt(msg.compound);
    }

    public static PacketAuctionLotUpdate decode(FriendlyByteBuf buf) {
        return new PacketAuctionLotUpdate(buf.readInt(), buf.readLong(), buf.readNbt());
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
            GuiNpcAuction gui = null;
            if (Minecraft.getInstance().screen instanceof GuiNpcAuction) { gui = (GuiNpcAuction) Minecraft.getInstance().screen; }
            processUpdateAuctionLot(gui, auction, compound);
        }
        CustomNpcs.debugData.end("Packets");
    }

    public static void processUpdateAuctionLot(@Nullable GuiNpcAuction gui, @Nonnull AuctionHouse auction, @Nonnull CompoundTag compound) {
        if (!compound.contains("CreationDate", 4)) { return; }
        long lotId = compound.getLong("CreationDate");
        AuctionLot existingLot = (AuctionLot) auction.getLot(lotId);
        if (existingLot == null) {
            // New lot - create and add
            AuctionLot lot = new AuctionLot(0);
            lot.load(compound);
            auction.addLot(lot);
        } else {
            // Existing lot - update in place
            existingLot.load(compound);
        }
        if (gui != null) { gui.updateLot(lotId); }
    }

}