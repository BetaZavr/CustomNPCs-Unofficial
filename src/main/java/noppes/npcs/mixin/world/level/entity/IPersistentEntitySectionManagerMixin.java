package noppes.npcs.mixin.world.level.entity;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PersistentEntitySectionManager.class, priority = 502)
public interface IPersistentEntitySectionManagerMixin<T extends EntityAccess> {

    @Accessor EntitySectionStorage<T> getSectionStorage();

}
