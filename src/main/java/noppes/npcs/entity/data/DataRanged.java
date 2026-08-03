package noppes.npcs.entity.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import noppes.npcs.api.entity.data.INPCRanged;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.ValueUtil;

public class DataRanged implements INPCRanged {

	private static final int version = 1;

	private boolean aimWhileShooting = false;
	private boolean pGlows = false;
	private boolean pPhysics = true;
	private boolean pRender3D = true;
	private boolean pSpin = false;
	private boolean pStick = false;
	private boolean pXlr8 = false;
	private int accuracy = 60;
	private int burstCount = 1;
	private int canFireIndirect = 0;
	private int fireRate = 5;
	private int maxDelay = 40;
	private int meleeDistance = 0;
	private int minDelay = 20;
	private int pImpact = 0; // knockback
	private int pArea = 0;
	private int pDamage = 4;
	private int pDur = 5;
	private int pEffAmp = 0;
	private int pEffect = 0;
	private int pSize = 5;
	private int pSpeed = 10;
	private int pTrail = 0;
	private int shotCount = 1;
	private double rangedRange = 15.0d;
	private String fireSound = "minecraft:entity.arrow.shoot";
	private String groundSound = "minecraft:block.stone.break";
	private String hitSound = "minecraft:entity.arrow.hit";
	private final EntityNPCInterface npc;

	public DataRanged(EntityNPCInterface npc) {
		this.npc = npc;
	}

	@Override
	public boolean getAccelerate() {
		return this.pXlr8;
	}

	@Override
	public int getAccuracy() {
		return this.accuracy;
	}

	@Override
	public int getBurst() {
		return this.burstCount;
	}

	@Override
	public int getBurstDelay() {
		return this.fireRate;
	}

	@Override
	public int getDelayMax() {
		return this.maxDelay;
	}

	@Override
	public int getDelayMin() {
		return this.minDelay;
	}

	@Override
	public int getDelayRNG() {
		int delay = this.minDelay;
		if (this.maxDelay - this.minDelay > 0) {
			delay += this.npc.world.rand.nextInt(this.maxDelay - this.minDelay);
		}
		return delay;
	}

	@Override
	public int getEffectStrength() {
		return this.pEffAmp;
	}

	@Override
	public int getEffectTime() {
		return this.pDur;
	}

	@Override
	public int getEffectType() {
		return this.pEffect;
	}

	@Override
	public int getExplodeSize() {
		return this.pArea;
	}

	@Override
	public int getFireType() {
		return this.canFireIndirect;
	}

	@Override
	public boolean getGlows() {
		return this.pGlows;
	}

	@Override
	public boolean getHasAimAnimation() {
		return this.aimWhileShooting;
	}

	@Override
	public boolean getHasGravity() {
		return this.pPhysics;
	}

	@Override
	public int getKnockback() {
		return this.pImpact;
	}

	@Override
	public int getMeleeRange() {
		return this.meleeDistance;
	}

	@Override
	public int getParticle() {
		return this.pTrail;
	}

	@Override
	public double getRange() {
		return this.rangedRange;
	}

	@Override
	public boolean getRender3D() {
		return this.pRender3D;
	}

	@Override
	public int getShotCount() {
		return this.shotCount;
	}

	@Override
	public int getSize() {
		return this.pSize;
	}

	@Override
	public String getSound(int type) {
		switch (type) {
		case 0:
			return this.fireSound;
		case 1:
			return this.hitSound;
		case 2:
			return this.groundSound;
		}
		return null;
	}

	public SoundEvent getSoundEvent(int type) {
		String sound = this.getSound(type);
		if (sound == null || sound.isEmpty()) {
			return null;
		}
		return SoundEvent.REGISTRY.getObject(new ResourceLocation(sound));
	}

	@Override
	public int getSpeed() {
		return this.pSpeed;
	}

	@Override
	public boolean getSpins() {
		return this.pSpin;
	}

	@Override
	public boolean getSticks() {
		return this.pStick;
	}

	@Override
	public int getStrength() {
		return this.pDamage;
	}

	public void readFromNBT(NBTTagCompound compound) {
		this.pDamage = compound.getInteger("pDamage");
		this.pSpeed = compound.getInteger("pSpeed");
		this.burstCount = compound.getInteger("BurstCount");
		this.pImpact = compound.getInteger("pImpact");
		if (version != compound.getInteger("version")) {
			int v = compound.getInteger("version");
			if (v < 1) { pImpact++; }
		}
		this.pSize = compound.getInteger("pSize");
		this.pArea = compound.getInteger("pArea");
		this.pTrail = compound.getInteger("pTrail");
		this.fireRate = compound.getInteger("FireRate");
		this.minDelay = ValueUtil.correctInt(compound.getInteger("minDelay"), 1, 9999);
		this.maxDelay = ValueUtil.correctInt(compound.getInteger("maxDelay"), 1, 9999);
		this.shotCount = ValueUtil.correctInt(compound.getInteger("ShotCount"), 1, 10);
		this.accuracy = compound.getInteger("Accuracy");
		this.pRender3D = compound.getBoolean("pRender3D");
		this.pSpin = compound.getBoolean("pSpin");
		this.pStick = compound.getBoolean("pStick");
		this.pPhysics = compound.getBoolean("pPhysics");
		this.pXlr8 = compound.getBoolean("pXlr8");
		this.pGlows = compound.getBoolean("pGlows");
		this.aimWhileShooting = compound.getBoolean("AimWhileShooting");
		this.pEffect = compound.getInteger("pEffect");
		this.pDur = compound.getInteger("pDur");
		this.pEffAmp = compound.getInteger("pEffAmp");
		this.fireSound = compound.getString("FiringSound");
		this.hitSound = compound.getString("HitSound");
		this.groundSound = compound.getString("GroundSound");
		this.canFireIndirect = compound.getInteger("FireIndirect");
		this.meleeDistance = compound.getInteger("DistanceToMelee");
		// New from Unofficial (BetaZavr)
		if (compound.hasKey("MaxFiringRange", 3)) { rangedRange = compound.getInteger("MaxFiringRange"); }
		else { rangedRange = compound.getDouble("MaxFiringRange"); }
		rangedRange = ValueUtil.correctDouble(rangedRange, 1.0d, 64.0d);
	}

	@Override
	public void setAccelerate(boolean accelerate) {
		this.pXlr8 = accelerate;
	}

	@Override
	public void setAccuracy(int accuracy) {
		this.accuracy = ValueUtil.correctInt(accuracy, 1, 100);
	}

	@Override
	public void setBurst(int count) {
		this.burstCount = count;
	}

	@Override
	public void setBurstDelay(int delay) {
		this.fireRate = delay;
	}

	@Override
	public void setDelay(int min, int max) {
		min = Math.min(min, max);
		this.minDelay = min;
		this.maxDelay = max;
	}

	@Override
	public void setEffect(int type, int strength, int time) {
		this.pEffect = type;
		this.pDur = time;
		this.pEffAmp = strength;
	}

	@Override
	public void setExplodeSize(int size) {
		this.pArea = size;
	}

	@Override
	public void setFireType(int type) {
		this.canFireIndirect = type;
	}

	@Override
	public void setGlows(boolean glows) {
		this.pGlows = glows;
	}

	@Override
	public void setHasAimAnimation(boolean aim) {
		this.aimWhileShooting = aim;
	}

	@Override
	public void setHasGravity(boolean hasGravity) {
		this.pPhysics = hasGravity;
	}

	@Override
	public void setKnockback(int punch) {
		this.pImpact = punch;
	}

	@Override
	public void setMeleeRange(int range) {
		this.meleeDistance = range;
		this.npc.updateAI = true;
	}

	@Override
	public void setParticle(int type) {
		this.pTrail = type;
	}

	@Override
	public void setRange(double range) {
		rangedRange = ValueUtil.correctDouble(range, 1.0d, 64.0d);
	}

	@Override
	public void setRender3D(boolean render3d) {
		this.pRender3D = render3d;
	}

	@Override
	public void setShotCount(int count) {
		this.shotCount = count;
	}

	@Override
	public void setSize(int size) {
		this.pSize = size;
	}

	@Override
	public void setSound(int type, String sound) {
		if (sound == null) {
			sound = "";
		}
		if (type == 0) {
			this.fireSound = sound;
		}
		if (type == 1) {
			this.hitSound = sound;
		}
		if (type == 2) {
			this.groundSound = sound;
		}
		this.npc.updateClient = true;
	}

	@Override
	public void setSpeed(int speed) {
		this.pSpeed = ValueUtil.correctInt(speed, 0, 100);
	}

	@Override
	public void setSpins(boolean spins) {
		this.pSpin = spins;
	}

	@Override
	public void setSticks(boolean sticks) {
		this.pStick = sticks;
	}

	@Override
	public void setStrength(int strength) {
		this.pDamage = strength;
	}

	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setInteger("BurstCount", this.burstCount);
		compound.setInteger("pSpeed", this.pSpeed);
		compound.setInteger("pDamage", this.pDamage);
		compound.setInteger("pImpact", this.pImpact);
		compound.setInteger("pSize", this.pSize);
		compound.setInteger("pArea", this.pArea);
		compound.setInteger("pTrail", this.pTrail);
		compound.setDouble("MaxFiringRange", this.rangedRange);
		compound.setInteger("FireRate", this.fireRate);
		compound.setInteger("minDelay", this.minDelay);
		compound.setInteger("maxDelay", this.maxDelay);
		compound.setInteger("ShotCount", this.shotCount);
		compound.setInteger("Accuracy", this.accuracy);
		compound.setBoolean("pRender3D", this.pRender3D);
		compound.setBoolean("pSpin", this.pSpin);
		compound.setBoolean("pStick", this.pStick);
		compound.setBoolean("pPhysics", this.pPhysics);
		compound.setBoolean("pXlr8", this.pXlr8);
		compound.setBoolean("pGlows", this.pGlows);
		compound.setBoolean("AimWhileShooting", this.aimWhileShooting);
		compound.setInteger("pEffect", this.pEffect);
		compound.setInteger("pDur", this.pDur);
		compound.setInteger("pEffAmp", this.pEffAmp);
		compound.setString("FiringSound", this.fireSound);
		compound.setString("HitSound", this.hitSound);
		compound.setString("GroundSound", this.groundSound);
		compound.setInteger("FireIndirect", this.canFireIndirect);
		compound.setInteger("DistanceToMelee", this.meleeDistance);
		compound.setInteger("version", version);
		return compound;
	}
}
