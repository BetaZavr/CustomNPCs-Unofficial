package noppes.npcs.client.renderer.blocks;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.model.blocks.ModelCarpentryBench;

import javax.annotation.Nullable;

public class BlockCarpentryBenchRenderer<T extends TileEntity> extends TileEntitySpecialRenderer<T> {

	public static final ResourceLocation TEXTURE = new ResourceLocation(CustomNpcs.MODID, "textures/models/carpentrybench.png");
	public static final ModelCarpentryBench model = new ModelCarpentryBench();

	@Override
	public void render(@Nullable TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		int rotation = 0;
		if (te != null && te.getPos() != BlockPos.ORIGIN) { rotation = te.getBlockMetadata() % 4; }
		GlStateManager.pushMatrix();
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.translate(x + 0.5f, y + 1.4f, z + 0.5f);
		GlStateManager.scale(0.95f, 0.95f, 0.95f);
		GlStateManager.rotate(180.0f, 0.0f, 0.0f, 1.0f);
		GlStateManager.rotate((90 * rotation), 0.0f, 1.0f, 0.0f);
		this.bindTexture(TEXTURE);
		model.render(0.0625f);
		GlStateManager.popMatrix();
	}

}
