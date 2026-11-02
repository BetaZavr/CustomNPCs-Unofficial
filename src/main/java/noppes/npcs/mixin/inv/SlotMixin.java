package noppes.npcs.mixin.inv;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.data.PlayerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Slot.class, priority = 498)
public abstract class SlotMixin {

    @Final @Shadow private int slotIndex;
    @Shadow @Final public IInventory inventory;
    @Shadow public abstract void putStack(ItemStack stack);

    @Inject(method = {"onTake"}, at = {@At("HEAD")})
    public void onTake(EntityPlayer playerIn, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (playerIn instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) playerIn;
            PlayerEvent.SlotChangedItemStackEvent event = new PlayerEvent.SlotChangedItemStackEvent(PlayerData.get(player).getIPlayer(),
                    slotIndex, stack, player.inventory.getItemStack(), inventory);
            EventHooks.onEvent(EventHooks.getPlayerHandler(), EnumScriptType.GUI_SLOT_CHANGED, event);
            if (!ItemStack.areItemStacksEqual(stack, event.slotStack.getMCItemStack())) { putStack(event.slotStack.getMCItemStack()); }
        }
    }

}
