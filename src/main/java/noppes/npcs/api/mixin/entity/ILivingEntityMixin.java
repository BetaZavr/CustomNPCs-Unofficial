package noppes.npcs.api.mixin.entity;

import net.minecraft.world.damagesource.DamageSource;

public interface ILivingEntityMixin {

    DamageSource npcs$getCurrentDamageSource();

    void npcs$setCurrentDamageSource(DamageSource source);

}