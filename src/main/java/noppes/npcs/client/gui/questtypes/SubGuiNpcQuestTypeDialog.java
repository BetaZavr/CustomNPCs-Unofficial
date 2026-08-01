package noppes.npcs.client.gui.questtypes;

import java.util.*;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.DimensionManager;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.gui.IDimensionGetter;
import noppes.npcs.api.handler.data.IDialog;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.GuiNpcManageQuest;
import noppes.npcs.client.gui.global.SubGuiQuestObjectiveSelect;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.select.SubGuiDialogSelection;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.BorderController;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketDimensionsGet;
import noppes.npcs.packets.server.SPacketQuestDialogTitles;
import noppes.npcs.packets.server.SPacketTeleportTo;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.GuiSelectionListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

// Change from Unofficial (BetaZavr)
public class SubGuiNpcQuestTypeDialog
		extends GuiNPCInterface
		implements IGuiData, ITextfieldListener, IDimensionGetter, GuiSelectionListener {

	protected GuiScreen parent;
	protected final QuestObjective task;
	protected final Map<Integer, Integer> dataDimIDs = new HashMap<>();
	protected String data = "";

	public SubGuiNpcQuestTypeDialog(EntityNPCInterface npcIn, QuestObjective taskObj, GuiScreen gui) {
		super(npcIn);
		setBackground("menubg.png");
		title = Component.translatable("quest.title.dialog");
		imageWidth = 256;
		imageHeight = 129;
		closeOnEsc = true;
		parent = gui;

		task = taskObj;
		IDialog d = DialogController.instance.get(task.getTargetID());
		if (d != null) { data = d.getName(); }
		Packets.sendServer(new SPacketDimensionsGet());
		Packets.sendServer(new SPacketQuestDialogTitles(task.getTargetID()));
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		// Back
		if (!hasSubGui()) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(guiLeft, guiTop + imageHeight, 0.0f);
			GlStateManager.scale(bgScale, bgScale, bgScale);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			minecraft.getTextureManager().bindTexture(background);
			drawTexturedModalRect(0, 0, 0, 213, imageWidth, 4);
			GlStateManager.popMatrix();
		}
	}

	public void initGui() {
		super.initGui();
		int lId = 0;
		int x = guiLeft + 10;
		int y = guiTop + 15;
		// add
		addButton(1, x + 24, y, data.isEmpty() ? "dialog.selectoption" : data)
				.setSize(210, 20)
				.setHoverTexts("quest.hover.edit.dialog.add");
		// del
		addButton(2, x, y, "X")
				.setSize(20, 20)
				.setHoverTexts("quest.hover.edit.dialog.del");
		// X
		Component compass = Component.translatable("quest.hover.compass");
		addLabel(lId++, x, (y += 23) + 2, "X:")
				.setSize(12, 10);
		addTextField(10, x + 10, y, 40, 14, task.pos.getX())
				.setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.pos.getX())
				.setHoverTexts(Component.translatable("quest.hover.compass.pos", "X").append(compass));
		// Y
		addLabel(lId++, x + 62, y + 2, "Y:")
				.setSize(12, 10);
		addTextField(11, x + 72, y, 40, 14, task.pos.getY())
				.setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.pos.getY())
				.setHoverTexts(Component.translatable("quest.hover.compass.pos", "Y").append(compass));
		// Z
		addLabel(lId++, x + 120, y + 2, "Z:")
				.setSize(12, 10);
		addTextField(12, x + 130, y, 40, 14, task.pos.getZ())
				.setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.pos.getZ())
				.setHoverTexts(Component.translatable("quest.hover.compass.pos", "Z").append(compass));
		// R
		addLabel(lId++, x + 180, y + 2, "R:")
				.setSize(12, 10);
		addTextField(14, x + 190, y, 43, 14, task.rangeCompass)
				.setMinMaxDefault(0, 64, task.rangeCompass)
				.setHoverTexts(Component.translatable("quest.hover.compass.range").append(compass));
		// dim ID
		addLabel(lId++, x, (y += 18) + 2, "D:")
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
			if (id == task.dimension) { p = i; }
			i++;
		}
		addButton(4, x + 9, y - 1, false, p, dimIDs)
				.setSize(180, 16)
				.setHoverTexts(Component.translatable("quest.hover.compass.dim", dimIDs[p]).append(compass));
		// region ID
		addLabel(lId++, x, (y += 17) + 2, "P:")
				.setSize(12, 10);
		addTextField(9, x + 10, y, 32, 14, "" + task.regionID)
				.setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, task.regionID)
				.setHoverTexts(Component.translatable("quest.hover.compass.reg", task.regionID).append(compass));
		// N
		addLabel(lId, x + 44, y + 2, "N:")
				.setSize(12, 10);
		addTextField(15, x + 54, y, 101, 14, task.entityName)
				.setHoverTexts(Component.translatable("quest.hover.compass.entity").append(compass));
		addButton(9, x + 220, y, "")
				.setSize(14, 14)
				.setIsAnim(true)
				.setTexture(GuiBasic.ANIMATION_BUTTONS)
				.setUV(220, 96, 36, 36)
				.setHoverTexts("color.hover")
				.layerColor = task.colorCompass | 0xFF000000;
		// mini map point
		addCheckBox(5, x, y += 17, "quest.set.minimap.point", null, task.isSetPointOnMiniMap())
				.setSize(194, 16)
				.setHoverTexts("quest.hover.set.minimap.point");
		// tp
		addButton(11, x + 196, y, "TP")
				.setSize(20, 16)
				.setHoverTexts("hover.teleport");
		// set player pos
		addButton(10, x + 218, y, "S")
				.setSize(16, 16)
				.setHoverTexts(Component.translatable("quest.hover.compass.set").append(compass));
		// exit
		addButton(66, x, y + 17, "gui.back")
				.setSize(40, 20)
				.setHoverTexts("hover.back");
	}

	public void buttonEvent(GuiButtonNop guiButton) {
		if (task == null) { return; }
		switch (guiButton.id) {
			case 1: setSubGui(new SubGuiDialogSelection(task.getTargetID())); break;
			case 2: task.setTargetID(0); initGui(); break;
			case 4: {
				if (!dataDimIDs.containsKey(guiButton.getValue())) { return; }
				task.dimension = dataDimIDs.get(guiButton.getValue());
				guiButton.setHoverTexts(Component.translatable("quest.hover.compass.dim", "" + task.dimension).append(Component.translatable("quest.hover.compass")));
				break;
			} // dimension
			case 5: task.setPointOnMiniMap(((GuiCheckBoxNop) guiButton).selected()); break;
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
			case 11: Packets.sendServer(new SPacketTeleportTo(task.dimension, task.pos)); break;
			case 66: onClose(); break;
		}
	}

	@Override
	public void onClose() {
		super.onClose();
		if (task.getTargetID() <= 0) {
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
	public void resetDimension() { initGui(); }

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (task == null) { return; }
		switch (textField.id) {
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
	public void setGuiData(NBTTagCompound compound) {
		data = "";
		if (compound.hasKey("Title", 8)) {
			int id = compound.getInteger("DialogID");
			for (QuestObjective taskObj : NoppesUtilServer.getEditingQuest(player).questInterface.tasks) {
				if (taskObj == task || taskObj.getEnumType() != EnumQuestTask.DIALOG) { continue; }
				if (taskObj.getTargetID() == id) { return; }
			}
			task.setTargetID(id);
			data = compound.getString("Title");
		}
		initGui();
	}

	@Override
	public void selected(int id, String name) {
		for (QuestObjective taskObj : NoppesUtilServer.getEditingQuest(player).questInterface.tasks) {
			if (taskObj == task || taskObj.getEnumType() != EnumQuestTask.DIALOG) {
				continue;
			}
			if (taskObj.getTargetID() == id) { return; }
		}
		task.setTargetID(id);
		data = name;
		initGui();
	}

}