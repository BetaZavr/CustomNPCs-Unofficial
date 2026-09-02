package noppes.npcs.controllers.data;

import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.Objective;
import noppes.npcs.*;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.ICompatibilty;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.handler.data.IAvailability;
import noppes.npcs.api.handler.data.IDialog;
import noppes.npcs.api.handler.data.IFaction;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.*;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.controllers.*;
import noppes.npcs.mixin.server.IServerScoreboardMixin;
import noppes.npcs.mixin.world.ISimpleContainerMixin;
import noppes.npcs.util.ValueUtil;

// Change from Unofficial (BetaZavr)
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
      for (int i = 0; i < stacks.getContainerSize(); i++) {
         if (!NoppesUtilServer.isItemStackNull(stacks.getItem(i))) { return true; }
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

   public boolean isAvailable(Player player) {
      if (!hasOptions) { return true; }
      if (daytime[0] >= 0 && daytime[0] <= 23 && daytime[1] >= 0 && daytime[1] <= 23 && daytime[0] != daytime[1]) {
         int time = (int) ((player.level().getDayTime() + 30000L) % 24000L) / 1000;
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
         if (!factionAvailable(id, factions.get(id).factionStance, factions.get(id).factionAvailable,  player)) { return false; }
      }
      for (String obj : scoreboards.keySet()) {
         if (!scoreboardAvailable(player, obj, scoreboards.get(obj).scoreboardType,  scoreboards.get(obj).scoreboardValue)) { return false; }
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
         if (!stackAvailable(player, stacksData.get(pos), stacks.getItem(pos))) { return false; }
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
               if (player.getName().getString().equals(name)) {
                  hasOnly = false;
                  exit = true;
               }
               break;
            }
            case Except: {
               if (player.getName().getString().equals(name)) {
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
      if (onlyGM && !player.isCreative()) { return false; }
      return player.experienceLevel >= minPlayerLevel;
   }

   public boolean dialogAvailable(int id, EnumAvailabilityDialog en, Player player) {
      if (en != EnumAvailabilityDialog.Always) {
         boolean hasRead = PlayerData.get(player).dialogData.has(id);
         return (hasRead && en == EnumAvailabilityDialog.After) || (!hasRead && en == EnumAvailabilityDialog.Before);
      }
      return true;
   }

   public boolean factionAvailable(int id, EnumAvailabilityFaction stance, EnumAvailabilityFactionType available, Player player) {
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

   public boolean questAvailable(int id, EnumAvailabilityQuest en, Player player) {
      return switch (en) {
         case Always -> true;
         case After -> PlayerQuestController.isQuestFinished(player, id);
         case Before -> !PlayerQuestController.isQuestFinished(player, id);
         case Active -> PlayerQuestController.isQuestActive(player, id);
         case NotActive -> !PlayerQuestController.isQuestActive(player, id);
         case Completed -> PlayerQuestController.isQuestCompleted(player, id);
         case CanStart -> PlayerQuestController.canQuestBeAccepted(player, id);
      };
   }

   public boolean scoreboardAvailable(Player player, String objective, EnumAvailabilityScoreboard type, int value) {
      if (!objective.isEmpty()) {
         Objective sbObjective = player.getScoreboard().getObjective(objective);
         if (sbObjective == null || !player.getScoreboard().hasPlayerScore(player.getName().getString(), sbObjective)) { return false; }
         int i = player.getScoreboard().getOrCreatePlayerScore(player.getName().getString(), sbObjective).getScore();
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
         case SMALLER -> {
            if (value > data.value) { return false; }
         }
         case BIGGER -> {
            if (value < data.value) { return false; }
         }
         default -> {
            if (value != data.value) { return false; }
         }
      }
      return true;
   }

   public boolean stackAvailable(Player player, AvailabilityStackData asd, ItemStack parent) {
      if (asd.type != EnumAvailabilityStackData.Always) {
         boolean found = false;
         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!NoppesUtilServer.isItemStackNull(stack) && NoppesUtilPlayer.compareItems(stack, parent, asd.ignoreDamage, asd.ignoreNBT)) {
               found = true;
               break;
            }
         }
         return (!found || asd.type != EnumAvailabilityStackData.Except) && (found || asd.type != EnumAvailabilityStackData.Contains);
      }
      return true;
   }

   public boolean regionAvailable(Player player, EnumAvailabilityRegion aData, Zone3D region) {
      if (aData != EnumAvailabilityRegion.Always) {
         boolean inSide = player.level().dimension().location().equals(region.dimension) && region.contains(player.getX(), player.getY(), player.getZ(), player.getBbHeight());
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
      if (objective != null && objective.isEmpty() && CustomNpcs.Server != null) {
         Availability.scores.add(objective);
         for (ServerLevel level: CustomNpcs.Server.getAllLevels()) {
            ServerScoreboard board = level.getScoreboard();
            Objective so = board.getObjective(objective);
            if (so != null) {
               Set<Objective> addedObjectives = ((IServerScoreboardMixin) board).getTrackedObjectives();
               if (addedObjectives != null && !addedObjectives.contains(so)) {
                  board.addObjective(so.getName(), so.getCriteria(), so.getDisplayName(), so.getRenderType());
               }
            }
         }
      }
   }

   public void load(CompoundTag compound) {
      clear();

      version = compound.getInt("ModRev");
      VersionCompatibility.CheckAvailabilityCompatibility(this, compound);
      minPlayerLevel = compound.getInt("AvailabilityMinPlayerLevel");

      if (compound.contains("AvailabilityDayTime", Tag.TAG_INT_ARRAY)) {
         daytime = compound.getIntArray("AvailabilityDayTime");
      }
      else {
         int v = compound.getInt("AvailabilityDayTime");
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

      if (compound.contains("AvailabilityDialogs", Tag.TAG_LIST)) {
         for (int d = 0; d < max && d < compound.getList("AvailabilityDialogs", 10).size(); d++) {
            CompoundTag nbtDialog = compound.getList("AvailabilityDialogs", 10).getCompound(d);
            int v = nbtDialog.getInt("Availability");
            if (v < 0) { v *= -1; }
            if (v >= EnumAvailabilityDialog.values().length) { v %= EnumAvailabilityDialog.values().length; }
            dialogues.put(nbtDialog.getInt("ID"), EnumAvailabilityDialog.values()[v]);
         }
      }
      else if (compound.contains("AvailabilityDialogId", Tag.TAG_INT)) {
         for (int i = 0; i < 4; i++) {
            String key = i == 0 ? "" : "" + (i + 1);
            if (compound.getInt("AvailabilityDialog" + key + "Id") > 0) {
               int v = compound.getInt("AvailabilityDialog" + key);
               if (v < 0) { v *= -1; }
               if (v >= EnumAvailabilityDialog.values().length) { v %= EnumAvailabilityDialog.values().length; }
               dialogues.put(compound.getInt("AvailabilityDialog" + key + "Id"), EnumAvailabilityDialog.values()[v]);
            }
         }
      } // OLD versions

      if (compound.contains("AvailabilityQuests", Tag.TAG_LIST)) {
         for (int q = 0; q < max && q < compound.getList("AvailabilityQuests", 10).size(); q++) {
            CompoundTag nbtQuest = compound.getList("AvailabilityQuests", 10).getCompound(q);
            int v = nbtQuest.getInt("Availability");
            if (v < 0) { v *= -1; }
            if (v >= EnumAvailabilityQuest.values().length) { v %= EnumAvailabilityQuest.values().length; }
            quests.put(nbtQuest.getInt("ID"), EnumAvailabilityQuest.values()[v]);
         }
      }
      else if (compound.contains("AvailabilityQuestId", Tag.TAG_INT)) {
         for (int i = 0; i < 4; i++) {
            String key = i == 0 ? "" : "" + (i + 1);
            if (compound.getInt("AvailabilityQuest" + key + "Id") > 0) {
               int v = compound.getInt("AvailabilityQuest" + key);
               if (v < 0) { v *= -1; }
               if (v >= EnumAvailabilityDialog.values().length) { v %= EnumAvailabilityDialog.values().length; }
               dialogues.put(compound.getInt("AvailabilityQuest" + key + "Id"), EnumAvailabilityDialog.values()[v]);
            }
         }
      } // OLD versions

      if (compound.contains("AvailabilityFactions", Tag.TAG_LIST)) {
         for (int f = 0; f < max && f < compound.getList("AvailabilityFactions", 10).size(); f++) {
            CompoundTag nbtFaction = compound.getList("AvailabilityFactions", 10).getCompound(f);
            int v = nbtFaction.getInt("Stance");
            if (v < 0) { v *= -1; }
            if (v >= EnumAvailabilityFaction.values().length) { v %= EnumAvailabilityFaction.values().length; }
            int g = nbtFaction.getInt("Availability");
            if (g < 0) { g *= -1; }
            if (g >= EnumAvailabilityFactionType.values().length) { v %= EnumAvailabilityFactionType.values().length; }
            factions.put(nbtFaction.getInt("ID"), new AvailabilityFactionData(EnumAvailabilityFactionType.values()[g], EnumAvailabilityFaction.values()[v]));
         }
      }
      else if (compound.contains("AvailabilityFactionId", Tag.TAG_INT)) {
         for (int i = 0; i < 4; i++) {
            String key = i == 0 ? "" : "2";
            if (compound.getInt("AvailabilityFaction" + key + "Id") > 0) {
               int v = compound.getInt("AvailabilityFaction" + key + "Stance");
               if (v < 0) { v *= -1; }
               if (v >= EnumAvailabilityFaction.values().length) { v %= EnumAvailabilityFaction.values().length; }
               int g = compound.getInt("AvailabilityFaction" + key);
               if (g < 0) { g *= -1; }
               if (g >= EnumAvailabilityFactionType.values().length) { g %= EnumAvailabilityFactionType.values().length; }
               factions.put(compound.getInt("AvailabilityFaction" + key + "Id"),
                       new AvailabilityFactionData(EnumAvailabilityFactionType.values()[g], EnumAvailabilityFaction.values()[v]));
            }
         }
      } // OLD versions

      if (compound.contains("AvailabilityScoreboards", Tag.TAG_LIST)) {
         for (int s = 0; s < max && s < compound.getList("AvailabilityScoreboards", 10).size(); s++) {
            CompoundTag nbtScoreboard = compound.getList("AvailabilityScoreboards", 10).getCompound(s);
            int v = nbtScoreboard.getInt("Availability");
            if (v < 0) { v *= -1; }
            v %= EnumAvailabilityScoreboard.values().length;
            scoreboards.put(nbtScoreboard.getString("Objective"), new AvailabilityScoreboardData(
                    EnumAvailabilityScoreboard.values()[v], nbtScoreboard.getInt("Value")));
            initScore(nbtScoreboard.getString("Objective"));
         }
      }
      else if (compound.contains("AvailabilityScoreboardObjective", Tag.TAG_STRING)) {
         for (int i = 0; i < 2; i++) {
            String key = i == 0 ? "" : "2";
            if (!compound.getString("AvailabilityScoreboard" + key + "Objective").isEmpty()) {
               String objective = compound.getString("AvailabilityScoreboard" + key + "Objective");
               int v = compound.getInt("AvailabilityScoreboardType" + key);
               if (v < 0) { v *= -1; }
               v %= EnumAvailabilityScoreboard.values().length;
               scoreboards.put(objective,
                       new AvailabilityScoreboardData(EnumAvailabilityScoreboard.values()[v], compound.getInt("AvailabilityScoreboard" + key + "Value")));
               initScore(objective);
            }
         }
      } // OLD versions

      if (compound.contains("AvailabilityPlayerNames", Tag.TAG_LIST)) {
         for (int s = 0; s < compound.getList("AvailabilityPlayerNames", 10).size(); s++) {
            CompoundTag nbtName = compound.getList("AvailabilityPlayerNames", 10).getCompound(s);
            int v = compound.getInt("Availability");
            if (v < 0) { v *= -1; }
            if (v >= EnumAvailabilityPlayerName.values().length) { v %= EnumAvailabilityPlayerName.values().length; }
            playerNames.put(nbtName.getString("Name"), EnumAvailabilityPlayerName.values()[v]);
         }
      }

      if (compound.contains("AvailabilityStoredData", Tag.TAG_LIST)) {
         for (int i = 0; i < compound.getList("AvailabilityStoredData", 10).size(); i++) {
            AvailabilityStoredData asd = new AvailabilityStoredData(compound.getList("AvailabilityStoredData", 10).getCompound(i));
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

      if (compound.contains("AvailabilityMoneys", Tag.TAG_LIST)) {
         for (int i = 0; i < compound.getList("AvailabilityMoneys", 10).size(); i++) {
            CompoundTag nbtMoney = compound.getList("AvailabilityMoneys", 10).getCompound(i);
            int t = nbtMoney.getInt("EqualsType");
            if (t < 0) { t *= -1; }
            moneys.put(EnumAvailabilityMoney.values()[t % EnumAvailabilityMoney.values().length], new AvailabilityMoneyData(nbtMoney));
         }
      }

      if (compound.contains("AvailabilityHealth", Tag.TAG_INT)) {
         health = compound.getInt("AvailabilityHealth");
         if (health < 0) { health = 0; }
         if (health > 100) { health = 100; }
         healthType = compound.getInt("AvailabilityHealthType");
         if (healthType < 0) { healthType *= -1; }
         if (healthType > 2) { healthType = healthType % 3; }
      }

      onlyGM = compound.getBoolean("OnlyGM");

      stacks.clearContent();
      if (compound.contains("NpcMiscInv", Tag.TAG_LIST)) { stacks.load(compound); }
      stacksData.clear();
      if (compound.contains("AvailabilityMiscInv", Tag.TAG_LIST)) {
         for (int i = 0; i < compound.getList("AvailabilityMiscInv", 10).size() && i < 9; i++) {
            stacksData.put(i, new AvailabilityStackData(compound.getList("AvailabilityMiscInv", 10).getCompound(i)));
         }
      }
      for (int i = 0; i < 9; i++) {
         if (stacksData.containsKey(i)) { continue; }
         stacksData.put(i, new AvailabilityStackData());
      }

      hasOptions = checkHasOptions();
   }

   @Override
   public CompoundTag save(CompoundTag compound) {
      compound.putInt("ModRev", version);
      compound.putIntArray("AvailabilityDayTime", daytime);
      compound.putInt("AvailabilityMinPlayerLevel", minPlayerLevel);

      ListTag listD = new ListTag();
      for (int id : dialogues.keySet()) {
         CompoundTag nbtDialog = new CompoundTag();
         nbtDialog.putInt("ID", id);
         nbtDialog.putInt("Availability", dialogues.get(id).ordinal());
         listD.add(nbtDialog);
      }
      compound.put("AvailabilityDialogs", listD);

      ListTag listQ = new ListTag();
      for (int id : quests.keySet()) {
         CompoundTag nbtQuest = new CompoundTag();
         nbtQuest.putInt("ID", id);
         nbtQuest.putInt("Availability", quests.get(id).ordinal());
         listQ.add(nbtQuest);
      }
      compound.put("AvailabilityQuests", listQ);

      ListTag listF = new ListTag();
      for (int id : factions.keySet()) {
         CompoundTag nbtFaction = new CompoundTag();
         nbtFaction.putInt("ID", id);
         nbtFaction.putInt("Availability", factions.get(id).factionAvailable.ordinal());
         nbtFaction.putInt("Stance", factions.get(id).factionStance.ordinal());
         listF.add(nbtFaction);
      }
      compound.put("AvailabilityFactions", listF);

      ListTag listS = new ListTag();
      for (String obj : scoreboards.keySet()) {
         CompoundTag nbtScoreboard = new CompoundTag();
         nbtScoreboard.putString("Objective", obj);
         nbtScoreboard.putInt("Availability", scoreboards.get(obj).scoreboardType.ordinal());
         nbtScoreboard.putInt("Value", scoreboards.get(obj).scoreboardValue);
         listS.add(nbtScoreboard);
      }
      compound.put("AvailabilityScoreboards", listS);

      ListTag listPN = new ListTag();
      for (String name : playerNames.keySet()) {
         CompoundTag nbtName = new CompoundTag();
         nbtName.putString("Name", name);
         nbtName.putInt("Availability", playerNames.get(name).ordinal());
         listPN.add(nbtName);
      }
      compound.put("AvailabilityPlayerNames", listPN);

      ListTag listSD = new ListTag();
      for (AvailabilityStoredData sd : storeddata) { listSD.add(sd.save()); }
      compound.put("AvailabilityStoredData", listSD);

      compound.putInt("AvailabilityHealth", health);
      compound.putInt("AvailabilityHealthType", healthType);

      compound.putBoolean("OnlyGM", onlyGM);

      compound.put("NpcMiscInv", NBTTags.nbtItemStackList(((ISimpleContainerMixin) stacks).getItems()));
      ListTag listMI = new ListTag();
      for (AvailabilityStackData mi : stacksData.values()) { listMI.add(mi.save()); }
      compound.put("AvailabilityMiscInv", listMI);

      ListTag listM = new ListTag();
      for (EnumAvailabilityMoney type : moneys.keySet()) {
         CompoundTag nbtMoney = new CompoundTag();
         nbtMoney.putInt("Type", type.ordinal());
         moneys.get(type).save(nbtMoney);
         listM.add(nbtMoney);
      }
      compound.put("AvailabilityMoneys", listM);

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
      switch (EnumDayTime.values()[ValueUtil.correctInt(type, 0, 2)]) {
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
      return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stacks.getItem(slotId));
   }

   @Override
   public IItemStack[] getIItemStacks() {
      List<IItemStack> list = new ArrayList<>();
      for (int i = 0; i < stacks.getContainerSize(); i++) {
         list.add(Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stacks.getItem(i)));
      }
      return list.toArray(new IItemStack[0]);
   }

   @Override
   public void setIItemStack(int slotId, IItemStack item) {
      if (slotId < 0 || slotId > 9) { return; }
      stacks.setItem(slotId, item.getMCItemStack());
   }

   @Override
   public void removeIItemStack(int slotId) {
      if (slotId < 0 || slotId > 9) { return; }
      stacks.setItem(slotId, ItemStack.EMPTY);
   }

   @Override
   public void setVersion(int versionIn) { version = versionIn; }

   public String toString() {
      int st = 0;
      for (int i = 0; i < stacks.getContainerSize(); i++) {
         if (!NoppesUtilServer.isItemStackNull(stacks.getItem(i))) { st++;}
      }
      return "Availability hasOptions: " + hasOptions + ", maxData: " + max + ", { scoreboards:"
              + scoreboards.size() + ", dialogues:" + dialogues.size() + ", quests:" + quests.size()
              + ", factions:" + factions.size() + ", time[min:" + daytime[0] + ", max:" + daytime[0]
              + "]" + ", playerNames:" + playerNames.size() + ", StoredDatas:" + storeddata.size()
              + ", ItemStacks:" + st + ", Regions:" + regions.size()
              + ", playerData[Lv:" + minPlayerLevel + ", H:" + health + ", HT:" + healthType
              + ", moneys:" + moneys.size() + "] }";
   }

   public List<Component> getAvailability(Player player, Component titleType) {
      List<Component> list = new ArrayList<>();
      if (!hasOptions || player == null) { return list; }
      MutableComponent title = Component.translatable("availability.options");
      if (titleType != null) { title.append(titleType); }
      list.add(title.append(":"));
      boolean gm = player.isCreative();
      // daytime
      if (daytime[0] >= 0 && daytime[0] <= 23 && daytime[1] >= 0 && daytime[1] <= 23 && daytime[0] != daytime[1]) {
         int time = (int) ((player.level().getDayTime() + 30000L) % 24000L) / 1000;
         boolean bo;
         if (daytime[0] < daytime[1]) { bo = time >= daytime[0] && time <= daytime[1]; }
         else { bo = time >= daytime[0] || time <= daytime[1]; }
         boolean hasClock = false;
         if (gm) { hasClock = true; }
         else {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
               ItemStack stack = player.getInventory().getItem(i);
               if (!NoppesUtilServer.isItemStackNull(stack) && stack.getItem() == Items.CLOCK) {
                  hasClock = true;
                  break;
               }
            }
         }
         if (hasClock) {
            list.add(Component.translatable("availability.type.daytime.1",
                    Component.literal(daytime[0]+":00").withStyle(ChatFormatting.DARK_GREEN),
                    Component.literal(daytime[1]+":00").withStyle(ChatFormatting.DARK_GREEN),
                    Component.literal(time+":00").withStyle(ChatFormatting.GOLD),
                    Component.translatable("quest.task.manual."+(bo ? "0" : "1"))));
         } else {
            list.add(Component.translatable("availability.type.daytime.0",
                    Component.translatable("quest.task.manual."+(bo ? "0" : "1"))));
         }
      }
      // dialogue
      MutableComponent data;
      if (!dialogues.isEmpty()) {
         data = Component.empty();
         for (int id : dialogues.keySet()) {
            if (dialogues.get(id) != EnumAvailabilityDialog.Always) {continue; }
            IDialog d = DialogController.instance.get(id);
            data.append(" ")
                    .append(Component.translatable("availability." + dialogues.get(id).name().toLowerCase()))
                    .append(" ");
            if (d == null || gm) {
               data.append(Component.literal( "ID: ").withStyle(ChatFormatting.GRAY))
                       .append(Component.literal( "" + id).withStyle(ChatFormatting.GOLD))
                       .append(Component.literal( gm ? " - " : "").withStyle(ChatFormatting.RESET));
            }
            if (d != null) { data.append(Component.translatable(d.getName())); }
            data.append(Component.translatable("quest.task.manual."+(dialogAvailable(id, dialogues.get(id), player) ? "0" : "1")));
         }
         if (!data.getString().isEmpty()) { list.add(Component.translatable("availability.type.dialogues").append(data)); }
      }
      // quests
      if (!quests.isEmpty()) {
         data = Component.empty();
         for (int id : quests.keySet()) {
            if (quests.get(id) != EnumAvailabilityQuest.Always) { continue; }
            IQuest q = QuestController.instance.get(id);
            data.append(" ");
            if (q == null || gm) {
               data.append(Component.literal( "ID: ").withStyle(ChatFormatting.GRAY))
                       .append(Component.literal( "" + id).withStyle(ChatFormatting.GOLD))
                       .append(Component.literal( gm ? " - " : "").withStyle(ChatFormatting.RESET));
            }
            data.append(Component.translatable("availability." + quests.get(id).name().toLowerCase()))
                    .append(" ");
            if (q != null) { data.append(q.getTitle()); }
            data.append(Component.translatable("quest.task.manual."+(questAvailable(id, quests.get(id), player) ? "0" : "1")));
         }
         if (!data.getString().isEmpty()) { list.add(Component.translatable("availability.type.quests").append(data)); }
      }
      // factions
      if (!factions.isEmpty()) {
         data = Component.empty();
         for (int id : factions.keySet()) {
            if (factions.get(id).factionAvailable == EnumAvailabilityFactionType.Always) { continue; }
            IFaction f = FactionController.instance.get(id);
            data.append(" ");
            if (f == null || gm) {
               data.append(Component.literal( "ID: ").withStyle(ChatFormatting.GRAY))
                       .append(Component.literal( "" + id).withStyle(ChatFormatting.GOLD))
                       .append(Component.literal( gm ? " - " : "").withStyle(ChatFormatting.RESET));
            }
            data.append(Component.translatable("availability." + factions.get(id).factionAvailable.name().toLowerCase()))
                    .append(" ");
            String attitude = factions.get(id).factionStance == EnumAvailabilityFaction.Hostile ? "aggressive": factions.get(id).factionAvailable.name().toLowerCase();
            data.append(Component.translatable("faction.name." + attitude))
                    .append(" ");
            if (f != null) { data.append(f.getName()); }
            data.append(Component.translatable("quest.task.manual."+(factionAvailable(id, factions.get(id).factionStance, factions.get(id).factionAvailable, player) ? "0" : "1")));
         }
         if (!data.getString().isEmpty()) { list.add(Component.translatable("availability.type.factions").append(data)); }
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
                    .append(Component.translatable("quest.task.manual."+(scoreboardAvailable(player, obj, scoreboards.get(obj).scoreboardType, scoreboards.get(obj).scoreboardValue) ? "0" : "1")));
         }
         if (!data.getString().isEmpty()) { list.add(Component.translatable("availability.type.scoreboards").append(data)); }
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
            data.append("]").append(Component.translatable("quest.task.manual."+(listOnly.contains(player.getName().getString()) ? "0" : "1")));
         }
         if (!listExcept.isEmpty()) {
            data.append(Component.translatable("availability.except")).append("[");
            boolean st = true;
            for (String name : listExcept) {
               if (!st) { data.append("; "); } else { st = false; }
               data.append(name);
            }
            data.append("]").append(Component.translatable("quest.task.manual."+(listExcept.contains(player.getName().getString()) ? "0" : "1")));
         }
         if (!data.getString().isEmpty()) { list.add(Component.translatable("availability.type.player.names").append(data)); }
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
                    .append(Component.translatable("quest.task.item."+(bo ? "0" : "1")));
         }
         if (!data.getString().isEmpty()) { list.add(Component.translatable("availability.type.storeddata").append(data)); }
      }
      // moneys
      if (!moneys.isEmpty()) {
         data = Component.empty();
         PlayerGameData gameData = PlayerData.get(player).game;
         for (EnumAvailabilityMoney eam : new ArrayList<>(moneys.keySet())) {
            long value = gameData.getMoney();
            if (eam == EnumAvailabilityMoney.DONAT) { value = gameData.getDonat(); }
            AvailabilityMoneyData money = moneys.get(eam);
            boolean bo = switch (money.type) {
               case SMALLER -> value > money.value;
               case BIGGER -> value < money.value;
               default -> value != money.value;
            };
            data.append(" ")
                    .append(Component.translatable("gui.name"))
                    .append(": ")
                    .append(Component.translatable("gui." + eam.name().toLowerCase()))
                    .append(Component.translatable("quest.task.item."+(bo ? "1" : "0")));
         }
         if (!data.getString().isEmpty()) { list.add(Component.translatable("availability.type.moneys").append(data)); }
      }
      // stacks
      if (!stacks.isEmpty()) {
         data = Component.empty();
         for (int i = 0; i < stacks.getContainerSize(); i++) {
            ItemStack stack = stacks.getItem(i);
            if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
            data.append(" ")
                    .append(stack.getHoverName())
                    .append(" x" + stack.getCount());
         }
         if (!data.getString().isEmpty()) { list.add(Component.translatable("availability.type.stacks").append(data)); }
      }
      // health
      if (healthType != 0) {
         int h = (int) (player.getHealth() / player.getMaxHealth() * 100);
         data = Component.empty().append(" ")
                 .append(Component.translatable("availability." + (healthType == 1 ? "smaller" : "bigger")))
                 .append(" " + h + "%")
                 .append(Component.translatable("quest.task.item."+((healthType == 1 && h < health) || (healthType == 2 && h > health) ? "1" : "0")));
         list.add(Component.translatable("availability.type.health").append(data));
      }
      // in creative mode
      if (onlyGM) {
         data = Component.empty().append(": ")
                 .append(Component.translatable("gui.enabled"))
                 .append(Component.translatable("quest.task.manual."+(gm ? "0" : "1")));
         list.add(Component.translatable("availability.type.only.gm.true").append(data));
      }
      // xp level
      if (minPlayerLevel > 0) {
         data = Component.empty().append(" ")
                 .append(Component.translatable("availability.bigger"))
                 .append(" " + minPlayerLevel)
                 .append(Component.translatable("quest.task.manual."+(player.experienceLevel >= minPlayerLevel ? "0" : "1")));
         list.add(Component.translatable("availability.type.level", data.toString()));
      }
      return list;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) { return true; }
      if (obj instanceof Availability avblt) {
         if (dialogues.size() != avblt.dialogues.size() ||
                 factions.size() != avblt.factions.size() ||
                 moneys.size() != avblt.moneys.size() ||
                 quests.size() != avblt.quests.size() ||
                 scoreboards.size() != avblt.scoreboards.size() ||
                 playerNames.size() != avblt.playerNames.size() ||
                 regions.size() != avblt.regions.size() ||
                 stacksData.size() != avblt.stacksData.size() ||
                 storeddata.size() != avblt.storeddata.size()) { return false; }
         for (Map.Entry<Integer, EnumAvailabilityDialog> entry : dialogues.entrySet()) {
            if (entry.getValue() != avblt.dialogues.get(entry.getKey())) { return false; }
         }
         for (Map.Entry<Integer, AvailabilityFactionData> entry : factions.entrySet()) {
            if (!avblt.factions.containsKey(entry.getKey()) ||
                    !entry.getValue().equals(avblt.factions.get(entry.getKey()))) { return false; }
         }
         for (Map.Entry<EnumAvailabilityMoney, AvailabilityMoneyData> entry : moneys.entrySet()) {
            if (!avblt.moneys.containsKey(entry.getKey()) ||
                    !entry.getValue().equals(avblt.moneys.get(entry.getKey()))) { return false; }
         }
         for (Map.Entry<Integer, EnumAvailabilityQuest> entry : quests.entrySet()) {
            if (entry.getValue() != avblt.quests.get(entry.getKey())) { return false; }
         }
         for (Map.Entry<String, AvailabilityScoreboardData> entry : scoreboards.entrySet()) {
            if (!avblt.scoreboards.containsKey(entry.getKey()) ||
                    !entry.getValue().equals(avblt.scoreboards.get(entry.getKey()))) { return false; }
         }
         for (Map.Entry<String, EnumAvailabilityPlayerName> entry : playerNames.entrySet()) {
            if (!avblt.playerNames.containsKey(entry.getKey()) ||
                    entry.getValue() != avblt.playerNames.get(entry.getKey())) { return false; }
         }
         for (Map.Entry<Integer, EnumAvailabilityRegion> entry : regions.entrySet()) {
            if (entry.getValue() != avblt.regions.get(entry.getKey())) { return false; }
         }
         for (Map.Entry<Integer, AvailabilityStackData> entry : stacksData.entrySet()) {
            if (!entry.getValue().equals(avblt.stacksData.get(entry.getKey()))) { return false; }
         }
         for (int i = 0; i < 9; i++) {
            if (!ItemStack.isSameItemSameTags(stacks.getItem(i), avblt.stacks.getItem(i))) { return false; }
         }
         for (int i = 0; i < storeddata.size(); i++) {
            if (i >= avblt.storeddata.size() ||
                    !storeddata.get(i).equals(avblt.storeddata.get(i))) { return false; }
         }
         return daytime[0] == avblt.daytime[0] && daytime[1] == avblt.daytime[1] &&
                 version == avblt.version && max == avblt.max && minPlayerLevel == avblt.minPlayerLevel &&
                 health == avblt.health && healthType == avblt.healthType && onlyGM == avblt.onlyGM;
      }
      return false;
   }

}
