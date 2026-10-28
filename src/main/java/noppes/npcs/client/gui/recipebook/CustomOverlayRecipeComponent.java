package noppes.npcs.client.gui.recipebook;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.recipebook.PlaceRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.crafting.IShapedRecipe;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.data.RecipeCarpentry;
import noppes.npcs.shared.client.gui.GuiBasic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

// Displaying variations of one recipe in the GUI recipe window
@OnlyIn(Dist.CLIENT)
public class CustomOverlayRecipeComponent extends OverlayRecipeComponent {

    protected static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");
    protected static final int MAX_ROW = 4;
    protected static final int MAX_ROW_LARGE = 5;
    protected static final float ITEM_RENDER_SCALE = 0.375F;
    public static int BUTTON_SIZE = 25;

    protected final List<OverlayRecipeButton> recipeButtons = Lists.newArrayList();
    protected boolean isVisible;
    protected int x;
    protected int y;
    protected RecipeCollection collection;
    @Nullable protected Recipe<?> lastRecipeClicked;
    protected float time;
    protected boolean isFurnaceMenu;

    // New from Unofficial (BetaZavr)
    protected static final ResourceLocation CUSTOM_RECIPE_BOOK_LOCATION = new ResourceLocation(CustomNpcs.MODID, "textures/gui/recipe_book.png");

    @Override
    public void init(@Nonnull Minecraft minecraftIn, @Nonnull RecipeCollection collectionIn,
                     int xIn, int yIn, int viewportWidth, int viewportHeight, float uiScalingFactor) {
        collection = collectionIn;
        if (minecraftIn.player != null && minecraftIn.player.containerMenu instanceof AbstractFurnaceMenu) {
            isFurnaceMenu = true;
        }
        boolean isFiltering = minecraftIn.player != null && minecraftIn.player.getRecipeBook().isFiltering((RecipeBookMenu<?>) minecraftIn.player.containerMenu);
        List<Recipe<?>> listCraftable = collection.getDisplayRecipes(true);
        List<Recipe<?>> listNotCraftable = isFiltering ? Collections.emptyList() : collection.getDisplayRecipes(false);

        Recipe<?> recipe = null;
        if (!listCraftable.isEmpty()) { recipe = listCraftable.get(0); }
        else if (!listNotCraftable.isEmpty()) { recipe = listNotCraftable.get(0); }
        BUTTON_SIZE = 25;
        if (recipe instanceof RecipeCarpentry npcRecipe && !npcRecipe.isGlobal) { BUTTON_SIZE = 32; }

        int craftableSize = listCraftable.size();
        int buttonAmount = craftableSize + listNotCraftable.size();
        int borderSize = buttonAmount <= 16 ? MAX_ROW : MAX_ROW_LARGE;
        int l = (int) Math.ceil((float) buttonAmount / (float) borderSize);
        x = xIn;
        y = yIn;

        float f = (float)(x + Math.min(buttonAmount, borderSize) * BUTTON_SIZE);
        float f1 = (float) (viewportWidth + BUTTON_SIZE * 2);
        if (f > f1) {
            x = (int)((float)x - uiScalingFactor * (float)((int)((f - f1) / uiScalingFactor)));
        }
        float f2 = (float)(y + l * BUTTON_SIZE);
        float f3 = (float)(viewportHeight + BUTTON_SIZE * 2);
        if (f2 > f3) {
            y = (int)((float)y - uiScalingFactor * (float) Mth.ceil((f2 - f3) / uiScalingFactor));
        }
        float f4 = (float)y;
        float f5 = (float)(viewportHeight - BUTTON_SIZE * 4);
        if (f4 < f5) {
            y = (int)((float)y - uiScalingFactor * (float)Mth.ceil((f4 - f5) / uiScalingFactor));
        }
        isVisible = true;
        recipeButtons.clear();
        for(int buttonId = 0; buttonId < buttonAmount; ++buttonId) {
            boolean isCraftable = buttonId < craftableSize;
            recipe = isCraftable ? listCraftable.get(buttonId) : listNotCraftable.get(buttonId - craftableSize);
            int buttonX = x + MAX_ROW + BUTTON_SIZE * (buttonId % borderSize);
            int buttonY = y + MAX_ROW_LARGE + BUTTON_SIZE * (buttonId / borderSize);
            if (isFurnaceMenu) { recipeButtons.add(new OverlaySmeltingRecipeButton(buttonX, buttonY, recipe, isCraftable)); }
            else { recipeButtons.add(new OverlayRecipeButton(buttonX, buttonY, recipe, isCraftable)); }
        }
        lastRecipeClicked = null;
    }

    @Override
    public @Nonnull RecipeCollection getRecipeCollection() { return collection; }

    @Override
    public @Nullable Recipe<?> getLastRecipeClicked() { return lastRecipeClicked; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0) {
            for(OverlayRecipeButton recipeButton : recipeButtons) {
                if (recipeButton.mouseClicked(mouseX, mouseY, mouseButton)) {
                    lastRecipeClicked = recipeButton.recipe;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (isVisible) {
            time += partialTicks;
            // background
            PoseStack matrixStack = graphics.pose();
            RenderSystem.enableBlend();
            matrixStack.pushPose();
            matrixStack.translate(0.0F, 0.0F, 1000.0F);
            int i = recipeButtons.size() <= 16 ? MAX_ROW : MAX_ROW_LARGE;
            int u = Math.min(recipeButtons.size(), i);
            int v = Mth.ceil((float) recipeButtons.size() / (float)i);
            graphics.blitNineSliced(RECIPE_BOOK_LOCATION, x, y, u * BUTTON_SIZE + 8, v * BUTTON_SIZE + 8,
                    MAX_ROW, 32, 32, 82, 208);
            RenderSystem.disableBlend();
            // recipes
            for(OverlayRecipeButton recipeButton : recipeButtons) { recipeButton.render(graphics, mouseX, mouseY, partialTicks); }
            matrixStack.popPose();
        }
    }

    @Override
    public void setVisible(boolean isVisibleIn) { isVisible = isVisibleIn; }

    @Override
    public boolean isVisible() { return isVisible; }

    @OnlyIn(Dist.CLIENT)
    public class OverlayRecipeButton extends AbstractWidget implements PlaceRecipe<Ingredient> {

        protected final Recipe<?> recipe;
        protected final boolean isCraftable;
        protected final List<OverlayRecipeButton.Pos> ingredientPos = Lists.newArrayList();

        public OverlayRecipeButton(int x, int y, Recipe<?> recipeIn, boolean isCraftableIn) {
            super(x, y, 200, 20, CommonComponents.EMPTY);
            recipe = recipeIn;
            int size = 24;
            if (recipe instanceof RecipeCarpentry npcRecipe && !npcRecipe.isGlobal) { size = 31; }
            width = size;
            height = size;
            isCraftable = isCraftableIn;
            calculateIngredientsPositions();
        }

        protected void calculateIngredientsPositions() {
            int recipeWidth = 3;
            int recipeHeight = 3;
            if (recipe instanceof RecipeCarpentry npcRecipe) {
                recipeWidth = npcRecipe.getWidth();
                recipeHeight = npcRecipe.getHeight();
            }
            placeRecipe(recipeWidth, recipeHeight, -1, recipe, recipe.getIngredients().iterator(), 0);
        }

        @Override
        public void updateWidgetNarration(@Nonnull NarrationElementOutput narrationElementOutput) { defaultButtonNarrationText(narrationElementOutput); }

        @Override
        public void addItemToSlot(Iterator<Ingredient> iterator, int slotId, int amount, int row, int column) {
            ItemStack[] aitemstack = iterator.next().getItems();
            if (aitemstack.length != 0) {
                ingredientPos.add(new Pos(3 + column * 7, 3 + row * 7, aitemstack));
            }
        }

        @Override
        public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            // background
            boolean isNcpModRecipe = BUTTON_SIZE == 32;
            int u = isNcpModRecipe ? 0 : 152;
            int v = isNcpModRecipe ? 0 : isFurnaceMenu ? 130 : 78;
            if (!isCraftable) { u += isNcpModRecipe ? 33 : 26; }
            if (isHoveredOrFocused()) { v += isNcpModRecipe ? 33 : 26; }
            graphics.blit(isNcpModRecipe ? CUSTOM_RECIPE_BOOK_LOCATION : RECIPE_BOOK_LOCATION, getX(), getY(), u, v, width, height);
            // items
            PoseStack matrixStack = graphics.pose();
            matrixStack.pushPose();
            matrixStack.translate(getX() + 2, getY() + 2, 150.0D);
            for(OverlayRecipeButton.Pos recipeButtonPos : ingredientPos) {
                if (recipeButtonPos.ingredients.length > 0) {
                    matrixStack.pushPose();
                    matrixStack.translate(recipeButtonPos.x, recipeButtonPos.y, 0.0D);
                    matrixStack.scale(ITEM_RENDER_SCALE, ITEM_RENDER_SCALE, 1.0F);
                    GuiBasic.renderStackInGUI(graphics, recipeButtonPos.ingredients[Mth.floor(time / 30.0F) % recipeButtonPos.ingredients.length],
                            -8.0D, -8.0D, 0.0f, 1.0f, "");
                    matrixStack.popPose();
                }
            }
            matrixStack.popPose();
            if (!isFurnaceMenu && isHovered) {
                graphics.drawCenteredString(Minecraft.getInstance().font,
                        Component.translatable("item.craft.type." +
                                (recipe instanceof IShapedRecipe || recipe instanceof RecipeCarpentry npcRecipe && npcRecipe.isShaped())),
                        getX() + width / 2, getY() - 12, 0xFFFFFFFF);
            }
        }

        @OnlyIn(Dist.CLIENT)
        protected static class Pos {
            public final ItemStack[] ingredients;
            public final int x;
            public final int y;

            public Pos(int xIn, int yIn, ItemStack[] ingredientsIn) {
                x = xIn;
                y = yIn;
                ingredients = ingredientsIn;
            }
        }

    }

    @OnlyIn(Dist.CLIENT)
    public class OverlaySmeltingRecipeButton extends OverlayRecipeButton {

        public OverlaySmeltingRecipeButton(int x, int y, Recipe<?> recipe, boolean isCraftable) {
            super(x, y, recipe, isCraftable);
        }

        @Override
        protected void calculateIngredientsPositions() {
            ingredientPos.add(new Pos(10, 10, recipe.getIngredients().get(0).getItems()));
        }

    }

}
