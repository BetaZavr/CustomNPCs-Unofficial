package noppes.npcs.controllers.data;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;

public class NpcScriptData extends BaseScriptData {

    @Override
    public MutableComponent noticeString(String type, Object event) {
        MutableComponent message = Component.literal("NPC's Scripts").withStyle(ChatFormatting.DARK_GRAY);
        if (type != null) {
            message.append(Component.literal(" hook \"").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(type).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\"; ").withStyle(ChatFormatting.DARK_GRAY));
        }
        else { message.append(Component.literal("; ").withStyle(ChatFormatting.DARK_GRAY)); }
        boolean bo = event instanceof NpcEvent && ((NpcEvent) event).npc != null;
        if (bo) {
            String dimID = ((NpcEvent) event).npc.getWorld() == null ? "overworld" : ((NpcEvent) event).npc.getWorld().getMCLevel().dimensionTypeId().location().toString();
            double x = Math.round(((NpcEvent) event).npc.getX() * 100.0d) / 100.0d;
            double y = Math.round(((NpcEvent) event).npc.getY() * 100.0d) / 100.0d;
            double z = Math.round(((NpcEvent) event).npc.getZ() * 100.0d) / 100.0d;
            MutableComponent posClick = Component.literal("dimension ID:" + dimID + "; X:" + x + "; Y:" + y + "; Z:" + z);
            Style style = posClick.getStyle().withColor(ChatFormatting.BLUE);
            style = style.withUnderlined(true);
            style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/noppes world tp @p " + dimID + " " + x + " " + y + " "+z));
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("script.hover.error.pos.tp")));
            posClick.setStyle(style);
            message.append(Component.literal("NPC \"").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(((NpcEvent) event).npc.getName()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\"; UUID: \"").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(((NpcEvent) event).npc.getUUID()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\" in ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(posClick);
        }
        return message.append(Component.literal((bo ? "; " : "") +"Side: " + (isClient() ? "Client" : "Server")).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void runScript(String type, Event event) {
        if (isEnabled()) {
            CustomNPCsScheduler.runTack(() -> {
                try {
                    if (ScriptController.Instance.lastLoaded > lastInited) {
                        lastInited = ScriptController.Instance.lastLoaded;
                        if (!type.equalsIgnoreCase(EnumScriptType.INIT.function)) {
                            EventHooks.onNPCsInit(this);
                        }
                    }
                    for (ScriptContainer script : scripts) { script.run(type, event); }
                }
                catch (Exception e) { LogWriter.error("Error:", e); }
            });
        }
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        EventHooks.onNPCsInit(this);
    }

}
