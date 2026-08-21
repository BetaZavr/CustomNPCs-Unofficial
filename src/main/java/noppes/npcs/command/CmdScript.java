package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.ForgeEventHandler;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.event.WorldEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketEventNames;

import java.util.*;

public class CmdScript {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        NpcAPI api = NpcAPI.Instance();
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("script");
        command.then(Commands.literal("reload")
                .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .executes((context) -> {
                    ScriptController.Instance.loadCategories();
                    // Players
                    if (ScriptController.Instance.loadPlayerScripts()) { context.getSource().sendSuccess(() -> Component.literal("Reload player scripts successfully"), false); }
                    else { context.getSource().sendSuccess(() -> Component.literal("Failed reloading player scripts"), false); }
                    // NPCs
                    if (ScriptController.Instance.loadNPCsScripts()) { context.getSource().sendSuccess(() -> Component.literal("Reload NPCs scripts successfully"), false); }
                    else { context.getSource().sendSuccess(() -> Component.literal("Failed reloading NPCs scripts"), false); }
                    // Forge
                    if (ScriptController.Instance.loadForgeScripts()) { context.getSource().sendSuccess(() -> Component.literal("Reload forge scripts successfully"), false); }
                    else { context.getSource().sendSuccess(() -> Component.literal("Failed reloading forge scripts"), false); }
                    // Clients
                    if (ScriptController.Instance.loadClientScripts()) { context.getSource().sendSuccess(() -> Component.literal("Reload client scripts successfully"), false); }
                    else { context.getSource().sendSuccess(() -> Component.literal("Failed reloading client scripts"), false); }
                    // Potions
                    if (ScriptController.Instance.loadPotionScripts()) { context.getSource().sendSuccess(() -> Component.literal("Reload potion scripts successfully"), false); }
                    else { context.getSource().sendSuccess(() -> Component.literal("Failed reloading potion scripts"), false); }
                    // Constants data
                    if (ScriptController.Instance.loadConstantData()) { context.getSource().sendSuccess(() -> Component.literal("Reload constant data successfully"), false); }
                    else { context.getSource().sendSuccess(() -> Component.literal("Failed reloading constant data"), false); }
                    // Stored data
                    if (ScriptController.Instance.loadStoredData()) { context.getSource().sendSuccess(() -> Component.literal("Reload stored data successfully"), false); }
                    else { context.getSource().sendSuccess(() -> Component.literal("Failed reloading stored data"), false); }
                    // Client data
                    for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) { ScriptController.Instance.sendClientTo(player); }
                    return 1;
                }));
        command.then(Commands.literal("trigger")
                .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                        .executes((context) -> {
                            if (api != null) {
                                IWorld level = api.getIWorld(context.getSource().getLevel());
                                Vec3 bpos = context.getSource().getPosition();
                                IPos pos = api.getIPos(bpos.x, bpos.y, bpos.z);
                                int id = IntegerArgumentType.getInteger(context, "id");
                                IEntity<?> e = api.getIEntity(context.getSource().getEntity());
                                EventHooks.onScriptTriggerEvent(id, level, pos, e, new String[0]);
                            }
                            return 1;
                        })
                )
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes((context) -> {
                            if (api != null) {
                                IWorld level = api.getIWorld(context.getSource().getLevel());
                                Vec3 bpos = context.getSource().getPosition();
                                IPos pos = api.getIPos(bpos.x, bpos.y, bpos.z);
                                IEntity<?> e = api.getIEntity(context.getSource().getEntity());
                                int id = IntegerArgumentType.getInteger(context, "id");
                                EventHooks.onScriptTriggerEvent(id, level, pos, e, StringArgumentType.getString(context, "args").split(" "));
                            }
                            return 1;
                        })
                )
        );
        command.then(Commands.literal("apilist").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .executes((context) -> {
                    MutableComponent message = Component.empty();
                    List<String> g = new ArrayList<>();
                    for (EnumScriptType est : EnumScriptType.values()) { g.add(est.function); }
                    Collections.sort(g);
                    boolean start = true;
                    for (String name : g) {
                        if (start) {
                            message.append(Component.literal("Mod APIs event names:\n").withStyle(ChatFormatting.GOLD));
                            start = false;
                        }
                        else { message.append(Component.literal(", ").withStyle(ChatFormatting.GOLD)); }
                        message.append(Component.literal(name).withStyle(ChatFormatting.RESET));
                    }
                    message.append(Component.literal(";\n").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal("Total Size: ").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal("" + g.size()).withStyle(ChatFormatting.YELLOW));
                    context.getSource().sendSuccess(() -> message, false);
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player != null) {
                        Map<String, String> names = new HashMap<>();
                        for (EnumScriptType est : EnumScriptType.values()) { names.put(est.function, ""); }
                        Packets.send(player, new PacketEventNames(names, (byte) 2));
                    }
                    return 1;
                })
                .then(Commands.argument("name", IntegerArgumentType.integer(0))
                        .suggests(getEventSuggests(0))
                        .executes((context) -> 2)
                ));
        command.then(Commands.literal("clientlist")
                .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .executes((context) -> {
                    MutableComponent message = Component.empty();
                    List<String> g = new ArrayList<>(ForgeEventHandler.clientEventNames.values());
                    Collections.sort(g);
                    boolean start = true;
                    for (String name : g) {
                        if (start) {
                            message.append(Component.literal("Client forge event names:\n").withStyle(ChatFormatting.GOLD));
                            start = false;
                        }
                        else { message.append(Component.literal(", ").withStyle(ChatFormatting.GOLD)); }
                        message.append(Component.literal(name).withStyle(ChatFormatting.RESET));
                    }
                    message.append(Component.literal(";\n").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal("Total Size: ").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal("" + g.size()).withStyle(ChatFormatting.YELLOW));
                    context.getSource().sendSuccess(() -> message, false);
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player != null) {
                        Map<String, String> names = new HashMap<>();
                        for (Map.Entry<Class<?>, String> entry : ForgeEventHandler.clientEventNames.entrySet()) {
                            names.put(entry.getKey().getName(), entry.getValue());
                        }
                        Packets.send(player, new PacketEventNames(names, (byte) 0));
                    }
                    return 1;
                })
                .then(Commands.argument("name", IntegerArgumentType.integer(0))
                        .suggests(getEventSuggests(0))
                        .executes((context) -> 1)
                ));
        command.then(Commands.literal("forgelist").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .executes((context) -> {
                    MutableComponent message = Component.empty();
                    List<String> g = new ArrayList<>(ForgeEventHandler.eventNames.values());
                    Collections.sort(g);
                    boolean start = true;
                    for (String name : g) {
                        if (start) {
                            message.append(Component.literal("Server forge event names:\n").withStyle(ChatFormatting.GOLD));
                            start = false;
                        }
                        else { message.append(Component.literal(", ").withStyle(ChatFormatting.GOLD)); }
                        message.append(Component.literal(name).withStyle(ChatFormatting.RESET));
                    }
                    message.append(Component.literal(";\n").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal("Total Size: ").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal("" + g.size()).withStyle(ChatFormatting.YELLOW));
                    context.getSource().sendSuccess(() -> message, false);
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player != null) {
                        Map<String, String> names = new HashMap<>();
                        for (Map.Entry<Class<?>, String> entry : ForgeEventHandler.eventNames.entrySet()) {
                            names.put(entry.getKey().getName(), entry.getValue());
                        }
                        Packets.send(player, new PacketEventNames(names, (byte) 1));
                    }
                    return 1;
                })
                .then(Commands.argument("name", IntegerArgumentType.integer(0))
                        .suggests(getEventSuggests(1))
                        .executes((context) -> 1)
                ));
        command.then(Commands.literal("logs").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .executes((context) -> {
                    List<Component> list = new ArrayList<>();
                    for (ScriptContainer container : ScriptController.Instance.getErrored()) {
                        Component message = container.noticeString();
                        list.add(message);
                    }
                    if (list.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.translatable("command.script.logs.empty"), false);
                    } else {
                        context.getSource().sendSuccess(() -> Component.translatable("command.script.logs.info"), false);
                        for (Component message : list) { context.getSource().sendSuccess(() -> message, false); }
                    }
                    context.getSource().sendSuccess(() -> Component.translatable("command.script.logs.end"), false);
                    return 1;
                }));
        command.then(Commands.literal("run").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .executes((context) -> {
                    IWorld world = Objects.requireNonNull(NpcAPI.Instance()).getIWorld(context.getSource().getLevel());
                    Vec3 bpos = context.getSource().getPosition();
                    IPos pos = Objects.requireNonNull(NpcAPI.Instance()).getIPos(bpos.x, bpos.y, bpos.z);
                    String args = StringArgumentType.getString(context, "remainingArgs");
                    WorldEvent.ScriptCommandEvent event = new WorldEvent.ScriptCommandEvent(world, pos, args.split(" "));
                    EventHooks.onWorldScriptEvent(event);
                    return 1;
                }));
        command.then(Commands.literal("list").requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .then(Commands.literal("blocks")
                        .executes((context) -> {
                            Component message = ScriptController.Instance.getElements(0);
                            if (message != null) {
                                context.getSource().sendSuccess(() -> Component.translatable("script.command.blocks"), false);
                                context.getSource().sendSuccess(() -> message, false);
                            }
                            else { context.getSource().sendSuccess(() -> Component.translatable("script.command.not.found"), false); }
                            return 1;
                        })
                )
                .then(Commands.literal("doors")
                        .executes((context) -> {
                            Component message = ScriptController.Instance.getElements(1);
                            if (message != null) {
                                context.getSource().sendSuccess(() -> Component.translatable("script.command.doors"), false);
                                context.getSource().sendSuccess(() -> message, false);
                            }
                            else { context.getSource().sendSuccess(() -> Component.translatable("script.command.not.found"), false); }
                            return 1;
                        })
                )
                .then(Commands.literal("npcs")
                        .executes((context) -> {
                            Component message = ScriptController.Instance.getElements(2);
                            if (message != null) {
                                context.getSource().sendSuccess(() -> Component.translatable("script.command.npcs"), false);
                                context.getSource().sendSuccess(() -> message, false);
                            }
                            else { context.getSource().sendSuccess(() -> Component.translatable("script.command.not.found"), false); }
                            return 1;
                        })
                )
                .then(Commands.literal("all")
                        .executes((context) -> {
                            boolean bo = true;
                            Component blocks = ScriptController.Instance.getElements(0);
                            if (blocks != null) {
                                context.getSource().sendSuccess(() -> Component.translatable("script.command.blocks"), false);
                                context.getSource().sendSuccess(() -> blocks, false);
                                bo = false;
                            }
                            Component doors = ScriptController.Instance.getElements(1);
                            if (doors != null) {
                                context.getSource().sendSuccess(() -> Component.translatable("script.command.doors"), false);
                                context.getSource().sendSuccess(() -> doors, false);
                                bo = false;
                            }
                            Component npcs = ScriptController.Instance.getElements(2);
                            if (npcs != null) {
                                context.getSource().sendSuccess(() -> Component.translatable("script.command.npcs"), false);
                                context.getSource().sendSuccess(() -> npcs, false);
                                bo = false;
                            }
                            if (bo) { context.getSource().sendSuccess(() -> Component.translatable("script.command.not.found"), false); }
                            return 1;
                        })
                )
        );
        return command;
    }

    private static SuggestionProvider<CommandSourceStack> getEventSuggests(int type) {
        return (context, builder) -> {
            switch (type) {
                case 0: {
                    for (String name : ForgeEventHandler.clientEventNames.values()) { builder.suggest(name); }
                    break;
                }
                case 1: {
                    for (String name : ForgeEventHandler.eventNames.values()) { builder.suggest(name); }
                    break;
                }
                default: {
                    for (EnumScriptType est : EnumScriptType.values()) { builder.suggest(est.function); }
                    break;
                }
            }
            return builder.buildFuture();
        };
    }

}
