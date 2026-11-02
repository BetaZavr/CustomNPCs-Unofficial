package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.*;

public class SPacketScriptConsole extends PacketServerBasic {

    protected static int channelId;
    private static final Map<Integer, Map<Long, String[]>> data = new HashMap<>();

    private int type;
    private int tab;
    private long time;
    private int id;
    private int maxIDs;
    private String part;

    public SPacketScriptConsole() { }

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
    public List<CustomNpcsPermissions.Permission> getPermission() { return Collections.singletonList(CustomNpcsPermissions.TOOL_SCRIPTER); }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(type);
        buf.writeInt(tab);
        buf.writeLong(time);
        buf.writeInt(id);
        buf.writeInt(maxIDs);
        buf.writeUtf(part);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        type = buf.readInt();
        tab = buf.readInt();
        time = buf.readLong();
        id = buf.readInt();
        maxIDs = buf.readInt();
        part = buf.readUtf();
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (SPacketScriptText.handlers.containsKey(type)) {
            if (type == 6 && !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.EDIT_CLIENT_SCRIPT)) {
                warn(CustomNpcsPermissions.EDIT_CLIENT_SCRIPT.getNodeName());
            } else {
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
                    if (container != null) { container.console.put(time, total.toString()); }
                    data.get(tab).remove(time);
                    if (data.get(tab).isEmpty()) { data.remove(tab); }
                }
            }
        }
        CustomNpcs.debugData.end("Packets");
    }

}