package noppes.npcs.mixin.world.item;

import net.minecraft.world.item.ArmorMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ArmorMaterials.class, priority = 502)
public interface IArmorMaterialsMixin {

    @Accessor int getDurabilityMultiplier();

}
