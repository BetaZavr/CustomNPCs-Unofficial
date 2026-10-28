package moe.plushie.armourers_workshop.api.common;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unused")
public interface IBlockMenuProvider<C, T> {

    C createMenu(IMenuType<?> var1, Block var2, int var3, Inventory var4, T var5);

}

