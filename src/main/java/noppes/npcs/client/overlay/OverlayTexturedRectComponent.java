package noppes.npcs.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.api.constants.AlignmentType;
import noppes.npcs.api.overlay.IOverlayTexturedRect;

import javax.annotation.Nonnull;

public class OverlayTexturedRectComponent implements IOverlayRenderComponent {

   protected final int x;
   protected final int y;
   protected final int width;
   protected final int height;
   protected final float textureX;
   protected final float textureY;
   protected final int textureMaxX;
   protected final int textureMaxY;
   protected final int id;
   protected final float[] layerColor;
   protected final float scale;
   protected final String texture;
   protected AlignmentType alignment;
   protected final @Nonnull Minecraft minecraft;

   public OverlayTexturedRectComponent(IOverlayTexturedRect component) {
      x = component.getPosX();
      y = component.getPosY();
      id = component.getId();
      width = component.getWidth();
      height = component.getHeight();
      texture = component.getTexture();
      textureX = component.getTextureX();
      textureY = component.getTextureY();
      textureMaxX = component.getTextureMaxX();
      textureMaxY = component.getTextureMaxY();
      alignment = AlignmentType.get(component.getAlignment());
      scale = component.getScale();
      layerColor = component.getRGB();
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
      matrixStack.scale(scale, scale, 1.0f);
      if (layerColor[3] != 0) { RenderSystem.setShaderColor(layerColor[0], layerColor[1], layerColor[2], layerColor[3]); }
      ResourceLocation resLoc = ResourceLocation.tryParse(texture);
      if (texture.isEmpty() || resLoc == null) { graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010); } // no texture
      else { graphics.blit(resLoc, 0, 0, textureX, textureY, width, height, textureMaxX, textureMaxY); }
      matrixStack.popPose();
   }

}
