package noppes.npcs.mixin.world.level.entity;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import noppes.npcs.api.mixin.world.level.entity.IEntitySectionStorageMixin;
import noppes.npcs.api.mixin.world.level.entity.ILevelEntityGetterAdapterMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(value = LevelEntityGetterAdapter.class, priority = 498)
public class LevelEntityGetterAdapterMixin<T extends EntityAccess> implements ILevelEntityGetterAdapterMixin<T> {

    @Final @Shadow private EntitySectionStorage<T> sectionStorage;

    @Override
    @SuppressWarnings("unchecked")
    public T npcs$getStorage(UUID uuid) { return ((IEntitySectionStorageMixin<T>) sectionStorage).npcs$getStorage(uuid); }

}
