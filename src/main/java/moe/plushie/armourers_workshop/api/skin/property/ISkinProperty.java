package moe.plushie.armourers_workshop.api.skin.property;

@SuppressWarnings("unused")
public interface ISkinProperty<T> {

    String key();

    T defaultValue();

}

