package moe.plushie.armourers_workshop.api.common;

import java.util.Optional;
import net.minecraft.world.entity.Entity;

public interface IEntityCapability<T> {

    Optional<T> get(Entity var1);

}

