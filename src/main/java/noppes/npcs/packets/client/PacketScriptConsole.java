package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.script.GuiScriptInterface;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.shared.common.PacketBasic;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PacketScriptConsole extends PacketBasic {

    protected static int channelId;
    private static final Map<Integer, Map<Long, String[]>> data = new HashMap<>();

    private final int tab;
    private final long time;
    private final int id;
    private final int maxIDs;
    private final String part;
    private final boolean isSetClient;

    public PacketScriptConsole(int tabIn, long timeIn, int idIn, int maxIDsIn, String partIn, boolean isSetClientIn) {
        tab = tabIn;
        time = timeIn;
        id = idIn;
        maxIDs = maxIDsIn;
        part = partIn;
        isSetClient = isSetClientIn;
    }

    public static void encode(PacketScriptConsole msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.tab);
        buf.writeLong(msg.time);
        buf.writeInt(msg.id);
        buf.writeInt(msg.maxIDs);
        buf.writeUtf(msg.part);
        buf.writeBoolean(msg.isSetClient);
    }

    public static PacketScriptConsole decode(FriendlyByteBuf buf) {
        return new PacketScriptConsole(buf.readInt(), buf.readLong(), buf.readInt(), buf.readInt(), buf.readUtf(), buf.readBoolean());
    }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
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
            if (Minecraft.getInstance().screen instanceof GuiScriptInterface gui) {
                gui.setTabConsole(tab, time, total.toString());
            }
            if (isSetClient) {
                ScriptContainer container = ScriptController.Instance.clientScripts.getScripts().get(tab);
                if (container != null) {
                    container.script = total.toString();
                    container.setInit(false);
                    ScriptController.Instance.clientScripts.init();
                }
            }
            data.get(tab).remove(time);
            if (data.get(tab).isEmpty()) { data.remove(tab); }
        }
        CustomNpcs.debugData.end("Packets");
    }

}