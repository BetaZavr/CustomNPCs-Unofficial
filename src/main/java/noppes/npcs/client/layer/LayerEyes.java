package noppes.npcs.client.layer;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.items.ItemNpcWand;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.animation.GuiNpcEmotion;
import noppes.npcs.client.model.ModelNpcAlt;
import noppes.npcs.client.model.animation.EmotionConfig;
import noppes.npcs.client.model.animation.EmotionFrame;
import noppes.npcs.constants.EnumParts;

public class LayerEyes<T extends EntityLivingBase>
extends LayerInterface<T> {

	private float alpha;
	private final EyeRenderData leftData = new EyeRenderData(true);
	private final EyeRenderData rightData = new EyeRenderData(false);
	private final BrowsRenderData browsData = new BrowsRenderData();

    public LayerEyes(RenderLiving<?> render) {
		super(render);
		alpha = 1.0f;
	}

	private void drawBrows(int selectType, int hoverColor) {
		// skin close
		if (browsData.showLeft) {
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.translate(browsData.offsetEyeLeftX, browsData.offsetEyeLeftY, 4.01f);
			GlStateManager.scale(browsData.scaleEyeLeftX, browsData.scaleEyeLeftY, 1.0f);
			drawRect(0, 0, 2.0, (playerdata.eyes.type != 0) ? 1.7f : 1, playerdata.eyes.skinColor, 0.0f);
			GlStateManager.popMatrix();
		}
		if (browsData.showRight) {
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.translate(browsData.offsetEyeRightX, browsData.offsetEyeRightY, 4.01f);
			GlStateManager.scale(browsData.scaleEyeRightX, browsData.scaleEyeRightY, 1.0f);
			drawRect(0, 0, -2.0, (playerdata.eyes.type != 0) ? 1.7f : 1, playerdata.eyes.skinColor, 0.0f);
			GlStateManager.popMatrix();
		}

		// skin blink
		if (browsData.isBlink) {
			if (browsData.showLeft) {
				GlStateManager.pushMatrix();
				GlStateManager.enableBlend();
				GlStateManager.translate(browsData.offsetEyeRightX, browsData.offsetEyeRightY, 4.02f);
				GlStateManager.scale(browsData.scaleEyeRightX, browsData.scaleEyeRightY, 1.0f);
				drawRect(0, 0, -2.0, browsData.leftBlinkY, playerdata.eyes.skinColor, 0.0f);
				GlStateManager.popMatrix();
			}
			if (browsData.showRight) {
				GlStateManager.pushMatrix();
				GlStateManager.enableBlend();
				GlStateManager.translate(browsData.offsetEyeLeftX, browsData.offsetEyeLeftY, 4.02f);
				GlStateManager.scale(browsData.scaleEyeLeftX, browsData.scaleEyeLeftY, 1.0f);
				drawRect(0, 0, 2.0, browsData.rightBlinkY, playerdata.eyes.skinColor, 0.0f);
				GlStateManager.popMatrix();
			}
		}

		// brow
		if (playerdata.eyes.browThickness == 0) { return; }

		// Right
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.translate(browsData.offsetBrowRightX, browsData.offsetBrowRightY, 4.01f);
		GlStateManager.scale(browsData.scaleBrowRightX, browsData.scaleBrowRightY, 1.0f);
		if (selectType == 1) {
			drawRect(0.125d, 0.125d, -2.145, -0.125 - browsData.thickness, hoverColor, 0.0D);
		}
		if (playerdata.eyes.type == 2) {
			GlStateManager.color(2.0f, 2.0f, 2.0f, 1.0f);
			GlStateManager.pushMatrix();
			GlStateManager.translate(-2.2f, -0.1f - browsData.thickness, 0.005f);
			GlStateManager.scale(0.009, browsData.scaleBrow, 1.0f);
			GlStateManager.enableBlend();
			render.bindTexture(playerdata.eyes.browRight);
			GlStateManager.color(browsData.rightColor[0], browsData.rightColor[1], browsData.rightColor[2], 1.0f);
			drawTexturedModalRect(0, 0, 0, 0, 0, 256, 256, true);
			GlStateManager.popMatrix();
		}
		else { drawRect(0, 0, -2.0, - browsData.thickness, playerdata.eyes.browColor[0], 0.0D); }
		GlStateManager.popMatrix();

		// Left
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.translate(browsData.offsetBrowLeftX, browsData.offsetBrowLeftY, 4.01f);
		GlStateManager.scale(browsData.scaleBrowLeftX, browsData.scaleBrowLeftY, 1.0f);
		if (selectType == 2) {
			drawRect(-0.125d, 0.125d, 2.145, -0.125 - browsData.thickness, hoverColor, 0.0D);
		}
		if (playerdata.eyes.type == 2) {
			GlStateManager.color(2.0f, 2.0f, 2.0f, 1.0f);
			GlStateManager.pushMatrix();
			GlStateManager.translate(-0.1f, -0.1f - browsData.thickness, 0.005f);
			GlStateManager.scale(0.009, browsData.scaleBrow, 1.0f);
			GlStateManager.enableBlend();
			render.bindTexture(playerdata.eyes.browRight);
			GlStateManager.color(browsData.leftColor[0], browsData.leftColor[1], browsData.leftColor[2], 1.0f);
			drawTexturedModalRect(0, 0, 0, 0, 0, 256, 256, false);
			GlStateManager.popMatrix();
		}
		else { drawRect(0, 0, 2.0, - browsData.thickness, playerdata.eyes.browColor[1], 0.0D); }
		GlStateManager.popMatrix();
	}

	private void drawLeft(int selectType, int hoverColor) {
		if (!leftData.isShow) { return; }
		// Eye
		GlStateManager.pushMatrix();
		GlStateManager.translate(leftData.eyePosX, leftData.eyePosY, 4.01f);
		GlStateManager.scale(leftData.eyeScaleX, leftData.eyeScaleY, 1.0f);
		if (selectType == 1) { drawRect(-0.125d, -0.125d, 2.135, 1.125 + leftData.eyeHoverY, hoverColor, 0.0D); }
		if (playerdata.eyes.type == 2) {
			GlStateManager.pushMatrix();
			GlStateManager.scale(0.007875f, 0.0067204f, 1.0f);
			GlStateManager.enableBlend();
			render.bindTexture(playerdata.eyes.eyeLeft);
			GlStateManager.color(leftData.eyeColor[0], leftData.eyeColor[1], leftData.eyeColor[2], 1.0f);
			drawTexturedModalRect(0, 0, 0, 0, 0, 256, 256, true);
			GlStateManager.popMatrix();
		}
		else { drawRect(0, 0, leftData.eyeWeight, leftData.eyeHeight, playerdata.eyes.eyeColor[0], 0.0D); }

		// Pupil
		GlStateManager.pushMatrix();
		GlStateManager.translate(leftData.pupilX, leftData.pupilY, 0.0f);
		GlStateManager.scale(leftData.pupilScaleX, leftData.pupilScaleY, 1.0f);
		if (selectType == 2) { drawRect(-0.625, -0.625, 0.625, 0.625, hoverColor, 0.0D); }
		if (playerdata.eyes.type == 2) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(0.525f, 0.375f, 0.01f);
			GlStateManager.scale(0.004f, 0.004f, 1.0f);
			render.bindTexture(playerdata.eyes.pupilLeft);
			GlStateManager.color(leftData.pupilColor[0], leftData.pupilColor[1], leftData.pupilColor[2], 1.0f);
			drawTexturedModalRect(0, 0, 0, 0, 0, 256, 256, true);
			GlStateManager.popMatrix();
		}
		else {
			drawRect(leftData.pupilLeft, leftData.pupilTop, leftData.pupilRight, leftData.pupilBottom, playerdata.eyes.pupilColor[0], 0.0D);
			if (playerdata.eyes.activeCenter) {
				drawRect(leftData.centerLeft, leftData.centerTop, leftData.centerRight, leftData.centerBottom, playerdata.eyes.centerColor, 0.0D);
			}
		}

		// Glint
		if (playerdata.eyes.glint && npc.isEntityAlive()) {
			GlStateManager.translate(0.0f, 0.0f, 0.001f);
			if (leftData.glintShow) {
				if (playerdata.eyes.type == 2) {
					GlStateManager.pushMatrix();
					GlStateManager.translate(0.65f, 0.475f, 0.01f);
					GlStateManager.translate(leftData.glintLeft, leftData.glintTop, 0.0f);
					GlStateManager.scale(0.003f, 0.003f, 1.0f);
					GlStateManager.color(1.0f, 1.0f, 1.0f, leftData.glintAlpha);
					render.bindTexture(playerdata.eyes.glintRes);
					drawTexturedModalRect(0, 0, 0, 0, 0, 256, 256, true);
					GlStateManager.popMatrix();
				}
				else {
					drawRect(leftData.glintLeft, leftData.glintTop, leftData.glintRight, leftData.glintBottom, leftData.glintColor, 0.0D);
				}
			}
		}
		GlStateManager.popMatrix(); // Pupil end

		GlStateManager.popMatrix();
	}

	private void drawRight(int selectType, int hoverColor) {
		if (!rightData.isShow) { return; }
		// Eye
		GlStateManager.pushMatrix();
		GlStateManager.translate(rightData.eyePosX, rightData.eyePosY, 4.01f);
		GlStateManager.scale(rightData.eyeScaleX, rightData.eyeScaleY, 1.0f);
		if (selectType == 1) { drawRect(-0.125d, -0.125d, 2.135, 1.125 + rightData.eyeHoverY, hoverColor, 0.0D); }
		if (playerdata.eyes.type == 2) {
			GlStateManager.pushMatrix();
			GlStateManager.scale(0.007875f, 0.0067204f, 1.0f);
			GlStateManager.enableBlend();
			render.bindTexture(playerdata.eyes.eyeRight);
			GlStateManager.color(rightData.eyeColor[0], rightData.eyeColor[1], rightData.eyeColor[2], 1.0f);
			drawTexturedModalRect(0, 0, 0, 0, 0, 256, 256, false);
			GlStateManager.popMatrix();
		}
		else { drawRect(0, 0, rightData.eyeWeight, rightData.eyeHeight, playerdata.eyes.eyeColor[1], 0.0D); }

		// Pupil
		GlStateManager.pushMatrix();
		GlStateManager.translate(rightData.pupilX, rightData.pupilY, 0.0f);
		GlStateManager.scale(rightData.pupilScaleX, rightData.pupilScaleY, 1.0f);
		if (selectType == 2) { drawRect(-0.625, -0.625, 0.625, 0.625, hoverColor, 0.0D); }
		if (playerdata.eyes.type == 2) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(0.525f, 0.375f, 0.01f);
			GlStateManager.scale(0.004f, 0.004f, 1.0f);
			render.bindTexture(playerdata.eyes.pupilRight);
			GlStateManager.color(rightData.pupilColor[0], rightData.pupilColor[1], rightData.pupilColor[2], 1.0f);
			drawTexturedModalRect(0, 0, 0, 0, 0, 256, 256, false);
			GlStateManager.popMatrix();
		}
		else {
			drawRect(rightData.pupilLeft, rightData.pupilTop, rightData.pupilRight, rightData.pupilBottom, playerdata.eyes.pupilColor[1], 0.0D);
			if (playerdata.eyes.activeCenter) {
				drawRect(rightData.centerLeft, rightData.centerTop, rightData.centerRight, rightData.centerBottom, playerdata.eyes.centerColor, 0.0D);
			}
		}

		// Glint
		if (playerdata.eyes.glint && npc.isEntityAlive()) {
			GlStateManager.translate(0.0f, 0.0f, 0.001f);
			if (rightData.glintShow) {
				if (playerdata.eyes.type == 2) {
					GlStateManager.pushMatrix();
					GlStateManager.translate(0.65f, 0.475f, 0.01f);
					GlStateManager.translate(rightData.glintLeft, rightData.glintTop, 0.0f);
					GlStateManager.scale(0.003f, 0.003f, 1.0f);
					GlStateManager.color(1.0f, 1.0f, 1.0f, rightData.glintAlpha);
					render.bindTexture(playerdata.eyes.glintRes);
					drawTexturedModalRect(0, 0, 0, 0, 0, 256, 256, false);
					GlStateManager.popMatrix();
				}
				else {
					drawRect(rightData.glintLeft, rightData.glintTop, rightData.glintRight, rightData.glintBottom, rightData.glintColor, 0.0D);
				}
			}
		}
		GlStateManager.popMatrix(); // Pupil end

		GlStateManager.popMatrix();
	}

	public void drawRect(double x, double y, double x2, double y2, int color, double z) {
		if (x < x2) {
			double j1 = x;
			x = x2;
			x2 = j1;
		}
		if (y < y2) {
			double j1 = y;
			y = y2;
			y2 = j1;
		}
		float f0 = (color >> 24 & 0xFF) / 255.0f;
		if (f0 == 0.0f) { f0 = alpha; }
		float f1 = (color >> 16 & 0xFF) / 255.0f;
		float f2 = (color >> 8 & 0xFF) / 255.0f;
		float f3 = (color & 0xFF) / 255.0f;
		GlStateManager.color(2.0f, 2.0f, 2.0f, 1.0f);
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(x, y, z).color(f1, f2, f3, f0).endVertex();
		buffer.pos(x, y2, z).color(f1, f2, f3, f0).endVertex();
		buffer.pos(x2, y2, z).color(f1, f2, f3, f0).endVertex();
		buffer.pos(x2, y, z).color(f1, f2, f3, f0).endVertex();
		Tessellator.getInstance().draw();
	}

	@Override
	public void render(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		if (!playerdata.eyes.isEnabled() || npc.display.getModel() != null || !npc.getClass().getSimpleName().equals("EntityCustomNpc") || !npc.animation.showParts.get(EnumParts.HEAD)) { return; }
		boolean isInvisible = false;
        Minecraft mc = Minecraft.getMinecraft();
		if (npc.display.getVisible() == 1) { isInvisible = npc.display.getAvailability().isAvailable(mc.player); }
		else if (npc.display.getVisible() == 2) { isInvisible = !(mc.player.getHeldItemMainhand().getItem() instanceof ItemNpcWand); }
		if (isInvisible) { alpha = 0.5f; }
		else if (npc.equals(ModelNpcAlt.editAnimDataSelect.displayNpc) && ModelNpcAlt.editAnimDataSelect.part != 0) {
			if (ModelNpcAlt.editAnimDataSelect.alpha >= 1.0f) { alpha = 1.0f; } else { alpha = 0.5f; }
		}
		else { alpha = 1.0f; }

		GlStateManager.pushMatrix();
		model.bipedHead.postRender(0.0625f);
		GlStateManager.scale(scale, scale, -scale);
		GlStateManager.translate(0.0f, (((playerdata.eyes.type != 0) ? 1 : 2) - playerdata.eyes.eyePos), 0.0f);
		GlStateManager.enableRescaleNormal(); // Normalization of vertex normals
		GlStateManager.shadeModel(GL11.GL_SMOOTH); // Smooth Shading Mode
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate( // Setting up blending functions
				GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.enableCull(); // Enable invisible edge culling
		GlStateManager.depthMask(false); // Disable recording of depth values

		mc.entityRenderer.setupFogColor(true);

		// [ offsetEyeY, offsetEyeY2, offsetPupilY, offsetPupilY2, pupilPosX, pupilPosY, offsetGlintYStart, offsetGlintYEnd ]
		float[] data;
		if (playerdata.eyes.type != 0) { data = new float[] { 0.3f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.7f, 0.15f }; }
		else { data = new float[] { 0.0f, 0.0f, -0.4f, 0.25f, 0.0f, 0.0f, 0.0f, 0.0f}; }

        Map<Integer, Float[]> emotionData = new HashMap<>();
		EmotionFrame frame = npc.animation.getCurrentEmotionFrame();
		EmotionConfig activeEmotion = npc.animation.getActiveEmotion();
		boolean isDisableMoved = npc.animation.isEmoted() && npc.animation.emotionIsDisableMoved();
		if (CustomNpcs.ShowCustomAnimation && activeEmotion != null) {
			emotionData = npc.animation.getEmotionData();
			if (frame != null && frame.isBlink()) {
				playerdata.eyes.ticks = (int) (npc.world.getTotalWorldTime() - npc.animation.getStartEmotionTime());
				if (playerdata.eyes.ticks == 0) { playerdata.eyes.blinkStart = System.currentTimeMillis(); }
			}
			else if (!activeEmotion.canBlink()) { playerdata.eyes.blinkStart = -20L; }
		}

		if (!isDisableMoved) {
			if (mc.currentScreen instanceof GuiNpcEmotion && activeEmotion != null && activeEmotion.isEdit && (activeEmotion.editFrame > -1 || activeEmotion.frames.size() == 1)) {
				EmotionFrame fr = npc.animation.getEmotionCurrentFrame();
				if (fr != null && fr.disable) {
					ScaledResolution sw = new ScaledResolution(mc);
					float x = sw.getScaledWidth() / 2.0f + 134.0f;
					float y = sw.getScaledHeight() / 2.0f + 41.0f;
					GuiNPCInterface gui = (GuiNPCInterface) mc.currentScreen;
					x -= gui.wrapper.mouseX;
					y -= gui.wrapper.mouseY;
					x = ValueUtil.correctFloat(x, -70.0f, 70.0f);
					y = ValueUtil.correctFloat(y, -70.0f, 70.0f);
					double theta = Math.atan2(y, x);
					data[4] = -0.45f * (float) Math.cos(theta);
					data[5] = -0.45f * (float) Math.sin(theta);
				}
			}
			else if (npc.lookAt != null || npc.lookPos[0] != 0 || npc.lookPos[1] != 0) {
				if (npc.lookAt != null) {
					double d0 = npc.posX - npc.lookAt.posX;
					double d1 = (npc.posY + (double) npc.getEyeHeight()) - (npc.lookAt.posY + (double) npc.lookAt.getEyeHeight());
					double d2 = npc.posZ - npc.lookAt.posZ;
					double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
					float orientation = npc.rotationYawHead;
					if (!npc.hasPath() && npc.ais.getStandingType() == 4 && npc.getAttackTarget() == null) { orientation = npc.ais.orientation; }
					float yaw = MathHelper.wrapDegrees(orientation - (float)(MathHelper.atan2(d2, d0) * (180D / Math.PI)) - 90.0F);
					float pitch = MathHelper.wrapDegrees(npc.rotationPitch + (float)(-(MathHelper.atan2(d1, d3) * (180D / Math.PI)))) / 2.0f;
					if (!npc.hasPath() && npc.ais.getStandingType() == 4) {
						if (yaw > -45.0f && yaw < 45.0f) { yaw = 0.0f; }
						if (yaw < -45.0f) { yaw += 45.0f; }
						else if (yaw > 45.0f) { yaw -= 45.0f; }
						if (pitch > -45.0f && pitch < 45.0f) { pitch = 0.0f; }
						if (pitch < -45.0f) { pitch += 45.0f; }
						else if (pitch > 45.0f) { pitch -= 45.0f; }
					}
					data[4] = ValueUtil.correctFloat(yaw, -45.0f, 45.0f) / 45.0f;
					data[5] = ValueUtil.correctFloat(pitch, -45.0f, 45.0f) / -45.0f;
				}
				else {
					// 45 -> 0.5
					data[4] = ValueUtil.correctFloat(npc.lookPos[0], -45.0f, 45.0f) / 45.0f;
					data[5] = ValueUtil.correctFloat(npc.lookPos[1], -45.0f, 45.0f) / -45.0f;
					if (npc.ais.getStandingType() == 0 || npc.ais.getStandingType() == 3) {
						float f0 = 0.25f;
						if (npc.lookPos[0] < 0.0f) { npc.lookPos[0] += f0; } else { npc.lookPos[0] -= f0; }
						if (npc.lookPos[1] < 0.0f) { npc.lookPos[1] += f0; } else { npc.lookPos[1] -= f0; }
						if (npc.lookPos[0] > -f0 && npc.lookPos[0] < f0) { npc.lookPos[0] = 0.0f; }
						if (npc.lookPos[1] > -f0 && npc.lookPos[1] < f0) { npc.lookPos[1] = 0.0f; }
					}
				}
			}
		}

		int elementType = -1;
		boolean isRight = true;
		if (mc.currentScreen instanceof GuiNpcEmotion && ((GuiNpcEmotion) mc.currentScreen).npcEmtn.equals(npc)) {
			elementType = ((GuiNpcEmotion) mc.currentScreen).elementType;
			isRight = ((GuiNpcEmotion) mc.currentScreen).isRight;
			playerdata.eyes.update(npc);
		}
		int t = (int) (npc.world.getTotalWorldTime() % 40L);
		int hoverColor = 0xFF00 | (int) Math.ceil((-12.25f * t + 255.0f) * (t > 20 ? -1.0f : 1.0f)) << 24;

		GlStateManager.disableLighting();
		float lastBrightnessX = OpenGlHelper.lastBrightnessX;
		float lastBrightnessY = OpenGlHelper.lastBrightnessY;
		int i = npc.getBrightnessForRender();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, i % 65536.0F, i / 65536.0F);

		Map<Integer, Float[]> finalEmotionData = emotionData;
		leftData.update(npc, playerdata.eyes, 2, data, finalEmotionData.get(1), finalEmotionData.get(3), isDisableMoved);
		int selectType = 0;
		if (elementType < 2 && !isRight) { selectType = elementType + 1; }
		drawLeft(selectType, hoverColor);

		rightData.update(npc, playerdata.eyes, 2, data, finalEmotionData.get(0), finalEmotionData.get(2), isDisableMoved);
		selectType = 0;
		if (elementType < 2 && isRight) { selectType = elementType + 1; }
		drawRight(selectType, hoverColor);

		browsData.update(npc, playerdata.eyes, finalEmotionData.get(4), finalEmotionData.get(5), finalEmotionData.get(0), finalEmotionData.get(1), frame);
		selectType = 0;
		if (elementType == 2) { selectType = isRight ? 1 : 2; }
		drawBrows(selectType, hoverColor);

		mc.entityRenderer.setupFogColor(false);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.disableCull();
		GlStateManager.enableLighting();
		GlStateManager.popMatrix();

		GlStateManager.enableTexture2D();
	}

	@Override
	public void rotate(float par1, float par2, float par3, float par4, float par5, float par6) { }

	@Override
	public boolean shouldCombineTextures() {
		return true;
	}

	public void drawTexturedModalRect(double x, double y, double z, int textureX, int textureY, int width, int height, boolean rev) {
		float f = 0.00390625F;
		GlStateManager.enableBlend();
		GlStateManager.enableTexture2D();
		GlStateManager.enableAlpha();
		GlStateManager.depthMask(true);
		GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
		if (rev) {
			bufferbuilder.pos(x, y + height, z).tex((textureX + width) * f, (textureY + height) * f).endVertex();
			bufferbuilder.pos(x + width, y + height, z).tex(textureX * f, (textureY + height) * f).endVertex();
			bufferbuilder.pos(x + width, y, z).tex(textureX * f, textureY * f).endVertex();
			bufferbuilder.pos(x, y, z).tex((textureX + width) * f, textureY * f).endVertex();
		} else {
			bufferbuilder.pos(x, y + height, z).tex((textureX) * f, (textureY + height) * f).endVertex();
			bufferbuilder.pos(x + width, y + height, z).tex((textureX + width) * f, (textureY + height) * f).endVertex();
			bufferbuilder.pos(x + width, y, z).tex((textureX + width) * f, textureY * f).endVertex();
			bufferbuilder.pos(x, y, z).tex(textureX * f, textureY* f).endVertex();
		}
		tessellator.draw();
		GlStateManager.disableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.disableAlpha();
		GlStateManager.depthMask(false);
	}

}
