package noppes.npcs.client.gui.player;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketQuestCompletionCheck;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

import java.util.Map;

public class GuiNpcQuestChooseReward extends GuiNPCInterface {

	protected final Map<Integer, ItemStack> rewardItems;
	protected final int questId;

	public GuiNpcQuestChooseReward(Quest quest, Map<Integer, ItemStack> rewardItemsIn) {
		super();
		title = Component.translatable("gui.quest", ": ").append(Component.translatable(quest.title));
		setBackground("smallbg.png");
		imageWidth = 176;
		imageHeight = 42 + (int) (Math.floor(rewardItemsIn.size() / 9.0f) * 18.0f);

		questId = quest.id;
		rewardItems = rewardItemsIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		int x = guiLeft + 4;
		int y = guiTop + 16;
		addButton(0, guiLeft + imageWidth - 114, guiTop + imageHeight - 24, "quest.no.thanks")
				.setSize(110, 20);
		for (Map.Entry<Integer, ItemStack> entry : rewardItems.entrySet()) {
			addButton(entry.getKey() + 1, x + (entry.getKey() % 9) * 18, y + (int) Math.floor((entry.getKey() / 9.0f) * 18), "quest.no.thanks")
					.setSize(18, 18)
					.setTexture(RESOURCE_SLOT)
					.setUV(220, 0, 36, 36)
					.setStacks(entry.getValue())
					.setCurrentStackPos(0)
					.setHoverTexts(entry.getValue().getTooltip(player,
							mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL));
		}
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		ItemStack stack = ItemStack.EMPTY;
		if (button.id == 0) { onClose(); }
		else { stack = button.renderStack; }
		Packets.sendServer(new SPacketQuestCompletionCheck(questId, stack));
	}

}
