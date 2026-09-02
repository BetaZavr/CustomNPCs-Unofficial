package noppes.npcs.controllers.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.client.gui.util.quests.QuestObjective;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class QuestData {

   public Quest quest;
   public long startIn = System.currentTimeMillis();
   public boolean isCompleted;
   public final CompoundTag extraData = new CompoundTag();

   public QuestData(@Nonnull Quest quest) { reset(quest); }

   public void reset(@Nonnull Quest questIn) {
      quest = questIn;
      int pos = 0;
      // delete the old
      ListTag targets = extraData.getList("Targets", 10);
      ListTag crafts = extraData.getList("Crafts", 10);
      ListTag locations = extraData.getList("Locations", 10);
      // delete tasks above the limit
      int size = questIn.questInterface.tasks.length;
      for (int i = targets.size() - 1; i >= 0; i--) {
         if (targets.getCompound(i).getInt("ObjectPos") >= size) {
            targets.remove(i);
         }
      }
      for (int i = crafts.size() - 1; i >= 0; i--) {
         if (crafts.getCompound(i).getInt("ObjectPos") >= size) {
            crafts.remove(i);
         }
      }
      for (int i = locations.size() - 1; i >= 0; i--) {
         if (locations.getCompound(i).getInt("ObjectPos") >= size) {
            locations.remove(i);
         }
      }
      // replace data in old tasks
      for (QuestObjective task : questIn.questInterface.tasks) {
         if (task.getEnumType() == EnumQuestTask.KILL ||
                 task.getEnumType() == EnumQuestTask.AREAKILL ||
                 task.getEnumType() == EnumQuestTask.MANUAL) {
            boolean found = false;
            // found in targets
            for (int i = 0; i < targets.size(); i++) {
               CompoundTag nbt = targets.getCompound(i);
               if (nbt.getInt("ObjectPos") == pos) {
                  if (nbt.contains("Slot", 8)) {
                     nbt.putString("Slot", task.getTargetName());
                     found = true;
                  }
                  else { targets.remove(i); }
                  break;
               }
            }
            for (int i = 0; i < crafts.size(); i++) {
               if (crafts.getCompound(i).getInt("ObjectPos") == pos) {
                  crafts.remove(i);
                  break;
               }
            }
            for (int i = 0; i < locations.size(); i++) {
               if (locations.getCompound(i).getInt("ObjectPos") == pos) {
                  locations.remove(i);
                  break;
               }
            }
            if (!found) {
               CompoundTag nbt = new CompoundTag();
               nbt.putString("Slot", task.getTargetName());
               nbt.putInt("Value", 0);
               nbt.putInt("ObjectPos", pos);
               targets.add(nbt);
            }
         }
         else if (task.getEnumType() == EnumQuestTask.CRAFT) {
            for (int i = 0; i < targets.size(); i++) {
               if (targets.getCompound(i).getInt("ObjectPos") == pos) {
                  targets.remove(i);
                  break;
               }
            }
            for (int i = 0; i < locations.size(); i++) {
               CompoundTag nbt = locations.getCompound(i);
               if (nbt.getInt("ObjectPos") == pos) {
                  locations.remove(i);
                  break;
               }
            }
            if (!task.getItem().isEmpty()) {
               boolean found = false;
               for (int i = 0; i < crafts.size(); i++) {
                  CompoundTag nbt = crafts.getCompound(i);
                  if (nbt.getInt("ObjectPos") == pos) {
                     if (nbt.contains("Slot", 8)) {
                        nbt.put("Item", task.getItemStack().save(new CompoundTag()));
                        found = true;
                     }
                     else { crafts.remove(i); }
                     break;
                  }
               }
               if (!found) {
                  CompoundTag nbt = new CompoundTag();
                  nbt.put("Item", task.getItemStack().save(new CompoundTag()));
                  nbt.putInt("Value", 0);
                  nbt.putInt("ObjectPos", pos);
                  crafts.add(nbt);
               }
            }
         }
         else if (task.getEnumType() == EnumQuestTask.LOCATION) {
            boolean found = false;
            for (int i = 0; i < targets.size(); i++) {
               if (targets.getCompound(i).getInt("ObjectPos") == pos) {
                  targets.remove(i);
                  break;
               }
            }
            for (int i = 0; i < crafts.size(); i++) {
               if (crafts.getCompound(i).getInt("ObjectPos") == pos) {
                  crafts.remove(i);
                  break;
               }
            }
            for (int i = 0; i < locations.size(); i++) {
               CompoundTag nbt = locations.getCompound(i);
               if (nbt.getInt("ObjectPos") == pos) {
                  if (nbt.contains("Location", 8)) {
                     nbt.putString("Location", task.getTargetName());
                     found = true;
                  }
                  else { locations.remove(i); }
                  break;
               }
            }
            if (!found) {
               CompoundTag nbt = new CompoundTag();
               nbt.putString("Location", task.getTargetName());
               nbt.putBoolean("Found", false);
               nbt.putInt("ObjectPos", pos);
               locations.add(nbt);
            }
         }
         pos++;
      }
      extraData.put("Targets", targets);
      extraData.put("Crafts", crafts);
      extraData.put("Locations", locations);
   }

   public void save(CompoundTag compound) {
      compound.putBoolean("QuestCompleted", isCompleted);
      compound.putLong("StartIn", startIn);
      compound.put("ExtraData", extraData);
   }

   public QuestData load(CompoundTag compound) {
      isCompleted = compound.getBoolean("QuestCompleted");
      startIn = compound.getLong("StartIn");
      for (String key : new ArrayList<>(extraData.getAllKeys())) { extraData.remove(key); }
      for (String key : new ArrayList<>(compound.getCompound("ExtraData").getAllKeys())) {
         Tag tag = compound.getCompound("ExtraData").get(key);
         if (tag != null) { extraData.put(key, tag); }
      }
      return this;
   }

}
