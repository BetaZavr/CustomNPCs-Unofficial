package noppes.npcs.mixin.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.model.animation.AnimationHandler;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.entity.EntityCustomNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HumanoidModel.class, priority = 498)
public class BipedBodyMixin<T extends LivingEntity> {

   @SuppressWarnings("unchecked")
   @Inject(
      at = {@At("HEAD")},
      method = {"setupAnim*"}
   )
   private void setupAnimPre(T livingEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callbackInfo) {
      HumanoidModel<T> bipedModel = (HumanoidModel<T>) (Object) this;
      if (livingEntity instanceof EntityCustomNpc playerEntity && bipedModel instanceof PlayerModel) {
         ClientProxy.data = playerEntity.modelData;
         ClientProxy.playerModel = (PlayerModel<LivingEntity>) bipedModel;
         RenderCustomNpc<EntityCustomNpc, HumanoidModel<EntityCustomNpc>> renderer = (RenderCustomNpc<EntityCustomNpc, HumanoidModel<EntityCustomNpc>>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(livingEntity);
         ClientProxy.armorLayer =  renderer.armorLayer;
         AnimationHandler.animateBipedPre(ClientProxy.data, bipedModel, livingEntity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      }
   }

   @SuppressWarnings("unchecked")
   @Inject(
      at = {@At("TAIL")},
      method = {"setupAnim*"}
   )
   private void setupAnimPost(T livingEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callbackInfo) {
      HumanoidModel<T> bipedModel = (HumanoidModel<T>) (Object) this;
      if (livingEntity instanceof EntityCustomNpc npc) {
         AnimationHandler.animateBipedPost(ClientProxy.data, bipedModel, livingEntity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
         if (npc.advanced.animationType == 1) {
            if (npc.puppet.isActive()) {
               float pi = (float) Math.PI;
               float partialTicks = Minecraft.getInstance().getDeltaFrameTime();
               if (!npc.puppet.head.disabled) {
                  bipedModel.hat.xRot = bipedModel.head.xRot = npc.puppet.getRotationX(npc.puppet.head, npc.puppet.head2, partialTicks) * pi;
                  bipedModel.hat.yRot = bipedModel.head.yRot = npc.puppet.getRotationY(npc.puppet.head, npc.puppet.head2, partialTicks) * pi;
                  bipedModel.hat.zRot = bipedModel.head.zRot = npc.puppet.getRotationZ(npc.puppet.head, npc.puppet.head2, partialTicks) * pi;
               }
               if (!npc.puppet.body.disabled) {
                  bipedModel.body.xRot = npc.puppet.getRotationX(npc.puppet.body, npc.puppet.body2, partialTicks) * pi;
                  bipedModel.body.yRot = npc.puppet.getRotationY(npc.puppet.body, npc.puppet.body2, partialTicks) * pi;
                  bipedModel.body.zRot = npc.puppet.getRotationZ(npc.puppet.body, npc.puppet.body2, partialTicks) * pi;
               }
               if (!npc.puppet.larm.disabled) {
                  bipedModel.leftArm.xRot = npc.puppet.getRotationX(npc.puppet.larm, npc.puppet.larm2, partialTicks) * pi;
                  bipedModel.leftArm.yRot = npc.puppet.getRotationY(npc.puppet.larm, npc.puppet.larm2, partialTicks) * pi;
                  bipedModel.leftArm.zRot = npc.puppet.getRotationZ(npc.puppet.larm, npc.puppet.larm2, partialTicks) * pi;
                  if (npc.display.getHasLivingAnimation()) {
                     bipedModel.leftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
                     bipedModel.leftArm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;
                  }
               }
               if (!npc.puppet.rarm.disabled) {
                  bipedModel.rightArm.xRot = npc.puppet.getRotationX(npc.puppet.rarm, npc.puppet.rarm2, partialTicks) * pi;
                  bipedModel.rightArm.yRot = npc.puppet.getRotationY(npc.puppet.rarm, npc.puppet.rarm2, partialTicks) * pi;
                  bipedModel.rightArm.zRot = npc.puppet.getRotationZ(npc.puppet.rarm, npc.puppet.rarm2, partialTicks) * pi;
                  if (npc.display.getHasLivingAnimation()) {
                     bipedModel.rightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
                     bipedModel.rightArm.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
                  }
               }
               if (!npc.puppet.rleg.disabled) {
                  bipedModel.rightLeg.xRot = npc.puppet.getRotationX(npc.puppet.rleg, npc.puppet.rleg2, partialTicks) * pi;
                  bipedModel.rightLeg.yRot = npc.puppet.getRotationY(npc.puppet.rleg, npc.puppet.rleg2, partialTicks) * pi;
                  bipedModel.rightLeg.zRot = npc.puppet.getRotationZ(npc.puppet.rleg, npc.puppet.rleg2, partialTicks) * pi;
               }
               if (!npc.puppet.lleg.disabled) {
                  bipedModel.leftLeg.xRot = npc.puppet.getRotationX(npc.puppet.lleg, npc.puppet.lleg2, partialTicks) * pi;
                  bipedModel.leftLeg.yRot = npc.puppet.getRotationY(npc.puppet.lleg, npc.puppet.lleg2, partialTicks) * pi;
                  bipedModel.leftLeg.zRot = npc.puppet.getRotationZ(npc.puppet.lleg, npc.puppet.lleg2, partialTicks) * pi;
               }
            }
         }
      }
   }

}
