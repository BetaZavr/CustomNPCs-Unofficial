package noppes.npcs.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.packets.client.PacketChat;
import noppes.npcs.api.handler.data.IDialog;
import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.data.MiniMapData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

public class PlayerQuestController {

	@SuppressWarnings("unused")
	public static boolean hasActiveQuests(EntityPlayer player) {
		return !PlayerData.get(player).questData.activeQuests.isEmpty();
	}

	public static boolean isQuestActive(EntityPlayer player, int questId) {
		return PlayerData.get(player).questData.activeQuests.containsKey(questId);
	}

	public static boolean isQuestCompleted(EntityPlayer player, int questId) {
		PlayerData data = PlayerData.get(player);
        QuestData q = data.questData.activeQuests.get(questId);
        return q != null && q.isCompleted;
    }

	public static boolean isQuestFinished(EntityPlayer player, int questId) {
		return PlayerData.get(player).questData.hasFinishedQuest(questId);
	}

	public static boolean canQuestBeAccepted(EntityPlayer player, int questId) {
		Quest quest = QuestController.instance.quests.get(questId);
		if (quest == null) { return false; }
		PlayerQuestData questData = PlayerData.get(player).questData;
		if (questData.activeQuests.containsKey(quest.id)) { return false; }
		if (questData.hasFinishedQuest(quest.id) && quest.repeat != EnumQuestRepeat.REPEATABLE) {
			if (quest.repeat == EnumQuestRepeat.NONE) { return false; }
			long questTime = questData.getFinishedTime(quest.id);
			long time;
			MinecraftServer server = player.getServer();
			if (server == null) { server = CustomNpcs.Server; }
			if (server != null) { time = server.getWorld(0).getTotalWorldTime(); }
			else { time = player.world.getTotalWorldTime(); }
			if (quest.repeat == EnumQuestRepeat.MCDAILY) { return time - questTime >= 24000L; }
			else if (quest.repeat == EnumQuestRepeat.MCWEEKLY) { return time - questTime >= 168000L; }
			else if (quest.repeat == EnumQuestRepeat.RLDAILY) { return System.currentTimeMillis() - questTime >= 86400000L; }
			else if (quest.repeat == EnumQuestRepeat.RLWEEKLY) { return System.currentTimeMillis() - questTime >= 604800000L; }
			return false;
		}
		return true;
	}

	public static void addActiveQuest(Quest quest, EntityPlayer player, boolean skipBeAccepted) {
		if (player == null || quest == null || !quest.isSetUp()) { return; }
		PlayerData data = PlayerData.get(player);
		if (skipBeAccepted || data.scriptData.getIPlayer().canQuestBeAccepted(quest.id)) {
			if (EventHooks.onQuestStarted(data.scriptData, quest)) { return; }
			data.questData.activeQuests.put(quest.id, new QuestData(quest));
			Packets.send((EntityPlayerMP) player, new PacketAchievement(Component.translatable("quest.newquest"), Component.translatable(quest.title), 2, new NBTTagCompound()));
			Packets.send((EntityPlayerMP) player, new PacketChat(Component.translatable("quest.newquest").append(":").append(Component.translatable(quest.title))));
			data.updateClient = true;
			CustomNPCsScheduler.runTack(() -> {
				int taskId = 0;
				for (QuestObjective obj : quest.getObjectives(player)) {
					if (obj.getEnumType() == EnumQuestTask.ITEM) {
						data.questData.checkQuestCompletion(player, data.questData.activeQuests.get(quest.id));
					}
					if (obj.isSetPointOnMiniMap() && !data.minimap.getModName().equals("non")) {
						String name = quest.getTitle() + "_";
						if (obj.getType() == EnumQuestTask.ITEM.ordinal() || obj.getType() == EnumQuestTask.CRAFT.ordinal()) {
							name += obj.getItem().getDisplayName();
						}
						if (obj.getType() == EnumQuestTask.DIALOG.ordinal()) {
							IDialog d = DialogController.instance.get(obj.getTargetID());
							if (d != null) { name += d.getName(); }
							else { name += obj.getTargetName(); }
						}
						else { name += obj.getTargetName(); }
						MiniMapData mmd = data.minimap.getQuestTask(quest.id, taskId, name, obj.getCompassDimension());
						if (mmd == null) { mmd = (MiniMapData) data.minimap.addPoint(obj.getCompassDimension()); }
						mmd.setName(Util.instance.deleteColor(name));
						mmd.setPos(obj.getCompassPos());
						mmd.setQuestId(quest.id);
						mmd.setTaskId(taskId);
					}
					taskId++;
				}
			});
		}
	}

	public static void setQuestFinished(Quest quest, EntityPlayer player) {
		PlayerData data = PlayerData.get(player);
        PlayerQuestData questData = data.questData;
		data.minimap.removeQuestPoints(quest.id);
		questData.finish(quest, player);
		if (quest.repeat != EnumQuestRepeat.NONE) { // Change
			for (QuestObjective obj : quest.questInterface.getObjectives(player)) { // forget dialogues
				if (obj.getEnumType() != EnumQuestTask.DIALOG) { continue; }
				data.dialogData.dialogsRead.remove(obj.getTargetID());
			}
			for (int dID : quest.forgetDialogues) { data.dialogData.dialogsRead.remove(dID); }
			for (int qID : quest.forgetQuests) { questData.removeFinishedQuest(qID); }
		}
		data.updateClient = true;
	}

	@SuppressWarnings("unused")
	public static Vector<Quest> getActiveQuests(EntityPlayer player) {
		Vector<Quest> quests = new Vector<>();
		PlayerData data = PlayerData.get(player);
        for (QuestData questdata : data.questData.activeQuests.values()) {
			if (questdata.quest != null) { quests.add(questdata.quest); }
		}
		return quests;
	}

	// New from Unofficial (BetaZavr)
	public static boolean getRemoveActiveQuest(EntityPlayer player, int id) {
		PlayerData data = PlayerData.get(player);
        PlayerQuestData questData = data.questData;
		data.minimap.removeQuestPoints(id);
		if (!questData.activeQuests.containsKey(id)) { return false; }
		HashMap<Integer, QuestData> newData = new HashMap<>();
		boolean del = false;
		for (int qid : new ArrayList<>(questData.activeQuests.keySet())) {
			if (qid == id) {
				del = true;
				Quest quest = QuestController.instance.quests.get(id);
				for (int dialogId : quest.forgetDialogues) { data.dialogData.dialogsRead.remove(dialogId); }
				for (int questId : quest.forgetQuests) { data.questData.removeFinishedQuest(questId); }
				continue;
			}
			newData.put(qid, questData.activeQuests.get(qid));
		}
		if (del) {
			data.questData.activeQuests.clear();
			data.questData.activeQuests.putAll(newData);
			data.updateClient = true;
		}
		return del;
	}

}
