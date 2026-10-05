package dev.ultracraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ultracraft.Ultracraft;
import dev.ultracraft.V1Arm;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * With ULTRAKILL's guns out, V1's arm and guns come from ULTRAKILL and Minecraft draws no hands. With Minecraft hands
 * out (Steve mode) the held item shows in V1's own right arm, which swings, eats and draws a bow the way Steve's
 * would; an empty hand is V1's fist. Steve's arm never shows.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@Shadow
	private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equip) {
		throw new AssertionError();
	}

	@Shadow
	private void swingArm(float swing, PoseStack poseStack, int side, HumanoidArm arm) {
		throw new AssertionError();
	}

	/** With V1's hand on them, held items come up into view (Minecraft holds them from just below the screen's edge). */
	@Inject(method = "applyItemArmTransform", at = @At("TAIL"))
	private void ultracraft$liftIntoView(PoseStack poseStack, HumanoidArm arm, float equip, CallbackInfo ci) {
		if (Ultracraft.active && Ultracraft.hands) poseStack.translate((arm == HumanoidArm.RIGHT ? 1 : -1) * V1Arm.LIFT_X, V1Arm.LIFT_Y, 0f);
	}

	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void ultracraft$hideHand(float f, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int light, CallbackInfo ci) {
		// at a shop's screen, ULTRAKILL's own arm points at it instead
		if (Ultracraft.active && (!Ultracraft.hands || Ultracraft.shopTouch)) ci.cancel();
	}

	/** An empty main hand: V1's fist where Steve's arm would be, with the same equip and swing. */
	@Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
	private void ultracraft$v1Fist(PoseStack poseStack, SubmitNodeCollector collector, int light, float equip, float swing, HumanoidArm arm, CallbackInfo ci) {
		if (!Ultracraft.active) return;
		ci.cancel();
		if (!Ultracraft.hands) return;
		applyItemArmTransform(poseStack, arm, equip);
		swingArm(swing, poseStack, arm == HumanoidArm.RIGHT ? 1 : -1, arm);
		V1Arm.render(poseStack, collector, light, arm, true);
	}

	/** Right before Minecraft draws a held item: V1's hand around it, moved by the same transforms. */
	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
	private void ultracraft$v1Hand(AbstractClientPlayer player, float partial, float pitch, InteractionHand hand, float swing, ItemStack stack, float equip,
		PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		if (!Ultracraft.active || !Ultracraft.hands || hand != InteractionHand.MAIN_HAND) return;
		V1Arm.render(poseStack, collector, light, player.getMainArm(), false);
	}
}
