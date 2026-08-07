package noppes.npcs.api.wrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.ITimers;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntityLiving;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.IProjectile;
import noppes.npcs.api.entity.data.INPCAdvanced;
import noppes.npcs.api.entity.data.INPCAi;
import noppes.npcs.api.entity.data.INPCDisplay;
import noppes.npcs.api.entity.data.INPCInventory;
import noppes.npcs.api.entity.data.INPCJob;
import noppes.npcs.api.entity.data.INPCRole;
import noppes.npcs.api.entity.data.INPCStats;
import noppes.npcs.api.handler.data.IDialog;
import noppes.npcs.api.handler.data.IFaction;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketNpcRotationUpdate;
import noppes.npcs.util.ValueUtil;

import java.util.Objects;
import java.util.TreeMap;

public class NPCWrapper<T extends EntityNPCInterface> extends EntityLivingWrapper<T> implements ICustomNpc<T> {

   public NPCWrapper(T npc) {
      super(npc);
   }

   public void setMaxHealth(float health) {
      if ((int)health != this.entity.stats.maxHealth) {
         super.setMaxHealth(health);
         this.entity.stats.maxHealth = (int)health;
         this.entity.updateClient = true;
      }
   }

   public INPCDisplay getDisplay() {
      return this.entity.display;
   }

   public INPCInventory getInventory() {
      return this.entity.inventory;
   }

   public INPCAi getAi() {
      return this.entity.ais;
   }

   public INPCAdvanced getAdvanced() {
      return this.entity.advanced;
   }

   public INPCStats getStats() {
      return this.entity.stats;
   }

   public IFaction getFaction() {
      return this.entity.faction;
   }

   public ITimers getTimers() {
      return this.entity.timers;
   }

   public void setFaction(int id) {
      Faction faction = FactionController.instance.getFaction(id);
      if (faction == null) {
         throw new CustomNPCsException("Unknown faction id: " + id);
      } else {
         this.entity.setFaction(id);
      }
   }

   public INPCRole getRole() {
      return this.entity.role;
   }

   public INPCJob getJob() {
      return this.entity.job;
   }

   public int getHomeX() {
      return this.entity.ais.startPos().getX();
   }

   public int getHomeY() {
      return this.entity.ais.startPos().getY();
   }

   public int getHomeZ() {
      return this.entity.ais.startPos().getZ();
   }

   public void setHome(int x, int y, int z) {
      this.entity.ais.setStartPos(new BlockPos(x, y, z));
   }

   public int getOffsetX() {
      return (int) this.entity.ais.bodyOffsetX;
   }

   public int getOffsetY() {
      return (int) this.entity.ais.bodyOffsetY;
   }

   @SuppressWarnings("all")
   public int getOffsetZ() {
      return (int) this.entity.ais.bodyOffsetZ;
   }

   @SuppressWarnings("all")
   public void setOffset(int x, int y, int z) {
      this.entity.ais.bodyOffsetX = ValueUtil.correctFloat((float)x, 0.0F, 10.0F);
      this.entity.ais.bodyOffsetY = ValueUtil.correctFloat((float)y, 0.0F, 100.0F);
      this.entity.ais.bodyOffsetZ = ValueUtil.correctFloat((float)z, 0.0F, 10.0F);
      this.entity.updateClient = true;
   }

   public void say(String message) {
      this.entity.saySurrounding(new Line(message));
   }

   public void sayTo(IPlayer<?> player, String message) {
      this.entity.say(player.getMCEntity(), new Line(message));
   }

   public void reset() {
      this.entity.reset();
   }

   public long getAge() {
      return this.entity.totalTicksAlive;
   }

   public IProjectile<?> shootItem(IEntityLiving<?> target, IItemStack item, int accuracy) {
      if (item == null) {
         throw new CustomNPCsException("No item was given");
      } else if (target == null) {
         throw new CustomNPCsException("No target was given");
      } else {
         accuracy = ValueUtil.correctInt(accuracy, 1, 100);
         return (IProjectile<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(this.entity.shoot(target.getMCEntity(), accuracy, item.getMCItemStack(), false));
      }
   }

   public IProjectile<?> shootItem(double x, double y, double z, IItemStack item, int accuracy) {
      if (item == null) {
         throw new CustomNPCsException("No item was given");
      } else {
         accuracy = ValueUtil.correctInt(accuracy, 1, 100);
         return (IProjectile<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(this.entity.shoot(x, y, z, accuracy, item.getMCItemStack(), false));
      }
   }

   public void giveItem(IPlayer<?> player, IItemStack item) {
      this.entity.givePlayerItem(player.getMCEntity(), item.getMCItemStack());
   }

   public String executeCommand(String command) {
      if (!Objects.requireNonNull(this.entity.getServer()).isCommandBlockEnabled()) {
         throw new CustomNPCsException("Command blocks need to be enabled to executeCommands");
      } else {
         return NoppesUtilServer.runCommand(this.entity, this.entity.getName().getString(), command, null);
      }
   }

   public int getType() {
      return 2;
   }

   public String getName() {
      return this.entity.display.getName();
   }

   public void setName(String name) {
      this.entity.display.setName(name);
   }

   public void setRotation(float rotation) {
      super.setRotation(rotation);
      int r = (int) rotation;
      if (entity.ais.orientation != r) {
         entity.ais.orientation = r;
         Packets.sendNearby(this.entity, new PacketNpcRotationUpdate(this.entity.getId(), this.entity.ais.orientation));
      }
   }

   public boolean typeOf(int type) {
      return type == 2 || super.typeOf(type);
   }

   @Override
   public void setDialog(int slot, IDialog dialog) {
      if (slot >= 0 && slot <= entity.dialogs.length) {
         if (dialog == null && slot < entity.dialogs.length) {
            int[] newIDs = new int[entity.dialogs.length - 1];
            for (int i = 0, j = 0; i < entity.dialogs.length; i++) {
               if (i == slot) {
                  continue;
               }
               newIDs[j] = entity.dialogs[i];
               j++;
            }
            entity.dialogs = newIDs;
         }
         else if (dialog != null) {
            if (slot == entity.dialogs.length) {
               int[] newIDs = new int[entity.dialogs.length + 1];
               System.arraycopy(entity.dialogs, 0, newIDs, 0, entity.dialogs.length);
               entity.dialogs = newIDs;
            }
            entity.dialogs[slot] = dialog.getId();
         }
      }
      else {
         throw new CustomNPCsException("Slot needs to be between 0 and " + entity.dialogs.length);
      }
   }

   public IDialog getDialog(int slot) {
      if (slot < 0 || slot >= entity.dialogs.length) {
         throw new CustomNPCsException("Slot needs to be between 0 and " + (this.entity.dialogs.length - 1));
      }
      IDialog dialog = null;
      int s = 0;
      TreeMap<Integer, Dialog> dialogs = DialogController.instance.dialogs;
      for (int dialogId : entity.dialogs) {
         if (s == slot) {
            if (dialogs.containsKey(dialogId)) { dialog = dialogs.get(dialogId); }
            break;
         }
      }
      return dialog;
   }

   public void updateClient() {
      this.entity.updateClient();
   }

   public IEntityLiving<?> getOwner() {
      LivingEntity owner = this.entity.getOwner();
      return owner != null ? (IEntityLiving<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(owner) : null;
   }

   public void trigger(int id, Object... arguments) {
      EventHooks.onScriptTriggerEvent(this.entity.script, id, this.getWorld(), this.getPos(), null, arguments);
   }
}
