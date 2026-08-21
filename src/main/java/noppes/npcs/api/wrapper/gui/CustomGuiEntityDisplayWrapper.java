package noppes.npcs.api.wrapper.gui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.api.INbt;
import noppes.npcs.api.constants.GuiComponentType;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.gui.IEntityDisplay;
import noppes.npcs.api.wrapper.NBTWrapper;

public class CustomGuiEntityDisplayWrapper
        extends CustomGuiComponentWrapper
        implements IEntityDisplay {

   protected IEntity<?> entity;
   protected INbt entityData = new NBTWrapper(new CompoundTag());
   protected int rotation;
   protected float scale = 1.0F;
   protected boolean showBackground = true;
   private boolean showRiders = false;

   public boolean isFollowingCursor = true;
   public int entityId = -1;

   public CustomGuiEntityDisplayWrapper() { }

   public CustomGuiEntityDisplayWrapper(int id, IEntity<?> entity, int x, int y) {
      setId(id);
      setEntity(entity);
      setPos(x, y);
   }

   @Override
   public IEntity<?> getEntity() { return entity; }

   public INbt getEntityData() { return entityData; }

   @Override
   public CustomGuiEntityDisplayWrapper setEntity(IEntity<?> entityIn) {
      entity = entityIn;
      if (entityIn == null) { entityData = new NBTWrapper(new CompoundTag()); }
      else { entityData = entityIn.getEntityNbt(); }
      if (entityIn != null && entityIn.getMCEntity() instanceof Player) { entityId = entityIn.getMCEntity().getId(); }
      return this;
   }

   @Override
   public int getRotation() { return rotation; }

   @Override
   public CustomGuiEntityDisplayWrapper setRotation(int rotationIn) {
      rotation = rotationIn;
      return this;
   }

   @Override
   public boolean isFollowingCursor() { return isFollowingCursor; }

   @Override
   public CustomGuiEntityDisplayWrapper setFollowingCursor(boolean state) {
      isFollowingCursor = state;
      return this;
   }

   @Override
   public float getScale() { return scale; }

   @Override
   public CustomGuiEntityDisplayWrapper setScale(float scaleIn) {
      scale = scaleIn;
      return this;
   }

   @Override
   public boolean getBackground() { return showBackground; }

   @Override
   public CustomGuiEntityDisplayWrapper setBackground(boolean bo) {
      showBackground = bo;
      return this;
   }

   @Override
   public int getType() { return GuiComponentType.ENTITY_DISPLAY.get(); }

   @Override
   public CompoundTag toNBT(CompoundTag compound) {
      super.toNBT(compound);
      compound.put("entity", entityData.getMCNBT());
      compound.putInt("entityId", entityId);
      compound.putInt("rotation", rotation);
      compound.putFloat("scale", scale);
      compound.putBoolean("followCursor", isFollowingCursor);
      compound.putBoolean("background", showBackground);
      compound.putBoolean("showRiders", showRiders);
      return compound;
   }

   @Override
   public CustomGuiEntityDisplayWrapper fromNBT(CompoundTag compound) {
      super.fromNBT(compound);
      entityData = new NBTWrapper(compound.getCompound("entity"));
      entityId = compound.getInt("entityId");
      setRotation(compound.getInt("rotation"));
      setScale(compound.getFloat("scale"));
      setFollowingCursor(compound.getBoolean("followCursor"));
      setBackground(compound.getBoolean("background"));
      showRiders(compound.getBoolean("showRiders"));
      return this;
   }

   // New Unofficial (Goodbird)
   @Override
   public CustomGuiEntityDisplayWrapper setEntitySyncedById(IEntity<?> entityIn) {
      entity = entityIn;
      if (entityIn == null) {
         entityData = new NBTWrapper(new CompoundTag());
         entityId = -1;
      }
      else { entityId = entityIn.getMCEntity().getId(); }
      return this;
   }

   public void showRiders(boolean isShow) { showRiders = isShow; }

   public boolean isShowingRiders() { return showRiders; }

}
