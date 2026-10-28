package noppes.npcs.client.gui.roles;

import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketBanksGet;
import noppes.npcs.packets.server.SPacketNpcRoleSave;
import noppes.npcs.roles.RoleBank;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.IScrollData;

public class SubGuiNpcBankSetup extends GuiNPCInterface2 implements IScrollData, ICustomScrollListener {

   protected GuiCustomScrollNop scroll;
   protected final Map<Component, Integer> data = new HashMap<>();
   protected final RoleBank role;

   public SubGuiNpcBankSetup(EntityNPCInterface npc) {
      super(npc);

      role = (RoleBank) npc.role;
      backGui = EnumGuiType.MainMenuAdvanced;
      Packets.sendServer(new SPacketBanksGet());
   }

   @Override
   public void init() {
      super.init();
      if (scroll == null) { scroll = addScroll(0).setSize(200, 152); }
      add(scroll.setPos(guiLeft + 85, guiTop + 20));
      List<Component> list = scroll.getNormalList();
      LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
      if (list != null && !list.isEmpty()) {
         int i = 0;
         for (Component key : list) {
            hts.put(i, Collections.singletonList(Component.literal("ID: " + data.get(key))));
            i++;
         }
      }
      scroll.setHoverTexts(hts);
   }

   @Override
   public void setData(Vector<String> dataList, Map<String, Integer> dataMap) {
      int bankId = role.getBankId();
      Component name = scroll.getNormalSelected();
      data.clear();
      Map<Component, Integer> map = new HashMap<>();
      for (Map.Entry<String, Integer> entry : dataMap.entrySet()) {
         Component key = Component.empty()
                 .append(Component.literal("ID:" + entry.getValue() + " ").withStyle(ChatFormatting.GRAY))
                 .append(Component.literal(entry.getKey()).withStyle(ChatFormatting.RESET));
         map.put(key, entry.getValue());
         if (entry.getValue() == bankId) { name = key; }
      }
      data.putAll(map);
      scroll.setNormalList(new ArrayList<>(data.keySet()));
      if (!name.getString().isEmpty()) { scroll.setSelected(name); }
      init();
   }

   @Override
   public void setSelected(String selected) { }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (scroll.id == 0 && data.containsKey(scroll.getNormalSelected())) {
         int id = data.get(scroll.getNormalSelected());
         if (id != role.bankId) {
            role.bankId = id;
            save();
         }
      }
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) { onClose(); }

   @Override
   public void save() { Packets.sendServer(new SPacketNpcRoleSave(role.save(new CompoundTag()))); }

}
