package dev.ultracraft.mixin;

import dev.ultracraft.Movement;
import dev.ultracraft.UkFrame;
import dev.ultracraft.UkLink;
import dev.ultracraft.Ultracraft;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft's camera sits exactly where ULTRAKILL's camera was for the frame we're about to show (V1's eye and look,
 * every frame). Taking the newest frame here, before the world is drawn, and the view it was drawn from, keeps
 * Minecraft's world and ULTRAKILL's layer (enemies, effects, what terrain hides) locked together while turning.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow protected abstract void setPosition(double x, double y, double z);
	@Shadow protected abstract void setRotation(float yaw, float pitch);

	@Inject(method = "setup", at = @At("TAIL"))
	private void ultracraft$v1Camera(Level level, Entity entity, boolean detached, boolean mirror, float partial, CallbackInfo ci) {
		if (!Ultracraft.active && Ultracraft.steveView) {
			// Steve with ULTRAKILL's enemies about: ULTRAKILL draws from where Minecraft's camera would be, and Minecraft
			// draws its world from the view of the frame ULTRAKILL drew, so the two never slide apart
			Ultracraft.steveDrawn = false;
			if (detached) return;
			Vec3 at = ((Camera) (Object) this).position();
			UkLink.send(String.format(java.util.Locale.ROOT, "STEVECAM %.4f %.4f %.4f %.3f %.3f %.2f", at.x, at.y, at.z,
				((Camera) (Object) this).yRot(), ((Camera) (Object) this).xRot(), Ultracraft.steveFov));
			UkFrame.updateForCamera();
			float[] f = UkFrame.framePose;
			if (f == null || !UkFrame.fresh()) return;
			setRotation(f[3], f[4]);
			setPosition(f[0], f[1], f[2]);
			Ultracraft.steveDrawn = true;
			return;
		}
		// asleep in a bed: Minecraft's own view from the pillow
		if (!Ultracraft.active || detached || Movement.sleeping) return;
		UkFrame.updateForCamera();
		float[] f = UkFrame.framePose;
		if (f != null) {
			setRotation(f[3], f[4]);
			setPosition(f[0], f[1], f[2]);
			return;
		}
		UkLink.Pose p = UkLink.pose;
		if (p == null) return;
		setRotation(p.yaw, p.pitch);
		setPosition(p.ex, p.ey, p.ez);
	}
}
