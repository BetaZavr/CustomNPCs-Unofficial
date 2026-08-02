package noppes.npcs;

import java.util.*;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.*;
import net.minecraft.util.NonNullList;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.mixin.item.crafting.IIngredientMixin;
import noppes.npcs.mixin.nbt.INBTTagLongArrayMixin;
import noppes.npcs.util.Util;

public class NBTTags {

	public static void getItemStackList(NBTTagList tagList, NonNullList<ItemStack> inv) {
		inv.clear();
		for (int i = 0; i < tagList.tagCount() && i < inv.size(); ++i) {
			NBTTagCompound nbtStack = tagList.getCompoundTagAt(i);
			int slotId;
			if (nbtStack.hasKey("Slot", 3)) { slotId = nbtStack.getInteger("Slot"); }
			else if (nbtStack.hasKey("Slot", 1)) { slotId = nbtStack.getByte("Slot") & 0xFF; }
			else { continue; }
			if (slotId >= 0 && slotId < inv.size()) { inv.set(slotId, new ItemStack(nbtStack)); }
		}
	}

	public static Map<Integer, IItemStack> getIItemStackMap(NBTTagList tagList) {
		Map<Integer, IItemStack> map = new HashMap<>();
		NpcAPI api = NpcAPI.Instance();
		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound nbtStack = tagList.getCompoundTagAt(i);
			int slotId;
			if (nbtStack.hasKey("Slot", 3)) { slotId = nbtStack.getInteger("Slot"); }
			else if (nbtStack.hasKey("Slot", 1)) { slotId = nbtStack.getByte("Slot") & 0xFF; }
			else { continue; }
			IItemStack iStack = api != null ? api.getIItemStack(new ItemStack(nbtStack)) : ItemStackWrapper.AIR;
			map.put(slotId, iStack);
		}
		return map;
	}

	public static NonNullList<Ingredient> getIngredientList(NBTTagList tagList) {
		NonNullList<Ingredient> list = NonNullList.create();
		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound compound = tagList.getCompoundTagAt(i);
			Ingredient ingredients;
			NBTBase tag = compound.getTag("Ingredients");
			if (tag instanceof NBTTagList && ((NBTTagList) tag).getTagType() == 10) {
				NBTTagList ingredientsNBT = (NBTTagList) tag;
				List<ItemStack> ings = new ArrayList<>();
				for (int j = 0; j < ingredientsNBT.tagCount(); j++) { ings.add(new ItemStack(ingredientsNBT.getCompoundTagAt(j))); }
				ingredients = Ingredient.fromStacks(ings.toArray(new ItemStack[0]));
			}
			else { ingredients = Ingredient.fromStacks(new ItemStack(compound)); }
			list.add(compound.getByte("Slot") & 0xFF, ingredients);
		}
		return list;
	}

	public static ArrayList<int[]> getIntegerArraySet(NBTTagList tagList) {
		ArrayList<int[]> set = new ArrayList<>();
		for (int i = 0; i < tagList.tagCount(); ++i) { set.add(tagList.getCompoundTagAt(i).getIntArray("Array")); }
		return set;
	}

	public static HashMap<Integer, Boolean> getBooleanList(NBTTagList tagList) {
		HashMap<Integer, Boolean> list = new HashMap<>();
		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound compound = tagList.getCompoundTagAt(i);
			list.put(compound.getInteger("Slot"), compound.getBoolean("Boolean"));
		}
		return list;
	}

	public static HashMap<Integer, Integer> getIntegerIntegerMap(NBTTagList tagList) {
		HashMap<Integer, Integer> list = new HashMap<>();
		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound compound = tagList.getCompoundTagAt(i);
			list.put(compound.getInteger("Slot"), compound.getInteger("Integer"));
		}
		return list;
	}

	public static HashMap<Integer, Long> getIntegerLongMap(NBTTagList tagList) {
		HashMap<Integer, Long> list = new HashMap<>();
		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound compound = tagList.getCompoundTagAt(i);
			list.put(compound.getInteger("Slot"), compound.getLong("Long"));
		}
		return list;
	}

	public static HashSet<Integer> getIntegerSet(NBTTagList tagList) {
		HashSet<Integer> list = new HashSet<>();
		for (int i = 0; i < tagList.tagCount(); ++i) { list.add(tagList.getCompoundTagAt(i).getInteger("Integer")); }
		return list;
	}

	public static List<Integer> getIntegerList(NBTTagList tagList) {
		List<Integer> list = new ArrayList<>();
		for (int i = 0; i < tagList.tagCount(); ++i) { list.add(tagList.getCompoundTagAt(i).getInteger("Integer")); }
		return list;
	}

	public static HashMap<Integer, String> getIntegerStringMap(NBTTagList tagList) {
		HashMap<Integer, String> list = new HashMap<>();
		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound compound = tagList.getCompoundTagAt(i);
			list.put(compound.getInteger("Slot"), compound.getString("Value"));
		}
		return list;
	}

	public static HashMap<String, Integer> getStringIntegerMap(NBTTagList tagList) {
		HashMap<String, Integer> list = new HashMap<>();
		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound compound = tagList.getCompoundTagAt(i);
			list.put(compound.getString("Slot"), compound.getInteger("Value"));
		}
		return list;
	}

	public static List<String> getStringList(NBTTagList tagList) {
		List<String> list = new ArrayList<>();
		for (int i = 0; i < tagList.tagCount(); ++i) { list.add(tagList.getCompoundTagAt(i).getString("Line")); }
		return list;
	}

	public static NBTTagList nbtIntegerArraySet(List<int[]> set) {
		NBTTagList nbtList = new NBTTagList();
		if (set != null) {
			for (int[] arr : set) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setIntArray("Array", arr);
				nbtList.appendTag(compound);
			}
		}
		return nbtList;
	}

	public static NBTTagList nbtItemStackList(IInventory inventory) {
		NBTTagList nbtList = new NBTTagList();
		for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
			ItemStack item = inventory.getStackInSlot(slot);
			if (!item.isEmpty()) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("Slot", slot);
				item.writeToNBT(compound);
				nbtList.appendTag(compound);
			}
		}
		return nbtList;
	}

	public static NBTTagList nbtIItemStackMap(Map<Integer, IItemStack> inventory) {
		NBTTagList nbtList = new NBTTagList();
		if (inventory != null) {
			for (Map.Entry<Integer, IItemStack> entry : inventory.entrySet()) {
				if (entry.getValue() == null) { continue; }
				if (!NoppesUtilServer.isItemStackNull(entry.getValue().getMCItemStack())) {
					NBTTagCompound compound = new NBTTagCompound();
					compound.setByte("Slot", entry.getKey().byteValue());
					entry.getValue().getMCItemStack().writeToNBT(compound);
					nbtList.appendTag(compound);
				}
			}
		}
		return nbtList;
	}

	public static NBTTagList nbtIngredientList(NonNullList<Ingredient> inventory) {
		NBTTagList nbtList = new NBTTagList();
		if (inventory != null) {
			for (int slot = 0; slot < inventory.size(); ++slot) {
				Ingredient ingredient = inventory.get(slot);
				NBTTagCompound compound = new NBTTagCompound();
				compound.setByte("Slot", (byte) slot);
				NBTTagList ingredients = new NBTTagList();
				for (ItemStack ing : ((IIngredientMixin) ingredient).getMatchingStacks()) {
					ingredients.appendTag(ing.writeToNBT(new NBTTagCompound()));
				}
				compound.setTag("Ingredients", ingredients);
				nbtList.appendTag(compound);
			}
		}
		return nbtList;
	}

	public static NBTTagList nbtIntegerIntegerMap(Map<Integer, Integer> lines) {
		NBTTagList nbtList = new NBTTagList();
		if (lines != null) {
			for (int slot : lines.keySet()) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("Slot", slot);
				compound.setInteger("Integer", lines.get(slot));
				nbtList.appendTag(compound);
			}
		}
		return nbtList;
	}

	public static NBTTagList nbtIntegerLongMap(HashMap<Integer, Long> lines) {
		NBTTagList nbtList = new NBTTagList();
		if (lines != null) {
			for (int slot : lines.keySet()) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("Slot", slot);
				compound.setLong("Long", lines.get(slot));
				nbtList.appendTag(compound);
			}
		}
		return nbtList;
	}

	public static NBTTagList nbtIntegerCollection(Collection<Integer> set) {
		NBTTagList nbtList = new NBTTagList();
		if (set != null) {
			for (int slot : set) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("Integer", slot);
				nbtList.appendTag(compound);
			}
		}
		return nbtList;
	}

	public static NBTTagList nbtIntegerStringMap(Map<Integer, String> map) {
		NBTTagList nbtList = new NBTTagList();
		if (map != null) {
			for (int slot : map.keySet()) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("Slot", slot);
				compound.setString("Value", map.get(slot));
				nbtList.appendTag(compound);
			}
		}
		return nbtList;
	}

	public static NBTTagList nbtStringList(List<String> list) {
		NBTTagList nbtList = new NBTTagList();
		if (list != null) {
			for (String s : list) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setString("Line", s);
				nbtList.appendTag(compound);
			}
		}
		return nbtList;
	}

	public static NBTTagList nbtDoubleList(double... values) {
		NBTTagList nbtList = new NBTTagList();
		if (values != null) {
			for (int i = values.length, j = 0; j < i; ++j) { nbtList.appendTag(new NBTTagDouble(values[j])); }
		}
		return nbtList;
	}

	public static NBTTagCompound nbtMerge(NBTTagCompound data, NBTTagCompound merge) {
		NBTTagCompound compound = data.copy();
		for (String name : merge.getKeySet()) {
			NBTBase tag = merge.getTag(name);
			if ( tag.getId() == 10) { tag = nbtMerge(compound.getCompoundTag(name), (NBTTagCompound) tag); }
			compound.setTag(name, tag);
		}
		return compound;
	}

	public static List<ScriptContainer> getScript(NBTTagList list, IScriptHandler handler) {
		List<ScriptContainer> scripts = new ArrayList<>();
		for (int i = 0; i < list.tagCount(); ++i) {
			NBTTagCompound compound = list.getCompoundTagAt(i);
			ScriptContainer script = new ScriptContainer(handler);
			script.load(compound);
			scripts.add(script);
		}
		return scripts;
	}

	public static NBTTagList nbtScript(List<ScriptContainer> scripts) {
		NBTTagList list = new NBTTagList();
		for (ScriptContainer script : scripts) {
			NBTTagCompound compound = new NBTTagCompound();
			script.save(compound);
			list.appendTag(compound);
		}
		return list;
	}

	public static TreeMap<Long, String> getLongStringMap(NBTTagList tagList) {
		TreeMap<Long, String> map = new TreeMap<>();
		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound compound = tagList.getCompoundTagAt(i);
			long time = compound.getLong("Long");
			if (compound.hasKey("String", 8)) {
				if (!compound.getString("String").isEmpty()) { map.put(time, compound.getString("String")); }
			} // OLD
			else if (compound.hasKey("String", 9) && ((NBTTagList) compound.getTag("String")).getTagType() == 8) {
				StringBuilder totalStr = new StringBuilder();
				for (NBTBase sNbt : compound.getTagList("String", 8)) {
					totalStr.append(((NBTTagString) sNbt).getString());
				}
				if (!totalStr.toString().isEmpty()) { map.put(time, totalStr.toString()); }
			} // NEW
		}
		return map;
	}

	public static NBTTagList nbtLongStringMap(Map<Long, String> map) {
		NBTTagList nbtList = new NBTTagList();
		if (map != null) {
			for (long slot : map.keySet()) {
				NBTTagCompound nbt = new NBTTagCompound();
				nbt.setLong("Long", slot);
				List<String> content = Util.splitString(map.get(slot));
				if (content.size() < 2) { nbt.setString("String", map.get(slot)); } // OLD
				else {
					NBTTagList list = new NBTTagList();
					for (String line : content) { list.appendTag(new NBTTagString(line)); }
					nbt.setTag("String", list);
				} // NEW
				nbtList.appendTag(nbt);
			}
		}
		return nbtList;
	}

	// New from Unofficial (BetaZavr)
	public static boolean compareNBT(NBTTagCompound compound1, NBTTagCompound compound2) {
		if (compound1.getKeySet().size() != compound2.getKeySet().size()) { return false; }
		for (String key : compound1.getKeySet()) {
			if (!compound2.hasKey(key)) { return false; }
			if (notCompareNBTBase(compound1.getTag(key), compound2.getTag(key))) { return false; }
		}
		return true;
	}

	private static boolean notCompareNBTBase(NBTBase tag1, NBTBase tag2) {
		if (tag1.getId() != tag2.getId()) { return true; }
		switch (tag1.getId()) {
			case 1: // TAG_BYTE
				return ((NBTTagByte) tag1).getByte() != ((NBTTagByte) tag2).getByte();
			case 2: // TAG_SHORT
				return ((NBTTagShort) tag1).getShort() != ((NBTTagShort) tag2).getShort();
			case 3: // TAG_INT
				return ((NBTTagInt) tag1).getInt() != ((NBTTagInt) tag2).getInt();
			case 4: // TAG_LONG
				return ((NBTTagLong) tag1).getLong() != ((NBTTagLong) tag2).getLong();
			case 5: // TAG_FLOAT
				return Float.floatToIntBits(((NBTTagFloat) tag1).getFloat()) != Float.floatToIntBits(((NBTTagFloat) tag2).getFloat());
			case 6: // TAG_DOUBLE
				return Double.doubleToLongBits(((NBTTagDouble) tag1).getDouble()) != Double.doubleToLongBits(((NBTTagDouble) tag2).getDouble());
			case 7: // TAG_BYTE_ARRAY
				return !Arrays.equals(((NBTTagByteArray) tag1).getByteArray(), ((NBTTagByteArray) tag2).getByteArray());
			case 8: // TAG_STRING
				return !((NBTTagString) tag1).getString().equals(((NBTTagString) tag2).getString());
			case 9: // TAG_LIST
				return notCompareNBTLists((NBTTagList) tag1, (NBTTagList) tag2);
			case 10: // TAG_COMPOUND
				return !compareNBT((NBTTagCompound) tag1, (NBTTagCompound) tag2);
			case 11: // TAG_INT_ARRAY
				return !Arrays.equals(((NBTTagIntArray) tag1).getIntArray(), ((NBTTagIntArray) tag2).getIntArray());
			case 12: // TAG_LONG_ARRAY
				return !Arrays.equals(((INBTTagLongArrayMixin) tag1).getData(), ((INBTTagLongArrayMixin) tag2).getData());
			default:
				return true;
		}
	}

	private static boolean notCompareNBTLists(NBTTagList list1, NBTTagList list2) {
		if (list1.tagCount() != list2.tagCount()) { return true; }
		for (int i = 0; i < list1.tagCount(); i++) {
			if (notCompareNBTBase(list1.get(i), list2.get(i))) { return true; }
		}
		return false;
	}

}
