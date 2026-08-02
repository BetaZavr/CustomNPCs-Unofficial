package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.gui.INpcMenuGui;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.PacketBasic;

public class PacketNpcInitData extends PacketBasic {

    protected static int channelId;
    private int npcId;
    private NBTTagCompound compound;

    public PacketNpcInitData() { }

    public PacketNpcInitData(int npcIdIn, NBTTagCompound compoundIn) {
        npcId = npcIdIn;
        compound = compoundIn;
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        npcId = buf.readInt();
        compound = buf.readAnySizeNbt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(npcId);
        buf.writeNbt(compound);
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Entity e = player.world.getEntityByID(npcId);
        if (e instanceof EntityNPCInterface
                && !(Minecraft.getMinecraft().currentScreen instanceof INpcMenuGui && NoppesUtilServer.getEditingNpc(player) == e)) {
            e.readFromNBT(compound);
        }
        CustomNpcs.debugData.end("Packets");
    }

}