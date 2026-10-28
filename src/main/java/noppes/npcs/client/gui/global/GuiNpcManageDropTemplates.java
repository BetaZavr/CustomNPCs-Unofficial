package noppes.npcs.client.gui.global;

import java.awt.*;
import java.util.*;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.drop.SubGuiDropEdit;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.DropController;
import noppes.npcs.controllers.data.DropsTemplate;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;

import javax.annotation.Nonnull;

public class GuiNpcManageDropTemplates extends GuiNPCInterface2
        implements IGuiData, ICustomScrollListener {

    protected final Map<Component, DropSet> dropsData = new LinkedHashMap<>();
    protected final Map<Component, String> groupsData = new LinkedHashMap<>();
    protected GuiCustomScrollNop scrollTemplates;
    protected GuiCustomScrollNop scrollGroups;
    protected GuiCustomScrollNop scrollItems;
    protected DropsTemplate temp;

    public GuiNpcManageDropTemplates(EntityNPCInterface npc) {
        super(npc);
        setBackground("menubg.png");
        widthTexture = 0;
        heightTexture = 0;
        backGui = EnumGuiType.MainMenuGlobal;

        temp = new DropsTemplate();
    }

    @Override
    public void init() {
        super.init();
        SubGuiDropEdit.parent = this;
        SubGuiDropEdit.parentContainer = null;
        SubGuiDropEdit.parentData = null;
        int w = (imageWidth - 12) / 2;
        int r0 = guiLeft + 5;
        int r1 = r0 + w + 2;
        int y = guiTop + 5;
        if (scrollTemplates == null) { scrollTemplates = addScroll(0).setSize(w - 1, 179); }
        if (scrollGroups == null) { scrollGroups = addScroll(1).setSize(w, 75); }
        if (scrollItems == null) { scrollItems = addScroll(2).setSize(w, 75); }

        // Templates section
        addLabel(0, r0 + 1, y, "gui.templates")
                .setSize(165, 12);
        List<Component> templateList = new ArrayList<>();
        for (String name : DropController.getInstance().templates.keySet()) {
            templateList.add(Component.literal(name));
        }
        add(scrollTemplates.setPos(r0, y + 10)
                .setNormalList(templateList));
        if (!scrollTemplates.hasSelected() && !templateList.isEmpty()) { scrollTemplates.setSelected(0); }
        // Load selected template
        if (scrollTemplates.hasSelected()) {
            String name = scrollTemplates.getSelected();
            if (DropController.getInstance().templates.containsKey(name)) {
                temp = DropController.getInstance().templates.get(name);
            } else {
                temp = new DropsTemplate();
            }
        }
        // Template buttons
        y += scrollTemplates.getHeight() + 12;
        int wb = (w - 4 * 2) / 4;
        addButton(1, r0, y, "gui.add")
                .setSize(wb, 16)
                .setHoverTexts("inv.hover.new.template");
        addButton(2, r0 + wb + 2, y, "gui.copy")
                .setSize(wb, 16)
                .setIsEnabled(scrollTemplates.hasSelected() && temp != null)
                .setHoverTexts("inv.hover.copy.template");
        addButton(3, r0 + (wb + 2) * 2, y, "selectServer.edit")
                .setSize(wb, 16)
                .setIsEnabled(scrollTemplates.hasSelected())
                .setHoverTexts("inv.hover.rename.template");
        addButton(4, r0 + (wb + 2) * 3 + 1, y, "gui.remove")
                .setSize(wb, 16)
                .setIsEnabled(scrollTemplates.hasSelected())
                .setHoverTexts("inv.hover.del.template");

        // Right side - template content
        y = guiTop + 5;
        addLabel(1, r1 + 1, y, Component.translatable("gui.groups").append(" [?]:"))
                .setSize(165, 12)
                .setHoverTexts("inv.hover.group.id");
        // Group buttons
        y += scrollGroups.getHeight() + 12;
        wb = (w - 2 * 3) / 3;
        addButton(10, r1, y, "gui.add")
                .setSize(wb, 16);
        addButton(11, r1 + wb + 3, y, "gui.copy")
                .setSize(wb, 16);
        addButton(12, r1 + (wb + 3) * 2, y, "gui.remove")
                .setSize(wb, 16);
        updateGroups();

        // Items section
        y += 18;
        addLabel(2, r1 + 1, y - 1, "inv.drops")
                .setSize(165, 12);
        // Item buttons
        y += scrollItems.getHeight() + 11;
        addButton(20, r1, y, "gui.add")
                .setSize(wb, 16);
        addButton(21, r1 + wb + 3, y, "selectServer.edit")
                .setSize(wb, 16)
                .setIsEnabled(scrollItems.hasSelected());
        addButton(22, r1 + (wb + 3) * 2, y, "gui.remove")
                .setSize(wb, 16)
                .setIsEnabled(scrollItems.hasSelected())
                .setHoverTexts("inv.hover.del.drop");
        updateItems();
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        String groupId = groupsData.get(scrollGroups.getNormalSelected());
        switch (button.id) {
            case 1: {
                setSubGui(new SubGuiEditText(1, Component.translatable("gui.new").getString()));
                break;
            } // create template
            case 2: {
                if (scrollTemplates == null || !scrollTemplates.hasSelected() || temp == null) { return; }
                String name = scrollTemplates.getSelected() + " (copy)";
                while (temp.groups.containsKey(name)) { name += "_"; }
                DropController dData = DropController.getInstance();
                while (dData.templates.containsKey(name)) { name += "_"; }
                dData.templates.put(name, DropsTemplate.from(temp));
                temp = dData.templates.get(name);
                Packets.sendServer(new SPacketDropTemplateSave(name, temp.getNBT()));
                init();
                scrollTemplates.setSelected(name);
                break;
            } // copy template
            case 3: {
                if (scrollTemplates == null || !scrollTemplates.hasSelected()) { return; }
                setSubGui(new SubGuiEditText(3, scrollTemplates.getSelected()));
                break;
            } // rename template
            case 4: {
                if (scrollTemplates == null || !scrollTemplates.hasSelected()) { return; }
                ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
                    if (agree) {
                        if (scrollTemplates == null || !scrollTemplates.hasSelected()) { return; }
                        String name = scrollTemplates.getSelected();
                        DropController.getInstance().templates.remove(name);
                        Packets.sendServer(new SPacketDropTemplateRemove(name));
                    }
                    NoppesUtil.openGUI(player, this);
                },
                        scrollTemplates.getNormalSelected(),
                        Component.translatable("message.delete"));
                setScreen(guiYesNo);
                break;
            } // delete template
            case 10: {
                if (temp != null) {
                    setSubGui(new SubGuiEditText(2, Component.translatable("gui.group")
                            .append(" #" + temp.groups.size()).getString()));
                }
                break;
            } // add group
            case 11: {
                if (temp != null && scrollTemplates != null && scrollTemplates.hasSelected() &&
                        groupId != null && temp.groups.containsKey(groupId)) {
                    TreeMap<Integer, DropSet> newGroup = new TreeMap<>();
                    for (Map.Entry<Integer, DropSet> entry : temp.groups.get(groupId).entrySet()) { newGroup.put(entry.getKey(), entry.getValue().copy()); }
                    String name = groupId + " (copy)";
                    while (temp.groups.containsKey(name)) { name += "_"; }
                    temp.groups.put(name, newGroup);
                    Packets.sendServer(new SPacketDropTemplateSave(scrollTemplates.getSelected(), temp.getNBT()));
                }
                break;
            } // copy group
            case 12: {
                if (temp != null && temp.groups.size() > 1 && scrollTemplates != null && scrollTemplates.hasSelected() &&
                        groupId != null && temp.groups.containsKey(groupId)) {
                    int pos = scrollGroups.getSelectedIndex();
                    if (pos == 0) { scrollGroups.setSelected(1); }
                    else { scrollGroups.setSelected(pos - 1); }
                    if (temp.groups.get(groupId).isEmpty()) {
                        temp.removeGroup(groupId);
                        Packets.sendServer(new SPacketDropTemplateSave(scrollTemplates.getSelected(), temp.getNBT()));
                    } // is empty
                    else {
                        ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
                            if (agree) {
                                if (temp != null && temp.groups.size() > 1 && scrollTemplates != null &&
                                        scrollTemplates.hasSelected() && temp.groups.containsKey(groupId)) {
                                    temp.removeGroup(groupId);
                                    Packets.sendServer(new SPacketDropTemplateSave(scrollTemplates.getSelected(), temp.getNBT()));
                                }
                            }
                            NoppesUtil.openGUI(player, this);
                        },
                                Component.translatable("gui.group").append(" ID: " + groupId),
                                Component.translatable("message.delete"));
                        setScreen(guiYesNo);
                    } // not empty
                    init();
                }
                break;
            } // delete group
            case 20: {
                if (temp == null || groupId == null || !temp.groups.containsKey(groupId)) { return; }
                CompoundTag compound = new CompoundTag();
                compound.putInt("InventoryType", 3);
                compound.putString("TemplateId", scrollTemplates.getSelected());
                compound.putString("GroupId", groupId);
                compound.putInt("Pos", -1);
                Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
                break;
            } // add drop
            case 21: {
                if (scrollItems == null || !scrollItems.hasSelected()) { return; }
                DropSet selectedDrop = dropsData.get(scrollItems.getNormalSelected());
                if (selectedDrop == null) { return; }
                CompoundTag compound = new CompoundTag();
                compound.putInt("InventoryType", 3);
                compound.putString("TemplateId", scrollTemplates.getSelected());
                compound.putString("GroupId", groupId);
                compound.putInt("Pos", selectedDrop.pos);
                Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
                break;
            } // edit drop
            case 22: {
                if (scrollItems == null || !scrollItems.hasSelected() || scrollTemplates == null || !scrollTemplates.hasSelected()) { return; }
                DropSet selectedDrop = dropsData.get(scrollItems.getNormalSelected());
                if (selectedDrop != null && groupId != null) {
                    if (temp != null && temp.groups.containsKey(groupId)) { temp.groups.get(groupId).remove(selectedDrop.pos); }
                    selectedDrop.item = ItemStack.EMPTY;
                    Packets.sendServer(new SPacketTemplateDropSetSave(scrollTemplates.getSelected(), groupId, selectedDrop.save()));
                    updateItems();
                }
                break;
            } // delete drop
        }
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics) {
        super.renderBackground(graphics);
        if (scrollGroups != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            int color = new Color(0xFF606060).getRGB();
            graphics.vLine(scrollGroups.getX() - 2, guiTop + 4, guiTop + imageHeight + 12, color);
        }
    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {
        if (!scroll.hasSelected()) { return; }
        if (scroll.id == 0) {
            String name = scroll.getSelected();
            if (DropController.getInstance().templates.containsKey(name)) {
                temp = DropController.getInstance().templates.get(name);
                scrollGroups.clearSelection();
            }
        } // template selected
        updateGroups();
        updateItems();
    }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
        if (scroll.id == 0) { setSubGui(new SubGuiEditText(3, scroll.getSelected())); } // rename template
        else if (scroll.id == 1) { setSubGui(new SubGuiEditText(4, scroll.getSelected())); } // rename group
        else if (scroll.id == 2) {
            if (scrollItems == null || !scrollItems.hasSelected() ||
                    scrollGroups == null || !groupsData.containsKey(scrollGroups.getNormalSelected())) { return; }
            DropSet selectedDrop = dropsData.get(scrollItems.getNormalSelected());
            if (selectedDrop == null) { return; }
            CompoundTag compound = new CompoundTag();
            compound.putInt("InventoryType", 3);
            compound.putString("TemplateId", scrollTemplates.getSelected());
            compound.putString("GroupId", groupsData.get(scrollGroups.getNormalSelected()));
            compound.putInt("Pos", selectedDrop.pos);
            Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
        }
    }

    @Override
    public void save() {
        if (temp != null && scrollTemplates != null && scrollTemplates.hasSelected()) {
            Packets.sendServer(new SPacketDropTemplateSave(scrollTemplates.getSelected(), temp.getNBT()));
        }
    }

    @Override
    public void setGuiData(CompoundTag compound) {
        if (compound.contains("DropsTemplate", 10)) {
            // Reload template from server
            String name = compound.getString("TemplateName");
            if (DropController.getInstance().templates.containsKey(name)) {
                temp = DropController.getInstance().templates.get(name);
                updateGroups();
                updateItems();
            }
        }
    }

    @Override
    public void subGuiClosed(Screen subgui) {
        if (subgui instanceof SubGuiEditText && !((SubGuiEditText) subgui).cancelled) {
            DropController dData = DropController.getInstance();
            String name = ((SubGuiEditText) subgui).text[0];
            if (((SubGuiEditText) subgui).id == 1) {
                while (dData.templates.containsKey(name)) { name += "_"; }
                DropsTemplate dt = new DropsTemplate();
                String groupId = Component.translatable("gui.group").append(" #0").getString();
                dt.groups.put(groupId, new TreeMap<>());
                dt.groups.remove("default");

                dData.templates.put(name, dt);
                init();
                scrollTemplates.setSelected(name);
                Packets.sendServer(new SPacketDropTemplateSave(name, dt.getNBT()));
            } // create template
            else if (((SubGuiEditText) subgui).id == 2) {
                if (name != null && temp != null) {
                    while (groupsData.containsValue(name)) { name += "_"; }
                    temp.groups.put(name, new TreeMap<>());
                    Packets.sendServer(new SPacketDropTemplateSave(name, temp.getNBT()));
                }
            } // create group
            else if (((SubGuiEditText) subgui).id == 3) {
                if (name == null || name.equals(scrollTemplates.getSelected()) ||
                        dData.templates.containsKey(name) || !dData.templates.containsKey(scrollTemplates.getSelected())) { return; }
                Packets.sendServer(new SPacketDropTemplateRename(scrollTemplates.getSelected(), name));
            } // rename template
            else if (((SubGuiEditText) subgui).id == 4) {
                String oldName = groupsData.get(scrollGroups.getNormalSelected());
                if (name == null || name.equals(scrollGroups.getSelected()) || temp == null ||
                        temp.groups.containsKey(name) || !groupsData.containsValue(oldName) || !temp.groups.containsKey(oldName)) { return; }
                while (temp.groups.containsKey(name) && !name.equals(oldName)) { name += "_"; }
                if (name.equals(oldName) || !temp.groups.containsKey(oldName)) { return; }
                temp.groups.put(name, temp.groups.get(oldName));
                temp.groups.remove(oldName);
            } // rename group
            init();
        }
        else if (subgui instanceof SubGuiDropEdit) {
            // Drop was added/edited - reload template
            if (temp != null && scrollTemplates != null && scrollTemplates.hasSelected()) {
                String name = scrollTemplates.getSelected();
                if (DropController.getInstance().templates.containsKey(name)) {
                    temp = DropController.getInstance().templates.get(name);
                    updateGroups();
                    updateItems();
                }
            }
        }
    }

    protected void updateGroups() {
        int selectedGroupIndex = scrollGroups != null ? scrollGroups.getSelectedIndex() : -1;
        LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
        List<ItemStack> stacks = new ArrayList<>();
        List<Component> groupList = new ArrayList<>();
        groupsData.clear();
        boolean hasEmptyGroups = false;
        if (temp != null) {
            Component key;
            for (String id : temp.groups.keySet()) {
                Map<Integer, DropSet> group = temp.groups.get(id);
                ItemStack stack = ItemStack.EMPTY;
                if (group.isEmpty()) { hasEmptyGroups = true; }
                for (DropSet ds : group.values()) {
                    if (ds.item != null && !ds.item.isEmpty()) {
                        stack = ds.item;
                        break;
                    }
                }
                stacks.add(stack);
                List<Component> hover = new ArrayList<>();
                hover.add(Component.translatable("gui.group").append(": \"").append(id).append("\""));
                hover.add(Component.translatable("gui.items").append(": " + group.size()));
                hts.put(groupList.size(), hover);
                key = Component.translatable(id);
                groupsData.put(key, id);
                groupList.add(key);
            }
        }
        int w = (imageWidth - 12) / 2;
        int x = guiLeft + 5 + w + 2;
        add(scrollGroups.setPos(x, guiTop + 15)
                .setUnsortedList(groupList)
                .setHoverTexts(hts)
                .setStacks(stacks));
        if (selectedGroupIndex >= 0 && selectedGroupIndex < groupList.size()) {
            scrollGroups.setSelected(selectedGroupIndex);
        }
        else if (!scrollGroups.hasSelected() && !groupsData.isEmpty()) { scrollGroups.setSelected(0); }
        boolean hasGroup = temp != null &&
                groupsData.containsKey(scrollGroups.getNormalSelected()) &&
                !temp.groups.isEmpty() && temp.groups.containsKey(groupsData.get(scrollGroups.getNormalSelected()));
        GuiButtonNop button = getButton(10);
        if (button != null) {
            int maxGroups = 36;
            MutableComponent hover = Component.translatable("inv.hover.add.group", maxGroups);
            if (hasEmptyGroups) {
                hover.append(Component.translatable("inv.hover.has.empty.group"));
            }
            button.setIsEnabled(!hasEmptyGroups && temp.groups.size() < maxGroups)
                    .setHoverTexts(hover);
        } // add group
        button = getButton(11);
        if (button != null) {
            MutableComponent hover = Component.translatable("inv.hover.copy.group");
            if (hasEmptyGroups) {
                hover.append(Component.translatable("inv.hover.has.empty.group"));
            }
            button.setIsEnabled(hasGroup && temp != null && scrollGroups.hasSelected() && !hasEmptyGroups)
                    .setHoverTexts(hover);
        } // copy
        button = getButton(12);
        if (button != null) {
            MutableComponent hover = Component.translatable("inv.hover.del.group");
            if (groupsData.size() <= 1) {
                hover.append(Component.translatable("inv.hover.is.one.group"));
            }
            button.setIsEnabled(hasGroup && groupsData.size() > 1)
                    .setHoverTexts(hover);
        }
    }

    protected void updateItems() {
        int selectedItemIndex = scrollItems != null ? scrollItems.getSelectedIndex() : -1;
        LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
        List<ItemStack> stacks = new ArrayList<>();
        dropsData.clear();
        String groupId = groupsData.get(scrollGroups.getNormalSelected());
        boolean hasGroup = temp != null && groupId != null && !temp.groups.isEmpty() && temp.groups.containsKey(groupId);
        if (hasGroup) {
            int i = 0;
            for (DropSet ds : temp.groups.get(groupId).values()) {
                dropsData.put(ds.getKey(), ds);
                hts.put(i++, ds.getHover(true));
                stacks.add(ds.item);
            }
        }
        int w = (imageWidth - 12) / 2;
        int x = guiLeft + 5 + w + 2;
        add(scrollItems.setPos(x,
                        scrollGroups.getY() + scrollGroups.getHeight() + 29)
                .setUnsortedList(new ArrayList<>(dropsData.keySet()))
                .setHoverTexts(hts)
                .setStacks(stacks));
        if (selectedItemIndex >= 0 && selectedItemIndex < dropsData.size()) {
            scrollItems.setSelected(selectedItemIndex);
        }
        else if (!scrollItems.hasSelected() && !dropsData.isEmpty()) { scrollItems.setSelected(0); }

        GuiButtonNop button = getButton(20);
        if (button != null) {
            int maxItems = 100;
            button.setIsEnabled(hasGroup && temp.groups.get(groupId).size() < maxItems)
                    .setHoverTexts(Component.translatable( "inv.hover.add.drop", maxItems));
        } // add
        button = getButton(21);
        if (button != null) {
            MutableComponent hover = Component.translatable("inv.hover.edit.drop");
            if (dropsData.containsKey(scrollItems.getNormalSelected())) { hover.append(" \"").append(dropsData.get(scrollItems.getNormalSelected()).getKey()).append("\""); }
            button.setIsEnabled(scrollItems.hasSelected()).setHoverTexts(hover);
        } // edit
        button = getButton(22);
        if (button != null) { button.setIsEnabled(scrollItems.hasSelected()); } // del
    }

}
