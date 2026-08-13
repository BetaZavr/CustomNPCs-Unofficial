package noppes.npcs.client.gui.global;

import java.util.*;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.DimensionManager;
import noppes.npcs.client.gui.SubGuiNpcTransportCategoryEdit;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerNPCTransports;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.TransportCategory;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;

// Changed by Unofficial (BetaZavr)
public class GuiNpcManageTransporters extends GuiContainerNPCInterface2<ContainerNPCTransports>
		implements IGuiData, ICustomScrollListener, ITextfieldListener {

	public static EnumGuiType backToGui = EnumGuiType.MainMenuGlobal;
	protected final ContainerNPCTransports container;
	protected final Map<Component, TransportCategory> dataCat = new LinkedHashMap<>();
	protected final Map<Component, TransportLocation> dataLoc = new LinkedHashMap<>();
	protected GuiCustomScrollNop categories;
	protected GuiCustomScrollNop locations;

	public GuiNpcManageTransporters(EntityNPCInterface npc, ContainerNPCTransports containerIn) {
		super(npc, containerIn, Component.empty());
		setBackground("tradersetup.png");
		ySize = 200;

		backGui = npc == null ? EnumGuiType.MainMenuGlobal : backToGui;
		container = containerIn;
		Packets.sendServer(new SPacketTransportCategoriesGet());
	}

	@Override
	public void initGui() {
		super.initGui();
		if (categories == null) { categories = addScroll(0).setSize(100, 102); }
		int x = guiLeft + 5;
		int y = guiTop + 14;
		add(categories.setPos(x, y)
				.setUnsortedList(new ArrayList<>(dataCat.keySet())));
		for (Map.Entry<Component, TransportCategory> entry : dataCat.entrySet()) {
			if (entry.getValue().id == container.location.category.id) { categories.setSelected(entry.getKey()); }
		}

		addLabel(0, guiLeft + 5, y - 10, "gui.categories");
		y += categories.height + 24;
		addButton(0, x, y, "gui.add")
				.setSize(49, 16)
				.setHoverTexts("manager.hover.transport.add");

		addButton(1, x + 51, y, "gui.remove")
				.setSize(49, 16)
				.setIsEnabled(categories.hasSelected())
				.setHoverTexts(Component.translatable("manager.hover.transport.del", "\"" + categories.getSelected() + "\""));

		if (locations == null) { locations = addScroll(1).setSize(100, 102); }
		x += 102;
		y = guiTop + 14;
		add(locations.setPos(x, y)
				.setUnsortedList(new ArrayList<>(dataLoc.keySet())));
		for (Map.Entry<Component, TransportLocation> entry : dataLoc.entrySet()) {
			if (entry.getValue().id == container.location.id) { locations.setSelected(entry.getKey()); }
		}
		addLabel(1, guiLeft + 113, y - 10, "gui.location");
		y += locations.height + 24;
		addButton(2, x, y, "transporter.travel")
				.setSize(49, 16)
				.setIsEnabled(locations.hasSelected())
				.setHoverTexts("hover.teleport");
		addButton(4, x + 51, y, "gui.remove")
				.setSize(49, 16)
				.setIsEnabled(locations.hasSelected())
				.setHoverTexts(Component.translatable("manager.hover.location.del", "\"" + locations.getSelected() + "\""));

		if (categories.hasSelected()) {
			y = guiTop + 192;
			addLabel(2, guiLeft + 216, y - 11, Component.translatable("parameter.ikeysetting.catname").append(":"))
					.setSize(200, 10);
			addTextField(0, guiLeft + 214, y, 200, 18, container.location.category.title)
					.setHoverTexts("manager.hover.transport.cat.name");
			if (locations.hasSelected()) {
				x = guiLeft + 214;
				y = guiTop + 8;
				addLabel(3, x + 2, y, "market.barter")
						.setSize(53, 10);
				addLabel(4, x + 2, (y += 81) - 11, "market.currency")
						.setSize(53, 10);
				addTextField(1, x, y, 54, 18, container.location.money)
						.setMinMaxDefault(0, Integer.MAX_VALUE, (int) container.location.money)
						.setHoverTexts("manager.hover.transport.money");
				addLabel(5, x + 2, (y += 34) - 11, Component.translatable("parameter.ikeysetting.name").append(":"))
						.setSize(200, 10);
				addTextField(2, x, y, 200, 18, container.location.name)
						.setHoverTexts("manager.hover.transport.loc.name");
				addLabel(6, x + 2, (y += 34) - 11, Component.literal("UUID NPC").append(":"))
						.setSize(200, 10);
				addTextField(3, x, y, 200, 18, container.location.npc == null ? "" : container.location.npc.toString())
						.setHoverTexts("parameter.entity.uuid");
				x += 60;
				y = guiTop + 20;
				addLabel(7, x + 2, y - 12, Component.translatable("parameter.world").append(":"))
						.setSize(107, 10);
				addLabel(8, x + 2, y + 6, "ID:")
						.setSize(20, 10);
				addTextField(4, x + 17, y, 123, 18, container.location.dimension)
						.setResourceLocationType(1)
						.setHoverTexts("parameter.dimension.id");
				addLabel(9, x + 2, (y += 34) - 11, Component.translatable("parameter.position").append(" (XYZ):"))
						.setSize(137, 10);
				String idt = "ID:" + container.location.id;
				addLabel(10, guiLeft + xSize - font.getStringWidth(idt) - 4, guiTop + 8, idt)
						.setSize(30, 10);
				for (int i = 0; i < 3; i++) {
					int v = i == 0 ? container.location.pos.getX()
							: i == 1 ? container.location.pos.getY() : container.location.pos.getZ();
					addTextField(5 + i, x + 1 + i * 48, y, 44, 18, v)
							.setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, v)
							.setHoverTexts("parameter.pos" + (i == 0 ? "x" : i == 1 ? "y" : "z"));
				}
				y += 34;
				addLabel(11, x + 2, y - 11, Component.translatable("gui.type").append(":"));
				addButton(3, x, y, false, container.location.type, "transporter.discovered", "transporter.start", "transporter.interaction")
						.setSize(140, 20)
						.setHoverTexts("manager.hover.transport.type");
			}
		}
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		switch (button.id) {
			case 0: {
				setSubGui(new SubGuiEditText(0, Util.instance.deleteColor(Component.translatable("gui.new").getString())));
				break;
			} // add category
			case 1: {
				if (container.location.category != null) { Packets.sendServer(new SPacketTransportCategoryRemove(container.location.category.id)); }
				break;
			} // del category
			case 2: {
				transfer(container.location);
				break;
			} // tp
			case 3: {
				if (container.location != null) { container.location.type = button.getValue(); }
				break;
			} // location type
			case 4: {
				if (container.location != null) { Packets.sendServer(new SPacketTransportLocationRemove(container.location.id)); }
				break;
			} // del location
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		if (locations != null && locations.hasSelected()) {
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			mc.getTextureManager().bindTexture(GuiBasic.RESOURCE_SLOT);
			for (int slotId = 0; slotId < 9; ++slotId) {
				drawTexturedModalRect(guiLeft + container.getSlot(slotId).xPos - 1, guiTop + container.getSlot(slotId).yPos - 1,
						0, 0, 18, 18);
			}
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (categories != null && categories.hasSelected()) {
			drawHorizontalLine(guiLeft + 212, guiLeft + xSize - 3, guiTop + 178, 0x80000000);
			drawVerticalLine(guiLeft + 211, guiTop + 4, guiTop + ySize + 12, 0x80000000);
			if (locations.hasSelected()) {
				drawVerticalLine(guiLeft + 271, guiTop + 4, guiTop + 111, 0x80000000);
				drawHorizontalLine(guiLeft + 212, guiLeft + 270, guiTop + 110, 0x80000000);
			}
			drawVerticalLine(guiLeft + 418, guiTop + 4, guiTop + ySize + 12, 0x80000000);
		}
	}

	@Override
	public void save() {
		GuiTextFieldNop.unfocus();
		if (container.location.id > 0 && container.location.category != null) {
			Packets.sendServer(new SPacketTransportCategorySave(container.saveTransport(container.location.category)));
		}
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		switch (scroll.id) {
			case 0: {
				save();
				if (dataCat.containsKey(scroll.getNormalSelected())) {
					container.location = new TransportLocation();
					container.location.category = dataCat.get(scroll.getNormalSelected());
					container.resetStacks();
					setGuiData(null);
					initGui();
				}
				break;
			}
			case 1: {
				save();
				if (dataLoc.containsKey(scroll.getNormalSelected()) &&
						dataCat.containsKey(categories.getNormalSelected())) {
					container.location = dataLoc.get(scroll.getNormalSelected());
					container.resetStacks();
					setGuiData(null);
					initGui();
				}
				break;
			}
		}
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
		if (scroll.id == 0 && container.location.category != null) { setSubGui(new SubGuiNpcTransportCategoryEdit(npc, container.location.category)); }
		if (scroll.id == 1 && scroll.hasSelected()) { transfer(container.location); }
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		dataCat.clear();
		dataLoc.clear();
		for (TransportCategory category : TransportController.getInstance().getCategories()) {
			Component catKey = Component.empty()
					.append(Component.literal("ID: " + category.id + " \"").withStyle(TextFormatting.GRAY))
					.append(Component.translatable(category.title).withStyle(TextFormatting.RESET))
					.append(Component.literal("\"").withStyle(TextFormatting.GRAY));
			dataCat.put(catKey, category);
			if (category.id == container.location.category.id) {
				for (TransportLocation loc : new ArrayList<>(category.locations.values())) {
					Component locKey = Component.empty()
							.append(Component.literal("ID: " + loc.id + " \"").withStyle(TextFormatting.GRAY))
							.append(Component.translatable(loc.name).withStyle(TextFormatting.RESET))
							.append(Component.literal("\"").withStyle(TextFormatting.GRAY));
					dataLoc.put(locKey, loc);
				}
			}
		}
		initGui();
	}

	@Override
	public void subGuiClosed(GuiScreen subgui) {
		if (subgui instanceof SubGuiEditText && !((SubGuiEditText) subgui).text[0].isEmpty()) {
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("CategoryId", -1);
			compound.setString("CategoryTitle", ((SubGuiEditText) subgui).text[0]);
			Packets.sendServer(new SPacketTransportCategorySave(compound));
		}
	}

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		switch (textField.id) {
			case 0: {
				if (!textField.getValue().isEmpty()) {
					container.location.category.title = textField.getValue();
				}
				break;
			} // cat name
			case 1: {
				if (!textField.getValue().isEmpty()) {
					container.location.money = textField.getInteger();
				}
				break;
			} // money
			case 2: {
				if (!textField.getValue().isEmpty()) {
					Component sel = locations.getNormalSelected();
					container.location.name = textField.getValue();
					locations.replace(sel, Component.empty()
							.append(Component.literal("ID: " + container.location.id + " \"").withStyle(TextFormatting.GRAY))
							.append(Component.translatable(container.location.name).withStyle(TextFormatting.RESET))
							.append(Component.literal("\"").withStyle(TextFormatting.GRAY)));
				}
				break;
			} // loc name
			case 3: {
				if (!textField.getValue().isEmpty()) {
					try { container.location.npc = UUID.fromString(textField.getValue()); }
					catch (Exception e) { textField.setValue(container.location.npc == null ? "" : container.location.npc.toString()); }
				}
				break;
			} // npc uuid
			case 4: {
				if (!textField.getValue().isEmpty()) {
					int dimId = textField.getInteger();
					if (!DimensionManager.isDimensionRegistered(dimId)) { textField.setValue(container.location.dimension); }
					else { container.location.dimension = dimId; }
				}
				break;
			} // dim ID
			case 5: {
				if (!textField.getValue().isEmpty()) {
					int y = container.location.pos.getY();
					int z = container.location.pos.getZ();
					container.location.pos = new BlockPos(textField.getInteger(), y, z);
				}
				break;
			} // X
			case 6: {
				if (!textField.getValue().isEmpty()) {
					int x = container.location.pos.getX();
					int z = container.location.pos.getZ();
					container.location.pos = new BlockPos(x, textField.getInteger(), z);
				}
				break;
			} // Y
			case 7: {
				if (!textField.getValue().isEmpty()) {
					int x = container.location.pos.getX();
					int y = container.location.pos.getY();
					container.location.pos = new BlockPos(x, y, textField.getInteger());
				}
				break;
			} // Z
		}
	}

	private void transfer(TransportLocation loc) {
		if (loc != null) { Packets.sendServer(new SPacketTeleportTo(loc.dimension, loc.pos)); }
	}

}
