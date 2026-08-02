package noppes.npcs.shared.client.gui.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.mixin.util.text.ITextFormattingMixin;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.LRUHashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.annotation.Nullable;

public class TrueTypeFont {

	private static final int TEXTURE_SIZE = 512;
	private static final List<Font> allFonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts());
	private static final Random random = new Random();

	private final List<Font> usedFonts = new ArrayList<>();
	private final LinkedHashMap<String, TrueTypeFont.GlyphCache> textCache = new LRUHashMap<>(2500); // text lines
	private final Map<Character, TrueTypeFont.Glyph> glyphCache = new HashMap<>();
	private final List<TrueTypeFont.TextureCache> textures = new ArrayList<>();
	private final Graphics2D globalG = (Graphics2D) (new BufferedImage(1, 1, 2)).getGraphics();
	private final ConcurrentLinkedQueue<TextureCache> pendingUploads = new ConcurrentLinkedQueue<>();
	public float scale = 1.0F;
	private char specialChar = (char) 167;

	private Font font;
	private int lineHeight = 1;
	private final float tabWidth;

	public TrueTypeFont(Font fontIn, float scaleIn) {
		font = fontIn;
		scale = scaleIn;
		globalG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		lineHeight = globalG.getFontMetrics(font).getHeight();
		tabWidth = getOrCreateGlyph('a').width * 3.0f;
	}

	public TrueTypeFont(ResourceLocation resource, float fontSize, float scaleIn) {
		try {
			InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(resource).getInputStream();
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			Font fontIn = Font.createFont(0, stream);
			ge.registerFont(fontIn);
			font = fontIn.deriveFont(Font.PLAIN, fontSize);
			scale = scaleIn;
			globalG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			lineHeight = globalG.getFontMetrics(font).getHeight();
			LogWriter.info("Loaded font \""+font.getFontName()+"\"");
		}
		catch (Exception e) { LogWriter.error("Error load font \"" + resource + "\"", e); }
		tabWidth = getOrCreateGlyph('a').width * 3.0f;
	}

	public void setSpecial(char c) { specialChar = c; }

	public int draw(String text, float x, float y, int color) {
		GlyphCache cache = getOrCreateCache(text);
		processPendingUploads();
		float a = (color >> 24 & 255) / 255.0f;
		float r = (color >> 16 & 255) / 255.0f;
		float g = (color >> 8 & 255) / 255.0f;
		float b = (color & 255) / 255.0f;
		if (a == 0) { a = 255; }

		GlStateManager.color(r, g, b, a);
		GlStateManager.enableBlend();

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, 0.0f);
		GlStateManager.scale(scale, scale, 1.0f);

		boolean bold = false;
		boolean italic = false;
		boolean underline = false;
		boolean strikethrough = false;
		boolean obfuscated = false;

		float currentX = 0.0F;
		float maxLineHeight = 0.0F;

		List<Glyph> glyphs = cache.glyphs;
		for (Glyph gl : glyphs) {
			switch (gl.type) {
				case RESET: {
					bold = italic = underline = strikethrough = obfuscated = false;
					GlStateManager.color(r, g, b, a);
					break;
				}
				case COLOR: {
					GlStateManager.color((gl.color >> 16 & 255) / 255.0f,
							(gl.color >> 8 & 255) / 255.0f,
							(gl.color & 255) / 255.0f, a);
					break;
				}
				case BOLD: bold = true; break;
				case ITALIC: italic = true; break;
				case UNDERLINE: underline = true; break;
				case STRIKETHROUGH: strikethrough = true; break;
				case RANDOM: obfuscated = true; break;
				case NORMAL: {
					float glWidth = (float) gl.width * textureScale();
					float glHeight = (float) gl.height * textureScale();
					maxLineHeight = Math.max(maxLineHeight, glHeight);
					// Obfuscated: Replace with a random character of the same width
					Glyph renderGlyph = obfuscated ? getObfuscatedGlyph(gl.originalChar, gl) : gl;
					// ITALIC: Move the top of the character to the right
					float italicOffset = italic ? glHeight * -0.3f : 0f;
					GlStateManager.pushMatrix();
					// Apply italic transformation to ONE character
					if (italic) {
						Matrix4f matrix = new Matrix4f();
						matrix.setIdentity();
						matrix.m10 = matrix.m11 * -0.3f;

						FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
						matrix.store(buffer);
						buffer.flip();

						GlStateManager.multMatrix(buffer);
					}
					if (gl.originalChar != (char) 9) {
						fillGradient(gl.texture, currentX + (italic ? -italicOffset : 0), 0.0F,
								(float) renderGlyph.x * textureScale(),
								(float) renderGlyph.y * textureScale(),
								glWidth, glHeight);
					}

					GlStateManager.popMatrix();
					// BOLD: draw a second time with an offset of 0.5pxl
					if (bold) {
						GlStateManager.pushMatrix();
						if (italic) {
							Matrix4f matrix = new Matrix4f();
							matrix.setIdentity();
							matrix.m10 = matrix.m11 * -0.3f;

							FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
							matrix.store(buffer);
							buffer.flip();

							GlStateManager.multMatrix(buffer);
						}
						fillGradient(gl.texture, currentX + 0.5F + (italic ? -italicOffset : 0), 0.0F,
								(float) renderGlyph.x * textureScale(),
								(float) renderGlyph.y * textureScale(),
								glWidth, glHeight);
						GlStateManager.popMatrix();
						currentX += 0.5F;
					}
					float f0 = 0.5f;
					if (strikethrough) {
						float lineY = glHeight * 0.5f;
						fillColor(currentX, lineY, glWidth, f0);
					}
					if (underline) {
						float lineY = glHeight - 1.0f;
						fillColor(currentX, lineY, glWidth, f0);
					}
					currentX += glWidth;
					break;
				}
			}
		}
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

		return (int) (x + currentX * scale);
	}

	public void fillGradient(int texture, float x, float y, float textureX, float textureY, float width, float height) {
		float f = 0.00390625F;
		float zLevel = 0.0f;

		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.bindTexture(texture);

		BufferBuilder builder = Tessellator.getInstance().getBuffer();
		builder.begin(7, DefaultVertexFormats.POSITION_TEX);
		builder.noColor();
		builder.pos(x, (y + height), zLevel)
				.tex((textureX * f), ((textureY + height) * f))
				.endVertex();
		builder.pos((x + width), (y + height), zLevel)
				.tex(((textureX + width) * f), ((textureY + height) * f))
				.endVertex();
		builder.pos((x + width), y, zLevel)
				.tex(((textureX + width) * f), (textureY * f))
				.endVertex();
		builder.pos(x, y, zLevel)
				.tex((textureX * f), (textureY * f))
				.endVertex();
		Tessellator.getInstance().draw();
		GlStateManager.disableBlend();
	}

	private void fillColor(float x, float y, float w, float h) {
		float zLevel = 0.0f;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
		bufferbuilder.pos(x, y + h, zLevel).endVertex();
		bufferbuilder.pos(x + w, y + h, zLevel).endVertex();
		bufferbuilder.pos(x + w, y, zLevel).endVertex();
		bufferbuilder.pos(x, y, zLevel).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	private Glyph getObfuscatedGlyph(char originalChar, Glyph original) {
		char[] randomChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
		char randomChar = randomChars[random.nextInt(randomChars.length)];
		while (randomChar == originalChar) { randomChar = randomChars[random.nextInt(randomChars.length)]; }
		GlyphCache cache = getOrCreateCache(String.valueOf(randomChar));
		if (!cache.glyphs.isEmpty()) { return cache.glyphs.get(0); }
		return original;
	}
	
	private GlyphCache getOrCreateCache(String text) {
		GlyphCache cache = textCache.get(text);
		if (cache == null) {
			cache = new GlyphCache();
			float currentLineWidth = 0;
			for (int i = 0; i < text.length(); ++i) {
				char c = text.charAt(i);
				if (c == specialChar && i + 1 < text.length()) {
					char next = text.toLowerCase(Locale.ENGLISH).charAt(i + 1);
					int index = "0123456789abcdefklmnor".indexOf(next);
					if (index >= 0) {
						Glyph g = new Glyph(specialChar);
						if (index < 16) {
							g.type = GlyphType.COLOR;
							TextFormatting tf = getByCode(next);
							if (tf != null) {
								switch (tf) {
									case BLACK: g.color = new Color(0x000000).getRGB(); break;
									case DARK_BLUE: g.color = new Color(0x0000AA).getRGB(); break;
									case DARK_GREEN: g.color = new Color(0x00AA00).getRGB(); break;
									case DARK_AQUA: g.color = new Color(0x00AAAA).getRGB(); break;
									case DARK_RED: g.color = new Color(0xAA0000).getRGB(); break;
									case DARK_PURPLE: g.color = new Color(0xAA00AA).getRGB(); break;
									case GOLD: g.color = new Color(0xFFAA00).getRGB(); break;
									case GRAY: g.color = new Color(0xAAAAAA).getRGB(); break;
									case DARK_GRAY: g.color = new Color(0x555555).getRGB(); break;
									case BLUE: g.color = new Color(0x5555FF).getRGB(); break;
									case GREEN: g.color = new Color(0x55FF55).getRGB(); break;
									case AQUA: g.color = new Color(0x55FFFF).getRGB(); break;
									case RED: g.color = new Color(0xFF5555).getRGB(); break;
									case LIGHT_PURPLE: g.color = new Color(0xFF55FF).getRGB(); break;
									case YELLOW: g.color = new Color(0xFFFF55).getRGB(); break;
									case WHITE: g.color = new Color(0xFFFFFF).getRGB(); break;
								}
							}
						}
						else if (index == 16) {
							g.type = GlyphType.RANDOM;
						} else if (index == 17) {
							g.type = GlyphType.BOLD;
						} else if (index == 18) {
							g.type = GlyphType.STRIKETHROUGH;
						} else if (index == 19) {
							g.type = GlyphType.UNDERLINE;
						} else if (index == 20) {
							g.type = GlyphType.ITALIC;
						} else {
							g.type = GlyphType.RESET;
						}
						cache.glyphs.add(g);
						++i;
						continue;
					} // has color code
				}
				if (c == '\t') {
					float tabPosition = (float) Math.floor((currentLineWidth + 1) / tabWidth) * tabWidth + tabWidth;
					float tabSize = tabPosition - currentLineWidth;

					Glyph g = new Glyph('\t');
					g.width = (int) Math.max(tabSize, tabWidth / 4);
					g.height = lineHeight;

					cache.glyphs.add(g);
					currentLineWidth += g.width;
					cache.width = (int) Math.max(cache.width, currentLineWidth);
					continue;
				} // tab
				Glyph g = getOrCreateGlyph(c);
				cache.glyphs.add(g);
				currentLineWidth += g.width;
				cache.width = (int) Math.max(cache.width, currentLineWidth);
				cache.height = Math.max(cache.height, g.height);
			}
			textCache.put(text, cache);
		}
		return cache;
	}

	public static @Nullable TextFormatting getByCode(char c) {
		char c0 = Character.toString(c).toLowerCase(Locale.ROOT).charAt(0);
		for(TextFormatting textFormatting : TextFormatting.values()) {
			if (((ITextFormattingMixin) (Object) textFormatting).getFormattingCode() == c0) { return textFormatting; }
		}
		return null;
	}

	public void processPendingUploads() {
		TextureCache cache;
		while ((cache = pendingUploads.poll()) != null) { uploadTexture(cache); }
	}

	private Glyph getOrCreateGlyph(char c) {
		Glyph g = glyphCache.get(c);
		if (g == null) {
			TextureCache cache = getCurrentTexture();
			Font font = getFontForChar(c);
			FontMetrics metrics = globalG.getFontMetrics(font);
			g = new Glyph(c);
			g.width = Math.max(metrics.charWidth(c), 1);
			g.height = Math.max(metrics.getHeight(), 1);
			if (cache.x + g.width >= TEXTURE_SIZE) {
				cache.x = 0;
				cache.y += lineHeight + 1;
				if (cache.y >= TEXTURE_SIZE) {
					cache.full = true;
					cache = getCurrentTexture();
				}
			}
			g.x = cache.x;
			g.y = cache.y;
			cache.x += g.width + 3;
			lineHeight = Math.max(lineHeight, g.height);
			cache.g.setFont(font);
			cache.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			cache.g.drawString("" + c, g.x, g.y + metrics.getAscent());
			g.texture = cache.textureId;
			cache.uploaded = false;
			if (!pendingUploads.contains(cache)) { pendingUploads.offer(cache); }
			glyphCache.put(c, g);
		}
		return g;
	}

	private void uploadTexture(TextureCache cache) {
		pendingUploads.remove(cache);
		if (!cache.uploaded) {
			TextureUtil.uploadTextureImage(cache.textureId, cache.bufferedImage);
			GlStateManager.bindTexture(cache.textureId);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			cache.uploaded = true;
		}
	}

	private TextureCache getCurrentTexture() {
		TrueTypeFont.TextureCache cache = null;
		for (TextureCache t : textures) {
			if (!t.full) {
				cache = t;
				break;
			}
		}
		if (cache == null) { textures.add(cache = new TextureCache()); }
		return cache;
	}

	private Font getFontForChar(char c) {
		if (!font.canDisplay(c)) {
			for (Font fontU : new ArrayList<>(usedFonts)) {
				if (fontU.canDisplay(c)) { return fontU; }
			}
			Font fontAUMS = new Font("Arial Unicode MS", Font.PLAIN, font.getSize());
			if (fontAUMS.canDisplay(c)) { return fontAUMS; }
			for (Font fontInAll : new ArrayList<>(TrueTypeFont.allFonts)) {
				if (fontInAll.canDisplay(c)) {
					usedFonts.add(fontInAll = fontInAll.deriveFont(Font.PLAIN, font.getSize()));
					return fontInAll;
				}
			}
		}
		return font;
	}

	public int width(String text) {
		GlyphCache cache = getOrCreateCache(text);
		return (int) ((float) cache.width * scale * textureScale());
	}

	public synchronized int height(String text) {
		if (text != null && !text.trim().isEmpty()) {
			TrueTypeFont.GlyphCache cache = getOrCreateCache(text);
			return Math.max(1, (int) ((float) cache.height * scale * textureScale()));
		}
		return (int) ((float) lineHeight * scale * textureScale());
	}

	private float textureScale() { return 0.5f; }

	public void dispose() {
		pendingUploads.clear();
		for (TextureCache cache : textures) { GlStateManager.deleteTexture(cache.textureId); }
		textCache.clear();
	}

	public String getFontName() { return font.getFontName(); }

	public boolean hasFont() { return font != null; }

	public String trimStringToWidth(String str, int maxWidth) {
		if (str == null || str.isEmpty()) { return ""; }
		float scaledMaxWidth = maxWidth / (scale * textureScale());
		int currentWidth = 0;
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == specialChar && i + 1 < str.length()) {
				char next = str.charAt(i + 1);
				if ("0123456789abcdefklmnor".indexOf(Character.toLowerCase(next)) >= 0) {
					result.append(c).append(next);
					i++;
					continue;
				}
			}
			Glyph glyph = getOrCreateGlyph(c);
			int charWidth = glyph.width;
			if (currentWidth + charWidth > scaledMaxWidth) { break; }
			currentWidth += charWidth;
			result.append(c);
		}
		return result.toString();
	}

	public String trimStringToWidth(String str, int maxWidth, boolean withEllipsis) {
		String result = trimStringToWidth(str, maxWidth);
		if (withEllipsis && result.length() < str.length()) {
			int ellipsisWidth = width("...");
			String withoutEllipsis = trimStringToWidth(str, maxWidth - ellipsisWidth);
			return withoutEllipsis + "...";
		}
		return result;
	}

	static class Glyph {
		TrueTypeFont.GlyphType type;
		int color;
		int x;
		int y;
		int height;
		int width;
		int texture;
		char originalChar;

		Glyph(char originalCharIn) {
			type = TrueTypeFont.GlyphType.NORMAL;
			originalChar = originalCharIn;
			color = -1;
		}
	}

	static class GlyphCache {
		public int width;
		public int height;
		List<TrueTypeFont.Glyph> glyphs = new ArrayList<>();
	}

	enum GlyphType {
		NORMAL, COLOR, RANDOM, BOLD, STRIKETHROUGH, UNDERLINE, ITALIC, RESET, OTHER
	}

	static class TextureCache {

		boolean uploaded = false;
		int x;
		int y;
		int textureId = GL11.glGenTextures();
		BufferedImage bufferedImage = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g;
		boolean full;

		TextureCache() {
			g = (Graphics2D) bufferedImage.getGraphics();
			// Clearing the entire texture to transparent black
			g.setComposite(AlphaComposite.Clear);
			g.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
			g.setComposite(AlphaComposite.SrcOver);
		}
	}

}
