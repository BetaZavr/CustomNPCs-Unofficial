package noppes.npcs.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ChatMessages;
import noppes.npcs.client.ClientEventHandler;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.shared.common.PacketBasic;
import noppes.npcs.util.Util;

public class PacketChatBubble extends PacketBasic {

   protected static int channelId;
   private final int id;
   private final Component message;
   private final boolean showMessage;

   public PacketChatBubble(int idIn, Component messageIn, boolean showMessageIn) {
      id = idIn;
      message = messageIn;
      showMessage = showMessageIn;
   }

   public static void encode(PacketChatBubble msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.id);
      buf.writeComponent(msg.message);
      buf.writeBoolean(msg.showMessage);
   }

   public static PacketChatBubble decode(FriendlyByteBuf buf) {
      return new PacketChatBubble(buf.readInt(), buf.readComponent(), buf.readBoolean());
   }

   @Override
   public int getChannelId() { return channelId; }

   @OnlyIn(Dist.CLIENT)
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         Entity entity = level.getEntity(id);
         if (entity instanceof EntityNPCInterface npc) {
            String text = NoppesStringUtils.formatText(message, player, npc);
            if (CustomNpcs.EnableChatBubbles) {
               if (npc.messages == null) { npc.messages = new ChatMessages(); }
               npc.messages.addMessage(text, npc);
            }
            if (showMessage) {
               player.sendSystemMessage(Component.literal(npc.getName().getString() + ": ").append(Component.translatable(text)));
            }
         }
         if (CustomNpcs.EnablePlayerChatBubbles && entity instanceof Player pl) {
            if (!ClientEventHandler.chatMessages.containsKey(pl)) { ClientEventHandler.chatMessages.put(pl, new ChatMessages()); }
            ClientEventHandler.chatMessages.get(pl).addMessage(Util.instance.getOldFormattedText(message), pl);
         }
      }
      CustomNpcs.debugData.end("Packets");
   }

}
