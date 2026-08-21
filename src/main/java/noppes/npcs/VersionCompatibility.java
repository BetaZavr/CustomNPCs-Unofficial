package noppes.npcs;

import java.util.Collection;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import noppes.npcs.api.ICompatibilty;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.Lines;
import noppes.npcs.entity.EntityNPCInterface;

public class VersionCompatibility {

   public static int ModRev = 18;

   public static void CheckNpcCompatibility(EntityNPCInterface npc, CompoundTag compound) {
      if (npc.npcVersion != ModRev) {
         if (npc.npcVersion < 12) {
            CompatabilityFix(compound, npc.advanced.save(new CompoundTag()));
            CompatabilityFix(compound, npc.ais.save(new CompoundTag()));
            CompatabilityFix(compound, npc.stats.save(new CompoundTag()));
            CompatabilityFix(compound, npc.display.save(new CompoundTag()));
            CompatabilityFix(compound, npc.inventory.save(new CompoundTag()));
         }
         if (npc.npcVersion < 5) {
            String texture = compound.getString("Texture");
            texture = texture.replace("/mob/" + CustomNpcs.MODID + "/", CustomNpcs.MODID + ":textures/entity/");
            texture = texture.replace("/mob/", CustomNpcs.MODID + ":textures/entity/");
            compound.putString("Texture", texture);
         }
         int i;
         if (npc.npcVersion < 6 && compound.get("NpcInteractLines") instanceof ListTag) {
            List<String> interactLines = NBTTags.getStringList(compound.getList("NpcInteractLines", 10));
            Lines lines = new Lines();
            for(i = 0; i < interactLines.size(); ++i) {
               Line line = new Line();
               line.setText((String)interactLines.toArray()[i]);
               lines.lines.put(i, line);
            }
            compound.put("NpcInteractLines", lines.save());
            List<String> worldLines = NBTTags.getStringList(compound.getList("NpcLines", 10));
            lines = new Lines();

            for(i = 0; i < worldLines.size(); ++i) {
               Line line = new Line();
               line.setText((String)worldLines.toArray()[i]);
               lines.lines.put(i, line);
            }
            compound.put("NpcLines", lines.save());
            List<String> attackLines = NBTTags.getStringList(compound.getList("NpcAttackLines", 10));
            lines = new Lines();

            for(i = 0; i < attackLines.size(); ++i) {
               Line line = new Line();
               line.setText((String)attackLines.toArray()[i]);
               lines.lines.put(i, line);
            }
            compound.put("NpcAttackLines", lines.save());
            List<String> killedLines = NBTTags.getStringList(compound.getList("NpcKilledLines", 10));
            lines = new Lines();

            for(i = 0; i < killedLines.size(); ++i) {
               Line line = new Line();
               line.setText((String)killedLines.toArray()[i]);
               lines.lines.put(i, line);
            }
            compound.put("NpcKilledLines", lines.save());
         }

         ListTag list;
         if (npc.npcVersion == 12) {
            list = compound.getList("StartPos", 3);
            if (list.size() == 3) {
               int x = ((IntTag)list.remove(1)).getAsInt();
               int y = ((IntTag)list.remove(0)).getAsInt();
               int z = ((IntTag)list.remove(2)).getAsInt();
               compound.putIntArray("StartPosNew", new int[] {x, y, z});
            }
         }

         if (npc.npcVersion == 13) {
            boolean bo = compound.getBoolean("HealthRegen");
            compound.putInt("HealthRegen", bo ? 1 : 0);
            CompoundTag comp = compound.getCompound("TransformStats");
            bo = comp.getBoolean("HealthRegen");
            comp.putInt("HealthRegen", bo ? 1 : 0);
            compound.put("TransformStats", comp);
         }

         if (npc.npcVersion == 15) {
            list = compound.getList("ScriptsContainers", 10);
            if (!list.isEmpty()) {
               ScriptContainer script = new ScriptContainer(npc.script);

               for(i = 0; i < list.size(); ++i) {
                  CompoundTag scriptOld = list.getCompound(i);
                  EnumScriptType type = EnumScriptType.values()[scriptOld.getInt("Type")];
                  String var10001 = script.script;
                  script.script = var10001 + "\nfunction " + type.function + "(event) {\n" + scriptOld.getString("Script") + "\n}";
                  for (String s : NBTTags.getStringList(compound.getList("ScriptList", 10))) {
                     if (!script.scripts.contains(s)) {
                        script.scripts.add(s);
                     }
                  }
               }
            }

            if (compound.getBoolean("CanDespawn")) {
               compound.putInt("SpawnCycle", 4);
            }

            if (compound.getInt("RangeAndMelee") <= 0) {
               compound.putInt("DistanceToMelee", 0);
            }
         }

         if (npc.npcVersion == 16) {
            compound.putString("HitSound", "random.bowhit");
            compound.putString("GroundSound", "random.break");
         }

         if (npc.npcVersion == 17) {
            if (compound.getString("NpcHurtSound").equals("minecraft:game.player.hurt")) {
               compound.putString("NpcHurtSound", "minecraft:entity.player.hurt");
            }

            if (compound.getString("NpcDeathSound").equals("minecraft:game.player.hurt")) {
               compound.putString("NpcDeathSound", "minecraft:entity.player.hurt");
            }

            if (compound.getString("FiringSound").equals("random.bow")) {
               compound.putString("FiringSound", "minecraft:entity.arrow.shoot");
            }

            if (compound.getString("HitSound").equals("random.bowhit")) {
               compound.putString("HitSound", "minecraft:entity.arrow.hit");
            }

            if (compound.getString("GroundSound").equals("random.break")) {
               compound.putString("GroundSound", "minecraft:block.stone.break");
            }
         }

         npc.npcVersion = ModRev;
      }
   }

   public static void CheckAvailabilityCompatibility(ICompatibilty compatibilty, CompoundTag compound) {
      if (compatibilty.getVersion() != ModRev) {
         CompatabilityFix(compound, compatibilty.save(new CompoundTag()));
         compatibilty.setVersion(ModRev);
      }
   }

   private static void CompatabilityFix(CompoundTag compound, CompoundTag check) {
      Collection<String> tags = check.getAllKeys();
      for (String name : tags) {
         Tag nbt = check.get(name);
         if (!compound.contains(name) && nbt != null) {
            compound.put(name, nbt);
         } else if (nbt instanceof CompoundTag && compound.get(name) instanceof CompoundTag) {
            CompatabilityFix(compound.getCompound(name), (CompoundTag) nbt);
         }
      }
   }

}
