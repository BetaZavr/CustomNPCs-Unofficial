package moe.plushie.armourers_workshop.api.registry;

import java.util.function.Supplier;
import moe.plushie.armourers_workshop.api.client.IEntityRenderer;
import moe.plushie.armourers_workshop.api.common.IEntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unused")
public interface IEntityTypeBuilder<T extends Entity> extends IRegistryBuilder<IEntityType<T>> {

    IEntityTypeBuilder<T> fixed(float var1, float var2);

    IEntityTypeBuilder<T> noSummon();

    IEntityTypeBuilder<T> noSave();

    IEntityTypeBuilder<T> fireImmune();

    IEntityTypeBuilder<T> specificSpawnBlocks(Block ... var1);

    IEntityTypeBuilder<T> spawnableFarFromPlayer();

    IEntityTypeBuilder<T> clientTrackingRange(int var1);

    IEntityTypeBuilder<T> updateInterval(int var1);

    IEntityTypeBuilder<T> bind(Supplier<IEntityRenderer.Provider<T>> var1);

}

