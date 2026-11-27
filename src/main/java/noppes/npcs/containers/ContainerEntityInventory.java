package noppes.npcs.containers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.containers.slots.SlotNPCArmor;
import noppes.npcs.containers.slots.SlotNPCHand;

import javax.annotation.Nonnull;

public class ContainerEntityInventory extends Container {

    public final EntityLivingBase entity;
    public final NpcMiscInventory inv = new NpcMiscInventory(EntityEquipmentSlot.values().length);

    public ContainerEntityInventory(EntityPlayer player, int entityId) {
        Entity e = player.world.getEntityByID(entityId);
        if (e instanceof EntityLivingBase) { entity = (EntityLivingBase) e; }
        else { entity = null; }
        if (entity != null) {
            for (EntityEquipmentSlot enumSlot : EntityEquipmentSlot.values()) {
                int slotId = enumSlot.getSlotIndex();
                inv.setInventorySlotContents(slotId, entity.getItemStackFromSlot(enumSlot));
                int x = 6;
                int y = 8;
                if (slotId > 0 && slotId < 5) {
                    y += (4 - slotId) * 18;
                    addSlotToContainer(new SlotNPCArmor(inv, slotId, x, y, enumSlot) {
                        @Override
                        public void onSlotChanged() {
                            inventory.markDirty();
                            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                                if (slot.getSlotIndex() == slotId) {
                                    entity.setItemStackToSlot(slot, getStack());
                                    break;
                                }
                            }
                        }
                    });
                } // armor
                else {
                    x += 73;
                    y += slotId == 0 ? 36 : 54;
                    addSlotToContainer(new SlotNPCHand(inv, slotId, x, y, slotId == 0 ? "weapon" : "shield") {
                        @Override
                        public void onSlotChanged() {
                            inventory.markDirty();
                            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                                if (slot.getSlotIndex() == slotId) {
                                    entity.setItemStackToSlot(slot, getStack());
                                    break;
                                }
                            }
                        }
                    });
                } // hands
            }
        }
        int i;
        for(i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) { addSlotToContainer(new Slot(player.inventory, j + i * 9 + 9, 6 + j * 18, 87 + i * 18)); }
        }
        for(i = 0; i < 9; ++i) { addSlotToContainer(new Slot(player.inventory, i, 6 + i * 18, 145)); }
    }

    @Override
    public @Nonnull ItemStack slotClick(int slotId, int dragType, @Nonnull ClickType clickTypeIn, @Nonnull EntityPlayer player) {
        if (clickTypeIn == ClickType.QUICK_MOVE && dragType == 0) { // shift + LMB
            if (slotId < 0) { return ItemStack.EMPTY; }
            Slot slot = inventorySlots.get(slotId);
            if (slot == null || !slot.canTakeStack(player) || !slot.getHasStack()) { return ItemStack.EMPTY; }
            if (slot.getHasStack()) {
                ItemStack inSlot = slot.getStack();
                if (slotId < 6) { // take off your equipment
                    if (player.inventory.addItemStackToInventory(inSlot)) { slot.putStack(ItemStack.EMPTY); }
                } // equipment
                else {
                    if (inSlot.getItem() instanceof ItemArmor) {
                        Slot armSlot = inventorySlots.get(((ItemArmor) inSlot.getItem()).getEquipmentSlot().getSlotIndex() + 1);
                        ItemStack armorStack = armSlot.getStack().copy();
                        armSlot.putStack(inSlot);
                        slot.putStack(armorStack);
                    }
                    else if (inSlot.getItem() instanceof ItemShield) {
                        Slot sldSlot = inventorySlots.get(1);
                        ItemStack offStack = sldSlot.getStack().copy();
                        sldSlot.putStack(inSlot);
                        slot.putStack(offStack);
                    }
                    else {
                        Slot wpnSlot = inventorySlots.get(0);
                        ItemStack wpnStack = wpnSlot.getStack().copy();
                        wpnSlot.putStack(inSlot);
                        slot.putStack(wpnStack);
                    }
                } // player inventory
            }
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    @Override
    public @Nonnull ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer entityplayer) { return true; }

}
