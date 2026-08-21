package noppes.npcs.controllers.data;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.Event;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.event.*;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class BaseScriptData implements IScriptHandler {

    protected final List<ScriptContainer> scripts = new ArrayList<>();
    protected String scriptLanguage = "ECMAScript";
    protected boolean enabled = false;
    public long lastInited = -1L;
    public boolean hadInteract = true;

    public void clear() { scripts.clear(); }

    @Override
    public abstract void runScript(String type, Event event);

    @Override
    public boolean isClient() { return Util.instance.getSide() == Dist.CLIENT; }

    @Override
    public boolean getEnabled() { return enabled; }

    @Override
    public void setEnabled(boolean bo) { enabled = bo; }

    @Override
    public String getLanguage() { return scriptLanguage; }

    @Override
    public void setLanguage(String language) {
        language = Util.instance.deleteColor(language);
        if (ScriptController.Instance.languages.containsKey(language)) { scriptLanguage = language; }
    }

    @Override
    public List<ScriptContainer> getScripts() { return scripts; }

    @Override
    public TreeMap<Long, String> getConsoleText() {
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
    public void clearConsole() {
        for (ScriptContainer script : this.getScripts()) { script.console.clear(); }
    }

    @Override
    public void clearConsoleText(Long key) {
        for (ScriptContainer script : this.getScripts()) { script.console.remove(key); }
    }

    @Override
    public boolean isEnabled() {
        return CustomNpcs.EnableScripting && enabled && ScriptController.HasStart && !scripts.isEmpty();
    }

    @Override
    public void setLastInited(long timeMC) { lastInited = timeMC; }

    public void load(CompoundTag compound) {
        clear();
        scripts.addAll(NBTTags.getScript(compound.getList("Scripts", 10), this));
        scriptLanguage = compound.getString("ScriptLanguage");
        enabled = compound.getBoolean("ScriptEnabled");
    }

    public CompoundTag save(CompoundTag compound) {
        compound.put("Scripts", NBTTags.nbtScript(scripts));
        compound.putString("ScriptLanguage", scriptLanguage);
        compound.putBoolean("ScriptEnabled", enabled);
        return compound;
    }

    public void init() { lastInited = -1; }

    // New from Unofficial (BetaZavr)
    @Override
    public MutableComponent noticeString(String type, Object event) {
        MutableComponent message = Component.empty().withStyle(ChatFormatting.DARK_GRAY);
        String pos = "";
        String dimID = "overworld";
        double x = 0.0d, y = 0.0d, z = 0.0d, tpY = 0.0d;
        if (type != null) {
            message.append(Component.literal(" hook \"").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(type).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\"; ").withStyle(ChatFormatting.DARK_GRAY));
        }
        else { message.append(Component.literal("; ").withStyle(ChatFormatting.DARK_GRAY)); }
        IEntity<?> iEntity = null;
        IBlock iBlock = null;
        if (event instanceof Event && !(event instanceof CustomNPCsEvent)) { event = new ForgeEvent((Event) event); }
        if (event instanceof ForgeEvent) { iEntity = ((ForgeEvent) event).entity; }
        if (event instanceof PlayerEvent) {
            if (((PlayerEvent) event).player != null) { iEntity =  ((PlayerEvent) event).player; }
            else { message.append(Component.literal("Global players script").withStyle(ChatFormatting.DARK_GRAY)); }
            if (((PlayerEvent) event).player != null) { iEntity =  ((PlayerEvent) event).player; }
        }
        else if (event instanceof NpcEvent && ((NpcEvent) event).npc != null) { iEntity = ((NpcEvent) event).npc; }
        if (iEntity != null) {
            MutableComponent mesEntity;
            if (iEntity.getMCEntity() instanceof Player) { mesEntity = Component.literal("Player \""); }
            else if (iEntity.getMCEntity() instanceof EntityNPCInterface) { mesEntity = Component.literal("NPC \""); }
            else { mesEntity = Component.literal("Entity \""); }
            mesEntity.withStyle(ChatFormatting.DARK_GRAY);
            message.append(mesEntity)
                    .append(Component.literal(iEntity.getName()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\"; UUID: \"").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(iEntity.getUUID()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\" in ").withStyle(ChatFormatting.DARK_GRAY));

            dimID = iEntity.getWorld().getMCLevel() == null ? "overworld" : iEntity.getWorld().getMCLevel().dimensionTypeId().location().toString();
            x = Math.round(iEntity.getPos().getX() * 100.0d) / 100.0d;
            y = Math.round(iEntity.getPos().getY() * 100.0d) / 100.0d;
            z = Math.round(iEntity.getPos().getZ() * 100.0d) / 100.0d;
            pos = "dimension ID:" + dimID + "; X:" + x + "; Y:" + y + "; Z:" + z;
        }
        if (pos.isEmpty() && event instanceof BlockEvent && ((BlockEvent) event).block != null) { iBlock = ((BlockEvent) event).block; }
        if (iBlock != null) {
            message.append(Component.literal("Block in ").withStyle(ChatFormatting.GRAY));
            dimID = iBlock.getWorld().getMCLevel() == null ? "overworld" : iBlock.getWorld().getMCLevel().dimensionTypeId().location().toString();
            x = Math.floor(iBlock.getPos().getX());
            y = Math.floor(iBlock.getPos().getY());
            z = Math.floor(iBlock.getPos().getZ());
            pos = "dimension ID:" + dimID + "; X:" + x + "; Y:" + y + "; Z:" + z;
            tpY = 1.0d;
        }
        if (!pos.isEmpty()) {
            MutableComponent posClick = Component.literal(pos);
            posClick.setStyle(posClick.getStyle().withColor(ChatFormatting.BLUE)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/noppes world tp @p " + dimID + " " + x + " " + (y + tpY) + " "+z))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("script.hover.error.pos.tp"))));
            message.append(posClick);
        }
        return message.append(Component.literal("; Side: " + (Util.instance.getSide() == Dist.DEDICATED_SERVER ? "Server" : "Client")).withStyle(ChatFormatting.DARK_GRAY));
    }

}
