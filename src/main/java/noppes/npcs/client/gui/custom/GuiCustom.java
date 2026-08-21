package noppes.npcs.client.gui.custom;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.wrapper.gui.CustomGuiTexturedRectWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiWrapper;
import noppes.npcs.packets.server.SPacketCustomGuiKeyPressed;
import noppes.npcs.shared.client.gui.GuiBasicContainer;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiTexturedRect;
import noppes.npcs.client.gui.util.GuiTooltipUtils;
import noppes.npcs.containers.ContainerCustomGui;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiSubGuiClosed;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class GuiCustom extends GuiBasicContainer<ContainerCustomGui> implements IGuiData {

   protected GuiCustomComponents components = new GuiCustomComponents();
   public GuiCustomScrollingPanel scrollingPanel = new GuiCustomScrollingPanel();
   public CustomGuiTexturedRect background;
   public CustomGuiWrapper guiWrapper;
   public GuiCustom subgui = null;
   public GuiCustom parent = null;
   public Inventory inv;
   public GuiCustom.InitCallback initCallback;

   public GuiCustom(ContainerCustomGui container, Inventory invIn, Component titleIn) {
      super(container, invIn, titleIn);
      inv = invIn;
   }

   @Override
   public void init() {
      super.init();
      if (guiWrapper != null) {
         scrollingPanel.setComponents(this, guiWrapper.getScrollingPanel());
         components.setComponents(this, guiWrapper);
      }
      if (initCallback != null) { initCallback.init(); }
      if (subgui != null) { subgui.init(); }
   }

   @Override
   public void containerTick() {
      if (subgui != null) { subgui.containerTick(); }
      else {
         components.containerTick();
         scrollingPanel.containerTick();
      }
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      hoverText.clear();
      PoseStack matrixStack = graphics.pose();
      renderBackground(graphics);
      PoseStack posestack = RenderSystem.getModelViewStack();
      posestack.pushPose();
      posestack.translate((float) getGuiLeft(), (float) getGuiTop(), 0.0F);
      RenderSystem.applyModelViewMatrix();
      matrixStack.pushPose();
      if (background != null) { background.render(graphics, mouseX, mouseY, partialTicks); }
      components.render(graphics, mouseX - getGuiLeft(), mouseY - getGuiTop(), partialTicks);
      scrollingPanel.render(graphics, mouseX - getGuiLeft(), mouseY - getGuiTop(), partialTicks);
      if (!hoverText.isEmpty() && subgui == null) {
         GuiTooltipUtils.renderTooltip(graphics, font, hoverText, Optional.empty(), mouseX - getGuiLeft(), mouseY - getGuiTop());
      }
      posestack.popPose();
      RenderSystem.applyModelViewMatrix();
      super.render(graphics, mouseX, mouseY, partialTicks);
      if (subgui == null) { renderTooltip(graphics, mouseX, mouseY); }
      matrixStack.popPose();
      if (subgui != null) {
         matrixStack.pushPose();
         posestack.pushPose();
         posestack.translate(0.0F, 0.0F, 40.0F);
         RenderSystem.applyModelViewMatrix();
         matrixStack.translate(0.0F, 0.0F, 40.0F);
         subgui.render(graphics, mouseX, mouseY, partialTicks);
         matrixStack.popPose();
         posestack.popPose();
         RenderSystem.applyModelViewMatrix();
      }
   }

   @Override
   protected void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int x, int y) {}

    @Override
   public boolean charTyped(char typedChar, int keyCode) {
      if (subgui != null) { return subgui.charTyped(typedChar, keyCode); }
      if (components.charTyped(typedChar, keyCode)) { return true; }
      return scrollingPanel.charTyped(typedChar, keyCode) || super.charTyped(typedChar, keyCode);
   }

   @Override
   public boolean keyPressed(int key, int key_1, int key_2) {
      if (subgui != null) { return subgui.keyPressed(key, key_1, key_2); }
      Packets.sendServer(new SPacketCustomGuiKeyPressed(key));
      if (components.keyPressed(key, key_1, key_2)) { return true; }
      if (scrollingPanel.keyPressed(key, key_1, key_2)) { return true; }
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      return minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(key, key_1)) || super.keyPressed(key, key_1, key_2);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (subgui != null) { return subgui.mouseClicked(mouseX, mouseY, mouseButton); }
      boolean clicked = false;
      clicked |= components.mouseClicked(mouseX - (double)getGuiLeft(), mouseY - (double)getGuiTop(), mouseButton);
      clicked |= scrollingPanel.mouseClicked(mouseX - (double)getGuiLeft(), mouseY - (double)getGuiTop(), mouseButton);
      return clicked | super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double mouseScrolled) {
      if (subgui != null) { return subgui.mouseScrolled(mouseX, mouseY, mouseScrolled); }
      if (super.mouseScrolled(mouseX, mouseY, mouseScrolled)) { return true; }
      return scrollingPanel.mouseScrolled(mouseX - (double)getGuiLeft(), mouseY - (double)getGuiTop(), mouseScrolled);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
      if (subgui != null) { return subgui.mouseDragged(mouseX, mouseY, mouseButton, dx, dy); }
      boolean clicked = false;
      clicked |= components.mouseDragged(mouseX - (double)getGuiLeft(), mouseY - (double)getGuiTop(), mouseButton, dx, dy);
      clicked |= scrollingPanel.mouseDragged(mouseX - (double)getGuiLeft(), mouseY - (double)getGuiTop(), mouseButton, dx, dy);
      return clicked | super.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
      if (subgui != null) { return subgui.mouseReleased(mouseX, mouseY, mouseButton); }
      boolean clicked = false;
      clicked |= components.mouseReleased(mouseX - (double)getGuiLeft(), mouseY - (double)getGuiTop(), mouseButton);
      clicked |= scrollingPanel.mouseReleased(mouseX - (double)getGuiLeft(), mouseY - (double)getGuiTop(), mouseButton);
      return clicked | super.mouseReleased(mouseX, mouseY, mouseButton);
   }

   @Override
   public boolean isPauseScreen() { return guiWrapper == null || guiWrapper.getDoesPauseGame(); }

   @Override
   public boolean shouldCloseOnEsc() { return guiWrapper == null || guiWrapper.getClosesOnEsc(); }

   @Override
   public void onClose() {
      if (subgui == null) {
         if (parent == null) { super.onClose(); }
         else {
            Packets.sendServer(new SPacketCustomGuiSubGuiClosed());
            parent.subgui = null;
         }
      }
      else { subgui.onClose(); }
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      setGuiWrapper((CustomGuiWrapper)(new CustomGuiWrapper((IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(Minecraft.getInstance().player))).of(compound));
      init();
   }

   @Override
   public void resize(@Nonnull Minecraft minecraft, int width, int height) {
      super.resize(minecraft, width, height);
      if (subgui != null) { subgui.resize(minecraft, width, height); }
   }

   public void setGuiWrapper(CustomGuiWrapper guiWrapperIn) {
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      guiWrapper = guiWrapperIn;
      imageWidth = guiWrapperIn.getWidth();
      imageHeight = guiWrapperIn.getHeight();
      background = new CustomGuiTexturedRect(this, (CustomGuiTexturedRectWrapper) guiWrapperIn.getBackgroundRect());
      if (guiWrapperIn.getSubGuiWrapper() != null) {
         if (subgui == null &&minecraft.player != null) {
            subgui = new GuiCustom(menu, minecraft.player.getInventory(), Component.empty());
            subgui.init(minecraft, width, height);
         }
         if (subgui != null) {
            subgui.parent = this;
            subgui.setGuiWrapper(guiWrapperIn.getSubGuiWrapper());
         }
      } else {
         menu.setGui(guiWrapperIn, Minecraft.getInstance().player);
         subgui = null;
         if (parent == null) {
            init();
         }
      }
   }

   @SuppressWarnings("unused")
   public int getTotalGuiLeft() { return parent != null ? parent.getTotalGuiLeft() + getGuiLeft() : getGuiLeft(); }

   @SuppressWarnings("unused")
   public int getTotalGuiTop() { return parent != null ? parent.getTotalGuiTop() + getGuiTop() : getGuiTop(); }

   public void addPanel(IComponentGui component) { scrollingPanel.components.put(component.getId(), component); }

   public IComponentGui getComponent(UUID id) {
      Optional<IComponentGui> c = components.components.values()
              .stream()
              .filter((t) -> t instanceof IComponentCustomGui iCCG &&
                      iCCG.component() != null &&
                      iCCG.component().getUniqueID().equals(id))
              .findFirst();
      if (c.isPresent()) { return c.get(); }
      c = scrollingPanel.components.values().stream().filter((t) -> t instanceof IComponentCustomGui iCCG &&
              iCCG.component() != null &&
              iCCG.component().getUniqueID().equals(id)).findFirst();
      return c.orElseGet(() -> subgui != null ? subgui.getComponent(id) : null);
   }

   public interface InitCallback {  void init(); }

}
