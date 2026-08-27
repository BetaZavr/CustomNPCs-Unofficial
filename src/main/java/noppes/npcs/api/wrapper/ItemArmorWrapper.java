package noppes.npcs.api.wrapper;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.constants.ItemType;
import noppes.npcs.api.item.IItemArmor;

public class ItemArmorWrapper extends ItemStackWrapper implements IItemArmor {

   protected ArmorItem armor;

   protected ItemArmorWrapper(ItemStack item) {
      super(item);
      this.armor = (ArmorItem)item.getItem();
   }

   @Override
   public int getType() { return ItemType.ARMOR.get(); }

   @Override
   public int getArmorSlot() { return this.armor.getEquipmentSlot().getIndex(); }

   @Override
   public String getArmorMaterial() { return this.armor.getMaterial().getName(); }

}
