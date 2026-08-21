package noppes.npcs.api.wrapper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IEntityLiving;
import noppes.npcs.api.entity.data.IMark;
import noppes.npcs.api.entity.data.INpcAttribute;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.data.AttributeWrapper;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.mixin.world.entity.ai.attributes.IAttributeMapMixin;
import noppes.npcs.mixin.world.entity.ai.attributes.IAttributeSupplierMixin;
import noppes.npcs.util.ValueUtil;

import java.util.*;

public class EntityLivingBaseWrapper<T extends LivingEntity>
        extends EntityWrapper<T>
        implements IEntityLiving<T> {

   public EntityLivingBaseWrapper(T entity) { super(entity); }

   @Override
   public float getHealth() { return entity.getHealth(); }

   @Override
   public void setHealth(float health) { entity.setHealth(health); }

   @Override
   public float getMaxHealth() { return entity.getMaxHealth(); }

   @Override
   public void setMaxHealth(float health) {
      if (!(health < 0.0F)) {
         Objects.requireNonNull(entity.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(health);
      }
   }

   @Override
   public boolean isAttacking() { return entity.getLastHurtByMob() != null; }

   @Override
   public void setAttackTarget(IEntityLiving<T> living) {
      if (living == null) { entity.setLastHurtByMob(null); }
      else { entity.setLastHurtByMob(living.getMCEntity()); }
   }

   @Override
   @SuppressWarnings("unchecked")
   public IEntityLiving<T> getAttackTarget() {
      return (IEntityLiving<T>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity.getLastHurtByMob());
   }

   @Override
   @SuppressWarnings("unchecked")
   public IEntityLiving<T> getLastAttacked() {
      return (IEntityLiving<T>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(entity.getLastHurtMob());
   }

   @Override
   public int getLastAttackedTime() { return entity.getLastHurtMobTimestamp(); }

   @Override
   public boolean canSeeEntity(IEntity<?> iEntity) { return entity.hasLineOfSight(iEntity.getMCEntity()); }

   @Override
   public void swingMainhand() { entity.swing(InteractionHand.MAIN_HAND); }

   @Override
   public void swingOffhand() { entity.swing(InteractionHand.OFF_HAND); }

   @SuppressWarnings("unused")
   public void addPotionEffect(String effect, int duration, int strength, boolean hideParticles) {
      addPotionEffect(ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effect)), duration, strength, hideParticles);
   }

   @Override
   public void addPotionEffect(int effect, int duration, int strength, boolean hideParticles) {
      addPotionEffect(MobEffect.byId(effect), duration, strength, hideParticles);
   }

   public void addPotionEffect(MobEffect p, int duration, int strength, boolean hideParticles) {
      if (p != null) {
         if (!p.isInstantenous()) { duration *= 20; }
         strength = ValueUtil.correctInt(strength, 0, 255);
         duration = ValueUtil.correctInt(duration, 0, 1000000);
         if (duration == 0) { entity.removeEffect(p); }
         else { entity.addEffect(new MobEffectInstance(p, duration, strength, false, hideParticles)); }
      }
   }

   @Override
   public void clearPotionEffects() {
      entity.removeAllEffects();
   }

   @Override
   public int getPotionEffect(int effect) {
      MobEffectInstance pf = entity.getEffect(Objects.requireNonNull(MobEffect.byId(effect)));
      return pf == null ? -1 : pf.getAmplifier();
   }

   @Override
   public IItemStack getMainhandItem() {
      return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(entity.getMainHandItem());
   }

   @Override
   public void setMainhandItem(IItemStack item) {
      entity.setItemInHand(InteractionHand.MAIN_HAND, item == null ? ItemStack.EMPTY : item.getMCItemStack());
   }

   @Override
   public IItemStack getOffhandItem() {
      return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(entity.getOffhandItem());
   }

   @Override
   public void setOffhandItem(IItemStack item) {
      entity.setItemInHand(InteractionHand.OFF_HAND, item == null ? ItemStack.EMPTY : item.getMCItemStack());
   }

   @Override
   public IItemStack getArmor(int slot) {
      if (slot < 0 || slot > 3) { throw new CustomNPCsException("Wrong slot id:" + slot); }
      EquipmentSlot s = getSlot(slot);
      if (s == null) { return ItemStackWrapper.AIR;}
      return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(entity.getItemBySlot(s));
   }

   @Override
   public void setArmor(int slot, IItemStack item) {
      if (slot < 0 || slot > 3) { throw new CustomNPCsException("Wrong slot id:" + slot); }
      EquipmentSlot s = getSlot(slot);
      if (s != null) { entity.setItemSlot(s, item == null ? ItemStack.EMPTY : item.getMCItemStack()); }
   }

   private EquipmentSlot getSlot(int slot) {
      return switch (slot) {
         case 1 -> EquipmentSlot.LEGS;
         case 2 -> EquipmentSlot.CHEST;
         case 3 -> EquipmentSlot.HEAD;
         case 0 -> EquipmentSlot.FEET;
         default -> null;
      };
   }

   @Override
   public float getRotation() { return entity.yBodyRot; }

   @Override
   public void setRotation(float rotation) { entity.yBodyRot = rotation; }

   @Override
   public int getType() { return 5; }

   @Override
   public boolean typeOf(int type) { return type == 5 || super.typeOf(type); }

   @Override
   public boolean isChild() { return entity.isBaby(); }

   @Override
   public IMark addMark(int type) {
      MarkData data = MarkData.get(entity);
      return data.addMark(type);
   }

   @Override
   public void removeMark(IMark mark) {
      MarkData data = MarkData.get(entity);
      data.marks.remove((MarkData.Mark) mark);
      data.syncClients();
   }

   @Override
   public IMark[] getMarks() {
      MarkData data = MarkData.get(entity);
      return data.marks.toArray(new IMark[0]);
   }

   @Override
   public float getMoveForward() { return entity.zza; }

   @Override
   public void setMoveForward(float move) { entity.zza = move; }

   @Override
   public float getMoveStrafing() { return entity.xxa; }

   @Override
   public void setMoveStrafing(float move) { entity.xxa = move; }

   @Override
   public float getMoveVertical() { return entity.yya; }

   @Override
   public void setMoveVertical(float move) { entity.yya = move; }

   // New from Unofficial (BetaZavr)
   @Override
   public INpcAttribute addAttribute(INpcAttribute attribute) {
      if (attribute == null || hasAttribute(attribute)) { return null; }
      Attribute baseAttribute = attribute.getMCBaseAttribute();
      if (baseAttribute == null) { return null; }
      AttributeInstance attr = attribute.getMCAttribute();
      // register
      Map<Attribute, AttributeInstance> attributes = ((IAttributeMapMixin) entity.getAttributes()).getAttributes();
      Set<AttributeInstance> dirtyAttributes = ((IAttributeMapMixin) entity.getAttributes()).getDirtyAttributes();
      AttributeSupplier supplier = ((IAttributeMapMixin) entity.getAttributes()).getSupplier();
      if (attr.getAttribute().isClientSyncable()) {
         for (AttributeInstance a : dirtyAttributes) {
            if (a.getAttribute().getDescriptionId().equals(attr.getAttribute().getDescriptionId())) {
               dirtyAttributes.remove(attr);
               break;
            }
         }
         dirtyAttributes.add(attr);
      }
      Attribute key = attr.getAttribute();
      for (Map.Entry<Attribute, AttributeInstance> entry : attributes.entrySet()) {
         if (entry.getKey().getDescriptionId().equals(attr.getAttribute().getDescriptionId())) {
            key = entry.getKey();
            break;
         }
      }
      attributes.put(key, attr);
      Map<Attribute, AttributeInstance> instances = ((IAttributeSupplierMixin) supplier).getInstances();
      instances.put(key, attr);
      return attribute;
   }

   @Override
   public INpcAttribute addAttribute(String attributeName, double baseValue, double minValue, double maxValue) {
      if (attributeName == null || attributeName.isEmpty() || hasAttribute(attributeName)) { return null; }
      return addAttribute(new AttributeWrapper(this.entity, attributeName, baseValue, minValue, maxValue));
   }

   @Override
   public boolean hasAttribute(INpcAttribute attribute) {
      return entity.getAttributes().hasAttribute(attribute.getMCBaseAttribute());
   }

   @Override
   public boolean hasAttribute(String attributeName) {
      for (AttributeInstance attr : this.entity.getAttributes().getDirtyAttributes()) {
         if (attr.getAttribute().getDescriptionId().equals(attributeName)) { return true; }
      }
      return false;
   }

   @Override
   public INpcAttribute getIAttribute(String attributeName) {
      for (AttributeInstance attr : this.entity.getAttributes().getDirtyAttributes()) {
         if (attr.getAttribute().getDescriptionId().equals(attributeName)) {
            return Objects.requireNonNull(NpcAPI.Instance()).getIAttribute(attr);
         }
      }
      return null;
   }

   @Override
   public String[] getIAttributeNames() {
      List<String> list = new ArrayList<>();
      for (AttributeInstance attr : this.entity.getAttributes().getDirtyAttributes()) {
         list.add(attr.getAttribute().getDescriptionId());
      }
      return list.toArray(new String[0]);
   }

   @Override
   public INpcAttribute[] getIAttributes() {
      List<INpcAttribute> list = new ArrayList<>();
      for (AttributeInstance attr : this.entity.getAttributes().getDirtyAttributes()) {
         list.add(Objects.requireNonNull(NpcAPI.Instance()).getIAttribute(attr));
      }
      return list.toArray(new INpcAttribute[0]);
   }

   @Override
   public boolean removeAttribute(INpcAttribute attribute) {
      if (attribute == null || !attribute.isCustom() || !this.hasAttribute(attribute)) { return false; }
      AttributeInstance attr = attribute.getMCAttribute();
      // remove
      Map<Attribute, AttributeInstance> attributes = ((IAttributeMapMixin) entity.getAttributes()).getAttributes();
      Set<AttributeInstance> dirtyAttributes = ((IAttributeMapMixin) entity.getAttributes()).getDirtyAttributes();
      AttributeSupplier supplier = ((IAttributeMapMixin) entity.getAttributes()).getSupplier();
      if (attr.getAttribute().isClientSyncable()) {
         for (AttributeInstance a : dirtyAttributes) {
            if (a.getAttribute().getDescriptionId().equals(attr.getAttribute().getDescriptionId())) {
               dirtyAttributes.remove(attr);
               break;
            }
         }
      }
      Attribute key = attr.getAttribute();
      for (Map.Entry<Attribute, AttributeInstance> entry : attributes.entrySet()) {
         if (entry.getKey().getDescriptionId().equals(attr.getAttribute().getDescriptionId())) {
            key = entry.getKey();
            break;
         }
      }
      attributes.remove(key);
      Map<Attribute, AttributeInstance> instances = ((IAttributeSupplierMixin) supplier).getInstances();
      instances.remove(key);
      return true;
   }

   @Override
   public boolean removeAttribute(String attributeName) {
      return this.removeAttribute(getIAttribute(attributeName));
   }

}
