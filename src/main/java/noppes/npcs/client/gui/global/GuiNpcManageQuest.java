package noppes.npcs.client.gui.global;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.drop.SubGuiDropEdit;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestCategory;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketQuestCategoryRemove;
import noppes.npcs.packets.server.SPacketQuestCategorySave;
import noppes.npcs.packets.server.SPacketQuestRemove;
import noppes.npcs.packets.server.SPacketQuestSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

public class GuiNpcManageQuest
		extends GuiNPCInterface2
		implements ICustomScrollListener {

	public static GuiNPCInterface Instance;
	protected HashMap<Component, QuestCategory> categoryData = new HashMap<>();
	protected HashMap<Component, Quest> questData = new HashMap<>();
	protected GuiCustomScrollNop scrollCategories;
	protected GuiCustomScrollNop scrollQuests;

	// New from Unofficial (BetaZavr)
	private static boolean sortByName = true;
	private Quest copyQuest = null;
	private Component selectedCategory = Component.empty();
	private Component selectedQuest = Component.empty();

	public GuiNpcManageQuest(EntityNPCInterface npc) {
		super(npc);
		Instance = this;

		backGui = EnumGuiType.MainMenuGlobal;
		// all data getting in SyncController
	}

	@Override
	public void initGui() {
		super.initGui();
		categoryData.clear();
		questData.clear();
		QuestController qData = QuestController.instance;
		LinkedHashMap<Integer, List<Component>> hts= new LinkedHashMap<>();
		for (QuestCategory category : qData.categories.values()) {
			Component key = category.getTitle();
			categoryData.put(key, category);
			if (selectedCategory.getFormattedText().isEmpty() || selectedCategory.getString().equals(key.getString())) { selectedCategory = key; }
		}
		boolean hasCategory = !selectedCategory.getFormattedText().isEmpty();
		if (hasCategory) {
			if (categoryData.containsKey(selectedCategory)) {
				Map<Component, Quest> map = new LinkedHashMap<>();
				for (Quest quest : new ArrayList<>(categoryData.get(selectedCategory).quests.values())) {
					map.put(quest.getLineKey(), quest);
				}
				List<Map.Entry<Component, Quest>> list = getEntryList(map);
				for (Map.Entry<Component, Quest> entry : list) {
					questData.put(entry.getKey(), entry.getValue());
					if (selectedQuest.getFormattedText().isEmpty()) {
						selectedQuest = entry.getKey();
					}
				}
			}
			else {
				selectedCategory = Component.empty();
				selectedQuest = Component.empty();
			}
			// Hover Text:
			if (!questData.isEmpty()) {
				int pos = 0;
				DialogController dData = DialogController.instance;
				for (Quest quest : questData.values()) {
					hts.put(pos++, getStrings(quest, qData, dData));
				}
			}
		}
		if (hasCategory && !categoryData.containsKey(selectedCategory)) { selectedCategory = Component.empty(); }
		boolean hasQuest = !selectedQuest.getFormattedText().isEmpty();
		if (hasQuest && !questData.containsKey(selectedQuest)) { selectedQuest = Component.empty(); }
		// scroll info
		addLabel(0, guiLeft + 8, guiTop + 4, "gui.categories");
		addLabel(1, guiLeft + 180, guiTop + 4, "quest.quests");
		// quest buttons
		int x = guiLeft + 350, y = guiTop + 8;
		addLabel(3, guiLeft + 356, guiTop + 8, "quest.quests");
		addButton(13, x, y += 10, "selectServer.edit")
				.setSize(64, 15)
				.setIsEnabled(hasQuest)
				.setHoverTexts("manager.hover.quest.edit", selectedQuest);
		addButton(12, x, y += 17, "gui.remove")
				.setSize(64, 15)
				.setIsEnabled(hasQuest)
				.setHoverTexts("manager.hover.quest.del", selectedQuest);
		addButton(11, x, y += 17, "gui.add")
				.setSize(64, 15)
				.setIsEnabled(hasCategory)
				.setHoverTexts("manager.hover.quest.add", selectedCategory);
		addButton(10, x, y += 21, "gui.copy")
				.setSize(64, 15)
				.setIsEnabled(hasQuest)
				.setHoverTexts("manager.hover.quest.copy", selectedQuest);
		addButton(9, x, y += 17, "gui.paste")
				.setSize(64, 15)
				.setIsEnabled(copyQuest != null)
				.setHoverTexts("manager.hover.quest.paste." + (copyQuest != null), copyQuest != null ? copyQuest.getLineKey() : "");
		GuiButtonNop checkBox = addCheckBox(14, x, y + 17, "gui.name", "ID", sortByName)
				.setSize(64, 15);
		checkBox.setHoverTexts(Component.translatable("hover.sort",
				Component.translatable("dialog.dialogs").getFormattedText(),
				checkBox.getMessage().getFormattedText()));

		// category buttons
		y = guiTop + 140;
		addLabel(2, x + 2, y, "gui.categories");
		addButton(3, x, y += 10, "selectServer.edit")
				.setSize(64, 15)
				.setIsEnabled(hasCategory)
				.setHoverTexts("manager.hover.category.edit");
		addButton(2, x, y += 17, "gui.remove")
				.setSize(64, 15)
				.setIsEnabled(hasCategory)
				.setHoverTexts("manager.hover.category.del");
		addButton(1, x, y + 17, "gui.add")
				.setSize(64, 15)
				.setHoverTexts("manager.hover.category.add");

		if (scrollCategories == null) { scrollCategories = addScroll(0).setSize(170, 198); }
		if (hasCategory) { scrollCategories.setSelected(selectedCategory); }
		scrollCategories.setNormalList(new ArrayList<>(categoryData.keySet()))
				.setPos( guiLeft + 4, guiTop + 15);
		add(scrollCategories);

		if (scrollQuests == null) { scrollQuests = addScroll(1).setSize(170, 198); }
		if (hasQuest) { scrollQuests.setSelected(selectedQuest); }
		scrollQuests.setUnsortedList(new ArrayList<>(questData.keySet()))
				.setHoverTexts(hts)
				.setPos(guiLeft + 176, guiTop + 15);
		add(scrollQuests);
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 1: {
				setSubGui(new SubGuiEditText(1, Component.translatable("gui.new").getFormattedText()));
				break;
			} // new category
			case 2: {
				if (!categoryData.containsKey(selectedCategory)) { return; }
				ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
					if (bo) { Packets.sendServer(new SPacketQuestCategoryRemove(categoryData.get(selectedCategory).id)); }
					NoppesUtil.openGUI(player, this);
				},
						selectedCategory.getParent(),
						Component.translatable("message.delete").getParent());
				setScreen(guiYesNo);
				break;
			} // remove category
			case 3: {
				if (!categoryData.containsKey(selectedCategory)) { return; }
				setSubGui(new SubGuiEditText(3, categoryData.get(selectedCategory).title));
				break;
			} // rename category
			case 9: {
				if (copyQuest == null || !categoryData.containsKey(selectedCategory)) { return; }
				Quest quest = copyQuest.copy();
				quest.id = -1;
				quest.category = categoryData.get(selectedCategory);
				StringBuilder t = new StringBuilder(quest.title);
				boolean has = true;
				while (has) {
					has = false;
					for (Quest q : quest.category.quests.values()) {
						if (quest.id != q.id && q.title.equalsIgnoreCase(t.toString())) {
							has = true;
							break;
						}
					}
					if (has) { t.append("_"); }
				}
				quest.setName(t.toString());
				selectedQuest = quest.getLineKey();
				Packets.sendServer(new SPacketQuestSave(quest.category.id, quest.save(new NBTTagCompound())));
				initGui();
				break;
			} // paste
			case 10: {
				if (!questData.containsKey(selectedQuest)) { return; }
				copyQuest = questData.get(selectedQuest);
				initGui();
				break;
			} // copy
			case 11: {
				setSubGui(new SubGuiEditText(11, Component.translatable("gui.new").getString()));
				break;
			} // new quest
			case 12: {
				if (!questData.containsKey(selectedQuest)) { return; }
				ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
					if (bo) { Packets.sendServer(new SPacketQuestRemove(questData.get(selectedQuest).id)); }
					NoppesUtil.openGUI(player, this);
				},
						selectedQuest.getParent(),
						Component.translatable("message.delete").getParent());
				setScreen(guiYesNo);
				break;
			} // remove quest
			case 13: {
				if (!questData.containsKey(selectedQuest)) { return; }
				SubGuiDropEdit.parent = this;
				SubGuiDropEdit.parentContainer = null;
				SubGuiDropEdit.parentData = null;
				setSubGui(new SubGuiQuestEdit(questData.get(selectedQuest)));
				break;
			} // edit quest
			case 14: {
				sortByName = ((GuiCheckBoxNop) button).selected();
				initGui();
				break;
			} // sort
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (!hasSubGui()) {
			drawHorizontalLine(guiLeft + 348, guiLeft + 414, guiTop + 128, new Color(0x80000000).getRGB());
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiEditText && !((SubGuiEditText) subgui).cancelled) {
			SubGuiEditText editText = (SubGuiEditText) subgui;
			if (editText.id == 1) {
				QuestCategory category = new QuestCategory();
				StringBuilder t = new StringBuilder(editText.text[0]);
				boolean has = true;
				while (has) {
					has = false;
					for (QuestCategory cat : new ArrayList<>(QuestController.instance.categories.values())) {
						if (category.id != cat.id && cat.title.equalsIgnoreCase(t.toString())) {
							has = true;
							break;
						}
					}
					if (has) { t.append("_"); }
				}
				category.title = t.toString();
				Packets.sendServer(new SPacketQuestCategorySave(category.save(new NBTTagCompound())));
			} // create category
			else if (editText.id == 3) {
				if (editText.text[0].isEmpty() || !categoryData.containsKey(selectedCategory)) { return; }
				QuestCategory category = categoryData.get(selectedCategory);
				if (category.getName().equals(editText.text[0])) { return; }
				StringBuilder t = new StringBuilder(editText.text[0]);
				boolean has = true;
				while (has) {
					has = false;
					for (QuestCategory cat : QuestController.instance.categories.values()) {
						if (category.id != cat.id && cat.title.equalsIgnoreCase(t.toString())) {
							has = true;
							break;
						}
					}
					if (has) { t.append("_"); }
				}
				category.title = t.toString();
				NBTTagCompound compound = category.save(new NBTTagCompound());
				selectedCategory = Component.translatable(category.title);
				Packets.sendServer(new SPacketQuestCategorySave(compound));
			} // rename category
			else if (editText.id == 11) {
				if (editText.text[0].isEmpty() || !categoryData.containsKey(selectedCategory)) { return; }
				QuestCategory category = categoryData.get(selectedCategory);
				Quest quest = new Quest(category);
				StringBuilder t = new StringBuilder(editText.text[0]);
				boolean has = true;
				while (has) {
					has = false;
					for (Quest qet : new ArrayList<>(quest.category.quests.values())) {
						if (qet.id != quest.id && qet.title.equalsIgnoreCase(t.toString())) {
							has = true;
							break;
						}
					}
					if (has) { t.append("_"); }
				}
				quest.title = t.toString();
				Packets.sendServer(new SPacketQuestSave(category.id, quest.save(new NBTTagCompound())));
			} // create quest
		}
		if (subgui instanceof SubGuiQuestEdit) { initGui(); }
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (!scroll.hasSelected()) { return; }
		if (scroll.id == 0) {
			if (selectedCategory.getFormattedText().equals(scroll.getSelected())) { return; }
			selectedCategory = scrollCategories.getNormalSelected();
			selectedQuest = Component.empty();
			scrollQuests.clearSelection();
		}
		else if (scroll.id == 1) {
			if (selectedQuest.getFormattedText().equals(scroll.getSelected())) { return; }
			selectedQuest = scrollQuests.getNormalSelected();
		}
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0 && categoryData.containsKey(selectedCategory)) { setSubGui(new SubGuiEditText(3, categoryData.get(selectedCategory).title)); }
		else if (scroll.id == 1 && questData.containsKey(selectedQuest)) {
			SubGuiDropEdit.parent = this;
			SubGuiDropEdit.parentContainer = null;
			SubGuiDropEdit.parentData = null;
			setSubGui(new SubGuiQuestEdit(questData.get(selectedQuest)));
		}
	}

	@Override
	public void save() { GuiTextFieldNop.unfocus(); }

	// New from Unofficial (BetaZavr)
	private static List<Map.Entry<Component, Quest>> getEntryList(Map<Component, Quest> map) {
		List<Map.Entry<Component, Quest>> list = new ArrayList<>(map.entrySet());
		list.sort((d_0, d_1) -> {
			if (sortByName) {
				String n_0 = (Component.translatable(d_0.getValue().title).getString() + "_" + d_0.getValue().id).toLowerCase();
				String n_1 = (Component.translatable(d_1.getValue().title).getString() + "_" + d_1.getValue().id).toLowerCase();
				return n_0.compareTo(n_1);
			} else {
				return Integer.compare(d_0.getValue().id, d_1.getValue().id);
			}
		});
		return list;
	}

	private List<Component> getStrings(Quest quest, QuestController qData, DialogController dData) {
		List<Component> h = new ArrayList<>();
		List<Component> quests = new ArrayList<>();
		List<Component> dialogs = new ArrayList<>();
		h.add(Component.translatable(quest.title).append(":"));
		for (Quest q : qData.quests.values()) {
			if (q.nextQuestId != quest.id) { continue; }
			quests.add(Component.empty()
					.append(Component.literal("ID:" + q.id).withStyle(TextFormatting.GRAY))
					.append(Component.literal(q.category.getName() + "/").withStyle(TextFormatting.DARK_GRAY))
					.append(Component.literal(q.getName()).withStyle(TextFormatting.RESET)));
		}
		for (Dialog d : dData.dialogs.values()) {
			if (d.quest != quest.id) {
				continue;
			}
			dialogs.add(Component.empty()
					.append(Component.literal("ID:" + d.id).withStyle(TextFormatting.GRAY))
					.append(Component.literal(d.category.getName() + "/").withStyle(TextFormatting.DARK_GRAY))
					.append(Component.literal(d.getName()).withStyle(TextFormatting.RESET)));
		}

		if (quests.isEmpty() && dialogs.isEmpty()) {
			h.add(Component.translatable("quest.hover.quest.0"));
		}

		if (!quests.isEmpty()) {
			h.add(Component.translatable("quest.hover.in.quests"));
			h.addAll(quests);
		} else {
			h.add(Component.translatable("quest.hover.quest.1"));
		}

		if (!dialogs.isEmpty()) {
			h.add(Component.translatable("quest.hover.in.dialogs"));
			h.addAll(dialogs);
		} else {
			h.add(Component.translatable("quest.hover.quest.2"));
		}
		return h;
	}

}
