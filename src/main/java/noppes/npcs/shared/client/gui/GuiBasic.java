package noppes.npcs.shared.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
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
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.components.GuiMenuSideButton;
import noppes.npcs.shared.client.gui.components.GuiMenuTopButton;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GuiBasic extends GuiScreen implements IGuiInterface {

    protected static long altHTime = System.currentTimeMillis();
    public static Function<Integer, Integer> getPosX = (pos) -> {
        int v;
        switch (pos) {
            case 1:
            case 3: v = -11; break;
            case 2: v = -15; break;
            case 5:
            case 7: v = 11; break;
            case 6: v = 15; break;
            default: v = 0; break;
        }
        return v;
    };
    public static Function<Integer, Integer> getPosY = (pos) -> {
        int v;
        switch (pos) {
            case 0: v = -15; break;
            case 1:
            case 7: v = -11; break;
            case 3:
            case 5: v = 11; break;
            case 4: v = 15; break;
            default: v = 0; break;
        }
        return v;
    };
    public static boolean showHoverText = true;

    public EntityPlayerSP player;
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
    public static final ResourceLocation MONEY = new ResourceLocation(CustomNpcs.MODID, "textures/items/coin_gold.png");
    public static final ResourceLocation DONAT = new ResourceLocation(CustomNpcs.MODID, "textures/items/coin_donat.png");
    public static final ResourceLocation INFO = new ResourceLocation(CustomNpcs.MODID, "textures/gui/info.png");
    public static final ResourceLocation RESOURCE_SLOT = new ResourceLocation(CustomNpcs.MODID, "textures/gui/slot.png");
    @SuppressWarnings("unused")
    public static final ResourceLocation MENU_BUTTON = new ResourceLocation(CustomNpcs.MODID, "textures/gui/menubutton.png");
    @SuppressWarnings("unused")
    public static final ResourceLocation MENU_SIDE_BUTTON = new ResourceLocation(CustomNpcs.MODID, "textures/gui/menusidebutton.png");
    @SuppressWarnings("unused")
    public static final ResourceLocation MENU_TOP_BUTTON = new ResourceLocation(CustomNpcs.MODID, "textures/gui/menutopbutton.png");
    public static final ResourceLocation ANIMATION_BUTTONS = new ResourceLocation(CustomNpcs.MODID, "textures/gui/animation/buttons.png");
    public static final ResourceLocation ANIMATION_BUTTONS_SLOTS = new ResourceLocation(CustomNpcs.MODID, "textures/gui/animation/button_slots.png");
    public static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    public static final ResourceLocation YDE_BUTTONS = getResource("animation/yde_buttons.png");
    public static final ResourceLocation YDE_VERT_BUTTONS = getResource("animation/yde_vertical_buttons.png");

    // 3D compass
    public static final Map<String, ResourceLocation> TEXTURES_COMPASS = new HashMap<>();
    public static final ResourceLocation RESOURCE_COMPASS = new ResourceLocation(CustomNpcs.MODID + ":models/util/compass.obj");

    static {
        TEXTURES_COMPASS.put(CustomNpcs.MODID + ":util/compass", new ResourceLocation(CustomNpcs.MODID, "util/compass"));
        TEXTURES_COMPASS.put(CustomNpcs.MODID + ":util/task_0", new ResourceLocation(CustomNpcs.MODID, "util/task_0"));
    }

    protected final List<Component> hoverText = new ArrayList<>();
    protected ClientProxy.FontContainer hoverFont = null;
    protected ScaledResolution scaledResolution;
    public int widthTexture = 0;
    public int heightTexture = 0;
    public int borderTexture = 4;

    // standard
    protected FontRenderer font;
    protected int eventButton;
    protected long lastMouseEvent;
    protected int touchValue;
    public final Minecraft minecraft;

    public GuiBasic() {
        mc = Minecraft.getMinecraft();
        minecraft = mc;
        scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        font = (fontRenderer = mc.fontRenderer);
        player = mc.player;
        itemRender = mc.getRenderItem();
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
    public void initGui() {
        super.initGui();
        buttonList.clear();
        guiLeft = (width - imageWidth) / 2;
        guiTop = (height - imageHeight) / 2;
        scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        wrapper.initGui(mc, width, height);
        setFocused(!hasSubGui());
    }

    @Override
    public GuiWrapper getWrapper() { return wrapper; }

    @Override
    public void updateScreen() { wrapper.tick(); }

    @Override
    public void buttonEvent(GuiButtonNop button) { }

    @Override
    public boolean mouseButtonEvent(GuiButtonNop button, int mouseButton) { return false; }

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
        if (dWheel != 0) { mouseScrolled(mouseX, mouseY, dWheel / 120); }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
        return wrapper.mouseScrolled(mouseX, mouseY, scrolled);
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
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (wrapper.mouseReleased(mouseX, mouseY, mouseButton)) { return true; }
        super.mouseReleased((int) mouseX, (int) mouseY, mouseButton);
        return false;
    }

    @Override
    public void setFocused(boolean hasFocusedControlIn) {
        if (hasFocusedControlIn) { wrapper.initFocus(); }
        else { wrapper.setFocus(null); }
    }

    public IComponentGui getFocused() { return wrapper.getFocused(); }

    @Override
    public IComponentGui getLastFocused() { return wrapper.getLastFocused(); }

    @Override
    public void subGuiClosed(GuiScreen subgui) { }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException { keyPressed(typedChar, keyCode); }

    public boolean keyPressed(char typedChar, int keyCode) {
        if (wrapper.subgui == null) { checkAltH(); }
        if (isEscKey(keyCode)) {
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
    public void focusedNextComponent() { wrapper.focusedNextComponent(); }

    @Override
    public void focusedPrevComponent() { wrapper.focusedPrevComponent(); }

    @Override
    public void onClose() { wrapper.close(); }

    @Override
    public void onGuiClosed() { save(); }

    @Override
    public void add(IComponentGui element) {
        wrapper.add(element);
        if (element instanceof GuiTextArea) { ((GuiTextArea) element).setListener(this); }
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
    public void save() { }

    @Override
    public void preDrawScreen(int mouseX, int mouseY) { }

    @Override
    public void drawDefaultBackground() {
        if (drawDefaultBackground) { super.drawDefaultBackground(); }
        if (background != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate((float)guiLeft, (float) guiTop, 0.0F);
            GlStateManager.scale(bgScale, bgScale, bgScale);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(background);
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
                        drawTexturedModalRect(col * tileWidth,
                                row * tileHeight,
                                col == 0 ? 0 : col == maxCol - 1 ? uMax : uOffset,
                                row == 0 ? 0 : row == maxRow - 1 ? vMax : vOffset,
                                col == maxCol - 1 ? lastTileWidth : tileWidth,
                                row == maxRow - 1 ? lastTileHeight : tileHeight);
                    }
                }
            }
            else if (imageWidth > 256) {
                drawTexturedModalRect(0, 0, 0, 0, 250, imageHeight);
                drawTexturedModalRect(250, 0, 256 - (imageWidth - 250), 0, imageWidth - 250, imageHeight);
            }
            else { drawTexturedModalRect(0, 0, 0, 0, imageWidth, imageHeight); }
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        preDrawScreen(mouseX, mouseY);
        wrapper.mouseX = mouseX;
        wrapper.mouseY = mouseY;
        int x = hasSubGui() ? 0 : mouseX;
        int y = hasSubGui() ? 0 : mouseY;
        drawDefaultBackground();
        if (title != null && !title.getFormattedText().isEmpty()) {
            GuiButtonNop.renderString(title, guiLeft + 4, guiTop + 5, guiLeft + imageWidth - 8, guiTop + 15,
                    CustomNpcs.LableColor.getRGB(), false, true, null);
        }
        for (IComponentGui component : new ArrayList<>(wrapper.components)) { component.render(x, y, partialTicks); }
        super.drawScreen(x, y, partialTicks);
        if (wrapper.subgui != null) {
            GlStateManager.translate(0.0F, 0.0F, 60.0F);
            wrapper.subgui.drawScreen(mouseX, mouseY, partialTicks);
            GlStateManager.translate(0.0F, 0.0F, -60.0F);
        }
        else if (!hoverText.isEmpty() && (hoverIsGame || (CustomNpcs.ShowDescriptions && GuiBasic.showHoverText))) {
            if (!hoverIsGame) { hoverText.add(Component.translatable("hover.alt.h")); }
            if (hoverFont == null) { drawHoveringText(toHoverText(), mouseX, mouseY, fontRenderer); }
            else { renderTooltipInternal(mouseX, ValueUtil.correctInt(mouseY, 16, height), this, hoverFont, hoverText, bgScale); }
            hoverText.clear();
        }
    }

    public static void renderTooltipInternal(int mouseX, int mouseY, GuiScreen screen, ClientProxy.FontContainer font, List<Component> collections, float ignoredScale) {
        if (font != null && !collections.isEmpty()) {
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            int i = 0;
            for (Component s : collections) {
                int j = font.width(s);
                if (j > i) { i = j; }
            }
            int l1 = mouseX + 12;
            int i2 = mouseY - 12;
            int k = 8;
            if (collections.size() > 1) { k += 2 + (collections.size() - 1) * 10; }
            if (l1 + i > screen.width)  { l1 -= 28 + i; }
            if (i2 + k + 6 > screen.height) { i2 = screen.height - k - 6; }

            Minecraft.getMinecraft().getRenderItem().zLevel = 300.0F;
            int l = -267386864;
            drawGradientRect(l1 - 3, i2 - 4, l1 + i + 3, i2 - 3, 300.0d, l, l);
            drawGradientRect(l1 - 3, i2 + k + 3, l1 + i + 3, i2 + k + 4, 300.0d, l, l);
            drawGradientRect(l1 - 3, i2 - 3, l1 + i + 3, i2 + k + 3, 300.0d, l, l);
            drawGradientRect(l1 - 4, i2 - 3, l1 - 3, i2 + k + 3, 300.0d, l, l);
            drawGradientRect(l1 + i + 3, i2 - 3, l1 + i + 4, i2 + k + 3, 300.0d, l, l);
            int i1 = 1347420415;
            int j1 = 1344798847;
            drawGradientRect(l1 - 3, i2 - 3 + 1, l1 - 3 + 1, i2 + k + 3 - 1, 300.0d, i1, j1);
            drawGradientRect(l1 + i + 2, i2 - 3 + 1, l1 + i + 3, i2 + k + 3 - 1, 300.0d, i1, j1);
            drawGradientRect(l1 - 3, i2 - 3, l1 + i + 3, i2 - 3 + 1, 300.0d, i1, i1);
            drawGradientRect(l1 - 3, i2 + k + 2, l1 + i + 3, i2 + k + 3, 300.0d, j1, j1);
            for (int k1 = 0; k1 < collections.size(); ++k1) {
                font.draw(collections.get(k1).getParent(), (float)l1, (float)i2, -1);
                if (k1 == 0)  { i2 += 2; }
                i2 += 10;
            }
            Minecraft.getMinecraft().getRenderItem().zLevel = 0.0F;
            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableRescaleNormal();
        }
    }

    protected static void drawGradientRect(double left, double top, double right, double bottom, double zLevel, int startColor, int endColor) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(right, top, zLevel).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(left, top, zLevel).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(left, bottom, zLevel).color(f5, f6, f7, f4).endVertex();
        bufferbuilder.pos(right, bottom, zLevel).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    protected List<String> toHoverText() {
        return hoverText.stream()
                .map(Component::getFormattedText)
                .collect(Collectors.toList());
    }

    public FontRenderer getFontRenderer() { return font; }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    public void setScreen(GuiScreen gui) {
        ClientEvent.NextToGuiCustomNpcs event = new ClientEvent.NextToGuiCustomNpcs(NoppesUtilServer.getEditingNpc(player), this, gui);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) { return; }
        mc.displayGuiScreen(event.returnGui);
        if (mc.currentScreen == null) { mc.setIngameFocus(); }
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

    public void drawNpc(Entity entity, int x, int y, float zoomed, int rotation, int vertical, int followCursor) {
        wrapper.drawNpc(entity, x, y, zoomed, rotation, vertical, followCursor, guiLeft, guiTop);
    }

    @Override
    public int getWidth() { return width; }

    @Override
    public int getHeight() { return height; }

    public void openLink(String link) {
        try {
            Class<?> oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop").invoke(null);
            oclass.getMethod("browse", URI.class).invoke(object, new URI(link));
        }
        catch (Throwable t) { LogWriter.error(t); }
    }

    @Override
    public GuiScreen getParent() { return wrapper.getParent(); }

    // New fields from Unofficial (BetaZavr)
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
            if (!hoverText.isEmpty()) { drawHoveringText(toHoverText(), wrapper.mouseX - guiLeft, wrapper.mouseY - guiTop, fontRenderer); }
            hoverText.clear();
            return;
        }
        setHoverText(text, args);
        if (!hoverText.isEmpty()) {
            drawHoveringText(toHoverText(), wrapper.mouseX - guiLeft, wrapper.mouseY - guiTop, fontRenderer);
            hoverText.clear();
        }
    }

    public static boolean isInventoryKey(int key) {
        return Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode() == key;
    }

    public static boolean isUpKey(int key) {
        return key == Minecraft.getMinecraft().gameSettings.keyBindForward.getKeyCode() || key == Keyboard.KEY_UP;
    }

    public static boolean isDownKey(int key) {
        return key == Minecraft.getMinecraft().gameSettings.keyBindBack.getKeyCode() || key == Keyboard.KEY_DOWN;
    }

    public static boolean isEnterKey(int key) {
        return key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER;
    }

    public static boolean isEscKey(int key) { return key == Keyboard.KEY_ESCAPE; }

    public static void checkAltH() {
        if (altHTime < System.currentTimeMillis() &&
                (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) &&
                Keyboard.isKeyDown(Keyboard.KEY_H)) {
            altHTime = System.currentTimeMillis() + 1000L;
            showHoverText = !showHoverText;
        }
    }

    @Override
    public void drawWait() {
        if (minecraft.world == null) { return; }
        int x = scaledResolution.getScaledWidth() / 2;
        int y = scaledResolution.getScaledHeight() / 2 - 30;
        drawCenteredString(font, Component.translatable("gui.wait", "").getFormattedText(), width / 2, height / 2, CustomNpcs.MainColor.getRGB());
        int pos_0 = (int) Math.floor((double) (minecraft.world.getTotalWorldTime() % 16) / 2.0d);
        mc.getTextureManager().bindTexture(INFO);
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        drawTexturedModalRect(x + getPosX.apply(pos_0) - 1, y + getPosY.apply(pos_0) - 1, 0, 12, 6, 6);
        int pos_1 = pos_0 - 1;
        if (pos_1 < 0) { pos_1 += 8; }
        drawTexturedModalRect(x + getPosX.apply(pos_1), y + getPosY.apply(pos_1), 6, 12, 5, 5);
        int pos_2 = pos_0 - 2;
        if (pos_2 < 0) { pos_2 += 8; }
        drawTexturedModalRect(x + getPosX.apply(pos_2) + 1, y + getPosY.apply(pos_2) + 1, 11, 12, 4, 4);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean doubleClicked(IComponentGui component) { return false; }

}
