package noppes.npcs.schematics;

import java.util.*;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.wrapper.WorldWrapper;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemBuilder;
import noppes.npcs.items.ItemPlacer;
import noppes.npcs.mixin.world.entity.decoration.IHangingEntityMixin;
import noppes.npcs.util.BuilderData;
import noppes.npcs.util.ValueUtil;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;

public class SchematicWrapper {

   @SuppressWarnings("deprecation")
   public static BlockState rotationState(BlockState state, int rotation) {
      if (rotation == 0) { return state; }
      Rotation rot = switch (rotation) {
         case 1 -> Rotation.CLOCKWISE_90;
         case 2 -> Rotation.CLOCKWISE_180;
         default -> Rotation.COUNTERCLOCKWISE_90;
      };
      if (state.getBlock() instanceof VineBlock ||
              state.getBlock() instanceof BannerBlock) {
         return state.getBlock().rotate(state, rot);
      }
      if (state.getBlock() instanceof RotatedPillarBlock) { return RotatedPillarBlock.rotatePillar(state, rot); }
      for (Property<?> property : state.getProperties()) {
         if (property instanceof DirectionProperty) {
            Direction direction = state.getValue((DirectionProperty) property);
            if (direction != Direction.UP && direction != Direction.DOWN) {
               for (int i = 0; i < rotation; ++i) { direction = direction.getClockWise(); }
               return state.setValue((DirectionProperty) property, direction);
            }
         }
      }
      return state;
   }

   // New from Unofficial (BetaZavr)
   public static Entity rotatePos(Entity entity, int rotation, BlockPos pos, BlockPos offset) {
      if (entity == null) {
         return null;
      }
      double x, y, z;
      if (entity instanceof HangingEntity eh) {
         x = eh.getX();
         y = eh.getY() - offset.getY();
         z = eh.getZ();
         eh.setYRot((eh.getYRot() + (float) rotation * 90.0f) % 360.0f);
         switch (rotation) {
            case 1:
            case 2:
               x += offset.getX() * -1.0d;
               z += -1.0d - offset.getZ();
               break;
            case 3:
               x += 1.0d + offset.getX() * -1.0d;
               z += -1.0d - offset.getZ();
               break;
            default:
               x -= offset.getX();
               z -= offset.getZ();
               break;
         }
         x += pos.getX();
         y += pos.getY();
         z += pos.getZ();
         for (int i = 0; i < rotation; i++) { resetDirection(eh); }
         entity.setPos(x, y, z);
         return entity;
      }
      x = entity.getX();
      y = entity.getY();
      z = entity.getZ();
      switch (rotation) {
         case 1:
            x = 1.0d + offset.getZ() - entity.getZ();
            z = 1.0d + entity.getX() + offset.getX() * -1.0d;
            break;
         case 2:
            x = 1.0d + entity.getX() * -1.0d + offset.getX();
            z = 1.0d + entity.getZ() * -1.0d + offset.getZ();
            break;
         case 3:
            x = 1.0d + entity.getZ() - offset.getZ();
            z = 1.0d + entity.getX() * -1.0d + offset.getX();
            break;
         default:
            x += 1.0d - offset.getX();
            z += 1.0d - offset.getZ();
            break;
      }
      entity.setYRot((entity.getYRot() + (float) rotation * 90.0f) % 360.0f);
      entity.setPos(x + pos.getX() + 0.5d, y + pos.getY(), z + pos.getZ() + 0.5d);
      if (entity instanceof PathfinderMob mob) { mob.restrictTo(entity.getOnPos(), (int) mob.getRestrictRadius()); }
      if (entity instanceof EntityNPCInterface npc) { npc.ais.orientation = (npc.ais.orientation + rotation * 90) % 360; }
      return entity;
   }

   private static void resetDirection(HangingEntity parent) {
      Direction direction = parent.getDirection().getClockWise();
      Validate.notNull(direction);
      Validate.isTrue(direction.getAxis().isHorizontal());
      ((IHangingEntityMixin) parent).setDirection(direction);
      parent.setYRot((float) (direction.get2DDataValue() * 90));
      parent.yRotO = parent.getYRot();
      BlockPos pos = ((IHangingEntityMixin) parent).getPos();
      double d0 = (double) pos.getX() + 0.5D;
      double d1 = (double) pos.getY() + 0.5D;
      double d2 = (double) pos.getZ() + 0.5D;
      double d3 = 0.46875D;
      double d4 = parent.getWidth() % 32 == 0 ? 0.5D : 0.0D;
      double d5 = parent.getHeight() % 32 == 0 ? 0.5D : 0.0D;
      d0 -= (double) direction.getStepX() * d3;
      d2 -= (double) direction.getStepZ() * d3;
      d1 += d5;
      Direction clockWiseDirection = direction.getCounterClockWise();
      d0 += d4 * (double) clockWiseDirection.getStepX();
      d2 += d4 * (double) clockWiseDirection.getStepZ();
      parent.setPosRaw(d0, d1, d2);
      double d6 = parent.getWidth();
      double d7 = parent.getHeight();
      double d8 = parent.getWidth();
      if (direction.getAxis() == Direction.Axis.Z) { d8 = 1.0D; }
      else { d6 = 1.0D; }
      d6 /= 32.0D;
      d7 /= 32.0D;
      d8 /= 32.0D;
      parent.setBoundingBox(new AABB(d0 - d6, d1 - d7, d2 - d8, d0 + d6, d1 + d7, d2 + d8));
   }

   protected final TreeMap<Integer, HashMap<ChunkPos, CompoundTag>> tileEntities = new TreeMap<>();
   protected @Nullable Level level;
   public BlockPos start = BlockPos.ZERO;

   public ISchematic schema;
   public int buildPos;
   public int size;
   public int rotation = 0;
   public boolean isBuilding = false;

   // New from Unofficial (BetaZavr)
   protected List<SchematicBlockData> listB = new ArrayList<>();
   protected List<Entity> listE = new ArrayList<>();
   protected boolean isBlock = true;
   protected BuilderData builder = null;
   protected long time = 0L;
   public CommandSourceStack sender = null;
   public int buildingPercentage;
   public int layer = 0;

   public SchematicWrapper(ISchematic schematic) {
      schema = schematic;
      size = schematic.getWidth() * schematic.getHeight() * schematic.getLength();
      for(int i = 0; i < schematic.getBlockEntityDimensions(); ++i) {
         CompoundTag teTag = schematic.getBlockEntity(i);
         int x = teTag.getInt("x");
         int y = teTag.getInt("y");
         int z = teTag.getInt("z");
         if (!tileEntities.containsKey(y)) { tileEntities.put(y, new HashMap<>()); }
         tileEntities.get(y).put(new ChunkPos(x, z), teTag);
      }
   }

   public void build() {
      if (level != null && isBuilding) {
         long endPos = ValueUtil.correctLong(buildPos + CustomNpcs.MaxBuilderBlocks, 0, size);
         // blocks first and next types
         if (layer < 2) {
            if (layer == 0 && builder != null) {
               listB = new ArrayList<>();
               listE = new ArrayList<>();
               BlockPos ps = start;
               int x = rotation % 2 == 0 ? schema.getWidth() : schema.getLength();
               int y = schema.getHeight();
               int z = rotation % 2 == 0 ? schema.getLength() : schema.getWidth();
               BlockPos pe = x == 0.0D && y == 0.0D && z == 0.0D ? start : new BlockPos(start.getX() + x, start.getY() + y, start.getZ() + z);
               try {
                  for (Entity e : level.getEntitiesOfClass(Entity.class,
                          new AABB(ps.getX() - 0.5d, ps.getY() - 0.5d, ps.getZ() - 0.5d,
                                  pe.getX() + 0.5d, pe.getY() + 0.5d, pe.getZ() + 0.5d),
                          (entity) -> !(entity instanceof Projectile || entity instanceof Arrow || entity instanceof Player))) {
                     listE.add(e);
                     e.discard();
                  }
               }
               catch (Exception ignored) { }
            } // remove Entity
            long t = System.currentTimeMillis();
            while (buildPos < endPos) {
               int x = buildPos % schema.getWidth();
               int z = (buildPos - x) / schema.getWidth() % schema.getLength();
               int y = ((buildPos - x) / schema.getWidth() - z) / schema.getLength();
               SchematicBlockData sbd = place(x, y, z, layer == 0);
               if (sbd != null) { listB.add(sbd); }
               ++buildPos;
            }
            time += System.currentTimeMillis() - t;
         }
         if (buildPos >= size) {
            switch (layer) {
               case 0: {
                  layer = 1;
                  buildPos = 0;
                  break;
               } // next blocks
               case 1: {
                  if (schema.hasEntitys()) {
                     ListTag list = schema.getEntitys();
                     for (int i = 0; i < list.size(); i++) { spawn(list.getCompound(i)); }
                  }
                  layer = 2;
                  SchematicController.time = time / ((long) schema.getHeight() * schema.getLength() * schema.getWidth());
                  time = 0L;
                  break;
               } // entitys
               default: {
                  layer = 3;
                  isBuilding = false;
                  if (builder != null) { builder.add(listB, listE); }
               }
            }
         }
      }
   }

   public CompoundTag getNBTSmall() {
      CompoundTag compound = new CompoundTag();
      compound.putShort("Width", schema.getWidth());
      compound.putShort("Height", schema.getHeight());
      compound.putShort("Length", schema.getLength());
      compound.putString("SchematicName", schema.getName());
      ListTag list = new ListTag();
      for(int i = 0; i < size && i < 25000; ++i) {
         BlockState state = schema.getBlockState(i);
         if (state.getBlock() != Blocks.AIR && state.getBlock() != Blocks.STRUCTURE_VOID) { list.add(NbtUtils.writeBlockState(schema.getBlockState(i))); }
         else { list.add(new CompoundTag()); }
      }
      compound.put("Data", list);
      return compound;
   }

   public int getPercentage() {
      double l = buildPos + (layer == 0 ? 0 : size);
      return (int) (l / size * 50.0);
   }

   public CompoundTag getBlockEntity(int x, int y, int z, BlockPos pos) {
      if (y < tileEntities.size() && tileEntities.containsKey(y)) {
         CompoundTag compound = tileEntities.get(y).get(new ChunkPos(x, z));
         if (compound == null) { return null; }
         compound = compound.copy();
         compound.putInt("x", pos.getX());
         compound.putInt("y", pos.getY());
         compound.putInt("z", pos.getZ());
         return compound;
      }
      return null;
   }

   public void init(BlockPos pos, Level levelIn, int rotationIn) {
      start = pos;
      level = levelIn;
      rotation = rotationIn;
      isBuilding = true;
      buildingPercentage = 0;
      layer = 0;
      isBlock = true;
      time = 0L;
      buildPos = 0;
   }

   /**
    * place block in world
    *
    * @param x,y,z - BlockPos
    * @param firstLayer - not Air and FullBlock, next vice versa
    */
   @SuppressWarnings("ConstantConditions")
   public  SchematicBlockData place(int x, int y, int z, boolean firstLayer) {
      BlockState state = schema.getBlockState(x, y, z);
      if (state == null ||
              (firstLayer && !state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) && state.getBlock() != Blocks.AIR) ||
              (!firstLayer && (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) || state.getBlock() == Blocks.AIR))) {

         return null;
      }
      int rot = rotation / 90;
      BlockPos rotPos = rotatePos(x, y, z, rot);
      BlockPos pos = rotPos.getX() == 0 && rotPos.getY() == 0 && rotPos.getZ() == 0 ?
              start :
              new BlockPos(start.getX() + rotPos.getX(), start.getY() + rotPos.getY(), start.getZ() + rotPos.getZ());
      SchematicBlockData sbd = new SchematicBlockData(level, level.getBlockState(pos), pos);
      state = rotationState(state, rot);
      if (builder != null) {
         if (state.getBlock() == Blocks.AIR && !builder.addAir) { return null; } // not place air
         if (sbd.state != null) {
            if (!builder.replaceAir && (sbd.state.getBlock() != Blocks.AIR || sbd.state.isValidSpawn(level, pos, EntityType.PLAYER))) { return null; } // not place air
            if (state.canBeReplaced() && builder.isSolid) { return null; } // not solid place
         }
      }
      level.setBlock(pos, state, 2);
      if (state.getBlock() instanceof EntityBlock) {
         BlockEntity tile = level.getBlockEntity(pos);
         if (tile != null) {
            CompoundTag comp = getBlockEntity(x, y, z, pos);
            if (comp != null) {
               if (rot != 0 && state.getBlock() instanceof SkullBlock && comp.contains("Rot", 1)) {
                  byte d = comp.getByte("Rot");
                  for (int i = 0; i < rot; ++i) { d += (byte) 4; }
                  d %= (byte) 16;
                  comp.putByte("Rot", d);
               }
               tile.load(comp);
            }
         }
      }
      level.setBlock(pos, state, 2);
      return sbd;
   }

   public BlockPos rotatePos(int x, int y, int z, int rotation) {
      return switch (rotation) {
         case 1 -> new BlockPos(schema.getLength() - z - 1, y, x);
         case 2 -> new BlockPos(schema.getWidth() - x - 1, y, schema.getLength() - z - 1);
         case 3 -> new BlockPos(z, y, schema.getWidth() - x - 1);
         default -> new BlockPos(x, y, z);
      };
   }

   public void setBuilder(CommandSourceStack senderIn) {
      sender = senderIn;
      isBuilding = true;
      buildingPercentage = 0;
      isBlock = false;
      ServerPlayer player = sender.getPlayer();
      if (player != null && player.getMainHandItem().getItem() instanceof ItemPlacer) {
         builder = ItemBuilder.getBuilder(player.getMainHandItem(), player);
      }
   }

   @SuppressWarnings("ConstantConditions")
   public void spawn(CompoundTag entityNbt) {
      entityNbt.putString("id", NoppesUtilServer.validLocation(entityNbt.getString("id")));
      Entity entity = EntityType.create(entityNbt, level).orElse(null);
      if (entity != null) {
         UUID uuid = entity.getUUID();
         List<Entity> entities = WorldWrapper.createNew(level).getEntities(Entity.class, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
         while (uuid != null) {
            boolean has = false;
            for (Entity e : entities) {
               if (e.getUUID().equals(entity.getUUID())) {
                  uuid = UUID.randomUUID();
                  entity.setUUID(uuid);
                  has = true;
                  break;
               }
            }
            if (has) { continue; }
            uuid = null;
         }
         entity = rotatePos(entity, rotation / 90, start, schema.getOffset().getMCBlockPos());
         level.addFreshEntity(entity);
         if (entity instanceof EntityNPCInterface npc) { npc.reset(50); }
      }
   }

}
