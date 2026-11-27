package noppes.npcs.roles;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.constants.JobType;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.role.IRoleFollower;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerGameData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RoleFollower extends RoleInterface implements IRoleFollower {

   public boolean disableGui = false;
   public boolean infiniteDays = false;
   public boolean isFollowing = true;
   public boolean refuseSoulStone = false;
   public int daysHired;
   public String ownerUUID;
   public long hiredTime;
   public long waitTime = 0;
   public int rentalMoney = 0;
   public boolean rentalIsDonat = false;
   public NpcMiscInventory rentalItems = new NpcMiscInventory(3);
   public NpcMiscInventory inventory = new NpcMiscInventory(0);
   public Player owner = null;
   public HashMap<Integer, Integer> rates = new HashMap<>();
   public String dialogFarewell = getTexts("follower.farewellText", " {player}");
   public String dialogFired = getTexts("follower.firedText", " {player}");
   public String dialogHire = getTexts("follower.hireText", " {days} ", Component.translatable("follower.days"));
   public String dialogHired = getTexts("follower.is.hired", " {player}");

   private String getTexts(@Nonnull String main, Object ... added) {
      MutableComponent component = Component.translatable(main);
      for (Object part : added) {
         if (part instanceof String str) { component.append(str); }
         else if (part instanceof Component comp) { component.append(comp); }
      }
      return Util.instance.deleteColor(Util.instance.getOldFormattedText(component));
   }

   public RoleFollower(EntityNPCInterface npc) {
      super(npc);
      type = RoleType.FOLLOWER;
   }

   @Override
   public void load(CompoundTag compound) {
      super.load(compound);
      type = RoleType.FOLLOWER;
      ownerUUID = compound.getString("MercenaryOwner");
      daysHired = compound.getInt("MercenaryDaysHired");
      hiredTime = compound.getLong("MercenaryHiredTime");
      rates = NBTTags.getIntegerIntegerMap(compound.getList("MercenaryDayRates", 10));
      if (compound.contains("MercenaryInventory", 10)) {
         int size = compound.getCompound("MercenaryInventory").getInt("NpcMiscInvSize");
         inventory = new NpcMiscInventory(ValueUtil.correctInt(size, 0, 9));
         inventory.load(compound.getCompound("MercenaryInventory"));
      }
      isFollowing = compound.getBoolean("MercenaryIsFollowing");
      disableGui = compound.getBoolean("MercenaryDisableGui");
      infiniteDays = compound.getBoolean("MercenaryInfiniteDays");
      refuseSoulStone = compound.getBoolean("MercenaryRefuseSoulstone");
      rentalIsDonat = compound.getBoolean("MercenaryRentalIsDonat");
      dialogHire = compound.getString("MercenaryDialogHired");
      dialogHired = compound.getString("MercenaryDialog");
      dialogFarewell = compound.getString("MercenaryDialogFarewell");
      // New from Unofficial (BetaZavr)
      rentalItems.load(compound.getCompound("MercenaryInv"));
      rentalMoney = compound.getInt("MercenaryMoney");
      if (compound.contains("MercenaryDialogFired", 8)) {
         dialogFired = compound.getString("MercenaryDialogFired");
      }
   }

   @Override
   public CompoundTag save(CompoundTag compound) {
      super.save(compound);
      compound.putInt("MercenaryDaysHired", daysHired);
      compound.putLong("MercenaryHiredTime", hiredTime);
      compound.putString("MercenaryDialogHired", dialogHire);
      compound.putString("MercenaryDialog", dialogHired);
      compound.putString("MercenaryDialogFarewell", dialogFarewell);
      if (hasOwner()) { compound.putString("MercenaryOwner", ownerUUID); }
      compound.put("MercenaryDayRates", NBTTags.nbtIntegerIntegerMap(rates));
      compound.put("MercenaryInventory", inventory.save());
      compound.putBoolean("MercenaryIsFollowing", isFollowing);
      compound.putBoolean("MercenaryDisableGui", disableGui);
      compound.putBoolean("MercenaryInfiniteDays", infiniteDays);
      compound.putBoolean("MercenaryRefuseSoulstone", refuseSoulStone);
      compound.putBoolean("MercenaryRentalIsDonat", rentalIsDonat);
      // New from Unofficial (BetaZavr)
      compound.put("MercenaryInv", rentalItems.save());
      compound.putInt("MercenaryMoney", rentalMoney);
      compound.putString("MercenaryDialogFired", dialogFired);
      return compound;
   }

   @Override
   public boolean aiShouldExecute() {
      // New from Unofficial (BetaZavr)
      if (npc.getHealth() <= 0.0f) { return false; }
      if ((ownerUUID == null || ownerUUID.isEmpty()) && !npc.level().dimension().location().equals(npc.homeDimensionId.location())) {
         npc = (EntityNPCInterface) Util.instance.teleportEntity(npc.level().getServer(), npc,
                 npc.homeDimensionId, npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos());
         return false;
      }
      PlayerData plData = getOwnerData();
      if (plData == null) {
         if (ownerUUID != null && !ownerUUID.isEmpty()) { killed(); }
         return false;
      }
      PlayerGameData.FollowerSet fs = plData.game.getFollower(npc);
      if (fs == null) { fs = plData.game.addFollower(npc); }
      fs.dimId = npc.level().dimension().location();
      fs.npc = npc;
      owner = getOwner();
      if (!infiniteDays && (getCurrentTime() - hiredTime) > (long) getDays() * (long) Level.TICKS_PER_DAY) {
         RoleEvent.FollowerFinishedEvent event = new RoleEvent.FollowerFinishedEvent(owner, npc.wrappedNPC);
         EventHooks.onNPCRole(npc, event);
         if (owner != null && owner.containerMenu instanceof ContainerNPCFollowerHire) { owner.closeContainer(); }
         npc.say(owner, new Line(NoppesStringUtils.formatText(dialogFarewell, owner, npc)));
         plData.game.removeFollower(npc);
         killed();
      }
      if (npc.getTarget() != null) { return false; }
      if (!isFollowing) {
         if (!npc.getNavigation().isDone()) { npc.getNavigation().stop(); }
         return false;
      }
      if (owner == null) { return false; }
      double dist = npc.distanceTo(owner);
      if (!owner.level().dimension().location().equals(npc.level().dimension().location())) {
         npc = (EntityNPCInterface) Util.instance.teleportEntity(npc.level().getServer(), npc, owner.level().dimension(), owner.getX(), owner.getY(), owner.getZ());
         fs.dimId = npc.level().dimension().location();
         fs.id = npc.getUUID();
         fs.npc = npc;
         npc.getNavigation().moveTo(owner, npc.ais.canSprint ? 1.3 : 1.0d);
      }
      else if (dist <= 2.5d) {
         if (!npc.getNavigation().isDone()) { npc.getNavigation().stop(); }
         return false;
      }
      else if (dist > getRange()) { npc.setPos(owner.getX(), owner.getY(), owner.getZ()); }
      else {
         boolean bo = npc.getNavigation().moveTo(owner, npc.ais.canSprint ? 1.3 : 1.0d);
         if (!bo && !npc.isMoving()) {
            if (waitTime == 0) {
               waitTime = 10;
               return false;
            }
            waitTime--;
            if (waitTime <= 0) { npc.setPos(owner.getX(), owner.getY(), owner.getZ()); }
         }
         else { waitTime = 0; }
      }
      return false;
   }

   @SuppressWarnings("ConstantConditions")
   public Player getOwner() {
      if (npc != null && npc.level() != null && !npc.level().isClientSide && (ownerUUID != null && !ownerUUID.isEmpty())) {
         try {
            UUID uuid = UUID.fromString(ownerUUID);
             return npc.level().getPlayerByUUID(uuid);
         }
         catch (IllegalArgumentException ignored) { }
         return ((ServerLevel)npc.level()).players()
                 .stream()
                 .filter((t) -> t.getName().getString().equals(ownerUUID))
                 .findFirst().orElse(null);
      }
      return null;
   }

   public boolean hasOwner() { return (infiniteDays || daysHired > 0) && ownerUUID != null && !ownerUUID.isEmpty(); }

   @Override
   public void killed() {
      if (!inventory.isEmpty()) {
         if (owner == null) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
               ItemStack stack = inventory.getItem(i);
               if (!stack.isEmpty()) {
                  npc.spawnAtLocation(stack, 0.0f);
               }
            }
         }
         else if (owner.level().dimension() == npc.level().dimension()) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
               ItemStack stack = inventory.getItem(i);
               if (!stack.isEmpty()) {
                  ItemEntity entityItem = new ItemEntity(owner.level(), owner.getX(), owner.getY(), owner.getZ(), stack);
                  entityItem.setPickUpDelay(0);
                  owner.level().addFreshEntity(entityItem);
               }
            }
         }
         inventory.clearContent();
      }
      ownerUUID = null;
      daysHired = 0;
      hiredTime = 0L;
      isFollowing = true;
      PlayerData plData = getOwnerData();
      if (plData != null) {
         plData.game.removeFollower(npc);
         plData.save(true);
      }
   }

   @Override
   public void reset() { killed(); }

   @Override
   public void interact(Player playerIn) {
      boolean bo = false;
      for (int i =0; i < rentalItems.getContainerSize(); i++) {
         if (!NoppesUtilServer.isItemStackNull(rentalItems.getItem(i)) && rates.get(i) > 0) {
            bo = true;
            break;
         }
      }
      if (rentalMoney > 0 && rates.get(3) > 0) { bo = true; }
      if (bo && playerIn instanceof ServerPlayer player) {
         if (ownerUUID != null && !ownerUUID.isEmpty()) {
            if (player.equals(getOwner()) && !disableGui) { SPacketGuiOpen.sendOpenGui(player, EnumGuiType.PlayerFollower, npc, BlockPos.ZERO); }
            else if (npc != null && !dialogHired.isEmpty()) { npc.say(player, new Line(dialogHired)); }
         }
         else {
            if (npc != null) { npc.say(player, npc.advanced.getInteractLine()); }
            SPacketGuiOpen.sendOpenGui(player, EnumGuiType.PlayerFollowerHire, npc, BlockPos.ZERO);
         }
      }
   }

   @Override
   public boolean defendOwner() { return isFollowing() && npc != null && npc.job.getEnumType() == JobType.GUARD; }

   @Override
   public boolean isFollowing() {
      return ownerUUID != null && !ownerUUID.isEmpty() && isFollowing &&
              (getCurrentTime() - hiredTime) < (long) getDays() * (long) Level.TICKS_PER_DAY;
   }

   public void setOwner(@Nullable Player player) {
      if (player == null ||
              ownerUUID == null ||
              !ownerUUID.equals(player.getUUID().toString())) { killed(); }
      if (player != null) { ownerUUID = player.getUUID().toString(); }
   }

   @Override
   public int getDays() {
      if (infiniteDays) { return 100; }
      return Math.max(daysHired, 0);
   }

   @Override
   public void addDays(int days) {
      if (hiredTime == 0L) {
         daysHired = days;
         hiredTime = getCurrentTime();
      }
      else { daysHired += days; }
   }

   @Override
   public boolean getInfinite() { return infiniteDays; }

   @Override
   public void setInfinite(boolean infinite) { infiniteDays = infinite; }

   @Override
   public boolean getGuiDisabled() { return disableGui; }

   @Override
   public void setGuiDisabled(boolean disabled) { disableGui = disabled; }

   @Override
   public boolean getRefuseSoulstone() { return refuseSoulStone; }

   @Override
   public void setRefuseSoulstone(boolean refuse) { refuseSoulStone = refuse; }

   @Override
   public boolean getRentalIsDonat() { return rentalIsDonat; }

   @Override
   public void setRentalIsDonat(boolean refuse) { rentalIsDonat = refuse; }

   @Override
   public IPlayer<?> getFollowing() {
      Player owner = getOwner();
      return owner != null ? (IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(owner) : null;
   }

   @Override
   public void setFollowing(IPlayer<?> player) {
      setOwner(player == null ? null : player.getMCEntity());
   }

   // New from Unofficial (BetaZavr)
   public long getCurrentTime() {
      Level level = null;
      if (npc != null) { level = npc.level(); }
      else if (CustomNpcs.Server != null) { level = CustomNpcs.Server.getLevel(Level.OVERWORLD); }
      return level != null ? level.getGameTime() : 0;
   }

   public int getRange() {
      if (npc.stats.aggroRange > CustomNpcs.NpcNavRange) { return CustomNpcs.NpcNavRange; }
      return npc.stats.aggroRange;
   }

   private PlayerData getOwnerData() {
      if (ownerUUID == null || ownerUUID.isEmpty() || CustomNpcs.Server == null || npc.level().getServer() == null) { return null; }
      return PlayerDataController.instance.getDataFromUsername(CustomNpcs.Server == null ? npc.level().getServer() : CustomNpcs.Server, ownerUUID);
   }

   @SuppressWarnings("unused")
   public int getDaysLeft() {
      if (infiniteDays) { return 100; }
      if (daysHired <= 0) { return 0; }
      int daysPassed = (int) Math.floor((double) (getCurrentTime() - hiredTime) / (double) Level.TICKS_PER_DAY);
      return Math.max(daysHired - daysPassed, 0);
   }

}
