package noppes.npcs.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketGiveStack;
import noppes.npcs.packets.server.SPacketNbtBookBlockSave;
import noppes.npcs.packets.server.SPacketNbtBookEntitySave;
import noppes.npcs.packets.server.SPacketNbtBookStackSave;
import noppes.npcs.shared.client.gui.GuiTextAreaScreen;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.NBTJsonUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.List;

public class GuiNbtBook extends GuiNPCInterface implements IGuiData {

   protected final BlockPos pos;
   protected BlockEntity tile;
   protected BlockState state;
   protected ItemStack blockStack;
   protected String faultyText = null;
   protected String errorMessage = null;

   // New from Unofficial (BetaZavr)
   protected ItemStack stack;
   protected GuiCustomScrollNop scroll;
   public CompoundTag originalCompound;
   public CompoundTag compound;
   public Entity entity;
   public int entityId;

   public GuiNbtBook(BlockPos posIn) {
      super();
      setBackground("menubg.png");
      imageWidth = 256;
      imageHeight = 217;

      pos = posIn;
   }

   @Override
   public void init() {
      super.init();
      boolean onlyClient = stack == null && state == null && entity == null;
      int h = 120;
      if (scroll == null) { scroll = addScroll(0).setSize(188, h); }
      add(scroll.setPos(guiLeft + 60, guiTop + 45));
      if (stack != null) {
         h = 118;
         scroll.setSize(188, h - 20);
         addLabel(11, guiLeft + 60, guiTop + 6, "id: \"" + ForgeRegistries.ITEMS.getKey(stack.getItem()) + "\"");
         addButton(1, guiLeft + 38, guiTop + 144, "gui.copy").setSize(180, 20);
         setObjectToScroll(stack);
      }
      else if (state != null) {
         addLabel(11, guiLeft + 60, guiTop + 6, "x: " + pos.getX() + ", y: " + pos.getY() + ", z: " + pos.getZ());
         addLabel(12, guiLeft + 60, guiTop + 16, "id: " + ForgeRegistries.BLOCKS.getKey(state.getBlock()));
         addLabel(13, guiLeft + 60, guiTop + 26, "meta: " + Block.getId(state));
         setObjectToScroll(state);
      }
      else if (entity != null) {
         h = 140;
         scroll.setSize(188, h - 20);
         String name;
         ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
         if (key == null) {
            name = "Not registered name!";
            onlyClient = true;
         }
         else { name = "id: " + key; }
         addLabel(12, guiLeft + 60, guiTop + 6, name);
         setObjectToScroll(entity);
      }
      addLabel(2, guiLeft + 4, guiTop + 172, "nbt.edit");
      addButton(0, guiLeft + 128, guiTop + 166, "nbt.edit")
              .setSize(59, 20)
              .setIsEnabled(compound != null && !compound.isEmpty());
      addButton(2, guiLeft + 189, guiTop + 166, "gui.fast")
              .setSize(59, 20)
              .setIsEnabled(compound != null && !compound.isEmpty());
      addLabel(0, guiLeft + 4, guiTop + 167, "")
              .setSize(58, 12)
              .setAlignLeft()
              .setColor(0xFFA00000);
      addLabel(1, guiLeft + 4, guiTop + 177, "")
              .setSize(imageWidth - 8, 12)
              .setAlignLeft()
              .setColor(0xFFA00000);
      addButton(66, guiLeft + 128, guiTop + 190, "gui.close")
              .setSize(120, 20);
      GuiButtonNop button = addButton(67, guiLeft + 4, guiTop + 190, "gui.save")
              .setSize(120, 20)
              .setIsEnabled(!onlyClient);
      if (!onlyClient) {
         if (errorMessage != null) {
            button.setIsEnabled(false);
            int i = errorMessage.indexOf(" at: ");
            if (i > 0) {
               getLabel(0).setSize(58, 12)
                       .setMessage(errorMessage.substring(0, i));
               getLabel(1).setSize(imageWidth - 8, 12)
                       .setMessage(errorMessage.substring(i));
            }
            else {
               getLabel(0).setSize(imageWidth - 8, 12)
                       .setMessage(errorMessage);
            }
         }
         else if (originalCompound != null) { button.setIsEnabled(!originalCompound.equals(compound)); }
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 0: {
            if (compound != null) {
               String text = NBTJsonUtil.Convert(compound);
               if (text.length() > 30000) { text = compound.toString(); }
               if (text.length() <= 100000) {
                  if (faultyText != null) { setSubGui((new GuiTextAreaScreen(0, text, faultyText)).enableHighlighting()); }
                  else { setSubGui((new GuiTextAreaScreen(0, text)).enableHighlighting()); }
               } else {
                  errorMessage = "NBT data is too long! Length: " + text.length();
                  NoppesStringUtils.setClipboardContents(text);
                  init();
               }
            }
            break;
         } // edit
         case 1: {
            if (stack != null && !stack.isEmpty()) { Packets.sendServer(new SPacketGiveStack(stack.save(new CompoundTag()))); }
            break;
         } // copy stack
         case 2: {
            if (compound != null) {
               String text = compound.toString();
               if (text.length() <= 100000) {
                  if (faultyText != null) { setSubGui((new GuiTextAreaScreen(0, text, faultyText)).enableHighlighting()); }
                  else { setSubGui((new GuiTextAreaScreen(0, text)).enableHighlighting()); }
               } else {
                  errorMessage = "NBT data is too long! Length: " + text.length();
                  NoppesStringUtils.setClipboardContents(text);
                  init();
               }
            }
            break;
         } // edit fast
         case 66: onClose(); break;
         case 67: {
            if (!compound.equals(originalCompound)) {
               if (stack != null) { Packets.sendServer(new SPacketNbtBookStackSave(compound)); }
               else if (tile == null) { Packets.sendServer(new SPacketNbtBookEntitySave(entityId, compound)); }
               else { Packets.sendServer(new SPacketNbtBookBlockSave(pos, compound)); }
               originalCompound = compound.copy();
               button.active = false;
               errorMessage = "Saved";
               init();
            }
            break;
         } // save changed
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!hasSubGui()) {
         PoseStack matrixStack = graphics.pose();
         if (stack != null || blockStack != null) {
            // background
            matrixStack.pushPose();
            matrixStack.translate(0.0f, 0.0f, 1.0f);
            graphics.fill(guiLeft + 3, guiTop + 3, guiLeft + 55, guiTop + 55, 0xFF808080);
            graphics.fill(guiLeft + 4, guiTop + 4, guiLeft + 54, guiTop + 54, 0xFF000000);
            matrixStack.popPose();
            // object
            renderStackInGUI(graphics, stack != null ? stack : blockStack, guiLeft + 5.0f, guiTop + 5.0f, 0.0f, 3.0f, null);
         }
         else if (entity != null) {
            matrixStack.pushPose();
            int x = 30;
            int y = 80;
            float s = 1.0F;
            if (entity instanceof ItemFrame) {
               x = 10;
               y = 54;
               s = 1.4f;
            }
            drawNpc(graphics, entity, x, y, s, 0, 0, entity instanceof LivingEntity ? 0 : 1);
            matrixStack.translate(0.0f, 0.0f, 1.0f);
            int color = 0xFF808080;
            if (ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()) == null) { color = 0xFFFF4040; }
            graphics.fill(guiLeft + 5, guiTop + 11, guiLeft + 55, guiTop + 97, color);
            graphics.fill(guiLeft + 6, guiTop + 12, guiLeft + 54, guiTop + 96, 0xFF000000);
            matrixStack.popPose();
         }
      }
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public void subGuiClosed(Screen subgui) {
      if (subgui instanceof GuiTextAreaScreen gui) {
         try {
            compound = TagParser.parseTag(gui.text);
            errorMessage = faultyText = null;
         } catch (CommandSyntaxException e) {
            errorMessage = e.getLocalizedMessage();
            faultyText = gui.text;
         }
         init();
      }
   }

   @Override
   public void setGuiData(CompoundTag nbt) {
      if (nbt.contains("Item") && nbt.getBoolean("Item")) { stack = ItemStack.of(nbt.getCompound("Data")); }
      else if (nbt.contains("EntityId")) {
         entityId = nbt.getInt("EntityId");
         entity = player.level().getEntity(entityId);
      } else {
         tile = player.level().getBlockEntity(pos);
         state = player.level().getBlockState(pos);
         blockStack = new ItemStack(Item.BY_BLOCK.get(state.getBlock()));
      }
      originalCompound = nbt.getCompound("Data");
      compound = originalCompound.copy();
      init();
   }

   // New from Unofficial (BetaZavr)
   private void setObjectToScroll(Object obj) {
      addLabel(15, guiLeft + 60, guiTop + (state != null ? 36 : 16), "(?) Class \"" + obj.getClass().getSimpleName() + "\":")
              .setHoverTexts(obj.getClass().getName());
      // get data
      Map<String, Field> fs = new TreeMap<>();
      Map<String, Method> ms = new TreeMap<>();
      Map<String, Class<?>> cs = new TreeMap<>();
      for (Field f : obj.getClass().getDeclaredFields()) { fs.put(f.getName(), f); }
      for (Field f : obj.getClass().getFields()) { if (!fs.containsKey(f.getName())) { fs.put(f.getName(), f); } }
      for (Method m : obj.getClass().getDeclaredMethods()) { ms.put(m.getName(), m); }
      for (Method m : obj.getClass().getMethods()) { if (!ms.containsKey(m.getName())) { ms.put(m.getName(), m); } }
      for (Class<?> c : obj.getClass().getDeclaredClasses()) { cs.put(c.getName(), c); }
      for (Class<?> c : obj.getClass().getClasses()) { if (!cs.containsKey(c.getName())) { cs.put(c.getName(), c); } }
      // create list
      List<Component> list = new ArrayList<>();
      LinkedHashMap<Integer, List<Component>> hts = new LinkedHashMap<>();
      int i = 0;
      for (String key : fs.keySet()) {
         try {
            Field f = fs.get(key);
            int mdf = f.getModifiers();
            list.add(Component.empty()
                    .append(Component.literal("F: ").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(key).withStyle(Modifier.isPublic(mdf) ? ChatFormatting.GREEN : ChatFormatting.RED)));
            hts.put(i++, getFieldTypes(obj, mdf, f));
         }
         catch (Exception e) { LogWriter.error("Error:", e); }
      }
      for (String key : ms.keySet()) {
         try {
            Method m = ms.get(key);
            int mdf = m.getModifiers();
            list.add(Component.empty()
                    .append(Component.literal("M: ").withStyle(ChatFormatting.DARK_AQUA))
                    .append(Component.literal(key).withStyle(Modifier.isPublic(mdf) ? ChatFormatting.GREEN : ChatFormatting.RED)));
            hts.put(i++, getMethodTypes(mdf, m));
         }
         catch (Exception e) { LogWriter.error("Error:", e); }
      }
      for (String key : cs.keySet()) {
         Class<?> c = cs.get(key);
         int mdf = c.getModifiers();
         MutableComponent mf = Component.empty();
         if (Modifier.isPublic(mdf)) { mf.append(Component.literal("public ").withStyle(ChatFormatting.GREEN)); }
         else if (Modifier.isProtected(mdf)) { mf.append(Component.literal("protected ").withStyle(ChatFormatting.RED)); }
         else { mf.append(Component.literal("private ").withStyle(ChatFormatting.DARK_RED)); }
         if (Modifier.isStatic(mdf)) { mf.append(Component.literal("static ").withStyle(ChatFormatting.YELLOW)); }
         if (Modifier.isFinal(mdf)) { mf.append(Component.literal("final ").withStyle(ChatFormatting.AQUA)); }
         mf.append(Component.literal("subclass:").withStyle(ChatFormatting.BLUE));
         List<Component> l = new ArrayList<>();
         l.add(mf);
         l.add(Component.literal(c.getSimpleName()));

         list.add(Component.empty()
                 .append(Component.literal("C: ").withStyle(ChatFormatting.DARK_BLUE))
                 .append(Component.literal(key).withStyle(Modifier.isPublic(mdf) ? ChatFormatting.GREEN : ChatFormatting.RED)));
         hts.put(i++, l);
      }
      scroll.setUnsortedList(list)
              .setHoverTexts(hts);
   }

   private static List<Component> getFieldTypes(Object obj, int mdf, Field f) {
      MutableComponent mf = Component.empty()
              .append(Component.literal("field: ").withStyle(ChatFormatting.GOLD));
      if (Modifier.isPublic(mdf)) { mf.append(Component.literal("public ").withStyle(ChatFormatting.GREEN)); }
      else if (Modifier.isProtected(mdf)) { mf.append(Component.literal("protected ").withStyle(ChatFormatting.RED)); }
      else { mf.append(Component.literal("private ").withStyle(ChatFormatting.DARK_RED)); }
      if (Modifier.isStatic(mdf)) { mf.append(Component.literal("static ").withStyle(ChatFormatting.YELLOW)); }
      if (Modifier.isFinal(mdf)) { mf.append(Component.literal("final ").withStyle(ChatFormatting.AQUA)); }
      Object v = null;
      try {
         boolean bo = !f.canAccess(obj);
         if (bo) { f.setAccessible(true); }
         v = f.get(obj);
         if (bo) { f.setAccessible(false); }
      } catch (Exception ignored) { }
      List<Component> hoverText = new ArrayList<>();
      hoverText.add(mf);
      hoverText.add(Component.empty()
              .append(Component.literal("value type: ").withStyle(ChatFormatting.GRAY))
              .append(Component.literal(f.getType().getName()).withStyle(ChatFormatting.RESET)));
      hoverText.add(Component.empty()
              .append(Component.literal("value: ").withStyle(ChatFormatting.GRAY))
              .append(Component.literal(v != null ? v.toString() : "null").withStyle(ChatFormatting.RESET)));
      return hoverText;
   }

   private static List<Component> getMethodTypes(int mdf, Method m) {
      MutableComponent mf = Component.empty()
              .append(Component.literal("method: ").withStyle(ChatFormatting.DARK_AQUA));
      if (Modifier.isPublic(mdf)) { mf.append(Component.literal("public ").withStyle(ChatFormatting.GREEN)); }
      else if (Modifier.isProtected(mdf)) { mf.append(Component.literal("protected ").withStyle(ChatFormatting.RED)); }
      else { mf.append(Component.literal("private ").withStyle(ChatFormatting.DARK_RED)); }
      if (Modifier.isStatic(mdf)) { mf.append(Component.literal("static ").withStyle(ChatFormatting.YELLOW)); }
      if (Modifier.isFinal(mdf)) { mf.append(Component.literal("final ").withStyle(ChatFormatting.AQUA)); }

      List<Component> hoverText = new ArrayList<>();
      hoverText.add(mf);
      if (m.getParameters() != null && m.getParameters().length > 0) {
         hoverText.add(Component.literal("parameters: (").withStyle(ChatFormatting.GRAY));
         Parameter[] prms = m.getParameters();
         for (int j = 0; j < prms.length; j++) {
            String pName = prms[j].getType().getName();
            String aName = prms[j].getType().getSimpleName();
            MutableComponent ps = Component.literal(" ").append(Component.literal(pName.replace(aName, "")).withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(aName).withStyle(ChatFormatting.YELLOW));
            if (j < prms.length - 1) { ps.append(Component.literal(",").withStyle(ChatFormatting.GRAY)); }
            hoverText.add(ps);
         }
         hoverText.add(Component.literal(")").withStyle(ChatFormatting.GRAY));
      } else {
         hoverText.add(Component.literal("parameters: ()").withStyle(ChatFormatting.GRAY));
      }
      hoverText.add(Component.empty()
              .append(Component.literal("return type: ").withStyle(ChatFormatting.GRAY))
              .append(Component.literal(m.getReturnType().getName()).withStyle(ChatFormatting.RESET)));
      return hoverText;
   }

}
