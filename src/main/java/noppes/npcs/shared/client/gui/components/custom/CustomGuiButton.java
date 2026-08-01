package noppes.npcs.shared.client.gui.components.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiButtonWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiButton;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

import java.awt.*;

public class CustomGuiButton extends GuiButtonNop implements IComponentCustomGui {

   protected CustomGuiTexturedRect background;
   protected int colour = new Color(0xFFFFFF).getRGB();
   protected OnPress onPress;
   public GuiCustom listener;
   public CustomGuiButtonWrapper component;
   public int id;

   public CustomGuiButton(GuiCustom parent, CustomGuiButtonWrapper componentIn) {
      super(parent, componentIn.getId(), componentIn.getLabel(), componentIn.getPosX(), componentIn.getPosY(), null);
      setSize(componentIn.getWidth(), componentIn.getHeight());
      listener = parent;
      onPress = (button) -> {
         if (!component.disablePackets) { Packets.sendServer(new SPacketCustomGuiButton(component.getUniqueID())); }
         else { component.onPress(listener.guiWrapper); }
      };
      component = componentIn;
      hoverText = componentIn.getHoverTextList();
      isSimple = true;
      init();
   }

   @Override
   protected void onClick(double x, double y) {
      if (display != null && display.length != 0) { setDisplay((displayValue + 1) % display.length); }
      if (hasSound) { Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)); }
      if (onPress != null) { onPress.onPress(this); }
      else if (listener != null) { listener.buttonEvent(this); }
   }

   public void init() {
      id = component.getId();
      setX(component.getPosX());
      setY(component.getPosY());
      setWidth(component.getWidth());
      setHeight(component.getHeight());
      background = new CustomGuiTexturedRect(listener, component.getTextureRect());
      setMessage(Component.translatable(component.getLabel()));
      active = component.getEnabled() && component.getVisible();
      enabled = component.getEnabled();
      visible = component.getVisible();
   }

   @Override
   public boolean keyPressed(char typedChar, int keyCode) { return false; }

   @Override
   public int getId() { return id; }

   @Override
   public void render(int mouseX, int mouseY, float partialTicks) {
      if (!active || !visible) { return; }
      super.render(mouseX, mouseY, partialTicks);
      if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
         listener.setHoverText(component.getHoverTextList());
      }
   }

   @Override
   public void renderWidget(int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (!visible) { return; }
      isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)getX(), (float)getY(), 0.0F);
      Minecraft mc = Minecraft.getMinecraft();
      int i;
      if (component.getTexture().equals("textures/gui/widgets.png")) { i = !active ? 0 : (isHovered ? 2 : 1); }
      else { i = isHovered ? 1 : 0; }
      background.textureY = component.getTextureY() + i * component.getTextureHoverOffset();
      background.render(mouseX - getX(), mouseY - getY(), partialTicks);
      renderLabel();
      if (!component.getDisplayItem().isEmpty()) {
         GlStateManager.pushMatrix();
         GlStateManager.translate((float)getX() + ((float)width - 16.0F) / 2.0F,
                 (float)getY() + ((float)height - 16.0F) / 2.0F + 1.0F,
                 -180.0F);
         mc.getRenderItem().renderItemAndEffectIntoGUI(component.getDisplayItem().getMCItemStack(), 0, 0);
         mc.getRenderItem().renderItemOverlays(mc.fontRenderer, component.getDisplayItem().getMCItemStack(), 0, 0);
         GlStateManager.popMatrix();
      }
      GlStateManager.popMatrix();
   }

   public void renderLabel() {
      if (!component.getLabel().isEmpty()) {
         int j = 0xE0E0E0;
         if (colour != 0) { j = colour; }
         else if (!active) { j = 0xA0A0A0; }
         else if (isHovered) { j = 0xFFFFA0; }
         Minecraft mc = Minecraft.getMinecraft();
         GlStateManager.translate(0.0F, 0.0F, (float)id);
         drawCenteredString(mc.fontRenderer, getMessage().getFormattedText(), width / 2, (height - 8) / 2, j);
      }
   }

   @Override
   public ICustomGuiComponent component() { return component; }

   @Override
   protected boolean isValidClickButton(int mouseButton) { return mouseButton == 0; }

}
