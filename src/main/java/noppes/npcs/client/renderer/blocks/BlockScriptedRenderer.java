package noppes.npcs.client.renderer.blocks;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.api.ILayerBlockModel;
import noppes.npcs.api.block.ITextPlane;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.BlockWrapper;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.blocks.tiles.TileScripted.TextPlane;
import noppes.npcs.client.ClientEventHandler;
import noppes.npcs.client.TextBlockClient;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.client.renderer.obj.ParameterizedModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BlockScriptedRenderer<T extends TileScripted> extends BlockRendererInterface<T> {

	protected static final Map<String, ParameterizedModel> cache = new HashMap<>();
	private static final RandomSource random = RandomSource.create();

	public BlockScriptedRenderer(BlockEntityRendererProvider.Context dispatcher) { super(dispatcher); }

	@Override
	public void render(@Nullable T tile, float partialTicks, @Nonnull PoseStack matrixStack, @Nonnull MultiBufferSource buffer, int light, int overlay) {
		if (tile == null) { return; }
		// Default model
        if(overrideModel()){
			matrixStack.pushPose();
			matrixStack.translate(0.5f, 0.5f, 0.5f);
        	renderItem(new ItemStack(CustomBlocks.scripted), matrixStack, buffer, light, overlay);
			matrixStack.popPose();
			return;
        }
		// Custom models
		for (ILayerBlockModel layer : new ArrayList<>(tile.getLayers())) {
			IItemStack itemModel = layer.getItemModel();
			BlockWrapper blockModel = (BlockWrapper) layer.getBlockModel();
			String objModel = layer.getOBJModel();
			if (!itemModel.isEmpty() || !blockModel.isEmpty() || objModel != null) {
				matrixStack.pushPose();
				RenderSystem.enableBlend();
				matrixStack.translate(0.5f, 0.0f, 0.5f);
				// offset
				matrixStack.translate(layer.getOffset(0), layer.getOffset(1), layer.getOffset(2));
				// rotate
				if (layer.isRotate(1)) { matrixStack.mulPose(Axis.YP.rotationDegrees(((float) System.currentTimeMillis() / layer.getRotateSpeed()) % 360)); }
				else { matrixStack.mulPose(Axis.YP.rotationDegrees(layer.getRotation(1))); }
				if (layer.isRotate(0)) { matrixStack.mulPose(Axis.XP.rotationDegrees(((float) System.currentTimeMillis() / layer.getRotateSpeed()) % 360)); }
				else { matrixStack.mulPose(Axis.XP.rotationDegrees(layer.getRotation(0))); }
				if (layer.isRotate(2)) { matrixStack.mulPose(Axis.ZP.rotationDegrees(((float) System.currentTimeMillis() / layer.getRotateSpeed()) % 360)); }
				else { matrixStack.mulPose(Axis.ZP.rotationDegrees(layer.getRotation(2))); }
				// scale
				matrixStack.scale(layer.getScale(0), layer.getScale(1), layer.getScale(2));
				// model
				if (!itemModel.isEmpty()) {
					matrixStack.translate(0.0, 0.5, 0.0);
					renderItem(itemModel.getMCItemStack(), matrixStack, buffer, light, overlay);
				}
				else if (!blockModel.isEmpty()) {
					renderBlock(tile, blockModel.getState(), matrixStack, buffer, light, overlay, partialTicks);
				}
				else {
					String key = objModel + layer.getOBJVisibleMeshes() + layer.getOBJMaterialsReplase();
					if (cache.size() > 500) { cache.clear(); }
					if (!cache.containsKey(key)) {
						cache.put(key, ModelBuffer.getParameterizedModel(new ResourceLocation(key),
								layer.getOBJVisibleMeshes(),
								layer.getOBJMaterialsReplase(),
								false, 0));
					}
					ModelBuffer.render(cache.get(key), matrixStack, buffer, light, overlay);
				}
				matrixStack.popPose();
			}
		}
		// texts
		for (ITextPlane iTextPlane : new ArrayList<>(tile.getTextPlanes())) {
			if(iTextPlane instanceof TextPlane textPlane &&
					!iTextPlane.getText().isEmpty()) { drawText(matrixStack, textPlane, buffer, light, overlay); }
		}
	}
	
	private void drawText(PoseStack matrixStack, TextPlane textPlane, MultiBufferSource buffer, int light, int overlay) {
		if(textPlane.textBlock == null || textPlane.textHasChanged){
			textPlane.textBlock = new TextBlockClient(textPlane.text, 336, true, Minecraft.getInstance().player);
			textPlane.textHasChanged = false;
		}
		matrixStack.pushPose();
		matrixStack.translate(0.5f, 0.5f, 0.5f);
		matrixStack.mulPose(Axis.YP.rotationDegrees(textPlane.rotationY));
		matrixStack.mulPose(Axis.XP.rotationDegrees(textPlane.rotationX));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(textPlane.rotationZ));
		matrixStack.scale(textPlane.scale, textPlane.scale, 1);
		matrixStack.translate(textPlane.offsetX, textPlane.offsetY, textPlane.offsetZ);
        float f1 = 0.6666667F;
        float f3 = 0.0133F * f1;
		matrixStack.translate(0.0F, 0.5f, 0.01F);
		matrixStack.scale(f3, -f3, f3);
        Font font = Minecraft.getInstance().font;
        float lineOffset = 0;
        if (textPlane.textBlock.lines.size() < 14) { lineOffset = (14f - textPlane.textBlock.lines.size()) / 2; }
    	for(int i = 0; i < textPlane.textBlock.lines.size(); i++){
			Component text = textPlane.textBlock.lines.get(i);
    		font.drawInBatch(text, (float) font.width(text) / -2.0f, (int)((lineOffset + i) * (font.lineHeight - 0.3f)),
					0, false, matrixStack.last().pose(), buffer, Font.DisplayMode.NORMAL, light, overlay);
    	}
		matrixStack.popPose();
	}
	
	private void renderItem(ItemStack item, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
		Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.NONE,
				light, overlay, matrixStack, buffer, null, 0);
	}

	private void renderBlock(@Nonnull T tile, BlockState state, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay, float partialTicks) {
		Level level = tile.getLevel();
		if (level != null) {
			matrixStack.pushPose();
			ClientEventHandler.renderBlock(level, state, tile.getBlockPos(), matrixStack, buffer, light, overlay, partialTicks);
			matrixStack.popPose();
			if (random.nextInt(12) == 1) { state.getBlock().animateTick(state, level, tile.getBlockPos(), random); }
		}
	}
	
	private boolean overrideModel() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player == null ||
				player.getMainHandItem().getItem() == CustomItems.wand ||
				player.getMainHandItem().getItem() == CustomItems.scripter;
	}

}
