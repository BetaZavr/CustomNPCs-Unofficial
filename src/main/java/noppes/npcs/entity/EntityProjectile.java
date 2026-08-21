package noppes.npcs.entity;

import java.util.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomEntities;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.constants.ParticleType;
import noppes.npcs.api.constants.PotionEffectType;
import noppes.npcs.api.event.ProjectileEvent;
import noppes.npcs.api.wrapper.BlockWrapper;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.entity.data.DataRanged;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class EntityProjectile extends ThrowableProjectile {

   private static final EntityDataAccessor<Boolean> Gravity = SynchedEntityData.defineId(EntityProjectile.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> Arrow = SynchedEntityData.defineId(EntityProjectile.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> Is3d = SynchedEntityData.defineId(EntityProjectile.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> Glows = SynchedEntityData.defineId(EntityProjectile.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> Rotating = SynchedEntityData.defineId(EntityProjectile.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> Sticks = SynchedEntityData.defineId(EntityProjectile.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<ItemStack> ItemStackThrown = SynchedEntityData.defineId(EntityProjectile.class, EntityDataSerializers.ITEM_STACK);
   private static final EntityDataAccessor<Integer> Velocity = SynchedEntityData.defineId(EntityProjectile.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> Size = SynchedEntityData.defineId(EntityProjectile.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> Particle = SynchedEntityData.defineId(EntityProjectile.class, EntityDataSerializers.INT);

   private BlockPos tilePos = BlockPos.ZERO;
   private BlockState inBlock;
   protected boolean inGround = false;
   public int throwableShake = 0;
   public int arrowShake = 0;
   public boolean canBePickedUp = false;
   public boolean destroyedOnEntityHit = true;
   private Entity thrower;
   private EntityNPCInterface npc;
   private String throwerName = null;
   private int ticksInGround;
   public int ticksInAir = 0;
   private double accelerationX;
   private double accelerationY;
   private double accelerationZ;
   public float damage = 5.0F;
   public int punch = 0;
   public boolean accelerate = false;
   public boolean explosiveDamage = true;
   public int explosiveRadius = 0;
   public int effect = 0;
   public int duration = 5;
   public int amplify = 0;
   public int accuracy = 60;
   public EntityProjectile.IProjectileCallback callback;
   public List<ScriptContainer> scripts = new ArrayList<>();

   @SuppressWarnings("unchecked")
   public EntityProjectile(EntityType<?> type, Level levelIn) {
      super((EntityType<? extends ThrowableProjectile>) type, levelIn);
   }

   protected void defineSynchedData() {
      entityData.define(ItemStackThrown, ItemStack.EMPTY);
      entityData.define(Velocity, 10);
      entityData.define(Size, 10);
      entityData.define(Particle, 0);
      entityData.define(Gravity, false);
      entityData.define(Glows, false);
      entityData.define(Arrow, false);
      entityData.define(Is3d, false);
      entityData.define(Rotating, false);
      entityData.define(Sticks, false);
   }

   @OnlyIn(Dist.CLIENT)
   public boolean shouldRenderAtSqrDistance(double par1) {
      double d1 = getBoundingBox().getSize() * 4.0D;
      d1 *= 64.0D;
      return par1 < d1 * d1;
   }

   public EntityProjectile(Level level, LivingEntity limbSwingAmountEntityLiving, ItemStack item, boolean isNPC) {
      super(CustomEntities.entityProjectile, level);
      thrower = limbSwingAmountEntityLiving;
      if (thrower != null) { throwerName = thrower.getUUID().toString(); }
      setThrownItem(item);
      entityData.set(Arrow, getItem() == Items.ARROW);
      if (limbSwingAmountEntityLiving != null) {
         moveTo(limbSwingAmountEntityLiving.getX(), limbSwingAmountEntityLiving.getY() + (double)limbSwingAmountEntityLiving.getEyeHeight(), limbSwingAmountEntityLiving.getZ(), limbSwingAmountEntityLiving.getYRot(), limbSwingAmountEntityLiving.getXRot());
      }
      double posX = getX() - (double)(Mth.cos(getYRot() / 180.0F * 3.1415927F) * 0.1F);
      double posY = getY() - 0.10000000149011612D;
      double posZ = getZ() - (double)(Mth.sin(getYRot() / 180.0F * 3.1415927F) * 0.1F);
      setPos(posX, posY, posZ);
      if (isNPC) {
         npc = (EntityNPCInterface)thrower;
         getStatProperties(npc.stats.ranged);
         refreshDimensions();
      }
   }

   public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> para) {
      if (Size.equals(para)) { refreshDimensions(); }
   }

   public void setThrownItem(ItemStack item) { entityData.set(ItemStackThrown, item); }

   public int getSize() {
      return entityData.get(Size);
   }

   public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
      return new EntityDimensions((float)getSize() / 10.0F, (float)getSize() / 10.0F, false);
   }

   public void shoot(double par1, double par3, double par5, float par7, float par8) {
      double f2 = Math.sqrt(par1 * par1 + par3 * par3 + par5 * par5);
      double f3 = Math.sqrt(par1 * par1 + par5 * par5);
      float yaw = (float)(Math.atan2(par1, par5) * 180.0D / 3.141592653589793D);
      float pitch = hasGravity() ? par7 : (float)(Math.atan2(par3, f3) * 180.0D / 3.141592653589793D);
      yRotO = yaw;
      xRotO = pitch;
      setYRot(yaw);
      setXRot(pitch);
      Vec3 m = (new Vec3(Mth.sin(yaw / 180.0F * 3.1415927F) * Mth.cos(pitch / 180.0F * 3.1415927F), Mth.sin((pitch + 1.0F) / 180.0F * 3.1415927F), Mth.cos(yaw / 180.0F * 3.1415927F) * Mth.cos(pitch / 180.0F * 3.1415927F))).add(random.nextGaussian() * 0.0075D * (double)par8, random.nextGaussian() * 0.0075D * (double)par8, random.nextGaussian() * 0.0075D * (double)par8).scale(getSpeed());
      setDeltaMovement(m);
      accelerationX = par1 / f2 * 0.1D;
      accelerationY = par3 / f2 * 0.1D;
      accelerationZ = par5 / f2 * 0.1D;
      ticksInGround = 0;
   }

   public float getAngleForXYZ(double ignoredVarX, double varY, double ignoredVarZ, float horizontalDist, boolean arc) {
      float g = getGravity();
      float var1 = getSpeed() * getSpeed();
      float var2 = g * horizontalDist;
      float var3 = (float)((double)(g * horizontalDist * horizontalDist) + 2.0D * varY * (double)var1);
      float var4 = var1 * var1 - g * var3;
      if (var4 < 0.0F) {
         return 30.0F;
      } else {
         float var6 = arc ? var1 + Mth.sqrt(var4) : var1 - Mth.sqrt(var4);
         return (float)(Math.atan2(var6, var2) * 180.0D / 3.141592653589793D);
      }
   }

   public void shoot(float speed) {
      double varX = -Mth.sin(getYRot() / 180.0F * 3.1415927F) * Mth.cos(getXRot() / 180.0F * 3.1415927F);
      double varZ = Mth.cos(getYRot() / 180.0F * 3.1415927F) * Mth.cos(getXRot() / 180.0F * 3.1415927F);
      double varY = -Mth.sin(getXRot() / 180.0F * 3.1415927F);
      shoot(varX, varY, varZ, -getXRot(), speed);
   }

   @OnlyIn(Dist.CLIENT)
   public void lerpTo(double par1, double par3, double par5, float par7, float par8, int par9, boolean bo) {
      if (!level().isClientSide || !inGround) {
         setPos(par1, par3, par5);
         setRot(par7, par8);
      }
   }

   public void tick() {
      super.baseTick();
      if (++tickCount % 10 == 0) {
         EventHooks.onProjectileTick(this);
      }
      Vec3 motion = getDeltaMovement();
      if (xRotO == 0.0F && yRotO == 0.0F) {
         double f = motion.horizontalDistance();
         setYRot((float)(Mth.atan2(motion.x, motion.z) * 57.2957763671875D));
         setXRot((float)(Mth.atan2(motion.y, f) * 57.2957763671875D));
         yRotO = getYRot();
         xRotO = getXRot();
      }
      BlockState state;
      if ((isArrow() || sticksToWalls()) && tilePos != BlockPos.ZERO && level().isLoaded(tilePos)) {
         state = level().getBlockState(tilePos);
         VoxelShape shape = state.getShape(level(), tilePos);
         if (!shape.isEmpty()) {
            AABB axisAlignedBB = shape.bounds();
            if (axisAlignedBB.contains(position())) {
               inGround = true;
            }
         }
      }
      if (arrowShake > 0) { --arrowShake; }
      if (inGround && level().isLoaded(tilePos)) {
         state = level().getBlockState(tilePos);
         if (state == inBlock) {
            ++ticksInGround;
            if (ticksInGround == CustomNpcs.ProjectileLifespan) { remove(RemovalReason.DISCARDED); }
         } else {
            inGround = false;
            setDeltaMovement(getDeltaMovement().multiply(random.nextFloat() * 0.2D, random.nextFloat() * 0.2D, random.nextFloat() * 0.2D));
            ticksInGround = 0;
            ticksInAir = 0;
         }
      } else {
         ++ticksInAir;
         if (ticksInAir == CustomNpcs.ProjectileLifespan) { remove(RemovalReason.DISCARDED); }
         Vec3 pos = position();
         Vec3 nextPos = pos.add(motion);
         HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHit);
         if (hitresult.getType() != Type.MISS) {
            entityData.set(Rotating, false);
            onHit(hitresult);
         }
         motion = getDeltaMovement();
         double f1 = motion.horizontalDistance();
         setXRot(lerpRotation(xRotO, (float)(Mth.atan2(motion.y, f1) * 57.2957763671875D)));
         setYRot(lerpRotation(yRotO, (float)(Mth.atan2(motion.x, motion.z) * 57.2957763671875D)));
         if (isRotating()) {
            int spin = isBlock() ? 10 : 20;
            setXRot(getXRot() - (float)spin * getSpeed());
         }
         double f2 = getMotionFactor();
         double f3 = getGravity();
         if (isInWater()) {
            for(int j = 0; j < 4; ++j) {
               level().addParticle(ParticleTypes.BUBBLE, nextPos.x - motion.x * 0.25D, nextPos.y - motion.y * 0.25D, nextPos.z - motion.z * 0.25D, motion.x, motion.y, motion.z);
            }
            f2 = 0.6D;
         }
         motion = motion.scale(f2);
         if (hasGravity()) {
            motion = motion.subtract(0.0D, f3, 0.0D);
         }
         if (accelerate) {
            motion = motion.add(accelerationX, accelerationY, accelerationZ);
         }
         if (level().isClientSide && entityData.get(Particle) > 0) {
            level().addParticle(Objects.requireNonNull(ParticleType.getMCType(entityData.get(Particle))), getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
         }
         setDeltaMovement(motion);
         setPos(nextPos.x, nextPos.y, nextPos.z);
         checkInsideBlocks();
      }
   }

   protected boolean canHit(Entity entity) {
      if (super.canHitEntity(entity) && entity != thrower && (npc == null || entity != npc && !npc.isAlliedTo(entity))) {
         if (entity instanceof Player entityplayer) {
            return !entityplayer.getAbilities().invulnerable && (!(thrower instanceof Player) || ((Player) thrower).canHarmPlayer(entityplayer));
         }
         return true;
      }
      return false;
   }

   public boolean isBlock() {
      ItemStack item = getItemDisplay();
      return !item.isEmpty() && item.getItem() instanceof BlockItem;
   }

   private Item getItem() {
      ItemStack item = getItemDisplay();
      return item.isEmpty() ? Items.AIR : item.getItem();
   }

   protected float getMotionFactor() {
      return accelerate ? 0.95F : 1.0F;
   }

   protected void onHit(@NotNull HitResult movingObjectPosition) {
      if (!level().isClientSide) {
         BlockPos pos;
         Entity e = null;
         ProjectileEvent.ImpactEvent event;
         if (movingObjectPosition.getType() == Type.ENTITY) {
            e = ((EntityHitResult)movingObjectPosition).getEntity();
            pos = e.blockPosition();
            event = new ProjectileEvent.ImpactEvent(this, 0, e);
         } else {
            pos = ((BlockHitResult)movingObjectPosition).getBlockPos();
            event = new ProjectileEvent.ImpactEvent(this, 1, BlockWrapper.createNew(level(), pos, level().getBlockState(pos)));
         }
         if (pos == BlockPos.ZERO) {
            pos = new BlockPos((int)movingObjectPosition.getLocation().x, (int)movingObjectPosition.getLocation().y, (int)movingObjectPosition.getLocation().z);
         }
         if (callback != null && callback.onImpact(this, pos, e)) {
            return;
         }
         EventHooks.onProjectileImpact(this, event);
      }
      MobEffect p;
      if (movingObjectPosition.getType() == Type.ENTITY) {
         Entity e = ((EntityHitResult)movingObjectPosition).getEntity();
         float d = damage;
         if (d == 0.0F) { d = 0.001F; }
         if (e.hurt(damageSources().thrown(this, getOwner()), d)) {
            if (e instanceof LivingEntity entityLiving) {
               if (!level().isClientSide && (isArrow() || sticksToWalls())) {
                  entityLiving.setArrowCount(entityLiving.getArrowCount() + 1);
               }
               if (destroyedOnEntityHit && !(e instanceof EnderMan)) {
                  remove(RemovalReason.DISCARDED);
               }
               if (effect != 0) {
                  if (effect != 666) {
                     p = PotionEffectType.getMCType(effect);
                     if (p != null) { entityLiving.addEffect(new MobEffectInstance(p, duration * 20, amplify)); }
                  } else {
                     entityLiving.setRemainingFireTicks(duration * 20);
                  }
               }
            }

            if (isBlock()) {
               level().levelEvent(null, 2001, e.blockPosition(), Block.getId(((BlockItem)getItem()).getBlock().defaultBlockState()));
            } else if (!isArrow() && !sticksToWalls()) {
               for(int i = 0; i < 8; ++i) {
                  level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, getItemDisplay()), getX(), getY(), getZ(), random.nextGaussian() * 0.15D, random.nextGaussian() * 0.2D, random.nextGaussian() * 0.15D);
               }
            }

            if (punch > 0) {
               Vec3 m = getDeltaMovement();
               double f3 = m.horizontalDistance();
               if (f3 > 0.0D) {
                  e.push(m.x() * (double)punch * 0.6D / f3, 0.1D, m.z() * (double)punch * 0.6D / f3);
               }
            }
         } else if (hasGravity() && (isArrow() || sticksToWalls())) {
            setDeltaMovement(getDeltaMovement().scale(-0.1D));
            setYRot(getYRot() + 180.0F);
            yRotO += 180.0F;
            ticksInAir = 0;
         }
      }
      else if (!isArrow() && !sticksToWalls()) {
         if (isBlock()) {
            level().levelEvent(null, 2001, blockPosition(), Block.getId(((BlockItem)getItem()).getBlock().defaultBlockState()));
         } else {
            for(int i = 0; i < 8; ++i) {
               level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, getItemDisplay()), getX(), getY(), getZ(), random.nextGaussian() * 0.15D, random.nextGaussian() * 0.2D, random.nextGaussian() * 0.15D);
            }
         }
      }
      else {
         tilePos = ((BlockHitResult)movingObjectPosition).getBlockPos();
         inBlock = level().getBlockState(tilePos);
         Vec3 m = movingObjectPosition.getLocation().subtract(position());
         setDeltaMovement(m);
         Vec3 vector3d1 = m.normalize().scale(0.05000000074505806D);
         setPosRaw(getX() - vector3d1.x, getY() - vector3d1.y, getZ() - vector3d1.z);
         inGround = true;
         arrowShake = 7;
         if (!hasGravity()) { entityData.set(Gravity, true); }
         if (inBlock != null) { inBlock.entityInside(level(), tilePos, this); }
      }
      if (explosiveRadius > 0) {
         boolean terrainDamage = level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && explosiveDamage;
         level().explode(getOwner() == null ? this : getOwner(), getX(), getY(), getZ(), (float)explosiveRadius, effect == 666, terrainDamage ? ExplosionInteraction.TNT : ExplosionInteraction.NONE);
         if (effect != 0) {
            AABB axisAlignedBB = getBoundingBox().inflate(explosiveRadius * 2.0D, explosiveRadius * 2.0D, explosiveRadius * 2.0D);
            List<LivingEntity> list1 = level().getEntitiesOfClass(LivingEntity.class, axisAlignedBB);
            p = PotionEffectType.getMCType(effect);
            for (LivingEntity entity : list1) {
               if (effect != 666) {
                  if (p != null) { entity.addEffect(new MobEffectInstance(p, duration * 20, amplify)); }
               } else {
                  entity.setRemainingFireTicks(duration * 20);
               }
            }
            level().levelEvent(null, 2002, blockPosition(), getPotionColor(effect));
         }
         remove(RemovalReason.DISCARDED);
      }
      if (!level().isClientSide && !isArrow() && !sticksToWalls()) { remove(RemovalReason.DISCARDED); }
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      compound.putShort("xTile", (short)tilePos.getX());
      compound.putShort("yTile", (short)tilePos.getY());
      compound.putShort("zTile", (short)tilePos.getZ());
      if (inBlock != null) {
         compound.put("inBlockState", NbtUtils.writeBlockState(inBlock));
      }
      compound.putByte("shake", (byte)throwableShake);
      compound.putBoolean("inGround", inGround);
      compound.putBoolean("isArrow", isArrow());
      Vec3 m = getDeltaMovement();
      compound.put("direction", newDoubleList(m.x, m.y, m.z));
      compound.putBoolean("canBePickedUp", canBePickedUp);
      if ((throwerName == null || throwerName.isEmpty()) && thrower != null && thrower instanceof Player) {
         throwerName = thrower.getUUID().toString();
      }
      compound.putString("ownerName", throwerName == null ? "" : throwerName);
      compound.put("Item", getItemDisplay().save(new CompoundTag()));
      compound.putFloat("damagev2", damage);
      compound.putInt("punch", punch);
      compound.putInt("size", entityData.get(Size));
      compound.putInt("velocity", entityData.get(Velocity));
      compound.putInt("explosiveRadius", explosiveRadius);
      compound.putInt("effectDuration", duration);
      compound.putBoolean("gravity", hasGravity());
      compound.putBoolean("accelerate", accelerate);
      compound.putBoolean("glows", entityData.get(Glows));
      compound.putInt("PotionEffect", effect);
      compound.putInt("trailenum", entityData.get(Particle));
      compound.putBoolean("Render3D", entityData.get(Is3d));
      compound.putBoolean("Spins", entityData.get(Rotating));
      compound.putBoolean("Sticks", entityData.get(Sticks));
      compound.putInt("accuracy", accuracy);
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      tilePos = new BlockPos(compound.getShort("xTile"), compound.getShort("yTile"), compound.getShort("zTile"));
      if (compound.contains("inBlockState", 10)) {
         inBlock = NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), compound.getCompound("inBlockState"));
      }
      throwableShake = compound.getByte("shake") & 255;
      inGround = compound.getByte("inGround") == 1;
      entityData.set(Arrow, compound.getBoolean("isArrow"));
      throwerName = compound.getString("ownerName");
      canBePickedUp = compound.getBoolean("canBePickedUp");
      damage = compound.getFloat("damagev2");
      punch = compound.getInt("punch");
      explosiveRadius = compound.getInt("explosiveRadius");
      duration = compound.getInt("effectDuration");
      accelerate = compound.getBoolean("accelerate");
      effect = compound.getInt("PotionEffect");
      accuracy = compound.getInt("accuracy");
      entityData.set(Particle, compound.getInt("trailenum"));
      entityData.set(Size, compound.getInt("size"));
      entityData.set(Glows, compound.getBoolean("glows"));
      entityData.set(Velocity, compound.getInt("velocity"));
      entityData.set(Gravity, compound.getBoolean("gravity"));
      entityData.set(Is3d, compound.getBoolean("Render3D"));
      entityData.set(Rotating, compound.getBoolean("Spins"));
      entityData.set(Sticks, compound.getBoolean("Sticks"));
      if (throwerName != null && throwerName.isEmpty()) { throwerName = null; }
      if (compound.contains("direction")) {
         ListTag tagList = compound.getList("direction", 6);
         setDeltaMovement(new Vec3(tagList.getDouble(0), tagList.getDouble(1), tagList.getDouble(2)));
      }
      CompoundTag var2 = compound.getCompound("Item");
      ItemStack item = ItemStack.of(var2);
      if (item.isEmpty()) { discard(); }
      else { entityData.set(ItemStackThrown, item); }
   }

   public Entity getOwner() {
      if (throwerName != null && !throwerName.isEmpty()) {
         try {
            UUID uuid = UUID.fromString(throwerName);
            if (thrower == null) {
               thrower = level().getPlayerByUUID(uuid);
            }
         } catch (IllegalArgumentException ignored) {}
         return thrower;
      } else {
         return null;
      }
   }

   private int getPotionColor(int p) {
       return switch (p) {
           case 2 -> 32698;
           case 9, 20 -> 32732;
           case 15 -> 15;
           case 17, 19 -> 32660;
           case 18 -> 32696;
           default -> 0;
       };
   }

   public void getStatProperties(DataRanged stats) {
      damage = (float)stats.getStrength();
      punch = stats.getKnockback();
      accelerate = stats.getAccelerate();
      explosiveRadius = stats.getExplodeSize();
      effect = stats.getEffectType();
      duration = stats.getEffectTime();
      amplify = stats.getEffectStrength();
      setParticleEffect(stats.getParticle());
      entityData.set(Size, stats.getSize());
      entityData.set(Glows, stats.getGlows());
      setSpeed(stats.getSpeed());
      setHasGravity(stats.getHasGravity());
      setIs3D(stats.getRender3D());
      setRotating(stats.getSpins());
      setStickInWall(stats.getSticks());
   }

   public void setParticleEffect(int type) {
      entityData.set(Particle, type);
   }

   public void setHasGravity(boolean bo) {
      entityData.set(Gravity, bo);
   }

   public void setIs3D(boolean bo) {
      entityData.set(Is3d, bo);
   }

   public void setStickInWall(boolean bo) {
      entityData.set(Sticks, bo);
   }

   public ItemStack getItemDisplay() {
      return entityData.get(ItemStackThrown);
   }

   /** @deprecated */
   @SuppressWarnings("all")
   @Deprecated
   public float getLightLevelDependentMagicValue() {
      return entityData.get(Glows) ? 1.0F : super.getLightLevelDependentMagicValue();
   }

   public boolean hasGravity() {
      return entityData.get(Gravity);
   }

   public void setSpeed(int speed) {
      entityData.set(Velocity, speed);
   }

   public float getSpeed() {
      return (float) entityData.get(Velocity) / 10.0F;
   }

   public boolean isArrow() {
      return entityData.get(Arrow);
   }

   public void setRotating(boolean bo) {
      entityData.set(Rotating, bo);
   }

   public boolean isRotating() {
      return entityData.get(Rotating);
   }

   public boolean glows() {
      return entityData.get(Glows);
   }

   public boolean is3D() {
      return entityData.get(Is3d) || isBlock();
   }

   public boolean sticksToWalls() {
      return is3D() && entityData.get(Sticks);
   }

   public void playerTouch(@NotNull Player playerIn) {
      if (!level().isClientSide && canBePickedUp && inGround && arrowShake <= 0) {
         if (playerIn.getInventory().add(getItemDisplay())) {
            inGround = false;
            playSound(SoundEvents.ITEM_PICKUP, 0.2F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            playerIn.take(this, 1);
            discard();
         }
      }
   }

   protected @NotNull MovementEmission getMovementEmission() {
      return MovementEmission.NONE;
   }

   public @NotNull Component getDisplayName() {
      return !getItemDisplay().isEmpty() ? getItemDisplay().getDisplayName() : super.getDisplayName();
   }

   public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
      Entity entity = getOwner();
      return new ClientboundAddEntityPacket(this, entity == null ? 0 : entity.getId());
   }

   public interface IProjectileCallback {
      boolean onImpact(EntityProjectile var1, BlockPos var2, Entity var3);
   }

   @Override
   public void remove(@Nonnull RemovalReason reason) {
      super.remove(reason);
      if (reason == RemovalReason.KILLED ||
              reason == RemovalReason.DISCARDED ||
              reason == RemovalReason.UNLOADED_WITH_PLAYER) {
         scripts.clear();
      }
   }

}
