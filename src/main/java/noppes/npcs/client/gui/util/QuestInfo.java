package noppes.npcs.client.gui.util;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.api.IPos;
import noppes.npcs.client.gui.model.GuiCreationEntities;
import noppes.npcs.client.gui.player.GuiLog;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.constants.EnumRewardType;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.util.Util;

import java.util.*;

public class QuestInfo {

    protected final Map<Integer, List<String>> map = new TreeMap<>(); // [key, data texts]
    protected final Level level;
    public final QuestData qData;
    public final List<ItemStack> stacks = new ArrayList<>();
    public final Map<Integer, Entity> entitys = new TreeMap<>();
    public EntityNPCInterface npc;

    protected boolean newInstance = true;

    public QuestInfo(QuestData qd, Level levelIn) {
        level = levelIn;
        qData = qd;
        npc = qd.quest.completer.getNpc();
    }

    public Map<Integer, List<String>> getText(int first, Player player) {
        if (!newInstance && !map.isEmpty()) { return map; }
        map.clear();
        stacks.clear();
        entitys.clear();
        List<String> preLines = new ArrayList<>();
        // quest name
        preLines.add(Util.instance.getOldFormattedText(Component.translatable("gui.quest", ": ")) + ChatFormatting.BOLD +
                Util.instance.getOldFormattedText(Component.translatable(qData.quest.title)));
        // completion npc name
        if (qData.quest.completion == EnumQuestCompletion.Npc) {
            preLines.add(Util.instance.getOldFormattedText(Component.translatable("quest.completewith", qData.quest.completer.getName())));
        }
        // all objectives
        QuestObjective[] allObj = qData.quest.getObjectives(player);
        if (allObj.length > 0) {
            preLines.add("");
            preLines.add(ChatFormatting.BOLD + Util.instance.getOldFormattedText(Component.translatable("quest.objectives." + qData.quest.step)));
            String line;
            for (int i = 0; i < allObj.length; i++) {
                line = (i + 1) + "-";
                if (allObj[i].getEnumType() == EnumQuestTask.ITEM || allObj[i].getEnumType() == EnumQuestTask.CRAFT) {
                    stacks.add(allObj[i].getItemStack());
                    line += " " + ((char) 0xffff) + " ";
                }
                else if (allObj[i].getEnumType() == EnumQuestTask.KILL || allObj[i].getEnumType() == EnumQuestTask.AREAKILL) {
                    line += " " + ((char) 0xfffe) + " ";
                    if (allObj[i].isNotShowLogEntity()) { entitys.put(entitys.size(), null); }
                    else {
                        String target = allObj[i].getTargetName();
                        Entity entity = null;
                        for (EntityType<? extends Entity> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
                            Entity e = entityType.create(player.level());
                            if (e instanceof LivingEntity && (e.getClass().getSimpleName().equals(target) ||
                                    e.getClass().getSimpleName().equals(allObj[i].entityClass) ||
                                    e.getName().getString().equals(target))) {
                                entity = e;
                                break;
                            }
                        }
                        if (entity == null) {
                            IPos pos = allObj[i].getCompassPos();
                            if (level.dimension().location().toString().equals(allObj[i].getCompassDimension())) {
                                int r = allObj[i].getCompassRange();
                                List<Entity> list = level.getEntities(null, new AABB(pos.getX() - r, pos.getY() - r, pos.getZ() - r, pos.getX() + r, pos.getY() + r, pos.getZ() + r));
                                for (Entity en : list) {
                                    if (en.getName().getString().equals(target) ||
                                            en.getClass().getSimpleName().equals(allObj[i].entityClass)) {
                                        CompoundTag compound = new CompoundTag();
                                        en.save(compound);
                                        Optional<Entity> type = EntityType.create(compound, level);
                                        if (type.isPresent() && type.get() instanceof EntityNPCInterface npcEntity) {
                                            entity = Util.instance.copyToGUI(npcEntity, level, false);
                                            break;
                                        }
                                    }
                                }
                            }
                        } // found in dimension
                        if (entity == null) {
                            for (Map.Entry<Component, EntityType<? extends Entity>> entry : GuiCreationEntities.getAllEntities(level, true).entrySet()) {
                                if (entry.getKey().getString().equals(target)) {
                                    entity = entry.getValue().create(level);
                                    break;
                                }
                                else if (!allObj[i].entityClass.isEmpty()) {
                                    Entity e = entry.getValue().create(level);
                                    if (e != null && e.getClass().getSimpleName().equals(allObj[i].entityClass)) {
                                        entity = e;
                                        break;
                                    }
                                }
                            }
                        } // is class set
                        entitys.put(entitys.size(), entity);
                    }
                }
                preLines.add(line + allObj[i].getText());
            }
        }
        preLines.addAll(qData.quest.getLogText());
        List<Quest.TempDropData> listTdd = new ArrayList<>();
        for (int i = 0; i < qData.quest.rewardItems.size(); i++) {
            DropSet ds = qData.quest.rewardItems.get(i);
            if (!ds.item.isEmpty()) {
                boolean has = false;
                if (qData.quest.rewardType == EnumRewardType.ALL) {
                    for (Quest.TempDropData tdd : listTdd) {
                        if (ItemStack.isSameItemSameTags(ds.item, tdd.getStack())) {
                            tdd.add(ds);
                            has = true;
                            break;
                        }
                    }
                }
                if (!has) { listTdd.add(new Quest.TempDropData(ds)); }
            }
        }
        if (!listTdd.isEmpty()) {
            for (Quest.TempDropData tdd : listTdd) {
                stacks.add(tdd.getStack());
            }
        }

        int currentList = 0;
        List<String> lines = new ArrayList<>();
        for (String sentence : preLines) { lines.addAll(GuiLog.createLines(sentence)); }

        List<String> list = new ArrayList<>();
        float height = 147.0f * GuiLog.scaleH;
        for (String l : lines) {
            if ((list.size() * GuiLog.fontHeight) > height - (currentList == 0 ? first : 0)) {
                map.put(currentList, list);
                list = new ArrayList<>();
                currentList++;
            }
            list.add(l);
        }
        if (!list.isEmpty()) { map.put(currentList, list); }
        newInstance = false;

        List<ItemStack> rewarList = new ArrayList<>();
        for (int i = 0; i < qData.quest.rewardItems.size(); i++) {
            ItemStack stack = qData.quest.rewardItems.get(i).getMCItemStack();
            if (stack.isEmpty()) { continue; }
            boolean has = false;
            if (qData.quest.rewardType == EnumRewardType.ALL) {
                for (ItemStack it : rewarList) {
                    if (ItemStack.isSameItemSameTags(stack, it)) {
                        has = true;
                        break;
                    }
                }
            }
            if (!has) { rewarList.add(stack); }
        }
        if (!rewarList.isEmpty()) { stacks.addAll(rewarList); }
        return map;
    }

    public void reset() { newInstance = true; }

    public int size() { return map.size(); }

}
