package noppes.npcs.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyMappingLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.throwables.MixinException;

import java.util.Map;
import java.util.Set;

@Mixin(value = KeyMapping.class, priority = 502)
public interface IKeyMappingMixin {

    @Accessor("ALL") static Map<String, KeyMapping> getAll() { throw new MixinException("Mixin did not initialize properly."); }

    @Accessor("MAP") static KeyMappingLookup getMap() { throw new MixinException("Mixin did not initialize properly."); }

    @Accessor("CATEGORIES") static Set<String> getCategories() { throw new MixinException("Mixin did not initialize properly."); }

    @Accessor String getName();

    @Accessor InputConstants.Key getKey();

    @Accessor void setKey(InputConstants.Key newKey);

    @Accessor void setCategory(String newCategory);

    @Accessor void setName(String newName);

    @Accessor void setDefaultKey(InputConstants.Key newKey);

}
