package noppes.npcs.shared.client.gui.components.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiTexturedRectWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

public class CustomGuiTexturedRect extends GuiLabel implements IComponentCustomGui {

   protected CustomGuiTexturedRectWrapper component;
   protected List<Component> hoverText;
   protected GuiCustom parent;
   protected ResourceLocation texture;
   public int textureX;
   public int textureY;
   public int textureMaxX;
   public int textureMaxY;
   public boolean hasRepeatingTexture = false;
   public int texRepWidth;
   public int texRepHeight;
   public int texRepBorderSize = 0;

   public CustomGuiTexturedRect(GuiCustom parentIn, CustomGuiTexturedRectWrapper componentIn) {
      super(parentIn, componentIn.getId(), Component.empty(), componentIn.getPosX(), componentIn.getPosY());
      component = componentIn;
      parent = parentIn;
      init();
   }

   @Override
   public void init() {
      id = component.getId();
      texture = ResourceLocation.tryParse(component.getTexture());
      setX(component.getPosX());
      setY(component.getPosY());
      setWidth(component.getWidth());
      setHeight(component.getHeight());
      textureX = component.getTextureX();
      textureY = component.getTextureY();
      textureMaxX = component.getTextureMaxX();
      textureMaxY = component.getTextureMaxY();
      hasRepeatingTexture = component.hasRepeatingTexture;
      texRepWidth = component.texRepWidth;
      texRepHeight = component.texRepHeight;
      texRepBorderSize = component.texRepBorderSize;
      enabled = component.getEnabled();
      visible = component.getVisible();
      if (component.hasHoverText()) { hoverText = component.getHoverTextList(); }
   }

   @SuppressWarnings("unused")
   public CustomGuiTexturedRect setRep(int texRepWidthIn, int texRepHeightIn, int texRepBorderSizeIn) {
      texRepWidth = texRepWidthIn;
      texRepHeight = texRepHeightIn;
      texRepBorderSize = texRepBorderSizeIn;
      hasRepeatingTexture = true;
      return this;
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!visible) { return; }
      super.render(graphics, mouseX, mouseY, partialTicks);
      if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
         listener.setHoverText(component.getHoverTextList());
      }
   }

   @Override
   protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (!visible || component.getTexture().isEmpty()) { return; }
      int x = getX();
      int y = getY();
      int r = x + (int) (width / component.getScale());
      int b = y + (int) (height  / component.getScale());
      isHovered = mouseX >= x && mouseY >= y && mouseX < r && mouseY < b;
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, texture);
      Matrix4f m = matrixStack.last().pose();
      if (textureMaxX > 0 && textureMaxY > 0) { graphics.blit(texture, x, y, (float)textureX, (float)textureY, width, height, textureMaxX, textureMaxY); }
      else if (!hasRepeatingTexture) { draw(m, (float) x, (float) y, (float) textureX, (float) textureY, (float) width, (float) height); }
      else {
         if (texRepBorderSize > 0) {
            draw(m, (float)x, (float)y, (float)textureX, (float)textureY, (float)texRepBorderSize, (float)texRepBorderSize);
            draw(m, (float)(x + width - texRepBorderSize), (float)y, (float)(textureX + texRepWidth - texRepBorderSize), (float)textureY, (float)texRepBorderSize, (float)texRepBorderSize);
            draw(m, (float)x, (float)(y + height - texRepBorderSize), (float)textureX, (float)(textureY + texRepHeight - texRepBorderSize), (float)texRepBorderSize, (float)texRepBorderSize);
            draw(m, (float)(x + width - texRepBorderSize), (float)(y + height - texRepBorderSize), (float)(textureX + texRepWidth - texRepBorderSize), (float)(textureY + texRepHeight - texRepBorderSize), (float)texRepBorderSize, (float)texRepBorderSize);
         }
         float w = (float)width - (float)texRepBorderSize * 2.0F;
         float h = (float)height - (float)texRepBorderSize * 2.0F;
         float tw = (float)texRepWidth - (float)texRepBorderSize * 2.0F;
         float th = (float)texRepHeight - (float)texRepBorderSize * 2.0F;
         float mx = w / tw;
         float my = h / th;
         for(int i = 0; (float)i < my; ++i) {
            float dh = th * Math.min(1.0F, my - (float)i);
            draw(m, (float)x, (float)(y + texRepBorderSize) + th * (float)i, (float)textureX, (float)(textureY + texRepBorderSize), (float)texRepBorderSize, dh);
            draw(m, (float)(x + width - texRepBorderSize), (float)(y + texRepBorderSize) + th * (float)i, (float)(textureX + texRepWidth - texRepBorderSize), (float)(textureY + texRepBorderSize), (float)texRepBorderSize, dh);

            for(int j = 0; (float)j < mx; ++j) {
               float dw = tw * Math.min(1.0F, mx - (float)j);
               draw(m, (float)(x + texRepBorderSize) + tw * (float)j, (float)y, (float)(textureX + texRepBorderSize), (float)textureY, dw, (float)texRepBorderSize);
               draw(m, (float)(x + texRepBorderSize) + tw * (float)j, (float)(y + height - texRepBorderSize), (float)(textureX + texRepBorderSize), (float)(textureY + texRepHeight - texRepBorderSize), dw, (float)texRepBorderSize);
               draw(m, (float)(x + texRepBorderSize) + tw * (float)j, (float)(y + texRepBorderSize) + th * (float)i, (float)(textureX + texRepBorderSize), (float)(textureY + texRepBorderSize), dw, dh);
            }
         }
      }
      matrixStack.popPose();
   }

   private void draw(Matrix4f m, float x, float y, float texX, float texY, float width, float height) {
      BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
      bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      float z = id * 0.01F;
      float f0 = 1.0f / 256.0f;
      bufferbuilder.vertex(m, x, y + height * component.getScale(), z).uv(texX * f0, (texY + height) * f0).endVertex();
      bufferbuilder.vertex(m, x + width * component.getScale(), y + height * component.getScale(), z).uv((texX + width) * f0, (texY + height) * f0).endVertex();
      bufferbuilder.vertex(m, x + width * component.getScale(), y, z).uv((texX + width) * f0, texY * f0).endVertex();
      bufferbuilder.vertex(m, x, y, z).uv(texX * f0, texY * f0).endVertex();
      BufferUploader.drawWithShader(bufferbuilder.end());
   }

   public void setTexture(ResourceLocation textureIn) { texture = textureIn; }

   @Override
   public ICustomGuiComponent component() { return component; }

   @Override
   public void playDownSound(@Nonnull SoundManager soundManager) { }

   @Override
   public GuiComponentType getElementType() { return GuiComponentType.TEXTURED_RECT; }

}
