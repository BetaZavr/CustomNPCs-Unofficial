package noppes.npcs.containers.slots;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import noppes.npcs.CustomNpcs;

public class SlotNPCHand extends Slot {

    protected final ResourceLocation texture;

    public SlotNPCHand(Container container, int slotIndex, int x, int y, String part) {
        super(container, slotIndex, x, y);
        texture = new ResourceLocation(CustomNpcs.MODID, "textures/item/empty_slot_" + part + ".png");
    }

    @Override
    public int getMaxStackSize() { return 1; }

    public ResourceLocation getTexture() { return texture; }

}
