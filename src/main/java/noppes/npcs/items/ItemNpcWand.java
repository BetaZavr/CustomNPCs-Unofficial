package noppes.npcs.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomEntities;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemNpcWand extends Item implements INPCToolItem {

   public ItemNpcWand() { super((new Properties()).stacksTo(1)); }

   @Override
   public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      if (level.isClientSide()) { CustomNpcs.proxy.openGui(player, EnumGuiType.NpcRemote); }
      return new InteractionResultHolder<>(InteractionResult.SUCCESS, itemstack);
   }

   @Override
   public int getUseDuration(@NotNull ItemStack itemStack) { return 72000; }

   @Override
   public @NotNull InteractionResult useOn(UseOnContext context) {
      if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
         if (CustomNpcs.OpsOnly && !CommonUtil.isOp(player)) {
            player.sendSystemMessage(Component.translatable("availability.permission"));
            return InteractionResult.FAIL;
         }
         Entity rayTraceEntity = Util.instance.getLookEntity(player, 5.0d, false);
         if (rayTraceEntity instanceof EntityNPCInterface npc) {
            if (!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_GUI)) {
               player.sendSystemMessage(Component.translatable("availability.permission"));
               return InteractionResult.FAIL;
            }
            NoppesUtilServer.sendOpenGui(player, EnumGuiType.MainMenuDisplay, npc);
            return InteractionResult.SUCCESS;
         }
         if (!CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_CREATE)) {
            player.sendSystemMessage(Component.translatable("availability.permission"));
            return InteractionResult.FAIL;
         }
         // create new
         EntityCustomNpc npc = new EntityCustomNpc(CustomEntities.entityCustomNpc, context.getLevel());
         npc.ais.setStartPos(context.getClickedPos().above());
         npc.moveTo(context.getClickedPos().getX() + 0.5D, npc.getStartYPos(), context.getClickedPos().getZ() + 0.5D, player.getYRot(), player.getXRot());
         context.getLevel().addFreshEntity(npc);
         npc.setHealth(npc.getMaxHealth());
         CustomNPCsScheduler.runTack(() -> NoppesUtilServer.sendOpenGui(player, EnumGuiType.MainMenuDisplay, npc), 100);
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level worldIn, @NotNull LivingEntity playerIn) { return stack; }

   // New from Unofficial (BetaZavr)
   @OnlyIn(Dist.CLIENT)
   @Override
   public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> list, @Nonnull TooltipFlag flagIn) {
      list.add(Component.translatable("info.item.wand"));
      for (int i = 0; i < 3; i++) { list.add(Component.translatable("info.item.wand." + i)); }
   }

}
