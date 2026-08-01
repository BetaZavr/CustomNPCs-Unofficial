package noppes.npcs.shared.client.gui.components.custom;

import net.minecraft.client.renderer.GlStateManager;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiTextAreaWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiTextFieldWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiTextUpdate;
import noppes.npcs.shared.client.gui.components.GuiTextArea;

public class CustomGuiTextArea extends GuiTextArea implements IComponentCustomGui {

   protected final CustomGuiTextFieldWrapper component;

   public CustomGuiTextArea(GuiCustom parent, CustomGuiTextAreaWrapper componentIn) {
      super(componentIn.getId(), componentIn.getPosX(), componentIn.getPosY(), componentIn.getWidth(), componentIn.getHeight(), "");
      component = componentIn;
      setListener(parent);
      init();
   }

   public void init() {
      id = component.getId();
      setX(component.getPosX());
      setY(component.getPosY());
      setWidth(component.getWidth());
      setHeight(component.getHeight());
      if (component.getText() != null && !component.getText().isEmpty()) { setText(component.getText()); }
      enabled = component.getEnabled();
      visible = component.getVisible();
      hoverText = component.getHoverTextList();
   }

   @Override
   public void render(int mouseX, int mouseY, float partialTicks) {
      if (!visible) { return; }
      if (height <= 0) { height = 10; }
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0F, 0.0F, (float)id);
      super.render(mouseX, mouseY, partialTicks);
      GlStateManager.popMatrix();
      if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
         listener.setHoverText(component.getHoverTextList());
      }
   }

   @Override
   public boolean keyPressed(char typedChar, int keyCode) {
      String text = getText();
      boolean bo = super.keyPressed(typedChar, keyCode);
      if (!text.equals(getText())) {
         component.setText(getText());
         Packets.sendServer(new SPacketCustomGuiTextUpdate(component.getUniqueID(), getText()));
      }
      return bo;
   }

   @Override
   public boolean charAllowed(char c, int i) {
      String text = getText();
      boolean bo = super.charAllowed(c, i);
      if (!text.equals(getText())) {
         component.setText(getText());
         Packets.sendServer(new SPacketCustomGuiTextUpdate(component.getUniqueID(), getText()));
      }
      return bo;
   }

   @Override
   public ICustomGuiComponent component() { return component; }

}
