package moe.plushie.armourers_workshop.api.skin;

import java.util.List;

import moe.plushie.armourers_workshop.api.skin.part.ISkinPart;

@SuppressWarnings("unused")
public interface ISkin {

    ISkinType type();

    List<? extends ISkinPart> parts();

}

