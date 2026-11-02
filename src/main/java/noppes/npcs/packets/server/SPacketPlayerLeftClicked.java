package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.EventHooks;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.ItemEvent;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.items.ItemScripted;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketPlayerLeftClicked extends PacketServerBasic {

   protected static int channelId;

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) { return true; }

   @Override
   public void encode(FriendlyByteBuf buf) { }

   @Override
   public void decode(FriendlyByteBuf buf) { }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      if (CustomNpcs.EnableScripting && !ScriptController.Instance.languages.isEmpty()) {
         ItemStack item = player.getHeldItemMainhand();
         IPlayer<?> iPlayer = PlayerData.get(player).getIPlayer();
         EventHooks.onPlayerAttack(new PlayerEvent.AttackEvent(iPlayer, 0, null));
         if (item.getItem() == CustomItems.scripter_item) {
            ItemScriptedWrapper isw = ItemScripted.GetWrapper(item);
            ItemEvent.AttackEvent itemEvent = new ItemEvent.AttackEvent(isw, iPlayer, 0, null);
            EventHooks.onScriptItemAttack(isw, itemEvent);
         }
      }
      CustomNpcs.debugData.end("Packets");
   }

}
