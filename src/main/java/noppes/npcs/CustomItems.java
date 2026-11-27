package noppes.npcs;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.items.*;
import noppes.npcs.items.custom.*;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.ModData;
import noppes.npcs.util.NBTJsonUtil;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

public class CustomItems {

    public static ItemNpcWand wand;
    public static ItemNpcCloner cloner;
    public static ItemNpcScripter scripter;
    public static ItemNpcMovingPath moving;
    public static ItemMounter mount;
    public static Item teleporter;
    public static ItemScripted scripter_item;
    public static ItemNbtBook nbt_book;
    public static ItemSoulstoneEmpty soulstoneEmpty;
    public static ItemSoulstoneFilled soulstoneFull;

    // New from Unofficial (BetaZavr)
    public static ItemBoundary npcboundary;
    public static ItemBuilder npcbuilder;
    public static ItemRemover npcremover;
    public static ItemPlacer npcplacer;
    public static ItemReplacer npcreplacer;
    public static ItemSaver npcsaver;
    // new
    public static ItemPotion itemPotion = null;
    public static ItemPotion itemSplashPotion = null;
    public static ItemPotion itemLingeringPotion = null;
    public static ItemTippedArrow itemTippedArrow = null;

    // custom
    public static List<ICustomElement> customitems = new ArrayList<>();

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        List<Item> items = new ArrayList<>();
        items.add(CustomTabs.TOOLS.item = wand = new ItemNpcWand());
        items.add(cloner = new ItemNpcCloner());
        items.add(scripter = new ItemNpcScripter());
        items.add(moving = new ItemNpcMovingPath());
        items.add(mount = new ItemMounter());
        items.add(teleporter = new ItemTeleporter());
        items.add(soulstoneEmpty = new ItemSoulstoneEmpty());
        items.add(soulstoneFull = new ItemSoulstoneFilled());
        items.add(nbt_book = new ItemNbtBook());
        items.add(npcboundary = new ItemBoundary());
        items.add(npcbuilder = new ItemBuilder());
        items.add(npcremover = new ItemRemover());
        items.add(npcplacer = new ItemPlacer());
        items.add(npcreplacer = new ItemReplacer());
        items.add(npcsaver = new ItemSaver());
        items.add(CustomTabs.ITEMS.item = scripter_item = new ItemScripted());
        List<String> names = new ArrayList<>();
        for (Item it : items) { names.add(Objects.requireNonNull(it.getRegistryName()).toString()); }
        // Custom Items
        File itemsFile = new File(CustomNpcs.Dir, "custom_items.js");
        NBTTagCompound nbtItems = getItemsNbt(itemsFile);
        boolean resave = nbtItems.getBoolean("resave");
        nbtItems.removeTag("resave");

        for (int i = 0; i < nbtItems.getTagList("Items", 10).tagCount(); i++) {
            NBTTagCompound nbtItem = nbtItems.getTagList("Items", 10).getCompoundTagAt(i);
            if (!nbtItem.hasKey("RegistryName", 8) || !nbtItem.hasKey("ItemType", 1)
                    || nbtItem.getString("RegistryName").isEmpty() || nbtItem.getByte("ItemType") < (byte) 0
                    || nbtItem.getByte("ItemType") > (byte) 8) {
                LogWriter.error("Attempt to load item pos: " + i + "; name: \"" + nbtItem.getString("RegistryName") + "\" - failed");
                continue;
            }
            String preName = nbtItem.getString("RegistryName");
            String name = NoppesUtilServer.validPath(preName);
            if (!preName.equals(name)) {
                nbtItem.setString("RegistryName", name);
                resave = true;
            }
            if (nbtItem.hasKey("CollectionBlocks", 9)) {
                NBTTagList list = nbtItem.getTagList("CollectionBlocks", 8);
                NBTTagList newList = new NBTTagList();
                boolean isChanged = false;
                for (int j = 0; j < list.tagCount(); j++) {
                    String n = list.getStringTagAt(i);
                    String v = NoppesUtilServer.validPath(n);
                    if (!n.equals(v)) { isChanged = true; }
                    newList.appendTag(new NBTTagString(v));
                }
                if (isChanged) {
                    resave = true;
                    nbtItem.setTag("CollectionBlocks", newList);
                }
            }
            String location = CustomNpcs.MODID + ":custom_" + name;
            switch (nbtItem.getByte("ItemType")) {
                case (byte) 1: {
                    registryItem(location, new CustomWeapon(CustomItem.getMaterialTool(nbtItem), nbtItem), names, items,
                            nbtItem.getBoolean("CreateDefaultFiles"));
                    break;
                } // Weapon
                case (byte) 2: {
                    Set<Block> effectiveBlocks = new HashSet<>();
                    if (nbtItem.hasKey("CollectionBlocks", 9)) {
                        for (int j = 0; j < nbtItem.getTagList("CollectionBlocks", 8).tagCount(); j++) {
                            Block block = Block
                                    .getBlockFromName(nbtItem.getTagList("CollectionBlocks", 8).getStringTagAt(j));
                            if (block != null) {
                                effectiveBlocks.add(block);
                            }
                        }
                    }
                    Item item;
                    float damage = (float) nbtItem.getDouble("EntityDamage");
                    float speed = (float) nbtItem.getDouble("SpeedAttack");
                    Item.ToolMaterial material = CustomItem.getMaterialTool(nbtItem);
                    switch (nbtItem.getString("ToolClass").toLowerCase()) {
                        case "axe": item = new CustomAxe(damage, speed, material, effectiveBlocks, nbtItem); break;
                        case "hoe": item = new CustomHoe(damage, speed, material, effectiveBlocks, nbtItem); break;
                        case "shovel": item = new CustomShovel(damage, speed, material, effectiveBlocks, nbtItem); break;
                        default: item = new CustomPickaxe(damage, speed, material, effectiveBlocks, nbtItem); break;
                    }
                    registryItem(location, item, names, items, nbtItem.getBoolean("CreateDefaultFiles"));
                    break;
                } // Tool
                case (byte) 3: {
                    ItemArmor.ArmorMaterial mat = CustomArmor.getMaterialArmor(nbtItem.getString("Material"));
                    for (int a = 0; a < nbtItem.getTagList("EquipmentSlots", 10).tagCount(); a++) {
                        NBTTagCompound part = nbtItem.getTagList("EquipmentSlots", 10).getCompoundTagAt(a);
                        EntityEquipmentSlot slot = CustomArmor.getSlotEquipment(part.getString("Slot"));
                        int rx = 2;
                        if (part.hasKey("RenderIndex", 3)) {
                            rx = nbtItem.getInteger("RenderIndex") % 5;
                            if (rx < 0) { rx *= -1; }
                        }
                        int maxStackDamage = ValueUtil.correctInt(part.getInteger("MaxStackDamage"), 0, Integer.MAX_VALUE);
                        int defense = ValueUtil.correctInt(part.getInteger("Defense"), 0, Integer.MAX_VALUE);
                        int enchantability = ValueUtil.correctInt(part.getInteger("Enchantability"), 0, Integer.MAX_VALUE);
                        float toughness = ValueUtil.correctFloat(part.getFloat("Toughness"), 0.0f, Float.MAX_VALUE);
                        ItemArmor armor = nbtItem.hasKey("OBJData", 10) ?
                                new CustomArmorOBJ(mat, rx, slot, maxStackDamage, defense, enchantability, toughness, nbtItem) :
                                new CustomArmor(mat, rx, slot, maxStackDamage, defense, enchantability, toughness, nbtItem);
                        registryItem(location + "_" + slot.getName().toLowerCase(), armor, names, items, nbtItem.getBoolean("CreateDefaultFiles"));
                    }
                    break;
                } // Armor
                case (byte) 4: {
                    registryItem(location, new CustomShield(nbtItem), names, items, nbtItem.getBoolean("CreateDefaultFiles"));
                    break;
                } // Shield
                case (byte) 5: {
                    registryItem(location, new CustomBow(nbtItem), names, items, nbtItem.getBoolean("CreateDefaultFiles"));
                    break;
                } // Bow
                case (byte) 6: {
                    registryItem(location, new CustomFood(nbtItem.getInteger("HealAmount"),
                                    nbtItem.getFloat("SaturationModifier"), nbtItem.getBoolean("IsWolfFood"), nbtItem),
                            names, items, nbtItem.getBoolean("CreateDefaultFiles"));
                    break;
                } // Food
                case (byte) 7: { continue; } // Potion
                case (byte) 8: {
                    registryItem(location, new CustomFishingRod(nbtItem), names, items, nbtItem.getBoolean("CreateDefaultFiles"));
                    break;
                } // Fishing Rod
                default: {
                    registryItem(location, new CustomItem(nbtItem), names, items, nbtItem.getBoolean("CreateDefaultFiles"));
                    break;
                }// 0: Simple
            }
            if (nbtItem.getBoolean("CreateDefaultFiles")) {
                nbtItem.removeTag("CreateDefaultFiles");
                resave = true;
            }
        }
        if (resave) { Util.instance.saveFile(itemsFile, nbtItems); }
        event.getRegistry().registerAll(items.toArray(new Item[0]));
        registerDispenser();
    }

    public static NBTTagCompound getItemsNbt(File file) {
        NBTTagCompound nbtInFile = new NBTTagCompound();
        NBTTagCompound compound = ModData.getExampleItems().copy();
        try {
            if (file.exists()) { nbtInFile = NBTJsonUtil.LoadFile(file); }
        }
        catch (Exception e) { LogWriter.error("Try Load " + file.getName() + ": ", e); }

        List<String> names = new ArrayList<>();
        NBTTagList listInFile = nbtInFile.getTagList("Items", 10);
        NBTTagList listItems = compound.getTagList("Items", 10);
        NBTTagList exampleItems = listItems.copy();
        boolean resave = false;
        for (int i = 0; i < listInFile.tagCount(); i++) {
            NBTTagCompound nbtItem = listInFile.getCompoundTagAt(i);
            String name = nbtItem.getString("RegistryName");
            boolean isExample = false;
            for (int j = 0; j < exampleItems.tagCount(); j++) {
                if (name.equals(exampleItems.getCompoundTagAt(j).getString("RegistryName"))) {
                    isExample = true;
                    break;
                }
            }
            if (names.contains(name)) {
                if (isExample) {
                    name = name.replace("example", "custom");
                    isExample = false;
                }
                while (names.contains(name)) { name += "_"; }
                nbtItem.setString("RegistryName", name);
                resave = true;
            }
            names.add(name);
            if (!isExample) { listItems.appendTag(nbtItem); }
        }
        compound.setBoolean("resave", resave);
        return compound;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(wand, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcwand", "inventory"));
        ModelLoader.setCustomModelResourceLocation(cloner, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcmobcloner", "inventory"));
        ModelLoader.setCustomModelResourceLocation(scripter, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcscripter", "inventory"));
        ModelLoader.setCustomModelResourceLocation(moving, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcmovingpath", "inventory"));
        ModelLoader.setCustomModelResourceLocation(mount, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcmounter", "inventory"));
        ModelLoader.setCustomModelResourceLocation(teleporter, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcteleporter", "inventory"));
        ModelLoader.setCustomModelResourceLocation(soulstoneEmpty, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcsoulstoneempty", "inventory"));
        ModelLoader.setCustomModelResourceLocation(soulstoneFull, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcsoulstonefilled", "inventory"));
        ModelLoader.setCustomModelResourceLocation(scripter_item, 0, new ModelResourceLocation(CustomNpcs.MODID + ":scripted_item", "inventory"));
        ModelLoader.setCustomModelResourceLocation(nbt_book, 0, new ModelResourceLocation(CustomNpcs.MODID + ":nbt_book", "inventory"));
        ModelLoader.setCustomModelResourceLocation(npcboundary, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcboundary", "inventory"));
        ModelLoader.setCustomModelResourceLocation(npcbuilder, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcbuilder", "inventory"));
        ModelLoader.setCustomModelResourceLocation(npcremover, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcremover", "inventory"));
        ModelLoader.setCustomModelResourceLocation(npcplacer, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcplacer", "inventory"));
        ModelLoader.setCustomModelResourceLocation(npcreplacer, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcreplacer", "inventory"));
        ModelLoader.setCustomModelResourceLocation(npcsaver, 0, new ModelResourceLocation(CustomNpcs.MODID + ":npcsaver", "inventory"));
        for (ICustomElement element : customitems) {
            if (element instanceof Item) {
                Item item = (Item) element;
                ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
            }
        }
    }

    public static void registerDispenser() {
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(soulstoneFull, new BehaviorDefaultDispenseItem() {
            public @Nonnull ItemStack dispenseStack(@Nonnull IBlockSource source, @Nonnull ItemStack item) {
                EnumFacing enumfacing = source.getBlockState().getValue(BlockDispenser.FACING);
                double x = source.getX() + enumfacing.getFrontOffsetX();
                double z = source.getZ() + enumfacing.getFrontOffsetZ();
                ItemSoulstoneFilled.Spawn(null, item, source.getWorld(), new BlockPos(x, source.getY(), z));
                item.splitStack(1);
                return item;
            }
        });
    }

    private void registryItem(String location, Item item, List<String> names, List<Item> items, boolean defFiles) {
        if (names.contains(location) || Item.getIdFromItem(item) > 0 || !(item instanceof ICustomElement)) {
            LogWriter.error("Attempt to load a registered item \"" + location + "\"");
            return;
        }
        boolean isExample = location.endsWith(":custom_itemexample") || location.endsWith(":custom_weaponexample") ||
                location.endsWith(":custom_armorexample") || location.endsWith(":custom_armorobjexample") ||
                location.endsWith(":custom_shieldexample") || location.endsWith(":custom_bowexample") || location.endsWith(":custom_toolexample") ||
                location.endsWith(":custom_axeexample") || location.endsWith(":custom_foodexample") || location.endsWith(":custom_fishingrodexample");
        if (isExample || defFiles) { CustomNpcs.proxy.createAllFiles((ICustomElement) item); }
        LogWriter.info("Load Custom Item \"" + location + "\"");
        items.add(item);
        customitems.add((ICustomElement) item);
        names.add(location);
        if (location.endsWith(":custom_itemexample") || CustomTabs.ITEMS.item == scripter_item) { CustomTabs.ITEMS.item = item; }
    }

}
