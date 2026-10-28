package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.player.GuiNpcAuction;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.AuctionHouse;
import noppes.npcs.shared.common.PacketBasic;

public class PacketAuctionHouseAddLots extends PacketBasic {

    protected static int channelId;
    protected final int auctionId;
    protected final CompoundTag compound;

    public PacketAuctionHouseAddLots(int auctionIdIn, CompoundTag compoundIn) {
        auctionId = auctionIdIn;
        compound = compoundIn;
    }

    public static void encode(PacketAuctionHouseAddLots msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.auctionId);
        buf.writeNbt(msg.compound);
    }

    public static PacketAuctionHouseAddLots decode(FriendlyByteBuf buf) {
        return new PacketAuctionHouseAddLots(buf.readInt(), buf.readNbt());
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
            ListTag list = compound.getList("Lots", 10);
            for (int i = 0; i < list.size(); i++) {
                PacketAuctionLotUpdate.processUpdateAuctionLot(gui, auction, list.getCompound(i));
            }
        }
        CustomNpcs.debugData.end("Packets");
    }

}