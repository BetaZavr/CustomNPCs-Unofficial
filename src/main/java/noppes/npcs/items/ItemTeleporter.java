package noppes.npcs.items;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiOpen;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.shared.common.util.LogWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemTeleporter extends Item implements INPCToolItem {

   public ItemTeleporter() { super((new Properties()).stacksTo(1)); }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level levelIn, @Nonnull List<Component> list, @Nonnull TooltipFlag flagIn) {
      list.add(Component.translatable("info.item.teleporter"));
      list.add(Component.translatable("info.item.teleporter.0"));
   }

   @Override
   public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, Player playerIn, @Nonnull InteractionHand hand) {
      ItemStack stack = playerIn.getItemInHand(hand);
      if (!level.isClientSide() && playerIn instanceof ServerPlayer player && hand == InteractionHand.MAIN_HAND) {
         if ((CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
                 !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_TELEPORTER)) {
            player.sendSystemMessage(Component.translatable("availability.permission"));
            return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
         }
         Packets.send(player, new PacketGuiOpen(EnumGuiType.NpcDimensions, BlockPos.ZERO));
      }
      return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
   }

   @Override
   public boolean onEntitySwing(ItemStack stack, LivingEntity livingEntity) {
      if (livingEntity instanceof ServerPlayer player) {
         if ((CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
                 !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_TELEPORTER)) {
            player.sendSystemMessage(Component.translatable("availability.permission"));
            return false;
         }
         float f = player.getXRot();
         float f1 = player.getYRot();
         Vec3 vector3d = player.getEyePosition(1.0F);
         float f2 = Mth.cos(-f1 * 0.017453292F - 3.1415927F);
         float f3 = Mth.sin(-f1 * 0.017453292F - 3.1415927F);
         float f4 = -Mth.cos(-f * 0.017453292F);
         float f5 = Mth.sin(-f * 0.017453292F);
         float f6 = f3 * f4;
         float f7 = f2 * f4;
         double d0 = 80.0D;
         Vec3 vector3d1 = vector3d.add((double)f6 * d0, (double)f5 * d0, (double)f7 * d0);
         BlockHitResult movingobjectposition = player.level().clip(new ClipContext(vector3d, vector3d1, Block.COLLIDER, Fluid.ANY, player));
         Vec3 vec32 = player.getViewVector(f);
         boolean flag = false;
         float f9 = 1.0F;
         List<Entity> list = player.level().getEntities(player, player.getBoundingBox().inflate(vec32.x * d0, vec32.y * d0, vec32.z * d0).inflate(f9, f9, f9));
         for (Entity entity : list) {
            if (entity.canBeCollidedWith()) {
               float f10 = entity.getPickRadius();
               AABB axisAlignedBB = entity.getBoundingBox().inflate(f10, f10, f10);
               if (axisAlignedBB.contains(vector3d)) { flag = true; }
            }
         }
         if (!flag) {
            if (movingobjectposition.getType() == Type.BLOCK) {
               BlockPos pos = movingobjectposition.getBlockPos();
               while (player.level().getBlockState(pos).getBlock() != Blocks.AIR) { pos = pos.above(); }
               player.teleportTo((float) pos.getX() + 0.5F, (float) pos.getY() + 1.0F, (float) pos.getZ() + 0.5F);
            }
         }
      }
      return true;
   }

   protected void permission(ServerPlayer player) {
      LogWriter.warn(player.getName().getString() + ": attempted to use a mechanism that was prohibited to him. Permission: " + CustomNpcsPermissions.TOOL_TELEPORTER.getNodeName());
      player.sendSystemMessage(Component.translatable("availability.permission"));
   }

}
