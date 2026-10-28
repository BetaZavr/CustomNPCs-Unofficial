package noppes.npcs.controllers.data;

import java.util.HashMap;
import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.handler.data.IPlayerData;
import noppes.npcs.api.wrapper.PlayerWrapper;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;

public class PlayerFactionData implements IPlayerData {

   protected static final String dataName = "FactionData";

   public final HashMap<Integer, Integer> factionData = new HashMap<>();

   @Override
   public void load(CompoundTag compound) {
      factionData.clear();
      if (compound != null && compound.contains(dataName, 9)) {
         ListTag list = compound.getList(dataName, 10);
         for (int i = 0; i < list.size(); ++i) {
            CompoundTag nbt = list.getCompound(i);
            factionData.put(nbt.getInt("Faction"), nbt.getInt("Points"));
         }
      }
   }

   @Override
   public CompoundTag save(CompoundTag compound) {
      ListTag list = new ListTag();
      for (int faction : factionData.keySet()) {
         CompoundTag nbt = new CompoundTag();
         nbt.putInt("Faction", faction);
         nbt.putInt("Points", factionData.get(faction));
         list.add(nbt);
      }
      compound.put(dataName, list);
      return compound;
   }

   public int getFactionPoints(Player player, int factionId) {
      Faction faction = FactionController.instance.getFaction(factionId);
      if (faction == null) { return 0; }
      if (!factionData.containsKey(factionId)) {
         if (player.level().isClientSide()) { factionData.put(factionId, faction.defaultPoints); }
         else {
            PlayerScriptData handler = PlayerData.get(player).scriptData;
            PlayerWrapper<?> wrapper = (PlayerWrapper<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player);
            PlayerEvent.FactionUpdateEvent event = new PlayerEvent.FactionUpdateEvent(wrapper, faction, faction.defaultPoints, true);
            EventHooks.onPlayerFactionChange(handler, event);
            if (!event.isCanceled() && event.faction != null) {
               int value = ValueUtil.correctInt(event.points, 0, Integer.MAX_VALUE);
               if (value >= 0) { factionData.put(event.faction.getId(), value); }
               else { factionData.remove(event.faction.getId()); }
               Packets.send((ServerPlayer) player, new PacketSyncUpdate(0, 13, save(new CompoundTag())));
            }
         }
      }
      return factionData.getOrDefault(factionId, 0);
   }

   public void increasePoints(Player player, int factionId, int points) {
      if (player != null && !player.level().isClientSide()) {
         Faction faction = FactionController.instance.getFaction(factionId);
         if (faction != null) {
            PlayerScriptData handler = PlayerData.get(player).scriptData;
            PlayerEvent.FactionUpdateEvent event;
            event = new PlayerEvent.FactionUpdateEvent(handler.getPlayer(), faction, points, false);
            EventHooks.onPlayerFactionChange(handler, event);
            if (event.faction != null) {
               factionId = event.faction.getId();
               int value = ValueUtil.correctInt(event.points, 0, Integer.MAX_VALUE);
               if (!factionData.containsKey(factionId)) { value += faction.defaultPoints; } else { value += factionData.get(factionId); }
               if (value > 0) { factionData.put(factionId, value); } else { factionData.remove(factionId); }
               Packets.send((ServerPlayer) player, new PacketSyncUpdate(0, 13, save(new CompoundTag())));
            }
         }
      }
   }

   public CompoundTag getPlayerGuiData(@Nonnull ServerPlayer player) {
      CompoundTag compound = new CompoundTag();
      save(compound);
      ListTag list = new ListTag();
      for (int id : factionData.keySet()) {
         Faction faction = FactionController.instance.getFaction(id);
         if (faction != null && (player.isCreative() || !faction.hideFaction)) {
            CompoundTag com = new CompoundTag();
            faction.save(com);
            list.add(com);
         }
      }
      compound.put("FactionList", list);
      return compound;
   }

   public void clear() { factionData.clear(); }

}
