package noppes.npcs.api.entity.data;

import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.entity.IPlayer;

@SuppressWarnings("all")
public interface INPCDisplay {

   String getName();

   void setName(@ParamName("name") String name);

   String getTitle();

   void setTitle(@ParamName("title") String title);

   String getSkinUrl();

   void setSkinUrl(@ParamName("url") String url);

   String getSkinPlayer();

   void setSkinPlayer(@ParamName("name") String name);

   String getSkinTexture();

   void setSkinTexture(@ParamName("texture") String texture);

   boolean getHasLivingAnimation();

   void setHasLivingAnimation(@ParamName("enabled") boolean enabled);

   int getVisible();

   void setVisible(@ParamName("type") int type);

   boolean isVisibleTo(@ParamName("player") IPlayer<?> playerIn);

   int getBossbar();

   void setBossbar(@ParamName("type") int type);

   float getSize();

   void setSize(@ParamName("size") float size);

   int getTint();

   void setTint(@ParamName("color") int color);

   int getShowName();

   void setShowName(@ParamName("type") int type);

   void setCapeTexture(@ParamName("texture") String texture);

   String getCapeTexture();

   void setOverlayTexture(@ParamName("texture") String texture);

   String getOverlayTexture();

   void setModelScale(@ParamName("part") int part, @ParamName("x") float x, @ParamName("y") float y, @ParamName("z") float z);

   float[] getModelScale(@ParamName("part") int part);

   int getBossColor();

   void setBossColor(@ParamName("color") int color);

   void setModel(@ParamName("id") String id);

   String getModel();

   void setHitboxState(@ParamName("state") byte state);

   byte getHitboxState();

   // New from Unofficial (GoodBird)
   boolean isOverlayGlowing();

   void setOverlayGlowing(@ParamName("glowing") boolean glowing);

   int[] getLineColors();

   void setLineColors(@ParamName("color1") int color1, @ParamName("color2") int color2, @ParamName("color3") int color3);

   // New from Unofficial (BetaZavr)
   float[] getDimensions();

   void setDimensions(@ParamName("width") float widthIn, @ParamName("height") float heightIn);

   int getShadowType();

   void setShadowType(@ParamName("type") int type);

}
