package moe.plushie.armourers_workshop.api.common;

import net.minecraft.nbt.CompoundTag;

@SuppressWarnings("unused")
public interface IConfigurableToolProperty<T> {

    String name();

    T empty();

    T get(CompoundTag var1);

    void set(CompoundTag var1, T var2);

}

