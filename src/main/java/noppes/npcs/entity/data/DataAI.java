package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.common.ForgeMod;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IPos;
import noppes.npcs.api.entity.data.INPCAi;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.constants.EnumNpcTactics;
import noppes.npcs.constants.EnumSeeTarget;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobBuilder;
import noppes.npcs.roles.JobFarmer;
import noppes.npcs.util.ValueUtil;

public class DataAI implements INPCAi {

   protected final EntityNPCInterface npc;
   protected List<int[]> movingPath = new ArrayList<>();
   protected BlockPos startPos = BlockPos.ZERO;
   protected int standingType = 0; // 0:NoRotation, 1:RotateBody, 2:Stalking, 3:HeadRotation, 4:EyeRotation
   protected int movingType = 0; // 0:Standing, 1:Wandering, 2:MovingPath -> EntityAIMovingPath
   protected int moveSpeed = 5;

   public boolean canSwim = true;
   public boolean reactsToFire = false;
   public boolean avoidsWater = false;
   public boolean avoidsSun = false;
   public boolean returnToStart = true;
   public boolean canLeap = false; // can jump to target
   public boolean canSprint = false;
   public boolean stopAndInteract = true;
   public boolean attackInvisible = false;
   public boolean npcInteracting = true;
   public boolean movingPause = true; // -> EntityAIMovingPath
   public boolean mountControl = false;

   public int onAttack = 0; // 0:Normal, 1:Panic, 2:Retreat, 3:Nothing
   public int doorInteract = 2;
   public int findShelter = 2; // 0:Night, 1:Day, 2:Disable
   public int movementType = 0; // 0:Ground, 1:Flying, 2:Swimming
   public int movingPos = 0; // -> EntityAIMovingPath
   public int movingPattern = 0;// -> EntityAIMovingPath
   public int animationType = 0;
   public int orientation = 0;
   public int walkingRange = 10;

   public float bodyOffsetX = 5.0F;
   public float bodyOffsetY = 5.0F;
   public float bodyOffsetZ = 5.0F;

   // in 1.12.2
   protected int tacticalRadius = 4;
   public EnumNpcTactics tacticalVariant = EnumNpcTactics.RUSH;

   // New from Unofficial (GoodBird)
   public int activeRange = 32;

   // New fields from Unofficial (BetaZavr)
   public EnumSeeTarget directLOS = EnumSeeTarget.NORMAL; // old: true
   protected int maxHurtResistantTime = CustomNpcs.DefaultHurtResistantTime * 2;
   public boolean aiDisabled = false;
   public boolean canBeCollide = true;
   public float stepheight = 0.6f;

   public DataAI(EntityNPCInterface npcIn) { npc = npcIn; }

   public void load(CompoundTag compound) {
      canSwim = compound.getBoolean("CanSwim");
      reactsToFire = compound.getBoolean("ReactsToFire");
      setAvoidsWater(compound.getBoolean("AvoidsWater"));
      avoidsSun = compound.getBoolean("AvoidsSun");
      returnToStart = compound.getBoolean("ReturnToStart");
      onAttack = compound.getInt("OnAttack");
      doorInteract = compound.getInt("DoorInteract");
      findShelter = compound.getInt("FindShelter");
      canLeap = compound.getBoolean("CanLeap");
      canSprint = compound.getBoolean("CanSprint");
      movingPause = compound.getBoolean("MovingPause");
      npcInteracting = compound.getBoolean("npcInteracting");
      stopAndInteract = compound.getBoolean("stopAndInteract");
      movementType = compound.getInt("MovementType");
      animationType = compound.getInt("MoveState");
      standingType = compound.getInt("StandingState");
      movingType = compound.getInt("MovingState");
      orientation = compound.getInt("Orientation");
      bodyOffsetY = compound.getFloat("PositionOffsetY");
      bodyOffsetZ = compound.getFloat("PositionOffsetZ");
      bodyOffsetX = compound.getFloat("PositionOffsetX");
      walkingRange = compound.getInt("WalkingRange");
      setWalkingSpeed(compound.getInt("MoveSpeed"));
      setMovingPath(NBTTags.getIntegerArraySet(compound.getList("MovingPathNew", 10)));
      movingPos = compound.getInt("MovingPos");
      movingPattern = compound.getInt("MovingPatern");
      attackInvisible = compound.getBoolean("AttackInvisible");
      if (compound.contains("StartPosNew")) {
         int[] startPos = compound.getIntArray("StartPosNew");
         setStartPos(new BlockPos(startPos[0], startPos[1], startPos[2]));
      }
      mountControl = compound.getBoolean("MountControl");

      // in 1.12.2
      if (compound.contains("TacticalRadius", 3)) { tacticalRadius = compound.getInt("TacticalRadius"); }
      setTacticalType(compound.getInt("TacticalVariant"));

      // New fields from Unofficial (BetaZavr)
      if (compound.contains("CanBeCollide", 1)) { canBeCollide = compound.getBoolean("CanBeCollide"); }
      if (compound.contains("StepHeight", 5)) { stepheight = compound.getFloat("StepHeight"); }
      npc.setMaxUpStep(stepheight);
      if (compound.contains("ActiveRange", 3)) { activeRange = compound.getInt("ActiveRange"); }
      if (compound.contains("MaxHurtResistantTime", 3)) { maxHurtResistantTime = compound.getInt("MaxHurtResistantTime"); }
      npc.hurtDuration = maxHurtResistantTime;
      aiDisabled = compound.getBoolean("AIDisabled");
      if (compound.contains("DirectLOS", 1)) {
         directLOS = compound.getBoolean("DirectLOS") ? EnumSeeTarget.NORMAL : EnumSeeTarget.NONE;
      } // OLD
      else { directLOS = EnumSeeTarget.values()[ValueUtil.onlyPositiveInt(compound.getInt("DirectLOS"), EnumSeeTarget.values().length - 1)]; }
   }

   public CompoundTag save(CompoundTag compound) {
      compound.putBoolean("CanSwim", canSwim);
      compound.putBoolean("ReactsToFire", reactsToFire);
      compound.putBoolean("AvoidsWater", avoidsWater);
      compound.putBoolean("AvoidsSun", avoidsSun);
      compound.putBoolean("ReturnToStart", returnToStart);
      compound.putInt("OnAttack", onAttack);
      compound.putInt("DoorInteract", doorInteract);
      compound.putInt("FindShelter", findShelter);
      compound.putBoolean("CanLeap", canLeap);
      compound.putBoolean("CanSprint", canSprint);
      compound.putBoolean("MovingPause", movingPause);
      compound.putBoolean("npcInteracting", npcInteracting);
      compound.putBoolean("stopAndInteract", stopAndInteract);
      compound.putInt("MoveState", animationType);
      compound.putInt("StandingState", standingType);
      compound.putInt("MovingState", movingType);
      compound.putInt("MovementType", movementType);
      compound.putInt("Orientation", orientation);
      compound.putFloat("PositionOffsetX", bodyOffsetX);
      compound.putFloat("PositionOffsetY", bodyOffsetY);
      compound.putFloat("PositionOffsetZ", bodyOffsetZ);
      compound.putInt("WalkingRange", walkingRange);
      compound.putInt("MoveSpeed", moveSpeed);
      compound.put("MovingPathNew", NBTTags.nbtIntegerArraySet(movingPath));
      compound.putInt("MovingPos", movingPos);
      compound.putInt("MovingPatern", movingPattern);
      setAvoidsWater(avoidsWater);
      compound.putIntArray("StartPosNew", getStartArray());
      compound.putBoolean("AttackInvisible", attackInvisible);
      compound.putBoolean("MountControl", mountControl);

      // in 1.12.2
      compound.putInt("TacticalRadius", tacticalRadius);
      compound.putInt("TacticalVariant", tacticalVariant.ordinal());

      // New fields from Unofficial (BetaZavr)
      compound.putBoolean("CanBeCollide", canBeCollide);
      compound.putInt("ActiveRange", activeRange);
      compound.putInt("MaxHurtResistantTime", maxHurtResistantTime);
      compound.putBoolean("AIDisabled", aiDisabled);
      compound.putInt("DirectLOS", directLOS.ordinal());
      return compound;
   }

   public List<int[]> getMovingPath() {
      if (movingPath.isEmpty() && startPos != null) { movingPath.add(getStartArray()); }
      return movingPath;
   }

   public void setMovingPath(List<int[]> list) {
      movingPath = list;
      if (!movingPath.isEmpty()) {
         int[] startPos = movingPath.get(0);
         setStartPos(new BlockPos(startPos[0], startPos[1], startPos[2]));
      }
   }

   public BlockPos startPos() {
      if (startPos == null || startPos == BlockPos.ZERO) { setStartPos(npc.blockPosition()); }
      return startPos;
   }

   public int[] getStartArray() {
      BlockPos pos = startPos();
      return new int[]{pos.getX(), pos.getY(), pos.getZ()};
   }

   public int[] getCurrentMovingPath() {
      List<int[]> list = getMovingPath();
      int size = list.size();
      if (size == 1) { return list.get(0); }
      int pos = movingPos;
      if (movingPattern == 0 && pos >= size) { pos = movingPos = 0; }
      else if (movingPattern == 1) {
         int size2 = size * 2 - 1;
         if (pos >= size2) { pos = movingPos = 0; }
         else if (pos >= size) { pos = size2 - pos; }
      }
      return list.get(pos);
   }

   @SuppressWarnings("unused")
   public void clearMovingPath() {
      movingPath.clear();
      movingPos = 0;
   }

   @SuppressWarnings("unused")
   public void setMovingPathPos(int m_pos, int[] pos) {
      if (m_pos < 0) { m_pos = 0; }
      movingPath.set(m_pos, pos);
   }

   @SuppressWarnings("unused")
   public int[] getMovingPathPos(int m_pos) { return movingPath.get(m_pos); }

   @SuppressWarnings("unused")
   public void appendMovingPath(int[] pos) { movingPath.add(pos); }

   @SuppressWarnings("unused")
   public int getMovingPos() { return movingPos; }

   @SuppressWarnings("unused")
   public void setMovingPos(int pos) { movingPos = pos; }

   @SuppressWarnings("unused")
   public int getMovingPathSize() { return movingPath.size(); }

   public void incrementMovingPath() {
      List<int[]> list = getMovingPath();
      if (list.size() == 1) { movingPos = 0; }
      else {
         ++movingPos;
         if (movingPattern == 0) { movingPos %= list.size(); }
         else if (movingPattern == 1) {
            int size = list.size() * 2 - 1;
            movingPos %= size;
         }
      }
   }

   public void decreaseMovingPath() {
      List<int[]> list = getMovingPath();
      if (list.size() == 1) { movingPos = 0; }
      else {
         --movingPos;
         if (movingPos < 0) {
            if (movingPattern == 0) { movingPos = list.size() - 1; }
            else if (movingPattern == 1) { movingPos = list.size() * 2 - 2; }
         }
      }
   }

   public double distanceToSqrToPathPoint() {
      int[] pos = getCurrentMovingPath();
      return npc.distanceToSqr(pos[0] + 0.5D, pos[1], pos[2] + 0.5D);
   }

   public IPos getStartPos() { return new BlockPosWrapper(npc == null ? null : npc.level(), startPos()); }

   public void setStartPos(BlockPos pos) {
      startPos = pos;
      npc.restrictTo(startPos, Math.max(npc.stats.aggroRange * 2, CustomNpcs.NpcNavRange * 2));
   }

   public void setStartPos(IPos pos) { setStartPos(pos.getMCBlockPos()); }

   public void setStartPos(double x, double y, double z) { setStartPos(new BlockPos((int)x, (int)y, (int)z)); }

   @Override
   public void setReturnsHome(boolean bo) { returnToStart = bo; }

   @Override
   public boolean getReturnsHome() { return returnToStart; }

   public boolean shouldReturnHome() {
      return (!(npc.job instanceof JobBuilder jobB) || !jobB.isBuilding()) &&
              (!(npc.job instanceof JobFarmer jobF) || !jobF.isPlucking()) &&
              returnToStart;
   }

   @Override
   public int getAnimation() { return animationType; }

   @Override
   public int getCurrentAnimation() { return npc.currentAnimation; }

   @Override
   public void setAnimation(int type) { animationType = type; }

   /**
    * @return 0:Normal, 1:Panic, 2:Retreat, 3:Nothing
    */
   @Override
   public int getRetaliateType() { return onAttack; }

   @Override
   public void setRetaliateType(int type) {
      if (type < 0 || type > 3) { throw new CustomNPCsException("Unknown retaliation type: " + type); }
      onAttack = type;
      npc.updateAI = true;
   }

   /**
    * @return
    * 		0: Standing
    * 		1: Wandering
    * 		2: MovingPath -> EntityAIMovingPath
    */
   @Override
   public int getMovingType() { return movingType; }

   @Override
   public void setMovingType(int type) {
      if (type < 0 || type > 2) { throw new CustomNPCsException("Unknown moving type: " + type); }
      movingType = type;
      npc.updateAI = true;
   }

   /**
    * 0:NoRotation, 1:RotateBody, 2:Stalking, 3:HeadRotation, 4:EyeRotation
    */
   @Override
   public int getStandingType() { return standingType; }

   @Override
   public void setStandingType(int type) {
      if (type < 0 || type > 3) { throw new CustomNPCsException("Unknown standing type: " + type); }
      standingType = type;
      npc.updateAI = true;
   }

   @Override
   public boolean getAttackInvisible() { return attackInvisible; }

   @Override
   public void setAttackInvisible(boolean attack) { attackInvisible = attack; }

   @Override
   public int getWanderingRange() { return walkingRange; }

   @Override
   public void setWanderingRange(int range) {
      if (range < 1 || range > 50) { throw new CustomNPCsException("Bad wandering range: " + range); }
      walkingRange = range;
      npc.updateAI = true;
   }

   @Override
   public boolean getInteractWithNPCs() { return npcInteracting; }

   @Override
   public void setInteractWithNPCs(boolean interact) { npcInteracting = interact; }

   @Override
   public boolean getStopOnInteract() { return stopAndInteract; }

   @Override
   public void setStopOnInteract(boolean stopOnInteract) { stopAndInteract = stopOnInteract; }

   @Override
   public int getWalkingSpeed() { return moveSpeed; }

   @Override
   public void setWalkingSpeed(int speed) {
      if (speed < 0 || speed > 100) { throw new CustomNPCsException("Wrong speed: " + speed); }
      moveSpeed = speed;
      Objects.requireNonNull(npc.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(npc.getSpeed());
      Objects.requireNonNull(npc.getAttribute(ForgeMod.SWIM_SPEED.get())).setBaseValue(npc.getSpeed() * 32.0F);
      Objects.requireNonNull(npc.getAttribute(Attributes.FLYING_SPEED)).setBaseValue(npc.getSpeed() * 2.0F);
   }

   @Override
   public int getMovingPathType() { return movingPattern; }

   @Override
   public boolean getMovingPathPauses() { return movingPause; }

   @Override
   public void setMovingPathType(int type, boolean pauses) {
      if (type != 0 && type != 1) { throw new CustomNPCsException("Moving path type: " + type); }
      movingPattern = type;
      movingPause = pauses;
   }

   @Override
   public int getDoorInteract() { return doorInteract; }

   @Override
   public void setDoorInteract(int type) {
      doorInteract = type;
      npc.updateAI = true;
   }

   @Override
   public boolean getCanSwim() { return canSwim; }

   @Override
   public void setCanSwim(boolean canSwimIn) { canSwim = canSwimIn; }

   /**
    * 0:Night, 1:Day, 2:Disable
    */
   @Override
   public int getSheltersFrom() { return findShelter; }

   @Override
   public void setSheltersFrom(int type) {
      findShelter = type;
      npc.updateAI = true;
   }

   @Override
   public boolean getAvoidsWater() { return avoidsWater; }

   @Override
   public void setAvoidsWater(boolean enabled) {
      npc.setPathfindingMalus(BlockPathTypes.WATER, movementType != 2 && enabled ? -1.0F : 0.0F);
      avoidsWater = enabled;
   }

   @Override
   public boolean getLeapAtTarget() { return canLeap; }

   @Override
   public void setLeapAtTarget(boolean leap) {
      canLeap = leap;
      npc.updateAI = true;
   }

   /**
    * @return 0:Ground, 1:Flying, 2:Swimming
    */
   @Override
   public int getNavigationType() { return movementType; }

   @Override
   public void setNavigationType(int type) { movementType = type; }

   @Override
   public void setMountControl(boolean enabled) { mountControl = enabled; }

   @Override
   public boolean isAIDisabled() { return aiDisabled; }

   @Override
   public void setIsAIDisabled(boolean bo) { aiDisabled = bo; }

   @Override
   public float getOffsetX() { return bodyOffsetX; }

   @Override
   public float getOffsetY() { return bodyOffsetY; }

   @Override
   public float getOffsetZ() { return bodyOffsetZ; }

   @Override
   public void setOffset(float x, float y, float z) {
      bodyOffsetX = ValueUtil.correctFloat(x, 0.0f, 9.99f);
      bodyOffsetY = ValueUtil.correctFloat(y, 0.0f, 9.99f);
      bodyOffsetZ = ValueUtil.correctFloat(z, 0.0f, 9.99f);
      npc.updateClient = true;
   }

   @Override
   public int getMaxHurtResistantTime() { return maxHurtResistantTime; }

   @Override
   public void setMaxHurtResistantTime(int ticks) {
      if (ticks < 0) { ticks *= -1; }
      if (ticks > 1200) { ticks = 1200; }
      maxHurtResistantTime = ticks;
   }

   // in 1.12.2
   @Override
   public int getTacticalRange() { return tacticalRadius; }

   @Override
   public void setTacticalRange(int range) { tacticalRadius = range; }

   @Override
   public int getTacticalType() { return tacticalVariant.ordinal(); }

   @Override
   public void setTacticalType(int type) {
      tacticalVariant = EnumNpcTactics.values()[ValueUtil.onlyPositiveInt(type, EnumNpcTactics.values().length - 1)];
      npc.updateAI = true;
   }

   // New from Unofficial (BetaZavr)
   @Override
   public EnumSeeTarget getAttackLOS() { return directLOS; }

   @Override
   public void setAttackLOS(int type) {
      directLOS = EnumSeeTarget.values()[ValueUtil.onlyPositiveInt(type, EnumSeeTarget.values().length - 1)];
      npc.updateAI = true;
   }

   @Override
   public boolean canBeCollide() { return canBeCollide; }

   @Override
   public void setCanBeCollide(boolean bo) {
      canBeCollide = bo;
      npc.updateAI = true;
   }

}
