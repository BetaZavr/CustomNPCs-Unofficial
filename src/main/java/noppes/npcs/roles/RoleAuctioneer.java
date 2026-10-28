package noppes.npcs.roles;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.AuctionHouse;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class RoleAuctioneer extends RoleInterface {

    private int auctionId = -1;

    public RoleAuctioneer(EntityNPCInterface npc) {
        super(npc);
        type = RoleType.AUCTIONEER;
    }

    public int getAuctionId() { return auctionId; }

    public void setAuctionId(int id) { auctionId = id; }

    @Override
    public void interact(Player playerIn) {
        npc.say(playerIn, npc.advanced.getInteractLine());
        AuctionHouse auction = AuctionController.getInstance().getAuction(auctionId);
        if (auction != null && playerIn instanceof ServerPlayer player) {
            auction.addPlayer(player);
            SPacketGuiOpen.sendOpenGui(player, EnumGuiType.PlayerAuction,
                    null, new BlockPos(auctionId, 0, 0));
        }
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        type = RoleType.AUCTIONEER;
        if (compound.contains("AuctionId", 3)) {
            auctionId = compound.getInt("AuctionId");
        }
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        super.save(compound);
        compound.putInt("AuctionId", auctionId);
        return compound;
    }

}