package noppes.npcs.shared.client.gui.components.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiItemRendererWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;

import javax.annotation.Nonnull;

public class CustomGuiItemRenderer
        extends GuiLabel
        implements IComponentCustomGui {

   protected final Minecraft minecraft;
   protected ItemStack stack;
   public CustomGuiItemRendererWrapper component;

   public CustomGuiItemRenderer(GuiCustom parentIn, CustomGuiItemRendererWrapper componentIn) {
      super(parentIn, componentIn.getId(), Component.empty(), componentIn.getPosX(), componentIn.getPosY());
      component = componentIn;
      minecraft = Minecraft.getInstance();
      init();
   }

   @Override
   public void init() {
      id = component.getId();
      setX(component.getPosX());
      setY(component.getPosY());
      setWidth(component.getWidth());
      setHeight(component.getHeight());
      if (component.hasStack()) { stack = component.getStack().getMCItemStack(); }
      else { stack = ItemStack.EMPTY; }
      enabled = component.getEnabled();
      visible = component.getVisible();
      if (component.hasHoverText()) { hoverText = component.getHoverTextList(); }
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!enabled || !visible) { return; }
      super.render(graphics, mouseX, mouseY, partialTicks);
      if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
         listener.setHoverText(component.getHoverTextList());
      }
   }

   @Override
   protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (!visible) { return; }
      int x = (int) (getX() / component.getScale());
      int y = (int) (getY() / component.getScale());
      int r = (int) ((getX() + width) / component.getScale());
      int b = (int) ((getY() + height)  / component.getScale());
      isHovered = mouseX >= x && mouseY >= y && mouseX < r && mouseY < b;
      if (!NoppesUtilServer.isItemStackNull(stack)) {
         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         matrixStack.scale(component.getScale(), component.getScale(), 1.0F);
         matrixStack.translate(x, y, (float) id * 0.01F);
         graphics.renderItem(stack, 0, 0);
         graphics.renderItemDecorations(minecraft.font, stack, 0, 0);
         matrixStack.popPose();
      }
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) { return true; }

   @Override
   public ICustomGuiComponent component() { return component; }

   // New Unofficial (Goodbird)
   @Override
   public void playDownSound(@Nonnull SoundManager soundManager) { }

   @Override
   public GuiComponentType getElementType() { return GuiComponentType.ITEM_RENDERER; }

}
