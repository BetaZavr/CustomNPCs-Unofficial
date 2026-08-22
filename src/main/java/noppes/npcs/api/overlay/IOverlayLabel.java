package noppes.npcs.api.overlay;

import noppes.npcs.api.interfaces.ParamName;

public interface IOverlayLabel extends IOverlayComponent {

   String getText();

   IOverlayLabel setText(@ParamName("label") String label);

   IOverlayLabel setCentered(@ParamName("bo") boolean bo);

   boolean isCentered();

   int getColor();

   void setColor(@ParamName("color") int color);

}
