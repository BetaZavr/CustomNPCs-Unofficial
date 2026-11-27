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
    protected static final Map<String, ParameterizedModel> MODELS = new HashMap<>(); // list of parameterized
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

        String key = getKey(modelLocation, visibleMeshes, materialTextures, reverseNormals, colorMask);
        ParameterizedModel model = MODELS.get(key);
        if (model == null) { MODELS.put(key, model = new ParameterizedModel(modelLocation, visibleMeshes, materialTextures, reverseNormals, colorMask)); }
        if (model.objModel == null) { model.load(); }
        if (model.objModel == null) { NOT_FOUND.add(modelLocation); return null; }
        return model;
    }

    private static String getKey(ResourceLocation modelLocation, List<String> visibleMeshes, Map<String, ResourceLocation> materialTextures,
                                 boolean reverseNormals, int colorMask) {
        StringBuilder key = new StringBuilder(String.valueOf(modelLocation));
        if (visibleMeshes != null) {
            List<String> meshes = new ArrayList<>(visibleMeshes);
            Collections.sort(meshes);
            key.append(";[");
            for (String mesh : meshes) { key.append(mesh).append(","); }
            key.append("]");
        }
        if (materialTextures != null) {
            TreeMap<String, ResourceLocation> map = materialTextures instanceof TreeMap ? (TreeMap<String, ResourceLocation>) materialTextures : new TreeMap<>(materialTextures);
            key.append(";{");
            for (String material : map.keySet()) { key.append("_").append(material).append("=").append(map.get(material)).append(","); }
            key.append("}");
        }
        key.append(";").append(reverseNormals).append(";").append(colorMask);
        return key.toString();
    }

    private static void trimCache() {
        for (String key : new ArrayList<>(MODELS.keySet())) {
            if (MODELS.size() <= 500) { break; }
            MODELS.remove(key);
        }
    }

}
