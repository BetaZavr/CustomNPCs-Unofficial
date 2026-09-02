package noppes.npcs.client.gui.player;

import java.awt.Color;
import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.ClientEventHandler;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketPlayerFactionsGet;
import noppes.npcs.packets.server.SPacketQuestRemoveActive;
import noppes.npcs.packets.server.SPacketScriptRun;
import noppes.npcs.packets.server.SPacketSyncUpdate;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCheckBoxNop;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.QuestEvent.QuestExtraButtonEvent;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.Util;

public class GuiLog extends GuiNPCInterface
		implements IGuiData, ISliderListener, ITextfieldListener {

	protected static final Map<Integer, ResourceLocation> ql = new TreeMap<>();
	protected static final ResourceLocation bookGuiTextures = new ResourceLocation("textures/gui/book.png");
	protected static final ResourceLocation killIcon = new ResourceLocation("textures/entity/skeleton/skeleton.png");
	public static float scaleW;
	public static float scaleH;
	public static int fontHeight = ClientProxy.LogFont.getHeight();
	protected static ScaledResolution sw;

	static {
		GuiLog.ql.clear();
		for (int i = 0; i < 6; i++) { GuiLog.ql.put(i,
				new ResourceLocation(CustomNpcs.MODID, "textures/quest/log/q_log_" + i + ".png")); }
	}

	public static QuestInfo activeQuest;

	public static float[] preDrawEntity(String modelName, Entity entity) {
		initScale();
		float[] offsets = new float[] { 0.0f, 0.0f, 1.0f };
		boolean canUpdate = true;
		switch (modelName) {
			case "customnpcs:npcslime":
			case "minecraft:shulker":
				offsets[0] = -2.0f * scaleW;
				offsets[1] = -15.0f * scaleH;
				break;
			case "minecraft:magma_cube":
			case "minecraft:silverfish":
			case "minecraft:slime":
				offsets[0] = -2.0f * scaleW;
				offsets[1] = -21.0f * scaleH;
				break;
			case "minecraft:zombie":
				offsets[0] = 3.0f * scaleW;
				offsets[1] = 9.0f * scaleH;
				break;
			case "minecraft:vex":
				offsets[0] = -3.0f * scaleW;
				offsets[1] = -15.0f * scaleH;
				break;
			case "minecraft:endermite":
				offsets[0] = -1.0f * scaleW;
				offsets[1] = -25.0f * scaleH;
				break;
			case "minecraft:enderman":
				offsets[1] = 30.0f * scaleH;
				break;
			case "minecraft:cave_spider":
				offsets[0] = -2.0f * scaleW;
				offsets[1] = -18.0f * scaleH;
				break;
			case "minecraft:chicken":
			case "minecraft:cow":
			case "minecraft:wolf":
			case "minecraft:ocelot":
			case "minecraft:spider":
				offsets[1] = -15.0f * scaleH;
				break;
			case "minecraft:squid":
				offsets[1] = -5.0f * scaleH;
				break;
			case "minecraft:guardian":
				offsets[0] = 4.0f * scaleW;
				offsets[1] = -18.5f * scaleH;
				canUpdate = false;
				break;
			case "minecraft:parrot":
			case "minecraft:rabbit":
			case "minecraft:bat":
				offsets[1] = -19.0f * scaleH;
				break;
			case "minecraft:horse":
			case "minecraft:illusion_illager":
			case "minecraft:villager":
			case "minecraft:snowman":
			case "minecraft:vindication_illager":
			case "minecraft:zombie_horse":
			case "minecraft:zombie_villager":
			case "minecraft:stray":
			case "minecraft:skeleton":
			case "minecraft:witch":
			case "minecraft:skeleton_horse":
			case "minecraft:mule":
			case "minecraft:evocation_illager":
			case "minecraft:zombie_pigman":
				offsets[1] = 5.0f * scaleH;
				break;
			case "minecraft:ender_dragon":
				offsets[0] = 35.0f * scaleW;
				offsets[1] = -32.0f * scaleH;
				offsets[2] = 0.5f;
				break;
			case "minecraft:elder_guardian":
				offsets[0] = 1.5f * scaleW;
				offsets[1] = -15.0f * scaleH;
				offsets[2] = 0.5f;
				canUpdate = false;
				break;
			case "minecraft:giant":
				offsets[1] = 15.0f * scaleH;
				offsets[2] = 0.1875f;
				canUpdate = false;
				break;
			case "customnpcs:npcdragon":
				offsets[0] = 22.0f * scaleW;
				offsets[1] = -16.0f * scaleH;
				canUpdate = false;
				break;
			case "customnpcs:npcpony":
				offsets[0] = -5.0f * scaleW;
				offsets[1] = 2.0f * scaleH;
				break;
			case "customnpcs:npccrystal":
				offsets[1] = 3.0f * scaleH;
				break;
			case "minecraft:wither_skeleton":
			case "minecraft:villager_golem":
			case "minecraft:customnpcs.npcgolem":
				offsets[1] = 18.0f * scaleH;
				break;
			case "minecraft:polar_bear":
				offsets[0] = -1.0f * scaleW;
				offsets[1] = -12.0f * scaleH;
				offsets[2] = 0.75f;
				break;
			case "minecraft:husk":
			case "minecraft:llama":
				offsets[1] = 12.0f * scaleH;
				break;
			case "minecraft:pig":
				offsets[1] = -12.0f * scaleH;
				break;
			case "minecraft:wither":
				offsets[0] = -3.0f * scaleW;
				offsets[1] = 3.0f * scaleH;
				offsets[2] = 0.5f;
				break;
			case "minecraft:ghast":
				offsets[0] = -2.0f * scaleW;
				offsets[1] = -21.0f * scaleH;
				offsets[2] = 0.2f;
				break;
			case "minecraft:customnpcs.customnpcalex":
				offsets[0] = -1.0f * scaleW;
				break;
			default:
				offsets[1] = -8.0f * scaleH;
				break;
		}
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		if (canUpdate) { entity.onUpdate(); }
		return offsets;
	}
	/*
	 * 0-tab inv; 1-tab factions; 2-tab quests; 3-tab compass 4-page right; 5-page
	 * left 6-quest; 7/14-tab categories 16-pre cat list; 17-next cat list 20/28-cat
	 * list 30 - extended button 31 - compass look 32 - cancel quest
	 */
	protected boolean toPrePage = true;
	protected final Random rnd = new Random();
	protected final Map<String, Map<Integer, QuestData>> quests = new TreeMap<>(); // {category, [questId, quest]}
	protected final Map<String, Color> categories = new TreeMap<>(); // [name, color]
	protected final List<Faction> playerFactions = new ArrayList<>();
	protected final int questLogColor;
	protected final int notEnableColor;
	protected final PlayerData data;
	protected int hoverButton;
	protected int hoverQuestId;
	protected int catRow;
	protected int catSelect;
	protected int page;
	protected int step;
	protected int tick;
	protected int milliTick;
	protected int temp;
	protected int guiLLeft;
	protected int guiLRight;
	protected int guiLTop;
	protected int guiTopLog;
	protected int guiCenter;
	public int type; // -1-inv; 0-faction; 1-quests; 2-compass

	protected PlayerCompassData compassData;
	protected PlayerFactionData factionData;

	public GuiLog(int t) {
		super();
		drawDefaultBackground = false;
		hoverIsGame = true;

		data = PlayerData.get(player);

		type = t;
		temp = 0;
		setNextTick(15, false);
		step = 0;

		imageWidth = 0;
		imageHeight = 0;
		width = 0;
		height = 0;
		hoverButton = -1;
		hoverQuestId = 0;
		catRow = 0;
		catSelect = 0;
		page = 0;
		factionData = data.factionData;
		compassData = data.compass;
		activeQuest = null;

		questLogColor = CustomNpcs.QuestLogColor.getRGB() | (int) Math.ceil(255.0F) << 24;
		notEnableColor = CustomNpcs.NotEnableColor.getRGB() | (int) Math.ceil(255.0F) << 24;
		if (ClientEventHandler.COMPASS_BODY == null) { ClientEventHandler.COMPASS_BODY = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
				Collections.singletonList("body"), GuiBasic.TEXTURES_COMPASS,  false, 0, false); }
		if (ClientEventHandler.COMPASS_DIAL == null) { ClientEventHandler.COMPASS_DIAL = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
				Collections.singletonList("dial"), GuiBasic.TEXTURES_COMPASS,  false, 0, false); }
		if (ClientEventHandler.COMPASS_ARROW_0 == null) { ClientEventHandler.COMPASS_ARROW_0 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
				Collections.singletonList("arrow_0"), GuiBasic.TEXTURES_COMPASS,  false, 0, false); }
		if (ClientEventHandler.COMPASS_ARROW_1 == null) { ClientEventHandler.COMPASS_ARROW_1 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
				Collections.singletonList("arrow_1"), GuiBasic.TEXTURES_COMPASS,  false, 0, false); }
		if (ClientEventHandler.COMPASS_ARROW_20 == null) { ClientEventHandler.COMPASS_ARROW_20 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
				Collections.singletonList("arrow_20"), GuiBasic.TEXTURES_COMPASS,  false, 0, false); }
		if (ClientEventHandler.COMPASS_ARROW_21 == null) { ClientEventHandler.COMPASS_ARROW_21 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
				Collections.singletonList("arrow_21"), GuiBasic.TEXTURES_COMPASS,  false, 0, false); }
		if (ClientEventHandler.COMPASS_ARROW_22 == null) { ClientEventHandler.COMPASS_ARROW_22 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
				Collections.singletonList("arrow_22"), GuiBasic.TEXTURES_COMPASS,  false, 0, false); }
		if (!ClientEventHandler.COMPASS_FASE.containsKey(type)) {
			Map<String, ResourceLocation> m = new HashMap<>();
			m.put("#material", new ResourceLocation(CustomNpcs.MODID, "util/compass"));
			m.put("#task", new ResourceLocation(CustomNpcs.MODID, "util/task_" + type));
			ClientEventHandler.COMPASS_FASE.put(type, ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
					Collections.singletonList("fase"), m,  false, 0, false));
		}
		if (t == 1) { Packets.sendServer(new SPacketPlayerFactionsGet()); }
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (type != 2) { return; }
		switch (button.id) {
			case 0: compassData.showQuestName = ((GuiCheckBoxNop) button).selected(); break;
			case 1: compassData.showTaskProgress = ((GuiCheckBoxNop) button).selected(); break;
			case 2: {
				CustomNpcs.TypeShowQuestCompass = ValueUtil.correctInt(button.getValue(), 0, 4);
				button.setHoverTexts("quest.screen.hover.compass.global", "quest.screen.hover.compass.type." + CustomNpcs.TypeShowQuestCompass);
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("value", CustomNpcs.TypeShowQuestCompass);
				Packets.sendServer(new SPacketSyncUpdate(0, compound));
				break;
			}
			case 3: compassData.showDial = ((GuiCheckBoxNop) button).selected(); break;
			case 4: compassData.showOfPlayer = ((GuiCheckBoxNop) button).selected(); break;
			case 5: {
				compassData.isFlat = ((GuiCheckBoxNop) button).selected();
				compassData.screenPos[0] = compassData.isFlat ? 0.5f : 0.145f;
				compassData.screenPos[1] = compassData.isFlat ? 0.025f : 0.765f;
				initGui();
				break;
			}
			case 6: compassData.questLogIsFast = ((GuiCheckBoxNop) button).selected(); break;
		}
	}

	public boolean buttonPress(int id) {
		if (type == 0 && id > 6 && id < 15) {
			int catList = catRow * 8 + id - 7;
			if (catSelect == catList && page != 0) {
				step = 11;
				setNextTick(10, false);
				page = 0;
			}
			if (catSelect != catList || activeQuest != null) {
				step = catSelect > catList || activeQuest != null ? 11 : 10;
				setNextTick(11, true);
				catSelect = catList;
				page = 0;
				activeQuest = null;
			}
			return true;
		} // quest category rows
		switch (id) {
			case 0: {
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				setNextTick(15, false);
				step = type + 7;
				type = -1;
				return true;
			} // inventory
			case 1: {
				if (type == 1) { return false; }
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				setNextTick(15, false);
				toPrePage = false;
				step = type + 7;
				page = 0;
				type = 1;
				Packets.sendServer(new SPacketPlayerFactionsGet());
				initGui();
				return true;
			} // factions
			case 2: {
				if (type == 1) { return false; }
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				setNextTick(15, false);
				toPrePage = type == 1;
				step = type + 7;
				catRow = 0;
				catSelect = 0;
				page = 0;
				activeQuest = null;
				type = 0;
				initGui();
				return true;
			} // quests
			case 3: {
				if (type != 2 && CustomNpcs.TypeShowQuestCompass != 4) {
					mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					setNextTick(15, false);
					toPrePage = true;
					step = type + 7;
					page = 0;
					type = 2;
					initGui();
				}
				return true;
			} // compass
			case 4: {
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				page++;
				step = 10;
				setNextTick(10, false);
				return true;
			} // page right
			case 5: {
				page--;
				step = 11;
				setNextTick(10, false);
				return true;
			} // page left
			case 6: {
				if (hoverQuestId >= 0) {
					String catName = "";
					int i = 0;
					for (String key : categories.keySet()) {
						if (i == catSelect) {
							catName = key;
							break;
						}
						i++;
					}
					if (catName.isEmpty() || !quests.containsKey(catName) || !quests.get(catName).containsKey(hoverQuestId)) { return false; }
					activeQuest = new QuestInfo(quests.get(catName).get(hoverQuestId), mc.world);
					step = 10;
					setNextTick(10, false);
				}
				return true;
			} // quest select
			case 16: {
				if (type != 0) { return false; }
				MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.sheet",
						(float) player.posX, (float) player.posY, (float) player.posZ, 1.0f,
						0.8f + 0.4f * rnd.nextFloat());
				catRow--;
				return true;
			} // pre cat list
			case 17: {
				if (type != 0) { return false; }
				MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.sheet",
						(float) player.posX, (float) player.posY, (float) player.posZ, 1.0f,
						0.8f + 0.4f * rnd.nextFloat());
				catRow++;
				return true;
			} // next cat list
			case 30: {
				if (hoverQuestId <= 0) { return false; }
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.QUEST_LOG_BUTTON, new QuestExtraButtonEvent((IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player), QuestController.instance.get(hoverQuestId)));
				Packets.sendServer(new SPacketScriptRun(EnumScriptType.QUEST_LOG_BUTTON, hoverQuestId));
				return true;
			} // extended button
			case 31: {
				if (hoverQuestId <= 0) { return false; }
				if (data.compass.questID == hoverQuestId) { data.compass.questID = -1; }
				else { data.compass.questID = hoverQuestId; }
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				return true;
			} // compass look
			case 32: {
				if (hoverQuestId <= 0) { return false; }
				for (Map<Integer, QuestData> map : quests.values()) {
					for (QuestData qd : map.values()) {
						if (qd.quest.id == hoverQuestId) {
							ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
								if (bo) {
									Packets.sendServer(new SPacketQuestRemoveActive(hoverQuestId));
									if (data.questData != null) {
										data.questData.activeQuests.remove(hoverQuestId);
										initGui();
									}
								}
								NoppesUtil.openGUI(player, this);
							},
									Component.translatable("drop.quest", qd.quest.getTitle().getFormattedText()).getParent(),
									Component.translatable("quest.cancel.info").getParent());
							setScreen(guiYesNo);
							break;
						}
					}
				}
				return true;
			} // cancel quest
		}
		return false;
	}

	protected void drawBox(int mouseX, int mouseY) {
		hoverButton = -1;
		hoverQuestId = 0;
		// tabs
		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLeft + 10, guiTop, 0.0f);
		boolean offset = false;
		for (int i = 0; i < (CustomNpcs.TypeShowQuestCompass != 4 ? 4 : 3); i++) {
			boolean hover;
			switch (i) {
				case 1: {
					if (offset) { GlStateManager.translate(0.0f, 0.0f, -1.0f); }
					offset = (type == 1);
					GlStateManager.translate(33.0f, 0.0f, offset ? 1.0f : 0.0f);
					hover = isMouseHover(mouseX, mouseY, guiLeft + 43, guiTop, 28, 30);
					break;
				}
				case 2: {
					if (offset) { GlStateManager.translate(0.0f, 0.0f, -1.0f); }
					offset = (type == 0);
					GlStateManager.translate(33.0f, 0.0f, offset ? 1.0f : 0.0f);
					hover = isMouseHover(mouseX, mouseY, guiLeft + 76, guiTop, 28, 30);
					break;
				}
				case 3: {
					if (offset) { GlStateManager.translate(0.0f, 0.0f, -1.0f); }
					offset = (type == 2);
					GlStateManager.translate(-114.0f + 256.0f * scaleW, 0.0f, offset ? 1.0f : 0.0f);
					hover = isMouseHover(mouseX, mouseY, (int) (guiLeft - 38.0f + 256.0f * scaleW), guiTop, 28, 30);
					break;
				}
				default: {
					hover = isMouseHover(mouseX, mouseY, guiLeft + 10, guiTop, 28, 30);
					break;
				}
			}
			if (hover) { hoverButton = i; }
			mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
			drawTexturedModalRect(0, 0, 0, hover || offset ? 30 : 60, 28, 30);

			GlStateManager.pushMatrix();
			GlStateManager.translate(6.0f, 8.0f, 0.0f);
			zLevel = 100.0f;
			itemRender.zLevel = 100.0f;
			GlStateManager.enableLighting();
			GlStateManager.enableRescaleNormal();
			ItemStack stack;
			switch (i) {
				case 1: stack = new ItemStack(Items.BANNER, 1, 1); break;
				case 2: stack = new ItemStack(Items.BOOK); break;
				case 3: stack = new ItemStack(Items.COMPASS); break;
				default: stack = new ItemStack(Blocks.CRAFTING_TABLE); break;
			}
			RenderHelper.enableGUIStandardItemLighting();
			itemRender.renderItemAndEffectIntoGUI(stack, 0, 0);
			itemRender.renderItemOverlayIntoGUI(mc.fontRenderer, stack, 6, 8, null);
			RenderHelper.disableStandardItemLighting();
			GlStateManager.disableLighting();
			itemRender.zLevel = 0.0f;
			zLevel = 0.0f;
			GlStateManager.popMatrix();
		}
		GlStateManager.popMatrix();

		// place
		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLeft, guiTopLog, 0.0f);
		GlStateManager.scale(scaleW, scaleH, 1.0f);
		mc.getTextureManager().bindTexture(GuiLog.ql.get(0));
		drawTexturedModalRect(0, 0, 0, 0, 256, 175);
		mc.getTextureManager().bindTexture(GuiLog.ql.get(1));
		drawTexturedModalRect(0, 0, 0, 0, 256, 175);
		GlStateManager.popMatrix();

		if (step == -1 && (type == 0 || type == 1)) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(guiLLeft + 2.0f * scaleH, guiLTop + 151.5f * scaleH, 0.0f);
			GlStateManager.scale(0.85f, 0.85f, 0.85f);
			draw("" + (page * 2 + 1), 0, 0, notEnableColor, 0);
			GlStateManager.popMatrix();
			GlStateManager.pushMatrix();
			String p = "" + (page * 2 + 2);
			GlStateManager.translate(guiLLeft - ClientProxy.LogFont.width(p) + 205.0f * scaleW, guiLTop + 151.5f * scaleH, 0.0f);
			GlStateManager.scale(0.85f, 0.85f, 0.85f);
			draw(p, 0, 0, notEnableColor, 0);
			GlStateManager.popMatrix();
		}
		if (step >= 0 && step < 10) { return; }
		if (type == 0) { drawQuestLog(mouseX, mouseY); }
		else if (type == 1) { drawFaction(mouseX, mouseY); }
		else if (type == 2) { drawCompass(); }
	}

	protected void drawCompass() {
		if (step != -1 || !(CustomNpcs.TypeShowQuestCompass != 4 || player.isCreative())) { return; }

		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLRight + (int) (52.0f * scaleW), guiLTop + (int) (57.0f * scaleH), 75.0f);
		if (compassData.isFlat) {
			GlStateManager.translate(-32.0f * scaleW, -26.0f * scaleH, 0.0f);
			GlStateManager.pushMatrix();
			GlStateManager.translate((-36.0f * compassData.scale + 36.0f) * scaleH, (-8.0f * compassData.scale + 8.0f) * scaleH, 0.0f);
			GlStateManager.scale(0.3f * scaleW * compassData.scale, 0.3f * scaleH * compassData.scale, 0.5f);
			minecraft.getTextureManager().bindTexture(GuiBasic.INFO);
			drawTexturedModalRect(0, 0, 0, 74, 104, 28);
			GlStateManager.translate(104.0f, 0.0f, 0.0f);
			drawTexturedModalRect(0, 0, 100, 102, 104, 28);
			GlStateManager.popMatrix();
		}
		else {
			float scale = 15.0f * compassData.scale;
			float incline = 45.0f + compassData.incline;

			GlStateManager.translate(0.0f, (-15.5f * compassData.scale - 18.75f) * scaleH, 0.0f);
			GlStateManager.scale(scale * scaleW, -scale * scaleH, scale);
			GlStateManager.rotate(incline, 1.0f, 0.0f, 0.0f);
			GlStateManager.rotate(180.0f + compassData.rot, 0.0f, 1.0f, 0.0f);
			// Body
			ModelBuffer.render(ClientEventHandler.COMPASS_BODY);
			ModelBuffer.render(ClientEventHandler.COMPASS_FASE.get(0));
			long l0 = System.currentTimeMillis() % 7500L;
			// up|down left arrow
			if (l0 <= 2750) { ModelBuffer.render(ClientEventHandler.COMPASS_ARROW_21); }
			else if (l0 >= 4750) { ModelBuffer.render(ClientEventHandler.COMPASS_ARROW_22); }
			else { ModelBuffer.render(ClientEventHandler.COMPASS_ARROW_20); }
			float f0;
			if (l0 > 3750) { f0 = (float) l0 * 0.000133f - 0.75f; }
			else { f0 = (float) l0 * -0.000133f + 0.25f; }
			GlStateManager.pushMatrix();
			GlStateManager.translate(0.0f, f0, 0.0f);
			ModelBuffer.render(ClientEventHandler.COMPASS_ARROW_1);
			GlStateManager.popMatrix();

			GlStateManager.pushMatrix();
			f0 = (float) l0 * 0.048f;
			GlStateManager.rotate(f0, 0.0f, 1.0f, 0.0f);
			// up|down right arrow
			ModelBuffer.render(ClientEventHandler.COMPASS_ARROW_0);
			// dial
			if (compassData.isShowDial()) {
				GlStateManager.rotate(-2.0f * f0, 0.0f, 1.0f, 0.0f);
				ModelBuffer.render(ClientEventHandler.COMPASS_DIAL);
			}
			GlStateManager.popMatrix();
		}
		GlStateManager.popMatrix();
		if (compassData.isFlat && compassData.isShowDial()) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(guiLRight + 50.0f * scaleW, guiLTop + (30.0f + (-3.5f * compassData.scale + 3.25f)) * scaleH, 100.0f);
			draw("N", 0, 0, questLogColor, (int) (105.0f * scaleW));
			GlStateManager.popMatrix();
		}
		draw(Component.translatable("quest.screen.pos").getFormattedText(),
				(int) (guiLLeft - 3.0f * scaleW), guiLTop - 1, questLogColor, (int) (105.0f * scaleW));

		// window
		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLLeft - 3.0f * scaleW, guiLTop + 11.0f, 0.0f);
		GlStateManager.scale(0.5f * scaleW, 0.5f * scaleH, 0.5f);
		drawRect(-1, -1, 207, 139, 0xFF808080);
		drawRect(0, 0, 206, 138, 0xFFF0F0F0);
		drawRect(58, 113, 149, 139, 0xFF808080);
		drawRect(59, 114, 148, 138, 0xFFA0A0A0);

		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		ScaledResolution sw = new ScaledResolution(Minecraft.getMinecraft());
		double d4 = sw.getScaledWidth() < mc.displayWidth
				? (int) Math.round((double) mc.displayWidth / (double) sw.getScaledWidth())
				: 1;
		GL11.glScissor((int) ((guiLLeft - 3.0d * scaleW) * d4),
				(int) ((double) mc.displayHeight - (guiLTop + 107.0d) * d4),
				Math.max(0, (int) (((guiLLeft + 100.5d * scaleW) - (guiLLeft - 3.0d * scaleW)) * d4)),
				Math.max(0, (int) (((guiLTop + 107.0d) - (guiLTop + 11.0d)) * d4)));

		GlStateManager.pushMatrix();
		GlStateManager.translate(compassData.screenPos[0] * 206.0d, compassData.screenPos[1] * 138.0d, 0.0d);
		drawRect(compassData.isFlat ? -14 : -3, -1, compassData.isFlat ? 14 : 4, 3, 0xFF0000FF);
		if (!compassData.isFlat) { drawRect(-3, 3, 4, 5, 0xFFFF00FF); }
		GlStateManager.popMatrix();
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		GlStateManager.popMatrix();

		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLRight + (int) (3.0f * scaleW),
				guiLTop + (int) (40.0f * scaleH), 1.0f);
		int i = 0;
		if (compassData.showQuestName) {
			Component text = Component.translatable("quest.setts.q.name");
			int w = (int) (49.0f * scaleW) - ClientProxy.LogFont.width(text) / 2;
			draw(text.getParent().getFormattedText(), w, 0, questLogColor, (int) (55.0f * scaleW));
			i = fontHeight;
		}
		if (compassData.showTaskProgress) {
			Component text = Component.translatable("quest.setts.q.tasks");
			int w = (int) (49.0f * scaleW) - ClientProxy.LogFont.width(text) / 2;
			draw(text.getParent().getFormattedText(), w, i, questLogColor, (int) (55.0f * scaleW));
		}
		GlStateManager.popMatrix();
	}

	protected void drawFaction(int mouseX, int mouseY) {
		if (step != -1) {
			return;
		}
		if (playerFactions.isEmpty()) {
			draw(Component.translatable("faction.nostanding").getFormattedText(),
					guiLLeft, guiLTop, questLogColor, (int) (-98.0f * scaleW));
			return;
		}
		if (playerFactions.size() > 16) {
			if (page > 0) { // left
				GlStateManager.pushMatrix();
				GlStateManager.translate(guiLLeft - 5.0f * scaleW, guiLTop + 160.0f * scaleH, 0.0f);
				if (isMouseHover(mouseX, mouseY, (int) (guiLLeft - 5.0f * scaleW), (int) (guiLTop + 160.0f * scaleH), 18, 10)) {
					hoverButton = 5;
				} // pre cat list;
				mc.getTextureManager().bindTexture(GuiLog.bookGuiTextures);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				drawTexturedModalRect(0, 0, hoverButton == 5 ? 26 : 3, 207, 18, 10);
				GlStateManager.popMatrix();
			}
			if (Math.floor(playerFactions.size() / 16.d) > page) { // right
				GlStateManager.pushMatrix();
				GlStateManager.translate(guiLeft + 230.0f * scaleW, guiLTop + 160.0f * scaleH, 0.0f);
				if (isMouseHover(mouseX, mouseY, (int) (guiLeft + 230.0f * scaleW), (int) (guiLTop + 160.0f * scaleH), 18, 10)) {
					hoverButton = 4;
				} // next cat list;
				mc.getTextureManager().bindTexture(GuiLog.bookGuiTextures);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				drawTexturedModalRect(0, 0, hoverButton == 4 ? 26 : 3, 194, 18, 10);
				GlStateManager.popMatrix();
			}
		}
		int i = 0, p = 0;
		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLLeft, guiLTop, 0.0f);
		for (Faction f : playerFactions) {
			if (f.hideFaction && !player.isCreative()) {
				continue;
			}
			if (p < page * 10) {
				p++;
				continue;
			}
			if (i == 8) {
				GlStateManager.translate(105.0f * scaleW, -7.0f * 19.0f * scaleH, 0.0f);
			} else if (i % 8 != 0) {
				GlStateManager.translate(0.0f, 19.0f * scaleH, 0.0f);
			}
			if (f.hideFaction) {
				GlStateManager.pushMatrix();
				GlStateManager.scale(scaleW, scaleH, 1.0f);
				drawGradientRect(1, 1, 90, 12, 0x20FF0000, 0x80FF0000);
				GlStateManager.popMatrix();
			}
			mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
			Color c = new Color(f.color);
			GlStateManager.color(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 1.0f);

			GlStateManager.pushMatrix();
			GlStateManager.scale(scaleW, scaleH, 1.0f);
			drawTexturedModalRect(0, 0, 158, 74, 98, 16);
			GlStateManager.popMatrix();

			float w;
			Color h;
			int points = factionData.getFactionPoints(player, f.id), nextPoint = 0, t = 0;
			if (f.isNeutralToPlayer(player)) {
				t = 1;
				h = new Color(0xF2DD00);
				w = (float) (f.friendlyPoints - points) / (float) (f.friendlyPoints - f.neutralPoints);
				nextPoint = f.friendlyPoints;
			} else if (f.isFriendlyToPlayer(player)) {
				t = 2;
				h = new Color(0x00DD00);
				w = (float) (f.friendlyPoints * 2 - points) / (float) f.friendlyPoints;
			} else {
				h = new Color(0xDD0000);
				w = (float) (f.neutralPoints - points) / (float) f.neutralPoints;
				nextPoint = f.neutralPoints;
			}

			if (w < 0.0f) {
				w = 0.0f;
			} else if (w > 1.0f) {
				w = 1.0f;
			}
			int em = (int) (89.0f * w), ew = 89 - em;
			if (em > 0) {
				GlStateManager.color(1.0f, 1.0f, 1.0f, 0.65f);
				GlStateManager.pushMatrix();
				GlStateManager.scale(scaleW, scaleH, 1.0f);
				drawTexturedModalRect(90 - em, 12, 256 - em, 71, em, 3);
				GlStateManager.popMatrix();
			}
			if (ew > 0) {
				GlStateManager.color(h.getRed() / 255.0f, h.getGreen() / 255.0f, h.getBlue() / 255.0f, 0.65f);
				GlStateManager.pushMatrix();
				GlStateManager.scale(scaleW, scaleH, 1.0f);
				drawTexturedModalRect(1, 12, 167, 71, ew, 3);
				GlStateManager.popMatrix();
			}
			draw(f.getName(), (int) (3.0f * scaleW), (int) (2.0f * scaleH), questLogColor, (int) (87.0f * scaleW));

			if (isMouseHover(mouseX, mouseY, (int) (guiLLeft + (i > 4 ? 105.0f : 0) * scaleW), (int) (guiLTop + (i % 8) * 19.0f * scaleH), (int) (98.0f * scaleW), (int) (16.0f * scaleH))) {
				List<ITextComponent> hover = new ArrayList<>();
				// GM
				if (f.hideFaction) { hover.add(Component.translatable("faction.hover.hidden").getParent()); }
				// name
				Component hName = Component.literal("");
				if (player.isCreative()) { hName.append(Component.literal("ID:" + f.id + "; ").withStyle(TextFormatting.GRAY).getParent()); }
				hover.add(hName.append(Component.translatable("gui.name").withStyle(TextFormatting.GRAY))
						.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
						.append(Component.translatable(f.getName()).withStyle(TextFormatting.RESET))
						.getParent());
				// attitude
				Component attitude = Component.empty()
						.append(Component.translatable("gui.attitude").withStyle(TextFormatting.GRAY))
						.append(Component.literal(": ").withStyle(TextFormatting.GRAY));
				if (t == 0) { attitude.append(Component.translatable("faction.unfriendly").withStyle(TextFormatting.DARK_RED)); }
				else if (t == 2) { attitude.append(Component.translatable("faction.friendly").withStyle(TextFormatting.DARK_GREEN)); }
				else { attitude.append(Component.translatable("faction.neutral").withStyle(TextFormatting.GOLD)); }
				hover.add(attitude.getParent());
				// points
				hover.add(Component.empty()
						.append(Component.translatable("faction.points").withStyle(TextFormatting.GRAY))
						.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
						.append(Component.translatable(points + (nextPoint != 0 ? "/" + nextPoint : "")).withStyle(TextFormatting.RESET))
						.getParent());
				if (!f.description.getFormattedText().isEmpty()) {
					hover.add(Component.translatable("gui.description").withStyle(TextFormatting.GRAY).getParent());
					hover.add(f.description);
				}
				setHoverText(hover);
			}
			mc.getTextureManager().bindTexture(f.flag);
            mc.getTextureManager().getTexture(f.flag);
            GlStateManager.pushMatrix();
            GlStateManager.translate(90.0f * scaleW, scaleH, 0.0f);
            GlStateManager.scale(0.175f, 0.11f, 1.0f);
            GlStateManager.scale(scaleW, scaleH, 1.0f);
            GlStateManager.color(2.0f, 2.0f, 2.0f, 1.0f);
            drawTexturedModalRect(0, 0, 4, 4, 40, 128);
            GlStateManager.popMatrix();
            i++;
			p++;
			if (i == 16) {
				break;
			}
		}
		GlStateManager.popMatrix();
	}

	protected void drawQuestLog(int mouseX, int mouseY) {
		if (categories.isEmpty()) {
			draw(Component.translatable("quest.noquests").getFormattedText(), guiLLeft, guiLTop, questLogColor, (int) (-98.0f * scaleW));
			return;
		}
		List<ITextComponent> hover = new ArrayList<>();

		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLeft, guiTopLog + 23.5f * scaleH, 0.0f);
		mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
		if (catRow > 0) { // pre Cats
			if (isMouseHover(mouseX, mouseY, guiLeft - (int) (17.0f * scaleW), (int) (guiTopLog + 7.5f * scaleH), (int) (18.0f * scaleW), (int) (16.0f * scaleH))) {
				hoverButton = 16;
			} // pre cat list;
			GlStateManager.pushMatrix();
			GlStateManager.scale(scaleW, scaleH, 1.0f);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			drawTexturedModalRect(-17, -16, 111, hoverButton == 16 ? 30 : 46, 18, 16);
			GlStateManager.popMatrix();
		}
		if (categories.size() - (catRow + 1) * 8 > 0) { // next Cats
			if (isMouseHover(mouseX, mouseY, guiLeft - (int) (17.0f * scaleW), (int) (guiTopLog + 151.5f * scaleH), (int) (18.0f * scaleW), (int) (16.0f * scaleH))) {
				hoverButton = 17;
			} // next cat list;
			GlStateManager.pushMatrix();
			GlStateManager.scale(scaleW, scaleH, 1.0f);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			drawTexturedModalRect(-17, 128, 129, hoverButton == 17 ? 30 : 46, 18, 16);
			GlStateManager.popMatrix();
		}
		int i = 0, p = 0, st = catRow * 8;
		String selectCat = "";
		for (String catName : categories.keySet()) {
			if (p < st) {
				if (catSelect == p && step < 0) { selectCat = catName; }
				p++;
				continue;
			}
			int catW = ClientProxy.LogFont.width(catName) + 10 + i;
			mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
			if (isMouseHover(mouseX, mouseY, guiLeft + (int) ((5 - catW) * scaleW), (int) (guiTopLog + (23.5f + i * 16.0f) * scaleH), (int) (catW * scaleH), (int) (16.0f * scaleH))) {
				hoverButton = 7 + i;
			} // 7/15-tab categories;
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.pushMatrix();
			GlStateManager.scale(scaleW, scaleH, 1.0f);
			drawTexturedModalRect(4 - (int) (catW / scaleW) + i, i * 16, 0, 90 + (catSelect == p || hoverButton == 7 + i ? 0 : 16), (int) (catW / scaleW), 16);
			GlStateManager.popMatrix();
			if (catSelect == p && step < 0) {
				selectCat = catName;
				GlStateManager.pushMatrix();
				GlStateManager.scale(scaleW, scaleH, 1.0f);
				drawTexturedModalRect(3 + i, i * 16, 234 + i, 90, 22 - i, 16);
				GlStateManager.popMatrix();
			}
			StringBuilder name = new StringBuilder();
			for (int j = 0; j < catName.length(); j++) {
				if (ClientProxy.LogFont.width(name.toString() + catName.charAt(j)) > catW - 5) {
					break;
				}
				name.append(catName.charAt(j));
			}
			draw(catName, 4 - catW + 7 + i, (int) ((16.0f * scaleH - 10.0f) / 2.0f + (i * 16.0f) * scaleH), questLogColor, 90 + i);
			i++;
			p++;
			if (i >= 8) {
				break;
			}
		}
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		GlStateManager.popMatrix();

		if (step != -1) { return; }
		if (activeQuest != null) {
			int first = 0;
			// NPC
			if (activeQuest.qData.quest.completion == EnumQuestCompletion.Npc && activeQuest.npc != null) {
				if (page == 0) {
					GlStateManager.pushMatrix();
					GL11.glEnable(GL11.GL_SCISSOR_TEST);
					int c = sw.getScaledWidth() < mc.displayWidth
							? (int) Math.round((double) mc.displayWidth / (double) sw.getScaledWidth())
							: 1;
					GL11.glScissor(((int) (guiLLeft + 22.0f * scaleW) * c), (int) (guiLTop + (12.0f * scaleH + 81.0f) * scaleH) * c,
							(int) (54.0f * scaleW) * c, (int) (38.0f * scaleH) * c);
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

					String modelName = "";
					if (activeQuest.npc.display.getModel() != null) { modelName = activeQuest.npc.display.getModel(); }
					else {
						ResourceLocation location = EntityList.getKey(activeQuest.npc);
						if (location != null) { modelName = location.toString(); }
					}
					float[] offsets = GuiLog.preDrawEntity(modelName, activeQuest.npc);
					drawNpc(activeQuest.npc, (int) (74.0f * scaleW + offsets[0]),
							70 + (int) (41.0f * scaleH + offsets[1]),
							offsets[2], 30, 0, 1);

					GL11.glDisable(GL11.GL_SCISSOR_TEST);
					GlStateManager.popMatrix();

					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLLeft + 16.5f * scaleW, guiLTop - 4.0f * scaleH, 500.0f);
					GlStateManager.scale(scaleW, scaleH, 1.0f);
					GlStateManager.enableBlend();
					GlStateManager.color(3.0f, 3.0f, 3.0f, 1.0f);
					mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
					drawTexturedModalRect(0, 0, 193, 0, 63, 52);
					GlStateManager.popMatrix();
				}
				first = (int) (44.0f * scaleH);
			}
			// Text
			ItemStack[] stacks = activeQuest.stacks.toArray(new ItemStack[0]);
			int j = 0, k = 0;
			for (int l = 0; l < 2; l++) {
				Map<Integer, List<String>> mapText = activeQuest.getText(first, player);
				if (page * 2 > mapText.size()) { page = (int) Math.floor(mapText.size() / 2.0f); }
				List<String> list = mapText.get(page * 2 + l);
				if (list != null) {
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLLeft, guiLTop, 501.0d);
					int h = 0;
					int xPos, yPos;
					float scale = (float) fontHeight / 16.0f;
					int size = (int) (fontHeight * scaleH);
					for (String line : list) {
						xPos = (int) ((l == 1 ? 105.0f : 0.0f) * scaleW);
						yPos = (page == 0 && l == 0 ? first : 0) + h * fontHeight;
						// item stack
						if (line.contains(" " + ((char) 0xffff) + " ") || line.contains(((char) 0xffff) + " ")) {
							String preText;
							String postText;
							String stackText = " ";
							while (ClientProxy.LogFont.width(stackText) < size + 1) { stackText += " "; }
							if (line.contains(" " + ((char) 0xffff) + " ")) {
								preText = line.substring(0, line.indexOf(" " + ((char) 0xffff) + " "));
								postText = line.substring(line.indexOf(" " + ((char) 0xffff) + " ") + 3);
							}
							else {
								preText = line.substring(0, line.indexOf(((char) 0xffff) + " "));
								postText = line.substring(line.indexOf(((char) 0xffff) + " ") + 2);
							}
							if (j < stacks.length) {
								int pos = ClientProxy.LogFont.width(preText) + 1;
								ItemStack stack = stacks[j];
								if (isMouseHover(mouseX, mouseY, guiLLeft + pos + xPos, guiLTop + yPos, size, size)) {
									setHoverText(stack.getTooltip(player, mc.gameSettings.advancedItemTooltips ? TooltipFlags.ADVANCED : TooltipFlags.NORMAL));
								}
								GlStateManager.pushMatrix();
								GlStateManager.translate(pos + xPos, yPos, 0.0f);
								GlStateManager.scale(scale, scale, scale);
								zLevel = 100.0f;
								itemRender.zLevel = 100.0f;
								GlStateManager.enableLighting();
								GlStateManager.enableRescaleNormal();
								RenderHelper.enableGUIStandardItemLighting();
								itemRender.renderItemAndEffectIntoGUI(stack, 0, 0);
								RenderHelper.disableStandardItemLighting();
								GlStateManager.disableLighting();
								itemRender.zLevel = 0.0f;
								zLevel = 0.0f;
								GlStateManager.popMatrix();
								j++;
							}
							line = preText + stackText + postText;
						}
						// entity
						if (line.contains((char) 0xfffe + " ")) {
							String preText = line.substring(0, line.indexOf((char) 0xfffe + " "));
							String postText = line.substring(line.indexOf((char) 0xfffe + " ") + 1);
							String entityText = " ";
							while (ClientProxy.LogFont.width(entityText) < size + 1) { entityText += " "; }
							if (activeQuest.entitys.containsKey(k)) {
								int pos = ClientProxy.LogFont.width(preText) + 1;
								if (isMouseHover(mouseX, mouseY, guiLLeft + pos + xPos, guiLTop + yPos + 1.5f * scaleH, size, size)) {
									if (!hoverMob(mouseX, mouseY, activeQuest.entitys.get(k))) {
										setHoverText("quest.hover.err.log.entity");
									}
								}
								GlStateManager.pushMatrix();
								GlStateManager.enableAlpha();
								GlStateManager.enableBlend();
								GlStateManager.translate(pos + xPos, yPos + 1.5f * scaleH, 0.0f);
								GlStateManager.scale(scale * 0.45f, scale * 0.225f, scale);
								GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
								mc.getTextureManager().bindTexture(killIcon);
								drawTexturedModalRect(0, 0, 32, 64, 32, 64);
								GlStateManager.popMatrix();
							}
							line = preText + entityText + postText;
							k++;
						}
						draw(line, xPos, yPos, questLogColor, (int) (98.0f * scaleW));
						h++;
					}
					GlStateManager.popMatrix();
				}
			}
			// buttons
			if (page > 0) {
				GlStateManager.pushMatrix();
				GlStateManager.enableBlend();
				GlStateManager.translate(guiLLeft - 5.0f * scaleW, guiLTop + 160.0f * scaleH, 0.0f);
				if (isMouseHover(mouseX, mouseY, (int) (guiLLeft - 5.0f * scaleW), (int) (guiLTop + 160.0f * scaleH), 18, 10)) { hoverButton = 5; } // pre cat list;
				mc.getTextureManager().bindTexture(GuiLog.bookGuiTextures);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				drawTexturedModalRect(0, 0, hoverButton == 5 ? 26 : 3, 207, 18, 10);
				GlStateManager.popMatrix();
			}
			if (page + 2 != activeQuest.size() && page * 2 < activeQuest.size()) {
				GlStateManager.pushMatrix();
				GlStateManager.enableBlend();
				GlStateManager.translate(guiLeft + 230.0f * scaleW, guiLTop + 160.0f * scaleH, 0.0f);
				if (isMouseHover(mouseX, mouseY, (int) (guiLeft + 230.0f * scaleW), (int) (guiLTop + 160.0f * scaleH), 18, 10)) { hoverButton = 4; } // next cat list;
				mc.getTextureManager().bindTexture(GuiLog.bookGuiTextures);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				drawTexturedModalRect(0, 0, hoverButton == 4 ? 26 : 3, 194, 18, 10);
				GlStateManager.popMatrix();
			}
		}
		else if (quests.containsKey(selectCat)) {
			if (page > 0) {
				GlStateManager.pushMatrix();
				GlStateManager.enableBlend();
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				GlStateManager.translate(guiLLeft - 5.0f * scaleW, guiLTop + 160.0f * scaleH, 0.0f);
				if (isMouseHover(mouseX, mouseY, (int) (guiLLeft - 5.0f * scaleW), (int) (guiLTop + 160.0f * scaleH),
						18, 10)) {
					hoverButton = 5;
				} // pre cat list;
				mc.getTextureManager().bindTexture(GuiLog.bookGuiTextures);
				drawTexturedModalRect(0, 0, hoverButton == 5 ? 26 : 3, 207, 18, 10);
				GlStateManager.popMatrix();
			}
			if (Math.floor(quests.size() / 10.d) > page) {
				GlStateManager.pushMatrix();
				GlStateManager.translate(guiLeft + 230.0f * scaleW, guiLTop + 160.0f * scaleH, 0.0f);
				if (isMouseHover(mouseX, mouseY, (int) (guiLeft + 230.0f * scaleW), (int) (guiLTop + 160.0f * scaleH),
						18, 10)) {
					hoverButton = 4;
				} // next cat list;
				mc.getTextureManager().bindTexture(GuiLog.bookGuiTextures);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				drawTexturedModalRect(0, 0, hoverButton == 4 ? 26 : 3, 194, 18, 10);
				GlStateManager.popMatrix();
			}
			i = 0;
			p = 0;
            Color color = categories.get(selectCat);
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.translate(guiLLeft, guiLTop - 1.5f * scaleH, 0.0f);
			for (int id : quests.get(selectCat).keySet()) {
				if (p < page * 16) {
					p++;
					continue;
				}
				if (i == 5) { GlStateManager.translate(105.0f * scaleW, -124.0f * scaleH, 0.0f); }
				else if (i % 5 != 0) { GlStateManager.translate(0.0f, 31.0f * scaleH, 0.0f); }

				GlStateManager.color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f,
						1.0f);
				mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
				GlStateManager.pushMatrix();
				GlStateManager.scale(scaleW, scaleH, 1.0f);
				drawTexturedModalRect(0, 0, 0, 0, 98, 30);
				GlStateManager.popMatrix();
				QuestData qd = quests.get(selectCat).get(id);
				Quest quest = qd.quest;

				GlStateManager.pushMatrix();
				GlStateManager.translate(3.0f * scaleW, 3.0f * scaleH, 0.0f);
				mc.getTextureManager().bindTexture(quest.icon);
				GlStateManager.scale(0.09375f, 0.09375f, 1.0f);
				GlStateManager.scale(scaleW, scaleH, 1.0f);
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				drawTexturedModalRect(0, 0, 0, 0, 256, 256);
				GlStateManager.popMatrix();

				int qxPos = (int) (guiLLeft + (i > 4 ? 105 : 0) * scaleW);
				int qyPos = (int) (guiLTop + (-1.5f + (i % 5) * 31.0f) * scaleH);

				boolean hasExtraButton = quest.extraButton != 0 || player.isCreative();
				if (hasExtraButton) {
					GlStateManager.pushMatrix();
					GlStateManager.translate(87.0f * scaleW, 19.0f * scaleH, 0.0f);
					GlStateManager.scale(scaleW, scaleH, 1.0f);
					int xo = quest.extraButton == 0 ? 9 : quest.extraButton * 9;
					if (quest.extraButton == 0 && player.isCreative()) {
						drawGradientRect(1, 1, 8, 8, 0x20FF0000, 0x80FF0000);
						GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
						xo = (int) ((System.currentTimeMillis() % 5000) / 1000) * 9 + 9;
					}
					mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
					if (isMouseHover(mouseX, mouseY, qxPos + (int) (87.0f * scaleW), qyPos + (int) (19.0f * scaleH),
							(int) (9.0f * scaleW), (int) (9.0f * scaleH))) {
						hoverButton = 30;
						hoverQuestId = id;
					}
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
					drawTexturedModalRect(0, 0, 116 + xo, hoverButton == 30 && hoverQuestId == id ? 9 : 0, 9, 9);
					GlStateManager.popMatrix();
				}

				boolean hasCompassButton = quest.hasCompassSettings() && (CustomNpcs.TypeShowQuestCompass != 4 || player.isCreative());
				if (hasCompassButton) {
					GlStateManager.pushMatrix();
					GlStateManager.translate((87.0f - (hasExtraButton ? 9.0f : 0.0f)) * scaleW, 19.0f * scaleH, 0.0f);
					GlStateManager.scale(scaleW, scaleH, 1.0f);
					if (CustomNpcs.TypeShowQuestCompass == 4 && player.isCreative()) {
						drawGradientRect(1, 1, 8, 8, 0x20FF0000, 0x80FF0000);
						GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
					}
					mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
					if (isMouseHover(mouseX, mouseY, qxPos + (int) ((87.0f - (hasExtraButton ? 9.0f : 0.0f)) * scaleW),
							qyPos + (int) (19.0f * scaleH), (int) (9.0f * scaleW), (int) (9.0f * scaleH))) {
						hoverButton = 31;
						hoverQuestId = id;
					}
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
					drawTexturedModalRect(0, 0, 107 + (compassData.questID == quest.id ? 0 : 9), hoverButton == 31 && hoverQuestId == id ? 9 : 0, 9, 9);
					GlStateManager.popMatrix();
				}

				boolean hasCancelableButton = quest.cancelable || player.isCreative();
				if (hasCancelableButton) {
					GlStateManager.pushMatrix();
					final float v = 87.0f - (hasExtraButton ? 9.0f : 0.0f) - (hasCompassButton ? 9.0f : 0.0f);
					GlStateManager.translate(
							v * scaleW,
							19.0f * scaleH, 0.0f);
					GlStateManager.scale(scaleW, scaleH, 1.0f);
					if (!quest.cancelable && player.isCreative()) {
						drawGradientRect(1, 1, 8, 8, 0x20FF0000, 0x80FF0000);
						GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
					}
					mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
					if (isMouseHover(mouseX, mouseY,
							qxPos + (int) (v
									* scaleW),
							qyPos + (int) (19.0f * scaleH), (int) (9.0f * scaleW), (int) (9.0f * scaleH))) {
						hoverButton = 32;
						hoverQuestId = id;
					}
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
					drawTexturedModalRect(0, 0, 98, hoverButton == 32 && hoverQuestId == id ? 9 : 0, 9, 9);
					GlStateManager.popMatrix();
				}

				GlStateManager.pushMatrix();
				GlStateManager.translate(29.0f * scaleW, 3.0f * scaleH, 0.0f);
				StringBuilder name = new StringBuilder();
				ITextComponent qName = quest.getTitle();
                if (ClientProxy.LogFont.width(qName.getFormattedText()) < 67.0f * scaleW) {
					name = new StringBuilder(qName.getFormattedText());
				} else {
					for (int j = 0; j < qName.getFormattedText().length(); j++) {
						if (ClientProxy.LogFont.width(name.toString() + qName.getFormattedText().charAt(j) + "...") >= 67.0f * scaleW) {
							break;
						}
						name.append(qName.getFormattedText().charAt(j));
					}
					name.append("...");
				}
				qName.getStyle().setColor(TextFormatting.RESET);
				ClientProxy.LogFont.draw(name.toString(), 0, 0, questLogColor);
				IQuestObjective[] objs;
				try { objs = quest.getObjectives(player); }
				catch (Exception e) { objs = new IQuestObjective[0]; }
                int j = 0;
				for (IQuestObjective iqo : objs) {
					if (iqo.isCompleted()) {
						j++;
					}
				}
				String progress = j + " / " + objs.length;
				ClientProxy.LogFont.draw(progress, 0, 10, questLogColor);

				if (hoverButton > 29 && hoverQuestId == id) {
					if (hoverButton == 30) {
						hover.add(Component.translatable(quest.extraButtonText.isEmpty() ? "quest.hover.extra.button" : quest.extraButtonText).getParent());
						if (quest.extraButton == 0 && player.isCreative()) {
							hover.add(Component.translatable("quest.hover.gm.info").getParent());
						}
					} else if (hoverButton == 31) {
						hover.add(Component.translatable("quest.hover.compass." + (compassData.questID == quest.id)).getParent());
						if (CustomNpcs.TypeShowQuestCompass == 4 && player.isCreative()) {
							hover.add(Component.translatable("quest.hover.gm.info").getParent());
						}
					} else if (hoverButton == 32) {
						hover.add(Component.translatable("drop.quest", quest.getName()).getParent());
						if (!quest.cancelable && player.isCreative()) {
							hover.add(Component.translatable("quest.hover.gm.info").getParent());
						}
					}
				}
				else if (isMouseHover(mouseX, mouseY, qxPos, qyPos, (int) (98.0f * scaleW), (int) (30.0f * scaleH))) {
					hoverButton = 6;
					hoverQuestId = id;
					hover.add(Component.empty()
							.append(Component.translatable("drop.category").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(Component.literal(selectCat).withStyle(TextFormatting.RESET))
							.getParent());
					hover.add(Component.empty()
							.append(Component.translatable("gui.name").withStyle(TextFormatting.GRAY))
							.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
							.append(qName)
							.getParent());
					hover.add(Component.empty()
							.append(Component.translatable("gui.progress", ": ").withStyle(TextFormatting.GRAY))
							.append(Component.literal(progress).withStyle(j >= objs.length ? TextFormatting.GREEN : TextFormatting.RED))
							.getParent());
					if (quest.completion == EnumQuestCompletion.Npc && quest.completer != null) {
						hover.add(Component.translatable("quest.completewith", quest.completer.getName()).getParent());
					}
				}
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				GlStateManager.popMatrix();
				i++;
				p++;
				if (i == 10) { break; }
			}
			GlStateManager.popMatrix();
			if (!hover.isEmpty()) { setHoverText(hover); }
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		factionData = data.factionData;
		compassData = data.compass;
		// Back
		GlStateManager.pushMatrix();
		drawGradientRect(0, 0, mc.displayWidth, mc.displayHeight, 0xAA000000, 0xAA000000);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		GlStateManager.popMatrix();
		// Animations
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		if (tick >= 0) {
			if (tick == 0) { partialTicks = 0.0f; }
			float part = (float) tick + partialTicks;
			float cos = (float) Math.cos(90.0d * part / (double) milliTick * Math.PI / 180.0d);
			if (cos < 0.0f) { cos = 0.0f; }
			else if (cos > 1.0f) { cos = 1.0f; }
			switch (step) {
				case 0: {
					mc.getTextureManager().bindTexture(GuiLog.ql.get(2));
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiCenter + (1.0f - cos) * (guiCenter + 50.0f),
							guiTopLog + (1.0f - cos) * 250.0f, 0.0f);
					GlStateManager.scale(scaleW, scaleH, 1.0f);
					drawTexturedModalRect(0, 0, 0, 0, 128, 175);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 1;
						setNextTick(21, true);
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.down",
								(float) player.posX, (float) player.posY, (float) player.posZ, 1.0f,
								0.75f + 0.25f * rnd.nextFloat());
					}
					break;
				} // start open
				case 1: {
					// right
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiCenter, guiTopLog, 0.0f);
					GlStateManager.scale(scaleW, scaleH, 1.0f);
					mc.getTextureManager().bindTexture(GuiLog.ql.get(0));
					drawTexturedModalRect(0, 0, 128, 0, 128, 175);
					GlStateManager.popMatrix();
					// left
					boolean up = tick >= milliTick / 2;
					GlStateManager.pushMatrix();
					if (up) {
						part = (float) (tick - (milliTick / 2)) + partialTicks;
						cos = (float) Math.cos(90.0d * part / ((double) milliTick / 2.0d) * Math.PI / 180.0d);
						if (cos < 0.0f) {
							cos = 0.0f;
						} else if (cos > 1.0f) {
							cos = 1.0f;
						}
						GlStateManager.translate(guiCenter, guiTopLog, 0.0f);
						GlStateManager.scale(1.0f - cos, 1.0f, 1.0f);
						GlStateManager.scale(scaleW, scaleH, 1.0f);
						mc.getTextureManager().bindTexture(GuiLog.ql.get(2));
						drawTexturedModalRect(0, 0, 0, 0, 128, 175);
					} else {
						part = (float) tick + partialTicks;
						cos = (float) Math.cos(90.0d * part / ((double) milliTick / 2.0d) * Math.PI / 180.0d);
						if (cos < 0.0f) {
							cos = 0.0f;
						} else if (cos > 1.0f) {
							cos = 1.0f;
						}
						GlStateManager.translate(guiCenter - cos * width / 2.0f, guiTopLog, 0.0f);
						GlStateManager.scale(cos, 1.0f, 1.0f);
						GlStateManager.scale(scaleW, scaleH, 1.0f);
						mc.getTextureManager().bindTexture(GuiLog.ql.get(0));
						drawTexturedModalRect(0, 0, 0, 0, 128, 175);
					}
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 2;
						setNextTick(11, true);
					}
					break;
				} // open
				case 2: {
					// place
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft, guiTopLog, 0.0f);
					GlStateManager.scale(scaleW, scaleH, 1.0f);
					mc.getTextureManager().bindTexture(GuiLog.ql.get(0));
					drawTexturedModalRect(0, 0, 0, 0, 256, 175);
					if (temp > 0) {
						mc.getTextureManager().bindTexture(GuiLog.ql.get(1));
						drawTexturedModalRect(0, 0, 0, 0, 256, 175);
					}
					GlStateManager.popMatrix();

					// left
					boolean up = tick >= milliTick / 2;
					GlStateManager.pushMatrix();
					if (up) {
						part = (float) (tick - (milliTick / 2)) + partialTicks;
						cos = (float) Math.cos(90.0d * part / ((double) milliTick / 2.0d) * Math.PI / 180.0d);
						if (cos < 0.0f) {
							cos = 0.0f;
						} else if (cos > 1.0f) {
							cos = 1.0f;
						}
						GlStateManager.translate(guiCenter, guiTopLog, 0.0f);
						GlStateManager.scale(1.0f - cos, 1.0f, 1.0f);
						GlStateManager.scale(scaleW, scaleH, 1.0f);
						mc.getTextureManager().bindTexture(GuiLog.ql.get(3));
						drawTexturedModalRect(0, 0, 128, 0, 128, 175);
					} else {
						part = (float) tick + partialTicks;
						cos = (float) Math.cos(90.0d * part / ((double) milliTick / 2.0d) * Math.PI / 180.0d);
						if (cos < 0.0f) {
							cos = 0.0f;
						} else if (cos > 1.0f) {
							cos = 1.0f;
						}
						GlStateManager.translate(guiCenter - cos * width / 2.0f, guiTopLog, 0.0f);
						GlStateManager.scale(cos, 1.0f, 1.0f);
						GlStateManager.scale(scaleW, scaleH, 1.0f);
						mc.getTextureManager().bindTexture(GuiLog.ql.get(3));
						drawTexturedModalRect(0, 0, 0, 0, 128, 175);
					}
					GlStateManager.popMatrix();

					if (tick == milliTick) {
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.sheet",
								(float) player.posX, (float) player.posY, (float) player.posZ, 1.0f,
								0.8f + 0.4f * rnd.nextFloat());
					}
					int time = 11;
					if (tick == 0) {
						if (temp < 3) {
							temp++;
							step = 2;
                        } else {
							temp = 0;
							step = 3;
							time = 21;
                        }
						setNextTick(time, true);
					}
					break;
				} // open lists
				case 3: {
					// Tabs
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 10, guiTop + (1.0f - cos) * 28.0f, 0.0f);
					mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
					drawTexturedModalRect(0, 0, 0, 30, 28, 30);
					GlStateManager.translate(33.0f, 0.0f, 0.0f);
					drawTexturedModalRect(0, 0, 0, 30, 28, 30);
					GlStateManager.translate(33.0f, 0.0f, 0.0f);
					drawTexturedModalRect(0, 0, 0, 30, 28, 30);
					if (CustomNpcs.TypeShowQuestCompass != 4 || player.isCreative()) {
						GlStateManager.translate(-114.0f + 256.0f * scaleW, 0.0f, 0.0f);
						drawTexturedModalRect(0, 0, 0, 30, 28, 30);
					}
					GlStateManager.popMatrix();
					// place
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft, guiTopLog, 0.0f);
					GlStateManager.scale(scaleW, scaleH, 1.0f);
					mc.getTextureManager().bindTexture(GuiLog.ql.get(0));
					drawTexturedModalRect(0, 0, 0, 0, 256, 175);
					mc.getTextureManager().bindTexture(GuiLog.ql.get(1));
					drawTexturedModalRect(0, 0, 0, 0, 256, 175);
					GlStateManager.popMatrix();

					if (tick == 0) {
						step = type + 4;
						setNextTick(21, true);
					}
					break;
				} // tab open
				case 4: {
					drawBox(mouseX, mouseY);
					if (!categories.isEmpty()) {
						GlStateManager.pushMatrix();
						GlStateManager.translate(guiLeft, guiTopLog + 23.5f * scaleH, 0.0f);
						mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
						int i = 0, p = 0;
						for (String catName : categories.keySet()) {
							int catW = (int) ((ClientProxy.LogFont.width(catName) + 10 + i) * cos);
							mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
							if (isMouseHover(mouseX, mouseY, guiLeft + (int) ((5 - catW) * scaleW),
									(int) (guiTopLog + (23.5f + i * 16.0f) * scaleH), (int) (catW * scaleH),
									(int) (16.0f * scaleH))) {
								hoverButton = 7 + i;
							} // 7/15-tab categories;
							GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
							GlStateManager.pushMatrix();
							GlStateManager.scale(scaleW, scaleH, 1.0f);
							drawTexturedModalRect(4 - (int) (catW / scaleW) + i, i * 16, 0,
									90 + (catSelect == p || hoverButton == 7 + i ? 0 : 16), (int) (catW / scaleW), 16);
							GlStateManager.popMatrix();
							StringBuilder name = new StringBuilder();
							for (int j = 0; j < catName.length(); j++) {
								if (ClientProxy.LogFont.width(name.toString() + catName.charAt(j)) > catW - 5) {
									break;
								}
								name.append(catName.charAt(j));
							}
							ClientProxy.LogFont.draw(name.toString(), 4 - catW + 10 + i, (16.0f * scaleH - 10.0f) / 2.0f + (i * 16) * scaleH, questLogColor);
							i++;
							p++;
							if (i >= 8) { break; }
						}
						GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
						GlStateManager.popMatrix();
					} else {
						tick = 0;
					}
					if (tick == 0) {
						step = toPrePage ? 10 : 11;
						setNextTick(11, true);
					}
					break;
				} // quest tab open
				case 5: {
					drawBox(mouseX, mouseY);
					if (tick == 0) {
						step = toPrePage ? 10 : 11;
						setNextTick(11, true);
					}
					break;
				} // faction open
				case 6: {
					drawBox(mouseX, mouseY);
					if (tick == 0) {
						step = toPrePage ? 10 : 11;
						setNextTick(11, true);
					}
					break;
				} // compass open
				case 7: {
					drawBox(mouseX, mouseY);
					if (!categories.isEmpty()) {
						temp = 1;
						GlStateManager.pushMatrix();
						GlStateManager.translate(guiLeft, guiTopLog + 7.5f, 0.0f);
						GlStateManager.translate(0.0f, 16.0f, 0.0f);
						int i = 0, p = 0, st = catRow * 8;
						for (String catName : categories.keySet()) {
							if (p < st) {
								p++;
								continue;
							}
							int catW = (int) ((ClientProxy.LogFont.width(catName) + 10) * (1.0f - cos));
							mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
							drawTexturedModalRect(5 - catW, i * 16, 0, 90 + (catSelect == p ? 0 : 16), catW, 16);
							StringBuilder name = new StringBuilder();
							for (int j = 0; j < catName.length(); j++) {
								if (ClientProxy.LogFont.width(name.toString() + catName.charAt(j)) > catW - 5) {
									break;
								}
								name.append(catName.charAt(j));
							}
							ClientProxy.LogFont.draw(name.toString(), 10 - catW, 3 + i * 16, questLogColor);
							GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
							i++;
							if (i >= 8) {
								break;
							}
						}
						GlStateManager.popMatrix();
					} else {
						tick = 0;
						temp = 0;
					}

					if (tick == 0) {
						if (type < 0) {
							step = 12;
						} else {
							step = type + 4;
						}
						setNextTick(21, true);
					}
					break;
				} // quest tab close
				case 8: {
					drawBox(mouseX, mouseY);

					if (tick == 0) {
						if (type < 0) {
							step = 12;
						} else {
							step = type + 4;
						}
						setNextTick(21, true);
					}
					break;
				} // faction close
				case 9: {
					drawBox(mouseX, mouseY);

					if (tick == 0) {
						if (type < 0) {
							step = 12;
						} else {
							step = type + 4;
						}
						setNextTick(21, true);
					}
					break;
				} // compass close
				case 10: {
					drawBox(mouseX, mouseY);
					boolean up = tick >= milliTick / 2;
					GlStateManager.pushMatrix();
					GlStateManager.enableBlend();
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
					if (up) {
						part = (float) (tick - (milliTick / 2)) + partialTicks;
						cos = (float) Math.cos(90.0d * part / ((double) milliTick / 2.0d) * Math.PI / 180.0d);
						if (cos < 0.0f) {
							cos = 0.0f;
						} else if (cos > 1.0f) {
							cos = 1.0f;
						}
						GlStateManager.translate(guiCenter, guiTopLog, 0.0f);
						GlStateManager.scale(1.0f - cos, 1.0f, 1.0f);
						GlStateManager.scale(scaleW, scaleH, 1.0f);
						mc.getTextureManager().bindTexture(GuiLog.ql.get(3));
						drawTexturedModalRect(0, 0, 128, 0, 128, 175);
					} else {
						part = (float) tick + partialTicks;
						cos = (float) Math.cos(90.0d * part / ((double) milliTick / 2.0d) * Math.PI / 180.0d);
						if (cos < 0.0f) {
							cos = 0.0f;
						} else if (cos > 1.0f) {
							cos = 1.0f;
						}
						GlStateManager.translate(guiCenter - cos * width / 2.0f, guiTopLog, 0.0f);
						GlStateManager.scale(cos, 1.0f, 1.0f);
						GlStateManager.scale(scaleW, scaleH, 1.0f);
						mc.getTextureManager().bindTexture(GuiLog.ql.get(3));
						drawTexturedModalRect(0, 0, 0, 0, 128, 175);
					}
					GlStateManager.popMatrix();

					if (tick == milliTick) {
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.sheet",
								(float) player.posX, (float) player.posY, (float) player.posZ, 1.0f,
								0.8f + 0.4f * rnd.nextFloat());
					}
					if (tick == 0) {
						step = -1;
						setNextTick(11, true);
					}
					break;
				} // next page
				case 11: {
					drawBox(mouseX, mouseY);
					boolean up = tick >= milliTick / 2;
					GlStateManager.pushMatrix();
					GlStateManager.enableBlend();
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
					if (up) {
						part = (float) (tick - (milliTick / 2)) + partialTicks;
						cos = (float) Math.cos(90.0d * part / ((double) milliTick / 2.0d) * Math.PI / 180.0d);
						if (cos < 0.0f) {
							cos = 0.0f;
						} else if (cos > 1.0f) {
							cos = 1.0f;
						}
						GlStateManager.translate(guiCenter - (1.0f - cos) * width / 2.0f, guiTopLog, 0.0f);
						GlStateManager.scale((1.0f - cos), 1.0f, 1.0f);
						GlStateManager.scale(scaleW, scaleH, 1.0f);
						mc.getTextureManager().bindTexture(GuiLog.ql.get(3));
						drawTexturedModalRect(0, 0, 0, 0, 128, 175);
					} else {
						part = (float) tick + partialTicks;
						cos = (float) Math.cos(90.0d * part / ((double) milliTick / 2.0d) * Math.PI / 180.0d);
						if (cos < 0.0f) {
							cos = 0.0f;
						} else if (cos > 1.0f) {
							cos = 1.0f;
						}
						GlStateManager.translate(guiCenter, guiTopLog, 0.0f);
						GlStateManager.scale(cos, 1.0f, 1.0f);
						GlStateManager.scale(scaleW, scaleH, 1.0f);
						mc.getTextureManager().bindTexture(GuiLog.ql.get(3));
						drawTexturedModalRect(0, 0, 128, 0, 128, 175);
					}
					GlStateManager.popMatrix();

					if (tick == milliTick) {
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.sheet",
								(float) player.posX, (float) player.posY, (float) player.posZ, 1.0f,
								0.8f + 0.4f * rnd.nextFloat());
					}
					if (tick == 0) {
						step = -1;
						setNextTick(11, true);
					}
					break;
				} // pre page
				case 12: {
					// Tabs
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft + 10, guiTop + cos * 28.0f, 0.0f);
					mc.getTextureManager().bindTexture(GuiLog.ql.get(4));
					drawTexturedModalRect(0, 0, 0, 30, 28, 30);
					GlStateManager.translate(33.0f, 0.0f, 0.0f);
					drawTexturedModalRect(0, 0, 0, 30, 28, 30);
					GlStateManager.translate(33.0f, 0.0f, 0.0f);
					drawTexturedModalRect(0, 0, 0, 30, 28, 30);
					if (CustomNpcs.TypeShowQuestCompass != 4 || player.isCreative()) {
						GlStateManager.translate(-114.0f + 256.0f * scaleW, 0.0f, 0.0f);
						drawTexturedModalRect(0, 0, 0, 30, 28, 30);
					}
					GlStateManager.popMatrix();
					// place
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiLeft, guiTopLog, 0.0f);
					GlStateManager.scale(scaleW, scaleH, 1.0f);
					mc.getTextureManager().bindTexture(GuiLog.ql.get(0));
					drawTexturedModalRect(0, 0, 0, 0, 256, 175);
					mc.getTextureManager().bindTexture(GuiLog.ql.get(1));
					drawTexturedModalRect(0, 0, 0, 0, 256, 175);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 13;
						setNextTick(21, true);
					}
					break;
				} // close tabs
				case 13: {
					// left
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiCenter - 64.0f * cos, guiTopLog, 0.0f);
					mc.getTextureManager().bindTexture(GuiLog.ql.get(0));
					GlStateManager.scale(scaleW, scaleH, 1.0f);
					drawTexturedModalRect(0, 0, 128, 0, 128, 175);
					GlStateManager.popMatrix();
					// right
					boolean up = tick >= milliTick / 2;
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiCenter - 64.0f * cos, guiTopLog, 0.0f);
					if (up) {
						part = (float) (tick - (milliTick / 2)) + partialTicks;
						cos = (float) Math.cos(90.0d * part / ((double) milliTick / 2.0d) * Math.PI / 180.0d);
						if (cos < 0.0f) { cos = 0.0f; }
						else if (cos > 1.0f) { cos = 1.0f; }
						GlStateManager.translate(-128.0f * (1.0d - cos), 0.0f, 0.0f);
						GlStateManager.scale(1.0f - cos, 1.0f, 1.0f);
						GlStateManager.scale(scaleW, scaleH, 1.0f);
						mc.getTextureManager().bindTexture(GuiLog.ql.get(0));
						drawTexturedModalRect(0, 0, 0, 0, 128, 175);
						if (temp > 0) {
							mc.getTextureManager().bindTexture(GuiLog.ql.get(1));
							drawTexturedModalRect(0, 0, 0, 0, 128, 175);
						}
					} else {
						part = (float) tick + partialTicks;
						cos = (float) Math.cos(90.0d * part / ((double) milliTick / 2.0d) * Math.PI / 180.0d);
						if (cos < 0.0f) {
							cos = 0.0f;
						} else if (cos > 1.0f) {
							cos = 1.0f;
						}
						GlStateManager.scale(cos, 1.0f, 1.0f);
						GlStateManager.scale(scaleW, scaleH, 1.0f);
						mc.getTextureManager().bindTexture(GuiLog.ql.get(2));
						drawTexturedModalRect(0, 0, 0, 0, 128, 175);
					}
					GlStateManager.popMatrix();

					if (tick == 0) {
						MusicController.Instance.playSound(SoundCategory.PLAYERS, CustomNpcs.MODID + ":book.down",
								(float) player.posX, (float) player.posY, (float) player.posZ, 1.0f,
								0.75f + 0.25f * rnd.nextFloat());
						step = 14;
						setNextTick(21, true);
					}
					break;
				} // close book
				case 14: {
					GlStateManager.pushMatrix();
					GlStateManager.translate(guiCenter - 64.0f + cos * (guiCenter + 50.0f), guiTopLog + cos * 250.0f, 0.0f);
					GlStateManager.scale(scaleW, scaleH, 1.0f);
					mc.getTextureManager().bindTexture(GuiLog.ql.get(2));
					drawTexturedModalRect(0, 0, 0, 0, 128, 175);
					GlStateManager.popMatrix();
					if (tick == 0) {
						step = 14;
						setNextTick(101, true);
						save();
						if (type == -1) {
							mc.displayGuiScreen(new GuiInventory(player));
						} else {
							setScreen(null);
							mc.setIngameFocus();
						}
					}
					break;
				} // close
			}
			tick--;
			if (step != -1) {
				GlStateManager.popMatrix();
				return;
			}
		}
		drawBox(mouseX, mouseY);
		GlStateManager.popMatrix();

		if (tick < 0 && step == -1) {
			GlStateManager.pushMatrix();
			super.drawScreen(mouseX, mouseY, partialTicks);
			GlStateManager.popMatrix();
		}
		else { drawHoverText(null); }
	}

	protected boolean hoverMob(int mouseX, int mouseY, Entity entity) {
		if (entity == null) { return false; }
		GlStateManager.pushMatrix();
		GlStateManager.translate((guiLeft + 22) * -1, guiTopLog * -1, 300.0d);
		GlStateManager.translate(mouseX, mouseY, 0.0f);
		if (mouseY > sw.getScaledHeight_double() / 2.0d) { GlStateManager.translate(0.0f, -15.0f, 0.0f); }
		else { GlStateManager.translate(0.0f, 45.0f, 0.0f); }

		String modelName = "";
		if (entity instanceof EntityNPCInterface && ((EntityNPCInterface) entity).display.getModel() != null) { modelName = ((EntityNPCInterface) entity).display.getModel(); }
		else {
			ResourceLocation location = EntityList.getKey(entity);
			if (location != null) { modelName = location.toString(); }
		}
		GlStateManager.rotate(180, 1.0f, 0.0f, 0.0f);
		GlStateManager.rotate((mc.world.getTotalWorldTime() % 360) * 5.0f, 0.0f, 1.0f, 0.0f);
		GlStateManager.enableBlend();
		GlStateManager.enableColorMaterial();
		GlStateManager.enableDepth();
		mc.getRenderManager().playerViewY = 180.0f;
		GlStateManager.scale(25.0f, 25.0f, 25.0f);
		entity.ticksExisted = 1;
		GuiLog.preDrawEntity(modelName, entity);
		mc.getRenderManager().renderEntity(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, false);
		GlStateManager.disableRescaleNormal();
		GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GlStateManager.disableTexture2D();
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GlStateManager.popMatrix();
		return true;
	}

	@Override
	public void initGui() {
		super.initGui();
		initScale();
		guiCenter = (int) Math.ceil(sw.getScaledWidth_double() / 2.0d + 15.0d * scaleW);
		width = (int) (256.0f * scaleW);
		height = (int) (203.0f * scaleH);
		guiLeft = guiCenter - (int) (128.0f * scaleW);
		guiTop = 25;
		guiTopLog = guiTop + 28;
		guiLLeft = guiLeft + (int) (26.0f * scaleW);
		guiLRight = guiCenter + (int) (2.0f * scaleW);
		guiLTop = guiTopLog + (int) (8.0f * scaleH);

		if (type == 0) {
			quests.clear();
			categories.clear();
			Collection<QuestData> list = data.questData.activeQuests.values();
			// Quest List
			if (!list.isEmpty()) {
				for (QuestData qd : list) {
					Quest quest = qd.quest;
					String catName = quest.category.getName();
					if (!categories.containsKey(catName)) {
						int r = 128, g = 32, b = 224;
						for (int i = 0; i < catName.length(); i++) {
							switch (i % 3) {
							case 0:
								r += catName.charAt(i);
								break;
							case 1:
								g += catName.charAt(i);
								break;
							case 2:
								b += catName.charAt(i);
								break;
							}
						}
						categories.put(catName, new Color((r * catName.length()) % 256,
								(g * catName.length()) % 256, (b * catName.length()) % 256));
					}
					if (!quests.containsKey(catName)) {
						quests.put(catName, new TreeMap<>());
					}
					quests.get(catName).put(quest.id, qd);
				}
			}
			if (activeQuest != null) {
				activeQuest.reset();
			}
			while (catSelect >= categories.size()) {
				catSelect--;
			}
		} // Quests
		else if (type == 2) {
			int x0 = guiLLeft + 7;
			int x1 = guiLRight + (int) (scaleW);
			int y = (int) (guiLTop + 86.0f * scaleH);
			int lId = 0;
			// Screen Pos
			addLabel(lId++, x0 - (int) (10.0f * scaleW), y - (int) (2.0f * scaleH), "U:")
					.setCustomFont(ClientProxy.LogFont)
					.setColor(questLogColor);
			addTextField(0, x0, y, (int) (40.0f * scaleW),
					(int) (11.0f * scaleH), "" + compassData.screenPos[0])
					.setMinMaxDefault(0.0f, 1.0f, compassData.screenPos[0])
					.setHoverTexts("quest.hover.compass.edit.ups");
			addLabel(lId++, x0 + (int) (45.0f * scaleW), y - (int) (2.0f * scaleH), "V:")
					.setCustomFont(ClientProxy.LogFont)
					.setColor(questLogColor);
			addTextField(1, x0 + (int) (54.0f * scaleW), y,
					(int) (40.0f * scaleW), (int) (11.0f * scaleH), "" + compassData.screenPos[1])
					.setMinMaxDefault(0.0d, 1.0d, compassData.screenPos[1])
					.setHoverTexts("quest.hover.compass.edit.vpos");
			// Scale
			x0 -= 1;
			addLabel(lId++, x0 - (int) (10.0f * scaleW), (y += (int) (18.0f * scaleH)) - (int) (3.0f * scaleH), "S:")
					.setCustomFont(ClientProxy.LogFont)
					.setColor(questLogColor);
			addSlider(0, x0, y, compassData.scale - 0.5f)
					.setSize((int) (94.0f * scaleW), (int) (fontHeight * scaleH))
					.setString(("" + compassData.scale).replace(".", ","))
					.setShowShadow(false)
					.setHoverTexts("quest.hover.compass.edit.scale");
			if (!compassData.isFlat) {
				// Incline
				addLabel(lId++, x0 - (int) (10.0f * scaleW), (y += (int) (17.0f * scaleH)) - (int) (3.0f * scaleH), "T:")
						.setCustomFont(ClientProxy.LogFont)
						.setColor(questLogColor);
				addSlider(1, x0, y, compassData.incline * -0.022222f + 0.5f)
						.setSize((int) (94.0f * scaleW), (int) (fontHeight * scaleH))
						.setString(("" + (45.0f + compassData.incline * -1.0f)).replace(".", ","))
						.setHoverTexts("quest.hover.compass.edit.incline");
				// Rotation
				addLabel(lId, x0 - (int) (10.0f * scaleW), (y += (int) (17.0f * scaleH)) - (int) (3.0f * scaleH), "R:")
						.setCustomFont(ClientProxy.LogFont)
						.setColor(questLogColor);
				addSlider(2, x0, y, compassData.rot * 0.016667f + 0.5f)
						.setSize((int) (94.0f * scaleW), (int) (fontHeight * scaleH))
						.setString(("" + compassData.rot).replace(".", ","))
						.setHoverTexts("quest.hover.compass.edit.rotation");
			}
			y = (int) (guiLTop + 64.0f * scaleH);
			addCheckBox(0, x1, y, "quest.screen.show.quest", null, compassData.showQuestName)
					.setSize((int) (104.0f * scaleW), (int) (fontHeight * scaleH))
					.setColor(questLogColor)
					.setCustomFont(ClientProxy.LogFont);
			addCheckBox(1, x1, y += (int) (14.0f * scaleH), "quest.screen.show.task", null, compassData.showTaskProgress)
					.setSize((int) (104.0f * scaleW), (int) (fontHeight * scaleH))
					.setColor(questLogColor)
					.setCustomFont(ClientProxy.LogFont);
			addCheckBox(3, x1, y += (int) (17.0f * scaleH), "quest.screen.show.dial", null, compassData.showDial)
					.setSize((int) (104.0f * scaleW), (int) (fontHeight * scaleH))
					.setColor(questLogColor)
					.setCustomFont(ClientProxy.LogFont);
			addCheckBox(4, x1, y += (int) (14.0f * scaleH), "quest.screen.compass.type.0", "quest.screen.compass.type.4", compassData.showOfPlayer)
					.setSize((int) (104.0f * scaleW), (int) (fontHeight * scaleH))
					.setColor(questLogColor)
					.setCustomFont(ClientProxy.LogFont);

			addCheckBox(5, x1, y += (int) (17.0f * scaleH), "quest.screen.is.flat", "quest.screen.is.3d", compassData.isFlat)
					.setSize((int) (104.0f * scaleW), (int) (fontHeight * scaleH))
					.setColor(questLogColor)
					.setCustomFont(ClientProxy.LogFont);
			addCheckBox(6, x1, y += (int) (14.0f * scaleH), "quest.screen.log.is.fast", "quest.screen.log.is.slow", compassData.questLogIsFast)
					.setSize((int) (104.0f * scaleW), (int) (fontHeight * scaleH))
					.setColor(questLogColor)
					.setCustomFont(ClientProxy.LogFont);
			addButton(2, x1, y + (int) (14.0f * scaleH), "")
					.setSize((int) (104.0f * scaleW), (int) (fontHeight * scaleH))
					.setTexture(ANIMATION_BUTTONS)
					.setUV(0, 96, 200, 20)
					.setIsVisible(player.isCreative())
					.setColor(questLogColor)
					.setCustomFont(ClientProxy.LogFont)
					.setHoverTexts("quest.screen.hover.compass.global", "quest.screen.hover.compass.type." + CustomNpcs.TypeShowQuestCompass)
					.setVariants("quest.screen.compass.type.0", "quest.screen.compass.type.1", "quest.screen.compass.type.2", "quest.screen.compass.type.3", "quest.screen.compass.type.4")
					.setDisplay(CustomNpcs.TypeShowQuestCompass);
		} // Compass
	}

	public static void initScale() {
		sw = new ScaledResolution(Minecraft.getMinecraft());
		scaleW = ((float) sw.getScaledWidth_double() - 160.0f) / 256.0f;
		scaleH = ((float) sw.getScaledHeight_double() - 78.0f) / 175.0f;
	}

	@Override
	public boolean keyPressed(char typedChar, int keyCode) {
		if (step >= 0) { return false; }
		if (keyCode == Keyboard.KEY_ESCAPE || isInventoryKey(keyCode)) {
			setNextTick(15, false);
			step = type + 7;
			type = keyCode == Keyboard.KEY_ESCAPE ? -2 : -1;
			return true;
		}
		return super.keyPressed(typedChar, keyCode);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (step >= 0) { return false; }
		boolean bo = false;
		if (type == 2) {
			bo = super.mouseClicked(mouseX, mouseY, mouseButton);
			if (mouseX >= (int) (guiLLeft - 3.0f * scaleW) && mouseX <= (int) (guiLLeft + 100.0f * scaleW) && mouseY >= guiLTop + 10 && mouseY <= (int) (guiLTop + 10 + (69.0f * scaleH))) {
				double x = (mouseX - (int) (guiLLeft - 3.0f * scaleW)) / scaleW;
				double y = (mouseY - (guiLTop + 10)) / scaleH;
				compassData.screenPos[0] = (float) (Math.round(x / 103.0d * 1000.0d) / 1000.0d);
				compassData.screenPos[1] = (float) (Math.round(y / 69.0d * 1000.0d) / 1000.0d);
				initGui();
				return true;
			}
		}
		return bo || buttonPress(hoverButton);
	}

	@Override
	public void mouseDragged(GuiSliderNop slider) {
		if (type != 2) { return; }
		switch (slider.id) {
			case 0: {
				compassData.scale = Math.round((slider.sliderValue + 0.5f) * 100.0f) / 100.0f;
				slider.setString(("" + compassData.scale).replace(".", ","));
				break;
			}
			case 1: {
				compassData.incline = Math.round((-45.0f * slider.sliderValue + 22.5f) * 100.0f) / 100.0f;
				slider.setString(("" + (45.0f + compassData.incline * -1.0f)).replace(".", ","));
				break;
			}
			case 2: {
				compassData.rot = Math.round((60.0f * slider.sliderValue - 30.0f) * 100.0f) / 100.0f;
				slider.setString(("" + compassData.rot).replace(".", ","));
				break;
			}
		}
	}

	@Override
	public void mousePressed(GuiSliderNop slider) { }

	@Override
	public void mouseReleased(GuiSliderNop slider) { }

	@Override
	public void save() {
		NBTTagCompound compound = compassData.save(new NBTTagCompound());
		data.compass.load(compound);
		Packets.sendServer(new SPacketSyncUpdate(10, compound));
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound.hasKey("FactionList", 9)) {
			playerFactions.clear();
			NBTTagList list = compound.getTagList("FactionList", 10);
			for (int i = 0; i < list.tagCount(); ++i) {
				Faction faction = new Faction();
				faction.load(list.getCompoundTagAt(i));
				playerFactions.add(faction);
			}
			PlayerFactionData data = new PlayerFactionData();
			data.load(compound);
			for (int id : data.factionData.keySet()) {
				int points = data.factionData.get(id);
				for (Faction faction2 : playerFactions) {
					if (faction2.id == id) { faction2.defaultPoints = points; }
				}
			}
		}
		initGui();
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		if (type != 2) { return; }
		switch (textField.id) {
			case 0: compassData.screenPos[0] = (float) (Math.round(textField.getDouble() * 100.0d) / 100.0d); break;
			case 1: compassData.screenPos[1] = (float) (Math.round(textField.getDouble() * 100.0d) / 100.0d); break;
		}
	}

	private void draw(String line, int x, int y, int color, int type) {
		if (type < 0) {
			type *= -1;
			List<String> lines = createLines(line);
			int i = 0;
			for (String l : lines) {
				draw(l, x, y + i * ClientProxy.LogFont.getHeight(), color, type);
				i++;
			}
		}
		else if (type > 0) {
			int bottom = y + ClientProxy.LogFont.getHeight() + 1;
			int right = x + type;
			int textWidth = ClientProxy.LogFont.width(line);
			int height = (y + bottom - ClientProxy.LogFont.getHeight()) / 2 + 1;
			int width = right - x - 1;
			if (textWidth > width) {
				int centerX = textWidth - width;
				double d0 = System.currentTimeMillis() / 1000.0d;
				double d1 = Math.max((double) centerX * 0.5, 3.0);
				double d2 = Math.sin(Math.PI / 2.0d * Math.cos(Math.PI * 2.0d * d0 / d1)) / 2.0 + 0.5;
				double d3 = d2 * centerX;

				GlStateManager.pushMatrix();
				GL11.glEnable(GL11.GL_SCISSOR_TEST);
				ScaledResolution sw = new ScaledResolution(Minecraft.getMinecraft());
				double d4 = sw.getScaledWidth() < mc.displayWidth
						? (int) Math.round((double) mc.displayWidth / (double) sw.getScaledWidth())
						: 1;
				GL11.glScissor((int) ((double) x * d4),
						(int) ((double) mc.displayHeight - (double) bottom * d4),
						Math.max(0, (int) ((double) (right - x) * d4)),
						Math.max(0, (int) ((double) (bottom - y) * d4)));

				ClientProxy.LogFont.draw(line, x - (int) d3, height, color);

				GL11.glDisable(GL11.GL_SCISSOR_TEST);
				GlStateManager.popMatrix();
			}
			else { ClientProxy.LogFont.draw(line, x, y, color); }
		}
		else { ClientProxy.LogFont.draw(line, x, y, color); }
	}

	public static List<String> createLines(String text) {
		List<String> lines = new ArrayList<>();
		if (text != null) {
			if (text.isEmpty()) { lines.add(""); }
			else {
				String[] words = text.split(" ");
				String line = "";
				String color = ((char) 167) + "r";
				float width = 98.0f * GuiLog.scaleW;
				for (String word : words) {
					Label_0236: {
						if (!word.isEmpty()) {
							if (word.length() == 1) {
								char c = word.charAt(0);
								if (c == '\r' || c == '\n') {
									lines.add(color + line);
									color = Util.instance.getLastColor(color, line);
									line = "";
									break Label_0236;
								}
							}
							String newLine;
							if (line.isEmpty()) { newLine = word; }
							else { newLine = line + " " + word; }
							if (ClientProxy.LogFont.width(newLine) > width) {
								lines.add(color + line);
								color = Util.instance.getLastColor(color, line);
								line = word.trim();
							}
							else { line = newLine; }
						}
					}
				}
				if (!line.isEmpty()) { lines.add(color + line); }
			}
		}
		return lines;
	}

	private void setNextTick(int time, boolean isNext) {
		tick = (int) (time / (CustomNpcs.IsFastAnimationGUI ? 3.0f : 1.0f));
		milliTick = tick - (isNext ? 1 : 0);
	}

}
