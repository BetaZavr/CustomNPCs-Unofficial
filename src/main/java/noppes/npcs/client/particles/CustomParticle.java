package noppes.npcs.client.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.renderer.obj.ModelBuffer;
import noppes.npcs.client.renderer.obj.ParameterizedModel;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.CustomParticleEvent;
import noppes.npcs.api.handler.data.ICustomParticle;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class CustomParticle extends Particle implements ICustomElement, ICustomParticle {

	public @Nonnull NBTTagCompound nbtData;
	public @Nonnull ResourceLocation texture;
	public @Nullable ResourceLocation obj;
	public ParameterizedModel objModel;
	protected float particleAngleX;
	protected float particleAngleZ;
	protected long rndStart;

	public CustomParticle(@Nonnull NBTTagCompound nbtParticle, World worldIn,
						  double xCoordIn, double yCoordIn, double zCoordIn,
						  double xSpeedIn, double ySpeedIn, double zSpeedIn) {
		super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
		nbtData = nbtParticle;
		rndStart = rand.nextInt(7000);

		String name = NoppesUtilServer.validPath(nbtParticle.hasKey("Texture", 8) ?
				nbtParticle.getString("Texture") :
				nbtParticle.getString("RegistryName"));
		texture = new ResourceLocation(CustomNpcs.MODID, "textures/particle/" + name + ".png");

		if (nbtParticle.hasKey("OBJModel", 8)) { obj = new ResourceLocation(CustomNpcs.MODID, "models/particle/" + nbtParticle.getString("OBJModel") + ".obj"); }
		if (nbtParticle.hasKey("MaxAge", 3)) { particleMaxAge = nbtParticle.getInteger("MaxAge"); }
		if (nbtParticle.hasKey("Gravity", 5)) { particleGravity = nbtParticle.getFloat("Gravity"); }
		if (nbtParticle.hasKey("Scale", 5)) { particleScale = nbtParticle.getFloat("Scale"); }
		if (xSpeedIn == 0.0d) { motionX = 0.0d; }
		if (ySpeedIn == 0.0d) { motionY = 0.0d; }
		if (zSpeedIn == 0.0d) { motionZ = 0.0d; }
		if (nbtParticle.hasKey("StartMotion", 9) && nbtParticle.getTagList("StartMotion", 6).tagCount() > 2) {
			NBTTagList list = nbtParticle.getTagList("StartMotion", 6);
			if (nbtParticle.getBoolean("IsRandomMotion")) {
				motionX = (Math.random() < 0.5d ? -1.0d : 1.0d) * Math.random() * list.getDoubleAt(0);
				motionY = (Math.random() < 0.5d && !nbtParticle.getBoolean("NotMotionY") ? -1.0d : 1.0d) * Math.random() * list.getDoubleAt(1);
				motionZ = (Math.random() < 0.5d ? -1.0d : 1.0d) * Math.random() * list.getDoubleAt(2);
			}
			else {
				motionX = list.getDoubleAt(0);
				motionY = list.getDoubleAt(1);
				motionZ = list.getDoubleAt(2);
			}
		}
		CustomParticleEvent event = new CustomParticleEvent.CreateEvent(this);
		EventHooks.onEvent(ScriptController.Instance.clientScripts, event.name, event);
	}

	@Override
	public boolean canCollide() { return canCollide; }

	@Override
	public int getAge() { return particleAge; }

	@Override
	public float getAlphaF() { return particleAlpha; }

	@Override
	public int getColorMask() { return (int) (((int) (particleRed * 255.0f) << 16) + ((int) (particleGreen * 255.0f) << 8) + particleBlue * 255.0f); }

	@Override
	public float getHeight() { return height; }

	@Override
	public String getObj() { return obj == null ? "" : obj.toString(); }

	@Override
	public double[] getPrevPoses() { return new double[] { prevPosX, prevPosY, prevPosZ }; }

	@Override
	public float getRotationX() { return particleAngleX; }

	@Override
	public float getRotationY() { return particleAngle; }

	@Override
	public float getRotationZ() { return particleAngleZ; }

	@Override
	public float getScale() { return particleScale; }

	@Override
	public String getTexture() { return texture.toString(); }

	@Override
	public int getTotalAge() { return particleMaxAge; }

	@Override
	public float getWidth() { return width; }

	@Override
	public IWorld getWorld() { return Objects.requireNonNull(NpcAPI.Instance()).getIWorld(world); }

	@Override
	public boolean onGround() { return onGround; }

	/** Every tick */
	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		if (particleAge++ >= particleMaxAge) { setExpired(); }
		else {
			CustomParticleEvent event = new CustomParticleEvent.UpdateEvent(this);
			EventHooks.onEvent(ScriptController.Instance.clientScripts, event.name, event);
			if (!event.isCanceled()) {
				motionY -= 0.04D * (double) particleGravity;
				move(motionX, motionY, motionZ);
				motionX *= 0.98D;
				motionY *= 0.98D;
				motionZ *= 0.98D;
				if (onGround) {
					motionX *= 0.7D;
					motionZ *= 0.7D;
				}
				if (obj != null && !onGround) {
					particleAngle = (float) (((System.currentTimeMillis() + rndStart) / 7L) % 360L);
					particleAngleX = (float) (((System.currentTimeMillis() + rndStart) / 7L) % 360L);
				}
			}
		}
	}

	@Override
	public double posX() { return posX; }

	@Override
	public double posY() { return posY; }

	@Override
	public double posZ() { return posZ; }

	@Override
	@SuppressWarnings("ConstantConditions")
	public void renderParticle(@Nonnull BufferBuilder buffer, @Nonnull Entity entity, float partialTicks, float rotationX, float rotationZ,
							   float rotationYZ, float rotationXY, float rotationXZ) {
		CustomParticleEvent.RenderEvent event = new CustomParticleEvent.RenderEvent(this, buffer, entity, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
		EventHooks.onEvent(ScriptController.Instance.clientScripts, event.name, event);
		buffer = event.buffer;
		partialTicks = event.partialTicks;
		rotationX = event.rotationX;
		rotationZ = event.rotationZ;
		rotationYZ = event.rotationYZ;
		rotationXY = event.rotationXY;
		rotationXZ = event.rotationXZ;
		if (!event.isCanceled()) {
			try {
				if (obj != null) {
					GlStateManager.pushMatrix();
					if (objModel == null) { objModel = ModelBuffer.getParameterizedModel(obj, null, null, false, 0, true); }
					if (objModel != null) {
						float dx = (float) (prevPosX + (posX - prevPosX) * (double) partialTicks - interpPosX);
						float dy = (float) (prevPosY + (posY - prevPosY) * (double) partialTicks - interpPosY);
						float dz = (float) (prevPosZ + (posZ - prevPosZ) * (double) partialTicks - interpPosZ);
						GlStateManager.translate(dx + posX, dy + posY, dz +  posZ);
						if (particleScale != 0.0f) { GlStateManager.scale(particleScale, particleScale, particleScale); }
						if (particleAngle != 0.0f) { GlStateManager.rotate(particleAngle, 1.0f, 0.0f, 0.0f); }
						if (particleAngleX != 0.0f) { GlStateManager.rotate(particleAngleX, 0.0f, 1.0f, 0.0f); }
						if (particleAngleZ != 0.0f) { GlStateManager.rotate(particleAngleZ, 0.0f, 0.0f, 1.0f); }
						GlStateManager.enableDepth();
						GlStateManager.color(particleRed, particleGreen, particleBlue, particleAlpha);
						GlStateManager.enableRescaleNormal();
						GlStateManager.enableLighting();
						RenderHelper.enableStandardItemLighting();
						OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 240.0f);
						ModelBuffer.render(objModel);
						GlStateManager.disableBlend();
					}
					GlStateManager.popMatrix();
				}
				else if (texture != null) {
					Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
					float f4 = 0.1F * particleScale;
					float f5 = (float) (prevPosX + (posX - prevPosX) * (double) partialTicks - interpPosX);
					float f6 = (float) (prevPosY + (posY - prevPosY) * (double) partialTicks - interpPosY);
					float f7 = (float) (prevPosZ + (posZ - prevPosZ) * (double) partialTicks - interpPosZ);
					int i = getBrightnessForRender(partialTicks);
					int j = i >> 16 & 65535;
					int k = i & 65535;
					Vec3d[] avec3d = new Vec3d[] {
							new Vec3d(-rotationX * f4 - rotationXY * f4, -rotationZ * f4, -rotationYZ * f4 - rotationXZ * f4),
							new Vec3d(-rotationX * f4 + rotationXY * f4, rotationZ * f4, -rotationYZ * f4 + rotationXZ * f4),
							new Vec3d(rotationX * f4 + rotationXY * f4, rotationZ * f4, rotationYZ * f4 + rotationXZ * f4),
							new Vec3d(rotationX * f4 - rotationXY * f4, -rotationZ * f4, rotationYZ * f4 - rotationXZ * f4) };

					if (particleAngle != 0.0F) {
						float f8 = particleAngle + (particleAngle - prevParticleAngle) * partialTicks;
						float f9 = MathHelper.cos(f8 * 0.5F);
						float f10 = MathHelper.sin(f8 * 0.5F) * (float) cameraViewDir.x;
						float f11 = MathHelper.sin(f8 * 0.5F) * (float) cameraViewDir.y;
						float f12 = MathHelper.sin(f8 * 0.5F) * (float) cameraViewDir.z;
						Vec3d vec3d = new Vec3d(f10, f11, f12);
						for (int l = 0; l < 4; ++l) {
							avec3d[l] = vec3d.scale(2.0D * avec3d[l].dotProduct(vec3d))
									.add(avec3d[l].scale((double) (f9 * f9) - vec3d.dotProduct(vec3d)))
									.add(vec3d.crossProduct(avec3d[l]).scale(2.0F * f9));
						}
					}
					buffer.pos((double) f5 + avec3d[0].x, (double) f6 + avec3d[0].y, (double) f7 + avec3d[0].z)
							.tex(1.0f, 1.0f)
							.color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(j, k)
							.endVertex();
					buffer.pos((double) f5 + avec3d[1].x, (double) f6 + avec3d[1].y, (double) f7 + avec3d[1].z)
							.tex(1.0f, 0.0f)
							.color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(j, k)
							.endVertex();
					buffer.pos((double) f5 + avec3d[2].x, (double) f6 + avec3d[2].y, (double) f7 + avec3d[2].z)
							.tex(0.0f, 0.0f)
							.color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(j, k)
							.endVertex();
					buffer.pos((double) f5 + avec3d[3].x, (double) f6 + avec3d[3].y, (double) f7 + avec3d[3].z)
							.tex(0.0f, 1.0f)
							.color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(j, k)
							.endVertex();
				}
			}
			catch (Exception e) { LogWriter.error(e); }
		}
	}

	@Override
	public void setAge(int ticks) { particleAge = ValueUtil.correctInt(ticks, 0, Integer.MAX_VALUE); }

	@Override
	public void setAlphaF(float alpha) { particleAlpha = ValueUtil.correctFloat(alpha, 0.0f, 1.0f); }

	@Override
	public void setCanCollide(boolean collide) { canCollide = collide; }

	@Override
	public void setColorMask(int color) {
		particleRed = (float) (color >> 16 & 255) / 255.0F;
		particleGreen = (float) (color >> 8 & 255) / 255.0F;
		particleBlue = (float) (color & 255) / 255.0F;
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
	public void setPos(double x, double y, double z) { setPosition(x, y, z); }

	@Override
	public void setRotation(float x, float y, float z) {
		particleAngleX = x % 360.0f;
		particleAngle = y % 360.0f;
		particleAngleZ = z % 360.0f;
	}

	@Override
	public void setScale(float scale) { particleScale = ValueUtil.correctFloat(scale, 0.001f, 50.0f); }

	@Override
	public void setTexture(String textureIn) {
		if (textureIn == null) { texture = new ResourceLocation(CustomNpcs.MODID, "textures/particle/" + nbtData.getString("Texture") + ".png"); }
		else { texture = new ResourceLocation(textureIn); }
	}

	@Override
	public void setTotalAge(int ticks) { particleMaxAge = ValueUtil.correctInt(ticks, 0, Integer.MAX_VALUE); }

	@Override
	public INbt getCustomNbt() { return Objects.requireNonNull(NpcAPI.Instance()).getINbt(nbtData); }

	@Override
	public String getCustomName() { return nbtData.getString("RegistryName").toLowerCase(); }

	@Override
	public int getElementType() { return this.obj != null ? 1 : 0; }

	@Override
	public boolean showInCreative() { return false; }

}
