package noppes.npcs.shared.client.gui.components.custom;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiEntityDisplayWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class CustomGuiEntityDisplay extends GuiLabel implements IComponentCustomGui {

   protected final Minecraft minecraft;
   protected Entity entity;
   public CustomGuiEntityDisplayWrapper component;

   public CustomGuiEntityDisplay(GuiCustom parent, CustomGuiEntityDisplayWrapper componentIn) {
      super(parent, componentIn.getId(), Component.empty(), componentIn.getPosX(), componentIn.getPosY());
      component = componentIn;
      minecraft = Minecraft.getInstance();
      init();
   }

   @SuppressWarnings("deprecation")
   public static void renderEntity(GuiGraphics graphics, Entity entity, double x, double y, double z, float yaw, float pitch) {
      Lighting.setupForEntityInInventory();
      EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
      entityrenderdispatcher.setRenderShadow(false);
      RenderSystem.runAsFancy(() -> entityrenderdispatcher.render(entity, x, y, z, yaw, pitch, graphics.pose(), graphics.bufferSource(), 15728880));
      graphics.flush();
      entityrenderdispatcher.setRenderShadow(true);
   }

   @Override
   public void init() {
      id = component.getId();
      setX(component.getPosX());
      setY(component.getPosY());
      setWidth(component.getWidth());
      setHeight(component.getHeight());
      if (component.entityId != -1) {
         if (minecraft.player != null) { entity = minecraft.player.getCommandSenderWorld().getEntity(component.entityId); }
      } else if (!component.getEntityData().isEmpty()) {
         if (minecraft.level != null) { entity = EntityType.create(component.getEntityData().getMCNBT(), minecraft.level).orElse(null); }
      }
      enabled = component.getEnabled();
      visible = component.getVisible();
      if (component.hasHoverText()) { hoverText = component.getHoverTextList(); }
   }

   public void setEntity(Entity entityIn) { entity = entityIn; }

   @Override
   public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!enabled || !visible) { return; }
      super.render(graphics, mouseX, mouseY, partialTicks);
      if (isHovered && component.hasHoverText() && !hoverText.isEmpty() && listener != null) {
         listener.setHoverText(component.getHoverTextList());
      }
   }

   @Override
   protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (!visible) { return; }
      if (component.getBackground()) {
         graphics.fillGradient(getX(), getY(), width + getX(), height + getY(), 0xC0101010, 0xD0101010);
      }
      if (entity != null) {
         drawEntity(graphics, entity, getX(), getY(), component.getScale(), component.getRotation() / 2 + 180, 0, mouseX, mouseY, (float)width / 2.0F, (float)height * 0.9F, component.isFollowingCursor ? 0 : 1, component.isShowingRiders());
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

   @SuppressWarnings("deprecation")
   public static void drawEntity(GuiGraphics graphics, Entity entity, double x, double y, float zoomed, int rotation, int vertical,
                                 double xMouse, double yMouse, float guiLeft, float guiTop,
                                 int followCursor, boolean showRiders) {
      if (entity == null) { return; }
      EntityNPCInterface npc = null;
      if (entity instanceof EntityNPCInterface) { npc = (EntityNPCInterface) entity; }

      LivingEntity livingEntity = null;
      if (entity instanceof LivingEntity) { livingEntity = (LivingEntity) entity; }

      float originalYaw = entity.getYRot();
      float originalPitch = entity.getXRot();
      float bodyRotation = 0.0F;
      float headRotationO = 0.0F;
      float headRotation = 0.0F;
      if (livingEntity != null) {
         bodyRotation = livingEntity.yBodyRot;
         headRotationO = livingEntity.yHeadRotO;
         headRotation = livingEntity.yHeadRot;
      }
      float scale = 1.0F;
      if ((double)entity.getBbHeight() > 2.4D) { scale = 2.0F / entity.getBbHeight(); }
      float f7 = guiLeft + (float)x - (float)xMouse;
      float f8 = (guiTop + (float)y - 50.0F * scale * zoomed) * (entity.getBbHeight() / entity.getEyeHeight()) - (float)yMouse;

      if (followCursor == 0 || followCursor == 2) { entity.setYRot((float)Math.atan(f7 / 80.0F) * 40.0F + (float) rotation); }
      else { entity.setYRot(followCursor == 1 ? 0.0f : (float) rotation); }

      if (followCursor == 0 || followCursor == 3) { entity.setXRot(-((float)Math.atan(f8 / 40.0F)) * 20.0F); }
      else { entity.setXRot(followCursor == 1 ? 0.0f : vertical); }

      if (livingEntity != null) { livingEntity.yHeadRotO = livingEntity.yHeadRot = livingEntity.yBodyRot = entity.getYRot(); }

      int orientation = 0;
      int showname = 0;
      if (npc != null) {
         orientation = npc.ais.orientation;
         npc.ais.orientation = (int)entity.getYRot();
         showname = npc.display.getShowName();
         npc.display.setShowName(1);
      }

      float scaledZoom = 30.0F * scale * zoomed;
      PoseStack posestack = RenderSystem.getModelViewStack();
      posestack.translate(0.0F, 0.0F, 1050.0F);
      posestack.scale(1.0F, 1.0F, -1.0F);
      RenderSystem.applyModelViewMatrix();
      PoseStack matrixStack = new PoseStack();
      matrixStack.translate(guiLeft + (float)x, guiTop + (float)y, 0.0F);
      matrixStack.scale(scaledZoom, scaledZoom, scaledZoom);
      matrixStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      matrixStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
      if (followCursor == 1) {
         matrixStack.mulPose(Axis.YN.rotationDegrees((float) rotation));
         matrixStack.mulPose(Axis.XN.rotationDegrees((float) vertical));
      }
      Lighting.setupForEntityInInventory();
      EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
      entityrenderdispatcher.setRenderShadow(false);
      RenderSystem.runAsFancy(() -> {
         if (showRiders) {
            entity.getPassengersAndSelf().forEach((e) -> {
               double offset = 0.0D;
               for(Entity cur = e; cur.getVehicle() != null; offset += cur.getPassengersRidingOffset()) { cur = cur.getVehicle(); }
               entityrenderdispatcher.render(entity, 0.0D, offset, 0.0D,
                       0.0F, 1.0F, matrixStack, graphics.bufferSource(), 0xF000F0);
            });
         }
         else {
            entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D,
                    0.0F, 1.0F, matrixStack, graphics.bufferSource(), 0xF000F0);
         }
      });

      graphics.flush();
      entityrenderdispatcher.setRenderShadow(true);
      posestack.scale(1.0F, 1.0F, -1.0F);
      posestack.translate(0.0F, 0.0F, -1050.0F);
      RenderSystem.applyModelViewMatrix();
      Lighting.setupFor3DItems();
      matrixStack.popPose();
      entity.setYRot(originalYaw);
      entity.setXRot(originalPitch);
      if (livingEntity != null) {
         livingEntity.yBodyRot = bodyRotation;
         livingEntity.yHeadRotO = headRotationO;
         livingEntity.yHeadRot = headRotation;
      }
      if (npc != null) {
         npc.ais.orientation = orientation;
         npc.display.setShowName(showname);
      }
   }

   @Override
   public ICustomGuiComponent component() { return component; }

   @Override
   public GuiComponentType getElementType() { return GuiComponentType.ENTITY_DISPLAY; }

}
