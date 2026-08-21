package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import java.util.Iterator;
import java.util.Map.Entry;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.data.DataScenes;

public class CmdScene {
   public static LiteralArgumentBuilder<CommandSourceStack> register() {
       return (((Commands.literal("scene")
               .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2)))
               .then(Commands.literal("time").executes((context) -> {
                   context.getSource().sendSuccess(() -> Component.literal("Active scenes:"), false);
                   for (Entry<String, DataScenes.SceneState> entry : DataScenes.StartedScenes.entrySet()) {
                      context.getSource().sendSuccess(() -> Component.translatable("scene.time.get", entry.getKey(), entry.getValue().ticks), false);
                   }
                   return 1;
                }).then(Commands.argument("time", IntegerArgumentType.integer(0)).executes((context) -> {
          int ticks = IntegerArgumentType.getInteger(context, "time");

          DataScenes.SceneState state;
          for(Iterator<DataScenes.SceneState> var2 = DataScenes.StartedScenes.values().iterator(); var2.hasNext(); state.ticks = ticks) {
             state = var2.next();
          }
          return 1;
       }).then(Commands.argument("name", StringArgumentType.string()).executes((context) -> {
          String name = StringArgumentType.getString(context, "name");
          DataScenes.SceneState state = DataScenes.StartedScenes.get(name.toLowerCase());
          if (state == null) {
             throw new CommandRuntimeException(Component.translatable("scene.unknown", name));
          } else {
             state.ticks = IntegerArgumentType.getInteger(context, "time");
             context.getSource().sendSuccess(() -> Component.translatable("scene.time.set", name, state.ticks), false);
             return 1;
          }
       })).then(Commands.literal("reset").executes((context) -> {
          DataScenes.Reset(null, null);
          return 1;
       })).then(Commands.argument("name", StringArgumentType.string()).executes((context) -> {
          DataScenes.Reset(StringArgumentType.getString(context, "name"), null);
          return 1;
       })).then(Commands.literal("start").then(Commands.argument("name", StringArgumentType.string()).executes((context) -> {
          DataScenes.Start(StringArgumentType.getString(context, "name"), null);
          return 1;
       })).then(Commands.literal("pause").executes((context) -> {
          DataScenes.Pause(null, null);
          return 1;
       })).then(Commands.argument("name", StringArgumentType.string()).executes((context) -> {
          DataScenes.Pause(StringArgumentType.getString(context, "name"), null);
          return 1;
       })))))));
   }
}
