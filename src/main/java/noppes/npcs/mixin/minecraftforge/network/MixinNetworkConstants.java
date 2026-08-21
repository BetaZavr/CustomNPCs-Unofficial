package noppes.npcs.mixin.minecraftforge.network;

import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.network.simple.SimpleChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.throwables.MixinException;

@Mixin(value = {NetworkConstants.class}, priority = 502, remap = false)
public interface MixinNetworkConstants {

    @Accessor static SimpleChannel getPlayChannel() { throw new MixinException("Mixin no loaded"); }

}
