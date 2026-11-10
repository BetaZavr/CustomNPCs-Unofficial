package noppes.npcs.controllers;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import noppes.npcs.*;
import noppes.npcs.api.handler.IRecipeHandler;
import noppes.npcs.api.handler.data.INpcRecipe;
import noppes.npcs.controllers.data.RecipeCarpentry;
import noppes.npcs.controllers.data.RecipesDefault;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSync;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nonnull;

public class RecipeController implements IRecipeHandler {

	public static final int version = 4;

	protected static RecipeController instance;

	protected final Map<String, List<RecipeCarpentry>> globalRecipes = new HashMap<>(); // { group, group recipes }
	protected final Map<String, List<RecipeCarpentry>> anvilRecipes = new HashMap<>(); // { group, group recipes }
	public final Map<String, List<RecipeCarpentry>> syncRecipes = new HashMap<>();

	public static RecipeController getInstance() {
		if (instance == null) { instance = new RecipeController(); }
		return instance;
	}

	@Override
	public boolean delete(String id) { return delete(new ResourceLocation(id)); }

	@Override
	public List<INpcRecipe> getGlobalRecipes(String group) {
		List<INpcRecipe> list = new ArrayList<>();
		if (globalRecipes.containsKey(group)) { list.addAll(globalRecipes.get(group)); }
		return list;
	}

	@Override
	public List<INpcRecipe> getAnvilRecipes(String group) {
		List<INpcRecipe> list = new ArrayList<>();
		if (anvilRecipes.containsKey(group)) { list.addAll(anvilRecipes.get(group)); }
		return list;
	}

	@Override
	public RecipeCarpentry addRecipe(String name, String group, boolean global, ItemStack result, Object... objects) {
		RecipeCarpentry recipe = RecipeCarpentry.createRecipe(new ResourceLocation(CustomNpcs.MODID, name), group, global, true, result, objects);
		return addAndSaveRecipe(recipe);
	}

	@Override
	public RecipeCarpentry addRecipe(String name, String group, boolean global, ItemStack result, int width, int height, ItemStack... objects) {
		NonNullList<Ingredient> list = NonNullList.create();
		for(ItemStack item : objects) {
			if (!item.isEmpty()) { list.add(Ingredient.fromStacks(item)); }
		}
		RecipeCarpentry recipe = new RecipeCarpentry(new ResourceLocation(CustomNpcs.MODID, name), group, width, height, global, true, list, result);
		return addAndSaveRecipe(recipe);
	}

	@Override
	public List<INpcRecipe> getAllAnvilRecipes() {
		List<INpcRecipe> list = new ArrayList<>();
		for (List<RecipeCarpentry> recipes : new ArrayList<>(anvilRecipes.values())) { list.addAll(recipes); }
		return list;
	}

	@Override
	public List<INpcRecipe> getAllGlobalRecipes() {
		List<INpcRecipe> list = new ArrayList<>();
		for (List<RecipeCarpentry> recipes : new ArrayList<>(globalRecipes.values())) { list.addAll(recipes); }
		return list;
	}

	public void load() {
		loadCategories();
		EventHooks.onGlobalRecipesLoaded(this);
		CustomNpcs.proxy.syncRecipeManager();
	}

	public void reloadGlobalRecipes() {
		globalRecipes.clear();
		globalRecipes.putAll(syncRecipes);
		syncRecipes.clear();
	}

	public void reloadAnvilRecipes() {
		anvilRecipes.clear();
		anvilRecipes.putAll(syncRecipes);
		syncRecipes.clear();
	}

	private void loadCategories() {
		File saveDir = CustomNpcs.getWorldSaveDirectory();
		try {
			File file = new File(saveDir, "recipes.dat");
			if (file.exists()) {
				loadCategories(file);
			}
			else {
				globalRecipes.clear();
				anvilRecipes.clear();
				loadDefaultRecipes(-1);
			}
		} catch (Exception e) {
			LogWriter.error(e);
			try {
				File file = new File(saveDir, "recipes.dat_old");
				if (file.exists()) { loadCategories(file); }
			} catch (Exception err) {
				LogWriter.error(err);
			}
		}
	}

	private void loadDefaultRecipes(int versionIn) {
		if (versionIn != version) {
			RecipesDefault.loadDefaultRecipes(versionIn);
		}
	}

	private void loadCategories(File file) throws Exception {
		NBTTagCompound compound = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
		globalRecipes.clear();
		anvilRecipes.clear();
		int version = compound.getInteger("Version");
		if (version < 3) {
			NBTTagList list = compound.getTagList("Data", 10);
			for(int i = 0; i < list.tagCount(); ++i) {
				RecipeCarpentry recipe = RecipeCarpentry.create(list.getCompoundTagAt(i));
				Map<String, List<RecipeCarpentry>> map = recipe.isGlobal ? globalRecipes : anvilRecipes;
				if (!map.containsKey("main")) { map.put("main", new ArrayList<>()); }
				map.get("main").add(recipe);
			}
		}
		else if (version == 3) {
			NBTTagList list = compound.getTagList("Data", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound nbtG = list.getCompoundTagAt(i);
				if (!nbtG.hasKey("GroupName", 8)) { continue; }
				Map<String, List<RecipeCarpentry>> map = nbtG.getBoolean("isGlobal") ? globalRecipes : anvilRecipes;
				if (!map.containsKey(nbtG.getString("GroupName"))) { map.put(nbtG.getString("GroupName"), new ArrayList<>()); }
				for (int j = 0; j < nbtG.getTagList("Recipes", 10).tagCount(); j++) {
					map.get(nbtG.getString("GroupName"))
							.add(RecipeCarpentry.create(nbtG.getTagList("Recipes", 10).getCompoundTagAt(j)));
				}
			}
		}
		else {
			for (int i = 0; i < 2; i++) {
				NBTTagCompound groups = compound.getCompoundTag(i == 0 ? "GlobalRecipes" : "AnvilRecipes");
				for(String group : groups.getKeySet()) {
					NBTTagList list = groups.getTagList(group, 10);
					for(int j = 0; j < list.tagCount(); ++j) {
						RecipeCarpentry recipe = RecipeCarpentry.create(list.getCompoundTagAt(j));
						Map<String, List<RecipeCarpentry>> map = i == 0 ? globalRecipes : anvilRecipes;
						if (!map.containsKey(group)) { map.put(group, new ArrayList<>()); }
						map.get(group).add(recipe);
					}
				}
			}
		}
		loadDefaultRecipes(version);
	}

	private void saveCategories() {
		try {
			File saveDir = CustomNpcs.getWorldSaveDirectory();
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("Version", version);
			for (int i = 0; i < 2; i++) {
				Map<String, List<RecipeCarpentry>> map = i == 0 ? globalRecipes : anvilRecipes;
				NBTTagCompound groups = new NBTTagCompound();
				for(Map.Entry<String, List<RecipeCarpentry>> entry : map.entrySet()) {
					NBTTagList list = new NBTTagList();
					for(RecipeCarpentry recipe : new ArrayList<>(entry.getValue())) { list.appendTag(recipe.saveTo()); }
					groups.setTag(entry.getKey(), list);
				}
				compound.setTag(i == 0 ? "GlobalRecipes" : "AnvilRecipes", groups);
			}
			File file = new File(saveDir, "recipes.dat_new");
			File file1 = new File(saveDir, "recipes.dat_old");
			File file2 = new File(saveDir, "recipes.dat");
			CompressedStreamTools.writeCompressed(compound, Files.newOutputStream(file.toPath()));
			//if (CustomNpcs.VerboseDebug) { NBTJsonUtil.SaveFile(new File(saveDir, "recipes.json"), compound); }
			if (file1.exists() && !file1.delete()) { LogWriter.debug("Error delete \"" + file1.getName() + "\" file"); }
			if (!file2.renameTo(file1) || (file2.exists() && !file2.delete())) { LogWriter.debug("Error delete or rename \"" + file2.getName() + "\" file"); }
			if (!file.renameTo(file2) || (file.exists() && !file.delete())) { LogWriter.debug("Error delete or rename \"" + file.getName() + "\" file"); }
		}
		catch (Exception e) { LogWriter.error(e); }
	}

	public RecipeCarpentry getRecipe(ResourceLocation id) {
		for (int i = 0; i < 2; i++) {
			for (Map.Entry<String, List<RecipeCarpentry>> entry : (i == 0 ? globalRecipes : anvilRecipes).entrySet()) {
				for(RecipeCarpentry recipe : new ArrayList<>(entry.getValue())) {
					if (recipe.getMCId().equals(id)) { return recipe; }
				}
			}
		}
		return null;
	}

	public RecipeCarpentry addAndSaveRecipe(RecipeCarpentry recipe) {
		if (recipe.getGroup().isEmpty()) { recipe.setGroup("npc_default"); }
		Map<String, List<RecipeCarpentry>> parent = recipe.isGlobal ? globalRecipes : anvilRecipes;
		Map<String, List<RecipeCarpentry>> invert = recipe.isGlobal ? anvilRecipes : globalRecipes;
		if (!parent.containsKey(recipe.getGroup())) { parent.put(recipe.getGroup(), new ArrayList<>()); }
		boolean found = false;
		for (RecipeCarpentry r : new ArrayList<>(parent.get(recipe.getGroup()))) {
			if (r.getMCId().equals(recipe.getMCId())) {
				r.loadFrom(recipe.saveTo());
				recipe = r;
				found = true;
				break;
			}
		}
		if (!found) { parent.get(recipe.getGroup()).add(recipe); }
		for (Map.Entry<String, List<RecipeCarpentry>> entry : new ArrayList<>(invert.entrySet())) {
			for (RecipeCarpentry r : new ArrayList<>(entry.getValue())) {
				if (r.getMCId().equals(recipe.getMCId())) {
					entry.getValue().remove(r);
					break;
				}
			}
			if (entry.getValue().isEmpty()) { invert.remove(entry.getKey()); }
		}
		saveCategories();
		updateToAll();
		return recipe;
	}

	public boolean delete(ResourceLocation id) {
		if (CustomNpcs.Server != null) {
			boolean isRemoved = false;
			for (int i = 0; i < 2; i++) {
				for (List<RecipeCarpentry> group : (i == 0 ? globalRecipes : anvilRecipes).values()) {
					for (RecipeCarpentry npcRecipe : group) {
						if (npcRecipe.getMCId().equals(id)) {
							isRemoved = true;
							break;
						}
					}
					if (isRemoved) { break; }
				}
				if (isRemoved) { break; }
			}
			globalRecipes.values().removeIf(List::isEmpty);
			anvilRecipes.values().removeIf(List::isEmpty);
			if (isRemoved) {
				saveCategories();
				updateToAll();
			}
			return isRemoved;
		}
		return false;
	}

	public void sendTo(EntityPlayerMP player) {
		// global recipes
		for (int i = 11; i < 13; i++) {
			for (List<RecipeCarpentry> group : new ArrayList<>((i == 11 ? globalRecipes : anvilRecipes).values())) {
				for (RecipeCarpentry recipe : new ArrayList<>(group)) {
					Packets.send(player, new PacketSync(i, recipe.saveTo(), false));
				}
			}
			Packets.send(player, new PacketSync(i, new NBTTagCompound(), true));
		}
		Packets.send(player, new PacketSync(18, new NBTTagCompound(), false));
	}

	public List<IRecipe> getKnownRecipes() {
		List<IRecipe> list = new ArrayList<>();
		IForgeRegistry<IRecipe> manager = CustomNpcs.proxy.getRecipeManager();
		if (manager != null) {
			for (int i = 0; i < 2; i++) {
				for (List<RecipeCarpentry> group : (i == 0 ? globalRecipes.values() : anvilRecipes.values())) {
					for (RecipeCarpentry recipe : new ArrayList<>(group)) {
						if (recipe.isKnown() && manager.containsKey(recipe.getMCId())) { list.add(recipe); }
					}
				}
			}
		}
		return list;
	}

	public void deleteGroup(boolean isGlobal, String group) {
		if (CustomNpcs.Server != null) {
			Map<String, List<RecipeCarpentry>> map = (isGlobal ? globalRecipes : anvilRecipes);
			if (map.containsKey(group)) {
				map.remove(group);
				updateToAll();
			}
		}
	}

	public void addGroup(boolean isGlobal, @Nonnull String group) {
		if (CustomNpcs.Server != null) {
			Map<String, List<RecipeCarpentry>> map = (isGlobal ? globalRecipes : anvilRecipes);
			if (!map.containsKey(group)) {
				map.put(group, new ArrayList<>());
				map.get(group).add(createNewRecipe(isGlobal, group));
				updateToAll();
			}
		}
	}

	private RecipeCarpentry createNewRecipe(boolean isGlobal, String group) {
		String name = "new";
		while (containsName(name)) { name += "_"; }
		return new RecipeCarpentry(new ResourceLocation(CustomNpcs.MODID, name), group,
				isGlobal ? 3 : 4, isGlobal ? 3 : 4, isGlobal, true,
				NonNullList.create(),
				new ItemStack(Blocks.COBBLESTONE));
	}

	public void renameGroup(boolean isGlobal, @Nonnull String oldGroup, @Nonnull String newGroup) {
		if (CustomNpcs.Server != null) {
			Map<String, List<RecipeCarpentry>> map = (isGlobal ? globalRecipes : anvilRecipes);
			while (map.containsKey(newGroup)) { newGroup += "_"; }
			List<RecipeCarpentry> list = new ArrayList<>();
			if (map.containsKey(oldGroup)) {
				list = map.get(oldGroup);
				map.remove(oldGroup);
			}
			if (list.isEmpty()) { list.add(createNewRecipe(isGlobal, newGroup)); }
			else {
				for (RecipeCarpentry r : list) { r.setGroup(newGroup); }
			}
			map.put(newGroup, list);
		}
	}

	public void renameRecipe(@Nonnull String oldName, @Nonnull String newName) {
		if (CustomNpcs.Server != null) {
			RecipeCarpentry recipe = getRecipe(new ResourceLocation(CustomNpcs.MODID, oldName));
			if (recipe != null) {
				while (containsName(newName)) { newName += "_"; }
				recipe.setId(new ResourceLocation(CustomNpcs.MODID, newName));
			}
		}
	}

	public void updateToAll() {
		if (CustomNpcs.Server != null) {
			for (EntityPlayerMP pl : CustomNpcs.Server.getPlayerList().getPlayers()) {
				CustomNpcs.proxy.syncRecipeManager();
				sendTo(pl);
			} // update all
		}
	}

	public boolean containsName(@Nonnull String name) {
		for (int i = 0; i < 2; i++) {
			for (Map.Entry<String, List<RecipeCarpentry>> entry : (i == 0 ? globalRecipes : anvilRecipes).entrySet()) {
				for(RecipeCarpentry recipe : new ArrayList<>(entry.getValue())) {
					if (recipe.getMCId().getResourcePath().equals(name)) { return true; }
				}
			}
		}
		return false;
	}

	public List<String> getGroups(boolean isGlobal) {
		return new ArrayList<>(isGlobal ? globalRecipes.keySet() : anvilRecipes.keySet());
	}

}
