package moe.plushie.armourers_workshop.api.skin.property;

@SuppressWarnings("unused")
public interface ISkinProperties {

    <T> void put(ISkinProperty<T> var1, T var2);

    <T> void remove(ISkinProperty<T> var1);

    <T> boolean containsKey(ISkinProperty<T> var1);

    <T> boolean containsValue(ISkinProperty<T> var1);

    <T> T get(ISkinProperty<T> var1);

}

