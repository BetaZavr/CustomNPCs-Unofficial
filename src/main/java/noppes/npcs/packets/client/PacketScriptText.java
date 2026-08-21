package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.script.GuiScriptInterface;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.shared.common.PacketBasic;

import java.util.HashMap;
import java.util.Map;

public class PacketScriptText extends PacketBasic {

    protected static int channelId;
    private static final Map<Integer, String[]> data = new HashMap<>();

    private final int tab;
    private final int id;
    private final int maxIDs;
    private final String part;
    private final boolean isSetClient;

    public PacketScriptText(int tabIn, int idIn, int maxIDsIn, String partIn, boolean isSetClientIn) {
        tab = tabIn;
        id = idIn;
        maxIDs = maxIDsIn;
        part = partIn;
        isSetClient = isSetClientIn;
    }

    public static void encode(PacketScriptText msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.tab);
        buf.writeInt(msg.id);
        buf.writeInt(msg.maxIDs);
        buf.writeUtf(msg.part);
        buf.writeBoolean(msg.isSetClient);
    }

    public static PacketScriptText decode(FriendlyByteBuf buf) {
        return new PacketScriptText(buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf(), buf.readBoolean());
    }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
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
            if (Minecraft.getInstance().screen instanceof GuiScriptInterface gui) {
                gui.setTabScript(tab, total.toString());
            }
            if (isSetClient) {
                ScriptContainer container = ScriptController.Instance.clientScripts.getScripts().get(tab);
                if (container != null) {
                    container.script = total.toString();
                    container.setInit(false);
                    ScriptController.Instance.clientScripts.init();
                    if (ScriptController.Instance.clientScripts.isEnabled()) {
                        player.sendSystemMessage(Component.translatable("scripts.client.received.server"));
                    }
                }
            }
            data.remove(tab);
        }
        CustomNpcs.debugData.end("Packets");
    }

}
