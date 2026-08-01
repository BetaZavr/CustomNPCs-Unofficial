package noppes.npcs.client.gui.questtypes;

import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.DimensionManager;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.GuiNpcManageQuest;
import noppes.npcs.client.gui.global.SubGuiQuestObjectiveSelect;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.containers.ContainerNpcQuestTypeItem;
import noppes.npcs.controllers.BorderController;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketTeleportTo;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class SubGuiNpcQuestTypeItem
		extends GuiContainerNPCInterface<ContainerNpcQuestTypeItem>
		implements ITextfieldListener {

	public static GuiScreen parent;
	protected final Map<Integer, Integer> dataDimIDs = new HashMap<>();
	protected final QuestObjective task;
	protected final Slot slot;
	protected GuiCustomScrollNop scroll;

	public SubGuiNpcQuestTypeItem(ContainerNpcQuestTypeItem container) {
		super(NoppesUtilServer.getEditingNpc(Minecraft.getMinecraft().player), container, Component.empty());
		setBackground("inventorymenu.png");
		xSize = 176;
		ySize = 217;
		closeOnEsc = true;

		task = container.task;
		slot = container.slot;
	}

	@Override
	public void initGui() {
		super.initGui();
		int lId = 0;
		int x0 = guiLeft + 6;
		int x1 = guiLeft + xSize - 56;
		int y = guiTop + 3;
		// take items
		addLabel(lId++, x0, y + 2, "quest.takeitems")
				.setSize(113, 10);
		addYesNo(0, x1, y, task != null && task.isItemLeave())
				.setSize(50, 14)
				.setHoverTexts("quest.hover.edit.item.leave");
		// ignore damage
		addLabel(lId++, x0, (y += 15) + 2, "gui.ignoreDamage")
				.setSize(113, 10);
		addYesNo(1, x1, y, task == null || task.isIgnoreDamage())
				.setSize(50, 14)
				.setHoverTexts("quest.hover.edit.item.ign.dam");
		// ignore nbt
		addLabel(lId++, x0, (y += 15) + 2, "gui.ignoreNBT")
				.setSize(113, 10);
		addYesNo(2, x1, y, task == null || task.isItemIgnoreNBT())
				.setSize(50, 14)
				.setHoverTexts("quest.hover.edit.item.ign.nbt");
		// item amount
		addLabel(lId++, x0, (y += 15) + 2, "quest.itemamount")
				.setSize(113, 10);
		addTextField(1, x1 + 1, y + 1, 48, 12, task != null ? task.getMaxProgress() : 0)
				.setMinMaxDefault(0, 576, 1).setHoverTexts("quest.hover.edit.item.max", "576");
		// X
		Component compass = Component.translatable("quest.hover.compass");
		addLabel(lId++, x0, (y += 16) + 2, "X:")
				.setSize(12, 10);
		addTextField(10, x0 + 10, y, 38, 12, task != null ? task.pos.getX() : 0)
				.setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task != null ? task.pos.getX() : 0)
				.setHoverTexts(Component.translatable("quest.hover.compass.pos", "X").append(compass));
		// Y
		addLabel(lId++, x0 + 50, y + 2, "Y:")
				.setSize(12, 10);
		addTextField(11, x0 + 60, y, 38, 12, task != null ? task.pos.getY() : 0)
				.setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task != null ? task.pos.getY() : 0)
				.setHoverTexts(Component.translatable("quest.hover.compass.pos", "Y").append(compass));
		// Z
		addLabel(lId++, x0 + 100, y + 2, "Z:")
				.setSize(12, 10);
		addTextField(12, x0 + 110, y, 38, 12, task != null ? task.pos.getZ() : 0)
				.setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task != null ? task.pos.getZ() : 0)
				.setHoverTexts(Component.translatable("quest.hover.compass.pos", "Z").append(compass));
		// tp
		addButton(11, x0 + 150, y - 1, "tp")
				.setSize(14, 14)
				.setHoverTexts("hover.teleport");
		// R
		addLabel(lId++, x0, (y += 14) + 2, "R:")
				.setSize(12, 10);
		addTextField(14, x0 + 10, y, 25, 12, task != null ? task.rangeCompass : 0)
				.setMinMaxDefault(0, 64, task != null ? task.rangeCompass : 0)
				.setHoverTexts(Component.translatable("quest.hover.compass.range").append(compass));
		// N
		addLabel(lId++, x0 + 37, y + 2, "N:")
				.setSize(12, 10);
		addTextField(15, x0 + 47, y, 101, 12, task != null ? task.entityName : "")
				.setHoverTexts(Component.translatable("quest.hover.compass.entity").append(compass));
		addButton(9, x0 + 150, y - 1, "")
				.setSize(14, 14)
				.setIsAnim(true)
				.setTexture(GuiBasic.ANIMATION_BUTTONS)
				.setUV(220, 96, 36, 36)
				.setHoverTexts("color.hover")
				.layerColor = task != null ? task.colorCompass | 0xFF000000 : -1;
		// dim ID
		addLabel(lId++, x0 + 21, (y += 16) + 2, "D:")
				.setSize(12, 10);
		int p = 0;
		int i = 0;
		List<Integer> ids = Arrays.asList(DimensionManager.getStaticDimensionIDs());
		Collections.sort(ids);
		Object[] dimIDs = new Object[ids.size()];
		dataDimIDs.clear();
		for (int id : ids) {
			dimIDs[i] = id + "";
			dataDimIDs.put(i, id);
			if (task != null && id == task.dimension) { p = i; }
			i++;
		}
		addButton(4, x0 + 29, y - 1, false, p, dimIDs)
				.setSize(74, 16)
				.setHoverTexts(Component.translatable("quest.hover.compass.dim", dimIDs[p]).append(compass));
		// region ID
		addLabel(lId, x0 + 105, y + 2, "P:")
				.setSize(12, 10);
		addTextField(9, x0 + 115, y, 30, 14, task != null ? task.regionID : "")
				.setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task != null ? task.regionID : 0)
				.setHoverTexts(Component.translatable("quest.hover.compass.reg", task != null ? task.regionID : 0).append(compass));
		// set player pos
		addButton(10, x0 + 147, y - 1, "S")
				.setSize(16, 16)
				.setHoverTexts(Component.translatable("quest.hover.compass.set").append(compass));
		// exit
		addButton(66, x0, y = guiTop + ySize - 25, "gui.back")
				.setSize(40, 20)
				.setHoverTexts("hover.back");
		// mini map point
		addCheckBox(5, x0 + 42, y + 2, "quest.set.minimap.point", null, task != null && task.isSetPointOnMiniMap())
				.setSize(xSize - 52, 16)
				.setHoverTexts("quest.hover.set.minimap.point");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (task == null) { return; }
		switch (button.id) {
			case 0: task.setItemLeave(button.getValue() == 0); break;
			case 1: task.setItemIgnoreDamage(((GuiButtonYesNo) button).getBoolean()); break;
			case 2: task.setItemIgnoreNBT(((GuiButtonYesNo) button).getBoolean()); break;
			case 4: {
				if (!dataDimIDs.containsKey(button.getValue())) { return; }
				task.dimension = dataDimIDs.get(button.getValue());
				button.setHoverTexts(Component.translatable("quest.hover.compass.dim", "" + task.dimension).append(Component.translatable("quest.hover.compass")));
				break;
			} // dimension
			case 5: task.setPointOnMiniMap(((GuiCheckBoxNop) button).selected()); break;
			case 9: {
				setSubGui(new SubGuiColorSelector(task.colorCompass, new SubGuiColorSelector.ColorCallback() {
					@Override
					public void color(int colorIn) {
						task.setCompassColor(colorIn);
						initGui();
					}
					@Override
					public void preColor(int colorIn) {
						task.setCompassColor(colorIn);
					}
				}));
				break;
			} // TP
			case 10: {
				task.pos = player.getPosition();
				task.dimension = player.world.provider.getDimension();
				initGui();
				break;
			} // set player pos
			case 11: Packets.sendServer(new SPacketTeleportTo(task.dimension, task.pos));
			case 66: onClose(); break;
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		drawCenteredString(font,
				Component.translatable("quest.title." + (task.getEnumType() == EnumQuestTask.ITEM ? "item" : "craft")).getFormattedText(),
				guiLeft + xSize / 2, guiTop - 12, 0xFFFFFFFF);
		// Back
		if (background != null) {
			GlStateManager.pushMatrix();
			GlStateManager.translate((float) guiLeft, (float) guiTop, 0.0F);
			GlStateManager.scale(bgScale, bgScale, bgScale);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			minecraft.getTextureManager().bindTexture(background);
			drawTexturedModalRect(0, 0, 0, 0, xSize - 4, ySize);
			drawTexturedModalRect(xSize - 4, 0, 252, 0, 4, ySize);
			drawTexturedModalRect(slot.xPos - 1, slot.yPos - 1, 7, 112, 18, 18);
			GlStateManager.popMatrix();
		}
	}

	public void unFocused(GuiTextFieldNop textField) {
		if (task == null) { return; }
		switch (textField.id) {
			case 1: task.setMaxProgress(textField.getInteger()); break;
			case 2: task.setAreaRange(textField.getInteger()); break;
			case 9: {
				if (!BorderController.getInstance().regions.containsKey(textField.getInteger())) {
					textField.setValue("" + textField.def);
					return;
				}
				task.regionID = textField.getInteger();
				textField.setHoverTexts(Component.translatable("quest.hover.compass.reg", "" + task.regionID).append(Component.translatable("quest.hover.compass")));
				break;
			}
			case 10: task.pos = new BlockPos(textField.getInteger(), task.pos.getY(), task.pos.getZ()); break;
			case 11: task.pos = new BlockPos(task.pos.getX(), textField.getInteger(), task.pos.getZ()); break;
			case 12: task.pos = new BlockPos(task.pos.getX(), task.pos.getY(), textField.getInteger()); break;
			case 14: task.rangeCompass = textField.getInteger(); break;
			case 15: task.entityName = textField.getValue(); break;
		}
	}

	@Override
	public void onClose() {
		super.onClose();
		task.setItem(slot.getStack());
		if (task.getItem().isEmpty()) {
			NoppesUtilServer.getEditingQuest(player).questInterface.removeTask(task);
			NoppesUtil.openGUI(player, GuiNpcManageQuest.Instance);
			return;
		}
		if (GuiNpcManageQuest.Instance.getSubGui() instanceof SubGuiQuestObjectiveSelect) {
			((SubGuiQuestObjectiveSelect) GuiNpcManageQuest.Instance.getSubGui()).onClose();
		}
		setScreen(GuiNpcManageQuest.Instance);
	}

	@Override
	public void save() {
		task.setMaxProgress(getTextField(1).getInteger());
		for (QuestObjective taskObj : NoppesUtilServer.getEditingQuest(player).questInterface.tasks) {
			if (taskObj == task || taskObj.getEnumType() != task.getEnumType()) { continue; }
			if (NoppesUtilPlayer.compareItems(taskObj.getItemStack(), task.getItemStack(), task.isIgnoreDamage(), task.isItemIgnoreNBT())) {
				task.setItem(ItemStack.EMPTY);
				break;
			}
		}
	}

}
