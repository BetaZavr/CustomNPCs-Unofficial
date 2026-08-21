package noppes.npcs.shared.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.shared.SharedReferences;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ImageDownloadAlt extends SimpleTexture {

   private static final Logger logger = LogManager.getLogger();
   public final File cacheFile;
   private final String imageUrl;
   private final boolean fix64;
   private final Runnable r;
   public final ResourceLocation location;
   public boolean uploaded = false;

   public ImageDownloadAlt(File file, String url, ResourceLocation locationIn, ResourceLocation defaultLocation, boolean fix64In, Runnable rIn) {
      super(defaultLocation);
      location = locationIn;
      cacheFile = file;
      imageUrl = url;
      fix64 = fix64In;
      r = rIn;
   }

   public void setImage(NativeImage image) {
      Minecraft.getInstance().execute(() -> {
         uploaded = true;
         if (!RenderSystem.isOnRenderThread()) { RenderSystem.recordRenderCall(() -> upload(image)); }
         else { upload(image); }
         r.run();
      });
   }

   private void upload(NativeImage imageIn) {
      TextureUtil.prepareImage(getId(), imageIn.getWidth(), imageIn.getHeight());
      imageIn.upload(0, 0, 0, true);
   }

   public void load(@NotNull ResourceManager resourceManager) throws IOException {
      if (cacheFile != null && cacheFile.isFile()) {
          logger.debug("Loading http texture from local cache ({})", cacheFile);
         NativeImage image;
         try {
            image = NativeImage.read(new FileInputStream(cacheFile));
            setImage(parseUserSkin(image));
            return;
         } catch (IOException e) {
            super.load(resourceManager);
             logger.error("Couldn't load skin ({})", cacheFile, e);
         }
      }
      if (!uploaded) {
         try {
            uploaded = true;
            super.load(resourceManager);
         } catch (Exception ignored) {
         }
      }
   }

   public void httpDownloadTextureFromServerAndUpload() {
      httpDownloadAndUpload(imageUrl, false);
   }

   private void httpDownloadAndUpload(String url, boolean wasRedirect) {
      HttpURLConnection connection = null;
      logger.debug("Downloading http texture from {} to {}", new Object[]{url, cacheFile});

      try {
         connection = (HttpURLConnection)(new URL(url)).openConnection();
         connection.setDoInput(true);
         connection.setDoOutput(false);
         connection.setInstanceFollowRedirects(false);
         connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
         connection.setRequestProperty("Content-Type", "image/png");
         connection.setRequestProperty("Accept", "image/png");
         connection.setRequestProperty("Expect", "100-continue");
         connection.connect();
         String type = connection.getContentType();
         long size = connection.getContentLengthLong();
         int statusCode = connection.getResponseCode();
         if (wasRedirect || statusCode != 302 && statusCode != 301 && statusCode != 303) {
            if (statusCode / 100 == 2 && type.equals("image/png") && (size <= 2000000L || Minecraft.getInstance().hasSingleplayerServer())) {
               FileUtils.copyInputStreamToFile(connection.getInputStream(), cacheFile);
            }
            return;
         }
         String newUrl = connection.getHeaderField("Location");
         if (newUrl != null && !newUrl.trim().isEmpty()) {
            httpDownloadAndUpload(newUrl, true);
         }
         logger.debug("Downloading http texture done");
      } catch (Exception e) {
         logger.error("Couldn't download http texture", e);
      } finally {
         if (connection != null) {
            connection.disconnect();
         }
      }
   }

   public NativeImage parseUserSkin(NativeImage image) {
      if (image.getHeight() != image.getWidth() && image.getWidth() / 2 != image.getHeight()) {
         int var10002 = image.getWidth();
         throw new IllegalArgumentException("Invalid texture size: " + var10002 + "x" + image.getHeight());
      }
      int scale = image.getWidth() / 64;
      boolean lvt_2_1_ = image.getHeight() != image.getWidth();
      if (lvt_2_1_ && fix64) {
         NativeImage nativeImage = new NativeImage(64 * scale, 64 * scale, true);
         nativeImage.copyFrom(image);
         image.close();
         image = nativeImage;
         nativeImage.fillRect(0, 32 * scale, 64 * scale, 32 * scale, 0);
         nativeImage.copyRect(4 * scale, 16 * scale, 16 * scale, 32 * scale, 4 * scale, 4 * scale, true, false);
         nativeImage.copyRect(8 * scale, 16 * scale, 16 * scale, 32 * scale, 4 * scale, 4 * scale, true, false);
         nativeImage.copyRect(0, 20 * scale, 24 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
         nativeImage.copyRect(4 * scale, 20 * scale, 16 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
         nativeImage.copyRect(8 * scale, 20 * scale, 8 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
         nativeImage.copyRect(12 * scale, 20 * scale, 16 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
         nativeImage.copyRect(44 * scale, 16 * scale, -8 * scale, 32 * scale, 4 * scale, 4 * scale, true, false);
         nativeImage.copyRect(48 * scale, 16 * scale, -8 * scale, 32 * scale, 4 * scale, 4 * scale, true, false);
         nativeImage.copyRect(40 * scale, 20 * scale, 0, 32 * scale, 4 * scale, 12 * scale, true, false);
         nativeImage.copyRect(44 * scale, 20 * scale, -8 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
         nativeImage.copyRect(48 * scale, 20 * scale, -16 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
         nativeImage.copyRect(52 * scale, 20 * scale, -8 * scale, 32 * scale, 4 * scale, 12 * scale, true, false);
      }
      int x = 0;
      int y = 0;
      if (!SharedReferences.AllowFullyInvisibleSkins()) {
         setAreaOpaque(image, x, y, 32 * scale, 16 * scale);
      }
      if (lvt_2_1_ && fix64) {
         setAreaTransparent(image, 32 * scale, y, 64 * scale, 32 * scale);
      }
      return image;
   }

   private static void setAreaTransparent(NativeImage image, int x, int y, int width, int height) {
      int l;
      int i1;
      for(l = x; l < width; ++l) {
         for(i1 = y; i1 < height; ++i1) {
            int k = image.getPixelRGBA(l, i1);
            if ((k >> 24 & 255) < 128) {
               return;
            }
         }
      }
      for(l = x; l < width; ++l) {
         for(i1 = y; i1 < height; ++i1) {
            image.setPixelRGBA(l, i1, image.getPixelRGBA(l, i1) & new Color(0xFFFFFF).getRGB());
         }
      }
   }

   private static void setAreaOpaque(NativeImage image, int x, int y, int width, int height) {
      for (int i = x; i < width; ++i) {
         for (int j = y; j < height; ++j) {
            image.setPixelRGBA(i, j, image.getPixelRGBA(i, j) | new Color(0xFF000000).getRGB());
         }
      }
   }

}
