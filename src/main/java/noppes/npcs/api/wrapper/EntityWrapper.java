package noppes.npcs.api.wrapper;

import com.google.common.collect.ImmutableList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;

import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import noppes.npcs.api.*;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IEntityItem;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.util.IRayTraceResults;
import noppes.npcs.api.wrapper.data.Data;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.mixin.world.entity.IEntityMixin;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

public class EntityWrapper<T extends Entity> implements IEntity<T> {

   protected T entity;
   private IWorld levelWrapper;
   protected final IData tempdata = new Data();
   protected final IData storeddata;

   @SuppressWarnings("all")
   public EntityWrapper(T entityIn) {
      entity = entityIn;
      Data sData = new Data();
      try {
         MethodHandle handle = MethodHandles.lookup()
                 .findVirtual(Entity.class, "npcs$getStoredData",
                         MethodType.methodType(Data.class));
         sData = (Data) handle.invokeExact(entityIn);
      }
      catch (Throwable e) { LogWriter.error(e); }
      storeddata = sData;
      resetLevel();
   }

   @SuppressWarnings("all")
   private void resetLevel() {
      if (entity.level() instanceof ServerLevel) {
         levelWrapper = Objects.requireNonNull(NpcAPI.Instance()).getIWorld(entity.level());
      }
      else if (entity.level() != null) {
         WorldWrapper w = WrapperNpcAPI.worldCache.get(entity.level().dimension());
         if (w != null) {
            if (w.level == null) { w.level = entity.level(); }
         } else {
            WrapperNpcAPI.worldCache.put(entity.level().dimension(), w = WorldWrapper.createNew(entity.level()));
         }
         levelWrapper = w;
      }
   }

   public double getX() {
      return this.entity.getX();
   }

   public void setX(double x) {
      this.entity.setPos(x, this.entity.getY(), this.entity.getZ());
   }

   public double getY() {
      return this.entity.getY();
   }

   public void setY(double y) {
      this.entity.setPos(this.entity.getX(), y, this.entity.getZ());
   }

   public double getZ() {
      return this.entity.getZ();
   }

   public void setZ(double z) {
      this.entity.setPos(this.entity.getX(), this.entity.getY(), z);
   }

   public int getBlockX() {
      return Mth.floor(this.entity.getX());
   }

   public int getBlockY() {
      return Mth.floor(this.entity.getY());
   }

   public int getBlockZ() {
      return Mth.floor(this.entity.getZ());
   }

   public String getEntityName() {
      String s = this.entity.getType().getDescriptionId();
      return Language.getInstance().getOrDefault(s);
   }

   public String getName() {
      return this.entity.getName().getString();
   }

   public void setName(String name) {
      this.entity.setCustomName(Component.literal(name));
   }

   public boolean hasCustomName() {
      return this.entity.hasCustomName();
   }

   public void setPosition(double x, double y, double z) {
      this.entity.setPos(x, y, z);
   }

   public IWorld getWorld() {
      if (levelWrapper == null || entity.level() != levelWrapper.getMCLevel()) { resetLevel(); }
      return levelWrapper;
   }

   public boolean isAlive() {
      return this.entity.isAlive();
   }

   public IData getTempdata() { return tempdata; }

   public IData getStoreddata() { return storeddata; }

   public long getAge() {
      return this.entity.tickCount;
   }

   public void damage(float amount) {
      if (getType() != 1 || ((IPlayer<?>) this).getGamemode() != 1 && ((IPlayer<?>) this).getGamemode() != 3) {
         entity.hurt(this.entity.damageSources().genericKill(), amount);
      }
   }

   @Override
   public void damage(float amount, IEntity<?> source) {
      if (source == null) {
         damage(amount);
         return;
      }
      entity.hurt(NpcEntityDamageSource.create("npc", source.getMCEntity()), amount);
   }

   @Override
   public void damage(float amount, IEntityDamageSource damageSource) {
      entity.hurt((DamageSource) damageSource, amount);
   }

   public void despawn() {
      this.entity.discard();
   }

   public void spawn() {
      if (levelWrapper == null || levelWrapper.getMCLevel().isClientSide) {
         return;
      }
      if (levelWrapper.getEntity(entity.getStringUUID()) != null) {
         throw new CustomNPCsException("Entity is already spawned");
      }
      ((IEntityMixin) entity).setRemoval(null);
      levelWrapper.getMCLevel().addFreshEntity(entity);
   }

   public void kill() {
      this.entity.kill();
   }

   public boolean inWater() {
      return this.entity.isInWater();
   }

   public boolean inLava() {
      return this.entity.isInLava();
   }

   public boolean inFire() {
      return this.entity.level().getBlockStates(this.entity.getBoundingBox()).anyMatch((state) -> state.is(BlockTags.FIRE));
   }

   public boolean isBurning() {
      return this.entity.isOnFire();
   }

   public void setBurning(int ticks) {
      this.entity.setRemainingFireTicks(ticks);
   }

   public void extinguish() {
      this.entity.clearFire();
   }

   public String getTypeName() {
      return this.entity.getEncodeId();
   }

   public IEntityItem<?> dropItem(IItemStack item) {
      return (IEntityItem<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(this.entity.spawnAtLocation(item.getMCItemStack(), 0.0F));
   }

   public IEntity<?>[] getRiders() {
      NpcAPI api = NpcAPI.Instance();
      if (api == null) { return new IEntity<?>[0]; }
      List<Entity> list = this.entity.getPassengers();
      IEntity<?>[] riders = new IEntity[list.size()];
      for(int i = 0; i < list.size(); ++i) { riders[i] = api.getIEntity(list.get(i)); }
      return riders;
   }

   public IRayTrace rayTraceBlock(double distance, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox) {
      Vec3 vec3d = this.entity.getEyePosition(1.0F);
      Vec3 vec3d1 = this.entity.getViewVector(1.0F);
      Vec3 vec3d2 = vec3d.add(vec3d1.x * distance, vec3d1.y * distance, vec3d1.z * distance);
      BlockHitResult result = this.entity.level().clip(new ClipContext(vec3d, vec3d2, Block.OUTLINE, stopOnLiquid ? Fluid.ANY : Fluid.NONE, this.entity));
      if (result.getType() == Type.MISS) {
         return null;
      } else {
         return new RayTraceWrapper(Objects.requireNonNull(NpcAPI.Instance()).getIBlock(this.entity.level(), result.getBlockPos()), result.getDirection().get3DDataValue());
      }
   }

   public IEntity<?>[] rayTraceEntities(double distance, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox) {
      Vec3 vec3d = this.entity.getEyePosition(1.0F);
      Vec3 vec3d1 = this.entity.getViewVector(1.0F);
      Vec3 vec3d2 = vec3d.add(vec3d1.x * distance, vec3d1.y * distance, vec3d1.z * distance);
      HitResult result = this.entity.level().clip(new ClipContext(vec3d, vec3d2, Block.COLLIDER, stopOnLiquid ? Fluid.ANY : Fluid.NONE, this.entity));
      if (result.getType() != Type.MISS) {
         vec3d2 = result.getLocation();
      }
      return findIEntityOnPath(entity, distance, vec3d, vec3d2);
   }

   public static List<Entity> findEntityOnPath(Entity entity, double distance, Vec3 vec3d, Vec3 vec3d1) {
      List<Entity> list = entity.level().getEntities(entity, entity.getBoundingBox().inflate(distance));
      List<Entity> result = new ArrayList<>();
      for (Entity entity1 : list) {
         if (entity1 != entity) {
            AABB axisAlignedBB = entity1.getBoundingBox().inflate(entity1.getPickRadius());
            Optional<Vec3> optional = axisAlignedBB.clip(vec3d, vec3d1);
            if (optional.isPresent()) {
               result.add(entity1);
            }
         }
      }
      result.sort((o1, o2) -> {
         double d1 = entity.distanceToSqr(o1);
         double d2 = entity.distanceToSqr(o2);
         if (d1 == d2) {
            return 0;
         } else {
            return d1 > d2 ? 1 : -1;
         }
      });
      return result;
   }

   public static IEntity<?>[] findIEntityOnPath(Entity entity, double distance, Vec3 vec3d, Vec3 vec3d1) {
      List<IEntity<?>> result = new ArrayList<>();
      for (Entity e : findEntityOnPath(entity, distance, vec3d, vec3d1)) {
         result.add(Objects.requireNonNull(NpcAPI.Instance()).getIEntity(e));
      }
      return result.toArray(new IEntity[0]);
   }

   public IEntity<?>[] getAllRiders() {
      NpcAPI api = NpcAPI.Instance();
      if (api == null) { return new IEntity<?>[0]; }
      List<Entity> list = ImmutableList.copyOf(this.entity.getIndirectPassengers());
      IEntity<?>[] riders = new IEntity[list.size()];
      for(int i = 0; i < list.size(); ++i) { riders[i] = api.getIEntity(list.get(i)); }
      return riders;
   }

   public void addRider(IEntity<?> entity) {
      if (entity != null) {
         entity.getMCEntity().startRiding(this.entity, true);
      }
   }

   public void clearRiders() {
      this.entity.ejectPassengers();
   }

   public IEntity<?> getMount() {
      return Objects.requireNonNull(NpcAPI.Instance()).getIEntity(this.entity.getVehicle());
   }

   public void setMount(IEntity<?> entity) {
      if (entity == null) {
         this.entity.stopRiding();
      } else {
         this.entity.startRiding(entity.getMCEntity(), true);
      }

   }

   public void setRotation(float rotation) {
      this.entity.setYRot(rotation);
   }

   public float getRotation() {
      return this.entity.getYRot();
   }

   public void setPitch(float rotation) {
      this.entity.setXRot(rotation);
   }

   public float getPitch() {
      return this.entity.getXRot();
   }

   public void knockback(int power, float direction) {
      float v = direction * 3.1415927F / 180.0F;
      this.entity.push(-Mth.sin(v) * (float)power, 0.1D + (double)((float)power * 0.04F), Mth.cos(v) * (float)power);
      this.entity.setDeltaMovement(this.entity.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
      this.entity.hurtMarked = true;
   }

   public boolean isSneaking() {
      return this.entity.isCrouching();
   }

   public boolean isSprinting() {
      return this.entity.isSprinting();
   }

   public T getMCEntity() {
      return this.entity;
   }

   public int getType() {
      return 0;
   }

   public boolean typeOf(int type) {
      if (type == noppes.npcs.api.constants.EntityType.ANY.get()) { return true; }
      return type == getType();
   }

   public String getUUID() {
      return this.entity.getUUID().toString();
   }

   public String generateNewUUID() {
      UUID id = UUID.randomUUID();
      entity.setUUID(id);
      return id.toString();
   }

   public INbt getNbt() { return new NBTWrapper(entity.getPersistentData()); }

   public void storeAsClone(int tab, String name) {
      CompoundTag compound = new CompoundTag();
      if (!this.entity.saveAsPassenger(compound)) {
         throw new CustomNPCsException("Cannot store dead entities");
      } else {
         ServerCloneController.Instance.addClone(compound, name, tab);
      }
   }

   public INbt getEntityNbt() {
      CompoundTag compound = new CompoundTag();
      this.entity.saveWithoutId(compound);
      ResourceLocation resourcelocation = EntityType.getKey(entity.getType());
      if (getType() == 1) {
         resourcelocation = new ResourceLocation("minecraft", "player");
      }
      compound.putString("id", resourcelocation.toString());
      return new NBTWrapper(compound);
   }

   public void setEntityNbt(INbt nbt) {
      this.entity.load(nbt.getMCNBT());
   }

   public void playAnimation(int type) {
      if (levelWrapper.getMCLevel().isClientSide()) {
         if (type == 0) { ((LivingEntity) entity).swing(InteractionHand.MAIN_HAND); }
         else if (type == 1) { entity.animateHurt(0.0f); }
         else if (type == 2) { ((Player) entity).stopSleepInBed(false, false); }
         else if (type == 3) { ((LivingEntity) entity).swing(InteractionHand.OFF_HAND); }
      }
      else { ((ServerLevel) levelWrapper.getMCLevel()).getChunkSource().broadcastAndSend(entity, new ClientboundAnimatePacket(entity, type)); }
   }

   public float getHeight() { return entity.getBbHeight(); }

   public float getEyeHeight() { return entity.getEyeHeight(); }

   public float getWidth() { return entity.getBbWidth(); }

   public IPos getPos() { return new BlockPosWrapper(entity.level(), entity.getX(), entity.getY(), entity.getZ()); }

   public void setPos(IPos pos) { this.entity.setPos((float)pos.getX() + 0.5F, pos.getY(), (float)pos.getZ() + 0.5F); }

   public String[] getTags() { return this.entity.getTags().toArray(new String[0]); }

   public void addTag(String tag) { this.entity.addTag(tag); }

   public boolean hasTag(String tag) {
      return this.entity.getTags().contains(tag);
   }

   public void removeTag(String tag) {
      this.entity.removeTag(tag);
   }

   public double getMotionX() {
      return this.entity.getDeltaMovement().x;
   }

   public double getMotionY() {
      return this.entity.getDeltaMovement().y;
   }

   public double getMotionZ() {
      return this.entity.getDeltaMovement().z;
   }

   public void setMotionX(double motion) {
      Vec3 mo = this.entity.getDeltaMovement();
      if (mo.x != motion) {
         this.entity.setDeltaMovement(motion, mo.y, mo.z);
         this.entity.hurtMarked = true;
      }
   }

   public void setMotionY(double motion) {
      Vec3 mo = this.entity.getDeltaMovement();
      if (mo.y != motion) {
         this.entity.setDeltaMovement(mo.x, motion, mo.z);
         this.entity.hurtMarked = true;
      }
   }

   public void setMotionZ(double motion) {
      Vec3 mo = this.entity.getDeltaMovement();
      if (mo.z != motion) {
         this.entity.setDeltaMovement(mo.x, mo.y, motion);
         this.entity.hurtMarked = true;
      }
   }

   // New from Unofficial (BetaZavr)
   @Override
   public IRayTraceResults rayTrace(double distance) {
      return Util.instance.rayTraceBlocksAndEntitys(entity, entity.getYRot(), entity.getXRot(), distance);
   }

}
