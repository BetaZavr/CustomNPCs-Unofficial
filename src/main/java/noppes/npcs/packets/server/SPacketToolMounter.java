package noppes.npcs.packets.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SPacketToolMounter extends PacketServerBasic {

   protected static int channelId;
   private final int type;
   private final String name;
   private final int tab;
   private final CompoundTag compound;

   private SPacketToolMounter(int typeIn, String nameIn, int tabIn, CompoundTag compoundIn) {
      type = typeIn;
      name = nameIn;
      tab = tabIn;
      compound = compoundIn;
   }

   public SPacketToolMounter(int typeIn, String nameIn, int tabIn) {
      type = typeIn;
      name = nameIn;
      tab = tabIn;
      compound = new CompoundTag();
   }

   public SPacketToolMounter(int typeIn, CompoundTag compoundIn) {
      type = typeIn;
      name = "";
      tab = -1;
      compound = compoundIn;
   }

   public SPacketToolMounter() {
      type = 3;
      name = "";
      tab = -1;
      compound = new CompoundTag();
   }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.mount; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return Collections.singletonList(CustomNpcsPermissions.TOOL_MOUNTER); }

   public static void encode(SPacketToolMounter msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.type);
      buf.writeUtf(msg.name);
      buf.writeInt(msg.tab);
      buf.writeNbt(msg.compound);
   }

   public static SPacketToolMounter decode(FriendlyByteBuf buf) {
      return new SPacketToolMounter(buf.readInt(), buf.readUtf(), buf.readInt(), buf.readNbt());
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      PlayerData data = PlayerData.get(player);
      if (data.mounted != null) {
         Entity entity;
         if (type == 0) {
            Optional<Entity> t = EntityType.create(compound, player.level());
            if (t.isPresent()) {
               entity = t.get();
               entity.setPos(data.mounted.getX(), data.mounted.getY(), data.mounted.getZ());
               player.level().addFreshEntity(entity);
               entity.startRiding(data.mounted, true);
            }
         }
         else if (type == 1) {
            entity = ServerCloneController.Instance.spawn(data.mounted.getX(), data.mounted.getY(), data.mounted.getZ(),
                    tab, name,
                    Objects.requireNonNull(NpcAPI.Instance()).getIWorld(player.level())).getMCEntity();
            if (entity != null) { entity.startRiding(data.mounted, true); }
         }
         else if (type == 2) {
            ResourceLocation loc = EntityUtil.getAllEntities(player.level(), false).get(name);
            EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(loc);
            if (t != null) {
               entity = t.create(player.level());
               if (entity != null) {
                  entity.setPos(data.mounted.getX(), data.mounted.getY(), data.mounted.getZ());
                  player.level().addFreshEntity(entity);
                  entity.startRiding(data.mounted, true);
               }
            }
         } else {
            player.startRiding(data.mounted, true);
         }
      }
      CustomNpcs.debugData.end("Packets");
   }
}
