package noppes.npcs.mixin.world.item;


import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Item.class, priority = 502)
public interface IItemMixin {

    @Accessor void setMaxDamage(int newMaxDamage);

}
