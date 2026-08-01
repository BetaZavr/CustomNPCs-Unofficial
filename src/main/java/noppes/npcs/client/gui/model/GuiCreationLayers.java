package noppes.npcs.client.gui.model;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.mixin.client.renderer.entity.IRenderLivingBaseMixin;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.containers.ContainerLayer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

import javax.annotation.Nonnull;
import java.util.*;

public class GuiCreationLayers  extends GuiCreationScreenInterface<ContainerLayer> implements ICustomScrollListener {

    private GuiCustomScrollNop scroll;

    public GuiCreationLayers(EntityNPCInterface npc, ContainerLayer container) {
        super(npc, container);
        closeOnEsc = true;
        active = 6;
        xOffset = 60;
    }

    @Override
    public void buttonEvent(@Nonnull GuiButtonNop button) {
        super.buttonEvent(button);
    }

    @Override
    public void initGui() {
        GuiTextFieldNop.unfocus();
        super.initGui();
        int x = guiLeft;
        int y = guiTop + 48;
        int h = 152;
        if (getButton(2) == null && getButton(3) == null) {
            y -= 22;
            h += 22;
        }
        if (scroll == null) { scroll = addScroll(1, true); }
        addLabel(20, x, y, "part.layers.info.1")
                .setColor(CustomNpcs.MainColor.getRGB());
        Render<Entity> render = mc.getRenderManager().getEntityRenderObject(npc);
        Map<Component, List<Component>> map = new LinkedHashMap<>();
        Map<Component, Component> sfx = new HashMap<>();
        if (render instanceof RenderCustomNpc) {
            for (LayerRenderer<?> layer : ((RenderCustomNpc<?>) render).getLayers()) {
                Component name = Component.literal(layer.getClass().getSimpleName());
                if (!name.getString().isEmpty()) {
                    map.put(name, Collections.singletonList(Component.empty()
                            .append(Component.literal("Layer from ").withStyle(TextFormatting.GRAY))
                            .append(Component.literal("RenderCustomNpc").withStyle(TextFormatting.GREEN))
                            .append(Component.literal("; in mod: ").withStyle(TextFormatting.GRAY))
                            .append(Component.literal("CustomNpc").withStyle(TextFormatting.GOLD))
                    ));
                    sfx.put(name, Component.literal("CN").withStyle(TextFormatting.GOLD));
                }
            }
        }
        EntityLivingBase customModel = playerdata.getEntity(npc);
        if (customModel != null) {
            render = mc.getRenderManager().getEntityRenderObject(customModel);
            if (render instanceof RenderLivingBase) {
                for (LayerRenderer<?> layer : ((IRenderLivingBaseMixin) render).npcs$getLayers()) {
                    Component name = Component.literal(layer.getClass().getSimpleName());
                    if (name.getString().isEmpty()) { continue; }
                    if (!map.containsKey(name)) {
                        String modName = "";
                        for (ModContainer mod : Loader.instance().getActiveModList()) {
                            if (mod.getOwnedPackages().contains(render.getClass().getName())) {
                                modName = mod.getModId();
                                break;
                            }
                        }
                        Component nameMod;
                        if (modName.isEmpty()) {
                            nameMod = Component.literal("in ").append(Component.literal("Minecraft").withStyle(TextFormatting.YELLOW));
                            sfx.put(name, Component.literal("MC").withStyle(TextFormatting.YELLOW));
                        } else {
                            nameMod = Component.literal("in mod: ").append(Component.literal(modName).withStyle(TextFormatting.GREEN));
                            sfx.put(name, Component.literal(String.valueOf(modName.charAt(0)).toUpperCase()).withStyle(TextFormatting.RED));
                        }
                        map.put(name, Collections.singletonList(Component.empty()
                                .append(Component.literal("Layer from ").withStyle(TextFormatting.GRAY))
                                .append(Component.literal(render.getClass().getSimpleName()).withStyle(TextFormatting.AQUA))
                                .append(Component.literal("; ").withStyle(TextFormatting.GRAY))
                                .append(nameMod)));
                    }
                }
            }
        }
        LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
        List<Component> suffixes = new ArrayList<>();
        int i = 0;
        for (Component key : map.keySet()) {
            hts.put(i++, map.get(key));
            suffixes.add(sfx.get(key));
        }
        y += 12;
        add(scroll.setPos(x, y)
                .setUnsortedList(new ArrayList<>(map.keySet()))
                .setSize(121, h)
                .setSelectedList(new HashSet<>(Arrays.asList(playerdata.getDisableLayers())))
                .setHoverTexts(hts)
                .setSuffixes(suffixes));
        for (Slot slot : inventorySlots.inventorySlots) {
            slot.xPos = -5000;
            slot.yPos = -5000;
        }
    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {
        List<String> disabled = new ArrayList<>();
        for (Component line : scroll.getSelectedList()) { disabled.add(line.getString()); }
        playerdata.setDisableLayers(disabled.toArray(new String[0]));
        initGui();
    }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

}
