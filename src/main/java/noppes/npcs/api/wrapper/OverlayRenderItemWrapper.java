package noppes.npcs.api.wrapper;

import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.INbt;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.overlay.IRenderItemOverlay;

import java.util.Objects;

public class OverlayRenderItemWrapper extends OverlayComponentWrapper implements IRenderItemOverlay {

   protected ItemStack item;

   public OverlayRenderItemWrapper(int id, int x, int y, IItemStack itemIn) {
      super(id, x, y);
      item = itemIn == null ? ItemStack.EMPTY : itemIn.getMCItemStack();
   }

   @Override
   public IItemStack getItem() { return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item); }

   @Override
   public IRenderItemOverlay setItem(IItemStack itemIn) {
      item = itemIn == null ? ItemStack.EMPTY : itemIn.getMCItemStack();
      return this;
   }

   @Override
   public int getType() { return 2; }

   @Override
   public void toNbt(INbt iNbt) {
      super.toNbt(iNbt);
      iNbt.mcSetTag("item", item.serializeNBT());
   }

   @Override
   public void fromNbt(INbt iNbt) {
      super.fromNbt(iNbt);
      item = ItemStack.of(iNbt.getCompound("item").getMCNBT());
   }

}
