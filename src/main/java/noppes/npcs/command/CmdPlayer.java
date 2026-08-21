package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.command.arguments.PlayerDataArgument;
import noppes.npcs.command.arguments.URLArgument;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.PlayerSkinController;
import noppes.npcs.controllers.data.Marcet;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.SkinData;
import noppes.npcs.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CmdPlayer {

   public static final SimpleCommandExceptionType NO_TYPE = new SimpleCommandExceptionType(Component.translatable("argument.texture.not.type"));
    public static final SimpleCommandExceptionType NO_REMOVE_DATA = new SimpleCommandExceptionType(Component.translatable("argument.entity.notfound.player"));

   public static LiteralArgumentBuilder<CommandSourceStack> register() {
      return Commands.literal("player")
              .then(registerOpenMarcet())
              .then(Commands.literal("all")
                      .then(registerClears())
              )
              .then(Commands.argument("playername", PlayerDataArgument.dataArg())
                      .suggests(PlayerDataArgument.getSuggests())
                      .then(registerSkins())
                      .then(registerCapes())
                      .then(registerElytras())
              )
              // New from Unofficial (GoodBird)
              .then(Commands.literal("data")
                      .then(registerData())
              );
    }

    public static ArgumentBuilder<CommandSourceStack,?> registerOpenMarcet() {
        return Commands.literal("openmarcet")
                .requires((source) -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("marcetID", IntegerArgumentType.integer())
                                .suggests((context, builder) -> {
                                    for (int id : MarcetController.getInstance().getMarketIDs()) { builder.suggest(id); }
                                    return builder.buildFuture();
                                })
                                .executes((context) -> {
                                    int marcetId = IntegerArgumentType.getInteger(context, "marcetID");
                                    Marcet marcet = MarcetController.getInstance().getMarcet(marcetId);
                                    if (marcet == null || !marcet.isValid()) {
                                        context.getSource().sendSuccess(() -> Component.translatable("command.player.openmarcet.error", marcetId), true);
                                    } else {
                                        NoppesUtilServer.openContainerGui(EntityArgument.getPlayer(context, "player"), EnumGuiType.PlayerTrader, buf -> buf.writeInt(marcetId));
                                    }
                                    return 1;
                                })));
    }

    private static ArgumentBuilder<CommandSourceStack,?> registerClears() {
        return Commands.literal("clear")
                .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .then(Commands.literal("skins").executes((context) -> {
                    skinsClear(null, context.getSource(), 0);
                    return 1;
                }))
                .then(Commands.literal("capes").executes((context) -> {
                    skinsClear(null, context.getSource(), 1);
                    return 1;
                }))
                .then(Commands.literal("elytras").executes((context) -> {
                    skinsClear(null, context.getSource(), 2);
                    return 1;
                }));
    }

    private static ArgumentBuilder<CommandSourceStack,?> registerSkins() {
        return Commands.literal("skin")
                .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .then(Commands.literal("get").executes((context) -> {
                    skinsGet(PlayerDataArgument.getData(context, "playername"), context.getSource(), 0);
                    return 1;
                }))
                .then(Commands.literal("clear").executes((context) -> {
                    skinsClear(PlayerDataArgument.getData(context, "playername"), context.getSource(), 0);
                    return 1;
                }))
                .then(Commands.literal("set")
                        .then(Commands.literal("url")
                                .then(Commands.argument("urllink", URLArgument.urlArg())
                                        .suggests(URLArgument.getSuggests()).executes((context) -> {
                                            skinsSet(PlayerDataArgument.getData(context, "playername"), context.getSource(), 0, 0, URLArgument.getURL(context, "urllink"));
                                            return 1;
                                        })))
                        .then(Commands.literal("location")
                                .then(Commands.argument("locationpath", ResourceLocationArgument.id()).executes((context) -> {
                                    skinsSet(PlayerDataArgument.getData(context, "playername"), context.getSource(), 0, 1, ResourceLocationArgument.getId(context, "locationpath").toString());
                                    return 1;
                                })))
                        .then(Commands.literal("composite")
                                .then(Commands.argument("genderID", IntegerArgumentType.integer()).suggests((context, builder) -> {
                                            builder.suggest(0);
                                            builder.suggest(1);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("bodyID", IntegerArgumentType.integer()).suggests((context, builder) -> {
                                                    builder.suggest(0);
                                                    for (int i : getPNGFileNames(IntegerArgumentType.getInteger(context, "genderID"), "torsos")) { builder.suggest(i); }
                                                    return builder.buildFuture();
                                                })
                                                .then(Commands.argument("bodyColor", IntegerArgumentType.integer()).suggests((context, builder) -> {
                                                            builder.suggest(0);
                                                            builder.suggest(16777215);
                                                            return builder.buildFuture();
                                                        })
                                                        .then(Commands.argument("hairID", IntegerArgumentType.integer()).suggests((context, builder) -> {
                                                                    builder.suggest(0);
                                                                    for (int i : getPNGFileNames(IntegerArgumentType.getInteger(context, "genderID"), "hairs")) { builder.suggest(i); }
                                                                    return builder.buildFuture();
                                                                })
                                                                .then(Commands.argument("hairColor", IntegerArgumentType.integer()).suggests((context, builder) -> {
                                                                            builder.suggest(0);
                                                                            builder.suggest(16777215);
                                                                            return builder.buildFuture();
                                                                        })
                                                                        .then(Commands.argument("faceID", IntegerArgumentType.integer()).suggests((context, builder) -> {
                                                                                    builder.suggest(0);
                                                                                    for (int i : getPNGFileNames(IntegerArgumentType.getInteger(context, "genderID"), "faces")) { builder.suggest(i); }
                                                                                    return builder.buildFuture();
                                                                                })
                                                                                .then(Commands.argument("eyesColor", IntegerArgumentType.integer()).suggests((context, builder) -> {
                                                                                            builder.suggest(0);
                                                                                            builder.suggest(16777215);
                                                                                            return builder.buildFuture();
                                                                                        })
                                                                                        .then(Commands.argument("legID", IntegerArgumentType.integer()).suggests((context, builder) -> {
                                                                                                    builder.suggest(0);
                                                                                                    for (int i : getPNGFileNames(IntegerArgumentType.getInteger(context, "genderID"), "legs")) { builder.suggest(i); }
                                                                                                    return builder.buildFuture();
                                                                                                })
                                                                                                .then(Commands.argument("jacketID", IntegerArgumentType.integer()).suggests((context, builder) -> {
                                                                                                                    builder.suggest(0);
                                                                                                                    for (int i : getPNGFileNames(IntegerArgumentType.getInteger(context, "genderID"), "jackets")) { builder.suggest(i); }
                                                                                                                    return builder.buildFuture();
                                                                                                                })
                                                                                                        .then(Commands.argument("shoesID", IntegerArgumentType.integer()).suggests((context, builder) -> {
                                                                                                                    builder.suggest(0);
                                                                                                                    for (int i : getPNGFileNames(IntegerArgumentType.getInteger(context, "genderID"), "shoes")) { builder.suggest(i); }
                                                                                                                    return builder.buildFuture();
                                                                                                                }).executes((context) -> {
                                                                                                                    String location = CustomNpcs.MODID + "textures/entity/custom/" + (IntegerArgumentType.getInteger(context, "genderID")== 0 ? "male" : "female");
                                                                                                                    location += "_" + IntegerArgumentType.getInteger(context, "bodyID");
                                                                                                                    location += "_" + IntegerArgumentType.getInteger(context, "bodyColor");
                                                                                                                    location += "_" + IntegerArgumentType.getInteger(context, "hairID");
                                                                                                                    location += "_" + IntegerArgumentType.getInteger(context, "hairColor");
                                                                                                                    location += "_" + IntegerArgumentType.getInteger(context, "faceID");
                                                                                                                    location += "_" + IntegerArgumentType.getInteger(context, "eyesColor");
                                                                                                                    location += "_" + IntegerArgumentType.getInteger(context, "legID");
                                                                                                                    location += "_" + IntegerArgumentType.getInteger(context, "jacketID");
                                                                                                                    location += "_" + IntegerArgumentType.getInteger(context, "shoesID");
                                                                                                                    location += ".png";
                                                                                                                    skinsSet(PlayerDataArgument.getData(context, "playername"), context.getSource(), 0, 2, location);
                                                                                                                    return 1;
                                                                                                                })
                                                                                                        )
                                                                                                ))))))))))
                );
    }

    private static ArgumentBuilder<CommandSourceStack,?> registerCapes() {
        return Commands.literal("cape")
                .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .then(Commands.literal("get").executes((context) -> {
                    skinsGet(PlayerDataArgument.getData(context, "playername"), context.getSource(), 1);
                    return 1;
                }))
                .then(Commands.literal("clear").executes((context) -> {
                            skinsClear(PlayerDataArgument.getData(context, "playername"), context.getSource(), 1);
                            return 1;
                        })
                )
                .then(Commands.literal("set")
                        .then(Commands.literal("url")
                                .then(Commands.argument("urllink", URLArgument.urlArg()).suggests(URLArgument.getSuggests()).executes((context) -> {
                                    skinsSet(PlayerDataArgument.getData(context, "playername"), context.getSource(), 1, 0, URLArgument.getURL(context, "urllink"));
                                    return 1;
                                })))
                        .then(Commands.literal("location")
                                .then(Commands.argument("locationpath", ResourceLocationArgument.id()).executes((context) -> {
                                    skinsSet(PlayerDataArgument.getData(context, "playername"), context.getSource(), 1, 1, ResourceLocationArgument.getId(context, "locationpath").toString());
                                    return 1;
                                })))
                );
    }

    private static ArgumentBuilder<CommandSourceStack,?> registerElytras() {
        return Commands.literal("elytra")
                .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .then(Commands.literal("get").executes((context) -> {
                    skinsGet(PlayerDataArgument.getData(context, "playername"), context.getSource(), 2);
                    return 1;
                }))
                .then(Commands.literal("clear").executes((context) -> {
                            skinsClear(PlayerDataArgument.getData(context, "playername"), context.getSource(), 2);
                            return 1;
                        })
                )
                .then(Commands.literal("set")
                        .then(Commands.literal("url")
                                .then(Commands.argument("urllink", URLArgument.urlArg()).suggests(URLArgument.getSuggests()).executes((context) -> {
                                    skinsSet(PlayerDataArgument.getData(context, "playername"), context.getSource(), 2, 0, URLArgument.getURL(context, "urllink"));
                                    return 1;
                                })))
                        .then(Commands.literal("location")
                                .then(Commands.argument("locationpath", ResourceLocationArgument.id()).executes((context) -> {
                                    skinsSet(PlayerDataArgument.getData(context, "playername"), context.getSource(), 2, 1, ResourceLocationArgument.getId(context, "locationpath").toString());
                                    return 1;
                                })))
                );
    }

    private static ArgumentBuilder<CommandSourceStack,?> registerData() {
        return Commands.literal("remove")
                .then(Commands.argument("playername", PlayerDataArgument.dataArg())
                        .suggests(PlayerDataArgument.getSuggests())
                        .executes(context -> {
                            PlayerData data = PlayerDataArgument.getData(context, "playername");
                            if (data == null) { throw EntityArgument.NO_PLAYERS_FOUND.create(); }
                            // remove dir
                            File playerDir = new File(CustomNpcs.getLevelSaveDirectory("playerdata"), data.uuid);
                            if (playerDir.exists() &&
                                    !Util.instance.removeFile(new File(CustomNpcs.getLevelSaveDirectory("playerdata"), data.uuid))) {
                                throw NO_REMOVE_DATA.create();
                            }
                            data.clear();
                            data.save(true);
                            // player
                            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(data.name);
                            context.getSource().sendSuccess(() -> Component.translatable("command.player.data.remove", data.name, (player != null ? "online" : "offline")), true);
                            return 1;
                        }));
    }

    private static List<Integer> getPNGFileNames(int genderID, String dirName) {
        File dir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/textures/entity/custom/" + (genderID == 0 ? "male" : "female") + "/" + dirName);
        File[] array = dir.listFiles();
        List<Integer> list = new ArrayList<>();
        if (array != null) {
            for (File file : array) {
                if (file.isDirectory() || file.getName().endsWith(".png")) { continue; }
                String n = file.getName().toLowerCase().substring(0, file.getName().lastIndexOf(".png"));
                try {
                    int i = Integer.parseInt(n);
                    if (i > 0) { list.add(i); }
                } catch (Exception ignored) { }
            }
        }
        Collections.sort(list);
        return list;
    }

    private static void skinsSet(PlayerData data, CommandSourceStack source, int slot, int type, String location) throws CommandSyntaxException {
       if (slot < 0 || type < 0 || type > 2 || slot > 2 || (slot != 0 && type > 1)) { throw NO_TYPE.create(); }
        try {
            UUID uuid = UUID.fromString(data.uuid);
            PlayerSkinController sData = PlayerSkinController.getInstance();
            sData.create(uuid, data.name, slot, type, location);
            String t = switch (type) {
                case 0 -> "(URL) ";
                case 1 -> "(Location) ";
                default -> "(Composite) ";
            };
            source.sendSuccess(() -> Component.translatable("command.player.set."+slot, data.name, t + location), true);
        } catch (Exception e) {
            throw EntityArgument.NO_PLAYERS_FOUND.create();
        }
    }

    private static void skinsClear(PlayerData data, CommandSourceStack source, int type) throws CommandSyntaxException {
        if (type < 0 || type > 2) { throw NO_TYPE.create(); }
        PlayerSkinController sData = PlayerSkinController.getInstance();
        if (data == null) { // all
            sData.clear(null, type);
            source.sendSuccess(() -> Component.translatable("command.player.clear.skin.all."+type), true);
            return;
        }
        if (data.uuid.isEmpty()) { throw EntityArgument.NO_PLAYERS_FOUND.create(); }
        sData.clear(data.uuid, type);
        source.sendSuccess(() -> Component.translatable("command.player.clear."+type, data.name), true);
    }

    private static void skinsGet(PlayerData data, CommandSourceStack source, int type) throws CommandSyntaxException {
        if (type < 0 || type > 2) { throw NO_TYPE.create(); }
       try {
           SkinData skinData = PlayerSkinController.getInstance().get(data.uuid, type);
           if (skinData == null) { throw EntityArgument.NO_PLAYERS_FOUND.create(); }
           String skin;
           if (skinData.isUrl()) { skin = "(URL) " + skinData.getUrl(); }
           else if (skinData.isLocation()) { skin = "(Location) " + skinData.getLocation(); }
           else {
               ResourceLocation location = skinData.getLocation();
               if (location == null) { skin = "(Not set or create texture)"; }
               else { skin = "(Composite) " + skinData.getLocation(); }
           }
           source.sendSuccess(() -> Component.translatable("command.player.get."+type, data.name, skin), true);
       } catch (Exception e) { throw EntityArgument.NO_PLAYERS_FOUND.create(); }
    }

}
