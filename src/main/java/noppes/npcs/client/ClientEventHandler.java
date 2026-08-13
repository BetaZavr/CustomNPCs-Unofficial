package noppes.npcs.client;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

import net.minecraft.block.BlockAir;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.api.util.IRayTraceRotate;
import noppes.npcs.api.util.IRayTraceVec;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.blocks.custom.CustomBlockLiquid;
import noppes.npcs.client.gui.player.GuiMailmanWrite;
import noppes.npcs.client.gui.player.GuiOpenCase;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.client.gui.yellow_de.data.UtilYDE;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.client.util.CrashesData;
import noppes.npcs.client.util.CustomNpcsLangPack;
import noppes.npcs.client.util.MusicData;
import noppes.npcs.constants.*;
import noppes.npcs.controllers.BorderController;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.fluids.CustomFluid;
import noppes.npcs.items.ItemBoundary;
import noppes.npcs.items.ItemNbtBook;
import noppes.npcs.items.ItemNpcMovingPath;
import noppes.npcs.mixin.client.audio.ISoundHandlerMixin;
import noppes.npcs.mixin.client.renderer.IBlockModelRendererMixin;
import noppes.npcs.mixin.client.renderer.IBlockRendererDispatcherMixin;
import noppes.npcs.mixin.pathfinding.IPathMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.google.gson.Gson;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.item.ISpecBuilder;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.client.gui.GuiNbtBook;
import noppes.npcs.client.gui.GuiNpcPather;
import noppes.npcs.client.gui.player.GuiLog;
import noppes.npcs.client.renderer.MarkRenderer;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.items.ItemBuilder;
import noppes.npcs.schematics.Schematic;
import noppes.npcs.schematics.SchematicWrapper;
import noppes.npcs.util.BuilderData;
import noppes.npcs.util.CustomNPCsScheduler;

public class ClientEventHandler extends Gui {

	protected static final ResourceLocation[] BORDER = new ResourceLocation[16];
	public static final ResourceLocation[] COMPASS_ICONS = new ResourceLocation[32];
	public static final Map<EntityPlayer, RenderChatMessages> chatMessages = new HashMap<>();

	static {
		for (int i = 0; i < 16; i++) {
			BORDER[i] = new ResourceLocation(CustomNpcs.MODID, "textures/util/border/" + (i < 10 ? "0" + i : i) + ".png");
		}
		for (int i =0; i < 32; i++) {
			String id = String.valueOf(i);
			if (id.length() < 2) { id = "0" + id; }
			COMPASS_ICONS[i] = new ResourceLocation("textures/items/compass_" + id + ".png");
		}
	}

	// Quest compass
	public static final ResourceLocation RESOURCE_COMPASS = new ResourceLocation(CustomNpcs.MODID + ":models/util/compass.obj");
	public static final Map<Integer, ParameterizedModel> COMPASS_FASE = new HashMap<>();
	public static ParameterizedModel COMPASS_BODY;
	public static ParameterizedModel COMPASS_DIAL;
	public static ParameterizedModel COMPASS_ARROW_0;
	public static ParameterizedModel COMPASS_ARROW_1;
	public static ParameterizedModel COMPASS_ARROW_20;
	public static ParameterizedModel COMPASS_ARROW_21;
	public static ParameterizedModel COMPASS_ARROW_22;
	public static ParameterizedModel COMPASS_ARROW_3;
	private int qt = 0;
	// Items:
	public static final List<Vec3d> movingPath = new ArrayList<>();
	// BuilderData
	public static int rotation;
	// Schematics:
	private static final List<TileBuilder> schemes = new ArrayList<>();
	// camera
	public static final CrashesData crashes = new CrashesData();
	// mails
	public static boolean hasNewMail = false;
	public static long showNewMail = 0L;
	public static long startMail = 0L;
	// main
	private static Minecraft mc;
	private static ScaledResolution sw;
	private double dx, dy, dz; // Camera
	// any
	public static long secs;
	private boolean miniMapLoaded;

	public static void addShowThis(TileBuilder tile) {
		if (Util.instance.getSide() == Side.SERVER || schemes.contains(tile)) { return; }
		schemes.add(tile);
	}

	public static void clearSchemes() { schemes.clear(); }

	public static void renderModelBlockQuads(List<BakedQuad> quads, float r, float g, float b) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		for (int j = quads.size(), i = 0; i < j; ++i) {
			BakedQuad bakedquad = quads.get(i);
			bufferbuilder.begin(7, DefaultVertexFormats.ITEM);
			bufferbuilder.addVertexData(bakedquad.getVertexData());
			if (bakedquad.hasTintIndex()) {
				bufferbuilder.putColorRGB_F4(r, g, b);
			} else {
				bufferbuilder.putColorRGB_F4(1.0f, 1.0f, 1.0f);
			}
			Vec3i vec3i = bakedquad.getFace().getDirectionVec();
			bufferbuilder.putNormal((float) vec3i.getX(), (float) vec3i.getY(), (float) vec3i.getZ());
			tessellator.draw();
		}
	}

	public static void renderSchem(SchematicWrapper schem, int rotation, double x, double y, double z) {
		mc = Minecraft.getMinecraft();
		if (mc.world == null || schem == null) { return; }
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		// Bound
		if (rotation % 2 == 0) { renderSelectionBox(new BlockPos(schem.schema.getWidth(), schem.schema.getHeight(), schem.schema.getLength())); }
		else { renderSelectionBox(new BlockPos(schem.schema.getLength(), schem.schema.getHeight(), schem.schema.getWidth())); }
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.depthMask(false);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F);
		try {
			for(int i = 0, j = 0; i < schem.size && j < 25000; ++i) {
				IBlockState state = schem.schema.getBlockState(i);
				if (state.getRenderType() != EnumBlockRenderType.INVISIBLE) {
					int posX = i % schem.schema.getWidth();
					int posZ = (i - posX) / schem.schema.getWidth() % schem.schema.getLength();
					int posY = ((i - posX) / schem.schema.getWidth() - posZ) / schem.schema.getLength();
					BlockPos pos = schem.rotatePos(posX, posY, posZ, rotation);
					GlStateManager.pushMatrix();
					GlStateManager.pushAttrib();
					GlStateManager.enableRescaleNormal();
					GlStateManager.translate((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
					state = SchematicWrapper.rotationState(state, rotation);
					try { renderBlock(state); }
					catch (Exception e) { LogWriter.error(e); }
					GlStateManager.popAttrib();
					GlStateManager.disableRescaleNormal();
					GlStateManager.popMatrix();
					j++;
				}
			}
		}
		catch (Exception e) { LogWriter.error("Error preview builder block", e); }
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	public static void renderSphere(BufferBuilder buffer,
									float x, float y, float z,
									float radius, int verticalRings, int horizontalRings,
									float r, float g, float b) {
		for (int i = 0; i <= verticalRings; i++) {
			float phi = (float) (i * Math.PI / verticalRings);
			float sinPhi = (float) Math.sin(phi);
			float cosPhi = (float) Math.cos(phi);

			for (int j = 0; j <= horizontalRings; j++) {
				float theta = (float) (j * 2 * Math.PI / horizontalRings);
				float sinTheta = (float) Math.sin(theta);
				float cosTheta = (float) Math.cos(theta);

				float x1 = x + radius * sinPhi * cosTheta;
				float y1 = y + radius * cosPhi;
				float z1 = z + radius * sinPhi * sinTheta;

				float nextI = i < verticalRings ? i + 1 : i;
				float nextPhi = (float) (nextI * Math.PI / verticalRings);
				float sinNextPhi = (float) Math.sin(nextPhi);
				float cosNextPhi = (float) Math.cos(nextPhi);

				float x2 = x + radius * sinNextPhi * cosTheta;
				float y2 = y + radius * cosNextPhi;
				float z2 = z + radius * sinNextPhi * sinTheta;

				buffer.pos(x1, y1, z1).color(r, g, b, 1.0f).endVertex();
				buffer.pos(x2, y2, z2).color(r, g, b, 1.0f).endVertex();
			}
		}
		for (int i = 0; i < verticalRings; i++) {
			float phi = (float) (i * Math.PI / verticalRings);
			float sinPhi = (float) Math.sin(phi);
			float cosPhi = (float) Math.cos(phi);

			for (int j = 0; j < horizontalRings; j++) {
				float theta = (float) (j * 2 * Math.PI / horizontalRings);
				float sinTheta = (float) Math.sin(theta);
				float cosTheta = (float) Math.cos(theta);

				float x1 = x + radius * sinPhi * cosTheta;
				float y1 = y + radius * cosPhi;
				float z1 = z + radius * sinPhi * sinTheta;

				float nextJ = j + 1;
				float nextTheta = (float) (nextJ * 2 * Math.PI / horizontalRings);
				float sinNextTheta = (float) Math.sin(nextTheta);
				float cosNextTheta = (float) Math.cos(nextTheta);

				float x2 = x + radius * sinPhi * cosNextTheta;
				float y2 = y + radius * cosPhi;
				float z2 = z + radius * sinPhi * sinNextTheta;
				buffer.pos(x1, y1, z1).color(r, g, b, 1.0f).endVertex();
				buffer.pos(x2, y2, z2).color(r, g, b, 1.0f).endVertex();
			}
		}
	}

	public static void renderBlock(IBlockState state) {
		mc = Minecraft.getMinecraft();
		WorldClient level = mc.world;
		if (level != null) {
			mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
			switch (state.getRenderType()) {
				case MODEL:
					IBakedModel ibakedmodel = dispatcher.getModelForState(state);
					GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
					BlockModelRenderer bmr = ((IBlockRendererDispatcherMixin) dispatcher).getBlockModelRenderer();
					BlockColors bc = ((IBlockModelRendererMixin) bmr).getBlockColors();
					int color = bc.colorMultiplier(state, null, null, 0);
					if (EntityRenderer.anaglyphEnable) {
						color = TextureUtil.anaglyphColor(color);
					}
					float r = (float) (color >> 16 & 255) / 255.0F;
					float g = (float) (color >> 8 & 255) / 255.0F;
					float b = (float) (color & 255) / 255.0F;
					for (EnumFacing enumfacing : EnumFacing.values()) {
						renderModelBlockQuads(ibakedmodel.getQuads(state, enumfacing, 0L), r, g, b);
					}
					renderModelBlockQuads(ibakedmodel.getQuads(state, null, 0L), r, g, b);
					break;
				case ENTITYBLOCK_ANIMATED:
					ChestRenderer chestRenderer = ((IBlockRendererDispatcherMixin) dispatcher).getChestRenderer();
					chestRenderer.renderChestBrightness(state.getBlock(), 1.0f);
				default:
					break;
			}
		}
	}

	private static void renderSelectionBox(BlockPos pos) {
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.disableBlend();
		AxisAlignedBB bb = new AxisAlignedBB(BlockPos.ORIGIN, pos);
		RenderGlobal.drawSelectionBoundingBox(bb, 1.0f, 0.0f, 0.0f, 1.0f);
		GlStateManager.enableTexture2D();
		GlStateManager.enableLighting();
		GlStateManager.enableCull();
		GlStateManager.disableBlend();
	}

	private static EntityNPCInterface getClosestNPC(double[] p, List<EntityNPCInterface> ents, QuestData qData) {
		double d = 65535.0d;
		Vec3i v = new Vec3i(p[0], p[1], p[2]);
		EntityNPCInterface npc = null;
		for (EntityNPCInterface el : ents) {
			if (el.getName().equals(qData.quest.getCompleterNpc().getName())) {
				double r = v.distanceSq(el.getPosition());
				if (npc != null && r >= d) { continue; }
				d = r;
				npc = el;
			}
		}
		return npc;
	}

	public static void entityClientEvent(EntityInteract event) {
		FriendlyByteBuf buffer = new FriendlyByteBuf();
		buffer.writeBlockPos(BlockPos.ORIGIN);
		CustomNpcs.proxy.openGui(null, EnumGuiType.NbtBook, buffer);
		CustomNPCsScheduler.runTack(() -> {
			Minecraft mc = Minecraft.getMinecraft();
			if (mc.currentScreen instanceof GuiNbtBook) {
				GuiNbtBook nbtBook = (GuiNbtBook) mc.currentScreen;
				nbtBook.entityId = event.getTarget().getEntityId();
				nbtBook.entity = event.getTarget();
				nbtBook.originalCompound = new NBTTagCompound();
				event.getTarget().writeToNBT(nbtBook.originalCompound);
				nbtBook.compound = nbtBook.originalCompound;
				nbtBook.initGui();
			}
		}, 250);
	}


	public static void renderBalance(GuiScreen parent, int mouseX, int mouseY, int x, int y) {
		if ((CustomNpcs.ShowMoney || CustomNpcs.ShowDonat) && parent != null && x !=0 && y != 0) {
			PlayerData data = CustomNpcs.proxy.getPlayerData(null);
			long money = data.game.getMoney();
			long donat = data.game.getDonat();
			int yM = y - (CustomNpcs.ShowMoney && CustomNpcs.ShowDonat ? 6 : 0);
			int yD = !CustomNpcs.ShowMoney ? y : yM + 12;
			// coins
			GlStateManager.pushMatrix();
			RenderHelper.enableGUIStandardItemLighting();
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.translate(x, yM, 0.0f);
			float s = 16.0f / 256.0f;
			GlStateManager.scale(s, s, s);
			if (CustomNpcs.ShowMoney) {
				parent.mc.getTextureManager().bindTexture(GuiBasic.MONEY);
				parent.drawTexturedModalRect(0, 0, 0, 0, 256, 256);
				if (CustomNpcs.ShowDonat) { GlStateManager.translate(0.0f, 192.0f, 0.0f); }
			}
			if (CustomNpcs.ShowDonat) {
				parent.mc.getTextureManager().bindTexture(GuiBasic.DONAT);
				parent.drawTexturedModalRect(0, 0, 0, 0, 256, 256);
			}
			GlStateManager.popMatrix();
			// text
			GlStateManager.pushMatrix();
			GlStateManager.translate(x + 16.0f, yM + (float) parent.mc.fontRenderer.FONT_HEIGHT / 2.0f, 1.0f);
			String text;
			if (CustomNpcs.ShowMoney) {
				text = Util.instance.getTextReducedNumber(money, true, true, false) + CustomNpcs.displayCurrencies;
				parent.mc.fontRenderer.drawString(text, 0, 0, CustomNpcs.LableColor.getRGB(), false);
				if (CustomNpcs.ShowDonat) { GlStateManager.translate(0.0f, 12.0f, 0.0f); }
			}
			if (CustomNpcs.ShowDonat) {
				text = Util.instance.getTextReducedNumber(donat, true, true, false) + CustomNpcs.displayDonation;
				parent.mc.fontRenderer.drawString(text, 0, 0, CustomNpcs.LableColor.getRGB(), false);
			}
			GlStateManager.popMatrix();
			// hover
			if (mouseX > x && mouseY > yM + 2 && mouseX < x + 50 && mouseY < yM + 34) {
				List<String> hoverText = new ArrayList<>();
				if (CustomNpcs.ShowMoney && mouseY < yM + 14) {
					hoverText.add(Component.translatable("inventory.hover.currency").getFormattedText());
					hoverText.add("" + money);
				} // money
				else if (CustomNpcs.ShowDonat && mouseY >= yD  && mouseY < yD + 14) {
					hoverText.add(Component.translatable("inventory.hover.donat").getFormattedText());
					hoverText.add("" + donat);
				} // donat
				if (!hoverText.isEmpty()) {
					GlStateManager.pushMatrix();
					GlStateManager.disableDepth();
					parent.drawHoveringText(hoverText, mouseX, mouseY);
					GlStateManager.popMatrix();
				}
			}
		}
	}

	@SubscribeEvent
	public <T extends EntityLivingBase> void cnpcPostLivingEvent(RenderLivingEvent.Post<T> event) {
		CustomNpcs.debugData.start(null);
		if (event.getEntity().isEntityAlive()) {
			MarkData data = MarkData.get(event.getEntity());
			for (MarkData.Mark m : data.marks) {
				if (m.getType() != 0 && m.availability.isAvailable(Minecraft.getMinecraft().player)) {
					MarkRenderer.render(event.getEntity(), event.getX(), event.getY(), event.getZ(), m);
					break;
				}
			}
		}
		if (event.getEntity() instanceof EntityPlayer && chatMessages.containsKey((EntityPlayer) event.getEntity())) {
			EntityPlayer player = (EntityPlayer) event.getEntity();
			float height = event.getEntity().height + 0.9f;
			chatMessages.get(player).renderMessages(event.getX(), event.getY() + height, event.getZ(),
					0.666667f * height, isInRange(Minecraft.getMinecraft().player, event.getX(), event.getY() + 1.2d, event.getZ()), true);
		}
		CustomNpcs.debugData.end(null);
	}

	@SubscribeEvent
	public void cnpcPlaySoundSource(PlaySoundSourceEvent event) {
		CustomNpcs.debugData.start("Players");
		processSoundPlay(event, event.getSound(), event.getUuid());
		CustomNpcs.debugData.end("Players");
	}

	@SubscribeEvent
	public void cnpcPlayStreamingSource(PlayStreamingSourceEvent event) {
		CustomNpcs.debugData.start("Players");
		processSoundPlay(event, event.getSound(), event.getUuid());
		CustomNpcs.debugData.end("Players");
	}

	@SubscribeEvent
	public void cnpcOpenGUIEvent(GuiOpenEvent event) {
		CustomNpcs.debugData.start(null);
		mc = Minecraft.getMinecraft();
		String oldScreenName = mc.currentScreen == null ? "GuiIngame" : mc.currentScreen.getClass().getSimpleName();
		String newGUI = event.getGui() == null ? "GuiIngame" : event.getGui().getClass().getSimpleName();
		if (event.getGui() instanceof GuiLog) {
			switch(((GuiLog) event.getGui()).type) {
				case 0: newGUI = "GuiFactionLog"; break;
				case 1: newGUI = "GuiQuestLog"; break;
				case 2: newGUI = "GuiCompassLog"; break;
				default: newGUI = "GuiInventoryLog"; break;
			}
		}
		LogWriter.debug(((event.getGui() == null ? "Close GUI " : "Open GUI - " + event.getGui().getClass().getName()) + "; OLD - " + oldScreenName));
		Packets.sendServer(new SPacketPlayerScreen(newGUI, oldScreenName));
		if (mc.currentScreen instanceof GuiNpcPather) { movingPath.clear(); }
		else if (event.getGui() instanceof GuiInventory) {
			if (mc.player.isCreative() && mc.player.getHeldItemMainhand().getItem() instanceof ISpecBuilder) {
				event.setCanceled(true);
				ISpecBuilder item = (ISpecBuilder) mc.player.getHeldItemMainhand().getItem();
				BuilderData builder = ItemBuilder.getBuilder(mc.player.getHeldItemMainhand(), mc.player);
				int id = builder == null ? -1 : builder.getID();
				int type = builder == null ? item.getType() : builder.getType();
				if (id > -1) { Packets.sendServer(new SPacketGetBuildData(id, type)); }
				CustomNPCsScheduler.runTack(() -> NoppesUtil.requestOpenGUI(item.getGUIType(), new BlockPos(id, type, 0)), 100);
				CustomNpcs.debugData.end(null);
				return;
			}
		}
		CustomNpcs.debugData.end(null);
	}

	/** HUD Bar Interface Canceled */
	@SubscribeEvent
	public void cnpcRenderGameOverlayPre(RenderGameOverlayEvent.Pre event) {
		if (event.getType() == RenderGameOverlayEvent.ElementType.CHAT) {
			renderMailOverlay(event.getResolution());
			renderCompassOverlay(event.getResolution());
			renderNbtBookOverlay(event.getResolution());
		}
		if (mc != null) {
			event.setCanceled(mc.currentScreen instanceof GuiOpenCase ||
					!CustomNpcs.proxy.getPlayerData(mc.player).overlay.isShowElementType(event.getType()));
		}
	}

	/** Any Regions */
	@SubscribeEvent
	public void cnpcRenderWorldLast(RenderWorldLastEvent event) {
		mc = Minecraft.getMinecraft();
		sw = new ScaledResolution(mc);
		if (mc.player == null || mc.world == null) { return; }
		CustomNpcs.debugData.start(mc.player);
		PlayerData playerData = CustomNpcs.proxy.getPlayerData(mc.player);
		boolean isMoved = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()) ||
				Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()) ||
				Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()) ||
				Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
		if (CustomNpcs.proxy.getPlayerData(mc.player).overlay.isMoved != isMoved) {
			playerData.overlay.isMoved = isMoved;
			Packets.sendServer(new SPacketPlayerIsMoved(isMoved));
		}
		// position
		dx = mc.player.lastTickPosX + (mc.player.posX - mc.player.lastTickPosX) * (double) event.getPartialTicks();
		dy = mc.player.lastTickPosY + (mc.player.posY - mc.player.lastTickPosY) * (double) event.getPartialTicks();
		dz = mc.player.lastTickPosZ + (mc.player.posZ - mc.player.lastTickPosZ) * (double) event.getPartialTicks();
		// in game
		if (!ClientTickHandler.inGame) {
			CustomNpcs.debugData.started = System.currentTimeMillis();
			CustomNpcs.debugData.startedTicks = ClientTickHandler.ticks;
			ClientTickHandler.inGame = true;
			PlayerData data = CustomNpcs.proxy.getPlayerData(mc.player);
			data.player = mc.player;
			data.name = mc.player.getName();
			data.uuid = mc.player.getUniqueID().toString();
			miniMapLoaded = false;
			updateMiniMaps(true);
			EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.LOGIN, new PlayerEvent.LoginEvent(data.scriptData.getIPlayer()));
			CustomNpcsLangPack.check();
			LogWriter.debug("Client Player: Start game");
		}
		// Schematics builder blocks
		if (!schemes.isEmpty()) {
			for (TileBuilder tile : new ArrayList<>(schemes)) {
				if (tile == null || !tile.hasWorld() ||
						!tile.hasSchematic() || !tile.getShow() ||
						!(Objects.requireNonNull(tile.getWorld()).getTileEntity(tile.getPos()) instanceof TileBuilder)) {
					schemes.remove(tile);
					continue;
				}
				if (tile.getPos() != BlockPos.ORIGIN &&
						tile.getPos().distanceSq(mc.player.getPosition()) <= 1000000.0D &&
						tile.getShow() &&
						mc.player.world == tile.getWorld() &&
						tile.getSchematic() != null) {
					renderSchem(tile.getSchematic(), tile.rotation,
							tile.getPos().getX() - dx + 1.0f,
							tile.getPos().getY() - dy + tile.yOffset,
							tile.getPos().getZ() - dz + 1.0f);
				}
			}
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		}
		// Show builder data
		BuilderData builder = ItemBuilder.getBuilder(mc.player.getHeldItemMainhand(), mc.player);
		if (builder != null && builder.getID() > -1 && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
			renderZone(builder, mc.objectMouseOver.getBlockPos());
		}
		// Show block tool hitboxes
		NBTTagCompound nbtMP = null;
		ItemStack mainStack = mc.player.getHeldItemMainhand();
		ItemStack offStack = mc.player.getHeldItemOffhand();
		if (CustomNpcs.ShowHitboxWhenHoldTools && mainStack.getItem() instanceof INPCToolItem ||
				offStack.getItem() instanceof INPCToolItem) {
			AxisAlignedBB aabb = new AxisAlignedBB(-5.0, -5.0, -5.0, 5.0, 5.0, 5.0).offset(mc.player.getPosition());
			List<Entity> entities = new ArrayList<>();
			try { entities = mc.player.world.getEntitiesWithinAABB(Entity.class, aabb); } catch (Exception ignored) { }
			entities.remove(mc.player);
			Entity rayTrE;
			if (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null) {
				rayTrE = Util.instance.getLookEntity(mc.player, (mainStack.getItem() instanceof ItemNbtBook ? CustomNpcs.proxy.getPlayerData(mc.player).game.renderDistance : null), false);
			} else { rayTrE = mc.objectMouseOver.entityHit; }
			if (rayTrE != null && !entities.contains(rayTrE)) { entities.add(rayTrE); }
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			GlStateManager.glLineWidth(2.0F);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			GlStateManager.translate(-dx, -dy, -dz);
			for (Entity entity : entities) {
				float w = entity.width / 2;
				if (entity.getDistance(mc.player) - w <= 5.0) {
					AxisAlignedBB col= entity.getCollisionBoundingBox();
					if (col == null || (entity instanceof EntityNPCInterface && ((EntityNPCInterface) entity).display.getHitboxState() == 2)) {
						col = new AxisAlignedBB(-w, 0.0, -w, w, entity.height, w);
					}
					GlStateManager.pushMatrix();
					GlStateManager.translate(entity.posX, entity.posY,  entity.posZ);
					RenderGlobal.drawSelectionBoundingBox(col,  0.8f, 0.8f, 0.8f, 0.8f);
					if (entity.equals(rayTrE)) { // hover entity
						GlStateManager.glLineWidth(3.0F);
						RenderGlobal.drawSelectionBoundingBox(col.grow(entity.width / 20.0),  0.8f, 0.3f, 0.6f, 1.0f);
					}
					if (entity instanceof EntityNPCInterface) {
						AnimationConfig anim = ((EntityNPCInterface) entity).animation.getAnimation();
						if (anim != null && anim.type == AnimationKind.ATTACKING) {
							List<AxisAlignedBB> hitBoxes = anim.getDamageHitboxes((EntityLivingBase) entity, ((EntityNPCInterface) entity).animation.getAnimationCurrentFrameID());
							if (hitBoxes != null && !hitBoxes.isEmpty()) {
								GlStateManager.glLineWidth(2.0F);
								GlStateManager.translate(-entity.posX, -entity.posY,  -entity.posZ);
								for (AxisAlignedBB hitBox : hitBoxes) {
									RenderGlobal.drawSelectionBoundingBox(hitBox,  1.0f, 0.0f, 0.0f, 0.5f);
								}
							}
						}
					}
					GlStateManager.popMatrix();
				}
			}
			GlStateManager.depthMask(true);
			GlStateManager.enableTexture2D();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}
		// Show NPC moving path
		if (mainStack.getItem() instanceof ItemNpcMovingPath) { nbtMP = mainStack.getTagCompound(); }
		else if (offStack.getItem() instanceof ItemNpcMovingPath) { nbtMP = offStack.getTagCompound(); }
		if (nbtMP != null && nbtMP.hasKey("NPCID", 3)) {
			Entity entity = mc.player.world.getEntityByID(nbtMP.getInteger("NPCID"));
			if (entity instanceof EntityCustomNpc) { renderNpcMovingPath((EntityCustomNpc) entity); }
			else { movingPath.clear(); }
		}
		int id = -1;
		// Show rayTrace point
		BorderController bData = BorderController.getInstance();
		if (mainStack.getItem() instanceof ItemBoundary) {
			if (mainStack.getTagCompound() != null && mainStack.getTagCompound().hasKey("RegionID", 3)) { id = mainStack.getTagCompound().getInteger("RegionID"); }
			Zone3D reg = bData.getRegion(id);
			// choosing a central position to create a new region
			if ((CustomNpcs.proxy.getPlayerData(mc.player).overlay.isPressedShift() || reg == null) &&
					mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit!= RayTraceResult.Type.MISS) {
				final BlockPos pos = getPos(mc.objectMouseOver);
				GlStateManager.pushMatrix();
				GlStateManager.enableBlend();
				GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
				GlStateManager.glLineWidth(3.0F);
				GlStateManager.disableTexture2D();
				GlStateManager.depthMask(false);
				GlStateManager.translate(pos.getX() - dx + 0.5d, pos.getY() - dy,  pos.getZ() - dz + 0.5d);
				GlStateManager.rotate((float) ((System.currentTimeMillis() / 7) % 360), 0.0f, 1.0f, 0.0f);
				RenderGlobal.drawSelectionBoundingBox((new AxisAlignedBB(-0.35d, 0.15d, -0.35d, 0.35d, 0.85d, 0.35d)),  1.0f, 0.50f, 1.0f, 1.0f);
				GlStateManager.depthMask(true);
				GlStateManager.enableTexture2D();
				GlStateManager.disableBlend();
				GlStateManager.popMatrix();
			}
		}
		// Show Regions
		GlStateManager.enableBlend();
		double dist = 250.0d;
		for (Zone3D reg : bData.getRegionsInWorld(mc.player.world.provider.getDimension())) {
			if (mc.player.isCreative()) { dist = Math.max(250.0d, Math.max(Math.max(reg.getMaxX(), reg.getMaxY()), reg.getMaxZ()) * 2.0d); }
			if (reg == null || reg.dimension != mc.player.world.provider.getDimension() || reg.distanceTo(mc.player) > dist) { continue; }
			if (mc.player.isCreative()) { renderRegion(event.getPartialTicks(), reg, id); }
			else if (reg.showInClient) { renderRegion(reg, -1); }
		}
		GlStateManager.disableBlend();
		// minimaps
		if (mc.player.world.getTotalWorldTime() % 100 == 0 && secs != System.currentTimeMillis() / 1000) {
			secs = System.currentTimeMillis() / 1000;
			updateMiniMaps(false);
		}
		CustomNpcs.debugData.end(mc.player);
	}

	@SubscribeEvent
	public void cnpcRenderHand(RenderSpecificHandEvent event) {
		if (event != null) {
			mc = Minecraft.getMinecraft();
			event.setCanceled(mc.currentScreen instanceof GuiOpenCase);
		}
	}

	/** Camera Shake */
	@SubscribeEvent
	public void cnpcCameraSetupEvent(EntityViewRenderEvent.CameraSetup event) {
		if (event.getEntity() instanceof EntityLivingBase && crashes.isActive) {
			CustomNpcs.debugData.start(null);
			float amplitude = crashes.get(crashes.endTime - event.getEntity().world.getTotalWorldTime());
			if (amplitude != 0.0f) {
				switch (crashes.type) {
					case 0: {
						event.setPitch(Minecraft.getMinecraft().getRenderPartialTicks() * amplitude + event.getPitch());
						break;
					} // vertical only
					case 1: {
						event.setYaw(Minecraft.getMinecraft().getRenderPartialTicks() * amplitude + event.getYaw());
						break;
					} // horizontal only
					case 2: {
						event.setRoll(Minecraft.getMinecraft().getRenderPartialTicks() * amplitude);
						break;
					} // arc only
					case 3: {
						event.setPitch(Minecraft.getMinecraft().getRenderPartialTicks() * amplitude + event.getPitch());
						event.setYaw(Minecraft.getMinecraft().getRenderPartialTicks() * amplitude + event.getYaw());
						break;
					} // vertical and horizontal
					case 4: {
						event.setRoll(Minecraft.getMinecraft().getRenderPartialTicks() * amplitude);
						event.setPitch(Minecraft.getMinecraft().getRenderPartialTicks() * amplitude + event.getPitch());
						break;
					} // vertical and arc
					default: {
						event.setRoll(Minecraft.getMinecraft().getRenderPartialTicks() * amplitude * -1.0f);
						event.setPitch(Minecraft.getMinecraft().getRenderPartialTicks() * amplitude + event.getPitch());
						event.setYaw(Minecraft.getMinecraft().getRenderPartialTicks() * amplitude + event.getYaw());
						break;
					} // all
				}
			}
			CustomNpcs.debugData.end(null);
		}
	}

	@SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
	public static void onFogDensity(EntityViewRenderEvent.FogDensity event) {
		Entity entity = event.getEntity();
		World world = entity.world;
		BlockPos pos = new BlockPos(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
		IBlockState state = world.getBlockState(pos);

		if (state.getBlock() instanceof CustomBlockLiquid) {
			Fluid fluid = ((CustomBlockLiquid) state.getBlock()).getFluid();
			if (fluid instanceof CustomFluid) {
				event.setDensity(fluid.getDensity() / 1000.0f);
				event.setCanceled(true);
			}
		}
	}

	private boolean isInRange(EntityPlayer player, double posX, double posY, double posZ) {
		double y = Math.abs(player.posY - posY);
		if (posY >= 0.0 && y > 16.0) { return false; }
		double x = Math.abs(player.posX - posX);
		double z = Math.abs(player.posZ - posZ);
		return x <= 16.0 && z <= 16.0;
	}

	private void processSoundPlay(Event event, ISound sound, String uuid) {
		if (sound == null) { return; }
		mc = Minecraft.getMinecraft();
		try {
			if (mc.world != null && mc.getConnection() != null) {
				MusicData md = new MusicData(sound, uuid, ((ISoundHandlerMixin) mc.getSoundHandler()).getSndManager());
				ClientTickHandler.musics.add(md);
				md.createClientEvent(event, mc.player, 0);
				Packets.sendServer(new SPacketPlayerSound(true, md));
			}
		}
		catch (Exception ignored) { }
    }

	private BlockPos getPos(RayTraceResult result) {
		int x = result.getBlockPos().getX();
		int y = result.getBlockPos().getY();
		int z = result.getBlockPos().getZ();
		try {
			switch (result.entityHit.getHorizontalFacing()) {
				case UP: y += 1; break;
				case NORTH: z -= 1; break;
				case SOUTH: z += 1; break;
				case WEST: x -= 1; break;
				case EAST: x += 1; break;
				default: y -= 1; break;
			}
		}
		catch (Exception e) { LogWriter.error(e); }
		return new BlockPos(x, y, z);
	}

	private void renderEntityForBook(Entity entityIn) {
		if (!(entityIn instanceof EntityLivingBase)) { return; }
		EntityLivingBase entity = (EntityLivingBase) entityIn;
		EntityNPCInterface npc = null;
		int visible = 0;
		int showName = 0;
		int orientation = 0;
		if (entity instanceof EntityNPCInterface) {
			npc = (EntityNPCInterface) entity;
			visible = npc.display.getVisible();
			showName = npc.display.getShowName();
			orientation = npc.ais.orientation;
			npc.display.setVisible(2);
			npc.display.setShowName(0);
			npc.ais.orientation = 0;
		}
		GlStateManager.pushMatrix();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		GlStateManager.enableColorMaterial();
		float scW = entity.width > 1.0f ? 1.0f / entity.width : 1.0f;
		float scH = entity.height > 2.4f ? 2.4f / entity.height : 1.0f;
		float scale = Math.min(scW, scH);
		GlStateManager.scale(-30.0f * scale * 0.75f, -30.0f * scale * 0.75f, -30.0f * scale * 0.75f);
		RenderHelper.enableStandardItemLighting();
		float f2 = entity.renderYawOffset;
		float f3 = entity.rotationYaw;
		float f4 = entity.rotationPitch;
		float f5 = entity.rotationYawHead;
		float f6 = 0.0f;
		GlStateManager.rotate(-20.0f, 1.0f, 0.0f, 0.0f);
		GlStateManager.rotate(210.0f, 0.0f, 1.0f, 0.0f);
		entity.renderYawOffset = 0;
		entity.rotationYaw = (float) (Math.atan(f6 / 80.0f) * 40.0f + 0);
		entity.rotationPitch = 0.0f;
		entity.rotationYawHead = entity.rotationYaw;
		mc.getRenderManager().playerViewY = 180.0f;
		mc.getRenderManager().renderEntity(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, false);
		entity.renderYawOffset = f2;
		entity.prevRenderYawOffset = f2;
		entity.rotationYaw = f3;
		entity.prevRotationYaw = f3;
		entity.rotationPitch = f4;
		entity.prevRotationPitch = f4;
		entity.rotationYawHead = f5;
		entity.prevRotationYawHead = f5;
		if (npc != null) {
			npc.display.setVisible(visible);
			npc.display.setShowName(showName);
			npc.ais.orientation = orientation;
		}
		GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GlStateManager.disableTexture2D();
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GlStateManager.popMatrix();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableRescaleNormal();
	}

	private void renderNpcMovingPath(EntityCustomNpc npc) {
		List<int[]> list = npc.ais.getMovingPath();
		if (list.size() < 2) { Packets.sendServerDelayed(new SPacketGetMovingPath(npc.getEntityId()), npc, 5000); }
		if (list.isEmpty()) {
			movingPath.clear();
			return;
		}
		boolean type = npc.ais.getMovingPathType() == 0;
		// create path
		if (npc.ais.getMovingType() == 2 && (movingPath.isEmpty() || mc.world.getTotalWorldTime() % 100L == 0L)) {
			NBTTagCompound npcNbt = new NBTTagCompound();
			npc.writeToNBTAtomically(npcNbt);
			Entity entity = EntityList.createEntityFromNBT(npcNbt, mc.world);
			if (entity != null) {
				entity.setUniqueId(UUID.randomUUID());
				List<Vec3d> newMovingPath = new ArrayList<>();
				if (entity instanceof EntityCustomNpc) {
					EntityCustomNpc newNpc = (EntityCustomNpc) entity;
					int[] pos = list.get(0);
					double yo = 0.0d;
					IBlockState state = mc.world.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
					if (state.isFullBlock() || state.isFullCube()) { yo = 1.0d; }
					newNpc.setPosition(pos[0], pos[1] + yo, pos[2]);
					newMovingPath.add(new Vec3d(pos[0] + 0.5d, pos[1] + yo + 0.4d, pos[2] + 0.5d));
					newNpc.display.setVisible(1);
					newNpc.display.setSize(1);
					newNpc.display.setShowName(1);
					mc.world.spawnEntity(newNpc);
					PathNavigate nv = newNpc.getNavigator();
					for (int i = 1; i < list.size(); i++) {
						pos = list.get(i);
						nv.clearPath();
						newNpc.motionX = 0.0d;
						newNpc.motionY = 0.0d;
						newNpc.motionZ = 0.0d;
						Path path = nv.getPathToXYZ(pos[0], pos[1], pos[2]);
						if (path == null) {
							newMovingPath.add(null);
							yo = 0.0d;
							state = mc.world.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
							if (state.isFullBlock() || state.isFullCube()) { yo = 1.0d; }
							newNpc.setPosition(pos[0], pos[1] + yo, pos[2]);
							continue;
						}
						for (int p = 0; p < path.getCurrentPathLength(); p++) {
							PathPoint pp = path.getPathPointFromIndex(p);
							newMovingPath.add(new Vec3d(pp.x + 0.5d, pp.y + 0.4d, pp.z + 0.5d));
						}
						yo = 0.0d;
						state = mc.world.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
						if (state.isFullBlock() || state.isFullCube()) { yo = 1.0d; }
						newNpc.setPosition(pos[0], pos[1] + yo, pos[2]);
					}
					if (type) {
						nv.clearPath();
						pos = list.get(list.size() - 1);
						yo = 0.0d;
						state = mc.world.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
						if (state.isFullBlock() || state.isFullCube()) { yo = 1.0d; }
						newNpc.setPosition(pos[0], pos[1] + yo, pos[2]);
						newNpc.motionX = 0.0d;
						newNpc.motionY = 0.0d;
						newNpc.motionZ = 0.0d;
						pos = list.get(0);
						Path path = nv.getPathToXYZ(pos[0], pos[1], pos[2]);
						if (path != null) {
							for (int p = 0; p < path.getCurrentPathLength(); p++) {
								PathPoint pp = path.getPathPointFromIndex(p);
								newMovingPath.add(new Vec3d(pp.x + 0.5d, pp.y + 0.4d, pp.z + 0.5d));
							}
						}
					}
					else {
						pos = list.get(list.size() - 1);
						yo = 0.0d;
						state = mc.world.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
						if (state.isFullBlock() || state.isFullCube()) { yo = 1.0d; }
						newNpc.setPosition(pos[0], pos[1] + yo, pos[2]);
						for (int i = list.size() - 1; i >= 0; i--) {
							pos = list.get(i);
							nv.clearPath();
							newNpc.motionX = 0.0d;
							newNpc.motionY = 0.0d;
							newNpc.motionZ = 0.0d;
							Path path = nv.getPathToXYZ(pos[0], pos[1], pos[2]);
							if (path == null) {
								newMovingPath.add(null);
								yo = 0.0d;
								state = mc.world.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
								if (state.isFullBlock() || state.isFullCube()) { yo = 1.0d; }
								newNpc.setPosition(pos[0], pos[1] + yo, pos[2]);
								continue;
							}
							for (int p = 0; p < path.getCurrentPathLength(); p++) {
								PathPoint pp = path.getPathPointFromIndex(p);
								newMovingPath.add(new Vec3d(pp.x + 0.5d, pp.y + 0.6d, pp.z + 0.5d));
							}
							yo = 0.0d;
							state = mc.world.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
							if (state.isFullBlock() || state.isFullCube()) { yo = 1.0d; }
							newNpc.setPosition(pos[0], pos[1] + yo, pos[2]);
						}
						nv.clearPath();
						pos = list.get(0);
						yo = 0.0d;
						state = mc.world.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
						if (state.isFullBlock() || state.isFullCube()) { yo = 1.0d; }
						newNpc.setPosition(pos[0], pos[1] + yo, pos[2]);
						newNpc.motionX = 0.0d;
						newNpc.motionY = 0.0d;
						newNpc.motionZ = 0.0d;
						pos = list.get(0);
						Path path = nv.getPathToXYZ(pos[0], pos[1], pos[2]);
						if (path != null) {
							for (int p = 0; p < path.getCurrentPathLength(); p++) {
								PathPoint pp = path.getPathPointFromIndex(p);
								newMovingPath.add(new Vec3d(pp.x + 0.5d, pp.y + 0.6d, pp.z + 0.5d));
							}
						}
					}
				}
				if (newMovingPath.size() > list.size() * 2) {
					movingPath.clear();
					movingPath.addAll(newMovingPath);
				}
				entity.isDead = true;
				mc.world.removeEntity(entity);
				mc.world.removeEntityDangerously(entity);
				Chunk chunk = entity.world.getChunkFromChunkCoords(entity.chunkCoordX, entity.chunkCoordZ);
				if (entity.addedToChunk && chunk.isLoaded()) {
					chunk.removeEntity(entity);
					entity.addedToChunk = false;
				}
			}
		}
		Vec3d pre;
		float r = 0.75f, g = 0.75f, b = 0.75f, ag = 15.0f;
		// HitBox
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.glLineWidth(1);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		GlStateManager.translate(npc.posX - dx, npc.posY - dy, npc.posZ - dz);
		double w = npc.width / 2;
		RenderGlobal.drawSelectionBoundingBox((new AxisAlignedBB(w * -1.0d, 0.0d, w * -1.0d, w, npc.height, w)), r, g, b, 1.0f);
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
		// Eyes + Head rotation
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.glLineWidth(2.0f);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		GlStateManager.translate(-dx, -dy, -dz);
		IRayTraceVec pHh = Util.instance.getPosition(npc.posX, npc.posY + npc.getEyeHeight(), npc.posZ, npc.rotationYawHead, 0.0d, npc.width / 2.0d);
		IRayTraceVec pEr = Util.instance.getPosition(pHh.getX(), pHh.getY(), pHh.getZ(), npc.rotationYawHead, npc.rotationPitch * -1.0d, 0.7d / 5.0d * npc.display.getSize());
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		r = 0.25f;
		g = 0.5f;
		buffer.pos(pHh.getX(), pHh.getY(), pHh.getZ()).color(r, g, b, 1.0f).endVertex();
		buffer.pos(pEr.getX(), pEr.getY(), pEr.getZ()).color(r, g, b, 1.0f).endVertex();
		// is direct
		if (npc.ais.directLOS != EnumSeeTarget.NORMAL && npc.ais.directLOS != EnumSeeTarget.BLIND && npc.ais.directLOS != EnumSeeTarget.NONE) {
			IRayTraceVec mr = Util.instance.getPosition(pHh.getX(), pHh.getY(), pHh.getZ(), npc.rotationYawHead + 60.0d, 0.0d, 0.7d / 5.0d * npc.display.getSize());
			IRayTraceVec nr = Util.instance.getPosition(pHh.getX(), pHh.getY(), pHh.getZ(), npc.rotationYawHead - 60.0d, 0.0d, 0.7d / 5.0d * npc.display.getSize());
			IRayTraceVec mp = Util.instance.getPosition(pHh.getX(), pHh.getY(), pHh.getZ(), npc.rotationYaw, ValueUtil.correctDouble(npc.rotationPitch * -1.0d + 60.0d, -90.0d, 90.0d), 1.4d / 5.0d * npc.display.getSize());
			IRayTraceVec np = Util.instance.getPosition(pHh.getX(), pHh.getY(), pHh.getZ(), npc.rotationYaw, ValueUtil.correctDouble(npc.rotationPitch * -1.0d - 60.0d, -90.0d, 90.0d), 1.4d / 5.0d * npc.display.getSize());
			r = 0.525f;
			g = 0.725f;
			b = 0.125f;
			buffer.pos(pHh.getX(), pHh.getY(), pHh.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(mr.getX(), mp.getY(), mr.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(pHh.getX(), pHh.getY(), pHh.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(mr.getX(), np.getY(), mr.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(mr.getX(), mp.getY(), mr.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(mr.getX(), np.getY(), mr.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(pHh.getX(), pHh.getY(), pHh.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(nr.getX(), mp.getY(), nr.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(pHh.getX(), pHh.getY(), pHh.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(nr.getX(), np.getY(), nr.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(nr.getX(), mp.getY(), nr.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(nr.getX(), np.getY(), nr.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(mr.getX(), mp.getY(), mr.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(nr.getX(), mp.getY(), nr.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(mr.getX(), np.getY(), mr.getZ()).color(r, g, b, 1.0f).endVertex();
			buffer.pos(nr.getX(), np.getY(), nr.getZ()).color(r, g, b, 1.0f).endVertex();
		}
		// aggroRange
		if (npc.ais.directLOS == EnumSeeTarget.BLIND) {
			r = 0.785f;
			g = 0.785f;
			b = 0.195f;
		} // sound only
		else {
			r = 0.785f;
			g = 0.195f;
			b = 0.195f;
		}
		renderSphere(buffer,
				(float) (npc.posX + dx), (float) (npc.posY + dy), (float) (npc.posZ + dz),
				npc.stats.aggroRange, 12, 16, r, g, b);
		if (npc.ais.directLOS == EnumSeeTarget.WARY || npc.ais.directLOS == EnumSeeTarget.REALISTIC) {
			renderSphere(buffer,
					(float) (npc.posX + dx), (float) (npc.posY + dy), (float) (npc.posZ + dz),
					ValueUtil.correctFloat(npc.stats.aggroRange / 4.0f, 3.0f, npc.stats.aggroRange),
					12, 16, r, g, b);
		} // guaranteed attack
		// tactic sphere
		if (npc.ais.tacticalVariant != EnumNpcTactics.RUSH && npc.ais.tacticalVariant != EnumNpcTactics.NONE && npc.ais.getTacticalRange() > 0) {
			renderSphere(buffer,
					(float) (npc.posX + dx), (float) (npc.posY + dy), (float) (npc.posZ + dz),
					npc.ais.getTacticalRange() * 0.99f, 14, 18, 0.195f, 0.195f, 0.785f);
		}
		tessellator.draw();
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
		// Target
		if (npc.getAttackTarget() != null) {
			EntityLivingBase target = npc.getAttackTarget();
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
					GlStateManager.DestFactor.ZERO);
			GlStateManager.glLineWidth(2.0f);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			GlStateManager.translate(-dx, -dy, -dz);
			tessellator = Tessellator.getInstance();
			buffer = tessellator.getBuffer();
			buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

			r = 0.8f;
			g = 0.0f;
			b = 0.8f;
			buffer.pos(target.posX, target.posY + target.getEyeHeight(), target.posZ).color(r, g, b, 1.0f).endVertex();
			buffer.pos(npc.posX, npc.posY + npc.getEyeHeight(), npc.posZ).color(r, g, b, 1.0f).endVertex();

			tessellator.draw();
			GlStateManager.depthMask(true);
			GlStateManager.enableTexture2D();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}
		// Now way
		if (npc.navigating != null) {
			PathPoint[] points = ((IPathMixin) npc.navigating).getPoints();
			if (points != null && points.length > 0) {
				GlStateManager.pushMatrix();
				GlStateManager.enableBlend();
				GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
				GlStateManager.glLineWidth(3.0f);
				GlStateManager.disableTexture2D();
				GlStateManager.depthMask(false);
				GlStateManager.translate(-dx, -dy, -dz);
				tessellator = Tessellator.getInstance();
				buffer = tessellator.getBuffer();
				buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

				r = 0.156862f;
				g = 0.705882f;
				b = 0.352941f;
				pre = new Vec3d(npc.posX, npc.posY + (double) npc.getEyeHeight(), npc.posZ);
				int currentPos = points.length - 1;
				double md = -1.0d;
				for (int i = 0; i < points.length; i++) {
					double d = npc.getDistance((double) points[i].x + 0.5d,
							(double) points[i].y + (double) npc.getEyeHeight() / 2.0d, (double) points[i].z + 0.5d);
					if (md == -1.0d || d <= md) {
						md = d;
						currentPos = i;
					}
				}
				for (int i = currentPos; i < points.length; i++) {
					Vec3d vec = new Vec3d((double) points[i].x + 0.5d,
							(double) points[i].y + (double) npc.getEyeHeight() / 2.0d,
							(double) points[i].z + 0.5d);
					buffer.pos(pre.x, pre.y, pre.z).color(r, g, b, 1.0f).endVertex();
					buffer.pos(vec.x, vec.y, vec.z).color(r, g, b, 1.0f).endVertex();
					pre = vec;
				}
				tessellator.draw();
				GlStateManager.depthMask(true);
				GlStateManager.enableTexture2D();
				GlStateManager.disableBlend();
				GlStateManager.popMatrix();
			}
		}
		// Can Way
		if (movingPath.size() > 1) {
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
					GlStateManager.DestFactor.ZERO);
			GlStateManager.glLineWidth(2.0f);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			GlStateManager.translate(-dx, -dy, -dz);
			tessellator = Tessellator.getInstance();
			buffer = tessellator.getBuffer();
			buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
			r = 0.8f;
			g = 0.8f;
			b = 0.8f;
			pre = null;
			for (Vec3d vec : movingPath) {
				if (vec == null) { pre = null; }
				else {
					if (pre != null) {
						buffer.pos(pre.x, pre.y, pre.z).color(r, g, b, 1.0f).endVertex();
						buffer.pos(vec.x, vec.y, vec.z).color(r, g, b, 1.0f).endVertex();
					}
					pre = vec;
				}
			}
			pre = movingPath.get(0);
			Vec3d vec = movingPath.get(movingPath.size() - 1);
			if (type && pre != null && vec != null) {
				buffer.pos(pre.x, pre.y, pre.z).color(r, g, b, 1.0f).endVertex();
				buffer.pos(vec.x, vec.y, vec.z).color(r, g, b, 1.0f).endVertex();
			}
			tessellator.draw();
			GlStateManager.depthMask(true);
			GlStateManager.enableTexture2D();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}
		// Way
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.glLineWidth(2.0f);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		GlStateManager.translate(-dx, -dy, -dz);
		tessellator = Tessellator.getInstance();
		buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		if (type) { r = 0.75f; g = 0.0f; }
		else { r = 0.0f; g = 0.0f; }
		b = 0.75f;
		pre = null;
		for (int i = 0; i < list.size(); i++) {
			int[] pos = list.get(i);
			double yo = 0.0d;
			IBlockState state = mc.world.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
			if (state.isFullBlock() || state.isFullCube()) { yo = 1.0d; }
			Vec3d vec = new Vec3d(pos[0] + 0.5d, pos[1] + 0.5d + yo, pos[2] + 0.5d);
			if (pre != null) {
				buffer.pos(pre.x, pre.y, pre.z).color(r, g, b, 1.0f).endVertex();
				buffer.pos(vec.x, vec.y, vec.z).color(r, g, b, 1.0f).endVertex();
				IRayTraceRotate d = Util.instance.getAngles3D(vec.x, vec.y, vec.z, pre.x, pre.y, pre.z);
				// to next arrow
				for (int h = 0; h < 4; h++) {
					IRayTraceVec p = Util.instance.getPosition(vec.x, vec.y, vec.z,
							d.getYaw() + (h == 0 ? ag : h == 1 ? -1.0d * ag : 0.0d),
							-d.getPitch() + (h == 2 ? ag : h == 3 ? -1.0d * ag : 0.0d),
							0.5d);
					buffer.pos(vec.x, vec.y, vec.z).color(r, g, b, 1.0f).endVertex();
					buffer.pos(p.getX(), p.getY(), p.getZ()).color(r, g, b, 1.0f).endVertex();
				}
				// back arrow
				if (!type) {
					d = Util.instance.getAngles3D(pre.x, pre.y, pre.z, vec.x, vec.y, vec.z);
					for (int h = 0; h < 4; h++) {
						IRayTraceVec p = Util.instance.getPosition(pre.x, pre.y, pre.z,
								d.getYaw() + (h == 0 ? ag : h == 1 ? -1.0d * ag : 0.0d),
								-d.getPitch() + (h == 2 ? ag : h == 3 ? -1.0d * ag : 0.0d),
								0.35d);
						buffer.pos(pre.x, pre.y, pre.z).color(r, g, b, 1.0f).endVertex();
						buffer.pos(p.getX(), p.getY(), p.getZ()).color(0.0f, 0.0f, 1.0f, 1.0f).endVertex();
					}
				}
			}
			pre = vec;
			// end line
			if (type && i == list.size() - 1 && list.size() > 1) {
				pos = list.get(0);
				vec = new Vec3d(pos[0] + 0.5d, pos[1] + 0.5d + yo, pos[2] + 0.5d);
				buffer.pos(pre.x, pre.y, pre.z).color(r, g, b, 1.0f).endVertex();
				buffer.pos(vec.x, vec.y, vec.z).color(r, g, b, 1.0f).endVertex();
				IRayTraceRotate d = Util.instance.getAngles3D(vec.x, vec.y - yo, vec.z, pre.x, pre.y, pre.z);
				// end arrow
				for (int h = 0; h < 4; h++) {
					IRayTraceVec p = Util.instance.getPosition(vec.x, vec.y - yo, vec.z,
							360.0d - d.getYaw() + (h == 0 ? ag : h == 1 ? -1.0d * ag : 0.0d),
							0.0 - d.getPitch() + (h == 2 ? ag : h == 3 ? -1.0d * ag : 0.0d),
							0.5d);
					buffer.pos(vec.x, vec.y, vec.z).color(r, g, b, 1.0f).endVertex();
					buffer.pos(p.getX(), p.getY(), p.getZ()).color(r, g, b, 1.0f).endVertex();
				}
			}
		}
		tessellator.draw();
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
		// Block Poses
		int guiSelect = -1;
		if (mc.currentScreen instanceof GuiNpcPather && ((GuiNpcPather) mc.currentScreen).scroll != null) {
			guiSelect = ((GuiNpcPather) mc.currentScreen).scroll.getSelectedIndex();
		}
		for (int i = 0; i < list.size(); i++) {
			if (i == 0) {
				r = 0.8f;
				g = 0.8f;
				b = 0.8f;
			}
			else { b = 0.0f; }
			int[] pos = list.get(i);
			double yo = 0.0d;
			IBlockState state = mc.world.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
			if (state.isFullBlock() || state.isFullCube()) { yo = 1.0d; }
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
					GlStateManager.DestFactor.ZERO);
			GlStateManager.glLineWidth(i == 0 ? 3.0f : 2.0F);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			GlStateManager.translate(pos[0] - dx + 0.5d, pos[1] - dy + 0.5d + yo, pos[2] - dz + 0.5d);
			double s = i == 0 ? 0.125d : 0.075d;
			RenderGlobal.drawSelectionBoundingBox((new AxisAlignedBB(-s, -s, -s, s, s, s)), r, g, b, 1.0f);
			if (guiSelect == i) {
				s *= 1.75d;
				RenderGlobal.drawSelectionBoundingBox((new AxisAlignedBB(-s, -s, -s, s, s, s)), 0.0f, 1.0f, 1.0f, 0.8f);
			}
			GlStateManager.depthMask(true);
			GlStateManager.enableTexture2D();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}
	}

	private void renderRegion(float partialTicks, Zone3D reg, int editID) {
		mc = Minecraft.getMinecraft();
		if (reg == null || reg.size() == 0) { return; }
		double distMin = Double.MAX_VALUE;
		boolean start = true;
		Point playerPoint = new Point(mc.player.getPosition().getX(), mc.player.getPosition().getZ());
		Point nearestPoint = null;
		double[] nt = new double[] { 0.0d, 255.0d };

		// vertex, bound and get nearest point
		renderRegion(reg, editID);
		if (reg.getId() != editID) { return; }
		// select point
		// select point
		for (Point p : reg.points.values()) {
			if (start || distMin > p.distance(playerPoint)) {
				start = false;
				distMin = p.distance(playerPoint);
				nearestPoint = p;
				nt[0] = (double) reg.y[0] - 0.175d;
				nt[1] = (double) reg.y[1] + 1.175d;
			}
		}
		if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit != RayTraceResult.Type.MISS) {
			BlockPos p = mc.objectMouseOver.getBlockPos();
			float min = p.getY() < reg.y[0] ? (float) p.getY() : (float) reg.y[0];
			float max = (p.getY() > reg.y[1] ? (float) p.getY() : (float) reg.y[1]) + 1.0f;
			float x = p.getX() + 0.5f;
			float y = p.getY() + 0.5f;
			float z = p.getZ() + 0.5f;
			if (mc.objectMouseOver.entityHit != null) {
				switch (mc.objectMouseOver.entityHit.getHorizontalFacing()) {
					case UP: y += 0.55f; break;
					case NORTH: z -= 0.55f; break;
					case SOUTH: z += 0.55f; break;
					case WEST: x -= 0.55f; break;
					case EAST: x += 0.55f; break;
					default: y -= 0.55f; break;
				}
			}
			renderVertex(x, y, z, 1.0f, 1.0f, 0.0f);
			// Bound
			Point pb = new Point((int) Math.floor(x), (int) Math.floor(z));
			Point[] pns = reg.getClosestPoints(pb, mc.player.posX, mc.player.posZ);
			renderAddSegment(pns, pb, min, max);
		}
		if (nearestPoint != null) {
			float red = 255.0f - (float) (reg.color >> 16 & 255) / 255.0f;
			float green = 255.0f - (float) (reg.color >> 8 & 255) / 255.0f;
			float blue = 255.0f - (float) (reg.color & 255) / 255.0f;

			GlStateManager.pushMatrix();
			GlStateManager.disableTexture2D();
			GlStateManager.disableLighting();
			GlStateManager.disableCull();
			GlStateManager.disableBlend();
			RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(nearestPoint.x + 0.35d, nt[0], nearestPoint.y + 0.35d,
					nearestPoint.x + 0.65d, nt[1], nearestPoint.y + 0.65d).offset(-dx, -dy, -dz), red, green, blue, 1.0f);
			GlStateManager.enableTexture2D();
			GlStateManager.enableLighting();
			GlStateManager.enableCull();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();

			float playerX = (float) mc.player.posX + (float) (mc.player.motionX * partialTicks) - (float) dx - (float) mc.player.motionX;
			float playerY = (float) mc.player.posY + mc.player.getEyeHeight() * 0.9f + (float) (mc.player.motionY * partialTicks) - (float) dy - (float) mc.player.motionY;
			float playerZ = (float) mc.player.posZ + (float) (mc.player.motionZ * partialTicks) - (float) dz - (float) mc.player.motionZ;
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder buffer = tessellator.getBuffer();
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
					GlStateManager.DestFactor.ZERO);
			GlStateManager.glLineWidth(2.0f);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			GlStateManager.translate(-dx, -dy, -dz);
			buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

			buffer.pos(playerX, playerY, playerZ).color(red, green, blue, 1.0f).endVertex();
			buffer.pos((float) nearestPoint.x + 0.5f - (float) dx,
					(float) (nt[0] + (nt[1] - nt[0]) / 2.0d - dy),
					(float) nearestPoint.y + 0.5f - (float) dz).color(red, green, blue, 1.0f).endVertex();
			tessellator.draw();
			GlStateManager.depthMask(true);
			GlStateManager.enableTexture2D();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		} // nearest vertex
	}

	private void renderRegion(Zone3D reg, int editID) {
		if (reg == null || reg.size() == 0) { return; }
		long time = mc.world != null ? mc.world.getTotalWorldTime() : 0L;
		float red = (float) (reg.color >> 16 & 255) / 255.0f;
		float green = (float) (reg.color >> 8 & 255) / 255.0f;
		float blue = (float) (reg.color & 255) / 255.0f;
		// polygon texture size
		int xm = reg.getMinX();
		int xs = reg.getMaxX() - reg.getMinX();
		int zm = reg.getMinZ();
		int zs = reg.getMaxZ() - zm;
		double size = (double) (Math.max(xs, zs)) / 4.0D;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		GlStateManager.disableCull(); // Enable double-sided rendering
		// Walls
		if (reg.size() > 1) {
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
					GlStateManager.DestFactor.ZERO);
			GlStateManager.depthMask(false);
			GlStateManager.translate(-dx, -dy, -dz);
			GlStateManager.color(red, green, blue, 1.0f);
			mc.getTextureManager().bindTexture(BORDER[(int) (time % 16L)]);
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
			float startU = 0.0f;
			float distH = reg.getHeight() + 1.0f;
			for (int pos : reg.points.keySet()) {
				Point p0 = reg.points.get(pos);
				boolean isEnd = pos == reg.points.size() - 1;
				Point p1 = isEnd ? reg.points.get(0) : reg.points.get(pos + 1);
				// seamless texture connection between walls
				float distW = (float) p0.distance(p1);
				if (isEnd) { distW = Math.round(distW + startU); }
				buffer.pos(p0.x + 0.5f, reg.y[0], p0.y + 0.5f).tex(startU, 0.0f).endVertex();
				buffer.pos(p1.x + 0.5f, reg.y[0], p1.y + 0.5f).tex(startU + distW, 0.0f).endVertex();
				buffer.pos(p1.x + 0.5f, reg.y[1] + 1.0f, p1.y + 0.5f).tex(startU + distW, distH).endVertex();
				buffer.pos(p0.x + 0.5f, reg.y[1] + 1.0f, p0.y + 0.5f).tex(startU, distH).endVertex();
				startU += distW % 1.0f;
			}
			GlStateManager.scale(1.0F, 1.0F, 1.0F);
			tessellator.draw();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.depthMask(true);
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}
		// Polygons up and down
		if (reg.size() > 2) {
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
					GlStateManager.DestFactor.ZERO);
			GlStateManager.depthMask(false);
			GlStateManager.translate(-dx, -dy, -dz);
			GlStateManager.color(red, green, blue, 1.0f);
			mc.getTextureManager().bindTexture(BORDER[(int) (time % 16L)]);
			buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX);
			List<float[][]> triangleList = reg.getTriangleList();
			double texU0, texV0, texU1, texV1, texU2, texV2;
			for (int i = 0; i < 2; i++) {
				float y = i == 0 ? (float) reg.y[1] + 0.98f : (float) reg.y[0] + 0.02f;
				for (float[][] tri : triangleList) {
					texU0 = 2.0f * size * (tri[0][0] - xm) / (double) xs;
					texV0 = 2.0f * size * (tri[0][1] - zm) / (double) zs;
					texU1 = 2.0f * size * (tri[1][0] - xm) / (double) xs;
					texV1 = 2.0f * size * (tri[1][1] - zm) / (double) zs;
					texU2 = 2.0f * size * (tri[2][0] - xm) / (double) xs;
					texV2 = 2.0f * size * (tri[2][1] - zm) / (double) zs;
					buffer.pos(tri[0][0] + 0.5f, y, tri[0][1] + 0.5f).tex(texU0, texV0).endVertex();
					buffer.pos(tri[1][0] + 0.5f, y, tri[1][1] + 0.5f).tex(texU1, texV1).endVertex();
					buffer.pos(tri[2][0] + 0.5f, y, tri[2][1] + 0.5f).tex(texU2, texV2).endVertex();
				}
			}
			tessellator.draw();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.depthMask(true);
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}
		GlStateManager.enableCull(); // Returning to standard rendering mode
		if (reg.getId() == editID) {
			// all vertex
			for (int i = 0; i < reg.points.size(); i++) {
				Point p0 = reg.points.get(i);
				// vertex as * down and up
				if (mc.player.getHeldItemMainhand().getItem() instanceof ItemBoundary) {
					renderVertex((double) p0.x + 0.5d, reg.y[0], (double) p0.y + 0.5d, red, green, blue);
					renderVertex((double) p0.x + 0.5d, (double) reg.y[1] + 1.0d, (double) p0.y + 0.5d, red, green, blue);
				}
			}
			// bound lines
			List<float[]> lines = reg.getContourLines();

			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
					GlStateManager.DestFactor.ZERO);
			GlStateManager.glLineWidth((float) 2.0);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			GlStateManager.translate(-dx, -dy, -dz);
			buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
			float x = 0.5f;
			float z = 0.5f;
			float minY = reg.y[0];
			float maxY = 1.0f + reg.y[1];
			// horizontal lines
			for (int i = 0; i < 2; i++) {
				float y = i == 0 ? minY : maxY;
				for (float[] cl : lines) {
					buffer.pos(cl[0] + x, y, cl[1] + z).color(red, green, blue, 0.5f).endVertex();
					buffer.pos(cl[2] + x, y, cl[3] + z).color(red, green, blue, 0.5f).endVertex();
					buffer.pos(cl[0] + x, y, cl[1] + z).color(red, green, blue, 0.5f).endVertex();
					buffer.pos(cl[2] + x, y, cl[3] + z).color(red, green, blue, 0.5f).endVertex();
				}
			}
			// vertical lines
			for (int i = 0; i < reg.points.size(); i++) {
				Point p0 = reg.points.get(i);
				buffer.pos(p0.x + x, minY, p0.y + z).color(red, green, blue, 0.5f).endVertex();
				buffer.pos(p0.x + x, maxY, p0.y + z).color(red, green, blue, 0.5f).endVertex();
				buffer.pos(p0.x + x, minY, p0.y + z).color(red, green, blue, 0.5f).endVertex();
				buffer.pos(p0.x + x, maxY, p0.y + z).color(red, green, blue, 0.5f).endVertex();
			}
			tessellator.draw();
			GlStateManager.popMatrix();
		}
	}

	private void renderAddSegment(Point[] pns, Point p1, double minY, double maxY) {
		if (pns == null || pns.length != 2 || p1 == null) {
			return;
		}
		Point p0 = pns[0], p2 = pns[1];
		if (p0 == null || p2 == null) {
			return;
		}
		float wallAlpha = 0.11f;
		// Walls
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		GlStateManager.translate(-dx, -dy, -dz);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

		buffer.pos(p0.x + 0.5d, minY, p0.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p0.x + 0.5d, maxY, p0.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p1.x + 0.5d, maxY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p1.x + 0.5d, minY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p1.x + 0.5d, minY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p1.x + 0.5d, maxY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p0.x + 0.5d, maxY, p0.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p0.x + 0.5d, minY, p0.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();

		buffer.pos(p1.x + 0.5d, minY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p1.x + 0.5d, maxY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p2.x + 0.5d, maxY, p2.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p2.x + 0.5d, minY, p2.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p2.x + 0.5d, minY, p2.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p2.x + 0.5d, maxY, p2.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p1.x + 0.5d, maxY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();
		buffer.pos(p1.x + 0.5d, minY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, wallAlpha).endVertex();

		tessellator.draw();
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();

		// Lines
		buffer = tessellator.getBuffer();
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.glLineWidth(2.0f);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		GlStateManager.translate(-dx, -dy, -dz);
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(p0.x + 0.5d, minY, p0.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p1.x + 0.5d, minY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p1.x + 0.5d, minY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p2.x + 0.5d, minY, p2.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		if (maxY - minY > 1) {
			for (double i = 1.0d; i < maxY - minY; i++) {
				buffer.pos(p0.x + 0.5d, minY + i, p0.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
				buffer.pos(p1.x + 0.5d, minY + i, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
				buffer.pos(p1.x + 0.5d, minY + i, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
				buffer.pos(p2.x + 0.5d, minY + i, p2.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
			}
		}
		buffer.pos(p0.x + 0.5d, maxY, p0.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p1.x + 0.5d, maxY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p1.x + 0.5d, maxY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p2.x + 0.5d, maxY, p2.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p0.x + 0.5d, minY, p0.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p0.x + 0.5d, maxY, p0.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p1.x + 0.5d, minY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p1.x + 0.5d, maxY, p1.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p2.x + 0.5d, minY, p2.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		buffer.pos(p2.x + 0.5d, maxY, p2.y + 0.5d).color(0.75f, 0.75f, 0.75f, 1.0f).endVertex();
		tessellator.draw();
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	private void renderVertex(double x, double y, double z, float red, float green, float blue) {
		double sizeS = 0.15D;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.glLineWidth(2.0f);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		GlStateManager.translate(-dx, -dy, -dz);
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(x - sizeS, y - sizeS, z - sizeS).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(x + sizeS, y + sizeS, z + sizeS).color(red, green, blue, 1.0f).endVertex();

		buffer.pos(x - sizeS, y - sizeS, z + sizeS).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(x + sizeS, y + sizeS, z - sizeS).color(red, green, blue, 1.0f).endVertex();

		buffer.pos(x + sizeS, y - sizeS, z + sizeS).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(x - sizeS, y + sizeS, z - sizeS).color(red, green, blue, 1.0f).endVertex();

		buffer.pos(x + sizeS, y - sizeS, z - sizeS).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(x - sizeS, y + sizeS, z + sizeS).color(red, green, blue, 1.0f).endVertex();

		buffer.pos(x - sizeS, y, z).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(x + sizeS, y, z).color(red, green, blue, 1.0f).endVertex();

		buffer.pos(x, y - sizeS, z).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(x, y + sizeS, z).color(red, green, blue, 1.0f).endVertex();

		buffer.pos(x, y, z - sizeS).color(red, green, blue, 1.0f).endVertex();
		buffer.pos(x, y, z + sizeS).color(red, green, blue, 1.0f).endVertex();
		tessellator.draw();
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	private void renderZone(BuilderData builder, BlockPos pos) {
		if (builder.getType() < 3) {
			int[] s = new int[] { 0, 0, 0 };
			int[] e = new int[] { 1, 1, 1 };
			float r = 1.0f, g = 0.0f, b = 0.0f;
			int[] m = builder.getDirections(mc.player);
			for (int j = 0; j < 3; j++) {
				s[j] = m[j];
				e[j] = m[j + 3];
			}
			if (builder.getType() == 1) {
				r = 0.0f;
				g = 1.0f;
				b = 1.0f;
			} else if (builder.getType() == 2) {
				g = 0.0f;
				b = 1.0f;
			}
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
					GlStateManager.DestFactor.ZERO);
			GlStateManager.glLineWidth(5.0F);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			GlStateManager.translate(pos.getX() + s[0] - dx, pos.getY() + s[1] - dy,
					pos.getZ() + s[2] - dz);
			RenderGlobal.drawSelectionBoundingBox((new AxisAlignedBB(0, 0, 0, e[0], e[1], e[2])), r, g, b, 1.0f);
			GlStateManager.depthMask(true);
			GlStateManager.enableTexture2D();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();

			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
					GlStateManager.DestFactor.ZERO);
			GlStateManager.glLineWidth(5.0F);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			GlStateManager.translate(pos.getX() - dx + 0.5d, pos.getY() - dy, pos.getZ() - dz + 0.5d);
			RenderGlobal.drawSelectionBoundingBox((new AxisAlignedBB(-0.5d, 0.0d, -0.5d, 0.5d, 1.0d, 0.5d)), 1.0f, 1.0f,
					1.0f, 1.0f);
			GlStateManager.depthMask(true);
			GlStateManager.enableTexture2D();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}
		if (builder.getType() == 3) {
			if (!builder.schematicName.isEmpty()) {
				String name = builder.schematicName + ".schematic";
				if (builder.schema == null) {
					builder.schema = SchematicController.Instance.load(name);
					if (builder.schema == null && !builder.schMap.isEmpty()) {
						builder.schema = new SchematicWrapper(Schematic.create(mc.world, mc.player.getHorizontalFacing(), name, builder.schMap));
					}
				}
				renderSchem(builder.schema, mc.player.getHorizontalFacing().getIndex() - 2,
						pos.getX() - dx + 1.0f, pos.getY() - dy + 0.003f, pos.getZ() - dz + 1.0f);
			}
		}
		else if (builder.getType() == 4) {
			if (!builder.schMap.containsKey(0)) {
				return;
			}
			pos = builder.schMap.get(0);
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
					GlStateManager.DestFactor.ZERO);
			GlStateManager.glLineWidth(5.0F);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			GlStateManager.translate(pos.getX() - dx, pos.getY() - dy, pos.getZ() - dz);
			RenderGlobal.drawSelectionBoundingBox((new AxisAlignedBB(0, 0, 0, 1, 1, 1)), 1, 1, 1, 1.0f);
			GlStateManager.depthMask(true);
			GlStateManager.enableTexture2D();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
			if (builder.schMap.containsKey(1)) {
				pos = builder.schMap.get(1);
				GlStateManager.pushMatrix();
				GlStateManager.enableBlend();
				GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
						GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
						GlStateManager.DestFactor.ZERO);
				GlStateManager.glLineWidth(3.0F);
				GlStateManager.disableTexture2D();
				GlStateManager.depthMask(false);
				GlStateManager.translate(pos.getX() - dx, pos.getY() - dy, pos.getZ() - dz);
				RenderGlobal.drawSelectionBoundingBox((new AxisAlignedBB(0, 0, 0, 1, 1, 1)), 0, 1, 0, 1.0f);
				GlStateManager.depthMask(true);
				GlStateManager.enableTexture2D();
				GlStateManager.disableBlend();
				GlStateManager.popMatrix();
			}
			if (builder.schMap.containsKey(2)) {
				pos = builder.schMap.get(2);
				GlStateManager.pushMatrix();
				GlStateManager.enableBlend();
				GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
						GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
						GlStateManager.DestFactor.ZERO);
				GlStateManager.glLineWidth(3.0F);
				GlStateManager.disableTexture2D();
				GlStateManager.depthMask(false);
				GlStateManager.translate(pos.getX() - dx, pos.getY() - dy, pos.getZ() - dz);
				RenderGlobal.drawSelectionBoundingBox((new AxisAlignedBB(0, 0, 0, 1, 1, 1)), 0, 0, 1, 1.0f);
				GlStateManager.depthMask(true);
				GlStateManager.enableTexture2D();
				GlStateManager.disableBlend();
				GlStateManager.popMatrix();
			}
			if (builder.schMap.containsKey(1) && builder.schMap.containsKey(2)) {
				AxisAlignedBB aabb = new AxisAlignedBB(builder.schMap.get(1), builder.schMap.get(2));
				pos = new BlockPos(aabb.minX, aabb.minY, aabb.minZ);
				GlStateManager.pushMatrix();
				GlStateManager.enableBlend();
				GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
						GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
						GlStateManager.DestFactor.ZERO);
				GlStateManager.glLineWidth(3.0F);
				GlStateManager.disableTexture2D();
				GlStateManager.depthMask(false);
				GlStateManager.translate(pos.getX() - dx, pos.getY() - dy, pos.getZ() - dz);
				RenderGlobal.drawSelectionBoundingBox((new AxisAlignedBB(0, 0, 0, aabb.maxX - aabb.minX + 1,
						aabb.maxY - aabb.minY + 1, aabb.maxZ - aabb.minZ + 1)), 1, 0, 0, 1.0f);
				GlStateManager.depthMask(true);
				GlStateManager.enableTexture2D();
				GlStateManager.disableBlend();
				GlStateManager.popMatrix();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void updateMiniMaps(boolean update) {
		PlayerMiniMapData mm = CustomNpcs.proxy.getPlayerData(Minecraft.getMinecraft().player).minimap;
		// Check save client Points:
		List<MiniMapData> points = new ArrayList<>();
		if (Loader.isModLoaded("journeymap")) {
			mm.addData.clear();
			if (!mm.modName.equals("journeymap")) {
				mm.modName = "journeymap";
				update = true;
			}
			try {
				Class<?> ws = Class.forName("journeymap.client.waypoint.WaypointStore");
				miniMapLoaded = (boolean) ws.getDeclaredMethod("hasLoaded").invoke(ws.getEnumConstants()[0]);
				if (!miniMapLoaded) {
					CustomNPCsScheduler.runTack(() -> updateMiniMaps(true), 50);
					return;
				}
				Collection<Object> waypoints = (Collection<Object>) ws.getDeclaredMethod("getAll")
						.invoke(ws.getEnumConstants()[0]); // Collection<Object> waypoints =
															// WaypointStore.INSTANCE.getAll();
				for (Object waypoint : waypoints) {
					Class<?> wc = waypoint.getClass();
					MiniMapData mmd = new MiniMapData();
					mmd.name = (String) wc.getDeclaredMethod("getName").invoke(waypoint); // String
					mmd.type = wc.getDeclaredMethod("getType").invoke(waypoint).toString(); // Normal, Death
					mmd.icon = wc.getDeclaredMethod("getIcon").invoke(waypoint).toString(); // String
					mmd.pos = new BlockPosWrapper(null, (BlockPos) wc.getDeclaredMethod("getBlockPos").invoke(waypoint)); // BlockPos
					mmd.color = new Color((int) wc.getDeclaredMethod("getR").invoke(waypoint),
							(int) wc.getDeclaredMethod("getG").invoke(waypoint),
							(int) wc.getDeclaredMethod("getB").invoke(waypoint)).getRGB();
					mmd.isEnable = (boolean) wc.getDeclaredMethod("isEnable").invoke(waypoint);
					Collection<Integer> dimensions = (Collection<Integer>) wc.getDeclaredMethod("getDimensions")
							.invoke(waypoint);
					mmd.dimIDs = new ArrayList<>();
                    mmd.dimIDs.addAll(dimensions);

					mmd.id = points.size();
					points.add(mmd);
					
					MiniMapData mmp = mm.get(mmd);
					if (mmp != null) { mmd.setQuest(mmp); } else { update = true; }
				}
			} catch (Exception e) { LogWriter.debug("JourneyMap tried to collect its points"); }
		}
		else if (Loader.isModLoaded("xaerominimap")) {
			if (!mm.modName.equals("xaerominimap")) {
				mm.modName = "xaerominimap";
				update = true;
			}
			try {
				Class<?> xms = Class.forName("xaero.common.XaeroMinimapSession");
				Object minimapSession = xms.getDeclaredMethod("getCurrentSession").invoke(xms); // XaeroMinimapSession
				if (minimapSession != null) {
					Field fl = xms.getDeclaredField("usable");
					fl.setAccessible(true);
					miniMapLoaded = fl.getBoolean(minimapSession);
				} else {
					miniMapLoaded = false;
				}
				if (!miniMapLoaded) {
					CustomNPCsScheduler.runTack(() -> updateMiniMaps(true), 50);
					return;
				}
				// xaero.common.minimap.waypoints.WaypointsManager
				Object waypointsManager = xms.getDeclaredMethod("getWaypointsManager").invoke(minimapSession);

				Method getWaypointMap = waypointsManager.getClass().getDeclaredMethod("getWaypointMap");
				// HashMap<String, WaypointWorldContainer>
				HashMap<String, Object> waypointMap = (HashMap<String, Object>) getWaypointMap.invoke(waypointsManager);

				String mainContainerID = (String) waypointsManager.getClass()
						.getDeclaredMethod("getAutoRootContainerID").invoke(waypointsManager);
				// xaero.common.minimap.waypoints.WaypointWorldContainer
				Object wwrc = waypointMap.get(mainContainerID);
				if (wwrc == null) {
					if (!mm.points.isEmpty()) {
						mm.points.clear();
						update = true;
					}
				}
				else {
					mm.addData.clear();
					Gson gson = new Gson();
					Field subContainers = wwrc.getClass().getField("subContainers");
					Field worlds = wwrc.getClass().getField("worlds");
					// HashMap<String, WaypointWorldContainer>
					HashMap<String, Object> dimMap = (HashMap<String, Object>) subContainers.get(wwrc);
					for (String k : dimMap.keySet()) {
						if (!mm.addData.containsKey("xaero_world_name")) {
							String world_name = (String) dimMap.get(k).getClass().getDeclaredMethod("getKey").invoke(dimMap.get(k));
							while (world_name.lastIndexOf("/") != -1) {
								world_name = world_name.substring(0, world_name.lastIndexOf("/"));
							}
							mm.addData.put("xaero_world_name", world_name);
						}
						// HashMap<String, WaypointWorld>
						HashMap<String, Object> worldMap = (HashMap<String, Object>) worlds.get(dimMap.get(k));
						for (String k1 : worldMap.keySet()) {
							if (!k1.equals("waypoints")) { continue; }
							Object waypointWorld = worldMap.get(k1); // WaypointWorld
							int dimId = (int) waypointWorld.getClass().getDeclaredMethod("getDimId") .invoke(waypointWorld);
							Field f = waypointWorld.getClass().getDeclaredField("sets");
							f.setAccessible(true);
							// HashMap<String, WaypointSet>
							HashMap<String, Object> sets = (HashMap<String, Object>) f.get(waypointWorld);
							for (String ks : sets.keySet()) {
								// xaero.common.minimap.waypoints.WaypointSet
								Object waypointSet = sets.get(ks);
								f = waypointSet.getClass().getDeclaredField("list");
								f.setAccessible(true);
								// ArrayList<Waypoint>
								ArrayList<Object> list = (ArrayList<Object>) f.get(waypointSet);
								for (Object waypoint : list) {
									// xaero.common.minimap.waypoints.Waypoint
									Class<?> wc = waypoint.getClass();
									MiniMapData mmd = new MiniMapData();
									mmd.name = (String) wc.getDeclaredMethod("getName").invoke(waypoint);
									mmd.type = wc.getDeclaredMethod("getWaypointType").invoke(waypoint).toString();
									mmd.icon = (String) wc.getDeclaredMethod("getSymbol").invoke(waypoint);
									int x = (int) wc.getDeclaredMethod("getX").invoke(waypoint);
									int y = (int) wc.getDeclaredMethod("getY").invoke(waypoint);
									int z = (int) wc.getDeclaredMethod("getZ").invoke(waypoint);
									mmd.pos = new BlockPosWrapper(null, new BlockPos(x, y, z));
									mmd.color = (int) wc.getDeclaredMethod("getColor").invoke(waypoint);
									mmd.isEnable = !((boolean) wc.getDeclaredMethod("isDisabled").invoke(waypoint));
									mmd.dimIDs = new ArrayList<>(Collections.singletonList(dimId));
									mmd.gsonData.put("temporary", gson.toJson(wc.getDeclaredMethod("isTemporary").invoke(waypoint)));
									mmd.id = points.size();
									points.add(mmd);
									MiniMapData mmp = mm.get(mmd);
									if (mmp != null) { mmd.setQuest(mmp); } else { update = true; }
								}
							}
						}
					}
				}
			} catch (Exception e) {LogWriter.debug("XaeroMap tried to collect its points");}
		}
		else if (Loader.isModLoaded("voxelmap")) {
			mm.addData.clear();
			if (!mm.modName.equals("voxelmap")) {
				mm.modName = "voxelmap";
				update = true;
			}
			try {
				Class<?> vm = Class.forName("com.mamiyaotaru.voxelmap.interfaces.VoxelMap");
				// com.mamiyaotaru.voxelmap.VoxelMap
				Object instance = vm.getMethod("getInstance").invoke(vm);
				// com.mamiyaotaru.voxelmap.interfaces.IWaypointManager
				Object waypointManager = vm.getMethod("getWaypointManager").invoke(instance);

				Field fl = waypointManager.getClass().getDeclaredField("loaded");
				fl.setAccessible(true);
				miniMapLoaded = fl.getBoolean(waypointManager);
				if (!miniMapLoaded) {
					CustomNPCsScheduler.runTack(() -> updateMiniMaps(true), 50);
					return;
				}
				List<Object> waypoints = (List<Object>) waypointManager.getClass().getMethod("getWaypoints").invoke(waypointManager);
				for (Object waypoint : waypoints) {
					Class<?> wc = waypoint.getClass();
					MiniMapData mmd = new MiniMapData();
					mmd.name = (String) wc.getDeclaredField("name").get(waypoint);
					mmd.gsonData.put("voxel_world_name", (String) wc.getDeclaredField("world").get(waypoint));
					mmd.icon = (String) wc.getDeclaredField("imageSuffix").get(waypoint);
					int x = (int) wc.getDeclaredField("x").get(waypoint);
					int y = (int) wc.getDeclaredField("y").get(waypoint);
					int z = (int) wc.getDeclaredField("z").get(waypoint);
					mmd.pos = Objects.requireNonNull(NpcAPI.Instance()).getIPos(x, y, z);
					mmd.color = new Color((float) wc.getDeclaredField("red").get(waypoint),
							(float) wc.getDeclaredField("green").get(waypoint),
							(float) wc.getDeclaredField("blue").get(waypoint)).getRGB();
					mmd.isEnable = (boolean) wc.getDeclaredField("enabled").get(waypoint);
					TreeSet<Integer> dimensions = (TreeSet<Integer>) wc.getDeclaredField("dimensions").get(waypoint);
					mmd.dimIDs = new ArrayList<>(dimensions);
					mmd.id = points.size();
					points.add(mmd);
					
					MiniMapData mmp = mm.get(mmd);
					if (mmp != null) { mmd.setQuest(mmp); } else { update = true; }
				}
			} catch (Exception e) {LogWriter.debug("VoxelMap tried to collect its points");}
		}
		else {
			mm.addData.clear();
			if (!mm.modName.equals("non")) {
				mm.modName = "non";
				update = true;
			}
		}
		if (!update && points.size() != mm.points.size()) { update = true; }
		// Send
		if (update) {
			mm.points.clear();
			mm.points.addAll(points);
			Packets.sendServer(new SPacketSyncUpdate(6, mm.save(new NBTTagCompound())));
		}
	}

	/** HUD: Mail Overlay */
	public void renderMailOverlay(ScaledResolution sr) {
		if ((hasNewMail || startMail > 0L) && CustomNpcs.MailWindow != -1) {
			mc = Minecraft.getMinecraft();
			sw = sr;
			CustomNpcs.MailWindow = 1;
			int[] offsets = new int[2];
			float sr_rot = -45.0f, su = 12.0f, sv = -32.0f;
			offsets[1] = (int) CustomNpcs.proxy.getPlayerData(mc.player).overlay.getWindowSize().getHeight() - 32;

			GlStateManager.pushMatrix();
			GlStateManager.translate(offsets[0] + 16, offsets[1] + 16, 0);

			if (startMail == 0L) { startMail = System.currentTimeMillis(); }
			long time = System.currentTimeMillis() - startMail;

			// Start animation
			if (showNewMail == 0L || (time - showNewMail > -500L && time - showNewMail < 0L)) {
				if (showNewMail == 0L) { showNewMail = time + 500L; }
				time -= showNewMail;
				GlStateManager.rotate(sr_rot * (float) time / 500.0f, 0.0f, 0.0f, 1.0f);
				GlStateManager.translate(su * (float) time / 500.0f, sv * (float) time / 500.0f, 0);
				if (time >= 0L) { startMail = 0L; }
			}
			// End animation
			if (!hasNewMail) {
				if (time > 0L) {
					startMail = System.currentTimeMillis() + 500L;
					time = System.currentTimeMillis() - startMail;
				}
				time += 500L;
				time *= -1L;
				GlStateManager.rotate(sr_rot * (float) time / 500.0f, 0.0f, 0.0f, 1.0f);
				GlStateManager.translate(su * (float) time / 500.0f, sv * (float) time / 500.0f, 0);
				if (time < -480L) { startMail = 0L; }
			}
			// Living animation
			else if (time % 31500 < 1750) {
				time = time % 1750;
				if (time < 500) {
					GlStateManager.rotate(30.0f * (float) time / 500.0f, 0.0f, 0.0f, 1.0f);
					GlStateManager.translate(-1.0f * (float) time / 500.0f, -5.0f * (float) time / 500.0f, 0);
				} else if (time < 1250) {
					GlStateManager.rotate(30.0f - 420.0f * (float) (time -= 500L) / 750.0f, 0.0f, 0.0f, 1.0f);
					GlStateManager.translate(-1.0f + (float) time / 750.0f, -5.0f + 5.0f * (float) time / 750.0f, 0);
				} else {
					GlStateManager.rotate(-30.0f + 30.0f * (float) (time - 1250L) / 500.0f, 0.0f, 0.0f, 1.0f);
				}
			}

			// Alpha pulse
			time = System.currentTimeMillis() % 3000;
			if (time < 1500) {
				GlStateManager.color(0.85f, 0.85f, 0.85f, 0.5f + 0.45f * (float) time / 1500.f);
			} else {
				GlStateManager.color(0.85f, 0.85f, 0.85f, 0.5f + 0.45f * (3000.0f - (float) time) / 1500.f);
			}

			GlStateManager.scale(0.5f, 0.5f, 0.5f);
			GlStateManager.enableBlend();
			mc.getTextureManager().bindTexture(GuiMailmanWrite.icons);
			drawTexturedModalRect(-16, -16, 0, 0, 32, 32);
			GlStateManager.popMatrix();
		}
	}

	/** HUD: Quest Compass Overlay */
	public void renderCompassOverlay(ScaledResolution sr) {
		mc = Minecraft.getMinecraft();
		sw = sr;
		if (mc.world == null || mc.player == null) return;
		if (!(mc.currentScreen == null || mc.currentScreen instanceof GuiChat || mc.currentScreen instanceof GuiLog)) return;

		PlayerData playerData = CustomNpcs.proxy.getPlayerData(mc.player);
		PlayerCompassData compassData = playerData.compass;

		if (CustomNpcs.TypeShowQuestCompass == 4 || !compassData.getShowOfPlayer()) return;
		if (mc.gameSettings.thirdPersonView == 0 && CustomNpcs.HideCompassInFirstPerson) return;

		// Compass requirement check
		boolean isShow = true;
		if (!mc.player.isCreative() && (CustomNpcs.TypeShowQuestCompass == 2 || CustomNpcs.TypeShowQuestCompass == 3)) {
			isShow = false;
			for (int slotId = 0; slotId < mc.player.inventory.getSizeInventory(); slotId++) {
				if (mc.player.inventory.getStackInSlot(slotId).getItem() == Items.COMPASS) {
					isShow = true;
					break;
				}
			}
		}
		if (!isShow) return;

		boolean needPoint = CustomNpcs.TypeShowQuestCompass == 1 || CustomNpcs.TypeShowQuestCompass == 3;
		String name = "", title = "";
		double[] point = null;
		int taskType = -1;
		int range = 5;
		int taskColor = 0x808080;
		String n = "";
		if (compassData.isCustomPoint) {
			point = new double[] { compassData.pos.getX() - 0.5d, compassData.pos.getY() + 0.5d, compassData.pos.getZ() + 0.5d };
			name = Component.translatable(compassData.name).getFormattedText();
			title = Component.translatable(compassData.title).getFormattedText();
			taskColor = compassData.color;
			taskType = compassData.getTaskType();
			if (mc.world.provider.getDimension() != compassData.getDimensionID()) { taskType = 7; }
			range = compassData.getRange();
			if (compassData.getNPCName().isEmpty()) {
				n = Component.translatable("entity." + compassData.getNPCName() + ".name").getString();
				n = n.substring(0, n.length() - 2);
				if (n.equals("entity." + compassData.getNPCName() + ".name")) { n = compassData.getNPCName(); }
			}
		}
		else {
			// Auto-select quest with compass settings
			if (!playerData.questData.activeQuests.containsKey(playerData.compass.questID) || playerData.compass.questID <= 0) {
				for (int id : playerData.questData.activeQuests.keySet()) {
					if (playerData.questData.activeQuests.get(id).quest.hasCompassSettings() && id != playerData.compass.questID && id > 0) {
						playerData.compass.questID = id;
						break;
					}
				}
			}
			QuestData qData = playerData.questData.activeQuests.get(playerData.compass.questID);
			if (qData != null) {
				double minD = Double.MAX_VALUE;
				QuestObjective select = null;
				for (QuestObjective io : qData.quest.questInterface.getObjectives(mc.player)) {
					if (io.isCompleted()) continue;
					if (qData.quest.step != 1) {
						if (io.rangeCompass == 0 && select == null) {
							select = io;
						} else if (io.rangeCompass != 0) {
							double d = Util.instance.distanceTo(io.pos.getX() + 0.5d, io.pos.getY(), io.pos.getZ() + 0.5d, mc.player.posX, mc.player.posY + mc.player.getEyeHeight(), mc.player.posZ);
							if (d <= minD) { minD = d; select = io; }
						}
						continue;
					}
					select = io;
					break;
				}
				if (select != null) {
					name = qData.quest.getTitle().getFormattedText();
					taskType = select.getType();
					taskColor = select.colorCompass;
					if (!select.getOrientationEntityName().isEmpty()) {
						n = Component.translatable("entity." + select.getOrientationEntityName() + ".name").getFormattedText();
						n = n.substring(0, n.length() - 2);
						if (n.equals("entity." + select.getOrientationEntityName() + ".name")) n = select.getOrientationEntityName();
					}
					if (mc.world.provider.getDimension() != select.dimension) taskType = 7;
					if (taskType != EnumQuestTask.KILL.ordinal() && taskType != EnumQuestTask.AREAKILL.ordinal()) range = 1;
					if (select.rangeCompass > 0) {
						range = select.rangeCompass;
						EnumQuestTask t = EnumQuestTask.values()[select.getType()];
						point = new double[] { select.pos.getX() - 0.5d, select.pos.getY() + 0.5d, select.pos.getZ() + 0.5d };
						if (t == EnumQuestTask.ITEM) {
							title = Component.translatable("gui.get").getFormattedText() + ": " + select.getItem().getDisplayName() + ": " + select.getProgress() + "/" + select.getMaxProgress();
						} else if (t == EnumQuestTask.CRAFT) {
							title = Component.translatable("gui.get").getFormattedText() + ": " + select.getItem().getDisplayName() + ": " + select.getProgress() + "/" + select.getMaxProgress();
						} else if (t == EnumQuestTask.DIALOG) {
							title = Component.translatable("gui.read").getFormattedText() + ": ";
							Dialog dialog = DialogController.instance.dialogs.get(select.getTargetID());
							title += dialog != null ? Component.translatable(dialog.title).getFormattedText() : "Dialog";
						} else if (t == EnumQuestTask.LOCATION) {
							title = Component.translatable("gui.found").getFormattedText() + ": " + select.getTargetName();
						} else if (EnumQuestTask.values()[select.getType()] == EnumQuestTask.MANUAL) {
							title = select.getTargetName();
						}
						if (t == EnumQuestTask.KILL || t == EnumQuestTask.AREAKILL) {
							n = Component.translatable("entity." + select.getTargetName() + ".name").getFormattedText();
							n = n.substring(0, n.length() - 2);
							if (n.equals("entity." + select.getTargetName() + ".name")) n = select.getTargetName();
							title = Component.translatable("gui.kill").getFormattedText() + ": " + n + ": " + select.getProgress() + "/" + select.getMaxProgress();
						}
					}
				} else if (qData.isCompleted && qData.quest.completion == EnumQuestCompletion.Npc && !qData.quest.completer.isEmpty()) {
					point = new double[] { qData.quest.completer.getPos().getX() - 0.5d,
							qData.quest.completer.getPos().getY() + 0.5d,
							qData.quest.completer.getPos().getZ() + 0.5d };
					taskType = EnumQuestTask.DIALOG.ordinal();
					taskColor = 0x72CA00;
					if (mc.world.provider.getDimension() != qData.quest.completer.getDimension()) {
						taskType = 7;
					} else {
						AxisAlignedBB bb = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).offset(point[0], point[1], point[2]).grow(64.0d, 128.0d, 64.0d);
						List<EntityNPCInterface> ents = new ArrayList<>();
						try { ents = mc.world.getEntitiesWithinAABB(EntityNPCInterface.class, bb); } catch (Exception ignored) {}
						final EntityNPCInterface npc = getClosestNPC(point, ents, qData);
						if (npc != null) {
							point[0] = npc.posX; point[1] = npc.posY; point[2] = npc.posZ;
							range = 1;
						}
					}
				}
			}
		}
		if (!n.isEmpty() && point != null) {
			EntityLivingBase e = null;
			AxisAlignedBB bb = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).offset(point[0], point[1], point[2]).grow(range, 1.5d, range);
			List<EntityLivingBase> ents = new ArrayList<>();
			try { ents = mc.world.getEntitiesWithinAABB(EntityLivingBase.class, bb); } catch (Exception ignored) {}
			EntityPlayer pl = mc.world.getClosestPlayerToEntity(mc.player, 32.0d);
			Potion pe = Potion.getPotionFromResourceLocation("invisibility");
			if (pl != null && pe != null && pl.getActivePotionEffect(pe) == null) { e = pl; range = 1; }
			if (e == null) {
				double d = range * range * range;
				EntityLivingBase et = null;
				Vec3i v = new Vec3i(point[0], point[1], point[2]);
				for (EntityLivingBase el : ents) {
					if (!el.getName().equals(n)) continue;
					double r = v.distanceSq(el.getPosition());
					if (et != null && r >= d) continue;
					d = r; et = el;
				}
				if (et == null) {
					bb = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).offset(point[0], point[1], point[2]).grow(range, range, range);
					ents.clear();
					try { ents = mc.world.getEntitiesWithinAABB(EntityLivingBase.class, bb); } catch (Exception ignored) {}
					d = range * range * range;
					for (EntityLivingBase el : ents) {
						if (!el.getName().equals(n)) continue;
						double r = v.distanceSq(el.getPosition());
						if (et != null && r >= d) continue;
						d = r; et = el;
					}
				}
				e = et; range = 1;
			}
			if (e != null) { point[0] = e.posX; point[1] = e.posY; point[2] = e.posZ; }
		}
		if (!needPoint || point != null) {
			float scale = (compassData.isFlat ? 1.0f : -15.0f) * compassData.scale;
			int[] uvPos = new int[] { (int) (sw.getScaledWidth_double() * compassData.screenPos[0]),
					(int) (sw.getScaledHeight_double() * compassData.screenPos[1]) };

			GlStateManager.pushMatrix();
			if (qt < 40) { qt++; }
			GlStateManager.translate(uvPos[0], uvPos[1], 0.0d);
			// Named
			GlStateManager.enableBlend();
			GlStateManager.disableDepth();
			if (compassData.showQuestName || compassData.showTaskProgress) {
				GlStateManager.pushMatrix();
				float yOffset = compassData.isFlat ? 14.0f : 30.0f;
				float h = 0.0f;
				float w = 0.0f;
				float w0 = 0.0f;
				float w1 = 0.0f;
				float l = 0.0f;
				if (compassData.showQuestName && !name.isEmpty()) {
					h += 10.0f;
					w = ClientProxy.LogFont.width(name);
					w0 = w;
					if (uvPos[0] - w0 / 2.0f < 1.5f) { w0 = uvPos[0] * 2.0f - 3.0f; } // left
					if (uvPos[0] + w0 / 2.0f > (float) sw.getScaledWidth_double() - 1.5f) {
						w0 = (w0 - ((float) sw.getScaledWidth_double() - uvPos[0]) + 3.0f) * 2.0f;
					} // right
					l = w0 / -2.0f;
				}
				if (compassData.showTaskProgress && !title.isEmpty()) {
					h += 10.0f;
					w1 = ClientProxy.LogFont.width(title);
					if (w < w1) { w = w1; }
					if (uvPos[0] - w1 / 2.0f < 1.5f) { w1 = uvPos[0] * 2.0f - 3.0f; } // left
					if (uvPos[0] + w1 / 2.0f > (float) sw.getScaledWidth_double() - 1.5f) {
						w1 = (w1 - ((float) sw.getScaledWidth_double() - uvPos[0]) + 3.0f) * 2.0f;
					} // right
					if (l > w1 / -2.0f) { l = w1 / -2.0f; }
				}
				// down
				if (uvPos[1] + yOffset + h > sw.getScaledHeight_double()) { yOffset = -h; }
				GlStateManager.translate(0.0d, yOffset, 0.0d);
				// background
				if (h > 0) {
					l -= 2.0f;
					w += 5.0f;
					int color = 0x03202020;
					drawRect((int) l, 0, (int) (l + w) - 1, 1, color);
					drawRect((int) l, 1, (int) (l) + 1, (int) h - 1, color);
					drawRect((int) (l + w) - 2, 1, (int) (l + w) - 1, (int) h - 1, color);
					drawRect((int) l, (int) h - 1, (int) (l + w) - 1, (int) h, color);
					color = 0x03303030;
					drawRect((int) l + 1, 1, (int) (l + w) - 2, (int) h - 1, color);
					// name
					if (compassData.showQuestName) {
						ClientProxy.LogFont.draw(name, w0 / -2.0f, 0, 0xC0FFFFFF);
					}
					if (compassData.showTaskProgress) {
						ClientProxy.LogFont.draw(title, w1 / -2.0f, compassData.showQuestName ? 10 : 0, 0xC0FFFFFF);
					}
				}
				GlStateManager.popMatrix();
			}
			GlStateManager.enableDepth();
			// Model
			GlStateManager.pushMatrix();
			GlStateManager.translate(0.0f, compassData.isFlat ? 6.5f : -16.0f * compassData.scale + 29.0f, 0.0f);
			GlStateManager.scale(scale, scale, scale);
			float yaw = mc.player.rotationYaw % 360;
			if (yaw < 0.0f) { yaw += 360.0f; }
			if (compassData.isFlat) {
				float l = 0.0f;
				if (uvPos[0] - 101 < 1.5f) { l = 101.5f - uvPos[0]; } // left
				if (uvPos[0] + 101.0f > (float) sw.getScaledWidth_double() - 1.5f) {
					l = (float) sw.getScaledWidth_double() - uvPos[0] - 101.5f;
				} // right
				mc.getTextureManager().bindTexture(GuiBasic.INFO);
				GlStateManager.translate(l, 0.0f, 0.0f);
				// background
				GlStateManager.enableBlend();
				GlStateManager.pushMatrix();
				GlStateManager.translate(-101.5f, -7.0f, 0.0f);
				GlStateManager.scale(0.5f, 0.5f, 0.5f);
				drawTexturedModalRect(0, 0, 0, 74, 204, 28);
				GlStateManager.translate(204.0f, 0.0f, 0.0f);
				drawTexturedModalRect(0, 0, 0, 102, 204, 28);
				GlStateManager.popMatrix();

				double left = uvPos[0] - (99.0f * compassData.scale + l);
				if (left < 0.0f) { left = 2.5f * compassData.scale; }
				double top = uvPos[1] + (-6.0f * compassData.scale + 7.0f);
				double right = left + 199.0f * compassData.scale;
				double bottom = top + (11.0f * compassData.scale);

				int guiScale = mc.gameSettings.guiScale;
				if (guiScale == 0) {
					guiScale = Math.max(1, Math.min((int) (mc.displayWidth / sw.getScaledWidth_double()),
							(int) (mc.displayHeight / sw.getScaledHeight_double())));
				}
				double scaleFactor = guiScale > 0 ? (double) mc.displayWidth / sw.getScaledWidth_double() : 1.0;

				GL11.glEnable(GL11.GL_SCISSOR_TEST);
				GL11.glScissor(
						(int) Math.max(0, left * scaleFactor),
						(int) Math.max(0, (mc.displayHeight - bottom * scaleFactor)),
						(int) Math.max(0, (right - left) * scaleFactor),
						(int) Math.max(0, (bottom - top) * scaleFactor)
				);

				// Dial
				if (compassData.isShowDial()) {
					GlStateManager.pushMatrix();
					GlStateManager.translate(yaw * - 2.222222f, -6.0f, 0.0f);
					//divisions
					GlStateManager.enableBlend();
					GlStateManager.pushMatrix();
					GlStateManager.translate(-0.5f, 1.0f, 0.0f);
					GlStateManager.scale(0.5f, 0.5f, 0.5f);
					for (int i = 0; i < 12; i++) {
						drawTexturedModalRect(i * 200, 0, 29, 0, 4, 20);
					}
					GlStateManager.popMatrix();
					// sides
					UtilYDE.FONT_HEADLINE.draw("W", -203, 0, 0xC0C8F0DC);
					UtilYDE.FONT_HEADLINE.draw("S", -2.5F, 0, 0xC0BEF0F0);
					UtilYDE.FONT_HEADLINE.draw("E", 197.5F, 0, 0xC0F0F0BE);
					UtilYDE.FONT_HEADLINE.draw("N", 397, 0, 0xC0F0DCBE);
					UtilYDE.FONT_HEADLINE.draw("W", 596, 0, 0xC0C8F0BE);
					UtilYDE.FONT_HEADLINE.draw("S", 797.5F, 0, 0xC0BEF0F0);
					GlStateManager.popMatrix();
				}
				// Arrow
				GlStateManager.pushMatrix();
				if (point != null) {
					IRayTraceRotate angles = Util.instance.getAngles3D(mc.player.posX, mc.player.posY + mc.player.getEyeHeight(), mc.player.posZ, point[0], point[1], point[2]);
					if (range == 1 || angles.getDistance() > range) {
						GlStateManager.pushMatrix();
						GlStateManager.enableBlend();
						GlStateManager.color((float)(taskColor >> 16 & 255) / 255.0F,
								(float)(taskColor >> 8 & 255) / 255.0F,
								(float)(taskColor & 255) / 255.0F,
								ValueUtil.correctFloat(-0.0175f * (float) (angles.getDistance() - range) + 1.175f, 0.3f, 1.0f));
						float rot = (yaw - (float) angles.getYaw()) % 360;
						if (rot > 180) { rot -= 360.0f; }
						if (rot < -180) { rot += 360.0f; }
						if (rot > 44.0f) {
							GlStateManager.translate(-99.0f, -3.5f, 0.0f);
							GlStateManager.scale(0.5f, 0.5f, 0.5f);
							drawTexturedModalRect(0, 0, 15, 34, 10, 14);
						}
						else if (rot < -42.9f) {
							GlStateManager.translate(95.0f, -3.5f, 0.0f);
							GlStateManager.scale(0.5f, 0.5f, 0.5f);
							drawTexturedModalRect(0, 0, 15, 20, 10, 14);
						}
						else {
							GlStateManager.translate(rot * - 2.222222f, -2.0f, 0.0f);
							GlStateManager.scale(0.5f, 0.5f, 0.5f);
							if (mc.player.posY < point[1] - range) {
								drawTexturedModalRect(-2, -6, 15, 0, 14, 10);
							} // up
							else if (mc.player.posY > point[1] + range) {
								drawTexturedModalRect(-2, 4, 15, 10, 14, 10);
							} // dow
							else {
								drawTexturedModalRect(0, 0, 15, 48, 10, 10);
							}
						}
						GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
						GlStateManager.popMatrix();
					} // direction
					else {
						long speed = 1500L;
						float a = 48.0f / (speed - (speed / 148.0f * 100.0f));
						float t = (System.currentTimeMillis() % (speed * 2L)) - speed;
						GlStateManager.pushMatrix();
						GlStateManager.enableBlend();
						GlStateManager.color((float)(taskColor >> 16 & 255) / 255.0F,
								(float)(taskColor >> 8 & 255) / 255.0F,
								(float)(taskColor & 255) / 255.0F,
								0.5f);
						int w;
						if (t < 0) {
							w = ValueUtil.correctInt((int) (a * (t + speed)), 0, 48);
							// left
							GlStateManager.pushMatrix();
							GlStateManager.translate(-a * t - 147.5f, 1.5f, 0.0f);
							GlStateManager.scale(1.0f, 0.5f, 1.0f);
							drawTexturedModalRect(0, 0, 81, 8, w, 8);
							GlStateManager.popMatrix();
							// right
							GlStateManager.pushMatrix();
							GlStateManager.translate(a * t + 147.5f, 1.5f, 0.0f);
							GlStateManager.scale(1.0f, 0.5f, 1.0f);
							drawTexturedModalRect(-w, 0, 81 - w, 8, w, 8);
							GlStateManager.popMatrix();
						} // down
						else {
							w = ValueUtil.correctInt((int) (-a * t + 148.0f), 0, 48);
							// left
							GlStateManager.pushMatrix();
							GlStateManager.translate(a * t - 147.5f, -5.5f, 0.0f);
							GlStateManager.scale(1.0f, 0.5f, 1.0f);
							drawTexturedModalRect(0, 0, 33, 0, w, 8);
							GlStateManager.popMatrix();
							// right
							GlStateManager.pushMatrix();
							GlStateManager.translate(-a * t + 99.5f, -5.5f, 0.0f);
							GlStateManager.scale(1.0f, 0.5f, 1.0f);
							drawTexturedModalRect(48 - w, 0, 129 - w, 0, w, 8);
							GlStateManager.popMatrix();
						} // up
						GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
						GlStateManager.popMatrix();
					} // found
				}
				GlStateManager.popMatrix();
				GL11.glDisable(GL11.GL_SCISSOR_TEST);
			}
			else {
				GlStateManager.rotate(-45.0f - compassData.incline, 1.0f, 0.0f, 0.0f);
				if (compassData.rot != 0.0f) { GlStateManager.rotate(compassData.rot, 0.0f, 1.0f, 0.0f); }
				// Body
				GlStateManager.pushMatrix();
				if (COMPASS_BODY == null) COMPASS_BODY = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
						Collections.singletonList("body"), GuiBasic.TEXTURES_COMPASS, false, 0, false);
				ModelBuffer.render(COMPASS_BODY);
				GlStateManager.popMatrix();
				// Dial
				if (compassData.isShowDial()) {
					GlStateManager.pushMatrix();
					GlStateManager.rotate(180.0f + mc.player.rotationYaw, 0.0f, 1.0f, 0.0f);
					if (COMPASS_DIAL == null) COMPASS_DIAL = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
							Collections.singletonList("dial"), GuiBasic.TEXTURES_COMPASS, false, 0, false);
					ModelBuffer.render(COMPASS_DIAL);
					GlStateManager.popMatrix();
				}
				if (point != null) {
					IRayTraceRotate angles = Util.instance.getAngles3D(mc.player.posX, mc.player.posY + mc.player.getEyeHeight(), mc.player.posZ, point[0], point[1], point[2]);
					if (range == 1 || angles.getDistance() > range) {
						// Arrow_0
						GlStateManager.pushMatrix();
						GlStateManager.rotate(yaw - (float) angles.getYaw(), 0.0f, 1.0f, 0.0f);
						if (COMPASS_ARROW_0 == null) COMPASS_ARROW_0 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
								Collections.singletonList("arrow_0"), GuiBasic.TEXTURES_COMPASS, false, 0, false);
						ModelBuffer.render(COMPASS_ARROW_0);
						GlStateManager.popMatrix();
						// Arrow_1 upper
						double yP;
						yP = -0.25d * (mc.player.posY - point[1]) / (double) range;
						GlStateManager.pushMatrix();
						if (yP >= -0.25d && yP <= 0.25d) {
							GlStateManager.translate(0.0d, yP, 0.0d);
						}
						else {
							if (yP > 0.25d) {
								GlStateManager.translate(0.0d, 0.275d, 0.0d);
							} else if (yP < -0.25d) {
								GlStateManager.translate(0.0d, -0.275d, 0.0d);
							}
							double t = System.currentTimeMillis() % 1000.0d;
							double f0 = t < 500.0d ? -0.025d + 0.05d * (t % 500.0d) / 500.0d
									: 0.025d - 0.05d * (t % 500.0d) / 500.0d;
							GlStateManager.translate(0.0d, f0, 0.0d);
						}
						if (COMPASS_ARROW_1 == null) COMPASS_ARROW_1 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
								Collections.singletonList("arrow_1"), GuiBasic.TEXTURES_COMPASS, false, 0, false);
						ModelBuffer.render(COMPASS_ARROW_1);
						GlStateManager.popMatrix();
						// Arrow_2
						GlStateManager.pushMatrix();
						if (yP > 0.25d) {
							if (COMPASS_ARROW_21 == null) COMPASS_ARROW_21 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
									Collections.singletonList("arrow_21"), GuiBasic.TEXTURES_COMPASS, false, 0, false);
							ModelBuffer.render(COMPASS_ARROW_21);
						}
						else if (yP < -0.25d) {
							if (COMPASS_ARROW_22 == null) COMPASS_ARROW_22 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
									Collections.singletonList("arrow_22"), GuiBasic.TEXTURES_COMPASS, false, 0, false);
							ModelBuffer.render(COMPASS_ARROW_22);
						}
						else {
							if (COMPASS_ARROW_20 == null) COMPASS_ARROW_20 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
									Collections.singletonList("arrow_20"), GuiBasic.TEXTURES_COMPASS, false, 0, false);
							ModelBuffer.render(COMPASS_ARROW_20);
						}
						GlStateManager.popMatrix();
					} // direction
					else {
						// Arrow_3
						GlStateManager.pushMatrix();
						float t = System.currentTimeMillis() % 4000L;
						float f0 = t < 2000.0f ? -0.00033f * t + 1.0f : 0.00033f * t - 0.30033f;
						GlStateManager.scale(f0, f0, f0);
						if (COMPASS_ARROW_3 == null) COMPASS_ARROW_3 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
								Collections.singletonList("arrow_3"), GuiBasic.TEXTURES_COMPASS, false, 0, false);
						ModelBuffer.render(COMPASS_ARROW_3);
						GlStateManager.popMatrix();
					} // found
				}
				if (taskType >= 0 && taskType <= EnumQuestTask.values().length) {
					GlStateManager.pushMatrix();
					if (!COMPASS_FASE.containsKey(taskType)) {
						Map<String, ResourceLocation> m = new HashMap<>();
						m.put("#material", new ResourceLocation(CustomNpcs.MODID, "util/compass"));
						m.put("#task", new ResourceLocation(CustomNpcs.MODID, "util/task_" + taskType));
						COMPASS_FASE.put(taskType, ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
										Collections.singletonList("fase"), m, false, 0, false));

					}
					ModelBuffer.render(COMPASS_FASE.get(taskType));
					GlStateManager.popMatrix();
				}
			}
			GlStateManager.popMatrix();
			GlStateManager.popMatrix();
			RenderHelper.disableStandardItemLighting();
			GlStateManager.disableLighting();
			GlStateManager.disableBlend();
			GlStateManager.enableDepth();
			GlStateManager.enableAlpha();
			GlStateManager.color(0.0f, 0.0f, 0.0f, 0.0f);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			mc.getTextureManager().bindTexture(Gui.ICONS);
		}
	}

	/** HUD: NBT Book Info Overlay */
	public void renderNbtBookOverlay(ScaledResolution sr) {
		mc = Minecraft.getMinecraft();
		sw = sr;
		if (mc.world == null || mc.player == null) return;
		if (!(mc.currentScreen == null || mc.currentScreen instanceof GuiChat || mc.currentScreen instanceof GuiLog)) return;
		if (!(mc.player.getHeldItemMainhand().getItem() instanceof ItemNbtBook ||
				mc.player.getHeldItemOffhand().getItem() instanceof ItemNbtBook)) return;

		PlayerData playerData = CustomNpcs.proxy.getPlayerData(mc.player);
		double distance = playerData.game.renderDistance;
		Vec3d vec3d = mc.player.getPositionEyes(1.0f);
		Vec3d vec3d1 = mc.player.getLook(1.0f);
		Vec3d vec3d2 = vec3d.addVector(vec3d1.x * distance, vec3d1.y * distance, vec3d1.z * distance);
		RayTraceResult result = mc.player.world.rayTraceBlocks(vec3d, vec3d2, false, false, true);

		Component rayName;
		Component rayTitle = Component.empty();
		double lH = mc.fontRenderer.FONT_HEIGHT + 1.0d;
		ItemStack st;

		if (result != null) {
			BlockPos blockPos = result.getBlockPos();
			Entity entity = Util.instance.getLookEntity(mc.player, distance, false);
			st = null;
			IBlockState state = null;
			double dist;
			Component rayPos = Component.empty();

			if (entity != null) {
				dist = Math.round(mc.player.getDistance(entity) * 10.0d) / 10.0d;
				ResourceLocation res = EntityList.getKey(entity);
				rayName = Component.empty()
						.append(Component.literal(" [" + res + "] ").withStyle(TextFormatting.GRAY))
						.append(Component.literal(entity.getName()).withStyle(TextFormatting.RESET));
				rayTitle = Component.literal(entity.getClass().getSimpleName()).withStyle(TextFormatting.YELLOW);
				rayPos = Component.empty()
						.append(Component.literal("[X:").withStyle(TextFormatting.AQUA))
						.append(Component.literal("" + (Math.round(entity.posX * 10.0d) / 10.0d)).withStyle(TextFormatting.GOLD))
						.append(Component.literal(", Y:").withStyle(TextFormatting.AQUA))
						.append(Component.literal("" + (Math.round(entity.posY * 10.0d) / 10.0d)).withStyle(TextFormatting.GOLD))
						.append(Component.literal(", Z:").withStyle(TextFormatting.AQUA))
						.append(Component.literal("" + (Math.round(entity.posZ * 10.0d) / 10.0d)).withStyle(TextFormatting.GOLD))
						.append(Component.literal("]").withStyle(TextFormatting.AQUA))
						.append(Component.literal(" " + dist).withStyle(TextFormatting.DARK_AQUA));
			} else {
				float f = (float) (mc.player.posX - blockPos.getX() + 0.5d);
				float f1 = (float) (mc.player.posY - blockPos.getY() + 0.5d);
				float f2 = (float) (mc.player.posZ - blockPos.getZ() + 0.5d);
				dist = Math.round(MathHelper.sqrt(f * f + f1 * f1 + f2 * f2) * 10.0d) / 10.0d;

				if (dist > playerData.game.renderDistance && !mc.player.getHeldItemOffhand().isEmpty()
						&& !(mc.player.getHeldItemOffhand().getItem() instanceof ItemNbtBook)) {
					st = mc.player.getHeldItemOffhand();
					rayName = Component.literal(st.getDisplayName());
				} else {
					state = mc.world.getBlockState(blockPos);
					if (dist > playerData.game.renderDistance) {
						result = mc.player.world.rayTraceBlocks(vec3d, vec3d2, true, false, true);
						if (result != null) {
							IBlockState tempState = mc.world.getBlockState(result.getBlockPos());
							if (!(tempState.getBlock() instanceof BlockAir)) state = tempState;
						}
					}
					rayName = Component.empty()
							.append(Component.literal(" [" + ForgeRegistries.BLOCKS.getKey(state.getBlock()) + "] ").withStyle(TextFormatting.GRAY))
							.append(Component.literal(state.getBlock().getLocalizedName()).withStyle(TextFormatting.RESET));

					String stateText = state.toString();
					if (stateText.contains("[")) {
						stateText = stateText.substring(stateText.indexOf("["));
					} else {
						stateText = "[]";
					}
					rayTitle = Component.empty()
							.append(Component.literal(state.getBlock().getClass().getSimpleName()).withStyle(TextFormatting.RESET))
							.append(Component.literal("; state: ").withStyle(TextFormatting.GRAY))
							.append(Component.literal(stateText).withStyle(TextFormatting.YELLOW));

					if (state.getBlock() instanceof ITileEntityProvider) {
						rayTitle.append(Component.literal("; ").withStyle(TextFormatting.GRAY))
								.append(Component.literal("hasTile").withStyle(TextFormatting.DARK_AQUA));
					}
					rayPos = Component.empty()
							.append(Component.literal(" [X:").withStyle(TextFormatting.GREEN))
							.append(Component.literal("" + blockPos.getX()).withStyle(TextFormatting.GOLD))
							.append(Component.literal(", Y:").withStyle(TextFormatting.GREEN))
							.append(Component.literal("" + blockPos.getY()).withStyle(TextFormatting.GOLD))
							.append(Component.literal(", Z:").withStyle(TextFormatting.GREEN))
							.append(Component.literal("" + blockPos.getZ()).withStyle(TextFormatting.GOLD))
							.append(Component.literal("]").withStyle(TextFormatting.GREEN))
							.append(Component.literal(" " + dist).withStyle(TextFormatting.DARK_AQUA));
				}
			}

			// Render entity preview
			GlStateManager.pushMatrix();
			if (entity != null) {
				GlStateManager.pushMatrix();
				GlStateManager.translate(8.0d + playerData.overlay.getWindowSize().getWidth() / 2.0d,
						playerData.overlay.getWindowSize().getHeight() - 45.0d - 3.5d * lH, -200.0d);
				renderEntityForBook(entity);
				GlStateManager.popMatrix();
			} else if (state != null) {
				st = new ItemStack(Item.getItemFromBlock(state.getBlock()), 1, state.getBlock().damageDropped(state));
			}

			// Render item icon
			if (st != null) {
				GlStateManager.pushMatrix();
				GlStateManager.translate(playerData.overlay.getWindowSize().getWidth() / 2.0d,
						playerData.overlay.getWindowSize().getHeight() - 45.0d - 4.5d * lH, 0.0d);
				RenderHelper.enableGUIStandardItemLighting();
				RenderItem itemRender = mc.getRenderItem();
				itemRender.renderItemAndEffectIntoGUI(st, 0, 0);
				itemRender.renderItemOverlays(mc.fontRenderer, st, 0, 0);
				RenderHelper.disableStandardItemLighting();
				GlStateManager.popMatrix();
			}

			// Text lines (смещение Y на -45 вместо -35 из 1.20.1)
			GlStateManager.translate(
					(playerData.overlay.getWindowSize().getWidth() - (double) mc.fontRenderer.getStringWidth(rayName.getFormattedText())) / 2.0d,
					playerData.overlay.getWindowSize().getHeight() - 45.0d - 3.0d * lH, 0.0d);
			drawString(mc.fontRenderer, rayName.getFormattedText(), 0, 0, 0xFFFFFF);
			GlStateManager.popMatrix();

			GlStateManager.pushMatrix();
			GlStateManager.translate(
					(playerData.overlay.getWindowSize().getWidth() - (double) mc.fontRenderer.getStringWidth(rayTitle.getFormattedText())) / 2.0d,
					playerData.overlay.getWindowSize().getHeight() - 45 - 2.0d * lH, 0.0d);
			drawString(mc.fontRenderer, rayTitle.getFormattedText(), 0, 0, 0xFFFFFF);
			GlStateManager.popMatrix();

			GlStateManager.pushMatrix();
			GlStateManager.translate(
					(playerData.overlay.getWindowSize().getWidth() - (double) mc.fontRenderer.getStringWidth(rayPos.getFormattedText())) / 2.0d,
					playerData.overlay.getWindowSize().getHeight() - 45 - lH, 0.0d);
			drawString(mc.fontRenderer, rayPos.getFormattedText(), 0, 0, 0xFFFFFF);
			GlStateManager.popMatrix();
		}
		// Offhand item when not looking at block
		else if (!mc.player.getHeldItemOffhand().isEmpty()) {
			st = mc.player.getHeldItemOffhand();
			rayName = Component.empty()
					.append(Component.literal(" [" + ForgeRegistries.ITEMS.getKey(st.getItem()) + "] ").withStyle(TextFormatting.GRAY))
					.append(st.getItem().getUnlocalizedName());
			rayTitle = Component.empty()
					.append(Component.literal(st.getItem().getClass().getSimpleName()).withStyle(TextFormatting.RESET))
					.append(Component.literal("; meta: ").withStyle(TextFormatting.GRAY))
					.append(Component.literal("" + st.getItemDamage()).withStyle(TextFormatting.YELLOW));
			if (st.hasTagCompound()) {
				rayTitle.append(Component.literal("; ").withStyle(TextFormatting.GRAY))
						.append(Component.literal("hasTags").withStyle(TextFormatting.DARK_AQUA));
			}

			GlStateManager.pushMatrix();
			GlStateManager.translate(
					(playerData.overlay.getWindowSize().getWidth() - (double) mc.fontRenderer.getStringWidth(rayName.getFormattedText())) / 2.0d,
					playerData.overlay.getWindowSize().getHeight() - 45.0d - 3.0d * lH, 0.0d);
			drawString(mc.fontRenderer, rayName.getFormattedText(), 0, 0, 0xFFFFFF);
			GlStateManager.popMatrix();

			GlStateManager.pushMatrix();
			GlStateManager.translate(
					(playerData.overlay.getWindowSize().getWidth() - (double) mc.fontRenderer.getStringWidth(rayTitle.getFormattedText())) / 2.0d,
					playerData.overlay.getWindowSize().getHeight() - 45 - 2.0d * lH, 0.0d);
			drawString(mc.fontRenderer, rayTitle.getFormattedText(), 0, 0, 0xFFFFFF);
			GlStateManager.popMatrix();
		}
	}

}
