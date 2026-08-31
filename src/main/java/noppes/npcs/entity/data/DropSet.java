package noppes.npcs.entity.data;

import java.util.*;

import com.google.common.collect.HashMultimap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.handler.data.IDropSetData;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.IAttributeSet;
import noppes.npcs.api.entity.data.ICustomDrop;
import noppes.npcs.api.entity.data.IDropNbtSet;
import noppes.npcs.api.entity.data.IEnchantSet;
import noppes.npcs.api.handler.data.IAvailability;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.constants.EnumAvailabilityQuest;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DropSet implements IInventory, ICustomDrop {

	protected final Map<String, Integer> attributeSlotsName = new HashMap<>();
	protected final @Nullable IDropSetData parent;

	public Availability availability = new Availability();
	public List<AttributeSet> attributes = new ArrayList<>();
	public List<EnchantSet> enchants = new ArrayList<>();
	public List<DropNbtSet> tags = new ArrayList<>();
	public ItemStack item = ItemStack.EMPTY;
	public int pos = -1;
	public int npcWorld;
	public int[] amount = new int[] { 1, 1 };
	public float damage = 1.0f;
	public double chance = 100.0d; // 0-100
	public int lootMode = 0; // 0: normal; 1: drop to Player; 2: inventory
	public boolean tiedToLevel = false;

	public DropSet(@Nullable IDropSetData parentIn) {
		parent = parentIn;
		npcWorld = parent == null ? 0 : parent.getNpcLevel();
		attributeSlotsName.put("mainhand", 0);
		attributeSlotsName.put("offhand", 1);
		attributeSlotsName.put("feet", 2);
		attributeSlotsName.put("legs", 3);
		attributeSlotsName.put("chest", 4);
		attributeSlotsName.put("head", 5);
	}

	@SuppressWarnings("all")
	public IAttributeSet addAttribute(IAttributeSet attribute) {
		attributes.add((AttributeSet) attribute);
		return attribute;
	}

	@Override
	public IAttributeSet addAttribute(String attributeName) {
		AttributeSet newAS = new AttributeSet(this);
		newAS.setAttribute(attributeName);
		attributes.add(newAS);
		return newAS;
	}

	@SuppressWarnings("all")
	public IDropNbtSet addDropNbtSet(IDropNbtSet nbtDS) {
		tags.add((DropNbtSet) nbtDS);
		return nbtDS;
	}

	@Override
	public IDropNbtSet addDropNbtSet(int type, double chance, String path, String[] values) {
		DropNbtSet dns = new DropNbtSet(this);
		dns.setType(type);
		dns.setChance(chance);
		dns.setPath(path);
		dns.setValues(values);
		tags.add(dns);
		return dns;
	}

	public IEnchantSet addEnchant(Enchantment enchant) {
		if (enchant != null) {
			EnchantSet newES = new EnchantSet(this);
			newES.setEnchant(enchant);
			enchants.add(newES);
			return newES;
		}
		return null;
	}

	@SuppressWarnings("all")
	public IEnchantSet addEnchant(IEnchantSet enchant) {
		if (enchant != null) {
			enchants.add((EnchantSet) enchant);
			return enchant;
		}
		return null;
	}

	@Override
	public IEnchantSet addEnchant(int enchantId) {
		return addEnchant(Enchantment.getEnchantmentByID(enchantId));
	}

	@Override
	public IEnchantSet addEnchant(String enchantName) {
		return addEnchant(Enchantment.getEnchantmentByLocation(enchantName));
	}

	@Override
	public void clear() { item = ItemStack.EMPTY; }

	@Override
	public void closeInventory(@Nonnull EntityPlayer player) {
	}

	@Override
	public @Nonnull IItemStack createLoot(double addChance) {
		return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(createMCLoot(addChance));
	}

	public @Nonnull ItemStack createMCLoot(double addChance) {
		ItemStack dItem = item.copy();
		// Amount
		int a = amount[0];
		if (amount[0] != amount[1]) {
			if (tiedToLevel) {
				a = (int) Math.round((double) amount[0]
						+ (double) (amount[1] - amount[0]) * (double) npcWorld / (double) CustomNpcs.MaxLv);
			} else {
				a = (int) Math.round((double) amount[0] + (double) (amount[1] - amount[0]) * Math.random());
			}
		}
		dItem.setCount(a);
		// Damage
		if (dItem.getMaxDamage() > 0 && (damage < 1.0f)) {
			int d, max = dItem.getMaxDamage();
			if (tiedToLevel) {
				d = Math.round((1.0f - damage) * (float) max * (float) npcWorld / (float) CustomNpcs.MaxLv);
			} else {
				d = (int) Math.round((1.0f - damage) * (float) max * Math.random());
			}
			dItem.setItemDamage(d);
		}
		// Enchants
		if (!enchants.isEmpty()) {
			for (EnchantSet es : enchants) {
				if (es.chance >= 1.0d || es.chance * addChance / 100.0d < Math.random()) {
					int lvlM = es.getMinLevel();
					int lvlN = es.getMaxLevel();
					if (lvlM == 0 && lvlN == 0) {
						continue;
					}
					int lvl = lvlM;
					if (lvlM != lvlN) {
						if (tiedToLevel) {
							lvl = (int) Math.round((double) lvlM
									+ (double) (lvlN - lvlM) * (double) npcWorld / (double) CustomNpcs.MaxLv);
						} else {
							lvl = (int) Math.round((double) lvlM + (double) (lvlN - lvlM) * Math.random());
						}
					}
					dItem.addEnchantment(es.ench, lvl);
				}
			}

		}
		// Attributes
		if (!attributes.isEmpty()) {
			for (AttributeSet as : attributes) {
				if (as.chance >= 1.0d || as.chance * addChance / 100.0d < Math.random()) {
					double vM = as.getMinValue();
					double vN = as.getMaxValue();
					if (vM == 0.0d && vN == 0.0d) {
						continue;
					}
					double v = vM;
					if (vM != vN) {
						if (tiedToLevel) {
							v = Math.round(
									(vM + (vN - vM) * (double) npcWorld / (double) CustomNpcs.MaxLv) * 10000.0d)
									/ 10000.0d;
						} else {
							v = Math.round((vM + (vN - vM) * Math.random()) * 10000.0d) / 10000.0d;
						}
					}
					(Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(dItem)).setAttribute(as.getAttribute(), v, as.getSlot());
				}
			}
		}
		// Tags
		if (!tags.isEmpty()) {
			NBTTagCompound tag = dItem.getTagCompound();
			if (tag == null) {
				dItem.setTagCompound(tag = new NBTTagCompound());
			}
			for (DropNbtSet dns : tags) {
				if (dns.values.length > 0 && (dns.chance >= 1.0d || dns.chance * addChance / 100.0d < Math.random())) {
					tag = dns.getConstructorTag(new NBTWrapper(tag)).getMCNBT();
				}
			}
		}
		if (dItem.hasTagCompound()) {
			if (dItem.getTagCompound() != null && dItem.getTagCompound().hasNoTags()) {
				dItem.setTagCompound(null);
			}
		}
		return dItem;
	}

	@Override
	public @Nonnull ItemStack decrStackSize(int index, int count) {
		if (index == 0) {
			ItemStack it;
			if (item.getCount() <= count) {
				it = item.copy();
				item = ItemStack.EMPTY;
			}
			else {
				item.splitStack(count);
				it = item.copy();
				if (item.getCount() == 0) { item = ItemStack.EMPTY; }
			}
			return it;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public IAttributeSet[] getAttributeSets() {
		IAttributeSet[] ass = new IAttributeSet[attributes.size()];
		int i = 0;
		for (AttributeSet as : attributes) {
			ass[i] = as;
			i++;
		}
		return ass;
	}

	@Override
	public double getChance() { return Math.round(chance * 10000.0d) / 10000.0d; }

	@Override
	public float getDamage() { return damage; }

	@Override
	public @Nonnull ITextComponent getDisplayName() {
		return new TextComponentString(getName());
	}

	@Override
	public IDropNbtSet[] getDropNbtSets() {
		IDropNbtSet[] nts = new IDropNbtSet[tags.size()];
		int i = 0;
		for (DropNbtSet ts : tags) {
			nts[i] = ts;
			i++;
		}
		return nts;
	}

	@Override
	public IEnchantSet[] getEnchantSets() {
		IEnchantSet[] ess = new IEnchantSet[enchants.size()];
		int i = 0;
		for (EnchantSet es : enchants) {
			ess[i] = es;
			i++;
		}
		return ess;
	}

	@Override
	public int getField(int id) { return 0; }

	@Override
	public int getFieldCount() { return 0; }

	@Override
	public int getInventoryStackLimit() { return 64; }

	@Override
	public IItemStack getItem() { return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item); }

	@Override
	public ItemStack getMCItemStack() { return item; }

	@Override
	public int getLootMode() { return lootMode; }

	@Override
	public int getMaxAmount() { return amount[1]; }

	@Override
	public int getMinAmount() { return amount[0]; }

	@Override
	public IAvailability getAvailability() { return availability; }

	// inventory
	@Override
	public @Nonnull String getName() {
		return "NPC Drop";
	}

	public NBTTagCompound save() {
		NBTTagCompound nbtDS = new NBTTagCompound();
		nbtDS.setTag("Item", item.writeToNBT(new NBTTagCompound()));
		nbtDS.setDouble("Chance", chance);
		nbtDS.setDouble("DamageToItem", damage);
		nbtDS.setInteger("LootMode", lootMode);
		nbtDS.setBoolean("TiedToLevel", tiedToLevel);
		nbtDS.setTag("Availability", availability.save(new NBTTagCompound()));
		nbtDS.setIntArray("Amount", amount);
		NBTTagList ench = new NBTTagList();
		for (EnchantSet es : enchants) { ench.appendTag(es.getNBT()); }
		nbtDS.setTag("EnchantSettings", ench);
		NBTTagList attr = new NBTTagList();
		for (AttributeSet as : attributes) { attr.appendTag(as.getNBT()); }
		nbtDS.setTag("AttributeSettings", attr);
		NBTTagList tgsl = new NBTTagList();
		for (DropNbtSet ts : tags) { tgsl.appendTag(ts.getNBT()); }
		nbtDS.setTag("TagSettings", tgsl);
		nbtDS.setInteger("Slot", pos);
		return nbtDS;
	}

	@Override
	public int getSizeInventory() { return 1; }

	@Override
	public @Nonnull ItemStack getStackInSlot(int index) {
		if (index == 0) { return item; }
		return ItemStack.EMPTY;
	}

	@Override
	public boolean getTiedToLevel() {
		return tiedToLevel;
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public boolean isEmpty() {
        return NoppesUtilServer.isItemStackNull(item) || item.isEmpty();
    }

	@Override
	public boolean isItemValidForSlot(int index, @Nonnull ItemStack stack) {
		return true;
	}

	@Override
	public boolean isUsableByPlayer(@Nonnull EntityPlayer player) {
		return true;
	}

	public void load(NBTTagCompound nbtDS) {
		item = new ItemStack(nbtDS.getCompoundTag("Item"));
		chance = nbtDS.getDouble("Chance");
		damage = nbtDS.getFloat("DamageToItem");
		if (nbtDS.hasKey("LootMode", 1)) { lootMode = nbtDS.getBoolean("LootMode") ? 1 : 0;}
		else if (nbtDS.hasKey("LootMode", 3)) { lootMode = nbtDS.getInteger("LootMode");}
		tiedToLevel = nbtDS.getBoolean("TiedToLevel");
		if (nbtDS.hasKey("Availability", 10)) { availability.load(nbtDS.getCompoundTag("Availability")); }
		else if (nbtDS.hasKey("Availability", 10)) { // OLD
			availability.clear();
			int questId = nbtDS.getInteger("QuestId");
			if (questId > 0) { availability.setQuest(questId, EnumAvailabilityQuest.Active.ordinal()); }
		}
		int[] cnts = nbtDS.getIntArray("Amount");
		if (nbtDS.hasKey("Amount", 9)) {
			cnts = new int[2];
			for (int i = 0; i < 2; i++) { cnts[i] = nbtDS.getTagList("Amount", 3).getIntAt(i); }
		}
		if (cnts.length != 2) {
			int m = 1, n = 1;
			if (cnts.length >= 1) { m = cnts[0]; }
			if (cnts.length >= 2) { n = cnts[1]; }
			cnts = new int[] { m, n };
		}
		List<EnchantSet> ench = new ArrayList<>();
		for (NBTBase ne : nbtDS.getTagList("EnchantSettings", 10)) {
			EnchantSet es = new EnchantSet(this);
			es.load((NBTTagCompound) ne);
			ench.add(es);
		}
		enchants = ench;
		List<AttributeSet> attr = new ArrayList<>();
		for (NBTBase na : nbtDS.getTagList("AttributeSettings", 10)) {
			AttributeSet as = new AttributeSet(this);
			as.load((NBTTagCompound) na);
			attr.add(as);
		}
		attributes = attr;
		List<DropNbtSet> tgsl = new ArrayList<>();
		for (NBTBase na : nbtDS.getTagList("TagSettings", 10)) {
			DropNbtSet ts = new DropNbtSet(this);
			ts.load((NBTTagCompound) na);
			tgsl.add(ts);
		}
		tags = tgsl;
		pos = nbtDS.getInteger("Slot");
		setAmount(cnts[0], cnts[1]);
	}

	@Override
	public void markDirty() { }

	@Override
	public void openInventory(@Nonnull EntityPlayer player) { }

	@Override
	public void remove() {
		if (parent != null) { parent.removeDrop(this); }
	}

	@Override
	public void removeAttribute(IAttributeSet attribute) {
		attributes.remove((AttributeSet) attribute);
	}

	@Override
	public void removeDropNbt(IDropNbtSet nbt) { tags.remove((DropNbtSet) nbt); }

	@Override
	public void removeEnchant(IEnchantSet enchant) { enchants.remove((EnchantSet) enchant); }

	@Override
	public @Nonnull ItemStack removeStackFromSlot(int index) {
		ItemStack stack = item;
		item = ItemStack.EMPTY;
		return stack;
	}

	@Override
	public void resetTo(IItemStack itemIn) {
		if (itemIn == null) { return; }
		resetTo(itemIn.getMCItemStack());
	}

	public void resetTo(ItemStack itemIn) {
		if (itemIn == null || itemIn.isEmpty()) { return; }
		double ch = 85.0d;
		damage = 1.0f;
		lootMode = 0;
		tiedToLevel = false;
		enchants = new ArrayList<>();
		attributes = new ArrayList<>();
		tags = new ArrayList<>();
		// Item Damage
		HashMultimap<String, AttributeModifier> map = (HashMultimap<String, AttributeModifier>) this.item.getAttributeModifiers(EntityEquipmentSlot.MAINHAND);
		Iterator<Map.Entry<String, AttributeModifier>> iterator = map.entries().iterator();
		double d = 0.0;
		while (iterator.hasNext()) {
			Map.Entry<String, AttributeModifier> entry = iterator.next();
			if (entry.getKey().equals(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
				try {
					AttributeModifier mod = entry.getValue();
					d = mod.getAmount();
				}
				catch (Exception e) { LogWriter.error(e); }
			}
		}
		d += EnchantmentHelper.getModifierForCreature(this.item, EnumCreatureAttribute.UNDEFINED);
		if (d > 1.0d) {
			if (itemIn.getItemDamage() == itemIn.getMaxDamage()) { damage = 1.0f; }
			else { damage = (float) Math.round((double) itemIn.getItemDamage() / (double) itemIn.getMaxDamage() * 100.0d) / 100.0f; }
		}
		amount = new int[] { 1, 1 };
		// Amount
		if (itemIn.getCount() > 1) { amount[1] = itemIn.getCount(); }
		NBTTagCompound itemNbt = itemIn.getTagCompound();
		// Enchants
		if (itemNbt != null && itemNbt.hasKey("ench")) {
			lootMode = 2;
			ch /= itemNbt.getTagList("ench", 10).tagCount();
			for (NBTBase nbtEnch : itemNbt.getTagList("ench", 10)) {
				IEnchantSet es = addEnchant(((NBTTagCompound) nbtEnch).getShort("id"));
				if (es != null) {
					es.setLevels(0, ((NBTTagCompound) nbtEnch).getShort("lvl"));
					es.setChance(85.0d / (double) itemNbt.getTagList("ench", 10).tagCount());
				}
			}
			itemNbt.removeTag("ench");
		}
		// Attributes
		if (itemNbt != null && itemNbt.hasKey("AttributeModifiers")) {
			lootMode = 2;
			ch /= itemNbt.getTagList("AttributeModifiers", 10).tagCount();
			for (NBTBase nbtAttr : itemNbt.getTagList("AttributeModifiers", 10)) {
				IAttributeSet as = addAttribute(((NBTTagCompound) nbtAttr).getString("AttributeName"));
				if (as != null) {
					int slot = -1;
					if (attributeSlotsName.containsKey(((NBTTagCompound) nbtAttr).getString("Slot"))) { slot = attributeSlotsName.get(((NBTTagCompound) nbtAttr).getString("Slot")); }
					as.setSlot(slot);
					double value = ((NBTTagCompound) nbtAttr).getDouble("Amount");
					if (value < 0.0d) { as.setValues(value, 0.0d); }
					else if (value > 0.0d) { as.setValues(0.0d, value); }
					else { as.setValues(0.0d, 0.05d); }
					as.setChance(85.0d / (double) itemNbt.getTagList("AttributeModifiers", 10).tagCount());
				}
			}
			itemNbt.removeTag("AttributeModifiers");
		}
		// Chance
		setChance(ch);
		// Simple Item Set
		NBTTagCompound itemFromNbt = new NBTTagCompound();
		itemIn.writeToNBT(itemFromNbt);
		if (itemNbt != null) { itemFromNbt.setTag("tag", itemNbt); }
		ItemStack newItem = new ItemStack(itemFromNbt);
		newItem.setCount(1);
		if (d > 1) { newItem.setItemDamage(0); }
		item = newItem;
	}

	@Override
	public void setAmount(int min, int max) {
		int newMin = min;
		int newMax = max;
		if (min > max) {
			newMin = max;
			newMax = min;
		}
		if (newMin < 1) {
			newMin = 1;
		}
		if (newMin > item.getMaxStackSize()) {
			newMin = item.getMaxStackSize();
		}
		if (newMax < newMin) {
			newMax = newMin;
		}
		if (newMax > item.getMaxStackSize()) {
			newMax = item.getMaxStackSize();
		}
		amount[0] = newMin;
		amount[1] = newMax;
	}

	@Override
	public void setChance(double chanceIn) { chance = Math.round(ValueUtil.correctDouble(chanceIn, 0.0001d, 100.0d) * 10000.0d) / 10000.0d; }

	@Override
	public void setDamage(float damage) { this.damage = ValueUtil.correctFloat(damage, 0.0f, 1.0f); }

	@Override
	public void setField(int id, int value) { }

	@Override
	public void setInventorySlotContents(int index, @Nonnull ItemStack stack) {
		if (index == 0) { item = stack; }
	}

	@Override
	public void setItem(IItemStack itemIn) { item = itemIn.getMCItemStack(); }

	@Override
	public void setLootMode(int mode) { lootMode = mode % 3; }

	@Override
	public void setTiedToLevel(boolean tied) { tiedToLevel = tied; }

	public Component getKey() {
		if (item == null) { return Component.literal("null"); }
		if (item.isEmpty()) { return Component.translatable("type.empty"); }
		Component keyName = Component.empty()
				.append(Component.literal((pos + 1) + ": ").withStyle(TextFormatting.GRAY));
		double ch = Math.round(chance * 10.0d) / 10.d;
		String chance = String.valueOf(ch).replace(".", ",");
		if (ch == (int) ch) { chance = String.valueOf((int) ch); }
		chance += "%";
		keyName.append(Component.literal(chance).withStyle(TextFormatting.YELLOW));
		if (amount[0] == amount[1]) {
			keyName.append(Component.literal("[").withStyle(TextFormatting.GRAY))
					.append(Component.literal("" + amount[0]).withStyle(TextFormatting.GOLD))
					.append(Component.literal("]").withStyle(TextFormatting.GRAY));
		}
		else {
			keyName.append(Component.literal("[").withStyle(TextFormatting.GRAY))
					.append(Component.literal("" + amount[0]).withStyle(TextFormatting.GOLD))
					.append(Component.literal("<>").withStyle(TextFormatting.GRAY)
							.append(Component.literal("" + amount[1]).withStyle(TextFormatting.GOLD))
							.append(Component.literal("]").withStyle(TextFormatting.GRAY)));
		}
		Component effs = Component.empty();
		if (!enchants.isEmpty()) {
			effs.append(Component.literal(" |").withStyle(TextFormatting.GRAY))
					.append(Component.literal("E").withStyle(TextFormatting.AQUA))
					.append(Component.literal("|").withStyle(TextFormatting.GRAY));
		}
		if (!attributes.isEmpty()) {
			effs.append(Component.literal(" |").withStyle(TextFormatting.GRAY))
					.append(Component.literal("A").withStyle(TextFormatting.GREEN))
					.append(Component.literal("|").withStyle(TextFormatting.GRAY));
		}
		if (!tags.isEmpty()) {
			effs.append(Component.literal(" |").withStyle(TextFormatting.GRAY))
					.append(Component.literal("T").withStyle(TextFormatting.RED))
					.append(Component.literal("|").withStyle(TextFormatting.GRAY));
		}
		keyName.append(effs)
				.append(Component.literal(item.getDisplayName()).withStyle(TextFormatting.RESET));
		if (pos < 0) {
			keyName.append(Component.literal("ID:" + toString().substring(toString().indexOf("@") + 1))
					.withStyle(TextFormatting.DARK_GRAY));
		}
		return keyName;
	}

	public List<Component> getHover(boolean isReward) {
		List<Component> list = new ArrayList<>();
		// pos
		if (pos < 0) {
			list.add(Component.empty()
					.append(Component.translatable("gui.position").withStyle(TextFormatting.GRAY))
					.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
					.append(Component.literal(toString().substring(toString().indexOf("@") + 1)).withStyle(TextFormatting.DARK_GRAY)));
		}
		else {
			list.add(Component.empty()
					.append(Component.translatable("gui.position").withStyle(TextFormatting.GRAY))
					.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
					.append(Component.literal("" + pos).withStyle(TextFormatting.RESET)));
		}
		// stack
		Component stackKey = Component.empty()
				.append(Component.translatable("gui.name").withStyle(TextFormatting.GRAY))
				.append(Component.literal(": ").withStyle(TextFormatting.GRAY));
		if (item == null) { stackKey.append(Component.literal("null").withStyle(TextFormatting.DARK_RED)); }
		else if (item.isEmpty()) { stackKey.append(Component.translatable("type.empty").withStyle(TextFormatting.RED)); }
		else { stackKey.append(Component.literal("" + item.getItem().getRegistryName()).withStyle(TextFormatting.RESET)); }
		list.add(stackKey);
		// amount
		Component amountKey = Component.empty()
				.append(Component.translatable("quest.itemamount").withStyle(TextFormatting.GRAY))
				.append(Component.literal(": ").withStyle(TextFormatting.GRAY));
		if (amount[0] == amount[1]) { amountKey.append(Component.literal("" + amount[0]).withStyle(TextFormatting.GOLD)); }
		else {
			amountKey.append(Component.literal("[min:").withStyle(TextFormatting.GRAY))
					.append(Component.literal("" + amount[0]).withStyle(TextFormatting.GOLD))
					.append(Component.literal("; max:").withStyle(TextFormatting.GRAY))
					.append(Component.literal("" + amount[1]).withStyle(TextFormatting.GOLD))
					.append(Component.literal("]").withStyle(TextFormatting.GRAY));
		}
		list.add(amountKey);
		// chance
		Component chanceKey = Component.empty()
				.append(Component.translatable("drop.chance").withStyle(TextFormatting.GRAY))
				.append(Component.literal(": ").withStyle(TextFormatting.GRAY));
		if (chance == (int) chance) {
			chanceKey.append(Component.literal("" + (int) chance).withStyle(TextFormatting.YELLOW))
					.append(Component.literal("%").withStyle(TextFormatting.GRAY));
		}
		else {
			chanceKey.append(Component.literal(("" + chance).replace(".", ",")).withStyle(TextFormatting.YELLOW))
					.append(Component.literal("%").withStyle(TextFormatting.GRAY));
		}
		list.add(chanceKey);
		// loot mode
		list.add(Component.empty()
				.append(Component.translatable("inv.lootpickup").withStyle(TextFormatting.GRAY))
				.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
				.append(Component.translatable("inv.lootmode." + (lootMode % 3) + "." + isReward).withStyle(TextFormatting.RESET)));
		// availability
		list.add(Component.empty()
				.append(Component.translatable("availability.available").withStyle(TextFormatting.GRAY))
				.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
				.append(Component.translatable("availability." + (availability.hasOptions() ? "contains" : "except")).withStyle(TextFormatting.RESET)));
		// enchants
		if (!enchants.isEmpty()) {
			Component enchKey = Component.empty()
					.append(Component.translatable("drop.enchants").withStyle(TextFormatting.GRAY))
					.append(Component.literal(": [").withStyle(TextFormatting.GRAY));
			boolean start = false;
			for (EnchantSet es : enchants) {
				if (start) { enchKey.append(Component.literal(", ").withStyle(TextFormatting.GRAY)); }
				if (es.ench == null) { enchKey.append(Component.literal("null").withStyle(TextFormatting.GRAY)); }
				else {
					enchKey.append(Component.literal("id: ").withStyle(TextFormatting.GRAY))
							.append(Component.literal("" + Enchantment.getEnchantmentID(es.ench)).withStyle(TextFormatting.AQUA));
				}
				start = true;
			}
			list.add(enchKey.append(Component.literal("]").withStyle(TextFormatting.GRAY)));
		}
		else {
			list.add(Component.empty()
					.append(Component.translatable("drop.enchants").withStyle(TextFormatting.GRAY))
					.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
					.append(Component.translatable("availability.except").withStyle(TextFormatting.RESET)));
		}
		// attributes
		if (!attributes.isEmpty()) {
			Component attrKey = Component.empty()
					.append(Component.translatable("drop.attributes").withStyle(TextFormatting.GRAY))
					.append(Component.literal(": [").withStyle(TextFormatting.GRAY));
			boolean start = false;
			for (AttributeSet as : attributes) {
				if (start) { attrKey.append(Component.literal(", ").withStyle(TextFormatting.GRAY)); }
				if (as.attr == null) { attrKey.append(Component.literal("null").withStyle(TextFormatting.GRAY)); }
				else {
					attrKey.append(Component.literal("id: ").withStyle(TextFormatting.GRAY))
							.append(Component.literal(as.attr.getName()).withStyle(TextFormatting.BLUE));
				}
				start = true;
			}
			list.add(attrKey.append(Component.literal("]").withStyle(TextFormatting.GRAY)));
		}
		else {
			list.add(Component.empty()
					.append(Component.translatable("drop.attributes").withStyle(TextFormatting.GRAY))
					.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
					.append(Component.translatable("availability.except").withStyle(TextFormatting.RESET)));
		}
		// tags
		if (!tags.isEmpty()) {
			Component nbtKey = Component.empty()
					.append(Component.translatable("drop.tags").withStyle(TextFormatting.GRAY))
					.append(Component.literal(": [").withStyle(TextFormatting.GRAY));
			boolean start = false;
			for (DropNbtSet ns : tags) {
				if (start) { nbtKey.append(Component.literal(", ").withStyle(TextFormatting.GRAY)); }
				if (ns.path == null) { nbtKey.append(Component.literal("null").withStyle(TextFormatting.GRAY)); }
				else {
					nbtKey.append(Component.literal("id: ").withStyle(TextFormatting.GRAY))
							.append(Component.literal(ns.path).withStyle(TextFormatting.BLUE));
				}
				start = true;
			}
			list.add(nbtKey.append(Component.literal("]").withStyle(TextFormatting.GRAY)));
		}
		else {
			list.add(Component.empty()
					.append(Component.translatable("drop.tags").withStyle(TextFormatting.GRAY))
					.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
					.append(Component.translatable("availability.except").withStyle(TextFormatting.RESET)));
		}
		return list;
	}

	public DropSet copy() {
		DropSet drop = new DropSet(parent);
		drop.load(save());
		return drop;
	}

}
