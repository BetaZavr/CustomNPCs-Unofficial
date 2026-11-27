package noppes.npcs.containers;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import noppes.npcs.CustomContainer;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.containers.slots.SlotNPCArmor;
import noppes.npcs.containers.slots.SlotNPCHand;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class ContainerEntityInventory extends AbstractContainerMenu {

    public final LivingEntity entity;
    public final NpcMiscInventory inv = new NpcMiscInventory(EquipmentSlot.values().length);

    public ContainerEntityInventory(int containerId, Inventory playerInventory, int entityId) {
        super(CustomContainer.container_entity_inventory, containerId);
        if (playerInventory.player.level().getEntity(entityId) instanceof LivingEntity e) { entity = e; }
        else { entity = null; }
        if (entity != null) {
            for (EquipmentSlot enumSlot : EquipmentSlot.values()) {
                int slotId = enumSlot.getFilterFlag();
                inv.setItem(slotId, entity.getItemBySlot(enumSlot));
                int x = 6;
                int y = 8;
                if (slotId > 0 && slotId < 5) {
                    y += (4 - slotId) * 18;
                    addSlot(new SlotNPCArmor(inv, slotId, x, y, enumSlot) {
                        @Override
                        public void setChanged() {
                            container.setChanged();
                            for (EquipmentSlot slot : EquipmentSlot.values()) {
                                if (slot.getFilterFlag() == slotId) {
                                    entity.setItemSlot(slot, getItem());
                                    break;
                                }
                            }
                        }
                    });
                } // armor
                else {
                    x += 73;
                    y += slotId == 0 ? 36 : 54;
                    addSlot(new SlotNPCHand(inv, slotId, x, y, slotId == 0 ? "weapon" : "shield") {
                        @Override
                        public void setChanged() {
                            container.setChanged();
                            for (EquipmentSlot slot : EquipmentSlot.values()) {
                                if (slot.getFilterFlag() == slotId) {
                                    entity.setItemSlot(slot, getItem());
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
            for(int j = 0; j < 9; ++j) { addSlot(new Slot(playerInventory, j + i * 9 + 9, 6 + j * 18, 87 + i * 18)); }
        }
        for(i = 0; i < 9; ++i) { addSlot(new Slot(playerInventory, i, 6 + i * 18, 145)); }
    }

    @Override
    public @Nonnull ItemStack quickMoveStack(@Nonnull Player playerIn, int index) {
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack inSlot = slot.getItem();
            if (index >= 6) {
                if (inSlot.getItem() instanceof ArmorItem armorItem) {
                    Slot armSlot = slots.get(armorItem.getEquipmentSlot().getFilterFlag() + 1);
                    ItemStack armorStack = armSlot.getItem().copy();
                    armSlot.set(inSlot);
                    slot.set(armorStack);
                }
                else if (inSlot.getItem() instanceof ShieldItem) {
                    Slot sldSlot = slots.get(1);
                    ItemStack offStack = sldSlot.getItem().copy();
                    sldSlot.set(inSlot);
                    slot.set(offStack);
                }
                else {
                    Slot wpnSlot = slots.get(0);
                    ItemStack wpnStack = wpnSlot.getItem().copy();
                    wpnSlot.set(inSlot);
                    slot.set(wpnStack);
                }
            } // player inventory
            else if (playerIn.getInventory().add(inSlot)) {
                slot.set(ItemStack.EMPTY);
                playerIn.inventoryMenu.broadcastChanges();
                if (playerIn instanceof ServerPlayer player) {
                    player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0,
                            player.getInventory().selected,
                            player.getInventory().getItem(player.getInventory().selected)));
                }
            } // equipment
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player playerIn) { return true; }

}
