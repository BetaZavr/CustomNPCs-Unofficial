package noppes.npcs.shared.client.util;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.mixin.client.resources.ISkinManagerMixin;
import noppes.npcs.shared.SharedReferences;
import noppes.npcs.util.CustomNPCsScheduler;

public class ResourceDownloader {
   private static final Set<ResourceLocation> active = Collections.synchronizedSet(new HashSet<>());

   public static void load(ImageDownloadAlt resource) {
      if (!active.contains(resource.location)) {
         active.add(resource.location);
         CustomNPCsScheduler.runTack(() -> {
            resource.loadTextureFromServer();
            CustomNPCsScheduler.runTack(() -> {
               Minecraft.getInstance().getTextureManager().register(resource.location, resource);
               active.remove(resource.location);
            });
         }, 400L);
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
