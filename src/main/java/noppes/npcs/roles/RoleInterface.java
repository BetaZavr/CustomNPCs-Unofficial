package noppes.npcs.roles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.api.entity.data.INPCRole;
import noppes.npcs.entity.EntityNPCInterface;

public abstract class RoleInterface implements INPCRole {

   public static final RoleInterface NONE = new RoleInterface(null) {

      @Override
      public CompoundTag save(CompoundTag compound) { return compound; }

      @Override
      public void load(CompoundTag compound) { }

      @Override
      public int getType() { return RoleType.NONE.get(); }

      @Override
      public RoleType getEnumType() { return RoleType.NONE; }

   };

   public EntityNPCInterface npc;

   // New from Unofficial (BetaZavr)
   public RoleType type = RoleType.NONE;

   public RoleInterface(EntityNPCInterface npcIn) { npc = npcIn; }

   public void killed() { }

   public void delete() { }

   @SuppressWarnings("unused")
   public void aiDeathExecute(Entity attackingEntity) { }

   public boolean aiShouldExecute() {
      return false;
   }

   public boolean aiContinueExecute() {
      return false;
   }

   public void aiStartExecuting() { }

   public void aiUpdateTask() { }

   public boolean defendOwner() {
      return false;
   }

   public boolean isFollowing() {
      return false;
   }

   public void clientUpdate() { }

   // New from Unofficial (BetaZavr)
   public void load(CompoundTag compound) { type = RoleType.get(compound.getInt("Type")); }

   public CompoundTag save(CompoundTag compound) {
      compound.putInt("Type", type.get());
      return compound;
   }

   @Override
   public int getType() { return type.get(); }

   public RoleType getEnumType() { return type; }

   public void interact(Player player) { }

}
