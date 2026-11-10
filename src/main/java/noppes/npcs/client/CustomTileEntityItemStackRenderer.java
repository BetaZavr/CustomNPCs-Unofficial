package noppes.npcs.client;

import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import noppes.npcs.blocks.custom.CustomBlockPortal;
import noppes.npcs.blocks.tiles.TileMailbox;
import noppes.npcs.client.renderer.items.BlockCustomPortalItemRenderer;
import noppes.npcs.items.ItemNpcBlock;

import javax.annotation.Nonnull;

public class CustomTileEntityItemStackRenderer extends TileEntityItemStackRenderer {

    public static final TileEntityItemStackRenderer itemRenderer = new CustomTileEntityItemStackRenderer();
    public static final TileEntityItemStackRenderer itemPortalRenderer = new BlockCustomPortalItemRenderer();

    private final HashMap<Block, TileEntity> data = new HashMap<>();

    @Override
    public void renderByItem(@Nonnull ItemStack stack, float partialTicks) {
        if (stack.getItem() instanceof ItemNpcBlock) {
            ItemNpcBlock item = (ItemNpcBlock) stack.getItem();
            Block block = item.getBlock();
            if (block instanceof CustomBlockPortal) { return; }
            TileEntity tile = data.get(block);
            if (tile == null && block instanceof ITileEntityProvider) {
                int meta = block.getMetaFromState(block.getDefaultState());
                tile = ((ITileEntityProvider) block).createNewTileEntity(Minecraft.getMinecraft().world, meta);
                if (tile != null) { data.put(block, tile); }
            }
            if (tile instanceof TileMailbox) { ((TileMailbox) tile).setModel(stack.getItemDamage()); }
            if (tile != null) { TileEntityRendererDispatcher.instance.render(tile, 0.0D, 0.0D, 0.0D, partialTicks); }
        }
    }

}