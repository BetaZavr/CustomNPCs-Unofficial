package noppes.npcs.api.wrapper.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.RecipeCarpentry;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class WrapperRecipe {

    public final @Nonnull Map<Integer, ItemStack[]> ingredients = new TreeMap<>(); // -> ItemStack[].length == 0 ... 16 max (not null)
    public @Nonnull Availability availability = new Availability();
    public @Nonnull ResourceLocation id = new ResourceLocation(CustomNpcs.MODID, "");
    public @Nonnull ItemStack product = ItemStack.EMPTY;
    public boolean isGlobal = true;
    public boolean isKnown = false;
    public boolean ignoreDamage = false;
    public boolean ignoreNBT = false;
    public boolean isShaped = true;
    public int width = 3;
    public int height = 3;
    public Component group = Component.empty();
    public CraftingRecipe parent = null;

    public void clear() {
        ingredients.clear();
        availability.clear();
        id = new ResourceLocation(CustomNpcs.MODID, "");
        product = ItemStack.EMPTY;
        group = Component.empty();
        parent = null;
    }

    public CompoundTag getNbt() {
        CompoundTag compound = new CompoundTag();
        compound.putString("Id", id.toString());
        compound.putString("Name", id.getPath());
        compound.putString("Group", group.getString());
        compound.put("Item", product.save(new CompoundTag()));
        int size = isGlobal ? 3 : 4;
        int maxX = 0;
        int maxY = 0;
        int minX = -1;
        int minY = -1;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int slotId = x + y * size;
                if (ingredients.get(slotId).length != 0) {
                    if (maxX < x) { maxX = x; }
                    if (maxY < y) { maxY = y; }
                    if (minX == -1 && minY == -1) { minX = x; minY = y; }
                }
            }
        }
        maxX += 1;
        maxY += 1;
        if (minX < 0) { minX = 0; }
        if (minY < 0) { minY = 0; }
        width = maxX - minX;
        height = maxY - minY;
        compound.putInt("Width", width);
        compound.putInt("Height", height);
        int nsPos = 0;
        ListTag list = new ListTag();
        for (int slotId = 0; slotId < size * size; slotId++) {
            CompoundTag nbt = new CompoundTag();
            ListTag ings = new ListTag();
            if (!isShaped) {
                if (ingredients.get(slotId) != null) {
                    for (ItemStack ing : ingredients.get(slotId)) { ings.add(ing.save(new CompoundTag())); }
                    if (!ings.isEmpty()) {
                        nbt.putByte("Slot", (byte) (nsPos++));
                        nbt.put("Ingredients", ings);
                    }
                }
            }
            else {
                int x = slotId % size;
                int y = (int) Math.floor((double) slotId / (double) size);
                if (x >= minX && y >= minY && x < minX + width && y < minY + height) {
                    x -= minX;
                    y -= minY;
                    for (ItemStack ing : ingredients.get(slotId)) { ings.add(ing.save(new CompoundTag())); }
                    nbt.putByte("Slot", (byte) (x + y * width));
                    nbt.put("Ingredients", ings);
                }
            }
            if (nbt.contains("Slot", 1)) { list.add(nbt); }
        }
        compound.put("Materials", list);
        compound.put("Availability", availability.save(new CompoundTag()));
        compound.putBoolean("Global", isGlobal);
        compound.putBoolean("IgnoreDamage", ignoreDamage);
        compound.putBoolean("IgnoreNBT", ignoreNBT);
        compound.putBoolean("IsKnown", isKnown);
        compound.putBoolean("IsShaped", isShaped);
        compound.putBoolean("ShowInRecipeBook", true);
        return compound;
    }

    public boolean isValid() {
        if (id.getPath().isEmpty() || !id.getNamespace().equals(CustomNpcs.MODID) ||
                group.getString().isEmpty() || ingredients.isEmpty() || product.isEmpty()) {
            return false;
        }
        for (ItemStack[] array : new ArrayList<>(ingredients.values())) {
            if (array == null) { continue; }
            for (ItemStack stack : array) {
                if (stack != null && !stack.isEmpty()) { return true; }
            }
        }
        return false;
    }

    public static WrapperRecipe of(@Nonnull Player player, @Nonnull CraftingRecipe recipe) {
        if (recipe instanceof RecipeCarpentry npcRecipe) { return npcRecipe.getWrapperRecipe(); }
        WrapperRecipe wrapper = new WrapperRecipe();
        wrapper.parent = recipe;
        wrapper.isGlobal = true;
        wrapper.isKnown = false;
        wrapper.ignoreDamage = false;
        wrapper.ignoreNBT = false;
        wrapper.id = recipe.getId();
        NonNullList<Ingredient> ings = recipe.getIngredients();
        if (recipe instanceof ShapedRecipe sRecipe) {
            wrapper.isShaped = true;
            wrapper.width = sRecipe.getRecipeWidth();
            wrapper.height = sRecipe.getRecipeHeight();
            for (int slotId = 0; slotId < 9; slotId++) {
                int x = slotId % 3;
                int y = (int) Math.floor((double) slotId / 3.0d);
                if (x < wrapper.width && y < wrapper.height) { wrapper.ingredients.put(slotId, ings.get(x + y * wrapper.width).getItems()); }
                else { wrapper.ingredients.put(slotId, new ItemStack[0]); }
            }
        }
        else {
            wrapper.isShaped = false;
            int size = ings.size();
            wrapper.width = Math.max(1, size / 2);
            wrapper.height = Math.max(1, size - wrapper.width);
            for (int slotId = 0; slotId < 9; slotId++) {
                if (slotId < size) { wrapper.ingredients.put(slotId, ings.get(slotId).getItems()); }
                else { wrapper.ingredients.put(slotId, new ItemStack[0]); }
            }
        }
        String groupName = recipe.getGroup().isEmpty() ? wrapper.id.getPath() : recipe.getGroup();
        if (!wrapper.id.getNamespace().equals(CustomNpcs.MODID)) {
            wrapper.group = Component.literal(groupName).withStyle(ChatFormatting.GRAY);
        } else {
            wrapper.group = Component.literal(groupName);
        }
        wrapper.product = recipe.getResultItem(player.level().registryAccess());
        return wrapper;
    }

    public Component getName() {
        MutableComponent name = Component.literal(id.getPath());
        if (!id.getNamespace().equals(CustomNpcs.MODID)) { name = name.withStyle(ChatFormatting.GRAY); }
        return name;
    }

}
