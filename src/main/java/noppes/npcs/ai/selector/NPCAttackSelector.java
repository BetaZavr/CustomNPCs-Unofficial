package noppes.npcs.ai.selector;

import com.google.common.base.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.constants.EnumCompanionJobs;
import noppes.npcs.constants.EnumSeeTarget;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobGuard;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.companion.CompanionGuard;

public class NPCAttackSelector implements Predicate<LivingEntity> {

   private final EntityNPCInterface npc;

   public NPCAttackSelector(EntityNPCInterface npcIn) { npc = npcIn; }

   @Override
   public boolean apply(LivingEntity entity) {
      if (entity != null && entity.isAlive() &&
              entity != npc &&
              npc.isInRange(entity, npc.stats.aggroRange) &&
              entity.getHealth() >= 0.1F) {
         if (npc.aiAttackTarget != null && !npc.aiAttackTarget.canNewAttack()) { return false; }
         return nearHome(entity) && wanted(entity) && (npc.ais.directLOS == EnumSeeTarget.NONE || npc.canSee(entity));
      }
      return false;
   }

   private boolean nearHome(LivingEntity entity) {
      if (!npc.isFollower() && npc.ais.shouldReturnHome()) {
         int allowedDistance = npc.stats.aggroRange * 2;
         if (npc.ais.getMovingType() == 1) {
            allowedDistance += npc.ais.walkingRange;
         }
         double distance = entity.distanceToSqr(npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos());
         if (npc.ais.getMovingType() == 2) {
            int[] arr = npc.ais.getCurrentMovingPath();
            distance = entity.distanceToSqr(arr[0], arr[1], arr[2]);
         }
         return distance <= allowedDistance * allowedDistance;
      }
      return true;
   }

   private boolean wanted(LivingEntity entity) {
      if (npc.job instanceof JobGuard && ((JobGuard) npc.job).isEntityApplicable(entity)) { return true; }
      if (npc.role instanceof RoleCompanion role) {
         if (role.job.getType() == EnumCompanionJobs.GUARD && ((CompanionGuard) role.job).isEntityApplicable(entity)) { return true; }
      }
      if (entity instanceof ServerPlayer player) {
         if (npc.faction.isAggressiveToPlayer(player) && !player.isCreative()) {
            if (CustomNpcs.EnableInvisibleNpcs && CustomNpcs.InvisibilityAlgorithm == 2) {
               return npc.display.isVisibleTo(player) || player.isSpectator() ||
                       entity.getMainHandItem().getItem() == CustomItems.wand;
            }
            return true;
         }
         return false;
      }
      if (entity instanceof EntityNPCInterface cNpc) {
         if (!cNpc.isKilled() && npc.advanced.attackOtherFactions) { return npc.faction.isAggressiveToNpc(cNpc); }
      }
      return false;
   }

}
