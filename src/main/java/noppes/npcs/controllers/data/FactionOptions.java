package noppes.npcs.controllers.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.controllers.FactionController;

import java.util.ArrayList;
import java.util.List;

// Change from Unofficial (BetaZavr)
public class FactionOptions {

   public final List<FactionOption> factionOptions = new ArrayList<>();

   public void load(CompoundTag compound) {
      factionOptions.clear();
      if (!compound.contains("FactionOptions", 9)) { // OLD version
         if (compound.getInt("OptionFactions1") > 0) {
            factionOptions.add(new FactionOption(compound.getInt("OptionFactions1"), compound.getInt("OptionFaction1Points"), compound.getBoolean("DecreaseFaction1Points")));
         }
         if (compound.getInt("OptionFactions2") > 0) {
            factionOptions.add(new FactionOption(compound.getInt("OptionFactions2"), compound.getInt("OptionFaction2Points"), compound.getBoolean("DecreaseFaction2Points")));
         }
      }
      else {
         for (int i = 0; i < compound.getList("FactionOptions", 10).size(); i++) {
            factionOptions.add(new FactionOption(compound.getList("FactionOptions", 10).getCompound(i)));
         }
      }
   }

   public CompoundTag save(CompoundTag compound) {
      ListTag list = new ListTag();
      for (FactionOption fo : factionOptions) { list.add(fo.save()); }
      compound.put("FactionOptions", list);
      return compound;
   }

   public boolean hasFaction(int id) {
      for (FactionOption fo : factionOptions) {
         if (fo.factionId == id) { return true; }
      }
      return false;
   }

   public void addPoints(Player player) {
      PlayerFactionData data = PlayerData.get(player).factionData;
      for (FactionOption fo : factionOptions) {
         if (fo.factionId < 0 || fo.factionPoints == 0) { continue; }
         int value = fo.factionPoints;
         boolean take = fo.decreaseFactionPoints;
         if (value < 0) {
            value *= -1;
            take = !take;
         }
         addPoints(player, data, fo.factionId, take, value);
      }
   }

   private void addPoints(Player player, PlayerFactionData data, int factionId, boolean decrease, int points) {
      Faction faction = FactionController.instance.getFaction(factionId);
      if (faction != null) {
         if (!faction.hideFaction) {
            String message = decrease ? "faction.decreasepoints" : "faction.increasepoints";
            player.sendSystemMessage(Component.translatable(message, faction.name, points));
         }
         data.increasePoints(player, factionId, decrease ? (-points) : points);
         PlayerData.get(player).updateClient = true;
      }
   }

   // New from Unofficial (BetaZavr)
   public FactionOptions copy() {
      FactionOptions fp = new FactionOptions();
      fp.load(save(new CompoundTag()));
      return fp;
   }

   public FactionOption get(int factionID) {
      for (FactionOption fo : factionOptions) {
         if (fo.factionId == factionID) { return fo; }
      }
      return null;
   }

   public boolean hasOptions() {
      for (FactionOption fo : factionOptions) {
         if (fo.factionId > 0 && fo.factionPoints != 0) { return true; }
      }
      return false;
   }

   public boolean remove(int factionID) {
      for (FactionOption fo : factionOptions) {
         if (fo.factionId == factionID) {
            factionOptions.remove(fo);
            return true;
         }
      }
      return false;
   }

   public List<Integer> getIDs() {
      List<Integer> list = new ArrayList<>();
      for (FactionOption fo : factionOptions) { if (!list.contains(fo.factionId)) { list.add(fo.factionId); } }
      return list;
   }

}
