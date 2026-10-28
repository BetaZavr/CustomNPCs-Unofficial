package noppes.npcs.controllers.data;

import java.util.*;

import com.google.gson.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.crafting.CraftingHelper;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.INbt;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.handler.data.IAvailability;
import noppes.npcs.api.handler.data.INpcRecipe;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.api.wrapper.gui.WrapperRecipe;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.util.CustomRecipeMatcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RecipeCarpentry implements CraftingRecipe, INpcRecipe {

   static int MAX_WIDTH = 4;
   static int MAX_HEIGHT = 4;

   public static final RecipeSerializer<RecipeCarpentry> CARPENTRY_RECIPE = new Serializer();
   public final Availability availability = new Availability();
   public boolean isGlobal;
   public boolean ignoreDamage = false;
   public boolean ignoreNBT = false;
   public String name;

   // New from Unofficial (BetaZavr)
   protected @Nonnull NonNullList<Ingredient> ingredients;
   protected @Nonnull ItemStack result;
   protected @Nonnull ResourceLocation id;
   protected @Nonnull String group;
   protected int width;
   protected int height;
   protected boolean isShaped;
   protected boolean isKnown = false;
   protected boolean showInRecipeBook = true;

   public RecipeCarpentry(@Nonnull ResourceLocation idIn, @Nonnull String groupIn, int widthIn, int heightIn, boolean isGlobalIn, boolean isShapedIn, @Nonnull NonNullList<Ingredient> recipeItemsIn, @Nonnull ItemStack resultIn) {
      id = idIn;
      group = groupIn;
      width = widthIn;
      height = heightIn;
      ingredients = recipeItemsIn;
      if (ingredients.isEmpty()) {
         ingredients = NonNullList.create();
         ingredients.add(Ingredient.of(new ItemStack(Blocks.COBBLESTONE)));
      }
      result = resultIn;
      if (result.isEmpty()) { result = new ItemStack(Blocks.COBBLESTONE); }

      name = idIn.getPath();
      isShaped = isShapedIn;
      isGlobal = isGlobalIn;
   }

   public static RecipeCarpentry create(CompoundTag compound) {
      RecipeCarpentry recipe = new RecipeCarpentry(new ResourceLocation(CustomNpcs.MODID, ""), "", 0, 0, true, true, NonNullList.create(), ItemStack.EMPTY);
      recipe.loadFrom(compound);
      return recipe;
   }

   public static RecipeCarpentry createRecipe(@Nonnull ResourceLocation location, String group, boolean isGlobal, boolean isShaped, ItemStack result, Object... objects) {
      StringBuilder lineData = new StringBuilder();
      int size = 0;
      int width = 0;
      int height = 0;
      if (objects[size] instanceof String[]) {
         for (String line : (String[]) objects[size++]) {
            ++height;
            width = line.length();
            lineData.append(line);
         }
      }
      else {
         while(objects[size] instanceof String) {
            String line = (String) objects[size++];
            ++height;
            width = line.length();
            lineData.append(line);
         }
      }
      HashMap<Character, ItemStack> mapData;
      for(mapData = new HashMap<>(); size < objects.length; size += 2) {
         Character character = (Character) objects[size];
         ItemStack stack = ItemStack.EMPTY;
         if (objects[size + 1] instanceof Item item) { stack = new ItemStack(item); }
         else if (objects[size + 1] instanceof Block block) { stack = new ItemStack(block, 1); }
         else if (objects[size + 1] instanceof ItemStack stackIn) { stack = stackIn; }
         mapData.put(character, stack);
      }
      NonNullList<Ingredient> ingredients = NonNullList.create();
      for(int slotId = 0; slotId < width * height; ++slotId) {
         char c = lineData.charAt(slotId);
         if (mapData.containsKey(c)) {
            ingredients.add(slotId, Ingredient.of(mapData.get(c).copy()));
         } else {
            ingredients.add(slotId, Ingredient.EMPTY);
         }
      }
      return new RecipeCarpentry(location, group, width, height, isGlobal, isShaped, ingredients, result);
   }

   @Override
   public boolean matches(@Nonnull CraftingContainer inventoryCrafting, @Nullable Level world) {
      if (isShaped) {
         for(int x = 0; x <= inventoryCrafting.getWidth() - width; ++x) {
            for(int y = 0; y <= inventoryCrafting.getHeight() - height; ++y) {
               if (checkMatch(inventoryCrafting, x, y, true)) { return true; }
               if (checkMatch(inventoryCrafting, x, y, false)) { return true; }
            }
         }
         return false;
      }
      List<ItemStack> inputs = new ArrayList<>();
      for(int slotId = 0; slotId < inventoryCrafting.getContainerSize(); ++slotId) {
         ItemStack stackInGrid = inventoryCrafting.getItem(slotId);
         if (!stackInGrid.isEmpty()) {
            inputs.add(stackInGrid);
         }
      }
      return CustomRecipeMatcher.findMatches(inputs, ingredients.stream().filter(ing -> !ing.isEmpty()).toList(),
              ignoreDamage, ignoreNBT) != null;
   }

   @Override
   public @Nonnull ItemStack assemble(@Nonnull CraftingContainer container, @Nonnull RegistryAccess registryAccess) { return result.copy(); }

   @Override
   public boolean canCraftInDimensions(int widthIn, int heightIn) {
      if (isShaped) { return widthIn >= width && heightIn >= height; }
      return widthIn * heightIn >= ingredients.stream().filter(ing -> !ing.isEmpty()).toList().size();
   }

   @Override
   public @Nonnull ItemStack getResultItem(@Nonnull RegistryAccess registryAccess) {
      return result.isEmpty() ? ItemStack.EMPTY : result.copy();
   }

   @Override
   public @Nonnull NonNullList<ItemStack> getRemainingItems(CraftingContainer inventoryCrafting) {
      NonNullList<ItemStack> list = NonNullList.withSize(inventoryCrafting.getContainerSize(), ItemStack.EMPTY);
      for(int i = 0; i < list.size(); ++i) {
         ItemStack itemstack = inventoryCrafting.getItem(i);
         list.set(i, ForgeHooks.getCraftingRemainingItem(itemstack));
      }
      return list;
   }

   @Override
   public String getName() { return name; }

   @Override
   public IItemStack getResult() { return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(getResultItem(RegistryAccess.EMPTY)); }

   @Override
   public boolean isGlobal() { return isGlobal; }

   @Override
   public void setIsGlobal(boolean isGlobalIn) {
      if (isGlobal != isGlobalIn && CustomNpcs.Server != null) {
         isGlobal = isGlobalIn;
         RecipeController.getInstance().updateToAll();
      }
   }

   @Override
   public boolean getIgnoreNBT() { return ignoreNBT; }

   @Override
   public void setIgnoreNBT(boolean ignoreNBTIn) {
      if (ignoreNBT != ignoreNBTIn && CustomNpcs.Server != null) {
         ignoreNBT = ignoreNBTIn;
         RecipeController.getInstance().updateToAll();
      }
   }

   @Override
   public boolean getIgnoreDamage() { return ignoreDamage; }

   @Override
   public void setIgnoreDamage(boolean ignoreDamageIn) {
      if (ignoreDamage != ignoreDamageIn && CustomNpcs.Server != null) {
         ignoreDamage = ignoreDamageIn;
         RecipeController.getInstance().updateToAll();
      }
   }

   @Override
   public int getWidth() { return width; }

   @Override
   public int getHeight() { return height; }

   @Override
   public void save() {
      if (CustomNpcs.Server != null) { RecipeController.getInstance().addAndSaveRecipe(this); }
   }

   @Override
   public void delete() {
      if (CustomNpcs.Server != null) { RecipeController.getInstance().delete(id); }
   }

   @Override
   public boolean isKnown() { return isKnown; }

   @Override
   public void setIsKnown(boolean isKnownIn) {
      if (isKnown != isKnownIn && CustomNpcs.Server != null) {
         isKnown = isKnownIn;
         RecipeController.getInstance().updateToAll();
      }
   }

   @Override
   public boolean showInRecipeBook() { return showInRecipeBook; }

   @Override
   public void setShowInRecipeBook(boolean showInRecipeBookIn) {
      if (showInRecipeBook != showInRecipeBookIn && CustomNpcs.Server != null) {
         showInRecipeBook = showInRecipeBookIn;
         RecipeController.getInstance().updateToAll();
      }
   }

   @Override
   public IAvailability getAvailability() { return availability; }

   @Override
   public IItemStack[][] getRecipe() {
      IItemStack[][] array = new IItemStack[ingredients.size()][];
      for (int i = 0; i < ingredients.size(); i++) {
         if (ingredients.get(i).getItems().length > 0) {
            ItemStack[] items = ingredients.get(i).getItems();
            array[i] = new IItemStack[items.length];
            for (int j = 0; j < items.length; j++) {
               array[i][j] = Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(items[j]);
            }
         }
         else { array[i] = new IItemStack[0]; }
      }
      return array;
   }

   @Override
   public boolean isSpecial() { return !showInRecipeBook; }

   @Override
   public @Nonnull ResourceLocation getId() { return id; }

   @Override
   public @Nonnull ResourceLocation getMCId() { return id; }

   @Override
   public INbt getNbt() { return new NBTWrapper(saveTo()); }

   @Override
   public String getNpcGroup() { return getGroup(); }

   @Override
   public void setNbt(INbt nbt) {
      if (nbt != null && CustomNpcs.Server != null) {
         loadFrom(nbt.getMCNBT());
         RecipeController.getInstance().updateToAll();
      }
   }

   @Override
   public boolean isRecipeItemsEmpty() {
      for (Ingredient ingredient : ingredients) {
         for (ItemStack stack : ingredient.getItems()) {
            if (!stack.isEmpty()) { return false; }
         }
      }
      return true;
   }

   @Override
   public @Nonnull RecipeSerializer<?> getSerializer() { return CARPENTRY_RECIPE; }

   @Override
   public @Nonnull NonNullList<Ingredient> getIngredients() { return ingredients; }

   @Override
   public @Nonnull RecipeType<?> getType() { return RecipeType.CRAFTING; }

   @Override
   public @Nonnull CraftingBookCategory category() { return CraftingBookCategory.MISC; }

   @Override
   public @Nonnull String getGroup() { return group; }

   @Override
   public boolean isShaped() { return isShaped; }

   @Override
   public void setIsShaped(boolean isShapedIn) {
      if (isShaped != isShapedIn && CustomNpcs.Server != null) {
         isShaped = isShapedIn;
         RecipeController.getInstance().updateToAll();
      }
   }

   @Override
   public void setResult(IItemStack newResult) {
      ItemStack resultIn = newResult == null ? ItemStack.EMPTY : newResult.getMCItemStack();
      if (!NoppesUtilPlayer.compareItems(result, resultIn, ignoreDamage, ignoreNBT) && CustomNpcs.Server != null) {
         result = resultIn;
         RecipeController.getInstance().updateToAll();
      }
   }

   public CompoundTag saveTo() {
      CompoundTag compound = new CompoundTag();
      compound.putString("Id", id.toString());
      compound.putString("Group", group);
      compound.putInt("Width", width);
      compound.putInt("Height", height);
      compound.put("Materials", NBTTags.nbtIngredientList(ingredients));
      compound.put("Item", result.save(new CompoundTag()));
      compound.put("Availability", availability.save(new CompoundTag()));
      compound.putBoolean("IgnoreDamage", ignoreDamage);
      compound.putBoolean("IgnoreNBT", ignoreNBT);
      compound.putBoolean("Global", isGlobal);
      compound.putBoolean("IsKnown", isKnown);
      compound.putBoolean("IsShaped", isShaped);
      compound.putBoolean("ShowInRecipeBook", showInRecipeBook);
      compound.putString("Name", name);
      return compound;
   }

   public void loadFrom(CompoundTag compound) {
      if (compound.contains("ID", 8)) { id = new ResourceLocation(CustomNpcs.MODID, NoppesUtilServer.validPath(compound.getString("ID"))); }
      else { id = new ResourceLocation(NoppesUtilServer.validLocation(compound.getString("Id"))); }
      group = compound.getString("Group");
      width = compound.getInt("Width");
      height = compound.getInt("Height");
      ingredients = NBTTags.getIngredientList(compound.getList("Materials", 10));
      if (ingredients.isEmpty()) {
         ingredients = NonNullList.create();
         ingredients.add(Ingredient.of(new ItemStack(Blocks.COBBLESTONE)));
      }
      result = compound.contains("Item", 10) ? ItemStack.of(compound.getCompound("Item")) : ItemStack.EMPTY;
      if (result.isEmpty()) { result = new ItemStack(Blocks.COBBLESTONE); }
      availability.load(compound.getCompound("Availability"));

      ignoreDamage = compound.getBoolean("IgnoreDamage");
      ignoreNBT = compound.getBoolean("IgnoreNBT");
      isGlobal = compound.getBoolean("Global");
      isKnown = compound.getBoolean("IsKnown");
      isShaped = compound.getBoolean("IsShaped");
      showInRecipeBook = compound.getBoolean("ShowInRecipeBook");
      name = compound.getString("Name");
   }

   private boolean checkMatch(CraftingContainer inventoryCrafting, int x, int y, boolean isRevers) {
      for(int i = 0; i < inventoryCrafting.getWidth(); ++i) {
         for(int j = 0; j < inventoryCrafting.getHeight(); ++j) {
            int u = i - x;
            int v = j - y;
            Ingredient ingredient = Ingredient.EMPTY;
            if (u >= 0 && v >= 0 && u < width && v < height) {
               if (isRevers) { ingredient = ingredients.get(width - u - 1 + v * width); }
               else { ingredient = ingredients.get(u + v * width); }
            }
            ItemStack stackInGrid = inventoryCrafting.getItem(i + j * inventoryCrafting.getWidth());
            if (ingredient.getItems().length != 0) {
               if (stackInGrid.isEmpty()) { return false; }
               ItemStack[] items = ingredient.getItems();
               boolean found = false;
               for (int k = 0; k < items.length; k++) {
                  if (NoppesUtilPlayer.compareItems(items[0], stackInGrid, ignoreDamage, ignoreNBT) &&
                          items[0].getCount() <= stackInGrid.getCount()) {
                     found = true;
                     break;
                  }
               }
               if (!found) { return false; }
            }
            else if (!stackInGrid.isEmpty()) { return false; }
         }
      }
      return true;
   }

   @Override
   public boolean isValid() {
      return !name.isEmpty() && !getResult().isEmpty() && !isRecipeItemsEmpty();
   }

   @Override
   public WrapperRecipe getWrapperRecipe() {
      WrapperRecipe wrapper = new WrapperRecipe();
      wrapper.parent = this;
      wrapper.isShaped = isShaped;
      wrapper.isGlobal = isGlobal;
      wrapper.isKnown = isKnown;
      wrapper.ignoreDamage = ignoreDamage;
      wrapper.ignoreNBT = ignoreNBT;
      wrapper.id = id;
      wrapper.width = width;
      wrapper.height = height;
      wrapper.group = Component.literal(getGroup());
      wrapper.product = result.copy();
      wrapper.availability = availability;

      wrapper.ingredients.clear();
      int size = isGlobal ? 3 : 4;
      for (int slotId = 0; slotId < size * size; slotId++) {
         int x = slotId % size;
         int y = (int) Math.floor((double) slotId / (double) size);
         if (isShaped) {
            if (x < width && y < height) {
               int ingredient = x + y * width;
               if (ingredient < ingredients.size()) { wrapper.ingredients.put(slotId, ingredients.get(ingredient).getItems()); }
               else { wrapper.ingredients.put(slotId, new ItemStack[0]); }
            }
            else { wrapper.ingredients.put(slotId, new ItemStack[0]); }
         } else {
            if (slotId < ingredients.size()) { wrapper.ingredients.put(slotId, ingredients.get(slotId).getItems()); }
            else { wrapper.ingredients.put(slotId, new ItemStack[0]); }
         }
      }
      return wrapper;
   }

   @Override
   public void setItems(IItemStack[][] items) {
      if (CustomNpcs.Server != null && items != null && items.length > 0) {
         Map<Integer, List<IItemStack>> mapItems = new HashMap<>();
         for (int slotId = 0; slotId < items.length && (!isShaped || slotId < width * height); slotId++) {
            mapItems.put(slotId, new ArrayList<>());
            for (IItemStack stack : items[slotId]) {
               if (stack != null && stack.isEmpty()) { mapItems.get(slotId).add(stack); }
            }
            if (items[slotId].length > 0 && mapItems.get(slotId).isEmpty()) {
               mapItems.get(slotId).add(ItemStackWrapper.AIR);
            }
         }
         setItems(mapItems);
      }
   }

   @Override
   public void setItems(Map<Integer, List<IItemStack>> mapItems) {
      if (CustomNpcs.Server != null) {
         if (isShaped) {
            int size = width * height;
            for (int slotId = 0; slotId < size; slotId++) {
               if (!mapItems.containsKey(slotId)) {
                  mapItems.put(slotId, new ArrayList<>());
                  mapItems.get(slotId).add(ItemStackWrapper.AIR);
               }
            }
            for (int slotId : new ArrayList<>(mapItems.keySet())) {
               if (slotId > size) { mapItems.remove(slotId); }
            }
         }
         ingredients = NonNullList.create();
         for (List<IItemStack> list : new ArrayList<>(mapItems.values())) {
            ItemStack[] items = new ItemStack[list.size()];
            int i = 0;
            for (IItemStack iStack : list) { items[i++] = iStack.getMCItemStack(); }
            ingredients.add(Ingredient.of(items));
         }
         RecipeController.getInstance().updateToAll();
      }
   }

   public void setGroup(String newGroup) {
      if (newGroup != null && !group.equals(newGroup) && CustomNpcs.Server != null) {
         group = newGroup;
         RecipeController.getInstance().updateToAll();
      }
   }

   public void setId(ResourceLocation newId) {
      if (newId != null && !id.equals(newId) && CustomNpcs.Server != null) {
         id = newId;
         RecipeController.getInstance().updateToAll();
      }
   }


   public static class Serializer implements RecipeSerializer<RecipeCarpentry> {

      public static CompoundTag getAsCompoundTag(JsonObject json, String key, CompoundTag defaultValue) {
         if (json.has(key)) {
            JsonElement element = json.get(key);
            if (element.isJsonNull()) { return defaultValue; }
            return parseCompoundTag(element, key);
         }
         return defaultValue;
      }

      private static CompoundTag parseCompoundTag(JsonElement element, String key) {
         if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String nbtString = element.getAsString();
            try { return TagParser.parseTag(nbtString); }
            catch (Exception e) { throw new JsonSyntaxException("Invalid NBT tag at '" + key + "': " + nbtString, e); }
         }
         if (element.isJsonObject()) {
            String jsonStr = element.toString();
            try { return TagParser.parseTag(jsonStr); }
            catch (Exception e) { throw new JsonSyntaxException("Invalid NBT JSON at '" + key + "': " + jsonStr, e); }
         }
         throw new JsonSyntaxException("Expected NBT tag to be a string or object at '" + key + "'");
      }

      @Override
      public @Nonnull RecipeCarpentry fromJson(@Nonnull ResourceLocation idIn, @Nonnull JsonObject jsonObject) {
         NonNullList<Ingredient> ingredients = itemsFromJson(GsonHelper.getAsJsonArray(jsonObject, "ingredients"));
         if (ingredients.isEmpty()) {
            throw new JsonParseException("No ingredients for shapeless recipe");
         }
         if (ingredients.size() > MAX_WIDTH * MAX_HEIGHT) {
            throw new JsonParseException("Too many ingredients for shapeless recipe. The maximum is " + (MAX_WIDTH * MAX_HEIGHT));
         }
         RecipeCarpentry npcRecipe = new RecipeCarpentry(idIn,
                 GsonHelper.getAsString(jsonObject, "group", "npc_new"),
                 GsonHelper.getAsInt(jsonObject, "width", 3),
                 GsonHelper.getAsInt(jsonObject, "height", 3),
                 GsonHelper.getAsBoolean(jsonObject, "is_global", true),
                 GsonHelper.getAsBoolean(jsonObject, "is_shaped", true),
                 ingredients,
                 CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(jsonObject, "result"), true, true));
         npcRecipe.showInRecipeBook = GsonHelper.getAsBoolean(jsonObject, "show_notification", true);
         npcRecipe.ignoreDamage = GsonHelper.getAsBoolean(jsonObject, "ignore_damage", false);
         npcRecipe.ignoreNBT = GsonHelper.getAsBoolean(jsonObject, "ignore_nbt", false);
         npcRecipe.isKnown = GsonHelper.getAsBoolean(jsonObject, "is_known", false);
         npcRecipe.name = GsonHelper.getAsString(jsonObject, "name", idIn.getPath()); // name
         CompoundTag compound = getAsCompoundTag(jsonObject, "availability", new CompoundTag());
         npcRecipe.availability.load(compound);
         return npcRecipe;
      }

      @Override
      public RecipeCarpentry fromNetwork(@Nonnull ResourceLocation idIn, FriendlyByteBuf buf) {
         int width = buf.readVarInt(); // width
         int height = buf.readVarInt(); // height
         String group = buf.readUtf(); // group
         NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
         ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buf)); // ingredients
         ItemStack itemstack = buf.readItem(); // result
         RecipeCarpentry npcRecipe = new RecipeCarpentry(idIn, group, width, height,
                 buf.readBoolean(), // isGlobal
                 buf.readBoolean(), // isShaped
                 ingredients, itemstack);
         npcRecipe.showInRecipeBook = buf.readBoolean(); // showInRecipeBook
         npcRecipe.ignoreDamage = buf.readBoolean(); // ignoreDamage
         npcRecipe.ignoreNBT = buf.readBoolean(); // ignoreNBT
         npcRecipe.isKnown = buf.readBoolean(); // isKnown
         npcRecipe.name = buf.readUtf(); // name
         npcRecipe.availability.load(Objects.requireNonNull(buf.readNbt())); // availability
         return npcRecipe;
      }

      @Override
      public void toNetwork(FriendlyByteBuf buf, RecipeCarpentry npcRecipe) {
         buf.writeVarInt(npcRecipe.width); // width
         buf.writeVarInt(npcRecipe.height); // height
         buf.writeUtf(npcRecipe.group); // group
         for(Ingredient ingredient : npcRecipe.getIngredients()) { ingredient.toNetwork(buf); } // ingredients
         buf.writeItem(npcRecipe.result); // result
         buf.writeBoolean(npcRecipe.isGlobal); // isGlobal
         buf.writeBoolean(npcRecipe.isShaped); // isShaped
         buf.writeBoolean(npcRecipe.showInRecipeBook); // showInRecipeBook
         buf.writeBoolean(npcRecipe.ignoreDamage); // ignoreDamage
         buf.writeBoolean(npcRecipe.ignoreNBT); // ignoreNBT
         buf.writeBoolean(npcRecipe.isKnown); // isKnown
         buf.writeUtf(npcRecipe.name); // name
         buf.writeNbt(npcRecipe.availability.save(new CompoundTag())); // availability
      }

      private static NonNullList<Ingredient> itemsFromJson(JsonArray jsonIngredients) {
         NonNullList<Ingredient> nonnulllist = NonNullList.create();
         for(int i = 0; i < jsonIngredients.size(); ++i) { nonnulllist.add(Ingredient.fromJson(jsonIngredients.get(i), false)); }
         return nonnulllist;
      }

   }

}
