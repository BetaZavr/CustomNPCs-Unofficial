package moe.plushie.armourers_workshop.api.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IConfigValue<T> {

    T read();

    void write(T var1);

    void bind(Consumer<T> var1, Supplier<T> var2);

}

