package noppes.npcs.client.gui.roles;

import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketNpcJobSave;
import noppes.npcs.roles.JobHealer;
import noppes.npcs.roles.data.HealerSettings;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;

public class GuiNpcHealer
        extends GuiNPCInterface2
        implements ITextfieldListener, ICustomScrollListener {

   protected final JobHealer job;

   // New from Unofficial (BetaZavr)
   private final Map<Component, Component> displays = new LinkedHashMap<>(); // [display name, registry name]
   private final Map<Component, ResourceLocation> potions = new TreeMap<>(Comparator.comparing(Component::getString)); // [registry name, registry ID]
   private int range = 8;
   private int speed = 10;
   private int amplifier = 0;
   private GuiCustomScrollNop options; // OLD name scroll1
   private GuiCustomScrollNop configured; // OLD name scroll2
   private byte type = (byte) 2;

   @SuppressWarnings("deprecation")
   public GuiNpcHealer(EntityNPCInterface npc) {
      super(npc);

      backGui = EnumGuiType.MainMenuAdvanced;
      job = (JobHealer)npc.job;
      for (ResourceLocation rl : BuiltInRegistries.MOB_EFFECT.keySet()) {
         MobEffect potion = BuiltInRegistries.MOB_EFFECT.get(rl);
         potions.put(Component.empty().append(Component.translatable("effect." + rl.toString().replace(":", "."))
                 .withStyle(potion == null ? ChatFormatting.LIGHT_PURPLE :
                         potion.isBeneficial() ? ChatFormatting.GREEN : ChatFormatting.RED)), rl);
      }
   }

   @SuppressWarnings("deprecation")
   @Override
   public void init() {
      super.init();
      int x0 = guiLeft + 4;
      int y = guiTop + 14;
      if (options == null) { options = addScroll(0).setSize(172, 154); }
      add(options.setPos(x0, y));
      addLabel(11, x0 + 2, y - 10, "beacon.availableEffects");
      if (configured == null) { configured = addScroll(1).setSize(172, 154); }
      add(configured.setPos(guiLeft + 238, y));
      addLabel(12, guiLeft + 239, y - 10, "beacon.currentEffects");
      displays.clear();
      LinkedHashMap<Integer, List<Component>> htsO = new LinkedHashMap<>();
      LinkedHashMap<Integer, List<Component>> htsC = new LinkedHashMap<>();
      Component r = Component.translatable("gui.range").withStyle(ChatFormatting.GRAY);
      Component s = Component.translatable("gui.repeatable").withStyle(ChatFormatting.GRAY);
      Component b = Component.translatable("gui.blocks").withStyle(ChatFormatting.GRAY);
      Component c = Component.translatable("gui.sec").withStyle(ChatFormatting.GRAY);
      Component t = Component.translatable("gui.time").withStyle(ChatFormatting.GRAY);
      Component p = Component.translatable("beacon.amplifier").withStyle(ChatFormatting.GRAY);
      Component l = Component.translatable("parameter.level").withStyle(ChatFormatting.GRAY);
      Component j = Component.translatable("gui.type").withStyle(ChatFormatting.GRAY);
      Component u = Component.translatable("script.target").withStyle(ChatFormatting.GRAY);
      for (Component name : potions.keySet()) {
         MobEffect potion = BuiltInRegistries.MOB_EFFECT.get(potions.get(name));
         int id = BuiltInRegistries.MOB_EFFECT.getId(potion);
         if (!job.effects.containsKey(id)) { // has potion ID
            htsO.put(htsO.size(), getHover(id, potions.get(name).toString()));
         }
         else { // to setts
            HealerSettings hs = job.effects.get(id);
            String lv = "enchantment.level." + (hs.amplifier + 1);
            if (!Component.translatable(lv).getString().equals(lv)) { lv = Component.translatable(lv).getString(); }
            else { lv = "" + (hs.amplifier + 1); }
            displays.put(name.copy().append(Component.literal(" " + lv).withStyle(ChatFormatting.RESET)), name);
            List<Component> hovers = getHover(id, potions.get(name).toString());
            hovers.add(Component.empty()
                    .append(r)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("" + hs.range).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                    .append(b));
            hovers.add(Component.empty()
                    .append(s)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("" + (Math.round((double) hs.speed / 2.0d) / 10.0d)).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                    .append(c));
            hovers.add(Component.empty()
                    .append(t)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("" + (Math.round((double) hs.time / 2.0d) / 10.0d)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                    .append(c));
            hovers.add(Component.empty()
                    .append(p)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("" + (hs.amplifier + 1)).withStyle(ChatFormatting.RED))
                    .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                    .append(l));
            hovers.add(Component.empty()
                    .append(j)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable(hs.type == (byte) 0 ? "faction.friendly" : hs.type == (byte) 1 ? "faction.unfriendly" : "spawner.all")
                            .withStyle(hs.type == (byte) 0 ? ChatFormatting.GREEN : ChatFormatting.DARK_AQUA)));
            hovers.add(Component.empty()
                    .append(u)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable(hs.isMassive ? "beacon.massive" : "beacon.not.massive")
                            .withStyle(hs.isMassive ? ChatFormatting.DARK_PURPLE : ChatFormatting.YELLOW)));
            htsC.put(htsC.size(), hovers);
         }
      }
      options.setUnsortedList(new ArrayList<>(potions.keySet()));
      options.setHoverTexts(htsO);
      configured.setUnsortedList(new ArrayList<>(displays.keySet()));
      configured.setHoverTexts(htsC);
      Component toall = Component.translatable("beacon.hover.toall");
      y += 156;
      int x1 = x0 + 102;
      int x2 = x1 + 72;
      int x3 = x2 + 102;
      addLabel(1, x0, y + 5, "beacon.range")
              .setSize(100, 10);
      addTextField(1, x1, y, 40, 20, range)
              .setMinMaxDefault(1, 64, 16)
              .setHoverTexts(Component.translatable("beacon.hover.dist").append(toall));
      addLabel(2, x2, y + 5, "stats.speed")
              .setSize(100, 10);
      addTextField(2, x3, y, 40, 20, speed)
              .setMinMaxDefault(10, 72000, 20)
              .setHoverTexts(Component.translatable("beacon.hover.speed").append(toall));
      addLabel(3, x0, (y += 22) + 5, "beacon.affect")
              .setSize(100, 10);
      addButton(1, x1 - 39, y, false, type, "faction.friendly", "faction.unfriendly", "spawner.all")
              .setSize(80, 20)
              .setHoverTexts(Component.translatable("beacon.hover.type").append(toall));
      addLabel(4, x2, y + 5, "beacon.amplifier")
              .setSize(100, 10);
      String lv = "enchantment.level." + (amplifier + 1);
      if (!Component.translatable(lv).getString().equals(lv)) { lv = Component.translatable(lv).getString(); }
      else { lv = "" + (amplifier + 1); }
      addTextField(3, x3, y, 40, 20, amplifier + 1)
              .setMinMaxDefault(1, 4, 1)
              .setHoverTexts(Component.translatable("beacon.hover.power", lv).append(toall));
      addButton(11, guiLeft + 177, (y -= 165), ">")
              .setSize(61, 20)
              .setIsEnabled(options.hasSelected())
              .setHoverTexts("hover.add.element");
      addButton(12, guiLeft + 177, (y += 22), "<")
              .setSize(61, 20)
              .setIsEnabled(configured.hasSelected())
              .setHoverTexts("hover.del.element");
      addButton(13, guiLeft + 177, (y += 44), ">>")
              .setSize(61, 20)
              .setIsEnabled(!potions.isEmpty())
              .setHoverTexts("hover.add.all.element");
      addButton(14, guiLeft + 177, (y += 22), "<<")
              .setSize(61, 20)
              .setIsEnabled(!displays.isEmpty())
              .setHoverTexts("hover.del.all.element");
      addButton(0, guiLeft + 177, y + 33, "gui.edit")
              .setSize(61, 20)
              .setIsEnabled(configured.hasSelected())
              .setHoverTexts("beacon.hover.edit.element");
   }

   @Override
   @SuppressWarnings("deprecation")
   public void buttonEvent(GuiButtonNop button) {
      switch (button.id) {
         case 0: {
            if (displays.containsKey(configured.getNormalSelected())) {
               MobEffect potion = BuiltInRegistries.MOB_EFFECT.get(potions.get(displays.get(configured.getNormalSelected())));
               int id = BuiltInRegistries.MOB_EFFECT.getId(potion);
               if (job.effects.containsKey(id)) {
                  setSubGui(new SubGuiNpcJobHealerSettings(job.effects.get(id)));
               }
            }
            break;
         } // edit HealerSettings
         case 1: type = (byte) button.getValue(); break;
         case 11: {
            if (potions.containsKey(options.getNormalSelected())) {
               GuiTextFieldNop.unfocus();
               MobEffect potion = BuiltInRegistries.MOB_EFFECT.get(potions.get(options.getNormalSelected()));
               int id = BuiltInRegistries.MOB_EFFECT.getId(potion);
               HealerSettings hs = new HealerSettings(id, range, speed, amplifier, type);
               job.effects.put(id, hs);
               options.setSelected(-1);
               configured.setSelected(-1);
               init();
            }
            break;
         } // >
         case 12: {
            if (displays.containsKey(configured.getNormalSelected())) {
               MobEffect potion = BuiltInRegistries.MOB_EFFECT.get(potions.get(displays.get(configured.getNormalSelected())));
               job.effects.remove(BuiltInRegistries.MOB_EFFECT.getId(potion));
               options.setSelected(-1);
               configured.setSelected(-1);
               init();
            }
            break;
         } // <
         case 13: {
            GuiTextFieldNop.unfocus();
            job.effects.clear();
            Registry<MobEffect> r = BuiltInRegistries.MOB_EFFECT;
            for (ResourceLocation rl : r.keySet()) {
               int id = r.getId(r.get(rl));
               HealerSettings hs = new HealerSettings(id, range, speed, amplifier, type);
               job.effects.put(id, hs);
            }
            options.setSelected(-1);
            configured.setSelected(-1);
            init();
            break;
         } // >>
         case 14: {
            job.effects.clear();
            options.setSelected(-1);
            configured.setSelected(-1);
            init();
            break;
         } // <<
      }
   }

   @Override
   public void save() { Packets.sendServer(new SPacketNpcJobSave(job.save(new CompoundTag()))); }

   // New from Unofficial (BetaZavr)
   @SuppressWarnings("deprecation")
   @Override
   public void subGuiClosed(Screen subgui) {
      if (subgui instanceof SubGuiNpcJobHealerSettings gui && configured.hasSelected()) {
         MobEffect potion = BuiltInRegistries.MOB_EFFECT.get(potions.get(displays.get(configured.getNormalSelected())));
         job.effects.put(BuiltInRegistries.MOB_EFFECT.getId(potion), gui.healerSettings);
         init();
      }
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) { init(); }

   @SuppressWarnings("deprecation")
   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
      if (scroll.id == 0) {
         if (!scroll.hasSelected() || !potions.containsKey(scroll.getNormalSelected())) { return; }
         GuiTextFieldNop.unfocus();
         MobEffect potion = BuiltInRegistries.MOB_EFFECT.get(potions.get(scroll.getNormalSelected()));
         HealerSettings hs = new HealerSettings(BuiltInRegistries.MOB_EFFECT.getId(potion), range, speed, amplifier, type);
         job.effects.put(hs.id, hs);
         options.setSelected(-1);
         configured.setSelected(-1);
         init();
      }
      else {
         if (!scroll.hasSelected() || !displays.containsKey(scroll.getNormalSelected())) { return; }
         MobEffect potion = BuiltInRegistries.MOB_EFFECT.get(potions.get(displays.get(scroll.getNormalSelected())));
         int id = BuiltInRegistries.MOB_EFFECT.getId(potion);
         if (!job.effects.containsKey(id)) { return; }
         setSubGui(new SubGuiNpcJobHealerSettings(job.effects.get(id)));
      }
   }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      switch (textField.id) {
         case 1: range = textField.getInteger(); break;
         case 2: speed = textField.getInteger(); break;
         case 3: amplifier = textField.getInteger() - 1; break;
      }
   }

   protected List<Component> getHover(int id, String regName) {
      List<Component> hover = new ArrayList<>();
      hover.add(Component.empty()
              .append(Component.literal("ID: "))
              .append(Component.literal("" + id).withStyle(ChatFormatting.GOLD)));
      hover.add(Component.empty()
              .append(Component.translatable("gui.name"))
              .append(": ")
              .append(Component.literal(regName).withStyle(ChatFormatting.DARK_AQUA)));
      return hover;
   }

}
