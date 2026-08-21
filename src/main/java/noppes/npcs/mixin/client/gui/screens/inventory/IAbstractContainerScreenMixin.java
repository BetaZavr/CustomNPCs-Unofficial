package noppes.npcs.mixin.client.gui.screens.inventory;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractContainerScreen.class, priority = 502)
public interface IAbstractContainerScreenMixin {

    @Accessor Slot getClickedSlot();

    @Accessor ItemStack getDraggingItem();

    @Accessor boolean getIsSplittingStack();

    @Accessor int getQuickCraftingType();

    @Accessor int getLeftPos();

    @Accessor int getTopPos();

    @Accessor int getQuickCraftingRemainder();

    @Accessor void setQuickCraftingRemainder(int newValue);

}
