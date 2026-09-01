package noppes.npcs.controllers.data;

import java.util.*;

import com.google.common.base.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.*;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.ICompatibilty;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.ICustomDrop;
import noppes.npcs.api.handler.data.IDropSetData;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.handler.data.IQuestCategory;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.constants.EnumRewardType;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.client.gui.util.quests.QuestInterface;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketQuestCompletion;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Quest implements ICompatibilty, IQuest, Predicate<EntityNPCInterface>, IDropSetData {

	public boolean cancelable = false;
	public boolean showProgressInChat = true;
	public boolean showProgressInWindow = true;
	public boolean showRewardText = true;
	public int id = -1;
	public int level = 0;
	public int nextQuestId = -1;
	public int rewardExp = 0;
	public int rewardMoney = 0;
	public int rewardDonat = 0;
	public int step = 0;
	public int extraButton = 0;
	public int version = VersionCompatibility.ModRev;
	public int[] forgetDialogues = new int[0];
	public int[] forgetQuests = new int[0];
	public String command = "";
	public String completeText = "";
	public String logText = "";
	public String nextQuestTitle = "";
	public String rewardText = "";
	public String title = "default";
	public String extraButtonText = "";
	public QuestCategory category;
	public FactionOptions factionOptions = new FactionOptions();
	public ResourceLocation icon = new ResourceLocation(CustomNpcs.MODID, "textures/quest/icon/q_0.png");
	public ResourceLocation texture = null;
	public PlayerMail mail = new PlayerMail();
	public QuestInterface questInterface = new QuestInterface();
	public EnumQuestRepeat repeat = EnumQuestRepeat.NONE;
	public EnumQuestCompletion completion = EnumQuestCompletion.Instant;
	public EnumRewardType rewardType = EnumRewardType.RANDOM_ONE;

	public final CompleterNPCData completer = new CompleterNPCData();

	public final Map<Integer, DropSet> rewardItems = new TreeMap<>();

	public Quest(QuestCategory categoryIn) { category = categoryIn; }

	public void load(NBTTagCompound compound) {
		id = compound.getInteger("Id");
		loadPartial(compound);
	}

	public void loadPartial(NBTTagCompound compound) {
		version = compound.getInteger("ModRev");
		VersionCompatibility.CheckAvailabilityCompatibility(this, compound);
		title = compound.getString("Title");
		logText = compound.getString("Text");
		completeText = compound.getString("CompleteText");
		command = compound.getString("QuestCommand");
		nextQuestId = compound.getInteger("NextQuestId");
		rewardExp = compound.getInteger("RewardExp");

		rewardItems.clear();
		if (compound.hasKey("Rewards", 10)) {
			NBTTagList tagList = compound.getCompoundTag("Rewards").getTagList("NpcMiscInv", 10);
			for(int i = 0, j = 0; i < tagList.tagCount(); ++i) {
				DropSet ds = new DropSet(this);
				ds.setInventorySlotContents(0, new ItemStack(tagList.getCompoundTagAt(i)));
				ds.chance = 100.0d;
				ds.amount = new int[] { ds.item.getCount(), ds.item.getCount() };
				ds.pos = j++;
				rewardItems.put(ds.pos, ds);
			}
		} // OLD
		else {
			for (int i = 0; i < compound.getTagList("Rewards", 10).tagCount(); i++) {
				DropSet ds = new DropSet(this);
				ds.load(compound.getTagList("Rewards", 10).getCompoundTagAt(i));
				ds.pos = i;
				rewardItems.put(i, ds);
			}
		} // NEW

		completion = EnumQuestCompletion.values()[compound.getInteger("QuestCompletion")];
		repeat = EnumQuestRepeat.values()[compound.getInteger("QuestRepeat")];
		questInterface.load(compound, id);
		factionOptions.load(compound.getCompoundTag("QuestFactionPoints"));
		mail.load(compound.getCompoundTag("QuestMail"));

		rewardType = EnumRewardType.values()[compound.getInteger("RewardType")];
		rewardMoney = compound.getInteger("RewardMoney");
		rewardDonat = compound.getInteger("RewardDonat");
		nextQuestTitle = compound.getString("NextQuestTitle");
		if (hasNewQuest()) {
			nextQuestTitle = getNextQuest().title;
		} else {
			nextQuestTitle = "";
		}
		if (compound.hasKey("QuestIcon", 8)) {
			String loc = compound.getString("QuestIcon");
			if (loc.contains("quest icon")) { loc = loc.replace("quest icon", "quest/icon"); }
			icon = new ResourceLocation(NoppesUtilServer.validLocation(loc));
		} else {
			icon = new ResourceLocation(CustomNpcs.MODID, "textures/quest/icon/q_0.png");
		}
		if (compound.hasKey("QuestTexture", 8)) {
			texture = new ResourceLocation(NoppesUtilServer.validLocation(compound.getString("QuestTexture")));
		} else { texture = null; }
		extraButtonText = compound.getString("ExtraButtonText");
		level = compound.getInteger("QuestLevel");
		cancelable = compound.getBoolean("Cancelable");
		if (compound.hasKey("ShowProgressInChat", 1)) {
			showProgressInChat = compound.getBoolean("ShowProgressInChat");
		}
		if (compound.hasKey("ShowProgressInWindow", 1)) {
			showProgressInWindow = compound.getBoolean("ShowProgressInWindow");
		}
		if (compound.hasKey("ShowRewardText", 1)) {
			showRewardText = compound.getBoolean("ShowRewardText");
		}
		setExtraButton(compound.getInteger("ExtraButton"));
		rewardText = compound.getString("AddRewardText");
		step = compound.getInteger("Step") % 3;
		if (step < 0) {
			step *= -1;
		}
		forgetDialogues = compound.getIntArray("ForgetDialogues");
		forgetQuests = compound.getIntArray("ForgetQuests");
		completer.load(compound);
	}

	@Override
	public NBTTagCompound save(NBTTagCompound compound) {
		compound.setInteger("Id", id);
		return savePartial(compound);
	}

	public NBTTagCompound savePartial(NBTTagCompound compound) {
		compound.setInteger("ModRev", version);
		compound.setString("Title", title);
		compound.setString("Text", logText);
		compound.setString("CompleteText", completeText);
		compound.setInteger("NextQuestId", nextQuestId);
		compound.setInteger("RewardExp", rewardExp);
		compound.setString("QuestCommand", command);
		compound.setInteger("QuestCompletion", completion.ordinal());
		compound.setInteger("QuestRepeat", repeat.ordinal());
		questInterface.save(compound);
		compound.setTag("QuestFactionPoints", factionOptions.save(new NBTTagCompound()));
		compound.setTag("QuestMail", mail.save());

		compound.setString("NextQuestTitle", nextQuestTitle);
		compound.setInteger("RewardMoney", rewardMoney);
		compound.setInteger("RewardDonat", rewardDonat);
		compound.setString("QuestIcon", icon.toString());
		if (texture != null) { compound.setString("QuestTexture", texture.toString()); }
		compound.setInteger("RewardType", rewardType.ordinal());
		compound.setInteger("QuestLevel", level);
		compound.setBoolean("Cancelable", cancelable);
		compound.setBoolean("ShowProgressInChat", showProgressInChat);
		compound.setBoolean("ShowProgressInWindow", showProgressInWindow);
		compound.setBoolean("ShowRewardText", showRewardText);
		compound.setString("ExtraButtonText", extraButtonText);
		compound.setInteger("ExtraButton", extraButton);
		compound.setString("AddRewardText", rewardText);
		compound.setInteger("Step", step);
		compound.setIntArray("ForgetDialogues", forgetDialogues);
		compound.setIntArray("ForgetQuests", forgetQuests);

		completer.save(compound);

		NBTTagList dropList = new NBTTagList();
		int s = 0;
		for (int slot : rewardItems.keySet()) {
			if (rewardItems.get(slot) == null) { continue; }
			if (rewardItems.get(slot).pos != s) { rewardItems.get(slot).pos = s; }
			dropList.appendTag(rewardItems.get(slot).save());
			s++;
		}
		compound.setTag("Rewards", dropList);

		return compound;
	}

	public boolean hasNewQuest() { return getNextQuest() != null; }

	@Override
	public Quest getNextQuest() { return QuestController.instance == null ? null : QuestController.instance.quests.get(nextQuestId); }

	public boolean complete(EntityPlayer player, QuestData data) {
		if (completion == EnumQuestCompletion.Instant) {
			if (player instanceof EntityPlayerMP) { Packets.send((EntityPlayerMP) player, new PacketQuestCompletion(data.quest.id)); }
			return true;
		}
		return false;
	}

	public Quest copy() {
		Quest quest = new Quest(category);
		quest.load(save(new NBTTagCompound()));
		return quest;
	}

	@Override
	public int getVersion() { return version; }

	@Override
	public void setVersion(int versionIn) { version = versionIn; }

	@Override
	public int getId() { return id; }

	@Override
	public String getName() { return title; }

	@Override
	public IQuestCategory getCategory() { return category; }

	@Override
	public void save() { QuestController.instance.saveQuest(category, this); }

	@Override
	public void setName(String name) { title = name; }

	@Override
	public void setLogText(String text) { logText = text; }

	@Override
	public String getCompleteText() { return completeText; }

	@Override
	public void setCompleteText(String text) { completeText = text == null ? "" : text; }

	@Override
	public void setNextQuest(IQuest quest) {
		if (quest == null) {
			nextQuestId = -1;
			nextQuestTitle = "";
		}
		else {
			if (quest.getId() < 0) { throw new CustomNPCsException("Quest id is lower than 0"); }
			nextQuestId = quest.getId();
			nextQuestTitle = quest.getTitle().getFormattedText();
		}
	}

	public QuestObjective[] getObjectives(EntityPlayer player) {
		if (player == null) { throw new CustomNPCsException("Player is NULL"); }
		PlayerData data = PlayerData.get(player);
		if (!data.questData.activeQuests.containsKey(id)) { throw new CustomNPCsException("Player doesnt have this quest active"); }
		return questInterface.getObjectives(player);
	}

	@Override
	public IQuestObjective[] getObjectives(IPlayer<?> player) {
		if (player == null) { throw new CustomNPCsException("Player is NULL"); }
		if (!player.hasActiveQuest(id)) { throw new CustomNPCsException("Player doesnt have this quest active"); }
		return questInterface.getObjectives(player.getMCEntity());
	}

	@Override
	public boolean getIsRepeatable() { return repeat != EnumQuestRepeat.NONE; }

	// New from Unofficial (BetaZavr)
	@Override
	public IQuestObjective addTask() { return questInterface.addTask(EnumQuestTask.ITEM); }

	@Override
	public ICustomNpc<?> getCompleterNpc() { return completer.getINpc(); }

	@Override
	public int getExtraButton() { return extraButton; }

	@Override
	public String getExtraButtonText() { return extraButtonText; }

	@Override
	public int[] getForgetDialogues() { return forgetDialogues; }

	@Override
	public int[] getForgetQuests() { return forgetQuests; }

	@Override
	public int getWorld() { return level; }

	@Override
	public int getRewardType() { return rewardType.ordinal(); }

	@Override
	public ITextComponent getTitle() {
		Component titleCom = Component.empty();
		if (level > 0) {
			titleCom.append(Component.translatable("type.level").withStyle(level <= CustomNpcs.MaxLv / 3 ? TextFormatting.DARK_GREEN :
							(float) level <= (float) CustomNpcs.MaxLv / 1.5f ? TextFormatting.YELLOW : TextFormatting.RED).getParent())
					.appendSibling(Component.literal(level + ": ").withStyle(TextFormatting.GRAY).getParent());
		}
		return titleCom.append(Component.translatable(title).getParent()).getParent();
	}

	@Override
	public boolean isCancelable() { return cancelable; }

	@Override
	public boolean isSetUp() {
		if (questInterface.tasks.length == 0) { return false; }
		for (QuestObjective task : questInterface.tasks) {
			if ((task.getEnumType() == EnumQuestTask.ITEM || task.getEnumType() == EnumQuestTask.CRAFT)) {
				if (task.getItemStack().isEmpty()) { return false; }
			}
			else if (task.getEnumType() == EnumQuestTask.DIALOG) {
				if (DialogController.instance.dialogs.get(task.getTargetID()) == null) { return false; }
			}
		}
		return true;
	}

	@Override
	public boolean removeTask(IQuestObjective task) { return questInterface.removeTask((QuestObjective) task); }

	@SideOnly(Side.SERVER)
	@Override
	public void sendChangeToAll() { Packets.sendAll(new PacketSyncUpdate(id, 2, save(new NBTTagCompound()))); }

	@Override
	public void setCancelable(boolean cancelableIn) { cancelable = cancelableIn; }

	@Override
	public void setCompleterNpc(ICustomNpc<?> npc) {
		if (npc != null) { completer.reset(npc.getMCEntity()); }
	}

	@Override
	public void setExtraButton(int type) {
		if (type < 0) { type *= -1; }
		extraButton = type % 6;
	}

	@Override
	public void setExtraButtonText(String hover) { extraButtonText = hover == null ? "" : hover; }

	@Override
	public void setForgetDialogues(int[] forget) { forgetDialogues = forget; }

	@Override
	public void setForgetQuests(int[] forget) { forgetQuests = forget; }

	@Override
	public void setLevel(int levelIn) {
		if (levelIn < 0 ) { levelIn *= -1; }
		level = ValueUtil.correctInt(levelIn, 1, CustomNpcs.MaxLv);
	}

	@Override
	public void setRewardText(String text) { rewardText = text; }

	@Override
	public void setRewardType(int type) {
		if (type < 0 || type >= EnumRewardType.values().length) { return; }
		rewardType = EnumRewardType.values()[type];
	}

	@Override
	public List<ICustomDrop> getRewards() { return new ArrayList<>(rewardItems.values()); }

	public Component getLineKey() {
		boolean b = isSetUp();
		ITextComponent t = getTitle();
		t.getStyle().setColor(TextFormatting.RESET);
		return Component.empty()
				.append(Component.literal("ID:" + id + "-\"").withStyle(TextFormatting.GRAY).getParent())
				.append(t)
				.append(Component.literal("\"").withStyle(TextFormatting.GRAY).getParent())
				.append(Component.literal(" (").withStyle(b ? TextFormatting.DARK_GREEN : TextFormatting.RED).getParent())
				.append(Component.translatable("quest.has." + b).getParent())
				.append(Component.literal(")").withStyle(b ? TextFormatting.DARK_GREEN : TextFormatting.RED).getParent());
	}

	@Override
	public boolean apply(EntityNPCInterface npc) { return completer.isNpc(npc); }

	@Override
	public List<String> getLogText() {
		List<String> allTextLogs = new ArrayList<>();
		if (showRewardText) {
			List<TempDropData> list = new ArrayList<>();
			for (int i = 0; i < rewardItems.size(); i++) {
				DropSet ds = rewardItems.get(i);
				if (!ds.item.isEmpty()) {
					boolean has = false;
					if (rewardType == EnumRewardType.ALL) {
						for (TempDropData tdd : list) {
							if (ds.item.isItemEqual(tdd.stack) && ItemStack.areItemStackShareTagsEqual(ds.item, tdd.stack)) {
								tdd.add(ds);
								has = true;
								break;
							}
						}
					}
					if (!has) { list.add(new TempDropData(ds)); }
				}
			}
			if (!list.isEmpty() || rewardExp > 0 || rewardMoney > 0 || rewardDonat > 0 ||!rewardText.isEmpty()) {
				allTextLogs.add("");
				allTextLogs.add(Component.translatable("questlog.reward").getFormattedText());
			}
			if (!list.isEmpty()) {
				allTextLogs.add(Component.translatable("questlog."
						+ (rewardType == EnumRewardType.ONE_SELECT ? "one" :
						rewardType == EnumRewardType.RANDOM_ONE ? "rnd" : "all") + ".reward").getFormattedText());
				for (TempDropData tdd : list) {
					StringBuilder line = new StringBuilder(" -  ")
							.append((char) 0xffff).append(" ")
							.append(tdd.stack.getDisplayName());
					if ((tdd.min == tdd.max || tdd.max < 1) && tdd.min > 1) { line.append(" x").append(tdd.min); }
					else if (tdd.max > 1 || tdd.min > 1) {
						line.append(" x(")
								.append(tdd.min > 1 ? tdd.min : "1")
								.append("...")
								.append(tdd.max > 1 ? tdd.max : "1")
								.append(")");
					}

					allTextLogs.add(line.toString());
				}
			}
			if (rewardMoney > 0) {
				allTextLogs.add(Component.translatable("questlog.rewardmoney",
						Util.instance.getTextReducedNumber(rewardMoney, true, true, false),
						CustomNpcs.displayCurrencies).getFormattedText());
			}
			if (rewardDonat > 0) {
				allTextLogs.add(Component.translatable("questlog.rewarddonat",
						Util.instance.getTextReducedNumber(rewardDonat, true, true, false),
						CustomNpcs.displayCurrencies).getFormattedText());
			}
			if (rewardExp > 0) {
				allTextLogs.add(Component.translatable("questlog.rewardexp", "" + rewardExp).getFormattedText());
			}
		}
		if (!rewardText.isEmpty()) {
			allTextLogs.add(rewardText.contains("%") ? rewardText : Component.translatable(rewardText).getFormattedText());
		}
		if (!logText.isEmpty()) {
			allTextLogs.add("");
			allTextLogs.add(TextFormatting.BOLD + Component.translatable("gui.description").getFormattedText());
			allTextLogs.add(logText.contains("%") ? logText : Component.translatable(logText).getFormattedText());
		}
		return allTextLogs;
	}

	public boolean hasCompassSettings() {
		for (QuestObjective task : questInterface.tasks) {
			if (task.rangeCompass > 3 && !task.pos.equals(BlockPos.ORIGIN)) { return true; }
		}
		return false;
	}

	@Override
	public int getNpcLevel() { return level; }

	@Override
	public boolean removeDrop(DropSet dropSet) {
		Map<Integer, DropSet> newDrop = new TreeMap<>();
		boolean del = false;
		int j = 0;
		for (int slot : rewardItems.keySet()) {
			if (rewardItems.get(slot) == dropSet) {
				del = true;
				continue;
			}
			newDrop.put(j, rewardItems.get(slot));
			newDrop.get(j).pos = j;
			j++;
		}
		if (del) {
			rewardItems.clear();
			rewardItems.putAll(newDrop);
		}
		return del;
	}

	public static class TempDropData {

		private boolean isCreate = false;
		private final List<Double> chances = new ArrayList<>();
		public final ItemStack stack;
		public int min;
		public int max;

		public TempDropData(DropSet ds) {
			min = ds.amount[0];
			max = ds.amount[1];
			chances.add(ds.chance);
			stack = ds.item.copy();
		}

		public TempDropData add(DropSet ds) {
			min += ds.amount[0];
			max += ds.amount[1];
			chances.add(ds.chance);
			return this;
		}

		public ItemStack getStack() {
			if (!isCreate) {
				isCreate = true;
				double chance = 0.0d;
				for (double ch : chances) { chance += ch; }
				chance /= chances.size();
				chance = ValueUtil.correctDouble(chance, 0.0d, 100.0d);
				if (chance != 100.0d) {
					NBTTagCompound compound = stack.getOrCreateSubCompound("display");
					NBTTagList tagList = compound.getTagList("Lore", 8);
					tagList.appendTag(new NBTTagString(ITextComponent.Serializer.componentToJson(
							Component.translatable("inv.dropChance")
									.appendText(": " + (Math.round(chance * 10.0d) / 10.0d) + "%"))));
					compound.setTag("Lore", tagList);
				}
			}
			return stack;
		}

	}

	public static class CompleterNPCData {

		private NBTTagCompound spawnData;
		private String name;
		private int dimension;
		private BlockPos npcPos;
		private UUID uuid;
		private boolean strict;

		public CompleterNPCData() { clear(); }

		public void load(NBTTagCompound compound) {
			clear();
			if (compound.hasKey("CompleterPos", 11)) {
				int[] pos = compound.getIntArray("CompleterPos");
				npcPos = new BlockPos(pos[0], pos[1], pos[2]);
				dimension = pos[3];
			}
			else if (compound.hasKey("CompleterPos", 4)) { npcPos = BlockPos.fromLong(compound.getLong("CompleterPos")); }
			if (compound.hasKey("CompleterPosDimension", 3)) { dimension = compound.getInteger("CompleterPosDimension"); }
			try { uuid = compound.getUniqueId("CompleterUUID"); } catch (Exception ignored) {}
			if (compound.hasKey("CompleterIsStrict", 1)) { strict = compound.getBoolean("CompleterIsStrict"); }
			if (compound.hasKey("CompleterNpc", 10)) { spawnData = compound.getCompoundTag("CompleterNpc"); }
			if (name.isEmpty() && !spawnData.getString("Name").isEmpty()) { name = spawnData.getString("Name"); }
		}

		public NBTTagCompound save(NBTTagCompound compound) {
			compound.setInteger("CompleterPosDimension", dimension);
			compound.setLong("CompleterPos", npcPos.toLong());
			compound.setUniqueId("CompleterUUID", uuid);
			compound.setString("CompleterName", name);
			compound.setBoolean("CompleterIsStrict", strict);
			compound.setTag("CompleterNpc", spawnData);
			return compound;
		}

		private void clear() {
			spawnData = new NBTTagCompound();
			name = "";
			uuid = UUID.randomUUID();
			dimension = 0;
			npcPos = BlockPos.ORIGIN;
			strict = false;
		}

		public void reset(@Nullable EntityNPCInterface npcIn) {
			if (npcIn != null) {
				spawnData = npcIn.writeSpawnData();
				spawnData.getCompoundTag("Puppet").setBoolean("PuppetAnimate", false);
				dimension = npcIn.homeDimensionId;
				npcPos = npcIn.getPosition();
				uuid = npcIn.getUniqueID();
				name = npcIn.getName();
				strict = true;
			}
		}

		public @Nullable EntityNPCInterface getNpc() {
			World world = CustomNpcs.proxy.getOverWorld();
			EntityNPCInterface npc = null;
			if (world != null) {
				npc = new EntityCustomNpc(world);
				npc.readSpawnData(spawnData);
				npc.display.setName(name);
				npc.display.setShowName(1);
			}
			return npc;
		}

		public @Nullable ICustomNpc<?> getINpc() {
			EntityNPCInterface npc = getNpc();
			return npc == null ? null : (ICustomNpc<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(npc);
		}

		public @Nonnull String getName() { return name; }

		public @Nonnull BlockPos getPos() { return npcPos; }

		public int getDimension() { return dimension; }

		public boolean isEmpty() { return name.isEmpty() || spawnData.hasNoTags(); }

		public boolean isStrict() { return strict; }

		public void setStrict(boolean isStrict) { strict = isStrict; }

		public boolean isNpc(@Nullable EntityNPCInterface npc) {
			if (npc != null && npc.getName().equals(name)) {
				return !strict || npc.getUniqueID().equals(uuid);
			}
			return false;
		}
	}

}
