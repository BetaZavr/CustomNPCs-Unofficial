package noppes.npcs.api.wrapper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.handler.capability.IWrapperEntityDataHandler;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.entity.EntityProjectile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WrapperEntityData implements IWrapperEntityDataHandler, ICapabilityProvider {

	@CapabilityInject(IWrapperEntityDataHandler.class)
	public static Capability<IWrapperEntityDataHandler> ENTITYDATA_CAPABILITY;
	private static final ResourceLocation CNPCS_CAPKEY = new ResourceLocation(CustomNpcs.MODID, "entitydata");
	private static final Set<Class<?>> WARNED = ConcurrentHashMap.newKeySet();

	public static IEntity<?> get(Entity entity) {
		if (entity == null || entity.world == null) { return null; }
		try {
			WrapperEntityData data = (WrapperEntityData) entity.getCapability(ENTITYDATA_CAPABILITY, null);
			if (data == null || data.base == null) {
				if (CustomNpcs.EnableScripting && WARNED.add(entity.getClass())) { LogWriter.warn("Unable to get EntityData for " + entity); }
				data = getData(entity);
			}
			return data.base;
		}
		catch (Exception e) { LogWriter.error(e); }
		return null;
	}

	public static WrapperEntityData getData(Entity entity) {
		if (entity == null) {
			return null;
		}
		if (entity instanceof EntityPlayer) {
			return new WrapperEntityData(new PlayerWrapper<>((EntityPlayer) entity));
		}
		if (PixelmonHelper.isPixelmon(entity)) {
			return new WrapperEntityData(new PixelmonWrapper<>((EntityTameable) entity));
		}
		if (entity instanceof EntityVillager) {
			return new WrapperEntityData(new VillagerWrapper<>((EntityVillager) entity));
		}
		if (entity instanceof EntityAnimal) {
			return new WrapperEntityData(new AnimalWrapper<>((EntityAnimal) entity));
		}
		if (entity instanceof EntityMob) {
			return new WrapperEntityData(new MonsterWrapper<>((EntityMob) entity));
		}
		if (entity instanceof EntityLiving) {
			return new WrapperEntityData(new EntityLivingWrapper<>((EntityLiving) entity));
		}
		if (entity instanceof EntityLivingBase) {
			return new WrapperEntityData(new EntityLivingBaseWrapper<>((EntityLivingBase) entity));
		}
		if (entity instanceof EntityItem) {
			return new WrapperEntityData(new EntityItemWrapper<>((EntityItem) entity));
		}
		if (entity instanceof EntityProjectile) {
			return new WrapperEntityData(new ProjectileWrapper<>((EntityProjectile) entity));
		}
		if (entity instanceof EntityThrowable) {
			return new WrapperEntityData(new ThrowableWrapper<>((EntityThrowable) entity));
		}
		if (entity instanceof EntityArrow) {
			return new WrapperEntityData(new ArrowWrapper<>((EntityArrow) entity));
		}
		return new WrapperEntityData(new EntityWrapper<>(entity));
	}

	public static void register(AttachCapabilitiesEvent<Entity> event) {
		if (CustomNpcs.EnableScripting || event.getObject() instanceof EntityPlayer) { event.addCapability(CNPCS_CAPKEY, getData(event.getObject())); }
	}

	public IEntity<?> base;

	public WrapperEntityData() {
	}

	public WrapperEntityData(IEntity<?> base) {
		this.base = base;
	}

	@SuppressWarnings("unchecked")
	public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing facing) {
		if (hasCapability(capability, facing)) {return (T) this;}
		return null;
	}

	@Override
	public NBTTagCompound getNBT() {
		return new NBTTagCompound();
	}

	public boolean hasCapability(@Nullable Capability<?> capability, EnumFacing facing) {
		return capability != null && capability == ENTITYDATA_CAPABILITY;
	}

	@Override
	public void setNBT(NBTTagCompound compound) {
	}

}
