package noppes.npcs.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import noppes.npcs.CustomNpcs;
import noppes.npcs.ServerTickHandler;
import noppes.npcs.client.ClientTickHandler;

public class CustomNPCsScheduler {

   public static void runTack(Runnable task) { runTack(task, 0L); }

   public static void runTack(Runnable task, long delayMilliSeconds) {
      if (Util.instance.getSide() == Dist.DEDICATED_SERVER && CustomNpcs.Server != null) {
         if (delayMilliSeconds == 0L) {
            if (CustomNpcs.Server.isRunning()) { CustomNpcs.Server.submit(task); }
         }
         else { ServerTickHandler.addTask(task, Math.max(1, delayMilliSeconds / 50L)); }
      }
      else if (Util.instance.getSide() == Dist.CLIENT) {
         if (delayMilliSeconds == 0L) { Minecraft.getInstance().submit(task); }
         else { ClientTickHandler.addTask(task, Math.max(1, delayMilliSeconds / 50L)); }
      }
   }

}
