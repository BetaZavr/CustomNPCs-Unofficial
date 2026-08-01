package noppes.npcs.client.gui.global;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.SubGuiNpcMobSpawnerSelector;
import noppes.npcs.client.gui.SubGuiNpcBiomes;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.SpawnData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNaturalSpawnGet;
import noppes.npcs.packets.server.SPacketNaturalSpawnGetAll;
import noppes.npcs.packets.server.SPacketNaturalSpawnRemove;
import noppes.npcs.packets.server.SPacketNaturalSpawnSave;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.*;
import noppes.npcs.util.ValueUtil;

// Changed by Unofficial (BetaZavr)
public class GuiNpcNaturalSpawns extends GuiNPCInterface2
		implements IGuiData, IScrollData, ITextfieldListener, ICustomScrollListener, ISliderListener, GuiYesNoCallback {

	protected final HashMap<Component, Integer> data = new HashMap<>();
	protected GuiCustomScrollNop scroll;
	protected SpawnData spawn = new SpawnData();
	protected Entity displayNpc = null;
	protected boolean accept = false;
	protected int maxSize;

	public GuiNpcNaturalSpawns(EntityNPCInterface npc) {
		super(npc);

		backGui = EnumGuiType.MainMenuGlobal;
		Packets.sendServer(new SPacketNaturalSpawnGetAll());
	}

	@Override
	public void initGui() {
		super.initGui();
		if (scroll == null) { scroll = addScroll(0).setSize(143, 208); }
		add(scroll.setPos(guiLeft + 214, guiTop + 4));
		addButton(1, guiLeft + 358, guiTop + 38, "gui.add")
				.setSize(58, 20)
				.setHoverTexts("spawning.hover.add");
		addButton(2, guiLeft + 358, guiTop + 61, "gui.remove")
				.setSize(58, 20)
				.setHoverTexts("spawning.hover.del");
		if (spawn.id < 0) { return; }
		// entity max size
		Entity entity = null;
		try { entity = EntityList.createEntityFromNBT(spawn.getCompound(), player.world); } catch (Exception ignored) { }
		maxSize = 50;
		if (entity instanceof EntityNPCInterface) { maxSize = 70; }
		else if (entity instanceof EntityAnimal) { maxSize = 10; }
		else if (entity instanceof EntityMob) { maxSize = 70; }
		// Spawner name
		int lId = 0;
		int x = guiLeft + 5;
		int y = guiTop + 5;
		addLabel(lId++, x, y + 3, "gui.title");
		addTextField(1, x + 56, y, 150, 14, spawn.name)
				.setHoverTexts("spawning.hover.name");
		// Biomes
		addLabel(lId++, x, (y += 17) + 3, "spawning.biomes");
		addButton(3, x + 56, y, "selectServer.edit")
				.setSize(74, 16)
				.setColor(spawn.biomes.isEmpty() ? new Color(0xFFF02020).getRGB() : 0)
				.setHoverTexts("spawning.hover.biomes");
		// spawner type
		addLabel(lId++, x, (y += 18) + 3, "gui.type");
		addButton(27, x + 56, y, false, spawn.type, "spawner.any", "spawner.dark", "spawner.light")
				.setSize(74, 16)
				.setHoverTexts("spawning.hover.type");
		addButton(4, x + 132, y, false, spawn.liquid ? 0 : 1, "spawning.liquid.0", "spawning.liquid.1")
				.setSize(74, 16)
				.setHoverTexts("spawning.hover.liquid." + (spawn.liquid ? 0 : 1));
		// select entity
		addButton(5, x, y += 18, getTitle(spawn.getCompound()))
				.setSize(184, 16)
				.setHoverTexts("spawning.hover.sel.npc");
		addButton(25, x + 186, y, "X")
				.setSize(20, 16)
				.setHoverTexts("spawning.hover.del.npc");
		// chance
		addLabel(lId++, x, (y += 29) - 10, Component.translatable("spawning.weightedChance").append(":"));
		addSlider(2, x, y, (float) spawn.itemWeight / 100.0f)
				.setSize(160, 12)
				.setHoverTexts("spawning.hover.chance");
		addTextField(2, x + 163, y, 43, 12, "" + spawn.itemWeight)
				.setMinMaxDefault(1, 100, spawn.itemWeight)
				.setHoverTexts("spawning.hover.chance");
		// group size
		addLabel(lId++, x, (y += 25) - 10, "spawning.group");
		addSlider(3, x, y, (float) spawn.group / 8.0f)
				.setSize(160, 12)
				.setHoverTexts("spawning.hover.chance");
		addTextField(3, x + 163, y, 43, 12, "" + spawn.group)
				.setMinMaxDefault(1, 8, spawn.group)
				.setHoverTexts("spawning.hover.group");
		// distance
		addLabel(lId++, x, (y += 25) - 10, "spawning.range");
		addSlider(4, x, y, (float) spawn.range / 16.0f)
				.setSize(160, 12)
				.setHoverTexts("spawning.hover.chance");
		addTextField(4, x + 163, y, 43, 12, "" + spawn.range)
				.setMinMaxDefault(1, 16, spawn.range)
				.setHoverTexts("spawning.hover.range");
		// maximum in player
		addLabel(lId, x, (y += 25) - 10, "spawning.maximum.in");
		addSlider(5, x, y, (float) spawn.maxNearPlayer / (float) maxSize)
				.setSize(160, 12)
				.setHoverTexts(Component.translatable("spawning.hover.maximum.in", TextFormatting.GOLD + "" + maxSize));
		addTextField(5, x + 163, y, 43, 12, "" + spawn.maxNearPlayer)
				.setMinMaxDefault(1, maxSize, spawn.maxNearPlayer)
				.setHoverTexts(Component.translatable("spawning.hover.maximum.in", TextFormatting.GOLD + "" + maxSize));
		// player can see
		addCheckBox(6, x, y + 14, "spawning.can.see", "spawning.not.see", spawn.canSeeSummon)
				.setSize(184, 14)
				.setHoverTexts("spawning.hover.can.see");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 1: {
				if (!accept) {
					ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
						if (agree) {
							accept = true;
							buttonEvent(getButton(1));
						}
						NoppesUtil.openGUI(player, this);
					},
							Component.translatable("spawning.accept.message").getParent(),
							Component.translatable("gui.acceptMessage").getParent());
					setScreen(guiYesNo);
				}
				else {
					save();
					String name = Component.translatable("gui.new").getString();
					while (true) {
						boolean found = false;
						for (Component key : data.keySet()) {
							if (key.getString().equals(name)) {
								found = true;
								name += "_";
								break;
							}
						}
						if (!found) { break;}
					}
					SpawnData spawn = new SpawnData();
					spawn.name = name;
					Packets.sendServer(new SPacketNaturalSpawnSave(spawn.save(new NBTTagCompound())));
				}
				break;
			} // add
			case 2: {
				if (data.containsKey(scroll.getNormalSelected())) {
					Packets.sendServer(new SPacketNaturalSpawnRemove(spawn.id));
					spawn = new SpawnData();
					scroll.clear();
					displayNpc = null;
				}
				break;
			} // remove
			case 3: setSubGui(new SubGuiNpcBiomes(spawn)); break; // set biome
			case 4: {
				spawn.liquid = button.getValue() == 0;
				button.setHoverTexts("spawning.hover.liquid." + button.getValue());
				break;
			} // set liquid
			case 5: setSubGui(new SubGuiNpcMobSpawnerSelector(null)); break; // select npc
			case 6: spawn.canSeeSummon = ((GuiCheckBoxNop) button).selected(); break; // select npc
			case 25: {
				spawn.setCompound(new NBTTagCompound());
				displayNpc = null;
				initGui();
				break;
			} // clear entity
			case 27: spawn.type = button.getValue(); break; // type
		}
	}

	@Override
	public void subGuiClosed(GuiScreen gui) {
		if (gui instanceof SubGuiNpcMobSpawnerSelector) {
			SubGuiNpcMobSpawnerSelector selector = (SubGuiNpcMobSpawnerSelector) gui;
			NBTTagCompound compound = selector.getCompound();
			if (compound != null) {
				spawn.setCompound(compound);
				if (compound.hasKey("SpawnCycle", 3)) { compound.setInteger("SpawnCycle", 4); }
			}
		}
		initGui();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (!hasSubGui()) {
			int r;
			int p = 0;
			int x = 387;
			int y = 196;
			GlStateManager.pushMatrix();
			GlStateManager.translate(0.0f, 0.0f, 1.0f);
			Gui.drawRect(guiLeft + x - 30, guiTop + y - 77, guiLeft + x + 31, guiTop + y + 9, 0xFF808080);
			Gui.drawRect(guiLeft + x - 29, guiTop + y - 76, guiLeft + x + 30, guiTop + y + 8, 0xFF000000);
			GlStateManager.popMatrix();
			if (displayNpc != null) {
				displayNpc.ticksExisted = player.ticksExisted;
				if (displayNpc instanceof EntityLivingBase) { r = (int) (3 * player.world.getTotalWorldTime() % 360); }
				else {
					r = 0;
					y -= 34;
					if (displayNpc instanceof EntityItem) {
						p = 30;
						y += 10;
					}
					if (displayNpc instanceof EntityItemFrame) { x += 16; }
				}
				GlStateManager.pushMatrix();
				drawNpc(displayNpc, x, y, 1.0f, r, p, 0);
				GlStateManager.popMatrix();
			}
		}
	}

	@Override
	public void mouseDragged(GuiSliderNop guiNpcSlider) {
		String value;
		switch (guiNpcSlider.id) {
			case 2: {
				spawn.itemWeight = ValueUtil.correctInt((int) (guiNpcSlider.sliderValue * 100.0f), 1, 100);
				value = "" + spawn.itemWeight;
				guiNpcSlider.setMessage(Component.literal(value + "%"));
				break;
			} // chance
			case 3: {
				spawn.group = ValueUtil.correctInt((int) (guiNpcSlider.sliderValue * 8.0f), 1, 8);
				value = "" + spawn.group;
				guiNpcSlider.setMessage(Component.literal(value));
				break;
			} // group
			case 4: {
				spawn.range = ValueUtil.correctInt((int) (guiNpcSlider.sliderValue * 16.0f), 1, 16);
				value = "" + spawn.range;
				guiNpcSlider.setMessage(Component.literal(value));
				break;
			} // range
			case 5: {
				spawn.maxNearPlayer = ValueUtil.correctInt((int) (guiNpcSlider.sliderValue * (float) maxSize), 1, maxSize);
				value = "" + spawn.maxNearPlayer;
				guiNpcSlider.setMessage(Component.literal(value));
				break;
			} // maximum near player
			default: return;
		}
		if (getTextField(guiNpcSlider.id) != null) { getTextField(guiNpcSlider.id).setValue(value); }
	}

	@Override
	public void mousePressed(GuiSliderNop guiNpcSlider) { }

	@Override
	public void mouseReleased(GuiSliderNop guiNpcSlider) {
		if (guiNpcSlider.id == 2) {
			spawn.itemWeight = ValueUtil.correctInt((int) (guiNpcSlider.sliderValue * 100.0f), 1, 100);
		}
	}

	@Override
	public void save() {
		GuiTextFieldNop.unfocus();
		if (spawn.id >= 0) { Packets.sendServer(new SPacketNaturalSpawnSave(spawn.save(new NBTTagCompound()))); }
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0 && data.containsKey(scroll.getNormalSelected())) {
			save();
			spawn = new SpawnData();
			Packets.sendServer(new SPacketNaturalSpawnGet(data.get(scroll.getNormalSelected())));
			initGui();
		}
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

	@Override
	public void setData(Vector<String> dataList, Map<String, Integer> dataMap) {
		String name = scroll.getSelected();
		data.clear();
		for (Map.Entry<String, Integer> entry : dataMap.entrySet()) {
			data.put(Component.translatable(entry.getKey()), entry.getValue());
		}
		scroll.setNormalList(new ArrayList<>(data.keySet()));
		scroll.setSelected(name);
		initGui();
	}

	@Override
	public void setSelected(String selected) { }

	@Override
	public void setGuiData(NBTTagCompound compound) {
		spawn.load(compound);
		setSelected(spawn.name);
		initGui();
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 1: {
				String name = textField.getValue();
				boolean found = false;
				for (Component key : data.keySet()) {
					if (key.getString().equals(name)) {
						found = true;
						break;
					}
				}
				if (name.isEmpty() || found) { textField.setValue(spawn.name); }
				else {
					for (Component key : new ArrayList<>(data.keySet())) {
						if (key.getString().equals(spawn.name)) {
							data.remove(key);
							spawn.name = name;
							Component newKey = Component.translatable(spawn.name);
							data.put(newKey, spawn.id);
							scroll.replace(key, newKey);
							break;
						}
					}
				}
				break;
			}
			case 2: {
				spawn.itemWeight = textField.getInteger();
				if (getSlider(4) != null) {
					getSlider(4).setMessage(Component.translatable("spawning.weightedChance")
						.append(": ")
						.append(String.valueOf(spawn.itemWeight)));
				}
				break;
			}
			case 3: spawn.group = textField.getInteger(); break;
			case 4: spawn.range = textField.getInteger(); break;
			case 5: spawn.maxNearPlayer = textField.getInteger(); break;
		}
	}

	private Component getTitle(NBTTagCompound compound) {
		displayNpc = EntityList.createEntityFromNBT(compound, player.world);
		if (displayNpc != null) { return Component.literal(displayNpc.getName()); }
		return Component.translatable("gui.selectnpc");
	}

}
