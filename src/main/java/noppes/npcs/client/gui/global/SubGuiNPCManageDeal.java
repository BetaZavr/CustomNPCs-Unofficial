package noppes.npcs.client.gui.global;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.entity.data.ICustomDrop;
import noppes.npcs.client.gui.availability.SubGuiNpcAvailability;
import noppes.npcs.client.gui.drop.SubGuiDropEdit;
import noppes.npcs.client.gui.select.SubGuiColorSelector;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerNPCTraderSetup;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.data.Deal;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketContainerOpen;
import noppes.npcs.packets.server.SPacketDealSave;
import noppes.npcs.packets.server.SPacketMarcetsGet;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import javax.annotation.Nonnull;

public class SubGuiNPCManageDeal extends GuiContainerNPCInterface<ContainerNPCTraderSetup>
		implements ICustomScrollListener, ITextfieldListener {

	protected static final Random rnd = new Random();
	protected final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/menubg.png");
	protected final Deal deal;
	protected final int[][] slotPoses = new int[10][2];
	protected GuiCustomScrollNop scroll;
	protected ResourceLocation objCase;
	public static GuiScreen parent;
	//case
	protected Map<String, ResourceLocation> materialTextures = new HashMap<>();
	protected ParameterizedModel CHEST_FULL;
	protected ParameterizedModel CHEST_BODY;
	protected ParameterizedModel CHEST_TOP;
	protected boolean type;
	protected boolean start;
	protected final ContainerNPCTraderSetup menu;

	public SubGuiNPCManageDeal(ContainerNPCTraderSetup container) {
		super(NoppesUtilServer.getEditingNpc(Minecraft.getMinecraft().player), container, Component.empty());
		setBackground("npcdrop.png");
		xSize = 380;
		ySize = 217;

		menu = container;
		for (int slotId = 0; slotId < 10; ++slotId) {
			slotPoses[slotId][0] = container.getSlot(slotId).xPos;
			slotPoses[slotId][1] = container.getSlot(slotId).yPos;
		}
		deal = container.deal;
		Packets.sendServer(new SPacketMarcetsGet(-1));
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		switch (button.id) {
			case 0: deal.setIgnoreDamage(button.getValue() == 1); break;
			case 1: deal.setIgnoreNBT(button.getValue() == 1); break;
			case 2: setSubGui(new SubGuiNpcAvailability(deal.availability,  parent)); initGui(); break;
			case 3: deal.setType(button.getValue()); break;
			case 4: deal.setIsCase(button.getValue() == 1); initGui(); break;
			case 5: {
				if (!deal.isCase()) { return; }
				SubGuiDropEdit.parent = null;
				SubGuiDropEdit.parentContainer = EnumGuiType.SetupTraderDeal;
				SubGuiDropEdit.parentData = new BlockPos(menu.marcet.getId(), deal.getId(), 0);
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("InventoryType", 1);
				compound.setInteger("Marcet", menu.marcet.getId());
				compound.setInteger("Deal", deal.getId());
				compound.setInteger("DropSet", -1);
				Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
				break;
			} // add
			case 6: {
				if (!deal.isCase() || !scroll.hasSelected()) { return; }
				deal.removeCaseItem(scroll.getSelectedIndex());
				initGui();
				break;
			} // del
			case 7: {
				if (!deal.isCase() || !scroll.hasSelected()) { return; }
				SubGuiDropEdit.parent = null;
				SubGuiDropEdit.parentContainer = EnumGuiType.SetupTraderDeal;
				SubGuiDropEdit.parentData = new BlockPos(menu.marcet.getId(), deal.getId(), 0);
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("InventoryType", 1);
				compound.setInteger("Marcet", menu.marcet.getId());
				compound.setInteger("Deal", deal.getId());
				compound.setInteger("DropSet", scroll.getSelectedIndex());
				Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
				break;
			} // edit
			case 8: {
				setSubGui(new SubGuiColorSelector(deal.getRarityColor(), new SubGuiColorSelector.ColorCallback() {
					@Override
					public void color(int colorIn) {
						deal.setRarityColor(colorIn);
						initGui();
					}
					@Override
					public void preColor(int colorIn) {
						((GuiColorButtonNop) button).setColor(colorIn);
						deal.setRarityColor(colorIn);
					}
				}));
				break;
			} // color
			case 9: if (deal.isCase()) { setSubGui(new SubGuiNpcDealCaseSetting(deal)); } break;
			case 11: if (deal.isCase()) { deal.setShowInCase(((GuiCheckBoxNop) button).selected()); } break;
			case 66: onClose(); break;
		}
	}

	@Override
	public void onClose() {
		super.onClose();
		if (parent != null) { setScreen(parent); }
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		// Background
		minecraft.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 182, ySize);
		minecraft.getTextureManager().bindTexture(resource);
		drawTexturedModalRect(guiLeft + 182, guiTop, 256 - xSize + 182, 0, xSize - 182, ySize);
		int x0 = guiLeft + 4;
		int x1 = x0 + 59;
		int y0 = guiTop + 3;
		int y1 = y0 + 129;
		int y2 = y0 + 36;
		if (deal.getRarityColor() != 0) {
			int color = 0xA0000000 | deal.getRarityColor();
			drawGradientRect(x0 + 1, y0 + 2, x1, y2, 0x0, color);
			drawGradientRect(x0 + 1, y2, x1, y1, 0x0, color);
		}
		// Slots
		for (int slotId = deal.isCase() ? 1 : 0; slotId < 10; ++slotId) {
			int x = guiLeft + slotPoses[slotId][0];
			int y = guiTop + slotPoses[slotId][1];
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			minecraft.getTextureManager().bindTexture(GuiBasic.RESOURCE_SLOT);
			drawTexturedModalRect(x - 1, y - 1, 0, 0, 18, 18);
		}
		int color = new Color(0x80000000).getRGB();
		drawHorizontalLine(guiLeft + 170, guiLeft + xSize - 4, guiTop + 15, color);
		drawHorizontalLine(guiLeft + 4, guiLeft + 170, guiTop + 132, color);
		drawVerticalLine(guiLeft + 170, guiTop + 3, guiTop + ySize - 4, color);
		drawHorizontalLine(x0 + 1, guiLeft + 170, y0 + 1, color);
		drawHorizontalLine(x0 + 1, x1, y2, color);
		drawVerticalLine(x0, y0, guiTop + ySize - 4, color);
		drawVerticalLine(x1, y0, y1, color);
		// case
		if (deal.isCase() && objCase != null) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(guiLeft + 30, guiTop + 21, 50.0f);
			if ((System.currentTimeMillis()) % 10000 < 2000) {
				float i = (float) ((System.currentTimeMillis()) % 2000);
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
			drawHorizontalLine(guiLeft + 170, guiLeft + xSize - 4, guiTop + 143, color);
		}
		else {
			drawHorizontalLine(guiLeft + 170, guiLeft + xSize - 4, guiTop + 160, color);
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		int lId = 0;
		int x = guiLeft + menu.getSlot(1).xPos;
		int y = guiTop + menu.getSlot(1).yPos - 48;
		addLabel(lId++, x, y, "market.product")
				.setHoverTexts("market.hover.product");
		y = guiTop + menu.getSlot(1).yPos - 11;
		addLabel(lId++, x, y, "market.barter")
				.setHoverTexts("market.hover.item");
		// Type
		x = guiLeft + 67;
		y = guiTop + 6;
		addLabel(lId++, x, y + 1, Component.translatable("gui.type").append(":"))
				.setSize(20, 12);
		addButton(4, x + 22, y, false, deal.isCase() ? 1 : 0, "enum.entity.item", "gui.case")
				.setSize(80, 14)
				.setHoverTexts("market.hover.deal.type");
		addButton(66, guiLeft + xSize - 17, y - 2, "X")
				.setSize(12, 12)
				.setHoverTexts("hover.back");
		ICustomDrop[] caseItems = deal.getCaseItems();
		if (scroll == null) { scroll = addScroll(0).setSize(102, 116); }
		add(scroll.setPos(x, y + 16)
				.setIsVisible(deal.isCase())
				.disabledSearch());
		if (deal.isCase()) {
			y += 111;
			List<Component> list = new ArrayList<>();
			List<ItemStack> stacks = new ArrayList<>();
			LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
			int i = 0;
			for (ICustomDrop dropSet : caseItems) {
				list.add(((DropSet) dropSet).getKey());
				stacks.add(dropSet.getMCItemStack());
				hts.put(i++, ((DropSet) dropSet).getHover(true));
			}
			scroll.setUnsortedList(list).setStacks(stacks).setHoverTexts(hts);
			addButton(5, x, y, "gui.add")
					.setSize(32, 14)
					.setHoverTexts("market.hover.case.add");
			addButton(6, x + 34, y, "gui.remove")
					.setSize(32, 14)
					.setIsEnabled(scroll.hasSelected())
					.setHoverTexts("market.hover.case.del");
			addButton(7, x + 68, y, "selectServer.edit")
					.setSize(34, 14)
					.setIsEnabled(scroll.hasSelected())
					.setHoverTexts("market.hover.case.edit");
		}

		// Dial settings
		x = guiLeft + 174;
		y = guiTop + 4;
		addLabel(lId++, x, y, "marcet.deal.settings")
				.setSize(200, 12)
				.setHoverTexts("market.hover.deal.section");
		addLabel(lId++, x, (y += 14) + 1, "market.currency")
				.setSize(98, 12);
		addLabel(lId++, x + 141, y + 1, CustomNpcs.displayCurrencies)
				.setSize(15, 12);
		addLabel(lId++, x + 195, y + 1, CustomNpcs.displayDonation)
				.setSize(15, 12);
		addTextField(0, x + 100, y, 39, 12, "" + deal.getMoney())
				.setMinMaxDefault(0, Integer.MAX_VALUE, deal.getMoney())
				.setHoverTexts("market.hover.set.currency");
		addTextField(4, x + 154, y, 39, 12, "" + deal.getDonat())
				.setMinMaxDefault(0, Integer.MAX_VALUE, deal.getDonat())
				.setHoverTexts("market.hover.set.donat");
		addLabel(lId++, x, (y += 16) + 1, "drop.chance")
				.setSize(98, 12);
		addLabel(lId++, x + 155, y + 1, "%")
				.setSize(10, 12);
		addTextField(1, x + 100, y, 50, 12, "" + deal.getChance())
				.setMinMaxDefault(0, 100, deal.getChance())
				.setHoverTexts("market.hover.set.chance");
		addLabel(lId++, x, (y += 16) + 1, "quest.itemamount")
				.setSize(98, 12);
		addTextField(2, x + 100, y, 40, 12, "" + deal.getMinCount())
				.setMinMaxDefault(0, Integer.MAX_VALUE, deal.getMinCount())
				.setHoverTexts("market.hover.set.amount");
		addLabel(lId++, x + 143, y + 1, "<->")
				.setSize(15, 12);
		addTextField(3, x + 160, y, 40, 12, "" + deal.getMaxCount())
				.setMinMaxDefault(0, Integer.MAX_VALUE, deal.getMaxCount())
				.setHoverTexts("market.hover.set.amount");
		addLabel(lId++, x, (y += 15) + 1, "gui.ignoreDamage")
				.setSize(98, 12);
		addButton(0, x + 100, y, false, deal.getIgnoreDamage() ? 1 : 0, "gui.ignoreDamage.0", "gui.ignoreDamage.1")
				.setSize(80, 14)
				.setHoverTexts("recipe.hover.damage");
		addLabel(lId++, x, (y += 16) + 1, "gui.ignoreNBT")
				.setSize(98, 12);
		addButton(1, x + 100, y, false, deal.getIgnoreNBT() ? 1 : 0, "gui.ignoreNBT.0", "gui.ignoreNBT.1")
				.setSize(80, 14)
				.setHoverTexts("recipe.hover.nbt");
		addLabel(lId++, x, (y += 16) + 1, "availability.options")
				.setSize(98, 12);
		addButton(2, x + 100, y, "selectServer.edit")
				.setSize(80, 14)
				.setHoverTexts("availability.hover");
		addLabel(lId++, x, (y += 16) + 1, "market.case.color")
				.setSize(98, 12);
		add(new GuiColorButtonNop(this, 8, x + 100, y, deal.getRarityColor())
				.setSize(80, 14)
				.setHoverTexts("market.hover.deal.color"));
		materialTextures.clear();
		addCheckBox(10, x, y += 16, "market.deal.barter.true", "market.deal.barter.false", false)
				.setSize(200, 12)
				.setIsEnabled(false);
		if (deal.isCase()) {
			addCheckBox(11, x, y += 16, "market.deal.show.case.info.true", "market.deal.show.case.info.false", deal.showInCase())
					.setSize(200, 12);
			addLabel(lId, x, (y += 17) + 1, Component.translatable("gui.case").append(":"))
					.setSize(98, 12);
			addButton(9, x + 100, y, "selectServer.edit")
					.setSize(80, 14)
					.setHoverTexts("market.hover.deal.case");
			objCase = deal.getCaseObjModel();
			if (objCase != null) {
				try {
					minecraft.getResourceManager().getResource(objCase);
					objCase = Deal.defaultCaseOBJ;
				}
				catch (Exception e) { objCase = null; }
			}
			menu.setSlotPos(0, new int[] { -5000, -5000 });
			materialTextures.put("#material", deal.getCaseTexture());
		}
		else {
			objCase = null;
			addButton(3, x, y + 16, false, deal.getType(), "market.deal.type.0", "market.deal.type.1", "market.deal.type.2")
					.setSize(200, 14)
					.setHoverTexts("market.hover.set.type");
			menu.setSlotPos(0, slotPoses[0]);
		}
		CHEST_FULL = ModelBuffer.getParameterizedModel(objCase, null, materialTextures, true, 0, false);
		CHEST_BODY = ModelBuffer.getParameterizedModel(objCase, Collections.singletonList("body"), materialTextures, true, 0, false);
		CHEST_TOP = ModelBuffer.getParameterizedModel(objCase, Collections.singletonList("top"), materialTextures, true, 0, false);
	}

	@Override
	public void save() {
		if (MarcetController.getInstance().deals.containsKey(deal.getId()) ||
				(deal.isCase() && deal.getCaseItems().length > 0) ||
				!deal.getProduct().isEmpty()) { Packets.sendServer(new SPacketDealSave(deal.saveData())); }
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 0: deal.setMoney(textField.getInteger()); break;
			case 1: deal.setChance(textField.getInteger()); break;
			case 2: deal.setCount(textField.getInteger(), deal.getMaxCount()); break;
			case 3: deal.setCount(deal.getMinCount(), textField.getInteger()); break;
			case 4: deal.setDonat(textField.getInteger()); break;
		}
		initGui();
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) { initGui(); }

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (!deal.isCase() || !scroll.hasSelected()) { return; }
		SubGuiDropEdit.parent = null;
		SubGuiDropEdit.parentContainer = EnumGuiType.SetupTraderDeal;
		SubGuiDropEdit.parentData = new BlockPos(menu.marcet.getId(), deal.getId(), 0);
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("InventoryType", 1);
		compound.setInteger("Marcet", menu.marcet.getId());
		compound.setInteger("Deal", deal.getId());
		compound.setInteger("DropSet", scroll.getSelectedIndex());
		Packets.sendServer(new SPacketContainerOpen(EnumGuiType.SetupDrop, (b) -> b.writeNbt(compound)));
	}

}
