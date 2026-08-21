package noppes.npcs.api.wrapper;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IProjectile;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.entity.EntityProjectile;

import java.util.Objects;

public class ProjectileWrapper<T extends EntityProjectile> extends ThrowableWrapper<T> implements IProjectile<T> {

   public ProjectileWrapper(T entity) {
      super(entity);
   }

   public IItemStack getItem() {
      return Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(entity.getItemDisplay());
   }

   public void setItem(IItemStack item) {
      if (item == null) { entity.setThrownItem(ItemStack.EMPTY); }
      else { entity.setThrownItem(item.getMCItemStack()); }
   }

   public boolean getHasGravity() { return entity.hasGravity(); }

   public void setHasGravity(boolean bo) { entity.setHasGravity(bo); }

   public int getAccuracy() {
      return entity.accuracy;
   }

   public void setAccuracy(int accuracy) { entity.accuracy = accuracy; }

   public void setHeading(IEntity<?> entity) { setHeading(entity.getX(), entity.getMCEntity().getBoundingBox().minY + (double)(entity.getHeight() / 2.0F), entity.getZ()); }

   public void setHeading(double x, double y, double z) {
      x -= entity.getX();
      y -= entity.getY();
      z -= entity.getZ();
      float varF = entity.hasGravity() ? (float)Math.sqrt(x * x + z * z) : 0.0F;
      float angle = entity.getAngleForXYZ(x, y, z, varF, false);
      float acc = 20.0F - (float)Mth.floor((float) entity.accuracy / 5.0F);
      entity.shoot(x, y, z, angle, acc);
   }

   public void setHeading(float yaw, float pitch) {
      entity.yRotO = yaw;
      entity.xRotO = pitch;
      entity.setYRot(yaw);
      entity.setXRot(pitch);
      double varX = -Mth.sin((float) Math.toRadians(yaw)) * Mth.cos((float) Math.toRadians(pitch));
      double varZ = Mth.cos((float) Math.toRadians(yaw)) * Mth.cos((float) Math.toRadians(pitch));
      double varY = -Mth.sin((float) Math.toRadians(pitch));
      float acc = 20.0F - (float)Mth.floor((float) entity.accuracy / 5.0F);
      entity.shoot(varX, varY, varZ, -pitch, acc);
   }

   public int getType() {
      return 7;
   }

   public boolean typeOf(int type) {
      return type == 7 || super.typeOf(type);
   }

   public void enableEvents() {
      if (!entity.scripts.contains(ScriptContainer.Current)) { entity.scripts.add(ScriptContainer.Current); }
   }

}
