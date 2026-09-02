package noppes.npcs.controllers.data;

import java.util.HashMap;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
	public boolean updateClient; // ServerTickHandler.cnpcPlayerTick() 114

	@Override
	public void load(NBTTagCompound mainCompound) {
		NBTTagCompound compound;
		if (mainCompound == null) { return; }
		if (mainCompound.hasKey(dataName, 10)) { compound = mainCompound.getCompoundTag(dataName); }
		else if (mainCompound.hasKey("CompletedQuests", 9) || mainCompound.hasKey("ActiveQuests", 9)) { compound = mainCompound; }
		else { return; }
		NBTTagList list = compound.getTagList("ActiveQuests", 10);
		activeQuests.clear();
		for(int i = 0; i < list.tagCount(); ++i) {
			NBTTagCompound nbt = list.getCompoundTagAt(i);
			int id = nbt.getInteger("Quest");
			Quest quest = QuestController.instance.quests.get(id);
			if (quest != null) {
				activeQuests.put(id, new QuestData(quest).load(nbt));
				activeQuests.get(id).reset(quest);
			}
		}
		list = compound.getTagList("CompletedQuests", 10);
		finishedQuests.clear();
		for(int i = 0; i < list.tagCount(); ++i) {
			NBTTagCompound nbt = list.getCompoundTagAt(i);
			int id = nbt.getInteger("Quest");
			if (!activeQuests.containsKey(id)) { finishedQuests.put(id, nbt.getLong("Date")); }
		}
	}

	@Override
	public NBTTagCompound save(NBTTagCompound mainCompound) {
		NBTTagCompound compound = new NBTTagCompound();
		NBTTagList listCompletedQuests = new NBTTagList();
		for (int quest : finishedQuests.keySet()) {
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setInteger("Quest", quest);
			nbt.setLong("Date", finishedQuests.get(quest));
			listCompletedQuests.appendTag(nbt);
		}
		compound.setTag("CompletedQuests", listCompletedQuests);

		NBTTagList listActiveQuests = new NBTTagList();
		for (int quest : activeQuests.keySet()) {
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setInteger("Quest", quest);
			activeQuests.get(quest).save(nbt);
			listActiveQuests.appendTag(nbt);
		}
		compound.setTag("ActiveQuests", listActiveQuests);

		mainCompound.setTag(dataName, compound);
		return compound;
	}

	public void clear() {
		activeQuests.clear();
		finishedQuests.clear();
	}

	public QuestData getQuestCompletion(EntityPlayer player, EntityNPCInterface npc) {
		for (QuestData data : activeQuests.values()) {
			Quest quest = data.quest;
			if (quest != null && quest.completion == EnumQuestCompletion.Npc && quest.completer.getName().equals(npc.getName()) && quest.questInterface.isCompleted(player)) {
				return data;
			}
		}
		return null;
	}

	public boolean checkQuestCompletion(EntityPlayer player, QuestData data) {
		QuestInterface inter = data.quest.questInterface;
		if (inter.isCompleted(player)) {
			if (data.isCompleted && data.quest.completion == EnumQuestCompletion.Npc) { return false; }
			if (!data.quest.complete(player, data)) {
				Packets.send((EntityPlayerMP) player, new PacketAchievement(Component.translatable("quest.completed"), Component.translatable(data.quest.title), 2, new NBTTagCompound()));
				Packets.send((EntityPlayerMP)player, new PacketChat(Component.translatable("quest.completed").append(": ").append(Component.translatable(data.quest.title))));
			}
			data.isCompleted = true;
			updateClient = true;
			EventHooks.onQuestFinished(PlayerData.get(player).scriptData, data.quest);
			return true;
		}
		return false;
	}

	public boolean finish(Quest quest, EntityPlayer player) {
		if (quest != null) {
			activeQuests.remove(quest.id);
			if (quest.repeat == EnumQuestRepeat.RLDAILY || quest.repeat == EnumQuestRepeat.RLWEEKLY) {
				finishedQuests.put(quest.id, System.currentTimeMillis());
			}
			else {
				if (player != null) {
					MinecraftServer server = player.getServer();
					if (server == null) { server = CustomNpcs.Server; }
					if (server != null) { finishedQuests.put(quest.id, server.getWorld(0).getTotalWorldTime()); }
					else { finishedQuests.put(quest.id, player.world.getTotalWorldTime()); }
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
