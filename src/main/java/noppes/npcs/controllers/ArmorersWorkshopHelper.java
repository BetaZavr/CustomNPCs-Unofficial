package noppes.npcs.controllers;

import java.util.ArrayList;
import java.util.List;

import moe.plushie.armourers_workshop.api.ArmourersWorkshopApi;
import moe.plushie.armourers_workshop.api.event.EventBus;
import moe.plushie.armourers_workshop.api.event.client.AddRendererLayerEvent;
import moe.plushie.armourers_workshop.api.event.client.RemoveRendererLayerEvent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.shared.common.util.LogWriter;

public class ArmorersWorkshopHelper {

    @SuppressWarnings({"unused", "unchecked"})
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
        if (ArmourersWorkshopApi.isAvailable()) {
            try {
                EventBus.register(AddRendererLayerEvent.class,
                        event -> onLayerRemoveEvent((RenderCustomNpc<?, ?>) event.getRenderer(), (RenderLayer<?, ?>) event.getLayer()));
                EventBus.register(RemoveRendererLayerEvent.class,
                        event -> onLayerRemoveEvent((RenderCustomNpc<?, ?>) event.getRenderer(), (RenderLayer<?, ?>) event.getLayer()));
            }
            catch (Exception e) { LogWriter.error(e); }
        }
    }

}
