package noppes.npcs.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomContainer;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.TransportCategory;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.mixin.world.inventory.IAbstractContainerMenuMixin;

import javax.annotation.Nonnull;

public class ContainerNPCTransports extends AbstractContainerMenu {

    protected final Inventory inv;
    public TransportLocation location;

    public ContainerNPCTransports(int containerId, Inventory invIn, BlockPos pos) {
        super(CustomContainer.container_managetransport, containerId);
        inv = invIn;
        TransportController tData = TransportController.getInstance();
        TransportLocation loc = tData.getTransport(pos.getX());
        if (loc == null) {
            TransportCategory cat = tData.getCategory(pos.getY());
            if (cat == null || cat.locations.isEmpty()) {
                for (TransportCategory tCat : tData.getCategories()) {
                    if (!tCat.locations.isEmpty()) {
                        loc = tCat.locations.firstEntry().getValue();
                        break;
                    }
                }
            }
            else { loc = cat.locations.firstEntry().getValue(); }
        }
        if (loc != null) {
            if (invIn.player.level().isClientSide()) { loc = loc.copy(); }
            location = loc;
        }
        resetStacks();
    }

    @Override
    public @Nonnull ItemStack quickMoveStack(@Nonnull Player player, int slotId) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(@Nonnull Player player) { return true; }

    public CompoundTag saveTransport(TransportCategory category) {
        CompoundTag compound = new CompoundTag();
        if (category != null) {
            for (int i = 0; i < 9; i++) { location.inventory.setItem(i, getSlot(i).getItem()); }
            category.locations.put(location.id, location);
            category.save(compound);
        }
        return compound;
    }

    public void resetStacks() {
        slots.clear();
        ((IAbstractContainerMenuMixin) this).getLastSlots().clear();
        ((IAbstractContainerMenuMixin) this).getRemoteSlots().clear();
        for (int v = 0; v < 3; ++v) {
            for (int u = 0; u < 3; ++u) {
                addSlot(new Slot(location.inventory, u + v * 3,
                        (location.id < 0 ? -5000 : 0) + 215 + u * 18, (location.id < 0 ? -5000 : 0) + 20 + v * 18));
            }
        }
        // player inventory
        for(int x = 0; x < 3; ++x) {
            for(int y = 0; y < 9; ++y) { addSlot(new Slot(inv, y + x * 9 + 9, y * 18 + 8, 113 + x * 18)); }
        }
        // player hotbar
        for(int x = 0; x < 9; ++x) { addSlot(new Slot(inv, x, x * 18 + 8, 171)); }
    }

}
