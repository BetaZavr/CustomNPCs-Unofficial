package noppes.npcs.shared.client.gui.components;

import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiEntityDisplay;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

@OnlyIn(Dist.CLIENT)
public class GuiWrapper {

   protected GuiCustomScrollNop onlyScroll;
   public List<IComponentGui> components = new ArrayList<>();
   public IGuiInterface parent;
   public IGuiInterface gui;
   public Screen subgui;
   public int mouseX;
   public int mouseY;

   // New fields from Unofficial (BetaZavr)
   public GuiGraphics graphics;

   public GuiWrapper(IGuiInterface guiIn) { gui = guiIn; }

   public void init(Minecraft mc, int width, int height) {
      GuiTextFieldNop.unfocus();
      if (subgui != null) { subgui.init(mc, width, height); }
      onlyScroll = null;
      components.clear();
   }

   public void tick() {
      if (subgui != null) { subgui.tick(); }
      else {
         for (IComponentGui component : new ArrayList<>(components)) {
            if (component instanceof Screen screen) { screen.tick(); }
            else if (component instanceof EditBox box) { box.tick(); }
            else if (component instanceof GuiTextArea area) { area.tick(); }
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
      if (subgui != null) {
         subgui.mouseScrolled(mouseX, mouseY, scrolled);
         return true;
      }
      // first scrolls
      for (IComponentGui component : new ArrayList<>(components)) {
         if (component.getElementType() == GuiComponentType.SCROLL &&
                 component instanceof GuiEventListener element &&
                 element.mouseScrolled(mouseX, mouseY, scrolled)) {
            return true;
         }
      }
      // then all the others
      for (IComponentGui component : new ArrayList<>(components)) {
         if (component.getElementType() != GuiComponentType.SCROLL &&
                 component instanceof GuiEventListener element &&
                 element.mouseScrolled(mouseX, mouseY, scrolled)) {
            return true;
         }
      }
      // and at end if there is only one scroll
      if (onlyScroll != null) { onlyScroll.mouseForcedScrolled(scrolled); }
      return false;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (subgui != null) {
         subgui.mouseClicked(mouseX, mouseY, mouseButton);
         return true;
      }
      if (GuiTextArea.getActive() != null && GuiTextArea.getActive().mouseClicked(mouseX, mouseY, mouseButton)) {
         return true;
      }
      // first text_fields
      for (IComponentGui component : new ArrayList<>(components)) {
         if ((component.getElementType() == GuiComponentType.TEXT_FIELD || component.getElementType() == GuiComponentType.TEXT_AREA) &&
                 component instanceof GuiEventListener element &&
                 element.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
         }
      }
      // next everything except text_fields and scrolls
      for (IComponentGui component : new ArrayList<>(components)) {
         if (component.getElementType() != GuiComponentType.TEXT_FIELD &&
                 component.getElementType() != GuiComponentType.TEXT_AREA &&
                 component.getElementType() != GuiComponentType.SCROLL &&
                 component instanceof GuiEventListener element &&
                 element.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
         }
      }
      // and at end only scrolls
      for (IComponentGui component : new ArrayList<>(components)) {
         if (component.getElementType() == GuiComponentType.SCROLL &&
                 component instanceof GuiEventListener element &&
                 element.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
         }
      }
      return false;
   }

   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
      if (subgui != null) {
         subgui.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
         return true;
      }
      GuiSliderNop slider = null;
      for (IComponentGui component : new ArrayList<>(components)) {
         if (component instanceof GuiSliderNop s && s.isDrag) { slider = s; break; }
      }
      if (slider != null && slider.mouseDragged(mouseX, mouseY, mouseButton, dx, dy)) { return true; }
      for (IComponentGui component : new ArrayList<>(components)) {
         if (component instanceof GuiEventListener element && element.mouseDragged(mouseX, mouseY, mouseButton, dx, dy)) { return true; }
      }
      return false;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
      if (subgui != null) {
         subgui.mouseReleased(mouseX, mouseY, mouseButton);
         return true;
      }
      for (IComponentGui component : new ArrayList<>(components)) {
         if (component instanceof GuiEventListener element && element.mouseReleased(mouseX, mouseY, mouseButton)) {
            return true;
         }
      }
      return false;
   }

   public boolean charTyped(char typedChar, int keyCode) {
      if (subgui != null) {
         subgui.charTyped(typedChar, keyCode);
         return true;
      }
      // first text_fields and scrolls
      for (IComponentGui component : new ArrayList<>(components)) {
         if (component.getElementType() != GuiComponentType.TEXT_FIELD &&
                 component.getElementType() != GuiComponentType.TEXT_AREA &&
                 component.getElementType() != GuiComponentType.SCROLL &&
                 component instanceof GuiEventListener element && element.charTyped(typedChar, keyCode)) {
            return true;
         }
      }
      // then all the others
      for (IComponentGui component : new ArrayList<>(components)) {
         if (component instanceof GuiEventListener element && element.charTyped(typedChar, keyCode)) {
            return true;
         }
      }
      return false;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (subgui != null) {
         subgui.keyPressed(keyCode, scanCode, modifiers);
         return true;
      }
      boolean active = GuiTextFieldNop.getActive() != null;
      if (!active) {
         for (IComponentGui component : new ArrayList<>(components)) {
            if (component instanceof GuiEventListener element && element.isFocused()) {
               active = true;
               break;
            }
         }
      }
      if (gui instanceof Screen screen && screen.shouldCloseOnEsc() && GuiBasic.isEscKey(keyCode) && !active) {
         screen.onClose();
         return true;
      }
      else {
         for (IComponentGui component : new ArrayList<>(components)) {
            if ((component instanceof GuiTextArea area && area.isFocused() && area.keyPressed(keyCode, scanCode, modifiers)) ||
               (component instanceof GuiTextFieldNop textField && textField.isFocused() && textField.keyPressed(keyCode, scanCode, modifiers))) {
               return true;
            }
         }
         for (IComponentGui component : new ArrayList<>(components)) {
            if (component instanceof GuiEventListener element && element.keyPressed(keyCode, scanCode, modifiers)) {
               return true;
            }
         }
      }
      return false;
   }

   public void drawNpc(GuiGraphics graphics, Entity entity, int x, int y, float zoomed, int rotation, int vertical, int followCursor, int guiLeft, int guiTop) {
      CustomGuiEntityDisplay.drawEntity(graphics, entity, x, y, zoomed, rotation, vertical, mouseX, mouseY, (float) guiLeft, (float) guiTop, followCursor, true);
   }

   public void changeFocus(GuiEventListener old, GuiEventListener gui) {
      if (old instanceof GuiSliderNop && gui != old) {
         ((GuiSliderNop)old).onRelease(0.0D, 0.0D);
      }
   }

   public void setSubgui(Screen subguiIn) {
      ((Screen) gui).setFocused(null);
      subgui = subguiIn;
      if (subgui != null) {
         subgui.init(Minecraft.getInstance(), ((Screen) gui).width, ((Screen) gui).height);
         if (subgui instanceof IGuiInterface) {
            ((IGuiInterface) subgui).getWrapper().parent = gui;
         }
      }
   }

   public Screen getSubGui() { return subgui instanceof IGuiInterface sGui && sGui.hasSubGui() ? sGui.getSubGui() : subgui; }

   public Screen getParent() { return parent != null ? parent.getParent() : (Screen) gui; }

   public void close() {
      GuiTextFieldNop.unfocus();
      gui.save();
      if (parent instanceof Screen screen) {
         screen.setFocused(null);
         parent.getWrapper().subgui = null;
         parent.subGuiClosed((Screen) gui);
         Minecraft mc = Minecraft.getInstance();
         screen.init(mc, screen.width, screen.height);
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         minecraft.setScreen((Screen) gui);
         minecraft.mouseHandler.grabMouse();
      }
   }

   public void add(IComponentGui element) {
      if (element == null) { return; }
      List<IComponentGui> newComponents = new ArrayList<>(components);
      for (IComponentGui comp : newComponents) {
         if (comp.getClass() == element.getClass() && comp.getId() == element.getId()) {
            newComponents.remove(comp);
            break;
         }
      }
      newComponents.add(element);
      newComponents.sort(Comparator.comparing(IComponentGui::getElementType)
              .thenComparingInt(IComponentGui::getId));
      components = newComponents;
   }

   public <C extends IComponentGui> List<C> getComponents(Class<C> clazz) {
      List<C> list = new ArrayList<>();
      for (IComponentGui component : new ArrayList<>(components)) {
         if (clazz.isAssignableFrom(component.getClass())) { list.add(clazz.cast(component)); }
      }
      return list;
   }

}
