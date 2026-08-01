package noppes.npcs.client.gui.global;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.SubGuiNPCLinesEdit;
import noppes.npcs.client.gui.player.GuiNPCTrader;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.controllers.data.Marcet;
import noppes.npcs.controllers.data.MarcetSection;
import noppes.npcs.controllers.data.MarkupData;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class SubGuiNpcMarketSettings extends GuiNPCInterface
		implements ICustomScrollListener, ITextfieldListener {

	protected static final Object[] icons = new Object[] { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29 };
	protected final Map<Component, Integer> data = new HashMap<>();
	protected GuiCustomScrollNop scroll;
	public Marcet marcet;
	public int level = 0;

	public SubGuiNpcMarketSettings(Marcet marketIn) {
		super();
		setBackground("menubg.png");
		imageWidth = 256;
		imageHeight = 217;

		marcet = marketIn;
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				marcet.limitedType = button.getValue();
				button.setHoverTexts("market.hover.only.currency." + marcet.limitedType);
				break;
			}
			case 1: setSubGui(new SubGuiNPCLinesEdit(0, npc, marcet.lines, null)); break; // message
			case 2: marcet.isLimited = ((GuiCheckBoxNop) button).selected(); break; // is limited
			case 3: marcet.showXP = ((GuiCheckBoxNop) button).selected(); break; // show xp
			case 4: {
				level = button.getValue();
				if (!marcet.markup.containsKey(0)) { marcet.markup.put(0, new MarkupData(0, 0.15f, 0.80f, 1000)); }
				if (!marcet.markup.containsKey(level)) { level = 0; }
				initGui();
				break;
			} // level
			case 5: {
				setSubGui(new SubGuiEditText(1, Util.instance.deleteColor(Component.translatable("gui.new").getString())));
				break;
			} // add section
			case 6: {
				if (!scroll.hasSelected()) { return; }
				ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
					if (agree && data.containsKey(scroll.getNormalSelected()) && marcet.sections.size() > 1) {
						marcet.sections.remove(data.get(scroll.getNormalSelected()));
						scroll.setSelect(scroll.getSelectedIndex() - 1);
						initGui();
					}
					NoppesUtil.openGUI(player, this);
				},
						Component.translatable("gui.sections").append(": ").append(scroll.getNormalSelected()).getParent(),
						Component.translatable("message.delete").getParent());
				setScreen(guiYesNo);
				break;
			} // del section
			case 7: {
				if (scroll.hasSelected() && data.containsKey(scroll.getNormalSelected())) {
					marcet.sections.get(data.get(scroll.getNormalSelected())).setIcon(button.getValue());
				}
				break;
			}
			case 66: onClose(); break;
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (hasSubGui()) { return; }
		if (getButton(4) != null) {
			int color = new Color(0x80000000).getRGB();
			drawHorizontalLine(guiLeft + 4, guiLeft + imageWidth - 4, guiTop + 47, color);
			drawHorizontalLine(guiLeft + 4, guiLeft + imageWidth - 4, guiTop + 133, color);
		}
		int u = 0;
		int v = 0;
		if (scroll.hasSelected() && data.containsKey(scroll.getNormalSelected())) {
			int icon = marcet.sections.get(data.get(scroll.getNormalSelected())).getIcon();
			u = (icon % 10) * 24;
			v = (int) Math.floor((float) icon / 10.0f) * 72;
		}
		minecraft.getTextureManager().bindTexture(GuiNPCTrader.ICONS);
		drawTexturedModalRect(guiLeft + 180, guiTop + 178, u, v, 24, 24);
	}

	@Override
	public void initGui() {
		super.initGui();
		int lID = 0;
		int x = guiLeft + 4;
		int y = guiTop + 5;
		// name
		addLabel(lID++, x + 2, y + 5, "role.trader")
				.setSize(78, 12);
		addTextField(0, x + 80, y, 167, 18, marcet.name)
				.setHoverTexts(Component.translatable("market.hover.set.name", Component.translatable(marcet.name)));
		// time
		addLabel(lID++, x + 2, (y += 22) + 5, "market.uptime")
				.setSize(78, 12);
		addTextField(1, x + 80, y, 60, 18, marcet.updateTime)
				.setMinMaxDefault(0, 360, marcet.updateTime)
				.setHoverTexts(Component.translatable("market.hover.set.update",
						Util.instance.ticksToElapsedTime(marcet.updateTime * 1200L, false, false, false)));
		if (marcet.updateTime >= 5) {
			y += 22;
			addButton(0, x, y, false, marcet.limitedType, "market.limited.0", "market.limited.1", "market.limited.2")
					.setSize(170, 20)
					.setHoverTexts("market.hover.only.currency." + marcet.limitedType);
		}
		// tabs
		addLabel(lID++, x + 176, y - 17, "gui.sections")
				.setSize(72, 12);
		if (scroll == null) { scroll = addScroll(0).setSize(72, 60); }
		List<Component> list = new ArrayList<>();
		data.clear();
		LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
		int i = 0;
		for (int id : marcet.sections.keySet()) {
			List<Component> l = new ArrayList<>();
			l.add(Component.literal("ID: " + id));
			l.add(Component.translatable("gui.name").append(": " + marcet.sections.get(id).toString()));
			hts.put(i, l);
			Component key = marcet.sections.get(id).getName();
			data.put(key, id);
			list.add(key);
			i++;
		}
		add(scroll.setPos(x + 175, y)
				.setUnsortedList(list)
				.setHoverTexts(hts));
		if (!scroll.hasSelected()) { scroll.setSelectedIndex(0); }
		// update message
		addButton(1, x, y += 22, "lines.title")
				.setSize(170, 20)
				.setHoverTexts("market.hover.message");
		// isLimited
		addCheckBox(2, x, y += 20, "market.select.limited.true", "market.select.limited.false", marcet.isLimited)
				.setSize(170, 18)
				.setHoverTexts("market.hover.limited");
		// show XP
		addCheckBox(3, x, y += 20, "market.select.show.xp.true", "market.select.show.xp.false", marcet.showXP)
				.setSize(170, 18)
				.setHoverTexts("market.hover.show.xp");
		// add new section
		addButton(5, x + 175, y, "type.add")
				.setSize(37, 20)
				.setHoverTexts("market.hover.section.add");
		// del section
		addButton(6, x + 213, y, "type.del")
				.setSize(35, 20)
				.setIsEnabled(marcet.sections.size() > 1 && scroll.hasSelected())
				.setHoverTexts("market.hover.section.del");
		// levels
		Object[] values = new Object[marcet.markup.size()];
		i = 0;
		for (int level : marcet.markup.keySet()) {
			values[i] = Component.translatable("type.level").append(" " + level);
			i++;
		}
		addLabel(lID++, x + 2, (y += 25) + 5, "gui.type")
				.setSize(48, 12);
		addButton(4, x + 22, y, true, level, values)
				.setSize(50, 20)
				.setHoverTexts("market.hover.extra.slot");
		// extra markup
		MarkupData md = marcet.markup.get(level);
		if (md == null) {
			level = 0;
			if (!marcet.markup.containsKey(0)) { marcet.markup.put(0, new MarkupData(0, 0.15f, 0.80f, 1000)); }
			md = marcet.markup.get(level);
		}
		// buy
		addLabel(lID++, x + 76, y + 5, "market.extra.markup")
				.setSize(48, 12);
		addLabel(lID++, x + 174, y + 5, "%")
				.setSize(10, 12);
		addTextField(2, x + 120, y, 50, 20, "" + Math.round(md.buy * 100.0f))
				.setMinMaxDefault(-100, 500, Math.round(md.buy * 100.0f))
				.setHoverTexts("market.hover.extra.buy");
		// sell
		addLabel(lID++, x + 238, y + 5, "%")
				.setSize(10, 12);
		addTextField(3, x + 184, y, 50, 20, "" + Math.round(md.sell * 100.0f))
				.setMinMaxDefault(-500, 100, Math.round(md.sell * 100.0f))
				.setHoverTexts("market.hover.extra.sell");
		// xp
		addLabel(lID++, x + 76, (y += 22) + 5, "quest.exp")
				.setSize(42, 12);
		addTextField(4, x + 120, y, 50, 20, "" + md.xp)
				.setMinMaxDefault(0, Integer.MAX_VALUE, md.xp)
				.setHoverTexts("market.hover.xp");
		// section icon
		if (scroll.hasSelected() && data.containsKey(scroll.getNormalSelected())) {
			int icon = marcet.sections.get(data.get(scroll.getNormalSelected())).getIcon();
			addLabel(lID, x + 76, (y += 22) + 5, "dialog.icon")
					.setSize(42, 12);
			addButton(7, x + 120, y, true, icon, icons)
					.setSize(50, 20)
					.setHoverTexts("market.hover.section.icon");
		}
		// exit
		addButton(66, x, guiTop + imageHeight - 24, "gui.done")
				.setSize(60, 20)
				.setHoverTexts("hover.back");
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) { initGui(); }

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (!scroll.hasSelected()) { return; }
		setSubGui(new SubGuiEditText(2, scroll.getSelected()));
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiEditText) {
			SubGuiEditText gui = (SubGuiEditText) subgui;
			if (gui.cancelled) { return; }
			if (gui.id == 1) {
				String name = ((SubGuiEditText) subgui).text[0];
				boolean has = true;
				while (has) {
					has = false;
					for (MarcetSection s : marcet.sections.values()) {
						if (s.name.equals(name)) {
							has = true;
							break;
						}
					}
					if (has) { name += "_"; }
				}
				MarcetSection ms = new MarcetSection(marcet.sections.size());
				ms.name = name;
				marcet.sections.put(ms.getId(), ms);
			}
			else if (gui.id == 2) {
				if (!data.containsKey(scroll.getNormalSelected())) { return; }
				String name = ((SubGuiEditText) subgui).text[0];
				int idSel = data.get(scroll.getNormalSelected());
				boolean next = true;
				while (next) {
					next = false;
					for (int id : marcet.sections.keySet()) {
						if (id == idSel) { continue; }
						if (marcet.sections.get(id).name.equals(name)) {
							name += "_";
							next = true;
							break;
						}
					}
				}
				marcet.sections.get(idSel).name = name;
			}
			initGui();
		}
		else if (subgui instanceof SubGuiNPCLinesEdit) {
			((SubGuiNPCLinesEdit) subgui).lines.correctLines();
			marcet.lines = ((SubGuiNPCLinesEdit) subgui).lines;
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (hasSubGui()) { return; }
		String text = textField.getValue();
		MarkupData md = marcet.markup.get(level);
		switch (textField.id) {
			case 0: {
				if (text.equals(marcet.name)) { return; }
				marcet.name = text;
				initGui();
				break;
			}
			case 1: {
				int time = textField.getInteger();
				if (time < 5) { time = 0; }
				if (time > 360) { time = 360; }
				marcet.updateTime = time;
				initGui();
				break;
			}
			case 2: {
				if (md == null) { return; }
				md.buy = (float) (Math.round((double) textField.getInteger() * 100.0d) / 10000.0d);
				initGui();
				break;
			}
			case 3: {
				if (md == null) { return; }
				md.sell = (float) (Math.round((double) textField.getInteger() * 100.0d) / 10000.0d);
				initGui();
				break;
			}
			case 4: {
				if (md == null) { return; }
				md.xp = textField.getInteger();
				break;
			}
		}
	}

}
