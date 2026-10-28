package noppes.npcs.api.entity.data;

import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.item.IItemStack;

@SuppressWarnings("unused")
public interface IDropNbtSet {

    double getChance();

    void createAndPut(@ParamName("item") IItemStack item);

    String getPath();

    int getType();

    int getTypeList();

    String[] getValues();

    void remove();

    void setChance(@ParamName("chance") double chance);

    void setPath(@ParamName("path") String path);

    void setType(@ParamName("type") int type);

    void setTypeList(@ParamName("type") int type);

    void setValues(@ParamName("values") String values);

    void setValues(@ParamName("values") String[] values);

}
