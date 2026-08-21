package noppes.npcs.mixin.world.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.*;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerScriptData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Slot.class, priority = 498)
public abstract class SlotMixin {

    @Shadow public int index;
    @Shadow @Final public Container container;
    @Shadow public abstract ItemStack getItem();
    @Shadow public abstract void set(ItemStack stack);

    @Inject(method = {"onTake"}, at = {@At("HEAD")})
    public void onTake(Player playerIn, ItemStack stack, CallbackInfo ci) {
        if (playerIn instanceof ServerPlayer player) {
            PlayerScriptData handler = PlayerData.get(player).scriptData;
            PlayerEvent.SlotChangedItemStackEvent event = new PlayerEvent.SlotChangedItemStackEvent(handler.getPlayer(),
                    index, stack, player.inventoryMenu.getCarried(), container);
            EventHooks.onEvent(handler, EnumScriptType.GUI_SLOT_CHANGED, event);
            if (!ItemStack.isSameItem(stack, event.slotStack.getMCItemStack())) { set(event.slotStack.getMCItemStack()); }
        }
    }

}
