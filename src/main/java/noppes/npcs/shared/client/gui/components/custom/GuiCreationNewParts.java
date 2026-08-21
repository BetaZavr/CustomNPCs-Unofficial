package noppes.npcs.shared.client.gui.components.custom;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.gui.subgui.AssetsGui;
import noppes.npcs.api.wrapper.gui.CustomGuiButtonListWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiButtonWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiEntityDisplayWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiScrollWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiSliderWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiTextFieldWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiTexturedRectWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiWrapper;
import noppes.npcs.api.wrapper.gui.GuiComponentsScrollableWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.client.gui.model.GuiModelColor;
import noppes.npcs.client.layer.LayerParts;
import noppes.npcs.client.parts.*;
import noppes.npcs.constants.BodyPart;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiParts;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;
import noppes.npcs.shared.common.util.*;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class GuiCreationNewParts
        extends GuiLabel
        implements IComponentCustomGui, ICustomScrollListener {

   private static String active = "";
   private static PlayerModel<LivingEntity> biped;
   private static final ResourceLocation colorWheel = new ResourceLocation("moreplayermodels", "textures/gui/colorwheel.png");
   public static final ResourceLocation buttonsResource = new ResourceLocation("moreplayermodels", "textures/gui/arrowbuttons.png");

   private final CustomGuiScroll scroll;
   private final CustomGuiSlider slider;
   private final CustomGuiEntityDisplay entity;
   private final ModelData data;
   private final ModelData renderData;
   private final EntityCustomNpc npc;
   private final Minecraft minecraft;
   private final List<GuiCreationNewParts.GuiMpmPart> guiParts = new ArrayList<>();
   public GuiCustom listener;

   public GuiCreationNewParts(GuiCustom parentIn, EntityCustomNpc npcIn) {
      super(parentIn, 0, Component.empty(), 0, 0);
      listener = parentIn;
      setSize(420, 200);
      npc = npcIn;
      renderData = new ModelData(npc);
      data = npc.modelData;
      minecraft = Minecraft.getInstance();
      String[] menus = MpmPartReader.PARTS.values().stream().map((p) -> p.menu).sorted(new NaturalOrderComparator()).distinct().toArray(String[]::new);
      if (active.isEmpty()) { active = menus[0]; }
      scroll = new CustomGuiScroll(this, new CustomGuiScrollWrapper(10, 4, 24, 100, 210, menus));
      scroll.disabledSearch();
      CustomGuiEntityDisplayWrapper wrapper = new CustomGuiEntityDisplayWrapper(-2, npc.wrappedNPC, 106, 90);
      wrapper.setSize(68, 90);
      entity = new CustomGuiEntityDisplay(parentIn, wrapper);
      slider = (new CustomGuiSlider(parentIn, (new CustomGuiSliderWrapper(-3, "", 106, 186, 68, 20)).setMax(360.0F).setDecimals(0).setValue(180.0F).setOnChange((gui, slider) -> {
         entity.component.setRotation((int)slider.getValue() - 180);
         entity.init();
      }))).disablePackets();
      biped = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), true);
   }

   public void openSubgui(GuiCustom parent, GuiCustom subgui) {
      subgui.init(minecraft, parent.width, parent.height);
      subgui.parent = parent;
      parent.subgui = subgui;
      if (subgui.guiWrapper != null) { subgui.background = new CustomGuiTexturedRect(subgui, (CustomGuiTexturedRectWrapper)subgui.guiWrapper.getBackgroundRect()); }
      if (subgui.scrollingPanel.comps == null) { subgui.scrollingPanel.comps = new GuiComponentsScrollableWrapper(subgui.guiWrapper, null); }
   }

   public int getID() {
      return -10;
   }

   @SuppressWarnings("unused")
   public void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      entity.visible = listener.subgui == null;
      render(graphics, mouseX, mouseY, partialTicks);
   }

   public void init() {
      listener.add(scroll);
      listener.add(entity);
      listener.add(slider);
      List<MpmPart> list = MpmPartReader.PARTS.values().stream().sorted(Comparator.comparing((t) -> t.id)).filter((t) -> t.menu.equals(active) && t.parentId == null).toList();
      scroll.setSelected(active);
      entity.setEntity(npc);
      int i;
      int column;
      if (guiParts.isEmpty()) {
         for(i = 0; i < list.size(); ++i) {
            column = i % 3;
            MpmPart part = list.get(i);
            GuiCreationNewParts.GuiMpmPart gui = new GuiCreationNewParts.GuiMpmPart(listener, 80 + i, column * 70 + column, i / 3 * 70, part);
            guiParts.add(gui);
            listener.addPanel(gui);
            listener.scrollingPanel.comps.addComponent(new PartsWrapper(gui));
         }
      }
      else {
         for(i = 0; i < guiParts.size(); ++i) {
            column = i % 3;
            GuiCreationNewParts.GuiMpmPart gui = guiParts.get(i);
            gui.setX(column * 70 + column);
            gui.setY(i / 3 * 70);
            listener.addPanel(gui);
         }
      }
      listener.scrollingPanel.setMaxSize(guiParts.stream().mapToInt((v) -> v.getY() + v.getHeight()).max().orElse(0));
   }

   @Override
   public ICustomGuiComponent component() { return null; }

   @Override
   protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) { return false; }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }

   public void save() { Packets.sendServer(new SPacketCustomGuiParts(data.save())); }

   public void openTextureSubgui(GuiCustom parentIn, MpmPartData dataIn, MpmPart partIn) {
      GuiCreationNewParts.TexturePart screen = new GuiCreationNewParts.TexturePart(dataIn, partIn);
      CustomGuiWrapper gui = screen.guiWrapper;
      gui.setBackgroundTexture(CustomNpcs.MODID + ":textures/gui/components.png");
      gui.setSize(310, 200);
      gui.getBackgroundRect().setTextureOffset(0, 0);
      gui.getBackgroundRect().setRepeatingTexture(64, 64, 4);
      gui.addButton(66, "x", 276, 4, 20, 20).setOnPress((gui2, button) -> screen.onClose()).setDisablePackets();
      if (!partIn.disableCustomTextures) {
         gui.addLabel(21, "gui.playerskin", 4, 110, 10, 100);
         gui.addButtonList(22, 76, 105, 50, 20).setValues("gui.no", "gui.yes").setSelected(dataIn.usePlayerSkin ? 1 : 0).setOnPress((gui2, button) -> {
            dataIn.usePlayerSkin = ((CustomGuiButtonListWrapper)button).getSelected() == 1;
            gui2.getComponent(23).setVisible(!dataIn.usePlayerSkin);
            gui2.getComponent(24).setVisible(!dataIn.usePlayerSkin);
            gui2.getComponent(25).setVisible(!dataIn.usePlayerSkin);
            gui2.getComponent(26).setVisible(!dataIn.usePlayerSkin);
            gui2.getComponent(27).setVisible(!dataIn.usePlayerSkin);
            data.refreshParts();
            save();
            screen.init();
         }).setDisablePackets();
         gui.addLabel(23, "gui.texture", 4, 130, 10, 100).setVisible(!dataIn.usePlayerSkin);
         ResourceLocation loc = dataIn.getDefaultTexture();
         CustomGuiTextFieldWrapper tf = (CustomGuiTextFieldWrapper)gui.addTextField(24, 4, 140, 220, 20).setText(loc == null ? "" : loc.toString()).setOnFocusLost((gui2, text) -> dataIn.setTexture(text.getText())).setVisible(!dataIn.usePlayerSkin).setDisablePackets();
         gui.addButton(25, "gui.select", 226, 140, 80, 20).setOnPress((gui2, button) -> openSubgui(screen, openTextureBasic(dataIn.getDefaultTexture() == null ? "" : dataIn.getDefaultTexture().toString(), (resource) -> {
            dataIn.setTexture(resource);
            tf.setText(resource);
            data.refreshParts();
            save();
            screen.init();
         }))).setVisible(!dataIn.usePlayerSkin).setDisablePackets();
         gui.addLabel(26, "config.skinurl", 4, 168, 10, 100).setVisible(!dataIn.usePlayerSkin);
         gui.addTextField(27, 4, 178, 220, 20).setText(dataIn.url).setOnFocusLost((gui2, text) -> {
            dataIn.setUrl(text.getText());
            data.refreshParts();
            save();
            screen.init();
         }).setVisible(!dataIn.usePlayerSkin).setDisablePackets();
      }

      screen.setGuiWrapper(gui);
      openSubgui(parentIn, screen);
   }

   public GuiCustom openTextureBasic(String resource, AssetsGui.SelectionCallback callback) {
      GuiCustom screen = new GuiCustom(listener.getMenu(), listener.inv, Component.empty());
      CustomGuiWrapper gui = screen.guiWrapper = new CustomGuiWrapper(null);
      gui.setBackgroundTexture(CustomNpcs.MODID + ":textures/gui/components.png");
      gui.setSize(308, 214);
      gui.getBackgroundRect().setTextureOffset(0, 0);
      gui.getBackgroundRect().setRepeatingTexture(64, 64, 4);
      CustomGuiButtonWrapper b = gui.addTexturedButton(666, "X", 290, -4, 14, 14, CustomNpcs.MODID + ":textures/gui/components.png", 0, 64);
      b.getTextureRect().setRepeatingTexture(64, 22, 3).setHoverText("gui.close");
      b.setTextureHoverOffset(22).setOnPress((guii, bb) -> screen.onClose());
      b.setDisablePackets();
      gui.addAssetsSelector(11, 4, 4, 300, 204).setSelected(resource).setOnPress((gui2, assets) -> screen.onClose()).setOnChange((gui2, assets) -> callback.call(assets.getSelected())).setDisablePackets();
      screen.setGuiWrapper(gui);
      return screen;
   }

   public void openEyesSubgui(GuiCustom parent, ModelEyeData data, MpmPartEyes part) {
      GuiCreationNewParts.EyesPart screen = new GuiCreationNewParts.EyesPart(data, part);
      CustomGuiWrapper gui = screen.guiWrapper = new CustomGuiWrapper(null);
      gui.setBackgroundTexture(CustomNpcs.MODID + ":textures/gui/components.png");
      gui.setSize(310, 200);
      gui.getBackgroundRect().setTextureOffset(0, 0);
      gui.getBackgroundRect().setRepeatingTexture(64, 64, 4);
      int y = 8;
      gui.addLabel(21, "part.eyes", 56, y + 5, 10, 100);
      gui.addButtonList(22, 110, y, 110, 20).setValues("gui.playerskin", "gui.normal", "gui.texture").setSelected(data.skinType).setOnPress((gui2, button) -> {
         data.skinType = ((CustomGuiButtonListWrapper)button).getSelected();
         gui2.getComponent(23).setVisible(data.skinType == 1);
         gui2.getComponent(24).setVisible(data.skinType == 2);
         gui2.getComponent(25).setVisible(data.skinType == 2);
         gui2.getComponent(27).setVisible(data.glint || data.skinType == 1 || data.skinType == 2);
         screen.init();
      }).setDisablePackets();
      gui.addButton(23, "", 230, y, 50, 20).setOnPress((gui2, button) -> openSubgui(screen, new GuiModelColor(screen, ColorUtil.rgbToColor(data.color), (color) -> {
         data.color = ColorUtil.colorToRgb(color);
         screen.init();
      }))).setVisible(data.skinType == 1).setDisablePackets();
      y += 25;
      gui.addLabel(24, "config.skinurl", 56, y + 5, 10, 100).setVisible(data.skinType == 2);
      gui.addTextField(25, 110, y, 195, 20).setText(data.url).setOnFocusLost((gui2, text) -> data.setUrl(text.getText())).setVisible(data.skinType == 2).setDisablePackets();
      y += 25;
      gui.addButtonList(26, 54, y, 100, 20).setValues("gui.normal", "gui.big").setSelected(data.eyeSize).setOnPress((gui2, button) -> {
         data.eyeSize = ((CustomGuiButtonListWrapper)button).getSelected();
         screen.init();
      }).setDisablePackets();
      gui.addButtonList(27, 156, y, 100, 20).setValues("gui.normal", "gui.mirror").setSelected(data.mirror ? 1 : 0).setOnPress((gui2, button) -> {
         data.mirror = ((CustomGuiButtonListWrapper)button).getSelected() == 1;
         screen.init();
      }).setVisible(data.glint || data.skinType == 1 || data.skinType == 2).setDisablePackets();
      gui.addLabel(28, "eye.pupil", 4, y + 5, 10, 100);
      y += 25;
      CustomGuiButtonListWrapper var10000 = gui.addButtonList(29, 54, y, 100, 20);
      String[] var10001 = new String[5];
      String var10004 = Component.translatable("gui.down").toString();
      var10001[0] = var10004 + "x2";
      var10001[1] = "gui.down";
      var10001[2] = "gui.normal";
      var10001[3] = "gui.up";
      var10004 = Component.translatable("gui.up").toString();
      var10001[4] = var10004 + "x2";
      var10000.setValues(var10001).setSelected(data.eyePos.y + 2).setOnPress((gui2, button) -> data.eyePos = new NopVector2i(data.eyePos.x, ((CustomGuiButtonListWrapper)button).getSelected() - 2)).setDisablePackets();
      gui.addButtonList(30, 156, y, 100, 20).setValues("gui.inward", "gui.normal", "gui.outward").setSelected(data.eyePos.x + 1).setOnPress((gui2, button) -> data.eyePos = new NopVector2i(((CustomGuiButtonListWrapper)button).getSelected() - 1, data.eyePos.y)).setDisablePackets();
      gui.addLabel(31, "gui.position", 4, y + 5, 10, 100);
      y += 25;
      gui.addButtonList(32, 54, y, 50, 20).setValues("gui.no", "gui.yes").setSelected(data.glint ? 1 : 0).setOnPress((gui2, button) -> {
         data.glint = ((CustomGuiButtonListWrapper)button).getSelected() == 1;
         gui2.getComponent(27).setVisible(data.glint || data.skinType == 1 || data.skinType == 2);
      }).setDisablePackets();
      gui.addLabel(33, "eye.glint", 4, y + 5, 10, 100);
      gui.addButton(34, "", 162, y, 50, 20).setOnPress((gui2, button) -> openSubgui(screen, new GuiModelColor(screen, ColorUtil.rgbToColor(data.browColor), (color) -> {
         data.browColor = ColorUtil.colorToRgb(color);
         screen.init();
      }))).setDisablePackets();
      gui.addButtonList(35, 214, y, 70, 20).setValues("gui.disabled", "1", "2", "3", "4", "5", "6", "7", "8").setSelected((int)(data.browThickness.y * 10.0F)).setOnPress((gui2, button) -> data.browThickness = new NopVector3f(1.0F, (float)((CustomGuiButtonListWrapper)button).getSelected() / 10.0F, 1.0F)).setDisablePackets();
      gui.addLabel(36, "eye.lash", 112, y + 5, 10, 100);
      y += 25;
      gui.addButtonList(37, 54, y, 50, 20).setValues("gui.no", "gui.yes").setSelected(data.disableBlink ? 0 : 1).setOnPress((gui2, button) -> {
         data.disableBlink = ((CustomGuiButtonListWrapper)button).getSelected() == 0;
         gui2.getComponent(39).setVisible(!data.disableBlink);
         gui2.getComponent(40).setVisible(!data.disableBlink);
         screen.init();
      }).setDisablePackets();
      gui.addLabel(38, "eye.blink", 4, y + 5, 10, 100);
      gui.addLabel(39, "eye.lid", 112, y + 5, 10, 100).setVisible(!data.disableBlink);
      gui.addButton(40, "", 162, y, 50, 20).setOnPress((gui2, button) -> openSubgui(screen, new GuiModelColor(screen, ColorUtil.rgbToColor(data.lidColor), (color) -> {
         data.lidColor = ColorUtil.colorToRgb(color);
         screen.init();
      }))).setVisible(!data.disableBlink).setDisablePackets();
      gui.addButton(66, "x", 288, 4, 20, 20).setOnPress((gui2, button) -> screen.onClose()).setDisablePackets();
      screen.setGuiWrapper(gui);
      openSubgui(parent, screen);
   }

   public class GuiMpmPart extends GuiLabel implements IComponentCustomGui {

      protected List<MpmPart> all = new ArrayList<>();
      protected MpmPart part;
      protected MpmPartData data;
      protected boolean selected = true;
      protected boolean colorPickerHovered = false;
      protected boolean infoHovered = false;
      protected boolean settingsHovered = false;
      protected boolean hoverL = false;
      protected boolean hoverR = false;
      protected int zPos = 0;
      public static final int SIZE = 70;
      public boolean basic = false;
      public GuiCustom listener;

      public GuiMpmPart(GuiCustom parent, int id, int x, int y, MpmPart partIn) {
         super(parent, id, Component.empty(), x, y);
         setSize(SIZE, SIZE);
         listener = parent;
         part = partIn;
         all.add(partIn);
         for (Entry<ResourceLocation, MpmPart> entry : MpmPartReader.PARTS.entrySet()) {
            if (entry.getValue().parentId != null && entry.getValue().parentId.equals(partIn.id)) {
               all.add(entry.getValue());
            }
         }
         for (MpmPart p : all) {
            data = GuiCreationNewParts.this.data.mpmParts.stream().filter((t) -> t.partId.equals(p.id)).findFirst().orElse(null);
            if (data != null) {
               part = p;
               break;
            }
         }

         all = all.stream().sorted(Comparator.comparing((t) -> t.id)).collect(Collectors.toList());
         if (data == null) {
            if (!partIn.id.equals(ModelEyeData.RESOURCE) && !partIn.id.equals(ModelEyeData.RESOURCE_RIGHT) && !partIn.id.equals(ModelEyeData.RESOURCE_LEFT)) { data = new MpmPartData(); }
            else { data = new ModelEyeData(); }
            data.partId = partIn.id;
            data.usePlayerSkin = partIn.defaultUsePlayerSkins;
            selected = false;
         }
      }

      @SuppressWarnings("deprecation")
      public void renderModel(GuiGraphics graphics) {
         int x1 = getX();
         int x2 = getX() + SIZE;
         int y1 = getY();
         int y2 = getY() + SIZE - 1;
         graphics.fill(x1, y1, x2, y2, -3750202);
         renderData.mpmParts = GuiCreationNewParts.this.data.mpmParts;
         PoseStack posestack = RenderSystem.getModelViewStack();
         posestack.pushPose();
         posestack.translate(0.0D, 0.0D, 100.0D + (double) zPos);
         posestack.scale(1.0F, 1.0F, -1.0F);
         RenderSystem.applyModelViewMatrix();
         PoseStack matrixStack = new PoseStack();
         matrixStack.translate((float) getX(), (float)(getY() - listener.scrollingPanel.comps.scrollAmount), 1.0F);
         matrixStack.pushPose();
         EntityRenderDispatcher entityRendererManager = minecraft.getEntityRenderDispatcher();
         entityRendererManager.setRenderShadow(false);
         BufferSource iRenderTypeBuffer = minecraft.renderBuffers().bufferSource();
         VertexConsumer iVertex = iRenderTypeBuffer.getBuffer(RenderType.entityCutoutNoCull(npc.textureLocation));
         Lighting.setupForEntityInInventory();
         RenderSystem.runAsFancy(() -> {
            GuiCreationNewParts.biped.leftLeg.visible = !part.hiddenParts.contains(BodyPart.LEFT_LEG) && !part.hiddenParts.contains(BodyPart.LEGS);
            GuiCreationNewParts.biped.leftPants.visible = GuiCreationNewParts.biped.leftPants.visible && GuiCreationNewParts.biped.leftLeg.visible;
            GuiCreationNewParts.biped.rightLeg.visible = !part.hiddenParts.contains(BodyPart.RIGHT_LEG) && !part.hiddenParts.contains(BodyPart.LEGS);
            GuiCreationNewParts.biped.rightPants.visible = GuiCreationNewParts.biped.rightPants.visible && GuiCreationNewParts.biped.rightLeg.visible;
            GuiCreationNewParts.biped.leftArm.visible = !part.hiddenParts.contains(BodyPart.LEFT_ARM) && !part.hiddenParts.contains(BodyPart.ARMS);
            GuiCreationNewParts.biped.leftSleeve.visible = GuiCreationNewParts.biped.leftSleeve.visible && GuiCreationNewParts.biped.leftArm.visible;
            GuiCreationNewParts.biped.rightArm.visible = !part.hiddenParts.contains(BodyPart.RIGHT_ARM) && !part.hiddenParts.contains(BodyPart.ARMS);
            GuiCreationNewParts.biped.rightSleeve.visible = GuiCreationNewParts.biped.rightSleeve.visible && GuiCreationNewParts.biped.rightArm.visible;
            GuiCreationNewParts.biped.body.visible = !part.hiddenParts.contains(BodyPart.BODY);
            GuiCreationNewParts.biped.jacket.visible = GuiCreationNewParts.biped.jacket.visible && GuiCreationNewParts.biped.body.visible;
            GuiCreationNewParts.biped.head.visible = !part.hiddenParts.contains(BodyPart.HEAD);
            GuiCreationNewParts.biped.hat.visible = GuiCreationNewParts.biped.hat.visible && GuiCreationNewParts.biped.head.visible;
            if (part.bodyPart == BodyPart.HEAD) {
               matrixStack.translate(32.0F, 46.0F, 25.0F);
               matrixStack.scale(36.0F, 36.0F, 36.0F);
               matrixStack.mulPose(Axis.XP.rotation(0.3926991F));
               matrixStack.mulPose(Axis.YP.rotation((float) part.previewRotation * 0.017453292F));
               GuiCreationNewParts.biped.head.render(matrixStack, iVertex, 15728880, OverlayTexture.NO_OVERLAY);
            }
            ModelPartWrapper modelPart;
            if (part.bodyPart == BodyPart.LEGS) {
               matrixStack.translate(18.0F, 12.0F, 25.0F);
               matrixStack.scale(36.0F, 36.0F, 36.0F);
               matrixStack.mulPose(Axis.XP.rotation(0.3926991F));
               matrixStack.mulPose(Axis.YP.rotation((float) part.previewRotation * 0.017453292F));
               GuiCreationNewParts.biped.body.render(matrixStack, iVertex, 15728880, OverlayTexture.NO_OVERLAY);
               if (part.animationType == PartBehaviorType.LEGS) {
                  modelPart = part.getPart("right_leg");
                  if (modelPart != null) {
                     modelPart.setRot(new NopVector3f(GuiCreationNewParts.biped.rightLeg.xRot, GuiCreationNewParts.biped.rightLeg.yRot, GuiCreationNewParts.biped.rightLeg.zRot));
                     modelPart.setPos(new NopVector3f(GuiCreationNewParts.biped.rightLeg.x, GuiCreationNewParts.biped.rightLeg.y, GuiCreationNewParts.biped.rightLeg.z));
                  }

                  modelPart = part.getPart("left_leg");
                  if (modelPart != null) {
                     modelPart.setRot(new NopVector3f(GuiCreationNewParts.biped.leftLeg.xRot, GuiCreationNewParts.biped.leftLeg.yRot, GuiCreationNewParts.biped.leftLeg.zRot));
                     modelPart.setPos(new NopVector3f(GuiCreationNewParts.biped.leftLeg.x, GuiCreationNewParts.biped.leftLeg.y, GuiCreationNewParts.biped.leftLeg.z));
                  }
               }

               GuiCreationNewParts.biped.rightLeg.render(matrixStack, iVertex, 15728880, OverlayTexture.NO_OVERLAY);
               GuiCreationNewParts.biped.leftLeg.render(matrixStack, iVertex, 15728880, OverlayTexture.NO_OVERLAY);
            }

            if (part.bodyPart == BodyPart.ARMS) {
               matrixStack.translate(18.0F, 12.0F, 25.0F);
               matrixStack.scale(36.0F, 36.0F, 36.0F);
               matrixStack.mulPose(Axis.XP.rotation(0.3926991F));
               matrixStack.mulPose(Axis.YP.rotation((float) part.previewRotation * 0.017453292F));
               GuiCreationNewParts.biped.body.render(matrixStack, iVertex, 15728880, OverlayTexture.NO_OVERLAY);
               if (part.animationType == PartBehaviorType.ARMS) {
                  modelPart = part.getPart("right_arm");
                  if (modelPart != null) {
                     modelPart.setRot(new NopVector3f(GuiCreationNewParts.biped.rightArm.xRot, GuiCreationNewParts.biped.rightArm.yRot, GuiCreationNewParts.biped.rightArm.zRot));
                     modelPart.setPos(new NopVector3f(GuiCreationNewParts.biped.rightArm.x, GuiCreationNewParts.biped.rightArm.y, GuiCreationNewParts.biped.rightArm.z));
                  }

                  modelPart = part.getPart("left_arm");
                  if (modelPart != null) {
                     modelPart.setRot(new NopVector3f(GuiCreationNewParts.biped.leftArm.xRot, GuiCreationNewParts.biped.leftArm.yRot, GuiCreationNewParts.biped.leftArm.zRot));
                     modelPart.setPos(new NopVector3f(GuiCreationNewParts.biped.leftArm.x, GuiCreationNewParts.biped.leftArm.y, GuiCreationNewParts.biped.leftArm.z));
                  }
               }

               GuiCreationNewParts.biped.leftArm.render(matrixStack, iVertex, 15728880, OverlayTexture.NO_OVERLAY);
               GuiCreationNewParts.biped.rightArm.render(matrixStack, iVertex, 15728880, OverlayTexture.NO_OVERLAY);
            }

            if (part.bodyPart == BodyPart.BODY) {
               matrixStack.translate(18.0F, 18.0F, 25.0F);
               matrixStack.scale(36.0F, 36.0F, 36.0F);
               matrixStack.mulPose(Axis.XP.rotation(0.3926991F));
               matrixStack.mulPose(Axis.YP.rotation((float) part.previewRotation * 0.017453292F));
               GuiCreationNewParts.biped.body.render(matrixStack, iVertex, 15728880, OverlayTexture.NO_OVERLAY);
            }

            if (part.renderType != PartRenderType.NONE) {
               MpmPartAbstractClient partC = (MpmPartAbstractClient) part;
               partC.pos = NopVector3f.ZERO;
               partC.rot = NopVector3f.ZERO;
               LayerParts.renderPart(data, partC, matrixStack, iRenderTypeBuffer, 15728880, npc, GuiCreationNewParts.biped, renderData);
            }

         });
         iRenderTypeBuffer.endBatch();
         matrixStack.popPose();
         posestack.popPose();
         entityRendererManager.setRenderShadow(true);
         RenderSystem.applyModelViewMatrix();
      }

      public void renderWidget(@Nonnull GuiGraphics graphics, int xMouse, int yMouse, float tick) { }

      public void renderIcons(GuiGraphics graphics, int xMouse, int yMouse) {
         int colorX = -1;
         int guiY;
         int x1;
         int x2x;
         if (!basic) {
            if (isHovered) { colorX = new Color(0xFFFF0000).getRGB(); }
            guiY = getX();
            int x2 = getX() + SIZE;
            x1 = getY();
            x2x = getY() + SIZE - 1;
            graphics.hLine(guiY, x2, x1, colorX);
            graphics.hLine(guiY, x2, x2x, colorX);
            graphics.hLine(guiY, x1, x2x, colorX);
            graphics.hLine(x2, x1, x2x, colorX);
            guiY = getX() + SIZE - 16;
            x2 = getX() + SIZE;
            x1 = getY() + 1;
            x2x = getY() + SIZE - 1;
            graphics.fill(guiY, x1, x2, x2x, -3750202);
            int color = -1;
            guiY = getX() + SIZE - 14;
            x2 = getX() + SIZE - 2;
            x1 = getY() + 2;
            x2x = getY() + 14;
            graphics.fill(guiY, x1, x2, x2x, -16777216);
            graphics.hLine(guiY, x2, x1, color);
            graphics.hLine(guiY, x2, x2x, color);
            graphics.hLine(guiY, x1, x2x, color);
            graphics.hLine(x2, x1, x2x, color);
            if (!part.isEnabled) {
               graphics.drawString(minecraft.font, Component.literal("X").withStyle(ChatFormatting.BOLD), guiY + 4, x1 + 3, 16711680);
            } else if (selected) {
               char c = (char) Integer.parseInt("2713", 16);
               graphics.drawString(minecraft.font, Component.literal("" + c).withStyle(ChatFormatting.BOLD), guiY + 3, x1 + 2, 65280);
            }
         }
         guiY = getY() + 16;
         RenderSystem.setShaderTexture(0, GuiCreationNewParts.colorWheel);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         int size = 14;
         x1 = getX() + SIZE - 15;
         x2x = x1 + size;
         int y1 = guiY;
         int y2 = guiY + size;
         colorPickerHovered = xMouse >= x1 && yMouse >= guiY && xMouse < x2x && yMouse < y2;
         if (colorPickerHovered) {
            --x1;
            y1 = guiY - 1;
            size = 16;
         }
         graphics.blit(GuiCreationNewParts.colorWheel, x1, y1, 0, 0.0F, 0.0F, size, size, size, size);
         guiY += 15;
         RenderSystem.setShaderTexture(0, GuiCreationNewParts.buttonsResource);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         if (all.size() > 1) {
            x1 = getX() + SIZE - 17;
            x2x = x1 + 6;
            y2 = guiY + 8;
            RenderSystem.setShaderTexture(0, GuiCreationNewParts.buttonsResource);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            hoverL = xMouse >= x1 && yMouse >= guiY && xMouse < x2x && yMouse < y2;
            graphics.blit(GuiCreationNewParts.buttonsResource, x1, guiY, 0, hoverL ? 76 : 60, 6, 8);
            String s = "" + all.indexOf(part);
            graphics.drawString(minecraft.font, s, (int)((float)x1 + 9.5F - (float) minecraft.font.width(s) / 2.0F), (int)((float)guiY + 0.5F), 0);
            RenderSystem.setShaderTexture(0, GuiCreationNewParts.buttonsResource);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            x1 = getX() + SIZE - 5;
            x2x = x1 + 6;
            y2 = guiY + 8;
            hoverR = xMouse >= x1 && yMouse >= guiY && xMouse < x2x && yMouse < y2;
            graphics.blit(GuiCreationNewParts.buttonsResource, x1, guiY, 6, hoverR ? 76 : 60, 6, 8);
            guiY += 11;
         }
         if (!basic) {
            if (selected) {
               x1 = getX() + SIZE - 15;
               x2x = x1 + 14;
               y2 = guiY + 14;
               settingsHovered = xMouse >= x1 && yMouse >= guiY && xMouse < x2x && yMouse < y2;
               graphics.blit(GuiCreationNewParts.buttonsResource, x1, guiY, 0, settingsHovered ? 140 : 126, 14, 14);
            }

            size = 8;
            x1 = getX() + SIZE - 10;
            x2x = x1 + size;
            y1 = getY() + SIZE - 12;
            y2 = y1 + size;
            infoHovered = xMouse >= x1 && yMouse >= y1 && xMouse < x2x && yMouse < y2;
            MutableComponent text = Component.literal("i").withStyle(ChatFormatting.BOLD);
            if (infoHovered) { text = text.withStyle(ChatFormatting.UNDERLINE); }
            graphics.drawString(minecraft.font, text, x1 + 3, y1 + 2, 0);
         }
      }

      public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
         if (!super.clicked(mouseX, mouseY)) { return false; }
         if (colorPickerHovered) {
            openSubgui(listener, new GuiModelColor(listener, data.getColor(), (color) -> {
               data.setColor(color);
               GuiCreationNewParts.this.save();
            }));
         }
         else {
            if (hoverL) {
               int index = (all.indexOf(part) + all.size() - 1) % all.size();
               part = all.get(index);
               data.partId = part.id;
            }
            else if (hoverR) {
               int index = (all.indexOf(part) + 1) % all.size();
               part = all.get(index);
               data.partId = part.id;
            }
            else if (settingsHovered) {
               if (data instanceof ModelEyeData) { openEyesSubgui(listener, (ModelEyeData)data, (MpmPartEyes)part); }
               else { openTextureSubgui(listener, data, part); }
            }
            else if (part.isEnabled && !basic) {
               selected = !selected;
               if (selected) { GuiCreationNewParts.this.data.mpmParts.add(data); }
               else { GuiCreationNewParts.this.data.mpmParts.removeIf((t) -> t.partId.equals(data.partId)); }
            }
         }
         GuiCreationNewParts.this.data.refreshParts();
         GuiCreationNewParts.this.save();
         return true;
      }

      @Override
      public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         if (listener.subgui == null) { renderModel(graphics); }
      }

      @SuppressWarnings("unused")
      public void onRenderPost(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         if (listener.subgui == null) {
            renderIcons(graphics, mouseX, mouseY);
            if (infoHovered) {
               List<Component> hover = Arrays.asList(Component.translatable(part.name), Component.translatable("message.madeby", part.author));
               if (!part.isEnabled) {
                  hover = new ArrayList<>();
                  hover.add(Component.translatable("gui.disabled", part.author));
               }
               listener.setHoverText(hover);
            }
         }
      }

      @Override
      public void init() { }

      public ICustomGuiComponent component() { return null; }

   }

   public static class PartsWrapper implements ICustomGuiComponent {

      private final GuiCreationNewParts.GuiMpmPart part;

      public PartsWrapper(GuiCreationNewParts.GuiMpmPart partIn) { part = partIn; }

      @Override
      public ICustomGuiComponent setId(int id) { return this; }

      @Override
      public int getId() { return 0; }

      @Override
      public UUID getUniqueID() { return null; }

      @Override
      public int getPosX() { return part.getX(); }

      @Override
      public int getPosY() { return part.getY(); }

      @Override
      public ICustomGuiComponent setPos(int x, int y) { return this; }

      @Override
      public int getWidth() { return part.getWidth(); }

      @Override
      public int getHeight() { return part.getHeight(); }

      @Override
      public ICustomGuiComponent setSize(int width, int height) { return null; }

      @Override
      public boolean hasHoverText() { return false; }

      @Override
      public String[] getHoverText() { return new String[0]; }

      @Override
      public ICustomGuiComponent setHoverText(String text) { return null; }

      @Override
      public ICustomGuiComponent setHoverText(String[] text) { return null; }

      @Override
      public ICustomGuiComponent setEnabled(boolean bo) { return this; }

      @Override
      public boolean getVisible() {
         return true;
      }

      @Override
      public ICustomGuiComponent setVisible(boolean bo){ return null; }

      @Override
      public boolean getEnabled() { return true; }

      @Override
      public int getType() { return -1; }

      @Override
      public int getOffsetType() { return 0; }

      @Override
      public void offSet(int offsetType, double[] windowSize) { }

   }

   class TexturePart extends GuiCustom {

      protected final MpmPart part;
      protected final MpmPartData data;
      private final GuiCreationNewParts.GuiMpmPart partGui;

      public TexturePart(MpmPartData dataIn, MpmPart partIn) {
         super(listener.getMenu(), listener.inv, Component.empty());
         data = dataIn;
         part = partIn;
         partGui = new GuiMpmPart(this, 70, 2, 2, partIn);
         partGui.zPos = 250;
         partGui.basic = true;
         guiWrapper = new CustomGuiWrapper(null);
         guiWrapper.addComponent(new PartsWrapper(partGui));
      }

      public void init() {
         super.init();
         add(partGui);
      }

      public void onClose() {
         super.onClose();
         GuiCreationNewParts.this.save();
      }

   }

   class EyesPart extends GuiCustom {

      protected final MpmPartEyes part;
      protected final ModelEyeData data;

      public EyesPart(ModelEyeData dataIn, MpmPartEyes partIn) {
         super(listener.getMenu(), listener.inv, Component.empty());
         data = dataIn;
         part = partIn;
      }

      @Override
      public void init() {
         super.init();
         components.components.put(23, new GuiColorButton(this, (CustomGuiButtonWrapper) guiWrapper.getComponent(23), ColorUtil.rgbToColor(data.color)));
         components.components.put(34, new GuiColorButton(this, (CustomGuiButtonWrapper) guiWrapper.getComponent(34), ColorUtil.rgbToColor(data.browColor)));
         components.components.put(40, new GuiColorButton(this, (CustomGuiButtonWrapper) guiWrapper.getComponent(40), ColorUtil.rgbToColor(data.lidColor)));
      }

      @Override
      public void renderBackground(@Nonnull GuiGraphics graphics) {
         super.renderBackground(graphics);
      }

      @Override
      @SuppressWarnings("deprecation")
      public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         if (minecraft == null) { minecraft = Minecraft.getInstance(); }
         super.render(graphics, mouseX, mouseY, partialTicks);

         PoseStack posestack = RenderSystem.getModelViewStack();
         posestack.pushPose();
         posestack.translate(leftPos + 10, topPos + 10, 150.0D);
         posestack.scale(1.0F, 1.0F, -1.0F);
         RenderSystem.applyModelViewMatrix();
         PoseStack matrixStack = new PoseStack();
         matrixStack.pushPose();
         EntityRenderDispatcher entityRendererManager = minecraft.getEntityRenderDispatcher();
         entityRendererManager.setRenderShadow(false);
         BufferSource iRenderTypeBuffer = minecraft.renderBuffers().bufferSource();
         VertexConsumer iVertex = iRenderTypeBuffer.getBuffer(RenderType.entityCutoutNoCull(npc.textureLocation));
         Lighting.setupForEntityInInventory();
         RenderSystem.runAsFancy(() -> {
            GuiCreationNewParts.biped.body.visible = !part.hiddenParts.contains(BodyPart.BODY);
            GuiCreationNewParts.biped.jacket.visible = GuiCreationNewParts.biped.jacket.visible && GuiCreationNewParts.biped.body.visible;
            GuiCreationNewParts.biped.head.visible = !part.hiddenParts.contains(BodyPart.HEAD);
            GuiCreationNewParts.biped.hat.visible = GuiCreationNewParts.biped.hat.visible && GuiCreationNewParts.biped.head.visible;
            matrixStack.translate(19.0F, 43.0F, 25.0F);
            matrixStack.scale(100.0F, 100.0F, 100.0F);
            GuiCreationNewParts.biped.head.render(matrixStack, iVertex, 15728880, OverlayTexture.NO_OVERLAY);
            part.pos = NopVector3f.ZERO;
            part.rot = NopVector3f.ZERO;
            LayerParts.renderPart(data, part, matrixStack, iRenderTypeBuffer, 15728880, npc, biped, renderData);
         });
         iRenderTypeBuffer.endBatch();
         matrixStack.popPose();
         posestack.popPose();
         entityRendererManager.setRenderShadow(true);
         RenderSystem.applyModelViewMatrix();
      }

      @Override
      public void onClose() {
         super.onClose();
         GuiCreationNewParts.this.save();
      }

   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (scroll.getSelectedIndex() >= 0) {
         GuiCreationNewParts.active = Util.instance.deleteColor(scroll.getSelected());
         for (GuiMpmPart part : GuiCreationNewParts.this.guiParts) { listener.scrollingPanel.comps.removeComponent(part.getId()); }
         GuiCreationNewParts.this.guiParts.clear();
         listener.init();
      }
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

}
