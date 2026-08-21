package noppes.npcs.client.model.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class AniWaving implements AnimationBase {

   public void animatePre(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel<? extends LivingEntity> model, int animationStart) {
   }

   public void animatePost(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel<? extends LivingEntity> model, int animationStart) {
      float f = Mth.sin((float)entity.tickCount * 0.27F);
      float f2 = Mth.sin((float)(entity.tickCount + 1) * 0.27F);
      f += (f2 - f) * Minecraft.getInstance().getDeltaFrameTime();
      model.rightArm.xRot = -0.1F;
      model.rightArm.yRot = 0.0F;
      model.rightArm.zRot = 2.1415926F - (f * 0.5F);
   }

}
