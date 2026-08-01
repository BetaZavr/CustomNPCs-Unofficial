package noppes.npcs.client.gui.advanced;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.controllers.data.DataTransform;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuGet;
import noppes.npcs.packets.server.SPacketMenuSave;
import noppes.npcs.packets.server.SPacketNpcTransform;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.listeners.IGuiData;

public class GuiNPCNightSetup extends GuiNPCInterface2 implements IGuiData {

	protected final DataTransform data;

	public GuiNPCNightSetup(EntityNPCInterface npc) {
		super(npc);
		data = npc.transform;
		backGui = EnumGuiType.MainMenuAdvanced;

		Packets.sendServer(new SPacketMenuGet(EnumMenuType.TRANSFORM));
	}

	@Override
	public void initGui() {
		super.initGui();
		int w = 80;
		int x0 = guiLeft + 5;
		int x1 = x0 + w + 2;
		int x2 = x0 + 170;
		int x3 = x2 + w + 2;
		int y = guiTop + 20;
		addLabel(0, x0, y + 5, "menu.display")
				.setSize(w, 10);
		addYesNo(0, x1, y, data.hasDisplay)
				.setHoverTexts(Component.translatable("transform.hover.tab",
						Component.translatable("menu.display").getFormattedText()));
		addLabel(10, x2, y + 5, "advanced.editingmode")
				.setSize(w, 10);
		addYesNo(10, x3, y, data.editingModus)
				.setHoverTexts("transform.hover.edit");
		addLabel(1, x0, (y += 22) + 5, "menu.stats")
				.setSize(w, 10);
		addYesNo(1, x1, y, data.hasStats)
				.setHoverTexts(Component.translatable("transform.hover.tab",
						Component.translatable("menu.stats").getFormattedText()));
		if (data.editingModus) {
			addButton(11, x2, y, "advanced.loadday")
					.setHoverTexts(Component.translatable("transform.hover.loadday")
							.append(Component.translatable("transform.hover.state")))
					.setSize(120, 20);
		}
		addLabel(2, x0, (y += 22) + 5, "menu.ai")
				.setSize(w, 10);
		addYesNo(2, x1, y, data.hasAi)
				.setHoverTexts(Component.translatable("transform.hover.tab",
						Component.translatable("menu.ai").getFormattedText()));
		if (data.editingModus) {
			addButton(12, x2, y, "advanced.loadnight")
					.setHoverTexts(Component.translatable("transform.hover.loadnight")
							.append(Component.translatable("transform.hover.state")))
					.setSize(120, 20);
		}
		addLabel(3, x0, (y += 22) + 5, "menu.inventory")
				.setSize(w, 10);
		addYesNo(3, x1, y, data.hasInv)
				.setHoverTexts(Component.translatable("transform.hover.tab",
						Component.translatable("menu.inventory").getFormattedText()));

		// New from Unofficial (BetaZavr)
		addLabel(7, x0, (y += 22) + 5, "movement.animation")
				.setSize(w, 10);
		addYesNo(7, x1, y, data.hasAnimations)
				.setHoverTexts("transform.hover.animation");

		addLabel(4, x0, (y += 22) + 5, "menu.advanced")
				.setSize(w, 10);
		addYesNo(4, x1, y, data.hasAdvanced)
				.setHoverTexts(Component.translatable("transform.hover.tab",
						Component.translatable("menu.advanced").getFormattedText()));
		addLabel(5, x0, (y += 22) + 5, "role.name")
				.setSize(w, 10);
		addYesNo(5, x1, y, data.hasRole)
				.setHoverTexts(Component.translatable("transform.hover.role",
						Component.translatable("menu.advanced").getFormattedText()));
		addLabel(6, x0, (y += 22) + 5, "job.name")
				.setSize(w, 10);
		addYesNo(6, x1, y, data.hasJob)
				.setHoverTexts(Component.translatable("transform.hover.job",
						Component.translatable("menu.advanced").getFormattedText()));
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: data.hasDisplay = ((GuiButtonYesNo) button).getBoolean(); break;
			case 1: data.hasStats = ((GuiButtonYesNo) button).getBoolean(); break;
			case 2: data.hasAi = ((GuiButtonYesNo) button).getBoolean(); break;
			case 3: data.hasInv = ((GuiButtonYesNo) button).getBoolean(); break;
			case 4: data.hasAdvanced = ((GuiButtonYesNo) button).getBoolean(); break;
			case 5: data.hasRole = ((GuiButtonYesNo) button).getBoolean(); break;
			case 6: data.hasJob = ((GuiButtonYesNo) button).getBoolean(); break;
			case 7: data.hasAnimations = ((GuiButtonYesNo) button).getBoolean(); break;
			case 10: data.editingModus = ((GuiButtonYesNo) button).getBoolean(); save(); initGui(); break;
			case 11: Packets.sendServer(new SPacketNpcTransform(false)); break;
			case 12: Packets.sendServer(new SPacketNpcTransform(true)); break;
		}
	}

	@Override
	public void save() { Packets.sendServer(new SPacketMenuSave(EnumMenuType.TRANSFORM, data.saveOptions(new NBTTagCompound()))); }

	@Override
	public void setGuiData(NBTTagCompound compound) {
		data.loadOptions(compound);
		initGui();
	}

}
