package noppes.npcs.api.overlay;

import noppes.npcs.api.interfaces.ParamName;

@SuppressWarnings("unused")
public interface IOverlayTexturedRect extends IOverlayComponent {

   String getTexture();

   IOverlayTexturedRect setTexture(@ParamName("texture") String texture);

   int getWidth();

   IOverlayTexturedRect setWidth(@ParamName("width") int width);

   int getHeight();

   IOverlayTexturedRect setHeight(@ParamName("height") int height);

   int getColor();

   IOverlayTexturedRect setUV(@ParamName("u0") float u0, @ParamName("v0") float v0, @ParamName("u1") float u1, @ParamName("v1") float v1);

   IOverlayTexturedRect setColor(@ParamName("color") int color);

   IOverlayTexturedRect setColor(@ParamName("red") float red, @ParamName("green") float green, @ParamName("blue") float blue, @ParamName("alpha") float alpha);

   IOverlayTexturedRect setColor(@ParamName("red") int red, @ParamName("green") int green, @ParamName("blue") int blue, @ParamName("alpha") int alpha);

   float[] getRGB();

   int getTextureX();

   int getTextureY();

   int getTextureMaxX();

   int getTextureMaxY();

   IOverlayTexturedRect setTextureOffset(@ParamName("offsetX") int offsetX, @ParamName("offsetY") int offsetY);

   @SuppressWarnings("UnusedReturnValue")
   IOverlayTexturedRect setTextureMaxSize(@ParamName("textureMaxX") int textureMaxX, @ParamName("textureMaxY") int textureMaxY);

}
