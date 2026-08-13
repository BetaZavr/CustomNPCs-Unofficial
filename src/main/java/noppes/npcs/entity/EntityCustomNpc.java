package noppes.npcs.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.client.parts.ModelPartData;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.model.part.ModelData;
import noppes.npcs.constants.EnumParts;

import javax.annotation.Nonnull;

public class EntityCustomNpc extends EntityNPCFlying {

	public ModelData modelData = new ModelData(this);

	public EntityCustomNpc(World world) {
		super(world);
		if (!CustomNpcs.EnableDefaultEyes) { modelData.eyes.type = -1; }
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (!isServerWorld()) {
			ModelPartData particles = modelData.getPartData(EnumParts.PARTICLES);
			if (particles != null && !this.isKilled()) {
				CustomNpcs.proxy.spawnParticle(this, "ModelData", modelData, particles);
			}
			EntityLivingBase entity = this.modelData.getEntity(this);
			if (entity != null) {
				try {
					entity.onUpdate();
				} catch (Exception e) { LogWriter.error(e); }
				EntityUtil.Copy(this, entity);
			}
		}
		this.modelData.eyes.update(this);
	}

	@Override
	public void readEntityFromNBT(@Nonnull NBTTagCompound compound) {
		if (compound.hasKey("NpcModelData")) {
			this.modelData.load(compound.getCompoundTag("NpcModelData"));
		}
		super.readEntityFromNBT(compound);
	}

	public boolean startRiding(@Nonnull Entity par1Entity, boolean force) {
		boolean b = super.startRiding(par1Entity, force);
		this.updateHitbox();
		return b;
	}

	@Override
	public void updateHitbox() {
		Entity entity = modelData.getEntity(this);
		if (modelData == null || entity == null) {
			if (modelData != null) {
				baseWidth = 0.6f - modelData.getBodyX() +
						(Math.max(modelData.getPartConfig(EnumParts.HEAD).scaleX,
								modelData.getPartConfig(EnumParts.HEAD).scaleZ) - 1.0f) / 2.0f;
				baseHeight = 1.9f - modelData.getBodyY() + (modelData.getPartConfig(EnumParts.HEAD).scaleY - 1.0f) / 2.0f;
				baseEyeHeight = baseHeight * (1.615f / 1.9f);
			}
			super.updateHitbox();
		} else {
			if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).updateHitbox(); }
			width = Math.max(0.1f, entity.width / 5.0f * display.getSize());
			height = Math.max(0.1f, entity.height / 5.0f * display.getSize());
			eyeHeight = Math.max(0.1f, entity.getEyeHeight() / 5.0f * display.getSize());
			double n = width / 2.0f;
			if (n > World.MAX_ENTITY_RADIUS) { World.MAX_ENTITY_RADIUS = width / 2.0f; }
			setPosition(posX, posY, posZ);
		}
	}

	@Override
	public void writeEntityToNBT(@Nonnull NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		compound.setTag("NpcModelData", this.modelData.save());
	}

	public boolean writeToNBTAtomically(@Nonnull NBTTagCompound compound) {
		boolean bo = super.writeToNBTAtomically(compound);
		if (bo) {
			String s = this.getEntityString();
            assert s != null;
            if (s.equals("minecraft:customnpcs.customnpc") || s.equals("minecraft:customnpcs:customnpc")) {
				compound.setString("id", CustomNpcs.MODID + ":customnpc");
			}
		}
		return bo;
	}

	public boolean writeToNBTOptional(@Nonnull NBTTagCompound compound) {
		boolean bo = super.writeToNBTAtomically(compound);
		if (bo) {
			String s = this.getEntityString();
            assert s != null;
            if (s.equals("minecraft:customnpcs.customnpc") || s.equals("minecraft:customnpcs:customnpc")) {
				compound.setString("id", CustomNpcs.MODID + ":customnpc");
			}
		}
		return bo;
	}

}
