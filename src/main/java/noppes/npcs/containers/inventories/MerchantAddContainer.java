package noppes.npcs.containers.inventories;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import noppes.npcs.mixin.world.item.trading.IMerchantOfferMixin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class MerchantAddContainer implements Container {

    private final Merchant merchant;
    private final NonNullList<ItemStack> itemStacks = NonNullList.withSize(3, ItemStack.EMPTY);
    private MerchantOffer activeOffer;

    public MerchantAddContainer(Merchant trader) { merchant = trader; }

    @Override
    public int getContainerSize() { return itemStacks.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : itemStacks) {
            if (!itemStack.isEmpty()) { return false; }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slotID) { return itemStacks.get(slotID); }

    @Override
    public @NotNull ItemStack removeItem(int slotID, int amount) {
        ItemStack stack = itemStacks.get(slotID);
        if (slotID == 2 && !stack.isEmpty()) { return ContainerHelper.removeItem(itemStacks, slotID, stack.getCount()); }
        ItemStack removeStack = ContainerHelper.removeItem(itemStacks, slotID, amount);
        updateItems();
        return removeStack;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slotID) { return ContainerHelper.takeItem(itemStacks, slotID); }

    @Override
    public void setItem(int slotID, @NotNull ItemStack stack) {
        itemStacks.set(slotID, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) { stack.setCount(getMaxStackSize()); }
        updateItems();
    }

    @Override
    public boolean stillValid(@NotNull Player player) { return true; }

    @Override
    public void setChanged() { updateItems(); }

    public @Nullable MerchantOffer getActiveOffer() { return activeOffer; }

    public void updateItems() {
        if (activeOffer != null) {
            ((IMerchantOfferMixin) activeOffer).setBaseCostA(itemStacks.get(0));
            ((IMerchantOfferMixin) activeOffer).setCostB(itemStacks.get(1));
            ((IMerchantOfferMixin) activeOffer).setResult(itemStacks.get(2));
        }
    }

    public void setSelectionHint(int shopItem) {
        if (shopItem >= 0 && shopItem < merchant.getOffers().size()) {
            activeOffer = merchant.getOffers().get(shopItem);
            itemStacks.set(0, activeOffer.getBaseCostA());
            itemStacks.set(1, activeOffer.getCostB());
            itemStacks.set(2, activeOffer.getResult());
        }
    }

    public void clearContent() { itemStacks.clear(); }

}
