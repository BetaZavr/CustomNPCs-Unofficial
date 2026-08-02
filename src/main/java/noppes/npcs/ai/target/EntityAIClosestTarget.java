package noppes.npcs.ai.target;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.monster.EntityMob;
import noppes.npcs.constants.EnumSeeTarget;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIClosestTarget extends EntityAITarget {

	private final EntityNPCInterface npc;
	private final int targetChance;
	private EntityLivingBase targetEntity;
	private final Predicate<EntityLivingBase> targetEntitySelector;
	private final EntityAINearestAttackableTarget.Sorter theNearestAttackableTargetSorter;

	public EntityAIClosestTarget(EntityNPCInterface npcIn, int targetChanceIn, EnumSeeTarget checkSight, boolean onlyNearby, Predicate<EntityLivingBase> attackEntitySelector) {
		super(npcIn, checkSight != EnumSeeTarget.NONE && checkSight != EnumSeeTarget.BLIND, onlyNearby);
		npc = npcIn;
		targetChance = targetChanceIn;
		theNearestAttackableTargetSorter = new EntityAINearestAttackableTarget.Sorter(npcIn);
        setMutexBits(1);
		targetEntitySelector = attackEntitySelector;
	}

	@Override
	public boolean shouldExecute() {
		if (npc.isKilled() || npc.getAttackTarget() != null) { return false; }
		if (targetChance > 0 && taskOwner.getRNG().nextInt(targetChance) != 0) { return false; }
		double dist = Math.max(getTargetDistance(), npc.stats.aggroRange);
		List<EntityLivingBase> list = new ArrayList<>(taskOwner.world.getEntitiesWithinAABB(EntityLivingBase.class,
				taskOwner.getEntityBoundingBox().grow(dist, 4.0d, dist), targetEntitySelector));
		if (list.isEmpty()) { return false; }
		list.sort(theNearestAttackableTargetSorter);
		targetEntity = list.get(0);
		return true;
	}

	@Override
	public void startExecuting() {
		taskOwner.setAttackTarget(targetEntity);
		if (targetEntity instanceof EntityMob && ((EntityMob) targetEntity).getAttackTarget() == null) {
			((EntityMob) targetEntity).setAttackTarget(taskOwner);
		}
		super.startExecuting();
	}

	@Override
	public void resetTask() {
		targetEntity = null;
		super.resetTask();
	}

}
