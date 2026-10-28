package noppes.npcs.packets.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.wrapper.gui.CustomGuiComponentWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.shared.client.gui.listeners.custom.IComponentCustomGui;
import noppes.npcs.shared.common.PacketBasic;

public class PacketGuiComponentUpdate extends PacketBasic {

   protected static int channelId;
   private final UUID id;
   private final CompoundTag data;

   public PacketGuiComponentUpdate(UUID idIn, CompoundTag dataIn) {
      id = idIn;
      data = dataIn;
   }

   public static void encode(PacketGuiComponentUpdate msg, FriendlyByteBuf buf) {
      buf.writeUUID(msg.id);
      buf.writeNbt(msg.data);
   }

   public static PacketGuiComponentUpdate decode(FriendlyByteBuf buf) {
      return new PacketGuiComponentUpdate(buf.readUUID(), buf.readNbt());
   }

   @Override
   public int getChannelId() { return channelId; }

   @OnlyIn(Dist.CLIENT)
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (Minecraft.getInstance().screen instanceof GuiCustom cGui) {
         CustomGuiComponentWrapper component = (CustomGuiComponentWrapper) cGui.guiWrapper.getComponentUuid(id);
         if (component != null) {
            component.fromNBT(data);
            if (cGui.getComponent(id) instanceof IComponentCustomGui iCCG) { iCCG.init(); }
         }
      }
      CustomNpcs.debugData.end("Packets");
   }

}
