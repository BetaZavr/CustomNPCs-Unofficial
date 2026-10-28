package moe.plushie.armourers_workshop.api.common;

import moe.plushie.armourers_workshop.api.network.IFriendlyByteBuf;

@SuppressWarnings("unused")
public interface IEntityDataSerializer<T> {

    T read(IFriendlyByteBuf var1);

    void write(IFriendlyByteBuf var1, T var2);

}

