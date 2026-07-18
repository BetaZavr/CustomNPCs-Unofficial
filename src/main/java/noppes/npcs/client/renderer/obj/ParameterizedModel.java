package noppes.npcs.client.renderer.obj;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.common.util.LogWriter;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class ParameterizedModel {

	protected static final FloatBuffer COLOR_BUFFER = GLAllocation.createDirectFloatBuffer(4);
	protected static final Vec3d LIGHT0_POS = (new Vec3d(0.2D, 1.0D, -0.7D)).normalize();
	protected static final Vec3d LIGHT1_POS = (new Vec3d(-0.2D, 1.0D, 0.7D)).normalize();

	protected final Function<ResourceLocation, TextureAtlasSprite> spriteFunction;
	protected final float red;
	protected final float green;
	protected final float blue;
	protected IBakedModel bakedModel;

	public final ResourceLocation modelLocation;
	public final int colorMask;
	public final boolean isDynamic;
	public final boolean reverseNormals;
	public final List<String> visibleMeshes = new ArrayList<>();
	public final Map<String, ResourceLocation> materialTextures = new HashMap<>();

	public int listId = -1;
	public OBJModel objModel = null;
	public ResourceLocation atlas = TextureMap.LOCATION_BLOCKS_TEXTURE;

	@SuppressWarnings("ConstantConditions")
	public ParameterizedModel(ResourceLocation modelLocationIn, List<String> visibleMeshesIn, Map<String, ResourceLocation> materialTexturesIn, boolean reverseNormalsIn,  int colorMaskIn, boolean isDynamicIn) {
		modelLocation = modelLocationIn;
		colorMask = colorMaskIn;
		red = (float) (colorMaskIn >> 16 & 255) / 255.0f;
		green = (float) (colorMaskIn >> 8 & 255) / 255.0f;
		blue = (float) (colorMaskIn & 255) / 255.0f;
		reverseNormals = reverseNormalsIn;
		isDynamic = isDynamicIn;
		if (visibleMeshesIn != null) { visibleMeshes.addAll(visibleMeshesIn); }
		if (materialTexturesIn != null) { materialTextures.putAll(materialTexturesIn); }
		spriteFunction = location -> {
			Minecraft mc = Minecraft.getMinecraft();
			if (location.toString().equals("minecraft:missingno") || location.toString().equals("minecraft:builtin/white")) {
				return mc.getTextureMapBlocks().getAtlasSprite(location.toString());
			}
			ResourceLocation loc = location;
			if (materialTextures.containsKey("All")) {
				loc = materialTextures.get("All");
				LogWriter.debug("Replace texture: " + location + " -> " + loc);
			}
			else if (materialTextures.containsKey(location.toString()) && !materialTextures.get(location.toString()).toString().equals(location.toString())) {
				loc = materialTextures.get(location.toString());
				LogWriter.debug("Replace texture: " + location + " -> " + loc);
			}
			TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(loc.toString());
			if (sprite == mc.getTextureMapBlocks().getMissingSprite()) {
				// custom or not load texture to blocks atlas
				try {
					ResourceLocation textureLocation = location;
					if (!location.getResourcePath().toLowerCase().endsWith(".png")) { textureLocation = new ResourceLocation(location.getResourceDomain(), "textures/" + location.getResourcePath() + ".png"); }
					IResource resource = mc.getResourceManager().getResource(textureLocation);
					String key = textureLocation.getResourcePath();
					if (key.contains("textures/")) { key = key.replace("textures/", ""); }
					ResourceLocation atlasKey = new ResourceLocation(CustomNpcs.MODID, "textures/atlas/" + key);
					if (sprite == mc.getTextureMapBlocks().getMissingSprite()) {
						LogWriter.debug("Need create custom atlas \""+atlasKey+"\"");
					}
					sprite = mc.getTextureMapBlocks().registerSprite(atlasKey);
					BufferedImage image = TextureUtil.readBufferedImage(resource.getInputStream());
					int[] pixels = new int[image.getWidth() * image.getHeight()];
					image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
					DynamicTexture dynamicTexture = (DynamicTexture) mc.getTextureManager().getTexture(atlasKey);
					boolean needLoad = dynamicTexture == null;
					if (needLoad) {
						dynamicTexture = new DynamicTexture(image);
						dynamicTexture.updateDynamicTexture();
					}
					sprite.setIconWidth(image.getWidth());
					sprite.setIconHeight(image.getHeight());
					int[][] mipmapPixels = new int[1][];
					mipmapPixels[0] = dynamicTexture.getTextureData();
					sprite.setFramesTextureData(Lists.<int[][]>newArrayList(mipmapPixels));
					sprite.initSprite(image.getWidth(), image.getHeight(), 0, 0, false);
					if (needLoad) {
						mc.getTextureManager().loadTexture(atlasKey, dynamicTexture);
						LogWriter.debug("Create custom atlas \""+atlasKey+"\": from texture: \"" + textureLocation+"\"");
					}
					atlas = atlasKey;
				}
				catch (Exception e) { LogWriter.error(e); }
			}
			return sprite;
		};
	}

	@SuppressWarnings("deprecation")
	public void load() {
		// load model
		try {
			objModel = (OBJModel) OBJLoader.INSTANCE.loadModel(modelLocation).process(ImmutableMap.copyOf(Collections.singletonMap("flip-v", "true")));
			if (visibleMeshes.isEmpty()) {
				visibleMeshes.addAll(objModel.getMatLib().getGroups().keySet());
				visibleMeshes.remove("OBJModel.Default.Element.Name");
			}
			// baked
			bakedModel = objModel.bake(new OBJModel.OBJState(ImmutableList.copyOf(visibleMeshes), true), DefaultVertexFormats.ITEM, spriteFunction);
		}
		catch (Exception ignored) { objModel = null; }
	}

	public void render() {
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.enableDepth();
		Minecraft.getMinecraft().getTextureManager().bindTexture(atlas);
		boolean lit = colorMask != 0;
		if (lit) { setupLight(); }
		if (isDynamic) { draw(); }
		else {
			if (listId < 0) {
				GL11.glNewList(listId = GL11.glGenLists(1), GL11.GL_COMPILE);
				draw();
				GL11.glEndList();
			}
			GlStateManager.callList(listId);
		}
		if (lit) { clearLight(); }
		GlStateManager.color(0.0f, 0.0f, 0.0f, 0.0f);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	protected void setupLight() {
		GlStateManager.enableLighting();
		GlStateManager.enableLight(0);
		GlStateManager.enableLight(1);
		GlStateManager.enableColorMaterial();
		GlStateManager.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
		// color of light from behind
		GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION,
				setColorBuffer((float) LIGHT1_POS.x, (float) LIGHT1_POS.y, (float) LIGHT1_POS.z, 0.0f));
		GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, setColorBuffer(red * 0.2f, green * 0.2f, blue * 0.2f, 1.0F));
		GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
		GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_SPECULAR, setColorBuffer(red * 0.2f, green * 0.2f, blue * 0.2f, 1.0F));
		// color of light from the front
		GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION,
				setColorBuffer((float) LIGHT0_POS.x, (float) -LIGHT0_POS.y, (float) LIGHT0_POS.z, 0.0f));
		GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, setColorBuffer(red * 0.75f, green * 0.75f, blue * 0.75f, 1.0F));
		GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
		GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_SPECULAR, setColorBuffer(red * 0.75f, green * 0.75f, blue * 0.75f, 1.0F));
		GL11.glShadeModel(GL11.GL_FLAT);
		// color of mask on model
		GlStateManager.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, setColorBuffer(red, green, blue, 1.0F));
	}

	protected void clearLight() {
		GlStateManager.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, setColorBuffer(0.2f, 0.2f, 0.2f, 1.0f));
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GlStateManager.disableColorMaterial();
		GlStateManager.disableLight(1);
		GlStateManager.disableLight(0);
		GlStateManager.disableLighting();
	}

	protected void draw() {
		// draw
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
		for (BakedQuad bakedquad : bakedModel.getQuads(null, null, 0)) { buffer.addVertexData(bakedquad.getVertexData()); }
		tessellator.draw();
	}

	private static FloatBuffer setColorBuffer(float red, float green, float blue, float alpha) {
		COLOR_BUFFER.clear();
		COLOR_BUFFER.put(red).put(green).put(blue).put(alpha);
		COLOR_BUFFER.flip();
		return COLOR_BUFFER;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ParameterizedModel)) { return false; }
		ParameterizedModel objPM = (ParameterizedModel) obj;
		if (this == objPM) { return true; }
		if (isDynamic != objPM.isDynamic || colorMask != objPM.colorMask || !modelLocation.equals(objPM.modelLocation)) { return false; }
		if (visibleMeshes.isEmpty() && materialTextures.isEmpty() && objPM.visibleMeshes.isEmpty() && objPM.materialTextures.isEmpty()) { return true; }
		if (visibleMeshes.size() != objPM.visibleMeshes.size()) { return false; }
		for (String name : visibleMeshes) {
			if (!objPM.visibleMeshes.contains(name)) { return false; }
		}
		if (materialTextures.size() != objPM.materialTextures.size()) { return false; }
		for (String name : materialTextures.keySet()) {
			if (!objPM.materialTextures.containsKey(name) || !objPM.materialTextures.get(name).equals(materialTextures.get(name))) { return false; }
		}
		return true;
	}

}
