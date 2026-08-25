package noppes.npcs.api.wrapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.INbt;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.wrapper.data.Data;
import noppes.npcs.blocks.BlockScripted;
import noppes.npcs.blocks.BlockScriptedDoor;
import noppes.npcs.blocks.tiles.TileNpcEntity;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.world.entity.IEntityMixin;
import noppes.npcs.shared.common.util.LRUHashMap;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockWrapper implements IBlock {

   /*
    * Used in:
    * A large number of Forge events
    * When checking vision when an NPC is looking at a target
    * Mod events and scripts
    */
   private static final Map<ResourceKey<Level>, LRUHashMap<Long, BlockWrapper>> dimensionCaches = new ConcurrentHashMap<>();
   private static final int MAX_PER_DIMENSION = 16384;

   public static BlockWrapper AIR = new BlockWrapper(null, Blocks.AIR.defaultBlockState(), null);
   public static void clearCache() { dimensionCaches.clear(); }
   public static BlockWrapper createNew(@Nullable Level level, @Nullable BlockPos pos, @Nonnull BlockState state) {
      if (level == null || pos == null) { return createBlockWrapper(level, state, pos); }
      ResourceKey<Level> dimId = level.dimension();
      long key = pos.asLong();
      LRUHashMap<Long, BlockWrapper> cache = dimensionCaches.computeIfAbsent(dimId, k -> new LRUHashMap<>(MAX_PER_DIMENSION));
      BlockWrapper wrapper = cache.get(key);
      if (wrapper != null && !wrapper.isStale(level, pos, state)) {
         return wrapper;
      }
      wrapper = createBlockWrapper(level, state, pos);
      cache.put(key, wrapper);
      return wrapper;
   }
   private static BlockWrapper createBlockWrapper(@Nullable Level level, @Nonnull BlockState state, @Nullable BlockPos pos) {
      Block block = state.getBlock();
      BlockWrapper wrapper;
      if (block instanceof BlockScripted) { wrapper = new BlockScriptedWrapper(level, state, pos); }
      else if (block instanceof BlockScriptedDoor) { wrapper = new BlockScriptedDoorWrapper(level, state, pos); }
      else if (block instanceof IFluidBlock) { wrapper = new BlockFluidContainerWrapper(level, state, pos); }
      else { wrapper = new BlockWrapper(level, state, pos); }
      if (level != null && pos != null) { wrapper.setTile(level.getBlockEntity(pos)); }
      return wrapper;
   }
   public static BlockWrapper of(CompoundTag compound) {
      Level level = CustomNpcs.proxy.overworld();
      BlockState state;
      if (level == null) { state = Blocks.AIR.defaultBlockState(); }
      else {
         LogWriter.info("[DEBUG] compound "+compound);
         state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), compound);
         LogWriter.info("[DEBUG] state "+state);
      }
      BlockPos pos = BlockPos.of(compound.getLong("BlockPos"));
      Block block = state.getBlock();
      if (block instanceof BlockScripted) { return new BlockScriptedWrapper(level, state, pos); }
      else if (block instanceof BlockScriptedDoor) { return new BlockScriptedDoorWrapper(level, state, pos); }
      else if (block instanceof IFluidBlock) { return new BlockFluidContainerWrapper(level, state, pos); }
      return new BlockWrapper(level, state, pos);
   }

   protected final @Nullable IWorld level;
   protected final @Nonnull BlockPosWrapper iPos;
   protected @Nullable BlockEntity tile;
   protected BlockState state;
   protected TileNpcEntity storage;

   private IData storeddata = new Data();
   private IData tempdata = new Data();

   protected BlockWrapper(@Nullable Level levelIn, @Nonnull BlockState stateIn, @Nullable BlockPos posIn) {
      level = levelIn == null ? null : Objects.requireNonNull(NpcAPI.Instance()).getIWorld(levelIn);
      state = stateIn;
      iPos = posIn == null ? BlockPosWrapper.ZERO : new BlockPosWrapper(levelIn, posIn);
      if (level != null) { setTile(level.getMCLevel().getBlockEntity(iPos.blockPos)); }
   }

   @Override
   public int getX() { return iPos.blockPos.getX(); }

   @Override
   public int getY() { return iPos.blockPos.getY(); }

   @Override
   public int getZ() { return iPos.blockPos.getZ(); }

   @Override
   public IPos getPos() { return iPos; }

   @Override
   @SuppressWarnings("unchecked")
   public <T extends Comparable<T>> T getProperty(String name) {
      BlockState st = getMCBlockState();
      for (Property<?> p : st.getProperties()) {
         if (p.getName().equalsIgnoreCase(name)) { return (T) st.getValue(p); }
      }
      throw new CustomNPCsException("Unknown property: " + name + " for block " + st);
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T extends Comparable<T>> void setProperty(String name, Comparable<T> value) {
      BlockState st = getMCBlockState();
      for (Property<?> p : st.getProperties()) {
         if (p.getName().equalsIgnoreCase(name)) {
            setPropertyValue((Property<T>) p, value);
            return;
         }
      }
      throw new CustomNPCsException("Unknown property: " + name + " for block " + st);
   }

   private <T extends Comparable<T>> void setPropertyValue(Property<T> p, Comparable<T> c) {
      if (level != null) {
         level.getMCLevel().setBlock(iPos.blockPos, getMCBlockState().setValue(p, p.getValueClass().cast(c)), 3);
         CustomNPCsScheduler.runTack(() -> setTile(level.getMCLevel().getBlockEntity(iPos.blockPos)), 60);
      }
   }

   @Override
   public List<String> getProperties() {
      Collection<Property<?>> props = getMCBlockState().getProperties();
      List<String> list = new ArrayList<>();
      for (Property<?> prop : props) { list.add(prop.getName()); }
      return list;
   }

   @Override
   public void remove() {
      if (level != null) { level.getMCLevel().removeBlock(iPos.blockPos, false); }
   }

   @Override
   public boolean isRemoved() {
      return level == null || !level.getMCLevel().getBlockState(iPos.blockPos).equals(state);
   }

   @Override
   public boolean isAir() { return getMCBlockState().isAir(); }

   @Override
   public BlockWrapper setBlock(String name) {
      if (level != null) {
         Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse(name));
         if (block != null) {
            BlockState st = block.defaultBlockState();
            level.getMCLevel().setBlock(iPos.blockPos, st, 2);
            return new BlockWrapper(level.getMCLevel(), st, iPos.blockPos);
         }
      }
      return this;
   }

   @Override
   public BlockWrapper setBlock(IBlock iBlock) {
      IWorld iLevel = iBlock.getWorld();
      if (iLevel == null) { iLevel = level; }
      if (iLevel != null) {
         BlockState st = iBlock.getMCBlockState();
         iLevel.getMCLevel().setBlock(iPos.blockPos, st, 2);
         return new BlockWrapper(iLevel.getMCLevel(), st, iPos.blockPos);
      }
      return new BlockWrapper(null, iBlock.getMCBlockState(), iPos.blockPos);
   }

   @Override
   public boolean isContainer() { return tile != null && tile instanceof Container container && container.getContainerSize() > 0; }

   @Override
   public IContainer getContainer() {
      if (!isContainer()) { throw new CustomNPCsException("This block is not a container"); }
      return Objects.requireNonNull(NpcAPI.Instance()).getIContainer((Container) tile);
   }

   @Override
   public IData getTempdata() { return tempdata; }

   @Override
   public IData getStoreddata() { return storeddata; }

   @Override
   public String getName() { return Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(getMCBlockState().getBlock())).toString(); }

   @Override
   public String getStateName() { return getMCBlockState().toString(); }

   @Override
   public String getDisplayName() { return tile != null && tile instanceof Nameable ? ((Nameable) tile).getDisplayName().getString() : getName(); }

   @Override
   public @Nullable IWorld getWorld() { return level; }

   @Override
   public Block getMCBlock() { return getMCBlockState().getBlock(); }

   @Override
   public boolean hasTileEntity() { return tile != null; }

   protected void setTile(BlockEntity tileIn) {
      tile = tileIn;
      if (tile instanceof TileNpcEntity) {
         storage = (TileNpcEntity) tile;
         tempdata = storage.tempData;
         storeddata = storage.storedData;
      }
   }

   @Override
   public INbt getBlockEntityNBT() {
      if (tile == null) { throw new CustomNPCsException("This block is not a entity"); }
      CompoundTag compound = tile.saveWithoutMetadata();
      return new NBTWrapper(compound);
   }

   @SuppressWarnings("unused")
   public INbt getTileEntityNBT() { return getBlockEntityNBT(); }

   @SuppressWarnings("unused")
   public void setBlockEntityNBT(INbt nbt) { setTileEntityNBT(nbt); }

   @Override
   public void setTileEntityNBT(INbt nbt) {
      if (tile == null) { throw new CustomNPCsException("This block is not a entity"); }
      tile.load(nbt.getMCNBT());
      tile.setChanged();
      if (level != null) {
         BlockState st = getMCBlockState();
         level.getMCLevel().sendBlockUpdated(iPos.blockPos, st, st, 3);
      }
   }

   @Override
   public BlockEntity getMCTileEntity() { return tile; }

   @Override
   public @Nonnull BlockState getMCBlockState() { return level == null ? state : level.getMCLevel().getBlockState(iPos.blockPos); }

   @Override
   public void blockEvent(int type, int data) {
      if (level != null) { level.getMCLevel().blockEvent(iPos.blockPos, getMCBlock(), type, data); }
   }

   @Override
   public void interact(int side) {
      if (level != null) {
         Player player = EntityNPCInterface.GenericPlayer;
         ((IEntityMixin) player).setLevel(level.getMCLevel());
         player.setPos(iPos.getX(), iPos.getY(), iPos.getZ());
         getMCBlockState().use(player.level(), EntityNPCInterface.CommandPlayer, InteractionHand.MAIN_HAND,
                 new BlockHitResult(Vec3.ZERO, Direction.from3DDataValue(side), iPos.blockPos, true));
      }
   }

   @Override
   public boolean isEmpty() { return state == null ? getMCBlock() == Blocks.AIR : state.isAir(); }

   public TileNpcEntity getStorage() { return storage; }

   public @Nonnull BlockState getState() { return state; }

   public @Nullable BlockEntity getTile() { return tile; }

   public CompoundTag save() {
      CompoundTag compound = NbtUtils.writeBlockState(state != null ? state : getMCBlockState());
      compound.putLong("BlockPos", iPos.blockPos.asLong());
      return compound;
   }

   public boolean isStale(@Nullable Level levelIn, @Nullable BlockPos pos, @Nonnull BlockState state) {
      if (levelIn == null || level == null) { return false; }
      if (level.getMCLevel() != levelIn) { return true; }
      if (pos != null && !iPos.blockPos.equals(pos)) { return true; }
      return !levelIn.getBlockState(iPos.blockPos).equals(state);
   }

}
