package noppes.npcs.items;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.*;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.api.util.IRayTraceResults;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiOpen;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.util.Util;

public class ItemNpcScripter extends Item implements INPCToolItem {

	public ItemNpcScripter() {
		setRegistryName(CustomNpcs.MODID, "npcscripter");
		setUnlocalizedName("npcscripter");
		setFull3D();
		maxStackSize = 1;
		setCreativeTab(CustomTabs.TOOLS);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<String> list, @Nonnull ITooltipFlag flagIn) {
        list.add(Component.translatable("info.item.scripter").getFormattedText());
		list.add(Component.translatable("info.item.scripter.0").getFormattedText());
	}

	@Override
	public @Nonnull ActionResult<ItemStack> onItemRightClick(@Nonnull World world, @Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand) {
		ItemStack itemstack = playerIn.getHeldItem(hand);
		if (!world.isRemote && hand == EnumHand.MAIN_HAND && playerIn instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) playerIn;
			if (!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_SCRIPTER) ||
					(CustomNpcs.OpsOnly && !CommonUtil.isOp(player))) {
				player.sendMessage(Component.translatable("availability.permission").getParent());
				return new ActionResult<>(EnumActionResult.FAIL, itemstack);
			}
			IRayTraceResults rayTrace = Util.instance.rayTraceBlocksAndEntitys(player, player.rotationYaw, player.rotationPitch, PlayerData.get(player).game.blockReachDistance);
			if (rayTrace.getEntitys().length == 0 || !(rayTrace.getEntitys()[0].getMCEntity() instanceof EntityNPCInterface)) {
				Packets.send(player, new PacketGuiOpen(EnumGuiType.ScriptPlayers, BlockPos.ORIGIN));
			}
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
	}

}
