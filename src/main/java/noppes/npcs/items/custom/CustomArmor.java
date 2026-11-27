package noppes.npcs.items.custom;

import noppes.npcs.CustomTabs;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.mixin.item.IItemArmorMixin;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class CustomArmor extends ItemArmor implements ICustomElement {

	public static @Nonnull ArmorMaterial getMaterialArmor(@Nonnull String materialName) {
		switch (materialName.toLowerCase()) {
			case "diamond": return ArmorMaterial.DIAMOND;
			case "chain": return ArmorMaterial.CHAIN;
			case "iron": return ArmorMaterial.IRON;
			case "gold": return ArmorMaterial.GOLD;
			default: return ArmorMaterial.LEATHER;
		}
	}
	
	public static @Nonnull EntityEquipmentSlot getSlotEquipment(@Nonnull String slotName) {
		switch (slotName.toLowerCase()) {
			case "head": return EntityEquipmentSlot.HEAD;
			case "chest": return EntityEquipmentSlot.CHEST;
			case "legs": return EntityEquipmentSlot.LEGS;
			default: return EntityEquipmentSlot.FEET;
		}
	}

	protected final @Nonnull NBTTagCompound nbtData;
	protected final ItemStack repairItemStack;
	protected final int enchantability;

	public CustomArmor(@Nonnull ArmorMaterial materialIn, int renderIndexIn, @Nonnull EntityEquipmentSlot slotIn,
					   int maxStDam, int damReAmt, int enchantabilityIn, float tough, @Nonnull NBTTagCompound nbtItem) {
		super(materialIn, renderIndexIn, slotIn);
		nbtData = nbtItem;
		String name = "custom_" + getCustomName() + "_" + slotIn.name().toLowerCase();
		setRegistryName(CustomNpcs.MODID, name);
		setUnlocalizedName(name);
		setCreativeTab(CustomTabs.ITEMS);
		if (nbtItem.hasKey("IsFull3D", 1) && nbtItem.getBoolean("IsFull3D")) { setFull3D(); }
		if (maxStDam > 1) { setMaxDamage(maxStDam); }
		if (damReAmt > 0) { ((IItemArmorMixin) this).setDefense(damReAmt); }
		if (tough > 0.0f) { ((IItemArmorMixin) this).setToughness(tough); }
		if (nbtItem.hasKey("RepairItem", 10)) { repairItemStack = new ItemStack(nbtItem.getCompoundTag("RepairItem")); }
		else { repairItemStack = materialIn.getRepairItemStack(); }
		if (enchantabilityIn > 0) { enchantability = enchantabilityIn; }
		else { enchantability = materialIn.getEnchantability(); }
	}

	@SideOnly(Side.CLIENT)
	@Override
	public String getArmorTexture(@Nonnull ItemStack stack, @Nonnull Entity entity, @Nonnull EntityEquipmentSlot slot, @Nonnull String type) {
		return CustomNpcs.MODID + ":textures/models/armor/" + getCustomName() + "_layer_" + (slot == EntityEquipmentSlot.LEGS ? "2" : "1") + ".png";
	}

	@Override
	public String getCustomName() { return nbtData.getString("RegistryName"); }

	@Override
	public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

	@Override
	public boolean getIsRepairable(@Nonnull ItemStack toRepair, @Nonnull ItemStack repair) {
		ItemStack mat = repairItemStack;
		if (repairItemStack.isEmpty()) {
			mat = getArmorMaterial().getRepairItemStack();
		}
		if (!mat.isEmpty() && net.minecraftforge.oredict.OreDictionary.itemMatches(mat, repair, false)) {
			return true;
		}
		return super.getIsRepairable(toRepair, repair);
	}

	@Override
	public int getItemEnchantability() {
		if (enchantability > 0) { return enchantability; }
		return super.getItemEnchantability();
	}

	@Override
	public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
		if (showInCreative() && (tab == CustomTabs.ITEMS || tab == CreativeTabs.SEARCH)) {
			items.add(new ItemStack(this));
			if (tab == CustomTabs.ITEMS) { Util.instance.sort(items); }
		}
	}
	
	@Override
	public int getElementType() {
		if (nbtData.hasKey("ItemType", 1)) { return nbtData.getByte("ItemType"); }
		return 3;
	}

	@Override
	public boolean showInCreative() {
		return !nbtData.hasKey("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative");
	}

}
