package noppes.npcs.client.gui.util.quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.containers.NpcMiscInventory;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;

// Global Changed
public class QuestInterface {

	private int id = 0;
	public int color = (int) (Math.random() * 16777215.0) | 0xFF000000;
	public NpcMiscInventory items = new NpcMiscInventory(1);
	public QuestObjective[] tasks = new QuestObjective[0];

	public QuestObjective addTask(EnumQuestTask type) {
		if (tasks.length >= 9) { return null; }
		QuestObjective[] ts = new QuestObjective[tasks.length + 1];
        System.arraycopy(tasks, 0, ts, 0, tasks.length);
		int objectPos = tasks.length;
		ts[objectPos] = createObjective(id, objectPos, type);
		tasks = ts;
		fix();
		return tasks[tasks.length - 1];
	}

	private QuestObjective createObjective(int id, int objectPos, EnumQuestTask type) {
		QuestObjective objective = new QuestObjective(id, objectPos, type);
		objective.colorCompass = color;
		return objective;
	}

	public void downPos(QuestObjective task) {
		QuestObjective[] ts = new QuestObjective[tasks.length];
		try {
			for (int i = 0, j = 0; i < tasks.length; i++) {
				if (tasks[i] != task) {
					ts[j] = tasks[i];
					ts[j].setObjectPos(j);
					j++;
					if ((i - 1) >= 0 && tasks[i - 1] == task) {
						ts[j] = tasks[i - 1];
						j++;
					}
				}
			}
			tasks = ts;
			fix();
		}
		catch (Exception e) { LogWriter.error(e); }
	}

	public void fix() {
		List<QuestObjective> tsl = new ArrayList<>();
		Map<Integer, ItemStack> stacks = new TreeMap<>();
		for (int i = 0; i < tasks.length; i++) {
			if (tasks[i] != null) {
				QuestObjective to = tasks[i];
				if (to.getMaxProgress() <= 0) { to.setMaxProgress(1); }
				stacks.put(i, ItemStack.EMPTY);
				if ((to.getEnumType() == EnumQuestTask.ITEM || to.getEnumType() == EnumQuestTask.CRAFT)) { stacks.put(i, to.getItemStack()); }
				else if (to.getEnumType() == EnumQuestTask.AREAKILL) {
					if (to.getAreaRange() < 3) { to.setAreaRange(3); }
					else if (to.getAreaRange() > 32) { to.setAreaRange(24); }
				}
				tsl.add(to);
			}
		}
		QuestObjective[] ts = new QuestObjective[tsl.size()];
		for (int i = 0; i < tsl.size(); i++) {
			ts[i] = tsl.get(i);
			ts[i].setObjectPos(i);
		}
		tasks = ts;
		items = new NpcMiscInventory(stacks.size());
		for (int i = 0; i < stacks.size(); i++) { items.setInventorySlotContents(i, stacks.get(i)); }
	}

	public boolean getFound(QuestData data, QuestObjective object) {
		for (NBTBase dataNBT : data.extraData.getTagList("Locations", 10)) {
			if (object.getTargetName().equalsIgnoreCase(((NBTTagCompound) dataNBT).getString("Location"))
					&& ((NBTTagCompound) dataNBT).getBoolean("Found")
					&& ((NBTTagCompound) dataNBT).getInteger("ObjectPos") == object.getObjectPos()) {
				return true;
			}
		}
		return false;
	}

	public int getId() { return id; }

	public Map<Component, QuestObjective> getKeys() {
		Map<Component, QuestObjective> keys = new HashMap<>();
		for (int i = 0; i < tasks.length; i++) {
			QuestObjective to = tasks[i];
			Component key = Component.literal((i + 1) + "-");
			Component name;
			switch (to.getEnumType()) {
				case DIALOG: {
					name = Component.translatable("quest.has.false");
					Dialog d = DialogController.instance.dialogs.get(to.getTargetID());
					if (d != null) {
						name = Component.empty();
						name.append(Component.literal(Util.instance.deleteColor(Component.translatable(d.category.getName()).getFormattedText()) + "/").withStyle(TextFormatting.DARK_GRAY))
								.append(Component.translatable(d.getName()).withStyle(TextFormatting.RESET));
					}
					keys.put(key.append(Component.literal("["))
							.append(Component.literal("Dr").withStyle(TextFormatting.AQUA))
							.append(Component.literal("]").withStyle(TextFormatting.RESET))
							.append(Component.literal(" "))
							.append(name), to);
					break;
				}
				case KILL:
				case AREAKILL: {
					name = Component.translatable("entity." + to.getTargetName() + ".name");
					String lName = name.getString();
					if (to.getTargetName().isEmpty()) { name = Component.translatable("quest.has.false"); }
					else if (lName.startsWith("entity.") && lName.endsWith(".name")) { name = Component.literal(to.getTargetName()); }
					key.append(Component.literal("["));
					if (to.getEnumType() == EnumQuestTask.KILL) { key.append(Component.literal("K").withStyle(TextFormatting.RED)); }
					else { key.append(Component.literal("AK").withStyle(TextFormatting.DARK_RED)); }
					key.append(Component.literal("]").withStyle(TextFormatting.RESET))
							.append(Component.literal(" "))
							.append(name)
							.append(Component.literal(" = ").withStyle(TextFormatting.RESET))
							.append(Component.literal("" + to.getMaxProgress()));
					keys.put(key, to);
					break;
				}
				case LOCATION: {
					name = Component.literal(to.getTargetName());
					if (to.getTargetName().isEmpty()) { name = Component.translatable("quest.has.false"); }
					keys.put(key.append(Component.literal("["))
							.append(Component.literal("L").withStyle(TextFormatting.DARK_GREEN))
							.append(Component.literal("]").withStyle(TextFormatting.RESET))
							.append(Component.literal(" "))
							.append(name), to);
					break;
				}
				case MANUAL: {
					name = Component.literal(to.getTargetName());
					if (to.getTargetName().isEmpty()) { name = Component.translatable("quest.has.false"); }
					keys.put(key.append(Component.literal("["))
							.append(Component.literal("M").withStyle(TextFormatting.LIGHT_PURPLE))
							.append(Component.literal("]").withStyle(TextFormatting.RESET))
							.append(Component.literal(" "))
							.append(name)
							.append(Component.literal(" = ").withStyle(TextFormatting.RESET))
							.append(Component.literal("" + to.getMaxProgress())), to);
					break;
				}
				default: { // CRAFT; ITEM
					boolean isCr = to.getEnumType() == EnumQuestTask.CRAFT;
					name = Component.literal(to.getItemStack().getDisplayName());
					if (to.getItemStack().isEmpty()) { name = Component.translatable("quest.has.false"); }
					keys.put(key.append(Component.literal("["))
							.append(Component.literal(isCr ? "Ic" : "If").withStyle(isCr ? TextFormatting.YELLOW : TextFormatting.GOLD))
							.append(Component.literal("]").withStyle(TextFormatting.RESET))
							.append(Component.literal(" "))
							.append(name)
							.append(Component.literal(" = ").withStyle(TextFormatting.RESET))
							.append(Component.literal("" + to.getMaxProgress())), to);
				}
			}
		}
		return keys;
	}

	public QuestObjective[] getObjectives(EntityPlayer player) {
		QuestObjective[] array = new QuestObjective[tasks.length];
		for (int i = 0; i < tasks.length; i++) { array[i] = tasks[i].copyToPlayer(player); }
		return array;
	}

	public int getPos(QuestObjective task) {
		for (int i = 0; i < tasks.length; i++) {
			if (tasks[i] == task) { return i; }
		}
		return -1;
	}

	public void handleComplete(@Nonnull EntityPlayer player) {
		boolean bo = false;
		for (QuestObjective to : tasks) {
			if (to.getEnumType() != EnumQuestTask.ITEM || to.getItemStack().isEmpty() || !to.isItemLeave()) { continue; }
			int stacksize = to.getMaxProgress();
			for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
				ItemStack stack = player.inventory.getStackInSlot(i);
				if (!NoppesUtilServer.isItemStackNull(stack) &&
						NoppesUtilPlayer.compareItems(stack, to.getItemStack(), to.isIgnoreDamage(), to.isItemIgnoreNBT())) {
					bo = true;
					int size = stack.getCount();
					if (stacksize - size >= 0) {
						player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
						stack.splitStack(size);
					}
					else { stack.splitStack(stacksize); }
					stacksize -= size;
					if (stacksize <= 0) { break; }
				}
			}
		}
		if (bo) { player.inventoryContainer.detectAndSendChanges(); }
	}

	public boolean isCompleted(@Nonnull EntityPlayer player) {
		PlayerData playerdata = PlayerData.get(player);
		QuestData data = playerdata.questData.activeQuests.get(id);
		if (data == null) { return false; }
		boolean complete = true;
		for (QuestObjective to : tasks) {
			switch (to.getEnumType()) {
				case DIALOG: {
					complete = playerdata.dialogData.has(to.getTargetID());
					break;
				}
				case LOCATION: {
					complete = getFound(data, to);
					break;
				}
				case AREAKILL:
				case MANUAL:
				case KILL: {
					HashMap<String, Integer> killed = to.getKilled(data);
					if (killed.isEmpty()) { complete = false; }
					for (String entity : killed.keySet()) {
						if (entity.equalsIgnoreCase(to.getTargetName())) {
							if (killed.get(entity) < to.getMaxProgress()) { complete = false; }
							break;
						}
					}
					break;
				}
				case CRAFT: {
					HashMap<ItemStack, Integer> crafted = to.getCrafted(data);
					for (ItemStack item : crafted.keySet()) {
						if (NoppesUtilPlayer.compareItems(to.getItemStack(), item, to.isIgnoreDamage(), to.isItemIgnoreNBT())) {
							if (crafted.get(item) < to.getMaxProgress()) { complete = false; }
							break;
						}
					}
					break;
				}
				default: { // ITEM
					complete = NoppesUtilPlayer.compareItems(player, to.getItemStack(), to.isIgnoreDamage(), to.isItemIgnoreNBT(), to.getMaxProgress());
				}
			}
			if (data.quest != null) {
				if (!complete && data.quest.step != 2) { return false; }
				if (complete && data.quest.step == 2) { return true; }
			}
		}
		return complete;
	}

	public void load(NBTTagCompound compound, int idIn) {
		id = idIn;
		if (compound.hasKey("QuestTasksColor", 3)) { color = compound.getInteger("QuestTasksColor"); }
		if (!compound.hasKey("Tasks", 9)) {
			List<QuestObjective> oldTasks = new ArrayList<>();
			int i = 0;
			if (compound.getInteger("Type") == 0) {
				items = new NpcMiscInventory(compound.getCompoundTag("Items").getTagList("NpcMiscInv", 10).tagCount());
				items.load(compound.getCompoundTag("Items"));
				for (int j = 0; j < items.getSizeInventory(); j++) {
					QuestObjective to = createObjective(id, i++, EnumQuestTask.ITEM);
					to.setItem(items.getStackInSlot(j));
					to.setItemLeave(compound.getBoolean("LeaveItems"));
					to.setItemIgnoreDamage(compound.getBoolean("IgnoreDamage"));
					to.setItemIgnoreNBT(compound.getBoolean("IgnoreNBT"));
					oldTasks.add(to);
				}
			} // Item
			else if (compound.getInteger("Type") == 1) {
				HashMap<Integer, Integer> dialogs = NBTTags.getIntegerIntegerMap(compound.getTagList("QuestDialogs", 10));
				for (int dId : dialogs.values()) {
					QuestObjective to = createObjective(id, i++, EnumQuestTask.DIALOG);
					to.setTargetID(dId); // DialogID
					oldTasks.add(to);
				}
			} // Dialogs
			else if (compound.getInteger("Type") == 2 || compound.getInteger("Type") == 4) {
				TreeMap<String, Integer> targets = new TreeMap<>(NBTTags.getStringIntegerMap(compound.getTagList("QuestDialogs", 10)));
				for (String name : targets.keySet()) {
					QuestObjective to = createObjective(id, i++, EnumQuestTask.values()[compound.getInteger("Type")]);
					to.setTargetName(name);
					to.setMaxProgress(targets.get(name));
					oldTasks.add(to);
				}
			} // Kill or Area Kill
			else if (compound.getInteger("Type") == 3) {
				if (compound.hasKey("QuestLocation", 8)) {
					QuestObjective t0 = createObjective(id, i++, EnumQuestTask.LOCATION);
					t0.setTargetName(compound.getString("QuestLocation"));
					oldTasks.add(t0);
				}
				if (compound.hasKey("QuestLocation2", 8)) {
					QuestObjective t1 = createObjective(id, i++, EnumQuestTask.LOCATION);
					t1.setTargetName(compound.getString("QuestLocation2"));
					oldTasks.add(t1);
				}
				if (compound.hasKey("QuestLocation3", 8)) {
					QuestObjective t2 = createObjective(id, i, EnumQuestTask.LOCATION);
					t2.setTargetName(compound.getString("QuestLocation3"));
					oldTasks.add(t2);
				}

			} // Location
			else {
				TreeMap<String, Integer> manuals = new TreeMap<>(NBTTags.getStringIntegerMap(compound.getTagList("QuestManual", 10)));
				for (String name : manuals.keySet()) {
					QuestObjective to = createObjective(id, i++, EnumQuestTask.MANUAL);
					to.setTargetName(name);
					to.setMaxProgress(manuals.get(name));
					oldTasks.add(to);
				}
			} // Manual
			tasks = oldTasks.toArray(new QuestObjective[Math.min(oldTasks.size(), 9)]);
		} // Old versions
		else {
			tasks = new QuestObjective[compound.getTagList("Tasks", 10).tagCount()];
			Map<Integer, ItemStack> stacks = new TreeMap<>();
			for (int i = 0; i < compound.getTagList("Tasks", 10).tagCount(); i++) {
				QuestObjective to = createObjective(id, i, EnumQuestTask.ITEM);
				to.load(compound.getTagList("Tasks", 10).getCompoundTagAt(i));
				if ((to.getEnumType() == EnumQuestTask.ITEM || to.getEnumType() == EnumQuestTask.CRAFT) && !to.getItemStack().isEmpty()) { stacks.put(i, to.getItemStack()); }
				else { stacks.put(i, ItemStack.EMPTY); }
				tasks[i] = to;
			}
			items = new NpcMiscInventory(stacks.size());
			for (int i = 0; i < stacks.size(); i++) { items.setInventorySlotContents(i, stacks.get(i)); }
		}
		fix();
	}

	public boolean removeTask(QuestObjective task) {
		if (task != null) {
			QuestObjective[] ts = new QuestObjective[tasks.length - 1];
			boolean hasRemoved = false;
			for (int i = 0, j = 0; i < tasks.length; i++) {
				if (tasks[i] == task) {
					hasRemoved = true;
					continue;
				}
				ts[j] = tasks[i];
				j++;
			}
			if (hasRemoved) {
				tasks = ts;
				fix();
			}
			return hasRemoved;
		}
		return false;
	}

	public boolean setFound(QuestData data, String location) {
		if (data != null && data.quest.id == id) {
			for (QuestObjective to : tasks) {
				if (to.getEnumType() != EnumQuestTask.LOCATION || !location.equalsIgnoreCase(to.getTargetName())) { continue; }
				NBTTagCompound dataNBT = new NBTTagCompound();
				dataNBT.setString("Location", to.getTargetName());
				dataNBT.setBoolean("Found", true);
				dataNBT.setInteger("ObjectPos", to.getObjectPos());
				if (data.extraData.getTagList("Locations", 10).tagCount() == 0) {
					NBTTagList list = new NBTTagList();
					list.appendTag(dataNBT);
					data.extraData.setTag("Locations", list);
					return true;
				}
				boolean found = false;
				for (int i = 0; i < data.extraData.getTagList("Locations", 10).tagCount(); i++) {
					NBTTagCompound dataLoc = data.extraData.getTagList("Locations", 10).getCompoundTagAt(i);
					if (location.equalsIgnoreCase(dataLoc.getString("Location"))) {
						if (!dataLoc.getBoolean("Found")) {
							data.extraData.getTagList("Locations", 10).getCompoundTagAt(i).setBoolean("Found", true);
						}
						else { return false; }
						found = true;
						break;
					}
				}
				if (!found) {
					data.extraData.getTagList("Locations", 10).appendTag(dataNBT);
					return true;
				}
			}
		}
		return false;
	}

	public void upPos(QuestObjective task) {
		QuestObjective[] ts = new QuestObjective[tasks.length];
		try {
			for (int i = 0, j = 0; i < tasks.length; i++) {
				if (tasks[i] != task) {
					if ((i + 1) < tasks.length && tasks[i + 1] == task) {
						ts[j] = tasks[i + 1];
						ts[j].setObjectPos(j);
						j++;
					}
					ts[j] = tasks[i];
					j++;
				}
			}
			tasks = ts;
			fix();
		}
		catch (Exception e) { LogWriter.error("CNPCs Error ", e); }
	}

	public void save(NBTTagCompound compound) {
		fix();
		compound.setInteger("QuestTasksColor", color);
		NBTTagList list = new NBTTagList();
        for (QuestObjective task : tasks) { list.appendTag(task.getNBT()); }
		compound.setTag("Tasks", list);
	}

}
