package noppes.npcs.client.gui.yellow_de.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.client.gui.yellow_de.data.nodes.*;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class YDEData {

    public final Map<Integer, YDENode> nodes = new TreeMap<>();
    public final List<YDELink> links = new ArrayList<>();

    public YDEData() {
        check();
        resetYpos();
    }

    public YDEData(NBTTagCompound compound) {
        NBTTagList listNodes = compound.getTagList("Nodes", 10);
        for (int i = 0; i < listNodes.tagCount(); i++) {
            NBTTagCompound nbt = listNodes.getCompoundTagAt(i);
            YDENode node;
            switch (EnumYDEType.values()[ValueUtil.onlyPositiveInt(nbt.getInteger("Type"), EnumYDEType.values().length)]) {
                case CATEGORY: node = new YDECategory(this, -1, ""); break;
                case NPC: node = new YDENpc(this, -1, "", null); break;
                case OPTION: node = new YDEOption(this, -1, "", -1, new DialogOption()); break;
                case QUEST: node = new YDEQuest(this, -1, "", -1); break;
                case AREA: node = new YDEArea(this, -1, "", ""); break;
                default: node = new YDEDialog(this, -1, "", -1); break;
            }
            try {
                node.load(listNodes.getCompoundTagAt(i));
                nodes.put(node.id, node);
            }
            catch (Exception e) { LogWriter.error(e); }
        }
        NBTTagList listLinks = compound.getTagList("Links", 10);
        for (int i = 0; i < listLinks.tagCount(); i++) {
            YDELink link = new YDELink(0, 0, EnumYDEType.DIALOG);
            link.load(listLinks.getCompoundTagAt(i));
            links.add(link);
        }
    }

    public NBTTagCompound save() {
        NBTTagCompound compound = new NBTTagCompound();

        NBTTagList listNodes = new NBTTagList();
        for (YDENode node : new ArrayList<>(nodes.values())) { listNodes.appendTag(node.save()); }
        compound.setTag("Nodes", listNodes);

        NBTTagList listLinks = new NBTTagList();
        for (YDELink link : new ArrayList<>(links)) { listLinks.appendTag(link.save()); }
        compound.setTag("Links", listLinks);

        return compound;
    }

    public int getEmptyNodeId() {
        int id = 0;
        while (nodes.containsKey(id)) { id++; }
        return id;
    }

    public YDEData check() {
        // process categories
        for (DialogCategory category : new ArrayList<>(DialogController.instance.categories.values())) {
            YDECategory yde_category = null;
            for (YDENode node : new ArrayList<>(nodes.values())) {
                if (node instanceof YDECategory) {
                    if (node.category.equals(category.title)) { yde_category = ((YDECategory) node); }
                }
            }
            if (yde_category == null) {
                yde_category = new YDECategory(this, getEmptyNodeId(), category.title);
                if (category.id > -1) { yde_category.categoryId = category.id; }
            }
            nodes.put(yde_category.id, yde_category);
        }
        // process dialogues
        Set<Integer> sets = new HashSet<>();
        for (YDENode node : new ArrayList<>(nodes.values())) {
            if (node instanceof YDEDialog) {
                if (((YDEDialog) node).dialogId > -1 &&
                        DialogController.instance.get(((YDEDialog) node).dialogId) == null || sets.contains(((YDEDialog) node).dialogId)) {
                    nodes.remove(node.id);
                } else { sets.add(((YDEDialog) node).dialogId); }
            }
        }
        for (DialogCategory category : new ArrayList<>(DialogController.instance.categories.values())) {
            for (Dialog dialog : new ArrayList<>(category.dialogs.values())) {
                YDEDialog yde_dialog = getDialog(dialog);
                if (yde_dialog == null) {
                    yde_dialog = new YDEDialog(this, getEmptyNodeId(), category.title, dialog.id);
                    nodes.put(yde_dialog.id, yde_dialog);
                    processDialog(yde_dialog, 0, getLastY(yde_dialog));
                }
            }
        }
        return this;
    }

    private void processDialog(YDEDialog yde_dialog, int x, int y) {
        yde_dialog.x = x;
        yde_dialog.y = y;
        if (yde_dialog.dialog == null) { yde_dialog.dialog = DialogController.instance.get(yde_dialog.dialogId); }
        if (yde_dialog.dialog != null) {
            if (yde_dialog.dialog.quest > -1) {
                YDEQuest yde_quest = getQuest(yde_dialog.category, yde_dialog.dialog.quest);
                if (yde_quest == null) {
                    yde_quest = new YDEQuest(this, getEmptyNodeId(), yde_dialog.category, yde_dialog.dialog.quest);
                    yde_quest.x = x + 45;
                    yde_quest.y = yde_dialog.y + 140;
                    nodes.put(yde_quest.id, yde_quest);
                }
                links.add(new YDELink(yde_dialog.id, yde_quest.id, EnumYDEType.QUEST));
            }
            if (!yde_dialog.dialog.startedNpcs.isEmpty()) {
                for (Dialog.StartedNpcData npcData : new ArrayList<>(yde_dialog.dialog.startedNpcs)) {
                    YDENpc yde_npc = getNpc(yde_dialog.category, npcData);
                    if (yde_npc == null) {
                        yde_npc = new YDENpc(this, getEmptyNodeId(), yde_dialog.category, npcData);
                        yde_npc.x = x - 100;
                        yde_npc.y = getLastY(yde_npc);
                        nodes.put(yde_npc.id, yde_npc);
                    }
                    links.add(new YDELink(yde_dialog.id, yde_npc.id, EnumYDEType.NPC));
                }
            }
            if (!yde_dialog.dialog.options.isEmpty()) {
                x += 200;
                for (Map.Entry<Integer, DialogOption> entry : new ArrayList<>(yde_dialog.dialog.options.entrySet())) {
                    entry.getValue().slot = entry.getKey();
                    YDEOption yde_option = getOption(entry.getValue());
                    if (yde_option == null) {
                        yde_option = new YDEOption(this, getEmptyNodeId(), yde_dialog.category, yde_dialog.dialogId, entry.getValue());
                        yde_option.x = x;
                        yde_option.y = getLastY(yde_option);
                        nodes.put(yde_option.id, yde_option);
                        if (entry.getValue().hasDialogs()) {
                            for (DialogOption.OptionDialogID optionDialog : new ArrayList<>(entry.getValue().dialogs)) {
                                YDEDialog yde_next_dialog = getDialog(optionDialog.dialogId);
                                if (yde_next_dialog == null) {
                                    yde_next_dialog = new YDEDialog(this, getEmptyNodeId(), yde_dialog.category, optionDialog.dialogId);
                                    Dialog nextDialog = DialogController.instance.get(optionDialog.dialogId);
                                    if (nextDialog == null) {
                                        DialogCategory category = DialogController.instance.getCategory(yde_dialog.category);
                                        nextDialog = new Dialog(category);
                                        nextDialog.id = optionDialog.dialogId;
                                        DialogController.instance.saveDialog(category, nextDialog);
                                    }
                                    links.add(new YDELink(yde_option.id, yde_next_dialog.id, EnumYDEType.OPTION));
                                    nodes.put(yde_next_dialog.id, yde_next_dialog);
                                    processDialog(yde_next_dialog, x + 200, yde_option.y);
                                } // Dialogue not found in mod data
                            }
                        }
                    }
                    links.add(new YDELink(yde_dialog.id, yde_option.id, EnumYDEType.DIALOG));
                }
            }
        }
    }

    private int getLastY(YDENode node) {
        int yMax = 0;
        for (YDENode n : new ArrayList<>(nodes.values())) {
            if (n instanceof YDECategory || n instanceof YDEArea || n.equals(node)) { continue; }
            if (n.x + n.width >= node.x && n.x < node.x + node.width) {
                int y = n.y + n.height + 20;
                if (yMax < y) { yMax = y; }
            }
        }
        return yMax;
    }

    private void resetYpos() {
        List<Map<Integer, YDENode>> tempList = new ArrayList<>();
        int x = 0;
        // rows
        while (true) {
            Map<Integer, YDENode> map = new TreeMap<>();
            for (YDENode node : new ArrayList<>(nodes.values())) {
                if (node instanceof YDECategory || node instanceof YDEArea) { continue; }
                boolean added = true;
                for (Map<Integer, YDENode> tempNodes : tempList) {
                    for (YDENode n : tempNodes.values()) {
                        if (n.equals(node)) {
                            added = false;
                            break;
                        }
                    }
                }
                if (added && node.x + node.width >= x && node.x < x + 200) { map.put(node.y, node); }
            }
            if (map.isEmpty()) { break; }
            else {
                tempList.add(map);
                x += 200;
            }
        }
        // y sets
        int yCenter = 0;
        for (Map<Integer, YDENode> tempNodes : tempList) {
            List<YDENode> hasLinks = new ArrayList<>();
            int y = 0;
            for (YDENode node : tempNodes.values()) {
                y += node.height + 20;
                if ((node instanceof YDEDialog || node instanceof YDEOption) && !getFromLinks(node.id).isEmpty()) { hasLinks.add(node); }
            }
            y -= 20;
            y /= -2;
            for (YDENode node : tempNodes.values()) {
                node.y = yCenter + y;
                y += node.height + 20;
            }
            if (!hasLinks.isEmpty()) {
                yCenter = 0;
                for (YDENode node : hasLinks) { yCenter += node.y + node.height / 2; }
                if (hasLinks.size() > 1) { yCenter /= 2; }
            }
        }
    }

    @SuppressWarnings("unused")
    public YDEArea getArea(String category, int areaId) {
        for (YDENode node : new ArrayList<>(nodes.values())) {
            if (node instanceof YDEArea && node.category.equals(category) && node.id == areaId) { return (YDEArea) node; }
        }
        return null;
    }

    public YDEOption getOption(DialogOption optionIn) {
        for (YDENode node : new ArrayList<>(nodes.values())) {
            if (node.type == EnumYDEType.OPTION && node instanceof YDEOption &&
                    ((YDEOption) node).option.equals(optionIn)) { return (YDEOption) node; }
        }
        return null;
    }

    public YDEOption createOption(@Nonnull String categoryTitle, @Nonnull DialogOption dialogOption, @Nullable Dialog dialog) {
        YDEOption yde_option = new YDEOption(this, getEmptyNodeId(), categoryTitle, dialog == null ? -1 : dialog.id, dialogOption);
        nodes.put(yde_option.id, yde_option);
        if (dialog != null) {
            YDEDialog yde_dialog = getDialog(dialog);
            if (yde_dialog != null) { links.add(new YDELink(yde_dialog.id, yde_option.id, EnumYDEType.DIALOG)); }
        }
        return yde_option;
    }

    public YDENpc getNpc(String category, Dialog.StartedNpcData npcData) {
        for (YDENode node : new ArrayList<>(nodes.values())) {
            if (node.type == EnumYDEType.NPC && node instanceof YDENpc &&
                    node.category.equals(category) && ((YDENpc) node).npcData.equals(npcData)) { return (YDENpc) node; }
        }
        return null;
    }

    public YDEQuest getQuest(String category, int questId) {
        for (YDENode node : new ArrayList<>(nodes.values())) {
            if (node.type == EnumYDEType.QUEST && node instanceof YDEQuest &&
                    node.category.equals(category) && ((YDEQuest) node).questId == questId) { return (YDEQuest) node; }
        }
        return null;
    }

    public YDEDialog getDialog(@Nonnull Dialog dialog) {
        for (YDENode node : new ArrayList<>(nodes.values())) {
            if (node.type == EnumYDEType.DIALOG && node instanceof YDEDialog &&
                    (dialog.equals(((YDEDialog) node).dialog) || (((YDEDialog) node).dialog != null &&
                            ((YDEDialog) node).dialog.id == dialog.id) || ((YDEDialog) node).dialogId == dialog.id)) { return (YDEDialog) node; }
        }
        return null;
    }

    public YDEDialog getDialog(int dialogId) {
        Dialog dialog = DialogController.instance.get(dialogId);
        for (YDENode node : new ArrayList<>(nodes.values())) {
            if (node.type == EnumYDEType.DIALOG && node instanceof YDEDialog &&
                    (dialog != null && dialog.equals(((YDEDialog) node).dialog) || ((YDEDialog) node).dialogId == dialogId)) { return (YDEDialog) node; }
        }
        return null;
    }

    public YDEDialog createDialog(@Nonnull Dialog dialog) {
        YDEDialog yde_dialog = getDialog(dialog);
        if (yde_dialog == null) {
            yde_dialog = new YDEDialog(this, getEmptyNodeId(), dialog.category.title, dialog.id);
            yde_dialog.dialog = dialog;
            nodes.put(yde_dialog.id, yde_dialog);
        }
        return yde_dialog;
    }

    public @Nonnull YDECategory getCategory(String categoryTitle) {
        YDECategory empty = null;
        for (YDENode node : new ArrayList<>(nodes.values())) {
            if (node instanceof YDECategory) {
                if (node.category.equals(categoryTitle)) { return ((YDECategory) node); }
                if (node.category.isEmpty()) { empty = (YDECategory) node; }
            }
        }
        if (empty == null) { nodes.put(-1, empty = new YDECategory(this, -1, "")); }
        empty.category = categoryTitle;
        return empty;
    }

    public List<YDENode> getToLinks(int nodeId) {
        List<YDENode> list = new ArrayList<>();
        for (YDELink link : new ArrayList<>(links)) {
            if (link.next == nodeId && nodes.containsKey(link.back)) { list.add(nodes.get(link.back)); }
        }
        return list;
    }

    public List<YDENode> getFromLinks(int nodeId) {
        List<YDENode> list = new ArrayList<>();
        for (YDELink link : new ArrayList<>(links)) {
            if (link.back == nodeId && nodes.containsKey(link.next)) { list.add(nodes.get(link.next)); }
        }
        return list;
    }

    public List<YDELink> getLinks(String categoryTitle) {
        List<YDELink> list = new ArrayList<>();
        for (YDELink link : new ArrayList<>(links)) {
            YDENode nodeB = nodes.get(link.back);
            YDENode nodeN = nodes.get(link.next);
            if ((nodeB != null && nodeB.category.equals(categoryTitle)) ||
                    (nodeN != null && nodeN.category.equals(categoryTitle))) { list.add(link); }
        }
        return list;
    }

    public void removeLink(YDELink selectLink) { links.remove(selectLink); }

}