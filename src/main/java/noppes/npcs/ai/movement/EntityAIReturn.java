package noppes.npcs.ai.movement;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import noppes.npcs.CustomNpcs;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.pathfinding.IPathMixin;
import noppes.npcs.util.Util;

public class EntityAIReturn extends EntityAIBase {

	protected static final int MaxTotalTicks = 600;
	protected final EntityNPCInterface npc;
	protected boolean wasAttacked = false;
	protected int stuckTicks = 0;
	protected int totalTicks = 0;
	protected int stuckCount = 0;
	protected Vec3d endPos;
	protected Vec3d preAttackPos;

	public EntityAIReturn(EntityNPCInterface npcIn) {
		npc = npcIn;
		setMutexBits(AiMutex.PASSIVE);
	}

	@Override
	public void resetTask() {
		wasAttacked = false;
	}

	@Override
	public boolean shouldContinueExecuting() {
		boolean bo = true;
		if (npc.ais.onAttack == 2) {
			double dist = Util.instance.distanceTo(npc.posX, npc.posY, npc.posZ, npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos());
			bo = dist > npc.stats.aggroRange;
		}
		return npc.getHealth() > 0 && !npc.isFollower() && !npc.isKilled() && !npc.isAttacking() &&
				!npc.isVeryNearAssignedPlace() && !npc.isInteracting() && !npc.isRiding() &&
				(!npc.getNavigator().noPath() || !wasAttacked || isTooFar()) &&
				totalTicks <= MaxTotalTicks && bo;
	}

	@Override
	public boolean shouldExecute() {
		if (npc.emptyOwner() &&
				!npc.isRiding() &&
				npc.ais.shouldReturnHome() &&
				!npc.isKilled() &&
				npc.getNavigator().noPath() &&
				!npc.isInteracting()) {
			// AI Attack
			if (npc.aiOwnerNPC != null && !npc.getNavigator().noPath()) {
				totalTicks = 0;
				return false;
			}
			// AI Panic
			if (npc.ais.onAttack == 1) {
				if (npc.isBurning() || npc.getAttackTarget() != null) {
					totalTicks = 0;
					return false;
				}
			}
			BlockPos pos = new BlockPos((int) npc.getStartXPos(), (int) npc.getStartYPos(), (int) npc.getStartZPos());
			// Shelter at Night
			if (npc.ais.findShelter == 0 && (!npc.world.isDaytime() || npc.world.isRaining()) &&
					!npc.world.provider.hasSkyLight() && (npc.world.canSeeSky(pos) || npc.world.getLight(pos) <= 8)) { return false; }
			// Shelter at Day
			else if (npc.ais.findShelter == 1 && npc.world.isDaytime() && npc.world.canSeeSky(pos)) { return false; }
			if (npc.isAttacking()) {
				if (!wasAttacked) {
					wasAttacked = true;
					preAttackPos = new Vec3d(npc.posX, npc.posY, npc.posZ);
				}
				return isTooFar();
			}
			else if (wasAttacked) { return true; }
			else if (npc.homeDimensionId != npc.world.provider.getDimension()) { return true; }
			else if (npc.ais.getMovingType() == 2) { return npc.ais.getDistanceSqToPathPoint() >= (double) (CustomNpcs.NpcNavRange * CustomNpcs.NpcNavRange); }
			else if (npc.ais.getMovingType() == 1) { return !npc.isInRange(npc.getStartXPos(), -6666.0D, npc.getStartZPos(), npc.ais.walkingRange); }
			else if (npc.ais.getMovingType() == 0) { return !npc.isVeryNearAssignedPlace(); }
		}
		return false;
	}

	@Override
	public void startExecuting() {
		stuckTicks = 0;
		totalTicks = 0;
		stuckCount = 0;
		navigate(false);
	}

	@Override
	public void updateTask() {
		++totalTicks;
		if (!wasAttacked && finishApproach()) { return; }
		if (totalTicks > MaxTotalTicks) { tryBackHome(endPos); }
		else {
			if (stuckTicks > 0) { --stuckTicks; }
			else if (npc.getNavigator().noPath()) {
				++stuckCount;
				stuckTicks = 10;
				if ((totalTicks <= 30 || !wasAttacked || !isTooFar()) && stuckCount <= 5) { navigate(stuckCount % 2 == 1); }
				else { tryBackHome(endPos); }
			}
			else { stuckCount = 0; }
		}
	}

	private boolean finishApproach() {
		double dx = npc.getStartXPos() - npc.posX;
		double dz = npc.getStartZPos() - npc.posZ;
		double distSq = dx * dx + dz * dz;
		if (distSq > 2.25d) { return false; }
		npc.getNavigator().clearPath();
		if (distSq < 0.0025d) {
			npc.setPositionAndUpdate(npc.getStartXPos(), npc.posY, npc.getStartZPos());
			return true;
		}
		npc.getMoveHelper().setMoveTo(npc.getStartXPos(), npc.posY, npc.getStartZPos(), 1.0d);
		return true;
	}

	private boolean isTooFar() {
		if (endPos == null) { return false; }
		if (npc.homeDimensionId != npc.world.provider.getDimension()) { return true; }
		int allowedDistance = npc.stats.aggroRange * 2;
		if (npc.ais.getMovingType() == 1) { allowedDistance += npc.ais.walkingRange; }
		return Util.instance.distanceTo(npc.posX, npc.posY, npc.posZ, endPos.x, endPos.y, endPos.z) > allowedDistance;
	}

	private void navigate(boolean towards) {
		if (!wasAttacked) { endPos = new Vec3d(npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos()); }
		else { endPos = preAttackPos; }
		Vec3d pos = endPos;
		double range = npc.getDistance(pos.x, pos.y, pos.z);
		if (range > (double)CustomNpcs.NpcNavRange || towards) {
			int distance = (int)range;
			if (distance > CustomNpcs.NpcNavRange) { distance = CustomNpcs.NpcNavRange / 2; }
			else { distance /= 2; }
			if (distance > 2) {
				pos = RandomPositionGenerator.findRandomTargetBlockTowards(npc, distance, Math.min(distance / 2, 7), pos);
				if (pos == null) { pos = endPos; }
			}
		}
		tryBackHome(pos);
	}

	// New from Unofficial (BetaZavr)
	private void tryBackHome(Vec3d pos) {
		if (wasAttacked) { npc.setAttackTarget(null); }
		if (pos.equals(endPos) && npc.posX == pos.x && npc.posY == pos.y && npc.posZ == pos.z) {
			endPos = preAttackPos = pos = new Vec3d(npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos());
		}
		npc.getNavigator().clearPath();
		if (npc.homeDimensionId != this.npc.world.provider.getDimension()) {
			Util.instance.teleportEntity(npc.getServer(), npc, npc.homeDimensionId, npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos());
		}
		else {
			Path path = npc.getNavigator().getPathToXYZ(pos.x, pos.y, pos.z);
			npc.getNavigator().setPath(path, 2.0D);
			if (path == null || ((IPathMixin) path).getPoints().length == 1) {
				npc.setPosition(pos.x, pos.y, pos.z);
				npc.getNavigator().clearPath();
			}
		}
	}

}
