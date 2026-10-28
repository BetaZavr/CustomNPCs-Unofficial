package moe.plushie.armourers_workshop.api.common;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface IEntityType<T extends Entity> extends Supplier<EntityType<T>> {

    T create(ServerLevel var1, BlockPos var2, @Nullable ItemStack var3, IEntitySpawnReason var4);

    interface Serializer<T extends Entity> {
        T create(EntityType<T> var1, Level var2);
    }

}

