package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import java.util.Collection;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.packets.client.PacketChat;
import noppes.npcs.packets.client.PacketSync;

import javax.annotation.Nonnull;

public class CmdQuest {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("quest");
        command.then(Commands.literal("start")
                .then(Commands.argument("players", EntityArgument.players())
                        .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                        .then(Commands.argument("quest", IntegerArgumentType.integer(0))
                                .suggests(getQuestSuggests())
                                .executes((context) -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                    if (!players.isEmpty()) {
                                        Quest quest = getQuest(context);
                                        for (ServerPlayer player : players) {
                                            PlayerData data = PlayerData.get(player);
                                            QuestData questdata = new QuestData(quest);
                                            data.questData.activeQuests.put(quest.id, questdata);
                                            data.save(true);
                                            Packets.send(player, new PacketAchievement(Component.translatable("quest.newquest"), Component.translatable(quest.title), 2, new CompoundTag()));
                                            Packets.send(player, new PacketChat(Component.translatable("quest.newquest").append(":").append(Component.translatable(quest.title))));
                                        }

                                    }
                                    return 1;
                                }))));
        command.then(Commands.literal("finish")
                .then(Commands.argument("players", EntityArgument.players()).requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                        .then(Commands.argument("quest", IntegerArgumentType.integer(0))
                                .suggests(getQuestSuggests())
                                .executes((context) -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                    if (!players.isEmpty()) {
                                        Quest quest = getQuest(context);
                                        for (ServerPlayer player : players) {
                                            PlayerData data = PlayerData.get(player);
                                            data.questData.finish(quest, player);
                                            data.save(true);
                                        }
                                    }
                                    return 1;
                                }))));
        command.then(Commands.literal("stop")
                .then(Commands.argument("players", EntityArgument.players()).requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                        .then(Commands.argument("quest", IntegerArgumentType.integer(0))
                                .suggests(getQuestSuggests())
                                .executes((context) -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                    if (!players.isEmpty()) {
                                        Quest quest = getQuest(context);
                                        for (ServerPlayer player : players) {
                                            PlayerData data = PlayerData.get(player);
                                            data.questData.activeQuests.remove(quest.id);
                                            data.save(true);
                                        }
                                        return 1;
                                    }
                                    return 1;
                                }))));
        command.then(Commands.literal("remove")
                .then(Commands.argument("players", EntityArgument.players()).requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                        .then(Commands.argument("quest", IntegerArgumentType.integer(0))
                                .suggests(getQuestSuggests())
                                .executes((context) -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                    if (!players.isEmpty()) {
                                        Quest quest = getQuest(context);
                                        for (ServerPlayer player : players) {
                                            PlayerData data = PlayerData.get(player);
                                            data.questData.activeQuests.remove(quest.id);
                                            data.questData.removeFinishedQuest(quest.id);
                                            data.save(true);
                                        }
                                    }
                                    return 1;
                                }))));
        command.then(Commands.literal("objective")
                .then(Commands.argument("players", EntityArgument.players()).requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                        .then(Commands.argument("quest", IntegerArgumentType.integer(0))
                                .suggests(getQuestSuggests())
                                .executes((context) -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                    if (!players.isEmpty()) {
                                        Quest quest = getQuest(context);
                                        for (ServerPlayer player : players) {
                                            PlayerData data = PlayerData.get(player);
                                            if (data.questData.activeQuests.containsKey(quest.id)) {
                                                for (IQuestObjective ob : quest.questInterface.getObjectives(player)) {
                                                    player.sendSystemMessage(ob.getMCText());
                                                }
                                            }
                                        }
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("objective", IntegerArgumentType.integer(0, 3))
                                        .executes((context) -> {
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                            if (!players.isEmpty()) {
                                                Quest quest = getQuest(context);
                                                int objective = IntegerArgumentType.getInteger(context, "objective");
                                                for (ServerPlayer player : players) {
                                                    PlayerData data = PlayerData.get(player);
                                                    if (data.questData.activeQuests.containsKey(quest.id)) {
                                                        IQuestObjective[] objectives = quest.questInterface.getObjectives(player);
                                                        if (objective < objectives.length) {
                                                            player.sendSystemMessage(objectives[objective].getMCText());
                                                        }
                                                    }
                                                }
                                            }
                                            return 1;
                                        })
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes((context) -> {
                                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                                                    if (!players.isEmpty()) {
                                                        Quest quest = getQuest(context);
                                                        int objective = IntegerArgumentType.getInteger(context, "objective");
                                                        int value = IntegerArgumentType.getInteger(context, "value");
                                                        for (ServerPlayer player : players) {
                                                            PlayerData data = PlayerData.get(player);
                                                            if (data.questData.activeQuests.containsKey(quest.id)) {
                                                                IQuestObjective[] objectives = quest.questInterface.getObjectives(player);
                                                                if (objective < objectives.length) {
                                                                    objectives[objective].setProgress(value);
                                                                }
                                                            }
                                                        }
                                                    }
                                                    return 1;
                                                }))))));
        command.requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2)).then(Commands.literal("reload").executes((context) -> {
            (new QuestController()).load();
            for (QuestCategory category : QuestController.instance.categories.values()) {
                Packets.sendAll(new PacketSync(3, category.save(new CompoundTag()), false));
            }
            Packets.sendAll(new PacketSync(3, new CompoundTag(), true));
            return 1;
        }));
        return command;
    }

    private static @Nonnull Quest getQuest(CommandContext<CommandSourceStack> context) throws CommandRuntimeException {
        String questValue = "";
        Quest quest = null;
        try {
            int id = context.getArgument("quest", Integer.TYPE);
            questValue = "" + id;
            quest = QuestController.instance.get(id);
        }
        catch (Exception ignored) { }
        if (quest == null) {
            try {
                questValue = context.getArgument("quest", String.class);
                quest = QuestController.instance.getQuestFromName(questValue);
            }
            catch (Exception ignored) { }
        }
        if (quest == null) { throw new CommandRuntimeException(Component.literal("Unknown QuestID \"").append(questValue).append("\"")); }
        return quest;
    }

    private static SuggestionProvider<CommandSourceStack> getQuestSuggests() {
        return (context, builder) -> {
            for (Quest quest : QuestController.instance.quests.values()) {
                builder.suggest("" + quest.id);
                builder.suggest(quest.getName());
            }
            return builder.buildFuture();
        };
    }

}
