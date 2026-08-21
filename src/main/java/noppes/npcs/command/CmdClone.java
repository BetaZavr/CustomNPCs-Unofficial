package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;

public class CmdClone {

   public static LiteralArgumentBuilder<CommandSourceStack> register() {
      LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("clone");
      command.then(Commands.literal("list")
              .requires((source) -> source.hasPermission(2))
              .then(Commands.argument("tab", IntegerArgumentType.integer(0))
                      .suggests(getTabSuggests())
                      .executes((context) -> {
                          int tab = IntegerArgumentType.getInteger(context, "tab");
                          context.getSource().sendSuccess(() -> Component.literal("--- Stored NPCs --- (server side)"), false);
                          for (String name : ServerCloneController.Instance.getClones(tab)) {
                              context.getSource().sendSuccess(() -> Component.literal(name), false);
                          }
                          context.getSource().sendSuccess(() -> Component.literal("------------------------------------"), false);
                          return 1;
                      })
              )
      );
      command.then(Commands.literal("add").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
              .then(Commands.argument("npc", StringArgumentType.string())
                      .then(Commands.argument("tab", IntegerArgumentType.integer(0))
                              .suggests(getTabSuggests())
                              .executes((context) -> {
                                  addClone(context, "");
                                  return 1;
                              })
                              .then(Commands.argument("name", StringArgumentType.string())
                                      .executes((context) -> {
                                          addClone(context, StringArgumentType.getString(context, "name"));
                                          return 1;
                                      })
                              ))));
      command.then(Commands.literal("remove").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
              .then(Commands.argument("npc", StringArgumentType.string())
                      .then(Commands.argument("tab", IntegerArgumentType.integer(0))
                              .suggests(getTabSuggests())
                              .executes((context) -> {
                                  String nameToDel = StringArgumentType.getString(context, "npc");
                                  int tab = IntegerArgumentType.getInteger(context, "tab");
                                  boolean deleted = false;
                                  for (String name : ServerCloneController.Instance.getClones(tab)) {
                                      if (nameToDel.equalsIgnoreCase(name)) {
                                          deleted = ServerCloneController.Instance.removeClone(name, tab);
                                          break;
                                      }
                                  }
                                  if (!deleted) { throw new CommandRuntimeException(Component.translatable("message.mod.error", tab, nameToDel)); }
                                  return 1;
                              })
                      )));
      command.then(Commands.literal("spawn").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2)).then(Commands.argument("npc", StringArgumentType.string())
              .then(Commands.argument("tab", IntegerArgumentType.integer(0))
                      .suggests(getTabSuggests())
                      .executes((context) -> {
                          spawnClone(context, new BlockPos((int) context.getSource().getPosition().x, (int) context.getSource().getPosition().y, (int) context.getSource().getPosition().z), "");
                          return 1;
                      })
                      .then(Commands.argument("pos", BlockPosArgument.blockPos())
                              .executes((context) -> {
                                  spawnClone(context, BlockPosArgument.getLoadedBlockPos(context, "pos"), "");
                                  return 1;
                              })
                              .then(Commands.argument("display_name", StringArgumentType.string())
                                      .executes((context) -> {
                                          spawnClone(context, BlockPosArgument.getLoadedBlockPos(context, "pos"), StringArgumentType.getString(context, "display_name"));
                                          return 1;
                                      })
                              )))));
      command.then(Commands.literal("grid").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
              .then(Commands.argument("npc", StringArgumentType.string())
                      .then(Commands.argument("tab", IntegerArgumentType.integer(0))
                              .suggests(getTabSuggests())
                              .then(Commands.argument("length", IntegerArgumentType.integer())
                                      .then(Commands.argument("width", IntegerArgumentType.integer())
                                              .then(Commands.argument("splitx", IntegerArgumentType.integer())
                                                      .then(Commands.argument("splitz", IntegerArgumentType.integer()).executes((context) -> {
                                                          int length = IntegerArgumentType.getInteger(context, "length");
                                                          int width = IntegerArgumentType.getInteger(context, "width");
                                                          int splitx = IntegerArgumentType.getInteger(context, "splitx");
                                                          int splitz = IntegerArgumentType.getInteger(context, "splitz");
                                                          for(int x = 0; x < length; ++x) {
                                                              for(int z = 0; z < width; ++z) {
                                                                  spawnClone(context, (new BlockPos((int) context.getSource().getPosition().x,
                                                                          (int) context.getSource().getPosition().y,
                                                                          (int) context.getSource().getPosition().z
                                                                  ))
                                                                          .offset(x * splitx, 0, z * splitz), "");
                                                              }
                                                          }

                                                          return 1;
                                                      }))
                                              .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                      .executes((context) -> {
                                                          int length = IntegerArgumentType.getInteger(context, "length");
                                                          int width = IntegerArgumentType.getInteger(context, "width");
                                                          int splitx = IntegerArgumentType.getInteger(context, "splitx");
                                                          int splitz = IntegerArgumentType.getInteger(context, "splitz");
                                                          for(int x = 0; x < length; ++x) {
                                                              for(int z = 0; z < width; ++z) {
                                                                  spawnClone(context, BlockPosArgument.getLoadedBlockPos(context, "pos")
                                                                          .offset(x * splitx, 0, z * splitz), "");
                                                              }
                                                          }
                                                          return 1;
                                                      })
                                                      .then(Commands.argument("display_name", StringArgumentType.string())
                                                              .executes((context) -> {
                                                                  int length = IntegerArgumentType.getInteger(context, "length");
                                                                  int width = IntegerArgumentType.getInteger(context, "width");
                                                                  int splitx = IntegerArgumentType.getInteger(context, "splitx");
                                                                  int splitz = IntegerArgumentType.getInteger(context, "splitz");
                                                                  for(int x = 0; x < length; ++x) {
                                                                      for(int z = 0; z < width; ++z) {
                                                                          spawnClone(context, BlockPosArgument.getLoadedBlockPos(context, "pos")
                                                                                  .offset(x * splitx, 0, z * splitz),
                                                                                  StringArgumentType.getString(context, "display_name"));
                                                                      }
                                                                  }
                                                                  return 1;
                                                              })
                                                      )))))
      )));
      return command;
   }

   private static SuggestionProvider<CommandSourceStack> getTabSuggests() {
      return (context, builder) -> {
         for (int i = 0; i <= 9; ++i) {
            builder.suggest(String.valueOf(i));
         }
         return builder.buildFuture();
      };
   }

   private static void addClone(CommandContext<CommandSourceStack> context, String newName) {
      String name = StringArgumentType.getString(context, "npc");
      if (newName.isEmpty()) {
         newName = name;
      }
      int tab = IntegerArgumentType.getInteger(context, "tab");
      List<EntityNPCInterface> list = CmdNoppes.getNpcsByName(context.getSource().getLevel(), name);
      if (!list.isEmpty()) {
         EntityNPCInterface npc = list.get(0);
         CompoundTag compound = new CompoundTag();
         if (npc.saveAsPassenger(compound)) {
            ServerCloneController.Instance.addClone(compound, newName, tab);
         }
      }
   }

   private static void spawnClone(CommandContext<CommandSourceStack> context, BlockPos pos, String newName) {
      String name = StringArgumentType.getString(context, "npc").replaceAll("%", " ");
      int tab = IntegerArgumentType.getInteger(context, "tab");
      CompoundTag compound = ServerCloneController.Instance.getCloneData(context.getSource(), name, tab);
      if (compound == null) {
         throw new CommandRuntimeException(Component.literal("Unknown npc"));
      } else if (pos == BlockPos.ZERO) {
         throw new CommandRuntimeException(Component.literal("Location needed"));
      } else {
         Level world = context.getSource().getLevel();
         Optional<Entity> p = EntityType.create(compound, world);
         if (p.isEmpty()) { return; }
         Entity entity = p.get();
         entity.setPos((double) pos.getX() + 0.5D, (double) pos.getY() + 1.0D, (double) pos.getZ() + 0.5D);
         if (entity instanceof EntityNPCInterface npc) {
            npc.ais.setStartPos(pos);
            if (!newName.isEmpty()) {
               npc.display.setName(newName.replaceAll("%", " "));
            }
         }
         world.addFreshEntity(entity);
      }
   }

}
