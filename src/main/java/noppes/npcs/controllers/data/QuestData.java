package noppes.npcs.controllers.data;

import java.util.ArrayList;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.client.gui.util.quests.QuestObjective;

import javax.annotation.Nonnull;

public class QuestData {

	public Quest quest;
	public long startIn = System.currentTimeMillis();
	public boolean isCompleted = false;
	public final NBTTagCompound extraData = new NBTTagCompound();

	public QuestData(@Nonnull Quest quest) { reset(quest); }

	public void reset(@Nonnull Quest questIn) {
		quest = questIn;
		int pos = 0;
		// delete the old
		NBTTagList targets = extraData.getTagList("Targets", 10);
		NBTTagList crafts = extraData.getTagList("Crafts", 10);
		NBTTagList locations = extraData.getTagList("Locations", 10);
		// delete tasks above the limit
		int size = questIn.questInterface.tasks.length;
		for (int i = targets.tagCount() - 1; i >= 0; i--) {
			if (targets.getCompoundTagAt(i).getInteger("ObjectPos") >= size) {
				targets.removeTag(i);
			}
		}
		for (int i = crafts.tagCount() - 1; i >= 0; i--) {
			if (crafts.getCompoundTagAt(i).getInteger("ObjectPos") >= size) {
				crafts.removeTag(i);
			}
		}
		for (int i = locations.tagCount() - 1; i >= 0; i--) {
			if (locations.getCompoundTagAt(i).getInteger("ObjectPos") >= size) {
				locations.removeTag(i);
			}
		}
		// replace data in old tasks
		for (QuestObjective task : questIn.questInterface.tasks) {
			if (task.getEnumType() == EnumQuestTask.KILL ||
					task.getEnumType() == EnumQuestTask.AREAKILL ||
					task.getEnumType() == EnumQuestTask.MANUAL) {
				boolean found = false;
				// found in targets
				for (int i = 0; i < targets.tagCount(); i++) {
					NBTTagCompound nbt = targets.getCompoundTagAt(i);
					if (nbt.getInteger("ObjectPos") == pos) {
						if (nbt.hasKey("Slot", 8)) {
							nbt.setString("Slot", task.getTargetName());
							found = true;
						}
						else { targets.removeTag(i); }
						break;
					}
				}
				for (int i = 0; i < crafts.tagCount(); i++) {
					if (crafts.getCompoundTagAt(i).getInteger("ObjectPos") == pos) {
						crafts.removeTag(i);
						break;
					}
				}
				for (int i = 0; i < locations.tagCount(); i++) {
					if (locations.getCompoundTagAt(i).getInteger("ObjectPos") == pos) {
						locations.removeTag(i);
						break;
					}
				}
				if (!found) {
					NBTTagCompound nbt = new NBTTagCompound();
					nbt.setString("Slot", task.getTargetName());
					nbt.setInteger("Value", 0);
					nbt.setInteger("ObjectPos", pos);
					targets.appendTag(nbt);
				}
			}
			else if (task.getEnumType() == EnumQuestTask.CRAFT) {
				for (int i = 0; i < targets.tagCount(); i++) {
					if (targets.getCompoundTagAt(i).getInteger("ObjectPos") == pos) {
						targets.removeTag(i);
						break;
					}
				}
				for (int i = 0; i < locations.tagCount(); i++) {
					NBTTagCompound nbt = locations.getCompoundTagAt(i);
					if (nbt.getInteger("ObjectPos") == pos) {
						locations.removeTag(i);
						break;
					}
				}
				if (!task.getItem().isEmpty()) {
					boolean found = false;
					for (int i = 0; i < crafts.tagCount(); i++) {
						NBTTagCompound nbt = crafts.getCompoundTagAt(i);
						if (nbt.getInteger("ObjectPos") == pos) {
							if (nbt.hasKey("Item", 10)) {
								nbt.setTag("Item", task.getItemStack().writeToNBT(new NBTTagCompound()));
								found = true;
							}
							else { crafts.removeTag(i); }
							break;
						}
					}
					if (!found) {
						NBTTagCompound nbt = new NBTTagCompound();
						nbt.setTag("Item", task.getItemStack().writeToNBT(new NBTTagCompound()));
						nbt.setInteger("Value", 0);
						nbt.setInteger("ObjectPos", pos);
						crafts.appendTag(nbt);
					}
				}
			}
			else if (task.getEnumType() == EnumQuestTask.LOCATION) {
				boolean found = false;
				for (int i = 0; i < targets.tagCount(); i++) {
					if (targets.getCompoundTagAt(i).getInteger("ObjectPos") == pos) {
						targets.removeTag(i);
						break;
					}
				}
				for (int i = 0; i < crafts.tagCount(); i++) {
					if (crafts.getCompoundTagAt(i).getInteger("ObjectPos") == pos) {
						crafts.removeTag(i);
						break;
					}
				}
				for (int i = 0; i < locations.tagCount(); i++) {
					NBTTagCompound nbt = locations.getCompoundTagAt(i);
					if (nbt.getInteger("ObjectPos") == pos) {
						if (nbt.hasKey("Location", 8)) {
							nbt.setString("Location", task.getTargetName());
							found = true;
						}
						else { locations.removeTag(i); }
						break;
					}
				}
				if (!found) {
					NBTTagCompound nbt = new NBTTagCompound();
					nbt.setString("Location", task.getTargetName());
					nbt.setBoolean("Found", false);
					nbt.setInteger("ObjectPos", pos);
					locations.appendTag(nbt);
				}
			}
			pos++;
		}
		extraData.setTag("Targets", targets);
		extraData.setTag("Crafts", crafts);
		extraData.setTag("Locations", locations);
	}

	public QuestData load(NBTTagCompound compound) {
		isCompleted = compound.getBoolean("QuestCompleted");
		startIn = compound.getLong("StartIn");
		for (String key : new ArrayList<>(extraData.getKeySet())) { extraData.removeTag(key); }
		for (String key : new ArrayList<>(compound.getCompoundTag("ExtraData").getKeySet())) {
			extraData.setTag(key, compound.getCompoundTag("ExtraData").getTag(key));
		}
		return this;
	}

	public void save(NBTTagCompound compound) {
		compound.setBoolean("QuestCompleted", isCompleted);
		compound.setLong("StartIn", startIn);
		compound.setTag("ExtraData", extraData);
	}

}
