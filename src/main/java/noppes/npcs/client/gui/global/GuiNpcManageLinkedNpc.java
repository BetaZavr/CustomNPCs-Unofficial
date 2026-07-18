package noppes.npcs.client.gui.global;

import java.util.*;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketLinkedAdd;
import noppes.npcs.packets.server.SPacketLinkedGet;
import noppes.npcs.packets.server.SPacketLinkedRemove;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.IScrollData;
import noppes.npcs.util.Util;

public class GuiNpcManageLinkedNpc
		extends GuiNPCInterface2
		implements IScrollData {

	protected final List<String> data = new ArrayList<>();
	protected GuiCustomScrollNop scroll;

	public GuiNpcManageLinkedNpc(EntityNPCInterface npc) {
		super(npc);

		backGui = EnumGuiType.MainMenuGlobal;
		Packets.sendServer(new SPacketLinkedGet());
	}

	@Override
	public void initGui() {
		super.initGui();
		addButton(1, guiLeft + 358, guiTop + 38, "gui.add")
				.setSize(58, 20);
		addButton(2, guiLeft + 358, guiTop + 61, "gui.remove")
				.setSize(58, 20);
		if (scroll == null) { scroll = addScroll(0).setSize(143, 208); }
		add(scroll.setList(data)
				.setPos(guiLeft + 214, guiTop + 4));
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 1: {
				save();
				setSubGui(new SubGuiEditText(Util.instance.deleteColor(Component.translatable("gui.new").getFormattedText())));
				break;
			}
			case 2: {
				if (scroll.hasSelected()) { Packets.sendServer(new SPacketLinkedRemove(scroll.getSelected())); }
				break;
			}
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (!((SubGuiEditText)subgui).cancelled) { Packets.sendServer(new SPacketLinkedAdd(((SubGuiEditText)subgui).text[0])); }
	}

	@Override
	public void setData(Vector<String> dataList, Map<String, Integer> dataMap) {
		data.clear();
		data.addAll(dataList);
		initGui();
	}

	@Override
	public void setSelected(String selected) { }

	@Override
	public void save() { }

}
