package noppes.npcs.client.renderer.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.blocks.BlockMailbox;
import noppes.npcs.blocks.tiles.TileMailbox;
import noppes.npcs.client.model.blocks.ModelMailboxUS;
import noppes.npcs.client.model.blocks.ModelMailboxWow;
import org.jetbrains.annotations.NotNull;

public class BlockMailboxRenderer<T extends TileMailbox> implements BlockEntityRenderer<T> {

	private final ModelMailboxUS model = new ModelMailboxUS();
	private final ModelMailboxWow model2 = new ModelMailboxWow();
	private static final RenderType type1 = RenderType.entityCutout(new ResourceLocation(CustomNpcs.MODID, "textures/models/mailbox1.png"));
	private static final RenderType type2 = RenderType.entityCutout(new ResourceLocation(CustomNpcs.MODID, "textures/models/mailbox2.png"));
	private static final RenderType type3 = RenderType.entityCutout(new ResourceLocation(CustomNpcs.MODID, "textures/models/mailbox3.png"));

	public BlockMailboxRenderer(Context ignoredDispatcher) { }

	public void render(T te, float partialTicks, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource buffer, int light, int overlay) {
		float meta = 0;
		int type = te.getModel();
		if (te.getBlockPos() != BlockPos.ZERO) { meta = (te.getBlockState().getValue(BlockMailbox.ROTATION) | 0x4); }
		matrixStack.pushPose();
		matrixStack.translate(0.5F, 1.5F, 0.5F);
		matrixStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
		matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f * meta));
		switch (type) {
			case 1 -> model2.renderToBuffer(matrixStack, buffer.getBuffer(type2), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
			case 2 -> model2.renderToBuffer(matrixStack, buffer.getBuffer(type3), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
			default -> model.renderToBuffer(matrixStack, buffer.getBuffer(type1), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
		}
		matrixStack.popPose();
	}

}
