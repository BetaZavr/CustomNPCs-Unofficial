package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.shared.client.gui.components.custom.GuiCreationNewParts;
import noppes.npcs.shared.common.PacketBasic;

public class PacketGuiParts extends PacketBasic {

   protected static int channelId;
   private final int id;
   private final CompoundTag data;

   public PacketGuiParts(int idIn, CompoundTag dataIn) {
      id = idIn;
      data = dataIn;
   }

   public static void encode(PacketGuiParts msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.id);
      buf.writeNbt(msg.data);
   }

   public static PacketGuiParts decode(FriendlyByteBuf buf) { return new PacketGuiParts(buf.readInt(), buf.readAnySizeNbt()); }

   @Override
   public int getChannelId() { return channelId; }

   @OnlyIn(Dist.CLIENT)
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      Entity entity = player.level().getEntity(id);
      if (Minecraft.getInstance().screen instanceof GuiCustom gui && entity instanceof EntityCustomNpc npc) {
         GuiCreationNewParts parts = new GuiCreationNewParts(gui, npc);
         gui.initCallback = () -> {
            gui.add(parts);
            parts.init();
         };
         gui.setGuiData(data);
      }
      CustomNpcs.debugData.end("Packets");
   }

}
