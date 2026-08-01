package noppes.npcs.client.gui.roles;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcJobSave;
import noppes.npcs.roles.JobFollower;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class GuiNpcFollowerJob extends GuiNPCInterface2
		implements ICustomScrollListener, ITextfieldListener {

	protected final JobFollower job;
	protected GuiCustomScrollNop scroll;

	protected final List<String> names = new ArrayList<>();

	public GuiNpcFollowerJob(EntityNPCInterface npc) {
		super(npc);

		backGui = EnumGuiType.MainMenuAdvanced;
		job = (JobFollower) npc.job;
		for (Entity entity : player.world.loadedEntityList) {
			if (entity instanceof EntityNPCInterface) {
				EntityNPCInterface cnpc = (EntityNPCInterface) entity;
				if (npc != cnpc && !names.contains(cnpc.display.getName())) { names.add(cnpc.display.getName()); }
			}
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		addLabel(0, guiLeft + 6, guiTop + 110, Component.translatable("gui.name").append(":"))
				.setSize(48, 10);
		addTextField(0, guiLeft + 50, guiTop + 105, 200, 20, job.name);
		int x = guiLeft + 268;
		addLabel(1, x + 1, guiTop + 5, Component.translatable("spawner.all").append(" NPC:"))
				.setSize(141, 10);
		if (scroll == null) { scroll = addScroll(0).setSize(143, 198); }
		add(scroll.setList(names).setPos(x, guiTop + 15));
	}

	@Override
	public void save() { Packets.sendServer(new SPacketNpcJobSave(job.save(new NBTTagCompound()))); }

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		job.name = scroll.getSelected();
		getTextField(0).setValue(scroll.getSelected());
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

	@Override
	public void unFocused(GuiTextFieldNop textField) { job.name = textField.getValue(); }

}
