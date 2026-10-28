package noppes.npcs.shared.client.gui.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.Util;

public class NoppesStringUtils {

   private static final int[] illegalChars = new int[] { 34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47 };
   private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]");

   static { Arrays.sort(illegalChars); }

   public static String cleanFileName(String badFileName) {
      StringBuilder cleanName = new StringBuilder();
      for(int i = 0; i < badFileName.length(); ++i) {
         int c = badFileName.charAt(i);
         if (Arrays.binarySearch(illegalChars, c) < 0) { cleanName.append((char)c); }
      }
      return cleanName.toString();
   }

   public static String formatText(Component text, Object... obs) { return formatText(text.getString(), obs); }

   public static String formatText(String text, Object... obs) {
      if (text != null && !text.isEmpty()) {
         text = Util.instance.getOldFormattedText(Component.translatable(text));
         for (Object ob : obs) {
            if (ob instanceof Player player) {
               String username = player.getDisplayName().getString();
               text = text.replace("{player}", username).replace("@p", username).replace("@dp", username);
            }
            else if (ob instanceof EntityNPCInterface npc) {
               text = text.replace("@npc", npc.getDisplayName().getString());
            }
         }
         return text.replace("&", "" + ((char) 167));
      }
      return "";
   }

   public static void setClipboardContents(String aString) { Minecraft.getInstance().keyboardHandler.setClipboard(aString); }

   public static String getClipboardContents() { return Minecraft.getInstance().keyboardHandler.getClipboard(); }

   public static String stripSpecialCharacters(String in) { return NON_ALPHANUMERIC.matcher(in).replaceAll(""); }

   public static String cleanResource(String s) {
      if (isCleanResource(s)) { return s; }
      return s.toLowerCase().replaceAll("[^a-z0-9_.\\-/:]", "");
   }

   private static boolean isCleanResource(String s) {
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-' || c == '/' || c == ':')) { return false; }
      }
      return true;
   }

   public static String[] splitLines(String s) { return s.split("\r\n|\r|\n"); }

   public static String newLine() { return System.lineSeparator(); }

   public static int parseInt(String input, int base) {
      try { return Integer.parseInt(input); }
      catch (NumberFormatException var3) { return base; }
   }

   public static boolean areEqual(String s1, String s2) {
      return Objects.equals(s1, s2) || s1 != null && s1.equalsIgnoreCase(s2);
   }

}
