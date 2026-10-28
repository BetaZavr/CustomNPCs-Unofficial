package moe.plushie.armourers_workshop.api.registry;

import java.util.function.Supplier;
import moe.plushie.armourers_workshop.api.client.key.IKeyMapping;
import moe.plushie.armourers_workshop.api.client.key.IKeyModifier;

@SuppressWarnings("unused")
public interface IKeyMappingBuilder<T extends IKeyMapping> extends IRegistryBuilder<T> {

    IKeyMappingBuilder<T> modifier(IKeyModifier var1);

    IKeyMappingBuilder<T> bind(Supplier<Runnable> var1);

}

