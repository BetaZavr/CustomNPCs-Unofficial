package noppes.npcs.mixin.world.entity.item;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ItemEntity.class, priority = 502)
public interface IItemEntityMixin {

    @Accessor int getPickupDelay();

    @Accessor void setAge(int newAge);

}
