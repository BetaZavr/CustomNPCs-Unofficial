package noppes.npcs.client.gui.select;

import com.google.common.collect.Lists;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.TransportCategory;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.GuiSelectionListener;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

import java.util.HashMap;

public class SubGuiNpcTransportSelection extends GuiBasic implements ICustomScrollListener {

    protected final HashMap<String, TransportCategory> categoryData = new HashMap<>();
    protected final HashMap<String, TransportLocation> transportData = new HashMap<>();
    protected GuiCustomScrollNop scrollCategories;
    protected GuiCustomScrollNop scrollTransports;
    protected TransportCategory selectedCategory;
    protected GuiSelectionListener listener;
    public TransportLocation selectedTransport;

    public SubGuiNpcTransportSelection(int locationId) {
        super();
        setBackground("menubg.png");
        drawDefaultBackground = false;
        imageWidth = 366;
        imageHeight = 226;

        selectedTransport = TransportController.getInstance().getTransport(locationId);
        if (selectedTransport != null) { selectedCategory = selectedTransport.category; }
    }

    @Override
    public void initGui() {
        super.initGui();
        if (wrapper.parent instanceof GuiSelectionListener) { listener = (GuiSelectionListener) wrapper.parent; }
        addLabel(0, guiLeft + 8, guiTop + 4, "gui.categories");
        addLabel(1, guiLeft + 175, guiTop + 4, "quest.quests");
        addButton(2, guiLeft + imageWidth - 22, guiTop + 4, "X")
                .setSize(12, 12);
        categoryData.clear();
        for (TransportCategory category : TransportController.getInstance().getCategories()) { categoryData.put(category.title, category); }
        transportData.clear();
        if (selectedCategory != null) {
            for (TransportLocation location : selectedCategory.locations.values()) { transportData.put(location.name, location); }
        }
        if (scrollCategories == null) { scrollCategories = addScroll(0).setSize(170, 200); }
        scrollCategories.setList(Lists.newArrayList(categoryData.keySet()));
        if (selectedCategory != null) { scrollCategories.setSelected(selectedCategory.title); }
        add(scrollCategories.setPos(guiLeft + 4, guiTop + 14));

        if (scrollTransports == null) { scrollTransports = addScroll(1).setSize(170, 200); }
        scrollTransports.setList(Lists.newArrayList(transportData.keySet()));
        if (selectedTransport != null) { scrollTransports.setSelected(selectedTransport.name); }
        add(scrollTransports.setPos(guiLeft + 175, guiTop + 14));
    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {
        if (scroll.id == 0) {
            selectedCategory = categoryData.get(scrollCategories.getSelected());
            selectedTransport = null;
            scrollTransports.clearSelection();
        }
        if (scroll.id == 1) { selectedTransport = transportData.get(scrollTransports.getSelected()); }
        initGui();
    }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
        if (selectedTransport != null) {
            if (listener != null) { listener.selected(selectedTransport.id, selectedTransport.name); }
            onClose();
        }
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        if (button.id == 2) {
            if (selectedTransport != null) { scrollDoubleClicked(null); }
            else { onClose(); }
        }
    }

}
