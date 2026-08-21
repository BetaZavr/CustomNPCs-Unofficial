package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import noppes.npcs.EventHooks;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.ITimers;
import noppes.npcs.controllers.scripts.IScriptBlockHandler;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;

public class DataTimers implements ITimers {

   private final Object parent;
   private final Map<Integer, DataTimers.Timer> timers = new HashMap<>();

   public DataTimers(Object parentIn) { parent = parentIn; }

   @Override
   public void start(int id, int ticks, boolean repeat) {
      if (timers.containsKey(id)) {
         throw new CustomNPCsException("There is already a timer with id: " + id);
      } else {
         timers.put(id, new DataTimers.Timer(id, ticks, repeat));
      }
   }

   @Override
   public void forceStart(int id, int ticks, boolean repeat) {
      timers.put(id, new DataTimers.Timer(id, ticks, repeat));
   }

   @Override
   public boolean has(int id) {
      return timers.containsKey(id);
   }

   @Override
   public boolean stop(int id) {
      return timers.remove(id) != null;
   }

   @Override
   public void reset(int id) {
      DataTimers.Timer timer = timers.get(id);
      if (timer == null) {
         throw new CustomNPCsException("There is no timer with id: " + id);
      } else {
         timer.ticks = 0;
      }
   }

   public void save(CompoundTag compound) {
      ListTag list = new ListTag();
      for (Timer timer : timers.values()) {
         CompoundTag c = new CompoundTag();
         c.putInt("ID", timer.id);
         c.putInt("TimerTicks", timer.id);
         c.putBoolean("Repeat", timer.repeat);
         c.putInt("Ticks", timer.ticks);
         list.add(c);
      }
      compound.put("NpcsTimers", list);
   }

   public void load(CompoundTag compound) {
      timers.clear();
      ListTag list = compound.getList("NpcsTimers", 10);
      for(int i = 0; i < list.size(); ++i) {
         CompoundTag c = list.getCompound(i);
         DataTimers.Timer t = new DataTimers.Timer(c.getInt("ID"), c.getInt("TimerTicks"), c.getBoolean("Repeat"));
         t.ticks = c.getInt("Ticks");
         timers.put(t.id, t);
      }
   }

   public void update() {
      for (Timer timer : new ArrayList<>(timers.values())) {
         timer.update();
      }
   }

   @Override
   public void clear() {
      timers.clear();
   }

   class Timer {

      public int id;
      private final boolean repeat;
      private final int timerTicks;
      private int ticks;

      public Timer(int idIn, int ticksIn, boolean repeatIn) {
         id = idIn;
         repeat = repeatIn;
         timerTicks = ticksIn;
         ticks = ticksIn;
      }

      public void update() {
         if (ticks-- <= 0) {
            if (repeat) { ticks = timerTicks; }
            else { stop(id); }
            Object ob = parent;
            if (ob instanceof EntityNPCInterface) {
               EventHooks.onNPCTimer((EntityNPCInterface)ob, id);
            } else if (ob instanceof PlayerData) {
               EventHooks.onPlayerTimer((PlayerData)ob, id);
            } else {
               EventHooks.onScriptBlockTimer((IScriptBlockHandler)ob, id);
            }
         }
      }
   }

}
