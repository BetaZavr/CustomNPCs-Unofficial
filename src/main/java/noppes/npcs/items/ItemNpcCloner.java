package noppes.npcs.items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.GuiNpcMobSpawnerAdd;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCloneSet;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.packets.server.SPacketToolMobSpawner;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemNpcCloner extends Item implements INPCToolItem {

   public ItemNpcCloner() {
      super((new Properties()).stacksTo(1));
   }

   @Override
   public @Nonnull InteractionResult useOn(UseOnContext context) {
      if (context.getLevel().isClientSide() && context.getPlayer() != null) {
         Player player = context.getPlayer();
         PlayerData data = PlayerData.get(player);
         boolean summon = false;
         ItemStack stackCloner = player.getMainHandItem();
         if (data.overlay.isPressedShift()) {
            CompoundTag nbt = stackCloner.getTag();
            if (nbt != null && nbt.contains("Settings", 10)) {
               CompoundTag nbtData = nbt.getCompound("Settings");
               if (nbtData.getBoolean("isServerClone")) {
                  Packets.sendServer(new SPacketToolMobSpawner(true, false, context.getClickedPos().above(),
                          nbtData.getString("Name"), nbtData.getInt("Tab"), new CompoundTag()));
               } else {
                  Packets.sendServer(new SPacketToolMobSpawner(false, false,
                       context.getClickedPos().above(), "", -1, nbtData.getCompound("EntityNBT")));
               }
               summon = true;
            }
         }
         if (!summon) {
            Entity rayTraceEntity = Util.instance.getLookEntity(player, 4.0d, false);
            if (rayTraceEntity instanceof EntityNPCInterface) {
               CompoundTag compound = new CompoundTag();
               if (!rayTraceEntity.save(compound)) { return InteractionResult.FAIL; }
               ServerCloneController.Instance.cleanTags(compound);
               try {
                  Packets.sendServer(new SPacketCloneSet(compound));
                  NoppesUtil.openGUI(player, new GuiNpcMobSpawnerAdd(compound));
               } catch (Exception e) { LogWriter.error("Error send data:", e); }
               return InteractionResult.FAIL;
            }
            Packets.sendServer(new SPacketGuiOpen(EnumGuiType.MobSpawner, context.getClickedPos().above()));
         }
      }
      return InteractionResult.SUCCESS;
   }

   // New from Unofficial (BetaZavr)
   @OnlyIn(Dist.CLIENT)
   @Override
   public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> list, @Nonnull TooltipFlag flagIn) {
      list.add(Component.translatable("info.item.cloner"));
      CompoundTag nbt = stack.getTag();
      if (nbt == null || !nbt.contains("Settings", 10)) {
         list.add(Component.translatable("info.item.cloner.empty.0"));
         list.add(Component.translatable("info.item.cloner.empty.1"));
      } else {
         list.add(Component.translatable("info.item.cloner.set.0", nbt.getCompound("Settings").getString("Name")));
         list.add(Component.translatable("info.item.cloner.set.1"));
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public boolean isFoil(@Nonnull ItemStack stack) {
      CompoundTag nbt = stack.getTag();
      return super.isFoil(stack) || (nbt != null && nbt.contains("Settings", 10) &&
              !nbt.getCompound("Settings").getString("Name").isEmpty());
   }

}
