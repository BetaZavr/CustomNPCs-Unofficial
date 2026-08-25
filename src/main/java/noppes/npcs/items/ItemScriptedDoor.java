package noppes.npcs.items;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.server.SPacketGuiOpen;
import org.jetbrains.annotations.NotNull;

public class ItemScriptedDoor extends DoubleHighBlockItem implements INPCToolItem {

   public ItemScriptedDoor(Block block) {
      super(block, (new Properties()).stacksTo(1));
   }

   public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
      InteractionResult res = super.useOn(context);
      if (res == InteractionResult.SUCCESS && !context.getLevel().isClientSide && context.getPlayer() != null) {
         PlayerData.get(context.getPlayer()).scriptBlockPos = context.getClickedPos();
         SPacketGuiOpen.sendOpenGui((ServerPlayer) context.getPlayer(), EnumGuiType.ScriptDoor, null, context.getClickedPos().above());
         return InteractionResult.SUCCESS;
      } else {
         return res;
      }
   }

   public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level worldIn, @NotNull LivingEntity playerIn) {
      return stack;
   }
}
