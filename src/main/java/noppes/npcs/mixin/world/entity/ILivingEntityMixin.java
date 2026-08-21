package noppes.npcs.mixin.world.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.throwables.MixinException;

import javax.annotation.Nonnull;

@Mixin(value = LivingEntity.class, priority = 502)
public interface ILivingEntityMixin {

   @Accessor("DATA_LIVING_ENTITY_FLAGS") static EntityDataAccessor<Byte> getHandStates() { throw new MixinException("Mixin did not initialize properly."); }

   @Accessor boolean getJumping();

   @Accessor void setUseItemRemaining(int newItemRemaining);

   @Accessor float getAnimStep();

   @Accessor void setAnimStep(float newAnimStep);

   @Accessor float getAnimStepO();

   @Accessor void setAnimStepO(float newAnimStep0);

   @Accessor float getSwimAmount();

   @Accessor void setSwimAmount(float newSwimAmount);

   @Accessor float getSwimAmountO();

   @Accessor void setSwimAmountO(float newSwimAmount0);

   @Accessor int getLastHurtByPlayerTime();

   @Accessor void setLastHurtByPlayerTime(int newLastHurtByPlayerTime);

   @Accessor float getLastHurt();

   @Accessor void setLastDamageSource(@Nonnull DamageSource newLastDamageSource);

   @Accessor void setLastDamageStamp(long newLastDamageStamp);

   @Invoker
   boolean invokeCheckTotemDeathProtection(@Nonnull DamageSource source);

}
