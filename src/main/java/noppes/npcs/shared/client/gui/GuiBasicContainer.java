package noppes.npcs.shared.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.api.event.ClientEvent;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.containers.ContainerEmpty;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class GuiBasicContainer<T extends Container> extends GuiContainer implements IGuiInterface {

    // standard
    private long lastMouseEvent;
    private int eventButton;
    private int touchValue;

    public boolean drawDefaultBackground = true;
    public int guiLeft;
    public int guiTop;
    public EntityPlayerSP player;
    public GuiWrapper wrapper = new GuiWrapper(this);
    public Component title;
    public boolean closeOnEsc = true;
    public boolean hoverIsGame = false;

    // New fields from Unofficial (BetaZavr)
    protected final List<Component> hoverText = new ArrayList<>();
    protected ClientProxy.FontContainer hoverFont = null;
    protected ScaledResolution scaledResolution;
    public ResourceLocation background = null;
    public float bgScale = 1.0F;
    public int widthTexture = 0;
    public int heightTexture = 0;
    public int borderTexture = 4;

    // standard
    public final Minecraft minecraft;
    protected FontRenderer font;
    protected final T menu;

    public GuiBasicContainer(T cont, Component titleIn) {
        super(cont);
        menu = cont;
        mc = Minecraft.getMinecraft();
        minecraft = mc;
        scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        font = (fontRenderer = mc.fontRenderer);
        itemRender = mc.getRenderItem();
        player = mc.player;
        title = titleIn;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
        scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        wrapper.initGui(mc, width, height);
        setFocused(!hasSubGui());
    }

    public static ResourceLocation getResource(String texture) {
        return new ResourceLocation(CustomNpcs.MODID, "textures/gui/" + texture);
    }

    @Override
    public void updateScreen() { wrapper.tick(); }

    @Override
    public void handleMouseInput() throws IOException {
        if (wrapper.subgui != null) {
            wrapper.subgui.handleMouseInput();
            return;
        }
        double mouseX = (double) Mouse.getX() / (double) scaledResolution.getScaleFactor();
        double mouseY = (double) (mc.displayHeight - Mouse.getY()) / (double) scaledResolution.getScaleFactor() - 1;
        int mouseButton = Mouse.getEventButton();
        if (Mouse.getEventButtonState()) {
            if (mc.gameSettings.touchscreen && touchValue++ > 0) { return; }
            eventButton = mouseButton;
            lastMouseEvent = Minecraft.getSystemTime();
            mouseClicked(mouseX, mouseY, mouseButton);
        }
        else if (mouseButton != -1) {
            if (mc.gameSettings.touchscreen && --touchValue > 0) { return; }
            eventButton = -1;
            mouseReleased(mouseX, mouseY, mouseButton);
        }
        else if (eventButton != -1 && lastMouseEvent > 0L) {
            double dx = (double) Mouse.getEventDX() / (double) scaledResolution.getScaleFactor();
            double dy = (double) -Mouse.getEventDY() / (double) scaledResolution.getScaleFactor();
            mouseDragged(mouseX, mouseY, eventButton, dx, dy);
        }
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) { mouseScrolled(mouseX, mouseY, (double) dWheel / 120.0d); }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean bo = wrapper.mouseClicked(mouseX, mouseY, mouseButton);
        try { super.mouseClicked((int) mouseX, (int) mouseY, mouseButton); } catch (IOException ignored) { }
        if (GuiTextFieldNop.getActive() != null) {
            for (IComponentGui component : wrapper.components) {
                if (component instanceof GuiTextArea) {
                    ((GuiTextArea) component).active = false;
                    ((GuiTextArea) component).setIsFocused(false);
                }
            }
        }
        return bo;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
        if (wrapper.mouseDragged(mouseX, mouseY, mouseButton, dx, dy)) { return true; }
        mouseClickMove((int) mouseX, (int) mouseY, eventButton, Minecraft.getSystemTime() - lastMouseEvent);
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
        return wrapper.mouseScrolled((int) mouseX, (int) mouseY, (int) scrolled);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (wrapper.mouseReleased(mouseX, mouseY, mouseButton)) { return true; }
        super.mouseReleased((int) mouseX, (int) mouseY, mouseButton);
        return false;
    }

    @Nullable
    public Slot findSlot(double mouseX, double mouseY, Slot foundSlot) { return foundSlot; }

    @Override
    public void subGuiClosed(GuiScreen subgui) { }

    @Override
    public IComponentGui getLastFocused() { return wrapper.getLastFocused(); }

    @Override
    public GuiWrapper getWrapper() { return wrapper; }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (wrapper.subgui == null && GuiTextFieldNop.getActive() == null) {
            if (closeOnEsc && GuiBasic.isEscKey(keyCode)) {
                onClose();
            }
            checkHotbarKeys(keyCode);
            if (getSlotUnderMouse() != null && getSlotUnderMouse().getHasStack()) {
                if (mc.gameSettings.keyBindPickBlock.isActiveAndMatches(keyCode)) {
                    handleMouseClick(getSlotUnderMouse(), getSlotUnderMouse().slotNumber, 0, ClickType.CLONE);
                    return;
                }
                else if (mc.gameSettings.keyBindDrop.isActiveAndMatches(keyCode)) {
                    handleMouseClick(getSlotUnderMouse(), getSlotUnderMouse().slotNumber, isCtrlKeyDown() ? 1 : 0, ClickType.THROW);
                    return;
                }
            }
        }
        keyPressed(typedChar, keyCode);
    }

    public boolean keyPressed(char typedChar, int keyCode) {
        if (wrapper.subgui == null) { GuiBasic.checkAltH(); }
        if (GuiBasic.isEscKey(keyCode)) {
            if (wrapper.subgui != null) { return wrapper.keyPressed(typedChar, keyCode); }
            if (GuiTextFieldNop.getActive() != null) {
                GuiTextFieldNop.unfocus();
                return true;
            }
            if (closeOnEsc) {
                onClose();
                return true;
            }
        }
        boolean bo = wrapper.keyPressed(typedChar, keyCode);
        if (!bo) {
            boolean typing = GuiTextFieldNop.getActive() != null;
            switch (keyCode) {
                case Keyboard.KEY_TAB: {
                    focusedNextComponent();
                    return true;
                } // focused next component
                case Keyboard.KEY_DOWN: {
                    if (!typing) { focusedNextComponent(); }
                    return true;
                } // focused next component
                case Keyboard.KEY_UP: {
                    if (!typing) { focusedPrevComponent(); }
                    return true;
                } // focused prev component
            }
        }
        return bo;
    }

    @Override
    public void setFocused(boolean hasFocusedControlIn) {
        if (hasFocusedControlIn) { wrapper.initFocus(); }
        else { wrapper.setFocus(null); }
    }

    public IComponentGui getFocused() { return wrapper.getFocused(); }

    @Override
    public void onClose() { wrapper.close(); }

    @Override
    public void onGuiClosed() {
        save();
        super.onGuiClosed();
    }

    @Override
    public void add(IComponentGui element) {
        wrapper.add(element);
        if (element instanceof GuiTextArea) { ((GuiTextArea) element).setListener(this); }
    }

    @Override
    public void buttonEvent(GuiButtonNop button) { }

    @Override
    public boolean mouseButtonEvent(GuiButtonNop button, int mouseButton) { return false; }

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
    public void drawDefaultBackground() {
        if (drawDefaultBackground) { super.drawDefaultBackground(); }
        if (wrapper.subgui == null) {
            {
                postDrawBackground();
                if (background != null && !(this instanceof GuiContainerNPCInterface2)) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(guiLeft, guiTop, 0.0f);
                    GlStateManager.scale(bgScale, bgScale, bgScale);
                    mc.getTextureManager().bindTexture(background);
                    if (widthTexture != 0 && heightTexture != 0) {
                        int maxRow = ValueUtil.correctInt((int) Math.ceil((float) ySize / (float) (heightTexture - 2 * borderTexture)), 2, 10);
                        int maxCol = ValueUtil.correctInt((int) Math.ceil((float) xSize / (float) (widthTexture - 2 * borderTexture)), 2, 10);
                        int tileWidth = xSize / maxCol;
                        int tileHeight = ySize / maxRow;
                        int lastTileWidth = xSize - tileWidth * (maxCol - 1);
                        int lastTileHeight = ySize - tileHeight * (maxRow - 1);

                        int uOffset = (widthTexture - 2 * borderTexture - tileWidth) / 2;
                        int uMax = widthTexture - lastTileWidth;
                        int vOffset = (heightTexture - 2 * borderTexture - tileHeight) / 2;
                        int vMax = heightTexture - lastTileHeight;
                        for (int col = 0; col < maxCol; ++col) {
                            for (int row = 0; row < maxRow; ++row) {
                                drawTexturedModalRect(col * tileWidth,
                                        row * tileHeight,
                                        col == 0 ? 0 : col == maxCol - 1 ? uMax : uOffset,
                                        row == 0 ? 0 : row == maxRow - 1 ? vMax : vOffset,
                                        col == maxCol - 1 ? lastTileWidth : tileWidth,
                                        row == maxRow - 1 ? lastTileHeight : tileHeight);
                            }
                        }
                    }
                    else { drawTexturedModalRect(0, 0, 0, 0, xSize, ySize); }
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    public void postDrawBackground() { }

    @Override
    public void save() { }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();
        if (title != null && !title.getFormattedText().isEmpty()) {
            GuiButtonNop.renderString(title, guiLeft + 4, guiTop + 5, guiLeft + xSize - 8, guiTop + 15,
                    CustomNpcs.LableColor.getRGB(), false, true, null);
        }
    }

    @Override
    public void preDrawScreen(int mouseX, int mouseY) { }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        preDrawScreen(mouseX, mouseY);
        wrapper.mouseX = mouseX;
        wrapper.mouseY = mouseY;
        Container container = inventorySlots;
        int x = mouseX;
        int y = mouseY;
        if (hasSubGui()) {
            inventorySlots = new ContainerEmpty();
            x = 0;
            y = 0;
        }
        super.drawScreen(x, y, partialTicks);
        zLevel = 0.0f;
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableLighting();
        for (IComponentGui component : new ArrayList<>(wrapper.components)) { component.render(x, y, partialTicks); }
        RenderHelper.disableStandardItemLighting();
        if (wrapper.subgui != null) {
            inventorySlots = container;
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 100.0F);
            wrapper.subgui.drawScreen(mouseX, mouseY, partialTicks);
            GlStateManager.popMatrix();
        }
        else { renderHoveredToolTip(mouseX, mouseY); }
    }

    @Override
    public void renderHoveredToolTip(int mouseX, int mouseY) {
        if (mc.player.inventory.getItemStack().isEmpty() && getSlotUnderMouse() != null && getSlotUnderMouse().getHasStack()) {
            renderToolTip(getSlotUnderMouse().getStack(), mouseX, mouseY);
        }
        else if (hoverIsGame || (CustomNpcs.ShowDescriptions && GuiNPCInterface.showHoverText) && !hoverText.isEmpty()) {
            drawHover(mouseX, mouseY);
            hoverText.clear();
        }
    }

    public void drawHover(int mouseX, int mouseY) {
        Component addHoverText = Component.translatable("hover.alt.h");
        if (!hoverIsGame && !Util.instance.equalsDeleteColor(hoverText.get(hoverText.size() - 1).getFormattedText(), addHoverText.getFormattedText(), false)) {
            hoverText.add(addHoverText);
        }
        if (hoverFont == null) { drawHoveringText(toHoverText(), mouseX, mouseY, fontRenderer); }
        else { GuiBasic.renderTooltipInternal(mouseX, ValueUtil.correctInt(mouseY, 16, height), this, hoverFont, hoverText, bgScale); }
    }

    protected List<String> toHoverText() {
        return hoverText.stream()
                .map(Component::getFormattedText)
                .collect(Collectors.toList());
    }

    public void setScreen(GuiScreen gui) {
        if (gui == null) {
            mc.displayGuiScreen(null);
            return;
        }
        ClientEvent.NextToGuiCustomNpcs event = new ClientEvent.NextToGuiCustomNpcs(NoppesUtilServer.getEditingNpc(player), this, gui);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.returnGui == null || event.isCanceled()) { return; }
        mc.displayGuiScreen(event.returnGui);
    }

    public void setSubGui(GuiScreen gui) {
        ClientEvent.SubGuiCustomNpcs event = new ClientEvent.SubGuiCustomNpcs(NoppesUtilServer.getEditingNpc(player), gui, wrapper.subgui);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) { return; }
        if (event.returnGui != null) { LogWriter.debug("Open SubGUI - " + event.returnGui.getClass() + "; In " + getClass().getSimpleName()); }
        else if (wrapper.subgui != null) {
            LogWriter.debug("Close SubGUI - " + wrapper.subgui.getClass() + "; In " + getClass().getSimpleName());
            subGuiClosed(wrapper.subgui);
        }
        wrapper.setSubgui(event.returnGui);
        initGui();
    }

    @Override
    public boolean hasSubGui() { return wrapper.subgui != null; }

    @Override
    public GuiScreen getSubGui() { return wrapper.getSubGui(); }

    @Override
    public int getWidth() { return width; }

    @Override
    public int getHeight() { return height; }

    @Override
    public GuiScreen getParent() { return wrapper.getParent(); }

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
            if (!hoverText.isEmpty()) { drawHover(wrapper.mouseX - guiLeft, wrapper.mouseY - guiTop); }
            hoverText.clear();
            return;
        }
        setHoverText(text, args);
        if (!hoverText.isEmpty()) {
            drawHover(wrapper.mouseX - guiLeft, wrapper.mouseY - guiTop);
            hoverText.clear();
        }
    }

    @Override
    public void drawWait() {
        if (minecraft.world == null) { return; }
        int x = scaledResolution.getScaledWidth() / 2;
        int y = scaledResolution.getScaledHeight() / 2 - 30;
        drawCenteredString(font, Component.translatable("gui.wait", "").getFormattedText(), width / 2, height / 2, CustomNpcs.MainColor.getRGB());
        int pos_0 = (int) Math.floor((double) (minecraft.world.getTotalWorldTime() % 16) / 2.0d);
        mc.getTextureManager().bindTexture(GuiBasic.INFO);
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        drawTexturedModalRect(x + GuiBasic.getPosX.apply(pos_0) - 1, y + GuiBasic.getPosY.apply(pos_0) - 1, 0, 12, 6, 6);
        int pos_1 = pos_0 - 1;
        if (pos_1 < 0) { pos_1 += 8; }
        drawTexturedModalRect(x + GuiBasic.getPosX.apply(pos_1), y + GuiBasic.getPosY.apply(pos_1), 6, 12, 5, 5);
        int pos_2 = pos_0 - 2;
        if (pos_2 < 0) { pos_2 += 8; }
        drawTexturedModalRect(x + GuiBasic.getPosX.apply(pos_2) + 1, y + GuiBasic.getPosY.apply(pos_2) + 1, 11, 12, 4, 4);
        GlStateManager.popMatrix();
    }

    @Override
    public void focusedNextComponent() { wrapper.focusedNextComponent(); }

    @Override
    public void focusedPrevComponent() { wrapper.focusedPrevComponent(); }
    
    public void drawNpc(Entity entity, int x, int y, float zoomed, int rotation, int vertical, int followCursor) {
        wrapper.drawNpc(entity, x, y, zoomed, rotation, vertical, followCursor, guiLeft, guiTop);
    }

    @Override
    public boolean doubleClicked(IComponentGui component) { return false; }

}
