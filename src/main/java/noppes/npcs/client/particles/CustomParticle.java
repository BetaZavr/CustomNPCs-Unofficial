package noppes.npcs.client.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.CustomParticleEvent;
import noppes.npcs.api.handler.data.ICustomParticle;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.mixin.client.particles.IParticleMixin;
import noppes.npcs.util.ValueUtil;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

//net.minecraft.client.particle.CritParticle
public class CustomParticle extends TextureSheetParticle implements ICustomElement, ICustomParticle {

    public final @Nonnull CompoundTag nbtData;
    public @Nonnull ResourceLocation texture;
    public @Nullable ResourceLocation obj;
    public ParameterizedModel objModel;

    protected float rollX;
    protected float rollZ;
    protected float oRollX;
    protected float oRollZ;

    public CustomParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, @Nonnull CompoundTag nbtParticle) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        nbtData = nbtParticle;

        String name = NoppesUtilServer.validPath(nbtParticle.contains("Texture", 8) ?
                nbtParticle.getString("Texture") :
                nbtParticle.getString("RegistryName"));
        texture = new ResourceLocation(CustomNpcs.MODID, "textures/particle/" + name + ".png");

        friction = 0.7F;
        gravity = 0.25F;
        xd *= 0.1F;
        yd *= 0.1F;
        zd *= 0.1F;
        xd += dx * 0.4D;
        yd += dy * 0.4D;
        zd += dz * 0.4D;
        float f = (float)(Math.random() * (double)0.3F + (double)0.6F);
        rCol = f;
        gCol = f;
        bCol = f;
        quadSize *= 0.75F;
        lifetime = 50;
        hasPhysics = false;
        tick();
        CustomParticleEvent event = new CustomParticleEvent.CreateEvent(this);
        EventHooks.onEvent(ScriptController.Instance.clientScripts, event.name, event);
    }

    @Override
    public void render(@Nonnull VertexConsumer consumer, @Nonnull Camera camera, float partialTicks) {
        CustomParticleEvent.RenderEvent event = new CustomParticleEvent.RenderEvent(this, consumer, camera, partialTicks);
        EventHooks.onEvent(ScriptController.Instance.clientScripts, "customParticleRenderEvent", event);
        consumer = event.consumer;
        camera = event.camera;
        partialTicks = event.partialTicks;
        if (!event.isCanceled()) {
            Vec3 cameraPos = camera.getPosition();
            float f = (float)(Mth.lerp(partialTicks, xo, x) - cameraPos.x());
            float f1 = (float)(Mth.lerp(partialTicks, yo, y) - cameraPos.y());
            float f2 = (float)(Mth.lerp(partialTicks, zo, z) - cameraPos.z());
            Quaternionf quaternionf;
            if (roll == 0.0F) { quaternionf = camera.rotation(); }
            else {
                quaternionf = new Quaternionf(camera.rotation());
                quaternionf.rotateZ(Mth.lerp(partialTicks, oRoll, roll));
            }
            Vector3f[] avector3f = new Vector3f[] { new Vector3f(-1.0F, -1.0F, 0.0F),
                    new Vector3f(-1.0F, 1.0F, 0.0F),
                    new Vector3f(1.0F, 1.0F, 0.0F),
                    new Vector3f(1.0F, -1.0F, 0.0F) };
            for(int i = 0; i < 4; ++i) {
                Vector3f vector3f = avector3f[i];
                vector3f.rotate(quaternionf);
                vector3f.mul(getQuadSize(partialTicks));
                vector3f.add(f, f1, f2);
            }
            int lightColor = getLightColor(partialTicks);
            consumer.vertex(avector3f[0].x(), avector3f[0].y(), avector3f[0].z()).uv(1.0f, 1.0f).color(rCol, gCol, bCol, alpha).uv2(lightColor).endVertex();
            consumer.vertex(avector3f[1].x(), avector3f[1].y(), avector3f[1].z()).uv(1.0f, 0.0f).color(rCol, gCol, bCol, alpha).uv2(lightColor).endVertex();
            consumer.vertex(avector3f[2].x(), avector3f[2].y(), avector3f[2].z()).uv(0.0f, 0.0f).color(rCol, gCol, bCol, alpha).uv2(lightColor).endVertex();
            consumer.vertex(avector3f[3].x(), avector3f[3].y(), avector3f[3].z()).uv(0.0f, 1.0f).color(rCol, gCol, bCol, alpha).uv2(lightColor).endVertex();
        }
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return 0.75f * Mth.clamp(((float) age + partialTicks) / (float) lifetime * 32.0f, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        oRollX = rollX;
        oRollZ = rollZ;
        if (age++ >= lifetime) { remove(); }
        else {
            CustomParticleEvent event = new CustomParticleEvent.UpdateEvent(this);
            EventHooks.onEvent(ScriptController.Instance.clientScripts, event.name, event);
            if (!event.isCanceled()) {
                yd -= 0.04D * (double)gravity;
                move(xd, yd, zd);
                if (speedUpWhenYMotionIsBlocked && y == yo) {
                    xd *= 1.1D;
                    zd *= 1.1D;
                }
                xd *= friction;
                yd *= friction;
                zd *= friction;
                if (onGround) {
                    xd *= 0.7F;
                    zd *= 0.7F;
                }
                gCol *= 0.96F;
                bCol *= 0.9F;
            }
        }
    }

    @Override
    public @Nonnull ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    @Override
    public boolean canCollide() { return ((IParticleMixin) this).getStoppedByCollision(); }

    @Override
    public int getAge() { return age; }

    @Override
    public int getColorMask() { return (int) (((int) (rCol * 255.0f) << 16) + ((int) (gCol * 255.0f) << 8) + bCol * 255.0f); }

    @Override
    public float getHeight() { return bbHeight; }

    @Override
    public String getObj() { return obj == null ? "" : obj.toString(); }

    @Override
    public double[] getPrevPoses() { return new double[] { xo, yo, zo }; }

    @Override
    public float getAlphaF() { return alpha; }

    @Override
    public float getRotationX() { return rollX; }

    @Override
    public float getRotationY() { return roll; }

    @Override
    public float getRotationZ() { return rollZ; }

    @Override
    public float getScale() { return quadSize; }

    @Override
    public String getTexture() { return texture.toString(); }

    @Override
    public int getTotalAge() { return lifetime; }

    @Override
    public float getWidth() { return bbWidth; }

    @Override
    public IWorld getWorld() { return Objects.requireNonNull(NpcAPI.Instance()).getIWorld(level); }

    @Override
    public boolean onGround() { return onGround; }

    @Override
    public double posX() { return x; }

    @Override
    public double posY() { return y; }

    @Override
    public double posZ() { return z; }

    @Override
    public void setAge(int ticks) { age = ValueUtil.correctInt(ticks, 0, Integer.MAX_VALUE); }

    @Override
    public void setCanCollide(boolean collide) { ((IParticleMixin) this).setStoppedByCollision(collide); }

    @Override
    public void setColorMask(int color) {
        rCol = (float) (color >> 16 & 255) / 255.0F;
        gCol = (float) (color >> 8 & 255) / 255.0F;
        bCol = (float) (color & 255) / 255.0F;
    }

    @Override
    public void setCustomSize(float width, float height) { setSize(width, height); }

    @Override
    public void setObj(String objPath) {
        if (objPath == null) { obj = null; }
        else { obj = new ResourceLocation(objPath); }
        objModel = null;
    }

    @Override
    public void setRotation(float x, float y, float z) {
        rollX = x % 360.0f;
        roll = y % 360.0f;
        rollZ = z % 360.0f;
    }

    @Override
    public void setScale(float scale) { quadSize = ValueUtil.correctFloat(scale, 0.001f, 50.0f); }

    @Override
    public void setTexture(String textureIn) {
        if (textureIn == null) { texture = new ResourceLocation(CustomNpcs.MODID, "textures/particle/" + nbtData.getString("Texture") + ".png"); }
        else { texture = new ResourceLocation(textureIn); }
    }

    @Override
    public void setTotalAge(int ticks) { lifetime = ValueUtil.correctInt(ticks, 0, Integer.MAX_VALUE); }


    @Override
    public String getCustomName() { return nbtData.getString("RegistryName").toLowerCase(); }

    @Override
    public INbt getCustomNbt() { return new NBTWrapper(nbtData); }

    @Override
    public int getElementType() { return nbtData.contains("OBJModel", 8) && !nbtData.getString("OBJModel").isEmpty() ? 1 : 0; }

    @Override
    public boolean showInCreative() { return false; }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<CustomParticleType> {

        public Provider(SpriteSet ignored) {}

        public CustomParticle createParticle(@Nonnull CustomParticleType particleType, @Nonnull ClientLevel level,
                                       double x, double y, double z, double dx, double dy, double dz) {
            return new CustomParticle(level, x, y, z, dx, dy, dz, particleType.nbtData);
        }
    }

}
