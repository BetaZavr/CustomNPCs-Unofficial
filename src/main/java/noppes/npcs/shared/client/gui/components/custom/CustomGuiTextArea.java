package noppes.npcs.shared.client.gui.components.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiTextAreaWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiTextFieldWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiTextUpdate;
import noppes.npcs.shared.client.gui.components.GuiTextArea;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;

import javax.annotation.Nonnull;

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
      enabled = component.getEnabled();
      visible = component.getVisible();
      if (component.hasHoverText()) { hoverText.addAll(component.getHoverTextList()); }
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (visible) { return; }
      if (height <= 0) { height = 10; }
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.translate(0.0F, 0.0F, (float)id * 0.01F);
      super.render(graphics, mouseX, mouseY, partialTicks);
      matrixStack.popPose();
      if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
         listener.setHoverText(component.getHoverTextList());
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      String text = getText();
      boolean bo = super.keyPressed(keyCode, scanCode, modifiers);
      if (!text.equals(getText())) {
         component.setText(getText());
         Packets.sendServer(new SPacketCustomGuiTextUpdate(component.getUniqueID(), getText()));
      }
      return bo;
   }

   @Override
   public boolean charTyped(char c, int i) {
      String text = getText();
      boolean bo = super.charTyped(c, i);
      if (!text.equals(getText())) {
         component.setText(getText());
         Packets.sendServer(new SPacketCustomGuiTextUpdate(component.getUniqueID(), getText()));
      }
      return bo;
   }

   @Override
   public ICustomGuiComponent component() { return component; }

}
