package noppes.npcs.shared.client.gui.components.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.DepthTestStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiColoredLineWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;

import javax.annotation.Nonnull;

public class CustomGuiColoredLine extends GuiLabel implements IComponentCustomGui {

   protected static final ShaderStateShard RENDERTYPE_GUI_SHADER = new ShaderStateShard(GameRenderer::getPositionColorShader);
   protected static final TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new TransparencyStateShard("translucent_transparency", () -> {
      RenderSystem.lineWidth(10.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   },
           () -> {
      RenderSystem.disableBlend();
      RenderSystem.lineWidth(1.0F);
      RenderSystem.defaultBlendFunc();
   });
   protected static final DepthTestStateShard L_EQUAL_DEPTH_TEST = new DepthTestStateShard("<=", 515);
   protected static final RenderType type = RenderType.create("gui", DefaultVertexFormat.POSITION_COLOR, Mode.LINES, 256, false, false, CompositeState.builder().setShaderState(RENDERTYPE_GUI_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(L_EQUAL_DEPTH_TEST).createCompositeState(false));
   public CustomGuiColoredLineWrapper component;

   public CustomGuiColoredLine(GuiCustom parent, CustomGuiColoredLineWrapper componentIn) {
      super(parent, componentIn.getId(), Component.empty(), componentIn.getPosX(), componentIn.getPosY());
      component = componentIn;
      init();
   }

   @Override
   public void init() {
      id = component.getId();
      setX(component.getPosX());
      setY(component.getPosY());
      setWidth(component.getXEnd() - component.getPosX());
      setHeight(component.getYEnd() - component.getPosY());
      enabled = true;
      visible = component.getVisible();
      hoverText.clear();
   }

   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!enabled || !visible) { return; }
      int color = component.getColor();
      int r = color >> 24 & 255;
      int g = color >> 16 & 255;
      int b = color >> 8 & 255;
      int a = color & 255;
      double dx = component.getXEnd() - getX();
      double dy = component.getYEnd() - getY();
      double length = Math.sqrt(dx * dx + dy * dy);
      double nx = -dy / length * component.getThickness() / 2.0D;
      double ny = dx / length * component.getThickness() / 2.0D;
      double z = id * 0.01D;
      VertexConsumer builder = graphics.bufferSource().getBuffer(RenderType.gui());
      builder.vertex(component.getXEnd() + nx, component.getYEnd() + ny, z).color(r, g, b, a).endVertex();
      builder.vertex(component.getXEnd() - nx, component.getYEnd() - ny, z).color(r, g, b, a).endVertex();
      builder.vertex((double) getX() - nx, (double) getY() - ny, z).color(r, g, b, a).endVertex();
      builder.vertex((double) getX() + nx, (double) getY() + ny, z).color(r, g, b, a).endVertex();
      graphics.flush();
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
      return true;
   }

   @Override
   public ICustomGuiComponent component() { return component; }

}
