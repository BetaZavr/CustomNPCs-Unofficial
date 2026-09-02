package noppes.npcs.client.gui.select;

import com.google.common.collect.Lists;
import java.util.HashMap;

import net.minecraft.network.chat.Component;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestCategory;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.GuiSelectionListener;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

import javax.annotation.Nonnull;

public class SubGuiQuestSelection extends GuiBasic implements ICustomScrollListener {

   protected final HashMap<Component, QuestCategory> categoryData = new HashMap<>();
   protected final HashMap<Component, Quest> questData = new HashMap<>();
   protected GuiCustomScrollNop scrollCategories;
   protected GuiCustomScrollNop scrollQuests;
   protected GuiSelectionListener listener;

   protected @Nonnull Component selectedQuest = Component.empty();
   protected @Nonnull Component selectedCategory = Component.empty();

   public SubGuiQuestSelection(int questId) {
      super();
      setBackground("menubg.png");
      drawDefaultBackground = false;
      imageWidth = 366;
      imageHeight = 226;

      Quest quest = QuestController.instance.quests.get(questId);
      if (quest != null) {
         selectedQuest = Component.translatable(quest.title);
         selectedCategory = Component.translatable(quest.category.title);
      }
   }

   @Override
   public void init() {
      super.init();
      if (wrapper.parent instanceof GuiSelectionListener) { listener = (GuiSelectionListener) wrapper.parent; }
      addLabel(0, guiLeft + 8, guiTop + 4, "gui.categories");
      addLabel(1, guiLeft + 175, guiTop + 4, "quest.quests");
      addButton(2, guiLeft + imageWidth - 22, guiTop + 4, "X")
              .setSize(12, 12);
      categoryData.clear();
      for (QuestCategory category : QuestController.instance.categories.values()) {
         categoryData.put(Component.translatable(category.title), category);
      }
      questData.clear();
      if (categoryData.containsKey(selectedCategory)) {
         QuestCategory category = categoryData.get(selectedCategory);
         if (category != null) {
            for (Quest quest : category.quests.values()) { questData.put(Component.translatable(quest.title), quest); }
         }
      }
      if (scrollCategories == null) { scrollCategories = addScroll(0).setSize(170, 200); }
      scrollCategories.setNormalList(Lists.newArrayList(categoryData.keySet()));
      scrollCategories.setSelected(selectedCategory);
      add(scrollCategories.setPos(guiLeft + 4, guiTop + 14));

      if (scrollQuests == null) { scrollQuests = addScroll(1).setSize(170, 200); }
      scrollQuests.setNormalList(Lists.newArrayList(questData.keySet()));
      scrollQuests.setSelected(selectedQuest);
      add(scrollQuests.setPos(guiLeft + 175, guiTop + 14));
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      if (button.id == 2) {
         if (!selectedQuest.getSiblings().isEmpty()) { scrollDoubleClicked(null); }
         onClose();
      }
   }

   @Override
   public void scrollClicked(GuiCustomScrollNop scroll) {
      if (scroll.id == 0) {
         if (!selectedCategory.getString().equals(scrollCategories.getNormalSelected().getString())) {
            selectedCategory = scrollCategories.getNormalSelected();
            selectedQuest = Component.empty();
            scrollQuests.clearSelection();
         }
      }
      if (scroll.id == 1) { selectedQuest = scrollQuests.getNormalSelected(); }
      init();
   }

   @Override
   public void scrollDoubleClicked(GuiCustomScrollNop scroll) {
      if (questData.containsKey(selectedQuest)) {
         Quest quest = questData.get(selectedQuest);
         if (listener != null) { listener.selected(quest.id, quest.title); }
         onClose();
      }
   }

   public Quest getQuest() {
      if (questData.containsKey(selectedQuest)) { return questData.get(selectedQuest); }
      return null;
   }

}
