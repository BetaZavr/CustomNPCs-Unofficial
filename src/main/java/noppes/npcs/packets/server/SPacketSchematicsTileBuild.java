package noppes.npcs.packets.server;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.schematics.Schematic;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.schematics.SchematicWrapper;

import java.util.List;

public class SPacketSchematicsTileBuild extends PacketServerBasic {

   protected static int channelId;
   private final BlockPos pos;
   private final int rotation;
   private final CompoundTag compound;

   public SPacketSchematicsTileBuild(BlockPos posIn, int rotationIn, CompoundTag compoundIn) {
      pos = posIn;
      rotation = rotationIn;
      compound = compoundIn;
   }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public List<PermissionNode<Boolean>> getPermission() { return null; }

   @Override
   public boolean toolAllowed(ItemStack item) {
      return item.getItem() == CustomItems.wand || item.getItem() == CustomBlocks.builder_item || item.getItem() == CustomBlocks.copy_item;
   }

   public static void encode(SPacketSchematicsTileBuild msg, FriendlyByteBuf buf) {
      buf.writeBlockPos(msg.pos);
      buf.writeInt(msg.rotation);
      buf.writeNbt(msg.compound);
   }

   public static SPacketSchematicsTileBuild decode(FriendlyByteBuf buf) {
      return new SPacketSchematicsTileBuild(buf.readBlockPos(), buf.readInt(), buf.readNbt());
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");
      SchematicWrapper wrapper;
      if (player.level().getBlockEntity(pos) instanceof TileBuilder tile && compound.isEmpty()) {
         wrapper = tile.getSchematic();
         wrapper.init(pos.offset(1, tile.yOffset, 1), player.level(), tile.rotation * 90);
         SchematicController.Instance.build(tile.getSchematic(), player.createCommandSourceStack());
         player.level().removeBlock(pos, false);
      }
      else {
         Schematic schema = new Schematic("");
         schema.load(compound);
         wrapper = new SchematicWrapper(schema);
         wrapper.init(pos.east().south(), player.level(), rotation);
         SchematicController.buildBlocks(player, pos, wrapper);
      }
      CustomNpcs.debugData.start("Packets");
   }
}
