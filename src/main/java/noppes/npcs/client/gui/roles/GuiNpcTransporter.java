package noppes.npcs.client.gui.roles;

import java.util.*;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.GuiNpcManageTransporters;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcTransportGet;
import noppes.npcs.packets.server.SPacketTransportCategoriesGet;
import noppes.npcs.packets.server.SPacketTransportLocationSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.TransportCategory;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;

import javax.annotation.Nonnull;

public class GuiNpcTransporter extends GuiNPCInterface2
		implements IGuiData, ICustomScrollListener, ITextfieldListener {

	protected final Map<Component, TransportCategory> dataCat = new HashMap<>();
	protected @Nonnull TransportLocation location = new TransportLocation();
	protected TransportCategory selectedCategory = null;
	protected GuiCustomScrollNop scroll;

	public GuiNpcTransporter(EntityNPCInterface npc) {
		super(npc);
		backGui = EnumGuiType.MainMenuAdvanced;

		Packets.sendServer(new SPacketTransportCategoriesGet());
	}

	@Override
	public void initGui() {
		super.initGui();
		if (scroll == null) { scroll = addScroll(0).setSize(143, 196); }
		int x = guiLeft + 6;
		int y = guiTop + 16;
		List<Component> list = new ArrayList<>();
		LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
		int i = 0;
		// Determine which category should be selected
		Component select = Component.empty();
		for (Component line : dataCat.keySet()) {
			list.add(line);
			TransportCategory cat = dataCat.get(line);
			// Prefer the user's explicit selection
			if (selectedCategory != null && cat.id == selectedCategory.id) {
				select = line;
			}
			// Fallback to the location's assigned category
			else if (select.getString().isEmpty() && location.category != null && cat.id == location.category.id) {
				select = line;
			}
			List<Component> hover = new ArrayList<>();
			if (cat != null && !cat.locations.isEmpty()) {
				hover.add(Component.translatable("gui.location", ":").withStyle(TextFormatting.GRAY));
				Component p = Component.translatable("gui.position").append(": ").withStyle(TextFormatting.GRAY);
				int j = 0;
				for (TransportLocation loc : cat.locations.values()) {
					if (j >= 5) {
						hover.add(Component.literal("...").withStyle(TextFormatting.GRAY));
						break;
					}
					else {
						hover.add(Component.empty()
								.append(Component.literal(" ID: ").withStyle(TextFormatting.GRAY))
								.append(Component.literal("" + loc.id).withStyle(TextFormatting.YELLOW))
								.append(Component.literal(" \"").withStyle(TextFormatting.GRAY))
								.append(Component.translatable(loc.name).withStyle(TextFormatting.RESET))
								.append(Component.literal("\"; ").withStyle(TextFormatting.GRAY))
								.append(p)
								.append(Component.literal("X: ").withStyle(TextFormatting.GRAY))
								.append(Component.literal("" + loc.pos.getX()).withStyle(TextFormatting.GOLD))
								.append(Component.literal("; Y: ").withStyle(TextFormatting.GRAY))
								.append(Component.literal("" + loc.pos.getY()).withStyle(TextFormatting.GOLD))
								.append(Component.literal("; Z: ").withStyle(TextFormatting.GRAY))
								.append(Component.literal("" + loc.pos.getZ()).withStyle(TextFormatting.GOLD))
								.append(Component.literal("; Dimension ID: ").withStyle(TextFormatting.GRAY))
								.append(Component.literal("" + loc.dimension).withStyle(TextFormatting.BLUE)));
						j++;
					}
				}
			}
			hts.put(i++, hover);
		}
		add(scroll.setPos(x, y)
				.setUnsortedList(list)
				.setHoverTexts(hts)
				.setSelected(select));
		addLabel(0, x + 2, y - 11, Component.translatable("gui.categories").append(":"));
		x += 147;
		addLabel(1, x, y - 11, Component.translatable("gui.name").append(":"))
				.setSize(200, 20)
				.setIsVisible(scroll.hasSelected());
		int w = font.getStringWidth("ID:") + 5;
		addLabel(2, x + 200 - w, y - 11, "ID:" + location.id)
				.setSize(w + 2, 20)
				.setIsVisible(scroll.hasSelected());
		addTextField(0, x, y, 200, 20, location.name)
				.setIsVisible(scroll.hasSelected())
				.setHoverTexts("manager.hover.transport.loc.name");
		addButton(0, x, y + 24, false, location.type, "transporter.discovered", "transporter.start", "transporter.interaction")
				.setSize(200, 20)
				.setIsVisible(scroll.hasSelected())
				.setHoverTexts(Component.translatable("manager.hover.transport.type")
						.append(Component.translatable("manager.hover.transport.addinfo")));
		// Settings button to open GuiNpcManageTransporters
		addButton(1, x, y + 48, "gui.settings")
				.setSize(200, 20)
				.setIsVisible(scroll.hasSelected())
				.setHoverTexts("manager.hover.transport.settings");
	}

	@Override
	public void buttonEvent(GuiButtonNop button) {
		if (button.id == 0) { location.type = button.getValue(); }
		else if (button.id == 1) {
			// Save current location first, then open the management GUI
			save();
			TransportCategory cat = selectedCategory != null ? selectedCategory : location.category;
			if (cat != null) {
				GuiNpcManageTransporters.backToGui = EnumGuiType.MainMenuAdvanced;
				NoppesUtil.requestOpenGUI(EnumGuiType.ManageTransport, new BlockPos(-1, cat.id, location.id));
			}
		}
	}

	@Override
	public void save() {
		TransportCategory cat = selectedCategory != null ? selectedCategory : location.category;
		if (cat != null) {
			location.pos = player.getPosition();
			location.dimension = player.world.provider.getDimension();
			Packets.sendServer(new SPacketTransportLocationSave(cat.id, location.save()));
		}
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		if (compound.hasNoTags()) {
			dataCat.clear();
			for (TransportCategory category : TransportController.getInstance().getCategories()) {
				Component catKey = Component.empty()
						.append(Component.literal("ID: " + category.id + " \"").withStyle(TextFormatting.GRAY))
						.append(Component.translatable(category.title).withStyle(TextFormatting.RESET))
						.append(Component.literal("\"").withStyle(TextFormatting.GRAY));
				dataCat.put(catKey, category);
			}
			Packets.sendServer(new SPacketNpcTransportGet());
		} // from SPacketTransportCategoriesGet
		else {
			location = new TransportLocation();
			location.load(compound);
			// Restore the category reference from the controller
			for (TransportCategory cat : TransportController.getInstance().getCategories()) {
				if (cat.locations.containsKey(location.id)) {
					location.category = cat;
					selectedCategory = cat;
					break;
				}
			}
		} // from SPacketNpcTransportGet
		initGui();
	}

	@Override
	public void scrollClicked(GuiCustomScrollNop scroll) {
		if (dataCat.containsKey(scroll.getNormalSelected())) {
			selectedCategory = dataCat.get(scroll.getNormalSelected());
			location.category = selectedCategory;
			initGui();
		}
	}

	@Override
	public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

	@Override
	public void unFocused(GuiTextFieldNop textField) {
		String name = textField.getValue();
		if (!name.isEmpty()) { location.name = name; }
	}

}