package noppes.npcs.ai;

import java.util.EnumSet;
import java.util.Iterator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.api.util.IRayTraceRotate;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.Util;

public class EntityAILook extends Goal {

   private final EntityNPCInterface npc;
   private int idle = 0;
   private double lookX;
   private double lookZ;
   private boolean forced = false;
   private Entity forcedEntity = null;

   // New from Unofficial (BetaZavr)
   private double lookY;
   boolean rotateBody;
   public boolean fastRotation = false;

   public EntityAILook(EntityNPCInterface npcIn) {
      npc = npcIn;
      setFlags(EnumSet.of(Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      if (forced) { return true; }
      if (!npc.isAttacking() && npc.getNavigation().isDone() && !npc.isSleeping() && npc.isAlive() && (!CustomNpcs.ShowCustomAnimation ||
              !npc.animation.isAnimated(AnimationKind.ATTACKING, AnimationKind.INIT, AnimationKind.INTERACT, AnimationKind.DIES))) {
         if (!npc.isInteracting() && npc.ais.getStandingType() <= 0 && idle <= 0) { return npc.getRandom().nextFloat() < 0.004F; }
         return true;
      }
      return false;
   }

   @Override
   public void start() {
      rotateBody = npc.ais.getStandingType() == 0 || npc.ais.getStandingType() == 3;
      if (rotateBody) {
         double d0 = Math.PI * 2.0d * npc.getRandom().nextDouble();
         if (npc.ais.getStandingType() == 3) {
            double d1 = Math.PI / 180.0d;
            double d2 = Math.PI / 5.0d;
            double d3 = Math.PI * 3.0d / 5.0d;
            d0 = d1 * npc.ais.orientation + d2 + d3 * npc.getRandom().nextDouble();
         }
         lookX = Math.cos(d0);
         lookZ = Math.sin(d0);
         idle = 20 + npc.getRandom().nextInt(20);
      }
   }

   @Override
   public void stop() {
      rotateBody = false;
      forced = false;
      forcedEntity = null;
   }

   @Override
   public void tick() {
      Entity lookat = null;
      if (forced && forcedEntity != null) { lookat = forcedEntity; }
      else if (npc.isInteracting()) {
         Iterator<LivingEntity> ita = npc.interactingEntities.iterator();
         double closestDistance = 12.0D;
         while(ita.hasNext()) {
            LivingEntity entity = ita.next();
            double distance = entity.distanceToSqr(npc);
            if (distance < closestDistance) {
               closestDistance = entity.distanceToSqr(npc);
               lookat = entity;
            }
            else if (distance > 12.0D) { ita.remove(); }
         }
      }
      else if (npc.ais.getStandingType() == 2 || npc.ais.getStandingType() == 4) {
         lookat = npc.level().getNearestPlayer(npc, 16.0D);
      } // Stalking or EyeRotation
      // looking at someone
      if (lookat != null) {
         npc.updateLook = npc.lookAt == null || !npc.lookAt.equals(lookat);
         npc.lookAt = lookat;
         double posY;
         if (lookat instanceof LivingEntity) { posY = lookat.getY() + lookat.getEyeHeight(); }
         else { posY = (lookat.getBoundingBox().minY + lookat.getBoundingBox().maxY) / 2.0D; }
         setLookPosition(lookat.getX(), posY, lookat.getZ(), npc.getMaxHeadXRot());
         return;
      }
      // looks in a random direction
      npc.updateLook = npc.lookAt != null;
      npc.lookAt = null;
      if (rotateBody) {
         if (idle == 0 && npc.getRandom().nextFloat() < 0.004f) {
            double d0 = Math.PI * npc.getRandom().nextDouble() * 2.0;
            if (npc.ais.getStandingType() == 3) { // only head
               double d1 = Math.PI / 180.0d;
               double d2 = Math.PI / 5.0d;
               double d3 = Math.PI * 3.0d / 5.0d;
               d0 = d1 * npc.ais.orientation + d2 + d3 * npc.getRandom().nextDouble();
            }
            lookX = Math.cos(d0);
            lookY = (npc.getRandom().nextFloat() - 0.5f) * 0.85f;
            lookZ = Math.sin(d0);

            IRayTraceRotate data = Util.instance.getAngles3D(npc.getX(), npc.getY(), npc.getZ(), lookX, lookY, lookZ);
            npc.lookPos[0] = (float) data.getYaw();
            npc.lookPos[1] = (float) data.getPitch();
            npc.updateClient();
            idle = 20 + npc.getRandom().nextInt(20);
         } else if (npc.ais.getStandingType() == 3 || npc.ais.getStandingType() == 0) {
            if (lookX != 0.0f && lookY != 0.0f && lookZ != 0.0f) {
               setLookPosition(npc.getX() + lookX, npc.getY() + npc.getEyeHeight() + lookY, npc.getZ() + lookZ, npc.getMaxHeadXRot());
            }
         }
         if (idle > 0) {
            --idle;
            setLookPosition(npc.getX() + lookX, npc.getY() + npc.getEyeHeight() + lookY, npc.getZ() + lookZ, npc.getMaxHeadXRot());
         }
      }
      // doesn't look at anyone
      if ((npc.ais.getStandingType() == 1 || npc.ais.getStandingType() == 4) && !forced) {
         npc.setYBodyRot(npc.ais.orientation);
         npc.setYRot(npc.ais.orientation);
         npc.setYHeadRot(npc.ais.orientation);
      }
   }

   public void rotate(Entity entity) {
      forced = true;
      forcedEntity = entity;
   }

   public void rotate(float degrees) {
      forced = true;
      npc.yHeadRot = npc.yBodyRot = degrees;
      npc.setYRot(degrees);
   }

   // New from Unofficial (BetaZavr)
   private void setLookPosition(double x, double y, double z, int verticalFaceSpeed) {
      if (!CustomNpcs.ShowCustomAnimation || !npc.animation.isAnimated(AnimationKind.ATTACKING, AnimationKind.INIT, AnimationKind.INTERACT, AnimationKind.DIES)) {
         npc.getLookControl().setLookAt(x, y, z, 10.0f, verticalFaceSpeed);
      }
   }

}
