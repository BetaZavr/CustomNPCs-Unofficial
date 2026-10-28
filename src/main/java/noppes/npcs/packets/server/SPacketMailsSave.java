package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketMailsSave extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag compound;

    public SPacketMailsSave(CompoundTag compoundIn) { compound = compoundIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_MAIL); }

    public static void encode(SPacketMailsSave msg, FriendlyByteBuf buf) { buf.writeNbt(msg.compound); }

    public static SPacketMailsSave decode(FriendlyByteBuf buf) { return new SPacketMailsSave(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        CustomNpcs.MailTimeWhenLettersWillBeDeleted = compound.getInt("LettersBeDeleted");
        int[] vs = compound.getIntArray("LettersBeReceived");
        System.arraycopy(vs, 0, CustomNpcs.MailTimeWhenLettersWillBeReceived, 0, vs.length);
        vs = compound.getIntArray("CostSendingLetter");
        System.arraycopy(vs, 0, CustomNpcs.MailCostSendingLetter, 0, vs.length);
        CustomNpcs.MailSendToYourself = compound.getBoolean("SendToYourself");
        compound.putInt("LettersBeDeleted", CustomNpcs.MailTimeWhenLettersWillBeDeleted);
        compound.putIntArray("LettersBeReceived", CustomNpcs.MailTimeWhenLettersWillBeReceived);
        compound.putIntArray("CostSendingLetter", CustomNpcs.MailCostSendingLetter);
        compound.putBoolean("SendToYourself", CustomNpcs.MailSendToYourself);
        MinecraftServer server = player.level().getServer();
        if (server != null && server.getPlayerList().getPlayers().size() > 1) {
            for (ServerPlayer pl : server.getPlayerList().getPlayers()) {
                Packets.send(pl, new PacketSyncUpdate(0, 12, compound));
            }
        }
        CustomNpcs.debugData.end("Packets");
    }

}
