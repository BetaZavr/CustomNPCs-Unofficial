package noppes.npcs.containers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomContainer;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.containers.slots.SlotAvailability;
import noppes.npcs.controllers.data.Availability;
import org.jetbrains.annotations.NotNull;

// New from Unofficial (BetaZavr)
public class ContainerNpcAvailabilityItem extends AbstractContainerMenu {

    public final Availability availability;
    public final NpcMiscInventory inv;
    public final SlotAvailability slot;

    public ContainerNpcAvailabilityItem(int containerId, Inventory playerInventory, CompoundTag availabilityNBT) {
        super(CustomContainer.container_availability_item, containerId);
        availability = new Availability();
        availability.load(availabilityNBT);
        inv = availability.stacks;

        addSlot(slot = new SlotAvailability(inv, 0, 8, 89));

        for(int y = 0; y < 3; ++y) {
            for(int x = 0; x < 9; ++x) { addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 113 + y * 18)); }
        }
        for(int x = 0; x < 9; ++x) {
            addSlot(new Slot(playerInventory, x, 8 + x * 18, 171));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player playerIn) { return true; }

}