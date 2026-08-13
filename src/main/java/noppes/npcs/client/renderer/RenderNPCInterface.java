package noppes.npcs.client.renderer;

import java.awt.Color;
import java.io.File;
import java.util.Map;

import net.minecraft.client.renderer.texture.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomItems;
import noppes.npcs.mixin.client.renderer.texture.ITextureManagerMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcRarityTitleGet;
import noppes.npcs.shared.client.util.ResourceDownloader;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.client.gui.util.GuiNpcUtil;
import noppes.npcs.client.util.ImageDownloadAlt;
import noppes.npcs.controllers.data.SkinData;
import org.lwjgl.opengl.GL11;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.client.model.part.ModelData;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.controllers.PlayerSkinController;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class RenderNPCInterface<T extends EntityNPCInterface> extends RenderLiving<T> {

	private static final DynamicTexture TEXTURE_BRIGHTNESS = new DynamicTexture(16, 16);
	public static int LastTextureTick; // ++ in ClientTickHandler.npcClientTick()

    public RenderNPCInterface(ModelBase model, float f) {
		super(Minecraft.getMinecraft().getRenderManager(), model, f);
	}

	@Override
	public ResourceLocation getEntityTexture(@Nonnull T npc) {
		return RenderNPCInterface.getNpcTexture(npc);
	}

	@Override
	protected void applyRotations(@Nonnull T npc, float handleRotation, float rotationYaw, float partialTicks) {
		if (npc.isPlayerSleeping()) {
			GlStateManager.rotate(npc.ais.orientation, 0.0f, 1.0f, 0.0f);
			GlStateManager.rotate(getDeathMaxRotation(npc), 0.0f, 0.0f, 1.0f);
			GlStateManager.rotate(270.0f, 0.0f, 1.0f, 0.0f);
		} else if (npc.currentAnimation == 7) {
			GlStateManager.rotate(270.0f - rotationYaw, 0.0f, 1.0f, 0.0f);
			float scale = npc.display.getSize() / 5.0f;
			GlStateManager.translate(-scale + ((EntityCustomNpc) npc).modelData.getLegsY() * scale, 0.14f, 0.0f);
			GlStateManager.rotate(270.0f, 0.0f, 0.0f, 1.0f);
			GlStateManager.rotate(270.0f, 0.0f, 1.0f, 0.0f);
		} else {
			if (npc.ais.getStandingType() == 4 && !npc.isAttacking() && !npc.hasPath()) {rotationYaw = npc.ais.orientation; }
			super.applyRotations(npc, handleRotation, rotationYaw, partialTicks);
		}
	}

	@Override
	public void doRender(@Nonnull T npc, double x, double y, double z, float entityYaw, float partialTicks) {
		if (npc.isInvisibleToPlayer(Minecraft.getMinecraft().player) || npc.isKilled() && npc.stats.hideKilledBody && npc.deathTime > 20) {
			return;
		}
		if (npc.ais.getStandingType() == 3 && !npc.isWalking() && !npc.isInteracting()) {
			float n = npc.ais.orientation;
			npc.renderYawOffset = n;
			npc.prevRenderYawOffset = n;
		}
		GlStateManager.enableBlendProfile(GlStateManager.Profile.PLAYER_SKIN);
		try { super.doRender(npc, x, y, z, entityYaw, partialTicks); } catch (Exception e) { LogWriter.error(e); }
		GlStateManager.disableBlendProfile(GlStateManager.Profile.PLAYER_SKIN);
	}

	@Override
	public void doRenderShadowAndFire(@Nonnull Entity entity, double par2, double par4, double par6, float par8, float par9) {
		EntityNPCInterface npc = (EntityNPCInterface) entity;
		shadowSize = npc.width / 1.25f * npc.display.shadowSize;
		if (!npc.isKilled()) {
			if (npc.display.getVisible() == 1 && npc.isInvisibleToPlayer(Minecraft.getMinecraft().player)) {
				shadowOpaque = 0.0f;
			} else if (npc.display.getVisible() == 2 && Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() != CustomItems.wand) {
				shadowOpaque = 0.3f;
			} else {
				shadowOpaque = 1.0f;
			}
			super.doRenderShadowAndFire(entity, par2, par4, par6, par8, par9);
		}
	}

	@Override
	protected float handleRotationFloat(@Nonnull T npc, float par2) {
		if (npc.isKilled() || !npc.display.getHasLivingAnimation()) { return 0.0f; }
		return super.handleRotationFloat(npc, par2);
	}

	@Override
	protected void preRenderCallback(@Nonnull T npc, float f) {
		renderColor(npc);
		int size = npc.display.getSize();
		GlStateManager.scale(npc.scaleX / 5.0f * size, npc.scaleY / 5.0f * size, npc.scaleZ / 5.0f * size);
	}

	@Override
	protected void renderLivingAt(@Nonnull T npc, double d, double d1, double d2) {
		shadowSize = npc.display.getSize() / 10.0f;
		float xOffset = 0.0f;
		float yOffset = npc.ais.bodyOffsetY / 10.0f - 0.5f;
		float zOffset = 0.0f;
		if (npc.isEntityAlive()) {
			if (npc.isPlayerSleeping()) {
				//xOffset = (float) (-Math.cos(Math.toRadians(180 - npc.ais.orientation)));
				//zOffset = (float) (-Math.sin(Math.toRadians(npc.ais.orientation)));
				//yOffset += 0.14f;
				float orientationRad = (float) Math.toRadians(npc.ais.orientation);
				xOffset = (float)(Math.cos(orientationRad) * 0.5F);
				zOffset = (float)(-Math.sin(orientationRad) * 0.5F);
				yOffset += 0.0575F;
			} else if (npc.currentAnimation == 1 || npc.isRiding()) {
				yOffset -= 0.3f - ((EntityCustomNpc) npc).modelData.getLegsY() * 0.8f; // 0.5F - ((...
			}
		}
		xOffset = xOffset / 5.0f * npc.display.getSize();
		yOffset = yOffset / 5.0f * npc.display.getSize();
		zOffset = zOffset / 5.0f * npc.display.getSize();
		super.renderLivingAt(npc, d + xOffset, d1 + yOffset, d2 + zOffset);
	}

	@Override
	protected void renderModel(@Nonnull T npc, float limbSwing, float par3, float par4, float par5, float par6, float par7) {
		boolean isTransparent = false;
		if (npc.display.getVisible() == 1) {
			isTransparent = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() != CustomItems.wand && npc.display.getAvailability().isAvailable(Minecraft.getMinecraft().player);
		} else if (npc.display.getVisible() == 2) {
			isTransparent = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() != CustomItems.wand;
		}

		// main model
		if (bindEntityTexture(npc)) {
			if (isTransparent) {
				GlStateManager.enableBlend();
				GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 0.5f);
				GlStateManager.depthMask(false);
			}
			try { mainModel.render(npc, limbSwing, par3, par4, par5, par6, par7); }
			finally {
				if (isTransparent) {
					GlStateManager.depthMask(true);
					GlStateManager.disableBlend();
					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				}
			}
		}
		// glowing
		if (!npc.display.getOverlayTexture().isEmpty()) {
			GlStateManager.depthFunc(515);
			if (npc.textureGlowLocation == null) {
				npc.textureGlowLocation = new ResourceLocation(npc.display.getOverlayTexture());
			}
			float f1 = 1.0f;

			GlStateManager.enableBlend();
			GlStateManager.blendFunc(1, 1);
			GlStateManager.disableLighting();
			if (isTransparent) {
				GlStateManager.enableBlend();
				GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 0.5f);
				GlStateManager.depthMask(false);
			}
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.pushMatrix();
			GlStateManager.scale(1.001f, 1.001f, 1.001f);
			mainModel.render(npc, limbSwing, par3, par4, par5, par6, par7);
			GlStateManager.popMatrix();
			GlStateManager.enableLighting();
			if (isTransparent) {
				GlStateManager.depthMask(true);
				GlStateManager.disableBlend();
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			}
			GlStateManager.color(1.0f, 1.0f, 1.0f, f1);
			GlStateManager.depthFunc(515);
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			GlStateManager.disableBlend();
		}
	}

	@Override
	public void renderName(@Nonnull T npc, double d, double d1, double d2) {
		if (!canRenderName(npc) || renderManager.renderViewEntity == null) {
			return;
		}
		double d3 = npc.getDistance(renderManager.renderViewEntity);
		if (d3 > 512.0) {
			return;
		}
		if (npc.messages != null) {
			float height = npc.baseHeight / 5.0f * npc.display.getSize();
			float offset = npc.height * (1.2f + (npc.display.showName() ? (npc.display.getTitle().isEmpty() ? 0.15f : 0.25f) : 0.0f));
			npc.messages.renderMessages(d, d1 + offset, d2, 0.666667f * height, npc.isInRange(renderManager.renderViewEntity, 4.0), false);
		}
		float scale = npc.baseHeight / 5.0f * npc.display.getSize();
		if (npc.display.showName()) {
			if (npc.currentAnimation == 1) {
				d1 -= 0.35f;
			}
			renderLivingLabel(npc, d, d1 + npc.height - 0.06f * scale, d2, npc.getName(), npc.display.getTitle());
			if (CustomNpcs.ShowLR) {
				Packets.sendServerDelayed(new SPacketNpcRarityTitleGet(npc.getEntityId()), npc, 5000);
				if (!npc.stats.getRarityTitle().isEmpty()) {
					renderLivingLabel(npc, d, d1 + npc.height - 0.06f * scale, d2, "", npc.stats.getRarityTitle());
				}
			}
		}
	}

	@Override
	protected boolean setBrightness(@Nonnull T npc, float partialTicks, boolean combineTextures) {
		float f = npc.getBrightness();
		int color = getColorMultiplier(npc, f, partialTicks);
		boolean hasColor = (color >> 24 & 255) > 0;
		boolean isHurt = !npc.animation.isAnimated(AnimationKind.DIES) && npc.hurtTime > 0 || npc.deathTime > 0;
		if (!hasColor && !isHurt) { return false; }
		else if (!hasColor && !combineTextures)  { return false; }
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GlStateManager.enableTexture2D();
		GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.defaultTexUnit);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PRIMARY_COLOR);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.defaultTexUnit);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
		GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GlStateManager.enableTexture2D();
		GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, OpenGlHelper.GL_INTERPOLATE);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_CONSTANT);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PREVIOUS);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE2_RGB, OpenGlHelper.GL_CONSTANT);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND2_RGB, 770);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
		brightnessBuffer.position(0);
		if (isHurt) {
			brightnessBuffer.put(1.0F);
			brightnessBuffer.put(0.0F);
			brightnessBuffer.put(0.0F);
			brightnessBuffer.put(0.3F);
		}
		else {
			brightnessBuffer.put((float) (color >> 16 & 255) / 255.0F);
			brightnessBuffer.put((float) (color >> 8 & 255) / 255.0F);
			brightnessBuffer.put((float) (color & 255) / 255.0F);
			brightnessBuffer.put(1.0F - (float) (color >> 24 & 255) / 255.0F);
		}
		brightnessBuffer.flip();
		GlStateManager.glTexEnv(8960, 8705, brightnessBuffer);
		GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
		GlStateManager.enableTexture2D();
		GlStateManager.bindTexture(TEXTURE_BRIGHTNESS.getGlTextureId());
		GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_PREVIOUS);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.lightmapTexUnit);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
		GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		return true;
	}

	@Override
	protected boolean bindEntityTexture(@Nonnull T entity) {
		ResourceLocation resourcelocation = getEntityTexture(entity);
		if (resourcelocation == null) { return false; }
		else {
			GuiNpcUtil.bindEntityTexture(renderManager.renderEngine, resourcelocation);
			return true;
		}
	}

	public static ResourceLocation getNpcTexture(EntityNPCInterface npc) {
		boolean isEmpty = npc.textureLocation == null || npc.textureLocation.getResourcePath().isEmpty();
		if (isEmpty) {
			if (npc.display.skinType == 0) {
				npc.textureLocation = new ResourceLocation(npc.display.getSkinTexture());
			} else {
				if (RenderNPCInterface.LastTextureTick < 5) { return DefaultPlayerSkin.getDefaultSkinLegacy(); }
				if (npc.display.skinType == 1) {
					if (npc.display.playerProfile == null) { npc.display.loadProfile(); }
					if (npc.display.playerProfile != null) {
						PlayerSkinController pData = PlayerSkinController.getInstance();
						SkinData data = pData.getData(npc.display.playerProfile.getId(), Type.SKIN);
						if (data.getLocation() != null) { npc.textureLocation = data.getLocation(); }
						else {
							Minecraft minecraft = Minecraft.getMinecraft();
							Map<Type, MinecraftProfileTexture> mapMC = minecraft.getSkinManager().loadSkinFromCache(npc.display.playerProfile);
							if (mapMC.containsKey(MinecraftProfileTexture.Type.SKIN)) {
								npc.textureLocation = minecraft.getSkinManager().loadSkin(mapMC.get(Type.SKIN), MinecraftProfileTexture.Type.SKIN);
							}
						}
					}
				}
				else if (npc.display.skinType == 2) {
					try {
						boolean fixSkin = npc instanceof EntityCustomNpc && ((EntityCustomNpc)npc).modelData.getEntity(npc) == null;
						File file = ResourceDownloader.getUrlFile(npc.display.getSkinUrl(), fixSkin);
						npc.textureLocation = ResourceDownloader.getUrlResourceLocation(npc.display.getSkinUrl(), fixSkin);
						loadSkin(file, npc.textureLocation, npc.display.getSkinUrl(), fixSkin);
					}
					catch (Exception e) { LogWriter.error(e); }
				}
			}
		}
		isEmpty = npc.textureLocation == null || npc.textureLocation.getResourcePath().isEmpty();
		if (isEmpty) {
			npc.textureLocation = DefaultPlayerSkin.getDefaultSkinLegacy();
			npc.display.setSkinTexture(npc.textureLocation.toString());
		}
		else if (npc.display.skinType == 1 && RenderNPCInterface.LastTextureTick % 100 == 0) {
			if (npc.display.playerProfile == null) { npc.display.loadProfile(); }
			if (npc.display.playerProfile != null) {
				PlayerSkinController pData = PlayerSkinController.getInstance();
				SkinData data = pData.getData(npc.display.playerProfile.getId(), Type.SKIN);
				if (data.getLocation() != null) { npc.textureLocation = data.getLocation(); }
			}
		}
		return npc.textureLocation;
	}

	private static void loadSkin(File file, ResourceLocation resource, String url, boolean fix64) {
		TextureManager texturemanager = Minecraft.getMinecraft().getTextureManager();
		Map<ResourceLocation, ITextureObject> mapTextureObjects = ((ITextureManagerMixin) texturemanager).getMapTextureObjects();
		if (!mapTextureObjects.containsKey(resource)) {
			ResourceDownloader.load(new ImageDownloadAlt(file, url, resource, DefaultPlayerSkin.getDefaultSkinLegacy(), fix64, () -> {}));
		}
	}

	protected void renderColor(EntityNPCInterface npc) {
		if (npc.animation.isAnimated(AnimationKind.DIES) && npc.hurtTime <= 0 && npc.deathTime <= 0) {
			float red = (npc.display.getTint() >> 16 & 0xFF) / 255.0f;
			float green = (npc.display.getTint() >> 8 & 0xFF) / 255.0f;
			float blue = (npc.display.getTint() & 0xFF) / 255.0f;
			GlStateManager.color(red, green, blue, 1.0f);
		}
	}

	protected void renderLivingLabel(EntityNPCInterface npc, double x, double y, double z, String name, String title) {
		FontRenderer fontrenderer = getFontRendererFromRenderManager();
		float f1 = npc.baseHeight / 5.0f * npc.display.getSize();
		float f2 = 0.01666667f * f1;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GL11.glNormal3f(0.0f, 1.0f, 0.0f);
		GlStateManager.rotate(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f);
		GlStateManager.rotate(renderManager.playerViewX, 1.0f, 0.0f, 0.0f);

		ModelData modeldata = ((EntityCustomNpc) npc).modelData;
		float height = (f1 + (modeldata != null
				&& (modeldata.getPartData(EnumParts.HORNS) != null || modeldata.getPartData(EnumParts.MOHAWK) != null)
						? 1.0f
						: 0.0f))
				/ 6.5f * 2.0f;
		Color c = new Color((float) (npc.getFaction().color >> 16 & 255) / 255.0F,
				(float) (npc.getFaction().color >> 8 & 255) / 255.0F, (float) (npc.getFaction().color & 255) / 255.0F,
				(float) (npc.getFaction().color >> 24 & 255) / 255.0F);

		boolean isInvisible = false;
		if (npc.display.getVisible() == 1) {
			isInvisible = npc.display.getAvailability().isAvailable(Minecraft.getMinecraft().player);
		} else if (npc.display.getVisible() == 2) {
			isInvisible = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() != CustomItems.wand;
		}

		c = new Color(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, isInvisible ? 0.3f : 1.0f);
		GlStateManager.disableLighting();
		GlStateManager.depthMask(false);
		GlStateManager.translate(0.0f, height, 0.0f);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		// Rarity
		if (name.isEmpty()) {
			float f3 = 0.01666667f * f1 * 0.6f;
			GlStateManager.translate(0.0f, -f1 / 6.5f * 0.9f, 0.0f);
			GlStateManager.scale(-f3, -f3, f3);
			fontrenderer.drawString(title, -fontrenderer.getStringWidth(title) / 2, 0, c.getRGB());
			GlStateManager.scale(1.0f / -f3, 1.0f / -f3, 1.0f / f3);
			GlStateManager.translate(0.0f, f1 / 6.5f * 0.85f, 0.0f);
		} else if (!title.isEmpty()) {
			title = "<" + title + ">";
			float f3 = 0.01666667f * f1 * 0.6f;
			GlStateManager.translate(0.0f, -f1 / 6.5f * 0.4f, 0.0f);
			GlStateManager.scale(-f3, -f3, f3);
			fontrenderer.drawString(title, -fontrenderer.getStringWidth(title) / 2, 0, c.getRGB());
			GlStateManager.scale(1.0f / -f3, 1.0f / -f3, 1.0f / f3);
			GlStateManager.translate(0.0f, f1 / 6.5f * 0.85f, 0.0f);
		}
		GlStateManager.scale(-f2, -f2, f2);
		if (npc.isInRange(renderManager.renderViewEntity, 4.0) && !name.isEmpty()) {
			GlStateManager.disableDepth();
			c = new Color(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, isInvisible ? 0.225f : 1.0f);
			fontrenderer.drawString(name, -fontrenderer.getStringWidth(name) / 2, 0, c.getRGB());
			GlStateManager.enableDepth();
		}
		GlStateManager.depthMask(true);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		if (!name.isEmpty()) {
			fontrenderer.drawString(name, -fontrenderer.getStringWidth(name) / 2, 0, c.getRGB());
		}
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		GlStateManager.popMatrix();
	}

}
