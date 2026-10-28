package moe.plushie.armourers_workshop.api.registry;

import moe.plushie.armourers_workshop.api.core.IDataComponentType;

@SuppressWarnings("unused")
public interface IDataComponentTypeBuilder<T> extends IRegistryBuilder<IDataComponentType<T>> {

    IDataComponentTypeBuilder<T> tag(String var1);

}

