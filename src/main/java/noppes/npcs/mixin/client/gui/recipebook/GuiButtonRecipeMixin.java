package noppes.npcs.mixin.client.gui.recipebook;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.recipebook.GuiButtonRecipe;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.crafting.IShapedRecipe;
import noppes.npcs.controllers.data.RecipeCarpentry;
import noppes.npcs.mixin.client.gui.IGuiButton;
import noppes.npcs.shared.client.gui.GuiBasic;
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

@Mixin(value = GuiButtonRecipe.class, priority = 498)
public class GuiButtonRecipeMixin {

    @Final @Shadow private static ResourceLocation RECIPE_BOOK;
    @Shadow private RecipeBook book;
    @Shadow private float time;
    @Shadow private float animationTime;
    @Shadow private int currentIndex;

    @Unique private int npcs$availabilityType = 0; // 0: normal; 1-not; 2-combo
    @Unique private List<String> npcs$availabilityList = null;

    /** npc recipe */
    @Inject(at = {@At("HEAD")}, method = {"drawButton"}, cancellable = true)
    public void npcs$drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GuiButtonRecipe parent = (GuiButtonRecipe) (Object) this;
        if (parent.getList() == null) { return; }
        IGuiButtonRecipeMixinMixin mixin = (IGuiButtonRecipeMixinMixin) this;
        IRecipe recipe = null;
        try { recipe = mixin.invokeGetRecipe(); } catch (Exception ignored) { }
        if (parent.visible && recipe instanceof RecipeCarpentry) {
            ci.cancel();
            RecipeCarpentry npcRecipe = (RecipeCarpentry) recipe;
            List<IRecipe> listOrderedRecipes = mixin.invokeGetOrderedRecipes();
            npcs$availabilityType = -1;
            npcs$availabilityList = null;
            for (IRecipe r : listOrderedRecipes) {
                if (r instanceof RecipeCarpentry) {
                    if (((RecipeCarpentry) recipe).availability.hasOptions()) {
                        if (npcs$availabilityList == null) {
                            npcs$availabilityList = new ArrayList<>();
                            for (Component tooltip : ((RecipeCarpentry) recipe).availability
                                    .getAvailability(mc.player, Component.translatable("recipe.hover.availability.type"))) {
                                npcs$availabilityList.add(tooltip.getFormattedText());
                            }
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
            if (!GuiScreen.isCtrlKeyDown()) { time += partialTicks; }
            ((IGuiButton) this).setHovered(mouseX >= parent.x && mouseY >= parent.y && mouseX < parent.x + parent.width && mouseY < parent.y + parent.height);
            mc.getTextureManager().bindTexture(RECIPE_BOOK);
            int i = 29;
            if (!parent.getList().containsCraftableRecipes() || !npcRecipe.availability.isAvailable(mc.player)) { i += 25; }
            int j = 206;
            if (parent.getList().getRecipes(book.isFilteringCraftable()).size() > 1) { j += 25; }
            boolean isAnimated = animationTime > 0.0F;
            if (isAnimated) {
                float f = 1.0F + 0.1F * (float)Math.sin(animationTime / 15.0F * (float)Math.PI);
                GlStateManager.pushMatrix();
                GlStateManager.translate((float)(parent.x + 8), (float)(parent.y + 12), 0.0F);
                GlStateManager.scale(f, f, 1.0F);
                GlStateManager.translate((float)(-(parent.x + 8)), (float)(-(parent.y + 12)), 0.0F);
                animationTime -= partialTicks;
            }
            parent.drawTexturedModalRect(parent.x, parent.y, i, j, parent.width, parent.height);
            currentIndex = MathHelper.floor(time / 30.0F) % listOrderedRecipes.size();
            ItemStack itemstack = listOrderedRecipes.get(currentIndex).getRecipeOutput();
            int k = 4;
            if (parent.getList().hasSingleResultItem() && listOrderedRecipes.size() > 1) {
                GuiBasic.renderStackInGUI(itemstack, parent.x + k + 1, parent.y + k + 1, 0.0f, 1.0f, "");
                --k;
            }
            GuiBasic.renderStackInGUI(itemstack, parent.x + k, parent.y + k, 0.0f, 1.0f, "");
            if (isAnimated) { GlStateManager.popMatrix(); }
        }
    }

    /** add availability info from npc recipe */
    @Inject(at = {@At("RETURN")}, method = {"getToolTipText"}, cancellable = true)
    public void npcs$getToolTipText(GuiScreen guiScreen, CallbackInfoReturnable<List<String>> cir) {
        GuiButtonRecipe parent = (GuiButtonRecipe) (Object) this;
        if (parent.getList() == null) { return; }
        IGuiButtonRecipeMixinMixin mixin = (IGuiButtonRecipeMixinMixin) this;
        IRecipe recipe = null;
        try { recipe = mixin.invokeGetRecipe(); } catch (Exception ignored) { }
        if (recipe instanceof RecipeCarpentry) {
            RecipeCarpentry npcRecipe = (RecipeCarpentry) recipe;
            if (npcs$availabilityList != null && GuiScreen.isShiftKeyDown()) { cir.setReturnValue(npcs$availabilityList); }
            else {
                List<String> hovers = guiScreen.getItemToolTip(recipe.getRecipeOutput());
                hovers.add("");
                hovers.add(Component.translatable("gui.type").append(": ")
                        .append(Component.translatable("item.craft.type."+(recipe instanceof IShapedRecipe || npcRecipe.isShaped()))).getFormattedText());
                if (npcs$availabilityType != 0) {
                    hovers.add(Component.translatable("gui.recipebook.availability."+ npcs$availabilityType).getFormattedText());
                }
                if (npcs$availabilityList != null) {
                    hovers.add(Component.translatable("gui.recipebook.availability.3").getFormattedText());
                }
                if (parent.getList().getRecipes(book.isFilteringCraftable()).size() > 1) {
                    hovers.add(Component.translatable("gui.recipebook.moreRecipes").getFormattedText());
                }
                cir.setReturnValue(hovers);
            }
        }
    }

}
