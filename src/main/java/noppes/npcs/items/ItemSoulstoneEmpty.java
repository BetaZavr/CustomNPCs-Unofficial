package noppes.npcs.items;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.Component;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.*;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.common.CommonUtil;

import javax.annotation.Nonnull;

public class ItemSoulstoneEmpty extends Item {

	public ItemSoulstoneEmpty() {
		setRegistryName(CustomNpcs.MODID, "npcsoulstoneempty");
		setUnlocalizedName("npcsoulstoneempty");
		setCreativeTab(CustomTabs.TOOLS);
		setMaxStackSize(64);
	}

	public void store(EntityLivingBase entity, ItemStack stack, EntityPlayerMP player) {
		if (hasPermission(entity, player) && !(entity instanceof EntityPlayer)) {
			ItemStack stone = new ItemStack(CustomItems.soulstoneFull);
			NBTTagCompound compound = new NBTTagCompound();
			if (entity.writeToNBTAtomically(compound)) {
				if (compound.getString("id").equals("minecraft:customnpcs.customnpc")
						|| compound.getString("id").equals("minecraft:customnpcs:customnpc")) {
					compound.setString("id", CustomNpcs.MODID + ":customnpc");
				}
				ServerCloneController.Instance.cleanTags(compound);
				if (CustomNpcs.SoulStoneFilledStackSize > 1) {
					for (String key : new String[] { "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround", "PortalCooldown", "HurtTime", "HurtByTimestamp", "DeathTime" }) { compound.removeTag(key); }
					if (entity instanceof EntityNPCInterface) { compound.removeTag("Health"); }
					NBTTagCompound forgeData = compound.getCompoundTag("ForgeData");
					if (forgeData.getCompoundTag("cnpcmarkdata").getTagList("marks", 10).hasNoTags()) { forgeData.removeTag("cnpcmarkdata"); }
					if (forgeData.hasNoTags()) { compound.removeTag("ForgeData"); }
				}
				stone.setTagInfo("Entity", compound);
				String name = EntityList.getEntityString(entity);
				if (name == null) { name = "generic"; }
				stone.setTagInfo("Name", new NBTTagString("entity." + name + ".name"));
				if (entity instanceof EntityNPCInterface) {
					EntityNPCInterface npc = (EntityNPCInterface) entity;
					stone.setTagInfo("DisplayName", new NBTTagString(entity.getName()));
					if (npc.role instanceof RoleCompanion) {
						stone.setTagInfo("ExtraText", new NBTTagString(
								"companion.stage,: ," + ((RoleCompanion) npc.role).stage.name));
					}
				}
				else if (entity instanceof EntityLiving && (entity).hasCustomName()) {
					stone.setTagInfo("DisplayName", new NBTTagString((entity).getCustomNameTag()));
				}
				NoppesUtilServer.givePlayerItem(player, player, stone);
				if (!player.isCreative()) {
					stack.splitStack(1);
					if (stack.getCount() <= 0) { player.inventory.deleteStack(stack); }
				}
				entity.isDead = true;
			}
		}
	}

	public boolean hasPermission(EntityLivingBase entity, EntityPlayerMP player) {
		if ((CustomNpcs.OpsOnly && CommonUtil.isOp(player)) ||
				CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.SOULSTONE_ALL)) { return true; }
		if (entity instanceof EntityNPCInterface) {
			EntityNPCInterface npc = (EntityNPCInterface) entity;
			if (npc.role instanceof RoleCompanion && ((RoleCompanion) npc.role).getOwner() == player) { return true; }
			if (npc.role instanceof RoleFollower && ((RoleFollower) npc.role).getOwner() == player) { return !((RoleFollower) npc.role).refuseSoulStone; }
			return CustomNpcs.SoulStoneNPCs;
		}
		return entity instanceof EntityAnimal && CustomNpcs.SoulStoneAnimals;
	}

	// New from Unofficial (BetaZavr)
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull ItemStack stack, World world, @Nonnull List<String> list, @Nonnull ITooltipFlag flag) {
		list.add(Component.translatable("info.item.soulstone.0").getFormattedText());
	}

}
