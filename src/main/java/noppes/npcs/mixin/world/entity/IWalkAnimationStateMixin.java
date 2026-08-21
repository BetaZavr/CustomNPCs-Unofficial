package noppes.npcs.mixin.world.entity;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = WalkAnimationState.class, priority = 499)
public interface IWalkAnimationStateMixin {

   @Accessor float getSpeedOld();

   @Accessor void setSpeedOld(float speed);

}
