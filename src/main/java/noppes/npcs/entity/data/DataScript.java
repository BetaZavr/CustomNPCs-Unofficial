package noppes.npcs.entity.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.BaseScriptData;
import noppes.npcs.entity.EntityNPCInterface;

public class DataScript extends BaseScriptData {

   protected final EntityNPCInterface npc;

   public DataScript(EntityNPCInterface npcIn) { npc = npcIn; }

   @Override
   @SuppressWarnings("ConstantConditions")
   public boolean isClient() {
      if (npc == null || npc.level() == null) { return super.isClient(); }
      return npc.level().isClientSide();
   }

   @Override
   @SuppressWarnings("ConstantConditions")
   public MutableComponent noticeString(String type, Object event) {
      MutableComponent message = Component.literal("NPC Script").withStyle(ChatFormatting.DARK_GRAY);
      if (type != null) {
         message.append(Component.literal(" hook \"").withStyle(ChatFormatting.DARK_GRAY))
                 .append(Component.literal(type).withStyle(ChatFormatting.GRAY))
                 .append(Component.literal("\"; ").withStyle(ChatFormatting.DARK_GRAY));
      }
      else { message.append(Component.literal("; ").withStyle(ChatFormatting.DARK_GRAY)); }
      message.append(Component.literal("name \"").withStyle(ChatFormatting.DARK_GRAY))
              .append(Component.literal(npc != null ? npc.getName().getString() : "null").withStyle(ChatFormatting.GRAY))
              .append(Component.literal("\"; UUID: \"").withStyle(ChatFormatting.DARK_GRAY))
              .append(Component.literal(npc != null ? npc.getUUID().toString() : "null").withStyle(ChatFormatting.GRAY))
              .append(Component.literal("\" in ").withStyle(ChatFormatting.DARK_GRAY));
      String dimID = npc == null || npc.level() == null ? "overworld" : npc.level().dimensionTypeId().location().toString();
      double x = npc != null ? Math.round(npc.getX() * 100.0d) / 100.0d : 0.0d;
      double y = npc != null ? Math.round(npc.getY() * 100.0d) / 100.0d : 0.0d;
      double z = npc != null ? Math.round(npc.getZ() * 100.0d) / 100.0d : 0.0d;
      MutableComponent posClick = Component.literal("dimension ID:" + dimID + "; X:" + x + "; Y:" + y + "; Z:" + z);
      Style style = posClick.getStyle().withColor(ChatFormatting.BLUE)
              .withUnderlined(true)
              .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/noppes world tp @p " + dimID + " " + x + " " + y + " "+z))
              .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("script.hover.error.pos.tp")));
      posClick.setStyle(style);
      return message.append(Component.literal("; Side: " + (isClient() ? "Client" : "Server")).withStyle(ChatFormatting.DARK_GRAY));
   }

   @Override
   public void runScript(String type, Event event) {
      if (isEnabled()) {
         if (ScriptController.Instance.lastLoaded > lastInited) {
            lastInited = ScriptController.Instance.lastLoaded;
            if (!type.equals(EnumScriptType.INIT.function)) { EventHooks.onNPCInit(npc); }
         }
         for (ScriptContainer script : scripts) { script.run(type, event); }
      }
   }

   public EntityNPCInterface getNPC() { return npc; }

}
