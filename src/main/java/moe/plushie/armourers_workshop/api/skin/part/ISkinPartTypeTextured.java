package moe.plushie.armourers_workshop.api.skin.part;

import moe.plushie.armourers_workshop.api.core.math.IVector2i;
import moe.plushie.armourers_workshop.api.core.math.IVector3i;

@SuppressWarnings("unused")
public interface ISkinPartTypeTextured extends ISkinPartType {

    IVector2i texturePos();

    IVector3i textureModelSize();

}

