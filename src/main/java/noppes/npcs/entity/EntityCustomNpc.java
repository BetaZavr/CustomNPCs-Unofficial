package noppes.npcs.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.parts.ModelData;
import noppes.npcs.client.parts.ModelEyeData;
import noppes.npcs.client.parts.MpmPartData;
import noppes.npcs.constants.EnumParts;

import javax.annotation.Nonnull;

public class EntityCustomNpc extends EntityNPCFlying {

   public ModelData modelData = new ModelData(this);

   public EntityCustomNpc(EntityType<? extends PathfinderMob> type, Level world) {
      super(type, world);
   }

   @Override
   public void readAdditionalSaveData(@Nonnull CompoundTag compound) {
      if (compound.contains("NpcModelData")) {
         modelData.load(compound.getCompound("NpcModelData"));
      }
      super.readAdditionalSaveData(compound);
   }

   @Override
   public void addAdditionalSaveData(@Nonnull CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.put("NpcModelData", modelData.save());
   }

   @Override
   public boolean saveAsPassenger(@Nonnull CompoundTag compound) {
      boolean bo = super.saveAsPassenger(compound);
      if (bo) {
         String s = getEncodeId();
         if (s != null && s.equals("minecraft:" + CustomNpcs.MODID + ".customnpc")) {
            compound.putString("id", CustomNpcs.MODID + ":customnpc");
         }
      }
      return bo;
   }

   @Override
   public void tick() {
      super.tick();
      if (isClientSide()) {
         LivingEntity entity = modelData.getEntity(this);
         if (entity != null) {
            try {
               entity.tick();
            } catch (Exception ignored) {
            }
            EntityUtil.Copy(this, entity);
         }
      }
      for (MpmPartData pd : modelData.mpmParts) {
         if (pd instanceof ModelEyeData med) { med.update(this); }
      }
   }

   @Override
   public boolean startRiding(@Nonnull Entity entityIn, boolean force) {
      boolean b = super.startRiding(entityIn, force);
      refreshDimensions();
      return b;
   }

   @Override
   public void refreshDimensions() {
      Entity entity = modelData.getEntity(this);
      if (entity != null) {
         entity.refreshDimensions();
      }
      super.refreshDimensions();
   }

   @Override
   public @Nonnull EntityDimensions getDimensions(@Nonnull Pose pos) {
      if (modelData == null) {
         return new EntityDimensions(0.6F, 1.8F, false);
      }
      else {
         Entity entity = modelData.getEntity(this);
         if (entity == null) {
            float height = 1.9F - modelData.getBodyY() + (modelData.getPartConfig(EnumParts.HEAD).scaleY - 1.0F) / 2.0F;
            if (baseSize.height != height) {
               baseSize = new EntityDimensions(baseSize.width, height, false);
            }
            return super.getDimensions(pos);
         } else {
            EntityDimensions size = entity.getDimensions(pos);
            if (entity instanceof EntityNPCInterface) {
               return size.scale(display.getSize() * 0.2F);
            } else {
               float width = size.width / 5.0F * display.getSize();
               float height = size.height / 5.0F * display.getSize();
               if (width < 0.1F) {
                  width = 0.1F;
               }
               if (height < 0.1F) {
                  height = 0.1F;
               }
               if (display.getHitboxState() == 1 || isKilled() && stats.hideKilledBody) {
                  width = 1.0E-5F;
               }
               if ((double)(width / 2.0F) > level().getMaxEntityRadius()) {
                  level().increaseMaxEntityRadius(width / 2.0D);
               }
               return new EntityDimensions(width, height, false);
            }
         }
      }
   }

   @Override
   public double getPassengersRidingOffset() {
      Entity entity = modelData.getEntity(this);
      return entity != null ? entity.getPassengersRidingOffset() / 5.0D * (double)display.getSize() : super.getPassengersRidingOffset();
   }

   // New Unofficial (Goodbird)
   @Override
   protected void pushEntities() {
      if (display.getHitboxState() == 0) {
         if (level().isClientSide() && CustomNpcs.EnableInvisibleNpcs && CustomNpcs.InvisibilityAlgorithm == 2) {
            Player player = CustomNpcs.proxy.getPlayer();
            if (!display.isVisibleTo(player) && player != null &&
                    !player.isSpectator() && player.getMainHandItem().getItem() != CustomItems.wand) { return; }
         }
         super.pushEntities();
      }
   }

}
