package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.common.PacketBasic;

public class PacketGuiData extends PacketBasic {

   protected static int channelId;
   private final CompoundTag data;

   public PacketGuiData(CompoundTag compound) { data = compound; }

   public static void encode(PacketGuiData msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

   public static PacketGuiData decode(FriendlyByteBuf buf) { return new PacketGuiData(buf.readNbt(new NbtAccounter(Long.MAX_VALUE))); }

   @Override
   public int getChannelId() { return channelId; }

   @OnlyIn(Dist.CLIENT)
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      Screen gui = Minecraft.getInstance().screen;
      while (gui instanceof IGuiInterface guiMod && guiMod.hasSubGui()) { gui = guiMod.getSubGui(); }
      if (gui instanceof IGuiData guiData) { guiData.setGuiData(data); }
      CustomNpcs.debugData.end("Packets");
   }

}
