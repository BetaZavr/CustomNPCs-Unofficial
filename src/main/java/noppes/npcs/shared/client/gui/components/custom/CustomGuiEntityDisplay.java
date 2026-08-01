package noppes.npcs.shared.client.gui.components.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiEntityDisplayWrapper;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiLabel;

public class CustomGuiEntityDisplay extends GuiLabel implements IComponentCustomGui {

   protected final Minecraft minecraft;
   protected Entity entity;
   public CustomGuiEntityDisplayWrapper component;

   public CustomGuiEntityDisplay(GuiCustom parent, CustomGuiEntityDisplayWrapper componentIn) {
      super(parent, componentIn.getId(), Component.empty(), componentIn.getPosX(), componentIn.getPosY());
      component = componentIn;
      minecraft = Minecraft.getMinecraft();
      init();
   }

   @Override
   public void init() {
      id = component.getId();
      setX(component.getPosX());
      setY(component.getPosY());
      setWidth(component.getWidth());
      setHeight(component.getHeight());
      if (component.entityId != -1) {
         if (minecraft.player != null) { entity = minecraft.player.getEntityWorld().getEntityByID(component.entityId); }
      } else if (!component.getEntityData().isEmpty()) {
         if (minecraft.world != null) { entity = EntityList.createEntityFromNBT(component.getEntityData().getMCNBT(), minecraft.world); }
      }
      enabled = component.getEnabled();
      visible = component.getVisible();
      if (component.hasHoverText()) { hoverText = component.getHoverTextList(); }
   }

   public void setEntity(Entity entityIn) { entity = entityIn; }

   @Override
   public void render(int mouseX, int mouseY, float partialTicks) {
      if (!enabled || !visible) { return; }
      super.render(mouseX, mouseY, partialTicks);
      if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
         listener.setHoverText(component.getHoverTextList());
      }
   }

   @Override
   public void renderWidget(int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (!visible) { return; }
      if (component.getBackground()) {
         drawGradientRect(getX(), getY(), width + getX(), height + getY(), 0xC0101010, 0xD0101010);
      }
      if (entity != null) {
         drawEntity(entity, getX(), getY(), component.getScale(), component.getRotation() / 2 + 180, 0, mouseX, mouseY, (float)width / 2.0F, (float)height * 0.9F, component.isFollowingCursor ? 0 : 1);
      }
      int x = (int) (getX() / component.getScale());
      int y = (int) (getY() / component.getScale());
      int r = (int) ((getX() + width) / component.getScale());
      int b = (int) ((getY() + height)  / component.getScale());
      isHovered = mouseX >= x && mouseY >= y && mouseX < r && mouseY < b;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
      return true;
   }

   public static void drawEntity(Entity entity, double x, double y, float zoomed, int rotation, int vertical, double xMouse, double yMouse, float guiLeft, float guiTop, int followCursor) {
      if (entity == null) { return; }

      GlStateManager.pushMatrix();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableColorMaterial();
      float lastBrightnessX = OpenGlHelper.lastBrightnessX;
      float lastBrightnessY = OpenGlHelper.lastBrightnessY;
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

      EntityNPCInterface npc = null;
      if (entity instanceof EntityNPCInterface) { npc = (EntityNPCInterface)entity; }
      EntityLiving livingEntity = null;
      if (entity instanceof EntityLiving) { livingEntity = (EntityLiving) entity; }
      float originalYaw = entity.rotationYaw;
      float originalPitch = entity.rotationPitch;
      float bodyRotation = 0.0F;
      float headRotationO = 0.0F;
      float headRotation = 0.0F;
      if (livingEntity != null) {
         bodyRotation = livingEntity.renderYawOffset;
         headRotationO = livingEntity.prevRotationYawHead;
         headRotation = livingEntity.rotationYawHead;
      }
      float scale = 1.0F;
      if (entity.height > 2.4F) { scale = 2.0F / entity.height; }
      float f7 = guiLeft + (float)x - (float)xMouse;
      float f8 = (guiTop + (float)y - 50.0F * scale * zoomed) * (entity.height / entity.getEyeHeight()) - (float)yMouse;

      if (followCursor == 0 || followCursor == 2) { entity.rotationYaw = (float)Math.atan(f7 / 80.0F) * 40.0F + (float) rotation; }
      else { entity.rotationYaw = followCursor == 1 ? 0.0f : (float) rotation; }

      if (followCursor == 0 || followCursor == 3) { entity.rotationPitch = -((float)Math.atan(f8 / 40.0F)) * 20.0F; }
      else { entity.rotationPitch = followCursor == 1 ? 0.0f : vertical; }

      if (livingEntity != null) { livingEntity.renderYawOffset = livingEntity.rotationYawHead = livingEntity.rotationYaw; }

      int orientation = 0;
      int showname = 0;
      if (npc != null) {
         orientation = npc.ais.orientation;
         npc.ais.orientation = (int) entity.rotationYaw;
         showname = npc.display.getShowName();
         npc.display.setShowName(1);
      }

      if (npc instanceof EntityCustomNpc) {
         EntityCustomNpc cnpc = (EntityCustomNpc) npc;
         if (cnpc.modelData.getEntity(cnpc) != null) {
            EntityUtil.Copy(npc, cnpc.modelData.getEntity(cnpc));
         }
      }

      float scaledZoom = 30.0F * scale * zoomed;
      GlStateManager.pushMatrix();
      GlStateManager.translate(guiLeft + (float) x, guiTop + (float) y, 50.0F);
      GlStateManager.scale(scaledZoom, scaledZoom, -scaledZoom);
      GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
      RenderHelper.enableStandardItemLighting();
      if (followCursor == 1) {
         GlStateManager.rotate(rotation, 0.0F, 1.0F, 0.0F);
         GlStateManager.rotate(vertical, 1.0F, 0.0F, 0.0F);
      }
      RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
      renderManager.setRenderShadow(false);
      renderManager.renderEntity(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
      renderManager.setRenderShadow(true);

      GlStateManager.popMatrix();

      entity.rotationYaw = originalYaw;
      entity.rotationPitch = originalPitch;
      if (livingEntity != null) {
         livingEntity.renderYawOffset = bodyRotation;
         livingEntity.prevRotationYawHead = headRotationO;
         livingEntity.rotationYawHead = headRotation;
      }
      if (npc != null) {
         npc.ais.orientation = orientation;
         npc.display.setShowName(showname);
      }
      if (npc instanceof EntityCustomNpc) {
         EntityCustomNpc cnpc = (EntityCustomNpc) npc;
         if (cnpc.modelData.getEntity(cnpc) != null) { EntityUtil.Copy(npc, cnpc.modelData.getEntity(cnpc)); }
      }

      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
      RenderHelper.disableStandardItemLighting();
      GlStateManager.disableColorMaterial();
      GlStateManager.disableRescaleNormal();
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GlStateManager.disableTexture2D();
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      GlStateManager.enableTexture2D();
      GlStateManager.enableCull();
      GlStateManager.depthMask(true);
      GlStateManager.depthFunc(515);
      GlStateManager.shadeModel(7424);
      GlStateManager.alphaFunc(516, 0.1F);
      GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   @Override
   public ICustomGuiComponent component() { return component; }

   @Override
   public GuiComponentType getElementType() { return GuiComponentType.ENTITY_DISPLAY; }

}
