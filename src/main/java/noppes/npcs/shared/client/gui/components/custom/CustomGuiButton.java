package noppes.npcs.shared.client.gui.components.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiButtonWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiButton;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
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
   public void onPress() { onPress.onPress(this); }

   @Override
   protected boolean isValidClickButton(int mouseButton) { return mouseButton == 0; }

   public void init() {
      id = component.getId();
      setX(component.getPosX());
      setY(component.getPosY());
      setWidth(component.getWidth());
      setHeight(component.getHeight());
      background = new CustomGuiTexturedRect(listener, component.getTextureRect());
      setMessage(Component.translatable(component.getLabel()));
      active = component.getEnabled() && component.getVisible();
      visible = component.getVisible();
   }

   @Override
   public boolean keyPressed(int key, int key_1, int key_2) { return false; }

   @Override
   public int getId() { return id; }

   @Override
   public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!active || !visible) { return; }
      super.render(graphics, mouseX, mouseY, partialTicks);
      if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
         listener.setHoverText(component.getHoverTextList());
      }
   }

   @Override
   public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (!visible) { return; }
      isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.translate((float)getX(), (float)getY(), (float) id * 0.01F);
      Minecraft mc = Minecraft.getInstance();
      int i;
      if (component.getTexture().equals("textures/gui/widgets.png")) { i = !active ? 0 : (isHovered ? 2 : 1); }
      else { i = isHovered ? 1 : 0; }
      background.textureY = component.getTextureY() + i * component.getTextureHoverOffset();
      background.render(graphics, mouseX - getX(), mouseY - getY(), partialTicks);
      renderLabel(graphics);
      if (!component.getDisplayItem().isEmpty()) {
         graphics.pose().pushPose();
         graphics.pose().translate(0.0F, 0.0F, -90.0F);
         PoseStack posestack = RenderSystem.getModelViewStack();
         posestack.pushPose();
         posestack.translate((float)getX() + ((float)width - 16.0F) / 2.0F,
                 (float)getY() + ((float)height - 16.0F) / 2.0F + 1.0F,
                 -90.0F);
         RenderSystem.applyModelViewMatrix();
         graphics.renderItem(component.getDisplayItem().getMCItemStack(), 0, 0);
         graphics.renderItemDecorations(mc.font, component.getDisplayItem().getMCItemStack(), 0, 0);
         posestack.popPose();
         graphics.pose().popPose();
         RenderSystem.applyModelViewMatrix();
      }
      matrixStack.popPose();
   }

   public void renderLabel(GuiGraphics graphics) {
      if (!component.getLabel().isEmpty()) {
         int j = 0xE0E0E0;
         if (colour != 0) { j = colour; }
         else if (!active) { j = 0xA0A0A0; }
         else if (isHovered) { j = 0xFFFFA0; }
         Minecraft mc = Minecraft.getInstance();
         graphics.pose().translate(0.0F, 0.0F, (float)id);
         graphics.drawCenteredString(mc.font, getMessage(), width / 2, (height - 8) / 2, j);
      }
   }

   @Override
   public ICustomGuiComponent component() { return component; }

}
