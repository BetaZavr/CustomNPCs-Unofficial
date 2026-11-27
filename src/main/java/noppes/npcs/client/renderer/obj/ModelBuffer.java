package noppes.npcs.client.renderer.obj;

import java.util.*;

import net.minecraft.util.ResourceLocation;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nullable;

public class ModelBuffer {

	// rendered models
	protected static final Map<String, ParameterizedModel> MODELS = new HashMap<>(); // list of parameterized
	public static List<ResourceLocation> NOT_FOUND = new ArrayList<>();	// list of missing models
	// so as not to freeze

	/**
	 * Attempting to display an OBJ model
	 * With all model settings checked
	 *
	 * @param modelLocation - resource for the location of the OBJ model
	 * @param visibleMeshes - list of names of meshes/grids that need to be displayed from model
	 * @param materialTextures - texture replacement map. Key is a resource for a texture from a material, Value is a new resource texture
	 * @param colorMask - color of the mask applied to the model
	 * @param isDynamic - measurement of overlay lighting is displayed on the model
	 */
	public static void render(ResourceLocation modelLocation,
							  List<String> visibleMeshes,
							  Map<String, ResourceLocation> materialTextures, int colorMask, boolean isDynamic) {
		render(getParameterizedModel(modelLocation, visibleMeshes, materialTextures, false, colorMask, isDynamic));
	}

	/**
	 * Trying to quickly display a finished OBJ model
	 *
	 * @param model - generated custom model
	 */
	public static void render(ParameterizedModel model) {
		trimCache();
		if (model != null) {
			try { model.render(); }
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
	public static @Nullable ParameterizedModel getParameterizedModel(ResourceLocation modelLocation,
																	 List<String> visibleMeshes,
																	 Map<String, ResourceLocation> materialTextures,
																	 boolean reverseNormals, int colorMask, boolean isDynamic) {
		if (modelLocation == null || NOT_FOUND.contains(modelLocation)) { return null; }
		String key = getKey(modelLocation, visibleMeshes, materialTextures, reverseNormals, colorMask, isDynamic);
		ParameterizedModel model = MODELS.get(key);
		if (model == null) { MODELS.put(key, model = new ParameterizedModel(modelLocation, visibleMeshes, materialTextures, reverseNormals, colorMask, isDynamic)); }
		if (model.objModel == null) { model.load(null); }
		if (model.objModel == null) { NOT_FOUND.add(modelLocation); return null; }
		return model;
	}

	private static String getKey(ResourceLocation modelLocation, List<String> visibleMeshes, Map<String, ResourceLocation> materialTextures,
								 boolean reverseNormals, int colorMask, boolean isDynamic) {
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
		key.append(";").append(reverseNormals).append(";").append(colorMask).append(";").append(isDynamic);
		return key.toString();
	}

	private static void trimCache() {
		for (String key : new ArrayList<>(MODELS.keySet())) {
			if (MODELS.size() <= 500) { break; }
			MODELS.remove(key);
		}
	}

}
