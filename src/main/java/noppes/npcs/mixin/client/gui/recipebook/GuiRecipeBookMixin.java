package noppes.npcs.mixin.client.gui.recipebook;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.recipebook.GhostRecipe;
import net.minecraft.client.gui.recipebook.GuiButtonRecipeTab;
import net.minecraft.client.gui.recipebook.GuiRecipeBook;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.client.ClientRegisterEvents;
import noppes.npcs.client.gui.player.GuiNpcCarpentryBench;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.data.RecipeCarpentry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(value = GuiRecipeBook.class, priority = 498)
public class GuiRecipeBookMixin {

    @Mutable @Final @Shadow private List<GuiButtonRecipeTab> recipeTabs;
    @Final @Shadow private GhostRecipe ghostRecipe;
    @Shadow private InventoryCrafting craftingSlots;
    @Shadow private Minecraft mc;
    @Shadow private int xOffset;
    @Shadow private int width;
    @Shadow private int height;

    /**
     * The "func_194303_a" method is essentially the same as "initGui"
     *
     * @author BetaZavr
     * @reason Custom recipes require more information
     */
    @Inject(method = "func_194303_a", at = @At("HEAD"))
    public void npcs$init(int w, int h, @Nonnull Minecraft minecraft, boolean widthTooNarrow, @Nonnull InventoryCrafting inv, CallbackInfo ci) {
        if (minecraft.currentScreen instanceof GuiNpcCarpentryBench) {
            if (recipeTabs.isEmpty() ||
                    recipeTabs.get(0).getCategory() != ClientRegisterEvents.CRAFTING_CUSTOM_ANVIL_CATEGORY) {
                recipeTabs.clear();
                recipeTabs.add(new GuiButtonRecipeTab(0, ClientRegisterEvents.CRAFTING_CUSTOM_ANVIL_CATEGORY));
            }
        } else {
            if (recipeTabs.isEmpty() ||
                    recipeTabs.size() < 6 ||
                    recipeTabs.get(5).getCategory() != ClientRegisterEvents.CRAFTING_CUSTOM_GLOBAL_CATEGORY) {
                if (recipeTabs.size() >= 6) { recipeTabs.remove(5); }
                recipeTabs.add(5, new GuiButtonRecipeTab(5, ClientRegisterEvents.CRAFTING_CUSTOM_GLOBAL_CATEGORY));
            }
        }
    }

    /** Custom tabs are always visible */
    @Inject(at = {@At("TAIL")}, method = {"updateTabs"})
    private void npcs$updateTabs(CallbackInfo ci) {
        int x = (width - 147) / 2 - xOffset - 30;
        int y = (height - 166) / 2 + 3;
        int tabHeight = 27;
        int tabId = 0;
        for(GuiButtonRecipeTab tabButton : recipeTabs) {
            CreativeTabs category = tabButton.getCategory();
            if (category == CreativeTabs.SEARCH ||
                    category == ClientRegisterEvents.CRAFTING_CUSTOM_GLOBAL_CATEGORY ||
                    category == ClientRegisterEvents.CRAFTING_CUSTOM_ANVIL_CATEGORY) {
                tabButton.visible = true;
                tabButton.setPosition(x, y + tabHeight * tabId);
                if (category != CreativeTabs.SEARCH) { tabButton.startAnimation(mc); }
            }
            else if (tabButton.updateVisibility()) {
                tabButton.setPosition(x, y + tabHeight * tabId);
                tabButton.startAnimation(mc);
            }
            tabId++;
        }
    }

    @Inject(
            method = "setupGhostRecipe",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void npcs$setupGhostRecipe(IRecipe recipe, List<Slot> slots, CallbackInfo ci) {
        if (recipe instanceof RecipeCarpentry) {
            RecipeCarpentry npcRecipe = (RecipeCarpentry) recipe;
            ci.cancel();
            ItemStack itemstack = recipe.getRecipeOutput();
            ghostRecipe.setRecipe(recipe);
            ghostRecipe.addIngredient(Ingredient.fromStacks(itemstack), (slots.get(0)).xPos, (slots.get(0)).yPos);
            int gridWidth = craftingSlots.getWidth();
            int gridHeight = craftingSlots.getHeight();
            int recipeWidth = gridWidth;
            int recipeHeight = gridHeight;
            RecipeCarpentry temp = RecipeController.getInstance().getRecipe(recipe.getRegistryName());
            if (temp != null) { npcRecipe = temp; }
            List<Ingredient> ingredients = new ArrayList<>(npcRecipe.getIngredients());
            if (npcRecipe.isShaped()) {
                recipeWidth = npcRecipe.getWidth();
                recipeHeight = npcRecipe.getHeight();
            }
            else {
                ingredients = ingredients.stream().filter(ing -> ing.getMatchingStacks().length != 0).collect(Collectors.toList());
            }
            Iterator<Ingredient> intList = ingredients.iterator();
            // Exact width and height of the recipe
            int slotId = 0;
            // We go along the height of the prescription grid
            for (int row = 0; row < gridHeight; ++row) {
                // Check if the current row is in the top half of the grid
                boolean isUpperHalf = (float) recipeHeight < (float) gridHeight / 2.0F;
                // Calculating the indentation to center the recipe
                int verticalOffset = MathHelper.floor((float) gridHeight / 2.0F - (float) recipeHeight / 2.0F);
                // If the recipe is in the top half and the indent is greater than the current line
                if (isUpperHalf && verticalOffset > row) {
                    slotId += gridWidth;
                    ++row;
                }
                // We go through the width of the prescription grid
                for (int column = 0; column < gridWidth; ++column) {
                    // Checking if there are any more ingredients
                    if (!intList.hasNext()) { return; }
                    // Check if the current column is in the left half of the grid
                    isUpperHalf = (float) recipeWidth < (float)gridWidth / 2.0F;
                    // Calculating the horizontal indentation to center the recipe
                    int horizontalOffset = MathHelper.floor((float)gridWidth / 2.0F - (float) recipeWidth / 2.0F);
                    // Maximum recipe column
                    int maxColumn = recipeWidth;
                    // Check if the current column is within the recipe
                    boolean isWithinRecipe = column < recipeWidth;
                    // If the recipe is centered
                    if (isUpperHalf) {
                        maxColumn = horizontalOffset + recipeWidth;
                        isWithinRecipe = horizontalOffset <= column && column < horizontalOffset + recipeWidth;
                    }
                    // If the current cell is within the recipe
                    if (isWithinRecipe) {
                        Ingredient ingredient = intList.next();
                        if (ingredient.getMatchingStacks().length != 0) {
                            ghostRecipe.addIngredient(ingredient, (slots.get(slotId + 1)).xPos, (slots.get(slotId + 1)).yPos);
                        }
                    }
                    // If you have reached the maximum column
                    else if (maxColumn == column) {
                        slotId += gridWidth - column;
                        break;
                    }
                    ++slotId;
                }
            }
        }
    }

}
