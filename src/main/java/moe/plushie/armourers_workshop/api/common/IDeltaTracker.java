package moe.plushie.armourers_workshop.api.common;

import net.minecraft.world.entity.Entity;

@SuppressWarnings("unused")
public interface IDeltaTracker {

    boolean isPaused();

    boolean isFrozen();

    float partialTick();

    float partialTick(Entity var1);

    float rate();

}

