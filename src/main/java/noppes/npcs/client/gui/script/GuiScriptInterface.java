package noppes.npcs.client.gui.script;

import java.io.File;
import java.util.*;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.ClientScriptData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.SPacketScriptConsole;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextChangeListener;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;

public class GuiScriptInterface
        extends GuiNPCInterface
        implements IGuiData, ITextChangeListener, ICustomScrollListener {

   // Old in 1.12.2
   private static final String web_site = "http://www.kodevelopment.nl/minecraft/customnpcs/scripting";
   private static final String api_doc_site = "https://github.com/BetaZavr/CustomNPCsAPI-Unofficial";
   private static final String api_site = "https://github.com/BetaZavr/CustomNPCsAPI-Unofficial";
   private static final String dis_site = "https://discord.gg/RGb4JqE6Qz"; // https://github.com/Noppes/cnpcs-scripting-examples

   protected int activeTab = 0;
   public IScriptHandler handler;
   public List<String> methods = new ArrayList<>();
   public boolean showFunctions = false;

   // New from Unofficial (BetaZavr)
   protected final Map<Integer, Long> dataLog = new HashMap<>();
   protected final int type;
   protected Long selectLog = 0L;
   public Map<String, Map<String, Long>> languages = new HashMap<>();
   public String path;
   public String ext;

   public GuiScriptInterface(int typeIn) {
      super();
      drawDefaultBackground = true;
      imageWidth = 420;
      setBackground("menubg.png");

      type = typeIn;
      Packets.sendServer(new SPacketScriptGet(type));
   }

   @Override
   public void init() {
      imageWidth = (int) (width * 0.88D);
      imageHeight = (int) (height * 0.90D);
      super.init();
      guiTop += 10;
      int y = guiTop + 5;
      GuiMenuTopButton top = addTopButton(0, guiLeft + 4, guiTop - 17, "gui.settings");
      for(int i = 0; i < handler.getScripts().size(); ++i) {
         top = addTopButton(i + 1, top.getX() + top.getWidth(), top.getY(), "" + (i + 1));
      }
      if (handler.getScripts().size() < CustomNpcs.ScriptMaxTabs) {
         addTopButton(41, top.getX() + top.getWidth(), top.getY(), "+");
      }
      top = getTopButton(activeTab);
      if (top == null) {
         activeTab = 0;
         top = getTopButton(0);
      }
      top.setIsFocused(true);
      if (activeTab > 0) {
         ScriptContainer container = handler.getScripts().get(activeTab - 1);
         final GuiTextArea ta = new GuiTextArea(3, guiLeft + 5, y, imageWidth - 132, imageHeight - 10, container == null ? "" : container.script)
                 .enableCodeHighlighting()
                 .setListener(this);
         add(ta);
         int x = guiLeft + 7 + ta.getWidth();
         add(new GuiButtonNop(this, 99, showFunctions ? "script.hideFunctions" : "script.show.functions", x, y, (button) -> {
            showFunctions = !showFunctions;
            init();
         }).setSize(121, 20));
         if (!showFunctions) {
            boolean hasCode = container != null && !container.script.isEmpty();
            addButton(102, x, y += 22, "gui.clear")
                    .setSize(60, 20)
                    .setIsEnabled(hasCode);
            addButton(101, x + 61, y, "gui.paste")
                    .setSize(60, 20)
                    .setIsEnabled(!NoppesStringUtils.getClipboardContents().isEmpty());
            addButton(100, x, y + 21, "gui.copy")
                    .setSize(60, 20)
                    .setIsEnabled(hasCode);
            addButton(105, x + 61, y + 21, "gui.remove")
                    .setSize(60, 20);
            addButton(115, x, y + 43, "gui.remove.all")
                    .setSize(60, 20);
            Map<String, Long> map = languages.get(noppes.npcs.util.Util.instance.deleteColor(handler.getLanguage()));
            addButton(107, x + 61, y + 43, "script.loadscript")
                    .setSize(60, 20)
                    .setIsEnabled(map != null && !map.isEmpty());
            GuiCustomScrollNop scroll = addScroll(0).setPos(x, y = guiTop + 93)
                    .setUnselectable()
                    .disabledSearch()
                    .setSize(120, imageHeight - 120);
            if (container != null) { scroll.setList(container.scripts); }
            addButton(118, x, y + 2 + scroll.height, "gui.encrypt")
                    .setSize(80, 20)
                    .setIsEnabled(!(this instanceof GuiScriptClient) && container != null && container.canEncryptCode())
                    .setHoverTexts("encrypt.hover.encrypt");
         } // scripts
         else {
            int h = (imageHeight - 34) / 2;
            addScroll(1).setPos(x, guiTop + 26)
                    .setSize(120, h).setList(methods);
            addScroll(2).setPos(x, guiTop + 29 + h)
                    .setSize(120, h)
                    .setList(new ArrayList<>(ScriptContainer.DATA.keySet()));
         } // functions
      } // scripts
      else {
         GuiTextArea ta = new GuiTextArea(3, guiLeft + 5, y, imageWidth - 175, imageHeight - 10, getConsoleText());
         Map<Long, String> map = handler.getConsoleText();
         if (map.size() > 1) {
            ta.setY(ta.getY() + 24);
            ta.setHeight(ta.getHeight() - 24);
         }
         ta.enabled = false;
         add(ta);
         int x = guiLeft + 7 + ta.getWidth();
         addButton(100, x, guiTop + 125, map.size() < 2 ? Component.translatable("gui.copy") :
                 Component.translatable("gui.copy").append(" ").append(Component.translatable("gui.all")))
                 .setSize(80, 20)
                 .setHoverTexts("script.hover.log.copy.all");
         addButton(102, x, guiTop + 146, map.size() < 2 ? Component.translatable("gui.clear") :
                 Component.translatable("gui.clear").append(" ").append(Component.translatable("gui.all")))
                 .setSize(80, 20)
                 .setHoverTexts("script.hover.log.clear.all");
         if (map.size() > 1) {
            List<String> selects = new ArrayList<>();
            dataLog.clear();
            int i = 0;
            int pos = 0;
            for(Long key : map.keySet()) {
               dataLog.put(i, key);
               if (Objects.equals(key, selectLog)) { pos = i; }
               selects.add((i + 1) + "/" + map.size() + ": " + new Date(key));
               i++;
            }
            addButton(119, guiLeft + 4, guiTop + 7, true, pos, selects)
                    .setSize(ta.getWidth(), 20);
            getButton(100).setX(x + 82);
            getButton(102).setX(x + 82);
            addButton(120, x, guiTop + 125, "gui.copy")
                    .setSize(80, 20)
                    .setHoverTexts("script.hover.log.copy");
            addButton(121, x, guiTop + 146, "gui.clear")
                    .setSize(80, 20)
                    .setHoverTexts("script.hover.log.clear");
         }
         addLabel(1, x, guiTop + 15, "script.language");
         Object[] codeNames = languages.keySet().toArray(new Object[0]);
         GuiButtonNop button = addButton(103, x + 60, guiTop + 10, languages.size() > 1, getScriptIndex(), codeNames)
                 .setIsEnabled(languages.size() > 1)
                 .setSize(80, 20);
         String[] data = getLanguageData(noppes.npcs.util.Util.instance.deleteColor(button.getMessage().getString()));
         button.setHoverTexts(Component.translatable("script.hover.info." + data[0]).
                 append(Component.translatable("script.hover.info.dir", data[1], data[2])));
         addLabel(3, x + 145, guiTop + 15, "[?]")
                 .setHoverTexts("script.hover.info");
         addLabel(2, x, guiTop + 36, "gui.enabled");
         addYesNo(104, x + 60, guiTop + 31, handler.getEnabled())
                 .setSize(50, 20);
         if (player.getServer() != null) {
            addButton(106, x, guiTop + 55, "script.openfolder")
                    .setSize(150, 20);
         }
         addButton(109, x, guiTop + 78, "gui.website")
                 .setSize(80, 20);
         addButton(112, x + 82, guiTop + 78, "script.examples")
                 .setSize(80, 20);
         addButton(110, x, guiTop + 99, "script.apidoc")
                 .setSize(80, 20);
         addButton(111, x + 82, guiTop + 99, "script.apisrc")
                 .setSize(80, 20);
      } // info
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      if (button.id >= 0 && button.id <= CustomNpcs.ScriptMaxTabs) {
         setScript();
         activeTab = button.id;
         init();
      }
      else if (button.id >= CustomNpcs.ScriptMaxTabs + 1 && button.id < 100) {
         if (handler.getScripts().size() >= CustomNpcs.ScriptMaxTabs) { activeTab = CustomNpcs.ScriptMaxTabs; }
         else {
            handler.getScripts().add(new ScriptContainer(handler));
            activeTab = handler.getScripts().size();
         }
         init();
      }
      else {
         final ScriptContainer container = activeTab > 0 && activeTab <= handler.getScripts().size() ? handler.getScripts().get(activeTab - 1) : null;
         switch (button.id) {
            case 100: {
               if (activeTab == 0) { // copy all logs
                  Map<Long, String> map = handler.getConsoleText();
                  StringBuilder builder = new StringBuilder();
                  for (Map.Entry<Long, String> entry : map.entrySet()) { builder.insert(0, new Date(entry.getKey()) + entry.getValue() + "\n\n"); }
                  NoppesStringUtils.setClipboardContents(builder.toString());
               }
               else { NoppesStringUtils.setClipboardContents(get(3, GuiTextArea.class).getText()); } // copy code
               break;
            } // copy code or all logs
            case 101: {
               if (container != null) {
                  String code = container.script == null ? "" : container.script.replace(" ", "")
                          .replace("" + ((char) 9), "")
                          .replace("" + ((char) 10), "");
                  String pasteText = NoppesStringUtils.getClipboardContents().replace(" ", "")
                          .replace("" + ((char) 9), "")
                          .replace("" + ((char) 10), "");
                  if (!code.isEmpty() && !pasteText.isEmpty() && !code.equals(pasteText)) {
                     ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
                        if (agree) {
                           get(3, GuiTextArea.class).setText(NoppesStringUtils.getClipboardContents());
                           setScript();
                        }
                        NoppesUtil.openGUI(player, this);
                     },
                             Component.empty(),
                             Component.translatable("message.delete"));
                     setScreen(guiYesNo);
                  }
                  else {
                     get(3, GuiTextArea.class).setText(NoppesStringUtils.getClipboardContents());
                     setScript();
                  }
               }
               break;
            } // paste
            case 102: {
               if (activeTab > 0) {
                  if (container != null) {
                     ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
                        if (agree && activeTab > 0) { container.script = ""; }
                        NoppesUtil.openGUI(player, this);
                     }, Component.empty(), Component.translatable("message.delete"));
                     setScreen(guiYesNo);
                  }
               }
               else { handler.clearConsole(); }
               init();
               break;
            } // clear code or all logs
            case 103: {
               String language = noppes.npcs.util.Util.instance.deleteColor(button.getMessage().getString());
               handler.setLanguage(language);
               String[] data = getLanguageData(language);
               button.setHoverTexts(Component.translatable("script.hover.info." + data[0]).
                       append(Component.translatable("script.hover.info.dir", data[1], data[2])));
               break;
            } // languages
            case 104: handler.setEnabled(button.getValue() == 1); break; // script enabled
            case 105: {
               if (container != null && container.scripts.isEmpty() && container.script.isEmpty()) {
                  handler.getScripts().remove(activeTab -= 1);
                  init();
               }
               else {
                  ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
                     if (bo) { handler.getScripts().remove(activeTab -= 1); }
                     setScreen(this);
                  }, Component.empty(), Component.translatable("message.delete"));
                  setScreen(guiYesNo);
               }
               break;
            } // remove tab
            case 106: NoppesUtil.openFolder(ScriptController.Instance.dir); break; // script open folder
            case 107: {
               ScriptContainer cont = container;
               if (cont == null) { handler.getScripts().add(cont = new ScriptContainer(handler)); }
               setSubGui(new SubGuiScriptList(languages.get(noppes.npcs.util.Util.instance.deleteColor(handler.getLanguage())), cont));
               break;
            } // load scripts
            case 108: {
               if (container != null) { setScript(); }
               break;
            } // set script to tab
            case 109: {
               setScreen(new ConfirmLinkScreen((bo) -> {
                  if (bo) { Util.getPlatform().openUri(web_site); }
                  setScreen(this);
               }, web_site, true));
               break;
            } // web site
            case 110: {
               setScreen(new ConfirmLinkScreen((bo) -> {
                  if (bo) { Util.getPlatform().openUri(api_doc_site); }
                  setScreen(this);
               }, api_doc_site, true));
               break;
            } // api doc site
            case 111: {
               setScreen(new ConfirmLinkScreen((bo) -> {
                  if (bo) { Util.getPlatform().openUri(api_site); }
                  setScreen(this);
               }, api_site, true));
               break;
            } // api site
            case 112: {
               setScreen(new ConfirmLinkScreen((bo) -> {
                  if (bo) { Util.getPlatform().openUri(dis_site); }
                  setScreen(this);
               }, dis_site, true));
               break;
            } // discord site
            case 115: {
               ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
                  if (bo) {
                     handler.getScripts().clear();
                     activeTab = 0;
                  }
                  setScreen(this);
               }, Component.translatable("gui.remove.all"), Component.translatable("message.delete"));
               setScreen(guiYesNo);
               break;
            } // remove all codes
            case 118: {
               if (container != null) {
                  setScript();
                  setSubGui(new SubGuiScriptEncrypt(path, ext));
               }
               break;
            } // encrypt
            case 119: {
               GuiTextArea area = get(3, GuiTextArea.class);
               if (activeTab == 0 && area != null &&
                       dataLog.containsKey(button.getValue()) &&
                       handler.getConsoleText().containsKey(dataLog.get(button.getValue()))) {
                  selectLog = dataLog.get(button.getValue());
                  area.setText(new Date(selectLog) + handler.getConsoleText().get(selectLog));
               }
               break;
            } // log tab
            case 120: {
               if (activeTab == 0) { NoppesStringUtils.setClipboardContents(get(3, GuiTextArea.class).getText()); }
               break;
            } // copy log
            case 121: {
               if (activeTab == 0) {
                  handler.clearConsoleText(selectLog);
                  init();
               }
               break;
            } // clear log
         }
      }
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      ListTag languagesList = compound.getList("Languages", 10);
      Map<String, Map<String, Long>> newLanguages = new HashMap<>();
      for(int i = 0; i < languagesList.size(); ++i) {
         CompoundTag comp = languagesList.getCompound(i);
         Map<String, Long> scripts = new TreeMap<>();
         ListTag list = comp.getList("Scripts", 8);
         long[] ld = new long[list.size()];
         if (comp.contains("sizes", 12)) { ld = comp.getLongArray("sizes"); }
         for (int j = 0; j < list.size(); ++j) { scripts.put(list.getString(j), ld[j]); }
         newLanguages.put(comp.getString("Language"), scripts);
         if (noppes.npcs.util.Util.instance.equalsDeleteColor(comp.getString("Language"), handler.getLanguage(), false)) {
            ext = comp.getString("FileSfx");
         }
      }
      languages = newLanguages;
      path = compound.getString("DirPath") + "/" + handler.getLanguage().toLowerCase();
      methods = NBTTags.getStringList(compound.getList("Methods", 10));
      init();
   }

   @Override
   public void save() { setScript(); }

   @Override
   public void textUpdate(IComponentGui component, String text) {
      ScriptContainer container = handler.getScripts().get(activeTab - 1);
      if (container != null) { container.script = text; }
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) { }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { get(3, GuiTextArea.class).addText(scroll.getSelected()); }

   private int getScriptIndex() {
      int i = 0;
      for(Iterator<String> var2 = languages.keySet().iterator(); var2.hasNext(); ++i) {
         String language = var2.next();
         if (language.equalsIgnoreCase(handler.getLanguage())) { return i; }
      }
      return 0;
   }

   private void setScript() {
      if (activeTab > 0) {
         ScriptContainer container = handler.getScripts().get(activeTab - 1);
         if (container == null) { handler.getScripts().add(container = new ScriptContainer(handler)); }
         String text = get(3, GuiTextArea.class).getText();
         text = text.replace("\r\n", "\n");
         text = text.replace("\r", "\n");
         container.script = text;
      }
   }

   // New from Unofficial (BetaZavr)
   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (player.level().getGameTime() % 5 == 0) {
         if (activeTab > 0) {
            ScriptContainer container = handler.getScripts().get(activeTab - 1);
            boolean e = container == null || container.script.isEmpty();
            GuiButtonNop button = getButton(100);
            if (button != null) {
               if (button.isEnabled() && e) { button.setIsEnabled(false); }
               else if (!button.isEnabled() && !e) { button.setIsEnabled(true); }
            } // copy
            button = getButton(102);
            if (button != null) {
               if (button.isEnabled() && e) { button.setIsEnabled(false); }
               else if (!button.isEnabled() && !e) { button.setIsEnabled(true); }
            } // clear
            button = getButton(107);
            if (button != null) {
               Map<String, Long> map = languages.get(noppes.npcs.util.Util.instance.deleteColor(handler.getLanguage()));
               e = map != null && !map.isEmpty();
               if (button.isEnabled() && !e) { button.setIsEnabled(false); }
               else if (!button.isEnabled() && e) { button.setIsEnabled(true); }
            } // files
            button = getButton(101);
            if (button != null) {
               try {
                  e = !NoppesStringUtils.getClipboardContents().isEmpty();
                  if (button.isEnabled() && !e) { button.setIsEnabled(false); }
                  else if (!button.isEnabled() && e) { button.setIsEnabled(true); }
               }
               catch (Exception ee) { LogWriter.error(ee); }
            } // paste
         }
         else {
            boolean e = handler == null || handler.getConsoleText().isEmpty();
            GuiButtonNop button = getButton(100);
            if (button != null) {
               if (button.isEnabled() && e) { button.setIsEnabled(false); }
               else if (!button.isEnabled() && !e) { button.setIsEnabled(true); }
            } // copy
            button = getButton(102);
            if (button != null) {
               if (button.isEnabled() && e) { button.setIsEnabled(false); }
               else if (!button.isEnabled() && !e) { button.setIsEnabled(true); }
            } // clear
         }
      }
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public void subGuiClosed(Screen subgui) {
      if (!(this instanceof GuiScriptClient) && subgui instanceof SubGuiScriptEncrypt gui && gui.send) {
         save();
         CompoundTag nbt = new CompoundTag();
         nbt.putString("Name", gui.getTextField(0).getValue() + gui.ext);
         String p = path.replaceAll("\\\\", "/") + "/" + nbt.getString("Name");
         if (p.contains("/scripts")) { p = p.substring(p.indexOf("/scripts") + 8); }
         nbt.putString("Path", p);
         nbt.putInt("Tab", activeTab - 1);
         nbt.putBoolean("OnlyTab", gui.onlyTab);
         Packets.sendServer(new SPacketScriptEncrypt(type, nbt));
         setScreen(null);
      }
   }

   private static String[] getLanguageData(String name) {
      String key = "ecmascript";
      String ext = ".js";
      if (name.toLowerCase().startsWith("graaljs")) { key = "graaljs"; }
      else if (name.toLowerCase().startsWith("luaj")) { key = "lua"; ext = ".lua"; }
      else if (name.toLowerCase().startsWith("jython")) { key = "jython"; ext = ".py"; }
      else if (name.toLowerCase().startsWith("jruby")) { key = "jruby"; ext = ".rb"; }
      else if (name.toLowerCase().startsWith("groovy")) { key = "groovy"; ext = ".groovy"; }
      else if (name.toLowerCase().startsWith("kotlin")) { key = "kotlin"; ext = ".kt"; }
      else if (name.toLowerCase().startsWith("rhino")) { key = "rhino"; }
      String dir = "./(world_name)/customnpcs/scripts/" + name.toLowerCase();
      if (CustomNpcs.Server != null) {
         File levelDir = CustomNpcs.getLevelSaveDirectory();
         if (levelDir != null) {
            dir = levelDir.getAbsolutePath().replace("\\", "/");
            if (dir.lastIndexOf("/./") != -1) { dir = dir.substring(dir.lastIndexOf("/./") + 1); }
            dir += "/scripts/" + name.toLowerCase();
         }
      }
      return new String[] { key, dir, ext };
   }

   private String getConsoleText() {
      Map<Long, String> map = handler.getConsoleText();
      if (!map.containsKey(selectLog)) {
         for (long time : map.keySet()) { if (selectLog < time) { selectLog = time; } }
      }
      return map.containsKey(selectLog) ? new Date(selectLog) + map.get(selectLog) : "";
   }

   public void setTabScript(int tab, String script) {
      ScriptContainer container = handler.getScripts().get(tab);
      if (container != null) {
         container.script = script;
         init();
      }
   }

   public void setTabConsole(int tab, long time, String log) {
      ScriptContainer container = handler.getScripts().get(tab);
      if (container != null) {
         container.console.put(time, log);
         init();
      }
   }

   protected void sendToServer(CompoundTag compound) {
      // collect and clear scripts and consoles
      Map<Integer, List<String>> mapScripts = new TreeMap<>();
      Map<Integer, Map<Long, List<String>>> mapConsoles = new TreeMap<>();
      ListTag scripts = compound.getList("Scripts", 10);
      for (int i = 0; i < scripts.size(); i++) {
         CompoundTag scriptNbt = scripts.getCompound(i);
         // Script
         if (scriptNbt.contains("Script", 8)) { mapScripts.put(i, noppes.npcs.util.Util.instance.getStringData(scriptNbt.getString("Script"))); }
         else {
            mapScripts.put(i, new ArrayList<>());
            ListTag list = scriptNbt.getList("Script", 8);
            for (int k = 0; k < list.size(); k++) { mapScripts.get(i).add(list.getString(k)); }
         }
         scriptNbt.put("Script", new ListTag());
         // Console
         ListTag consoles = scriptNbt.getList("Console", 10);
         for (int j = 0; j < consoles.size(); j++) {
            if (!mapConsoles.containsKey(i)) { mapConsoles.put(i, new LinkedHashMap<>()); }
            CompoundTag errorNbt = consoles.getCompound(j);
            long time = errorNbt.getLong("Long");
            if (errorNbt.contains("String", 8)) {
               mapConsoles.get(i).put(time, noppes.npcs.util.Util.instance.getStringData(errorNbt.getString("String")));
            }
            else {
               mapConsoles.get(i).put(time, new ArrayList<>());
               ListTag list = errorNbt.getList("String", 8);
               for (int k = 0; k < list.size(); k++) { mapConsoles.get(i).get(time).add(list.getString(k)); }
            }
            errorNbt.put("String", new ListTag());
         }
      }
      Packets.sendServer(new SPacketScriptSave(type, compound));
      for (int tab : mapScripts.keySet()) {
         List<String> scriptStrings = mapScripts.get(tab);
         int i = 0;
         for (String part : scriptStrings) {
            Packets.sendServer(new SPacketScriptText(type, tab, i++, scriptStrings.size(), part));
         }
      }
      for (int tab : mapConsoles.keySet()) {
         for (long time : mapConsoles.get(tab).keySet()) {
            List<String> consoleStrings = mapConsoles.get(tab).get(time);
            int i = 0;
            for (String part : consoleStrings) {
               Packets.sendServer(new SPacketScriptConsole(type, tab, time, i++, consoleStrings.size(), part));
            }
         }
      }
      if (handler instanceof ClientScriptData) { CustomNPCsScheduler.runTack(() -> Packets.sendServer(new SPacketSaveClientScripts()), 500); }
   }

}
