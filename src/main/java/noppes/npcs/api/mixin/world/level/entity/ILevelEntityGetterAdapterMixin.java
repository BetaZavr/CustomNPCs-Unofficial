package noppes.npcs.api.mixin.world.level.entity;

import net.minecraft.world.level.entity.EntityAccess;

import java.util.UUID;

public interface ILevelEntityGetterAdapterMixin<T extends EntityAccess> {

    T npcs$getStorage(UUID uuid);

}
