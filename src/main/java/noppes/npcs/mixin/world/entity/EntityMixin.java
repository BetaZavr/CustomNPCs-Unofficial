package noppes.npcs.mixin.world.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import noppes.npcs.api.mixin.world.entity.IEntityIMixin;
import noppes.npcs.api.wrapper.data.Data;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class)
public class EntityMixin implements IEntityIMixin {

    @Unique
    protected final Data npcs$storeddata = new Data();

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    public void npcs$saveWithoutId(CompoundTag compound, CallbackInfoReturnable<Boolean> cir) {
        compound.put("CustomStoredData", npcs$storeddata.getNbt().getMCNBT());
    }

    @Inject(method = "load", at = @At("RETURN"))
    public void npcs$load(CompoundTag compound, CallbackInfo ci) {
        npcs$storeddata.setNbt(compound.getCompound("CustomStoredData"));
    }

    @Unique
    public Data npcs$getStoredData() {
        return npcs$storeddata; }

}
