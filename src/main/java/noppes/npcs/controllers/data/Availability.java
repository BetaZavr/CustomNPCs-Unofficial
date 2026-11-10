package noppes.npcs.controllers.data;

import java.util.*;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemClock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.Component;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import noppes.npcs.*;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.ICompatibilty;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.handler.data.*;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.*;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.controllers.*;
import noppes.npcs.mixin.scoreboard.IServerScoreboardMixin;
import noppes.npcs.util.ValueUtil;

public class Availability implements ICompatibilty, IAvailability {

	protected boolean hasOptions = false;

	public static HashSet<String> scores = new HashSet<>();
	public int[] daytime = new int[] { 0, 0 };
	public final Map<Integer, EnumAvailabilityDialog> dialogues = new TreeMap<>(); // ID, Availability
	public final Map<Integer, AvailabilityFactionData> factions = new TreeMap<>(); // ID, [Stance, Availability]
	public final Map<EnumAvailabilityMoney, AvailabilityMoneyData> moneys = new HashMap<>(); // ID, [Stance, Availability]


	public final Map<Integer, EnumAvailabilityQuest> quests = new TreeMap<>(); // ID, Availability
	public final Map<String, noppes.npcs.controllers.data.AvailabilityScoreboardData> scoreboards = new TreeMap<>(); // Objective, [Value, Availability]
	public final Map<String, EnumAvailabilityPlayerName> playerNames = new TreeMap<>();
	public final Map<Integer, EnumAvailabilityRegion> regions = new TreeMap<>(); // [ Region ID, Type ]
	public final Map<Integer, AvailabilityStackData> stacksData = new TreeMap<>(); // [ Slot ID, Type ]
	public final NpcMiscInventory stacks = new NpcMiscInventory(9);
	public final List<AvailabilityStoredData> storeddata = new ArrayList<>();
	public int version = VersionCompatibility.ModRev;
	public int max = 10;
	public int minPlayerLevel = 0;
	public int health = 100;
	public int healthType = 0;
	public boolean onlyGM = false;

	public Availability() {
		for (int i = 0; i < 9; i++) {
			stacksData.put(i, new AvailabilityStackData());
		}
	}

	private boolean checkHasOptions() {
		for (EnumAvailabilityDialog ead : dialogues.values()) {
			if (ead != EnumAvailabilityDialog.Always) { return true; }
		}
		for (EnumAvailabilityQuest eaq : quests.values()) {
			if (eaq != EnumAvailabilityQuest.Always) { return true; }
		}
		for (AvailabilityFactionData afd : factions.values()) {
			if (afd.factionAvailable != EnumAvailabilityFactionType.Always) { return true; }
		}
		for (String obj : scoreboards.keySet()) {
			if (!obj.isEmpty()) { return true; }
		}
		if (!playerNames.isEmpty()) { return true; }
		if (!storeddata.isEmpty()) { return true; }
		if (!moneys.isEmpty()) { return true; }
		if (hasHealth()) { return true; }
		if (daytime[0] >= 0 && daytime[0] <= 23 && daytime[1] >= 0 && daytime[1] <= 23 && daytime[0] != daytime[1]) { return true; }
		for (int i = 0; i < stacks.getSizeInventory(); i++) {
			if (!NoppesUtilServer.isItemStackNull(stacks.getStackInSlot(i))) { return true; }
		}
		if (!regions.isEmpty()) {
			for (int id : regions.keySet()) {
				if (regions.get(id) != EnumAvailabilityRegion.Always && BorderController.getInstance().regions.containsKey(id)) { return true; }
			}
		}
		return minPlayerLevel > 0 || onlyGM;
	}

	public void clear() {
		hasOptions = false;
		daytime[0] = 0;
		daytime[1] = 0;
		minPlayerLevel = 0;
		health = 100;
		healthType = 0;
		dialogues.clear();
		quests.clear();
		factions.clear();
		scoreboards.clear();
		playerNames.clear();
		moneys.clear();
	}

	public boolean isAvailable(EntityPlayer player) {
		if (!hasOptions) { return true; }
		if (daytime[0] >= 0 && daytime[0] <= 23 && daytime[1] >= 0 && daytime[1] <= 23 && daytime[0] != daytime[1]) {
			int time = (int) ((player.world.getWorldTime() + 30000L) % 24000L) / 1000;
			boolean bo;
			if (daytime[0] < daytime[1]) { bo = time >= daytime[0] && time <= daytime[1]; }
			else { bo = time >= daytime[0] || time <= daytime[1]; }
			if (!bo) { return false; }
		}
		for (int id : dialogues.keySet()) {
			if (!dialogAvailable(id, dialogues.get(id), player)) { return false; }
		}
		for (int id : quests.keySet()) {
			if (!questAvailable(id, quests.get(id), player)) { return false; }
		}
		for (int id : factions.keySet()) {
			if (!factionAvailable(id, factions.get(id).factionStance, factions.get(id).factionAvailable, player)) { return false; }
		}
		for (String obj : scoreboards.keySet()) {
			if (!scoreboardAvailable(player, obj, scoreboards.get(obj).scoreboardType, scoreboards.get(obj).scoreboardValue)) { return false; }
		}
		IData dataP = Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player).getStoreddata();
		for (AvailabilityStoredData sd : storeddata) {
			if (!storeddataAvailable(dataP, sd)) { return false; }
		}
		PlayerGameData gameData = PlayerData.get(player).game;
		for (EnumAvailabilityMoney eam : new ArrayList<>(moneys.keySet())) {
			if (!moneyAvailable(gameData, eam, moneys.get(eam))) { return false; }
		}
		for (int pos : stacksData.keySet()) {
			if (!stackAvailable(player, stacksData.get(pos), stacks.getStackInSlot(pos))) { return false; }
		}
		for (int id : regions.keySet()) {
			if (!regionAvailable(player, regions.get(id), BorderController.getInstance().regions.get(id))) { return false; }
		}
		boolean returnName = false;
		boolean hasOnly = false;
		for (String name : playerNames.keySet()) {
			boolean exit = false;
			switch (playerNames.get(name)) {
				case Only: {
					hasOnly = true;
					if (player.getName().equals(name)) {
						hasOnly = false;
						exit = true;
					}
					break;
				}
				case Except: {
					if (player.getName().equals(name)) {
						returnName = true;
						exit = true;
					}
					break;
				}
			}
			if (exit) { break; }
		}
		if (returnName || hasOnly) { return false; }
		if (healthType != 0) {
			int h = (int) (player.getHealth() / player.getMaxHealth() * 100);
			if ((healthType == 1 && h < health) || (healthType == 2 && h > health)) { return false; }
		}
		if (onlyGM && !player.capabilities.isCreativeMode) { return false; }
		return player.experienceLevel >= minPlayerLevel;
	}

	public boolean dialogAvailable(int id, EnumAvailabilityDialog en, EntityPlayer player) {
		if (en != EnumAvailabilityDialog.Always) {
			boolean hasRead = PlayerData.get(player).dialogData.has(id);
			return (hasRead && en == EnumAvailabilityDialog.After) || (!hasRead && en == EnumAvailabilityDialog.Before);
		}
		return true;
	}

	public boolean factionAvailable(int id, EnumAvailabilityFaction stance, EnumAvailabilityFactionType available, EntityPlayer player) {
		if (available != EnumAvailabilityFactionType.Always) {
			Faction faction = FactionController.instance.getFaction(id);
			if (faction != null) {
				PlayerFactionData data = PlayerData.get(player).factionData;
				int points = data.getFactionPoints(player, id);
				EnumAvailabilityFaction current = EnumAvailabilityFaction.Neutral;
				if (points < faction.neutralPoints) { current = EnumAvailabilityFaction.Hostile; }
				if (points >= faction.friendlyPoints) { current = EnumAvailabilityFaction.Friendly; }
				return (available == EnumAvailabilityFactionType.Is && stance == current)
						|| (available == EnumAvailabilityFactionType.IsNot && stance != current);
			}
			return true;
		}
		return true;
	}

	public boolean questAvailable(int id, EnumAvailabilityQuest en, EntityPlayer player) {
		switch (en) {
			case Always: return true;
			case After: return PlayerQuestController.isQuestFinished(player, id);
			case Before: return !PlayerQuestController.isQuestFinished(player, id);
			case Active: return PlayerQuestController.isQuestActive(player, id);
			case NotActive: return !PlayerQuestController.isQuestActive(player, id);
			case Completed: return PlayerQuestController.isQuestCompleted(player, id);
			case CanStart: return PlayerQuestController.canQuestBeAccepted(player, id);
			default: return false;
		}
	}

	public boolean scoreboardAvailable(EntityPlayer player, String objective, EnumAvailabilityScoreboard type, int value) {
		if (!objective.isEmpty()) {
			ScoreObjective sbObjective = player.getWorldScoreboard().getObjective(objective);
			if (sbObjective == null || !player.getWorldScoreboard().entityHasObjective(player.getName(), sbObjective)) { return false; }
			int i = player.getWorldScoreboard().getOrCreateScore(player.getName(), sbObjective).getScorePoints();
			if (type == EnumAvailabilityScoreboard.EQUAL) { return i == value; }
			if (type == EnumAvailabilityScoreboard.BIGGER) { return i > value; }
			return i < value;
		}
		return true;
	}

	public boolean storeddataAvailable(IData dataP, AvailabilityStoredData sd) {
		EnumAvailabilityStoredData type = sd.type;
		Object value = dataP.get(sd.key);
		boolean isNumber = false;
		if (type != EnumAvailabilityStoredData.ONLY && type != EnumAvailabilityStoredData.EXCEPT) {
			if (!(value instanceof Number || value instanceof String)) { return false; }
			try {
				double aV = Double.parseDouble(sd.value);
				double dsV = value instanceof Number ? (double) value : Double.parseDouble((String) value);
				if (type == EnumAvailabilityStoredData.EQUAL && dsV != aV) { return false; }
				if (type == EnumAvailabilityStoredData.BIGGER && dsV < aV) { return false; }
				if (type == EnumAvailabilityStoredData.SMALLER && dsV > aV) { return false; }
				isNumber = true;
			}
			catch (Exception e) { return false; }
		}
		if (!isNumber) {
			return (!dataP.has(sd.key) || type != EnumAvailabilityStoredData.EXCEPT) && (dataP.has(sd.key) || type != EnumAvailabilityStoredData.ONLY);
		}
		return true;
	}

	public boolean moneyAvailable(PlayerGameData gameData, EnumAvailabilityMoney eam, AvailabilityMoneyData data) {
		long value = gameData.getMoney();
		if (eam == EnumAvailabilityMoney.DONAT) { value = gameData.getDonat(); }
		switch (data.type) {
			case SMALLER: if (value > data.value) { return false; } break;
			case BIGGER: if (value < data.value) { return false; } break;
			default: if (value != data.value) { return false; } break;
		}
		return true;
	}

	public boolean stackAvailable(EntityPlayer player, AvailabilityStackData asd, ItemStack parent) {
		if (asd.type != EnumAvailabilityStackData.Always) {
			boolean found = false;
			for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
				ItemStack stack = player.inventory.getStackInSlot(i);
				if (!NoppesUtilServer.isItemStackNull(stack) && NoppesUtilPlayer.compareItems(stack, parent, asd.ignoreDamage, asd.ignoreNBT)) {
					found = true;
					break;
				}
			}
			return (!found || asd.type != EnumAvailabilityStackData.Except) && (found || asd.type != EnumAvailabilityStackData.Contains);
		}
		return true;
	}

	public boolean regionAvailable(EntityPlayer player, EnumAvailabilityRegion aData, Zone3D region) {
		if (aData != EnumAvailabilityRegion.Always) {
			boolean inSide = player.world.provider.getDimension() == region.dimension && region.contains(player.posX, player.posY, player.posZ, player.height);
			return (!inSide || aData != EnumAvailabilityRegion.OutSide) && (inSide || aData != EnumAvailabilityRegion.InSide);
		}
		return true;
	}

	@Override
	public boolean isAvailable(IPlayer<?> player) { return isAvailable(player.getMCEntity()); }

	@Override
	public int[] getDaytime() { return daytime; }

	@Override
	public int getHealth() { return health; }

	@Override
	public int getHealthType() { return healthType; }

	@Override
	public int getMinPlayerLevel() { return minPlayerLevel; }

	@Override
	public String[] getPlayerNames() { return playerNames.keySet().toArray(new String[0]); }

	@Override
	public String getStoredDataValue(String key) {
		for (AvailabilityStoredData sd : storeddata) {
			if (sd.key.equals(key)) { return sd.value; }
		}
		return null;
	}

	@Override
	public int getMoneyValue(int type) {
		if (type < 0) { type *= -1; }
		return moneys.get(EnumAvailabilityMoney.values()[type % EnumAvailabilityMoney.values().length]).value;
	}

	@Override
	public int getVersion() { return version; }

	@Override
	public boolean hasDialog(int id) { return dialogues.containsKey(id); }

	@Override
	public boolean hasFaction(int id) { return factions.containsKey(id); }

	public boolean hasHealth() { return healthType != 0; }

	public boolean hasOptions() { return hasOptions = checkHasOptions(); }

	@Override
	public boolean hasPlayerName(String name) { return playerNames.containsKey(name); }

	@Override
	public boolean hasQuest(int id) { return quests.containsKey(id); }

	@Override
	public boolean hasScoreboard(String objective) {
		if (scoreboards.containsKey(objective)) { return true; }
		for (String obj : scoreboards.keySet()) {
			if (obj.equals(objective)) { return true; }
		}
		return false;
	}

	@Override
	public boolean hasStoredData(String key) {
		for (AvailabilityStoredData sd : storeddata) {
			if (sd.key.equals(key)) { return true; }
		}
		return false;
	}

	@Override
	public boolean hasMoneyData(int type) {
		if (type < 0) { type *= -1; }
		return moneys.containsKey(EnumAvailabilityMoney.values()[type % EnumAvailabilityMoney.values().length]);
	}

	private void initScore(String objective) {
		if (objective != null && !objective.isEmpty() && CustomNpcs.Server != null) {
			Availability.scores.add(objective);
			for (WorldServer world : CustomNpcs.Server.worlds) {
				ServerScoreboard board = (ServerScoreboard) world.getScoreboard();
				ScoreObjective so = board.getObjective(objective);
				if (so != null) {
					Set<ScoreObjective> addedObjectives = ((IServerScoreboardMixin) board).getAddedObjectives();
					if (addedObjectives != null && !addedObjectives.contains(so)) { board.addObjective(so); }
				}
			}
		}
	}

	public void load(NBTTagCompound compound) {
		clear();

		version = compound.getInteger("ModRev");
		VersionCompatibility.CheckAvailabilityCompatibility(this, compound);
		minPlayerLevel = compound.getInteger("AvailabilityMinPlayerLevel");

		if (compound.hasKey("AvailabilityDayTime", 11)) { daytime = compound.getIntArray("AvailabilityDayTime"); }
		else {
			int v = compound.getInteger("AvailabilityDayTime");
			if (v < 0) { v *= -1; }
			if (v >= EnumDayTime.values().length) { v %= EnumDayTime.values().length; }
			switch (EnumDayTime.values()[v]) {
			case Night: {
				daytime[0] = 18;
				daytime[1] = 6;
				break;
			}
			case Day: {
				daytime[0] = 6;
				daytime[1] = 18;
				break;
			}
			default: {
				daytime[0] = 0;
				daytime[1] = 0;
			}
			}
		} // OLD versions

		if (compound.hasKey("AvailabilityDialogs", 9)) {
			for (int d = 0; d < max && d < compound.getTagList("AvailabilityDialogs", 10).tagCount(); d++) {
				NBTTagCompound nbtDialog = compound.getTagList("AvailabilityDialogs", 10).getCompoundTagAt(d);
				int v = nbtDialog.getInteger("Availability");
				if (v < 0) { v *= -1; }
				if (v >= EnumAvailabilityDialog.values().length) { v %= EnumAvailabilityDialog.values().length; }
				dialogues.put(nbtDialog.getInteger("ID"), EnumAvailabilityDialog.values()[v]);
			}
		}
		else if (compound.hasKey("AvailabilityDialogId", 3)) {
			for (int i = 0; i < 4; i++) {
				String key = i == 0 ? "" : "" + (i + 1);
				if (compound.getInteger("AvailabilityDialog" + key + "Id") > 0) {
					int v = compound.getInteger("AvailabilityDialog" + key);
					if (v < 0) { v *= -1; }
					if (v >= EnumAvailabilityDialog.values().length) { v %= EnumAvailabilityDialog.values().length; }
					dialogues.put(compound.getInteger("AvailabilityDialog" + key + "Id"), EnumAvailabilityDialog.values()[v]);
				}
			}
		} // OLD versions

		if (compound.hasKey("AvailabilityQuests", 9)) {
			for (int q = 0; q < max && q < compound.getTagList("AvailabilityQuests", 10).tagCount(); q++) {
				NBTTagCompound nbtQuest = compound.getTagList("AvailabilityQuests", 10).getCompoundTagAt(q);
				int v = nbtQuest.getInteger("Availability");
				if (v < 0) { v *= -1; }
				if (v >= EnumAvailabilityQuest.values().length) { v %= EnumAvailabilityQuest.values().length; }
				quests.put(nbtQuest.getInteger("ID"), EnumAvailabilityQuest.values()[v]);
			}
		}
		else if (compound.hasKey("AvailabilityQuestId", 3)) {
			for (int i = 0; i < 4; i++) {
				String key = i == 0 ? "" : "" + (i + 1);
				if (compound.getInteger("AvailabilityQuest" + key + "Id") > 0) {
					int v = compound.getInteger("AvailabilityQuest" + key);
					if (v < 0) {
						v *= -1;
					}
					if (v >= EnumAvailabilityDialog.values().length) {
						v %= EnumAvailabilityDialog.values().length;
					}
					dialogues.put(compound.getInteger("AvailabilityQuest" + key + "Id"),
							EnumAvailabilityDialog.values()[v]);
				}
			}
		} // OLD versions

		if (compound.hasKey("AvailabilityFactions", 9)) {
			for (int f = 0; f < max && f < compound.getTagList("AvailabilityFactions", 10).tagCount(); f++) {
				NBTTagCompound nbtFaction = compound.getTagList("AvailabilityFactions", 10).getCompoundTagAt(f);
				int v = nbtFaction.getInteger("Stance");
				if (v < 0) { v *= -1; }
				if (v >= EnumAvailabilityFaction.values().length) { v %= EnumAvailabilityFaction.values().length; }
				int g = nbtFaction.getInteger("Availability");
				if (g < 0) { g *= -1; }
				if (g >= EnumAvailabilityFactionType.values().length) { v %= EnumAvailabilityFactionType.values().length; }
				factions.put(nbtFaction.getInteger("ID"), new AvailabilityFactionData(
						EnumAvailabilityFactionType.values()[g], EnumAvailabilityFaction.values()[v]));
			}
		}
		else if (compound.hasKey("AvailabilityFactionId", 3)) {
			for (int i = 0; i < 4; i++) {
				String key = i == 0 ? "" : "2";
				if (compound.getInteger("AvailabilityFaction" + key + "Id") > 0) {
					int v = compound.getInteger("AvailabilityFaction" + key + "Stance");
					if (v < 0) { v *= -1; }
					if (v >= EnumAvailabilityFaction.values().length) { v %= EnumAvailabilityFaction.values().length; }
					int g = compound.getInteger("AvailabilityFaction" + key);
					if (g < 0) { g *= -1; }
					if (g >= EnumAvailabilityFactionType.values().length) { g %= EnumAvailabilityFactionType.values().length; }
					factions.put(compound.getInteger("AvailabilityFaction" + key + "Id"),
							new AvailabilityFactionData(EnumAvailabilityFactionType.values()[g],
									EnumAvailabilityFaction.values()[v]));
				}
			}
		} // OLD versions

		if (compound.hasKey("AvailabilityScoreboards", 9)) {
			for (int s = 0; s < max && s < compound.getTagList("AvailabilityScoreboards", 10).tagCount(); s++) {
				NBTTagCompound nbtScoreboard = compound.getTagList("AvailabilityScoreboards", 10).getCompoundTagAt(s);
				int v = nbtScoreboard.getInteger("Availability");
				if (v < 0) { v *= -1; }
				v %= EnumAvailabilityScoreboard.values().length;
				scoreboards.put(nbtScoreboard.getString("Objective"), new AvailabilityScoreboardData(
						EnumAvailabilityScoreboard.values()[v], nbtScoreboard.getInteger("Value")));
				initScore(nbtScoreboard.getString("Objective"));
			}
		}
		else if (compound.hasKey("AvailabilityScoreboardObjective", 8)) {
			for (int i = 0; i < 2; i++) {
				String key = i == 0 ? "" : "2";
				if (!compound.getString("AvailabilityScoreboard" + key + "Objective").isEmpty()) {
					String objective = compound.getString("AvailabilityScoreboard" + key + "Objective");
					int v = compound.getInteger("AvailabilityScoreboardType" + key);
					if (v < 0) { v *= -1; }
					v %= EnumAvailabilityScoreboard.values().length;
					scoreboards.put(objective,
							new AvailabilityScoreboardData(EnumAvailabilityScoreboard.values()[v],
									compound.getInteger("AvailabilityScoreboard" + key + "Value")));
					initScore(objective);
				}
			}
		} // OLD versions

		if (compound.hasKey("AvailabilityPlayerNames", 9)) {
			for (int s = 0; s < compound.getTagList("AvailabilityPlayerNames", 10).tagCount(); s++) {
				NBTTagCompound nbtName = compound.getTagList("AvailabilityPlayerNames", 10).getCompoundTagAt(s);
				int v = compound.getInteger("Availability");
				if (v < 0) { v *= -1; }
				if (v >= EnumAvailabilityPlayerName.values().length) { v %= EnumAvailabilityPlayerName.values().length; }
				playerNames.put(nbtName.getString("Name"), EnumAvailabilityPlayerName.values()[v]);
			}
		}

		if (compound.hasKey("AvailabilityStoredData", 9)) {
			for (int i = 0; i < compound.getTagList("AvailabilityStoredData", 10).tagCount(); i++) {
				AvailabilityStoredData asd = new AvailabilityStoredData(compound.getTagList("AvailabilityStoredData", 10).getCompoundTagAt(i));
				boolean found = false;
				for (AvailabilityStoredData sd : storeddata) {
					if (sd.key.equals(asd.key)) {
						found = true;
						sd.value = asd.value;
						sd.type = asd.type;
						break;
					}
				}
				if (!found) { storeddata.add(asd); }
			}
		}

		if (compound.hasKey("AvailabilityMoneys", 9)) {
			for (int i = 0; i < compound.getTagList("AvailabilityMoneys", 10).tagCount(); i++) {
				NBTTagCompound nbtMoney = compound.getTagList("AvailabilityMoneys", 10).getCompoundTagAt(i);
				int t = nbtMoney.getInteger("EqualsType");
				if (t < 0) { t *= -1; }
				moneys.put(EnumAvailabilityMoney.values()[t % EnumAvailabilityMoney.values().length], new AvailabilityMoneyData(nbtMoney));
			}
		}

		if (compound.hasKey("AvailabilityHealth", 3)) {
			health = compound.getInteger("AvailabilityHealth");
			if (health < 0) { health = 0; }
			if (health > 100) { health = 100; }
			healthType = compound.getInteger("AvailabilityHealthType");
			if (healthType < 0) { healthType *= -1; }
			if (healthType > 2) { healthType = healthType % 3; }
		}

		onlyGM = compound.getBoolean("OnlyGM");

		stacks.clear();
		if (compound.hasKey("NpcMiscInv", 9)) { stacks.load(compound); }
		stacksData.clear();
		if (compound.hasKey("AvailabilityMiscInv", 9)) {
			for (int i = 0; i < compound.getTagList("AvailabilityMiscInv", 10).tagCount() && i < 9; i++) {
				stacksData.put(i, new AvailabilityStackData(compound.getTagList("AvailabilityMiscInv", 10).getCompoundTagAt(i)));
			}
		}
		for (int i = 0; i < 9; i++) {
			if (stacksData.containsKey(i)) { continue; }
			stacksData.put(i, new AvailabilityStackData());
		}

		hasOptions = checkHasOptions();
	}

	@Override
	public NBTTagCompound save(NBTTagCompound compound) {
		compound.setInteger("ModRev", version);
		compound.setIntArray("AvailabilityDayTime", daytime);
		compound.setInteger("AvailabilityMinPlayerLevel", minPlayerLevel);

		NBTTagList listD = new NBTTagList();
		for (int id : dialogues.keySet()) {
			NBTTagCompound nbtDialog = new NBTTagCompound();
			nbtDialog.setInteger("ID", id);
			nbtDialog.setInteger("Availability", dialogues.get(id).ordinal());
			listD.appendTag(nbtDialog);
		}
		compound.setTag("AvailabilityDialogs", listD);

		NBTTagList listQ = new NBTTagList();
		for (int id : quests.keySet()) {
			NBTTagCompound nbtQuest = new NBTTagCompound();
			nbtQuest.setInteger("ID", id);
			nbtQuest.setInteger("Availability", quests.get(id).ordinal());
			listQ.appendTag(nbtQuest);
		}
		compound.setTag("AvailabilityQuests", listQ);

		NBTTagList listF = new NBTTagList();
		for (int id : factions.keySet()) {
			NBTTagCompound nbtFaction = new NBTTagCompound();
			nbtFaction.setInteger("ID", id);
			nbtFaction.setInteger("Availability", factions.get(id).factionAvailable.ordinal());
			nbtFaction.setInteger("Stance", factions.get(id).factionStance.ordinal());
			listF.appendTag(nbtFaction);
		}
		compound.setTag("AvailabilityFactions", listF);

		NBTTagList listS = new NBTTagList();
		for (String obj : scoreboards.keySet()) {
			NBTTagCompound nbtScoreboard = new NBTTagCompound();
			nbtScoreboard.setString("Objective", obj);
			nbtScoreboard.setInteger("Availability", scoreboards.get(obj).scoreboardType.ordinal());
			nbtScoreboard.setInteger("Value", scoreboards.get(obj).scoreboardValue);
			listS.appendTag(nbtScoreboard);
		}
		compound.setTag("AvailabilityScoreboards", listS);

		NBTTagList listPN = new NBTTagList();
		for (String name : playerNames.keySet()) {
			NBTTagCompound nbtName = new NBTTagCompound();
			nbtName.setString("Name", name);
			nbtName.setInteger("Availability", playerNames.get(name).ordinal());
			listPN.appendTag(nbtName);
		}
		compound.setTag("AvailabilityPlayerNames", listPN);

		NBTTagList listSD = new NBTTagList();
		for (AvailabilityStoredData sd : storeddata) { listSD.appendTag(sd.writeToNBT()); }
		compound.setTag("AvailabilityStoredData", listSD);

		compound.setInteger("AvailabilityHealth", health);
		compound.setInteger("AvailabilityHealthType", healthType);

		compound.setBoolean("OnlyGM", onlyGM);

		compound.setTag("NpcMiscInv", NBTTags.nbtItemStackList(stacks));
		NBTTagList listMI = new NBTTagList();
		for (AvailabilityStackData mi : stacksData.values()) { listMI.appendTag(mi.writeToNBT()); }
		compound.setTag("AvailabilityMiscInv", listMI);

		NBTTagList listM = new NBTTagList();
		for (EnumAvailabilityMoney type : moneys.keySet()) {
			NBTTagCompound nbtMoney = new NBTTagCompound();
			nbtMoney.setInteger("Type", type.ordinal());
			moneys.get(type).save(nbtMoney);
			listM.appendTag(nbtMoney);
		}
		compound.setTag("AvailabilityMoneys", listM);

		return compound;
	}

	@Override
	public void removeDialog(int id) {
		dialogues.remove(id);
		hasOptions = checkHasOptions();
	}

	@Override
	public void removeFaction(int id) {
		factions.remove(id);
		hasOptions = checkHasOptions();
	}

	@Override
	public void removePlayerName(String name) {
		playerNames.remove(name);
		hasOptions = checkHasOptions();
	}

	@Override
	public void removeQuest(int id) {
		quests.remove(id);
		hasOptions = checkHasOptions();
	}

	@Override
	public void removeScoreboard(String objective) {
        scoreboards.remove(objective);
		for (String obj : scoreboards.keySet()) {
			if (obj.equals(objective)) {
				scoreboards.remove(obj);
				return;
			}
		}
	}

	@Override
	public void removeStoredData(String key) {
		for (AvailabilityStoredData sd : storeddata) {
			if (sd.key.equals(key)) {
				storeddata.remove(sd);
				break;
			}
		}
		hasOptions = checkHasOptions();
	}

	@Override
	public void removeMoneyData(int type) {
		if (type < 0) { type *= -1; }
		moneys.remove(EnumAvailabilityMoney.values()[type % EnumAvailabilityMoney.values().length]);
		hasOptions = checkHasOptions();
	}

	@Override
	public void setDaytime(int type) {
		switch (EnumDayTime.values()[MathHelper.clamp(type, 0, 2)]) {
			case Night: {
				daytime[0] = 18;
				daytime[1] = 6;
				break;
			}
			case Day: {
				daytime[0] = 6;
				daytime[1] = 18;
				break;
			}
			default: {
				daytime[0] = 0;
				daytime[1] = 0;
			}
		}
		hasOptions = checkHasOptions();
	}

	@Override
	public void setDaytime(int minHour, int maxHour) {
		daytime[0] = minHour;
		daytime[1] = maxHour;
		hasOptions = checkHasOptions();
	}

	@Override
	public void setDialog(int id, int type) {
		if (dialogues.size() >= max) {
			throw new CustomNPCsException("The maximum number is already set to " + max);
		}
		dialogues.put(id, EnumAvailabilityDialog.values()[ValueUtil.correctInt(type, 0, 2)]);
		hasOptions = checkHasOptions();
	}

	@Override
	public void setFaction(int id, int type, int stance) {
		if (factions.size() >= max) {
			throw new CustomNPCsException("The maximum number is already set to " + max);
		}
		factions.put(id,
				new AvailabilityFactionData(EnumAvailabilityFactionType.values()[ValueUtil.correctInt(type, 0, 2)],
						EnumAvailabilityFaction.values()[ValueUtil.correctInt(stance, 0, 2)]));
		hasOptions = checkHasOptions();
	}

	@Override
	public void setHealth(int value, int type) {
		if (value < 0) { value = 0; }
		if (value > 100) { value = 100; }
		health = value;

		if (type < 0) { type *= -1; }
		if (type > 2) { type = type % 3; }
		healthType = type;
	}

	@Override
	public void setMinPlayerLevel(int level) {
		minPlayerLevel = level;
		hasOptions = checkHasOptions();
	}

	@Override
	public void setPlayerName(String name, int type) {
		if (type < 0) { type *= -1; }
		type %= EnumAvailabilityPlayerName.values().length;
		playerNames.put(name, EnumAvailabilityPlayerName.values()[type]);
		hasOptions = checkHasOptions();
	}

	@Override
	public void setQuest(int id, int type) {
		if (quests.size() >= max) {
			throw new CustomNPCsException("The maximum number is already set to " + max);
		}
		quests.put(id, EnumAvailabilityQuest.values()[ValueUtil.correctInt(type, 0, 6)]);
		hasOptions = checkHasOptions();
	}

	@Override
	public void setScoreboard(String objective, int type, int value) {
		if (scoreboards.size() >= max) {
			throw new CustomNPCsException("The maximum number is already set to " + max);
		}
		if (objective == null || objective.isEmpty()) {
			throw new CustomNPCsException("Objective must not be empty");
		}
		scoreboards.put(objective, new AvailabilityScoreboardData(EnumAvailabilityScoreboard.values()[ValueUtil
				.correctInt(type, 0, EnumAvailabilityScoreboard.values().length - 1)], value));
		hasOptions = checkHasOptions();
	}

	@Override
	public void setStoredData(String key, String value, int type) {
		boolean found = false;
		if (type < 0) { type *= -1; }
		EnumAvailabilityStoredData t = EnumAvailabilityStoredData.values()[type % EnumAvailabilityStoredData.values().length];
		for (AvailabilityStoredData sd : storeddata) {
			if (sd.key.equals(key)) {
				found = true;
				sd.value = value;
				sd.type = t;
				break;
			}
		}
		if (!found) { storeddata.add(new AvailabilityStoredData(key, value, t)); }
		hasOptions = checkHasOptions();
	}

	@Override
	public void setMoneyData(int type, int equal, int value) {
		if (type < 0) { type *= -1; }
		if (equal < 0) { equal *= -1; }
		EnumAvailabilityMoney t = EnumAvailabilityMoney.values()[type % EnumAvailabilityMoney.values().length];
		EnumAvailabilityScoreboard e = EnumAvailabilityScoreboard.values()[equal % EnumAvailabilityScoreboard.values().length];
		if (moneys.containsKey(t)) {
			moneys.get(t).type = e;
			moneys.get(t).value = value;
		}
		else { moneys.put(t, new AvailabilityMoneyData(value, e)); }
		hasOptions = checkHasOptions();
	}

	@Override
	public boolean getGMOnly() { return onlyGM; }

	@Override
	public void setGMOnly(boolean gmOnly) { onlyGM = gmOnly; }

	@Override
	public IItemStack getIItemStack(int slotId) {
		if (slotId < 0 || slotId > 9) { return null; }
		return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stacks.getStackInSlot(slotId));
	}

	@Override
	public IItemStack[] getIItemStacks() {
		List<IItemStack> list = new ArrayList<>();
		for (int i = 0; i < stacks.getSizeInventory(); i++) {
			list.add(Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stacks.getStackInSlot(i)));
		}
		return list.toArray(new IItemStack[0]);
	}

	@Override
	public void setIItemStack(int slotId, IItemStack item) {
		if (slotId < 0 || slotId > 9) { return; }
		stacks.setInventorySlotContents(slotId, item.getMCItemStack());
	}

	@Override
	public void removeIItemStack(int slotId) {
		if (slotId < 0 || slotId > 9) { return; }
		stacks.setInventorySlotContents(slotId, ItemStack.EMPTY);
	}

	@Override
	public void setVersion(int versionIn) { version = versionIn; }

	public String toString() {
		int st = 0;
		for (int i = 0; i < stacks.getSizeInventory(); i++) {
			if (!NoppesUtilServer.isItemStackNull(stacks.getStackInSlot(i))) { st++;}
		}
		return "Availability hasOptions: " + hasOptions + ", maxData: " + max + ", { scoreboards:"
				+ scoreboards.size() + ", dialogues:" + dialogues.size() + ", quests:" + quests.size()
				+ ", factions:" + factions.size() + ", time[min:" + daytime[0] + ", max:" + daytime[0]
				+ "]" + ", playerNames:" + playerNames.size() + ", StoredDatas:" + storeddata.size()
				+ ", ItemStacks:" + st + ", Regions:" + regions.size()
				+ ", playerData[Lv:" + minPlayerLevel + ", H:" + health + ", HT:" + healthType
				+ ", moneys:" + moneys.size() + "] }";
	}

	public List<Component> getAvailability(EntityPlayer player, Component titleType) {
		List<Component> list = new ArrayList<>();
		if (!hasOptions || player == null) { return list; }
		Component title = Component.translatable("availability.options");
		if (titleType != null) { title.append(titleType); }
		list.add(title.append(":"));
        boolean gm = player.isCreative();
		// daytime
		if (daytime[0] >= 0 && daytime[0] <= 23 && daytime[1] >= 0 && daytime[1] <= 23 && daytime[0] != daytime[1]) {
			int time = (int) ((player.world.getWorldTime() + 30000L) % 24000L) / 1000;
			boolean bo;
			if (daytime[0] < daytime[1]) { bo = time >= daytime[0] && time <= daytime[1]; }
			else { bo = time >= daytime[0] || time <= daytime[1]; }
			boolean hasClock = false;
			if (player.capabilities.isCreativeMode) { hasClock = true; }
			else {
				for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
					ItemStack stack = player.inventory.getStackInSlot(i);
					if (!NoppesUtilServer.isItemStackNull(stack) && stack.getItem() instanceof ItemClock) {
						hasClock = true;
						break;
					}
				}
			}
			if (hasClock) {
				list.add(Component.translatable("availability.type.daytime.1",
						Component.literal(daytime[0]+":00").withStyle(TextFormatting.DARK_GREEN),
						Component.literal(daytime[1]+":00").withStyle(TextFormatting.DARK_GREEN),
						Component.literal(time+":00").withStyle(TextFormatting.GOLD), " " +
								Component.translatable("quest.task.manual."+(bo ? "0" : "1")).getFormattedText()));
			} else {
				list.add(Component.translatable("availability.type.daytime.0", " " +
						Component.translatable("quest.task.manual."+(bo ? "0" : "1")).getFormattedText()));
			}
		}
		// dialogue
		Component data;
		if (!dialogues.isEmpty()) {
			data = Component.empty();
			for (int id : dialogues.keySet()) {
				if (dialogues.get(id) != EnumAvailabilityDialog.Always) {continue; }
				IDialog d = DialogController.instance.get(id);
				data.append(" ")
						.append(Component.translatable("availability." + dialogues.get(id).name().toLowerCase()))
						.append(" ");
				if (d == null || gm) {
					data.append(Component.literal( "ID: ").withStyle(TextFormatting.GRAY))
							.append(Component.literal( "" + id).withStyle(TextFormatting.GOLD))
							.append(Component.literal( gm ? " - " : "").withStyle(TextFormatting.RESET));
				}
				if (d != null) { data.append(Component.translatable(d.getName())); }
				data.append(" ")
						.append(Component.translatable("quest.task.manual."+(dialogAvailable(id, dialogues.get(id), player) ? "0" : "1")));
			}
			if (!data.getFormattedText().isEmpty()) { list.add(Component.translatable("availability.type.dialogues").append(data)); }
		}
		// quests
		if (!quests.isEmpty()) {
			data = Component.empty();
			for (int id : quests.keySet()) {
				if (quests.get(id) != EnumAvailabilityQuest.Always) { continue; }
				IQuest q = QuestController.instance.get(id);
				data.append(" ");
				if (q == null || gm) {
					data.append(Component.literal( "ID: ").withStyle(TextFormatting.GRAY))
							.append(Component.literal( "" + id).withStyle(TextFormatting.GOLD))
							.append(Component.literal( gm ? " - " : "").withStyle(TextFormatting.RESET));
				}
				data.append(Component.translatable("availability." + quests.get(id).name().toLowerCase()))
						.append(" ");
				if (q != null) { data.append(q.getTitle()); }
				data.append(" ")
						.append(Component.translatable("quest.task.manual."+(questAvailable(id, quests.get(id), player) ? "0" : "1")));
			}
			if (!data.getFormattedText().isEmpty()) { list.add(Component.translatable("availability.type.quests").append(data)); }
		}
		// factions
		if (!factions.isEmpty()) {
			data = Component.empty();
			for (int id : factions.keySet()) {
				if (factions.get(id).factionAvailable == EnumAvailabilityFactionType.Always) { continue; }
				IFaction f = FactionController.instance.get(id);
				data.append(" ");
				if (f == null || gm) {
					data.append(Component.literal( "ID: ").withStyle(TextFormatting.GRAY))
							.append(Component.literal( "" + id).withStyle(TextFormatting.GOLD))
							.append(Component.literal( gm ? " - " : "").withStyle(TextFormatting.RESET));
				}
				data.append(Component.translatable("availability." + factions.get(id).factionAvailable.name().toLowerCase()))
						.append(" ");
				String attitude = factions.get(id).factionStance == EnumAvailabilityFaction.Hostile ? "aggressive": factions.get(id).factionAvailable.name().toLowerCase();
				data.append(Component.translatable("faction.name." + attitude))
						.append(" ");
				if (f != null) { data.append(f.getName()); }
				data.append(" ")
						.append(Component.translatable("quest.task.manual."+(factionAvailable(id, factions.get(id).factionStance, factions.get(id).factionAvailable, player) ? "0" : "1")));
			}
			if (!data.getFormattedText().isEmpty()) { list.add(Component.translatable("availability.type.factions").append(data)); }
		}
		// scoreboards
		if (!scoreboards.isEmpty()) {
			data = Component.empty();
			for (String obj : scoreboards.keySet()) {
				data.append(" ")
						.append(Component.translatable("gui.name")).append(": ").append(obj)
						.append(Component.translatable("availability." + scoreboards.get(obj).scoreboardType.name().toLowerCase()))
						.append(" ")
						.append(String.valueOf(scoreboards.get(obj).scoreboardValue))
						.append(" ")
						.append(Component.translatable("quest.task.manual."+(scoreboardAvailable(player, obj, scoreboards.get(obj).scoreboardType, scoreboards.get(obj).scoreboardValue) ? "0" : "1")));
			}
			if (!data.getFormattedText().isEmpty()) { list.add(Component.translatable("availability.type.scoreboards").append(data)); }
		}
		// player names
		if (!playerNames.isEmpty()) {
			data = Component.empty();
			List<String> listOnly = new ArrayList<>();
			List<String> listExcept = new ArrayList<>();
			for (String name : playerNames.keySet()) {
				switch (playerNames.get(name)) {
					case Only: {
						listOnly.add(name);
						break;
					}
					case Except: {
						listExcept.add(name);
						break;
					}
				}
			}
			if (!listOnly.isEmpty() || !listExcept.isEmpty()) { data.append(" "); }
			if (!listOnly.isEmpty()) {
				data.append(Component.translatable("availability.only")).append("[");
				boolean st = true;
				for (String name : listOnly) {
					if (!st) { data.append("; "); } else { st = false; }
					data.append(name);
				}
				data.append("] ")
						.append(Component.translatable("quest.task.manual."+(listOnly.contains(player.getName()) ? "0" : "1")));
			}
			if (!listExcept.isEmpty()) {
				data.append(Component.translatable("availability.except")).append("[");
				boolean st = true;
				for (String name : listExcept) {
					if (!st) { data.append("; "); } else { st = false; }
					data.append(name);
				}
				data.append("] ")
						.append(Component.translatable("quest.task.manual."+(listExcept.contains(player.getName()) ? "0" : "1")));
			}
			if (!data.getFormattedText().isEmpty()) { list.add(Component.translatable("availability.type.player.names").append(data)); }
		}
		// storeddata
		if (!storeddata.isEmpty()) {
			data = Component.empty();
			IData dataP = Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player).getStoreddata();
			for (AvailabilityStoredData sd : storeddata) {
				EnumAvailabilityStoredData type = sd.type;
				Object value = dataP.get(sd.key);
				boolean isNumber = false;
				boolean bo = true;
				if (type != EnumAvailabilityStoredData.ONLY && type != EnumAvailabilityStoredData.EXCEPT) {
					if (!(value instanceof Number || value instanceof String)) { bo = false; }
					try {
						double aV = Double.parseDouble(sd.value);
						double dsV = value instanceof Number ? (double) value : Double.parseDouble((String) value);
						if (type == EnumAvailabilityStoredData.EQUAL && dsV != aV) { bo = false; }
						if (type == EnumAvailabilityStoredData.BIGGER && dsV < aV) { bo = false; }
						if (type == EnumAvailabilityStoredData.SMALLER && dsV > aV) { bo = false; }
						isNumber = true;
					}
					catch (Exception e) { bo = false; }
				}
				if (!isNumber) {
					if ((dataP.has(sd.key) && type == EnumAvailabilityStoredData.EXCEPT) || (!dataP.has(sd.key) && type == EnumAvailabilityStoredData.ONLY)) { bo = false; }
				}
				data.append(" ")
						.append(Component.translatable("gui.name"))
						.append(": ")
						.append(sd.key)
						.append(" ")
						.append(Component.translatable("quest.task.item."+(bo ? "0" : "1")));
			}
			if (!data.getFormattedText().isEmpty()) { list.add(Component.translatable("availability.type.storeddata").append(data)); }
		}
		// moneys
		if (!moneys.isEmpty()) {
			data = Component.empty();
			PlayerGameData gameData = PlayerData.get(player).game;
			for (EnumAvailabilityMoney eam : new ArrayList<>(moneys.keySet())) {
				long value = gameData.getMoney();
				if (eam == EnumAvailabilityMoney.DONAT) { value = gameData.getDonat(); }
				AvailabilityMoneyData money = moneys.get(eam);
				boolean bo;
				switch (money.type) {
					case SMALLER: bo = value > money.value; break;
					case BIGGER: bo = value < money.value; break;
					default: bo = value != money.value; break;
				}
				data.append(" ")
						.append(Component.translatable("gui.name"))
						.append(": ")
						.append(Component.translatable("gui." + eam.name().toLowerCase()))
						.append(" ")
						.append(Component.translatable("quest.task.item."+(bo ? "1" : "0")));
			}
			if (!data.getFormattedText().isEmpty()) { list.add(Component.translatable("availability.type.moneys").append(data)); }
		}
		// stacks
		if (!stacks.isEmpty()) {
			data = Component.empty();
			for (int i = 0; i < stacks.getSizeInventory(); i++) {
				ItemStack stack = stacks.getStackInSlot(i);
				if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
				data.append(" ")
						.append(stack.getDisplayName())
						.append(" x" + stack.getCount());
			}
			if (!data.getFormattedText().isEmpty()) { list.add(Component.translatable("availability.type.stacks").append(data)); }
		}
		// health
		if (healthType != 0) {
			int h = (int) (player.getHealth() / player.getMaxHealth() * 100);
			data = Component.empty().append(" ")
					.append(Component.translatable("availability." + (healthType == 1 ? "smaller" : "bigger")))
					.append(" " + h + "% ")
					.append(Component.translatable("quest.task.item."+((healthType == 1 && h < health) || (healthType == 2 && h > health) ? "1" : "0")));
			list.add(Component.translatable("availability.type.health").append(data));
		}
		// in creative mode
		if (onlyGM) {
			data = Component.empty().append(": ")
					.append(Component.translatable("gui.enabled"))
					.append(" ")
					.append(Component.translatable("quest.task.manual."+(gm ? "0" : "1")));
			list.add(Component.translatable("availability.type.only.gm.true").append(data));
		}
		// xp level
		if (minPlayerLevel > 0) {
			data = Component.empty().append(" ")
					.append(Component.translatable("availability.bigger"))
					.append(" " + minPlayerLevel + " ")
					.append(Component.translatable("quest.task.manual."+(player.experienceLevel >= minPlayerLevel ? "0" : "1")));
			list.add(Component.translatable("availability.type.level", data.toString()));
		}
		return list;
	}

}
