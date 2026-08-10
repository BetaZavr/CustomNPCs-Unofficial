package noppes.npcs.mixin.minecraftforge.eventbus;

import net.minecraftforge.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.lang.reflect.Method;

@Mixin(value = EventBus.class, priority = 502, remap = false)
public interface IEventBusMixin {

    @Invoker("register") void invokeRegister(Class<?> eventType, Object target, Method method);

}