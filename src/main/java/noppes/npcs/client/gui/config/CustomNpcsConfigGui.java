package noppes.npcs.client.gui.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.config.ConfigElement;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.config.GuiConfigEntries;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CustomNpcsConfigGui extends GuiBasic {

    protected final Screen lastScreen;
    public final List<ConfigElement> configElements;
    public final String modID;
    public Component titleLine;
    public Component titleLine2;
    public GuiConfigEntries entryList;

    public CustomNpcsConfigGui(Screen parentScreen, List<ConfigElement> childElements, String modname) {
        super();
        drawDefaultBackground = false;
        titleLine = Component.literal(modname);
        closeOnEsc = false;

        lastScreen = parentScreen;
        configElements = childElements;

        modID = modname;
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        switch (button.id) {
            case 2000: {
                if (entryList != null && entryList.id != -1) {
                    ConfigElement configElement = configElements.get(entryList.id);
                    if (configElement != null && configElement.isArray()) {
                        titleLine2 = null;
                        configElement.setArrayValue(entryList.listEntries);
                    }
                    entryList = null;
                    init();
                }
                else { onClose(); }
                break;
            }
            case 2001: {
                if (getButton(2003) == null || !((GuiCheckBoxNop) getButton(2003)).selected()) { return; }
                for (ConfigElement ce : configElements) { ce.setValue(ce.defaultValue); }
                init();
                break;
            } // reset ☄
            case 2002: {
                if (getButton(2003) == null || !((GuiCheckBoxNop) getButton(2003)).selected()) { return; }
                for (ConfigElement ce : configElements) { ce.setValue(ce.firstValue); }
                init();
                break;
            } // undo ↶
            case 2003: { break; } // apply global
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        renderBackground(graphics);
        graphics.drawCenteredString(font, titleLine, width / 2, 8, 0xFFFFFF);
        if (titleLine2 != null) { graphics.drawCenteredString(font, titleLine2, width / 2, 20, 0xFFFFFF); }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void init() {
        super.init();
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        if (entryList != null && entryList.id != -1) {
            ConfigElement configElement = configElements.get(entryList.id);
            if (configElement != null && configElement.isArray()) {
                titleLine2 = Component.literal(configElement.name+":");
                entryList.resetList(configElement.getArrayElements());
            }
            else { entryList = null; }
        }
        if (entryList == null || entryList.id == -1) {
            entryList = new GuiConfigEntries(this, -1, 0, titleLine2 != null ? 32 : 20, width, height - 32, 22);
            entryList.resetList(configElements);
        }
        add(entryList);
        int buttonWidthHalf = 416 / 2;
        addButton(2000, width / 2 - buttonWidthHalf, height - 29, "gui.done")
                .setSize(100, 20);
        addLabel(2001, width / 2 - buttonWidthHalf + 105, height - 25, "☄")
                .setSize(8, 12)
                .setColor(0xFFFFFF);
        addButton(2001, width / 2 - buttonWidthHalf + 115, height - 29, "gui.default")
                .setSize(90, 20)
                .setHoverTexts("fml.configgui.tooltip.resetToDefault");

        addLabel(2002, width / 2 - buttonWidthHalf + 210, height - 25, "↶")
                .setSize(8, 12)
                .setColor(0xFFFFFF);
        addButton(2002, width / 2 - buttonWidthHalf + 220, height - 29, "gui.restore")
                .setSize(90, 20)
                .setHoverTexts("fml.configgui.tooltip.undoChanges");
        addCheckBox(2003, width / 2 - buttonWidthHalf + 315, height - 26, "fml.configgui.applyGlobally", null, true);
        if (getSubGui() instanceof GuiBasic subgui) {
            subgui.width = width;
            subgui.height = height;
            subgui.init();
        }
    }

    @Override
    public boolean keyPressed(int key_0, int key_1, int key_2) {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        if (hasSubGui()) {
            getSubGui().keyPressed(key_0, key_1, key_2);
            return true;
        }
        if (entryList.keyPressed(key_0, key_1, key_2)) { return true; }
        if (key_0 == InputConstants.KEY_ESCAPE) {
            if (entryList != null && entryList.id != -1) {
                ConfigElement configElement = configElements.get(entryList.id);
                if (configElement != null && configElement.isArray()) {
                    titleLine2 = null;
                    configElement.setArrayValue(entryList.listEntries);
                }
                entryList = null;
                init();
            }
            else { onClose(); }
            return true;
        }
        return super.keyPressed(key_0, key_1, key_2);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (hasSubGui()) {
            getSubGui().mouseClicked(mouseX, mouseY, mouseButton);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (hasSubGui()) {
            getSubGui().mouseReleased(mouseX, mouseY, mouseButton);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double x, double y, int mouseButton, double dx, double dy) {
        if (hasSubGui()) {
            getSubGui().mouseDragged(x, y, mouseButton, dx, dy);
            return true;
        }
        return super.mouseDragged(x, y, mouseButton, dx, dy);
    }

    @Override
    public void onClose() {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        CustomNpcs.Config.updateConfig();
        CustomNpcs.resetChars();
        minecraft.setScreen(this.lastScreen);
    }

}
