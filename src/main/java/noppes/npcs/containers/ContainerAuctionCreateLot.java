package noppes.npcs.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomContainer;
import noppes.npcs.controllers.AuctionController;
import noppes.npcs.controllers.data.AuctionHouse;
import noppes.npcs.controllers.data.AuctionLot;

import javax.annotation.Nonnull;

public class ContainerAuctionCreateLot extends AbstractContainerMenu {

    public final AuctionHouse auction;
    public final AuctionLot lot;

    public ContainerAuctionCreateLot(int containerId, Inventory inv, BlockPos blockPos) {
        super(CustomContainer.container_auction_create_lot, containerId);
        auction = AuctionController.getInstance().getAuction(blockPos.getX());
        lot = new AuctionLot(System.currentTimeMillis());
        lot.setDuration(auction.getDelays()[1]);
        lot.setOwnerName(inv.player.getName().getString());
        // Player Inventory
        for (int r = 0; r < 3; ++r) {
            for (int p = 0; p < 9; ++p) {
                addSlot(new Slot(inv, p + r * 9 + 9, 8 + p * 18, 103 + r * 18));
            }
        }
        for (int p = 0; p < 9; ++p) {
            addSlot(new Slot(inv, p, 8 + p * 18, 161));
        }
    }

    @Override
    public boolean stillValid(@Nonnull Player playerIn) { return true; }

    @Override
    public @Nonnull ItemStack quickMoveStack(@Nonnull Player player, int slotId) { return ItemStack.EMPTY; }

}
