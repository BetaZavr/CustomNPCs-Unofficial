package noppes.npcs.items;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.client.ClientEventHandler;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketMenuSave;
import noppes.npcs.packets.server.SPacketResetItemMoving;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemNpcMovingPath extends Item implements INPCToolItem {

   public ItemNpcMovingPath() {
      super((new Properties()).stacksTo(1));
   }

   public static void register(@Nonnull EntityNPCInterface npc, @Nonnull ItemStack stack, @Nonnull Player player) {
      CompoundTag compound = stack.getOrCreateTag();
      if (compound.getInt("NPCID") != npc.getId() ||
              !compound.getUUID("NPCUUID").equals(npc.getUUID()) ||
              !compound.getString("NPCDIM").equals(npc.level().dimension().location().toString())) {
         compound.putInt("NPCID", npc.getId());
         compound.putUUID("NPCUUID", npc.getUUID());
         compound.putString("NPCDIM", npc.level().dimension().location().toString());
         player.sendSystemMessage(Component.translatable("message.pather.register", npc.getName(), stack.getDisplayName()));
         if (player instanceof  ServerPlayer sPlayer) {
            Packets.send(sPlayer, new PacketMenuSave(npc, EnumMenuType.MOVING_PATH));
         }
      }
   }

   public static @Nullable EntityNPCInterface getNpc(@Nonnull ItemStack stack, @Nonnull Level levelIn) {
      CompoundTag compound = stack.getTag();
      if (compound != null && compound.contains("NPCID", 3)) {
         Level level = levelIn;
         Entity entity = null;
         if (compound.contains("NPCDIM", 8)) {
            ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(compound.getString("NPCDIM")));
            if (!level.dimension().equals(levelKey)) {
               MinecraftServer server = level.getServer() != null ? level.getServer() : CustomNpcs.Server;
               if (server != null) { level = server.getLevel(levelKey); }
               else { level = null; }
            }
            if (level != null) {
               entity = level.getEntity(compound.getInt("NPCID"));
               if (!(entity instanceof EntityNPCInterface)) {
                  entity = Util.instance.getEntityByUUID(compound.getUUID("NPCUUID"), level, true);
               }
            }
         }
         else { entity = level.getEntity(compound.getInt("NPCID")); }
         if (!(entity instanceof EntityNPCInterface) && levelIn.isClientSide()) {
            Packets.sendServerDelayed(new SPacketResetItemMoving(), stack, 5000);
         }
         return entity instanceof EntityNPCInterface npc ? npc : null;
      }
      return null;
   }

   public @Nonnull InteractionResultHolder<ItemStack> use(Level level, Player playerIn, @Nonnull InteractionHand hand) {
      ItemStack stack = playerIn.getItemInHand(hand);
      if (!level.isClientSide() && playerIn instanceof ServerPlayer player && hand == InteractionHand.MAIN_HAND) {
         EntityNPCInterface npc = getNpc(stack, level);
         if (npc != null && (player.isCrouching() || npc.ais.getMovingType() == 2)) {
            if ((CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
                    !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_PATHER)) {
               player.sendSystemMessage(Component.translatable("availability.permission"));
               return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
            }
            NoppesUtilServer.sendOpenGui(player, EnumGuiType.MovingPath, npc);
         }
      }
      return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
   }

   public @Nonnull InteractionResult useOn(UseOnContext context) {
      if (context.getLevel().isClientSide()) { ClientEventHandler.movingPath.clear(); }
      else if (context.getPlayer() instanceof ServerPlayer player &&
              context.getHand() == InteractionHand.MAIN_HAND) {
         if ((CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) ||
                 !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_PATHER)) {
            player.sendSystemMessage(Component.translatable("availability.permission"));
            return InteractionResult.FAIL;
         }
         ItemStack stack = context.getItemInHand();
         EntityNPCInterface npc = getNpc(stack, context.getLevel());
         if (npc != null) {
            List<int[]> list = npc.ais.getMovingPath();
            int[] pos = list.get(list.size() - 1);
            int x = context.getClickedPos().getX();
            int y = context.getClickedPos().getY();
            int z = context.getClickedPos().getZ();
            if (npc.ais.getMovingType() != 2) {
               npc.ais.setStartPos(new BlockPos(x, y, z));
               player.sendSystemMessage(Component.translatable("message.pather.home",
                       ((char) 167) + "6" + x, ((char) 167) + "6" + y, ((char) 167) + "6" + z,
                       npc.getName()));
            }
            else {
               boolean added = true;
               if (!list.isEmpty()) {
                  int[] p = list.get(list.size() - 1);
                  added = !(p[0] == x && p[1] == y && p[2] == z);
               }
               if (added) {
                  list.add(new int[] { x, y, z });
                  player.sendSystemMessage(Component.translatable("message.pather.added",
                          ((char) 167) + "6" + x, ((char) 167) + "6" + y, ((char) 167) + "6" + z,
                          npc.getName()));
                  double d0 = x - pos[0];
                  double d1 = y - pos[1];
                  double d2 = z - pos[2];
                  double distance = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                  if (distance > CustomNpcs.NpcNavRange) {
                     player.sendSystemMessage(Component.translatable("message.pather.warn.added", ((char) 167) + "6" + CustomNpcs.NpcNavRange));
                  }
                  Packets.send(player, new PacketMenuSave(npc, EnumMenuType.AI));
               }
            }
         }
      }
      return InteractionResult.SUCCESS;
   }

   // New from Unofficial (BetaZavr)
   @OnlyIn(Dist.CLIENT)
   @Override
   public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> list, @Nonnull TooltipFlag flagIn) {
      list.add(Component.translatable("info.item.moving.path"));
      for (int i = 0; i <= 6; i++) {
         if (i == 1 || i == 2) {
            list.add(Component.translatable("info.item.moving.path." + i, Component.translatable("ai.movingpath").getString()));
            continue;
         }
         list.add(Component.translatable("info.item.moving.path." + i));
      }
   }

}
