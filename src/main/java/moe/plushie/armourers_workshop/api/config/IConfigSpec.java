package moe.plushie.armourers_workshop.api.config;

import java.util.Map;

@SuppressWarnings("unused")
public interface IConfigSpec {

    Map<String, Object> snapshot();

    void apply(Map<String, Object> var1);

    void reload();

    void save();

    void notify(Runnable var1);

}

