package noppes.npcs.api.handler.data;

import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.item.IItemStack;

@SuppressWarnings("unused")
public interface IForbiddenItem {

    IItemStack getItem();

    void setItem(@ParamName("item") IItemStack item);

    boolean isIgnoreDamage();

    void setIsIgnoreDamage(@ParamName("ignore") boolean ignore);

    boolean isIgnoreNBT();

    void setIsIgnoreNBT(@ParamName("ignore") boolean ignore);

}
