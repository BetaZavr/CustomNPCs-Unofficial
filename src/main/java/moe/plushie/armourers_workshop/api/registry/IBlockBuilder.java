package moe.plushie.armourers_workshop.api.registry;

import java.util.function.ToIntFunction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
public interface IBlockBuilder<T extends Block> extends IRegistryBuilder<T> {

    IBlockBuilder<T> noCollission();

    IBlockBuilder<T> noOcclusion();

    IBlockBuilder<T> noLootTable();

    IBlockBuilder<T> friction(float var1);

    IBlockBuilder<T> speedFactor(float var1);

    IBlockBuilder<T> jumpFactor(float var1);

    IBlockBuilder<T> sound(SoundType var1);

    IBlockBuilder<T> lightLevel(ToIntFunction<BlockState> var1);

    IBlockBuilder<T> strength(float var1, float var2);

    IBlockBuilder<T> randomTicks();

    IBlockBuilder<T> dynamicShape();

    IBlockBuilder<T> air();

    IBlockBuilder<T> forceSolid();

    IBlockBuilder<T> isValidSpawn(BlockBehaviour.StateArgumentPredicate<EntityType<?>> var1);

    IBlockBuilder<T> isRedstoneConductor(BlockBehaviour.StatePredicate var1);

    IBlockBuilder<T> isSuffocating(BlockBehaviour.StatePredicate var1);

    IBlockBuilder<T> isViewBlocking(BlockBehaviour.StatePredicate var1);

    IBlockBuilder<T> emissiveRendering(BlockBehaviour.StatePredicate var1);

    IBlockBuilder<T> requiresCorrectToolForDrops();

    default IBlockBuilder<T> lightLevel(int level) { return this.lightLevel(state -> level); }

}

