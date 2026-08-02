package noppes.npcs.packets.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NBTTags;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class SPacketToolMobSpawner extends PacketServerBasic {

   protected static int channelId;
   private boolean createSpawner;
   private boolean isServerClone;
   private BlockPos pos;
   private String name;
   private int tab;
   private NBTTagCompound clone;

   public SPacketToolMobSpawner() { }

   public SPacketToolMobSpawner(boolean isServer, boolean createSpawnerIn, BlockPos posIn, String nameIn, int tabIn, NBTTagCompound cloneIn) {
      isServerClone = isServer;
      createSpawner = createSpawnerIn;
      pos = posIn;
      name = nameIn;
      tab = tabIn;
      clone = cloneIn;
   }

   @Override
   public boolean toolAllowed(ItemStack item) { return item.getItem() == CustomItems.wand || item.getItem() == CustomItems.cloner || item.getItem() == CustomItems.mount; }

   @Override
   public boolean requiresNpc() { return false; }

   @Override
   public List<CustomNpcsPermissions.Permission> getPermission() {
      return Collections.singletonList(createSpawner ? CustomNpcsPermissions.SPAWNER_CREATE : CustomNpcsPermissions.SPAWNER_MOB);
   }

   @Override
   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(isServerClone);
      buf.writeBoolean(createSpawner);
      buf.writeBlockPos(pos);
      buf.writeUtf(name);
      buf.writeInt(tab);
      buf.writeNbt(clone);
   }

   @Override
   public void decode(FriendlyByteBuf buf) {
      isServerClone = buf.readBoolean();
      createSpawner = buf.readBoolean();
      pos = buf.readBlockPos();
      name = buf.readUtf();
      tab = buf.readInt();
      clone = buf.readNbt();
   }

   @Override
   public int getChannelId() { return channelId; }

   @Override
   protected void handle() {
      CustomNpcs.debugData.start("Packets");

      NBTTagCompound compound;
      NBTTagCompound nbtData = new NBTTagCompound();
      if (isServerClone) {
         nbtData.setString("Name", name);
         nbtData.setInteger("Tab", tab);
         nbtData.setBoolean("isServerClone", true);
         compound = ServerCloneController.Instance.getCloneData(player, name, tab);
      }
      else {
         compound = clone;
         nbtData.setString("Name", compound.getString("Name"));
         nbtData.setBoolean("isServerClone", false);
      }
      if (compound != null && !compound.hasNoTags()) {
         if (createSpawner) { createMobSpawner(pos, compound, player); }
         else {
            if (!isServerClone) { nbtData.setTag("EntityNBT", compound); }
            Entity entity = SPacketToolMobSpawner.spawnClone(compound, pos.getX() + 0.5d, pos.getY() + groundOffset(player.world, pos), pos.getZ() + 0.5d, player.world);
            ItemStack stack = player.getHeldItemMainhand();
            NBTTagCompound nbt = null;
            if (stack.getItem() == CustomItems.cloner) {
               nbt = stack.getTagCompound();
               if (nbt == null) { stack.setTagCompound(nbt = new NBTTagCompound()); }
            }
            if (entity == null) {
               if (nbt != null && nbt.hasKey("Settings")) {
                  nbt.removeTag("Settings");
                  player.openContainer.detectAndSendChanges();
               }
               player.sendMessage(Component.literal("Failed to create an entity out of your clone").getParent());
            }
            else {
               if (nbt != null) {
                  if (!nbtData.hasKey("Name", 8) || nbtData.getString("Name").isEmpty()) { nbtData.setString("Name", entity.getName()); }
                  nbt.setTag("Settings", nbtData);
                  player.openContainer.detectAndSendChanges();
               }
            }
         }
      }
      CustomNpcs.debugData.end("Packets");
   }

   private static double groundOffset(World world, BlockPos pos) {
      AxisAlignedBB area = new AxisAlignedBB(pos).expand(0.0d, -1.0d, 0.0d);
      List<AxisAlignedBB> list = world.getCollisionBoxes(null, area);
      if (list.isEmpty()) { return 0.0d; }
      double top = area.minY;
      for (AxisAlignedBB box : list) { top = Math.max(box.maxY, top); }
      return top - pos.getY();
   }

   public static @Nullable Entity spawnClone(NBTTagCompound compound, double x, double y, double z, World world) {
      if (world == null || world.isRemote) {
         LogWriter.error("Clone summoning Error: World is Client: " + (world == null ? "null" : "true") + " - " + world);
         return null;
      }
      if (compound == null) {
         LogWriter.error("Clone summoning Error: Missing NBT Tags: "
                 + "null or World: "
                 + world.provider.getDimension());
         return null;
      }
      ServerCloneController.Instance.cleanTags(compound);
      compound.setTag("Pos", NBTTags.nbtDoubleList(x, y, z));
      Entity entity = EntityList.createEntityFromNBT(compound, world);
      if (entity == null) {
         LogWriter.error("Clone summoning error: Failed to create an entity based on the passed NBT tags: " + compound);
         return null;
      }
      BlockPos pos = new BlockPos(x, y, z);
      if (entity instanceof EntityNPCInterface) {
         ((EntityNPCInterface) entity).homeDimensionId = world.provider.getDimension();
         ((EntityNPCInterface) entity).ais.setStartPos(pos);
      }
      else if (entity instanceof EntityCreature) { ((EntityCreature) entity).setHomePosAndDistance(pos, (int) ((EntityCreature) entity).getMaximumHomeDistance()); }
      entity.readFromNBT(compound);
      entity.setPosition(x, y, z);
      world.spawnEntity(entity);
      LogWriter.debug("Summon Clone: Successful \"" + entity.getName() + "\"; " + entity.world.isRemote);
      return entity;
   }

   public static void createMobSpawner(BlockPos pos, NBTTagCompound comp, EntityPlayerMP player) {
      ServerCloneController.Instance.cleanTags(comp);
      if (comp.getString("id").equalsIgnoreCase("entityhorse")) {
         player.sendMessage(Component.translatable("message.error.create.mob.spawner").getParent());
      }
      else {
         player.world.setBlockState(pos, Blocks.MOB_SPAWNER.getDefaultState());
         TileEntity tile =  player.world.getTileEntity(pos);
         if (tile instanceof TileEntityMobSpawner) {
            MobSpawnerBaseLogic logic = ((TileEntityMobSpawner) tile).getSpawnerBaseLogic();
            if (!comp.hasKey("id", 8)) { comp.setString("id", "Pig"); }
            comp.setIntArray("StartPosNew", new int[] { pos.getX(), pos.getY(), pos.getZ() });
            logic.setNextSpawnData(new WeightedSpawnerEntity(1, comp));
         }
      }
   }

}
