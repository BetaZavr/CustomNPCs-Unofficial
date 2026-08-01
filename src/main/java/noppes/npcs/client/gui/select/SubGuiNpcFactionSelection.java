package noppes.npcs.client.gui.select;

import java.util.*;

import net.minecraft.network.chat.Component;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.GuiSelectionListener;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

public class SubGuiNpcFactionSelection extends GuiNPCInterface implements ICustomScrollListener {

	protected final GuiSelectionListener listener;
	protected int factionId;

	// New from Unofficial (BetaZavr)
	protected Map<Component, Integer> data = new LinkedHashMap<>();
	protected GuiCustomScrollNop scroll;

	public SubGuiNpcFactionSelection(GuiSelectionListener parent, int factionIdIn) {
		super();
		title = Component.literal("Select Dialog Category");
		setBackground("smallbg.png");
		drawDefaultBackground = false;
		imageWidth = 176;
		imageHeight = 222;

		listener = parent;
		factionId = factionIdIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		Component selected = Component.empty();
		List<Component> list = new ArrayList<>();
		data.clear();
		for (Faction faction : FactionController.instance.factions.values()) {
			Component key = Component.empty()
					.append(Component.literal("ID:" + faction.getId() + " "))
					.append(Component.translatable(faction.getName()).withColor(faction.color));
			list.add(key);
			data.put(key, faction.getId());
			if (factionId == faction.getId()) { selected = key; }
		}
		if (scroll == null) { scroll = addScroll(0).setSize(168, 180); }
		add(scroll.setPos(guiLeft + 4, guiTop + 15)
				.setUnsortedList(list)
				.setSelected(selected));
		addButton(0, guiLeft + 89, guiTop + scroll.height + 39, "mco.template.button.select")
				.setSize(83, 20)
				.setIsEnabled(scroll.hasSelected());
		addButton(66, guiLeft + 4, guiTop + scroll.height + 39, "gui.back")
				.setSize(83, 20)
				.setHoverTexts("hover.back");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (button.id == 0) { scrollDoubleClicked(scroll); }
		onClose();
	}

	@Override
	public void onClose() {
		super.onClose();
		if (listener != null) { NoppesUtil.openGUI(player, listener); }
	}

	@Override
	public void save() {
		if (factionId >= 0 && listener != null) { listener.selected(factionId, scroll.getSelected()); }
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (getButton(0) != null) { getButton(0).setIsEnabled(scroll.hasSelected()); }
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (scroll.hasSelected() && data.containsKey(scroll.getNormalSelected())) {
			factionId = data.get(scroll.getNormalSelected());
		}
	}

}
