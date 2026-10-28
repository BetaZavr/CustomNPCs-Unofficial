package moe.plushie.armourers_workshop.api.client;

import java.util.function.Supplier;
import moe.plushie.armourers_workshop.api.client.state.IBlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public interface IBlockEntityRenderer<T extends BlockEntity, S extends IBlockEntityRenderState> {

    interface Context extends Supplier<Object> {
    }

    interface Provider<T extends BlockEntity> {
        IBlockEntityRenderer<T, ?> create(Context var1);
    }
}

