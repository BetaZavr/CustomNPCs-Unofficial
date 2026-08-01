package noppes.npcs.client.gui.global;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.SubGuiMailmanSendSetup;
import noppes.npcs.client.gui.availability.SubGuiNpcAvailability;
import noppes.npcs.client.gui.SubGuiNpcCommand;
import noppes.npcs.client.gui.SubGuiNpcFactionOptions;
import noppes.npcs.client.gui.select.SubGuiQuestSelection;
import noppes.npcs.client.gui.select.SubGuiSoundSelection;
import noppes.npcs.client.gui.select.SubGuiTextureSelection;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketDialogMinID;
import noppes.npcs.packets.server.SPacketDialogRemove;
import noppes.npcs.packets.server.SPacketDialogSave;
import noppes.npcs.shared.client.gui.GuiTextAreaScreen;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class SubGuiDialogEdit
		extends GuiNPCInterface
		implements ITextfieldListener, IGuiData {

	protected Dialog dialog;

	// New from Unofficial (BetaZavr)
	public final GuiScreen parent;

	public SubGuiDialogEdit(EntityNPCInterface npcIn, Dialog dialogIn, GuiScreen gui) {
		super(npcIn);
		setBackground("menubg.png");
		imageWidth = 386;
		imageHeight = 226;

		dialog = dialogIn;
		parent = gui;
	}

	@Override
	public void initGui() {
		super.initGui();
		if (dialog == null) { onClose(); return; }
		int lID = 0;
		int y = guiTop + 4;
		int x = guiLeft + 120;
		int xl = guiLeft + 4;
		// name
		addLabel(lID++, xl, y + 5, "gui.title")
				.setSize(40, 12);
		addTextField(1, x - 74, y + 1, 220, 18, dialog.title)
				.setHoverTexts("dialog.hover.name");
		// reset id
		addLabel(lID++, x + 150, y + 5, "ID: " + dialog.id)
				.setSize(36, 12);
		addButton(24, x + 188, y, "gui.reset")
				.setSize(50, 20)
				.setHoverTexts("hover.reset.id");
		// exit
		addButton(66, x + 240, y, "X")
				.setSize(20, 20)
				.setHoverTexts("hover.back");
		// text
		addLabel(lID++, xl, (y += 22) + 5, "dialog.dialogtext")
				.setSize(114, 12);
		addButton(3, x, y, "selectServer.edit")
				.setSize(50, 20)
				.setHoverTexts("dialog.hover.text");
		// availability
		addLabel(lID++, xl, (y += 22) + 5, "availability.options")
				.setSize(114, 12);
		addButton(4, x, y, "selectServer.edit")
				.setSize(50, 20)
				.setHoverTexts("availability.hover");
		// faction
		addLabel(lID++, xl, (y += 22) + 5, "faction.options")
				.setSize(114, 12);
		addButton(5, x, y, "selectServer.edit")
				.setSize(50, 20)
				.setHoverTexts("dialog.hover.faction");
		// options
		addLabel(lID++, xl, (y += 22) + 5, "dialog.options")
				.setSize(114, 12);
		addButton(6, x, y, "selectServer.edit")
				.setSize(50, 20)
				.setHoverTexts("dialog.hover.options");
		// quest
		GuiButtonNop button = addButton(7, xl, y += 22, "availability.selectquest")
				.setSize(166, 20)
				.setHoverTexts("dialog.hover.quests");
		if (dialog.hasQuest()) { button.setDisplayText(dialog.getQuest().getTitle()); }
		addButton(8, xl + 168, y, "X")
				.setSize(20, 20)
				.setHoverTexts("dialog.hover.quests.del");
		// mail
		button = addButton(13, xl, y += 22, "mailbox.setup")
				.setSize(166, 20)
				.setHoverTexts("dialog.hover.mail");
		if (!dialog.mail.title.isEmpty()) { button.setDisplayText(dialog.mail.title); }
		addButton(14, xl + 168, y, "X")
				.setSize(20, 20)
				.setHoverTexts("dialog.hover.mail.del");
		// sound
		addLabel(lID++, xl, (y += 28) + 5,  "gui.selectSound")
				.setSize(68, 12);
		addTextField(2, xl + 70, y, 252, 18, dialog.sound)
				.setHoverTexts("dialog.hover.sound");
		// sound select
		addButton(9, xl + 326, y - 1, "mco.template.button.select")
				.setSize(50, 20)
				.setHoverTexts("dialog.hover.sound.del");
		// texture
		addLabel(lID++, xl, (y += 22) + 5, "gui.texture")
				.setSize(68, 12);
		addTextField(4, xl + 70, y, 252, 18, dialog.texture)
				.setHoverTexts("dialog.hover.texture");
		addButton(16, xl + 326, y - 1, "mco.template.button.select")
				.setSize(50, 20)
				.setHoverTexts("dialog.hover.texture.del");
		y = guiTop + 26;
		xl = guiLeft + 200;
		x = guiLeft + 330;
		addCheckBox(11, xl, y, "dialog.hideNPC", null, dialog.hideNPC)
				.setSize(180, 14)
				.setHoverTexts("dialog.hover.hidenpc");
		addCheckBox(12, xl, y += 16, "dialog.showWheel", null, dialog.showWheel)
				.setSize(180, 14)
				.setHoverTexts("dialog.hover.wheel");
		addCheckBox(15, xl, y += 16, "dialog.disableEsc", null, dialog.disableEsc)
				.setSize(180, 14)
				.setHoverTexts("dialog.hover.esc");
		addCheckBox(17, xl, y += 16, "dialog.sound.stop", null, dialog.stopSound)
				.setSize(180, 14)
				.setHoverTexts("dialog.hover.sound.stop");
		addCheckBox(18, xl, y + 16, "dialog.showFits", null, dialog.showFits)
				.setSize(180, 14)
				.setHoverTexts("dialog.hover.show.fits");
		// delay
		y = guiTop + 137;
		addLabel(lID++, xl, y + 4, "dialog.cooldown.time")
				.setSize(128, 12);
		addTextField(3, x + 1, y, 48, 18, "" + dialog.delay)
				.setMinMaxDefault(0, 1200, dialog.delay)
				.setHoverTexts("dialog.hover.delay");
		// command
		addLabel(lID, xl, (y -= 22) + 5, "advMode.command")
				.setSize(128, 12);
		addButton(10, x, y, "selectServer.edit")
				.setSize(50, 20)
				.setHoverTexts("dialog.hover.command");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 3: setSubGui(new GuiTextAreaScreen(0, dialog.text)); break;
			case 4: setSubGui(new SubGuiNpcAvailability(dialog.availability, parent)); break;
			case 5: setSubGui(new SubGuiNpcFactionOptions(dialog.factionOptions)); break;
			case 6: setSubGui(new SubGuiNpcDialogOptions(npc, dialog, this)); break;
			case 7: setSubGui(new SubGuiQuestSelection(dialog.quest)); break;
			case 8: dialog.quest = -1; initGui(); break;
			case 9: setSubGui(new SubGuiSoundSelection(this, 0, npc, getTextField(2).getValue())); break;
			case 10: setSubGui(new SubGuiNpcCommand(dialog.command)); break;
			case 11: dialog.hideNPC = ((GuiCheckBoxNop) button).selected(); break;
			case 12: dialog.showWheel = ((GuiCheckBoxNop) button).selected(); break;
			case 13: setSubGui(new SubGuiMailmanSendSetup(dialog.mail)); break;
			case 14: dialog.mail = new PlayerMail(); initGui(); break;
			case 15: dialog.disableEsc = ((GuiCheckBoxNop) button).selected(); break;
			case 16: setSubGui(new SubGuiTextureSelection(this, 0, null, dialog.texture, "png", 3)); break;
			case 17: dialog.stopSound = ((GuiCheckBoxNop) button).selected(); break;
			case 18: dialog.showFits = ((GuiCheckBoxNop) button).selected(); break;
			case 24: {
				ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
					if (bo) { Packets.sendServer(new SPacketDialogMinID(dialog.id)); }
					NoppesUtil.openGUI(player, this);
				},
						Component.translatable("message.change.id", "" + dialog.id).getParent(),
						Component.translatable("message.change").getParent());
				setScreen(guiYesNo);
				break;
			} // reset ID
			case 66: onClose(); break;
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 1: {
				StringBuilder t = new StringBuilder(textField.getValue());
				boolean has = true;
				while (has) {
					has = false;
					for (Dialog dia : dialog.category.dialogs.values()) {
						if (dia.id != dialog.id && dia.title.equalsIgnoreCase(t.toString())) {
							has = true;
							break;
						}
					}
					if (has) { t.append("_"); }
				}
				dialog.title = t.toString();
				break;
			}
			case 2: dialog.sound = textField.getResourceLocation(); break;
			case 3: dialog.delay = textField.getInteger(); break;
			case 4: dialog.texture = textField.getValue(); break;
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof GuiTextAreaScreen) { dialog.text = ((GuiTextAreaScreen) subgui).text; }
		else if (subgui instanceof SubGuiNpcDialogOption) { setSubGui(new SubGuiNpcDialogOptions(npc, dialog, this)); }
		else if (subgui instanceof SubGuiNpcCommand) { dialog.command = ((SubGuiNpcCommand) subgui).command; }
		else if (subgui instanceof SubGuiQuestSelection) {
			if (((SubGuiQuestSelection) subgui).selectedQuest != null) {
				dialog.quest = ((SubGuiQuestSelection) subgui).selectedQuest.id;
				initGui();
			}
		}
		else if (subgui instanceof SubGuiSoundSelection) {
			if (((SubGuiSoundSelection) subgui).resource != null) {
				getTextField(2).setValue(((SubGuiSoundSelection) subgui).resource.toString());
				unFocused(getTextField(2));
			}
		}
		else if (subgui instanceof SubGuiTextureSelection) {
			if (((SubGuiTextureSelection) subgui).resource == null) { return; }
			dialog.texture = ((SubGuiTextureSelection) subgui).resource.toString();
			initGui();
		}
	}

	@Override
	public void save() {
		GuiTextFieldNop.unfocus();
		Packets.sendServer(new SPacketDialogSave(dialog.category.id, dialog.save(new NBTTagCompound())));
	}

	// New from Unofficial (BetaZavr)
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (getButton(17) != null) {
			getButton(17).setIsEnabled(getTextField(2) != null && !getTextField(2).getValue().isEmpty());
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (!hasSubGui()) {
			drawHorizontalLine(guiLeft + 196, guiTop + 24, guiTop + 159, 0xFF808080);
			drawVerticalLine(guiLeft + 4, guiLeft + imageWidth - 5, guiTop + 159, 0xFF808080);
		}
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound != null && compound.hasKey("MinimumID", 3) && dialog.id != compound.getInteger("MinimumID")) {
			Packets.sendServer(new SPacketDialogRemove(dialog.id));
			dialog.id = compound.getInteger("MinimumID");
			Packets.sendServer(new SPacketDialogSave(dialog.category.id, dialog.save(new NBTTagCompound())));
			initGui();
		}
	}

}
