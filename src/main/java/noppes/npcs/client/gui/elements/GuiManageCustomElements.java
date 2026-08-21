package noppes.npcs.client.gui.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import noppes.npcs.*;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.blocks.custom.CustomCauldronBlock;
import noppes.npcs.client.ClientEventHandler;
import noppes.npcs.client.particles.CustomParticleType;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.fluids.CustomFluid;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main manager GUI for creating and editing custom elements (Blocks, Items, Particles).
 * Left scroll shows available subtypes for the selected main type.
 * Right scroll shows already existing elements of that main type.
 */
public class GuiManageCustomElements extends GuiBasic implements ICustomScrollListener {

    protected GuiCustomScrollNop scrollSubtypes;
    protected GuiCustomScrollNop scrollElements;

    // 0 = Blocks, 1 = Items, 2 = Particles
    protected int currentType = 0;
    protected final Object[] typeNames = { Component.translatable("soundCategory.block"),
            Component.translatable("stat.itemsButton"),
            Component.translatable("part.particles") };

    // Subtype names per type
    protected final Map<Integer, List<Component>> subtypes = new HashMap<>();
    // Existing element names per type
    protected final Map<Component, ICustomElement> existingElements = new HashMap<>();
    // obj particle
    protected ParameterizedModel objParticle = null;

    public GuiManageCustomElements() {
        super();
        setBackground("menubg.png");
        imageWidth = 252;
        imageHeight = 240;
        title = Component.translatable("gui.manage.custom.elements");

        // Initialize subtype lists for each main type
        Component simple = Component.translatable("elements.type.simple");
        subtypes.put(0, Arrays.asList(simple,
                Component.translatable("elements.type.block.liquid"),
                Component.translatable("elements.type.block.chest"),
                Component.translatable("elements.type.block.stairs"),
                Component.translatable("elements.type.block.slab"),
                Component.translatable("elements.type.block.portal"),
                Component.translatable("elements.type.block.door")));
        subtypes.put(1, Arrays.asList(simple,
                Component.translatable("elements.type.item.weapon"),
                Component.translatable("elements.type.item.tool"),
                Component.translatable("elements.type.item.armor"),
                Component.translatable("elements.type.item.shield"),
                Component.translatable("elements.type.item.bow"),
                Component.translatable("elements.type.item.food"),
                Component.translatable("elements.type.item.potion"),
                Component.translatable("elements.type.item.fishing.rod")));
        subtypes.put(2, Arrays.asList(simple,
                Component.translatable("elements.type.particle.obj")));
    }

    @Override
    public void init() {
        super.init();
        int x0 = guiLeft + 5;
        int x1 = x0 + 122;
        int y = guiTop + 17;
        // Main type selector button (top center)
        addLabel(0, x0, y, "gui.manage.types")
                .setSize(120, 10);
        addLabel(1, x1, y + 18, "gui.manage.elements")
                .setSize(92, 10);
        addButton(0, x0, y += 12, true, currentType, typeNames)
                .setSize(120, 16)
                .layerColor = currentType == 1 ? 0xFFBEE72E : currentType == 2 ? 0xFFE72E97 : 0xFF2EA8E7;
        y += 18;
        // Left scroll: subtypes (creation variants)
        if (scrollSubtypes == null) {
            scrollSubtypes = addScroll(0, false)
                    .disabledSearch();
        }
        add(scrollSubtypes.setSize(120, 166)
                .setUnsortedList(new ArrayList<>(subtypes.get(currentType)))
                .setPos(x0, y));
        if (!scrollSubtypes.hasSelected()) { scrollSubtypes.setSelectedIndex(0); }
        // Right scroll: existing elements
        if (scrollElements == null) {
            scrollElements = addScroll(1, false)
                    .disabledSearch();
            refreshElementsList();
            scrollElements.type = currentType;
        }
        if (scrollElements.type != currentType) {
            refreshElementsList();
            scrollElements.type = currentType;
        }
        add(scrollElements.setSize(120, 166)
                .setPos(x1, y));
        if (!scrollElements.hasSelected()) {scrollElements.setSelectedIndex(0); }
        y += scrollSubtypes.height + 2;
        // Bottom buttons
        addButton(1, x1, y, "gui.add").setSize(59, 20);
        addButton(2, x1 + 61, y, "gui.edit").setSize(59, 20);
        addButton(3, x0, y, "display.hover.X").setSize(59, 20);
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        switch (button.id) {
            case 0: {
                currentType = button.getValue();
                init();
                break;
            } // Cycle main type (Blocks -> Items -> Particles)
            case 1: openEditor(true); break; // create new element
            case 2: openEditor(false); break;  // edit existing element
            case 3: onClose(); break;
            default: break;
        }
    }

    private void openEditor(boolean isNew) {
        if (scrollSubtypes.hasSelected()) {
            // TODO: open creation GUI for selectedSubtype under currentType
            switch (currentType) {
                case 1: {

                    break;
                } // item
                case 2: {

                    break;
                } // particle
                default:  {

                    break;
                } // block
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        drawPreviewSlot(graphics, partialTicks);
    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {
        // Selection changes are handled directly by the scroll component
        refreshElementsList();
        init();
    }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
        // Double-clicking an existing element triggers edit
        if (scroll.id == 1 && scroll.hasSelected()) {
            buttonEvent(getButton(2));
        }
    }

    /**
     * Draws the slot background and the preview of the hovered/selected element inside it.
     */
    private void drawPreviewSlot(GuiGraphics graphics, float partialTicks) {
        int slotS = 28;
        int slotX = scrollElements.getX() + scrollElements.getWidth() - slotS + 2;
        int slotY = scrollElements.getY() - slotS - 2;
        // Draw slot background
        graphics.blit(RESOURCE_SLOT, slotX, slotY, 0, 0, 16, 16);
        graphics.blit(RESOURCE_SLOT, slotX, slotY + 16, 0, 8, 16, 10);
        graphics.blit(RESOURCE_SLOT, slotX + 16, slotY, 8, 0, 10, 16);
        graphics.blit(RESOURCE_SLOT, slotX + 16, slotY + 16, 8, 8, 10, 10);
        if (scrollElements != null) {
            ICustomElement select;
            int hover = scrollElements.getHover();
            if (scrollElements.getHover() < 0) { select = existingElements.get(scrollElements.getNormalSelected()); }
            else { select = existingElements.get(scrollElements.getNormalList().get(hover)); }
            if (select != null) {
                if (select instanceof Block block) {
                    renderBlockInSlot(graphics, block, slotX + slotS / 2, slotY + slotS / 2, partialTicks);
                }
                else if (select instanceof CustomParticleType particle) {
                    renderParticleInSlot(graphics, particle, slotX, slotY);
                }
                else if (select instanceof Item) {
                    PoseStack matrixStack = graphics.pose();
                    ItemStack stack = new ItemStack((Item) select);
                    matrixStack.pushPose();
                    matrixStack.translate(slotX + 12.5f, slotY + 12.5f, 50.0f);
                    matrixStack.mulPose(Axis.XP.rotationDegrees(-30.0f));
                    long time = 5000L;
                    matrixStack.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() % time) * 360.0f / time));
                    matrixStack.scale(16.0f, -16.0f, 16.0f);
                    graphics.renderItem(stack, 0, 0, 0);
                    matrixStack.popPose();
                }
            }
        }
    }

    /**
     * Renders a block inside the preview field using the same matrix setup as SubGuiEditAnimation.
     */
    private void renderBlockInSlot(GuiGraphics graphics, Block block, int centerX, int centerY, float partialTicks) {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        PoseStack matrixStack = graphics.pose();
        BlockState state = block.defaultBlockState();

        matrixStack.pushPose();
        matrixStack.translate(centerX - 1.25f, centerY - 1.0f, 50.0f);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-30.0f));
        long time = 5000L;
        matrixStack.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() % time) * 360.0f / time));
        matrixStack.scale(-16.0f, -16.0f, -16.0f);
        // Center the block on its origin
        matrixStack.translate(-0.5f, -0.5f, -0.5f);
        if (block instanceof DoorBlock) {
            matrixStack.translate(0.0f, -0.5f, -0.4f);
        }
        ClientEventHandler.renderBlock(minecraft.level, state, player.blockPosition(), matrixStack,
                graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, partialTicks);
        matrixStack.popPose();
    }

    /**
     * Renders a particle preview inside the field.
     * For textured particles: draws the texture scaled to fit.
     * For OBJ particles: renders the model rotated.
     */
    private void renderParticleInSlot(GuiGraphics graphics, CustomParticleType particle, int x, int y) {
        PoseStack matrixStack = graphics.pose();
        String name = NoppesUtilServer.validPath(particle.nbtData.contains("Texture", 8) ?
                particle.nbtData.getString("Texture") :
                particle.nbtData.getString("RegistryName"));
        ResourceLocation texture = new ResourceLocation(CustomNpcs.MODID, "textures/particle/" + name + ".png");
        if (particle.nbtData.contains("OBJModel", 8)) {
            ResourceLocation obj = new ResourceLocation(CustomNpcs.MODID, "models/particle/" + particle.nbtData.getString("OBJModel") + ".obj");
            if (objParticle == null || !objParticle.modelLocation.equals(obj)) {
                objParticle = ModelBuffer.getParameterizedModel(obj, null, null, false, 0);
            }
            if (objParticle != null) {
                float particleScale = particle.nbtData.getFloat("Scale") * 26.0f;
                matrixStack.pushPose();
                matrixStack.translate(x + 13.0f, y + 13.0f, 50.0f);
                matrixStack.mulPose(Axis.XP.rotationDegrees(30.0f));
                long time = 5000L;
                matrixStack.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() % time) * 360.0f / time));
                matrixStack.scale(-particleScale, -particleScale, -particleScale);
                ModelBuffer.render(objParticle, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                matrixStack.popPose();
                texture = null;
            }
        }
        if (texture != null) {
            matrixStack.pushPose();
            matrixStack.translate(x, y, 0.0f);
            matrixStack.scale(0.1015325f, 0.1015325f, 1.0f);
            graphics.blit(texture, 0, 0, 0, 0, 256, 256);
            matrixStack.popPose();
        }
    }

    /**
     * Refills the right scroll with currently loaded custom elements based on currentType.
     */
    private void refreshElementsList() {
        existingElements.clear();
        switch (currentType) {
            case 0: {
                for (ICustomElement element : CustomBlocks.customblocks.keySet()) {
                    if (element instanceof Block &&
                            !(element instanceof CustomCauldronBlock) &&
                            element.getElementType() == scrollSubtypes.getSelectedIndex() ) {
                        addElement(element);
                    }
                }
                if (scrollSubtypes.getSelectedIndex() == 1) {
                    for (ICustomElement element : CustomBlocks.customfluid.values()) {
                        if (element instanceof CustomFluid fluid && fluid.getBlock() != null) { addElement(fluid.getBlock()); }
                    }
                }
                break;
            } // Blocks
            case 1: {
                for (ICustomElement element : CustomItems.customitems) {
                    if (element instanceof Item && element.getElementType() == scrollSubtypes.getSelectedIndex()) { addElement(element); }
                }
                break;
            } // Items
            case 2: {
                for (ParticleType<CustomParticleType> particleType : CustomParticleTypes.CUSTOMS.values()) {
                    if (particleType instanceof ICustomElement element &&
                            element.getElementType() == scrollSubtypes.getSelectedIndex()) { addElement(element); }
                }
                break;
            } // Particles
            default: break;
        }
        if (scrollElements != null) { scrollElements.setNormalList(new ArrayList<>(existingElements.keySet())); }
    }

    private void addElement(ICustomElement element) {
        boolean has = false;
        String name = element.getCustomName();
        for (Component c : existingElements.keySet()) {
            if (c.getString().equals(name)) {
                has = true;
                break;
            }
        }
        if (!has) { existingElements.put(Component.literal(name), element); }
    }

}