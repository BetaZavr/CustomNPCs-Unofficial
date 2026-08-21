package noppes.npcs.mixin.world.level.entity;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = LevelEntityGetterAdapter.class, priority = 498)
public interface ILevelEntityGetterAdapterMixin<T extends EntityAccess> {

    @Accessor EntitySectionStorage<T> getSectionStorage();

}
