package noppes.npcs.client.gui.availability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.gui.select.SubGuiQuestSelection;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumAvailabilityQuest;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.GuiSelectionListener;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

// Change from Unofficial (BetaZavr)
public class SubGuiNpcAvailabilityQuest
		extends GuiNPCInterface
		implements GuiSelectionListener, ICustomScrollListener {

	protected static final Object[] types = new Object[] { "availability.always", "availability.after", "availability.before", "availability.active", "availability.notactive", "availability.completed", "availability.canstart" };
	protected final Availability availability;
	protected final Map<Component, EnumAvailabilityQuest> dataEnum = new HashMap<>();
	protected final Map<Component, Integer> dataIDs = new HashMap<>();
	protected GuiCustomScrollNop scroll;
	protected Component select = Component.empty();

	public SubGuiNpcAvailabilityQuest(Availability availabilityIn) {
		super();
		setBackground("menubg.png");
		imageWidth = 316;
		imageHeight = 217;

		availability = availabilityIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		boolean isSelect = !select.getFormattedText().isEmpty();
		// title
		addLabel(0, guiLeft + 6, guiTop + 4, "availability.available.1")
				.setSize(imageWidth - 12, 12)
				.setCenter(imageWidth - 12);
		// exit
		addButton(66, guiLeft + 6, guiTop + 192, "gui.done")
				.setSize(70, 20)
				.setHoverTexts("hover.back");
		// data
		if (scroll == null) { scroll = addScroll(6).setSize(imageWidth - 12, imageHeight - 66); }
		dataIDs.clear();
		dataEnum.clear();
		for (int id : availability.quests.keySet()) {
			Component key = Component.literal("ID:" + id + " - ");
			Quest quest = QuestController.instance.get(id);
			if (quest == null) {
				key.append(Component.translatable("quest.notfound").withStyle(TextFormatting.DARK_RED));
			} else {
				key.append(Component.translatable(quest.getCategory().getName() + "/").withStyle(TextFormatting.GRAY))
						.append(Component.literal(quest.getName()).withStyle(TextFormatting.RESET))
						.append(Component.literal(" (").withStyle(TextFormatting.GRAY))
						.append(Component.translatable("availability." + availability.quests.get(id).name().toLowerCase()).withStyle(TextFormatting.BLUE))
						.append(Component.literal(")").withStyle(TextFormatting.GRAY));
			}
			dataIDs.put(key, id);
			dataEnum.put(key, availability.quests.get(id));
		}
		if (isSelect) {
			boolean found = false;
			for (Component line : dataIDs.keySet()) {
				if (line.getString().equals(select.getString())) {
					found= true;
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
		int p = 0;
		if (isSelect) { p = dataEnum.get(select).ordinal(); }
		// type
		addButton(0, guiLeft + 6, guiTop + imageHeight - 46, false, p, types)
				.setSize(100, 20)
				.setHoverTexts("availability.hover.enum.type");
		// select
		addButton(1, guiLeft + 108, guiTop + imageHeight - 46, "availability.select")
				.setSize(180, 20)
				.setHoverTexts("availability.hover.quest");
		// del
		addButton(2, guiLeft + 290, guiTop + imageHeight - 46, "X")
				.setSize(20, 20)
				.setHoverTexts("availability.hover.remove");
		// extra
		addButton(3, guiLeft + imageHeight - 76, guiTop + 192, "availability.more")
				.setSize(70, 20)
				.setIsEnabled(isSelect)
				.setHoverTexts("availability.hover.more");
		updateGuiButtons();
	}

	private void updateGuiButtons() {
		int p = 0;
		getButton(1).setDisplayText("availability.selectquest");
		Quest quest = null;
		boolean isSelect = !select.getFormattedText().isEmpty();
		if (isSelect) {
			quest = QuestController.instance.quests.get(dataIDs.get(select));
			p = dataEnum.get(select).ordinal();
		}
		getButton(0).setDisplay(p)
				.setIsEnabled(isSelect);
		getButton(1).setIsEnabled(p != 0 || !isSelect)
				.setDisplayText(quest == null ? "availability.select" : quest.getName());
		getButton(2).setIsEnabled(isSelect);
	}

	@Override
	public void buttonEvent(GuiButtonNop guiButton) {
		switch (guiButton.id) {
			case 0 : {
				if (!dataIDs.containsKey(select)) { return; }
				EnumAvailabilityQuest ead = EnumAvailabilityQuest.values()[guiButton.getValue()];
				int id = dataIDs.get(select);
				availability.quests.put(id, ead);
				select = Component.literal("ID:" + id + " - ");
				Quest quest = QuestController.instance.get(id);
				if (quest == null) {
					select.append(Component.translatable("quest.notfound").withStyle(TextFormatting.DARK_RED));
				} else {
					select.append(Component.translatable(quest.getCategory().getName() + "/").withStyle(TextFormatting.GRAY))
							.append(Component.literal(quest.getName()).withStyle(TextFormatting.RESET))
							.append(Component.literal(" (").withStyle(TextFormatting.GRAY))
							.append(Component.translatable("availability." + ead.name().toLowerCase()).withStyle(TextFormatting.BLUE))
							.append(Component.literal(")").withStyle(TextFormatting.GRAY));
				}
				initGui();
				break;
			}
			case 1 : {
				setSubGui(new SubGuiQuestSelection(select.getFormattedText().isEmpty() ? 0 : dataIDs.get(select)));
				break;
			}
			case 2 : {
				availability.quests.remove(dataIDs.get(select));
				select = Component.empty();
				initGui();
				break;
			}
			case 3 : { // More
				save();
				initGui();
				break;
			}
			case 66 : onClose(); break;
		}
	}

	@Override
	public void selected(int id, String name) {
		if (id <= 0) { return; }
		if (dataIDs.containsKey(select)) { availability.quests.remove(dataIDs.get(select)); }
		Quest quest = QuestController.instance.quests.get(id);
		if (quest == null) { return; }
		select = Component.literal("ID:" + id + " - ")
				.append(Component.translatable(quest.category.getName() + "/").withStyle(TextFormatting.GRAY))
				.append(Component.literal(quest.getName()).withStyle(TextFormatting.RESET))
				.append(Component.literal(" (").withStyle(TextFormatting.GRAY))
				.append(Component.translatable("availability.after").withStyle(TextFormatting.BLUE))
				.append(Component.literal(")").withStyle(TextFormatting.GRAY));
		availability.quests.put(id, EnumAvailabilityQuest.After);
		initGui();
	}

	// New from Unofficial (BetaZavr)
	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		select = scroll.getNormalSelected();
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		setSubGui(new SubGuiQuestSelection(dataIDs.getOrDefault(select, 0)));
	}

	@Override
	public void onClose() {
		super.onClose();
		for (int id : new ArrayList<>(availability.quests.keySet())) {
			if (availability.quests.get(id) == EnumAvailabilityQuest.Always) { availability.quests.remove(id); }
		}
	}

	@Override
	public void save() {
		if (!dataIDs.containsKey(select)) { return; }
		EnumAvailabilityQuest ead = EnumAvailabilityQuest.values()[getButton(0).getValue()];
		int id = dataIDs.get(select);
		if (ead != EnumAvailabilityQuest.Always) {
			availability.quests.put(id, ead);
			dataEnum.put(select, ead);
		}
		else { availability.quests.remove(id); }
		select = Component.empty();
	}

}
