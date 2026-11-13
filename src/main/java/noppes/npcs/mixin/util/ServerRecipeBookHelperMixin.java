package noppes.npcs.mixin.util;

import net.minecraft.inventory.*;
import net.minecraft.network.play.server.SPacketPlaceGhostRecipe;
import net.minecraftforge.common.crafting.IRecipeContainer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ServerRecipeBookHelper;
import noppes.npcs.controllers.data.RecipeCarpentry;
import noppes.npcs.recipebook.CustomServerPlaceRecipe;
import noppes.npcs.recipebook.CustomStackedContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(value = ServerRecipeBookHelper.class, remap = false, priority = 498)
public abstract class ServerRecipeBookHelperMixin {

    @Shadow(aliases = "field_194332_c") private EntityPlayerMP player;
    @Shadow(aliases = "field_194333_d") private IRecipe recipe;
    @Shadow(aliases = "field_194334_e") private boolean isShiftPressed;
    @Shadow(aliases = "field_194335_f") private InventoryCraftResult inventoryResult;
    @Shadow(aliases = "field_194336_g") private InventoryCrafting menu;
    @Shadow(aliases = "field_194337_h") private List<Slot> slots;
    @Shadow(aliases = "func_194326_a") private void clearGrid() { }
    @Shadow(aliases = "func_194328_c") private boolean testClearGrid() { return false; }

    @Unique protected CustomStackedContents npcs$customStackedContents = new CustomStackedContents();

    /**
     * NetHandlerPlayServer.func_194308_a(CPacketPlaceRecipe cPacket) {} -> here:
     * func_194327_a(EntityPlayerMP playerMP, IRecipe checkRecipe, boolean shiftPressed) {}
     * checks if the player open container has a crafting grid;
     * if the player has items for crafting, then places them; -> func_194329_b()
     * if there are not enough items, then sends back a ghost recipe; -> player.connection.sendPacket(new SPacketPlaceGhostRecipe())
     *
     * @author BetaZavr
     * @reason Own conditions for custom recipes
     */
    @Inject(method = "func_194327_a", at = @At("HEAD"), cancellable = true)
    public void npcs$processCraftRecipe(EntityPlayerMP playerMP, @Nullable IRecipe iRecipe, boolean isShiftPress, CallbackInfo ci) {
        if (iRecipe instanceof RecipeCarpentry && playerMP.getRecipeBook().isUnlocked(iRecipe)) {
            ci.cancel();
            RecipeCarpentry npcRecipe = (RecipeCarpentry) iRecipe;
            player = playerMP;
            recipe = npcRecipe;
            isShiftPressed = isShiftPress;
            slots = playerMP.openContainer.inventorySlots;
            Container container = playerMP.openContainer;
            inventoryResult = null;
            menu = null;
            if (container instanceof ContainerWorkbench) {
                inventoryResult = ((ContainerWorkbench)container).craftResult;
                menu = ((ContainerWorkbench)container).craftMatrix;
            }
            else if (container instanceof ContainerPlayer) {
                inventoryResult = ((ContainerPlayer)container).craftResult;
                menu = ((ContainerPlayer)container).craftMatrix;
            }
            else if (container instanceof IRecipeContainer) {
                inventoryResult = ((IRecipeContainer)container).getCraftResult();
                menu = ((IRecipeContainer)container).getCraftMatrix();
            }

            if (inventoryResult != null && menu != null && (testClearGrid() || player.isCreative())) {
                npcs$customStackedContents.clear();
                npcs$customStackedContents.setRecipe(npcRecipe);
                playerMP.inventory.fillStackedContents(npcs$customStackedContents, false);
                menu.fillStackedContents(npcs$customStackedContents);
                if (npcRecipe.availability.isAvailable(player) &&
                        npcs$customStackedContents.canNpcCraft(npcRecipe)) {
                    new CustomServerPlaceRecipe(recipe, isShiftPress, playerMP.inventory, menu, inventoryResult,
                            playerMP.openContainer.inventorySlots, npcs$customStackedContents);
                }
                else {
                    clearGrid();
                    playerMP.connection.sendPacket(new SPacketPlaceGhostRecipe(playerMP.openContainer.windowId, recipe));
                }
                playerMP.inventory.markDirty();
            }
            npcs$customStackedContents.clear();
        }
    }

}
