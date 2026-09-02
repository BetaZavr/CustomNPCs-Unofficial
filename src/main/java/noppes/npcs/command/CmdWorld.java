package noppes.npcs.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.DimensionController;
import noppes.npcs.mixin.world.level.ILevelMixin;
import noppes.npcs.util.Util;

import java.util.Collection;

public class CmdWorld {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("world")
                .requires((source) -> source.hasPermission(2))
                .then(Commands.literal("tp") // TeleportCommand
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("dimension", DimensionArgument.dimension()).suggests(getLevelSuggests())
                                        .executes((context) -> {
                                            ServerLevel level = DimensionArgument.getDimension(context, "dimension");
                                            BlockPos pos;
                                            float angle;
                                            if (((ILevelMixin) level).getLevelData() instanceof PrimaryLevelData) {
                                                pos = level.getSharedSpawnPos();
                                                angle = level.getSharedSpawnAngle();
                                            }
                                            else {
                                                pos = DimensionController.get(level).spawnPos;
                                                angle = DimensionController.get(level).spawnAngle;
                                            }
                                            teleportTo(context.getSource(), EntityArgument.getPlayers(context, "targets"), level,
                                                    pos.getX() + 0.5d, pos.getY(), pos.getZ() + 0.5d, angle, 0.0f);
                                        return 1;
                                })
                                .then(Commands.argument("location", Vec3Argument.vec3())
                                        .executes((context) -> {
                                            ServerLevel level = DimensionArgument.getDimension(context, "dimension");
                                            Coordinates location = Vec3Argument.getCoordinates(context, "location");
                                            Vec3 pos = location.getPosition(context.getSource());
                                            float angle;
                                            if (((ILevelMixin) level).getLevelData() instanceof PrimaryLevelData) { angle = level.getSharedSpawnAngle(); }
                                            else { angle = DimensionController.get(level).spawnAngle; }
                                            teleportTo(context.getSource(), EntityArgument.getPlayers(context, "targets"), level,
                                                    pos.x, pos.y, pos.z, angle, 0.0f);

                                            return 1;
                                        })
                                        .then(Commands.argument("yaw", AngleArgument.angle())
                                                .executes((context) -> {
                                                    Coordinates location = Vec3Argument.getCoordinates(context, "location");
                                                    Vec3 pos = location.getPosition(context.getSource());
                                                    teleportTo(context.getSource(), EntityArgument.getPlayers(context, "targets"), DimensionArgument.getDimension(context, "dimension"),
                                                            pos.x, pos.y, pos.z, AngleArgument.getAngle(context, "yaw"), 0.0f);
                                                    return 1;
                                                })
                                                .then(Commands.argument("pitch", AngleArgument.angle())
                                                        .executes((context) -> {
                                                            Coordinates location = Vec3Argument.getCoordinates(context, "location");
                                                            Vec3 pos = location.getPosition(context.getSource());
                                                            teleportTo(context.getSource(), EntityArgument.getPlayers(context, "targets"), DimensionArgument.getDimension(context, "dimension"),
                                                                    pos.x, pos.y, pos.z, AngleArgument.getAngle(context, "yaw"), AngleArgument.getAngle(context, "pitch"));
                                                            return 1;
                                                        })
                                                )
                                        )
                                ))
                        )
                )
                .then(Commands.literal("getspawn")
                        .executes((context) -> {
                            context.getSource().sendSuccess(() -> getInfoMessage(context.getSource().getLevel()), false);
                            return 1;
                        })
                        .then(Commands.argument("dimension", DimensionArgument.dimension()).suggests(getLevelSuggests())
                                .executes((context) -> {
                                    ServerLevel level = DimensionArgument.getDimension(context, "dimension");
                                    context.getSource().sendSuccess(() -> getInfoMessage(level), false);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("setspawn") // SetWorldSpawnCommand
                        .executes((context) -> {
                            ServerLevel level = context.getSource().getLevel();
                            Vec3 vec = context.getSource().getPosition();
                            float angle;
                            if (((ILevelMixin) level).getLevelData() instanceof PrimaryLevelData) { angle = level.getSharedSpawnAngle(); }
                            else { angle = DimensionController.get(level).spawnAngle; }
                            if (context.getSource().getPlayer() != null) { angle = context.getSource().getPlayer().getYRot(); }
                            setSpawn(context.getSource(), level, new BlockPos((int) Math.floor(vec.x), (int) Math.floor(vec.y), (int) Math.floor(vec.z)), angle);
                            return 1;
                        })
                        .then(Commands.argument("location", Vec3Argument.vec3())
                                .executes((context) -> {
                                    ServerLevel level = context.getSource().getLevel();
                                    float angle;
                                    if (((ILevelMixin) level).getLevelData() instanceof PrimaryLevelData) { angle = level.getSharedSpawnAngle(); }
                                    else { angle = DimensionController.get(level).spawnAngle; }
                                    if (context.getSource().getPlayer() != null) { angle = context.getSource().getPlayer().getYRot(); }
                                    Coordinates location = Vec3Argument.getCoordinates(context, "location");
                                    Vec3 pos = location.getPosition(context.getSource());
                                    setSpawn(context.getSource(), level, new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z)), angle);
                                    return 1;
                                })
                                .then(Commands.argument("angle", AngleArgument.angle())
                                        .executes((context) -> {
                                            ServerLevel level = context.getSource().getLevel();
                                            Coordinates location = Vec3Argument.getCoordinates(context, "location");
                                            Vec3 pos = location.getPosition(context.getSource());
                                            setSpawn(context.getSource(), level, new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z)), AngleArgument.getAngle(context, "angle"));
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.argument("dimension", DimensionArgument.dimension()).suggests(getLevelSuggests())
                                .executes((context) -> {
                                    ServerLevel level = DimensionArgument.getDimension(context, "dimension");
                                    Vec3 vec = context.getSource().getPosition();
                                    float angle;
                                    if (((ILevelMixin) level).getLevelData() instanceof PrimaryLevelData) { angle = level.getSharedSpawnAngle(); }
                                    else { angle = DimensionController.get(level).spawnAngle; }
                                    setSpawn(context.getSource(), level, new BlockPos((int) Math.floor(vec.x), (int) Math.floor(vec.y), (int) Math.floor(vec.z)), angle);
                                    return 1;
                                })
                                .then(Commands.argument("location", Vec3Argument.vec3())
                                        .executes((context) -> {
                                            ServerLevel level = DimensionArgument.getDimension(context, "dimension");
                                            Coordinates location = Vec3Argument.getCoordinates(context, "location");
                                            Vec3 pos = location.getPosition(context.getSource());
                                            float angle;
                                            if (((ILevelMixin) level).getLevelData() instanceof PrimaryLevelData) { angle = level.getSharedSpawnAngle(); }
                                            else { angle = DimensionController.get(level).spawnAngle; }
                                            if (context.getSource().getPlayer() != null) { angle = context.getSource().getPlayer().getYRot(); }
                                            setSpawn(context.getSource(), level, new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z)), angle);
                                            return 1;
                                        })
                                        .then(Commands.argument("angle", AngleArgument.angle())
                                                .executes((context) -> {
                                                    ServerLevel level = DimensionArgument.getDimension(context, "dimension");
                                                    Coordinates location = Vec3Argument.getCoordinates(context, "location");
                                                    Vec3 pos = location.getPosition(context.getSource());
                                                    setSpawn(context.getSource(), level, new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z)), AngleArgument.getAngle(context, "angle"));
                                                    return 1;
                                                })
                                        )
                                ))
                )
                .then(Commands.literal("list")
                        .executes((context) -> {
                            context.getSource().sendSuccess(() -> Component.translatable("command.world.all"), false);
                            int i = 0;
                            for (ServerLevel level : CustomNpcs.Server.getAllLevels()) {
                                int id = i;
                                context.getSource().sendSuccess(() -> Component.translatable("command.world.level.info",
                                        ((char) 167) + "6" + id, level.dimension().location().toString(),
                                        Component.translatable("command.world.load." + level.isLoaded(BlockPos.ZERO)).getString()), false);
                                i++;
                            }
                            return 1;
                        })
                );
    }

    private static Component getInfoMessage(ServerLevel level) {
        BlockPos pos;
        float angle;
        if (((ILevelMixin) level).getLevelData() instanceof PrimaryLevelData) {
            pos = level.getSharedSpawnPos();
            angle = level.getSharedSpawnAngle();
        }
        else {
            pos = DimensionController.get(level).spawnPos;
            angle = DimensionController.get(level).spawnAngle;
        }
        return Component.empty()
                .append(Component.literal("\"").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(level.dimension().location().toString()).withStyle(ChatFormatting.RESET))
                .append(Component.literal("\" X:").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("" + pos.getX()).withStyle(ChatFormatting.RESET))
                .append(Component.literal(" Y:").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("" + pos.getY()).withStyle(ChatFormatting.RESET))
                .append(Component.literal(" Z:").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("" + pos.getZ()).withStyle(ChatFormatting.RESET))
                .append(Component.literal(" angle:").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("" + angle).withStyle(ChatFormatting.RESET));
    }

    private static void setSpawn(CommandSourceStack source, ServerLevel level, BlockPos pos, float angle) {
        if (((ILevelMixin) level).getLevelData() instanceof PrimaryLevelData) { level.setDefaultSpawnPos(pos, angle); }
        DimensionController.setSpawn(level, pos, angle);
        source.sendSuccess(() -> Component.translatable("command.world.set.spawn", level.dimension().location().toString(), "" + pos.getX(), "" + pos.getY(), "" + pos.getZ(), "" + angle), true);
    }

    private static void teleportTo(CommandSourceStack source, Collection<ServerPlayer> targets, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        for (ServerPlayer target : targets) {
            Util.instance.transferEntity(target, level, x, y, z, yaw, pitch);
            source.sendSuccess(() -> Component.translatable("command.world.tp", target.getName().getString(), level.dimension().location().toString()), false);
        }
    }

    public static SuggestionProvider<CommandSourceStack> getLevelSuggests() {
        return (context, builder) -> {
            for (ResourceKey<Level>  key : CustomNpcs.Server.levelKeys()) {
                builder.suggest(key.location().toString());
            }
            return builder.buildFuture();
        };
    }

}
