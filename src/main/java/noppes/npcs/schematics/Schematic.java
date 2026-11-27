package noppes.npcs.schematics;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.IPos;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

import javax.annotation.Nullable;

@SuppressWarnings({"deprecation","UnstableApiUsage"})
public class Schematic implements ISchematic {

   public static Schematic create(Level levelIn, Direction rotate, String name, Map<Integer, BlockPos> schMap) {
      CommonUtil.NotifyOPs("Generating the \"" + name + "\" schema may be a little late");
      BlockPos p = schMap.get(0); // offset
      BlockPos m = schMap.get(1); // min
      BlockPos n = schMap.get(2); // max
      AABB bb = new AABB(m, n);
      short height = (short) (Math.abs(bb.maxY - bb.minY) + 1);
      short width = (short) (Math.abs(bb.maxX - bb.minX) + 1);
      short length = (short) (Math.abs(bb.maxZ - bb.minZ) + 1);
      Schematic schema = new Schematic(name);
      schema.height = height;
      schema.width = (rotate == Direction.EAST || rotate == Direction.WEST) ? length : width;
      schema.length = (rotate == Direction.EAST || rotate == Direction.WEST) ? width : length;
      int size = height * width * length;
      schema.blockStates = new BlockState[size];
      schema.level = levelIn;
      int rot = switch (rotate) {
         case EAST -> 1;
         case NORTH -> 2;
         case WEST -> 3;
         default -> 0;
      };
      int x;
      int y;
      int z;
      IPos iPos = new BlockPosWrapper(levelIn, bb.minX, bb.minY, bb.minZ);
      for (int i = 0; i < size; ++i) {
         y = i / (width * length);
         switch (rotate) {
            case EAST: {
               x = i / length - y * width;
               z = length - 1 - i % length;
               break;
            }
            case NORTH: {
               x = width - 1 - i % width;
               z = length - 1 - (i / width) % length;
               break;
            }
            case WEST: {
               x = width - 1 - (i / length) % width;
               z = i % length;
               break;
            }
            default: { // SOUTH
               x = i % width;
               z = (i - x) / width % length;
               break;
            }
         }

         BlockPos pos = iPos.offset(x, y, z).getMCBlockPos();
         schema.blockStates[i] = SchematicWrapper.rotationState(levelIn.getBlockState(pos), rot);
         if (schema.blockStates[i].getBlock() instanceof EntityBlock) {
            BlockEntity tile = levelIn.getBlockEntity(pos);
            CompoundTag compound = new CompoundTag();
            if (tile != null) { compound = tile.saveWithFullMetadata(); }
            compound.putInt("x", x);
            compound.putInt("y", y);
            compound.putInt("z", z);
            schema.tileList.add(compound);
         }
      }
      // Added by mod
      schema.offset = new BlockPosWrapper(levelIn, (int) Math.floor(bb.minX - p.getX()), (int) Math.floor(bb.minY - p.getY()), (int) Math.floor(bb.minZ - p.getZ()))
              .rotate(rotate);
      // Get Entitys:
      try {
         for (Entity e : levelIn.getEntitiesOfClass(Entity.class,
                 new AABB(bb.minX - 0.25d, bb.minY - 0.25d, bb.minZ - 0.25d,
                         bb.maxX + 0.25d, bb.maxY + 0.25d, bb.maxZ + 0.25d),
                 (entity) -> !(entity instanceof Projectile || entity instanceof Arrow || entity instanceof Player))) {
            CompoundTag nbtEntity = e.serializeNBT();
            if (!nbtEntity.contains("UUID", 8)) { nbtEntity.putString("UUID", e.getStringUUID()); }
            ListTag posList = new ListTag();
            double[] d = new double[] { e.getX() - p.getX() - 1.0d, e.getY() - p.getY(), e.getZ() - p.getZ() - 1.0d };
            double[] ed = new double[] { d[0], d[1], d[2] };
            if (e instanceof HangingEntity) {
               d = new double[] { e.getX() - p.getX(), e.getY() - 1 - p.getY(), e.getZ() - p.getZ() };
               ed = new double[] { d[0], d[1], d[2] };
               float er = nbtEntity.getList("Rotation", 5).getFloat(0);
               byte f = nbtEntity.getByte("Facing");
               switch (rot) {
                  case 1:
                     f += 1;
                     er += 90.0f;
                     ed[0] = d[2];
                     ed[2] = d[0];
                     break;
                  case 2:
                     f += 2;
                     er += 180.0f;
                     ed[0] *= -1.0d;
                     ed[2] *= -1.0d;
                     break;
                  case 3:
                     f += 3;
                     er += 270.0f;
                     ed[0] = d[2] * -1.0d;
                     ed[2] = d[0] * -1.0d;
                     break;
                  default:
                     break;
               }
               f %= (byte) 4;
               nbtEntity.putByte("Facing", f);
               nbtEntity.getList("Rotation", 5).set(0, FloatTag.valueOf(er % 360.0f));
               nbtEntity.putInt("TileX", (int) ed[0]);
               nbtEntity.putInt("TileY", (int) ed[1]);
               nbtEntity.putInt("TileZ", (int) ed[2]);
               posList.add(DoubleTag.valueOf(ed[0]));
               posList.add(DoubleTag.valueOf(ed[1]));
               posList.add(DoubleTag.valueOf(ed[2]));
            }
            else {
               switch (rot) {
                  case 1:
                     ed[0] = d[2];
                     ed[2] = d[0];
                     break;
                  case 2:
                     ed[0] *= -1.0d;
                     ed[0] -= 1.0d;
                     ed[2] *= -1.0d;
                     ed[2] -= 1.0d;
                     break;
                  case 3:
                     ed[0] = d[2] * -1.0d;
                     ed[0] -= 1.0d;
                     ed[2] = d[0] * -1.0d;
                     ed[2] -= 1.0d;
                     break;
                  default:
                     break;
               }
               posList.add(DoubleTag.valueOf(ed[0] - 0.5d));
               posList.add(DoubleTag.valueOf(ed[1]));
               posList.add(DoubleTag.valueOf(ed[2] - 0.5d));
            }
            nbtEntity.put("Pos", posList);
            schema.entityList.add(nbtEntity);
         }
      }
      catch (Exception ignored) { }
      return schema;
   }

   public static final HashMap<String, BlockState> staticBlockIds = new HashMap<>();
   protected IPos offset = BlockPosWrapper.ZERO;
   protected ListTag entityList = new ListTag();

   public String name;
   public short width;
   public short height;
   public short length;
   public ListTag tileList = new ListTag();
   public BlockState[] blockStates;
   private @Nullable Level level;

   @SuppressWarnings("unused")
   private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> prop, String val) {
      Optional<T> optional = prop.getValue(val);
      return optional.map(t -> state.setValue(prop, t)).orElse(state);
   }

   public Schematic(String nameIn) { name = nameIn.toLowerCase(); }

   public void load(CompoundTag compound) {
      if (compound.contains("Name", 8)) { name = compound.getString("Name"); }
      width = compound.getShort("Width");
      height = compound.getShort("Height");
      length = compound.getShort("Length");
      byte[] addId = compound.contains("AddBlocks") ? compound.getByteArray("AddBlocks") : new byte[0];
      setBlockStates(compound.getByteArray("Blocks"), addId, compound.getByteArray("Data"));
      entityList = compound.getList("Entities", 10);
      tileList = compound.getList("TileEntities", 10);
      offset = BlockPosWrapper.ZERO;
      if (compound.contains("Offset", 4)) { offset = new BlockPosWrapper(level, BlockPos.of(compound.getLong("Offset"))); }
   }

   @Override
   public CompoundTag getNBT() {
      CompoundTag compound = new CompoundTag();
      compound.putString("Name", name);
      compound.putShort("Width", width);
      compound.putShort("Height", height);
      compound.putShort("Length", length);
      byte[][] arr = getBlockBytes();
      compound.putByteArray("Blocks", arr[0]);
      if (arr[1] != null && arr[1].length > 1) { compound.putByteArray("AddBlocks", arr[1]); }
      compound.putByteArray("Data", arr[2]);
      compound.put("TileEntities", tileList);
      compound.put("Entities", entityList);
      compound.putLong("Offset", offset.getMCBlockPos().asLong());
      return compound;
   }

   @Override
   public IPos getOffset() { return offset; }

   @Override
   public boolean hasEntitys() { return entityList != null && !entityList.isEmpty(); }

   @Override
   public ListTag getEntitys() { return entityList; }

   @Override
   public BlockState getBlockState(int x, int y, int z) { return getBlockState(xyzToIndex(x, y, z)); }

   @Override
   public BlockState getBlockState(int pos) {
      if (pos < 0 || pos >= blockStates.length) { return Blocks.AIR.defaultBlockState(); }
      return blockStates[pos] == null ? Blocks.AIR.defaultBlockState() : blockStates[pos];
   }

   @Override
   public short getWidth() { return width; }

   @Override
   public short getHeight() { return height; }

   @Override
   public short getLength() { return length; }

   @Override
   public int getBlockEntityDimensions() { return tileList == null ? 0 : tileList.size(); }

   @Override
   public CompoundTag getBlockEntity(int i) { return tileList.getCompound(i); }

   @Override
   public String getName() { return name; }

   public void setBlockStates(byte[] blockId, byte[] addId, byte[] data) {
      blockStates = new BlockState[blockId.length];
      for(int index = 0; index < blockId.length; ++index) {
         short id = (short)(blockId[index] & 255);
         if (index >> 1 < addId.length) {
            if ((index & 1) == 0) { id += (short)((addId[index >> 1] & 15) << 8); }
            else { id += (short)((addId[index >> 1] & 240) << 4); }
         }
         blockStates[index] = staticBlockIds.get(id + ":" + data[index]);
      }
   }

   public byte[][] getBlockBytes() {
      byte[] blockIds = new byte[blockStates.length];
      byte[] addBlocks = null;
      byte[] datas = new byte[blockStates.length];
      HashMap<BlockState, int[]> tempIds = new HashMap<>();
      for(int i = 0; i < blockIds.length; ++i) {
         int[] ids;
         if (tempIds.containsKey(blockStates[i])) { ids = tempIds.get(blockStates[i]); }
         else {
            ids = new int[] { ((ForgeRegistry<Block>) ForgeRegistries.BLOCKS).getID(blockStates[i].getBlock()), 0 };
            for (Map.Entry<String, BlockState> entry : staticBlockIds.entrySet()) {
               if (entry.getValue() == blockStates[i]) {
                  try {
                     ids[1] = Integer.parseInt(entry.getKey().substring(entry.getKey().lastIndexOf(":") + 1));
                  }
                  catch (Exception ignored) { }
               }
            }
            tempIds.put(blockStates[i], ids);
         }
         if (ids[0] > 255) {
            if (addBlocks == null) { addBlocks = new byte[(blockIds.length >> 1) + 1]; }
            if ((i & 1) == 0) { addBlocks[i >> 1] = (byte)(addBlocks[i >> 1] & 240 | ids[0] >> 8 & 15); }
            else { addBlocks[i >> 1] = (byte)(addBlocks[i >> 1] & 15 | (ids[0] >> 8 & 15) << 4); }
         }
         blockIds[i] = (byte) ids[0];
         datas[i] = (byte) ids[1];
      }
      return new byte[][] { blockIds, addBlocks, datas };
   }

   public int xyzToIndex(int x, int y, int z) { return (y * length + z) * width + x; }

   // New from Unofficial (BetaZavr)
   static {
      Map<Block, String> blockIds = new HashMap<>();
      Map<Block, List<BlockState>> states = new HashMap<>();
      for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
         Block block = state.getBlock();
         if (!blockIds.containsKey(block)) { blockIds.put(block, "" + ((ForgeRegistry<Block>) ForgeRegistries.BLOCKS).getID(block)); }
         if (!states.containsKey(block)) { states.put(block, new ArrayList<>()); }
         if (!states.get(block).contains(state)) { states.get(block).add(state); }
      }
      int i;
      for (Map.Entry<Block, String> entry : blockIds.entrySet()) {
         i = 0;
         for (BlockState state : states.get(entry.getKey())) {
            staticBlockIds.put(entry.getValue() + ":" + i, state);
            i++;
         }
      }
   }

   public void save(Player player) {
      if (player == null || !player.level().isClientSide()) { return; }
      try {
         File dir = SchematicController.getDir();
         if (dir != null && !name.isEmpty()) {
            File file = new File(dir, name);
            NbtIo.writeCompressed(getNBT(), Files.newOutputStream(file.toPath()));
            player.sendSystemMessage(Component.literal("Save Schematic file: \"" + file + "\"").withStyle(ChatFormatting.GRAY));
            SchematicController sData = SchematicController.Instance;
            if (sData.map.containsKey(name)) { sData.map.put(name, new SchematicWrapper(this)); }
            if (CustomNpcs.VerboseDebug) { Util.instance.saveFile(new File(dir, name.replace(".schematic", "") + ".json"), getNBT()); }
         }
      } catch (Exception e) { LogWriter.error(e); }
   }

}
