package noppes.npcs.client.gui.global;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketPermissionsAdd;
import noppes.npcs.packets.server.SPacketPermissionsDel;
import noppes.npcs.packets.server.SPacketPermissionsGet;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.common.util.ComponentOrderComparator;
import noppes.npcs.util.Util;

import java.util.*;

public class GuiPermissionsEdit extends GuiNPCInterface implements ICustomScrollListener {

    public static final String UserNameRegex = "^[a-zA-Z][a-zA-Z0-9_]{3,15}$";
    protected final Map<Component, List<Component>> data = new HashMap<>();
    protected GuiCustomScrollNop permissions;
    protected GuiCustomScrollNop names;
    protected boolean wait = true;

    public GuiPermissionsEdit() {
        setBackground("menubg.png");
        imageWidth = 384;
        imageHeight = 217;

        Packets.sendServer(new SPacketPermissionsGet());
    }

    @Override
    public void initGui() {
        wait = false;
        super.initGui();
        int w = imageWidth / 2 - 6;
        CustomNpcsPermissions.putToData(data);
        List<Component> list = new ArrayList<>(data.keySet());
        list.sort(new ComponentOrderComparator());
        LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            String name = ((TextComponentTranslation) list.get(i).getContents()).getKey().substring(11);
            List<Component> hovers = new ArrayList<>();
            hovers.add(Component.empty()
                    .append(Component.translatable("gui.name").append(": ").withStyle(TextFormatting.GRAY))
                    .append(Component.literal(name).withStyle(TextFormatting.RESET)));
            hovers.add(Component.translatable("permission.hover."+name));
            hts.put(i, hovers);
        }
        if (permissions == null) { permissions = addScroll(0).setSize(w, imageHeight - 21); }
        permissions.setPos(guiLeft + 5, guiTop + 16)
                .setUnsortedList(list)
                .setHoverTexts(hts);
        add(permissions);
        int x = guiLeft + imageWidth / 2 + 1;
        if (names == null) { names = addScroll(1).setSize(w, imageHeight - 43); }
        names.setPos(x, guiTop + 16)
                .setHoverTexts("permission.hover.names");
        if (permissions.hasSelected() && data.containsKey(permissions.getNormalSelected())) { names.setNormalList(data.get(permissions.getNormalSelected())); }
        add(names);
        addLabel(0, permissions.getX() + 1, guiTop + 4, "permission.nodes")
                .setSize(w - 2, 12);
        addLabel(1, x, guiTop + 4, Component.translatable("playerdata.players").append(":"))
                .setSize(w - 14, 12);
        int y = guiTop + imageHeight - 25;
        addButton(0, names.getX(), y, "gui.add")
                .setSize(w / 2 - 1, 20)
                .setIsEnabled(permissions.hasSelected())
                .setHoverTexts("permission.hover.add");
        addButton(1, names.getX() + w / 2 + 1, y, "gui.remove")
                .setSize(w / 2 - 1, 20)
                .setIsEnabled(names.hasSelected())
                .setHoverTexts("permission.hover.del");
        addButton(66, guiLeft + imageWidth - 16, guiTop + 4, "X")
                .setSize(12, 12)
                .setHoverTexts("hover.exit");
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        switch (button.id) {
            case 0: {
                setSubGui(new SubGuiEditText(player.getName())
                        .setHoverTexts(Component.translatable("permission.hover.names")));
                break;
            }
            case 1: {
                if (!permissions.hasSelected() || !names.hasSelected() || !data.containsKey(permissions.getNormalSelected())) { return; }
                String node = ((TextComponentTranslation) permissions.getNormalSelected().getContents()).getKey().substring(11);
                Packets.sendServer(new SPacketPermissionsDel(names.getSelected(), node));
                wait = true;
                break;
            }
            case 66: onClose(); break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (wait) { drawWait(); }
        else { super.drawScreen(mouseX, mouseY, partialTicks); }
    }

    @Override
    public void subGuiClosed(GuiScreen subgui) {
        if (subgui instanceof SubGuiEditText && !((SubGuiEditText) subgui).cancelled && !((SubGuiEditText) subgui).text[0].isEmpty()) {
            SubGuiEditText gui = (SubGuiEditText) subgui;
            if (!permissions.hasSelected() || !data.containsKey(permissions.getNormalSelected())) { return; }
            String name = gui.text[0];
            if (name.equalsIgnoreCase("all")) { name = "All"; }
            else if (name.equalsIgnoreCase("command block")) { name = "Command Block"; }
            else if (!name.matches(UserNameRegex)) {
                String error = "§c" + Util.instance.translateGoogle(player, "Player name must be at least 4 characters long and must not contain spaces or characters other than _");
                if (error.contains("4")) { error = error.replace("4", "§64§c"); }
                if (error.contains("_")) { error = error.replace("_", "'§f_§c'"); }
                player.sendMessage(Component.literal(error).getParent());
                return;
            }
            String node = ((TextComponentTranslation) permissions.getNormalSelected().getContents()).getKey().substring(11);
            Packets.sendServer(new SPacketPermissionsAdd(name, node));
            wait = true;
        }
    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {
        if (scroll.id == 0) { names.setSelectedIndex(-1); }
        initGui();
    }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

}
