package noppes.npcs.client.gui.dimensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.dimensions.CustomWorldInfo;
import noppes.npcs.dimensions.DimensionHandler;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketDimensionSettings;

import javax.annotation.Nonnull;
import java.util.Random;

public class GuiCreateDimension extends Screen {

    private final ResourceKey<Level> editingKey;
    private EditBox nameField;
    private EditBox seedField;
    private String gameTypeName = "survival";
    private boolean generateStructures = true;
    private boolean allowCheats = false;
    private String generatorType = "overworld";

    private Button btnGameType;
    private Button btnStructures;
    private Button btnCheats;
    private Button btnGenerator;

    public GuiCreateDimension(ResourceKey<Level> editingKey) {
        super(Component.translatable(editingKey != null ? "dimensions.edit" : "dimensions.create"));
        this.editingKey = editingKey;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2 - 50;

        nameField = new EditBox(this.font, cx - 100, cy, 200, 20, Component.translatable("dimensions.enter.name"));
        nameField.setMaxLength(64);
        seedField = new EditBox(this.font, cx - 100, cy + 35, 200, 20, Component.translatable("selectWorld.enterSeed"));
        seedField.setMaxLength(32);

        if (editingKey != null) {
            CustomWorldInfo info = (CustomWorldInfo) DimensionHandler.getInstance().getMCWorldInfo(editingKey.location().toString());
            if (info != null) {
                nameField.setValue(info.getMCLevelName());
                gameTypeName = info.getMCGameType().getName();
                generateStructures = info.isMCMapFeaturesEnabled();
                allowCheats = info.isMCAllowCommands();
                //generatorType = info.getMCGeneratorType();
                seedField.setValue(String.valueOf(info.getMCSeed()));
            }
        } else {
            nameField.setValue("custom_dimension");
        }
        addWidget(nameField);
        nameField.setFocused(true);
        addWidget(seedField);
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        btnGameType = addRenderableWidget(Button.builder(
                Component.translatable("selectWorld.gameMode").append(": ").append(gameTypeName),
                b -> toggleGameType()).bounds(cx - 100, cy + 65, 200, 20).build());

        btnStructures = addRenderableWidget(Button.builder(
                        Component.translatable("selectWorld.mapFeatures").append(": ").append(generateStructures ? "ON" : "OFF"),
                        b -> { generateStructures = !generateStructures; updateButtons(); })
                .bounds(cx - 100, cy + 90, 200, 20).build());

        btnCheats = addRenderableWidget(Button.builder(
                        Component.translatable("selectWorld.allowCommands").append(": ").append(allowCheats ? "ON" : "OFF"),
                        b -> { allowCheats = !allowCheats; updateButtons(); })
                .bounds(cx - 100, cy + 115, 200, 20).build());

        btnGenerator = addRenderableWidget(Button.builder(
                        Component.literal("Generator: ").append(generatorType),
                        b -> { generatorType = generatorType.equals("overworld") ? "flat" : "overworld"; updateButtons(); })
                .bounds(cx - 100, cy + 140, 200, 20).build());
    }

    private void toggleGameType() {
        switch (gameTypeName) {
            case "survival": gameTypeName = "creative"; break;
            case "creative": gameTypeName = "adventure"; break;
            case "adventure": gameTypeName = "spectator"; break;
            default: gameTypeName = "survival";
        }
        updateButtons();
    }

    private void updateButtons() {
        btnGameType.setMessage(Component.translatable("selectWorld.gameMode").append(": ").append(gameTypeName));
        btnStructures.setMessage(Component.translatable("selectWorld.mapFeatures").append(": ").append(generateStructures ? "ON" : "OFF"));
        btnCheats.setMessage(Component.translatable("selectWorld.allowCommands").append(": ").append(allowCheats ? "ON" : "OFF"));
        btnGenerator.setMessage(Component.literal("Generator: ").append(generatorType));
    }

    private void sendSettings() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) return;

        long seed = new Random().nextLong();
        String s = seedField.getValue().trim();
        if (!s.isEmpty()) {
            try { seed = Long.parseLong(s); } catch (NumberFormatException e) { seed = s.hashCode(); }
        }

        CustomWorldInfo info = new CustomWorldInfo(new CompoundTag());
        info.setMCLevelName(NoppesUtilServer.validPath(name.toLowerCase()));
        info.setMCSeed(seed);
        info.setMCGameType(GameType.byName(gameTypeName));
        info.setMCMapFeaturesEnabled(generateStructures);
        info.setMCAllowCommands(allowCheats);
        //info.setMCGeneratorType(generatorType);

        Packets.sendServer(new SPacketDimensionSettings(info.getMCLevelName(), info.save(), editingKey));
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        this.minecraft.setScreen(null);
    }

    @Override
    public void render(@Nonnull net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("dimensions.enter.name"), nameField.getX(), nameField.getY() - 12, 0xA0A0A0);
        g.drawString(this.font, Component.translatable("selectWorld.enterSeed"), seedField.getX(), seedField.getY() - 12, 0xA0A0A0);
        nameField.render(g, mx, my, pt);
        seedField.render(g, mx, my, pt);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 || key == 335) { sendSettings(); return true; }
        return super.keyPressed(key, scan, mods);
    }

}