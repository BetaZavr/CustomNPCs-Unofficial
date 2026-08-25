package noppes.npcs.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import noppes.npcs.CustomNpcs;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CustomNPCsScheduler {

   private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

   public static void runTack(Runnable task) { runTack(task, 0L); }

   public static void runTack(Runnable task, long delayMilliSeconds) {
      boolean isServer = Util.instance.getSide() == Dist.DEDICATED_SERVER;
      if (executor.isShutdown()) { executor = Executors.newScheduledThreadPool(1); }
      executor.schedule(() -> {
         if (isServer && CustomNpcs.Server != null) { CustomNpcs.Server.submit(task); }
         else if (!isServer) { Minecraft.getInstance().submit(task); }
      }, delayMilliSeconds, TimeUnit.MILLISECONDS);
   }

   public static void shutDown() {
      if (!executor.isShutdown()) {
         executor.shutdown();
         try {
            if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) { executor.shutdownNow(); }
         }
         catch (InterruptedException e) { executor.shutdownNow(); }
      }
   }

}
