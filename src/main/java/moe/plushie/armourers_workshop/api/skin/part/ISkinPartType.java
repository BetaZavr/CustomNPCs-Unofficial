package moe.plushie.armourers_workshop.api.skin.part;

import moe.plushie.armourers_workshop.api.core.IRegistryEntry;
import moe.plushie.armourers_workshop.api.core.math.IRectangle3i;
import moe.plushie.armourers_workshop.api.core.math.IVector3i;

@SuppressWarnings("unused")
public interface ISkinPartType extends IRegistryEntry {

    String name();

    IRectangle3i buildingSpace();

    IRectangle3i guideSpace();

    IVector3i offset();

    IRectangle3i bounds();

    IVector3i renderOffset();

    default float renderPolygonOffset() {
        return 0.0f;
    }

    int minimumMarkersNeeded();

    int maximumMarkersNeeded();

    boolean isPartRequired();

}

