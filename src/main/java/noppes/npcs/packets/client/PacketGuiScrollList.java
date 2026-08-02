package noppes.npcs.packets.client;

import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.listeners.IScrollData;
import noppes.npcs.shared.common.PacketBasic;

public class PacketGuiScrollList extends PacketBasic {

   private static final Map<UUID, Vector<String>> listData = new HashMap<>();
   protected static int channelId;

   private Vector<String> data;
   private UUID id;
   private int step;
   private int size;

   public PacketGuiScrollList() { }

   public PacketGuiScrollList(Vector<String> dataIn, UUID idIn, int stepIn, int sizeIn) {
      id = idIn;
      data = dataIn;
      step = stepIn;
      size = sizeIn;
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      int s = buf.readInt();
      data = new Vector<>();
      for(int i = 0; i < s; ++i) { data.add(buf.readUtf()); }
      id  = buf.readUUID();
      step = buf.readInt();
      size = buf.readInt();
   }

   @Override
   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(data.size());
      for (String s : data) { buf.writeUtf(s); }
      buf.writeUUID(id);
      buf.writeInt(step);
      buf.writeInt(size);
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (!listData.containsKey(id)) { listData.put(id, new Vector<>()); }
      listData.get(id).addAll(data);
      if (step == size) {
         GuiScreen gui = Minecraft.getMinecraft().currentScreen;
         while (gui instanceof IGuiInterface && ((IGuiInterface) gui).hasSubGui()) { gui = ((IGuiInterface) gui).getSubGui(); }
         Vector<String> list = listData.get(id);
         if (gui instanceof IScrollData) { ((IScrollData) gui).setData(list, null); }
         listData.remove(id);
      }
      CustomNpcs.debugData.end("Packets");
   }

}
