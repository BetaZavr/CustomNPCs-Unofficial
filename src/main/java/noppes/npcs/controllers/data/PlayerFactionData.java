package noppes.npcs.controllers.data;

import java.util.HashMap;
import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.handler.data.IPlayerData;
import noppes.npcs.api.wrapper.PlayerWrapper;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;

public class PlayerFactionData implements IPlayerData {

	protected static final String dataName = "FactionData";

	public HashMap<Integer, Integer> factionData = new HashMap<>();

	@Override
	public void load(NBTTagCompound compound) {
		factionData.clear();
		if (compound != null && compound.hasKey(dataName, 9)) {
			NBTTagList list = compound.getTagList(dataName, 10);
			for (int i = 0; i < list.tagCount(); ++i) {
				NBTTagCompound nbttagcompound = list.getCompoundTagAt(i);
				factionData.put(nbttagcompound.getInteger("Faction"), nbttagcompound.getInteger("Points"));
			}
		}
	}

	@Override
	public NBTTagCompound save(NBTTagCompound compound) {
		NBTTagList list = new NBTTagList();
		for (int faction : factionData.keySet()) {
			NBTTagCompound nbttagcompound = new NBTTagCompound();
			nbttagcompound.setInteger("Faction", faction);
			nbttagcompound.setInteger("Points", factionData.get(faction));
			list.appendTag(nbttagcompound);
		}
		compound.setTag(dataName, list);
		return compound;
	}

	public int getFactionPoints(EntityPlayer player, int factionId) {
		Faction faction = FactionController.instance.getFaction(factionId);
		if (faction == null) { return 0; }
		if (!factionData.containsKey(factionId)) {
			if (player.world.isRemote) { factionData.put(factionId, faction.defaultPoints); }
			else {
				PlayerWrapper<?> wrapper = (PlayerWrapper<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player);
				PlayerEvent.FactionUpdateEvent event = new PlayerEvent.FactionUpdateEvent(wrapper, faction, faction.defaultPoints, true);
				EventHooks.onPlayerFactionChange(event);
				if (!event.isCanceled() && event.faction != null) {
					int value = ValueUtil.correctInt(event.points, 0, Integer.MAX_VALUE);
					if (value >= 0) { factionData.put(event.faction.getId(), value); }
					else { factionData.remove(event.faction.getId()); }
					Packets.send((EntityPlayerMP) player, new PacketSyncUpdate(0, 13, save(new NBTTagCompound())));
				}
			}
		}
		return factionData.getOrDefault(factionId, 0);
	}

	public void increasePoints(EntityPlayer player, int factionId, int points) {
		if (player != null && !player.world.isRemote) {
			Faction faction = FactionController.instance.getFaction(factionId);
			if (faction != null) {
				PlayerEvent.FactionUpdateEvent event = new PlayerEvent.FactionUpdateEvent(PlayerData.get(player).getIPlayer(), faction, points, false);
				EventHooks.onPlayerFactionChange(event);
				if (event.faction != null) {
					factionId = event.faction.getId();
					int value = ValueUtil.correctInt(event.points, 0, Integer.MAX_VALUE);
					if (!factionData.containsKey(factionId)) { value += faction.defaultPoints; } else { value += factionData.get(factionId); }
					if (value > 0) { factionData.put(factionId, value); } else { factionData.remove(factionId); }
					Packets.send((EntityPlayerMP) player, new PacketSyncUpdate(0, 13, save(new NBTTagCompound())));
				}
			}
		}
	}

	public NBTTagCompound getPlayerGuiData(@Nonnull EntityPlayerMP player) {
		NBTTagCompound compound = new NBTTagCompound();
		save(compound);
		NBTTagList list = new NBTTagList();
		for (int id : factionData.keySet()) {
			Faction faction = FactionController.instance.getFaction(id);
			if (faction != null && (player.isCreative() || !faction.hideFaction)) {
				NBTTagCompound com = new NBTTagCompound();
				faction.save(com);
				list.appendTag(com);
			}
		}
		compound.setTag("FactionList", list);
		return compound;
	}

	public void clear() { factionData.clear(); }

}
