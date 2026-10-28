package moe.plushie.armourers_workshop.api.skin.geometry;

import moe.plushie.armourers_workshop.api.core.math.ITransform3f;
import moe.plushie.armourers_workshop.api.skin.texture.ISkinTexturePos;

@SuppressWarnings("unused")
public interface ISkinGeometryFace {

    int id();

    ISkinGeometryType type();

    ISkinGeometryOptions options();

    ITransform3f transform();

    ISkinTexturePos texturePos();

    Iterable<? extends ISkinGeometryVertex> vertices();

}

