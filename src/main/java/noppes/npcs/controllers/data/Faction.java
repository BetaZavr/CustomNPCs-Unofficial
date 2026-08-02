package noppes.npcs.controllers.data;

import java.util.HashSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.IFaction;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.entity.EntityNPCInterface;

public class Faction implements IFaction {

	public String name;
	public int color = 0xFFFFFF;
	public final HashSet<Integer> attackFactions = new HashSet<>();
	public int id = -1;
	public int neutralPoints = 500;
	public int friendlyPoints = 1500;
	public int defaultPoints = 1000;
	public boolean hideFaction = false;
	public boolean getsAttacked = false;
	public FactionOptions factions = new FactionOptions();

	// New from Unofficial (BetaZavr)
	public HashSet<Integer> frendFactions = new HashSet<>();
	public ITextComponent description = new TextComponentString("");
	public ResourceLocation flag = new ResourceLocation(CustomNpcs.MODID + ":textures/cloak/mojang.png");

	public Faction() { }

	public Faction(int idIn, String nameIn, int colorIn, int defaultPointsIn) {
		id = idIn;
		name = nameIn;
		color = colorIn;
		defaultPoints = defaultPointsIn;
	}

	public void load(NBTTagCompound compound) {
		name = compound.getString("Name");
		color = compound.getInteger("Color");
		id = compound.getInteger("Slot");
		neutralPoints = compound.getInteger("NeutralPoints");
		friendlyPoints = compound.getInteger("FriendlyPoints");
		defaultPoints = compound.getInteger("DefaultPoints");
		hideFaction = compound.getBoolean("HideFaction");
		getsAttacked = compound.getBoolean("GetsAttacked");
		factions.load(compound.getCompoundTag("FactionPoints"));
		attackFactions.clear();
		attackFactions.addAll(NBTTags.getIntegerSet(compound.getTagList("AttackFactions", 10)));

		// New from Unofficial (BetaZavr)
		frendFactions.clear();
		frendFactions.addAll(NBTTags.getIntegerSet(compound.getTagList("FrendFactions", 10)));
		if (compound.hasKey("Flag", 8)) { setFlag(compound.getString("Flag")); }
		if (compound.hasKey("Description", 8)) {
			description = Component.jsonToComponent(compound.getString("Description")).getParent();
		}
	}

	@Override
	public void save() { FactionController.instance.saveFaction(this); }

	public NBTTagCompound save(NBTTagCompound compound) {
		compound.setInteger("Slot", id);
		compound.setString("Name", name);
		compound.setInteger("Color", color);
		compound.setInteger("NeutralPoints", neutralPoints);
		compound.setInteger("FriendlyPoints", friendlyPoints);
		compound.setInteger("DefaultPoints", defaultPoints);
		compound.setBoolean("HideFaction", hideFaction);
		compound.setBoolean("GetsAttacked", getsAttacked);
		compound.setTag("AttackFactions", NBTTags.nbtIntegerCollection(attackFactions));
		compound.setTag("FactionPoints", factions.save(new NBTTagCompound()));

		// New from Unofficial (BetaZavr)
		compound.setString("Flag", flag != null ? flag.toString() : "");
		compound.setString("Description", ITextComponent.Serializer.componentToJson(description));
		compound.setTag("FrendFactions", NBTTags.nbtIntegerCollection(frendFactions));
		return compound;
	}

	public boolean isFriendlyToPlayer(EntityPlayer player) {
		PlayerFactionData data = PlayerData.get(player).factionData;
		if (data.getFactionPoints(player, id) < friendlyPoints) { return false; }
		FactionController fData = FactionController.instance;
		for (int idIn : attackFactions) {
			Faction faction = fData.factions.get(idIn);
			if (faction != null && data.getFactionPoints(player, idIn) < faction.neutralPoints) { return false; }
		}
		return true;
	}

	public boolean isAggressiveToPlayer(EntityPlayer player) {
		if (player.isCreative()) { return false; }
		PlayerFactionData data = PlayerData.get(player).factionData;
		if (data.getFactionPoints(player, id) < neutralPoints) { return true; }
		FactionController fData = FactionController.instance;
		for (int idIn : attackFactions) {
			Faction faction = fData.factions.get(idIn);
			if (faction != null && data.getFactionPoints(player, idIn) < faction.neutralPoints) { return true; }
		}
		return false;
	}

	public boolean isNeutralToPlayer(EntityPlayer player) {
		PlayerFactionData data = PlayerData.get(player).factionData;
		int points = data.getFactionPoints(player, id);
		if (points < neutralPoints || points >= friendlyPoints) { return false; }
		FactionController fData = FactionController.instance;
		for (int idIn : attackFactions) {
			Faction faction = fData.factions.get(idIn);
			if (faction != null && data.getFactionPoints(player, idIn) < faction.neutralPoints) { return false; }
		}
		return true;
	}

	public boolean isAggressiveToNpc(EntityNPCInterface npc) {
		return attackFactions.contains(npc.faction.id) || npc.advanced.attackFactions.contains(id);
	}

	@Override
	public int getId() { return id; }

	@Override
	public String getName() { return name; }

	@Override
	public int getDefaultPoints() { return defaultPoints; }

	@Override
	public void setDefaultPoints(int points) { defaultPoints = points; }

	@Override
	public int getColor() { return color; }

	@Override
	public int playerStatus(IPlayer<?> player) {
		return player == null ? -1 : playerStatus(player.getMCEntity());
	}

	public int playerStatus(EntityPlayer player) {
		return player == null || isAggressiveToPlayer(player) ? -1 : isFriendlyToPlayer(player) ? 1 : 0;
	}

	@Override
	public boolean hostileToNpc(ICustomNpc<?> npc) { return npc != null && attackFactions.contains(npc.getFaction().getId()); }

	@Override
	public int[] getHostileList() {
		int[] a = new int[attackFactions.size()];
		int i = 0;
		for (Integer val : attackFactions) { a[i++] = val; }
		return a;
	}

	@Override
	public void addHostile(int factionId) {
		if (attackFactions.contains(factionId)) { throw new CustomNPCsException("Faction " + factionId + " is already hostile to " + id); }
		frendFactions.remove(factionId);
		attackFactions.add(factionId);
	}

	@Override
	public void removeHostile(int factionId) { attackFactions.remove(factionId); }

	@Override
	public boolean hasHostile(int factionId) { return attackFactions.contains(factionId); }

	@Override
	public boolean getIsHidden() { return hideFaction; }

	@Override
	public void setIsHidden(boolean bo) { hideFaction = bo; }

	@Override
	public boolean getAttackedByMobs() { return getsAttacked; }

	@Override
	public void setAttackedByMobs(boolean bo) { getsAttacked = bo; }

	// New from Unofficial (BetaZavr)
	@Override
	public String getDescription() { return description.getFormattedText(); }

	@Override
	public void setDescription(String descriptionIn) {
		if (descriptionIn == null || descriptionIn.isEmpty()) { description = new TextComponentString(""); }
		else { description = new TextComponentTranslation(descriptionIn); }
	}

	@Override
	public String getFlag() { return flag == null ? "" : flag.toString(); }

	@Override
	public boolean hostileToFaction(int factionId) { return attackFactions.contains(factionId); }

	@Override
	public void setFlag(String flagPath) {
		if (flagPath == null || flagPath.isEmpty()) {
			flag = null;
			return;
		}
		flag = new ResourceLocation(NoppesUtilServer.validLocation(flagPath));
	}

}
