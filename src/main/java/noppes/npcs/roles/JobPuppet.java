package noppes.npcs.roles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.constants.JobType;
import noppes.npcs.api.entity.data.role.IJobPuppet;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;

public class JobPuppet extends JobInterface implements IJobPuppet {

   protected final @Nonnull EntityNPCInterface npc;

   public JobPuppet.PartConfig head = new JobPuppet.PartConfig();
   public JobPuppet.PartConfig larm = new JobPuppet.PartConfig();
   public JobPuppet.PartConfig rarm = new JobPuppet.PartConfig();
   public JobPuppet.PartConfig body = new JobPuppet.PartConfig();
   public JobPuppet.PartConfig lleg = new JobPuppet.PartConfig();
   public JobPuppet.PartConfig rleg = new JobPuppet.PartConfig();
   public JobPuppet.PartConfig head2 = new JobPuppet.PartConfig();
   public JobPuppet.PartConfig larm2 = new JobPuppet.PartConfig();
   public JobPuppet.PartConfig rarm2 = new JobPuppet.PartConfig();
   public JobPuppet.PartConfig body2 = new JobPuppet.PartConfig();
   public JobPuppet.PartConfig lleg2 = new JobPuppet.PartConfig();
   public JobPuppet.PartConfig rleg2 = new JobPuppet.PartConfig();
   public boolean whileStanding = true;
   public boolean whileAttacking = false;
   public boolean whileMoving = false;
   public boolean animate = false;
   public int animationSpeed = 4;
   private int prevTicks = 0;
   private int startTick = 0;
   private float val = 0.0F;
   private float valNext = 0.0F;

   public JobPuppet(@Nonnull EntityNPCInterface npcIn) {
      super(npcIn);
      npc = npcIn;
      type = JobType.PUPPET;
   }

   @Override
   public IJobPuppet.IJobPuppetPart getPart(int part) {
      return switch (part) {
         case 0 -> head;
         case 1 -> larm;
         case 2 -> rarm;
         case 3 -> body;
         case 4 -> lleg;
         case 5 -> rleg;
         case 6 -> head2;
         case 7 -> larm2;
         case 8 -> rarm2;
         case 9 -> body2;
         case 10 -> lleg2;
         case 11 -> rleg2;
         default -> throw new CustomNPCsException("Unknown part " + part);
      };
   }

   @Override
   public CompoundTag save(CompoundTag compound) {
      super.save(compound);
      compound.put("PuppetHead", head.save());
      compound.put("PuppetLArm", larm.save());
      compound.put("PuppetRArm", rarm.save());
      compound.put("PuppetBody", body.save());
      compound.put("PuppetLLeg", lleg.save());
      compound.put("PuppetRLeg", rleg.save());
      compound.put("PuppetHead2", head2.save());
      compound.put("PuppetLArm2", larm2.save());
      compound.put("PuppetRArm2", rarm2.save());
      compound.put("PuppetBody2", body2.save());
      compound.put("PuppetLLeg2", lleg2.save());
      compound.put("PuppetRLeg2", rleg2.save());
      compound.putBoolean("PuppetStanding", whileStanding);
      compound.putBoolean("PuppetAttacking", whileAttacking);
      compound.putBoolean("PuppetMoving", whileMoving);
      compound.putBoolean("PuppetAnimate", animate);
      compound.putInt("PuppetAnimationSpeed", animationSpeed);
      return compound;
   }

   @Override
   public void load(CompoundTag compound) {
      super.load(compound);
      type = JobType.PUPPET;
      head.load(compound.getCompound("PuppetHead"));
      larm.load(compound.getCompound("PuppetLArm"));
      rarm.load(compound.getCompound("PuppetRArm"));
      body.load(compound.getCompound("PuppetBody"));
      lleg.load(compound.getCompound("PuppetLLeg"));
      rleg.load(compound.getCompound("PuppetRLeg"));
      head2.load(compound.getCompound("PuppetHead2"));
      larm2.load(compound.getCompound("PuppetLArm2"));
      rarm2.load(compound.getCompound("PuppetRArm2"));
      body2.load(compound.getCompound("PuppetBody2"));
      lleg2.load(compound.getCompound("PuppetLLeg2"));
      rleg2.load(compound.getCompound("PuppetRLeg2"));
      whileStanding = compound.getBoolean("PuppetStanding");
      whileAttacking = compound.getBoolean("PuppetAttacking");
      whileMoving = compound.getBoolean("PuppetMoving");
      setIsAnimated(compound.getBoolean("PuppetAnimate"));
      setAnimationSpeed(compound.getInt("PuppetAnimationSpeed"));
   }

   private float calcRotation(float r, float r2, float partialTicks) {
      if (!animate) { return r; }
      if (prevTicks != npc.tickCount) {
         float speed = switch (animationSpeed) {
            case 0 -> 40.0F;
            case 1 -> 24.0F;
            case 2 -> 13.0F;
            case 3 -> 10.0F;
            case 4 -> 7.0F;
            case 5 -> 4.0F;
            case 6 -> 3.0F;
            case 7 -> 2.0F;
            default -> 0.0F;
         };
         float ticks = (float) npc.tickCount - (float) startTick;
         val = 1.0F - (Mth.cos(ticks / speed * (float) Math.PI / 2.0F) + 1.0F) / 2.0F;
         valNext = 1.0F - (Mth.cos((ticks + 1) / speed * (float) Math.PI / 2.0F) + 1.0F) / 2.0F;
         prevTicks = npc.tickCount;
      }
      return r + (r2 - r) * (val + (valNext - val) * partialTicks);
   }

   public float getRotationX(JobPuppet.PartConfig part1, JobPuppet.PartConfig part2, float partialTicks) {
      return calcRotation(part1.rotationX, part2.rotationX, partialTicks);
   }

   public float getRotationY(JobPuppet.PartConfig part1, JobPuppet.PartConfig part2, float partialTicks) {
      return calcRotation(part1.rotationY, part2.rotationY, partialTicks);
   }

   public float getRotationZ(JobPuppet.PartConfig part1, JobPuppet.PartConfig part2, float partialTicks) {
      return calcRotation(part1.rotationZ, part2.rotationZ, partialTicks);
   }

   @Override
   public void reset() {
      val = 0.0F;
      valNext = 0.0F;
      prevTicks = 0;
      startTick = npc.tickCount;
   }

   public boolean isActive() {
      return npc.advanced.animationType == 1 &&
              npc.isAlive() &&
              ((whileAttacking && npc.isAttacking()) || (whileMoving && npc.isWalking()) || (whileStanding && !npc.isWalking()));
   }

   @Override
   public boolean getIsAnimated() { return animate; }

   @Override
   public void setIsAnimated(boolean bo) {
      if (npc.advanced.animationType == 1) {
         animate = bo;
         if (!bo) {
            val = 0.0F;
            valNext = 0.0F;
            prevTicks = 0;
         }
         else { startTick = npc.tickCount; }
         npc.updateClient = true;
      }
   }

   @Override
   public int getAnimationSpeed() { return animationSpeed; }

   @Override
   public void setAnimationSpeed(int speed) {
      animationSpeed = ValueUtil.correctInt(speed, 0, 7);
      npc.updateClient = true;
   }

   public class PartConfig implements IJobPuppet.IJobPuppetPart {

      public float rotationX = 0.0F;
      public float rotationY = 0.0F;
      public float rotationZ = 0.0F;
      public boolean disabled = false;

      public CompoundTag save() {
         CompoundTag compound = new CompoundTag();
         compound.putFloat("RotationX", rotationX);
         compound.putFloat("RotationY", rotationY);
         compound.putFloat("RotationZ", rotationZ);
         compound.putBoolean("Disabled", disabled);
         return compound;
      }

      public void load(CompoundTag compound) {
         rotationX = ValueUtil.correctFloat(compound.getFloat("RotationX"), -1.0F, 1.0F);
         rotationY = ValueUtil.correctFloat(compound.getFloat("RotationY"), -1.0F, 1.0F);
         rotationZ = ValueUtil.correctFloat(compound.getFloat("RotationZ"), -1.0F, 1.0F);
         disabled = compound.getBoolean("Disabled");
      }

      @Override
      public int getRotationX() { return (int)((rotationX + 1.0F) * 180.0F); }

      @Override
      public int getRotationY() { return (int)((rotationY + 1.0F) * 180.0F); }

      @Override
      public int getRotationZ() { return (int)((rotationZ + 1.0F) * 180.0F); }

      @Override
      public void setRotation(int x, int y, int z) {
         disabled = false;
         rotationX = ValueUtil.correctFloat((float)x / 180.0F - 1.0F, -1.0F, 1.0F);
         rotationY = ValueUtil.correctFloat((float)y / 180.0F - 1.0F, -1.0F, 1.0F);
         rotationZ = ValueUtil.correctFloat((float)z / 180.0F - 1.0F, -1.0F, 1.0F);
         npc.updateClient = true;
      }

   }

}
