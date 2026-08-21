package noppes.npcs.api.event;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.Cancelable;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.ICustomParticle;
import noppes.npcs.api.interfaces.EventFunction;
import noppes.npcs.client.particles.CustomParticle;
import noppes.npcs.controllers.data.PlayerData;

import javax.annotation.Nonnull;

public class CustomParticleEvent extends CustomNPCsEvent {

    private static final String TICK = "customParticleTick";
    private static final String RENDER = "customParticleRender";
    private static final String CREATE = "customParticleCreate";

    public final @Nonnull String name;
    public @Nonnull ICustomParticle particle;
    public IPlayer<?> player;

    public CustomParticleEvent(@Nonnull CustomParticle particleIn, @Nonnull String nameIn) {
        super();
        particle = particleIn;
        name = nameIn;
        player = PlayerData.get(Minecraft.getInstance().player).scriptData.getPlayer();
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

        public @Nonnull VertexConsumer consumer;
        public @Nonnull Camera camera;
        public float partialTicks;

        public RenderEvent(@Nonnull CustomParticle particle, @Nonnull VertexConsumer consumerIn, @Nonnull Camera cameraIn, float partialTicksIn) {
            super(particle, RENDER);
            consumer = consumerIn;
            camera = cameraIn;
            partialTicks = partialTicksIn;
        }

    }

}
