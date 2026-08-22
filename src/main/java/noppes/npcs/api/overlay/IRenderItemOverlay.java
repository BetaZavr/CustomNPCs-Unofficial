package noppes.npcs.api.overlay;

import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.item.IItemStack;

public interface IRenderItemOverlay extends IOverlayComponent {

    IItemStack getItem();

    IRenderItemOverlay setItem(@ParamName("stack") IItemStack stack);

}
