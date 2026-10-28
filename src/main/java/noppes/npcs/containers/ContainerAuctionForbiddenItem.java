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

import javax.annotation.Nonnull;

public class ContainerAuctionForbiddenItem extends AbstractContainerMenu {

    public final AuctionHouse auction;
    public final int pos;
    public final AuctionHouse.ForbiddenItem forbiddenItem;

    public ContainerAuctionForbiddenItem(int containerId, Inventory inv, BlockPos blockPos) {
        super(CustomContainer.container_auction_forbidden_item, containerId);
        auction = AuctionController.getInstance().getAuction(blockPos.getX());
        AuctionHouse.ForbiddenItem forbiddenItemIn = auction.getForbiddenItem(pos = blockPos.getY());
        if (pos<0 || forbiddenItemIn == null) { forbiddenItemIn = new AuctionHouse.ForbiddenItem(ItemStack.EMPTY, false, false); }
        forbiddenItem = forbiddenItemIn;
        addSlot(new Slot(forbiddenItem.inventory, 0, 8, 103));
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
