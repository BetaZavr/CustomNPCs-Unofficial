package noppes.npcs.shared.client.gui.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

public class NoppesStringUtils {

	private static final int[] illegalChars = new int[] { 34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47 };
	static { Arrays.sort(illegalChars); }

	public static String cleanFileName(String badFileName) {
		StringBuilder cleanName = new StringBuilder();
		for (int i = 0; i < badFileName.length(); ++i) {
			int c = badFileName.charAt(i);
			if (Arrays.binarySearch(NoppesStringUtils.illegalChars, c) < 0) {
				cleanName.append((char) c);
			}
		}
		return cleanName.toString();
	}

	public static String formatText(String text, Object... obs) {
		if (text == null || text.isEmpty()) { return ""; }
		text = translate(text);
		for (Object ob : obs) {
			if (ob instanceof EntityPlayer) {
				String username = ((EntityPlayer) ob).getDisplayNameString();
				text = text.replace("{player}", username);
				text = text.replace("@p", username);
			} else if (ob instanceof EntityNPCInterface) {
				text = text.replace("@npc", ((EntityNPCInterface) ob).getName());
			}
		}
		text = text.replace("&", ((char) 167) + "");
		return text;
	}

	public static String getClipboardContents() {
		String result = "";
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clipboard.getContents(null);
		boolean hasTransferableText = contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		if (hasTransferableText) {
			try {
				result = (String) contents.getTransferData(DataFlavor.stringFlavor);
			} catch (Exception ex) {
				LogWriter.except(ex);
			}
		}
		return removeHidden(result);
	}

	public static int parseInt(String s, int i) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException ex) {
			return i;
		}
	}

	public static String removeHidden(String text) {
		StringBuilder newString = new StringBuilder(text.length());
		int offset = 0;
		while (offset < text.length()) {
			int codePoint = text.codePointAt(offset);
			offset += Character.charCount(codePoint);
			switch (Character.getType(codePoint)) {
				case 0:
				case 16:
				case 18:
				case 19: {
					continue;
				}
				default: {
					newString.append(Character.toChars(codePoint));
				}
			}
		}
		return newString.toString();
	}

	public static void setClipboardContents(String aString) {
		StringSelection stringSelection = new StringSelection(aString);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, (arg0, arg1) -> {
		});
	}

	public static String translate(Object... arr) {
		StringBuilder s = new StringBuilder();
		for (Object str : arr) {
			try {
				if (str instanceof ITextComponent) { s.append(((ITextComponent) str).getFormattedText()); }
				else { s.append(new TextComponentTranslation(str.toString()).getFormattedText()); }
			} catch (Exception e) { s.append(str); }
		}
		return s.toString();
	}

	@SuppressWarnings("unused")
	public static String cleanResource(String s) {
		if (CustomNpcs.FastNpcLayers && isCleanResource(s)) { return s; }
		return s.toLowerCase().replaceAll("[^a-z0-9_.\\-/: ()]", "");
	}

	private static boolean isCleanResource(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-' || c == '/' || c == ':' || c == ' ' || c == '(' || c == ')')) { return false; }
		}
		return true;
	}

}
