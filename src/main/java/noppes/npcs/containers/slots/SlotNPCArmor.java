package noppes.npcs.containers.slots;

import com.mojang.datafixers.util.Pair;
import moe.plushie.armourers_workshop.api.ArmourersWorkshopApi;
import moe.plushie.armourers_workshop.api.skin.ISkinDescriptor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

public class SlotNPCArmor extends Slot {

   final EquipmentSlot armorType;

   public SlotNPCArmor(Container container, int slotIndex, int x, int y, EquipmentSlot slot) {
      super(container, slotIndex, x, y);
      armorType = slot;
   }

   @Override
   public int getMaxStackSize() { return 1; }

   @Override
   @OnlyIn(Dist.CLIENT)
   public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
      return Pair.of(InventoryMenu.BLOCK_ATLAS, SlotCompanionArmor.ARMOR_SLOT_TEXTURES[armorType.getIndex()]);
   }

   @Override
   public boolean mayPlace(@Nonnull ItemStack itemstack) {
      if (itemstack.getItem() instanceof ElytraItem elytra) { return elytra.getEquipmentSlot() == armorType; }
      if (itemstack.getItem() instanceof ArmorItem armor) { return armor.getEquipmentSlot() == armorType; }
      if (itemstack.getItem() instanceof BlockItem ||
              itemstack.getItem() instanceof SwordItem ||
              itemstack.getItem() instanceof BowItem) { return armorType == EquipmentSlot.HEAD; }
      if (ArmourersWorkshopApi.isAvailable()) {
         ISkinDescriptor skinDescriptor = ArmourersWorkshopApi.getSkinDescriptor(itemstack);
         if (skinDescriptor != null) {
            return skinDescriptor.type().name().equalsIgnoreCase(armorType.getName());
         }
      }
      return false;
   }

}
