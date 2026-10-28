package moe.plushie.armourers_workshop.api.skin;

import java.util.List;
import moe.plushie.armourers_workshop.api.core.IRegistryEntry;
import moe.plushie.armourers_workshop.api.skin.part.ISkinPartType;

@SuppressWarnings("unused")
public interface ISkinType extends IRegistryEntry {

    int id();

    String name();

    List<? extends ISkinPartType> parts();

    boolean isTool();

    boolean isArmour();

    default boolean isEquipment() { return isArmour() || isTool(); }

}

