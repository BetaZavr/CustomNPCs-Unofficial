package noppes.npcs.mixin.world.entity.ai.attributes;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RangedAttribute.class, priority = 502)
public interface IRangedAttributeMixin {

    @Accessor void setMinValue(double newMinValue);

    @Accessor void setMaxValue(double newMaxValue);

}
