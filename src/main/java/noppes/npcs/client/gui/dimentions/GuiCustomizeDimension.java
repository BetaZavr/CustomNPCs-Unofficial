package noppes.npcs.client.gui.dimentions;

import java.io.IOException;
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.primitives.Floats;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListButton;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class GuiCustomizeDimension extends GuiScreen
		implements GuiSlider.FormatHelper, GuiPageButtonList.GuiResponder {

	private final GuiCreateDimension parent;
	protected String title = "Customize Dimension Settings";
	protected String subtitle = "Page 1 of 3";
	protected String pageTitle = "Basic Settings";
	protected String[] pageNames = new String[4];
	private GuiPageButtonList list;
	private GuiButton done;
	private GuiButton randomize;
	private GuiButton defaults;
	private GuiButton previousPage;
	private GuiButton nextPage;
	private GuiButton confirm;
	private GuiButton cancel;
	private GuiButton presets;
	private boolean settingsModified = false;
	private int confirmMode = 0;
	private boolean confirmDismissed = false;
	private final Predicate<String> numberFilter = new Predicate<String>() {

		@Override
		public boolean apply(String value) { return tryParseValidFloat(value); }

		public boolean tryParseValidFloat(String value) {
			Float f = null;
			try { f = Float.parseFloat(value); }
			catch (Exception ignored) { }
			return value.isEmpty() || f != null && Floats.isFinite(f) && f >= 0.0F;
		}

	};
	private final ChunkGeneratorSettings.Factory defaultSettings = new ChunkGeneratorSettings.Factory();
	private ChunkGeneratorSettings.Factory settings;
	/** A Random instance for this world customization */
	private final Random random = new Random();

	public GuiCustomizeDimension(GuiScreen gui, String chunkSettings) {
		parent = (GuiCreateDimension) gui;
		loadValues(chunkSettings);
	}

	@Override
	protected void actionPerformed(@Nonnull GuiButton button) throws IOException {
		if (button.enabled) {
			switch (button.id) {
			case 300:
				parent.chunkProviderSettingsJson = settings.toString();
				mc.displayGuiScreen(parent);
				break;
			case 301:
				for (int i = 0; i < list.getSize(); ++i) {
					GuiPageButtonList.GuiEntry guientry = list.getListEntry(i);
					Gui gui = guientry.getComponent1();
					if (gui instanceof GuiButton) {
						GuiButton guibutton = (GuiButton) gui;
						if (guibutton instanceof GuiSlider) {
							float f = ((GuiSlider) guibutton).getSliderPosition()
									* (0.75F + random.nextFloat() * 0.5F)
									+ (random.nextFloat() * 0.1F - 0.05F);
							((GuiSlider) guibutton).setSliderPosition(MathHelper.clamp(f, 0.0F, 1.0F));
						}
						else if (guibutton instanceof GuiListButton) { ((GuiListButton) guibutton).setValue(random.nextBoolean()); }
					}
					Gui gui1 = guientry.getComponent2();
					if (gui1 instanceof GuiButton) {
						GuiButton guibutton = (GuiButton) gui1;
						if (guibutton instanceof GuiSlider) {
							float f1 = ((GuiSlider) guibutton).getSliderPosition()
									* (0.75F + random.nextFloat() * 0.5F)
									+ (random.nextFloat() * 0.1F - 0.05F);
							((GuiSlider) guibutton).setSliderPosition(MathHelper.clamp(f1, 0.0F, 1.0F));
						}
						else if (guibutton instanceof GuiListButton) { ((GuiListButton) guibutton).setValue(random.nextBoolean()); }
					}
				}

				return;
			case 302:
				list.previousPage();
				updatePageControls();
				break;
			case 303:
				list.nextPage();
				updatePageControls();
				break;
			case 304:
				if (settingsModified) { enterConfirmation(); }
				break;
			case 305:
				mc.displayGuiScreen(new GuiScreenCustomizeDimensionPresets(this));
				break;
			case 306:
				exitConfirmation();
				break;
			case 307:
				confirmMode = 0;
				exitConfirmation();
			}
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		list.drawScreen(mouseX, mouseY, partialTicks);
		drawCenteredString(fontRenderer, title, width / 2, 2, 0xFFFFFF);
		drawCenteredString(fontRenderer, subtitle, width / 2, 12, 0xFFFFFF);
		drawCenteredString(fontRenderer, pageTitle, width / 2, 22, 0xFFFFFF);
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (confirmMode != 0) {
			drawRect(0, 0, width, height, Integer.MIN_VALUE);
			drawHorizontalLine(width / 2 - 91, width / 2 + 90, 99, 0xFFE0E0E0);
			drawHorizontalLine(width / 2 - 91, width / 2 + 90, 185, 0xFFA0A0A0);
			drawVerticalLine(width / 2 - 91, 99, 185, 0xFFE0E0E0);
			drawVerticalLine(width / 2 + 90, 99, 185, 0xFFA0A0A0);
			GlStateManager.disableLighting();
			GlStateManager.disableFog();
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder vertexBuffer = tessellator.getBuffer();
			mc.getTextureManager().bindTexture(OPTIONS_BACKGROUND);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			vertexBuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			vertexBuffer.pos(width / 2.0D - 90.0D, 185.0D, 0.0D).tex(0.0D, 2.65625D).color(64, 64, 64, 64).endVertex();
			vertexBuffer.pos(width / 2.0D + 90.0D, 185.0D, 0.0D).tex(5.625D, 2.65625D).color(64, 64, 64, 64).endVertex();
			vertexBuffer.pos(width / 2.0D + 90.0D, 100.0D, 0.0D).tex(5.625D, 0.0D).color(64, 64, 64, 64).endVertex();
			vertexBuffer.pos(width / 2.0D - 90.0D, 100.0D, 0.0D).tex(0.0D, 0.0D).color(64, 64, 64, 64).endVertex();
			tessellator.draw();
			drawCenteredString(fontRenderer,
					new TextComponentTranslation("createWorld.customize.custom.confirmTitle").getFormattedText(),
					width / 2, 105, 0xFFFFFF);
			drawCenteredString(fontRenderer,
					new TextComponentTranslation("createWorld.customize.custom.confirm1").getFormattedText(),
					width / 2, 125, 0xFFFFFF);
			drawCenteredString(fontRenderer,
					new TextComponentTranslation("createWorld.customize.custom.confirm2").getFormattedText(),
					width / 2, 135, 0xFFFFFF);
			confirm.drawButton(mc, mouseX, mouseY, partialTicks);
			cancel.drawButton(mc, mouseX, mouseY, partialTicks);
		}
	}

	private void enterConfirmation() {
		confirmMode = 304;
		setConfirmationControls(true);
	}

	public String saveValues() {
		return settings.toString().replace("\n", "");
	}

	public void loadValues(String chunkSettings) {
		if (chunkSettings != null && !chunkSettings.isEmpty()) { settings = ChunkGeneratorSettings.Factory.jsonToFactory(chunkSettings); }
		else { settings = new ChunkGeneratorSettings.Factory(); }
	}

	private void createPagedList() {
		GuiPageButtonList.GuiListEntry[] guiListentry = new GuiPageButtonList.GuiListEntry[] {
				new GuiPageButtonList.GuiSlideEntry(160,
						new TextComponentTranslation("createWorld.customize.custom.seaLevel").getFormattedText(), true,
						this, 1.0F, 255.0F, settings.seaLevel),
				new GuiPageButtonList.GuiButtonEntry(148,
						new TextComponentTranslation("createWorld.customize.custom.useCaves").getFormattedText(), true,
						settings.useCaves),
				new GuiPageButtonList.GuiButtonEntry(150,
						new TextComponentTranslation("createWorld.customize.custom.useStrongholds").getFormattedText(),
						true, settings.useStrongholds),
				new GuiPageButtonList.GuiButtonEntry(151,
						new TextComponentTranslation("createWorld.customize.custom.useVillages").getFormattedText(),
						true, settings.useVillages),
				new GuiPageButtonList.GuiButtonEntry(152,
						new TextComponentTranslation("createWorld.customize.custom.useMineShafts").getFormattedText(),
						true, settings.useMineShafts),
				new GuiPageButtonList.GuiButtonEntry(153,
						new TextComponentTranslation("createWorld.customize.custom.useTemples").getFormattedText(),
						true, settings.useTemples),
				new GuiPageButtonList.GuiButtonEntry(210,
						new TextComponentTranslation("createWorld.customize.custom.useMonuments").getFormattedText(),
						true, settings.useMonuments),
				new GuiPageButtonList.GuiButtonEntry(154,
						new TextComponentTranslation("createWorld.customize.custom.useRavines").getFormattedText(),
						true, settings.useRavines),
				new GuiPageButtonList.GuiButtonEntry(149,
						new TextComponentTranslation("createWorld.customize.custom.useDungeons").getFormattedText(),
						true, settings.useDungeons),
				new GuiPageButtonList.GuiSlideEntry(157,
						new TextComponentTranslation("createWorld.customize.custom.dungeonChance").getFormattedText(),
						true, this, 1.0F, 100.0F, settings.dungeonChance),
				new GuiPageButtonList.GuiButtonEntry(155,
						new TextComponentTranslation("createWorld.customize.custom.useWaterLakes").getFormattedText(),
						true, settings.useWaterLakes),
				new GuiPageButtonList.GuiSlideEntry(158,
						new TextComponentTranslation("createWorld.customize.custom.waterLakeChance").getFormattedText(),
						true, this, 1.0F, 100.0F, settings.waterLakeChance),
				new GuiPageButtonList.GuiButtonEntry(156,
						new TextComponentTranslation("createWorld.customize.custom.useLavaLakes").getFormattedText(),
						true, settings.useLavaLakes),
				new GuiPageButtonList.GuiSlideEntry(159,
						new TextComponentTranslation("createWorld.customize.custom.lavaLakeChance").getFormattedText(),
						true, this, 10.0F, 100.0F, settings.lavaLakeChance),
				new GuiPageButtonList.GuiButtonEntry(161,
						new TextComponentTranslation("createWorld.customize.custom.useLavaOceans").getFormattedText(),
						true, settings.useLavaOceans),
				new GuiPageButtonList.GuiSlideEntry(162,
						new TextComponentTranslation("createWorld.customize.custom.fixedBiome").getFormattedText(),
						true, this, -1.0F, 37.0F, settings.fixedBiome),
				new GuiPageButtonList.GuiSlideEntry(163,
						new TextComponentTranslation("createWorld.customize.custom.biomeSize").getFormattedText(), true,
						this, 1.0F, 8.0F, settings.biomeSize),
				new GuiPageButtonList.GuiSlideEntry(164,
						new TextComponentTranslation("createWorld.customize.custom.riverSize").getFormattedText(), true,
						this, 1.0F, 5.0F, settings.riverSize) };
		GuiPageButtonList.GuiListEntry[] guiListentry_2 = new GuiPageButtonList.GuiListEntry[] {
				new GuiPageButtonList.GuiLabelEntry(416,
						new TextComponentTranslation("tile.dirt.name").getFormattedText(), false),
				null,
				new GuiPageButtonList.GuiSlideEntry(165,
						new TextComponentTranslation("createWorld.customize.custom.size").getFormattedText(), false,
						this, 1.0F, 50.0F, settings.dirtSize),
				new GuiPageButtonList.GuiSlideEntry(166,
						new TextComponentTranslation("createWorld.customize.custom.count").getFormattedText(), false,
						this, 0.0F, 40.0F, settings.dirtCount),
				new GuiPageButtonList.GuiSlideEntry(167,
						new TextComponentTranslation("createWorld.customize.custom.minHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.dirtMinHeight),
				new GuiPageButtonList.GuiSlideEntry(168,
						new TextComponentTranslation("createWorld.customize.custom.maxHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.dirtMaxHeight),
				new GuiPageButtonList.GuiLabelEntry(417,
						new TextComponentTranslation("tile.gravel.name").getFormattedText(), false),
				null,
				new GuiPageButtonList.GuiSlideEntry(169,
						new TextComponentTranslation("createWorld.customize.custom.size").getFormattedText(), false,
						this, 1.0F, 50.0F, settings.gravelSize),
				new GuiPageButtonList.GuiSlideEntry(170,
						new TextComponentTranslation("createWorld.customize.custom.count").getFormattedText(), false,
						this, 0.0F, 40.0F, settings.gravelCount),
				new GuiPageButtonList.GuiSlideEntry(171,
						new TextComponentTranslation("createWorld.customize.custom.minHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.gravelMinHeight),
				new GuiPageButtonList.GuiSlideEntry(172,
						new TextComponentTranslation("createWorld.customize.custom.maxHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.gravelMaxHeight),
				new GuiPageButtonList.GuiLabelEntry(418,
						new TextComponentTranslation("tile.stone.granite.name").getFormattedText(), false),
				null,
				new GuiPageButtonList.GuiSlideEntry(173,
						new TextComponentTranslation("createWorld.customize.custom.size").getFormattedText(), false,
						this, 1.0F, 50.0F, settings.graniteSize),
				new GuiPageButtonList.GuiSlideEntry(174,
						new TextComponentTranslation("createWorld.customize.custom.count").getFormattedText(), false,
						this, 0.0F, 40.0F, settings.graniteCount),
				new GuiPageButtonList.GuiSlideEntry(175,
						new TextComponentTranslation("createWorld.customize.custom.minHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.graniteMinHeight),
				new GuiPageButtonList.GuiSlideEntry(176,
						new TextComponentTranslation("createWorld.customize.custom.maxHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.graniteMaxHeight),
				new GuiPageButtonList.GuiLabelEntry(419, new TextComponentTranslation("tile.stone.diorite.name").getFormattedText(), false), null,
				new GuiPageButtonList.GuiSlideEntry(177,
						new TextComponentTranslation("createWorld.customize.custom.size").getFormattedText(), false,
						this, 1.0F, 50.0F, settings.dioriteSize),
				new GuiPageButtonList.GuiSlideEntry(178,
						new TextComponentTranslation("createWorld.customize.custom.count").getFormattedText(), false,
						this, 0.0F, 40.0F, settings.dioriteCount),
				new GuiPageButtonList.GuiSlideEntry(179,
						new TextComponentTranslation("createWorld.customize.custom.minHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.dioriteMinHeight),
				new GuiPageButtonList.GuiSlideEntry(180,
						new TextComponentTranslation("createWorld.customize.custom.maxHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.dioriteMaxHeight),
				new GuiPageButtonList.GuiLabelEntry(420,
						new TextComponentTranslation("tile.stone.andesite.name").getFormattedText(), false),
				null,
				new GuiPageButtonList.GuiSlideEntry(181,
						new TextComponentTranslation("createWorld.customize.custom.size").getFormattedText(), false,
						this, 1.0F, 50.0F, settings.andesiteSize),
				new GuiPageButtonList.GuiSlideEntry(182,
						new TextComponentTranslation("createWorld.customize.custom.count").getFormattedText(), false,
						this, 0.0F, 40.0F, settings.andesiteCount),
				new GuiPageButtonList.GuiSlideEntry(183,
						new TextComponentTranslation("createWorld.customize.custom.minHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.andesiteMinHeight),
				new GuiPageButtonList.GuiSlideEntry(184,
						new TextComponentTranslation("createWorld.customize.custom.maxHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.andesiteMaxHeight),
				new GuiPageButtonList.GuiLabelEntry(421,
						new TextComponentTranslation("tile.oreCoal.name").getFormattedText(), false),
				null,
				new GuiPageButtonList.GuiSlideEntry(185,
						new TextComponentTranslation("createWorld.customize.custom.size").getFormattedText(), false,
						this, 1.0F, 50.0F, settings.coalSize),
				new GuiPageButtonList.GuiSlideEntry(186,
						new TextComponentTranslation("createWorld.customize.custom.count").getFormattedText(), false,
						this, 0.0F, 40.0F, settings.coalCount),
				new GuiPageButtonList.GuiSlideEntry(187,
						new TextComponentTranslation("createWorld.customize.custom.minHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.coalMinHeight),
				new GuiPageButtonList.GuiSlideEntry(189,
						new TextComponentTranslation("createWorld.customize.custom.maxHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.coalMaxHeight),
				new GuiPageButtonList.GuiLabelEntry(422,
						new TextComponentTranslation("tile.oreIron.name").getFormattedText(), false),
				null,
				new GuiPageButtonList.GuiSlideEntry(190,
						new TextComponentTranslation("createWorld.customize.custom.size").getFormattedText(), false,
						this, 1.0F, 50.0F, settings.ironSize),
				new GuiPageButtonList.GuiSlideEntry(191,
						new TextComponentTranslation("createWorld.customize.custom.count").getFormattedText(), false,
						this, 0.0F, 40.0F, settings.ironCount),
				new GuiPageButtonList.GuiSlideEntry(192,
						new TextComponentTranslation("createWorld.customize.custom.minHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.ironMinHeight),
				new GuiPageButtonList.GuiSlideEntry(193,
						new TextComponentTranslation("createWorld.customize.custom.maxHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.ironMaxHeight),
				new GuiPageButtonList.GuiLabelEntry(423,
						new TextComponentTranslation("tile.oreGold.name").getFormattedText(), false),
				null,
				new GuiPageButtonList.GuiSlideEntry(194,
						new TextComponentTranslation("createWorld.customize.custom.size").getFormattedText(), false,
						this, 1.0F, 50.0F, settings.goldSize),
				new GuiPageButtonList.GuiSlideEntry(195,
						new TextComponentTranslation("createWorld.customize.custom.count").getFormattedText(), false,
						this, 0.0F, 40.0F, settings.goldCount),
				new GuiPageButtonList.GuiSlideEntry(196,
						new TextComponentTranslation("createWorld.customize.custom.minHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.goldMinHeight),
				new GuiPageButtonList.GuiSlideEntry(197,
						new TextComponentTranslation("createWorld.customize.custom.maxHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.goldMaxHeight),
				new GuiPageButtonList.GuiLabelEntry(424,
						new TextComponentTranslation("tile.oreRedstone.name").getFormattedText(), false),
				null,
				new GuiPageButtonList.GuiSlideEntry(198,
						new TextComponentTranslation("createWorld.customize.custom.size").getFormattedText(), false,
						this, 1.0F, 50.0F, settings.redstoneSize),
				new GuiPageButtonList.GuiSlideEntry(199,
						new TextComponentTranslation("createWorld.customize.custom.count").getFormattedText(), false,
						this, 0.0F, 40.0F, settings.redstoneCount),
				new GuiPageButtonList.GuiSlideEntry(200,
						new TextComponentTranslation("createWorld.customize.custom.minHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.redstoneMinHeight),
				new GuiPageButtonList.GuiSlideEntry(201,
						new TextComponentTranslation("createWorld.customize.custom.maxHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.redstoneMaxHeight),
				new GuiPageButtonList.GuiLabelEntry(425,
						new TextComponentTranslation("tile.oreDiamond.name").getFormattedText(), false),
				null,
				new GuiPageButtonList.GuiSlideEntry(202,
						new TextComponentTranslation("createWorld.customize.custom.size").getFormattedText(), false,
						this, 1.0F, 50.0F, settings.diamondSize),
				new GuiPageButtonList.GuiSlideEntry(203,
						new TextComponentTranslation("createWorld.customize.custom.count").getFormattedText(), false,
						this, 0.0F, 40.0F, settings.diamondCount),
				new GuiPageButtonList.GuiSlideEntry(204,
						new TextComponentTranslation("createWorld.customize.custom.minHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.diamondMinHeight),
				new GuiPageButtonList.GuiSlideEntry(205,
						new TextComponentTranslation("createWorld.customize.custom.maxHeight").getFormattedText(),
						false, this, 0.0F, 255.0F, settings.diamondMaxHeight),
				new GuiPageButtonList.GuiLabelEntry(426,
						new TextComponentTranslation("tile.oreLapis.name").getFormattedText(), false),
				null,
				new GuiPageButtonList.GuiSlideEntry(206,
						new TextComponentTranslation("createWorld.customize.custom.size").getFormattedText(), false,
						this, 1.0F, 50.0F, settings.lapisSize),
				new GuiPageButtonList.GuiSlideEntry(207,
						new TextComponentTranslation("createWorld.customize.custom.count").getFormattedText(), false,
						this, 0.0F, 40.0F, settings.lapisCount),
				new GuiPageButtonList.GuiSlideEntry(208,
						new TextComponentTranslation("createWorld.customize.custom.center").getFormattedText(), false,
						this, 0.0F, 255.0F, settings.lapisCenterHeight),
				new GuiPageButtonList.GuiSlideEntry(209,
						new TextComponentTranslation("createWorld.customize.custom.spread").getFormattedText(), false,
						this, 0.0F, 255.0F, settings.lapisSpread) };
		GuiPageButtonList.GuiListEntry[] guiListentry_3 = new GuiPageButtonList.GuiListEntry[]{
				new GuiPageButtonList.GuiSlideEntry(100,
						new TextComponentTranslation("createWorld.customize.custom.mainNoiseScaleX").getFormattedText(),
						false, this, 1.0F, 5000.0F, settings.mainNoiseScaleX),
				new GuiPageButtonList.GuiSlideEntry(101,
						new TextComponentTranslation("createWorld.customize.custom.mainNoiseScaleY").getFormattedText(),
						false, this, 1.0F, 5000.0F, settings.mainNoiseScaleY),
				new GuiPageButtonList.GuiSlideEntry(102,
						new TextComponentTranslation("createWorld.customize.custom.mainNoiseScaleZ").getFormattedText(),
						false, this, 1.0F, 5000.0F, settings.mainNoiseScaleZ),
				new GuiPageButtonList.GuiSlideEntry(103,
						new TextComponentTranslation("createWorld.customize.custom.depthNoiseScaleX")
								.getFormattedText(),
						false, this, 1.0F, 2000.0F, settings.depthNoiseScaleX),
				new GuiPageButtonList.GuiSlideEntry(104,
						new TextComponentTranslation("createWorld.customize.custom.depthNoiseScaleZ")
								.getFormattedText(),
						false, this, 1.0F, 2000.0F, settings.depthNoiseScaleZ),
				new GuiPageButtonList.GuiSlideEntry(105,
						new TextComponentTranslation("createWorld.customize.custom.depthNoiseScaleExponent")
								.getFormattedText(),
						false, this, 0.01F, 20.0F, settings.depthNoiseScaleExponent),
				new GuiPageButtonList.GuiSlideEntry(106,
						new TextComponentTranslation("createWorld.customize.custom.baseSize").getFormattedText(), false,
						this, 1.0F, 25.0F, settings.baseSize),
				new GuiPageButtonList.GuiSlideEntry(107,
						new TextComponentTranslation("createWorld.customize.custom.coordinateScale").getFormattedText(),
						false, this, 1.0F, 6000.0F, settings.coordinateScale),
				new GuiPageButtonList.GuiSlideEntry(108,
						new TextComponentTranslation("createWorld.customize.custom.heightScale").getFormattedText(),
						false, this, 1.0F, 6000.0F, settings.heightScale),
				new GuiPageButtonList.GuiSlideEntry(109,
						new TextComponentTranslation("createWorld.customize.custom.stretchY").getFormattedText(), false,
						this, 0.01F, 50.0F, settings.stretchY),
				new GuiPageButtonList.GuiSlideEntry(110,
						new TextComponentTranslation("createWorld.customize.custom.upperLimitScale").getFormattedText(),
						false, this, 1.0F, 5000.0F, settings.upperLimitScale),
				new GuiPageButtonList.GuiSlideEntry(111,
						new TextComponentTranslation("createWorld.customize.custom.lowerLimitScale").getFormattedText(),
						false, this, 1.0F, 5000.0F, settings.lowerLimitScale),
				new GuiPageButtonList.GuiSlideEntry(112,
						new TextComponentTranslation("createWorld.customize.custom.biomeDepthWeight")
								.getFormattedText(),
						false, this, 1.0F, 20.0F, settings.biomeDepthWeight),
				new GuiPageButtonList.GuiSlideEntry(113,
						new TextComponentTranslation("createWorld.customize.custom.biomeDepthOffset")
								.getFormattedText(),
						false, this, 0.0F, 20.0F, settings.biomeDepthOffset),
				new GuiPageButtonList.GuiSlideEntry(114,
						new TextComponentTranslation("createWorld.customize.custom.biomeScaleWeight")
								.getFormattedText(),
						false, this, 1.0F, 20.0F, settings.biomeScaleWeight),
				new GuiPageButtonList.GuiSlideEntry(115,
						new TextComponentTranslation("createWorld.customize.custom.biomeScaleOffset")
								.getFormattedText(),
						false, this, 0.0F, 20.0F, settings.biomeScaleOffset)};
		GuiPageButtonList.GuiListEntry[] guiListentry_4 = new GuiPageButtonList.GuiListEntry[] {
				new GuiPageButtonList.GuiLabelEntry(400,
						new TextComponentTranslation("createWorld.customize.custom.mainNoiseScaleX").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(132,
						String.format("%5.3f", settings.mainNoiseScaleX),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(401,
						new TextComponentTranslation("createWorld.customize.custom.mainNoiseScaleY").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(133,
						String.format("%5.3f", settings.mainNoiseScaleY),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(402,
						new TextComponentTranslation("createWorld.customize.custom.mainNoiseScaleZ").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(134,
						String.format("%5.3f", settings.mainNoiseScaleZ),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(403,
						new TextComponentTranslation("createWorld.customize.custom.depthNoiseScaleX").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(135,
						String.format("%5.3f", settings.depthNoiseScaleX),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(404,
						new TextComponentTranslation("createWorld.customize.custom.depthNoiseScaleZ").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(136,
						String.format("%5.3f", settings.depthNoiseScaleZ),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(405,
						new TextComponentTranslation("createWorld.customize.custom.depthNoiseScaleExponent")
								.getFormattedText() + ":",
						false),
				new GuiPageButtonList.EditBoxEntry(137,
						String.format("%2.3f", settings.depthNoiseScaleExponent),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(406,
						new TextComponentTranslation("createWorld.customize.custom.baseSize").getFormattedText() + ":",
						false),
				new GuiPageButtonList.EditBoxEntry(138,
						String.format("%2.3f", settings.baseSize), false,
						numberFilter),
				new GuiPageButtonList.GuiLabelEntry(407,
						new TextComponentTranslation("createWorld.customize.custom.coordinateScale").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(139,
						String.format("%5.3f", settings.coordinateScale),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(408,
						new TextComponentTranslation("createWorld.customize.custom.heightScale").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(140,
						String.format("%5.3f", settings.heightScale), false,
						numberFilter),
				new GuiPageButtonList.GuiLabelEntry(409,
						new TextComponentTranslation("createWorld.customize.custom.stretchY").getFormattedText() + ":",
						false),
				new GuiPageButtonList.EditBoxEntry(141,
						String.format("%2.3f", settings.stretchY), false,
						numberFilter),
				new GuiPageButtonList.GuiLabelEntry(410,
						new TextComponentTranslation("createWorld.customize.custom.upperLimitScale").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(142,
						String.format("%5.3f", settings.upperLimitScale),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(411,
						new TextComponentTranslation("createWorld.customize.custom.lowerLimitScale").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(143,
						String.format("%5.3f", settings.lowerLimitScale),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(412,
						new TextComponentTranslation("createWorld.customize.custom.biomeDepthWeight").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(144,
						String.format("%2.3f", settings.biomeDepthWeight),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(413,
						new TextComponentTranslation("createWorld.customize.custom.biomeDepthOffset").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(145,
						String.format("%2.3f", settings.biomeDepthOffset),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(414,
						new TextComponentTranslation("createWorld.customize.custom.biomeScaleWeight").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(146,
						String.format("%2.3f", settings.biomeScaleWeight),
						false, numberFilter),
				new GuiPageButtonList.GuiLabelEntry(415,
						new TextComponentTranslation("createWorld.customize.custom.biomeScaleOffset").getFormattedText()
								+ ":",
						false),
				new GuiPageButtonList.EditBoxEntry(147,
						String.format("%2.3f", settings.biomeScaleOffset),
						false, numberFilter) };
		list = new GuiPageButtonList(mc, width, height, 32, height - 32, 25, this,
				guiListentry, guiListentry_2, guiListentry_3,
				guiListentry_4);
		for (int i = 0; i < 4; ++i) {
			pageNames[i] = new TextComponentTranslation("createWorld.customize.custom.page" + i)
					.getFormattedText();
		}
		updatePageControls();
	}

	private void restoreDefaults() {
		settings.setDefaults();
		createPagedList();
	}

	private void modifyFocusValue(float value) {
		Gui gui = list.getFocusedControl();
		if (gui instanceof GuiTextField) {
			float f1 = value;
			if (GuiScreen.isShiftKeyDown()) {
				f1 = value * 0.1F;
				if (GuiScreen.isCtrlKeyDown()) {
					f1 *= 0.1F;
				}
			} else if (GuiScreen.isCtrlKeyDown()) {
				f1 = value * 10.0F;
				if (GuiScreen.isAltKeyDown()) {
					f1 *= 10.0F;
				}
			}
			GuiTextField guitextfield = (GuiTextField) gui;
			Float f2 = null;
			try { f2 = Float.parseFloat(guitextfield.getText()); } catch (Exception e) { LogWriter.error(e); }
			if (f2 != null) {
				f2 = f2 + f1;
				int i = guitextfield.getId();
				String s = getFormattedValue(guitextfield.getId(), f2);
				guitextfield.setText(s);
				setEntryValue(i, s);
			}
		}
	}

	private void updatePageControls() {
		previousPage.enabled = list.getPage() != 0;
		nextPage.enabled = list.getPage() != list.getPageCount() - 1;
		subtitle = new TextComponentTranslation("book.pageIndicator", list.getPage() + 1, list.getPageCount()).getFormattedText();
		pageTitle = pageNames[list.getPage()];
		randomize.enabled = list.getPage() != list.getPageCount() - 1;
	}

	private void setConfirmationControls(boolean isVisible) {
		confirm.visible = isVisible;
		cancel.visible = isVisible;
		randomize.enabled = !isVisible;
		done.enabled = !isVisible;
		previousPage.enabled = !isVisible;
		nextPage.enabled = !isVisible;
		defaults.enabled = !isVisible;
		presets.enabled = !isVisible;
	}

	private String getFormattedValue(int type, float value) {
		switch (type) {
			case 100:
			case 101:
			case 102:
			case 103:
			case 104:
			case 107:
			case 108:
			case 110:
			case 111:
			case 132:
			case 133:
			case 134:
			case 135:
			case 136:
			case 139:
			case 140:
			case 142:
			case 143:
				return String.format("%5.3f", value);
			case 105:
			case 106:
			case 109:
			case 112:
			case 113:
			case 114:
			case 115:
			case 137:
			case 138:
			case 141:
			case 144:
			case 145:
			case 146:
			case 147:
				return String.format("%2.3f", value);
            case 162:
				if (value < 0.0F) {
					return new TextComponentTranslation("gui.all").getFormattedText();
				} else {
					Biome biome_gen_base;
					if ((int) value >= Biome.getIdForBiome(Biomes.HELL)) {
						biome_gen_base = Biome.getBiome((int) value + 2);
					} else {
						biome_gen_base = Biome.getBiome((int) value);
					}
					return biome_gen_base != null ? biome_gen_base.getBiomeName() : "?";
				}
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            default:
				return String.format("%d", (int) value);
		}
	}

	private void exitConfirmation() throws IOException {
		switch (confirmMode) {
		case 300:
			actionPerformed((GuiListButton) list.getComponent(300));
			break;
		case 304:
			restoreDefaults();
		}
		confirmMode = 0;
		confirmDismissed = true;
		setConfirmationControls(false);
	}

	@Nonnull
	@Override
	public String getText(int type, @Nonnull String domain, float value) {
		return domain + ": " + getFormattedValue(type, value);
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		list.handleMouseInput();
	}

	@Override
	public void initGui() {
		title = new TextComponentTranslation("dimensions.customize.title").getFormattedText();
		buttonList.clear();
		buttonList.add(previousPage = new GuiButton(302, 20, 5, 80, 20,
				new TextComponentTranslation("createWorld.customize.custom.prev").getFormattedText()));
		buttonList.add(nextPage = new GuiButton(303, width - 100, 5, 80, 20,
				new TextComponentTranslation("createWorld.customize.custom.next").getFormattedText()));
		buttonList.add(defaults = new GuiButton(304, width / 2 - 187, height - 27, 90, 20,
				new TextComponentTranslation("createWorld.customize.custom.defaults").getFormattedText()));
		buttonList.add(randomize = new GuiButton(301, width / 2 - 92, height - 27, 90, 20,
				new TextComponentTranslation("createWorld.customize.custom.randomize").getFormattedText()));
		buttonList.add(presets = new GuiButton(305, width / 2 + 3, height - 27, 90, 20,
				new TextComponentTranslation("createWorld.customize.custom.presets").getFormattedText()));
		buttonList.add(done = new GuiButton(300, width / 2 + 98, height - 27, 90, 20,
				new TextComponentTranslation("gui.done").getFormattedText()));
		confirm = new GuiButton(306, width / 2 - 55, 160, 50, 20,
				new TextComponentTranslation("gui.yes").getFormattedText());
		confirm.visible = false;
		buttonList.add(confirm);
		cancel = new GuiButton(307, width / 2 + 5, 160, 50, 20,
				new TextComponentTranslation("gui.no").getFormattedText());
		cancel.visible = false;
		buttonList.add(cancel);
		createPagedList();
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		super.keyTyped(typedChar, keyCode);
		if (confirmMode == 0) {
			switch (keyCode) {
				case 200: modifyFocusValue(1.0F);break;
				case 208: modifyFocusValue(-1.0F);break;
				default: list.onKeyPressed(typedChar, keyCode);
			}
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (confirmMode == 0 && !confirmDismissed) { list.mouseClicked(mouseX, mouseY, mouseButton); }
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		if (confirmDismissed) {
			confirmDismissed = false;
		} else if (confirmMode == 0) {
			list.mouseReleased(mouseX, mouseY, state);
		}
	}

	@Override
	public void setEntryValue(int type, boolean value) {
		switch (type) {
			case 148: settings.useCaves = value; break;
			case 149: settings.useDungeons = value; break;
			case 150: settings.useStrongholds = value; break;
			case 151: settings.useVillages = value; break;
			case 152: settings.useMineShafts = value; break;
			case 153: settings.useTemples = value; break;
			case 154: settings.useRavines = value; break;
			case 155: settings.useWaterLakes = value; break;
			case 156: settings.useLavaLakes = value; break;
			case 161: settings.useLavaOceans = value; break;
			case 210: settings.useMonuments = value;
		}
		if (!settings.equals(defaultSettings)) {
			settingsModified = true;
		}
	}

	@Override
	public void setEntryValue(int type, float value) {
		switch (type) {
			case 100: settings.mainNoiseScaleX = value; break;
			case 101: settings.mainNoiseScaleY = value; break;
			case 102: settings.mainNoiseScaleZ = value; break;
			case 103: settings.depthNoiseScaleX = value; break;
			case 104: settings.depthNoiseScaleZ = value; break;
			case 105: settings.depthNoiseScaleExponent = value; break;
			case 106: settings.baseSize = value; break;
			case 107: settings.coordinateScale = value; break;
			case 108: settings.heightScale = value; break;
			case 109: settings.stretchY = value; break;
			case 110: settings.upperLimitScale = value; break;
			case 111: settings.lowerLimitScale = value; break;
			case 112: settings.biomeDepthWeight = value; break;
			case 113: settings.biomeDepthOffset = value; break;
			case 114: settings.biomeScaleWeight = value; break;
			case 115: settings.biomeScaleOffset = value; break;
            case 157: settings.dungeonChance = (int) value; break;
			case 158: settings.waterLakeChance = (int) value; break;
			case 159: settings.lavaLakeChance = (int) value; break;
			case 160: settings.seaLevel = (int) value; break;
			case 162: settings.fixedBiome = (int) value; break;
			case 163: settings.biomeSize = (int) value; break;
			case 164: settings.riverSize = (int) value; break;
			case 165: settings.dirtSize = (int) value; break;
			case 166: settings.dirtCount = (int) value; break;
			case 167: settings.dirtMinHeight = (int) value; break;
			case 168: settings.dirtMaxHeight = (int) value; break;
			case 169: settings.gravelSize = (int) value; break;
			case 170: settings.gravelCount = (int) value; break;
			case 171: settings.gravelMinHeight = (int) value; break;
			case 172: settings.gravelMaxHeight = (int) value; break;
			case 173: settings.graniteSize = (int) value; break;
			case 174: settings.graniteCount = (int) value; break;
			case 175: settings.graniteMinHeight = (int) value; break;
			case 176: settings.graniteMaxHeight = (int) value; break;
			case 177: settings.dioriteSize = (int) value; break;
			case 178: settings.dioriteCount = (int) value; break;
			case 179: settings.dioriteMinHeight = (int) value; break;
			case 180: settings.dioriteMaxHeight = (int) value; break;
			case 181: settings.andesiteSize = (int) value; break;
			case 182: settings.andesiteCount = (int) value; break;
			case 183: settings.andesiteMinHeight = (int) value; break;
			case 184: settings.andesiteMaxHeight = (int) value; break;
			case 185: settings.coalSize = (int) value; break;
			case 186: settings.coalCount = (int) value; break;
			case 187: settings.coalMinHeight = (int) value; break;
			case 189: settings.coalMaxHeight = (int) value; break;
			case 190: settings.ironSize = (int) value; break;
			case 191: settings.ironCount = (int) value; break;
			case 192: settings.ironMinHeight = (int) value; break;
			case 193: settings.ironMaxHeight = (int) value; break;
			case 194: settings.goldSize = (int) value; break;
			case 195: settings.goldCount = (int) value; break;
			case 196: settings.goldMinHeight = (int) value; break;
			case 197: settings.goldMaxHeight = (int) value; break;
			case 198: settings.redstoneSize = (int) value; break;
			case 199: settings.redstoneCount = (int) value; break;
			case 200: settings.redstoneMinHeight = (int) value; break;
			case 201: settings.redstoneMaxHeight = (int) value; break;
			case 202: settings.diamondSize = (int) value; break;
			case 203: settings.diamondCount = (int) value; break;
			case 204: settings.diamondMinHeight = (int) value; break;
			case 205: settings.diamondMaxHeight = (int) value; break;
			case 206: settings.lapisSize = (int) value; break;
			case 207: settings.lapisCount = (int) value; break;
			case 208: settings.lapisCenterHeight = (int) value; break;
			case 209: settings.lapisSpread = (int) value;
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 153:
            case 154:
            case 155:
            case 156:
            case 161:
            case 188:
            default: break;
		}
		if (type >= 100 && type < 116) {
			Gui gui = list.getComponent(type - 100 + 132);
            ((GuiTextField) gui).setText(getFormattedValue(type, value));
        }
		if (!settings.equals(defaultSettings)) { settingsModified = true; }
	}

	@Override
	public void setEntryValue(int type, @Nonnull String value) {
		float f = 0.0F;
		try { f = Float.parseFloat(value); } catch (NumberFormatException e) { LogWriter.error(e); }
		float f1 = 0.0F;
		switch (type) {
			case 132: f1 = settings.mainNoiseScaleX = MathHelper.clamp(f, 1.0F, 5000.0F); break;
			case 133: f1 = settings.mainNoiseScaleY = MathHelper.clamp(f, 1.0F, 5000.0F); break;
			case 134: f1 = settings.mainNoiseScaleZ = MathHelper.clamp(f, 1.0F, 5000.0F); break;
			case 135: f1 = settings.depthNoiseScaleX = MathHelper.clamp(f, 1.0F, 2000.0F); break;
			case 136: f1 = settings.depthNoiseScaleZ = MathHelper.clamp(f, 1.0F, 2000.0F); break;
			case 137: f1 = settings.depthNoiseScaleExponent = MathHelper.clamp(f, 0.01F, 20.0F); break;
			case 138: f1 = settings.baseSize = MathHelper.clamp(f, 1.0F, 25.0F); break;
			case 139: f1 = settings.coordinateScale = MathHelper.clamp(f, 1.0F, 6000.0F); break;
			case 140: f1 = settings.heightScale = MathHelper.clamp(f, 1.0F, 6000.0F); break;
			case 141: f1 = settings.stretchY = MathHelper.clamp(f, 0.01F, 50.0F); break;
			case 142: f1 = settings.upperLimitScale = MathHelper.clamp(f, 1.0F, 5000.0F); break;
			case 143: f1 = settings.lowerLimitScale = MathHelper.clamp(f, 1.0F, 5000.0F); break;
			case 144: f1 = settings.biomeDepthWeight = MathHelper.clamp(f, 1.0F, 20.0F); break;
			case 145: f1 = settings.biomeDepthOffset = MathHelper.clamp(f, 0.0F, 20.0F); break;
			case 146: f1 = settings.biomeScaleWeight = MathHelper.clamp(f, 1.0F, 20.0F); break;
			case 147: f1 = settings.biomeScaleOffset = MathHelper.clamp(f, 0.0F, 20.0F);
		}
		if (f1 != f && f != 0.0F) { ((GuiTextField) list.getComponent(type)).setText(getFormattedValue(type, f1)); }
		((GuiSlider) list.getComponent(type - 132 + 100)).setSliderValue(f1, false);
		if (!settings.equals(defaultSettings)) { settingsModified = true; }
	}

}
