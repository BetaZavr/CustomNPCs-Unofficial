package noppes.npcs.items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.common.CommonUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemSoulstoneEmpty extends Item {

   public ItemSoulstoneEmpty() { super((new Properties()).stacksTo(64)); }

   public void store(LivingEntity entity, ItemStack stack, ServerPlayer player) {
      if (hasPermission(entity, player) && !(entity instanceof Player)) {
         ItemStack stone = new ItemStack(CustomItems.soulstoneFull);
         CompoundTag compound = new CompoundTag();
         if (entity.saveAsPassenger(compound)) {
            ServerCloneController.Instance.cleanTags(compound);
            stone.addTagElement("Entity", compound);
            String name = entity.getEncodeId();
            if (name == null) { name = "generic"; }
            stone.addTagElement("Name", StringTag.valueOf(name));
            if (entity instanceof EntityNPCInterface npc) {
               stone.addTagElement("DisplayName", StringTag.valueOf(Component.Serializer.toJson(entity.getName())));
               if (npc.role.getEnumType() == RoleType.COMPANION) {
                  RoleCompanion role = (RoleCompanion) npc.role;
                  stone.addTagElement("ExtraText", StringTag.valueOf("companion.stage,: ," + role.stage.name));
               }
            }
            else if (entity.hasCustomName() && entity.getCustomName() != null) {
               stone.addTagElement("DisplayName", StringTag.valueOf(Component.Serializer.toJson(entity.getCustomName())));
            }
            NoppesUtilServer.givePlayerItem(player, player, stone);
            if (!player.isCreative()) {
               stack.split(1);
               if (stack.getCount() <= 0) { player.getInventory().removeItem(stack); }
            }
            entity.discard();
         }
      }
   }

   public boolean hasPermission(LivingEntity entity, ServerPlayer player) {
      if (player == null ||
              (CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
              !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.SOULSTONE_ALL)) {
         if (player != null) {
            player.sendSystemMessage(Component.translatable("availability.permission"));
         }
         return false;
      }

      if (entity instanceof EntityNPCInterface npc) {
         if (npc.role instanceof RoleCompanion role && role.getOwner() == player) { return true; }
         else if (npc.role instanceof RoleFollower role && role.getOwner() == player) { return !role.refuseSoulStone; }
         return CustomNpcs.SoulStoneNPCs;
      }
      return entity instanceof Animal && CustomNpcs.SoulStoneAnimals;
   }

   // New from Unofficial (BetaZavr)
   @OnlyIn(Dist.CLIENT)
   @Override
   public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> list, @Nonnull TooltipFlag flagIn) {
      list.add(Component.translatable("info.item.soulstone.0"));
   }

}
