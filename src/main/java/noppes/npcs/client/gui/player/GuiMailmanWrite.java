package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ClientTickHandler;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.containers.ContainerMail;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.mixin.world.inventory.ISlotMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNextPage;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.*;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.*;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiMailmanWrite extends GuiContainerNPCInterface<ContainerMail>
        implements ITextfieldListener, ITextChangeListener, IGuiError, IGuiClose, IGuiData {

   protected static final ResourceLocation mEnvelope = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/envelope.png");
   protected static final ResourceLocation mList = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/list.png");
   protected static final ResourceLocation mRansom = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/ransom.png");
   protected static final ResourceLocation mTable = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/table.png");
   protected static final ResourceLocation mSendBox = new ResourceLocation(CustomNpcs.MODID, "textures/gui/mail/send_box.png");
   protected static final ResourceLocation widgets = new ResourceLocation("textures/gui/widgets.png");
   public static PlayerMail mail = new PlayerMail();
   public static Screen parent;

   protected final TextFieldHelper pageEdit = new TextFieldHelper(this::getText, this::setText, this::getClipboard, this::setClipboard,
           (text) -> {
              String withCursor = text + ChatFormatting.BLACK + "_";
              if (font.width(withCursor) >= 1590) { return false; }
              int lines = font.wordWrapHeight(withCursor, 152) / font.lineHeight;
              return lines < 11 || (lines == 11 && !text.endsWith("\n"));
           });

   protected GuiButtonNextPage buttonNextPage;
   protected GuiButtonNextPage buttonPreviousPage;
   protected ListTag bookPages;
   protected GuiLabel error;
   protected boolean hasStacks;
   protected boolean hasSend;
   protected int bookTotalPages;
   protected int currPage;
   protected int updateCount;
   protected int type;
   protected long totalCost;
   protected final Map<Integer, Long> cost = new TreeMap<>();
   protected String username;
   // Animations
   protected int step;
   protected int tick;
   protected int milliTick;
   protected int aType;
   protected long errTick;
   protected final Random rnd = new Random();
   protected final PlayerData playerData;

   public GuiMailmanWrite(ContainerMail container, Inventory inv, Component titleIn) {
      super(null, container, inv, titleIn);
      drawDefaultBackground = false;
      closeOnEsc = true;
      imageWidth = 306;
      imageHeight = 248;

      playerData = PlayerData.get(player);

      bookTotalPages = 1;
      hasSend = false;
      username = "";
      type = 0;
      errTick = 0;
      if (mail.message.contains("pages")) { bookPages = mail.message.getList("pages", 8); }
      if (bookPages != null) {
         bookPages = bookPages.copy();
         bookTotalPages = bookPages.size();
         if (bookTotalPages < 1) { bookTotalPages = 1; }
      } else {
         (bookPages = new ListTag()).add(StringTag.valueOf(""));
         bookTotalPages = 1;
      }
      ClientTickHandler.checkMails = true;
      setNextTick(30, false);
      step = 0;
   }

   @Override
   public void containerTick() {
      super.containerTick();
      ++updateCount;
      if (getLabel(4) != null) { getLabel(4).setIsEnabled(false); }
      if (menu.canEdit) {
         if (getLabel(4) != null) { getLabel(4).setIsEnabled(true); }
      } else {
         if (!menu.canSend && mail.money > 0) {
            if (getLabel(4) != null) { getLabel(4).setIsEnabled(true); }
         }
      }
   }

   @Override
   public void init() {
      super.init();
      int x = guiLeft + 170;
      int y = guiTop + 48;
      // player name
      addLabel(0, x, y, "mailbox." + (!menu.canEdit || !menu.canSend ? "sender" : "username"))
              .setSize(132, 10);
      if (menu.canEdit) {
         if (!menu.canSend) {
            addTextField(2, x, y += 10, 112, 14, mail.sender)
                    .setHoverTexts("mailbox.hover.to");
         } else {
            addTextField(0, x, y += 10, 112, 14, username)
                    .setHoverTexts("mailbox.hover.to");
         }
      }
      else {
         addLabel(10, x + 2, y += 10, "\"" + mail.sender + "\"")
              .setSize(132, 10);
      }
      // title
      addLabel(1, x, y += 18, "mailbox.subject")
              .setSize(132, 10);
      if (menu.canEdit) {
         addTextField(1, x, y += 10, 112, 14, mail.title)
                 .setHoverTexts("mailbox.hover.title");
      }
      else {

         addLabel(11, x, y += 10, "\"" + mail.title + "\"")
                 .setSize(132, 10);
      }
      // ransom
      if (!menu.canEdit) {
         if (mail.ransom > 0) {
            addLabel(7, x, y + 18, Component.translatable("mailbox.ransom").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                    .setSize(46, 10);
            addLabel(8,x + 2, y + 28,
                    Util.instance.getTextReducedNumber(mail.ransom, true, false, false)
                            + " " + CustomNpcs.displayCurrencies)
                    .setSize(46, 10);
         }
         if (mail.money > 0) {
            addLabel(7, x, y + 18, "market.currency")
                    .setSize(46, 10);
            addLabel(8, x + 2, y + 28,
                    Util.instance.getTextReducedNumber(mail.money, true, false, false)
                            + " " + CustomNpcs.displayCurrencies)
                    .setSize(46, 10);
         }
      }
      error = addLabel(2, x - 10, guiTop + 145, "")
              .setSize(150, 10)
              .setColor(new Color(0xFFFF0000).getRGB());
      // moneys
      if (menu.canEdit) {
         addLabel(3, x, (y += 19) + 4, "market.currency")
                 .setSize(100, 10);
         addLabel(6, x + 102, y + 4, CustomNpcs.displayCurrencies)
                 .setSize(20, 10);
         addTextField(3, x + 48, y, 50, 16, "" + mail.money)
                 .setMinMaxDefault(0,
                         (int) (player.isCreative() ? Integer.MAX_VALUE : playerData.game.getMoney()),
                         mail.money)
                 .setHoverTexts("mailbox.hover.money");
         addLabel(7, x, (y += 19) + 4, "mailbox.ransom")
                 .setSize(46, 10);
         addLabel(8, x + 102, y + 4, CustomNpcs.displayCurrencies)
                 .setSize(20, 10);
         addTextField(4, x + 48, y, 50, 16, "" + mail.ransom)
                 .setMinMaxDefault(0, Integer.MAX_VALUE, mail.ransom)
                 .setHoverTexts("mailbox.hover.ransom");
      }
      // main
      x = guiLeft + 7;
      y = guiTop + 149;
      if (menu.canEdit && !menu.canSend) {
         addButton(0, x + 52, y, "gui.done")
                 .setSize(50, 14)
                 .setTexture(GuiMailbox.buttons)
                 .setUV(0, 120, 0, 0);
      } // dialog/quest add to
      else if (menu.canEdit) {
         addButton(0, x + 52, y, "mailbox.send")
                 .setSize(50, 14)
                 .setTexture(GuiMailbox.buttons)
                 .setUV(0, 120, 0, 0);
      } // write
      if (!menu.canEdit && !menu.canSend) {
         if (mail.ransom > 0) {
            addButton(6, x + 220, y - 42, "gui.pay")
                    .setSize(74, 14)
                    .setTexture(GuiMailbox.buttons)
                    .setUV(0, 120, 0, 0);
         }
         else if (mail.money > 0) {
            addButton(6, x + 220, y - 42, "gui.take")
                    .setSize(50, 14)
                    .setTexture(GuiMailbox.buttons)
                    .setUV(0, 120, 0, 0)
                    .setHoverTexts("display.hover.X");
         }
         addButton(4, x, y, "gui.remove")
                 .setSize(50, 14)
                 .setTexture(GuiMailbox.buttons)
                 .setUV(0, 120, 0, 0);
         if (!mail.isReturned()) {
            addButton(7, x + 52, y, "mailbox.back")
                    .setSize(50, 14)
                    .setTexture(GuiMailbox.buttons)
                    .setUV(0, 120, 0, 0);
         }
      } // read -> delete
      if (!menu.canEdit || menu.canSend) {
         addButton(3, x + 104, y, !menu.canEdit ? "gui.back" : "gui.cancel")
                 .setSize(50, 14)
                 .setTexture(GuiMailbox.buttons)
                 .setUV(0, 120, 0, 0)
                 .setHoverTexts("display.hover.X");
      } // write -> cancel
      add(buttonNextPage = new GuiButtonNextPage(this, 1, x + 135, y - 16, true, null));
      add(buttonPreviousPage = new GuiButtonNextPage(this, 2, x, y - 16, false, null));
      updateButtons();
   }

   @Override
   public void onClose() {
      GuiTextFieldNop.unfocus();
      ClientTickHandler.checkMails = true;
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      player.closeContainer();
      setScreen(parent);
      if (parent instanceof GuiMailbox gui) { gui.init(); }
      parent = null;
      mail = new PlayerMail();
   }

   private void updateButtons() {
      if (!menu.canEdit && mail.ransom > 0) {
         buttonNextPage.setIsVisible(false);
         buttonPreviousPage.setIsVisible(false);
         return;
      }
      buttonNextPage.setIsVisible((menu.canEdit && !bookPages.getString(currPage).isEmpty()) || currPage < bookTotalPages - 1);
      buttonPreviousPage.setIsVisible(currPage > 0);
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
      if (!button.active) { return; }
      switch (button.id) {
         case 0: {
            mail.message.put("pages", bookPages);
            if (menu.canSend) {
               if (!hasSend) {
                  hasSend = true;
                  menu.sendMail = true;
                  Packets.sendServer(new SPacketPlayerMailSend(username, totalCost, mail.save()));
               }
            } else {
               aType = 0;
               animClose();
            }
            break;
         } // send
         case 1: {
            if (currPage < bookTotalPages - 1) {
               ++currPage;
               pageEdit.setCursorPos(bookPages.getString(currPage).length(), false);
            }
            else if (menu.canEdit) {
               addNewPage();
               if (currPage < bookTotalPages - 1) { ++currPage; }
               pageEdit.setCursorPos(0, false);
            }
            break;
         } // next page
         case 2: {
            if (currPage > 0) {
               --currPage;
               pageEdit.setCursorPos(bookPages.getString(currPage).length(), false);
            }
            break;
         } // pre page
         case 3: {
            aType = 0;
            animClose();
            break;
         } // back / exit
         case 4: {
            ConfirmScreen guiYesNo = new ConfirmScreen((agree) -> {
               if (agree) {
                  Packets.sendServer(new SPacketPlayerMailDelete(0, mail.timeWhenReceived, mail.sender));
                  removeMail();
                  aType = 2;
                  animClose();
               }
               NoppesUtil.openGUI(player, this);
            },
                    Component.empty(),
                    Component.translatable("message.delete"));
            setScreen(guiYesNo);
            break;
         } // delete
         case 6: {
            if (mail.ransom > 0) { Packets.sendServer(new SPacketPlayerMailRansom(mail.timeWhenReceived)); }
            else if (mail.money > 0) { Packets.sendServer(new SPacketPlayerMailTakeMoney(mail.timeWhenReceived)); }
            break;
         } // ransom
         case 7: {
            Packets.sendServer(new SPacketPlayerMailReturn(mail.timeWhenReceived));
            removeMail();
            aType = 1;
            animClose();
            break;
         } // return letter
      }
      updateButtons();
   }

   private void removeMail() {
      playerData.mailData.playerMails.removeIf(mailIn -> mailIn.timeWhenReceived == mail.timeWhenReceived && mailIn.sender.equals(mail.sender));
   }

   @Override
   public boolean charTyped(char typedChar, int keyCode) {
      if (GuiTextFieldNop.getActive() == null && menu.canEdit && SharedConstants.isAllowedChatCharacter(typedChar)) {
         pageEdit.insertText(Character.toString(typedChar));
         return true;
      }
      super.charTyped(typedChar, keyCode);
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!hasSubGui()) {
         if (GuiBasic.isEscKey(keyCode)) {
            aType = 0;
            animClose();
            return true;
         }
         if (menu.canEdit && bookKeyPressed(keyCode)) { return true; }
      }
      GuiBasic.checkAltH();
      return wrapper.keyPressed(keyCode, scanCode, modifiers);
   }

   // OLD: keyTypedInBook
   private boolean bookKeyPressed(int keyCode) {
      if (step != 5 || GuiTextFieldNop.getActive() != null) { return false; }
      if (Screen.isSelectAll(keyCode)) {
         pageEdit.selectAll();
         return true;
      }
      else if (Screen.isCopy(keyCode)) {
         pageEdit.copy();
         return true;
      }
      else if (Screen.isPaste(keyCode)) {
         if (menu.canEdit) { pageEdit.paste(); }
         return true;
      }
      else if (Screen.isCut(keyCode)) {
         if (menu.canEdit) { pageEdit.cut(); }
         else { pageEdit.copy(); }
         return true;
      }
      else {
         return switch (keyCode) {
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
               if (menu.canEdit) { pageEdit.insertText("\n"); }
               yield true;
            } // enter
            case InputConstants.KEY_BACKSPACE -> {
               if (menu.canEdit) { pageEdit.removeCharsFromCursor(-1); }
               yield true;
            } // backspace
            case InputConstants.KEY_DELETE -> {
               if (menu.canEdit) { pageEdit.removeCharsFromCursor(1); }
               yield true;
            } // delete
            case InputConstants.KEY_RIGHT -> {
               pageEdit.moveByChars(1, Screen.hasShiftDown());
               yield true;
            } // right
            case InputConstants.KEY_LEFT -> {
               pageEdit.moveByChars(-1, Screen.hasShiftDown());
               yield true;
            } // left
            case InputConstants.KEY_PAGEUP -> {
               buttonPreviousPage.onPress();
               yield true;
            } // page up
            case InputConstants.KEY_PAGEDOWN -> {
               buttonNextPage.onPress();
               yield true;
            } // page down
            default -> false;
         };
      }
   }

   private String getText() {
      return bookPages != null && currPage >= 0 && currPage < bookPages.size() ? bookPages.getString(currPage) : "";
   }

   private void setText(String str) {
      if (bookPages != null && currPage >= 0 && currPage < bookPages.size()) {
         bookPages.set(currPage, StringTag.valueOf(str));
      }
   }

   private void setClipboard(String clipboard) {
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      TextFieldHelper.setClipboardContents(minecraft, clipboard);
   }

   private String getClipboard() {
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      return TextFieldHelper.getClipboardContents(minecraft);
   }

   private void drawPlace(GuiGraphics graphics, float u, float v, int mouseX, int mouseY) {
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.translate(u, v, 0.0f);
      graphics.blit(mTable, 0, -5, 0, 0, 174, 248);
      matrixStack.popPose();
      if (step == 5) {
         // envelope
         matrixStack.pushPose();
         matrixStack.translate(guiLeft + 142.0f, guiTop - 10.0f, 0.0f);
         graphics.blit(mEnvelope, 5, 40, 0, 0, 164, 137);
         matrixStack.popPose();
         // list
         matrixStack.pushPose();
         matrixStack.translate(guiLeft + 9.0f, guiTop + 12.0f, 0.0f);
         graphics.blit(!menu.canEdit && !menu.canSend && mail.ransom > 0 ? mRansom : mList,
                 0, 0, 0, 0, 156, 134);
         matrixStack.popPose();
         // handle
         matrixStack.pushPose();
         matrixStack.translate(guiLeft + 49.0f, guiTop + 3.0f, 0.0f);
         graphics.blit(mTable, 0, 0, 174, 0, 76, 22);
         matrixStack.popPose();
         // box
         if (hasStacks || menu.canEdit) {
            matrixStack.pushPose();
            matrixStack.translate(guiLeft + 180.0f, guiTop + 179.0f, 0.0f);
            graphics.blit(mSendBox, 0, 0, 0, 0, 74, 54);
            matrixStack.popPose();
         }
         // slots
         matrixStack.pushPose();
         matrixStack.translate(guiLeft + 6.0f, guiTop + 167.0f, 0.0f);
         for (int j = 0; j < 9; j++) {
            for (int k = 0; k < 4; k++) {
               graphics.blit(mTable, 18 * j, k * 18 + (k == 3 ? 2 : 0), 174, 22, 18, 18);
            }
         }
         matrixStack.popPose();
         for (int slotId = 0; slotId < menu.slots.size(); slotId++) {
            Slot slot = menu.getSlot(slotId);
            if (slotId < 4) {
               boolean show = !menu.canEdit && !menu.canSend ? mail.ransom <= 0 && slot.hasItem() : menu.canEdit;
               if (show && slot.x == 250000) {
                  ((ISlotMixin) slot).setX(197 + (slotId % 2) * 23);
                  ((ISlotMixin) slot).setY(187 + (slotId / 2) * 23);
               } else if (!show && slot.x != 250000) {
                  ((ISlotMixin) slot).setX(250000);
                  ((ISlotMixin) slot).setY(250000);
               }
            } else if (slot.x == 250000) { // Inventory
               ((ISlotMixin) slot).setX(7 + ((slotId - 4) % 9) * 18);
               ((ISlotMixin) slot).setY(168 + ((slotId - 4) / 9) * 18 + (slotId >= 31 ? 2 : 0));
            }
         }
         int x = guiLeft + 170;
         int y = guiTop + 48;
         // player name
         GuiLabel l = getLabel(0);
         GuiTextFieldNop tf;
         GuiButtonNop b;
         if (l != null && l.getX() == 250000) {
            l.setX(x);
            l.setY(y);
         }
         if (menu.canEdit) {
            tf = getTextField(!menu.canSend ? 2 : 0);
            if (tf != null && tf.getX() == 250000) {
               tf.setX(x);
               tf.setY(y += 10);
            }
         }
         else {
            l = getLabel(10);
            if (l != null && l.getX() == 250000) {
               l.setX(x + 2);
               l.setY(y += 10);
            }
         }
         // title
         l = getLabel(1);
         if (l != null && l.getX() == 250000) {
            l.setX(x);
            l.setY(y += 18);
         }
         if (menu.canEdit) {
            tf = getTextField(1);
            if (tf != null && tf.getX() == 250000) {
               tf.setX(x);
               tf.setY(y += 10);
            }
         }
         else {
            l = getLabel(11);
            if (l != null && l.getX() == 250000) {
               l.setX(x);
               l.setY(y += 10);
            }
         }
         // ransom
         if (getLabel(7) != null) { getLabel(7).setBackColor(0); }
         if (getLabel(8) != null) { getLabel(8).setBackColor(0); }
         if (getButton(6) != null) {
            getButton(6).layerColor = 0;
            if (!menu.canEdit && !menu.canSend) {
               if (mail.ransom > 0) {
                  getButton(6).setIsEnabled(player.isCreative() || PlayerData.get(player).game.getMoney() >= mail.ransom);
               }
               else { getButton(6).setIsEnabled(mail.money > 0); }
            }
         }
         if (!menu.canEdit) {
            if (mail.ransom > 0 || mail.money > 0) {
               l = getLabel(7);
               if (l != null && l.getX() == 250000) {
                  l.setX(x);
                  l.setY(y + 18);
               }
               l = getLabel(8);
               if (l != null && l.getX() == 250000) {
                  l.setX(x + 2);
                  l.setY(y + 28);
               }
            }
         }
         l = error;
         if (l != null && l.getX() == 250000) {
            l.setX(x);
            l.setY(guiTop + 145);
         }
         // Moneys
         if (menu.canEdit) {
            l = getLabel(3);
            if (l != null && l.getX() == 250000) {
               l.setX(x);
               l.setY((y += 19) + 4);
            }
            l = getLabel(6);
            if (l != null && l.getX() == 250000) {
               l.setX(x + 102);
               l.setY(y + 4);
            }
            GuiTextFieldNop tf3 = getTextField(3);
            GuiTextFieldNop tf4 = getTextField(4);
            if (tf3 != null) {
               if (tf3.getX() == 250000) {
                  tf3.setX(x + 48);
                  tf3.setY(y);
               }
               int tfV = tf3.isEmpty() ? (int) tf3.def : tf3.isInteger() ? tf3.getInteger() : 0;
               if (tfV > 0) {
                  if (tf4 != null) { tf4.setIsVisible(false); }
                  if (l != null) { l.setIsEnabled(false); }
                  if (getLabel(7) != null) { getLabel(7).setIsEnabled(false); }
                  if (getLabel(8) != null) { getLabel(8).setIsEnabled(false); }
               } else {
                  if (tf4 != null) { tf4.setIsVisible(true); }
                  if (l != null) { l.setIsEnabled(true); }
                  if (getLabel(7) != null) { getLabel(7).setIsEnabled(true); }
                  if (getLabel(8) != null) { getLabel(8).setIsEnabled(true); }
               }
            }
            l = getLabel(7);
            if (l != null && l.getX() == 250000) {
               l.setX(x);
               l.setY((y += 19) + 4);
            }
            l = getLabel(8);
            if (l != null && l.getX() == 250000) {
               l.setX(x + 102);
               l.setY(y + 4);
            }
            if (tf4 != null) {
               if (tf4.getX() == 250000) {
                  tf4.setX(x + 48);
                  tf4.setY(y);
               }
               int tfV = tf4.isEmpty() ? (int) tf4.def : tf4.isInteger() ? tf4.getInteger() : 0;
               if (tfV > 0) {
                  if (tf3 != null) { tf3.setIsVisible(false); }
                  if (getLabel(3) != null) { getLabel(3).setIsEnabled(false); }
                  if (getLabel(6) != null) { getLabel(6).setIsEnabled(false); }
               } else {
                  if (tf3 != null) { tf3.setIsVisible(true); }
                  if (getLabel(3) != null) { getLabel(3).setIsEnabled(true); }
                  if (getLabel(6) != null) { getLabel(6).setIsEnabled(true); }
               }
            }
         }
         x = guiLeft + 7;
         y = guiTop + 149;
         if (menu.canEdit && !menu.canSend) {
            b = getButton(0);
            if (b != null && b.getX() == 250000) {
               b.setX(x + 52);
               b.setY(y);
            }
         } // dialog/quest add to
         else if (menu.canEdit) {
            b = getButton(0);
            if (b != null && b.getX() == 250000) {
               b.setX(x + 52);
               b.setY(y);
            }
         } // write
         if (!menu.canEdit && !menu.canSend) {
            if (mail.ransom > 0 || mail.money > 0) {
               b = getButton(6);
               if (b != null && b.getX() == 250000) {
                  b.setX(x + 220);
                  b.setY(y - 42);
               }
            }
            b = getButton(4);
            if (b != null && b.getX() == 250000) {
               b.setX(x);
               b.setY(y);
            }
            b = getButton(7);
            if (b != null && b.getX() == 250000) {
               b.setX(x + 52);
               b.setY(y);
            }
         } // read -> delete
         if (!menu.canEdit || menu.canSend) {
            b = getButton(3);
            if (b != null && b.getX() == 250000) {
               b.setX(x + 104);
               b.setY(y);
            }
         } // write -> cancel
         if (buttonNextPage != null && buttonNextPage.getX() == 250000 && (menu.canEdit || mail.ransom == 0)) {
            buttonNextPage.setX(x + 135);
            buttonNextPage.setY(y - 16);
         }
         if (buttonPreviousPage != null && buttonPreviousPage.getX() == 250000) {
            buttonPreviousPage.setX(x);
            buttonPreviousPage.setY(y - 16);
         }
         // Text
         String s = Component.translatable("book.pageIndicator", currPage + 1, bookTotalPages).getString();
         String totalText = "";
         String drawEnd = "";
         if (bookPages != null && currPage >= 0 && currPage < bookPages.size()) { totalText = bookPages.getString(currPage); }
         if (menu.canEdit) {
            int cursorPos = pageEdit.getCursorPos();
            cursorPos = Math.min(cursorPos, totalText.length());
            String beforeCursor = totalText.substring(0, cursorPos);
            String afterCursor = totalText.substring(cursorPos);
            String cursorStr = ((updateCount / 6 % 2 == 0) ? ChatFormatting.BLACK : ChatFormatting.GRAY) + "_" + ChatFormatting.RESET;
            String withCursor = beforeCursor + cursorStr + afterCursor;

            if (font.wordWrapHeight(withCursor, 152) > 108) {
               // Текст с курсором не влезает — рисуем курсор отдельно (в конце видимой области)
               if (updateCount / 6 % 2 == 0) { drawEnd = ChatFormatting.BLACK + "_" + ChatFormatting.RESET; }
               else { drawEnd = ChatFormatting.GRAY + "_" + ChatFormatting.RESET; }
            } else {
               totalText = withCursor;
            }
         }
         else if (mail.ransom > 0) {
            StringBuilder newText = new StringBuilder();
            char g = ((char) 167);
            boolean rnd = false;
            for (int i = 0; i < totalText.length(); i++) {
               char c = totalText.charAt(i);
               if (font.wordWrapHeight(newText + "" + c, 152) > 9) {
                  newText.append(g).append("k");
                  rnd = true;
               }
               if (!rnd || c == ((char) 10) || c == ' ') { newText.append(c); }
               else { newText.append(' '); }
            }
            totalText = newText.toString();
         }
         if (buttonNextPage != null && bookPages != null) {
            buttonNextPage.setIsVisible((menu.canEdit && !bookPages.getString(currPage).isEmpty()) || currPage < bookTotalPages - 1);
         }

         if (minecraft == null) { minecraft = Minecraft.getInstance(); }
         if (!menu.canEdit && !menu.canSend && mail.ransom > 0) {
            if (isMouseHover(mouseX, mouseY, guiLeft + 11, guiTop + 36, 152, 108)) {
               setHoverText("mailbox.hover.ransom.sell");
               getLabel(7).setBackColor(new Color(0x80FF0000).getRGB());
               getLabel(8).setBackColor(new Color(0x80FF0000).getRGB());
               getButton(6).layerColor = new Color(0xFFF00000).getRGB();
            }
         }
         else {
            if (!drawEnd.isEmpty()) { graphics.drawString(font, drawEnd, guiLeft + 159, guiTop + 127, 0, false); }
            graphics.drawWordWrap(minecraft.font, Component.translatable(totalText),
                    guiLeft + 15, guiTop + 30, 144, 0);
            if (!totalText.isEmpty() || menu.canEdit || currPage > 0) {
               int ls = minecraft.font.width(s);
               graphics.drawString(font, s, guiLeft + 128 - ls, guiTop + 132,
                       CustomNpcs.LableColor.getRGB(), false);
            }
         }
         // add slots
         for (int slotId = 0; slotId < 4; slotId++) {
            Slot slot = menu.getSlot(slotId);
            boolean show = !menu.canEdit && !menu.canSend ? slot.hasItem() : slot.x != 250000;
            if (show) {
               int px = guiLeft + 193 + 23 * (slotId % 2);
               int py = guiTop + 183 + 23 * (slotId / 2);
               matrixStack.pushPose();
               matrixStack.translate(px, py, 0.0f);
               graphics.fillGradient(3, 3, 21, 21,
                       new Color(0xC0101010).getRGB(),
                       new Color(0xD0101010).getRGB());
               graphics.blit(widgets, 0, 0, 0, 22, 24, 24);
               matrixStack.popPose();
               if (!menu.canEdit && !menu.canSend && mail.ransom > 0) {
                  matrixStack.pushPose();
                  matrixStack.translate(px + 4, py + 4, 0.0f);
                  graphics.renderItem(slot.getItem(), 0, 0);
                  graphics.renderItemDecorations(font, slot.getItem(), 0, 0);
                  matrixStack.translate(0.0f, 0.0f, 200.0f);
                  if (isMouseHover(mouseX, mouseY, px, py, 18, 18)) {
                     List<Component> list = new ArrayList<>();
                     list.add(Component.translatable("mailbox.hover.ransom.sell"));
                     list.addAll(slot.getItem().getTooltipLines(player,
                             minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL));
                     setHoverText(list);
                     getLabel(7).setBackColor(new Color(0x80FF0000).getRGB());
                     getLabel(8).setBackColor(new Color(0x80FF0000).getRGB());
                     getButton(6).layerColor = new Color(0xFFF00000).getRGB();
                  }
                  graphics.blit(GuiMailbox.icons, -2, -2, 0, 32, 20, 20);
                  matrixStack.popPose();
               }
            }
         }
      }
      else {
         for (int slotId = 0; slotId < menu.slots.size(); slotId++) {
            Slot slot = menu.getSlot(slotId);
            if (slot.x == 250000) { continue; }
            ((ISlotMixin) slot).setX(250000);
            ((ISlotMixin) slot).setY(250000);
         }
         if (buttonNextPage != null && buttonNextPage.getX() != 250000) {
            buttonNextPage.setX(250000);
            buttonNextPage.setY(250000);
         }
         if (buttonPreviousPage != null && buttonPreviousPage.getX() != 250000) {
            buttonPreviousPage.setX(250000);
            buttonPreviousPage.setY(250000);
         }
         for (int i = 0; i < 12; i++) {
            GuiLabel l = getLabel(i);
            if (l != null && l.getX() != 250000) {
               l.setX(250000);
               l.setY(250000);
            }
            GuiButtonNop b = getButton(i);
            if (b != null && b.getX() != 250000) {
               b.setX(250000);
               b.setY(250000);
            }
            GuiTextFieldNop tf = getTextField(i);
            if (tf != null && tf.getX() != 250000) {
               tf.setX(250000);
               tf.setY(250000);
            }
         }
      }
   }

   @Override
   public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderBackground(graphics);
      PoseStack matrixStack = graphics.pose();
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      if (errTick != 0L && minecraft.level != null && errTick < minecraft.level.getGameTime()) {
         errTick = 0L;
         if (error != null) { error.setMessage(""); }
      }
      // Info
      if (!menu.canEdit && mail.ransom > 0) {
         graphics.blit(GuiMailbox.icons, guiLeft + 33, guiTop + 43, 0, 126, 120, 130);
         graphics.drawCenteredString(font, Component.translatable("mailbox.hover.ransom.sell"),
                 guiLeft + 87, guiTop + 100,
                 CustomNpcs.MainColor.getRGB() | 0xFF000000);
      }
      boolean hasMail = false;
      cost.clear();
      long c = CustomNpcs.MailCostSendingLetter[0];
      cost.put(0, c);
      totalCost = c;
      if (bookPages != null) {
         c = 0;
         for (int i = 0; i < bookPages.size(); ++i) {
            if (bookPages.getString(i).isEmpty()) { continue; }
            c += CustomNpcs.MailCostSendingLetter[1];
            hasMail = true;
         }
         cost.put(1, c);
         totalCost += c;
      }
      c = 0;
      hasStacks = false;
      for (int i = 0; i < 4; i++) {
         Slot slot = menu.getSlot(i);
         if (slot.getItem().isEmpty()) { continue; }
         c += (int) ((float) CustomNpcs.MailCostSendingLetter[2] * (float) slot.getItem().getCount()
                 / (float) slot.getItem().getMaxStackSize());
         hasStacks = true;
         hasMail = true;
      }
      cost.put(3, c);
      totalCost += c;
      if (mail.money > 0) {
         c = (long) ((float) mail.money * (float) CustomNpcs.MailCostSendingLetter[3] / 100.0f);
         cost.put(2, c);
         totalCost += c;
         totalCost += mail.money;
         hasMail = true;
      }
      if (mail.ransom > 0) {
         c = (long) (int) ((float) mail.ransom * (float) CustomNpcs.MailCostSendingLetter[4]
                 / 100.0f);
         cost.put(4, c);
         totalCost += c;
      }
      if (getLabel(5) != null) {
         getLabel(5).setMessage(Component.translatable("mailbox.cost.send",
                 "" + (totalCost == 0L ? 0
                         : Util.instance.getTextReducedNumber(totalCost, true, false, false)),
                 CustomNpcs.displayCurrencies));
      }
      if (menu.canEdit && menu.canSend && getButton(0) != null) {
         type = 0;
         type = !player.isCreative() && username.equals(player.getName().getString()) && !CustomNpcs.MailSendToYourself ? 3 : 0;
         if (type == 0) { type = getTextField(0) != null && getTextField(0).getValue().isEmpty() ? 1 : 0; } // player
         if (type == 0) { type = mail.title.isEmpty() ? 4 : 0; } // title
         if (type == 0 && !player.isCreative()) { type = PlayerData.get(player).game.getMoney() < totalCost ? 2 : 0; } // money
         if (type == 0 && !hasMail) { type = 5; } // empty
         getButton(0).setIsEnabled(type == 0);
      }
      // Animations
      matrixStack.pushPose();
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
      if (tick >= 0) {
         if (tick == 0) { partialTicks = 0.0f; }
         float part = (float) tick + partialTicks;
         float cos = ValueUtil.correctFloat((float) Math.cos(90.0d * part / (double) milliTick * Math.PI / 180.0d), 0.0f, 1.0f);
         switch (step) {
            case 0: {
               float u = guiLeft + (1.0f - cos) * 174.0f;
               float v = guiTop + (1.0f - cos) * 248.0f;
               drawPlace(graphics, u, v, mouseX, mouseY);
               matrixStack.pushPose();
               matrixStack.translate(u, v, 2.0f);
               graphics.blit(mEnvelope, 5, 40, 0, 0, 164, 137);
               graphics.blit(mSendBox, 53, 168, 0, 54, 68, 74);
               graphics.blit(mTable, 49, 3, 174, 0, 76, 22);
               matrixStack.popPose();
               if (tick == 0) {
                  step = 1;
                  setNextTick(21, true);
                  MusicController.Instance.playSound(SoundSource.PLAYERS, CustomNpcs.MODID + ":book.down",
                          player.getX(), player.getY(), player.getZ(), 1.0f,
                          0.75f + 0.25f * rnd.nextFloat());
               }
               break;
            } // open
            case 1: {
               drawPlace(graphics, guiLeft, guiTop, mouseX, mouseY);
               matrixStack.pushPose();
               matrixStack.translate(guiLeft, guiTop + cos * 150.0f, 2.0f);
               graphics.blit(mEnvelope, 5, 40, 0, 0, 164, 137);
               matrixStack.popPose();
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + cos * 183.0f, guiTop + cos * 169.0f, 2.0f);
               graphics.blit(mSendBox, 0, 0, 0, 54, 68, 74);
               matrixStack.popPose();
               // handle
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 49.0f, guiTop + 3.0f, 0.0f);
               graphics.blit(mTable, 0, 0, 174, 0, 76, 22);
               matrixStack.popPose();
               if (tick == 0) {
                  step = 2;
                  setNextTick(21, true);
                  MusicController.Instance.playSound(SoundSource.PLAYERS, CustomNpcs.MODID + ":book.sheet",
                          player.getX(), player.getY(), player.getZ(), 1.0f,
                          0.8f + 0.4f * rnd.nextFloat());
               }
               break;
            } // box and envelope_0 offset
            case 2: {
               drawPlace(graphics, guiLeft, guiTop, mouseX, mouseY);
               // envelope_0
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 5, guiTop + 190.0f, 2.0f);
               graphics.blit(mEnvelope, 0, 0, 0, 0, 164, 137);
               matrixStack.popPose();
               // list
               float v = guiTop + 150.0f - cos * 134.0f;
               int h = 186 - (int) v;
               if (h > 134) {
                  h = 134;
               }
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 9, v, 2.0f);
               graphics.blit(mList, 0, 0, 0, 0, 156, h);
               matrixStack.popPose();
               // handle
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 49.0f, guiTop + 3.0f, 2.0f);
               graphics.blit(mTable, 0, 0, 174, 0, 76, 22);
               matrixStack.popPose();
               // box
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
               graphics.blit(mSendBox, 0, 0, 0, 54, 68, 74);
               graphics.blit(mTable, 25, 27, 174, 22, 18, 18);
               graphics.blit(mSendBox, 0, 41, 0, 95, 68, 33);
               matrixStack.popPose();
               if (tick == 0) {
                  step = 3;
                  setNextTick(16, true);
                  MusicController.Instance.playSound(SoundSource.PLAYERS, CustomNpcs.MODID + ":book.sheet",
                          player.getX(), player.getY(), player.getZ(), 1.0f,
                          0.8f + 0.4f * rnd.nextFloat());
               }
               break;
            } // box added slots and show list
            case 3: {
               drawPlace(graphics, guiLeft, guiTop, mouseX, mouseY);
               // envelope_1
               float u = guiLeft + cos * 142.0f;
               float v = guiTop + 150.0f - cos * 160.0f;
               matrixStack.pushPose();
               matrixStack.translate(u, v, 2.0f);
               graphics.blit(mEnvelope, 5, 40, 0, 0, 164, 137);
               matrixStack.popPose();
               // list
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 9.0f, guiTop + 12.0f, 2.0f);
               graphics.blit(mList, 0, 0, 0, 0, 156, 134);
               matrixStack.popPose();
               // handle
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 49.0f, guiTop + 3.0f, 2.0f);
               graphics.blit(mTable, 0, 0, 174, 0, 76, 22);
               matrixStack.popPose();
               // box
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
               graphics.blit(mSendBox, 0, 0, 0, 54, 68, 74);
               graphics.blit(mTable, 25, 27, 174, 22, 18, 18);
               graphics.blit(mSendBox, 0, 41, 0, 95, 68, 33);
               matrixStack.popPose();
               if (tick == 0) {
                  step = 4;
                  setNextTick(36, true);
               }
               break;
            } // envelope_1 offset
            case 4: {
               drawPlace(graphics, guiLeft, guiTop, mouseX, mouseY);
               // envelope
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 142.0f, guiTop - 10.0f, 2.0f);
               graphics.blit(mEnvelope, 5, 40, 0, 0, 164, 137);
               matrixStack.popPose();
               // list
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 9.0f, guiTop + 12.0f, 2.0f);
               graphics.blit(mList, 0, 0, 0, 0, 156, 134);
               matrixStack.popPose();
               // handle
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 49.0f, guiTop + 3.0f, 2.0f);
               graphics.blit(mTable, 0, 0, 174, 0, 76, 22);
               matrixStack.popPose();
               // box
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
               graphics.blit(mSendBox, 0, 0, 0, 54, 68, 74);
               matrixStack.popPose();
               // slots
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 208.0f, guiTop + 196.0f, 2.0f);
               graphics.blit(mTable, 0, 0, 174, 22, 18, 18);
               double h = 36.0d;
               int x0 = 0, y0 = 0;
               for (int s = 0; s < 36; s++) {
                  int t = tick + s - 35;
                  int x = s % 9;
                  int y = (int) Math.floor((double) s / 9.0d);
                  int x1 = x * 18 - 202;
                  int y1 = y * 18 + (y == 3 ? 2 : 0) - 29;
                  if (t < 0) {
                     graphics.blit(mTable, x1, y1, 174, 22, 18, 18);
                  } else {
                     cos = (float) Math.cos(90.0d * ((double) t + partialTicks) / 35.0d * Math.PI / 180.0d);
                     if (cos < 0.0f) {
                        cos = 0.0f;
                     } else if (cos > 1.0f) {
                        cos = 1.0f;
                     }
                     double px = x0 - x1, py = y0 - y1;
                     double dist = Math.sqrt(px * px + py * py);
                     double r = (Math.pow(dist / 2.0d, 2.0d) + Math.pow(h, 2.0d)) / (2.0d * h);
                     double tg = Math.abs(py / px);
                     double angle = Math.atan(tg) * 180.0d / Math.PI;
                     double cx = (x0 - x1) / 2.0d + x1;
                     double cy = (y0 - y1) / 2.0d + y1;
                     double rx = cx + Math.sin(angle * Math.PI / 180) * (r - h) * -1;
                     double ry = cy + Math.cos(angle * Math.PI / 180) * (r - h);
                     px = x0 - rx;
                     py = y0 - ry;
                     tg = Math.abs(py / px);
                     double startAngle = Math.atan(tg) * 180.0d / Math.PI;
                     px = x1 - rx;
                     py = y1 - ry;
                     tg = Math.abs(py / px);
                     angle = 180.0d - startAngle - Math.atan(tg) * 180.0d / Math.PI;
                     double nowAngle = cos * angle + startAngle;
                     x1 = (int) (rx + Math.cos(nowAngle * Math.PI / 180) * r);
                     y1 = (int) (ry + Math.sin(nowAngle * Math.PI / 180) * r * -1);
                     graphics.blit(mTable, x1, y1, 174, 22, 18, 18);
                  }
               }
               matrixStack.popPose();
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
               graphics.blit(mSendBox, 0, 41, 0, 95, 68, 33);
               matrixStack.popPose();
               if (tick % 4 == 0) {
                  MusicController.Instance.playSound(SoundSource.PLAYERS, CustomNpcs.MODID + ":mail.slot",
                          player.getX(), player.getY(), player.getZ(), 1.0f,
                          0.7f + 0.4f * rnd.nextFloat());
               }
               if (tick == 0) {
                  step = 5;
                  milliTick = 0;
               }
               break;
            } // slots
            case 6: {
               drawPlace(graphics, guiLeft, guiTop, mouseX, mouseY);
               if (tick == 0) {
                  step = 7;
                  setNextTick(36, false);
               }
            } // simple close/back
            case 7: {
               // table
               matrixStack.pushPose();
               matrixStack.translate(guiLeft, guiTop, 0.0f);
               graphics.blit(mTable, 0, -5, 0, 0, 174, 248);
               matrixStack.popPose();
               // envelope
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 142.0f, guiTop - 10.0f, 2.0f);
               graphics.blit(mEnvelope, 5, 40, 0, 0, 164, 137);
               matrixStack.popPose();
               // list
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 9.0f, guiTop + 12.0f, 2.0f);
               graphics.blit(mList, 0, 0, 0, 0, 156, 134);
               matrixStack.popPose();
               // handle
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 49.0f, guiTop + 3.0f, 2.0f);
               graphics.blit(mTable, 0, 0, 174, 0, 76, 22);
               matrixStack.popPose();
               // box
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
               graphics.blit(mSendBox, 0, 0, 0, 54, 68, 74);
               matrixStack.popPose();
               // slots
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 208.0f, guiTop + 196.0f, 2.0f);
               graphics.blit(mTable, 0, 0, 174, 22, 18, 18);
               int x0 = 0, y0 = 0;
               for (int s = 0; s < 36; s++) {
                  int x = s % 9;
                  int y = (int) Math.floor((double) s / 9.0d);
                  int x1 = x * 18 - 202;
                  int y1 = y * 18 + (y == 3 ? 2 : 0) - 29;
                  double px = x0 - x1, py = y0 - y1;
                  graphics.blit(mTable, (int) (x1 + px * cos), (int) (y1 + py * cos), 174, 22, 18, 18);
               }
               matrixStack.popPose();
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 183.0f, guiTop + 169.0f, 2.0f);
               graphics.blit(mSendBox, 0, 41, 0, 95, 68, 33);
               matrixStack.popPose();
               if (tick % 6 == 0) {
                  MusicController.Instance.playSound(SoundSource.PLAYERS, CustomNpcs.MODID + ":mail.slot",
                          player.getX(), player.getY(), player.getZ(), 1.0f,
                          0.9f + 0.3f * rnd.nextFloat());
               }
               if (tick == 0) {
                  step = aType == 0 ? 8 : aType == 1 ? 10 : 13;
                  setNextTick(31, true);
                  MusicController.Instance.playSound(SoundSource.PLAYERS, CustomNpcs.MODID + ":book.sheet",
                          player.getX(), player.getY(), player.getZ(), 1.0f,
                          0.8f + 0.4f * rnd.nextFloat());
               }
               break;
            } // 0 _ simple close/back
            case 8: {
               // table
               matrixStack.pushPose();
               matrixStack.translate(guiLeft, guiTop, 0.0f);
               graphics.blit(mTable, 0, -5, 0, 0, 174, 248);
               matrixStack.popPose();
               float u = guiLeft + 9.0f;
               float v = guiTop + 12.0f + cos * 53.0f;
               // list
               matrixStack.pushPose();
               matrixStack.translate(u, v, 2.0f);
               graphics.blit(mList, 0, 0, 0, 0, 156, 134);
               matrixStack.popPose();
               // handle
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 49.0f, guiTop + 3.0f, 2.0f);
               graphics.blit(mTable, 0, 0, 174, 0, 76, 22);
               matrixStack.popPose();
               u = guiLeft + 142.0f - cos * 142.0f;
               v = guiTop - 10.0f + cos * 34.0f;
               // envelope
               matrixStack.pushPose();
               matrixStack.translate(u, v, 2.0f);
               graphics.blit(mEnvelope, 5, 40, 0, 0, 164, 137);
               matrixStack.popPose();
               // box
               u = guiLeft + 183.0f - cos * 130.0f;
               v = guiTop + 169.0f - cos * 75.0f;
               matrixStack.pushPose();
               matrixStack.translate(u, v, 2.0f);
               graphics.blit(mSendBox, 0, 0, 0, 54, 68, 74);
               matrixStack.popPose();
               if (tick == 0) {
                  step = 9;
                  setNextTick(31, true);
               }
               break;
            } // 1 _ simple close/back
            case 9: {
               // table
               float u = guiLeft + cos * 174.0f;
               float v = guiTop + cos * 248.0f;
               matrixStack.pushPose();
               matrixStack.translate(u, v, 2.0f);
               graphics.blit(mTable, 0, -5, 0, 0, 174, 248);
               graphics.blit(mTable, 49, 3, 174, 0, 76, 22);
               // envelope
               matrixStack.pushPose();
               matrixStack.translate(u, v + 24.0f, 2.0f);
               graphics.blit(mEnvelope, 5, 40, 0, 0, 164, 137);
               matrixStack.popPose();
               // box
               matrixStack.pushPose();
               matrixStack.translate(u + 53.0f, v + 94.0f, 2.0f);
               graphics.blit(mSendBox, 0, 0, 0, 54, 68, 74);
               matrixStack.popPose();
               if (tick == 0) {
                  step = 0;
                  milliTick = 0;
                  onClose();
               }
               break;
            } // 2 _ simple close/back
            case 10: {
               // box
               if (hasStacks) {
                  matrixStack.pushPose();
                  matrixStack.translate(guiLeft + 183.0f, guiTop + 169.0f, 0.0f);
                  graphics.blit(mSendBox, 0, 0, 74, 0, 74, 54);
                  matrixStack.popPose();
               } else {
                  matrixStack.pushPose();
                  matrixStack.translate(guiLeft + 183.0f, guiTop + 169.0f, 0.0f);
                  graphics.blit(mSendBox, 0, 0, 0, 54, 68, 74);
                  matrixStack.popPose();
               }
               // table
               matrixStack.pushPose();
               matrixStack.translate(guiLeft, guiTop, 0.0f);
               graphics.blit(mTable, 0, -5, 0, 0, 174, 248);
               graphics.blit(mTable, 49, 3, 174, 0, 76, 22);
               matrixStack.popPose();
               // list
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 9.0f, guiTop + 12.0f, 0.0f);
               graphics.blit(mList, 0, 0, 0, 0, 156, 134);
               matrixStack.popPose();
               // envelope
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 142.0f - cos * 142.0f, guiTop - 10.0f + cos * 160.0f,
                       1.0f);
               graphics.blit(mEnvelope, 5, 40, 0, 0, 164, 137);
               matrixStack.popPose();
               if (tick == 0) {
                  step = 11;
                  setNextTick(16, true);
                  MusicController.Instance.playSound(SoundSource.PLAYERS, CustomNpcs.MODID + ":book.sheet",
                          player.getX(), player.getY(), player.getZ(), 1.0f,
                          0.8f + 0.4f * rnd.nextFloat());
               }
               break;
            } // 1 _ send
            case 11: {
               // table
               matrixStack.pushPose();
               matrixStack.translate(guiLeft, guiTop, 0.0f);
               graphics.blit(mTable, 0, -5, 0, 0, 174, 248);
               graphics.blit(mTable, 49, 3, 174, 0, 76, 22);
               matrixStack.popPose();
               // envelope
               matrixStack.pushPose();
               matrixStack.translate(guiLeft, guiTop + 150.0f, 0.0f);
               graphics.blit(mEnvelope, 5, 40, 0, 0, 164, 137);
               matrixStack.popPose();
               // list
               float v = guiTop + 150.0f - (1.0f - cos) * 134.0f;
               int h = 186 - (int) v;
               if (h > 134) {
                  h = 134;
               }
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 9, v, 2.0f);
               graphics.blit(mList, 0, 0, 0, 0, 156, h);
               matrixStack.popPose();
               if (hasStacks) {
                  matrixStack.pushPose();
                  matrixStack.translate(guiLeft + 183.0f, guiTop + 169.0f, 0.0f);
                  graphics.blit(mSendBox, 0, 0, 74, 0, 74, 54);
                  matrixStack.popPose();
               }
               if (tick == 0) {
                  step = 12;
                  setNextTick(31, true);
                  MusicController.Instance.playSound(SoundSource.PLAYERS, CustomNpcs.MODID + ":book.sheet",
                          player.getX(), player.getY(), player.getZ(), 1.0f,
                          0.8f + 0.4f * rnd.nextFloat());
               }
               break;
            } // 2 _ send
            case 12: {
               // table
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + cos * 174.0f, guiTop + cos * 248.0f, 0.0f);
               graphics.blit(mTable, 0, -5, 0, 0, 174, 248);
               graphics.blit(mTable, 49, 3, 174, 0, 76, 22);
               matrixStack.popPose();
               // envelope
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + cos * 300.0f, guiTop + 150.0f - cos * 248.0f, 1.0f);
               graphics.blit(mEnvelope, 5, 40, 0, 0, 164, 137);
               matrixStack.popPose();
               if (hasStacks) {
                  matrixStack.pushPose();
                  matrixStack.translate(guiLeft + 183.0f + cos * 300.0f, guiTop + 169.0f - cos * 248.0f, 0.0f);
                  graphics.blit(mSendBox, 0, 0, 74, 0, 74, 54);
                  matrixStack.popPose();
               }
               if (tick == 0) {
                  step = 0;
                  milliTick = 0;
                  onClose();
               }
               break;
            } // 3 _ send
            case 13: {
               // table
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + cos * 174.0f, guiTop + cos * 248.0f, 0.0f);
               graphics.blit(mTable, 0, -5, 0, 0, 174, 248);
               graphics.blit(mTable, 49, 3, 174, 0, 76, 22);
               matrixStack.popPose();
               // box
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 183.0f - cos * 41.0f, guiTop + 169.0f - cos * 74.0f, 1.0f);
               graphics.blit(mSendBox, 0, 0, 0, 54, 68, 74);
               matrixStack.popPose();
               // envelope
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 147.0f - cos * 54.0f, guiTop + 30.0f + cos * 33.0f, 2.0f);
               graphics.blit(mEnvelope, 0, 0, 0, 0, 164, 137);
               matrixStack.popPose();
               // list
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 9.0f + cos * 88.0f, guiTop + 12.0f + cos * 52.0f, 2.0f);
               graphics.blit(mList, 0, 0, 0, 0, 156, 134);
               matrixStack.popPose();
               if (tick == 0) {
                  step = 14;
                  setNextTick(16, true);
                  MusicController.Instance.playSound(SoundSource.PLAYERS, CustomNpcs.MODID + ":mail.delete",
                          player.getX(), player.getY(), player.getZ(), 1.0f,
                          0.8f + 0.4f * rnd.nextFloat());
               }
               break;
            } // 1 _ delete
            case 14: {
               // envelope
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 93.0f, guiTop + 63.0f, 1.0f);
               RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f - cos);
               graphics.blit(mEnvelope, 0, 0, 0, 0, 164, 137);
               matrixStack.popPose();
               // list
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 97.0f, guiTop + 64.0f, 1.0f);
               RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f - cos);
               graphics.blit(mList, 0, 0, 0, 0, 156, 134);
               matrixStack.popPose();
               // list
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 97.0f + cos * 28.0f, guiTop + 64.0f + cos * 19.5f, 2.0f);
               matrixStack.scale(1.31092f - 0.47059f * cos, 1.09836f - 0.31967f * cos, 1.0f);
               RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, cos);
               graphics.blit(mList, 0, 0, 0, 134, 119, 122);
               matrixStack.popPose();
               if (tick == 0) {
                  step = 15;
                  setNextTick(31, true);
               }
               break;
            } // 2 _ delete
            case 15: {
               // list
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 97.0f + 28.0f, guiTop + 64.0f + 19.5f, 1.0f);
               matrixStack.scale(1.31092f - 0.47059f, 1.09836f - 0.31967f, 1.0f);
               RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f - cos);
               graphics.blit(mList, 0, 0, 0, 134, 119, 122);
               matrixStack.popPose();

               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 130.0f + cos * 7.5f, guiTop + 88.0f + cos * 7.125f, 2.0f);
               matrixStack.scale(0.85f - 0.15f * cos, 0.85f - 0.15f * cos, 1.0f);
               RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, cos);
               graphics.blit(mList, 0, 0, 156, 0, 100, 95);
               matrixStack.popPose();
               if (tick == 0) {
                  step = 16;
                  setNextTick(31, true);
               }
               break;
            } // 3 _ delete
            case 16: {
               matrixStack.pushPose();
               matrixStack.translate(guiLeft + 137.5f - cos * 300.0f, guiTop + 95.125f - cos * 150.0f, 2.0f);
               matrixStack.scale(0.6f, 0.6f, 1.0f);
               graphics.blit(mList, 0, 0, 156, 0, 100, 95);
               matrixStack.popPose();
               if (tick == 0) {
                  step = 0;
                  milliTick = 0;
                  onClose();
               }
               break;
            } // 4 _ delete
         }
         tick--;
      }
      else { drawPlace(graphics, guiLeft, guiTop, mouseX, mouseY); }
      matrixStack.popPose();
      if (step != 5) { return; }
      if (getButton(0) != null) {
         if (!menu.canSend) { getButton(0).setHoverTexts("mailbox.hover.done"); } // done
         else {
            if (type == 0) {
               MutableComponent mes = Component.translatable("mailbox.hover.send.0",
                       Util.instance.ticksToElapsedTime(CustomNpcs.MailTimeWhenLettersWillBeReceived[1] * 20L, false, true, true),
                       "" + totalCost,
                       CustomNpcs.displayCurrencies);
               for (int i : cost.keySet()) {
                  if (cost.get(i) > 0L) {
                     String p0 = "" + cost.get(i);
                     String p1 = "", p2 = "";
                     switch (i) {
                        case 1:
                           p1 = "" + CustomNpcs.MailCostSendingLetter[1];
                           break;
                        case 2:
                           p0 = "" + mail.money;
                           p1 = "" + cost.get(i);
                           p2 = "" + CustomNpcs.MailCostSendingLetter[3];
                           break;
                        case 3:
                           p1 = "" + CustomNpcs.MailCostSendingLetter[2];
                           break;
                        case 4:
                           p1 = "" + CustomNpcs.MailCostSendingLetter[4];
                           break;
                     }
                     mes.append(Component.translatable("mailbox.hover.send.2." + i,
                             p0, CustomNpcs.displayCurrencies, p1, CustomNpcs.displayCurrencies, p2));
                  }
               }
               getButton(0).setHoverTexts(mes);
            }
            else if (type == 2) {
               MutableComponent mes = Component.translatable("mailbox.hover.send.2",
                       "" + totalCost, CustomNpcs.displayCurrencies);
               for (int i : cost.keySet()) {
                  if (cost.get(i) > 0L) {
                     String p0 = "" + cost.get(i);
                     String p1 = "", p2 = "";
                     switch (i) {
                        case 1:
                           p1 = "" + CustomNpcs.MailCostSendingLetter[1];
                           break;
                        case 2:
                           p0 = "" + mail.money;
                           p1 = "" + cost.get(i);
                           p2 = "" + CustomNpcs.MailCostSendingLetter[3];
                           break;
                        case 3:
                           p1 = "" + CustomNpcs.MailCostSendingLetter[2];
                           break;
                        case 4:
                           p1 = "" + CustomNpcs.MailCostSendingLetter[4];
                           break;
                        default:
                           break;
                     }
                     mes.append(Component.translatable("mailbox.hover.send.2." + i, p0,
                             CustomNpcs.displayCurrencies, p1, CustomNpcs.displayCurrencies, p2));
                  }
               }
               getButton(0).setHoverTexts(mes);
            }
            else {
               getButton(0).setHoverTexts(Component.translatable("mailbox.hover.send." + type));
            }
         }
      }
      super.render(graphics, mouseX, mouseY, partialTicks);
      // Player Money
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
      if (menu.canSend) {
         int x = guiLeft + 166;
         int y = guiTop + 150;
         matrixStack.pushPose();
         matrixStack.translate(x, y, 0.0f);
         float sc = 16.0f / 250.f;
         matrixStack.scale(sc, sc, sc);
         graphics.blit(GuiBasic.MONEY, 0, 0, 0, 0, 256, 256);
         matrixStack.popPose();
         matrixStack.pushPose();
         graphics.drawString(font, Util.instance.getTextReducedNumber(PlayerData.get(player).game.getMoney(), true, true, false)
                 + CustomNpcs.displayCurrencies, x + 15, y + 8.0f / 2.0f, new Color(0x404040).getRGB(), false);
         matrixStack.popPose();
      }
   }

   @Override
   public void unFocused(GuiTextFieldNop textField) {
      switch (textField.id) {
         case 0: username = textField.getValue(); break;
         case 1: mail.title = textField.getValue(); break;
         case 2: mail.sender = textField.getValue(); break;
         case 3: {
            mail.money = textField.getInteger();
            textField.setValue("" + mail.money);
            textField.setMinMaxDefault(textField.min, textField.max, mail.money);
            break;
         }
         case 4: {
            mail.ransom = textField.getInteger();
            textField.setValue("" + mail.ransom);
            textField.setMinMaxDefault(textField.min, textField.max, mail.ransom);
            break;
         }
      }
   }

   @Override
   public void setError(int id, CompoundTag data) {
      hasSend = false;
      if (error == null) { return; }
      error.setMessage(switch (id) {
         case 1 -> "mailbox.error.subject";
         case 2 -> "mailbox.error.yourself";
         case 3 -> "mailbox.error.nomoney";
         default -> "mailbox.error.username";
      });
      if (minecraft == null) { minecraft = Minecraft.getInstance(); }
      if (minecraft.level != null) { errTick = minecraft.level.getGameTime() + 200L; }
   }

   @Override
   public void setClose(CompoundTag data) {
      aType = 1;
      animClose();
   }

   private void addNewPage() {
      if (bookPages != null && bookPages.size() < 50) {
         bookPages.add(StringTag.valueOf(""));
         ++bookTotalPages;
      }
   }

   // New from Unofficial (BetaZavr)
   private void setNextTick(int time, boolean isNext) {
      tick = (int) (time / (CustomNpcs.IsFastAnimationGUI ? 3.0f : 1.0f));
      milliTick = tick - (isNext ? 1 : 0);
   }

   private void animClose() {
      if (step != 5) { return; }
      step = 6;
      setNextTick(5, false);
   }

   @Override
   public void textUpdate(IComponentGui component, String text) {}

   @Override
   public void setGuiData(CompoundTag compound) {
      if (compound.contains("TimeWhenReceived", Tag.TAG_LONG)) { mail.load(compound); }
      init();
   }

}
