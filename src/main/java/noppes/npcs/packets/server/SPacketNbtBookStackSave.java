package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.util.CustomNPCsScheduler;

import java.util.Collections;
import java.util.List;

public class SPacketNbtBookStackSave extends PacketServerBasic {

    protected static int channelId;
    private final CompoundTag data;

    public SPacketNbtBookStackSave(CompoundTag stackNBT) { data = stackNBT; }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.TOOL_NBTBOOK); }

    public static void encode(SPacketNbtBookStackSave msg, FriendlyByteBuf buf) {buf.writeNbt(msg.data); }

    public static SPacketNbtBookStackSave decode(FriendlyByteBuf buf) { return new SPacketNbtBookStackSave(buf.readNbt()); }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        ItemStack mcItem = ItemStack.of(data);
        if (mcItem.isEmpty()) { player.sendSystemMessage(Component.translatable("nbt.book.not.correct.nbt")); }
        else {
            player.setItemInHand(InteractionHand.OFF_HAND, mcItem);
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