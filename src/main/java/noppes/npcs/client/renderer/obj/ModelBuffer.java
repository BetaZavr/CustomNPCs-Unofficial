package noppes.npcs.client.renderer.obj;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.shared.common.util.LogWriter;

import java.util.*;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ModelBuffer {

    /** parameterized rendered models */
    private static final List<ParameterizedModel> MODELS = new ArrayList<>();
    /** missing models so as not to freeze the client */
    private static final List<ResourceLocation> NOT_FOUND = new ArrayList<>();

    /**
     * Attempting to display an OBJ model
     * With all model settings checked
     *
     * @param matrixStack - current matrix
     * @param bufferSource - buffer with rendering settings
     * @param modelLocation - resource for the location of the OBJ model
     * @param visibleMeshes - list of names of meshes/grids that need to be displayed from model
     * @param materialTextures - texture replacement map. Key is a resource for a texture from a material, Value is a new resource texture
     * @param lightMap - [0 <-> 15728880] lighting shade "LightTexture"
     * @param overlay - [0 <-> 655360] lighting from the world "OverlayTexture"
     * @param colorMask - color of the mask applied to the model
     */
    public static void render(PoseStack matrixStack,
                              MultiBufferSource bufferSource,
                              ResourceLocation modelLocation,
                              List<String> visibleMeshes,
                              Map<String, ResourceLocation> materialTextures,
                              int lightMap, int overlay, int colorMask) {
        render(getParameterizedModel(modelLocation, visibleMeshes, materialTextures, false, colorMask), matrixStack, bufferSource, lightMap, overlay);
    }

    /**
     * Trying to quickly display a finished OBJ model
     *
     * @param model - generated custom model
     * @param matrixStack - current matrix
     * @param bufferSource - buffer with rendering settings
     * @param lightMap - [0 <-> 15728880] lighting shade "LightTexture"
     * @param overlay - [0 <-> 655360] lighting from the world "OverlayTexture"
     */
    public static void render(ParameterizedModel model, PoseStack matrixStack, MultiBufferSource bufferSource, int lightMap, int overlay) {
        trimCache();
        if (model != null) {
            try { model.render(matrixStack, bufferSource, lightMap, overlay, Minecraft.getInstance().getPartialTick(), null); }
            catch (Exception e) {
                LogWriter.error("Error render OBJ model \"" + model.modelLocation + "\"", e);
                NOT_FOUND.add(model.modelLocation);
            }
        }
    }

    /**
     * formation of the 3B model
     *
     * @param modelLocation - resource for the location of the OBJ model
     * @param visibleMeshes - list of names of meshes/grids that need to be displayed from model
     * @param materialTextures - texture replacement map. Key is a resource for a texture from a material, Value is a new resource texture
     * @param colorMask - color of the mask applied to the model
     * @return generated custom model
     */
    public static ParameterizedModel getParameterizedModel(ResourceLocation modelLocation,
                                                           List<String> visibleMeshes,
                                                           Map<String, ResourceLocation> materialTextures,
                                                           boolean reverseNormals, int colorMask) {
        if (modelLocation == null || NOT_FOUND.contains(modelLocation)) { return null; }
        ParameterizedModel model = new ParameterizedModel(modelLocation, visibleMeshes, materialTextures, reverseNormals, colorMask);
        boolean found = false;
        for (ParameterizedModel pm : MODELS) {
            if (pm.equals(model)) {
                model = pm;
                found = true;
                break;
            }
        }
        if (model.objModel == null) { model.load(); }
        if (model.objModel == null) { NOT_FOUND.add(modelLocation); return null; }
        if (!found) { MODELS.add(model); }
        return model;
    }

    private static void trimCache() {
        while (MODELS.size() > 500) { MODELS.remove(0); }
    }

}
