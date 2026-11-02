package noppes.npcs.items;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.CustomTabs;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiOpen;
import noppes.npcs.shared.common.CommonUtil;

public class ItemTeleporter extends Item implements INPCToolItem {

	public ItemTeleporter() {
		setRegistryName(CustomNpcs.MODID, "npcteleporter");
		setUnlocalizedName("npcteleporter");
		setFull3D();
		maxStackSize = 1;
		setCreativeTab(CustomTabs.TOOLS);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<String> list, @Nonnull ITooltipFlag flagIn) {
        list.add(new TextComponentTranslation("info.item.teleporter").getFormattedText());
		list.add(new TextComponentTranslation("info.item.teleporter.0").getFormattedText());
	}

	@Override
	public @Nonnull ActionResult<ItemStack> onItemRightClick(@Nonnull World world, @Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand) {
		ItemStack stack = playerIn.getHeldItem(hand);
		if (!world.isRemote && playerIn instanceof EntityPlayerMP && hand == EnumHand.MAIN_HAND) {
			EntityPlayerMP player = (EntityPlayerMP) playerIn;
			if ((CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
					!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_TELEPORTER)) {
				player.sendMessage(Component.translatable("availability.permission").getParent());
				return new ActionResult<>(EnumActionResult.FAIL, stack);
			}
			Packets.send(player, new PacketGuiOpen(EnumGuiType.NpcDimensions, BlockPos.ORIGIN));
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}

	@Override
	public boolean onEntitySwing(@Nonnull EntityLivingBase entityLivingBase, @Nonnull ItemStack stack) {
		if (entityLivingBase instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) entityLivingBase;
			if ((CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
					!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_TELEPORTER)) {
				player.sendMessage(Component.translatable("availability.permission").getParent());
				return false;
			}
			float f = 1.0f;
			float f2 = player.prevRotationPitch
					+ (player.rotationPitch - player.prevRotationPitch) * f;
			float f3 = player.prevRotationYaw
					+ (player.rotationYaw - player.prevRotationYaw) * f;
			double d0 = player.prevPosX + (player.posX - player.prevPosX) * f;
			double d2 = player.prevPosY + (player.posY - player.prevPosY) * f + 1.62;
			double d3 = player.prevPosZ + (player.posZ - player.prevPosZ) * f;
			Vec3d vec3 = new Vec3d(d0, d2, d3);
			float f4 = MathHelper.cos(-f3 * 0.017453292f - 3.1415927f);
			float f5 = MathHelper.sin(-f3 * 0.017453292f - 3.1415927f);
			float f6 = -MathHelper.cos(-f2 * 0.017453292f);
			float f7 = MathHelper.sin(-f2 * 0.017453292f);
			float f8 = f5 * f6;
			float f9 = f4 * f6;
			double d4 = 80.0;
			Vec3d vec4 = vec3.addVector(f8 * d4, f7 * d4, f9 * d4);
			RayTraceResult movingobjectposition = player.world.rayTraceBlocks(vec3, vec4, true);
			if (movingobjectposition == null) { return false; }
			Vec3d vec5 = player.getLook(f);
			boolean flag = false;
			float f10 = 1.0f;
			List<Entity> list = new ArrayList<>();
			try {
				list = player.world.getEntitiesWithinAABBExcludingEntity(player,
						player.getEntityBoundingBox()
								.grow(vec5.x * d4, vec5.y * d4, vec5.z * d4)
								.grow(f10, f10, f10));
			}  catch (Exception ignored) { }
			for (Entity entity : list) {
				if (entity.canBeCollidedWith()) {
					float f11 = entity.getCollisionBorderSize();
					AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().grow(f11, f11, f11);
					if (axisalignedbb.contains(vec3)) {
						flag = true;
					}
				}
			}
			if (flag) { return false; }
			if (movingobjectposition.typeOfHit == RayTraceResult.Type.BLOCK) {
				BlockPos pos = movingobjectposition.getBlockPos();
				while (player.world.getBlockState(pos).getBlock() != Blocks.AIR && pos.getY() < 256) { pos = pos.up(); }
				player.setPositionAndUpdate((pos.getX() + 0.5f), (pos.getY() + 1.0f), (pos.getZ() + 0.5f));
			}
		}
		return true;
	}

}
