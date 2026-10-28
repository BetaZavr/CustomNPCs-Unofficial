package noppes.npcs.api.entity;

import net.minecraft.world.entity.player.Player;
import noppes.npcs.api.*;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.data.IPlayerMail;
import noppes.npcs.api.entity.data.IPlayerMiniMap;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.overlay.IOverlay;

@SuppressWarnings("unused")
public interface IPlayer<T extends Player> extends IEntityLiving<T> {

   String getDisplayName();

   boolean hasFinishedQuest(@ParamName("id") int id);

   boolean hasActiveQuest(@ParamName("id") int id);

   void startQuest(@ParamName("id") int id);

   int factionStatus(@ParamName("id") int id);

   void finishQuest(@ParamName("id") int id);

   void stopQuest(@ParamName("id") int id);

   void removeQuest(@ParamName("id") int id);

   boolean hasReadDialog(@ParamName("id") int id);

   void showDialog(@ParamName("id") int id, @ParamName("name") String name);

   void showSoundSelectionGUI();

   void removeDialog(@ParamName("id") int id);

   void addDialog(@ParamName("id") int id);

   void addFactionPoints(@ParamName("id") int id, @ParamName("points") int points);

   int getFactionPoints(@ParamName("id") int id);

   void message(@ParamName("message") String message);

   int getGamemode();

   void setGamemode(@ParamName("mode") int mode);

   /** @deprecated */
   @Deprecated
   int inventoryItemCount(@ParamName("stack") IItemStack stack);

   /** @deprecated */
   @Deprecated
   int inventoryItemCount(@ParamName("name") String name);

   IContainer getInventory();

   IItemStack getInventoryHeldItem();

   boolean removeItem(@ParamName("stack") IItemStack stack, @ParamName("count") int count);

   boolean removeItem(@ParamName("name") String name, @ParamName("count") int count);

   void removeAllItems(@ParamName("stack") IItemStack stack);

   boolean giveItem(@ParamName("stack") IItemStack stack);

   boolean giveItem(@ParamName("id") String id, @ParamName("amount") int amount);

   void setSpawnpoint(@ParamName("x") int x, @ParamName("y") int y, @ParamName("z") int z);

   void resetSpawnpoint();

   boolean hasAdvancement(@ParamName("name") String name);

   int getExpLevel();

   void setExpLevel(@ParamName("level") int level);

   boolean hasPermission(@ParamName("permissionName") String permissionName);

   Object getPixelmonData();

   ITimers getTimers();

   void closeGui();

   T getMCEntity();

   IBlock getSpawnPoint();

   void setSpawnPoint(@ParamName("block") IBlock block);

   int getHunger();

   void setHunger(@ParamName("level") int level);

   void kick(@ParamName("message") String message);

   void sendNotification(@ParamName("title") String title, @ParamName("message") String message, @ParamName("type") int type);

   void sendMail(@ParamName("mail") IPlayerMail mail);

   void clearData();

   IQuest[] getActiveQuests();

   IQuest[] getFinishedQuests();

   void updatePlayerInventory();

   void playSound(@ParamName("sound") String sound, @ParamName("volume") float volume, @ParamName("pitch") float pitch);

   void playMusic(@ParamName("sound") String sound, @ParamName("background") boolean background, @ParamName("loops") boolean loops);

   IContainer getOpenContainer();

   boolean canQuestBeAccepted(@ParamName("id") int id);

   void showCustomGui(@ParamName("gui") ICustomGui gui);

   ICustomGui getCustomGui();

   void trigger(@ParamName("id") int id, @ParamName("arguments") Object... arguments);

   // New from Unofficial (Goodbird)
   void showOverlay(@ParamName("overlay") IOverlay overlay);

   void hideOverlay(@ParamName("overlayType") int overlayType);

   void hideAllOverlays();

   IPlayerSkin getSkin();

   IPlayerSkin getSkin(@ParamName("type") int type);

   // New from Unofficial (BetaZavr)
   void addMoney(@ParamName("value") long value);

   void cameraShakingPlay(@ParamName("time") int time, @ParamName("amplitude") int amplitude,
                          @ParamName("type") int type, @ParamName("isFading") boolean isFading);

   void cameraShakingStop();

   void completeQuest(@ParamName("id") int id);

   IContainer getBubblesInventory();

   int[] getKeyPressed();

   String getLanguage();

   //IPlayerMiniMap getMiniMapData();

   long getMoney();

   int[] getMousePressed();

   //IOverlayHUD getOverlayHUD();

   String getSkinType(@ParamName("type") int type);

   IScreenSize getWindowSize();

   boolean hasMousePress(@ParamName("key") int key);

   boolean hasOrKeyPressed(@ParamName("key") int[] key);

   @Deprecated

   int inventoryItemCount(@ParamName("stack") IItemStack stack, @ParamName("ignoreDamage") boolean ignoreDamage, @ParamName("ignoreNBT") boolean ignoreNBT);

   @Deprecated
   int inventoryItemCount(@ParamName("id") String id, @ParamName("amount") int amount);

   boolean isCompleteQuest(@ParamName("id") int id);

   boolean isMoved();

   void playSound(@ParamName("category") int category, @ParamName("pos") IPos pos,
                  @ParamName("sound") String sound, @ParamName("volume") float volume, @ParamName("pitch") float pitch);

   void sendTo(@ParamName("nbt") INbt nbt);

   void setMoney(@ParamName("value") long value);

   void setSkin(@ParamName("type") int type, @ParamName("gender") int gender,
                @ParamName("body") int body, @ParamName("bodyColor") int bodyColor,
                @ParamName("hair") int hair, @ParamName("hairColor") int hairColor,
                @ParamName("face") int face, @ParamName("eyesColor") int eyesColor,
                @ParamName("leg") int leg, @ParamName("jacket") int jacket,
                @ParamName("shoes") int shoes, @ParamName("peculiarities") int... peculiarities);

   void setSkinType(@ParamName("type") int type, @ParamName("location") String location);

   void stopSound(@ParamName("category") int category, @ParamName("sound") String sound);

   IEntity<?> getRidingEntity();

   IEntity<?> getLookingEntity();

   IBlock getLookingBlock();

   double getBlockReachDistance();

   void showMarket(@ParamName("marcetID") int marcetID);

   void showAuction(int auctionId);

   IScreenSize getScreenSize();

   void showBank(@ParamName("bankId") int bankId);

   IPlayerMiniMap getMiniMapData();
}
