package noppes.npcs.client.gui.player;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketQuestCompletionCheck;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

import javax.annotation.Nonnull;
import java.util.Map;

public class GuiNpcQuestChooseReward extends GuiNPCInterface {

	protected static final ResourceLocation SLOT = new ResourceLocation(CustomNpcs.MODID, "textures/gui/itemsetup.png");
	protected final Map<Integer, ItemStack> rewardItems;
	protected final Quest quest;

	public GuiNpcQuestChooseReward(@Nonnull Quest questIn, @Nonnull Map<Integer, ItemStack> rewardItemsIn) {
		super();
		setBackground("smallbg.png");
		int slots = rewardItemsIn.size() % 9;
		imageWidth = slots < 6 ? 98 : slots * 18 + 8;
		imageHeight = 38 + (int) (Math.ceil(rewardItemsIn.size() / 9.0f) * 18.0f);

		quest = questIn;
		rewardItems = rewardItemsIn;
	}

	@Override
	public void initGui() {
		super.initGui();
		addLabel(0, guiLeft + 4, guiTop + 4,"quest.choose.reward")
				.setSize(imageWidth - 8, 10);
		addButton(0, guiLeft + imageWidth - 94, guiTop + imageHeight - 20, "quest.no.thanks")
				.setSize(90, 16);
		int x = guiLeft + (imageWidth - (rewardItems.size() % 9) * 18) / 2;
		int y = guiTop + 16;
		for (Map.Entry<Integer, ItemStack> entry : rewardItems.entrySet()) {
			addButton(entry.getKey() + 1, x + (entry.getKey() % 9) * 18,
					y + ((int) Math.floor(entry.getKey() / 9.0f) * 18),
					"")
					.setSize(18, 18)
					.setTexture(SLOT)
					.setUV(7, 112, 18, 18)
					.setStacks(entry.getValue())
					.setCurrentStackPos(0)
					.setHoverTexts(entry.getValue().getTooltip(player,
							mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL))
					.isSimple = true;
		}
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		Packets.sendServer(new SPacketQuestCompletionCheck(quest.id, button.id == 0 ?ItemStack.EMPTY : button.renderStack));
		onClose();
	}

}
