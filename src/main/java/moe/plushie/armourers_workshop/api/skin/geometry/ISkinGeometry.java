package moe.plushie.armourers_workshop.api.skin.geometry;

import moe.plushie.armourers_workshop.api.core.math.ITransform3f;
import moe.plushie.armourers_workshop.api.core.math.IVoxelShape;

@SuppressWarnings("unused")
public interface ISkinGeometry {

    ISkinGeometryType type();

    ISkinGeometryOptions options();

    ITransform3f transform();

    IVoxelShape shape();

    Iterable<? extends ISkinGeometryFace> faces();

}

