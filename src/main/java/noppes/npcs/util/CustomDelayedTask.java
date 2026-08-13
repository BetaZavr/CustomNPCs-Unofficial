package noppes.npcs.util;

public class CustomDelayedTask {

    public final Runnable task;
    public long ticksRemaining;

    public CustomDelayedTask(Runnable taskIn, long delayIn) {
        task = taskIn;
        ticksRemaining = delayIn;
    }

}
