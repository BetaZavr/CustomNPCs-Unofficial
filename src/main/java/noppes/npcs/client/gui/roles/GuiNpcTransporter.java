package noppes.npcs.client.gui.roles;

import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.TransportCategory;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcTransportGet;
import noppes.npcs.packets.server.SPacketTransportCategoriesGet;
import noppes.npcs.packets.server.SPacketTransportSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

import javax.annotation.Nonnull;

public class GuiNpcTransporter extends GuiNPCInterface2
        implements IGuiData, ICustomScrollListener, ITextfieldListener {

   protected final Map<Component, TransportCategory> dataCat = new HashMap<>();
   protected @Nonnull TransportLocation location = new TransportLocation();
   protected GuiCustomScrollNop scroll;

   public GuiNpcTransporter(EntityNPCInterface npc) {
      super(npc);

      backGui = EnumGuiType.MainMenuAdvanced;
      Packets.sendServer(new SPacketTransportCategoriesGet());
      Packets.sendServer(new SPacketNpcTransportGet());
   }

   @Override
   public void init() {
      super.init();
      if (scroll == null) { scroll = addScroll(0).setSize(143, 196); }
      int x = guiLeft + 6;
      int y = guiTop + 16;
      List<Component> list = new ArrayList<>();
      LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
      int i = 0;
      Component select = scroll.getNormalSelected();
      for (Component line : dataCat.keySet()) {
         list.add(line);
         if (dataCat.get(line).locations.containsKey(location.id)) { select = line; }
         List<Component> hover = new ArrayList<>();
         TransportCategory cat = dataCat.get(line);
         if (cat != null && !cat.locations.isEmpty()) {
            hover.add(Component.translatable("gui.location", ":").withStyle(ChatFormatting.GRAY));
            Component p = Component.translatable("gui.position").append(": ").withStyle(ChatFormatting.GRAY);
            int j = 0;
            for (int id : cat.locations.keySet()) {
               if (j >= 5) {
                  hover.add(Component.literal("...").withStyle(ChatFormatting.GRAY));
                  break;
               }
               else {
                  TransportLocation loc = cat.locations.get(id);
                  hover.add(Component.empty()
                          .append(Component.literal(" ID: ").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal("" + id).withStyle(ChatFormatting.YELLOW))
                          .append(Component.literal(" \"").withStyle(ChatFormatting.GRAY))
                          .append(Component.translatable(loc.name).withStyle(ChatFormatting.RESET))
                          .append(Component.literal("\"; ").withStyle(ChatFormatting.GRAY))
                          .append(p)
                          .append(Component.literal("X: ").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal("" + loc.pos.getX()).withStyle(ChatFormatting.GOLD))
                          .append(Component.literal("; Y: ").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal("" + loc.pos.getY()).withStyle(ChatFormatting.GOLD))
                          .append(Component.literal("; Z: ").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal("" + loc.pos.getZ()).withStyle(ChatFormatting.GOLD))
                          .append(Component.literal("; Dimension: ").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal(loc.dimension.location().toString()).withStyle(ChatFormatting.BLUE)));
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
      int w = font.width("ID:") + 5;
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
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      if (button.id == 0) { location.type = button.getValue(); }
   }

   @Override
   public void save() {
      if (dataCat.containsKey(scroll.getNormalSelected())) {
         location.pos = player.blockPosition();
         location.dimension = player.getCommandSenderWorld().dimension();
         Packets.sendServer(new SPacketTransportSave(dataCat.get(scroll.getNormalSelected()).id, location.save()));
      }
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      if (compound.isEmpty()) {
         dataCat.clear();
         for (TransportCategory category : TransportController.getInstance().getCategories()) {
            Component catKey = Component.empty()
                    .append(Component.literal("ID: " + category.id + " \"").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable(category.title).withStyle(ChatFormatting.RESET))
                    .append(Component.literal("\"").withStyle(ChatFormatting.GRAY));
            dataCat.put(catKey, category);
         }
      }
      else {
         location = new TransportLocation();
         location.load(compound);
      }
      init();
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) { init(); }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      String name = textField.getValue();
      if (!name.isEmpty()) { location.name = name; }
   }

}
