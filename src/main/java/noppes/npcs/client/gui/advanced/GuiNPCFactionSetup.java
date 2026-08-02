package noppes.npcs.client.gui.advanced;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.client.gui.SubGuiNpcFactionOptions;
import noppes.npcs.client.gui.SubGuiNpcFactionSelect;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketFactionsGet;
import noppes.npcs.packets.server.SPacketMenuSave;
import noppes.npcs.packets.server.SPacketNpcFactionSet;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IScrollData;

public class GuiNPCFactionSetup extends GuiNPCInterface2
		implements IScrollData, ICustomScrollListener {

	protected final Map<Component, Integer> data = new HashMap<>();
	protected GuiCustomScrollNop scrollFactions;

	public GuiNPCFactionSetup(EntityNPCInterface npc) {
		super(npc);
		backGui = EnumGuiType.MainMenuAdvanced;
	}

	@Override
	public void initGui() {
		super.initGui();
		int lId = 0;
		int x0 = guiLeft + 5;
		int x1 = x0 + 210;
		int x2 = x1 - 62;
		int w = x2 - x0 - 2;
		int y = guiTop + 26;
		// attack hostile
		addLabel(lId++, x0, y + 5, "faction.attackHostile")
				.setSize(w, 10);
		addYesNo(0, x2, y, npc.advanced.attackOtherFactions)
				.setSize(60, 20);
		// defend
		addLabel(lId++, x0, (y += 23) + 5, "faction.defend")
				.setSize(w, 10);
		addYesNo(1, x2, y, npc.advanced.defendFaction)
				.setSize(60, 20);
		if (npc.advanced.defendFaction) {
			addCheckBox(5, guiLeft + 4, y += 23, "faction.through.walls", null, npc.advanced.throughWalls)
					.setSize(180, 20)
					.setHoverTexts("faction.hover.through.walls");
		}
		// friends
		addLabel(lId++, x0, (y += 23) + 5, "faction.friends");
		addButton(2, x2, y, "selectServer.edit")
				.setSize(60, 20)
				.setHoverTexts(Component.translatable("faction.hover.addfrends")
						.append(Component.translatable("faction.hover.info.add")));
		// hostiles
		addLabel(lId++, x0, (y += 23) + 5, "faction.hostiles");
		addButton(3, x2, y,"selectServer.edit")
				.setSize(60, 20)
				.setHoverTexts(Component.translatable("faction.hover.addhostiles")
						.append(Component.translatable("faction.hover.info.add")));
		// on death
		addLabel(lId, x0, (y += 23) + 5, "faction.ondeath")
				.setSize(w, 10);
		addButton(4, x2, y, "faction.points")
				.setSize(60, 20)
				.setHoverTexts("faction.hover.replace");
		if (scrollFactions == null) { scrollFactions = addScroll(0).setSize(200, 208); }
		add(scrollFactions.setPos(x1, guiTop + 4));
		Packets.sendServer(new SPacketFactionsGet());
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: npc.advanced.attackOtherFactions = button.getValue() == 1; break;
			case 1: npc.advanced.defendFaction = button.getValue() == 1; initGui(); break;
			case 2: {
				HashMap<Component, Integer> corData = new HashMap<>();
				for (Component name : data.keySet()) {
					int id = data.get(name);
					if (npc.faction.id == id || npc.faction.attackFactions.contains(id)
							|| npc.faction.frendFactions.contains(id)
							|| npc.advanced.attackFactions.contains(id)) {
						continue;
					}
					corData.put(name, id);
				}
				setSubGui(new SubGuiNpcFactionSelect(0, "faction.friends", npc.advanced.friendFactions, corData));
				break;
			} // friends
			case 3: {
				HashMap<Component, Integer> corData = new HashMap<>();
				for (Component name : data.keySet()) {
					int id = data.get(name);
					if (npc.faction.id == id || npc.faction.attackFactions.contains(id)
							|| npc.faction.frendFactions.contains(id)
							|| npc.advanced.friendFactions.contains(id)) {
						continue;
					}
					corData.put(name, id);
				}
				setSubGui(new SubGuiNpcFactionSelect(1, "faction.hostiles", npc.advanced.attackFactions, corData));
				break;
			} // hostiles
			case 4: setSubGui(new SubGuiNpcFactionOptions(npc.advanced.factions)); break;
			case 5: npc.advanced.throughWalls = ((GuiCheckBoxNop) button).selected(); break;
		}
	}

	@Override
	public void setData(Vector<String> dataList, Map<String, Integer> dataMap) {
		int selectedId = npc.getFaction().id;
		data.clear();
		Component select = null;
		List<Faction> factions = new ArrayList<>(FactionController.instance.factions.values());
		factions.sort(Comparator.comparingInt(f -> f.id));
		for (Faction f : factions) {
			Component key = Component.empty()
					.append(Component.literal("ID:" + f.id + " ").withStyle(TextFormatting.GRAY))
					.append(Component.translatable(f.name).withStyle(TextFormatting.RESET).withColor(f.color));
			data.put(key, f.id);
			if (f.id == selectedId) { select = key; }
		}
		scrollFactions.setUnsortedList(new ArrayList<>(data.keySet()));
		if (select != null) { scrollFactions.setSelected(select); }
	}

	@Override
	public void setSelected(String selected) { scrollFactions.setSelected(selected); }

	@Override
	public void scrollClicked(GuiCustomScrollNop guiCustomScroll) {
		if (guiCustomScroll.id == 0) {
			Integer factionId = data.get(scrollFactions.getNormalSelected());
			if (factionId != null) {
				npc.setFaction(factionId);
				Packets.sendServer(new SPacketNpcFactionSet(factionId));
			}
		}
	}

	@Override
	public void save() { Packets.sendServer(new SPacketMenuSave(EnumMenuType.ADVANCED, npc.advanced.save(new NBTTagCompound()))); }

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

	// New from Unofficial (BetaZavr)
	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiNpcFactionSelect) {
			SubGuiNpcFactionSelect gui = (SubGuiNpcFactionSelect) subgui;
			if (gui.id == 0) {
				npc.advanced.friendFactions.clear();
				for (int id : gui.selectFactions) {
					npc.advanced.attackFactions.remove(id);
					npc.advanced.friendFactions.add(id);
				}
			} else if (gui.id == 1) {
				npc.advanced.attackFactions.clear();
				for (int id : gui.selectFactions) {
					npc.advanced.friendFactions.remove(id);
					npc.advanced.attackFactions.add(id);
				}
			}
			save();
		}
	}

}
