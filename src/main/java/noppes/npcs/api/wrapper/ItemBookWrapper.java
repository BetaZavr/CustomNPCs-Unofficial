package noppes.npcs.api.wrapper;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.constants.ItemType;
import noppes.npcs.api.item.IItemBook;

public class ItemBookWrapper extends ItemStackWrapper implements IItemBook {

   protected ItemBookWrapper(ItemStack item) { super(item); }

   @Override
   public String getTitle() { return getTag().getString("title"); }

   @Override
   public void setTitle(String title) { getTag().putString("title", title); }

   @Override
   public String getAuthor() { return getTag().getString("author"); }

   @Override
   public void setAuthor(String author) { getTag().putString("author", author); }

   @Override
   public String[] getText() {
      List<String> list = new ArrayList<>();
      ListTag pages = getTag().getList("pages", 8);
      for(int i = 0; i < pages.size(); ++i) {
         list.add(pages.getString(i));
      }
      return list.toArray(new String[0]);
   }

   @Override
   public void setText(String... pages) {
      ListTag list = new ListTag();
      if (pages != null) {
         for (String page : pages) { list.add(StringTag.valueOf(page)); }
      }
      getTag().put("pages", list);
   }

   @Override
   public boolean isBook() { return true; }

   @Override
   public int getType() { return ItemType.BOOK.get(); }

   private CompoundTag getTag() {
      CompoundTag comp = item.getTag();
      if (comp == null) { item.setTag(comp = new CompoundTag()); }
      return comp;
   }

}
