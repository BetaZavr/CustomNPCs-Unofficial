package noppes.npcs.client.gui.global;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketContainerOpen;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import java.util.*;

// Changed by Unofficial (BetaZavr)
public class SubGuiNpcQuestReward extends GuiBasic implements ITextfieldListener, ICustomScrollListener {

	protected final Map<Component, DropSet> data = new HashMap<>();
	protected final Quest quest;
	protected GuiCustomScrollNop scroll;

	public SubGuiNpcQuestReward(Quest questIn) {
		super();
		setBackground("largebg.png");
		imageWidth = 192;
		imageHeight = 231;
		title = Component.translatable("questlog.reward");

		quest = questIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		int w = 120;
		int x0 = guiLeft + 4;
		int x1 = x0 + w + 2;
		int y = guiTop + 16;
		int h = 14;
		// items
		if (scroll == null) { scroll = addScroll(0).setSize(184, 118); }
		int i = 0;
		LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
		List<ItemStack> stacks = new ArrayList<>();
		data.clear();
		for (DropSet ds : quest.rewardItems.values()) {
			data.put(ds.getKey(), ds);
			hts.put(i++, ds.getHover(true));
			stacks.add(ds.item);
		}
		add(scroll.setPos(x0, y)
				.setUnsortedList(new ArrayList<>(data.keySet()))
				.setHoverTexts(hts)
				.setStacks(stacks));
		// exit
		addButton(66, x0 + imageWidth - 20, y - 12, "X")
				.setSize(12, 12);
		// add/del/edit item
		addButton(1, x0, y += scroll.getHeight() + 1, "gui.add")
				.setSize(60, 14)
				.setHoverTexts("quest.hover.reward.add");
		addButton(2, x0 + 62, y, "gui.remove")
				.setSize(60, 14)
				.setIsEnabled(scroll.hasSelected())
				.setHoverTexts("quest.hover.reward.del");
		addButton(3, x0 + 124, y, "selectServer.edit")
				.setSize(60, 14)
				.setIsEnabled(scroll.hasSelected())
				.setHoverTexts("quest.hover.reward.edit");
		// type
		Component label = Component.translatable("quest.reward.get.item");
		addLabel(0, x1 - 2 - font.getStringWidth(label.getFormattedText()), (y += h + 2) + 2, label);
		addButton(0, x1, y, false, quest.rewardType.ordinal(),
				"drop.type.all", "drop.type.one", "drop.type.random")
				.setSize(62, h)
				.setHoverTexts("quest.hover.edit.reward.type");
		// xp
		label = Component.translatable("quest.exp").append(":");
		addLabel(1, x1 - 2 - font.getStringWidth(label.getFormattedText()), (y += h + 3) + 1, label);
		int max = 99999;
		addTextField(0, x1 + 1, y, 60, h - 2, quest.rewardExp)
				.setMinMaxDefault(0, max, quest.rewardExp)
				.setHoverTexts(Component.translatable("quest.hover.edit.reward.xp", ((char) 167) + "6" + max));
		// money
		label = Component.translatable("gui.money").append(":");
		addLabel(2, x1 - 2 - font.getStringWidth(label.getFormattedText()), (y += h + 2) + 1, label);
		max = 99999999;
		addTextField(1, x1 + 1, y, 60, h - 2, quest.rewardMoney)
				.setMinMaxDefault(0, max, quest.rewardMoney)
				.setHoverTexts(Component.translatable("quest.hover.edit.reward.money", ((char) 167) + "6" + max));
		// donat
		label = Component.translatable("gui.donat").append(":");
		addLabel(3, x1 - 2 - font.getStringWidth(label.getFormattedText()), (y += h + 2) + 1, label);
		addTextField(2, x1 + 1, y, 60, h - 2, quest.rewardDonat)
				.setMinMaxDefault(0, max, quest.rewardDonat)
				.setHoverTexts(Component.translatable("quest.hover.edit.reward.donat", ((char) 167) + "6" + max));
		// show reward text
		addCheckBox(4, x0, y + h + 1, "gui.enabled", "gui.disabled", quest.showRewardText)
				.setSize(97, 12)
				.setHoverTexts("quest.hover.edit.reward.show");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: quest.setRewardType(button.getValue()); break;
			case 1: {
				openItemEdit(-1);
				break;
			} // add
			case 2: {
				if (scroll.hasSelected() && data.containsKey(scroll.getNormalSelected())) {
					quest.removeDrop(data.get(scroll.getNormalSelected()));
					initGui();
				}
				break;
			} // del
			case 3: {
				if (scroll.hasSelected() && data.containsKey(scroll.getNormalSelected())) { openItemEdit(scroll.getSelectedIndex()); }
				break;
			} // edit
			case 4: quest.showRewardText = ((GuiCheckBoxNop) button).selected(); break;
			case 66: onClose(); break;
		}
	}

	@Override
	public void onClose() {
		super.onClose();
		NoppesUtil.openGUI(player, GuiNpcManageQuest.Instance);
	}

	@Override
	public void unFocused(GuiTextFieldNop textfield) {
		switch (textfield.id) {
			case 0: quest.rewardExp = textfield.getInteger(); break;
			case 1: quest.rewardMoney = textfield.getInteger(); break;
			case 2: quest.rewardDonat = textfield.getInteger(); break;
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) { initGui(); }

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (data.get(scroll.getNormalSelected()) != null) { openItemEdit(scroll.getSelectedIndex()); }
	}

	private void openItemEdit(int pos) {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("InventoryType", 2);
		compound.setInteger("QuestID", quest.getId());
		compound.setInteger("DropSet", pos);
		Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
	}

}
