package noppes.npcs.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.shared.common.PacketBasic;

public class PacketSyncRecipeRemove extends PacketBasic {

    protected static int channelId;
    private final ResourceLocation id;
    private final int type;
    private final CompoundTag data;

    public PacketSyncRecipeRemove(ResourceLocation idIn, int typeIn, CompoundTag dataIn) {
        id = idIn;
        type = typeIn;
        data = dataIn;
    }

    public static void encode(PacketSyncRecipeRemove msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.id);
        buf.writeInt(msg.type);
        buf.writeNbt(msg.data);
    }

    public static PacketSyncRecipeRemove decode(FriendlyByteBuf buf) {
        return new PacketSyncRecipeRemove(buf.readResourceLocation(), buf.readInt(), buf.readNbt());
    }

    @Override
    public int getChannelId() { return channelId; }

    @OnlyIn(Dist.CLIENT)
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (type == 6) {
            RecipeController.getInstance().delete(id);
            RecipeController.getInstance().reloadGlobalRecipes();
        }
        else if (type == 7) {
            RecipeController.getInstance().delete(id);
            RecipeController.getInstance().reloadAnvilRecipes();
        }
        CustomNpcs.debugData.end("Packets");
    }

}