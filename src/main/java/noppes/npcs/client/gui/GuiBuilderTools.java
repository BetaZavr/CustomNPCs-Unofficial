package noppes.npcs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.AABB;
import noppes.npcs.containers.ContainerBuilderSettings;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketSetBuildData;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.GuiBasicContainer;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.ITextChangeListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.BuilderData;
import noppes.npcs.util.CustomNPCsScheduler;

import javax.annotation.Nonnull;
import java.awt.*;

public class GuiBuilderTools extends GuiBasicContainer<ContainerBuilderSettings>
        implements ITextfieldListener, ITextChangeListener {

    protected static final ResourceLocation inventory = getResource("baseinventory.png");
    protected final ContainerBuilderSettings container;
    protected final BuilderData builder;
    protected int maxRange = 10;

    public GuiBuilderTools(ContainerBuilderSettings cont, Inventory inv, Component titleIn) {
        super(cont, inv, titleIn);
        setBackground("bgfilled.png");
        closeOnEsc = true;
        imageWidth = 228;
        imageHeight = 216;

        container = cont;
        builder = cont.builderData;
        GuiBuilderSchematic.reloadFiles();
    }

    @Override
    public void buttonEvent(@Nonnull GuiButtonNop button) {
        switch (button.id) {
            case 1: builder.facing = button.getValue(); break;
            case 2: builder.region[0] = button.getValue() + 1; break;
            case 3: builder.region[1] = button.getValue() + 1; break;
            case 4: builder.region[2] = button.getValue() + 1; break;
            case 5: builder.addAir = ((GuiCheckBoxNop) button).selected(); break;
            case 6: builder.replaceAir = ((GuiCheckBoxNop) button).selected(); break;
            case 7: builder.isSolid = ((GuiCheckBoxNop) button).selected(); break;
        }
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(guiLeft, guiTop, 1.0f);
        // Region
        if (builder != null) {
            // place
            int lineColor = new Color(0xFF808080).getRGB();
            graphics.fill(140, 92, 200, 130, new Color(0xFF404040).getRGB());
            graphics.fill(141, 93, 199, 129, new Color(0xFF606060).getRGB());
            graphics.blit(inventory, 140, 120, 0, 80, 16, 10); // eye
            // Borders
            graphics.hLine(4, 170, 132, lineColor);
            graphics.vLine(170, 131, 212, lineColor);
            if (builder.getType() == 2) {
                graphics.hLine(58, 112, 108, lineColor);
                graphics.vLine(58, 108, 132, lineColor);
            }
            matrixStack.translate(7.0f, 135.0f, 0.0f);
            graphics.blit(inventory, 0, 0, 0, 0, 162, 76); // player inventory
            matrixStack.translate(0.0f, -119.0f, 0.0f);
            // Slots
            for (int i = 1; i < 10; i++) { graphics.blit(GuiBasic.RESOURCE_SLOT, (i / 6) * 54, ((i < 6 ? 0 : -5) + i - 1) * 24, 0, 0, 18, 18); } // main
            if (builder.getType() == 2) { graphics.blit(GuiBasic.RESOURCE_SLOT, 54, 96, 0, 0, 18, 18); }
            graphics.hLine(-3, 106, -2, lineColor);
            graphics.vLine(106, -13, 117, lineColor);
            matrixStack.popPose();

            // Show Region
            float r = 1.0f, g = 0.0f, b = 0.0f;
            if (builder.getType() == 1) {
                r = 0.0f;
                g = 1.0f;
                b = 1.0f;
            } else if (builder.getType() == 2) {
                g = 0.0f;
                b = 1.0f;
            }
            float size = (float) builder.region[2] + (float) (builder.region[0] + builder.region[1]) / 2.0f;
            float scale = size <= 0.0f ? 7.0f : 36.0f / size;

            matrixStack.pushPose();
            RenderSystem.enableBlend();
            matrixStack.translate(guiLeft + 170, guiTop + 111, 100.0f);
            matrixStack.scale(scale, scale, scale);
            matrixStack.mulPose(Axis.YP.rotationDegrees(45.0f));
            matrixStack.mulPose(Axis.XP.rotationDegrees(30.0f));
            matrixStack.mulPose(Axis.ZP.rotationDegrees(30.0f));
            LevelRenderer.renderLineBox(matrixStack, graphics.bufferSource().getBuffer(RenderType.lines()),
                    new AABB(-0.5d, -0.5d, -0.5d, 0.5d, 0.5d, 0.5d),
                    1.0f, 1.0f, 1.0f, 1.0f);
            if (builder.facing == 0) { matrixStack.translate(0.0f, 0.0f, 1.01f); }
            else if (builder.facing == 2) { matrixStack.translate(0.0f, 0.0f, -1.1f); }
            LevelRenderer.renderLineBox(matrixStack, graphics.bufferSource().getBuffer(RenderType.lines()),
                    new AABB(-0.5d * (double) builder.region[0],
                            -0.5d * (double) builder.region[1], -0.5d * (double) builder.region[2],
                            0.5d * (double) builder.region[0], 0.5d * (double) builder.region[1],
                            0.5d * (double) builder.region[2]),
                    r, g, b, 1.0f);
            matrixStack.popPose();
        }
        matrixStack.popPose();
        super.renderBg(graphics, partialTicks, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = findSlot(mouseX, mouseY);
        if (slot != null && slot.index >= 36) {
            int id = slot.index - (builder.getType() == 2 ? 36 : 35);
            GuiTextFieldNop textField = getTextField(id);
            if (textField != null) {
                CustomNPCsScheduler.runTack(() -> {
                    if (!slot.hasItem()) { textField.setValue(""); }
                    else {
                        if (!builder.chances.containsKey(id)) { builder.chances.put(id, 100); }
                        textField.setValue("" + builder.chances.get(id));
                    }
                }, 150);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void init() {
        super.init();
        if (builder == null) { return; }
        maxRange = PlayerData.get(player).game.op ? 100 : 10;
        int type = builder.getType();
        int y = guiTop + 4;
        if (builder.getID() > -1) {
            addLabel(1, guiLeft + 120, y, "ID:" + builder.getID());
            y += 12;
        }
        addLabel(0, guiLeft + 4, guiTop + 4, Component.translatable("gui.help.block").append(" [?]:"))
                .setHoverTexts("builder.hover.blocks." + type);
        addLabel(3, guiLeft + 120, y, Component.translatable("gui.area").append(" [?]:"))
                .setHoverTexts("builder.hover.type." + type);
        y += 12;
        for (int i = 0; i < 3; i++) { // Region
            String hover;
            if (i == 0) { hover = "scale.width"; }
            else if (i == 1) { hover = "scale.depth"; }
            else { hover = "schematic.height"; }
            addTextField(i + 10, guiLeft + 120 + i * 34, y, 30, 15, "" + builder.region[i])
                    .setMinMaxDefault(1, maxRange, builder.region[i])
                    .setHoverTexts(hover);
        }
        addButton(1, guiLeft + 120, y += 18, true, builder.facing, "builder.fasing.0", "builder.fasing.1", "builder.fasing.2")
                .setSize(99, 20)
                .setHoverTexts("builder.hover.fasing");
        double[] vs = new double[10];
        double total = 0;
        for (int i = 1; i < 10; i++) {
            if (menu.getSlot(i + (type == 2 ? 36 : 35)).hasItem()) {
                if (!builder.chances.containsKey(i)) { builder.chances.put(i, 100); }
                total += builder.chances.get(i);
            }
            else { vs[i] = 0.0d; }
        }
        if (builder.addAir) { total += 100; }
        for (int i = 1; i < 10; i++) {
            if (menu.getSlot(i + (type == 2 ? 36 : 35)).hasItem() && builder.chances.containsKey(i)) {
                vs[i] = Math.round(builder.chances.get(i) / total * 1000.d) / 10.d;
            }
        }
        boolean bo;
        for (int i = 1; i < 10; i++) { // Blocks
            bo = menu.getSlot(i + (type == 2 ? 36 : 35)).hasItem();
            addTextField(i, guiLeft + 28 + (i / 6) * 54, guiTop + 17 + ((i < 6 ? 0 : -5) + i - 1) * 24, 28, 15,
                    "" + (builder.chances.containsKey(i) && bo ? builder.chances.get(i) : ""))
                    .setMinMaxDefault(1, 100, builder.chances.getOrDefault(i, 100))
                    .setIsEnabled(bo)
                    .setHoverTexts(Component.translatable("builder.hover.chance." + type, ((char) 167) + "a" + vs[i]));
        }
        if (type < 3) {
            addCheckBox(5, guiLeft + 120, y + 22, "block.minecraft.air", null, builder.addAir)
                    .setSize(99, 15)
                    .setHoverTexts("schematic.air");
            if (type == 2) {
                addCheckBox(6, guiLeft + 172, guiTop + 145, "drop.type.all", null, builder.replaceAir)
                        .setSize(70, 15)
                        .setHoverTexts("schematic.replace");
            }
        }
        if (type == 2) {
            addLabel(4, guiLeft + 88, guiTop + 116, "_[?]_")
                    .setHoverTexts("builder.hover.main.block");
        }
    }

    @Override
    public void save() {
        if (builder != null) { Packets.sendServer(new SPacketSetBuildData(builder.getNbt())); }
    }

    @Override
    public void unFocused(GuiTextFieldNop textField) {
        if (builder == null) { return; }
        if (textField.id < 10) {
            if (textField.getValue().isEmpty() || builder.inv.getItem(textField.id).isEmpty()) { textField.setValue(""); }
            else { builder.chances.put(textField.id, textField.getInteger()); }
        }
        else {
            int pos = textField.id - 10;
            int value = textField.getInteger();
            if (value > maxRange) { value = maxRange; }
            if (value <= 0) { value = 1; }
            builder.region[pos] = value;
            init();
        }
    }

    @Override
    public void textUpdate(IComponentGui component, String text) {
        int type = builder.getType();
        double[] vs = new double[10];
        double total = 0;
        boolean bo;
        for (int i = 1; i < 10; i++) {
            if (menu.getSlot(i + (type == 2 ? 36 : 35)).hasItem() && builder.chances.containsKey(i)) { total += builder.chances.get(i); }
            else { vs[i] = 0.0d; }
        }
        if (builder.addAir) { total += 100; }
        for (int i = 1; i < 10; i++) {
            GuiTextFieldNop textField = getTextField(i);
            if (textField != null && menu.getSlot(i + (type == 2 ? 36 : 35)).hasItem()) {
                vs[i] = Math.round(textField.getInteger() / total * 1000.d) / 10.d;
            }
        }
        for (int i = 1; i < 10; i++) { // Blocks
            bo = menu.getSlot(i + (type == 2 ? 36 : 35)).hasItem();
            GuiTextFieldNop textField = getTextField(i);
            if (textField != null) {
                textField.setIsEnabled(bo)
                        .setHoverTexts(Component.translatable("builder.hover.chance." + type, ((char) 167) + "a" + vs[i]));
            }
        }
    }

}
