package moe.plushie.armourers_workshop.api.core;

@SuppressWarnings("unused")
public interface IResourceKey {

    String namespace();

    String path();

    IResourceKey withNamespace(String var1);

    IResourceKey withPath(String var1);

    default String toLanguageKey() { return namespace() + "." + path(); }

    default String toLanguageKey(String prefix) { return prefix + "." + toLanguageKey(); }

    default  String toLanguageKey(String prefix, String suffix) { return prefix + "." + toLanguageKey() + "." + suffix; }

}

