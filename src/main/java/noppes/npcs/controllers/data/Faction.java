package noppes.npcs.controllers.data;

import java.util.HashSet;
import java.util.Iterator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.IFaction;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.entity.EntityNPCInterface;

public class Faction implements IFaction {

   public String name;
   public int color = 0xFFFFFF;
   public final HashSet<Integer> attackFactions = new HashSet<>();
   public int id = -1;
   public int neutralPoints = 500;
   public int friendlyPoints = 1500;
   public int defaultPoints = 1000;
   public boolean hideFaction = false;
   public boolean getsAttacked = false;
   public FactionOptions factions = new FactionOptions();

   // New from Unofficial (BetaZavr)
   public HashSet<Integer> frendFactions = new HashSet<>();
   public MutableComponent description = Component.empty();
   public ResourceLocation flag = new ResourceLocation(CustomNpcs.MODID + ":textures/cloak/mojang.png");

   public Faction() { }

   public Faction(int idIn, String nameIn, int colorIn, int defaultPointsIn) {
      id = idIn;
      name = nameIn;
      color = colorIn;
      defaultPoints = defaultPointsIn;
   }

   public void load(CompoundTag compound) {
      name = compound.getString("Name");
      color = compound.getInt("Color");
      id = compound.getInt("Slot");
      neutralPoints = compound.getInt("NeutralPoints");
      friendlyPoints = compound.getInt("FriendlyPoints");
      defaultPoints = compound.getInt("DefaultPoints");
      hideFaction = compound.getBoolean("HideFaction");
      getsAttacked = compound.getBoolean("GetsAttacked");
      factions.load(compound.getCompound("FactionPoints"));
      attackFactions.clear();
      attackFactions.addAll(NBTTags.getIntegerSet(compound.getList("AttackFactions", 10)));

      // New from Unofficial (BetaZavr)
      frendFactions.addAll(NBTTags.getIntegerSet(compound.getList("FrendFactions", 10)));
      if (compound.contains("Flag", 8)) { setFlag(compound.getString("Flag")); }
      if (compound.contains("Description", 8)) { description = Component.Serializer.fromJson(compound.getString("Description")); }
   }

   @Override
   public void save() { FactionController.instance.saveFaction(this); }

   public CompoundTag save(CompoundTag compound) {
      compound.putInt("Slot", id);
      compound.putString("Name", name);
      compound.putInt("Color", color);
      compound.putInt("NeutralPoints", neutralPoints);
      compound.putInt("FriendlyPoints", friendlyPoints);
      compound.putInt("DefaultPoints", defaultPoints);
      compound.putBoolean("HideFaction", hideFaction);
      compound.putBoolean("GetsAttacked", getsAttacked);
      compound.put("AttackFactions", NBTTags.nbtIntegerCollection(attackFactions));
      compound.put("FactionPoints", factions.save(new CompoundTag()));

      // New from Unofficial (BetaZavr)
      compound.putString("Flag", flag != null ? flag.toString() : "");
      compound.putString("Description", Component.Serializer.toJson(description));
      compound.put("FrendFactions", NBTTags.nbtIntegerCollection(frendFactions));
      return compound;
   }

   public boolean isFriendlyToPlayer(Player player) {
      return PlayerData.get(player).factionData.getFactionPoints(player, id) >= friendlyPoints;
   }

   public boolean isAggressiveToPlayer(Player player) {
      if (player.isCreative()) { return false; }
      return PlayerData.get(player).factionData.getFactionPoints(player, id) < neutralPoints;
   }

   public boolean isNeutralToPlayer(Player player) {
      int points = PlayerData.get(player).factionData.getFactionPoints(player, id);
      return points >= neutralPoints && points < friendlyPoints;
   }

   public boolean isAggressiveToNpc(EntityNPCInterface npc) {
      return attackFactions.contains(npc.faction.id) || npc.advanced.attackFactions.contains(id);
   }

   @Override
   public int getId() { return id; }

   @Override
   public String getName() { return name; }

   @Override
   public int getDefaultPoints() { return defaultPoints; }

   @Override
   public void setDefaultPoints(int points) { defaultPoints = points; }

   @Override
   public int getColor() { return color; }

   @Override
   public int playerStatus(IPlayer<?> player) {
      return player == null ? -1 : playerStatus(player.getMCEntity());
   }

   public int playerStatus(Player player) {
      return player == null || isAggressiveToPlayer(player) ? -1 : isFriendlyToPlayer(player) ? 1 : 0;
   }

   @Override
   public boolean hostileToNpc(ICustomNpc<?> npc) { return npc != null && attackFactions.contains(npc.getFaction().getId()); }

   @Override
   public boolean hostileToFaction(int factionId) { return attackFactions.contains(factionId); }

   @Override
   public int[] getHostileList() {
      int[] a = new int[attackFactions.size()];
      int i = 0;
      Integer val;
      for(Iterator<Integer> var3 = attackFactions.iterator(); var3.hasNext(); a[i++] = val) { val = var3.next(); }
      return a;
   }

   @Override
   public void addHostile(int factionId) {
      if (attackFactions.contains(factionId)) { throw new CustomNPCsException("Faction " + factionId + " is already hostile to " + id); }
      frendFactions.remove(factionId);
      attackFactions.add(factionId);
   }

   @Override
   public void removeHostile(int id) { attackFactions.remove(id); }

   @Override
   public boolean hasHostile(int id) { return attackFactions.contains(id); }

   @Override
   public boolean getIsHidden() { return hideFaction; }

   @Override
   public void setIsHidden(boolean bo) { hideFaction = bo; }

   @Override
   public boolean getAttackedByMobs() { return getsAttacked; }

   @Override
   public void setAttackedByMobs(boolean bo) { getsAttacked = bo; }

   // New from Unofficial (BetaZavr)
   @Override
   public String getDescription() { return description.getString(); }

   @Override
   public void setDescription(String descriptionIn) {
      if (descriptionIn == null || descriptionIn.isEmpty()) { description = Component.empty(); }
      else { description = Component.translatable(descriptionIn); }
   }

   @Override
   public String getFlag() { return flag == null ? "" : flag.toString(); }

   @Override
   public void setFlag(String flagPath) {
      if (flagPath == null || flagPath.isEmpty()) {
         flag = null;
         return;
      }
      flag = new ResourceLocation(NoppesUtilServer.validLocation(flagPath));
   }

}
