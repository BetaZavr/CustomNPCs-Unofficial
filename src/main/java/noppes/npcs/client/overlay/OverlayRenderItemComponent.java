package noppes.npcs.client.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.constants.AlignmentType;
import noppes.npcs.api.overlay.IRenderItemOverlay;

import javax.annotation.Nonnull;

public class OverlayRenderItemComponent implements IOverlayRenderComponent {

   protected final int x;
   protected final int y;
   protected final int id;
   protected final float scale;
   protected final ItemStack item;
   protected AlignmentType alignment;
   protected final @Nonnull Minecraft minecraft;

   public OverlayRenderItemComponent(IRenderItemOverlay component) {
      x = component.getPosX();
      y = component.getPosY();
      id = component.getId();
      scale = component.getScale();
      item = component.getItem().getMCItemStack();
      alignment = AlignmentType.get(component.getAlignment());
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
      graphics.renderItem(item, 0, 0);
      graphics.renderItemDecorations(minecraft.font, item, 0, 0);
      matrixStack.popPose();
   }

}
