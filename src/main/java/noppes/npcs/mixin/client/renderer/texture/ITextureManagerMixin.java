package noppes.npcs.mixin.client.renderer.texture;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = TextureManager.class, priority = 502)
public interface ITextureManagerMixin {

    @Accessor Map<ResourceLocation, AbstractTexture> getByPath();

}
