package noppes.npcs.client.gui.drop;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.entity.data.IAttributeSet;
import noppes.npcs.api.entity.data.IDropNbtSet;
import noppes.npcs.api.entity.data.IEnchantSet;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.availability.SubGuiNpcAvailability;
import noppes.npcs.client.gui.global.GuiNpcManageQuest;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerNPCDropSetup;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.AttributeSet;
import noppes.npcs.entity.data.DropNbtSet;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.entity.data.EnchantSet;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketDealDropSetSave;
import noppes.npcs.packets.server.SPacketNpcInvDropSetSave;
import noppes.npcs.packets.server.SPacketQuestDropSetSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nonnull;
import java.util.*;

public class SubGuiDropEdit extends GuiContainerNPCInterface<ContainerNPCDropSetup>
		implements ICustomScrollListener, ITextfieldListener {

	protected final ResourceLocation resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/menubg.png");

	public static GuiScreen parent;
	public static BlockPos parentData;
	public static EnumGuiType parentContainer;

	protected Map<Component, AttributeSet> attributesData = new LinkedHashMap<>();
	protected Map<Component, EnchantSet> enchantData = new LinkedHashMap<>();
	protected Map<Component, DropNbtSet> tagsData = new LinkedHashMap<>();
	protected AttributeSet attribute;
	protected EnchantSet enchant;
	protected DropNbtSet tag;
	protected GuiCustomScrollNop scrollAttributes;
	protected GuiCustomScrollNop scrollEnchants;
	protected GuiCustomScrollNop scrollTags;
	protected DropSet drop;
	protected int[] amount;
	protected int reset;
	protected int slot;
	protected ContainerNPCDropSetup menu;

	public SubGuiDropEdit(EntityNPCInterface npc, ContainerNPCDropSetup container) {
		super(npc, container, Component.empty());
		setBackground("npcdrop.png");
		closeOnEsc = true;
		xSize = 421;
		ySize = 217;

		menu = container;
		drop = container.inventoryDS;
		if (drop != null) { amount = new int[] { drop.getMinAmount(), drop.getMaxAmount() }; }
	}

	@Override
	public void buttonEvent(@Nonnull GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				drop.resetTo(drop.item);
				initGui();
				break;
			} // reset drop
			case 1: {
				enchant = (EnchantSet) drop.addEnchant(0);
				setSubGui(new SubGuiDropEnchant(enchant));
				break;
			} // add enchant
			case 2: {
				drop.removeEnchant(enchantData.get(scrollEnchants.getNormalSelected()));
				initGui();
				break;
			} // remove enchant
			case 3: setSubGui(new SubGuiDropEnchant(enchant)); break; // edit enchant
			case 4: {
				attribute = (AttributeSet) drop.addAttribute("");
				setSubGui(new SubGuiDropAttribute(attribute));
				break;
			} // add attribute
			case 5: {
				drop.removeAttribute(attributesData.get(scrollAttributes.getNormalSelected()));
				initGui();
				break;
			} // remove attribute
			case 6: setSubGui(new SubGuiDropAttribute(attribute)); break; // edit attribute
			case 7: {
				tag = (DropNbtSet) drop.addDropNbtSet(0, 100.0d, "", new String[0]);
				setSubGui(new SubGuiDropValueNbt(tag));
				break;
			} // add tag
			case 8: {
				drop.removeDropNbt(tagsData.get(scrollTags.getNormalSelected()));
				initGui();
				break;
			} // remove tag
			case 9: setSubGui(new SubGuiDropValueNbt(tag)); break; // edit tag
			case 10: drop.setLootMode(button.getValue()); break; // loot mode
			case 11: drop.setTiedToLevel(button.getValue() == 1); initGui(); break; // tied mode
			case 12: setSubGui(new SubGuiNpcAvailability(drop.availability, this)); break; // availability
		}
	}

	@Override
	public void drawDefaultBackground() {
		RenderHelper.disableStandardItemLighting();
		// Background
		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLeft, guiTop, 0.0f);
		GlStateManager.scale(bgScale, bgScale, bgScale);
		mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(0, 0, 0, 0, 252, ySize);
		mc.getTextureManager().bindTexture(resource);
		drawTexturedModalRect(252, 0, 256 - xSize + 252, 0, xSize - 252, ySize);
		GlStateManager.popMatrix();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (drop == null || (parent == null && (parentData == null || parentContainer == null))) {
			String message = "";
			if (drop == null) { message = "drop; "; }
			if (parent == null) {
				message += "parent";
				if (parentData == null || parentContainer == null) { message += " and data or container"; }
				message += "; ";
			}
			LogWriter.pathInfo("Not set " + message + " to GUI", -1);
			onClose();
			return;
		}
		if (reset > 0) {
			reset--;
			if (reset == 0) { initGui(); }
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void initGui() {
		super.initGui();
		if (drop.item != menu.getSlot(0).getStack()) { drop.item = menu.getSlot(0).getStack(); }
		int lId = 0;
		// slot
		addLabel(lId++, guiLeft + 171, guiTop + 137, "drop.slot")
				.setSize(28, 12)
				.setHoverTexts("drop.hover.slot");
		int x = guiLeft + 225;
		int y = guiTop + 135;
		// chance
		addLabel(lId++, x, y + 2, "drop.chance")
				.setSize(48, 12)
				.setIsVisible(!drop.item.isEmpty());
		addTextField(0, x + 50, y, 50, 16, drop.getChance())
				.setMinMaxDefault(0.0001d, 100.0d, drop.getChance())
				.setIsVisible(!drop.item.isEmpty())
				.setHoverTexts("drop.hover.chance");
		// amount
		String tied = Component.translatable("drop.tied.random").getFormattedText();
		if (drop.tiedToLevel) { tied = Component.translatable("drop.tied.level").getFormattedText(); }
		boolean needReAmount = false;
		amount = drop.amount;
		if (drop.getMinAmount() > drop.item.getMaxStackSize()) {
			amount[0] = drop.item.getMaxStackSize();
			needReAmount = true;
		}
		else if (drop.getMinAmount() <= 0) {
			amount[0] = 1;
			needReAmount = true;
		}
		if (drop.getMaxAmount() > drop.item.getMaxStackSize()) {
			amount[1] = drop.item.getMaxStackSize();
			needReAmount = true;
		}
		else if (drop.getMaxAmount() <= 0) {
			amount[1] = 1;
			needReAmount = true;
		}
		if (needReAmount) { drop.setAmount(amount[0], amount[1]); }
		addLabel(lId++, x, y += 22, Component.translatable("drop.amount").append(":"))
				.setSize(48, 12)
				.setIsVisible(!drop.item.isEmpty());
		addLabel(lId++, x, (y += 16) + 2, "gui.min")
				.setSize(48, 12)
				.setIsVisible(!drop.item.isEmpty());
		addTextField(1, x + 50, y, 50, 16, "" + amount[0])
				.setMinMaxDefault(1, drop.item.getMaxStackSize(), drop.item.getCount())
				.setIsVisible(!drop.item.isEmpty())
				.setHoverTexts(Component.translatable("drop.hover.amount", tied));
		addLabel(lId++, x, (y += 20) + 2, "gui.max")
				.setSize(48, 12)
				.setIsVisible(!drop.item.isEmpty());
		addTextField(2, x + 50, y, 50, 16, "" + amount[1])
				.setMinMaxDefault(1, drop.item.getMaxStackSize(), drop.item.getCount())
				.setIsVisible(!drop.item.isEmpty())
				.setHoverTexts(Component.translatable("drop.hover.amount", tied));
		// reset
		addButton(0, guiLeft + 171, guiTop + 169, "remote.reset")
				.setSize(48, 20)
				.setIsEnabled(!drop.item.isEmpty())
				.setHoverTexts("drop.hover.reset");
		// Enchants:
		// List
		addLabel(lId++, guiLeft + 4, guiTop + 5, "drop.enchants")
				.setSize(133, 12)
				.setHoverTexts("drop.hover.enchants");
		enchantData.clear();
		for (IEnchantSet ies : drop.getEnchantSets()) { enchantData.put(((EnchantSet) ies).getKey(), (EnchantSet) ies); }
		if (scrollEnchants == null) { scrollEnchants = addScroll(0).setSize(133, 115); }
		scrollEnchants.setNormalList(new ArrayList<>(enchantData.keySet()))
				.disabledSearch();
		if (enchant != null) { scrollEnchants.setSelected(enchant.getKey()); }
		add(scrollEnchants.setPos(guiLeft + 4, guiTop + 16));
		// enchant add
		addButton(1, guiLeft + 4, guiTop + 112, "gui.add")
				.setSize(43, 20)
				.setIsEnabled(!drop.item.isEmpty() && enchantData.size() <= 16)
				.setHoverTexts("drop.hover.enchant.add");
		// enchant del
		addButton(2, guiLeft + 4 + 45, guiTop + 112, "gui.remove")
				.setSize(43, 20)
				.setIsEnabled(scrollEnchants.hasSelected())
				.setHoverTexts("drop.hover.enchant.del");
		// enchant edit
		addButton(3, guiLeft + 4 + 91, guiTop + 112, "selectServer.edit")
				.setSize(43, 20)
				.setIsEnabled(scrollEnchants.hasSelected())
				.setHoverTexts("drop.hover.enchant.edit");
		// Attributes:
		// List
		addLabel(lId++, guiLeft + 143, guiTop + 5, "drop.attributes")
				.setSize(133, 12)
				.setHoverTexts("drop.hover.attributes");
		attributesData.clear();
		for (IAttributeSet ias : drop.getAttributeSets()) { attributesData.put(((AttributeSet) ias).getKey(), ((AttributeSet) ias)); }
		if (scrollAttributes == null) { scrollAttributes = addScroll(1).setSize(133, 115); }
		scrollAttributes.setNormalList(new ArrayList<>(attributesData.keySet()))
				.disabledSearch();
		if (attribute != null) { scrollAttributes.setSelected(attribute.getKey()); }
		add(scrollAttributes.setPos(guiLeft + 143, guiTop + 16));
		// attribute add
		addButton(4, guiLeft + 143, guiTop + 112, "gui.add")
				.setSize(43, 20)
				.setIsEnabled(!drop.item.isEmpty() && attributesData.size() <= 16)
				.setHoverTexts("drop.hover.attribute.add");
		// attribute del
		addButton(5, guiLeft + 143 + 45, guiTop + 112, "gui.remove")
				.setSize(44, 20)
				.setIsEnabled(scrollAttributes.hasSelected())
				.setHoverTexts("drop.hover.attribute.del");
		// attribute edit
		addButton(6, guiLeft + 143 + 91, guiTop + 112, "selectServer.edit")
				.setSize(43, 20)
				.setIsEnabled(scrollAttributes.hasSelected())
				.setHoverTexts("drop.hover.attribute.edit");
		// Tags:
		// List
		addLabel(lId, guiLeft + 283, guiTop + 5, "drop.tags")
				.setSize(133, 12)
				.setHoverTexts("drop.hover.tags");
		tagsData.clear();
		for (IDropNbtSet dns : drop.getDropNbtSets()) { tagsData.put(((DropNbtSet) dns).getKey(), (DropNbtSet) dns); }
		if (scrollTags == null) { scrollTags = addScroll(2).setSize(133, 115); }
		scrollTags.setNormalList(new ArrayList<>(tagsData.keySet()))
				.disabledSearch();
		if (tag != null) { scrollTags.setSelected(tag.getKey()); }
		add(scrollTags.setPos(guiLeft + 283, guiTop + 16));
		// tag add
		addButton(7, guiLeft + 283, guiTop + 112, "gui.add")
				.setSize(43, 20)
				.setIsEnabled(!drop.item.isEmpty() && tagsData.size() <= 24)
				.setHoverTexts("drop.hover.tag.add");
		// tag del
		addButton(8, guiLeft + 283 + 45, guiTop + 112, "gui.remove")
				.setSize(43, 20)
				.setIsEnabled(scrollTags.hasSelected())
				.setHoverTexts("drop.hover.tag.del");
		// tag edit
		addButton(9, guiLeft + 283 + 91, guiTop + 112, "selectServer.edit")
				.setSize(43, 20)
				.setIsEnabled(scrollTags.hasSelected())
				.setHoverTexts("drop.hover.tag.edit");
		x = guiLeft + 329;
		y = guiTop + 146;
		// availability
		addButton(12, x, y, "availability.available")
				.setSize(87, 20)
				.setIsVisible(!drop.item.isEmpty())
				.setHoverTexts("availability.hover");
		// lootMode
		addButton(10, x, y += 23, false, drop.lootMode, "stats.normal", "inv.auto", "inv.inventory")
				.setSize(87, 20)
				.setIsVisible(!drop.item.isEmpty() && parentContainer == EnumGuiType.MainMenuInv)
				.setHoverTexts("drop.hover.mode");
		// tied level
		int t = (int) (3.0f + 9.0f * 17.0f / (float) CustomNpcs.MaxLv);
		addButton(11, x, y + 23, false, drop.tiedToLevel ? 1 : 0, "drop.type.random", "drop.type.level")
				.setSize(87, 20)
				.setIsVisible(!drop.item.isEmpty() && (parentContainer == EnumGuiType.MainMenuInv || parent instanceof GuiNpcManageQuest))
				.setHoverTexts(Component.translatable("drop.hover.tied", TextFormatting.RED + "" + CustomNpcs.MaxLv, CustomNpcs.MaxLv, TextFormatting.YELLOW + "" + t));
	}

	@Override
	protected void handleMouseClick(@Nonnull Slot slotIn, int slotId, int mouseButton, @Nonnull ClickType type) {
		if (hasSubGui()) { return; }
		super.handleMouseClick(slotIn, slotId, mouseButton, type);
		reset = 1;
	}

	@Override
	public void onClose() {
		GuiTextFieldNop.unfocus();
		save();
		if (parent != null) { setScreen(parent); }
		else if (parentData != null && parentContainer != null) { NoppesUtil.requestOpenGUI(parentContainer, parentData); }
	}

	@Override
	public void save() {
		if (drop.pos == -1) {
			if (drop.item.isEmpty()) { return; }
			if (drop.getMinAmount() == 1 && drop.getMaxAmount() == 1) { drop.setAmount(drop.item.getCount(), drop.item.getCount()); }
		}
		drop.item.setCount(1);
		if (menu.dataType == 0 ) { Packets.sendServer(new SPacketNpcInvDropSetSave(menu.dropType, menu.groupId, drop.pos, drop.save())); }
		else if (menu.dataType == 1) { Packets.sendServer(new SPacketDealDropSetSave(menu.marcetID, menu.dealID, drop.save())); }
		else if (menu.dataType == 2) { Packets.sendServer(new SPacketQuestDropSetSave(menu.questID, drop.save())); }
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (!scroll.hasSelected()) { return; }
		GuiTextFieldNop.unfocus();
		switch (scroll.id) {
			case 0: enchant = enchantData.get(scroll.getNormalSelected()); break; // scrollEnchants
			case 1: attribute = attributesData.get(scroll.getNormalSelected()); break; // scrollAttributes
			case 2: tag = tagsData.get(scroll.getNormalSelected()); break; // scrollTags
		}
		initGui();
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		switch (scroll.id) {
			case 0: setSubGui(new SubGuiDropEnchant(enchant)); break; // scrollEnchants
			case 1: setSubGui(new SubGuiDropAttribute(attribute)); break; // scrollAttributes
			case 2: setSubGui(new SubGuiDropValueNbt(tag)); break; // scrollTags
		}
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiDropEnchant) {
			enchant.load(((SubGuiDropEnchant) subgui).enchant.getNBT());
		}
		else if (subgui instanceof SubGuiDropAttribute) {
			if (((SubGuiDropAttribute) subgui).attribute.getAttribute().isEmpty()) { drop.removeAttribute(attribute); }
			else { attribute.load(((SubGuiDropAttribute) subgui).attribute.getNBT()); }
		}
		else if (subgui instanceof SubGuiDropValueNbt) {
			if (((SubGuiDropValueNbt) subgui).tag.getPath().isEmpty() ||
					((SubGuiDropValueNbt) subgui).tag.getValues().length == 0) { drop.removeDropNbt(tag); }
			else { tag.load(((SubGuiDropValueNbt) subgui).tag.getNBT()); }
		}
		initGui();
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 0: drop.setChance(textField.getDouble()); break; // common chance
			case 1: {
				amount[0] = textField.getInteger();
				drop.setAmount(amount[0], amount[1]);
				break;
			} // amount min
			case 2: {
				amount[1] = textField.getInteger();
				drop.setAmount(amount[0], amount[1]);
				break;
			} // amount max
		}
	}

}
