package noppes.npcs.packets.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.ClientTickHandler;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketRemoveLoadFile;
import noppes.npcs.shared.common.PacketBasic;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.TempFile;
import noppes.npcs.util.Util;

import java.io.File;

public class PacketSendFilePart extends PacketBasic {

    protected static int channelId;
    private final boolean remove;
    private final int partId;
    private final String name;
    private final String partText;

    public PacketSendFilePart(boolean isRemove, int part, String nameIn, String partTextIn) {
        remove = isRemove;
        partId = part;
        name = nameIn;
        partText = partTextIn;
    }

    public static void encode(PacketSendFilePart msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.remove);
        buf.writeInt(msg.partId);
        buf.writeUtf(msg.name);
        buf.writeUtf(msg.partText);
    }

    public static PacketSendFilePart decode(FriendlyByteBuf buf) {
        return new PacketSendFilePart(buf.readBoolean(),buf.readInt(), buf.readUtf(), buf.readUtf());
    }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (remove) { ClientProxy.loadFiles.remove(name); }
        if (!ClientProxy.loadFiles.containsKey(name)) {
            CustomNpcs.debugData.end("Packets");
            return;
        }
        TempFile file = ClientProxy.loadFiles.get(name);
        file.data.put(partId, partText);
        file.lastLoad = System.currentTimeMillis() - 15000L;
        file.tryLoads = 0;
        if (file.isLoad()) {
            if (file.saveType == 1) {
                LogWriter.info("Script Client file was received from the Server: \"" + name + "\"");
                File normalFile = new File(CustomNpcs.Dir, ScriptController.Instance.clientScripts.getLanguage().toLowerCase() + "/" + name);
                if (player.isCreative() || PlayerData.get(player).game.op) {
                    String s = "" + file.size;
                    if (file.size > 999) {
                        s = Util.instance.getTextReducedNumber(file.size, false, false, false);
                    }
                    player.sendSystemMessage(Component.literal("CustomNpcs").withStyle(ChatFormatting.DARK_GREEN)
                            .append(Component.literal(": Received client script: \"").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(normalFile.getAbsolutePath()).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("\" (").withStyle(ChatFormatting.GRAY))
                            .append(s)
                            .append(Component.literal("b)").withStyle(ChatFormatting.GRAY)));
                }
                // Put to session
                ScriptController.Instance.clients.put(name, file.getDataText());
                ScriptController.Instance.clientSizes.put(name, file.size);
                // save on client
                Util.instance.saveFile(normalFile, file.getDataText());
            }
            else { file.save(); }
            ClientProxy.loadFiles.remove(name);
            Packets.sendServer(new SPacketRemoveLoadFile(name));
        }
        ClientTickHandler.loadFiles();
        CustomNpcs.debugData.end("Packets");
    }

}
