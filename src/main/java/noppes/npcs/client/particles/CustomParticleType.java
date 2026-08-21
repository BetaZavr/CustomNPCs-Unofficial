package noppes.npcs.client.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.wrapper.NBTWrapper;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class CustomParticleType extends ParticleType<CustomParticleType> implements ParticleOptions, ICustomElement {

    @SuppressWarnings("deprecation")
    private static final ParticleOptions.Deserializer<CustomParticleType> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        public @Nonnull CustomParticleType fromCommand(@Nonnull ParticleType<CustomParticleType> p_123846_, @Nonnull StringReader p_123847_) {
            return (CustomParticleType) p_123846_;
        }

        public @Nonnull CustomParticleType fromNetwork(@Nonnull ParticleType<CustomParticleType> p_123849_, @Nonnull FriendlyByteBuf buf) {
            return (CustomParticleType) p_123849_;
        }
    };
    private final Codec<CustomParticleType> codec = Codec.unit(this::getType);

    public final @Nonnull CompoundTag nbtData;

    public CustomParticleType(boolean overrideLimiter, @Nonnull CompoundTag nbtParticle) {
        super(overrideLimiter, DESERIALIZER);
        nbtData = nbtParticle;
    }

    @Override
    public @Nonnull CustomParticleType getType() { return this; }

    @Override
    public @Nonnull Codec<CustomParticleType> codec() { return codec; }

    @Override
    public void writeToNetwork(@Nonnull FriendlyByteBuf buf) {}

    @Override
    public @Nonnull String writeToString() {
        @Nullable ResourceLocation location = ForgeRegistries.PARTICLE_TYPES.getKey(this);
        return location != null ? location.toString() : "custom_particle";
    }

    @Override
    public String getCustomName() { return nbtData.getString("RegistryName").toLowerCase(); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() { return nbtData.contains("OBJModel", 8) && !nbtData.getString("OBJModel").isEmpty() ? 1 : 0; }

    @Override
    public boolean showInCreative() { return false; }

}
