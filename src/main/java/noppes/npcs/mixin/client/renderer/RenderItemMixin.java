package noppes.npcs.mixin.client.renderer;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.obj.OBJModel;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.items.custom.CustomArmorOBJ;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderItem.class, priority = 498)
public abstract class RenderItemMixin {

    @Final @Shadow private TextureManager textureManager;
    @Shadow public float zLevel;
    @Shadow public void renderItem(ItemStack stack, IBakedModel model) { }
    @Shadow private void setupGuiTransform(int xPosition, int yPosition, boolean isGui3d) { }

    @Inject(method = "renderItemModel",
            at = @At("HEAD"),
            cancellable = true)
    protected void npcs$renderItemModel(ItemStack stack, IBakedModel bakedmodel, ItemCameraTransforms.TransformType transform, boolean leftHanded, CallbackInfo ci) {
        if (!stack.isEmpty() && stack.getItem() instanceof CustomArmorOBJ) {
            ParameterizedModel iModel = ((CustomArmorOBJ) stack.getItem()).getStackModel(stack);
            if (iModel != null) {
                ci.cancel();
                if (!(bakedmodel instanceof OBJModel.OBJBakedModel)) { bakedmodel = iModel.getIBakedModel(); }
                GlStateManager.pushMatrix();
                textureManager.bindTexture(iModel.atlas);
                textureManager.getTexture(iModel.atlas).setBlurMipmap(false, false);

                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.enableRescaleNormal();
                GlStateManager.alphaFunc(516, 0.1F);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

                renderItem(stack, ForgeHooksClient.handleCameraTransforms(bakedmodel, transform, leftHanded));
                GlStateManager.cullFace(GlStateManager.CullFace.BACK);

                // Disable alpha test that was enabled at the start of this method
                GlStateManager.disableAlpha();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableBlend();

                textureManager.bindTexture(iModel.atlas);
                textureManager.getTexture(iModel.atlas).restoreLastBlurMipmap();
                GlStateManager.popMatrix();
            }
        }
    }

    @Inject(method = "renderItemModelIntoGUI",
            at = @At("HEAD"),
            cancellable = true)
    protected void npcs$renderItemModelIntoGUI(ItemStack stack, int x, int y, IBakedModel bakedmodel, CallbackInfo ci) {
        if (!stack.isEmpty() && stack.getItem() instanceof CustomArmorOBJ) {
            ParameterizedModel iModel = ((CustomArmorOBJ) stack.getItem()).getStackModel(stack);
            if (iModel != null) {
                ci.cancel();
                if (!(bakedmodel instanceof OBJModel.OBJBakedModel)) { bakedmodel = iModel.getIBakedModel(); }
                GlStateManager.pushMatrix();
                textureManager.bindTexture(iModel.atlas);
                textureManager.getTexture(iModel.atlas).setBlurMipmap(false, false);

                GlStateManager.enableRescaleNormal();
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(516, 0.1F);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                setupGuiTransform(x, y, bakedmodel.isGui3d());
                renderItem(stack, ForgeHooksClient.handleCameraTransforms(bakedmodel, ItemCameraTransforms.TransformType.GUI, false));

                // Disable alpha test that was enabled at the start of this method
                GlStateManager.disableAlpha();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableLighting();

                textureManager.bindTexture(iModel.atlas);
                textureManager.getTexture(iModel.atlas).restoreLastBlurMipmap();
                GlStateManager.popMatrix();

            }
        }
    }

    /**
     * OBJ ItemStack Model Replace
     */
    @Inject(method = "getItemModelWithOverrides",
            at = @At("HEAD"),
            cancellable = true)
    protected void npcs$getItemModelWithOverrides(ItemStack stack, World worldIn, EntityLivingBase entityIn, CallbackInfoReturnable<IBakedModel> cir) {
        if (!stack.isEmpty() && stack.getItem() instanceof CustomArmorOBJ) {
            ParameterizedModel iModel = ((CustomArmorOBJ) stack.getItem()).getStackModel(stack);
            if (iModel != null) { cir.setReturnValue(iModel.getIBakedModel()); }
        }
    }

}
