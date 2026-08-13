package noppes.npcs.roles;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.role.IRoleTransporter;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketChatBubble;
import noppes.npcs.packets.server.SPacketDimensionTeleport;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RoleTransporter extends RoleInterface implements IRoleTransporter {

   protected int ticks = 10;
   public int transportId = -1;
   public String name;

   public RoleTransporter(EntityNPCInterface npc) {
      super(npc);
      type = RoleType.TRANSPORTER;
   }

   @Override
   public CompoundTag save(CompoundTag compound) {
      super.save(compound);
      compound.putInt("TransporterId", transportId);
      return compound;
   }

   @Override
   public void load(CompoundTag compound) {
      super.load(compound);
      type = RoleType.TRANSPORTER;
      transportId = compound.getInt("TransporterId");
      TransportLocation loc = getLocation();
      if (loc != null) { name = loc.name; } else { name = ""; }
   }

   @Override
   public boolean aiShouldExecute() {
      --ticks;
       if (ticks <= 0) {
           ticks = 10;
           if (hasTransport()) {
              TransportLocation loc = getLocation();
              if (loc != null && loc.type == 0) {
                 List<Player> inRange = npc.level().getEntitiesOfClass(Player.class, npc.getBoundingBox().inflate(6.0D, 6.0D, 6.0D));
                 for (Player player : inRange) {
                    if (npc.canSee(player)) { unlock(player, loc); }
                 }
              }
           }
       }
       return false;
   }

   @Override
   public void interact(Player player) {
      if (player instanceof ServerPlayer sPlayer) {
         TransportLocation loc = getLocation();
         if (loc != null) {
            if (loc.type != 1) { unlock(player, loc); }
            NoppesUtilServer.sendOpenGui(sPlayer, EnumGuiType.PlayerTransporter, npc);
         }
      }
   }

   public void transport(ServerPlayer player, int location) {
      TransportLocation loc = TransportController.getInstance().getTransport(location);
      PlayerData playerdata = PlayerData.get(player);
      if (loc != null && loc.id > -1 && (loc.isDefault() || playerdata.transportData.transports.contains(loc.id))) {
         RoleEvent.TransporterUseEvent event = new RoleEvent.TransporterUseEvent(player, npc.wrappedNPC, loc);
         if (!EventHooks.onNPCRole(npc, event) && event.location != null) {
            loc = (TransportLocation) event.location;
            if (!player.isCreative()) {
               if (loc.money > 0) {
                  if (loc.money > playerdata.game.getMoney()) {
                     player.sendSystemMessage(Component.translatable("transporter.hover.not.money"));
                     return;
                  }
                  playerdata.game.addMoney(-1L * loc.money);
               }
               if (!loc.inventory.isEmpty()) {
                  Map<ItemStack, Boolean> barterItems = Util.instance.getInventoryItemCount(player, loc.inventory);
                  for (ItemStack stack : barterItems.keySet()) {
                     if (!barterItems.get(stack)) {
                        player.sendSystemMessage(Component.translatable("transporter.hover.not.money"));
                        return;
                     }
                  }
                  for (ItemStack stack : barterItems.keySet()) {
                     int amount = stack.getCount();
                     for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                        ItemStack is = player.getInventory().getItem(i);
                        if (isItemEqual(stack, is)) {
                           if (amount < is.getCount()) {
                              is.split(amount);
                              break;
                           }
                           player.getInventory().setItem(i, ItemStack.EMPTY);
                           amount -= is.getCount();
                        }
                     }
                  }
                  player.containerMenu.broadcastChanges();
                  CustomNPCsScheduler.runTack(() -> {
                     for (QuestData data : playerdata.questData.activeQuests.values()) {
                        for (IQuestObjective obj : data.quest
                                .getObjectives((IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player))) {
                           if (obj.getType() == 0) { playerdata.questData.checkQuestCompletion(player, data); }
                        }
                     }
                  });
               }
            }
            npc.say(player, new Line(Component.translatable("transporter.go.way").getString()));
            SPacketDimensionTeleport.teleportPlayer(player, loc.dimension, loc.pos.getX(), loc.pos.getY(), loc.pos.getZ(), loc.yaw, loc.pitch);
         }
      }
   }

   private void unlock(Player player, @Nonnull TransportLocation loc) {
      PlayerTransportData data = PlayerData.get(player).transportData;
      if (!data.transports.contains(transportId) && player instanceof ServerPlayer sPlayer) {
         RoleEvent.TransporterUnlockedEvent event = new RoleEvent.TransporterUnlockedEvent(sPlayer, npc.wrappedNPC);
         if (!EventHooks.onNPCRole(npc, event)) {
            data.transports.add(transportId);
            Packets.send(sPlayer, new PacketChatBubble(npc.getId(),
                    Component.translatable("transporter.unlock",
                    Component.translatable(loc.name).getString(),
                    Component.translatable(loc.category.title).getString()), true));
         }
      }
   }

   @Override
   public @Nullable TransportLocation getLocation() {
      return TransportController.getInstance().getTransport(transportId);
   }

   public boolean hasTransport() {
      TransportLocation loc = getLocation();
      return loc != null && loc.id == transportId;
   }

   public void setTransport(TransportLocation location) {
      transportId = location.id;
      name = location.name;
   }

   // New from Unofficial (BetaZavr)
   private boolean isItemEqual(ItemStack stack, ItemStack other) {
      return !other.isEmpty() && stack.getItem() == other.getItem();
   }

}
