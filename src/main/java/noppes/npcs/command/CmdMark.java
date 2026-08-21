package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import java.awt.*;
import java.util.Collection;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.data.MarkData;

public class CmdMark {

   public static LiteralArgumentBuilder<CommandSourceStack> register() {
      LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("mark")
              .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2));
      command.then(Commands.argument("clear", EntityArgument.entities()).executes((context) -> {
         Collection<? extends Entity> entities = EntityArgument.getEntities(context, "entities");
         for (Entity entity : entities) {
            if (entity instanceof LivingEntity) {
               MarkData data = MarkData.get((LivingEntity) entity);
               data.marks.clear();
               data.syncClients();
            }
         }
         return 1;
      }));
      command.then(Commands.argument("entities", EntityArgument.entities())
              .then((Commands.argument("type", IntegerArgumentType.integer(0)).executes((context) -> {
                         Collection<? extends Entity> entities = EntityArgument.getEntities(context, "entities");
                          if (!entities.isEmpty()) {
                              int type = IntegerArgumentType.getInteger(context, "type");
                              for (Entity entity : entities) {
                                  if (entity instanceof LivingEntity) {
                                      MarkData data = MarkData.get((LivingEntity) entity);
                                      data.marks.clear();
                                      MarkData.Mark mark = data.addMark(type, 0);
                                      mark.setColor(new Color(0xFFFFFF).getRGB());
                                  }
                              }
                          }
                          return 1;
                      })
                      .then(Commands.argument("color", StringArgumentType.word()))).executes((context) -> {
                 Collection<? extends Entity> entities = EntityArgument.getEntities(context, "entities");
                 if (!entities.isEmpty()) {
                    int type = IntegerArgumentType.getInteger(context, "type");
                    int color = new Color(0xFFFFFF).getRGB();
                    try { color = Integer.parseInt(StringArgumentType.getString(context, "color"), 16); }
                    catch (Exception ignored) {}
                    for (Entity entity : entities) {
                       if (entity instanceof LivingEntity) {
                          MarkData data = MarkData.get((LivingEntity) entity);
                          data.marks.clear();
                          data.addMark(type, color);
                       }
                    }
                 }
                 return 1;
              })));
      return command;
   }

}
