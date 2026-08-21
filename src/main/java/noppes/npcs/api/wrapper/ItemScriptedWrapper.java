package noppes.npcs.api.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.ItemEvent;
import noppes.npcs.api.item.IItemScripted;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;

public class ItemScriptedWrapper extends ItemStackWrapper implements IItemScripted, IScriptHandler {

   public List<ScriptContainer> scripts = new ArrayList<>();
   public String scriptLanguage = "ECMAScript";
   public boolean enabled = false;
   public long lastInited = -1L;
   public boolean updateClient = false;
   public boolean durabilityShow = true;
   public float durabilityValue = 1.0F;
   public int durabilityColor = -1;
   public int itemColor = -1;
   public int stackSize = 64;
   public boolean loaded = false;
   public ResourceLocation texture = null;

   public ItemScriptedWrapper(ItemStack item) {
      super(item);
   }

   @Override
   public boolean hasTexture(int damage) {
      return texture != null;
   }

   @Override
   public String getTexture(int damage) {
      return getTexture();
   }

   @Override
   public String getTexture() {
      return texture == null ? null : texture.toString();
   }

   @Override
   public void setTexture(int damage, String texture) {
      setTexture(texture);
   }

   @Override
   public void setTexture(String textureIn) {
      if (textureIn == null) { texture = null; }
      else { texture = ResourceLocation.tryParse(textureIn); }
   }

   public CompoundTag getScriptNBT(CompoundTag compound) {
      compound.put("Scripts", NBTTags.nbtScript(scripts));
      compound.putString("ScriptLanguage", scriptLanguage);
      compound.putBoolean("ScriptEnabled", enabled);
      if (texture != null) {
         compound.putString("ScriptTexture", texture.toString());
      }

      return compound;
   }

   @Override
   public CompoundTag getMCNbt() {
      CompoundTag compound = super.getMCNbt();
      getScriptNBT(compound);
      compound.putBoolean("DurabilityShow", durabilityShow);
      compound.putFloat("DurabilityValue", durabilityValue);
      compound.putInt("DurabilityColor", durabilityColor);
      compound.putInt("ItemColor", itemColor);
      compound.putInt("MaxStackSize", stackSize);
      return compound;
   }

   public void setScriptNBT(CompoundTag compound) {
      if (compound.contains("Scripts")) {
         scripts = NBTTags.getScript(compound.getList("Scripts", 10), this);
         scriptLanguage = compound.getString("ScriptLanguage");
         enabled = compound.getBoolean("ScriptEnabled");
         if (compound.contains("ScriptTexture")) {
            texture = ResourceLocation.tryParse(compound.getString("ScriptTexture"));
         }
      }
   }

   @Override
   public void setMCNbt(CompoundTag compound) {
      super.setMCNbt(compound);
      setScriptNBT(compound);
      durabilityShow = compound.getBoolean("DurabilityShow");
      durabilityValue = compound.getFloat("DurabilityValue");
      if (compound.contains("DurabilityColor")) {
         durabilityColor = compound.getInt("DurabilityColor");
      }

      itemColor = compound.getInt("ItemColor");
      stackSize = compound.getInt("MaxStackSize");
   }

   @Override
   public int getType() {
      return 6;
   }

   @Override
   public void runScript(String type, Event event) {
      if (!loaded) {
         loadScriptData();
         loaded = true;
      }

      if (isEnabled()) {
         if (ScriptController.Instance.lastLoaded > lastInited) {
            lastInited = ScriptController.Instance.lastLoaded;
            if (!type.equals(EnumScriptType.INIT.function)) {
               EventHooks.onScriptItemInit(this);
            }
         }
         for (ScriptContainer script : scripts) { script.run(type, event); }
      }
   }

   @Override
   public boolean isEnabled() {
      return enabled && ScriptController.HasStart;
   }

   @Override
   public void clearConsoleText(Long key) {
      for (ScriptContainer script : getScripts()) { script.console.remove(key); }
   }

   @Override
   public void setLastInited(long timeMC) { lastInited = timeMC; }

   @Override
   public boolean isClient() {
      return false;
   }

   @Override
   public boolean getEnabled() {
      return enabled;
   }

   @Override
   public void setEnabled(boolean bo) {
      enabled = bo;
   }

   @Override
   public String getLanguage() {
      return scriptLanguage;
   }

   @Override
   public void setLanguage(String lang) {
      scriptLanguage = lang;
   }

   @Override
   public List<ScriptContainer> getScripts() {
      return scripts;
   }

   @Override
   public MutableComponent noticeString(String type, Object event) {
      MutableComponent message = Component.literal("Scripted Item")
              .withStyle(ChatFormatting.DARK_GRAY);
      if (type != null) {
         message.append(Component.literal(" hook \"").withStyle(ChatFormatting.DARK_GRAY))
                 .append(Component.literal(type).withStyle(ChatFormatting.GRAY))
                 .append(Component.literal("\"; ").withStyle(ChatFormatting.DARK_GRAY));
      }
      else { message.append(Component.literal("; ").withStyle(ChatFormatting.DARK_GRAY)); }

      IPlayer<?> iPlayer = getIPlayer(event);
      if (iPlayer != null) {
         String dimID = iPlayer.getWorld().getMCLevel().dimensionTypeId().location().toString();
         double x = Math.round(iPlayer.getX() * 100.0d) / 100.0d;
         double y = Math.round(iPlayer.getY() * 100.0d) / 100.0d;
         double z = Math.round(iPlayer.getZ() * 100.0d) / 100.0d;
         MutableComponent posClick = Component.literal("dimension ID:" + dimID + "; X:" + x + "; Y:" + y + "; Z:" + z);
         Style style = posClick.getStyle().withColor(ChatFormatting.BLUE);
         style = style.withUnderlined(true);
         style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/noppes world tp @p " + dimID + " " + x + " " + y + " "+z));
         style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("script.hover.error.pos.tp")));
         posClick.setStyle(style);
         message.append(Component.literal("Player: \"").withStyle(ChatFormatting.DARK_GRAY))
                 .append(Component.literal(iPlayer.getName()).withStyle(ChatFormatting.GRAY))
                 .append(Component.literal("\"; UUID: \"").withStyle(ChatFormatting.DARK_GRAY))
                 .append(Component.literal(iPlayer.getUUID()).withStyle(ChatFormatting.GRAY))
                 .append(Component.literal("\" in ").withStyle(ChatFormatting.DARK_GRAY))
                 .append(posClick);
      }
      return message.append(Component.literal((iPlayer != null ? "; " : "") + "Side: " + (isClient() ? "Client" : "Server")).withStyle(ChatFormatting.DARK_GRAY));
   }

   private static IPlayer<?> getIPlayer(Object event) {
      if (event instanceof ItemEvent.AttackEvent) { return ((ItemEvent.AttackEvent) event).player; }
      if (event instanceof ItemEvent.InteractEvent) { return ((ItemEvent.InteractEvent) event).player; }
      if (event instanceof ItemEvent.PickedUpEvent) { return ((ItemEvent.PickedUpEvent) event).player; }
      if (event instanceof ItemEvent.TossedEvent) { return ((ItemEvent.TossedEvent) event).player; }
      if (event instanceof ItemEvent.UpdateEvent) { return ((ItemEvent.UpdateEvent) event).player; }
      return null;
   }

   @Override
   public Map<Long, String> getConsoleText() {
      Map<Long, String> map = new TreeMap<>();
      int tab = 0;
      for (ScriptContainer script : getScripts()) {
         ++tab;
         for (Entry<Long, String> entry : script.console.entrySet()) {
            map.put(entry.getKey(), " tab " + tab + ":\n" + entry.getValue());
         }
      }
      return map;
   }

   @Override
   public void clearConsole() {
      for (ScriptContainer script : getScripts()) {
         script.console.clear();
      }
   }

   @Override
   public int getMaxStackSize() {
      return stackSize;
   }

   @Override
   public void setMaxStackSize(int size) {
      if (size >= 1 && size <= stackSize) {
         stackSize = size;
      } else {
         throw new CustomNPCsException("Stack size has to be between 1 and " + stackSize);
      }
   }

   @Override
   public double getDurabilityValue() {
      return durabilityValue;
   }

   @Override
   public void setDurabilityValue(float value) {
      if (value != durabilityValue) {
         updateClient = true;
      }

      durabilityValue = value;
   }

   @Override
   public boolean getDurabilityShow() {
      return durabilityShow;
   }

   @Override
   public void setDurabilityShow(boolean bo) {
      if (bo != durabilityShow) {
         updateClient = true;
      }

      durabilityShow = bo;
   }

   @Override
   public int getDurabilityColor() {
      return durabilityColor;
   }

   @Override
   public void setDurabilityColor(int color) {
      if (color != durabilityColor) {
         updateClient = true;
      }

      durabilityColor = color;
   }

   @Override
   public int getColor() {
      return itemColor;
   }

   @Override
   public void setColor(int color) {
      if (color != itemColor) {
         updateClient = true;
      }

      itemColor = color;
   }

   public void saveScriptData() {
      CompoundTag c = item.getTag();
      if (c == null) {
         item.setTag(c = new CompoundTag());
      }

      c.put("ScriptedData", getScriptNBT(new CompoundTag()));
   }

   public void loadScriptData() {
      CompoundTag c = item.getTag();
      if (c != null) {
         setScriptNBT(c.getCompound("ScriptedData"));
      }
   }

   public void init() { lastInited = -1; }

}
