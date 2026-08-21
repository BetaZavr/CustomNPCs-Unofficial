package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSync;

public class CmdDialog {

   public static LiteralArgumentBuilder<CommandSourceStack> register() {
      LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("dialog");
      command.then(Commands.literal("reload").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))).executes((context) -> {
         (new DialogController()).load();
          for (DialogCategory category : DialogController.instance.categories.values()) {
              Packets.sendAll(new PacketSync(5, category.save(new CompoundTag()), false));
          }
          Packets.sendAll(new PacketSync(5, new CompoundTag(), true));
         return 1;
      });
      command.then(Commands.literal("read").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2)))
              .then(Commands.argument("players", EntityArgument.players()).then(Commands.argument("dialog", IntegerArgumentType.integer(0)).executes((context) -> {
                 Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                 if (!players.isEmpty()) {
                    Dialog dialog = DialogController.instance.dialogs.get(IntegerArgumentType.getInteger(context, "dialog"));
                    if (dialog == null) {
                       throw new CommandRuntimeException(Component.literal("Unknown DialogID"));
                    }
                    for (ServerPlayer player : players) {
                       PlayerData data = PlayerData.get(player);
                       if (!data.dialogData.has(dialog.id)) {
                          data.dialogData.read(dialog.id);
                          data.save(true);
                       }
                    }
                 }
                 return 1;
              })));
      command.then(Commands.literal("unread").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
              .then(Commands.argument("players", EntityArgument.players())
                      .then(Commands.argument("dialog", IntegerArgumentType.integer(0)).executes((context) -> {
                         Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                         if (!players.isEmpty()) {
                            Dialog dialog = DialogController.instance.dialogs.get(IntegerArgumentType.getInteger(context, "dialog"));
                            if (dialog == null) {
                               throw new CommandRuntimeException(Component.literal("Unknown DialogID"));
                            }
                            for (ServerPlayer player : players) {
                               PlayerData data = PlayerData.get(player);
                               if (data.dialogData.has(dialog.id)) {
                                  data.dialogData.dialogsRead.remove(dialog.id);
                                  data.save(true);
                               }
                            }
                         }
                         return 1;
                      }))));
      command.then(Commands.literal("show").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
              .then(Commands.argument("players", EntityArgument.players())
                      .then(Commands.argument("dialog", IntegerArgumentType.integer(0))
                              .then(Commands.argument("name", StringArgumentType.string()).executes((context) -> {
                                 Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                 if (!players.isEmpty()) {
                                     Dialog dialog = DialogController.instance.dialogs.get(IntegerArgumentType.getInteger(context, "dialog"));
                                     if (dialog == null) {
                                         throw new CommandRuntimeException(Component.literal("Unknown DialogID"));
                                     }
                                     EntityDialogNpc npc = new EntityDialogNpc(context.getSource().getLevel());
                                     npc.dialogs = new int[] { dialog.id };
                                     npc.display.setName(StringArgumentType.getString(context, "name"));
                                     for (ServerPlayer player : players) {
                                         EntityUtil.Copy(player, npc);
                                         NoppesUtilServer.openDialog(player, npc, dialog);
                                     }
                                 }
                                 return 1;
                              })))));
      return command;
   }

}
