package noppes.npcs.entity;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.DataItem;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.server.ServerLifecycleHooks;
import noppes.npcs.*;
import noppes.npcs.ai.CombatHandler;
import noppes.npcs.ai.EntityAIAnimation;
import noppes.npcs.ai.attack.*;
import noppes.npcs.ai.EntityAIBustDoor;
import noppes.npcs.ai.EntityAIFindShade;
import noppes.npcs.ai.movement.EntityAIFollow;
import noppes.npcs.ai.EntityAIJob;
import noppes.npcs.ai.EntityAILook;
import noppes.npcs.ai.movement.EntityAIMoveIndoors;
import noppes.npcs.ai.movement.EntityAIMovingPath;
import noppes.npcs.ai.movement.EntityAINpcPanic;
import noppes.npcs.ai.movement.EntityAIReturn;
import noppes.npcs.ai.EntityAIRole;
import noppes.npcs.ai.movement.EntityAISprintToTarget;
import noppes.npcs.ai.EntityAITransform;
import noppes.npcs.ai.movement.EntityAIWander;
import noppes.npcs.ai.target.EntityAIWatchClosest;
import noppes.npcs.ai.movement.EntityAIWaterNav;
import noppes.npcs.ai.EntityAIWorldLines;
import noppes.npcs.ai.FlyingMoveHelper;
import noppes.npcs.ai.movement.NpcGroundPathNavigator;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.ai.target.EntityAIClearTarget;
import noppes.npcs.ai.target.EntityAIOwnerHurtByTarget;
import noppes.npcs.ai.target.EntityAIOwnerHurtTarget;
import noppes.npcs.ai.target.EntityAIClosestTarget;
import noppes.npcs.api.IChatMessages;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.constants.*;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IProjectile;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.util.IRayTraceResults;
import noppes.npcs.api.util.IRayTraceRotate;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.api.wrapper.NPCWrapper;
import noppes.npcs.api.wrapper.data.DataBlock;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.ISynchedEntityData;
import noppes.npcs.client.SkinUtil;
import noppes.npcs.client.parts.ModelData;
import noppes.npcs.client.parts.ModelPartConfig;
import noppes.npcs.constants.EnumNPCAnimationType;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.constants.EnumSeeTarget;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.LinkedNpcController;
import noppes.npcs.controllers.VisibilityController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.data.*;
import noppes.npcs.items.ItemNpcMovingPath;
import noppes.npcs.items.ItemSoulstoneFilled;
import noppes.npcs.mixin.world.entity.ILivingEntityMixin;
import noppes.npcs.mixin.world.entity.IEntityMixin;
import noppes.npcs.mixin.world.entity.ai.goal.IGoalSelectorMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.*;
import noppes.npcs.packets.server.SPacketNpcInitData;
import noppes.npcs.roles.*;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.GameProfileAlt;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

public abstract class EntityNPCInterface
        extends PathfinderMob
        implements IEntityAdditionalSpawnData, RangedAttackMob {

   public static final EntityDataAccessor<Boolean> Attacking = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.BOOLEAN);
   protected static final EntityDataAccessor<Integer> Animation = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<String> RoleData = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> JobData = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> FactionData = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> Walking= SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> Interacting= SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> IsDead= SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.BOOLEAN);
   public static final GameProfileAlt CommandProfile = new GameProfileAlt();
   public static final GameProfileAlt ChatEventProfile = new GameProfileAlt();
   public static final GameProfileAlt GenericProfile = new GameProfileAlt();
   public static FakePlayer ChatEventPlayer;
   public static FakePlayer CommandPlayer;
   public static FakePlayer GenericPlayer;
   public NPCWrapper<EntityNPCInterface> wrappedNPC;
   public final DataAbilities abilities = new DataAbilities(this);
   public DataDisplay display = new DataDisplay(this);
   public DataStats stats = new DataStats(this);
   public DataInventory inventory = new DataInventory(this);
   public final DataAI ais = new DataAI(this);
   public final DataAdvanced advanced = new DataAdvanced(this);
   public final DataScript script = new DataScript(this);
   public final DataTransform transform = new DataTransform(this);
   public final DataTimers timers = new DataTimers(this);
   public CombatHandler combatHandler = new CombatHandler(this);
   public String linkedName = "";
   public long linkedLast = 0L;
   public LinkedNpcController.LinkedData linkedData;
   public EntityDimensions baseSize = new EntityDimensions(0.6F, 1.8F, false);
   public float scaleX = 0.9375F;
   public float scaleY = 0.9375F;
   public float scaleZ = 0.9375F;
   private boolean wasKilled = false;
   public RoleInterface role = RoleInterface.NONE;
   public JobInterface job = JobInterface.NONE;
   public int[] dialogs = new int[0];
   public boolean hasDied = false;
   public long killedTime = 0L;
   public long totalTicksAlive = 0L;
   private int taskCount = 1;
   public int lastInteract = 0;
   public Faction faction;
   public EntityAILook lookAi;
   public EntityAIAnimation animateAi;
   public List<LivingEntity> interactingEntities = new ArrayList<>();
   public ResourceLocation textureLocation = null;
   public ResourceLocation textureGlowLocation = null;
   public ResourceLocation textureCloakLocation = null;
   public int currentAnimation = 0;
   public int animationStart = 0;
   public int npcVersion = VersionCompatibility.ModRev;
   public IChatMessages messages;
   public boolean updateClient = false;
   public boolean updateAI = false;
   public final ServerBossEvent bossInfo;
   public double prevChasingPosX;
   public double prevChasingPosY;
   public double prevChasingPosZ;
   public double chasingPosX;
   public double chasingPosY;
   public double chasingPosZ;

   // New from Unofficial (GoodBird)
   public final HashSet<Integer> tracking = new HashSet<>();
   private double startYPos = -6666.0D;

   // New from Unofficial (BetaZavr)
   public static final EntityDataAccessor<Float> AimRotationYaw = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.FLOAT); // fix bug while aiming
   protected long initTime;
   public DataAnimation animation;
   public EntityAICustom aiAttackTarget = null;
   public EntityNPCInterface aiOwnerNPC;
   public Path navigating;
   public ResourceKey<Level> homeDimensionId;
   public EnumNPCAnimationType animationType = EnumNPCAnimationType.PUPPET;
   public JobPuppet puppet = new JobPuppet(this);
   public Entity lookAt = null;
   @SuppressWarnings("unused")
   public float[] lookPos = new float[] { 0.0f, 0.0f };
   public boolean updateLook = false;

   public EntityNPCInterface(EntityType<? extends PathfinderMob> type, Level level) {
      super(type, level);
      wrappedNPC = new NPCWrapper<>(this);
      registerBaseAttributes();
      if (!CustomNpcs.DefaultInteractLine.isEmpty()) { advanced.interactLines.lines.put(0, new Line(CustomNpcs.DefaultInteractLine)); }
      xpReward = 0;
      faction = getFaction();
      setFaction(faction.id);
      updateAI = true;
      bossInfo = new ServerBossEvent(getDisplayName(), BossBarColor.PURPLE, BossBarOverlay.PROGRESS);
      bossInfo.setVisible(false);
      // New from Unofficial (BetaZavr)
      if (isClientSide()) { SkinUtil.checkTexture(this); }
      initTime = System.currentTimeMillis();
      animation.tryRunAnimation(AnimationKind.INIT);
      homeDimensionId = level.dimension();
      hurtDuration = ais.getMaxHurtResistantTime();
   }

   @Override
   @SuppressWarnings("deprecation")
   public boolean canBreatheUnderwater() { return ais.movementType == 2; }

   @Override
   @SuppressWarnings("deprecation")
   public boolean isPushedByFluid() { return ais.movementType != 2; }

   @Nullable
   @Override
   public LivingEntity getControllingPassenger() {
      return !getPassengers().isEmpty() && getPassengers().get(0) instanceof LivingEntity && ais.mountControl ? (LivingEntity)getPassengers().get(0) : null;
   }

   private void registerBaseAttributes() {
      if (animation == null) { animation = new DataAnimation(this); }
      Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(stats.maxHealth);
      Objects.requireNonNull(getAttribute(Attributes.FOLLOW_RANGE)).setBaseValue(CustomNpcs.NpcNavRange);
      Objects.requireNonNull(getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(getSpeed());
      Objects.requireNonNull(getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(stats.melee.getStrength());
      Objects.requireNonNull(getAttribute(Attributes.FLYING_SPEED)).setBaseValue(getSpeed() * 2.0F);
   }

   public static @Nonnull Builder createMobAttributes() {
      return LivingEntity.createLivingAttributes().add(Attributes.ATTACK_DAMAGE).add(Attributes.FLYING_SPEED).add(Attributes.FOLLOW_RANGE);
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      entityData.define(RoleData, "");
      entityData.define(JobData, "");
      entityData.define(FactionData, 0);
      entityData.define(Animation, 0);
      entityData.define(Walking, false);
      entityData.define(Interacting, false);
      entityData.define(IsDead, false);
      entityData.define(Attacking, false);
      entityData.define(AimRotationYaw, 361.0f);
   }

   @Override
   @SuppressWarnings("ConstantConditions")
   public boolean isAlive() {
      boolean bo = super.isAlive();
      if (!bo || ais != null && ais.aiDisabled) { return bo; }
      return !isKilled();
   }

   @Override
   public void tick() {
      super.tick();
      if (animation != null) { animation.updateTime(); }
      if (!ais.aiDisabled && tickCount % 10 == 0) {
         // fixing NPC data leak when initializing from the server
         if (initTime != 0L && isClientSide() && initTime < System.currentTimeMillis() - 1000L) {
            Packets.sendServer(new SPacketNpcInitData(getId()));
            initTime = 0L;
         }
         // hitbox when dead
         if (!isKilled() && getBbWidth() <= 1.0E-5f) { refreshDimensions(); }
         // path change
         if (!isClientSide()) {
            Path path = getNavigation().getPath();
            if (path != null) {
               Node fp = path.getEndNode();
               BlockPos pos = blockPosition();
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
         startYPos = calculateStartYPos(ais.startPos()) + 1.0D;
         if (startYPos < (double) level().getMinBuildHeight() && !isClientSide()) { discard(); }
         // to script event
         EventHooks.onNPCTick(this);
      }
      // reset creative player's target
      if (deathTime > 0 || (getTarget() instanceof Player player && player.isCreative())) {
         super.setTarget(null);
         updateTargetClient();
      }
      // timers
      if (!ais.aiDisabled) { timers.update(); }
      // refresh hitbox
      if (!isClientSide()) {
         if (wasKilled != isKilled()) {
            deathTime = 0;
            refreshDimensions();
         }
      }
      // if killed
      wasKilled = isKilled();
      if (currentAnimation == 14) { deathTime = 19; }
   }

   @Override
   public boolean dismountsUnderwater() { return false; }

   @Override
   public boolean doHurtTarget(@Nonnull Entity entity) {
      if (animateAi != null && !isClientSide()) {
         return animateAi.playAttackEntityCustomAnimation(entity);
      }
      return tryAttackEntityAsMob(entity, 0);
   }

   public boolean tryAttackEntityAsMob(Entity target, int frameID) {
      if (ais.aiDisabled || target == null || !target.isAlive()) { return false; }
      Set<Entity> entityList = new HashSet<>();
      entityList.add(target);
      if (CustomNpcs.ShowCustomAnimation && !isClientSide() && animation.isAnimated(AnimationKind.ATTACKING)) {
         List<AABB> aabbs = animation.getAnimation().getDamageHitboxes(this, frameID);
         if (aabbs.isEmpty()) { // only target
            double range = stats.melee.getRange();
            double minRange = (getBbWidth() + target.getBbWidth()) * 0.425d; // (/ 2.0 * 0.85)
            double yaw = Math.abs(Util.instance.getVector3D(getX(), getY(), getZ(), target.getX(), target.getY(), target.getZ()).getYaw());
            if (distanceTo(target) - minRange > range || yaw > 60.0d) { return false; }
         }
         else { // custom targets
            for (AABB aabb : aabbs) {
               List<Entity> list = new ArrayList<>();
               try { list = level().getEntitiesOfClass(Entity.class, aabb); } catch (Exception ignored) { }
               entityList.addAll(list);
            }
            entityList.remove(this);
         }
      }
      float amount = (float)stats.melee.getStrength();
      DamageSource damageSource = NpcDamageSource.create(this);
      boolean attackEntity = false;
      for (Entity entity : entityList) {
         if (stats.melee.getDelay() < 10) { entity.invulnerableTime = 0; }
         if (entity instanceof LivingEntity) {
            NpcEvent.MeleeAttackEvent event = new NpcEvent.MeleeAttackEvent(wrappedNPC, (LivingEntity) entity, amount);
            if (EventHooks.onNPCAttacksMelee(this, event)) { return false; }
            amount = event.damage;
         }
         attackEntity = entity.hurt(damageSource, amount);
         if (attackEntity) {
            if (getOwner() instanceof Player && entity instanceof LivingEntity) { ((ILivingEntityMixin) entity).setLastHurtByPlayerTime(100); }
            if (stats.melee.getKnockback() > 0) {
               entity.push(-Mth.sin(getYRot() * 3.1415927F / 180.0F) * (float)stats.melee.getKnockback() * 0.5F,
                       0.1D,
                       Mth.cos(getYRot() * 3.1415927F / 180.0F) * (float)stats.melee.getKnockback() * 0.5F);
               setDeltaMovement(getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
            }
            if (role.getEnumType() == RoleType.COMPANION) { ((RoleCompanion) role).attackedEntity(entity); }
         }
         if (stats.melee.getEffectType() != 0) {
            if (stats.melee.getEffectType() != 666) {
               if (entity instanceof LivingEntity living) {
                  living.addEffect(new MobEffectInstance(Objects.requireNonNull(PotionEffectType.getMCType(stats.melee.getEffectType())), stats.melee.getEffectTime() * 20, stats.melee.getEffectStrength()));
               }
            }
            else { entity.setRemainingFireTicks(stats.melee.getEffectTime() * 20); }
         }
      }
      return attackEntity;
   }

   @Override
   protected float tickHeadTurn(float targetYaw, float movementSpeed) {
      if (!isAlive()) { return 0.0F; }
      return super.tickHeadTurn(targetYaw, movementSpeed);
   }

   @Override
   @SuppressWarnings("deprecation")
   public void aiStep() { // old: onLivingUpdate()
      if (CustomNpcs.FreezeNPCs) { return; }
      if (isNoAi()) { super.aiStep(); }
      else {
         ++totalTicksAlive;
         updateSwingTime();
         if (tickCount % 20 == 0) {
            faction = getFaction();
            if (!interactingEntities.isEmpty()) {
               interactingEntities.removeIf(entity -> entity == null || (entity instanceof ServerPlayer sp && sp.hasDisconnected()));
            }
            if (!tracking.isEmpty()) {
               tracking.removeIf(id -> {
                  Entity entity = level().getEntity(id);
                  return entity == null || (entity instanceof ServerPlayer sp && sp.hasDisconnected());
               });
            }
         }
         if (!isClientSide()) {
            if (!ais.aiDisabled) {
               if (aiAttackTarget != null) { aiAttackTarget.update(); }
               if (!isKilled() && tickCount % 20 == 0) {
                  advanced.scenes.update();
                  // heal
                  if (getHealth() < getMaxHealth()) {
                     if (stats.healthRegen > 0 && !isAttacking()) {
                        heal((float)stats.healthRegen);
                        if (CustomNpcs.ShowHealingParticles) {
                           level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight(), getZ(), getBbWidth() / 3.0d, 0.05d, getBbWidth() / 3.0d);
                        }
                     }
                     if (stats.combatRegen > 0 && isAttacking()) {
                        heal((float)stats.combatRegen);
                        if (CustomNpcs.ShowHealingParticles) {
                           level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight(), getZ(), getBbWidth() / 3.0d, 0.05d, getBbWidth() / 3.0d);
                        }
                     }
                  }
                  // mob attacking
                  if (faction.getsAttacked && !isAttacking()) {
                     // New from Unofficial (BetaZavr)
                     AttributeInstance attribute = getAttribute(Attributes.FOLLOW_RANGE);
                     double range = attribute == null ? 16.0d : attribute.getValue();
                     for (Monster mob : level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(range, range, range))) {
                        if (mob.getTarget() == null && mob.hasLineOfSight(this)) { mob.setTarget(this); }
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
            if (getHealth() <= 0.0F && !isKilled()) {
               removeAllEffects();
               entityData.set(IsDead, true);
               updateTasks();
               refreshDimensions();
            }
            if (display.getBossbar() == 2) { bossInfo.setVisible(getTarget() != null); }
            entityData.set(Walking, !getNavigation().isDone());
            entityData.set(Interacting, isInteracting());
            combatHandler.update();
            onCollide();

            // New from Unofficial (BetaZavr)
            if (updateLook) { Packets.sendAll(new PacketNpcLookPos(level().dimension(), getId(), lookAt == null ? -1 : lookAt.getId())); }
         }
         else {
            // New from Unofficial (BetaZavr)
            EntityDataAccessor<Byte> hand_states = ILivingEntityMixin.getHandStates();
            ItemStack stack = getMainHandItem();
            // imitation of using items
            if (hand_states != null &&
                    (stack.getItem() instanceof BowItem || stack.getItem() instanceof ShieldItem || stack.getItem() instanceof FishingRodItem)) {
               if (currentAnimation == 6 && entityData.get(hand_states) != 1) {
                  if (entityData.get(hand_states) != 1) {
                     entityData.set(hand_states, (byte) 1);
                     startUsingItem(InteractionHand.MAIN_HAND);
                  }
               } else if (currentAnimation != 6 && entityData.get(hand_states) == 1) {
                  releaseUsingItem();
                  entityData.set(hand_states, (byte) 0);
               }
               stack.getItem().inventoryTick(stack, level(), this, 0, true);
            }
         }
         // New from Unofficial (BetaZavr)
         if (animateAi != null && !isClientSide()) { animateAi.livingUpdate(); }
         if (wasKilled != isKilled() && wasKilled) { reset(); }
         if (level().isDay() && !isClientSide() && stats.burnInSun) {
            float f = getLightLevelDependentMagicValue();
            if (f > 0.5F && random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && level().canSeeSky(blockPosition())) { setRemainingFireTicks(160); }
         }
         super.aiStep();
         if (isAttacking() && getTarget() != null) { entityData.set(EntityNPCInterface.AimRotationYaw, getYHeadRot()); }
         if (isClientSide()) {
            role.clientUpdate();
            if (textureCloakLocation != null) { cloakUpdate(); }
            if (currentAnimation != entityData.get(Animation)) {
               currentAnimation = entityData.get(Animation);
               animationStart = tickCount;
               refreshDimensions();
            }
            if (!ais.aiDisabled && job instanceof JobBard jobBard) { jobBard.aiStep(); }
         }
         if (display.getBossbar() > 0) { bossInfo.setProgress(getHealth() / getMaxHealth()); }
      }
   }

   public void updateClient() {
      Packets.sendNearby(this, new PacketNpcUpdate(getId(), writeSpawnData()));
      updateClient = false;
      updateNavClient();
   }

   public void updateNavClient() {
      if (!isClientSide()) {
         Packets.sendNearby(level(), getOnPos(), 160, new PacketNpcNavigation(getId(), navigating));
      }
   }

   public void updateTargetClient() {
      if (!isClientSide()) {
         Packets.sendNearby(level(), getOnPos(), 160, new PacketNpcTarget(getId(), getTarget() == null ? -1 : getTarget().getId()));
      }
   }

   @Override
   protected @Nonnull InteractionResult mobInteract(@Nonnull Player player, @Nonnull InteractionHand hand) {
      if (level().isClientSide) { return isAttacking() ? InteractionResult.FAIL : InteractionResult.PASS; }
      else if (hand != InteractionHand.MAIN_HAND) { return InteractionResult.PASS; }
      else if (CustomNpcs.EnableInvisibleNpcs && CustomNpcs.InvisibilityAlgorithm == 2 && !display.isVisibleTo(player) && !player.isSpectator() && player.getMainHandItem().getItem() != CustomItems.wand) { return InteractionResult.PASS; }
      else {
         ItemStack stack = player.getItemInHand(hand);
         Item item = stack.getItem();
         if (item == CustomItems.cloner || item == CustomItems.wand || item == CustomItems.mount || item == CustomItems.scripter) {
            if (getTarget() != null) { setTarget(null);}
            setLastHurtByMob(null);
            return InteractionResult.SUCCESS;
         }
         if (item == CustomItems.moving) {
            if (getTarget() != null) { setTarget(null); }
            ItemNpcMovingPath.register(this, stack, player);
            return InteractionResult.SUCCESS;
         }
         if (!ais.aiDisabled && EventHooks.onNPCInteract(this, player)) { return InteractionResult.FAIL; }
         if (!getFaction().isAggressiveToPlayer(player) && !isAttacking()) {
            addInteract(player);
            if (animateAi != null && (lookAi == null || !lookAi.fastRotation) && !isClientSide()) { animateAi.playInteractCustomAnimation(); }
            Dialog dialog = getDialog(player);
            QuestData data = PlayerData.get(player).questData.getQuestCompletion(player, this);
            if (data != null) { Packets.send((ServerPlayer)player, new PacketQuestCompletion(data.quest.id)); }
            else if (dialog != null) { NoppesUtilServer.openDialog(player, this, dialog); }
            else if (!ais.aiDisabled && role.getType() != 0) { role.interact(player); }
            else { say(player, advanced.getInteractLine()); }
            return InteractionResult.PASS;
         }
         else { return InteractionResult.FAIL; }
      }
   }

   public void addInteract(LivingEntity entity) {
      if (ais.stopAndInteract && !isAttacking() && entity.isAlive() && !isNoAi()) {
         if (tickCount - lastInteract < 180) { interactingEntities.clear(); }
         getNavigation().stop();
         lastInteract = tickCount;
         if (!interactingEntities.contains(entity)) { interactingEntities.add(entity); }
      }
   }

   public boolean isInteracting() {
      if (tickCount - lastInteract < 40 || isClientSide() && entityData.get(Interacting)) { return true; }
      return ais.stopAndInteract && !interactingEntities.isEmpty() && tickCount - lastInteract < 180;
   }

   private Dialog getDialog(Player player) {
      Set<Integer> newDS = new HashSet<>();
      Dialog dialog = null;
      for (int dialogId : dialogs) {
         if (!DialogController.instance.hasDialog(dialogId)) { continue; }
         newDS.add(dialogId);
         if (dialog != null) { continue; }
         Dialog d = DialogController.instance.get(dialogId);
         if (d.availability.isAvailable(player)) { dialog = d; }
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

   @Override
   public boolean hurt(@Nonnull DamageSource damagesource, float damage) {
      if (aiAttackTarget != null && aiAttackTarget.damaged()) { return false; }
      if (level().isClientSide() || CustomNpcs.FreezeNPCs || damagesource.getMsgId().equals("inWall")) { return false; }
      if (damagesource.getClass().getSimpleName().equals("TGDamageSource") && damage < 0.5f) { return false; }
      if (role.getEnumType() == RoleType.FOLLOWER && role.isFollowing() && damagesource.getMsgId().equals("fall")) { return false; } // fix

      if (damagesource.getMsgId().equals("outOfLevel") && isKilled()) { reset(); }
      damage = stats.resistances.applyResistance(damagesource, damage);
      if (!combatHandler.canDamage(damagesource, damage)) { return false; }

      Entity entity = NoppesUtilServer.getDamageSource(damagesource);
      LivingEntity attackingEntity = null;
      if (entity instanceof LivingEntity) { attackingEntity = (LivingEntity)entity; }
      if (attackingEntity != null && attackingEntity == getOwner()) { return false; }
      if (attackingEntity instanceof EntityNPCInterface npc) {
         if (npc.faction.id == faction.id) { return false; }
         if (npc.getOwner() instanceof Player) { hurtTime = 100; }
      }
      else if (attackingEntity instanceof Player && faction.isFriendlyToPlayer((Player)attackingEntity)) {
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
      //else if (attackingEntity == null) { return super.hurt(damagesource, damage); }

      boolean isHurt = false;
      if (attackingEntity == null) {
         isHurt = customHurt(damagesource, damage);
      }
      else {
         try {
            boolean check = false;
            if (!(attackingEntity instanceof Player player) || !player.isCreative()) {
               if (damage > 0.0f) {
                  List<EntityNPCInterface> inRange = new ArrayList<>();
                  try { inRange = level().getEntitiesOfClass(EntityNPCInterface.class, getBoundingBox().inflate(32.0, 16.0, 32.0)); } catch (Exception ignored) { }
                  for (EntityNPCInterface npc : inRange) {
                     if (npc.equals(this)) { continue; }
                     npc.advanced.tryDefendFaction(faction.id, this, attackingEntity);
                  }
               }
               if (isAttacking()) {
                  if (getTarget() != null && distanceToSqr(getTarget()) > distanceToSqr(attackingEntity)) { setTarget(attackingEntity); }
                  isHurt = customHurt(damagesource, damage);
                  check = true;
               }
               else if (damage > 0.0f) { setTarget(attackingEntity); }
            }
            if (!check) { isHurt = customHurt(damagesource, damage); }
         }
         finally {
            if (event.clearTarget) {
               setTarget(null);
               setLastHurtByMob(null);
            }
         }
      }
      if (!isKilled()) {
         if (isHurt && damage > 0.0f) {
            if (animateAi != null && !isClientSide()) { animateAi.playHitCustomAnimation(); }
         }
         else if (!damagesource.is(DamageTypeTags.IS_PROJECTILE) && attackingEntity != null) {
            if (animateAi != null && !isClientSide()) { animateAi.playBlockedCustomAnimation(); }
            blockUsingShield(attackingEntity);
         }
      }
      return isHurt;
   }

   private boolean customHurt(@Nonnull DamageSource source, float amount) {
      if (!net.minecraftforge.common.ForgeHooks.onLivingAttack(this, source, amount)) return false;
      if (isInvulnerableTo(source)) { return false; }
      if (level().isClientSide) { return false; }
      if (isDeadOrDying()) { return false; }
      if (source.is(DamageTypeTags.IS_FIRE) && hasEffect(MobEffects.FIRE_RESISTANCE)) { return false; }
      if (isSleeping() && !level().isClientSide) { stopSleeping(); }
      noActionTime = 0;
      float f = amount;
      boolean isBlockedDamage = false;
      if (amount > 0.0F && isDamageSourceBlocked(source)) {
         net.minecraftforge.event.entity.living.ShieldBlockEvent ev = net.minecraftforge.common.ForgeHooks.onShieldBlock(this, source, amount);
         if(!ev.isCanceled()) {
            if(ev.shieldTakesDamage()) hurtCurrentlyUsedShield(amount);
            amount -= ev.getBlockedDamage();
            if (!source.is(DamageTypeTags.IS_PROJECTILE)) {
               Entity entity = source.getDirectEntity();
               if (entity instanceof LivingEntity livingentity) { blockUsingShield(livingentity); }
            }
            isBlockedDamage = amount <= 0;
         }
      }
      if (source.is(DamageTypeTags.IS_FREEZING) && getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) { amount *= 5.0F; }
      walkAnimation.setSpeed(1.5F);
      boolean damageCanBeDone = true;
      int f0 = ais.getMaxHurtResistantTime();
      if (f0 != 0 && invulnerableTime > f0 / 2.0f && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
         if (amount <= lastHurt) { return false; }
         actuallyHurt(source, amount - lastHurt);
         lastHurt = amount;
         damageCanBeDone = false;
      }
      else {
         lastHurt = amount;
         invulnerableTime = f0;
         actuallyHurt(source, amount);
         hurtDuration = f0 / 2;
         hurtTime = hurtDuration;
      }
      if (source.is(DamageTypeTags.DAMAGES_HELMET) && !getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
         hurtHelmet(source, amount);
         amount *= 0.75F;
      }
      Entity entity1 = source.getEntity();
      if (entity1 != null) {
         if (entity1 instanceof LivingEntity livingentity) {
            if (!source.is(DamageTypeTags.NO_ANGER)) { setLastHurtByMob(livingentity); }
         }
         if (entity1 instanceof Player player) {
            lastHurtByPlayerTime = 100;
            lastHurtByPlayer = player;
         }
         else if (entity1 instanceof TamableAnimal tamableEntity && tamableEntity.isTame()) {
            lastHurtByPlayerTime = 100;
            LivingEntity owner = tamableEntity.getOwner();
            if (owner instanceof Player ownerPlayer) { lastHurtByPlayer = ownerPlayer; }
            else { lastHurtByPlayer = null; }
         }
      }
      if (damageCanBeDone) {
         if (isBlockedDamage) { level().broadcastEntityEvent(this, (byte)29); }
         else { level().broadcastDamageEvent(this, source); }
         if (!source.is(DamageTypeTags.NO_IMPACT) && (!isBlockedDamage || amount > 0.0F)) { markHurt(); }
         if (entity1 != null && !source.is(DamageTypeTags.IS_EXPLOSION)) {
            double d0 = entity1.getX() - getX();

            double d1;
            for(d1 = entity1.getZ() - getZ(); d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
               d0 = (Math.random() - Math.random()) * 0.01D;
            }

            knockback(0.4F, d0, d1);
            if (!isBlockedDamage) {
               indicateDamage(d0, d1);
            }
         }
      }
      if (isDeadOrDying()) {
         if (!((ILivingEntityMixin) this).invokeCheckTotemDeathProtection(source)) {
            SoundEvent soundevent = getDeathSound();
            if (damageCanBeDone && soundevent != null) { playSound(soundevent, getSoundVolume(), getVoicePitch()); }
            die(source);
         }
      } else if (damageCanBeDone) {
         playHurtSound(source);
      }
      boolean isDamaged = !isBlockedDamage || amount > 0.0F;
      if (isDamaged) {
         ((ILivingEntityMixin) this).setLastDamageSource(source);
         ((ILivingEntityMixin) this).setLastDamageStamp(level().getGameTime());
      }
      if (entity1 instanceof ServerPlayer) {
         CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer)entity1, this, source, f, amount, isBlockedDamage);
      }
      return isDamaged;
   }

   @Override
   protected void actuallyHurt(@Nonnull DamageSource damageSrc, float damageAmount) {
      super.actuallyHurt(damageSrc, damageAmount);
      combatHandler.damage(damageSrc, damageAmount);
   }

   public void onAttack(LivingEntity entity) {
      if (!ais.aiDisabled && entity != null && entity != this && !isAttacking() && ais.onAttack != 3 && entity != getOwner()) {
         super.setTarget(entity);
         updateTargetClient();
      }
   }

   @Override
   public void setTarget(LivingEntity entity) { // old: setAttackTarget
      if ((!(entity instanceof Player player) || !player.getAbilities().invulnerable) && (entity == null || entity != getOwner()) && getTarget() != entity) {
         // New from Unofficial (BetaZavr): priority target
         if (getTarget() != null && combatHandler.priorityTarget != null) { return; }
         if (entity != null) {
            LivingEntity parent = entity;
            NpcEvent.TargetEvent event = new NpcEvent.TargetEvent(wrappedNPC, entity);
            if (EventHooks.onNPCTarget(this, event)) { return; }
            if (event.entity == null) { entity = null; }
            else { entity = event.entity.getMCEntity(); }
            // New from Unofficial (BetaZavr): new check
            if (!parent.equals(entity) &&
                    ((entity instanceof Player player && player.getAbilities().invulnerable) || entity == getOwner() || getTarget() == entity)) {
               return;
            }
         }
         else {
            for (WrappedGoal en : targetSelector.getAvailableGoals()) { en.stop(); }
            if (EventHooks.onNPCTargetLost(this, getTarget())) { return; }
         }
         if (entity != null && entity != this && ais.onAttack != 3 && !isAttacking() && !isClientSide()) {
            Line line = advanced.getAttackLine();
            if (line != null) { saySurrounding(Line.formatTarget(line, entity)); }
         }
         super.setTarget(entity);
         updateTargetClient();
      }
   }

   @Override
   public void performRangedAttack(@Nonnull LivingEntity entity, float f) {
      if (ais.aiDisabled) { return; }
      ItemStack proj = ItemStackWrapper.MCItem(inventory.getProjectile());
      if (proj == null) { updateAI = true; }
      else {
         NpcEvent.RangedLaunchedEvent event = new NpcEvent.RangedLaunchedEvent(wrappedNPC, entity, (float)stats.ranged.getStrength());
         for(int i = 0; i < stats.ranged.getShotCount(); ++i) {
            EntityProjectile projectile = shoot(entity, stats.ranged.getAccuracy(), proj, f == 1.0F);
            projectile.damage = event.damage;
            projectile.callback = (projectile_0, pos, entity1) -> {
               if (proj.getItem() == CustomItems.soulstoneFull) {
                  Entity e = ItemSoulstoneFilled.Spawn(null, proj, level(), pos);
                  if (e instanceof LivingEntity livingEntity && entity1 instanceof LivingEntity) {
                     if (livingEntity instanceof Mob mob) { mob.setTarget((LivingEntity)entity1); }
                     else { livingEntity.setLastHurtByMob((LivingEntity)entity1); }
                  }
               }
               SoundEvent se = stats.ranged.getSoundEvent((entity1 != null) ? 1 : 2);
               String sound = stats.ranged.getSound((entity1 != null) ? 1 : 2);
               float pitch = 1.2f / (random.nextFloat() * 0.2f + 0.9f);
               if (se != null) { projectile_0.playSound(se, 1.0f, pitch); }
               else if (!sound.isEmpty()) { Packets.sendNearby(level(), getOnPos(), 64,
                       new PacketPlaySound(sound, SoundSource.NEUTRAL, getX(), getY(), getZ(), 1.0f, pitch)); }
               return false;
            };
            SoundEvent se = stats.ranged.getSoundEvent(0);
            String sound = stats.ranged.getSound(0);
            if (se != null) { playSound(se, 2.0f, 1.0f); }
            else if (!sound.isEmpty()) { Packets.sendNearby(level(), getOnPos(), 64,
                    new PacketPlaySound(sound, SoundSource.NEUTRAL, getX(), getY(), getZ(), 2.0f, 1.0f)); }
            event.projectiles.add((IProjectile<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(projectile));
         }
         EventHooks.onNPCRangedLaunched(this, event);
      }
   }

   public EntityProjectile shoot(LivingEntity entity, int accuracy, ItemStack proj, boolean indirect) {
      return shoot(entity.getX(), entity.getBoundingBox().minY + (double)(entity.getBbHeight() / 2.0F), entity.getZ(), accuracy, proj, indirect);
   }

   public EntityProjectile shoot(double x, double y, double z, int accuracy, ItemStack proj, boolean indirect) {
      EntityProjectile projectile = new EntityProjectile(level(), this, proj.copy(), true);
      double varX = x - getX();
      double varY = y - getY() - (double) getEyeHeight();
      double varZ = z - getZ();
      float varF = projectile.hasGravity() ? (float)Math.sqrt(varX * varX + varZ * varZ) : 0.0F;
      float angle = projectile.getAngleForXYZ(varX, varY, varZ, varF, indirect);
      float acc = 20.0F - (float)Mth.floor((float)accuracy / 5.0F);
      projectile.shoot(varX, varY, varZ, angle, acc);
      level().addFreshEntity(projectile);
      if (animateAi != null && !isClientSide()) { animateAi.playShootCustomAnimation(); }
      return projectile;
   }

   private void clearTasks(GoalSelector tasks) {
      List<WrappedGoal> list = new ArrayList<>(tasks.getAvailableGoals());
      for (WrappedGoal entityAiTaskEntry : list) { tasks.removeGoal(entityAiTaskEntry); }
      tasks.getAvailableGoals().clear();
      ((IGoalSelectorMixin) tasks).getLockedFlags().clear();
      ((IGoalSelectorMixin) tasks).getDisabledFlags().clear();
   }

   private void updateTasks() {
       if (!level().isClientSide && level() instanceof ServerLevel) {
         clearTasks(goalSelector);
         clearTasks(targetSelector);
         if (!isKilled()) {
            targetSelector.addGoal(0, new EntityAIClearTarget(this));
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
            targetSelector.addGoal(2, new EntityAIClosestTarget<>(this, LivingEntity.class, 4, ais.directLOS, false, new NPCAttackSelector(this)));
            targetSelector.addGoal(3, new EntityAIOwnerHurtByTarget(this));
            targetSelector.addGoal(4, new EntityAIOwnerHurtTarget(this));
            if (ais.movementType == 1) {
               moveControl = new FlyingMoveHelper(this);
               if (!(navigation instanceof FlyingPathNavigation)) {
                  navigation = new FlyingPathNavigation(this, level()) {
                     public boolean isStableDestination(@Nonnull BlockPos blockPos) {
                        return true;
                     }
                  };
               }
            }
            else if (ais.movementType == 2) {
               moveControl = new FlyingMoveHelper(this);
               if (!(navigation instanceof WaterBoundPathNavigation)) { navigation = new WaterBoundPathNavigation(this, level()); }
            }
            else {
               moveControl = new MoveControl(this);
               if (!(navigation instanceof GroundPathNavigation)) { navigation = new NpcGroundPathNavigator(this, level()); }
               goalSelector.addGoal(0, new EntityAIWaterNav(this));
            }
            taskCount = 1;
            addRegularEntries();
            doorInteractType();
            seekShelter();
            setResponse();
            setMoveType();
         }
      }
   }

   @Override
   protected @Nonnull PathNavigation createNavigation(@Nonnull Level level) {
      return new NpcGroundPathNavigator(this, level);
   }

   private void setResponse() {
      aiAttackTarget = null;
      setPose(Pose.STANDING);
      //aiOwnerNPC = null;
      if (!ais.aiDisabled) {
         if (ais.canSprint) { goalSelector.addGoal(taskCount++, new EntityAISprintToTarget(this)); }
         if (ais.onAttack == 1) { goalSelector.addGoal(taskCount++, new EntityAINpcPanic(this, 1.2F)); }
         else if (ais.onAttack == 2) { goalSelector.addGoal(taskCount++, new EntityAIAvoidTarget(this)); }
         else if (ais.onAttack == 0) {
            if (ais.canLeap) { goalSelector.addGoal(taskCount++, new EntityAIPounceTarget(this)); } // can Jump
            switch (ais.tacticalVariant) {
               case RUSH: {
                  goalSelector.addGoal(taskCount++, (aiAttackTarget = new EntityAIOnslaught(this)));
                  break;
               }
               case STAGGER: {
                  goalSelector.addGoal(taskCount++, (aiAttackTarget = new EntityAIDodge(this)));
                  break;
               }
               case ORBIT: {
                  goalSelector.addGoal(taskCount++,
                          (aiAttackTarget = new EntityAISurround(this)));
                  break;
               }
               case HIT_AND_RUN: {
                  goalSelector.addGoal(taskCount++, (aiAttackTarget = new EntityAIHitAndRun(this)));
                  break;
               }
               case COMMANDER: {
                  goalSelector.addGoal(taskCount++, aiAttackTarget = new EntityAICommanderTarget(this));
                  break;
               }
               case STALK: {
                  goalSelector.addGoal(taskCount++, aiAttackTarget = new EntityAIStalkTarget(this));
                  break;
               }
               default: {
                  goalSelector.addGoal(taskCount++, (aiAttackTarget = new EntityAINoTactic(this)));
                  break;
               }
            }
         } // Attack
         //else if (ais.onAttack == 3) {}
      }
      else { goalSelector.addGoal(taskCount++, (aiAttackTarget = new EntityAIOnslaught(this))); }
   }

   public boolean canFly() { return navigation instanceof FlyingPathNavigation; }

   public void setMoveType() {
      if (ais.getMovingType() == 1) { goalSelector.addGoal(taskCount++, new EntityAIWander(this)); }
      if (ais.getMovingType() == 2) { goalSelector.addGoal(taskCount++, new EntityAIMovingPath(this)); }
   }

   public void doorInteractType() {
      if (navigation instanceof GroundPathNavigation) {
         Goal aiDoor = null;
         if (ais.doorInteract == 1) { goalSelector.addGoal(taskCount++, aiDoor = new OpenDoorGoal(this, true)); }
         else if (ais.doorInteract == 0) { goalSelector.addGoal(taskCount++, aiDoor = new EntityAIBustDoor(this)); }
         ((GroundPathNavigation)navigation).setCanOpenDoors(aiDoor != null);
      }
   }

   public void seekShelter() {
      if (!ais.aiDisabled) {
         if (ais.findShelter == 0) { goalSelector.addGoal(taskCount++, new EntityAIMoveIndoors(this)); }
         else if (ais.findShelter == 1) {
            if (!canFly()) { goalSelector.addGoal(taskCount++, new RestrictSunGoal(this)); }
            goalSelector.addGoal(taskCount++, new EntityAIFindShade(this));
         }
      }
   }

   public void addRegularEntries() {
      goalSelector.addGoal(taskCount++, new EntityAIReturn(this));
      goalSelector.addGoal(taskCount++, new EntityAIFollow(this));
      if (ais.getStandingType() != 1 && ais.getStandingType() != 3) {
         goalSelector.addGoal(taskCount++, new EntityAIWatchClosest(this, LivingEntity.class, 5.0F));
      }
      goalSelector.addGoal(taskCount++, lookAi = new EntityAILook(this));
      goalSelector.addGoal(taskCount++, new EntityAIWorldLines(this));
      if (!ais.aiDisabled) {
         goalSelector.addGoal(taskCount++, new EntityAIJob(this));
         goalSelector.addGoal(taskCount++, new EntityAIRole(this));
      }
      goalSelector.addGoal(taskCount++, animateAi = new EntityAIAnimation(this));
      if (transform.isValid()) { goalSelector.addGoal(taskCount++, new EntityAITransform(this)); }
   }

   @Override
   public float getSpeed() { return (float) ais.getWalkingSpeed() / 20.0F; }

   @Override
   protected float getWaterSlowDown() { return ais.movementType == 2 ? 0.95F : 0.8F; }

   @Override
   public float getWalkTargetValue(@Nonnull BlockPos pos) {
      if (ais.movementType == 2) { return isInWater() ? 10.0F : 0.0F; }
      else {
         float weight = (float)level().getLightEmission(pos) - 0.5F;
         if (level().getBlockState(pos).isSolidRender(level(), pos)) { weight += 10.0F; }
         return weight;
      }
   }

   @Override
   protected int decreaseAirSupply(int par1) { return !stats.canDrown ? par1 : super.decreaseAirSupply(par1); }

   @Override
   public @Nonnull MobType getMobType() { return stats == null ? MobType.UNDEFINED : stats.creatureType; }

   @Override
   public int getAmbientSoundInterval() { return 160; }

   @Override
   public void playAmbientSound() {
      if (isAlive()) { advanced.playSound(getTarget() != null ? 1 : 0, getSoundVolume(), getVoicePitch()); }
   }

   @Override
   protected void playHurtSound(@Nonnull DamageSource source) { advanced.playSound(2, getSoundVolume(), getVoicePitch()); }

   @Override
   public SoundEvent getDeathSound() { return null; }

   @Override
   public float getVoicePitch() { return advanced.disablePitch ? 1.0F : super.getVoicePitch(); }

   @Override
   protected void playStepSound(@Nonnull BlockPos pos, @Nonnull BlockState state) {
      if (advanced.getSound(4) != null) { advanced.playSound(4, 0.15F, 1.0F); }
      else { super.playStepSound(pos, state); }
   }

   public ServerPlayer getFakeChatPlayer() {
      if (level().isClientSide) { return null; }
      else {
         EntityUtil.Copy(this, ChatEventPlayer);
         ChatEventProfile.npc = this;
         ((IEntityMixin) ChatEventPlayer).setLevel(level());
         ChatEventPlayer.setPos(getX(), getY(), getZ());
         return ChatEventPlayer;
      }
   }

   @SuppressWarnings("UnstableApiUsage")
   public void saySurrounding(Line line) {
      if (line != null) {
         if (line.getShowText() && !line.getText().isEmpty()) {
            ServerChatEvent event = new ServerChatEvent(getFakeChatPlayer(), line.getText(), Component.translatable(line.getText().replace("%", "%%")));
            if (CustomNpcs.NpcSpeachTriggersChatEvent && (MinecraftForge.EVENT_BUS.post(event) || event.getMessage() == null)) { return; }
            line.setText(event.getMessage().getString().replace("%%", "%"));
         }
         List<Player> inRange = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(20.0D, 20.0D, 20.0D));
         for (Player player : inRange) { say(player, line); }
      }
   }

   public void say(Player playerIn, Line line) {
      if (line != null && playerIn instanceof ServerPlayer player && canSee(playerIn)) {
         if (!line.getSound().isEmpty()) {
            Packets.send(player, new PacketPlaySound(line.getSound(), SoundSource.NEUTRAL, getX(), getY(), getZ(), getSoundVolume(), getVoicePitch()));
         }
         if (!line.getText().replaceAll(" ", "").replaceAll("" + ((char) 9), "").isEmpty()) {
            Packets.send(player, new PacketChatBubble(getId(), Component.translatable(line.getText()), line.getShowText()));
         }
      }
   }

   @Override
   public boolean shouldShowName() { return true; }

   @Override
   public void push(double d, double d1, double d2) {
      if (isWalking() && !isKilled()) { super.push(d, d1, d2); }
   }

   @Override
   public void readAdditionalSaveData(@Nonnull CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      npcVersion = compound.getInt("ModRev");
      VersionCompatibility.CheckNpcCompatibility(this, compound);
      display.load(compound);
      stats.load(compound);
      ais.load(compound);
      script.load(compound);
      timers.load(compound);
      advanced.load(compound);
      role.load(compound);
      job.load(compound);
      inventory.load(compound);
      transform.load(compound);
      killedTime = compound.getLong("KilledTime");
      totalTicksAlive = compound.getLong("TotalTicksAlive");
      linkedName = compound.getString("LinkedNpcName");
      if (!isClientSide()) { LinkedNpcController.Instance.loadNpcData(this); }
      Objects.requireNonNull(getAttribute(Attributes.FOLLOW_RANGE)).setBaseValue(CustomNpcs.NpcNavRange);
      if (compound.contains("Puppet", Tag.TAG_COMPOUND)) { puppet.load(compound.getCompound("Puppet")); }
      updateAI = true;
   }

   @Override
   public void addAdditionalSaveData(@Nonnull CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      display.save(compound);
      stats.save(compound);
      ais.save(compound);
      script.save(compound);
      timers.save(compound);
      advanced.save(compound);
      role.save(compound);
      job.save(compound);
      inventory.save(compound);
      transform.save(compound);
      compound.putLong("KilledTime", killedTime);
      compound.putLong("TotalTicksAlive", totalTicksAlive);
      compound.putInt("ModRev", npcVersion);
      compound.putString("LinkedNpcName", linkedName);

      compound.put("Puppet", puppet.save(new CompoundTag()));
   }

   @Override
   public @Nonnull EntityDimensions getDimensions(@Nonnull Pose poseIn) {
      if (display.getHitboxState() == 1 || isKilled() && stats.hideKilledBody) { return EntityDimensions.scalable(1.0E-5F, baseSize.height); }
      float w = baseSize.width;
      float h = baseSize.height;
      float[] displaySize = display.getDimensions();
      if (displaySize[0] != 0.0f || displaySize[1] != 0.0f) {
         w = displaySize[0];
         h = displaySize[1];
      }
      if (((currentAnimation == AnimationType.SLEEP.get() || currentAnimation == AnimationType.CRAWL.get()) && !isAttacking())
              && deathTime <= 0) {
         w /= 0.75f;
         h /= 4.75f;
      }
      else if (isPassenger() || currentAnimation == AnimationType.SIT.get()) { h /= 1.3f; }
      if (display.getModel() == null && this instanceof EntityCustomNpc npc) {
         ModelData modeldata = npc.modelData;
         ModelPartConfig model = modeldata.getPartConfig(EnumParts.HEAD);
         float scaleHead = Math.max(model.scaleX, model.scaleZ);
         model = modeldata.getPartConfig(EnumParts.BODY);
         float scaleBody = Math.max(model.scaleX, model.scaleZ);
         w *= Math.max(scaleHead, scaleBody);
         w = w / 5.0f * display.getSize();
         h = h / 5.0f * display.getSize();
      }
      EntityDimensions size = new EntityDimensions(w, h, false);
      size = size.scale(display.getSize() * 0.2F);
      if (getHealth() <= 0.0f) {
         size = new EntityDimensions(size.height / 2.0f, size.width / 3.0f, false);
      }
      if ((double) (size.width / 2.0F) > level().getMaxEntityRadius()) { level().increaseMaxEntityRadius(size.width / 2.0F); }
      return size;
   }

   @Override
   public void tickDeath() { // old: onDeathUpdate
      if (stats.spawnCycle != 3 && stats.spawnCycle != 4) {
         ++deathTime;
         if (!isClientSide()) {
            if (!hasDied) { remove(RemovalReason.KILLED); }
            if (killedTime < System.currentTimeMillis() &&
                    (stats.spawnCycle == 0 || level().isDay() && stats.spawnCycle == 1 || !level().isDay() && stats.spawnCycle == 2)) {
               reset();
            }
         }
      }
      else { super.tickDeath(); }
   }

   public void reset(int delay) { CustomNPCsScheduler.runTack(this::reset, delay); }

   @SuppressWarnings("ConstantConditions")
   public void reset() {
      boolean needsSync = hasDied;
      hasDied = false;
      unsetRemoved();
      dead = false;
      revive();
      wasKilled = false;
      setSprinting(false);
      setHealth(getMaxHealth());
      entityData.set(Animation, 0);
      entityData.set(Walking, false);
      entityData.set(IsDead, false);
      entityData.set(Interacting, false);
      interactingEntities.clear();
      combatHandler.reset();
      setTarget(null);
      setLastHurtByMob(null);
      deathTime = 0;
      if (ais.returnToStart && emptyOwner() && !isClientSide() && !isPassenger()) {
         boolean isTransfer = false;
         double x = getStartXPos();
         double y = getStartYPos();
         double z = getStartZPos();
         if (!level().dimension().equals(homeDimensionId) && getServer() != null) {
            ServerLevel homeLevel = getServer().getLevel(homeDimensionId);
            if (homeLevel != null) {
               if (changeDimension(homeLevel) instanceof EntityNPCInterface cnpc) {
                  cnpc.moveTo(x, y, z, getYRot(), getXRot());
                  try { cnpc.setUUID(getUUID()); } catch (Exception ignored) { }
                  isTransfer = true;
               }
            }
         }
         if (!isTransfer) { moveTo(x, y, z, getYRot(), getXRot()); }
      }
      killedTime = 0L;
      clearFire();
      removeAllEffects();
      travel(Vec3.ZERO);
      walkDistO = walkDist = 0.0F;
      getNavigation().stop();
      currentAnimation = 0;
      refreshDimensions();
      updateAI = true;
      ais.movingPos = 0;
      if (getOwner() != null) { getOwner().setLastHurtMob(null); }
      bossInfo.setVisible(display.getBossbar() == 1);
      job.reset();
      EventHooks.onNPCInit(this);
      if (needsSync) {
         List<DataValue<?>> data = getEntityData().getNonDefaultValues();
         for(ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (display.isVisibleTo(player) || player.isSpectator() || player.getMainHandItem().getItem() == CustomItems.wand) {
               Packets.send(player, new PacketUpdatePhysics(this));
               if (data != null) { player.connection.send(new ClientboundSetEntityDataPacket(getId(), data)); }
               Packets.send(player, new PacketNpcUpdate(getId(), writeSpawnData()));
            }
         }
      }

      // New from Unofficial (BetaZavr)
      if (animateAi != null) {
         animation.stopAnimation();
         if (!isClientSide()) { animateAi.playInitCustomAnimation(); }
      }
      updateClient = true;
      setMaxUpStep(ais.stepheight * 1.8333F);
      lookPos[0] = 0.0f;
      lookPos[1] = 0.0f;
      puppet.reset();
      setPose(Pose.STANDING);
   }

   public void onCollide() {
      if (!ais.aiDisabled && isAlive() && tickCount % 4 == 0 && !level().isClientSide) {
         AABB axisAlignedBB;
         if (getVehicle() != null && getVehicle().isAlive()) { axisAlignedBB = getBoundingBox().minmax(getVehicle().getBoundingBox()).inflate(1.0D, 0.0D, 1.0D); }
         else { axisAlignedBB = getBoundingBox().inflate(1.0D, 0.5D, 1.0D); }
         List<LivingEntity> list = level().getEntitiesOfClass(LivingEntity.class, axisAlignedBB);
         for (LivingEntity entity : list) {
            if (entity != this && entity.isAlive()) { EventHooks.onNPCCollide(this, entity); }
         }
      }
   }

   @Override
   public void handleInsidePortal(@Nonnull BlockPos pos) { }

   public void cloakUpdate() {
      prevChasingPosX = chasingPosX;
      prevChasingPosY = chasingPosY;
      prevChasingPosZ = chasingPosZ;
      double d0 = getX() - chasingPosX;
      double d1 = getY() - chasingPosY;
      double d2 = getZ() - chasingPosZ;
      double d3 = 10.0D;
      if (d0 > d3) { prevChasingPosX = chasingPosX = getX(); }
      if (d2 > d3) { prevChasingPosZ = chasingPosZ = getZ(); }
      if (d1 > d3) { prevChasingPosY = chasingPosY = getY(); }
      if (d0 < -d3) { prevChasingPosX = chasingPosX = getX(); }
      if (d2 < -d3) { prevChasingPosZ = chasingPosZ = getZ(); }
      if (d1 < -d3) { prevChasingPosY = chasingPosY = getY(); }
      chasingPosX += d0 * 0.25D;
      chasingPosZ += d2 * 0.25D;
      chasingPosY += d1 * 0.25D;
   }

   @Override
   public boolean removeWhenFarAway(double distanceToPlayer) { return stats != null && stats.spawnCycle == 4; }

   @Override
   public @Nonnull ItemStack getMainHandItem() {
      IItemStack item;
      if (isAttacking()) { item = inventory.getRightHand(); }
      else if (role instanceof RoleCompanion roleCompanion) { item = roleCompanion.getItemInHand(); }
      else if (job.overrideMainHand) { item = job.getMainhand(); }
      else { item = inventory.getRightHand(); }
      return ItemStackWrapper.MCItem(item);
   }

   @Override
   public @Nonnull ItemStack getOffhandItem() {
      IItemStack item;
      if (isAttacking()) { item = inventory.getLeftHand(); }
      else if (job.overrideOffHand) { item = job.getOffhand(); }
      else { item = inventory.getLeftHand(); }
      return ItemStackWrapper.MCItem(item);
   }

   @Override
   public @Nonnull ItemStack getItemBySlot(@Nonnull EquipmentSlot slot) {
      if (slot == EquipmentSlot.MAINHAND) { return getMainHandItem(); }
      return slot == EquipmentSlot.OFFHAND ? getOffhandItem() : ItemStackWrapper.MCItem(inventory.getArmor(3 - slot.getIndex()));
   }

   @Override
   public void setItemSlot(@Nonnull EquipmentSlot slot, @Nonnull ItemStack item) {
      if (slot == EquipmentSlot.MAINHAND) { inventory.weapons.put(0, Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item)); }
      else if (slot == EquipmentSlot.OFFHAND) { inventory.weapons.put(2, Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item)); }
      else { inventory.armor.put(3 - slot.getIndex(), Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item)); }
   }

   @Override
   public @Nonnull Iterable<ItemStack> getArmorSlots() {
      ArrayList<ItemStack> list = new ArrayList<>();
      for(int i = 0; i < 4; ++i) { list.add(ItemStackWrapper.MCItem(inventory.armor.get(3 - i))); }
      return list;
   }

   @Override
   public @Nonnull Iterable<ItemStack> getAllSlots() {
      ArrayList<ItemStack> list = new ArrayList<>();
      list.add(ItemStackWrapper.MCItem(inventory.weapons.get(0)));
      list.add(ItemStackWrapper.MCItem(inventory.weapons.get(2)));
      return list;
   }

   @Override
   protected void dropCustomDeathLoot(@Nonnull DamageSource source, int looting, boolean recentlyHitIn) { }

   @Override
   protected void dropFromLootTable(@Nonnull DamageSource damageSourceIn, boolean attackedRecently) { }

   @Override
   public void die(@Nonnull DamageSource damagesource) {
      setSprinting(false);
      getNavigation().stop();
      clearFire();
      removeAllEffects();
      Entity attackingEntity = NoppesUtilServer.getDamageSource(damagesource);
      if (role != null) { role.aiDeathExecute(attackingEntity); }
      if (job != null) {
         job.aiDeathExecute(attackingEntity);
         puppet.aiDeathExecute(attackingEntity);
      }
      if (!isClientSide()) {
         advanced.playSound(3, getSoundVolume(), getVoicePitch());
         NpcEvent.DiedEvent event = new NpcEvent.DiedEvent(wrappedNPC, damagesource, attackingEntity, combatHandler);
         // chance
         double baseChance = 1.0d;
         if (!combatHandler.aggressors.isEmpty()) {
            double luck = 0.0d;
            double enchLv = 0.0d;
            int i = 0;
            int j = 0;
            for (LivingEntity e : combatHandler.aggressors.keySet()) {
               AttributeInstance l = e.getAttribute(Attributes.LUCK);
               if (l != null) {
                  luck += l.getValue();
                  i++;
               }
               ItemStack held = !e.getMainHandItem().isEmpty() ? e.getMainHandItem() : e.getOffhandItem();
               if (held.isEnchanted()) {
                  enchLv += EnchantmentHelper.getMobLooting(e);
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
         if (role instanceof RoleFollower follower && !follower.inventory.isEmpty()) {
            for (int i = 0; i < follower.inventory.getContainerSize(); i++) {
               ItemStack stack = follower.inventory.getItem(i);
               if (!NoppesUtilServer.isItemStackNull(stack)) { spawnAtLocation(stack, 0.0f); }
            }
            follower.inventory.clearContent();
         }
         // to scripts
         EventHooks.onNPCDied(this, event);
         bossInfo.setVisible(false);
         inventory.dropStuff(event, attackingEntity, damagesource);
         if (event.line != null) {
            saySurrounding(Line.formatTarget((Line) event.line, attackingEntity instanceof LivingEntity ? (LivingEntity) attackingEntity : null));
         }
         if (animateAi != null && !isClientSide()) { animateAi.playDeathCustomAnimation(); }
      }

      super.die(damagesource);
   }

   @Override
   public void startSeenByPlayer(@Nonnull ServerPlayer player) {
      super.startSeenByPlayer(player);
      bossInfo.addPlayer(player);
   }

   @Override
   public void stopSeenByPlayer(@Nonnull ServerPlayer player) {
      super.stopSeenByPlayer(player);
      bossInfo.removePlayer(player);
   }

   @Override
   public void remove(@Nonnull RemovalReason reason) {
      if (reason != RemovalReason.KILLED) { super.remove(reason); }
      else {
         hasDied = true;
         ejectPassengers();
         stopRiding();
         if (!level().isClientSide && stats.spawnCycle != 3 && stats.spawnCycle != 4) {
            setHealth(-1.0F);
            setSprinting(false);
            getNavigation().stop();
            setCurrentAnimation(2);
            refreshDimensions();
            if (killedTime <= 0L) { killedTime = (stats.respawnTime * 1000L) + System.currentTimeMillis(); }
            if (!ais.aiDisabled) {
               role.killed();
               job.killed();
               puppet.killed();
            }
         }
         else { delete(); }
      }
   }

   public void delete() {
      VisibilityController.instance.remove(this);
      role.delete();
      job.delete();
      super.remove(RemovalReason.DISCARDED);
   }

   public float getStartXPos() { return (float) ais.startPos().getX() + ais.bodyOffsetX / 10.0F; }

   public float getStartZPos() { return (float) ais.startPos().getZ() + ais.bodyOffsetZ / 10.0F; }

   public boolean isVeryNearAssignedPlace() {
      double xx = getX() - (double) getStartXPos();
      double zz = getZ() - (double) getStartZPos();
      if (!(xx < -0.2D) && !(xx > 0.2D)) { return !(zz < -0.2D) && !(zz > 0.2D); }
      return false;
   }

   public double getStartYPos() {
      return startYPos < (double) level().getMinBuildHeight() ? calculateStartYPos(ais.startPos()) : startYPos;
   }

   private double calculateStartYPos(BlockPos pos) {
      BlockPos startPos = ais.startPos();
      while(pos.getY() > level().getMinBuildHeight()) {
         BlockState state = level().getBlockState(pos);
         VoxelShape shape = state.getShape(level(), pos);
         if (!shape.isEmpty()) {
            AABB bb = shape.bounds().move(pos);
            if (ais.movementType != 2 || startPos.getY() > pos.getY() || !state.is(Blocks.WATER)) { return bb.maxY; }
         }
         pos = pos.below();
      }
      return level().getMinBuildHeight();
   }

   private BlockPos calculateTopPos(BlockPos pos) {
      for(BlockPos check = pos; check.getY() > level().getMinBuildHeight(); check = check.below()) {
         BlockState state = level().getBlockState(pos);
         VoxelShape shape = state.getShape(level(), pos);
         if (!shape.isEmpty()) { return check; }
      }
      return pos;
   }

   public boolean isInRange(Entity entity, double range) {
      return isInRange(entity.getX(), entity.getY(), entity.getZ(), range);
   }

   public boolean isInRange(double posX, double posY, double posZ, double range) {
      double y = Math.abs(getY() - posY);
      if (posY >= (double) level().getMinBuildHeight() && y > range) { return false; }
      double x = Math.abs(getX() - posX);
      double z = Math.abs(getZ() - posZ);
      return x <= range && z <= range;
   }

   public void givePlayerItem(Player player, ItemStack stack) {
      if (!ais.aiDisabled && !level().isClientSide) {
         stack = stack.copy();
         float f = 0.7F;
         double d = (double)(level().random.nextFloat() * f) + (double)(1.0F - f);
         double d1 = (double)(level().random.nextFloat() * f) + (double)(1.0F - f);
         double d2 = (double)(level().random.nextFloat() * f) + (double)(1.0F - f);
         ItemEntity entityItem = new ItemEntity(level(), getX() + d, getY() + d1, getZ() + d2, stack);
         entityItem.setPickUpDelay(2);
         level().addFreshEntity(entityItem);
         int i = stack.getCount();
         if (player.getInventory().add(stack)) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            player.take(entityItem, i);
            if (stack.getCount() <= 0) { entityItem.discard(); }
         }
      }
   }

   @Override
   public boolean isSleeping() { return getHealth() <= 0.0f || currentAnimation == 2 && !isAttacking() && getTarget() == null && navigating == null; }

   public boolean isWalking() { return ais.getMovingType() != 0 || isAttacking() || isFollower() || entityData.get(Walking); }

   @Override
   public boolean isCrouching() { return currentAnimation == 4 || super.isCrouching(); }

   @Override
   public void knockback(double strength, double ratioX, double ratioZ) {
      super.knockback(strength * (double)(2.0F - stats.resistances.get("knockback")), ratioX, ratioZ);
   }

   public Faction getFaction() {
      Faction fac = FactionController.instance.getFaction(entityData.get(FactionData));
      return fac == null ? FactionController.instance.getFaction(FactionController.instance.getFirstFactionId()) : fac;
   }

   public boolean isClientSide() { return level().isClientSide; }

   public void setFaction(int id) {
      if (id >= 0 && !isClientSide()) { entityData.set(FactionData, id); }
   }

   @Override
   public boolean canBeAffected(@Nonnull MobEffectInstance effect) {
      if (stats.potionImmune) { return false; }
      return (getMobType() != MobType.ARTHROPOD || effect.getEffect() != MobEffects.POISON) && super.canBeAffected(effect);
   }

   public boolean isAttacking() { return entityData.get(Attacking); }

   public boolean isKilled() { return isRemoved() || entityData.get(IsDead); }

   @Override
   public void writeSpawnData(FriendlyByteBuf buffer) { buffer.writeNbt(writeSpawnData()); }

   public CompoundTag writeSpawnData() {
      CompoundTag compound = new CompoundTag();
      display.save(compound);
      compound.putInt("MaxHealth", stats.maxHealth);
      compound.put("Armor", NBTTags.nbtIItemStackMap(inventory.armor));
      compound.put("Weapons", NBTTags.nbtIItemStackMap(inventory.weapons));
      compound.putInt("Speed", ais.getWalkingSpeed());
      compound.putBoolean("MountControl", ais.mountControl);
      compound.putBoolean("DeadBody", stats.hideKilledBody);
      compound.putInt("StandingState", ais.getStandingType());
      compound.putInt("MovingState", ais.getMovingType());
      compound.putInt("Orientation", ais.orientation);
      compound.putFloat("PositionXOffset", ais.bodyOffsetX);
      compound.putFloat("PositionYOffset", ais.bodyOffsetY);
      compound.putFloat("PositionZOffset", ais.bodyOffsetZ);

      CompoundTag roleNbt = new CompoundTag();
      CompoundTag jobNbt = new CompoundTag();
      role.save(roleNbt);
      job.save(jobNbt);
      compound.put("Role", roleNbt);
      compound.put("Job", jobNbt);
      CompoundTag nbt;
      if (job.getEnumType() == JobType.BARD) {
         nbt = compound.getCompound("Bard");
         job.save(nbt);
         compound.put("Bard", nbt);
      }
      if (role.getEnumType() == RoleType.COMPANION) {
         nbt = compound.getCompound("Companion");
         role.save(nbt);
         compound.put("Companion", nbt);
      }

      // animation
      compound.putInt("AnimationType", animationType.ordinal());
      compound.put("Puppet", puppet.save(new CompoundTag()));
      if (this instanceof EntityCustomNpc) { compound.put("ModelData", ((EntityCustomNpc)this).modelData.save()); }
      compound.putString("HomeDimension", homeDimensionId.location().toString());
      return compound;
   }

   @Override
   public void readSpawnData(FriendlyByteBuf buf) { readSpawnData(Objects.requireNonNull(buf.readNbt())); }

   public void readSpawnData(CompoundTag compound) {
      stats.setMaxHealth(compound.getInt("MaxHealth"));
      ais.setWalkingSpeed(compound.getInt("Speed"));
      stats.hideKilledBody = compound.getBoolean("DeadBody");
      ais.setStandingType(compound.getInt("StandingState"));
      ais.mountControl = compound.getBoolean("MountControl");
      ais.setMovingType(compound.getInt("MovingState"));
      ais.orientation = compound.getInt("Orientation");
      ais.bodyOffsetX = compound.getFloat("PositionXOffset");
      ais.bodyOffsetY = compound.getFloat("PositionYOffset");
      ais.bodyOffsetZ = compound.getFloat("PositionZOffset");
      inventory.armor.clear();
      inventory.armor.putAll(NBTTags.getIItemStackMap(compound.getList("Armor", 10)));
      inventory.weapons.clear();
      inventory.weapons.putAll(NBTTags.getIItemStackMap(compound.getList("Weapons", 10)));
      // Old Noppes
      if (compound.contains("Role", Tag.TAG_INT) && compound.contains("NpcJob", Tag.TAG_INT)) {
         advanced.setRole(compound.getInt("Role"));
         advanced.setJob(compound.getInt("NpcJob"));
         role.load(compound);
         job.load(compound);
      }
      // Old BetaZavr
      if (compound.contains("Role", Tag.TAG_COMPOUND) && compound.contains("Job", Tag.TAG_COMPOUND)) {
         advanced.setRole(compound.getCompound("Role").getInt("Type"));
         advanced.setJob(compound.getCompound("Job").getInt("Type"));
         role.load(compound.getCompound("Role"));
         job.load(compound.getCompound("Job"));
      }
      if (role instanceof RoleTrader && compound.contains("MarketID", Tag.TAG_INT)) { role.load(compound); }
      if (job.getEnumType() == JobType.BARD) { job.load(compound.getCompound("Bard")); }
      if (role.getEnumType() == RoleType.COMPANION) { role.load(compound.getCompound("Companion")); }
      if (compound.contains("Puppet", Tag.TAG_COMPOUND)) { puppet.load(compound.getCompound("Puppet")); }
      if (this instanceof EntityCustomNpc cNpc) { cNpc.modelData.load(compound.getCompound("ModelData")); }
      display.load(compound);
      if (compound.contains("HomeDimension", Tag.TAG_STRING)) {
         homeDimensionId = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(compound.getString("HomeDimension")));
      }
      refreshDimensions();
   }

   @Override
   @SuppressWarnings("ConstantConditions")
   public @Nonnull CommandSourceStack createCommandSourceStack() {
      if (level().isClientSide) { return super.createCommandSourceStack(); }
      EntityUtil.Copy(this, CommandPlayer);
      ((IEntityMixin) ChatEventPlayer).setLevel(level());
      CommandPlayer.setPos(getX(), getY(), getZ());
      return new CommandSourceStack(this, position(), getRotationVector(), level() instanceof ServerLevel ? (ServerLevel) level() : null,
              getPermissionLevel(), getName().getString(), getDisplayName(), Objects.requireNonNull(level().getServer()), this);
   }

   @Override
   public @Nonnull Component getName() { return Component.translatable(display.getName()); }

   public void setImmuneToFire(boolean immuneToFire) { stats.immuneToFire = immuneToFire; }

   @Override
   public boolean fireImmune() { return stats.immuneToFire; }

   @Override
   public boolean causeFallDamage(float distance, float modifier, @Nonnull DamageSource source) {
      return !stats.noFallDamage && super.causeFallDamage(distance, modifier, source);
   }

   @Override
   public void makeStuckInBlock(@Nonnull BlockState state, @Nonnull Vec3 motionMultiplierIn) {
      if (!state.is(Blocks.COBWEB) || !stats.ignoreCobweb) { super.makeStuckInBlock(state, motionMultiplierIn); }
   }

   @Override
   public boolean canCollideWith(@Nonnull Entity entity) {
      return canBeCollidedWith() && ais.canBeCollide && super.canCollideWith(entity);
   }

   @Override
   public boolean canBeCollidedWith() { return !isKilled() && display.getHitboxState() == 2; }

   @Override
   protected void pushEntities() {
      if (display.getHitboxState() == 0) { super.pushEntities(); }
   }

   @Override
   public boolean isPushable() { return isWalking() && !isKilled(); }

   @Override
   public @Nonnull PushReaction getPistonPushReaction() { return display.getHitboxState() == 0 ? super.getPistonPushReaction() : PushReaction.IGNORE; }

   public String getRoleData() { return entityData.get(RoleData); }

   public void setRoleData(String s) { entityData.set(RoleData, s); }

   public String getJobData() { return entityData.get(RoleData); }

   public void setJobData(String s) { entityData.set(RoleData, s); }

   @Override
   public @Nonnull Level getCommandSenderWorld() { return level(); }

   @Override
   public boolean isInvisibleTo(@Nonnull Player player) { return display.getVisible() == 1 && player.getMainHandItem().getItem() != CustomItems.wand && !display.getAvailability().hasOptions(); }

   @Override
   public boolean isInvisible() { return display.getVisible() != 0 && !display.getAvailability().hasOptions(); }

   public void setCurrentAnimation(int animation) {
      currentAnimation = animation;
      entityData.set(Animation, animation);
      if (animation != 4 && aiAttackTarget instanceof EntityAICommanderTarget aiTarget) { aiTarget.baseAnimation = animation; }
   }

   public boolean isFollower() {
      return !ais.aiDisabled && advanced.scenes.getOwner() != null || role.isFollowing() || job.isFollowing();
   }

   public LivingEntity getOwner() {
      if (ais.aiDisabled) { return null; }
      if (advanced.scenes.getOwner() != null) { return advanced.scenes.getOwner(); }
      else if (role.getEnumType() == RoleType.FOLLOWER && role instanceof RoleFollower) { return ((RoleFollower)role).owner; }
      else if (role.getEnumType() == RoleType.COMPANION && role instanceof RoleCompanion) { return ((RoleCompanion)role).owner; }
      return job.getType() == 5 && job instanceof JobFollower ? ((JobFollower)job).following : null;
   }

   public boolean emptyOwner() {
      return (ais.aiDisabled || advanced.scenes.getOwner() == null) &&
              (!(role instanceof RoleFollower) || !((RoleFollower) role).hasOwner()) &&
              (!(role instanceof RoleCompanion) || !((RoleCompanion) role).hasOwner()) &&
              (!(job instanceof JobFollower) || !((JobFollower) job).hasOwner());
   }

   public int followRange() {
      if ((advanced.scenes.getOwner() != null) ||
              (role.getEnumType() == RoleType.COMPANION && role.isFollowing()) ||
              (job.getEnumType() == JobType.FOLLOWER && job.isFollowing())) { return 4; }
      if (role.getEnumType() == RoleType.FOLLOWER && role.isFollowing()) { return 6; }
      return Math.max(1, stats.aggroRange - 1);
   }

   @Override
   protected float getDamageAfterArmorAbsorb(@Nonnull DamageSource source, float damage) {
      if (role.getEnumType() == RoleType.COMPANION) {
         damage = ((RoleCompanion)role).getDamageAfterArmorAbsorb(source, damage); }
      return damage;
   }

   @Override
   public boolean isAlliedTo(@Nonnull Entity entity) {
      if (!isClientSide()) {
         if (entity instanceof Player && getFaction().isFriendlyToPlayer((Player)entity)) { return true; }
         if (entity == getOwner()) { return true; }
         if (entity instanceof EntityNPCInterface && ((EntityNPCInterface)entity).faction.id == faction.id) { return true; }
      }
      return super.isAlliedTo(entity);
   }

   @SuppressWarnings("unchecked")
   public void setDataWatcher(SynchedEntityData entityData) {
      List<DataValue<?>> list = new ArrayList<>();
      for (DataItem<Object> entry : ((ISynchedEntityData) entityData).cnpcs$getAll()) {
         if (entry.getValue() instanceof DataValue) { list.add((DataValue<Object>) entry.getValue()); }
      }
      entityData.assignValues(list);
   }

   @Override
   public void travel(@Nonnull Vec3 travelVector) {
      BlockPos pos = blockPosition();
      if (isAlive() && isVehicle() && ais.mountControl && getControllingPassenger() != null) {
         LivingEntity livingentity = getControllingPassenger();
         setYRot(livingentity.getYRot());
         yRotO = getYRot();
         setXRot(livingentity.getXRot() * 0.5F);
         setRot(getYRot(), getXRot());
         yBodyRot = getYRot();
         yHeadRot = yBodyRot;
         float f = livingentity.xxa * 0.5F;
         float f1 = livingentity.zza;
         if (f1 <= 0.0F) { f1 *= 0.25F; }
         setMaxUpStep(ais.stepheight * 1.8333F);
         if (canFly()) {
            setNoGravity(true);
            fallDistance = 0.0F;
            super.travel(new Vec3(f * 2.0F, -Math.sin(Math.toRadians(getXRot())) * 2.0D, f1 * 2.0F));
         }
         else {
            setNoGravity(false);
            super.travel(new Vec3(f, travelVector.y, f1));
         }
      }
      else {
         setMaxUpStep(ais.stepheight * 0.8333F);
         super.travel(travelVector);
      }
      if (!ais.aiDisabled && role.getEnumType() == RoleType.COMPANION && !isClientSide()) {
         BlockPos delta = blockPosition().subtract(pos);
         ((RoleCompanion) role).addMovementStat(delta.getX(), delta.getY(), delta.getZ());
      }
   }

   @Override
   public boolean canBeLeashed(@Nonnull Player player) { return false; }

   @Override
   public boolean isLeashed() { return false; }

   public boolean nearPosition(BlockPos pos) {
      BlockPos npcPos = blockPosition();
      float x = (float) (npcPos.getX() - pos.getX());
      float z = (float) (npcPos.getZ() - pos.getZ());
      float y = (float) (npcPos.getY() - pos.getY());
      float height = (float) (Mth.ceil(getBbHeight() + 1.0F) * Mth.ceil(getBbHeight() + 1.0F));
      return (double) (x * x + z * z) < 2.5D && (double) (y * y) < (double) height + 2.5D;
   }

   public void tpTo(LivingEntity owner) {
      if (owner != null) {
         Direction facing = owner.getDirection().getOpposite();
         BlockPos pos = new BlockPos((int)owner.getX(), (int)owner.getBoundingBox().minY, (int)owner.getZ());
         pos = pos.offset(facing.getStepX(), 0, facing.getStepZ());
         pos = calculateTopPos(pos);
         for(int i = -1; i < 2; ++i) {
            for(int j = 0; j < 3; ++j) {
               BlockPos check;
               if (facing.getStepX() == 0) { check = pos.offset(i, 0, j * facing.getStepZ()); }
               else { check = pos.offset(j * facing.getStepX(), 0, i); }
               check = calculateTopPos(check);
               if (!level().getBlockState(check).isSolidRender(level(), check) && !level().getBlockState(check.above()).isSolidRender(level(), check.above())) {
                  moveTo(check.getX() + 0.5D, check.getY(), check.getZ() + 0.5D, getYRot(), getXRot());
                  getNavigation().stop();
                  break;
               }
            }
         }

      }
   }

   @Override
   public boolean canBeRiddenUnderFluidType(FluidType type, Entity rider) { return true; }

   @Override
   public void onSyncedDataUpdated(@Nonnull EntityDataAccessor<?> para) {
      super.onSyncedDataUpdated(para);
      if (Animation.equals(para)) { refreshDimensions(); }
   }

   @Override
   protected void updateControlFlags() {
      boolean flag1 = !(getVehicle() instanceof Boat);
      goalSelector.setControlFlag(Flag.MOVE, true);
      goalSelector.setControlFlag(Flag.JUMP, flag1);
      goalSelector.setControlFlag(Flag.LOOK, true);
   }

   @Override
   public void checkDespawn() {
      super.checkDespawn();
      if (getNoActionTime() != 0) {
          Entity entity = level().getNearestPlayer(this, -1.0D);
          if (entity != null) {
              double d0 = entity.distanceToSqr(this);
              double range = (double) ais.activeRange * (double) ais.activeRange;
              if (d0 < range) {
                  noActionTime = 0;
              }
          }
      }

   }

   public void setPriorityAttackTarget(LivingEntity entityTarget) {
      if (!isAlive() ||
              getTarget() == entityTarget ||
              (entityTarget instanceof Player && ((Player) entityTarget).getAbilities().invulnerable) ||
              (entityTarget != null && entityTarget == getOwner()) ||
              (entityTarget instanceof EntityNPCInterface && isFriend(entityTarget))
      ) { return; }
      super.setTarget(entityTarget);
      updateTargetClient();
   }

   public boolean isFriend(LivingEntity entityTarget) {
      if (!(entityTarget instanceof EntityNPCInterface npcTarget)) { return false; }
      return faction.id == npcTarget.faction.id || npcTarget.faction.frendFactions.contains(faction.id)
              || npcTarget.advanced.friendFactions.contains(faction.id)
              || faction.frendFactions.contains(npcTarget.faction.id)
              || advanced.friendFactions.contains(npcTarget.faction.id);
   }

   // New Unofficial (Goodbird)
   @Override
   public @Nonnull Packet<ClientGamePacketListener> getAddEntityPacket() {
      return CustomNpcs.EnableInvisibleNpcs && CustomNpcs.InvisibilityAlgorithm == 0 ? super.getAddEntityPacket() : NetworkHooks.getEntitySpawningPacket(this);
   }

   public void startFallFlying() { setSharedFlag(7, true); }

   public void stopFallFlying() {
      setSharedFlag(7, true);
      setSharedFlag(7, false);
   }

   public boolean canSee(Entity entity) {
      return entity != null && getSensing().hasLineOfSight(entity);
   }

   @Override
   public boolean hasLineOfSight(@Nonnull Entity target) {
      if (ais.directLOS == EnumSeeTarget.DEAF) { return false; }
      try {
         double aggroRange = ValueUtil.correctDouble(stats.getAggroRange() / (isSleeping() ? 4.0d : 1.0d), 1.0d, 512.0d);
         IRayTraceRotate rtr = Util.instance.getAngles3D(getX(), getY() + getEyeHeight(), getZ(), target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
         if (rtr.getDistance() > aggroRange) { return false; }
         if (ais.directLOS == EnumSeeTarget.NONE) { return true; }
         boolean isDirect = ais.directLOS == EnumSeeTarget.WARY ||
                 ais.directLOS == EnumSeeTarget.CALM ||
                 ais.directLOS == EnumSeeTarget.REALISTIC;
         if (isDirect && isSleeping()) { return false; }
         if (ais.directLOS != EnumSeeTarget.BLIND) {
            IRayTraceResults rtrs = Util.instance.rayTraceBlocksAndEntitys(this, rtr.getYaw(), rtr.getPitch(), rtr.getDistance());
            if (rtrs != null) {
               for (DataBlock db : rtrs.getMCBlocks()) {
                  if (!db.state.isPathfindable(level(), db.pos, PathComputationType.LAND) ||
                          (ais.directLOS != EnumSeeTarget.NORMAL && db.state.isViewBlocking(level(), db.pos))) { return false; }
               }
               rtrs.clear();
            }
         }
         if (isDirect) {
            double yaw = (yHeadRot - rtr.getYaw()) % 360.0d;
            double pitch = (getXRot() - rtr.getPitch()) % 360.0d;
            while (yaw < 0.0d) { yaw += 360.0d; }
            if ((yaw > 60.0d && yaw < 300.0d) || pitch > 60.0d || pitch < -60.0d) {
               if (ais.directLOS == EnumSeeTarget.WARY || ais.directLOS == EnumSeeTarget.REALISTIC) {
                  double d0 = ValueUtil.correctDouble(aggroRange / 4.0d, 3.0d, aggroRange);
                  double d1 = (target.isCrouching() ? 3.0d : 1.0d);
                  double chance;
                  if (rtr.getDistance() < d0) {
                     chance = ValueUtil.correctDouble(( 0.9d * rtr.getDistance() / d0 + 1.0d) / d1, 0.0d, 1.0d);
                  }
                  else {
                     if (target.isCrouching()) { return false; }
                     double d2 = 0.1d / (4.0d - aggroRange);
                     double d3 = 0.1d - d2 * 4.0d;
                     chance = ValueUtil.correctDouble((d2 * rtr.getDistance() + d3) / d1 / 10.0d, 0.0d, 1.0d); // 10: time in tick
                  }
                  if (chance < Math.random()) { return false; }
               }
               else { return false; }
            }
         }
         if (target instanceof LivingEntity living) {
            MobEffectInstance effect = living.getEffect(MobEffects.INVISIBILITY);
            if (effect != null && ais.directLOS != EnumSeeTarget.NORMAL) {
               double invisible = 1.0d + effect.getAmplifier();
               double chance = ValueUtil.correctDouble(invisible == 0 ? 1.0d :
                               (-0.00026d * Math.pow(invisible, 3.0d) + 0.00489d * Math.pow(invisible, 2.0d) - 0.03166d * invisible + 0.08d),
                       0.002d, 1.0d);
               if (chance != 1.0d) { chance *= -1.0d * (rtr.getDistance() / aggroRange) + 1.0d; } // distance
               if (chance != 1.0d && target.isCrouching()) { chance *= 0.3d; } // is sneaking
               return ValueUtil.correctDouble(chance, 0.0d, 1.0d) > Math.random();
            }
         }
         return true;
      }
      catch (Exception ignored) { }
      return false;
   }

   @Override
   public boolean isDamageSourceBlocked(DamageSource damageSourceIn) {
      boolean isBlocked = false;
      int type = damageSourceIn.is(DamageTypeTags.BYPASSES_SHIELD) || damageSourceIn.getSourcePosition() == null ? -1 : 0;
      if (damageSourceIn.getSourcePosition() != null) {
         type = 0;
         Vec3 vec3 = damageSourceIn.getSourcePosition(); // position from which damage is dealt
         float angle = (float) Util.instance.getAngles3D(getX(), 0.0d, getZ(), vec3.x, 0.0d, vec3.z).getYaw() - getYRot();
         Vec3 vec31 = getLookAngle(); // which way is looking this NPC
         Vec3 vec32 = vec3.vectorTo(new Vec3(getX(), getY(), getZ())).normalize();
         vec32 = new Vec3(vec32.x, 0.0D, vec32.z);
         if (vec32.dot(vec31) < 0.0D) {
            if (angle < 180.0f) { type = 1; } else { type = 2; }
         }
      }
      if (type != -1 && !damageSourceIn.is(DamageTypeTags.BYPASSES_SHIELD)) {
         float chance = stats.getChanceBlockDamage() / 100.0f;
         if (chance > 0.0f && type > 0) { // in front of
            ItemStack stack;
            if (inventory.getProjectile() != null) { chance /= 3.0f; }
            else if (type == 1) { // to the right
               stack = inventory.getRightHand() != null ? inventory.getRightHand().getMCItemStack() : ItemStack.EMPTY;
               if (stack.getItem() instanceof SwordItem) {
                  chance *= 1.3333f;
                  if (chance < 0.25f) { chance = 0.25f; }
               }
               else if (stack.getItem() instanceof ShieldItem) {
                  chance *= 2.0f;
                  if (chance < 0.75f) { chance = 0.75f; }
               }
            }
            else { // to the left
               stack = inventory.getLeftHand() != null ? inventory.getLeftHand().getMCItemStack() : ItemStack.EMPTY;
               if (stack.getItem() instanceof SwordItem) {
                  chance *= 1.1667f;
                  if (chance < 0.1f) { chance = 0.1f; }
               }
               else if (stack.getItem() instanceof ShieldItem) {
                  chance *= 1.75f;
                  if (chance < 0.5f) { chance = 0.5f; }
               }
            }
            float f = random.nextFloat();
            isBlocked = chance >= f;
         }
      }
      NpcEvent.NeedBlockDamage event = new NpcEvent.NeedBlockDamage(wrappedNPC, damageSourceIn, isBlocked, type);
      EventHooks.onNPCNeedBlockDamage(this, event);
      return event.isBlocked && !event.isCanceled();
   }

   // New from Unofficial (GoodBird)
   public void setInvisible(ServerPlayer player) {
      if (tracking.contains(player.getId())) {
         tracking.remove(player.getId());
         Packets.send(player, new PacketNpcVisibleFalse(this));
      }
   }

   public void setVisible(ServerPlayer player) {
      if (!tracking.contains(player.getId())) {
         tracking.add(player.getId());
         Packets.send(player, new PacketNpcVisibleTrue(this));
         List<DataValue<?>> data = getEntityData().getNonDefaultValues();
         if (data != null) { player.connection.send(new ClientboundSetEntityDataPacket(getId(), data)); }
      }
      Packets.send(player, new PacketNpcUpdate(getId(), writeSpawnData()));
      MarkData.get(this).syncClients();
   }

   // New from Unofficial (BetaZavr)
   public boolean isMoving() {
      if (!getNavigation().isDone()) { return true; }
      double sp = Objects.requireNonNull(getAttribute(Attributes.MOVEMENT_SPEED)).getValue();
      double speed = 0.069d;
      if (sp != 0.0d) { speed = speed * 0.25d / sp; }
      Vec3 motion = getDeltaMovement();
      double xz = Math.sqrt(Math.pow(motion.x, 2.0d) + Math.pow(motion.z, 2.0d));
      return xz >= (speed / 2.0d) && (motion.y <= -speed || motion.y > 0.0d);
   }

   @Override
   public boolean canAttack(@Nonnull LivingEntity entity) {
      return !ais.aiDisabled && !(entity instanceof Bat) && super.canAttack(entity);
   }


}
