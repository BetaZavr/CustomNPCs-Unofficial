package noppes.npcs.shared.client.gui.components;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.listeners.ITextChangeListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiTextFieldNop extends Gui implements IComponentGui {

    public static char[] filePath = new char[] { ':', '*', '?', '"', '<', '>', '&', '|' };

    // Standard
    protected final FontRenderer font;
    protected String text = "";
    protected int maxStringLength = 32;
    protected int cursorCounter;
    protected boolean enableBackgroundDrawing = true;
    protected boolean canLoseFocus = true;
    protected boolean focused = false;
    protected boolean enabled = true;
    protected boolean visible = true;
    protected int lineScrollOffset;
    protected int cursorPosition;
    protected int selectionEnd;
    protected int enabledColor = CustomNpcs.MainColor.getRGB();
    private GuiPageButtonList.GuiResponder guiResponder;
    private Predicate<String> validator = Predicates.alwaysTrue();
    public int id;
    public int x;
    public int y;
    public int width;
    public int height;

    // From mod
    private static GuiTextFieldNop activeTextfield = null;
    protected final String defaultValue;
    protected final List<Component> hoverText = new ArrayList<>();
    protected ClientProxy.FontContainer customFont = null;
    protected final int[] allowedSpecialKeyIDs = new int[] { 14, 211, 203, 205 };
    protected boolean isFileName = false;
    protected boolean latinAlphabetOnly = false;
    protected boolean allowUppercase = true;
    protected boolean numbersOnly = false;
    protected boolean doublesOnly = false;
    protected IGuiInterface listener;
    /**
     * 0: none; 1: full resource; 2: resource path; 3: resource domain
     */
    protected int resourceLocationType = 0;
    public char[] prohibitedSpecialChars = new char[] {};
    public boolean isHovered;
    public long min = 0;
    public long max = Integer.MAX_VALUE;
    public long def = 0;
    public double minD = 0.0d;
    public double maxD = Double.MAX_VALUE;
    public double defD = 0.0d;

    public GuiTextFieldNop(@Nullable IGuiInterface parent, int idIn, int xIn, int yIn, int widthIn, int heightIn, Object value) {
        // Standard
        id = idIn;
        font = Minecraft.getMinecraft().fontRenderer;
        x = xIn;
        y = yIn;
        width = widthIn;
        height = heightIn;
        // From mod
        setMaxStringLength(500);
        setValue(value);
        listener = parent;
        defaultValue = String.copyValueOf(getValue().toCharArray());
    }

    public static GuiTextFieldNop getActive() { return activeTextfield; }

    public static void unfocus() {
        GuiTextFieldNop prev = activeTextfield;
        activeTextfield = null;
        if (prev != null) { prev.unFocused(); }
    }

    public void setGuiResponder(GuiPageButtonList.GuiResponder guiResponderIn) { guiResponder = guiResponderIn; }

    public void setValue(@Nullable Object object) {
        setValue(object instanceof Component ? ((Component) object).getFormattedText() :
                object instanceof ITextComponent ? ((ITextComponent) object).getFormattedText() :
                    object == null ? "" : object.toString());
    }

    public void setValue(String textIn) {
        if (textIn == null) { text = ""; }
        else {
            String oldText = getValue();
            if (validator.apply(textIn)) {
                if (textIn.length() > maxStringLength) { text = textIn.substring(0, maxStringLength); }
                else  { text = textIn; }
                setCursorPositionEnd();
                if (listener instanceof ITextChangeListener && !oldText.equals(text)) { ((ITextChangeListener) listener).textUpdate(this, text); }
            }
        }
    }

    public String getValue() { return text; }

    public String getSelectedText()  { return text.substring(Math.min(cursorPosition, selectionEnd), Math.max(cursorPosition, selectionEnd)); }

    public void setValidator(Predicate<String> theValidator)  { validator = theValidator; }

    public void writeText(String textToWrite) {
        String s = "";
        String s1 = ChatAllowedCharacters.filterAllowedCharacters(textToWrite);
        int i = Math.min(cursorPosition, selectionEnd);
        int j = Math.max(cursorPosition, selectionEnd);
        int k = maxStringLength - text.length() - (i - j);
        if (!text.isEmpty()) { s = s + text.substring(0, i); }
        int l;
        if (k < s1.length()) {
            s = s + s1.substring(0, k);
            l = k;
        }
        else {
            s = s + s1;
            l = s1.length();
        }
        if (!text.isEmpty() && j < text.length())  { s = s + text.substring(j); }
        if (validator.apply(s)) {
            String oldText = text;
            text = s;
            moveCursorBy(i - selectionEnd + l);
            setResponderEntryValue(id, text);
            if (listener instanceof ITextChangeListener && !oldText.equals(text)) { ((ITextChangeListener) listener).textUpdate(this, text); }
        }
    }

    public void setResponderEntryValue(int idIn, String textIn) {
        if (guiResponder != null) { guiResponder.setEntryValue(idIn, textIn); }
    }

    public void deleteWords(int num) {
        if (!text.isEmpty()) {
            if (selectionEnd != cursorPosition) { writeText(""); }
            else { deleteFromCursor(getNthWordFromCursor(num) - cursorPosition); }
        }
    }

    public void deleteFromCursor(int num) {
        if (!text.isEmpty()) {
            if (selectionEnd != cursorPosition) { writeText(""); }
            else {
                boolean flag = num < 0;
                int i = flag ? cursorPosition + num : cursorPosition;
                int j = flag ? cursorPosition : cursorPosition + num;
                String s = "";
                if (i >= 0) { s = text.substring(0, i); }
                if (j < text.length()) { s = s + text.substring(j); }
                if (validator.apply(s)) {
                    String oldText = text;
                    text = s;
                    if (flag) { moveCursorBy(num); }
                    setResponderEntryValue(id, text);
                    if (listener instanceof ITextChangeListener && !oldText.equals(text)) { ((ITextChangeListener) listener).textUpdate(this, text); }
                }
            }
        }
    }

    @Override
    public int getId() { return id; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public boolean isVisible() { return visible; }

    public int getNthWordFromCursor(int numWords) { return getNthWordFromPos(numWords, getCursorPosition()); }

    public int getNthWordFromPos(int n, int pos) { return getNthWordFromPosWS(n, pos, true); }

    public int getNthWordFromPosWS(int n, int pos, boolean skipWs) {
        int i = pos;
        boolean flag = n < 0;
        int j = Math.abs(n);
        for (int k = 0; k < j; ++k) {
            if (!flag) {
                int l = text.length();
                i = text.indexOf(32, i);
                if (i == -1) { i = l; }
                else {
                    while (skipWs && i < l && text.charAt(i) == ' ') { ++i; }
                }
            }
            else {
                while (skipWs && i > 0 && text.charAt(i - 1) == ' ') { --i; }
                while (i > 0 && text.charAt(i - 1) != ' ') { --i; }
            }
        }
        return i;
    }

    public void moveCursorBy(int num) { setCursorPosition(selectionEnd + num); }

    public void setCursorPosition(int pos)  {
        cursorPosition = pos;
        int i = text.length();
        cursorPosition = MathHelper.clamp(cursorPosition, 0, i);
        setSelectionPos(cursorPosition);
    }

    public void setCursorPositionZero() { setCursorPosition(0); }

    public void setCursorPositionEnd() { setCursorPosition(text.length()); }

    @Override
    public boolean keyPressed(char typedChar, int keyCode) {
        if (!visible || !enabled || !focused) { return false; }
        if (isFocused() && GuiBasic.isEnterKey(keyCode)) { // Enter
            unFocused();
            return true;
        }
        if (latinAlphabetOnly && typedChar == ' ') { typedChar = '_'; }
        if (Keyboard.getKeyName(keyCode).startsWith("NUMPAD")) {
            typedChar = Keyboard.getKeyName(keyCode).replace("NUMPAD", "").charAt(0);
            try {
                int i = Integer.parseInt("" + typedChar);
                if (i == 0) { keyCode = Keyboard.KEY_0; }
                else { keyCode = i + 1; }
            }
            catch (NumberFormatException ignored) {  }
        }
        if (GuiScreen.isKeyComboCtrlA(keyCode)) {
            setCursorPositionEnd();
            setSelectionPos(0);
            return true;
        } // select all
        if (GuiScreen.isKeyComboCtrlC(keyCode)) {
            GuiScreen.setClipboardString(getSelectedText());
            return true;
        } // copy
        if (GuiScreen.isKeyComboCtrlV(keyCode)) {
            if (enabled) { writeText(GuiScreen.getClipboardString()); }
            return true;
        } // parse
        if (GuiScreen.isKeyComboCtrlX(keyCode)) {
            GuiScreen.setClipboardString(getSelectedText());
            if (enabled) { writeText(""); }
            return true;
        } // cut
        switch (keyCode) {
            case Keyboard.KEY_BACK: {
                if (enabled) {
                    if (GuiScreen.isCtrlKeyDown()) { deleteWords(-1); }
                    else { deleteFromCursor(-1); }
                }
                return true;
            } // backspace
            case Keyboard.KEY_HOME: {
                if (GuiScreen.isShiftKeyDown()) { setSelectionPos(0); }
                else { setCursorPositionZero(); }
                return true;
            }
            case Keyboard.KEY_LEFT: {
                if (GuiScreen.isShiftKeyDown()) {
                    if (GuiScreen.isCtrlKeyDown()) { setSelectionPos(getNthWordFromPos(-1, getSelectionEnd())); }
                    else { setSelectionPos(getSelectionEnd() - 1); }
                }
                else if (GuiScreen.isCtrlKeyDown()) { setCursorPosition(getNthWordFromCursor(-1)); }
                else { moveCursorBy(-1); }
                return true;
            }
            case Keyboard.KEY_RIGHT: {
                if (GuiScreen.isShiftKeyDown()) {
                    if (GuiScreen.isCtrlKeyDown()) { setSelectionPos(getNthWordFromPos(1, getSelectionEnd())); }
                    else { setSelectionPos(getSelectionEnd() + 1); }
                }
                else if (GuiScreen.isCtrlKeyDown()) { setCursorPosition(getNthWordFromCursor(1)); }
                else { moveCursorBy(1); }
                return true;
            }
            case Keyboard.KEY_END: {
                if (GuiScreen.isShiftKeyDown()) { setSelectionPos(text.length()); }
                else { setCursorPositionEnd(); }
                return true;
            }
            case Keyboard.KEY_DELETE: {
                if (enabled) {
                    if (GuiScreen.isCtrlKeyDown()) { deleteWords(1); }
                    else { deleteFromCursor(1); }
                }
                return true;
            }
            default: {
                if (enabled && charAllowed(typedChar, keyCode)) {
                    String preText = getValue();
                    writeText(Character.toString(typedChar));
                    return !preText.equals(getValue());
                }
            } // any symbol
        }
        return false;
    }

    protected boolean charAllowed(char typedChar, int keyCode) {
        for (char g : prohibitedSpecialChars) {
            if (g == typedChar) { return false; }
        }
        for (int j : allowedSpecialKeyIDs) {
            if (j == keyCode) { return true; }
        }
        boolean selectAll = getSelectedText().equals(text);
        if (isFileName) {
            for (char g : ChatAllowedCharacters.ILLEGAL_FILE_CHARACTERS) {
                if (g == typedChar) { return false; }
            }
        }
        if (numbersOnly) {
            return Character.isDigit(typedChar)
                    || (typedChar == '-' && (selectAll || getCursorPosition() == 0) && !text.contains("-"));
        }
        if (doublesOnly) {
            boolean hasDot = text.contains(".") || text.contains(",");
            return Character.isDigit(typedChar)
                    || (typedChar == '-' && (selectAll || getCursorPosition() == 0) && !text.contains("-"))
                    || ((typedChar == '.' || typedChar == ',') && (selectAll || !hasDot));
        }
        if (resourceLocationType != 0) {
            if (typedChar == ':' && (resourceLocationType != 1 || text.contains(":"))) { return false; }
            if (typedChar == '_' ||
                    typedChar == '-' ||
                    typedChar >= 'a' && typedChar <= 'z' ||
                    (!text.isEmpty() && typedChar >= '0' && typedChar <= '9') ||
                    typedChar == '.') {
                return true;
            }
            return (resourceLocationType == 1 || resourceLocationType == 2) && typedChar == '/';
        }
        if (!latinAlphabetOnly || Character.isLetterOrDigit(typedChar) || typedChar == '_') {
            return true;
        }
        return allowUppercase || Character.isLowerCase(typedChar);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (!enabled || !visible) { return false; }
        boolean clicked = mouseButton == 0 && clicked(mouseX, mouseY);
        if (clicked) { onClick(mouseX, mouseY); }
        setIsFocused(isHovered && clicked);
        return clicked;
    }

    protected boolean clicked(double mouseX, double mouseY) {
        return enabled && visible && mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + getHeight();
    }

    private void onClick(double mouseX, double ignoredMouseY) {
        int i = (int) Math.floor(mouseX) - getX();
        if (enableBackgroundDrawing) { i -= (customFont != null ? 2 : 4); }
        String value = getValue();
        String s = customFont != null ? customFont.getFont().trimStringToWidth(value.substring(lineScrollOffset), getWidth()) :
                font.trimStringToWidth(text.substring(lineScrollOffset), getWidth());
        setCursorPosition((customFont != null ? customFont.getFont().trimStringToWidth(s, i) :
                font.trimStringToWidth(s, i)).length() + lineScrollOffset);
    }

    public void unFocused() {
        if (numbersOnly) {
            if (isEmpty() || !isLong()) { setValue(def + ""); }
            else if (getLong() < min) { setValue(min + ""); }
            else if (getLong() > max) { setValue(max + ""); }
        }
        else if (doublesOnly) {
            if (isEmpty() || !isDouble()) { setValue(defD + ""); }
            else if (getDouble() < minD) { setValue(minD + ""); }
            else if (getDouble() > maxD) { setValue(maxD + ""); }
        }
        else if (resourceLocationType != 0) {
            if (isEmpty()) { setValue(defaultValue); }
        }
        if (listener instanceof ITextfieldListener) { ((ITextfieldListener) listener).unFocused(this); }
        if (this == activeTextfield) { activeTextfield = null; }
        focused = false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) { return false; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double mouseScrolled) {
        if (isHovered && (doublesOnly || numbersOnly) && mouseScrolled != 0) {
            if (doublesOnly) {
                double d = isDouble() ? getDouble() : defD;
                double step = Math.max((maxD - minD) / 100.0d, 0.001d);
                double t = d + (mouseScrolled > 0 ? step : -step);
                setValue("" + ValueUtil.correctDouble(Math.round(t * 1000.0d) / 1000.0d, minD, maxD));
            }
            else {
                long i = isLong() ? getLong() : def;
                long step = Math.max((max - min) / 100L, 1L);
                long t = i + (mouseScrolled > 0 ? step : -step);
                setValue("" + ValueUtil.correctLong(t, min, max));
            }
            if (listener instanceof ITextfieldListener) { ((ITextfieldListener) listener).unFocused(this); }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) { return false; }

    public void renderWidget(int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            if (getEnableBackgroundDrawing()) {
                drawRect(x - 1, y - 1, x + width + 1, y + height + 1, enabled && isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0);
                drawRect(x, y, x + width, y + height, enabled && isFocused() ? 0xFF181818 : 0xFF000000);
            }
            int color = getTextColor();
            int j = cursorPosition - lineScrollOffset;
            int k = selectionEnd - lineScrollOffset;
            String s = font.trimStringToWidth(text.substring(lineScrollOffset), getWidth());
            boolean flag = j >= 0 && j <= s.length();
            boolean flag1 = focused && cursorCounter / 6 % 2 == 0 && flag;
            int l = enableBackgroundDrawing ? x + 4 : x;
            int i1 = enableBackgroundDrawing ? y + (height - 8) / 2 : y;
            int j1 = l;
            if (k > s.length()) { k = s.length(); }
            if (!s.isEmpty()) {
                String s1 = flag ? s.substring(0, j) : s;
                j1 = font.drawStringWithShadow(s1, (float)l, (float)i1, color);
            }
            boolean flag2 = cursorPosition < text.length() || text.length() >= getMaxStringLength();
            int k1 = j1;
            if (!flag) { k1 = j > 0 ? l + width : l; }
            else if (flag2) {
                k1 = j1 - 1;
                --j1;
            }
            if (!s.isEmpty() && flag && j < s.length()) {
                font.drawStringWithShadow(s.substring(j), (float)j1, (float)i1, color);
            }
            if (flag1) {
                if (flag2) { Gui.drawRect(k1, i1 - 1, k1 + 1, i1 + 1 + font.FONT_HEIGHT, 0xFFD0D0D0); }
                else { font.drawStringWithShadow("_", (float)k1, (float)i1, color); }
            }
            if (k != j) {
                int l1 = l + font.getStringWidth(s.substring(0, k));
                drawSelectionBox(k1, i1 - 1, l1 - 1, i1 + 1 + font.FONT_HEIGHT);
            }
        }
    }

    public int getTextColor() {
        if (numbersOnly || doublesOnly) {
            if (numbersOnly && (!isLong() || getLong() < min || getLong() > max)) { return new Color(0xFC0345).getRGB(); }
            if (doublesOnly && (!isDouble() || getDouble() < minD || getDouble() > maxD)) { return new Color(0xFC0345).getRGB(); }
        }
        return enabled ? enabledColor : CustomNpcs.NotEnableColor.getRGB();
    }

    private void drawSelectionBox(int startX, int startY, int endX, int endY) {
        if (startX < endX) {
            int i = startX;
            startX = endX;
            endX = i;
        }
        if (startY < endY) {
            int j = startY;
            startY = endY;
            endY = j;
        }
        if (endX > x + width) { endX = x + width; }
        if (startX > x + width) { startX = x + width; }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.color(0.0F, 0.0F, 255.0F, 255.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.enableColorLogic();
        GlStateManager.colorLogicOp(GlStateManager.LogicOp.OR_REVERSE);
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferbuilder.pos(startX, endY, 0.0D).endVertex();
        bufferbuilder.pos(endX, endY, 0.0D).endVertex();
        bufferbuilder.pos(endX, startY, 0.0D).endVertex();
        bufferbuilder.pos(startX, startY, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.disableColorLogic();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public GuiTextFieldNop setMaxStringLength(int length) {
        maxStringLength = length;
        if (text.length() > length) {
            text = text.substring(0, length);
            if (listener instanceof ITextChangeListener) { ((ITextChangeListener) listener).textUpdate(this, text); }
        }
        return this;
    }

    public int getMaxStringLength() { return maxStringLength; }

    public int getCursorPosition() { return cursorPosition; }

    public boolean getEnableBackgroundDrawing() { return enableBackgroundDrawing; }

    public void setEnableBackgroundDrawing(boolean enableBackgroundDrawingIn) { enableBackgroundDrawing = enableBackgroundDrawingIn; }

    public void setTextColor(int color) { enabledColor = color; }

    @Override
    public boolean isFocused() { return getElementType().isSelectable() && focused; }

    public int getSelectionEnd() { return selectionEnd; }

    public void setSelectionPos(int position) {
        int i = text.length();
        position = ValueUtil.correctInt(position, 0, i);
        selectionEnd = position;
        if (font != null) {
            if (lineScrollOffset > i) { lineScrollOffset = i; }
            int j = getWidth();
            String s = font.trimStringToWidth(text.substring(lineScrollOffset), j);
            int k = s.length() + lineScrollOffset;
            if (position == lineScrollOffset) { lineScrollOffset -= font.trimStringToWidth(text, j, true).length(); }
            if (position > k) { lineScrollOffset += position - k; }
            else if (position <= lineScrollOffset) { lineScrollOffset -= lineScrollOffset - position; }
            lineScrollOffset = MathHelper.clamp(lineScrollOffset, 0, i);
        }
    }

    public void setCanLoseFocus(boolean canLoseFocusIn) { canLoseFocus = canLoseFocusIn; }

    // From mod
    public GuiTextFieldNop setIsFile(boolean bo) {
        isFileName = bo;
        return this;
    }

    public boolean isEmpty() { return text.trim().isEmpty(); }

    public int getInteger() { return Integer.parseInt(getValue()); }

    public long getLong() { return Long.parseLong(getValue()); }

    public float getFloat() { return Float.parseFloat(getValue()); }

    public double getDouble() { return Double.parseDouble(getValue()); }

    public boolean isInteger() {
        try {
            Integer.parseInt(getValue());
            return true;
        }
        catch (NumberFormatException ignored) { }
        return false;
    }

    public boolean isLong() {
        try {
            Long.parseLong(getValue());
            return true;
        }
        catch (NumberFormatException ignored) { }
        return false;
    }

    public boolean isFloat() {
        try {
            Float.parseFloat(getValue());
            return true;
        }
        catch (NumberFormatException ignored) { }
        return false;
    }

    public boolean isDouble() {
        try {
            Double.parseDouble(getValue());
            return true;
        }
        catch (NumberFormatException ignored) { }
        return false;
    }

    public GuiTextFieldNop setMinMaxDefault(long minValue, long maxValue, long defaultValue) {
        numbersOnly = true;
        doublesOnly = false;
        if (minValue > maxValue) {
            long i = minValue;
            minValue = maxValue;
            maxValue = i;
        }
        min = minValue;
        max = maxValue;
        def = defaultValue;
        return this;
    }

    public GuiTextFieldNop setMinMaxDefault(double minValue, double maxValue, double defaultValue) {
        numbersOnly = false;
        doublesOnly = true;
        if (minValue > maxValue) {
            double i = minValue;
            minValue = maxValue;
            maxValue = i;
        }
        minD = minValue;
        maxD = maxValue;
        defD = defaultValue;
        return this;
    }

    @SuppressWarnings("unused")
    public boolean isAllowUppercase() { return allowUppercase; }

    public GuiTextFieldNop setAllowUppercase(boolean isAllowUppercase) { allowUppercase = isAllowUppercase; return this; }

    // New from Unofficial (BetaZavr)
    /**
     * @param type - 0: none; 1: full resource; 2: resource path; 3: resource domain
     */
    public GuiTextFieldNop setResourceLocationType(int type) {
        resourceLocationType = ValueUtil.correctInt(type, 0, 3);
        return this;
    }

    public GuiTextFieldNop setColor(int color) {
        setTextColor(color);
        return this;
    }

    public GuiTextFieldNop setEditableIn(boolean editable) {
        enabled = editable;
        return this;
    }

    @SuppressWarnings("unused")
    public boolean isLatinAlphabetOnly() { return latinAlphabetOnly; }

    public GuiTextFieldNop setLatinAlphabetOnly(boolean isLatinAlphabetOnly) { latinAlphabetOnly = isLatinAlphabetOnly; return this; }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            isHovered = false;
            return;
        }
        isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        renderWidget(mouseX, mouseY, partialTicks);
        if (visible && isHovered && listener != null) {
            if (resourceLocationType != 0) {
                List<Component> hovers = new ArrayList<>(hoverText);
                hovers.add(Component.translatable("text.field.is.resource.location"));
                listener.setHoverText(hovers);
            }
            else if (Arrays.equals(prohibitedSpecialChars, filePath)) {
                List<Component> hovers = new ArrayList<>(hoverText);
                hovers.add(Component.translatable("text.field.is.fine.name"));
                listener.setHoverText(hovers);
            }
            else if (!hoverText.isEmpty()) { listener.setHoverText(hoverText); }
        }
    }

    @Override
    public List<Component> getHoversText() { return hoverText; }

    public boolean isHovered() { return isHovered; }

    @Override
    public int[] getCenter() { return new int[] { x + width / 2, y + height / 2}; }

    @Override
    public GuiTextFieldNop setHoverTexts(Object... components) {
        hoverText.clear();
        if (components == null) { return this; }
        Util.instance.putHovers(hoverText, components);
        return this;
    }

    @Override
    public GuiTextFieldNop setIsEnabled(boolean isEnable) {
        enabled = isEnable;
        return this;
    }

    @Override
    public GuiTextFieldNop setIsVisible(boolean bo) {
        visible = bo;
        return this;
    }

    @Override
    public GuiTextFieldNop setIsFocused(boolean isFocused) {
        if (canLoseFocus || isFocused) {
            boolean wasFocused = focused;
            focused = isFocused;
            if (isFocused) {
                cursorCounter = 0;
                activeTextfield = this;
            }
            else if (wasFocused) { unFocused(); }
        }
        return this;
    }

    @Override
    public GuiTextFieldNop setSize(int widthIn, int heightIn) {
        width = widthIn;
        height = heightIn;
        return this;
    }

    @Override
    public void moveTo(int addX, int addY) {
        x += addX;
        y += addY;
    }
    @Override
    public GuiTextFieldNop setCustomFont(ClientProxy.FontContainer font) {
        customFont = font;
        return this;
    }

    @Override
    public void tick() {
        ++cursorCounter;
    }

    public int getX() { return x; }

    public void setX(int xIn) { x = xIn; }

    public int getY() { return y; }

    public void setY(int yIn) { y = yIn; }

    public int getHeight() { return height; }

    public void setHeight(int heightIn) { height = heightIn; }

    public int getWidth() { return getEnableBackgroundDrawing() ? width - 8 : width; }

    public void setWidth(int widthIn) { width = widthIn; }

    @Override
    public GuiComponentType getElementType() { return GuiComponentType.TEXT_FIELD; }

    public ResourceLocation getResourceLocation() {
        ResourceLocation location = new ResourceLocation(NoppesUtilServer.validLocation(getValue()));
        if (!location.toString().equals(getValue())) { setValue(location.toString()); }
        return location;
    }

    public @Nonnull ResourceLocation getResourceLocation(@Nullable String domain) {
        if (domain == null || domain.isEmpty()) { domain = "minecraft"; }
        if (isEmpty()) { return new ResourceLocation(domain, "empty"); }
        String value = getValue();
        if (resourceLocationType == 2 || !value.contains(":")) {
            String path = NoppesUtilServer.validPath(value);
            if (!path.equals(getValue())) { setValue(path); }
            return new ResourceLocation(domain, path);
        }
        ResourceLocation location = new ResourceLocation(NoppesUtilServer.validLocation(value));
        if (!location.toString().equals(getValue())) { setValue(location.toString()); }
        return location;
    }

}
