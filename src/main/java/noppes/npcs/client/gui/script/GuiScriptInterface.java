package noppes.npcs.client.gui.script;

import java.io.File;
import java.util.*;

import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.mixin.nbt.INBTTagLongArrayMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketScriptConsole;
import noppes.npcs.packets.server.SPacketSaveClientScripts;
import noppes.npcs.packets.server.SPacketScriptEncrypt;
import noppes.npcs.packets.server.SPacketScriptSave;
import noppes.npcs.packets.server.SPacketScriptText;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiMenuTopButton;
import noppes.npcs.shared.client.gui.components.GuiTextArea;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.ClientScriptData;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextChangeListener;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

public class GuiScriptInterface extends GuiNPCInterface
		implements IGuiData, ITextChangeListener, ICustomScrollListener {

	private static final String web_site = "http://www.kodevelopment.nl/minecraft/customnpcs/scripting";
	private static final String api_doc_site = "https://github.com/BetaZavr/CustomNPCsAPI-Unofficial";
	private static final String api_site = "https://github.com/BetaZavr/CustomNPCsAPI-Unofficial";
	private static final String dis_site = "https://discord.gg/RGb4JqE6Qz"; // https://github.com/Noppes/cnpcs-scripting-examples

	protected int activeTab = 0;
	public IScriptHandler handler;

	// New from Unofficial (GoodBird)
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
	}

	@Override
	public void initGui() {
		imageWidth = (int) (width * 0.88D);
		imageHeight = (int) (height * 0.90D);
		super.initGui();
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
		top.active = true;
		if (activeTab > 0) {
			ScriptContainer container = handler.getScripts().get(activeTab - 1);
			final GuiTextArea ta = new GuiTextArea(3, guiLeft + 5, y, imageWidth - 132, imageHeight - 10, container == null ? "" : container.script)
					.enableCodeHighlighting()
					.setListener(this);
			add(ta);
			int x = guiLeft + 7 + ta.width;

			// New from Unofficial (GoodBird)
			add(new GuiButtonNop(this, 99, showFunctions ? "script.hideFunctions" : "script.show.functions", x, y, (button) -> {
				showFunctions = !showFunctions;
				initGui();
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
						.setList(new ArrayList<>(ScriptContainer.Data.keySet()));
			} // functions
		} // scripts
		else {
			GuiTextArea ta = new GuiTextArea(3, guiLeft + 5, y, imageWidth - 175, imageHeight - 10, getConsoleText());
			Map<Long, String> map = handler.getConsoleText();
			if (map.size() > 1) {
				ta.y += 24;
				ta.height -= 24;
			}
			ta.enabled = false;
			add(ta);
			int x = guiLeft + 7 + ta.width;
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
						.setSize(ta.width, 20);
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
					.setIsEnabled(!languages.isEmpty())
					.setSize(80, 20);
			String[] data = getLanguageData(Util.instance.deleteColor(button.getMessage().getString()));
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
			initGui();
		}
		else if (button.id >= CustomNpcs.ScriptMaxTabs + 1 && button.id < 100) {
			if (handler.getScripts().size() >= CustomNpcs.ScriptMaxTabs) { activeTab = CustomNpcs.ScriptMaxTabs; }
			else {
				handler.getScripts().add(new ScriptContainer(handler));
				activeTab = handler.getScripts().size();
			}
			initGui();
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
									Component.empty().getParent(),
									Component.translatable("message.delete").getParent());
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
							},
									Component.empty().getParent(),
									Component.translatable("message.delete").getParent());
							setScreen(guiYesNo);
						}
					}
					else { handler.clearConsole(); }
					initGui();
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
						initGui();
					}
					else {
						ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
							if (bo) { handler.getScripts().remove(activeTab -= 1); }
							setScreen(this);
						},
								Component.empty().getParent(),
								Component.translatable("message.delete").getParent());
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
				case 109: setScreen(new GuiConfirmOpenLink(this, web_site, 0, true)); break;
				case 110: setScreen(new GuiConfirmOpenLink(this, api_doc_site, 1, true)); break;
				case 111: setScreen(new GuiConfirmOpenLink(this, api_site, 2, true)); break;
				case 112: setScreen(new GuiConfirmOpenLink(this, dis_site, 3, true)); break;
				case 115: {
					ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
						if (bo) {
							handler.getScripts().clear();
							activeTab = 0;
						}
						setScreen(this);
					},
							Component.translatable("gui.remove.all").getParent(),
							Component.translatable("message.delete").getParent());
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
						initGui();
					}
					break;
				} // clear log
			}
		}
	}

	@Override
	public void confirmClicked(boolean result, int id) {
		if (result) {
			switch (id) {
				case 0: openLink(web_site); break;
				case 1: openLink(api_doc_site); break;
				case 2: openLink(api_site); break;
				case 3: openLink(dis_site); break;
			}
		}
		setScreen(this);
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		NBTTagList data = compound.getTagList("Languages", 10);
		Map<String, Map<String, Long>> newLanguages = new HashMap<>();
		for (int i = 0; i < data.tagCount(); ++i) {
			NBTTagCompound comp = data.getCompoundTagAt(i);
			Map<String, Long> scripts = new TreeMap<>();
			NBTTagList list = comp.getTagList("Scripts", 8);
			long[] ld = new long[list.tagCount()];
			if (comp.hasKey("sizes", 12)) { ld = ((INBTTagLongArrayMixin) comp.getTag("sizes")).getData(); }
			if (ld != null) {
				for (int j = 0; j < list.tagCount(); ++j) { scripts.put(list.getStringTagAt(j), ld[j]); }
			}
			newLanguages.put(comp.getString("Language"), scripts);
			if (Util.instance.equalsDeleteColor(comp.getString("Language"), handler.getLanguage(), false)) { ext = comp.getString("FileSfx"); }
		}
		languages = newLanguages;
		path = compound.getString("DirPath") + "/" + handler.getLanguage().toLowerCase();
		methods = NBTTags.getStringList(compound.getTagList("Methods", 10));
		initGui();
	}

	@Override
	public void save() { setScript(); }

	@Override
	public void textUpdate(IComponentGui component, String text) {
		if (activeTab <= 0 || activeTab > handler.getScripts().size()) { return; }
		ScriptContainer container = handler.getScripts().get(activeTab - 1);
		if (container != null) { container.script = text; }
	}

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

	// New from Unofficial (GoodBird)
	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) { }

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { get(3, GuiTextArea.class).addText(scroll.getSelected()); }

	// New from Unofficial (BetaZavr)
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (player.world.getTotalWorldTime() % 5 == 0) {
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
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiScriptEncrypt && ((SubGuiScriptEncrypt) subgui).send) {
			SubGuiScriptEncrypt gui = (SubGuiScriptEncrypt) subgui;
			save();
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setString("Name", gui.getTextField(0).getValue() + gui.ext);
			String p = path.replaceAll("\\\\", "/") + "/" + nbt.getString("Name");
			if (p.contains("/scripts")) { p = p.substring(p.indexOf("/scripts") + 8); }
			nbt.setString("Path", p);
			nbt.setInteger("Tab", activeTab - 1);
			nbt.setBoolean("OnlyTab", gui.onlyTab);
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
			File levelDir = CustomNpcs.getWorldSaveDirectory();
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
			initGui();
		}
	}

	public void setTabConsole(int tab, long time, String log) {
		ScriptContainer container = handler.getScripts().get(tab);
		if (container != null) {
			container.console.put(time, log);
			initGui();
		}
	}

	protected void sendToServer(NBTTagCompound compound) {
		// collect and clear scripts and consoles
		Map<Integer, List<String>> mapScripts = new TreeMap<>();
		Map<Integer, Map<Long, List<String>>> mapConsoles = new TreeMap<>();
		NBTTagList scripts = compound.getTagList("Scripts", 10);
		for (int i = 0; i < scripts.tagCount(); i++) {
			NBTTagCompound scriptNbt = scripts.getCompoundTagAt(i);
			// Script
			if (scriptNbt.hasKey("Script", 8)) { mapScripts.put(i, noppes.npcs.util.Util.instance.getStringData(scriptNbt.getString("Script"))); }
			else {
				mapScripts.put(i, new ArrayList<>());
				NBTTagList list = scriptNbt.getTagList("Script", 8);
				for (int k = 0; k < list.tagCount(); k++) { mapScripts.get(i).add(list.getStringTagAt(k)); }
			}
			scriptNbt.setTag("Script", new NBTTagList());
			// Console
			NBTTagList consoles = scriptNbt.getTagList("Console", 10);
			for (int j = 0; j < consoles.tagCount(); j++) {
				if (!mapConsoles.containsKey(i)) { mapConsoles.put(i, new LinkedHashMap<>()); }
				NBTTagCompound errorNbt = consoles.getCompoundTagAt(j);
				long time = errorNbt.getLong("Long");
				if (errorNbt.hasKey("String", 8)) {
					mapConsoles.get(i).put(time, noppes.npcs.util.Util.instance.getStringData(errorNbt.getString("String")));
				}
				else {
					mapConsoles.get(i).put(time, new ArrayList<>());
					NBTTagList list = errorNbt.getTagList("String", 8);
					for (int k = 0; k < list.tagCount(); k++) { mapConsoles.get(i).get(time).add(list.getStringTagAt(k)); }
				}
				errorNbt.setTag("String", new NBTTagList());
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
		if (handler instanceof ClientScriptData) {
			CustomNPCsScheduler.runTack(() -> Packets.sendServer(new SPacketSaveClientScripts()), 500);
		}
	}

}
