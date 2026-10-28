package noppes.npcs.containers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomContainer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.entity.data.ICustomDrop;
import noppes.npcs.controllers.DropController;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Deal;
import noppes.npcs.controllers.data.DropsTemplate;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DropSet;

import javax.annotation.Nonnull;

public class ContainerNPCDropSetup extends AbstractContainerMenu {

    public final DropSet inventoryDS;
    public final int dataType;

    public int dropType;
    public String templateId;
    public String groupId;
    public int marcetID;
    public int dealID;
    public int questID;

    public ContainerNPCDropSetup(int containerId, Inventory playerInventory, CompoundTag compound) {
        super(CustomContainer.container_dropsetup, containerId);
        DropSet inv = null;
        dataType = compound.getInt("InventoryType");
        if (dataType == 0) {
            EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(playerInventory.player);
            if (npc == null && compound.contains("EntityId")) {
                Entity e = playerInventory.player.level().getEntity(compound.getInt("EntityId"));
                if (e instanceof EntityNPCInterface npcIn) { npc = npcIn; }
            }
            if (npc != null) {
                dropType = compound.getInt("DropType");
                groupId = compound.getString("GroupId");
                int pos = compound.getInt("Pos");
                if (dropType == 1) {
                    DropsTemplate template = DropController.getInstance().templates.get(npc.inventory.saveDropsName);
                    if (template != null && template.groups.containsKey(groupId) && template.groups.get(groupId).containsKey(pos)) {
                        inv = template.groups.get(groupId).get(pos);
                    }
                } else {
                    if (npc.inventory.drops.containsKey(pos)) { inv = npc.inventory.drops.get(pos); }
                }
                if (inv == null) { inv = new DropSet(npc.inventory); }
            }
        }
        else if (dataType == 1) {
            marcetID = compound.getInt("Marcet");
            dealID = compound.getInt("Deal");
            Deal deal = MarcetController.getInstance().deals.get(dealID);
            if (deal != null) {
                int pos = compound.getInt("DropSet");
                ICustomDrop[] drops = deal.getCaseItems();
                if (pos >= 0 && pos < drops.length) { inv = (DropSet) drops[pos]; }
                if (inv == null) {
                    inv = new DropSet(deal);
                    inv.pos = pos;
                }
            }
        } // Marcet deal
        else if (dataType == 2) {
            questID = compound.getInt("QuestID");
            Quest quest = QuestController.instance.get(questID);
            if (quest != null) {
                int pos = compound.getInt("DropSet");
                if (pos >= 0 && quest.rewardItems.containsKey(pos)) { inv = quest.rewardItems.get(pos); }
                if (inv == null) {
                    inv = new DropSet(quest);
                    inv.pos = pos;
                }
            }
        } // quest
        else if (dataType == 3) {
            groupId = compound.getString("GroupId");
            templateId = compound.getString("TemplateId");
            int pos = compound.getInt("Pos");
            DropsTemplate template = DropController.getInstance().templates.get(templateId);
            if (template != null && template.groups.containsKey(groupId) && template.groups.get(groupId).containsKey(pos)) {
                inv = template.groups.get(groupId).get(pos);
            }
            if (inv == null) { inv = new DropSet(null); }
        } // templates

        if (inv != null && playerInventory.player instanceof ServerPlayer) { inventoryDS = inv.copy(); }
        else { inventoryDS = inv; }
        if (inventoryDS != null) { addSlot(new Slot(inventoryDS, 0, 202, 135)); }
        for (int i1 = 0; i1 < 3; ++i1) {
            for (int l2 = 0; l2 < 9; ++l2) { addSlot(new Slot(playerInventory, l2 + i1 * 9 + 9, l2 * 18 + 8, 135 + i1 * 18)); }
        }
        for (int j1 = 0; j1 < 9; ++j1) { addSlot(new Slot(playerInventory, j1, j1 * 18 + 8, 193)); }
    }

    @Override
    public @Nonnull ItemStack quickMoveStack(@Nonnull Player playerIn, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(@Nonnull Player playerIn) { return true; }

}
