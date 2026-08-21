package noppes.npcs.blocks.tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.wrapper.BlockScriptedDoorWrapper;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.scripts.IScriptBlockHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.data.DataTimers;
import org.jetbrains.annotations.NotNull;

public class TileScriptedDoor extends TileDoor implements IScriptBlockHandler {

   protected IBlock blockDummy = null;
   public List<ScriptContainer> scripts = new ArrayList<>();
   public String scriptLanguage = "ECMAScript";
   public DataTimers timers = new DataTimers(this);
   public boolean enabled = false;
   public int newPower = 0;
   public int prevPower = 0;
   private short tickCount = 0;
   public float blockHardness = 5.0F;
   public float blockResistance = 10.0F;
   public long lastInited = -1L;

   public TileScriptedDoor(BlockPos pos, BlockState state) { super(CustomBlocks.tile_scripteddoor, pos, state); }

   @Override
   public IBlock getBlock() {
      if (blockDummy == null) { blockDummy = new BlockScriptedDoorWrapper(getLevel(), CustomBlocks.scripted_door.defaultBlockState(), getBlockPos()); }
      return blockDummy;
   }

   @Override
   public void load(@NotNull CompoundTag compound) {
      super.load(compound);
      setNBT(compound);
      timers.load(compound);
   }

   public void setNBT(CompoundTag compound) {
      scripts = NBTTags.getScript(compound.getList("Scripts", 10), this);
      scriptLanguage = compound.getString("ScriptLanguage");
      enabled = compound.getBoolean("ScriptEnabled");
      prevPower = compound.getInt("BlockPrevPower");
      if (compound.contains("BlockHardness")) {
         blockHardness = compound.getFloat("BlockHardness");
         blockResistance = compound.getFloat("BlockResistance");
      }

   }

   @Override
   public void saveAdditional(@NotNull CompoundTag compound) {
      getNBT(compound);
      timers.save(compound);
      super.saveAdditional(compound);
   }

   public CompoundTag getNBT(CompoundTag compound) {
      compound.put("Scripts", NBTTags.nbtScript(scripts));
      compound.putString("ScriptLanguage", scriptLanguage);
      compound.putBoolean("ScriptEnabled", enabled);
      compound.putInt("BlockPrevPower", prevPower);
      compound.putFloat("BlockHardness", blockHardness);
      compound.putFloat("BlockResistance", blockResistance);
      return compound;
   }

   @Override
   public void runScript(String type, Event event) {
      if (isEnabled()) {
         if (ScriptController.Instance.lastLoaded > lastInited) {
            lastInited = ScriptController.Instance.lastLoaded;
            if (!type.equals(EnumScriptType.INIT.function)) {
               EventHooks.onScriptBlockInit(this);
            }
         }
         for (ScriptContainer script : scripts) { script.run(type, event); }
      }
   }

   @Override
   public boolean isEnabled() { return CustomNpcs.EnableScripting && enabled && ScriptController.HasStart && !scripts.isEmpty() && level != null && !level.isClientSide; }

   @Override
   public void clearConsoleText(Long key) {
      for (ScriptContainer script : getScripts()) { script.console.remove(key); }
   }

   @Override
   public void setLastInited(long timeMC) { lastInited = timeMC; }

   public static void tick(Level ignoredLevel, BlockPos ignoredPos, BlockState ignoredState, TileScriptedDoor tile) {
      ++tile.tickCount;
      if (tile.prevPower != tile.newPower) {
         EventHooks.onScriptBlockRedstonePower(tile, tile.prevPower, tile.newPower);
         tile.prevPower = tile.newPower;
      }
      tile.timers.update();
      if (tile.tickCount >= 10) {
         EventHooks.onScriptBlockUpdate(tile);
         tile.tickCount = 0;
      }
   }

   @Override
   public boolean isClient() { return getLevel() == null || getLevel().isClientSide; }

   @Override
   public boolean getEnabled() { return enabled; }

   @Override
   public void setEnabled(boolean bo) { enabled = bo; }

   @Override
   public String getLanguage() { return scriptLanguage; }

   @Override
   public void setLanguage(String lang) { scriptLanguage = lang; }

   @Override
   public List<ScriptContainer> getScripts() { return scripts; }

   @Override
   public MutableComponent noticeString(String type, Object event) {
      MutableComponent message = Component.literal("Scripted Door")
              .withStyle(ChatFormatting.DARK_GRAY);
      if (type != null) {
         message.append(Component.literal(" hook \"").withStyle(ChatFormatting.DARK_GRAY))
                 .append(Component.literal(type).withStyle(ChatFormatting.GRAY))
                 .append(Component.literal("\"; ").withStyle(ChatFormatting.DARK_GRAY));
      }
      else { message.append(Component.literal("; ").withStyle(ChatFormatting.DARK_GRAY)); }
      String dimID = level == null ? "null" : level.dimensionTypeId().location().toString();
      double x = Math.round(worldPosition.getX() * 100.0d) / 100.0d;
      double y = Math.round(worldPosition.getY() * 100.0d) / 100.0d;
      double z = Math.round(worldPosition.getZ() * 100.0d) / 100.0d;
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
   public Map<Long, String> getConsoleText() {
      Map<Long, String> map = new TreeMap<>();
      int tab = 0;
      for (ScriptContainer script : getScripts()) {
         ++tab;
         for (Entry<Long, String> entry : script.console.entrySet()) { map.put(entry.getKey(), " tab " + tab + ":\n" + entry.getValue()); }
      }
      return map;
   }

   @Override
   public void clearConsole() {
      for (ScriptContainer script : getScripts()) { script.console.clear(); }
   }

   @Override
   public void init() { lastInited = -1; }

   // New from Unofficial (BetaZavr)
   @SuppressWarnings("unused")
   public String getSound(boolean isOpen) { return ""; }

   @SuppressWarnings("unused")
   public void setSound(boolean isOpen, String song) {
   }

}
