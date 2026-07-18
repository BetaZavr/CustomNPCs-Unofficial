package noppes.npcs.client.gui.global;

import java.util.*;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerManageBanks;
import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.inv.ISlotMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.IScrollData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.ValueUtil;

public class GuiNpcManageBanks
		extends GuiContainerNPCInterface2<ContainerManageBanks>
		implements IScrollData, ICustomScrollListener, ITextfieldListener, IGuiData {

	protected final HashMap<Component, Integer> data = new HashMap<>();
	protected final ContainerManageBanks container;
	protected final Bank bank = new Bank();
	protected GuiCustomScrollNop scroll;
	protected Component selected = Component.empty();
	protected boolean isWait;
	protected int ceil = 0;

	public GuiNpcManageBanks(EntityNPCInterface npc, ContainerManageBanks containerIn) {
		super(npc, containerIn, Component.empty());
		setBackground("inventorymenu.png");
		drawDefaultBackground = false;
		closeOnEsc = true;
		ySize = 200;
		backGui = EnumGuiType.MainMenuGlobal;

		container = containerIn;
		isWait = true;
		Packets.sendServer(new SPacketBanksGet());
	}

	@Override
	public void initGui() {
		super.initGui();
		if (scroll == null) { scroll = addScroll(0).setSize(160, 185); }
		if (isWait) { return; }
		int x = guiLeft + 255;
		int y = guiTop + 5;
		add(scroll.setPos(x, y));
		scroll.setSelected(selected);
		selected = scroll.getNormalSelected();
		for (int slotId = 0; slotId < 2; slotId++) {
			((ISlotMixin) container.getSlot(slotId)).setX(selected.getFormattedText().isEmpty() ? -5000 : 180);
			((ISlotMixin) container.getSlot(slotId)).setY(selected.getFormattedText().isEmpty() ? -5000 : slotId == 0 ? 107 : 166);
		}
		List<Component> list = scroll.getNormalList();
		LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
		if (list != null && !list.isEmpty()) {
			int i = 0;
			for (Component key : list) {
				hts.put(i, Collections.singletonList(Component.literal("ID: " + data.get(key))));
				i++;
			}
		}
		scroll.setHoverTexts(hts);
		boolean hasSelectBank = !selected.getFormattedText().isEmpty();
		Component change = Component.translatable("bank.hover.change");
		// add bank
		y += scroll.height + 24;
		addButton(6, x, y, "gui.add")
				.setSize(50, 20)
				.setHoverTexts("bank.hover.add");
		// del bank
		addButton(7, x + scroll.width - 50, y, "gui.remove")
				.setSize(50, 20)
				.setIsEnabled(hasSelectBank && data.size() > 1)
				.setHoverTexts(Component.translatable("bank.hover.del")
						.append(change));
		// name
		int x0 = guiLeft + 5;
		x = x0 + 45;
		y = guiTop + 6;
		int lId = 0;
		addLabel(lId++, x0, y + 4, Component.translatable("gui.name").append(":"))
				.setSize(68, 10)
				.setIsVisible(hasSelectBank);
		addTextField(0, x, y, 202, 18, bank.name)
				.setIsVisible(hasSelectBank)
				.setMaxStringLength(20)
				.setHoverTexts(Component.translatable("bank.hover.name")
						.append("<br>\"").append(Component.translatable(bank.name)).append("\""));
		// cells
		y += 22;
		addLabel(lId++, x0, y + 4, Component.translatable("gui.ceil", ":"))
				.setSize(68, 10)
				.setIsVisible(hasSelectBank);
		List<String> csIds = new ArrayList<>();
		for (int i = 0; i < bank.ceilSettings.size(); i++) { csIds.add("" + (i + 1)); }
		addButton(0, x, y, true, ceil, csIds.toArray(new Object[0]))
				.setSize(66, 20)
				.setIsVisible(hasSelectBank)
				.setHoverTexts(Component.translatable("bank.hover.ceil", "" + bank.ceilSettings.size()));
		// add ceil
		addButton(1, x + 68, y, "gui.add")
				.setSize(66, 20)
				.setIsVisible(hasSelectBank)
				.setHoverTexts(Component.translatable("bank.hover.ceil.add").append(change));
		// del ceil
		addButton(2, x + 137, y, "gui.remove")
				.setSize(66, 20)
				.setIsVisible(hasSelectBank)
				.setIsEnabled(ceil > 0)
				.setHoverTexts(Component.translatable("bank.hover.ceil.add").append(change));
		// slots
		y += 22;
		Bank.CeilSettings cs = bank.ceilSettings.get(ceil);
		int sc = cs.startCells;
		int mc = cs.maxCells;
		// min
		addLabel(lId++, x0, y + 4, Component.translatable("gui.start").append(":"))
				.setSize(68, 10)
				.setIsVisible(hasSelectBank);
		addTextField(1, x + 1, y + 1, 64, 18, "" + sc)
				.setIsVisible(hasSelectBank)
				.setMinMaxDefault(1, mc, sc)
				.setHoverTexts(Component.translatable("bank.hover.slots.min").append(change));
		// max
		addLabel(lId++, x + 68, y + 4, Component.translatable("gui.max").append(":"))
				.setSize(48, 10)
				.setIsVisible(hasSelectBank);
		addTextField(2, x + 138, y + 1, 64, 18, "" + mc)
				.setIsVisible(hasSelectBank)
				.setMinMaxDefault(1, 198, mc)
				.setHoverTexts(Component.translatable("bank.hover.slots.max").append(change));
		// is public
		addCheckBox(3, x0, (y += 22), "bank.public.true", "bank.public.false", bank.isPublic)
				.setSize(180, 16)
				.setIsVisible(hasSelectBank)
				.setHoverTexts("bank.hover.public");
		// setting names
		Component hoverOwner = Component.translatable("bank.hover.settings");
		if (!bank.owner.isEmpty()) {
			hoverOwner.append("<br>")
					.append(Component.empty()
							.append(Component.translatable("bank.owner").append(": ").withStyle(TextFormatting.GRAY))
							.append(bank.owner));
		}
		// lock settings
		addButton(8, x0 + 182, y, "")
				.setTexture(GuiBasic.WIDGETS) // lock
				.setUV(bank.owner.isEmpty() ? 20 : 0, 146, 20, 20)
				.setSize(20, 20)
				.setIsVisible(hasSelectBank && bank.isPublic)
				.setHoverTexts(hoverOwner);
		// is free
		addCheckBox(4, x0, y + 22, "bank.free.true", "bank.free.false", cs.isFree)
				.setSize(180, 16)
				.setIsVisible(hasSelectBank)
				.setHoverTexts("bank.hover.free");

		// open money
		x = guiLeft + 201;
		y = guiTop + 95;
		addLabel(lId++, x - 22, y, Component.translatable("bank.tab.cost").append(":"))
				.setSize(68, 10)
				.setIsVisible(hasSelectBank);
		addTextField(3, x, y += 31, 51, 12, "" + cs.openMoney)
				.setIsVisible(hasSelectBank)
				.setMinMaxDefault(0, Integer.MAX_VALUE, cs.openMoney)
				.setHoverTexts("bank.hover.open.money");
		addTextField(5, x, y += 14, 51, 12, "" + cs.openDonat)
				.setIsVisible(hasSelectBank)
				.setMinMaxDefault(0, Integer.MAX_VALUE, cs.openDonat)
				.setHoverTexts("bank.hover.open.donat");
		// upgrade money
		addLabel(lId, x - 22, y += 14, Component.translatable("bank.upg.cost").append(":"))
				.setSize(68, 10)
				.setIsVisible(hasSelectBank);
		addTextField(4, x, y += 31, 51, 12, "" + cs.upgradeMoney)
				.setIsVisible(hasSelectBank)
				.setMinMaxDefault(0, Integer.MAX_VALUE, cs.upgradeMoney)
				.setHoverTexts("bank.hover.upgrade.money");
		addTextField(6, x, y + 14, 51, 12, "" + cs.upgradeDonat)
				.setIsVisible(hasSelectBank)
				.setMinMaxDefault(0, Integer.MAX_VALUE, cs.upgradeDonat)
				.setHoverTexts("bank.hover.upgrade.donat");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				if (ceil == button.getValue()) { return; }
				save();
				ceil = button.getValue();
				initGui();
				break;
			} // select ceil
			case 1: {
				ceil = bank.addCeil().ceil;
				initGui();
				break;
			} // add ceil
			case 2: {
				if (!data.containsKey(selected) || !bank.ceilSettings.containsKey(ceil) || bank.ceilSettings.size() < 2) { return; }
				String msg = getMessage("bank.hover.ceil.del");
				ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
					if (bo && data.containsKey(selected) && bank.ceilSettings.size() > 1 && bank.ceilSettings.containsKey(ceil)) {
						bank.removeCeil(ceil);
						ceil = ValueUtil.correctInt(ceil - 1, 0, Integer.MAX_VALUE);
						initGui();
					}
					NoppesUtil.openGUI(player, this);
				},
						Component.translatable("bank.name", ": ID:" + bank.id + " \"" + bank.name + "\"; " +
								Component.translatable("gui.ceil", ": ID:" + (ceil + 1)).getFormattedText()).getParent(),
						Component.literal(msg).getParent());
				setScreen(guiYesNo);
				break;
			} // remove ceil
			case 3: bank.isPublic = ((GuiCheckBoxNop) button).selected(); initGui(); break; // is public
			case 4: bank.ceilSettings.get(ceil).isFree = ((GuiCheckBoxNop) button).selected(); break; // is public
			case 6: {
				save();
				Bank b = BankController.getInstance().addNewBank();
				ceil = 0;
				selected = Component.literal(b.name);
				bank.load(b.save());
				Packets.sendServer(new SPacketBankSave(ceil, b.save()));
				Packets.sendServer(new SPacketBankGet(bank.id, ceil));
				initGui();
				break;
			} // add bank
			case 7: {
				if (!data.containsKey(selected)) { return; }
				String msg = getMessage("bank.hover.del");
				ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
					if (bo && data.containsKey(selected)) {
						Packets.sendServer(new SPacketBankRemove(bank.id));
						ceil = 0;
						selected = Component.empty();
						scroll.clear();
						BankController.getInstance().removeBank(bank.id);
						initGui();
					}
					NoppesUtil.openGUI(player, this);
				},
						Component.translatable("bank.name", ": ID:" + bank.id + " \"" + bank.name + "\"").getParent(),
						Component.literal(msg).getParent());
				setScreen(guiYesNo);
				break;
			} // remove bank
			case 8: {
				if (bank == null) { return; }
				setSubGui(new SubGuiEditBankAccess(bank));
				break;
			} // settings
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		if (isWait || hasSubGui()) { return; }
		for (int slotId = 0; slotId < 2; ++slotId) {
			Slot slot = container.getSlot(slotId);
			int x = guiLeft + slot.xPos;
			int y = guiTop + slot.yPos;
			minecraft.getTextureManager().bindTexture(GuiBasic.RESOURCE_SLOT);
			drawTexturedModalRect(x - 1, y - 1, 0, 0, 18, 18);
			GlStateManager.pushMatrix();
			GlStateManager.translate(x + 2, y + 15, 0.0f);
			float s = 16.0f / 250.f;
			GlStateManager.scale(s, s, s);
			minecraft.getTextureManager().bindTexture(GuiBasic.MONEY);
			drawTexturedModalRect(0, 0, 0, 0, 256, 256);

			GlStateManager.translate(0.0f, 256.0f, 0.0f);
			minecraft.getTextureManager().bindTexture(GuiBasic.DONAT);
			drawTexturedModalRect(0, 0, 0, 0, 256, 256);

			GlStateManager.popMatrix();
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (isWait) {
			Component text = Component.translatable("gui.wait", ": " + Component.translatable("gui.wait.data").getFormattedText());
			font.drawString(text.getFormattedText(), guiLeft + (float) (width - font.getStringWidth(text.getFormattedText())) / 2.0f, guiTop + 60,
					CustomNpcResourceListener.DefaultTextColor, false);
			return;
		}
		if (hasSubGui() || !CustomNpcs.ShowDescriptions || selected.getFormattedText().isEmpty()) { return; }
		for (int slotId = 0; slotId < 2; ++slotId) {
			Slot slot = container.getSlot(slotId);
			if (!slot.getHasStack() && isMouseHover(mouseX, mouseY, guiLeft + slot.xPos, guiTop + slot.yPos, 18, 18)) {
				drawHoverText("bank." + (slotId == 0 ? "tab" : "upg") + ".cost.info");
			}
		}
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		bank.load(compound);
		if (compound.hasKey("CurrentCeil", 3)) { ceil = compound.getInteger("CurrentCeil"); }
		container.setBank(bank, ceil);
		selected = Component.translatable(bank.name);
		isWait = false;
		initGui();
	}

	@Override
	public void setData(Vector<String> dataList, Map<String, Integer> dataMap) {
		data.clear();
		Map<Component, Integer> map = new HashMap<>();
		for (Map.Entry<String, Integer> entry : dataMap.entrySet()) {
			map.put(Component.empty()
					.append(Component.literal("ID:" + entry.getValue() + " ").withStyle(TextFormatting.GRAY))
					.append(Component.literal(entry.getKey()).withStyle(TextFormatting.RESET)), entry.getValue());
		}
		data.putAll(map);
		scroll.setNormalList(new ArrayList<>(data.keySet()))
				.setSelected(selected);
		selected = scroll.getNormalSelected();
		if (data.containsKey(selected)) {
			Bank b = BankController.getInstance().getBank(data.get(selected));
			if (b != null) {
				bank.load(b.save());
			}
		}
		isWait = false;
		initGui();
	}

	@Override
	public void setSelected(String selectedIn) { }

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0 && !selected.getFormattedText().equals(scroll.getSelected()) && data.containsKey(scroll.getNormalSelected())) {
			save();
			ceil = 0;
			selected = scroll.getNormalSelected();
			int id = data.get(selected);
			Bank b = BankController.getInstance().getBank(id);
			if (b != null) { bank.load(b.save()); }
			Packets.sendServer(new SPacketBankGet(id, ceil));
			isWait = true;
			initGui();
		}
	}

	@Override
	public void save() {
		if (selected != null && data.containsKey(selected) && bank != null && bank.id >= 0 && bank.ceilSettings.containsKey(ceil)) {
			bank.ceilSettings.get(ceil).openStack = container.getSlot(0).getStack();
			bank.ceilSettings.get(ceil).upgradeStack = container.getSlot(1).getStack();
			Packets.sendServer(new SPacketBankSave(ceil, bank.save()));
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (bank.id == -1) { return; }
		switch (textField.id) {
			case 0: {
				String name = textField.getValue();
				if (!name.isEmpty()) {
					boolean has = false;
					Component old = null;
					for (Component c : data.keySet()) {
						if (c.getString().equals(name)) { has = true; break; }
						if (c.getString().equals(bank.name)) { old = c; }
					}
					if (!has && old != null) {
						data.remove(old);
						bank.name = name;
						selected = Component.translatable(name);
						data.put(selected, bank.id);
						scroll.replace(old, selected);
					}
				}
				break;
			} // name
			case 1: {
				if (!textField.isInteger()) {
					textField.setValue(textField.def);
					return;
				}
				bank.ceilSettings.get(ceil).startCells = textField.getInteger();
				break;
			} // startCells
			case 2: {
				if (!textField.isInteger()) {
					textField.setValue(textField.def);
					return;
				}
				bank.ceilSettings.get(ceil).maxCells = textField.getInteger();
				break;
			} // maxCells
			case 3: {
				if (!textField.isInteger()) {
					textField.setValue(textField.def);
					return;
				}
				bank.ceilSettings.get(ceil).openMoney = textField.getInteger();
				break;
			} // open money
			case 4: {
				if (!textField.isInteger()) {
					textField.setValue(textField.def);
					return;
				}
				bank.ceilSettings.get(ceil).upgradeMoney = textField.getInteger();
				break;
			} // upgrade money
			case 5: {
				if (!textField.isInteger()) {
					textField.setValue(textField.def);
					return;
				}
				bank.ceilSettings.get(ceil).openDonat = textField.getInteger();
				break;
			} // open donat
			case 6: {
				if (!textField.isInteger()) {
					textField.setValue(textField.def);
					return;
				}
				bank.ceilSettings.get(ceil).upgradeDonat = textField.getInteger();
				break;
			} // upgrade donat
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiEditBankAccess) {
			SubGuiEditBankAccess gui = (SubGuiEditBankAccess) subgui;
			if (bank.isChanging != gui.isChanging) { bank.isChanging = gui.isChanging; }
			if (!bank.owner.equals(gui.owner)) { bank.owner = gui.owner; }
			if (gui.names.size() != bank.access.size()) {
				bank.access.clear();
				bank.access.addAll(gui.names);
			} else {
				for (String name : gui.names) {
					if (bank.access.contains(name)) { continue; }
					bank.access.clear();
					bank.access.addAll(gui.names);
					break;
				}
			}
		}
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

	private String getMessage(String locKey) {
		String str = Component.translatable(locKey).getFormattedText();
		while (str.contains("<br>")) { str = str.replace("<br>", "" + ((char) 10)); }
		return str;
	}

}
