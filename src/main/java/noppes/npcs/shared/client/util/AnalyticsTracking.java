package noppes.npcs.shared.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.common.util.LogWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AnalyticsTracking {

   public static void sendData(UUID uuid, String eventName, String data) {
      (new Thread(() -> {
         try {
            String analyticsPostData = getJSONString(uuid, data);
            URL url = new URL("https://www.google-analytics.com/mp/collect?measurement_id=G-VYV9D53HFS&api_secret=BQOVck8WTRG8yaCF_OhPdQ");
            HttpURLConnection connection = getHttpURLConnection(url, analyticsPostData);
            OutputStream dataOutput = connection.getOutputStream();
            dataOutput.write(analyticsPostData.getBytes(StandardCharsets.UTF_8));
            dataOutput.flush();
            dataOutput.close();
            connection.getInputStream().close();
            connection.disconnect();
         } catch (Exception e) {
            LogWriter.error(e);
         }
      })).start();
   }

   private static @NotNull HttpURLConnection getHttpURLConnection(URL url, String analyticsPostData) throws IOException {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(10000);
      connection.setDoOutput(true);
      connection.setUseCaches(false);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("Content-Length", Integer.toString(analyticsPostData.getBytes(StandardCharsets.UTF_8).length));
      return connection;
   }

   private static String getJSONString(UUID uuid, String data) {
      JsonObject body = new JsonObject();
      body.addProperty("client_id", uuid.toString());
      JsonArray events = new JsonArray();
      JsonObject event = new JsonObject();
      event.addProperty("name", CustomNpcs.MODID + "_1_20_1");
      JsonObject eventParams = new JsonObject();
      eventParams.addProperty("type", data);
      eventParams.addProperty("version", "1.20.1");
      event.add("params", eventParams);
      events.add(event);
      body.add("events", events);
       return body.toString();
   }

}
