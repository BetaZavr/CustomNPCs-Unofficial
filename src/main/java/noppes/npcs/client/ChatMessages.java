package noppes.npcs.client;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import java.awt.*;
import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.CullStateShard;
import net.minecraft.client.renderer.RenderStateShard.DepthTestStateShard;
import net.minecraft.client.renderer.RenderStateShard.LightmapStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.IChatMessages;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ChatMessages implements IChatMessages {

   protected static final Map<String, ChatMessages> users = new Hashtable<>();
   protected static final TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new TransparencyStateShard("translucent_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
   },
           () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   protected static final ShaderStateShard shader = new ShaderStateShard(GameRenderer::getPositionColorLightmapShader);
   protected static final RenderType type = RenderType.create("chatbubble", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, Mode.QUADS, 256, false, false, CompositeState.builder().setCullState(new CullStateShard(true)).setLightmapState(new LightmapStateShard(true)).setShaderState(shader).createCompositeState(true));
   protected static final RenderType typeDepth = RenderType.create("chatbubbledepth", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, Mode.QUADS, 256, false, true, CompositeState.builder().setCullState(new CullStateShard(true)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setShaderState(shader).setLightmapState(new LightmapStateShard(true)).setDepthTestState(new DepthTestStateShard("always", 519)).createCompositeState(false));
   protected Map<Long, TextBlockClient> messages = new TreeMap<>();
   protected final int boxLength = 46;
   protected String lastMessage = "";
   protected long lastMessageTime = 0L;

   @Override
   public void addMessage(String message, Entity entity) {
      if (!CustomNpcs.EnableChatBubbles) { return; }
      long time = System.currentTimeMillis();
      if (message.equals(lastMessage) && lastMessageTime + 5000L > time) { return; }
      Map<Long, TextBlockClient> newMessages = new TreeMap<>(messages);
      newMessages.put(time, new TextBlockClient(message, boxLength * 4, true, Minecraft.getInstance().player, entity));
      if (newMessages.size() > 3) { newMessages.remove(newMessages.keySet().iterator().next()); }
      messages = newMessages;
      lastMessage = message;
      lastMessageTime = time;
   }

   @Override
   public void renderMessages(PoseStack poseStack, MultiBufferSource typeBuffer, float textscale, boolean inRange, int lightMapUV, boolean isPlayer) {
      Map<Long, TextBlockClient> messages = getMessages();
      if (!messages.isEmpty()) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShader(GameRenderer::getPositionColorLightmapShader);
         if (inRange) { render(poseStack, typeBuffer, typeBuffer.getBuffer(typeDepth), textscale, false, lightMapUV, isPlayer); }
         render(poseStack, typeBuffer, typeBuffer.getBuffer(type), textscale, true, lightMapUV, isPlayer);
      }
   }

   public void render(PoseStack poseStack, MultiBufferSource typeBuffer, VertexConsumer iVertex, float textScale, boolean depth, int lightMapUV, boolean isPlayer) {
      Font font = Minecraft.getInstance().font;
      float var14 = 0.02666667F;
      int size = 0;
      TextBlockClient block;
      for(Iterator<TextBlockClient> var10 = messages.values().iterator(); var10.hasNext(); size += block.lines.size()) { block = var10.next(); }
      Minecraft mc = Minecraft.getInstance();
      Objects.requireNonNull(font);
      float scale = 0.5F;
      int textYSize = (int)((float)(size * 9) * scale);
      poseStack.pushPose();
      poseStack.translate(0.0F, (float)textYSize * var14, 0.0F);
      poseStack.scale(textScale, textScale, textScale);
      poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
      poseStack.scale(-var14, -var14, var14);

      Color[] cs = isPlayer ? CustomNpcs.ChatPlayerColors : CustomNpcs.ChatNpcColors;
      int color = cs[0].getRGB() | Mth.ceil((depth ? 255.0f : 85.0f)) << 24;
      int border = cs[1].getRGB() | Mth.ceil((depth ? 255.0f : 85.0f)) << 24;
      int place = cs[2].getRGB() | Mth.ceil((depth ? 187.0f : 68.0f)) << 24;

      Pose entry = poseStack.last();
      Matrix4f matrix = entry.pose();
      // calculate max text width
      int w = 0;
      for (TextBlockClient block2 : messages.values()) {
         for (Component chat : block2.lines) {
            int g = font.width(chat) / 3;
            if (g > w) { w = g; }
         }
      }
      if (w > boxLength) { w = boxLength; }
      // place
      drawRect(iVertex, matrix, lightMapUV, (float)(-w - 1), -2.0F, (float)(w + 1), (float)(textYSize + 1), place, 0.11F); // main center
      drawRect(iVertex, matrix, lightMapUV, (float)(-w - 2), -1.0F, (float)(-w - 1), (float)(textYSize), place, 0.11F); // main left
      drawRect(iVertex, matrix, lightMapUV, (float)(w + 1), -1.0F, (float)(w + 2), (float)(textYSize), place, 0.11F); // main right
      drawRect(iVertex, matrix, lightMapUV, 0.0F, (float)(textYSize + 1), 3.0F, (float)(textYSize + 4), place, 0.11F); // arrow up
      drawRect(iVertex, matrix, lightMapUV, -1.0F, (float)(textYSize + 4), 1.0F, (float)(textYSize + 5), place, 0.11F); // arrow down
      // border
      drawRect(iVertex, matrix, lightMapUV, (float)(-w - 1), -3.0F, (float)(w + 1), -2.0F, border, 0.1F); // main up
      drawRect(iVertex, matrix, lightMapUV, (float)(-w - 1), (float)(textYSize + 2), -1.0F, (float)(textYSize + 1), border, 0.1F); // main down left
      drawRect(iVertex, matrix, lightMapUV, 3.0F, (float)(textYSize + 2), (float)(w + 1), (float)(textYSize + 1), border, 0.1F); // main down right
      drawRect(iVertex, matrix, lightMapUV, (float)(-w - 3), -1.0F, (float)(-w - 2), (float)textYSize, border, 0.1F); // main left
      drawRect(iVertex, matrix, lightMapUV, (float)(w + 3), -1.0F, (float)(w + 2), (float)textYSize, border, 0.1F); // main right
      drawRect(iVertex, matrix, lightMapUV, (float)(-w - 2), -2.0F, (float)(-w - 1), -1.0F, border, 0.1F); // main up left
      drawRect(iVertex, matrix, lightMapUV, (float)(w + 2), -2.0F, (float)(w + 1), -1.0F, border, 0.1F); // main up right
      drawRect(iVertex, matrix, lightMapUV, (float)(-w - 2), (float)(textYSize + 1), (float)(-w - 1), (float)textYSize, border, 0.1F); // main down left
      drawRect(iVertex, matrix, lightMapUV, (float)(w + 2), (float)(textYSize + 1), (float)(w + 1), (float)textYSize, border, 0.1F); // main down right
      drawRect(iVertex, matrix, lightMapUV, -1.0F, (float)(textYSize + 1), 0.0F, (float)(textYSize + 4), border, 0.1F); // arrow up left
      drawRect(iVertex, matrix, lightMapUV, 3.0F, (float)(textYSize + 1), 4.0F, (float)(textYSize + 3), border, 0.1F); // arrow up right
      drawRect(iVertex, matrix, lightMapUV, 2.0F, (float)(textYSize + 3), 3.0F, (float)(textYSize + 4), border, 0.1F); // arrow center right 0
      drawRect(iVertex, matrix, lightMapUV, 1.0F, (float)(textYSize + 4), 2.0F, (float)(textYSize + 5), border, 0.1F); // arrow center right 1
      drawRect(iVertex, matrix, lightMapUV, -2.0F, (float)(textYSize + 4), -1.0F, (float)(textYSize + 5), border, 0.1F); // arrow down left
      drawRect(iVertex, matrix, lightMapUV, -2.0F, (float)(textYSize + 5), 1.0F, (float)(textYSize + 6), border, 0.1F); // arrow down
      // texts
      poseStack.scale(scale, scale, scale);
      float index = 0.0f;
      for (TextBlockClient block2 : messages.values()) {
         for (Component chat : block2.lines) {
            font.drawInBatch(chat, (float) -font.width(chat) / 2.0f, index * 9.0f, color, false, matrix, typeBuffer, !depth ? DisplayMode.SEE_THROUGH : DisplayMode.NORMAL, 0, lightMapUV);
            ++index;
         }
      }
      poseStack.popPose();
   }

   public void drawRect(VertexConsumer iVertex, Matrix4f matrix, int lightMapUV, float x, float y, float x2, float y2, int color, float z) {
      float j1;
      if (x < x2) {
         j1 = x;
         x = x2;
         x2 = j1;
      }
      if (y < y2) {
         j1 = y;
         y = y2;
         y2 = j1;
      }
      float f1 = (float)(color >> 16 & 255) / 255.0F;
      float f2 = (float)(color >> 8 & 255) / 255.0F;
      float f3 = (float)(color & 255) / 255.0F;
      draw(iVertex, matrix, lightMapUV, x, y, z, f1, f2, f3);
      draw(iVertex, matrix, lightMapUV, x, y2, z, f1, f2, f3);
      draw(iVertex, matrix, lightMapUV, x2, y2, z, f1, f2, f3);
      draw(iVertex, matrix, lightMapUV, x2, y, z, f1, f2, f3);
   }

   private void draw(VertexConsumer iVertex, Matrix4f matrix, int lightMapUV, float x, float y, float z, float red, float green, float blue) {
      Vector4f v = new Vector4f(x, y, z, 1.0F);
      v.mul(matrix);
      iVertex.vertex(v.x(), v.y(), v.z()).color(red, green, blue, 1.0F).uv2(lightMapUV).endVertex();
   }

   @SuppressWarnings("unused")
   public static ChatMessages getChatMessages(String username) {
      if (users.containsKey(username)) { return users.get(username); }
      else {
         ChatMessages chat = new ChatMessages();
         users.put(username, chat);
         return chat;
      }
   }

   private Map<Long, TextBlockClient> getMessages() {
      Map<Long, TextBlockClient> newMessages = new TreeMap<>();
      long time = System.currentTimeMillis();
      for (Map.Entry<Long, TextBlockClient> entry : new ArrayList<>(messages.entrySet())) {
         if (time <= entry.getKey() + Math.max(5000L, entry.getValue().size() * 500L)) { newMessages.put(entry.getKey(), entry.getValue()); }
      }
      return messages = newMessages;
   }

   public boolean hasMessage() { return !messages.isEmpty(); }

}
