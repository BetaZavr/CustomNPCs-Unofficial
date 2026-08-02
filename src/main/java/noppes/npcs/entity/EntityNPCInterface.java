package noppes.npcs.entity;

import java.lang.reflect.Method;
import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import io.netty.buffer.ByteBuf;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.network.internal.FMLMessage;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import noppes.npcs.*;
import noppes.npcs.ai.CombatHandler;
import noppes.npcs.ai.EntityAIAnimation;
import noppes.npcs.ai.EntityAIBustDoor;
import noppes.npcs.ai.EntityAIFindShade;
import noppes.npcs.ai.EntityAIJob;
import noppes.npcs.ai.movement.*;
import noppes.npcs.ai.movement.EntityAIFollow;
import noppes.npcs.ai.movement.EntityAIMoveIndoors;
import noppes.npcs.ai.movement.EntityAIWander;
import noppes.npcs.ai.target.EntityAILook;
import noppes.npcs.ai.EntityAIRole;
import noppes.npcs.ai.EntityAITransform;
import noppes.npcs.ai.EntityAIWorldLines;
import noppes.npcs.ai.FlyingMoveHelper;
import noppes.npcs.ai.attack.EntityAIAvoidTarget;
import noppes.npcs.ai.attack.EntityAICommanderTarget;
import noppes.npcs.ai.attack.EntityAICustom;
import noppes.npcs.ai.attack.EntityAIDodge;
import noppes.npcs.ai.attack.EntityAIHitAndRun;
import noppes.npcs.ai.attack.EntityAINoTactic;
import noppes.npcs.ai.attack.EntityAIOnslaught;
import noppes.npcs.ai.attack.EntityAIPounceTarget;
import noppes.npcs.ai.attack.EntityAIStalkTarget;
import noppes.npcs.ai.attack.EntityAISurround;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.ai.target.EntityAIClearTarget;
import noppes.npcs.ai.target.EntityAIClosestTarget;
import noppes.npcs.ai.target.EntityAIOwnerHurtByTarget;
import noppes.npcs.ai.target.EntityAIOwnerHurtTarget;
import noppes.npcs.ai.target.EntityAIWatchClosest;
import noppes.npcs.api.IChatMessages;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.constants.*;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IProjectile;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.api.util.IRayTraceResults;
import noppes.npcs.api.util.IRayTraceRotate;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.api.wrapper.NPCWrapper;
import noppes.npcs.api.wrapper.PlayerWrapper;
import noppes.npcs.api.wrapper.data.DataBlock;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.SkinUtil;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.client.model.animation.AnimationFrameConfig;
import noppes.npcs.client.model.part.ModelData;
import noppes.npcs.client.model.part.ModelPartConfig;
import noppes.npcs.constants.EnumAnimationStages;
import noppes.npcs.constants.EnumNPCAnimationType;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.constants.EnumSeeTarget;
import noppes.npcs.containers.NpcMiscInventory;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.LinkedNpcController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataAbilities;
import noppes.npcs.entity.data.DataAdvanced;
import noppes.npcs.entity.data.DataAnimation;
import noppes.npcs.entity.data.DataDisplay;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataScript;
import noppes.npcs.entity.data.DataStats;
import noppes.npcs.entity.data.DataTimers;
import noppes.npcs.items.ItemNpcMovingPath;
import noppes.npcs.items.ItemSoulstoneFilled;
import noppes.npcs.mixin.entity.IEntityLivingBaseMixin;
import noppes.npcs.mixin.world.IWorldMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.*;
import noppes.npcs.packets.server.SPacketNpcInitData;
import noppes.npcs.roles.*;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.*;
import noppes.npcs.util.Util;

public abstract class EntityNPCInterface
extends EntityCreature
implements IEntityAdditionalSpawnData, ICommandSender, IRangedAttackMob, IAnimals, INpc {

	public static FakePlayer ChatEventPlayer;
	public static FakePlayer CommandPlayer;
	public static FakePlayer GenericPlayer;
	public static GameProfileAlt ChatEventProfile = new GameProfileAlt();
	public static GameProfileAlt CommandProfile = new GameProfileAlt();
	public static GameProfileAlt GenericProfile = new GameProfileAlt();
	protected static DataParameter<Integer> Animation = EntityDataManager.createKey(EntityNPCInterface.class, DataSerializers.VARINT);
	public static final DataParameter<Boolean> Attacking = EntityDataManager.createKey(EntityNPCInterface.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Integer> FactionData = EntityDataManager.createKey(EntityNPCInterface.class, DataSerializers.VARINT);
	private static final DataParameter<Boolean> Interacting = EntityDataManager.createKey(EntityNPCInterface.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> IsDead = EntityDataManager.createKey(EntityNPCInterface.class, DataSerializers.BOOLEAN);
	private static final DataParameter<String> JobData = EntityDataManager.createKey(EntityNPCInterface.class, DataSerializers.STRING);
	private static final DataParameter<String> RoleData = EntityDataManager.createKey(EntityNPCInterface.class, DataSerializers.STRING);
	private static final DataParameter<Boolean> Walking = EntityDataManager.createKey(EntityNPCInterface.class, DataSerializers.BOOLEAN);
	public DataAbilities abilities;
	public DataDisplay display;
	public DataStats stats;
	public DataAI ais;
	public DataInventory inventory;
	public DataAdvanced advanced;
	public DataScript script;
	public RoleInterface role = RoleInterface.NONE;
	public JobInterface job = JobInterface.NONE;
	public int animationStart = 0;
	public int currentAnimation = 0;
	public float baseWidth = 0.6f;
	public float baseHeight = 1.9f;
	public float baseEyeHeight = 1.615f;
	public float eyeHeight = 1.615f;
	public BossInfoServer bossInfo;
	public CombatHandler combatHandler;
	public int[] dialogs = new int[0];
	public Faction faction;
	public double chasingPosZ;
	public double chasingPosY;
	public double chasingPosX;
	public double prevChasingPosZ;
	public double prevChasingPosY;
	public double prevChasingPosX;
	public boolean hasDied = false;
	public List<EntityLivingBase> interactingEntities = new ArrayList<>();
	public EntityAIAnimation animateAi;
	public EntityAILook lookAi;
	public long killedTime = 0L;
	public int lastInteract = 0;
	public LinkedNpcController.LinkedData linkedData;
	public long linkedLast = 0L;
	public String linkedName = "";
	public IChatMessages messages;
	public int npcVersion = VersionCompatibility.ModRev;
	public float scaleX;
	public float scaleY;
	public float scaleZ;
    private int taskCount = 1;
	public ResourceLocation textureCloakLocation = null;
	public ResourceLocation textureGlowLocation = null;
	public ResourceLocation textureLocation = null;
	public DataTimers timers;
	public long totalTicksAlive = 0L;
	public DataTransform transform;
	public boolean updateClient = false;
	private boolean wasKilled = false;
	public ICustomNpc<?> wrappedNPC;
	public boolean updateAI = true;
	private boolean isOldSneaking;
	public final Map<Entity, double[]> hitboxRiding = new HashMap<>();

	// New from Unofficial (GoodBird)
	public final HashSet<Integer> tracking = new HashSet<>();
	private double startYPos = -6666.0D;

	// New from Unofficial (BetaZavr)
	public static final DataParameter<Float> AimRotationYaw = EntityDataManager.createKey(EntityNPCInterface.class, DataSerializers.FLOAT); // fix bug while aiming
	protected long initTime;
	public DataAnimation animation;
	public EntityAICustom aiAttackTarget = null;
	public EntityNPCInterface aiOwnerNPC;
	public Path navigating;
	public EnumNPCAnimationType animationType = EnumNPCAnimationType.NONE;
	public JobPuppet puppet = new JobPuppet(this);
	public Entity lookAt = null;
	public float[] lookPos = new float[] { 0.0f, 0.0f };
	public boolean updateLook = false;
	public int homeDimensionId;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public EntityNPCInterface(World world) {
		super(world);
		combatHandler = new CombatHandler(this);
		bossInfo = new BossInfoServer(getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS);
		wrappedNPC = new NPCWrapper(this);
		if (!CustomNpcs.DefaultInteractLine.isEmpty()) { advanced.interactLines.lines.put(0, new Line(CustomNpcs.DefaultInteractLine)); }
		experienceValue = 0;
		float f = 0.9375f;
		scaleZ = f;
		scaleY = f;
		scaleX = f;
		faction = getFaction();
		setFaction(faction.id);
		setSize(1.0f, 1.0f);
		bossInfo.setVisible(false);
		// New from Unofficial (BetaZavr)
		if (!isServerWorld()) { SkinUtil.checkTexture(this); }
		maxHurtResistantTime = ais.getMaxHurtResistantTime();
		stepHeight = ais.stepheight;
		initTime = System.currentTimeMillis();
		animation.tryRunAnimation(AnimationKind.INIT);
		homeDimensionId = world.provider.getDimension();
	}

	public void addInteract(EntityLivingBase entity) {
		if (!ais.stopAndInteract || isAttacking() || !entity.isEntityAlive() || isAIDisabled()) { return; }
		if (ticksExisted - lastInteract < 180) { interactingEntities.clear(); }
		getNavigator().clearPath();
		lastInteract = ticksExisted;
		if (!interactingEntities.contains(entity)) { interactingEntities.add(entity); }
	}

	public void addRegularEntries() {
		tasks.addTask(taskCount++, new EntityAIReturn(this));
		tasks.addTask(taskCount++, new EntityAIFollow(this));
		if (ais.getStandingType() != 1 && ais.getStandingType() != 3) {
			tasks.addTask(taskCount++, new EntityAIWatchClosest(this, EntityLivingBase.class, 5.0f));
		}
		tasks.addTask(taskCount++, (lookAi = new EntityAILook(this)));
		tasks.addTask(taskCount++, new EntityAIWorldLines(this));
		if (!ais.aiDisabled) {
			tasks.addTask(taskCount++, new EntityAIJob(this));
			tasks.addTask(taskCount++, new EntityAIRole(this));
		}
		tasks.addTask(taskCount++, (animateAi = new EntityAIAnimation(this)));
		if (transform.isValid()) {
			tasks.addTask(taskCount++, new EntityAITransform(this));
		}
	}

	public void addTrackingPlayer(@Nonnull EntityPlayerMP player) {
		super.addTrackingPlayer(player);
		bossInfo.addPlayer(player);
	}

	public void addVelocity(double d, double d1, double d2) {
		if (isWalking() && !isKilled()) {
			super.addVelocity(d, d1, d2);
		}
	}

	protected float applyArmorCalculations(@Nonnull DamageSource source, float damage) {
		if (role instanceof RoleCompanion) {
			damage = ((RoleCompanion) role).getDamageAfterArmorAbsorb(source, damage);
		}
		return damage;
	}

	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		abilities = new DataAbilities(this);
		display = new DataDisplay(this);
		stats = new DataStats(this);
		ais = new DataAI(this);
		advanced = new DataAdvanced(this);
		inventory = new DataInventory(this);
		transform = new DataTransform(this);
		script = new DataScript(this);
		timers = new DataTimers(this);
		if (animation == null) { animation = new DataAnimation(this); }
		getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
		getAttributeMap().registerAttribute(SharedMonsterAttributes.FLYING_SPEED);
		getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(stats.maxHealth);
		getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(CustomNpcs.NpcNavRange);
		getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(getSpeed());
		getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(stats.melee.getStrength());
		getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).setBaseValue((getSpeed() * 2.0f));
	}

	public boolean attackEntityAsMob(@Nonnull Entity entity) { // this NPCs attempt to damage the target <- EntityAICustom
		AnimationConfig anim = animation.tryRunAnimation(AnimationKind.ATTACKING);
		if (anim != null) {
			boolean found = false;
			for (AnimationFrameConfig frame : anim.frames.values()) {
				if (frame.isNowDamage() && frame.damageDelay != 0) {
					final int time = frame.damageDelay * 50;
					CustomNPCsScheduler.runTack(() -> tryAttackEntityAsMob(entity, frame.id), time);
					found = true;
				}
			}
			if (!found) {
				final int time = (anim.totalTicks - 1) * 50;
				CustomNPCsScheduler.runTack(() -> tryAttackEntityAsMob(entity, anim.frames.size() - 1), time);
			}
			return false;
		}
		return tryAttackEntityAsMob(entity, 0);
	}

	private boolean tryAttackEntityAsMob(Entity target, int frameID) {
		if (ais.aiDisabled || target == null || !target.isEntityAlive()) { return false; }
		Set<Entity> entityList = new HashSet<>();
		entityList.add(target);
		if (CustomNpcs.ShowCustomAnimation && isServerWorld() && animation.isAnimated(AnimationKind.ATTACKING)) {
			List<AxisAlignedBB> aabbs = animation.getAnimation().getDamageHitboxes(this, frameID);
			if (aabbs.isEmpty()) { // only target
				double range = stats.melee.getRange();
				double minRange = (width + target.width) * 0.425d; // (/ 2.0 * 0.85)
				double yaw = Math.abs(Util.instance.getVector3D(posX, posY, posZ, target.posX, target.posY, target.posZ).getYaw());
				if (getDistance(target) - minRange > range || yaw > 60.0d) { return false; }
			}
			else { // custom targets
				for (AxisAlignedBB aabb : aabbs) {
					List<Entity> list = new ArrayList<>();
					try { list = world.getEntitiesWithinAABB(Entity.class, aabb); } catch (Exception ignored) { }
                    entityList.addAll(list);
				}
				entityList.remove(this);
			}
		}
		float amount = stats.melee.getStrength();
		DamageSource damageSource = new NpcDamageSource("npc", this);
		boolean attackEntity = false;
		for (Entity entity : entityList) {
			if (stats.melee.getDelay() < 10) { entity.hurtResistantTime = 0; }
			if (entity instanceof EntityLivingBase) {
				NpcEvent.MeleeAttackEvent event = new NpcEvent.MeleeAttackEvent(wrappedNPC, (EntityLivingBase) entity, amount);
				if (EventHooks.onNPCAttacksMelee(this, event)) { return false; }
				amount = event.damage;
			}
			attackEntity = entity.attackEntityFrom(damageSource, amount);
			if (attackEntity) {
				if (getOwner() instanceof EntityPlayer && entity instanceof EntityLivingBase) { ((IEntityLivingBaseMixin) entity).setRecentlyHit(100); }
				if (role instanceof RoleCompanion) { ((RoleCompanion) role).attackedEntity(entity); }
			}
			if (stats.melee.getEffectType() != 0) {
				if (stats.melee.getEffectType() != 1) {
					Potion eff = PotionEffectType.getMCType(stats.melee.getEffectType());
					if (eff != null && entity instanceof EntityLivingBase) {
						((EntityLivingBase) entity) .addPotionEffect(new PotionEffect(eff, stats.melee.getEffectTime() * 20, stats.melee.getEffectStrength()));
					}
				} else {
					entity.setFire(stats.melee.getEffectTime());
				}
			}
		}
		return attackEntity;
	}

	private boolean canBlockDamageSource(DamageSource damageSourceIn) {
		boolean isBlocked = false;
		int type = damageSourceIn.isUnblockable() || damageSourceIn.getDamageLocation() == null ? -1 : 0;
		if (damageSourceIn.getDamageLocation() != null) {
			type = 0;
			Vec3d vec3d = damageSourceIn.getDamageLocation(); // position from which damage is dealt
			float angle = (float) Util.instance.getAngles3D(posX, 0.0d, posZ, vec3d.x, 0.0d, vec3d.z).getYaw() - rotationYaw;
			Vec3d vec3d1 = getLook(1.0F); // which way is looking this NPC
			Vec3d vec3d2 = vec3d.subtractReverse(new Vec3d(posX, posY, posZ)).normalize();
			vec3d2 = new Vec3d(vec3d2.x, 0.0D, vec3d2.z);
			if (vec3d2.dotProduct(vec3d1) < 0.0D) {
				if (angle < 180.0f) { type = 1; } else { type = 2; }
			}
		}
		if (type != -1 && !damageSourceIn.isUnblockable()) {
			float chance = stats.getChanceBlockDamage() / 100.0f;
			if (chance > 0.0f && type > 0) { // in front of
				ItemStack stack;
				if (inventory.getProjectile() != null) { chance /= 3.0f; }
				else if (type == 1) { // to the right
					stack = inventory.getRightHand() != null ? inventory.getRightHand().getMCItemStack() : ItemStack.EMPTY;
					if (stack.getItem() instanceof ItemSword) {
						chance *= 1.3333f;
						if (chance < 0.25f) { chance = 0.25f; }
					}
					else if (stack.getItem() instanceof ItemShield) {
						chance *= 2.0f;
						if (chance < 0.75f) { chance = 0.75f; }
					}
				}
				else { // to the left
					stack = inventory.getLeftHand() != null ? inventory.getLeftHand().getMCItemStack() : ItemStack.EMPTY;
					if (stack.getItem() instanceof ItemSword) {
						chance *= 1.1667f;
						if (chance < 0.1f) { chance = 0.1f; }
					}
					else if (stack.getItem() instanceof ItemShield) {
						chance *= 1.75f;
						if (chance < 0.5f) { chance = 0.5f; }
					}
				}
				float f = rand.nextFloat();
				isBlocked = chance >= f;
			}
		}
		NpcEvent.NeedBlockDamage event = new NpcEvent.NeedBlockDamage(wrappedNPC, damageSourceIn, isBlocked, type);
		EventHooks.onNPCNeedBlockDamage(this, event);
		return event.isBlocked && !event.isCanceled();
	}

	@Override
	public boolean attackEntityFrom(@Nonnull DamageSource damagesource, float damage) {
		if (aiAttackTarget != null && aiAttackTarget.damaged()) { return false; }
		if (!isServerWorld() || CustomNpcs.FreezeNPCs || damagesource.damageType.equals("inWall")) { return false; }
		if (damagesource.getClass().getSimpleName().equals("TGDamageSource") && damage < 0.5f) { return false; }
		if (role.getEnumType() == RoleType.FOLLOWER && role.isFollowing() && damagesource == DamageSource.FALL) { return false; } // fix

		if (damagesource.damageType.equals("outOfWorld") && isKilled()) { reset(); }
		damage = stats.resistances.applyResistance(damagesource, damage);
		if (!combatHandler.canDamage(damagesource, damage)) { return false; }

		Entity entity = NoppesUtilServer.getDamageSource(damagesource);
		EntityLivingBase attackingEntity = null;
		if (entity instanceof EntityLivingBase) { attackingEntity = (EntityLivingBase) entity; }
		if (attackingEntity != null && attackingEntity == getOwner()) { return false; }
		if (attackingEntity instanceof EntityNPCInterface) {
			EntityNPCInterface npc = (EntityNPCInterface) attackingEntity;
			if (npc.faction.id == faction.id) { return false; }
			if (npc.getOwner() instanceof EntityPlayer) { recentlyHit = 100; }
		}
		else if (attackingEntity instanceof EntityPlayer && faction.isFriendlyToPlayer((EntityPlayer) attackingEntity)) {
			if (!damagesource.getClass().getSimpleName().equals("TGDamageSource")) { ForgeHooks.onLivingAttack(this, damagesource, damage); }
			return false;
		}

		NpcEvent.DamagedEvent event = new NpcEvent.DamagedEvent(wrappedNPC, entity, damage, damagesource);
		if (EventHooks.onNPCDamaged(this, event)) {
			ForgeHooks.onLivingAttack(this, damagesource, damage);
			return false;
		}
		damage = event.damage;
		if (isKilled()) { return false; }

		if (damagesource.damageType.contains("inFire")) { setFire(8); } // -> onFire
		boolean isHurt = false;
		if (attackingEntity == null) {
			isHurt = customAttackEntityFrom(damagesource, damage);
		}
		else {
			try {
				boolean check = false;
				if (!(attackingEntity instanceof EntityPlayer) || !((EntityPlayer) attackingEntity).isCreative()) {
					if (damage > 0.0f) {
						List<EntityNPCInterface> inRange = new ArrayList<>();
						try { inRange = world.getEntitiesWithinAABB(EntityNPCInterface.class, getEntityBoundingBox().grow(32.0, 16.0, 32.0)); } catch (Exception ignored) { }
						for (EntityNPCInterface npc : inRange) {
							if (npc.equals(this)) { continue; }
							npc.advanced.tryDefendFaction(faction.id, this, attackingEntity);
						}
					}
					if (isAttacking()) {
						if (getAttackTarget() != null && getDistance(getAttackTarget()) > getDistance(attackingEntity)) { setAttackTarget(attackingEntity); }
						isHurt = customAttackEntityFrom(damagesource, damage);
						check = true;
					}
					else if (damage > 0.0f) { setAttackTarget(attackingEntity); }
				}
				if (!check) { isHurt = customAttackEntityFrom(damagesource, damage); }
			} finally {
				if (event.clearTarget) {
					setAttackTarget(null);
					setRevengeTarget(null);
				}
			}
		}
		if (!isKilled()) {
			if (isHurt && damage > 0.0f) {
				animation.tryRunAnimation(AnimationKind.HIT);
			}
			else  {
				AnimationConfig anim = animation.tryRunAnimation(AnimationKind.BLOCKED);
				if (anim == null && !damagesource.isProjectile() && attackingEntity != null) { blockUsingShield(attackingEntity); }
			}
		}
		return isHurt;
	}

	private boolean customAttackEntityFrom(DamageSource source, float amount) {
		if (!net.minecraftforge.common.ForgeHooks.onLivingAttack(this, source, amount)) { return false; }
		if (!isServerWorld() || isEntityInvulnerable(source)) { return false; }
		idleTime = 0;
		if (getHealth() <= 0.0F || (source.isFireDamage() && isPotionActive(MobEffects.FIRE_RESISTANCE))) { return false; }
		float f = amount;
		if ((source == DamageSource.ANVIL || source == DamageSource.FALLING_BLOCK) && !getItemStackFromSlot(EntityEquipmentSlot.HEAD).isEmpty()) {
			getItemStackFromSlot(EntityEquipmentSlot.HEAD).damageItem((int)(amount * 4.0F + rand.nextFloat() * amount * 2.0F), this);
			amount *= 0.75F;
		}
		boolean isBlockedDamage = false;
		if (amount > 0.0F && canBlockDamageSource(source)) {
			damageShield(amount);
			amount = 0.0F;
			isBlockedDamage = true;
		}
		limbSwingAmount = 1.5F;
		boolean damageCanBeDone = true;
		int f0 = ais.getMaxHurtResistantTime();
		if (f0 != 0 && (float) hurtResistantTime > (float) f0 / 2.0F) {
			if (amount <= lastDamage) { return false; }
			damageEntity(source, amount - lastDamage);
			lastDamage = amount;
			damageCanBeDone = false;
		}
		else {
			lastDamage = amount;
			hurtResistantTime = f0;
			damageEntity(source, amount);
			maxHurtTime = f0 / 2;
			hurtTime = maxHurtTime;
		}
		attackedAtYaw = 0.0F;
		Entity entity1 = source.getTrueSource();
		if (entity1 != null) {
			if (entity1 instanceof EntityLivingBase) { setRevengeTarget((EntityLivingBase) entity1); }
			if (entity1 instanceof EntityPlayer) {
				recentlyHit = 100;
				attackingPlayer = (EntityPlayer) entity1;
			}
			else if (entity1 instanceof net.minecraft.entity.passive.EntityTameable) {
				net.minecraft.entity.passive.EntityTameable entity_wolf = (net.minecraft.entity.passive.EntityTameable)entity1;
				if (entity_wolf.isTamed()) {
					recentlyHit = 100;
					attackingPlayer = null;
				}
			}
		}
		if (damageCanBeDone) {
			if (isBlockedDamage) { world.setEntityState(this, (byte)29); }
			else if (source instanceof EntityDamageSource && ((EntityDamageSource)source).getIsThornsDamage()) { world.setEntityState(this, (byte)33); }
			else {
				byte b0;
				if (source == DamageSource.DROWN) { b0 = 36; }
				else if (source.isFireDamage()) { b0 = 37; }
				else{ b0 = 2; }
				world.setEntityState(this, b0);
			}
			if (source != DamageSource.DROWN && !isBlockedDamage) { markVelocityChanged(); }

			if (entity1 != null) {
				double d1 = entity1.posX - posX;
				double d0;
				for (d0 = entity1.posZ - posZ; d1 * d1 + d0 * d0 < 1.0E-4D; d0 = (Math.random() - Math.random()) * 0.01D) { d1 = (Math.random() - Math.random()) * 0.01D; }
				attackedAtYaw = (float)(MathHelper.atan2(d0, d1) * (180D / Math.PI) - (double) rotationYaw);
				knockBack(entity1, 0.4F, d1, d0);
			} else { attackedAtYaw = (float)((int)(Math.random() * 2.0D) * 180); }
		}

		if (getHealth() <= 0.0F) { onDeath(source); }
		else if (damageCanBeDone) { playHurtSound(source, isBlockedDamage); }
		boolean isDamaged = !isBlockedDamage;
		if (isDamaged) {
			((IEntityLivingBaseMixin) this).setLastDamageSource(source);
			((IEntityLivingBaseMixin) this).setLastDamageStamp(world.getTotalWorldTime());
		}
		if (entity1 instanceof EntityPlayerMP) {
			CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((EntityPlayerMP) entity1, this, source, f, amount, isBlockedDamage);
		}
		return isDamaged;
	}

	public void attackEntityWithRangedAttack(@Nonnull EntityLivingBase entity, float distanceFactor) {
		if (ais.aiDisabled) { return; }
		ItemStack proj = ItemStackWrapper.MCItem(inventory.getProjectile());
		if (proj == null) {
			updateAI = true;
			return;
		}
		NpcEvent.RangedLaunchedEvent event = new NpcEvent.RangedLaunchedEvent(wrappedNPC, entity, stats.ranged.getStrength());
		for (int i = 0; i < stats.ranged.getShotCount(); ++i) {
			EntityProjectile projectile = shoot(entity, stats.ranged.getAccuracy(), proj, distanceFactor == 1.0f);
			projectile.damage = event.damage;
			ItemStack stack = entity.getHeldItemMainhand();
			projectile.callback = ((projectile_0, pos, entity1) -> {
				if (stack.getItem() == CustomItems.soulstoneFull) {
					Entity e = ItemSoulstoneFilled.Spawn(null, stack, world, pos);
					if (e instanceof EntityLiving && entity1 instanceof EntityLiving) { ((EntityLiving) e).setRevengeTarget((EntityLiving) entity1); }
					else if (e instanceof EntityLivingBase && entity1 instanceof EntityLivingBase) { ((EntityLivingBase) e).setRevengeTarget((EntityLivingBase) entity1); }
				}
				SoundEvent se = stats.ranged.getSoundEvent((entity1 != null) ? 1 : 2);
				String sound = stats.ranged.getSound((entity1 != null) ? 1 : 2);
				float pitch = 1.2f / (getRNG().nextFloat() * 0.2f + 0.9f);
				if (se != null) { projectile_0.playSound(se, 1.0f, pitch); }
				else if (!sound.isEmpty()) { Packets.sendNearby(world, getPosition(), 64,
						new PacketPlaySound(sound, SoundCategory.NEUTRAL, posX, posY, posZ, 1.0f, pitch)); }
				return false;
			});
			SoundEvent se = stats.ranged.getSoundEvent(0);
			String sound = stats.ranged.getSound(0);
			if (se != null) { playSound(se, 2.0f, 1.0f); }
			else if (!sound.isEmpty()) { Packets.sendNearby(world, getPosition(), 64,
					new PacketPlaySound(sound, SoundCategory.NEUTRAL, posX, posY, posZ, 2.0f, 1.0f)); }
			event.projectiles.add((IProjectile<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(projectile));
		}
		EventHooks.onNPCRangedLaunched(this, event);
	}

	private double calculateStartYPos(BlockPos pos) {
		BlockPos startPos = ais.startPos();
		while (pos.getY() > 0) {
			IBlockState state = world.getBlockState(pos);
			if (ais.movementType == 2 && startPos.getY() <= pos.getY() && state.getMaterial() == Material.WATER) {
				pos = pos.down();
				continue;
			}
			if (state.getCollisionBoundingBox(world, pos) != null) {
				return state.getBoundingBox(world, pos).offset(pos).maxY;
			}
			pos = pos.down();
		}
		return 0.0;
	}

	private BlockPos calculateTopPos(BlockPos pos) {
		for (BlockPos check = pos; check.getY() > 0; check = check.down()) {
			IBlockState state = world.getBlockState(check);
			if (state.getBlock().isAir(state, world, check)) { return check; }
        }
		return pos;
	}

	@Override
	public boolean canAttackClass(@Nonnull Class<? extends EntityLivingBase> clazz) {
		return !ais.aiDisabled && EntityBat.class != clazz;
	}

	@Override
	public boolean canBeCollidedWith() { return !isKilled() && display.getHitboxState() != 1; }

	@Override
	public boolean canBeLeashedTo(@Nonnull EntityPlayer player) {
		return false;
	}

	@Override
	public boolean canBePushed() { return super.canBePushed() && display.getHitboxState() == 0; }

	@Override
	public boolean canBreatheUnderwater() { return ais.movementType == 2; }

	@Override
	protected boolean canDespawn() { return stats.spawnCycle == 4; }

	public boolean canFly() { return false; }

	private void clearTasks(EntityAITasks tasks) {
		List<EntityAITasks.EntityAITaskEntry> list = new ArrayList<>(tasks.taskEntries);
		for (EntityAITasks.EntityAITaskEntry entityaitaskentry : list) {
			try {
				tasks.removeTask(entityaitaskentry.action);
			} catch (Exception e) { LogWriter.error(e); }
		}
		tasks.taskEntries.clear();
	}

	public void cloakUpdate() {
		prevChasingPosX = chasingPosX;
		prevChasingPosY = chasingPosY;
		prevChasingPosZ = chasingPosZ;
		double d0 = posX - chasingPosX;
		double d1 = posY - chasingPosY;
		double d2 = posZ - chasingPosZ;
		double d3 = 10.0D;
		if (d0 > d3) { prevChasingPosX = chasingPosX = posX; }
		if (d2 > d3) { prevChasingPosZ = chasingPosZ = posY; }
		if (d1 > d3) { prevChasingPosY = chasingPosY = posZ; }
		if (d0 < -d3) { prevChasingPosX = chasingPosX = posX; }
		if (d2 < -d3) { prevChasingPosZ = chasingPosZ = posY; }
		if (d1 < -d3) { prevChasingPosY = chasingPosY = posZ; }
		chasingPosX += d0 * 0.25D;
		chasingPosZ += d2 * 0.25D;
		chasingPosY += d1 * 0.25D;
	}

	protected void damageEntity(@Nonnull DamageSource damageSrc, float damageAmount) {
		super.damageEntity(damageSrc, damageAmount);
		combatHandler.damage(damageSrc, damageAmount);
	}

	protected int decreaseAirSupply(int par1) {
		if (!stats.canDrown) { return par1; }
		return super.decreaseAirSupply(par1);
	}

	public void delete() {
		role.delete();
		job.delete();
		puppet.delete();
		super.setDead();
	}

	public void doorInteractType() {
		if (canFly()) {
			return;
		}
		EntityAIBase aiDoor = null;
		if (ais.doorInteract == 1) {
			tasks.addTask(taskCount++, aiDoor = new EntityAIOpenDoor(this, true));
		} else if (ais.doorInteract == 0) {
			tasks.addTask(taskCount++, aiDoor = new EntityAIBustDoor(this));
		}
		if (getNavigator() instanceof PathNavigateGround) {
			((PathNavigateGround) getNavigator()).setBreakDoors(aiDoor != null);
		}
	}

	protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
	}

	protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
	}

	protected void entityInit() {
		super.entityInit();
		dataManager.register(RoleData, "");
		dataManager.register(JobData, "");
		dataManager.register(FactionData, 0);
		dataManager.register(Animation, 0);
		dataManager.register(Walking, false);
		dataManager.register(Interacting, false);
		dataManager.register(IsDead, false);
		dataManager.register(Attacking, false);
		dataManager.register(AimRotationYaw, 361.0f);
	}

	public int followRange() {
		if ((advanced.scenes.getOwner() != null) ||
				(role.getEnumType() == RoleType.COMPANION && role.isFollowing()) ||
				(job.getEnumType() == JobType.FOLLOWER && job.isFollowing())) { return 4; }
		if (role.getEnumType() == RoleType.FOLLOWER && role.isFollowing()) { return 6; }
		return Math.max(1, stats.aggroRange - 1);
	}

	public boolean getAlwaysRenderNameTagForRender() {
		return true;
	}

	public @Nonnull Iterable<ItemStack> getArmorInventoryList() {
		ArrayList<ItemStack> list = new ArrayList<>();
		for (int i = 0; i < 4; ++i) {
			list.add(ItemStackWrapper.MCItem(inventory.armor.get(3 - i)));
		}
		return list;
	}

	public float getBlockPathWeight(@Nonnull BlockPos pos) {
		if (ais.movementType == 2) {
			return (world.getBlockState(pos).getMaterial() == Material.WATER) ? 10.0f : 0.0f;
		}
		float weight = world.getLightBrightness(pos) - 0.5f;
		if (world.getBlockState(pos).isOpaqueCube()) {
			weight += 10.0f;
		}
		return weight;
	}

	public boolean getCanSpawnHere() {
		return getBlockPathWeight(new BlockPos(posX, getEntityBoundingBox().minY, posZ)) >= 0.0f && world.getBlockState(new BlockPos(this).down()).canEntitySpawn(this);
	}

	public Entity getCommandSenderEntity() {
		if (!isServerWorld()) { return this; }
		EntityUtil.Copy(this, EntityNPCInterface.CommandPlayer);
		EntityNPCInterface.CommandPlayer.setWorld(world);
		EntityNPCInterface.CommandPlayer.setPosition(posX, posY, posZ);
		return EntityNPCInterface.CommandPlayer;
	}

	public @Nonnull EnumCreatureAttribute getCreatureAttribute() {
		return (stats == null) ? EnumCreatureAttribute.UNDEFINED : stats.creatureType;
	}

	public SoundEvent getDeathSound() {
		return null;
	}

	private Dialog getDialog(EntityPlayer player) {
		Set<Integer> newDS = new HashSet<>();
		Dialog dialog = null;
		for (int dialogId : dialogs) {
			if (!DialogController.instance.hasDialog(dialogId)) {
				continue;
			}
			newDS.add(dialogId);
			if (dialog != null) {
				continue;
			}
			Dialog d = DialogController.instance.get(dialogId);
			if (d.availability.isAvailable(player)) {
				dialog = d;
			}
		}
		if (newDS.size() != dialogs.length) {
			dialogs = new int[newDS.size()];
			int i = 0;
			for (int id : newDS) {
				dialogs[i] = id;
				i++;
			}
		}
		return dialog;
	}

	public Faction getFaction() {
		Faction fac = FactionController.instance.getFaction(dataManager.get(EntityNPCInterface.FactionData));
		if (fac == null) {
			return FactionController.instance.getFaction(FactionController.instance.getFirstFactionId());
		}
		return fac;
	}

	public EntityPlayerMP getFakeChatPlayer() {
		if (!isServerWorld()) { return null; }
		EntityUtil.Copy(this, EntityNPCInterface.ChatEventPlayer);
		EntityNPCInterface.ChatEventProfile.npc = this;
		EntityNPCInterface.ChatEventPlayer.refreshDisplayName();
		EntityNPCInterface.ChatEventPlayer.setWorld(world);
		EntityNPCInterface.ChatEventPlayer.setPosition(posX, posY, posZ);
		return EntityNPCInterface.ChatEventPlayer;
	}

	public @Nonnull Iterable<ItemStack> getHeldEquipment() {
		List<ItemStack> list = new ArrayList<>();
		list.add(ItemStackWrapper.MCItem(inventory.weapons.get(0)));
		list.add(ItemStackWrapper.MCItem(inventory.weapons.get(2)));
		return list;
	}

	public @Nonnull ItemStack getHeldItemMainhand() {
		IItemStack item;
		if (isAttacking()) {
			item = inventory.getRightHand();
		} else if (role instanceof RoleCompanion) {
			item = ((RoleCompanion) role).getItemInHand();
		} else if (job.overrideMainHand) {
			item = job.getMainhand();
		} else {
			item = inventory.getRightHand();
		}
		return ItemStackWrapper.MCItem(item);
	}

	public @Nonnull ItemStack getHeldItemOffhand() {
		IItemStack item;
		if (isAttacking()) {
			item = inventory.getLeftHand();
		} else if (job.overrideOffHand) {
			item = job.getOffhand();
		} else {
			item = inventory.getLeftHand();
		}
		return ItemStackWrapper.MCItem(item);
	}

	public @Nonnull ItemStack getItemStackFromSlot(@Nonnull EntityEquipmentSlot slot) {
		if (slot == EntityEquipmentSlot.MAINHAND) {
			return getHeldItemMainhand();
		}
		if (slot == EntityEquipmentSlot.OFFHAND) {
			return getHeldItemOffhand();
		}
		return ItemStackWrapper.MCItem(inventory.getArmor(3 - slot.getIndex()));
	}

	public String getJobData() { return dataManager.get(EntityNPCInterface.RoleData); }

	public boolean getLeashed() { return false; }

	public int getMaxSpawnedInChunk() { return 8; }

	public @Nonnull String getName() {
		if (display == null) { return "Display is null!"; }
		return display.getName();
	}

	public EntityLivingBase getOwner() {
		if (ais.aiDisabled) { return null; }
		if (advanced.scenes.getOwner() != null) { return advanced.scenes.getOwner(); }
		if (role instanceof RoleFollower) { return ((RoleFollower) role).owner; }
		if (role instanceof RoleCompanion) { return ((RoleCompanion) role).owner; }
		if (job instanceof JobFollower) { return ((JobFollower) job).following; }
		return null;
	}

	public @Nonnull BlockPos getPosition() {
		return new BlockPos(posX, posY, posZ);
	}

	public @Nonnull Vec3d getPositionVector() {
		return new Vec3d(posX, posY, posZ);
	}

	public @Nonnull EnumPushReaction getPushReaction() {
		return display.getHitboxState() == 0 ? super.getPushReaction() : EnumPushReaction.IGNORE;
	}

	public String getRoleData() { return dataManager.get(EntityNPCInterface.RoleData); }

	protected float getVoicePitch() {
		if (advanced.disablePitch) { return 1.0f; }
		return super.getSoundPitch();
	}

	public float getSpeed() {
		return ais.getWalkingSpeed() / 20.0f;
	}

	public float getStartXPos() {
		return ais.startPos().getX() + ais.bodyOffsetX / 10.0f;
	}

	public double getStartYPos() {
		return startYPos < 0.0d ? calculateStartYPos(ais.startPos()) : startYPos;
	}

	public float getStartZPos() {
		return ais.startPos().getZ() + ais.bodyOffsetZ / 10.0f;
	}

	@Override
	public int getTalkInterval() { return 160; }

	public void givePlayerItem(EntityPlayer player, ItemStack stack) {
		if (!ais.aiDisabled && isServerWorld()) {
			stack = stack.copy();
			float f = 0.7f;
			double d = world.rand.nextFloat() * f + (1.0f - f);
			double d2 = world.rand.nextFloat() * f + (1.0f - f);
			double d3 = world.rand.nextFloat() * f + (1.0f - f);
			EntityItem entityitem = new EntityItem(world, posX + d, posY + d2, posZ + d3, stack);
			entityitem.setPickupDelay(2);
			world.spawnEntity(entityitem);
			int i = stack.getCount();
			if (player.inventory.addItemStackToInventory(stack)) {
				world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_ITEM_PICKUP,
						SoundCategory.PLAYERS, 0.2f,
						((rand.nextFloat() - rand.nextFloat()) * 0.7f + 1.0f) * 2.0f);
				player.onItemPickup(entityitem, i);
				if (stack.getCount() <= 0) {
					entityitem.setDead();
				}
			}
		}
	}

	public boolean emptyOwner() {
		return (ais.aiDisabled || advanced.scenes.getOwner() == null) &&
				(role.getEnumType() != RoleType.FOLLOWER || !((RoleFollower) role).hasOwner()) &&
				(role.getEnumType() != RoleType.COMPANION || !((RoleCompanion) role).hasOwner()) &&
				(job.getEnumType() != JobType.FOLLOWER || !((JobFollower) job).hasOwner());
	}

	public boolean isAttacking() {
		return dataManager.get(EntityNPCInterface.Attacking);
	}

	public boolean isEntityAlive() {
		boolean bo = super.isEntityAlive();
		if (!bo || ais != null && ais.aiDisabled) { return bo; }
		return !isKilled();
	}

	public boolean isFollower() {
		return !ais.aiDisabled && advanced.scenes.getOwner() != null || role.isFollowing() || job.isFollowing();
	}

	public boolean isFriend(Entity entityTarget) {
		if (!(entityTarget instanceof EntityNPCInterface)) {
			return false;
		}
		EntityNPCInterface npcTarget = (EntityNPCInterface) entityTarget;
        return faction.id == npcTarget.faction.id || npcTarget.faction.frendFactions.contains(faction.id)
                || npcTarget.advanced.friendFactions.contains(faction.id)
                || faction.frendFactions.contains(npcTarget.faction.id)
                || advanced.friendFactions.contains(npcTarget.faction.id);
    }

	public boolean isInRange(double x, double y, double z, double range) {
		double dy = Math.abs(posY - y);
		if (y >= 0.0 && dy > range) {
			return false;
		}
		double dx = Math.abs(posX - x);
		double dz = Math.abs(posZ - z);
		return dx <= range && dz <= range;
	}

	public boolean isInRange(Entity entity, double range) {
		return isInRange(entity.posX, entity.posY, entity.posZ, range);
	}

	public boolean isInteracting() {
		if (!interactingEntities.isEmpty() && ticksExisted % 20 == 0) { // раз в секунду
			interactingEntities.removeIf(e -> e == null || !e.isEntityAlive() ||
					(e instanceof EntityPlayerMP && ((EntityPlayerMP)e).connection == null));
		}
		return ticksExisted - lastInteract < 40
				|| (!isServerWorld() && dataManager.get(EntityNPCInterface.Interacting))
				|| (ais.stopAndInteract && !interactingEntities.isEmpty()
				&& ticksExisted - lastInteract < 180);
	}

	public boolean isInvisible() { return display.getVisible() == 1; }

	public boolean isInvisibleToPlayer(@Nonnull EntityPlayer player) {
		return isInvisible() && !(player.getHeldItemMainhand().getItem() instanceof INPCToolItem)
				&& display.getAvailability().isAvailable(player);
	}

	public boolean isKilled() {
		return isDead || (dataManager != null && dataManager.get(EntityNPCInterface.IsDead));
	}

	public boolean isMoving() {
		double sp = getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
		double speed = 0.069d;
		if (sp != 0.0d) { speed = speed * 0.25d / sp; }
		double xz = Math.sqrt(Math.pow(motionX, 2.0d) + Math.pow(motionZ, 2.0d));
		return !getNavigator().noPath() || xz >= (speed / 2.0d) && (motionY <= -speed || motionY > 0.0d);
	}

	@Override
	public boolean isOnSameTeam(@Nonnull Entity entity) {
		if (isServerWorld()) {
			return entity instanceof EntityPlayer && getFaction().isFriendlyToPlayer((EntityPlayer) entity) ||
					entity == getOwner() || entity instanceof EntityNPCInterface && ((EntityNPCInterface) entity).faction.id == faction.id;
		}
		return super.isOnSameTeam(entity);
	}

	@Override
	public boolean isPlayerSleeping() { return getHealth() <= 0.0f || currentAnimation == 2 && !isAttacking() && getAttackingEntity() == null && navigating == null; }

	@Override
	public boolean isPotionApplicable(@Nonnull PotionEffect effect) {
		return !stats.potionImmune && (getCreatureAttribute() != EnumCreatureAttribute.ARTHROPOD || effect.getPotion() != MobEffects.POISON) && super.isPotionApplicable(effect);
	}

	@Override
	public boolean isPushedByWater() {
		return ais.movementType != 2;
	}

	@Override
	public boolean isSneaking() { return currentAnimation == 4 || super.isSneaking(); }

	public boolean isVeryNearAssignedPlace() {
		double xx = posX - getStartXPos();
		double yy = posY - getStartYPos();
		double zz = posZ - getStartZPos();
		return xx >= -0.1 && xx <= 0.1 && zz >= -0.1 && zz <= 0.1 && yy >= -0.6 && yy <= 0.6;
	}

	public boolean isWalking() {
		return ais.getMovingType() != 0 || isAttacking() || isFollower() || dataManager.get(EntityNPCInterface.Walking);
	}

	public void knockBack(@Nonnull Entity entity, float strength, double ratioX, double ratioZ) {
		super.knockBack(entity, strength * (2.0f - stats.resistances.get("knockback")), ratioX, ratioZ);
	}

	public boolean nearPosition(BlockPos pos) {
		BlockPos npcPos = getPosition();
		float x = (npcPos.getX() - pos.getX());
		float z = (npcPos.getZ() - pos.getZ());
		float y = (npcPos.getY() - pos.getY());
		float h = (MathHelper.ceil(height + 1.0f) * MathHelper.ceil(height + 1.0f));
		return x * x + z * z < 2.5 && y * y < h + 2.5;
	}

	@Override
	protected void collideWithEntity(@Nonnull Entity entityIn) {
		if (canBeCollidedWith() && display.getHitboxState() != 1 && ais.canBeCollide && !hitboxRiding.containsKey(entityIn)) {
			entityIn.applyEntityCollision(this);
		}
	}

	@Override
	public void applyEntityCollision(@Nonnull Entity entityIn) {
		if (!canBeCollidedWith() || !ais.canBeCollide || hitboxRiding.containsKey(entityIn)) { return; }
		super.applyEntityCollision(entityIn);
	}

	public void onCollide() {
		if (!ais.aiDisabled && isEntityAlive() && ticksExisted % 4 == 0 && isServerWorld()) {
			AxisAlignedBB axisalignedbb;
			if (getRidingEntity() != null && getRidingEntity().isEntityAlive()) { axisalignedbb = getEntityBoundingBox().union(getRidingEntity().getEntityBoundingBox()).grow(1.0, 0.0, 1.0); }
			else { axisalignedbb = getEntityBoundingBox().grow(1.0, 0.5, 1.0); }
			List<EntityLivingBase> list = new ArrayList<>();
			try { list = world.getEntitiesWithinAABB(EntityLivingBase.class, axisalignedbb); }
			catch (Exception ignored) { }
			for (Entity entity : list) {
				if (entity != this && entity.isEntityAlive()) { EventHooks.onNPCCollide(this, entity); }
			}
		}
	}

	@SuppressWarnings("ConstantConditions")
	public void onDeath(@Nonnull DamageSource damagesource) {
		setSprinting(false);
		getNavigator().clearPath();
		extinguish();
		clearActivePotions();
		Entity attackingEntity = NoppesUtilServer.getDamageSource(damagesource);
		if (role != null) { role.aiDeathExecute(attackingEntity); }
		if (job != null) {
			job.aiDeathExecute(attackingEntity);
			puppet.aiDeathExecute(attackingEntity);
		}
		if (isServerWorld()) {
			advanced.playSound(3, getSoundVolume(), getSoundPitch());
			NpcEvent.DiedEvent event = new NpcEvent.DiedEvent(wrappedNPC, damagesource, attackingEntity, combatHandler);
			// chance
			double baseChance = 1.0d;
			if (!combatHandler.aggressors.isEmpty()) {
				double luck = 0.0d;
				double enchLv = 0.0d;
				int i = 0;
				int j = 0;
				for (EntityLivingBase e : combatHandler.aggressors.keySet()) {
					IAttributeInstance l = e.getEntityAttribute(SharedMonsterAttributes.LUCK);
					if (l != null) {
						luck += l.getAttributeValue();
						i++;
					}
					ItemStack held = !e.getHeldItemMainhand().isEmpty() ? e.getHeldItemMainhand() : e.getHeldItemOffhand();
					if (held.isItemEnchanted()) {
						enchLv += EnchantmentHelper.getLootingModifier(e);
						j++;
					}
				}
				// Luck
				if (i > 0 && luck > 0.0d) {
					luck /= i;
					if (luck < 0) {
						luck *= -1;
						baseChance -= luck * luck * -0.005555d + luck * 0.255555d; // 1lv = 25%$ 10lv = 200%
					} else {
						baseChance += luck * luck * -0.005555d + luck * 0.255555d; // 1lv = 25%$ 10lv = 200%
					}
				}
				// Enchantment
				if (j > 0 && enchLv > 0.0d) {
					enchLv /= j;
					baseChance += enchLv * enchLv * 0.000555d + enchLv * 0.019444d; // 1lv = +2%$ 10lv = +25%
				}
			}
			// drop on ground
			Map<IEntity<?>, List<IItemStack>> mapD = inventory.createDrops(0, baseChance);
			if (mapD.isEmpty()) { event.droppedItems = new IItemStack[0]; }
			else {
				List<IItemStack> list = new ArrayList<>();
				event.droppedItems = new IItemStack[mapD.size()];
				for(IEntity<?> attacking : mapD.keySet()) {
					for (IItemStack iStack : mapD.get(attacking)) {
						if (!list.contains(iStack)) { list.add(iStack); }
					}
				}
				event.droppedItems = list.toArray(new IItemStack[0]);
			}
			// drop on player
			event.lootedItems = inventory.createDrops(1, baseChance);
			// to inventory from player
			event.inventoryItems = inventory.createDrops(2, baseChance);
			event.expDropped = inventory.getExpRNG();
			event.line = advanced.getKilledLine();
			if (role instanceof RoleFollower && !((RoleFollower) role).inventory.isEmpty()) {
				NpcMiscInventory inv = ((RoleFollower) role).inventory;
				for (int i = 0; i < inv.getSizeInventory(); i++) { entityDropItem(inv.getStackInSlot(i), 0.0f); }
				((RoleFollower) role).inventory.clear();
			}
			// to scripts
			EventHooks.onNPCDied(this, event);
			bossInfo.setVisible(false);
			inventory.dropStuff(event, damagesource);
			if (event.line != null) {
				saySurrounding(Line.formatTarget((Line) event.line, (attackingEntity instanceof EntityLivingBase) ? (EntityLivingBase) attackingEntity : null));
			}
		}
		AnimationConfig anim = animation.tryRunAnimation(AnimationKind.DIES);
		if (anim != null) {
			motionX = 0.0d;
			motionY = 0.0d;
			motionZ = 0.0d;
		}
		super.onDeath(damagesource);
	}

	@Override
	public void onDeathUpdate() {
		if (stats.spawnCycle != 3 && stats.spawnCycle != 4) {
			++deathTime;
			if (isServerWorld()) {
				if (!hasDied) { setDead(); }
				if (killedTime < System.currentTimeMillis() &&
						(stats.spawnCycle == 0 || world.isDaytime() && stats.spawnCycle == 1 || !world.isDaytime() && stats.spawnCycle == 2)) {
					reset();
				}
			}
		}
		else { super.onDeathUpdate(); }
	}

	@SuppressWarnings("ConstantConditions")
	public void onLivingUpdate() {
		if (CustomNpcs.FreezeNPCs) { return; }
		if (!hitboxRiding.isEmpty()) {
			if (display.getHitboxState() != 2 || isKilled()) { hitboxRiding.clear(); }
			else {
				for (Map.Entry<Entity, double[]> entry : new ArrayList<>(hitboxRiding.entrySet())) {
					Entity entity = entry.getKey();
					if (entity instanceof EntityPlayer && isServerWorld()) { continue; }
					boolean isSetPos = !isPassenger(entity) && entity.motionY <= 0.0f;
					if (isSetPos) {
						AxisAlignedBB npcBB = getEntityBoundingBox();
						AxisAlignedBB entityBB = entity.getEntityBoundingBox();
						if (!getNavigator().noPath()) { npcBB = npcBB.grow(width * 0.25, 0.0, width * 0.25); }
						isSetPos = npcBB.minX < entityBB.maxX &&
								npcBB.maxX > entityBB.minX &&
								npcBB.minZ < entityBB.maxZ &&
								npcBB.maxZ > entityBB.minZ;
					}
					if (isSetPos) {
						double[] addPos = entry.getValue();
						if (motionY <= 0.0f) {
							addPos[0] -= entity.motionX * 1.5;
							addPos[1] -= entity.motionZ * 1.5;
							if (motionX != 0.0f || motionZ != 0.0f) {
								entity.motionX *= 0.75;
								entity.motionZ *= 0.75;
							}
						}
						double x = posX - addPos[0];
						double y = posY + height * 1.025;
						double z = posZ - addPos[1];
						entity.onGround = true;
						entity.setPosition(x, y, z);
						entity.velocityChanged = true;
						if (entity instanceof EntityPlayer) {
							CustomNpcs.proxy.updatePlayerPos();
						}
					}
					else {
						entity.motionX += motionX * 1.1;
						entity.motionY += motionY;
						entity.motionZ += motionZ * 1.1;
						hitboxRiding.remove(entity);
					}
				}
			}
		} // custom mechanics for transporting entities on yourself
		if (isAIDisabled()) { super.onLivingUpdate(); }
		else {
			++totalTicksAlive;
			updateArmSwingProgress();
			if (totalTicksAlive % 20 == 0) {
				faction = getFaction();
				if (!interactingEntities.isEmpty()) {
					interactingEntities.removeIf(entity -> entity == null || entity.isDead ||
                            (entity instanceof EntityPlayerMP && ((EntityPlayerMP) entity).connection == null));
				}
				if (!hitboxRiding.isEmpty()) {
					hitboxRiding.entrySet().removeIf(entry -> {
						Entity entity = entry.getKey();
						return entity == null || entity.isDead ||
								(entity instanceof EntityPlayerMP && ((EntityPlayerMP) entity).connection == null);
					});
				}
				if (!tracking.isEmpty()) {
					tracking.removeIf(id -> {
						Entity entity = world.getEntityByID(id); // или level().getEntity(id)
						return entity == null || entity.isDead ||
								(entity instanceof EntityPlayerMP && ((EntityPlayerMP) entity).connection == null);
					});
				}
			}
			if (isServerWorld()) {
				if (!ais.aiDisabled) {
					if (aiAttackTarget != null) { aiAttackTarget.update(); }
					if (!isKilled() && totalTicksAlive % 20 == 0) {
						advanced.scenes.update();
						// heal
						if (getHealth() < getMaxHealth()) {
							if (stats.healthRegen > 0 && !isAttacking()) {
								heal(stats.healthRegen);
								if (CustomNpcs.ShowHealingParticles) {
									((WorldServer) world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, posX, posY + height, posZ, 1, width / 3.0d, 0.05d, width / 3.0d, 1.0d);
								}
							}
							if (stats.combatRegen > 0 && isAttacking()) {
								heal(stats.combatRegen);
								if (CustomNpcs.ShowHealingParticles) {
									((WorldServer) world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, posX, posY + height, posZ, 1, width / 3.0d, 0.05d, width / 3.0d, 1.0d);
								}
							}
						}
						// mob attacking
						if (faction.getsAttacked) {
							IAttributeInstance attribute = getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
							double range = attribute == null ? 16.0d : attribute.getAttributeValue();
							for (EntityMob mob : world.getEntitiesWithinAABB(EntityMob.class, getEntityBoundingBox().grow(range, range, range))) {
								if (mob.getAttackTarget() == null && mob.canEntityBeSeen(this)) { mob.setAttackTarget(this); }
							}
						}
						// linked NPC
						if (linkedData != null && linkedData.time > linkedLast) { LinkedNpcController.Instance.loadNpcData(this); }
						if (updateClient) { updateClient(); }
						if (updateAI) {
							updateTasks();
							updateAI = false;
						}
					}
				}
				if (getHealth() <= 0.0f && !isKilled()) {
					clearActivePotions();
					dataManager.set(IsDead, true);
					updateTasks();
					updateHitbox();
				}
				if (display.getBossbar() == 2) { bossInfo.setVisible(getAttackTarget() != null); }
				dataManager.set(Walking, !getNavigator().noPath());
				dataManager.set(Interacting, isInteracting());
				combatHandler.update();
				onCollide();

				// New from Unofficial (BetaZavr)
				if (updateLook) { Packets.sendAll(new PacketNpcLookPos(world.provider.getDimension(), getEntityId(), lookAt == null ? -1 : lookAt.getEntityId())); }
			}
			else {
				// New from Unofficial (BetaZavr)
				DataParameter<Byte> hand_states = ((IEntityLivingBaseMixin) this).getHandStates();
				ItemStack stack = getHeldItemMainhand();
				// imitation of using items
				if (hand_states != null &&
						(stack.getItem() instanceof ItemBow || stack.getItem() instanceof ItemShield || stack.getItem() instanceof ItemFishingRod)) {
					if (currentAnimation == 6 && dataManager.get(hand_states) != 1) {
						if (dataManager.get(hand_states) != 1) {
							dataManager.set(hand_states, (byte) 1);
							setActiveHand(EnumHand.MAIN_HAND);
						}
					} else if (currentAnimation != 6 && dataManager.get(hand_states) == 1) {
						stopActiveHand();
						dataManager.set(hand_states, (byte) 0);
					}
					stack.getItem().onUpdate(stack, world, this, 0, true);
				}
				isAirBorne = canFly() && world.getBlockState(getPosition().down()).getMaterial() == Material.AIR;
			}
			// New from Unofficial (BetaZavr)
			if (CustomNpcs.ShowCustomAnimation) {
				CustomNPCsScheduler.runTack(() -> {
					// Jump
					if (!animation.getJump() && !isKilled() && getHealth() > 0.0f && world != null && !(isInWater() || isInLava()) && ais.getNavigationType() == 0 && !onGround && motionY > 0.0d) {
						BlockPos posUnderfoot = getPosition().down();
						BlockPos posAhead = getPosition().add(motionX, 0, motionZ).down();
						boolean canJumpHere = !(world.getBlockState(posUnderfoot).getBlock() instanceof BlockStairs);
						boolean canLandThere = !(world.getBlockState(posAhead).getBlock() instanceof BlockStairs);
						if (canJumpHere && canLandThere) {
							animation.setJump(true);
							animation.tryRunAnimation(AnimationKind.JUMP);
						}
					}
					else if (animation.getJump() && onGround && animation.getAnimationStage() != EnumAnimationStages.Started) {
						animation.setJump(false);
						if (animation.isAnimated(AnimationKind.JUMP)) {
							animation.stopAnimation();
						}
					}
					// Swing
					if (!animation.getSwing() && swingProgress > 0.0f) {
						animation.setSwing(true);
						if (!animation.isAnimated(AnimationKind.ATTACKING, AnimationKind.AIM, AnimationKind.SHOOT)) {
							AnimationConfig anim = animation.tryRunAnimation(AnimationKind.SWING);
							if (anim != null) {
								swingProgress = 0.0f;
								swingProgressInt = 0;
								prevSwingProgress = 0.0f;
								isSwingInProgress = false;
							}
						}
					}
					else if (animation.getSwing() && swingProgress == 0.0f) {
						animation.setSwing(false);
					}
					// walking or standing
					animation.resetWalkAndStandAnimations();
				});
			}

			if (wasKilled != isKilled() && wasKilled) { reset(); }
			if (world.isDaytime() && isServerWorld() && stats.burnInSun) {
				float f = getBrightness();
				if (f > 0.5f && rand.nextFloat() * 30.0f < (f - 0.4f) * 2.0f && world.canBlockSeeSky(new BlockPos(this))) { setFire(8); }
			}
			super.onLivingUpdate();
			if (isAttacking() && getAttackTarget() != null) { dataManager.set(EntityNPCInterface.AimRotationYaw, rotationYawHead); }
			if (!isServerWorld()) {
				role.clientUpdate();
				if (textureCloakLocation != null) { cloakUpdate(); }
				if (currentAnimation != dataManager.get(EntityNPCInterface.Animation)) {
					currentAnimation = dataManager.get(EntityNPCInterface.Animation);
					animationStart = ticksExisted;
					updateHitbox();
				}
				if (!ais.aiDisabled && job instanceof JobBard) { ((JobBard) job).onLivingUpdate(); }
			}
			if (display.getBossbar() > 0) {
				bossInfo.setPercent(getHealth() / getMaxHealth());
			}
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (animation != null) { animation.updateTime(); }
		if (!ais.aiDisabled && ticksExisted % 10 == 0) {
			// fixing NPC data leak when initializing from the server
			if (initTime != 0L && !isServerWorld() && initTime < System.currentTimeMillis() - 1000L) {
				Packets.sendServer(new SPacketNpcInitData(getEntityId()));
				initTime = 0L;
			}
			// hitbox when dead
			if (!isKilled() && width <= 1.0E-5f) { updateHitbox(); }
			// path change
			if (isServerWorld()) {
				Path path = getNavigator().getPath();
				if (path != null) {
					PathPoint fp = path.getFinalPathPoint();
					BlockPos pos = getPosition();
					if (fp == null || pos.getX() == fp.x && pos.getY() == fp.y && pos.getZ() == fp.z) {
						navigating = null;
						updateNavClient();
					}
					else if (path != navigating) {
						navigating = path;
						updateNavClient();
					}
				}
				else if (navigating != null) {
					navigating = null;
					updateNavClient();
				}
			}
			// fell out of the world
			startYPos = calculateStartYPos(ais.startPos());
			if (startYPos < 0.0d && isServerWorld()) { isDead = true; }
			// to script event
			EventHooks.onNPCTick(this);
		}
		if ((isSneaking() && !isOldSneaking) || (!isSneaking() && isOldSneaking)) {
			updateHitbox();
			isOldSneaking = isSneaking();
		}
		// reset creative player's target
		if (deathTime > 0 || (getAttackTarget() instanceof EntityPlayer && ((EntityPlayer) getAttackTarget()).isCreative())) {
			super.setAttackTarget(null);
			updateTargetClient();
		}
		// timers
		if (!ais.aiDisabled) { timers.update(); }
		// refresh hitbox
		if (!isServerWorld()) {
			if (wasKilled != isKilled()) {
				deathTime = 0;
				updateHitbox();
			}
		}
		// if killed
		wasKilled = isKilled();
		if (currentAnimation == 14) { deathTime = 19; }
	}

	protected void playHurtSound(DamageSource ignoredSource, boolean isBlocked) {
		advanced.playSound(isBlocked ? 5 : 2, getSoundVolume(), getSoundPitch());
	}

	public void playLivingSound() {
		if (!isEntityAlive()) {
			return;
		}
		advanced.playSound((getAttackTarget() != null) ? 1 : 0, getSoundVolume(), getSoundPitch());
	}

	protected void playStepSound(@Nonnull BlockPos pos, @Nonnull Block block) {
		if (advanced.getSound(4) != null) {
			advanced.playSound(4, 0.15f, 1.0f);
		} else {
			super.playStepSound(pos, block);
		}
	}

	public boolean processInteract(@Nonnull EntityPlayer player, @Nonnull EnumHand hand) {
		if (!isServerWorld()) { return !isAttacking(); }
		if (hand != EnumHand.MAIN_HAND) { return true; }
		ItemStack stack = player.getHeldItem(hand);
        Item item = stack.getItem();
        if (item == CustomItems.moving) {
            setAttackTarget(null);
			ItemNpcMovingPath.register(this, stack, player);
            return true;
        }
		else if (item instanceof INPCToolItem) {
            setAttackTarget(null);
            setRevengeTarget(null);
            return true;
        }
        if (!ais.aiDisabled && EventHooks.onNPCInteract(this, player)) { return false; }
		if (getFaction().isAggressiveToPlayer(player)) {
			if (!isAttacking()) { setAttackTarget(player); }
			return !isAttacking();
		}
		addInteract(player);
		if (lookAi == null || !lookAi.fastRotation) {
			AnimationConfig anim = animation.tryRunAnimation(AnimationKind.INTERACT);
			if (anim != null ) {
				lookAi.fastRotation = true;
				CustomNPCsScheduler.runTack(() -> lookAi.fastRotation = false , anim.totalTicks * 50);
			}
		}
		Dialog dialog = getDialog(player);
		PlayerData pd = PlayerData.get(player);
		if (!faction.getIsHidden() && !pd.factionData.factionData.containsKey(faction.id)) {
			PlayerEvent.FactionUpdateEvent event = new PlayerEvent.FactionUpdateEvent((PlayerWrapper<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player), faction, faction.defaultPoints, true);
			EventHooks.onPlayerFactionChange(pd.scriptData, event);
			pd.factionData.factionData.put(faction.id, event.points);
		}
		QuestData data = pd.questData.getQuestCompletion(player, this);
		if (data != null) { Packets.send((EntityPlayerMP) player, new PacketQuestCompletion(data.quest.id)); }
		else if (dialog != null) { NoppesUtilServer.openDialog(player, this, dialog); }
		else if (!ais.aiDisabled && role.getType() > 0) { role.interact(player); }
		else { say(player, advanced.getInteractLine()); }
		return true;
	}

	public void readEntityFromNBT(@Nonnull NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		npcVersion = compound.getInteger("ModRev");
		VersionCompatibility.CheckNpcCompatibility(this, compound);
		display.load(compound);
		stats.load(compound);
		ais.load(compound);
		script.load(compound);
		timers.readFromNBT(compound);
		advanced.load(compound);
		role.load(compound);
		job.load(compound);
		animation.load(compound);
		inventory.load(compound);
		transform.load(compound);
		killedTime = compound.getLong("KilledTime");
		totalTicksAlive = compound.getLong("TotalTicksAlive");
		linkedName = compound.getString("LinkedNpcName");
		if (isServerWorld()) {
			LinkedNpcController.Instance.loadNpcData(this);
		}
		getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(CustomNpcs.NpcNavRange);
		updateAI = true;
		if (compound.hasKey("HomeDimensionId", 3)) {
			homeDimensionId = compound.getInteger("HomeDimensionId");
		}

		if (compound.hasKey("Puppet")) { puppet.load(compound.getCompoundTag("Puppet")); }
	}

	public void readSpawnData(ByteBuf buf) { readSpawnData((new FriendlyByteBuf(buf)).readNbt()); }

	public void readSpawnData(NBTTagCompound compound) {
		display.load(compound);
		animation.load(compound);
		stats.setLevel(compound.getInteger("NPCLevel"));
		stats.setRarity(compound.getInteger("NPCRarity"));
		stats.setRarityTitle(compound.getString("NPCRarityTitle"));
		stats.setMaxHealth(compound.getDouble("MaxHealth"));
		stats.hideKilledBody = compound.getBoolean("DeadBody");
		stats.aggroRange = compound.getInteger("AggroRange");
		if (stats.aggroRange < 1) {
			stats.aggroRange = 1;
		}
		IAttributeInstance follow_range = getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        follow_range.setBaseValue(stats.aggroRange);
        ais.setWalkingSpeed(compound.getInteger("Speed"));
		ais.setStandingType(compound.getInteger("StandingState"));
		ais.setMovingType(compound.getInteger("MovingState"));
		ais.orientation = compound.getInteger("Orientation");
		ais.bodyOffsetX = compound.getFloat("PositionXOffset");
		ais.bodyOffsetY = compound.getFloat("PositionYOffset");
		ais.bodyOffsetZ = compound.getFloat("PositionZOffset");
		if (compound.hasKey("MaxHurtResistantTime", 3)) {
			ais.setMaxHurtResistantTime(compound.getInteger("MaxHurtResistantTime"));
			maxHurtResistantTime = ais.getMaxHurtResistantTime();
		}
		inventory.armor = NBTTags.getIItemStackMap(compound.getTagList("Armor", 10));
		inventory.weapons = NBTTags.getIItemStackMap(compound.getTagList("Weapons", 10));
		if (job.getEnumType() == JobType.BARD) { job.load(compound.getCompoundTag("Bard")); }
		if (role.getEnumType() == RoleType.COMPANION) { role.load(compound.getCompoundTag("Companion")); }
		if (this instanceof EntityCustomNpc) {
			((EntityCustomNpc) this).modelData.load(compound.getCompoundTag("ModelData"));
		}
		advanced.load(compound);
		dataManager.set(EntityNPCInterface.IsDead, compound.getBoolean("IsDead"));
		deathTime = compound.getInteger("DeathTime");

		NBTTagList lookPosList = compound.getTagList("LookPos", 5);
		lookPos[0] = ValueUtil.correctFloat(lookPosList.getFloatAt(0), -45.0f, 45.0f);
		lookPos[1] = ValueUtil.correctFloat(lookPosList.getFloatAt(1), -45.0f, 45.0f);
		baseWidth = compound.getFloat("BaseWidth");
		baseHeight = compound.getFloat("BaseHeight");
		width = compound.getFloat("Width");
		height = compound.getFloat("Height");

		// New from Unofficial (GoodBird)
		ais.mountControl = compound.getBoolean("MountControl");

		// animation
		animationType = EnumNPCAnimationType.values()[compound.getInteger("AnimationType")];
		if (compound.hasKey("Puppet")) { puppet.load(compound.getCompoundTag("Puppet")); }
	}

	public void removeTrackingPlayer(@Nonnull EntityPlayerMP player) {
		super.removeTrackingPlayer(player);
		bossInfo.removePlayer(player);
	}

	public void reset() {
		baseWidth = 0.6f;
		baseHeight = 1.9f;
		baseEyeHeight = 1.615f;
		eyeHeight = 1.615f;
		if (this instanceof EntityCustomNpc) {
			EntityLivingBase entity = ((EntityCustomNpc) this).modelData.getEntity(this);
			if (entity != null) {
				baseWidth = entity.width;
				baseHeight = entity.height;
				baseEyeHeight = entity.getEyeHeight();
				eyeHeight = entity.getEyeHeight();
			}
		}
		if (!isServerWorld()) {
			// Remove special death effect from specific weapons mod TechGuns
			try	{
				Class<?> tgDTC = Class.forName("techguns.capabilities.TGDeathTypeCap");
				Method get = tgDTC.getMethod("get", EntityLivingBase.class);
				Object cap = get.invoke(null, this);
				Method getDeathType = tgDTC.getMethod("getDeathType");
				Object dt = getDeathType.invoke(cap);
				if (!dt.toString().equals("DEFAULT")) {
					Class<?> edt = Class.forName("techguns.deatheffects.EntityDeathUtils$DeathType");
					Method setDeathType = tgDTC.getMethod("setDeathType", edt);
					setDeathType.invoke(cap, edt.getEnumConstants()[0]);
				}
			}
			catch (Exception ignored) { }
		}
		hasDied = false;
		isDead = false;
		setSprinting(wasKilled = false);
		aiOwnerNPC = null;
		setHealth(getMaxHealth());
		dataManager.set(EntityNPCInterface.Animation, 0);
		dataManager.set(EntityNPCInterface.Walking, false);
		dataManager.set(EntityNPCInterface.IsDead, false);
		dataManager.set(EntityNPCInterface.Interacting, false);
		interactingEntities.clear();
		combatHandler.reset();
		setAttackTarget(null);
		setRevengeTarget(null);
		deathTime = 0;
		setFire(0);
		lookAt = null;
		if (lookAi != null) { lookAi.fastRotation = false; }
		updateLook = false;
		if (ais.returnToStart && emptyOwner() && isServerWorld() && !isRiding()) {
			boolean isTransfer = false;
			if (world != null && world.provider.getDimension() != homeDimensionId && getServer() != null) {
				isTransfer = Util.instance.teleportEntity(getServer(), this, homeDimensionId,
						getStartXPos(), getStartYPos(), getStartZPos()) instanceof EntityNPCInterface;
			}
			if (!isTransfer) { setLocationAndAngles(getStartXPos(), getStartYPos(), getStartZPos(), rotationYaw, rotationPitch); }
		}
		maxHurtResistantTime = ais.getMaxHurtResistantTime();
		killedTime = 0L;
		extinguish();
		clearActivePotions();
		travel(0.0f, 0.0f, 0.0f);
		distanceWalkedModified = 0.0f;
		getNavigator().clearPath();
		currentAnimation = 0;
		updateHitbox();
		updateAI = true;
		ais.movingPos = 0;
		if (getOwner() != null) { getOwner().setLastAttackedEntity(Objects.requireNonNull(EntityList.newEntity(EntityPainting.class, world))); }
		bossInfo.setVisible(display.getBossbar() == 1);
		job.reset();
		animation.stopAnimation();
		animation.tryRunAnimation(AnimationKind.INIT);
		updateClient = true;
		stepHeight = ais.stepheight;
		lookPos[0] = 0.0f;
		lookPos[1] = 0.0f;
		EventHooks.onNPCInit(this);

		// New from Unofficial (BetaZavr)
		puppet.reset();
		setFlag(1, false);
	}

	public void reset(int delay) {
		CustomNPCsScheduler.runTack(this::reset, delay);
	}

	public void say(EntityPlayer playerIn, Line line) {
		if (line != null && playerIn instanceof EntityPlayerMP && getEntitySenses().canSee(playerIn)) {
			EntityPlayerMP player = (EntityPlayerMP ) playerIn;
			if (!line.getSound().isEmpty()) {
				Packets.send(player, new PacketPlaySound(line.getSound(), SoundCategory.NEUTRAL, posX, posY, posZ, getSoundVolume(), getVoicePitch()));
			}
			if (!line.getText().replaceAll(" ", "").replaceAll("" + ((char) 9), "").isEmpty()) {
				Packets.send(player, new PacketChatBubble(getEntityId(), Component.translatable(line.getText()), line.getShowText()));
			}
		}
	}

	public void saySurrounding(Line line) {
		if (line == null) {
			return;
		}
		if (line.getShowText() && !line.getText().isEmpty()) {
			ServerChatEvent event = new ServerChatEvent(getFakeChatPlayer(), line.getText(), new TextComponentTranslation(line.getText().replace("%", "%%")));
			if (CustomNpcs.NpcSpeachTriggersChatEvent && (MinecraftForge.EVENT_BUS.post(event) || event.getComponent() == null)) {
				return;
			}
			line.setText(event.getComponent().getUnformattedText().replace("%%", "%"));
		}
		for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class,
				new AxisAlignedBB(-20.0d, -20.0d, -20.0d, 20.0d, 20.0d, 20.0d).offset(posX, posY, posZ),
				(e) -> e.getDistance(this) < 20.0d)) {
			say(player, line);
		}
	}

	public void seekShelter() {
		if (!ais.aiDisabled) {
			if (ais.findShelter == 0) {
				tasks.addTask(taskCount++, new EntityAIMoveIndoors(this));
			} else if (ais.findShelter == 1) {
				if (!canFly()) { tasks.addTask(taskCount++, new EntityAIRestrictSun(this)); }
				tasks.addTask(taskCount++, new EntityAIFindShade(this));
			}
		}
	}

	public void setPriorityAttackTarget(EntityLivingBase entityTarget) {
		if (!isEntityAlive() ||
				getAttackTarget() == entityTarget ||
				(entityTarget instanceof EntityPlayer && ((EntityPlayer) entityTarget).capabilities.disableDamage) ||
				(entityTarget != null && entityTarget == getOwner()) ||
				(entityTarget instanceof EntityNPCInterface && isFriend(entityTarget))
		) {
			return;
		}
		super.setAttackTarget(entityTarget);
		updateTargetClient();
	}


	public void onAttack(EntityLivingBase entity) {
		if (!ais.aiDisabled && entity != null && entity != this && !isAttacking() && ais.onAttack != 3 && entity != getOwner()) {
			super.setAttackTarget(entity);
			updateTargetClient();
		}
	}

	/**
	 * dataManager.set(EntityNPCInterface.Attacking, boolean); in to CombatHandler
	 */
    public void setAttackTarget(EntityLivingBase entity) {
		if ((!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).capabilities.disableDamage) && (entity == null || entity != getOwner()) && getAttackTarget() != entity) {
			// New from Unofficial (BetaZavr): priority target
			if (getAttackTarget() != null && combatHandler.priorityTarget != null) { return; }
			if (entity != null) {
				EntityLivingBase parent = entity;
				NpcEvent.TargetEvent event = new NpcEvent.TargetEvent(wrappedNPC, entity);
				if (EventHooks.onNPCTarget(this, event)) { return; }
				if (event.entity == null) { entity = null; }
				else { entity = event.entity.getMCEntity(); }
				// New from Unofficial (BetaZavr): new check
				if (!parent.equals(entity) &&
						((entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage) || entity == getOwner() || getAttackTarget() == entity)) {
					return;
				}
			}
			else {
				for (EntityAITasks.EntityAITaskEntry en : targetTasks.taskEntries) {
					if (en.using) {
						en.using = false;
						en.action.resetTask();
					}
				}
				if (EventHooks.onNPCTargetLost(this, getAttackTarget())) { return; }
			}
			if (entity != null && entity != this && ais.onAttack != 3 && !isAttacking() && isServerWorld()) {
				Line line = advanced.getAttackLine();
				if (line != null) { saySurrounding(Line.formatTarget(line, entity)); }
			}
			super.setAttackTarget(entity);
			updateTargetClient();
		}
	}

	public void setCurrentAnimation(int animation) {
		currentAnimation = animation;
		dataManager.set(EntityNPCInterface.Animation, animation);
		if (animation != 4 && aiAttackTarget instanceof EntityAICommanderTarget) {
			((EntityAICommanderTarget) aiAttackTarget).baseAnimation = animation;
		}
	}

	public void setDataWatcher(EntityDataManager dataManagerIn) { dataManager = dataManagerIn; }

	public void setDead() {
		hasDied = true;
		removePassengers();
		dismountRidingEntity();
		if (!isServerWorld() || stats.spawnCycle == 3 || stats.spawnCycle == 4) {
			delete();
		} else {
			setHealth(-1.0f);
			setSprinting(false);
			getNavigator().clearPath();
			setCurrentAnimation(2);
			updateHitbox();
			if (killedTime <= 0L) {
				killedTime = stats.respawnTime * 1000L + System.currentTimeMillis();
			}
			if (!ais.aiDisabled) {
				role.killed();
				job.killed();
				puppet.killed();
			}
		}
	}

	public void setFaction(int id) {
		if (id < 0 || !isServerWorld()) { return; }
		dataManager.set(EntityNPCInterface.FactionData, id);
	}

	public void setImmuneToFire(boolean immuneToFire) {
		isImmuneToFire = immuneToFire;
		stats.immuneToFire = immuneToFire;
	}

	public void setInWeb() {
		if (!stats.ignoreCobweb) {
			super.setInWeb();
		}
	}

	public void setItemStackToSlot(@Nonnull EntityEquipmentSlot slot, @Nonnull ItemStack item) {
		if (slot == EntityEquipmentSlot.MAINHAND) {
			inventory.weapons.put(0, Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item));
		} else if (slot == EntityEquipmentSlot.OFFHAND) {
			inventory.weapons.put(2, Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item));
		} else {
			inventory.armor.put(3 - slot.getIndex(), Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item));
		}
	}

	public void setJobData(String s) {
		dataManager.set(EntityNPCInterface.RoleData, s);
	}

	public void setMoveType() {
		if (ais.getMovingType() == 1) {
			tasks.addTask(taskCount++, new EntityAIWander(this));
		}
		if (ais.getMovingType() == 2) {
			tasks.addTask(taskCount++, new EntityAIMovingPath(this));
		}
	}

	public void setPortal(@Nonnull BlockPos pos) {
	}

	private void setResponse() {
		aiAttackTarget = null;
		setFlag(1, false);
		aiOwnerNPC = null;
		if (!ais.aiDisabled) {
			if (ais.canSprint) { tasks.addTask(taskCount++, new EntityAISprintToTarget(this)); }
			if (ais.onAttack == 1) { tasks.addTask(taskCount++, new EntityAINpcPanic(this, 1.2F)); }
			else if (ais.onAttack == 2) { // Avoid
				tasks.addTask(taskCount++, aiAttackTarget = new EntityAIAvoidTarget(this));
			}
			else if (ais.onAttack == 0) {
				if (ais.canLeap) { tasks.addTask(taskCount++, new EntityAIPounceTarget(this)); } // can Jump
				switch (ais.tacticalVariant) {
					case RUSH: {
						tasks.addTask(taskCount++, (aiAttackTarget = new EntityAIOnslaught(this)));
						break;
					}
					case STAGGER: {
						tasks.addTask(taskCount++, (aiAttackTarget = new EntityAIDodge(this)));
						break;
					}
					case ORBIT: {
						tasks.addTask(taskCount++,
								(aiAttackTarget = new EntityAISurround(this)));
						break;
					}
					case HIT_AND_RUN: {
						tasks.addTask(taskCount++, (aiAttackTarget = new EntityAIHitAndRun(this)));
						break;
					}
					case COMMANDER: {
						tasks.addTask(taskCount++, aiAttackTarget = new EntityAICommanderTarget(this));
						break;
					}
					case STALK: {
						tasks.addTask(taskCount++, aiAttackTarget = new EntityAIStalkTarget(this));
						break;
					}
					default: {
						tasks.addTask(taskCount++, (aiAttackTarget = new EntityAINoTactic(this)));
						break;
					}
				}
			} // Attack
		}
		else { tasks.addTask(taskCount++, (aiAttackTarget = new EntityAIOnslaught(this))); }
	}

	public void setRoleData(String s) {
		dataManager.set(EntityNPCInterface.RoleData, s);
	}

	public void setSwingingArms(boolean swingingArms) {
	}

	public EntityProjectile shoot(double x, double y, double z, int accuracy, ItemStack proj, boolean indirect) {
		EntityProjectile projectile = new EntityProjectile(world, this, proj.copy(), true);
		double varX = x - posX;
		double varY = y - (posY + getEyeHeight());
		double varZ = z - posZ;
		float varF = projectile.hasGravity() ? MathHelper.sqrt(varX * varX + varZ * varZ) : 0.0f;
		float angle = projectile.getAngleForXYZ(varY, varF, indirect);
		float acc = 20.0f - MathHelper.floor(accuracy / 5.0f);
		projectile.shoot(varX, varY, varZ, angle, acc);
		world.spawnEntity(projectile);
		animation.tryRunAnimation(AnimationKind.SHOOT);
		if (animation.isAnimated(AnimationKind.AIM)) {
			animation.stopAnimation();
		}
		return projectile;
	}

	public EntityProjectile shoot(EntityLivingBase entity, int accuracy, ItemStack proj, boolean indirect) {
		return shoot(entity.posX, entity.getEntityBoundingBox().minY + entity.height / 2.0f, entity.posZ, accuracy, proj, indirect);
	}

	public boolean shouldDismountInWater(@Nonnull Entity rider) {
		return false;
	}

	public void tpTo(EntityLivingBase owner) {
		if (owner == null) { return; }
		EnumFacing facing = owner.getHorizontalFacing().getOpposite();
		BlockPos pos = new BlockPos(owner.posX, owner.getEntityBoundingBox().minY, owner.posZ);
		pos = pos.add(facing.getFrontOffsetX(), 0, facing.getFrontOffsetZ());
		pos = calculateTopPos(pos);
		for (int i = -1; i < 2; ++i) {
			for (int j = 0; j < 3; ++j) {
				BlockPos check;
				if (facing.getFrontOffsetX() == 0) { check = pos.add(i, 0, j * facing.getFrontOffsetZ()); }
				else { check = pos.add(j * facing.getFrontOffsetX(), 0, i); }
				check = calculateTopPos(check);
				if (!owner.world.getBlockState(check).isFullBlock() && !owner.world.getBlockState(check.up()).isFullBlock()) {
					if (owner.world.provider.getDimension() != world.provider.getDimension()) {
						try { Util.instance.teleportEntity(getServer(), this, owner.world.provider.getDimension(), (check.getX() + 0.5f), check.getY(), (check.getZ() + 0.5f)); }
						catch (Exception ignored) { }
                    }
					else { setLocationAndAngles((check.getX() + 0.5f), check.getY(), (check.getZ() + 0.5f), rotationYaw, rotationPitch); }
					getNavigator().clearPath();
					break;
				}
			}
		}
	}

	public void travel(float f1, float f2, float f3) {
		double d0 = posX;
		double d2 = posY;
		double d3 = posZ;
		super.travel(f1, f2, f3);
		if (!ais.aiDisabled && role instanceof RoleCompanion && isServerWorld()) {
			((RoleCompanion) role).addMovementStat(posX - d0, posY - d2, posZ - d3);
		}
	}

	public void updateClient() {
		Packets.sendNearby(this, new PacketNpcUpdate(getEntityId(), writeSpawnData()));
		updateClient = false;
		updateNavClient();
	}

	public void updateNavClient() {
		if (isServerWorld()) {
			Packets.sendNearby(world, getPosition(), 160, new PacketNpcNavigation(getEntityId(), navigating));
		}
	}

	public void updateTargetClient() {
		if (isServerWorld()) {
			Packets.sendNearby(world, getPosition(), 160, new PacketNpcTarget(getEntityId(), getAttackTarget() == null ? -1 : getAttackTarget().getEntityId()));
		}
	}

	public void updateHitbox() {
		// collide in
		// EntityRenderer.getMouseOver(0.0f);
		// AABB = getEntityBoundingBox == (boundingBox);
		// set in setPosition();
		float w = baseWidth;
		float h = baseHeight;
		float eh = baseEyeHeight;
		if (display.width != 0.0f || display.height != 0.0f) {
			w = display.width;
			h = display.height;
		}
		if (((currentAnimation == AnimationType.SLEEP.get() || currentAnimation == AnimationType.CRAWL.get()) && !isAttacking()) || deathTime > 0) {
			width = w / 0.75f;
			height = h / 4.75f;
			eyeHeight = eh / 4.75f;
		}
		else if (isRiding() || isSneaking()) {
			width = w;
			height = h / 1.3f;
			eyeHeight = eh / 1.3f;
		}
		else {
			width = w;
			height = h;
			eyeHeight = eh;
		}
		if (display.getModel() == null && this instanceof EntityCustomNpc) {
			ModelData modeldata = ((EntityCustomNpc) this).modelData;
			ModelPartConfig model = modeldata.getPartConfig(EnumParts.HEAD);
			float scaleHead = Math.max(model.scaleX, model.scaleZ);
			model = modeldata.getPartConfig(EnumParts.BODY);
			float scaleBody = Math.max(model.scaleX, model.scaleZ);
			width *= Math.max(scaleHead, scaleBody);
			width = width / 5.0f * display.getSize();
			height = height / 5.0f * display.getSize();
			eyeHeight = eyeHeight / 5.0f * display.getSize();
		}
		double n = width / 2.0f;
		if (n > World.MAX_ENTITY_RADIUS) { World.MAX_ENTITY_RADIUS = n; }
		if (getHealth() == 0) { return; }
		setPosition(posX, posY, posZ); // set BoundingBox
	}

	private void updateTasks() {
		if (!isServerWorld()) { return; }
		clearTasks(tasks);
		clearTasks(targetTasks);
		if (isKilled()) {
			return;
		}
		Predicate<EntityLivingBase> attackEntitySelector = new NPCAttackSelector(this);
		targetTasks.addTask(0, new EntityAIClearTarget(this));
		targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
		targetTasks.addTask(2, new EntityAIClosestTarget(this,4, ais.directLOS, false, attackEntitySelector));
		targetTasks.addTask(3, new EntityAIOwnerHurtByTarget(this));
		targetTasks.addTask(4, new EntityAIOwnerHurtTarget(this));

		PathWorldListener pwl = ((IWorldMixin) world).getPathListener();
		if (pwl != null) { pwl.onEntityRemoved(this); }
		if (ais.movementType == 1) {
			moveHelper = new FlyingMoveHelper(this);
			navigator = new PathNavigateFlying(this, world);
		} else if (ais.movementType == 2) {
			moveHelper = new FlyingMoveHelper(this);
			navigator = new PathNavigateSwimmer(this, world);
		} else {
			moveHelper = new EntityMoveHelper(this);
			navigator = new PathNavigateGround(this, world);
			tasks.addTask(0, new EntityAIWaterNav(this));
		}
		if (pwl != null) { pwl.onEntityAdded(this); }
		taskCount = 1;
		addRegularEntries();
		doorInteractType();
		seekShelter();
		setResponse();
		setMoveType();
	}

	public void writeEntityToNBT(@Nonnull NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		display.save(compound);
		stats.save(compound);
		ais.save(compound);
		script.save(compound);
		timers.writeToNBT(compound);
		advanced.save(compound);
		role.save(compound);
		job.save(compound);
		inventory.save(compound);
		transform.save(compound);
		animation.save(compound);
		compound.setLong("KilledTime", killedTime);
		compound.setLong("TotalTicksAlive", totalTicksAlive);
		compound.setInteger("ModRev", npcVersion);
		compound.setString("LinkedNpcName", linkedName);
		compound.setInteger("HomeDimensionId", homeDimensionId);

		puppet.save(compound);
	}

	public NBTTagCompound writeSpawnData() {
		NBTTagCompound compound = new NBTTagCompound();
		display.save(compound);
		advanced.save(compound);
		animation.save(compound);
		compound.setInteger("NPCLevel", stats.getLevel());
		compound.setInteger("NPCRarity", stats.getRarity());
		compound.setString("NPCRarityTitle", stats.getRarityTitle());
		compound.setDouble("MaxHealth", stats.maxHealth);
		compound.setBoolean("DeadBody", stats.hideKilledBody);
		compound.setInteger("AggroRange", stats.aggroRange);
		compound.setTag("Armor", NBTTags.nbtIItemStackMap(inventory.armor));
		compound.setTag("Weapons", NBTTags.nbtIItemStackMap(inventory.weapons));
		compound.setInteger("Speed", ais.getWalkingSpeed());
		compound.setInteger("CurrentAnimation", currentAnimation);
		compound.setInteger("StandingState", ais.getStandingType());
		compound.setInteger("MovingState", ais.getMovingType());
		compound.setInteger("Orientation", ais.orientation);
		compound.setFloat("PositionXOffset", ais.bodyOffsetX);
		compound.setFloat("PositionYOffset", ais.bodyOffsetY);
		compound.setFloat("PositionZOffset", ais.bodyOffsetZ);
		NBTTagCompound nbt;
		if (job instanceof JobBard) {
			nbt = compound.getCompoundTag("Bard");
			job.save(nbt);
			compound.setTag("Bard", nbt);
		}
		if (role.getEnumType() == RoleType.COMPANION) {
			nbt = compound.getCompoundTag("Companion");
			role.save(nbt);
			compound.setTag("Companion", nbt);
		}
		if (this instanceof EntityCustomNpc) { compound.setTag("ModelData", ((EntityCustomNpc) this).modelData.save()); }
		compound.setBoolean("IsDead", dataManager.get(EntityNPCInterface.IsDead));
		compound.setInteger("DeathTime", deathTime);

		NBTTagList lookPosList = new NBTTagList();
		lookPosList.appendTag(new NBTTagFloat(lookPos[0]));
		lookPosList.appendTag(new NBTTagFloat(lookPos[1]));
		compound.setTag("LookPos", lookPosList);

		updateHitbox();
		compound.setFloat("BaseWidth", baseWidth);
		compound.setFloat("BaseHeight", baseHeight);
		compound.setFloat("Width", width);
		compound.setFloat("Height", height);

		// New from Unofficial (GoodBird)
		compound.setBoolean("MountControl", ais.mountControl);

		// animation
		compound.setInteger("AnimationType", animationType.ordinal());
		compound.setTag("Puppet", puppet.save(new NBTTagCompound()));
		return compound;
	}

	public void writeSpawnData(ByteBuf buffer) {
		new FriendlyByteBuf(buffer).writeNbt(writeSpawnData());
	}

	public float getEyeHeight() { return eyeHeight; }

	@SuppressWarnings("unused")
	public void addRidingEntity(Entity entity) {
		if (!hitboxRiding.containsKey(entity) && display.getHitboxState() == 2 && !isPassenger(entity)) {
			if (Math.abs(entity.getEntityBoundingBox().minY - getEntityBoundingBox().maxY) < 0.1) {
				hitboxRiding.put(entity, new double[] { posX - entity.posX, posZ - entity.posZ });
			}
		}
	}

	public void fall(float distance, float modifier) {
		for (Entity entity : hitboxRiding.keySet()) { entity.fall(distance, modifier); }
		if (!stats.noFallDamage || (role.getEnumType() == RoleType.FOLLOWER && role.isFollowing())) { return; }
		super.fall(distance, modifier);
	}


	public boolean canSee(Entity entity) { return getEntitySenses().canSee(entity); }

	@Override
	@SuppressWarnings("deprecation")
	public boolean canEntityBeSeen(@Nonnull Entity target) {
		if (ais.directLOS == EnumSeeTarget.DEAF) { return false; }
		try {
			double aggroRange = ValueUtil.correctDouble(stats.getAggroRange() / (isPlayerSleeping() ? 4.0d : 1.0d), 1.0d, 512.0d);
			IRayTraceRotate rtr = Util.instance.getAngles3D(posX, posY + getEyeHeight(), posZ, target.posX, target.posY + target.getEyeHeight(), target.posZ);
			if (rtr.getDistance() > aggroRange) { return false; }
			if (ais.directLOS == EnumSeeTarget.NONE) { return true; }
			boolean isDirect = ais.directLOS == EnumSeeTarget.WARY ||
					ais.directLOS == EnumSeeTarget.CALM ||
					ais.directLOS == EnumSeeTarget.REALISTIC;
			if (isDirect && isPlayerSleeping()) { return false; }
			if (ais.directLOS != EnumSeeTarget.BLIND) {
				IRayTraceResults rtrs = Util.instance.rayTraceBlocksAndEntitys(this, rtr.getYaw(), rtr.getPitch(), rtr.getDistance());
				if (rtrs != null) {
					for (DataBlock db : rtrs.getMCBlocks()) {
						if (!db.state.getBlock().isPassable(world, db.pos) ||
								(ais.directLOS != EnumSeeTarget.NORMAL && db.state.getBlock().isOpaqueCube(world.getBlockState(db.pos)))) { return false; }
					}
					rtrs.clear();
				}
			}
			if (isDirect) {
				double yaw = (rotationYawHead - rtr.getYaw()) % 360.0d;
				double pitch = (rotationPitch - rtr.getPitch()) % 360.0d;
				while (yaw < 0.0d) { yaw += 360.0d; }
				if ((yaw > 60.0d && yaw < 300.0d) || pitch > 60.0d || pitch < -60.0d) {
					if (ais.directLOS == EnumSeeTarget.WARY || ais.directLOS == EnumSeeTarget.REALISTIC) {
						double d0 = ValueUtil.correctDouble(aggroRange / 4.0d, 3.0d, aggroRange);
						double d1 = (target.isSneaking() ? 3.0d : 1.0d);
						double chance;
						if (rtr.getDistance() < d0) {
							chance = ValueUtil.correctDouble(( 0.9d * rtr.getDistance() / d0 + 1.0d) / d1, 0.0d, 1.0d);
						}
						else {
							if (target.isSneaking()) { return false; }
							double d2 = 0.1d / (4.0d - aggroRange);
							double d3 = 0.1d - d2 * 4.0d;
							chance = ValueUtil.correctDouble((d2 * rtr.getDistance() + d3) / d1 / 10.0d, 0.0d, 1.0d); // 10: time in tick
						}
						if (chance < Math.random()) { return false; }
					}
					else { return false; }
				}
			}
			if (target instanceof EntityLiving) {
				EntityLiving living = (EntityLiving) target;
				PotionEffect effect = living.getActivePotionEffect(MobEffects.INVISIBILITY);
				if (effect != null && ais.directLOS != EnumSeeTarget.NORMAL) {
					double invisible = 1.0d + effect.getAmplifier();
					double chance = ValueUtil.correctDouble(invisible == 0 ? 1.0d :
									(-0.00026d * Math.pow(invisible, 3.0d) + 0.00489d * Math.pow(invisible, 2.0d) - 0.03166d * invisible + 0.08d),
							0.002d, 1.0d);
					if (chance != 1.0d) { chance *= -1.0d * (rtr.getDistance() / aggroRange) + 1.0d; } // distance
					if (chance != 1.0d && target.isSneaking()) { chance *= 0.3d; } // is sneaking
					return ValueUtil.correctDouble(chance, 0.0d, 1.0d) > Math.random();
				}
			}
			return true;
		}
		catch (Exception ignored) { }
		return false;
	}

	// New from Unofficial (GoodBird)
	public void setInvisible(EntityPlayerMP player) {
		if (tracking.contains(player.getEntityId())) {
			tracking.remove(player.getEntityId());
			Packets.send(player, new PacketNpcVisibleFalse(this));
		}
	}

	public void setVisible(EntityPlayerMP player) {
		if (!tracking.contains(player.getEntityId())) {
			tracking.add(player.getEntityId());
			EntityRegistry.EntityRegistration er = EntityRegistry.instance().lookupModSpawn(this.getClass(), false);
			if (er != null) {
				Packets.send(player, new PacketNpcVisibleTrue(this, new FMLMessage.EntitySpawnMessage(er, this, er.getContainer())));
			}
		}
		Packets.send(player, new PacketNpcUpdate(getEntityId(), writeSpawnData()));
		MarkData.get(this).syncClients();
	}

	@Nullable
	@Override
	public EntityLiving getControllingPassenger() {
		return !getPassengers().isEmpty() && getPassengers().get(0) instanceof EntityLiving && ais.mountControl ? (EntityLiving) getPassengers().get(0) : null;
	}

	// New from Unofficial (BetaZavr)
	@Nullable
	public AxisAlignedBB getCollisionBoundingBox() {
		if (display.getHitboxState() == 2) { return getEntityBoundingBox(); }
		return null;
	}

	public AxisAlignedBB getCollisionBox(@Nonnull Entity entity) {
		/*if (display.getHitboxState() == 2 && !isPassenger(entity)) {
			return entity.getEntityBoundingBox();
		}*/
		return null;
	}

	public void setPose(int id, boolean bo) { setFlag(id, bo); }

	public boolean hasPose(int id) { return getFlag(id); }

}
