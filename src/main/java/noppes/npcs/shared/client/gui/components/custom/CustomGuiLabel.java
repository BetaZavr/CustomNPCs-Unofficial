package noppes.npcs.shared.client.gui.components.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiLabelWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class CustomGuiLabel extends GuiLabel implements IComponentCustomGui {

   protected final CustomGuiLabelWrapper component;

   // New from Unofficial (BetaZavr)

   public CustomGuiLabel(GuiCustom parentIn, CustomGuiLabelWrapper componenIn) {
      super(parentIn, componenIn.getId(), componenIn.getText(), componenIn.getPosX(), componenIn.getPosY());
      component = componenIn;
      init();
   }

   @Override
   public void init() {
      id = component.getId();
      setX(component.getPosX());
      setY(component.getPosY());
      setWidth(component.getWidth());
      setHeight(component.getHeight());
      enabled = component.getEnabled();
      visible = component.getVisible();
      if (component.hasHoverText()) { hoverText = component.getHoverTextList(); }
      centered = component.getCentered();
      textColor = component.getColor();
      showShadow = component.isShadow();
      setMessage(Component.translatable(component.getText()));
   }

   @Override
   public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
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
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.translate(0.0F, 0.0F, (float)id * 0.01F);
      matrixStack.scale(component.getScale(), component.getScale(), 0.0F);
      if (offsetHoverX != 0 || offsetHoverY != 0) {
         mouseX -= offsetHoverX;
         mouseY -= offsetHoverY;
      }
      int x = (int) (getX() / component.getScale());
      int y = (int) (getY() / component.getScale());
      int r = (int) ((getX() + width) / component.getScale());
      int b = (int) ((getY() + height)  / component.getScale());
      isHovered = mouseX >= x && mouseY >= y && mouseX < r && mouseY < b;
      drawBox(graphics);
      GuiButtonNop.renderString(graphics, getMessage(), x, y, r, b, textColor, showShadow, centered, null);
      matrixStack.popPose();
   }

   public void setText(String s) { setMessage(Component.translatable(s)); }

   @Override
   public ICustomGuiComponent component() { return component; }

   @Override
   public void playDownSound(@NotNull SoundManager soundManager) {}

}
