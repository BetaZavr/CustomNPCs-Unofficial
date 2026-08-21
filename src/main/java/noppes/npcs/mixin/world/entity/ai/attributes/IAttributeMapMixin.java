package noppes.npcs.mixin.world.entity.ai.attributes;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Set;

@Mixin(value = AttributeMap.class, priority = 502)
public interface IAttributeMapMixin {

    @Accessor Map<Attribute, AttributeInstance> getAttributes();

    @Accessor Set<AttributeInstance> getDirtyAttributes();

    @Accessor AttributeSupplier getSupplier();

}
