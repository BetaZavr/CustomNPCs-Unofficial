package moe.plushie.armourers_workshop.api.event;

import moe.plushie.armourers_workshop.api.registry.IEventHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public final class EventBus {

    public static <E> void register(Class<E> eventClass, Consumer<E> eventHandler) {
        try {
            Class<?> clazz = Class.forName("moe.plushie.armourers_workshop.init.platform.EventManager");
            clazz.getDeclaredMethod("listen", Class.class, Consumer.class)
                    .invoke(null, eventClass, eventHandler);
        }
        catch (Exception ignored) { }
    }

    public static <E> void post(Class<E> eventClass, E event) {
        try {
            Class<?> clazz = Class.forName("moe.plushie.armourers_workshop.init.platform.EventManager");
            for (Method method : clazz.getDeclaredMethods()) {
                Parameter[] parameters = method.getParameters();
                if (parameters.length == 2 &&
                        parameters[0].getType() == Class.class &&
                        parameters[1].getType() != IEventHandler.class) {
                    method.invoke(null, eventClass, event);
                    break;
                }
            }
        }
        catch (Exception ignored) { }
    }

    public static void init() { }

}

