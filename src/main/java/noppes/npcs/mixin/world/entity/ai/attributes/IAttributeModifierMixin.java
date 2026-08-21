package noppes.npcs.mixin.world.entity.ai.attributes;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(value = AttributeModifier.class, priority = 502)
public interface IAttributeModifierMixin {

    @Mutable @Accessor void setAmount(double newAmount);

    @Mutable @Accessor void setOperation(AttributeModifier.Operation newOperation);

    @Mutable @Accessor void setId(UUID newId);

}