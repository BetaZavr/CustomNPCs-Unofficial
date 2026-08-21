package noppes.npcs.mixin.mezz.jei.gui.events;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import noppes.npcs.client.gui.custom.GuiCustom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"mezz.jei.gui.events.GuiEventHandler"}, priority = 498, remap = false)
public class MixinJEIEventHandler {

    @Inject(
            method = {"onDrawBackgroundPost"},
            at = {@At("HEAD")},
            remap = false,
            cancellable = true
    )
    public void onScreenEvent(Screen screen, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (screen instanceof GuiCustom) { ci.cancel(); }
    }

    @Inject(
            method = {"onDrawForeground"},
            at = {@At("HEAD")},
            remap = false,
            cancellable = true
    )
    public void onScreenEvent(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (screen instanceof GuiCustom) { ci.cancel(); }
    }

    @Inject(
            method = {"onDrawScreenPost"},
            at = {@At("HEAD")},
            remap = false,
            cancellable = true
    )
    public void onScreenEvent(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (screen instanceof GuiCustom) { ci.cancel(); }
    }

}
