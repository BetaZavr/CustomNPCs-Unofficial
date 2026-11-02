package noppes.npcs.controllers.data;

import java.util.*;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;

public class PlayerScriptData
extends BaseScriptData {


	protected static final Map<EntityPlayerMP, List<ScriptContainer>> PLAYER_CASH = new HashMap<>();
	private static final TreeMap<Long, String> console = new TreeMap<>();
	private static final List<Integer> errored = new ArrayList<>();

	@Override
	public void clear() {
		console.clear();
		errored.clear();
		scripts.clear();
		PLAYER_CASH.clear();
	}

	@Override
	public void load(NBTTagCompound compound) {
		super.load(compound);
		console.clear();
		console.putAll(NBTTags.getLongStringMap(compound.getTagList("ScriptConsole", 10)));
	}

	@Override
	public NBTTagCompound save(NBTTagCompound compound) {
		super.save(compound);
		compound.setTag("ScriptConsole", NBTTags.nbtLongStringMap(console));
		return compound;
	}

	@Override
	public void runScript(String type, Event event) {
		if (!isEnabled() ||
				!(event instanceof PlayerEvent) ||
				((PlayerEvent) event).player == null ||
				!(((PlayerEvent) event).player.getMCEntity() instanceof EntityPlayerMP)) { return; }
		EntityPlayerMP player = (EntityPlayerMP) ((PlayerEvent) event).player.getMCEntity();
		if (ScriptController.Instance.lastLoaded > lastInited) {
			errored.clear();
			lastInited = ScriptController.Instance.lastLoaded;
			if (!type.equals(EnumScriptType.INIT.function)) {
				for (EntityPlayerMP pl : PLAYER_CASH.keySet()) {
					PLAYER_CASH.get(pl).clear();
					EventHooks.onPlayerInit(new PlayerEvent.InitEvent(PlayerData.get(pl).getIPlayer()));
				}
			}
		} // init event
		if (!PLAYER_CASH.containsKey(player)) {
			ArrayList<ScriptContainer> list = new ArrayList<>();
			for (ScriptContainer scriptContainer : scripts) {
				ScriptContainer s = new ScriptContainer(this);
				s.load(scriptContainer.save(new NBTTagCompound()));
				list.add(s);
			}
			PLAYER_CASH.put(player, list);
			if (!type.equals(EnumScriptType.INIT.function)) {
				EventHooks.onPlayerInit(new PlayerEvent.InitEvent(PlayerData.get(player).getIPlayer()));
			}
		} // create cash to player
		if (PLAYER_CASH.containsKey(player)) {
			ScriptContainer script;
			for(int i = 0; i < PLAYER_CASH.get(player).size(); ++i) {
				if (!errored.contains(i)) {
					script = scripts.get(i);
					script.run(type, event);
					if (script.isErrored()) { errored.add(i); }
					for (Map.Entry<Long, String> entry : script.console.entrySet()) {
						if (!console.containsKey(entry.getKey())) { console.put(entry.getKey(), " tab " + (i + 1) + ":\n" + entry.getValue()); }
					}
					script.console.clear();
				}
			}
		} // run
		while (console.size() > 40) { console.remove(console.firstKey()); }
	}

	@Override
	public String getLanguage() { return ScriptController.Instance.playerScripts.scriptLanguage; }

	@Override
	public void setLanguage(String lang) { ScriptController.Instance.playerScripts.scriptLanguage = lang; }

	@Override
	public Component noticeString(String type, Object event) {
		Component message = Component.literal("Players script")
				.withStyle(TextFormatting.DARK_GRAY);
		if (type != null) {
			message.append(Component.literal(" hook \"").withStyle(TextFormatting.DARK_GRAY))
					.append(Component.literal(type).withStyle(TextFormatting.GRAY))
					.append(Component.literal("\"; ").withStyle(TextFormatting.DARK_GRAY));
		}
		else { message.append(Component.literal("; ").withStyle(TextFormatting.DARK_GRAY)); }
		if (event instanceof PlayerEvent && ((PlayerEvent) event).player != null) {
			EntityPlayer player = ((PlayerEvent) event).player.getMCEntity();
			int dimID = player.world == null ? 0 : player.world.provider.getDimension();
			double x = Math.round(player.posX * 100.0d) / 100.0d;
			double y = Math.round(player.posY * 100.0d) / 100.0d;
			double z = Math.round(player.posZ * 100.0d) / 100.0d;
			Component posClick = Component.literal("dimension ID:" + dimID + "; X:" + x + "; Y:" + y + "; Z:" + z);
			posClick.getStyle().setColor(TextFormatting.BLUE)
					.setUnderlined(true)
					.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/noppes world tp @p " + dimID + " " + x + " " + y + " "+z))
					.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("script.hover.error.pos.tp")));
			message.append(Component.literal("name: \"").withStyle(TextFormatting.DARK_GRAY))
					.append(Component.literal(player.getName()).withStyle(TextFormatting.GRAY))
					.append(Component.literal("\"; UUID: \"").withStyle(TextFormatting.DARK_GRAY))
					.append(Component.literal(player.getUniqueID().toString()).withStyle(TextFormatting.GRAY))
					.append(Component.literal("\" in ").withStyle(TextFormatting.DARK_GRAY))
					.append(posClick);
		}
		return message.append(Component.literal("; Side: " + (isClient() ? "Client" : "Server")).withStyle(TextFormatting.DARK_GRAY));
	}

	@Override
	public TreeMap<Long, String> getConsoleText() { return console; }

	@Override
	public void clearConsole() { PlayerScriptData.console.clear(); }

	@Override
	public void clearConsoleText(Long key) { PlayerScriptData.console.remove(key); }

	public static void removePlayer(EntityPlayerMP player) {
		if (player != null) { PLAYER_CASH.remove(player); }
	}

}
