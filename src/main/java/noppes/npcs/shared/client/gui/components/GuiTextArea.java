package noppes.npcs.shared.client.gui.components;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ChatAllowedCharacters;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.shared.client.gui.util.TrueTypeFont;
import noppes.npcs.shared.client.gui.util.AreaUndoData;
import noppes.npcs.shared.client.gui.listeners.ITextChangeListener;
import noppes.npcs.shared.client.gui.util.TextContainer;
import noppes.npcs.shared.client.gui.util.TextLineData;
import noppes.npcs.util.Util;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuiTextArea
        extends Gui
        implements IComponentGui {

    protected static TrueTypeFont font = new TrueTypeFont(new Font(CustomNpcs.FontType, Font.PLAIN, CustomNpcs.FontSize), 1.0f);
    protected static final char colorChar = '\uffff';

    protected IGuiInterface listener;
    protected TextContainer container = null;
    protected boolean enableCodeHighlighting = false;
    protected boolean focused;
    protected long lastClicked = 0L;
    protected int cursorCounter;
    protected int startSelection;
    protected int endSelection;
    protected int cursorPosition;
    protected int scrolledLine = 0;

    public int id;
    public int x;
    public int y;
    public int width;
    public int height;
    public int packedFGColor = 0xFFE0E0E0;
    public String text = null;
    public boolean isHovered;
    public boolean active = false;
    public boolean enabled = true;
    public boolean visible = true;
    public boolean clicked = false;
    public boolean doubleClicked = false;
    public boolean clickScrolling = false;
    public List<AreaUndoData> undoList = new ArrayList<>();
    public List<AreaUndoData> redoList = new ArrayList<>();
    public boolean undoing;

    // New from Unofficial (BetaZavr)
    private static GuiTextArea activeArea = null;
    public static void unfocus() {
        GuiTextArea prev = activeArea;
        activeArea = null;
        if (prev instanceof ITextChangeListener) { ((ITextChangeListener) prev).textUpdate(prev, prev.text); }
    }
    protected List<Component> hoverText = new ArrayList<>();
    public boolean isYDE = false;

    public GuiTextArea(int idIn, int xIn, int yIn, int widthIn, int heightIn, String text) {
        id = idIn;
        x = xIn;
        y = yIn;
        width = widthIn;
        height = heightIn;
        undoing = true;
        setText(text);
        undoing = false;
        font.setSpecial(colorChar);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        isHovered = false;
        if (!visible) { return; }
        isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, enabled && isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0);
        drawRect(x, y, x + width, y + height, isFocused() ? 0xFF181818 : 0xFF000000);
        container.visibleLines = height / container.lineHeight;
        int startBracket;
        if (clicked) {
            clicked = Mouse.isButtonDown(0);
            startBracket = getSelectionPos(mouseX, mouseY);
            if (startBracket != cursorPosition) {
                if (doubleClicked) {
                    startSelection = endSelection = cursorPosition;
                    doubleClicked = false;
                }
                setCursor(startBracket, true);
            }
        }
        else if (doubleClicked) { doubleClicked = false; }
        if (clickScrolling) {
            clickScrolling = Mouse.isButtonDown(0);
            startBracket = container.linesCount - container.visibleLines;
            scrolledLine = Math.min(Math.max((int)(1.0F * (float)startBracket * (float)(mouseY - y) / (float)height), 0), startBracket);
        }
        startBracket = 0;
        int endBracket = 0;
        if (endSelection - startSelection == 1 || startSelection == endSelection && startSelection < text.length()) {
            int found = getFound();
            if (found != 0) {
                startBracket = startSelection;
                endBracket = startSelection + found;
            }
        }
        List<TextLineData> list = new ArrayList<>(container.lines);
        String wordHeightLight = null;
        if (startSelection != endSelection) {
            Matcher m = container.regexWord.matcher(text);
            while(m.find()) {
                if (m.start() == startSelection && m.end() == endSelection) {
                    wordHeightLight = text.substring(startSelection, endSelection);
                }
            }
        }
        int i;
        for(i = 0; i < list.size(); ++i) {
            TextLineData data = list.get(i);
            String line = data.text;
            int w = line.length();
            int yPos;
            int posX;
            int e;
            if (startBracket != endBracket) {
                if (startBracket >= data.start && startBracket < data.end) {
                    yPos = font.width(line.substring(0, startBracket - data.start));
                    posX = font.width(line.substring(0, startBracket - data.start + 1)) + 1;
                    e = y + 1 + (i - scrolledLine) * container.lineHeight;
                    drawRect(x + 1 + yPos, e, x + 1 + posX, e + container.lineHeight + 1, -1728001024);
                }
                if (endBracket >= data.start && endBracket < data.end) {
                    yPos = font.width(line.substring(0, endBracket - data.start));
                    posX = font.width(line.substring(0, endBracket - data.start + 1)) + 1;
                    e = y + 1 + (i - scrolledLine) * container.lineHeight;
                    drawRect(x + 1 + yPos, e, x + 1 + posX, e + container.lineHeight + 1, -1728001024);
                }
            }
            if (i >= scrolledLine && i < scrolledLine + container.visibleLines) {
                if (wordHeightLight != null) {
                    Matcher m = container.regexWord.matcher(line);
                    while(m.find()) {
                        if (line.substring(m.start(), m.end()).equals(wordHeightLight)) {
                            posX = font.width(line.substring(0, m.start()));
                            e = font.width(line.substring(0, m.end())) + 1;
                            int posY = y + 1 + (i - scrolledLine) * container.lineHeight;
                            drawRect(x + 1 + posX, posY, x + 1 + e, posY + container.lineHeight + 1, -1728033792);
                        }
                    }
                }
                if (startSelection != endSelection && endSelection > data.start && startSelection <= data.end && startSelection < data.end) {
                    yPos = font.width(line.substring(0, Math.max(startSelection - data.start, 0)));
                    posX = font.width(line.substring(0, Math.min(endSelection - data.start, w))) + 1;
                    e = y + 1 + (i - scrolledLine) * container.lineHeight;
                    drawRect(x + 1 + yPos, e, x + 1 + posX, e + container.lineHeight + 1, -1728052993);
                }
                yPos = y + (i - scrolledLine) * container.lineHeight + 1;
                font.draw(data.getFormattedString(container.makeup), (float)(x + 1), (float) yPos, packedFGColor);
                if (active && isEnabled() && cursorCounter / 6 % 2 == 0 && cursorPosition >= data.start && cursorPosition < data.end) {
                    posX = x + font.width(line.substring(0, cursorPosition - data.start));
                    drawRect(posX + 1, yPos, posX + 2, yPos + 1 + container.lineHeight, -3092272);
                }
            }
        }
        if (hasVerticalScrollbar()) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(GuiCustomScrollNop.resource);
            int sbSize = (int) Math.max((1.0f * container.visibleLines / container.linesCount * height), 2);
            int posX2 = x + width - 6;
            int posY3 = (int) ((y + 1.0f * scrolledLine / container.linesCount * (height - 4)) + 1);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawRect(posX2, posY3, posX2 + 5, posY3 + sbSize, 0xFFE0E0E0);
        }
    }

    @Override
    public int[] getCenter() { return new int[] { x + width / 2, y + height / 2}; }

    @Override
    public List<Component> getHoversText() { return hoverText; }

    private int getFound() {
        char c = text.charAt(startSelection);
        int found = 0;
        if (c == '{') { found = findClosingBracket(text.substring(startSelection), '{', '}'); }
        else if (c == '[') { found = findClosingBracket(text.substring(startSelection), '[', ']'); }
        else if (c == '(') { found = findClosingBracket(text.substring(startSelection), '(', ')'); }
        else if (c == '}') { found = findOpeningBracket(text.substring(0, startSelection + 1), '{', '}'); }
        else if (c == ']') { found = findOpeningBracket(text.substring(0, startSelection + 1), '[', ']'); }
        else if (c == ')') { found = findOpeningBracket(text.substring(0, startSelection + 1), '(', ')'); }
        return found;
    }

    private int findClosingBracket(String str, char s, char e) {
        int found = 0;
        char[] chars = str.toCharArray();
        for(int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            if (c == s) { ++found; }
            else if (c == e) {
                --found;
                if (found == 0) { return i; }
            }
        }
        return 0;
    }

    private int findOpeningBracket(String str, char s, char e) {
        int found = 0;
        char[] chars = str.toCharArray();
        for(int i = chars.length - 1; i >= 0; --i) {
            char c = chars[i];
            if (c == e) { ++found; }
            else if (c == s) {
                --found;
                if (found == 0) { return i - chars.length + 1; }
            }
        }
        return 0;
    }

    private int getSelectionPos(double xMouse, double yMouse) {
        xMouse -= x + 1;
        yMouse -= y + 1;
        List<TextLineData> list = new ArrayList<>(container.lines);
        for(int i = 0; i < list.size(); ++i) {
            TextLineData data = list.get(i);
            if (i >= scrolledLine && i < scrolledLine + container.visibleLines) {
                int yPos = (i - scrolledLine) * container.lineHeight;
                if (yMouse >= (double)yPos && yMouse < (double)(yPos + container.lineHeight)) {
                    int lineWidth = 0;
                    char[] chars = data.text.toCharArray();
                    for(int j = 1; j <= chars.length; ++j) {
                        int w = font.width(data.text.substring(0, j));
                        if (xMouse < (double)(lineWidth + (w - lineWidth) / 2)) { return data.start + j - 1; }
                        lineWidth = w;
                    }
                    return data.end - 1;
                }
            }
        }
        return container.text.length();
    }

    @Override
    public int getId() { return id; }

    protected boolean charAllowed(char typedChar, int keyCode) {
        if (!active) { return false; }
        if (!isEnabled()) { return false; }
        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) { addText(Character.toString(typedChar)); }
        return true;
    }

    @Override
    public boolean keyPressed(char typedChar, int keyCode) {
        if (!visible || !enabled || !active) { return false; }
        if (GuiScreen.isKeyComboCtrlA(keyCode)) {
            int n = 0;
            cursorPosition = n;
            startSelection = n;
            endSelection = text.length();
            return true;
        } // select all
        int j;
        if (keyCode == Keyboard.KEY_LEFT) {
            j = 1;
            if (GuiScreen.isCtrlKeyDown()) {
                Matcher m = container.regexWord.matcher(text.substring(0, cursorPosition));
                while (m.find()) {
                    if (m.start() == m.end()) { continue; }
                    j = cursorPosition - m.start();
                }
            }
            setCursor(cursorPosition - j, GuiScreen.isShiftKeyDown());
            return true;
        } // left arrow
        if (keyCode == Keyboard.KEY_RIGHT) {
            j = 1;
            if (GuiScreen.isCtrlKeyDown()) {
                Matcher m = container.regexWord.matcher(text.substring(cursorPosition));
                if ((m.find() && m.start() > 0) || m.find()) {
                    j = m.start();
                }
            }
            setCursor(cursorPosition + j, GuiScreen.isShiftKeyDown());
            return true;
        }// right arrow
        if (keyCode == Keyboard.KEY_UP) {
            setCursor(cursorUp(), GuiScreen.isShiftKeyDown());
            return true;
        } // up arrow
        if (keyCode == Keyboard.KEY_DOWN) {
            setCursor(cursorDown(), GuiScreen.isShiftKeyDown());
            return true;
        } // down arrow
        String select;
        if (GuiScreen.isKeyComboCtrlX(keyCode)) {
            if (startSelection != endSelection) {
                NoppesStringUtils.setClipboardContents(text.substring(startSelection, endSelection));
                if (enabled) {
                    select = getSelectionBeforeText();
                    setText(select + getSelectionAfterText());
                    cursorPosition = startSelection = endSelection = select.length();
                }
            }
            return true;
        } // cut
        if (GuiScreen.isKeyComboCtrlC(keyCode)) {
            if (startSelection != endSelection) {
                NoppesStringUtils.setClipboardContents(text.substring(startSelection, endSelection));
            }
            return true;
        } // copy
        if (!enabled) { return false; }
        if (keyCode == Keyboard.KEY_DELETE) {
            select = getSelectionAfterText();
            if (!select.isEmpty() && startSelection == endSelection) { select = select.substring(1); }
            setText(getSelectionBeforeText() + select);
            cursorPosition = startSelection;
            endSelection = startSelection;
            return true;
        } // delete
        if (keyCode == Keyboard.KEY_BACK) {
            select = getSelectionBeforeText();
            if (startSelection > 0 && startSelection == endSelection) {
                select = select.substring(0, select.length() - 1);
                --startSelection;
            }
            setText(select + getSelectionAfterText());
            cursorPosition = startSelection;
            endSelection = startSelection;
            return true;
        } // backspace
        if (GuiScreen.isKeyComboCtrlV(keyCode)) {
            addText(NoppesStringUtils.getClipboardContents());
            return true;
        } // parse
        if (keyCode == Keyboard.KEY_Z && GuiScreen.isCtrlKeyDown()) {
            if (!undoList.isEmpty()) {
                redoList.add(new AreaUndoData(text, cursorPosition, startSelection, endSelection, scrolledLine));
                setUndoData(undoList.remove(undoList.size() - 1));
            }
            return true;
        } // undo (Ctrl+Z)
        if (keyCode == Keyboard.KEY_Y && GuiScreen.isCtrlKeyDown()) {
            if (!redoList.isEmpty()) {
                undoList.add(new AreaUndoData(text, cursorPosition, startSelection, endSelection, scrolledLine));
                if (undoList.size() > 100) { undoList.remove(0); }
                setUndoData(redoList.remove(redoList.size() - 1));
            }
            return true;
        } // redo (Ctrl+Y)
        if (keyCode == Keyboard.KEY_TAB) { addText("\t"); } // Tab
        if (GuiBasic.isEnterKey(keyCode)) {
            addText('\n' + getIndentCurrentLine());
        } // Enter
        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) { addText(Character.toString(typedChar)); }
        return true;
    }

    private void setUndoData(AreaUndoData data) {
        undoing = true;
        setText(data.text);
        undoing = false;
        cursorPosition = data.cursorPosition;
        startSelection = data.startSelection;
        endSelection = data.endSelection;
    }

    private String getIndentCurrentLine() {
        for (TextLineData data : container.lines) {
            if (cursorPosition > data.start && cursorPosition <= data.end) {
                int i = 0;
                while (i < data.text.length() && data.text.charAt(i) == ' ') { ++i; }
                return data.text.substring(0, i);
            }
        }
        return "";
    }

    private void setCursor(int i, boolean select) {
        i = Math.min(Math.max(i, 0), text.length());
        if (i != cursorPosition) {
            if (!select) { endSelection = startSelection = cursorPosition = i; }
            else {
                int diff = cursorPosition - i;
                if (cursorPosition == startSelection) { startSelection -= diff; }
                else if (cursorPosition == endSelection) { endSelection -= diff; }
                if (startSelection > endSelection) {
                    int j = endSelection;
                    endSelection = startSelection;
                    startSelection = j;
                }
                cursorPosition = i;
            }
        }
    }

    public void addText(String s) {
        if (s == null || s.isEmpty()) { return;}
        undoList.add(new AreaUndoData(text, cursorPosition, startSelection, endSelection, scrolledLine));
        if (undoList.size() > 100) { undoList.remove(0); }
        setText(getSelectionBeforeText() + s + getSelectionAfterText());
        endSelection = startSelection + s.length();
        cursorPosition = endSelection;
        startSelection = endSelection;
    }

    private int cursorUp() {
        for(int i = 0; i < container.lines.size(); ++i) {
            TextLineData data = container.lines.get(i);
            if (cursorPosition >= data.start && cursorPosition < data.end) {
                if (i == 0) { return 0; }
                return getSelectionPos(x + 1 + font.width(data.text.substring(0, cursorPosition - data.start)), y + 1 + (i - 1 - scrolledLine) * container.lineHeight);
            }
        }
        return 0;
    }

    private int cursorDown() {
        for(int i = 0; i < container.lines.size(); ++i) {
            TextLineData data = container.lines.get(i);
            if (cursorPosition >= data.start && cursorPosition < data.end) {
                return getSelectionPos(x + 1 + font.width(data.text.substring(0, cursorPosition - data.start)), y + 1 + (i + 1 - scrolledLine) * container.lineHeight);
            }
        }
        return text.length();
    }

    public String getSelectionBeforeText() {
        return startSelection == 0 ? "" : text.substring(0, Math.min(startSelection, text.length()));
    }

    public String getSelectionAfterText() {
        try { return text.substring(endSelection); }
        catch (Exception ignored) { }
        return text;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        active = mouseX >= (double)x && mouseX < (double)(x + width) && mouseY >= (double)y && mouseY < (double)(y + height);
        if (active) {
            startSelection = endSelection = cursorPosition = getSelectionPos(mouseX, mouseY);
            clicked = mouseButton == 0;
            doubleClicked = false;
            long time = System.currentTimeMillis();
            if (clicked && container.linesCount * container.lineHeight > height && mouseX > (double)(x + width - 8)) {
                clicked = false;
                clickScrolling = true;
            } else if (time - lastClicked < 500L) {
                doubleClicked = true;
                Matcher m = container.regexWord.matcher(text);
                while(m.find()) {
                    if (cursorPosition > m.start() && cursorPosition < m.end()) {
                        startSelection = m.start();
                        endSelection = m.end();
                        break;
                    }
                }
            }
            lastClicked = time;
        }
        return active;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double mouseScrolled) {
        if (active) {
            scrolledLine += (mouseScrolled > 0 ? -1 : 1);
            scrolledLine = Math.max(Math.min(scrolledLine, container.linesCount - height / container.lineHeight), 0);
            return true;
        }
        return false;
    }

    @Override
    public void tick() { ++cursorCounter; }

    public void setText(String textIn) {
        textIn = textIn.replace("\r", "");
        if (text == null || !text.equals(textIn)) {
            if (listener instanceof ITextChangeListener) { ((ITextChangeListener) listener).textUpdate(this, textIn); }

            if (!undoing) {
                undoList.add(new AreaUndoData(text, cursorPosition, startSelection, endSelection, scrolledLine));
                redoList.clear();
            }
            text = textIn;
            container = new TextContainer(text, font, width, height, enableCodeHighlighting);
            container.init();
            if (scrolledLine > container.linesCount - container.visibleLines) {
                scrolledLine = Math.max(0, container.linesCount - container.visibleLines);
            }
        }
    }

    public String getText() { return text; }

    public boolean isEnabled() { return enabled && visible; }

    @Override
    public boolean isVisible() { return visible; }

    public boolean isHovered() { return active; }

    @Override
    public void moveTo(int addX, int addY) {
        x += addX;
        y += addY;
    }

    public boolean hasVerticalScrollbar() { return container.visibleLines < container.linesCount; }

    public GuiTextArea enableCodeHighlighting() {
        enableCodeHighlighting = true;
        container.setLighting(true);
        return this;
    }

    public GuiTextArea setListener(IGuiInterface listenerIn) {
        listener = listenerIn;
        return this;
    }

    // New from Unofficial (BetaZavr)
    @Override
    public boolean isFocused() { return active; }

    @Override
    public GuiTextArea setHoverTexts(Object... components) {
        hoverText.clear();
        if (components == null) { return this; }
        Util.instance.putHovers(hoverText, components);
        return this;
    }

    @Override
    public GuiTextArea setIsEnabled(boolean isEnabled) {
        enabled = isEnabled;
        return this;
    }

    @Override
    public GuiTextArea setIsVisible(boolean isVisible) {
        visible = isVisible;
        return this;
    }

    @Override
    public GuiTextArea setIsFocused(boolean isFocused) {
        if (isFocused != focused) {
            focused = isFocused;
            active = isFocused;
            if (isFocused) { cursorCounter = 0; }
        }
        return this;
    }

    @Override
    public GuiTextArea setSize(int widthIn, int heightIn) {
        width = widthIn;
        height = heightIn;
        return this;
    }

    @Override
    public GuiComponentType getElementType() { return GuiComponentType.TEXT_AREA; }

    @Override
    public GuiTextArea setCustomFont(ClientProxy.FontContainer fontIn) {
        if (fontIn != null && fontIn.getFont() != null) { font = fontIn.getFont(); }
        return this;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) { return false; }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) { return false; }

    public int getX() { return x; }

    public void setX(int xIn) { x = xIn; }

    public int getY() { return y; }

    public void setY(int yIn) { y = yIn; }

    public int getHeight() { return height; }

    public void setHeight(int heightIn) { height = heightIn; }

    public int getWidth() { return width; }

    public void setWidth(int widthIn) { width = widthIn; }

    public GuiTextArea setColor(int color) {
        packedFGColor = color;
        return this;
    }

}