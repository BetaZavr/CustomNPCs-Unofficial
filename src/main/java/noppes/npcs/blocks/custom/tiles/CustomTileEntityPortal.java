package noppes.npcs.blocks.custom.tiles;

import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.blocks.custom.CustomBlockPortal;
import noppes.npcs.controllers.DimensionController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import java.util.Objects;

public class CustomTileEntityPortal extends TheEndPortalBlockEntity {

    public @Nonnull Availability availability = new Availability();
    protected ResourceLocation SKY_TEXTURE;
    protected ResourceLocation PORTAL_TEXTURE;
    protected float alpha = 0.0f;
    public BlockPos posTp = new BlockPos(0, -1, 0);
    public BlockPos posHomeTp = new BlockPos(0, -1, 0);
    public ResourceKey<Level> dimensionId = Level.END;
    public ResourceKey<Level> homeDimensionId = Level.OVERWORLD;
    public int type;

    public CustomTileEntityPortal(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        super(CustomBlocks.tile_custom_portal, pos, state);
        type = state.getValue(CustomBlockPortal.TYPE);
        if (state.getBlock() instanceof CustomBlockPortal portal) {
            SKY_TEXTURE = new ResourceLocation(CustomNpcs.MODID, "textures/environment/" + portal.getCustomName() + "_sky.png");
            PORTAL_TEXTURE = new ResourceLocation(CustomNpcs.MODID, "textures/entity/" + portal.getCustomName() + "_portal.png");
            if (portal.getCustomNbt().has("RenderData", 10) && portal.getCustomNbt().getCompound("RenderData").has("Transparency", 5)) {
                alpha = ValueUtil.correctFloat(portal.getCustomNbt().getCompound("RenderData").getFloat("Transparency"), 0.15f, 1.0f);
            }
        }
    }

    public @Nonnull ResourceLocation getPortalTexture() {
        if (PORTAL_TEXTURE == null && level != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof CustomBlockPortal portal) {
                PORTAL_TEXTURE = new ResourceLocation(CustomNpcs.MODID, "textures/entity/" + portal.getCustomName() + "_portal.png");
            }
        }
        return PORTAL_TEXTURE != null ? PORTAL_TEXTURE : TheEndPortalRenderer.END_PORTAL_LOCATION;
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

    public @Nonnull ResourceLocation getSkyTexture() {
        if (SKY_TEXTURE == null && level != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof CustomBlockPortal portal) {
                SKY_TEXTURE = new ResourceLocation(CustomNpcs.MODID, "textures/environment/" + portal.getCustomName() + "_sky.png");
            }
        }
        return SKY_TEXTURE != null ? SKY_TEXTURE : TheEndPortalRenderer.END_SKY_LOCATION;
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
        if (alpha == 0.0f && level != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof CustomBlockPortal portal &&
                    portal.getCustomNbt().has("RenderData", 10) &&
                    portal.getCustomNbt().getCompound("RenderData").has("Transparency", 5)) {
                setAlpha(portal.getCustomNbt().getCompound("RenderData").getFloat("Transparency"));
            }
        }
        if (alpha == 0.0f) { setAlpha(0.75f); }
        return alpha;
    }

    public void setAlpha(float transparency) {
        alpha = ValueUtil.correctFloat(transparency, 0.15f, 1.0f);
    }

    public BlockPos getPosTp(boolean getHome) {
        BlockPos pos = null;
        ServerLevel sLevel = null;
        MinecraftServer server = level != null ? level.getServer() : CustomNpcs.Server;
        if (getHome) {
            if (hasDimension(server, homeDimensionId)) {
                pos = new BlockPos(posHomeTp);
                if (server != null) { sLevel = server.getLevel(homeDimensionId); }
            }
        }
        else if (hasDimension(server, dimensionId)) {
            pos = new BlockPos(posTp);
            if (server != null) { sLevel = server.getLevel(dimensionId); }
        }
        if (pos == null) { pos  = new BlockPos(0, -1, 0); }
        if (pos.getY() < 0 && sLevel != null) { pos = new BlockPos(sLevel.getSharedSpawnPos()); }
        if (pos.getY() < 0) { pos.above(70 - pos.getY()); }
        return NoppesUtilServer.getSafeTpPos(sLevel, pos, 253, 1);
    }

    private boolean hasDimension(MinecraftServer server, ResourceKey<Level> dimensionId) {
        return dimensionId != null && ((server != null && server.getLevel(dimensionId) != null) || DimensionController.has(dimensionId));
    }

    @Override
    public boolean shouldRenderFace(@Nonnull Direction facing) {
        return switch (type) {
            case 1 -> facing == Direction.SOUTH || facing == Direction.NORTH;
            case 2 -> facing == Direction.WEST || facing == Direction.EAST;
            default -> facing == Direction.UP || facing == Direction.DOWN;
        };
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) { handleUpdateTag(Objects.requireNonNull(pkt.getTag())); }

    @Override
    public void handleUpdateTag(CompoundTag compound) { readDisplay(compound); }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public @Nonnull CompoundTag getUpdateTag() {
        CompoundTag compound = writeDisplay(new CompoundTag());
        compound.putInt("x", worldPosition.getX());
        compound.putInt("y", worldPosition.getY());
        compound.putInt("z", worldPosition.getZ());
        return compound;
    }

    @Override
    public void load(@Nonnull CompoundTag compound) {
        super.load(compound);
        readDisplay(compound);
        posHomeTp = BlockPos.of(compound.getLong("HomePosition"));
        posTp = BlockPos.of(compound.getLong("TpPosition"));
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag compound) {
        super.saveAdditional(compound);
        writeDisplay(compound);
        compound.putLong("HomePosition", posHomeTp.asLong());
        compound.putLong("TpPosition", posTp.asLong());
    }

    public void readDisplay(CompoundTag compound) {
        dimensionId = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(compound.getString("DimensionID")));
        homeDimensionId = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(compound.getString("HomeDimensionID")));
        if (compound.contains("TexturePortal")) { PORTAL_TEXTURE = new ResourceLocation(compound.getString("TexturePortal")); }
        if (compound.contains("TextureSky")) { SKY_TEXTURE = new ResourceLocation(compound.getString("TextureSky")); }
        type = compound.getInt("Type");
        setAlpha(compound.getFloat("Alpha"));
        availability.load(compound.getCompound("Availability"));
    }

    private CompoundTag writeDisplay(CompoundTag compound) {
        compound.putString("DimensionID", dimensionId.location().toString());
        compound.putString("HomeDimensionID", homeDimensionId.location().toString());
        if (PORTAL_TEXTURE != null) { compound.putString("TexturePortal", PORTAL_TEXTURE.toString()); }
        if (SKY_TEXTURE != null) { compound.putString("TextureSky", SKY_TEXTURE.toString()); }
        compound.putInt("Type", type);
        compound.putFloat("Alpha", alpha);
        compound.put("Availability", availability.save(new CompoundTag()));
        return compound;
    }

}
