package noppes.npcs.shared.client.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.mixin.client.resources.ISkinManagerMixin;
import noppes.npcs.shared.SharedReferences;

public class ResourceDownloader {
   private static final Set<ResourceLocation> active = Collections.synchronizedSet(new HashSet<>());
   private static final ScheduledExecutorService networkExecutor = Executors.newScheduledThreadPool(8);
   private static final AtomicInteger tickTaskBalance = new AtomicInteger(0);
   public static final int MAX_TASK_PER_TICK = 16;

   public static void resetTickTaskBalance() { tickTaskBalance.set(MAX_TASK_PER_TICK); }

   public static void load(ImageDownloadAlt resource) {
      if (!active.contains(resource.location)) {
         active.add(resource.location);
         networkExecutor.execute(() -> {
            if (tickTaskBalance.addAndGet(-1) >= 0) {
               resource.httpDownloadTextureFromServerAndUpload();
               Minecraft.getInstance().submit(() -> {
                  try { resource.load(Minecraft.getInstance().getResourceManager()); }
                  catch (IOException ignored) {  }
               });
               active.remove(resource.location);
            }
         });
      }
   }

   public static ResourceLocation getUrlResourceLocation(String url, boolean fixSkin) {
      return new ResourceLocation(SharedReferences.modid(), "skins/" + (url + fixSkin).hashCode() + (fixSkin ? "" : "32"));
   }

   public static File getUrlFile(String url, boolean fixSkin) {
      return new File(((ISkinManagerMixin) Minecraft.getInstance().getSkinManager()).getSkinsDirectory(), "" + (url + fixSkin).hashCode());
   }

   public static boolean contains(ResourceLocation location) {
      return active.contains(location);
   }

}
