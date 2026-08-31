package noppes.npcs.mixin.minecraftforge.registries;

import net.minecraftforge.registries.ForgeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.BitSet;

@Mixin(value = ForgeRegistry.class, priority = 502, remap = false)
public interface ForgeRegistryMixin {

    @Accessor("availabilityMap") BitSet getAvailabilityMap();

}