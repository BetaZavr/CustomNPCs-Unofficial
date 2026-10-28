package moe.plushie.armourers_workshop.api.skin.part;

import java.util.Collection;
import moe.plushie.armourers_workshop.api.core.math.ITransform;
import moe.plushie.armourers_workshop.api.skin.ISkinMarker;
import moe.plushie.armourers_workshop.api.skin.geometry.ISkinGeometrySet;

@SuppressWarnings("unused")
public interface ISkinPart {

    ISkinPartType type();

    ITransform transform();

    ISkinGeometrySet<?> geometries();

    Collection<? extends ISkinPart> children();

    Collection<? extends ISkinMarker> markers();

}

