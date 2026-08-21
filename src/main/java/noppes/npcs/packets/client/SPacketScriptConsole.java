package noppes.npcs.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.packets.server.SPacketScriptText;

import java.util.*;

public class SPacketScriptConsole extends PacketServerBasic {

    protected static int channelId;
    private static final Map<Integer, Map<Long, String[]>> data = new HashMap<>();

    private final int type;
    private final int tab;
    private final long time;
    private final int id;
    private final int maxIDs;
    private final String part;

    public SPacketScriptConsole(int typeIn, int tabIn, long timeIn, int idIn, int maxIDsIn, String partIn) {
        type = typeIn;
        tab = tabIn;
        time = timeIn;
        id = idIn;
        maxIDs = maxIDsIn;
        part = partIn;
    }


    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.TOOL_SCRIPTER); }

    public static void encode(SPacketScriptConsole msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.type);
        buf.writeInt(msg.tab);
        buf.writeLong(msg.time);
        buf.writeInt(msg.id);
        buf.writeInt(msg.maxIDs);
        buf.writeUtf(msg.part);
    }

    public static SPacketScriptConsole decode(FriendlyByteBuf buf) { return new SPacketScriptConsole(buf.readInt(), buf.readInt(), buf.readLong(), buf.readInt(), buf.readInt(), buf.readUtf()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (SPacketScriptText.handlers.containsKey(type)) {
            if (type == 6 && !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.EDIT_CLIENT_SCRIPT)) {
                warn(CustomNpcsPermissions.EDIT_CLIENT_SCRIPT.getNodeName());
            }
            else {
                IScriptHandler handler = SPacketScriptText.handlers.get(type);
                if (!data.containsKey(tab)) { data.put(tab, new LinkedHashMap<>()); }
                if (!data.get(tab).containsKey(time)) { data.get(tab).put(time, new String[maxIDs]); }
                data.get(tab).get(time)[id] = part;
                boolean done = true;
                StringBuilder total = new StringBuilder();
                for (String str : data.get(tab).get(time)) {
                    if (str == null) {
                        done = false;
                        break;
                    }
                    total.append(str);
                }
                if (done) {
                    ScriptContainer container = handler.getScripts().get(tab);
                    if (container != null) {
                        container.console.put(time, total.toString());
                        handler.init();
                    }
                    data.get(tab).remove(time);
                    if (data.get(tab).isEmpty()) { data.remove(tab); }
                }
            }
        }
        CustomNpcs.debugData.end("Packets");
    }

}