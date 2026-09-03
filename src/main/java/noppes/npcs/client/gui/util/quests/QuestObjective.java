package noppes.npcs.client.gui.util.quests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomNpcs;
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
import noppes.npcs.controllers.DimensionController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

// New from Unofficial (BetaZavr)
public class QuestObjective implements IQuestObjective {

    private final int parentID;
    private final Player player;
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
    public ResourceLocation dimension = new ResourceLocation("minecraft", "any");
    public int rangeCompass = 5;
    public int colorCompass = (int) (Math.random() * 16777215.0) | 0xFF000000;
    public BlockPos pos = BlockPos.ZERO;
    public String compassEntityName = "";
    public String entityClass = "";

    public QuestObjective(int parentIDIn, int objectPosIn, Player playerIn) {
        parentID = parentIDIn;
        player = playerIn;
        objectPos = objectPosIn;
    }

    public QuestObjective(int parentID, int objectPosIn, EnumQuestTask typeIn) {
        this(parentID, objectPosIn, (Player) null);
        type = typeIn;
    }

    public QuestObjective copyToPlayer(Player player) {
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
        newObj.compassEntityName = compassEntityName;
        newObj.entityClass = entityClass;
        return newObj;
    }

    @Override
    public int getAreaRange() { return range; }

    @Override
    public String getCompassDimension() { return dimension == null ? "null" : dimension.toString(); }

    @Override
    public IPos getCompassPos() { return Objects.requireNonNull(NpcAPI.Instance()).getIPos(pos); }

    @Override
    public int getCompassRange() { return rangeCompass; }

    @Override
    public int getCompassColor() { return colorCompass; }

    public HashMap<ItemStack, Integer> getCrafted(QuestData data) {
        if (!data.extraData.contains("Crafts", 9)) { data.extraData.put("Crafts", new ListTag()); }
        HashMap<ItemStack, Integer> map = new HashMap<>();
        for (int i = 0; i < data.extraData.getList("Crafts", 10).size(); ++i) {
            CompoundTag compound = data.extraData.getList("Crafts", 10).getCompound(i);
            if (compound.getInt("ObjectPos") != objectPos) { continue; }
            map.put(ItemStack.of(compound.getCompound("Item")), compound.getInt("Value"));
        }
        return map;
    }

    public EnumQuestTask getEnumType() { return type; }

    @Override
    public IItemStack getItem() { return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(item); }

    public ItemStack getItemStack() { return item; }

    public HashMap<String, Integer> getKilled(QuestData data) {
        if (!data.extraData.contains("Targets", 9)) { data.extraData.put("Targets", new ListTag()); }
        HashMap<String, Integer> map = new HashMap<>();
        for(int i = 0; i < data.extraData.getList("Targets", 10).size(); ++i) {
            CompoundTag compound = data.extraData.getList("Targets", 10).getCompound(i);
            if (compound.getInt("ObjectPos") != objectPos) { continue; }
            map.put(compound.getString("Slot"), compound.getInt("Value"));
        }
        return map;
    }

    @Override
    public int getMaxProgress() { return type == EnumQuestTask.DIALOG || type == EnumQuestTask.LOCATION ? 1 : maxProgress; }

    public CompoundTag getNBT() {
        CompoundTag nbtTask = new CompoundTag();
        nbtTask.putInt("Type", type.ordinal());
        nbtTask.putBoolean("SetPointOnMiniMap", setPointOnMiniMap);
        CompoundTag nbtCompass = new CompoundTag();
        nbtCompass.putIntArray("Pos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
        nbtCompass.putString("Dimension", dimension == null ? "minecraft:any" : dimension.toString());
        nbtCompass.putInt("RegionID", regionID);
        nbtCompass.putInt("Range", rangeCompass);
        nbtCompass.putInt("Color", colorCompass);
        nbtCompass.putInt("ObjectPos", objectPos);
        nbtCompass.putString("EntityName", compassEntityName);
        nbtCompass.putString("EntityClass", entityClass);
        nbtTask.put("CompassData", nbtCompass);
        if (maxProgress > 0) { nbtTask.putInt("Progress", maxProgress); }
        if (id > 0) { nbtTask.putInt("TargetID", id); }
        if (!name.isEmpty()) {
            nbtTask.putString("TargetName", name);
            nbtTask.putBoolean("TargetPart", partName);
            nbtTask.putBoolean("TargetTitle", andTitle);
            nbtTask.putBoolean("NotShowLogEntity", notShowLogEntity);
        }
        if (type == EnumQuestTask.AREAKILL) { nbtTask.putInt("Range", range); }
        if (!item.isEmpty()) {
            nbtTask.put("Item", item.save(new CompoundTag()));
            nbtTask.putBoolean("LeaveItem", leaveItem);
            nbtTask.putBoolean("IgnoreDamage", ignoreDamage);
            nbtTask.putBoolean("IgnoreNBT", ignoreNBT);
        }
        return nbtTask;
    }

    @Override
    public String getOrientationEntityName() { return compassEntityName; }

    @Override
    public int getProgress() {
        if (type == EnumQuestTask.ITEM) {
            int count = 0;
            for (int i = 0; i < Objects.requireNonNull(player).getInventory().getContainerSize(); ++i) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!NoppesUtilServer.isItemStackNull(item) && NoppesUtilPlayer.compareItems(item, stack, ignoreDamage, ignoreNBT)) {
                    count += stack.getCount();
                }
            }
            return ValueUtil.correctInt(count, 0, maxProgress);
        }
        PlayerData data = PlayerData.get(player);
        if (type == EnumQuestTask.DIALOG) { return data.dialogData.has(id) ? 1 : 0; }
        QuestData questData = data.questData.activeQuests.get(parentID);
        if (type == EnumQuestTask.LOCATION) {
            for (Tag dataNBT : questData.extraData.getList("Locations", 10)) {
                if (name.equalsIgnoreCase(((CompoundTag) dataNBT).getString("Location"))) {
                    return ((CompoundTag) dataNBT).getBoolean("Found") ? 1 : 0;
                }
            }
            return 0;
        }
        if (questData == null) { return 0; }
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
    public Component getMCText() {
        MutableComponent text;
        boolean bo = isCompleted();
        if (type == EnumQuestTask.ITEM || type == EnumQuestTask.CRAFT) {
            text = Component.empty()
                    .append(item.getHoverName())
                    .append(Component.literal(": ").withStyle(ChatFormatting.RESET))
                    .append(Component.literal("" + getProgress()).withStyle(bo ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED))
                    .append(Component.literal("/" + getMaxProgress() + " ").withStyle(ChatFormatting.RESET));
            text.append(Component.translatable("quest.task." + (type == EnumQuestTask.ITEM ? "item" : "craft") + "."+(isCompleted() ? "0" : "1")));
            if (leaveItem) { text.append(Component.translatable("quest.take.log")); }
        } // Collect Item or Craft Item
        else if (type == EnumQuestTask.DIALOG) {
            MutableComponent name = Component.literal("null");
            Dialog dialog = DialogController.instance.dialogs.get(id);
            if (dialog != null) { name = Component.translatable(dialog.title); }
            text = Component.empty()
                    .append(name.withStyle(bo ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED))
                    .append(Component.translatable("quest.task.dialog." + (bo ? "0" : "1")).withStyle(ChatFormatting.RESET));
        } // Dialog
        else if (type == EnumQuestTask.KILL || type == EnumQuestTask.AREAKILL) {
            text = Component.translatable("entity." + name + ".name");
            Level level = CustomNpcs.proxy.getOverWorld();
            if (!entityClass.isEmpty() && level != null) {
                for (EntityType<?> entry : ForgeRegistries.ENTITY_TYPES.getValues()) {
                    try {
                        Entity e = entry.create(level);
                        if (e != null && e.getClass().getSimpleName().equals(entityClass)) {
                            text = Component.literal(e.getName().getString());
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (text.getString().contains("entity.") && text.getString().indexOf(".name") > 0) {
                text = Component.literal(name);
            }
            text.append(Component.literal(" " + getProgress()).withStyle(bo ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED))
                    .append(Component.literal("/" + getMaxProgress() + " ").withStyle(ChatFormatting.RESET));
            text.append(Component.translatable("quest.task.kill."+(isCompleted() ? "0" : "1")));
        } // Kill
        else if (type == EnumQuestTask.LOCATION) {
            text = Component.empty()
                    .append(Component.translatable(name).withStyle(bo ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED))
                    .append(Component.literal(": ").withStyle(ChatFormatting.RESET))
                    .append(Component.translatable("quest.task.location." + (bo ? "0" : "1"))).withStyle(ChatFormatting.RESET);
        } // Location
        else if (type == EnumQuestTask.MANUAL) {
            text = Component.empty()
                    .append(Component.translatable(name).withStyle(bo ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED))
                    .append(Component.literal(": ").withStyle(ChatFormatting.RESET))
                    .append(Component.literal("" + getProgress()).withStyle(bo ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED))
                    .append(Component.literal("/" + getMaxProgress() + " ").withStyle(ChatFormatting.RESET));
            text.append(Component.translatable("quest.task.manual."+(isCompleted() ? "0" : "1")));
        } // Manual
        else { text = Component.literal("null type: " + type + " #" + toString().substring(toString().indexOf("@") + 1)); }
        return text;
    }

    @Override
    public String getText() { return Util.instance.getOldFormattedText(getMCText()); }

    @Override
    public int getType() { return type.ordinal(); }

    @Override
    public boolean isAndTitle() { return andTitle; }

    @Override
    public boolean isCompleted() {
        if (type == EnumQuestTask.ITEM) { return NoppesUtilPlayer.compareItems(player, item, ignoreDamage, ignoreNBT, maxProgress); }
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

    public void load(CompoundTag nbtTask) {
        type = EnumQuestTask.values()[nbtTask.getInt("Type")];
        setPointOnMiniMap = nbtTask.getBoolean("SetPointOnMiniMap");
        objectPos = nbtTask.getInt("ObjectPos");
        if (nbtTask.contains("CompassData", 10)) {
            CompoundTag nbtCompass = nbtTask.getCompound("CompassData");
            int[] bp = nbtCompass.getIntArray("Pos");
            pos = new BlockPos(bp[0], bp[1], bp[2]);
            String dimLoc = nbtCompass.getString("Dimension");
            if (!dimLoc.contains(":")) { dimLoc = "minecraft:any"; }
            dimension = new ResourceLocation(dimLoc);
            if (nbtCompass.contains("RegionID", 3)) { regionID = nbtCompass.getInt("RegionID"); }
            if (nbtCompass.contains("Color", 3)) { colorCompass = nbtCompass.getInt("Color"); }
            rangeCompass = nbtCompass.getInt("Range");
            compassEntityName = nbtCompass.getString("EntityName");
        }
        if (nbtTask.contains("Progress", 3)) { setMaxProgress(nbtTask.getInt("Progress")); }
        if (nbtTask.contains("TargetID", 3)) { setTargetID(nbtTask.getInt("TargetID")); }
        if (nbtTask.contains("TargetName", 8)) {
            entityClass = nbtTask.getString("EntityClass");
            setTargetName(nbtTask.getString("TargetName"));
            partName = nbtTask.getBoolean("TargetPart");
            andTitle = nbtTask.getBoolean("TargetTitle");
            notShowLogEntity = nbtTask.getBoolean("NotShowLogEntity");
        }
        if (nbtTask.contains("Range", 3)) { setAreaRange(nbtTask.getInt("Range")); }
        if (nbtTask.contains("Item", 10)) {
            setItem(ItemStack.of(nbtTask.getCompound("Item")));
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
    public void setCompassDimension(String dimensionId) {
        ResourceLocation location = new ResourceLocation(NoppesUtilServer.validLocation(dimensionId));
        if (CustomNpcs.Server == null) {
            if (DimensionController.has(ResourceKey.create(Registries.DIMENSION, location))) { dimension = location; }
            return;
        }
        else {
            for (ResourceKey<Level> key : CustomNpcs.Server.levelKeys()) {
                if (key.location().equals(location)) {
                    dimension = location;
                    return;
                }
            }
        }
        throw new CustomNPCsException("Dimension: \"" + dimensionId + "\" not found");
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
        colorCompass = color & 0xFFFFFF;
    }

    public void setCrafted(QuestData data, HashMap<ItemStack, Integer> crafted) {
        ListTag nbtList = data.extraData.getList("Crafts", 10);
        if (crafted != null) {
            Set<ItemStack> sets = new HashSet<>();
            for(int i = 0; i < nbtList.size(); ++i) {
                CompoundTag compound = nbtList.getCompound(i);
                ItemStack item = ItemStack.of(compound.getCompound("Item"));
                if (compound.getInt("ObjectPos") == objectPos) {
                    for (ItemStack craft : crafted.keySet()) {
                        if (NoppesUtilPlayer.compareItems(item, craft, ignoreDamage, ignoreNBT)) {
                            compound.putInt("Value", crafted.get(craft));
                            sets.add(craft);
                        }
                    }
                }
            }
            for (ItemStack item : crafted.keySet()) {
                if (sets.contains(item)) { continue; }
                CompoundTag compound = new CompoundTag();
                compound.put("Item", item.save(new CompoundTag()));
                compound.putInt("Value", crafted.get(item));
                compound.putInt("ObjectPos", objectPos);
                nbtList.add(compound);
            }
        }
        data.extraData.put("Crafts", nbtList);
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
        ListTag nbtList = data.extraData.getList("Targets", 10);
        if (killed != null) {
            Set<String> sets = new HashSet<>();
            for(int i = 0; i < nbtList.size(); ++i) {
                CompoundTag compound = nbtList.getCompound(i);
                String slot = compound.getString("Slot");
                if (compound.getInt("ObjectPos") == objectPos && killed.containsKey(slot)) {
                    compound.putInt("Value", killed.get(slot));
                    sets.add(slot);
                }
            }
            for (String slot : killed.keySet()) {
                if (sets.contains(slot)) { continue; }
                CompoundTag compound = new CompoundTag();
                compound.putString("Slot", slot);
                compound.putInt("Value", killed.get(slot));
                compound.putInt("ObjectPos", objectPos);
                nbtList.add(compound);
            }
        }
        data.extraData.put("Targets", nbtList);
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
    public void setOrientationEntityName(String name) { compassEntityName = name; }

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
                if (questData.quest.showProgressInWindow && player instanceof ServerPlayer sPlayer) {
                    CompoundTag compound = new CompoundTag();
                    compound.putInt("QuestID", questData.quest.id);
                    compound.putString("Type", "dialog");
                    compound.putIntArray("Progress", new int[] { progress, 1 });
                    compound.putString("TargetName", dialog);
                    Packets.send(sPlayer, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
                }
                if (questData.quest.showProgressInChat) {
                    player.sendSystemMessage(Component.translatable("quest.message.dialog." + progress,
                            Component.translatable(dialog).getString(), questData.quest.getTitle()));
                }
            }
            data.updateClient = true;
        }
        else if (type == EnumQuestTask.LOCATION) {
            if (progress < 0 || progress > 1) { throw new CustomNPCsException("Progress has to be 0 or 1"); }
            if (!questData.extraData.contains("Locations", 9)) {
                ListTag list = new ListTag();
                CompoundTag dataNBT = new CompoundTag();
                dataNBT.putString("Location", name);
                dataNBT.putBoolean("Found", progress == 1);
                dataNBT.putInt("ObjectPos", objectPos);
                list.add(dataNBT);
                questData.extraData.put("Locations", list);
            }
            else {
                boolean found = false;
                for (Tag dataNBT : questData.extraData.getList("Locations", 10)) {
                    if (name.equalsIgnoreCase(((CompoundTag) dataNBT).getString("Location"))) {
                        boolean completed = ((CompoundTag) dataNBT).getBoolean("Found");
                        if ((completed && progress == 1) || (!completed && progress == 0)) { return; }
                        ((CompoundTag) dataNBT).putBoolean("Found", progress == 1);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    CompoundTag dataNBT = new CompoundTag();
                    dataNBT.putString("Location", name);
                    dataNBT.putBoolean("Found", progress == 1);
                    dataNBT.putInt("ObjectPos", objectPos);
                    questData.extraData.getList("Locations", 10).add(dataNBT);
                }
            }
            // Message
            if (progress == 1) {
                CompoundTag compound = new CompoundTag();
                compound.putInt("QuestID", questData.quest.id);
                compound.putString("Type", "location");
                compound.putIntArray("Progress", new int[] { progress, 1 });
                compound.putString("TargetName", name);
                if (player instanceof ServerPlayer sPlayer) {
                    Packets.send(sPlayer, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
                }
                player.sendSystemMessage(Component.translatable("quest.message.location." + progress,
                        Component.translatable(name).getString(), questData.quest.getTitle()));
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
                    CompoundTag compound = new CompoundTag();
                    compound.putInt("QuestID", questData.quest.id);
                    compound.putString("Type", key);
                    compound.putIntArray("Progress", new int[] { progress, maxProgress });
                    compound.putString("TargetName", name);
                    if (player instanceof ServerPlayer sPlayer) {
                        Packets.send(sPlayer, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
                    }
                    player.sendSystemMessage(Component.translatable("quest.message." + key + ".0",
                            Component.translatable(name).getString(), "" + progress,
                            "" + maxProgress, questData.quest.getTitle()));
                }
                killed.put(name, progress);
                setKilled(questData, killed);
                if (progress >= maxProgress) {
                    player.sendSystemMessage(Component.translatable("quest.message." + key + ".1",
                            Component.translatable(name).getString(), questData.quest.getTitle()));
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
            CompoundTag compound = new CompoundTag();
            compound.putInt("QuestID", questData.quest.id);
            compound.putString("Type", "craft");
            compound.putIntArray("Progress", new int[] { progress, maxProgress });
            compound.put("Item", item.save(new CompoundTag()));
            if (player instanceof ServerPlayer sPlayer) {
                Packets.send(sPlayer, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
            }
            if (progress >= maxProgress) {
                player.sendSystemMessage(Component.translatable("quest.message.craft.1",
                        item.getDisplayName(), questData.quest.getTitle()));
            }
            else {
                player.sendSystemMessage(Component.translatable("quest.message.craft.0",
                        item.getDisplayName(), "" + progress, "" + maxProgress, questData.quest.getTitle()));
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
    public void setTargetName(String nameIn) {
        name = nameIn == null ? "" : nameIn;
        Level level = CustomNpcs.proxy.getOverWorld();
        if (entityClass.isEmpty() && !name.isEmpty() && level != null) {
            for (EntityType<?> entry : ForgeRegistries.ENTITY_TYPES.getValues()) {
                try {
                    Entity e = entry.create(level);
                    if (e != null && e.getClass().getSimpleName().equals(name)) {
                        entityClass = e.getClass().getSimpleName();
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public void setType(EnumQuestTask typeIn) { type = typeIn; }

    @Override
    public void setType(int typeIn) {
        if (typeIn < 0 || typeIn >= EnumQuestTask.values().length) { throw new CustomNPCsException("Type must be between 0 and " + (EnumQuestTask.values().length - 1)); }
        type = EnumQuestTask.values()[typeIn];
    }

    public void setObjectPos(int pos) { objectPos = ValueUtil.correctInt(pos, 0, 9); }

    public int getObjectPos() { return objectPos; }

}
