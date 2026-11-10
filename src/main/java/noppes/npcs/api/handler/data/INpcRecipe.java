package noppes.npcs.api.handler.data;

import net.minecraft.util.ResourceLocation;
import noppes.npcs.api.INbt;
import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.gui.WrapperRecipe;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public interface INpcRecipe {

	String getName();

	boolean isGlobal();

	void setIsGlobal(@ParamName("isGlobal") boolean isGlobal);

	boolean getIgnoreNBT();

	void setIgnoreNBT(@ParamName("ignoreNBT") boolean ignoreNBT);

	boolean getIgnoreDamage();

	void setIgnoreDamage(@ParamName("ignoreDamage") boolean ignoreDamage);

	int getWidth();

	int getHeight();

	IItemStack getResult();

	IItemStack[][] getRecipe();

	void save();

	void delete();

	boolean isShaped();

	void setIsShaped(@ParamName("isShaped") boolean isShaped);

	// New from Unofficial (BetaZavr)
	boolean isValid();

	boolean isKnown();

	void setIsKnown(@ParamName("isKnown") boolean isKnown);

	boolean showInRecipeBook();

	void setShowInRecipeBook(@ParamName("showInRecipeBook") boolean showInRecipeBook);

	IAvailability getAvailability();

	ResourceLocation getMCId();

	INbt getNbt();

	String getNpcGroup();

	void setNbt(@ParamName("nbt") INbt nbt);

	boolean isRecipeItemsEmpty();

	WrapperRecipe getWrapperRecipe();

    void setResult(@ParamName("item") IItemStack item);

	void setItems(@ParamName("items") IItemStack[][] items);

	void setItems(@ParamName("mapItems") Map<Integer, List<IItemStack>> mapItems);

}
