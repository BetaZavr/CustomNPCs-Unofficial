package noppes.npcs.mixin.world.level.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EntitySectionStorage.class, priority = 498)
public interface IEntitySectionStorageMixin<T extends EntityAccess> {

    @Accessor Long2ObjectMap<EntitySection<T>> getSections();

}
