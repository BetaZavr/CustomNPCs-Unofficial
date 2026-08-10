package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.entity.data.role.IJobSpawner;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;
import noppes.npcs.roles.JobSpawner;
import noppes.npcs.roles.data.JobSpawnerCloneData;
import noppes.npcs.roles.data.JobSpawnerNbtData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketNpcJobSpawnerAdd extends PacketServerBasic {

    protected static int channelId;
    private final boolean isDead;
    private final int tab;
    private final String name;
    private final CompoundTag spawnerData;

    public SPacketNpcJobSpawnerAdd(boolean isDeadIn, String nameIn, int tabIn, CompoundTag compoundIn) {
        isDead = isDeadIn;
        name = nameIn;
        tab = tabIn;
        spawnerData = compoundIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketNpcJobSpawnerAdd msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isDead);
        buf.writeUtf(msg.name);
        buf.writeInt(msg.tab);
        buf.writeNbt(msg.spawnerData);
    }

    public static SPacketNpcJobSpawnerAdd decode(FriendlyByteBuf buf) {
        return new SPacketNpcJobSpawnerAdd(buf.readBoolean(), buf.readUtf(), buf.readInt(), buf.readAnySizeNbt());
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (npc.job instanceof JobSpawner job) {
            IJobSpawner.IJobSpawnerData sd;
            if (spawnerData.isEmpty() && !name.isEmpty() && tab > 0 && tab < 10) {
                sd = job.get(isDead).add(true);
                if (sd instanceof JobSpawnerCloneData jobData) {
                    jobData.setName(name);
                    jobData.setTab(tab);
                }
            } // server
            else if (!spawnerData.isEmpty()) {
                sd = job.get(isDead).add(false);
                if (sd instanceof JobSpawnerNbtData jobData) { jobData.load(spawnerData); }
            } // client
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("JobData", true);
            job.save(nbt);
            job.cleanCompound(nbt);
            nbt.putBoolean("SetDead", isDead);
            nbt.putInt("SetPos", job.get(isDead).dataEntitys.size() - 1);
            Packets.send(player, new PacketGuiData(nbt));
        }
        CustomNpcs.debugData.end("Packets");
    }

}