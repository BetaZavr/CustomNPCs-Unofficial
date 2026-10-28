package moe.plushie.armourers_workshop.api.registry;

import java.util.function.Supplier;
import moe.plushie.armourers_workshop.api.client.IBlockEntityRenderer;
import moe.plushie.armourers_workshop.api.common.IBlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public interface IBlockEntityTypeBuilder<T extends BlockEntity> extends IRegistryBuilder<IBlockEntityType<T>> {

    IBlockEntityTypeBuilder<T> of(Supplier<Block> var1);

    IBlockEntityTypeBuilder<T> bind(Supplier<IBlockEntityRenderer.Provider<T>> var1);

}

