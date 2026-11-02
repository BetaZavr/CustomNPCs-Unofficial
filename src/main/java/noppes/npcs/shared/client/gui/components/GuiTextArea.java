package noppes.npcs.shared.client.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.mixin.client.IMouseHandlerMixin;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.util.AreaUndoData;
import noppes.npcs.shared.client.gui.util.TextContainer;
import noppes.npcs.shared.client.gui.util.TextLineData;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.ITextChangeListener;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.shared.client.gui.util.TrueTypeFont;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class GuiTextArea
        extends AbstractWidget
        implements IComponentGui, GuiEventListener {

   protected static @Nonnull TrueTypeFont font = new TrueTypeFont(new Font("Arial Unicode MS", Font.PLAIN, CustomNpcs.FontSize), 1.0F);
   protected static final char colorChar = '\uffff';

   protected IGuiInterface listener;
   protected TextContainer container = null;
   protected boolean enableCodeHighlighting = false;
   protected long lastClicked = 0L;
   protected int cursorCounter;
   protected int startSelection;
   protected int endSelection;
   protected int cursorPosition;
   protected int scrolledLine = 0;

   public int id;
   public String text = null;
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
      if (prev instanceof ITextChangeListener textChanger) { textChanger.textUpdate(prev, prev.text); }
   }
   protected final List<Component> hoverText = new ArrayList<>();
   public boolean isYDE = false;

   public GuiTextArea(int idIn, int xIn, int yIn, int widthIn, int heightIn, String text) {
      super(xIn, yIn, widthIn, heightIn, Component.literal(text));
      id = idIn;
      setX(xIn);
      setY(yIn);
      width = widthIn;
      height = heightIn;
      undoing = true;
      setText(text);
      undoing = false;
      font.setSpecial(colorChar);
      setFGColor(0xFFE0E0E0);
   }

   public static GuiTextArea getActive() { return activeArea; }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      isHovered = false;
      if (!visible) { return; }
      isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
      PoseStack matrixStack = graphics.pose();
      // background
      matrixStack.pushPose();
      matrixStack.translate(getX() - 1, getY() - 1, 0.0f);
      if (isYDE) {
         matrixStack.scale(0.5f, 0.5f, 0.5f);
         graphics.fill(0, 0, width * 2 + 2, height * 2 + 2, 0xFFA0A0A0);
         graphics.fill(1, 1, width * 2 + 1, height * 2 + 1, 0xFF000000);
      }
      else {
         graphics.fill(0, 0, width + 2, height + 2, 0xFFA0A0A0);
         graphics.fill(0, 0, width + 1, height + 1, 0xFF000000);
      }
      matrixStack.popPose();
      container.visibleLines = height / container.lineHeight;
      int startBracket;
      if (clicked) {
         clicked = ((IMouseHandlerMixin) Minecraft.getInstance().mouseHandler).getActiveButton() == 0;
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
         clickScrolling = ((IMouseHandlerMixin) Minecraft.getInstance().mouseHandler).getActiveButton() == 0;
         startBracket = container.linesCount - container.visibleLines;
         scrolledLine = Math.min(Math.max((int)(1.0F * (float)startBracket * (float)(mouseY - getY()) / (float)height), 0), startBracket);
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
               e = getY() + 1 + (i - scrolledLine) * container.lineHeight;
               graphics.fill(getX() + 1 + yPos, e, getX() + 1 + posX, e + container.lineHeight + 1, 0x9900CC00);
            }
            if (endBracket >= data.start && endBracket < data.end) {
               yPos = font.width(line.substring(0, endBracket - data.start));
               posX = font.width(line.substring(0, endBracket - data.start + 1)) + 1;
               e = getY() + 1 + (i - scrolledLine) * container.lineHeight;
               graphics.fill(getX() + 1 + yPos, e, getX() + 1 + posX, e + container.lineHeight + 1, 0x9900CC00);
            }
         }
         if (i >= scrolledLine && i < scrolledLine + container.visibleLines) {
            if (wordHeightLight != null) {
               Matcher m = container.regexWord.matcher(line);
               while(m.find()) {
                  if (line.substring(m.start(), m.end()).equals(wordHeightLight)) {
                     posX = font.width(line.substring(0, m.start()));
                     e = font.width(line.substring(0, m.end())) + 1;
                     int posY = getY() + 1 + (i - scrolledLine) * container.lineHeight;
                     graphics.fill(getX() + 1 + posX, posY + 1, getX() + 1 + e, posY + container.lineHeight + 1, 0x99004C00);
                  }
               }
            }
            if (startSelection != endSelection && endSelection > data.start && startSelection <= data.end && startSelection < data.end) {
               yPos = font.width(line.substring(0, Math.max(startSelection - data.start, 0)));
               posX = font.width(line.substring(0, Math.min(endSelection - data.start, w))) + 1;
               e = getY() + 1 + (i - scrolledLine) * container.lineHeight;
               graphics.fill(getX() + 1 + yPos, e + 1, getX() + 1 + posX, e + container.lineHeight + 1, 0x990000FF);
            }
            yPos = getY() + (i - scrolledLine) * container.lineHeight + 1;
            font.draw(graphics.pose(), data.getFormattedString(container.makeup), (float)(getX() + 1), (float) yPos, getFGColor());
            if (activeArea == this && isEnabled() && cursorCounter / 8 % 2 == 0 && cursorPosition >= data.start && cursorPosition < data.end) {
               posX = getX() + font.width(line.substring(0, cursorPosition - data.start));
               graphics.fill(posX + 1, yPos + 1, posX + 2, yPos + 1 + container.lineHeight, 0xFFD0D0D0);
            }
         }
      }
      if (hasVerticalScrollbar()) {
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShaderTexture(0, GuiCustomScrollNop.resource);
         int sbSize = (int) Math.max((1.0f * container.visibleLines / container.linesCount * height), 2);
         int posX2 = getX() + width - 6;
         int posY3 = (int) ((getY() + 1.0f * scrolledLine / container.linesCount * (height - 4)) + 1);
         graphics.fill(posX2, posY3, posX2 + 5, posY3 + sbSize, 0xFFE0E0E0);
      }
      if (isYDE && isHovered && text != null && !text.isEmpty()) {
         try {
            String tr = Component.translatable(text).getString();
            if (!Util.instance.equalsDeleteColor(text, tr, false)) {
               if (tr.length() > 200) { tr = tr.substring(0, 200) + "..."; }
               hoverText.clear();
               for (String line : tr.split("\n")) { hoverText.add(Component.literal(line)); }
            }
         }
         catch (Exception ignored) {}
      }
      if (listener != null && isHovered && !hoverText.isEmpty()) { listener.setHoverText(hoverText); }
   }

   @Override
   public int[] getCenter() { return new int[] { getX() + width / 2, getY() + height / 2}; }

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
      xMouse -= getX() + 1;
      yMouse -= getY() + 1;
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

   @Override
   public boolean charTyped(char c, int i) {
      if (activeArea != this) { return false; }
      if (!isEnabled()) { return false; }
      if (SharedConstants.isAllowedChatCharacter(c)) { addText(Character.toString(c)); }
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!visible || !enabled || activeArea != this || !isFocused()) { return false; }
      if (activeArea == this && GuiBasic.isEscKey(keyCode)) {
         unfocus();
         return true;
      }
      if (Screen.isSelectAll(keyCode)) {
         int n = 0;
         cursorPosition = n;
         startSelection = n;
         endSelection = text.length();
         return true;
      } // select all
      int j;
      if (keyCode == InputConstants.KEY_LEFT) {
         j = 1;
         if (Screen.hasControlDown()) {
            Matcher m = container.regexWord.matcher(text.substring(0, cursorPosition));
            while (m.find()) {
               if (m.start() == m.end()) { continue; }
               j = cursorPosition - m.start();
            }
         }
         setCursor(cursorPosition - j, Screen.hasShiftDown());
         return true;
      } // left arrow
      if (keyCode == InputConstants.KEY_RIGHT) {
         j = 1;
         if (Screen.hasControlDown()) {
            Matcher m = container.regexWord.matcher(text.substring(cursorPosition));
            if ((m.find() && m.start() > 0) || m.find()) {
               j = m.start();
            }
         }
         setCursor(cursorPosition + j, Screen.hasShiftDown());
         return true;
      }// right arrow
      if (keyCode == InputConstants.getKey("key.keyboard.up").getValue()) {
         setCursor(cursorUp(), Screen.hasShiftDown());
         return true;
      } // up arrow
      if (keyCode == InputConstants.getKey("key.keyboard.down").getValue()) {
         setCursor(cursorDown(), Screen.hasShiftDown());
         return true;
      } // down arrow
      String select;
      if (Screen.isCut(keyCode)) {
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
      if (Screen.isCopy(keyCode)) {
         if (startSelection != endSelection) {
            NoppesStringUtils.setClipboardContents(text.substring(startSelection, endSelection));
         }
         return true;
      } // copy
      if (!enabled) { return false; }
      if (keyCode == InputConstants.getKey("key.keyboard.delete").getValue()) {
         select = getSelectionAfterText();
         if (!select.isEmpty() && startSelection == endSelection) { select = select.substring(1); }
         setText(getSelectionBeforeText() + select);
         cursorPosition = startSelection;
         endSelection = startSelection;
         return true;
      } // delete
      if (keyCode == InputConstants.KEY_BACKSPACE) {
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
      if (Screen.isPaste(keyCode)) {
         addText(NoppesStringUtils.getClipboardContents());
         return true;
      } // parse
      if (keyCode == InputConstants.KEY_Z && Screen.hasControlDown()) {
         if (!undoList.isEmpty()) {
            redoList.add(new AreaUndoData(text, cursorPosition, startSelection, endSelection, scrolledLine));
            setUndoData(undoList.remove(undoList.size() - 1));
         }
         return true;
      } // undo (Ctrl+Z)
      if (keyCode == InputConstants.KEY_Y && Screen.hasControlDown()) {
         if (!redoList.isEmpty()) {
            undoList.add(new AreaUndoData(text, cursorPosition, startSelection, endSelection, scrolledLine));
            if (undoList.size() > 100) { undoList.remove(0); }
            setUndoData(redoList.remove(redoList.size() - 1));
         }
         return true;
      } // redo (Ctrl+Y)
      if (keyCode == InputConstants.KEY_TAB) { addText("\t"); } // Tab
      if (GuiBasic.isEnterKey(keyCode)) { addText('\n' + getIndentCurrentLine()); } // Enter
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
            return getSelectionPos(getX() + 1 + font.width(data.text.substring(0, cursorPosition - data.start)),
                    getY() + 1 + (i - 1 - scrolledLine) * container.lineHeight);
         }
      }
      return 0;
   }

   private int cursorDown() {
      for(int i = 0; i < container.lines.size(); ++i) {
         TextLineData data = container.lines.get(i);
         if (cursorPosition >= data.start && cursorPosition < data.end) {
            return getSelectionPos(getX() + 1 + font.width(data.text.substring(0, cursorPosition - data.start)),
                    getY() + 1 + (i + 1 - scrolledLine) * container.lineHeight);
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
   protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }

   @Override
   public boolean mouseClicked(double xMouse, double yMouse, int mouseButton) {
      isHovered = xMouse >= (double)getX() && xMouse < (double)(getX() + width) && yMouse >= (double)getY() && yMouse < (double)(getY() + height);
      setFocused(isHovered);
      if (isHovered) {
         GuiTextFieldNop.unfocus();
         if (activeArea != null && activeArea != this) { unfocus(); }
         activeArea = this;
         startSelection = endSelection = cursorPosition = getSelectionPos(xMouse, yMouse);
         clicked = mouseButton == 0;
         doubleClicked = false;
         long time = System.currentTimeMillis();
         if (clicked && container.linesCount * container.lineHeight > height && xMouse > (double)(getX() + width - 8)) {
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
      else if (activeArea == this) { unfocus(); }
      return isHovered;
   }

   public void tick() { ++cursorCounter; }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
      if (activeArea == this && scrolled != 0.0D) {
         scrolledLine += scrolled > 0.0D ? -1 : 1;
         scrolledLine = Math.max(Math.min(scrolledLine, container.linesCount - height / container.lineHeight), 0);
         return true;
      }
      return false;
   }

   public void setText(String textIn) {
      textIn = textIn.replace("\r", "");
      if (text == null || !text.equals(textIn)) {
         if (listener instanceof ITextChangeListener textChanger) { textChanger.textUpdate(this, text); }
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

   @Override
   public void moveTo(int addX, int addY) {
      setX(getX() + addX);
      setY(getY() + addY);
   }

   public boolean hasVerticalScrollbar() {
      return container.visibleLines < container.linesCount;
   }

   public GuiTextArea enableCodeHighlighting() {
      enableCodeHighlighting = true;
      container.setLighting(true);
      return this;
   }

   public GuiTextArea setListener(IGuiInterface listenerIn) {
      listener = listenerIn;
      return this;
   }

   public boolean isActive() { return activeArea == this; }

   protected void updateWidgetNarration(@Nonnull NarrationElementOutput narrationElementOutput) { }

   // New from Unofficial (BetaZavr)
   @Override
   public GuiTextArea setHoverTexts(Object... components) {
      hoverText.clear();
      if (components == null) { return this; }
      Util.instance.putHovers(hoverText, components);
      return this;
   }

   @Override
   public GuiTextArea setCustomFont(ClientProxy.FontContainer fontIn) {
      if (fontIn != null && fontIn.getFont() != null) { font = fontIn.getFont(); }
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
      setFocused(isFocused);
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
   public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
      super.mouseDragged(mouseX, mouseY, mouseButton, dx, dy);
      return false;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
      super.mouseReleased(mouseX, mouseY, mouseButton);
      return false;
   }

   public GuiTextArea setColor(int color) {
      setFGColor(color);
      return this;
   }

}
