package noppes.npcs.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.StairBlock;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.client.model.animation.AnimationFrameConfig;
import noppes.npcs.constants.EnumAnimationStages;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.CustomNPCsScheduler;

public class EntityAIAnimation extends Goal {

   private final EntityNPCInterface npc;
   private boolean isAttacking = false;
   private boolean removed = false;
   private boolean isAtStartPoint = false;
   private boolean hasPath = false;
   public int temp = 0;

   public EntityAIAnimation(EntityNPCInterface npcIn) { npc = npcIn; }

   @Override
   public boolean canUse() {
      removed = !npc.isAlive();
      if (removed) { return npc.currentAnimation != 2; }
      if (npc.stats.ranged.getHasAimAnimation() && npc.isAttacking()) { return npc.currentAnimation != 6; }
      hasPath = !npc.getNavigation().isDone();
      isAttacking = npc.isAttacking();
      isAtStartPoint = npc.ais.shouldReturnHome() && npc.isVeryNearAssignedPlace();
      if (temp != 0) {
         if (!hasNavigation()) { return npc.currentAnimation != temp; }
         temp = 0;
      }
      if (hasNavigation() && notWalkingAnimation(npc.currentAnimation)) { return npc.currentAnimation != 0; }
      return npc.currentAnimation != npc.ais.animationType;
   }

   @Override
   public void tick() {
      if (npc.stats.ranged.getHasAimAnimation() && npc.isAttacking()) { setAnimation(6); }
      else {
         int type = npc.ais.animationType;
         if (removed) { type = 2; }
         else if (notWalkingAnimation(npc.ais.animationType) && hasNavigation()) { type = 0; }
         else if (temp != 0) {
            if (hasNavigation()) { temp = 0; }
            else { type = temp;}
         }
         // if (this.npc.stats.ranged.getHasAimAnimation() && this.npc.isAttacking()) { type = 6; } // <- AI target
         setAnimation(type);
      }
   }

   public static int getWalkingAnimationGuiIndex(int animation) {
      return switch (animation) {
         case 3 -> 5;
         case 4 -> 1;
         case 5 -> 3;
         case 6 -> 2;
         case 7 -> 4;
         default -> 0;
      };
   }

   public static boolean notWalkingAnimation(int animation) { return getWalkingAnimationGuiIndex(animation) == 0; }

   private void setAnimation(int animation) {
      npc.setCurrentAnimation(animation);
      npc.refreshDimensions();
      npc.setPos(npc.getX(), npc.getY(), npc.getZ());
   }

   private boolean hasNavigation() { return isAttacking || npc.ais.shouldReturnHome() && !isAtStartPoint && !npc.isFollower() || hasPath; }

   // New from Unofficial (BetaZavr)
   public boolean playAttackEntityCustomAnimation(Entity target) {
      AnimationConfig anim = npc.animation.tryRunAnimation(AnimationKind.ATTACKING);
      if (anim != null) {
         for (int i = 0; i < anim.frames.size(); i++) {
            AnimationFrameConfig frame = anim.frames.get(i);
            if (frame.isNowDamage() && frame.damageDelay != 0) {
               CustomNPCsScheduler.runTack(() -> npc.tryAttackEntityAsMob(target, frame.id), frame.damageDelay * 50L);
               return false;
            }
         }
      }
      return npc.tryAttackEntityAsMob(target, 0);
   }

   public void playHitCustomAnimation() { npc.animation.tryRunAnimation(AnimationKind.HIT); }

   public void playBlockedCustomAnimation() { npc.animation.tryRunAnimation(AnimationKind.BLOCKED); }

   public void playDeathCustomAnimation() {
      AnimationConfig anim = npc.animation.tryRunAnimation(AnimationKind.DIES);
      if (anim != null) {
         npc.setDeltaMovement(0.0d, 0.0d, 0.0d);
      }
   }

   public void playShootCustomAnimation() {
      npc.animation.tryRunAnimation(AnimationKind.SHOOT);
      if (npc.animation.isAnimated(AnimationKind.AIM)) { npc.animation.stopAnimation(); }
   }

   public void playInteractCustomAnimation() {
      AnimationConfig anim = npc.animation.tryRunAnimation(AnimationKind.INTERACT);
      if (anim != null ) {
         npc.lookAi.fastRotation = true;
         CustomNPCsScheduler.runTack(() -> npc.lookAi.fastRotation = false , anim.totalTicks * 50L);
      }
   }

   public void playInitCustomAnimation() { npc.animation.tryRunAnimation(AnimationKind.INIT); }

   public void livingUpdate() {
      if (CustomNpcs.ShowCustomAnimation && !npc.isKilled()) {
         CustomNPCsScheduler.runTack(() -> {
            // Jump
            if (!npc.animation.getJump() && !npc.isKilled() &&
                    npc.getHealth() > 0.0f &&
                    !(npc.isInWater() || npc.isInLava()) && npc.ais.getNavigationType() == 0 &&
                    !npc.onGround() && npc.getDeltaMovement().y > 0.0d) {
               BlockPos posUnderfoot = npc.blockPosition().below();
               BlockPos posAhead = npc.blockPosition().offset(Mth.floor(npc.getDeltaMovement().x), 0, Mth.floor(npc.getDeltaMovement().z)).below();
               boolean canJumpHere = !(npc.level().getBlockState(posUnderfoot).getBlock() instanceof StairBlock);
               boolean canLandThere = !(npc.level().getBlockState(posAhead).getBlock() instanceof StairBlock);
               if (canJumpHere && canLandThere) {
                  npc.animation.setJump(true);
                  npc.animation.tryRunAnimation(AnimationKind.JUMP);
               }
            }
            else if (npc.animation.getJump() && npc.onGround() && npc.animation.getAnimationStage() != EnumAnimationStages.Started) {
               npc.animation.setJump(false);
               if (npc.animation.isAnimated(AnimationKind.JUMP)) { npc.animation.stopAnimation(); }
            }
            // Swing
            if (!npc.animation.getSwing() && npc.attackAnim > 0.0f) {
               npc.animation.setSwing(true);
               if (!npc.animation.isAnimated(AnimationKind.ATTACKING, AnimationKind.AIM, AnimationKind.SHOOT)) {
                  AnimationConfig anim = npc.animation.tryRunAnimation(AnimationKind.SWING);
                  if (anim != null) {
                     npc.attackAnim = 0.0f;
                     npc.swingTime = 0;
                     npc.oAttackAnim = 0.0f;
                     npc.swinging = false;
                  }
               }
            }
            else if (npc.animation.getSwing() && npc.swingTime == 0.0f) {
               npc.animation.setSwing(false);
            }
            // walking or standing
            npc.animation.resetWalkAndStandAnimations();
         });
      }
   }

}
