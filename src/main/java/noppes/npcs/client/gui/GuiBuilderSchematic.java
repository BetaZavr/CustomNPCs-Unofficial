package noppes.npcs.client.gui;

import net.minecraft.init.Blocks;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ClientEventHandler;
import noppes.npcs.controllers.SyncController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketSetBuildData;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.schematics.Schematic;
import noppes.npcs.schematics.SchematicWrapper;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.BuilderData;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

public class GuiBuilderSchematic extends GuiBasic implements ICustomScrollListener, ITextfieldListener {

	public static final Map<String, SchematicWrapper> baseFiles = new TreeMap<>();
	public static final Map<String, SchematicWrapper> files = new TreeMap<>();
	protected final BuilderData builder;
	protected GuiCustomScrollNop schematics;
	protected int maxRange = 10;

	public static void reloadFiles() {
		baseFiles.clear();
		files.clear();
		// base in mod:
		for (String name : SchematicController.included) {
			SchematicWrapper schema = SchematicController.Instance.load(name);
			if (CustomNpcs.proxy.getPlayerData(null).game.op || schema.size <= CustomNpcs.MaxBuilderBlocks) {
				baseFiles.put(name, schema);
			}
		}
		files.putAll(baseFiles);
		File schematicDir = SchematicController.getDir();
		if (schematicDir != null && schematicDir.exists()) {
			for (File f : Objects.requireNonNull(schematicDir.listFiles())) {
				if (f.isFile() && (f.getName().endsWith(".schematic"))) {
					try {
						InputStream stream = Files.newInputStream(f.toPath());
						NBTTagCompound compound = CompressedStreamTools.readCompressed(stream);
						stream.close();
						Schematic sch = new Schematic(f.getName());
						sch.load(compound);
						SchematicWrapper schema = new SchematicWrapper(sch);
						if (CustomNpcs.proxy.getPlayerData(null).game.op || schema.size <= CustomNpcs.MaxBuilderBlocks) {
							files.put(f.getName(), schema);
						}
					} catch (Exception e) { LogWriter.error(e); }
				}
			}
		}
	}

	public GuiBuilderSchematic(int buildId, int type) {
		super();
		setBackground("bgfilled.png");
		closeOnEsc = true;
		imageWidth = 228;
		imageHeight = 216;

		reloadFiles();
		BuilderData base = SyncController.dataBuilder.get(buildId);
		if (base != null) { builder = base; }
		else { builder = new BuilderData(buildId, type); }
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		switch (button.id) {
			case 5: builder.addAir = ((GuiCheckBoxNop) button).selected(); break;
			case 6: builder.replaceAir = ((GuiCheckBoxNop) button).selected(); break;
			case 7: builder.isSolid = ((GuiCheckBoxNop) button).selected(); break;
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (builder != null) {
			int lineColor = new Color(0xFF808080).getRGB();
			// Borders
			drawHorizontalLine(guiLeft + 118, guiLeft + 223, guiTop + 36, lineColor);
			drawVerticalLine(guiLeft + 117, guiTop + 3, guiTop + 212, lineColor);
			if (files.containsKey(schematics.getSelected())) {
				SchematicWrapper schem = files.get(schematics.getSelected());
				GlStateManager.pushMatrix();
				GlStateManager.translate(guiLeft + imageWidth, guiTop + 26.0f, 0.0f);
				// background
				minecraft.getTextureManager().bindTexture(background);
				drawTexturedModalRect(0, 0, 172, 0, 84, 80);
				drawTexturedModalRect(0, 80, 172, 213, 84, 4);
				// schem
				int w = schem.schema.getWidth();
				int l = schem.schema.getLength();
				int h = schem.schema.getHeight();
				float sW = (float) (w + l) * (float) Math.cos(Math.toRadians(30));
				float sH = (float) (w + l) * (float) Math.sin(Math.toRadians(30)) + (float) h;
				float scale;
				if (sW > sH) { scale = 84.0f / sW; } else { scale = 84.0f / sH; }

				GL11.glEnable(GL11.GL_SCISSOR_TEST);
				ScaledResolution sw = new ScaledResolution(Minecraft.getMinecraft());
				double d4 = sw.getScaledWidth() < mc.displayWidth
						? (int) Math.round((double) mc.displayWidth / (double) sw.getScaledWidth())
						: 1;
				GL11.glScissor((int) ((double) (guiLeft + imageWidth + 2) * d4),
						(int) ((double) mc.displayHeight - (double) (guiTop + 106) * d4),
						Math.max(0, (int) ((double) (imageWidth + 78) * d4)),
						Math.max(0, (int) ((double) (guiTop + 30) * d4)));
				GlStateManager.translate(42.0f - (w / 2.0f) * scale, 41.0f + (h / 2.0f) * scale, 150.0f);
				GlStateManager.scale(scale, -scale, -scale);

				GlStateManager.pushMatrix();
				GlStateManager.translate(w / 2.0f, h / 2.0f, l / 2.0f);
				float f0 = (minecraft.player.world.getTotalWorldTime() % 360.0f) * 2.0f;
				GlStateManager.rotate(30.0f, 1.0f, 0.0f, 0.0f);
				GlStateManager.rotate(f0, 0.0f, 1.0f, 0.0f);
				GlStateManager.translate(-w / 2.0f, -h / 2.0f, -l / 2.0f);

				ClientEventHandler.renderSchem(schem, 0, 0, 0, 0);
				GlStateManager.popMatrix();

				GL11.glDisable(GL11.GL_SCISSOR_TEST);
				GlStateManager.popMatrix();
			}
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		if (builder == null) { return; }
		int type = builder.getType();
		if (builder.getID() > -1) { addLabel(1, guiLeft + 120, guiTop + 4, "ID:" + builder.getID()); }
		maxRange = CustomNpcs.proxy.getPlayerData(null).game.op ? 100 : 10;
		if (schematics == null) { schematics = addScroll(0).setSize(110, 197); }
		schematics.setList(new ArrayList<>(GuiBuilderSchematic.files.keySet()));
		if (!builder.schematicName.isEmpty()) {
			int i = 0;
			for (String key : schematics.getList()) {
				String fName = key;
				if (key.endsWith(".schematic")) { fName = key.substring(0, key.lastIndexOf(".schematic")); }
				else if (key.endsWith(".blueprint")) { fName = key.substring(0, key.lastIndexOf(".blueprint")); }
				if (fName.equals(builder.schematicName)) {
					schematics.setSelect(i);
					break;
				}
				i++;
			}
			if (i == schematics.getList().size()) { schematics.setSelect(-1); }
		}
		add(schematics.setPos(guiLeft + 5, guiTop + 14));
		addLabel(6, guiLeft + 120, guiTop + 40, Component.translatable("gui.name").append(":"));
		addTextField(10, guiLeft + 120, guiTop + 54, 99, 15, builder.schematicName)
				.setHoverTexts("scale.width");
		addLabel(5, guiLeft + 4, guiTop + 4, Component.translatable("gui.file.list").append(" [?]:"))
				.setHoverTexts("builder.hover.list", "" + maxRange);
		if (type == 3) {
			addCheckBox(5, guiLeft + 120, guiTop + 72, Blocks.AIR.getUnlocalizedName() + ".name", null, builder.addAir)
					.setSize(99, 15)
					.setHoverTexts("schematic.schem.air");
			addCheckBox(6, guiLeft + 120, guiTop + 87, "drop.type.all", null, builder.replaceAir)
					.setSize(70, 15)
					.setHoverTexts("schematic.schem.replace");
			addCheckBox(7, guiLeft + 120, guiTop + 102,  "gui.solid", null, builder.isSolid)
					.setSize(70, 15)
					.setHoverTexts("schematic.schem.solid");
		}
	}

	@Override
	public void save() {
		if (builder != null) { Packets.sendServer(new SPacketSetBuildData(builder.getNbt())); }
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		// File List
		builder.schematicName = scroll.getSelected();
		if (builder.schematicName.endsWith(".schematic")) { builder.schematicName = builder.schematicName.substring(0, builder.schematicName.lastIndexOf(".schematic")); }
		else if (builder.schematicName.endsWith(".blueprint")) { builder.schematicName = builder.schematicName.substring(0, builder.schematicName.lastIndexOf(".blueprint")); }
		SchematicWrapper schema = SchematicController.Instance.map.get(builder.schematicName + ".schematic");
		if (schema != null) {
			builder.region[0] = schema.schema.getLength();
			builder.region[1] = schema.schema.getWidth();
			builder.region[2] = schema.schema.getHeight();
		}
		GuiTextFieldNop textField = getTextField(10);
		if (textField != null) { textField.setValue(builder.schematicName); }
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (builder != null) {
			builder.schematicName = textField.getValue();
			initGui();
		}
	}

}
