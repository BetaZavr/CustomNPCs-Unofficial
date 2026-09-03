package noppes.npcs.client.gui.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
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
    protected final World world;
    public final QuestData qData;
    public final List<ItemStack> stacks = new ArrayList<>();
    public final Map<Integer, Entity> entitys = new TreeMap<>();
    public EntityNPCInterface npc;

    protected boolean newInstance = true;

    public QuestInfo(QuestData qd, World worldIn) {
        world = worldIn;
        qData = qd;
        npc = qd.quest.completer.getNpc();
    }

    public Map<Integer, List<String>> getText(int first, EntityPlayer player) {
        if (!newInstance && !map.isEmpty()) { return map; }
        map.clear();
        stacks.clear();
        entitys.clear();
        List<String> preLines = new ArrayList<>();
        // quest name
        preLines.add(Component.translatable("gui.quest", ": ").getFormattedText() + TextFormatting.BOLD +
                Component.translatable(qData.quest.title).getFormattedText());
        // completion npc name
        if (qData.quest.completion == EnumQuestCompletion.Npc && !qData.quest.completer.isEmpty()) {
            preLines.add(Component.translatable("quest.completewith", qData.quest.completer.getName()).getFormattedText());
        }
        // all objectives
        QuestObjective[] allObj = qData.quest.getObjectives(player);
        if (allObj.length > 0) {
            preLines.add("");
            preLines.add(TextFormatting.BOLD + Component.translatable("quest.objectives." + qData.quest.step).getFormattedText());
            String line;
            for (int i = 0; i < allObj.length; i++) {
                line = (i + 1) + "-";
                if (allObj[i].getEnumType() == EnumQuestTask.ITEM || allObj[i].getEnumType() == EnumQuestTask.CRAFT) {
                    stacks.add(allObj[i].getItemStack());
                    line += " " + ((char) 0xffff) + " ";
                }
                else if (allObj[i].getEnumType() == EnumQuestTask.KILL ||
                        allObj[i].getEnumType() == EnumQuestTask.AREAKILL) {
                    line += " " + ((char) 0xfffe) + " ";
                    if (allObj[i].isNotShowLogEntity()) { entitys.put(entitys.size(), null); }
                    else {
                        String target = allObj[i].getTargetName();
                        Entity entity = EntityList.createEntityByIDFromName(new ResourceLocation(target), world);
                        if (entity == null) {
                            IPos pos = allObj[i].getCompassPos();
                            if (world.provider.getDimension() == allObj[i].getCompassDimension()) {
                                int r = allObj[i].getCompassRange();
                                List<Entity> list = new ArrayList<>();
                                try {
                                    list = world.getEntitiesWithinAABB(Entity.class,
                                            new AxisAlignedBB(pos.getX() - r, pos.getY() - r, pos.getZ() - r,
                                                    pos.getX() + r, pos.getY() + r, pos.getZ() + r));
                                }
                                catch (Exception ignored) { }
                                for (Entity en : list) {
                                    if (en.getName().equals(target) ||
                                            en.getClass().getSimpleName().equals(allObj[i].entityClass)) {
                                        NBTTagCompound compound = new NBTTagCompound();
                                        en.writeToNBTAtomically(compound);
                                        Entity e = EntityList.createEntityFromNBT(compound, world);
                                        if (e instanceof EntityNPCInterface) {
                                            entity = Util.instance.copyToGUI((EntityNPCInterface) e, world, false);
                                        }
                                        break;
                                    }
                                }
                            }
                        } // found in dimension
                        if (entity == null) {
                            for (Map.Entry<Component, EntityEntry> entry : GuiCreationEntities.getAllEntities(true).entrySet()) {
                                if (entry.getKey().getString().equals(target) ||
                                        entry.getValue().getEntityClass().getSimpleName().equals(allObj[i].entityClass)) {
                                    entity = entry.getValue().newInstance(world);
                                    break;
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
                        if (ds.item.isItemEqual(tdd.stack) && ItemStack.areItemStackShareTagsEqual(ds.item, tdd.stack)) {
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
                    if (it.isItemEqual(stack) && ItemStack.areItemStackShareTagsEqual(it, stack)) {
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
