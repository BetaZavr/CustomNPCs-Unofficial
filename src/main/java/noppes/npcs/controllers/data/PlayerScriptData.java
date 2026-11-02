package noppes.npcs.controllers.data;

import java.util.*;
import java.util.Map.Entry;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;

public class PlayerScriptData extends BaseScriptData {

   protected static final Map<ServerPlayer, List<ScriptContainer>> PLAYER_CASH = new HashMap<>();
   protected static final TreeMap<Long, String> console = new TreeMap<>();
   protected static final List<Integer> errored = new ArrayList<>();

   @Override
   public void clear() {
      console.clear();
      errored.clear();
      scripts.clear();
      PLAYER_CASH.clear();
   }

   @Override
   public void load(CompoundTag compound) {
      super.load(compound);
      console.clear();
      console.putAll(NBTTags.getLongStringMap(compound.getList("ScriptConsole", 10)));
   }

   @Override
   public CompoundTag save(CompoundTag compound) {
      super.save(compound);
      compound.put("ScriptConsole", NBTTags.nbtLongStringMap(console));
      return compound;
   }

   @Override
   public void runScript(String type, Event event) {
      if (!isEnabled() ||
              !(event instanceof PlayerEvent playerEvent) ||
              playerEvent.player == null ||
              !(playerEvent.player.getMCEntity() instanceof ServerPlayer player)) { return; }
      if (ScriptController.Instance.lastLoaded > lastInited) {
         errored.clear();
         lastInited = ScriptController.Instance.lastLoaded;
         if (!type.equals(EnumScriptType.INIT.function)) {
            for (ServerPlayer pl : PLAYER_CASH.keySet()) {
               PLAYER_CASH.get(pl).clear();
               EventHooks.onPlayerInit(new PlayerEvent.InitEvent(PlayerData.get(pl).getIPlayer()));
            }
         }
      } // init event
      if (!PLAYER_CASH.containsKey(player)) {
         ArrayList<ScriptContainer> list = new ArrayList<>();
         for (ScriptContainer scriptContainer : scripts) {
            ScriptContainer s = new ScriptContainer(this);
            s.load(scriptContainer.save(new CompoundTag()));
            list.add(s);
         }
         PLAYER_CASH.put(player, list);
         if (!type.equals(EnumScriptType.INIT.function)) {
            EventHooks.onPlayerInit(new PlayerEvent.InitEvent(PlayerData.get(player).getIPlayer()));
         }
      } // create cash to player
      if (PLAYER_CASH.containsKey(player)) {
         ScriptContainer script;
         for(int i = 0; i < PLAYER_CASH.get(player).size(); ++i) {
            if (!errored.contains(i)) {
               script = scripts.get(i);
               script.run(type, event);
               if (script.isErrored()) { errored.add(i); }
               for (Entry<Long, String> entry : script.console.entrySet()) {
                  if (!console.containsKey(entry.getKey())) { console.put(entry.getKey(), " tab " + (i + 1) + ":\n" + entry.getValue()); }
               }
               script.console.clear();
            }
         }
      } // run
      while (console.size() > 40) { console.remove(console.firstKey()); }
   }

   @Override
   public String getLanguage() {
      return ScriptController.Instance.playerScripts.scriptLanguage;
   }

   @Override
   public void setLanguage(String lang) { ScriptController.Instance.playerScripts.scriptLanguage = lang; }

   @Override
   public MutableComponent noticeString(String type, Object event) {
      MutableComponent message = Component.literal("Players script")
              .withStyle(ChatFormatting.DARK_GRAY);
      if (type != null) {
         message.append(Component.literal(" hook \"").withStyle(ChatFormatting.DARK_GRAY))
                 .append(Component.literal(type).withStyle(ChatFormatting.GRAY))
                 .append(Component.literal("\"; ").withStyle(ChatFormatting.DARK_GRAY));
      }
      else { message.append(Component.literal("; ").withStyle(ChatFormatting.DARK_GRAY)); }
      if (event instanceof PlayerEvent && ((PlayerEvent) event).player instanceof Player player) {
         String dimID = player.level().dimensionTypeId().location().toString();
         double x = Math.round(player.getX() * 100.0d) / 100.0d;
         double y = Math.round(player.getY() * 100.0d) / 100.0d;
         double z = Math.round(player.getZ() * 100.0d) / 100.0d;
         MutableComponent posClick = Component.literal("dimension ID:" + dimID + "; X:" + x + "; Y:" + y + "; Z:" + z);
         Style style = posClick.getStyle().withColor(ChatFormatting.BLUE);
         style = style.withUnderlined(true);
         style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/noppes world tp @p " + dimID + " " + x + " " + y + " "+z));
         style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("script.hover.error.pos.tp")));
         posClick.setStyle(style);
         message.append(Component.literal("name: \"").withStyle(ChatFormatting.DARK_GRAY))
                 .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.GRAY))
                 .append(Component.literal("\"; UUID: \"").withStyle(ChatFormatting.DARK_GRAY))
                 .append(Component.literal(player.getUUID().toString()).withStyle(ChatFormatting.GRAY))
                 .append(Component.literal("\" in ").withStyle(ChatFormatting.DARK_GRAY))
                 .append(posClick);
      }
      return message.append(Component.literal("; Side: " + (isClient() ? "Client" : "Server")).withStyle(ChatFormatting.DARK_GRAY));
   }

   @Override
   public TreeMap<Long, String> getConsoleText() { return console; }

   @Override
   public void clearConsole() { console.clear(); }

   public static void removePlayer(ServerPlayer player) {
      if (player != null) { PLAYER_CASH.remove(player); }
   }

}
