package noppes.npcs.blocks.custom.tiles;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.blocks.custom.CustomBlockPortal;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomTileEntityPortal extends TileEntityEndPortal {

	public @Nonnull Availability availability = new Availability();
	protected ResourceLocation SKY_TEXTURE;
	protected ResourceLocation PORTAL_TEXTURE;
	protected float alpha = 0.0f;
	public int dimensionId = -1;
	public int homeDimensionId = 0;
	public int type = 0;

	public BlockPos posTp = new BlockPos(0, -1, 0);
	public BlockPos posHomeTp = new BlockPos(0, -1, 0);

	public ResourceLocation getPortalTexture() {
		if (PORTAL_TEXTURE == null && world != null) {
			IBlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof CustomBlockPortal) {
				PORTAL_TEXTURE = new ResourceLocation(CustomNpcs.MODID, "textures/entity/"
						+ ((CustomBlockPortal) state.getBlock()).getCustomName() + "_portal.png");
			}
		}
		if (PORTAL_TEXTURE == null) { PORTAL_TEXTURE = new ResourceLocation("textures/entity/end_portal.png"); }
		return PORTAL_TEXTURE;
	}

	public void setPortalTexture(String location) {
		if (location == null || location.isEmpty()) {
			PORTAL_TEXTURE = null;
			getPortalTexture();
		}
		else {
			PORTAL_TEXTURE = new ResourceLocation(NoppesUtilServer.validLocation(location));
		}
	}

	public ResourceLocation getSkyTexture() {
		if (SKY_TEXTURE == null && world != null) {
			IBlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof CustomBlockPortal) {
				SKY_TEXTURE = new ResourceLocation(CustomNpcs.MODID, "textures/environment/"
						+ ((CustomBlockPortal) state.getBlock()).getCustomName() + "_sky.png");
			}
		}
		if (SKY_TEXTURE == null) { SKY_TEXTURE = new ResourceLocation("textures/environment/end_sky.png"); }
		return SKY_TEXTURE;
	}

	public void setSkyTexture(String location) {
		if (location == null || location.isEmpty()) {
			SKY_TEXTURE = null;
			getSkyTexture();
		}
		else {
			SKY_TEXTURE = new ResourceLocation(NoppesUtilServer.validLocation(location));
		}
	}

	public float getAlpha() {
		if (alpha == 0.0f && world != null) {
			IBlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof CustomBlockPortal && ((CustomBlockPortal) state.getBlock()).getCustomNbt().has("RenderData", 10) &&
					((CustomBlockPortal) state.getBlock()).getCustomNbt().getCompound("RenderData").has("Transparency", 5)) {
				setAlpha(((CustomBlockPortal) state.getBlock()).getCustomNbt().getCompound("RenderData").getFloat("Transparency"));
			}
		}
		if (alpha == 0.0f) { setAlpha(0.75f); }
		return alpha;
	}

	public void setAlpha(float transparency) {
		alpha = ValueUtil.correctFloat(transparency, 0.15f, 1.0f);
	}

	public BlockPos getPosTp(boolean isHome) {
		BlockPos pos = null;
		WorldServer sLevel = null;
		MinecraftServer server = world != null ? world.getMinecraftServer() : CustomNpcs.Server;
		if (isHome) {
			if (DimensionManager.isDimensionRegistered(homeDimensionId)) {
				pos = new BlockPos(posHomeTp);
				if (server != null) { sLevel = server.getWorld(homeDimensionId); }
			}
		}
		else if (DimensionManager.isDimensionRegistered(dimensionId)) {
			pos = new BlockPos(posTp);
			if (server != null) { sLevel = server.getWorld(dimensionId); }
		}
		if (pos == null) { pos  = new BlockPos(0, -1, 0); }
		if (pos.getY() < 0 && sLevel != null) {
			if (sLevel.getSpawnCoordinate() != null) { pos = new BlockPos(sLevel.getSpawnCoordinate()); }
			else { pos = new BlockPos(sLevel.getSpawnPoint()); }
		}
		if (pos.getY() < 0) { pos.up(70 - pos.getY()); }
		return NoppesUtilServer.getSafeTpPos(sLevel, pos, 253, 1);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldRenderFace(@Nonnull EnumFacing facing) {
		switch (type) {
			case 1: return facing == EnumFacing.SOUTH || facing == EnumFacing.NORTH;
			case 2: return facing == EnumFacing.WEST || facing == EnumFacing.EAST;
			default: return facing == EnumFacing.UP || facing == EnumFacing.DOWN;
		}
	}

	@Override
	@Nonnull
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	@Nullable
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 0, writeDisplay(new NBTTagCompound()));
	}

	@Override
	public void onDataPacket(@Nonnull NetworkManager net, @Nonnull SPacketUpdateTileEntity pkt) {
		readDisplay(pkt.getNbtCompound());
	}

	@Override
	public void readFromNBT(@Nonnull NBTTagCompound compound) {
		super.readFromNBT(compound);
		readDisplay(compound);
		if (compound.hasKey("HomePosition", 11)) {
			int[] p = compound.getIntArray("HomePosition");
			if (p.length >= 3) { posHomeTp = new BlockPos(p[0], p[1], p[2]); }
		}
		else { posHomeTp = BlockPos.fromLong(compound.getLong("HomePosition")); }
		if (compound.hasKey("TpPosition", 11)) {
			int[] p = compound.getIntArray("TpPosition");
			if (p.length >= 3) { posTp = new BlockPos(p[0], p[1], p[2]); }
		}
		else { posTp = BlockPos.fromLong(compound.getLong("TpPosition")); }
	}

	@Override
	public @Nonnull NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
		super.writeToNBT(compound);
		writeDisplay(compound);
		compound.setLong("HomePosition", posHomeTp.toLong());
		compound.setLong("TpPosition", posTp.toLong());
		return compound;
	}

	private void readDisplay(NBTTagCompound compound) {
		dimensionId = compound.getInteger("DimensionID");
		homeDimensionId = compound.getInteger("HomeDimensionID");
		if (compound.hasKey("TexturePortal")) { PORTAL_TEXTURE = new ResourceLocation(compound.getString("TexturePortal")); }
		if (compound.hasKey("TextureSky")) { SKY_TEXTURE = new ResourceLocation(compound.getString("TextureSky")); }
		type = compound.getInteger("Type");
		setAlpha(compound.getFloat("Alpha"));
		availability.load(compound.getCompoundTag("Availability"));
	}

	private NBTTagCompound writeDisplay(NBTTagCompound compound) {
		compound.setInteger("DimensionID", dimensionId);
		compound.setInteger("HomeDimensionID", homeDimensionId);
		if (PORTAL_TEXTURE != null) { compound.setString("TexturePortal", PORTAL_TEXTURE.toString()); }
		if (SKY_TEXTURE != null) { compound.setString("TextureSky", SKY_TEXTURE.toString()); }
		compound.setInteger("Type", type);
		compound.setFloat("Alpha", alpha);
		compound.setTag("Availability", availability.save(new NBTTagCompound()));
		return compound;
	}

	public void setBlock(@Nonnull CustomBlockPortal block) { blockType = block; }

}
