package noppes.npcs.client.gui.util.quests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.DimensionManager;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IPos;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.handler.data.IDialog;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.ValueUtil;

public class QuestObjective implements IQuestObjective {

	private final int parentID;
	private final EntityPlayer player;
	private int objectPos;

	private boolean ignoreDamage = false;
	private boolean ignoreNBT = false;
	private boolean leaveItem = false;
	private boolean partName = false;
	private boolean andTitle = false;
	private boolean notShowLogEntity = false;
	private boolean setPointOnMiniMap = false;
	private int id = 0;
	private int maxProgress = 1;
	private int range = 10;
	private EnumQuestTask type = EnumQuestTask.ITEM;
	private ItemStack item = ItemStack.EMPTY;
	private String name = "";

	public int regionID = -1;
	public int dimension = 0;
	public int rangeCompass = 5;
	public int colorCompass = (int) (Math.random() * 16777215.0) | 0xFF000000;
	public BlockPos pos = BlockPos.ORIGIN;
	public String entityName = "";

	public QuestObjective(int parentIDIn, int objectPosIn, EntityPlayer playerIn) {
		parentID = parentIDIn;
		player = playerIn;
		objectPos = objectPosIn;
	}

	public QuestObjective(int parentID, int objectPosIn, EnumQuestTask typeIn) {
		this(parentID, objectPosIn, (EntityPlayer) null);
		type = typeIn;
	}

	public QuestObjective copyToPlayer(EntityPlayer player) {
		QuestObjective newObj = new QuestObjective(parentID, objectPos, player);
		newObj.type = type;
		newObj.maxProgress = maxProgress;
		newObj.id = id;
		newObj.range = range;
		newObj.name = name;
		newObj.item = item;
		newObj.leaveItem = leaveItem;
		newObj.ignoreDamage = ignoreDamage;
		newObj.ignoreNBT = ignoreNBT;
		newObj.setPointOnMiniMap = setPointOnMiniMap;
		newObj.partName = partName;
		newObj.andTitle = andTitle;
		newObj.notShowLogEntity = notShowLogEntity;
		newObj.pos = pos;
		newObj.dimension = dimension;
		newObj.rangeCompass = rangeCompass;
		newObj.colorCompass = colorCompass;
		newObj.entityName = entityName;
		return newObj;
	}

	@Override
	public int getAreaRange() { return range; }

	@Override
	public int getCompassDimension() { return dimension; }

	@Override
	public IPos getCompassPos() { return Objects.requireNonNull(NpcAPI.Instance()).getIPos(pos); }

	@Override
	public int getCompassRange() { return rangeCompass; }

	@Override
	public int getCompassColor() { return colorCompass; }

	public HashMap<ItemStack, Integer> getCrafted(QuestData data) {
		if (!data.extraData.hasKey("Crafts", 9)) { data.extraData.setTag("Crafts", new NBTTagList()); }
		HashMap<ItemStack, Integer> map = new HashMap<>();
		for (int i = 0; i < data.extraData.getTagList("Crafts", 10).tagCount(); ++i) {
			NBTTagCompound compound = data.extraData.getTagList("Crafts", 10).getCompoundTagAt(i);
			if (compound.getInteger("ObjectPos") != objectPos) { continue; }
			map.put(new ItemStack(compound.getCompoundTag("Item")), compound.getInteger("Value"));
		}
		return map;
	}

	public EnumQuestTask getEnumType() { return type; }

	@Override
	public IItemStack getItem() { return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item); }

	public ItemStack getItemStack() { return item; }

	public HashMap<String, Integer> getKilled(QuestData data) {
		if (!data.extraData.hasKey("Targets", 9)) { data.extraData.setTag("Targets", new NBTTagList()); }
		HashMap<String, Integer> map = new HashMap<>();
		for(int i = 0; i < data.extraData.getTagList("Targets", 10).tagCount(); ++i) {
			NBTTagCompound compound = data.extraData.getTagList("Targets", 10).getCompoundTagAt(i);
			if (compound.getInteger("ObjectPos") != objectPos) { continue; }
			map.put(compound.getString("Slot"), compound.getInteger("Value"));
		}
		return map;
	}

	@Override
	public int getMaxProgress() { return type == EnumQuestTask.DIALOG || type == EnumQuestTask.LOCATION ? 1 : maxProgress; }

	public NBTTagCompound getNBT() {
		NBTTagCompound nbtTask = new NBTTagCompound();
		nbtTask.setInteger("Type", type.ordinal());
		nbtTask.setBoolean("SetPointOnMiniMap", setPointOnMiniMap);
		NBTTagCompound nbtCompass = new NBTTagCompound();
		nbtCompass.setIntArray("Pos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
		nbtCompass.setInteger("Dimension", dimension);
		nbtCompass.setInteger("RegionID", regionID);
		nbtCompass.setInteger("Range", rangeCompass);
		nbtCompass.setInteger("Color", colorCompass);
		nbtCompass.setString("EntityName", entityName);
		nbtTask.setTag("CompassData", nbtCompass);

		if (maxProgress > 0) { nbtTask.setInteger("Progress", maxProgress); }
		if (id > 0) { nbtTask.setInteger("TargetID", id); }
		if (!name.isEmpty()) {
			nbtTask.setString("TargetName", name);
			nbtTask.setBoolean("TargetPart", partName);
			nbtTask.setBoolean("TargetTitle", andTitle);
			nbtTask.setBoolean("NotShowLogEntity", notShowLogEntity);
		}
		if (type == EnumQuestTask.AREAKILL) { nbtTask.setInteger("Range", range); }
		if (!item.isEmpty()) {
			nbtTask.setTag("Item", item.writeToNBT(new NBTTagCompound()));
			nbtTask.setBoolean("LeaveItem", leaveItem);
			nbtTask.setBoolean("IgnoreDamage", ignoreDamage);
			nbtTask.setBoolean("IgnoreNBT", ignoreNBT);
		}
		return nbtTask;
	}

	@Override
	public String getOrientationEntityName() { return entityName; }

	@Override
	public int getProgress() {
		if (type == EnumQuestTask.ITEM) {
			int count = 0;
			for (int i = 0; i < Objects.requireNonNull(player).inventory.getSizeInventory(); ++i) {
				ItemStack stack = player.inventory.getStackInSlot(i);
				if (!NoppesUtilServer.isItemStackNull(item) && NoppesUtilPlayer.compareItems(item, stack, ignoreDamage, ignoreNBT)) {
					count += stack.getCount();
				}
			}
			return ValueUtil.correctInt(count, 0, maxProgress);
		}
		PlayerData data = PlayerData.get(player);
		if (type == EnumQuestTask.DIALOG) { return data.dialogData.has(id) ? 1 : 0; }
		QuestData questData = data.questData.activeQuests.get(parentID);
		if (questData == null) { return 0; }
		if (type == EnumQuestTask.LOCATION) {
			for (NBTBase dataNBT : questData.extraData.getTagList("Locations", 10)) {
				if (name.equalsIgnoreCase(((NBTTagCompound) dataNBT).getString("Location"))) {
					return ((NBTTagCompound) dataNBT).getBoolean("Found") ? 1 : 0;
				}
			}
			return 0;
		}
		if (type == EnumQuestTask.KILL || type == EnumQuestTask.AREAKILL || type == EnumQuestTask.MANUAL) {
			HashMap<String, Integer> killed = getKilled(questData);
			if (!killed.containsKey(name)) { return 0; }
			return killed.get(name);
		}
		if (type == EnumQuestTask.CRAFT) {
			HashMap<ItemStack, Integer> crafted = getCrafted(questData);
			for (ItemStack stack : crafted.keySet()) {
				if (NoppesUtilPlayer.compareItems(item, stack, ignoreDamage, ignoreNBT)) { return crafted.get(stack); }
			}
		}
		return 0;
	}

	@Override
	public int getTargetID() { return id; }

	@Override
	public String getTargetName() { return name; }

	@Override
	public ITextComponent getMCText() {
		Component text;
		boolean bo = isCompleted();
		if (type == EnumQuestTask.ITEM || type == EnumQuestTask.CRAFT) {
			text = Component.empty()
					.append(item.getDisplayName())
					.append(Component.literal(": ").withStyle(TextFormatting.RESET))
					.append(Component.literal("" + getProgress()).withStyle(bo ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED))
					.append(Component.literal("/" + getMaxProgress() + " ").withStyle(TextFormatting.RESET));
			text.append(Component.translatable("quest.task." + (type == EnumQuestTask.ITEM ? "item" : "craft") + "."+(isCompleted() ? "0" : "1")));
			if (leaveItem) { text.append(Component.translatable("quest.take.log")); }
		} // Collect Item or Craft Item
		else if (type == EnumQuestTask.DIALOG) {
			Component name = Component.literal("null");
			Dialog dialog = DialogController.instance.dialogs.get(id);
			if (dialog != null) { name = Component.translatable(dialog.title); }
			text = Component.empty()
					.append(name.withStyle(bo ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED))
					.append(Component.translatable("quest.task.dialog." + (bo ? "0" : "1")).withStyle(TextFormatting.RESET));
		} // Dialog
		else if (type == EnumQuestTask.KILL || type == EnumQuestTask.AREAKILL) {
			text = Component.translatable("entity." + name + ".name");
			if (text.getFormattedText().contains("entity.") && text.getFormattedText().indexOf(".name") > 0) {
				text = Component.literal(name);
			}
			text.append(Component.literal(" " + getProgress()).withStyle(bo ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED))
					.append(Component.literal("/" + getMaxProgress() + " ").withStyle(TextFormatting.RESET));
			text.append(Component.translatable("quest.task.kill."+(isCompleted() ? "0" : "1")));
		} // Kill
		else if (type == EnumQuestTask.LOCATION) {
			text = Component.empty()
					.append(Component.translatable(name).withStyle(bo ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED))
					.append(Component.literal(": ").withStyle(TextFormatting.RESET))
					.append(Component.translatable("quest.task.location." + (bo ? "0" : "1"))).withStyle(TextFormatting.RESET);
		} // Location
		else if (type == EnumQuestTask.MANUAL) {
			text = Component.empty()
					.append(Component.translatable(name).withStyle(bo ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED))
					.append(Component.literal(": ").withStyle(TextFormatting.RESET))
					.append(Component.literal("" + getProgress()).withStyle(bo ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED))
					.append(Component.literal("/" + getMaxProgress() + " ").withStyle(TextFormatting.RESET));
			text.append(Component.translatable("quest.task.manual."+(isCompleted() ? "0" : "1")));
		} // Manual
		else { text = Component.literal("null type: " + type + " #" + toString().substring(toString().indexOf("@") + 1)); }
		return text.getParent();
	}

	@Override
	public String getText() { return getMCText().getFormattedText(); }

	@Override
	public int getType() { return type.ordinal(); }

	@Override
	public boolean isAndTitle() { return andTitle; }

	@Override
	public boolean isCompleted() {
		if (type == EnumQuestTask.ITEM) {return NoppesUtilPlayer.compareItems(player, item, ignoreDamage, ignoreNBT, maxProgress); }
		if (type == EnumQuestTask.DIALOG) { return PlayerData.get(player).dialogData.has(id); }
		return getProgress() >= maxProgress;
	}

	@Override
	public boolean isIgnoreDamage() { return ignoreDamage; }

	@Override
	public boolean isItemIgnoreNBT() { return ignoreNBT; }

	@Override
	public boolean isItemLeave() { return leaveItem; }

	@Override
	public boolean isNotShowLogEntity() { return notShowLogEntity; }

	@Override
	public boolean isPartName() { return partName; }

	@Override
	public boolean isSetPointOnMiniMap() { return setPointOnMiniMap; }

	public void load(NBTTagCompound nbtTask) {
		type = EnumQuestTask.values()[nbtTask.getInteger("Type")];
		setPointOnMiniMap = nbtTask.getBoolean("SetPointOnMiniMap");
		objectPos = nbtTask.getInteger("ObjectPos");
		if (nbtTask.hasKey("CompassData", 10)) {
			NBTTagCompound nbtCompass = nbtTask.getCompoundTag("CompassData");
			int[] bp = nbtCompass.getIntArray("Pos");
			pos = new BlockPos(bp[0], bp[1], bp[2]);
			dimension = nbtCompass.hasKey("DimensionID", 3) ? nbtCompass.getInteger("DimensionID") : nbtCompass.getInteger("Dimension");
			if (nbtCompass.hasKey("RegionID", 3)) { regionID = nbtCompass.getInteger("RegionID"); }
			if (nbtCompass.hasKey("Color", 3)) { colorCompass = nbtCompass.getInteger("Color"); }
			rangeCompass = nbtCompass.getInteger("Range");
			entityName = nbtCompass.getString("EntityName");
		}
		if (nbtTask.hasKey("Progress", 3)) { setMaxProgress(nbtTask.getInteger("Progress")); }
		if (nbtTask.hasKey("TargetID", 3)) { setTargetID(nbtTask.getInteger("TargetID")); }
		if (nbtTask.hasKey("TargetName", 8)) {
			setTargetName(nbtTask.getString("TargetName"));
			partName = nbtTask.getBoolean("TargetPart");
			andTitle = nbtTask.getBoolean("TargetTitle");
			notShowLogEntity = nbtTask.getBoolean("NotShowLogEntity");
		}
		if (nbtTask.hasKey("Range", 3)) { setAreaRange(nbtTask.getInteger("Range")); }
		if (nbtTask.hasKey("Item", 10)) {
			setItem(new ItemStack(nbtTask.getCompoundTag("Item")));
			leaveItem = nbtTask.getBoolean("LeaveItem");
			ignoreDamage = nbtTask.getBoolean("IgnoreDamage");
			ignoreNBT = nbtTask.getBoolean("IgnoreNBT");
		}
	}

	@Override
	public void setAndTitle(boolean addTitle) { andTitle = addTitle; }

	@Override
	public void setAreaRange(int rangeIn) {
		if (rangeIn < 3 || rangeIn > 32) { throw new CustomNPCsException("Range must be between 3 and 32"); }
		range = rangeIn;
	}

	@Override
	public void setCompassDimension(int dimensionId) {
		if (DimensionManager.isDimensionRegistered(dimensionId)) { throw new CustomNPCsException("Dimension ID:" + dimensionId + " not found"); }
		dimension = dimensionId;
	}

	@Override
	public void setCompassPos(int x, int y, int z) { pos = new BlockPos(x, y, z); }

	@Override
	public void setCompassPos(IPos posIn) { pos = posIn.getMCBlockPos(); }

	@Override
	public void setCompassRange(int range) {
		if (range < 0 || range > 64) { throw new CustomNPCsException("Compass Range must be between 3 and 64"); }
		rangeCompass = range;
	}

	@Override
	public void setCompassColor(int color) {
		rangeCompass = color & 0xFFFFFF;
	}

	public void setCrafted(QuestData data, HashMap<ItemStack, Integer> crafted) {
		NBTTagList nbtList = data.extraData.getTagList("Crafts", 10);
		if (crafted != null) {
			Set<ItemStack> sets = new HashSet<>();
			for(int i = 0; i < nbtList.tagCount(); ++i) {
				NBTTagCompound compound = nbtList.getCompoundTagAt(i);
				ItemStack item = new ItemStack(compound.getCompoundTag("Item"));
				if (compound.getInteger("ObjectPos") == objectPos) {
					for (ItemStack craft : crafted.keySet()) {
						if (NoppesUtilPlayer.compareItems(item, craft, ignoreDamage, ignoreNBT)) {
							compound.setInteger("Value", crafted.get(craft));
							sets.add(craft);
						}
					}
				}
			}
			for (ItemStack item : crafted.keySet()) {
				if (sets.contains(item)) { continue; }
				NBTTagCompound compound = new NBTTagCompound();
				compound.setTag("Item", item.writeToNBT(new NBTTagCompound()));
				compound.setInteger("Value", crafted.get(item));
				compound.setInteger("ObjectPos", objectPos);
				nbtList.appendTag(compound);
			}
		}
		data.extraData.setTag("Crafts", nbtList);
	}

	@Override
	public void setItem(IItemStack itemIn) { item = itemIn.getMCItemStack(); }

	public void setItem(ItemStack itemIn) { item = itemIn; }

	@Override
	public void setItemIgnoreDamage(boolean bo) { ignoreDamage = bo; }

	@Override
	public void setItemIgnoreNBT(boolean bo) { ignoreNBT = bo; }

	@Override
	public void setItemLeave(boolean bo) { leaveItem = bo; }

	public void setKilled(QuestData data, HashMap<String, Integer> killed) {
		NBTTagList nbtList = data.extraData.getTagList("Targets", 10);
		if (killed != null) {
			Set<String> sets = new HashSet<>();
			for(int i = 0; i < nbtList.tagCount(); ++i) {
				NBTTagCompound compound = nbtList.getCompoundTagAt(i);
				String slot = compound.getString("Slot");
				if (compound.getInteger("ObjectPos") == objectPos && killed.containsKey(slot)) {
					compound.setInteger("Value", killed.get(slot));
					sets.add(slot);
				}
			}
			for (String slot : killed.keySet()) {
				if (sets.contains(slot)) { continue; }
				NBTTagCompound compound = new NBTTagCompound();
				compound.setString("Slot", slot);
				compound.setInteger("Value", killed.get(slot));
				compound.setInteger("ObjectPos", objectPos);
				nbtList.appendTag(compound);
			}
		}
		data.extraData.setTag("Targets", nbtList);
	}

	@Override
	public void setMaxProgress(int value) {
		if (value < 1 || value > 10000000) { throw new CustomNPCsException("Progress must be between 1 and 10000000"); }
		if ((type == EnumQuestTask.DIALOG || type == EnumQuestTask.LOCATION) && value > 1) { throw new CustomNPCsException("Progress has to be 0 or 1"); }
		maxProgress = value;
	}

	@Override
	public void setNotShowLogEntity(boolean notShowLogEntityIn) { notShowLogEntity = notShowLogEntityIn; }

	@Override
	public void setOrientationEntityName(String name) { entityName = name; }

	@Override
	public void setPartName(boolean isPart) { partName = isPart; }

	@Override
	public void setPointOnMiniMap(boolean bo) { setPointOnMiniMap = bo; }

	@Override
	public void setProgress(int progress) {
		if (type == EnumQuestTask.ITEM) { throw new CustomNPCsException("Cant set the progress of ItemTask"); }
		if (player == null) { throw new CustomNPCsException("Player not is NULL"); }
		PlayerData data = PlayerData.get(player);
		QuestData questData = data.questData.activeQuests.get(parentID);
		if (type == EnumQuestTask.DIALOG) {
			if (progress < 0 || progress > 1) { throw new CustomNPCsException("Progress has to be 0 or 1"); }
			boolean completed = data.dialogData.has(id);
			if (progress == 0 && completed) { data.dialogData.dialogsRead.remove(id); }
			else if (progress == 1 && !completed) { data.dialogData.read(id); }
			else { return; }
			// Message
			if (progress == 1) {
				String dialog = "dialog ID:" + id;
				IDialog d = DialogController.instance.get(id);
				if (d != null) { dialog = d.getName(); }
				if (questData.quest.showProgressInWindow && player instanceof EntityPlayerMP) {
					NBTTagCompound compound = new NBTTagCompound();
					compound.setInteger("QuestID", questData.quest.id);
					compound.setString("Type", "dialog");
					compound.setIntArray("Progress", new int[] { progress, 1 });
					compound.setString("TargetName", dialog);
					Packets.send((EntityPlayerMP) player, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
				}
				if (questData.quest.showProgressInChat) {
					player.sendMessage(Component.translatable("quest.message.dialog." + progress,
							Component.translatable(dialog).getFormattedText(), questData.quest.getTitle()).getParent());
				}
			}
			data.updateClient = true;
		}
		else if (type == EnumQuestTask.LOCATION) {
			if (progress < 0 || progress > 1) { throw new CustomNPCsException("Progress has to be 0 or 1"); }
			if (!questData.extraData.hasKey("Locations", 9)) {
				NBTTagList list = new NBTTagList();
				NBTTagCompound dataNBT = new NBTTagCompound();
				dataNBT.setString("Location", name);
				dataNBT.setBoolean("Found", progress == 1);
				dataNBT.setInteger("ObjectPos", objectPos);
				list.appendTag(dataNBT);
				questData.extraData.setTag("Locations", list);
			}
			else {
				boolean found = false;
				for (NBTBase dataNBT : questData.extraData.getTagList("Locations", 10)) {
					if (name.equalsIgnoreCase(((NBTTagCompound) dataNBT).getString("Location"))) {
						boolean completed = ((NBTTagCompound) dataNBT).getBoolean("Found");
						if ((completed && progress == 1) || (!completed && progress == 0)) { return; }
						((NBTTagCompound) dataNBT).setBoolean("Found", progress == 1);
						found = true;
						break;
					}
				}
				if (!found) {
					NBTTagCompound dataNBT = new NBTTagCompound();
					dataNBT.setString("Location", name);
					dataNBT.setBoolean("Found", progress == 1);
					dataNBT.setInteger("ObjectPos", objectPos);
					questData.extraData.getTagList("Locations", 10).appendTag(dataNBT);
				}
			}
			// Message
			if (progress == 1) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("QuestID", questData.quest.id);
				compound.setString("Type", "location");
				compound.setIntArray("Progress", new int[] { progress, 1 });
				compound.setString("TargetName", name);
				if (player instanceof EntityPlayerMP) {
					Packets.send((EntityPlayerMP) player, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
				}
				player.sendMessage(Component.translatable("quest.message.location." + progress,
						Component.translatable(name).getFormattedText(), questData.quest.getTitle()).getParent());
			}
			data.updateClient = true;
		}
		else if (type == EnumQuestTask.KILL || type == EnumQuestTask.AREAKILL || type == EnumQuestTask.MANUAL) {
			if (progress < 0 || progress > maxProgress) { throw new CustomNPCsException("Progress has to be between 0 and " + maxProgress); }
			HashMap<String, Integer> killed = getKilled(questData);
			if (!killed.containsKey(name) || killed.get(name) != progress) {
				String key = type == EnumQuestTask.MANUAL ? "manual" : "kill";
				// Message
				if (killed.get(name) < progress) {
					NBTTagCompound compound = new NBTTagCompound();
					compound.setInteger("QuestID", questData.quest.id);
					compound.setString("Type", key);
					compound.setIntArray("Progress", new int[] { progress, maxProgress });
					compound.setString("TargetName", name);
					if (player instanceof EntityPlayerMP) {
						Packets.send((EntityPlayerMP) player, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
					}
					player.sendMessage(Component.translatable("quest.message." + key + ".0",
							Component.translatable(name).getFormattedText(), "" + progress,
							"" + maxProgress, questData.quest.getTitle()).getParent());
				}
				killed.put(name, progress);
				setKilled(questData, killed);
				if (progress >= maxProgress) {
					player.sendMessage(Component.translatable("quest.message." + key + ".1",
							Component.translatable(name).getFormattedText(), questData.quest.getTitle()).getParent());
				}
				data.updateClient = true;
			}
		}
		else if (type == EnumQuestTask.CRAFT) {
			if (progress < 0 || progress > maxProgress) { throw new CustomNPCsException("Progress has to be between 0 and " + maxProgress); }
			HashMap<ItemStack, Integer> crafted = getCrafted(questData);
			for (ItemStack item : crafted.keySet()) {
				if (NoppesUtilPlayer.compareItems(item, item, ignoreDamage, ignoreNBT)) {
					if (crafted.get(item) != progress) { crafted.put(item, progress); }
					break;
				}
			}
			setCrafted(questData, crafted);
			// Message
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("QuestID", questData.quest.id);
			compound.setString("Type", "craft");
			compound.setIntArray("Progress", new int[] { progress, maxProgress });
			compound.setString("TargetName", item.getDisplayName());
			if (player instanceof EntityPlayerMP) {
				Packets.send((EntityPlayerMP) player, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
			}
			if (progress >= maxProgress) {
				player.sendMessage(Component.translatable("quest.message.craft.1",
						item.getDisplayName(), questData.quest.getTitle()).getParent());
			}
			else {
				player.sendMessage(Component.translatable("quest.message.craft.0",
						item.getDisplayName(), "" + progress, "" + maxProgress, questData.quest.getTitle()).getParent());
			}
			data.updateClient = true;
		}
		CustomNPCsScheduler.runTack(() -> {
			for (QuestObjective obj : questData.quest.getObjectives(player)) {
				if (obj.getEnumType() == type) { data.questData.checkQuestCompletion(player, questData); }
			}
		});
	}

	@Override
	public void setTargetID(int idIn) {
		if (idIn < 0) { throw new CustomNPCsException("Task ID must be greater than 0"); }
		id = idIn;
	}

	@Override
	public void setTargetName(String nameIn) { name = nameIn == null ? "" : nameIn; }

	public void setType(EnumQuestTask typeIn) { type = typeIn; }

	@Override
	public void setType(int typeIn) {
		if (typeIn < 0 || typeIn >= EnumQuestTask.values().length) { throw new CustomNPCsException("Type must be between 0 and " + (EnumQuestTask.values().length - 1)); }
		type = EnumQuestTask.values()[typeIn];
	}

	public void setObjectPos(int pos) { objectPos = ValueUtil.correctInt(pos, 0, 9); }

	public int getObjectPos() { return objectPos; }

}
