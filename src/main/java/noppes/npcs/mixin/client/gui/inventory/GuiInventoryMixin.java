package noppes.npcs.mixin.client.gui.inventory;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.RenderHelper;
import noppes.npcs.client.ClientEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiInventory.class, priority = 498)
public class GuiInventoryMixin {

    @Inject(method = "drawScreen", at = @At("TAIL"))
    public void npcs$drawScreenPost(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GuiInventory parent = (GuiInventory) (Object) this;
        ClientEventHandler.renderBalance(parent, mouseX, mouseY,
                parent.getGuiLeft() + 124,
                parent.getGuiTop() + 62);
        RenderHelper.enableGUIStandardItemLighting();
    }

}
