package noppes.npcs.ai.attack;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.EnumHand;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.entity.ai.IEntityAITasksMixin;

public abstract class EntityAICustom extends EntityAIBase {

	protected final EntityNPCInterface npc;
	protected final int tickRate;
	protected EntityLivingBase target;

	public boolean hasAttack;
	public boolean startRangedAttack;
	public boolean isRanged;
	public boolean canSeeToAttack;
	public boolean inMove;
	public boolean isFriend;

	protected int burstCount;
	protected int tacticalRange;
	protected int rangedTick;
	protected int meleeTick;
	protected int step;

	public double distance;
	public double range;

	public EntityAICustom(EntityNPCInterface npcIn) {
		npc = npcIn;
		tickRate = ((IEntityAITasksMixin) npc.tasks).getTickRate();
		step = 0;
		distance = -1.0d;
		setMutexBits(AiMutex.PATHING);
	}

	public EntityAICustom(IRangedAttackMob npcIn) {
		if (!(npcIn instanceof EntityNPCInterface)) {
			throw new IllegalArgumentException("ArrowAttackGoal requires Mob implements RangedAttackMob");
		}
		npc = (EntityNPCInterface) npcIn;
		tickRate = ((IEntityAITasksMixin) npc.tasks).getTickRate();
		distance = -1.0d;
		setMutexBits(AiMutex.PATHING);
	}

	public EntityLivingBase getTarget() { return target; }

	/**
	 * resets this AI's work when "shouldContinueExecuting" returns "false"
	 */
	@Override
	public void resetTask() {
		canSeeToAttack = false;
		npc.updateHitbox();
	}

	/**
	 * checks whether this AI can continue to execute -> updateTask
	 */
	@Override
	public boolean shouldContinueExecuting() { return npc != null && npc.isEntityAlive() && setTarget(); }

	private boolean setTarget() {
		target = npc.getAttackTarget();
		if (npc.aiOwnerNPC != null && npc.aiOwnerNPC.isEntityAlive()) {
			EntityLivingBase ownerTarget = npc.aiOwnerNPC.getAttackTarget();
			if (ownerTarget != null && ownerTarget.equals(target)) {
				npc.setAttackTarget(ownerTarget);
			}
			target = npc.getAttackTarget();
		}
		if (target == null || !target.isEntityAlive()) {
			startRangedAttack = false;
			return false;
		}
		// target is GM Player reset in EntityNPCInterface.onUpdate()
		isFriend = npc.isFriend(target);
		return target != null;
	}

	/**
	 * checks the possibility of running this AI
	 */
	@Override
	public boolean shouldExecute() {
		distance = -1.0d;
		canSeeToAttack = false;
		hasAttack = false;
		setTarget();
		return setTarget();
	}

	protected void tryMoveToTarget() {
		if (!CustomNpcs.ShowCustomAnimation || !npc.animation.isAnimated(AnimationKind.INIT, AnimationKind.DIES)) {
			double baseSpeed = npc.ais.canSprint ? 1.5d : 1.3d;
			if (target.equals(npc.combatHandler.priorityTarget)) { baseSpeed = npc.ais.canSprint ? 1.6d : 1.4d; }
			double dist = npc.getDistance(target.posX, target.posY, target.posZ);
			double speed = (0.75d / (double) npc.stats.aggroRange * dist + 0.5d) * baseSpeed;
			if (speed < 1.3d) { speed = 1.3d; }
			else if (speed > baseSpeed) { speed = baseSpeed; }
			npc.getNavigator().tryMoveToEntityLiving(target, speed);
		}
	}

	protected void tryToCauseDamage() {
		if (isRanged) {
			if (rangedTick > 0 || distance > range || (!canSeeToAttack && npc.stats.ranged.getFireType() != 2)) {
				if (rangedTick == 0 && !canSeeToAttack) { rangedTick = 5; }
				startRangedAttack = false;
				return;
			}
			startRangedAttack = true;
			return;
		}
		if (meleeTick > 0 || distance > range || !canSeeToAttack) {
			if (meleeTick == 0 && !canSeeToAttack) { meleeTick = 5; }
			return;
		}
		meleeTick = npc.stats.melee.getDelayRNG();
		npc.swingArm(EnumHand.MAIN_HAND);
		npc.attackEntityAsMob(target);
		attacked();
		hasAttack = true;
	}

	public void update() {
		if (!startRangedAttack || target == null || !target.isEntityAlive() || !npc.isEntityAlive()) {
			startRangedAttack = false;
			//step = 0; burstCount = 0;
			return;
		}
		step++;
		if (step >= tickRate) { step = 0; }
		if (rangedTick > step) { return; }

		if (burstCount++ <= npc.stats.ranged.getBurst()) { rangedTick = npc.stats.ranged.getBurstDelay(); }
		else {
			burstCount = 0;
			hasAttack = true;
			rangedTick = npc.stats.ranged.getDelayRNG();
		}
		if (burstCount > 1) {
			boolean indirect = false;
			switch (npc.stats.ranged.getFireType()) {
				case 1: {
					indirect = (distance < range / 2.0);
					break;
				}
				case 2: {
					indirect = !npc.getEntitySenses().canSee(target);
					break;
				}
			}
			npc.attackEntityWithRangedAttack(target, indirect ? 1.0f : 0.0f);
			attacked();
			if (npc.currentAnimation != 6) { npc.swingArm(EnumHand.MAIN_HAND); }
			step = 0;
		}
	}

	/**
	 * will run every tick until "shouldContinueExecuting" returns "true"
	 */
	@Override
	public void updateTask() {
		if (target != null && (!CustomNpcs.ShowCustomAnimation
				|| !npc.animation.isAnimated(AnimationKind.ATTACKING, AnimationKind.INIT, AnimationKind.INTERACT, AnimationKind.DIES))) {
			npc.getLookHelper().setLookPositionWithEntity(target, 30.0f, npc.getVerticalFaceSpeed());
		}
		inMove = !npc.getNavigator().noPath();
		tacticalRange = npc.ais.getTacticalRange();
		distance = npc.getDistance(target.posX, target.getEntityBoundingBox().minY, target.posZ);
		isRanged = npc.inventory.getProjectile() != null && (npc.stats.ranged.getMeleeRange() <= 0 || distance > npc.stats.ranged.getMeleeRange());
		if (isRanged) {
			rangedTick--;
			range = npc.stats.ranged.getRange();
			double reach = noppes.npcs.entity.EntityProjectile.maxBallisticRange(npc.stats.ranged.getSpeed() / 10.0d, npc.getEyeHeight());
			if (reach > 0.0d && reach < range) { range = reach; }
		} else {
			meleeTick--;
			range = npc.stats.melee.getRange();
			double minRange = (npc.width + target.width) / 2.0d;
			if (minRange > range) {
				range = minRange;
			}
		}
	}

    public boolean canNewAttack() { return true; }

	public void attacked() { }

	public boolean damaged() { return false; }

}
