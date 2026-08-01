package noppes.npcs.client.gui.roles;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.gui.select.SubGuiSoundSelection;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcJobSave;
import noppes.npcs.roles.JobBard;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class GuiNpcBard extends GuiNPCInterface2 implements ITextfieldListener {

	protected final JobBard job;

	public GuiNpcBard(EntityNPCInterface npc) {
		super(npc);

		backGui = EnumGuiType.MainMenuAdvanced;
		job = (JobBard) npc.job;
	}

	@Override
	public void initGui() {
		super.initGui();
		int x0 = guiLeft + 31;
		int x1 = x0 + 122;
		int x2 = x1 + 122;
		int y = guiTop + 20;
		// song
		addTextField(1, x0 + 1, y + 1, 240, 18, job.song == null ? "" : job.song.toString())
				.setHoverTexts("bard.hover.song");
		// select sound
		addButton(0, x2, y, "gui.selectSound")
				.setSize(98, 20)
				.setHoverTexts("bard.hover.select");
		// del sound
		addButton(1, x2 + 100, y, "X")
				.setSize(20, 20)
				.setHoverTexts("bard.hover.del");
		// is streamer
		addButton(2, x0, y += 30, false, job.isStreamer ? 0 : 1, "bard.jukebox", "bard.background")
				.setSize(120, 20)
				.setHoverTexts(Component.translatable("bard.hover.range." + (job.isStreamer ? 0 : 1))
						.append(Component.translatable("bard.hover.range.2")));
		addButton(3, x1, y, false, job.hasOffRange ? 0 : 1, "bard.hasoff", "bard.hason")
				.setSize(120, 20)
				.setHoverTexts("bard.hover.dist." + (job.hasOffRange ? 0 : 1));
		addButton(4, x2, y, false, job.isRange ? 0 : 1, "type.range", "parameter.position")
				.setSize(120, 20)
				.setHoverTexts("bard.hover.type." + job.isRange);
		// on
		addLabel(0, x0, (y += 30) + 6, Component.translatable("bard.ondistance").append(":"))
				.setSize(120, 10);
		GuiTextFieldNop textField;
		for (int i = 0; i < (job.isRange ? 1 : 3); i++) {
			textField = addTextField(2 + i, x1 + i * 44, y, 40, 20, "")
					.setMinMaxDefault(2, 64, 5);
			if (job.isRange && i == 0) {
				textField.setValue(job.range[0] + "");
				textField.setHoverTexts("bard.hover.min");
			}
			else {
				textField.setValue(job.minPos[i] + "");
				textField.setIsEnabled(!job.isRange);
				if (i == 0) {
					textField.setHoverTexts(Component.translatable("bard.hover.min")
							.append("<br>").append(Component.translatable("hover.scale.x")));
				}
				else if (i == 1) { textField.setHoverTexts(Component.translatable("bard.hover.min")
						.append("<br>").append(Component.translatable("hover.scale.y")));
				}
				else { textField.setHoverTexts(Component.translatable("bard.hover.min")
						.append("<br>").append(Component.translatable("hover.scale.z")));
				}
			}
		}
		// off
		if (job.hasOffRange) {
			addLabel(1, x0, (y += 30) + 6, Component.translatable("bard.offdistance").append(":"))
					.setSize(120, 10);
			for (int i = 0; i < (job.isRange ? 1 : 3); i++) {
				textField = addTextField(5 + i, x1 + i * 44, y, 40, 20, "")
						.setMinMaxDefault(2, 256, 64);
				if (job.isRange && i == 0) {
					textField.setHoverTexts(Component.translatable("bard.hover.max"))
							.setValue(job.range[1] + "");
				} else {
					textField.setValue(job.maxPos[i] + "");
					textField.setIsEnabled(!job.isRange);
					if (i == 0) {
						textField.setHoverTexts(Component.translatable("bard.hover.max")
								.append("<br>").append(Component.translatable("hover.scale.x")));
					}
					else if (i == 1) {
						textField.setHoverTexts(Component.translatable("bard.hover.max")
								.append("<br>").append(Component.translatable("hover.scale.y")));
					}
					else {
						textField.setHoverTexts(Component.translatable("bard.hover.max")
								.append("<br>").append(Component.translatable("hover.scale.z")));
					}
				}
			}
		}
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				setSubGui(new SubGuiSoundSelection(this, 0, npc, job.song == null ? "" : job.song.toString()));
				MusicController.Instance.stopSounds();
				break;
			} // select sound
			case 1: {
				job.song = null;
				getTextField(1).setValue("");
				MusicController.Instance.stopSounds();
				initGui();
				break;
			} // clear sound
			case 2: job.isStreamer = button.getValue() == 0; initGui(); break;
			case 3: job.hasOffRange = button.getValue() == 0; initGui(); break;
			case 4: job.isRange = button.getValue() == 0; initGui(); break;
		}
	}

	@Override
	public void save() {
		if (job.range[0] > job.range[1]) { job.range[1] = job.range[0]; }
		MusicController.Instance.stopSounds();
		Packets.sendServer(new SPacketNpcJobSave(job.save(new NBTTagCompound())));
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiSoundSelection && ((SubGuiSoundSelection) subgui).resource != null) {
			job.song = ((SubGuiSoundSelection) subgui).resource;
			initGui();
		}
	}

	// New from Unofficial (BetaZavr)
	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 1: job.song = textField.getResourceLocation(); break;
			case 2: {
				if (job.isRange) { job.range[0] = textField.getInteger(); }
				else { job.minPos[0] = textField.getInteger(); }
				break;
			}
			case 3: job.minPos[1] = textField.getInteger(); break;
			case 4: job.minPos[2] = textField.getInteger(); break;
			case 5: {
				if (job.isRange) { job.range[1] = textField.getInteger(); }
				else { job.maxPos[0] = textField.getInteger(); }
				break;
			}
			case 6: job.maxPos[1] = textField.getInteger(); break;
			case 7: job.maxPos[2] = textField.getInteger(); break;
		}
	}

}
