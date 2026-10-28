package moe.plushie.armourers_workshop.api.skin.texture;

import moe.plushie.armourers_workshop.api.core.IRegistryEntry;

@SuppressWarnings("unused")
public interface ISkinPaintType extends IRegistryEntry {

    int id();

    int ordinal();

    ISkinDyeType dyeType();

    ISkinTexturePos texturePos();

}

