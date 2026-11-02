package noppes.npcs.mixin.minecraftforge.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.client.gui.widget.ModListWidget;
import net.minecraftforge.common.ForgeI18n;
import net.minecraftforge.common.util.MavenVersionStringHelper;
import net.minecraftforge.common.util.Size2i;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.resource.ResourcePackLoader;
import noppes.npcs.CustomNpcs;
import noppes.npcs.mixin.minecraftforge.client.gui.widget.IScrollPanelMixin;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Mixin(value = ModListScreen.class, priority = 498, remap = false)
public class ModListScreenMixin {

    @Shadow private ModListWidget.ModEntry selected = null;
    @Shadow private Button configButton;

    @Unique private Field npcs$modInfo;
    @Unique private Method npcs$setInfo;
    @Unique private Method npcs$clearInfo;

    @Inject(method = "updateCache", at = @At("HEAD"), cancellable = true)
    public void npcs$updateCache(CallbackInfo ci) {
        ModListScreen parent = (ModListScreen) (Object) this;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (npcs$modInfo == null) { npcs$modInfo = ModListScreen.class.getDeclaredField("modInfo"); }
            if (npcs$setInfo == null) {
                for (Class<?> subclass : ModListScreen.class.getDeclaredClasses()) {
                    if (subclass.getSimpleName().equals("InfoPanel")) {
                        npcs$setInfo = subclass.getDeclaredMethod("setInfo", List.class, ResourceLocation.class, Size2i.class);
                        break;
                    }
                }
            }
            if (npcs$clearInfo == null) {
                for (Class<?> subclass : ModListScreen.class.getDeclaredClasses()) {
                    if (subclass.getSimpleName().equals("InfoPanel")) {
                        npcs$clearInfo = subclass.getDeclaredMethod("clearInfo");
                        break;
                    }
                }
            }
            Object modInfo = npcs$modInfo.get(parent);
            if (selected == null) {
                configButton.active = false;
                if (npcs$clearInfo != null) { npcs$clearInfo.invoke(modInfo); }
                ci.cancel();
                return;
            }

            IModInfo selectedMod = selected.getInfo();

            configButton.active = ConfigScreenHandler.getScreenFactoryFor(selectedMod).isPresent();
            List<String> lines = new ArrayList<>();
            VersionChecker.CheckResult verCheck = VersionChecker.getResult(selectedMod);

            Pair<ResourceLocation, Size2i> logoData = selectedMod.getLogoFile()
                    .map(logoFile-> {
                        TextureManager tm = minecraft.getTextureManager();
                        final PathPackResources resourcePack = ResourcePackLoader.getPackFor(selectedMod.getModId())
                                .orElse(ResourcePackLoader.getPackFor("forge").
                                        orElseThrow(()->new RuntimeException("Can't find forge, WHAT!")));
                        try {
                            NativeImage logo = null;
                            IoSupplier<InputStream> logoResource = resourcePack.getRootResource(logoFile);
                            if (logoResource != null) { logo = NativeImage.read(logoResource.get()); }
                            if (logo != null) {
                                return Pair.of(tm.register("modlogo", new DynamicTexture(logo) {
                                            @Override
                                            public void upload() {
                                                bind();
                                                NativeImage td = getPixels();
                                                if (td != null) {
                                                    // Use custom "blur" value which controls texture filtering (nearest-neighbor vs linear)
                                                    td.upload(0, 0, 0, 0, 0,
                                                            td.getWidth(), td.getHeight(), selectedMod.getLogoBlur(),
                                                            false, false, false);
                                                }
                                            }
                                        }),
                                        new Size2i(logo.getWidth(), logo.getHeight()));
                            }
                        }
                        catch (IOException ignored) { }
                        return Pair.<ResourceLocation, Size2i>of(null, new Size2i(0, 0));})
                    .orElse(Pair.of(null, new Size2i(0, 0)));

            String displayName = selectedMod.getDisplayName();
            String colorChar = ((char) 65533) + "";
            if (displayName.contains(colorChar)) {
                displayName = displayName.replace(colorChar, ((char) 167) + "");
            }
            lines.add(displayName);

            lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.version",
                    MavenVersionStringHelper.artifactVersionToString(selectedMod.getVersion())));

            String language = "en_us";
            LanguageManager lm = Minecraft.getInstance().getLanguageManager();
            if (lm != null) { language = lm.getSelected(); }
            if (!language.equals("en_us")) {
                if (language.contains("_")) {
                    if (language.equals("zh_cn")) { language = "zh_CN"; }
                    else if (language.equals("zh_tw")) { language = "zh_TW"; }
                    else { language = language.substring(0, language.indexOf("_")); }
                }
            }

            String tempKey = "|||";
            String idAndState = ForgeI18n.parseMessage("fml.menu.mods.info.idstate",
                    ChatFormatting.BLUE + selectedMod.getModId() + tempKey, ChatFormatting.GOLD + ModList.get().getModContainerById(selectedMod.getModId()).
                            map(ModContainer::getCurrentState).map(Object::toString).orElse("NONE"));
            lines.add(ChatFormatting.GRAY + idAndState.substring(0, idAndState.indexOf(tempKey))); // id
            lines.add(ChatFormatting.GRAY + idAndState.substring(idAndState.indexOf(tempKey) + tempKey.length() + 1)); // state

            AtomicReference<String> credit = new AtomicReference<>("");
            selectedMod.getConfig().getConfigElement("credits")
                    .ifPresent(credits-> credit.set(ForgeI18n.parseMessage("fml.menu.mods.info.credits", credits)));
            if (!credit.get().isEmpty()) {
                String title = credit.get().substring(0, credit.get().indexOf(":") + 2);
                String names = credit.get().substring(title.length());
                if (selectedMod.getModId().equals(CustomNpcs.MODID)) { names = Util.instance.translateGoogle("auto", language, names); }
                lines.add(title + ChatFormatting.AQUA + names);
            }

            selectedMod.getConfig().getConfigElement("authors").ifPresent(authors ->
                    lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.authors", authors)));

            selectedMod.getConfig().getConfigElement("displayURL").ifPresent(displayURL ->
                    lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.displayurl", displayURL)));

            if (selectedMod.getOwningFile() == null || selectedMod.getOwningFile().getMods().size() == 1) {
                lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.nochildmods"));
            } else {
                lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.childmods",
                        selectedMod.getOwningFile().getMods().stream()
                                .map(IModInfo::getDisplayName)
                                .collect(Collectors.joining(","))));
            }

            if (verCheck.status().isOutdated()) {
                lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.updateavailable", verCheck.url() == null ? "" : verCheck.url()));
            }
            lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.license", selectedMod.getOwningFile().getLicense()));

            lines.add(null);
            String description = Component.translatable(selectedMod.getDescription()).getString();
            if (Util.instance.equalsDeleteColor(description, selectedMod.getDescription(), false)) {
                description = Util.instance.translateGoogle("auto", language, description);
            }
            lines.add(description);

            if (verCheck.status().isOutdated() && !verCheck.changes().isEmpty()) {
                lines.add(null);
                lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.changelogheader"));
                for (Map.Entry<ComparableVersion, String> entry : verCheck.changes().entrySet()) {
                    lines.add("  " + entry.getKey() + ":");
                    lines.add(entry.getValue());
                    lines.add(null);
                }
            }
            lines.add(null);

            if (npcs$setInfo != null) {
                npcs$setInfo.invoke(modInfo, lines, logoData.getLeft(), logoData.getRight());
                ci.cancel();
            }
        }
        catch (Exception e) { LogWriter.error(e); }
    }

    @Inject(method = "setSelected", at = @At("TAIL"))
    public void npcs$setSelected(ModListWidget.ModEntry entry, CallbackInfo ci) {
        try {
            if (npcs$modInfo == null) { npcs$modInfo = ModListScreen.class.getDeclaredField("modInfo"); }
            ((IScrollPanelMixin) npcs$modInfo.get(this)).setScrollDistance(-2);
        }
        catch (Exception ignored) {}
    }

}
