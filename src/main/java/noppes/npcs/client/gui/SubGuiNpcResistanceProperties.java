package noppes.npcs.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.Resistances;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketGetResistances;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IScrollData;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.util.Util;

import java.util.*;

public class SubGuiNpcResistanceProperties extends GuiNPCInterface
        implements ICustomScrollListener, ISliderListener, IScrollData, ITextfieldListener {

   protected final Resistances resistances;

   // New from Unofficial (BetaZavr)
   protected final Map<Component, String> data = new LinkedHashMap<>();
   protected GuiCustomScrollNop scroll;
   protected Component select = Component.empty();

   public SubGuiNpcResistanceProperties(EntityNPCInterface npc, Resistances resistancesIn) {
      super(npc);
      setBackground("menubg.png");
      imageWidth = 256;
      imageHeight = 216;

      resistances = resistancesIn;
      Packets.sendServer(new SPacketGetResistances());
   }

   @Override
   public void init() {
      super.init();
      List<Component> names = new ArrayList<>();
      List<Component> notList = new ArrayList<>();
      Map<Component, Component> mapSfx = new HashMap<>();
      for (Component key : data.keySet()) {
         String name = data.get(key);
         Component sfx;
         if (resistances.data.containsKey(name)) {
            names.add(key);
            float v = (2.0f - resistances.data.get(name));
            int t = (int) (v * -100.0f + 100.0f);
            sfx = Component.literal(t == 0 ? "" : ((t < 0 ? "" : "+") + t + "%")).withStyle(t < 0 ? ChatFormatting.RED : ChatFormatting.GREEN);
         }
         else {
            notList.add(key);
            sfx = Component.empty();
         }
         mapSfx.put(key, sfx);
      }
      names.addAll(notList);
      if (select.getString().isEmpty() && !names.isEmpty()) { select = names.get(0); }
      List<Component> suffixes = new ArrayList<>();
      LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
      int i = 0;
      for (Component key : names) {
         suffixes.add(mapSfx.get(key));
         hts.put(i++, Collections.singletonList(Component.empty()
                 .append(Component.literal("Damage Type name: \"").withStyle(ChatFormatting.GRAY))
                 .append(Component.literal(data.get(key)).withStyle(ChatFormatting.GOLD))
                 .append(Component.literal("\"").withStyle(ChatFormatting.GRAY))));
      }
      if (scroll == null) { scroll = addScroll(0).setSize(248, 176); }
      add(scroll.setSelected(npc.linkedName)
              .setPos(guiLeft + 4, guiTop + 4)
              .setUnsortedList(names)
              .setSuffixes(suffixes)
              .setSelected(select)
              .setHoverTexts(hts));

      int y = guiTop + imageHeight - 34;
      if (!select.getString().isEmpty()) {
         float v = (2.0f - resistances.get(data.get(select)));
         int t = (int) (v * -100.0f + 100.0f);
         MutableComponent mes = Component.translatable("stats.hover.resist", select.getString());
         if (t == 0) { mes.append(Component.translatable("stats.hover.resist.0")); }
         else if (t < 0) { mes.append(Component.translatable("stats.hover.resist.1", t)); }
         else { mes.append(Component.translatable("stats.hover.resist.2", t)); }
         addSlider(0, guiLeft + 4, y,(float) t * 0.001667f + 0.833333f)
                 .setSize(248, 14)
                 .setString((t == 0 ? "" : (((char) 167) + (t < 0 ? "c" : "a+"))) + String.valueOf(t).replace(".", ",") + "%")
                 .setHoverTexts(mes);
         addTextField(0, guiLeft + 4, y + 16, 60, 14, t)
                 .setMinMaxDefault(-500, 100, t)
                 .setHoverTexts(mes);
      }
      addButton(66, guiLeft + 190, y + 16, "gui.done")
              .setSize(60, 14)
              .setHoverTexts("hover.back");
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      if (button.id == 66) { onClose(); }
   }

   @Override
   public void mouseDragged(GuiSliderNop slider) {
      float n = 5.0f / 6.0f;
      int t = Math.round(slider.sliderValue * 600.0f - 500.0f);
      MutableComponent message = Component.empty();
      ChatFormatting color = slider.sliderValue < n ? ChatFormatting.RED : ChatFormatting.GREEN;
      if (slider.sliderValue != n) { message.append(Component.literal(slider.sliderValue < n ? "" : "+").withStyle(color)); }
      message.append(Component.literal(String.valueOf(t).replace(".", ",") + "%").withStyle(color));
      slider.setMessage(message);
      MutableComponent mes = Component.translatable("stats.hover.resist", Util.instance.deleteColor(select.getString()));
      if (t == 0) { mes.append(Component.translatable("stats.hover.resist.0")); }
      else if (t < 0) { mes.append(Component.translatable("stats.hover.resist.1", "" + t)); }
      else { mes.append(Component.translatable("stats.hover.resist.2", "" + t)); }
      slider.setHoverTexts(mes.getString());
      if (getTextField(0) != null) {
         getTextField(0).setHoverTexts(mes.getString()).setValue("" + t);
      }
   }

   @Override
   public void mousePressed(GuiSliderNop slider) { }

   @Override
   public void mouseReleased(GuiSliderNop slider) {
      if (!data.containsKey(select)) { return; }
      setValue(data.get(select), (int) (slider.sliderValue * 600.0f - 500.0f));
   }

   // New from Unofficial (BetaZavr)
   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (!scroll.hasSelected() || !data.containsKey(scroll.getNormalSelected())) { return; }
      select = scroll.getNormalSelected();
      init();
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { }

   @Override
   public void setData(Vector<String> dataList, Map<String, Integer> dataMap) {
      data.clear();
      Collections.sort(dataList);
      Map<Component, String> preDataMap = new HashMap<>();
      Map<String, Component> hasMap = new TreeMap<>();
      Map<String, Component> notHasMap = new TreeMap<>();
      for (String name : dataList) {
         MutableComponent trName;
         if (name.isEmpty()) { trName = Component.literal("ANY"); }
         else {
            trName = Component.translatable("resistance." + name.toLowerCase());
            if (trName.getString().equals("resistance." + name.toLowerCase())) { trName = Component.literal(name); }
         }
         if (!resistances.data.containsKey(name)) {
            trName = trName.withStyle(ChatFormatting.GRAY);
            notHasMap.put(trName.getString(), trName);
         } else { hasMap.put(trName.getString(), trName); }
         preDataMap.put(trName, name);
      }
      for (Map.Entry<String, Component> entry : hasMap.entrySet()) {
         data.put(entry.getValue(), preDataMap.getOrDefault(entry.getValue(), "any"));
      }
      for (Map.Entry<String, Component> entry : notHasMap.entrySet()) {
         data.put(entry.getValue(), preDataMap.getOrDefault(entry.getValue(), "any"));
      }
      init();
   }

   @Override
   public void setSelected(String select) { }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      if (!data.containsKey(select)) { return; }
      setValue(data.get(select), textField.getInteger());
      float v = (2.0f - resistances.get(data.get(select)));
      int t = (int) (v * -100.0f + 100.0f);
      getSlider(0).sliderValue = (float) t * 0.001667f + 0.833333f;
      getSlider(0).setString((t == 0 ? "" : (((char) 167) + (t < 0 ? "c" : "a+"))) +
              String.valueOf(t).replace(".", ",") + "%");
   }

   private void setValue(String damageType, int value) {
      if (value == 0 && !damageType.equals("arrow") && !damageType.equals("thrown") &&
              !damageType.equals("player") && !damageType.equals("mob") &&
              !damageType.equals("explosion") && !damageType.equals("explosion.player") &&
              !damageType.equals("knockback")) {
         resistances.data.remove(damageType);
      }
      else { resistances.data.put(damageType, value * 0.01f + 1.0f); }

      List<Component> suffixes = new ArrayList<>();
      for (Component key : scroll.getNormalList()) {
         Component sfx;
         if (resistances.data.containsKey(data.get(key))) {
            float v = (2.0f - resistances.data.get(data.get(key)));
            int tt = (int) (v * -100.0f + 100.0f);
            sfx = Component.literal(tt == 0 ? "" : ((tt < 0 ? "" : "+") + tt + "%"))
                    .withStyle(tt < 0 ? ChatFormatting.RED : ChatFormatting.GREEN);
         }
         else { sfx = Component.empty(); }
         suffixes.add(sfx);
      }
      scroll.setSuffixes(suffixes);
   }

}
