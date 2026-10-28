package moe.plushie.armourers_workshop.api.core;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface IDataComponentType<T> {

    boolean has(ItemStack var1);

    @Nullable T get(ItemStack var1);

    T getOrDefault(ItemStack var1, T var2);

    void set(ItemStack var1, @Nullable T var2);

    void remove(ItemStack var1);

}

