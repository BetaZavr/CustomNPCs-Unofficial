package noppes.npcs.client.gui.global;

import java.util.*;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.*;
import noppes.npcs.client.gui.questtypes.*;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketQuestMinID;
import noppes.npcs.packets.server.SPacketQuestOpen;
import noppes.npcs.packets.server.SPacketQuestRemove;
import noppes.npcs.packets.server.SPacketQuestSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.shared.client.gui.GuiTextAreaScreen;
import noppes.npcs.client.gui.select.SubGuiQuestSelection;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.shared.client.gui.listeners.GuiSelectionListener;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class SubGuiQuestEdit
		extends GuiNPCInterface
		implements GuiSelectionListener, ITextfieldListener, ICustomScrollListener, IGuiData {

	protected Quest quest;

	// New from Unofficial (BetaZavr)
	protected GuiCustomScrollNop scrollTasks;
	protected Map<Component, QuestObjective> tasksData;
	protected Component selectTask = Component.empty();

	public SubGuiQuestEdit(Quest questIn) {
		super();
		setBackground("menubg.png");
		imageWidth = 386;
		imageHeight = 226;

		quest = questIn;
		NoppesUtilServer.setEditingQuest(player, quest);
	}

	@Override
	public void initGui() {
		super.initGui();
		quest = NoppesUtilServer.getEditingQuest(player);
		quest.questInterface.fix();
		tasksData = quest.questInterface.getKeys();
		NoppesUtilServer.setEditingQuest(player, quest);
		int x0 = guiLeft + 4;
		int x1 = guiLeft + 110;
		int x2 = guiLeft + 172;
		int y = guiTop + 5;
		int w  = 60;
		int lId = 0;
		if (quest.completer.isEmpty() && npc != null) {
			quest.completion = EnumQuestCompletion.Npc;
			quest.completer.reset(npc);
		}
		if (scrollTasks == null) { scrollTasks = addScroll(6).setSize(209, 118); }
		else if (scrollTasks.hasSelected()) { selectTask = scrollTasks.getNormalSelected(); }
		add(scrollTasks.setNormalList(new ArrayList<>(tasksData.keySet()))
				.disabledSearch());

		// name and id
		addLabel(lId++, x0, y + 5, "type.name")
				.setSize(60, 12);
		addTextField(1, x0 + 62, y + 1, 102, 18, quest.getName())
				.setHoverTexts("quest.hover.edit.quest.name");
		// ID
		addLabel(lId++, x2, y + 5, "ID: " + quest.id)
				.setSize(60, 12);
		addButton(24, x2  + 62, y, "gui.reset")
				.setSize(80, 20)
				.setHoverTexts("hover.reset.id");
		// exit
		addButton(66, guiLeft + 361, y, "X")
				.setSize(20, 20)
				.setHoverTexts("hover.back");
		// end text
		addLabel(lId++, x0, (y += 22) + 5, "quest.completedtext")
				.setSize(105, 12);
		addButton(3, x1, y, quest.completeText.isEmpty() ? "selectServer.edit" : "advanced.editingmode")
				.setSize(w, 20)
				.setHoverTexts("quest.hover.edit.quest.completedtext");
		// level
		addLabel(lId++, x2, y + 5, "type.level")
				.setSize(60, 12);
		Object[] lvls = new Object[CustomNpcs.MaxLv + 1];
		lvls[0] = "gui.none";
		for (int g = 1; g <= CustomNpcs.MaxLv; g++) { lvls[g] = g; }
		addButton(23, x2 + 62, y, true, quest.level, lvls)
				.setSize(80, 20)
				.setHoverTexts("quest.hover.edit.quest.level");
		// log text
		addLabel(lId++, x0, (y += 22) +5, "quest.questlogtext")
				.setSize(105, 12);
		addButton(4, x1, y, quest.logText.isEmpty() ? "selectServer.edit" : "advanced.editingmode")
				.setSize(w, 20)
				.setHoverTexts("quest.hover.edit.quest.questlogtext");
		// cancelable
		addButton(22, x2, y, false, quest.isCancelable() ? 0 : 1, "quest.cancelable.true", "quest.cancelable.false")
				.setSize(103, 20)
				.setHoverTexts("quest.hover.edit.quest.cancelable");
		// extra options
		addButton(9, x2 + 105, y, "gui.extraoptions")
				.setSize(104, 20)
				.setColor(0xFF00FFF0)
				.setHoverTexts("quest.hover.edit.quest.extra");
		// reward
		addLabel(lId++, x0, (y += 22) + 5, "quest.reward")
				.setSize(105, 12);
		addButton(5, x1, y, quest.rewardItems.isEmpty() && quest.rewardExp <= 0 ? "selectServer.edit" : "advanced.editingmode")
				.setSize(w, 20)
				.setHoverTexts("quest.hover.edit.quest.reward");
		// cancelable error info
		addLabel(50, x2, y, "quest.has." + (quest.forgetDialogues.length > 0 || quest.forgetQuests.length > 0))
				.setSize(120, 12)
				.setIsVisible(quest.isCancelable());
		// Tasks (?)
		add(getTasksLabel(x2, y +10));
		// task offset
		int pos = -1;
		boolean hasTask = !selectTask.getFormattedText().isEmpty();
		if (hasTask) {
			if (!tasksData.containsKey(selectTask)) {
				scrollTasks.setSelect(-1);
				selectTask = Component.empty(); }
			else {
				scrollTasks.setSelected(selectTask);
				pos = quest.questInterface.getPos(tasksData.get(selectTask));
			}
		}
		addButton(17, x2 + 135, y, "type.down")
				.setSize(36, 20)
				.setIsEnabled(hasTask && pos > -1 && pos < tasksData.size() - 1)
				.setHoverTexts("quest.hover.edit.quest.down");
		addButton(16, x2 + 173, y, "type.up")
				.setSize(36, 20)
				.setIsEnabled(hasTask && pos != 0)
				.setHoverTexts("quest.hover.edit.quest.up");
		// faction
		addLabel(lId++, x0, (y += 22) + 5, "faction.options")
				.setSize(105, 12);
		addButton(10, x1, y, quest.factionOptions.hasOptions() ? "advanced.editingmode" : "selectServer.edit")
				.setSize(w, 20)
				.setHoverTexts("quest.hover.edit.quest.faction");
		// tasks
		scrollTasks.setPos(x2, y);
		// command
		addLabel(lId++, x0, (y += 22) + 5, "advMode.command")
				.setSize(105, 12);
		addButton(15, x1, y, quest.command.isEmpty() ? "selectServer.edit" : "advanced.editingmode")
				.setSize(w, 20)
				.setHoverTexts("quest.hover.edit.quest.command");
		// repeat
		addLabel(lId, x0, (y += 32) + 5, "gui.repeatable")
				.setSize(61, 12);
		addButton(8, x0 + 62, y, true, quest.repeat.ordinal(),
				"gui.no", "gui.yes", "quest.mcdaily", "quest.mcweekly", "quest.rldaily", "quest.rlweekly")
				.setSize(103, 20)
				.setHoverTexts("quest.hover.edit.quest.repeat");
		// mail
		GuiButtonNop button = addButton(13, x0, y += 22, "mailbox.setup")
				.setSize(144, 20)
				.setHoverTexts("quest.hover.edit.quest.mail");
		if (!quest.mail.title.isEmpty()) { button.setDisplayText(quest.mail.title); }
		addButton(14, x0 + 146, y, "X")
				.setSize(20, 20)
				.setHoverTexts("quest.hover.edit.quest.del.mail");
		// next quest
		button = addButton(11, x0, y += 22, "quest.next")
				.setSize(144, 20);
		if (quest.nextQuestId != -1) {
			if (!quest.nextQuestTitle.isEmpty()) { button.setDisplayText(quest.nextQuestTitle); }
			Quest q = QuestController.instance.quests.get(quest.nextQuestId);
			Component hover = Component.empty().append(Component.translatable("quest.hover.edit.quest.next"));
			if (q != null) {
				hover.append(Component.literal("<br>"))
						.append(Component.translatable(q.category.title).withStyle(TextFormatting.GRAY))
						.append(Component.literal("/").withStyle(TextFormatting.GRAY))
						.append(Component.translatable(q.title).withStyle(TextFormatting.RESET));
			}
			button.setHoverTexts(hover);
		}
		else { button.setHoverTexts("quest.hover.edit.quest.next"); }

		addButton(12, x0 + 146, y, "X")
				.setSize(20, 20)
				.setHoverTexts("quest.hover.edit.quest.del.next");
		// tasks settings
		addButton(18, x2, y, false, quest.step, "attribute.slot.0", "quest.task.step.1", "quest.task.step.2")
				.setSize(51, 20)
				.setIsEnabled(!tasksData.isEmpty())
				.setHoverTexts("quest.hover.edit.quest.step");
		addButton(19, x2 += 53, y, "gui.add")
				.setSize(51, 20)
				.setIsEnabled(tasksData.size() < 9)
				.setHoverTexts("quest.hover.edit.quest.add");
		addButton(20, x2 += 53, y, "gui.remove")
				.setSize(51, 20)
				.setIsEnabled(scrollTasks.hasSelected())
				.setHoverTexts("quest.hover.edit.quest.del");
		addButton(21, x2 + 53, y, "selectServer.edit")
				.setSize(50, 20)
				.setIsEnabled(hasTask)
				.setHoverTexts("quest.hover.edit.quest.edit");
	}

	@Override
	public void buttonEvent(GuiButtonNop guiButton) {
		switch (guiButton.id) {
			case 3: setSubGui(new GuiTextAreaScreen(0, quest.completeText)); break; // end text
			case 4: setSubGui(new GuiTextAreaScreen(1, quest.logText)); break; // log text
			case 5: {
				SubGuiNpcQuestTypeItem.parent = this;
				setSubGui(new SubGuiNpcQuestReward(quest));
				break;
			} // reward
			case 8: quest.repeat = EnumQuestRepeat.values()[guiButton.getValue()]; break; // reiteration
			case 9: setSubGui(new SubGuiNpcQuestExtra(quest)); break; // NPC to End
			case 10: setSubGui(new SubGuiNpcFactionOptions(quest.factionOptions)); break; // faction
			case 11: setSubGui(new SubGuiQuestSelection(quest.nextQuestId)); break; // next quest
			case 12: quest.nextQuestId = -1; initGui(); break; // remove next quest
			case 13: setSubGui(new SubGuiMailmanSendSetup(quest.mail)); break; // edit mail
			case 14: quest.mail = new PlayerMail(); initGui(); break; // remove mail
			case 15: setSubGui(new SubGuiNpcCommand(quest.command)); break; // command
			case 16: {
				quest.questInterface.upPos(tasksData.get(scrollTasks.getNormalSelected()));
				save();
				initGui();
				break;
			} // up
			case 17: {
				quest.questInterface.downPos(tasksData.get(scrollTasks.getNormalSelected()));
				save();
				initGui();
				break;
			} // down
			case 18: quest.step = guiButton.getValue(); break; // type step task
			case 19: setSubGui(new SubGuiQuestObjectiveSelect(this)); break; // add task
			case 20: {
				if (quest.questInterface.removeTask(tasksData.get(scrollTasks.getNormalSelected()))) {
					selectTask = Component.empty();
					save();
				}
				initGui();
				break;
			} // remove task
			case 21: {
				if (selectTask.getFormattedText().isEmpty() || !tasksData.containsKey(selectTask)) { return; }
				QuestObjective questObjective = tasksData.get(selectTask);
				switch (tasksData.get(selectTask).getEnumType()) {
					case DIALOG: setSubGui(new SubGuiNpcQuestTypeDialog(npc, questObjective, this)); break;
					case KILL:
					case AREAKILL: setSubGui(new SubGuiNpcQuestTypeKill(npc, questObjective, this)); break;
					case LOCATION: setSubGui(new SubGuiNpcQuestTypeLocation(npc, questObjective, this)); break;
					case MANUAL: setSubGui(new SubGuiNpcQuestTypeManual(npc, questObjective, this)); break;
					default: {
						SubGuiNpcQuestTypeItem.parent = this;
						Packets.sendServer(new SPacketQuestOpen(EnumGuiType.QuestTypeItem, quest.save(new NBTTagCompound()), quest.questInterface.getPos(questObjective)));
						break;
					} // ITEM or CRAFT
				}
				break;
			} // edit task
			case 22: {
				quest.setCancelable(guiButton.getValue() == 0);
				getLabel(50).setIsVisible(quest.isCancelable());
				if (quest.isCancelable()) {
					if (quest.forgetDialogues.length == 0) {
						TreeMap<Integer, Dialog> dialogs = DialogController.instance.dialogs;
						for (int id : dialogs.keySet()) {
							if (dialogs.get(id).quest == quest.id) {
								quest.forgetDialogues = new int[] { id };
								break;
							}
						}
					}
					if (quest.forgetQuests.length == 0) {
						TreeMap<Integer, Quest> quests = QuestController.instance.quests;
						for (int id : quests.keySet()) {
							if (id != quest.id && quests.get(id).nextQuestId == quest.id) {
								quest.forgetQuests = new int[] { id };
								break;
							}
						}
					}
					String[] texts = new String[] { "", "" };
					int i = 0;
					for (int id : quest.forgetDialogues) {
						texts[0] += id;
						if (i < quest.forgetDialogues.length - 1) {
							texts[0] += ",";
						}
						i++;
					}
					i = 0;
					for (int id : quest.forgetQuests) {
						texts[1] += id;
						if (i < quest.forgetQuests.length - 1) {
							texts[1] += ",";
						}
						i++;
					}
					SubGuiEditText subgui = new SubGuiEditText(1, texts);
					subgui.setHoverTexts(Component.translatable("quest.hover.forget.dialogues"),
							Component.translatable("quest.hover.forget.quests"));
					setSubGui(subgui);
				}
				break;
			} // cancelable
			case 23: quest.level = guiButton.getValue(); break; // level
			case 24: {
				ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
					if (bo) { Packets.sendServer(new SPacketQuestMinID(quest.id)); }
					NoppesUtil.openGUI(player, this);
				},
						Component.translatable("message.change.id", "" + quest.id).getParent(),
						Component.translatable("message.change").getParent());
				setScreen(guiYesNo);
				break;
			} // reset ID
			case 66: onClose(); break; // exit
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (textField.id == 1) {
			StringBuilder t = new StringBuilder(textField.getValue());
			boolean has = true;
			while (has) {
				has = false;
				for (Quest qes : quest.category.quests.values()) {
					if (qes.id != quest.id && qes.title.equalsIgnoreCase(t.toString())) {
						has = true;
						break;
					}
				}
				if (has) { t.append("_"); }
			}
			quest.setName(t.toString());
		}
		initGui();
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof GuiTextAreaScreen) {
			GuiTextAreaScreen gui = (GuiTextAreaScreen) subgui;
			if (gui.id == 0) { quest.completeText = gui.text; }
			else if (gui.id == 1) { quest.logText = gui.text; }
		}
		else if (subgui instanceof SubGuiNpcCommand) { quest.command = ((SubGuiNpcCommand) subgui).command; }
		else if (subgui instanceof SubGuiEditText && ((SubGuiEditText) subgui).text.length == 2) {
			String dialogIDs = ((SubGuiEditText) subgui).text[0];
			String questIDs = ((SubGuiEditText) subgui).text[1];
			while (dialogIDs.contains(" ")) { dialogIDs = dialogIDs.replace(" ", ""); }
			while (questIDs.contains(" ")) { questIDs = questIDs.replace(" ", ""); }
			java.util.List<Integer> vdt = new ArrayList<>();
			for (String td : dialogIDs.split(",")) {
				try {
					int id = Integer.parseInt(td);
					if (!vdt.contains(id)) { vdt.add(id); }
				} catch (NumberFormatException ignored) { }
			}
			Collections.sort(vdt);
			quest.forgetDialogues = new int[vdt.size()];
			int i = 0;
			for (int id : vdt) { quest.forgetDialogues[i] = id; i++; }

			List<Integer> vqt = new ArrayList<>();
			for (String tq : questIDs.split(",")) {
				try {
					int id = Integer.parseInt(tq);
					if (!vqt.contains(id)) { vqt.add(id); }
				} catch (NumberFormatException ignored) { }
			}
			Collections.sort(vqt);
			quest.forgetQuests = new int[vqt.size()];
			i = 0;
			for (int id : vqt) { quest.forgetQuests[i] = id; i++; }
		}
		initGui();
	}

	@Override
	public void selected(int id, String name) {
		quest.nextQuestId = id;
		quest.nextQuestTitle = name;
		initGui();
	}

	@Override
	public void save() {
		GuiTextFieldNop.unfocus();
		Packets.sendServer(new SPacketQuestSave(quest.category.id, quest.save(new NBTTagCompound())));
	}

	// New from Unofficial (BetaZavr)
	private GuiLabel getTasksLabel(int x, int y) {
		String[] isGet = new String[] {"2", "", ""};
		TreeMap<Integer, Dialog> dialogs = DialogController.instance.dialogs;
		for (int id : dialogs.keySet()) {
			if (dialogs.get(id).quest == quest.id) {
				isGet = new String[] { "0", "" + id, ((char) 167) + "8" + dialogs.get(id).category.title + "/" + ((char) 167) + "r" + dialogs.get(id).title };
				break;
			}
		}
		Component task;
		if (isGet[0].equals("2")) {
			task = Component.literal("[?]").withStyle(TextFormatting.DARK_RED, TextFormatting.BOLD);
			TreeMap<Integer, Quest> quests = QuestController.instance.quests;
			for (int id : quests.keySet()) {
				if (id != quest.id && quests.get(id).nextQuestId == quest.id) {
					isGet = new String[] { "1", "" + id, ((char) 167) + "8" + quests.get(id).category.title + "/" + ((char) 167) + "r" + quests.get(id).getTitle() };
					break;
				}
			}
		}
		else { task = Component.literal("[?]").withStyle(TextFormatting.GREEN, TextFormatting.BOLD); }
		return new GuiLabel(this, 51, Component.translatable("gui.tasks").append(task), x, y)
				.setSize(120, 10)
				.setHoverTexts(Component.translatable("quest.hover.edit.quest.tasks",
						Component.translatable("quest.hover.edit.is.get." + isGet[0], isGet[1], isGet[2])));
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (!scroll.hasSelected()) { return; }
		if (scroll.id == 6) { selectTask = scroll.getNormalSelected(); }
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 6) {
			if (selectTask.getFormattedText().isEmpty() || !tasksData.containsKey(selectTask)) { return; }
			QuestObjective questObjective = tasksData.get(selectTask);
			switch (tasksData.get(selectTask).getEnumType()) {
				case DIALOG: setSubGui(new SubGuiNpcQuestTypeDialog(npc, questObjective, this)); break;
				case KILL:
				case AREAKILL: setSubGui(new SubGuiNpcQuestTypeKill(npc, questObjective, this)); break;
				case LOCATION: setSubGui(new SubGuiNpcQuestTypeLocation(npc, questObjective, this)); break;
				case MANUAL: setSubGui(new SubGuiNpcQuestTypeManual(npc, questObjective, this)); break;
				default: {
					SubGuiNpcQuestTypeItem.parent = this;
					Packets.sendServer(new SPacketQuestOpen(EnumGuiType.QuestTypeItem, quest.save(new NBTTagCompound()), quest.questInterface.getPos(questObjective)));
					break;
				} // ITEM or CRAFT
			}
		}
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound != null && compound.hasKey("MinimumID", 3) && quest.id != compound.getInteger("MinimumID")) {
			Packets.sendServer(new SPacketQuestRemove(quest.id));
			quest.id = compound.getInteger("MinimumID");
			save();
			initGui();
		}
	}

}
