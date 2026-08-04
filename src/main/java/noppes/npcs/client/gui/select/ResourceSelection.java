package noppes.npcs.client.gui.select;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.resource.DelegatingPackResources;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.client.renderer.texture.ITextureAtlasMixin;
import noppes.npcs.mixin.client.renderer.texture.ITextureManagerMixin;
import noppes.npcs.mixin.minecraftforge.resource.IDelegatingPackResourcesMixin;
import noppes.npcs.mixin.server.packs.IFilePackResourcesMixin;
import noppes.npcs.mixin.server.packs.IVanillaPackResourcesMixin;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.GuiBasicContainer;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class ResourceSelection
        extends GuiNPCInterface
        implements ICustomScrollListener {

    public static Map<String, Map<String, TreeMap<ResourceLocation, Long>>> resourcesData = new HashMap<>();
    private static final Set<String> loadingSuffixes = ConcurrentHashMap.newKeySet();

    protected final Map<String, TreeMap<ResourceLocation, Long>> data = new TreeMap<>(); // (Directory, Files)
    protected final Screen parent;
    protected final MutableComponent back;
    protected ResourceLocation selectDir;
    protected GuiCustomScrollNop scroll;
    protected String suffix;
    protected String baseResource = "";
    protected int offsetX = 0;
    protected int scrollWidth;
    protected Component select = Component.empty();

    public final int id;
    public ResourceLocation resource;

    public ResourceSelection(Screen parentIn, int idIn, EntityNPCInterface npcIn, @Nonnull String startIn, String suffixIn) {
        super(npcIn);
        drawDefaultBackground = false;
        setBackground("menubg.png");
        imageWidth = 366;
        imageHeight = 226;
        scrollWidth = imageWidth - 10;

        id = idIn;
        parent = parentIn;
        suffix = suffixIn.toLowerCase();
        back = Component.literal("   " + Character.toChars(0x2190)[0] + " (")
                .append(Component.translatable("gui.back")).append(Component.literal(")"))
                .withStyle(ChatFormatting.GOLD);

        if (resourcesData.containsKey(suffix)) { data.putAll(resourcesData.get(suffix)); }

        selectDir = null;
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        ResourceLocation loc = new ResourceLocation(startIn);
        if (data.containsKey(loc.getNamespace()) && !data.get(loc.getNamespace()).containsKey(loc)) {
            try {
                if (!startIn.isEmpty()) { minecraft.getTextureManager().getTexture(loc); }
            }
            catch (Exception ignored) { }
        }
        if (!data.containsKey(loc.getNamespace())) {
            loadFiles();
        }
        baseResource = startIn;
        if (!startIn.isEmpty()) {
            resource = new ResourceLocation(startIn);
            if (startIn.lastIndexOf("/") != -1) {
                startIn = startIn.substring(0, startIn.lastIndexOf("/"));
            }
            selectDir = new ResourceLocation(startIn);
            if (!data.containsKey(selectDir.getNamespace())) {
                selectDir = null;
                return;
            }
            for (ResourceLocation r : data.get(selectDir.getNamespace()).keySet()) {
                if (r.getPath().indexOf(selectDir.getPath()) == 0) { return; }
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
    public void init() {
        super.init();
        guiLeft += offsetX;
        int h = guiTop + imageHeight - 25;
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        addButton(2, guiLeft + 296, h, "gui.done")
                .setSize(65, 20)
                .setHoverTexts("selection.hover.done");
        addButton(1, guiLeft + 5, h, "gui.cancel")
                .setSize(65, 20)
                .setHoverTexts("hover.back");
        if (scroll == null) { scroll = addScroll(0).setSize(scrollWidth, 180); }
        Component domain = Component.literal("All Data in Game (mods)/");
        if (selectDir == null) { scroll.setList(new ArrayList<>(data.keySet())); }
        else {
            List<Component> list = new ArrayList<>();
            Map<String, Long> ds = new TreeMap<>();
            Map<String, Long> fs = new TreeMap<>();
            String path = selectDir.getPath();
            TreeMap<ResourceLocation, Long> files = data.getOrDefault(selectDir.getNamespace(), new TreeMap<>());
            for (ResourceLocation res : files.keySet()) {
                if (!res.getPath().contains("/")) {
                    fs.put(res.getPath(), files.get(res));
                }
                else if (res.getPath().indexOf(path) == 0) {
                    String key = res.getPath().substring(path.length() + 1);
                    if (key.contains("/")) {
                        ds.put(key.substring(0, key.indexOf("/")), data.get(selectDir.getNamespace()).get(res));
                    } else if ((suffix.isEmpty() || res.getPath().toLowerCase().endsWith(suffix))) {
                        fs.put(res.getPath().substring(res.getPath().lastIndexOf("/") + 1), data.get(selectDir.getNamespace()).get(res));
                    }
                }
            }
            String txrName = resource != null ? resource.getPath() : "";
            if (!txrName.isEmpty()) {
                txrName = txrName.substring(txrName.lastIndexOf("/") + 1);
            }
            List<Component> suffixes = new ArrayList<>();
            int i = 1, pos = -1;
            suffixes.add(Component.empty());
            for (String key : ds.keySet()) {
                suffixes.add(Component.empty());
                MutableComponent line = Component.literal(key).withStyle(ChatFormatting.GOLD);
                list.add(line);
                i++;
            }
            for (String key : fs.keySet()) {
                if (fs.get(key) == 0L) { suffixes.add(Component.empty()); }
                else {
                    suffixes.add(Component.literal(Util.instance.getTextReducedNumber(fs.get(key), false, false, true) + "b"));
                }
                MutableComponent line = Component.literal(key);
                line.withStyle(line.getStyle().withColor(0xCAEAEA));
                list.add(line);
                if (txrName.equals(key)) {
                    pos = i;
                }
                i++;
            }
            list.add(0, back);
            scroll.setUnsortedList(list).setSuffixes(suffixes);
            if (scroll.getHover() != pos) { scroll.setSelected(pos); }
            domain = Component.empty().append(Component.literal(selectDir.getNamespace() + "/" + path));
            while (minecraft.font.width(domain) > 250 && path.contains("/")) {
                path = path.substring(path.indexOf("/") + 1);
                domain = Component.empty().append(Component.literal(selectDir.getNamespace() + "/.../" + path));
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
    public boolean keyPressed(int key, int key_1, int key_2) {
        if (shouldCloseOnEsc() && GuiBasic.isEscKey(key)) { cancel(); }
        if (scroll != null && scroll.getSearchValue().isEmpty() &&
                key == InputConstants.KEY_BACKSPACE) {
            List<String> list = scroll.getList();
            if (!list.isEmpty() && list.get(0).equals(back.getString())) {
                if (selectDir != null) {
                    if (!selectDir.getPath().contains("/")) { selectDir = null; }
                    else { selectDir = new ResourceLocation(selectDir.getNamespace(), selectDir.getPath().substring(0, selectDir.getPath().lastIndexOf("/"))); }
                    init();
                    return true;
                }
            }
        }
        return super.keyPressed(key, key_1, key_2);
    }

    @Override
    public void scrollClicked(GuiCustomScrollNop scroll) {
        if (scroll.getNormalSelected().equals(back)) {
            if (selectDir == null) { return; }
            if (!selectDir.getPath().contains("/")) { selectDir = null; }
            else { selectDir = new ResourceLocation(selectDir.getNamespace(), selectDir.getPath().substring(0, selectDir.getPath().lastIndexOf("/"))); }
            init();
        }
        else if (selectDir != null) {
            if (!scroll.getSelected().endsWith(suffix)) {
                if (suffix.equals(".ogg")) { resource = new ResourceLocation(selectDir.getNamespace(), scroll.getSelected()); }
                else {
                    selectDir = new ResourceLocation(selectDir.getNamespace(), selectDir.getPath() + "/" + scroll.getSelected());
                    init();
                }
            } else {
                resource = new ResourceLocation(selectDir.getNamespace(), selectDir.getPath() + "/" + scroll.getSelected());
            }
        }
        else if (data.containsKey(scroll.getSelected())) {
            String res = null, def = null;
            for (ResourceLocation loc : data.get(scroll.getSelected()).keySet()) {
                if (def == null) {
                    if (loc.getPath().contains("/")) { def = loc.getPath().substring(0, loc.getPath().indexOf("/")); }
                    else { def = loc.getPath(); }
                }
                if (loc.getPath().contains("/") &&
                        loc.getPath().substring(0, loc.getPath().indexOf("/")).equals("textures")) {
                    res = "textures";
                    break;
                }
            }
            if (res == null) { res = def; }
            if (res != null) { selectDir = new ResourceLocation(scroll.getSelected(), res); }
            init();
        }
    }

    @Override
    public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
        if (resource != null) {
            onClose();
            if (parent instanceof GuiBasic gui) { gui.init(); }
            else if (parent instanceof GuiBasicContainer<?> gui) { gui.init(); }
        }
    }

    public static String normalizeSuffix(String suffixIn) {
        String s = suffixIn == null ? "" : suffixIn.toLowerCase().trim();
        if (!s.isEmpty() && !s.startsWith(".")) {
            s = "." + s;
        }
        return s;
    }

    public static boolean isLoading(String suffixIn) {
        return loadingSuffixes.contains(normalizeSuffix(suffixIn));
    }

    public static void preload(String suffixIn) {
        String key = normalizeSuffix(suffixIn);
        if (key.isEmpty() || resourcesData.containsKey(key) || !loadingSuffixes.add(key)) { return; }

        List<ResourceLocation> registered = key.equals(".png")
                ? collectRegisteredTextures()
                : Collections.emptyList();

        Thread thread = new Thread(() -> {
            Map<String, TreeMap<ResourceLocation, Long>> map = new TreeMap<>();
            try {
                for (ResourceLocation location : registered) {
                    addFile(map, key, location, resourceSize(location));
                }
                scanSources(map, key);
                resourcesData.put(key, map);
            } catch (Exception e) {
                LogWriter.error(e);
            } finally {
                loadingSuffixes.remove(key);
            }
        }, "CustomNpcs Resources " + key);
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private static List<ResourceLocation> collectRegisteredTextures() {
        Minecraft mc = Minecraft.getInstance();
        List<ResourceLocation> list = new ArrayList<>();
        try {
            list.addAll(((ITextureManagerMixin) mc.getTextureManager()).getByPath().keySet());
        } catch (Exception ignored) {
        }
        try {
            Map<ResourceLocation, TextureAtlasSprite> texturesByName =
                    ((ITextureAtlasMixin) mc.getModelManager()
                            .getAtlas(new ResourceLocation("minecraft", "textures/atlas/blocks.png")))
                            .getTexturesByName();
            for (ResourceLocation key : texturesByName.keySet()) {
                try {
                    list.add(new ResourceLocation(key.getNamespace(),
                            "textures/" + key.getPath() + ".png"));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private static long resourceSize(ResourceLocation location) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(location);
            if (res.isPresent()) {
                try (InputStream is = res.get().open()) {
                    return is.available();
                }
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    private static void scanSources(Map<String, TreeMap<ResourceLocation, Long>> out, String suffix) {
        List<Callable<Map<String, TreeMap<ResourceLocation, Long>>>> tasks = new ArrayList<>();
        /* Mod jars */
        for (IModInfo mod : ModList.get().getMods()) {
            Optional<? extends ModContainer> modContainer = ModList.get().getModContainerById(mod.getModId());
            modContainer.ifPresent(container -> {
                Path modPath = modContainer.get().getModInfo().getOwningFile().getFile().getFilePath();
                tasks.add(() -> {
                    Map<String, TreeMap<ResourceLocation, Long>> local = new TreeMap<>();
                    progressPath(local, suffix, modPath);
                    return local;
                });
            });
        }
        /* Resource packs */
        PackRepository repos = Minecraft.getInstance().getResourcePackRepository();
        for (Pack pack : repos.getSelectedPacks()) {
            if (pack == null) { continue; }
            try {
                PackResources packResources = pack.open();
                tasks.add(() -> {
                    Map<String, TreeMap<ResourceLocation, Long>> local = new TreeMap<>();
                    scanPackResources(local, suffix, packResources);
                    return local;
                });
            } catch (Exception ignored) {
            }
        }
        /* Custom mod resources */
        tasks.add(() -> {
            Map<String, TreeMap<ResourceLocation, Long>> local = new TreeMap<>();
            checkFolder(local, suffix, new File(CustomNpcs.Dir, "assets"));
            return local;
        });

        int threads = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 1));
        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "CustomNpcs Resource Scan");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        List<Future<Map<String, TreeMap<ResourceLocation, Long>>>> futures = new ArrayList<>();
        for (Callable<Map<String, TreeMap<ResourceLocation, Long>>> task : tasks) { futures.add(pool.submit(task)); }
        pool.shutdown();

        for (Future<Map<String, TreeMap<ResourceLocation, Long>>> future : futures) {
            try { merge(out, future.get()); }
            catch (Exception e) { LogWriter.error(e); }
        }
    }

    private static void merge(Map<String, TreeMap<ResourceLocation, Long>> out,
                              Map<String, TreeMap<ResourceLocation, Long>> in) {
        for (Map.Entry<String, TreeMap<ResourceLocation, Long>> entry : in.entrySet()) {
            TreeMap<ResourceLocation, Long> target = out.get(entry.getKey());
            if (target == null) {
                out.put(entry.getKey(), entry.getValue());
            } else {
                for (Map.Entry<ResourceLocation, Long> file : entry.getValue().entrySet()) {
                    target.putIfAbsent(file.getKey(), file.getValue());
                }
            }
        }
    }

    private static void addFile(Map<String, TreeMap<ResourceLocation, Long>> map,
                                String suffix, ResourceLocation location, long size) {
        if (!suffix.isEmpty() && !location.getPath().toLowerCase().endsWith(suffix)) {
            return;
        }
        TreeMap<ResourceLocation, Long> domain = map.computeIfAbsent(location.getNamespace(), k -> new TreeMap<>());
        domain.putIfAbsent(location, size);
    }

    private static void addFile(Map<String, TreeMap<ResourceLocation, Long>> map,
                                String suffix, String path, long size) {
        if (path == null || !path.contains("assets")) {
            return;
        }
        if (!suffix.isEmpty() && !path.toLowerCase().endsWith(suffix)) {
            return;
        }
        path = path.replace('\\', '/');
        path = path.substring(path.lastIndexOf("assets") + 7);
        int split = path.indexOf("/");
        if (split <= 0) {
            return;
        }
        try {
            addFile(map, suffix,
                    new ResourceLocation(path.substring(0, split), path.substring(split + 1)),
                    size);
        } catch (Exception ignored) {
        }
    }


    private static void scanPackResources(Map<String, TreeMap<ResourceLocation, Long>> map,
                                          String suffix,
                                          PackResources packResources) {
        if (packResources instanceof IFilePackResourcesMixin filePack) {
            progressFile(map, suffix, filePack.getFile());
            try {
                checkZipFile(map, suffix, filePack.getZipFile());
            } catch (Exception ignored) {
            }
        } else if (packResources instanceof DelegatingPackResources delegatingPack) {
            Map<String, List<PackResources>> namespaces =
                    ((IDelegatingPackResourcesMixin) delegatingPack).getNamespacesAssets();
            for (List<PackResources> list : namespaces.values()) {
                for (PackResources packRes : list) {
                    scanPackResources(map, suffix, packRes);
                }
            }
        } else if (packResources instanceof VanillaPackResources vanillaPack) {
            Map<PackType, List<Path>> pathsForType =
                    ((IVanillaPackResourcesMixin) vanillaPack).getPathsForType();
            List<Path> paths = pathsForType.get(PackType.CLIENT_RESOURCES);
            if (paths != null) {
                for (Path path : paths) {
                    progressPath(map, suffix, path);
                }
            }
        }
    }

    private static void progressPath(Map<String, TreeMap<ResourceLocation, Long>> map,
                                     String suffix,
                                     Path path) {
        if (path == null) return;
        try {
            progressFile(map, suffix, path.toFile());
            return;
        } catch (Throwable ignored) {
        }
        if (!Files.exists(path)) return;
        Set<Path> allPaths = new HashSet<>();
        try (Stream<Path> stream = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) {
            stream.filter(Files::isRegularFile).forEach(allPaths::add);
        } catch (Throwable ignored) {
            return;
        }
        for (Path p : allPaths) {
            addPath(map, suffix, p);
        }
    }

    private static void addPath(Map<String, TreeMap<ResourceLocation, Long>> map,
                                String suffix,
                                Path path) {
        if (path == null) return;
        String p = path.toString();
        if (!p.startsWith("assets")) return;
        p = p.substring(7);
        if (!p.contains("/")) return;

        long size = 0L;
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            size = attrs.size();
        } catch (Exception e) {
            LogWriter.info("Error create png file path: " + e);
        }

        try {
            ResourceLocation location = new ResourceLocation(
                    p.substring(0, p.indexOf("/")),
                    p.substring(p.indexOf("/") + 1));
            if (!suffix.isEmpty() && !location.getPath().toLowerCase().endsWith(suffix.toLowerCase())) return;

            TreeMap<ResourceLocation, Long> domain = map.get(location.getNamespace());
            if (domain == null) {
                map.put(location.getNamespace(), domain = new TreeMap<>());
            } else {
                for (ResourceLocation r : domain.keySet()) {
                    if (r.getPath().equals(location.getPath())) return;
                }
            }
            domain.put(location, size);
        } catch (Exception ignored) {}
    }

    private static void checkZipFile(Map<String, TreeMap<ResourceLocation, Long>> map,
                                     String suffix,
                                     ZipFile zip) throws IOException {
        if (zip == null) return;
        try (zip) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipentry = entries.nextElement();
                String entryName = zipentry.getName();
                if (entryName.contains("assets")) {
                    addFile(map, suffix, entryName, zipentry.getSize());
                }
            }
        }
    }

    private static void progressFile(Map<String, TreeMap<ResourceLocation, Long>> map,
                                     String suffix,
                                     File file) {
        try {
            if (!file.isDirectory() && (file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))) {
                checkZipFile(map, suffix, new ZipFile(file));
            } else if (file.isDirectory()) {
                checkFolder(map, suffix, file);
            }
        } catch (Exception e) {
            LogWriter.error(e);
        }
    }

    private static void checkFolder(Map<String, TreeMap<ResourceLocation, Long>> map,
                                    String suffix,
                                    File file) {
        if (file == null) return;
        File[] files = file.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                checkFolder(map, suffix, f);
            } else {
                addFile(map, suffix, f.getAbsolutePath(), f.length());
            }
        }
    }

    protected void loadFiles() {
        Map<String, TreeMap<ResourceLocation, Long>> cache = resourcesData.get(suffix);
        if (cache != null) { data.putAll(cache); }
        else { preload(suffix); }
    }

    @Override
    public void tick() {
        super.tick();
        if (!data.isEmpty()) { return; }
        Map<String, TreeMap<ResourceLocation, Long>> cache = resourcesData.get(suffix);
        if (cache != null && !cache.isEmpty()) {
            data.putAll(cache);
            init();
        }
    }

    protected void cancel() {
        if (!baseResource.isEmpty()) { resource = new ResourceLocation(baseResource); }
        else { resource = null; }
    }

    protected void resetFiles() {
        data.clear();
        if (suffix.isEmpty()) { return; }
        if (minecraft == null) { minecraft = Minecraft.getInstance(); }
        if (suffix.equals(".png")) {
            /* Texture manager data */
            for (ResourceLocation key : ((ITextureManagerMixin) minecraft.getTextureManager()).getByPath().keySet()) {
                addFile(key);
            }
            /* Texture blocks data */
            Map<ResourceLocation, TextureAtlasSprite> texturesByName = ((ITextureAtlasMixin) minecraft.getModelManager()
                    .getAtlas(new ResourceLocation("minecraft", "textures/atlas/blocks.png"))).getTexturesByName();
            for (ResourceLocation key : texturesByName.keySet()) {
                addFile(new ResourceLocation(key.getNamespace(), "textures/" + key.getPath() + ".png"));
            }
        }
        /* Mod jars */
        for (IModInfo mod : ModList.get().getMods()) {
            Optional<? extends ModContainer> modContainer = ModList.get().getModContainerById(mod.getModId());
            modContainer.ifPresent(container -> progressPath(container.getModInfo().getOwningFile().getFile().getFilePath()));
        }
        /* Resource packs */
        PackRepository repos = Minecraft.getInstance().getResourcePackRepository();
        for (Pack pack : repos.getSelectedPacks()) {
            if (pack == null) { continue; }
            progressPackResources(pack.open());
        }
        /* Custom mod resources */
        checkFolder(new File(CustomNpcs.Dir, "assets"));
    }

    protected void progressPackResources(PackResources packResources) {
        if (packResources instanceof IFilePackResourcesMixin filePack) {
            progressFile(filePack.getFile());
            try { checkZipFile(filePack.getZipFile()); } catch (Exception ignored) { }
        }
        else if (packResources instanceof DelegatingPackResources delegatingPack) {
            Map<String, List<PackResources>> map = ((IDelegatingPackResourcesMixin) delegatingPack).getNamespacesAssets();
            for (String mod : map.keySet()) {
                for (PackResources packRes : map.get(mod)) { progressPackResources(packRes); }
            }
        }
        else if (packResources instanceof VanillaPackResources vanillaPack) {
            for (Path path : ((IVanillaPackResourcesMixin) vanillaPack).getPathsForType().get(PackType.CLIENT_RESOURCES)) { progressPath(path); }
        }
    }

    protected void progressPath(Path path) {
        if (path == null) { return; }
        try {
            progressFile(path.toFile());
            return;
        }
        catch (Throwable ignored) { }
        if (!Files.exists(path)) { return; }
        Set<Path> allPaths = new HashSet<>();
        try (Stream<Path> stream = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) { stream.filter(Files::isRegularFile).forEach(allPaths::add); }
        catch (Throwable ignored) { return; }
        for (Path p : allPaths) { addPath(p); }
    }

    protected void progressFile(File file) {
        try {
            if (!file.isDirectory() && (file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))) { checkZipFile(new ZipFile(file)); }
            else if (file.isDirectory()) { checkFolder(file); }
        } catch (Exception e) { LogWriter.error("Error:", e); }
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
            if (!suffix.isEmpty() && !location.getPath().toLowerCase().endsWith(suffix.toLowerCase())) { return; }
            if (!data.containsKey(location.getNamespace())) { data.put(location.getNamespace(), new TreeMap<>()); }
            else {
                for (ResourceLocation r : data.get(location.getNamespace()).keySet()) {
                    if (r.getPath().equals(location.getPath())) { return; }
                }
            }
            data.get(location.getNamespace()).put(location, size);
        }
        catch (Exception ignored) {}
    }

    protected void addFile(ResourceLocation location) {
        String path = location.getPath();
        if (!suffix.isEmpty() && !path.toLowerCase().endsWith(suffix.toLowerCase())) { return; }
        String domain = location.getNamespace();
        if (!data.containsKey(domain)) { data.put(domain, new TreeMap<>()); }
        else {
            for (ResourceLocation r : data.get(domain).keySet()) {
                if (r.getPath().equals(path)) { return; }
            }
        }
        long size = 0L;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(location);
            if (res.isPresent()) {
                Resource stream = res.get();
                try (InputStream inputStream = stream.open()) { size = inputStream.available(); }
            }
        }
        catch (Exception ignored) { }
        data.get(domain).put(location, size);
    }

    private void addFile(String path, long size) {
        if (path == null || !path.contains("assets") || (!suffix.isEmpty() && !path.toLowerCase().endsWith(suffix.toLowerCase()))) { return; }
        if (path.contains("\\")) {
            List<String> list = new ArrayList<>();
            while (path.contains("\\")) {
                list.add(path.substring(0, path.indexOf("\\")));
                path = path.substring(path.indexOf("\\") + 1);
            }
            list.add(path);
            StringBuilder pathBuilder = new StringBuilder();
            for (String p : list) {
                pathBuilder.append(p).append("/");
            }
            path = pathBuilder.toString();
            path = path.substring(0, path.length() - 1);
        }
        path = path.substring(path.lastIndexOf("assets") + 7);
        String domain = path.substring(0, path.indexOf("/"));
        if (domain.isEmpty()) { return; }
        path = path.substring(path.indexOf("/") + 1);
        ResourceLocation res = new ResourceLocation(domain, path);
        if (!data.containsKey(domain)) {
            data.put(domain, new TreeMap<>());
        } else {
            for (ResourceLocation r : data.get(domain).keySet()) {
                if (r.getPath().equals(path)) { return; }
            }
        }
        data.get(domain).put(res, size);
    }

    private void checkZipFile(ZipFile zip) throws IOException {
        if (zip == null) { return; }
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipentry = entries.nextElement();
            String entryName = zipentry.getName();
            int a = entryName.indexOf("assets");
            int t = entryName.indexOf("texture", a);
            if (a != -1 && t != -1) {
                addFile(entryName, zipentry.getSize());
            }
        }
        zip.close();
    }

    private void checkFolder(File file) {
        if (file != null) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        checkFolder(f);
                        continue;
                    }
                    addFile(f.getAbsolutePath(), f.length());
                }
            }
        }
    }

    public ResourceSelection setOffsetX(int posX) {
        offsetX = posX;
        return this;
    }

}
