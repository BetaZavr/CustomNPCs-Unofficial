package noppes.npcs.items.custom;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomTabs;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.client.model.ModelBipedAlt;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.client.util.CustomArmorOBJData;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.mixin.item.IItemArmorMixin;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CustomArmorOBJ extends ItemArmor implements ICustomElement {

    protected static final Map<String, CustomArmorOBJData> OBJ_CASH = new HashMap<>();

    protected final @Nonnull NBTTagCompound nbtData;
    protected final ItemStack repairItemStack;
    protected final int enchantability;

    public CustomArmorOBJ(@Nonnull ItemArmor.ArmorMaterial materialIn, int renderIndexIn, @Nonnull EntityEquipmentSlot slotIn,
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

    @Override
    @SideOnly(Side.CLIENT)
    public ModelBiped getArmorModel(@Nonnull EntityLivingBase entity, @Nonnull ItemStack itemStack, @Nonnull EntityEquipmentSlot slot,
                                    @Nonnull ModelBiped defModel) {
        if (getArmorOBJData().isValid()) {
            if (defModel instanceof ModelBipedAlt) { return defModel; }
            return CustomArmorOBJData.getArmorModel(slot);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public String getArmorTexture(@Nonnull ItemStack stack, @Nonnull Entity entity, @Nonnull EntityEquipmentSlot slot, @Nonnull String type) {
        return CustomNpcs.MODID + ":textures/items/null.png";
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

    @SideOnly(Side.CLIENT)
    public @Nullable IBakedModel getIBakedModel() { return getArmorOBJData().getIBakedModel(getEquipmentSlot()); }

    @SideOnly(Side.CLIENT)
    public @Nullable ParameterizedModel getParameterizedModel(@Nonnull EnumParts part, @Nonnull ItemStack stack,
                                                              @Nullable ResourceLocation textureLocation, boolean isDown) {
        if (isDown) {
            switch(part) {
                case ARM_RIGHT: part = EnumParts.WRIST_RIGHT; break;
                case ARM_LEFT: part = EnumParts.WRIST_LEFT; break;
                case LEG_RIGHT: part = EnumParts.FEET_RIGHT; break;
                case LEG_LEFT: part = EnumParts.FEET_LEFT; break;
                default: { break; }
            }
        }
        return getArmorOBJData().getParameterizedModel(part, stack, textureLocation);
    }

    @SideOnly(Side.CLIENT)
    public @Nullable ParameterizedModel getStackModel(@Nonnull ItemStack stack) { return getArmorOBJData().getStackModel(stack); }

    public @Nonnull CustomArmorOBJData getArmorOBJData() {
        String name = getCustomName();
        if (!OBJ_CASH.containsKey(name)) { OBJ_CASH.put(name, new CustomArmorOBJData(nbtData)); }
        return OBJ_CASH.get(name);
    }

}
