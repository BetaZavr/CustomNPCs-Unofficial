package noppes.npcs.mixin;

import net.minecraft.Util;
import noppes.npcs.util.CustomNPCsScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Util.class, priority = 498)
public class UtilMixin {

    @Inject(method={"shutdownExecutors"}, at={@At(value="TAIL")})
    private static void shutdownExecutors(CallbackInfo ci) {
        CustomNPCsScheduler.shutDown();
    }

}
