package noppes.npcs.controllers;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSyncRemove;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.api.handler.IQuestHandler;
import noppes.npcs.api.handler.data.IQuestCategory;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestCategory;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;
import noppes.npcs.util.NBTJsonUtil;

import javax.annotation.Nullable;

public class QuestController implements IQuestHandler {

	public static QuestController instance = new QuestController();
	public final TreeMap<Integer, QuestCategory> categoriesSync = new TreeMap<>();
	public final TreeMap<Integer, QuestCategory> categories = new TreeMap<>();
	public final TreeMap<Integer, Quest> quests = new TreeMap<>();
	private int lastUsedCatID = 1;
	private int lastUsedQuestID = 1;

	public QuestController() { instance = this; }

	public void load() {
		CustomNpcs.debugData.start(null);
		categories.clear();
		quests.clear();
		lastUsedCatID = 0;
		lastUsedQuestID = 0;
		// OLD variant
		try {
			File file = new File(CustomNpcs.getWorldSaveDirectory(), "quests.dat");
			if (file.exists()) {
				loadCategoriesOld(file);
				if (!file.delete()) { LogWriter.debug("Error delete \"" + file.getName() + "\" file"); }
				file = new File(CustomNpcs.getWorldSaveDirectory(), "quests.dat_old");
				if (file.exists() && !file.delete()) { LogWriter.debug("Error delete \"" + file.getName() + "\" file"); }
				CustomNpcs.debugData.end(null);
				return;
			}
		}
		catch (Exception e) { LogWriter.error(e); }

		File dir = getDir();
		if (!dir.exists()) {
			if (dir.mkdirs()) { loadDefaultQuests(); }
		}
		else {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File questFile : files) {
					if (questFile.isDirectory()) {
						QuestCategory category = loadCategoryDir(questFile);
						Iterator<Integer> ite = category.quests.keySet().iterator();
						while (ite.hasNext()) {
							int id = ite.next();
							if (id > lastUsedQuestID) {
								lastUsedQuestID = id;
							}
							Quest quest = category.quests.get(id);
							if (quests.containsKey(id)) {
								LogWriter.error("Duplicate id " + quest.id + " from category " + category.title);
								ite.remove();
							} else {
								quests.put(id, quest);
							}
						}
						++lastUsedCatID;
						category.id = lastUsedCatID;
						categories.put(category.id, category);
					}
				}
			}
		}
		CustomNpcs.debugData.end(null);
	}

	private QuestCategory loadCategoryDir(File dir) {
		QuestCategory category = new QuestCategory();
		category.title = dir.getName();
		for (File file : Objects.requireNonNull(dir.listFiles())) {
			if (file.isFile()) {
				if (file.getName().endsWith(".json")) {
					try {
						Quest quest = new Quest(category);
						quest.id = Integer.parseInt(file.getName().substring(0, file.getName().length() - 5));
						quest.loadPartial(NBTJsonUtil.LoadFile(file));
						category.quests.put(quest.id, quest);
					} catch (Exception e) {
						LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
					}
				}
			}
		}
		return category;
	}

	private void loadCategoriesOld(File file) throws Exception {
		NBTTagCompound compound = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
		lastUsedCatID = compound.getInteger("lastID");
		lastUsedQuestID = compound.getInteger("lastQuestID");
		NBTTagList list = compound.getTagList("Data", 10);
		for (int i = 0; i < list.tagCount(); ++i) {
			QuestCategory category = new QuestCategory();
			category.load(list.getCompoundTagAt(i));
			categories.put(category.id, category);
			saveCategory(category);
			Iterator<Map.Entry<Integer, Quest>> ita = category.quests.entrySet().iterator();
			while (ita.hasNext()) {
				Map.Entry<Integer, Quest> entry = ita.next();
				Quest quest = entry.getValue();
				quest.id = entry.getKey();
				if (quests.containsKey(quest.id)) { ita.remove(); }
				else { saveQuest(category, quest); }
			}
		}
	}

	private void loadDefaultQuests() {
		QuestCategory cat = new QuestCategory();
		cat.id = lastUsedCatID++;
		cat.title = "Village";

		Quest qst1 = new Quest(cat);
		qst1.id = lastUsedQuestID++;
		qst1.level = 1;
		qst1.rewardMoney = 2;
		qst1.repeat = EnumQuestRepeat.MCWEEKLY;
		qst1.title = "quest.base.0";
		qst1.logText = "quest.base.log.text.0";
		qst1.completeText = "quest.base.complete.text.0";

		QuestObjective task = qst1.questInterface.addTask(EnumQuestTask.ITEM);
		task.setItem(new ItemStack(Blocks.LOG, 5, 0));
		task.setMaxProgress(5);

		DropSet ds = new DropSet(qst1);
		ds.pos = 0;
		ds.setInventorySlotContents(0, new ItemStack(Items.MUSHROOM_STEW, 1, 0));

		qst1.rewardItems.put(ds.pos, ds);

		saveCategory(cat);
		saveQuest(cat, qst1);
	}

	public void removeCategory(int category) {
		QuestCategory cat = categories.get(category);
		if (cat != null) {
			File dir = new File(getDir(), cat.title);
			if (!Util.instance.removeFile(dir)) {
				LogWriter.error("Error delete " + dir + "; no access or file not uploaded!");
				return;
			}
			for (Integer qId : cat.quests.keySet()) { quests.remove(qId); }
			categories.remove(category);
			Packets.sendAll(new PacketSyncRemove(category, 3));
		}
	}

	public void saveCategory(QuestCategory category) {
		category.title = NoppesStringUtils.cleanFileName(category.title);
		if (categories.containsKey(category.id)) {
			QuestCategory currentCategory = categories.get(category.id);
			if (!currentCategory.title.equals(category.title)) {
				List<String> names = new ArrayList<>();
				for (QuestCategory qc : new ArrayList<>(categories.values())) {
					if (!qc.equals(category) && qc.id != category.id) { names.add(qc.title); }
				}
				String name = category.title;
				while(names.contains(name)) { name = name + "_"; }
				category.title = name;
				File newDir = new File(getDir(), category.title);
				File oldDir = new File(getDir(), currentCategory.title);
				if (newDir.exists()) {
					if (oldDir.exists()) { Util.instance.removeFile(oldDir); }
					return;
				}
				else if (!oldDir.renameTo(newDir)) { return; }
			}
			category.quests.clear();
			category.quests.putAll(currentCategory.quests);
		}
		else {
			if (category.id < 0) {
				++lastUsedCatID;
				category.id = lastUsedCatID;
			}
			List<String> names = new ArrayList<>();
			for (QuestCategory qc : new ArrayList<>(categories.values())) {
				if (!qc.equals(category) && qc.id != category.id) { names.add(qc.title); }
			}
			String name = category.title;
			while(names.contains(name)) { name = name + "_"; }
			category.title = name;
			File dir = new File(getDir(), category.title);
			if (!dir.exists() && !dir.mkdirs()) { LogWriter.error("Error create dir " + dir); }
		}
		categories.put(category.id, category);
		Packets.sendAll(new PacketSyncUpdate(category.id, 3, category.save(new NBTTagCompound())));
	}

	public void saveQuest(QuestCategory category, Quest quest) {
		if (category != null) {
			List<String> names = new ArrayList<>();
			boolean found = false;
			for (Quest q : new ArrayList<>(quest.category.quests.values())) {
				if (q.equals(quest) || q.id == quest.id) {
					q.load(quest.savePartial(new NBTTagCompound()));
					quest = q;
					found = true;
					break;
				} else { names.add(q.title); }
			}
			if (!found) {
				String name = quest.title;
				while(names.contains(name)) { name = name + "_"; }
				quest.title = name;
				if (quest.id < 0) {
					++lastUsedQuestID;
					quest.id = lastUsedQuestID;
				}
			}
			else {
				Quest finalQuest = quest;
				CustomNPCsScheduler.runTack(() -> {
					for (String name : PlayerDataController.instance.getPlayerNames()) {
						PlayerData pData = PlayerDataController.instance.getDataFromUsername(CustomNpcs.Server, name);
						if (pData != null) {
							for (QuestData qd : pData.questData.activeQuests.values()) {
								if (qd.quest.id == finalQuest.id) {
									qd.reset(finalQuest);
									break;
								}
							}
						}
					}
				});
			}
			quests.put(quest.id, quest);
			category.quests.put(quest.id, quest);
			File dir = new File(getDir(), category.title);
			if (dir.exists() || dir.mkdirs()) {
				File file = new File(dir, quest.id + ".json_new");
				File file1 = new File(dir, quest.id + ".json");
				try {
					NBTJsonUtil.SaveFile(file, quest.savePartial(new NBTTagCompound()));
					if (file1.exists() && !file1.delete()) { LogWriter.error("Error delete " + file1 + "; no access or file not uploaded!"); }
					if (file.renameTo(file1)) { LogWriter.error("Error rename " + file + "; no access or file not uploaded!"); }
					Packets.sendAll(new PacketSyncUpdate(category.id, 2, quest.save(new NBTTagCompound())));
				} catch (Exception e) {
					LogWriter.error(e);
				}
			}
		}
	}

	public void removeQuest(Quest quest) {
		File file = new File(new File(getDir(), quest.category.title), quest.id + ".json");
		if (file.delete()) {
			quests.remove(quest.id);
			quest.category.quests.remove(quest.id);
			Packets.sendAll(new PacketSyncRemove(quest.id, 2));
		}
	}

	private File getDir() { return new File(CustomNpcs.getWorldSaveDirectory(), "quests"); }

	@Override
	public List<IQuestCategory> categories() { return new ArrayList<>(categories.values()); }

	@Override
	public @Nullable Quest get(int id) { return quests.get(id); }

	public @Nullable Quest getQuestFromName(String questName) {
		for (Quest quest : quests.values()) {
			if (quest.getName().equalsIgnoreCase(questName)) { return quest; }
		}
		return null;
	}

}
