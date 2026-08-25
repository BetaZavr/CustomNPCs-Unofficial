package noppes.npcs.client;

import com.google.gson.Gson;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.IPos;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.item.IItemBoundary;
import noppes.npcs.api.item.INPCToolItem;
import noppes.npcs.api.item.ISpecBuilder;
import noppes.npcs.api.util.IRayTraceRotate;
import noppes.npcs.api.util.IRayTraceVec;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.client.controllers.YDEController;
import noppes.npcs.client.gui.GuiNbtBook;
import noppes.npcs.client.gui.GuiNpcPather;
import noppes.npcs.client.gui.player.GuiLog;
import noppes.npcs.client.gui.player.GuiMailbox;
import noppes.npcs.client.gui.player.GuiOpenCase;
import noppes.npcs.client.gui.player.tabs.InventoryTabFactions;
import noppes.npcs.client.gui.player.tabs.InventoryTabQuests;
import noppes.npcs.client.gui.player.tabs.InventoryTabVanilla;
import noppes.npcs.client.gui.util.GuiTooltipUtils;
import noppes.npcs.client.gui.util.quests.QuestObjective;
import noppes.npcs.client.gui.yellow_de.GuiYellowDialogEditor;
import noppes.npcs.client.gui.yellow_de.data.UtilYDE;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.client.renderer.MarkRenderer;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.client.util.CrashesData;
import noppes.npcs.client.util.CustomNpcsLangPack;
import noppes.npcs.client.util.MusicData;
import noppes.npcs.constants.*;
import noppes.npcs.controllers.*;
import noppes.npcs.controllers.data.*;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemBoundary;
import noppes.npcs.items.ItemBuilder;
import noppes.npcs.items.ItemNbtBook;
import noppes.npcs.items.ItemNpcMovingPath;
import noppes.npcs.mixin.client.ICameraMixin;
import noppes.npcs.mixin.client.gui.screens.inventory.IAbstractContainerScreenMixin;
import noppes.npcs.mixin.client.renderer.ICubeMapMixin;
import noppes.npcs.mixin.world.level.pathfinder.IPathMixin;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.*;
import noppes.npcs.schematics.Schematic;
import noppes.npcs.schematics.SchematicWrapper;
import noppes.npcs.shared.client.gui.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.BuilderData;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

public class ClientEventHandler {

    public static final Map<Player, ChatMessages> chatMessages = new HashMap<>();

    // New from Unofficial (BetaZavr)
    private static final ResourceLocation[] BORDER = new ResourceLocation[16];
    private static final ResourceLocation[] COMPASS_ICONS = new ResourceLocation[32];
    private static final ResourceLocation CREATIVE_TABS = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    private static final ResourceLocation BOOK = new ResourceLocation("textures/item/written_book.png");

    // Quest compass
    public static final ResourceLocation RESOURCE_COMPASS = new ResourceLocation(CustomNpcs.MODID + ":models/util/compass.obj");
    public static final Map<Integer, ParameterizedModel> COMPASS_FASE = new HashMap<>();
    public static ParameterizedModel COMPASS_BODY;
    public static ParameterizedModel COMPASS_DIAL;
    public static ParameterizedModel COMPASS_ARROW_0;
    public static ParameterizedModel COMPASS_ARROW_1;
    public static ParameterizedModel COMPASS_ARROW_20;
    public static ParameterizedModel COMPASS_ARROW_21;
    public static ParameterizedModel COMPASS_ARROW_22;
    public static ParameterizedModel COMPASS_ARROW_3;
    private static int qt = 0;
    // Items:
    public static final List<Vec3> movingPath = new ArrayList<>();
    // Schematics:
    private static final List<TileBuilder> schemes = new ArrayList<>();
    // camera
    public static final CrashesData crashes = new CrashesData();
    // mails
    public static boolean hasNewMail = false;
    public static long showNewMail = 0L;
    public static long startMail = 0L;
    // in world
    public static long secs;
    // main
    private static Minecraft mc;
    // any
    private boolean miniMapLoaded;

    static {
        for (int i = 0; i < 16; i++) {
            BORDER[i] = new ResourceLocation(CustomNpcs.MODID, "textures/util/border/" + (i < 10 ? "0" + i : i) + ".png");
        }
        for (int i =0; i < 32; i++) {
            String id = String.valueOf(i);
            if (id.length() < 2) { id = "0" + id; }
            COMPASS_ICONS[i] = new ResourceLocation("textures/item/compass_" + id + ".png");
        }
    }

    public static void addShowThis(TileBuilder tile) {
        if (Util.instance.getSide() == Dist.DEDICATED_SERVER || schemes.contains(tile)) { return; }
        schemes.add(tile);
    }

    public static void clearSchemes() { schemes.clear(); }

    public static void renderSchem(PoseStack matrixStack, MultiBufferSource.BufferSource bufferSource, float partialTick, SchematicWrapper schem, int rotation, double x, double y, double z) {
        mc = Minecraft.getInstance();
        if (mc.level == null || schem == null) { return; }
        matrixStack.pushPose();
        matrixStack.translate(x, y, z);
        // Bound
        if (rotation % 2 == 0) { renderSelectionBox(matrixStack, bufferSource, new BlockPos(schem.schema.getWidth(), schem.schema.getHeight(), schem.schema.getLength())); }
        else { renderSelectionBox(matrixStack, bufferSource, new BlockPos(schem.schema.getLength(), schem.schema.getHeight(), schem.schema.getWidth())); }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        try {
            for(int i = 0, j = 0;
                i < schem.size && (PlayerData.get(mc.player).overlay.isPressedCtrl() || j < 25000);
                ++i) {
                BlockState state = schem.schema.getBlockState(i);
                if (state.getRenderShape() != RenderShape.INVISIBLE) {
                    int posX = i % schem.schema.getWidth();
                    int posZ = (i - posX) / schem.schema.getWidth() % schem.schema.getLength();
                    int posY = ((i - posX) / schem.schema.getWidth() - posZ) / schem.schema.getLength();
                    BlockPos pos = schem.rotatePos(posX, posY, posZ, rotation);
                    matrixStack.pushPose();
                    matrixStack.translate((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
                    state = SchematicWrapper.rotationState(state, rotation);
                    try { renderBlock(mc.level, state, pos, matrixStack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, partialTick); }
                    catch (Exception e) { LogWriter.error(e); }
                    matrixStack.popPose();
                    j++;
                }
            }
        }
        catch (Exception e) { LogWriter.error("Error preview builder block", e); }
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrixStack.popPose();
    }

    public static void renderSphere(Matrix4f matrix, VertexConsumer consumer,
                                    float x, float y, float z,
                                    float radius, int verticalRings, int horizontalRings,
                                    float r, float g, float b) {
        for (int i = 0; i <= verticalRings; i++) {
            float phi = (float) (i * Math.PI / verticalRings);
            float sinPhi = Mth.sin(phi);
            float cosPhi = Mth.cos(phi);

            for (int j = 0; j <= horizontalRings; j++) {
                float theta = (float) (j * 2 * Math.PI / horizontalRings);
                float sinTheta = Mth.sin(theta);
                float cosTheta = Mth.cos(theta);

                float x1 = x + radius * sinPhi * cosTheta;
                float y1 = y + radius * cosPhi;
                float z1 = z + radius * sinPhi * sinTheta;

                float nextI = i < verticalRings ? i + 1 : i;
                float nextPhi = (float) (nextI * Math.PI / verticalRings);
                float sinNextPhi = Mth.sin(nextPhi);
                float cosNextPhi = Mth.cos(nextPhi);

                float x2 = x + radius * sinNextPhi * cosTheta;
                float y2 = y + radius * cosNextPhi;
                float z2 = z + radius * sinNextPhi * sinTheta;

                float nx = (x1 + x2) / 2 - x;
                float ny = (y1 + y2) / 2 - y;
                float nz = (z1 + z2) / 2 - z;

                consumer.vertex(matrix, x1, y1, z1)
                        .color(r, g, b, 1.0f)
                        .uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                        .normal(nx, ny, nz)
                        .endVertex();
                consumer.vertex(matrix, x2, y2, z2)
                        .color(r, g, b, 1.0f)
                        .uv(0, 0)
                        .uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                        .normal(nx, ny, nz)
                        .endVertex();
            }
        }
        for (int i = 0; i < verticalRings; i++) {
            float phi = (float) (i * Math.PI / verticalRings);
            float sinPhi = Mth.sin(phi);
            float cosPhi = Mth.cos(phi);

            for (int j = 0; j < horizontalRings; j++) {
                float theta = (float) (j * 2 * Math.PI / horizontalRings);
                float sinTheta = Mth.sin(theta);
                float cosTheta = Mth.cos(theta);

                float x1 = x + radius * sinPhi * cosTheta;
                float y1 = y + radius * cosPhi;
                float z1 = z + radius * sinPhi * sinTheta;

                float nextJ = j + 1;
                float nextTheta = (float) (nextJ * 2 * Math.PI / horizontalRings);
                float sinNextTheta = Mth.sin(nextTheta);
                float cosNextTheta = Mth.cos(nextTheta);

                float x2 = x + radius * sinPhi * cosNextTheta;
                float y2 = y + radius * cosPhi;
                float z2 = z + radius * sinPhi * sinNextTheta;

                float nx = (x1 + x2) / 2 - x;
                float ny = (y1 + y2) / 2 - y;
                float nz = (z1 + z2) / 2 - z;

                consumer.vertex(matrix, x1, y1, z1)
                        .color(r, g, b, 1.0f)
                        .uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                        .normal(nx, ny, nz)
                        .endVertex();
                consumer.vertex(matrix, x2, y2, z2)
                        .color(r, g, b, 1.0f)
                        .uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                        .normal(nx, ny, nz)
                        .endVertex();
            }
        }
    }

    public static void renderBlock(Level level, BlockState state, BlockPos pos, PoseStack matrixStack,
                                   MultiBufferSource buffer, int light, int overlay, float partialTick) {
        if (level == null) { return; }
        renderBlockPart(level, state, pos, matrixStack, buffer, light, overlay, partialTick);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos otherPos = (half == DoubleBlockHalf.LOWER) ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (!otherState.is(state.getBlock()) || !otherState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                otherState = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                        half == DoubleBlockHalf.LOWER ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
            }
            Vec3 offset = Vec3.atLowerCornerOf(otherPos.subtract(pos));
            matrixStack.pushPose();
            matrixStack.translate(offset.x, offset.y, offset.z);
            renderBlockPart(level, otherState, otherPos, matrixStack, buffer, light, overlay, partialTick);
            matrixStack.popPose();
        }
        else if (state.getBlock() instanceof BedBlock && state.hasProperty(BedBlock.PART)) {
            BedPart part = state.getValue(BedBlock.PART);
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos otherPos = (part == BedPart.FOOT) ? pos.relative(facing) : pos.relative(facing.getOpposite());
            BlockState otherState = level.getBlockState(otherPos);
            if (!otherState.is(state.getBlock()) || !otherState.hasProperty(BedBlock.PART)) {
                otherState = state.setValue(BedBlock.PART,
                        part == BedPart.FOOT ? BedPart.HEAD : BedPart.FOOT);
            }
            Vec3 offset = Vec3.atLowerCornerOf(otherPos.subtract(pos));
            matrixStack.pushPose();
            matrixStack.translate(offset.x, offset.y, offset.z);
            renderBlockPart(level, otherState, otherPos, matrixStack, buffer, light, overlay, partialTick);
            matrixStack.popPose();
        }
    }

    private static void renderBlockPart(@Nonnull Level level, BlockState state, BlockPos pos, PoseStack matrixStack,
                                        MultiBufferSource buffer, int light, int overlay, float partialTick) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockEntityRenderDispatcher entityDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        BakedModel bakedModel = dispatcher.getBlockModel(state);
        // liquid
        if (!state.getFluidState().isEmpty()) {
            dispatcher.renderLiquid(BlockPos.ZERO,
                    new FakeBlockAndTintGetter(state, pos, level),
                    buffer.getBuffer(RenderType.translucent()),
                    state, state.getFluidState());
            return;
        }
        // any
        // Use deterministic seed for consistent rendering
        for (RenderType rType : bakedModel.getRenderTypes(state, RandomSource.create(42L), ModelData.EMPTY)) {
            @Nonnull RenderType renderType = RenderTypeHelper.getEntityRenderType(rType, false);
            // Get the proper block tint color instead of biome grass color
            int blockColor = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
            float red   = ((blockColor >> 16) & 0xFF) / 255.0F;
            float green = ((blockColor >> 8)  & 0xFF) / 255.0F;
            float blue  = (blockColor         & 0xFF) / 255.0F;
            // Use the correct render type buffer, not hardcoded translucent
            VertexConsumer consumer = buffer.getBuffer(renderType);
            dispatcher.getModelRenderer().renderModel(matrixStack.last(), consumer, state, bakedModel,
                    red, green, blue, light, overlay, ModelData.EMPTY, renderType);
        }
        // has spec. render
        if (state.getBlock() instanceof EntityBlock) {
            BlockEntity tile = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
            if (tile != null) {
                BlockEntityRenderer<BlockEntity> renderer = entityDispatcher.getRenderer(tile);
                if (renderer != null) {
                    tile.setLevel(level);
                    renderer.render(tile, partialTick, matrixStack, buffer, light, overlay);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
                }
            }
        }
    }

    private static EntityNPCInterface getClosestNPC(double[] p, List<EntityNPCInterface> ents, QuestData qData) {
        double d = 65535.0d;
        Vec3 v = new Vec3(p[0], p[1], p[2]);
        EntityNPCInterface npc = null;
        for (EntityNPCInterface el : ents) {
            if (el.getName().getString().equals(qData.quest.getCompleterNpc().getName())) {
                double r = v.distanceToSqr(el.getX(), el.getY(), el.getZ());
                if (npc != null && r >= d) { continue; }
                d = r;
                npc = el;
            }
        }
        return npc;
    }

    private static void renderSelectionBox(PoseStack matrixStack, MultiBufferSource.BufferSource bufferSource, BlockPos pos) {
        LevelRenderer.renderLineBox(matrixStack, bufferSource.getBuffer(RenderType.lines()),
                new AABB(BlockPos.ZERO, pos),
                1.0f, 0.0f, 0.0f, 1.0f);
    }

    private static void renderEntityForBook(@Nonnull GuiGraphics graphics, @Nonnull LivingEntity entity) {
        EntityNPCInterface npc = null;
        int visible = 0;
        int showName = 0;
        int orientation = 0;
        if (entity instanceof EntityNPCInterface cnpc) {
            npc = cnpc;
            visible = npc.display.getVisible();
            showName = npc.display.getShowName();
            orientation = npc.ais.orientation;
            npc.display.setVisible(2);
            npc.display.setShowName(0);
            npc.ais.orientation = 0;
        }
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        float scW = entity.getBbWidth() > 1.0f ? 1.0f / entity.getBbWidth() : 1.0f;
        float scH = entity.getBbHeight() > 2.4f ? 2.4f / entity.getBbHeight() : 1.0f;
        float scale = Math.min(scW, scH);
        matrixStack.scale(-30.0f * scale * 0.75f, -30.0f * scale * 0.75f, -30.0f * scale * 0.75f);
        float f2 = entity.getYRot();
        float f3 = entity.getXRot();
        float f4 = entity.yHeadRot;
        float f5 = entity.yBodyRot;
        float f6 = entity.yBodyRotO;
        matrixStack.mulPose(Axis.XP.rotationDegrees(-20.0f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(210.0f));
        entity.setYRot(0.0f);
        entity.setXRot(0.0f);
        entity.yHeadRot = 0.0f;
        entity.yBodyRot = 0.0f;
        entity.yBodyRotO = 0.0f;
        EntityRenderDispatcher renderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        renderDispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, matrixStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT);
        entity.setYRot(f2);
        entity.setXRot(f3);
        entity.yHeadRot = f4;
        entity.yBodyRot = f5;
        entity.yBodyRotO = f6;
        if (npc != null) {
            npc.display.setVisible(visible);
            npc.display.setShowName(showName);
            npc.ais.orientation = orientation;
        }
        matrixStack.popPose();
    }

    // DebugRenderer.renderFilledBox(PoseStack, MultiBufferSource, aabb, red, green, blue, alpha);
    public void renderFilledBox(VertexConsumer consumer, Matrix4f matrix4f, AABB aabb,
                                 boolean addFromInside, float red, float green, float blue, float alpha) {
        float minX = (float) aabb.minX;
        float minY = (float) aabb.minY;
        float minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX;
        float maxY = (float) aabb.maxY;
        float maxZ = (float) aabb.maxZ;

        consumer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();

        if (addFromInside) {
            consumer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            consumer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void entityClientEvent(PlayerInteractEvent.EntityInteract event) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeBlockPos(BlockPos.ZERO);
        CustomNpcs.proxy.openGui(null, EnumGuiType.NbtBook, buffer);
        CustomNPCsScheduler.runTack(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof GuiNbtBook nbtBook) {
                nbtBook.entityId = event.getTarget().getId();
                nbtBook.entity = event.getTarget();
                nbtBook.originalCompound = new CompoundTag();
                event.getTarget().save(nbtBook.originalCompound);
                nbtBook.compound = nbtBook.originalCompound;
                nbtBook.init();
            }
        }, 250);
    }

    @SubscribeEvent
    public <T extends LivingEntity, M extends EntityModel<T>> void cnpcPostLivingEvent(Post<T, M> event) {
        CustomNpcs.debugData.start("Players");
        MarkData data = MarkData.get(event.getEntity());
        Player player = Minecraft.getInstance().player;
        for (MarkData.Mark m : data.marks) {
            if (m.getType() != 0 && m.availability.isAvailable(player)) {
                MarkRenderer.render(event, m);
                break;
            }
        }
        if (player != null && event.getEntity() instanceof Player pl && ClientEventHandler.chatMessages.containsKey(pl)) {
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            float height = pl.getBbHeight() + 0.9f;
            float offset = pl.getBbHeight() * 1.2F;
            poseStack.translate(0.0F, offset, 0.0F);
            ClientEventHandler.chatMessages.get(pl).renderMessages(poseStack, event.getMultiBufferSource(), 0.666667F * height,
                    isInRange(player, pl), event.getPackedLight(), true);
            poseStack.translate(0.0F, -offset, 0.0F);
            poseStack.popPose();
        }
        CustomNpcs.debugData.end("Players");
    }

    @SubscribeEvent
    public void cnpcPlaySoundSource(PlaySoundSourceEvent event) {
        CustomNpcs.debugData.start("Players");
        processSoundPlay(event, event.getName(), event.getSound(), event.getChannel());
        CustomNpcs.debugData.end("Players");
    }

    @SubscribeEvent
    public void cnpcPlayStreamingSource(PlayStreamingSourceEvent event) {
        CustomNpcs.debugData.start("Players");
        processSoundPlay(event, event.getName(), event.getSound(), event.getChannel());
        CustomNpcs.debugData.end("Players");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void cnpcScreenPostInit(ScreenEvent.Init.Post event) {
        CustomNpcs.debugData.start("Players");
        if (event.getScreen() instanceof InventoryScreen screen && CustomNpcs.InventoryGuiEnabled) {
            event.addListener((new InventoryTabVanilla()).init(screen));
            event.addListener((new InventoryTabFactions()).init(screen));
            event.addListener((new InventoryTabQuests()).init(screen));
        }
        else if (event.getScreen() instanceof TitleScreen) {
            CustomNpcs.resetChars(CustomNpcs.CharCurrencies, CustomNpcs.CharDonation);
            if (CustomNpcs.ReplaceCustomBackground && CustomNpcs.ShowButtonsInGuiMenu) {
                try {
                    String name = "cnpc$variant";
                    Field field = CubeMap.class.getField(name);
                    field.setAccessible(true);
                    GuiButtonNop button = new GuiButtonNop(null, 150, 3, 3, (b) -> {
                                int v = ((GuiButtonNop) b).getValue() - 1;
                                try { field.set(TitleScreen.CUBE_MAP, v); }  catch (Exception ignored) { }
                                ResourceLocation[] images = ((ICubeMapMixin) TitleScreen.CUBE_MAP).getImages();
                                for(int i = 0; i < 6; ++i) {
                                    if (v < 0) { images[i] = new ResourceLocation("textures/gui/title/background/panorama_" + i + ".png"); }
                                    else { images[i] = new ResourceLocation(CustomNpcs.MODID, "textures/gui/title/background/" + v + "/panorama_" + i + ".png"); }
                                }
                            },
                            (int) field.get(TitleScreen.CUBE_MAP) + 1, Component.literal("MC"),
                            Component.literal("1"),
                            Component.literal("2"),
                            Component.literal("3"),
                            Component.literal("4"))
                            .setSize(20, 16);
                    event.addListener(button);
                }
                catch (Exception ignored) {}
            }
        }
        else if (event.getScreen() instanceof CreativeModeInventoryScreen screen && CustomNpcs.InventoryGuiEnabled) {
            try {
                int x = screen.getGuiLeft() - 28;
                int y = screen.getGuiTop() + 6;
                GuiButtonNop button = new GuiButtonNop(null, 150, "", x, y,
                        (clicked) -> CustomNpcs.proxy.openGui(Minecraft.getInstance().player, new GuiLog(2)),
                        (b, graphics, mouseX, mouseY, partialTicks) -> {
                            PoseStack matrixStack = graphics.pose();
                            matrixStack.pushPose();
                            matrixStack.translate(b.getX(), b.getY() + b.getWidth() - 3, 0.0f);
                            matrixStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                            if (b.isHovered()) {
                                graphics.blit(CREATIVE_TABS, 0, 0, 26, 32, 26, 32);
                            } else {
                                graphics.blit(CREATIVE_TABS, 0, 0, 0, 2, 26, 29);
                            }
                            matrixStack.popPose();

                            matrixStack.pushPose();
                            matrixStack.translate(b.getX() + 9.0f, b.getY() + 5.0f, 0.0f);
                            float s = 16.0f / 256.0f;
                            matrixStack.scale(s, s, s);
                            graphics.blit(COMPASS_ICONS[(int) (31L - (System.currentTimeMillis() / 100L) % 32L)],
                                    0, 0, 0, 0, 256, 256);
                            matrixStack.popPose();
                        })
                        .setSize(29, 26)
                        .setTexture(CREATIVE_TABS)
                        .setUV(0, 96, 200, 20)
                        .setHoverTexts("quest.hover.compass.settings");
                event.addListener(button);
                button = new GuiButtonNop(null, 151, "", x, y + 24,
                        (clicked) -> CustomNpcs.proxy.openGui(Minecraft.getInstance().player, new GuiLog(0)),
                        (b, graphics, mouseX, mouseY, partialTicks) -> {
                            PoseStack matrixStack = graphics.pose();
                            matrixStack.pushPose();
                            matrixStack.translate(b.getX(), b.getY() + b.getWidth() - 3, 0.0f);
                            matrixStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                            if (b.isHovered()) {
                                graphics.blit(CREATIVE_TABS, 0, 0, 26, 32, 26, 32);
                            } else {
                                graphics.blit(CREATIVE_TABS, 0, 0, 0, 2, 26, 29);
                            }
                            matrixStack.popPose();

                            matrixStack.pushPose();
                            matrixStack.translate(b.getX() + 9.0f, b.getY() + 7.0f, 0.0f);
                            float s = 16.0f / 256.0f;
                            matrixStack.scale(s, s, s);
                            graphics.blit(BOOK, 0, 0, 0, 2, 256, 256);
                            matrixStack.popPose();
                        })
                        .setSize(32, 28)
                        .setTexture(CREATIVE_TABS)
                        .setUV(0, 128, 200, 20)
                        .setHoverTexts("key.quest.log");
                event.addListener(button);
            } catch (Exception e) { return; }
        }
        CustomNpcs.debugData.end("Players");
    }

    // New from Unofficial (BetaZavr)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void cnpcBackgroundRendered(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof InventoryScreen gui) {
            renderBalance(event.getGuiGraphics(), gui.getMinecraft().font, event.getMouseX(), event.getMouseY(),
                    ((IAbstractContainerScreenMixin) gui).getLeftPos() + 124,
                    ((IAbstractContainerScreenMixin) gui).getTopPos() + 61);
        }
        else if (event.getScreen() instanceof CreativeModeInventoryScreen gui && gui.isInventoryOpen()) {
            renderBalance(event.getGuiGraphics(), gui.getMinecraft().font, event.getMouseX(), event.getMouseY(),
                    ((IAbstractContainerScreenMixin) gui).getLeftPos() + 125,
                    ((IAbstractContainerScreenMixin) gui).getTopPos() + 32);
        }
    }

    @SubscribeEvent
    public void cnpcScreenOpening(ScreenEvent.Opening event) {
        CustomNpcs.debugData.start("Players");
        mc = Minecraft.getInstance();
        String oldScreenName = mc.screen == null ? "GuiIngame" : mc.screen.getClass().getSimpleName();
        if (oldScreenName.equals(event.getScreen().getClass().getSimpleName())) { LogWriter.info("Re-open GUI - " + event.getScreen().getClass()); }
        else {
            String newGUI = event.getScreen().getClass().getSimpleName();
            if (event.getScreen() instanceof GuiLog) {
                newGUI = switch (((GuiLog) event.getScreen()).type) {
                    case 0 -> "GuiFactionLog";
                    case 1 -> "GuiQuestLog";
                    case 2 -> "GuiCompassLog";
                    default -> "GuiInventoryLog";
                };
            }
            LogWriter.debug("Open GUI - " + event.getScreen().getClass().getName() + "; OLD - " + oldScreenName);
            Packets.sendServer(new SPacketPlayerScreen(newGUI, oldScreenName));
        }
        if (event.getScreen() instanceof GuiNpcPather) { movingPath.clear(); }
        else if (event.getScreen() instanceof EffectRenderingInventoryScreen && mc.player != null &&
                mc.player.isCreative() &&
                mc.player.getMainHandItem().getItem() instanceof ISpecBuilder item) {
            event.setCanceled(true);
            BuilderData builder = ItemBuilder.getBuilder(mc.player.getMainHandItem(), mc.player);
            int id = builder == null ? -1 : builder.getID();
            int type = builder == null ? item.getType() : builder.getType();
            if (id > -1) { Packets.sendServer(new SPacketGetBuildData(id, type)); }
            CustomNPCsScheduler.runTack(() -> NoppesUtil.requestOpenGUI(item.getGUIType(), new BlockPos(id, type, 0)), 100);
        }
        CustomNpcs.debugData.end("Players");
    }

    @SubscribeEvent
    public void cnpcScreenClosing(ScreenEvent.Closing event) {
        CustomNPCsScheduler.runTack(() -> {
            CustomNpcs.debugData.start("Players");
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) { Packets.sendServer(new SPacketPlayerScreen("GuiIngame", event.getScreen().getClass().getSimpleName())); }
            CustomNpcs.debugData.end("Players");
        }, 100);
    }

    /** HUD Bar Interface Canceled */
    @SubscribeEvent
    public void cnpcRenderGameOverlayPre(RenderGuiOverlayEvent.Pre event) {
        mc = Minecraft.getInstance();
        event.setCanceled(mc.screen instanceof GuiOpenCase || mc.screen instanceof GuiYellowDialogEditor ||
                !PlayerData.get(mc.player).overlay.isShowElementType(event.getOverlay().id()));
    }

    /** Any Regions + Camera Shake */
    @SubscribeEvent
    public void cnpcRenderWorldLast(RenderLevelStageEvent event) {
        mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { return; }
        CustomNpcs.debugData.start(mc.player);
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        // in game
        if (!ClientTickHandler.inGame) {
            CustomNpcs.debugData.started = System.currentTimeMillis();
            CustomNpcs.debugData.startedTicks = ClientTickHandler.ticks;
            ClientTickHandler.inGame = true;
            PlayerData data = PlayerData.get(mc.player);
            data.player = mc.player;
            data.name = mc.player.getName().getString();
            data.uuid = mc.player.getUUID().toString();
            miniMapLoaded = false;
            updateMiniMaps(true);
            YDEController.getInstance().getLevelData(ScriptController.getLevelKey());
            EventHooks.onEvent(ScriptController.Instance.clientScripts, EnumScriptType.LOGIN, new PlayerEvent.LoginEvent(data.scriptData.getPlayer()));
            CustomNpcsLangPack.check();
            LogWriter.debug("Client Player: Start game");
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            // Schematics builder blocks
            if (!schemes.isEmpty()) {
                for (TileBuilder tile : new ArrayList<>(schemes)) {
                    if (tile == null || !tile.hasLevel() ||
                            !tile.hasSchematic() || !tile.getShow() ||
                            !(Objects.requireNonNull(tile.getLevel()).getBlockEntity(tile.getBlockPos()) instanceof TileBuilder)) {
                        schemes.remove(tile);
                        continue;
                    }
                    SchematicWrapper wrapper = tile.getSchematic();
                    if (tile.getBlockPos() != BlockPos.ZERO &&
                            tile.getBlockPos().distSqr(mc.player.blockPosition()) <= 1000000.0D &&
                            tile.getShow() &&
                            mc.player.level() == tile.getLevel() &&
                            wrapper != null) {
                        renderSchem(event.getPoseStack(), bufferSource, event.getPartialTick(), wrapper, tile.rotation,
                                tile.getBlockPos().getX() - event.getCamera().getPosition().x + 1.0f,
                                tile.getBlockPos().getY() - event.getCamera().getPosition().y + tile.yOffset,
                                tile.getBlockPos().getZ() - event.getCamera().getPosition().z + 1.0f);
                    }
                }
            }
            // Show builder data
            BuilderData builder = ItemBuilder.getBuilder(mc.player.getMainHandItem(), mc.player);
            if (builder != null && builder.getID() > -1) {
                BlockPos pos = null;
                if (builder.getType() == 4) { pos = BlockPos.ZERO; }
                else if (mc.hitResult instanceof BlockHitResult blockHitResult && mc.hitResult.getType() == HitResult.Type.BLOCK) { pos = blockHitResult.getBlockPos(); }
                if (pos != null) { renderZone(event.getPoseStack(), bufferSource, event.getCamera(), builder, pos); }
            }
            // Show quest points
            if (CustomNpcs.TypeShowQuestCompass != 4) {
                float red;
                float green;
                float blue;
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                for (Quest quest : new ArrayList<>(QuestController.instance.quests.values())) {
                    for (QuestObjective task : List.of(quest.questInterface.tasks)) {
                        if (task != null && mc.player.level().dimension().location().equals(task.dimension) && Mth.sqrt((float) mc.player.distanceToSqr(task.pos.getCenter())) < 500.0) {
                            int r = task.rangeCompass -1;
                            AABB aabb = new AABB(-r, -r, -r, r, r, r);
                            red = FastColor.ARGB32.red(task.colorCompass) / 255.0f;
                            green = FastColor.ARGB32.green(task.colorCompass) / 255.0f;
                            blue = FastColor.ARGB32.blue(task.colorCompass) / 255.0f;

                            event.getPoseStack().pushPose();
                            RenderSystem.enableBlend();
                            event.getPoseStack().translate(task.pos.getX() + 0.5d, task.pos.getY() + 0.5d, task.pos.getZ() + 0.5d);
                            event.getPoseStack().translate(-event.getCamera().getPosition().x, -event.getCamera().getPosition().y, -event.getCamera().getPosition().z);
                            LevelRenderer.renderLineBox(event.getPoseStack(), bufferSource.getBuffer(RenderType.lines()), aabb,
                                    red, green, blue, 0.75f);
                            renderFilledBox(bufferSource.getBuffer(RenderType.debugFilledBox()),
                                    event.getPoseStack().last().pose(),
                                    aabb, true, red, green, blue, 0.15f);
                            RenderSystem.disableBlend();
                            event.getPoseStack().popPose();
                        }
                    }
                }
            }
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            if (!schemes.isEmpty()) { RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); }
            // Show block tool hitboxes
            ItemStack mainStack = mc.player.getMainHandItem();
            ItemStack offStack = mc.player.getOffhandItem();
            if (CustomNpcs.ShowHitboxWhenHoldTools && mainStack.getItem() instanceof INPCToolItem || offStack.getItem() instanceof INPCToolItem) {
                AABB searchArea = new AABB(-5.0, -5.0, -5.0, 5.0, 5.0, 5.0).move(mc.player.blockPosition());
                List<Entity> entities = new ArrayList<>(mc.level.getEntitiesOfClass(Entity.class, searchArea));
                entities.remove(mc.player);
                Entity highlightedEntity = null;
                if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) { highlightedEntity = ((EntityHitResult) mc.hitResult).getEntity(); }
                if (highlightedEntity != null && !entities.contains(highlightedEntity)) { entities.add(highlightedEntity); }
                event.getPoseStack().pushPose();
                RenderSystem.enableBlend();
                event.getPoseStack().translate(- event.getCamera().getPosition().x, - event.getCamera().getPosition().y, - event.getCamera().getPosition().z);
                for (Entity entity : entities) {
                    AABB bounds = entity.getBoundingBox();
                    float w = entity.getBbWidth() / 2;
                    if (mc.player.distanceTo(entity) - w <= 5.0) {
                        event.getPoseStack().pushPose();
                        LevelRenderer.renderLineBox(event.getPoseStack(), bufferSource.getBuffer(RenderType.lines()), bounds,
                                0.8f, 0.8f, 0.8f, 0.8f);
                        if (entity.equals(highlightedEntity)) {
                            LevelRenderer.renderLineBox(event.getPoseStack(), bufferSource.getBuffer(RenderType.lines()), bounds.inflate(entity.getBbWidth() / 20.0),
                                    0.8f, 0.3f, 0.6f, 1.0f);
                        }
                        if (entity instanceof EntityNPCInterface npc) {
                            AnimationConfig anim = npc.animation.getAnimation();
                            if (anim != null && anim.type == AnimationKind.ATTACKING) {
                                List<AABB> hitBoxes = anim.getDamageHitboxes(npc, npc.animation.getAnimationCurrentFrameID());
                                if (hitBoxes != null && !hitBoxes.isEmpty()) {
                                    for (AABB hitBox : hitBoxes) {
                                        LevelRenderer.renderLineBox(event.getPoseStack(), bufferSource.getBuffer(RenderType.lines()), hitBox,
                                                1.0f, 0.0f, 0.0f, 0.5f);
                                    }
                                }
                            }
                        }
                        event.getPoseStack().popPose();
                    }
                }
                RenderSystem.disableBlend();
                event.getPoseStack().popPose();
            }
            // Show NPC moving path
            if (mainStack.getItem() instanceof ItemNpcMovingPath || offStack.getItem() instanceof ItemNpcMovingPath) {
                Entity entity = ItemNpcMovingPath.getNpc(mainStack.getItem() instanceof ItemNpcMovingPath ? mainStack : offStack, mc.level);
                if (entity instanceof EntityCustomNpc npc) {
                    renderNpcMovingPath(event.getPoseStack(), bufferSource, event.getCamera(), npc);
                }
                else { movingPath.clear(); }
            }
            // Show rayTrace point
            int id = -1;
            if (mainStack.getItem() instanceof IItemBoundary) {
                if (mainStack.getTag() != null && mainStack.getTag().contains("RegionID", 3)) { id = mainStack.getTag().getInt("RegionID"); }
                Zone3D reg = BorderController.getInstance().getRegion(id);
                // choosing a central position to create a new region
                if ((PlayerData.get(mc.player).overlay.isPressedShift() || reg == null) &&
                        mc.hitResult instanceof BlockHitResult result && result.getType() != HitResult.Type.MISS) {
                    final BlockPos pos = getPos(result);
                    event.getPoseStack().pushPose();
                    RenderSystem.enableBlend();
                    event.getPoseStack().translate(pos.getX() + 0.5d, pos.getY(), pos.getZ() + 0.5d);
                    event.getPoseStack().translate(-event.getCamera().getPosition().x, -event.getCamera().getPosition().y, -event.getCamera().getPosition().z);
                    event.getPoseStack().mulPose(Axis.YP.rotationDegrees((float) ((System.currentTimeMillis() / 7) % 360)));
                    LevelRenderer.renderLineBox(event.getPoseStack(), bufferSource.getBuffer(RenderType.lines()),
                            new AABB(-0.35d, 0.15d, -0.35d, 0.35d, 0.85d, 0.35d),
                            1.0f, 0.50f, 1.0f, 1.0f);
                    RenderSystem.disableBlend();
                    event.getPoseStack().popPose();
                }
            }
            // Show Regions
            RenderSystem.enableBlend();
            double dist = 250.0d;
            for (Zone3D reg : BorderController.getInstance().getRegionsInWorld(mc.level.dimension().location())) {
                if (mc.player.isCreative()) { dist = Math.max(250.0d, Math.max(Math.max(reg.getMaxX(), reg.getMaxY()), reg.getMaxZ()) * 2.0d); }
                if (reg == null || !reg.dimension.equals(mc.level.dimension().location()) || reg.distanceTo(mc.player) > dist) { continue; }
                if (mc.player.isCreative()) { renderRegion(event.getPoseStack(), bufferSource, event.getCamera(), event.getPartialTick(), reg, id); }
                else if (reg.showInClient) { renderRegion(event.getPoseStack(), bufferSource, event.getCamera(), reg, -1); }
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            // minimaps
            if (mc.player.level().getGameTime() % 100 == 0 && secs != System.currentTimeMillis() / 1000) {
                secs = System.currentTimeMillis() / 1000;
                updateMiniMaps(false);
            }
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            long winID = mc.getWindow().getWindow();
            PlayerData playerData = PlayerData.get(mc.player);
            boolean isMoved = InputConstants.isKeyDown(winID, mc.options.keyUp.getKey().getValue()) ||
                    InputConstants.isKeyDown(winID, mc.options.keyDown.getKey().getValue()) ||
                    InputConstants.isKeyDown(winID, mc.options.keyRight.getKey().getValue()) ||
                    InputConstants.isKeyDown(winID, mc.options.keyLeft.getKey().getValue());
            if (playerData.overlay.isMoved != isMoved) {
                PlayerData.get(mc.player).overlay.isMoved = isMoved;
                Packets.sendServer(new SPacketPlayerIsMoved(isMoved));
            }
            Camera camera = mc.gameRenderer.getMainCamera();
            float amplitude = crashes.get(crashes.endTime - mc.level.getGameTime());
            if (amplitude != 0.0f) {
                float partialTicks = mc.getDeltaFrameTime();
                float currentYaw = camera.getYRot();
                float currentPitch = camera.getXRot();
                switch (crashes.type) {
                    case 0: {
                        setRotation(camera, currentYaw, currentPitch + partialTicks * amplitude);
                        break;
                    } // vertical only
                    case 1: {
                        setRotation(camera, currentYaw + partialTicks * amplitude, currentPitch);
                        break;
                    } // horizontal only
                    case 2: break; // arc only
                    case 3: {
                        setRotation(camera, currentYaw + partialTicks * amplitude, currentPitch + partialTicks * amplitude);
                        break;
                    } // vertical and horizontal
                    case 4: {
                        setRotation(camera, currentYaw, currentPitch + partialTicks * amplitude);
                        break;
                    } // vertical and arc
                    default: {
                        setRotation(camera, currentYaw + partialTicks * amplitude, currentPitch + partialTicks * amplitude);
                        break;
                    } // all
                }
            }
        }
        CustomNpcs.debugData.end(mc.player);
    }

    private void setRotation(Camera camera, float yaw, float pitch) {
        ICameraMixin ic = (ICameraMixin) camera;
        ic.setYRot(yaw);
        ic.setXRot(pitch);
        Quaternionf rotation = ic.getRotation();
        rotation.rotationYXZ(-yaw * ((float)Math.PI / 180F), pitch * ((float)Math.PI / 180F), 0.0F);
        ic.getForwards().set(0.0F, 0.0F, 1.0F).rotate(rotation);
        ic.getUp().set(0.0F, 1.0F, 0.0F).rotate(rotation);
        ic.getLeft().set(1.0F, 0.0F, 0.0F).rotate(rotation);
    }

    private boolean isInRange(Player main, Player player) {
        double y = Math.abs(main.getY() - player.getY());
        if (player.getY() >= 0.0 && y > 8.0) { return false; }
        double x = Math.abs(main.getX() - player.getX());
        double z = Math.abs(main.getZ() - player.getZ());
        return x <= 8.0 && z <= 8.0;
    }

    private void processSoundPlay(Event event, String name, SoundInstance sound, Channel channel) {
        if (sound == null || channel.stopped()) { return; }
        Minecraft mc = Minecraft.getInstance();
        MusicData md = new MusicData(name, sound, channel, mc.level);
        ClientTickHandler.musics.add(md);
        md.createClientEvent(event, mc.player, 0);
        if (mc.level != null && mc.getConnection() != null) { Packets.sendServer(new SPacketPlayerSound(true, md)); }
    }

    private BlockPos getPos(BlockHitResult result) {
        int x = result.getBlockPos().getX();
        int y = result.getBlockPos().getY();
        int z = result.getBlockPos().getZ();
        try {
            switch (result.getDirection()) {
                case UP: y += 1; break;
                case NORTH: z -= 1; break;
                case SOUTH: z += 1; break;
                case WEST: x -= 1; break;
                case EAST: x += 1; break;
                default: y -= 1; break;
            }
        }
        catch (Exception e) { LogWriter.error(e); }
        return new BlockPos(x, y, z);
    }

    private void renderNpcMovingPath(PoseStack matrixStack, MultiBufferSource.BufferSource bufferSource, Camera camera, EntityCustomNpc npc) {
        List<int[]> list = npc.ais.getMovingPath();
        if (list.size() < 2) { Packets.sendServerDelayed(new SPacketGetMovingPath(npc.getId()), npc, 5000); }
        if (list.isEmpty() || mc.level == null) {
            movingPath.clear();
            return;
        }
        boolean type = npc.ais.getMovingPathType() == 0;
        // create path
        if (npc.ais.getMovingType() == 2 && (movingPath.isEmpty() || mc.level.getGameTime() % 100L == 0L)) {
            CompoundTag npcNbt = new CompoundTag();
            npc.saveAsPassenger(npcNbt);
            Optional<Entity> entityO = EntityType.create(npcNbt, mc.level);
            if (entityO.isPresent()) {
                Entity entity = entityO.get();
                entity.setUUID(UUID.randomUUID());
                List<Vec3> newMovingPath = new ArrayList<>();
                if (entity instanceof EntityCustomNpc newNpc) {
                    int[] pos = list.get(0);
                    double yo = 0.0d;
                    BlockState state = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
                    if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) { yo = 1.0d; }
                    newNpc.setPos(pos[0], pos[1] + yo, pos[2]);
                    newMovingPath.add(new Vec3(pos[0] + 0.5d, pos[1] + yo + 0.4d, pos[2] + 0.5d));
                    newNpc.display.setVisible(1);
                    newNpc.display.setSize(1);
                    newNpc.display.setShowName(1);
                    mc.level.addFreshEntity(newNpc);
                    PathNavigation nv = newNpc.getNavigation();
                    for (int i = 1; i < list.size(); i++) {
                        pos = list.get(i);
                        nv.stop();
                        newNpc.setDeltaMovement(new Vec3(0.0d, 0.0d, 0.0d));
                        Path path = nv.createPath(pos[0], pos[1], pos[2], 1);
                        if (path == null) {
                            newMovingPath.add(null);
                            yo = 0.0d;
                            state = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
                            if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) { yo = 1.0d; }
                            newNpc.setPos(pos[0], pos[1] + yo, pos[2]);
                            continue;
                        }
                        for (int p = 0; p < path.getNodeCount(); p++) {
                            Node pp = path.getNode(p);
                            newMovingPath.add(new Vec3(pp.x + 0.5d, pp.y + 0.4d, pp.z + 0.5d));
                        }
                        yo = 0.0d;
                        state = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
                        if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) { yo = 1.0d; }
                        newNpc.setPos(pos[0], pos[1] + yo, pos[2]);
                    }
                    if (type) {
                        nv.stop();
                        pos = list.get(list.size() - 1);
                        yo = 0.0d;
                        state = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
                        if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) { yo = 1.0d; }
                        newNpc.setPos(pos[0], pos[1] + yo, pos[2]);
                        newNpc.setDeltaMovement(new Vec3(0.0d, 0.0d, 0.0d));
                        pos = list.get(0);
                        Path path = nv.createPath(pos[0], pos[1], pos[2], 1);
                        if (path != null) {
                            for (int p = 0; p < path.getNodeCount(); p++) {
                                Node pp = path.getNode(p);
                                newMovingPath.add(new Vec3(pp.x + 0.5d, pp.y + 0.4d, pp.z + 0.5d));
                            }
                        }
                    }
                    else {
                        pos = list.get(list.size() - 1);
                        yo = 0.0d;
                        state = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
                        if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) { yo = 1.0d; }
                        newNpc.setPos(pos[0], pos[1] + yo, pos[2]);
                        for (int i = list.size() - 1; i >= 0; i--) {
                            pos = list.get(i);
                            nv.stop();
                            newNpc.setDeltaMovement(new Vec3(0.0d, 0.0d, 0.0d));
                            Path path = nv.createPath(pos[0], pos[1], pos[2], 1);
                            if (path == null) {
                                newMovingPath.add(null);
                                yo = 0.0d;
                                state = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
                                if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) { yo = 1.0d; }
                                newNpc.setPos(pos[0], pos[1] + yo, pos[2]);
                                continue;
                            }
                            for (int p = 0; p < path.getNodeCount(); p++) {
                                Node pp = path.getNode(p);
                                newMovingPath.add(new Vec3(pp.x + 0.5d, pp.y + 0.6d, pp.z + 0.5d));
                            }
                            yo = 0.0d;
                            state = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
                            if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) { yo = 1.0d; }
                            newNpc.setPos(pos[0], pos[1] + yo, pos[2]);
                        }
                        nv.stop();
                        pos = list.get(0);
                        yo = 0.0d;
                        state = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
                        if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) { yo = 1.0d; }
                        newNpc.setPos(pos[0], pos[1] + yo, pos[2]);
                        newNpc.setDeltaMovement(new Vec3(0.0d, 0.0d, 0.0d));
                        pos = list.get(0);
                        Path path = nv.createPath(pos[0], pos[1], pos[2], 1);
                        if (path != null) {
                            for (int p = 0; p < path.getNodeCount(); p++) {
                                Node pp = path.getNode(p);
                                newMovingPath.add(new Vec3(pp.x + 0.5d, pp.y + 0.6d, pp.z + 0.5d));
                            }
                        }
                    }
                }
                if (newMovingPath.size() > list.size() * 2) {
                    movingPath.clear();
                    movingPath.addAll(newMovingPath);
                }
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        }
        Vec3 pre;
        double dx = -camera.getPosition().x;
        double dy = -camera.getPosition().y;
        double dz = -camera.getPosition().z;
        float r = 0.75f, g = 0.75f, b = 0.75f, ag = 15.0f;
        VertexConsumer consumerLine = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = matrixStack.last().pose();
        // HitBox
        matrixStack.pushPose();
        matrixStack.translate(npc.getX(), npc.getY(), npc.getZ());
        double w = npc.getBbWidth() / 2;
        LevelRenderer.renderLineBox(matrixStack, consumerLine,
                new AABB(w * -1.0d, 0.0d, w * -1.0d, w, npc.getBbHeight(), w).move(dx, dy, dz), r, g, b, 1.0f);
        matrixStack.popPose();
        // Eyes + Head rotation
        matrixStack.pushPose();
        IRayTraceVec pHh = Util.instance.getPosition(npc.getX(), npc.getY() + npc.getEyeHeight(), npc.getZ(), npc.getYHeadRot(), 0.0d, npc.getBbWidth() / 2.0d);
        IRayTraceVec pEr = Util.instance.getPosition(pHh.getX(), pHh.getY(), pHh.getZ(), npc.getYHeadRot(), npc.getXRot() * -1.0d, 0.7d / 5.0d * npc.display.getSize());
        r = 0.25f;
        g = 0.5f;
        consumerLine.vertex(matrix, (float) (pHh.getX() + dx), (float) (pHh.getY() + dy), (float) (pHh.getZ() + dz))
                .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                .normal(0, 0, 1)
                .endVertex();
        consumerLine.vertex(matrix, (float) (pEr.getX() + dx), (float) (pEr.getY() + dy), (float) (pEr.getZ() + dz))
                .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                .normal(0, 0, 1)
                .endVertex();
        // is direct
        if (npc.ais.directLOS != EnumSeeTarget.NORMAL && npc.ais.directLOS != EnumSeeTarget.BLIND && npc.ais.directLOS != EnumSeeTarget.NONE) {
            IRayTraceVec mr = Util.instance.getPosition(pHh.getX(), pHh.getY(), pHh.getZ(), npc.getYHeadRot() + 60.0d, 0.0d, 0.7d / 5.0d * npc.display.getSize());
            IRayTraceVec nr = Util.instance.getPosition(pHh.getX(), pHh.getY(), pHh.getZ(), npc.getYHeadRot() - 60.0d, 0.0d, 0.7d / 5.0d * npc.display.getSize());
            IRayTraceVec mp = Util.instance.getPosition(pHh.getX(), pHh.getY(), pHh.getZ(), npc.getYRot(), ValueUtil.correctDouble(npc.getXRot() * -1.0d + 60.0d, -90.0d, 90.0d), 1.4d / 5.0d * npc.display.getSize());
            IRayTraceVec np = Util.instance.getPosition(pHh.getX(), pHh.getY(), pHh.getZ(), npc.getYRot(), ValueUtil.correctDouble(npc.getXRot() * -1.0d - 60.0d, -90.0d, 90.0d), 1.4d / 5.0d * npc.display.getSize());
            r = 0.525f;
            g = 0.725f;
            b = 0.125f;
            consumerLine.vertex(matrix, (float) (pHh.getX() + dx), (float) (pHh.getY() + dy), (float) (pHh.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (mr.getX() + dx), (float) (mp.getY() + dy), (float) (mr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (pHh.getX() + dx), (float) (pHh.getY() + dy), (float) (pHh.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (mr.getX() + dx), (float) (np.getY() + dy), (float) (mr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();

            consumerLine.vertex(matrix, (float) (mr.getX() + dx), (float) (mp.getY() + dy), (float) (mr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (mr.getX() + dx), (float) (np.getY() + dy), (float) (mr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (pHh.getX() + dx), (float) (pHh.getY() + dy), (float) (pHh.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (nr.getX() + dx), (float) (mp.getY() + dy), (float) (nr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();

            consumerLine.vertex(matrix, (float) (pHh.getX() + dx), (float) (pHh.getY() + dy), (float) (pHh.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (nr.getX() + dx), (float) (np.getY() + dy), (float) (nr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (nr.getX() + dx), (float) (mp.getY() + dy), (float) (nr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (nr.getX() + dx), (float) (np.getY() + dy), (float) (nr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();

            consumerLine.vertex(matrix, (float) (mr.getX() + dx), (float) (mp.getY() + dy), (float) (mr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (nr.getX() + dx), (float) (mp.getY() + dy), (float) (nr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (mr.getX() + dx), (float) (np.getY() + dy), (float) (mr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (nr.getX() + dx), (float) (np.getY() + dy), (float) (nr.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
        }
        // aggroRange
        if (npc.ais.directLOS == EnumSeeTarget.BLIND) {
            r = 0.785f;
            g = 0.785f;
            b = 0.195f;
        } // sound only
        else {
            r = 0.785f;
            g = 0.195f;
            b = 0.195f;
        }
        renderSphere(matrix, consumerLine,
                (float) (npc.getX() + dx), (float) (npc.getY() + dy), (float) (npc.getZ() + dz),
                npc.stats.aggroRange, 12, 16, r, g, b);
        if (npc.ais.directLOS == EnumSeeTarget.WARY || npc.ais.directLOS == EnumSeeTarget.REALISTIC) {
            renderSphere(matrix, consumerLine,
                    (float) (npc.getX() + dx), (float) (npc.getY() + dy), (float) (npc.getZ() + dz),
                    ValueUtil.correctFloat(npc.stats.aggroRange / 4.0f, 3.0f, npc.stats.aggroRange),
                    12, 16, r, g, b);
        } // guaranteed attack
        // tactic sphere
        if (npc.ais.tacticalVariant != EnumNpcTactics.RUSH && npc.ais.tacticalVariant != EnumNpcTactics.NONE && npc.ais.getTacticalRange() > 0) {
            renderSphere(matrix, consumerLine,
                    (float) (npc.getX() + dx), (float) (npc.getY() + dy), (float) (npc.getZ() + dz),
                    npc.ais.getTacticalRange() * 0.99f, 14, 18, 0.195f, 0.195f, 0.785f);
        }
        matrixStack.popPose();
        // Target
        if (npc.getTarget() != null) {
            LivingEntity target = npc.getTarget();
            matrixStack.pushPose();
            matrixStack.translate(dx, dy, dz);
            r = 0.8f;
            g = 0.0f;
            b = 0.8f;
            consumerLine.vertex(matrix, (float) (target.getX() + dx), (float) (target.getY() + target.getEyeHeight() + dy), (float) (target.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            consumerLine.vertex(matrix, (float) (npc.getX() + dx), (float) (npc.getY() + npc.getEyeHeight() + dy), (float) (npc.getZ() + dz))
                    .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    .normal(0, 0, 1)
                    .endVertex();
            matrixStack.popPose();
        }
        // Now way
        if (npc.navigating != null) {
            List<Node> nodes = ((IPathMixin) npc.navigating).getNodes();
            if (nodes != null) {
                matrixStack.pushPose();
                matrixStack.translate(dx, dy, dz);
                r = 0.156862f;
                g = 0.705882f;
                b = 0.352941f;
                pre = new Vec3(npc.getX(), npc.getY() + (double) npc.getEyeHeight(), npc.getZ());
                int currentPos = nodes.size() - 1;
                double md = -1.0d;
                int i = 0;
                for (Node node : nodes) {
                    double d = npc.distanceToSqr((double) node.x + 0.5d,
                            (double) node.y + (double) npc.getEyeHeight() / 2.0d, (double) node.z + 0.5d);
                    if (md == -1.0d || d <= md) {
                        md = d;
                        currentPos = i;
                    }
                    i++;
                }
                i = 0;
                for (Node node : nodes) {
                    if (i >= currentPos) {
                        Vec3 vec = new Vec3((double) node.x + 0.5d,
                                (double) node.y + (double) npc.getEyeHeight() / 2.0d,
                                (double) node.z + 0.5d);
                        consumerLine.vertex(matrix, (float) (pre.x + dx), (float) (pre.y + dy), (float) (pre.z + dz))
                                .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                                .normal(0, 0, 1)
                                .endVertex();
                        consumerLine.vertex(matrix, (float) (vec.x + dx), (float) (vec.y + dy), (float) (vec.z + dz))
                                .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                                .normal(0, 0, 1)
                                .endVertex();
                        pre = vec;
                    }
                    i++;
                }
                matrixStack.popPose();
            }
        }
        // Can Way
        if (movingPath.size() > 1) {
            matrixStack.pushPose();
            matrixStack.translate(dx, dy, dz);
            r = 0.8f;
            g = 0.8f;
            b = 0.8f;
            pre = null;
            for (Vec3 vec : movingPath) {
                if (vec == null) { pre = null; }
                else {
                    if (pre != null) {
                        consumerLine.vertex(matrix, (float) (pre.x + dx), (float) (pre.y + dy), (float) (pre.z + dz))
                                .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                                .normal(0, 0, 1)
                                .endVertex();
                        consumerLine.vertex(matrix, (float) (vec.x + dx), (float) (vec.y + dy), (float) (vec.z + dz))
                                .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                                .normal(0, 0, 1)
                                .endVertex();
                    }
                    pre = vec;
                }
            }
            pre = movingPath.get(0);
            Vec3 vec = movingPath.get(movingPath.size() - 1);
            if (type && pre != null && vec != null) {
                consumerLine.vertex(matrix, (float) (pre.x + dx), (float) (pre.y + dy), (float) (pre.z + dz))
                        .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                        .normal(0, 0, 1)
                        .endVertex();
                consumerLine.vertex(matrix, (float) (vec.x + dx), (float) (vec.y + dy), (float) (vec.z + dz))
                        .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                        .normal(0, 0, 1)
                        .endVertex();
            }
            matrixStack.popPose();
        }
        // Way
        matrixStack.pushPose();
        r = type ? 0.75f : 0.0f;
        g = 0.0f;
        b = 0.75f;
        pre = null;
        for (int i = 0; i < list.size(); i++) {
            int[] pos = list.get(i);
            double yo = 0.0d;
            BlockState state = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
            if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) { yo = 1.0d; }
            Vec3 vec = new Vec3(pos[0] + 0.5d, pos[1] + 0.5d + yo, pos[2] + 0.5d);
            if (pre != null) {
                consumerLine.vertex(matrix, (float) (pre.x + dx), (float) (pre.y + dy), (float) (pre.z + dz))
                        .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                        .normal(0, 0, 1)
                        .endVertex();
                consumerLine.vertex(matrix, (float) (vec.x + dx), (float) (vec.y + dy), (float) (vec.z + dz))
                        .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                        .normal(0, 0, 1)
                        .endVertex();
                IRayTraceRotate d = Util.instance.getAngles3D(vec.x, vec.y, vec.z, pre.x, pre.y, pre.z);
                // to next arrow
                for (int h = 0; h < 4; h++) {
                    IRayTraceVec p = Util.instance.getPosition(vec.x, vec.y, vec.z,
                            d.getYaw() + (h == 0 ? ag : h == 1 ? -1.0d * ag : 0.0d),
                            -d.getPitch() + (h == 2 ? ag : h == 3 ? -1.0d * ag : 0.0d),
                            0.5d);
                    consumerLine.vertex(matrix, (float) (vec.x + dx), (float) (vec.y + dy), (float) (vec.z + dz))
                            .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                            .normal(0, 0, 1)
                            .endVertex();
                    consumerLine.vertex(matrix, (float) (p.getX() + dx), (float) (p.getY() + dy), (float) (p.getZ() + dz))
                            .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                            .normal(0, 0, 1)
                            .endVertex();
                }
                // back arrow
                if (!type) {
                    d = Util.instance.getAngles3D(pre.x, pre.y, pre.z, vec.x, vec.y, vec.z);
                    for (int h = 0; h < 4; h++) {
                        IRayTraceVec p = Util.instance.getPosition(pre.x, pre.y, pre.z,
                                d.getYaw() + (h == 0 ? ag : h == 1 ? -1.0d * ag : 0.0d),
                                -d.getPitch() + (h == 2 ? ag : h == 3 ? -1.0d * ag : 0.0d),
                                0.35d);
                        consumerLine.vertex(matrix, (float) (pre.x + dx), (float) (pre.y + dy), (float) (pre.z + dz))
                                .color(0.75f, 0.0f, 0.0f, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                                .normal(0, 0, 1)
                                .endVertex();
                        consumerLine.vertex(matrix, (float) (p.getX() + dx), (float) (p.getY() + dy), (float) (p.getZ() + dz))
                                .color(0.75f, 0.0f, 0.0f, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                                .normal(0, 0, 1)
                                .endVertex();
                    }
                }
            }
            pre = vec;
            // end line
            if (type && i == list.size() - 1 && list.size() > 1) {
                pos = list.get(0);
                vec = new Vec3(pos[0] + 0.5d, pos[1] + 0.5d + yo, pos[2] + 0.5d);
                consumerLine.vertex(matrix, (float) (pre.x + dx), (float) (pre.y + dy), (float) (pre.z + dz))
                        .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                        .normal(0, 0, 1)
                        .endVertex();
                consumerLine.vertex(matrix, (float) (vec.x + dx), (float) (vec.y + dy - yo), (float) (vec.z + dz))
                        .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                        .normal(0, 0, 1)
                        .endVertex();
                IRayTraceRotate d = Util.instance.getAngles3D(vec.x, vec.y - yo, vec.z, pre.x, pre.y, pre.z);
                // end arrow
                for (int h = 0; h < 4; h++) {
                    IRayTraceVec p = Util.instance.getPosition(vec.x, vec.y - yo, vec.z,
                            360.0d - d.getYaw() + (h == 0 ? ag : h == 1 ? -1.0d * ag : 0.0d),
                            0.0 - d.getPitch() + (h == 2 ? ag : h == 3 ? -1.0d * ag : 0.0d),
                            0.5d);
                    consumerLine.vertex(matrix, (float) (vec.x + dx), (float) (vec.y + dy - yo), (float) (vec.z + dz))
                            .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                            .normal(0, 0, 1)
                            .endVertex();
                    consumerLine.vertex(matrix, (float) (p.getX() + dx), (float) (p.getY() + dy), (float) (p.getZ() + dz))
                            .color(r, g, b, 1.0f).uv(0, 0).uv2(LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                            .normal(0, 0, 1)
                            .endVertex();
                }
            }
        }
        matrixStack.popPose();
        // Block Poses
        int guiSelect = -1;
        if (mc.screen instanceof GuiNpcPather gui && gui.scroll != null) { guiSelect = gui.scroll.getSelectedIndex(); }
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                r = 0.8f;
                g = 0.8f;
                b = 0.8f;
            }
            else { b = 0.0f; }
            int[] pos = list.get(i);
            double yo = 0.0d;
            BlockState state = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
            if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) { yo = 1.0d; }
            matrixStack.pushPose();
            matrixStack.translate(pos[0] + dx + 0.5d, pos[1] + dy + 0.5d + yo, pos[2] + dz + 0.5d);
            double s = i == 0 ? 0.125d : 0.075d;
            LevelRenderer.renderLineBox(matrixStack, consumerLine, new AABB(-s, -s, -s, s, s, s), r, g, b, 1.0f);
            if (guiSelect == i) {
                s *= 1.75d;
                LevelRenderer.renderLineBox(matrixStack, consumerLine, new AABB(-s, -s, -s, s, s, s), 0.0f, 1.0f, 1.0f, 0.8f);
            }
            matrixStack.popPose();
        }
    }

    private void renderRegion(PoseStack matrixStack, MultiBufferSource.BufferSource bufferSource, Camera camera, float partialTicks, Zone3D reg, int editID) {
        mc = Minecraft.getInstance();
        if (reg.size() == 0 || mc.player == null) { return; }
        double distMin = Double.MAX_VALUE;
        boolean start = true;
        Point playerPoint = new Point(mc.player.getBlockX(), mc.player.getBlockZ());
        Point nearestPoint = null;
        double[] nt = new double[] { 0.0d, 255.0d };

        // vertex, bound and get nearest point
        renderRegion(matrixStack, bufferSource, camera, reg, editID);
        if (reg.getId() != editID) { return; }
        // select point
        for (Point p : reg.points.values()) {
            if (start || distMin > p.distance(playerPoint)) {
                start = false;
                distMin = p.distance(playerPoint);
                nearestPoint = p;
                nt[0] = (double) reg.y[0] - 0.175d;
                nt[1] = (double) reg.y[1] + 1.175d;
            }
        }
        if (mc.hitResult instanceof BlockHitResult result && result.getType() != HitResult.Type.MISS) {
            BlockPos p = result.getBlockPos();
            float min = p.getY() < reg.y[0] ? (float) p.getY() : (float) reg.y[0];
            float max = (p.getY() > reg.y[1] ? (float) p.getY() : (float) reg.y[1]) + 1.0f;
            float x = p.getX() + 0.5f;
            float y = p.getY() + 0.5f;
            float z = p.getZ() + 0.5f;
            switch (result.getDirection()) {
                case UP: y += 0.55f; break;
                case NORTH: z -= 0.55f; break;
                case SOUTH: z += 0.55f; break;
                case WEST: x -= 0.55f; break;
                case EAST: x += 0.55f; break;
                default: y -= 0.55f; break;
            }
            renderVertex(matrixStack, bufferSource.getBuffer(RenderType.lines()), camera, x, y, z, 1.0f, 1.0f, 0.0f);
            // Bound
            Point pb = new Point((int) Math.floor(x), (int) Math.floor(z));
            Point[] pns = reg.getClosestPoints(pb, mc.player.getX(), mc.player.getZ());
            renderAddSegment(matrixStack, bufferSource, camera, pns, pb, min, max);
        }
        if (nearestPoint != null) {
            float red = 255.0f - (float) (reg.color >> 16 & 255) / 255.0f;
            float green = 255.0f - (float) (reg.color >> 8 & 255) / 255.0f;
            float blue = 255.0f - (float) (reg.color & 255) / 255.0f;

            matrixStack.pushPose();
            LevelRenderer.renderLineBox(matrixStack, bufferSource.getBuffer(RenderType.lines()),
                    new AABB(nearestPoint.x + 0.35d, nt[0], nearestPoint.y + 0.35d,
                            nearestPoint.x + 0.65d, nt[1], nearestPoint.y + 0.65d)
                            .move(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z),
                    red, green, blue, 1.0f);
            matrixStack.popPose();

            Matrix4f matrix4f = matrixStack.last().pose();
            Matrix3f matrix3f = matrixStack.last().normal();
            VertexConsumer vertices = bufferSource.getBuffer(RenderType.lines());
            float playerX = (float) mc.player.getX() + (float) (mc.player.getDeltaMovement().x * partialTicks) - (float) camera.getPosition().x - (float) mc.player.getDeltaMovement().x;
            float playerY = (float) mc.player.getY() + mc.player.getEyeHeight() * 0.9f + (float) (mc.player.getDeltaMovement().y * partialTicks) - (float) camera.getPosition().y - (float) mc.player.getDeltaMovement().y;
            float playerZ = (float) mc.player.getZ() + (float) (mc.player.getDeltaMovement().z * partialTicks) - (float) camera.getPosition().z - (float) mc.player.getDeltaMovement().z;
            vertices.vertex(matrix4f, playerX, playerY, playerZ)
                    .color(red, green, blue, 1.0f)
                    .normal(matrix3f, 0.0F, 1.0F, 0.0F)
                    .endVertex();
            vertices.vertex(matrix4f, (float) nearestPoint.x + 0.5f - (float) camera.getPosition().x,
                            (float) (nt[0] + (nt[1] - nt[0]) / 2.0d - camera.getPosition().y),
                            (float) nearestPoint.y + 0.5f - (float) camera.getPosition().z)
                    .color(red, green, blue, 1.0f)
                    .normal(matrix3f, 0.0F, 1.0F, 0.0F)
                    .endVertex();
        } // nearest vertex
    }

    private void renderRegion(PoseStack matrixStack, MultiBufferSource.BufferSource bufferSource, Camera camera, Zone3D reg, int editID) {
        if (reg == null || reg.size() == 0) { return; }
        long time = mc.level != null ? mc.level.getGameTime() : 0L;
        float red = (float) (reg.color >> 16 & 255) / 255.0f;
        float green = (float) (reg.color >> 8 & 255) / 255.0f;
        float blue = (float) (reg.color & 255) / 255.0f;
        // polygon texture size
        int xm = reg.getMinX();
        int xs = reg.getMaxX() - reg.getMinX();
        int zm = reg.getMinZ();
        int zs = reg.getMaxZ() - zm;
        float size = (float) (Math.max(xs, zs)) / 4.0f;
        RenderSystem.disableCull(); // Enable double-sided rendering
        // Walls
        if (reg.size() > 1) {
            matrixStack.pushPose();
            matrixStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
            RenderSystem.setShaderColor(red, green, blue, 1.0f);
            RenderSystem.setShaderTexture(0, BORDER[(int) (time % 16L)]);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            Matrix4f matrix4f = matrixStack.last().pose();
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            float startU = 0.0f;
            float distH = reg.getHeight() + 1.0f;
            for (int pos : reg.points.keySet()) {
                Point p0 = reg.points.get(pos);
                boolean isEnd = pos == reg.points.size() - 1;
                Point p1 = isEnd ? reg.points.get(0) : reg.points.get(pos + 1);
                // seamless texture connection between walls
                float distW = (float) p0.distance(p1);
                if (isEnd) { distW = Math.round(distW + startU); }
                buffer.vertex(matrix4f, p0.x + 0.5f, reg.y[0], p0.y + 0.5f).uv(startU, 0.0f).endVertex();
                buffer.vertex(matrix4f, p1.x + 0.5f, reg.y[0], p1.y + 0.5f).uv(startU + distW, 0.0f).endVertex();
                buffer.vertex(matrix4f, p1.x + 0.5f, reg.y[1] + 1.0f, p1.y + 0.5f).uv(startU + distW, distH).endVertex();
                buffer.vertex(matrix4f, p0.x + 0.5f, reg.y[1] + 1.0f, p0.y + 0.5f).uv(startU, distH).endVertex();
                startU += distW % 1.0f;
            }
            matrixStack.scale(1.0F, 1.0F, 1.0F);
            BufferUploader.drawWithShader(buffer.end());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            matrixStack.popPose();
        }
        // Polygons up and down
        if (reg.size() > 2) {
            matrixStack.pushPose();
            matrixStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
            RenderSystem.setShaderTexture(0, BORDER[(int) (time % 16L)]);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            Matrix4f matrix4f = matrixStack.last().pose();
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
            RenderSystem.setShaderColor(red, green, blue, 1.0f);
            List<float[][]> triangleList = reg.getTriangleList();
            float texU0, texV0, texU1, texV1, texU2, texV2;
            for (int i = 0; i < 2; i++) {
                float y = i == 0 ? (float) reg.y[1] + 0.98f : (float) reg.y[0] + 0.02f;
                for (float[][] tri : triangleList) {
                    texU0 = 2.0f * size * (tri[0][0] - xm) / (float) xs;
                    texV0 = 2.0f * size * (tri[0][1] - zm) / (float) zs;
                    texU1 = 2.0f * size * (tri[1][0] - xm) / (float) xs;
                    texV1 = 2.0f * size * (tri[1][1] - zm) / (float) zs;
                    texU2 = 2.0f * size * (tri[2][0] - xm) / (float) xs;
                    texV2 = 2.0f * size * (tri[2][1] - zm) / (float) zs;
                    buffer.vertex(matrix4f, tri[0][0] + 0.5f, y, tri[0][1] + 0.5f).uv(texU0, texV0).endVertex();
                    buffer.vertex(matrix4f, tri[1][0] + 0.5f, y, tri[1][1] + 0.5f).uv(texU1, texV1).endVertex();
                    buffer.vertex(matrix4f, tri[2][0] + 0.5f, y, tri[2][1] + 0.5f).uv(texU2, texV2).endVertex();
                }
            }
            BufferUploader.drawWithShader(buffer.end());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            matrixStack.popPose();
        }
        RenderSystem.enableCull(); // Returning to standard rendering mode
        if (reg.getId() == editID) {
            // all vertex
            for (int i = 0; i < reg.points.size(); i++) {
                Point p0 = reg.points.get(i);
                // vertex as * down and up
                if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof ItemBoundary) {
                    renderVertex(matrixStack, bufferSource.getBuffer(RenderType.lines()), camera, (float) p0.x + 0.5f, reg.y[0], (float) p0.y + 0.5f, red, green, blue);
                    renderVertex(matrixStack, bufferSource.getBuffer(RenderType.lines()), camera, (float) p0.x + 0.5f, (float) reg.y[1] + 1.0f, (float) p0.y + 0.5f, red, green, blue);
                }
            }
            // bound lines
            List<float[]> lines = reg.getContourLines();
            Matrix4f matrix4f = matrixStack.last().pose();
            Matrix3f matrix3f = matrixStack.last().normal();
            VertexConsumer vertices = bufferSource.getBuffer(RenderType.lines());
            matrixStack.pushPose();
            float x = 0.5f - (float) camera.getPosition().x;
            float z = 0.5f - (float) camera.getPosition().z;
            float minY = reg.y[0] - (float) camera.getPosition().y;
            float maxY = 1.0f + reg.y[1] - (float) camera.getPosition().y;
            // horizontal lines
            for (int i = 0; i < 2; i++) {
                float y = i == 0 ? minY : maxY;
                for (float[] cl : lines) {
                    vertices.vertex(matrix4f, cl[0] + x, y, cl[1] + z)
                            .color(red, green, blue, 0.5f)
                            .normal(matrix3f, 1.0F, 1.0F, 1.0F)
                            .endVertex();
                    vertices.vertex(matrix4f, cl[2] + x, y, cl[3] + z)
                            .color(red, green, blue, 0.5f)
                            .normal(matrix3f, 1.0F, 1.0F, 1.0F)
                            .endVertex();
                    vertices.vertex(matrix4f, cl[0] + x, y, cl[1] + z)
                            .color(red, green, blue, 0.5f)
                            .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                            .endVertex();
                    vertices.vertex(matrix4f, cl[2] + x, y, cl[3] + z)
                            .color(red, green, blue, 0.5f)
                            .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                            .endVertex();
                }
            }
            // vertical lines
            for (int i = 0; i < reg.points.size(); i++) {
                Point p0 = reg.points.get(i);
                vertices.vertex(matrix4f, p0.x + x, minY, p0.y + z)
                        .color(red, green, blue, 0.5f)
                        .normal(matrix3f, 0.0F, 1.0F, 0.0F)
                        .endVertex();
                vertices.vertex(matrix4f, p0.x + x, maxY, p0.y + z)
                        .color(red, green, blue, 0.5f)
                        .normal(matrix3f, 0.0F, 1.0F, 0.0F)
                        .endVertex();
                vertices.vertex(matrix4f, p0.x + x, minY, p0.y + z)
                        .color(red, green, blue, 0.5f)
                        .normal(matrix3f, 0.0F, -1.0F, 0.0F)
                        .endVertex();
                vertices.vertex(matrix4f, p0.x + x, maxY, p0.y + z)
                        .color(red, green, blue, 0.5f)
                        .normal(matrix3f, 0.0F, -1.0F, 0.0F)
                        .endVertex();
            }
            matrixStack.popPose();
        }
    }

    private void renderAddSegment(PoseStack matrixStack, MultiBufferSource.BufferSource bufferSource, Camera camera, Point[] points, Point p1, float minY, float maxY) {
        if (points == null || points.length != 2 || p1 == null) { return; }
        Matrix4f matrix4f = matrixStack.last().pose();
        Matrix3f matrix3f = matrixStack.last().normal();
        Point p0 = points[0];
        Point p2 = points[1];
        if (p0 == null || p2 == null) { return; }
        minY -= (float) camera.getPosition().y;
        maxY -= (float) camera.getPosition().y;
        float x = 0.5f - (float) camera.getPosition().x;
        float z = 0.5f - (float) camera.getPosition().z;
        // Walls
        float alpha = 0.3f;
        matrixStack.pushPose();
        VertexConsumer vertices = bufferSource.getBuffer(RenderType.gui());

        vertices.vertex(matrix4f, p0.x + x, minY, p0.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p0.x + x, maxY, p0.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p1.x + x, maxY, p1.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p1.x + x, minY, p1.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p1.x + x, minY, p1.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p1.x + x, maxY, p1.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p0.x + x, maxY, p0.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p0.x + x, minY, p0.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();

        vertices.vertex(matrix4f, p1.x + x, minY, p1.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p1.x + x, maxY, p1.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p2.x + x, maxY, p2.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p2.x + x, minY, p2.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p2.x + x, minY, p2.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p2.x + x, maxY, p2.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p1.x + x, maxY, p1.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();
        vertices.vertex(matrix4f, p1.x + x, minY, p1.y + z).color(0.75f, 0.75f, 0.75f, alpha).endVertex();

        // Lines
        vertices = bufferSource.getBuffer(RenderType.lines());
        alpha = 0.75f;

        vertices.vertex(matrix4f, p0.x + x, minY, p0.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 1.0F)
                .endVertex();
        vertices.vertex(matrix4f, p1.x + x, minY, p1.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 1.0F)
                .endVertex();
        vertices.vertex(matrix4f, p1.x + x, minY, p1.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, p2.x + x, minY, p2.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        if (maxY - minY > 1) {
            for (float i = 1.0f; i < maxY - minY; i++) {
                vertices.vertex(matrix4f, p0.x + x, minY + i, p0.y + z)
                        .color(0.75f, 0.75f, 0.75f, alpha)
                        .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                        .endVertex();
                vertices.vertex(matrix4f, p1.x + x, minY + i, p1.y + z)
                        .color(0.75f, 0.75f, 0.75f, alpha)
                        .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                        .endVertex();
                vertices.vertex(matrix4f, p1.x + x, minY + i, p1.y + z)
                        .color(0.75f, 0.75f, 0.75f, alpha)
                        .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                        .endVertex();
                vertices.vertex(matrix4f, p2.x + x, minY + i, p2.y + z)
                        .color(0.75f, 0.75f, 0.75f, alpha)
                        .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                        .endVertex();
            }
        }
        vertices.vertex(matrix4f, p0.x + x, maxY, p0.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, p1.x + x, maxY, p1.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, p1.x + x, maxY, p1.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, p2.x + x, maxY, p2.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, p0.x + x, minY, p0.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, p0.x + x, maxY, p0.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, p1.x + x, minY, p1.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, p1.x + x, maxY, p1.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, p2.x + x, minY, p2.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, p2.x + x, maxY, p2.y + z)
                .color(0.75f, 0.75f, 0.75f, alpha)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        matrixStack.popPose();
    }

    private void renderVertex(PoseStack matrixStack, VertexConsumer vertices, Camera camera, float x, float y, float z, float red, float green, float blue) {
        Matrix4f matrix4f = matrixStack.last().pose();
        Matrix3f matrix3f = matrixStack.last().normal();
        float sizeS = 0.15f;

        x -= (float) camera.getPosition().x;
        y -= (float) camera.getPosition().y;
        z -= (float) camera.getPosition().z;

        matrixStack.pushPose();

        vertices.vertex(matrix4f, x - sizeS, y - sizeS, z - sizeS)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, 1.0F, 0.0F, 1.0F)
                .endVertex();
        vertices.vertex(matrix4f, x + sizeS, y + sizeS, z + sizeS)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, -1.0F, 0.0F, -1.0F)
                .endVertex();

        vertices.vertex(matrix4f, x - sizeS, y - sizeS, z + sizeS)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, 1.0F, 1.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, x + sizeS, y + sizeS, z - sizeS)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, -1.0F, -1.0F, 0.0F)
                .endVertex();

        vertices.vertex(matrix4f, x + sizeS, y - sizeS, z + sizeS)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, 1.0F, 1.0F, 1.0F)
                .endVertex();
        vertices.vertex(matrix4f, x - sizeS, y + sizeS, z - sizeS)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, -1.0F, -1.0F, -1.0F)
                .endVertex();

        vertices.vertex(matrix4f, x + sizeS, y - sizeS, z - sizeS)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, 1.0F, 1.0F, 1.0F)
                .endVertex();
        vertices.vertex(matrix4f, x - sizeS, y + sizeS, z + sizeS)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, -1.0F, -1.0F, -1.0F)
                .endVertex();

        vertices.vertex(matrix4f, x - sizeS, y, z)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, 1.0F, 0.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, x + sizeS, y, z)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, -1.0F, 0.0F, 0.0F)
                .endVertex();

        vertices.vertex(matrix4f, x, y - sizeS, z)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, 0.0F, 1.0F, 0.0F)
                .endVertex();
        vertices.vertex(matrix4f, x, y + sizeS, z)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, 0.0F, -1.0F, 0.0F)
                .endVertex();

        vertices.vertex(matrix4f, x, y, z - sizeS)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, 0.0F, 0.0F, 1.0F)
                .endVertex();
        vertices.vertex(matrix4f, x, y, z + sizeS)
                .color(red, green, blue, 1.0f)
                .normal(matrix3f, 0.0F, 0.0F, -1.0F)
                .endVertex();

        matrixStack.popPose();
    }

    private void renderZone(PoseStack matrixStack, MultiBufferSource.BufferSource bufferSource, Camera camera, BuilderData builder, @Nonnull BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { return; }
        double dx = camera.getPosition().x;
        double dy = camera.getPosition().y;
        double dz = camera.getPosition().z;
        if (builder.getType() < 3) {
            int[] s = new int[] { 0, 0, 0 };
            int[] e = new int[] { 1, 1, 1 };
            float r = 1.0f, g = 0.0f, b = 0.0f;
            int[] m = builder.getDirections(mc.player);
            for (int j = 0; j < 3; j++) {
                s[j] = m[j];
                e[j] = m[j + 3];
            }
            if (builder.getType() == 1) {
                r = 0.0f;
                g = 1.0f;
                b = 1.0f;
            }
            else if (builder.getType() == 2) {
                g = 0.0f;
                b = 1.0f;
            }
            RenderSystem.enableBlend();
            matrixStack.pushPose();
            matrixStack.translate(pos.getX() + s[0] - dx, pos.getY() + s[1] - dy, pos.getZ() + s[2] - dz);
            LevelRenderer.renderLineBox(matrixStack, bufferSource.getBuffer(RenderType.lines()),
                    new AABB(0, 0, 0, e[0], e[1], e[2]),
                    r, g, b, 1.0f);
            matrixStack.popPose();

            matrixStack.pushPose();
            matrixStack.translate(pos.getX() - dx + 0.5d, pos.getY() - dy, pos.getZ() - dz + 0.5d);
            LevelRenderer.renderLineBox(matrixStack, bufferSource.getBuffer(RenderType.lines()),
                    new AABB(-0.5d, 0.0d, -0.5d, 0.5d, 1.0d, 0.5d),
                    1.0f, 1.0f, 1.0f, 1.0f);
            matrixStack.popPose();
        }
        else if (builder.getType() == 3) {
            if (!builder.schematicName.isEmpty()) {
                String name = builder.schematicName + ".schematic";
                if (builder.schema == null) {
                    builder.schema = SchematicController.Instance.load(name);
                    if (builder.schema == null && !builder.schMap.isEmpty()) {
                        builder.schema = new SchematicWrapper(Schematic.create(mc.level, mc.player.getDirection(), name, builder.schMap));
                    }
                }
                if (builder.schema != null) {
                    IPos trPos = ((BlockPosWrapper) builder.schema.schema.getOffset()).rotate(mc.player.getDirection());
                    int rot;
                    switch (mc.player.getDirection()) {
                        case NORTH: {
                            trPos = trPos.offset(-builder.schema.schema.getWidth(), 0, -builder.schema.schema.getLength());
                            rot = 2;
                            break;
                        }
                        case WEST: {
                            trPos = trPos.offset(-builder.schema.schema.getLength(), 0, -1 * (builder.schema.schema.getWidth() - 1));
                            rot = 1;
                            break;
                        }
                        case EAST: {
                            trPos = trPos.offset(-1, 0, -builder.schema.schema.getWidth());
                            rot = 3;
                            break;
                        }
                        default: {
                            trPos = trPos.offset(-1, 0, -1);
                            rot = 0;
                            break;
                        }// SOUTH
                    }
                    renderSchem(matrixStack, bufferSource, mc.getPartialTick(), builder.schema, rot,
                            pos.getX() - dx + 1.0f + trPos.getX(), pos.getY() - dy + 0.003f + trPos.getY(), pos.getZ() - dz + 1.0f + trPos.getZ());
                }
            }
        }
        else if (builder.getType() == 4) {
            if (builder.schMap.size() < 3) {
                float r = 1.0f;
                float g = 1.0f;
                float b = 1.0f;
                if (builder.schMap.containsKey(1)) { r = 0.0f; g = 0.0f; }
                else if (builder.schMap.containsKey(0)) { r = 0.0f; b = 0.0f; }
                matrixStack.pushPose();
                matrixStack.translate(pos.getX() - dx, pos.getY() - dy, pos.getZ() - dz);
                LevelRenderer.renderLineBox(matrixStack, bufferSource.getBuffer(RenderType.lines()),
                        new AABB(-0.05d, -0.05d, -0.05d, 1.05d, 1.05d, 1.05d), r, g, b, 1.0f);
                matrixStack.popPose();
            }
            if (!builder.schMap.containsKey(0)) { return; }
            pos = builder.schMap.get(0);
            matrixStack.pushPose();
            matrixStack.translate(pos.getX() - dx, pos.getY() - dy, pos.getZ() - dz);
            LevelRenderer.renderLineBox(matrixStack, bufferSource.getBuffer(RenderType.lines()),
                    new AABB(0, 0, 0, 1, 1, 1),
                    1.0f, 1.0f, 1.0f, 1.0f);
            matrixStack.popPose();
            if (builder.schMap.containsKey(1)) {
                pos = builder.schMap.get(1);
                matrixStack.pushPose();
                matrixStack.translate(pos.getX() - dx, pos.getY() - dy, pos.getZ() - dz);
                LevelRenderer.renderLineBox(matrixStack, bufferSource.getBuffer(RenderType.lines()),
                        new AABB(0, 0, 0, 1, 1, 1),
                        0.0f, 1.0f, 0.0f, 1.0f);
                matrixStack.popPose();
            }
            if (builder.schMap.containsKey(2)) {
                pos = builder.schMap.get(2);
                matrixStack.pushPose();
                matrixStack.translate(pos.getX() - dx, pos.getY() - dy, pos.getZ() - dz);
                LevelRenderer.renderLineBox(matrixStack, bufferSource.getBuffer(RenderType.lines()),
                        new AABB(0, 0, 0, 1, 1, 1),
                        0.0f, 0.0f, 1.0f, 1.0f);
                matrixStack.popPose();
            }
            if (builder.schMap.containsKey(1) && builder.schMap.containsKey(2)) {
                AABB aabb = new AABB(builder.schMap.get(1), builder.schMap.get(2));
                pos = new BlockPos((int) Math.floor(aabb.minX), (int) Math.floor(aabb.minY), (int) Math.floor(aabb.minZ));
                matrixStack.pushPose();
                matrixStack.translate(pos.getX() - dx, pos.getY() - dy, pos.getZ() - dz);
                LevelRenderer.renderLineBox(matrixStack, bufferSource.getBuffer(RenderType.lines()),
                        new AABB(0, 0, 0,
                                aabb.maxX - aabb.minX + 1, aabb.maxY - aabb.minY + 1, aabb.maxZ - aabb.minZ + 1),
                        1.0f, 0.0f, 0.0f, 1.0f);
                matrixStack.popPose();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateMiniMaps(boolean update) {
        if (mc == null) { mc = Minecraft.getInstance(); }
        PlayerMiniMapData mm = PlayerData.get(mc.player).minimap;
        // Check save client Points:
        List<MiniMapData> points = new ArrayList<>();
        /**/
        if (ModList.get().isLoaded("journeymap")) {
            mm.addData.clear();
            if (!mm.modName.equals("journeymap")) {
                mm.modName = "journeymap";
                update = true;
            }
            try {
                Class<?> ws = Class.forName("journeymap.client.waypoint.WaypointStore");
                miniMapLoaded = (boolean) ws.getDeclaredMethod("hasLoaded").invoke(ws.getEnumConstants()[0]);
                if (!miniMapLoaded) {
                    CustomNPCsScheduler.runTack(() -> updateMiniMaps(true), 50);
                    return;
                }
                Collection<Object> waypoints = (Collection<Object>) ws.getDeclaredMethod("getAll")
                        .invoke(ws.getEnumConstants()[0]); // Collection<Object> waypoints = WaypointStore.INSTANCE.getAll();
                for (Object waypoint : waypoints) {
                    Class<?> wc = waypoint.getClass();
                    MiniMapData mmd = new MiniMapData();
                    mmd.name = (String) wc.getDeclaredMethod("getName").invoke(waypoint); // String
                    mmd.type = wc.getDeclaredMethod("getType").invoke(waypoint).toString(); // Normal, Death
                    mmd.icon = (String) wc.getDeclaredMethod("getIcon").invoke(waypoint); // ResourceLocation
                    mmd.pos = new BlockPosWrapper(null, (BlockPos) wc.getDeclaredMethod("getBlockPos").invoke(waypoint)); // BlockPos
                    mmd.color = new Color((int) wc.getDeclaredMethod("getR").invoke(waypoint),
                            (int) wc.getDeclaredMethod("getG").invoke(waypoint),
                            (int) wc.getDeclaredMethod("getB").invoke(waypoint)).getRGB();
                    mmd.isEnable = (boolean) wc.getDeclaredMethod("isEnable").invoke(waypoint);
                    Collection<String> dimensions = (Collection<String>) wc.getDeclaredMethod("getDimensions")
                            .invoke(waypoint);
                    mmd.dimIDs = new ArrayList<>();
                    for (String dim : dimensions) { mmd.dimIDs.add(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dim))); }
                    mmd.id = points.size();
                    points.add(mmd);
                    MiniMapData mmp = mm.get(mmd);
                    if (mmp != null) { mmd.setQuest(mmp); } else { update = true; }
                }
            } catch (Exception e) { LogWriter.debug("JourneyMap tried to collect its points"); }
        }
        else if (ModList.get().isLoaded("xaerominimap")) {
            if (!mm.modName.equals("xaerominimap")) {
                mm.modName = "xaerominimap";
                update = true;
            }
            try {
                Class<?> xhs = Class.forName("xaero.hud.HudSession");
                Object minimapSession = xhs.getDeclaredMethod("getCurrentSession").invoke(xhs); // HudSession
                if (minimapSession != null) {
                    Field fl = xhs.getDeclaredField("usable");
                    fl.setAccessible(true);
                    miniMapLoaded = fl.getBoolean(minimapSession);
                }
                else { miniMapLoaded = false; }
                if (!miniMapLoaded) {
                    CustomNPCsScheduler.runTack(() -> this.updateMiniMaps(true), 50);
                    return;
                }

                Class<?> xms = Class.forName("xaero.common.XaeroMinimapSession");
                // xaero.common.minimap.waypoints.WaypointsManager
                Object waypointsManager = xms.getDeclaredMethod("getWaypointsManager").invoke(minimapSession);

                Method getWaypointMap = waypointsManager.getClass().getDeclaredMethod("getWaypointMap");
                // HashMap<String, WaypointWorldContainer>
                HashMap<String, Object> waypointMap = (HashMap<String, Object>) getWaypointMap.invoke(waypointsManager);

                String mainContainerID = (String) waypointsManager.getClass()
                        .getDeclaredMethod("getAutoRootContainerID").invoke(waypointsManager);
                // xaero.common.minimap.waypoints.WaypointWorldContainer
                Object wwrc = waypointMap.get(mainContainerID);
                if (wwrc == null) {
                    if (!mm.points.isEmpty()) {
                        mm.points.clear();
                        update = true;
                    }
                }
                else {
                    mm.addData.clear();
                    Gson gson = new Gson();

                    Class<?> xmwc = Class.forName("xaero.hud.minimap.world.container.MinimapWorldContainer");
                    Field subContainers = xmwc.getField("subContainers");
                    Field worlds = wwrc.getClass().getField("worlds");
                    // Map<String, MinimapWorldContainer>
                    HashMap<String, Object> dimMap = (HashMap<String, Object>) subContainers.get(wwrc);
                    for (String k : dimMap.keySet()) {
                        if (!mm.addData.containsKey("xaero_world_name")) {
                            String world_name = (String) dimMap.get(k).getClass().getDeclaredMethod("getKey")
                                    .invoke(dimMap.get(k));
                            while (world_name.lastIndexOf("/") != -1) {
                                world_name = world_name.substring(0, world_name.lastIndexOf("/"));
                            }
                            mm.addData.put("xaero_world_name", world_name);
                        }
                        // Map<String, MinimapWorld>
                        HashMap<String, Object> worldMap = (HashMap<String, Object>) worlds.get(dimMap.get(k));
                        for (String k1 : worldMap.keySet()) {
                            if (!k1.equals("waypoints")) { continue; }
                            // xaero.hud.minimap.world.MinimapWorld
                            Object minimapWorld = worldMap.get(k1);
                            ResourceKey<Level> dimId = (ResourceKey<Level>) minimapWorld.getClass().getDeclaredMethod("getDimId") .invoke(minimapWorld);
                            Field f = minimapWorld.getClass().getDeclaredField("waypointSets");
                            f.trySetAccessible();
                            // Map<String, WaypointSet>
                            Map<String, Object> sets = (Map<String, Object>) f.get(minimapWorld);
                            for (String ks : sets.keySet()) {
                                // xaero.hud.minimap.waypoint.set.WaypointSet
                                Object waypointSet = sets.get(ks);

                                f = waypointSet.getClass().getDeclaredField("list");
                                f.setAccessible(true);
                                // List<Waypoint>
                                List<Object> list = (List<Object>) f.get(waypointSet);
                                for (Object waypoint : list) {
                                    // xaero.common.minimap.waypoints.Waypoint
                                    Class<?> wc = waypoint.getClass();
                                    MiniMapData mmd = new MiniMapData();
                                    mmd.name = (String) wc.getDeclaredMethod("getName").invoke(waypoint);
                                    mmd.type = wc.getDeclaredMethod("getWaypointType").invoke(waypoint).toString();
                                    mmd.icon = (String) wc.getDeclaredMethod("getSymbol").invoke(waypoint);
                                    int x = (int) wc.getDeclaredMethod("getX").invoke(waypoint);
                                    int y = (int) wc.getDeclaredMethod("getY").invoke(waypoint);
                                    int z = (int) wc.getDeclaredMethod("getZ").invoke(waypoint);
                                    mmd.pos = new BlockPosWrapper(null, new BlockPos(x, y, z));
                                    mmd.color = (int) wc.getDeclaredMethod("getColor").invoke(waypoint);
                                    mmd.isEnable = !((boolean) wc.getDeclaredMethod("isDisabled").invoke(waypoint));
                                    mmd.dimIDs = new ArrayList<>(Collections.singletonList(dimId));
                                    mmd.gsonData.put("temporary", gson.toJson(wc.getDeclaredMethod("isTemporary").invoke(waypoint)));
                                    mmd.id = points.size();
                                    points.add(mmd);
                                    MiniMapData mmp = mm.get(mmd);
                                    if (mmp != null) { mmd.setQuest(mmp); } else { update = true; }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {LogWriter.debug("XaeroMap tried to collect its points");}
        }
        else {
            mm.addData.clear();
            if (!mm.modName.equals("non")) {
                mm.modName = "non";
                update = true;
            }
        }
        if (!update && points.size() != mm.points.size()) { update = true; }
        // Send
        if (update) {
            mm.points.clear();
            mm.points.addAll(points);
            Packets.sendServer(new SPacketSyncUpdate(6, mm.save(new CompoundTag())));
        }
    }

    private void renderBalance(GuiGraphics graphics, Font font, int mouseX, int mouseY, int x, int y) {
        if ((CustomNpcs.ShowMoney || CustomNpcs.ShowDonat) && graphics != null && font != null && x !=0 && y != 0) {
            if (mc == null) { mc = Minecraft.getInstance(); }
            PlayerData data = PlayerData.get(mc.player);
            long money = data.game.getMoney();
            long donat = data.game.getDonat();
            int yM = y - (CustomNpcs.ShowMoney && CustomNpcs.ShowDonat ? 5 : 0);
            int yD = !CustomNpcs.ShowMoney ? y : yM + 12;
            PoseStack matrixStack = graphics.pose();
            // coins
            matrixStack.pushPose();
            matrixStack.translate(x, yM, 1.0f);
            float s = 16.0f / 256.0f;
            matrixStack.scale(s, s, s);
            if (CustomNpcs.ShowMoney) {
                graphics.blit(GuiBasic.MONEY, 0, 0, 0, 0, 256, 256);
                if (CustomNpcs.ShowDonat) { matrixStack.translate(0.0f, 192.0f, 0.0f); }
            }
            if (CustomNpcs.ShowDonat) {
                graphics.blit(GuiBasic.DONAT, 0, 0, 0, 0, 256, 256);
            }
            matrixStack.popPose();
            // text
            matrixStack.pushPose();
            matrixStack.translate(x + 16.0f, yM + (float) font.lineHeight / 2.0f, 1.0f);
            String text;
            if (CustomNpcs.ShowMoney) {
                text = Util.instance.getTextReducedNumber(money, true, true, false) + CustomNpcs.displayCurrencies;
                graphics.drawString(font, text, 0, 0, CustomNpcs.LableColor.getRGB(), false);
                if (CustomNpcs.ShowDonat) { matrixStack.translate(0.0f, 12.0f, 0.0f); }
            }
            if (CustomNpcs.ShowDonat) {
                text = Util.instance.getTextReducedNumber(donat, true, true, false) + CustomNpcs.displayDonation;
                graphics.drawString(font, text, 0, 0, CustomNpcs.LableColor.getRGB(), false);
            }
            matrixStack.popPose();
            // hover
            if (mouseX > x && mouseY > yM + 2 && mouseX < x + 50 && mouseY < yM + 34) {
                List<Component> hoverText = new ArrayList<>();
                if (CustomNpcs.ShowMoney && mouseY < yM + 14) {
                    hoverText.add(Component.translatable("inventory.hover.currency"));
                    hoverText.add(Component.literal("" + money));
                } // money
                else if (CustomNpcs.ShowDonat && mouseY >= yD  && mouseY < yD + 14) {
                    hoverText.add(Component.translatable("inventory.hover.donat"));
                    hoverText.add(Component.literal("" + donat));
                } // donat
                if (!hoverText.isEmpty()) {
                    RenderSystem.disableDepthTest();
                    GuiTooltipUtils.renderTooltip(graphics, font, hoverText, Optional.empty(), mouseX, mouseY);
                }
            }
        }
    }

    /** HUD: Mail Overlay */
    public static void renderMailOverlay(ForgeGui ignoredGui, GuiGraphics graphics, float ignoredPartialTick, int ignoredScreenWidth, int ignoredScreenHeight) {
        if ((hasNewMail || startMail > 0L) && CustomNpcs.MailWindow != -1) {
            PoseStack matrixStack = graphics.pose();
            CustomNpcs.MailWindow = 1;
            int[] offsets = new int[2];
            float sr = -45.0f, su = 12.0f, sv = -32.0f; // sr = 45.0f, su = 12.0f, sv = 32.0f;
            offsets[1] = (int) PlayerData.get(mc.player).overlay.getWindowSize().getHeight() - 32;
            matrixStack.pushPose();
            matrixStack.translate(offsets[0] + 16, offsets[1] + 16, 0);
            if (startMail == 0L) { startMail = System.currentTimeMillis(); }
            long time = System.currentTimeMillis() - startMail;
            // animation
            if (showNewMail == 0L || (time - showNewMail > -500L && time - showNewMail < 0L)) { // start
                if (showNewMail == 0L) {
                    showNewMail = time + 500L;
                }
                time -= showNewMail;
                matrixStack.mulPose(Axis.ZP.rotationDegrees(sr * (float) time / 500.0f));
                matrixStack.translate(su * (float) time / 500.0f, sv * (float) time / 500.0f, 0);
                if (time >= 0L) {
                    startMail = 0L;
                }
            }
            if (!hasNewMail) {
                if (time > 0L) {
                    startMail = System.currentTimeMillis() + 500L;
                    time = System.currentTimeMillis() - startMail;
                }
                time += 500L;
                time *= -1L;
                matrixStack.mulPose(Axis.ZP.rotationDegrees(sr * (float) time / 500.0f));
                matrixStack.translate(su * (float) time / 500.0f, sv * (float) time / 500.0f, 0);
                if (time < -480L) {
                    startMail = 0L;
                }
            } // end
            else if (time % 31500 < 1750) {
                time = time % 1750;
                if (time < 500) {
                    matrixStack.mulPose(Axis.ZP.rotationDegrees(30.0f * (float) time / 500.0f));
                    matrixStack.translate(-1.0f * (float) time / 500.0f, -5.0f * (float) time / 500.0f, 0);
                } else if (time < 1250) {
                    matrixStack.mulPose(Axis.ZP.rotationDegrees(30.0f - 420.0f * (float) (time -= 500L) / 750.0f));
                    matrixStack.translate(-1.0f + (float) time / 750.0f, -5.0f + 5.0f * (float) time / 750.0f,
                            0);
                } else {
                    matrixStack.mulPose(Axis.ZP.rotationDegrees(-30.0f + 30.0f * (float) (time - 1250L) / 500.0f));
                }
            } // living
            time = System.currentTimeMillis() % 3000;
            if (time < 1500) { RenderSystem.setShaderColor(0.85f, 0.85f, 0.85f, 0.5f + 0.45f * (float) time / 1500.f); }
            else { RenderSystem.setShaderColor(0.85f, 0.85f, 0.85f, 0.5f + 0.45f * (3000.0f - (float) time) / 1500.f); }
            matrixStack.scale(0.5f, 0.5f, 0.5f);
            RenderSystem.enableBlend();
            graphics.blit(GuiMailbox.icons, -16, -16, 0, 0, 32, 32);
            matrixStack.popPose();
        }
    }

    /** HUD: Quest Compass Overlay */
    public static void renderCompassOverlay(ForgeGui ignoredGui, GuiGraphics graphics, float ignoredPartialTick, int ignoredScreenWidth, int ignoredScreenHeight) {
        mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && (mc.screen == null || mc.screen instanceof ChatScreen || mc.screen instanceof GuiLog)) {
            PlayerData playerData = PlayerData.get(mc.player);
            PlayerCompassData compassData = playerData.compass;
            if (CustomNpcs.TypeShowQuestCompass != 4 && compassData.getShowOfPlayer()) {
                PoseStack matrixStack = graphics.pose();
                boolean isShow = true;
                if (!mc.player.isCreative() && (CustomNpcs.TypeShowQuestCompass == 2 || CustomNpcs.TypeShowQuestCompass == 3)) {
                    isShow = false;
                    for (int slotId = 0; slotId < mc.player.getInventory().getContainerSize(); slotId++) {
                        if (mc.player.getInventory().getItem(slotId).getItem() == Items.COMPASS) {
                            isShow = true;
                            break;
                        }
                    }
                }
                if (isShow) {
                    boolean needPoint = CustomNpcs.TypeShowQuestCompass == 1 || CustomNpcs.TypeShowQuestCompass == 3;
                    String name = "";
                    String title = "";
                    double[] point = null;
                    int taskType = -1;
                    int range = 5;
                    int taskColor = 0x808080;
                    String n = "";
                    if (compassData.isCustomPoint()) {
                        point = new double[] { compassData.pos.getX() - 0.5d, compassData.pos.getY() + 0.5d, compassData.pos.getZ() + 0.5d };
                        name = Util.instance.getOldFormattedText(Component.translatable(compassData.name));
                        title = Util.instance.getOldFormattedText(Component.translatable(compassData.title));
                        taskColor = compassData.color;
                        taskType = compassData.getTaskType();
                        if (!mc.level.dimension().location().toString().equals(compassData.getDimensionID())) { taskType = 7; }
                        range = compassData.getRange();
                        if (compassData.getNPCName().isEmpty()) {
                            n = Component.translatable("entity." + compassData.getNPCName() + ".name").getString();
                            n = n.substring(0, n.length() - 2);
                            if (n.equals("entity." + compassData.getNPCName() + ".name")) { n = compassData.getNPCName(); }
                        }
                    }
                    else {
                        if (!playerData.questData.activeQuests.containsKey(compassData.questID) || compassData.questID <= 0) {
                            for (int id : playerData.questData.activeQuests.keySet()) {
                                if (playerData.questData.activeQuests.get(id).quest.hasCompassSettings() && id != compassData.questID && id > 0) {
                                    compassData.questID = id;
                                    break;
                                }
                            }
                        }
                        QuestData qData = playerData.questData.activeQuests.get(compassData.questID);
                        if (qData != null) {
                            double minD = Double.MAX_VALUE;
                            QuestObjective select = null;
                            for (QuestObjective io : qData.quest.questInterface.getObjectives(mc.player)) {
                                if (io.isCompleted()) { continue; }
                                if (qData.quest.step != 1) {
                                    if (io.rangeCompass == 0 && select == null) {
                                        select = io;
                                    } else if (io.rangeCompass != 0) {
                                        double d = Util.instance.distanceTo(io.pos.getX() + 0.5d, io.pos.getY(), io.pos.getZ() + 0.5d, mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(), mc.player.getZ());
                                        if (d <= minD) {
                                            minD = d;
                                            select = io;
                                        }
                                    }
                                    continue;
                                }
                                select = io;
                                break;
                            }
                            if (select != null) {
                                name = Util.instance.getOldFormattedText(qData.quest.getTitle());
                                taskType = select.getType();
                                taskColor = select.colorCompass;
                                if (!select.getOrientationEntityName().isEmpty()) {
                                    n = Component.translatable("entity." + select.getOrientationEntityName() + ".name").getString();
                                    n = n.substring(0, n.length() - 2);
                                    if (n.equals("entity." + select.getOrientationEntityName() + ".name")) {
                                        n = select.getOrientationEntityName();
                                    }
                                }
                                if (!mc.level.dimension().location().equals(select.dimension)) { taskType = 7; }
                                if (taskType != EnumQuestTask.KILL.ordinal() && taskType != EnumQuestTask.AREAKILL.ordinal()) { range = 1; }
                                if (select.rangeCompass > 0) {
                                    range = select.rangeCompass;
                                    EnumQuestTask t = EnumQuestTask.values()[select.getType()];
                                    point = new double[] { select.pos.getX() - 0.5d, select.pos.getY() + 0.5d,
                                            select.pos.getZ() + 0.5d };
                                    if (t == EnumQuestTask.ITEM) {
                                        title = Component.translatable("gui.get").getString() + ": "
                                                + select.getItem().getDisplayName() + ": " + select.getProgress() + "/"
                                                + select.getMaxProgress();
                                    } else if (t == EnumQuestTask.CRAFT) {
                                        title = Component.translatable("gui.get").getString() + ": "
                                                + select.getItem().getDisplayName() + ": " + select.getProgress() + "/"
                                                + select.getMaxProgress();
                                    } else if (t == EnumQuestTask.DIALOG) {
                                        title = Component.translatable("gui.read").getString() + ": ";
                                        Dialog dialog = DialogController.instance.dialogs.get(select.getTargetID());
                                        if (dialog != null) {
                                            title += Component.translatable(dialog.title).getString();
                                        } else {
                                            title = "Dialog";
                                        }
                                    } else if (t == EnumQuestTask.LOCATION) {
                                        title = Component.translatable("gui.found").getString() + ": "
                                                + select.getTargetName();
                                    } else if (EnumQuestTask.values()[select.getType()] == EnumQuestTask.MANUAL) {
                                        title = select.getTargetName();
                                    }
                                    if (t == EnumQuestTask.KILL || t == EnumQuestTask.AREAKILL) {
                                        n = Component.translatable("entity." + select.getTargetName() + ".name").getString();
                                        n = n.substring(0, n.length() - 2);
                                        if (n.equals("entity." + select.getTargetName() + ".name")) {
                                            n = select.getTargetName();
                                        }
                                        title = Component.translatable("gui.kill").getString() + ": " + n + ": " + select.getProgress() + "/" + select.getMaxProgress();
                                    }
                                }
                            }
                            else if (qData.isCompleted && qData.quest.completion == EnumQuestCompletion.Npc && !qData.quest.completer.isEmpty()) {
                                point = new double[] { qData.quest.completer.getPos().getX() - 0.5d,
                                        qData.quest.completer.getPos().getY() + 0.5d,
                                        qData.quest.completer.getPos().getZ() + 0.5d };
                                taskType = EnumQuestTask.DIALOG.ordinal();
                                taskColor = 0x72CA00;
                                if (!mc.level.dimension().equals(qData.quest.completer.getDimension())) { taskType = 7; }
                                else {
                                    AABB bb = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).move(point[0], point[1], point[2]).inflate(64.0d, 128.0d, 64.0d);
                                    List<EntityNPCInterface> ents = mc.level.getEntitiesOfClass(EntityNPCInterface.class, bb);
                                    final EntityNPCInterface npc = getClosestNPC(point, ents, qData);
                                    if (npc != null) {
                                        point[0] = npc.getX();
                                        point[1] = npc.getY();
                                        point[2] = npc.getZ();
                                        range = 1;
                                    }
                                }
                            }
                        }
                    }

                    if (!n.isEmpty() && point != null) {
                        LivingEntity e = null;
                        AABB bb = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).move(point[0], point[1], point[2]).inflate(range, 1.5d, range);
                        List<LivingEntity> ents = mc.level.getEntitiesOfClass(LivingEntity.class, bb);
                        Player pl = mc.level.getNearestPlayer(mc.player, 32.0d);
                        if (pl != null && pl.getEffect(MobEffects.INVISIBILITY) == null) {
                            e = pl;
                            range = 1;
                        }
                        if (e == null) {
                            double d = range * range * range;
                            LivingEntity et = null;
                            Vec3 v = new Vec3(point[0], point[1], point[2]);
                            for (LivingEntity el : ents) {
                                if (!el.getName().getString().equals(n)) { continue; }
                                double r = v.distanceToSqr(el.getX(), el.getY(),el.getZ());
                                if (et != null) {
                                    if (r >= d) { continue; }
                                }
                                d = r;
                                et = el;
                            }
                            if (et == null) {
                                bb = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).move(point[0], point[1], point[2]).inflate(range, range, range);
                                ents.clear();
                                ents = mc.level.getEntitiesOfClass(LivingEntity.class, bb);
                                d = range * range * range;
                                for (LivingEntity el : ents) {
                                    if (!el.getName().getString().equals(n)) { continue; }
                                    double r = v.distanceToSqr(el.getX(), el.getY(),el.getZ());
                                    if (et != null) {
                                        if (r >= d) { continue; }
                                    }
                                    d = r;
                                    et = el;
                                }
                            }
                            e = et;
                            range = 1;
                        }
                        if (e != null) {
                            point[0] = e.getX();
                            point[1] = e.getY();
                            point[2] = e.getZ();
                        }
                    }

                    if (!needPoint || point != null) {
                        float scale = (compassData.isFlat ? 1.0f : -15.0f) * compassData.scale;
                        int[] uvPos = new int[] {(int) (mc.getWindow().getGuiScaledWidth() * compassData.screenPos[0]),
                                (int) (mc.getWindow().getGuiScaledHeight() * compassData.screenPos[1])};

                        matrixStack.pushPose();
                        if (qt < 40) { qt++; }
                        matrixStack.translate(uvPos[0], uvPos[1], 0.0d);
                        // Named
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.disableDepthTest();
                        if (compassData.showQuestName || compassData.showTaskProgress) {
                            matrixStack.pushPose();
                            float yOffset = compassData.isFlat ? 14.0f : 30.0f;
                            float h = 0.0f;
                            float w = 0.0f;
                            float w0 = 0.0f;
                            float w1 = 0.0f;
                            float l = 0.0f;
                            if (compassData.showQuestName && !name.isEmpty()) {
                                h += 10.0f;
                                w = ClientProxy.LogFont.width(name);
                                w0 = w;
                                if (uvPos[0] - w0 / 2.0f < 1.5f) { w0 = uvPos[0] * 2.0f - 3.0f; } // left
                                if (uvPos[0] + w0 / 2.0f > mc.getWindow().getGuiScaledWidth() - 1.5f) {
                                    w0 = (w0 - (mc.getWindow().getGuiScaledWidth() - uvPos[0]) + 3.0f) * 2.0f;
                                } // right
                                l = w0 / -2.0f;
                            }
                            if (compassData.showTaskProgress && !title.isEmpty()) {
                                h += 10.0f;
                                w1 = ClientProxy.LogFont.width(title);
                                if (w < w1) { w = w1; }
                                if (uvPos[0] - w1 / 2.0f < 1.5f) { w1 = uvPos[0] * 2.0f - 3.0f; } // left
                                if (uvPos[0] + w1 / 2.0f > mc.getWindow().getGuiScaledWidth() - 1.5f) {
                                    w1 = (w1 - (mc.getWindow().getGuiScaledWidth() - uvPos[0]) + 3.0f) * 2.0f;
                                } // right
                                if (l > w1 / -2.0f) { l = w1 / -2.0f; }
                            }
                            // down
                            if (uvPos[1] + yOffset + h > mc.getWindow().getGuiScaledHeight()) { yOffset = -h; }
                            matrixStack.translate(0.0d, yOffset, 0.0d);
                            // background
                            if (h > 0) {
                                l -= 2.0f;
                                w += 5.0f;
                                int color = 0x03202020;
                                graphics.fill((int) l, 0, (int) (l + w) - 1, 1, color);
                                graphics.fill((int) l, 1, (int) (l) + 1, (int) h - 1, color);
                                graphics.fill((int) (l + w) - 2, 1, (int) (l + w) - 1, (int) h - 1, color);
                                graphics.fill((int) l, (int) h - 1, (int) (l + w) - 1, (int) h, color);
                                color = 0x03303030;
                                graphics.fill((int) l + 1, 1, (int) (l + w) - 2, (int) h - 1, color);
                                // name
                                if (compassData.showQuestName) {
                                    ClientProxy.LogFont.draw(graphics, name, w0 / -2.0f, 0, 0x0FFFFFFF);
                                }
                                if (compassData.showTaskProgress) {
                                    ClientProxy.LogFont.draw(graphics, title, w1 / -2.0f, compassData.showQuestName ? 10 : 0, 0x0FFFFFFF);
                                }
                            }
                            matrixStack.popPose();
                        }
                        RenderSystem.enableDepthTest();
                        // Model
                        matrixStack.pushPose();
                        matrixStack.translate(0.0f, compassData.isFlat ? 6.5f : -16.0f * compassData.scale + 29.0f, 0.0f);
                        matrixStack.scale(scale, scale, scale);
                        float yaw = mc.player.getYRot() % 360;
                        if (yaw < 0.0f) { yaw += 360.0f; }
                        if (compassData.isFlat) {
                            float l = 0.0f;
                            if (uvPos[0] - 101 < 1.5f) { l = 101.5f - uvPos[0]; } // left
                            if (uvPos[0] + 101.0f > mc.getWindow().getGuiScaledWidth() - 1.5f) {
                                l = mc.getWindow().getGuiScaledWidth() - uvPos[0] - 101.5f;
                            } // right
                            matrixStack.translate(l, 0.0f, 0.0f);
                            // background
                            RenderSystem.enableBlend();
                            RenderSystem.defaultBlendFunc();
                            matrixStack.pushPose();
                            matrixStack.translate(-101.5f, -7.0f, 0.0f);
                            matrixStack.scale(0.5f, 0.5f, 0.5f);
                            graphics.blit(GuiBasic.INFO, 0, 0, 0, 74, 204, 28);
                            matrixStack.translate(204.0f, 0.0f, 0.0f);
                            graphics.blit(GuiBasic.INFO, 0, 0, 0, 102, 204, 28);
                            matrixStack.popPose();

                            graphics.enableScissor(uvPos[0] - (int) (99.0f * compassData.scale + l),
                                    uvPos[1] + (int) (-6.0f * compassData.scale + 7.0f),
                                    uvPos[0] + (int) (100.0f * compassData.scale + l),
                                    uvPos[1] + (int) (5.0f * compassData.scale + 6.5f));

                            // Dial
                            if (compassData.isShowDial()) {
                                matrixStack.pushPose();
                                matrixStack.translate(yaw * - 2.222222f, -6.0f, 0.0f);
                                //divisions
                                RenderSystem.enableBlend();
                                RenderSystem.defaultBlendFunc();
                                matrixStack.pushPose();
                                matrixStack.translate(-0.5f, 1.0f, 0.0f);
                                matrixStack.scale(0.5f, 0.5f, 0.5f);
                                for (int i = 0; i < 12; i++) {
                                    graphics.blit(GuiBasic.INFO, i * 200, 0, 29, 0, 4, 20);
                                }
                                matrixStack.popPose();
                                // sides
                                UtilYDE.FONT_HEADLINE.draw(graphics, "W", -203, 0, 0xC0C8F0DC);
                                UtilYDE.FONT_HEADLINE.draw(graphics, "S", -2.5F, 0, 0xC0BEF0F0);
                                UtilYDE.FONT_HEADLINE.draw(graphics, "E", 197.5F, 0, 0xC0F0F0BE);
                                UtilYDE.FONT_HEADLINE.draw(graphics, "N", 397, 0, 0xC0F0DCBE);
                                UtilYDE.FONT_HEADLINE.draw(graphics, "W", 596, 0, 0xC0C8F0BE);
                                UtilYDE.FONT_HEADLINE.draw(graphics, "S", 797.5F, 0, 0xC0BEF0F0);
                                matrixStack.popPose();
                            }
                            // Arrow
                            matrixStack.pushPose();
                            if (point != null) {
                                IRayTraceRotate angles = Util.instance.getAngles3D(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(), mc.player.getZ(), point[0], point[1], point[2]);
                                if (range == 1 || angles.getDistance() > range) {
                                    matrixStack.pushPose();
                                    RenderSystem.enableBlend();
                                    RenderSystem.defaultBlendFunc();
                                    RenderSystem.setShaderColor(FastColor.ARGB32.red(taskColor) / 255.0f,
                                            FastColor.ARGB32.green(taskColor) / 255.0f,
                                            FastColor.ARGB32.blue(taskColor) / 255.0f,
                                            ValueUtil.correctFloat(-0.0175f * (float) (angles.getDistance() - range) + 1.175f, 0.3f, 1.0f));
                                    float rot = (yaw - (float) angles.getYaw()) % 360;
                                    if (rot > 180) { rot -= 360.0f; }
                                    if (rot < -180) { rot += 360.0f; }
                                    if (rot > 44.0f) {
                                        matrixStack.translate(-99.0f, -3.5f, 0.0f);
                                        matrixStack.scale(0.5f, 0.5f, 0.5f);
                                        graphics.blit(GuiBasic.INFO, 0, 0, 15, 34, 10, 14);
                                    }
                                    else if (rot < -42.9f) {
                                        matrixStack.translate(95.0f, -3.5f, 0.0f);
                                        matrixStack.scale(0.5f, 0.5f, 0.5f);
                                        graphics.blit(GuiBasic.INFO, 0, 0, 15, 20, 10, 14);
                                    }
                                    else {
                                        matrixStack.translate(rot * - 2.222222f, -2.0f, 0.0f);
                                        matrixStack.scale(0.5f, 0.5f, 0.5f);
                                        if (mc.player.getY() < point[1] - range) {
                                            graphics.blit(GuiBasic.INFO, -2, -6, 15, 0, 14, 10);
                                        } // up
                                        else if (mc.player.getY() > point[1] + range) {
                                            graphics.blit(GuiBasic.INFO, -2, 4, 15, 10, 14, 10);
                                        } // dow
                                        else {
                                            graphics.blit(GuiBasic.INFO, 0, 0, 15, 48, 10, 10);
                                        }
                                    }
                                } // direction
                                else {
                                    long speed = 1500L;
                                    float a = 48.0f / (speed - (speed / 148.0f * 100.0f));
                                    float t = (System.currentTimeMillis() % (speed * 2L)) - speed;
                                    matrixStack.pushPose();
                                    RenderSystem.enableBlend();
                                    RenderSystem.defaultBlendFunc();
                                    RenderSystem.setShaderColor(FastColor.ARGB32.red(taskColor) / 255.0f,
                                            FastColor.ARGB32.green(taskColor) / 255.0f,
                                            FastColor.ARGB32.blue(taskColor) / 255.0f,
                                            0.5f);
                                    int w;
                                    if (t < 0) {
                                        w = ValueUtil.correctInt((int) (a * (t + speed)), 0, 48);
                                        // left
                                        matrixStack.pushPose();
                                        matrixStack.translate(-a * t - 147.5f, 1.5f, 0.0f);
                                        matrixStack.scale(1.0f, 0.5f, 1.0f);
                                        graphics.blit(GuiBasic.INFO, 0, 0, 81, 8, w, 8);
                                        matrixStack.popPose();
                                        // right
                                        matrixStack.pushPose();
                                        matrixStack.translate(a * t + 147.5f, 1.5f, 0.0f);
                                        matrixStack.scale(1.0f, 0.5f, 1.0f);
                                        graphics.blit(GuiBasic.INFO, -w, 0, 81 - w, 8, w, 8);
                                        matrixStack.popPose();
                                    } // down
                                    else {
                                        w = ValueUtil.correctInt((int) (-a * t + 148.0f), 0, 48);
                                        // left
                                        matrixStack.pushPose();
                                        matrixStack.translate(a * t - 147.5f, -5.5f, 0.0f);
                                        matrixStack.scale(1.0f, 0.5f, 1.0f);
                                        graphics.blit(GuiBasic.INFO, 0, 0, 33, 0, w, 8);
                                        matrixStack.popPose();
                                        // right
                                        matrixStack.pushPose();
                                        matrixStack.translate(-a * t + 99.5f, -5.5f, 0.0f);
                                        matrixStack.scale(1.0f, 0.5f, 1.0f);
                                        graphics.blit(GuiBasic.INFO, 48 - w, 0, 129 - w, 0, w, 8);
                                        matrixStack.popPose();
                                    } // up
                                } // found
                                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                                matrixStack.popPose();
                            }
                            matrixStack.popPose();
                            graphics.disableScissor();
                        }
                        else {
                            Lighting.setupForFlatItems();
                            matrixStack.mulPose(Axis.XP.rotationDegrees(-45.0f + compassData.incline));
                            if (compassData.rot != 0.0f) { matrixStack.mulPose(Axis.YP.rotationDegrees(compassData.rot)); }
                            // Body
                            matrixStack.pushPose();
                            if (COMPASS_BODY == null) { COMPASS_BODY = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
                                    Collections.singletonList("body"), GuiBasic.TEXTURES_COMPASS,  false, 0); }
                            ModelBuffer.render(COMPASS_BODY, matrixStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                            matrixStack.popPose();
                            // Dial
                            if (compassData.isShowDial()) {
                                matrixStack.pushPose();
                                matrixStack.mulPose(Axis.YP.rotationDegrees(mc.player.getYRot()));
                                if (COMPASS_DIAL == null) { COMPASS_DIAL = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
                                        Collections.singletonList("dial"), GuiBasic.TEXTURES_COMPASS,  false, 0); }
                                ModelBuffer.render(COMPASS_DIAL, matrixStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                                matrixStack.popPose();
                            }
                            if (point != null) {
                                IRayTraceRotate angles = Util.instance.getAngles3D(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(), mc.player.getZ(), point[0], point[1], point[2]);
                                if (range == 1 || angles.getDistance() > range) {
                                    // Arrow_0
                                    matrixStack.pushPose();
                                    matrixStack.mulPose(Axis.YP.rotationDegrees(yaw - (float) angles.getYaw()));
                                    if (COMPASS_ARROW_0 == null) { COMPASS_ARROW_0 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
                                            Collections.singletonList("arrow_0"), GuiBasic.TEXTURES_COMPASS,  false, 0); }
                                    ModelBuffer.render(COMPASS_ARROW_0, matrixStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                                    matrixStack.popPose();
                                    // Arrow_1 upper
                                    double yP;
                                    yP = -0.25d * (mc.player.getY() - point[1]) / (double) range;
                                    matrixStack.pushPose();
                                    if (yP >= -0.25d && yP <= 0.25d) {
                                        matrixStack.translate(0.0d, yP, 0.0d);
                                    }
                                    else {
                                        if (yP > 0.25d) {
                                            matrixStack.translate(0.0d, 0.275d, 0.0d);
                                        } else if (yP < -0.25d) {
                                            matrixStack.translate(0.0d, -0.275d, 0.0d);
                                        }
                                        double t = System.currentTimeMillis() % 1000.0d;
                                        double f0 = t < 500.0d ? -0.025d + 0.05d * (t % 500.0d) / 500.0d
                                                : 0.025d - 0.05d * (t % 500.0d) / 500.0d;
                                        matrixStack.translate(0.0d, f0, 0.0d);
                                    }
                                    if (COMPASS_ARROW_1 == null) { COMPASS_ARROW_1 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
                                            Collections.singletonList("arrow_1"), GuiBasic.TEXTURES_COMPASS,  false, 0); }
                                    ModelBuffer.render(COMPASS_ARROW_1, matrixStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                                    matrixStack.popPose();
                                    // Arrow_2
                                    matrixStack.pushPose();
                                    if (yP > 0.25d) {
                                        if (COMPASS_ARROW_21 == null) { COMPASS_ARROW_21 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
                                                Collections.singletonList("arrow_21"), GuiBasic.TEXTURES_COMPASS,  false, 0); }
                                        ModelBuffer.render(COMPASS_ARROW_21, matrixStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                                    }
                                    else if (yP < -0.25d) {
                                        if (COMPASS_ARROW_22 == null) { COMPASS_ARROW_22 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
                                                Collections.singletonList("arrow_22"), GuiBasic.TEXTURES_COMPASS,  false, 0); }
                                        ModelBuffer.render(COMPASS_ARROW_22, matrixStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                                    }
                                    else {
                                        if (COMPASS_ARROW_20 == null) { COMPASS_ARROW_20 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
                                                Collections.singletonList("arrow_20"), GuiBasic.TEXTURES_COMPASS,  false, 0); }
                                        ModelBuffer.render(COMPASS_ARROW_20, matrixStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                                    }
                                    matrixStack.popPose();
                                } // direction
                                else {
                                    // Arrow_3
                                    matrixStack.pushPose();
                                    float t = System.currentTimeMillis() % 4000L;
                                    float f0 = t < 2000.0f ? -0.00033f * t + 1.0f : 0.00033f * t - 0.30033f;
                                    matrixStack.scale(f0, f0, f0);
                                    if (COMPASS_ARROW_3 == null) { COMPASS_ARROW_3 = ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
                                            Collections.singletonList("arrow_3"), GuiBasic.TEXTURES_COMPASS,  false, 0); }
                                    ModelBuffer.render(COMPASS_ARROW_3, matrixStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                                    matrixStack.popPose();
                                } // found
                            }
                            if (taskType >= 0 && taskType <= EnumQuestTask.values().length) {
                                matrixStack.pushPose();
                                if (!COMPASS_FASE.containsKey(taskType)) {
                                    Map<String, ResourceLocation> m = new HashMap<>();
                                    m.put("#material", new ResourceLocation(CustomNpcs.MODID, "util/compass"));
                                    m.put("#task", new ResourceLocation(CustomNpcs.MODID, "util/task_" + taskType));
                                    COMPASS_FASE.put(taskType, ModelBuffer.getParameterizedModel(RESOURCE_COMPASS,
                                            Collections.singletonList("fase"), m,  false, 0));
                                }
                                ModelBuffer.render(COMPASS_FASE.get(taskType), matrixStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                                matrixStack.popPose();
                            }
                        }
                        matrixStack.popPose();
                        matrixStack.popPose();
                    }
                }
            }
        }
    }

    /** HUD: NBT Book Info Overlay */
    public static void renderNbtBookOverlay(ForgeGui ignoredGui, GuiGraphics graphics, float ignoredPartialTick, int ignoredScreenWidth, int ignoredScreenHeight) {
        mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null &&
                (mc.screen == null || mc.screen instanceof ChatScreen || mc.screen instanceof GuiLog) &&
                (mc.player.getMainHandItem().getItem() instanceof ItemNbtBook || mc.player.getOffhandItem().getItem() instanceof ItemNbtBook)) {
            PlayerData playerData = PlayerData.get(mc.player);
            PoseStack matrixStack = graphics.pose();
            double distance = playerData.game.renderDistance;
            Vec3 vec3d = mc.player.getEyePosition(1.0f);
            Vec3 vec3d1 = mc.player.getLookAngle();
            Vec3 vec3d2 = vec3d.add(vec3d1.x * distance, vec3d1.y * distance, vec3d1.z * distance);
            BlockHitResult result = mc.level.clip(new ClipContext(vec3d, vec3d2, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
            MutableComponent rayName;
            MutableComponent rayTitle = Component.empty();
            double lH = mc.font.lineHeight + 1.0d;
            ItemStack st;
            if (result.getType() != HitResult.Type.MISS) {
                BlockPos blockPos = result.getBlockPos();
                Entity entity = Util.instance.getLookEntity(mc.player, distance, false);
                st = null;
                BlockState state = null;
                double dist;
                MutableComponent rayPos = Component.empty();
                if (entity != null) {
                    dist = Math.round(mc.player.distanceTo(entity) * 10.0d) / 10.0d;
                    ResourceLocation res = EntityType.getKey(entity.getType());
                    rayName = Component.empty()
                            .append(Component.literal(" [" + res + "] ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(entity.getName().getString()).withStyle(ChatFormatting.RESET));
                    rayTitle = Component.literal(entity.getClass().getSimpleName()).withStyle(ChatFormatting.YELLOW);
                    rayPos = Component.empty()
                            .append(Component.literal("[X:").withStyle(ChatFormatting.AQUA))
                            .append(Component.literal("" + (Math.round(entity.getX() * 10.0d) / 10.0d)).withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(", Y:").withStyle(ChatFormatting.AQUA))
                            .append(Component.literal("" + (Math.round(entity.getY() * 10.0d) / 10.0d)).withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(", Z:").withStyle(ChatFormatting.AQUA))
                            .append(Component.literal("" + (Math.round(entity.getZ() * 10.0d) / 10.0d)).withStyle(ChatFormatting.GOLD))
                            .append(Component.literal("]").withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" " + dist).withStyle(ChatFormatting.DARK_AQUA));
                }
                else {
                    float f = (float) (mc.player.getX() - blockPos.getX() + 0.5d);
                    float f1 = (float) (mc.player.getY() - blockPos.getY() + 0.5d);
                    float f2 = (float) (mc.player.getZ() - blockPos.getZ() + 0.5d);
                    dist = Math.round(Math.sqrt(f * f + f1 * f1 + f2 * f2) * 10.0d) / 10.0d;
                    if (dist > playerData.game.renderDistance && !mc.player.getOffhandItem().isEmpty()
                            && !(mc.player.getOffhandItem().getItem() instanceof ItemNbtBook)) {
                        st = mc.player.getOffhandItem();
                        rayName = Component.literal(st.getHoverName().getString());
                    } else {
                        state = mc.level.getBlockState(blockPos);
                        if (dist > playerData.game.renderDistance) {
                            result = mc.level.clip(new ClipContext(vec3d, vec3d2, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
                            if (result.getType() != HitResult.Type.MISS) {
                                BlockState tempState = mc.level.getBlockState(result.getBlockPos());
                                if (!(tempState.getBlock() instanceof AirBlock)) {
                                    state = tempState;
                                }
                            }
                        }
                        rayName = Component.empty()
                                .append(Component.literal(" [" + ForgeRegistries.BLOCKS.getKey(state.getBlock()) + "] ").withStyle(ChatFormatting.GRAY))
                                .append(state.getBlock().getName().withStyle(ChatFormatting.RESET));
                        String stateText = state.toString();
                        if (stateText.contains("[")) {
                            stateText = stateText.substring(stateText.indexOf("["));
                        } else {
                            stateText = "[]";
                        }
                        rayTitle = Component.empty()
                                .append(Component.literal(state.getBlock().getClass().getSimpleName()).withStyle(ChatFormatting.RESET))
                                .append(Component.literal("; state: ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(stateText).withStyle(ChatFormatting.YELLOW));
                        if (state.getBlock() instanceof BaseEntityBlock) {
                            rayTitle.append(Component.literal("; ").withStyle(ChatFormatting.GRAY))
                                    .append(Component.literal("hasTile").withStyle(ChatFormatting.DARK_AQUA));
                        }
                        rayPos = Component.empty()
                                .append(Component.literal(" [X:").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal("" + blockPos.getX()).withStyle(ChatFormatting.GOLD))
                                .append(Component.literal(", Y:").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal("" + blockPos.getY()).withStyle(ChatFormatting.GOLD))
                                .append(Component.literal(", Z:").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal("" + blockPos.getZ()).withStyle(ChatFormatting.GOLD))
                                .append(Component.literal("]").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(" " + dist).withStyle(ChatFormatting.DARK_AQUA));
                    }
                }
                matrixStack.pushPose();
                if (entity instanceof LivingEntity living) {
                    matrixStack.pushPose();
                    matrixStack.translate(8.0d + playerData.overlay.getWindowSize().getWidth() / 2.0d,
                            playerData.overlay.getWindowSize().getHeight() - 45.0d - 3.5d * lH,
                            -200.0d);
                    renderEntityForBook(graphics, living);
                    matrixStack.popPose();
                }
                else if (state != null) {
                    st = new ItemStack(state.getBlock().asItem(), 1);
                }
                if (st != null) {
                    matrixStack.pushPose();
                    matrixStack.translate(playerData.overlay.getWindowSize().getWidth() / 2.0d,
                            playerData.overlay.getWindowSize().getHeight() - 45.0d - 4.5d * lH,
                            0.0d);
                    graphics.renderItem(st, 0, 0);
                    graphics.renderItemDecorations(mc.font, st, 0, 0);
                    matrixStack.popPose();
                }
                matrixStack.translate(
                        (playerData.overlay.getWindowSize().getWidth() - (double) mc.font.width(rayName)) / 2.0d,
                        playerData.overlay.getWindowSize().getHeight() - 45.0d - 3.0d * lH,
                        0.0d);
                graphics.drawString(mc.font, rayName, 0, 0, 0xFFFFFF);
                matrixStack.popPose();

                matrixStack.pushPose();
                matrixStack.translate(
                        (playerData.overlay.getWindowSize().getWidth() - (double) mc.font.width(rayTitle)) / 2.0d,
                        playerData.overlay.getWindowSize().getHeight() - 45.0d - 2.0d * lH,
                        0.0d);
                graphics.drawString(mc.font, rayTitle, 0, 0, 0xFFFFFF);
                matrixStack.popPose();

                matrixStack.pushPose();
                matrixStack.translate(
                        (playerData.overlay.getWindowSize().getWidth() - (double) mc.font.width(rayPos)) / 2.0d,
                        playerData.overlay.getWindowSize().getHeight() - 45.0d - lH,
                        0.0d);
                graphics.drawString(mc.font, rayPos, 0, 0, 0xFFFFFF);
                matrixStack.popPose();

            }
            else if (!mc.player.getOffhandItem().isEmpty()) {
                st = mc.player.getOffhandItem();
                rayName = Component.empty()
                        .append(Component.literal(" [" + ForgeRegistries.ITEMS.getKey(st.getItem()) + "] ").withStyle(ChatFormatting.GRAY))
                        .append(st.getItem().getName(st));

                rayTitle = Component.empty()
                        .append(Component.literal(st.getItem().getClass().getSimpleName()).withStyle(ChatFormatting.RESET));
                if (st.hasTag()) {
                    rayTitle.append(Component.literal("; ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("hasTags").withStyle(ChatFormatting.DARK_AQUA));
                }

                matrixStack.pushPose();
                matrixStack.translate(
                        (playerData.overlay.getWindowSize().getWidth() - (double) mc.font.width(rayName)) / 2.0d,
                        playerData.overlay.getWindowSize().getHeight() - 45.0d - 3.0d * lH,
                        0.0d);
                graphics.drawString(mc.font, rayName, 0, 0, 0xFFFFFF);
                matrixStack.popPose();

                matrixStack.pushPose();
                matrixStack.translate(
                        (playerData.overlay.getWindowSize().getWidth() - (double) mc.font.width(rayTitle)) / 2.0d,
                        playerData.overlay.getWindowSize().getHeight() - 45.0d - 2.0d * lH,
                        0.0d);
                graphics.drawString(mc.font, rayTitle, 0, 0, 0xFFFFFF);
                matrixStack.popPose();
            }
        }
    }

    private static final class FakeBlockAndTintGetter implements BlockAndTintGetter {

        private final BlockState centerState;
        private final FluidState centerFluid;
        private final BlockPos centerPos;
        private final Holder<Biome> biome;
        private final LevelLightEngine lightEngine;

        FakeBlockAndTintGetter(BlockState centerStateIn, BlockPos centerPosIn, @Nonnull Level realLevel) {
            centerState = centerStateIn;
            centerFluid = centerState.getFluidState();
            centerPos = centerPosIn.immutable();
            biome = realLevel.getBiome(centerPos);
            lightEngine = new LevelLightEngine(new LightChunkGetter() {
                @Override
                public @Nullable LightChunk getChunkForLighting(int i, int i1) { return null; }
                @Override
                public @Nonnull BlockGetter getLevel() { return realLevel; }
            }, false, false);
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(@Nonnull BlockPos pos) { return null; }

        @Override
        public @Nonnull BlockState getBlockState(@Nonnull BlockPos pos) { return pos.equals(centerPos) ? centerState : Blocks.AIR.defaultBlockState(); }

        @Override
        public @Nonnull FluidState getFluidState(@Nonnull BlockPos pos) { return pos.equals(centerPos) ? centerFluid : Fluids.EMPTY.defaultFluidState(); }

        @Override
        public float getShade(@Nonnull Direction direction, boolean shade) { return 1.0F; }

        @Override
        public @Nonnull LevelLightEngine getLightEngine() { return lightEngine; }

        @Override
        public int getBlockTint(@Nonnull BlockPos pos, @Nonnull ColorResolver colorResolver) {
            if (biome != null) { return colorResolver.getColor(biome.value(), pos.getX(), pos.getZ()); }
            if (colorResolver == BiomeColors.WATER_COLOR_RESOLVER) return 0x3F76E4;
            if (colorResolver == BiomeColors.GRASS_COLOR_RESOLVER) return 0x8CBD57;
            if (colorResolver == BiomeColors.FOLIAGE_COLOR_RESOLVER) return 0x59A315;
            return 0xFFFFFF;
        }

        @Override
        public int getHeight() { return mc.level != null ? mc.level.getHeight() : 384; }

        @Override
        public int getMinBuildHeight() { return mc.level != null ? mc.level.getMinBuildHeight() : -64; }

        @Override
        public int getMaxBuildHeight() { return getMinBuildHeight() + getHeight(); }

        @Override
        public int getLightEmission(@Nonnull BlockPos pos) { return centerState.getLightEmission(this, pos); }

        @Override
        public int getMaxLightLevel() { return mc.level != null ? mc.level.getMaxLightLevel() : 15; }

    }

}
