package noppes.npcs.client.gui.roles;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcRoleSave;
import noppes.npcs.shared.client.gui.GuiTextAreaScreen;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleDialog;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class GuiRoleDialog extends GuiNPCInterface2 implements ITextfieldListener {

	protected final RoleDialog role;
	protected int slot = 0;

	public GuiRoleDialog(EntityNPCInterface npc) {
		super(npc);

		backGui = EnumGuiType.MainMenuAdvanced;
		role = (RoleDialog)npc.role;
	}

	@Override
	public void initGui() {
		super.initGui();
		int lId = 0;
		int x0 = guiLeft + 5;
		int x1 = x0 + 12;
		int x2 = x1 + 283;
		int y = guiTop + 5;
		addLabel(lId++, x0, y + 5, Component.translatable("dialog.starttext").append(":"))
				.setSize(120, 10);
		addButton(0, x0 + 122, y,"selectServer.edit")
				.setSize(60, 20)
				.setHoverTexts("role.dialog.hover.text");
		addLabel(lId++, x2, y + 5, "_[?]_")
				.setSize(120, 10)
				.setHoverTexts("role.dialog.hover.info");
		addLabel(lId++, x0, y += 22, Component.translatable("dialog.options").append(":"))
				.setSize(280, 10);
		y += 11;
		for(int i = 1; i <= 6; ++i) {
			addLabel(lId++, x0, y + 4, i + ":")
					.setSize(10, 10);
			addTextField(i, x1, y + 1, 280, 18, role.options.getOrDefault(i, ""))
					.setHoverTexts(Component.translatable("role.dialog.hover.option", "" + i));
			addButton(i, x2, y, "selectServer.edit")
					.setSize(60, 20)
					.setHoverTexts("role.dialog.hover.option.text");
			y += 22;
		}
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (button.id <= 6) {
			save();
			slot = button.id;
			String text = role.dialog;
			if (slot >= 1) { text = role.optionsTexts.get(slot); }
			setSubGui(new GuiTextAreaScreen(0, text != null ? text : ""));
		}
	}

	@Override
	public void save() { Packets.sendServer(new SPacketNpcRoleSave(role.save(new NBTTagCompound()))); }

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof GuiTextAreaScreen) {
			GuiTextAreaScreen text = (GuiTextAreaScreen) subgui;
			if (slot == 0) { role.dialog = text.text; }
			else if (text.text.isEmpty()) { role.optionsTexts.remove(slot); }
			else { role.optionsTexts.put(slot, text.text); }
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) { role.options.put(textField.id, textField.getValue()); }

}
