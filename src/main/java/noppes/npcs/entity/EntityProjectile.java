package noppes.npcs.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.constants.ParticleType;
import noppes.npcs.api.constants.PotionEffectType;
import noppes.npcs.api.entity.IProjectile;
import noppes.npcs.api.event.ProjectileEvent;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.entity.data.DataRanged;

import javax.annotation.Nonnull;

public class EntityProjectile extends EntityThrowable {

	public interface IProjectileCallback {
		boolean onImpact(EntityProjectile p0, BlockPos p1, Entity p2);
	}

	private static final DataParameter<Boolean> Arrow = EntityDataManager.createKey(EntityProjectile.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> Glows = EntityDataManager.createKey(EntityProjectile.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> Gravity = EntityDataManager.createKey(EntityProjectile.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> Is3d = EntityDataManager.createKey(EntityProjectile.class, DataSerializers.BOOLEAN);
	private static final DataParameter<ItemStack> ItemStackThrown = EntityDataManager.createKey(EntityProjectile.class, DataSerializers.ITEM_STACK);
	private static final DataParameter<Integer> Particle = EntityDataManager.createKey(EntityProjectile.class, DataSerializers.VARINT);
	private static final DataParameter<Boolean> Rotating = EntityDataManager.createKey(EntityProjectile.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Integer> Size = EntityDataManager.createKey(EntityProjectile.class, DataSerializers.VARINT);
	private static final DataParameter<Boolean> Sticks = EntityDataManager.createKey(EntityProjectile.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Integer> Velocity = EntityDataManager.createKey(EntityProjectile.class, DataSerializers.VARINT);

	protected boolean inGround = false;
	public boolean accelerate = false;
	public boolean canBePickedUp = false;
	public boolean destroyedOnEntityHit = true;
	public boolean explosiveDamage = true;
	private int inData = 0;
	private int ticksInGround;
	public int accuracy = 60;
	public int amplify = 0;
	public int arrowShake = 0;
	public int duration = 5;
	public int effect = 0;
	public int explosiveRadius = 0;
	public int punch = 0; // knockback
	public int throwableShake = 0;
	public int ticksInAir = 0;
	public float damage = 5.0f;
	private double accelerationX;
	private double accelerationY;
	private double accelerationZ;
	private Block inTile;
	private EntityNPCInterface npc;
	public List<ScriptContainer> scripts = new ArrayList<>();
	public IProjectileCallback callback;
	private EntityLivingBase thrower = null;
	private String throwerName = null;

	private BlockPos tilePos = BlockPos.ORIGIN;

	@SuppressWarnings("unused")
	public EntityProjectile(World world) {
		super(world);
		setSize(0.25f, 0.25f);
	}

	public EntityProjectile(World world, EntityLivingBase throwerIn, ItemStack item, boolean isNPC) {
		super(world);
		thrower = throwerIn;
		if (thrower != null) { throwerName = thrower.getUniqueID().toString(); }
		setThrownItem(item);
		dataManager.set(EntityProjectile.Arrow, getItem() instanceof ItemArrow);
		setSize(getSize() / 10.0f, getSize() / 10.0f);
		if (thrower != null) { setLocationAndAngles(thrower.posX, thrower.posY + thrower.getEyeHeight(),  thrower.posZ, thrower.rotationYaw, thrower.rotationPitch); }
		posX -= MathHelper.cos(rotationYaw / 180.0f * 3.1415927f) * 0.1f;
		posY -= 0.10000000149011612;
		posZ -= MathHelper.sin(rotationYaw / 180.0f * 3.1415927f) * 0.1f;
		setPosition(posX, posY, posZ);
		if (isNPC && thrower != null) {
			npc = (EntityNPCInterface) thrower;
			getStatProperties(npc.stats.ranged);
		}
	}

	@Override
	protected boolean canTriggerWalking() { return false; }

	@Override
	protected void entityInit() {
		dataManager.register(EntityProjectile.ItemStackThrown, ItemStack.EMPTY);
		dataManager.register(EntityProjectile.Velocity, 10);
		dataManager.register(EntityProjectile.Size, 10);
		dataManager.register(EntityProjectile.Particle, 0);
		dataManager.register(EntityProjectile.Gravity, false);
		dataManager.register(EntityProjectile.Glows, false);
		dataManager.register(EntityProjectile.Arrow, false);
		dataManager.register(EntityProjectile.Is3d, false);
		dataManager.register(EntityProjectile.Rotating, false);
		dataManager.register(EntityProjectile.Sticks, false);
	}

	public static double maxBallisticRange(double speed, double eyeHeight) {
		if (speed <= 0.0d) { return 0.0d; }
		double g = 0.03d;
		return speed / g * Math.sqrt(speed * speed + 2.0d * g * eyeHeight) * 0.75d;
	}

	public float getAngleForXYZ(double varY, double horizontalDist, boolean arc) {
		float g = getGravityVelocity();
		float var1 = getSpeed() * getSpeed();
		double var2 = g * horizontalDist;
		double var3 = g * horizontalDist * horizontalDist + 2.0 * varY * var1;
		double var4 = var1 * var1 - g * var3;
		if (var4 < 0.0) {
			return 45.0f;
		}
		float var5 = arc ? (var1 + MathHelper.sqrt(var4)) : (var1 - MathHelper.sqrt(var4));
        return (float) Math.atan2(var5, var2) * 180.0f / 3.141592653589793f;
	}

	@Override
	public float getBrightness() {
		return dataManager.get(EntityProjectile.Glows) ? 1.0f : super.getBrightness();
	}

	@SideOnly(Side.CLIENT)
	public int getBrightnessForRender() {
		return dataManager.get(EntityProjectile.Glows) ? 15728880 : super.getBrightnessForRender();
	}

	public @Nonnull ITextComponent getDisplayName() {
		if (!getItemDisplay().isEmpty()) {
			return new TextComponentTranslation(getItemDisplay().getDisplayName());
		}
		return super.getDisplayName();
	}

	private Item getItem() {
		ItemStack item = getItemDisplay();
		if (item.isEmpty()) {
			return Items.AIR;
		}
		return item.getItem();
	}

	public ItemStack getItemDisplay() {
		try {
			return dataManager.get(EntityProjectile.ItemStackThrown);
		} catch (Exception ex) {
			return ItemStack.EMPTY;
		}
	}

	protected float getMotionFactor() {
		return accelerate ? 0.95f : 1.0f;
	}

	private int getPotionColor(int p) {
		switch (p) {
			case 2:
				case 3: {
				return 32660;
			}
				case 4: {
				return 32696;
			}
			case 5: {
				return 32698;
			}
			case 6:
				case 8: {
				return 32732;
			}
			case 7: {
				return 15;
			}
				default: {
				return 0;
			}
		}
	}

	public int getSize() {
		return dataManager.get(EntityProjectile.Size);
	}

	public float getSpeed() {
		return dataManager.get(EntityProjectile.Velocity) / 10.0f;
	}

	public void getStatProperties(DataRanged stats) {
		damage = stats.getStrength();
		punch = stats.getKnockback();
		accelerate = stats.getAccelerate();
		explosiveRadius = stats.getExplodeSize();
		effect = stats.getEffectType();
		duration = stats.getEffectTime();
		amplify = stats.getEffectStrength();
		setParticleEffect(stats.getParticle());
		dataManager.set(EntityProjectile.Size, stats.getSize());
		dataManager.set(EntityProjectile.Glows, stats.getGlows());
		setSpeed(stats.getSpeed());
		setHasGravity(stats.getHasGravity());
		setIs3D(stats.getRender3D());
		setRotating(stats.getSpins());
		setStickInWall(stats.getSticks());
	}

	public EntityLivingBase getThrower() {
		if (throwerName == null || throwerName.isEmpty()) {
			return null;
		}
		try {
			UUID uuid = UUID.fromString(throwerName);
			if (thrower == null) {
				thrower = world.getPlayerEntityByUUID(uuid);
			}
		} catch (Exception e) { LogWriter.error(e); }
		return thrower;
	}

	public boolean glows() { return dataManager.get(EntityProjectile.Glows); }

	public boolean hasGravity() { return dataManager.get(EntityProjectile.Gravity); }

	public boolean is3D() { return dataManager.get(EntityProjectile.Is3d) || isBlock(); }

	public boolean isArrow() { return dataManager.get(EntityProjectile.Arrow); }

	public boolean isBlock() {
		ItemStack item = getItemDisplay();
		return !item.isEmpty() && item.getItem() instanceof ItemBlock;
	}

	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRenderDist(double par1) {
		double d1 = getEntityBoundingBox().getAverageEdgeLength() * 4.0;
		d1 *= 64.0;
		return par1 < d1 * d1;
	}

	public boolean isRotating() { return dataManager.get(EntityProjectile.Rotating); }

	public void onCollideWithPlayer(@Nonnull EntityPlayer player) {
		if (world.isRemote || !canBePickedUp || !inGround || arrowShake > 0) {
			return;
		}
		if (player.inventory.addItemStackToInventory(getItemDisplay())) {
			inGround = false;
			playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.2f, ((rand.nextFloat() - rand.nextFloat()) * 0.7f + 1.0f) * 2.0f);
			player.onItemPickup(this, 1);
			setDead();
		}
	}

	protected void onImpact(@Nonnull RayTraceResult movingobjectposition) {
		if (!world.isRemote) {
			BlockPos pos;
			ProjectileEvent.ImpactEvent event;
			if (movingobjectposition.entityHit != null) {
				pos = movingobjectposition.entityHit.getPosition();
				event = new ProjectileEvent.ImpactEvent((IProjectile<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(this), 0,
						movingobjectposition.entityHit);
			} else {
				pos = movingobjectposition.getBlockPos();
				event = new ProjectileEvent.ImpactEvent((IProjectile<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(this), 1, Objects.requireNonNull(NpcAPI.Instance()).getIBlock(world, pos));
			}
			if (pos == BlockPos.ORIGIN) {
				pos = new BlockPos(movingobjectposition.hitVec);
			}
			if (callback != null && callback.onImpact(this, pos, movingobjectposition.entityHit)) {
				return;
			}
			EventHooks.onProjectileImpact(this, event);
		}
		if (movingobjectposition.entityHit != null) {
			float d = damage;
			if (d == 0.0f) { d = 0.001f; }
			if (movingobjectposition.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), d)) {
				if (movingobjectposition.entityHit instanceof EntityLivingBase && (isArrow() || sticksToWalls())) {
					EntityLivingBase entityliving = (EntityLivingBase) movingobjectposition.entityHit;
					if (!world.isRemote) {
						entityliving.setArrowCountInEntity(entityliving.getArrowCountInEntity() + 1);
					}
					if (destroyedOnEntityHit && !(movingobjectposition.entityHit instanceof EntityEnderman)) {
						setDead();
					}
				}
				if (isBlock()) {
					world.playEvent(null, 2001, movingobjectposition.entityHit.getPosition(),
							Item.getIdFromItem(getItem()));
				} else if (!isArrow() && !sticksToWalls()) {
					int[] intArr = { Item.getIdFromItem(getItem()) };
					if (getItem().getHasSubtypes()) {
						intArr = new int[] { Item.getIdFromItem(getItem()), getItemDisplay().getMetadata() };
					}
					for (int i = 0; i < 8; ++i) {
						world.spawnParticle(EnumParticleTypes.ITEM_CRACK, posX, posY, posZ,
								rand.nextGaussian() * 0.15, rand.nextGaussian() * 0.2,
								rand.nextGaussian() * 0.15, intArr);
					}
				}
				if (effect != 0 && movingobjectposition.entityHit instanceof EntityLivingBase) {
					if (effect != 1) {
						Potion p = PotionEffectType.getMCType(effect);
						if (p != null) {
							((EntityLivingBase) movingobjectposition.entityHit).addPotionEffect(new PotionEffect(p, duration * 20, amplify));
						}
					} else {
						movingobjectposition.entityHit.setFire(duration);
					}
				}
			} else if (hasGravity() && (isArrow() || sticksToWalls())) {
				motionX *= -0.10000000149011612;
				motionY *= -0.10000000149011612;
				motionZ *= -0.10000000149011612;
				rotationYaw += 180.0f;
				prevRotationYaw += 180.0f;
				ticksInAir = 0;
			}
		} else if (isArrow() || sticksToWalls()) {
			tilePos = movingobjectposition.getBlockPos();
			IBlockState state = world.getBlockState(tilePos);
			inTile = state.getBlock();
			inData = inTile.getMetaFromState(state);
			motionX = (movingobjectposition.hitVec.x - posX);
			motionY = (movingobjectposition.hitVec.y - posY);
			motionZ = (movingobjectposition.hitVec.z - posZ);
			float f4 = MathHelper
					.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
			posX -= motionX / f4 * 0.05000000074505806;
			posY -= motionY / f4 * 0.05000000074505806;
			posZ -= motionZ / f4 * 0.05000000074505806;
			inGround = true;
			arrowShake = 7;
			if (!hasGravity()) {
				dataManager.set(EntityProjectile.Gravity, true);
			}
			if (inTile != null) {
				inTile.onEntityCollidedWithBlock(world, tilePos, state, this);
			}
		} else if (isBlock()) {
			world.playEvent(null, 2001, getPosition(), Item.getIdFromItem(getItem()));
		} else {
			int[] intArr2 = { Item.getIdFromItem(getItem()) };
			if (getItem().getHasSubtypes()) {
				intArr2 = new int[] { Item.getIdFromItem(getItem()), getItemDisplay().getMetadata() };
			}
			for (int j = 0; j < 8; ++j) {
				world.spawnParticle(EnumParticleTypes.ITEM_CRACK, posX, posY, posZ,
						rand.nextGaussian() * 0.15, rand.nextGaussian() * 0.2,
						rand.nextGaussian() * 0.15, intArr2);
			}
		}
		if (explosiveRadius > 0) {
			boolean terrainDamage = world.getGameRules().getBoolean("mobGriefing") && explosiveDamage;
			world.newExplosion(((getThrower() == null) ? this : getThrower()), posX, posY,
					posZ, explosiveRadius, effect == 1, terrainDamage);
			if (effect != 0) {
				AxisAlignedBB axisalignedbb = getEntityBoundingBox().grow((explosiveRadius * 2),
						(explosiveRadius * 2), (explosiveRadius * 2));
				List<EntityLivingBase> list = new ArrayList<>();
				try { list = world.getEntitiesWithinAABB(EntityLivingBase.class, axisalignedbb); }  catch (Exception ignored) { }
				for (EntityLivingBase entity : list) {
					if (effect != 1) {
						Potion p2 = PotionEffectType.getMCType(effect);
						if (p2 == null) {
							continue;
						}
						entity.addPotionEffect(new PotionEffect(p2, duration * 20, amplify));
					} else {
						entity.setFire(duration);
					}
				}
				world.playEvent(null, 2002, getPosition(), getPotionColor(effect));
			}
			setDead();
		}
		if (!world.isRemote && !isArrow() && !sticksToWalls()) { setDead(); }
	}

	public void onUpdate() {
		super.onEntityUpdate();
		if (++ticksExisted % 10 == 0) {
			EventHooks.onProjectileTick(this);
		}
		if (effect == 1 && !inGround) {
			setFire(1);
		}
		IBlockState state = world.getBlockState(tilePos);
		Block block = state.getBlock();
		if ((isArrow() || sticksToWalls()) && tilePos != BlockPos.ORIGIN) {
			AxisAlignedBB axisalignedbb = state.getCollisionBoundingBox(world, tilePos);
			if (axisalignedbb != null && axisalignedbb.contains(new Vec3d(posX, posY, posZ))) {
				inGround = true;
			}
		}
		if (arrowShake > 0) {
			--arrowShake;
		}
		if (inGround) {
			int j = block.getMetaFromState(state);
			if (block == inTile && j == inData) {
				++ticksInGround;
				if (ticksInGround == CustomNpcs.ProjectileLifespan) {
					setDead();
				}
			} else {
				inGround = false;
				motionX *= rand.nextFloat() * 0.2f;
				motionY *= rand.nextFloat() * 0.2f;
				motionZ *= rand.nextFloat() * 0.2f;
				ticksInGround = 0;
				ticksInAir = 0;
			}
		} else {
			++ticksInAir;
			if (ticksInAir == CustomNpcs.ProjectileLifespan) {
				setDead();
			}
			Vec3d vec3 = new Vec3d(posX, posY, posZ);
			Vec3d vec4 = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);
			RayTraceResult movingobjectposition = world.rayTraceBlocks(vec3, vec4, false, true, false);
			vec3 = new Vec3d(posX, posY, posZ);
			vec4 = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);
			if (movingobjectposition != null) {
				vec4 = new Vec3d(movingobjectposition.hitVec.x, movingobjectposition.hitVec.y,
						movingobjectposition.hitVec.z);
			}
			if (!world.isRemote) {
				Entity entity = null;
				List<Entity> list = new ArrayList<>();
				try {
					list = world.getEntitiesWithinAABBExcludingEntity(this,
							getEntityBoundingBox().grow(motionX, motionY, motionZ).grow(1.0, 1.0, 1.0));
				}
				catch (Exception ignored) { }
				double d0 = 0.0;
                for (Entity entity2 : list) {
                    if (entity2.canBeCollidedWith()
                            && (!entity2.isEntityEqual(thrower) || ticksInAir >= 25)) {
                        float f = 0.3f;
                        AxisAlignedBB axisAlignedBB = entity2.getEntityBoundingBox().grow(f, f, f);
                        RayTraceResult movingobjectposition2 = axisAlignedBB.calculateIntercept(vec3, vec4);
                        if (movingobjectposition2 != null) {
                            double d2 = vec3.distanceTo(movingobjectposition2.hitVec);
                            if (d2 < d0 || d0 == 0.0) {
                                entity = entity2;
                                d0 = d2;
                            }
                        }
                    }
                }
				if (entity != null) {
					movingobjectposition = new RayTraceResult(entity);
				}
				if (movingobjectposition != null && movingobjectposition.entityHit != null) {
					if (npc != null && movingobjectposition.entityHit instanceof EntityLivingBase
							&& npc.isOnSameTeam(movingobjectposition.entityHit)) {
						movingobjectposition = null;
					} else if (movingobjectposition.entityHit instanceof EntityPlayer) {
						EntityPlayer entityplayer = (EntityPlayer) movingobjectposition.entityHit;
						if (entityplayer.capabilities.disableDamage || (thrower instanceof EntityPlayer
								&& !((EntityPlayer) thrower).canAttackPlayer(entityplayer))) {
							movingobjectposition = null;
						}
					}
				}
			}
			if (movingobjectposition != null) {
				if (movingobjectposition.typeOfHit == RayTraceResult.Type.BLOCK
						&& world.getBlockState(movingobjectposition.getBlockPos()).getBlock() == Blocks.PORTAL) {
					setPortal(movingobjectposition.getBlockPos());
				} else {
					dataManager.set(EntityProjectile.Rotating, false);
					onImpact(movingobjectposition);
				}
			}
			posX += motionX;
			posY += motionY;
			posZ += motionZ;
			float f2 = MathHelper.sqrt(motionX * motionX + motionZ * motionZ);
			rotationYaw = (float) Math.atan2(motionX, motionZ) * 180.0f / 3.141592653589793f;
			rotationPitch = (float) Math.atan2(motionY, f2) * 180.0f / 3.141592653589793f;
			while (rotationPitch - prevRotationPitch < -180.0f) { prevRotationPitch -= 360.0f; }
			while (rotationPitch - prevRotationPitch >= 180.0f) { prevRotationPitch += 360.0f; }
			while (rotationYaw - prevRotationYaw < -180.0f) { prevRotationYaw -= 360.0f; }
			while (rotationYaw - prevRotationYaw >= 180.0f) { prevRotationYaw += 360.0f; }
			rotationPitch = prevRotationPitch + (rotationPitch - prevRotationPitch);
			rotationYaw = prevRotationYaw + (rotationYaw - prevRotationYaw);
			if (isRotating()) {
				int spin = isBlock() ? 10 : 20;
				rotationPitch -= ticksInAir % 15 * spin * getSpeed();
			}
			float f3 = getMotionFactor();
			float f4 = getGravityVelocity();
			if (isInWater()) {
				if (world.isRemote) {
					for (int i = 0; i < 4; ++i) {
						float f5 = 0.25f;
						world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, posX - motionX * f5, posY - motionY * f5, posZ - motionZ * f5, motionX, motionY, motionZ);
					}
				}
				f3 = 0.8f;
			}
			motionX *= f3;
			motionY *= f3;
			motionZ *= f3;
			if (hasGravity()) { motionY -= f4; }
			if (accelerate) {
				motionX += accelerationX;
				motionY += accelerationY;
				motionZ += accelerationZ;
			}
			if (world.isRemote && dataManager.get(EntityProjectile.Particle) > 0) {
				world.spawnParticle(Objects.requireNonNull(ParticleType.getMCType(dataManager.get(EntityProjectile.Particle))), posX, posY, posZ, 0.0, 0.0, 0.0);
			}
			setPosition(posX, posY, posZ);
			doBlockCollisions();
		}
	}

	public void readEntityFromNBT(@Nonnull NBTTagCompound compound) {
		tilePos = new BlockPos(compound.getShort("xTile"), compound.getShort("yTile"), compound.getShort("zTile"));
		inTile = Block.getBlockById(compound.getByte("inTile") & 0xFF);
		inData = (compound.getByte("inData") & 0xFF);
		throwableShake = (compound.getByte("shake") & 0xFF);
		inGround = (compound.getByte("inGround") == 1);
		dataManager.set(EntityProjectile.Arrow, compound.getBoolean("isArrow"));
		throwerName = compound.getString("ownerName");
		canBePickedUp = compound.getBoolean("canBePickedUp");
		damage = compound.getFloat("damagev2");
		punch = compound.getInteger("punch");
		explosiveRadius = compound.getInteger("explosiveRadius");
		duration = compound.getInteger("effectDuration");
		accelerate = compound.getBoolean("accelerate");
		effect = compound.getInteger("PotionEffect");
		accuracy = compound.getInteger("accuracy");
		dataManager.set(EntityProjectile.Particle, compound.getInteger("trailenum"));
		dataManager.set(EntityProjectile.Size, compound.getInteger("size"));
		dataManager.set(EntityProjectile.Glows, compound.getBoolean("glows"));
		dataManager.set(EntityProjectile.Velocity, compound.getInteger("velocity"));
		dataManager.set(EntityProjectile.Gravity, compound.getBoolean("gravity"));
		dataManager.set(EntityProjectile.Is3d, compound.getBoolean("Render3D"));
		dataManager.set(EntityProjectile.Rotating, compound.getBoolean("Spins"));
		dataManager.set(EntityProjectile.Sticks, compound.getBoolean("Sticks"));
		if (throwerName != null && throwerName.isEmpty()) {
			throwerName = null;
		}
		if (compound.hasKey("direction")) {
			NBTTagList nbttaglist = compound.getTagList("direction", 6);
			motionX = nbttaglist.getDoubleAt(0);
			motionY = nbttaglist.getDoubleAt(1);
			motionZ = nbttaglist.getDoubleAt(2);
		}
		NBTTagCompound var2 = compound.getCompoundTag("Item");
		ItemStack item = new ItemStack(var2);
		if (item.isEmpty()) { setDead(); }
		else { dataManager.set(EntityProjectile.ItemStackThrown, item); }
	}

	public void setHasGravity(boolean bo) { dataManager.set(EntityProjectile.Gravity, bo); }

	public void setIs3D(boolean bo) { dataManager.set(EntityProjectile.Is3d, bo); }

	public void setParticleEffect(int type) { dataManager.set(EntityProjectile.Particle, type); }

	@SideOnly(Side.CLIENT)
	public void setPositionAndRotationDirect(double par1, double par3, double par5, float par7, float par8, int par9, boolean bo) {
		if (world.isRemote && inGround) { return; }
		setPosition(par1, par3, par5);
		setRotation(par7, par8);
	}

	public void setRotating(boolean bo) { dataManager.set(EntityProjectile.Rotating, bo); }

	public void setSpeed(int speed) { dataManager.set(EntityProjectile.Velocity, speed); }

	public void setStickInWall(boolean bo) { dataManager.set(EntityProjectile.Sticks, bo); }

	public void setThrownItem(ItemStack item) { dataManager.set(EntityProjectile.ItemStackThrown, item); }

	public void shoot(double par1, double par3, double par5, float par7, float par8) {
		float f2 = MathHelper.sqrt(par1 * par1 + par3 * par3 + par5 * par5);
		float f3 = MathHelper.sqrt(par1 * par1 + par5 * par5);
		float yaw = (float) Math.atan2(par1, par5) * 180.0f / 3.141592653589793f;
		float pitch = hasGravity() ? par7 : (float) Math.atan2(par3, f3) * 180.0f / 3.141592653589793f;
        rotationYaw = yaw;
		prevRotationYaw = yaw;
        rotationPitch = pitch;
		prevRotationPitch = pitch;
		motionX = MathHelper.sin(yaw / 180.0f * 3.1415927f) * MathHelper.cos(pitch / 180.0f * 3.1415927f);
		motionZ = MathHelper.cos(yaw / 180.0f * 3.1415927f) * MathHelper.cos(pitch / 180.0f * 3.1415927f);
		motionY = MathHelper.sin((pitch + 1.0f) / 180.0f * 3.1415927f);
		motionX += rand.nextGaussian() * 0.007499999832361937 * par8;
		motionZ += rand.nextGaussian() * 0.007499999832361937 * par8;
		motionY += rand.nextGaussian() * 0.007499999832361937 * par8;
		motionX *= getSpeed();
		motionZ *= getSpeed();
		motionY *= getSpeed();
		accelerationX = par1 / f2 * 0.1;
		accelerationY = par3 / f2 * 0.1;
		accelerationZ = par5 / f2 * 0.1;
		ticksInGround = 0;
	}

	public boolean sticksToWalls() { return is3D() && dataManager.get(EntityProjectile.Sticks); }

	@Override
	public void writeEntityToNBT(@Nonnull NBTTagCompound par1NBTTagCompound) {
		par1NBTTagCompound.setShort("xTile", (short) tilePos.getX());
		par1NBTTagCompound.setShort("yTile", (short) tilePos.getY());
		par1NBTTagCompound.setShort("zTile", (short) tilePos.getZ());
		par1NBTTagCompound.setByte("inTile", (byte) Block.getIdFromBlock(inTile));
		par1NBTTagCompound.setByte("inData", (byte) inData);
		par1NBTTagCompound.setByte("shake", (byte) throwableShake);
		par1NBTTagCompound.setBoolean("inGround", inGround);
		par1NBTTagCompound.setBoolean("isArrow", isArrow());
		par1NBTTagCompound.setTag("direction", newDoubleNBTList(motionX, motionY, motionZ));
		par1NBTTagCompound.setBoolean("canBePickedUp", canBePickedUp);
		if ((throwerName == null || throwerName.isEmpty()) && thrower != null
				&& thrower instanceof EntityPlayer) {
			throwerName = thrower.getUniqueID().toString();
		}
		par1NBTTagCompound.setString("ownerName", (throwerName == null) ? "" : throwerName);
		par1NBTTagCompound.setTag("Item", getItemDisplay().writeToNBT(new NBTTagCompound()));
		par1NBTTagCompound.setFloat("damagev2", damage);
		par1NBTTagCompound.setInteger("punch", punch);
		par1NBTTagCompound.setInteger("size", dataManager.get(EntityProjectile.Size));
		par1NBTTagCompound.setInteger("velocity", dataManager.get(EntityProjectile.Velocity));
		par1NBTTagCompound.setInteger("explosiveRadius", explosiveRadius);
		par1NBTTagCompound.setInteger("effectDuration", duration);
		par1NBTTagCompound.setBoolean("gravity", hasGravity());
		par1NBTTagCompound.setBoolean("accelerate", accelerate);
		par1NBTTagCompound.setBoolean("glows", dataManager.get(EntityProjectile.Glows));
		par1NBTTagCompound.setInteger("PotionEffect", effect);
		par1NBTTagCompound.setInteger("trailenum", dataManager.get(EntityProjectile.Particle));
		par1NBTTagCompound.setBoolean("Render3D", dataManager.get(EntityProjectile.Is3d));
		par1NBTTagCompound.setBoolean("Spins", dataManager.get(EntityProjectile.Rotating));
		par1NBTTagCompound.setBoolean("Sticks", dataManager.get(EntityProjectile.Sticks));
		par1NBTTagCompound.setInteger("accuracy", accuracy);
	}

	@Override
	public void setDead() {
		super.setDead();
		scripts.clear();
	}

}
