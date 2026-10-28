package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.AuctionHouse;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SPacketAuctionSave extends PacketServerBasic {

    protected static int channelId;
    private final int houseId;
    private final CompoundTag compound;

    public SPacketAuctionSave(int houseIdIn, CompoundTag compoundIn) {
        houseId = houseIdIn;
        compound = compoundIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return new ArrayList<>(Collections.singletonList(CustomNpcsPermissions.GLOBAL_AUCTIONS)); }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketAuctionSave msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.houseId);
        buf.writeNbt(msg.compound);
    }

    public static SPacketAuctionSave decode(FriendlyByteBuf buf) { return new SPacketAuctionSave(buf.readInt(), buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        AuctionController controller = AuctionController.getInstance();
        if (compound != null && compound.contains("HouseID", 3)) {
            int id = compound.getInt("HouseID");
            AuctionHouse auction = controller.getAuction(id);
            if (auction == null) { auction = controller.addAuction(); } // new
            if (auction != null) {
                auction.load(compound, false);
                controller.save(auction.getId());
            } // reset
        }
        CustomNpcs.debugData.end("Packets");
    }

}