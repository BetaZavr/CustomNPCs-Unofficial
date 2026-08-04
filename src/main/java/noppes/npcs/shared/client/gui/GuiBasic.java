package noppes.npcs.shared.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import java.net.URI;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.api.event.ClientEvent;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.controllers.YDEController;
import noppes.npcs.client.gui.util.GuiTooltipUtils;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.joml.Vector2ic;

@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public abstract class GuiBasic extends Screen implements IGuiInterface {

   protected static long altHTime = System.currentTimeMillis();
   public static Function<Integer, Integer> getPosX = (pos) -> switch (pos) {
      case 1, 3 -> -11;
      case 2 -> -15;
      case 5, 7 -> 11;
      case 6 -> 15;
      default -> 0;
   };
   public static Function<Integer, Integer> getPosY = (pos) -> switch (pos) {
      case 0 -> -15;
      case 1, 7 -> -11;
      case 3, 5 -> 11;
      case 4 -> 15;
      default -> 0;
   };

   public static boolean showHoverText = true;

   public LocalPlayer player;
   public boolean drawDefaultBackground = true;
   public ResourceLocation background = null;
   public Component title = Component.empty();
   public boolean closeOnEsc = true;
   public boolean hoverIsGame = false;
   public int guiLeft;
   public int guiTop;
   public int imageWidth = 200;
   public int imageHeight = 222;
   public float bgScale = 1.0F;
   public GuiWrapper wrapper = new GuiWrapper(this);

   // Mod Resources
   public static final DecimalFormat df = new DecimalFormat("#.#");
   public static final DecimalFormat df2 = new DecimalFormat("#.##");
   public static final DecimalFormat df3 = new DecimalFormat("#.###");
   public static final DecimalFormat df4 = new DecimalFormat("#.####");
   public static final ResourceLocation MONEY = new ResourceLocation(CustomNpcs.MODID, "textures/item/coin_gold.png");
   public static final ResourceLocation DONAT = new ResourceLocation(CustomNpcs.MODID, "textures/item/coin_donat.png");
   public static final ResourceLocation INFO = getResource("info.png");
   public static final ResourceLocation RESOURCE_SLOT = getResource("slot.png");
   public static final ResourceLocation MENU_BUTTON = getResource("menubutton.png");
   public static final ResourceLocation MENU_SIDE_BUTTON = getResource("menusidebutton.png");
   public static final ResourceLocation MENU_TOP_BUTTON = getResource("menutopbutton.png");
   public static final ResourceLocation ANIMATION_BUTTONS = getResource("animation/buttons.png");
   public static final ResourceLocation ANIMATION_BUTTONS_SLOTS = getResource("animation/button_slots.png");
   public static final ResourceLocation YDE_BUTTONS = getResource("animation/yde_buttons.png");
   public static final ResourceLocation YDE_VERT_BUTTONS = getResource("animation/yde_vertical_buttons.png");

   // 3D compass
   public static final Map<String, ResourceLocation> TEXTURES_COMPASS = new HashMap<>();
   public static final ResourceLocation RESOURCE_COMPASS = new ResourceLocation(CustomNpcs.MODID + ":models/util/compass.obj");

   static {
      TEXTURES_COMPASS.put("#material", new ResourceLocation(CustomNpcs.MODID, "util/compass"));
      TEXTURES_COMPASS.put("#task", new ResourceLocation(CustomNpcs.MODID, "util/task_0"));
   }

   protected final List<Component> hoverText = new ArrayList<>();
   protected ClientProxy.FontContainer hoverFont = null;
   public int widthTexture = 0;
   public int heightTexture = 0;
   public int borderTexture = 4;

   public GuiBasic() {
      super(Component.empty());
      minecraft = Minecraft.getInstance();
      player = minecraft.player;
      font = minecraft.font;
   }

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

   public static ResourceLocation getResource(String texture) {
      return new ResourceLocation(CustomNpcs.MODID, "textures/gui/" + texture);
   }

   @Override
   public void init() {
      super.init();
      setFocused(null);
      guiLeft = (width - imageWidth) / 2;
      guiTop = (height - imageHeight) / 2;
      renderables.clear();
      children().clear();
      wrapper.init(minecraft, width, height);
   }

   @Override
   public GuiWrapper getWrapper() { return wrapper; }

   @Override
   public void tick() { wrapper.tick(); }

   @Override
   public void buttonEvent(GuiButtonNop button) { }

   @Override
   public boolean mouseButtonEvent(GuiButtonNop button, int mouseButton) { return false; }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
      return wrapper.mouseScrolled(mouseX, mouseY, scrolled) || super.mouseScrolled(mouseX, mouseY, scrolled);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      boolean bo = wrapper.mouseClicked(mouseX, mouseY, mouseButton) || super.mouseClicked(mouseX, mouseY, mouseButton);
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
      return wrapper.mouseDragged(mouseX, mouseY, mouseButton, dx, dy) || super.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
   }

   @Override
   public boolean mouseReleased(double x, double y, int mouseButton) {
      return wrapper.mouseReleased(x, y, mouseButton) || super.mouseReleased(x, y, mouseButton);
   }

   @Override
   public void setFocused(@Nullable GuiEventListener gui) {
      if (wrapper.subgui != null) {
         wrapper.subgui.setFocused(gui);
      } else {
         if (gui != null && !children().contains(gui)) { return; }
         wrapper.changeFocus(getFocused(), gui);
         super.setFocused(gui);
      }
   }

   @Override
   public GuiEventListener getFocused() {
      return wrapper.subgui != null ? wrapper.subgui.getFocused() : super.getFocused();
   }

   @Override
   public void subGuiClosed(Screen subgui) { }

   @Override
   public boolean charTyped(char c, int i) {
      return wrapper.charTyped(c, i) || super.charTyped(c, i);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      checkAltH();
      return wrapper.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean shouldCloseOnEsc() { return closeOnEsc; }

   @Override
   public void onClose() { wrapper.close(); }

   @Override
   public void add(IComponentGui element) {
      wrapper.add(element);
      if (element instanceof GuiTextArea area) { area.setListener(this); }
   }

   public <C extends IComponentGui> C get(int id, Class<C> clazz) {
      for (IComponentGui component : new ArrayList<>(wrapper.components)) {
         if (clazz.isAssignableFrom(component.getClass()) && component.getId() == id) { return clazz.cast(component); }
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

   public GuiMenuSideButton getSideButton(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.SIDE_BUTTON &&
                 element.getId() == id &&
                 element instanceof GuiMenuSideButton) { return (GuiMenuSideButton) element; }
      }
      return null;
   }

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

   public GuiSliderNop getSlider(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.SLIDER &&
                 element.getId() == id &&
                 element instanceof GuiSliderNop) { return (GuiSliderNop) element; }
      }
      return null;
   }

   public GuiCustomScrollNop getScroll(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.SCROLL &&
                 element.getId() == id &&
                 element instanceof GuiCustomScrollNop) { return (GuiCustomScrollNop) element; }
      }
      return null;
   }

   public IComponentGui getExtra(int id) {
      for (IComponentGui element : new ArrayList<>(wrapper.components)) {
         if (element.getElementType() == GuiComponentType.EXTRA && element.getId() == id) { return element; }
      }
      return null;
   }

   @Override
   public void save() { }

   @Override
   public void renderBackground(@Nonnull GuiGraphics graphics) {
      if (drawDefaultBackground) { super.renderBackground(graphics); }
      if (background != null) {
         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         matrixStack.translate((float)guiLeft, (float) guiTop, 0.0F);
         RenderSystem.enableBlend();

         //LogWriter.info("TEST: ["+imageWidth+"; "+imageHeight+"]; "+width+"; "+height+"] "+background);

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
         else { graphics.blit(background, 0, 0, 0, 0, imageWidth, imageHeight); }
         matrixStack.popPose();
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      PoseStack matrixStack = graphics.pose();
      wrapper.graphics = graphics;
      wrapper.mouseX = mouseX;
      wrapper.mouseY = mouseY;
      int x = hasSubGui() ? 0 : (int) (mouseX / bgScale);
      int y = hasSubGui() ? 0 : (int) (mouseY / bgScale);
      matrixStack.scale(bgScale, bgScale, bgScale);
      renderBackground(graphics);
      if (title != null && !title.getString().isEmpty()) {
         GuiButtonNop.renderString(graphics, title, guiLeft + 4, guiTop + 5, guiLeft + imageWidth - 8, guiTop + 15,
                 CustomNpcs.LableColor.getRGB(), false, true, null);
      }
      for (IComponentGui component : new ArrayList<>(wrapper.components)) {
         if (component instanceof Renderable renderable) { renderable.render(graphics, x, y, partialTicks); }
      }
      try { super.render(graphics, x, y, partialTicks); } catch (Exception ignored) { }
      if (wrapper.subgui != null) {
         matrixStack.translate(0.0F, 0.0F, 60.0F);
         wrapper.subgui.render(graphics, mouseX, mouseY, partialTicks);
         matrixStack.translate(0.0F, 0.0F, -60.0F);
      }
      else if ((hoverIsGame || (CustomNpcs.ShowDescriptions && GuiBasic.showHoverText)) && !hoverText.isEmpty()) {
         if (!hoverIsGame) { hoverText.add(Component.translatable("hover.alt.h")); }
         RenderSystem.disableDepthTest();
         if (hoverFont == null) { GuiTooltipUtils.renderTooltip(graphics, font, hoverText, Optional.empty(), mouseX, ValueUtil.correctInt(mouseY, 16, height)); }
         else { renderTooltipInternal(graphics, mouseX, ValueUtil.correctInt(mouseY, 16, height), hoverFont, hoverText, bgScale); }
         hoverText.clear();
      }
   }

   public static void renderTooltipInternal(GuiGraphics graphics, int mouseX, int mouseY, ClientProxy.FontContainer font, List<Component> collections, float scale) {
      if (font != null && !collections.isEmpty()) {
         int toolWidth = 0;
         int toolHeight = (collections.size() == 1 ? -2 : 0);
         for (Component c : new ArrayList<>(collections)) {
            int k = font.width(c);
            if (k > toolWidth) { toolWidth = k; }
         }
         Vector2ic vector2ic = DefaultTooltipPositioner.INSTANCE.positionTooltip(graphics.guiWidth(), graphics.guiHeight(), mouseX, mouseY,
                 (int) (toolWidth * scale), (int) (toolHeight * scale));
         int x = vector2ic.x();
         int y = vector2ic.y();
         PoseStack matrixStack = graphics.pose();
         matrixStack.pushPose();
         matrixStack.translate(x, y, 3600.0F);
         matrixStack.scale(scale, scale, scale);
         toolHeight = collections.size() * font.getHeight() + 1;

         matrixStack.pushPose();
         matrixStack.translate(- 1, - 1, 0.0F);
         matrixStack.scale(0.5f, 0.5f, 0.5f);
         int r = (toolWidth + 1) * 2;
         int b = (toolHeight + 4) * 2;
         int color = YDEController.backColor & 0xFFFFFF | 0xE0000000;
         graphics.fill(-1, -1, r + 3, b + 1, color);
         color = YDEController.windowLineColor & 0xFFFFFF | 0xE0000000;
         graphics.hLine(1, r, 0, color);
         graphics.hLine(1, r, b - 1, color);
         graphics.vLine(0, 0, b - 1, color);
         graphics.vLine(r + 1, 0, b - 1, color);
         matrixStack.popPose();

         matrixStack.translate(0.0F, 0.0F, 400.0F);
         int k1 = 0;
         int k2;
         Component c;
         for(k2 = 0; k2 < collections.size(); ++k2) {
            c = collections.get(k2);
            font.draw(graphics, c, 0, k1, 0xFFFFFF);
            k1 += font.getHeight() + (k2 == 0 ? 2 : 0);
         }
         matrixStack.popPose();
      }

   }

   public Font getFontRenderer() { return font; }

   @Override
   public boolean isPauseScreen() { return false; }

   public void setScreen(Screen gui) {
      ClientEvent.NextToGuiCustomNpcs event = new ClientEvent.NextToGuiCustomNpcs(NoppesUtilServer.getEditingNpc(player), this, gui);
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) { return; }
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
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

   public void drawNpc(GuiGraphics graphics, Entity entity, int x, int y, float zoomed, int rotation, int vertical, int followCursor) {
      wrapper.drawNpc(graphics, entity, x, y, zoomed, rotation, vertical, followCursor, guiLeft, guiTop);
   }

   @Override
   public int getWidth() { return imageWidth; }

   @Override
   public int getHeight() { return imageHeight; }

   @Override
   public boolean doubleClicked(IComponentGui component) { return false; }

   public void openLink(String link) {
      try {
         Class<?> oclass = Class.forName("java.awt.Desktop");
         Object object = oclass.getMethod("getDesktop").invoke(null);
         oclass.getMethod("browse", URI.class).invoke(object, new URI(link));
      } catch (Throwable t) {
         LogWriter.error(t);
      }
   }

   @Override
   public Screen getParent() {
      return wrapper.getParent();
   }

   // New from Unofficial (BetaZavr)
   @Override
   public boolean isMouseHover(double mX, double mY, double px, double py, double pWidth, double pHeight) {
      return mX >= px && mY >= py && mX < (px + pWidth) && mY < (py + pHeight);
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
      if (components != null) { Util.instance.putHovers(hoverText, components) ; }
   }

   @Override
   public void drawHoverText(String text, Object... args) {
      if (!CustomNpcs.ShowDescriptions) { return; }
      if (text == null) {
         if (!hoverText.isEmpty() && wrapper.graphics != null) {
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

   public static boolean isInventoryKey(int key) {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft.options.keyInventory.getKey().getValue() == key;
   }

   public static boolean isUpKey(int key) {
      Minecraft minecraft = Minecraft.getInstance();
      return key == minecraft.options.keyUp.getKey().getValue() || key == InputConstants.getKey("key.keyboard.up").getValue();
   }

   public static boolean isDownKey(int key) {
      Minecraft minecraft = Minecraft.getInstance();
      return key == minecraft.options.keyDown.getKey().getValue() || key == InputConstants.getKey("key.keyboard.down").getValue();
   }

   public static boolean isEnterKey(int key) {
      return key == InputConstants.getKey("key.keyboard.enter").getValue() || key == InputConstants.getKey("key.keyboard.keypad.enter").getValue();
   }

   public static boolean isEscKey(int key) {
      return key == InputConstants.getKey("key.keyboard.escape").getValue();
   }

   public static void checkAltH() {
      if (altHTime >= System.currentTimeMillis()) { return; }
      long id = Minecraft.getInstance().getWindow().getWindow();
      if ((InputConstants.isKeyDown(id, InputConstants.KEY_LALT) || InputConstants.isKeyDown(id, InputConstants.KEY_RALT)) && InputConstants.isKeyDown(id, InputConstants.KEY_H)) {
         altHTime = System.currentTimeMillis() + 1000L;
         showHoverText = !showHoverText;
      }
   }

   @Override
   public void drawWait(GuiGraphics graphics) {
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      if (minecraft.level == null) { return; }
      int x = minecraft.getWindow().getGuiScaledWidth() / 2;
      int y = minecraft.getWindow().getGuiScaledHeight() / 2 - 30;
      PoseStack matrixStack = graphics.pose();
      graphics.drawCenteredString(minecraft.font, Component.translatable("gui.wait", ""), width / 2, height / 2, CustomNpcs.MainColor.getRGB());
      int pos_0 = (int) Math.floor((double) (minecraft.level.getGameTime() % 16) / 2.0d);
      matrixStack.pushPose();
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
      graphics.blit(INFO, x + getPosX.apply(pos_0) - 1, y + getPosY.apply(pos_0) - 1, 0, 12, 6, 6);
      int pos_1 = pos_0 - 1;
      if (pos_1 < 0) { pos_1 += 8; }
      graphics.blit(INFO, x + getPosX.apply(pos_1), y + getPosY.apply(pos_1), 6, 12, 5, 5);
      int pos_2 = pos_0 - 2;
      if (pos_2 < 0) { pos_2 += 8; }
      graphics.blit(INFO, x + getPosX.apply(pos_2) + 1, y + getPosY.apply(pos_2) + 1, 11, 12, 4, 4);
      matrixStack.popPose();
   }

}
