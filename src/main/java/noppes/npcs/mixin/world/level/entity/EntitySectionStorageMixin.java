package noppes.npcs.mixin.world.level.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import noppes.npcs.api.mixin.world.level.entity.IEntitySectionStorageMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(value = EntitySectionStorage.class, priority = 498)
public class EntitySectionStorageMixin<T extends EntityAccess> implements IEntitySectionStorageMixin<T> {

    @Shadow @Final private Long2ObjectMap<EntitySection<T>> sections;

    @Override
    public T npcs$getStorage(UUID uuid) {
        if (uuid != null) {
            for (EntitySection<T> section : sections.values()) {
                if (section == null || section.isEmpty()) { continue; }
                T found = section.getEntities()
                        .filter(e -> e instanceof Entity && e.getUUID().equals(uuid))
                        .findFirst()
                        .orElse(null);
                if (found != null) { return found; }
            }
        }
        return null;
    }
}
