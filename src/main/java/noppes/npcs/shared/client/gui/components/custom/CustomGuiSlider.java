package noppes.npcs.shared.client.gui.components.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiSliderWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiTextFieldWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiSliderUpdate;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;

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

   // standard
   public boolean active = false;

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
   public void render(int mouseX, int mouseY, float partialTicks) {
      if (!visible) {
         isHovered = false;
         return;
      }
      super.render(mouseX, mouseY, partialTicks);
      if (textfield != null) {
         textfield.render(mouseX, mouseY, partialTicks);
         if (!textfield.isFocused()) { closeTextfield(); }
      }
      if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
         listener.setHoverText(component.getHoverTextList());
      }
   }

   @Override
   public void setSliderValue(float value) {
      value = ValueUtil.correctFloat(value, 0.0F, 1.0F);
      if (value != sliderValue) {
         sliderValue = value;
         component.setValue(value * total + component.getMin());
         setMessage(Component.translatable(component.getFormat(), component.getValue()));
         if (!disablePackets) { Packets.sendServer(new SPacketCustomGuiSliderUpdate(component.getUniqueID(), component.getValue())); }
         else { component.onChange(null); }
         if (listener instanceof ISliderListener) { ((ISliderListener) listener).mouseDragged(this); }
      }
   }

   @Override
   public boolean keyPressed(char typedChar, int keyCode) {
      if (textfield != null && (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)) {
         closeTextfield();
      }
      return textfield != null ? textfield.keyPressed(typedChar, keyCode) : super.keyPressed(typedChar, keyCode);
   }

   private void closeTextfield() {
      setSliderValue((tfComponent.getFloat() + component.getMin()) / total);
      textfield = null;
   }

   @Override
   public void renderWidget(int mouseX, int mouseY, float partialTicks) {
      isHovered = visible && mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
      if (!visible) { return; }
      Minecraft minecraft = Minecraft.getMinecraft();
      FontRenderer font = minecraft.fontRenderer;
      minecraft.getTextureManager().bindTexture(GuiButtonNop.WIDGETS_LOCATION);
      GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
      GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableBlend();
      GlStateManager.enableDepth();
      drawTexturedModalRect(getX(), getY(), 0, 46, width / 2, height);
      drawTexturedModalRect(getX() + width / 2, getY(), 200 - width / 2, 46, width / 2, height);
      drawDefaultBackground();
      drawCenteredString(font, getMessage().getFormattedText(), getX() + width / 2, getY() + (height - 8) / 2, getFGColor() | 255 << 24);
   }

   public int getFGColor() {
      if (packedFGColor != 0) { return packedFGColor; }
      else if (!active) { return CustomNpcs.NotEnableColor.getRGB(); }
      else if (isHovered) { return CustomNpcs.HoverColor.getRGB(); }
      return CustomNpcs.ButtonColor.getRGB();
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
      if (mouseButton != 0 || (!isHovered && !isDrag)) { return false; }
      isDrag = true;
      setSliderValue((float)(mouseX - (double)(getX() + 4)) / (float)(width - 8));
      return true;
   }

   public void tick() {
      if (textfield != null) { textfield.tick(); }
   }

   @Override
   public void onRelease(double x, double y) {
      if (sliderValue != startValue) {
         Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         startValue = sliderValue;
      }
   }

   @Override
   public void drawDefaultBackground() {
      if (visible) {
         Minecraft.getMinecraft().getTextureManager().bindTexture(GuiButtonNop.WIDGETS_LOCATION);
         GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
         GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         int lvt_4_1_ = (isHovered ? 2 : 1) * 20;
         GlStateManager.pushMatrix();
         GlStateManager.translate(0.0F, 0.0F, 10.0F);
         drawTexturedModalRect(getX() + (int)((double)sliderValue * (double)(width - 8)), getY(), 0, 46 + lvt_4_1_, 4, height / 2);
         drawTexturedModalRect(getX() + (int)((double)sliderValue * (double)(width - 8)), getY() + height / 2, 0, 46 + lvt_4_1_ + 20 - height / 2, 4, height / 2);
         drawTexturedModalRect(getX() + (int)((double)sliderValue * (double)(width - 8)) + 4, getY(), 196, 46 + lvt_4_1_, 4, height / 2);
         drawTexturedModalRect(getX() + (int)((double)sliderValue * (double)(width - 8)) + 4, getY() + height / 2, 196, 46 + lvt_4_1_ + 20 - height / 2, 4, height / 2);
         GlStateManager.popMatrix();
      }
   }

   @Override
   public ICustomGuiComponent component() { return component; }

}
