package noppes.npcs.api.wrapper;

import net.minecraft.nbt.Tag;
import noppes.npcs.api.INbt;
import noppes.npcs.api.overlay.IOverlayLabel;

public class OverlayLabelWrapper extends OverlayComponentWrapper implements IOverlayLabel {

   protected int color = 0xFFFFFF;
   protected String text;
   protected boolean isCenter = false;

   public OverlayLabelWrapper(int id, int x, int y, String textIn) {
      super(id, x, y);
      text = textIn;
   }

   @Override
   public String getText() { return text; }

   @Override
   public IOverlayLabel setText(String textIn) {
      text = textIn;
      return this;
   }

   @Override
   public int getColor() { return (color & 0x00FFFFFF) | 0xFF000000; }

   @Override
   public void setColor(int colorIn) { color = colorIn & 0x00FFFFFF; }

   @Override
   public IOverlayLabel setCentered(boolean centered) {
      isCenter = centered;
      return this;
   }

   @Override
   public boolean isCentered() { return isCenter; }

   @Override
   public int getType() { return 0; }

   @Override
   public void toNbt(INbt iNbt) {
      super.toNbt(iNbt);
      iNbt.setInteger("color", color);
      iNbt.setString("text", text);
      iNbt.setBoolean("centered", isCenter);
   }

   @Override
   public void fromNbt(INbt iNbt) {
      super.fromNbt(iNbt);
      color = 0xFFFFFF;
      if (iNbt.has("color", Tag.TAG_ANY_NUMERIC)) { color = iNbt.getInteger("color"); }
      text = iNbt.getString("text");
      isCenter = iNbt.getBoolean("centered");
   }

}
