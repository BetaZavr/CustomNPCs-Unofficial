package moe.plushie.armourers_workshop.api.network;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import moe.plushie.armourers_workshop.api.core.IDataCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("unused")
public interface IFriendlyByteBuf {

    static IFriendlyByteBuf wrap(ByteBuf buf) {
        try {
            Class<?> clazz = Class.forName("moe.plushie.armourers_workshop.compat.core.data.AbstractFriendlyByteBuf");
            return (IFriendlyByteBuf) clazz.getDeclaredMethod("wrap", ByteBuf.class).invoke(null, buf);
        }
        catch (Exception ignored) { }
        return null;
    }

    int readByte();

    void writeByte(int var1);

    int readInt();

    void writeInt(int var1);

    long readLong();

    void writeLong(long var1);

    float readFloat();

    void writeFloat(float var1);

    double readDouble();

    void writeDouble(double var1);

    boolean readBoolean();

    void writeBoolean(boolean var1);

    int readVarInt();

    void writeVarInt(int var1);

    ByteBuf readBytes(int var1);

    void writeBytes(ByteBuf var1);

    String readUtf();

    void writeUtf(String var1);

    UUID readUUID();

    void writeUUID(UUID var1);

    <T extends Enum<T>> T readEnum(Class<T> var1);

    <T extends Enum<T>> void writeEnum(T var1);

    ItemStack readItem();

    void writeItem(ItemStack var1);

    BlockPos readBlockPos();

    void writeBlockPos(BlockPos var1);

    GlobalPos readGlobalPos();

    void writeGlobalPos(GlobalPos var1);

    BlockHitResult readBlockHitResult();

    void writeBlockHitResult(BlockHitResult var1);

    CompoundTag readNbt();

    void writeNbt(CompoundTag var1);

    <T> T readNbtWithCodec(IDataCodec<T> var1);

    <T> void writeNbtWithCodec(IDataCodec<T> var1, T var2);

    Component readComponent();

    void writeComponent(Component var1);

    IFriendlyByteBuf copy();

    IFriendlyByteBuf slice();

    IFriendlyByteBuf retainedSlice();

    IFriendlyByteBuf duplicate();

    IFriendlyByteBuf retainedDuplicate();

    ByteBuf asByteBuf();

}

