package noppes.npcs.api.item;

import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.INbt;
import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IMob;
import noppes.npcs.api.entity.data.IData;

@SuppressWarnings("unused")
public interface IItemStack {

   int getStackSize();

   void setStackSize(@ParamName("size") int size);

   int getMaxStackSize();

   boolean isDamageable();

   int getDamage();

   void setDamage(@ParamName("value") int value);

   int getMaxDamage();

   double getAttackDamage();

   void damageItem(@ParamName("damage") int damage, @ParamName("living") IMob<?> living);

   void addEnchantment(@ParamName("id") String id, @ParamName("strenght") int strenght);

   boolean isEnchanted();

   boolean hasEnchant(@ParamName("id") String id);

   boolean removeEnchant(@ParamName("id") String id);

   @SuppressWarnings("all")
   boolean isBlock();

   boolean isWearable();

   boolean hasCustomName();

   void setCustomName(@ParamName("name") String name);

   String getDisplayName();

   String getItemName();

   String getName();

   @SuppressWarnings("all")
   /** @deprecated */
   boolean isBook();

   IItemStack copy();

   ItemStack getMCItemStack();

   INbt getNbt();

   boolean hasNbt();

   void removeNbt();

   INbt getItemNbt();

   boolean isEmpty();

   int getType();

   String[] getLore();

   void setLore(@ParamName("lore") String[] lore);

   @SuppressWarnings("all")
   /** @deprecated */
   void setAttribute(@ParamName("name") String name, @ParamName("value") double value);

   void setAttribute(@ParamName("name") String name, @ParamName("value") double value, @ParamName("slot") int slot);

   double getAttribute(@ParamName("name") String name);

   boolean hasAttribute(@ParamName("name") String name);

   IData getTempdata();

   IData getStoreddata();

   int getFoodLevel();

   boolean compare(@ParamName("item") IItemStack item, @ParamName("ignoreNBT") boolean ignoreNBT);

   // New from Unofficial (BetaZavr)
   IEntity<?> getOwner();

   void setOwner(@ParamName("entity") IEntity<?> entity);

}
