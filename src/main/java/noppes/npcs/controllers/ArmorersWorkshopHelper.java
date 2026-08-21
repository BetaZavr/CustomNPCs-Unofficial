package noppes.npcs.controllers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraftforge.fml.ModList;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.shared.common.util.LogWriter;

public class ArmorersWorkshopHelper {

    public static boolean Enabled = ModList.get().isLoaded("armourers_workshop");

    @SuppressWarnings("unchecked")
    public static void onLayerAddEvent(RenderCustomNpc<?, ?> renderer, RenderLayer<?, ?> layer) {
        if (renderer.npcLayers == null) { renderer.npcLayers = new ArrayList<>(); }
        renderer.npcLayers.add(layer);
    }

    @SuppressWarnings("unchecked")
    public static void onLayerRemoveEvent(RenderCustomNpc<?, ?> renderer, RenderLayer<?, ?> layer) {
        List<? extends RenderLayer<?, ?>> layers = renderer.npcLayers;
        if (layers != null) {
            for (int i = 0; i < layers.size(); ++i) {
                if (layers.get(i) == layer) { layers.remove(i--); }
            }
        }
    }

    public static void register() {
        if (!Enabled) return;
        try {
            Class<?> addEvent = Class.forName("moe.plushie.armourers_workshop.api.event.client.AddRendererLayerEvent");
            Method addEventGetRenderer = addEvent.getMethod("getRenderer");
            Method addEventGetLayer = addEvent.getMethod("getLayer");

            Class<?> removeEvent = Class.forName("moe.plushie.armourers_workshop.api.event.client.RemoveRendererLayerEvent");
            Method removeEventGetRenderer = removeEvent.getMethod("getRenderer");
            Method removeEventGetLayer = removeEvent.getMethod("getLayer");

            Class<?> eventBus = Class.forName("moe.plushie.armourers_workshop.api.event.EventBus");
            Method eventBusRegister = eventBus.getMethod("register", Class.class, Consumer.class);

            eventBusRegister.invoke(eventBus, addEvent, (Consumer<Object>) event -> {
                try {
                    Object renderer = addEventGetRenderer.invoke(event);
                    Object layer = addEventGetLayer.invoke(event);
                    onLayerAddEvent((RenderCustomNpc<?, ?>) renderer, (RenderLayer<?, ?>) layer);
                }
                catch (Exception e) { LogWriter.error(e); }
            });

            eventBusRegister.invoke(eventBus, removeEvent, (Consumer<Object>) event -> {
                try {
                    Object renderer = removeEventGetRenderer.invoke(event);
                    Object layer = removeEventGetLayer.invoke(event);
                    onLayerRemoveEvent((RenderCustomNpc<?, ?>) renderer, (RenderLayer<?, ?>) layer);
                }
                catch (Exception e) { LogWriter.error(e); }
            });
        }
        catch (Exception e) { LogWriter.error(e); }
    }

}
