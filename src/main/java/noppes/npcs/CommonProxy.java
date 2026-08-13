package noppes.npcs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.handler.data.INpcRecipe;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.data.RecipeCarpentry;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.stats.IRecipeBookMixin;
import noppes.npcs.mixin.world.item.crafting.IRecipeManagerMixin;

import javax.annotation.Nullable;
import java.util.*;

public class CommonProxy {

   public void load() { }

   public void postload() { }

   public void openGui(Player player, Object guiscreen) { }

   public void openGui(EntityNPCInterface npc, EnumGuiType gui, @Nullable FriendlyByteBuf buffer) { }

   public void spawnParticle(LivingEntity player, String string, Object... ob) { }

   public @Nullable Player getPlayer() { return null; }

   public void spawnParticle(ParticleOptions type, double x, double y, double z, double motionX, double motionY, double motionZ, float scale) { }

   // New from Unofficial (BetaZavr)
   public String getTranslateLanguage(Player player) {
      String lang = getLanguage(player);
      if (lang.contains("_")) { lang = lang.substring(0, lang.indexOf("_")); }
      return lang;
   }

   public String getLanguage(Player entity) {
      if (entity instanceof ServerPlayer player) { return player.getLanguage(); }
      return "en_en";
   }

   public void init() { }

   public void updateKeys() { }

   public void loadAnimationModel(AnimationConfig animationConfig) { }

   public void createAllFiles(ICustomElement customElement) {
      if (customElement instanceof Block) { NoppesUtilServer.createAllBlockFiles(customElement); }
      if (customElement instanceof Item) { NoppesUtilServer.createAllItemFiles(customElement); }
   }

   public void playSound(SoundSource category, String sound, double x, double y, double z, float volume, float pitch, boolean streaming, boolean looping) {  }

   public void stopSound(int category, String sound) { }

   public @Nullable Level overworld() {
      if (CustomNpcs.Server != null) { return CustomNpcs.Server.overworld(); }
      return null;
   }

   public RecipeManager getRecipeManager() {
      if (CustomNpcs.Server != null) { return CustomNpcs.Server.getRecipeManager(); }
      return null;
   }

   public void syncRecipeManager() {
      RecipeManager manager = CustomNpcs.proxy.getRecipeManager();
      if (manager == null) { return; }
      Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes = ((IRecipeManagerMixin) manager).getRecipes();
      Map<ResourceLocation, Recipe<?>> byName = ((IRecipeManagerMixin) manager).getByName();
      // new
      Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> newRecipes = Maps.newHashMap();
      Map<ResourceLocation, Recipe<?>> newByName = Maps.newHashMap(byName);
      // collect
      for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> entry : new ArrayList<>(recipes.entrySet())) {
         if (entry.getKey() != RecipeType.CRAFTING) {
            newRecipes.put(entry.getKey(), entry.getValue());
         }
         else {
            Map<ResourceLocation, Recipe<?>> map = new HashMap<>();
            if (recipes.get(entry.getKey()) != null) {
               for (Map.Entry<ResourceLocation, Recipe<?>> entryCr : new ArrayList<>(recipes.get(RecipeType.CRAFTING).entrySet())) {
                  if (!(entryCr.getValue() instanceof RecipeCarpentry)) { map.put(entryCr.getKey(), entryCr.getValue()); }
               }
            }
            RecipeController rData = RecipeController.getInstance();
            for (int i = 0; i < 2; i++) {
               for (INpcRecipe npcRecipe : (i == 0 ? rData.getAllGlobalRecipes() : rData.getAllAnvilRecipes())) {
                  RecipeCarpentry recipe = (RecipeCarpentry) npcRecipe;
                  map.put(recipe.getId(), recipe);
                  newByName.put(recipe.getId(), recipe);
               }
            }
            newRecipes.put(entry.getKey(), map);
         }
      }
      // changed
      ((IRecipeManagerMixin) manager).setRecipes(ImmutableMap.copyOf(newRecipes));
      ((IRecipeManagerMixin) manager).setByName(ImmutableMap.copyOf(newByName));
      if (CustomNpcs.Server != null) {
         for (ServerPlayer player : CustomNpcs.Server.getPlayerList().getPlayers()) { syncRecipe(player.getRecipeBook()); }
      }
   }

   protected void syncRecipe(RecipeBook book) {
      RecipeManager manager = CustomNpcs.proxy.getRecipeManager();
      Map<ResourceLocation, Recipe<?>> byName = ((IRecipeManagerMixin) manager).getByName();
      Set<ResourceLocation> known = ((IRecipeBookMixin) book).getKnown();
      known.removeIf(id -> !byName.containsKey(id));
      ((IRecipeBookMixin) book).getHighlight().removeIf(id -> !byName.containsKey(id));
      RecipeController rData = RecipeController.getInstance();
      for (int i = 0; i < 2; i++) {
         for (INpcRecipe npcRecipe : (i == 0 ? rData.getAllGlobalRecipes() : rData.getAllAnvilRecipes())) {
            if (npcRecipe.isKnown()) { book.add((Recipe<?>) npcRecipe); }
         }
      }

   }

   public @Nullable Level getOverWorld() {
      if (CustomNpcs.Server != null) { return CustomNpcs.Server.getLevel(Level.OVERWORLD); }
      return null;
   }

}
