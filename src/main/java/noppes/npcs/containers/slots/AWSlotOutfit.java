package noppes.npcs.containers.slots;

import moe.plushie.armourers_workshop.api.ArmourersWorkshopApi;
import moe.plushie.armourers_workshop.api.skin.ISkinDescriptor;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class AWSlotOutfit extends Slot {

    public AWSlotOutfit(Container container, int slotIndex, int x, int y) {
        super(container, slotIndex, x, y);
    }

    @Override
    public int getMaxStackSize() { return 1; }

    @Override
    public boolean mayPlace(@Nonnull ItemStack itemstack) {
        if (ArmourersWorkshopApi.isAvailable()) {
            ISkinDescriptor skinDescriptor = ArmourersWorkshopApi.getSkinDescriptor(itemstack);
            if (skinDescriptor != null) {
                return skinDescriptor.type().name().equalsIgnoreCase("outfit");
            }
        }
        return false;
    }

}
