package moe.plushie.armourers_workshop.api.core;

@SuppressWarnings("unused")
public interface IDataSerializer {

    <T> T read(IDataSerializerKey<T> var1);

    <T> void write(IDataSerializerKey<T> var1, T var2);

}

