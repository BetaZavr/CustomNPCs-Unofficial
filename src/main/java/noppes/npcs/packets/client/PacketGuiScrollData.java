package noppes.npcs.packets.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.listeners.IScrollData;
import noppes.npcs.shared.common.PacketBasic;

public class PacketGuiScrollData extends PacketBasic {

   private static final Map<UUID, Map<String, Integer>> scrollData = new HashMap<>();
   protected static int channelId;

   private Map<String, Integer> data;
   private UUID id;
   private int step;
   private int size;

   public PacketGuiScrollData() { }

   public PacketGuiScrollData(Map<String, Integer> dataIn, UUID idIn, int stepIn, int sizeIn) {
      id = idIn;
      data = dataIn;
      step = stepIn;
      size = sizeIn;
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      int s = buf.readInt();
      data = new HashMap<>();
      for(int i = 0; i < s; ++i) { data.put(buf.readUtf(), buf.readInt()); }
      id  = buf.readUUID();
      step = buf.readInt();
      size = buf.readInt();
   }

   @Override
   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(data.size());
      for (Entry<String, Integer> e : data.entrySet()) {
         buf.writeUtf(e.getKey());
         buf.writeInt(e.getValue());
      }
      buf.writeUUID(id);
      buf.writeInt(step);
      buf.writeInt(size);
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (!scrollData.containsKey(id)) { scrollData.put(id, new HashMap<>()); }
      scrollData.get(id).putAll(data);
      if (step == size) {
         GuiScreen gui = Minecraft.getMinecraft().currentScreen;
         while (gui instanceof IGuiInterface && ((IGuiInterface) gui).hasSubGui()) { gui = ((IGuiInterface) gui).getSubGui(); }
         Map<String, Integer> map = scrollData.get(id);
         if (gui instanceof IScrollData) { ((IScrollData) gui).setData(new Vector<>(map.keySet()), map); }
         scrollData.remove(id);
      }
      CustomNpcs.debugData.end("Packets");
   }

}
