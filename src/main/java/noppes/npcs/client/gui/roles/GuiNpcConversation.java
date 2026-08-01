package noppes.npcs.client.gui.roles;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.gui.availability.SubGuiNpcAvailability;
import noppes.npcs.client.gui.select.SubGuiQuestSelection;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcJobSave;
import noppes.npcs.roles.JobConversation;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.GuiSelectionListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

// Changed by Unofficial (BetaZavr)
public class GuiNpcConversation extends GuiNPCInterface2
		implements ITextfieldListener, GuiSelectionListener {

	protected final JobConversation job;
	protected final Map<Component, Integer> data = new LinkedHashMap<>();
	protected GuiCustomScrollNop scroll;
	protected Component select = Component.empty();

	public GuiNpcConversation(EntityNPCInterface npc) {
		super(npc);

		backGui = EnumGuiType.MainMenuAdvanced;
		job = (JobConversation) npc.job;
	}

	@Override
	public void initGui() {
		super.initGui();
		int lId = 0;
		int x0 = guiLeft + 5;
		int y = guiTop + 15;
		int sW = 205;
		addLabel(lId++, x0 + 1, y - 10, Component.translatable("conversation.line").append(":"))
				.setSize(100, 10);
		addLabel(lId++, x0 + 165, y - 10, "_[?]_")
				.setSize(25, 10)
				.setHoverTexts("job.conversation.hover.info");
		data.clear();
		// create 0 line
		job.getLine(0);
		// data
		for (Map.Entry<Integer, JobConversation.ConversationLine> entry : job.lines.entrySet()) {
			Component key = Component.empty()
					.append(Component.literal("ID:").withStyle(TextFormatting.GRAY))
					.append(Component.literal("" + entry.getKey()).withStyle(TextFormatting.YELLOW))
					.append(Component.literal("; NPC:\"").withStyle(TextFormatting.GRAY));
			// npc
			if (entry.getValue().npc.isEmpty()) {
				key.append(Component.translatable("type.empty").withStyle(TextFormatting.RED));
			}
			else { key.append(Component.literal(entry.getValue().npc).withStyle(TextFormatting.RESET)); }
			key.append(Component.literal("\"; Text: ").withStyle(TextFormatting.GRAY));
			// text
			String textLine = entry.getValue().getText();
			if (textLine.isEmpty()) {
				key.append(Component.translatable("type.empty").withStyle(TextFormatting.RED));
			}
			else {
				if (font.getStringWidth(key.getFormattedText() + textLine) <= sW - 5) { key.append(Component.literal("...").withStyle(TextFormatting.GRAY)); }
				else {
					StringBuilder text = new StringBuilder();
					for (int i = 0; i < textLine.length() && font.getStringWidth(key.getFormattedText() + text) < sW - 20; i++) { text.append(textLine.charAt(i)); }
					key.append(Component.literal(text + "...").withStyle(TextFormatting.RESET));
				}
			}
			data.put(key, entry.getKey());
			if (select.getFormattedText().isEmpty()) { select = key; }
		}
		if (scroll == null) { scroll = addScroll(0).setSize(sW, 197); }
		add(scroll.setPos(x0, y)
				.setUnsortedList(new ArrayList<>(data.keySet()))
				.setSelected(select));
		if (scroll.hasSelected() && data.containsKey(scroll.getNormalSelected())) {
			JobConversation.ConversationLine line = job.getLine(data.get(scroll.getNormalSelected()));
			int x1 = x0 + sW + 2;
			int x2 = x1 + 132;
			// name
			addLabel(lId++, x1, y - 10, Component.translatable("gui.name").append(" NPC:"))
					.setSize(200, 10);
			addTextField(0, x1 + 1, y + 1, 200, 18, line.npc)
					.setHoverTexts("job.conversation.hover.npc.name");
			// message
			addLabel(lId++, x1, (y += 23) + 3, Component.translatable("parameter.iline.text").append(":"))
					.setSize(130, 10);
			addButton(0, x2, y, "selectServer.edit")
					.setSize(70, 20)
					.setHoverTexts("job.conversation.hover.edit.message");
			// delay
			y += 24;
			if (scroll.getSelectedIndex() > 0) {
				addLabel(lId++, x1, y + 3, Component.translatable("conversation.delay").append(":"))
						.setSize(120, 10);
				addTextField(1, x2 + 1, y, 68, 18, line.delay)
						.setMinMaxDefault(5, 1000, 40)
						.setHoverTexts("job.conversation.hover.delay");
			}
			// quest
			y = guiTop + imageHeight - 73;
			x2 = x1 + 72;
			addLabel(lId++, x1, y + 5, Component.translatable("quest.quest").append(":"))
					.setSize(70, 10);
			Component qTitle = Component.translatable("gui.select");
			Quest quest = QuestController.instance.get(job.quest);
			if (quest != null) { qTitle = quest.getLineKey(); }
			addButton(1, x2, y, qTitle)
					.setSize(108, 20)
					.setHoverTexts("job.conversation.hover.quest");
			addButton(2, x2 + 110, y, "X")
					.setSize(20, 20)
					.setIsEnabled(quest != null)
					.setHoverTexts(Component.translatable("manager.hover.quest.del", quest != null ? quest.title : ""));
			// quest delay
			addLabel(lId++, x1, (y += 22) + 5, Component.translatable("conversation.delay").append(":"))
					.setSize(70, 10);
			addTextField(2, x2 + 1, y, 68, 18, job.generalDelay)
					.setMinMaxDefault(10, 1000000, 400)
					.setIsEnabled(quest != null)
					.setHoverTexts("job.conversation.hover.quest.delay");
			// availability
			addLabel(lId++, x1, (y += 22) + 5, Component.translatable("availability.name").append(":"))
					.setSize(70, 10);
			addButton(3, x2, y, "selectServer.edit")
					.setSize(130, 20)
					.setHoverTexts("availability.hover");
			// range
			addLabel(lId, x1, (y += 22) + 5, Component.translatable("gui.range").append(":"))
					.setSize(70, 10);
			addTextField(3, x2 + 1, y + 1, 30, 18, job.range)
					.setMinMaxDefault(4, 60, 20)
					.setIsEnabled(!job.mode)
					.setHoverTexts("job.conversation.hover.range");
			// type
			addButton(4, x2 + 34, y, false, job.mode ? 0 : 1, "gui.always", "gui.playernearby")
					.setSize(96, 20)
					.setHoverTexts(Component.translatable("job.conversation.hover.type",
							Component.translatable("gui.always").getFormattedText(),
							Component.translatable("gui.playernearby").getFormattedText()));
		}
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				if (scroll.hasSelected() && data.containsKey(scroll.getNormalSelected())) {
					JobConversation.ConversationLine line = job.getLine(data.get(scroll.getNormalSelected()));
					setSubGui(new SubGuiNpcConversationLine(line.getText(), line.getSound()));
				}
				break;
			} // edit text and sound
			case 1: setSubGui(new SubGuiQuestSelection(job.quest)); break;
			case 2: {
				job.quest = -1;
				initGui();
				break;
			} // clear quest
			case 3: setSubGui(new SubGuiNpcAvailability(job.availability, this)); break;
			case 4: job.mode = button.getValue() == 0; break;
		}
	}

	@Override
	public void subGuiClosed(GuiScreen gui) {
		if (gui instanceof SubGuiNpcConversationLine && scroll.hasSelected() && data.containsKey(scroll.getNormalSelected())) {
			SubGuiNpcConversationLine sub = (SubGuiNpcConversationLine) gui;
			JobConversation.ConversationLine line = job.getLine(data.get(scroll.getNormalSelected()));
			line.setText(sub.line);
			line.setSound(sub.sound == null ? "" : sub.sound.toString());
			initGui();
		}
	}

	@Override
	public void save() { Packets.sendServer(new SPacketNpcJobSave(job.save(new NBTTagCompound()))); }

	@Override
	public void selected(int id, String name) {
		job.quest = id;
		initGui();
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (scroll.hasSelected() && data.containsKey(scroll.getNormalSelected())) {
			JobConversation.ConversationLine line = scroll.hasSelected() && data.containsKey(scroll.getNormalSelected()) ?
					job.getLine(data.get(scroll.getNormalSelected())) : null;
			switch (textField.id) {
				case 0: if (line != null) { line.npc = textField.getValue(); initGui(); } break;
				case 1: if (line != null) { line.delay = textField.getInteger(); } break;
				case 2: job.generalDelay = textField.getInteger(); break;
				case 3: job.range = textField.getInteger(); break;
			}
		}
	}

}
