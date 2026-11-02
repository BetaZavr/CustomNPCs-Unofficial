package noppes.npcs.packets.server;

import net.minecraft.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SoundCategory;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.EventHooks;
import noppes.npcs.api.IPos;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.client.util.MusicData;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.shared.common.PacketServerBasic;

import java.util.List;

public class SPacketPlayerSound extends PacketServerBasic {

   protected static int channelId;
   private boolean isStart;
   private double x;
   private double y;
   private double z;
   private float volume;
   private float pitch;
   private String name;
   private String resource;
   private String category;
   private boolean looping;

   public SPacketPlayerSound() { }

   public SPacketPlayerSound(boolean isStartIn, MusicData md) {
      isStart = isStartIn;
      name = md.resource.getResourcePath().replaceAll("/", ".");
      resource = md.name;
      category = md.category.getName();
      looping = false;
      IPos pos = md.getPos();
      x = pos.getX();
      y = pos.getY();
      z = pos.getZ();
      volume = md.sound.getVolume();
      pitch = md.sound.getPitch();
   }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) { return true; }

   @Override
   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(isStart);
      buf.writeUtf(name == null ? "" : name);
      buf.writeUtf(resource == null ? "" : resource);
      buf.writeUtf(category == null ? SoundCategory.MASTER.getName() : category);
      buf.writeBoolean(looping);
      buf.writeDouble(x);
      buf.writeDouble(y);
      buf.writeDouble(z);
      buf.writeFloat(volume);
      buf.writeFloat(pitch);
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      isStart = buf.readBoolean();
      name = buf.readUtf();
      resource = buf.readUtf();
      category = buf.readUtf();
      looping = buf.readBoolean();
      x = buf.readDouble();
      y = buf.readDouble();
      z = buf.readDouble();
      volume = buf.readFloat();
      pitch = buf.readFloat();
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      EventHooks.onEvent(EventHooks.getPlayerHandler(), isStart ? EnumScriptType.PLAY_SOUND : EnumScriptType.STOP_SOUND,
              new PlayerEvent.PlayerSound(PlayerData.get(player).getIPlayer(), name, resource, category, looping, x, y, z, volume, pitch));
      CustomNpcs.debugData.end("Packets");
   }

}
