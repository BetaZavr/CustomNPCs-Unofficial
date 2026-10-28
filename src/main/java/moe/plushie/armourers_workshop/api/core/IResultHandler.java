package moe.plushie.armourers_workshop.api.core;

@SuppressWarnings("unused")
public interface IResultHandler<T> {

    void apply(T var1, Throwable var2);

    default void accept(T value) { apply(value, null); }

    default void abort(Throwable throwable) { apply(null, throwable); }

}

