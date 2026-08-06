package noppes.npcs.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor.TargetPoint;
import net.minecraftforge.network.simple.SimpleChannel;
import noppes.npcs.CustomNpcs;
import noppes.npcs.packets.client.*;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.common.PacketBasic;
import noppes.npcs.shared.common.PacketServerBasic;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Packets {

   protected static final String PROTOCOL = "CNPCS";
   protected static final Map<Integer, SimpleChannel> CHANNELS = new HashMap<>();
   protected static int totalIndex;
   protected static int channelId;
   protected static int index;

   // New Unofficial (BetaZavr)
   private static final Map<Object, Long> delaySendMap = new HashMap<>();
   private static final List<Class<? extends PacketBasic>> ignoredDebug = new ArrayList<>();
   static {
      ignoredDebug.add(SPacketPlayerMousePressed.class);
      ignoredDebug.add(SPacketPlayerKeyPressed.class);
      ignoredDebug.add(SPacketPlayerSound.class);
      ignoredDebug.add(SPacketPlayerIsMoved.class);
      ignoredDebug.add(SPacketPlayerScreen.class);
      ignoredDebug.add(SPacketScriptConsole.class);
      ignoredDebug.add(SPacketScriptText.class);
      ignoredDebug.add(SPacketGetMovingPath.class);
      ignoredDebug.add(SPacketNpcRarityTitleGet.class);
      ignoredDebug.add(SPacketQuestCompletionCheckAll.class);

      ignoredDebug.add(PacketScriptConsole.class);
      ignoredDebug.add(PacketScriptText.class);
      ignoredDebug.add(PacketSyncUpdate.class);
      ignoredDebug.add(PacketEyeBlink.class);
      ignoredDebug.add(PacketMenuSave.class);
      ignoredDebug.add(PacketNpcNavigation.class);
      ignoredDebug.add(PacketNpcTarget.class);
      ignoredDebug.add(PacketNpcRarityTitleSet.class);
      ignoredDebug.add(PacketSync.class);
      ignoredDebug.add(PacketCustomAnimationBaseSet.class);
      ignoredDebug.add(PacketCustomAnimationRun.class);
      ignoredDebug.add(PacketCustomAnimationStop.class);
      ignoredDebug.add(PacketPlaySound.class);
   }

   public static void register() {
      totalIndex = 0;
      channelId = 0;
      index = 0;
      // Server -> Client
      register(PacketAchievement.class);
      register(PacketChat.class);
      register(PacketChatBubble.class);
      register(PacketConfigFont.class);
      register(PacketDialog.class);
      register(PacketDialogDummy.class);
      register(PacketEyeBlink.class);
      register(PacketGuiCloneOpen.class);
      register(PacketGuiClose.class);
      register(PacketGuiData.class);
      register(PacketGuiComponentUpdate.class);
      register(PacketGuiError.class);
      register(PacketGuiOpen.class);
      register(PacketGuiScrollData.class);
      register(PacketGuiScrollList.class);
      register(PacketGuiScrollSelected.class);
      register(PacketGuiUpdate.class);
      register(PacketItemUpdate.class);
      register(PacketMarkData.class);
      register(PacketNpcDelete.class);
      register(PacketNpcEdit.class);
      register(PacketNpcRole.class);
      register(PacketNpcUpdate.class);
      register(PacketParticle.class);
      register(PacketPlayMusic.class);
      register(PacketPlaySound.class);
      register(PacketQuestCompletion.class);
      register(PacketSync.class);
      register(PacketSyncRemove.class);
      register(PacketSyncUpdate.class);
      register(PacketNpcVisibleFalse.class);
      register(PacketNpcVisibleTrue.class);
      register(PacketGuiParts.class);
      register(PacketSoundGUIOpen.class);
      register(PacketNpcRotationUpdate.class);

      // New Unofficial (Goodbird)
      register(PacketUpdatePhysics.class);
      register(PacketOverlayShow.class);
      register(PacketOverlayHide.class);
      register(PacketHideAllOverlays.class);

      // New Unofficial (BetaZavr)
      register(PacketSpeak.class);
      register(PacketClientScripts.class);
      register(PacketSendFileList.class);
      register(PacketSendFilePart.class);
      register(PacketEventNames.class);
      register(PacketRemoteNpcsEntity.class);
      register(PacketSkin.class);
      register(PacketDetectHeldItem.class);
      register(PacketDataGuiOpen.class);
      register(PacketScriptText.class);
      register(PacketScriptConsole.class);
      register(PacketDebug.class);
      register(PacketCustomNBT.class);
      register(PacketStopSound.class);
      register(PacketClearMarcets.class);
      register(PacketUpdateMarcetGui.class);
      register(PacketDealData.class);
      register(PacketMarcetData.class);
      register(PacketMarcetRemove.class);
      register(PacketDealUpdate.class);
      register(PacketMarcetClose.class);
      register(PacketOpenCase.class);
      register(PacketPermissionMenu.class);
      register(PacketPermissionGlobal.class);
      register(PacketDropTemplateClear.class);
      register(PacketDropTemplateSave.class);
      register(PacketOverworldTime.class);
      register(PacketBankSetPlayer.class);
      register(PacketBankClearPos.class);
      register(PacketBankReOpen.class);
      register(PacketBankSave.class);
      register(PacketSaveSchematic.class);
      register(PacketScriptError.class);
      register(PacketNpcNavigation.class);
      register(PacketNpcTarget.class);
      register(PacketMenuSave.class);
      register(PacketCustomAnimationSet.class);
      register(PacketCustomAnimationStop.class);
      register(PacketCustomAnimationBaseSet.class);
      register(PacketCustomAnimationRun.class);
      register(PacketCustomEmotionStop.class);
      register(PacketCustomEmotionRun.class);
      register(PacketNpcInitData.class);
      register(PacketNpcLookPos.class);
      register(PacketNpcRarityTitleSet.class);
      register(PacketBorderData.class);
      register(PacketBorderClear.class);
      register(PacketHeapAnalyzer.class);

      // Client -> Server
      register(SPacketBankGet.class);
      register(SPacketBankRemove.class);
      register(SPacketBankSave.class);
      register(SPacketBanksGet.class);
      register(SPacketBankUnlock.class);
      register(SPacketBankUpgrade.class);
      register(SPacketCloneList.class);
      register(SPacketCloneNameCheck.class);
      register(SPacketCloneRemove.class);
      register(SPacketCloneSave.class);
      register(SPacketCompanionOpenInv.class);
      register(SPacketCompanionTalentExp.class);
      register(SPacketDialogCategoryRemove.class);
      register(SPacketDialogRemove.class);
      register(SPacketDialogSelected.class);
      register(SPacketDimensionsGet.class);
      register(SPacketDimensionTeleport.class);
      register(SPacketFactionGet.class);
      register(SPacketFactionRemove.class);
      register(SPacketFactionSave.class);
      register(SPacketFactionsGet.class);
      register(SPacketFollowerExtend.class);
      register(SPacketFollowerHire.class);
      register(SPacketFollowerState.class);
      register(SPacketGuiOpen.class);
      register(SPacketLinkedAdd.class);
      register(SPacketLinkedGet.class);
      register(SPacketLinkedRemove.class);
      register(SPacketLinkedSet.class);
      register(SPacketMailSetup.class);
      register(SPacketMenuClose.class);
      register(SPacketMenuGet.class);
      register(SPacketMenuSave.class);
      register(SPacketNaturalSpawnGet.class);
      register(SPacketNaturalSpawnGetAll.class);
      register(SPacketNaturalSpawnRemove.class);
      register(SPacketNaturalSpawnSave.class);
      register(SPacketNbtBookBlockSave.class);
      register(SPacketNbtBookEntitySave.class);
      register(SPacketNpcDelete.class);
      register(SPacketNpcDialogRemove.class);
      register(SPacketNpcDialogSet.class);
      register(SPacketNpcDialogsGet.class);
      register(SPacketNpcFactionSet.class);
      register(SPacketNpcJobGet.class);
      register(SPacketNpcJobSave.class);
      register(SPacketNpcRoleCompanionUpdate.class);
      register(SPacketNpcRoleGet.class);
      register(SPacketNpcRoleSave.class);
      register(SPacketNpcTransform.class);
      register(SPacketNpcTransportGet.class);
      register(SPacketPlayerCloseContainer.class);
      register(SPacketPlayerDataGet.class);
      register(SPacketPlayerDataRemove.class);
      register(SPacketPlayerKeyPressed.class);
      register(SPacketPlayerLeftClicked.class);
      register(SPacketPlayerMailDelete.class);
      register(SPacketPlayerMailGet.class);
      register(SPacketPlayerMailOpen.class);
      register(SPacketPlayerMailRead.class);
      register(SPacketPlayerMailSend.class);
      register(SPacketPlayerTransport.class);
      register(SPacketQuestCategoryRemove.class);
      register(SPacketQuestCompletionCheck.class);
      register(SPacketQuestCompletionCheckAll.class);
      register(SPacketQuestDialogTitles.class);
      register(SPacketQuestOpen.class);
      register(SPacketQuestRemove.class);
      register(SPacketRecipeGroupSave.class);
      register(SPacketRecipeGroupRename.class);
      register(SPacketRecipeGroupRemove.class);
      register(SPacketRecipeSave.class);
      register(SPacketRecipeRename.class);
      register(SPacketRecipeRemove.class);
      register(SPacketRemoteFreeze.class);
      register(SPacketRemoteMenuOpen.class);
      register(SPacketRemoteNpcDelete.class);
      register(SPacketRemoteNpcReset.class);
      register(SPacketRemoteNpcsGet.class);
      register(SPacketRemoteNpcTp.class);
      register(SPacketSceneReset.class);
      register(SPacketSceneStart.class);
      register(SPacketSchematicsStore.class);
      register(SPacketSchematicsTileBuild.class);
      register(SPacketSchematicsTileGet.class);
      register(SPacketSchematicsTileSave.class);
      register(SPacketSchematicsTileSet.class);
      register(SPacketScriptGet.class);
      register(SPacketTileEntityGet.class);
      register(SPacketTileEntitySave.class);
      register(SPacketToolMounter.class);
      register(SPacketTransportCategoriesGet.class);
      register(SPacketTransportCategoryRemove.class);
      register(SPacketTransportCategorySave.class);
      register(SPacketTransportGet.class);
      register(SPacketTransportRemove.class);
      register(SPacketTransportSave.class);
      register(SPacketCustomGuiButton.class);
      register(SPacketCustomGuiButtonList.class);
      register(SPacketCustomGuiTextUpdate.class);
      register(SPacketCustomGuiSliderUpdate.class);
      register(SPacketCustomGuiFocusUpdate.class);
      register(SPacketCustomGuiScrollClick.class);
      register(SPacketCustomGuiSubGuiClosed.class);
      register(SPacketCustomGuiParts.class);
      register(SPacketNpRandomNameSet.class);
      register(SPacketPlayerSound.class);
      register(SPacketScriptSave.class);
      register(SPacketToolMobSpawner.class);
      register(SPacketQuestSave.class);
      register(SPacketQuestCategorySave.class);
      register(SPacketDialogSave.class);
      register(SPacketDialogCategorySave.class);
      register(SPacketOpenParts.class);

      // New Unofficial (Goodbird)
      register(SPacketScreenSize.class);

      // New from Unofficial (BetaZavr)
      register(SPacketPlayerScreen.class);
      register(SPacketGetFilePart.class);
      register(SPacketHudTimerEnd.class);
      register(SPacketRemoteNpcsEntity.class);
      register(SPacketGiveStack.class);
      register(SPacketNbtBookStackSave.class);
      register(SPacketSkin.class);
      register(SPacketQuestMinID.class);
      register(SPacketTeleportTo.class);
      register(SPacketSetSlotIndex.class);
      register(SPacketScriptText.class);
      register(SPacketScriptConsole.class);
      register(SPacketSyncUpdate.class);
      register(SPacketScriptRun.class);
      register(SPacketQuestRemoveActive.class);
      register(SPacketOpenEditClientScript.class);
      register(SPacketSaveClientScripts.class);
      register(SPacketDialogMinID.class);
      register(SPacketDialogCategoryGet.class);
      register(SPacketDialogOptionRemove.class);
      register(SPacketDialogOptionMove.class);
      register(SPacketDialogGuiSettings.class);
      register(SPacketCustomNBT.class);
      register(SPacketGetResistances.class);
      register(SPacketVillagerMenuOpen.class);
      register(SPacketMerchantSetSlot.class);
      register(SPacketMarcetsGet.class);
      register(SPacketMarcetDelete.class);
      register(SPacketDealDelete.class);
      register(SPacketMarcetSave.class);
      register(SPacketDealSave.class);
      register(SPacketTraderMarketBuy.class);
      register(SPacketTraderMarketSell.class);
      register(SPacketTraderMarketReset.class);
      register(SPacketMarketTime.class);
      register(SPacketTraderLivePlayer.class);
      register(SPacketPermissionsGet.class);
      register(SPacketPermissionsAdd.class);
      register(SPacketPermissionsDel.class);
      register(SPacketPermissionMenuGet.class);
      register(SPacketPermissionGlobalGet.class);
      register(SPacketPlayerFactionsGet.class);
      register(SPacketRegionRemove.class);
      register(SPacketRegionSave.class);
      register(SPacketRegionSetOnItem.class);
      register(SPacketPlayerMousePressed.class);
      register(SPacketNpcInvDropSetSave.class);
      register(SPacketDealDropSetSave.class);
      register(SPacketQuestDropSetSave.class);
      register(SPacketDropTemplateClear.class);
      register(SPacketDropTemplateSave.class);
      register(SPacketDropTemplateRemove.class);
      register(SPacketWindowSize.class);
      register(SPacketQuestChooseReward.class);
      register(SPacketPlayerDataSet.class);
      register(SPacketPlayerDataCleaning.class);
      register(SPacketBankOpen.class);
      register(SPacketBankClearCeil.class);
      register(SPacketBankLock.class);
      register(SPacketBankRegrade.class);
      register(SPacketBankResetCeil.class);
      register(SPacketBankOpenPlayer.class);
      register(SPacketGetBuildData.class);
      register(SPacketSetBuildData.class);
      register(SPacketKeyActive.class);
      register(SPacketContainerOpen.class);
      register(SPacketPlayerMailRansom.class);
      register(SPacketPlayerMailTakeMoney.class);
      register(SPacketPlayerMailReturn.class);
      register(SPacketPlayerIsMoved.class);
      register(SPacketResetItemMoving.class);
      register(SPacketGetMovingPath.class);
      register(SPacketNpcJobSpawnerAdd.class);
      register(SPacketNpcJobSpawnerRemove.class);
      register(SPacketNpcJobSpawnerMove.class);
      register(SPacketGetServerCloneEntity.class);
      register(SPacketScriptEncrypt.class);
      register(SPacketNpcInitData.class);
      register(SPacketNpcPuppetSave.class);
      register(SPacketCloneSet.class);
      register(SPacketAnimationGet.class);
      register(SPacketAnimationSave.class);
      register(SPacketAnimationChange.class);
      register(SPacketGetFirstID.class);
      register(SPacketMailsGet.class);
      register(SPacketMailsSave.class);
      register(SPacketDetectHeldItem.class);
      register(SPacketItemChange.class);
      register(SPacketDeadLootsGet.class);
      register(SPacketDeadLootsOpen.class);
      register(SPacketNpcRarityTitleGet.class);
      register(SPacketRemoveLoadFile.class);

   }

   private static <MSG> void register(Class<MSG> packetClass) {
      if (index > 0xFF) { index = 0; channelId++; }
      if (!CHANNELS.containsKey(channelId)) {
         String newProtocol = PROTOCOL + "_" + channelId;
         CHANNELS.put(channelId,
                 ChannelBuilder.named(new ResourceLocation(CustomNpcs.MODID, "packets_" + channelId))
                         .clientAcceptedVersions(newProtocol::equals)
                         .serverAcceptedVersions(newProtocol::equals)
                         .networkProtocolVersion(() -> newProtocol)
                         .simpleChannel());
      }
      try {
         Field channel = packetClass.getDeclaredField("channelId");
         channel.trySetAccessible();
         channel.set(null, channelId);

         Method encode = packetClass.getDeclaredMethod("encode", packetClass, FriendlyByteBuf.class);
         encode.trySetAccessible();
         Method decode = packetClass.getDeclaredMethod("decode", FriendlyByteBuf.class);
         decode.trySetAccessible();

         BiConsumer<MSG, FriendlyByteBuf> encoder = (pkt, buf) -> {
            try { encode.invoke(null, pkt, buf); }
            catch (Exception e) { throw new RuntimeException(e); }
         };
         @SuppressWarnings("unchecked")
         Function<FriendlyByteBuf, MSG> decoder = buf -> {
            try { return (MSG) decode.invoke(null, buf); }
            catch (Exception e) { throw new RuntimeException(e); }
         };
         LogWriter.debug(totalIndex + " -register packet["+index+"]; channel["+channelId+"] "+packetClass.getSimpleName()+"; Side: "
                 + (PacketServerBasic.class.isAssignableFrom(packetClass) ? "Server" : "Client"));
         CHANNELS.get(channelId).registerMessage(index++,
                 packetClass,
                 encoder,
                 decoder,
                 PacketServerBasic.class.isAssignableFrom(packetClass) ? PacketServerBasic::handle : PacketBasic::handle);
         totalIndex++;
      }
      catch (Exception e) {
         LogWriter.error("Error register packet["+index+"/"+totalIndex+"]: " + packetClass.getSimpleName()+
                 "; channelId: "+channelId+"; Side: "+(PacketServerBasic.class.isAssignableFrom(packetClass) ? "Server" : "Client"), e);
         throw new RuntimeException("Error register " + CustomNpcs.MODNAME + " packet " + packetClass.getSimpleName());
      }
   }

   public static <MSG extends PacketBasic> void send(ServerPlayer player, MSG msg) {
      logged(msg, true);
      CHANNELS.get(msg.getChannelId()).send(PacketDistributor.PLAYER.with(() -> player), msg);
   }

   public static <MSG extends PacketBasic> void sendDelayed(ServerPlayer player, MSG msg, int delay) {
      logged(msg, true);
      CustomNPCsScheduler.runTack(() -> CHANNELS.get(msg.getChannelId())
              .send(PacketDistributor.PLAYER.with(() -> player), msg), delay);
   }

   public static <MSG extends PacketBasic> void sendNearby(Level level, BlockPos pos, int range, MSG msg) {
      logged(msg, true);
      CHANNELS.get(msg.getChannelId()).send(PacketDistributor.NEAR
              .with(() -> new TargetPoint(pos.getX(), pos.getY(), pos.getZ(), range, level.dimension())), msg);
   }

   public static <MSG extends PacketBasic> void sendNearby(Entity entity, MSG msg) {
      logged(msg, true);
      CHANNELS.get(msg.getChannelId()).send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
   }

   public static <MSG extends PacketBasic> void sendAll(MSG msg) {
      logged(msg, true);
      CHANNELS.get(msg.getChannelId()).send(PacketDistributor.ALL.noArg(), msg);
   }

   public static <MSG extends PacketServerBasic> void sendServer(MSG msg) {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      if (connection != null) {
         logged(msg, false);
         CHANNELS.get(msg.getChannelId()).sendToServer(msg);
      }
   }

   // New from Unofficial (BetaZavr)
   public static <MSG extends PacketServerBasic> void sendServerDelayed(MSG msg, Object key, int delay) {
      Long existing = delaySendMap.get(key);
      long now = System.currentTimeMillis();
      if (delaySendMap.size() > 1000) { delaySendMap.entrySet().removeIf(entry -> entry.getValue() <= now); }
      if (existing != null && existing > now) { return; }
      logged(msg, false);
      CHANNELS.get(msg.getChannelId()).sendToServer(msg);
      delaySendMap.put(key, now + delay);
   }

   private static <MSG> void logged(MSG msg, boolean onClients) {
      if (onClients == msg instanceof PacketServerBasic) {
         LogWriter.warn("Packet \"" + msg.getClass().getSimpleName() + "\" is sent from the wrong side!");
      }
      if (CustomNpcs.VerboseDebug && !ignoredDebug.contains(msg.getClass())) {
         StringBuilder message = new StringBuilder(("" + Util.instance.getSide()).replace("DEDICATED_", ""));
         message.append(" send packet: ")
                 .append(msg.getClass().getSimpleName())
                 .append("; Channel ID:").append(((PacketBasic) msg).getChannelId());
         String line = Thread.currentThread().getStackTrace()[3].toString();
         if (line.contains("noppes.npcs")) { line = line.substring(line.indexOf("noppes.npcs")); }
         message.append("; At: ").append(line);
         LogWriter.debug(message.toString());
      }
   }

   public static void clearDelaySendMap() {
      long now = System.currentTimeMillis();
      delaySendMap.entrySet().removeIf(entry -> entry.getValue() <= now);
   }

}
