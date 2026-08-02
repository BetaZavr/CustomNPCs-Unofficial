package noppes.npcs.entity.data;

import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.data.INPCMelee;
import noppes.npcs.api.entity.data.INPCRanged;
import noppes.npcs.api.entity.data.INPCStats;
import noppes.npcs.constants.EnumCreatureRarity;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.ValueUtil;

public class DataStats implements INPCStats {

	public boolean burnInSun = false;
	public boolean canDrown = true;
	public boolean hideKilledBody = false;
	public boolean ignoreCobweb = false;
	public boolean immuneToFire = false;
	public boolean noFallDamage = false;
	public boolean potionImmune = false;
	public int aggroRange = 16;
	public int combatRegen = 0;
	public int healthRegen = 1;
	public int respawnTime = 20;
	public int spawnCycle = 0;
	private float chanceBlockDamage = 2.0f;
	public double maxHealth = 20.0d;
	public EnumCreatureAttribute creatureType = EnumCreatureAttribute.UNDEFINED;
	public DataMelee melee;
	private final EntityNPCInterface npc;
	public DataRanged ranged;
	public Resistances resistances = new Resistances();

	// New from Unofficial (BetaZavr)
	public boolean calmdown = true;
	private int level = 1;
	private String rarityTitle = ((char) 167) + "flv." + ((char) 167) + "21";
	private EnumCreatureRarity rarity = EnumCreatureRarity.NORMAL;

	public DataStats(EntityNPCInterface npcIn) {
		npc = npcIn;
		melee = new DataMelee(npc);
		ranged = new DataRanged(npc);
	}

	@Override
	public int getAggroRange() { return aggroRange; }

	@Override
	public int getCombatRegen() { return combatRegen; }

	@Override
	public int getCreatureType() { return creatureType.ordinal(); }

	@Override
	public int getHealthRegen() { return healthRegen; }

	@Override
	public boolean getHideDeadBody() { return hideKilledBody; }

	@Override
	public boolean getImmune(int type) {
		switch (type) {
			case 0: return potionImmune;
			case 1: return noFallDamage;
			case 2: return burnInSun;
			case 3: return immuneToFire;
			case 4: return canDrown;
			case 5: return ignoreCobweb;
		}
		throw new CustomNPCsException("Unknown immune type: " + type);
	}

	@Override
	public double getMaxHealth() { return maxHealth; }

	@Override
	public INPCMelee getMelee() { return melee; }

	@Override
	public INPCRanged getRanged() { return ranged; }

	@Override
	public int getRespawnTime() { return respawnTime; }

	@Override
	public int getRespawnType() { return spawnCycle; }

	public void load(NBTTagCompound compound) {
		if (compound.hasKey("MaxHealth", 3)) { setMaxHealth(compound.getInteger("MaxHealth")); } // Old
		else { setMaxHealth(compound.getDouble("MaxHealth")); }
		hideKilledBody = compound.getBoolean("HideBodyWhenKilled");
		aggroRange = compound.getInteger("AggroRange");
		respawnTime = compound.getInteger("RespawnTime");
		spawnCycle = compound.getInteger("SpawnCycle");
		creatureType = EnumCreatureAttribute.values()[compound.getInteger("CreatureType")];
		healthRegen = compound.getInteger("HealthRegen");
		combatRegen = compound.getInteger("CombatRegen");
		immuneToFire = compound.getBoolean("ImmuneToFire");
		potionImmune = compound.getBoolean("PotionImmune");
		canDrown = compound.getBoolean("CanDrown");
		burnInSun = compound.getBoolean("BurnInSun");
		noFallDamage = compound.getBoolean("NoFallDamage");
		npc.setImmuneToFire(immuneToFire);
		ignoreCobweb = compound.getBoolean("IgnoreCobweb");
		melee.load(compound);
		ranged.readFromNBT(compound);
		if (aggroRange < 1) { aggroRange = 1; }
		IAttributeInstance follow_range = npc.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        follow_range.setBaseValue(aggroRange);

		// New from Unofficial (BetaZavr)
		if (compound.hasKey("ChanceBlockDamage", 5)) { setChanceBlockDamage(compound.getFloat("ChanceBlockDamage")); }
		if (compound.hasKey("Resistances", 9)) { resistances.load(compound.getTagList("Resistances", 10)); }
		else { resistances.oldLoad(compound.getCompoundTag("Resistances")); }
		level = compound.getInteger("NPCLevel");
		rarity = EnumCreatureRarity.values()[compound.getInteger("NPCRarity")];
		rarityTitle = compound.getString("RarityTitle");
		if (compound.hasKey("CalmdownRange", 1)) { calmdown = compound.getBoolean("CalmdownRange"); }
    }

	@Override
	public void setAggroRange(int range) { aggroRange = range; }

	@Override
	public void setCombatRegen(int regen) { combatRegen = regen; }

	@Override
	public void setCreatureType(int type) { creatureType = EnumCreatureAttribute.values()[type]; }

	@Override
	public void setHealthRegen(int regen) { healthRegen = regen; }

	@Override
	public void setHideDeadBody(boolean hide) {
		hideKilledBody = hide;
		npc.updateClient = true;
	}

	@Override
	public void setImmune(int type, boolean bo) {
		if (type < 0 || type > 5) { throw new CustomNPCsException("Unknown immune type: " + type); }
		switch (type) {
			case 0: potionImmune = bo; break;
			case 1: noFallDamage = bo; break;
			case 2: burnInSun = bo; break;
			case 3: npc.setImmuneToFire(bo); break;
			case 4: canDrown = bo; break;
			case 5: ignoreCobweb = bo; break;
		}
	}

	@Override
	public void setMaxHealth(double maxHealthIn) {
		if (maxHealth != maxHealthIn) {
			maxHealth = maxHealthIn;
			npc.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(maxHealth);
			npc.updateClient = true;
		}
	}

	@Override
	public void setRespawnTime(int seconds) { respawnTime = seconds; }

	@Override
	public void setRespawnType(int type) { spawnCycle = type; }

	@Override
	public float getChanceBlockDamage() { return chanceBlockDamage; }

	@Override
	public void setChanceBlockDamage(float chance) {
		if (chance < 0.0f) { chance *= -1.0f; }
		if (chance > 100.0f) { chance = 100.0f; }
		chanceBlockDamage = chance;
	}

	public NBTTagCompound save(NBTTagCompound compound) {
		compound.setDouble("MaxHealth", maxHealth);
		compound.setInteger("AggroRange", aggroRange);
		compound.setBoolean("HideBodyWhenKilled", hideKilledBody);
		compound.setInteger("RespawnTime", respawnTime);
		compound.setInteger("SpawnCycle", spawnCycle);
		compound.setInteger("CreatureType", creatureType.ordinal());
		compound.setInteger("HealthRegen", healthRegen);
		compound.setInteger("CombatRegen", combatRegen);
		compound.setBoolean("ImmuneToFire", immuneToFire);
		compound.setBoolean("PotionImmune", potionImmune);
		compound.setBoolean("CanDrown", canDrown);
		compound.setBoolean("BurnInSun", burnInSun);
		compound.setBoolean("NoFallDamage", noFallDamage);
		compound.setBoolean("IgnoreCobweb", ignoreCobweb);
		melee.save(compound);
		ranged.writeToNBT(compound);

		// New from Unofficial (BetaZavr)
		compound.setFloat("ChanceBlockDamage", chanceBlockDamage);
		compound.setTag("Resistances", resistances.save());
		compound.setInteger("NPCLevel", level);
		compound.setInteger("NPCRarity", rarity.ordinal());
		compound.setString("RarityTitle", rarityTitle);
		compound.setBoolean("CalmdownRange", calmdown);
		return compound;
	}

	// New from Unofficial (BetaZavr)
	@Override
	public String[] getResistanceKeys() { return resistances.data.keySet().toArray(new String[0]); }

	@Override
	public float getResistance(String damageName) { return resistances.get(damageName); }

	@Override
	public void setResistance(String damageName, float value) { resistances.data.put(damageName, ValueUtil.correctFloat(value, 0.0f, 2.0f)); }

	@Override
	public int getLevel() { return level = ValueUtil.correctInt(level, 1, CustomNpcs.MaxLv); }

	@Override
	public void setLevel(int levelIn) { level = ValueUtil.correctInt(levelIn, 1, CustomNpcs.MaxLv); }

	@Override
	public int getRarity() { return rarity.ordinal(); }

	@Override
	public void setRarity(int rarityIn) {
		rarity = EnumCreatureRarity.values()[ValueUtil.correctInt(rarityIn, 0, EnumCreatureRarity.values().length - 1)];
		npc.updateClient = true;
	}

	@Override
	public String getRarityTitle() { return rarityTitle; }

	@Override
	public void setRarityTitle(String rarity) {
		if (rarityTitle.equals(rarity)) { return; }
		rarityTitle = rarity;
		npc.updateClient = true;
	}

	public double getHP() {
		int[] corr = CustomNpcs.HealthNormal;
		if (rarity == EnumCreatureRarity.ELITE) { corr = CustomNpcs.HealthElite; }
		else if (rarity == EnumCreatureRarity.BOSS) { corr = CustomNpcs.HealthBoss; }
		double a = ((double) corr[0] - (double) corr[1]) / (1 - Math.pow(CustomNpcs.MaxLv, 2));
		double b = (double) corr[0] - a;
		double hp = Math.round(a * Math.pow(level, 2) + b);
		if (hp <= 1.0d) { hp = 1.0d; }
		if (hp > 10000) { hp = Math.ceil(hp / 100.0d) * 100.0d; }
		else if (hp > 1000) { hp = Math.ceil(hp / 25.0d) * 25.0d; }
		else if (hp > 100) { hp = Math.ceil(hp / 10.0d) * 10.0d; }
		else if (hp > 50) { hp = Math.ceil(hp / 5.0d) * 5.0d; }
		else { hp = Math.ceil(hp); }
		if (hp > (double) corr[1]) { hp = corr[1]; }
		return hp;
	}

	@Override
	public boolean isCalmdown() { return calmdown; }

	@Override
	public void setCalmdown(boolean range) { calmdown = range; }

}
