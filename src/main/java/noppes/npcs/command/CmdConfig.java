package noppes.npcs.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockIce;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockVine;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketConfigFont;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.controllers.ChunkController;

import javax.annotation.Nonnull;

public class CmdConfig extends CommandNoppesBase {

	@Override
	public int getRequiredPermissionLevel() { return 2; }

	@Override
	public String getDescription() { return "Some config things you can set"; }

	@Nonnull
	public String getName() { return "config"; }

	@SubCommand(desc = "Set how many active chunkloaders you can have", usage = "<number>", permission = 4)
	public void chunkloaders(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0) {
			this.sendMessage(sender, "ChunkLoaders: " + ChunkController.instance.size() + "/" + CustomNpcs.ChuckLoaders);
		} else {
			try {
				CustomNpcs.ChuckLoaders = Integer.parseInt(args[0]);
			} catch (NumberFormatException ex) {
				throw new CommandException("Didn't get a number: " + args[0]);
			}
			CustomNpcs.Config.updateConfig();
			int size = ChunkController.instance.size();
			if (size > CustomNpcs.ChuckLoaders) {
				ChunkController.instance.unload(size - CustomNpcs.ChuckLoaders);
				this.sendMessage(sender, size - CustomNpcs.ChuckLoaders + " chunks loaders unloaded");
			}
			this.sendMessage(sender, "ChunkLoaders: " + ChunkController.instance.size() + "/" + CustomNpcs.ChuckLoaders);
		}
	}

	@SubCommand(desc = "Add debug info to log", permission = 4)
	public void debug(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0) {
			CustomNpcs.VerboseDebug = !CustomNpcs.VerboseDebug;
			sender.sendMessage(new TextComponentTranslation("command.debug." + CustomNpcs.VerboseDebug));
		}
		else if (args[0].equals("start")) { CustomNpcs.debugData.startDebugging(sender); }
		else if (args[0].equals("stop")) { CustomNpcs.debugData.stopDebugging(sender); }
		else {
			try {
				CustomNpcs.VerboseDebug = Boolean.parseBoolean(args[0].toLowerCase());
				sender.sendMessage(new TextComponentTranslation("command.debug." + CustomNpcs.VerboseDebug));
			} catch (Exception e) {
				throw new CommandException("\""+args[0]+"\" is not a subcommand or boolean value");
			}
		}
	}

	@SubCommand(desc = "Get/Set font", usage = "[type] [size]", permission = 2)
	public void font(MinecraftServer server, ICommandSender sender, String[] args) {
		if (!(sender instanceof EntityPlayerMP)) { return; }
		int size = 18;
		if (args.length > 1) {
			try {
				size = Integer.parseInt(args[args.length - 1]);
				args = Arrays.copyOfRange(args, 0, args.length - 1);
			} catch (Exception e) { LogWriter.error(e); }
		}
		StringBuilder font = new StringBuilder();
		for (String arg : args) {
			font.append(" ").append(arg);
		}
		Packets.send((EntityPlayerMP) sender, new PacketConfigFont(font.toString().trim(), size));
	}

	@SubCommand(desc = "Freezes/Unfreezes npcs", usage = "[true/false]", permission = 4)
	public void freezenpcs(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			this.sendMessage(sender, "Frozen NPCs: " + CustomNpcs.FreezeNPCs);
		} else {
			CustomNpcs.FreezeNPCs = Boolean.parseBoolean(args[0]);
			this.sendMessage(sender, "FrozenNPCs is now " + CustomNpcs.FreezeNPCs);
		}
	}

	@SubCommand(desc = "Disable/Enable the ice melting", usage = "[true/false]", permission = 4)
	public void icemelts(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			this.sendMessage(sender, "IceMelts: " + CustomNpcs.IceMeltsEnabled);
		} else {
			CustomNpcs.IceMeltsEnabled = Boolean.parseBoolean(args[0]);
			CustomNpcs.Config.updateConfig();
			Set<ResourceLocation> names = Block.REGISTRY.getKeys();
			for (ResourceLocation name : names) {
				Block block = Block.REGISTRY.getObject(name);
				if (block instanceof BlockIce) {
					block.setTickRandomly(CustomNpcs.IceMeltsEnabled);
				}
			}
			this.sendMessage(sender, "IceMelts is now " + CustomNpcs.IceMeltsEnabled);
		}
	}

	@SubCommand(desc = "Disable/Enable the natural leaves decay", usage = "[true/false]", permission = 4)
	public void leavesdecay(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			this.sendMessage(sender, "LeavesDecay: " + CustomNpcs.LeavesDecayEnabled);
		} else {
			CustomNpcs.LeavesDecayEnabled = Boolean.parseBoolean(args[0]);
			CustomNpcs.Config.updateConfig();
			Set<ResourceLocation> names = Block.REGISTRY.getKeys();
			for (ResourceLocation name : names) {
				Block block = Block.REGISTRY.getObject(name);
				if (block instanceof BlockLeaves) {
					block.setTickRandomly(CustomNpcs.LeavesDecayEnabled);
				}
			}
			this.sendMessage(sender, "LeavesDecay is now " + CustomNpcs.LeavesDecayEnabled);
		}
	}

	@SubCommand(desc = "Enables/Disables scripting", usage = "[true/false]", permission = 4)
	public void scripting(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			this.sendMessage(sender, "Scripting: " + CustomNpcs.EnableScripting);
		} else {
			CustomNpcs.EnableScripting = Boolean.parseBoolean(args[0]);
			CustomNpcs.Config.updateConfig();
			this.sendMessage(sender, "Scripting is now " + CustomNpcs.EnableScripting);
		}
	}

	@SubCommand(desc = "Disable/Enable the vines growing", usage = "[true/false]", permission = 4)
	public void vinegrowth(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			this.sendMessage(sender, "VineGrowth: " + CustomNpcs.VineGrowthEnabled);
		} else {
			CustomNpcs.VineGrowthEnabled = Boolean.parseBoolean(args[0]);
			CustomNpcs.Config.updateConfig();
			Set<ResourceLocation> names = Block.REGISTRY.getKeys();
			for (ResourceLocation name : names) {
				Block block = Block.REGISTRY.getObject(name);
				if (block instanceof BlockVine) {
					block.setTickRandomly(CustomNpcs.VineGrowthEnabled);
				}
			}
			this.sendMessage(sender, "VineGrowth is now " + CustomNpcs.VineGrowthEnabled);
		}
	}

	@SubCommand(desc = "Enables/Disables invisible NPCs", usage = "[true/false]", permission = 4)
	public void invisiblenpcs(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (!(sender instanceof EntityPlayerMP) || !CustomNpcsPermissions.hasPermission((EntityPlayerMP) sender, CustomNpcsPermissions.NPC_DISPLAY)) {
			throw new CommandException(Component.translatable("availability.permission").getString());
		}
		if (args.length == 0) {
			sendMessage(sender, "Invisible NPCs: " + CustomNpcs.EnableInvisibleNpcs);
		} else {
			CustomNpcs.EnableInvisibleNpcs = Boolean.parseBoolean(args[0]);
			CustomNpcs.Config.updateConfig();
			this.sendMessage(sender, "Invisible NPCs is now " + CustomNpcs.EnableInvisibleNpcs);
		}
	}

	@SubCommand(desc = "Open custom elements manager GUI", permission = 4)
	public void customelements(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (sender instanceof EntityPlayerMP) {
			if (!CustomNpcsPermissions.hasPermission((EntityPlayerMP) sender, CustomNpcsPermissions.EDIT_ELEMENTS)) { throw new CommandException("availability.permission"); }
			NoppesUtilServer.sendOpenGui((EntityPlayerMP) sender, EnumGuiType.ManageCustomElements, null);
		}
	}

	@Override
	public @Nonnull List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args, BlockPos pos) {
		List<String> list = new ArrayList<>();
		if (args.length == 2) {
			if (args[0].equals("debug")) {
				list.add("true");
				list.add("false");
				list.add("start");
				list.add("stop");
			}
		}
		return list;
	}

}
