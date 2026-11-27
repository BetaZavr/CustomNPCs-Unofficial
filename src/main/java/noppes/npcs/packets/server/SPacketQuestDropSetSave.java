package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;

public class SPacketQuestDropSetSave extends PacketServerBasic {

    protected static int channelId;
    private int questId;
    private NBTTagCompound compound;

    public SPacketQuestDropSetSave() { }

    public SPacketQuestDropSetSave(int questIdIn, NBTTagCompound compoundIn) {
        questId = questIdIn;
        compound = compoundIn;
    }

    @Override
    public boolean requiresNpc() { return false; }

    @Override
    public List<CustomNpcsPermissions.Permission> getPermission() { return Collections.singletonList(CustomNpcsPermissions.GLOBAL_QUEST); }

    @Override
    public boolean toolAllowed(ItemStack item) { return true; }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(questId);
        buf.writeNbt(compound);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        questId = buf.readInt();
        compound = buf.readNbt();
    }

    @Override
    public int getChannelId() { return channelId; }

    @Override
    protected void handle() {
        CustomNpcs.debugData.start("Packets");
        Quest quest = QuestController.instance.get(questId);
        if (quest != null) {
            int pos = compound.getInteger("Slot");
            if (pos < 0 || !quest.rewardItems.containsKey(pos)) {
                pos = quest.rewardItems.size();
                DropSet ds = new DropSet(quest);
                ds.pos = pos;
                quest.rewardItems.put(ds.pos, ds);
            }
            quest.rewardItems.get(pos).load(compound);
            Packets.send(player, new PacketGuiData(quest.save(new NBTTagCompound())));
        }
        CustomNpcs.debugData.end("Packets");
    }

}
