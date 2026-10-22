package noppes.npcs.items;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.Component;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleFollower;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemSoulstoneFilled extends Item {

	public ItemSoulstoneFilled() {
		setRegistryName(CustomNpcs.MODID, "npcsoulstonefilled");
		setUnlocalizedName("npcsoulstonefilled");
		setMaxStackSize(1);
	}

	@Override
	public int getItemStackLimit(@Nonnull ItemStack stack) { return Math.max(1, Math.min(64, CustomNpcs.SoulStoneFilledStackSize)); }

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull ItemStack stack, World world, @Nonnull List<String> list, @Nonnull ITooltipFlag flag) {
		NBTTagCompound compound = stack.getTagCompound();
		if (compound != null && compound.hasKey("Entity", 10)) {
			Component name = Component.translatable(compound.getString("Name"));
			if (compound.hasKey("DisplayName", 8)) {
				Component displayName = Component.empty()
						.append(Component.translatable(compound.getString("DisplayName")).withStyle(TextFormatting.BLUE));
				if (flag == ITooltipFlag.TooltipFlags.ADVANCED) {
					name = displayName.append(Component.literal(" (").withStyle(TextFormatting.DARK_GRAY))
							.append(name.withStyle(TextFormatting.DARK_GRAY))
							.append(Component.literal(")").withStyle(TextFormatting.DARK_GRAY));
				}
			}
			list.add(TextFormatting.BLUE + name.getFormattedText());
			if (stack.getTagCompound().hasKey("ExtraText")) {
				Component text = Component.literal("");
				String[] split = compound.getString("ExtraText").split(",");
				for (String s : split) { text.append(Component.translatable(s)); }
				list.add(text.getFormattedText());
			}
		}
		else { list.add(TextFormatting.RED + "Error"); }
	}

	@Override
	public @Nonnull EnumActionResult onItemUse(@Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumHand hand, @Nonnull EnumFacing side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) { return EnumActionResult.SUCCESS; }
		ItemStack stack = player.getHeldItem(hand);
		if (Spawn(player, stack, world, pos) == null) { return EnumActionResult.FAIL; }
		if (!player.isCreative()) { stack.splitStack(1); }
		return EnumActionResult.SUCCESS;
	}

	public static @Nullable Entity Spawn(EntityPlayer player, ItemStack stack, World world, BlockPos pos) {
		if (!world.isRemote && stack.getTagCompound() != null && stack.getTagCompound().hasKey("Entity", 10)) {
			NBTTagCompound compound = stack.getTagCompound().getCompoundTag("Entity");
			if (compound.getString("id").equals("minecraft:customnpcs.customnpc")
					|| compound.getString("id").equals("minecraft:customnpcs:customnpc")) {
				compound.setString("id", CustomNpcs.MODID + ":customnpc");
			}
			Entity entity = EntityList.createEntityFromNBT(compound, world);
			if (entity != null) {
				entity.setPosition(pos.getX() + 0.5, (pos.getY() + 1 + 0.2f), pos.getZ() + 0.5);
				if (entity instanceof EntityNPCInterface) {
					EntityNPCInterface npc = (EntityNPCInterface) entity;
					npc.ais.setStartPos(pos);
					npc.setHealth(npc.getMaxHealth());
					npc.setPosition((pos.getX() + 0.5f), npc.getStartYPos(), (pos.getZ() + 0.5f));
					if (npc.role instanceof RoleCompanion && player != null) {
						PlayerData data = PlayerData.get(player);
						if (data.hasCompanion()) { return null; }
						((RoleCompanion) npc.role).setOwner(player);
						data.setCompanion(npc);
					}
					if (npc.role instanceof RoleFollower && player != null) { ((RoleFollower) npc.role).setOwner(player); }
				}
				if (!world.spawnEntity(entity)) {
					if (player != null) { player.sendMessage(new TextComponentTranslation("error.failedToSpawn")); }
					return null;
				}
				return entity;
			}
		}
		return null;
	}

}
