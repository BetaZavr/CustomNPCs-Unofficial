package moe.plushie.armourers_workshop.api.skin.geometry;

import moe.plushie.armourers_workshop.api.core.IRegistryEntry;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unused")
public interface ISkinGeometryType extends IRegistryEntry {

    int id();

    Block block();

}

