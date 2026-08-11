package noppes.npcs.shared.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.util.ResourceData;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.common.util.ComponentOrderComparator;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GuiCustomScrollNop extends Gui implements IComponentGui {

    public static final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/misc.png");
    // standard
    protected final Minecraft minecraft;
    protected final FontRenderer font;
    public int x = 0;
    public int y = 0;
    // main
    public int id;
    public int width;
    public int height;
    protected int hover = -1;
    protected Object listener;
    // data
    protected final List<Component> list = new ArrayList<>();
    protected final List<Integer> selectedList = new ArrayList<>();
    protected boolean selectable = true;
    protected int listSize = 0;
    protected int selected = -1;
    public boolean multipleSelection = false;
    // scroll vars
    protected final GuiTextFieldNop textField = new GuiTextFieldNop(null, 0, 1, 1, 175, 18, "");
    protected int listHeight = 0;
    protected int scrollY = 0;
    protected int maxScrollY;
    protected int scrollHeight = 0;
    protected boolean isSorted = true;
    protected boolean mouseInList = false;
    protected int lastClickedItem = -1;
    protected long lastClickedTime = 0L;
    protected boolean hasSearch = true;
    protected String searchStr = "";
    protected String[] searchWords = new String[0];
    protected boolean focused = false;
    protected boolean enabled = true;
    public boolean visible = true;

    // New from Unofficial (BetaZavr)
    protected final Map<Integer, List<Component>> hoversTexts = new TreeMap<>();
    protected final List<Component> ignoreSelected = new ArrayList<>();
    protected ClientProxy.FontContainer customFont = null;
    protected List<Component> hoverText = new ArrayList<>();
    protected List<Component> suffixes;
    protected List<ResourceData> prefixes;
    protected List<ItemStack> stacks = null;
    protected boolean isScrolling = false;
    protected boolean isSimpleSelect = false;
    protected float scaleX = 1.0f;
    protected float scaleY = 1.0f;
    public int lineHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 4;
    public int colorBackS = 0xC0101010;
    public int colorBackE = 0xE0101010;
    public int border = 0xFF000000;
    public int type = 0;

    public GuiCustomScrollNop(Object parent, int idIn) {
        id = idIn;
        width = 176;
        height = 159;
        listener = parent;
        minecraft = Minecraft.getMinecraft();
        font = minecraft.fontRenderer;
    }

    public GuiCustomScrollNop(Object parent, int id, boolean isMultipleSelection) {
        this(parent, id);
        multipleSelection = isMultipleSelection;
    }

    public GuiCustomScrollNop setSize(int x, int y) {
        textField.width = x - 2;
        height = y - textFieldHeight();
        width = x;
        listHeight = lineHeight * listSize;
        if (listHeight > 0) { scrollHeight = (int)((double)(height - 2) / (double)listHeight * (double)(height - 2)); }
        else { scrollHeight = Integer.MAX_VALUE; }
        maxScrollY = Math.max(0, listHeight - (height - 2) - 1);
        resetRoll();
        return this;
    }

    public GuiCustomScrollNop disabledSearch() {
        hasSearch = false;
        return this;
    }

    private int textFieldHeight() { return hasSearch ? 22 : 0; }

    private void reset() {
        if (searchWords.length == 0) { listSize = list.size(); }
        else { listSize = (int) list.stream().filter((line) -> isSearched(line.getString())).count(); }
        setSize(width, height + textFieldHeight());
        scrollY = ValueUtil.correctInt(scrollY, 0, maxScrollY);
        if (selected >= 0 && selected >= list.size()) { selected = -1; }
        selectedList.clear();
        lastClickedItem = -1;
    }

    private boolean isSearched(String s) {
        String line = s.toLowerCase();
        for (String k : searchWords) {
            if (!line.contains(k.toLowerCase())) { return false; }
        }
        return true;
    }

    public int getWidth() { return width; }

    public int getHeight() { return height + textFieldHeight(); }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!visible) { return; }
        if (hasSearch) {
            textField.x = x + 1;
            textField.y = y + 1;
            textField.render(mouseX, mouseY, partialTicks);
        }
        y += textFieldHeight();
        mouseInList = isMouseOver(mouseX, mouseY);

        // add elements
        boolean parentAllows = !(listener instanceof IGuiInterface) || !((IGuiInterface) listener).hasSubGui();
        if (parentAllows) {
            if (prefixes != null) { drawPrefixes(); }
            if (stacks != null) { drawStacks(); }
        }

        // background
        if (border != 0xFF000000) {
            GlStateManager.pushMatrix();
            int w = width + 1;
            int h = height + 1;
            float step = customFont != null ? 0.5f : 1.0f;
            GlStateManager.translate(x - step, y - step, 0.0f);
            if (customFont != null) {
                GlStateManager.scale(0.5f, 0.5f, 0.5f);
                w *= 2;
                h *= 2;
            }
            drawHorizontalLine(0, w, 0, border);
            drawHorizontalLine(0, w, h, border);
            drawVerticalLine(0, 0, h, border);
            drawVerticalLine(w, 0, h, border);
            GlStateManager.popMatrix();
        }
        if ((colorBackS >> 24 & 255) > 0 || (colorBackE >> 24 & 255) > 0) {
            int sx = x;
            int sy = y;
            int ex = width + x;
            int ey = height + y;
            drawGradientRect(sx, sy, ex, ey, colorBackS, colorBackE);
        }

        // draw scrolling
        if (scrollHeight < height - 2) {
            double xPos = mouseX - x;
            double yPos = mouseY - y;
            float color = isScrolling ? 0.5f : xPos >= width - 10 && xPos < width - 1 && yPos >= 1 && yPos < height - 2 ? 0.75f : 1.0f;
            drawScrollBar(color);
        }

        // positions:
        GlStateManager.pushMatrix();
        if (selectable) { hover = getMouseOver(mouseX, mouseY); }
        drawItems();
        GlStateManager.popMatrix();

        // scrolling pos
        if (scrollHeight < height - 2) {
            mouseY -= y;
            if (isScrolling) {
                isScrolling = Mouse.isButtonDown(0);
                if (isScrolling) {
                    scrollY = (int) ((mouseY - 1 - scrollHeight / 2.0d) * listHeight / (height - 2));
                    if (scrollY < 0) { scrollY = 0; }
                    if (scrollY > maxScrollY) { scrollY = maxScrollY; }
                }
            }
        }
        if (listener instanceof IGuiInterface) {
            IGuiInterface gui = (IGuiInterface) listener;
            if (mouseInList && !hoverText.isEmpty()) { gui.setHoverText(hoverText); }
            else if (hover >= 0 && hover < list.size() && parentAllows) {
                if (hoversTexts.containsKey(hover)) { gui.setHoverText(hoversTexts.get(hover)); }
                else if (stacks != null && hover < stacks.size() && minecraft != null) {
                    gui.setHoverText(stacks.get(hover).getTooltip(minecraft.player, minecraft.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL));
                }
            }
        }
        y -= textFieldHeight();
    }

    @Override
    public void tick() { textField.tick(); }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) { return false; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double mouseScrolled) {
        if (visible && isHovered() && mouseScrolled != 0.0D && mouseInList) {
            scrollY += mouseScrolled > 0 ? -lineHeight : lineHeight;
            if (scrollY > maxScrollY) { scrollY = maxScrollY; }
            if (scrollY < 0) { scrollY = 0; }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) { return false; }

    @Override
    public void moveTo(int addX, int addY) {
        x += addX;
        y += addY;
    }

    public void mouseForcedScrolled(double mouseScrolled) {
        if (visible && mouseScrolled != 0.0d && mouseInList) {
            scrollY += mouseScrolled > 0.0d ? -lineHeight : lineHeight;
            if (scrollY > maxScrollY) { scrollY = maxScrollY; }
            if (scrollY < 0) { scrollY = 0; }
        }
    }

    public boolean mouseInOption(int mouseX, int mouseY, int displayIndex) {
        int xOffset = scrollHeight < height - 2 ? 10 : 0;
        int posX = 4;
        int posY = lineHeight * displayIndex + 4 - scrollY;
        if (posY < 4 || posY + 10 > height) { return false; }
        return mouseX >= posX - 1 && mouseX < width - 2 - xOffset && mouseY >= posY - 3 && mouseY < posY + lineHeight - 2;
    }

    protected void drawItems() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int xOffset = listHeight < height - 2 ? 0 : 10;
        int displayIndex = 0;
        int alpha = 255 << 24;
        for(int i = 0; i < list.size(); ++i) {
            if (!isSearched(list.get(i).getString())) { continue; }
            int left = x + 3;
            int top = lineHeight * displayIndex + 4 - scrollY;
            ++displayIndex;
            if (top < 4 || top + 10 > height) { continue; }
            top += y;
            int r = left + width - 5 - xOffset;
            Component displayString = list.get(i) == null ? Component.literal("null") : list.get(i);
            //Component text;
            // add bz
            // add bz
            if ((stacks != null && i < stacks.size()) || (prefixes != null && i < prefixes.size())) { left += 10; }
            // main
            int right = r - 1;
            // add bz
            if (suffixes != null && i < suffixes.size() && suffixes.get(i) != null && !suffixes.get(i).getFormattedText().isEmpty()) {
                int w = 1 + (customFont != null ? customFont.width(suffixes.get(i).getFormattedText()) : font.getStringWidth(suffixes.get(i).getFormattedText()));
                right -= w;
                GuiButtonNop.renderString(suffixes.get(i), right, top, right + w, top + 10,
                        (i == hover ? CustomNpcs.HoverColor.getRGB() : CustomNpcs.MainColor.getRGB()), true, false, customFont);
            }
            if (multipleSelection && selectedList.contains(i) ||
                    isSimpleSelect && hover == i ||
                    !multipleSelection && selected == i) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(left - 2.0f, top - 3.0f, 0.0f);
                if (customFont != null) {
                    int c = border != 0xFF000000 ? border : -1;
                    GlStateManager.scale(0.5f, 0.5f, 0.5f);
                    r = (int) ((float) (r - left) * scaleX) + 1;
                    int h = (int) (lineHeight * 2.0f * scaleY) + 2;
                    drawRect(0, 3, r, h, (c & 0xFFFFFF) | 0x60000000);
                    drawVerticalLine(0, 3, h, c);
                    drawVerticalLine(r, 3, h, c);
                    drawHorizontalLine(0, r, 3, c);
                    drawHorizontalLine(0, r, h, c);
                }
                else {
                    drawVerticalLine(0, 0, lineHeight + 1, -1);
                    drawVerticalLine(r - left + 2, 0, lineHeight + 1, -1);
                    drawHorizontalLine(1, r - left + 2, 1, -1);
                    drawHorizontalLine(1, r - left + 2, lineHeight, -1);
                }
                GlStateManager.popMatrix();
                GuiButtonNop.renderString(displayString, left, top, right, top + 10,
                        (i == hover ? CustomNpcs.HoverColor.getRGB() : CustomNpcs.MainColor.getRGB()) | alpha, true, false, customFont);
            }
            else if (i == hover) {
                GuiButtonNop.renderString(displayString, left, top, right, top + 10, CustomNpcs.HoverColor.getRGB() | alpha, true, false, customFont);
            }
            else {
                GuiButtonNop.renderString(displayString, left, top, right, top + 10, CustomNpcs.MainColor.getRGB() | alpha, true, false, customFont);
            }
        }
    }

    public @Nonnull String getSelected() {
        return selected >= 0 && selected < list.size() ? list.get(selected).getString() : "";
    }

    public @Nonnull Component getNormalSelected() {
        return selected >= 0 && selected < list.size() ? list.get(selected) : Component.empty();
    }

    private int getMouseOver(int mouseX, int mouseY) {
        mouseX -= x;
        mouseY -= y;
        if (mouseX >= 4 && mouseX < width - 4 && mouseY >= 1 && mouseY < height - 2) {
            int displayIndex = 0;
            for(int index = 0; index < list.size(); ++index) {
                String line = list.get(index).getString();
                if (!isSearched(line)) { continue; }
                if (mouseInOption(mouseX, mouseY, displayIndex)) {
                    boolean isIgnore = false;
                    for (Component ignore : ignoreSelected) {
                        if (ignore.getString().equals(line)) {
                            isIgnore = true;
                            break;
                        }
                    }
                    if (!isIgnore) { return index; }
                }
                ++displayIndex;
            }
        }
        return -1;
    }

    @Override
    public int[] getCenter() { return new int[] { x + width / 2, y + height  / 2}; }

    @Override
    public List<Component> getHoversText() { return hoverText; }

    @Override
    public int getId() { return id; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public boolean isVisible() { return visible; }

    @Override
    public boolean isHovered() { return mouseInList; }

    @Override
    public boolean isFocused() { return getElementType().isSelectable() && focused; }

    @Override
    public boolean keyPressed(char typedChar, int keyCode) {
        if (!visible || !enabled) { return false; }
        if (hasSearch && textField.isFocused()) {
            boolean bo = textField.keyPressed(typedChar, keyCode);
            if (!searchStr.equals(textField.getValue())) {
                searchStr = textField.getValue().trim();
                searchWords = searchStr.split(" ");
                if (selected >= 0 && !isSearched(list.get(selected).getString())) { selected = -1; }
                scrollY = 0;
                reset();
            }
            return bo;
        }
        if (list.size() <= 1) { return false; }
        boolean canPressed = GuiTextFieldNop.getActive() == null;
        if (canPressed && listener instanceof IGuiInterface && !((IGuiInterface) listener).hasSubGui()) {
            canPressed = ((IGuiInterface) listener).getWrapper().onlyScroll == this || mouseInList;
        }
        if (canPressed) {
            if (keyCode == Keyboard.KEY_UP  || keyCode == minecraft.gameSettings.keyBindForward.getKeyCode()) { // up
                if (multipleSelection) { scrollY = ValueUtil.correctInt(scrollY - lineHeight, 0, maxScrollY); }
                else {
                    if (selected < 1) { return false; }
                    selected--;
                    resetRoll();
                    if (listener instanceof ICustomScrollListener) { ((ICustomScrollListener) listener).scrollClicked(this); }
                }
                return true;
            }
            else if (keyCode == Keyboard.KEY_DOWN || keyCode == minecraft.gameSettings.keyBindBack.getKeyCode()) { // down
                if (multipleSelection) { scrollY = ValueUtil.correctInt(scrollY + lineHeight, 0, maxScrollY); }
                else {
                    if (selected >= getList().size() - 1) { return false; }
                    selected++;
                    resetRoll();
                    if (listener instanceof ICustomScrollListener) { ((ICustomScrollListener) listener).scrollClicked(this); }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (!selectable || !visible) { return false; }
        if (hasSearch) { textField.mouseClicked(mouseX, mouseY, mouseButton); }
        if (scrollHeight < height - 2) {
            double xPos = mouseX - x;
            double yPos = mouseY - y;
            if (hasSearch) { yPos -= textFieldHeight(); }
            isScrolling = xPos >= width - 10 && xPos < width - 1 && yPos >= 1 && yPos < height - 2;
            if (isScrolling) { return true; }
        }
        if (mouseButton != 0 || hover < 0) { return false; }
        boolean clicked = true;
        if (multipleSelection) {
            if (selectedList.contains(hover)) { selectedList.removeIf(value -> value == hover); }
            else { selectedList.add(hover); }
        }
        else {
            clicked = selected != hover;
            selected = hover;
            hover = -1;
        }
        if (clicked && listener instanceof ICustomScrollListener) {
            if (isSimpleSelect) { ((ICustomScrollListener) listener).scrollDoubleClicked(this); }
            else { ((ICustomScrollListener) listener).scrollClicked(this); }
        }
        long time = System.currentTimeMillis();
        if (!isSimpleSelect && listener instanceof ICustomScrollListener &&
                selected >= 0 && selected == lastClickedItem && time - lastClickedTime < 500L) {
            ((ICustomScrollListener) listener).scrollDoubleClicked(this);
        }
        lastClickedTime = time;
        lastClickedItem = selected;
        return true;
    }

    private void drawScrollBar(float color) {
        minecraft.getTextureManager().bindTexture(resource);

        GlStateManager.pushMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0F);
        GlStateManager.translate(x + width - 10, y, 0.0f);
        int h0 = height / 2;
        int h1 = height - h0;
        drawTexturedModalRect(0, 0, 0, 0, 10, h0);
        drawTexturedModalRect(0, h0, 0, 256 - h1, 10, h1);
        GlStateManager.popMatrix();

        h0 = (scrollHeight - 1) / 2;
        h1 = scrollHeight - h0;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + width - 9.0f, y + (int) ((float) scrollY / (float) listHeight * (float)(height - 2)) + 1.0f, 0.0f);
        GlStateManager.color(color, color, color, 1.0F);
        drawTexturedModalRect(0, 0, 10, 0, 8, h0);
        drawTexturedModalRect(0, h0, 10, 256 - h1, 8, h1);
        GlStateManager.popMatrix();
    }

    public boolean hasSelected() {
        return selected >= 0;
    }

    public GuiCustomScrollNop setList(List<String> newList) {
        List<Component> list = new ArrayList<>();
        for (String line : newList) { list.add(Component.literal(line)); }
        setNormalList(list);
        return this;
    }

    public GuiCustomScrollNop setNormalList(List<Component> newList) {
        if (!isSameList(newList)) {
            isSorted = true;
            scrollY = 0;
            newList.sort(new ComponentOrderComparator());
            list.clear();
            list.addAll(newList);
            reset();
        }
        return this;
    }

    public GuiCustomScrollNop setUnsortedList(List<Component> newList) {
        if (isSameList(newList)) { return this; }
        isSorted = false;
        list.clear();
        list.addAll(newList);
        reset();
        return this;
    }

    private boolean isSameList(List<Component> checklist) {
        if (list.size() != checklist.size()) { return false; }
        List<String> main = new ArrayList<>();
        for (Component component : list) { main.add(component.getFormattedText()); }
        List<String> check = new ArrayList<>();
        for (Component component : checklist) { check.add(component.getFormattedText()); }
        for (int i = 0; i < check.size(); i++) {
            String line = main.get(i);
            if (!check.contains(line) || !check.get(i).equalsIgnoreCase(line)) { return false; }
        }
        return true;
    }


    public void replace(Component old, Component newLine) {
        int i = 0;
        for (Component line : new ArrayList<>(list)) {
            if (line.getString().equals(old.getString())) {
                list.remove(line);
                list.add(i, newLine);
                if (isSorted) { list.sort(new ComponentOrderComparator()); }
                reset();
                break;
            }
            i++;
        }
    }

    public void replace(String old, String newLine) { replace(Component.literal(old), Component.literal(newLine)); }

    public GuiCustomScrollNop setSelected(String line) {
        int i = 0;
        selected = -1;
        if (line != null && !line.isEmpty()) {
            for (Component l : list) {
                if (Util.instance.equalsDeleteColor(l.getFormattedText(), line, false)) {
                    selected = i;
                    break;
                }
                i++;
            }
        }
        return this;
    }

    public GuiCustomScrollNop setSelected(Component line) {
        if (list.contains(line)) { selected = list.indexOf(line); }
        else { setSelected(line == null ? "" : line.getFormattedText()); }
        return this;
    }

    public void clear() {
        list.clear();
        selected = -1;
        scrollY = 0;
        searchStr = "";
        searchWords = new String[0];
        textField.setValue("");
        reset();
    }

    public void clearSelection() {
        selectedList.clear();
        selected = -1;
    }

    public List<String> getList() {
        List<String> retList = new ArrayList<>();
        for (Component line : list) { retList.add(line.getFormattedText()); }
        return retList;
    }

    public List<Component> getSelectedList() {
        return IntStream.range(0, list.size())
                .filter(selectedList::contains)
                .mapToObj(list::get)
                .collect(Collectors.toList());
    }

    public GuiCustomScrollNop setSelectedList(HashSet<Component> newSelectedList) {
        int i = 0;
        selectedList.clear();
        for (Component line : list) {
            for (Component component : newSelectedList) {
                if (line.getString().equals(component.getString())) {
                    selectedList.add(i);
                    break;
                }
            }
            i++;
        }
        return this;
    }

    public GuiCustomScrollNop setSelectedList(Collection<String> newSelectedList) {
        int i = 0;
        selectedList.clear();
        for (Component line : list) {
            String sLine = line.getString();
            for (String str : newSelectedList) {
                if (sLine.equals(str)) {
                    selectedList.add(i);
                    break;
                }
            }
            i++;
        }
        return this;
    }

    public GuiCustomScrollNop setUnselectable() {
        selectable = false;
        return this;
    }

    public void scrollTo(String name) {
        int i = 0;
        for (Component line : list) {
            if (line.getString().equals(name)) {
                if (i >= 0 && scrollHeight < height - 2) {
                    int pos = (int)((float) i / (float)list.size() * (float)listHeight);
                    if (pos > maxScrollY) { pos = maxScrollY; }
                    scrollY = pos;
                }
                break;
            }
            i++;
        }
    }

    public void scrollTo(Component name) {
        if (name == null) { return; }
        int i = list.indexOf(name);
        if (i >= 0 && scrollHeight < height - 2) {
            int pos = (int) ((float) i / (float) listSize * (float) listHeight);
            if (pos > maxScrollY) { pos = maxScrollY; }
            scrollY = pos;
        }
    }

    public void resetRoll() {
        if (selected < 0 || selected >= list.size() || scrollHeight >= height - 2) { return; }
        if (!isSearched(list.get(selected).getString())) { return; }
        int displayIndex = 0;
        for (int i = 0; i < selected; ++i) {
            if (isSearched(list.get(i).getString())) { displayIndex++; }
        }
        int pos = lineHeight * displayIndex;
        if (pos < scrollY) { scrollY = pos; }
        else if (pos + lineHeight > scrollY + height - 2) { scrollY = pos + lineHeight - height + 2; }
        scrollY = ValueUtil.correctInt(scrollY, 0, maxScrollY);
    }

    public boolean isMouseOver(int xPos, int yPos) {
        return xPos >= x && xPos <= x + width && yPos >= y && yPos <= y + height;
    }

    public int getSelectedIndex() {
        return selected;
    }

    public void setSelectedIndex(int i) {
        selected = i < 0 ? -1 : i >= list.size() ? list.size() - 1 : i;
    }

    public GuiCustomScrollNop setSelected(int index) {
        if (index < 0) { selected = -1; }
        else if (index < list.size()) { selected = index; }
        return this;
    }

    public Map<Integer, List<Component>> getHoversTexts() { return hoversTexts; }

    public GuiCustomScrollNop setHoverTexts(LinkedHashMap<Integer, List<Component>> map) {
        hoversTexts.clear();
        if (map == null || map.isEmpty()) { return this; }
        hoverText.clear();
        hoversTexts.putAll(map);
        return this;
    }

    public GuiCustomScrollNop setStacks(List<ItemStack> newStacks) { stacks = newStacks; return this; }

    public GuiCustomScrollNop setSuffixes(List<Component> newSuffixes) { suffixes = newSuffixes; return this; }

    @SuppressWarnings("all")
    public GuiCustomScrollNop setPrefixes(List<ResourceData> newPrefixes) { prefixes = newPrefixes; return this; }

    private void drawStacks() {
        int displayIndex = 0;
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < list.size() && i < stacks.size(); ++i) {
            if (!isSearched(list.get(i).getString())) { continue; }
            int k = lineHeight * displayIndex + 4 - scrollY;
            displayIndex++;
            if (k < 4 || k + 10 > height) { continue; }
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0.0f);
            GlStateManager.translate(0, k - 2.5f, 300.0f);
            GlStateManager.scale(0.75f, 0.75f, 0.75f);
            minecraft.getRenderItem().renderItemAndEffectIntoGUI(stacks.get(i), 0, 0);
            GlStateManager.popMatrix();
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
    }

    private void drawPrefixes() {
        int size = Math.min(list.size(), prefixes.size());
        if (size == 0) { return; }
        int displayIndex = 0;
        for (int i = 0; i < list.size() && i < prefixes.size(); ++i) {
            if (!isSearched(list.get(i).getString())) { continue; }
            ResourceData rd = prefixes.get(i);
            int k = lineHeight * displayIndex + 4 - scrollY;
            displayIndex++;
            if (rd == null || rd.resource == null || rd.width <= 0 || rd.height <= 0) { continue; }
            if (k < 4 || k + 12 > height) { continue; }
            GlStateManager.pushMatrix();
            if (rd.isOBJ()) {
                GlStateManager.translate(x + 5.0f + rd.tW, y + k + 3.0f + rd.tH, rd.tD);
                if (rd.rotateX != 0.0f) { GlStateManager.rotate(rd.rotateX, 1.0f, 0.0f, 0.0f); }
                if (rd.rotateY != 0.0f) { GlStateManager.rotate(rd.rotateY, 0.0f, 1.0f, 0.0f); }
                if (rd.rotateZ != 0.0f) { GlStateManager.rotate(rd.rotateZ, 0.0f, 0.0f, 1.0f); }
                GlStateManager.scale(rd.scaleX, -rd.scaleY, rd.scaleZ);
                if (rd.modelOBJ == null) { rd.modelOBJ = ModelBuffer.getParameterizedModel(rd.resource, rd.visibleMeshes, rd.materialTextures, true, 0, false); }
                ModelBuffer.render(rd.modelOBJ);
            }
            else {
                boolean hasStack = stacks != null && !stacks.isEmpty() && i < stacks.size();
                GlStateManager.translate(x + (hasStack ? -13.0f : 0.5f) + rd.tW, y + k - 1.5f + rd.tH, rd.tD);
                float scale = 12.0f / (float) (Math.max(rd.width, rd.height));
                float scaleX = scale;
                float scaleY = scale;
                if (rd.scaleX != 0.0f || rd.scaleY != 0.0f) {
                    scaleX *= rd.scaleX;
                    scaleY *= rd.scaleY;
                    GlStateManager.translate(12.0f * rd.scaleX, 6.0f * rd.scaleY, 0.0f);
                }
                GlStateManager.scale(scaleX, scaleY, 1.0f);
                minecraft.getTextureManager().bindTexture(rd.resource);
                drawTexturedModalRect(0, 0, rd.u, rd.v, rd.width, rd.height);
            }
            GlStateManager.popMatrix();
        }
    }

    public int getHover() { return hover; }

    public List<Component> getNormalList() { return list; }

    @SuppressWarnings("unused")
    public boolean hasSearch() { return hasSearch; }

    @Override
    public GuiCustomScrollNop setHoverTexts(Object... components) {
        hoverText.clear();
        if (components == null) { return this; }
        Util.instance.putHovers(hoverText, components);
        return this;
    }

    @Override
    public GuiCustomScrollNop setIsEnabled(boolean isEnabled) {
        enabled = isEnabled;
        return this;
    }

    @Override
    public GuiCustomScrollNop setIsVisible(boolean isVisible) {
        visible = isVisible;
        if (!isVisible) { type = 0; }
        return this;
    }

    @Override
    public GuiCustomScrollNop setIsFocused(boolean isFocused) {
        focused = isFocused;
        return this;
    }

    @Override
    public GuiComponentType getElementType() { return GuiComponentType.SCROLL; }

    @Override
    public GuiCustomScrollNop setCustomFont(ClientProxy.FontContainer font) {
        customFont = font;
        textField.setCustomFont(font);
        lineHeight = font != null ? font.getHeight() + 4 : Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 4;
        reset();
        return this;
    }

    public GuiCustomScrollNop setPos(int xIn, int yIn) {
        x = xIn;
        y = yIn;
        return this;
    }

    public int getX() { return x; }

    public int getY() { return y; }

    public String getSearchValue() { return textField.getValue(); }

    @SuppressWarnings("unused")
    public void setSearchValue(String text) { textField.setValue(text); }

    @SuppressWarnings("unused")
    public GuiCustomScrollNop setIgnoreSelected(ArrayList<Component> list) {
        ignoreSelected.clear();
        if (list != null) { ignoreSelected.addAll(list); }
        return this;
    }

    public GuiCustomScrollNop setIsSimpleSelect(boolean isSimpleSelectIn) {
        isSimpleSelect = isSimpleSelectIn;
        return this;
    }

    public GuiCustomScrollNop setHoverScale(float x, float y) {
        scaleX = ValueUtil.correctFloat(x, 0.0f, 5.0f);
        scaleY = ValueUtil.correctFloat(y, 0.0f, 5.0f);
        return this;
    }

}
