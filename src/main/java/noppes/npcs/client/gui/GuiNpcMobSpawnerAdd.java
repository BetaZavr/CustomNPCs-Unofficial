package noppes.npcs.client.gui;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.controllers.ClientCloneController;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCloneNameCheck;
import noppes.npcs.packets.server.SPacketCloneSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;

public class GuiNpcMobSpawnerAdd extends GuiNPCInterface implements IGuiData, ITextfieldListener {

	protected static boolean serverSide = true;
	protected static int tab = 1;
	protected final NBTTagCompound compound;
	protected Entity toClone;

	public GuiNpcMobSpawnerAdd(NBTTagCompound nbt) {
		super();
		setBackground("menubg.png");
		imageWidth = 256;
		imageHeight = 216;

		toClone = EntityList.createEntityFromNBT(nbt, minecraft.world);
		compound = nbt;
	}

	@Override
	public void initGui() {
		super.initGui();
		String name = Util.instance.sanitizeFilename(toClone.getName());
		addLabel(0, guiLeft + 4, guiTop + 6, "Save as");
		addTextField(0, guiLeft + 4, guiTop + 18, 200, 20, name);
		addLabel(1, guiLeft + 10, guiTop + 50, "gui.tab");
		Object[] ons = new Object[9];
		for (int i = 1; i < 10; i++) { ons[i - 1] = i; }
		addButton(2, guiLeft + 40, guiTop + 45, false, tab - 1, ons)
				.setSize(20, 20);
		addButton(3, guiLeft + 4, guiTop + 95, false, serverSide ? 1 : 0, "clone.client", "clone.server");
		addButton(0, guiLeft + 4, guiTop + 70, "gui.save")
				.setSize(80, 20);
		addButton(66, guiLeft + 86, guiTop + 70, "gui.cancel")
				.setSize(80, 20)
				.setHoverTexts("hover.back");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				String name = getTextField(0).getValue();
				if (name.isEmpty()) { return; }
				if (!serverSide) {
					if (ClientCloneController.Instance.getCloneData(null, name, tab) != null) {
						setScreen(new ConfirmScreen(this::accept, Component.empty().getParent(),
								Component.translatable("clone.overwrite").getParent()));
					}
					else { accept(true); }
				}
				else { Packets.sendServer(new SPacketCloneNameCheck(name, tab)); }
				break;
			}
			case 2: tab = button.getValue() + 1; break;
			case 3: serverSide = button.getValue() == 1; break;
			case 66: onClose(); break;
		}
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound.hasKey("NameExists")) {
			if (compound.getBoolean("NameExists")) {
				setScreen(new ConfirmScreen(this::accept, Component.empty().getParent(),
						Component.translatable("clone.overwrite").getParent()));
			}
			else { accept(true); }
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		String name = Util.instance.sanitizeFilename(textField.getValue());
		if (!textField.getValue().equals(name)) { textField.setValue(name); }
	}

	public void accept(boolean confirm) {
		if (confirm) {
			String name = getTextField(0).getValue();
			if (!serverSide) { ClientCloneController.Instance.addClone(compound, name, tab); }
			else { Packets.sendServer(new SPacketCloneSave(name, tab)); }
			GuiNpcMobSpawner.showingClones = serverSide ? 2 : 0;
			GuiNpcMobSpawner.activeTab = tab;
			onClose();
		}
		else { setScreen(this); }
	}

}
