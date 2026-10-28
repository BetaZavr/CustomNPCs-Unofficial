package moe.plushie.armourers_workshop.api;

import moe.plushie.armourers_workshop.api.skin.ISkinDescriptor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class ArmourersWorkshopApi {

    private static Boolean available;

    public static boolean isAvailable() {
        if (available == null) { available = ModList.get().isLoaded("armourers_workshop"); }
        return available;
    }

    private ArmourersWorkshopApi() { throw new IllegalAccessError(); }

    public static ISkinDescriptor getSkinDescriptor(ItemStack stack) {
        try {
            Class<?> clazz = Class.forName("moe.plushie.armourers_workshop.core.skin.SkinDescriptor");
            return (ISkinDescriptor) clazz.getDeclaredMethod("of", ItemStack.class).invoke(null, stack);
        } catch (Exception ignored) { }
        return null;
    }
    
}
