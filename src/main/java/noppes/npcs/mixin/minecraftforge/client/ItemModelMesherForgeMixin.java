package noppes.npcs.mixin.minecraftforge.client;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.Item;
import net.minecraftforge.client.ItemModelMesherForge;
import noppes.npcs.items.custom.CustomArmorOBJ;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemModelMesherForge.class, priority = 498, remap = false)
public class ItemModelMesherForgeMixin {

    /**
     * OBJ ItemStack Model Replace
     */
    @Inject(method = "getItemModel", at = @At("HEAD"), cancellable = true)
    protected void getItemModel(Item item, int meta, CallbackInfoReturnable<IBakedModel> cir) {
        if (item instanceof CustomArmorOBJ) {
            cir.setReturnValue(((CustomArmorOBJ) item).getIBakedModel());
        }
    }

}
