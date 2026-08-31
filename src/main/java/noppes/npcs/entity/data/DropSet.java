package noppes.npcs.entity.data;

import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.IAttributeSet;
import noppes.npcs.api.entity.data.ICustomDrop;
import noppes.npcs.api.entity.data.IDropNbtSet;
import noppes.npcs.api.entity.data.IEnchantSet;
import noppes.npcs.api.handler.data.IDropSetData;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.constants.EnumAvailabilityQuest;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.util.ValueUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DropSet implements Container, ICustomDrop {

    protected final Map<String, Integer> attributeSlotsName = new HashMap<>();
    protected final @Nullable IDropSetData parent;

    public Availability availability = new Availability();
    public List<AttributeSet> attributes = new ArrayList<>();
    public List<EnchantSet> enchants = new ArrayList<>();
    public List<DropNbtSet> tags = new ArrayList<>();
    public ItemStack item = ItemStack.EMPTY;
    public int pos = -1;
    public int npcLevel;
    public int[] amount = new int[] { 1, 1 };
    public double chance = 100.0d; // 0-100
    public int lootMode = 0; // 0: normal; 1: drop to Player; 2: inventory
    public boolean tiedToLevel = false;

    public DropSet(@Nullable IDropSetData parentIn) {
        parent = parentIn;
        npcLevel = parent == null ? 0 : parent.getNpcLevel();
        attributeSlotsName.put("mainhand", 0);
        attributeSlotsName.put("offhand", 1);
        attributeSlotsName.put("feet", 2);
        attributeSlotsName.put("legs", 3);
        attributeSlotsName.put("chest", 4);
        attributeSlotsName.put("head", 5);
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public IEnchantSet addEnchant(IEnchantSet enchant) {
        if (enchant != null) {
            enchants.add((EnchantSet) enchant);
            return enchant;
        }
        return null;
    }

    @Override
    public IEnchantSet addEnchant(int enchantId) { return addEnchant(Enchantment.byId(enchantId)); }

    @Override
    @SuppressWarnings("deprecation")
    public IEnchantSet addEnchant(String enchantName) {
        return addEnchant(BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(enchantName)));
    }

    public void clear() { item = ItemStack.EMPTY; }

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
                        + (double) (amount[1] - amount[0]) * (double) npcLevel / (double) CustomNpcs.MaxLv);
            }
            else { a = (int) Math.round((double) amount[0] + (double) (amount[1] - amount[0]) * Math.random());}
        }
        dItem.setCount(a);
        // Enchants
        if (!enchants.isEmpty()) {
            for (EnchantSet es : enchants) {
                if (es.chance >= 1.0d || es.chance * addChance / 100.0d < Math.random()) {
                    int lvlM = es.getMinLevel();
                    int lvlN = es.getMaxLevel();
                    if (lvlM == 0 && lvlN == 0) { continue; }
                    int lvl = lvlM;
                    if (lvlM != lvlN) {
                        if (tiedToLevel) {
                            lvl = (int) Math.round((double) lvlM
                                    + (double) (lvlN - lvlM) * (double) npcLevel / (double) CustomNpcs.MaxLv);
                        }
                        else { lvl = (int) Math.round((double) lvlM + (double) (lvlN - lvlM) * Math.random()); }
                    }
                    dItem.enchant(es.ench, lvl);
                }
            }

        }
        // Attributes
        if (!attributes.isEmpty()) {
            for (AttributeSet as : attributes) {
                if (as.chance >= 1.0d || as.chance * addChance / 100.0d < Math.random()) {
                    double vM = as.getMinValue();
                    double vN = as.getMaxValue();
                    if (vM == 0.0d && vN == 0.0d) { continue; }
                    double v = vM;
                    if (vM != vN) {
                        if (tiedToLevel) {
                            v = Math.round(
                                    (vM + (vN - vM) * (double) npcLevel / (double) CustomNpcs.MaxLv) * 10000.0d)
                                    / 10000.0d;
                        }
                        else { v = Math.round((vM + (vN - vM) * Math.random()) * 10000.0d) / 10000.0d; }
                    }
                    (Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(dItem)).setAttribute(as.getAttribute(), v, as.getSlot());
                }
            }
        }
        // Tags
        if (!tags.isEmpty()) {
            CompoundTag tag = dItem.getOrCreateTag();
            for (DropNbtSet dns : tags) {
                if (dns.values.length > 0 && (dns.chance >= 1.0d || dns.chance * addChance / 100.0d < Math.random())) {
                    tag = dns.getConstructorTag(new NBTWrapper(tag)).getMCNBT();
                }
            }
        }
        if (dItem.hasTag()) {
            if (dItem.getTag() != null && dItem.getTag().isEmpty()) { dItem.setTag(null); }
        }
        return dItem;
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
    public Availability getAvailability() { return availability; }

    @Override
    public boolean getTiedToLevel() { return tiedToLevel; }

    @Override
    public void remove() {
        if (parent != null) { parent.removeDrop(this); }
    }

    @Override
    public void removeAttribute(IAttributeSet attribute) { attributes.remove((AttributeSet) attribute); }

    @Override
    public void removeDropNbt(IDropNbtSet nbt) { tags.remove((DropNbtSet) nbt); }

    @Override
    public void removeEnchant(IEnchantSet enchant) { enchants.remove((EnchantSet) enchant); }

    @Override
    public void resetTo(IItemStack itemIn) {
        if (itemIn == null || itemIn.isEmpty()) { return; }
        resetTo(itemIn.getMCItemStack());
    }

    public void resetTo(ItemStack itemIn) {
        if (itemIn == null || itemIn.isEmpty()) { return; }
        double ch = 85.0d;
        lootMode = 0;
        tiedToLevel = false;
        enchants = new ArrayList<>();
        attributes = new ArrayList<>();
        tags = new ArrayList<>();
        amount = new int[] { 1, 1 };
        // Amount
        if (itemIn.getCount() > 1) { amount[1] = itemIn.getCount(); }
        CompoundTag itemNbt = itemIn.getTag() == null ? new CompoundTag() : itemIn.getTag();
        // Enchants
        if (itemNbt.contains("ench", 9) || itemNbt.contains("StoredEnchantments", 9)) {
            String key = itemNbt.contains("ench", 9) ? "ench" : "StoredEnchantments";
            enchants.clear();
            if (!itemNbt.getList(key, 10).isEmpty()) {
                lootMode = 2;
                ch /= itemNbt.getList(key, 10).size();
                for (Tag nbtEnch : itemNbt.getList(key, 10)) {
                    IEnchantSet es = addEnchant(((CompoundTag) nbtEnch).getString("id"));
                    if (es != null) {
                        es.setLevels(0, ((CompoundTag) nbtEnch).getShort("lvl"));
                        es.setChance(85.0d / (double) itemNbt.getList(key, 10).size());
                    }
                }
                itemNbt.remove(key);
            }
        }
        // Attributes
        if (itemNbt.contains("AttributeModifiers")) {
            lootMode = 2;
            ch /= itemNbt.getList("AttributeModifiers", 10).size();
            for (Tag nbtAttr : itemNbt.getList("AttributeModifiers", 10)) {
                IAttributeSet as = addAttribute(((CompoundTag) nbtAttr).getString("AttributeName"));
                if (as != null) {
                    int slot = -1;
                    if (attributeSlotsName.containsKey(((CompoundTag) nbtAttr).getString("Slot"))) { slot = attributeSlotsName.get(((CompoundTag) nbtAttr).getString("Slot")); }
                    as.setSlot(slot);
                    double value = ((CompoundTag) nbtAttr).getDouble("Amount");
                    if (value < 0.0d) { as.setValues(value, 0.0d); }
                    else if (value > 0.0d) { as.setValues(0.0d, value); }
                    else { as.setValues(0.0d, 0.05d); }
                    as.setChance(85.0d / (double) itemNbt.getList("AttributeModifiers", 10).size());
                }
            }
            itemNbt.remove("AttributeModifiers");
        }
        // Chance
        setChance(ch);
        // Simple Item Set
        CompoundTag itemFromNbt = new CompoundTag();
        itemIn.save(itemFromNbt);
        if (!itemNbt.isEmpty()) { itemFromNbt.put("tag", itemNbt); }
        ItemStack newItem = ItemStack.of(itemFromNbt);
        newItem.setCount(1);
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
        if (newMin < 1) { newMin = 1; }
        if (newMin > item.getMaxStackSize()) { newMin = item.getMaxStackSize(); }
        if (newMax < newMin) { newMax = newMin; }
        if (newMax > item.getMaxStackSize()) { newMax = item.getMaxStackSize(); }
        amount[0] = newMin;
        amount[1] = newMax;
    }

    @Override
    public void setChance(double chanceIn) { chance = Math.round(ValueUtil.correctDouble(chanceIn, 0.0001d, 100.0d) * 10000.0d) / 10000.0d; }

    @Override
    public void setItem(IItemStack itemIn) { item = itemIn.getMCItemStack(); }

    @Override
    public void setLootMode(int mode) { lootMode = mode % 3; }

    @Override
    public void setTiedToLevel(boolean tied) { tiedToLevel = tied; }

    public Component getKey() {
        if (item == null) { return Component.literal("null"); }
        if (item.isEmpty()) { return Component.translatable("type.empty"); }
        MutableComponent keyName = Component.empty()
                .append(Component.literal((pos + 1) + ": ").withStyle(ChatFormatting.GRAY));
        double ch = Math.round(chance * 10.0d) / 10.d;
        String chance = String.valueOf(ch).replace(".", ",");
        if (ch == (int) ch) { chance = String.valueOf((int) ch); }
        chance += "%";
        keyName.append(Component.literal(chance).withStyle(ChatFormatting.YELLOW));
        if (amount[0] == amount[1]) {
            keyName.append(Component.literal("[").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("" + amount[0]).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("]").withStyle(ChatFormatting.GRAY));
        }
        else {
            keyName.append(Component.literal("[").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("" + amount[0]).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("<>").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("" + amount[1]).withStyle(ChatFormatting.GOLD))
                            .append(Component.literal("]").withStyle(ChatFormatting.GRAY)));
        }
        MutableComponent effs = Component.empty();
        if (!enchants.isEmpty()) {
            effs.append(Component.literal(" |").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("E").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("|").withStyle(ChatFormatting.GRAY));
        }
        if (!attributes.isEmpty()) {
            effs.append(Component.literal(" |").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("A").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("|").withStyle(ChatFormatting.GRAY));
        }
        if (!tags.isEmpty()) {
            effs.append(Component.literal(" |").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("T").withStyle(ChatFormatting.RED))
                    .append(Component.literal("|").withStyle(ChatFormatting.GRAY));
        }
        keyName.append(effs)
                .append(((MutableComponent) item.getDisplayName()).withStyle(ChatFormatting.RESET));
        if (pos < 0) {
            keyName.append(Component.literal("ID:" + toString().substring(toString().indexOf("@") + 1))
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        return keyName;
    }

    @SuppressWarnings("deprecation")
    public List<Component> getHover(boolean isReward) {
        List<Component> list = new ArrayList<>();
        // pos
        if (pos < 0) {
            list.add(Component.empty()
                    .append(Component.translatable("gui.position").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(toString().substring(toString().indexOf("@") + 1)).withStyle(ChatFormatting.DARK_GRAY)));
        }
        else {
            list.add(Component.empty()
                    .append(Component.translatable("gui.position").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("" + pos).withStyle(ChatFormatting.RESET)));
        }
        // stack
        MutableComponent stackKey = Component.empty()
                .append(Component.translatable("gui.name").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
        if (item == null) { stackKey.append(Component.literal("null").withStyle(ChatFormatting.DARK_RED)); }
        else if (item.isEmpty()) { stackKey.append(Component.translatable("type.empty").withStyle(ChatFormatting.RED)); }
        else { stackKey.append(Component.literal("" + BuiltInRegistries.ITEM.getKey(item.getItem())).withStyle(ChatFormatting.RESET)); }
        list.add(stackKey);
        // amount
        MutableComponent amountKey = Component.empty()
                .append(Component.translatable("quest.itemamount").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
        if (amount[0] == amount[1]) { amountKey.append(Component.literal("" + amount[0]).withStyle(ChatFormatting.GOLD)); }
        else {
            amountKey.append(Component.literal("[min:").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("" + amount[0]).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("; max:").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("" + amount[1]).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("]").withStyle(ChatFormatting.GRAY));
        }
        list.add(amountKey);
        // chance
        MutableComponent chanceKey = Component.empty()
                .append(Component.translatable("drop.chance").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
        if (chance == (int) chance) {
            chanceKey.append(Component.literal("" + (int) chance).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("%").withStyle(ChatFormatting.GRAY));
        }
        else {
            chanceKey.append(Component.literal(("" + chance).replace(".", ",")).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("%").withStyle(ChatFormatting.GRAY));
        }
        list.add(chanceKey);
        // loot mode
        list.add(Component.empty()
                .append(Component.translatable("inv.lootpickup").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("inv.lootmode." + (lootMode % 3) + "." + isReward).withStyle(ChatFormatting.RESET)));
        // availability
        list.add(Component.empty()
                .append(Component.translatable("availability.available").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("availability." + (availability.hasOptions() ? "contains" : "except")).withStyle(ChatFormatting.RESET)));
        // enchants
        if (!enchants.isEmpty()) {
            MutableComponent enchKey = Component.empty()
                    .append(Component.translatable("drop.enchants").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": [").withStyle(ChatFormatting.GRAY));
            boolean start = false;
            for (EnchantSet es : enchants) {
                if (start) { enchKey.append(Component.literal(", ").withStyle(ChatFormatting.GRAY)); }
                if (es.ench == null) { enchKey.append(Component.literal("null").withStyle(ChatFormatting.GRAY)); }
                else {
                    enchKey.append(Component.literal("id: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("" + BuiltInRegistries.ENCHANTMENT.getId(es.ench)).withStyle(ChatFormatting.AQUA));
                }
                start = true;
            }
            list.add(enchKey.append(Component.literal("]").withStyle(ChatFormatting.GRAY)));
        }
        else {
            list.add(Component.empty()
                    .append(Component.translatable("drop.enchants").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable("availability.except").withStyle(ChatFormatting.RESET)));
        }
        // attributes
        if (!attributes.isEmpty()) {
            MutableComponent attrKey = Component.empty()
                    .append(Component.translatable("drop.attributes").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": [").withStyle(ChatFormatting.GRAY));
            boolean start = false;
            for (AttributeSet as : attributes) {
                if (start) { attrKey.append(Component.literal(", ").withStyle(ChatFormatting.GRAY)); }
                if (as.attr == null) { attrKey.append(Component.literal("null").withStyle(ChatFormatting.GRAY)); }
                else {
                    attrKey.append(Component.literal("id: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(as.attr.getDescriptionId()).withStyle(ChatFormatting.BLUE));
                }
                start = true;
            }
            list.add(attrKey.append(Component.literal("]").withStyle(ChatFormatting.GRAY)));
        }
        else {
            list.add(Component.empty()
                    .append(Component.translatable("drop.attributes").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable("availability.except").withStyle(ChatFormatting.RESET)));
        }
        // tags
        if (!tags.isEmpty()) {
            MutableComponent nbtKey = Component.empty()
                    .append(Component.translatable("drop.tags").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": [").withStyle(ChatFormatting.GRAY));
            boolean start = false;
            for (DropNbtSet ns : tags) {
                if (start) { nbtKey.append(Component.literal(", ").withStyle(ChatFormatting.GRAY)); }
                if (ns.path == null) { nbtKey.append(Component.literal("null").withStyle(ChatFormatting.GRAY)); }
                else {
                    nbtKey.append(Component.literal("id: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(ns.path).withStyle(ChatFormatting.BLUE));
                }
                start = true;
            }
            list.add(nbtKey.append(Component.literal("]").withStyle(ChatFormatting.GRAY)));
        }
        else {
            list.add(Component.empty()
                    .append(Component.translatable("drop.tags").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable("availability.except").withStyle(ChatFormatting.RESET)));
        }
        return list;
    }

    @Override
    public int getContainerSize() { return 1; }

    @Override
    public boolean isEmpty() { return item.isEmpty(); }

    @Override
    public @NotNull ItemStack getItem(int slotId) { return item; }

    @Override
    public @NotNull ItemStack removeItem(int slotId, int count) {
        ItemStack itemStack = ItemStack.EMPTY;
        if (!item.isEmpty()) {
            if (item.getCount() <= count) {
                itemStack = item.copy();
                item = ItemStack.EMPTY;
            } else {
                itemStack = item.split(count);
                if (item.getCount() == 0) { item = ItemStack.EMPTY; }
            }
        }
        return itemStack;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slotId) {
        ItemStack stack = item;
        item = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void setItem(int slotId, @NotNull ItemStack itemStack) { item = itemStack; }

    @Override
    public void setChanged() { CustomNpcs.proxy.init(); }

    @Override
    public boolean stillValid(@NotNull Player player) { return true; }

    @Override
    public boolean canPlaceItem(int slotId, @NotNull ItemStack itemstack) { return true; }

    @Override
    public void startOpen(@NotNull Player player) { }

    @Override
    public void stopOpen(@NotNull Player player) { }

    @Override
    public void clearContent() { }

    public void load(CompoundTag nbtDS) {
        item = ItemStack.of(nbtDS.getCompound("Item"));
        chance = nbtDS.getDouble("Chance");
        if (nbtDS.contains("LootMode", 1)) { lootMode = nbtDS.getBoolean("LootMode") ? 1 : 0;}
        else if (nbtDS.contains("LootMode", 3)) { lootMode = nbtDS.getInt("LootMode");}
        tiedToLevel = nbtDS.getBoolean("TiedToLevel");
        if (nbtDS.contains("Availability", 10)) { availability.load(nbtDS.getCompound("Availability")); }
        else if (nbtDS.contains("Availability", 10)) { // OLD
            availability.clear();
            int questId = nbtDS.getInt("QuestId");
            if (questId > 0) { availability.setQuest(questId, EnumAvailabilityQuest.Active.ordinal()); }
        }
        int[] cnts = nbtDS.getIntArray("Amount");
        if (nbtDS.contains("Amount", 9)) {
            cnts = new int[2];
            for (int i = 0; i < 2; i++) { cnts[i] = nbtDS.getList("Amount", 3).getInt(i); }
        }
        if (cnts.length != 2) {
            int m = 1, n = 1;
            if (cnts.length >= 1) { m = cnts[0]; }
            if (cnts.length >= 2) { n = cnts[1]; }
            cnts = new int[] { m, n };
        }
        List<EnchantSet> ench = new ArrayList<>();
        for (Tag ne : nbtDS.getList("EnchantSettings", 10)) {
            EnchantSet es = new EnchantSet(this);
            es.load((CompoundTag) ne);
            ench.add(es);
        }
        enchants = ench;
        List<AttributeSet> attr = new ArrayList<>();
        for (Tag na : nbtDS.getList("AttributeSettings", 10)) {
            AttributeSet as = new AttributeSet(this);
            as.load((CompoundTag) na);
            attr.add(as);
        }
        attributes = attr;
        List<DropNbtSet> tgsl = new ArrayList<>();
        for (Tag na : nbtDS.getList("TagSettings", 10)) {
            DropNbtSet ts = new DropNbtSet(this);
            ts.load((CompoundTag) na);
            tgsl.add(ts);
        }
        tags = tgsl;
        pos = nbtDS.getInt("Slot");
        setAmount(cnts[0], cnts[1]);
    }

    public CompoundTag save() {
        CompoundTag nbtDS = new CompoundTag();
        nbtDS.put("Item", item.save(new CompoundTag()));
        nbtDS.putDouble("Chance", chance);
        nbtDS.putInt("LootMode", lootMode);
        nbtDS.putBoolean("TiedToLevel", tiedToLevel);
        nbtDS.put("Availability", availability.save(new CompoundTag()));
        nbtDS.putIntArray("Amount", amount);
        ListTag ench = new ListTag();
        for (EnchantSet es : enchants) { ench.add(es.getNBT()); }
        nbtDS.put("EnchantSettings", ench);
        ListTag attr = new ListTag();
        for (AttributeSet as : attributes) { attr.add(as.getNBT()); }
        nbtDS.put("AttributeSettings", attr);
        ListTag tgsl = new ListTag();
        for (DropNbtSet ts : tags) { tgsl.add(ts.getNBT()); }
        nbtDS.put("TagSettings", tgsl);
        nbtDS.putInt("Slot", pos);
        return nbtDS;
    }

    public DropSet copy() {
        DropSet drop = new DropSet(parent);
        drop.load(save());
        return drop;
    }

}
