package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.item.ISpecBuilder;
import noppes.npcs.controllers.SyncController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.util.BuilderData;

import java.util.List;

public class SPacketSetBuildData extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag compound;

    public SPacketSetBuildData(CompoundTag compoundIn) { compound = compoundIn; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return null; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    public static void encode(SPacketSetBuildData msg, FriendlyByteBuf buf) { buf.writeNbt(msg.compound); }

    public static SPacketSetBuildData decode(FriendlyByteBuf buf) { return new SPacketSetBuildData(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    public void handle() {
        CustomNpcs.debugData.start("Packets");
        ItemStack stack = player.getInventory().getSelected();
        if (stack.getItem() instanceof ISpecBuilder &&
                compound.contains("ID", 3) &&
                compound.contains("BuilderType", 3)) {
            int id = compound.getInt("ID");
            BuilderData builder = SyncController.dataBuilder.get(id);
            if (id < 0 || builder == null) {
                if (id < 0 && builder != null) { SyncController.dataBuilder.remove(id); }
                id = 0;
                while (SyncController.dataBuilder.containsKey(id)) { id ++; }
                builder = new BuilderData(id, compound.getInt("BuilderType"));
                compound.putInt("ID", id);
            }
            builder.read(compound);
            SyncController.dataBuilder.put(id, builder);
            CompoundTag nbtStack = builder.getNbt();
            if (compound.getInt("BuilderType") != ((ISpecBuilder) stack.getItem()).getType()) {
                stack = switch (compound.getInt("BuilderType")) {
                    case 1 -> new ItemStack(CustomItems.npcbuilder);
                    case 2 -> new ItemStack(CustomItems.npcreplacer);
                    case 3 -> new ItemStack(CustomItems.npcplacer);
                    case 4 -> new ItemStack(CustomItems.npcsaver);
                    default -> new ItemStack(CustomItems.npcremover);
                };
            }
            else { stack = stack.copy(); }
            stack.setTag(nbtStack);
            player.getInventory().setItem(player.getInventory().selected, stack);
            player.containerMenu.broadcastChanges();
            Packets.send(player, new PacketSyncUpdate(id, 7, nbtStack));
        }
        CustomNpcs.debugData.end("Packets");
    }

}