package noppes.npcs.api.wrapper;

import net.minecraft.nbt.Tag;
import noppes.npcs.api.INbt;
import noppes.npcs.api.constants.AlignmentType;
import noppes.npcs.api.overlay.IOverlayComponent;

public abstract class OverlayComponentWrapper implements IOverlayComponent {

   protected int id;
   protected int x;
   protected int y;
   protected float scale = 1.0F;
   protected AlignmentType alignment = AlignmentType.NONE;

   public OverlayComponentWrapper(int idIn, int xIn, int yIn) {
      x = xIn;
      y = yIn;
      id = idIn;
   }

   @Override
   public int getId() { return id; }

   @Override
   public int getPosX() { return x; }

   @Override
   public int getPosY() { return y; }

   @Override
   public IOverlayComponent setPos(int xIn, int yIn) {
      x = xIn;
      y = yIn;
      return this;
   }

   @Override
   public void toNbt(INbt iNbt) {
      iNbt.setInteger("id", id);
      iNbt.setInteger("type", getType());
      iNbt.setInteger("alignment", getAlignment());
      iNbt.setIntegerArray("pos", new int[]{ x, y });
      iNbt.setFloat("scale", scale);
   }

   @Override
   public void fromNbt(INbt iNbt) {
      int[] pos = iNbt.getIntegerArray("pos");
      x = pos[0];
      y = pos[1];
      id = iNbt.getInteger("id");
      scale = 1.0f;
      alignment = AlignmentType.NONE;
      if (iNbt.has("scale", Tag.TAG_ANY_NUMERIC)) { setScale(iNbt.getFloat("scale")); }
      if (iNbt.has("alignment", Tag.TAG_ANY_NUMERIC)) { setAlignment(iNbt.getInteger("alignment")); }
   }

   @Override
   public int getAlignment() { return alignment.get(); }

   @Override
   public void setAlignment(int type) { alignment = AlignmentType.get(type); }

   @SuppressWarnings("unused")
   public void setAlignment(AlignmentType type) { alignment = type; }

   @Override
   public float getScale() { return scale; }

   @Override
   public void setScale(float scaleIn) { scale = Math.min(Math.max(scaleIn, 0.01F), 25.0F); }

}
