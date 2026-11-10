package noppes.npcs.mixin;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import noppes.npcs.CustomNpcs;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name(CustomNpcs.MODID)
public class NpcMixinCore implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() { return new String[0]; }

    @Override
    public String getModContainerClass() { return null; }

    @Nullable
    @Override
    public String getSetupClass() { return null; }

    @Override
    public void injectData(Map<String, Object> data) {
        Mixins.addConfiguration("mixins." + CustomNpcs.MODID + ".json");
    }

    @Override
    public String getAccessTransformerClass() { return null; }

}
