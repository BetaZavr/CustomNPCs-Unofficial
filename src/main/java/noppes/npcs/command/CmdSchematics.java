package noppes.npcs.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.schematics.SchematicWrapper;

public class CmdSchematics {

   public static final List<String> names = new ArrayList<>();

   public static final SuggestionProvider<CommandSourceStack> SCHEMAS = SuggestionProviders.register(new ResourceLocation(CustomNpcs.MODID, "schemas"), (context, builder) -> SharedSuggestionProvider.suggest(names.stream(), builder));
   public static final SuggestionProvider<CommandSourceStack> ROTATION = SuggestionProviders.register(new ResourceLocation(CustomNpcs.MODID, "rotation"), (context, builder) -> SharedSuggestionProvider.suggest(new String[]{"0", "90", "180", "270"}, builder));

   public static LiteralArgumentBuilder<CommandSourceStack> register() {
       return ((((Commands.literal("schema")
               .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2)))
               .then(Commands.literal("build")
                       .then(Commands.argument("name", StringArgumentType.word()).suggests(SCHEMAS)
                               .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                       .then(Commands.argument("rotation", StringArgumentType.word()).suggests(ROTATION).executes((context) -> {
                                           String name = StringArgumentType.getString(context, "name");
                                           BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                           int rotation = Integer.parseInt(StringArgumentType.getString(context, "rotation"));
                                           SchematicWrapper schem = SchematicController.Instance.load(name);
                                           schem.init(pos, context.getSource().getLevel(), rotation);
                                           SchematicController.Instance.build(schem, context.getSource());
                                           return 1;
                                        }))))))
               .then(Commands.literal("stop").executes((context) -> {
          SchematicController.Instance.stop(context.getSource());
          return 1;
       }))).then(Commands.literal("info").executes((context) -> {
          SchematicController.Instance.info(context.getSource());
          return 1;
       }))).then(Commands.literal("list").executes((context) -> {
          List<String> list = SchematicController.Instance.list();
          if (list.isEmpty()) {
             context.getSource().sendSuccess(() -> Component.translatable("schemas.no.available"), false);
          } else {
             String files = "";
             int i = 0;
             for (String file : list) {
                files += file;
                if (i < list.size() -1) { files += ", "; }
                i++;
             }
             String finalFiles = files;
             context.getSource().sendSuccess(() -> Component.translatable(finalFiles), false);
          }
          return 1;
       }));
   }

}
