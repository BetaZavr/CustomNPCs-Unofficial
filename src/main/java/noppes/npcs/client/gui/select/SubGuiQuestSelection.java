package noppes.npcs.client.gui.select;

import java.util.HashMap;

import com.google.common.collect.Lists;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestCategory;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.GuiSelectionListener;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

public class SubGuiQuestSelection extends GuiBasic implements ICustomScrollListener {

	protected final HashMap<String, QuestCategory> categoryData = new HashMap<>();
	protected final HashMap<String, Quest> questData = new HashMap<>();
	protected GuiCustomScrollNop scrollCategories;
	protected GuiCustomScrollNop scrollQuests;
	protected QuestCategory selectedCategory;
	protected GuiSelectionListener listener;
	public Quest selectedQuest;

	public SubGuiQuestSelection(int questId) {
		super();
		setBackground("menubg.png");
		drawDefaultBackground = false;
		imageWidth = 366;
		imageHeight = 226;

		selectedQuest = QuestController.instance.quests.get(questId);
		if (selectedQuest != null) { selectedCategory = selectedQuest.category; }
	}

	@Override
	public void initGui() {
		super.initGui();
		if (wrapper.parent instanceof GuiSelectionListener) { listener = (GuiSelectionListener) wrapper.parent; }
		addLabel(0, guiLeft + 8, guiTop + 4, "gui.categories");
		addLabel(1, guiLeft + 175, guiTop + 4, "quest.quests");
		addButton(2, guiLeft + imageWidth - 22, guiTop + 4, "X")
				.setSize(12, 12);
		categoryData.clear();
		for (QuestCategory category : QuestController.instance.categories.values()) { categoryData.put(category.title, category); }
		questData.clear();
		if (selectedCategory != null) {
			for (Quest quest : selectedCategory.quests.values()) { questData.put(quest.title, quest); }
		}
		if (scrollCategories == null) { scrollCategories = addScroll(0).setSize(170, 200); }
		scrollCategories.setList(Lists.newArrayList(categoryData.keySet()));
		if (selectedCategory != null) { scrollCategories.setSelected(selectedCategory.title); }
		add(scrollCategories.setPos(guiLeft + 4, guiTop + 14));

		if (scrollQuests == null) { scrollQuests = addScroll(1).setSize(170, 200); }
		scrollQuests.setList(Lists.newArrayList(questData.keySet()));
		if (selectedQuest != null) { scrollQuests.setSelected(selectedQuest.title); }
		add(scrollQuests.setPos(guiLeft + 175, guiTop + 14));
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (button.id == 2) {
			if (selectedQuest != null) { scrollDoubleClicked(null); }
			else { onClose(); }
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0) {
			selectedCategory = categoryData.get(scrollCategories.getSelected());
			selectedQuest = null;
			scrollQuests.clearSelection();
		}
		if (scroll.id == 1) { selectedQuest = questData.get(scrollQuests.getSelected()); }
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (selectedQuest != null) {
			if (listener != null) { listener.selected(selectedQuest.id, selectedQuest.title); }
			onClose();
		}
	}

}
