package noppes.npcs.client.gui.global;

import java.text.SimpleDateFormat;
import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.select.SubGuiDialogSelection;
import noppes.npcs.client.gui.select.SubGuiNpcFactionSelection;
import noppes.npcs.client.gui.select.SubGuiNpcTransportSelection;
import noppes.npcs.client.gui.select.SubGuiQuestSelection;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPlayerData;
import noppes.npcs.containers.ContainerNPCBank;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.*;
import noppes.npcs.shared.common.util.ComponentOrderComparator;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

public class GuiNpcManagePlayerData extends GuiNPCInterface2
        implements IScrollData, ICustomScrollListener, IGuiData, ITextfieldListener, GuiSelectionListener {

   protected static final SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss dd MMM yyyy (EEE)");

   protected final Map<Component, Integer> data = new HashMap<>();
   protected HashMap<Component, Component> scrollData = new HashMap<>();
   protected CompoundTag gameData = new CompoundTag();
   protected GuiCustomScrollNop scroll;
   protected boolean isOnline = false;
   protected int totalPlayers = 0;
   protected Component selected = Component.empty();
   public EnumPlayerData selection = EnumPlayerData.Players;
   public Component selectedPlayer = Component.empty();

   public GuiNpcManagePlayerData(EntityNPCInterface npc) {
      super(npc);
      backGui = EnumGuiType.MainMenuGlobal;
      Packets.sendServer(new SPacketPlayerDataGet(selection, selectedPlayer.getString()));
   }

   @Override
   public void init() {
      super.init();
      if (scroll == null) { scroll = addScroll(0).setSize(301, 174); }
      add(scroll.setPos(guiLeft + 7, guiTop + 16));
      selected = Component.empty();
      addLabel(0, guiLeft + 10, guiTop + 6, Component.translatable("data.all.players").append(":"))
              .setSize(296, 10);
      int x = guiLeft + 313;
      int y = guiTop + 16;
      int w = 99;
      // main buttons
      addButton(1, x, y, "playerdata.players")
              .setSize(w, 20)
              .setHoverTexts("data.hover.list");
      addButton(2, x, (y += 22), "quest.quest")
              .setSize(w, 20)
              .setHoverTexts("data.hover.quests");
      addButton(3, x, (y += 22), "dialog.dialog")
              .setSize(w, 20)
              .setHoverTexts("data.hover.dialogs");
      addButton(4, x, (y += 22), "global.transport")
              .setSize(w, 20)
              .setHoverTexts("data.hover.transports");
      addButton(5, x, (y += 22), "global.banks")
              .setSize(w, 20)
              .setHoverTexts("data.hover.banks");
      addButton(6, x, (y += 22), "menu.factions")
              .setSize(w, 20)
              .setHoverTexts("data.hover.factions");
      addButton(9, x, (y += 22), "gui.game")
              .setSize(w, 20)
              .setHoverTexts("data.hover.game");
      addButton(7, x, (y += 22), "gui.wipe")
              .setSize(w, 20)
              .setHoverTexts("data.hover.wipe");
      addButton(12, x, y + 22, "gui.cleaning")
              .setSize(w, 20)
              .setHoverTexts("data.hover.cleaning");
      // edit data buttons
      y = guiTop + 170;
      x = guiLeft + 7;
      w = 73;
      addButton(8, x, (y += 22), "gui.add")
              .setSize(w, 20)
              .setHoverTexts("hover.add");
      addButton(0, (x += w + 3), y, "gui.remove")
              .setSize(w, 20)
              .setHoverTexts("hover.delete");
      addButton(10, (x += w + 3), y, "gui.remove.all")
              .setSize(w, 20)
              .setHoverTexts("hover.delete.all");
      addButton(11, x + w + 3, y, "selectServer.edit")
              .setSize(w, 20)
              .setHoverTexts("hover.edit");
      initButtons();
      if (selection == EnumPlayerData.Game) {
         y = guiTop + 18;
         addLabel(2, guiLeft + 10, y + 5, "gui.money");
         addTextField(1, guiLeft + 66, y, 120, 20, "" + gameData.getLong("Money"))
                 .setMinMaxDefault(0, Long.MAX_VALUE, gameData.getLong("Money"))
                 .setHoverTexts("data.hover.money", "" + Long.MAX_VALUE);
         addLabel(3, guiLeft + 10, y + 25, Component.translatable("global.market").append(":"))
                 .setHoverTexts("data.hover.markets");
      }
   }

   public void initButtons() {
      boolean hasPlayer = selectedPlayer != null && !selectedPlayer.getString().isEmpty();
      getButton(0).setIsVisible(true)
              .setIsEnabled(hasPlayer && scroll.hasSelected()); // remove
      getButton(1).setIsEnabled(selection != EnumPlayerData.Players && hasPlayer);
      getButton(2).setIsEnabled(selection != EnumPlayerData.Quest && hasPlayer);
      getButton(3).setIsEnabled(selection != EnumPlayerData.Dialog && hasPlayer);
      getButton(4).setIsEnabled(selection != EnumPlayerData.Transport && hasPlayer);
      getButton(5).setIsEnabled(selection != EnumPlayerData.Bank && hasPlayer);
      getButton(6).setIsEnabled(selection != EnumPlayerData.Factions && hasPlayer);
      getButton(9).setIsEnabled(selection != EnumPlayerData.Game && hasPlayer);
      boolean canEdit = selection != EnumPlayerData.Players && selection != EnumPlayerData.Wipe;
      getButton(8).setIsVisible(true); // add
      getButton(10).setIsVisible(true); // remove.all
      getButton(11).setIsVisible(true); // selectServer.edit"
      getButton(12).setIsEnabled(selection == EnumPlayerData.Players);
      if (scroll != null) {
         if (selection != EnumPlayerData.Game) {
            scroll.setPos(guiLeft + 7, guiTop + 16)
                    .setSize(301, 174);
         }
         else {
            scroll.setPos(guiLeft + 7, guiTop + 52)
                    .setSize(120, 138);
         }
      }
      if (getLabel(2) != null) { getLabel(2).setIsEnabled(selection == EnumPlayerData.Game); }
      if (getLabel(3) != null) { getLabel(3).setIsEnabled(selection == EnumPlayerData.Game); }
      if (getTextField(1) != null) { getTextField(1).setIsVisible(selection == EnumPlayerData.Game); }
      switch (selection) {
         case Quest:
         case Transport: {
            getButton(8).setIsEnabled(canEdit && hasPlayer);
            getButton(10).setIsEnabled(canEdit && hasPlayer && !scroll.getList().isEmpty());
            getButton(11).setIsEnabled(canEdit && hasPlayer && scroll.hasSelected());
            break;
         }
         case Dialog:  {
            getButton(8).setIsEnabled(canEdit && hasPlayer);
            getButton(10).setIsEnabled(canEdit && hasPlayer && !scroll.getList().isEmpty());
            getButton(11).setIsEnabled(false);
            break;
         }
         case Bank: {
            getButton(8).setIsEnabled(canEdit && hasPlayer && data.size() < BankController.getInstance().getBanks().size());
            getButton(10).setIsEnabled(canEdit && hasPlayer && !scroll.getList().isEmpty());
            getButton(11).setIsEnabled(canEdit && hasPlayer && scroll != null && scroll.hasSelected());
            break;
         }
         case Factions: {
            getButton(8).setIsEnabled(canEdit && hasPlayer && data.size() < FactionController.instance.factions.size());
            getButton(10).setIsEnabled(canEdit && hasPlayer && !scroll.getList().isEmpty());
            getButton(11).setIsEnabled(canEdit && hasPlayer && scroll != null && scroll.hasSelected());
            break;
         }
         case Game: {
            getButton(0).setIsEnabled(canEdit && hasPlayer && scroll.hasSelected());
            getButton(8).setIsVisible(false);
            getButton(10).setIsEnabled(canEdit && hasPlayer && gameData != null);
            getButton(11).setIsEnabled(canEdit && hasPlayer && scroll.hasSelected());
            break;
         }
         default: {
            getButton(8).setIsVisible(false);
            getButton(10).setIsVisible(false);
            getButton(11).setIsVisible(false);
         }
      }
      if (!hasPlayer) {
         getLabel(0).setMessage(Component.translatable("data.all.players")
                 .append(" (")
                 .append(scroll.getList() == null ? "1" : "" + scroll.getList().size())
                 .append(")")
         );
      }
      else {
         if (selection == EnumPlayerData.Players) { totalPlayers = scroll.getList() == null ? 1 : scroll.getList().size(); }
         getLabel(0).setMessage(Component.translatable("data.sel.player")
                 .append(" (" + totalPlayers + "): ")
                 .append(Component.literal(selectedPlayer.getString()).withStyle(isOnline ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED).withStyle(ChatFormatting.BOLD)));
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      MutableComponent title = switch (selection) {
         case Quest -> Component.translatable("quest.quest");
         case Dialog -> Component.translatable("dialog.dialog");
         case Transport -> Component.translatable("global.transport");
         case Bank -> Component.translatable("global.banks");
         case Factions -> Component.translatable("menu.factions");
         case Game -> Component.translatable("gui.game");
         default -> (MutableComponent) selectedPlayer;
      };
      if (button.id == 0) {
         if (selection == EnumPlayerData.Players || !scroll.hasSelected()) {
            ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
               if (bo && data.containsKey(selected)) {
                  Packets.sendServer(new SPacketPlayerDataRemove(selection, selectedPlayer.getString(), data.get(selected)));
                  data.clear();
                  selected = Component.empty();
                  selectedPlayer = Component.empty();
                  scroll.setSelected(-1);
                  initButtons();
               }
               NoppesUtil.openGUI(player, this);
            },
                    Component.empty()
                            .append(Component.translatable("global.playerdata").append(": ").withStyle(ChatFormatting.GRAY))
                            .append(title.withStyle(ChatFormatting.RESET)),
                    Component.translatable("message.delete"));
            setScreen(guiYesNo);
         }
         else if (data.containsKey(scrollData.get(scroll.getNormalSelected()))) {
            Packets.sendServer(new SPacketPlayerDataRemove(selection, selectedPlayer.getString(), data.get(scrollData.get(scroll.getNormalSelected()))));
         }
      } // del
      else if (button.id >= 1 && button.id <= 6 || button.id == 9) {
         if (selectedPlayer.getString().isEmpty() && button.id != 1) { return; }
         if (selection == EnumPlayerData.Game) { save(); }
         if (button.id == 9) { selection = EnumPlayerData.Game; }
         else { selection = EnumPlayerData.values()[button.id - 1]; }
         scroll.clear();
         data.clear();
         selected = Component.empty();
         initButtons();
         Packets.sendServer(new SPacketPlayerDataGet(selection, selectedPlayer.getString()));
      }
      else if (button.id == 7) {
         String mes = Component.translatable("data.hover.wipe").getString().replace("<br>", "" + (char) 10);
         ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
            if (bo) {
               selection = EnumPlayerData.Wipe;
               scroll.clear();
               Packets.sendServer(new SPacketPlayerDataRemove(selection, "1noppes", 0));
               data.clear();
               selected = Component.empty();
               selectedPlayer = Component.empty();
               scroll.setSelected(-1);
               selection = EnumPlayerData.Players;
               initButtons();
            }
            NoppesUtil.openGUI(player, this);
         },
                 Component.translatable("gui.wipe").append("?"),
                 Component.literal(mes));
         setScreen(guiYesNo);
      } // wipe
      else if (button.id == 8) {
         switch (selection) {
            case Quest: setSubGui(new SubGuiQuestSelection(-1)); return;
            case Dialog: setSubGui(new SubGuiDialogSelection(-1)); return;
            case Transport: setSubGui(new SubGuiNpcTransportSelection(-1)); return;
            case Factions: setSubGui(new SubGuiNpcFactionSelection(this, -1)); return;
            case Bank: {
               List<Component> hovers = new ArrayList<>();
               hovers.add(Component.translatable("gui.options").append(" ID:"));
               StringBuilder ids = new StringBuilder();
               for (Bank bank : BankController.getInstance().getBanks()) {
                  if (data.containsValue(bank.id)) { continue; }
                  if (!ids.isEmpty()) { ids.append(", "); }
                  ids.append(bank.id);
               }
               hovers.add(Component.literal(ids.toString()).withStyle(ChatFormatting.GOLD));
               SubGuiEditText subgui = new SubGuiEditText(0, "");
               subgui.label = "gui.add";
               subgui.hovers.put(0, hovers);
               setSubGui(subgui);
               break;
            }
         }
      } // Add
      else if (button.id == 10) {
         ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
            if (bo) {
               Packets.sendServer(new SPacketPlayerDataRemove(selection, selectedPlayer.getString(), -1));
            }
            NoppesUtil.openGUI(player, this);
         },
                 Component.translatable("global.playerdata").append(": ").append(title),
                 Component.translatable("message.delete"));
         setScreen(guiYesNo);
      } // Del all data
      else if (button.id == 11) { editData(); } // edit
      else if (button.id == 12) {
         String mes = Component.translatable("data.hover.cleaning").getString().replace("<br>", "" + (char) 10);
         ConfirmScreen guiYesNo = new ConfirmScreen((bo) -> {
            if (bo) {
               SubGuiDataSend subgui = new SubGuiDataSend();
               setSubGui(subgui);
            }
            NoppesUtil.openGUI(player, this);
         },
                 Component.translatable("gui.cleaning").append("?"),
                 Component.literal(mes));
         setScreen(guiYesNo);
      } // cleaning
   }

   @Override
   public void save() {
      ContainerNPCBank.editPlayerBankData = null;
      if (selection == EnumPlayerData.Game) {
         boolean hasPlayer = selectedPlayer != null && !selectedPlayer.getString().isEmpty();
         if (hasPlayer && gameData != null) {
            Packets.sendServer(new SPacketPlayerDataSet(EnumPlayerData.Game, selectedPlayer.getString(), 0, gameData));
         }
      }
   }

   @Override
   public void setData(Vector<String> dataList, Map<String, Integer> dataMap) {
      if (selection == EnumPlayerData.Game) { return; }
      data.clear();

      data.putAll(Util.instance.convertStringMap(dataMap));
      setCurrentList();
      if (selection == EnumPlayerData.Players && selectedPlayer != null) {
         scroll.setSelected(selectedPlayer);
         selected = selectedPlayer;
      }
      if (selection == EnumPlayerData.Wipe) { selection = EnumPlayerData.Players; }
      initButtons();
      if (ContainerNPCBank.editPlayerBankData != null) {
         buttonEvent(new GuiButtonNop(this, 5, "", 0, 0, null));
         ContainerNPCBank.editPlayerBankData = null;
      }
   }

   @Override
   public void setSelected(String selected) { }

   @Override
   public void setGuiData(CompoundTag compound) {
      if (selection != EnumPlayerData.Game || !compound.contains("GameData", 10)) { return; }
      gameData = compound;
      Map<Integer, Integer> map = new TreeMap<>();
      for (int i = 0; i < compound.getCompound("GameData").getList("MarketData", 10).size(); i++) {
         CompoundTag nbt = compound.getCompound("GameData").getList("MarketData", 10).getCompound(i);
         map.put(nbt.getInt("MarketID"), nbt.getInt("Slot"));
      }
      List<Component> list = new ArrayList<>();
      MarcetController mData = MarcetController.getInstance();
      data.clear();
      int i = 0;
      LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
      for (int id : map.keySet()) {
         Component key = Component.empty()
                 .append(Component.literal("ID:" + id + " ").withStyle(ChatFormatting.GRAY))
                 .append(Component.translatable("bank.slot").append(": ").withStyle(ChatFormatting.GRAY))
                 .append(Component.literal("" + map.get(id)).withStyle(ChatFormatting.RESET));
         list.add(key);
         data.put(key, id);
         Marcet m = mData.getMarcet(id);
         List<Component> hList = new ArrayList<>();
         if (m != null) {
            hList.add(Component.literal("ID:" + id + " \"" + m.getName() + "\""));
            hList.add(Component.translatable("gui.max").append(": " + (m.markup.size() - 1)));
         } else {
            hList.add(Component.translatable("global.market").append(" - ").append(Component.translatable("quest.notfound")));
         }
         hts.put(i, hList);
         i++;
      }
      scroll.setUnsortedList(list).setHoverTexts(hts);
      init();
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (!selected.getString().equals(scroll.getSelected())) {
         selected = scroll.getNormalSelected();
         if (selection == EnumPlayerData.Players) {
            selectedPlayer = selected;
            isOnline = data.get(selected) == 1;
         }
         initButtons();
      }
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { editData(); }

   @Override
   public void subGuiClosed(Screen subgui) {
      if (subgui instanceof SubGuiEditText gui) {
         if (gui.id == 0) {
            try { Packets.sendServer(new SPacketPlayerDataSet(selection, selectedPlayer.getString(), Integer.parseInt(gui.text[0]), null)); } catch (Exception ignored) { }
         } // add
         else if (gui.id == 1) {
            try {
               CompoundTag nbt = new CompoundTag();
               nbt.putInt("value", Integer.parseInt(gui.text[0]));
               Packets.sendServer(new SPacketPlayerDataSet(selection, selectedPlayer.getString(), data.get(scrollData.get(scroll.getNormalSelected())), nbt));
            } catch (Exception e) { LogWriter.error(e); }
         } // set
         else if (gui.id == 2) {
            if (gameData == null || !data.containsKey(scroll.getNormalSelected())) {
               return;
            }
            int id = data.get(scroll.getNormalSelected());
            for (int i = 0; i < gameData.getCompound("GameData").getList("MarketData", 10).size(); i++) {
               CompoundTag nbt = gameData.getCompound("GameData").getList("MarketData", 10).getCompound(i);
               if (id != nbt.getInt("MarketID")) { continue; }
               nbt.putInt("Slot", gui.getTextField(0).getInteger());
               break;
            }
            setGuiData(gameData);
         } // change market slot
         return;
      }
      if (subgui instanceof SubGuiDataSend gui) { Packets.sendServer(new SPacketPlayerDataCleaning(gui.time)); }
   }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      if (hasSubGui() && wrapper.subgui instanceof SubGuiDataSend gui) {
         gui.unFocused(textField);
         return;
      }
      if (textField.id != 1 || gameData == null || !textField.isLong()) { return; }
      gameData.getCompound("GameData").putLong("Money", textField.getLong());
   }

   @Override
   public void selected(int id, String name) {
      if (!data.containsValue(id)) {
         data.put(Component.empty(), id);
         Packets.sendServer(new SPacketPlayerDataSet(selection, selectedPlayer.getString(), id, null));
      }
   }

   // New from Unofficial (BetaZavr)
   private void editData() {
      if (!scroll.hasSelected()) { return; }
      switch (selection) {
         case Quest: {
            Packets.sendServer(new SPacketPlayerDataSet(selection, selectedPlayer.getString(), -1 * data.get(scrollData.get(scroll.getNormalSelected())), null));
            break;
         }
         case Bank: {
            ContainerNPCBank.editPlayerBankData = selectedPlayer.getString();
            Packets.sendServer(new SPacketBankOpenPlayer(data.get(scrollData.get(scroll.getNormalSelected())), selectedPlayer.getString()));
            break;
         }
         case Factions: {
            int factionId = data.get(scrollData.get(scroll.getNormalSelected()));
            SubGuiEditText subgui = new SubGuiEditText(1, "");
            Faction f = FactionController.instance.factions.get(factionId);
            String v = Util.instance.deleteColor(scroll.getHoversTexts().get(scroll.getSelectedIndex()).get(1).getString());
            int value = -1;
            try { value = Integer.parseInt(v.substring(v.lastIndexOf(" ") + 1)); } catch (Exception e) { LogWriter.error(e); }
            if (f != null) { subgui.numbersOnly = new int[] { 0, f.friendlyPoints * 2, value }; }
            else { subgui.numbersOnly = new int[] { 0, Integer.MAX_VALUE, value }; }
            subgui.text[0] = "" + value;
            subgui.label = "gui.set.new.value";
            setSubGui(subgui);
            break;
         }
         case Game: {
            if (gameData == null || !data.containsKey(scroll.getNormalSelected())) { return; }
            SubGuiEditText subgui = new SubGuiEditText(2, "");
            subgui.init();
            subgui.getTextField(0).setMinMaxDefault(Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
            int m = 3;
            int s = 0;
            int id = data.get(scroll.getNormalSelected());
            MarcetController mData = MarcetController.getInstance();
            for (int i = 0; i < gameData.getCompound("GameData").getList("MarketData", 10).size(); i++) {
               CompoundTag nbt = gameData.getCompound("GameData").getList("MarketData", 10).getCompound(i);
               if (id != nbt.getInt("MarketID")) { continue; }
               s = nbt.getInt("Slot");
               Marcet marcet = mData.getMarcet(id);
               if (marcet == null) { break; }
               m = marcet.markup.size() - 1;
               break;
            }
            subgui.text[0] = "" + s;
            subgui.getTextField(0).setMinMaxDefault(0, m, s);
            subgui.label = "gui.set.new.value";
            setSubGui(subgui);
         }
      }
   }

   private void setCurrentList() {
      if (scroll == null) { return; }
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      List<Component> list = new ArrayList<>();
      List<Component> hovers = new ArrayList<>();
      List<Component> suffixes = new ArrayList<>();
      if (selection == EnumPlayerData.Wipe) { selection = EnumPlayerData.Players; }
      String search = scroll.getSearchValue();
      switch (selection) {
         case Players: {
            List<Component> listOn = new ArrayList<>();
            List<Component> listOff = new ArrayList<>();
            for (Component name : data.keySet()) {
               if (search.isEmpty() || name.getString().toLowerCase().contains(search)) {
                  if (data.get(name) == 1) { listOn.add(name); }
                  else { listOff.add(name); }
               }
            }
            listOn.sort(new ComponentOrderComparator());
            listOff.sort(new ComponentOrderComparator());
            list = listOn;
            list.addAll(listOff);
            for (Component name : list) {
               suffixes.add(Component.translatable(data.get(name) == 0 ? "gui.offline" : "gui.online"));
            }
            break;
         }
         case Quest: {
            Map<String, Map<Integer, String>> mapA = new TreeMap<>();
            Map<String, Map<Integer, String>> mapF = new TreeMap<>();
            Map<String, Component> mapH = new LinkedHashMap<>();
            for (Component str : data.keySet()) {
               String line = Util.instance.getOldFormattedText(str);
               String cat = line.substring(0, line.indexOf(": "));
               String name = line.substring(line.indexOf(": ") + 2);
               Map<String, Map<Integer, String>> map;
               MutableComponent hover = Component.empty()
                       .append(Component.literal("ID: ").withStyle(ChatFormatting.GRAY))
                       .append(Component.literal("" + data.get(str)).withStyle(ChatFormatting.GOLD));
               String h = "A";
               if (name.endsWith("(Active quest)")) {
                  name = name.substring(0, name.lastIndexOf("(Active quest)"));
                  map = mapA;
               }
               else {
                  h = "F";
                  name = name.substring(0, name.lastIndexOf("(Finished quest)"));
                  if (name.contains("(") && name.contains(")") && name.lastIndexOf(")") > name.lastIndexOf("(") && minecraft.level != null) {
                     try {
                        String l = name.substring(name.lastIndexOf("(") + 1, name.lastIndexOf(")"));
                        long v = Long.parseLong(l);
                        name = name.substring(0, name.lastIndexOf("("));
                        long time = v;
                        if (v < 1321603200000L) { time = System.currentTimeMillis() - (PlayerData.get(player).questData.overworldTime - v) * 50L; }
                        long ago = (System.currentTimeMillis() - time) / 50L;
                        hover.append("<br>")
                                .append(Component.translatable("availability.completed").append(": ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(formatter.format(new Date(time))))
                                .append("<br>")
                                .append(Component.translatable("mailbox.timesend", Util.instance.ticksToElapsedTime(ago, false, false, false)).withStyle(ChatFormatting.GRAY));
                     } catch (Exception ignored) { }
                  }
                  map = mapF;
               }
               if (!map.containsKey(cat)) { map.put(cat, new TreeMap<>()); }
               map.get(cat).put(data.get(str), name);
               mapH.put(h + cat + name, hover);
            }
            for (String cat : mapA.keySet()) {
               Component sfx = Component.translatable("availability.active").withStyle(ChatFormatting.GREEN);
               for (int id : mapA.get(cat).keySet()) {
                  suffixes.add(sfx);
                  Component key = Component.empty()
                          .append(Component.literal("ID:" + id + " ").withStyle(ChatFormatting.DARK_GRAY))
                          .append(Component.literal(cat + ": \"").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal(mapA.get(cat).get(id)))
                          .append(Component.literal("\"").withStyle(ChatFormatting.GRAY));
                  list.add(key);
                  hovers.add(mapH.get("A" + cat + mapA.get(cat).get(id)));
                  for (Component str : data.keySet()) {
                     if (data.get(str) == id) {
                        scrollData.put(key, str);
                        break;
                     }
                  }
               }
            }
            for (String cat : mapF.keySet()) {
               Component sfx = Component.translatable("quest.complete").withStyle(ChatFormatting.LIGHT_PURPLE);
               for (int id : mapF.get(cat).keySet()) {
                  suffixes.add(sfx);
                  Component key = Component.empty()
                          .append(Component.literal("ID:" + id + " ").withStyle(ChatFormatting.DARK_GRAY))
                          .append(Component.literal(cat + ": \"").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal(mapF.get(cat).get(id)))
                          .append(Component.literal("\"").withStyle(ChatFormatting.GRAY));
                  list.add(key);
                  hovers.add(mapH.get("F" + cat + mapF.get(cat).get(id)));
                  for (Component str : data.keySet()) {
                     if (data.get(str) == id) {
                        scrollData.put(key, str);
                        break;
                     }
                  }
               }
            }
            break;
         }
         case Dialog: {
            Map<Integer, Component> map = new TreeMap<>();
            for (Component str : data.keySet()) {
               String line = Util.instance.getOldFormattedText(str);
               String cat = line.substring(0, line.indexOf(": "));
               String name = line.substring(line.indexOf(": ") + 2);
               map.put(data.get(str), Component.empty()
                       .append(Component.literal("ID:" + data.get(str) + " ").withStyle(ChatFormatting.DARK_GRAY))
                       .append(Component.literal(cat + ": ").withStyle(ChatFormatting.GRAY))
                       .append(Component.literal(name).withStyle(ChatFormatting.RESET)));
               hovers.add(Component.empty()
                       .append(Component.literal("ID: ").withStyle(ChatFormatting.GRAY))
                       .append(Component.literal("" + data.get(str)).withStyle(ChatFormatting.GOLD)));
            }
            for (int id : map.keySet()) {
               list.add(map.get(id));
               for (Component str : data.keySet()) {
                  if (data.get(str) == id) {
                     scrollData.put(map.get(id), str);
                     break;
                  }
               }
            }
            break;
         }
         case Transport: {
            Map<Integer, Component> map = new TreeMap<>();
            for (Component str : data.keySet()) {
               String line = Util.instance.getOldFormattedText(str);
               String cat = line.substring(0, line.indexOf(": "));
               String name = line.substring(line.indexOf(": ") + 2);
               map.put(data.get(str), Component.empty()
                       .append(Component.literal("ID:" + data.get(str) + " ").withStyle(ChatFormatting.DARK_GRAY))
                       .append(Component.literal(cat + ": ").withStyle(ChatFormatting.GRAY))
                       .append(Component.literal(name).withStyle(ChatFormatting.RESET)));
            }
            for (int id : map.keySet()) {
               list.add(map.get(id));
               MutableComponent catData = Component.literal("cat null");
               MutableComponent locData = Component.literal("loc null");
               MutableComponent pos = Component.literal("pos null");
               TransportLocation loc = TransportController.getInstance().getTransport(id);
               if (loc != null) {
                  catData = Component.empty()
                          .append(Component.translatable("drop.category").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal(": \"").withStyle(ChatFormatting.GRAY))
                          .append(Component.translatable(loc.category.title).withStyle(ChatFormatting.RESET))
                          .append(Component.literal("\" ID: ").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal("" + loc.category.id).withStyle(ChatFormatting.GOLD));
                  locData = Component.empty()
                          .append(Component.translatable("gui.location").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal(": \"").withStyle(ChatFormatting.GRAY))
                          .append(Component.translatable(loc.name).withStyle(ChatFormatting.RESET))
                          .append(Component.literal("\" ID: ").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal("" + id).withStyle(ChatFormatting.GOLD));
                  pos = Component.empty()
                          .append(Component.translatable("parameter.world").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal(" ID:\"").withStyle(ChatFormatting.GRAY))
                          .append(Component.translatable(loc.dimension.location().toString()).withStyle(ChatFormatting.GREEN))
                          .append(Component.literal("\"").withStyle(ChatFormatting.GRAY))
                          .append("<br>")
                          .append(Component.translatable("parameter.position").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal(" X:").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal("" + loc.getX()).withStyle(ChatFormatting.AQUA))
                          .append(Component.literal(" Y:").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal("" + loc.getY()).withStyle(ChatFormatting.AQUA))
                          .append(Component.literal(" Z:").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal("" + loc.getZ()).withStyle(ChatFormatting.AQUA));
               }
               hovers.add(catData.append("<br>").append(locData).append("<br>").append(pos));
               for (Component str : data.keySet()) {
                  if (data.get(str) == id) {
                     scrollData.put(map.get(id), str);
                     break;
                  }
               }
            }
            break;
         }
         case Bank: {
            for (Component str : data.keySet()) {
               Component key = Component.empty()
                       .append(Component.literal("ID:" + data.get(str) + " ").withStyle(ChatFormatting.DARK_GRAY))
                       .append(((MutableComponent) str).withStyle(ChatFormatting.RESET));
               list.add(key);
               hovers.add(Component.literal("ID: " + data.get(str)));
               scrollData.put(key, str);
            }
            list.sort(new ComponentOrderComparator());
            break;
         }
         case Factions: {
            Map<Component, Component> map = new HashMap<>();
            scrollData.clear();
            for (Component str : data.keySet()) {
               String line = Util.instance.getOldFormattedText(str);
               if (search.isEmpty() || line.toLowerCase().contains(search)) {
                  String[] l = line.split(";");
                  int value = -1;
                  try { value = Integer.parseInt(l[1]); } catch (Exception e) { LogWriter.error(e); }
                  int color = 0xFFFFFF;
                  MutableComponent hover = Component.empty()
                          .append(Component.literal("ID: "))
                          .append(Component.literal("" + data.get(str)).withStyle(ChatFormatting.GOLD))
                          .append("<br>")
                          .append(Component.translatable("type.value").append(": "))
                          .append(Component.literal("" + value).withStyle(ChatFormatting.DARK_AQUA));
                  Faction f = FactionController.instance.factions.get(data.get(str));
                  if (f != null) {
                     hover.append("<br>").append(Component.translatable("gui.attitude")).append(": ");
                     if (value < f.neutralPoints) { hover.append(Component.translatable("faction.unfriendly").withStyle(ChatFormatting.DARK_RED)); }
                     else if (value < f.friendlyPoints) { hover.append(Component.translatable("faction.neutral").withStyle(ChatFormatting.GOLD)); }
                     else { hover.append(Component.translatable("faction.friendly").withStyle(ChatFormatting.DARK_GREEN)); }
                     color = f.color;
                  }

                  MutableComponent key = Component.empty()
                          .append(Component.translatable(l[0]));
                  Style style = key.getStyle().withColor(color);
                  key.setStyle(style);

                  list.add(key);
                  scrollData.put(key, str);
                  map.put(key, hover);
               }
            }
            list.sort(new ComponentOrderComparator());
            for (Component key : list) { hovers.add(map.getOrDefault(key, Component.empty())); }
            break;
         }
      }
      LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
      if (!hovers.isEmpty()) {
         int i = 0;
         for (Component str : hovers) {
            if (str.getString().contains("<br>")) {
               List<Component> lines = new ArrayList<>();
               String[] ls = Util.instance.getOldFormattedText(str).split("<br>");
               for (String l : ls) { lines.add(Component.literal(l)); }
               hts.put(i, lines);
            }
            else { hts.put(i, List.of(str)); }
            i++;
         }
      }
      scroll.setUnsortedList(list)
              .setSuffixes(!suffixes.isEmpty() ? suffixes : null)
              .setHoverTexts(hts);
   }

}
