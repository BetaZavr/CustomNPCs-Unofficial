package noppes.npcs.controllers.data;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.shared.common.util.LogWriter;

public class PotionScriptData extends BaseScriptData {

    @Override
    public MutableComponent noticeString(String type, Object event) {
        return Component.literal("Potion Scripts ").withStyle(ChatFormatting.DARK_GRAY)
                .append(super.noticeString(type, event));
    }

    @Override
    public void runScript(String type, Event event) {
        if (isEnabled()) {
            try {
                if (ScriptController.Instance.lastLoaded > lastInited) {
                    lastInited = ScriptController.Instance.lastLoaded;
                    if (!type.equalsIgnoreCase(EnumScriptType.INIT.function)) { EventHooks.onPotionInit(this); }
                }
                for (ScriptContainer script : scripts) { script.run(type, event); }
            } catch (Exception e) { LogWriter.error("Error run script: ", e); }
        }
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        if (scripts.isEmpty() || scripts.get(0).script.isEmpty()) {
            ScriptContainer script = new ScriptContainer(this);
            script.script = """
                    // IPotion.getCustomName() - String (custom potion name)
                    // IPotion.getNbt() - INbt (nbt data)
                    function isReady(event) {
                      /* event.potion - IPotion
                         event.duration - int (ticks)
                         event.amplifier - int (potion power) */
                    }
                    function performEffect(event) {
                      /* event.potion - IPotion
                         event.entity - IEntity
                         event.amplifier - int (potion power) */
                    }
                    function affectEntity(event) {
                      /* event.potion - IPotion
                         event.entity - IEntity
                         event.source - IEntity
                         event.indirectSource - IEntity
                         event.amplifier - int (potion power)
                         event.health - double (health value) */
                    }
                    function endEffect(event) {
                      /* event.potion - IPotion
                         event.entity - IEntity
                         event.amplifier - int (potion power) */
                    }""";
            if (scripts.isEmpty()) { scripts.add(script); }
            else {
                scripts.remove(0);
                scripts.add(0, script);
            }
        }
        EventHooks.onPotionInit(this);
    }

}
