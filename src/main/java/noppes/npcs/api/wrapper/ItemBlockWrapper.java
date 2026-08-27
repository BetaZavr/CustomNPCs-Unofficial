package noppes.npcs.api.wrapper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.api.constants.ItemType;
import noppes.npcs.api.item.IItemBlock;

public class ItemBlockWrapper extends ItemStackWrapper implements IItemBlock {

   protected String blockName;

   protected ItemBlockWrapper(ItemStack item) {
      super(item);
      Block b = Block.byItem(item.getItem());
      ResourceLocation registryName = ForgeRegistries.BLOCKS.getKey(b);
      if (registryName == null) { registryName = new ResourceLocation("minecraft", "air"); }
      this.blockName = registryName.toString();
   }

   @Override
   public int getType() { return ItemType.BLOCK.get(); }

   @Override
   public String getBlockName() { return blockName; }

}
