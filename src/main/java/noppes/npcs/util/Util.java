package noppes.npcs.util;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import net.minecraft.entity.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.*;

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandBase.CoordinateArg;
import net.minecraft.command.CommandException;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import noppes.npcs.api.*;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.api.mixin.entity.IEntityIMixin;
import noppes.npcs.api.util.IRayTraceResults;
import noppes.npcs.api.util.IRayTraceRotate;
import noppes.npcs.api.util.IRayTraceVec;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.client.TranslateUtil;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.custom.CustomArmor;
import noppes.npcs.mixin.nbt.INBTTagLongArrayMixin;
import noppes.npcs.mixin.world.IWorldMixin;
import noppes.npcs.shared.common.util.LogWriter;
import org.apache.commons.io.IOUtils;

public class Util implements IMethods {

    private static class NumInfo {
		public final int step;
		public final String type;
		public final String color;
		private NumInfo(int stepIn, String typeIn, String colorIn) {
			step = stepIn;
			type = typeIn;
			color = colorIn;
		}
	}
	private static final NumInfo[] steps = new NumInfo[17];
	private static final TreeMap<Integer, String> ROMAN_DIGITS = new TreeMap<Integer, String>() {{
		put(1, "I");
		put(5, "V");
		put(10, "X");
		put(50, "L");
		put(100, "C");
		put(500, "D");
		put(1000, "M");
	}};
	public static final Util instance = new Util();
	private static final Gson gson = new Gson();
	public static Object temp;

	static {
		steps[0] = new NumInfo(-24, "y", "d");  // yocto
		steps[1] = new NumInfo(-21, "z", "9");  // zeppo
		steps[2] = new NumInfo(-18, "a", "3");  // atto
		steps[3] = new NumInfo(-15, "f", "b");  // femto
		steps[4] = new NumInfo(-12, "p", "2");  // pico
		steps[5] = new NumInfo(-9,  "n", "a");  // nano
		steps[6] = new NumInfo(-6,  "µ", "e");  // micro
		steps[7] = new NumInfo(-3,  "m", "5");  // milli
		steps[8] = new NumInfo(3,   "",  "" );  // hecto
		steps[9] = new NumInfo(6,   "K", "e");  // kilo
		steps[10] = new NumInfo(9,  "M", "a");  // mega
		steps[11] = new NumInfo(12, "G", "2");  // giga
		steps[12] = new NumInfo(15, "T", "b");  // tera
		steps[13] = new NumInfo(18, "P", "3");  // peta
		steps[14] = new NumInfo(21, "E", "9");  // hexa
		steps[15] = new NumInfo(24, "E", "d");  // zetta
		steps[16] = new NumInfo(27, "Y", "5");  // yotta
	}

	public List<String> getStringData(String str) {
		int maxLength = 32767;
		List<String> list = new ArrayList<>();
		for (int i = 0; i < str.length(); i += maxLength) {
			int endIndex = Math.min(i + maxLength, str.length());
			String part = str.substring(i, endIndex);
			list.add(part);
		}
		return list;
	}

	public String sanitizeFilename(String name) {
		String forbiddenChars = "/\\:*?\"<>|";
		char[] chars = name.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (forbiddenChars.indexOf(chars[i]) >= 0) { chars[i] = '_'; }
		}
		String newName = new String(chars);
		while (newName.contains("__")) { newName = newName.replace("__", "_"); }
		return newName;
	}

	public Map<Component, Integer> convertStringMap(Map<String, Integer> parent) {
		Map<Component, Integer> map = new HashMap<>();
		for (Map.Entry<String, Integer> entry : parent.entrySet()) {
			map.put(Component.literal(entry.getKey()), entry.getValue());
		}
		return map;
	}

	public boolean canAddItemAfterRemoveItems(NonNullList<ItemStack> inventory, ItemStack addStack, Map<ItemStack, Integer> items, boolean ignoreDamage, boolean ignoreNBT) {
		if (inventory == null || addStack.isEmpty()) { return false; }
		NonNullList<ItemStack> inv = NonNullList.withSize(inventory.size(), ItemStack.EMPTY);
		for (int i = 0; i < inventory.size(); ++i) {
			if (NoppesUtilServer.isItemStackNull(inventory.get(i))) { continue; }
			inv.set(i, inventory.get(i).copy());
		}
		if (items != null && !items.isEmpty()) {
			for (ItemStack stack : items.keySet()) {
				if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
				int count = items.get(stack);
				for (int i = 0; i < inv.size(); ++i) {
					ItemStack is = inv.get(i);
					if (NoppesUtilServer.isItemStackNull(is)) { continue; }
					if (NoppesUtilPlayer.compareItems(stack, is, ignoreDamage, ignoreNBT)) {
						if (count < is.getCount()) {
							is.splitStack(count);
							inv.set(i, is);
							count = 0;
						} else {
							count -= is.getCount();
							inv.set(i, ItemStack.EMPTY);
						}
						if (count <= 0) { break; }
					}
				}
			}
		}
        for (ItemStack itemStack : inv) {
            if (itemStack.isEmpty() || NoppesUtilPlayer.compareItems(addStack, itemStack, ignoreDamage, ignoreNBT)) { return true; }
        }
		return false;
	}

	public boolean canRemoveItems(Map<ItemStack, Integer> inventory, Map<ItemStack, Integer> items, boolean ignoreDamage, boolean ignoreNBT) {
		if (inventory == null || items == null || items.isEmpty()) { return false; }
		for (ItemStack stack : items.keySet()) {
			int count = items.get(stack);
			if (NoppesUtilServer.isItemStackNull(stack)) { continue; }
			for (ItemStack is : inventory.keySet()) {
				if (!NoppesUtilServer.isItemStackNull(is) && NoppesUtilPlayer.compareItems(stack, is, ignoreDamage, ignoreNBT)) { count -= inventory.get(is); }
				if (count <= 0) { break; }
			}
			if (count > 0) { return false; }
		}
		return true;
	}

	public boolean canRemoveItems(NonNullList<ItemStack> inventory, ItemStack stack, boolean ignoreDamage, boolean ignoreNBT) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		Map<ItemStack, Integer> items = new HashMap<>();
		items.put(stack, stack.getCount());
		return canRemoveItems(inventory, items, ignoreDamage, ignoreNBT);
	}

	public boolean canRemoveItems(NonNullList<ItemStack> inventory, Map<ItemStack, Integer> items, boolean ignoreDamage, boolean ignoreNBT) {
		if (inventory == null) { return false; }
		if (items == null || items.isEmpty()) { return true; }
		Map<ItemStack, Integer> inv = new HashMap<>();
        for (ItemStack stack : inventory) {
            if (NoppesUtilServer.isItemStackNull(stack) || stack.isEmpty()) { continue; }
            boolean found = false;
            for (ItemStack st : inv.keySet()) {
                if (NoppesUtilServer.isItemStackNull(st) || st.isEmpty()) { continue; }
                if (NoppesUtilPlayer.compareItems(stack, st, false, false)) {
                    inv.put(st, inv.get(st) + stack.getCount());
                    found = true;
                    break;
                }
            }
            if (!found) { inv.put(stack, stack.getCount()); }
        }
		return canRemoveItems(inv, items, ignoreDamage, ignoreNBT);
	}

	public EntityNPCInterface copyToGUI(EntityNPCInterface npcParent, World world, boolean copyRotation) {
		NBTTagCompound npcNbt = new NBTTagCompound();
		if (npcParent == null) {
			npcParent = (EntityNPCInterface) EntityList.createEntityByIDFromName(new ResourceLocation(CustomNpcs.MODID, "customnpc"), world);
		}
        assert npcParent != null;
        npcParent.writeEntityToNBT(npcNbt);
		npcParent.writeToNBTOptional(npcNbt);
		Entity entity = EntityList.createEntityFromNBT(npcNbt, world);
		if (!(entity instanceof EntityNPCInterface)) {
			entity = EntityList.createEntityByIDFromName(new ResourceLocation(CustomNpcs.MODID, "customnpc"), world);
			if (!(entity instanceof EntityNPCInterface)) {
				return npcParent;
			}
			entity.readFromNBT(npcNbt);
		}
		EntityNPCInterface npc = (EntityNPCInterface) entity;
		MarkData.get(npc).marks.clear();
		npc.display.setShowName(1);
		npc.setHealth(npc.getMaxHealth());
		npc.deathTime = 0;
		npc.rotationYaw = 0;
		npc.prevRotationYaw = 0;
		npc.rotationYawHead = 0;
		npc.rotationPitch = 0;
		npc.prevRotationPitch = 0;
		npc.ais.orientation = 0;
		if (copyRotation) {
			npc.rotationYaw = npcParent.rotationYaw;
			npc.prevRotationYaw = npcParent.prevRotationYaw;
			npc.rotationYawHead = npcParent.rotationYawHead;
			npc.prevRotationYawHead = npcParent.prevRotationYawHead;
			
			npc.rotationPitch = npcParent.rotationPitch;
			npc.prevRotationPitch = npcParent.prevRotationPitch;
			npc.ais.orientation = npcParent.ais.orientation;
			if (npcParent.ais.getStandingType() != 0 && npcParent.ais.getStandingType() != 2) {
				npc.rotationYaw = npcParent.ais.orientation;
			}
		}
		npc.ais.setStandingType(1);
		npc.ticksExisted = 100;
		if (npc instanceof EntityCustomNpc && npcParent instanceof EntityCustomNpc) {
			((EntityCustomNpc) npc).modelData.entity = ((EntityCustomNpc) npcParent).modelData.entity;
		}
		return npc;
	}

	public boolean equalsDeleteColor(String str0, String str1, boolean ignoreCase) {
		str0 = Util.instance.deleteColor(str0);
		str1 = Util.instance.deleteColor(str1);
		return ignoreCase ? str0.equalsIgnoreCase(str1) : str0.equals(str1);
	}

	public IRayTraceRotate getAngles3D(Entity entity, Entity target) {
		if (entity == null || target == null) { return RayTraceRotate.EMPTY; }
		return Util.instance.getAngles3D(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ, target.posX, target.posY + target.getEyeHeight(), target.posZ);
	}

	public Entity getEntityByUUID(UUID uuid, World startWorld) {
		if (startWorld == null) { return null; }
		Entity e = getEntityInWorld(uuid, startWorld);
		if (e == null) {
			MinecraftServer server = CustomNpcs.Server != null ? CustomNpcs.Server
					: startWorld.getMinecraftServer() != null ? startWorld.getMinecraftServer()
							: CustomNpcs.proxy.getPlayer() != null && CustomNpcs.proxy.getPlayer().world != null
									&& CustomNpcs.proxy.getPlayer().world.getMinecraftServer() != null
											? CustomNpcs.proxy.getPlayer().world.getMinecraftServer()
											: null;
			if (server != null) {
				for (WorldServer world : server.worlds) {
					if (world.equals(startWorld)) {
						continue;
					}
					e = getEntityInWorld(uuid, world);
					if (e != null) {
						return e;
					}
				}
			}
		}
		return e;
	}

	public Entity getEntityInWorld(UUID uuid, World world) {
		for (Entity entity : world.loadedEntityList) {
			if (entity.getUniqueID().equals(uuid)) {
				return entity;
			}
		}
		List<Entity> unloadedEntityList = ((IWorldMixin) world).getUnloadedEntityList();
		if (unloadedEntityList != null) {
			for (Entity entity : unloadedEntityList) {
				if (entity.getUniqueID().equals(uuid)) {
					return entity;
				}
			}
		}
		return null;
	}

	@Override
	public List<File> getFiles(File directory, String index) {
		if (CustomNpcs.VerboseDebug && temp == null) {
			temp = new Object[] { directory, System.currentTimeMillis(), 0, 1 };
		}
		List<File> list = new ArrayList<>();
		if (directory != null && directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();
			if (files != null) {
				if (CustomNpcs.VerboseDebug && temp instanceof Object[] && ((Object[]) temp).length > 3 && ((Object[]) temp)[0] == directory) {
					int i = 0;
					for (File f : files) {
						if (f.isDirectory()) { i++; }
					}
					((Object[]) temp)[3] = i;
				}
				for (File f : files) {
					if (f.isDirectory()) {
						list.addAll(getFiles(f, index));
						if (CustomNpcs.VerboseDebug && temp instanceof Object[] && ((Object[]) temp).length > 3 && ((Object[]) temp)[0] == directory) {
							((Object[]) temp)[2] = ((int) ((Object[]) temp)[2]) + 1;
							//LogWriter.debug(ticksToElapsedTime(System.currentTimeMillis() - (long) ((Object[]) temp)[1], true, false, false) + " ... process found files["+((Object[]) temp)[2]+"/"+((Object[]) temp)[3]+"] in \"" + ((Object[]) temp)[0] + "\"; now: \""+f+"\"");
						}
					}
					else {
						if (!f.isFile() || (index != null && !index.isEmpty() && !f.getName().toLowerCase().endsWith(index.toLowerCase()))) { continue; }
						list.add(f);
					}
				}
			}
		}
		try {
			if (CustomNpcs.VerboseDebug && temp instanceof Object[] &&
					((Object[]) temp).length > 0 &&
					((Object[]) temp)[0] == directory)
			{ temp = null; }
		} catch (Exception e) {
			LogWriter.error("[DEBUG] "+temp, e);
        }
        return list;
	}

	public Map<ItemStack, Boolean> getInventoryItemCount(EntityPlayer player, IInventory inventory) {
		Map<ItemStack, Integer> counts = new HashMap<>();
		Map<ItemStack, ItemStack> base = new HashMap<>();
		List<ItemStack> list = new ArrayList<>();
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (NoppesUtilServer.isItemStackNull(stack)) {
				continue;
			}
			boolean has = false;
			if (stack.getMaxStackSize() > 1) {
				for (ItemStack s : list) {
					if (NoppesUtilPlayer.compareItems(stack, s, false, false)) {
						if (s.getCount() != s.getMaxStackSize()) {
							if (stack.getCount() + s.getCount() > stack.getMaxStackSize()) {
								ItemStack c = stack.copy();
								c.setCount((stack.getCount() + s.getCount()) % s.getMaxStackSize());
								s.setCount(s.getMaxStackSize());
								list.add(c);
							} else {
								s.setCount(stack.getCount() + s.getCount());
							}
							has = true;
							break;
						}
					}
				}
			}
			if (!has) {
				list.add(stack.copy());
			}
		}
		list.sort((st_0, st_1) -> Integer.compare(st_1.getCount(), st_0.getCount()));
		for (ItemStack stack : list) {
			for (ItemStack s : counts.keySet()) {
				if (NoppesUtilPlayer.compareItems(stack, s, false, false)) {
					counts.put(s, counts.get(s) + stack.getCount());
					base.put(stack, s);
					stack = s;
					break;
				}
			}
			if (!counts.containsKey(stack)) {
				counts.put(stack, stack.getCount());
				base.put(stack, stack);
			}
		}
		Map<ItemStack, Boolean> map = new HashMap<>();
		for (ItemStack stack : counts.keySet()) {
			int count = 0;
			for (int i = 0; i < player.inventory.mainInventory.size(); ++i) {
				ItemStack s = player.inventory.mainInventory.get(i);
				if (NoppesUtilServer.isItemStackNull(s)) {
					continue;
				}
				if (NoppesUtilPlayer.compareItems(stack, s, false, false)) {
					count += s.getCount();
				}
			}
			boolean has = count >= counts.get(stack);
			for (ItemStack inInvStack : base.keySet()) {
				if (base.get(inInvStack) == stack) {
					map.put(inInvStack, has);
				}
			}
		}
		Map<ItemStack, Boolean> total = new LinkedHashMap<>();
		for (ItemStack stack : list) {
			total.put(stack, map.get(stack));
		}
		return total;
	}

	public String getLastColor(String color, String str) {
		char c = (char) 167;
		if (str.lastIndexOf(c) != -1) {
			if (str.lastIndexOf(c) + 1 < str.length()) {
				int start = str.lastIndexOf(c);
				int end = start + 2;
				while (start - 2 >= 0 && str.charAt(start - 2) == c) {
					start -= 2;
				}
				color = str.substring(start, end);
			} else {
				color = getLastColor(color, str.substring(0, str.length() - 1));
			}
		}
		return color;
	}

	public IRayTraceVec getPosition(BlockPos pos, double yaw, double pitch, double radius) {
		if (pos == null) { return RayTraceVec.EMPTY; }
		return Util.instance.getPosition(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, yaw, pitch, radius);
	}

	@Override
	public String getTextNumberToRoman(int value) {
		if (value > 3999) { return "" + value; }
		StringBuilder sb = new StringBuilder();
		for (int key : ROMAN_DIGITS.descendingKeySet()) {
			while (value >= key) {
				sb.append(ROMAN_DIGITS.get(key));
				value -= key;
			}
		}
		String total = sb.toString();
		if (total.contains("IIII")) {
			if (total.contains("VIIII")) { total = total.replace("VIIII", "IX"); }
			else { total = total.replace("IIII", "IV"); }
		}
		return total;
	}

	@Override
	public String getTextReducedNumber(double value, boolean isInteger, boolean color, boolean notPfx) {
		if (value == 0.0d) { return isInteger ? "0" : "0,0"; }
		String chr = "" + ((char) 167);
		StringBuilder type = new StringBuilder();
		String sufc = "";
		double corr = value;
		int exp;
		boolean negatively = false;
		// Number processing
		if (value <= 0) {
			negatively = true;
			value *= -1.0d;
		}
		boolean found = false;
        for (NumInfo numInfo : steps) {
            if (numInfo.step < 0) {
				if (value < Math.pow(10, numInfo.step)) {
					int step = numInfo.step * -1;
					corr = Math.round(value * Math.pow(10, step)) / Math.pow(10, step);
					if (color && !numInfo.color.isEmpty()) { type = new StringBuilder(chr + numInfo.color); }
					if (!steps[0].type.isEmpty()) { type.append(numInfo.type); }
					if (Math.abs(corr * Math.pow(10, step) - value) > 0.001) { sufc = "≈"; }
					found = true;
				}
            }
			else if (numInfo.step == 3 && value < Math.pow(10, 3)) {
                corr = Math.round(value * 10.0d) / 10.0d;
                found = true;
            }
			else {
				if (value < Math.pow(10, numInfo.step)) {
					corr = Math.round(value / Math.pow(10, numInfo.step - 4)) / 10.0d;
					if (color && !numInfo.color.isEmpty()) { type = new StringBuilder(chr + numInfo.color); }
					if (!steps[0].type.isEmpty()) { type.append(numInfo.type); }
					if (corr * Math.pow(10, numInfo.step - 3) != value) { sufc = "≈"; }
					found = true;
				}
            }
            if (found) { break; }
        }
		if (!found) {
			String power = "+";
			if (value < Math.pow(10, -24)) { power = "-"; }
			boolean contains = String.valueOf(value).contains("E" + power);
			if (String.valueOf(value).contains("e" + power) || contains) {
				String index = "e" + power;
				if (contains) { index = "E" + power; }
				exp = Integer.parseInt(String.valueOf(value).substring(String.valueOf(value).indexOf(index) + 2));
				corr = Math.round(Integer.parseInt(String.valueOf(value).substring(0, String.valueOf(value).indexOf(index))) * 1000.0d) / 1000.0d;
			}
			else {
				exp = String.valueOf(corr).length();
				corr = value;
			}
			type = new StringBuilder("E" + power + exp);
		}
		if (negatively) {
			if (color) { sufc = chr + "c"; }
			if (corr != 0.0d) { sufc += "-"; }
		} // negative or zero
		String end = "";
		if (color) { end = chr + "r"; }
		if (notPfx) { sufc = ""; }
		String num = isInteger ? ("" + (long) corr) : ("" + corr).replace(".", ",");
		return sufc + num + type + end;
	}

	public int inventoryItemCount(EntityPlayer player, ItemStack stack, Availability availability, boolean ignoreDamage, boolean ignoreNBT) {
		if (player == null || (availability != null && !availability.isAvailable(player)) || stack.isEmpty()) {
			return 0;
		}
		int count = 0;
		for (int i = 0; i < player.inventory.mainInventory.size(); ++i) {
			ItemStack is = player.inventory.mainInventory.get(i);
			if (NoppesUtilServer.isItemStackNull(is)) {
				continue;
			}
			if (NoppesUtilPlayer.compareItems(stack, is, ignoreDamage, ignoreNBT)) {
				count += is.getCount();
			}
		}
		return count;
	}

	/** Correct deletion of folders */
	@Override
	public boolean removeFile(File directory) {
		if (directory == null) { return false; }
		LogWriter.debug("Trying remove file \"" + directory + "\"");
		if (!directory.isDirectory()) {
			return directory.delete();
		}
		File[] list = directory.listFiles();
		if (list != null) {
			for (File tempFile : list) {
				removeFile(tempFile);
			}
		}
		return directory.delete();
	}

	public boolean removeItem(EntityPlayerMP player, ItemStack stack, boolean ignoreDamage, boolean ignoreNBT) {
		if (player == null || stack == null || stack.isEmpty()) {
			return false;
		}
		return removeItem(player, stack, stack.getCount(), ignoreDamage, ignoreNBT);
	}

	public boolean removeItem(EntityPlayerMP player, ItemStack stack, int count, boolean ignoreDamage, boolean ignoreNBT) {
		if (player == null || stack == null || stack.isEmpty()) {
			return false;
		}
		if (count <= 0) {
			return true;
		}
		for (int i = 0; i < player.inventory.mainInventory.size(); ++i) {
			ItemStack is = player.inventory.getStackInSlot(i);
			if (NoppesUtilServer.isItemStackNull(is)) {
				continue;
			}
			if (NoppesUtilPlayer.compareItems(stack, is, ignoreDamage, ignoreNBT)) {
				if (count < is.getCount()) {
					is.splitStack(count);
					updatePlayerInventory(player);
					return true;
				}
				count -= is.getCount();
				player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
				if (count <= 0) {
					updatePlayerInventory(player);
					return true;
				}
			}
		}
		updatePlayerInventory(player);
		return false;
	}

	/* Vanilla Teleport in world */
	public void teleportEntity(Entity entityIn, CoordinateArg argX, CoordinateArg argY, CoordinateArg argZ, CoordinateArg argYaw, CoordinateArg argPitch) {
		if (entityIn instanceof EntityPlayerMP) {
			Set<SPacketPlayerPosLook.EnumFlags> set = EnumSet.noneOf(SPacketPlayerPosLook.EnumFlags.class);
			if (argX.isRelative()) { set.add(SPacketPlayerPosLook.EnumFlags.X); }
			if (argY.isRelative()) { set.add(SPacketPlayerPosLook.EnumFlags.Y); }
			if (argZ.isRelative()) { set.add(SPacketPlayerPosLook.EnumFlags.Z); }
			if (argPitch.isRelative()) { set.add(SPacketPlayerPosLook.EnumFlags.X_ROT); }
			if (argYaw.isRelative()) { set.add(SPacketPlayerPosLook.EnumFlags.Y_ROT); }
			float f = (float) argYaw.getAmount();
			if (!argYaw.isRelative()) { f = MathHelper.wrapDegrees(f); }
			float f1 = (float) argPitch.getAmount();
			if (!argPitch.isRelative()) { f1 = MathHelper.wrapDegrees(f1); }
			entityIn.dismountRidingEntity();
			((EntityPlayerMP) entityIn).connection.setPlayerLocation(argX.getAmount(), argY.getAmount(), argZ.getAmount(), f, f1, set);
			entityIn.setRotationYawHead(f);
		}
		else {
			float f2 = (float) MathHelper.wrapDegrees(argYaw.getResult());
			float f3 = (float) MathHelper.wrapDegrees(argPitch.getResult());
			f3 = MathHelper.clamp(f3, -90.0F, 90.0F);
			entityIn.setLocationAndAngles(argX.getResult(), argY.getResult(), argZ.getResult(), f2, f3);
			entityIn.setRotationYawHead(f2);
		}
		if (!(entityIn instanceof EntityLivingBase) || !((EntityLivingBase) entityIn).isElytraFlying()) {
			entityIn.motionY = 0.0D;
			entityIn.onGround = true;
		}
	}

	public Entity teleportEntity(MinecraftServer server, Entity entity, int dimension, BlockPos pos) throws CommandException {
		return teleportEntity(server, entity, dimension, pos.getX() + 0.5d, pos.getY(), pos.getZ() + 0.5d);
	}

	public Entity teleportEntity(MinecraftServer server, Entity entity, int dimension, double x, double y, double z) {
		Entity newEntity = entity;
		if (server != null && entity != null) {
			int homeDim = entity.world.provider.getDimension();
			if (entity instanceof EntityNPCInterface) {
				homeDim = ((EntityNPCInterface) entity).homeDimensionId;
			}
			if (entity.world.provider.getDimension() != dimension) {
				newEntity = travelAndCopyEntity(server, entity, dimension);
				if (entity instanceof EntityNPCInterface) {
					((EntityNPCInterface) entity).homeDimensionId = homeDim;
				}
			}
			if (newEntity == null) { newEntity = entity; }
			try {
				CoordinateArg xn = CommandBase.parseCoordinate(entity.posX, "" + x, true);
				CoordinateArg yn = CommandBase.parseCoordinate(entity.posY, "" + y, -4096, 4096, false);
				CoordinateArg zn = CommandBase.parseCoordinate(entity.posZ, "" + z, true);
				CoordinateArg w = CommandBase.parseCoordinate(entity.rotationYaw, "~", false);
				CoordinateArg p = CommandBase.parseCoordinate(entity.rotationPitch, "~", false);
				teleportEntity(newEntity, xn, yn, zn, w, p);
			}
			catch (Exception e) { newEntity.setPositionAndRotation(x, y, z, entity.rotationYaw, entity.rotationPitch); }
        }
		return entity;
	}

	@Override
	public String ticksToElapsedTime(long ticks, boolean isMilliSeconds, boolean colored, boolean upped) {
		String time = isMilliSeconds ? "0.000" : "--/--";
		String chr = "" + ((char) 167);
		if (ticks < 0) {
			return (colored ? chr + "8" : "") + time;
		}
		long timeSeconds = (isMilliSeconds ? ticks : ticks * 50L) / 1000L;
		int ms = (int) ((isMilliSeconds ? ticks : ticks * 50L) % 1000L);
		int sec = (int) (timeSeconds % 60L);
		int min = (int) (timeSeconds % 3600L) / 60;
		int hour = (int) (timeSeconds % 86400L) / 3600;
		int day = (int) (timeSeconds % 2592000L) / 86400;
		int month = (int) (timeSeconds % 31449600L) / 2620800;
		int year = (int) (timeSeconds / 31449600L);
		String mins, secs;
		if (min < 10) {
			mins = "0" + min;
		} else {
			mins = "" + min;
		}
		if (sec < 10) {
			secs = "0" + sec;
		} else {
			secs = "" + sec;
		}
		time = "";
		if (year > 0) {
			if (colored) {
				time += chr + "r" + year + chr + "6y ";
			} else {
				time += year + "y ";
			}
		}
		if (upped && !time.isEmpty()) {
			return time;
		}
		if (month > 0) {
			if (colored) {
				time += chr + "r" + month + chr + "1m ";
			} else {
				time += month + "m ";
			}
		}
		if (upped && !time.isEmpty()) {
			return time;
		}
		if (day > 0) {
			if (colored) {
				time += chr + "r" + day + chr + "2d ";
			} else {
				time += day + "d ";
			}
		}
		if (upped && !time.isEmpty()) {
			return time;
		}
		if (hour > 0 || year > 0 || month > 0 || day > 0) {
			if (colored) {
				time += chr + "r" + hour + ":";
			} else {
				time += hour + ":";
			}
		}
		time += (colored ? chr + "r" : "") + mins + ":" + secs;
		if (isMilliSeconds) {
			StringBuilder mss = new StringBuilder("" + ms);
			while (mss.length() < 3) { mss.insert(0, "0"); }
			time += (colored ? chr + "8" : "") + "." + mss;
		}
		return time;
	}

	@SuppressWarnings("ConstantConditions")
	public Entity travelAndCopyEntity(MinecraftServer server, Entity entity, int dimension) {
		if (server != null) {
			if (entity instanceof EntityPlayerMP) {
				try {
					WorldServer world = (WorldServer) server.getEntityWorld();
					if (world == null || world.provider == null) { world = (WorldServer) entity.world; }
					server.getPlayerList().transferPlayerToDimension((EntityPlayerMP) entity, dimension, new CustomNpcsTeleporter(world));
				} catch (Exception e) {
					LogWriter.error("Try travel player: "+entity.getName()+"; to "+dimension, e);
				}
				return entity;
			}
			return travelEntity(server, entity, dimension);
		}
		return entity;
	}

	/* [Teleport] Copy and Place Entity to Spawn next Dimensions */
	public Entity travelEntity(MinecraftServer server, Entity entity, int dimensionId) {
		if (entity.world.isRemote || entity.isDead) {
			return null;
		}
		ForgeHooks.onTravelToDimension(entity, dimensionId);
		entity.world.profiler.startSection("changeDimension");
		int dimensionStart = entity.dimension;
		WorldServer worldserverStart = server.getWorld(dimensionStart);
		WorldServer worldserverEnd = server.getWorld(dimensionId);
		entity.dimension = dimensionId;
		Entity newEntity = EntityList.createEntityByIDFromName(Objects.requireNonNull(EntityList.getKey(entity.getClass())), worldserverEnd);
		if (newEntity != null) {
			((IEntityIMixin) newEntity).npcs$copyDataFromOld(entity);
			entity.world.removeEntity(entity);
			newEntity.forceSpawn = true;
			worldserverEnd.spawnEntity(newEntity);
		}
		try {
            assert newEntity != null;
            worldserverEnd.updateEntityWithOptionalForce(newEntity, true);
			entity.isDead = true;
			entity.world.profiler.endSection();
			worldserverStart.resetUpdateEntityTick();
			worldserverEnd.resetUpdateEntityTick();
			entity.world.profiler.endSection();
		} catch (Exception e) { LogWriter.error(e); }
		return newEntity;
	}

	public void updatePlayerInventory(EntityPlayerMP player) {
		if (player == null) {
			return;
		}
		if (player.openContainer != null) { player.openContainer.detectAndSendChanges(); }
		else { player.inventoryContainer.detectAndSendChanges(); }
		PlayerQuestData playerdata = PlayerData.get(player).questData;
		for (QuestData data : playerdata.activeQuests.values()) {
			for (IQuestObjective obj : data.quest.getObjectives((IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(player))) {
				if (obj.getType() != 0) {
					continue;
				}
				playerdata.checkQuestCompletion(player, data);
			}
		}
	}

	// Stripping a string of color
	@Override
	public String deleteColor(String input) {
		if (input == null || input.isEmpty()) { return input; }
		return input.replaceAll("§[0-9A-Za-z]", "")
				.replaceAll("&[0-9A-Za-z]", "")
				.replaceAll("\uffff[0-9A-Za-z]", "");
	}

	@Override
	public double distanceTo(double x0, double y0, double z0, double x1, double y1, double z1) {
		double d0 = x0 - x1;
		double d1 = y0 - y1;
		double d2 = z0 - z1;
		return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
	}

	public double distanceTo(Entity entity, Entity target) {
		if (entity == null || target == null) { return 0.0d; }
		return distanceTo(entity.posX, entity.posY, entity.posZ, target.posX, target.posY, target.posZ);
	}

	@Override
	public double distanceTo(IEntity<?> entity, IEntity<?> target) {
		if (entity == null || target == null) { return 0.0d; }
		return distanceTo(entity.getMCEntity().posX, entity.getMCEntity().posY, entity.getMCEntity().posZ,
				target.getMCEntity().posX, target.getMCEntity().posY, target.getMCEntity().posZ);
	}

	@Override
	public @Nonnull IRayTraceRotate getAngles3D(double x0, double y0, double z0, double x1, double y1, double z1) {
		RayTraceRotate rtr = new RayTraceRotate();
		rtr.calculate(x0, y0, z0, x1, y1, z1);
		return rtr;
	}

	@Override
	public @Nonnull IRayTraceRotate getAngles3D(IEntity<?> entity, IEntity<?> target) {
		if (entity == null || target == null) { return RayTraceRotate.EMPTY; }
		return getAngles3D(entity.getMCEntity(), target.getMCEntity());
	}

	@Override
	public String getJSONStringFromObject(Object obj) {
		if (obj == null) { return ""; }
		LogWriter.debug("Trying to write object \"" + obj.getClass().getName() + "\" to JSON string");
		StringBuilder str = new StringBuilder();
		if (obj.getClass().isArray()) {
			str = new StringBuilder("[");
			for (Object value : (Object[]) obj) {
				String s = getJSONStringFromObject(value);
				if (str.length() > 0) {
					str.append(", ");
				}
				str.append(s);
			}
			str.append("]");
		} else if (obj instanceof Number) { str = new StringBuilder(obj.toString()); }
		else if (obj instanceof String) { str = new StringBuilder("\"" + obj + "\""); }
		else if (obj instanceof Bindings) {
			ScriptEngine engine = ScriptController.Instance.getEngineByName("ECMAScript");
			if (engine != null) {
				engine.put("temp", obj);
				try {
					str = new StringBuilder((String) engine.eval("JSON.stringify(temp)"));
				}
				catch (ScriptException e) { LogWriter.error(e); }
			}
		}
		return str.toString();
	}

	public InputStream getModInputStream(String fileName) {
		if (fileName == null || fileName.isEmpty() || fileName.lastIndexOf(".") == -1) {
			return null;
		}
		//LogWriter.debug("Getting a list of mod files by key \"" + fileName + "\"");
		InputStream inputStream = null;
		for (ModContainer mod : Loader.instance().getModList()) {
			if (mod.getSource().exists() && (mod.getModId().equals(CustomNpcs.MODID) || mod.getSource().getName().endsWith("bin") || mod.getSource().getName().endsWith("main"))) {
				if (!mod.getSource().isDirectory() && (mod.getSource().getName().endsWith(".jar") || mod.getSource().getName().endsWith(".zip"))) {
					try {
						ZipFile zip = new ZipFile(mod.getSource());
						Enumeration<? extends ZipEntry> entries = zip.entries();
						while (entries.hasMoreElements()) {
							ZipEntry zipentry = entries.nextElement();
							if (zipentry.isDirectory() || !zipentry.getName().endsWith(fileName)) {
								continue;
							}
							inputStream = zip.getInputStream(zipentry);
							break;
						}
						// java.util.zip.ZipFile.ZipFileInflaterInputStream -> java.io.ByteArrayInputStream
						if (inputStream != null) {
							InputStream copyStream = new ByteArrayInputStream(IOUtils.toByteArray(inputStream));
							IOUtils.closeQuietly(inputStream);
							inputStream = copyStream;
						}
						zip.close();
					} catch (Exception e) { LogWriter.error(e); }
				} else {
					List<File> list = getFiles(mod.getSource(), fileName.substring(fileName.lastIndexOf(".")));
					for (File file : list) {
						if (!file.isFile() || !file.getName().equals(fileName)) { continue; }
						try {
							inputStream = Files.newInputStream(file.toPath());
						}
						catch (Exception e) { LogWriter.error(e); }
						break;
					}
				}
			}
			if (inputStream != null) { break; }
		}
		return inputStream;
	}

	@Override
	public String loadFile(File file) {
		//LogWriter.debug("Trying to load file \"" + file.getAbsolutePath() + "\"");
		StringBuilder text = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				text.append(line).append((char) 10);
			}
			reader.close();
		}
		catch (Exception e) { LogWriter.error("Error load file \"" + file.getAbsolutePath() + "\"", e); }
		return text.toString();
	}

	@Override
	public boolean saveFile(File file, String text) {
		if (file == null || text == null) {
			return false;
		}
		//LogWriter.debug("Trying save text to file \"" + file.getAbsolutePath() + "\"");
		if (file.getParentFile() != null && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) { // create directories
			LogWriter.debug("Error creating directories from file path \"" + file.getAbsolutePath() + "\"");
			return false;
		}
		try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
			writer.write(text);
		} catch (IOException e) {
			LogWriter.debug("Error Save Default Item File \"" + file.getAbsolutePath() + "\"");
			return false;
		}
		return true;
	}

	@Override
	public boolean saveFile(File file, NBTTagCompound compound) {
		if (compound == null) { return false; }
		return saveFile(file, NBTJsonUtil.Convert(compound));
	}

	@Override
	public String getDataFile(String fileName) {
		if (fileName == null) { return ""; }
		//LogWriter.debug("Trying to get text from mod data file \"" + fileName + "\"");
		InputStream inputStream = getModInputStream(fileName);
		String text = "";
		try {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			for (int length; (length = inputStream.read(buffer)) != -1; ) {
				result.write(buffer, 0, length);
			}
			text = result.toString("UTF-8");
		}
		catch (Throwable t) { LogWriter.error("Error get text from mod data file: \"" + fileName + "\"; InputStream: " + inputStream, t); }
		return text;
	}

	@Override
	public IRayTraceVec getPosition(double x, double y, double z, double yaw, double pitch, double radius) {
		RayTraceVec rtv = new RayTraceVec();
		rtv.calculatePos(x, y, z, yaw, pitch, radius);
		return rtv;
	}

	@Override
	public IRayTraceVec getPosition(IEntity<?> entity, double yaw, double pitch, double radius) {
		if (entity == null) { return RayTraceVec.EMPTY; }
		return getPosition(entity.getMCEntity().posX, entity.getMCEntity().posY, entity.getMCEntity().posZ, yaw,
				pitch, radius);
	}

	@Override
	public RayTraceVec getVector3D(double x0, double y0, double z0, double x1, double y1, double z1) {
		RayTraceVec rtv = new RayTraceVec();
		rtv.calculateVec(x0, y0, z0, x1, y1, z1);
		return rtv;
	}

	@Override
	public RayTraceVec getVector3D(IEntity<?> entity, IEntity<?> target) {
		if (entity == null || target == null) { return RayTraceVec.EMPTY; }
		return getVector3D(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
	}

	@Override
	public RayTraceVec getVector3D(IEntity<?> entity, IPos pos) {
		if (entity == null || pos == null) { return RayTraceVec.EMPTY; }
		return getVector3D(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), pos.getX(), pos.getY(), pos.getZ());
	}

	public IRayTraceResults rayTraceBlocksAndEntitys(Entity entity, double yaw, double pitch, double distance) {
		if (entity == null || entity.world == null || distance <= 0.0d) { return RayTraceResults.EMPTY; }
		RayTraceResults rtrs = new RayTraceResults();

		Vec3d vecStart = entity.getPositionEyes(1.0f);
		double rad = Math.PI / 180.0d;
		double f = Math.cos(-yaw * rad - Math.PI);
		double f1 = Math.sin(-yaw * rad - Math.PI);
		double f2 = -Math.cos(-pitch * rad);
		double f3 = Math.sin(-pitch * rad);
		Vec3d vecLook = new Vec3d(f1 * f2, f3, f * f2);
		Vec3d vecEnd = vecStart.addVector(vecLook.x * distance, vecLook.y * distance, vecLook.z * distance);
		rtrs.add(entity, distance, vecStart, vecEnd);

		int x0 = MathHelper.floor(vecStart.x);
		int y0 = MathHelper.floor(vecStart.y);
		int z0 = MathHelper.floor(vecStart.z);
		int x1 = MathHelper.floor(vecEnd.x);
		int y1 = MathHelper.floor(vecEnd.y);
		int z1 = MathHelper.floor(vecEnd.z);

		BlockPos pos = new BlockPos(x0, y0, z0);
		IBlockState state = entity.world.getBlockState(pos);
		rtrs.add(entity.world, pos, state);

		int k1 = 200;
		while (k1-- >= 0) {
			if (x0 == x1 && y0 == y1 && z0 == z1) { return rtrs; }

			boolean butEqualX = true;
			boolean butEqualY = true;
			boolean butEqualZ = true;
			double d0 = 999.0D;
			double d1 = 999.0D;
			double d2 = 999.0D;

			if (x1 > x0) {
				d0 = (double) x0 + 1.0D;
			} else if (x1 < x0) {
				d0 = (double) x0 + 0.0D;
			} else {
				butEqualX = false;
			}

			if (y1 > y0) {
				d1 = (double) y0 + 1.0D;
			} else if (y1 < y0) {
				d1 = (double) y0 + 0.0D;
			} else {
				butEqualY = false;
			}

			if (z1 > z0) {
				d2 = (double) z0 + 1.0D;
			} else if (z1 < z0) {
				d2 = (double) z0 + 0.0D;
			} else {
				butEqualZ = false;
			}

			double d3 = 999.0D;
			double d4 = 999.0D;
			double d5 = 999.0D;
			double d6 = vecEnd.x - vecStart.x;
			double d7 = vecEnd.y - vecStart.y;
			double d8 = vecEnd.z - vecStart.z;

			if (butEqualX) {
				d3 = (d0 - vecStart.x) / d6;
			}
			if (butEqualY) {
				d4 = (d1 - vecStart.y) / d7;
			}
			if (butEqualZ) {
				d5 = (d2 - vecStart.z) / d8;
			}

			if (d3 == -0.0D) {
				d3 = -1.0E-4D;
			}
			if (d4 == -0.0D) {
				d4 = -1.0E-4D;
			}
			if (d5 == -0.0D) {
				d5 = -1.0E-4D;
			}

			EnumFacing enumfacing;
			if (d3 < d4 && d3 < d5) {
				enumfacing = x1 > x0 ? EnumFacing.WEST : EnumFacing.EAST;
				vecStart = new Vec3d(d0, vecStart.y + d7 * d3, vecStart.z + d8 * d3);
			} else if (d4 < d5) {
				enumfacing = y1 > y0 ? EnumFacing.DOWN : EnumFacing.UP;
				vecStart = new Vec3d(vecStart.x + d6 * d4, d1, vecStart.z + d8 * d4);
			} else {
				enumfacing = z1 > z0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
				vecStart = new Vec3d(vecStart.x + d6 * d5, vecStart.y + d7 * d5, d2);
			}

			x0 = MathHelper.floor(vecStart.x) - (enumfacing == EnumFacing.EAST ? 1 : 0);
			y0 = MathHelper.floor(vecStart.y) - (enumfacing == EnumFacing.UP ? 1 : 0);
			z0 = MathHelper.floor(vecStart.z) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
			pos = new BlockPos(x0, y0, z0);
			state = entity.world.getBlockState(pos);
			rtrs.add(entity.world, pos, state);
		}
		return rtrs;
	}

	@Override
	public IRayTraceResults rayTraceBlocksAndEntitys(IEntity<?> entity, double yaw, double pitch, double distance) {
		if (entity == null) { return RayTraceResults.EMPTY; }
		return rayTraceBlocksAndEntitys(entity.getMCEntity(), yaw, pitch, distance);
	}

	@Override
	public Object readObjectFromNbt(NBTBase tag) {
		if (tag == null) { return null; }
		if (tag instanceof NBTTagCompound) {
			NBTTagCompound compound = (NBTTagCompound) tag;
			if (compound.getKeySet().isEmpty()) { return null; }
			if (compound.getBoolean("IsBindings")) {
				ScriptEngine engine = ScriptController.Instance.getEngineByName("ECMAScript");
				if (engine == null) { return null; }
				boolean isArray = compound.getBoolean("IsArray");
				try {
					StringBuilder str = new StringBuilder("JSON.parse('" + (isArray ? "[" : "{"));
					Set<String> sets = ((NBTTagCompound) tag).getKeySet();
					Map<String, Object> map = new TreeMap<>();
					for (String k : sets) {
						if (k.equals("IsArray") || k.equals("IsBindings")) { continue; }
						Object v = readObjectFromNbt(((NBTTagCompound) tag).getTag(k));
						if (v != null) { map.put(k, v); }
					}
					for (String k : map.keySet()) {
						String s = getJSONStringFromObject(map.get(k));
						if (isArray) { str.append(s).append(", "); }
						else { str.append("\"").append(k).append("\":").append(s).append(", "); }
					}
					if (!map.isEmpty()) { str = new StringBuilder(str.substring(0, str.length() - 2)); }
					str.append(isArray ? "]" : "}").append("')");
					try { return engine.eval("" +str); }
					catch (Exception e) {
						LogWriter.error("Error parse \""+str+"\"", e);
					}
                    return null;
				} catch (Exception e) { LogWriter.error(e); }
			}
			else if (compound.getBoolean("IsList")) {
				Map<Integer, Object> map = new TreeMap<>();
				for (String k : compound.getKeySet()) {
					try { map.put(Integer.parseInt(k.replace("K", "")), readObjectFromNbt(Objects.requireNonNull(compound.getTag(k)))); }
					catch (Exception e) { map.put(map.size(), null); }
				}
				return new ArrayList<>(map.values());
			}
			else if (compound.hasKey("IsMap", 3)) {
				Map<Object, Object> map;
				switch (compound.getInteger("IsMap")) {
					case 1: map = new TreeMap<>(); break;
					case 2: map = new LinkedHashMap<>(); break;
					case 3: map = new LinkedTreeMap<>(); break;
					default: map = new HashMap<>(); break;
				}
				NBTTagCompound content = compound.getCompoundTag("Content");
				Map<Integer, NBTTagCompound> keys = new TreeMap<>();
				for (String key : content.getKeySet()) {
					try	{ keys.put( Integer.parseInt(key.replace("Slot_", "")), content.getCompoundTag(key)); } catch (Exception ignored) { }
                }
				for (NBTTagCompound nbt : keys.values()) {
					Object k = readObjectFromNbt(nbt.getTag("K"));
					Object v = readObjectFromNbt(nbt.getTag("V"));
					if (k != null && v != null) { map.put(k, v); }
				}
				return map;
			}
			else if (compound.hasKey("IsNBT", 1)) {
				NBTBase nbt = compound.getTag("V");
				if (!compound.getBoolean("IsNBT") && nbt instanceof NBTTagCompound) { return new NBTWrapper((NBTTagCompound) nbt); }
				return nbt;
			}
			else if (compound.getBoolean("IsJSON")) {
				try {
					Class<?> clss = Class.forName(compound.getString("Class"));
					return gson.fromJson(compound.getString("Content"), clss);
				} catch (Exception ignored) { }
			}
			else if (compound.getBoolean("IsColor")) { return new Color(compound.getInteger("V")); }
			else if (compound.getBoolean("IsBoolean")) { return compound.getBoolean("V"); }
		}
		else if (tag instanceof NBTTagEnd) { return null; }
		else if (tag instanceof NBTTagByte) { return ((NBTTagByte) tag).getByte(); }
		else if (tag instanceof NBTTagShort) { return ((NBTTagShort) tag).getShort(); }
		else if (tag instanceof NBTTagInt) { return ((NBTTagInt) tag).getInt(); }
		else if (tag instanceof NBTTagLong) { return ((NBTTagLong) tag).getLong(); }
		else if (tag instanceof NBTTagFloat) { return ((NBTTagFloat) tag).getFloat(); }
		else if (tag instanceof NBTTagDouble) { return ((NBTTagDouble) tag).getDouble(); }
		else if (tag instanceof NBTTagString) { return ((NBTTagString) tag).getString(); }
		else if (tag instanceof NBTTagByteArray) { return ((NBTTagByteArray) tag).getByteArray(); }
		else if (tag instanceof NBTTagIntArray) { return ((NBTTagIntArray) tag).getIntArray(); }
		else if (tag instanceof NBTTagLongArray) { return ((INBTTagLongArrayMixin) tag).getData(); }
		else if (tag instanceof NBTTagList) {
			Object[] arr = new Object[((NBTTagList) tag).tagCount()];
			int i = 0;
			for (NBTBase listTag : (NBTTagList) tag) {
				arr[i] = readObjectFromNbt(listTag);
				i++;
			}
			return arr;
		}
		LogWriter.warn("Not read tag: \""+tag+"\" to Object");
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public @Nullable NBTBase writeObjectToNbt(Object value) {
		if (value == null) { return null; }
		if (value instanceof NBTBase || value instanceof INbt) {
			NBTTagCompound compound = new NBTTagCompound();
			compound.setBoolean("IsNBT", value instanceof NBTBase);
			if (value instanceof NBTBase) { compound.setTag("V", (NBTBase) value); }
			else { compound.setTag("V", ((INbt) value).getMCNBT());}
			return compound;
		}
		else if (value.getClass().isArray()) {
			Object[] values = (Object[]) value;
			NBTTagList list = new NBTTagList();
			for (Object v : values) {
				NBTBase tag = writeObjectToNbt(v);
				if (tag != null) { list.appendTag(tag); }
			}
			return list;
		}
		else if (value instanceof Boolean) {
			NBTTagCompound compound = new NBTTagCompound();
			compound.setBoolean("IsBoolean", true);
			compound.setBoolean("V", (Boolean) value);
			return compound;
		}
		else if (value instanceof Byte) { return new NBTTagByte((Byte) value); }
		else if (value instanceof Short) { return new NBTTagShort((Short) value); }
		else if (value instanceof Integer) { return new NBTTagInt((Integer) value); }
		else if (value instanceof Color) {
			NBTTagCompound compound = new NBTTagCompound();
			compound.setBoolean("IsColor", true);
			compound.setInteger("V", ((Color) value).getRGB());
			return compound;
		}
		else if (value instanceof Long) { return new NBTTagLong((Long) value); }
		else if (value instanceof Float) { return new NBTTagFloat((Float) value); }
		else if (value instanceof Double) { return new NBTTagDouble((Double) value); }
		else if (value instanceof Number) { return new NBTTagDouble(((Number) value).doubleValue()); }
		else if (value instanceof String) { return new NBTTagString((String) value); }
		else if (value instanceof Bindings) {
			String clazz = value.toString();
			if (!clazz.equals("[object Array]") && !clazz.equals("[object Object]")) { return null; }
			boolean isArray = clazz.equals("[object Array]");
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setBoolean("IsArray", isArray);
			nbt.setBoolean("IsBindings", true);
			for (Map.Entry<String, Object> scopeEntry : ((Bindings) value).entrySet()) {
				Object v = scopeEntry.getValue();
				if (v.getClass().isArray()) {
					Object[] vs = (Object[]) v;
					if (vs.length == 0) { nbt.setTag(scopeEntry.getKey(), new NBTTagList()); }
					else if (vs[0] instanceof Byte) {
						List<Byte> l = new ArrayList<>();
						for (Object va : vs) {
							if (va instanceof Byte) { l.add((Byte) va); }
						}
						byte[] arr = new byte[l.size()];
						int i = 0;
						for (byte d : l) {
							arr[i] = d;
							i++;
						}
						nbt.setByteArray(scopeEntry.getKey(), arr);
					}
					else if (vs[0] instanceof Integer) {
						List<Integer> l = new ArrayList<>();
						for (Object va : vs) {
							if (va instanceof Integer) { l.add((Integer) va); }
						}
						int[] arr = new int[l.size()];
						int i = 0;
						for (int d : l) {
							arr[i] = d;
							i++;
						}
						nbt.setIntArray(scopeEntry.getKey(), arr);
					}
					else if (vs[0] instanceof Long) {
						List<Long> l = new ArrayList<>();
						for (Object va : vs) {
							if (va instanceof Long) { l.add((Long) va); }
						}
						long[] arr = new long[l.size()];
						int i = 0;
						for (long d : l) {
							arr[i] = d;
							i++;
						}
						nbt.setTag(scopeEntry.getKey(), new NBTTagLongArray(arr));
					}
					else if (vs[0] instanceof String) {
						NBTTagList list = new NBTTagList();
						for (Object va : vs) { list.appendTag(new NBTTagString((String) va)); }
						nbt.setTag(scopeEntry.getKey(), list);
					}
					else if (vs[0] instanceof Short || vs[0] instanceof Float || vs[0] instanceof Double || vs[0] instanceof Number) {
						NBTTagList list = new NBTTagList();
						for (Object va : vs) {
							double d;
							if (va instanceof Short) { d = (double) (Short) va; }
							else if (va instanceof Float) { d = (double) (Float) va; }
							else if (va instanceof Double) { d = (Double) va; }
							else if (va instanceof Number) { d = ((Number) va).doubleValue(); }
							else { continue; }
							list.appendTag(new NBTTagDouble(d));
						}
						nbt.setTag(scopeEntry.getKey(), list);
					}
					else { nbt.setTag(scopeEntry.getKey(), new NBTTagList()); }
				}
				else if (v instanceof Byte) { nbt.setByte(scopeEntry.getKey(), (Byte) v); }
				else if (v instanceof Short) { nbt.setShort(scopeEntry.getKey(), (Short) v); }
				else if (v instanceof Integer) { nbt.setInteger(scopeEntry.getKey(), (Integer) v); }
				else if (v instanceof Long) { nbt.setLong(scopeEntry.getKey(), (Long) v); }
				else if (v instanceof Float) { nbt.setFloat(scopeEntry.getKey(), (Float) v); }
				else if (v instanceof Double) { nbt.setDouble(scopeEntry.getKey(), (Double) v); }
				else if (v instanceof Number) {nbt.setDouble(scopeEntry.getKey(), ((Number) v).doubleValue()); }
				else if (v instanceof String) { nbt.setString(scopeEntry.getKey(), (String) v); }
				else {
					NBTBase n = writeObjectToNbt(v);
					if (n != null) { nbt.setTag(scopeEntry.getKey(), n); }
				}
			}
			return nbt;
		}
		else if (value instanceof Map) {
			try {
				Map<Object, Object> map = (Map<Object, Object>) value;
				NBTTagCompound compound = new NBTTagCompound();
				int type = 0; // HashMap
				if (value instanceof TreeMap) { type = 1; }
				else if (value instanceof LinkedHashMap) { type = 2; }
				else if (value instanceof LinkedTreeMap) { type = 3; }
				compound.setInteger("IsMap", type);
				NBTTagCompound content = new NBTTagCompound();
				int i = 0;
				for (Object key : map.keySet()) {
					NBTBase k = writeObjectToNbt(key);
					NBTBase v = writeObjectToNbt(map.get(key));
					if (k != null && v != null) {
						NBTTagCompound nbt = new NBTTagCompound();
						nbt.setTag("K", k);
						nbt.setTag("V", v);
						content.setTag("Slot_"+i, nbt);
					}
					i++;
				}
				compound.setTag("Content", content);
				return compound;
			}
			catch (Exception ignored) { }
		}
		else if (value instanceof List) {
			try {
				List<Object> list = (List<Object>) value;
				NBTTagCompound compound = new NBTTagCompound();
				compound.setBoolean("IsList", true);
				int i = 0;
				for (Object obj : list) {
					NBTBase tag = writeObjectToNbt(obj);
					if (tag == null) { continue; }
					compound.setTag("K" + i, tag);
					i++;
				}
				return compound;
			}
			catch (Exception ignored) { }
		}
		try {
			String jsonString = gson.toJson(value);
			Object obj = gson.fromJson(jsonString, value.getClass());
			if (obj != null) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setBoolean("IsJSON", true);
				compound.setString("Class", value.getClass().getName());
				compound.setString("Content", jsonString);
				return compound;
			}
		}
		catch (Exception ignored) {  }
		LogWriter.warn("Not write object: \""+value+"\" to NBT");
		return null;
	}

	@Override
	public IEntity<?> transferEntity(IEntity<?> entity, int dimension, IPos pos) {
		Entity e = null;
		try {
			if (pos != null) {
				e = teleportEntity(CustomNpcs.Server, entity.getMCEntity(), dimension, pos.getMCBlockPos());
			} else {
				e = travelAndCopyEntity(CustomNpcs.Server, entity.getMCEntity(), dimension);
			}
		} catch (Exception ee) { LogWriter.error(ee); }
		if (e != null) {
			return Objects.requireNonNull(NpcAPI.Instance()).getIEntity(e);
		}
		return entity;
	}


	public void sort(NonNullList<ItemStack> items) {
		Map<String, List<ItemStack>> mapArmor = new TreeMap<>();
		Map<String, List<ItemStack>> mapPotion = new TreeMap<>();
		Map<Integer, List<ItemStack>> mapSimple = new TreeMap<>();
		Map<String, List<ItemStack>> mapAny = new TreeMap<>();
		// Collect
		for (ItemStack stack : items) {
			if (stack.getItem() instanceof CustomArmor) {
				String key = ((CustomArmor) stack.getItem()).getCustomName();
				if (!mapArmor.containsKey(key)) { mapArmor.put(key, new ArrayList<>()); }
				mapArmor.get(key).add(stack);
			}
			else if (stack.getItem() instanceof ItemPotion) {
				String key = stack.getItem().getClass().getSimpleName();
				if (!mapPotion.containsKey(key)) { mapPotion.put(key, new ArrayList<>()); }
				mapPotion.get(key).add(stack);
			}
			else if (stack.getItem() instanceof ICustomElement) {
				int key = ((ICustomElement) stack.getItem()).getElementType();
				if (!mapSimple.containsKey(key)) { mapSimple.put(key, new ArrayList<>()); }
				mapSimple.get(key).add(stack);
			}
			else {
				String key = stack.getItem().getClass().getSimpleName();
				if (!mapAny.containsKey(key)) { mapAny.put(key, new ArrayList<>()); }
				mapAny.get(key).add(stack);
			}
		}
		items.clear();
		// sort
		for (List<ItemStack> list: mapArmor.values()) {
			list.sort((st_0, st_1) -> {
                CustomArmor a_0 = (CustomArmor) st_0.getItem();
                CustomArmor a_1 = (CustomArmor) st_1.getItem();
                return Integer.compare(a_0.getEquipmentSlot().ordinal(), a_1.getEquipmentSlot().ordinal());
            });
			items.addAll(list);
		}
		for (List<ItemStack> list: mapPotion.values()) {
			list.sort((st_0, st_1) -> st_1.getDisplayName().compareTo(st_0.getDisplayName()));
			items.addAll(list);
		}
		for (List<ItemStack> list: mapSimple.values()) {
			list.sort((st_0, st_1) -> st_1.getDisplayName().compareTo(st_0.getDisplayName()));
			items.addAll(list);
		}
		for (List<ItemStack> list: mapAny.values()) {
			list.sort((st_0, st_1) -> st_1.getDisplayName().compareTo(st_0.getDisplayName()));
			items.addAll(list);
		}
	}

	public Entity getLookEntity(Entity entity, Double d0, boolean aliveOnly) {
		Entity target = null;
		if (d0 == null) {
			d0 = 32.0;
			if (entity instanceof EntityPlayer) { d0 = PlayerData.get((EntityPlayer) entity).game.blockReachDistance; }
		}
		Vec3d vec3d1 = entity.getLook(1.0F);
		Vec3d vec3d = entity.getPositionEyes(1.0f);
        Vec3d vec3d2 = vec3d.addVector(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0);
		List<Entity> list = new ArrayList<>();
		try {
			list = entity.world.getEntitiesWithinAABB(Entity.class, entity.getEntityBoundingBox().expand(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0).grow(1.0D, 1.0D, 1.0D));
		}
		catch (Exception ignored) { }
        list.remove(entity);
        double d2 = d0;
        Vec3d vec3d3 = null;
        for (Entity e : list) {
            if (!e.isEntityAlive() && aliveOnly || (e instanceof EntityNPCInterface) && ((EntityNPCInterface) e).stats.hideKilledBody) { continue; }
			AxisAlignedBB axisalignedbb = e.getEntityBoundingBox().grow(e.getCollisionBorderSize());
            RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);
            if (axisalignedbb.contains(vec3d)) {
                if (d2 >= 0.0D) {
                    target = e;
                    vec3d3 = raytraceresult == null ? vec3d : raytraceresult.hitVec;
                    d2 = 0.0D;
                }
            } else if (raytraceresult != null) {
                double d3 = vec3d.distanceTo(raytraceresult.hitVec);
                if (d3 < d2 || d2 == 0.0D) {
                    if (e.getLowestRidingEntity() == entity.getLowestRidingEntity() && !e.canRiderInteract()) {
                        if (d2 == 0.0D) {
                            target = e;
                            vec3d3 = raytraceresult.hitVec;
                        }
                    } else {
                        target = e;
                        vec3d3 = raytraceresult.hitVec;
                        d2 = d3;
                    }
                }
            }
            if (target != null) {
                break;
            }
        }
        if (target != null) {
        	RayTraceResult er = new RayTraceResult(target, vec3d3);
        	RayTraceResult eb = entity.world.rayTraceBlocks(vec3d, vec3d2, false, false, false);
        	if (eb != null) {
        		Vec3d pp = new Vec3d(entity.posX, entity.posY, entity.posZ);
        		if (er.hitVec.distanceTo(pp) >= eb.hitVec.distanceTo(pp)) { target = null; }
        	}
        }
		return target;
	}

	public String translateGoogle(EntityPlayer player, String originalText) {
		return translateGoogle("en", CustomNpcs.proxy.getTranslateLanguage(player), originalText);
	}

	@Override
	public String translateGoogle(String textLanguageKey, String translationLanguageKey, String originalText) {
		return TranslateUtil.translate(textLanguageKey, translationLanguageKey, originalText);
	}

	public boolean canMoveEntityToEntity(EntityNPCInterface entity, EntityLivingBase entityTo) {
		if (entity == null || entityTo == null) { return false; }
		Path path = entity.getNavigator().getPathToEntityLiving(entityTo);
		if (path == null) { return false; }
		PathPoint pos = path.getFinalPathPoint();
		if (pos == null) { return false; }
		return Math.abs(entityTo.posX - (double) pos.x) <= 1.0 && Math.abs(entityTo.posY - (double) pos.y) < 2.0d && Math.abs(entityTo.posZ - (double) pos.z) <= 1.0d;
	}

	@SuppressWarnings("ConstantConditions")
	public float getCurrentXZSpeed(EntityLivingBase entity) {
		IAttributeInstance movementAttribute = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
		float speed = 1.0f;
		if (movementAttribute != null) {
			if (movementAttribute.getBaseValue() != 0.0d) {
				speed = (float) (movementAttribute.getAttributeValue() / movementAttribute.getBaseValue());
			}
		}
		return ValueUtil.correctFloat(speed, 0.25f, 1.0f);
	}

	@SuppressWarnings("ConstantConditions")
	public boolean isMoving(EntityLivingBase entity) {
		if (entity instanceof EntityLiving && !((EntityLiving) entity).getNavigator().noPath()) { return true; }
		IAttributeInstance movementAttribute = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
		double speed = 0.004d;
		if (movementAttribute != null) {
			speed = movementAttribute.getBaseValue() / 5.0d;
		}
		return Math.sqrt(Math.pow(entity.motionX, 2.0d) + Math.pow(entity.motionZ, 2.0d)) > speed;
	}

	public int getColorI(int index) {
		switch (index) {
			case 0: return 0x000000; // BLACK
			case 1: return 0x0000A8; // DARK_BLUE
			case 2: return 0x00A800; // DARK_GREEN
			case 3: return 0x00A8A8; // DARK_AQUA
			case 4: return 0xA80000; // DARK_RED
			case 5: return 0xA800A8; // DARK_PURPLE
			case 6: return 0xFFAA00; // GOLD
			case 7: return 0xA8A8A8; // GRAY
			case 8: return 0x545454; // DARK_GRAY
			case 9: return 0x5757FF; // BLUE
			case 10: return 0x57FF57; // a / GREEN
			case 11: return 0x57FFFF; // b / AQUA
			case 12: return 0xFF5757; // c / RED
			case 13: return 0xFF57FF; // d / LIGHT_PURPLE
			case 14: return 0xFFFF57; // e / YELLOW
		}
		return 0xFFFFFF; // f / WHITE
	}

	public float[] getColorF(int index) {
		int c = getColorI(index);
		return new float[] {
				(float)(c >> 16 & 255) / 255.0F, // red
				(float)(c >> 8 & 255) / 255.0F, // green
				(float)(c & 255) / 255.0F, // blue
				1.0f // alpha
		};
	}

	public <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(Map<K, V> map) {
		if (map == null || map.isEmpty()) { return new LinkedHashMap<>(); }
		Comparator<Map.Entry<K, V>> comparator = null;
		V value = map.values().iterator().next();
		if (value instanceof String) { comparator = Map.Entry.comparingByValue(); }
		else if (value instanceof Integer) { comparator = Comparator.comparingInt(e -> (Integer) e.getValue()); }
		else if (value instanceof Long) { comparator = Comparator.comparingLong(e -> (Long) e.getValue()); }
		else if (value instanceof Double || value instanceof Float) { comparator = Comparator.comparingDouble(e -> ((Number) e.getValue()).doubleValue()); }
		if (comparator != null) {
			return map.entrySet()
					.stream()
					.sorted(comparator)
					.collect(Collectors.toMap(
							Map.Entry::getKey,
							Map.Entry::getValue,
							(e1, e2) -> e1,
							LinkedHashMap::new
					));
		}
		return new LinkedHashMap<>(map);
	}

	public static List<String> splitString(String input) {
		List<String> result = new ArrayList<>();
		if (input != null && !input.isEmpty()) {
			byte[] abyte = input.getBytes(StandardCharsets.UTF_8);
			StringBuilder temp = new StringBuilder();
			int size = 0;
			for (int i = 0; i < abyte.length; i++) {
				if (size + abyte[i] > 32768) {
					result.add(temp.toString());
					temp = new StringBuilder("" + input.charAt(i));
					size = 0;
				}
				else {
					temp.append(input.charAt(i));
					size += abyte[i];
				}
			}
			if (!temp.toString().isEmpty()) { result.add(temp.toString()); }
		}
		return result;
	}

	public Side getSide() {
		return FMLCommonHandler.instance().getEffectiveSide();
	}

	@SideOnly(Side.CLIENT)
	public void putHovers(List<Component> hoverText, Object... components) {
		if (hoverText == null || components == null) { return; }
		for (Object component : components) {
			if (component == null) { continue; }
			if (component instanceof List<?>) {
				putHovers(hoverText, ((List<?>) component).toArray());
				continue;
			}
			String text = null;
			if (component instanceof String) {
				String lines = (String) component;
				if (!lines.contains("%")) { text = Component.translatable(lines).getFormattedText(); }
				else { hoverText.add(Component.literal(lines)); }
			}
			else if (component instanceof Component) { text = ((Component) component).getFormattedText(); }
			else if (component instanceof ITextComponent) { text = ((ITextComponent) component).getFormattedText(); }
			if (text != null) {
				if (text.contains("~~~")) { text = text.replaceAll("~~~", "%"); }
				while (text.contains("<br>")) {
					hoverText.add(Component.literal(text.substring(0, text.indexOf("<br>"))));
					text = text.substring(text.indexOf("<br>") + 4);
				}
				if (!text.isEmpty()) { hoverText.add(Component.literal(text)); }
			}
		}
	}

	@SuppressWarnings("unused")
	public void jumpTowards(IEntity<?> iEntity, IPos iPos) { jumpTowards(1.3f, iEntity.getMCEntity(), iPos.getMCVec3()); }

	public void jumpTowards(float speed, Entity entity, Vec3d vec) {
		double x = vec.x - entity.posX;
		double y = vec.y - entity.getEntityBoundingBox().minY;
		double z = vec.z - entity.posZ;
		float varF = (float) Math.sqrt(x * x + z * z);
		float pitch = getPitch(speed, y, varF);
		float yaw = (float)(Math.atan2(x, z) * 180.0D / Math.PI);
		float f0 = (float) Math.PI;
		Vec3d motion = new Vec3d(Math.sin(yaw / 180.0F * f0) * Math.cos(pitch / 180.0F * f0),
				Math.sin((pitch + 1.0F) / 180.0F * f0),
				Math.cos(yaw / 180.0F * f0) * Math.cos(pitch / 180.0F * f0));
		motion.scale(speed);
		entity.motionX = motion.x;
		entity.motionY = motion.y;
		entity.motionZ = motion.z;
		entity.velocityChanged = true;
	}

	public float getPitch(float speed, double y, double horizontalDist) {
		float f0 = 0.2F;
		float f1 = speed * speed;
		double f2 = (double) f0 * horizontalDist;
		double f3 = (double) f0 * horizontalDist * horizontalDist + 2.0D * y * (double) f1;
		double f4 = (double) (f1 * f1) - (double) f0 * f3;
		if (f4 < 0.0D) { return 90.0F; }
		float f5 = f1 - (float) Math.sqrt(f4);
		return (float) (Math.atan2(f5, f2) * 180.0D / Math.PI);
	}

}