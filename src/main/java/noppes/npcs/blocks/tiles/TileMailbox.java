package noppes.npcs.blocks.tiles;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

import javax.annotation.Nonnull;

public class TileMailbox extends TileNpcEntity {

    private int type = 0;

    public TileMailbox setModel(int typeIn) {
        type = typeIn;
        return this;
    }

    public int getModel() { return type; }

    @Override
    public @Nonnull NBTTagCompound getUpdateTag() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("x", pos.getX());
        compound.setInteger("y", pos.getY());
        compound.setInteger("z", pos.getZ());
        compound.setInteger("Type", type);
        return compound;
    }

    @Override
    public void onDataPacket(@Nonnull NetworkManager net, @Nonnull SPacketUpdateTileEntity pkt) {
        type = pkt.getNbtCompound().getInteger("Type");
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        type = compound.getInteger("Type");
    }

    @Override
    public @Nonnull NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
        compound.setInteger("Type", type);
        return super.writeToNBT(compound);
    }

}
