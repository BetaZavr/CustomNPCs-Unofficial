package moe.plushie.armourers_workshop.api.skin.serializer;

import java.io.InputStream;

@SuppressWarnings("unused")
public interface ISkinFileProvider {

    InputStream loadSkin(String var1) throws Exception;

}

