package noppes.npcs.client.gui.global;

import java.util.*;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.ConfirmScreen;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketDealDelete;
import noppes.npcs.packets.server.SPacketMarcetDelete;
import noppes.npcs.packets.server.SPacketMarcetSave;
import noppes.npcs.packets.server.SPacketMarcetsGet;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.handler.data.IDeal;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.player.GuiNPCTrader;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.shared.client.gui.util.ResourceData;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.data.Deal;
import noppes.npcs.controllers.data.DealMarkup;
import noppes.npcs.controllers.data.Marcet;
import noppes.npcs.controllers.data.MarcetSection;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.util.CustomNPCsScheduler;

import javax.annotation.Nonnull;

public class GuiNpcManageMarkets extends GuiNPCInterface2
		implements IGuiData, ICustomScrollListener {

	protected static Marcet selectedMarcet;
	protected static Deal selectedDeal;
	public static int marcetId;
	public static int dealId;

	protected final Map<Component, Integer> dataDeals = new LinkedHashMap<>();
	protected final Map<Component, Integer> dataMarkets = new LinkedHashMap<>();
	protected GuiCustomScrollNop scrollMarkets;
	protected GuiCustomScrollNop scrollDeals;
	protected GuiCustomScrollNop scrollAllDeals;
	protected MarcetController mData;
	private int tabSelect;

	public GuiNpcManageMarkets(EntityNPCInterface npc) {
		super(npc);
		imageHeight = 200;

		backGui = EnumGuiType.MainMenuGlobal;

		mData = MarcetController.getInstance();
		selectedMarcet = mData.getMarcet(marcetId);
		if (selectedMarcet != null) {
			if (selectedMarcet.getSection(dealId) >= 0) {
				IDeal[] deals = selectedMarcet.getAllDeals();
				if (deals.length > 0) { dealId = deals[0].getId(); }
				else { dealId = 0; }
			}
			selectedDeal = mData.getDeal(dealId);
		}
		tabSelect = 0;
		Packets.sendServer(new SPacketMarcetsGet(-1));
	}

	@Override
	public void initGui() {
		super.initGui();
		mData = MarcetController.getInstance();
		int w = 120;
		int h = imageHeight - 24;
		if (scrollMarkets == null) { scrollMarkets = addScroll(0).setSize(w, h); }
		if (scrollDeals == null) { scrollDeals = addScroll(1).setSize(w, h); }
		if (scrollAllDeals == null) { scrollAllDeals = addScroll(2).setSize(w, h); }
		int x0 = guiLeft + 5;
		int x1 = x0 + w + 5;
		int x2 = x1 + w + 45;
		int y = guiTop + 14;
		// Markets:
		LinkedHashMap<Integer, List<Component>> htsM = new LinkedHashMap<>();
		if (marcetId < 0 || selectedMarcet == null) {
			for (Marcet m : mData.markets.values()) {
				selectedMarcet = m;
				marcetId = m.getId();
				break;
			}
		}
		if (!dataMarkets.isEmpty()) {
			int i = 0;
			for (int id : dataMarkets.values()) {
				Marcet marcet = mData.getMarcet(id);
				if (marcet == null) {
					setGuiData(new NBTTagCompound());
					return;
				}
				List<Component> info = new ArrayList<>();
				info.add(Component.empty()
						.append(Component.literal("ID: ").withStyle(TextFormatting.GRAY))
						.append(Component.literal("" + marcet.getId()).withStyle(TextFormatting.RESET)));
				info.add(Component.empty()
						.append(Component.translatable("gui.name").withStyle(TextFormatting.GRAY))
						.append(Component.literal(": ").withStyle(TextFormatting.GRAY))
						.append(Component.literal(marcet.name).withStyle(TextFormatting.RESET)));
				if (!marcet.isValid()) {
					info.add(Component.translatable("market.hover.nv.market"));
					for (MarcetSection ms : selectedMarcet.sections.values()) {
						for (Deal deal : ms.deals) {
							if (deal.isValid()) { continue; }
							if (deal.getProduct().getMCItemStack() == null || deal.getProduct().getMCItemStack().getItem() == Items.AIR) {info.add(Component.translatable("market.hover.nv.market.deal.0", "" + deal.getId())); }
							if (deal.getMoney() == 0 && deal.getCurrency().isEmpty()) { info.add(Component.translatable("market.hover.nv.market.deal.1", "" + deal.getId())); }
						}
					}
					if (info.size() > 9) {
						info.add(Component.literal("..."));
						break;
					}
				}
				htsM.put(i, info);
				i++;
			}
		}
		scrollMarkets.setUnsortedList(new ArrayList<>(dataMarkets.keySet()))
				.setHoverTexts(htsM);
		if (selectedMarcet != null) { scrollMarkets.setSelected(selectedMarcet.getSettingName()); }
		// Deals:
		if (!dataDeals.isEmpty()) {
			List<Component> allDeals = new ArrayList<>(dataDeals.keySet());
			LinkedHashMap<Integer, List<Component>> htsAD = new LinkedHashMap<>();
			List<ItemStack> stacks = new ArrayList<>();
			List<ResourceData> allPrefixes = new ArrayList<>();
			Map<Integer, TempDealInfo> map = new TreeMap<>();
			int i = 0;
			for (Component key : dataDeals.keySet()) {
				int dealID = dataDeals.get(key);
				Deal deal = mData.getDeal(dealID);
				List<Component> totalInfo = new ArrayList<>();
				totalInfo.add(Component.empty()
						.append(Component.literal("ID: ").withStyle(TextFormatting.GRAY))
						.append(Component.literal("" + dealID).withStyle(TextFormatting.RESET)));
				List<Component> marcetInfo = new ArrayList<>();
				marcetInfo.add(Component.empty()
						.append(Component.literal("ID: ").withStyle(TextFormatting.GRAY))
						.append(Component.literal("" + dealID).withStyle(TextFormatting.RESET)));
				DealMarkup dm = new DealMarkup();
				if (deal != null) { dm.set(deal); }
				ItemStack stack = ItemStack.EMPTY;
				int tab = selectedMarcet.getSection(dealID);
				if (deal == null || !deal.isValid()) {
					totalInfo.add(Component.translatable("market.hover.nv.deal"));
					if (deal == null) { totalInfo.add(Component.translatable("hover.total.error")); }
					else {
						if (deal.isCase()) {
							if (deal.getCaseItems().length ==0 || deal.getCaseItems()[0].getItem().isEmpty()) { totalInfo.add(Component.translatable("market.hover.nv.deal.product")); }
						} else {
							stack = dm.main;
							if (dm.main == null || dm.main.getItem() == Items.AIR) { totalInfo.add(Component.translatable("market.hover.nv.deal.product")); }
						}
						if (dm.baseMoney <= 0 || dm.baseDonat <= 0 || dm.baseItems.isEmpty()) { totalInfo.add(Component.translatable("market.hover.nv.deal.barter")); }
					}
				}
				else {
					if (tab == tabSelect) {
						marcetInfo.add(Component.empty()
								.append(Component.translatable("gui.sections").withStyle(TextFormatting.GRAY))
								.append(Component.literal(" ID: ").withStyle(TextFormatting.GRAY))
								.append(Component.literal("" + tab).withStyle(TextFormatting.RESET))
								.append(Component.literal("; ").withStyle(TextFormatting.GRAY))
								.append(Component.translatable("gui.name").withStyle(TextFormatting.GRAY))
								.append(Component.literal(": \"").withStyle(TextFormatting.GRAY))
								.append(Component.translatable(selectedMarcet.sections.get(tab).name).withStyle(TextFormatting.RESET))
								.append(Component.literal("\"").withStyle(TextFormatting.GRAY)));
					}
					stack = dm.main;
					if (deal.isCase()) {
						totalInfo.add(Component.translatable("market.hover.case"));
						marcetInfo.add(Component.translatable("market.hover.case"));
						totalInfo.add(Component.translatable("market.deal.case.count", deal.getCaseCount()));
						marcetInfo.add(Component.translatable("market.deal.case.count", deal.getCaseCount()));
						if (!deal.showInCase()) {
							totalInfo.add(Component.translatable("market.case.show.false").withStyle(TextFormatting.RED));
							marcetInfo.add(Component.translatable("market.case.show.false").withStyle(TextFormatting.RED));
						}
						deal.putHoverCaseItems(totalInfo, ITooltipFlag.TooltipFlags.ADVANCED);
						deal.putHoverCaseItems(marcetInfo, ITooltipFlag.TooltipFlags.ADVANCED);
					}
					else {
						totalInfo.add(Component.translatable("market.hover.product"));
						marcetInfo.add(Component.translatable("market.hover.product"));
						totalInfo.add(Component.literal(dm.main.getDisplayName()).append(" x" + dm.count));
						marcetInfo.add(Component.literal(dm.main.getDisplayName()).append(" x" + dm.count + (deal.getMaxCount() > 0
								? Component.translatable("market.hover.item.amount", "" + deal.getAmount())
								: "")));
					}
					// ItemStack
					if (!dm.baseItems.isEmpty()) {
						totalInfo.add(Component.translatable("market.hover.item." + (deal.showInCase() ? 1 : 0)));
						for (ItemStack curr : dm.baseItems.keySet()) { totalInfo.add(Component.literal(dm.main.getDisplayName()).append(" x" + dm.baseItems.get(curr))); }
					}
					if (dm.baseMoney > 0) { totalInfo.add(Component.literal("Money: " + dm.baseMoney + CustomNpcs.displayCurrencies)); }
					if (dm.baseDonat > 0) { totalInfo.add(Component.literal("Donat: " + dm.baseDonat + CustomNpcs.displayDonation)); }
					totalInfo.add(Component.translatable("market.deal.type." + dm.deal.getType()).withStyle(TextFormatting.YELLOW));
					totalInfo.add(Component.translatable("market.deal.chance").append(" " + dm.deal.getChance() + "%").withStyle(TextFormatting.RESET));
				}
				if (tabSelect == tab) {
					map.put(dealID, new TempDealInfo(key, stack, marcetInfo));
					allDeals.remove(key);
				}
				else {
					htsAD.put(i++, totalInfo);
					stacks.add(stack);
					if (deal != null && deal.isCase()) {
						ResourceLocation objCase = deal.getCaseObjModel();
						if (objCase != null) {
							try {minecraft.getResourceManager().getResource(objCase); } catch (Exception e) { objCase = null; }
						}
						if (objCase == null) {
							try {
								minecraft.getResourceManager().getResource(Deal.defaultCaseOBJ);
								objCase = Deal.defaultCaseOBJ;
							} catch (Exception ignored) { }
						}
						if (objCase == null) { allPrefixes.add(ResourceData.EMPTY); }
						else {
							ResourceData rd = new ResourceData(objCase, 0, 0, 1, 1);
							rd.materialTextures.put("#material", new ResourceLocation("minecraft", "entity/chest/normal"));
							rd.rotateX = -15.0f;
							rd.rotateY = -75.0f;
							rd.tH = -2.0f;
							rd.scaleX = rd.scaleY = rd.scaleZ = 8.0f;
							allPrefixes.add(rd);
						}
					}
					else { allPrefixes.add(ResourceData.EMPTY); }
				}
			}
			int j = 0;
			List<Component> marcetDeals = new ArrayList<>();
			List<ItemStack> marcetStacks = new ArrayList<>();
			List<ResourceData> marcetPrefixes = new ArrayList<>();
			LinkedHashMap<Integer, List<Component>> htsD = new LinkedHashMap<>();
			for (IDeal deal : selectedMarcet.getDeals(tabSelect)) {
				if (map.containsKey(deal.getId())) {
					TempDealInfo tdi = map.get(deal.getId());
					marcetDeals.add(tdi.key);
					marcetStacks.add(tdi.stack);
					if (deal.isCase()) {
						ResourceLocation objCase = deal.getCaseObjModel();
						if (objCase != null) {
							try { minecraft.getResourceManager().getResource(objCase); } catch (Exception e) { objCase = null; }
						}
						if (objCase == null) {
							try {
								minecraft.getResourceManager().getResource(Deal.defaultCaseOBJ);
								objCase = Deal.defaultCaseOBJ;
							} catch (Exception ignored) { }
						}
						if (objCase == null) { marcetPrefixes.add(ResourceData.EMPTY); }
						else {
							ResourceData rd = new ResourceData(objCase, 0, 0, 1, 1);
							rd.materialTextures.put("#material", new ResourceLocation("minecraft", "entity/chest/normal"));
							rd.rotateX = -15.0f;
							rd.rotateY = -75.0f;
							rd.tH = -2.0f;
							rd.scaleX = rd.scaleY = rd.scaleZ = 8.0f;
							marcetPrefixes.add(rd);
						}
					} else { marcetPrefixes.add(ResourceData.EMPTY); }
					htsD.put(j++, tdi.marcetInfo);
				}
			}
			scrollAllDeals.setUnsortedList(allDeals)
					.setHoverTexts(htsAD)
					.setStacks(stacks)
					.setPrefixes(allPrefixes);
			scrollDeals.setUnsortedList(marcetDeals)
					.setStacks(marcetStacks)
					.setHoverTexts(htsD)
					.setPrefixes(marcetPrefixes);
		}
		if (selectedDeal != null) {
			scrollAllDeals.setSelected(selectedDeal.getSettingName());
			scrollDeals.setSelected(selectedDeal.getSettingName());
		}
		add(scrollMarkets.setPos(x0, y));
		add(scrollDeals.setPos(x1, y));
		add(scrollAllDeals.setPos(x2, y));
		scrollMarkets.resetRoll();
		scrollDeals.resetRoll();
		scrollAllDeals.resetRoll();
		int lId = 0;
		// market keys
		addLabel(lId++, x0 + 2, y - 9, "global.market")
				.setHoverTexts("market.hover.names");
		// market deals keys
		addLabel(lId++, x1, y - 9, "market.deals")
				.setHoverTexts("market.hover.deals");
		// all deals keys
		addLabel(lId, x2, y - 9, "market.all.deals")
				.setHoverTexts("market.hover.all.deals");
		y += h + 2;
		int bw = (w - 2) / 3;
		// add market
		addButton(0, x0, y, "gui.add")
				.setSize(bw, 20)
				.setHoverTexts("market.hover.market.add");
		// del market
		addButton(1, x0 + 2 + bw, y, "gui.remove")
				.setSize(bw, 20)
				.setIsEnabled(marcetId > 0 && selectedMarcet != null && mData.markets.size() > 1)
				.setHoverTexts("market.hover.market.del");
		// edit market
		addButton(2, x0 + (2 + bw) * 2, y, "selectServer.edit")
				.setSize(bw, 20)
				.setIsEnabled(selectedMarcet != null)
				.setHoverTexts("market.hover.market.settings");
		// add deal
		addButton(3, x2, y, "gui.add")
				.setSize(bw, 20)
				.setHoverTexts("market.hover.deal.add");
		// del deal
		addButton(4, x2 + 2 + bw, y, "gui.remove")
				.setSize(bw, 20)
				.setIsEnabled(dealId >= 0 && selectedDeal != null && dataDeals.size() > 1)
				.setHoverTexts("market.hover.deal.del");
		// edit deal
		addButton(5, x2 + (2 + bw) * 2, y, "selectServer.edit")
				.setSize(bw, 20)
				.setIsEnabled(selectedDeal != null)
				.setHoverTexts("market.hover.deal.settings");
		// market tabs
		Object[] tabs = new Object[selectedMarcet.sections.size()];
		int i = 0;
		for (MarcetSection tab : selectedMarcet.sections.values()) {
			tabs[i] = Component.translatable(tab.name);
			i++;
		}
		addButton(6, x1, y, true, tabSelect, tabs)
				.setSize(w, 20)
				.setHoverTexts("market.hover.section");
		// work buttons
		int x3 = x2 - 43;
		y = guiTop + 44;
		// add
		addButton(7, x3, y, "<")
				.setSize(41, 20)
				.setIsEnabled(scrollAllDeals.hasSelected())
				.setHoverTexts("market.hover.add.deal");
		// del
		addButton(8, x3, y += 22, ">")
				.setSize(41, 20)
				.setIsEnabled(scrollDeals.hasSelected())
				.setHoverTexts("market.hover.del.deal");
		// add all
		addButton(9, x3, y += 22, "<<")
				.setSize(41, 20)
				.setIsEnabled(!scrollAllDeals.getList().isEmpty())
				.setHoverTexts("market.hover.add.deals");
		// del all
		addButton(10, x3, y += 22, ">>")
				.setSize(41, 20)
				.setIsEnabled(!scrollDeals.getList().isEmpty())
				.setHoverTexts("market.hover.del.deals");
		// up
		addButton(11, x3, y += 28, Component.literal("↑ ").append(Component.translatable("gui.up")))
				.setSize(41, 20)
				.setIsEnabled(scrollDeals.getSelectedIndex() > 0)
				.setHoverTexts("hover.up");
		// down
		addButton(12, x3, y + 22, Component.literal("↓ ").append(Component.translatable("gui.down")))
				.setSize(41, 20)
				.setIsEnabled(scrollDeals.getSelectedIndex() >= 0 && scrollDeals.getSelectedIndex() < scrollDeals.getNormalList().size() - 1)
				.setHoverTexts("hover.down");
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				save();
				selectedMarcet = mData.addMarcet();
				marcetId = selectedMarcet.getId();
				initGui();
				CustomNPCsScheduler.runTack(() -> setSubGui(new SubGuiNpcMarketSettings(selectedMarcet)), 50);
				break;
			} // Add market
			case 1: {
				ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
					if (agree) {
						if (selectedMarcet == null) { return; }
						selectedMarcet = null;
						Packets.sendServer(new SPacketMarcetDelete(marcetId));
						marcetId = 0;
					}
					NoppesUtil.openGUI(player, this);
				},
						scrollMarkets.getNormalSelected().getParent(),
						Component.translatable("message.delete").getParent());
				setScreen(guiYesNo);
				break;
			} // Del market
			case 2: {
				setSubGui(new SubGuiNpcMarketSettings(selectedMarcet));
				break;
			} // Market settings
			case 3: {
				save();
				SubGuiNPCManageDeal.parent = this;
				NoppesUtil.requestOpenGUI(EnumGuiType.SetupTraderDeal, new BlockPos(marcetId, mData.getUnusedDealId(), 0));
				break;
			} // Add deal
			case 4: {
				if (!dataDeals.containsKey(scrollAllDeals.getNormalSelected()) && !dataDeals.containsKey(scrollDeals.getNormalSelected())) { return; }
				selectedDeal = null;
				Packets.sendServer(new SPacketDealDelete(dealId));
				dealId = 0;
				break;
			} // Del deal
			case 5: {
				if (dealId < 0) { return; }
				SubGuiNPCManageDeal.parent = this;
				NoppesUtil.requestOpenGUI(EnumGuiType.SetupTraderDeal, new BlockPos(marcetId, mData.getUnusedDealId(), 0));
				onClose();
				break;
			} // Deal settings
			case 6: {
				tabSelect = button.getValue();
				initGui();
				break;
			} // tab / MarcetSection
			case 7: {
				if (selectedMarcet == null || selectedDeal == null) { return; }
				int tab = selectedMarcet.getSection(selectedDeal.getId());
				if (tab == tabSelect) { return; }
				selectedMarcet.sections.get(tabSelect).addDeal(selectedDeal.getId());
				scrollAllDeals.setSelectedIndex(-1);
				scrollDeals.setSelectedIndex(-1);
				setGuiData(null);
				break;
			} // <
			case 8: {
				if (!dataDeals.containsKey(scrollDeals.getNormalSelected())) { return; }
				int id = dataDeals.get(scrollDeals.getNormalSelected());
				selectedMarcet.sections.get(tabSelect).removeDeal(id);
				scrollAllDeals.setSelectedIndex(-1);
				scrollDeals.setSelectedIndex(-1);
				setGuiData(null);
				break;
			} // >
			case 9: {
				if (dataDeals.isEmpty()) { return; }
				for (int id : dataDeals.values()) { selectedMarcet.sections.get(tabSelect).addDeal(id); }
				scrollAllDeals.setSelectedIndex(-1);
				scrollDeals.setSelectedIndex(-1);
				setGuiData(null);
				break;
			} // <<
			case 10: {
				if (scrollDeals.getList().isEmpty()) { return; }
				selectedMarcet.sections.get(tabSelect).removeAllDeals();
				scrollAllDeals.setSelectedIndex(-1);
				scrollDeals.setSelectedIndex(-1);
				setGuiData(null);
				break;
			} // >>
			case 11: {
				if (selectedMarcet == null || !dataDeals.containsKey(scrollDeals.getNormalSelected())) { return; }
				int pos = scrollDeals.getSelectedIndex();
				Collections.swap(selectedMarcet.sections.get(tabSelect).deals, pos, pos - 1);
				scrollDeals.setSelectedIndex(pos - 1);
				setGuiData(null);
				break;
			} // up
			case 12: {
				if (selectedMarcet == null || !dataDeals.containsKey(scrollDeals.getNormalSelected())) { return; }
				int pos = scrollDeals.getSelectedIndex();
				Collections.swap(selectedMarcet.sections.get(tabSelect).deals, pos, pos + 1);
				scrollDeals.setSelectedIndex(pos + 1);
				setGuiData(null);
				break;
			} // down
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		GlStateManager.pushMatrix();
		if (hasSubGui()) { GlStateManager.translate(0.0f, 0.0f, -2.0f); }
		int u = 0;
		int v = 0;
		if (selectedMarcet != null && selectedMarcet.sections.containsKey(tabSelect)) {
			int icon = selectedMarcet.sections.get(tabSelect).getIcon();
			u = (icon % 10) * 24;
			v = (int) Math.floor((float) icon / 10.0f) * 72;
		}
		mc.getTextureManager().bindTexture(GuiNPCTrader.ICONS);
		drawTexturedModalRect(guiLeft + 252, guiTop + 189, u, v, 24, 24);
		GlStateManager.popMatrix();
	}

	@Override
	public void save() {
		if (selectedMarcet != null) {
			selectedMarcet.resetAllDeals();
			Packets.sendServer(new SPacketMarcetSave(selectedMarcet.save()));
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		try {
			switch (scroll.id) {
				case 0: {
					if (!dataMarkets.containsKey(scroll.getNormalSelected())) { return; }
					selectedMarcet = mData.getMarcet(dataMarkets.get(scroll.getNormalSelected()));
					marcetId = selectedMarcet.getId();
					initGui();
					break;
				} // Markets
				case 1: // Deals
				case 2: {
					if (!dataDeals.containsKey(scroll.getNormalSelected())) { return; }
					selectedDeal = mData.getDeal(dataDeals.get(scroll.getNormalSelected()));
					if (selectedDeal != null) { dealId = selectedDeal.getId(); }
					if (scroll.id == 1) { scrollAllDeals.setSelectedIndex(-1); }
					else { scrollDeals.setSelectedIndex(-1); }
					initGui();
					break;
				} // All Deals
			}
		} catch (Exception e) { LogWriter.error(e); }
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		switch (scroll.id) {
			case 0: {
				if (selectedMarcet == null) { return; }
				setSubGui(new SubGuiNpcMarketSettings(selectedMarcet));
				break;
			} // Markets
			case 1: // Deals
			case 2: {
				if (selectedMarcet == null || !dataDeals.containsKey(scroll.getNormalSelected())) { return; }
				save();
				SubGuiNPCManageDeal.parent = this;
				NoppesUtil.requestOpenGUI(EnumGuiType.SetupTraderDeal, new BlockPos(marcetId, dealId, 0));
				break;
			} // All Deals
		}
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		mData = MarcetController.getInstance();
		dataMarkets.clear();
		for (Marcet m : mData.markets.values()) {
			dataMarkets.put(m.getSettingName(), m.getId());
			if (marcetId < 0 || marcetId == m.getId() || selectedMarcet == null) {
				selectedMarcet = m;
				marcetId = m.getId();
			}
		}
		dataDeals.clear();
		for (Deal d : mData.deals.values()) {
			dataDeals.put(d.getSettingName(), d.getId());
			if (dealId < 0 || dealId == d.getId() || selectedDeal == null) {
				selectedDeal = d;
				dealId = d.getId();
			}
		}
		initGui();
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		setGuiData(null);
		NoppesUtil.openGUI(player, this);
	}

	public static class TempDealInfo {

		public final Component key;
		public final ItemStack stack;
		public final List<Component> marcetInfo;

		public TempDealInfo(Component keyIn, ItemStack stackIn, List<Component> marcetInfoIn) {
			key = keyIn;
			stack = stackIn;
			marcetInfo = marcetInfoIn;
		}

	}

}
