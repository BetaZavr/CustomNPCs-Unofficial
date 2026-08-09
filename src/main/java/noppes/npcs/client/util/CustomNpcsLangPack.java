package noppes.npcs.client.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.mixin.util.text.translation.ILanguageMapMixin;
import noppes.npcs.client.TranslateUtil;
import noppes.npcs.mixin.client.resources.II18nMixin;
import noppes.npcs.mixin.client.resources.ILocaleMixin;
import noppes.npcs.mixin.util.text.translation.II18nOldMixin;
import noppes.npcs.shared.common.util.LogWriter;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class CustomNpcsLangPack {

    private static String currentLanguage = "en_us";
    private static ILocaleMixin locale;
    private static ILanguageMapMixin languageMap;
    private static final Map<String, String> enProperties = Maps.newTreeMap();
    private static final Map<String, String> properties = Maps.newTreeMap();
    private static boolean init = false;

    public static void load() {
        if (!init) { registerReloadListener(); }
    }

    private static void registerReloadListener() {
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(manager -> {
                    try {
                        reload();
                        check();
                    }
                    catch (Exception e) { LogWriter.error(e); }
                });
        init = true;
    }

    private static void reload() throws IOException {
        locale = (ILocaleMixin) II18nMixin.getI18nLocale();
        languageMap = (ILanguageMapMixin) II18nOldMixin.getLocalizedName();
        clear();
        Path options = CustomNpcs.Dir.getParentFile().getParentFile().getParentFile().toPath().resolve("options.txt");
        if (Files.exists(options)) {
            try (BufferedReader reader = Files.newBufferedReader(options, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("lang:")) {
                        currentLanguage = line.substring("lang:".length());
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        Path langDir = CustomNpcs.Dir.toPath().resolve("assets/" + CustomNpcs.MODID + "/lang");
        if (Files.exists(langDir) || Files.createDirectories(langDir) != null) {
            Path enLang = langDir.resolve("en_us.json");
            if (Files.exists(enLang)) {
                loadLocaleData(true, Files.newInputStream(enLang));
            }
            Path curLang = langDir.resolve(currentLanguage + ".json");
            if (Files.exists(curLang)) {
                loadLocaleData(false, Files.newInputStream(curLang));
            }
        }
        save();
    }

    public static void save() {
        File langDir = new File(CustomNpcs.Dir, "assets/" + CustomNpcs.MODID + "/lang");
        if (langDir.exists() || langDir.mkdirs()) {
            saveLocaleData(true, new File(langDir, "en_us.json"));
            if (!currentLanguage.equals("en_us")) { saveLocaleData(false, new File(langDir, currentLanguage + ".json")); }
        }
    }

    private static void loadLocaleData(boolean isEn, InputStream inputStreamIn) throws IOException {
        inputStreamIn = net.minecraftforge.fml.common.FMLCommonHandler.instance().loadLanguage(properties, inputStreamIn);
        if (inputStreamIn == null) return;
        for (String s : IOUtils.readLines(inputStreamIn, StandardCharsets.UTF_8)) {
            if (!s.isEmpty() && s.charAt(0) != '#') {
                String[] astring = Iterables.toArray(locale.getSplitter().split(s), String.class);
                if (astring != null && astring.length == 2) {
                    String s1 = astring[0];
                    String s2 = locale.getPattern().matcher(astring[1]).replaceAll("%$1s");
                    if (isEn) { enProperties.put(s1, s2); } else { properties.put(s1, s2); }
                    locale.getProperties().put(s1, s2);
                    languageMap.npcs$getLanguageList().put(s1, s2);
                }
            }
        }
    }

    private static void saveLocaleData(boolean isEn, File lang) {
        Map<String, String> targetMap = isEn ? enProperties : properties;
        try (BufferedWriter writer = Files.newBufferedWriter(lang.toPath())) {
            StringBuilder jsonStr = new StringBuilder();
            String str = "";
            for (Map.Entry<String, String> entry : targetMap.entrySet()) {
                String pre = entry.getKey().contains(".") ? entry.getKey().substring(0, entry.getKey().indexOf(".")) : entry.getKey();
                if (!str.isEmpty() && !str.equals(pre)) { jsonStr.append((char) 10); }
                str = pre;
                jsonStr.append(entry.getKey()).append("=").append(entry.getValue()).append((char) 10);
            }
            writer.write(jsonStr.toString());
        }
        catch (Exception e) { LogWriter.error(e); }
    }

    private static void clear() {
        enProperties.clear();
        properties.clear();
    }

    /**
     * Add custom localization
     * @param key - localization
     * @param value - for the key, in "en_us" language
     */
    public static void added(String key, String value) {
        if (key == null || key.isEmpty() || value == null) { return; }
        if (currentLanguage.equals("en_us")) {
            store(key, value, value);
            return;
        }
        String language = currentLanguage;
        if (currentLanguage.contains("_")) {
            if (currentLanguage.equals("zh_cn")) { language = "zh_CN"; }
            else if (currentLanguage.equals("zh_tw")) { language = "zh_TW"; }
            else { language = currentLanguage.substring(0, currentLanguage.indexOf("_")); }
        }
        TranslateUtil.translate("en", language, value, translated ->
                Minecraft.getMinecraft().addScheduledTask(() -> store(key, value, translated == null ? value : translated)));
    }

    private static void store(String key, String value, String translateValue) {
        boolean needSave = !enProperties.containsKey(key) || !properties.containsKey(key);
        if (!needSave && (!enProperties.get(key).equals(value) || !properties.get(key).equals(translateValue))) { needSave = true; }
        enProperties.put(key, value);
        properties.put(key, translateValue);
        if (needSave) { save(); }
    }

    public static void check() {
        if (!properties.isEmpty()) {
            if (locale != null) { locale.getProperties().putAll(properties); }
            if (languageMap != null) { languageMap.npcs$getLanguageList().putAll(properties); }
            clear();
        }
    }

}
