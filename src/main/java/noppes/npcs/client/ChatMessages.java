package noppes.npcs.client;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.network.chat.Component;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.IChatMessages;

public class ChatMessages implements IChatMessages {

	private final int boxLength = 46;
	private String lastMessage = "";
	private long lastMessageTime = 0L;
	private Map<Long, TextBlockClient> messages = new TreeMap<>();

    @Override
	public void addMessage(String message, Entity entity) {
		if (!CustomNpcs.EnableChatBubbles) {
			return; }
		long time = System.currentTimeMillis();
		if (message.equals(lastMessage) && lastMessageTime + 5000L > time) { return; }
		Map<Long, TextBlockClient> newMessages = new TreeMap<>(messages);
		newMessages.put(time, new TextBlockClient(message, boxLength * 4, true, entity, Minecraft.getMinecraft().player, entity));
		if (newMessages.size() > 3) { newMessages.remove(newMessages.keySet().iterator().next()); }
		messages = newMessages;
		lastMessage = message;
		lastMessageTime = time;
	}

	private void drawRect(int left, int top, int right, int bottom, int color, double zLevel) {
		if (left < right) {
			int j1 = left;
			left = right;
			right = j1;
		}
		if (top < bottom) {
			int j1 = top;
			top = bottom;
			bottom = j1;
		}
		float f = (color >> 24 & 0xFF) / 255.0f;
		float f2 = (color >> 16 & 0xFF) / 255.0f;
		float f3 = (color >> 8 & 0xFF) / 255.0f;
		float f4 = (color & 0xFF) / 255.0f;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		GlStateManager.color(f2, f3, f4, f);
		buffer.begin(7, DefaultVertexFormats.POSITION);
		buffer.pos(left, bottom, zLevel).endVertex();
		buffer.pos(right, bottom, zLevel).endVertex();
		buffer.pos(right, top, zLevel).endVertex();
		buffer.pos(left, top, zLevel).endVertex();
		tessellator.draw();
	}

	private Map<Long, TextBlockClient> getMessages() {
		Map<Long, TextBlockClient> newMessages = new TreeMap<>();
		long time = System.currentTimeMillis();
		for (Map.Entry<Long, TextBlockClient> entry : messages.entrySet()) {
			if (time > entry.getKey() + 10000L) {
				continue;
			}
			newMessages.put(entry.getKey(), entry.getValue());
		}
		return messages = newMessages;
	}

	private void render(double x, double y, double z, float textscale, boolean depth, boolean isPlayer) {
		FontRenderer font = Minecraft.getMinecraft().fontRenderer;
		float var13 = 1.6f;
		float var14 = 0.016666668f * var13;
		int size = 0;
		List<Long> del = new ArrayList<>();
		for (Long time : messages.keySet()) {
			TextBlockClient block = messages.get(time);
			if (block.entity != null && !block.entity.isEntityAlive()) {
				del.add(time);
				if (lastMessage.equals(block.text)) {
					lastMessage = "";
					lastMessageTime = 0L;
				}
				continue;
			}
			size += block.lines.size();
		}
		for (Long key : del) { messages.remove(key); }
		if (size == 0) { return; }
		GlStateManager.pushMatrix();
		Minecraft mc = Minecraft.getMinecraft();
        float scale = 0.5f;
        int textYSize = (int) (size * font.FONT_HEIGHT * scale);
		GlStateManager.translate(x + 0.0f, y + textYSize * textscale * var14, z);
		GlStateManager.scale(textscale, textscale, textscale);
		GL11.glNormal3f(0.0f, 1.0f, 0.0f);
		GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
		GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0f, 0.0f, 0.0f);
		GlStateManager.scale(-var14, -var14, var14);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		GlStateManager.depthMask(true);
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		if (depth) { GlStateManager.enableDepth(); }
		else { GlStateManager.disableDepth(); }
		Color[] cs = isPlayer ? CustomNpcs.ChatPlayerColors : CustomNpcs.ChatNpcColors;
		int color = cs[0].getRGB() | (int) Math.ceil((depth ? 255.0f : 85.0f)) << 24;
		int border = cs[1].getRGB() | (int) Math.ceil((depth ? 255.0f : 85.0f)) << 24;
		int place = cs[2].getRGB() | (int) Math.ceil((depth ? 187.0f : 68.0f)) << 24;
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.disableTexture2D();
		GlStateManager.enableCull();
		int w = 0;
		for (TextBlockClient block2 : messages.values()) {
			for (Component chat : block2.lines) {
				int g = font.getStringWidth(chat.getFormattedText()) / 3;
				if (g > w) { w = g; }
			}
		}
		if (w > boxLength) { w = boxLength; }
		drawRect(-w - 2, -2, w + 2, textYSize + 1, place, 0.11);
		drawRect(-w - 1, -3, w + 1, -2, border, 0.1);
		drawRect(-w - 1, textYSize + 2, -1, textYSize + 1, border, 0.1);
		drawRect(3, textYSize + 2, w + 1, textYSize + 1, border, 0.1);
		drawRect(-w - 3, -1, -w - 2, textYSize, border, 0.1);
		drawRect(w + 3, -1, w + 2, textYSize, border, 0.1);
		drawRect(-w - 2, -2, -w - 1, -1, border, 0.1);
		drawRect(w + 2, -2, w + 1, -1, border, 0.1);
		drawRect(-w - 2, textYSize + 1, -w - 1, textYSize, border, 0.1);
		drawRect(w + 2, textYSize + 1, w + 1, textYSize, border, 0.1);
		drawRect(0, textYSize + 1, 3, textYSize + 4, place, 0.11);
		drawRect(-1, textYSize + 4, 1, textYSize + 5, place, 0.11);
		drawRect(-1, textYSize + 1, 0, textYSize + 4, border, 0.1);
		drawRect(3, textYSize + 1, 4, textYSize + 3, border, 0.1);
		drawRect(2, textYSize + 3, 3, textYSize + 4, border, 0.1);
		drawRect(1, textYSize + 4, 2, textYSize + 5, border, 0.1);
		drawRect(-2, textYSize + 4, -1, textYSize + 5, border, 0.1);
		drawRect(-2, textYSize + 5, 1, textYSize + 6, border, 0.1);
		GlStateManager.enableTexture2D();
		GlStateManager.depthMask(true);
		GlStateManager.scale(scale, scale, scale);
		int index = 0;
		for (TextBlockClient block2 : messages.values()) {
			for (Component chat : block2.lines) {
				String message = chat.getFormattedText();
				font.drawString(message, -font.getStringWidth(message) / 2, index * font.FONT_HEIGHT, color);
				++index;
			}
		}
		GlStateManager.disableCull();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.enableDepth();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		GlStateManager.popMatrix();
	}

	@Override
	public void renderMessages(double x, double y, double z, float textscale, boolean inRange, boolean isPlayer) {
		Map<Long, TextBlockClient> messages = getMessages();
		if (messages.isEmpty()) { return; }
		if (inRange) { render(x, y, z, textscale, false, isPlayer); }
		render(x, y, z, textscale, true, isPlayer);
	}

}
