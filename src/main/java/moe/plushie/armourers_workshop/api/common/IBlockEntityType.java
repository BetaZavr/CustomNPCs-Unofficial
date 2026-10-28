package moe.plushie.armourers_workshop.api.common;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
public interface IBlockEntityType<T extends BlockEntity> extends Supplier<BlockEntityType<T>> {

    T create(BlockGetter var1, BlockPos var2, BlockState var3);

    interface Serializer<T extends BlockEntity> {
        T create(BlockEntityType<?> var1, BlockPos var2, BlockState var3);
    }

}

