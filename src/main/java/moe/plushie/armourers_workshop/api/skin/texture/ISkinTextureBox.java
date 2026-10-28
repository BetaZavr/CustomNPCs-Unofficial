package moe.plushie.armourers_workshop.api.skin.texture;

import moe.plushie.armourers_workshop.api.core.utils.IDirection;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface ISkinTextureBox {

    @Nullable ISkinTexturePos getTexture(IDirection var1);

}

