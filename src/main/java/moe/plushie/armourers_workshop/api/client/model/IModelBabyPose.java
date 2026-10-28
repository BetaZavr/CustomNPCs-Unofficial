package moe.plushie.armourers_workshop.api.client.model;

import moe.plushie.armourers_workshop.api.core.math.IVector3f;

@SuppressWarnings("unused")
public interface IModelBabyPose {

    float headScale();

    IVector3f headOffset();

    float bodyScale();

    IVector3f bodyOffset();

}

