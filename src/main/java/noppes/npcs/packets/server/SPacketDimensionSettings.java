package noppes.npcs.packets.server;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.dimensions.CustomWorldInfo;
import noppes.npcs.dimensions.DimensionHandler;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketDimensionSettings extends PacketServerBasic {

    protected static int channelId;
    private final String dimensionName;
    private final CompoundTag settings;
    private final ResourceKey<Level> existingKey; // null = create new

    public SPacketDimensionSettings(String name, CompoundTag settings, ResourceKey<Level> existingKey) {
        this.dimensionName = name;
        this.settings = settings;
        this.existingKey = existingKey;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    public static void encode(SPacketDimensionSettings msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.dimensionName);
        buf.writeNbt(msg.settings);
        buf.writeBoolean(msg.existingKey != null);
        if (msg.existingKey != null) {
            buf.writeResourceKey(msg.existingKey);
        }
    }

    public static SPacketDimensionSettings decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        CompoundTag tag = buf.readNbt();
        ResourceKey<Level> key = null;
        if (buf.readBoolean()) {
            key = buf.readResourceKey(Registries.DIMENSION);
        }
        return new SPacketDimensionSettings(name, tag, key);
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        if (settings == null) { CustomNpcs.debugData.end("Packets"); return; }

        String cleanName = NoppesUtilServer.validPath(dimensionName.toLowerCase());
        DimensionHandler handler = DimensionHandler.getInstance();

        if (existingKey != null) {
            CustomWorldInfo existing = (CustomWorldInfo) handler.getMCWorldInfo(existingKey.location().toString());
            if (existing != null) {
                existing.load(settings);
                handler.setDirty();
                player.sendSystemMessage(Component.translatable("gui.save"));
            }
        } else {
            CustomWorldInfo info = new CustomWorldInfo(settings);
            info.setMCLevelName(cleanName);
            handler.createDimension(player, cleanName, info);
        }
        CustomNpcs.debugData.end("Packets");
    }
}