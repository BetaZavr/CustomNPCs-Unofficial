package noppes.npcs.controllers.data;

import java.util.HashMap;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.handler.data.IPlayerData;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.packets.client.PacketChat;
import noppes.npcs.client.gui.util.quests.QuestInterface;

public class PlayerQuestData implements IPlayerData {

   protected static final String dataName = "QuestData";

   public final HashMap<Integer, QuestData> activeQuests = new HashMap<>();
   protected final HashMap<Integer, Long> finishedQuests = new HashMap<>();
   public long overworldTime = 0L;

   // New from Unofficial (BetaZavr)
   public boolean updateClient;

   @Override
   public void load(CompoundTag mainCompound) {
      CompoundTag compound;
      if (mainCompound == null) { return; }
      if (mainCompound.contains(dataName, 10)) { compound = mainCompound.getCompound(dataName); }
      else if (mainCompound.contains("CompletedQuests", 9) || mainCompound.contains("ActiveQuests", 9)) { compound = mainCompound; }
      else { return; }
      ListTag list = compound.getList("ActiveQuests", 10);
      activeQuests.clear();
      for(int i = 0; i < list.size(); ++i) {
         CompoundTag nbt = list.getCompound(i);
         int id = nbt.getInt("Quest");
         Quest quest = QuestController.instance.quests.get(id);
         if (quest != null) {
            activeQuests.put(id, new QuestData(quest).load(nbt));
            activeQuests.get(id).reset(quest);
         }
      }
      list = compound.getList("CompletedQuests", 10);
      finishedQuests.clear();
      for(int i = 0; i < list.size(); ++i) {
         CompoundTag nbt = list.getCompound(i);
         int id = nbt.getInt("Quest");
         if (!activeQuests.containsKey(id)) { finishedQuests.put(id, nbt.getLong("Date")); }
      }
   }

   @Override
   public CompoundTag save(CompoundTag mainCompound) {
      CompoundTag compound = new CompoundTag();
      ListTag listCompletedQuests = new ListTag();
      for (int quest : finishedQuests.keySet()) {
         CompoundTag nbt = new CompoundTag();
         nbt.putInt("Quest", quest);
         nbt.putLong("Date", finishedQuests.get(quest));
         listCompletedQuests.add(nbt);
      }
      compound.put("CompletedQuests", listCompletedQuests);

      ListTag listActiveQuests = new ListTag();
      for (int quest : activeQuests.keySet()) {
         CompoundTag nbt = new CompoundTag();
         nbt.putInt("Quest", quest);
         activeQuests.get(quest).save(nbt);
         listActiveQuests.add(nbt);
      }
      compound.put("ActiveQuests", listActiveQuests);

      mainCompound.put(dataName, compound);
      return compound;
   }

   public void clear() {
      activeQuests.clear();
      finishedQuests.clear();
   }

   public QuestData getQuestCompletion(Player player, EntityNPCInterface npc) {
      for (QuestData data : activeQuests.values()) {
         Quest quest = data.quest;
         if (quest != null && quest.completion == EnumQuestCompletion.Npc && quest.completer.isNpc(npc) && quest.questInterface.isCompleted(player)) {
            return data;
         }
      }
      return null;
   }

   public boolean checkQuestCompletion(Player player, QuestData data) {
      QuestInterface inter = data.quest.questInterface;
      if (inter.isCompleted(player)) {
         if (data.isCompleted && data.quest.completion == EnumQuestCompletion.Npc) { return false; }
         if (!data.quest.complete(player, data)) {
            Packets.send((ServerPlayer)player, new PacketAchievement(Component.translatable("quest.completed"), Component.translatable(data.quest.title), 2, new CompoundTag()));
            Packets.send((ServerPlayer)player, new PacketChat(Component.translatable("quest.completed").append(": ").append(Component.translatable(data.quest.title))));
         }
         data.isCompleted = true;
         updateClient = true;
         EventHooks.onQuestFinished(PlayerData.get(player).scriptData, data.quest);
         return true;
      }
      return false;
   }

   public boolean finish(Quest quest, Player player) {
      if (quest != null) {
         activeQuests.remove(quest.id);
         if (quest.repeat == EnumQuestRepeat.RLDAILY || quest.repeat == EnumQuestRepeat.RLWEEKLY) {
            finishedQuests.put(quest.id, System.currentTimeMillis());
         }
         else {
            if (player != null) {
               MinecraftServer server = player.getServer();
               if (server == null) { server = CustomNpcs.Server; }
               if (server != null) { finishedQuests.put(quest.id, server.overworld().getGameTime()); }
               else { finishedQuests.put(quest.id, player.level().getGameTime()); }
            }
            else { finishedQuests.put(quest.id, System.currentTimeMillis()); }
         }
         updateClient = true;
         return true;
      }
      return false;
   }

   public void removeFinishedQuest(int questId) {
      finishedQuests.remove(questId);
      updateClient = true;
   }

   public boolean hasFinishedQuest(int questId) { return finishedQuests.containsKey(questId); }

   public Set<Integer> getFinishedQuest() { return finishedQuests.keySet(); }

   public long getFinishedTime(int questId) { return finishedQuests.get(questId); }

   public void clearFinishedQuests() {
      finishedQuests.clear();
      updateClient = true;
   }

}
