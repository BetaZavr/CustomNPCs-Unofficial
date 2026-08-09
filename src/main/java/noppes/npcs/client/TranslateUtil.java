package noppes.npcs.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.entity.player.EntityPlayerMP;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSpeak;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TranslateUtil {

    private static volatile boolean hasInternet = true;
    private static final Map<String, String> translateDate = new ConcurrentHashMap<>();
    private static final Set<String> pending = ConcurrentHashMap.newKeySet();
    private static final ExecutorService executor = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "CustomNPCs Translate");
        thread.setDaemon(true);
        return thread;
    });

    private static final String TranslateUrl = "https://translate.google.com/translate_a/single?client=gtx&sl=%s&tl=%s&dt=t&q=%s";
    public static final String AudioUrl = "http://translate.google.com/translate_tts?q=%s&tl=%s";

    @SuppressWarnings("all")
    public static boolean hasInternet() { return hasInternet; }

    public static String translate(String textLanguageKey, String translationLanguageKey, String originalText) {
        return translate(textLanguageKey, translationLanguageKey, originalText, null);
    }

    public static String translate(String textLanguageKey, String translationLanguageKey, String originalText, Consumer<String> callback) {
        if (translationLanguageKey == null || translationLanguageKey.isEmpty() || originalText == null || originalText.isEmpty()) {
            if (callback != null) { callback.accept(originalText); }
            return originalText;
        }
        if (textLanguageKey == null || textLanguageKey.isEmpty()) { textLanguageKey = "auto"; }
        String key = textLanguageKey+"_"+translationLanguageKey+"_"+originalText;
        String cached = translateDate.get(key);
        if (cached != null) {
            if (callback != null) { callback.accept(cached); }
            return cached;
        }
        if (!hasInternet || !pending.add(key)) {
            if (callback != null) { callback.accept(originalText); }
            return originalText;
        }
        final String textKey = textLanguageKey;
        executor.execute(() -> {
            String result = originalText;
            try { result = translateAll(textKey, translationLanguageKey, originalText); }
            catch (Exception e) { LogWriter.error("Error trying to translate via Google", e); }
            finally { pending.remove(key); }
            translateDate.put(key, result);
            if (callback != null) { callback.accept(result); }
        });
        return originalText;
    }

    private static String translateAll(String textLanguageKey, String translationLanguageKey, String originalText) {
        if (originalText.length() <= 5000) { return translateGoogle(textLanguageKey, translationLanguageKey, originalText); }
        String type = " "; // simple words
        if (originalText.contains("\n")) { type = "\n"; } // some code
        else if (originalText.contains(". ")) { type = ". "; } // suggestions
        List<String> translatedParts = new ArrayList<>();
        for (String part : originalText.split(type)) {
            if (part.length() <= 5000) { translatedParts.add(translateGoogle(textLanguageKey, translationLanguageKey, part)); }
            else if (!type.equals(" ")) {
                List<String> translatedSubParts = new ArrayList<>();
                for (String subPart : part.split(" ")) {
                    if (subPart.length() <= 5000) { translatedSubParts.add(translateGoogle(textLanguageKey, translationLanguageKey, subPart)); }
                    else { translatedSubParts.add(subPart) ; }
                }
                StringBuilder subText = new StringBuilder();
                for (String subTranslatedPart : translatedSubParts) { subText.append(subTranslatedPart).append(" "); }
                translatedParts.add(subText.toString());
            }
            else { translatedParts.add(part); }
        }
        StringBuilder text = new StringBuilder();
        for (String translatedPart : translatedParts) { text.append(translatedPart).append(type); }
        return text.toString();
    }

    private static String translateGoogle(String textLanguageKey, String translationLanguageKey, String originalText) {
        if (!hasInternet) { return originalText; }
        originalText = Util.instance.deleteColor(originalText);
        try {
            URLConnection connection = new URL(String.format(TranslateUrl, textLanguageKey, translationLanguageKey,
                    URLEncoder.encode(originalText, "UTF-8"))).openConnection();
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("User-Agent", "Chrome/99.0.4844.51");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) { text.append(line).append("\n"); }
            reader.close();

            Gson gson = new Gson();
            JsonElement element = gson.fromJson(text.toString(), JsonElement.class);
            JsonArray outerArray = element.getAsJsonArray();
            JsonArray innerArray = outerArray.get(0).getAsJsonArray();
            JsonArray firstPair = innerArray.get(0).getAsJsonArray();
            String translatedText = firstPair.get(0).getAsString();

            hasInternet = true;
            return translatedText;
        } catch (SocketTimeoutException | SSLHandshakeException se) {
            hasInternet = false;
            LogWriter.error("Error: No internet connection", se);
        }
        catch (Exception e) { LogWriter.error("Error trying to translate via Google", e); }
        return originalText;
    }

    /**
     * Text-to-speech via Google Translate TTS.
     * @param player player to make a sound
     * @param languageKey language to translate text into
     * @param text text to be spoken
     * @param volume sound volume
     */
    @SuppressWarnings("all")
    public static void speak(IPlayer<?> player, String languageKey, String text, float volume) {
        if (player == null) { return; }
        if (!(player.getMCEntity() instanceof EntityPlayerMP)) {
            MusicController.Instance.speak(languageKey, text, volume);
            return;
        }
        Packets.send((EntityPlayerMP) player.getMCEntity(), new PacketSpeak(languageKey, text, volume));
    }

    /**
     * Text-to-speech via Google Translate TTS.
     * @param world world in which to reproduce translation
     * @param pos position in the world where to play translation
     * @param range search distance for players around. Default is 64 blocks
     * @param languageKey language to translate text into
     * @param text text to be spoken
     * @param volume sound volume
     */
    @SuppressWarnings("all")
    public static void speak(IWorld world, IPos pos, int range, String languageKey, String text, float volume) {
        if (world == null || world.getMCWorld() == null || pos == null) { return; }
        if (world.getMCWorld().isRemote) {
            MusicController.Instance.speak(languageKey, text, volume);
            return;
        }
        Packets.sendNearby(world.getMCWorld(), pos.getMCBlockPos(), Math.max(range, 3), new PacketSpeak(languageKey, text, volume));
    }

}