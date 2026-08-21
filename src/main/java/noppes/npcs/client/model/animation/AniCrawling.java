package noppes.npcs.client.model.animation;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class AniCrawling implements AnimationBase {

   public void animatePre(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel<? extends LivingEntity> model, int animationStart) {
   }

   public void animatePost(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel<? extends LivingEntity> model, int animationStart) {
      float pi = (float) Math.PI;
      model.head.zRot = -netHeadYaw * pi / 180.0F;
      model.head.yRot = 0.0F;
      model.head.xRot = -0.95993114F;
      model.hat.xRot = model.head.xRot;
      model.hat.yRot = model.head.yRot;
      model.hat.zRot = model.head.zRot;
      if ((double)limbSwingAmount > 0.25D) {
         limbSwingAmount = 0.25F;
      }
      float movement = Mth.cos(limbSwing * 0.8F + pi) * limbSwingAmount;
      model.leftArm.xRot = pi - movement * 0.25F;
      model.leftArm.yRot = movement * -0.46F;
      model.leftArm.zRot = movement * -0.2F;
      model.leftArm.y = 2.0F - movement * 9.0F;
      model.rightArm.xRot = pi + movement * 0.25F;
      model.rightArm.yRot = movement * -0.4F;
      model.rightArm.zRot = movement * -0.2F;
      model.rightArm.y = 2.0F + movement * 9.0F;
      model.body.yRot = movement * 0.1F;
      model.body.xRot = 0.0F;
      model.body.zRot = movement * 0.1F;
      model.leftLeg.xRot = movement * 0.1F;
      model.leftLeg.yRot = movement * 0.1F;
      model.leftLeg.zRot = -0.122173056F - movement * 0.25F;
      model.leftLeg.y = 10.4F + movement * 9.0F;
      model.leftLeg.z = movement * 0.6F;
      model.rightLeg.xRot = movement * -0.1F;
      model.rightLeg.yRot = movement * 0.1F;
      model.rightLeg.zRot = 0.122173056F - movement * 0.25F;
      model.rightLeg.y = 10.4F - movement * 9.0F;
      model.rightLeg.z = movement * -0.6F;
   }

}
