package noppes.npcs.shared.client.gui.components.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiTextFieldWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiFocusUpdate;
import noppes.npcs.packets.server.SPacketCustomGuiTextUpdate;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;

import javax.annotation.Nonnull;

public class CustomGuiTextField extends GuiTextFieldNop implements IComponentCustomGui {

   protected static CustomGuiTextField focused = null;
   protected final CustomGuiTextFieldWrapper component;

   public CustomGuiTextField(GuiCustom parentIn, CustomGuiTextFieldWrapper componentIn) {
      super(parentIn, componentIn.getId(), componentIn.getPosX(), componentIn.getPosY(), componentIn.getWidth(), componentIn.getHeight(), componentIn.getText());
      setMaxLength(500);
      component = componentIn;
      init();
   }

   @Override
   public void init() {
      id = component.getId();
      setX(component.getPosX());
      setY(component.getPosY());
      width = component.getWidth();
      height = component.getHeight();
      enabled = component.getEnabled();
      visible = component.getVisible();
      setTextColor(component.getColor());
      setFocused(component.getFocused());
      if (component.getText() != null) { setValue(component.getText()); }
      if (component.hasHoverText()) { setHoverTexts(component.getHoverTextList()); }
   }

   @Override
   public ICustomGuiComponent component() { return component; }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (visible) {
         super.render(graphics, mouseX, mouseY, partialTicks);
         if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
            listener.setHoverText(component.getHoverTextList());
         }
      }
   }

   @Override
   public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (!visible) { return; }
      isHovered = mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.translate(0.0F, 0.0F, (float) id * 0.01F);
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);
      matrixStack.popPose();
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      String text = getValue();
      boolean bo = super.keyPressed(keyCode, scanCode, modifiers);
      component.setText(getValue());
      if (!getValue().equals(component.getText())) {
         setValue(component.getText());
      }
      if (!text.equals(getValue())) {
         if (!component.disablePackets) {
            Packets.sendServer(new SPacketCustomGuiTextUpdate(component.getUniqueID(), getValue()));
         } else {
            component.onChange(null);
         }
      }
      return bo;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      setFocused(isHovered);
      return super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   private boolean isValidChar(char c) {
      if (component.getCharacterType() == 1) { return Character.isDigit(c); }
      if (component.getCharacterType() == 2) { return Character.isDigit(c) || Character.toLowerCase(c) >= 'a' && Character.toLowerCase(c) <= 'f'; }
      if (component.getCharacterType() != 3) { return true; }
      return Character.isDigit(c) || c == '.' && !getValue().contains(".") || c == '-' && getCursorPosition() == 0;
   }

   @Override
   public boolean charTyped(char c, int i) {
      if (!isValidChar(c)) { return false; }
      else {
         String text = getValue();
         boolean bo = super.charTyped(c, i);
         if (!text.equals(getValue())) {
            component.setText(getValue());
            if (!component.disablePackets) { Packets.sendServer(new SPacketCustomGuiTextUpdate(component.getUniqueID(), getValue())); }
            else { component.onChange(null); }
         }
         return bo;
      }
   }

   @Override
   public void setFocused(boolean bo) {
      if (isFocused() != bo) {
         super.setFocused(bo);
         if (component.getFocused() != bo) {
            if (!component.getText().isEmpty() && (component.getCharacterType() == 1 || component.getCharacterType() == 2)) {
               component.setInteger(component.getInteger());
               setValue(component.getText());
               if (!component.disablePackets) { Packets.sendServer(new SPacketCustomGuiTextUpdate(component.getUniqueID(), component.getText())); }
               component.onChange(null);
            }
            component.setFocused(bo);
            if (!component.disablePackets) { Packets.sendServer(new SPacketCustomGuiFocusUpdate(component.getUniqueID(), bo)); }
            else { component.onFocusLost(null); }
         }
         if (isFocused() && focused != this) {
            if (focused != null) { focused.setFocused(false); }
            focused = this;
         }
         if (!isFocused() && focused != this) { focused = null; }
      }
   }

   @Override
   public void playDownSound(@Nonnull SoundManager soundManager) { }

}
