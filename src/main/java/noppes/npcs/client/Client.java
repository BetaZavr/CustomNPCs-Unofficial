package noppes.npcs.client;

import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.realmsclient.gui.ChatFormatting;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.internal.EntitySpawnMessageHelper;
import noppes.npcs.*;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.gui.INpcMenuGui;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.mixin.entity.player.IEntityPlayerMixin;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.api.wrapper.OverlayWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiComponentWrapper;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.controllers.OverlayController;
import noppes.npcs.client.gui.GuiAchievement;
import noppes.npcs.client.gui.GuiNpcDimension;
import noppes.npcs.client.gui.GuiNpcMobSpawnerAdd;
import noppes.npcs.client.gui.GuiNpcRemoteEditor;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.client.gui.global.*;
import noppes.npcs.client.gui.mainmenu.GuiNpcGlobalMainMenu;
import noppes.npcs.client.gui.player.*;
import noppes.npcs.client.gui.script.GuiScriptInterface;
import noppes.npcs.client.gui.select.SubGuiSoundSelection;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.client.model.animation.EmotionConfig;
import noppes.npcs.client.util.CustomTexturesPayload;
import noppes.npcs.command.CmdHeapAnalyzer;
import noppes.npcs.config.ConfigLoader;
import noppes.npcs.constants.EnumRewardType;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.containers.ContainerNPCBank;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.DimensionController;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemScripted;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.*;
import noppes.npcs.packets.server.*;
import noppes.npcs.schematics.Schematic;
import noppes.npcs.schematics.SchematicWrapper;
import noppes.npcs.shared.client.gui.listeners.*;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.shared.common.PacketBasic;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.BuilderData;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.TempFile;
import noppes.npcs.util.Util;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class Client {

    private static Minecraft minecraft;
    private static final Gson gson = new Gson();

    public static <MSG extends PacketBasic> void processPacket(MSG msg) {
        CustomNpcs.debugData.start("Packet", msg.getClass().getSimpleName());
        minecraft = Minecraft.getMinecraft();
        if (msg instanceof PacketAchievement) { packetAchievement((PacketAchievement) msg); }
        else if (msg instanceof PacketChat) { packetChat((PacketChat) msg); }
        else if (msg instanceof PacketChatBubble) { packetChatBubble((PacketChatBubble) msg); }
        else if (msg instanceof PacketConfigFont) { packetConfigFont((PacketConfigFont) msg); }
        else if (msg instanceof PacketDialog) { packetDialog((PacketDialog) msg); }
        else if (msg instanceof PacketDialogDummy) { packetDialogDummy((PacketDialogDummy) msg); }
        else if (msg instanceof PacketEyeBlink) { packetEyeBlink((PacketEyeBlink) msg); }
        else if (msg instanceof PacketGuiCloneOpen) { packetGuiCloneOpen((PacketGuiCloneOpen) msg); }
        else if (msg instanceof PacketGuiClose) { packetGuiClose((PacketGuiClose) msg); }
        else if (msg instanceof PacketGuiData) { packetGuiData((PacketGuiData) msg); }
        else if (msg instanceof PacketGuiComponentUpdate) { packetGuiComponentUpdate((PacketGuiComponentUpdate) msg); }
        else if (msg instanceof PacketGuiError) { packetGuiError((PacketGuiError) msg); }
        else if (msg instanceof PacketGuiOpen) { packetGuiOpen((PacketGuiOpen) msg); }
        else if (msg instanceof PacketGuiScrollData) { packetGuiScrollData((PacketGuiScrollData) msg); }
        else if (msg instanceof PacketGuiScrollList) { packetGuiScrollList((PacketGuiScrollList) msg); }
        else if (msg instanceof PacketGuiScrollSelected) { packetGuiScrollSelected((PacketGuiScrollSelected) msg); }
        else if (msg instanceof PacketGuiUpdate) { packetGuiUpdate(); }
        else if (msg instanceof PacketItemUpdate) { packetItemUpdate((PacketItemUpdate) msg); }
        else if (msg instanceof PacketMarkData) { packetMarkData((PacketMarkData) msg); }
        else if (msg instanceof PacketNpcDelete) { packetNpcDelete((PacketNpcDelete) msg); }
        else if (msg instanceof PacketNpcEdit) { packetNpcEdit((PacketNpcEdit) msg); }
        else if (msg instanceof PacketNpcRole) { packetNpcRole((PacketNpcRole) msg); }
        else if (msg instanceof PacketNpcUpdate) { packetNpcUpdate((PacketNpcUpdate) msg); }
        else if (msg instanceof PacketParticle) { packetParticle((PacketParticle) msg); }
        else if (msg instanceof PacketPlayMusic) { packetPlayMusic((PacketPlayMusic) msg); }
        else if (msg instanceof PacketPlaySound) { packetPlaySound((PacketPlaySound) msg); }
        else if (msg instanceof PacketQuestCompletion) { packetQuestCompletion((PacketQuestCompletion) msg); }
        else if (msg instanceof PacketSync) { packetSync((PacketSync) msg); }
        else if (msg instanceof PacketSyncRemove) { packetSyncRemove((PacketSyncRemove) msg); }
        else if (msg instanceof PacketSyncUpdate) { packetSyncUpdate((PacketSyncUpdate) msg); }
        else if (msg instanceof PacketNpcVisibleFalse) { packetNpcVisibleFalse((PacketNpcVisibleFalse) msg); }
        else if (msg instanceof PacketNpcVisibleTrue) { packetNpcVisibleTrue((PacketNpcVisibleTrue) msg); }
        else if (msg instanceof PacketGuiParts) { packetGuiParts((PacketGuiParts) msg); }
        else if (msg instanceof PacketSoundGUIOpen) { packetSoundGUIOpen(); }
        else if (msg instanceof PacketNpcRotationUpdate) { packetNpcRotationUpdate((PacketNpcRotationUpdate) msg); }

        // New Unofficial (Goodbird)
        else if (msg instanceof PacketSyncRecipeUpdate) { packetSyncRecipeUpdate((PacketSyncRecipeUpdate) msg); }
        else if (msg instanceof PacketSyncRecipeRemove) { packetSyncRecipeRemove((PacketSyncRecipeRemove) msg); }
        else if (msg instanceof PacketUpdatePhysics) { packetUpdatePhysics((PacketUpdatePhysics) msg); }
        else if (msg instanceof PacketOverlayShow) { packetOverlayShow((PacketOverlayShow) msg); }
        else if (msg instanceof PacketOverlayHide) { packetOverlayHide((PacketOverlayHide) msg); }
        else if (msg instanceof PacketHideAllOverlays) { packetHideAllOverlays(); }

        // New Unofficial (BetaZavr)
        else if (msg instanceof PacketSpeak) { packetSpeak((PacketSpeak) msg); }
        else if (msg instanceof PacketClientScripts) { packetClientScripts((PacketClientScripts) msg); }
        else if (msg instanceof PacketSendFileList) { packetSendFileList((PacketSendFileList) msg); }
        else if (msg instanceof PacketSendFilePart) { packetSendFilePart((PacketSendFilePart) msg); }
        else if (msg instanceof PacketEventNames) { packetEventNames((PacketEventNames) msg); }
        else if (msg instanceof PacketRemoteNpcsEntity) { packetRemoteNpcsEntity((PacketRemoteNpcsEntity) msg); }
        else if (msg instanceof PacketSkin) { packetSkin((PacketSkin) msg); }
        else if (msg instanceof PacketDetectHeldItem) { packetDetectHeldItem((PacketDetectHeldItem) msg); }
        else if (msg instanceof PacketDataGuiOpen) { packetDataGuiOpen((PacketDataGuiOpen) msg); }
        else if (msg instanceof PacketScriptText) { packetScriptText((PacketScriptText) msg); }
        else if (msg instanceof PacketScriptConsole) { packetScriptConsole((PacketScriptConsole) msg); }
        else if (msg instanceof PacketDebug) { packetDebug((PacketDebug) msg); }
        else if (msg instanceof PacketCustomNBT) { packetCustomNBT((PacketCustomNBT) msg); }
        else if (msg instanceof PacketStopSound) { packetStopSound((PacketStopSound) msg); }
        else if (msg instanceof PacketClearMarcets) { packetClearMarcets(); }
        else if (msg instanceof PacketUpdateMarcetGui) { packetUpdateMarcetGui(); }
        else if (msg instanceof PacketDealData) { packetDealData((PacketDealData) msg); }
        else if (msg instanceof PacketMarcetData) { packetMarcetData((PacketMarcetData) msg); }
        else if (msg instanceof PacketMarcetRemove) { packetMarcetRemove((PacketMarcetRemove) msg); }
        else if (msg instanceof PacketDealUpdate) { packetDealUpdate((PacketDealUpdate) msg); }
        else if (msg instanceof PacketMarcetClose) { packetMarcetClose((PacketMarcetClose) msg); }
        else if (msg instanceof PacketOpenCase) { packetOpenCase((PacketOpenCase) msg); }
        else if (msg instanceof PacketPermissionMenu) { packetPermissionMenu((PacketPermissionMenu) msg); }
        else if (msg instanceof PacketPermissionGlobal) { packetPermissionGlobal((PacketPermissionGlobal) msg); }
        else if (msg instanceof PacketDropTemplateClear) { packetDropTemplateClear(); }
        else if (msg instanceof PacketDropTemplateSave) { packetDropTemplateSave((PacketDropTemplateSave) msg); }
        else if (msg instanceof PacketOverworldTime) { packetOverworldTime((PacketOverworldTime) msg); }
        else if (msg instanceof PacketBankSetPlayer) { packetBankSetPlayer((PacketBankSetPlayer) msg); }
        else if (msg instanceof PacketBankClearPos) { packetBankClearPos(); }
        else if (msg instanceof PacketBankReOpen) { packetBankReOpen(); }
        else if (msg instanceof PacketBankSave) { packetBankSave((PacketBankSave) msg); }
        else if (msg instanceof PacketSaveSchematic) { packetSaveSchematic((PacketSaveSchematic) msg); }
        else if (msg instanceof PacketScriptError) { packetScriptError((PacketScriptError) msg); }
        else if (msg instanceof PacketNpcNavigation) { packetNpcNavigation((PacketNpcNavigation) msg); }
        else if (msg instanceof PacketNpcTarget) { packetNpcTarget((PacketNpcTarget) msg); }
        else if (msg instanceof PacketMenuSave) { packetMenuSave((PacketMenuSave) msg); }
        else if (msg instanceof PacketCustomAnimationSet) { packetCustomAnimationSet((PacketCustomAnimationSet) msg); }
        else if (msg instanceof PacketCustomAnimationStop) { packetCustomAnimationStop((PacketCustomAnimationStop) msg); }
        else if (msg instanceof PacketCustomAnimationBaseSet) { packetCustomAnimationBaseSet((PacketCustomAnimationBaseSet) msg); }
        else if (msg instanceof PacketCustomAnimationRun) { packetCustomAnimationRun((PacketCustomAnimationRun) msg); }
        else if (msg instanceof PacketCustomEmotionStop) { packetCustomEmotionStop((PacketCustomEmotionStop) msg); }
        else if (msg instanceof PacketCustomEmotionRun) { packetCustomEmotionRun((PacketCustomEmotionRun) msg); }
        else if (msg instanceof PacketNpcInitData) { packetNpcInitData((PacketNpcInitData) msg); }
        else if (msg instanceof PacketNpcLookPos) { packetNpcLookPos((PacketNpcLookPos) msg); }
        else if (msg instanceof PacketCustomChestName) { packetCustomChestName((PacketCustomChestName) msg); }
        else if (msg instanceof PacketNpcRarityTitleSet) { packetNpcRarityTitleSet((PacketNpcRarityTitleSet) msg); }
        else if (msg instanceof PacketBorderData) { packetBorderData((PacketBorderData) msg); }
        else if (msg instanceof PacketBorderClear) { packetBorderClear(); }
        else if (msg instanceof PacketHeapAnalyzer) { packetHeapAnalyzer((PacketHeapAnalyzer) msg); }
        CustomNpcs.debugData.end("Packet", msg.getClass().getSimpleName());
    }

    private static void packetAchievement(PacketAchievement msg) {
        Component showTitle = Component.empty().append(msg.title);
        Component showMessage = Component.empty().append(msg.message);
        // has quest
        if (msg.compound.hasKey("questID", 3) && msg.compound.getInteger("questID") >= 0) {
            IQuest quest = QuestController.instance.get(msg.compound.getInteger("questID"));
            if (quest == null) { return; }
            showTitle = Component.translatable("quest.name")
                    .append(": " + quest.getTitle());
            int[] pr = msg.compound.getIntArray("Progress");
            if (msg.compound.getString("Type").equalsIgnoreCase("craft")) {
                ItemStack item = new ItemStack(msg.compound.getCompoundTag("Item"));
                showMessage = Component.literal(item.getDisplayName());
            }
            else { showMessage = Component.translatable(msg.compound.getString("TargetName")); }
            if (pr[0] >= pr[1]) { // is complete
                showMessage.append(" -");
                showMessage.append(Component.translatable("quest.task." + msg.compound.getString("Type") + ".0"));
            }
            else { showMessage.appendText(" = " + pr[0] + "/" + pr[1]); }
        }
        // get all toasts
        Object[] visible = null; // GuiToast.ToastInstance<?>[]
        for (Field f : GuiToast.class.getDeclaredFields()) {
            if (f.getType().getName().contains("ToastInstance")) {
                try {
                    f.setAccessible(true);
                    visible = (Object[]) f.get(minecraft.getToastGui());
                }
                catch (Exception e) { LogWriter.debug(e.toString()); }
            }
        }
        if (visible == null) { return; }
        // change old or add new toast
        boolean found = false;
        for (Object obj : visible) {
            if (obj == null) { continue; }
            Field toast = obj.getClass().getDeclaredFields()[0];
            toast.setAccessible(true);
            try {
                if (!(toast.get(obj) instanceof GuiAchievement)) { continue; }
                GuiAchievement achn = (GuiAchievement) toast.get(obj);
                Field titleF = GuiAchievement.class.getDeclaredFields()[3];
                Field typeF = GuiAchievement.class.getDeclaredFields()[4];
                titleF.setAccessible(true);
                typeF.setAccessible(true);
                String titleD = Util.instance.deleteColor((String) titleF.get(achn));
                int typeD = (int) typeF.get(achn);
                if (!titleD.equals(Util.instance.deleteColor(msg.title.getFormattedText())) || msg.type != typeD) { continue; }
                achn.setDisplayedText(showTitle.getParent(), showMessage.getParent());
                found = true;
            }
            catch (Exception ignored) { }
        }
        if (!found) { minecraft.getToastGui().add(new GuiAchievement(msg.title, msg.message, msg.type)); }
    }

    private static void packetChat(PacketChat msg) { msg.player.sendMessage(msg.message); }

    private static void packetChatBubble(PacketChatBubble msg) {
        Entity entity = minecraft.world.getEntityByID(msg.id);
        if (CustomNpcs.EnableChatBubbles && entity instanceof EntityNPCInterface) {
            EntityNPCInterface npc = (EntityNPCInterface) entity;
            if (npc.messages == null) { npc.messages = new RenderChatMessages(); }
            String text = NoppesStringUtils.formatText(msg.message.getFormattedText(), msg.player, npc);
            npc.messages.addMessage(text, npc);
            if (msg.showMessage) { msg.player.sendMessage(new TextComponentString(npc.getName() + ": " + text)); }
        }
        else if (CustomNpcs.EnablePlayerChatBubbles && entity instanceof EntityPlayer) {
            EntityPlayer pl = (EntityPlayer) entity;
            if (!ClientEventHandler.chatMessages.containsKey(pl)) { ClientEventHandler.chatMessages.put(pl, new RenderChatMessages()); }
            ClientEventHandler.chatMessages.get(pl).addMessage(msg.message.getFormattedText(), pl);
        }
    }

    private static void packetConfigFont(PacketConfigFont msg) {
        CustomNPCsScheduler.runTack(() -> {
            if (msg.font != null && !msg.font.isEmpty()) {
                CustomNpcs.FontType = msg.font;
                CustomNpcs.FontSize = msg.size;
                ClientProxy.Font.clear();
                ClientProxy.Font = new ClientProxy.FontContainer(CustomNpcs.FontType, CustomNpcs.FontSize);
                CustomNpcs.Config.updateConfig();
                msg.player.sendMessage(new TextComponentTranslation("Font set to %s", ClientProxy.Font.getName()));
            }
            else { msg.player.sendMessage(new TextComponentTranslation("Current font is %s", ClientProxy.Font.getName())); }
        });
    }

    private static void packetDialog(PacketDialog msg) {
        if (minecraft.world == null) { return; }
        Entity entity = minecraft.world.getEntityByID(msg.entityId);
        if (entity instanceof EntityNPCInterface) {
            Dialog dialog = DialogController.instance.dialogs.get(msg.dialogId);
            PacketDialog.openDialog(dialog, (EntityNPCInterface) entity, msg.player);
        }
    }

    private static void packetDialogDummy(PacketDialogDummy msg) {
        EntityDialogNpc npc = new EntityDialogNpc(msg.player.world);
        npc.display.setName(Component.translatable(msg.name).toString());
        EntityUtil.Copy(msg.player, npc);
        Dialog dialog = new Dialog(null);
        dialog.load(msg.data);
        PacketDialog.openDialog(dialog, npc, msg.player);
    }

    private static void packetEyeBlink(PacketEyeBlink msg) {
        if (minecraft.world != null) {
            Entity entity = minecraft.world.getEntityByID(msg.id);
            if (entity instanceof EntityNPCInterface) {
                ((EntityCustomNpc) entity).modelData.eyes.blinkStart = System.currentTimeMillis();
            }
        }
    }

    private static void packetGuiCloneOpen(PacketGuiCloneOpen msg) {
        NoppesUtil.openGUI(msg.player, new GuiNpcMobSpawnerAdd(msg.data));
    }

    private static void packetGuiClose(PacketGuiClose msg) {
        if (minecraft.currentScreen != null) {
            if (minecraft.currentScreen instanceof IGuiClose) {
                ((IGuiClose) minecraft.currentScreen).setClose(msg.data);
                if (minecraft.currentScreen instanceof GuiMailmanWrite) { return; }
            }
            minecraft.displayGuiScreen(null);
            minecraft.setIngameFocus();
        }
    }

    private static void packetGuiData(PacketGuiData msg) {
        GuiScreen gui = minecraft.currentScreen;
        while (gui instanceof IGuiInterface && ((IGuiInterface) gui).hasSubGui()) { gui = ((IGuiInterface) gui).getSubGui(); }
        if (gui instanceof IGuiData) { ((IGuiData) gui).setGuiData(msg.data); }
    }

    private static void packetGuiComponentUpdate(PacketGuiComponentUpdate msg) {
        if (minecraft.currentScreen instanceof GuiCustom) {
            GuiCustom cGui = (GuiCustom) minecraft.currentScreen;
            CustomGuiComponentWrapper component = (CustomGuiComponentWrapper) cGui.guiWrapper.getComponentUuid(msg.id);
            if (component != null) {
                component.fromNBT(msg.data);
            }
        }
    }

    private static void packetGuiError(PacketGuiError msg) {
        if (minecraft.currentScreen instanceof IGuiError) {
            ((IGuiError) minecraft.currentScreen).setError(msg.error, msg.data);
        }
    }

    private static void packetGuiOpen(PacketGuiOpen msg) {
        CustomNpcs.proxy.openGui(NoppesUtilServer.getEditingNpc(msg.player), msg.gui, msg.buffer);
        if (msg.windowId != 0 && msg.player != null && msg.player.openContainer != null && msg.player.openContainer != msg.player.inventoryContainer) {
            msg.player.openContainer.windowId = msg.windowId;
        }
    }

    private static void packetGuiScrollData(PacketGuiScrollData msg) {
        Map<UUID, Map<String, Integer>> scrollData = PacketGuiScrollData.scrollData;
        if (!scrollData.containsKey(msg.id)) { scrollData.put(msg.id, new HashMap<>()); }
        scrollData.get(msg.id).putAll(msg.data);
        if (msg.step == msg.size) {
            GuiScreen gui = minecraft.currentScreen;
            while (gui instanceof IGuiInterface && ((IGuiInterface) gui).hasSubGui()) { gui = ((IGuiInterface) gui).getSubGui(); }
            Map<String, Integer> map = scrollData.get(msg.id);
            if (gui instanceof IScrollData) { ((IScrollData) gui).setData(new Vector<>(map.keySet()), map); }
            scrollData.remove(msg.id);
        }
    }

    private static void packetGuiScrollList(PacketGuiScrollList msg) {
        Map<UUID, Vector<String>> listData = PacketGuiScrollList.listData;
        if (!listData.containsKey(msg.id)) { listData.put(msg.id, new Vector<>()); }
        listData.get(msg.id).addAll(msg.data);
        if (msg.step == msg.size) {
            GuiScreen gui = minecraft.currentScreen;
            while (gui instanceof IGuiInterface && ((IGuiInterface) gui).hasSubGui()) { gui = ((IGuiInterface) gui).getSubGui(); }
            Vector<String> list = listData.get(msg.id);
            if (gui instanceof IScrollData) { ((IScrollData) gui).setData(list, null); }
            listData.remove(msg.id);
        }
    }

    private static void packetGuiScrollSelected(PacketGuiScrollSelected msg) {
        if (minecraft.currentScreen instanceof IScrollData) {
            ((IScrollData) minecraft.currentScreen).setSelected(msg.selected);
        }
    }

    private static void packetGuiUpdate() {
        if (minecraft.currentScreen instanceof IGuiInterface) {
            minecraft.currentScreen.setWorldAndResolution(minecraft, minecraft.currentScreen.width, minecraft.currentScreen.height);
        }
    }

    private static void packetItemUpdate(PacketItemUpdate msg) {
        ItemStack stack = msg.player.inventory.getStackInSlot(msg.id);
        if (!stack.isEmpty()) {
            ((ItemStackWrapper) Objects.requireNonNull(NpcAPI.Instance()).getIItemStack(stack)).setMCNbt(msg.data);
        }
    }

    private static void packetMarkData(PacketMarkData msg) {
        if (minecraft.world != null) {
            Entity entity = minecraft.world.getEntityByID(msg.id);
            if (entity instanceof EntityLivingBase) {
                MarkData mark = MarkData.get((EntityLivingBase) entity);
                mark.setNBT(msg.data);
            }
        }
    }

    private static void packetNpcDelete(PacketNpcDelete msg) {
        if (minecraft.world != null) {
            Entity entity = minecraft.world.getEntityByID(msg.id);
            if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).delete(); }
            else if (entity != null) { entity.setDead(); }
        }
    }

    private static void packetNpcEdit(PacketNpcEdit msg) {
        if (minecraft.world != null) {
            Entity entity = minecraft.world.getEntityByID(msg.id);
            if (entity instanceof EntityNPCInterface) { NoppesUtilServer.setEditingNpc(msg.player, (EntityNPCInterface) entity); }
            else { NoppesUtilServer.setEditingNpc(msg.player, null); }
        }
    }

    private static void packetNpcRole(PacketNpcRole msg) {
        if (minecraft.world != null) {
            Entity entity = minecraft.world.getEntityByID(msg.id);
            if (entity instanceof EntityNPCInterface) {
                ((EntityNPCInterface) entity).advanced.setRole(msg.data.getInteger("Type"));
                ((EntityNPCInterface) entity).role.load(msg.data);
                NoppesUtilServer.setEditingNpc(msg.player, (EntityNPCInterface) entity);
            }
        }
    }

    private static void packetNpcUpdate(PacketNpcUpdate msg) {
        WorldClient world = minecraft.world;
        if (world != null) {
            Entity entity = world.getEntityByID(msg.id);
            if (entity instanceof EntityNPCInterface) {
                if (minecraft.currentScreen instanceof INpcMenuGui && NoppesUtilServer.getEditingNpc(msg.player) == entity) { return; }
                ((EntityNPCInterface) entity).readSpawnData(msg.data);
            }
        }
    }

    private static void packetParticle(PacketParticle msg) {
        if (minecraft.world != null) {
            Random rand = minecraft.world.rand;
            if (msg.name.equals("heal")) {
                for (int k = 0; k < 6; ++k) {
                    minecraft.world.spawnParticle(EnumParticleTypes.SPELL_INSTANT, msg.posX + (rand.nextDouble() - 0.5) * msg.width, msg.posY + rand.nextDouble() * msg.height, msg.posZ + (rand.nextDouble() - 0.5) * msg.width, 0.0, 0.0, 0.0);
                    minecraft.world.spawnParticle(EnumParticleTypes.SPELL, msg.posX + (rand.nextDouble() - 0.5) * msg.width, msg.posY + rand.nextDouble() * msg.height, msg.posZ + (rand.nextDouble() - 0.5) * msg.width, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    private static void packetPlayMusic(PacketPlayMusic msg) {
        if (msg.streaming) { MusicController.Instance.playStreaming(msg.name, msg.player, msg.looping); }
        else { MusicController.Instance.playMusic(msg.name, msg.player, msg.looping); }
    }

    private static void packetPlaySound(PacketPlaySound msg) {
        MusicController.Instance.playSound(msg.category, msg.name, msg.x, msg.y, msg.z, msg.volume, msg.pitch);
    }

    private static void packetQuestCompletion(PacketQuestCompletion msg) {
        Quest quest = QuestController.instance.get(msg.id);
        if (quest != null) {
            if (quest.rewardType == EnumRewardType.ONE_SELECT && !quest.rewardItems.isEmpty()) { Packets.sendServer(new SPacketQuestChooseReward(msg.id)); }
            else { Packets.sendServer(new SPacketQuestCompletionCheck(msg.id, ItemStack.EMPTY)); }
        }
    }

    private static void packetSync(PacketSync msg) {
        switch (msg.type) {
            case 1: {
                NBTTagList list = msg.data.getTagList("Data", 10);
                for(int i = 0; i < list.tagCount(); ++i) {
                    Faction faction = new Faction();
                    faction.load(list.getCompoundTagAt(i));
                    FactionController.instance.factionsSync.put(faction.id, faction);
                }
                if (msg.syncEnd) {
                    FactionController.instance.factions.clear();
                    FactionController.instance.factions.putAll(FactionController.instance.factionsSync);
                    FactionController.instance.factionsSync.clear();
                }
                break;
            } // factions
            case 2: {
                if (!msg.data.hasNoTags()) { PlayerData.get(msg.player).game.load(msg.data); }
                break;
            } // gameData
            case 3: {
                if (!msg.data.hasNoTags()) {
                    QuestCategory category;
                    if (QuestController.instance.categoriesSync.containsKey(msg.data.getInteger("Slot"))) {
                        category = QuestController.instance.categoriesSync.get(msg.data.getInteger("Slot"));
                    }
                    else { category = new QuestCategory(); }
                    category.load(msg.data);
                    QuestController.instance.categoriesSync.put(category.id, category);
                }
                if (msg.syncEnd) {
                    TreeMap<Integer, Quest> map = new TreeMap<>();
                    for (QuestCategory category : QuestController.instance.categoriesSync.values()) {
                        for (Quest quest : category.quests.values()) { map.put(quest.id, quest); }
                    }
                    QuestController.instance.categories.clear();
                    QuestController.instance.categories.putAll(QuestController.instance.categoriesSync);
                    QuestController.instance.quests.clear();
                    QuestController.instance.quests.putAll(map);
                    QuestController.instance.categoriesSync.clear();
                }
                if (minecraft.currentScreen instanceof GuiNpcManageQuest) { minecraft.currentScreen.initGui(); }
                break;
            } // quests
            case 4: {
                if (!msg.data.hasNoTags()) { PlayerData.get(msg.player).questData.load(msg.data); }
                if (minecraft.currentScreen instanceof GuiLog) { minecraft.currentScreen.initGui(); }
                break;
            } // questData
            case 5: {
                if (!msg.data.hasNoTags()) {
                    DialogCategory category;
                    if (DialogController.instance.categoriesSync.containsKey(msg.data.getInteger("Slot"))) {
                        category = DialogController.instance.categoriesSync.get(msg.data.getInteger("Slot"));
                    }
                    else { category = new DialogCategory(); }
                    category.load(msg.data);
                    DialogController.instance.categoriesSync.put(category.id, category);
                }
                if (msg.syncEnd) {
                    TreeMap<Integer, Dialog> map = new TreeMap<>();
                    for (DialogCategory category : DialogController.instance.categoriesSync.values()) {
                        for (Dialog dialog : category.dialogs.values()) { map.put(dialog.id, dialog); }
                    }
                    DialogController.instance.categories.clear();
                    DialogController.instance.categories.putAll(DialogController.instance.categoriesSync);
                    DialogController.instance.dialogs.clear();
                    DialogController.instance.dialogs.putAll(map);
                    DialogController.instance.categoriesSync.clear();
                }
                if (minecraft.currentScreen instanceof GuiNpcManageDialogs) { minecraft.currentScreen.initGui(); }
                break;
            } // dialogs
            case 6: {
                if (!msg.data.hasNoTags()) { PlayerData.get(msg.player).overlay.load(msg.data); }
                break;
            } // overlay data
            case 7: {
                //RecipeController.instance.load();
                break;
            } // recipes
            case 8: {
                PlayerData.get(msg.player).setNBT(msg.data);
                break;
            } // playerData
            case 9: {
                if (minecraft.currentScreen instanceof GuiNpcDimension) {
                    ((GuiNpcDimension) minecraft.currentScreen).currentDimId = msg.data.getInteger("CurrentDimensionId");
                }
                DimensionController.loadData(msg.data);
                break;
            } // dimensions
            case 10: {
                DialogController.instance.getGuiSettings().load(msg.data);
                break;
            } // dialog gui settings
            case 11: {
                RecipeController rData = RecipeController.getInstance();
                if (msg.syncEnd) { rData.reloadGlobalRecipes(); }
                else {
                    RecipeCarpentry recipe = RecipeCarpentry.create(msg.data);
                    if (!rData.syncRecipes.containsKey(recipe.getGroup())) { rData.syncRecipes.put(recipe.getGroup(), new ArrayList<>()); }
                    rData.syncRecipes.get(recipe.getGroup()).add(recipe);
                }
                break;
            } // global recipes
            case 12: {
                RecipeController rData = RecipeController.getInstance();
                if (msg.syncEnd) { rData.reloadAnvilRecipes(); }
                else {
                    RecipeCarpentry recipe = RecipeCarpentry.create(msg.data);
                    if (!rData.syncRecipes.containsKey(recipe.getGroup())) { rData.syncRecipes.put(recipe.getGroup(), new ArrayList<>()); }
                    rData.syncRecipes.get(recipe.getGroup()).add(recipe);
                }
                break;
            } // mod recipes
            case 13: {
                ConfigLoader.load(msg.data);
                break;
            } // mod data
            case 14: {
                CustomNpcsPermissions.set(msg.data);
                if (minecraft.currentScreen instanceof GuiPermissionsEdit) {
                    minecraft.currentScreen.setWorldAndResolution(minecraft, minecraft.currentScreen.width, minecraft.currentScreen.height);
                }
                break;
            } // permissions
            case 15: {
                if (!msg.data.hasNoTags()) { PlayerData.get(msg.player).overlay.load(msg.data); }
                break;
            } // overlayData
            case 16: {
                if (msg.player.getServer() == null) {
                    ItemScripted.Resources = NBTTags.getIntegerStringMap(msg.data.getTagList("List", 10));
                }
                CustomNpcs.proxy.reloadItemTextures();
                break;
            } // ItemScriptedModels
            case 17: {
                KeyController.getInstance().loadKeys(msg.data);
                CustomNpcs.proxy.updateKeys();
                break;
            } // custom keys
            case 18: {
                CustomNpcs.proxy.syncRecipeManager();
                if (minecraft.currentScreen instanceof GuiNpcManageRecipes) {
                    ((GuiNpcManageRecipes) minecraft.currentScreen).resetData();
                    minecraft.currentScreen.initGui();
                }
                break;
            } // synchronized recipes
        }
    }

    private static void packetSyncRemove(PacketSyncRemove msg) {
        switch (msg.type) {
            case 1: {
                FactionController.instance.factions.remove(msg.id);
                break;
            } // faction
            case 2: {
                Quest quest = QuestController.instance.quests.remove(msg.id);
                if (quest != null) { quest.category.quests.remove(msg.id); }
                break;
            } // quest
            case 3: {
                QuestCategory category = QuestController.instance.categories.remove(msg.id);
                if (category != null) { QuestController.instance.quests.keySet().removeAll(category.quests.keySet()); }
                break;
            } // quest category
            case 4: {
                Dialog dialog = DialogController.instance.dialogs.remove(msg.id);
                if (dialog != null) { dialog.category.dialogs.remove(msg.id); }
                break;
            } // dialog
            case 5: {
                DialogCategory category = DialogController.instance.categories.remove(msg.id);
                if (category != null) { DialogController.instance.dialogs.keySet().removeAll(category.dialogs.keySet()); }
                break;
            } // dialog category
            case 6: {
                MarcetController.getInstance().removeMarcet(msg.id);
                if (minecraft.currentScreen instanceof GuiNpcManageMarkets) { ((GuiNpcManageMarkets) minecraft.currentScreen).setGuiData(new NBTTagCompound()); }
                else if (minecraft.currentScreen instanceof GuiNPCTrader) { ((GuiNPCTrader) minecraft.currentScreen).setGuiData(new NBTTagCompound()); }
                break;
            } // marcet deal
            case 7: {
                MarcetController.getInstance().removeDeal(msg.id);
                if (minecraft.currentScreen instanceof GuiNpcManageMarkets) { ((GuiNpcManageMarkets) minecraft.currentScreen).setGuiData(new NBTTagCompound()); }
                else if (minecraft.currentScreen instanceof GuiNPCTrader) { ((GuiNPCTrader) minecraft.currentScreen).setGuiData(new NBTTagCompound()); }
                break;
            } // marcet deal
            case 8: {
                KeyController.getInstance().removeKeySetting(msg.id);
                CustomNpcs.proxy.updateKeys();
                break;
            } // IKeySetting
            case 9: {
                if (msg.id < 0) { AnimationController.getInstance().clearAnimations(); }
                else { AnimationController.getInstance().removeAnimation(msg.id); }
                break;
            } // custom animation
            case 10: {
                if (msg.id < 0) { AnimationController.getInstance().clearEmotions(); }
                else { AnimationController.getInstance().removeEmotion(msg.id); }
                break;
            } // custom emotion
        }
    }

    private static void packetSyncUpdate(PacketSyncUpdate msg) {
        switch (msg.type) {
            case 1: {
                Faction faction = new Faction();
                faction.load(msg.data);
                FactionController.instance.factions.put(faction.id, faction);
                break;
            } // faction
            case 2: {
                QuestController qData = QuestController.instance;
                Quest quest = qData.get(msg.data.getInteger("Id"));
                if (quest != null) { quest.load(msg.data); }
                else {
                    QuestCategory category = QuestController.instance.categories.get(msg.id);
                    quest = new Quest(category);
                    quest.load(msg.data);
                    QuestController.instance.quests.put(quest.id, quest);
                    category.quests.put(quest.id, quest);
                }
                PlayerQuestData questData = PlayerData.get(msg.player).questData;
                for (QuestData qd : new ArrayList<>(questData.activeQuests.values())) {
                    if (qd.quest.id == quest.id) {
                        qd.reset(quest);
                        break;
                    }
                }
                break;
            } // quest
            case 3: {
                QuestCategory category = new QuestCategory();
                category.load(msg.data);
                QuestController.instance.categories.put(category.id, category);
                break;
            } // quest category
            case 4: {
                DialogController dData = DialogController.instance;
                Dialog dialog = dData.get(msg.data.getInteger("DialogId"));
                if (dialog != null) { dialog.load(msg.data); }
                else {
                    DialogCategory category = DialogController.instance.categories.get(msg.id);
                    dialog = new Dialog(category);
                    dialog.load(msg.data);
                    DialogController.instance.dialogs.put(dialog.id, dialog);
                    category.dialogs.put(dialog.id, dialog);
                }
                break;
            } // dialog
            case 5: {
                DialogCategory category = new DialogCategory();
                category.load(msg.data);
                DialogController.instance.categories.put(category.id, category);
                break;
            } // dialog category
            case 6: {
                updateMinimap(msg);
                break;
            } // minimap
            case 7: {
                BuilderData builder;
                if (SyncController.dataBuilder.containsKey(msg.id)) { builder = SyncController.dataBuilder.get(msg.id); }
                else { builder = new BuilderData(msg.id, msg.data.getInteger("BuilderType")); }
                builder.read(msg.data);
                break;
            } // builder data
            case 8: {
                KeyController.getInstance().loadKey(msg.data);
                CustomNpcs.proxy.updateKeys();
                break;
            } // change or add IKeySetting
            case 9: {
                if (!msg.data.hasNoTags()) { AnimationController.getInstance().loadAnimation(msg.data); }
                break;
            } // custom animation set
            case 10: {
                if (!msg.data.hasNoTags()) { AnimationController.getInstance().loadEmotion(msg.data); }
                break;
            } // custom emotion set
            case 11: {
                DialogController.instance.getGuiSettings().load(msg.data);
                break;
            } // dialog gui settings
            case 12: {
                if (msg.data.hasKey("MailData", 9)) { PlayerData.get(msg.player).mailData.load(msg.data); }
                if (msg.data.hasKey("LettersBeDeleted", 3)) { CustomNpcs.MailTimeWhenLettersWillBeDeleted = msg.data.getInteger("LettersBeDeleted"); }
                if (msg.data.hasKey("LettersBeReceived", 11)) {
                    int[] vs = msg.data.getIntArray("LettersBeReceived");
                    System.arraycopy(vs, 0, CustomNpcs.MailTimeWhenLettersWillBeReceived, 0, vs.length);
                }
                if (msg.data.hasKey("CostSendingLetter", 11)) {
                    int[] vs = msg.data.getIntArray("CostSendingLetter");
                    System.arraycopy(vs, 0, CustomNpcs.MailCostSendingLetter, 0, vs.length);
                }
                if (msg.data.hasKey("SendToYourself", 1)) { CustomNpcs.MailSendToYourself = msg.data.getBoolean("SendToYourself"); }
                break;
            } // new mail info ou screen
            case 13: {
                PlayerData.get(msg.player).factionData.load(msg.data);
                break;
            } // faction data update
            case 14: {
                if (msg.id < 0) { TransportController.getInstance().clear(); }
                else { TransportController.getInstance().loadCategory(msg.data); }
                break;
            } // transport update
            case 15: {
                AnimationController.getInstance().loadEmotion(msg.data);
                break;
            } // set custom emotion
            case 16: {
                CustomNpcs.TypeShowQuestCompass = msg.id;
                break;
            } // TypeShowQuestCompass
        }
    }

    private static void packetNpcVisibleFalse(PacketNpcVisibleFalse msg) {
        WorldClient world = (WorldClient) msg.player.world;
        List<EntityNPCInterface> npcInterfaces = world.getEntities(EntityNPCInterface.class, entity -> entity.getUniqueID().equals(msg.uuid) && entity.getEntityId() == msg.id);
        for (EntityNPCInterface npc : npcInterfaces) {
            if (npc == null) { continue; }
            world.removeEntity(npc);
        }
    }

    private static void packetNpcVisibleTrue(PacketNpcVisibleTrue msg) {
        WorldClient world = (WorldClient) msg.player.world;
        List<EntityNPCInterface> npcInterfaces = world.getEntities(EntityNPCInterface.class,
                entity -> entity.getUniqueID().equals(msg.uuid));
        if (npcInterfaces.isEmpty()) {
            npcInterfaces = world.getEntities(EntityNPCInterface.class, entity -> entity.getEntityId() == msg.id);
        }
        if (npcInterfaces.isEmpty()) {
            LogWriter.debug("Tries to visible summon an entity into the client world.");
            EntitySpawnMessageHelper.spawn(msg.pkt);
        }
        if (!npcInterfaces.isEmpty()) {
            for (EntityNPCInterface npc : npcInterfaces) {
                if (npc == null) {
                    LogWriter.debug("Tries to visible summon an NPC into the client world.");
                    EntitySpawnMessageHelper.spawn(msg.pkt);
                }
            }
        }
    }

    private static void packetGuiParts(PacketGuiParts msg) {
        Entity entity = msg.player.world.getEntityByID(msg.id);
        if (minecraft.currentScreen instanceof GuiCustom && entity instanceof EntityCustomNpc) {
         /*GuiCreationNewParts parts = new GuiCreationNewParts(((GuiCustom) mc.currentScreen), ((EntityCustomNpc) entity));
         gui.initCallback = () -> {
            gui.add(parts);
            parts.init();
         };*/
            ((GuiCustom) minecraft.currentScreen).setGuiData(msg.data);
        }
    }

    private static void packetNpcRotationUpdate(PacketNpcRotationUpdate msg) {
        WorldClient world = minecraft.world;
        if (world != null) {
            Entity entity = world.getEntityByID(msg.id);
            if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).ais.orientation = msg.orientation; }
        }
    }

    private static void packetSoundGUIOpen() {
        minecraft.displayGuiScreen(new SubGuiSoundSelection(minecraft.currentScreen, 0, null, ""));
    }

    private static void packetSyncRecipeUpdate(PacketSyncRecipeUpdate msg) {
        RecipeController.getInstance().addAndSaveRecipe(RecipeCarpentry.create(msg.data));
    }

    private static void packetSyncRecipeRemove(PacketSyncRecipeRemove msg) {
        if (msg.type == 6) {
            RecipeController.getInstance().delete(msg.id);
            RecipeController.getInstance().reloadGlobalRecipes();
        }
        else if (msg.type == 7) {
            RecipeController.getInstance().delete(msg.id);
            RecipeController.getInstance().reloadAnvilRecipes();
        }
    }

    private static void packetUpdatePhysics(PacketUpdatePhysics msg) {
        WorldClient world = (WorldClient) msg.player.world;
        List<EntityNPCInterface> npcInterfaces = world.getEntities(EntityNPCInterface.class,
                entity -> entity.getUniqueID().equals(msg.uuid));
        if (npcInterfaces.isEmpty()) {
            npcInterfaces = world.getEntities(EntityNPCInterface.class, entity -> entity.getEntityId() == msg.id);
        }
        if (npcInterfaces.isEmpty()) {
            LogWriter.debug("Tries to visible summon an entity into the client world.");
            EntitySpawnMessageHelper.spawn(msg.pkt);
            if (PhysicsHelper.Enabled) { PhysicsHelper.resetEntityPhysics(world, msg.id); }
        }
        else {
            for (EntityNPCInterface npc : npcInterfaces) {
                if (npc == null) {
                    LogWriter.debug("Tries to visible summon an NPC into the client world.");
                    EntitySpawnMessageHelper.spawn(msg.pkt);
                    if (PhysicsHelper.Enabled) { PhysicsHelper.resetEntityPhysics(world, msg.id); }
                }
            }
        }
    }

    private static void packetOverlayShow(PacketOverlayShow msg) {
        OverlayWrapper wrapper = new OverlayWrapper(0);
        wrapper.load(new NBTWrapper(msg.compound));
        OverlayController.getInstance().addOverlay(wrapper);
    }

    private static void packetOverlayHide(PacketOverlayHide msg) { OverlayController.getInstance().removeOverlay(msg.id); }

    private static void packetHideAllOverlays() { OverlayController.getInstance().clear(); }

    private static void packetSpeak(PacketSpeak msg) {
        MusicController.Instance.speak(msg.languageKey, msg.text, msg.volume);
    }

    private static void packetClientScripts(PacketClientScripts msg) {
        ScriptController.HasStart = true;
        ScriptController.Instance.setClientScripts(msg.compound);
    }

    private static void packetSendFileList(PacketSendFileList msg) {
        for (int i = 0; i < msg.compound.getTagList("FileList", 10).tagCount(); i++) {
            NBTTagCompound tempFile = msg.compound.getTagList("FileList", 10).getCompoundTagAt(i);
            String name = tempFile.getString("name");
            if (!ClientProxy.loadFiles.containsKey(name)) { ClientProxy.loadFiles.put(name, new TempFile()); }
            TempFile file = ClientProxy.loadFiles.get(name);
            file.setTitle(tempFile);
        }
        ClientTickHandler.loadFiles();
    }

    private static void packetSendFilePart(PacketSendFilePart msg) {
        if (msg.remove) { ClientProxy.loadFiles.remove(msg.name); }
        if (!ClientProxy.loadFiles.containsKey(msg.name)) { return; }
        TempFile file = ClientProxy.loadFiles.get(msg.name);
        file.data.put(msg.partId, msg.partText);
        file.lastLoad = System.currentTimeMillis() - 15000L;
        file.tryLoads = 0;
        if (file.isLoad()) {
            if (file.saveType == 1) {
                LogWriter.info("Script Client file was received from the Server: \"" + msg.name + "\"");
                File normalFile = new File(CustomNpcs.Dir, ScriptController.Instance.clientScripts.getLanguage().toLowerCase() + "/" + msg.name);
                if (msg.player.isCreative() || PlayerData.get(msg.player).game.op) {
                    String s = "" + file.size;
                    if (file.size > 999) {
                        s = Util.instance.getTextReducedNumber(file.size, false, false, false);
                    }
                    msg.player.sendMessage(Component.literal("CustomNpcs").withStyle(TextFormatting.DARK_GREEN)
                            .append(Component.literal(": Received client script: \"").withStyle(TextFormatting.GRAY))
                            .append(Component.literal(normalFile.getAbsolutePath()).withStyle(TextFormatting.WHITE))
                            .append(Component.literal("\" (").withStyle(TextFormatting.GRAY))
                            .append(s)
                            .append(Component.literal("b)").withStyle(TextFormatting.GRAY))
                            .getParent());
                }
                // Put to session
                ScriptController.Instance.clients.put(msg.name, file.getDataText());
                ScriptController.Instance.clientSizes.put(msg.name, file.size);
                // save on client
                Util.instance.saveFile(normalFile, file.getDataText());
            }
            else { file.save(); }
            ClientProxy.loadFiles.remove(msg.name);
            Packets.sendServer(new SPacketRemoveLoadFile(msg.name));
        }
        ClientTickHandler.loadFiles();
    }

    private static void packetEventNames(PacketEventNames msg) {
        if (!msg.names.isEmpty()) {
            List<String> list;
            if (msg.type != (byte) 2) {
                for (Map.Entry<String, String> entry : msg.names.entrySet()) {
                    try {
                        Class<?> clazz = Class.forName(entry.getKey());
                        if (msg.type == (byte) 0) { ForgeEventHandler.clientEventNames.put(clazz, entry.getValue()); }
                        else { ForgeEventHandler.eventNames.put(clazz, entry.getValue()); }
                    }
                    catch (Exception ignored) {}
                }
                if (msg.type == (byte) 0) { list = new ArrayList<>(ForgeEventHandler.clientEventNames.values()); }
                else { list = new ArrayList<>(ForgeEventHandler.eventNames.values()); }
            }
            else { list = new ArrayList<>(msg.names.keySet()); }
            Collections.sort(list);
            String pre = "";
            StringBuilder text = new StringBuilder();
            for (String name : list) {
                if (pre.isEmpty()) { pre = "" + name.charAt(0); }
                else if (!pre.equals("" + name.charAt(0))) {
                    text.append(System.lineSeparator());
                    pre = "" + name.charAt(0);
                }
                text.append(name);
                text.append(System.lineSeparator());
            }
            File file = new File(CustomNpcs.Dir.getParentFile().getParentFile().getParentFile(), "all "+(msg.type == (byte) 0 ? "client" : msg.type == (byte) 1 ? "forge" : "api" )+ " event names.txt");
            Util.instance.saveFile(file, text.toString());
            msg.player.sendMessage(Component.literal("CustomNpcs").withStyle(TextFormatting.DARK_GREEN)
                    .append(Component.literal(": Save event names to file: ").withStyle(TextFormatting.GRAY))
                    .append(file.getAbsolutePath()).getParent());
        }
    }

    private static void packetRemoteNpcsEntity(PacketRemoteNpcsEntity msg) {
        Entity entity = EntityList.createEntityFromNBT(msg.data, msg.player.world);
        if (entity != null) {
            if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).ais.setStartPos(msg.npc.getPosition()); }
            entity.readFromNBT(msg.data);
            entity.setPosition(0.0d, 0.0d, 0.0d);
            if (minecraft.currentScreen instanceof GuiNpcRemoteEditor) {
                ((GuiNpcRemoteEditor) minecraft.currentScreen).selectEntity = entity;
                minecraft.currentScreen.initGui();
            }
        }
    }

    @SuppressWarnings("all")
    private static void packetSkin(PacketSkin msg) {
        switch (msg.type) {
            case 0: {
                GameProfile profile = msg.player.getGameProfile();
                Property property = Iterables.getFirst(profile.getProperties().get("textures"), null);
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = ImmutableMap.of();
                if (property != null) {
                    try {
                        String json = new String(Base64.getDecoder().decode(property.getValue()), StandardCharsets.UTF_8);
                        CustomTexturesPayload ctp = (CustomTexturesPayload) gson.fromJson(json, MinecraftTexturesPayload.class);
                        map = ctp.textures;
                    }
                    catch (JsonParseException ignored) {}
                }
                NBTTagCompound compound = new NBTTagCompound();
                compound.setUniqueId("UUID", msg.player.getUniqueID());
                NBTTagList list = new NBTTagList();
                for (MinecraftProfileTexture.Type t : MinecraftProfileTexture.Type.values()) {
                    ResourceLocation location;
                    if (map.containsKey(t)) {
                        MinecraftProfileTexture mpt = map.get(t);
                        String sha1 = Hashing.sha1().hashUnencodedChars(mpt.getHash()).toString();
                        switch (t) {
                            case CAPE: location = new ResourceLocation("capes/" + sha1); break;
                            case ELYTRA: location = new ResourceLocation("elytra/" + sha1); break;
                            default: location = new ResourceLocation("skins/" + sha1); break;
                        }
                        SkinData skinData = SkinData.create(t, location);
                        skinData.setIsDefault();
                        list.appendTag(skinData.save());
                    }
                    else if (t == MinecraftProfileTexture.Type.SKIN) {
                        UUID uuid = profile.getId();
                        if (uuid == null) { uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + profile.getName()).getBytes(StandardCharsets.UTF_8)); }
                        location = DefaultPlayerSkin.getDefaultSkin(uuid);
                        SkinData skinData = SkinData.create(t, location);
                        skinData.setIsDefault();
                        list.appendTag(skinData.save());
                    }
                }
                compound.setTag("Textures", list);
                Packets.sendServer(new SPacketSkin(compound));
                break;
            } // get skin
            case 1: {
                SkinUtil.resetSkin(PlayerSkinController.getInstance().loadPlayerSkin(msg.data));
                break;
            } // set
        }
    }

    private static void packetDetectHeldItem(PacketDetectHeldItem msg) {
        ItemStack stack = new ItemStack(msg.data);
        if (msg.slotID >= 0) { msg.player.inventory.setInventorySlotContents(msg.slotID, stack); }
        else { msg.player.inventory.setItemStack(stack); }
    }

    private static void packetDataGuiOpen(PacketDataGuiOpen msg) {
        try {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeNbt(msg.data);
            minecraft.displayGuiScreen(ClientProxy.getGui(msg.gui, NoppesUtilServer.getEditingNpc(msg.player), buffer));
        }
        catch (Exception e) { LogWriter.error("Error in gui: " + msg.gui, e); }
    }

    private static void packetScriptText(PacketScriptText msg) {
        Map<Integer, String[]> data = PacketScriptText.data;
        if (!data.containsKey(msg.tab)) { data.put(msg.tab, new String[msg.maxIDs]); }
        data.get(msg.tab)[msg.id] = msg.part;
        boolean done = true;
        StringBuilder total = new StringBuilder();
        for (String str : data.get(msg.tab)) {
            if (str == null) {
                done = false;
                break;
            }
            total.append(str);
        }
        if (done) {
            if (minecraft.currentScreen instanceof GuiScriptInterface) {
                ((GuiScriptInterface) minecraft.currentScreen).setTabScript(msg.tab, total.toString());
            }
            if (msg.isSetClient) {
                ScriptContainer container = ScriptController.Instance.clientScripts.getScripts().get(msg.tab);
                if (container != null) {
                    container.script = total.toString();
                    container.setInit(false);
                    ScriptController.Instance.clientScripts.init();
                    if (ScriptController.Instance.clientScripts.isEnabled()) {
                        msg.player.sendMessage(Component.translatable("scripts.client.received.server").getParent());
                    }
                }
            }
            data.remove(msg.tab);
        }
    }

    private static void packetScriptConsole(PacketScriptConsole msg) {
        Map<Integer, Map<Long, String[]>> data = PacketScriptConsole.data;
        if (!data.containsKey(msg.tab)) { data.put(msg.tab, new LinkedHashMap<>()); }
        if (!data.get(msg.tab).containsKey(msg.time)) { data.get(msg.tab).put(msg.time, new String[msg.maxIDs]); }
        data.get(msg.tab).get(msg.time)[msg.id] = msg.part;
        boolean done = true;
        StringBuilder total = new StringBuilder();
        for (String str : data.get(msg.tab).get(msg.time)) {
            if (str == null) {
                done = false;
                break;
            }
            total.append(str);
        }
        if (done) {
            if (minecraft.currentScreen instanceof GuiScriptInterface) {
                ((GuiScriptInterface) minecraft.currentScreen).setTabConsole(msg.tab, msg.time, total.toString());
            }
            if (msg.isSetClient) {
                ScriptContainer container = ScriptController.Instance.clientScripts.getScripts().get(msg.tab);
                if (container != null) {
                    container.script = total.toString();
                    container.setInit(false);
                    ScriptController.Instance.clientScripts.init();
                }
            }
            data.get(msg.tab).remove(msg.time);
            if (data.get(msg.tab).isEmpty()) { data.remove(msg.tab); }
        }
    }

    private static void packetDebug(PacketDebug msg) {
        if (msg.isLogOrClear) { CustomNpcs.debugData.logging(); }
        else { CustomNpcs.debugData.clear(); }
    }

    private static void packetCustomNBT(PacketCustomNBT msg) {
        EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.PACKAGE_FROM,
                new PlayerEvent.PlayerPackage(PlayerData.get(msg.player).scriptData.getIPlayer(), msg.data));
    }

    private static void packetStopSound(PacketStopSound msg) {
        MusicController.Instance.stopSound(msg.sound, SoundCategory.values()[msg.category % SoundCategory.values().length]);
    }

    private static void packetClearMarcets() {
        if (MarcetController.isNotLocalServerData()) {
            MarcetController.getInstance().markets.clear();
            MarcetController.getInstance().deals.clear();
        }
    }

    private static void packetUpdateMarcetGui() {
        if (minecraft.currentScreen instanceof IGuiData) {
            ((IGuiData) minecraft.currentScreen).setGuiData(new NBTTagCompound());
        }
    }

    private static void packetDealData(PacketDealData msg) {
        if (MarcetController.isNotLocalServerData()) { MarcetController.getInstance().loadDeal(msg.data); }
        if (minecraft.currentScreen instanceof IGuiData) { ((IGuiData) minecraft.currentScreen).setGuiData(new NBTTagCompound()); }
    }

    private static void packetMarcetData(PacketMarcetData msg) {
        if (MarcetController.isNotLocalServerData()) { MarcetController.getInstance().loadMarcet(msg.data); }
        if (minecraft.currentScreen instanceof IGuiData) { ((IGuiData) minecraft.currentScreen).setGuiData(new NBTTagCompound()); }
    }

    private static void packetMarcetRemove(PacketMarcetRemove msg) {
        if (MarcetController.isNotLocalServerData()) { MarcetController.getInstance().removeMarcet(msg.marcetID); }
        if (minecraft.currentScreen instanceof IGuiData) { ((IGuiData) minecraft.currentScreen).setGuiData(new NBTTagCompound()); }
    }

    private static void packetDealUpdate(PacketDealUpdate msg) {
        if (MarcetController.isNotLocalServerData()) {
            Marcet marcet = MarcetController.getInstance().getMarcet(msg.marcetID);
            if (marcet != null) {
                Deal deal = marcet.getDeal(msg.dealData.getInteger("DealID"));
                if (deal != null) { deal.loadData(msg.dealData); }
            }
        }
        if (minecraft.currentScreen instanceof IGuiData) { ((IGuiData) minecraft.currentScreen).setGuiData(new NBTTagCompound()); }
    }

    private static void packetMarcetClose(PacketMarcetClose msg) {
        Marcet m = MarcetController.getInstance().getMarcet(msg.marcetID);
        if (m != null) {
            if (minecraft.currentScreen instanceof GuiNPCTrader &&
                    GuiNPCTrader.marcet != null &&
                    GuiNPCTrader.marcet.getId() == msg.marcetID) { ((GuiNPCTrader) minecraft.currentScreen).onClose(); }
        }
    }

    private static void packetOpenCase(PacketOpenCase msg) {
        if (!msg.map.isEmpty() && minecraft.currentScreen instanceof GuiContainer) {
            minecraft.displayGuiScreen(new GuiOpenCase((GuiContainer) minecraft.currentScreen, msg.dealID, msg.map));
        }
    }

    private static void packetPermissionMenu(PacketPermissionMenu msg) {
        if (minecraft.currentScreen instanceof INpcMenuGui) {
            ((INpcMenuGui) minecraft.currentScreen).setMenuData(msg.display, msg.stats, msg.ai, msg.inventory, msg.advanced);
        }
    }

    private static void packetPermissionGlobal(PacketPermissionGlobal msg) {
        if (minecraft.currentScreen instanceof GuiNpcGlobalMainMenu) {
            ((GuiNpcGlobalMainMenu) minecraft.currentScreen).setMenuData(msg.admin, msg.banks, msg.factions, msg.dialogs, msg.quests,
                    msg.transports, msg.players_data, msg.recipes, msg.natural_spawns, msg.linkeds, msg.markets, msg.auctions,
                    msg.mails, msg.elements, msg.dungeons, msg.permissions);
        }
    }

    private static void packetDropTemplateClear() { DropController.getInstance().templates.clear(); }

    private static void packetDropTemplateSave(PacketDropTemplateSave msg) {
        if (msg.data.hasKey("Name", 8)) {
            DropsTemplate template = new DropsTemplate(msg.data.getCompoundTag("Groups"));
            DropController.getInstance().templates.put(msg.data.getString("Name"), template);
        }
    }

    private static void packetOverworldTime(PacketOverworldTime msg) {
        PlayerData.get(msg.player).questData.overworldTime = msg.overworldTime;
    }

    private static void packetBankSetPlayer(PacketBankSetPlayer msg) {
        ContainerNPCBank.editPlayerBankData = msg.name.isEmpty() ? null : msg.name;
    }

    private static void packetBankClearPos() {
        GuiNPCBankChest.startXMouse = 0;
        GuiNPCBankChest.startYMouse = 0;
    }

    private static void packetBankReOpen() {
        if (minecraft.currentScreen instanceof GuiNPCBankChest) {
            GuiNPCBankChest gui = (GuiNPCBankChest) minecraft.currentScreen;
            gui.isWait = true;
            Packets.sendServer(new SPacketBankOpen(gui.menu.data.bank.id, gui.menu.ceil, gui.ceilPos, gui.scrollY, gui.ceilsUpdate));
        }
    }

    private static void packetBankSave(PacketBankSave msg) {
        int id = msg.data.getInteger("BankID");
        if (id >= 0) {
            Bank bank = BankController.getInstance().getBank(id);
            if (bank == null) { bank = BankController.getInstance().addNewBank(); }
            bank.load(msg.data);
            PlayerData.get(msg.player).bankData.lastBank = null;
        }
    }

    private static void packetSaveSchematic(PacketSaveSchematic msg) {
        Schematic schema = new Schematic("");
        schema.load(msg.data);
        schema.save(msg.player);
        SchematicController.Instance.map.put(schema.getName(), new SchematicWrapper(schema));
    }

    private static void packetScriptError(PacketScriptError msg) {
        if (CustomNpcs.DisplayErrorInChat && msg.component != null && !msg.component.getFormattedText().isEmpty()) {
            msg.player.sendMessage(msg.component);
        }
    }

    private static void packetNpcNavigation(PacketNpcNavigation msg) {
        Entity entity = msg.player.world.getEntityByID(msg.entityId);
        if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).navigating = msg.path; }
    }

    private static void packetNpcTarget(PacketNpcTarget msg) {
        Entity entity = msg.player.world.getEntityByID(msg.entityId);
        if (entity instanceof EntityNPCInterface) {
            EntityNPCInterface npc = (EntityNPCInterface) entity;
            if (msg.targetId > -1) {
                Entity target = npc.world.getEntityByID(msg.targetId);
                if (target instanceof EntityLivingBase) { npc.setAttackTarget((EntityLivingBase) target); }
                else { npc.setAttackTarget(null); }
            }
            else { npc.setAttackTarget(null); }
        }
    }

    private static void packetMenuSave(PacketMenuSave msg) {
        Entity entity = msg.player.world.getEntityByID(msg.npcId);
        if (entity instanceof EntityNPCInterface) {
            EntityNPCInterface cnpc = (EntityNPCInterface) entity;
            switch (msg.type) {
                case DISPLAY: cnpc.display.load(msg.data); break;
                case STATS: cnpc.stats.load(msg.data); break;
                case INVENTORY: cnpc.inventory.load(msg.data); break;
                case AI: {
                    cnpc.ais.load(msg.data);
                    ClientEventHandler.movingPath.clear();
                    break;
                }
                case ADVANCED: cnpc.advanced.load(msg.data); break;
                case MODEL: ((EntityCustomNpc) cnpc).modelData.load(msg.data); break;
                case TRANSFORM: cnpc.transform.loadOptions(msg.data); break;
                case MOVING_PATH: {
                    cnpc.ais.setMovingPath(NBTTags.getIntegerArraySet(msg.data.getTagList("MovingPathNew", 10)));
                    ClientEventHandler.movingPath.clear();
                    break;
                }
                case MARK: MarkData.get(cnpc).setNBT(msg.data); break;
            }
        }
    }

    private static void packetCustomAnimationSet(PacketCustomAnimationSet msg) {
        if (msg.player.world.provider.getDimension() == msg.dimension) {
            if (msg.isPlayer) { // is Player
                IEntityPlayerMixin pl = (IEntityPlayerMixin) msg.player.world.getPlayerEntityByUUID(msg.uuid);
                if (pl != null) { pl.npcs$getAnimation().load(msg.data); }
            }
            else { // is NPC
                Entity entity = msg.player.world.getEntityByID(msg.id);
                if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).animation.load(msg.data); }
            }
        }
    }

    private static void packetCustomAnimationStop(PacketCustomAnimationStop msg) {
        if (msg.player.world.provider.getDimension() == msg.dimension) {
            if (msg.isPlayer) { // is Player
                IEntityPlayerMixin pl = (IEntityPlayerMixin) msg.player.world.getPlayerEntityByUUID(msg.uuid);
                if (pl != null) { pl.npcs$getAnimation().stopAnimation(); }
            }
            else { // is NPC
                Entity entity = msg.player.world.getEntityByID(msg.id);
                if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).animation.stopAnimation(); }
            }
        }
    }

    private static void packetCustomAnimationBaseSet(PacketCustomAnimationBaseSet msg) {
        if (msg.player.world.provider.getDimension() == msg.dimension) {
            if (msg.isPlayer) { // is Player
                IEntityPlayerMixin pl = (IEntityPlayerMixin) msg.player.world.getPlayerEntityByUUID(msg.uuid);
                if (pl != null) { pl.npcs$getAnimation().loadBaseAnimations(msg.map); }
            }
            else { // is NPC
                Entity entity = msg.player.world.getEntityByID(msg.id);
                if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).animation.loadBaseAnimations(msg.map); }
            }
        }
    }

    private static void packetCustomAnimationRun(PacketCustomAnimationRun msg) {
        if (msg.player.world.provider.getDimension() == msg.dimension) {
            AnimationConfig ac = AnimationController.getInstance().getAnimation(msg.animId);
            if (ac != null) {
                if (msg.isPlayer) { // is Player
                    IEntityPlayerMixin pl = (IEntityPlayerMixin) msg.player.world.getPlayerEntityByUUID(msg.uuid);
                    if (pl != null) { pl.npcs$getAnimation().tryRunAnimation(ac, msg.animType); }
                } else { // is NPC
                    Entity entity = msg.player.world.getEntityByID(msg.id);
                    if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).animation.tryRunAnimation(ac, msg.animType); }
                }
            }
        }
    }

    private static void packetCustomEmotionStop(PacketCustomEmotionStop msg) {
        if (msg.player.world.provider.getDimension() == msg.dimension) {
            if (msg.isPlayer) { // is Player
                IEntityPlayerMixin pl = (IEntityPlayerMixin) msg.player.world.getPlayerEntityByUUID(msg.uuid);
                if (pl != null) { pl.npcs$getAnimation().stopEmotion(); }
            }
            else { // is NPC
                Entity entity = msg.player.world.getEntityByID(msg.id);
                if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).animation.stopEmotion(); }
            }
        }
    }

    private static void packetCustomEmotionRun(PacketCustomEmotionRun msg) {
        if (msg.player.world.provider.getDimension() == msg.dimension) {
            EmotionConfig ec = AnimationController.getInstance().getEmotion(msg.emtnId);
            if (ec != null) {
                if (msg.isPlayer) { // is Player
                    IEntityPlayerMixin pl = (IEntityPlayerMixin) msg.player.world.getPlayerEntityByUUID(msg.uuid);
                    if (pl != null) { pl.npcs$getAnimation().tryRunEmotion(ec); }
                } else { // is NPC
                    Entity entity = msg.player.world.getEntityByID(msg.id);
                    if (entity instanceof EntityNPCInterface) { ((EntityNPCInterface) entity).animation.tryRunEmotion(ec); }
                }
            }
        }
    }

    private static void packetNpcInitData(PacketNpcInitData msg) {
        Entity e = msg.player.world.getEntityByID(msg.npcId);
        if (e instanceof EntityNPCInterface
                && !(minecraft.currentScreen instanceof INpcMenuGui && NoppesUtilServer.getEditingNpc(msg.player) == e)) {
            e.readFromNBT(msg.compound);
        }
    }

    private static void packetNpcLookPos(PacketNpcLookPos msg) {
        if (msg.player.world.provider.getDimension() == msg.dimensionId) {
            Entity e = msg.player.world.getEntityByID(msg.npcId);
            if (e instanceof EntityNPCInterface) {
                if (msg.lookId < 0) { ((EntityNPCInterface) e).lookAt = null; }
                else { ((EntityNPCInterface) e).lookAt = msg.player.world.getEntityByID(msg.lookId); }
            }
        }
    }

    private static void packetCustomChestName(PacketCustomChestName msg) {
        if (minecraft.currentScreen instanceof GuiCustomChest) {
            ((GuiCustomChest) minecraft.currentScreen).title = Component.translatable(msg.name).getFormattedText();
        }
    }

    private static void packetNpcRarityTitleSet(PacketNpcRarityTitleSet msg) {
        Entity e = msg.player.world.getEntityByID(msg.npcId);
        if (e instanceof EntityNPCInterface) {
            if (minecraft.currentScreen instanceof INpcMenuGui && NoppesUtilServer.getEditingNpc(msg.player) == e) { return; }
            ((EntityNPCInterface) e).stats.setLevel(msg.compound.getInteger("NPCLevel"));
            ((EntityNPCInterface) e).stats.setRarity(msg.compound.getInteger("NPCRarity"));
            ((EntityNPCInterface) e).stats.setRarityTitle(msg.compound.getString("RarityTitle"));
        }
    }

    private static void packetBorderData(PacketBorderData msg) { BorderController.getInstance().loadRegion(msg.data); }

    private static void packetBorderClear() { BorderController.getInstance().regions.clear(); }

    private static void packetHeapAnalyzer(PacketHeapAnalyzer msg) {
        switch (msg.type) {
            case START: CmdHeapAnalyzer.startTracking(null, msg.count); break;
            case STOP: CmdHeapAnalyzer.stopTracking(null, msg.count); break;
            case MANUAL: CmdHeapAnalyzer.doManual(null, msg.count); break;
        }
    }

    @SuppressWarnings("unchecked")
    private static void updateMinimap(PacketSyncUpdate msg) {
        PlayerMiniMapData mm = PlayerData.get(msg.player).minimap;
        mm.load(msg.data);
        int isChanged = 0;
        if (mm.modName.endsWith("journeymap")) {
            try {
                Class<?> ws = Class.forName("journeymap.client.waypoint.WaypointStore");
                Class<?> wp = Class.forName("journeymap.client.model.Waypoint");
                Constructor<?> wc = null;
                for (Constructor<?> c : wp.getDeclaredConstructors()) {
                    if (c.getParameterCount() == 12) {
                        Parameter[] ps = c.getParameters();
                        if (ps[0].getType() == String.class && ps[1].getType() == int.class
                                && ps[2].getType() == int.class && ps[3].getType() == int.class
                                && ps[4].getType() == boolean.class && ps[5].getType() == int.class
                                && ps[6].getType() == int.class && ps[7].getType() == int.class
                                && ps[8].getType().getSimpleName().equals("Type") && ps[9].getType() == String.class
                                && ps[10].getType() == Integer.class && ps[11].getType() == Collection.class) {
                            wc = c;
                            break;
                        }
                    }
                }
                Field cacheField = ws.getDeclaredField("cache");
                Field groupCacheField = ws.getDeclaredField("groupCache");
                Field dimensionsField = ws.getDeclaredField("dimensions");
                Method load = null, remove = null;
                for (Method m : ws.getDeclaredMethods()) {
                    if (m.getName().equals("load") && m.getParameterCount() == 2
                            && m.getParameters()[0].getType() == Collection.class
                            && m.getParameters()[1].getType() == boolean.class) {
                        load = m;
                    }
                    if (m.getName().equals("remove") && m.getParameterCount() == 1
                            && m.getParameters()[0].getType().getSimpleName().equals("Waypoint")) {
                        remove = m;
                    }
                }
                if (!cacheField.isAccessible()) { cacheField.setAccessible(true); }
                if (!groupCacheField.isAccessible()) { groupCacheField.setAccessible(true); }
                if (!dimensionsField.isAccessible()) { dimensionsField.setAccessible(true); }
                Object waypointStore = ws.getEnumConstants()[0];

                // Clear OLD
                Set<Integer> dimensions = (Set<Integer>) dimensionsField.get(waypointStore);
                dimensions.clear();
                Cache<Long, Object> groupCache = (Cache<Long, Object>) groupCacheField.get(waypointStore);
                groupCache.invalidateAll();
                Cache<String, Object> cache = (Cache<String, Object>) cacheField.get(waypointStore);
                Map<String, Object> map = cache.asMap();
                for (String name : map.keySet()) {
                    assert remove != null;
                    remove.invoke(waypointStore, map.get(name));
                }
                cache.invalidateAll();

                // Create and Add new
                isChanged = 1;
                List<Object> waypoints = new ArrayList<>();
                for (MiniMapData mmd : mm.points) {
                    int dimID = mmd.dimIDs.get(0);
                    Object t = null;
                    for (Object enumType : wp.getClasses()[0].getEnumConstants()) {
                        if (t == null) { t = enumType; }
                        if (enumType.toString().equalsIgnoreCase(mmd.type)) {
                            t = enumType;
                            break;
                        }
                    }
                    if (t == null) {
                        continue;
                    }
                    int x = (int) mmd.pos.getX();
                    int y = (int) mmd.pos.getY();
                    int z = (int) mmd.pos.getZ();
                    Color color = new Color(mmd.color);
                    List<Integer> dim = new ArrayList<>(mmd.dimIDs);
                    assert wc != null;
                    Object waypoint = wc.newInstance(mmd.name, x, y, z, mmd.isEnable, color.getRed(), color.getGreen(), color.getBlue(), t, "journeymap", dimID, dim);
                    wp.getDeclaredMethod("setIcon", String.class).invoke(waypoint, mmd.icon);
                    waypoints.add(waypoint);
                }
                assert load != null;
                load.invoke(waypointStore, waypoints, true);
            }
            catch (Exception e) { isChanged = 2; }
        }
        else if (mm.modName.endsWith("xaerominimap")) {
            try {
                Class<?> xms = Class.forName("xaero.common.XaeroMinimapSession");
                Object minimapSession = xms.getDeclaredMethod("getCurrentSession").invoke(xms); // XaeroMinimapSession
                Object waypointsManager = xms.getDeclaredMethod("getWaypointsManager").invoke(minimapSession); // WaypointsManager
                Method getWaypointMap = waypointsManager.getClass().getDeclaredMethod("getWaypointMap");
                HashMap<String, Object> waypointMap = (HashMap<String, Object>) getWaypointMap
                        .invoke(waypointsManager);
                String mainContainerID = (String) waypointsManager.getClass()
                        .getDeclaredMethod("getAutoRootContainerID").invoke(waypointsManager);
                Object wwrc = waypointMap.get(mainContainerID);// WaypointWorldRootContainer
                Field fwwrc = wwrc.getClass().getDeclaredField("dimensionTypes");
                fwwrc.setAccessible(true);
                Int2ObjectMap<Object> dimensionTypes = (Int2ObjectMap<Object>) fwwrc.get(wwrc);
                boolean saveConfig = false;
                for (int dim : DimensionManager.getStaticDimensionIDs()) {
                    if (!dimensionTypes.containsKey(dim)) {
                        World ret = DimensionManager.getWorld(dim, true);
                        if (ret == null) {
                            DimensionManager.initDimension(dim);
                            ret = DimensionManager.getWorld(dim);
                        }
                        dimensionTypes.put(dim, wwrc.getClass().getDeclaredMethod("createDimensionType", World.class).invoke(wwrc, ret)); // WaypointDimensionTypeInfo
                        saveConfig = true;
                    }
                }
                if (saveConfig) { wwrc.getClass().getDeclaredMethod("saveConfig").invoke(wwrc); }
                Class<?> xm = Class.forName("xaero.minimap.XaeroMinimap");
                Object instance = xm.getField("instance").get(xm);
                File parentFile = (File) xm.getDeclaredMethod("getWaypointsFolder").invoke(instance);
                String world_name = (String) mm.addData.get("xaero_world_name");
                if (world_name == null || world_name.isEmpty()) {
                    HashMap<String, Object> dimMap = (HashMap<String, Object>) wwrc.getClass()
                            .getField("subContainers").get(wwrc);
                    for (String k : dimMap.keySet()) {
                        world_name = (String) dimMap.get(k).getClass().getDeclaredMethod("getKey").invoke(dimMap.get(k));
                    }
                }
                assert world_name != null;
                File worldDir = new File(parentFile, world_name);
                Gson gson = new Gson();
                Map<File, TempWaypointText> map = new HashMap<>();
                for (MiniMapData mmd : mm.points) {
                    if (mmd.gsonData.containsKey("temporary") && gson.fromJson(mmd.gsonData.get("temporary"), boolean.class)) { continue; }
                    int dimID = mmd.dimIDs.get(0);
                    File dimDir = new File(worldDir, "dim%" + dimID);
                    if (dimDir.exists() || dimDir.mkdirs()) {
                        File dimFile = new File(dimDir, "/waypoints.txt");
                        StringBuilder text = new StringBuilder();
                        StringBuilder endText = new StringBuilder();
                        if (!map.containsKey(dimFile)) {
                            if (!dimFile.exists()) {
                                text.append("#" + ((char) 10));
                                text.append("#waypoint:name:initials:x:y:z:color:disabled:type:set:rotate_on_tp:tp_yaw:visibility_type:destination" + ((char) 10));
                                text.append("#" + ((char) 10));
                            }
                            else {
                                FileInputStream inputStream = new FileInputStream(dimFile);
                                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                                    boolean end = false;
                                    for (String line = br.readLine(); line != null; line = br.readLine()) {
                                        if (!end && line.indexOf("#") != 0) {
                                            end = true;
                                            continue;
                                        }
                                        if (line.indexOf("waypoint:") == 0) {
                                            continue;
                                        }
                                        if (end) {
                                            endText.append(line).append((char) 10);
                                        } else {
                                            text.append(line).append((char) 10);
                                        }
                                    }
                                }
                            }
                            map.put(dimFile, new TempWaypointText(text.toString(), endText.toString()));
                        }
                    }
                }
                for (File dimFile : map.keySet()) { Util.instance.saveFile(dimFile, map.get(dimFile).getString()); }
                Object settings = xm.getDeclaredMethod("getSettings").invoke(instance); // ModSettings
                Method loadWaypointsFromAllSources = null;
                for (Method m : settings.getClass().getDeclaredMethods()) {
                    if (m.getName().equals("loadWaypointsFromAllSources") && m.getParameterCount() == 1) {
                        loadWaypointsFromAllSources = m;
                        break;
                    }
                }
                assert loadWaypointsFromAllSources != null;
                loadWaypointsFromAllSources.invoke(settings, waypointsManager);
            }
            catch (Exception e) {
                LogWriter.error("Error:", e);
                isChanged = 2;
            }
        }
        else if (mm.modName.endsWith("voxelmap")) {
            try {
                Class<?> vm = Class.forName("com.mamiyaotaru.voxelmap.VoxelMap");
                Object instance = vm.getMethod("getInstance").invoke(vm);
                Object waypointManager = vm.getMethod("getWaypointManager").invoke(instance);
                List<Object> waypoints = (List<Object>) waypointManager.getClass().getMethod("getWaypoints").invoke(waypointManager);
                // Clear OLD
                waypoints.clear();
                // Create and Add new
                isChanged = 1;
                Class<?> wc = Class.forName("com.mamiyaotaru.voxelmap.util.Waypoint");
                Constructor<?> cw = wc.getConstructor(String.class, int.class, int.class, int.class, boolean.class, float.class, float.class, float.class, String.class, String.class, TreeSet.class);
                for (MiniMapData mmd : mm.points) {
                    int dimID = mmd.dimIDs.get(0);
                    int x = (int) mmd.pos.getX();
                    int y = (int) mmd.pos.getY();
                    int z = (int) mmd.pos.getZ();
                    Color color = new Color(mmd.color);
                    TreeSet<Integer> dim = new TreeSet<>(mmd.dimIDs);
                    String worldName = mmd.gsonData.getOrDefault("voxel_world_name", "");
                    if (worldName.isEmpty()) {
                        World ret = DimensionManager.getWorld(dimID, true);
                        if (ret == null) {
                            DimensionManager.initDimension(dimID);
                            ret = DimensionManager.getWorld(dimID);
                        }
                        worldName = ret.getProviderName();
                    }
                    Object waypoint = cw.newInstance(mmd.name, x, z, y, mmd.isEnable(), color.getRed() / 255.0f,
                            color.getGreen() / 255.0f, color.getBlue() / 255.0f, mmd.icon, worldName, dim);
                    waypoints.add(waypoint);
                }
                waypointManager.getClass().getMethod("saveWaypoints").invoke(waypointManager);
                Method loadWaypoints = waypointManager.getClass().getDeclaredMethod("loadWaypoints");
                loadWaypoints.setAccessible(true);
                loadWaypoints.invoke(waypointManager);
                // remove any dimension points;
                Method med = waypointManager.getClass().getDeclaredMethod("enteredDimension", int.class);
                med.setAccessible(true);
                med.invoke(waypointManager, msg.player.world.provider.getDimension());
            }
            catch (Exception e) {
                LogWriter.error("Error:", e);
                isChanged = 2;
            }
        }
        if (isChanged != 0) {
            msg.player.sendMessage(Component.translatable("minimap.set.points." + isChanged, ChatFormatting.GRAY + mm.modName).getParent());
        }
    }

    public static class TempWaypointText {

        public String text;
        String endText;

        public TempWaypointText(String t, String e) {
            text = t;
            endText = e;
        }

        public String getString() { return text + endText; }

    }

}
