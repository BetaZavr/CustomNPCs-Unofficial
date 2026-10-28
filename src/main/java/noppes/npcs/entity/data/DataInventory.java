package noppes.npcs.entity.data;

import java.util.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.ForgeHooks;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.ICustomDrop;
import noppes.npcs.api.entity.data.INPCInventory;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.handler.data.IDropSetData;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.controllers.DropController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataInventory extends SimpleContainer implements INPCInventory, IDropSetData {

   private int minExp = 0;
   private int maxExp = 0;
   public final Map<Integer, IItemStack> weapons = new TreeMap<>();
   public final Map<Integer, IItemStack> armor = new TreeMap<>();
   public final EntityNPCInterface npc;

   // New from Unofficial (BetaZavr)
   public final Map<Integer, DropSet> drops = new TreeMap<>();
   public SimpleContainer deadLoot; // <- PlayerInteractEvent.RightClickBlock
   public Map<LivingEntity, SimpleContainer> deadLoots;
   public String saveDropsName = "";
   public int dropType = 0; // 0-npc drops, 1-template drops, 2-both
   public int limitation = 0;
   public boolean lootMode = true;

   public DataInventory(EntityNPCInterface npcIn) {
      super();
      npc = npcIn;
   }

   public CompoundTag save(CompoundTag compound) {
      compound.putInt("MinExp", minExp);
      compound.putInt("MaxExp", maxExp);
      compound.put("Armor", NBTTags.nbtIItemStackMap(armor));
      compound.put("Weapons", NBTTags.nbtIItemStackMap(weapons));
      // New from Unofficial (BetaZavr)
      compound.putBoolean("LootMode", lootMode);
      compound.putString("SaveDropsName", saveDropsName);
      compound.putInt("Limitation", limitation);
      compound.putInt("DropType", dropType);
      ListTag dropList = new ListTag();
      int s = 0;
      for (int slot : drops.keySet()) {
         if (drops.get(slot) == null) { continue; }
         if (drops.get(slot).pos != s) { drops.get(slot).pos = s; }
         dropList.add(drops.get(slot).save());
         s++;
      }
      compound.put("NpcInv", dropList);
      return compound;
   }

   public void load(CompoundTag compound) {
      minExp = compound.getInt("MinExp");
      maxExp = compound.getInt("MaxExp");
      drops.clear();
      armor.clear();
      armor.putAll(NBTTags.getIItemStackMap(compound.getList("Armor", 10)));
      weapons.clear();
      weapons.putAll(NBTTags.getIItemStackMap(compound.getList("Weapons", 10)));

      // New from Unofficial (BetaZavr)
      if (compound.contains("LootMode", 3)) { lootMode = compound.getInt("LootMode") != 0; }
      else { lootMode = compound.getBoolean("LootMode"); }
      saveDropsName = compound.getString("SaveDropsName");
      limitation = compound.getInt("Limitation");
      dropType = compound.getInt("DropType");
      if (dropType < 0) { dropType *= -1; }
      if (dropType > 2) { dropType %= 3; }
      if (compound.contains("DropChance", 9)) { // if old items
         Map<Integer, IItemStack> d_old = NBTTags.getIItemStackMap(compound.getList("NpcInv", 10));
         Map<Integer, Integer> dc_old = NBTTags.getIntegerIntegerMap(compound.getList("DropChance", 10));
         int i = 0;
         for (int slot : d_old.keySet()) {
            if (dc_old.get(slot) <= 0) { continue; }
            DropSet ds = new DropSet(this);
            ds.item = d_old.get(slot).getMCItemStack();
            ds.chance = (double) dc_old.get(slot);
            ds.amount = new int[] { ds.item.getCount(), ds.item.getCount() };
            ds.pos = i;
            drops.put(i, ds);
            i++;
         }
      } else { // create data
         for (int i = 0; i < compound.getList("NpcInv", 10).size(); i++) {
            DropSet ds = new DropSet(this);
            ds.load(compound.getList("NpcInv", 10).getCompound(i));
            ds.pos = i;
            drops.put(ds.pos, ds);
         }
      }
   }

   @Override
   public @Nullable IItemStack getArmor(int slot) {
      return armor.get(slot);
   }

   @Override
   public void setArmor(int slot, IItemStack item) {
      armor.put(slot, item);
      if (npc != null) { npc.updateClient = true; }
   }

   @Override
   public @Nullable IItemStack getRightHand() {
      return weapons.get(0);
   }

   @Override
   public void setRightHand(IItemStack item) {
      weapons.put(0, item);
      if (npc != null) { npc.updateClient = true; }
   }

   @Override
   public @Nullable IItemStack getProjectile() {
      return weapons.get(1);
   }

   @Override
   public void setProjectile(IItemStack item) {
      weapons.put(1, item);
      if (npc != null) { npc.updateClient = true; }
   }

   @Override
   public @Nullable IItemStack getLeftHand() {
      return weapons.get(2);
   }

   @Override
   public void setLeftHand(IItemStack item) {
      weapons.put(2, item);
      if (npc != null) { npc.updateClient = true; }
   }

   @Override
   public @Nullable IItemStack getDropItem(int slot) {
      if (slot < 0 || slot >= drops.size()) {
         throw new CustomNPCsException("Bad slot number: " + slot + " in " + drops.size() + " maximum");
      }
      DropSet g = drops.get(slot);
      return g.getItem();
   }

   @Override
   public int getContainerSize() { return 15; }

   @Override
   public @NotNull ItemStack removeItem(int slotId, int count) {
      Map<Integer, IItemStack> map = armor;
      if (slotId >= 4) {
         map = weapons;
         slotId -= 4;
      }
      ItemStack itemStack = ItemStack.EMPTY;
      if (map.get(slotId) != null) {
         if (map.get(slotId).getMCItemStack().getCount() <= count) {
            itemStack = map.get(slotId).getMCItemStack();
            map.put(slotId, null);
         } else {
            itemStack = map.get(slotId).getMCItemStack().split(count);
            if ((map.get(slotId)).getMCItemStack().getCount() == 0) {
               map.put(slotId, null);
            }
         }
      }
      return itemStack;
   }

   @Override
   public boolean removeDrop(int slot) {
      if (drops.containsKey(slot)) {
         drops.remove(slot);
         Map<Integer, DropSet> newDrop = new TreeMap<>();
         int j = 0;
         for (int s : drops.keySet()) {
            if (s == slot) {
               continue;
            }
            newDrop.put(j, drops.get(s));
            newDrop.get(j).pos = j;
            j++;
         }
         drops.clear();
         drops.putAll(newDrop);
         return true;
      }
      return false;
   }

   @Override
   public @NotNull ItemStack removeItemNoUpdate(int slotId) {
      Map<Integer, IItemStack> map = armor;
      if (slotId >= 4) {
         map = weapons;
         slotId -= 4;
      }
      ItemStack itemStack = ItemStack.EMPTY;
      if (map.get(slotId) != null) {
         itemStack = (map.get(slotId)).getMCItemStack();
         map.put(slotId, null);
      }
      return itemStack;
   }

   @Override
   public @NotNull ItemStack getItem(int i) {
      if (i < 4) { return ItemStackWrapper.MCItem(getArmor(i)); }
      return i < 7 ? ItemStackWrapper.MCItem(weapons.get(i - 4)) : ItemStack.EMPTY;
   }

   @Override
   public void setItem(int slotId, @NotNull ItemStack stack) {
      Map<Integer, IItemStack> map = armor;
      if (slotId >= 4) {
         map = weapons;
         slotId -= 4;
      }
      if (stack.isEmpty()) { map.remove(slotId); }
      else { map.put(slotId, Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stack)); }
   }

   @Override
   public boolean stillValid(@NotNull Player player) { return true; }

   @Override
   public boolean canPlaceItem(int slotId, @NotNull ItemStack itemstack) { return true; }

   @Override
   public void setChanged() { }

   @Override
   public void startOpen(@NotNull Player player) { }

   @Override
   public void stopOpen(@NotNull Player player) { }

   @Override
   public int getExpMin() {
      return npc == null ? minExp : npc.inventory.minExp;
   }

   @Override
   public int getExpMax() {
      return npc == null ? maxExp : npc.inventory.maxExp;
   }

   @Override
   public int getExpRNG() {
      int exp = minExp;
      if (maxExp - minExp > 0) {
         int f = maxExp - minExp;
         exp += npc != null ? npc.level().random.nextInt(f) : new Random().nextInt(f);
      }
      return exp;
   }

   @Override
   public void setExp(int min, int max) {
      min = Math.min(min, max);
      if (npc != null) {
         npc.inventory.minExp = min;
         npc.inventory.maxExp = max;
      } else {
         minExp = min;
         maxExp = max;
      }
   }

   @Override
   public boolean isEmpty() {
      for(int slot = 0; slot < getContainerSize(); ++slot) {
         ItemStack item = getItem(slot);
         if (!NoppesUtilServer.isItemStackNull(item) && !item.isEmpty()) {
            return false;
         }
      }
      return true;
   }

   @Override
   public void clearContent() { }

   // New from Unofficial (BetaZavr)
   @Override
   public ICustomDrop addDropItem(IItemStack item, double chance) {
      return addDropItem(item == null ? ItemStack.EMPTY : item.getMCItemStack(), chance);
   }

   public ICustomDrop addDropItem(ItemStack item, double chance) {
      if (drops.size() >= CustomNpcs.MaxItemInDropsNPC) {
         throw new CustomNPCsException("Bad maximum size: " + drops.size() + " (" + CustomNpcs.MaxItemInDropsNPC + " slots maximum)");
      }
      chance = ValueUtil.correctDouble(chance, 0.0001d, 100.0d);
      DropSet ds = new DropSet(this);
      ds.item = item == null ? ItemStack.EMPTY : item;
      ds.chance = chance;
      ds.pos = drops.size();
      drops.put(ds.pos, ds);
      return ds;
   }

   /**
    * @param lootType 0: drop on ground
    * 1: drop under player feet
    * 2: into inventory
    * @param baseChance 0 <> 1.0
    * @return inventories map
    */
   @Override
   public Map<IEntity<?>, List<IItemStack>> createDrops(int lootType, double baseChance) {
      List<DropSet> allDrops = new ArrayList<>();
      if (dropType != 0 && !saveDropsName.isEmpty()) { allDrops = DropController.getInstance().getDrops(saveDropsName); } // template drops
      if (dropType != 1) { allDrops.addAll(drops.values()); } // both
      List<IItemStack> anyItems = new ArrayList<>();
      Map<IEntity<?>, List<IItemStack>> map = new HashMap<>();
      for (DropSet ds : allDrops) {
         double c = ds.chance * baseChance / 100.0d;
         double r = Math.random();
         if (ds.item == null || ds.item.isEmpty() || lootType != ds.lootMode || (ds.amount[0] == 0 && ds.amount[1] == 0) || (c < 1.0d && c < r)) { continue; }
         IItemStack iStack = ds.createLoot(baseChance);
         if (iStack.isEmpty()) { continue; }
         if (ds.availability.hasOptions()) {
            for (LivingEntity attacking : npc.combatHandler.aggressors.keySet()) {
               if (attacking instanceof Player player && ds.availability.isAvailable(player)) {
                  IEntity<?> iEntity = Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player);
                  if (iEntity != null) {
                     if (!map.containsKey(iEntity)) { map.put(iEntity, new ArrayList<>()); }
                     map.get(iEntity).add(iStack);
                     iStack.setOwner(iEntity);
                  }
               }
            }
         }
         else { anyItems.add(iStack); }
      }
      // put simple items
      if (!anyItems.isEmpty()) {
         // shuffle
         Collections.shuffle(anyItems, new Random());
         // sort aggressors
         LinkedHashMap<LivingEntity, Double> aggressors = Util.instance.sortByValue(npc.combatHandler.aggressors);
         // create aggressors list
         IEntity<?> damageLieder = null;
         Map<IEntity<?>, Double> entitys = new LinkedHashMap<>();
         double totalDamageValue = 0.0d;
         for (LivingEntity attacking : aggressors.keySet()) {
            if (attacking instanceof Player || !npc.combatHandler.onlyPlayers && lootType != 2) {
               IEntity<?> iEntity = Objects.requireNonNull(NpcAPI.Instance()).getIEntity(attacking);
               if (iEntity != null) {
                  if (damageLieder == null) { damageLieder = iEntity; }
                  entitys.put(iEntity, aggressors.get(attacking));
                  totalDamageValue += aggressors.get(attacking);
               }
            }
         }
         // amount rewards
         Map<IEntity<?>, Integer> itemsToEntity = new HashMap<>();
         int s = anyItems.size();
         for (IEntity<?> attacking : entitys.keySet()) {
            int amount = (int) Math.round(totalDamageValue / entitys.get(attacking) * anyItems.size());
            if (amount > s) { amount = 2; }
            itemsToEntity.put(attacking, amount);
            s -= amount;
            if (s == 0) { break; }
         }
         if (damageLieder == null) {
            map.put(npc.wrappedNPC, anyItems);
         } else {
            if (s != 0) { itemsToEntity.put(damageLieder, itemsToEntity.get(damageLieder) + s); }
            // put items to entities
            for (IEntity<?> attacking : itemsToEntity.keySet()) {
               if (anyItems.isEmpty()) { break; }
               for (int i = 0; i < itemsToEntity.get(attacking); i++) {
                  if (anyItems.isEmpty()) { break; }
                  IItemStack iStack = anyItems.get(0);
                  if (!map.containsKey(attacking)) { map.put(attacking, new ArrayList<>()); }
                  map.get(attacking).add(iStack);
                  anyItems.remove(iStack);
               }
            }
         }
      }
      return map;
   }

   /**
    * Merges identical item stacks into combined stacks.
    * Items are considered identical if they have the same item type, damage value, and NBT tags.
    */
   private List<IItemStack> mergeItemStacks(List<IItemStack> stacks) {
      List<IItemStack> merged = new ArrayList<>();
      for (IItemStack newStack : stacks) {
         if (newStack == null || newStack.isEmpty()) { continue; }
         ItemStack mcNew = newStack.getMCItemStack();
         boolean mergedIntoExisting = false;
         for (IItemStack existing : merged) {
            ItemStack mcExisting = existing.getMCItemStack();
            if (NoppesUtilPlayer.compareItems(mcNew, mcExisting, false, false)) {
               int spaceLeft = mcExisting.getMaxStackSize() - mcExisting.getCount();
               if (spaceLeft > 0) {
                  int toAdd = Math.min(spaceLeft, mcNew.getCount());
                  mcExisting.setCount(mcExisting.getCount() + toAdd);
                  mcNew.setCount(mcNew.getCount() - toAdd);
                  if (mcNew.getCount() == 0) {
                     mergedIntoExisting = true;
                     break;
                  }
               }
            }
         }
         if (!mergedIntoExisting && mcNew.getCount() > 0) {
            merged.add(newStack);
         }
      }
      return merged;
   }

   @SuppressWarnings("all")
   public void dropStuff(NpcEvent.DiedEvent event, Entity entity, DamageSource damagesource) {
      deadLoot = null;
      deadLoots = null;
      // Vanilla
      ArrayList<ItemEntity> list = new ArrayList<>();
      if (event.droppedItems != null) {
         for (IItemStack iStack : event.droppedItems) {
            if (iStack == null || iStack.isEmpty()) { continue; }
            ItemEntity e = getItemEntity(iStack.getMCItemStack().copy(), event.droppedItems.length > 7);
            if (e != null) {
               if (iStack.getOwner() != null) {
                  IEntity<?> iEntity = iStack.getOwner();
                  e.setPickUpDelay(2);
                  e.setTarget(iEntity.getMCEntity().getUUID());
                  e.setPos(iEntity.getPos().getX(), iEntity.getPos().getY() + iEntity.getMCEntity().getEyeHeight() / 2.0d, iEntity.getPos().getZ());
               }
               list.add(e);
            }
         }
      }
      int enchant = 0;
      if (damagesource.getEntity() instanceof Player player) { enchant = EnchantmentHelper.getMobLooting(player); }
      if (!ForgeHooks.onLivingDrops(npc, damagesource, list, enchant, true)) {
         Entity e = Objects.requireNonNullElse(npc, entity);
         for (ItemEntity item : list) {
            if (item == null) { continue; }
            e.level().addFreshEntity(item);
         }
      }
      list.clear();
      if (event.lootedItems != null) {
         for (IEntity<?> iEntity : event.lootedItems.keySet()) {
            for (IItemStack iStack : event.lootedItems.get(iEntity)) {
               if (iStack == null || iStack.isEmpty()) { continue; }
               ItemEntity e = getItemEntity(iStack.getMCItemStack().copy(), event.lootedItems.get(iEntity).size() > 7);
               if (e == null) { continue; }
               if (iEntity instanceof IPlayer iPlayer) {
                  Player player = iPlayer.getMCEntity();
                  e.setPickUpDelay(2);
                  e.setTarget(player.getUUID());
                  npc.level().addFreshEntity(e);
                  ItemStack stack = e.getItem();
                  if (!player.getInventory().add(stack)) { continue; }
                  player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                  player.onItemPickup(e);
                  if (stack.getCount() <= 0) { e.onRemovedFromWorld(); }
               } else {
                  e.setPos(iEntity.getPos().getX(), iEntity.getPos().getY() + iEntity.getMCEntity().getEyeHeight() / 2.0d, iEntity.getPos().getZ());
                  npc.level().addFreshEntity(e);
               }
            }
         }
      }
      if (event.inventoryItems != null) {
         // common inventory
         if (event.totalDamageOnlyPlayers == 0.0d) {
            List<IItemStack> rawStacks = new ArrayList<>();
            for (IEntity<?> iEntity : event.inventoryItems.keySet()) {
               for (IItemStack iStack : event.inventoryItems.get(iEntity)) {
                  if (!rawStacks.contains(iStack)) { rawStacks.add(iStack); }
                  if (rawStacks.size() == 54) { break; }
               }
               if (rawStacks.size() == 54) { break; }
            }
            List<IItemStack> stacks = mergeItemStacks(rawStacks);
            deadLoot = new SimpleContainer(Math.max(9, (int) (Math.ceil((double) stacks.size() / 9.0d) * 9.0d)));
            int i = 0;
            for (IItemStack stack : stacks) {
               deadLoot.setItem(i, stack.getMCItemStack());
               i++;
            }
         }
         // private inventory
         for (IEntity<?> iEntity : event.inventoryItems.keySet()) {
            List<IItemStack> rawStacks = new ArrayList<>();
            for (IItemStack iStack : event.inventoryItems.get(iEntity)) {
               if (!rawStacks.contains(iStack)) { rawStacks.add(iStack); }
               if (rawStacks.size() == 54) { break; }
            }
            List<IItemStack> stacks = mergeItemStacks(rawStacks);
            SimpleContainer inv = new SimpleContainer(Math.max(9, (int) (Math.ceil((double) stacks.size() / 9.0d) * 9.0d)));
            int i = 0;
            for (IItemStack stack : stacks) {
               inv.setItem(i, stack.getMCItemStack());
               i++;
            }
            if (deadLoots == null) { deadLoots = new HashMap<>(); }
            deadLoots.put((LivingEntity) iEntity.getMCEntity(), inv);
         }
      }
      if (event.expDropped > 0) {
         if (!lootMode) {
            int exp = event.expDropped;
            while (exp > 0) {
               int currentValue = ExperienceOrb.getExperienceValue(exp);
               exp -= currentValue;
               npc.level().addFreshEntity(new ExperienceOrb(npc.level(), npc.getX(), npc.getY(), npc.getZ(), currentValue));
            }
         }
         else {
            for (IEntity<?> iEntity : event.damageMap.keySet()) {
               if (!(iEntity instanceof IPlayer)) { continue; }
               int exp = (int) ((double) event.expDropped * event.damageMap.get(iEntity) / event.totalDamageOnlyPlayers);
               Entity player = iEntity.getMCEntity();
               while (exp > 0) {
                  int currentValue = ExperienceOrb.getExperienceValue(exp);
                  exp -= currentValue;
                  npc.level().addFreshEntity(new ExperienceOrb(npc.level(), npc.getX(), npc.getY(), npc.getZ(), currentValue));
               }
            }
         }
      }
   }

   public ItemEntity getItemEntity(ItemStack itemstack, boolean throwFar) {
      if (npc != null && itemstack != null && !itemstack.isEmpty()) {
         ItemEntity entityItem = new ItemEntity(npc.level(), npc.getX(), npc.getY() - 0.30000001192092896D + npc.getEyeHeight(), npc.getZ(), itemstack);
         entityItem.setPickUpDelay(40);
         if (throwFar) {
            float f = npc.getRandom().nextFloat() * 0.5f;
            float f1 = npc.getRandom().nextFloat() * (float) Math.PI * 2.0f;
            entityItem.setDeltaMovement((-Mth.sin(f1) * f), 0.20000000298023224D, Mth.cos(f1) * f);
         }
         return entityItem;
      }
      return null;
   }

   @Override
   public ICustomDrop[] getDrops() {
      ICustomDrop[] dss = new ICustomDrop[drops.size()];
      int i = 0;
      for (DropSet ds : drops.values()) {
         dss[i] = ds;
         i++;
      }
      return dss;
   }

   @Override
   public int getNpcLevel() { return npc.stats.getLevel(); }

   @Override
   public boolean removeDrop(DropSet dropSet) {
      Map<Integer, DropSet> newDrop = new TreeMap<>();
      boolean del = false;
      int j = 0;
      for (int slot : drops.keySet()) {
         if (drops.get(slot) == dropSet) {
            del = true;
            continue;
         }
         newDrop.put(j, drops.get(slot));
         newDrop.get(j).pos = j;
         j++;
      }
      if (del) {
         drops.clear();
         drops.putAll(newDrop);
      }
      return del;
   }

}
