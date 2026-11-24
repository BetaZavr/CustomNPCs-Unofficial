package noppes.npcs.client.gui.script;

import java.util.*;

import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class SubGuiScriptList extends GuiNPCInterface implements ICustomScrollListener {

	protected final ScriptContainer container;
	protected GuiCustomScrollNop base;
	protected GuiCustomScrollNop selected;

	// New from Unofficial (BetaZavr)
	protected static final Comparator<Component> comparator = Comparator.comparing(Component::getFormattedText);
	protected final Map<String, Long> scripts;
	protected final Map<FilePath, Component> data = new TreeMap<>();
	protected final Component back = Component.literal("   ← (").append(Component.translatable("gui.back")).append(")");
	protected String path = "";

	public SubGuiScriptList(Map<String, Long> scriptsList, ScriptContainer cont) {
		super();
		setBackground("menubg.png");
		imageWidth = 346;
		imageHeight = 216;

		container = cont;
		if (scriptsList == null) { scriptsList = new TreeMap<>(); }
		scripts = scriptsList;
		for (String path : scripts.keySet()) {
			FilePath fp = new FilePath(path);
			data.put(fp, Component.literal(fp.getName()));
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		if (base == null) { base = addScroll(0).setSize(140, 180); }
		add(base.setPos(guiLeft + 4, guiTop + 14));
		addLabel(1, guiLeft + 4, guiTop + 4, "script.availableScripts");
		if (selected == null) { selected = addScroll(1).setSize(141, 180); }
		add(selected.setPos(guiLeft + 200, guiTop + 14));
		addLabel(2, guiLeft + 201, guiTop + 4, "script.loadedScripts");
		addLabel(3, guiLeft + 4, guiTop + 40 + base.height,
				Component.literal(".../" + path).withStyle(TextFormatting.BLACK, TextFormatting.BOLD))
				.setSize(imageWidth - 8, 10);
		List<String> temp = new ArrayList<>(scripts.keySet());
		temp.removeAll(container.scripts);
		Map<Component, Long> ds = new TreeMap<>(comparator);
		Map<Component, Long> fs = new TreeMap<>(comparator);
		Map<Component, Long> ft = new TreeMap<>(comparator);
		Map<String, ArrayList<Component>> hs = new HashMap<>();
		List<Component> listBase = new ArrayList<>();
		List<Component> suffixesBase = new ArrayList<>();
		List<Component> list = new ArrayList<>();
		List<Component> suffixes = new ArrayList<>();
		int t = 1;
		if (!path.isEmpty()) { ds.put(back, 0L); }
		for (FilePath res : data.keySet()) {
			Component key = data.get(res);
			boolean hasDir = !res.getSpace().equals("base");
			String file = (hasDir ? res.getSpace() + "/" : "") + res.getName();
			if (temp.contains(file) || container.scripts.contains(file)) {
				boolean isBase = temp.contains(file);
				if (isBase) {
					String folder = hasDir ? res.getSpace() : "";
					if (folder.isEmpty()) {
						if (path.isEmpty()) {
							fs.put(key, scripts.get(file));
							hs.put(key.getFormattedText(), new ArrayList<>(Collections.singletonList(Component.literal(file))));
						}
					}
					else {
						if (!path.isEmpty()) {
							if (folder.equals(path)) { folder = ""; }
							else if (folder.startsWith(path + "/")) { folder = folder.substring(path.length() + 1); }
							else if (!folder.contains("/")) { continue; }
						}
						if (folder.contains("/")) { folder = folder.substring(0, folder.indexOf("/")); }
						Component fld = Component.literal(folder);
						if (path.isEmpty() && !folder.isEmpty()) {
							ds.put(fld, 0L);
							hs.put(fld.getFormattedText(), new ArrayList<>(Collections.singletonList(Component.literal(folder))));
						}
						else if (folder.isEmpty()) {
							fs.put(key, scripts.get(file));
							hs.put(key.getFormattedText(), new ArrayList<>(Collections.singletonList(Component.literal(file))));
						}
						else {
							ds.put(fld, 0L);
							hs.put(fld.getFormattedText(), new ArrayList<>(Collections.singletonList(Component.literal(path + "/" + folder))));
						}
					}
				}
				else {
					Component line = Component.empty()
							.append(Component.literal(t + ":").withStyle(TextFormatting.GRAY))
							.append(key.withStyle(TextFormatting.RESET));
					ft.put(line, scripts.get(file));
					hs.put(line.getFormattedText(), new ArrayList<>(Collections.singletonList(Component.literal(file))));
					t++;
				}
			}
		}
		LinkedHashMap<Integer, List<Component>> htsB= new LinkedHashMap<>();
		int i = 0;
		for (Component key : ds.keySet()) {
			if (key == null) { key = Component.empty(); }
			suffixesBase.add(Component.empty());
			listBase.add(key);
			if (hs.containsKey(key.getFormattedText())) { htsB.put(i, hs.get(key.getFormattedText())); }
			key.getStyle().setColor(TextFormatting.GOLD);
			i++;
		}
		for (Component key : fs.keySet()) {
			if (key == null) { key = Component.empty(); }
			long l = fs.get(key);
			String size = "" + Math.abs(l);
			if (Math.abs(l) > 999) { size = Util.instance.getTextReducedNumber(Math.abs(l), false, false, true); }
			suffixesBase.add(Component.literal(size).withStyle(TextFormatting.AQUA));
			listBase.add(key);
			if (hs.containsKey(key.getFormattedText())) {
				List<Component> hoverList = hs.get(key.getFormattedText());
				if (l < 0) {
					hs.get(key.getFormattedText()).add(Component.translatable("gui.encrypted").withStyle(TextFormatting.DARK_RED));
				}
				htsB.put(i, hoverList);
			}
			key.getStyle().setColor(l >= 0 ? TextFormatting.AQUA : TextFormatting.YELLOW);
			i++;
		}
		LinkedHashMap<Integer, List<Component>> htsS= new LinkedHashMap<>();
		i = 0;
		for (Component key : ft.keySet()) {
			long l = ft.get(key);
			String size = "" + Math.abs(l);
			if (Math.abs(l) > 999) { size = Util.instance.getTextReducedNumber(Math.abs(l), false, false, true); }
			suffixes.add(Component.literal(size).withStyle(TextFormatting.AQUA));
			list.add(key);
			if (hs.containsKey(key.getFormattedText())) {
				List<Component> hoverList = hs.get(key.getFormattedText());
				if (l < 0) {
					hs.get(key.getFormattedText()).add(Component.translatable("gui.encrypted").withStyle(TextFormatting.DARK_RED));
				}
				htsS.put(i, hoverList);
			}
			key.getStyle().setColor(l >= 0 ? TextFormatting.AQUA : TextFormatting.YELLOW);
			i++;
		}
		base.setUnsortedList(listBase).setSuffixes(suffixesBase).setHoverTexts(htsB);
		selected.setUnsortedList(list).setSuffixes(suffixes).setHoverTexts(htsS);
		int x = guiLeft + 145;
		int y = guiTop + 40;
		addButton(1, x, y, ">")
				.setIsEnabled(base.hasSelected())
				.setSize(55, 20);
		addButton(2, x, y += 22, "<")
				.setIsEnabled(selected.hasSelected())
				.setSize(55, 20);
		addButton(5, x, y += 44, ">>>")
				.setIsEnabled(!temp.isEmpty())
				.setSize(55, 20);
		addButton(3, x, y += 44, ">>")
				.setIsEnabled(!temp.isEmpty())
				.setSize(55, 20);
		addButton(4, x, y += 22, "<<")
				.setIsEnabled(!container.scripts.isEmpty())
				.setSize(55, 20);
		addButton(66, x, y + 24, "gui.done")
				.setSize(55, 20);
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 1: {
				if (!base.hasSelected()) { return; }
				String file;
				try { file = base.getHoversTexts().get(base.getSelectedIndex()).get(0).getFormattedText(); } catch (Exception e) { return; }
				container.scripts.add(file);
				base.setSelected(-1);
				initGui();
				break;
			} // >
			case 2: {
				if (!selected.hasSelected()) { return; }
				String file;
				try { file = selected.getHoversTexts().get(selected.getSelectedIndex()).get(0).getFormattedText(); } catch (Exception e) { return; }
				container.scripts.remove(file);
				selected.setSelected(-1);
				initGui();
				break;
			} // <
			case 3: {
				container.scripts.clear();
				for (String name : scripts.keySet()) {
					if (path.isEmpty() && !name.contains("/") || (!path.isEmpty() && name.startsWith(path))) {
						container.scripts.add(name);
					}
				}
				base.setSelected(-1);
				initGui();
				break;
			} // >>
			case 4: {
				container.scripts.clear();
				base.setSelected(-1);
				initGui();
				break;
			} // <<
			case 66: onClose(); break;
		}
	}

    @Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0) {
			if (scroll.getSelected().equals(back.getFormattedText())) {
				if (path.lastIndexOf("/") == -1) { path = ""; }
				else { path = path.substring(0, path.lastIndexOf("/")); }
				base.setSelected(-1);
			} else if (scroll.getNormalSelected().getStyle().getColor() == TextFormatting.GOLD) {
				if (!path.isEmpty()) { path += "/"; }
				path += scroll.getSelected();
				base.setSelected(-1);
			}
		}
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		String file = "";
		try { file = scroll.getHoversTexts().get(scroll.getSelectedIndex()).get(0).getFormattedText(); }
		catch (Exception e) { LogWriter.error(e); }
		if (file.isEmpty()) { return; }
		if (scroll.id == 0) {
			container.scripts.add(file);
			base.setSelected(-1);
			initGui();
		}
		if (scroll.id == 1) {
			container.scripts.remove(file);
			selected.setSelected(-1);
			initGui();
		}
	}

	public static class FilePath implements Comparable<FilePath> {

		String spase = "base";
		String name;

		public FilePath(String path) {
			name = path;
			if (path.contains("/")) {
				spase = path.substring(0, path.lastIndexOf("/"));
				name = path.substring(path.lastIndexOf("/") + 1);
			}
		}

		public String getSpace() { return spase; }

		public String getName() { return name; }

		@Override
		public int compareTo(@Nonnull FilePath other) { return (spase + "/" + name).compareTo(other.spase + "/" + other.name); }

	}

}
