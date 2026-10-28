package moe.plushie.armourers_workshop.api.skin;

import moe.plushie.armourers_workshop.api.core.IDataSerializable;

@SuppressWarnings("unused")
public interface ISkinDescriptor {

    String identifier();

    ISkinType type();

    IDataSerializable.Immutable paintScheme();

    IDataSerializable.Immutable options();

}

