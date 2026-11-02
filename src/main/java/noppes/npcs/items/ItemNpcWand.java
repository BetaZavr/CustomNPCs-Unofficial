package noppes.npcs.items;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.*;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

public class ItemNpcWand extends Item implements INPCToolItem {

	public ItemNpcWand() {
		super();
		setRegistryName(CustomNpcs.MODID, "npcwand");
		setUnlocalizedName("npcwand");
		setFull3D();
		maxStackSize = 1;
		setCreativeTab(CustomTabs.TOOLS);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<String> list, @Nonnull ITooltipFlag flagIn) {
        list.add(new TextComponentTranslation("info.item.wand").getFormattedText());
		for (int i = 0; i < 3; i++) {
			list.add(new TextComponentTranslation("info.item.wand." + i).getFormattedText());
		}
	}

	public int getMaxItemUseDuration(@Nonnull ItemStack par1ItemStack) { return 72000; }

	public @Nonnull ActionResult<ItemStack> onItemRightClick(@Nonnull World world, @Nonnull EntityPlayer player, @Nonnull EnumHand hand) {
		ItemStack itemstack = player.getHeldItem(hand);
		if (world.isRemote) { CustomNpcs.proxy.openGui(player, EnumGuiType.NpcRemote); }
		return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
	}

	public @Nonnull EnumActionResult onItemUse(@Nonnull EntityPlayer playerIn, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumHand hand,
											   @Nonnull EnumFacing side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote && playerIn instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) playerIn;
			if (CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) {
				player.sendMessage(Component.translatable("availability.permission").getParent());
				return EnumActionResult.FAIL;
			}
			Entity rayTraceEntity = Util.instance.getLookEntity(player, 4.0d, false);
			if (rayTraceEntity instanceof EntityNPCInterface) {
				if (!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_GUI)) {
					player.sendMessage(Component.translatable("availability.permission").getParent());
					return EnumActionResult.FAIL;
				}
				NoppesUtilServer.sendOpenGui(player, EnumGuiType.MainMenuDisplay, (EntityNPCInterface) rayTraceEntity);
				return EnumActionResult.SUCCESS;
			}
			if (!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_CREATE)) {
				player.sendMessage(Component.translatable("availability.permission").getParent());
				return EnumActionResult.FAIL;
			}
			// create new
			EntityCustomNpc npc = new EntityCustomNpc(world);
			npc.ais.setStartPos(pos.up());
			npc.setLocationAndAngles((pos.getX() + 0.5f), npc.getStartYPos(), (pos.getZ() + 0.5f), player.rotationYaw, player.rotationPitch);
			world.spawnEntity(npc);
			npc.setHealth(npc.getMaxHealth());
			CustomNPCsScheduler.runTack(() -> NoppesUtilServer.sendOpenGui(player, EnumGuiType.MainMenuDisplay, npc), 100);
		}
		return EnumActionResult.SUCCESS;
	}

}
