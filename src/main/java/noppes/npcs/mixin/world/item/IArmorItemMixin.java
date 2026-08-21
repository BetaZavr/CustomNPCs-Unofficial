package noppes.npcs.mixin.world.item;

import net.minecraft.world.item.ArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.throwables.MixinException;

import java.util.EnumMap;
import java.util.UUID;

@Mixin(value = ArmorItem.class, priority = 502)
public interface IArmorItemMixin {

    @Accessor("ARMOR_MODIFIER_UUID_PER_TYPE") static EnumMap<ArmorItem.Type, UUID> getArmorModifiers() { throw new MixinException("Mixin did not initialize properly."); }

    @Accessor("defense") int defense();

    @Accessor("toughness") float toughness();

    @Accessor void setDefense(int maxStDam);

    @Accessor void setToughness(float newToughness);

    @Accessor void setKnockbackResistance(float newKnockbackResistance);

}
