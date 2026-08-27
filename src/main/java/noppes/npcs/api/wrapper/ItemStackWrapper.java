package noppes.npcs.api.wrapper;

import com.google.common.collect.Multimap;
import com.google.gson.JsonParseException;

import java.util.*;
import java.util.Map.Entry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.INbt;
import noppes.npcs.api.constants.ItemType;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IMob;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.data.Data;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemScripted;
import org.jetbrains.annotations.NotNull;

public class ItemStackWrapper implements IItemStack, ICapabilitySerializable<CompoundTag> {

   protected static final EquipmentSlot[] VALID_EQUIPMENT_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   protected static final ResourceLocation key = new ResourceLocation(CustomNpcs.MODID, "itemscripteddata");

   public static Capability<ItemStackWrapper> ITEMSCRIPTEDDATA_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
   public static ItemStackWrapper AIR = new ItemStackWrapper(ItemStack.EMPTY);

   protected final LazyOptional<ItemStackWrapper> instance = LazyOptional.of(() -> this);
   protected final Data tempdata;
   protected final Data storeddata;
   protected IEntity<?> owner = null;

   public final ItemStack item;

   protected ItemStackWrapper(ItemStack stack) {
      if (stack.isEmpty()) {
         tempdata = null;
         storeddata = null;
      } else {
         tempdata = new Data();
         storeddata = new Data();
      }
      item = stack;
   }

   @Override
   public IData getTempdata() { return tempdata; }

   @Override
   public IData getStoreddata() { return storeddata; }

   @Override
   public int getStackSize() { return item.getCount(); }

   @Override
   public void setStackSize(int size) {
      if (size > getMaxStackSize()) { throw new CustomNPCsException("Can't set the stack size bigger than Max Stack size"); }
      item.setCount(size);
   }

   @Override
   public void setAttribute(String name, double value) { setAttribute(name, value, -1); }

   @Override
   public void setAttribute(String name, double value, int slot) {
      if (slot >= -1 && slot <= 5) {
         CompoundTag compound = item.getTag();
         if (compound == null) { item.setTag(compound = new CompoundTag()); }
         ListTag tagList = compound.getList("AttributeModifiers", 10);
         ListTag newList = new ListTag();
         UUID uuid = null;
         for(int i = 0; i < tagList.size(); ++i) {
            CompoundTag c = tagList.getCompound(i);
            if (!c.getString("AttributeName").equals(name)) { newList.add(c); }
            else { uuid = c.getUUID("UUID"); }
         }
         if (value != 0.0D) {
            CompoundTag nbt = (new AttributeModifier(name, value, Operation.ADDITION)).save();
            nbt.putString("AttributeName", name);
            if (slot >= 0) { nbt.putString("Slot", EquipmentSlot.values()[slot].getName()); }
            if (uuid != null) { nbt.putUUID("UUID", uuid); }
            newList.add(nbt);
         }
         compound.put("AttributeModifiers", newList);
      }
      else { throw new CustomNPCsException("Slot has to be between -1 and 5, given was: " + slot); }
   }

   @Override
   public double getAttribute(String name) {
      CompoundTag compound = item.getTag();
      if (compound == null) { return 0.0D; }
      Multimap<Attribute, AttributeModifier> map = item.getAttributeModifiers(EquipmentSlot.MAINHAND);
      Iterator<Entry<Attribute, AttributeModifier>> var4 = map.entries().iterator();
      Entry<Attribute, AttributeModifier> entry;
      do {
         if (!var4.hasNext()) { return 0.0D; }
         entry = var4.next();
      } while(!entry.getKey().getDescriptionId().equals(name));
      AttributeModifier mod = entry.getValue();
      return mod.getAmount();
   }

   @Override
   public boolean hasAttribute(String name) {
      CompoundTag compound = item.getTag();
      if (compound != null) {
         ListTag tagList = compound.getList("AttributeModifiers", 10);
         for (int i = 0; i < tagList.size(); ++i) {
            CompoundTag c = tagList.getCompound(i);
            if (c.getString("AttributeName").equals(name)) { return true; }
         }
      }
      return false;
   }

   @Override
   public void addEnchantment(String id, int strenght) {
      Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.tryParse(id));
      if (ench == null) { throw new CustomNPCsException("Unknown enchant id:" + id); }
      item.enchant(ench, strenght);
   }

   @Override
   public boolean isEnchanted() { return item.isEnchanted(); }

   @Override
   public boolean hasEnchant(String id) {
      Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.tryParse(id));
      if (ench == null) { throw new CustomNPCsException("Unknown enchant id:" + id); }
      if (!isEnchanted()) { return false; }
      ListTag list = item.getEnchantmentTags();
      for(int i = 0; i < list.size(); ++i) {
         CompoundTag compound = list.getCompound(i);
         if (compound.getString("id").equalsIgnoreCase(id)) { return true; }
      }
      return false;
   }

   @Override
   public boolean removeEnchant(String id) {
      Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.tryParse(id));
      if (ench == null) { throw new CustomNPCsException("Unknown enchant id:" + id); }
      if (!isEnchanted()) { return false; }
      ListTag list = item.getEnchantmentTags();
      ListTag newList = new ListTag();
      for(int i = 0; i < list.size(); ++i) {
         CompoundTag compound = list.getCompound(i);
         if (!compound.getString("id").equalsIgnoreCase(id)) { newList.add(compound); }
      }
      if (list.size() == newList.size()) { return false; }
      CompoundTag compound = item.getTag();
      if (compound == null) { item.setTag(compound = new CompoundTag()); }
      compound.put("ench", newList);
      return true;
   }

   @Override
   public boolean isBlock() { return Block.byItem(item.getItem()) != Blocks.AIR; }

   @Override
   public boolean hasCustomName() { return item.hasCustomHoverName(); }

   @Override
   public void setCustomName(String name) { item.setHoverName(Component.translatable(name)); }

   @Override
   public String getDisplayName() { return item.getHoverName().getString(); }

   @Override
   public String getItemName() { return item.getItem().getName(item).getString(); }

   @Override
   public String getName() {
      return Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item.getItem())).toString();
   }

   @Override
   public INbt getNbt() {
      CompoundTag compound = item.getTag();
      if (compound == null) {
         item.setTag(compound = new CompoundTag());
      }
      return new NBTWrapper(compound);
   }

   @Override
   public boolean hasNbt() {
      CompoundTag compound = item.getTag();
      return compound != null && !compound.isEmpty();
   }

   @Override
   public ItemStack getMCItemStack() { return item; }

   public static ItemStack MCItem(IItemStack item) { return item == null ? ItemStack.EMPTY : item.getMCItemStack(); }

   @Override
   public void damageItem(int damage, IMob<?> living) {
      if (living != null) {
         item.hurtAndBreak(damage, living.getMCEntity(), (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      }
      else if (item.isDamageableItem()) {
         if (item.getDamageValue() <= damage) {
            item.shrink(1);
            item.setDamageValue(0);
         } else {
            item.setDamageValue(item.getDamageValue() - damage);
         }
      }
   }

   @Override
   public boolean isBook() { return false; }

   @Override
   @SuppressWarnings("deprecation")
   public int getFoodLevel() { return item.getItem().getFoodProperties() != null ? item.getItem().getFoodProperties().getNutrition() : 0; }

   @Override
   public IItemStack copy() { return createNew(item.copy()); }

   @Override
   public int getMaxStackSize() { return item.getMaxStackSize(); }

   @Override
   public boolean isDamageable() { return item.isDamageableItem(); }

   @Override
   public int getDamage() { return getItemDamage(); }

   @Override
   public void setDamage(int value) { setItemDamage(value); }

   public int getItemDamage() { return item.getDamageValue(); }

   public void setItemDamage(int value) { item.setDamageValue(value); }

   @Override
   public int getMaxDamage() {
      return item.getMaxDamage();
   }

   @Override
   public INbt getItemNbt() {
      CompoundTag compound = new CompoundTag();
      item.save(compound);
      return new NBTWrapper(compound);
   }

   @Override
   public double getAttackDamage() {
      Multimap<Attribute, AttributeModifier> map = item.getAttributeModifiers(EquipmentSlot.MAINHAND);
      double damage = 0.0D;
      for (Entry<Attribute, AttributeModifier> entry : map.entries()) {
         if (entry.getKey() == Attributes.ATTACK_DAMAGE) {
            AttributeModifier mod = entry.getValue();
            damage = mod.getAmount();
         }
      }
      return damage + (double)EnchantmentHelper.getDamageBonus(item, MobType.UNDEFINED);
   }

   @Override
   public boolean isEmpty() { return item.isEmpty(); }

   @Override
   public int getType() {
      if (item.getItem() instanceof IPlantable) { return ItemType.SEEDS.get(); }
      return item.getItem() instanceof SwordItem ? ItemType.SWORD.get() : ItemType.NORMAL.get();
   }

   @Override
   public boolean isWearable() {
      for (EquipmentSlot slot : VALID_EQUIPMENT_SLOTS) {
         if (item.getItem().canEquip(item, slot, EntityNPCInterface.CommandPlayer)) { return true; }
      }
      return false;
   }

   @Override
   public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, Direction facing) {
      return capability == ITEMSCRIPTEDDATA_CAPABILITY ? instance.cast() : LazyOptional.empty();
   }

   public static void register(AttachCapabilitiesEvent<ItemStack> event) {
      ItemStackWrapper wrapper = createNew(event.getObject());
      event.addCapability(key, wrapper);
   }

   private static ItemStackWrapper createNew(ItemStack item) {
      if (item != null && !item.isEmpty()) {
         if (item.getItem() instanceof ItemScripted) { return new ItemScriptedWrapper(item); }
         if (item.getItem() != Items.WRITTEN_BOOK && item.getItem() != Items.WRITABLE_BOOK && !(item.getItem() instanceof WritableBookItem) && !(item.getItem() instanceof WrittenBookItem)) {
            if (item.getItem() instanceof ArmorItem) { return new ItemArmorWrapper(item); }
            Block block = Block.byItem(item.getItem());
            return block != Blocks.AIR ? new ItemBlockWrapper(item) : new ItemStackWrapper(item);
         }
         return new ItemBookWrapper(item);
      }
      return AIR;
   }

   @Override
   public String[] getLore() {
      CompoundTag compound = item.getTagElement("display");
      if (compound != null && compound.getTagType("Lore") == 9) {
         ListTag tagList = compound.getList("Lore", 8);
         if (tagList.isEmpty()) { return new String[0]; }
         List<String> lore = new ArrayList<>();
         for(int i = 0; i < tagList.size(); ++i) { lore.add(tagList.getString(i)); }
         return lore.toArray(new String[0]);
      }
      return new String[0];
   }

   @Override
   public void setLore(String[] lore) {
      CompoundTag compound = item.getOrCreateTagElement("display");
      if (lore != null && lore.length != 0) {
         ListTag tagList = new ListTag();
         for (String string : lore) {
            String s = string;
            try {
               Serializer.fromJson(s);
            } catch (JsonParseException var9) {
               s = Serializer.toJson(Component.translatable(s));
            }
            tagList.add(StringTag.valueOf(s));
         }
         compound.put("Lore", tagList);
      } else {
         compound.remove("Lore");
      }
   }

   @Override
   public CompoundTag serializeNBT() {
      return getMCNbt();
   }

   @Override
   public void deserializeNBT(CompoundTag nbt) {
      setMCNbt(nbt);
   }

   public CompoundTag getMCNbt() {
      CompoundTag compound = new CompoundTag();
      if (storeddata != null && !storeddata.getNbt().isEmpty()) {
         compound.put("StoredData", storeddata.getNbt().getMCNBT());
      }
      return compound;
   }

   public void setMCNbt(CompoundTag compound) {
      if (storeddata == null) { return; }
      if (compound == null) { storeddata.clear(); }
      else { storeddata.setNbt(compound.getCompound("StoredData")); }
   }

   @Override
   public void removeNbt() { item.setTag(null); }

   @Override
   public boolean compare(IItemStack item, boolean ignoreNBT) {
      if (item == null) { item = AIR; }
      return NoppesUtilPlayer.compareItems(getMCItemStack(), item.getMCItemStack(), false, ignoreNBT);
   }

   // New from Unofficial (BetaZavr)
   @Override
   public IEntity<?> getOwner() { return owner; }

   @Override
   public void setOwner(IEntity<?> iEntity) { owner = iEntity; }

}
