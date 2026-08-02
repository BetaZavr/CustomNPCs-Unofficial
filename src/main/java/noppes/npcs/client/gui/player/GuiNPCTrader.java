package noppes.npcs.client.gui.player;

import java.util.*;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.handler.data.IDeal;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.containers.ContainerNPCTrader;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.IComponentGui;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextChangeListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

// Changed from Unofficial (BetaZavr)
@SideOnly(Side.CLIENT)
public class GuiNPCTrader extends GuiContainerNPCInterface<ContainerNPCTrader>
		implements IGuiData, ITextfieldListener, ITextChangeListener {

	public static final ResourceLocation BUTTONS = new ResourceLocation(CustomNpcs.MODID, "textures/gui/trade/buttons.png");
	public static final ResourceLocation HOVERS = new ResourceLocation(CustomNpcs.MODID, "textures/gui/trade/hovers.png");
	public static final ResourceLocation INV = new ResourceLocation(CustomNpcs.MODID, "textures/gui/trade/player_inventory.png");
	public static final ResourceLocation SCROLL = new ResourceLocation(CustomNpcs.MODID, "textures/gui/trade/scroll.png");
	public static final ResourceLocation ICONS = new ResourceLocation(CustomNpcs.MODID, "textures/gui/trade/sections.png");
	public static Marcet marcet;
	protected static String search = "";
	protected static boolean isIdSort = true;
	protected static int section = -1;
	protected static final Comparator<Deal> comparator = (t1, t2) -> {
		if (isIdSort) {
			Map<Integer, Integer> indexMap = new HashMap<>();
			int i = 0;
			for (IDeal iDeal : GuiNPCTrader.marcet.getDeals(GuiNPCTrader.section)) { indexMap.put(iDeal.getId(), i++); }
			return Integer.compare(indexMap.getOrDefault(t1.getId(), Integer.MAX_VALUE), indexMap.getOrDefault(t2.getId(), Integer.MAX_VALUE));
		}
		else { return t1.getName().compareToIgnoreCase(t2.getName()); }
	};

	protected final Map<Integer, Deal> data = new LinkedHashMap<>();
	protected DealMarkup selectDealData;

	protected List<Integer> canBuy = new ArrayList<>();
	protected List<Integer> canSell = new ArrayList<>();
	protected int count = 1;
	protected final ContainerNPCTrader menu;

	// display
	protected boolean wait = false;
	protected int invPosX;
	protected int invPosY;
	// scroll
	protected boolean isScrolled;
	protected int scrollWidth;
	protected int scrollHMax;
	protected int scrollBMax;
	protected int scrollHeight;
	protected int scrollBHeight;
	protected int scrollY;
	protected int scrollMaxY;
	// hovers
	protected List<Component> hovers = new ArrayList<>();
	protected boolean isHovered;
	protected int hoverHeightMax;
	protected int hoverHMax;
	protected int hoverBMax;
	protected int hoverHeight;
	protected int hoverBHeight;
	protected int hoverY;
	protected int hoverMaxY;
	// model rotate
	protected Map<String, ResourceLocation> materialTextures = new HashMap<>();
	protected ParameterizedModel CHEST_FULL;
	protected float rotateX = 0.0f;
	protected float rotateZ = 0.0f;

	// Tabs
	protected int ceilHeight = 0;
	protected int ceilList = -1;

	public GuiNPCTrader(ContainerNPCTrader container) {
		super(NoppesUtilServer.getEditingNpc(Minecraft.getMinecraft().player), container, Component.empty());
		drawDefaultBackground = false;
		closeOnEsc = true;
		hoverIsGame = true;

		ScaledResolution sw = new ScaledResolution(mc);
		xSize = (int) sw.getScaledWidth_double();
		ySize = (int) sw.getScaledHeight_double();
		marcet = container.marcet;
		menu = container;
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		if (button instanceof TradeButtonBiDirectional) {
			count = button.getValue() + 1;
			initGui();
			return;
		}
		if (button instanceof SectionButton && button.id >= 20) {
			int s = button.id - 20 + ceilList * ceilHeight;
			if (s != section) {
				section = s;
				selectDealData = null;
				scrollY = 0;
				hoverY = 0;
				rotateX = 0.0f;
				rotateZ = 0.0f;
				count = 1;
				initGui();
			}
			return;
		}
		if (button instanceof TradeButton) {
			TradeButton tradeB = (TradeButton) button;
			if (selectDealData == null || selectDealData.deal == null || selectDealData.deal.getId() != tradeB.deal.getId()) {
				selectDealData = tradeB.dm;
				hoverY = 0;
				rotateX = 0.0f;
				rotateZ = 0.0f;
				count = 1;
				initGui();
			}
			return;
		} // select deal
		switch (button.id) {
			case 0: Packets.sendServer(new SPacketTraderMarketBuy(marcet.getId(), selectDealData.deal.getId(), npc == null ? -1 : npc.getEntityId(), count)); break; // buy
			case 1: Packets.sendServer(new SPacketTraderMarketSell(marcet.getId(), selectDealData.deal.getId(), npc == null ? -1 : npc.getEntityId(), count)); break; // Sell
			case 2: Packets.sendServer(new SPacketTraderMarketReset(marcet.getId())); break; // Reset
			case 3: {
				if (ceilList <= 0) { return; }
				ceilList--;
				initGui();
				return;
			} // up
			case 4: {
				if (ceilList >= Math.floor((double) marcet.sections.size() / 5.0d)) { return; }
				ceilList++;
				initGui();
				return;
			} // down
			case 11: {
				isIdSort = ((GuiCheckBoxNop) button).selected();
				initGui();
				return;
			} // sort type
		}
		wait = true;
		initGui();
	}

	@Override
	public void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);

		GlStateManager.enableBlend();
		GlStateManager.translate(0.0f, 0.0f, -1.0f);
		int w;
		int h = (250 - scrollHeight) / 2;
		Component text;
		// update / money pos
		mc.getTextureManager().bindTexture(INV);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		drawTexturedModalRect(invPosX, 0, 0, 142, 178, 24);
		// player inventory:
		drawTexturedModalRect(invPosX, invPosY, 0, 0, 178, 118);
		// Scroll
		int s = 0;
		int v;
		mc.getTextureManager().bindTexture(SCROLL);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		for (int i = 0; i < scrollHMax; i++) {
			if (i == 0) { v = 0; } else { v = 3 + h; }
			drawTexturedModalRect(0, i * scrollHeight, 0, v, scrollWidth, scrollHeight); // left
			drawTexturedModalRect(scrollWidth, i * scrollHeight, 214 - scrollWidth, v, scrollWidth, scrollHeight); // right
			s += scrollHeight;
		}
		// end
		h = ySize - s;
		drawTexturedModalRect(0, ySize - h, 0, 256 - h, scrollWidth, h); // left
		drawTexturedModalRect(scrollWidth, ySize - h, 214 - scrollWidth, 256 - h, scrollWidth, h); // right
		// bar
		if (scrollMaxY > 0) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(scrollWidth * 2 - 14, 4.0f, 0.0f);
			GlStateManager.scale(0.5f, 0.5f, 1.0f);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			drawTexturedModalRect(0, 0, 236, 0, 20, 20);
			drawTexturedModalRect(0, (ySize - 61) * 2, 236, 236, 20, 20);
			GlStateManager.scale(1.0f, 2.0f, 1.0f);
			s = 10;
			h = (216 - scrollBHeight) / 2;
			for (int i = 0; i < scrollBMax; i++) {
				if (i == 0) { v = 20; } else { v = h; }
				drawTexturedModalRect(0, 10 + i * scrollBHeight, 236, v, 20, scrollBHeight); // bar
				s += scrollBHeight;
			}
			h = 28;
			drawTexturedModalRect(0, s, 236, 236 - scrollHeight, 20, scrollHeight - h); // bar
			GlStateManager.popMatrix();
		}
		mc.getTextureManager().bindTexture(BUTTONS);
		// place of sale
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		drawTexturedModalRect(4, ySize - 46, 0, 214, scrollWidth - 4, 42);
		drawTexturedModalRect(scrollWidth, ySize - 46, 260 - scrollWidth, 214, scrollWidth - 4, 42);
		// hover deal
		if (selectDealData != null && selectDealData.deal != null) {
			int x = xSize - 155;
			int y = 24;
			mc.getTextureManager().bindTexture(HOVERS);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			drawTexturedModalRect(x, y, 0, 0, 132, 50);
			if (hoverHeightMax >= 24) {
				y += 50;
				s = 0;
				for (int i = 0; i < hoverHMax; i++) {
					if (i == 0) { v = 50; } else { v = 53; }
					drawTexturedModalRect(x, y + i * hoverHeight, 0, v, 132, hoverHeight);
					s += hoverHeight;
				}
				// end
				h = hoverHeightMax - s;
				drawTexturedModalRect(x, invPosY - 24 - h, 0, 256 - h, 132, h);
				// bar
				if (hoverMaxY > 0) {
					GlStateManager.pushMatrix();
					GlStateManager.translate(x + 120.0f, y + 2, 0.0f);
					GlStateManager.scale(0.5f, 0.5f, 1.0f);
					mc.getTextureManager().bindTexture(SCROLL);
					GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
					drawTexturedModalRect(0, 0, 236, 0, 20, 20);
					drawTexturedModalRect(0, (hoverHeightMax - 14) * 2, 236, 236, 20, 20);
					if (hoverHeightMax > 24) {
						GlStateManager.scale(1.0f, 2.0f, 1.0f);
						s = 10;
						h = (216 - hoverBHeight) / 2;
						for (int i = 0; i < hoverBMax; i++) {
							if (i == 0) { v = 20; } else { v = h; }
							drawTexturedModalRect(0, 10 + i * hoverBHeight, 236, v, 20, hoverBHeight); // bar
							s += hoverBHeight;
						}
						h = hoverHeightMax % 2 != 0 ? 9 : 10;
						drawTexturedModalRect(0, s, 236, 236 - hoverHeight, 20, hoverHeight - h); // bar
					}
					GlStateManager.popMatrix();
				}
			}
		}
		PlayerData playerData = CustomNpcs.proxy.getPlayerData(player);
		// Market level
		if (marcet.showXP) {
			mc.getTextureManager().bindTexture(INV);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			drawTexturedModalRect(xSize - 100, invPosY - 24, 0, 118, 100, 24);
			MarkupData md = playerData.game.getMarkupData(marcet.getId());
			MarkupData mm = marcet.markup.get(md.level);
			if (md.xp > 0) {
				double mXP = mm.xp;
				if (md.xp >= mXP) { s = 96; }
				else { s = (int) (96.0d * md.xp / mXP); }
				GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
				drawTexturedModalRect(xSize - 2 - s, ySize - 142, 198 - s, 118, s, 24);
			}
			String lv = "enchantment.level." + (md.level + 1);
			if (!Component.translatable(lv).getFormattedText().equals(lv)) { lv = Component.translatable(lv).getFormattedText(); }
			else { lv = "" + (md.level + 1); }
			drawString(fontRenderer, lv, xSize - 6 - fontRenderer.getStringWidth(lv), ySize - 131, CustomNpcs.MainColor.getRGB());
			if (isMouseHover(mouseX, mouseY, xSize - 100, ySize - 142, 100, 24)) {
				setHoverText(Component.translatable("market.hover.you.level", "" + (md.level + 1),
						"" + Math.min(md.xp, mm.xp), "" + mm.xp,
						(mm.buy <= 0.0f ? TextFormatting.GREEN: TextFormatting.RED) + "" + Math.round(mm.buy * 100.0f),
						(mm.sell < 0.0f ? TextFormatting.RED: TextFormatting.GREEN) + "" + Math.round(mm.sell * 100.0f)));
			} // hover market xp
		}

		// name
		if (marcet.getName().isEmpty()) { text = Component.translatable("role.trader"); }
		else { text = Component.translatable(marcet.getName()); }
		w = ClientProxy.Font.width(text) / 2;
		ClientProxy.Font.draw(text, scrollWidth - w + 10, 2, CustomNpcs.MainColor.getRGB());
		// update
		if (marcet.updateTime > 0) {
			TextFormatting color = TextFormatting.RESET;
			if (marcet.nextTime <= 60000 && marcet.nextTime % 1000 < 500) { color = TextFormatting.GOLD; }
			else if (marcet.nextTime <= 10000) { color = marcet.nextTime % 1000 < 500 ? TextFormatting.GOLD : TextFormatting.RED; }
			text = Component.translatable("market.uptime",
					color + Util.instance.ticksToElapsedTime(marcet.nextTime / 50, false, false, false));
			w = ClientProxy.Font.width(text);
			ClientProxy.Font.draw(text, invPosX + 3, 2, CustomNpcs.MainColor.getRGB());
			if (marcet.nextTime <= 0) { Packets.sendServerDelayed(new SPacketMarketTime(marcet.getId()), this, 2500); }
			if (isMouseHover(mouseX, mouseY, invPosX, 0, w, 24)) {
				setHoverText("market.hover.update");
			} // hover update time
		}
		// marcet money
		text = Component.literal(Util.instance.getTextReducedNumber(marcet.money, true, true, false) + CustomNpcs.displayCurrencies);
		w = ClientProxy.Font.width(text);
		ClientProxy.Font.draw(text, xSize - w - 15, 2, CustomNpcs.MainColor.getRGB());
		if (isMouseHover(mouseX, mouseY, xSize - w - 14, 0, w, 24)) {
			setHoverText(Component.translatable("market.hover.currency.1", marcet.money, CustomNpcs.displayCurrencies));
		}
		GlStateManager.pushMatrix();
		GlStateManager.translate(xSize - 15, 0, 1.0f);
		GlStateManager.scale(0.0625f, 0.0625f, 0.0625f);
		mc.getTextureManager().bindTexture(GuiNPCInterface.MONEY);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		drawTexturedModalRect(0, 0, 0, 0, 256, 256);
		GlStateManager.popMatrix();
		// money
		int x = invPosX + 4;
		int y = invPosY + 9;
		text = Component.translatable("questlog.rewardmoney", playerData.game.getTextMoney(), CustomNpcs.displayCurrencies);
		ClientProxy.Font.draw(text, x, y, CustomNpcs.MainColor.getRGB());
		w = ClientProxy.Font.width(text);
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + w, y - 2.0f, 1.0f);
		GlStateManager.scale(0.0625f, 0.0625f, 0.0625f);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		mc.getTextureManager().bindTexture(GuiNPCInterface.MONEY);
		drawTexturedModalRect(0, 0, 0, 0, 256, 256);
		GlStateManager.popMatrix();
		if (isMouseHover(mouseX, mouseY, x, y, w + 16, 16)) {
			setHoverText(Component.translatable("inventory.hover.currency").append(" " + playerData.game.getMoney()));
		} // hover money
		// donat
		text = Component.translatable("questlog.rewarddonat", playerData.game.getTextDonat(), CustomNpcs.displayDonation);
		w = ClientProxy.Font.width(text);
		x = xSize - 18 - w;
		ClientProxy.Font.draw(text, x, y, CustomNpcs.MainColor.getRGB());
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + w, y - 2.0f, 1.0f);
		GlStateManager.scale(0.0625f, 0.0625f, 0.0625f);
		mc.getTextureManager().bindTexture(GuiNPCInterface.DONAT);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		drawTexturedModalRect(0, 0, 0, 0, 256, 256);
		GlStateManager.popMatrix();
		if (isMouseHover(mouseX, mouseY, x, y, w + 16, 16)) {
			setHoverText(Component.translatable("inventory.hover.donat").append(" " + playerData.game.getDonat()));
		} // hover donat
		// search icon
		GlStateManager.pushMatrix();
		GlStateManager.translate(5.0f, ySize - 25.0f, 0.0f);
		GlStateManager.scale(0.833333f, 0.833333f, 0.833333f);
		mc.getTextureManager().bindTexture(ICONS);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		drawTexturedModalRect(0, 0, 0, 216, 24, 24);
		GlStateManager.popMatrix();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (marcet == null) { onClose(); return; }
		GlStateManager.enableBlend();
		if (!hovers.isEmpty()) {
			int i = 0;
			int x = xSize - 152;
			int y;
			GlStateManager.pushMatrix();

			GL11.glEnable(GL11.GL_SCISSOR_TEST);
			int c = xSize < mc.displayWidth ? (int) Math.round((double) mc.displayWidth / (double) xSize) : 1;
			GL11.glScissor(x * c, (ySize - 72 - hoverHeightMax) * c, 127 * c, (hoverHeightMax - 4) * c);
			for (Component hover : hovers) {
				y = 77 + hoverY + i * (fontRenderer.FONT_HEIGHT + 1);
				if (y >= 77 - fontRenderer.FONT_HEIGHT && y < 71 + hoverHeightMax) {
					drawString(fontRenderer, hover.getFormattedText(), x, y, CustomNpcs.MainColor.getRGB());
				}
				if (y >= 71 + hoverHeightMax) { break; }
				i++;
			}
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
			GlStateManager.popMatrix();
		}
		if (scrollMaxY != 0) {
			float f0 = (float) -scrollY / (float) scrollMaxY * (float) (ySize - 81);
			GlStateManager.pushMatrix();
			GlStateManager.enableAlpha();
			GlStateManager.enableBlend();
			GlStateManager.translate(scrollWidth * 2.0f - 14.5f, 4.0f + f0, 0.0f);
			GlStateManager.scale(0.5f, 0.5f, 0.5f);
			mc.getTextureManager().bindTexture(SCROLL);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			drawTexturedModalRect(0, 0, 214, 0, 22, 60);
			GlStateManager.popMatrix();
		}
		if (hoverMaxY != 0) {
			//isHovered = isMouseHover(mouseX, mouseY, xSize - 35, 76, 10, hoverHeightMax - 4);
			float f0 = (float) -hoverY / (float) hoverMaxY * (float) (hoverHeightMax - 20);
			GlStateManager.pushMatrix();
			GlStateManager.enableAlpha();
			GlStateManager.enableBlend();
			GlStateManager.translate(xSize - 35.0f, 76.0f + f0, 0.0f);
			GlStateManager.scale(0.5f, 0.5f, 0.5f);
			mc.getTextureManager().bindTexture(SCROLL);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			drawTexturedModalRect(0, 0, 214, 0, 22, 16);
			drawTexturedModalRect(0, 16, 214, 44, 22, 16);
			GlStateManager.popMatrix();
		}
		if (selectDealData != null && selectDealData.deal != null) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(xSize - 89.0f, 49.0f, 50.0f);
			if (rotateX == 0 && rotateZ == 0 && mc.world != null) {
				GlStateManager.rotate(-30.0f, 1.0f, 0.0f, 0.0f);
				GlStateManager.rotate((float) (System.currentTimeMillis() % 36000) / -20.0f, 0.0f, 1.0f, 0.0f);
			}
			else {
				GlStateManager.rotate(-30.0f + rotateX, 1.0f, 0.0f, 0.0f);
				GlStateManager.rotate(rotateZ, 0.0f, 0.0f, 1.0f);
			}
			if (selectDealData.deal.isCase()) {
				GlStateManager.translate(-16.0f, -8.0f, 0.0f);
				GlStateManager.scale(32.0f, -32.0f, 32.0f);
				ModelBuffer.render(CHEST_FULL);
			}
			else {
				if (!selectDealData.deal.getProduct().isEmpty()) {
					ItemStack stack = selectDealData.deal.getProduct().getMCItemStack();
					GlStateManager.scale(32.0f, -32.0f, 32.0f);
					mc.getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
				}
			}
			GlStateManager.popMatrix();
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void initGui() {
		// window
		ScaledResolution sw = new ScaledResolution(mc);
		if (xSize != (int) sw.getScaledWidth_double() || ySize != (int) sw.getScaledHeight_double()) {
			scrollY = 0;
			hoverY = 0;
		}
		xSize = (int) sw.getScaledWidth_double();
		ySize = (int) sw.getScaledHeight_double();
		menu.reset(xSize, ySize);
		boolean focus = getTextField(0) != null && getTextField(0).isFocused();
		super.initGui();
		invPosX = xSize - 178;
		invPosY = ySize - 118;
		scrollWidth = ValueUtil.correctInt(214, 0, xSize - 202) / 2;
		if (ySize <= 512) {
			scrollHMax = 1;
			scrollBMax = 1;
			scrollHeight = ySize / 2;
			scrollBHeight = (ySize - 85) / 2;
		}
		else {
			scrollHMax = (int) Math.ceil(((float) ySize - 10.0f) / 216.0f);
			scrollHeight = (int) Math.ceil(((float) ySize - 6.0f) / (float) scrollHMax);
			scrollHMax--;
			scrollBMax = (int) Math.ceil(((float) ySize - 85.0f) / 200.0f);
			scrollBHeight = (int) Math.ceil(((float) ySize - 85) / (float) scrollHMax);
			scrollBMax--;
		}
		hoverHeightMax = invPosY - 98;
		if (hoverHeightMax <= 206) {
			hoverHMax = 1;
			hoverBMax = 1;
			hoverHeight = (hoverHeightMax - 4) / 2;
			hoverBHeight = (hoverHeightMax - 24) / 2;
		}
		else {
			hoverHMax = (int) Math.ceil(((float) hoverHeightMax - 4.0f) / 216.0f);
			hoverHeight = (int) Math.ceil(((float) hoverHeightMax - 4.0f) / (float) hoverHMax);
			hoverHMax--;
			hoverBMax = (int) Math.ceil(((float) hoverHeightMax - 24.0f) / 200.0f);
			hoverBHeight = (int) Math.ceil(((float) hoverHeightMax - 24.0f) / (float) hoverHMax);
			hoverBMax--;
		}
		ceilHeight = (int) Math.floor(((float) ySize - 36.0f) / 24.0f);
		// gm buttons
		addButton(2, invPosX, invPosY - 22, "remote.reset")
				.setSize(76, 20)
				.setIsVisible(player.isCreative())
				.setHoverTexts("market.hover.reset");
		// section tabs
		SectionButton tab;
		if (ceilList < 0) {
			ceilList = 0;
			section = 0;
		}
		if (marcet.sections.size() > 1) {
			int offsetY = 4;
			if (marcet.sections.size() > ceilHeight) {
				if (ceilList > 0 && section != ceilList) {
					add(new SectionButton(this, 3, null, scrollWidth * 2 + 3, ySize - 16));
				} // down | next
				if (ceilList < Math.floor((double) marcet.sections.size() / (double) ceilHeight)) {
					add(new SectionButton(this, 4, null, scrollWidth * 2 + 3, 7));
				} // up | back
				offsetY += 14;
			}
			int id;
			for (int i = 0; i < ceilHeight && (i + ceilList * ceilHeight) < marcet.sections.size(); i++) {
				id = i + ceilList * ceilHeight;
				tab = new SectionButton(this, 20 + i, marcet.sections.get(id), scrollWidth * 2 + 9, offsetY + i * 24);
				tab.setHoverTexts(Component.empty()
						.append(Component.translatable("market.hover.section").withStyle(TextFormatting.GRAY))
						.append("<br>").append(marcet.sections.get(id).getName()));
				add(tab);
				if (i + ceilList * ceilHeight == section) { tab.active = true; }
			}
		}
		// section deals
		int level = CustomNpcs.proxy.getPlayerData(player).game.getMarcetLevel(marcet.getId());
		List<Deal> dealInTrade = new ArrayList<>();
		List<Deal> caseInTrade = new ArrayList<>();
		List<Deal> dealNotTrade = new ArrayList<>();
		List<Deal> caseNotTrade = new ArrayList<>();
		MarcetController mData = MarcetController.getInstance();
		MarcetSection ms = marcet.sections.get(section);
		String s = search.toLowerCase();
		if (ms != null && !ms.deals.isEmpty()) {
			for (Deal deal : ms.deals) {
				if (!s.isEmpty() && !deal.getName().toLowerCase().contains(s)) { continue; }
				if (deal.getMaxCount() != 0 && deal.getAmount() == 0) {
					if (deal.isCase()) { caseNotTrade.add(deal); }
					else { dealNotTrade.add(deal); }
				}
				else {
					if (deal.isCase()) { caseInTrade.add(deal); }
					else { dealInTrade.add(deal); }
				}
			}
		}
		dealInTrade.sort(comparator);
		caseInTrade.sort(comparator);
		dealNotTrade.sort(comparator);
		caseNotTrade.sort(comparator);
		data.clear();
		for (Deal deal : caseInTrade) { data.put(deal.getId(), deal); }
		for (Deal deal : caseNotTrade) { data.put(deal.getId(), deal); }
		for (Deal deal : dealInTrade) { data.put(deal.getId(), deal); }
		for (Deal deal : dealNotTrade) { data.put(deal.getId(), deal); }
		if (data.isEmpty()) { scrollMaxY = 0; }
		else { scrollMaxY = ValueUtil.correctInt(data.size() * 28 - ySize + 62, 0, Integer.MAX_VALUE); }
		int i = 0;
		for (Deal deal : data.values()) {
			add(new TradeButton(this, deal, level, 5, 15 + i * 28, (scrollWidth * 2) - (scrollMaxY == 0 ? 9 : 22), dealInTrade.contains(deal) || caseInTrade.contains(deal)));
			i++;
			if ((selectDealData == null || selectDealData.deal == null) && (player.isCreative() || deal.getMaxCount() == 0 || deal.getAmount() > 0)) { selectDealData = mData.getBuyData(marcet, deal, level, count); }
		}
		if (selectDealData != null && selectDealData.deal != null) {
			selectDealData = mData.getBuyData(marcet, selectDealData.deal, level, count);
			boolean found = false;
			for (Deal deal : data.values()) {
				if (deal.getId() == selectDealData.deal.getId()) {
					found = true;
					break;
				}
			}
			if (found) { selectDealData.check(player.inventory.mainInventory); }
			else {
				selectDealData = null;
				scrollY = 0;
				hoverY = 0;
				rotateX = 0.0f;
				rotateZ = 0.0f;
				return;
			}
			hovers.clear();
			List<Component> temp = new ArrayList<>();
			if (selectDealData.deal.isCase()) {
				materialTextures.put("minecraft:entity/chest/christmas", selectDealData.deal.getCaseTexture());
				if (selectDealData.deal.showInCase() || player.isCreative())
				{ selectDealData.deal.putHoverCaseItems(temp, minecraft.gameSettings.advancedItemTooltips ? TooltipFlags.ADVANCED : TooltipFlags.NORMAL); }
				CHEST_FULL = ModelBuffer.getParameterizedModel(selectDealData.deal.getCaseObjModel(), null, materialTextures, true, 0, false);
			}
			else if (!selectDealData.deal.getProduct().isEmpty()) {
				for (String line : selectDealData.deal.getProduct().getMCItemStack().getTooltip(player, minecraft.gameSettings.advancedItemTooltips ? TooltipFlags.ADVANCED : TooltipFlags.NORMAL)) {
					temp.add(new Component(line, true));
				}
			}
			if (!temp.isEmpty()) {
				String lastColor;
				int w = 116;
				for (Component cpt : temp) {
					String line = cpt.getFormattedText();
					if (minecraft.fontRenderer.getStringWidth(line) < w) { hovers.add(cpt); }
					else {
						lastColor = "";
						StringBuilder l = new StringBuilder();
						for (int j = 0; j < line.length(); j++) {
							char c = line.charAt(j);
							try {
								if ((int) c == 167) { lastColor = c + "" + line.charAt(j + 1); }
							}
							catch (Exception ignored) { }
							if (minecraft.fontRenderer.getStringWidth(l.toString() + c) > w) {
								hovers.add(Component.literal(l.toString()));
								l = new StringBuilder(lastColor + c);
								lastColor = "";
							}
							else { l.append(c); }
						}
						if (!l.toString().isEmpty()) { hovers.add(Component.literal(l.toString())); }
					}
				}
			}
			if (hovers.isEmpty()) { hoverMaxY = 0; }
			else { hoverMaxY = ValueUtil.correctInt(hovers.size() * (minecraft.fontRenderer.FONT_HEIGHT + 1) - hoverHeightMax + 4, 0, Integer.MAX_VALUE); }
		}
		// buy
		int x = scrollWidth;
		int y = ySize - 45;
		boolean enableBuy = selectDealData != null && selectDealData.deal != null && selectDealData.deal.getType() != 1;
		GuiButtonNop buyButton = addButton(0, x, y, Component.literal("   ").append(Component.translatable("gui.buy")))
				.setSize(scrollWidth - 5, 20)
				.setTexture(BUTTONS)
				.setUV(0, 144, 128, 20)
				.setIsEnabled(enableBuy);
		buyButton.isSimple = true;
		canBuy.clear();
		if (enableBuy) {
			if (wait || selectDealData.deal.getType() == 1) { canBuy.add(1); }
			if (selectDealData.deal.getMaxCount() != 0 && selectDealData.deal.getAmount() <= 0) { canBuy.add(6); }
			if (!selectDealData.deal.availability.isAvailable(player)) { canBuy.add(2); }
			PlayerData pd = CustomNpcs.proxy.getPlayerData(player);
			if (selectDealData.buyMoney > 0 && pd.game.getMoney() < selectDealData.buyMoney) { canBuy.add(3); }
			if (selectDealData.buyDonat > 0 && pd.game.getDonat() < selectDealData.buyDonat) { canBuy.add(7); }
			if (!Util.instance.canRemoveItems(player.inventory.mainInventory, selectDealData.buyItems, selectDealData.ignoreDamage, selectDealData.ignoreNBT)) { canBuy.add(4); }
			if (!selectDealData.deal.isCase() && !Util.instance.canAddItemAfterRemoveItems(player.inventory.mainInventory, selectDealData.main, selectDealData.buyItems, selectDealData.ignoreDamage, selectDealData.ignoreNBT)) { canBuy.add(5); }
			Map<ItemStack, Integer> mainItem = new LinkedHashMap<>();
			mainItem.put(selectDealData.main, selectDealData.count);
			if (marcet.isLimited && !selectDealData.deal.isCase() && !Util.instance.canRemoveItems(marcet.inventory, mainItem, selectDealData.ignoreDamage, selectDealData.ignoreNBT)) { canBuy.add(8); }
			if (buyButton.isActive()) {
				if (!player.isCreative()) { buyButton.setIsEnabled(canBuy.isEmpty()); }
				if (!canBuy.isEmpty()) { buyButton.layerColor = 0xFF800000; }
			}
		}
		// sell
		boolean enableSell = selectDealData != null && selectDealData.deal != null  && selectDealData.deal.getType() != 0;
		GuiButtonNop sellButton = addButton(1, x, y + 20, Component.translatable("gui.sell").append("   "))
				.setSize(scrollWidth - 5, 20)
				.setTexture(BUTTONS)
				.setUV(128, 144, 128, 20)
				.setIsEnabled(enableSell);
		sellButton.isSimple = true;
		canSell.clear();
		if (enableSell) {
			if (wait) { canSell.add(1); }
			if (!selectDealData.deal.availability.isAvailable(player)) { canSell.add(2); }
			Map<ItemStack, Integer> mainItem = new HashMap<>();
			mainItem.put(selectDealData.main, selectDealData.count);
			if (!selectDealData.main.isEmpty()  && !Util.instance.canRemoveItems(player.inventory.mainInventory, mainItem,  selectDealData.ignoreDamage, selectDealData.ignoreNBT)) { canSell.add(3); }
			if (marcet.isLimited) {
				if (selectDealData.sellMoney > marcet.money) { canSell.add(4); }
				if (!selectDealData.sellItems.isEmpty() && !Util.instance.canRemoveItems(marcet.inventory,  selectDealData.sellItems, selectDealData.ignoreDamage, selectDealData.ignoreNBT)) { canSell.add(5); }
			}
			if (selectDealData.deal.getType() == 1) { canSell.add(6); }
			if (sellButton.isActive()) {
				if (!player.isCreative()) { sellButton.setIsEnabled(canSell.isEmpty()); }
				if (!canSell.isEmpty()) { sellButton.layerColor = 0xFF800000; }
			}
		}
		// prise buttons
		if (selectDealData != null && selectDealData.deal != null) {
			List<Component> hoverBuy = new ArrayList<>();
			List<Component> hoverSell = new ArrayList<>();
			{
				if (!canBuy.isEmpty()) {
					for (int id : canBuy) { hoverBuy.add(Component.translatable("market.hover.notbuy." + id)); }
				}
				if (!canSell.isEmpty()) {
					for (int id : canSell) { hoverSell.add(Component.translatable("market.hover.notsell." + id)); }
				}
				if (!canBuy.isEmpty() || selectDealData.deal.getAmount() <= 0) { hoverBuy.add(Component.translatable("gui.allowed")); }
				if (!canSell.isEmpty()) { hoverSell.add(Component.translatable("gui.allowed")); }
				// buy hover info
				if (!selectDealData.buyItems.isEmpty()) {
					hoverBuy.add(Component.translatable("market.hover.item.buy"));
					for (ItemStack curr : selectDealData.buyItems.keySet()) {
						hoverBuy.add(Component.literal(curr.getDisplayName())
								.append(Component.literal(" x").withStyle(TextFormatting.GRAY)
										.append(Component.literal(selectDealData.buyItems.get(curr) + " ").withStyle(TextFormatting.GOLD))));
					}
				}
				if (selectDealData.buyMoney > 0) { hoverBuy.add(Component.translatable("market.hover.currency.buy", selectDealData.buyMoney, CustomNpcs.displayCurrencies)); }
				if (selectDealData.buyDonat > 0) { hoverBuy.add(Component.translatable("market.hover.donat.buy", selectDealData.buyDonat, CustomNpcs.displayDonation)); }
				// sell hover info
				if (!selectDealData.sellItems.isEmpty()) {
					hoverSell.add(Component.translatable("market.hover.item.sell"));
					for (ItemStack curr : selectDealData.sellItems.keySet()) {
						hoverSell.add(Component.literal(curr.getDisplayName())
								.append(Component.literal(" x").withStyle(TextFormatting.GRAY)
										.append(Component.literal(selectDealData.sellItems.get(curr) + " ").withStyle(TextFormatting.GOLD))));
					}
				}
				if (selectDealData.sellMoney > 0) { hoverSell.add(Component.translatable("market.hover.currency.sell", selectDealData.sellMoney, CustomNpcs.displayCurrencies)); }
			}
			if (selectDealData.deal.getType() != 1) { buyButton.setHoverTexts(hoverBuy); }
			if (selectDealData.deal.getType() != 0) { sellButton.setHoverTexts(hoverSell); }
		}
		add(new TradeButtonBiDirectional(this, 6, y, scrollWidth - 5));
		addCheckBox(11, 3, 3, "type.id", "N", isIdSort)
				.setSize(26, 12)
				.setHoverTexts(Component.translatable("hover.sort",
						Component.translatable("market.deals").getFormattedText(),
						Component.translatable(isIdSort ? "type.id" : "gui.name")));
		add(new MarcetTextField(this, 28, ySize - 19, scrollWidth - 31)
				.setHoverTexts("market.hover.is.search"));
		getTextField(0).setIsFocused(focus);
	}

	@Override
	public boolean keyPressed(char typedChar, int keyCode) {
		if (!hasSubGui()) {
			if (keyCode == Keyboard.KEY_UP || keyCode == mc.gameSettings.keyBindForward.getKeyCode()) {
				if (!hovers.isEmpty() && isMouseHover(wrapper.mouseX, wrapper.mouseY, xSize - 153, 76, 128, hoverHeightMax - 4)) {
					hoverY = ValueUtil.correctInt(hoverY + fontRenderer.FONT_HEIGHT + 1, -hoverMaxY, 0);
					return true;
				} else {
					scrollY = ValueUtil.correctInt(scrollY + 28, -scrollMaxY, 0);
				}
			} else if (keyCode == Keyboard.KEY_DOWN || keyCode == mc.gameSettings.keyBindBack.getKeyCode()) {
				if (!hovers.isEmpty() && isMouseHover(wrapper.mouseX, wrapper.mouseY, xSize - 153, 76, 128, hoverHeightMax - 4)) {
					hoverY = ValueUtil.correctInt(hoverY - fontRenderer.FONT_HEIGHT - 1, -hoverMaxY, 0);
					return true;
				} else {
					scrollY = ValueUtil.correctInt(scrollY - 28, -scrollMaxY, 0);
				}
			}
		}
		return super.keyPressed(typedChar, keyCode);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrolled) {
		if (isMouseHover(mouseX, mouseY, 3, 3, scrollWidth * 2 - 6, ySize - 50)) {
			scrollY = ValueUtil.correctInt(scrollY + (int) (scrolled * 28.0d), -scrollMaxY, 0);
		}
		else if (isMouseHover(mouseX, mouseY, xSize - 153, 76, 128, hoverHeightMax - 4)) {
			hoverY = ValueUtil.correctInt(hoverY + (int) (scrolled * ((double) minecraft.fontRenderer.FONT_HEIGHT + 1.0d)), -hoverMaxY, 0);
		}
		return super.mouseScrolled(mouseX, mouseY, scrolled);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (mouseButton == 0) {
			isScrolled = isMouseHover(mouseX, mouseY, scrollWidth * 2 - 14, 4, 10, ySize - 51);
			if (isScrolled) {
				double yPos = ValueUtil.correctDouble(mouseY, 20.0d, ySize - 71.0d) - 20.0d;
				scrollY = ValueUtil.correctInt((int) (yPos / (ySize - 91.0d) * -scrollMaxY), -scrollMaxY, 0);
			}
			isHovered = isMouseHover(mouseX, mouseY, xSize - 35, 76, 10, hoverHeightMax - 4);
			if (isHovered) {
				double yPos = ValueUtil.correctDouble(mouseY, 86.0d, 61.0d + hoverHeightMax) - 86.0d;
				hoverY = ValueUtil.correctInt((int) (yPos / (hoverHeightMax - 26.0d) * -hoverMaxY), -hoverMaxY, 0);
			}
		}
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		if (Mouse.isButtonDown(0)) {
			if (isScrolled) {
				double yPos = ValueUtil.correctDouble(mouseY, 20.0d, ySize - 71.0d) - 20.0d;
				scrollY = ValueUtil.correctInt((int) (yPos / (ySize - 91.0d) * -scrollMaxY), -scrollMaxY, 0);
			} else if (isHovered) {
				double yPos = ValueUtil.correctDouble(mouseY, 86.0d, 61.0d + hoverHeightMax) - 86.0d;
				hoverY = ValueUtil.correctInt((int) (yPos / (hoverHeightMax - 26.0d) * -hoverMaxY), -hoverMaxY, 0);
			}
		}
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
	}

	@Override
	public void save() { }

	@Override
	public void setGuiData(NBTTagCompound compound) {
		wait = false;
		marcet = MarcetController.getInstance().getMarcet(marcet.getId());
		((ContainerNPCTrader) inventorySlots).marcet = marcet;
		initGui();
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) { }

	@Override
	public void textUpdate(IComponentGui component, String text) {
		search = text;
		initGui();
	}

	@SideOnly(Side.CLIENT)
	public static class TradeButton extends GuiButtonNop {

		protected static final Random rnd = new Random();
		protected final Minecraft mc = Minecraft.getMinecraft();
		protected final Deal deal;
		protected final DealMarkup dm;
		protected final boolean inTrade;
		// case
		protected ResourceLocation objCase;
		protected Map<String, ResourceLocation> materialTextures = new HashMap<>();
		protected boolean type;
		protected boolean start;
		protected int rncd;
		// hovers
		protected final List<Component> hoverMain = new ArrayList<>();
		protected final List<Component> hoverPrise = new ArrayList<>();

		protected ParameterizedModel CHEST_FULL;
		protected ParameterizedModel CHEST_BODY;
		protected ParameterizedModel CHEST_TOP;

		public TradeButton(GuiNPCTrader gui, Deal dealIn, int level, int x, int y, int w, boolean inTradeIn) {
			super(gui, dealIn.getId(), dealIn.getName(), x, y, null);
			texture = BUTTONS;
			deal = dealIn;
			txrW = 256;
			txrH = 28;
			width = w;
			height = 28;

			dm = MarcetController.getInstance().getBuyData(marcet, deal, level, gui.count);
			if (!deal.isCase()) {
				renderStack = dm.main;
			}
			inTrade = inTradeIn;
			// product info
			if (deal.isCase()) {
				hoverMain.add(Component.translatable("market.hover.case"));
				hoverMain.add(Component.translatable("market.deal.case.count", deal.getCaseCount()));
				if (!deal.showInCase()) {
					hoverMain.add(Component.translatable("market.case.show.false").withStyle(TextFormatting.RED));
				}
				if (deal.showInCase() ||
						(mc.player != null && mc.player.isCreative())) {
					deal.putHoverCaseItems(hoverMain, TooltipFlags.NORMAL);
				}
			}
			else {
				hoverMain.add(Component.translatable("market.hover.product"));
				hoverMain.add(Component.literal(dm.main.getDisplayName())
						.append(Component.literal(" x").withStyle(TextFormatting.GRAY)
								.append(Component.literal(dm.count + " ").withStyle(TextFormatting.GOLD))
								.append(Component.translatable("market.hover.item." + (deal.getMaxCount() > 0 ? deal.getAmount() == 0 ? "not" : "amount" : "infinitely"), "" + deal.getAmount()))));
			}
			if (deal.getAmount() > 0 || (mc.player != null && mc.player.isCreative())) {
				if (deal.getAmount() <= 0) {
					hoverPrise.add(Component.translatable("gui.allowed"));
				}
				// buy hover info
				if (!dm.buyItems.isEmpty()) {
					hoverPrise.add(Component.translatable("market.hover.item.buy"));
					for (ItemStack curr : dm.buyItems.keySet()) {
						hoverPrise.add(Component.literal(curr.getDisplayName())
								.append(Component.literal(" x").withStyle(TextFormatting.GRAY)
										.append(Component.literal(dm.buyItems.get(curr) + " ").withStyle(TextFormatting.GOLD))));
					}
				}
				if (dm.buyMoney > 0) {
					hoverPrise.add(Component.translatable("market.hover.currency.buy", dm.buyMoney, CustomNpcs.displayCurrencies));
				}
				if (dm.buyDonat > 0) {
					hoverPrise.add(Component.translatable("market.hover.donat.buy", dm.buyDonat, CustomNpcs.displayDonation));
				}
				// sell hover info
				if (!dm.sellItems.isEmpty()) {
					hoverPrise.add(Component.translatable("market.hover.item.sell"));
					for (ItemStack curr : dm.sellItems.keySet()) {
						hoverPrise.add(Component.literal(curr.getDisplayName())
								.append(Component.literal(" x").withStyle(TextFormatting.GRAY)
										.append(Component.literal(dm.sellItems.get(curr) + " ").withStyle(TextFormatting.GOLD))));
					}
				}
				if (dm.sellMoney > 0) {
					hoverPrise.add(Component.translatable("market.hover.currency.sell", dm.sellMoney, CustomNpcs.displayCurrencies));
				}
			}
			// case model
			rncd = rnd.nextInt(10000);
			objCase = deal.getCaseObjModel();
			if (objCase != null) {
				try {
					mc.getResourceManager().getResource(objCase);
					objCase = Deal.defaultCaseOBJ;
				}
				catch (Exception e) { objCase = null; }
			}
			materialTextures.put("minecraft:entity/chest/christmas", deal.getCaseTexture());
			CHEST_FULL = ModelBuffer.getParameterizedModel(objCase, null, materialTextures, true, 0, false);
			CHEST_BODY = ModelBuffer.getParameterizedModel(objCase, Collections.singletonList("body"), materialTextures, true, 0, false);
			CHEST_TOP = ModelBuffer.getParameterizedModel(objCase, Collections.singletonList("top"), materialTextures, true, 0, false);
		}

		@Override
		public void renderWidget(int mouseX, int mouseY, float partialTicks) {
			if (!visible) { return; }
			GlStateManager.enableBlend();
			GuiNPCTrader parent = (GuiNPCTrader) listener;
			int y = getY() + parent.scrollY;
			isHovered = mouseY > 14 && mouseY < parent.ySize - 47 && mouseX >= getX() && mouseY >= y && mouseX < getX() + width && mouseY < y + height;
			if (isHovered) {
				hoverText.clear();
				hoverText.addAll(hoverMain);
			}
			int x = getX();
			if (y + height < 15 || y > parent.ySize - 48) { return; }
			y = getY();

			GL11.glEnable(GL11.GL_SCISSOR_TEST);
			int c = parent.xSize < mc.displayWidth ? (int) Math.round((double) mc.displayWidth / (double) parent.xSize) : 1;
			GL11.glScissor(4 * c, 47 * c, (parent.scrollWidth * 2) * c, (parent.ySize - 62) * c);

			GlStateManager.pushMatrix();
			GlStateManager.translate(0, parent.scrollY, 0);

			GlStateManager.pushMatrix();
			boolean isPrefabricated = txrW == 0;
			float scaleH = height / (float) txrH;
			float scaleW = isPrefabricated ? scaleH : width / (float) txrW;
			GlStateManager.scale(scaleW, scaleH, 1.0f);
			GlStateManager.translate(x / scaleW, y / scaleH, 0.0f);
			mc.getTextureManager().bindTexture(texture);
			drawTexturedModalRect(0, 0, txrX, txrY + getState(inTrade) * txrH, txrW, txrH);
			GlStateManager.popMatrix();

			// rarity color
			if (deal.getRarityColor() != 0) { drawGradientRect(x + 2, y + 2, x + width - 2, y + height - 2, 0x0, deal.getRarityColor() | 0x80000000); }
			// case obj model
			if (deal.isCase() && objCase != null) {
				GlStateManager.pushMatrix();
				GlStateManager.translate(x + 16.0f, y + 8.5f, 16.0f);
				if ((System.currentTimeMillis() + rncd) % 10000 < 2000 || isHovered && isMouseHover(mouseX, mouseY, x + 1, y + parent.scrollY + 2, 32, 22)) {
					float i = (float) ((System.currentTimeMillis() + rncd) % 2000);
					if (!start) {
						GlStateManager.rotate(-15.0f, 1.0f, 0.0f, 0.0f);
						GlStateManager.rotate(-75.0f, 0.0f, 1.0f, 0.0f);
						GlStateManager.scale(16.0f, -16.0f, 16.0f);
						ModelBuffer.render(CHEST_FULL);
						if (i >= 1980) { start = true; }
					}
					else {
						if (i <= 20) { type = rnd.nextFloat() < 0.5f; }
						float rot;
						if (type) {
							if (i < 600) { rot = 0.033333f * i; }
							else if (i < 1700) { rot = - 0.027273f * i + 36.363636f; }
							else { rot = 0.033333f * i - 66.666666f; }
							GlStateManager.rotate(-15.0f, 1.0f, 0.0f, 0.0f);
							GlStateManager.rotate(-75.0f + rot, 0.0f, 1.0f, 0.0f);
							GlStateManager.scale(16.0f, -16.0f, 16.0f);
							ModelBuffer.render(CHEST_FULL);
						}
						else {
							GlStateManager.rotate(-15.0f, 1.0f, 0.0f, 0.0f);
							GlStateManager.rotate(-75.0f, 0.0f, 1.0f, 0.0f);
							GlStateManager.scale(16.0f, -16.0f, 16.0f);
							ModelBuffer.render(CHEST_BODY);
							if (i < 1500) { rot = 0.016667f * i; }
							else if (i < 1900) { rot = 25.0f; }
							else { rot = -0.25f * i + 500.0f; }
							GlStateManager.pushMatrix();
							GlStateManager.rotate(rot, 0.0f, 0.0f, 1.0f);
							ModelBuffer.render(CHEST_TOP);
							GlStateManager.popMatrix();
						}
					}
				}
				else {
					GlStateManager.rotate(-15.0f, 1.0f, 0.0f, 0.0f);
					GlStateManager.rotate(-75.0f, 0.0f, 1.0f, 0.0f);
					GlStateManager.scale(16.0f, -16.0f, 16.0f);
					ModelBuffer.render(CHEST_FULL);
				}
				GlStateManager.popMatrix();
			}
			if (renderStack != null && !renderStack.isEmpty()) {
				mc.getTextureManager().bindTexture(GuiNPCTrader.ICONS);
				drawTexturedModalRect(x + 6, y + 2, 0, getState(true) * 24, 24, 24);
				if (!inTrade) { GlStateManager.color(0.4F, 0.4F, 0.4F, 1.0F); }
				mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, renderStack, x + 10, y + 6);
				mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, renderStack, x + 10, y + 6, "");
				if (!inTrade) { GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); }
				if (isHovered && isMouseHover(mouseX, mouseY, x + 6, y + parent.scrollY + 3, 22, 22)) {
					hoverText.clear();
					for (String line : renderStack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? TooltipFlags.ADVANCED : TooltipFlags.NORMAL)) {
						hoverText.add(new Component(line, true));
					}
				}
			}
			// money and barter
			int mw = 0;
			if (deal.getAmount() > 0 || (mc.player != null && mc.player.isCreative())) {
				// money
				Component money = Component.empty();
				if (dm.sellMoney > 0) {
					money.append(Component.literal("↑").withStyle(TextFormatting.YELLOW))
							.append(Component.literal(Util.instance.getTextReducedNumber(dm.sellMoney, true, true, false)).withStyle(TextFormatting.RESET));
				}
				if (dm.buyMoney > 0) {
					if (!money.getString().isEmpty()) { money.append(" "); }
					money.append(Component.literal("↓").withStyle(TextFormatting.GREEN))
							.append(Component.literal(Util.instance.getTextReducedNumber(dm.buyMoney, true, true, true)).withStyle(TextFormatting.RESET));
				}
				Component donat = Component.empty();
				if (dm.buyDonat > 0) {
					if (!donat.getString().isEmpty()) { donat.append(" "); }
					donat.append(Component.literal("↓").withStyle(TextFormatting.BLUE))
							.append(Component.literal(Util.instance.getTextReducedNumber(dm.buyDonat, true, true, false)).withStyle(TextFormatting.RESET));
				}
				int mt = 0;
				boolean hasM = !money.getString().isEmpty();
				boolean hasD = !donat.getString().isEmpty();
				if (hasM && hasD) {
					x = getX() + width - 14;
					y = getY() + 3;
					mt = 1;
					mw = mc.fontRenderer.getStringWidth(money.getString());
					if (System.currentTimeMillis() % 4000 < 2000) { mt = 2; mw = mc.fontRenderer.getStringWidth(donat.getString()); }
				}
				else if (hasM || hasD) {
					x = getX() + width - 14;
					y = getY() + 3;
					if (hasM) { mt = 1; mw = mc.fontRenderer.getStringWidth(money.getString()); }
					if (hasD) { mt = 2; mw = mc.fontRenderer.getStringWidth(donat.getString()); }
				}
				// draw prise info
				if (mt != 0) {
					x -= mw;
					mc.fontRenderer.drawString((mt == 1 ? money : donat).getString(), x, y, CustomNpcs.MainColor.getRGB() | 0xFF000000);
					GlStateManager.pushMatrix();
					GlStateManager.translate(x + mw - 2, y - 4, 0.0f);
					GlStateManager.scale(0.0625f, 0.0625f, 0.0625f);
					mc.getTextureManager().bindTexture(mt == 1 ? GuiBasic.MONEY : GuiBasic.DONAT);
					drawTexturedModalRect(0, 0, 0, 0, 256, 256);
					GlStateManager.popMatrix();
					if (isHovered && isMouseHover(mouseX, mouseY, x, y + parent.scrollY, mw + 14, 10)) {
						hoverText.clear();
						hoverText.addAll(hoverPrise);
					}
				}
				// barter
				if (!dm.buyItems.isEmpty()) {
					float sc = 1.0f;
					int size = dm.buyItems.size();
					int bw = size * 16;
					if (width - 34 < bw) { sc = (width - 34.0f) / (float) bw; }
					float s = 0.666666f * sc;
					// slots
					GlStateManager.pushMatrix();
					GlStateManager.translate(getX() + width - 2 - bw * sc, getY() + height - 2.0f - 16.0f * sc, 0.0f);
					GlStateManager.scale(s, s, s);
					mc.getTextureManager().bindTexture(GuiNPCTrader.ICONS);
					for (int i = 0; i < size; i++) {
						drawTexturedModalRect(i * 24, 0, 0, 0, 24, 24);
					}
					GlStateManager.popMatrix();
					s = 0.875f * sc;
					// stacks
					GlStateManager.pushMatrix();
					x = (int) (getX() + width - 1 - bw * sc);
					y = (int) (getY() + height - 1.0f - 16.0f * sc);
					GlStateManager.translate(x, y, 0.0f);
					GlStateManager.scale(s, s, s);
					int i = 0;
					List<Component> hovers = new ArrayList<>();
					for (ItemStack stack : dm.buyItems.keySet()) {
						mc.getRenderItem().renderItemAndEffectIntoGUI(stack, i * 18, 0);
						GlStateManager.pushMatrix();
						String sCount = String.valueOf(dm.buyItems.get(stack));
						GlStateManager.translate(i * 18.0f + 17.0f, 16.0f, 200.0f);
						GlStateManager.scale(0.75f, 0.75f, 0.75f);
						GlStateManager.disableLighting();
						GlStateManager.disableDepth();
						GlStateManager.disableBlend();
						mc.fontRenderer.drawStringWithShadow(sCount, 24.0f - (float) mc.fontRenderer.getStringWidth(sCount), -7.5f, CustomNpcs.MainColor.getRGB());
						GlStateManager.enableLighting();
						GlStateManager.enableDepth();
						GlStateManager.enableBlend();
						GlStateManager.popMatrix();

						if (isHovered && isMouseHover(mouseX, mouseY, x + (i * 18) * s, y + parent.scrollY, 16.0f * s, 16.0f * s)) {
							hoverText.clear();
							List<String> tooltip = stack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? TooltipFlags.ADVANCED : TooltipFlags.NORMAL);
							boolean isStart = true;
							for (String line : tooltip) {
								if (isStart) {
									hovers.add(Component.literal(line).append(TextFormatting.RESET + " x" + sCount));
									isStart = false;
								} else { hovers.add(new Component(line, true)); }
							}
							hoverText.addAll(hovers);
						}
						i++;
					}
					GlStateManager.popMatrix();
				}
			}
			// name
			GlStateManager.popMatrix();

			GlStateManager.pushMatrix();
			x = getX() + 36;
			y = getY() + 2 + parent.scrollY;
			renderString(getMessage(), x, y, x + width - 39 - mw, y + 10,
					CustomNpcs.MainColor.getRGB() | 0xFF000000, true, false, customFont);
			GlStateManager.popMatrix();

			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
		}

		public int getState(boolean tradeIn) {
			if (!tradeIn) { return 2; }
			if (!listener.hasSubGui()) {
				try {
					if (((GuiNPCTrader) listener).selectDealData.deal.equals(deal)) { return 1; }
				}
				catch (Exception ignored) { }
				if (isHovered && !listener.hasSubGui()) {
					return Mouse.isButtonDown(0) ? 2 : 1;
				}
			}
			return 0;
		}

		public boolean isMouseHover(double mX, double mY, double px, double py, double pWidth, double pHeight) {
			return mX >= px && mY >= py && mX < (px + pWidth) && mY < (py + pHeight);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
			if (enabled && visible && isValidClickButton(mouseButton) && isHovered) {
				onClick(mouseX, mouseY);
				return true;
			}
			return false;
		}

	}

	@SideOnly(Side.CLIENT)
	public static class SectionButton extends GuiMenuSideButton {

		public SectionButton(GuiNPCTrader gui, int id, MarcetSection sectionIn, int x, int y) {
			super(gui, id, Component.empty(), x, y);
			setWidth(sectionIn == null ? 16 : 24);
			setHeight(sectionIn == null ? 9 : 24);
			texture = GuiNPCTrader.ICONS;
			if (sectionIn != null) {
				txrX = (sectionIn.getIcon() % 10) * 24;
				txrY = (int) Math.floor((float) sectionIn.getIcon() / 10.0f) * 72;
			} else {
				txrX = 240;
				txrY = id == 3 ? 27 : 0;
			}
		}

		@Override
		public void render(int mouseX, int mouseY, float partialTicks) {
			if (!visible) { return; }
			isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
			int state = 0;
			boolean lbm = Mouse.isButtonDown(0);
			if (isHoveredOrFocused() && !listener.hasSubGui()) { state = (lbm ? 2 : 1) * height; }
			else if (active) { state = height; }
			GlStateManager.pushMatrix();
			GlStateManager.enableAlpha();
			GlStateManager.enableBlend();
			Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.translate(0.0f, 0.0f, 1.0f);
			drawTexturedModalRect(x, y, txrX, txrY + state, width, height);

			if (isHovered && !hoverText.isEmpty() && listener != null) { listener.setHoverText(hoverText); }
			GlStateManager.popMatrix();
		}

	}

	@SideOnly(Side.CLIENT)
	public static class TradeButtonBiDirectional extends GuiButtonBiDirectional {

		public TradeButtonBiDirectional(GuiNPCTrader gui, int x, int y, int w) {
			super(gui, 0, x, y, 0, new Object[1]);
			texture = BUTTONS;
			txrY = 84;
			txrW = 256;
			txrH = 20;
			width = w;

			display = new Component[64];
			for (int i = 0; i < 64; i++) { display[i] = Component.literal("" + (i + 1)); }
			displayValue = gui.count - 1;
			if (displayValue < display.length) { setDisplayText(display[displayValue]); }
		}

		@Override
		public void render(int mouseX, int mouseY, float partialTicks) {
			if (!visible) { return; }
			isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
			hoverL = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + 20 && mouseY < getY() + height;
			hoverR = !hoverL && mouseX >= getX() + width - 19 && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
			Minecraft mc = Minecraft.getMinecraft();

			boolean lmb = Mouse.isButtonDown(0);
			int stateL = !enabled ? 40 : hoverL ? (display.length > 1 ? lmb ? 40 : 20 : 0) : 0;
			int stateR = !enabled ? 40 : hoverR ? (display.length > 1 ? lmb ? 40 : 20 : 0) : 0;
			int state = !enabled ? 40 : isHovered && display.length > 1 ? 20 : 0;
			int wl = (width - 38) / 2;
			int wr = width - 39 - wl;

			GlStateManager.pushMatrix();
			GlStateManager.enableAlpha();
			GlStateManager.enableBlend();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.translate(getX(), getY(), 0.0f);
			mc.getTextureManager().bindTexture(GuiNPCTrader.BUTTONS);
			drawTexturedModalRect(0, 0, 0, txrY + stateL, 19, 20);
			drawTexturedModalRect(width - 20, 0, 256 - 19, txrY + stateR, 19, 20);
			drawTexturedModalRect(19, 0, 19, txrY + state, wl, 20);
			drawTexturedModalRect(19 + wl, 0, 236 - wr, txrY + state,  wr, 20);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.popMatrix();

			Component mes = getMessage();
			if (isHovered) { mes = Component.empty().append(mes).withStyle(TextFormatting.UNDERLINE); }
			renderString(mes, getX() + 11, getY(), getX() + getWidth() - 11, getY() + getHeight(),
					getFGColor() | (int) Math.ceil(alpha * 255.0F) << 24, showShadow, true, customFont);
			if (isHovered && !hoverText.isEmpty() && listener != null) { listener.setHoverText(hoverText); }
		}

	}

	@SideOnly(Side.CLIENT)
	public static class MarcetTextField extends GuiTextFieldNop implements IComponentGui {

		public MarcetTextField(GuiNPCTrader gui, int x, int y, int widthIn) {
			super(gui, 0, x, y, widthIn, 18, search);
			setEnableBackgroundDrawing(false);
		}

		@Override
		public void renderWidget(int mouseX, int mouseY, float partialTicks) {
			if (!enabled || !visible) { return; }
			setTextColor(getTextColor());
			int x = getX() - 3;
			int y = getY() - 6;
			int w = width + 6;
			isHovered = mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + height + 2;

			int w0 = w / 2;
			int w1 = w - w0;
			int state = isFocused() || !isHovered ? 56 : 0;
			Minecraft mc = Minecraft.getMinecraft();
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.pushMatrix();
			GlStateManager.enableAlpha();
			GlStateManager.enableBlend();
			mc.getTextureManager().bindTexture(GuiNPCTrader.BUTTONS);
			GlStateManager.translate(0.0f, 0.0f, 1.0f);
			drawTexturedModalRect(x, y, 0, state, w0, 10); // left
			drawTexturedModalRect(x, y + 10, 0, state + 18, w0, 10); // left down
			drawTexturedModalRect(x + w0, y, 256 - w1, state, w1, 10); // right up
			drawTexturedModalRect(x + w0, y + 10, 256 - w1, state + 18, w1, 10); // right down
			super.renderWidget(mouseX, mouseY, partialTicks);
			GlStateManager.popMatrix();
			if (isHovered && !hoverText.isEmpty() && listener != null) { listener.setHoverText(hoverText); }
		}

	}

}
