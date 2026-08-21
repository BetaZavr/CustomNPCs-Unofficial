package noppes.npcs.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumGuiType;

public class CmdPermissions {

    public static final SimpleCommandExceptionType NO_PERMISSION = new SimpleCommandExceptionType(Component.translatable("availability.permission"));

    public static ArgumentBuilder<CommandSourceStack,?> register() {
        return Commands.literal("permissions")
                .requires((source) -> source.hasPermission(CustomNpcs.NoppesCommandOpOnly ? 4 : 2))
                .executes((context) -> {
                    if (!CustomNpcsPermissions.hasPermission(context.getSource().getPlayer(), CustomNpcsPermissions.EDIT_PERMISSION)) { throw NO_PERMISSION.create(); }
                    if (context.getSource().getPlayer() != null) {
                        NoppesUtilServer.sendOpenGui(context.getSource().getPlayer(), EnumGuiType.PermissionsEdit, null);
                    }
                    return 1;
                });
    }

}
