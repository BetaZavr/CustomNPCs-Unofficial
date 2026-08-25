package noppes.npcs.client.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.CustomNpcs;
import noppes.npcs.constants.BodyPart;
import noppes.npcs.constants.EnumParts;

public abstract class ModelDataShared {

   public ModelPartConfig arm1 = new ModelPartConfig();
   public ModelPartConfig arm2 = new ModelPartConfig();
   public ModelPartConfig body = new ModelPartConfig();
   public ModelPartConfig leg1 = new ModelPartConfig();
   public ModelPartConfig leg2 = new ModelPartConfig();
   public ModelPartConfig head = new ModelPartConfig();

   protected ResourceLocation entityName = null;
   public LivingEntity entity;
   public CompoundTag extra = new CompoundTag();
   public ListTag oldPartData = new ListTag();
   public List<MpmPartData> mpmParts = new ArrayList<>();
   public List<BodyPart> hiddenParts = new ArrayList<>();
   public int wingMode = 0;
   public String url = "";
   public String displayName = "";
   public long lastEdited = System.currentTimeMillis();
   public int inLove = 0;
   public int animationTime = -1;
   public int modelType = 0;
   public int moveAnimation = 16;
   public boolean startMoveAnimation = false;
   public int animation = 0;
   public boolean startAnimation = false;
   public int animationStart = 0;

   public CompoundTag save() {
      CompoundTag compound = new CompoundTag();
      if (entityName != null) { compound.putString("EntityName", entityName.toString()); }
      compound.put("ArmsConfig", arm1.save());
      compound.put("Arms2Config", arm2.save());
      compound.put("BodyConfig", body.save());
      compound.put("LegsConfig", leg1.save());
      compound.put("Legs2Config", leg2.save());
      compound.put("HeadConfig", head.save());
      compound.put("ExtraData", extra);
      compound.putInt("WingMode", wingMode);
      compound.putString("CustomSkinUrl", url);
      compound.putString("DisplayName", displayName);
      compound.putInt("Animation", animation);
      compound.putInt("MoveAnimation", moveAnimation);
      compound.putInt("ModelType", modelType);
      compound.putLong("LastEdited", lastEdited);
      compound.put("Parts", oldPartData);
      ListTag list = new ListTag();
      for (MpmPartData e : mpmParts) { list.add(e.getNbt()); }
      compound.put("NewParts", list);
      return compound;
   }

   public void load(CompoundTag compound) {
      String rl = compound.getString("EntityName");
      setEntity(rl.isEmpty() ? null : ResourceLocation.tryParse(rl));
      arm1.load(compound.getCompound("ArmsConfig"));
      arm2.load(compound.getCompound("Arms2Config"));
      body.load(compound.getCompound("BodyConfig"));
      leg1.load(compound.getCompound("LegsConfig"));
      leg2.load(compound.getCompound("Legs2Config"));
      head.load(compound.getCompound("HeadConfig"));
      extra = compound.getCompound("ExtraData");
      wingMode = compound.getInt("WingMode");
      url = compound.getString("CustomSkinUrl");
      displayName = compound.getString("DisplayName");
      animation = compound.getInt("Animation");
      moveAnimation = compound.getInt("MoveAnimation");
      modelType = compound.getInt("ModelType");
      lastEdited = compound.getLong("LastEdited");
      mpmParts.clear();
      ListTag list = compound.getList("NewParts", 10);

      int i;
      for(i = 0; i < list.size(); ++i) {
         MpmPartData part = new MpmPartData();
         part.setNbt(list.getCompound(i));
         if (part.partId.equals(ModelEyeData.RESOURCE) ||
                 part.partId.equals(ModelEyeData.RESOURCE_RIGHT) ||
                 part.partId.equals(ModelEyeData.RESOURCE_LEFT)) {
            part = new ModelEyeData();
            part.setNbt(list.getCompound(i));
         }
         mpmParts.add(part);
      }
      oldPartData = compound.getList("Parts", 10);
      if (mpmParts.isEmpty()) {
         for(i = 0; i < list.size(); ++i) {
            mpmParts.add(EnumParts.convertOldPart(list.getCompound(i)));
         }
      }

      refreshParts();
      updateTranslate();
   }

   public void setMoveAnimation(int ani) {
      startMoveAnimation = moveAnimation != ani;
      moveAnimation = ani;
   }

   public int getMoveAnimation(LivingEntity player) {
      if (player.isPassenger()) {
         return 1;
      } else if (player.isSleeping()) {
         return 2;
      } else {
         return moveAnimation == 16 && player.isCrouching() ? 4 : moveAnimation;
      }
   }

   public boolean isMovementAnimation(int ani) {
      return ani == 2 || ani == 7 || ani == 4 || ani == 1 || ani == 14 || ani == 15 || ani == 16 || ani == 18 || ani == 17;
   }

   public void setAnimation(int ani) {
      if (isMovementAnimation(ani)) {
         setMoveAnimation(ani);
      } else {
         animationTime = -1;
         animation = ani;
         lastEdited = System.currentTimeMillis();
         startAnimation = animation != ani;
         if (animation == 10) {
            animationTime = 80;
         }

         if (animation == 13 || animation == 12) {
            animationTime = 60;
         }

         if (getOwner() != null && ani != 0) {
            animationStart = getOwner().tickCount;
         } else {
            animationStart = -1;
         }

      }
   }

   public void updateTranslate() {
      EnumParts[] var1 = EnumParts.values();
      for (EnumParts part : var1) {
         ModelPartConfig config = getPartConfig(part);
         if (config != null) {
            if (part == EnumParts.HEAD) {
               config.setTranslate(0.0F, getBodyY(), 0.0F);
            } else {
               ModelPartConfig leg;
               float x;
               float y;
               if (part == EnumParts.ARM_LEFT) {
                  leg = getPartConfig(EnumParts.BODY);
                  x = (1.0F - leg.scaleX) * 0.25F + (1.0F - config.scaleX) * 0.0625F;
                  y = getBodyY() + (1.0F - config.scaleY) * -0.125F;
                  config.setTranslate(-x, y, 0.0F);
                  if (!config.notShared) {
                     ModelPartConfig arm = getPartConfig(EnumParts.ARM_RIGHT);
                     arm.copyValues(config);
                  }
               } else if (part == EnumParts.ARM_RIGHT) {
                  leg = getPartConfig(EnumParts.BODY);
                  x = (1.0F - leg.scaleX) * 0.25F + (1.0F - config.scaleX) * 0.0625F;
                  y = getBodyY() + (1.0F - config.scaleY) * -0.125F;
                  config.setTranslate(x, y, 0.0F);
               } else if (part == EnumParts.LEG_LEFT) {
                  config.setTranslate(-(1.0F - config.scaleX) * 0.118F, getLegsY(), -(1.0F - config.scaleZ) * 0.00625F);
                  if (!config.notShared) {
                     leg = getPartConfig(EnumParts.LEG_RIGHT);
                     leg.copyValues(config);
                  }
               } else if (part == EnumParts.LEG_RIGHT) {
                  config.setTranslate((1.0F - config.scaleX) * 0.118F, getLegsY(), -(1.0F - config.scaleZ) * 0.00625F);
               } else if (part == EnumParts.BODY) {
                  config.setTranslate(0.0F, getBodyY(), 0.0F);
               }
            }
         }
      }
   }

   public void setEntity(ResourceLocation entityNameIn) {
      if (new ResourceLocation(CustomNpcs.MODID, "customnpc").equals(entityNameIn)) { entityNameIn = null; }
      entityName = entityNameIn;
      clearEntity();
      extra = new CompoundTag();
   }

   public ResourceLocation getEntityName() {
      return entityName;
   }

   public boolean hasEntity() {
      return entityName != null;
   }

   public float offsetY() {
      return entity == null ? -getBodyY() : entity.getBbHeight() - 1.8F;
   }

   public void clearEntity() {
      entity = null;
   }

   public ModelPartConfig getPartConfig(EnumParts type) {
      if (type == EnumParts.BODY) {
         return body;
      } else if (type == EnumParts.ARM_LEFT) {
         return arm1;
      } else if (type == EnumParts.ARM_RIGHT) {
         return arm2;
      } else if (type == EnumParts.LEG_LEFT) {
         return leg1;
      } else {
         return type == EnumParts.LEG_RIGHT ? leg2 : head;
      }
   }

   public abstract LivingEntity getOwner();

   public float getBodyY() {
      return entity != null ? entity.getBbHeight() : (1.0F - body.scaleY) * 0.75F + getLegsY();
   }

   public float getLegsY() {
      ModelPartConfig legs = leg1;
      if (leg1.notShared && leg2.scaleY > leg1.scaleY) { legs = leg2; }
      return (1.0F - legs.scaleY) * 0.75F;
   }

   public void refreshParts() {
      hiddenParts = mpmParts.stream().flatMap((part) -> {
         MpmPart p = part.getPart();
         return p != null ? p.hiddenParts.stream() : Stream.empty();
      }).distinct().collect(Collectors.toList());
   }
}
