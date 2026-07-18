package noppes.npcs.client.gui.global;

import java.util.*;
import java.util.Map.Entry;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.SubGuiNpcFactionOptions;
import noppes.npcs.client.gui.SubGuiNpcFactionPoints;
import noppes.npcs.client.gui.SubGuiNpcFactionSelect;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiTextAreaScreen;
import noppes.npcs.client.gui.select.SubGuiTextureSelection;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.IScrollData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

public class GuiNpcManageFactions
		extends GuiNPCInterface2
		implements IScrollData, ICustomScrollListener, ITextfieldListener, IGuiData {

	protected final Map<Component, Integer> data = new HashMap<>();
	protected GuiCustomScrollNop scroll;
	protected @Nonnull Faction faction = new Faction();

	// New from Unofficial (BetaZavr)
	public static boolean sortByName = true;

	public GuiNpcManageFactions(EntityNPCInterface npc) {
		super(npc);
		backGui = EnumGuiType.MainMenuGlobal;
		Packets.sendServer(new SPacketFactionsGet());
	}

	@Override
	public void initGui() {
		super.initGui();
		int x = guiLeft + 368;
		int y = guiTop + 8;
		// add
		addButton(0, x, y, "gui.add")
				.setSize(45, 16)
				.setHoverTexts("faction.hover.add");
		// del
		addButton(1, x, y += 18, "gui.remove")
				.setSize(45, 16)
				.setHoverTexts("faction.hover.del");
		// get flag
		addButton(11, x, y + 87, "gui.get")
				.setSize(45, 16)
				.setIsVisible(faction.id > -1)
				.setHoverTexts("faction.hover.flag.get");
		// factions list
		if (scroll == null) { scroll = addScroll(0).setSize(143, 208); }
		add(scroll.setPos(guiLeft + 220, guiTop + 4));
		// sort
		GuiButtonNop button = addCheckBox(14, x, y + 18, "gui.name" ,"ID", sortByName)
				.setSize(45, 12);
		button.setHoverTexts(Component.translatable("hover.sort",
				Component.translatable("global.factions").getString(),
				button.getMessage().getString()));
		if (faction.id == -1) { return; }
		// name
		addTextField(0, guiLeft + 40, guiTop + 4, 136, 16, Util.instance.deleteColor(faction.name))
				.setHoverTexts("faction.hover.name");
		getTextField(0).setMaxStringLength(50);
		// info
		addLabel(0, guiLeft + 8, guiTop + 9, "gui.name");
		addLabel(10, guiLeft + 178, guiTop + 4, "ID");
		addLabel(11, guiLeft + 178, guiTop + 14, faction.id + "");
		// color
		StringBuilder color = new StringBuilder(Integer.toHexString(faction.color));
		while (color.length() < 6) { color.insert(0, "0"); }
		addButton(10, guiLeft + 40, guiTop + 26, color.toString())
				.setSize(60, 16)
				.setHoverTexts("faction.hover.color")
				.setColor(faction.color);
		addLabel(1, guiLeft + 8, guiTop + 31, "gui.color");
		// reset ID
		x = guiLeft + 170;
		y = guiTop + 26;
		int x0 = guiLeft + 8;
		addButton(24, x, y, "gui.reset")
				.setSize(45, 16)
				.setHoverTexts("hover.reset.id");
		// points
		addLabel(2, x0, (y += 18) + 5, "faction.points");
		addButton(2, x, y, "selectServer.edit")
				.setSize(45, 16)
				.setHoverTexts("faction.hover.points");
		// hidden
		addLabel(3, x0, (y += 18) + 5, "faction.hidden");
		addYesNo(3, x, y, faction.hideFaction)
				.setSize(45, 16)
				.setHoverTexts("faction.hover.hide");
		// attacked
		addLabel(4, x0, (y += 18) + 5, "faction.attacked");
		addYesNo(4, x, y, faction.getsAttacked)
				.setSize(45, 16)
				.setHoverTexts("faction.hover.mobs");
		// death points
		addLabel(5, x0, (y += 18) + 5, "faction.ondeath");
		addButton(5, x, y, "faction.points")
				.setSize(45, 16)
				.setHoverTexts("faction.hover.dead.points");
		// hostiles
		addLabel(6, x0, (y += 18) + 5, "faction.hostiles");
		addButton(6, x, y, "selectServer.edit")
				.setSize(45, 16)
				.setHoverTexts("faction.hover.hostiles");
		// friends
		addLabel(7, x0, (y += 18) + 5, "faction.friends");
		addButton(7, x, y, "selectServer.edit")
				.setSize(45, 16)
				.setHoverTexts("faction.hover.addfrends");
		// description
		addLabel(8, x0, (y += 18) + 5, "faction.description");
		addButton(8, x, y, "selectServer.edit")
				.setSize(45, 16)
				.setHoverTexts("faction.hover.description");
		// flag
		addLabel(9, x0, (y += 18) + 5, "faction.flag");
		addButton( 9, x, y, "selectServer.edit")
				.setSize(45, 16)
				.setHoverTexts("faction.hover.flag.txr");
		addTextField(1, x0, y + 18, 208, 16, faction.flag.toString())
				.setHoverTexts("faction.hover.flag.txr");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				save();
				String name = Component.translatable("gui.new").getString();
				while (true) {
					boolean found = false;
					for (Component key : data.keySet()) {
						if (key.getString().equals(name)) {
							name += "_";
							found = true;
							break;
						}
					}
					if (!found) { break; }
				}
				Faction faction = new Faction(-1, name, 0x00FF00, 1000);
				NBTTagCompound compound = new NBTTagCompound();
				faction.save(compound);
				Packets.sendServer(new SPacketFactionSave(compound));
				break;
			} // add new
			case 1: {
				if (data.containsKey(scroll.getNormalSelected())) {
					Packets.sendServer(new SPacketFactionRemove(data.get(scroll.getNormalSelected())));
					scroll.clear();
					faction = new Faction();
					initGui();
				}
				break;
			} // delete
			case 2: setSubGui(new SubGuiNpcFactionPoints(faction)); break;
			case 3: faction.hideFaction = (button.getValue() == 1); break;
			case 4: faction.getsAttacked = (button.getValue() == 1); break;
			case 5: setSubGui(new SubGuiNpcFactionOptions(faction.factions)); break;
			case 6: {
				if (scroll.hasSelected()) {
					HashMap<Component, Integer> corData = new HashMap<>();
					for (Component name : data.keySet()) {
						int id = data.get(name);
						if (faction.id == id || faction.frendFactions.contains(id)) { continue; }
						corData.put(name, id);
					}
					setSubGui(new SubGuiNpcFactionSelect(6, scroll.getSelected(), faction.attackFactions, corData));
				}
				break;
			} // select attack factions
			case 7: {
				if (scroll.hasSelected()) {
					HashMap<Component, Integer> corData = new HashMap<>();
					for (Component name : data.keySet()) {
						int id = data.get(name);
						if (faction.id == id || faction.attackFactions.contains(id)) { continue; }
						corData.put(name, id);
					}
					setSubGui(new SubGuiNpcFactionSelect(7, scroll.getSelected(), faction.frendFactions, corData));
				}
				break;
			} // select frend factions
			case 8: {
				setSubGui(new GuiTextAreaScreen(0, ITextComponent.Serializer.componentToJson(faction.description)));
				break;
			} // description set
			case 9: {
				setSubGui(new SubGuiTextureSelection(this, 0, null, faction.flag.toString(), "png", 4));
				break;
			} // select flag
			case 10: {
				setSubGui(new SubGuiColorSelector(faction.color));
				break;
			} // color
			case 11: {
				if (faction.id >= 0) {
					ItemStack stack = new ItemStack(Items.BANNER);
					stack.setStackDisplayName(faction.name);
					NBTTagCompound nbt = stack.getTagCompound();
					if (nbt == null) { stack.setTagCompound(nbt = new NBTTagCompound()); }
					nbt.setTag("BlockEntityTag", new NBTTagCompound());
					nbt.getCompoundTag("BlockEntityTag").setInteger("FactionID", faction.id);
					Packets.sendServer(new SPacketGiveStack(stack.writeToNBT(new NBTTagCompound())));
				}
				break;
			} // get flag
			case 14: {
				sortByName = ((GuiCheckBoxNop) button).selected();
				if (!data.isEmpty()) {
					Map<String, Integer> dataMap = new HashMap<>();
					for (Map.Entry<Component, Integer> entry : data.entrySet()) {
						Faction f = FactionController.instance.getFaction(entry.getValue());
						if (f != null) { dataMap.put(f.name, entry.getValue()); }
						else {
							String key = entry.getKey().getString();
							if (key.contains(" ")) { dataMap.put(key.substring(key.indexOf(" ") + 1), entry.getValue()); }
							else { dataMap.put(key, entry.getValue()); }
						}
					}
					setData(new Vector<>(dataMap.keySet()), dataMap);
				}
				button.setHoverTexts(Component.translatable("hover.sort",
						Component.translatable("global.factions").getString(),
						button.getMessage().getString()));
				break;
			} // sort type
			case 24: Packets.sendServer(new SPacketGetFirstID(0, faction.id)); break; // reset ID
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLeft + 380.0f, guiTop + 70.0f, 1.0f);
		drawGradientRect(-1, -1, 21, 40, 0xFF404040, 0xFF404040);
		drawGradientRect(0, 0, 20, 39, 0xFFA0A0A0, 0xFFA0A0A0);
		if (faction.id > -1) {
			GlStateManager.scale(0.5f, 0.305f, 1.0f);
			minecraft.getTextureManager().bindTexture(faction.flag);
			drawTexturedModalRect(0, 0, 4, 4, 40, 128);
		}
		GlStateManager.popMatrix();
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (!scroll.hasSelected() && faction.id > -1 && data.containsValue(faction.id)) {
			for (Component name : data.keySet()) {
				if (data.get(name) == faction.id) {
					scroll.setSelected(name);
					break;
				}
			}
		}
	}

	@Override
	public void save() {
		if (scroll != null && scroll.hasSelected() && data.containsKey(scroll.getNormalSelected()) && faction.id > -1) {
			NBTTagCompound compound = new NBTTagCompound();
			faction.save(compound);
			Packets.sendServer(new SPacketFactionSave(compound));
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0 && data.containsKey(scroll.getNormalSelected())) {
			save();
			Packets.sendServer(new SPacketFactionGet(data.get(scroll.getNormalSelected())));
		}
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

	@Override
	public void setData(Vector<String> dataList, Map<String, Integer> dataMap) {
		data.clear();
		List<Entry<String, Integer>> newList = new ArrayList<>(dataMap.entrySet());
		newList.sort((f_0, f_1) -> {
			if (sortByName) { return f_0.getKey().compareTo(f_1.getKey()); }
			else { return f_0.getValue().compareTo(f_1.getValue()); }
		});
		Component select = Component.empty();
		for (Entry<String, Integer> entry : newList) {
			int id = entry.getValue();
			Faction f = FactionController.instance.getFaction(id);
			Component name;
			if (f != null) {
				name = Component.translatable(f.name).withStyle(TextFormatting.RESET).withColor(f.color);
			}
			else { name = Component.translatable(entry.getKey()).withStyle(TextFormatting.RESET); }
			Component key = Component.empty()
					.append(Component.literal("ID:" + id + " ").withStyle(TextFormatting.GRAY))
					.append(name);
			data.put(key, id);
			if (faction.id == id) { select = key; }
		}
		if (scroll != null) {
			scroll.setUnsortedList(new ArrayList<>(data.keySet()))
					.setSelected(select);
		}
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound == null) { return; }
		if (compound.hasKey("MinimumID", 3)) {
			if (faction.id != compound.getInteger("MinimumID")) {
				Packets.sendServer(new SPacketFactionRemove(faction.id));
				faction.id = compound.getInteger("MinimumID");
				compound = new NBTTagCompound();
				faction.save(compound);
				Packets.sendServer(new SPacketFactionSave(compound));
				initGui();
			}
		} else {
			(faction = new Faction()).load(compound);
			setSelected("ID:" + faction.id + " " + faction.name);
			initGui();
		}
	}

	@Override
	public void setSelected(String selected) {
		for (Component key : scroll.getNormalList()) {
			if (Util.instance.equalsDeleteColor(key.getString(), selected, false) && data.containsKey(key)) {
				scroll.setSelected(key);
				return;
			}
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiTextureSelection) {
			faction.flag = ((SubGuiTextureSelection) subgui).resource;
			initGui();
		}
		if (subgui instanceof GuiTextAreaScreen) {
			faction.description = Component.jsonToComponent(((GuiTextAreaScreen) subgui).text).getParent();
		}
		else if (subgui instanceof SubGuiColorSelector) {
			faction.color = ((SubGuiColorSelector) subgui).color;
			initGui();
		} else if (subgui instanceof SubGuiNpcFactionSelect) {
			SubGuiNpcFactionSelect gui = (SubGuiNpcFactionSelect) subgui;
			if (gui.id == 6) {
				faction.attackFactions.clear();
				for (int id : gui.selectFactions) {
					faction.frendFactions.remove(id);
					faction.attackFactions.add(id);
				}
			} else if (gui.id == 7) {
				faction.frendFactions.clear();
				for (int id : gui.selectFactions) {
					faction.attackFactions.remove(id);
					faction.frendFactions.add(id);
				}
			}
			save();
			initGui();
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (faction.id == -1) { return; }
		if (textField.id == 0) {
			String name = textField.getValue();
			while (true) {
				boolean found = false;
				for (Component key : data.keySet()) {
					if (key.getString().equals(name)) {
						name += "_";
						found = true;
						break;
					}
				}
				if (!found) { break; }
			}
			if (!textField.getValue().equals(name)) { textField.setValue(name); }
			if (!name.isEmpty()) {
				Component old = scroll.getNormalSelected();
				data.remove(old);
				faction.name = name;
				Component key = Component.empty()
						.append(Component.literal("ID:" + faction.id + " ").withStyle(TextFormatting.GRAY))
						.append(Component.translatable(name).withStyle(TextFormatting.RESET));
				data.put(key, faction.id);
				scroll.replace(old, key);
			}
			initGui();
		}
		else if (textField.id == 1) { faction.setFlag(textField.getResourceLocation().toString()); }
	}

}
