package noppes.npcs.roles;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.*;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.constants.JobType;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.role.IRoleFollower;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.containers.inventories.NpcMiscInventory;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerGameData.FollowerSet;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;

public class RoleFollower extends RoleInterface implements IRoleFollower {

	public boolean disableGui = false;
	public boolean infiniteDays = false;
	public boolean isFollowing = true;
	public boolean refuseSoulStone = false;
	public int daysHired;
	public String ownerUUID;
	public long hiredTime; // ticks
	public long waitTime = 0;
	public int rentalMoney = 0;
	public boolean rentalIsDonat = false;
	public NpcMiscInventory rentalItems = new NpcMiscInventory(3);
	public NpcMiscInventory inventory = new NpcMiscInventory(0);
	public EntityPlayer owner = null;
	public HashMap<Integer, Integer> rates = new HashMap<>();
	public String dialogFarewell = getTexts("follower.farewellText", " {player}");
	public String dialogFired = getTexts("follower.firedText", " {player}");
	public String dialogHire = getTexts("follower.hireText", " {days} ", Component.translatable("follower.days"));
	public String dialogHired = getTexts("follower.is.hired", " {player}");

	private String getTexts(@Nonnull String main, Object ... added) {
		Component component = Component.translatable(main);
		for (Object part : added) {
			if (part instanceof String) { component.append((String) part); }
			else if (part instanceof Component) { component.append((Component) part); }
		}
		return Util.instance.deleteColor(component.getFormattedText());
	}

	public RoleFollower(EntityNPCInterface npc) {
		super(npc);
		type = RoleType.FOLLOWER;
	}

	@Override
	public void load(NBTTagCompound compound) {
		super.load(compound);
		type = RoleType.FOLLOWER;
		ownerUUID = compound.getString("MercenaryOwner");
		daysHired = compound.getInteger("MercenaryDaysHired");
		hiredTime = compound.getLong("MercenaryHiredTime");
		rates = NBTTags.getIntegerIntegerMap(compound.getTagList("MercenaryDayRates", 10));
		if (compound.hasKey("MercenaryInventory", 10)) {
			int size = compound.getCompoundTag("MercenaryInventory").getInteger("NpcMiscInvSize");
			inventory = new NpcMiscInventory(ValueUtil.correctInt(size, 0, 9));
			inventory.load(compound.getCompoundTag("MercenaryInventory"));
		}
		isFollowing = compound.getBoolean("MercenaryIsFollowing");
		disableGui = compound.getBoolean("MercenaryDisableGui");
		infiniteDays = compound.getBoolean("MercenaryInfiniteDays");
		refuseSoulStone = compound.getBoolean("MercenaryRefuseSoulstone");
		rentalIsDonat = compound.getBoolean("MercenaryRentalIsDonat");
		dialogHire = compound.getString("MercenaryDialogHired");
		dialogHired = compound.getString("MercenaryDialog");
		dialogFarewell = compound.getString("MercenaryDialogFarewell");
		// New from Unofficial (BetaZavr)
		rentalItems.load(compound.getCompoundTag("MercenaryInv"));
		rentalMoney = compound.getInteger("MercenaryMoney");
		if (compound.hasKey("MercenaryDialogFired", 8)) {
			dialogFired = compound.getString("MercenaryDialogFired");
		}
	}

	@Override
	public NBTTagCompound save(NBTTagCompound compound) {
		super.save(compound);
		compound.setInteger("MercenaryDaysHired", daysHired);
		compound.setLong("MercenaryHiredTime", hiredTime);
		compound.setString("MercenaryDialogHired", dialogHire);
		compound.setString("MercenaryDialog", dialogHired);
		compound.setString("MercenaryDialogFarewell", dialogFarewell);
		if (hasOwner()) { compound.setString("MercenaryOwner", ownerUUID); }
		compound.setTag("MercenaryDayRates", NBTTags.nbtIntegerIntegerMap(rates));
		compound.setTag("MercenaryInventory", inventory.save());
		compound.setBoolean("MercenaryIsFollowing", isFollowing);
		compound.setBoolean("MercenaryDisableGui", disableGui);
		compound.setBoolean("MercenaryInfiniteDays", infiniteDays);
		compound.setBoolean("MercenaryRefuseSoulstone", refuseSoulStone);
		compound.setBoolean("MercenaryRentalIsDonat", rentalIsDonat);
		// New from Unofficial (BetaZavr)
		compound.setTag("MercenaryInv", rentalItems.save());
		compound.setInteger("MercenaryMoney", rentalMoney);
		compound.setString("MercenaryDialogFired", dialogFired);
		return compound;
	}

	@Override
	public void addDays(int days) {
		if (hiredTime == 0L) {
			daysHired = days;
			hiredTime = getCurrentTime();
		}
		else { daysHired += days; }
	}

	@Override
	public boolean aiShouldExecute() {
		// New from Unofficial (BetaZavr)
		if (npc.getHealth() <= 0.0f) { return false; }
		if ((ownerUUID == null || ownerUUID.isEmpty()) && npc.world.provider.getDimension() != npc.homeDimensionId) {
			npc = (EntityNPCInterface) Util.instance.teleportEntity(npc.world.getMinecraftServer(), npc,
						npc.homeDimensionId, npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos());
			return false;
		}
		PlayerData plData = getOwnerData();
		if (plData == null) {
			if (ownerUUID != null && !ownerUUID.isEmpty()) { killed(); }
			return false;
		}
		FollowerSet fs = plData.game.getFollower(npc);
        if (fs == null) { fs = plData.game.addFollower(npc); }
        fs.dimId = npc.world.provider.getDimension();
        fs.npc = npc;
        owner = getOwner();
		if (!infiniteDays && (getCurrentTime() - hiredTime) > (long) getDays() * 24000L) {
			RoleEvent.FollowerFinishedEvent event = new RoleEvent.FollowerFinishedEvent(owner, npc.wrappedNPC);
			EventHooks.onNPCRole(npc, event);
			if (owner != null && owner.openContainer instanceof ContainerNPCFollowerHire) { owner.closeScreen(); }
			npc.say(owner, new Line(NoppesStringUtils.formatText(dialogFarewell, owner, npc)));
            plData.game.removeFollower(npc);
            killed();
		}
		if (npc.getAttackTarget() != null) { return false; }
		if (!isFollowing) {
			if (!npc.getNavigator().noPath()) { npc.getNavigator().clearPath(); }
			return false;
		}
		if (owner == null) { return false; }
		double dist = npc.getDistance(owner);
		if (owner.world.provider.getDimension() != npc.world.provider.getDimension()) {
			npc = (EntityNPCInterface) Util.instance.teleportEntity(npc.world.getMinecraftServer(), npc, owner.world.provider.getDimension(), owner.posX, owner.posY, owner.posZ);
			fs.dimId = npc.world.provider.getDimension();
			fs.id = npc.getUniqueID();
			fs.npc = npc;
			npc.getNavigator().tryMoveToEntityLiving(owner, npc.ais.canSprint ? 1.3 : 1.0d);
		}
		else if (dist <= 2.5d) {
			if (!npc.getNavigator().noPath()) { npc.getNavigator().clearPath(); }
			return false;
		}
		else if (dist > getRange()) { npc.setPosition(owner.posX, owner.posY, owner.posZ); }
		else {
			boolean bo = npc.getNavigator().tryMoveToEntityLiving(owner, npc.ais.canSprint ? 1.3 : 1.0d);
			if (!bo && !npc.isMoving()) {
				if (waitTime == 0) {
					waitTime = 10;
					return false;
				}
				waitTime--;
				if (waitTime <= 0) { npc.setPosition(owner.posX, owner.posY, owner.posZ); }
			}
			else { waitTime = 0; }
		}
		return false;
	}

	@Override
	public boolean defendOwner() { return isFollowing() && npc != null && npc.job.getEnumType() == JobType.GUARD; }

    @Override
	public int getDays() {
		if (infiniteDays) { return 100; }
        return Math.max(daysHired, 0);
    }

	@Override
	public IPlayer<?> getFollowing() {
		EntityPlayer owner = getOwner();
		if (owner != null) {
			return (IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(owner);
		}
		return null;
	}

	@Override
	public boolean getGuiDisabled() { return disableGui; }

	@Override
	public boolean getInfinite() { return infiniteDays; }

	@SuppressWarnings("ConstantConditions")
	public EntityPlayer getOwner() {
		if (npc != null && npc.world != null && !npc.world.isRemote && (ownerUUID != null && !ownerUUID.isEmpty())) {
			try {
				UUID uuid = UUID.fromString(ownerUUID);
				MinecraftServer server = npc.world.getMinecraftServer();
				if (server == null && CustomNpcs.Server != null) { server = CustomNpcs.Server; }
				if (server != null) { return server.getPlayerList().getPlayerByUUID(uuid); }
			} catch (Exception e) { LogWriter.error(e); }
			return npc.world.getPlayerEntityByName(ownerUUID);
		}
		return null;
	}

	@Override
	public boolean getRefuseSoulstone() { return refuseSoulStone; }

	@Override
	public boolean getRentalIsDonat() { return rentalIsDonat; }

	public boolean hasOwner() {
		return (infiniteDays || daysHired > 0) && ownerUUID != null && !ownerUUID.isEmpty();
	}

	@Override
	public void interact(EntityPlayer playerIn) {
		boolean bo = false;
		for (int i =0; i < rentalItems.getSizeInventory(); i++) {
			if (!NoppesUtilServer.isItemStackNull(rentalItems.getStackInSlot(i)) && rates.get(i) > 0) {
				bo = true;
				break;
			}
		}
		if (rentalMoney > 0 && rates.get(3) > 0) { bo = true; }
		if (bo && playerIn instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) playerIn;
			if (ownerUUID != null && !ownerUUID.isEmpty()) {
				if (player.equals(getOwner()) && !disableGui) { SPacketGuiOpen.sendOpenGui(player, EnumGuiType.PlayerFollower, npc, BlockPos.ORIGIN); }
				else if (npc != null && !dialogHired.isEmpty()) { npc.say(player, new Line(dialogHired)); }
			}
			else {
				if (npc != null) { npc.say(player, npc.advanced.getInteractLine()); }
				SPacketGuiOpen.sendOpenGui(player, EnumGuiType.PlayerFollowerHire, npc, BlockPos.ORIGIN);
			}
		}
	}

	@Override
	public boolean isFollowing() {
		return ownerUUID != null && !ownerUUID.isEmpty() && isFollowing &&
				(getCurrentTime() - hiredTime) < (long) getDays() * 24000L;
	}

	@Override
	public void killed() {
		if (!inventory.isEmpty()) {
			if (owner == null) {
				for (int i = 0; i < inventory.getSizeInventory(); i++) {
					ItemStack stack = inventory.getStackInSlot(i);
					if (!NoppesUtilServer.isItemStackNull(stack)) { npc.entityDropItem(stack, 0.0f); }
				}
			}
			else if (owner.world.provider.getDimension() == npc.world.provider.getDimension()) {
				for (int i = 0; i < inventory.getSizeInventory(); i++) {
					ItemStack entityItem = inventory.getStackInSlot(i);
					if (!NoppesUtilServer.isItemStackNull(entityItem)) {
						EntityItem entityitem = new EntityItem(owner.world, owner.posX, owner.posY, owner.posZ, entityItem);
						entityitem.setPickupDelay(0);
						owner.world.spawnEntity(entityitem);
					}
				}
			}
			inventory.clear();
		}
		ownerUUID = null;
		daysHired = 0;
		hiredTime = 0L;
		isFollowing = true;
		PlayerData plData = getOwnerData();
		if (plData != null) {
			plData.game.removeFollower(npc);
			plData.save(true);
		}
	}

	@Override
	public void reset() { killed(); }

	@Override
	public void setFollowing(IPlayer<?> player) {
		if (player == null) { ownerUUID = null; }
		else { setOwner(player.getMCEntity()); }
	}

	@Override
	public void setGuiDisabled(boolean disabled) { disableGui = disabled; }

	@Override
	public void setInfinite(boolean infinite) { infiniteDays = infinite; }

	public void setOwner(EntityPlayer player) {
		if (player == null ||
				ownerUUID == null ||
				!ownerUUID.equals(player.getUniqueID().toString())) { killed(); }
		if (player != null) { ownerUUID = player.getUniqueID().toString(); }
	}

	@Override
	public void setRefuseSoulstone(boolean refuse) { refuseSoulStone = refuse; }

	@Override
	public void setRentalIsDonat(boolean refuse) { rentalIsDonat = refuse; }

	// New from Unofficial (BetaZavr)
	public long getCurrentTime() {
		World level = null;
		if (npc != null) { level = npc.world; }
		else if (CustomNpcs.Server != null) { level = CustomNpcs.Server.getWorld(0); }
		return level != null ? level.getTotalWorldTime() : 0;
	}

	private PlayerData getOwnerData() {
		if (ownerUUID == null || ownerUUID.isEmpty() || CustomNpcs.Server == null || npc.world == null || npc.world.getMinecraftServer() == null) {
			return null;
		}
		return PlayerDataController.instance.getDataFromUsername(
				CustomNpcs.Server == null ? npc.world.getMinecraftServer() : CustomNpcs.Server, ownerUUID);
	}

	public int getRange() {
		if (npc.stats.aggroRange > CustomNpcs.NpcNavRange) {
			return CustomNpcs.NpcNavRange;
		}
		return npc.stats.aggroRange;
	}

	@SuppressWarnings("unused")
	public int getDaysLeft() {
		if (infiniteDays) { return 100; }
		if (daysHired <= 0) { return 0; }
		int daysPassed = (int) Math.floor((double) (getCurrentTime() - hiredTime) / 24000.0d);
		return Math.max(daysHired - daysPassed, 0);
	}

}
