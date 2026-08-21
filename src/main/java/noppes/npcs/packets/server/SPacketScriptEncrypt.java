package noppes.npcs.packets.server;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.util.ScriptEncryption;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class SPacketScriptEncrypt extends PacketServerBasic {

    protected static int channelId;
    private final int type;
    private final CompoundTag compound;

    public SPacketScriptEncrypt(int typeIn, CompoundTag compoundIn) {
        type = typeIn;
        compound = compoundIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.TOOL_SCRIPTER); }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketScriptEncrypt msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.type);
        buf.writeNbt(msg.compound);
    }

    public static SPacketScriptEncrypt decode(FriendlyByteBuf buf) { return new SPacketScriptEncrypt(buf.readInt(), buf.readAnySizeNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (SPacketScriptText.handlers.containsKey(type)) {
            if (type == 6) { warn("You can't crypt scripts for the client!"); }
            else {
                IScriptHandler handler = SPacketScriptText.handlers.get(type);
                // file
                boolean error = false;
                File dir = CustomNpcs.getLevelSaveDirectory("scripts");
                if (dir != null) {
                    File file = new File(dir, compound.getString("Path"));
                    // data
                    String handlerType = "";
                    if (handler != null) {
                        handlerType = handler.getClass().getSimpleName();
                        ScriptContainer container = handler.getScripts().get(compound.getInt("Tab"));
                        error = container == null;
                        if (!error) {
                            boolean onlyTab = compound.getBoolean("OnlyTab");
                            String code = "";
                            if (onlyTab) { code = container.script; } else {
                                try {
                                    Method getTotalCode = container.getClass().getDeclaredMethod("getFullCode");
                                    getTotalCode.trySetAccessible();
                                    code = (String) getTotalCode.invoke(container);
                                }
                                catch (Exception e) { error = true; }
                            }
                            if (!error) { error = !ScriptEncryption.encryptScript(file, compound.getString("Name"), code, onlyTab, container, handler); }
                        }
                    }
                    player.sendSystemMessage(Component.empty()
                            .append(Component.literal("CustomNPCs").withStyle(ChatFormatting.DARK_GREEN))
                            .append(Component.literal((error ? ": Error encrypt" : ": Encrypt") + " script to file \"")
                                    .withStyle(error ? ChatFormatting.RED : ChatFormatting.GRAY))
                            .append(Component.literal(file.getAbsolutePath()).withStyle(ChatFormatting.RESET))
                            .append(Component.literal("\" for ").withStyle(error ? ChatFormatting.RED : ChatFormatting.GRAY))
                            .append(Component.literal(handlerType).withStyle(ChatFormatting.RESET))
                    );
                }
            }
        }
        CustomNpcs.debugData.end("Packets");
    }

}