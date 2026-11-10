package noppes.npcs.client.gui.global;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.handler.data.INpcRecipe;
import noppes.npcs.api.wrapper.gui.WrapperRecipe;
import noppes.npcs.client.gui.SubGuiEditIngredients;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.availability.SubGuiNpcAvailability;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerManageRecipes;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class GuiNpcManageRecipes
        extends GuiContainerNPCInterface2<ContainerManageRecipes>
        implements ICustomScrollListener {

   private static boolean onlyCustomNpc = true;
   private static WrapperRecipe recipe = new WrapperRecipe();
   private static final int green = new Color(0xFF70F070).getRGB();
   private static final int red = new Color(0xFFF07070).getRGB();
   private static final int gray = new Color(0xFF808080).getRGB();

   private final Map<Boolean, LinkedHashMap<Component, List<WrapperRecipe>>> data = new HashMap<>(); // <isGlobal, <Group, recipe data>>
   private GuiCustomScrollNop groups;
   private GuiCustomScrollNop recipes;
   private boolean wait = false;

   public GuiNpcManageRecipes(ContainerManageRecipes containerIn, Inventory inv, Component titleIn) {
      super(NoppesUtilServer.getEditingNpc(Minecraft.getInstance().player), containerIn, inv, titleIn);
      setBackground("inventorymenu.png");
      drawDefaultBackground = false;
      imageHeight = 200;
      backGui = EnumGuiType.MainMenuGlobal;

      resetData();
   }

   @Override
   public void init() {
      super.init();
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      boolean isModRecipe = recipe.id.getNamespace().equals(CustomNpcs.MODID);
      if (onlyCustomNpc && !isModRecipe) {
         boolean isGlobal = recipe.isGlobal;
         recipe = new WrapperRecipe();
         recipe.isGlobal = isGlobal;
      }
      if (!data.get(recipe.isGlobal).isEmpty()) {
         if (!recipe.group.getString().isEmpty()) {
            String name = Util.instance.getOldFormattedText(recipe.group);
            for (Component group : data.get(recipe.isGlobal).keySet()) {
               if (name.equals(Util.instance.getOldFormattedText(group))) {
                  recipe.group = group;
                  break;
               }
            }
         }
         if (recipe.group.getString().isEmpty() || !data.get(recipe.isGlobal).containsKey(recipe.group)) {
            boolean isGlobal = recipe.isGlobal;
            recipe = new WrapperRecipe();
            recipe.isGlobal = isGlobal;
            recipe.group = data.get(recipe.isGlobal).values().iterator().next().get(0).group;
         }
      }
      if (groups != null && !groups.hasSelected()) { recipe.clear(); }
      else if (!recipe.id.getPath().isEmpty()) {
         boolean found = false;
         if (data.get(recipe.isGlobal).containsKey(recipe.group) && !data.get(recipe.isGlobal).get(recipe.group).isEmpty()) {
            for (WrapperRecipe wr : data.get(recipe.isGlobal).get(recipe.group)) {
               if (wr.id.getPath().equals(recipe.id.getPath())) {
                  found = true;
                  recipe = wr;
                  break;
               }
            }
         }
         if (!found) { recipe.id = new ResourceLocation(CustomNpcs.MODID, ""); }
      }
      if (recipe.id.getPath().isEmpty() && !data.get(recipe.isGlobal).isEmpty()) {
         if (recipe.group.getString().isEmpty() ||
                 !data.get(recipe.isGlobal).containsKey(recipe.group) ||
                 data.get(recipe.isGlobal).get(recipe.group).isEmpty()) {
            for (Component key : data.get(recipe.isGlobal).keySet()) {
               if (!onlyCustomNpc || key.getStyle().getColor() == null) {
                  recipe.group = key;
                  break;
               }
            }
         }
         if (data.get(recipe.isGlobal).containsKey(recipe.group)) { recipe = data.get(recipe.isGlobal).get(recipe.group).get(0); }
      }
      // groups
      addLabel(0, guiLeft + 172, guiTop + 8, "gui.recipe.groups")
              .setHoverTexts("recipe.hover.info.groups");
      // crafts
      addLabel(1, guiLeft + 294, guiTop + 8, "gui.recipe.crafts")
              .setHoverTexts("recipe.hover.info.crafts");
      if (groups == null) { groups = addScroll(0).setSize(120, 168); }
      if (recipes == null) { recipes = addScroll(1).setSize(120, 168); }
      List<Component> recipesList = new ArrayList<>();
      List<Component> groupsList = new ArrayList<>();
      for (Component groupName : data.get(recipe.isGlobal).keySet()) {
         if (groupName.getStyle().getColor() == null || !onlyCustomNpc) { groupsList.add(groupName); }
      }
      LinkedHashMap<Integer, List<Component>> htsG = new LinkedHashMap<>();
      int i = 0;
      for (Component group : groupsList) {
         String domen = CustomNpcs.MODID;
         MutableComponent itemName = Component.literal("Empty");
         Component count = Component.empty();
         if (!data.get(recipe.isGlobal).get(group).isEmpty()) {
            domen = data.get(recipe.isGlobal).get(group).get(0).id.getNamespace();
            ItemStack stack = data.get(recipe.isGlobal).get(group).get(0).product;
            @Nullable ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (registryName != null) {
               itemName = Component.literal(registryName.toString());
               count = Component.literal("Count: ").withStyle(ChatFormatting.GRAY)
                       .append(Component.literal("" + stack.getCount()).withStyle(ChatFormatting.GOLD));
               if (stack.hasTag()) {
                  itemName.append(Component.literal("; (").withStyle(ChatFormatting.GRAY))
                          .append(Component.literal("has NBT").withStyle(ChatFormatting.LIGHT_PURPLE))
                          .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
               }
            }
         }
         List<Component> ht = new ArrayList<>();
         ht.add(Component.empty()
                 .append(Component.literal("Group: ").withStyle(ChatFormatting.GRAY))
                 .append(group));
         ht.add(Component.empty()
                 .append(Component.literal("Item: ").withStyle(ChatFormatting.GRAY))
                 .append(itemName));
         if (!count.getString().isEmpty()) { ht.add(count); }
         ht.add(Component.empty()
                 .append(Component.literal("Mod: ").withStyle(ChatFormatting.GRAY))
                 .append(Component.literal(domen).withStyle(domen.equals(CustomNpcs.MODID) ? ChatFormatting.GREEN : ChatFormatting.AQUA)));
         ht.add(Component.empty()
                 .append(Component.literal("Is global: ").withStyle(ChatFormatting.GRAY))
                 .append(Component.literal(recipe.isGlobal ? "true" : "false")
                         .withStyle(recipe.isGlobal ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)));
         htsG.put(i++, ht);
      }
      LinkedHashMap<Integer, List<Component>> htsR = new LinkedHashMap<>();
      if (data.get(recipe.isGlobal).containsKey(recipe.group)) {
         for (WrapperRecipe wrapper : data.get(recipe.isGlobal).get(recipe.group)) {
            String domen = wrapper.id.getNamespace();
            MutableComponent name = Component.literal(wrapper.id.getPath());
            if (!domen.equals(CustomNpcs.MODID)) { name.withStyle(ChatFormatting.GRAY); }
            recipesList.add(name);
            List<Component> ht = new ArrayList<>();
            ht.add(Component.empty()
                    .append(Component.literal("Group: ").withStyle(ChatFormatting.GRAY))
                    .append(wrapper.group));
            ht.add(Component.empty()
                    .append(Component.literal("Name: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(wrapper.id.getPath()).withStyle(ChatFormatting.RESET)));
            ht.add(Component.empty()
                    .append(Component.literal("ID: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(wrapper.id.toString()).withStyle(ChatFormatting.GOLD)));
            ht.add(Component.empty()
                    .append(Component.literal("Mod: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(domen).withStyle(domen.equals(CustomNpcs.MODID) ? ChatFormatting.GREEN : ChatFormatting.AQUA)));
            ht.add(Component.empty()
                    .append(Component.literal("Is global: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(wrapper.isGlobal ? "true" : "false")
                            .withStyle(wrapper.isGlobal ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)));
            ht.add(Component.empty()
                    .append(Component.literal("Is shaped: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(wrapper.isShaped ? "true" : "false")
                            .withStyle(wrapper.isShaped ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)));
            ht.add(Component.empty()
                    .append(Component.literal("Always known: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(wrapper.isKnown ? "true" : "false")
                            .withStyle(wrapper.isKnown ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)));
            htsR.put(i++, ht);
         }
      }
      add(groups.setPos(guiLeft + 172, guiTop + 20)
              .setUnsortedList(groupsList)
              .setHoverTexts(htsG));
      if (!recipe.group.getString().isEmpty()) { groups.setSelected(recipe.group); }
      add(recipes.setPos(guiLeft + 294, guiTop + 20)
              .setUnsortedList(recipesList)
              .setHoverTexts(htsR));
      if (!recipe.id.getPath().isEmpty()) { recipes.setSelected(recipe.id.getPath()); }
      int x = guiLeft + 119;
      int y = guiTop + 191;
      // Global type
      addButton(0, guiLeft + 6, y, true, recipe.isGlobal ? 0 : 1, "menu.global", "block.customnpcs.npccarpentybench")
              .setSize(163, 20)
              .setHoverTexts("recipe.hover.type")
              .layerColor = recipe.isGlobal ?
              new Color(0x4000FF00).getRGB() :
              new Color(0x400000FF).getRGB();
      // Only mod list
      boolean isValid = recipe.isValid();
      if (recipe.isGlobal) {
         addCheckBox(30, guiLeft + 7, guiTop + 97, "gui.recipe.type.true", "gui.recipe.type.false", onlyCustomNpc)
                 .setSize(isModRecipe ? 111 : 163, 12);
      }
      // Groups
      addButton(1, guiLeft + 172, y, "gui.add")
              .setSize(59, 20)
              .setHoverTexts("recipe.hover.add.group");
      addButton(2, guiLeft + 234, y, "gui.remove")
              .setSize(59, 20)
              .setIsEnabled(groups.hasSelected() && isModRecipe)
              .setHoverTexts("recipe.hover.del.group");
      // Recipes
      addButton(3, guiLeft + 294, y, "gui.copy")
              .setSize(59, 20)
              .setIsEnabled(!isModRecipe || recipes.getList().size() < 16)
              .setHoverTexts("recipe.hover.add.recipe");
      addButton(4, guiLeft + 356, y, "gui.remove")
              .setSize(59, 20)
              .setIsEnabled(recipes.hasSelected() && isModRecipe)
              .setHoverTexts("recipe.hover.del.recipe");
      // Recipe settings
      if (isModRecipe) {
         y = guiTop + 4;
         addLabel(2, guiLeft + 6, y + 5, "availability.options");
         addButton(8, x, y, "selectServer.edit")
                 .setSize(50, 20)
                 .setIsEnabled(isValid)
                 .setHoverTexts("availability.hover")
                 .layerColor = recipe.availability.hasOptions() ? green : 0;
         addButton(9, x, y += 21, false, recipe.isShaped ? 1 : 0, "gui.shaped.0", "gui.shaped.1")
                 .setSize(50, 20)
                 .setIsEnabled(isValid)
                 .setHoverTexts("recipe.hover.shared")
                 .layerColor = isValid ? recipe.isShaped ? green :
                 new Color(0xFF7070FF).getRGB() :
                 new Color(0x0).getRGB();
         addButton(7, x, y += 21, false, recipe.isKnown ? 1 : 0, "gui.known.0", "gui.known.1")
                 .setSize(50, 20)
                 .setIsEnabled(isValid)
                 .setHoverTexts("recipe.hover.known")
                 .layerColor = isValid ? recipe.isKnown ? green : red : 0;
         addButton(5, x, y += 21, false, recipe.ignoreDamage ? 0 : 1, "gui.ignoreDamage.0", "gui.ignoreDamage.1")
                 .setSize(50, 20)
                 .setHoverTexts("recipe.hover.damage")
                 .layerColor = isValid ? recipe.ignoreDamage ? green : red : 0;

         addButton(6, x, y + 21, false, recipe.ignoreNBT ? 0 : 1, "gui.ignoreNBT.0", "gui.ignoreNBT.1")
                 .setSize(50, 20)
                 .setHoverTexts("recipe.hover.nbt")
                 .layerColor = isValid ? recipe.ignoreNBT ? green : red : 0;
      }
      // Product
      int craftOffset = recipe.isGlobal ? 9 : 0;
      MutableComponent hover = Component.translatable("recipe.hover.product");
      if (isModRecipe) {
         hover.append(Component.translatable("recipe.hover.ingredient.1"));
         hover.append(Component.translatable("recipe.hover.ingredient.2"));
      }
      hover.append(Component.translatable("recipe.hover.ingredient.3"));
      if (!recipe.product.isEmpty()) {
         for (Component line : recipe.product.getTooltipLines(player,
                 minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL)) { hover.append("<br>").append(line); }
      }
      addButton(10, guiLeft + 7 + craftOffset + (recipe.isGlobal ? 61 : 76), guiTop + 14 + craftOffset + (int) ((recipe.isGlobal ? 1.0 : 1.5) * 19.0), "")
              .setSize(30, 30)
              .setTexture(GuiBasic.ANIMATION_BUTTONS)
              .setDefBack(false)
              .setIsAnim(true)
              .setUV(220, 96, 36, 36)
              .setStacks(recipe.product)
              .setHoverTexts(hover)
              .layerColor = isModRecipe ? !recipe.id.getPath().isEmpty() &&
              recipe.id.getNamespace().equals(CustomNpcs.MODID) &&
              !recipe.group.getString().isEmpty() &&
              !recipe.product.isEmpty() ? 0 : red : gray;
      // Craft grid
      // set buttons / recipe
      int s = recipe.isGlobal ? 3 : 4;
      for (int h = 0; h < s; ++h) {
         for (int w = 0; w < s; ++w) {
            int slotId = w + h * s;
            addButton(11 + slotId, guiLeft + craftOffset + w * 19 + 7, guiTop + craftOffset + h * 19 + 20, "")
                    .setSize(18, 18)
                    .setTexture(GuiBasic.ANIMATION_BUTTONS)
                    .setDefBack(false)
                    .setIsAnim(true)
                    .setUV(220, 96, 36, 36)
                    .setStacks(recipe.ingredients.get(slotId))
                    .setHoverTexts(hover)
                    .layerColor = isModRecipe ?
                    recipe.ingredients.get(slotId) != null && recipe.ingredients.get(slotId).length > 0 ? green : red
                    : gray;
         }
      }
      // Clear
      addButton(28, guiLeft + 92, guiTop + 77, "")
              .setSize(18, 18)
              .setTexture(GuiBasic.ANIMATION_BUTTONS)
              .setDefBack(false)
              .setIsAnim(true)
              .setUV(120, 0, 24, 24)
              .setIsVisible(isModRecipe)
              .setHoverTexts(hover);
   }

   @Override
   public boolean mouseButtonEvent(GuiButtonNop button, int mouseButton) {
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      ItemStack heldStack = menu.getCarried().copy();
      boolean isModRecipe = recipe.id.getNamespace().equals(CustomNpcs.MODID);
      int id = button.id;
      switch (mouseButton) {
         case 1: {
            if (isModRecipe && id >= 10 && id < 27) {
               if (id == 10) {
                  if (heldStack.isEmpty()) {
                     recipe.product.setCount(Math.max(1, recipe.product.getCount() - 1));
                  } // -1
                  else if (NoppesUtilPlayer.compareItems(recipe.product, heldStack, false, false)) {
                     recipe.product.setCount(Math.min(recipe.product.getMaxStackSize(), recipe.product.getCount() + 1));
                  } // +N
                  button.layerColor = !recipe.id.getPath().isEmpty() && recipe.id.getNamespace().equals(CustomNpcs.MODID) && !recipe.group.getString().isEmpty() && !recipe.product.isEmpty() ? 0 : red;
               } // product
               else {
                  int pos = id - 11;
                  ItemStack[] array = recipe.ingredients.get(pos);
                  if (heldStack.isEmpty() && array != null && array.length > 0) {
                     int p = button.getRenderStackId();
                     if (p >= 0 && p < array.length) {
                        int count = Math.max(0, array[p].getCount() - 1);
                        if (count > 0) { array[p].setCount(count); }
                        else {
                           List<ItemStack> list = new ArrayList<>();
                           for (int i = 0; i < array.length; i++) {
                              if (i == p) { continue; }
                              list.add(array[i]);
                           }
                           array = list.toArray(new ItemStack[0]);
                        }
                        button.setStacks(array);
                        button.setCurrentStackPos(p);
                        recipe.ingredients.put(pos, array);
                     }
                  } // -1
                  else if ((array == null || array.length == 0) && !heldStack.isEmpty()) {
                     ItemStack stack = heldStack.copy();
                     stack.setCount(1);
                     array = new ItemStack[] { stack };
                     button.setStacks(array);
                     recipe.ingredients.put(pos, array);
                  } // put
                  else if (array != null) {
                     for (int i = 0; i < array.length; i++) {
                        if (!array[i].isEmpty() && NoppesUtilPlayer.compareItems(array[i], heldStack, false, false)) {
                           array[i].setCount(Math.min(array[i].getMaxStackSize(), array[i].getCount() + 1));
                           button.setStacks(array);
                           button.setCurrentStackPos(i);
                           recipe.ingredients.put(pos, array);
                           break;
                        }
                     }
                  } // +N
                  button.layerColor = button.renderStacks != null && button.renderStacks.length > 0 ? green : red;
               } // ingredient
               return true;
            }
            break;
         } // RMB
         case 2: {
            ItemStack stack = button.getRenderStack().copy();
            if (heldStack.isEmpty()) {
               if (Screen.hasControlDown()) { stack.setCount(stack.getMaxStackSize()); }
               Packets.sendServer(new SPacketDetectHeldItem(stack));
               return true;
            } // copy
            break;
         } // CMB
         default: {
            if (id >= 10 && id < 27) {
               if (!isModRecipe) { return false; }
               if (id == 10) {
                  if (Screen.hasAltDown()) { recipe.product.setCount(1); }
                  else {
                     if (heldStack.isEmpty()) { recipe.product.setCount(Math.max(1, recipe.product.getCount() - 1)); } // -1
                     else if (NoppesUtilPlayer.compareItems(recipe.product, heldStack, false, false)) { // +N
                        recipe.product.setCount(Math.min(recipe.product.getMaxStackSize(), recipe.product.getCount() + heldStack.getCount()));
                     }
                     else {
                        Packets.sendServer(new SPacketDetectHeldItem(recipe.product));
                        recipe.product = heldStack.copy();
                     } // replace
                     button.setStacks(recipe.product);
                  }
                  button.layerColor = !recipe.id.getPath().isEmpty() && recipe.id.getNamespace().equals(CustomNpcs.MODID) && !recipe.group.getString().isEmpty() && !recipe.product.isEmpty() ? 0 : red;
               } // product
               else {
                  if (Screen.hasShiftDown()) {
                     if (button.renderStacks != null && button.renderStacks.length > 0) {
                        setSubGui(new SubGuiEditIngredients(id, button.renderStacks));
                     }
                     return true;
                  } // show list of ingredients
                  int pos = id - 11;
                  ItemStack[] array = recipe.ingredients.get(pos);
                  if (Screen.hasControlDown()) {
                     if (heldStack.isEmpty() || array.length >= 16) { return false; }
                     if (array.length == 0) {
                        array = new ItemStack[] { heldStack.copy() };
                        array[0].setCount(1);
                        button.setStacks(array);
                        recipe.ingredients.put(pos, array);
                     }
                     else {
                        boolean found = false;
                        for (ItemStack stack : array) {
                           if (!stack.isEmpty() && NoppesUtilPlayer.compareItems(stack, heldStack, false, false)) {
                              found = true;
                              break;
                           }
                        }
                        if (!found) {
                           array = Arrays.copyOf(array, array.length + 1);
                           array[array.length - 1] = heldStack.copy();
                           button.setStacks(array);
                           recipe.ingredients.put(pos, array);
                        }
                     }
                  } // try to add new
                  else if (Screen.hasAltDown()) {
                     if (button.getRenderStackId() < array.length) {
                        array[button.getRenderStackId()].setCount(1);
                        button.setStacks(array);
                        recipe.ingredients.put(pos, array);
                     }
                     else if (array.length == 0 && !heldStack.isEmpty()) {
                        array = new ItemStack[] { heldStack.copy() };
                        array[0].setCount(1);
                        button.setStacks(array);
                        recipe.ingredients.put(pos, array);
                     }
                  } // set count == 1
                  else if (array == null || array.length == 0) {
                     if (!heldStack.isEmpty()) {
                        array = new ItemStack[]{ heldStack.copy() };
                        button.setStacks(array);
                        recipe.ingredients.put(pos, array);
                     }
                  } // install at least something
                  else {
                     if (heldStack.isEmpty()) {
                        int p = button.getRenderStackId();
                        if (p >= 0 && p < array.length) {
                           int count = Math.max(0, array[p].getCount() - 1);
                           if (count > 0) { array[p].setCount(count); }
                           else {
                              List<ItemStack> list = new ArrayList<>();
                              for (int i = 0; i < array.length; i++) {
                                 if (i == p) { continue; }
                                 list.add(array[i]);
                              }
                              array = list.toArray(new ItemStack[0]);
                           }
                           button.setStacks(array);
                           button.setCurrentStackPos(p);
                           recipe.ingredients.put(pos, array);
                        }
                     } // -1
                     else {
                        boolean found = false;
                        for (int i = 0; i < array.length; i++) {
                           if (!array[i].isEmpty() && NoppesUtilPlayer.compareItems(array[i], heldStack, false, false)) {
                              found = true;
                              array[i].setCount(Math.min(array[i].getMaxStackSize(), array[i].getCount() + heldStack.getCount()));
                              button.setStacks(array);
                              button.setCurrentStackPos(i);
                              break;
                           }
                        }
                        if (!found) {
                           array[button.getRenderStackId()] = heldStack.copy();
                           button.setStacks(array);
                           button.setCurrentStackPos(button.getRenderStackId());
                        }
                     } // +N
                     recipe.ingredients.put(pos, array);
                  } // +/- count? and set display found stack
                  button.layerColor = button.renderStacks != null && button.renderStacks.length > 0 ? green : red;
               } // ingredient
               return true;
            }
            switch (id) {
               case 0: {
                  save();
                  recipe.isGlobal = button.getValue() == 0;
                  resetData();
                  init();
                  return true;
               } // global type
               case 1: {
                  SubGuiEditText subGui = new SubGuiEditText(0, new String[]{ "npc_new" });
                  subGui.latinAlphabetOnly = true;
                  subGui.allowUppercase = false;
                  setSubGui(subGui);
                  return true;
               } // Add Group
               case 2: {
                  Packets.sendServer(new SPacketRecipeGroupRemove(recipe.isGlobal, Util.instance.getOldFormattedText(recipe.group)));
                  boolean isGlobal = recipe.isGlobal;
                  recipe = new WrapperRecipe();
                  recipe.isGlobal = isGlobal;
                  wait = true;
                  return true;
               } // Del Group
               case 3: {
                  int i;
                  String[] text;
                  Map<Integer, List<Component>> hovers = new HashMap<>();
                  String label;
                  if (isModRecipe) {
                     i = 1;
                     text = new String[] { recipe.id.getPath() };
                     label = Component.translatable("gui.name").append(":").getString();
                     hovers.put(0, Collections.singletonList(Component.translatable("recipe.hover.recipe.named").append(". ").append(Component.translatable("hover.latin.alphabet.only"))));
                  } // Add new Recipe
                  else {
                     i = 4;
                     text = new String[] { Util.instance.getOldFormattedText(recipe.group), recipe.id.getPath() };
                     label = Component.translatable("gui.group").append(" / ").append(Component.translatable("gui.name")).append(":").getString();
                     hovers.put(0, Collections.singletonList(Component.translatable("recipe.hover.group.named").append(". ").append(Component.translatable("hover.latin.alphabet.only"))));
                     hovers.put(1, Collections.singletonList(Component.translatable("recipe.hover.recipe.named").append(". ").append(Component.translatable("hover.latin.alphabet.only"))));
                  } // Copy vanilla Recipe
                  SubGuiEditText subGui = new SubGuiEditText(i, text);
                  subGui.label = label;
                  subGui.hovers.putAll(hovers);
                  subGui.latinAlphabetOnly = true;
                  subGui.allowUppercase = false;
                  setSubGui(subGui);
                  return true;
               } // Add Recipe
               case 4: {
                  Packets.sendServer(new SPacketRecipeRemove(recipe.id));
                  Component group = recipe.group;
                  boolean isGlobal = recipe.isGlobal;
                  recipe = new WrapperRecipe();
                  recipe.isGlobal = isGlobal;
                  recipe.group = group;
                  wait = true;
                  return true;
               } // Del Recipe
               case 5: {
                  recipe.ignoreDamage = !recipe.ignoreDamage;
                  save();
                  init();
                  return true;
               } // ignore Meta
               case 6: {
                  recipe.ignoreNBT = !recipe.ignoreNBT;
                  save();
                  init();
                  return true;
               } // ignore NBT
               case 7: {
                  recipe.isKnown = !recipe.isKnown;
                  save();
                  init();
                  return true;
               } // know
               case 8: {
                  setSubGui(new SubGuiNpcAvailability(recipe.availability, this));
                  return true;
               } // availability
               case 9: {
                  recipe.isShaped = !recipe.isShaped;
                  save();
                  return true;
               } // replace shaped <-> shapeless
               case 28: {
                  if (!heldStack.isEmpty()) {
                     player.inventoryMenu.setCarried(ItemStack.EMPTY);
                     Packets.sendServer(new SPacketDetectHeldItem(ItemStack.EMPTY));
                     return true;
                  }
                  break;
               } // clear held stack
               case 30: {
                  onlyCustomNpc = ((GuiCheckBoxNop) button).selected();
                  resetData();
                  init();
                  return true;
               } // only custom npc
            }
            break;
         } // LMB
      }
      return false;
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (wait) {
         drawWait(graphics);
         return;
      }
      if (!hasSubGui() && CustomNpcs.ShowDescriptions) {
         if (minecraft == null) { minecraft = Minecraft.getInstance(); }
         for (int i = 11; i < 27; i++) {
            GuiButtonNop button = getButton(i);
            if (button != null && button.isVisible() && button.isHovered()) {
               if (button.getRenderStack().isEmpty()) { button.setHoverTexts((Object) null); }
               else {
                  MutableComponent hover = Component.translatable("recipe.hover.ingredients", "" + (i - 11));
                  if (recipe.id.getNamespace().equals(CustomNpcs.MODID)) {
                     hover.append(Component.translatable("recipe.hover.ingredient.0"));
                     hover.append(Component.translatable("recipe.hover.ingredient.1"));
                     hover.append(Component.translatable("recipe.hover.ingredient.2"));
                  }
                  hover.append(Component.translatable("recipe.hover.ingredient.3"));
                  for (Component line : button.getRenderStack().getTooltipLines(player,
                          minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL)) { hover.append("<br>").append(line); }
                  button.setHoverTexts(hover);
               }
               break;
            }
         }
      } // stack hover info in button
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      switch (scroll.id) {
         case 0: {
            if (!recipe.group.equals(groups.getNormalSelected()) && data.get(recipe.isGlobal).containsKey(groups.getNormalSelected())) {
               List<WrapperRecipe> l = data.get(recipe.isGlobal).get(groups.getNormalSelected());
               if (!l.isEmpty()) {
                  save();
                  recipe = l.get(0);
                  init();
               }
            }
            break;
         } // group
         case 1: {
            if (!recipe.id.getPath().equals(recipes.getSelected()) && data.get(recipe.isGlobal).containsKey(recipe.group)) {
               for (WrapperRecipe wrapper : data.get(recipe.isGlobal).get(recipe.group)) {
                  if (wrapper.id.getPath().equals(recipes.getSelected())) {
                     save();
                     recipe = wrapper;
                     init();
                     break;
                  }
               }
            }
            break;
         } // recipe
      }
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
      switch (scroll.id) {
         case 0: setSubGui(new SubGuiEditText(2, new String[] { scroll.getSelected() })); break; // rename Group
         case 1: setSubGui(new SubGuiEditText(3, new String[] { scroll.getSelected() })); break; // rename Recipe
      }
   }

   @Override
   public void save() {
      GuiTextFieldNop.unfocus();
      if (recipe.isValid() &&
              recipe.parent instanceof INpcRecipe &&
              recipe.id.getNamespace().equals(CustomNpcs.MODID)) {
         recipe.getNbt();
         Packets.sendServer(new SPacketRecipeSave(recipe.getNbt()));
         wait = true;
      }
   }

   @Override
   public void subGuiClosed(Screen subgui) {
      if (subgui instanceof SubGuiNpcAvailability) { save(); }
      else if (subgui instanceof SubGuiEditIngredients gui) {
         ItemStack[] stacks = new ItemStack[0];
         if (gui.stacks != null) {
            List<ItemStack> list = new ArrayList<>();
            for (ItemStack stack : gui.stacks) {
               if (stack.isEmpty()) { continue; }
               list.add(stack);
            }
            if (!list.isEmpty()) { stacks = list.toArray(stacks); }
         }
         GuiButtonNop button = getButton(gui.id);
         if (button != null) { button.setStacks(stacks).setCurrentStackPos(0); }
         recipe.ingredients.put(gui.id - 11, stacks);
      } // set new stacks to ingredient
      else if (subgui instanceof SubGuiEditText gui && !gui.cancelled) {
         switch (gui.id) {
            case 0: {
               save();
               boolean isGlobal = recipe.isGlobal;
               recipe = new WrapperRecipe();
               recipe.isGlobal = isGlobal;
               String name = NoppesUtilServer.validNamespace(gui.text[0]);
               recipe.group = Component.literal(name);
               Packets.sendServer(new SPacketRecipeGroupSave(recipe.isGlobal, name));
               wait = true;
               break;
            } // Add new Group
            case 1: {
               save();
               String name = NoppesUtilServer.validPath(gui.text[0]);
               RecipeController rData = RecipeController.getInstance();
               while (rData.containsName(name)) { name += "_"; }
               recipe.id = new ResourceLocation(recipe.id.getNamespace(), name);
               Packets.sendServer(new SPacketRecipeSave(recipe.getNbt()));
               wait = true;
               break;
            } // Add new Recipe
            case 2: {
               String name = NoppesUtilServer.validNamespace(gui.text[0]);
               Packets.sendServer(new SPacketRecipeGroupRename(recipe.isGlobal, recipe.group.getString(), name));
               recipe.group = Component.literal(name);
               wait = true;
               break;
            } // Rename Group
            case 3: {
               String name = NoppesUtilServer.validPath(gui.text[0]);
               RecipeController rData = RecipeController.getInstance();
               while (rData.containsName(name)) { name += "_"; }
               Packets.sendServer(new SPacketRecipeRename(recipe.id.getPath(), name));
               recipe.id = new ResourceLocation(recipe.id.getNamespace(), name);
               wait = true;
               break;
            } // Rename Recipe
            case 4: {
               String name = NoppesUtilServer.validPath(gui.text[1]);
               RecipeController rData = RecipeController.getInstance();
               while (rData.containsName(name)) { name += "_"; }
               recipe.group = Component.literal(NoppesUtilServer.validNamespace(gui.text[0]));
               recipe.id = new ResourceLocation(CustomNpcs.MODID, name);
               Packets.sendServer(new SPacketRecipeSave(recipe.getNbt()));
               wait = true;
               break;
            } // Copy vanilla Recipe
         }
      }
   }

   public void resetData() {
      wait = false;
      data.clear();
      CustomNpcs.proxy.getRecipeManager().getRecipes().forEach(r -> {
         if (r instanceof INpcRecipe || (recipe.isGlobal && !onlyCustomNpc &&
                 (r instanceof ShapedRecipe sRecipe && sRecipe.getRecipeWidth() < 4 && sRecipe.getRecipeHeight() < 4) ||
                 (r instanceof ShapelessRecipe lRecipe && lRecipe.getIngredients().size() < 10))) {
            WrapperRecipe wrapper = WrapperRecipe.of(player, (CraftingRecipe) r);
            if (!data.containsKey(wrapper.isGlobal)) { data.put(wrapper.isGlobal, new LinkedHashMap<>()); }
            if (!data.get(wrapper.isGlobal).containsKey(wrapper.group)) { data.get(wrapper.isGlobal).put(wrapper.group, new ArrayList<>()); }
            data.get(wrapper.isGlobal).get(wrapper.group).add(wrapper);
         }
      });
      RecipeController rData = RecipeController.getInstance();
      for (String group : rData.getGroups(recipe.isGlobal)) {
         for (INpcRecipe r : recipe.isGlobal ? rData.getGlobalRecipes(group) : rData.getAnvilRecipes(group)) {
            WrapperRecipe wrapper = WrapperRecipe.of(player, (CraftingRecipe) r);
            if (!data.containsKey(wrapper.isGlobal)) { data.put(wrapper.isGlobal, new LinkedHashMap<>()); }
            if (!data.get(wrapper.isGlobal).containsKey(wrapper.group)) { data.get(wrapper.isGlobal).put(wrapper.group, new ArrayList<>()); }
            boolean notFound = true;
            for (WrapperRecipe wr : data.get(wrapper.isGlobal).get(wrapper.group)) {
               if (wr == wrapper || wr.id.equals(wrapper.id)) {
                  notFound = false;
                  break;
               }
            }
            if (notFound) { data.get(wrapper.isGlobal).get(wrapper.group).add(wrapper); }
         }
      }
      data.replaceAll((key, map) -> map.entrySet().stream()
              .sorted(Map.Entry.comparingByKey(
                      Comparator.<Component, Boolean>comparing(c -> c.getStyle().getColor() != null)
                              .thenComparing(Component::getString, String.CASE_INSENSITIVE_ORDER)
              ))
              .collect(Collectors.toMap(
                      Map.Entry::getKey,
                      e -> e.getValue().stream()
                              .sorted(Comparator.comparing(
                                      w -> w.getName().getString(),
                                      String.CASE_INSENSITIVE_ORDER
                              ))
                              .collect(Collectors.toList()),
                      (a, b) -> a,
                      LinkedHashMap::new
              )));
      if (!data.containsKey(Boolean.TRUE)) { data.put(Boolean.TRUE, new LinkedHashMap<>()); }
      if (!data.containsKey(Boolean.FALSE)) { data.put(Boolean.FALSE, new LinkedHashMap<>()); }
   }

}
