package noppes.npcs.fluids;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class CustomFluidType extends FluidType implements ICustomElement {

    protected final @Nonnull CompoundTag nbtData;
    protected final @Nonnull ResourceLocation stillTexture;
    protected final @Nonnull ResourceLocation flowingTexture;
    protected final @Nonnull ResourceLocation overlayTexture;
    protected final @Nonnull Vector3f fogColor;
    protected final int tintColor;

    public CustomFluidType(@Nonnull ResourceLocation stillTextureIn,
                           @Nonnull ResourceLocation flowingTextureIn,
                           @Nonnull ResourceLocation overlayTextureIn,
                           int tintColorIn,
                           @Nonnull Vector3f fogColorIn,
                           @Nonnull Properties properties,
                           @Nonnull CompoundTag nbtBlock) {
        super(properties);
        nbtData = nbtBlock;
        stillTexture = stillTextureIn;
        flowingTexture = flowingTextureIn;
        overlayTexture = overlayTextureIn;
        fogColor = fogColorIn;
        tintColor = tintColorIn;
    }

    @SuppressWarnings("unused")
    public @Nonnull ResourceLocation getStillTexture() { return stillTexture; }

    @SuppressWarnings("unused")
    public @Nonnull ResourceLocation getFlowingTexture() { return flowingTexture; }

    public int getTintColor() { return tintColor; }

    public @Nonnull ResourceLocation getOverlayTexture() { return overlayTexture; }

    public @Nonnull Vector3f getFogColor() { return fogColor; }

    @Override
    public String getCustomName() { return nbtData.getString("RegistryName"); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() {
        if (nbtData.contains("BlockType", 1)) { return nbtData.getByte("BlockType"); }
        return 1;
    }

    @Override
    public boolean showInCreative() { return !nbtData.contains("ShowInCreative", 1) || nbtData.getBoolean("ShowInCreative"); }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() { return stillTexture; }

            @Override
            public ResourceLocation getFlowingTexture() { return flowingTexture; }

            @Override
            public @Nonnull ResourceLocation getOverlayTexture() { return overlayTexture; }

            @Override
            public int getTintColor() { return tintColor; }

            @Override
            public @Nonnull Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return fogColor;
            }

            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick,
                                        float nearDistance, float farDistance, FogShape shape) {
                RenderSystem.setShaderFogStart(1f);
                RenderSystem.setShaderFogEnd(6f); // distance when the fog starts
            }
        });
    }
}
