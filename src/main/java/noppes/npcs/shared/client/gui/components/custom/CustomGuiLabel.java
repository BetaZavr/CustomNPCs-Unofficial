package noppes.npcs.shared.client.gui.components.custom;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiLabelWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;

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
   public void render(int mouseX, int mouseY, float partialTicks) {
      if (!enabled || !visible) { return; }
      if (height <= 0) { setHeight(0); }
      renderWidget(mouseX, mouseY, partialTicks);
      if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
         listener.setHoverText(component.getHoverTextList());
      }
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Override
   public void renderWidget(int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (!visible) { return; }
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0F, 0.0F, (float)id);
      GlStateManager.scale(component.getScale(), component.getScale(), 0.0F);
      if (offsetHoverX != 0 || offsetHoverY != 0) {
         mouseX -= offsetHoverX;
         mouseY -= offsetHoverY;
      }
      int x = (int) (getX() / component.getScale());
      int y = (int) (getY() / component.getScale());
      int r = (int) ((getX() + width) / component.getScale());
      int b = (int) ((getY() + height)  / component.getScale());
      isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
      drawBox();
      GuiButtonNop.renderString(getMessage(), x, y, r, b, textColor, showShadow, centered, customFont);
      GlStateManager.popMatrix();
   }

   public void setText(String s) { setMessage(Component.translatable(s)); }

   @Override
   public ICustomGuiComponent component() { return component; }

}
