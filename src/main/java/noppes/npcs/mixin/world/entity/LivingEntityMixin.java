package noppes.npcs.mixin.world.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.api.mixin.entity.ILivingEntityMixin;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 499)
public class LivingEntityMixin implements ILivingEntityMixin {

    @Unique
    private DamageSource npcs$currentDamageSource;

    @Inject(
            at = {@At("HEAD")},
            method = {"addAdditionalSaveData"}
    )
    private void cnpcs$renderToBuffer(CompoundTag compound, CallbackInfo callbackInfo) {
        LivingEntity e = (LivingEntity) (Object) this;
        if (!e.level().isClientSide()) { MarkData.get(e).save(); }
    }

    // remember the source of damage
    @Inject(method = "hurt", at = @At("HEAD"))
    private void npcs$saveDamageSource(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        npcs$currentDamageSource = source;
    }

    // Replace knockback force when dealing damage
    @Redirect(
            method = "hurt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V")
    )
    private void npcs$hurt(LivingEntity instance, double strength, double xRatio, double zRatio) {
        if (instance instanceof EntityNPCInterface npc) {
            float f0, f1;
            if (npcs$currentDamageSource != null && npcs$currentDamageSource.is(DamageTypeTags.IS_PROJECTILE)) {
                f0 = 0.25f;
                f1 = 0.15f * (float) npc.stats.ranged.getKnockback();
            } else {
                f0 = 0.2f;
                f1 = 0.2f  * (float) npc.stats.melee.getKnockback();
            }
            if (f1 != 0.0f) { strength = f0 + f1; }
        }
        if (strength != 0) { instance.knockback(strength, xRatio, zRatio); }
    }

    @Override
    public DamageSource npcs$getCurrentDamageSource() { return npcs$currentDamageSource; }

    @Override
    public void npcs$setCurrentDamageSource(DamageSource source) { npcs$currentDamageSource = source; }

}
