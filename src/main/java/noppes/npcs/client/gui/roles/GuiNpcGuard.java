package noppes.npcs.client.gui.roles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcJobSave;
import noppes.npcs.roles.JobGuard;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

public class GuiNpcGuard extends GuiNPCInterface2 implements ICustomScrollListener {

	protected final JobGuard role;
	protected final HashMap<String, EntityEntry> entityData = new HashMap<>(); // descriptionId, resource
	protected final Map<Component, String> namesData = new HashMap<>();
	protected GuiCustomScrollNop scrollAllEntities;
	protected GuiCustomScrollNop scrollSelected;

	protected Component select = Component.empty();

	public GuiNpcGuard(EntityNPCInterface npc) {
		super(npc);
		backGui = EnumGuiType.MainMenuAdvanced;
		role = (JobGuard)npc.job;

		for (EntityEntry ent : ForgeRegistries.ENTITIES.getValuesCollection()) {
			try {
				Entity entity = ent.newInstance(player.world);
				if (entity != null) {
					if (EntityLivingBase.class.isAssignableFrom(entity.getClass())) {
						entityData.put("entity." + ent.getName() + ".name", ent);
						namesData.put(Component.translatable("entity." + ent.getName() + ".name"), "entity." + ent.getName() + ".name");
					}
					entity.isDead = true;
				}
			} catch (Exception ignored) {}
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		int x = guiLeft + 5;
		int y = guiTop + 5;
		List<Component> selected = new ArrayList<>();
		for (String descriptionId : role.targets) {
			EntityEntry type = entityData.get(descriptionId);
			boolean needAdd = true;
			if (type != null) {
				Entity entity = type.newInstance(player.world);
				if (entity != null) {
					selected.add(Component.translatable("entity." + type.getName() + ".name"));
					needAdd = false;
				}
			}
			if (needAdd) { selected.add(Component.literal(descriptionId)); }
		}
		List<Component> allNames = new ArrayList<>();
		for (Component key : namesData.keySet()) {
			boolean needAdd = true;
			for (Component name : selected) {
				if (name.getString().equals(key.getString())) {
					needAdd = false;
					break;
				}
			}
			if (needAdd) { allNames.add(key); }
		}
		addButton(0, x, y, "guard.animals")
				.setSize(100, 20);
		addButton(1, x + 130, y, "guard.mobs")
				.setSize(100, 20);
		addButton(2, x + 260, y, "guard.creepers")
				.setSize(100, 20);
		y += 34;
		if (scrollAllEntities == null) { scrollAllEntities = addScroll(0).setSize(175, 174); }
		scrollAllEntities.setNormalList(allNames);
		if (!select.getFormattedText().isEmpty()) { scrollAllEntities.setSelected(select); }
		add(scrollAllEntities.setPos(x, y));
		addLabel(11, x + 1, y - 10, "guard.availableTargets");
		if (scrollSelected == null) { scrollSelected = addScroll(1).setSize(175, 174); }
		x = guiLeft + 183;
		scrollSelected.setNormalList(selected);
		if (!select.getFormattedText().isEmpty()) { scrollSelected.setSelected(select); }
		add(scrollSelected.setPos(x + 58, y));
		addLabel(12, x + 59, y - 10, "guard.currentTargets");
		addButton(11, x, y += 22, ">")
				.setIsEnabled(scrollAllEntities.hasSelected())
				.setSize(55, 20);
		addButton(12, x, y += 22, "<")
				.setIsEnabled(scrollSelected.hasSelected())
				.setSize(55, 20);
		addButton(13, x, y += 22, ">>")
				.setIsEnabled(!allNames.isEmpty())
				.setSize(55, 20);
		addButton(14, x, y + 22, "<<")
				.setIsEnabled(!selected.isEmpty())
				.setSize(55, 20);
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				for (EntityEntry ent : ForgeRegistries.ENTITIES.getValuesCollection()) {
					Class<? extends Entity> cl = ent.getEntityClass();
					String name = "entity." + ent.getName() + ".name";
					if (EntityAnimal.class.isAssignableFrom(cl) && !role.targets.contains(name)) { role.targets.add(name); }
				}
				scrollAllEntities.clearSelection();
				scrollSelected.clearSelection();
				initGui();
				break;
			} // all animals
			case 1: {
				for (EntityEntry ent : ForgeRegistries.ENTITIES.getValuesCollection()) {
					Class<? extends Entity> cl = ent.getEntityClass();
					String name = "entity." + ent.getName() + ".name";
					if (EntityMob.class.isAssignableFrom(cl) && !EntityCreeper.class.isAssignableFrom(cl)  && !role.targets.contains(name)) { role.targets.add(name); }
				}
				scrollAllEntities.clearSelection();
				scrollSelected.clearSelection();
				initGui();
				break;
			} // all mobs
			case 2: {
				for (EntityEntry ent : ForgeRegistries.ENTITIES.getValuesCollection()) {
					Class<? extends Entity> cl = ent.getEntityClass();
					String name = "entity." + ent.getName() + ".name";
					if (EntityCreeper.class.isAssignableFrom(cl) && !role.targets.contains(name)) {
						role.targets.add(name);
					}
				}
				scrollAllEntities.clearSelection();
				scrollSelected.clearSelection();
				initGui();
				break;
			} // all creepers
			case 11: {
				if (namesData.containsKey(scrollAllEntities.getNormalSelected())) {
					role.targets.add(namesData.get(scrollAllEntities.getNormalSelected()));
					scrollAllEntities.clearSelection();
					scrollSelected.clearSelection();
					initGui();
				}
				break;
			} // >
			case 12: {
				if (namesData.containsKey(scrollSelected.getNormalSelected())) {
					role.targets.remove(namesData.get(scrollSelected.getNormalSelected()));
					scrollSelected.clearSelection();
					initGui();
				}
				break;
			} // <
			case 13: {
				role.targets.clear();
				for (EntityEntry ent : ForgeRegistries.ENTITIES.getValuesCollection()) {
					Class<? extends Entity> cl = ent.getEntityClass();
					String name = "entity." + ent.getName() + ".name";
					if (EntityLivingBase.class.isAssignableFrom(cl) && !EntityNPCInterface.class.isAssignableFrom(cl)) {
						role.targets.add(name);
					}
				}
				scrollAllEntities.clearSelection();
				scrollSelected.clearSelection();
				initGui();
				break;
			} // >>
			case 14: {
				role.targets.clear();
				scrollAllEntities.clearSelection();
				scrollSelected.clearSelection();
				initGui();
				break;
			} // <<
		}
	}


	@Override
	public void save() { Packets.sendServer(new SPacketNpcJobSave(role.save(new NBTTagCompound()))); }

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		select = scroll.getNormalSelected();
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

}
