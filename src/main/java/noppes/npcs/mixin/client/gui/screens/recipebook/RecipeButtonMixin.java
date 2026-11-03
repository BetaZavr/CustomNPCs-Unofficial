package noppes.npcs.mixin.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.crafting.IShapedRecipe;
import noppes.npcs.controllers.data.RecipeCarpentry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = RecipeButton.class, priority = 498)
public class RecipeButtonMixin {

    @Final @Shadow private static ResourceLocation RECIPE_BOOK_LOCATION;
    @Final @Shadow private static Component MORE_RECIPES_TOOLTIP;
    @Shadow private RecipeBookMenu<?> menu;
    @Shadow private RecipeBook book;
    @Shadow private RecipeCollection collection;
    @Shadow private float time;
    @Shadow private float animationTime;
    @Shadow private int currentIndex;

    @Unique private int npcs$availabilityType = 0; // 0: normal; 1-not; 2-combo
    @Unique private List<Component> npcs$availabilityList = null;

    /** npc recipe */
    @Inject(at = {@At("HEAD")}, method = {"renderWidget"}, cancellable = true)
    private void npcs$renderWidget(GuiGraphics graphics, int x, int y, float partialTicks, CallbackInfo ci) {
        RecipeButton parent = (RecipeButton) (Object) this;
        if (parent.getCollection() == null) { return; }
        IRecipeButtonMixin mixin = (IRecipeButtonMixin) this;
        Recipe<?> recipe = null;
        try { recipe = mixin.invokeGetRecipe(); } catch (Exception ignored) { }
        if (parent.visible && recipe instanceof RecipeCarpentry npcRecipe) {
            ci.cancel();
            Minecraft mc = Minecraft.getInstance();
            List<Recipe<?>> listOrderedRecipes = mixin.invokeGetOrderedRecipes();
            npcs$availabilityType = -1;
            npcs$availabilityList = null;
            for (Recipe<?> r : listOrderedRecipes) {
                if (r instanceof RecipeCarpentry) {
                    if (((RecipeCarpentry) recipe).availability.hasOptions()) {
                        if (npcs$availabilityList == null) {
                            npcs$availabilityList = new ArrayList<>();
                            npcs$availabilityList.addAll(((RecipeCarpentry) recipe).availability
                                    .getAvailability(mc.player, Component.translatable("recipe.hover.availability.type")));
                        }
                        if (((RecipeCarpentry) recipe).availability.isAvailable(mc.player)) {
                            if (npcs$availabilityType == -1) { npcs$availabilityType = 0; }
                            else if (npcs$availabilityType == 1) { npcs$availabilityType = 2; }
                        }
                        else {
                            if (npcs$availabilityType == 0) { npcs$availabilityType = 2; }
                            else if (npcs$availabilityType != 2) { npcs$availabilityType = 1; }
                        }
                    }
                }
            }
            if (npcs$availabilityType == -1) { npcs$availabilityType = 0; }
            // vanilla
            if (!Screen.hasControlDown()) { time += partialTicks; }
            int u = 29;
            if (!collection.hasCraftable() || !npcRecipe.availability.isAvailable(mc.player)) { u += 25; }
            int v = 206;
            if (collection.getRecipes(book.isFiltering(menu)).size() > 1) { v += 25; }
            boolean isAnimated = animationTime > 0.0F;
            if (isAnimated) {
                float f = 1.0F + 0.1F * (float)Math.sin(animationTime / 15.0F * (float)Math.PI);
                graphics.pose().pushPose();
                graphics.pose().translate((float)(parent.getX() + 8), (float)(parent.getY() + 12), 0.0F);
                graphics.pose().scale(f, f, 1.0F);
                graphics.pose().translate((float)(-(parent.getX() + 8)), (float)(-(parent.getY() + 12)), 0.0F);
                animationTime -= partialTicks;
            }
            graphics.blit(RECIPE_BOOK_LOCATION, parent.getX(), parent.getY(), u, v, parent.getWidth(), parent.getHeight());
            currentIndex = Mth.floor(time / 30.0F) % listOrderedRecipes.size();
            ItemStack itemstack = listOrderedRecipes.get(currentIndex).getResultItem(collection.registryAccess());
            int k = 4;
            if (collection.hasSingleResultItem() && listOrderedRecipes.size() > 1) {
                graphics.renderItem(itemstack, parent.getX() + k + 1, parent.getY() + k + 1, 0, 10);
                --k;
            }
            graphics.renderFakeItem(itemstack, parent.getX() + k, parent.getY() + k);
            if (isAnimated) { graphics.pose().popPose(); }
        }

    }

    /** add availability info from npc recipe */
    @Inject(at = {@At("RETURN")}, method = {"getTooltipText"}, cancellable = true)
    public void npcs$getTooltipText(CallbackInfoReturnable<List<Component>> cir) {
        RecipeButton parent = (RecipeButton) (Object) this;
        if (parent.getCollection() == null) { return; }
        IRecipeButtonMixin mixin = (IRecipeButtonMixin) this;
        Recipe<?> recipe = null;
        try { recipe = mixin.invokeGetRecipe(); } catch (Exception ignored) { }
        if (recipe instanceof RecipeCarpentry npcRecipe) {
            if (npcs$availabilityList != null && Screen.hasShiftDown()) { cir.setReturnValue(npcs$availabilityList); }
            else {
                List<Component> hovers = Lists.newArrayList(Screen.getTooltipFromItem(Minecraft.getInstance(),
                        recipe.getResultItem(collection.registryAccess())));
                hovers.add(Component.empty());
                hovers.add(Component.translatable("gui.type").append(": ")
                        .append(Component.translatable("item.craft.type."+(recipe instanceof IShapedRecipe || npcRecipe.isShaped()))));
                if (npcs$availabilityType != 0) {
                    hovers.add(Component.translatable("gui.recipebook.availability."+ npcs$availabilityType));
                }
                if (npcs$availabilityList != null) {
                    hovers.add(Component.translatable("gui.recipebook.availability.3"));
                }
                if (collection.getRecipes(book.isFiltering(menu)).size() > 1) {
                    hovers.add(MORE_RECIPES_TOOLTIP);
                }
                cir.setReturnValue(hovers);
            }


        }
    }

}
