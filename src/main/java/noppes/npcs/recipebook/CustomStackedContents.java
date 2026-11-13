package noppes.npcs.recipebook;

import com.google.common.collect.Lists;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.controllers.data.RecipeCarpentry;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class CustomStackedContents extends RecipeItemHelper {

    public final HashMap<ItemStack, Integer> contents = new HashMap<>(); // {itemId, count}
    protected RecipeCarpentry npcRecipe;

    @Override
    public void accountStack(@Nonnull ItemStack stack) { accountStack(stack, 64); }

    @Override
    public void accountStack(@Nonnull ItemStack stack, int countIn) {
        if (!stack.isEmpty()) {
            put(stack, Math.min(countIn, stack.getCount()));
        }
    }

    public boolean has(@Nonnull ItemStack stack) { return getKey(stack) != null; }

    public @Nullable ItemStack getKey(@Nonnull ItemStack stack) {
        for (ItemStack key : new ArrayList<>(contents.keySet())) {
            if (contents.get(key) > 0) {
                if (npcRecipe != null) {
                    if (NoppesUtilPlayer.compareItems(stack, key, npcRecipe.getIgnoreDamage(), npcRecipe.getIgnoreNBT())) { return key; }
                }
                else if (!key.isEmpty() && ItemStack.areItemStacksEqual(stack, key) &&
                        !stack.isItemDamaged() && !stack.isItemEnchanted() && !stack.hasDisplayName()) { return key; }
            }
        }
        return null;
    }

    public @Nullable ItemStack take(@Nonnull ItemStack stack, int amountIn) {
        ItemStack key = getKey(stack);
        if (key != null) {
            int amount = contents.get(key);
            if (amount > 0 && amount >= amountIn) {
                contents.put(key, amount - amountIn);
                ItemStack itemStack = key.copy();
                itemStack.setCount(amountIn);
                return itemStack;
            }
        }
        return null;
    }

    void put(@Nonnull ItemStack stack, int amount) {
        ItemStack key = getKey(stack);
        if (key == null) {
            key = stack.copy();
            key.setCount(1);
            contents.put(key, 0);
        }
        contents.put(key, contents.get(key) + amount);
    }

    public boolean canNpcCraft(RecipeCarpentry npcRecipeIn) {
        npcRecipe = npcRecipeIn;
        return (new CustomRecipePicker(npcRecipe, null)).tryPick(1, true) != null;
    }

    public @Nullable Map<Integer, ItemStack> getCraftableStack(RecipeCarpentry npcRecipe, int craftSize, InventoryCrafting menu) {
        return (new CustomRecipePicker(npcRecipe, menu)).tryPickAll(craftSize);
    }

    public void clear() {
        contents.clear();
        npcRecipe = null;
    }

    public void setRecipe(RecipeCarpentry npcRecipeIn) { npcRecipe = npcRecipeIn; }

    public class CustomRecipePicker {

        private final RecipeCarpentry recipe;
        private final Map<Integer, ItemStack> ingredientsToSlots = new HashMap<>();

        public CustomRecipePicker(RecipeCarpentry npcRecipeIn, @Nullable InventoryCrafting menu) {
            recipe = npcRecipeIn;
            List<Ingredient> ingredients = Lists.newArrayList();
            ingredients.addAll(npcRecipeIn.getIngredients());
            if (menu == null || !recipe.isShaped()) {
                ingredients.clear();
                for (Ingredient ingredient : npcRecipeIn.getIngredients()) {
                    if (ingredient.getMatchingStacks().length != 0) { ingredients.add(ingredient); }
                }
            }
            if (menu != null && recipe.isShaped()) {
                int gridWidth = menu.getWidth();
                int gridHeight = menu.getHeight();
                int width = recipe.getWidth();
                int height = recipe.getHeight();
                int ingrId = 0;
                for(int slotId = 0; slotId < gridWidth * gridHeight; slotId++) { ingredientsToSlots.put(slotId, ItemStack.EMPTY); }
                for(Ingredient ingredient : ingredients) {
                    int row = (int) Math.floor((double) ingrId / (double) width);
                    int col = ingrId % width;
                    ingredientsToSlots.put(row * gridWidth + col, getMatchingStack(ingredient));
                    ingrId++;
                    if (ingrId >= width * height) { break; }
                }
            } else {
                int slotId = 0;
                for(Ingredient ingredient : ingredients) { ingredientsToSlots.put(slotId++, getMatchingStack(ingredient)); }
            }
        }

        private @Nonnull ItemStack getMatchingStack(Ingredient ingredient) {
            ItemStack[] items = ingredient.getMatchingStacks();
            ItemStack stack = ItemStack.EMPTY;
            for (ItemStack key : contents.keySet()) {
                if (!key.isEmpty()) {
                    boolean isStart = true;
                    for (ItemStack target : items) {
                        if (isStart && !target.isEmpty()) {
                            isStart = false;
                            stack = target.copy();
                            stack.setCount(target.getCount());
                        }
                        if (NoppesUtilPlayer.compareItems(target, key, recipe.getIgnoreDamage(), recipe.getIgnoreNBT())) {
                            stack = key.copy();
                            stack.setCount(target.getCount());
                            break;
                        }
                    }
                }
            }
            return stack;
        }

        public @Nullable Map<Integer, ItemStack> tryPick(int craftSize, boolean notTake) {
            if (craftSize <= 0 || ingredientsToSlots.isEmpty() || !canMakeCraft(craftSize)) { return null; }
            Map<Integer, ItemStack> map = new HashMap<>();
            HashMap<ItemStack, Integer> temp = new HashMap<>(contents);
            for(Map.Entry<Integer, ItemStack> entry : ingredientsToSlots.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    ItemStack stack;
                    int amount = entry.getValue().getCount() * craftSize;
                    if (notTake) {
                        stack = getKey(entry.getValue());
                        if (stack == null || temp.get(stack) < amount) { return null; }
                        else { temp.put(stack, temp.get(stack) - amount); }
                    }
                    else {
                        stack = take(entry.getValue(), entry.getValue().getCount() * craftSize);
                        if (stack == null) { return null; }
                        else { map.put(entry.getKey(), stack); }
                    }
                }
                else { map.put(entry.getKey(), ItemStack.EMPTY); }
            }
            return map;
        }

        public @Nullable Map<Integer, ItemStack> tryPickAll(int craftSize) {
            return tryPick(getMaxCraftSize(craftSize), false);
        }

        private boolean canMakeCraft(int craftSize) {
            HashMap<ItemStack, Integer> temp = new HashMap<>(contents);
            for(Map.Entry<Integer, ItemStack> entry : ingredientsToSlots.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    ItemStack key = getKey(entry.getValue());
                    if (key == null) { return false; }
                    int amount = entry.getValue().getCount() * craftSize;
                    if (temp.get(key) >= amount) { temp.put(key, temp.get(key) - amount); }
                    else { return false; }
                }
            }
            return true;
        }

        private int getMaxCraftSize(int requiredCraftSize) {
            int craftSize = Integer.MAX_VALUE;
            Map<ItemStack, Integer> map = new HashMap<>();
            for (Map.Entry<Integer, ItemStack> entry : ingredientsToSlots.entrySet()) {
                ItemStack key = getKey(entry.getValue());
                if (key != null) {
                    if (!map.containsKey(key)) { map.put(key, 0); }
                    map.put(key, map.get(key) + entry.getValue().getCount());
                }
            }
            for(Map.Entry<ItemStack, Integer> entry : map.entrySet()) {
                ItemStack key = entry.getKey();
                if (!key.isEmpty()) {
                    double size = entry.getValue();
                    craftSize = ValueUtil.min(craftSize, (int) Math.floor((double) key.getMaxStackSize() / size), (int) Math.floor((double) contents.get(key) / size));
                    if (craftSize == 0) { break; }
                }
            }
            return Math.min(craftSize, requiredCraftSize);
        }

    }

}
