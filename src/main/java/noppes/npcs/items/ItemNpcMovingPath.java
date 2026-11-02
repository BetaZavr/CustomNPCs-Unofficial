package noppes.npcs.items;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.*;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.client.ClientEventHandler;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketMenuSave;
import noppes.npcs.packets.server.SPacketResetItemMoving;
import noppes.npcs.shared.common.CommonUtil;

public class ItemNpcMovingPath extends Item implements INPCToolItem {

	public ItemNpcMovingPath() {
		setRegistryName(CustomNpcs.MODID, "npcmovingpath");
		setUnlocalizedName("npcmovingpath");
		setFull3D();
		maxStackSize = 1;
		setCreativeTab(CustomTabs.TOOLS);
	}

	public static void register(EntityNPCInterface npc, ItemStack stack, EntityPlayer player) {
		NBTTagCompound compound = stack.getTagCompound();
		if (compound == null) { stack.setTagCompound(compound = new NBTTagCompound()); }
		UUID uuid = compound.getUniqueId("NPCUUID");
		if (compound.getInteger("NPCID") != npc.getEntityId() ||
				uuid == null || !uuid.equals(npc.getUniqueID()) ||
				compound.getInteger("NPCDIM") != npc.world.provider.getDimension()) {
			compound.setInteger("NPCID", npc.getEntityId());
			compound.setUniqueId("NPCUUID", npc.getUniqueID());
			compound.setInteger("NPCDIM", npc.world.provider.getDimension());
			player.sendMessage(Component.translatable("message.pather.register", npc.getName(), stack.getDisplayName()).getParent());
			if (player instanceof  EntityPlayerMP) {
				Packets.send((EntityPlayerMP) player, new PacketMenuSave(npc, EnumMenuType.MOVING_PATH));
			}
		}
	}

	public static @Nullable EntityNPCInterface getNpc(@Nonnull ItemStack stack, @Nonnull World worldIn) {
		NBTTagCompound compound = stack.getTagCompound();
		if (compound != null && compound.hasKey("NPCID", 3)) {
			World world = worldIn;
			Entity entity = null;
			if (compound.hasKey("NPCDIM", 3)) {
				int levelKey = compound.getInteger("NPCDIM");
				if (world.provider.getDimension() != levelKey) {
					MinecraftServer server = world.getMinecraftServer() != null ? world.getMinecraftServer() : CustomNpcs.Server;
					if (server != null) { world = server.getWorld(levelKey); }
					else { world = null; }
				}
				if (world != null) {
					entity = world.getEntityByID(compound.getInteger("NPCID"));
					if (!(entity instanceof EntityNPCInterface)) {
						UUID uuid = compound.getUniqueId("NPCUUID");
						for (Entity e : world.loadedEntityList) {
							if (e.getUniqueID().equals(uuid)) {
								entity = e;
								break;
							}
						}
					}
				}
			}
			else { entity = world.getEntityByID(compound.getInteger("NPCID")); }
			if (!(entity instanceof EntityNPCInterface) && worldIn.isRemote) {
				Packets.sendServerDelayed(new SPacketResetItemMoving(), stack, 5000);
			}
			return entity instanceof EntityNPCInterface ? (EntityNPCInterface) entity : null;
		}
		return null;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<String> list, @Nonnull ITooltipFlag flagIn) {
        list.add(Component.translatable("info.item.moving.path").getFormattedText());
		for (int i = 0; i <= 6; i++) {
			if (i == 1 || i == 2) {
				list.add(Component.translatable("info.item.moving.path." + i,
						Component.translatable("ai.movingpath").getFormattedText()).getFormattedText());
				continue;
			}
			list.add(Component.translatable("info.item.moving.path." + i).getFormattedText());
		}
	}

	public @Nonnull ActionResult<ItemStack> onItemRightClick(@Nonnull World world, @Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand) {
		ItemStack stack = playerIn.getHeldItem(hand);
		if (!world.isRemote && playerIn instanceof EntityPlayerMP && hand == EnumHand.MAIN_HAND) {
			EntityPlayerMP player = (EntityPlayerMP) playerIn;
			EntityNPCInterface npc = getNpc(stack, world);
			if (npc != null && (player.isSneaking() || npc.ais.getMovingType() == 2)) {
				if ((CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
						!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_PATHER)) {
					player.sendMessage(Component.translatable("availability.permission").getParent());
					return new ActionResult<>(EnumActionResult.FAIL, stack);
				}
				NoppesUtilServer.sendOpenGui(player, EnumGuiType.MovingPath, npc);
			}
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}

	public @Nonnull EnumActionResult onItemUse(@Nonnull EntityPlayer playerIn, @Nonnull World world, @Nonnull BlockPos bpos,
											   @Nonnull EnumHand hand, @Nonnull EnumFacing side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) { ClientEventHandler.movingPath.clear(); }
		else if (playerIn instanceof EntityPlayerMP &&
				hand == EnumHand.MAIN_HAND) {
			EntityPlayerMP player = (EntityPlayerMP) playerIn;
			if ((CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
					!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_PATHER)) {
				player.sendMessage(Component.translatable("availability.permission").getParent());
				return EnumActionResult.FAIL;
			}
			ItemStack stack = player.getHeldItem(hand);
			EntityNPCInterface npc = getNpc(stack, world);
			if (npc != null) {
				List<int[]> list = npc.ais.getMovingPath();
				int[] pos = list.get(list.size() - 1);
				int x = bpos.getX();
				int y = bpos.getY();
				int z = bpos.getZ();
				if (npc.ais.getMovingType() != 2) {
					npc.ais.setStartPos(new BlockPos(x, y, z));
					player.sendMessage(Component.translatable("message.pather.home",
							((char) 167) + "6" + x, ((char) 167) + "6" + y, ((char) 167) + "6" + z,
							npc.getName()).getParent());
				}
				else {
					boolean added = true;
					if (!list.isEmpty()) {
						int[] p = list.get(list.size() - 1);
						added = !(p[0] == x && p[1] == y && p[2] == z);
					}
					if (added) {
						list.add(new int[] { x, y, z });
						player.sendMessage(Component.translatable("message.pather.added",
								((char) 167) + "6" + x, ((char) 167) + "6" + y, ((char) 167) + "6" + z,
								npc.getName()).getParent());
						double d0 = x - pos[0];
						double d1 = y - pos[1];
						double d2 = z - pos[2];
						double distance = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
						if (distance > CustomNpcs.NpcNavRange) {
							player.sendMessage(Component.translatable("message.pather.warn.added", ((char) 167) + "6" + CustomNpcs.NpcNavRange).getParent());
						}
						Packets.send(player, new PacketMenuSave(npc, EnumMenuType.AI));
					}
				}
			}
		}
		return EnumActionResult.SUCCESS;
	}

}
