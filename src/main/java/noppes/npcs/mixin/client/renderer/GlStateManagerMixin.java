package noppes.npcs.mixin.client.renderer;

import net.minecraft.client.renderer.GlStateManager;
import noppes.npcs.client.util.GlCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlStateManager.class, priority = 499)
public class GlStateManagerMixin {

    @Inject(method = "deleteTexture", at = @At("HEAD"))
    private static void npcs$deleteTexture(int texture, CallbackInfo ci) { GlCache.forget(texture); }

    @Inject(method = "bindTexture", at = @At("HEAD"))
    private static void npcs$bindTexture(int texture, CallbackInfo ci) { GlCache.onBind(texture); }

    @Inject(method = "setActiveTexture", at = @At("HEAD"))
    private static void npcs$setActiveTexture(int texture, CallbackInfo ci) { GlCache.onActiveTexture(texture); }

}
