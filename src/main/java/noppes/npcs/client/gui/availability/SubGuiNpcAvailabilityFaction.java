package noppes.npcs.client.gui.availability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.gui.select.SubGuiNpcFactionSelection;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumAvailabilityFaction;
import noppes.npcs.constants.EnumAvailabilityFactionType;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.AvailabilityFactionData;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.GuiSelectionListener;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

// New from Unofficial (BetaZavr)
public class SubGuiNpcAvailabilityFaction
		extends GuiNPCInterface
		implements ICustomScrollListener, GuiSelectionListener {

	protected final Availability availability;
	protected final Map<Component, AvailabilityFactionData> dataSets = new HashMap<>();
	protected final Map<Component, Integer> dataIDs = new HashMap<>();
	protected GuiCustomScrollNop scroll;
	protected Component select = Component.empty();

	public SubGuiNpcAvailabilityFaction(Availability availabilityIn) {
		super();
		setBackground("menubg.png");
		imageWidth = 256;
		imageHeight = 217;

		availability = availabilityIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		boolean isSelect = !select.getFormattedText().isEmpty();
		// title
		addLabel(1, guiLeft + 6, guiTop + 4, "availability.available.2")
				.setSize(imageWidth - 12, 12)
				.setCenter(imageWidth - 12);
		// exit
		addButton(66, guiLeft + 6, guiTop + 192, "gui.done")
				.setSize(70, 20)
				.setHoverTexts("hover.back");
		// data
		if (scroll == null) { scroll = addScroll(6).setSize(imageWidth - 12, imageHeight - 66); }
		dataIDs.clear();
		dataSets.clear();
		for (int id : availability.factions.keySet()) {
			Component key = Component.literal("ID:" + id + " - ");
			Faction faction = FactionController.instance.factions.get(id);
			AvailabilityFactionData afd = availability.factions.get(id);
			if (faction == null) { key.append(Component.translatable("faction.notfound").withStyle(TextFormatting.DARK_RED)); }
			else {
				String stance;
				switch (afd.factionStance) {
					case Friendly: stance = "faction.friendly"; break;
					case Neutral: stance = "faction.neutral"; break;
					default: stance = "faction.unfriendly"; break;
				}
				key.append(Component.translatable(faction.getName()).withStyle(TextFormatting.RESET))
						.append(Component.literal(" (").withStyle(TextFormatting.GRAY))
						.append(Component.translatable("availability." + afd.factionAvailable.name().toLowerCase()).withStyle(TextFormatting.DARK_AQUA))
						.append(Component.literal(") (").withStyle(TextFormatting.GRAY))
						.append(Component.translatable(stance).withStyle(TextFormatting.BLUE))
						.append(Component.literal(")").withStyle(TextFormatting.GRAY));
			}
			dataIDs.put(key, id);
			dataSets.put(key, availability.factions.get(id));
		}
		if (isSelect) {
			boolean found = false;
			for (Component line : dataIDs.keySet()) {
				if (line.getString().equals(select.getString())) {
					found = true;
					break;
				}
			}
			if (!found) {
				select = Component.empty();
				isSelect = false;
			}
		}
		scroll.setNormalList(new ArrayList<>(dataIDs.keySet()));
		if (isSelect) { scroll.setSelected(select); }
		add(scroll.setPos(guiLeft + 6, guiTop + 14));
		// type
		int p = 0;
		int s = 0;
		if (isSelect) {
			p = dataSets.get(select).factionAvailable.ordinal();
			s = dataSets.get(select).factionStance.ordinal();
		}
		// type
		addButton(0, guiLeft + 6, guiTop + imageHeight - 46, false, p,
				"availability.always", "availability.is", "availability.isnot")
				.setSize(50, 20)
				.setHoverTexts("availability.hover.enum.type");
		// faction type
		addButton(4, guiLeft + 58, guiTop + imageHeight - 46, false, s,
				"faction.friendly", "faction.neutral", "faction.unfriendly")
				.setSize(50, 20)
				.setHoverTexts("availability.hover.faction.type");
		// select
		addButton(1, guiLeft + 110, guiTop + imageHeight - 46, "availability.select")
				.setSize(118, 20)
				.setHoverTexts("availability.hover.faction");
		// del
		addButton(2, guiLeft + 230, guiTop + imageHeight - 46, "X")
				.setSize(20, 20)
				.setHoverTexts("availability.hover.remove");
		// extra
		addButton(3, guiLeft + imageWidth - 76, guiTop + 192, "availability.more")
				.setSize(70, 20)
				.setIsEnabled(isSelect)
				.setHoverTexts("availability.hover.more");
		updateGuiButtons();
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				if (!dataIDs.containsKey(select)) { return; }
				int id = dataIDs.get(select);
				AvailabilityFactionData afd = availability.factions.get(id);
				afd.factionAvailable = EnumAvailabilityFactionType.values()[button.getValue()];
				availability.factions.put(id, afd);
				select = Component.literal("ID:" + id + " - ");
				Faction faction = FactionController.instance.factions.get(id);
				if (faction == null) { select.append(Component.translatable("faction.notfound").withStyle(TextFormatting.DARK_RED)); }
				else {
					String stance;
					switch (afd.factionStance) {
						case Friendly: stance = "faction.friendly"; break;
						case Neutral: stance = "faction.neutral"; break;
						default: stance = "faction.unfriendly"; break;
					}
					select.append(Component.translatable(faction.getName()).withStyle(TextFormatting.RESET))
							.append(Component.literal(" (").withStyle(TextFormatting.GRAY))
							.append(Component.translatable("availability." + afd.factionAvailable.name().toLowerCase()).withStyle(TextFormatting.DARK_AQUA))
							.append(Component.literal(") (").withStyle(TextFormatting.GRAY))
							.append(Component.translatable(stance).withStyle(TextFormatting.BLUE))
							.append(Component.literal(")").withStyle(TextFormatting.GRAY));
				}
				initGui();
				break;
			}
			case 1: {
				setSubGui(new SubGuiNpcFactionSelection(this, dataIDs.getOrDefault(select, -1)));
				break;
			}
			case 2: {
				availability.factions.remove(dataIDs.get(select));
				select = Component.empty();
				initGui();
				break;
			}
			case 3: { // More
				save();
				initGui();
				break;
			}
			case 4: {
				if (!dataIDs.containsKey(select)) { return; }
				EnumAvailabilityFaction eaf = EnumAvailabilityFaction.values()[button.getValue()];
				int id = dataIDs.get(select);
				AvailabilityFactionData afd = availability.factions.get(id);
				afd.factionStance = eaf;
				availability.factions.put(id, afd);
				select = Component.literal("ID:" + id + " - ");
				Faction faction = FactionController.instance.factions.get(id);
				if (faction == null) { select.append(Component.translatable("faction.notfound").withStyle(TextFormatting.DARK_RED)); }
				else {
					String stance;
					switch (afd.factionStance) {
						case Friendly: stance = "faction.friendly"; break;
						case Neutral: stance = "faction.neutral"; break;
						default: stance = "faction.unfriendly"; break;
					}
					select.append(Component.translatable(faction.getName()).withStyle(TextFormatting.RESET))
							.append(Component.literal(" (").withStyle(TextFormatting.GRAY))
							.append(Component.translatable("availability." + afd.factionAvailable.name().toLowerCase()).withStyle(TextFormatting.DARK_AQUA))
							.append(Component.literal(") (").withStyle(TextFormatting.GRAY))
							.append(Component.translatable(stance).withStyle(TextFormatting.BLUE))
							.append(Component.literal(")").withStyle(TextFormatting.GRAY));
				}
				initGui();
				break;
			}
			case 66: onClose(); break;
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		select = scroll.getNormalSelected();
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		setSubGui(new SubGuiNpcFactionSelection(this, dataIDs.getOrDefault(select, -1)));
	}

	@Override
	public void selected(int id, String name) {
		if (id < 0) { return; }
		if (dataIDs.containsKey(select)) { availability.factions.remove(dataIDs.get(select)); }
		AvailabilityFactionData afd = new AvailabilityFactionData(EnumAvailabilityFactionType.Is,  EnumAvailabilityFaction.Friendly);
		select = Component.literal("ID:" + id + " - ");
		Faction faction = FactionController.instance.factions.get(id);
		if (faction == null) {
			select.append(Component.translatable("faction.notfound").withStyle(TextFormatting.DARK_RED));
		} else {
			select.append(Component.translatable(faction.getName()).withStyle(TextFormatting.RESET))
					.append(Component.literal(" (").withStyle(TextFormatting.GRAY))
					.append(Component.translatable("availability.is").withStyle(TextFormatting.DARK_AQUA))
					.append(Component.literal(") (").withStyle(TextFormatting.GRAY))
					.append(Component.translatable("faction.friendly").withStyle(TextFormatting.BLUE))
					.append(Component.literal(")").withStyle(TextFormatting.GRAY));
		}
		availability.factions.put(id, afd);
		initGui();
		updateGuiButtons();
	}

	@Override
	public void onClose() {
		super.onClose();
		for (int id : new ArrayList<>(availability.factions.keySet())) {
			if (availability.factions.get(id).factionAvailable == EnumAvailabilityFactionType.Always) { availability.factions.remove(id); }
		}
	}

	@Override
	public void save() {
		if (!dataIDs.containsKey(select)) { return; }
		EnumAvailabilityFactionType eaft = EnumAvailabilityFactionType.values()[getButton(0).getValue()];
		EnumAvailabilityFaction eaf = EnumAvailabilityFaction.values()[getButton(4).getValue()];
		AvailabilityFactionData afd = new AvailabilityFactionData(eaft, eaf);
		int id = dataIDs.get(select);
		if (eaft != EnumAvailabilityFactionType.Always) {
			availability.factions.put(id, afd);
			dataSets.put(select, afd);
		}
		else { availability.factions.remove(id); }
		select = Component.empty();
	}

	private void updateGuiButtons() {
		getButton(1).setDisplayText("availability.selectquest");
		int p = 0, s = 0;
		Faction faction = null;
		boolean isSelect = !select.getFormattedText().isEmpty();
		if (isSelect) {
			faction = FactionController.instance.factions.get(dataIDs.get(select));
			p = dataSets.get(select).factionAvailable.ordinal();
			s = dataSets.get(select).factionStance.ordinal();
		}
		getButton(0).setDisplay(p)
				.setIsEnabled(isSelect);
		getButton(4).setDisplay(s)
				.setIsEnabled(isSelect);
		getButton(1).setIsEnabled(p != 0 || !isSelect)
				.setDisplayText(faction == null ? "availability.select" : faction.getName());
		getButton(2).setIsEnabled(isSelect);
	}

}
