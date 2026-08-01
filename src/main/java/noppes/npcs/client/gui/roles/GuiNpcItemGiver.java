package noppes.npcs.client.gui.roles;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.availability.SubGuiNpcAvailability;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerNpcItemGiver;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcJobSave;
import noppes.npcs.roles.JobItemGiver;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;

public class GuiNpcItemGiver extends GuiContainerNPCInterface2<ContainerNpcItemGiver> {

	protected final JobItemGiver role;

	public GuiNpcItemGiver(EntityNPCInterface npc, ContainerNpcItemGiver container) {
		super(npc, container, Component.empty());
		setBackground("npcitemgiver.png");
		ySize = 200;

		backGui = EnumGuiType.MainMenuAdvanced;
		role = (JobItemGiver) npc.job;
	}

	@Override
	public void initGui() {
		super.initGui();
		int x = guiLeft + 6;
		addButton(0, x, guiTop + 6, false, role.givingMethod,
				"role.give.rnd", "role.give.all", "role.give.not.owned", "role.give.doesnt.own", "role.give.chained")
				.setSize(140, 20);
		addButton(1, x, guiTop + 30, false, role.cooldownType,
				"role.cooldown.timer", "role.cooldown.one", "quest.rldaily")
				.setSize(140, 20);
		addLabel(0, x + 1, guiTop + 59, Component.translatable("spawner.cooldown").append(":"))
				.setSize(55, 10)
				.setIsVisible(role.isOnTimer());
		addLabel(2, x + 116, guiTop + 59, "gui.sec")
				.setSize(33, 10)
				.setIsVisible(role.isOnTimer());
		addTextField(0, x + 58, guiTop + 55, 55, 18, role.cooldown)
				.setMinMaxDefault(0, Integer.MAX_VALUE, role.cooldown)
				.setIsVisible(role.isOnTimer());
		addLabel(1, x + 1, guiTop + 79, Component.translatable("role.items.give").append(":"))
				.setSize(162, 10);
		x += 142;
		for(int i = 0; i < 3; ++i) {
			addTextField(i + 1, x + 1, guiTop + 7 + i * 24, 236, 18, i < role.lines.size() ? role.lines.get(i) : "");
		}
		addLabel(4, x += 24, guiTop + 92, Component.translatable("availability.options").append(":"))
				.setSize(118, 10);
		addButton(4, x + 120, guiTop + 87, "selectServer.edit")
				.setSize(60, 20);
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: role.givingMethod = button.getValue(); break;
			case 1: {
				role.cooldownType = button.getValue();
				getTextField(0).setIsVisible(role.isOnTimer());
				getLabel(0).setIsVisible(role.isOnTimer());
				getLabel(2).setIsVisible(role.isOnTimer());
				break;
			}
			case 4: setSubGui(new SubGuiNpcAvailability(role.availability, this)); break;
		}
	}

	@Override
	public void save() {
		List<String> lines = new ArrayList<>();
		int cc;
		for(cc = 1; cc < 4; ++cc) {
			GuiTextFieldNop tf = getTextField(cc);
			if (!tf.isEmpty()) { lines.add(tf.getValue()); }
		}
		role.lines = lines;
		cc = 10;
		if (!getTextField(0).isEmpty() && getTextField(0).isInteger()) { cc = getTextField(0).getInteger(); }
		role.cooldown = cc;
		Packets.sendServer(new SPacketNpcJobSave(role.save(new NBTTagCompound())));
	}

}
