package noppes.npcs.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.MarkType;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.controllers.data.MarkData.Mark;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.model.Model2DRenderer;

import java.util.HashMap;
import java.util.Map;

public class MarkRenderer {

	public static final Map<Boolean, Map<MarkType, ResourceLocation>> locations = new HashMap<>();
	static {
		locations.put(true, new HashMap<>());
		locations.put(false, new HashMap<>());
		for (MarkType mt : MarkType.values()) {
			locations.get(true).put(mt, new ResourceLocation(CustomNpcs.MODID, "models/util/" + mt.name().toLowerCase() + ".obj"));
			locations.get(false).put(mt, new ResourceLocation(CustomNpcs.MODID, "textures/marks/" + mt.name().toLowerCase() + ".png"));
		}
	}

	public static Model2DRenderer renderer = new Model2DRenderer(32, 32, 0, 0, 32, 32, locations.get(false).get(MarkType.EXCLAMATION));

	@SuppressWarnings("unchecked")
	public static void render(RenderLivingEvent.Post<?, ?> event, Mark mark) {
		int color = mark.color;
		float alpha = 1.0f;
		float size = 1.0f;
		int light = event.getPackedLight();
		int overlay = OverlayTexture.NO_OVERLAY;
		PoseStack matrixStack = event.getPoseStack();
		matrixStack.pushPose();
		if(event.getEntity() instanceof EntityNPCInterface npc) {
			if (npc.isInvisible()) {
				alpha = 0.333333f;
				color = (85 << 24) | (color & 0x00FFFFFF);
			}
			size = 0.2f * npc.display.getSize();
		}
		matrixStack.translate(0.0f, event.getEntity().getBbHeight() +  0.6f, 0.0f);
		matrixStack.mulPose(Axis.XP.rotationDegrees(180));
		matrixStack.mulPose(Axis.YP.rotationDegrees(event.getEntity().yHeadRot));
		matrixStack.scale(size, size, size);

		if (mark.isRotate()) { matrixStack.mulPose(Axis.YP.rotationDegrees((float) (event.getEntity().level().getGameTime() % 360) / 0.25f)); }
		if (mark.is3D()) {
			matrixStack.mulPose(Axis.XP.rotationDegrees(180.0f));
            ModelBuffer.render(matrixStack, event.getMultiBufferSource(), locations.get(true).get(mark.type), null, null, light, overlay, color);
		}
		else {
			matrixStack.translate(-0.5f, 0, 0);
			ResourceLocation location = locations.get(false).get(mark.type);
			float red = (float) (color >> 16 & 255) / 255.0f;
			float green = (float) (color >> 8 & 255) / 255.0f;
			float blue = (float) (color & 255) / 255.0f;

			Minecraft minecraft = Minecraft.getInstance();
			boolean notBodyVisible = !event.getEntity().isInvisible();
			boolean isGlowing = minecraft.shouldEntityAppearGlowing(event.getEntity());
			EntityRenderer<LivingEntity> eRenderer = (EntityRenderer<LivingEntity>) minecraft.getEntityRenderDispatcher().getRenderer(event.getEntity());
			RenderType renderType;
			if (notBodyVisible) { renderType = ((RenderNPCInterface<?, ?>) eRenderer).getModel().renderType(location); }
			else { renderType = isGlowing ? RenderType.outline(location) : RenderType.entityTranslucent(location); }

			renderer.render(location, matrixStack, event.getMultiBufferSource().getBuffer(renderType), light, overlay, red, green, blue, alpha);
		}
		matrixStack.popPose();
	}

}
