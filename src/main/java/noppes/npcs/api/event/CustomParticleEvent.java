package noppes.npcs.api.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.ICustomParticle;
import noppes.npcs.api.interfaces.EventFunction;
import noppes.npcs.client.particles.CustomParticle;
import noppes.npcs.controllers.data.PlayerData;

import javax.annotation.Nonnull;

public class CustomParticleEvent extends CustomNPCsEvent {

	public static final String TICK = "customParticleTick";
	public static final String RENDER = "customParticleRender";
	public static final String CREATE = "customParticleCreate";

	public final @Nonnull String name;
	public @Nonnull ICustomParticle particle;
	public IPlayer<?> player;

	public CustomParticleEvent(@Nonnull CustomParticle particleIn, @Nonnull String nameIn) {
		super();
		particle = particleIn;
		name = nameIn;
		player = PlayerData.get(Minecraft.getMinecraft().player).getIPlayer();
	}

	@EventFunction(CREATE)
	public static class CreateEvent extends CustomParticleEvent {
		public CreateEvent(@Nonnull CustomParticle particle) { super(particle, CREATE); }
	}

	@Cancelable
	@EventFunction(TICK)
	public static class UpdateEvent extends CustomParticleEvent {
		public UpdateEvent(@Nonnull CustomParticle particle) { super(particle, TICK); }
	}

	@Cancelable
	@EventFunction(RENDER)
	public static class RenderEvent extends CustomParticleEvent {

		public @Nonnull BufferBuilder buffer;
		public @Nonnull Entity entity;
		public float partialTicks;
		public float rotationX;
		public float rotationZ;
		public float rotationYZ;
		public float rotationXY;
		public float rotationXZ;

		public RenderEvent(@Nonnull CustomParticle particle, @Nonnull BufferBuilder bufferIn, @Nonnull Entity entityIn,
						   float partialTicksIn, float rotationXIn, float rotationZIn,
						   float rotationYZIn, float rotationXYIn, float rotationXZIn) {
			super(particle, RENDER);
			buffer = bufferIn;
			entity = entityIn;
			partialTicks = partialTicksIn;
			rotationX = rotationXIn;
			rotationZ = rotationZIn;
			rotationYZ = rotationYZIn;
			rotationXY = rotationXYIn;
			rotationXZ = rotationXZIn;
		}
	}

}
