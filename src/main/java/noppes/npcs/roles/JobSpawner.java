package noppes.npcs.roles;

import java.util.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.constants.JobType;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.data.role.IJobSpawner;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.data.JobSpawnerNbtData;
import noppes.npcs.roles.data.NPCSpawnerSetting;
import noppes.npcs.util.ValueUtil;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nonnull;

public class JobSpawner extends JobInterface implements IJobSpawner {

   protected final @Nonnull EntityNPCInterface npc;
   protected final Map<Boolean, NPCSpawnerSetting> data = new HashMap<>(); // false=alive | true=dead
   protected String id = RandomStringUtils.random(8, true, true);
   protected long cooldownSet = 3000L; // setting time cooldown
   protected long cooldown = System.currentTimeMillis() + cooldownSet; // cooldown time if alive
   protected int distance = 60;
   protected LivingEntity target;
   public boolean exact = false;
   public boolean resetUpdate = true;

   public JobSpawner(@Nonnull EntityNPCInterface npcIn) {
      super(npcIn);
      npc = npcIn;
      type = JobType.SPAWNER;
      data.put(false, new NPCSpawnerSetting(npc));
      data.put(true, new NPCSpawnerSetting(npc));
   }

   @Override
   public CompoundTag save(CompoundTag compound) {
      super.save(compound);
      if (!data.containsKey(false)) { data.put(false, new NPCSpawnerSetting(npc)); }
      if (!data.containsKey(true)) { data.put(true, new NPCSpawnerSetting(npc)); }
      compound.putString("SpawnerId", id);
      compound.put("SettingWhenAlive", data.get(false).save());
      compound.put("SettingWhenDead", data.get(true).save());
      compound.putInt("SettingDistance", distance);
      compound.putLong("SpawnerCooldownSetting", cooldownSet);
      compound.putBoolean("IsExactOffsetSpawn", exact);
      compound.putBoolean("DespawnInReset", resetUpdate);
      return compound;
   }

   @Override
   public void load(CompoundTag compound) {
      super.load(compound);
      type = JobType.SPAWNER;
      id = compound.getString("SpawnerId");
      NPCSpawnerSetting alive = data.get(false);
      NPCSpawnerSetting dead = data.get(true);
      alive.clear();
      dead.clear();
      distance = 60;
      if (compound.contains("SpawnerDoesntDie", 1)) {
         cooldownSet = 3000L;
         exact = false;
         resetUpdate = true;
         List<IJobSpawner.IJobSpawnerData> sDs = new ArrayList<>();
         for (int i = 1; i < 7; i++) {
            if (!compound.contains("SpawnerNBT" + i, 10)) { continue; }
            JobSpawnerNbtData sd = new JobSpawnerNbtData(npc);
            sd.load(compound.getCompound("SpawnerNBT" + i));
            sDs.add(sd);
         }
         int i = 0;
         if (compound.getBoolean("SpawnerDoesntDie")) {
            dead.spawnType = compound.getInt("SpawnerType");
            dead.offset[0] = compound.getInt("SpawnerXOffset");
            dead.offset[1] = compound.getInt("SpawnerYOffset");
            dead.offset[2] = compound.getInt("SpawnerZOffset");
            dead.despawnOnTargetLost = compound.getBoolean("DespawnOnTargetLost");
            for (IJobSpawner.IJobSpawnerData sd : sDs) { dead.dataEntitys.put(i++, sd); }
         } // dead
         else {
            alive.spawnType = compound.getInt("SpawnerType");
            alive.offset[0] = compound.getInt("SpawnerXOffset");
            alive.offset[1] = compound.getInt("SpawnerYOffset");
            alive.offset[2] = compound.getInt("SpawnerZOffset");
            alive.despawnOnTargetLost = compound.getBoolean("DespawnOnTargetLost");
            for (IJobSpawner.IJobSpawnerData sd : sDs) { alive.dataEntitys.put(i++, sd); }
         } // Alive
      } // very OLD
      else {
         cooldownSet = compound.getLong("SpawnerCooldownSetting");
         exact = compound.getBoolean("IsExactOffsetSpawn");
         resetUpdate = compound.getBoolean("DespawnInReset");
         if (compound.contains("SettingDistance", 3)) { distance = compound.getInt("SettingDistance"); }
         if (compound.contains("SpawnerWhenAlive", 3)) {
            alive.spawnType = compound.getInt("SpawnerWhenAlive");
            dead.spawnType = compound.getInt("SpawnerWhenDead");
            for (int i = 0; i < 2; i++) {
               int[] array = compound.getIntArray("OffsetWhen" + (i == 0 ? "Alive" : "Dead"));
               for (int k = 0; k < 3 && k < array.length; k++) {
                  (i == 0 ? alive : dead).offset[k] = array[k];
               }
               ListTag nbt = compound.getList("DataEntitysWhen" + (i == 0 ? "Alive" : "Dead"), 10);
               for (int slot = 0; slot < nbt.size(); slot++) {
                  JobSpawnerNbtData sd = new JobSpawnerNbtData(npc);
                  sd.load(nbt.getCompound(slot));
                  (i == 0 ? alive : dead).dataEntitys.put(slot, sd);
               }
            }
            alive.despawnOnTargetLost = compound.getBoolean("DespawnOnTargetLostWhenAlive");
            dead.despawnOnTargetLost = compound.getBoolean("DespawnOnTargetLostWhenDead");
         } // OLD in 1.12.2
         else {
            alive.load(compound.getCompound("SettingWhenAlive"));
            dead.load(compound.getCompound("SettingWhenDead"));
         }
      } // NEW
   }

   @Override
   public void aiDeathExecute(Entity attackingEntity) {
      if (attackingEntity instanceof LivingEntity) { target = (LivingEntity) attackingEntity; }
      aiUpdateTask();
   } // when death

   @Override
   public boolean aiShouldExecute() {
      if (!data.containsKey(false)) { data.put(false, new NPCSpawnerSetting(npc)); }
      if (!data.containsKey(true)) { data.put(true, new NPCSpawnerSetting(npc)); }
      boolean isDead = npc.getHealth() <= 0;
      if (isEmpty(isDead) || npc.isKilled()) { return false; }
      target = getTarget();
      if (!data.get(isDead).spawned.isEmpty()) { checkSpawns(); }
      return target != null;
   }

   @Override
   public void aiStartExecuting() {
      for (int i = 0; i < 2; i++) {
         NPCSpawnerSetting npcSS = data.get(i == 0);
         npcSS.number = 0;
         for (Entity entity : new ArrayList<>(npcSS.spawned)) {
            int slot = entity.getPersistentData().getInt("NpcSpawnerSlot");
            if (slot > npcSS.number) { npcSS.number = slot; }
            conveyTarget(entity, getTarget());
         }
      }
   } // after reset NPC

   @Override
   public void aiUpdateTask() {
      boolean isDead = npc.getHealth() <= 0;
      NPCSpawnerSetting npcSS = data.get(isDead);
      if (!npcSS.spawned.isEmpty()) {
         if (npc.level().getGameTime() % 20 == 0) {
            cooldown = System.currentTimeMillis()
                    + (long) ((double) cooldownSet * (npc.getRandom().nextFloat() < 0.5f ? 1.1d : 0.9d));
         }
         checkSpawns();
         return;
      } // Has Spawned
      if (getTarget() == null || !isDead && isOnCooldown()) { return; } // is Alive and or Cooldown
      switch (npcSS.spawnType) {
         case 0: {
            spawnEntity(npcSS.number, isDead);
            npcSS.number++;
            if (npcSS.number > npcSS.dataEntitys.size()) { npcSS.number = 0; }
            break;
         } // one to one
         case 1: {
            while (npcSS.dataEntitys.size() > 7) { npcSS.dataEntitys.remove(npcSS.dataEntitys.size() - 1); }
            for (int slot : npcSS.dataEntitys.keySet()) {
               npcSS.number = slot;
               spawnEntity(slot, isDead);
            }
            break;
         } // all
         default: {
            npcSS.number = npc.getRandom().nextInt(npcSS.dataEntitys.size());
            spawnEntity(npcSS.number, isDead);
            break;
         } // random
      }
   } // after start any 20 ticks

   public void checkSpawns() {
      for (int i = 0; i < 2; i++) {
         NPCSpawnerSetting npcSS = data.get(i == 0);
         for (Entity spawn : new ArrayList<>(npcSS.spawned)) {
            if (shouldDelete(spawn)) {
               spawn.discard();
               npcSS.spawned.remove(spawn);
            }
            else { checkTarget(spawn); }
         }
      }
   }

   public void checkTarget(Entity entity) {
      if (entity instanceof Mob liv) {
         if (liv.getTarget() == null || npc.getRandom().nextInt(100) == 1) {
            liv.setTarget(target);
         }
      }
      else if (entity instanceof LivingEntity living) {
         if (living.getLastHurtByMob() == null || npc.getRandom().nextInt(100) == 1) {
            living.setLastHurtByMob(target);
         }
      }
   }

   public void cleanCompound(CompoundTag compound) {
      for (int i = 0; i < 2; i++) {
         String key = "DataEntitysWhen" + (i == 0 ? "Alive" : "Dead");
         ListTag list = compound.getList(key, 10);
         for (int j = 0; j < list.size(); j++) {
            CompoundTag sdNbt = list.getCompound(j).getCompound("EntityNBT");
            String name = "type.empty";
            sdNbt = sdNbt.copy();
            if (sdNbt.contains("ClonedName", 8)) {
               name = sdNbt.getString("ClonedName");
            }
            else if (sdNbt.contains("id", 8)) {
               Optional<Entity> entity = EntityType.create(sdNbt, npc.level());
               if (entity.isPresent()) { name = entity.get().getName().getString(); }
            }
            compound.getList(key, 10).getCompound(j).remove("EntityNBT");
            compound.getList(key, 10).getCompound(j).putString("Name", name);
            if (sdNbt.contains("ClonedName", 8)) {
               compound.getList(key, 10).getCompound(j).putString("ClonedName", sdNbt.getString("ClonedName"));
            }
            if (sdNbt.contains("ClonedTab", 3)) {
               compound.getList(key, 10).getCompound(j).putInt("ClonedTab", sdNbt.getInt("ClonedTab"));
            }
         }
      }
   }

   public void clear(boolean isDead) { data.get(isDead).dataEntitys.clear(); }

   @Override
   public NPCSpawnerSetting get(boolean isDead) { return data.get(isDead); }

   public long getCooldown() { return cooldownSet; }

   public boolean getDespawnOnTargetLost(boolean isDead) { return data.get(isDead).despawnOnTargetLost; }

   public String getId() { return id; }

   private List<LivingEntity> getNearbySpawned(boolean isDead) {
      List<LivingEntity> list = npc.level().getEntitiesOfClass(LivingEntity.class, npc.getBoundingBox().inflate(distance, distance, distance),
              entity -> !entity.isRemoved() && entity.getPersistentData().getString("NpcSpawnerId").equals(id)
              && entity.getPersistentData().getBoolean("NpcSpawnerDead") == isDead);
      return new ArrayList<>(list);
   }

   public int[] getOffset(boolean isDead) { return data.get(isDead).offset; }

   public int getSpawnType(boolean isDead) { return data.get(isDead).spawnType; }

   private LivingEntity getTarget() {
      target = getTarget(npc);
      if (target != null) { return target; }
      for (int i = 0; i < 2; i++) {
         for (Entity entity : data.get(i == 0).spawned) {
            if (entity instanceof LivingEntity living) {
               target = getTarget(living);
               if (target != null) { return target; }
            }
         }
      }
      return target;
   }

   private LivingEntity getTarget(LivingEntity entity) {
      if (entity == null || (entity == npc && (entity.isRemoved() || entity.getHealth() <= 0.0))) {
         return target;
      }
      if (entity instanceof Mob liv) {
         target = liv.getTarget();
         if (target != null && !target.isRemoved() && target.getHealth() > 0.0f) {
            return target;
         }
      }
      target = entity.getLastHurtByMob();
      if (target != null && !target.isRemoved() && target.getHealth() > 0.0f) {
         return entity.distanceTo(target) > distance ? null : target;
      }
      return null;
   }

   public boolean isOnCooldown() { return System.currentTimeMillis() < cooldown; }

   @Override
   public void killed() { reset(); }

   @Override
   public void clear() {
      for (int i = 0; i < 2; i++) {
         for (Entity entity : data.get(i == 0).spawned) { entity.discard(); }
         data.get(i == 0).spawned.clear();
      }
   }

   @SuppressWarnings("unused")
   public void removeCompound(CompoundTag compound) {
      for (int i = 0; i < 2; i++) {
         String keyOld = "DataEntitysWhen" + (i == 0 ? "Alive" : "Dead");
         String key = "SettingWhen" + (i == 0 ? "Alive" : "Dead");
         ListTag list = compound.getList(keyOld, 10);
         for (int j = 0; j < list.size(); j++) { list.getCompound(j).remove("EntityNBT"); }
         list = compound.getCompound(key).getList("DataEntitys", 10);
         for (int j = 0; j < list.size(); j++) {
            CompoundTag nbt = list.getCompound(j);
            if (!nbt.contains("tag", 3) && !nbt.contains("name", 8)) { list.remove(nbt); }
         }
      }
   }

   public void removeSpawned(int slot, boolean isDead) {
      NPCSpawnerSetting settings = data.get(isDead);
      if (slot >= 0 && slot < settings.dataEntitys.size()) {
         Map<Integer, IJobSpawner.IJobSpawnerData> newSData = new HashMap<>();
         for (int i = 0, j = 0; i < settings.dataEntitys.size(); i++) {
            if (i != slot) {
               newSData.put(j, settings.dataEntitys.get(i));
               j++;
            }
         }
         settings.dataEntitys.clear();
         settings.dataEntitys.putAll(newSData);
      }
   }

   @Override
   public void reset() {
      for (int i = 0; i < 2; i++) {
         data.get(i == 0).number = 0;
         if (data.get(i == 0).spawned.isEmpty()) { data.get(i == 0).spawned.addAll(getNearbySpawned(i == 0)); }
      }
      target = null;
      cooldown = 0L;
      checkSpawns();
   }

   @Override
   public void stop() { reset(); }

   public void setCooldown(int ticks) { cooldownSet = ValueUtil.onlyPositiveInt(ticks, 6000) * 50L; }

   public void setCooldown(long ticks) {
      if (ticks < 0L) { ticks *= -1; }
      if (ticks > 300000L) { ticks = 300000L; }
      cooldownSet = ticks;
   }

   public void setDespawnOnTargetLost(boolean isDead, boolean isLost) { data.get(isDead).despawnOnTargetLost = isLost; }

   public void setSpawnType(boolean isDead, int readInt) {
      if (readInt < 0) { readInt *= -1; }
      if (readInt > 2) { readInt = readInt % 3; }
      data.get(isDead).spawnType = readInt;
   }

   private void conveyTarget(Entity summon, LivingEntity targetIn) {
      if (summon instanceof Mob liv) { liv.setTarget(targetIn); }
      else if (summon instanceof LivingEntity living) { living.setLastHurtByMob(targetIn); }
      if (npc == summon) {
         if (npc.getHealth() > 0.0f) { npc.setTarget(targetIn); }
         target = targetIn;
      }
   }

   public boolean shouldDelete(Entity entity) {
      IJobSpawner.IJobSpawnerData sp = null;
      boolean sets = false;
      boolean isDead = npc.getHealth() <= 0;
      // EntityData
      CompoundTag eNbt = entity.getPersistentData();
      if (eNbt.contains("NpcSpawnerEntityId", 3) && eNbt.contains("NpcSpawnerSlot", 3)
              && eNbt.contains("NpcSpawnerId", 8)
              && eNbt.contains("NpcSpawnerDead", 1)) {
         if (resetUpdate && isDead != eNbt.getBoolean("NpcSpawnerDead")) { return true; }
         sets = eNbt.getString("NpcSpawnerId").equals(id) && eNbt.getInt("NpcSpawnerEntityId") == npc.getId();
         sp = data.get(eNbt.getBoolean("NpcSpawnerDead")).get(eNbt.getInt("NpcSpawnerSlot"));
      }
      if (!sets || sp == null) { return true; }
      // Destination or Dead
      if (entity.isRemoved() || (entity instanceof LivingEntity living && living.getHealth() <= 0.0f)) { return true; }
      if (!npc.isInRange(entity, distance)) {
         if (entity instanceof LivingEntity living) { living.setLastHurtByMob(null); }
         entity.setPos(npc.getX(), npc.getY(), npc.getZ());
         return false;
      }
      // Target
      if (!data.get(isDead).despawnOnTargetLost) { return false; }
      if (entity instanceof LivingEntity living) {
         if (living.getLastHurtByMob() == null) { conveyTarget(living, getTarget()); } // try set
         if (living.getLastHurtByMob() == null) { living.setLastHurtByMob(getTarget()); }
         return living.getLastHurtByMob() == null;
      }
      return false;
   }

   public int size(boolean isDead) { return data.get(isDead).dataEntitys.size(); }

   @Override
   public List<IEntity<?>> spawnEntity(int slotId, boolean isDead) {
      NPCSpawnerSetting settings = data.get(isDead);
      IJobSpawner.IJobSpawnerData sd = settings.get(slotId);
      List<IEntity<?>> list = new ArrayList<>();
      if (sd != null && sd.isValid()) {
         if (target == null) { target = npc.getTarget(); }
         if (!isDead && (target == null || npc.distanceTo(getTarget()) > npc.stats.aggroRange)) { return list; }
         for (int i = 0; i < sd.getCount(); i++) {
            Entity entity = sd.getEntity().getMCEntity();
            if (npc.level().getDifficulty() == Difficulty.PEACEFUL && entity instanceof Mob) { continue; }
            int add = !exact && settings.spawnType == 1 ? 2 : 0;
            double x = npc.getX() + (add + settings.offset[0]) * (exact ? 1 : npc.getRandom().nextFloat() * (npc.getRandom().nextFloat() < 0.5f ? -1 : 1)) - 0.5 + npc.getRandom().nextFloat();
            double y = npc.getY() + (add + settings.offset[1]) * (exact ? 1 : npc.getRandom().nextFloat() * (npc.getRandom().nextFloat() < 0.5f ? -1 : 1));
            double z = npc.getZ() + (add + settings.offset[2]) * (exact ? 1 : npc.getRandom().nextFloat() * (npc.getRandom().nextFloat() < 0.5f ? -1 : 1)) - 0.5 + npc.getRandom().nextFloat();
            Path path = npc.getNavigation().createPath(x, y, z, 1);
            if (path != null && path.getEndNode() != null) {
               x = path.getEndNode().x;
               y = path.getEndNode().y;
               z = path.getEndNode().z;
            } // Corrector
            else {
               x = npc.getX();
               y = npc.getY();
               z = npc.getZ();
            }
            entity.setPos(x, y, z);
            npc.level().addFreshEntity(entity);
            entity.getPersistentData().putInt("NpcSpawnerEntityId", npc.getId());
            entity.getPersistentData().putInt("NpcSpawnerSlot", data.get(isDead).number);
            entity.getPersistentData().putString("NpcSpawnerId", id);
            entity.getPersistentData().putBoolean("NpcSpawnerDead", isDead);
            conveyTarget(entity, target);
            entity.setPos(x, y, z);
            if (entity instanceof EntityNPCInterface cnpc) {
               cnpc.advanced.spawner = npc;
               cnpc.stats.spawnCycle = 4;
               cnpc.stats.respawnTime = 0;
               cnpc.ais.returnToStart = false;
               cnpc.ais.onAttack = 0;
            }
            data.get(isDead).spawned.add(entity);
            list.add(Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity));
         }
      }
      return list;
   }

   private boolean isEmpty(boolean isDead) {
      for (IJobSpawner.IJobSpawnerData sd : data.get(isDead).dataEntitys.values()) {
         if (sd.isValid()) { return false; }
      }
      return true;
   }

}
