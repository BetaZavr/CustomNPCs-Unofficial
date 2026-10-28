package moe.plushie.armourers_workshop.api.common;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface ILootContext {

    boolean hasParameter(IContextKey<?> var1);

    <T> T getParameter(IContextKey<T> var1);

    @Nullable <T> T getOptionalParameter(IContextKey<T> var1);

    IRandomSource randomSource();

}

