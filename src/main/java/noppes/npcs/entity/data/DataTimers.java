package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.EventHooks;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.ITimers;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.controllers.scripts.IScriptBlockHandler;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;

public class DataTimers implements ITimers {

	protected class Timer {

		public int id;
		protected final boolean repeat;
		protected int ticks;
		protected final int timerTicks;

		public Timer(int idIn, int ticksIn, boolean repeatIn) {
            id = idIn;
			repeat = repeatIn;
			timerTicks = ticksIn;
			ticks = ticksIn;
		}

		public void update() {
			if (ticks-- > 0) { return; }
			if (repeat) { ticks = timerTicks; }
			else { stop(id); }
			Object ob = parent;
			if (ob instanceof EntityNPCInterface) { EventHooks.onNPCTimer((EntityNPCInterface) ob, id); }
			else if (ob instanceof PlayerData) { EventHooks.onPlayerTimer(new PlayerEvent.TimerEvent(((PlayerData) ob).getIPlayer(), id)); }
			else { EventHooks.onScriptBlockTimer((IScriptBlockHandler) ob, id); }
		}
	}

	protected final Object parent;
	protected Map<Integer, Timer> timers;

	public DataTimers(Object parentIn) {
		timers = new HashMap<>();
		parent = parentIn;
	}

	@Override
	public void clear() { timers = new HashMap<>(); }

	@Override
	public void forceStart(int id, int ticks, boolean repeat) { timers.put(id, new Timer(id, ticks, repeat)); }

	@Override
	public boolean has(int id) { return timers.containsKey(id); }

	public void readFromNBT(NBTTagCompound compound) {
		Map<Integer, Timer> timersIn = new HashMap<>();
		NBTTagList list = compound.getTagList("NpcsTimers", 10);
		for (int i = 0; i < list.tagCount(); ++i) {
			NBTTagCompound c = list.getCompoundTagAt(i);
			Timer t = new Timer(c.getInteger("ID"), c.getInteger("TimerTicks"), c.getBoolean("Repeat"));
			t.ticks = c.getInteger("Ticks");
			timersIn.put(t.id, t);
		}
		timers = timersIn;
	}

	@Override
	public void reset(int id) {
		Timer timer = timers.get(id);
		if (timer == null) { throw new CustomNPCsException("There is no timer with id: " + id); }
		timer.ticks = 0;
	}

	@Override
	public void start(int id, int ticks, boolean repeat) {
		if (timers.containsKey(id)) { throw new CustomNPCsException("There is already a timer with id: " + id); }
		timers.put(id, new Timer(id, ticks, repeat));
	}

	@Override
	public boolean stop(int id) { return timers.remove(id) != null; }

	public void update() {
		if (!timers.isEmpty()) {
			for (Timer timer : new ArrayList<>(timers.values())) { timer.update(); }
		}
	}

	public void writeToNBT(NBTTagCompound compound) {
		NBTTagList list = new NBTTagList();
		for (Timer timer : timers.values()) {
			NBTTagCompound c = new NBTTagCompound();
			c.setInteger("ID", timer.id);
			c.setInteger("TimerTicks", timer.id);
			c.setBoolean("Repeat", timer.repeat);
			c.setInteger("Ticks", timer.ticks);
			list.appendTag(c);
		}
		compound.setTag("NpcsTimers", list);
	}

}
