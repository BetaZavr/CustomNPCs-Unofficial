package noppes.npcs.mixin.client.renderer.texture;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = TextureAtlas.class, priority = 502)
public interface ITextureAtlasMixin {

    @Accessor Map<ResourceLocation, TextureAtlasSprite> getTexturesByName();

}
