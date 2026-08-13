package noppes.npcs.controllers.data;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.DimensionManager;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.data.role.ITransportLocation;
import noppes.npcs.containers.NpcMiscInventory;

public class TransportLocation implements ITransportLocation {

	public int id = -1;
	public String name = "default name";
	public BlockPos pos;
	public int type = 0;
	public int dimension = 0;
	public TransportCategory category;

	// New from Unofficial (BetaZavr)
	public final NpcMiscInventory inventory = new NpcMiscInventory(9);
	public UUID npc = null;
	public long money = 0;
	public float yaw = 0.0f;
	public float pitch = 0.0f;

	public void load(NBTTagCompound compound) {
		if (compound == null) {
			return;
		}
		id = compound.getInteger("Id");
		pos = new BlockPos(compound.getDouble("PosX"), compound.getDouble("PosY"), compound.getDouble("PosZ"));
		type = compound.getInteger("Type");
		dimension = compound.getInteger("Dimension");
		name = compound.getString("Name");

		// New from Unofficial (BetaZavr)
		inventory.clear();
		if (compound.hasKey("CostInv", 10)) {
			inventory.load(compound.getCompoundTag("CostInv"));
		}
		npc = null;
		if (compound.hasKey("NpcUUIDMost", 4) && compound.hasKey("NpcUUIDLeast", 4)) {
			npc = compound.getUniqueId("NpcUUID");
		}
		money = compound.getLong("Cost");
		yaw = compound.getFloat("PlayerYaw");
		pitch = compound.getFloat("PlayerPitch");
	}

	public NBTTagCompound save() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("Id", id);
		compound.setDouble("PosX", pos.getX());
		compound.setDouble("PosY", pos.getY());
		compound.setDouble("PosZ", pos.getZ());
		compound.setInteger("Type", type);
		compound.setInteger("Dimension", dimension);
		compound.setString("Name", name);

		// New from Unofficial (BetaZavr)
		compound.setTag("CostInv", inventory.save());
		if (npc != null) { compound.setUniqueId("NpcUUID", npc); }
		compound.setLong("Cost", money);
		compound.setFloat("PlayerYaw", yaw);
		compound.setFloat("PlayerPitch", pitch);
		return compound;
	}

	@Override
	public int getDimension() { return dimension; }

	@Override
	public int getId() { return id; }

	@Override
	public String getName() { return name; }

	@Override
	public int getType() { return type; }

	@Override
	public int getX() { return pos.getX(); }

	@Override
	public int getY() { return pos.getY(); }

	@Override
	public int getZ() { return pos.getZ(); }

	public boolean isDefault() { return type == 1; }

	// New from Unofficial (BetaZavr)
	@Override
	public void setPos(int dimensionId, int x, int y, int z) {
		if (DimensionManager.isDimensionRegistered(dimensionId)) {
			dimension = dimensionId;
			pos = new BlockPos(x, y, z);
		}
		else { throw new CustomNPCsException("Unknown dimensionId: " + dimensionId); }
	}

	@Override
	public void setType(int typeIn) {
		if (typeIn < 0 || typeIn > 2) { throw new CustomNPCsException("Unknown location type (0<>2): " + typeIn); }
		type = typeIn;
	}

	public TransportLocation copy() {
		TransportLocation tl = new TransportLocation();
		tl.id = id;
		tl.name = name;
		tl.type = type;
		tl.dimension = dimension;
		tl.npc = npc;
		tl.money = money;
		tl.pos = pos;
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			tl.inventory.setInventorySlotContents(i, inventory.getStackInSlot(i).copy());
		}
		tl.category = category;
		tl.yaw = yaw;
		tl.pitch = yaw;
		return tl;
	}

}
