package noppes.npcs.blocks.tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.client.gui.util.quests.QuestInterface;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;

import javax.annotation.Nonnull;

public class TileWaypoint
extends TileNpcEntity
implements ITickable {

	public String name = "";
	public int range = 10;
	private List<EntityPlayer> recentlyChecked = new ArrayList<>();
	private int ticks = 10;

	private List<EntityPlayer> getPlayerList(int x, int y, int z) {
		List<EntityPlayer> list = new ArrayList<>();
		try {
			list = world.getEntitiesWithinAABB(EntityPlayer.class,
					new AxisAlignedBB(pos, pos.add(1, 1, 1)).grow(x, y, z));
		}
		catch (Exception ignored) { }
		return list;
	}

	@Override
	public @Nonnull NBTTagCompound getUpdateTag() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("x", pos.getX());
		compound.setInteger("y", pos.getY());
		compound.setInteger("z", pos.getZ());
		compound.setInteger("Range", range);
		return compound;
	}

	@Override
	public void onDataPacket(@Nonnull NetworkManager net, @Nonnull SPacketUpdateTileEntity pkt) {
		range = pkt.getNbtCompound().getInteger("Range");
	}

	@Override
	public void readFromNBT(@Nonnull NBTTagCompound compound) {
		super.readFromNBT(compound);
		name = compound.getString("LocationName");
		range = Math.max(2, compound.getInteger("LocationRange"));
	}

	public void update() {
		if (world.isRemote || name.isEmpty()) { return; }
		--ticks;
		if (ticks > 0) { return; }
		ticks = 10;
		List<EntityPlayer> around = getPlayerList(range, range, range);
		if (around.isEmpty()) {
			recentlyChecked = new ArrayList<>();
			return;
		}
        List<EntityPlayer> toCheck;
        (toCheck = around).removeAll(recentlyChecked);
		int rng = range + (Math.min(range, 10));
		List<EntityPlayer> listMax = getPlayerList(rng, rng, rng);
		recentlyChecked.retainAll(listMax);
		toCheck.addAll(recentlyChecked);
		if (toCheck.isEmpty()) { return; }
		for (EntityPlayer player : toCheck) {
			PlayerData pdata = PlayerData.get(player);
			PlayerQuestData questData = pdata.questData;
			for (QuestData data : questData.activeQuests.values()) {
				if (data.quest.step == 2 && data.quest.questInterface.isCompleted(player)) {
					continue;
				}
				boolean bo = data.quest.step == 1;
				for (QuestObjective obj : data.quest
						.getObjectives((IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player))) {
					if (data.quest.step == 1 && !bo) {
						break;
					}
					bo = obj.isCompleted();
					if (obj.getEnumType() != EnumQuestTask.LOCATION || !obj.getTargetName().equals(name)) {
						continue;
					}

					QuestInterface quest = data.quest.questInterface;
					if (!quest.setFound(data, name)) {
						continue;
					}
					if (data.quest.showProgressInWindow) {
						NBTTagCompound compound = new NBTTagCompound();
						compound.setInteger("QuestID", data.quest.id);
						compound.setString("Type", "location");
						compound.setIntArray("Progress", new int[] { 1, 1 });
						compound.setString("TargetName", obj.getTargetName());
						Packets.send((EntityPlayerMP) player, new PacketAchievement(Component.empty(), Component.empty(), 0, compound));
					}
					if (data.quest.showProgressInChat) {
						player.sendMessage(new TextComponentTranslation("quest.message.location.1",
								new TextComponentTranslation(obj.getTargetName()).getFormattedText(),
								data.quest.getTitle()));
					}
					questData.checkQuestCompletion(player, data);
					questData.updateClient = true;
				}
			}
		}
	}

	@Override
	public @Nonnull NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
		if (!name.isEmpty()) { compound.setString("LocationName", name); }
		compound.setInteger("LocationRange", range);
		return super.writeToNBT(compound);
	}
}
