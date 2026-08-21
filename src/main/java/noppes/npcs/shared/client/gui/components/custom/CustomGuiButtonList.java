package noppes.npcs.shared.client.gui.components.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.wrapper.gui.CustomGuiButtonListWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiTexturedRectWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiButtonList;

import javax.annotation.Nonnull;

public class CustomGuiButtonList extends CustomGuiButton {

   protected CustomGuiTexturedRect left;
   protected CustomGuiTexturedRect right;
   protected CustomGuiTexturedRectWrapper leftWrapper;
   protected CustomGuiTexturedRectWrapper rightWrapper;
   protected boolean isRight = false;

   public CustomGuiButtonList(GuiCustom parent, CustomGuiButtonListWrapper component) {
      super(parent, component);
      onPress = (button) -> {
         CustomGuiButtonList list = (CustomGuiButtonList)button;
         component.setSelected(component.getSelected() + (list.isRight ? 1 : -1));
         list.setMessage(Component.translatable(component.getLabel()));
         sendPacket();
         if (!component.disablePackets) { Packets.sendServer(new SPacketCustomGuiButtonList(component.getUniqueID(), list.isRight)); }
         else { component.onPress(parent.guiWrapper); }
      };
   }

   private void sendPacket() {
      Packets.sendServer(new SPacketCustomGuiButtonList(component.getUniqueID(), isRight));
   }

   @SuppressWarnings("unused")
   public CustomGuiButtonList(GuiCustom parent, CustomGuiButtonListWrapper componentIn, OnPress onPressIn) {
      super(parent, componentIn);
      component = componentIn;
      onPress = onPressIn;
      init();
   }

   @Override
   public void init() {
      super.init();
      leftWrapper = ((CustomGuiButtonListWrapper) component).getLeftTexture();
      rightWrapper = ((CustomGuiButtonListWrapper) component).getRightTexture();
      left = new CustomGuiTexturedRect(listener, leftWrapper);
      right = new CustomGuiTexturedRect(listener, rightWrapper);
   }

   protected int getYImage(boolean p_93668_) {
      int i = 1;
      if (!active) { i = 0; }
      else if (p_93668_) { i = 2; }
      return i;
   }

   @Override
   public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!visible) { return; }
      PoseStack matrixStack = graphics.pose();
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);
      matrixStack.pushPose();
      matrixStack.translate((float)getX(), (float)getY(), (float) id * 0.01F);
      isRight = mouseX >= getX() + width / 2;
      left.textureY = leftWrapper.getTextureY() + getYImage(isHovered && !isRight) * leftWrapper.getHeight();
      left.render(graphics, mouseX - getX(), mouseY - getY(), partialTicks);
      right.textureY = rightWrapper.getTextureY() + getYImage(isHovered && isRight) * rightWrapper.getHeight();
      right.render(graphics, mouseX - getX(), mouseY - getY(), partialTicks);
      renderLabel(graphics);
      matrixStack.popPose();
   }

}
