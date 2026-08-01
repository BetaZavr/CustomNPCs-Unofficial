package noppes.npcs.client.gui.select;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.client.renderer.texture.ITextureManagerMixin;
import noppes.npcs.mixin.client.renderer.texture.ITextureMapMixin;
import noppes.npcs.mixin.client.resources.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceSelection
        extends GuiNPCInterface
        implements ICustomScrollListener {


    public static Map<String, Map<String, TreeMap<ResourceLocation, Long>>> resourcesData = new ConcurrentHashMap<>();

    private static final Set<String> loadingSuffixes = ConcurrentHashMap.newKeySet();

    protected final Map<String, TreeMap<ResourceLocation, Long>> data = new TreeMap<>(); // (Directory, Files)
    protected final GuiScreen parent;
    protected final Component back;
    protected ResourceLocation selectDir;
    protected GuiCustomScrollNop scroll;
    protected String suffix;
    protected String baseResource = "";
    protected int offsetX = 0;
    protected int scrollWidth;
    protected Component select = Component.empty();

    public final int id;
    public ResourceLocation resource;

    public ResourceSelection(GuiScreen parentIn, int idIn, EntityNPCInterface npcIn, @Nonnull String startIn, String suffixIn) {
        super(npcIn);
        drawDefaultBackground = false;
        setBackground("menubg.png");
        imageWidth = 366;
        imageHeight = 226;
        scrollWidth = imageWidth - 10;

        id = idIn;
        parent = parentIn;
        suffix = normalizeSuffix(suffixIn);
        back = Component.literal("   " + Character.toChars(0x2190)[0] + " (")
                .append(Component.translatable("gui.back")).append(Component.literal(")"))
                .withStyle(TextFormatting.GOLD);

        if (resourcesData.containsKey(suffix)) { data.putAll(resourcesData.get(suffix)); }

        selectDir = null;
        ResourceLocation loc = new ResourceLocation(startIn);
        if (data.containsKey(loc.getResourceDomain()) && !data.get(loc.getResourceDomain()).containsKey(loc)) {
            try {
                if (!startIn.isEmpty()) { minecraft.getTextureManager().getTexture(loc); }
            }
            catch (Exception ignored) { }
        }
        if (!data.containsKey(loc.getResourceDomain())) { loadFiles(); }
        baseResource = startIn;
        if (!startIn.isEmpty()) {
            resource = new ResourceLocation(startIn);
            if (startIn.lastIndexOf("/") != -1) {
                startIn = startIn.substring(0, startIn.lastIndexOf("/"));
            }
            selectDir = new ResourceLocation(startIn);
            if (!data.containsKey(selectDir.getResourceDomain())) {
                selectDir = null;
                return;
            }
            for (ResourceLocation r : data.get(selectDir.getResourceDomain()).keySet()) {
                if (r.getResourcePath().indexOf(selectDir.getResourcePath()) == 0) { return; }
            }
            selectDir = null;
        }
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        if (button.id == 1 || button.id == 2 || button.id == 66) {
            if ((button.id == 1 || button.id == 66)) { cancel(); }
            onClose();
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        guiLeft += offsetX;
        int h = guiTop + imageHeight - 25;
        addButton(2, guiLeft + 271, h, "gui.done")
                .setSize(90, 20)
                .setHoverTexts("selection.hover.done");
        addButton(1, guiLeft + 5, h, "gui.cancel")
                .setSize(90, 20)
                .setHoverTexts("hover.back");
        if (scroll == null) { scroll = addScroll(0).setSize(scrollWidth, 180); }
        Component domain = Component.literal("All Data in Game (mods)/");
        if (data.isEmpty() && isLoading(suffix)) { domain = Component.translatable("gui.wait", ""); }
        if (selectDir == null) { scroll.setList(new ArrayList<>(data.keySet())); }
        else {
            List<Component> list = new ArrayList<>();
            Map<String, Long> ds = new TreeMap<>();
            Map<String, Long> fs = new TreeMap<>();
            String path = selectDir.getResourcePath();
            TreeMap<ResourceLocation, Long> files = data.get(selectDir.getResourceDomain());
            if (files == null) { files = new TreeMap<>(); }
            for (ResourceLocation res : files.keySet()) {
                String resPath = res.getResourcePath();
                if (!path.isEmpty() && resPath.indexOf(path + "/") != 0) { continue; }
                String key = path.isEmpty() ? resPath : resPath.substring(path.length() + 1);
                if (key.contains("/")) { ds.put(key.substring(0, key.indexOf("/")), files.get(res)); }
                else if (suffix.isEmpty() || resPath.toLowerCase().endsWith(suffix)) { fs.put(key, files.get(res)); }
            }
            String txrName = resource != null ? resource.getResourcePath() : "";
            if (!txrName.isEmpty()) {
                txrName = txrName.substring(txrName.lastIndexOf("/") + 1);
            }
            List<Component> suffixes = new ArrayList<>();
            int i = 1, pos = -1;
            suffixes.add(Component.empty());
            for (String key : ds.keySet()) {
                suffixes.add(Component.empty());
                Component line = Component.literal(key).withStyle(TextFormatting.GOLD);
                list.add(line);
                i++;
            }
            for (String key : fs.keySet()) {
                if (fs.get(key) == 0L) { suffixes.add(Component.empty()); }
                else {
                    suffixes.add(Component.literal(Util.instance.getTextReducedNumber(fs.get(key), false, false, true) + "b"));
                }
                Component line = Component.literal(key).withStyle(TextFormatting.AQUA);
                list.add(line);
                if (txrName.equals(key)) {
                    pos = i;
                }
                i++;
            }
            list.add(0, back);
            scroll.setUnsortedList(list).setSuffixes(suffixes);
            if (scroll.getSelectedIndex() != pos) { scroll.setSelect(pos); }
            domain = Component.empty().append(Component.literal(selectDir.getResourceDomain() + "/" + path));
            while (font.getStringWidth(domain.getFormattedText()) > 250 && path.contains("/")) {
                path = path.substring(path.indexOf("/") + 1);
                domain = Component.empty().append(Component.literal(selectDir.getResourceDomain() + "/.../" + path));
            }
        }
        add(scroll.setPos(guiLeft + 5, guiTop + 19));
        addLabel(0, guiLeft + 6, guiTop + 6, domain)
                .setSize(250, 10)
                .setColor(new Color(0xFF000000).getRGB());
        addButton(66, guiLeft + imageWidth - 17, guiTop + 5, "X")
                .setSize(12, 12);
        select = scroll.getNormalSelected();
    }

    @Override
    public boolean keyPressed(char typedChar, int keyCode) {
        if (closeOnEsc && GuiBasic.isEscKey(keyCode)) { cancel(); }
        if (scroll != null && scroll.getSearchValue().isEmpty() && keyCode == Keyboard.KEY_BACK) {
            List<String> list = scroll.getList();
            if (!list.isEmpty() && list.get(0).equals(back.getFormattedText())) {
                if (selectDir != null) {
                    if (!selectDir.getResourcePath().contains("/")) { selectDir = null; }
                    else { selectDir = new ResourceLocation(selectDir.getResourceDomain(), selectDir.getResourcePath().substring(0, selectDir.getResourcePath().lastIndexOf("/"))); }
                    initGui();
                    return true;
                }
            }
        }
        return super.keyPressed(typedChar, keyCode);
    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {
        if (scroll.getNormalSelected().equals(back)) {
            if (selectDir == null) { return; }
            if (!selectDir.getResourcePath().contains("/")) { selectDir = null; }
            else { selectDir = new ResourceLocation(selectDir.getResourceDomain(), selectDir.getResourcePath().substring(0, selectDir.getResourcePath().lastIndexOf("/"))); }
            initGui();
        }
        else if (selectDir != null) {
            String name = selectedName(scroll);
            if (name.isEmpty()) { return; }
            if (!name.toLowerCase().endsWith(suffix)) {
                if (suffix.equals(".ogg")) { resource = new ResourceLocation(selectDir.getResourceDomain(), name); }
                else {
                    selectDir = new ResourceLocation(selectDir.getResourceDomain(), selectDir.getResourcePath() + "/" + name);
                    initGui();
                }
            } else {
                resource = new ResourceLocation(selectDir.getResourceDomain(), selectDir.getResourcePath() + "/" + name);
            }
        }
        else if (data.containsKey(selectedName(scroll))) {
            String domain = selectedName(scroll);
            String res = null, def = null;
            for (ResourceLocation loc : data.get(domain).keySet()) {
                if (def == null) {
                    if (loc.getResourcePath().contains("/")) { def = loc.getResourcePath().substring(0, loc.getResourcePath().indexOf("/")); }
                    else { def = loc.getResourcePath(); }
                }
                if (loc.getResourcePath().contains("/") &&
                        loc.getResourcePath().substring(0, loc.getResourcePath().indexOf("/")).equals("textures")) {
                    res = "textures";
                    break;
                }
            }
            if (res == null) { res = def; }
            if (res != null) { selectDir = new ResourceLocation(domain, res); }
            initGui();
        }
    }

    /** Row text without the {@code §} formatting codes the scroll adds for display. */
    protected String selectedName(GuiCustomScrollNop scroll) { return scroll.getNormalSelected().getString(); }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
        if (resource != null) {
            onClose();
            if (parent != null) { parent.initGui(); }
        }
    }

    protected void cancel() {
        if (!baseResource.isEmpty()) { resource = new ResourceLocation(baseResource); }
        else { resource = null; }
    }

    public static String normalizeSuffix(String suffixIn) {
        String s = suffixIn == null ? "" : suffixIn.toLowerCase().trim();
        if (!s.isEmpty() && !s.startsWith(".")) { s = "." + s; }
        return s;
    }

    public static boolean isLoading(String suffixIn) { return loadingSuffixes.contains(normalizeSuffix(suffixIn)); }

    /** Scans every asset source for a suffix on background threads. Safe to call from the main thread at any time. */
    public static void preload(String suffixIn) {
        String key = normalizeSuffix(suffixIn);
        if (key.isEmpty() || resourcesData.containsKey(key) || !loadingSuffixes.add(key)) { return; }
        List<ResourceLocation> registered = key.equals(".png") ? collectRegisteredTextures() : Collections.emptyList();
        Thread thread = new Thread(() -> {
            Map<String, TreeMap<ResourceLocation, Long>> map = new TreeMap<>();
            try {
                for (ResourceLocation location : registered) { addFile(map, key, location, resourceSize(location)); }
                scanSources(map, key);
                resourcesData.put(key, map);
            }
            catch (Exception e) { LogWriter.error(e); }
            finally { loadingSuffixes.remove(key); }
        }, "CustomNpcs Resources " + key);
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private static List<ResourceLocation> collectRegisteredTextures() {
        Minecraft mc = Minecraft.getMinecraft();
        List<ResourceLocation> list = new ArrayList<>();
        try { list.addAll(((ITextureManagerMixin) mc.getTextureManager()).getMapTextureObjects().keySet()); }
        catch (Exception ignored) { }
        try {
            for (String key : new ArrayList<>(((ITextureMapMixin) mc.getTextureMapBlocks()).getMapRegisteredSprites().keySet())) {
                try { list.add(new ResourceLocation(key.substring(0, key.indexOf(":")), "textures/" + key.substring(key.indexOf(":") + 1) + ".png")); }
                catch (Exception ignored) { }
            }
        }
        catch (Exception ignored) { }
        return list;
    }

    private static long resourceSize(ResourceLocation location) {
        try (java.io.InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream()) { return is.available(); }
        catch (Exception ignored) { return 0L; }
    }

    private static void scanSources(Map<String, TreeMap<ResourceLocation, Long>> out, String suffix) {
        List<File> sources = new ArrayList<>();
        for (ModContainer mod : Loader.instance().getModList()) {
            if (mod.getSource().exists()) { sources.add(mod.getSource()); }
        }
        ResourcePackRepository repos = Minecraft.getMinecraft().getResourcePackRepository();
        for (ResourcePackRepository.Entry entry : repos.getRepositoryEntries()) {
            File file = new File(repos.getDirResourcepacks(), entry.getResourcePackName());
            if (file.exists()) { sources.add(file); }
        }
        sources.add(new File(CustomNpcs.Dir, "assets"));

        int threads = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 1));
        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "CustomNpcs Resource Scan");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        List<Future<Map<String, TreeMap<ResourceLocation, Long>>>> futures = new ArrayList<>();
        for (File source : sources) {
            futures.add(pool.submit(() -> {
                Map<String, TreeMap<ResourceLocation, Long>> local = new TreeMap<>();
                progressFile(local, suffix, source);
                return local;
            }));
        }
        pool.shutdown();
        for (Future<Map<String, TreeMap<ResourceLocation, Long>>> future : futures) {
            try { merge(out, future.get()); }
            catch (Exception e) { LogWriter.error(e); }
        }
    }

    private static void merge(Map<String, TreeMap<ResourceLocation, Long>> out, Map<String, TreeMap<ResourceLocation, Long>> in) {
        for (Map.Entry<String, TreeMap<ResourceLocation, Long>> entry : in.entrySet()) {
            TreeMap<ResourceLocation, Long> target = out.get(entry.getKey());
            if (target == null) { out.put(entry.getKey(), entry.getValue()); }
            else { for (Map.Entry<ResourceLocation, Long> file : entry.getValue().entrySet()) { target.putIfAbsent(file.getKey(), file.getValue()); } }
        }
    }

    private static void progressFile(Map<String, TreeMap<ResourceLocation, Long>> map, String suffix, File file) {
        try {
            if (!file.isDirectory() && (file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))) {
                ZipFile zip = new ZipFile(file);
                try {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry zipentry = entries.nextElement();
                        String entryName = zipentry.getName();
                        if (entryName.contains("assets")) { addFile(map, suffix, entryName, zipentry.getSize()); }
                    }
                }
                finally { zip.close(); }
            } else if (file.isDirectory()) {
                checkFolder(map, suffix, file);
            }
        } catch (Exception e) { LogWriter.error(e); }
    }

    private static void checkFolder(Map<String, TreeMap<ResourceLocation, Long>> map, String suffix, File file) {
        if (file == null) { return; }
        File[] files = file.listFiles();
        if (files == null) { return; }
        for (File f : files) {
            if (f.isDirectory()) { checkFolder(map, suffix, f); }
            else { addFile(map, suffix, f.getAbsolutePath(), f.length()); }
        }
    }

    private static void addFile(Map<String, TreeMap<ResourceLocation, Long>> map, String suffix, ResourceLocation location, long size) {
        if (!suffix.isEmpty() && !location.getResourcePath().toLowerCase().endsWith(suffix)) { return; }
        TreeMap<ResourceLocation, Long> domain = map.get(location.getResourceDomain());
        if (domain == null) { map.put(location.getResourceDomain(), domain = new TreeMap<>()); }
        domain.putIfAbsent(location, size);
    }

    private static void addFile(Map<String, TreeMap<ResourceLocation, Long>> map, String suffix, String path, long size) {
        if (path == null || !path.contains("assets")) { return; }
        if (!suffix.isEmpty() && !path.toLowerCase().endsWith(suffix)) { return; }
        path = path.replace('\\', '/');
        path = path.substring(path.lastIndexOf("assets") + 7);
        int split = path.indexOf("/");
        if (split <= 0) { return; }
        try { addFile(map, suffix, new ResourceLocation(path.substring(0, split), path.substring(split + 1)), size); }
        catch (Exception ignored) { }
    }

    /** Fills {@link #data} from the shared cache, kicking off a background scan when nothing is cached yet. */
    protected void loadFiles() {
        Map<String, TreeMap<ResourceLocation, Long>> cache = resourcesData.get(suffix);
        if (cache != null) { data.putAll(cache); }
        else { preload(suffix); }
    }

    protected void resetFiles() {
        data.clear();
        if (suffix.isEmpty()) { return; }
        Map<String, TreeMap<ResourceLocation, Long>> map = new TreeMap<>();
        if (suffix.equals(".png")) {
            for (ResourceLocation location : collectRegisteredTextures()) { addFile(map, suffix, location, resourceSize(location)); }
        }
        scanSources(map, suffix);
        data.putAll(map);
        resourcesData.put(suffix, map);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!data.isEmpty()) { return; }
        Map<String, TreeMap<ResourceLocation, Long>> cache = resourcesData.get(suffix);
        if (cache != null && !cache.isEmpty()) {
            data.putAll(cache);
            initGui();
        }
    }

    protected void addPath(Path path) {
        if (path == null) { return; }
        String p = path.toString();
        if (!p.startsWith("assets")) { return; }
        p = p.substring(7);
        if (!p.contains("/")) { return; }
        //ResourceLocation location = new ResourceLocation()
        long size = 0L;
        try  {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            size = attrs.size();
        }
        catch (Exception e) { LogWriter.info("Error create png file path: "+e); }
        try  {
            ResourceLocation location = new ResourceLocation(p.substring(0, p.indexOf("/")), p.substring(p.indexOf("/") + 1));
            if (!suffix.isEmpty() && !location.getResourcePath().toLowerCase().endsWith(suffix.toLowerCase())) { return; }
            if (!data.containsKey(location.getResourceDomain())) { data.put(location.getResourceDomain(), new TreeMap<>()); }
            else {
                for (ResourceLocation r : data.get(location.getResourceDomain()).keySet()) {
                    if (r.getResourcePath().equals(location.getResourcePath())) { return; }
                }
            }
            data.get(location.getResourceDomain()).put(location, size);
        }
        catch (Exception ignored) {}
    }

    protected void addFile(ResourceLocation location) {
        String path = location.getResourcePath();
        if (!suffix.isEmpty() && !path.toLowerCase().endsWith(suffix.toLowerCase())) { return; }
        String domain = location.getResourceDomain();
        if (!data.containsKey(domain)) { data.put(domain, new TreeMap<>()); }
        else {
            for (ResourceLocation r : data.get(domain).keySet()) {
                if (r.getResourcePath().equals(path)) { return; }
            }
        }
        long size = 0L;
        try { size = minecraft.getResourceManager().getResource(location).getInputStream().available(); }
        catch (Exception ignored) { }
        data.get(domain).put(location, size);
    }

    public ResourceSelection setOffsetX(int posX) {
        offsetX = posX;
        return this;
    }

}
