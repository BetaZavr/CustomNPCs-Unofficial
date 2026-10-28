package moe.plushie.armourers_workshop.api.common;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

@SuppressWarnings("unused")
public interface IBlockEntity {

    default AABB getVisibleBox(BlockState blockState) { return null; }

}

