package noppes.npcs.client.gui.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.containers.ContainerCarpentryBench;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

import javax.annotation.Nonnull;

// net.minecraft.client.gui.screens.inventory.CraftingScreen
@OnlyIn(Dist.CLIENT)
public class GuiNpcCarpentryBench extends GuiContainerNPCInterface<ContainerCarpentryBench>
        implements RecipeUpdateListener {

   // from CraftingScreen
   protected final ResourceLocation RECIPE_BUTTON_LOCATION = new ResourceLocation("minecraft", "textures/gui/recipe_button.png");
   protected final RecipeBookComponent recipeBookComponent = new RecipeBookComponent();
   protected boolean widthTooNarrow;

   public GuiNpcCarpentryBench(ContainerCarpentryBench container, Inventory inv, Component titleIn) {
      super(null, container, inv, titleIn);
      setBackground("carpentry.png");
      imageHeight = 180;
      titleLabelX = 7;
   }

   @Override
   public void init() {
      super.init();
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      widthTooNarrow = width < 379;
      recipeBookComponent.init(width, height, minecraft, widthTooNarrow, menu);
      guiLeft = recipeBookComponent.updateScreenPosition(width, imageWidth);
      leftPos = guiLeft;
      topPos = guiTop;
      GuiButtonNop button = new GuiButtonNop(this, 10, "", guiLeft + 5, height / 2 - 49, (b) -> {
         recipeBookComponent.toggleVisibility();
         guiLeft = recipeBookComponent.updateScreenPosition(width, imageWidth);
         leftPos = guiLeft;
         topPos = guiTop;
         b.setPosition(guiLeft + 5, height / 2 - 49);
      })
              .setSize(20, 19)
              .setTexture(RECIPE_BUTTON_LOCATION)
              .setUV(0, 0, 20 ,19);
      button.isSimple = true;
      add(button);
      addWidget(recipeBookComponent);
      setInitialFocus(recipeBookComponent);
   }

   @Override
   public void containerTick() {
      super.containerTick();
      recipeBookComponent.tick();
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      renderBackground(graphics);
      if (recipeBookComponent.isVisible() && widthTooNarrow) {
         renderBg(graphics, partialTicks, mouseX, mouseY);
         recipeBookComponent.render(graphics, mouseX, mouseY, partialTicks);
      }
      else {
         super.render(graphics, mouseX, mouseY, partialTicks);
         recipeBookComponent.render(graphics, mouseX, mouseY, partialTicks);
         recipeBookComponent.renderGhostRecipe(graphics, guiLeft, guiTop, false, partialTicks);
      }
      renderTooltip(graphics, mouseX, mouseY);
      recipeBookComponent.renderTooltip(graphics, guiLeft, guiTop, mouseX, mouseY);
   }

   @Override
   protected boolean isHovering(int x, int y, int widthIn, int heightIn, double mouseX, double mouseY) {
      return (!widthTooNarrow || !recipeBookComponent.isVisible()) && super.isHovering(x, y, widthIn, heightIn, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (recipeBookComponent.mouseClicked(mouseX, mouseY, mouseButton)) {
         setFocused(recipeBookComponent);
         return true;
      }
      return widthTooNarrow && recipeBookComponent.isVisible() || super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   @Override
   protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int mouseButton) {
      boolean flag = mouseX < (double) left || mouseY < (double) top || mouseX >= (double)(left + imageWidth) || mouseY >= (double)(top + imageHeight);
      return recipeBookComponent.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, imageWidth, imageHeight, mouseButton) && flag;
   }

   @Override
   protected void slotClicked(@Nonnull Slot slot, int mouseX, int mouseY, @Nonnull ClickType type) {
      super.slotClicked(slot, mouseX, mouseY, type);
      recipeBookComponent.slotClicked(slot);
   }

   @Override
   public void recipesUpdated() { recipeBookComponent.recipesUpdated(); }

   @Override
   public @Nonnull RecipeBookComponent getRecipeBookComponent() { return recipeBookComponent; }

   @Override
   protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(font, Component.translatable("block.customnpcs.npccarpentybench").append(":"),
              titleLabelX, titleLabelY - 2, CustomNpcResourceListener.DefaultTextColor, false);
      graphics.drawString(font, Component.translatable("container.inventory").append(":"),
              titleLabelX, titleLabelY + 82, CustomNpcResourceListener.DefaultTextColor, false);
   }

}
