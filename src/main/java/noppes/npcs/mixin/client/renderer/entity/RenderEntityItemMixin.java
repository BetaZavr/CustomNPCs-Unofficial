package noppes.npcs.mixin.client.renderer.entity;

import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.items.custom.CustomArmorOBJ;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderEntityItem.class, priority = 498)
public class RenderEntityItemMixin {

    @Inject(method = "getEntityTexture*",
            at = @At("RETURN"),
            cancellable = true)
    protected void npcs$getEntityTexture(EntityItem entity, CallbackInfoReturnable<ResourceLocation> cir) {
        ItemStack stack = entity.getItem();
        if (!stack.isEmpty() && stack.getItem() instanceof CustomArmorOBJ) {
            ParameterizedModel iModel = ((CustomArmorOBJ) stack.getItem()).getStackModel(stack);
            if (iModel != null) { cir.setReturnValue(iModel.atlas); }
        }
    }

}
