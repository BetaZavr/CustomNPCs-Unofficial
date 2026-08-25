package noppes.npcs.blocks.tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.*;
import noppes.npcs.api.ILayerBlockModel;
import noppes.npcs.api.INbt;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.block.ITextPlane;
import noppes.npcs.api.wrapper.BlockScriptedWrapper;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.client.layer.block.LayerBlockModel;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.scripts.IScriptBlockHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.data.DataTimers;
import noppes.npcs.entity.data.TextBlock;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;

public class TileScripted extends TileNpcEntity implements IScriptBlockHandler {

   public class TextPlane implements ITextPlane {

      public boolean textHasChanged = true;
      public TextBlock textBlock;
      public String text = "";
      public int rotationX = 0;
      public int rotationY = 0;
      public int rotationZ = 0;
      public float offsetX = 0.0F;
      public float offsetY = 0.0F;
      public float offsetZ = 0.5F;
      public float scale = 1.0F;

      @Override
      public String getText() { return text; }

      public void setText(String textIn) {
         if (!text.equals(textIn) && textIn != null) {
            text = textIn;
            textHasChanged = true;
            needsClientUpdate = true;
         }
      }

      @Override
      public int getRotationX() { return rotationX; }

      @Override
      public int getRotationY() { return rotationY; }

      @Override
      public int getRotationZ() { return rotationZ; }

      @Override
      public void setRotationX(int x) {
         x = ValueUtil.correctInt(x % 360, 0, 359);
         if (rotationX != x) {
            rotationX = x;
            needsClientUpdate = true;
         }
      }

      @Override
      public void setRotationY(int y) {
         y = ValueUtil.correctInt(y % 360, 0, 359);
         if (rotationY != y) {
            rotationY = y;
            needsClientUpdate = true;
         }
      }

      @Override
      public void setRotationZ(int z) {
         z = ValueUtil.correctInt(z % 360, 0, 359);
         if (rotationZ != z) {
            rotationZ = z;
            needsClientUpdate = true;
         }
      }

      @Override
      public float getOffsetX() { return offsetX; }

      @Override
      public float getOffsetY() { return offsetY; }

      @Override
      public float getOffsetZ() { return offsetZ; }

      @Override
      public void setOffsetX(float x) {
         x = ValueUtil.correctFloat(x, -1.0F, 1.0F);
         if (offsetX != x) {
            offsetX = x;
            needsClientUpdate = true;
         }
      }

      @Override
      public void setOffsetY(float y) {
         y = ValueUtil.correctFloat(y, -1.0F, 1.0F);
         if (offsetY != y) {
            offsetY = y;
            needsClientUpdate = true;
         }
      }

      @Override
      public void setOffsetZ(float z) {
         z = ValueUtil.correctFloat(z, -1.0F, 1.0F);
         if (offsetZ != z) {
            offsetZ = z;
            needsClientUpdate = true;
         }
      }

      @Override
      public float getScale() { return scale; }

      @Override
      public void setScale(float scaleIn) {
         if (scale != scaleIn) {
            scale = scaleIn;
            needsClientUpdate = true;
         }
      }

      @Override
      public INbt getNbt() {
         CompoundTag compound = new CompoundTag();
         compound.putString("Text", text);
         compound.putInt("RotationX", rotationX);
         compound.putInt("RotationY", rotationY);
         compound.putInt("RotationZ", rotationZ);
         compound.putFloat("OffsetX", offsetX);
         compound.putFloat("OffsetY", offsetY);
         compound.putFloat("OffsetZ", offsetZ);
         compound.putFloat("Scale", scale);
         return new NBTWrapper(compound);
      }

      @Override
      public void setNbt(INbt nbt) {
         setText(nbt.getString("Text"));
         rotationX = nbt.getInteger("RotationX");
         rotationY = nbt.getInteger("RotationY");
         rotationZ = nbt.getInteger("RotationZ");
         offsetX = nbt.getFloat("OffsetX");
         offsetY = nbt.getFloat("OffsetY");
         offsetZ = nbt.getFloat("OffsetZ");
         scale = nbt.getFloat("Scale");
      }

   }

   protected IBlock blockDummy = null;
   protected short tickCount = 0;

   public List<ScriptContainer> scripts = new ArrayList<>();
   public String scriptLanguage = "ECMAScript";
   public DataTimers timers;

   public boolean renderTileErrored = true;
   public boolean needsClientUpdate = false;
   public boolean enabled = false;
   public boolean isPassable = false;
   public boolean isLadder = false;

   public int powering = 0;
   public int activePowering = 0;
   public int newPower = 0;
   public int prevPower = 0;
   public int lightValue = 0;

   public float blockHardness = 5.0f;
   public float blockResistance = 10.0f;

   public long lastInited = -1L;

   // render
   public BlockEntityTicker<BlockEntity> renderTileUpdate = null;
   public BlockEntity renderTile;
   public BlockState renderState;

   // New from Unofficial (BetaZavr)
   protected @Nonnull final List<ILayerBlockModel> layers = new ArrayList<>();
   protected @Nonnull final List<ITextPlane> textPlanes = new ArrayList<>();

   public TileScripted(BlockPos pos, BlockState state) {
      super(CustomBlocks.tile_scripted, pos, state);
      timers = new DataTimers(this);
      layers.add(new LayerBlockModel(0, new ItemStack(CustomBlocks.scripted), this));
      for (int i = 0; i < 7; i++) { textPlanes.add(new TextPlane()); }
   }

   @Override
   public IBlock getBlock() {
      if (blockDummy == null) {
         blockDummy = new BlockScriptedWrapper(level,
                 level == null ? CustomBlocks.scripted.defaultBlockState() : level.getBlockState(worldPosition),
                 worldPosition);
      }
      return blockDummy;
   }

   @Override
   public void load(@Nonnull CompoundTag compound) {
      super.load(compound);
      setNBT(compound);
      setDisplayNBT(compound);
      timers.load(compound);
   }

   public void setNBT(CompoundTag compound) {
      scripts = NBTTags.getScript(compound.getList("Scripts", 10), this);
      scriptLanguage = compound.getString("ScriptLanguage");
      enabled = compound.getBoolean("ScriptEnabled");
      activePowering = powering = compound.getInt("BlockPowering");
      prevPower = compound.getInt("BlockPrevPower");
      if (compound.contains("BlockHardness")) {
         blockHardness = compound.getFloat("BlockHardness");
         blockResistance = compound.getFloat("BlockResistance");
      }
   }

   public void setDisplayNBT(CompoundTag compound) {
      renderTileUpdate = null;
      renderTile = null;
      renderTileErrored = false;
      lightValue = compound.getInt("LightValue");
      isLadder = compound.getBoolean("IsLadder");
      isPassable = compound.getBoolean("IsPassible");
      textPlanes.clear();
      // old
      for (int i = 1; i < 7; i++) {
         String key = "Text" + i;
         if (compound.contains(key, 10)) {
            TextPlane textPlane = new TextPlane();
            textPlane.setNbt(new NBTWrapper(compound.getCompound(key)));
            textPlanes.add(textPlane);
         }
      }
      // new
      if (compound.contains("TextPlanes", 9)) {
         ListTag list = compound.getList("TextPlanes", 10);
         for (int i = 0; i < list.size(); i++) {
            TextPlane textPlane = new TextPlane();
            textPlane.setNbt(new NBTWrapper(list.getCompound(i)));
            textPlanes.add(textPlane);
         }
      }
      // New from Unofficial (BetaZavr)
      layers.clear();
      for (int i = 0; i < compound.getList("Layers", 10).size(); i++) {
         LayerBlockModel lbm = new LayerBlockModel(this);
         lbm.setNbt(new NBTWrapper(compound.getList("Layers", 10).getCompound(i)));
         layers.add(lbm);
      }
      if (layers.isEmpty()) { layers.add(new LayerBlockModel(0, new ItemStack(CustomBlocks.scripted), this)); }
   }

   @Override
   public void saveAdditional(@Nonnull CompoundTag compound) {
      save(compound);
      saveDisplayNBT(compound);
      timers.save(compound);
      super.saveAdditional(compound);
   }

   public CompoundTag save(CompoundTag compound) {
      compound.put("Scripts", NBTTags.nbtScript(scripts));
      compound.putString("ScriptLanguage", scriptLanguage);
      compound.putBoolean("ScriptEnabled", enabled);
      compound.putInt("BlockPowering", powering);
      compound.putInt("BlockPrevPower", prevPower);
      compound.putFloat("BlockHardness", blockHardness);
      compound.putFloat("BlockResistance", blockResistance);
      return compound;
   }

   public void saveDisplayNBT(CompoundTag compound) {
      compound.putInt("LightValue", lightValue);
      compound.putBoolean("IsLadder", isLadder);
      compound.putBoolean("IsPassible", isPassable);

      // New from Unofficial (BetaZavr)
      ListTag list = new ListTag();
      for (ITextPlane textPlane : new ArrayList<>(textPlanes)) { list.add(textPlane.getNbt().getMCNBT()); }
      compound.put("TextPlanes", list);
      list = new ListTag();
      for (ILayerBlockModel layer : layers) { list.add(layer.getNbt().getMCNBT()); }
      compound.put("Layers", list);
   }

   @Override
   public boolean isEnabled() { return CustomNpcs.EnableScripting && enabled && ScriptController.HasStart && !scripts.isEmpty() && level != null && !level.isClientSide; }

   @Override
   public void clearConsoleText(Long key) {
      for (ScriptContainer script : getScripts()) { script.console.remove(key); }
   }

   @Override
   public void setLastInited(long timeMC) { lastInited = timeMC; }

   public static void tick(Level level, BlockPos pos, BlockState state, TileScripted tile) {
      if (tile.renderTileUpdate != null) {
         try { tile.renderTileUpdate.tick(level, pos, tile.renderState, tile.renderTile); }
         catch (Exception var5) { tile.renderTileUpdate = null; }
      }
      ++tile.tickCount;
      if (tile.prevPower != tile.newPower && tile.powering <= 0) {
         EventHooks.onScriptBlockRedstonePower(tile, tile.prevPower, tile.newPower);
         tile.prevPower = tile.newPower;
      }
      tile.timers.update();
      if (tile.tickCount >= 10) {
         if (tile.isEnabled()) {
            ScriptController.Instance.tryAdd(0, tile);
            EventHooks.onScriptBlockUpdate(tile);
         }
         tile.tickCount = 0;
      }
      if (tile.needsClientUpdate && !level.isClientSide()) {
         tile.setChanged();
         level.sendBlockUpdated(pos, state, state, 3);
         tile.needsClientUpdate = false;
      }
   }

   @Override
   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) { handleUpdateTag(pkt.getTag()); }

   @Override
   public void handleUpdateTag(CompoundTag tag) {
      int light = lightValue;
      setDisplayNBT(tag);
      if (light != lightValue && level != null) {
         level.getLightEngine().checkBlock(worldPosition);
      }
   }

   @Override
   public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

   @Override
   public @Nonnull CompoundTag getUpdateTag() {
      CompoundTag compound = new CompoundTag();
      compound.putInt("x", worldPosition.getX());
      compound.putInt("y", worldPosition.getY());
      compound.putInt("z", worldPosition.getZ());
      saveDisplayNBT(compound);
      return compound;
   }

   public void setLightValue(int value) {
      if (value != lightValue) {
         lightValue = ValueUtil.correctInt(value, 0, 15);
         needsClientUpdate = true;
      }
   }

   public void setRedstonePower(int strength) {
      if (powering != strength) {
         prevPower = activePowering = ValueUtil.correctInt(strength, 0, 15);
         if (level != null) { level.updateNeighborsAt(worldPosition, CustomBlocks.scripted); }
         powering = activePowering;
      }
   }

   public ILayerBlockModel getMainModel() {
      ILayerBlockModel lbm = layers.get(0);
      if (lbm == null) {
         lbm = new LayerBlockModel(0, new ItemStack(CustomBlocks.scripted), this);
         layers.add(0, lbm);
         int i = 0;
         for (ILayerBlockModel model : new ArrayList<>(layers)) {
            if (model instanceof LayerBlockModel layer) { layer.setId(i); }
            i++;
         }
         needsClientUpdate = true;
      }
      return lbm;
   }

   public ILayerBlockModel createLayerModel() {
      if (layers.size() >= 25) { return layers.get(24); }
      LayerBlockModel lbm = new LayerBlockModel(this);
      layers.add(lbm);
      int i = 0;
      for (ILayerBlockModel model : new ArrayList<>(layers)) {
         if (model instanceof LayerBlockModel layer) { layer.setId(i); }
         i++;
      }
      return lbm;
   }

   public boolean removeLayerModel(ILayerBlockModel layer) {
      if (!(layer instanceof LayerBlockModel)) { return false; }
      return layers.remove(layer);
   }

   @Override
   public void runScript(String type, Event event) {
      if (isEnabled()) {
         if (ScriptController.Instance.lastLoaded > lastInited) {
            lastInited = ScriptController.Instance.lastLoaded;
            if (!type.equals(EnumScriptType.INIT.function)) { EventHooks.onScriptBlockInit(this); }
         }
         for (ScriptContainer script : scripts) { script.run(type, event); }
      }
   }

   @Override
   public boolean isClient() { return level == null || level.isClientSide; }

   @Override
   public boolean getEnabled() { return enabled; }

   @Override
   public void setEnabled(boolean bo) { enabled = bo; }

   @Override
   public MutableComponent noticeString(String type, Object event) {
      MutableComponent message = Component.literal("Scripted Block")
              .withStyle(ChatFormatting.DARK_GRAY);
      if (type != null) {
         message.append(Component.literal(" hook \"").withStyle(ChatFormatting.DARK_GRAY))
                 .append(Component.literal(type).withStyle(ChatFormatting.GRAY))
                 .append(Component.literal("\"; ").withStyle(ChatFormatting.DARK_GRAY));
      }
      else { message.append(Component.literal("; ").withStyle(ChatFormatting.DARK_GRAY)); }
      String dimID = level == null ? "null" : level.dimensionTypeId().location().toString();
      double x = 0.5d + Math.round(worldPosition.getX() * 100.0d) / 100.0d;
      double y = 0.5d + Math.round(worldPosition.getY() * 100.0d) / 100.0d;
      double z = 0.5d + Math.round(worldPosition.getZ() * 100.0d) / 100.0d;
      MutableComponent posClick = Component.literal("dimension ID:" + dimID + "; X:" + x + "; Y:" + y + "; Z:" + z);
      Style style = posClick.getStyle().withColor(ChatFormatting.BLUE);
      style = style.withUnderlined(true);
      style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/noppes world tp @p " + dimID + " " + x + " " + y + " "+z));
      style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("script.hover.error.pos.tp")));
      posClick.setStyle(style);
      message.append(Component.literal("in ").withStyle(ChatFormatting.DARK_GRAY))
              .append(posClick);
      return message.append(Component.literal("; Side: " + (isClient() ? "Client" : "Server")).withStyle(ChatFormatting.DARK_GRAY));
   }

   @Override
   public String getLanguage() { return scriptLanguage; }

   @Override
   public void setLanguage(String lang) {
      if (lang == null || lang.isEmpty()) { lang = "ECMAScript"; }
      scriptLanguage = lang;
   }

   @Override
   public List<ScriptContainer> getScripts() { return scripts; }

   @Override
   public Map<Long, String> getConsoleText() {
      TreeMap<Long, String> map = new TreeMap<>();
      int tab = 0;
      for (ScriptContainer script : getScripts()) {
         ++tab;
         for (Map.Entry<Long, String> entry : script.console.entrySet()) {
            String log;
            if (map.containsKey(entry.getKey())) { log = map.get(entry.getKey()) + "\n\n" + "ScriptTab " + tab + ":\n" + entry.getValue(); }
            else { log = " ScriptTab " + tab + ":\n" + entry.getValue(); }
            map.put(entry.getKey(), log);
         }
      }
      return map;
   }

   @Override
   public void clearConsole() { for (ScriptContainer script : getScripts()) { script.console.clear(); } }

   @OnlyIn(Dist.CLIENT)
   @Override
   public AABB getRenderBoundingBox() {
      double minX = 0.0f;
      double minY = 0.0f;
      double minZ = 0.0f;
      double maxX = 0.0f;
      double maxY = 0.0f;
      double maxZ = 0.0f;
      for (ILayerBlockModel layer : new ArrayList<>(layers)) {
         AABB aabb = layer.getBoundingBox();
         if (minX > aabb.minX) { minX = aabb.minX; }
         if (minY > aabb.minY) { minY = aabb.minY; }
         if (minZ > aabb.minZ) { minZ = aabb.minZ; }
         if (maxX < aabb.maxX) { maxX = aabb.maxX; }
         if (maxY < aabb.maxY) { maxY = aabb.maxY; }
         if (maxZ < aabb.maxZ) { maxZ = aabb.maxZ; }
      }
      return new AABB(minX, minY, minZ, maxX, maxY, maxZ).move(worldPosition);
   }

   @Override
   public void init() { lastInited = -1; }

   // New from Unofficial (BetaZavr)
   public @Nonnull List<ILayerBlockModel> getLayers() { return layers; }

   public @Nonnull List<ITextPlane> getTextPlanes() { return textPlanes; }

}
