package noppes.npcs.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcs;
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
   private final boolean isStart;
   private final double x;
   private final double y;
   private final double z;
   private final float volume;
   private final float pitch;
   private final String name;
   private final String resource;
   private final String category;
   private final boolean looping;

   public SPacketPlayerSound(boolean isStartIn, MusicData md) {
      isStart = isStartIn;
      name = md.resource.getPath().replaceAll("/", ".");
      resource = md.name;
      category = md.category.getName();
      looping = md.sound.isLooping();
      IPos pos = md.getPos();
      x = pos.getX();
      y = pos.getY();
      z = pos.getZ();
      volume = md.sound.getVolume();
      pitch = md.sound.getPitch();
   }

   public SPacketPlayerSound(boolean isStartIn, String soundIn, String resourceIn, String categoryIn, boolean loopingIn,
                             double xIn, double yIn, double zIn, float volumeIn, float pitchIn) {
      isStart = isStartIn;
      name = soundIn;
      resource = resourceIn;
      category = categoryIn;
      looping = loopingIn;
      x = xIn;
      y = yIn;
      z = zIn;
      volume = volumeIn;
      pitch = pitchIn;
   }

   @Override
   public boolean toolAllowed(ItemStack item) { return true; }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return null; }

   public static void encode(SPacketPlayerSound msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.isStart);
      buf.writeUtf(msg.name == null ? "" : msg.name);
      buf.writeUtf(msg.resource == null ? "" : msg.resource);
      buf.writeUtf(msg.category == null ? SoundSource.MASTER.getName() : msg.category);
      buf.writeBoolean(msg.looping);
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.volume);
      buf.writeFloat(msg.pitch);
   }

   public static SPacketPlayerSound decode(FriendlyByteBuf buf) {
      return new SPacketPlayerSound(buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(),
              buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat());
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
