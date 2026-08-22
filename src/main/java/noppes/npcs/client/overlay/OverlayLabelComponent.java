package noppes.npcs.client.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import noppes.npcs.api.constants.AlignmentType;
import noppes.npcs.api.overlay.IOverlayLabel;

import javax.annotation.Nonnull;

public class OverlayLabelComponent implements IOverlayRenderComponent {

   protected final Component text;
   protected final int x;
   protected final int y;
   protected final int id;
   protected final int color;
   protected final float scale;
   protected AlignmentType alignment;
   protected final @Nonnull Minecraft minecraft;

   public OverlayLabelComponent(IOverlayLabel component) {
      String textIn = component.getText();
      x = component.getPosX();
      y = component.getPosY();
      id = component.getId();
      color = component.getColor();
      scale = component.getScale();
      alignment = AlignmentType.get(component.getAlignment());
      MutableComponent tempText = Component.empty();
      if (textIn.contains("<br>") || textIn.contains("&t")) {
         String nl = textIn.contains("<br>") ? "<br>" : "&t";
         for (String s : textIn.split(nl)) { tempText.append(Component.translatable(s)); }
      }
      else { tempText.append(Component.translatable(textIn)); }
      text = tempText;
      minecraft = Minecraft.getInstance();
   }

   public void render(GuiGraphics graphics, int linkSide) {
      int widthWin = Minecraft.getInstance().getWindow().getGuiScaledWidth();
      int heightWin = Minecraft.getInstance().getWindow().getGuiScaledHeight();
      float xPos = x;
      float yPos = y;
      AlignmentType type = alignment != AlignmentType.NONE ? alignment : AlignmentType.get(linkSide);
      if (type != AlignmentType.NONE) {
         xPos += alignment.getOffsetX(widthWin / 2);
         yPos += alignment.getOffsetY(heightWin / 2);
      }
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.translate(xPos, yPos, (float) id * 0.01F);
      matrixStack.scale(scale, scale, scale);
      graphics.drawString(minecraft.font, text, 0, 0, color);
      matrixStack.popPose();
   }

}
