package noppes.npcs.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CmdMoney extends CommandBase {

    @Override
    public @Nonnull String getName() { return "moneynpc"; }

    @Override
    public @Nonnull String getUsage(@Nonnull ICommandSender sender) { return "Use as /" + getName() + " subcommands"; }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
        if (args.length < 2) { throw new CommandException(getUsage(sender)); }
        switch (args[0]) {
            case "get": {
                switch (args[1]) {
                    case "money": {
                        String name = (args.length > 2 && !args[2].isEmpty()) ? args[2] : null;
                        PlayerData data = getData(server, sender, name, CustomNpcsPermissions.MONEY_MANAGER);
                        if (name == null || sender.getName().equalsIgnoreCase(name)) {
                            sender.sendMessage(Component.translatable("command.money.get.money", data.game.getMoney(), CustomNpcs.displayCurrencies).getParent());
                        } else {
                            sender.sendMessage(Component.translatable("command.money.get.money.player", name, data.game.getMoney(), CustomNpcs.displayCurrencies).getParent());
                        }
                        break;
                    }
                    case "donat": {
                        String name = (args.length > 2 && !args[2].isEmpty()) ? args[2] : null;
                        PlayerData data = getData(server, sender, name, CustomNpcsPermissions.DONAT_MANAGER);
                        if (name == null || sender.getName().equalsIgnoreCase(name)) {
                            sender.sendMessage(Component.translatable("command.money.get.donat", data.game.getMoney(), CustomNpcs.displayCurrencies).getParent());
                        } else {
                            sender.sendMessage(Component.translatable("command.money.get.donat.player", name, data.game.getMoney(), CustomNpcs.displayCurrencies).getParent());
                        }
                        break;
                    }
                    default: throw new CommandException("Use as /" + getName() + " get <money|donat> <player>");
                }
                break;
            }
            case "set": {
                String error = "Use as /" + getName() + " set <money|donat> (<player|value> or <player> <value>)";
                if (args.length < 3) { throw new CommandException(error); }
                int value;
                try { value = ValueUtil.correctInt(Integer.parseInt(args.length == 4 ? args[3] : args[2]), 0, Integer.MAX_VALUE); }
                catch (NumberFormatException ex) { throw new CommandException((args.length == 4 ? args[3] : args[2]) + " must be an integer"); }
                switch (args[1]) {
                    case "money": {
                        String name = (args.length == 4 && !args[2].isEmpty()) ? args[2] : sender.getName();
                        PlayerData data = getData(server, sender, name, CustomNpcsPermissions.MONEY_MANAGER);
                        data.game.setMoney(value);
                        EntityPlayerMP player = null;
                        if (args.length == 4) { player = server.getPlayerList().getPlayerByUsername(name); }
                        if (player != null) {
                            if (player.getName().equals(sender.getName())) {
                                sender.sendMessage(Component.translatable("command.money.set.money", value, CustomNpcs.displayCurrencies).getParent());
                            }
                            else {
                                sender.sendMessage(Component.translatable("command.money.set.money.player", name, value, CustomNpcs.displayCurrencies).getParent());
                                Component message = Component.translatable("command.money.set.money", value, CustomNpcs.displayCurrencies);
                                if (sender instanceof EntityPlayerMP) {
                                    message.append(Component.literal(" <Admin: " + sender.getName() + ">").withStyle(TextFormatting.DARK_GRAY));
                                }
                                player.sendMessage(message.getParent());
                            }
                        } else {
                            sender.sendMessage(Component.translatable("command.money.set.money.player", name, value, CustomNpcs.displayCurrencies).getParent());
                        }
                        if (data.player == null) { data.save(false); }
                        break;
                    }
                    case "donat": {
                        String name = (args.length == 4 && !args[2].isEmpty()) ? args[2] : sender.getName();
                        PlayerData data = getData(server, sender, name, CustomNpcsPermissions.DONAT_MANAGER);
                        data.game.setDonat(value);
                        EntityPlayerMP player = null;
                        if (args.length == 4) { player = server.getPlayerList().getPlayerByUsername(name); }
                        if (player != null) {
                            if (player.getName().equals(sender.getName())) {
                                sender.sendMessage(Component.translatable("command.money.set.donat", value, CustomNpcs.displayCurrencies).getParent());
                            }
                            else {
                                sender.sendMessage(Component.translatable("command.money.set.donat.player", name, value, CustomNpcs.displayCurrencies).getParent());
                                Component message = Component.translatable("command.money.set.donat", value, CustomNpcs.displayCurrencies);
                                if (sender instanceof EntityPlayerMP) {
                                    message.append(Component.literal(" <Admin: " + sender.getName() + ">").withStyle(TextFormatting.DARK_GRAY));
                                }
                                player.sendMessage(message.getParent());
                            }
                        }
                        else { sender.sendMessage(Component.translatable("command.money.set.donat.player", name, value, CustomNpcs.displayCurrencies).getParent()); }
                        if (data.player == null) { data.save(false); }
                        break;
                    }
                    default: throw new CommandException(error);
                }
                break;
            }
            case "add": {
                String error = "Use as /" + getName() + " add <money|donat> (<player|value> or <player> <value>)";
                if (args.length < 3) { throw new CommandException(error); }
                int value;
                try { value = ValueUtil.correctInt(Integer.parseInt(args.length == 4 ? args[3] : args[2]), 0, Integer.MAX_VALUE); }
                catch (NumberFormatException ex) { throw new CommandException((args.length == 4 ? args[3] : args[2]) + " must be an integer"); }
                switch (args[1]) {
                    case "money": {
                        String name = (args.length == 4 && !args[2].isEmpty()) ? args[2] : sender.getName();
                        PlayerData data = getData(server, sender, name, CustomNpcsPermissions.MONEY_MANAGER);
                        data.game.addMoney(value);
                        EntityPlayerMP player = null;
                        if (args.length == 4) { player = server.getPlayerList().getPlayerByUsername(name); }
                        if (player != null) {
                            if (player.getName().equals(sender.getName())) {
                                sender.sendMessage(Component.translatable("command.money.add.money", value, CustomNpcs.displayCurrencies).getParent());
                            }
                            else {
                                sender.sendMessage(Component.translatable("command.money.add.money.player", name, value, CustomNpcs.displayCurrencies).getParent());
                                Component message = Component.translatable("command.money.add.money", value, CustomNpcs.displayCurrencies);
                                if (sender instanceof EntityPlayerMP) {
                                    message.append(Component.literal(" <Admin: " + sender.getName() + ">").withStyle(TextFormatting.DARK_GRAY));
                                }
                                player.sendMessage(message.getParent());
                            }
                        } else {
                            sender.sendMessage(Component.translatable("command.money.add.money.player", name, value, CustomNpcs.displayCurrencies).getParent());
                        }
                        if (data.player == null) { data.save(false); }
                        break;
                    }
                    case "donat": {
                        String name = (args.length == 4 && !args[2].isEmpty()) ? args[2] : sender.getName();
                        PlayerData data = getData(server, sender, name, CustomNpcsPermissions.DONAT_MANAGER);
                        data.game.addDonat(value);
                        EntityPlayerMP player = null;
                        if (args.length == 4) { player = server.getPlayerList().getPlayerByUsername(name); }
                        if (player != null) {
                            if (player.getName().equals(sender.getName())) {
                                sender.sendMessage(Component.translatable("command.money.add.donat", value, CustomNpcs.displayCurrencies).getParent());
                            }
                            else {
                                sender.sendMessage(Component.translatable("command.money.add.donat.player", name, value, CustomNpcs.displayCurrencies).getParent());
                                Component message = Component.translatable("command.money.add.donat", value, CustomNpcs.displayCurrencies);
                                if (sender instanceof EntityPlayerMP) {
                                    message.append(Component.literal(" <Admin: " + sender.getName() + ">").withStyle(TextFormatting.DARK_GRAY));
                                }
                                player.sendMessage(message.getParent());
                            }
                        }
                        else { sender.sendMessage(Component.translatable("command.money.add.donat.player", name, value, CustomNpcs.displayCurrencies).getParent()); }
                        if (data.player == null) { data.save(false); }
                        break;
                    }
                    default: throw new CommandException(error);
                }
                break;
            }
            case "pay": {
                if (sender instanceof EntityPlayerMP) {
                    if (args.length < 3 || args[2].isEmpty()) { throw new CommandException("Use as /" + getName() + " pay <player> <value>)"); }
                    EntityPlayerMP senderPlayer = (EntityPlayerMP) sender;
                    if (senderPlayer.getName().equalsIgnoreCase(args[1])) { throw new CommandException("argument.entity.selector.not_allowed"); }
                    PlayerData data = getData(server, sender, args[1], null);
                    int value;
                    try { value = ValueUtil.correctInt(Integer.parseInt(args[2]), 0, Integer.MAX_VALUE); }
                    catch (NumberFormatException ex) { throw new CommandException(args[2] + " must be an integer"); }
                    if (!senderPlayer.isCreative()) {
                        float commission = ValueUtil.correctFloat(CustomNpcs.CoinCommission, 0.0f, Integer.MAX_VALUE);
                        int total = value + (int) Math.max(((float) value * commission / 100.0f), 1.0f);
                        PlayerData senderData = PlayerData.get(senderPlayer);
                        if (senderData.game.getMoney() < total) {
                            throw new CommandException(Component.translatable("command.money.not.balance."+(commission > 0.0f ? "1" : "0"),
                                            TextFormatting.WHITE + "" + commission, TextFormatting.WHITE + "" + total)
                                    .append(" ")
                                    .append(Component.translatable("command.money.get.money",
                                            TextFormatting.WHITE + "" + senderData.game.getMoney(), TextFormatting.GRAY + CustomNpcs.displayCurrencies))
                                    .getFormattedText());
                        }
                        senderData.game.addMoney(-total);
                    }
                    data.game.addMoney(value);
                    if (data.player == null) { data.save(false); }
                }
                else { throw new CommandException("Command for" + " players only"); }
                break;
            }
        }
    }

    private PlayerData getData(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender,
                               @Nullable String player, @Nullable CustomNpcsPermissions.Permission permission) throws CommandException {
        if (player != null && permission != null && (!(sender instanceof EntityPlayerMP) ||
                !CustomNpcsPermissions.hasPermission((EntityPlayerMP) sender, CustomNpcsPermissions.MONEY_MANAGER)) ||
                !CustomNpcsPermissions.hasPermission((EntityPlayerMP) sender, permission)) {
            throw new CommandException("availability.permission");
        }
        if (player == null) {
            if (sender instanceof EntityPlayerMP && permission != null) { player = sender.getName(); }
            else { throw new PlayerNotFoundException("commands.generic.player.notFound", "NULL"); }
        }
        PlayerData data = PlayerDataController.instance.getDataFromUsername(server, player);
        if (data == null) { throw new PlayerNotFoundException("commands.generic.player.notFound", player); }
        return data;
    }

    @Override
    public @Nonnull List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args, BlockPos pos) {
        List<String> list = new ArrayList<>();
        int per = CommandNoppes.getPermissionLevel(server, sender);
        if (args.length == 1) {
            list.add("get");
            list.add("pay");
            if (per > 4) {
                list.add("set");
                list.add("add");
            }
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("get") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add")) {
                list.add("money");
                list.add("donat");
            }
            else if (args[0].equalsIgnoreCase("pay")) {
                list.addAll(PlayerDataController.instance.getPlayerNames());
            }
        }
        else if (args.length == 3) {
            if ((sender instanceof EntityPlayerMP) &&
                    (args[0].equalsIgnoreCase("get") || args[0].equalsIgnoreCase("set") ||
                            args[0].equalsIgnoreCase("add"))) {
                CustomNpcsPermissions.Permission permission;
                if (args[1].equalsIgnoreCase("money")) { permission = CustomNpcsPermissions.MONEY_MANAGER; }
                else { permission = CustomNpcsPermissions.DONAT_MANAGER; }
                if (CustomNpcsPermissions.hasPermission((EntityPlayerMP) sender, permission)) {
                    list.addAll(PlayerDataController.instance.getPlayerNames());
                }
            }
        }
        return list;
    }

}
