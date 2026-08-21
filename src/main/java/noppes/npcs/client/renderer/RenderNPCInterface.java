package noppes.npcs.client.renderer;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.File;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.mojang.blaze3d.vertex.IPoseStackMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcRarityTitleGet;
import noppes.npcs.shared.client.util.ImageDownloadAlt;
import noppes.npcs.shared.client.util.ResourceDownloader;
import noppes.npcs.shared.common.util.LogWriter;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

public class RenderNPCInterface<T extends EntityNPCInterface, M extends EntityModel<T>> extends LivingEntityRenderer<T, M> {

	public static EntityNPCInterface currentNpc;

	public RenderNPCInterface(Context manager, M model, float shadowRadius) {
		super(manager, model, shadowRadius);
	}

	@Override
	public void renderNameTag(@Nonnull T npc, @Nonnull Component text, @Nonnull PoseStack matrixStack, @Nonnull MultiBufferSource buffer, int light) {
		if (shouldShowName(npc)) {
			double d0 = entityRenderDispatcher.distanceToSqr(npc);
			if (!(d0 > 512.0D)) {
				matrixStack.pushPose();
				Vec3 renderOffset = getRenderOffset(npc, 0.0F);
				matrixStack.translate(-renderOffset.x(), -renderOffset.y(), -renderOffset.z());
				if (npc.messages != null) {
					float height = npc.baseSize.height / 5.0F * npc.display.getSize();
					float offset = npc.getBbHeight() * (1.2F + (!npc.display.showName() ? 0.0F : (npc.display.getTitle().isEmpty() ? 0.15F : 0.25F)));
					matrixStack.translate(0.0F, offset, 0.0F);
					npc.messages.renderMessages(matrixStack, buffer, 0.666667F * height, npc.isInRange(entityRenderDispatcher.camera.getEntity(), 4.0D), light, false);
					matrixStack.translate(0.0F, -offset, 0.0F);
				}
				if (npc.display.showName()) {
					renderLivingLabel(npc, matrixStack, buffer, light);
				}
				matrixStack.popPose();
			}
		}
	}

	protected void renderLivingLabel(T npc, PoseStack matrixStack, MultiBufferSource buffer, int light) {
		float scale = npc.baseSize.height / 5.0F * npc.display.getSize();
		float height = npc.getBbHeight() - 0.06F * scale;
		matrixStack.pushPose();
		Font font = getFont();
		float f2 = 0.01666667F * scale;
		matrixStack.translate(0.0F, height, 0.0F);
		matrixStack.mulPose(entityRenderDispatcher.cameraOrientation());
		int color = npc.getFaction().color;
		float f1 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
		if (npc.isInvisible()) {
			color = (42 << 24) | (color & 0x00FFFFFF);
			f1 /= 2.25f;
		}
		matrixStack.translate(0.0F, scale / 6.5F * 2.0F, 0.0F);
		int backgroundAlpha = (int) (f1 * 255.0F) << 24;
		matrixStack.scale(-f2, -f2, f2);
		Matrix4f matrix4f = matrixStack.last().pose();
		float y = 0.0F;
		boolean nearby = npc.isInRange(entityRenderDispatcher.camera.getEntity(), 8.0D);
		boolean showLR = CustomNpcs.ShowLR;
		if (showLR) { Packets.sendServerDelayed(new SPacketNpcRarityTitleGet(npc.getId()), npc, 5000); }
		String title = npc.display.getTitle();
		String rarityTitle = npc.stats.getRarityTitle();
		if ((!title.isEmpty() || (showLR && !rarityTitle.isEmpty())) && nearby) {
			Component component;
			float f3 = 0.6F;
			matrixStack.translate(0.0F, 4.0F, 0.0F);
			matrixStack.scale(f3, f3, f3);
			if (!title.isEmpty()) {
				component = Component.literal("<").append(Component.translatable(title)).append(">");
				font.drawInBatch(component, (float)(-font.width(component) / 2), 0.0F, color, false, matrix4f, buffer, DisplayMode.NORMAL, backgroundAlpha, light);
			}
			if (showLR && !rarityTitle.isEmpty()) {
				component = Component.translatable(rarityTitle);
				font.drawInBatch(component, (float)(-font.width(component) / 2), -27.0F, color, false, matrix4f, buffer, DisplayMode.NORMAL, backgroundAlpha, light);
			}
			matrixStack.scale(1.0F / f3, 1.0F / f3, 1.0F / f3);
			y = -9.65F;
		}
		Component name = npc.getName();
		font.drawInBatch(name, (float)(-font.width(name) / 2), y, color, false, matrix4f, buffer, DisplayMode.NORMAL, backgroundAlpha, light);
		if (nearby) {
			font.drawInBatch(name, (float)(-font.width(name) / 2), y, color, false, matrix4f, buffer, DisplayMode.NORMAL, 0, light);
		}
		matrixStack.popPose();
	}

	protected void renderColor(EntityNPCInterface npc) {
		if (npc.hurtTime <= 0 && npc.deathTime <= 0) {
			float red = (float)(npc.display.getTint() >> 16 & 255) / 255.0F;
			float green = (float)(npc.display.getTint() >> 8 & 255) / 255.0F;
			float blue = (float)(npc.display.getTint() & 255) / 255.0F;
			RenderSystem.setShaderColor(red, green, blue, 1.0F);
		}
	}

	@Override
	protected void setupRotations(T npc, @Nonnull PoseStack matrixScale, float f, float f1, float f2) {
		if (npc.isAlive()) {
			if (npc.isSleeping()) {
				matrixScale.mulPose(Axis.YP.rotationDegrees((float)npc.ais.orientation));
				matrixScale.mulPose(Axis.ZP.rotationDegrees(getFlipDegrees(npc)));
				matrixScale.mulPose(Axis.YP.rotationDegrees(270.0F));
				return;
			}
			else if (npc.currentAnimation == 7) {
				matrixScale.mulPose(Axis.YP.rotationDegrees(270.0F - f1));
				float scale = npc.display.getSize() / 5.0F;
				matrixScale.translate(-scale + ((EntityCustomNpc)npc).modelData.getLegsY() * scale, 0.14F, 0.0F);
				matrixScale.mulPose(Axis.ZP.rotationDegrees(270.0F));
				matrixScale.mulPose(Axis.YP.rotationDegrees(270.0F));
				return;
			}
		}
		super.setupRotations(npc, matrixScale, f, f1, f2);
	}

	@Override
	protected void scale(@Nonnull T npc, PoseStack matrixScale, float f) {
		renderColor(npc);
		float size = npc.display.getSize();
		matrixScale.scale(npc.scaleX / 5.0F * size, npc.scaleY / 5.0F * size, npc.scaleZ / 5.0F * size);
	}

	@Override
	public void render(T npc, float entityYaw, float partialTicks, @Nonnull PoseStack matrixStack, @Nonnull MultiBufferSource buffer, int packedLight) {
		if (npc.isKilled()) { shadowRadius = 0.0F; }
		if (!npc.isKilled() || !npc.stats.hideKilledBody || npc.deathTime <= 20) {
			Vec3 renderOffset = getRenderOffset(npc, 0.0F);
			matrixStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);
			//if ((npc.display.getBossbar() == 1 || npc.display.getBossbar() == 2 && npc.isAttacking()) && !npc.isKilled() && npc.deathTime <= 20 && npc.canNpcSee(Minecraft.getInstance().player)) { }
			if (npc.ais.getStandingType() == 3 && !npc.isWalking() && !npc.isInteracting()) {
				npc.yBodyRotO = npc.yBodyRot = (float) npc.ais.orientation;
			}

			shadowRadius = npc.getBbWidth() * 0.8F * npc.display.getShadowSize();
			int stackSize = ((IPoseStackMixin) matrixStack).getPoseStack().size();
			try {
				currentNpc = npc;
				super.render(npc, entityYaw, partialTicks, matrixStack, buffer, packedLight);
			}
			catch (Throwable var15) {
				while(((IPoseStackMixin) matrixStack).getPoseStack().size() > stackSize) { matrixStack.popPose(); }
				LogWriter.except(var15);
			}
			finally { currentNpc = null; }
		}
	}

	@Override
	public @Nonnull Vec3 getRenderOffset(T npc, float partialTicks) {
		float xOffset = 0.0F;
		float yOffset = npc.currentAnimation == 0 ? npc.ais.bodyOffsetY / 10.0F - 0.5F : 0.0F;
		float zOffset = 0.0F;
		if (npc.isAlive()) {
			if (npc.isSleeping()) {
				float orientationRad = (float) Math.toRadians(npc.ais.orientation);
				xOffset = (float)(Math.cos(orientationRad) * 0.5F);
				zOffset = (float)(-Math.sin(orientationRad) * 0.5F);
				yOffset += 0.0575F;
			}
			else if (npc.currentAnimation != 1 && !npc.isPassenger()) {
				if (npc.isCrouching()) {
					yOffset = (float)((double) yOffset - 0.125D);
				}
			}
			else if (npc instanceof EntityCustomNpc cNpc) {
				yOffset -= 0.3F - cNpc.modelData.getLegsY() * 0.8F;
			}
		}
		return new Vec3(xOffset * (npc.display.getSize() / 5.0F),
				yOffset * (npc.display.getSize() / 5.0F),
				zOffset * (npc.display.getSize() / 5.0F));
	}

	@Override
	protected float getBob(T npc, float limbSwingAmount) {
		return !npc.isKilled() && npc.display.getHasLivingAnimation() ? super.getBob(npc, limbSwingAmount) : 0.0F;
	}

	@Override
	public @Nonnull ResourceLocation getTextureLocation(T npc) {
		if (npc.textureLocation == null) {
			if (npc.display.skinType == 0) {
				npc.textureLocation = ResourceLocation.tryParse(npc.display.getSkinTexture());
			} else {
				if (npc.display.skinType == 1 && npc.display.playerProfile != null) {
					Minecraft minecraft = Minecraft.getInstance();
					Map<Type, MinecraftProfileTexture> map = minecraft.getSkinManager().getInsecureSkinInformation(npc.display.playerProfile);
					if (map.containsKey(Type.SKIN)) {
						npc.textureLocation = minecraft.getSkinManager().registerTexture(map.get(Type.SKIN), Type.SKIN);
					} else {
						npc.textureLocation = DefaultPlayerSkin.getDefaultSkin(UUIDUtil.getOrCreatePlayerUUID(npc.display.playerProfile));
					}
				} else if (npc.display.skinType == 2 && !npc.display.getSkinUrl().isEmpty()) {
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
		return npc.textureLocation == null ? DefaultPlayerSkin.getDefaultSkin() : npc.textureLocation;
	}

	private void loadSkin(File file, ResourceLocation resource, String url, boolean fix64) {
		TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
		SimpleTexture empty = new SimpleTexture(resource);
		AbstractTexture object = texturemanager.getTexture(resource, empty);
		if (object == empty) {
			ResourceDownloader.load(new ImageDownloadAlt(file, url, resource, DefaultPlayerSkin.getDefaultSkin(), fix64, () -> {}));
		}
	}

}
