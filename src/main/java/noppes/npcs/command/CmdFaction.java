package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerFactionData;

public class CmdFaction {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("faction")
                .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("faction", IntegerArgumentType.integer(0))
                                .suggests(getFactionSuggests())
                                .then(Commands.literal("add")
                                        .then(Commands.argument("points", IntegerArgumentType.integer()).executes((context) -> {
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                            if (!players.isEmpty()) {
                                                Faction faction = null;
                                                String factionValue = "";
                                                try {
                                                    int id = context.getArgument("faction", Integer.TYPE);
                                                    factionValue = "" + id;
                                                    faction = FactionController.instance.getFaction(id);
                                                }
                                                catch (Exception ignored) { }
                                                if (faction == null) {
                                                    try {
                                                        factionValue = context.getArgument("faction", String.class);
                                                        faction = FactionController.instance.getFactionFromName(factionValue);
                                                    }
                                                    catch (Exception ignored) { }
                                                }
                                                if (faction == null) { throw new CommandRuntimeException(Component.literal("Unknown FactionID \"").append(factionValue).append("\"")); }
                                                int points = IntegerArgumentType.getInteger(context, "points");
                                                for (ServerPlayer player : players) {
                                                    PlayerData data = PlayerData.get(player);
                                                    PlayerFactionData playerfactiondata = data.factionData;
                                                    playerfactiondata.increasePoints(player, faction.id, points);
                                                    data.save(true);
                                                }
                                            }
                                            return 1;
                                        })))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("points", IntegerArgumentType.integer()).executes((context) -> {
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                            if (!players.isEmpty()) {
                                                Faction faction = FactionController.instance.factions.get(IntegerArgumentType.getInteger(context, "faction"));
                                                if (faction == null) {
                                                    throw new CommandRuntimeException(Component.literal("Unknown FactionID"));
                                                }
                                                int points = IntegerArgumentType.getInteger(context, "points");
                                                for (ServerPlayer player : players) {
                                                    PlayerData data = PlayerData.get(player);
                                                    PlayerFactionData playerfactiondata = data.factionData;
                                                    playerfactiondata.factionData.put(faction.id, points);
                                                    data.save(true);
                                                }
                                            }
                                            return 1;
                                        })))
                                .then(Commands.literal("reset").executes((context) -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                    if (!players.isEmpty()) {
                                        Faction faction = FactionController.instance.factions.get(IntegerArgumentType.getInteger(context, "faction"));
                                        if (faction == null) {
                                            throw new CommandRuntimeException(Component.literal("Unknown FactionID"));
                                        }
                                        for (ServerPlayer player : players) {
                                            PlayerData data = PlayerData.get(player);
                                            data.factionData.factionData.put(faction.id, faction.defaultPoints);
                                            data.save(true);
                                        }
                                    }
                                    return 1;
                                }))
                                .then(Commands.literal("drop").executes((context) -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                    if (!players.isEmpty()) {
                                        Faction faction = FactionController.instance.factions.get(IntegerArgumentType.getInteger(context, "faction"));
                                        if (faction == null) {
                                            throw new CommandRuntimeException(Component.literal("Unknown FactionID"));
                                        }
                                        for (ServerPlayer player : players) {
                                            PlayerData data = PlayerData.get(player);
                                            data.factionData.factionData.remove(faction.id);
                                            data.save(true);
                                        }
                                    }
                                    return 1;
                                }))));
    }

    private static SuggestionProvider<CommandSourceStack> getFactionSuggests() {
        return (context, builder) -> {
            for (Faction faction : FactionController.instance.factions.values()) {
                builder.suggest("" + faction.id);
                builder.suggest(faction.getName());
            }
            return builder.buildFuture();
        };
    }

}
