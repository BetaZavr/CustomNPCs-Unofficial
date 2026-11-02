package noppes.npcs.controllers.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;
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

public abstract class BaseScriptData
implements IScriptHandler {

	protected final List<ScriptContainer> scripts = new ArrayList<>();
	protected String scriptLanguage = "ECMAScript";
	protected boolean enabled = false;
	public boolean hadInteract = true;
	public long lastInited = -1L;

	public void clear() { scripts.clear(); }

	@Override
	public abstract void runScript(String type, Event event);

	@Override
	public boolean isClient() { return Util.instance.getSide() == Side.CLIENT; }

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
		for (ScriptContainer script : getScripts()) { script.console.clear(); }
	}

	@Override
	public void clearConsoleText(Long key) {
		for (ScriptContainer script : getScripts()) { script.console.remove(key); }
	}

	public boolean isEnabled() {
		return CustomNpcs.EnableScripting && enabled && ScriptController.HasStart && !scripts.isEmpty();
	}

	@Override
	public void setLastInited(long timeMC) { lastInited = timeMC; }

	public void load(NBTTagCompound compound) {
		clear();
		scripts.addAll(NBTTags.getScript(compound.getTagList("Scripts", 10), this));
		scriptLanguage = Util.instance.deleteColor(compound.getString("ScriptLanguage"));
		enabled = compound.getBoolean("ScriptEnabled");
	}

	@Override
	public void init() { lastInited = -1; }

	public NBTTagCompound save(NBTTagCompound compound) {
		compound.setTag("Scripts", NBTTags.nbtScript(scripts));
		compound.setString("ScriptLanguage", scriptLanguage);
		compound.setBoolean("ScriptEnabled", enabled);
		return compound;
	}

	// New from Unofficial (BetaZavr)
	@Override
	public Component noticeString(String type, Object event) {
		Component message = Component.empty().withStyle(TextFormatting.DARK_GRAY);
		String pos = "";
		String dimID = "0";
		double x = 0.0d, y = 0.0d, z = 0.0d, tpY = 0.0d;
		if (type != null) {
			message.append(Component.literal(" hook \"").withStyle(TextFormatting.DARK_GRAY))
					.append(Component.literal(type).withStyle(TextFormatting.GRAY))
					.append(Component.literal("\"; ").withStyle(TextFormatting.DARK_GRAY));
		}
		else { message.append(Component.literal("; ").withStyle(TextFormatting.DARK_GRAY)); }
		IEntity<?> iEntity = null;
		IBlock iBlock = null;
		if (event instanceof PlayerEvent) {
			iEntity =  ((PlayerEvent) event).player;
			if (iEntity == null) { message.append(Component.literal("Global players script").withStyle(TextFormatting.DARK_GRAY)); }
		}
		else if (event instanceof NpcEvent && ((NpcEvent) event).npc != null) { iEntity = ((NpcEvent) event).npc; }
		if (iEntity != null) {
			Component mesEntity;
			if (iEntity.getMCEntity() instanceof EntityPlayer) { mesEntity = Component.literal("Player \""); }
			else if (iEntity.getMCEntity() instanceof EntityNPCInterface) { mesEntity = Component.literal("NPC \""); }
			else { mesEntity = Component.literal("Entity \""); }
			mesEntity.withStyle(TextFormatting.DARK_GRAY);
			message.append(mesEntity)
					.append(Component.literal(iEntity.getName()).withStyle(TextFormatting.GRAY))
					.append(Component.literal("\"; UUID: \"").withStyle(TextFormatting.DARK_GRAY))
					.append(Component.literal(iEntity.getUUID()).withStyle(TextFormatting.GRAY))
					.append(Component.literal("\" in ").withStyle(TextFormatting.DARK_GRAY));

			dimID = iEntity.getWorld().getMCWorld() == null ? "0" : "" + iEntity.getWorld().getMCWorld().provider.getDimension();
			x = Math.round(iEntity.getPos().getX() * 100.0d) / 100.0d;
			y = Math.round(iEntity.getPos().getY() * 100.0d) / 100.0d;
			z = Math.round(iEntity.getPos().getZ() * 100.0d) / 100.0d;
			pos = "dimension ID:" + dimID + "; X:" + x + "; Y:" + y + "; Z:" + z;
		}
		if (pos.isEmpty() && event instanceof BlockEvent && ((BlockEvent) event).block != null) { iBlock = ((BlockEvent) event).block; }
		if (iBlock != null) {
			message.append(Component.literal("Block in ").withStyle(TextFormatting.GRAY));
			dimID = iBlock.getWorld().getMCWorld() == null ? "0" : "" + iBlock.getWorld().getMCWorld().provider.getDimension();
			x = Math.floor(iBlock.getPos().getX());
			y = Math.floor(iBlock.getPos().getY());
			z = Math.floor(iBlock.getPos().getZ());
			pos = "dimension ID:" + dimID + "; X:" + x + "; Y:" + y + "; Z:" + z;
			tpY = 1.0d;
		}
		if (!pos.isEmpty()) {
			Component posClick = Component.literal(pos);
			posClick.setStyle(posClick.getStyle().setColor(TextFormatting.BLUE)
					.setUnderlined(true)
					.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/noppes world tp @p " + dimID + " " + x + " " + (y + tpY) + " "+z))
					.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("script.hover.error.pos.tp"))));
			message.append(posClick);
		}
		return message.append(Component.literal("; Side: " + (Util.instance.getSide() == Side.SERVER ? "Server" : "Client")).withStyle(TextFormatting.DARK_GRAY));
	}

}
