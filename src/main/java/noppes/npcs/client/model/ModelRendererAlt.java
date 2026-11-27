package noppes.npcs.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.client.model.animation.AddedPartConfig;
import noppes.npcs.constants.EnumParts;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("unused")
public class ModelRendererAlt {

    // Data
    public EnumParts part;
    public int partId;
    public int parentPartId = -1;

    // Box geometry
    public float x;
    public float y;
    public float z;
    public float xe;
    public float ye0;
    public float ye1;
    public float ye2;
    public float ze;
    public float dx;
    public float dy0;
    public float dy1;
    public float dy2;
    public float dz;

    // From ModelPart (stored directly since ModelPart is final)
    public float rotationPointX, rotationPointY, rotationPointZ;
    public float rotateAngleX, rotateAngleY, rotateAngleZ;
    public boolean mirror;
    public boolean showModel = true;
    public int u = 0, v = 0;
    public int textureWidth = 64, textureHeight = 64;

    // Custom rotation (extra joints)
    public float rotateAngleX1 = 0.0f;
    public float rotateAngleY1 = 0.0f;

    // Scale and offset (custom)
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    public float scaleZ = 1.0f;
    public float offsetAnimX = 0.0f;
    public float offsetAnimY = 0.0f;
    public float offsetAnimZ = 0.0f;

    public final boolean baseNormal;
    public boolean smallArms;
    public boolean isAnimPart;
    public boolean isNormal = false;
    public boolean isArmor = false;

    // Custom
    public ResourceLocation textureLocation = null;
    public ResourceLocation objLocationUp = null;
    public ResourceLocation objLocationDown = null;

    // Color tinting
    private final float r = 1.0f;
    private final float g = 1.0f;
    private final float b = 1.0f;
    private final float al = 0.0f;

    private final float[] baseRotationPoint;

    // Vertex and quad data for custom rendering
    //private final Map<Integer, PositionTextureVertex> vs = new HashMap<>();
    //private final Map<Integer, TexturedQuad> quads = new HashMap<>();
    //private final Vec2f[] tvs = new Vec2f[8];

    private final int displayList = -1;

    public ModelRendererAlt(EnumParts enumPart, int textureU, int textureV, boolean normal) {
        part = enumPart;
        partId = enumPart.ordinal();
        u = textureU;
        v = textureV;
        textureHeight = 32;
        baseNormal = normal;
        isNormal = normal;
        baseRotationPoint = new float[] { rotationPointX, rotationPointY, rotationPointZ };
    }

    public ModelRendererAlt(AddedPartConfig addedPartConfig) {
        part = EnumParts.CUSTOM;
        partId = addedPartConfig.id;
        parentPartId = addedPartConfig.parentPart;
        u = addedPartConfig.textureU;
        v = addedPartConfig.textureV;
        textureHeight = 32;
        baseNormal = addedPartConfig.isNormal;
        isNormal = addedPartConfig.isNormal;
        textureLocation = addedPartConfig.location;
        baseRotationPoint = new float[] { rotationPointX, rotationPointY, rotationPointZ };
    }

    public ModelRendererAlt() {
        baseNormal = true;
        baseRotationPoint = new float[] { rotationPointX, rotationPointY, rotationPointZ };
    }

    public void setBox(float x, float y, float z, float dx, float dy0, float dy1, float dy2, float dz, float wear) {
        xe = x + dx + wear;
        ye0 = y + dy0;
        ye1 = y + dy0 + dy1;
        ye2 = y + dy0 + dy1 + dy2 + wear;
        ze = z + dz + wear;
        this.x = x - wear;
        this.y = y - wear;
        this.z = z - wear;
        this.dx = dx;
        this.dz = dz;
        this.dy0 = dy0;
        this.dy1 = this.dy0 + dy1;
        this.dy2 = this.dy1 + dy2;
        if (mirror) {
            float f3 = xe;
            xe = x;
            this.x = f3;
        }
    }

    /*
    public void translateAndRotate(PoseStack pose, float partialTicks) {
        Matrix4f matrix4f = pose.last().pose();
        Matrix3f matrix3f = pose.last().normal();

        float ox = offsetAnimX;
        if (smallArms) {
            ox += part == EnumParts.ARM_LEFT ? -0.020833f : 0.020833f;
        }
        matrix4f.translate(ox, offsetAnimY, offsetAnimZ);

        matrix4f.translate(rotationPointX, rotationPointY + offsetAnimY, rotationPointZ + offsetAnimZ);

        float f = rotateAngleZ;
        if (f != 0.0F) {
            Matrix4f.mul(matrix4f, Axis.ZP.rotation(f), matrix4f);
        }
        f = rotateAngleY;
        if (f != 0.0F) {
            Matrix4f.mul(matrix4f, Axis.YP.rotation(f), matrix4f);
        }
        f = rotateAngleX;
        if (f != 0.0F) {
            Matrix4f.mul(matrix4f, Axis.XP.rotation(f), matrix4f);
        }

        if (scaleX != 1.0f || scaleY != 1.0f || scaleZ != 1.0f || smallArms) {
            float sx = scaleX;
            if (smallArms) {
                sx *= dx / (dx + 1.0f);
            }
            matrix4f.scale(sx, scaleY, scaleZ);
        }
    }

    public void render(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                       float red, float green, float blue, float alpha, float scale) {
        if (!showModel) {
            return;
        }

        poseStack.pushPose();
        translateAndRotate(poseStack, 0.0f);
    }

    ///Overload for preview rendering with scale only
    public void render(float scale) {
        if (!showModel) {
            return;
        }
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        translateAndRotate(poseStack, 0.0f);
        poseStack.popPose();
    }

        // Tint
        float finalR = r;
        float finalG = g;
        float finalB = b;
        float finalA = al > 0.0f ? al : 1.0f;

        if (textureLocation != null) {
            RenderSystem.setShaderTexture(0, textureLocation);
        }

        // Render the box
        if (!notOBJModel()) {
            objDraw(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        } else {
            clearData();
            if (isNormal || !CustomNpcs.ShowJoints || (rotateAngleX1 == 0.0f && rotateAngleY1 == 0.0f)) {
                simpleDraw(poseStack, vertexConsumer, packedLight, packedOverlay, scale);
            } else {
                rotateTop();
                drawJoint(poseStack, vertexConsumer, packedLight, packedOverlay, scale);
            }
        }

        poseStack.popPose();
    }

    private void clearData() {
        vs.clear();
        quads.clear();
    }

    private void rotateTop() {
        if (rotateAngleY1 == 0.0f) {
            if (!baseNormal) {
                tvs[0] = new Vec2f(x, z);
                tvs[1] = new Vec2f(xe, z);
                tvs[2] = new Vec2f(xe, ze);
                tvs[3] = new Vec2f(x, ze);
                tvs[4] = tvs[0];
                tvs[5] = tvs[1];
                tvs[6] = tvs[2];
                tvs[7] = tvs[3];
                baseNormal = true;
            }
        } else {
            double limit = Math.PI / 2.0;
            float clamped = Mth.clamp(rotateAngleY1, (float) -limit, (float) limit);
            double cos = Math.cos(-clamped);
            double sin = Math.sin(-clamped);
            tvs[0] = rotate(new Vec2f(x + (xe - x) / 2, z + (ze - z) / 2), new Vec2f(x, z), cos, sin);
            tvs[1] = rotate(new Vec2f(x + (xe - x) / 2, z + (ze - z) / 2), new Vec2f(xe, z), cos, sin);
            tvs[2] = rotate(new Vec2f(x + (xe - x) / 2, z + (ze - z) / 2), new Vec2f(xe, ze), cos, sin);
            tvs[3] = rotate(new Vec2f(x + (xe - x) / 2, z + (ze - z) / 2), new Vec2f(x, ze), cos, sin);
            tvs[4] = tvs[0];
            tvs[5] = tvs[1];
            tvs[6] = tvs[2];
            tvs[7] = tvs[3];
            baseNormal = false;
        }
    }

    private Vec2f rotate(Vec2f center, Vec2f vertex, double cos, double sin) {
        float y = (float) (center.y + (vertex.y - center.y) * cos - (vertex.x - center.x) * sin);
        float x = (float) (center.x + (vertex.y - center.y) * sin + (vertex.x - center.x) * cos);
        return new Vec2f(x, y);
    }

    private void objDraw(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                         float baseRed, float baseGreen, float baseBlue, float baseAlpha) {
        // OBJ model rendering with proper offsets
        float offsetX = 0.0f;
        float offsetY = 0.0f;
        float offsetZ = 0.0f;

        switch (part) {
            case HEAD:
            case BODY:
                offsetY = 1.5f;
                break;
            case ARM_RIGHT:
                offsetX = 0.3175f;
                offsetY = 1.375f;
                break;
            case ARM_LEFT:
                offsetX = -0.3175f;
                offsetY = 1.375f;
                break;
            case LEG_RIGHT:
                offsetX = 0.125f;
                offsetY = 0.75f;
                break;
            case LEG_LEFT:
                offsetX = -0.115f;
                offsetY = 0.75f;
                break;
        }

        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, offsetZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));

        if (displayList > 0) {
            // Display list rendering would go here for OBJ models
        }

        poseStack.popPose();
    }

    private void simpleDraw(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float scale) {
        if (displayList > 0) {
            // Legacy display list support
            return;
        }

        // Build vertices
        vs.put(0, new PositionTextureVertex(x, y, z, 0.0F, 0.0F));
        vs.put(1, new PositionTextureVertex(xe, y, z, 0.0F, 8.0F));
        vs.put(2, new PositionTextureVertex(xe, ye2, z, 8.0F, 8.0F));
        vs.put(3, new PositionTextureVertex(x, ye2, z, 8.0F, 0.0F));
        vs.put(4, new PositionTextureVertex(x, y, ze, 0.0F, 0.0F));
        vs.put(5, new PositionTextureVertex(xe, y, ze, 0.0F, 8.0F));
        vs.put(6, new PositionTextureVertex(xe, ye2, ze, 8.0F, 8.0F));
        vs.put(7, new PositionTextureVertex(x, ye2, ze, 8.0F, 0.0F));

        // 6 faces: right, left, up, down, front, back
        setQuad(0, vs.get(5), vs.get(1), vs.get(2), vs.get(6), u + dz + dx, v + dz, u + dz + dx + dz, v + dz + dy2);
        setQuad(1, vs.get(0), vs.get(4), vs.get(7), vs.get(3), u, v + dz, u + dz, v + dz + dy2);
        setQuad(2, vs.get(5), vs.get(4), vs.get(0), vs.get(1), u + dz, v, u + dz + dx, v + dz);
        setQuad(3, vs.get(2), vs.get(3), vs.get(7), vs.get(6), u + dz + dx, v + dz, u + dz + dx + dx, v);
        setQuad(4, vs.get(1), vs.get(0), vs.get(3), vs.get(2), u + dz, v + dz, u + dz + dx, v + dz + dy2);
        setQuad(5, vs.get(4), vs.get(5), vs.get(6), vs.get(7), u + dz + dx + dz, v + dz, u + dz + dx + dz + dx, v + dz + dy2);

        draw(poseStack, vertexConsumer, packedLight, packedOverlay, scale);
    }

    private void drawJoint(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float scale) {
        boolean counterClockwise = rotateAngleY1 <= 0.0f;
        if (isArmor && part != EnumParts.ARM_RIGHT && part != EnumParts.LEG_RIGHT) {
            counterClockwise = !counterClockwise;
        }

        // Build base vertices
        vs.put(0, new PositionTextureVertex(x, y, z, 0.0F, 0.0F));
        vs.put(1, new PositionTextureVertex(xe, y, z, 0.0F, 8.0F));
        vs.put(2, new PositionTextureVertex(xe, y, ze, 8.0F, 8.0F));
        vs.put(3, new PositionTextureVertex(x, y, ze, 8.0F, 0.0F));

        int i = 0;
        // Top face
        setQuad(i++, vs.get(2), vs.get(3), vs.get(0), vs.get(1), u + dz, v, u + dz + dx, v + dz);

        // Joint calculation
        float cos0 = (float) Math.cos(-rotateAngleX1 / 3.0f);
        float sin0 = (float) Math.sin(-rotateAngleX1 / 3.0f);
        float cos1 = (float) Math.cos(-rotateAngleX1 / 1.5f);
        float sin1 = (float) Math.sin(-rotateAngleX1 / 1.5f);
        float cos2 = (float) Math.cos(-rotateAngleX1);
        float sin2 = (float) Math.sin(-rotateAngleX1);
        float tan = (float) Math.tan(-rotateAngleX1 / 2.0f);
        if (rotateAngleX1 <= -3.1415f) {
            tan = 0.0f;
        }

        Vec2f cr = new Vec2f(ye0, -rotateAngleX1 * ze / (float) -Math.PI);

        if (rotateAngleX1 < 0.0f) {
            Vec2f g0 = new Vec2f(cr.x + (ze - cr.y) * sin0 + (ye0 - cr.x) * cos0,
                    cr.y + (ze - cr.y) * cos0 - (ye0 - cr.x) * sin0);
            Vec2f g1 = new Vec2f(cr.x + (ze - cr.y) * sin1 + (ye0 - cr.x) * cos1,
                    cr.y + (ze - cr.y) * cos1 - (ye0 - cr.x) * sin1);
            Vec2f g2 = new Vec2f(cr.x + (ze - cr.y) * sin2 + (ye0 - cr.x) * cos2,
                    cr.y + (ze - cr.y) * cos2 - (ye0 - cr.x) * sin2);

            float t = (z - cr.y) * tan;
            Vec2f g3 = new Vec2f(ye0 + t, z);

            float d0 = (float) Math.hypot(g0.x - g1.x, g0.y - g1.y);
            float n0 = dy0 / (dy0 + d0 * 1.5f) * d0 * 1.5f;
            float n1 = (dy1 - dy0) / ((dy1 - dy0) + d0 * 1.5f) * d0 * 1.5f;
            float dy00 = dy0 - n0;
            float dy01 = dy00 + n0 / 1.5f;
            float dy02 = dy0 + n1 / 3.0f;
            float dy03 = (dy0 + n1) * 0.85f;

            vs.put(4, new PositionTextureVertex(x, g3.x, z, 0.0F, 0.0F));
            vs.put(5, new PositionTextureVertex(xe, g3.x, z, 0.0F, 8.0F));
            vs.put(6, new PositionTextureVertex(xe, ye0, ze, 8.0F, 8.0F));
            vs.put(7, new PositionTextureVertex(x, ye0, ze, 8.0F, 0.0F));

            setQuad(i++, vs.get(1), vs.get(0), vs.get(4), vs.get(5), u + dz, v + dz, u + dz + dx, v + dz + dy0);
            setQuad(i++, vs.get(3), vs.get(2), vs.get(6), vs.get(7), u + 2 * dz + dx, v + dz, u + 2 * (dz + dx), v + dz + dy00);
            setQuad(i++, vs.get(1), vs.get(2), vs.get(6), vs.get(5), u + dz + dx, v + dz, u + 2 * dz + dx, v + dz + dy00, u + dz + dx, v + dz + dy0);
            setQuad(i++, vs.get(0), vs.get(3), vs.get(7), vs.get(4), u + dz, v + dz, u, v + dz, u, v + dz + dy00, u + dz, v + dz + dy0);

            // More joint levels would follow... (simplified for brevity)
        }

        draw(poseStack, vertexConsumer, packedLight, packedOverlay, scale);
    }

    private void draw(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float scale) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        for (int j : quads.keySet()) {
            TexturedQuad tq = quads.get(j);
            if (tq == null) {
                continue;
            }
            tq.draw(bufferbuilder, scale);
        }

        BufferSpec bufferSpec = new BufferSpec();
        BufferBuilder finishedbuffer = tesselator.end();
        if (finishedbuffer != null) {
            vertexConsumer.sendBatch(finishedbuffer);
        }
    }

    private void setQuad(int i, PositionTextureVertex ptv0, PositionTextureVertex ptv1,
                         PositionTextureVertex ptv2, PositionTextureVertex ptv3,
                         float u1, float v1, float u2, float v2) {
        quads.put(i, new TexturedQuad(
                new PositionTextureVertex[]{ptv0, ptv1, ptv2, ptv3},
                u1 / textureWidth, v1 / textureHeight,
                u2 / textureWidth, v2 / textureHeight,
                mirror));
    }

    private void setQuad(int i, PositionTextureVertex ptv0, PositionTextureVertex ptv1,
                         PositionTextureVertex ptv2, PositionTextureVertex ptv3,
                         float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
        quads.put(i, new TexturedQuad(
                new PositionTextureVertex[]{ptv0, ptv1, ptv2, ptv3},
                u0 / textureWidth, v0 / textureHeight,
                u1 / textureWidth, v1 / textureHeight,
                u2 / textureWidth, v2 / textureHeight,
                u3 / textureWidth, v3 / textureHeight,
                mirror));
    }

    public void setIsNormal(boolean bo) {
        isNormal = bo;
    }

    public void copyModelAngles(ModelRendererAlt source) {
        rotateAngleX = source.rotateAngleX;
        rotateAngleY = source.rotateAngleY;
        rotateAngleZ = source.rotateAngleZ;
        rotationPointX = source.rotationPointX;
        rotationPointY = source.rotationPointY;
        rotationPointZ = source.rotationPointZ;
        rotateAngleX1 = source.rotateAngleX1;
        rotateAngleY1 = source.rotateAngleY1;
        scaleX = source.scaleX;
        scaleY = source.scaleY;
        scaleZ = source.scaleZ;
        offsetAnimX = source.offsetAnimX;
        offsetAnimY = source.offsetAnimY;
        offsetAnimZ = source.offsetAnimZ;
    }

    public void checkBacklightColor(float red, float green, float blue) {
        r = red;
        g = green;
        b = blue;
        al = 1.0f;
        if (ModelNpcAlt.editAnimDataSelect.isNPC) {
            if (ModelNpcAlt.editAnimDataSelect.part == part.ordinal()) {
                r = ModelNpcAlt.editAnimDataSelect.red;
                g = ModelNpcAlt.editAnimDataSelect.green;
                b = ModelNpcAlt.editAnimDataSelect.blue;
            } else {
                al = ModelNpcAlt.editAnimDataSelect.alpha;
            }
        }
    }

    public void clearOBJ() {
        objLocationUp = null;
        objLocationDown = null;
    }

    public void setOBJModel(net.minecraft.world.item.ItemStack stack, EnumParts part) {
        if (stack == null || !(stack.getItem() instanceof noppes.npcs.items.custom.CustomArmor)) {
            objLocationUp = null;
            objLocationDown = null;
            return;
        }
        noppes.npcs.items.custom.CustomArmor armor = (noppes.npcs.items.custom.CustomArmor) stack.getItem();
        if (armor.objModel == null) {
            objLocationUp = null;
            objLocationDown = null;
            return;
        }
        // OBJ model loading would be handled by ModelBuffer
        objLocationUp = armor.objModel;
    }

    public boolean notOBJModel() {
        return objLocationUp == null && objLocationDown == null;
    }

    public void setBaseData(noppes.npcs.client.model.animation.ModelPartConfig config) {
        if (config == null) {
            offsetAnimX = 0.0f;
            offsetAnimY = 0.0f;
            offsetAnimZ = 0.0f;
            scaleX = 1.0f;
            scaleY = 1.0f;
            scaleZ = 1.0f;
            return;
        }
        offsetAnimX = config.transX;
        offsetAnimY = config.transY;
        offsetAnimZ = config.transZ;
        scaleX = config.scaleX;
        scaleY = config.scaleY;
        scaleZ = config.scaleZ;
    }

    public void putAnimation(DataAnimation animation) {
        AnimationFrameConfig preFrame = animation.getPreFrame();
        if (preFrame == null || !preFrame.parts.containsKey(partId)) {
            return;
        }
        preFrame.parts.get(partId).show = showModel;

        preFrame.parts.get(partId).rotation[0] = rotateAngleX;
        preFrame.parts.get(partId).rotation[1] = rotateAngleY;
        preFrame.parts.get(partId).rotation[2] = rotateAngleZ;
        preFrame.parts.get(partId).rotation[3] = rotateAngleX1;
        preFrame.parts.get(partId).rotation[4] = rotateAngleY1;

        preFrame.parts.get(partId).offset[0] = offsetAnimX;
        preFrame.parts.get(partId).offset[1] = offsetAnimY;
        preFrame.parts.get(partId).offset[2] = offsetAnimZ;

        preFrame.parts.get(partId).scale[0] = scaleX;
        preFrame.parts.get(partId).scale[1] = scaleY;
        preFrame.parts.get(partId).scale[2] = scaleZ;
    }

    public void setAnimation(Float[] partSets) {
        if (partSets == null) {
            return;
        }
        rotateAngleX = partSets[0];
        rotateAngleY = partSets[1];
        rotateAngleZ = partSets[2];
        offsetAnimX += partSets[3];
        offsetAnimY += partSets[4];
        offsetAnimZ += partSets[5];
        scaleX *= partSets[6];
        scaleY *= partSets[7];
        scaleZ *= partSets[8];
        if (partSets[9] != 0.0f || partSets[10] != 0.0f) {
            rotateAngleX1 = partSets[9];
            rotateAngleY1 = partSets[10];
            if (isNormal) {
                isNormal = false;
            }
        }
        isAnimPart = partSets[0] != 0.0f || partSets[1] != 0.0f || partSets[2] != 0.0f ||
                partSets[3] != 0.0f || partSets[4] != 0.0f || partSets[5] != 0.0f ||
                partSets[6] != 1.0f || partSets[7] != 1.0f || partSets[8] != 1.0f ||
                partSets[9] != 0.0f || partSets[10] != 0.0f;
    }

    public void setAnimation(DataAnimation animation) {
        if (!showModel) {
            return;
        }
        showModel = animation.getAnimationPartShow(partId);
        if (!showModel) {
            return;
        }
        setAnimation(animation.getAnimationPartData(partId));
    }

    @Override
    public void setRotationPoint(float rotationPointXIn, float rotationPointYIn, float rotationPointZIn) {
        rotationPointX = rotationPointXIn;
        rotationPointY = rotationPointYIn;
        rotationPointZ = rotationPointZIn;
        baseRotationPoint[0] = rotationPointXIn;
        baseRotationPoint[1] = rotationPointYIn;
        baseRotationPoint[2] = rotationPointZIn;
    }

    public void clearRotations() {
        rotationPointX = baseRotationPoint[0];
        rotationPointY = baseRotationPoint[1];
        rotationPointZ = baseRotationPoint[2];
        rotateAngleX = 0.0f;
        rotateAngleY = 0.0f;
        rotateAngleZ = 0.0f;
        rotateAngleX1 = 0.0f;
        rotateAngleY1 = 0.0f;
        offsetAnimX = 0.0f;
        offsetAnimY = 0.0f;
        offsetAnimZ = 0.0f;
        scaleX = 1.0f;
        scaleY = 1.0f;
        scaleZ = 1.0f;
        isAnimPart = false;
        showModel = true;
        isNormal = baseNormal;
    }

    public float[] getRotations() {
        return new float[] { rotateAngleX, rotateAngleY, rotateAngleZ, rotateAngleX1, rotateAngleY1 };
    }

    public float[] getOffsets() {
        return new float[] { offsetAnimX, offsetAnimY, offsetAnimZ };
    }

    public float[] getScales() {
        return new float[] { scaleX, scaleY, scaleZ };
    }

    // Inner classes
    public static class PositionTextureVertex {
        public final Vector3f pos;
        public final float u;
        public final float v;

        public PositionTextureVertex(float x, float y, float z, float posU, float posV) {
            this.pos = new Vector3f(x, y, z);
            this.u = posU;
            this.v = posV;
        }

        public PositionTextureVertex setTexturePosition(float posU, float posV) {
            this.u = posU;
            this.v = posV;
            return this;
        }
    }

    public static class TexturedQuad {
        public final PositionTextureVertex[] vertices;

        public TexturedQuad(PositionTextureVertex[] positionTextureVertex,
                            float u0, float v0, float u1, float v1, boolean mirror) {
            this.vertices = positionTextureVertex;
            // UV coordinates are set in PositionTextureVertex
        }

        public TexturedQuad(PositionTextureVertex[] positionTextureVertex,
                            float u0, float v0, float u1, float v1, float u2, float v2,
                            float u3, float v3, boolean mirror) {
            this.vertices = positionTextureVertex;
        }

        public void draw(BufferBuilder bufferbuilder, float scale) {
            if (vertices == null || vertices.length < 4) {
                return;
            }

            for (int i = 0; i < 4; i++) {
                PositionTextureVertex vertex = vertices[i];
                Vector4f pos = new Vector4f(vertex.pos.x(), vertex.pos.y(), vertex.pos.z(), 1.0f);
                bufferbuilder.vertex(pos.x(), pos.y(), pos.z())
                        .uv(vertex.u, vertex.v)
                        .endVertex();
            }
        }
    }

    public static class Vec2f {
        public float x, y;

        public Vec2f(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
    /**/
}
