package moe.plushie.armourers_workshop.api.client;

import java.util.function.Supplier;
import moe.plushie.armourers_workshop.api.client.state.IEntityRenderState;
import net.minecraft.world.entity.Entity;

@SuppressWarnings("unused")
public interface IEntityRenderer<T extends Entity, S extends IEntityRenderState> {

    interface Context extends Supplier<Object> {
    }

    interface Provider<T extends Entity> {
        IEntityRenderer<T, ?> create(Context var1);
    }

}

