package moe.plushie.armourers_workshop.api.common;

import net.minecraft.network.syncher.EntityDataAccessor;

@SuppressWarnings("unused")
public interface IEntityDataBuilder {

    <T> void define(EntityDataAccessor<T> var1, T var2);

}

