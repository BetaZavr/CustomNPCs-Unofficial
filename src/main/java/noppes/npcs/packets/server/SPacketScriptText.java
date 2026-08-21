package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPacketScriptText extends PacketServerBasic {

    protected static int channelId;
    protected static final Map<Integer, String[]> data = new HashMap<>();
    public static Map<Integer, IScriptHandler> handlers = new HashMap<>();

    private final int type;
    private final int tab;
    private final int id;
    private final int maxIDs;
    private final String part;

    public SPacketScriptText(int typeIn, int tabIn, int idIn, int maxIDsIn, String partIn) {
        type = typeIn;
        tab = tabIn;
        id = idIn;
        maxIDs = maxIDsIn;
        part = partIn;
    }

    @Override
    public boolean toolAllowed(ItemStack item){ return true; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.TOOL_SCRIPTER); }

    public static void encode(SPacketScriptText msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.type);
        buf.writeInt(msg.tab);
        buf.writeInt(msg.id);
        buf.writeInt(msg.maxIDs);
        buf.writeUtf(msg.part);
    }

    public static SPacketScriptText decode(FriendlyByteBuf buf) {
        return new SPacketScriptText(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf());
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (handlers.containsKey(type)) {
            if (type == 6 && !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.EDIT_CLIENT_SCRIPT)) {
                warn(CustomNpcsPermissions.EDIT_CLIENT_SCRIPT.getNodeName());
            }
            else {
                IScriptHandler handler = handlers.get(type);
                if (!data.containsKey(tab)) { data.put(tab, new String[maxIDs]); }
                data.get(tab)[id] = part;
                boolean done = true;
                StringBuilder total = new StringBuilder();
                for (String str : data.get(tab)) {
                    if (str == null) {
                        done = false;
                        break;
                    }
                    total.append(str);
                }
                if (done) {
                    ScriptContainer container = handler.getScripts().get(tab);
                    if (container != null) {
                        container.script = total.toString();
                        handler.init();
                    }
                    data.remove(tab);
                }
            }
        }
        CustomNpcs.debugData.end("Packets");
    }

}