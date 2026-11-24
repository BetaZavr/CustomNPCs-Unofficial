package noppes.npcs.ai;

import java.util.*;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.ability.AbstractAbility;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.world.entity.ILivingEntityMixin;
import noppes.npcs.util.Util;

public class CombatHandler {

   public final Map<LivingEntity, Long> lastDamages = new HashMap<>();
   public final Map<LivingEntity, Double> aggressors = new HashMap<>();
   private int combatResetTimer = 0;
   private int delay = 10;
   private final EntityNPCInterface npc;
   public boolean onlyPlayers = false;
   public LivingEntity priorityTarget = null;

   public CombatHandler(EntityNPCInterface npcIn) { npc = npcIn; }

   public boolean checkTarget() {
      if (aggressors.isEmpty() || npc.tickCount % 10 != 0) { return false; }
      LivingEntity target = npc.getTarget();
      Double current = 0.0d;
      if (isValidTarget(target)) {
         current = aggressors.get(target);
         if (current == null) { current = 0.0d; }
      } else {
         target = null;
      }
      for (Map.Entry<LivingEntity, Double> entry : aggressors.entrySet()) {
         if (entry.getValue() > current && isValidTarget(entry.getKey())) {
            current = entry.getValue();
            target = entry.getKey();
         }
      }
      if (target == null && priorityTarget != null && isValidTarget(priorityTarget)) {
         return false;
      }
      return target == null;
   }

   public void update() {
      if (npc.isKilled()) {
         if (npc.isAttacking()) { reset(); }
         return;
      }
      if (npc.getTarget() != null && !npc.isAttacking()) { start(); }
      if (!shouldCombatContinue()) {
         if (combatResetTimer++ > 40) { reset(); }
         return;
      }
      combatResetTimer = 0;
      if (aggressors.isEmpty()) {
         delay = 10;
         return;
      }
      delay--;
      if (delay > 0) { return; }
      delay = 10;
      List<LivingEntity> del = new ArrayList<>();
      double maxValue = Double.MIN_VALUE;
      priorityTarget = null;
      double maxDist = npc.stats.aggroRange * 2.0d;
      for (LivingEntity entity : aggressors.keySet()) {
         if (!isValidTarget(entity)) {
            del.add(entity);
            continue;
         }
         if (!Util.instance.canMoveEntityToEntity(npc, entity)) { continue; }
         double d = npc.distanceToSqr(entity);
         if (d > maxDist) { del.add(entity); }
         if (maxValue == Double.MIN_VALUE || aggressors.get(entity) >= maxValue) {
            maxValue = aggressors.get(entity);
            priorityTarget = entity;
         }
      }
      for (LivingEntity entity : del) { aggressors.remove(entity); }
      LivingEntity currentTarget = npc.getTarget();
      if (currentTarget != null && !isValidTarget(currentTarget)) { npc.setTarget(null); }
      // set priority target
      if (priorityTarget != null && (npc.getTarget() == null || !npc.getTarget().equals(priorityTarget))) {
         npc.setPriorityAttackTarget(priorityTarget);
         npc.getNavigation().moveTo(priorityTarget, 1.5);
         delay = 60;
      }
   }

   private boolean shouldCombatContinue() {
      return npc.getTarget() != null && isValidTarget(npc.getTarget());
   }

   public void damage(DamageSource source, double damageAmount) {
      combatResetTimer = 0;
      Entity e = NoppesUtilServer.getDamageSource(source);
      if (!(e instanceof LivingEntity attackingEntity)) { return; }
      if (attackingEntity instanceof Player) { onlyPlayers = true; }
      // Minimum
      if (damageAmount <= 0.25d) { damageAmount = 0.25d; }
      // Distance
      double dist = npc.distanceToSqr(attackingEntity);
      // Value
      double newValue = damageAmount;
      // further target, greater anger [5_block +0%; 32_blocks +25%]
      if (dist > 5 && dist < 32) { newValue *= 0.009259d * dist + 0.953704d; }
      // is player
      if (attackingEntity instanceof Player) { newValue *= 1.1d; }
      // target is tank
      if (attackingEntity.getMainHandItem().getItem() instanceof ShieldItem || attackingEntity.getOffhandItem().getItem() instanceof ShieldItem) {
         newValue *= 1.2d;
      }
      // or target is a damage dealer
      else if (source.is(DamageTypeTags.IS_PROJECTILE)) { newValue *= 1.025d; }
      // is current target
      if (npc.getTarget() != null && npc.getTarget().equals(attackingEntity)) { newValue *= 1.05d; }
      // add
      Double oldValue = aggressors.get(attackingEntity);
      if (oldValue == null) { oldValue = 0.0d; }
      aggressors.put(attackingEntity, oldValue + newValue);
      lastDamages.put(attackingEntity, npc.level().getGameTime());
      if (priorityTarget == null) { priorityTarget = attackingEntity; }
   }

   public void start() {
      combatResetTimer = 0;
      npc.getEntityData().set(EntityNPCInterface.Attacking, true);
      for (AbstractAbility ab : npc.abilities.abilities) { ab.startCombat(); }
   }

   public void reset() {
      combatResetTimer = 0;
      delay = 10;
      onlyPlayers = false;
      aggressors.clear();
      lastDamages.clear();
      priorityTarget = null;
      npc.getEntityData().set(EntityNPCInterface.Attacking, false);
      if (npc.getTarget() != null) { npc.setTarget(null); }
   }

   public boolean isValidTarget(LivingEntity target) {
      return target != null && target.isAlive() &&
              (!(target instanceof Player) || !((Player) target).getAbilities().invulnerable) &&
              npc.isInRange(target, npc.stats.aggroRange) &&
              npc.level().dimension().equals(target.level().dimension());
   }


   public boolean canDamage(DamageSource damageSource, float damage) {
      //(float) npc.invulnerableTime > 20.0F / 2.0F && damage <= ((ILivingEntityMixin) npc).lastHurt();
      Entity entity = NoppesUtilServer.getDamageSource(damageSource);
      int maxHurtTime = npc.ais.getMaxHurtResistantTime();
      if (!(entity instanceof LivingEntity)) {
         if (maxHurtTime != 0 && npc.hurtTime > maxHurtTime / 2.0F) {
            return damage > ((ILivingEntityMixin) npc).getLastHurt();
         }
         return true;
      }
      if (!lastDamages.containsKey(entity) || maxHurtTime == 0 || (lastDamages.get(entity) + maxHurtTime / 2) < npc.level().getGameTime()) {
         lastDamages.put((LivingEntity) entity, npc.level().getGameTime());
         return true;
      }
      return false;
   }

}
