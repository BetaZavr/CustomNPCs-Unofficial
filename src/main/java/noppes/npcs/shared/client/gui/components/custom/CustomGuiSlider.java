package noppes.npcs.shared.client.gui.components.custom;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiSliderWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiTextFieldWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiSliderUpdate;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;

import javax.annotation.Nonnull;

public class CustomGuiSlider extends GuiSliderNop implements IComponentCustomGui {

   protected final CustomGuiSliderWrapper component;
   protected final CustomGuiTextFieldWrapper tfComponent;
   protected CustomGuiTextField textfield;
   protected float sliderValue;
   protected float startValue;
   protected long lastClickedTime = 0L;
   protected float total;
   protected boolean disablePackets = false;
   public int id;

   public CustomGuiSlider(GuiCustom parent, CustomGuiSliderWrapper componentIn) {
      super(parent, componentIn.getId(), componentIn.getPosX(), componentIn.getPosY(), componentIn.getValue());
      component = componentIn;
      tfComponent = (new CustomGuiTextFieldWrapper(id, getX(), getY(), width, height)).setCharacterType(3);
      init();
   }

   public void init() {
      id = component.getId();
      setX(component.getPosX());
      setY(component.getPosY());
      setWidth(component.getWidth());
      setHeight(component.getHeight());
      total = component.getMax() - component.getMin();
      startValue = sliderValue = (component.getValue() - component.getMin()) / total;
      tfComponent.setId(id);
      tfComponent.setPos(component.getPosX(), component.getPosY());
      tfComponent.setSize(component.getWidth(), component.getHeight());
      enabled = component.getEnabled();
      visible = component.getVisible();
      if (component.hasHoverText()) { hoverText = component.getHoverTextList(); }
      setMessage(Component.translatable(component.getFormat(), component.getValue()));
   }

   public CustomGuiSlider disablePackets() {
      disablePackets = true;
      return this;
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (!visible) { return; }
      super.render(graphics, mouseX, mouseY, partialTicks);
      if (textfield != null) {
         textfield.render(graphics, mouseX, mouseY, partialTicks);
         if (!textfield.isFocused()) { closeTextfield(); }
      }
      if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
         listener.setHoverText(component.getHoverTextList());
      }
   }

   @Override
   public void setSliderValue(float value) {
      value = Mth.clamp(value, 0.0F, 1.0F);
      if (value != sliderValue) {
         sliderValue = value;
         component.setValue(value * total + component.getMin());
         setMessage(Component.translatable(component.getFormat(), component.getValue()));
         if (!disablePackets) { Packets.sendServer(new SPacketCustomGuiSliderUpdate(component.getUniqueID(), component.getValue())); }
         else { component.onChange(null); }
         if (listener instanceof ISliderListener parent) { parent.mouseDragged(this); }
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (textfield != null && (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)) {
         closeTextfield();
      }
      return textfield != null ? textfield.keyPressed(keyCode, scanCode, modifiers) : super.keyPressed(keyCode, scanCode, modifiers);
   }

   private void closeTextfield() {
      setSliderValue((tfComponent.getFloat() + component.getMin()) / total);
      textfield = null;
   }

   @Override
   public boolean charTyped(char c, int i) {
      return textfield != null ? textfield.charTyped(c, i) : super.charTyped(c, i);
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!visible) { return; }
      Minecraft minecraft = Minecraft.getInstance();
      Font font = minecraft.font;
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      graphics.blit(WIDGETS_LOCATION, getX(), getY(), 0, 46, width / 2, height);
      graphics.blit(WIDGETS_LOCATION, getX() + width / 2, getY(), 200 - width / 2, 46, width / 2, height);
      renderBg(graphics);
      int j = getFGColor();
      graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, j | Mth.ceil(alpha * 255.0F) << 24);
   }

   @Override
   public void onClick(double x, double y) {
      if (!visible || !enabled) { return; }
      long time = System.currentTimeMillis();
      if (time - lastClickedTime < 500L) {
         tfComponent.setText(component.getValue());
         textfield = new CustomGuiTextField((GuiCustom) listener, tfComponent);
         textfield.setFocused(true);
      }
      else if (textfield != null) {
         textfield.mouseClicked(x, y, 0);
         return;
      }
      lastClickedTime = time;
      setSliderValue((float)(x - (double)(getX() + 4)) / (float)(width - 8));
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      return textfield != null ? textfield.mouseClicked(mouseX, mouseY, mouseButton) : super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
      setSliderValue((float)(mouseX - (double)(getX() + 4)) / (float)(width - 8));
      return true;
   }

   public void tick() {
      if (textfield != null) { textfield.tick(); }
   }

   @Override
   public void onRelease(double x, double y) {
      if (sliderValue != startValue) {
         super.playDownSound(Minecraft.getInstance().getSoundManager());
         startValue = sliderValue;
      }
   }

   @Override
   public void renderBg(GuiGraphics graphics) {
      if (visible) {
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
         int lvt_4_1_ = (isHovered ? 2 : 1) * 20;
         graphics.pose().pushPose();
         graphics.pose().translate(0.0F, 0.0F, id * 0.01F);
         graphics.blit(WIDGETS_LOCATION, getX() + (int)((double)sliderValue * (double)(width - 8)), getY(), 0, 46 + lvt_4_1_, 4, height / 2);
         graphics.blit(WIDGETS_LOCATION, getX() + (int)((double)sliderValue * (double)(width - 8)), getY() + height / 2, 0, 46 + lvt_4_1_ + 20 - height / 2, 4, height / 2);
         graphics.blit(WIDGETS_LOCATION, getX() + (int)((double)sliderValue * (double)(width - 8)) + 4, getY(), 196, 46 + lvt_4_1_, 4, height / 2);
         graphics.blit(WIDGETS_LOCATION, getX() + (int)((double)sliderValue * (double)(width - 8)) + 4, getY() + height / 2, 196, 46 + lvt_4_1_ + 20 - height / 2, 4, height / 2);
         graphics.pose().popPose();
      }
   }

   @Override
   public ICustomGuiComponent component() { return component; }

}
