package noppes.npcs.mixin.client.gui.worldselection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.storage.LevelSummary;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.shared.common.util.LogWriter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = WorldSelectionList.WorldListEntry.class, priority = 498)
public class SelectWorldScreenMixin {

    @Final @Shadow private Minecraft minecraft;
    @Final @Shadow private SelectWorldScreen screen;
    @Final @Shadow private LevelSummary summary;

    @Inject(
            at = {@At("HEAD")},
            method = "loadWorld",
            cancellable = true
    )
    private void npcsLoadWorld(CallbackInfo ci) {
        try {
            ScriptController.setLevelKey(summary.getLevelName()+"_"+summary.getLevelId()+"_"+summary.getWorldVersionName().getString()+"_"+summary.getSettings().gameType());
            if (ScriptController.Instance.notAgreement(ScriptController.getLevelKey())) {
                ci.cancel();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                Screen backScreen = minecraft.screen;
                minecraft.setScreen(new ConfirmScreen((agree) -> {
                    if (agree && minecraft.getLevelSource().levelExists(summary.getLevelId())) {
                        ScriptController.Instance.setAgreement(ScriptController.getLevelKey(), true);
                        minecraft.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));
                        minecraft.createWorldOpenFlows().loadLevel(screen, summary.getLevelId());
                    }
                    else {
                        minecraft.setScreen(backScreen);
                        ScriptController.setLevelKey("");
                    }
                },
                        Component.empty(),
                        Component.translatable("system.check.scripts.agree"),
                        Component.translatable("gui.agree"),
                        Component.translatable("gui.cancel")));
            }
        }
        catch (Exception e) { LogWriter.error("Error while checking user agreement: "); }
    }

    /*
     * Check all worlds.
     * If it was deleted, then the agreement is cancelled
     */
    @Inject(method = "doDeleteWorld", at = @At("TAIL"))
    private void npcs$doDeleteWorld(CallbackInfo ci) {
        try {
            List<String> checkList = new ArrayList<>();
            var source = minecraft.getLevelSource();
            var candidates = source.findLevelCandidates();
            List<LevelSummary> summaries = source.loadLevelSummaries(candidates).join();
            for (LevelSummary lvlSummary : summaries) {
                if (lvlSummary != null) {
                    checkList.add(
                            lvlSummary.getLevelName() + "_" +
                                    lvlSummary.getLevelId() + "_" +
                                    lvlSummary.getWorldVersionName().getString() + "_" +
                                    lvlSummary.getSettings().gameType()
                    );
                }
            }
            ScriptController.Instance.checkAgreements(checkList);
        } catch (Exception e) {
            LogWriter.error("Error while getting check list: " + e.getMessage());
        }
    }

}
