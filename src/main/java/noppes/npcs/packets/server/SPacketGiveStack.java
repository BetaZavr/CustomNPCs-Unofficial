package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.util.CustomNPCsScheduler;

import java.util.List;

public class SPacketGiveStack extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag data;

    public SPacketGiveStack(CompoundTag stackNBT) { data = stackNBT;}

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() {
        return List.of(CustomNpcsPermissions.TOOL_NBTBOOK, CustomNpcsPermissions.GLOBAL_FACTION);
    }

    @Override
    public boolean toolAllowed(ItemStack item){ return true; }

    public static void encode(SPacketGiveStack msg, FriendlyByteBuf buf) {buf.writeNbt(msg.data); }

    public static SPacketGiveStack decode(FriendlyByteBuf buf) { return new SPacketGiveStack(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        ItemStack mcItem = ItemStack.of(data);
        if (!mcItem.isEmpty() && player.getInventory().add(mcItem.copy())) {
            NoppesUtilServer.playSound(player, SoundEvents.ITEM_PICKUP, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            player.inventoryMenu.broadcastChanges();
            player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, player.getInventory().selected, player.getInventory().getItem(player.getInventory().selected)));
            PlayerQuestData playerdata = PlayerData.get(player).questData;
            CustomNPCsScheduler.runTack(() -> {
                for (QuestData data : playerdata.activeQuests.values()) {
                    for (QuestObjective obj : data.quest.getObjectives(player)) {
                        if (obj.getEnumType() != EnumQuestTask.ITEM) { continue; }
                        playerdata.checkQuestCompletion(player, data);
                    }
                }
            });
        }
        CustomNpcs.debugData.end("Packets");
    }

}