package noppes.npcs.shared.client.gui;

import java.util.*;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.api.event.ClientEvent;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.gui.util.GuiTooltipUtils;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

@OnlyIn(Dist.CLIENT)
public class GuiBasicContainer<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements IGuiInterface {

   public boolean drawDefaultBackground = true;
   public int guiLeft;
   public int guiTop;
   public LocalPlayer player;
   public GuiWrapper wrapper = new GuiWrapper(this);
   public Component title = Component.empty();
   public boolean closeOnEsc = true;
   public boolean hoverIsGame = false;

   // New fields from Unofficial (BetaZavr)
   public float bgScale = 1.0F;
   public ResourceLocation background = null;
   protected final List<Component> hoverText = new ArrayList<>();
   protected ClientProxy.FontContainer hoverFont = null;
   public int widthTexture = 0;
   public int heightTexture = 0;
   public int borderTexture = 4;

   public GuiBasicContainer(T cont, Inventory inv, Component titleIn) {
      super(cont, inv, titleIn);
      player = Minecraft.getInstance().player;
      minecraft = Minecraft.getInstance();
      font = minecraft.font;
   }

   @Override
   public boolean shouldCloseOnEsc() { return closeOnEsc; }

   @Override
   public void init() {
      super.init();
      setFocused(null);
      guiLeft = (width - imageWidth) / 2;
      guiTop = (height - imageHeight) / 2;
      renderables.clear();
      children().clear();
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      wrapper.init(minecraft, width, height);
   }

   public static ResourceLocation getResource(String texture) {
      return new ResourceLocation(CustomNpcs.MODID, "textures/gui/" + texture);
   }

   @Override
   public void containerTick() { wrapper.tick(); }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
      return wrapper.mouseScrolled(mouseX, mouseY, scrolled) || super.mouseScrolled(mouseX, mouseY, scrolled);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      boolean bo  = wrapper.mouseClicked(mouseX, mouseY, mouseButton) || super.mouseClicked(mouseX, mouseY, mouseButton);
      if (GuiTextFieldNop.getActive() != null) {
         for (IComponentGui component : wrapper.components) {
            if (component instanceof GuiTextArea area) {
               area.active = false;
               area.setIsFocused(false);
            }
         }
      }
      return bo;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
      if (wrapper.mouseDragged(mouseX, mouseY, mouseButton, dx, dy)) {
         return true;
      } else if (getFocused() != null && isDragging() && mouseButton == 0) {
         getFocused().mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
      return wrapper.mouseReleased(mouseX, mouseY, mouseButton) || super.mouseReleased(mouseX, mouseY, mouseButton);
   }

   @Nullable
   public Slot findSlot(double mouseX, double mouseY, Slot foundSlot) { return foundSlot; }

   @Override
   public void subGuiClosed(Screen subgui) { }

   @Override
   public GuiWrapper getWrapper() { return wrapper; }

   @Override
   public boolean charTyped(char c, int i) { return wrapper.charTyped(c, i) || super.charTyped(c, i); }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      GuiBasic.checkAltH();
      return wrapper.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public void setFocused(@Nullable GuiEventListener gui) {
      if (wrapper.subgui != null) {
         wrapper.subgui.setFocused(gui);
      } else {
         if (gui != null && !children().contains(gui)) {
            return;
         }
         wrapper.changeFocus(getFocused(), gui);
         super.setFocused(gui);
      }
   }

   @Override
   public GuiEventListener getFocused() {
      return wrapper.subgui != null ? wrapper.subgui.getFocused() : super.getFocused();
   }

   @Override
   public void onClose() {
      wrapper.close();
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      minecraft.mouseHandler.grabMouse();
   }

   @Override
   public void add(IComponentGui element) {
      wrapper.add(element);
      if (element instanceof GuiTextArea area) { area.setListener(this); }
   }

   public IComponentGui get(int id, Class<?> type) {
      for (IComponentGui component : new ArrayList<>(wrapper.components)) {
         if (type.isAssignableFrom(component.getClass()) && component.getId() == id) { return component; }
      }
      return null;
   }

   @Override
   public IComponentGui get(int id) {
      for (IComponentGui component : new ArrayList<>(wrapper.components)) {
         if (component.getId() == id) { return component; }
      }
      return null;
   }

   @Override
   public GuiLabel addLabel(int id, int x, int y, Object label) {
      GuiLabel element = new GuiLabel(this, id, label, x, y);
      wrapper.add(element);
      return element;
   }

   @Override
   public GuiButtonNop addButton(int id, int x, int y, Object label) {
      GuiButtonNop element = new GuiButtonNop(this, id, label, x, y, null);
      wrapper.add(element);
      return element;
   }

   @Override
   public GuiButtonNop addButton(int id, int x, int y, boolean isBiDirectional, int variant, Object... variants) {
      GuiButtonNop element;
      if (isBiDirectional) { element = new GuiButtonBiDirectional(this, id, x, y, variant, variants); }
      else { element = new GuiButtonNop(this, id, x, y, variant, variants); }
      wrapper.add(element);
      return element;
   }

   @Override
   public GuiCheckBoxNop addCheckBox(int id, int x, int y, Object labelTrue, Object labelFalse, boolean selected) {
      GuiCheckBoxNop element = new GuiCheckBoxNop(this, id, x, y, labelTrue, labelFalse, selected);
      wrapper.add(element);
      return element;
   }

   @Override
   public GuiMenuTopButton addTopButton(int id, int x, int y, Object label) {
      GuiMenuTopButton element = new GuiMenuTopButton(this, id, label, x, y);
      wrapper.add(element);
      return element;
   }

   @Override
   public GuiMenuTopIconButton addTopButton(int id, int x, int y, Object label, ItemStack stack) {
      GuiMenuTopIconButton element = new GuiMenuTopIconButton(this, id, label, x, y, stack);
      wrapper.add(element);
      return element;
   }

   @Override
   public GuiMenuSideButton addSideButton(int id, int x, int y, Object label) {
      GuiMenuSideButton element = new GuiMenuSideButton(this, id, label, x, y);
      wrapper.add(element);
      return element;
   }

   @Override
   public GuiButtonYesNo addYesNo(int id, int x, int y, boolean isYes) {
      GuiButtonYesNo element = new GuiButtonYesNo(this, id, x, y, isYes);
      wrapper.add(element);
      return element;
   }

   @Override
   public GuiSliderNop addSlider(int id, int x, int y, float sliderValue) {
      GuiSliderNop element = new GuiSliderNop(this, id, x, y, sliderValue);
      wrapper.add(element);
      return element;
   }

   @Override
   public GuiTextFieldNop addTextField(int id, int x, int y, int width, int height, Object value) {
      GuiTextFieldNop element = new GuiTextFieldNop(this, id, x, y, width, height, value);
      wrapper.add(element);
      return element;
   }

   @Override
   public GuiCustomScrollNop addScroll(int id) {
      GuiCustomScrollNop element = new GuiCustomScrollNop(this, id);
      wrapper.add(element);
      return element;
   }

   @Override
   public GuiCustomScrollNop addScroll(int id, boolean isMultipleSelection) {
      GuiCustomScrollNop element = new GuiCustomScrollNop(this, id, isMultipleSelection);
      wrapper.add(element);
      return element;
   }

   @Override
   public void extraEvent(Object extra) { }

   @Override
   public int getX() { return guiLeft; }

   @Override
   public int getY() { return guiTop; }

   public GuiButtonNop getButton(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.BUTTON &&
                 element.getId() == id &&
                 element instanceof GuiButtonNop) { return (GuiButtonNop) element; }
      }
      return null;
   }

   @SuppressWarnings("unused")
   public GuiMenuSideButton getSideButton(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.SIDE_BUTTON &&
                 element.getId() == id &&
                 element instanceof GuiMenuSideButton) { return (GuiMenuSideButton) element; }
      }
      return null;
   }

   @SuppressWarnings("unused")
   public GuiMenuTopButton getTopButton(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.TOP_BUTTON &&
                 element.getId() == id &&
                 element instanceof GuiMenuTopButton) { return (GuiMenuTopButton) element; }
      }
      return null;
   }

   public GuiTextFieldNop getTextField(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.TEXT_FIELD &&
                 element.getId() == id &&
                 element instanceof GuiTextFieldNop) { return (GuiTextFieldNop) element; }
      }
      return null;
   }

   public GuiLabel getLabel(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.LABEL &&
                 element.getId() == id &&
                 element instanceof GuiLabel) { return (GuiLabel) element; }
      }
      return null;
   }

   @SuppressWarnings("unused")
   public GuiSliderNop getSlider(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.SLIDER &&
                 element.getId() == id &&
                 element instanceof GuiSliderNop) { return (GuiSliderNop) element; }
      }
      return null;
   }

   @SuppressWarnings("unused")
   public GuiCustomScrollNop getScroll(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.SCROLL &&
                 element.getId() == id &&
                 element instanceof GuiCustomScrollNop) { return (GuiCustomScrollNop) element; }
      }
      return null;
   }

   @SuppressWarnings("unused")
   public IComponentGui getExtra(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.EXTRA && element.getId() == id) { return element; }
      }
      return null;
   }

   @Override
   protected void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      if (background != null) {
         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         matrixStack.translate((float)guiLeft, (float)guiTop, 0.0F);
         matrixStack.scale(bgScale, bgScale, bgScale);
         if (widthTexture != 0 && heightTexture != 0) {
            int maxRow = ValueUtil.correctInt((int) Math.ceil((float) imageHeight / (float) (heightTexture - 2 * borderTexture)), 2, 10);
            int maxCol = ValueUtil.correctInt((int) Math.ceil((float) imageWidth / (float) (widthTexture - 2 * borderTexture)), 2, 10);
            int tileWidth = imageWidth / maxCol;
            int tileHeight = imageHeight / maxRow;
            int lastTileWidth = imageWidth - tileWidth * (maxCol - 1);
            int lastTileHeight = imageHeight - tileHeight * (maxRow - 1);

            int uOffset = (widthTexture - 2 * borderTexture - tileWidth) / 2;
            int uMax = widthTexture - lastTileWidth;
            int vOffset = (heightTexture - 2 * borderTexture - tileHeight) / 2;
            int vMax = heightTexture - lastTileHeight;

            for (int col = 0; col < maxCol; ++col) {
               for (int row = 0; row < maxRow; ++row) {
                  graphics.blit(background, col * tileWidth,
                          row * tileHeight,
                          col == 0 ? 0 : col == maxCol - 1 ? uMax : uOffset,
                          row == 0 ? 0 : row == maxRow - 1 ? vMax : vOffset,
                          col == maxCol - 1 ? lastTileWidth : tileWidth,
                          row == maxRow - 1 ? lastTileHeight : tileHeight);
               }
            }
         }
         else if (imageWidth > 256) {
            graphics.blit(background, 0, 0, 0, 0, 250, imageHeight);
            graphics.blit(background, 250, 0, 256 - (imageWidth - 250), 0, imageWidth - 250, imageHeight);
         }
         else {
            graphics.blit(background, 0, 0, 0, 0, imageWidth, imageHeight);
         }
         matrixStack.popPose();
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop button) { }

   @Override
   public boolean mouseButtonEvent(GuiButtonNop button, int mouseButton) { return false; }

   @Override
   public void save() { }

   @Override
   protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {}

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      wrapper.graphics = graphics;
      wrapper.mouseX = mouseX;
      wrapper.mouseY = mouseY;
      ArrayList<Slot> slots = new ArrayList<>(menu.slots);
      int x = mouseX;
      int y = mouseY;
      if (hasSubGui()) {
         menu.slots.clear();
         x = 0;
         y = 0;
      }
      super.render(graphics, x, y, partialTicks);
      if (title != null && !title.getString().isEmpty()) {
         GuiButtonNop.renderString(graphics, title, guiLeft + 4, guiTop + 5, guiLeft + imageWidth - 8, guiTop + 15,
                 CustomNpcs.LableColor.getRGB(), false, true, null);
      }
      for (IComponentGui component : new ArrayList<>(wrapper.components)) {
         if (component instanceof Renderable renderable) { renderable.render(graphics, x, y, partialTicks); }
      }
      if (hasSubGui()) {
         menu.slots.addAll(slots);
         graphics.pose().pushPose();
         graphics.pose().translate(0.0F, 0.0F, 100.0F);
         wrapper.subgui.render(graphics, mouseX, mouseY, partialTicks);
         graphics.pose().popPose();
      }
      else { renderTooltip(graphics, mouseX, mouseY); }
   }

   @Override
   public void renderTooltip(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
      if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.hasItem()) {
         ItemStack itemstack = hoveredSlot.getItem();
         GuiTooltipUtils.renderTooltip(graphics, font, getTooltipFromContainerItem(itemstack), itemstack.getTooltipImage(), itemstack, mouseX, mouseY);
      }
      else if ((hoverIsGame || (CustomNpcs.ShowDescriptions && GuiBasic.showHoverText)) && !hoverText.isEmpty()) {
         if (!hoverIsGame) { hoverText.add(Component.translatable("hover.alt.h")); }
         if (hoverFont == null) { GuiTooltipUtils.renderTooltip(graphics, font, hoverText, Optional.empty(), mouseX, ValueUtil.correctInt(mouseY, 16, height)); }
         else { GuiBasic.renderTooltipInternal(graphics, mouseX, ValueUtil.correctInt(mouseY, 16, height), hoverFont, hoverText, bgScale); }
         hoverText.clear();
      }
   }

   @Override
   public void renderBackground(@Nonnull GuiGraphics graphics) {
      if (drawDefaultBackground) { super.renderBackground(graphics); }
      if (wrapper.subgui == null) { postDrawBackground(); }
   }

   public void postDrawBackground() { }

   public void setScreen(Screen gui) {
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      if (gui == null) {
         minecraft.setScreen(null);
         return;
      }
      ClientEvent.NextToGuiCustomNpcs event = new ClientEvent.NextToGuiCustomNpcs(NoppesUtilServer.getEditingNpc(player), this, gui);
      MinecraftForge.EVENT_BUS.post(event);
      if (event.returnGui == null || event.isCanceled()) { return; }
      minecraft.setScreen(event.returnGui);
   }

   public void setSubGui(Screen gui) {
      ClientEvent.SubGuiCustomNpcs event = new ClientEvent.SubGuiCustomNpcs(NoppesUtilServer.getEditingNpc(player), gui, wrapper.subgui);
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) { return; }

      if (event.returnGui != null) { LogWriter.debug("Open SubGUI - " + event.returnGui.getClass() + "; In " + getClass().getSimpleName()); }
      else if (wrapper.subgui != null) {
         LogWriter.debug("Close SubGUI - " + wrapper.subgui.getClass() + "; In " + getClass().getSimpleName());
         subGuiClosed(wrapper.subgui);
      }

      wrapper.setSubgui(event.returnGui);
      init();
   }

   @Override
   public boolean hasSubGui() { return wrapper.subgui != null; }

   @Override
   public Screen getSubGui() { return wrapper.getSubGui(); }

   @Override
   public int getWidth() { return width; }

   @Override
   public int getHeight() { return height; }

   @Override
   public boolean doubleClicked(IComponentGui component) { return false; }

   @Override
   public Screen getParent() { return wrapper.getParent(); }

   // New from Unofficial (BetaZavr)
   public void setBackground(String texture) {
      background = new ResourceLocation(CustomNpcs.MODID, "textures/gui/" + texture);
      switch (texture) {
         case "bgfilled.png": {
            widthTexture = 256;
            heightTexture = 256;
            break;
         }
         case "companion_empty.png": {
            widthTexture = 172;
            heightTexture = 167;
            break;
         }
         case "extrasmallbg.png": {
            widthTexture = 176;
            heightTexture = 71;
            break;
         }
         case "largebg.png": {
            widthTexture = 192;
            heightTexture = 231;
            break;
         }
         case "menubg.png": {
            widthTexture = 256;
            heightTexture = 217;
            break;
         }
         case "smallbg.png": {
            widthTexture = 176;
            heightTexture = 222;
            break;
         }
         case "standardbg.png": {
            widthTexture = 256;
            heightTexture = 195;
            break;
         }
      }
   }

   @Override
   public boolean isMouseHover(double mX, double mY, double px, double py, double pwidth, double pheight) {
      return mX >= px && mY >= py && mX < (px + pwidth) && mY < (py + pheight);
   }

   @Override
   public List<Component> getHoverText() { return hoverText; }

   @Override
   public void setHoverText(@Nullable List<Component> components) {
      hoverText.clear();
      if (components != null && !components.isEmpty()) { Util.instance.putHovers(hoverText, components); }
   }

   @Override
   public void setHoverText(Object... components) {
      hoverText.clear();
      if (components != null) {
         List<Component> lines = new ArrayList<>();
         Util.instance.putHovers(lines, components);
         hoverText.addAll(lines);
      }
   }

   @Override
   public void drawHoverText(String text, Object... args) {
      if (!CustomNpcs.ShowDescriptions) { return; }
      if (text == null) {
         if (!hoverText.isEmpty()) {
            GuiTooltipUtils.renderTooltip(wrapper.graphics, font, hoverText, Optional.empty(), wrapper.mouseX, wrapper.mouseY);
         }
         hoverText.clear();
         return;
      }
      setHoverText(text, args);
      if (!hoverText.isEmpty()) {
         GuiTooltipUtils.renderTooltip(wrapper.graphics, font, hoverText, Optional.empty(), wrapper.mouseX, wrapper.mouseY);
         hoverText.clear();
      }
   }

   @Override
   public void drawWait(GuiGraphics graphics) {
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      if (minecraft.level != null) {
         int x = minecraft.getWindow().getGuiScaledWidth() / 2;
         int y = minecraft.getWindow().getGuiScaledHeight() / 2 - 30;
         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         matrixStack.translate(0.0f, 0.0f, 150.0f);
         graphics.drawCenteredString(minecraft.font, Component.translatable("gui.wait", ""), width / 2, height / 2, CustomNpcs.MainColor.getRGB());
         int pos_0 = (int) Math.floor((double) (minecraft.level.getGameTime() % 16) / 2.0d);
         graphics.blit(GuiBasic.INFO, x + GuiBasic.getPosX.apply(pos_0) - 1, y + GuiBasic.getPosY.apply(pos_0) - 1, 0, 12, 6, 6);
         int pos_1 = pos_0 - 1;
         if (pos_1 < 0) { pos_1 += 8; }
         graphics.blit(GuiBasic.INFO, x + GuiBasic.getPosX.apply(pos_1), y + GuiBasic.getPosY.apply(pos_1), 6, 12, 5, 5);
         int pos_2 = pos_0 - 2;
         if (pos_2 < 0) { pos_2 += 8; }
         graphics.blit(GuiBasic.INFO, x + GuiBasic.getPosX.apply(pos_2) + 1, y + GuiBasic.getPosY.apply(pos_2) + 1, 11, 12, 4, 4);
         matrixStack.popPose();
      }
   }

   @Nullable
   protected Slot findSlot(double mouseX, double mouseY) {
      for(int i = 0; i < menu.slots.size(); ++i) {
         Slot slot = menu.slots.get(i);
         if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.isActive()) { return slot; }
      }
      return null;
   }

}
