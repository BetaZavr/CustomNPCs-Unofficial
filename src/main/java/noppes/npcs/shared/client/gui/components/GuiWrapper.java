package noppes.npcs.shared.client.gui.components;

import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.GuiBasicContainer;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiEntityDisplay;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.common.util.LogWriter;

public class GuiWrapper {

    protected GuiCustomScrollNop onlyScroll;
    protected IComponentGui lastFocusedComponent;

    public List<IComponentGui> components = new ArrayList<>();
    public GuiScreen parent;
    public IGuiInterface gui;
    public GuiScreen subgui;
    public int mouseX;
    public int mouseY;

    public GuiWrapper(IGuiInterface guiIn) { gui = guiIn; }

    public void initGui(Minecraft mc, int width, int height) {
        lastFocusedComponent = null;
        GuiTextFieldNop.unfocus();
        if (subgui != null) { subgui.setWorldAndResolution(mc, width, height); }
        onlyScroll = null;
        components.clear();
    }

    public void tick() {
        if (subgui != null) { subgui.updateScreen(); }
        else {
            for (IComponentGui component : new ArrayList<>(components)) { component.tick(); }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
        if (subgui != null) {
            if (subgui instanceof IGuiInterface) { return ((IGuiInterface) subgui).mouseScrolled(mouseX, mouseY, scrolled); }
            return true;
        }
        // first scrolls
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component.getElementType() == GuiComponentType.SCROLL && component.mouseScrolled(mouseX, mouseY, scrolled)) {
                return true;
            }
        }
        // then all the others
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component.getElementType() != GuiComponentType.SCROLL && component.mouseScrolled(mouseX, mouseY, scrolled)) {
                return true;
            }
        }
        // and at end if there is only one scroll
        if (onlyScroll != null) {
            onlyScroll.mouseForcedScrolled(scrolled);
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (subgui != null) {
            if (subgui instanceof IGuiInterface) { return ((IGuiInterface) subgui).mouseClicked(mouseX, mouseY, mouseButton); }
            setFocus(null);
            return true;
        }
        // first text_fields
        for (IComponentGui component : new ArrayList<>(components)) {
            if ((component.getElementType() == GuiComponentType.TEXT_FIELD || component.getElementType() == GuiComponentType.TEXT_AREA) &&
                    component.mouseClicked(mouseX, mouseY, mouseButton)) {
                setFocus(component);
                return true;
            }
        }
        // next everything except text_fields and scrolls
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component.getElementType() != GuiComponentType.TEXT_FIELD &&
                    component.getElementType() != GuiComponentType.TEXT_AREA &&
                    component.getElementType() != GuiComponentType.SCROLL &&
                    component.mouseClicked(mouseX, mouseY, mouseButton)) {
                setFocus(component);
                return true;
            }
        }
        // and at end only scrolls
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component.getElementType() == GuiComponentType.SCROLL && component.mouseClicked(mouseX, mouseY, mouseButton)) {
                setFocus(component);
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
        if (subgui != null) {
            if (subgui instanceof IGuiInterface) { return ((IGuiInterface) subgui).mouseDragged(mouseX, mouseY, mouseButton, dx, dy); }
            return true;
        }
        GuiSliderNop slider = null;
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component instanceof GuiSliderNop && ((GuiSliderNop) component).isDrag) { slider = (GuiSliderNop) component; break; }
        }
        if (slider != null && slider.mouseDragged(mouseX, mouseY, mouseButton, dx, dy)) { return true; }
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component.mouseDragged(mouseX, mouseY, mouseButton, dx, dy)) { return true; }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (subgui != null) {
            if (subgui instanceof IGuiInterface) { return ((IGuiInterface) subgui).mouseReleased(mouseX, mouseY, mouseButton); }
            return true;
        }
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(char typedChar, int keyCode) {
        if (subgui != null) {
            if (subgui instanceof IGuiInterface) { return ((IGuiInterface) subgui).keyPressed(typedChar, keyCode); }
            return true;
        }
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component.keyPressed(typedChar, keyCode)) { return true; }
        }
        return false;
    }

    public void drawNpc(Entity entity, int x, int y, float zoomed, int yaw, int pitch, int followCursor, int guiLeft, int guiTop) {
        CustomGuiEntityDisplay.drawEntity(entity, x, y, zoomed, yaw, pitch, mouseX, mouseY, (float) guiLeft, (float) guiTop, followCursor);
    }

    public void setFocus(IComponentGui focused) {
        if (focused != null && !focused.isHovered()) { return; }
        for (IComponentGui component : new ArrayList<>(components)) {
            component.setIsFocused(component == focused);
            if (component instanceof GuiCustomScrollNop) { ((GuiCustomScrollNop) component).textField.setIsFocused(component == focused); }
        }
        if (focused instanceof GuiSliderNop && lastFocusedComponent != focused) { ((GuiSliderNop) focused).onRelease(0.0D, 0.0D); }
        lastFocusedComponent = focused;
    }

    public void initFocus() {
        // first text_fields
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component.getElementType() == GuiComponentType.TEXT_FIELD) {
                setFocus(component);
                return;
            }
        }
        // next scrolls
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component.getElementType() == GuiComponentType.SCROLL) {
                setFocus(component);
                return;
            }
        }
        // then all the others
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component.getElementType().isSelectable() &&
                    component.getElementType() != GuiComponentType.TEXT_FIELD &&
                    component.getElementType() != GuiComponentType.SCROLL &&
                    component.getElementType() != GuiComponentType.LABEL &&
                    component.getElementType() != GuiComponentType.EXTRA) {
                setFocus(component);
                return;
            }
        }
    }

    public void setSubgui(GuiScreen subguiIn) {
        ((GuiScreen) gui).setFocused(false);
        subgui = subguiIn;
        if (subgui != null) {
            if (subgui instanceof IGuiInterface) {
                ((IGuiInterface) subgui).getWrapper().parent = (GuiScreen) gui;
            }
        }
    }

    public GuiScreen getSubGui() { return subgui instanceof IGuiInterface && ((IGuiInterface) subgui).hasSubGui() ? ((IGuiInterface) subgui).getSubGui() : subgui; }

    public GuiScreen getParent() { return parent != null ? ((IGuiInterface) parent).getParent() : (GuiScreen) gui; }

    public void close() {
        GuiTextFieldNop.unfocus();
        Minecraft mc = Minecraft.getMinecraft();
        if (parent != null) {
            gui.save();
            if (parent instanceof IGuiInterface) {
                parent.setFocused(false);
                ((IGuiInterface) parent).getWrapper().subgui = null;
                ((IGuiInterface) parent).subGuiClosed((GuiScreen) gui);
                parent.initGui();
            }
            else { mc.displayGuiScreen(parent); }
        }
        else {
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
        }
    }

    public void add(IComponentGui element) {
        if (element == null) { return; }
        List<IComponentGui> newComponents = new ArrayList<>(components);
        for (IComponentGui comp : newComponents) {
            if (comp != null && comp.getClass() == element.getClass() && comp.getId() == element.getId()) {
                newComponents.remove(comp);
                break;
            }
        }
        newComponents.add(element);
        //newComponents.sort(Comparator.comparing(IComponentGui::getElementType).thenComparingInt(IComponentGui::getId));
        components = newComponents;
    }

    public <C extends IComponentGui> List<C> getComponents(Class<C> clazz) {
        List<C> list = new ArrayList<>();
        for (IComponentGui component : new ArrayList<>(components)) {
            if (clazz.isAssignableFrom(component.getClass())) { list.add(clazz.cast(component)); }
        }
        return list;
    }

    public IComponentGui getFocused() {
        if (subgui instanceof GuiBasic) { return ((GuiBasic) subgui).getFocused(); }
        if (subgui instanceof GuiBasicContainer) { return ((GuiBasicContainer<?>) subgui).getFocused(); }
        for (IComponentGui component : new ArrayList<>(components)) {
            if (component.isFocused()) { return component; }
        }
        return null;
    }

    public IComponentGui getLastFocused() { return lastFocusedComponent; }

    public void focusedNextComponent() {
        if (subgui != null) {
            if (subgui instanceof IGuiInterface) { ((IGuiInterface) subgui).focusedNextComponent(); }
            return;
        }
        boolean found = false;
        boolean change = false;
        IComponentGui first = null;
        for (IComponentGui component : new ArrayList<>(components)) {
            if (!component.getElementType().isSelectable() || !component.isVisible() || !component.isEnabled()) {
                continue;
            }
            if (first == null) { first = component; }
            if (!found && component.isFocused()) {
                component.setIsFocused(false);
                found = true;
            }
            else if (found && !component.isFocused()) {
                component.setIsFocused(true);
                change = true;
                break;
            }
        }
        if (!change && first != null) { first.setIsFocused(true); }
    }

    public void focusedPrevComponent() {
        if (subgui != null) {
            if (subgui instanceof IGuiInterface) { ((IGuiInterface) subgui).focusedPrevComponent(); }
            return;
        }
        boolean change = false;
        IComponentGui last = null;
        for (IComponentGui component : new ArrayList<>(components)) {
            if (!component.getElementType().isSelectable() || !component.isVisible() || !component.isEnabled()) { continue; }
            if (component.isFocused()) {
                component.setIsFocused(false);
                if (last != null) {
                    last.setIsFocused(true);
                    change = true;
                    break;
                }
            }
            last = component;
        }
        if (!change && last != null) { last.setIsFocused(true); }
    }

}
