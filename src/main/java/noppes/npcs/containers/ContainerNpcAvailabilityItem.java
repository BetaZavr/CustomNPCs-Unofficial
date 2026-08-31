package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.controllers.data.Availability;

import javax.annotation.Nonnull;

public class ContainerNpcAvailabilityItem extends Container {

    public final Availability availability;
    public final NpcMiscInventory inv;
    public final SlotAvailability slot;

    public ContainerNpcAvailabilityItem(EntityPlayer player, FriendlyByteBuf buffer) {
        availability = new Availability();
        availability.load(buffer.readNbt());
        inv = availability.stacks;

        addSlotToContainer(slot = new SlotAvailability(inv, 0, 8, 89));

        for(int y = 0; y < 3; ++y) {
            for(int x = 0; x < 9; ++x) { addSlotToContainer(new Slot(player.inventory, x + y * 9 + 9, 8 + x * 18, 113 + y * 18)); }
        }
        for(int x = 0; x < 9; ++x) {
            addSlotToContainer(new Slot(player.inventory, x, 8 + x * 18, 171));
        }
    }

    @Override
    public @Nonnull ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer playerIn) { return true; }

}
