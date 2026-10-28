package moe.plushie.armourers_workshop.api.core;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public interface IRegistryHolder<T> extends IRegistryEntry, Supplier<T> {
}

