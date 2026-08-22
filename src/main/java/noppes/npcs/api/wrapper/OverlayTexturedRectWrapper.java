package noppes.npcs.api.wrapper;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.FastColor;
import noppes.npcs.api.INbt;
import noppes.npcs.api.overlay.IOverlayTexturedRect;
import noppes.npcs.util.ValueUtil;

public class OverlayTexturedRectWrapper extends OverlayComponentWrapper implements IOverlayTexturedRect {

   protected String texture;
   protected int width;
   protected int height;
   protected int color = 0xFFFFFFFF;
   protected int textureX = 0;
   protected int textureY = 0;
   protected int textureMaxX = 256;
   protected int textureMaxY = 256;

   public OverlayTexturedRectWrapper(int id, int x, int y, String textureIn, int widthIn, int heightIn) {
      super(id, x, y);
      texture = textureIn;
      width = widthIn;
      height = heightIn;
   }

   public OverlayTexturedRectWrapper(int id, int x, int y, String textureIn, int widthIn, int heightIn, int textureX, int textureY) {
      super(id, x, y);
      texture = textureIn;
      width = widthIn;
      height = heightIn;
      setTextureOffset(textureX, textureY);
   }

   public OverlayTexturedRectWrapper(int id, int x, int y, String textureIn, int widthIn, int heightIn, int textureX, int textureY, int textureMaxX, int textureMaxY) {
      super(id, x, y);
      texture = textureIn;
      width = widthIn;
      height = heightIn;
      setTextureOffset(textureX, textureY);
      setTextureMaxSize(textureMaxX, textureMaxY);
   }

   @Override
   public int getTextureX() { return textureX; }

   @Override
   public int getTextureY() { return textureY; }

   @Override
   public int getTextureMaxX() { return textureMaxX; }

   @Override
   public int getTextureMaxY() { return textureMaxY; }

   @Override
   public IOverlayTexturedRect setTextureOffset(int offsetX, int offsetY) {
      textureX = ValueUtil.onlyPositiveInt(offsetX, 256);
      textureY = ValueUtil.onlyPositiveInt(offsetY, 256);
      return this;
   }

   @Override
   public IOverlayTexturedRect setTextureMaxSize(int textureMaxXIn, int textureMaxYIn) {
      textureMaxX = ValueUtil.onlyPositiveInt(textureMaxXIn, 256);
      textureMaxY = ValueUtil.onlyPositiveInt(textureMaxYIn, 256);
      return this;
   }

   @Override
   public String getTexture() { return texture; }

   @Override
   public IOverlayTexturedRect setTexture(String textureIn) {
      texture = textureIn;
      return this;
   }

   @Override
   public int getWidth() { return width; }

   @Override
   public IOverlayTexturedRect setWidth(int widthIn) {
      width = widthIn;
      return this;
   }

   @Override
   public int getHeight() { return height; }

   @Override
   public IOverlayTexturedRect setHeight(int heightIn) {
      height = heightIn;
      return this;
   }

   @Override
   public int getType() { return 1; }

   @Override
   public IOverlayTexturedRect setUV(float u0, float v0, float u1, float v1) {
      textureX = (int) ((u0 % 1.0F) * 256.0F);
      textureY = (int) ((v0 % 1.0F) * 256.0F);
      textureMaxX = (int) ((u1 % 1.0F) * 256.0F);
      textureMaxY = (int) ((v1 % 1.0F) * 256.0F);
      return this;
   }

   @Deprecated
   public IOverlayTexturedRect setRGB(float red, float green, float blue, float alpha) {
      setColor(red, green, blue, alpha);
      return this;
   }

   @Override
   public IOverlayTexturedRect setColor(float red, float green, float blue, float alpha) {
      setColor((int) (ValueUtil.correctFloat(alpha, 0.0f, 1.0f) * 255.0f) << 24,
              (int) (ValueUtil.correctFloat(red, 0.0f, 1.0f) * 255.0f) << 16,
              (int) (ValueUtil.correctFloat(green, 0.0f, 1.0f) * 255.0f) << 8,
              (int) (ValueUtil.correctFloat(blue, 0.0f, 1.0f) * 255.0f));
      return this;
   }

   @Override
   public IOverlayTexturedRect setColor(int red, int green, int blue, int alpha) {
      color = ValueUtil.correctInt(alpha, 0, 255) |
              ValueUtil.correctInt(red, 0, 255) |
              ValueUtil.correctInt(green, 0, 255) |
              ValueUtil.correctInt(blue, 0, 255);
      return this;
   }

   @Override
   public IOverlayTexturedRect setColor(int colorIn) {
      color = colorIn;
      return this;
   }

   @Override
   public float[] getRGB() { return new float[] { FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color) }; }

   @Override
   public int getColor() { return color; }

   @Override
   public void toNbt(INbt iNbt) {
      super.toNbt(iNbt);
      iNbt.setString("texture", texture);
      iNbt.setInteger("width", width);
      iNbt.setInteger("height", height);
      iNbt.setInteger("c", color);
      iNbt.setIntegerArray("texPosMax", new int[] { textureMaxX, textureMaxY });
      iNbt.setIntegerArray("texPos", new int[] { textureX, textureY });
   }

   @Override
   public void fromNbt(INbt iNbt) {
      super.fromNbt(iNbt);
      texture = iNbt.getString("texture");
      width = iNbt.getInteger("width");
      height = iNbt.getInteger("height");
      color = 0xFFFFFFFF;
      float[] uv = new float[] { 0.0F, 0.0F, 1.0F, 1.0F };
      if (iNbt.has("u", Tag.TAG_LIST) && iNbt.mcGetTag("u") instanceof ListTag list && list.getElementType() == Tag.TAG_FLOAT) {
         for (int i = 0; i < 4; i++) { uv[i] = i < list.size() ? list.getFloat(i) : (i < 2 ? 0.0F : 1.0f); }
      } // OLD uv
      else if (iNbt.has("u", Tag.TAG_ANY_NUMERIC)) { setColor(iNbt.getInteger("u")); } // OLD color
      setUV(uv[0], uv[1], uv[2], uv[3]);
      // normal
      if (iNbt.has("c", Tag.TAG_ANY_NUMERIC)) { setColor(iNbt.getInteger("c")); }
      if (iNbt.has("texPos", Tag.TAG_INT_ARRAY)) { setTextureOffset(iNbt.getIntegerArray("texPos")[0], iNbt.getIntegerArray("texPos")[1]); }
      if (iNbt.has("texPosMax", Tag.TAG_INT_ARRAY)) { setTextureMaxSize(iNbt.getIntegerArray("texPosMax")[0], iNbt.getIntegerArray("texPosMax")[1]); }
   }

}
