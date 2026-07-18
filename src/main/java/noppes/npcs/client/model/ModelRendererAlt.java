package noppes.npcs.client.model;

import java.util.*;

import noppes.npcs.api.util.IModelRenderer;
import noppes.npcs.client.model.animation.AnimationFrameConfig;
import noppes.npcs.client.model.part.ModelPartConfig;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec2f;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.model.animation.AddedPartConfig;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.data.DataAnimation;
import noppes.npcs.items.custom.CustomArmor;

public class ModelRendererAlt
		extends ModelRenderer
		implements IModelRenderer {

	// Data
	public EnumParts part;
	public int partId;
	public int parentPartId = -1;

	private final Map<Integer, PositionTextureVertex> vs = new HashMap<>(); // vs
	private final Map<Integer, TexturedQuad> quads = new HashMap<>(); // fases
	private final Vec2f[] tvs = new Vec2f[8];

	public float x;
	public float y;
	public float z;
	public float xe;
	public float ye0;
	public float ye1;
	public float ye2;
	public float ze;
	public float dx;
	public float dy0;
	public float dy1;
	public float dy2;
	public float dz;
	public float u;
	public float v;

	private int displayList = -1;
	private ParameterizedModel displayOBJListUp = null;
	private ParameterizedModel displayOBJListDown = null;
	public float rotateAngleX1 = 0.0f;
	public float rotateAngleY1 = 0.0f;

	public float scaleX = 1.0f;
	public float scaleY = 1.0f;
	public float scaleZ = 1.0f;
	public float offsetAnimX = 0.0f;
	public float offsetAnimY = 0.0f;
	public float offsetAnimZ = 0.0f;
	private boolean normalTop = false;
	public boolean isNormal = false;
	public final boolean baseNormal;

	public boolean smallArms;
	public boolean isAnimPart;
	private float r = 1.0f;
	private float g = 1.0f;
	private float b = 1.0f;
	private float al = 0.0f;
	private final float[] baseRotationPoint;

	// Custom
	public ResourceLocation textureLocation = null;
	public ResourceLocation objLocationUp = null;
	public ResourceLocation objLocationDown = null;
	public boolean isArmor;

	public ModelRendererAlt(ModelBase model, EnumParts enumPart, int textureU, int textureV, boolean normal) {
		super(model, textureU, textureV);
		part = enumPart;
		partId = enumPart.patterns;
		u = textureU;
		v = textureV;
		isNormal = normal;
		baseNormal = normal;
		baseRotationPoint = new float[] { rotationPointX, rotationPointY, rotationPointZ };
	}

	public ModelRendererAlt(ModelBase baseModel, AddedPartConfig addedPartConfig) {
		super(baseModel, addedPartConfig.textureU, addedPartConfig.textureV);
		part = EnumParts.CUSTOM;
		partId = addedPartConfig.id;
		parentPartId = addedPartConfig.parentPart;
		u = addedPartConfig.textureU;
		v = addedPartConfig.textureV;
		isNormal = addedPartConfig.isNormal;
		baseNormal = addedPartConfig.isNormal;
		objLocationUp = addedPartConfig.objUp;
		objLocationDown = addedPartConfig.objDown;
		textureLocation = addedPartConfig.location;
		Map<String, ResourceLocation> map = new HashMap<>();
		map.put("All", textureLocation);
		if (objLocationUp != null) { displayOBJListUp = ModelBuffer.getParameterizedModel(objLocationUp, null, map, false, 0, true); }
		if (objLocationDown != null) { displayOBJListDown = ModelBuffer.getParameterizedModel(objLocationDown, null, map, false, 0, true); }
		baseRotationPoint = new float[] { rotationPointX, rotationPointY, rotationPointZ };
		setBox(addedPartConfig.pos[0], addedPartConfig.pos[1], addedPartConfig.pos[2], addedPartConfig.size[0], addedPartConfig.size[1], addedPartConfig.size[2], addedPartConfig.size[3], addedPartConfig.size[4], 0.0f);
		setRotationPoint(addedPartConfig.rot[0], addedPartConfig.rot[1], addedPartConfig.rot[2]);
	}

	public ModelRendererAlt(ModelBase model) {
		super(model);
		baseNormal = true;
		baseRotationPoint = new float[] { rotationPointX, rotationPointY, rotationPointZ };
	}

	public void setBox(float x, float y, float z, float dx, float dy0, float dy1, float dy2, float dz, float wear) {
		xe = x + dx + wear;
		ye0 = y + dy0;
		ye1 = y + dy0 + dy1;
		ye2 = y + dy0 + dy1 + dy2 + wear;
		ze = z + dz + wear;
		this.x = x - wear;
		this.y = y - wear;
		this.z = z - wear;
		this.dx = dx;
		this.dz = dz;
		this.dy0 = dy0;
		this.dy1 = this.dy0 + dy1;
		this.dy2 = this.dy1 + dy2;
		if (mirror) {
			float f3 = xe;
			xe = x;
			this.x = f3;
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void postRender(float scale) {
		if (offsetAnimX != 0.0f || offsetAnimY != 0.0f || offsetAnimZ != 0.0f || smallArms) {
			float ox = offsetAnimX;
			if (smallArms) { ox += part == EnumParts.ARM_LEFT ? -0.020833f : 0.020833f; }
			GlStateManager.translate(ox, offsetAnimY, offsetAnimZ);
		}
		GlStateManager.translate(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);

		if (rotateAngleZ != 0.0F) { GlStateManager.rotate(rotateAngleZ * (180F / (float)Math.PI), 0.0F, 0.0F, 1.0F); }
		if (rotateAngleY != 0.0F) { GlStateManager.rotate(rotateAngleY * (180F / (float)Math.PI), 0.0F, 1.0F, 0.0F); }
		if (rotateAngleX != 0.0F) { GlStateManager.rotate(rotateAngleX * (180F / (float)Math.PI), 1.0F, 0.0F, 0.0F); }

		if (scaleX != 1.0f || scaleY != 1.0f || scaleZ != 1.0f || smallArms) {
			float sx = scaleX;
			if (smallArms) { sx *= dx / (dx + 1.0f); }
			GlStateManager.scale(sx, scaleY, scaleZ);
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void render(float scale) {
		if (isHidden || !showModel) { return; }
		GlStateManager.pushMatrix();
		postRender(scale);
		// Tint
		if (al != 0.0f) {
			GlStateManager.color(r, g, b, al);
		} else {
			GlStateManager.color(r, g, b, 1.0f);
		}
		// render
		if (textureLocation != null) { Minecraft.getMinecraft().getTextureManager().bindTexture(textureLocation); }
		if (!notOBJModel()) { objDraw(); }
		else {
			clearData();
			if (isNormal || !CustomNpcs.ShowJoints || (rotateAngleX1 == 0.0f && rotateAngleY1 == 0.0f)) {
				simpleDraw(scale);
			} else {
				rotateTop();
				drawJoint(scale);
			}
		}
		// Child Models
		if (childModels != null && !childModels.isEmpty()) {
			List<ModelRenderer> del = new ArrayList<>();
			for (ModelRenderer model : childModels) {
				if (model instanceof ModelRendererAlt) {
					((ModelRendererAlt) model).checkBacklightColor(r, g, b);
					if (((ModelRendererAlt) model).part == EnumParts.CUSTOM) { del.add(model); }
				}
				model.render(scale);
			}
			for (ModelRenderer model : del) { childModels.remove(model); }
		}
		GlStateManager.popMatrix();
	}

	private void clearData() {
		vs.clear();
		quads.clear();
	}

	private void rotateTop() {
		if (rotateAngleY1 == 0.0f) { // Normal
			if (!normalTop) {
				tvs[0] = new Vec2f(x, z);
				tvs[1] = new Vec2f(xe, z);
				tvs[2] = new Vec2f(xe, ze);
				tvs[3] = new Vec2f(x, ze);
				tvs[4] = new Vec2f(x, z);
				tvs[5] = new Vec2f(xe, z);
				tvs[6] = new Vec2f(xe, ze);
				tvs[7] = new Vec2f(x, ze);
				normalTop = true;
			}
		} else { // rotate
			// Base
			float limit = (float) (Math.PI / 2.0f);
			if (rotateAngleY1 > limit) { rotateAngleY1 = limit; }
			if (rotateAngleY1 < -limit) { rotateAngleY1 = -limit; }
			double cos = Math.cos(-rotateAngleY1);
			double sin = Math.sin(-rotateAngleY1);
			Vec2f xMzM = rotate(new Vec2f(x + (xe - x) / 2,  z + (ze - z) / 2), new Vec2f(x, z), cos, sin);
			Vec2f xNzM = rotate(new Vec2f(x + (xe - x) / 2,  z + (ze - z) / 2), new Vec2f(xe, z), cos, sin);
			Vec2f xNzN = rotate(new Vec2f(x + (xe - x) / 2,  z + (ze - z) / 2), new Vec2f(xe, ze), cos, sin);
			Vec2f xMzN = rotate(new Vec2f(x + (xe - x) / 2,  z + (ze - z) / 2), new Vec2f(x, ze), cos, sin);
			tvs[0] = new Vec2f(xMzM.x, xMzM.y);
			tvs[1] = new Vec2f(xNzM.x, xNzM.y);
			tvs[2] = new Vec2f(xNzN.x, xNzN.y);
			tvs[3] = new Vec2f(xMzN.x, xMzN.y);
			tvs[4] = new Vec2f(xMzM.x, xMzM.y);
			tvs[5] = new Vec2f(xNzM.x, xNzM.y);
			tvs[6] = new Vec2f(xNzN.x, xNzN.y);
			tvs[7] = new Vec2f(xMzN.x, xMzN.y);
			normalTop = false;
		}
	}

	// 3D part model
	private void objDraw() {
		switch (part) {
			case HEAD:
			case BODY: {
				GlStateManager.translate(0.0f, 1.5f, 0.0f);
				break;
			}
			case ARM_RIGHT: {
				GlStateManager.translate(0.3175f, 1.375f, 0.0f);
				break;
			}
			case ARM_LEFT: {
				GlStateManager.translate(-0.3175f, 1.375f, 0.0f);
				break;
			}
			case LEG_RIGHT: {
				GlStateManager.translate(0.125f, 0.75f, 0.0f);
				break;
			}
			case LEG_LEFT: {
				GlStateManager.translate(-0.115f, 0.75f, 0.0f);
				break;
			}
			default: {
				break;
			}
		}
		GlStateManager.enableBlend();
		GlStateManager.rotate(180.0f, 1.0f, 0.0f, 0.0f);
		if (displayOBJListUp != null) {
			ModelBuffer.render(displayOBJListUp);
		}
		if (displayOBJListDown != null) {
			if (rotateAngleX1 != 0.0f) {
				boolean isArm = part.name().toLowerCase().contains("arm");
				float ofsY = dy2 - dy0;
				if (isArm) {
					GlStateManager.translate(0.0f, 0.75f, 0.0f);
				}
				float ofsZ = rotateAngleX1 * (dz / 2.0f) / (float) -Math.PI;
				GlStateManager.translate(0.0f, ofsY * 0.0625f, ofsZ * 0.0625f);
				GlStateManager.rotate(rotateAngleX1 * 180.0f / (float) Math.PI, 1.0f, 0.0f, 0.0f);
				GlStateManager.translate(0.0f, ofsY * -0.0625f, ofsZ * -0.0625f);
				if (isArm) {
					GlStateManager.translate(0.0f, -0.75f, 0.0f);
				}
			}
			if (rotateAngleY1 != 0.0f) {
				boolean isArm = part.name().toLowerCase().contains("arm");
				float ofs = (part.name().toLowerCase().contains("right") ? -1.0f : 1.0f) * (isArm ? 0.375f : 0.125f);
				GlStateManager.translate(ofs, 0.0f, 0.0f);
				GlStateManager.rotate(rotateAngleY1 * 180.0f / (float) Math.PI, 0.0f, 1.0f, 0.0f);
				GlStateManager.translate(-ofs, 0.0f, 0.0f);
			}
			ModelBuffer.render(displayOBJListDown);
		}
		GlStateManager.disableBlend();
	}

	// Normal cuboid
	private void simpleDraw(float scale) {
		if (displayList > 0) {
			GlStateManager.callList(displayList);
			if (childModels != null) {
				for (ModelRenderer childModel : childModels) { childModel.render(scale); }
			}
			return;
		}
		vs.put(0, new PositionTextureVertex(x, y, z, 0.0F, 0.0F));
		vs.put(1, new PositionTextureVertex(xe, y, z, 0.0F, 8.0F));
		vs.put(2, new PositionTextureVertex(xe, ye2, z, 8.0F, 8.0F));
		vs.put(3, new PositionTextureVertex(x, ye2, z, 8.0F, 0.0F));
		vs.put(4, new PositionTextureVertex(x, y, ze, 0.0F, 0.0F));
		vs.put(5, new PositionTextureVertex(xe, y, ze, 0.0F, 8.0F));
		vs.put(6, new PositionTextureVertex(xe, ye2, ze, 8.0F, 8.0F));
		vs.put(7, new PositionTextureVertex(x, ye2, ze, 8.0F, 0.0F));
		setQuard(0, vs.get(5), vs.get(1), vs.get(2), vs.get(6), u + dz + dx, v + dz, u + dz + dx + dz, v + dz + dy2); // right
		setQuard(1, vs.get(0), vs.get(4), vs.get(7), vs.get(3), u, v + dz, u + dz, v + dz + dy2); // left
		setQuard(2, vs.get(5), vs.get(4), vs.get(0), vs.get(1), u + dz, v, u + dz + dx, v + dz); // up
		setQuard(3, vs.get(2), vs.get(3), vs.get(7), vs.get(6), u + dz + dx, v + dz, u + dz + dx + dx, v); // down
		setQuard(4, vs.get(1), vs.get(0), vs.get(3), vs.get(2), u + dz, v + dz, u + dz + dx, v + dz + dy2); // front
		setQuard(5, vs.get(4), vs.get(5), vs.get(6), vs.get(7), u + dz + dx + dz, v + dz, u + dz + dx + dz + dx, v + dz + dy2); // back
		GlStateManager.glNewList(displayList = GLAllocation.generateDisplayLists(1), 4864);
		draw(scale);
		GL11.glEndList();
	}

	// Joint and rotate Top
	private void drawJoint(float scale) {
		boolean counterClockWise = rotateAngleY1 <= 0.0f;
		if (isArmor && part != EnumParts.ARM_RIGHT && part != EnumParts.LEG_RIGHT) { counterClockWise = !counterClockWise; }
		vs.put(0, new PositionTextureVertex(x, y, z, 0.0F, 0.0F));
		vs.put(1, new PositionTextureVertex(xe, y, z, 0.0F, 8.0F));
		vs.put(2, new PositionTextureVertex(xe, y, ze, 8.0F, 8.0F));
		vs.put(3, new PositionTextureVertex(x, y, ze, 8.0F, 0.0F));
		int i = 0;
		// level #0
		setQuard(i++, vs.get(2), vs.get(3), vs.get(0), vs.get(1), u + dz, v, u + dz + dx, v + dz); // up
		// Calculate
		float cos0 = (float) Math.cos(-rotateAngleX1 / 3.0f), sin0 = (float) Math.sin(-rotateAngleX1 / 3.0f);
		float cos1 = (float) Math.cos(-rotateAngleX1 / 1.5f), sin1 = (float) Math.sin(-rotateAngleX1 / 1.5f);
		float cos2 = (float) Math.cos(-rotateAngleX1), sin2 = (float) Math.sin(-rotateAngleX1);
		float tan = (float) Math.tan(-rotateAngleX1 / 2.0f);
		if (rotateAngleX1 <= -3.1415) { tan = 0.0f; } // remove infinity
		Vec2f cr = new Vec2f(ye0, rotateAngleX1 * -ze / (float) -Math.PI); // center
		Vec2f g0;
		Vec2f g1;
		Vec2f g2;
		float t;// down
		if (rotateAngleX1 < 0.0f) {
			// Calculated fillet positions
			g0 = new Vec2f(cr.x + (ze - cr.y) * sin0 + (ye0 - cr.x) * cos0, cr.y + (ze - cr.y) * cos0 - (ye0 - cr.x) * sin0);
			g1 = new Vec2f(cr.x + (ze - cr.y) * sin1 + (ye0 - cr.x) * cos1, cr.y + (ze - cr.y) * cos1 - (ye0 - cr.x) * sin1);
			g2 = new Vec2f(cr.x + (ze - cr.y) * sin2 + (ye0 - cr.x) * cos2, cr.y + (ze - cr.y) * cos2 - (ye0 - cr.x) * sin2);
			t = (z - cr.y) * tan;
			Vec2f g3 = new Vec2f(ye0 + t, z);

			float d0 = (float) Math.hypot(g0.x-g1.x, g0.y-g1.y);
			float n0 = dy0 / (dy0 + d0 * 1.5f) * d0 * 1.5f;
			float n1 = (dy1 - dy0) / ((dy1 - dy0) + d0 * 1.5f) * d0 * 1.5f;
			float dy00 = dy0 - n0;
			float dy01 = dy00 + n0 / 1.5f;
			float dy02 = dy0 + n1 / 3.0f;
			float dy03 = (dy0 + n1) * 0.85f;
			vs.put(4, new PositionTextureVertex(x, g3.x, z, 0.0F, 0.0F));
			vs.put(5, new PositionTextureVertex(xe, g3.x, z, 0.0F, 8.0F));
			vs.put(6, new PositionTextureVertex(xe, ye0, ze, 8.0F, 8.0F));
			vs.put(7, new PositionTextureVertex(x, ye0, ze, 8.0F, 0.0F));
			// level #0
			setQuard(i++, vs.get(1), vs.get(0), vs.get(4), vs.get(5), u + dz, v + dz, u + dz + dx, v + dz + dy0); // front
			setQuard(i++, vs.get(3), vs.get(2), vs.get(6), vs.get(7), u + 2 * dz + dx, v + dz, u + 2 * (dz + dx), v + dz + dy00); // back
			setQuard(i++, vs.get(1), vs.get(2), vs.get(6), vs.get(5), // inside
					u + dz + dx, v + dz,
					u + 2 * dz + dx, v + dz,
					u + 2 * dz + dx, v + dz + dy00,
					u + dz + dx, v + dz + dy0);
			setQuard(i++, vs.get(0), vs.get(3), vs.get(7), vs.get(4), // outside
					u + dz, v + dz,
					u, v + dz,
					u, v + dz + dy00,
					u + dz, v + dz + dy0);
			// level #1
			vs.put(8, new PositionTextureVertex(xe, g0.x, g0.y, 0.0F, 0.0F));
			vs.put(9, new PositionTextureVertex(x, g0.x, g0.y, 0.0F, 8.0F));
			setQuard(i++, vs.get(7), vs.get(6), vs.get(8), vs.get(9), // back
					u + 2 * (dz + dx), v + dz + dy00,
					u + 2 * dz + dx, v + dz + dy00,
					u + 2 * dz + dx, v + dz + dy01,
					u + 2 * (dz + dx), v + dz + dy01);
			setQuard(i++, vs.get(5), vs.get(6), vs.get(8), vs.get(5), // inside
					u + dz + dx, v + dz + dy0,
					u + 2 * dz + dx, v + dz + dy00,
					u + 2 * dz + dx, v + dz + dy01,
					u + dz + dx, v + dz + dy0);
			setQuard(i++, vs.get(4), vs.get(7), vs.get(9), vs.get(4), // outside
					u + dz, v + dz + dy0,
					u, v + dz + dy00,
					u, v + dz + dy01,
					u + dz, v + dz + dy0);
			// level #2
			vs.put(10, new PositionTextureVertex(xe, g1.x, g1.y, 0.0F, 0.0F));
			vs.put(11, new PositionTextureVertex(x, g1.x, g1.y, 0.0F, 8.0F));
			setQuard(i++, vs.get(9), vs.get(8), vs.get(10), vs.get(11), // back
					u + 2 * (dz + dx), v + dz + dy01,
					u + 2 * dz + dx, v + dz + dy01,
					u + 2 * dz + dx, v + dz + dy02,
					u + 2 * (dz + dx), v + dz + dy02);
			setQuard(i++, vs.get(5), vs.get(8), vs.get(10), vs.get(5), // inside
					u + dz + dx, v + dz + dy0,
					u + 2 * dz + dx, v + dz + dy01,
					u + 2 * dz + dx, v + dz + dy02,
					u + dz + dx, v + dz + dy0);
			setQuard(i++, vs.get(4), vs.get(9), vs.get(11), vs.get(4), // outside
					u + dz, v + dz + dy0,
					u, v + dz + dy01,
					u, v + dz + dy02,
					u + dz, v + dz + dy0);
			// level #3
			vs.put(12, new PositionTextureVertex(xe, g2.x, g2.y, 0.0F, 0.0F));
			vs.put(13, new PositionTextureVertex(x, g2.x, g2.y, 0.0F, 8.0F));
			setQuard(i++, vs.get(11), vs.get(10), vs.get(12), vs.get(13), // back
					u + 2 * (dz + dx), v + dz + dy02,
					u + 2 * dz + dx, v + dz + dy02,
					u + 2 * dz + dx, v + dz + dy03,
					u + 2 * (dz + dx), v + dz + dy03);
			setQuard(i++, vs.get(5), vs.get(10), vs.get(12), vs.get(5), // inside
					u + dz + dx, v + dz + dy0,
					u + 2 * dz + dx, v + dz + dy02,
					u + 2 * dz + dx, v + dz + dy03,
					u + dz + dx, v + dz + dy0);
			setQuard(i++, vs.get(4), vs.get(11), vs.get(13), vs.get(4), // outside
					u + dz, v + dz + dy0,
					u, v + dz + dy02,
					u, v + dz + dy03,
					u + dz, v + dz + dy0);
			// level #4
			Vec2f yz14 = rotate(cr, new Vec2f(ye1, tvs[0].y), cos2, sin2);
			Vec2f yz15 = rotate(cr, new Vec2f(ye1, tvs[1].y), cos2, sin2);
			Vec2f yz16 = rotate(cr, new Vec2f(ye1, tvs[2].y), cos2, sin2);
			Vec2f yz17 = rotate(cr, new Vec2f(ye1, tvs[3].y), cos2, sin2);
			vs.put(14, new PositionTextureVertex(tvs[0].x, yz14.x, yz14.y, 0.0F, 0.0F));
			vs.put(15, new PositionTextureVertex(tvs[1].x, yz15.x, yz15.y, 0.0F, 0.0F));
			vs.put(16, new PositionTextureVertex(tvs[2].x, yz16.x, yz16.y, 0.0F, 0.0F));
			vs.put(17, new PositionTextureVertex(tvs[3].x, yz17.x, yz17.y, 0.0F, 0.0F));
			if (counterClockWise) {
				setQuard(i++, vs.get(5), vs.get(4), vs.get(15), vs.get(15),
						u + dz + dx, v + dz + dy0,
						u + dz, v + dz + dy0,
						u + dz + dx, v + dz + dy1,
						u + dz + dx, v + dz + dy1); // front-left
				setQuard(i++, vs.get(15), vs.get(4), vs.get(14), vs.get(14),
						u + dz + dx, v + dz + dy1,
						u + dz, v + dz + dy0,
						u + dz, v + dz + dy1,
						u + dz, v + dz + dy1); // front-right
				setQuard(i++, vs.get(13), vs.get(12), vs.get(17), vs.get(17),
						u + 2 * dz + dx,
						v + dz + dy03,
						u + 2 * (dz + dx),
						v + dz + dy1); // back-left
				setQuard(i++, vs.get(16), vs.get(17), vs.get(12), vs.get(16),
						u + 2 * dz + dx, v + dz + dy1,
						u + 2 * (dz + dx), v + dz + dy1,
						u + 2 * dz + dx, v + dz + dy03,
						u + 2 * dz + dx, v + dz + dy1); // back-right
				setQuard(i++, vs.get(16), vs.get(5), vs.get(15), vs.get(15),
						u + 2 * dz + dx, v + dz + dy1,
						u + dz + dx, v + dz + dy0,
						u + dz + dx, v + dz + dy1,
						u + dz + dx, v + dz + dy1); // left-front
				setQuard(i++, vs.get(12), vs.get(5), vs.get(16), vs.get(16),
						u + 2 * dz + dx, v + dz + dy03,
						u + dz + dx, v + dz + dy0,
						u + 2 * dz + dx, v + dz + dy1,
						u + 2 * dz + dx, v + dz + dy1); // left-back
				setQuard(i++, vs.get(4), vs.get(13), vs.get(14), vs.get(14),
						u + dz, v + dz + dy0,
						u, v + dz + dy03,
						u + dz, v + dz + dy1,
						u + dz, v + dz + dy1); // right-front
				setQuard(i++, vs.get(14), vs.get(13), vs.get(17), vs.get(17),
						u + dz, v + dz + dy1,
						u, v + dz + dy03,
						u, v + dz + dy1,
						u, v + dz + dy1); // right-back
			}
			else {
				setQuard(i++, vs.get(14), vs.get(15), vs.get(5), vs.get(5),
						u + dz, v + dz + dy1,
						u + dz + dx, v + dz + dy1,
						u + dz + dx, v + dz + dy03,
						u + dz + dx, v + dz + dy03); // front-left
				setQuard(i++, vs.get(5), vs.get(4), vs.get(14), vs.get(14),
						u + dz + dx, v + dz + dy03,
						u + dz, v + dz + dy03,
						u + dz, v + dz + dy1,
						u + dz, v + dz + dy1); // front-right
				setQuard(i++, vs.get(16), vs.get(17), vs.get(13), vs.get(13),
						u + 2 * dz + dx, v + dz + dy1,
						u + 2 * (dz + dx), v + dz + dy1,
						u + 2 * (dz + dx), v + dz + dy03,
						u + 2 * (dz + dx), v + dz + dy03); // back-left
				setQuard(i++, vs.get(13), vs.get(12), vs.get(16), vs.get(16),
						u + 2 * (dz + dx), v + dz + dy03,
						u + 2 * dz + dx, v + dz + dy03,
						u + 2 * dz + dx, v + dz + dy1,
						u + 2 * dz + dx, v + dz + dy1); // back-right
				setQuard(i++, vs.get(12), vs.get(5), vs.get(15), vs.get(15),
						u + 2 * dz + dx, v + dz + dy03,
						u + dz + dx, v + dz + dy03,
						u + dz + dx, v + dz + dy1,
						u + dz + dx, v + dz + dy1); // left-front
				setQuard(i++, vs.get(15), vs.get(16), vs.get(12), vs.get(12),
						u + dz + dx, v + dz + dy1,
						u + 2 * dz + dx, v + dz + dy1,
						u + 2 * dz + dx, v + dz + dy03,
						u + 2 * dz + dx, v + dz + dy03); // left-back
				setQuard(i++, vs.get(17), vs.get(14), vs.get(4), vs.get(4),
						u, v + dz + dy1,
						u + dz, v + dz + dy1,
						u + dz, v + dz + dy03,
						u + dz, v + dz + dy03); // right-front
				setQuard(i++, vs.get(4), vs.get(13), vs.get(17), vs.get(17),
						u + dz, v + dz + dy03,
						u, v + dz + dy03,
						u, v + dz + dy1,
						u, v + dz + dy1); // right-back
			}
			// level #5
			Vec2f yz18 = rotate(cr, new Vec2f(ye2, tvs[4].y), cos2, sin2);
			Vec2f yz19 = rotate(cr, new Vec2f(ye2, tvs[5].y), cos2, sin2);
			Vec2f yz20 = rotate(cr, new Vec2f(ye2, tvs[6].y), cos2, sin2);
			Vec2f yz21 = rotate(cr, new Vec2f(ye2, tvs[7].y), cos2, sin2);
			vs.put(18, new PositionTextureVertex(tvs[4].x, yz18.x, yz18.y, 0.0F, 0.0F));
			vs.put(19, new PositionTextureVertex(tvs[5].x, yz19.x, yz19.y, 0.0F, 0.0F));
			vs.put(20, new PositionTextureVertex(tvs[6].x, yz20.x, yz20.y, 0.0F, 0.0F));
			vs.put(21, new PositionTextureVertex(tvs[7].x, yz21.x, yz21.y, 0.0F, 0.0F));
		}
		else {
			// Calculated fillet positions
			g0 = new Vec2f(cr.x + (z - cr.y) * sin0 + (ye0 - cr.x) * cos0, cr.y + (z - cr.y) * cos0 - (ye0 - cr.x) * sin0);
			g1 = new Vec2f(cr.x + (z - cr.y) * sin1 + (ye0 - cr.x) * cos1, cr.y + (z - cr.y) * cos1 - (ye0 - cr.x) * sin1);
			g2 = new Vec2f(cr.x + (z - cr.y) * sin2 + (ye0 - cr.x) * cos2, cr.y + (z - cr.y) * cos2 - (ye0 - cr.x) * sin2);
			t = (ze - cr.y) * tan;
			Vec2f g3 = new Vec2f(ye0 + t, ze);
			float d0 = (float) Math.hypot(g0.x-g1.x, g0.y-g1.y);
			float n0 = dy0 / (dy0 + d0 * 1.5f) * d0 * 1.5f;
			float n1 = (dy1 - dy0) / ((dy1 - dy0) + d0 * 1.5f) * d0 * 1.5f;
			float dy00 = dy0 - n0;
			float dy01 = dy00 + n0 / 1.5f;
			float dy02 = dy0 + n1 / 3.0f;
			float dy03 = dy0 + n1;
			vs.put(4, new PositionTextureVertex(x, ye0, z, 0.0F, 0.0F));
			vs.put(5, new PositionTextureVertex(xe, ye0, z, 0.0F, 8.0F));
			vs.put(6, new PositionTextureVertex(xe, g3.x, ze, 8.0F, 8.0F));
			vs.put(7, new PositionTextureVertex(x, g3.x, ze, 8.0F, 0.0F));
			// level #0
			setQuard(i++, vs.get(1), vs.get(0), vs.get(4), vs.get(5), u + dz, v + dz, u + dz + dx, v + dz + dy00); // front
			setQuard(i++, vs.get(3), vs.get(2), vs.get(6), vs.get(7), u + 2 * dz + dx, v + dz, u + 2 * (dz + dx), v + dz + dy0); // back
			setQuard(i++, vs.get(1), vs.get(2), vs.get(6), vs.get(5), // inside
					u + dz + dx, v + dz,
					u + 2 * dz + dx, v + dz,
					u + 2 * dz + dx, v + dz + dy0,
					u + dz + dx, v + dz + dy00);
			setQuard(i++, vs.get(0), vs.get(3), vs.get(7), vs.get(4), // outside
					u + dz, v + dz,
					u, v + dz,
					u, v + dz + dy0,
					u + dz, v + dz + dy00);
			// level #1
			vs.put(8, new PositionTextureVertex(xe, g0.x, g0.y, 0.0F, 0.0F));
			vs.put(9, new PositionTextureVertex(x, g0.x, g0.y, 0.0F, 8.0F));
			setQuard(i++, vs.get(5), vs.get(4), vs.get(9), vs.get(8),
					u + dz + dx, v + dz + dy00,
					u + dz, v + dz + dy00,
					u + dz, v + dz + dy01,
					u + dz + dx, v + dz + dy01); // front-segment-1
			setQuard(i++, vs.get(5), vs.get(6), vs.get(8), vs.get(5),
					u + dz + dx, v + dz + dy00,
					u + 2 * dz + dx, v + dz + dy0,
					u + dz + dx, v + dz + dy01,
					u + dz + dx, v + dz + dy00); // left-segment-1
			setQuard(i++, vs.get(4), vs.get(7), vs.get(9), vs.get(4),
					u + dz, v + dz + dy00,
					u, v + dz + dy0,
					u + dz, v + dz + dy01,
					u + dz, v + dz + dy00); // right-segment-1
			// level #2
			vs.put(10, new PositionTextureVertex(xe, g1.x, g1.y, 0.0F, 0.0F));
			vs.put(11, new PositionTextureVertex(x, g1.x, g1.y, 0.0F, 8.0F));
			setQuard(i++, vs.get(8), vs.get(9), vs.get(11), vs.get(10),
					u + dz + dx, v + dz + dy01,
					u + dz, v + dz + dy01,
					u + dz, v + dz + dy02,
					u + dz + dx, v + dz + dy02); // front-segment-2
			setQuard(i++, vs.get(8), vs.get(6), vs.get(10), vs.get(8),
					u + dz + dx, v + dz + dy01,
					u + 2 * dz + dx, v + dz + dy0,
					u + dz + dx, v + dz + dy02,
					u + dz + dx, v + dz + dy01); // left-segment-2
			setQuard(i++, vs.get(9), vs.get(7), vs.get(11), vs.get(9),
					u + dz, v + dz + dy01,
					u, v + dz + dy0,
					u + dz, v + dz + dy02,
					u + dz, v + dz + dy01); // right-segment-2
			// level #3
			vs.put(12, new PositionTextureVertex(xe, g2.x, g2.y, 0.0F, 0.0F));
			vs.put(13, new PositionTextureVertex(x, g2.x, g2.y, 0.0F, 8.0F));
			setQuard(i++, vs.get(10), vs.get(11), vs.get(13), vs.get(12),
					u + dz + dx, v + dz + dy02,
					u + dz, v + dz + dy02,
					u + dz, v + dz + dy03,
					u + dz + dx, v + dz + dy03); // front-segment-3
			setQuard(i++, vs.get(10), vs.get(6), vs.get(12), vs.get(10),
					u + dz + dx, v + dz + dy02,
					u + 2 * dz + dx, v + dz + dy0,
					u + dz + dx, v + dz + dy03,
					u + dz + dx, v + dz + dy02); // left-segment-3
			setQuard(i++, vs.get(11), vs.get(7), vs.get(13), vs.get(11),
					u + dz, v + dz + dy02,
					u, v + dz + dy0,
					u + dz, v + dz + dy03,
					u + dz, v + dz + dy02); // right-segment-3
			// level #4
			Vec2f yz14 = rotate(cr, new Vec2f(ye1, tvs[0].y), cos2, sin2);
			Vec2f yz15 = rotate(cr, new Vec2f(ye1, tvs[1].y), cos2, sin2);
			Vec2f yz16 = rotate(cr, new Vec2f(ye1, tvs[2].y), cos2, sin2);
			Vec2f yz17 = rotate(cr, new Vec2f(ye1, tvs[3].y), cos2, sin2);
			vs.put(14, new PositionTextureVertex(tvs[0].x, yz14.x, yz14.y, 0.0F, 0.0F));
			vs.put(15, new PositionTextureVertex(tvs[1].x, yz15.x, yz15.y, 0.0F, 0.0F));
			vs.put(16, new PositionTextureVertex(tvs[2].x, yz16.x, yz16.y, 0.0F, 0.0F));
			vs.put(17, new PositionTextureVertex(tvs[3].x, yz17.x, yz17.y, 0.0F, 0.0F));
			if (counterClockWise) {
				setQuard(i++, vs.get(15), vs.get(12), vs.get(13), vs.get(15),
						u + dz + dx, v + dz + dy1,
						u + dz + dx, v + dz + dy03,
						u + dz, v + dz + dy03,
						u + dz + dx, v + dz + dy1); // front-left
				setQuard(i++, vs.get(15), vs.get(13), vs.get(14), vs.get(15),
						u + dz + dx, v + dz + dy1,
						u + dz, v + dz + dy03,
						u + dz, v + dz + dy1,
						u + dz + dx, v + dz + dy1); // front-right
				setQuard(i++, vs.get(7), vs.get(6), vs.get(17), vs.get(17),
						u + 2 * dz + dx,
						v + dz + dy0,
						u + 2 * (dz + dx),
						v + dz + dy1); // back-right
				setQuard(i++, vs.get(17), vs.get(6), vs.get(16), vs.get(17),
						u + 2 * dz + dx,
						v + dz + dy0,
						u + 2 * (dz + dx),
						v + dz + dy1); // back-left
				setQuard(i++, vs.get(6), vs.get(12), vs.get(16), vs.get(16),
						u + dz + dx,
						v + dz + dy03,
						u + 2 * dz + dx,
						v + dz + dy1); // left-back
				setQuard(i++, vs.get(16), vs.get(12), vs.get(15), vs.get(16),
						u + dz + dx,
						v + dz + dy03,
						u + 2 * dz + dx,
						v + dz + dy1); // left-front
				setQuard(i++, vs.get(13), vs.get(7), vs.get(14), vs.get(14),
						u,
						v + dz + dy03,
						u + dz,
						v + dz + dy1); // right-back
				setQuard(i++, vs.get(14), vs.get(7), vs.get(17), vs.get(14),
						u,
						v + dz + dy03,
						u + dz,
						v + dz + dy1); // right-front
			}
			else {
				setQuard(i++, vs.get(13), vs.get(12), vs.get(14), vs.get(14),
						u + dz + dx,
						v + dz + dy03,
						u + dz,
						v + dz + dy1); // front-left
				setQuard(i++, vs.get(14), vs.get(12), vs.get(15), vs.get(14),
						u + dz + dx,
						v + dz + dy03,
						u + dz,
						v + dz + dy1); // front-right
				setQuard(i++, vs.get(16), vs.get(7), vs.get(6), vs.get(16),
						u + 2 * dz + dx, v + dz + dy1,
						u + 2 * (dz + dx), v + dz + dy0,
						u + 2 * dz + dx, v + dz + dy0,
						u + 2 * dz + dx, v + dz + dy1); // back-left
				setQuard(i++, vs.get(7), vs.get(16), vs.get(17), vs.get(17),
						u + 2 * (dz + dx), v + dz + dy0,
						u + 2 * dz + dx, v + dz + dy1,
						u + 2 * (dz + dx), v + dz + dy1,
						u + 2 * (dz + dx), v + dz + dy1); // back-right
				setQuard(i++, vs.get(6), vs.get(12), vs.get(15), vs.get(15),
						u + 2 * dz + dx, v + dz + dy0,
						u + dz + dx, v + dz + dy03,
						u + dz + dx, v + dz + dy1,
						u + dz + dx, v + dz + dy1); // left-front
				setQuard(i++, vs.get(16), vs.get(6), vs.get(15), vs.get(16),
						u + 2 * dz + dx,v + dz + dy1,
						u + 2 * dz + dx, v + dz + dy0,
						u + dz + dx, v + dz + dy1,
						u + 2 * dz + dx,v + dz + dy1); // left-back
				setQuard(i++, vs.get(13), vs.get(17), vs.get(14), vs.get(14), // outside 1
						u + dz, v + dz + dy03,
						u, v + dz + dy1,
						u + dz, v + dz + dy1,
						u + dz, v + dz + dy1);
				setQuard(i++, vs.get(13), vs.get(7), vs.get(17), vs.get(17), // outside 0
						u + dz, v + dz + dy03,
						u, v + dz + dy0,
						u, v + dz + dy1,
						u, v + dz + dy1); // right-front
			}
			// level #5
			Vec2f yz18 = rotate(cr, new Vec2f(ye2, tvs[4].y), cos2, sin2);
			Vec2f yz19 = rotate(cr, new Vec2f(ye2, tvs[5].y), cos2, sin2);
			Vec2f yz20 = rotate(cr, new Vec2f(ye2, tvs[6].y), cos2, sin2);
			Vec2f yz21 = rotate(cr, new Vec2f(ye2, tvs[7].y), cos2, sin2);
			vs.put(18, new PositionTextureVertex(tvs[4].x, yz18.x, yz18.y, 0.0F, 0.0F));
			vs.put(19, new PositionTextureVertex(tvs[5].x, yz19.x, yz19.y, 0.0F, 0.0F));
			vs.put(20, new PositionTextureVertex(tvs[6].x, yz20.x, yz20.y, 0.0F, 0.0F));
			vs.put(21, new PositionTextureVertex(tvs[7].x, yz21.x, yz21.y, 0.0F, 0.0F));
		}
		setQuard(i++, vs.get(15), vs.get(14), vs.get(18), vs.get(19), u + dz, v + dz + dy1, u + dz + dx, v + dz + dy2); // front
		setQuard(i++, vs.get(17), vs.get(16), vs.get(20), vs.get(21), u + 2 * dz + dx, v + dz + dy1, u + 2 * (dz + dx), v + dz + dy2); // back
		setQuard(i++, vs.get(16), vs.get(15), vs.get(19), vs.get(20), u + dz + dx, v + dz + dy1, u + 2 * dz + dx, v + dz + dy2);// inside
		setQuard(i++, vs.get(14), vs.get(17), vs.get(21), vs.get(18), u, v + dz + dy1, u + dz, v + dz + dy2);// outside
		setQuard(i, vs.get(19), vs.get(18), vs.get(21), vs.get(20), u + dz + dx, v, u + 2 * dz + dx, v + dz); // down
		draw(scale);
	}

	private void draw(float scale) {
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder br = tess.getBuffer();
		for (TexturedQuad tq : quads.values()) {
			if (tq == null) { continue; }
			tq.draw(br, scale);
		}
	}

	private Vec2f rotate(Vec2f center, Vec2f vertex, double cos, double sin) {
		float y = (float) (center.y + (vertex.y - center.y) * cos - (vertex.x - center.x) * sin);
		float x = (float) (center.x + (vertex.y - center.y) * sin + (vertex.x - center.x) * cos);
		return new Vec2f(x, y);
	}

	private void setQuard(int i, PositionTextureVertex ptv0, PositionTextureVertex ptv1, PositionTextureVertex ptv2, PositionTextureVertex ptv3, float u1, float v1, float u2, float v2) {
		quads.put(i, new TexturedQuad(new PositionTextureVertex[] { ptv0, ptv1, ptv2, ptv3}));
		quads.get(i).vertexPositions[0] = ptv0.setTexturePosition(u2 / textureWidth, v1 / textureHeight);
		quads.get(i).vertexPositions[1] = ptv1.setTexturePosition(u1 / textureWidth, v1 / textureHeight);
		quads.get(i).vertexPositions[2] = ptv2.setTexturePosition(u1 / textureWidth, v2 / textureHeight);
		quads.get(i).vertexPositions[3] = ptv3.setTexturePosition(u2 / textureWidth, v2 / textureHeight);
	}

	private void setQuard(int i, PositionTextureVertex ptv0, PositionTextureVertex ptv1, PositionTextureVertex ptv2, PositionTextureVertex ptv3, float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
		quads.put(i, new TexturedQuad(new PositionTextureVertex[] { ptv0, ptv1, ptv2, ptv3}));
		quads.get(i).vertexPositions[0] = ptv0.setTexturePosition(u0 / textureWidth, v0 / textureHeight);
		quads.get(i).vertexPositions[1] = ptv1.setTexturePosition(u1 / textureWidth, v1 / textureHeight);
		quads.get(i).vertexPositions[2] = ptv2.setTexturePosition(u2 / textureWidth, v2 / textureHeight);
		quads.get(i).vertexPositions[3] = ptv3.setTexturePosition(u3 / textureWidth, v3 / textureHeight);
	}

	public void setIsNormal(boolean bo) { isNormal = bo; }

	public void copyModelAngles(ModelRendererAlt source) {
		rotateAngleX = source.rotateAngleX;
		rotateAngleY = source.rotateAngleY;
		rotateAngleZ = source.rotateAngleZ;
		rotationPointX = source.rotationPointX;
		rotationPointY = source.rotationPointY;
		rotationPointZ = source.rotationPointZ;
		rotateAngleX1 = source.rotateAngleX1;
		rotateAngleY1 = source.rotateAngleY1;
		scaleX = source.scaleX;
		scaleY = source.scaleY;
		scaleZ = source.scaleZ;
		offsetAnimX = source.offsetAnimX;
		offsetAnimY = source.offsetAnimY;
		offsetAnimZ = source.offsetAnimZ;
	}

	public void checkBacklightColor(float red, float green, float blue) {
		r = red;
		g = green;
		b = blue;
		al = 1.0f;
		if (ModelNpcAlt.editAnimDataSelect.isNPC) {
			if (ModelNpcAlt.editAnimDataSelect.part == part.patterns) {
				r = ModelNpcAlt.editAnimDataSelect.red;
				g = ModelNpcAlt.editAnimDataSelect.green;
				b = ModelNpcAlt.editAnimDataSelect.blue;
			} else {
				al = ModelNpcAlt.editAnimDataSelect.alpha;
			}
		}
		if (childModels != null) {
			for (ModelRenderer modelRenderer : childModels) {
				if (modelRenderer instanceof ModelRendererAlt) {
					((ModelRendererAlt) modelRenderer).checkBacklightColor(r, g, b);
				}
			}
		}
	}

	public void clearOBJ() {
		if (displayOBJListUp != null) { displayOBJListUp = null; }
		if (displayOBJListDown != null) { displayOBJListDown = null; }
	}

	public void setOBJModel(ItemStack stack, EnumParts part) {
		if (stack == null || !(stack.getItem() instanceof CustomArmor)) {
			displayOBJListUp = null;
			displayOBJListDown = null;
			return;
		}
		CustomArmor armor = (CustomArmor) stack.getItem();
		Map<String, ResourceLocation> map = new HashMap<>();
		ResourceLocation mainTexture = ModelBuffer.getMainOBJTexture(armor.objModel);
		if (mainTexture != null && stack.hasTagCompound() && stack.getTagCompound() != null && stack.getTagCompound().hasKey("OBJTexture")) {
			map.put(mainTexture.toString(), new ResourceLocation(stack.getTagCompound().getString("OBJTexture")));
		} else if (textureLocation != null) {
			map.put("All", textureLocation);
		}
		if ((this.part == EnumParts.LEG_RIGHT && part == EnumParts.FEET_RIGHT) ||
				(this.part == EnumParts.LEG_LEFT && part == EnumParts.FEET_LEFT)) {
			displayOBJListDown = ModelBuffer.getParameterizedModel(armor.objModel, armor.getMeshNames(part), map, false, 0, true);
		}
		else {
			displayOBJListUp = ModelBuffer.getParameterizedModel(armor.objModel, armor.getMeshNames(part != null ? part : this.part), map, false, 0, true);
			switch(this.part) {
				case ARM_RIGHT: {
					displayOBJListDown = ModelBuffer.getParameterizedModel(armor.objModel, armor.getMeshNames(EnumParts.WRIST_RIGHT), map, false, 0, true);
					break;
				}
				case ARM_LEFT: {
					displayOBJListDown = ModelBuffer.getParameterizedModel(armor.objModel, armor.getMeshNames(EnumParts.WRIST_LEFT), map, false, 0, true);
					break;
				}
				case LEG_RIGHT: {
					displayOBJListDown = ModelBuffer.getParameterizedModel(armor.objModel, armor.getMeshNames(EnumParts.FEET_RIGHT), map, false, 0, true);
					break;
				}
				case LEG_LEFT: {
					displayOBJListDown = ModelBuffer.getParameterizedModel(armor.objModel, armor.getMeshNames(EnumParts.FEET_LEFT), map, false, 0, true);
					break;
				}
				default: { break; }
			}
		}
	}

	public boolean notOBJModel() { return displayOBJListUp == null && displayOBJListDown == null; }

	public void setBaseData(ModelPartConfig config) {
		if (config == null) {
			offsetAnimX = 0.0f;
			offsetAnimY = 0.0f;
			offsetAnimZ = 0.0f;
			scaleX = 1.0f;
			scaleY = 1.0f;
			scaleZ = 1.0f;
			return;
		}
		offsetAnimX = config.transX;
		offsetAnimY = config.transY;
		offsetAnimZ = config.transZ;
		scaleX = config.scaleX;
		scaleY = config.scaleY;
		scaleZ = config.scaleZ;
	}

	public void putAnimation(DataAnimation animation) {
		AnimationFrameConfig preFrame = animation.getPreFrame();
		if (preFrame == null || !preFrame.parts.containsKey(partId)) { return; }
		preFrame.parts.get(partId).show = showModel;

		preFrame.parts.get(partId).rotation[0] = rotateAngleX;
		preFrame.parts.get(partId).rotation[1] = rotateAngleY;
		preFrame.parts.get(partId).rotation[2] = rotateAngleZ;
		preFrame.parts.get(partId).rotation[3] = rotateAngleX1;
		preFrame.parts.get(partId).rotation[4] = rotateAngleY1;

		preFrame.parts.get(partId).offset[0] = offsetAnimX;
		preFrame.parts.get(partId).offset[1] = offsetAnimY;
		preFrame.parts.get(partId).offset[2] = offsetAnimZ;

		preFrame.parts.get(partId).scale[0] = scaleX;
		preFrame.parts.get(partId).scale[1] = scaleY;
		preFrame.parts.get(partId).scale[2] = scaleZ;
	}

	public void setAnimation(Float[] partSets) {
		if (partSets == null) { return; }
		rotateAngleX = partSets[0];
		rotateAngleY = partSets[1];
		rotateAngleZ = partSets[2];
		offsetAnimX += partSets[3];
		offsetAnimY += partSets[4];
		offsetAnimZ += partSets[5];
		scaleX *= partSets[6];
		scaleY *= partSets[7];
		scaleZ *= partSets[8];
		if (partSets[9] != 0.0f || partSets[10] != 0.0f) {
			rotateAngleX1 = partSets[9];
			rotateAngleY1 = partSets[10];
			if (isNormal) { isNormal = false; }
		}
		isAnimPart = partSets[0] != 0.0f || partSets[1] != 0.0f || partSets[2] != 0.0f || partSets[3] != 0.0f || partSets[4] != 0.0f || partSets[5] != 0.0f || partSets[6] != 1.0f || partSets[7] != 1.0f || partSets[8] != 1.0f || partSets[9] != 0.0f || partSets[10] != 0.0f;
	}

	/**
	 * @param animation = rots[ 0:rotX, 1:rotY, 2:rotZ, 3:ofsX, 4:ofsY, 5:ofsZ, 6:scX, 7:scY, 8:scZ, 9:rotX1, 10:rotY1 ]
	 */
	public void setAnimation(DataAnimation animation) {
		if (!showModel) { return; }
		showModel = animation.getAnimationPartShow(partId);
		if (!showModel) { return; }
		setAnimation(animation.getAnimationPartData(partId));
	}

	@Override
	public void setRotationPoint(float rotationPointXIn, float rotationPointYIn, float rotationPointZIn) {
		rotationPointX = rotationPointXIn;
		rotationPointY = rotationPointYIn;
		rotationPointZ = rotationPointZIn;
		baseRotationPoint[0] = rotationPointXIn;
		baseRotationPoint[1] = rotationPointYIn;
		baseRotationPoint[2] = rotationPointZIn;
	}

	public void clearRotations() {
		rotationPointX = baseRotationPoint[0];
		rotationPointY = baseRotationPoint[1];
		rotationPointZ = baseRotationPoint[2];
		rotateAngleX = 0.0f;
		rotateAngleY = 0.0f;
		rotateAngleZ = 0.0f;
		rotateAngleX1 = 0.0f;
		rotateAngleY1 = 0.0f;
		offsetAnimX = 0.0f;
		offsetAnimY = 0.0f;
		offsetAnimZ = 0.0f;
		scaleX = 1.0f;
		scaleY = 1.0f;
		scaleZ = 1.0f;
		isAnimPart = false;
		showModel = true;
		isNormal = baseNormal;
	}

	@Override
	public boolean isShowModel() { return showModel; }

	@Override
	public float[] getRotations() {
		return new float[] { rotateAngleX, rotateAngleY, rotateAngleZ, rotateAngleX1, rotateAngleY1 };
	}

	@Override
	public float[] getOffsets() {
		return new float[] { offsetAnimX, offsetAnimY, offsetAnimZ };
	}

	@Override
	public float[] getScales() {
		return new float[] { scaleX, scaleY, scaleZ };
	}

}
