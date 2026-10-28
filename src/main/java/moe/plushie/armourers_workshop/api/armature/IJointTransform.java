package moe.plushie.armourers_workshop.api.armature;

import moe.plushie.armourers_workshop.api.core.math.IPoseStack;

@SuppressWarnings("unused")
public interface IJointTransform {

    IJointTransform NONE = poseStack -> {};

    void apply(IPoseStack poseStack);

}

