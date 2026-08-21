package noppes.npcs.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MouseHandler.class, priority = 502)
public interface IMouseHandlerMixin {

    @Accessor int getActiveButton();

    @Accessor("xpos") void setX(double newX);

    @Accessor("ypos") void setY(double newY);

    @Accessor("mouseGrabbed") void setGrabbed(boolean newGrabbed);

}
