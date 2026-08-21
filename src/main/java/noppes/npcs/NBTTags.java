package noppes.npcs;

import java.util.*;
import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.util.Util;

public class NBTTags {

   public static void getItemStackList(ListTag tagList, NpcMiscInventory inv) {
      inv.clearContent();
      for(int i = 0; i < tagList.size() && i < inv.getContainerSize(); ++i) {
         CompoundTag nbtStack = tagList.getCompound(i);
         int slotId;
         if (nbtStack.contains("Slot", 3)) { slotId = nbtStack.getInt("Slot"); }
         else if (nbtStack.contains("Slot", 1)) { slotId = nbtStack.getByte("Slot") & 0xFF; }
         else { continue; }
         if (slotId >= 0 && slotId < inv.getContainerSize()) { inv.setItem(slotId, ItemStack.of(nbtStack)); }
      }
   }

   public static HashMap<Integer, IItemStack> getIItemStackMap(ListTag tagList) {
      HashMap<Integer, IItemStack> map = new HashMap<>();
      NpcAPI api = NpcAPI.Instance();
      for(int i = 0; i < tagList.size(); ++i) {
         CompoundTag nbtStack = tagList.getCompound(i);
         int slotId;
         if (nbtStack.contains("Slot", 3)) { slotId = nbtStack.getInt("Slot"); }
         else if (nbtStack.contains("Slot", 1)) { slotId = nbtStack.getByte("Slot") & 0xFF; }
         else { continue; }
         IItemStack iStack = api != null ? api.getIItemStack(ItemStack.of(nbtStack)) : ItemStackWrapper.AIR;
         map.put(slotId, iStack);
      }
      return map;
   }

   public static NonNullList<Ingredient> getIngredientList(ListTag tagList) {
      NonNullList<Ingredient> list = NonNullList.create();
      for(int i = 0; i < tagList.size(); ++i) {
         CompoundTag compound = tagList.getCompound(i);
         Ingredient ingredients;
         Tag tag = compound.get("Ingredients");
         if (tag instanceof ListTag ingredientsNBT && ingredientsNBT.getElementType() == (byte) 10) {
            List<ItemStack> ings = new ArrayList<>();
            for (int j = 0; j < ingredientsNBT.size(); j++) { ings.add(ItemStack.of(ingredientsNBT.getCompound(j))); }
            ingredients = Ingredient.of(ings.toArray(new ItemStack[0]));
         }
         else { ingredients = Ingredient.of(ItemStack.of(compound)); }
         list.add(compound.getByte("Slot") & 0xFF, ingredients);
      }
      return list;
   }

   public static ArrayList<int[]> getIntegerArraySet(ListTag tagList) {
      ArrayList<int[]> set = new ArrayList<>();
      for(int i = 0; i < tagList.size(); ++i) { set.add(tagList.getCompound(i).getIntArray("Array")); }
      return set;
   }

   public static HashMap<Integer, Boolean> getBooleanList(ListTag tagList) {
      HashMap<Integer, Boolean> list = new HashMap<>();
      for(int i = 0; i < tagList.size(); ++i) {
         CompoundTag compound = tagList.getCompound(i);
         list.put(compound.getInt("Slot"), compound.getBoolean("Boolean"));
      }
      return list;
   }

   public static HashMap<Integer, Integer> getIntegerIntegerMap(ListTag tagList) {
      HashMap<Integer, Integer> list = new HashMap<>();
      for(int i = 0; i < tagList.size(); ++i) {
         CompoundTag compound = tagList.getCompound(i);
         list.put(compound.getInt("Slot"), compound.getInt("Integer"));
      }
      return list;
   }

   public static HashMap<Integer, Long> getIntegerLongMap(ListTag tagList) {
      HashMap<Integer, Long> list = new HashMap<>();
      for(int i = 0; i < tagList.size(); ++i) {
         CompoundTag compound = tagList.getCompound(i);
         list.put(compound.getInt("Slot"), compound.getLong("Long"));
      }
      return list;
   }

   public static HashSet<Integer> getIntegerSet(ListTag tagList) {
      HashSet<Integer> list = new HashSet<>();
      for(int i = 0; i < tagList.size(); ++i) { list.add(tagList.getCompound(i).getInt("Integer")); }
      return list;
   }

   public static List<Integer> getIntegerList(ListTag tagList) {
      List<Integer> list = new ArrayList<>();
      for(int i = 0; i < tagList.size(); ++i) { list.add(tagList.getCompound(i).getInt("Integer")); }
      return list;
   }

   public static HashMap<Integer, String> getIntegerStringMap(ListTag tagList) {
      HashMap<Integer, String> list = new HashMap<>();
      for(int i = 0; i < tagList.size(); ++i) {
         CompoundTag compound = tagList.getCompound(i);
         list.put(compound.getInt("Slot"), compound.getString("Value"));
      }
      return list;
   }

   public static HashMap<String, Integer> getStringIntegerMap(ListTag tagList) {
      HashMap<String, Integer> list = new HashMap<>();
      for(int i = 0; i < tagList.size(); ++i) {
         CompoundTag compound = tagList.getCompound(i);
         list.put(compound.getString("Slot"), compound.getInt("Value"));
      }
      return list;
   }

   public static List<String> getStringList(ListTag tagList) {
      List<String> list = new ArrayList<>();
      for(int i = 0; i < tagList.size(); ++i) { list.add(tagList.getCompound(i).getString("Line")); }
      return list;
   }

   public static List<ResourceLocation> getResourceLocationList(ListTag tagList) {
      List<ResourceLocation> list = new ArrayList<>();
      for(int i = 0; i < tagList.size(); ++i) {
         CompoundTag compound = tagList.getCompound(i);
         ResourceLocation line = ResourceLocation.tryParse(compound.getString("Line"));
         list.add(line);
      }
      return list;
   }

   public static ListTag nbtIntegerArraySet(List<int[]> set) {
      ListTag nbtList = new ListTag();
      if (set != null) {
         for (int[] arr : set) {
            CompoundTag compound = new CompoundTag();
            compound.putIntArray("Array", arr);
            nbtList.add(compound);
         }
      }
      return nbtList;
   }

   public static ListTag nbtItemStackList(NonNullList<ItemStack> inventory) {
      ListTag nbtList = new ListTag();
      for(int slot = 0; slot < inventory.size(); ++slot) {
         ItemStack item = inventory.get(slot);
         if (!item.isEmpty()) {
            CompoundTag compound = new CompoundTag();
            compound.putByte("Slot", (byte)slot);
            item.save(compound);
            nbtList.add(compound);
         }
      }
      return nbtList;
   }

   public static ListTag nbtIItemStackMap(Map<Integer, IItemStack> inventory) {
      ListTag nbtList = new ListTag();
      if (inventory != null) {
         for (Map.Entry<Integer, IItemStack> entry : inventory.entrySet()) {
            if (!NoppesUtilServer.isItemStackNull(entry.getValue().getMCItemStack())) {
               CompoundTag compound = new CompoundTag();
               compound.putByte("Slot", entry.getKey().byteValue());
               entry.getValue().getMCItemStack().save(compound);
               nbtList.add(compound);
            }
         }
      }
      return nbtList;
   }

   public static ListTag nbtIngredientList(NonNullList<Ingredient> inventory) {
      ListTag nbtList = new ListTag();
      if (inventory != null) {
         for (int slot = 0; slot < inventory.size(); ++slot) {
            Ingredient ingredient = inventory.get(slot);
            CompoundTag compound = new CompoundTag();
            compound.putByte("Slot", (byte) slot);
            ListTag ingredients = new ListTag();
            for (ItemStack ing : ingredient.getItems()) { ingredients.add(ing.save(new CompoundTag())); }
            compound.put("Ingredients", ingredients);
            nbtList.add(compound);
         }
      }
      return nbtList;
   }

   public static ListTag nbtIntegerIntegerMap(Map<Integer, Integer> lines) {
      ListTag nbtList = new ListTag();
      if (lines != null) {
         for (int slot : lines.keySet()) {
            CompoundTag compound = new CompoundTag();
            compound.putInt("Slot", slot);
            compound.putInt("Integer", lines.get(slot));
            nbtList.add(compound);
         }
      }
      return nbtList;
   }

   public static ListTag nbtIntegerLongMap(HashMap<Integer, Long> lines) {
      ListTag nbtList = new ListTag();
      if (lines != null) {
         for (int slot : lines.keySet()) {
            CompoundTag compound = new CompoundTag();
            compound.putInt("Slot", slot);
            compound.putLong("Long", lines.get(slot));
            nbtList.add(compound);
         }
      }
      return nbtList;
   }

   public static ListTag nbtIntegerCollection(Collection<Integer> set) {
      ListTag nbtList = new ListTag();
      if (set != null) {
         for (int slot : set) {
            CompoundTag compound = new CompoundTag();
            compound.putInt("Integer", slot);
            nbtList.add(compound);
         }
      }
      return nbtList;
   }

   public static Tag nbtIntegerStringMap(Map<Integer, String> map) {
      ListTag nbtList = new ListTag();
      if (map != null) {
         for (int slot : map.keySet()) {
            CompoundTag compound = new CompoundTag();
            compound.putInt("Slot", slot);
            compound.putString("Value", map.get(slot));
            nbtList.add(compound);
         }
      }
      return nbtList;
   }

   @SuppressWarnings("unused")
   public static IntArrayTag nbtIntegerList(List<Integer> list) {
      int[] data = new int[0];
      if (list != null) {
         data = new int[list.size()];
         int i = 0;
         for (int s : list) { data[i++] = s; }
      }
      return new IntArrayTag(data);
   }

   public static ListTag nbtStringList(List<String> list) {
      ListTag nbtList = new ListTag();
      if (list != null) {
         for (String s : list) {
            CompoundTag compound = new CompoundTag();
            compound.putString("Line", s);
            nbtList.add(compound);
         }
      }
      return nbtList;
   }

   public static ListTag nbtResourceLocationList(List<ResourceLocation> list) {
      ListTag nbtList = new ListTag();
      for (ResourceLocation s : list) {
         CompoundTag compound = new CompoundTag();
         compound.putString("Line", s.toString());
         nbtList.add(compound);
      }
      return nbtList;
   }

   public static ListTag nbtDoubleList(double... doubles) {
      ListTag nbtList = new ListTag();
      if ( doubles != null) {
         for (double d1 : doubles) { nbtList.add(DoubleTag.valueOf(d1)); }
      }
      return nbtList;
   }

   public static CompoundTag nbtMerge(CompoundTag data, CompoundTag merge) {
      CompoundTag compound = data.copy();
      for (String name : merge.getAllKeys()) {
         Tag tag = merge.get(name);
         if (tag != null) {
            if ( tag.getId() == 10) { tag = nbtMerge(compound.getCompound(name), (CompoundTag) tag); }
            compound.put(name, tag);
         }
      }
      return compound;
   }

   public static List<ScriptContainer> getScript(ListTag list, IScriptHandler handler) {
      List<ScriptContainer> scripts = new ArrayList<>();
      for(int i = 0; i < list.size(); ++i) {
         CompoundTag compound = list.getCompound(i);
         ScriptContainer script = new ScriptContainer(handler);
         script.load(compound);
         scripts.add(script);
      }
      return scripts;
   }

   public static ListTag nbtScript(List<ScriptContainer> scripts) {
      ListTag nbtList = new ListTag();
      for (ScriptContainer script : scripts) {
         CompoundTag compound = new CompoundTag();
         script.save(compound);
         nbtList.add(compound);
      }
      return nbtList;
   }

   public static TreeMap<Long, String> getLongStringMap(ListTag tagList) {
      TreeMap<Long, String> map = new TreeMap<>();
      for(int i = 0; i < tagList.size(); ++i) {
         CompoundTag compound = tagList.getCompound(i);
         long time = compound.getLong("Long");
         if (compound.contains("String", 8)) {
            if (!compound.getString("String").isEmpty()) { map.put(time, compound.getString("String")); }
         } // OLD
         else {
            Tag tag = compound.get("String");
            if (tag instanceof ListTag list && list.getElementType() == 8) {
               StringBuilder totalStr = new StringBuilder();
               for (int j = 0; j < list.size(); j++) { totalStr.append(list.getString(j)); }
               if (!totalStr.isEmpty()) { map.put(time, totalStr.toString()); }
            }
         } // NEW
      }
      return map;
   }

   public static ListTag nbtLongStringMap(Map<Long, String> map) {
      ListTag nbtList = new ListTag();
      if (map != null) {
         for (long slot : map.keySet()) {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("Long", slot);
            List<String> content = Util.splitString(map.get(slot));
            if (content.size() < 2) { nbt.putString("String", map.get(slot)); } // OLD
            else {
               ListTag list = new ListTag();
               for (String line : content) { list.add(StringTag.valueOf(line)); }
               nbt.put("String", list);
            } // NEW
            nbtList.add(nbt);
         }
      }
      return nbtList;
   }

   // New from Unofficial (BetaZavr)
   public static boolean compareNBT(CompoundTag compound1, CompoundTag compound2) {
      if (compound1.getAllKeys().size() != compound2.getAllKeys().size()) { return false; }
      for (String key : compound1.getAllKeys()) {
         Tag tag1 = compound1.get(key);
         Tag tag2 = compound2.get(key);
         if (tag1 == null || tag2 == null || notCompareNBTBase(tag1, tag2)) { return false; }
      }
      return true;
   }

   private static boolean notCompareNBTBase(Tag tag1, Tag tag2) {
      if (tag1.getId() != tag2.getId()) { return true; }
      return switch (tag1.getId()) {
         case 1 -> // TAG_BYTE
                 ((ByteTag) tag1).getAsByte() != ((ByteTag) tag2).getAsByte();
         case 2 -> // TAG_SHORT
                 ((ShortTag) tag1).getAsShort() != ((ShortTag) tag2).getAsShort();
         case 3 -> // TAG_INT
                 ((IntTag) tag1).getAsInt() != ((IntTag) tag2).getAsInt();
         case 4 -> // TAG_LONG
                 ((LongTag) tag1).getAsLong() != ((LongTag) tag2).getAsLong();
         case 5 -> // TAG_FLOAT
                 Float.floatToIntBits(((FloatTag) tag1).getAsFloat()) != Float.floatToIntBits(((FloatTag) tag2).getAsFloat());
         case 6 -> // TAG_DOUBLE
                 Double.doubleToLongBits(((DoubleTag) tag1).getAsDouble()) != Double.doubleToLongBits(((DoubleTag) tag2).getAsDouble());
         case 7 -> // TAG_BYTE_ARRAY
                 !Arrays.equals(((ByteArrayTag) tag1).getAsByteArray(), ((ByteArrayTag) tag2).getAsByteArray());
         case 8 -> // TAG_STRING
                 !tag1.getAsString().equals(tag2.getAsString());
         case 9 -> // TAG_LIST
                 notCompareNBTLists((ListTag) tag1, (ListTag) tag2);
         case 10 -> // TAG_COMPOUND
                 !compareNBT((CompoundTag) tag1, (CompoundTag) tag2);
         case 11 -> // TAG_INT_ARRAY
                 !Arrays.equals(((IntArrayTag) tag1).getAsIntArray(), ((IntArrayTag) tag2).getAsIntArray());
         case 12 -> // TAG_LONG_ARRAY
                 !Arrays.equals(((LongArrayTag) tag1).getAsLongArray(), ((LongArrayTag) tag2).getAsLongArray());
         default -> true;
      };
   }

   private static boolean notCompareNBTLists(ListTag list1, ListTag list2) {
      if (list1.size() != list2.size()) { return true; }
      for (int i = 0; i < list1.size(); i++) {
         if (notCompareNBTBase(list1.get(i), list2.get(i))) { return true; }
      }
      return false;
   }

}
