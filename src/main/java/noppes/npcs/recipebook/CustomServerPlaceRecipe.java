package noppes.npcs.recipebook;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.controllers.data.RecipeCarpentry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomServerPlaceRecipe {

    protected final InventoryPlayer inventory;
    protected final InventoryCrafting menu;
    protected final InventoryCraftResult inventoryResult;
    protected final List<Slot> inventorySlots;
    protected final CustomStackedContents customStackedContents;

    public CustomServerPlaceRecipe(IRecipe npcRecipe, boolean isShiftPress, InventoryPlayer inventoryIn,
                                   InventoryCrafting menuIn, InventoryCraftResult inventoryResultIn,
                                   List<Slot> inventorySlotsIn, CustomStackedContents customStackedContentsIn) {
        customStackedContents = customStackedContentsIn;
        inventory = inventoryIn;
        menu = menuIn;
        inventoryResult = inventoryResultIn;
        inventorySlots = inventorySlotsIn;
        handleRecipeClicked(npcRecipe, isShiftPress);
    }

    public void handleRecipeClicked(IRecipe recipe, boolean isShiftPress) {
        RecipeCarpentry npcRecipe = (RecipeCarpentry) recipe;
        int craftSize = isShiftPress ? Integer.MAX_VALUE : 1;
        if (!isShiftPress && recipe.matches(menu, inventory.player.world)) { craftSize = getStackSize(npcRecipe) + 1; }
        if (craftSize != 0) {
            Map<Integer, ItemStack> ingredients = customStackedContents.getCraftableStack(npcRecipe, craftSize, menu);
            if (ingredients != null) {
                clearGrid();
                for (Map.Entry<Integer, ItemStack> entry : ingredients.entrySet()) {
                    ItemStack stack = entry.getValue();
                    if (!stack.isEmpty()) {
                        ItemStack oneItem = stack.copy();
                        oneItem.setCount(1);
                        for(int i = 0; i < stack.getCount(); ++i) {
                            moveItemToGrid(npcRecipe, inventorySlots.get(entry.getKey() + 1), oneItem);
                        }
                    }
                }
                inventoryResult.setInventorySlotContents(0, npcRecipe.getResult().getMCItemStack());
            }
        }
    }

    private int getStackSize(RecipeCarpentry npcRecipe) {
        List<Ingredient> ingredients = Lists.newArrayList();
        ingredients.addAll(npcRecipe.getIngredients());
        int craftSize = Integer.MAX_VALUE;
        if (npcRecipe.isShaped()) {
            int sX = 0;
            int sY = 0;
            int slotId;
            for (slotId = 0; slotId < menu.getWidth() * menu.getHeight(); slotId++) {
                if (!menu.getStackInSlot(slotId).isEmpty()) {
                    sX = slotId % menu.getWidth();
                    sY = (int) Math.floor((double) slotId / (double) menu.getHeight());
                    break;
                }
            }
            slotId = 0;
            for(Ingredient ingredient : ingredients) {
                if (ingredient.getMatchingStacks().length != 0) {
                    int slot = sX + slotId % npcRecipe.getWidth() +
                            (sY + (int) Math.floor((double) slotId / (double) npcRecipe.getWidth())) * menu.getWidth();
                    ItemStack stack = menu.getStackInSlot(slot);
                    for (ItemStack target : ingredient.getMatchingStacks()) {
                        if (NoppesUtilPlayer.compareItems(target, stack, npcRecipe.getIgnoreDamage(), npcRecipe.getIgnoreNBT())) {
                            craftSize = (int) Math.min(craftSize, Math.floor((double) stack.getCount() / (double) target.getCount()));
                            break;
                        }
                    }
                }
                slotId++;
            }
        }
        else {
            HashMap<ItemStack, Integer> contents = new HashMap<>();
            for (int slotId = 0; slotId < menu.getWidth() * menu.getHeight(); slotId++) {
                ItemStack stack = menu.getStackInSlot(slotId);
                ItemStack key = customStackedContents.getKey(stack);
                if (key != null) {
                    if (!contents.containsKey(key)) { contents.put(key, 0); }
                    contents.put(key, contents.get(key) + stack.getCount());
                }
            }
            for(Ingredient ingredient : ingredients) {
                if (ingredient.getMatchingStacks().length != 0) {
                    for (ItemStack key : contents.keySet()) {
                        for (ItemStack target : ingredient.getMatchingStacks()) {
                            if (NoppesUtilPlayer.compareItems(target, key, npcRecipe.getIgnoreDamage(), npcRecipe.getIgnoreNBT())) {
                                craftSize = (int) Math.min(craftSize, Math.floor((double) contents.get(key) / (double) target.getCount()));
                                break;
                            }
                        }
                    }
                }
            }
        }
        return craftSize == Integer.MAX_VALUE ? 0 : craftSize;
    }

    protected void clearGrid() {
        for (int i = 0; i < menu.getSizeInventory(); ++i) {
            ItemStack itemstack = menu.getStackInSlot(i);
            if (!itemstack.isEmpty()) {
                while (itemstack.getCount() > 0)  {
                    int j = inventory.storeItemStack(itemstack);
                    if (j == -1) { j = inventory.getFirstEmptyStack(); }
                    ItemStack itemstack1 = itemstack.copy();
                    itemstack1.setCount(1);
                    inventory.add(j, itemstack1);
                    menu.decrStackSize(i, 1);
                }
            }
        }
        menu.clear();
        inventoryResult.clear();
    }

    protected void moveItemToGrid(RecipeCarpentry npcRecipe, Slot slot, ItemStack stack) {
        int slotId = findSlotMatchingUnusedItem(npcRecipe, stack);
        if (slotId != -1) {
            ItemStack itemstack = inventory.getStackInSlot(slotId).copy();
            if (!itemstack.isEmpty()) {
                if (itemstack.getCount() > 1)  { inventory.decrStackSize(slotId, 1); }
                else { inventory.removeStackFromSlot(slotId); }
                itemstack.setCount(1);
                if (slot.getStack().isEmpty()) { slot.putStack(itemstack); }
                else { slot.getStack().grow(1); }
            }
        }
    }

    protected int findSlotMatchingUnusedItem(RecipeCarpentry npcRecipe, ItemStack stack) {
        for(int slotId = 0; slotId < inventory.mainInventory.size(); ++slotId) {
            ItemStack itemstack = inventory.mainInventory.get(slotId);
            if (NoppesUtilPlayer.compareItems(stack, itemstack, npcRecipe.getIgnoreDamage(), npcRecipe.getIgnoreNBT())) {
                return slotId;
            }
        }
        return -1;
    }

}
