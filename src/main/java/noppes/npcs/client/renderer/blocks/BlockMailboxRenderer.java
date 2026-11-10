package noppes.npcs.client.renderer.blocks;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.CustomNpcs;
import noppes.npcs.blocks.tiles.TileMailbox;
import noppes.npcs.client.model.blocks.ModelMailboxUS;
import noppes.npcs.client.model.blocks.ModelMailboxWow;

import javax.annotation.Nonnull;

public class BlockMailboxRenderer<T extends TileMailbox> extends TileEntitySpecialRenderer<T> {

	public static final ModelMailboxUS model = new ModelMailboxUS();
	public static final ModelMailboxWow model2 = new ModelMailboxWow();
	public static final ResourceLocation text1 = new ResourceLocation(CustomNpcs.MODID, "textures/models/mailbox1.png");
	public static final ResourceLocation text2 = new ResourceLocation(CustomNpcs.MODID, "textures/models/mailbox2.png");
	public static final ResourceLocation text3 = new ResourceLocation(CustomNpcs.MODID, "textures/models/mailbox3.png");

	@Override
	public void render(@Nonnull T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		float meta = 0;
		int type = te.getModel();
		if (te.getPos() != BlockPos.ORIGIN) { meta = (te.getBlockMetadata() | 0x4); }
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5f, y + 1.5f, z + 0.5f);
		GlStateManager.rotate(180.0f, 0.0f, 0.0f, 1.0f);
		GlStateManager.rotate(90.0f * meta, 0.0f, 1.0f, 0.0f);
		switch (type) {
			case 1: {
				bindTexture(BlockMailboxRenderer.text2);
				model2.render(0.0625f);
				break;
			}
			case 2: {
				bindTexture(BlockMailboxRenderer.text3);
				model2.render(0.0625f);
				break;
			}
			default: {
				bindTexture(BlockMailboxRenderer.text1);
				model.render(0.0625f);
				break;
			}
		}
		GlStateManager.popMatrix();
	}

}
