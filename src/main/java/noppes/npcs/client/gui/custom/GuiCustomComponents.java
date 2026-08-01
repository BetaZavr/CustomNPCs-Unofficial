package noppes.npcs.client.gui.custom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.gui.IComponentsWrapper;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.gui.IItemSlot;
import noppes.npcs.api.wrapper.gui.CustomGuiAssetsSelectorWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiButtonListWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiButtonWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiColoredLineWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiEntityDisplayWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiItemRendererWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiLabelWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiScrollWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiSliderWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiTextAreaWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiTextFieldWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiTexturedRectWrapper;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiAssetsSelector;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiButton;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiButtonList;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiColoredLine;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiEntityDisplay;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiItemRenderer;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiLabel;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiScroll;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiSlider;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiTextArea;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiTextField;
import noppes.npcs.shared.client.gui.components.custom.CustomGuiTexturedRect;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;

public class GuiCustomComponents extends Gui {

    public static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/components.png");
    protected List<IItemSlot> slots = new ArrayList<>();
    protected int draggingId = -1;
    public Map<Integer, IComponentGui> components = new HashMap<>();

    public void setComponents(GuiCustom gui, IComponentsWrapper comps) {
        Map<Integer, IComponentGui> newComponents = new HashMap<>();
        for (ICustomGuiComponent comp : comps.getComponents()) {
            switch (comp.getType()) {
                case 0:
                    CustomGuiButton button = new CustomGuiButton(gui, (CustomGuiButtonWrapper) comp);
                    newComponents.put(button.getId(), button);
                    break;
                case 1:
                    CustomGuiLabel lbl = new CustomGuiLabel(gui, (CustomGuiLabelWrapper) comp);
                    newComponents.put(lbl.getId(), lbl);
                    break;
                case 2:
                    CustomGuiTexturedRect rect = new CustomGuiTexturedRect(gui, (CustomGuiTexturedRectWrapper) comp);
                    newComponents.put(rect.getId(), rect);
                    break;
                case 3:
                    CustomGuiTextField textField = new CustomGuiTextField(gui, (CustomGuiTextFieldWrapper) comp);
                    newComponents.put(textField.getId(), textField);
                    break;
                case 4:
                    CustomGuiScroll scroll = new CustomGuiScroll(gui, (CustomGuiScrollWrapper) comp);
                    newComponents.put(scroll.getId(), scroll);
                    break;
                case 6:
                    if (comp instanceof CustomGuiTextAreaWrapper) {
                        CustomGuiTextArea textArea = new CustomGuiTextArea(gui, (CustomGuiTextAreaWrapper) comp);
                        newComponents.put(textArea.id, textArea);
                    }
                    break;
                case 7:
                    newComponents.put(comp.getId(), new CustomGuiButtonList(gui, (CustomGuiButtonListWrapper) comp));
                    break;
                case 8:
                    CustomGuiSlider slider = new CustomGuiSlider(gui, (CustomGuiSliderWrapper) comp);
                    newComponents.put(slider.getId(), slider);
                    break;
                case 9:
                    CustomGuiEntityDisplay display = new CustomGuiEntityDisplay(gui, (CustomGuiEntityDisplayWrapper) comp);
                    newComponents.put(display.getId(), display);
                    break;
                case 10:
                    CustomGuiAssetsSelector assets = new CustomGuiAssetsSelector(gui, (CustomGuiAssetsSelectorWrapper) comp);
                    newComponents.put(assets.getId(), assets);
                    break;
                case 11:
                    CustomGuiColoredLine coloredLine = new CustomGuiColoredLine(gui, (CustomGuiColoredLineWrapper) comp);
                    newComponents.put(coloredLine.getId(), coloredLine);
                    break;
                case 12:
                    CustomGuiItemRenderer itemRenderer = new CustomGuiItemRenderer(gui, (CustomGuiItemRendererWrapper) comp);
                    newComponents.put(itemRenderer.getId(), itemRenderer);
                    break;
                default:
                    break;
            }
        }
        components = newComponents;
        List<IItemSlot> newSlots = new ArrayList<>();
        newSlots.addAll(comps.getSlots());
        newSlots.addAll(comps.getPlayerSlots());
        slots = newSlots;
    }

    public void render(int mouseX, int mouseY, float partialTicks) {
        for (IItemSlot slot : slots) {
            if (slot.getGuiType() > 0) { renderSlot(slot); }
        }
        for (IComponentGui component : components.values()) {
            component.render(mouseX, mouseY, partialTicks);
        }
    }

    public void renderSlot(IItemSlot slot) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(resource);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(slot.getPosX() - 1, slot.getPosY() - 1, 0, 80, 18, 18);
    }

    public void containerTick() {
        for (IComponentGui component : components.values()) { component.tick(); }
    }

    public boolean keyPressed(char typedChar, int keyCode) {
        for (IComponentGui comp : components.values()) {
            if (comp.keyPressed(typedChar, keyCode)) { return true; }
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean hasClickedAny = false;
        for (IComponentGui comp : components.values()) {
            if (comp.mouseClicked(mouseX, mouseY, mouseButton)) {
                if (mouseButton == 0) { draggingId = comp.getId(); }
                hasClickedAny = true;
            }
        }
        return hasClickedAny;
    }

    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        for (IComponentGui comp : components.values()) {
            if (comp.getId() == draggingId && comp.mouseDragged(x, y, button, dx, dy)) { return true; }
        }
        return false;
    }

    public boolean mouseReleased(double x, double y, int button) {
        for (IComponentGui comp : components.values()) {
            if (comp.getId() == draggingId && comp.mouseReleased(x, y, button)) {
                draggingId = -1;
                return true;
            }
        }
        draggingId = -1;
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double mouseScrolled) {
        for (IComponentGui comp : components.values()) {
            if (comp.mouseScrolled(mouseX, mouseY, mouseScrolled)) { return true; }
        }
        return false;
    }

}
