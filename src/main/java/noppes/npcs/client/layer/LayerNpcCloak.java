package noppes.npcs.client.layer;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.client.model.ModelNpcAlt;
import noppes.npcs.client.model.part.ModelPartConfig;
import noppes.npcs.constants.EnumParts;

public class LayerNpcCloak<T extends EntityLivingBase> extends LayerInterface<T> {

	public LayerNpcCloak(RenderLiving<?> render) {
		super(render);
	}

	@Override
	public void render(float par2, float par3, float par4, float par5, float par6, float par7) {
		if (!(this.model instanceof ModelNpcAlt)) {
			return;
		}
		if (this.npc.textureCloakLocation == null) {
			if (this.npc.display.getCapeTexture() == null || this.npc.display.getCapeTexture().isEmpty()
					|| !(this.model instanceof ModelNpcAlt)) {
				return;
			}
			this.npc.textureCloakLocation = new ResourceLocation(this.npc.display.getCapeTexture());
		}
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		this.render.bindTexture(this.npc.textureCloakLocation);
		GlStateManager.pushMatrix();
		ModelPartConfig config = this.playerdata.getPartConfig(EnumParts.BODY);
		GlStateManager.translate(config.transX, config.transY, config.transZ);
		GlStateManager.translate(0.0f, 0.0f, 0.125f);
		double d = this.npc.prevChasingPosX + (this.npc.chasingPosX - this.npc.prevChasingPosX) * par7 - (this.npc.prevPosX + (this.npc.posX - this.npc.prevPosX) * par7);
		double d3 = this.npc.prevChasingPosZ + (this.npc.chasingPosZ - this.npc.prevChasingPosZ) * par7 - (this.npc.prevPosZ + (this.npc.posZ - this.npc.prevPosZ) * par7);
		float f11 = this.npc.prevRenderYawOffset + (this.npc.renderYawOffset - this.npc.prevRenderYawOffset) * par7;
		double d4 = MathHelper.sin(f11 * 3.141593f / 180.0f);
		double d5 = -MathHelper.cos(f11 * 3.141593f / 180.0f);
		float f12 = (float) (d * d4 + d3 * d5) * 100.0f;
		float f13 = (float) (d * d5 - d3 * d4) * 100.0f;
		if (f12 < 0.0f) {
			f12 = 0.0f;
		}
		float f15 = 5.0f;
		if (this.npc.isSneaking()) {
			f15 += 25.0f;
		}
		GlStateManager.rotate(6.0f + f12 / 2.0f + f15, 1.0f, 0.0f, 0.0f);
		GlStateManager.rotate(f13 / 2.0f, 0.0f, 0.0f, 1.0f);
		GlStateManager.rotate(-f13 / 2.0f, 0.0f, 1.0f, 0.0f);
		GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f);
		((ModelNpcAlt) this.model).renderCape(0.0625f);
		GlStateManager.popMatrix();
	}

	@Override
	public void rotate(float par1, float par2, float par3, float par4, float par5, float par6) {
	}
}
