package dev.ultracraft.mixin;

import dev.ultracraft.WorldMesh;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks broken or placed (by explosions, by anyone) change ULTRAKILL's far terrain too. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "sendBlockUpdated", at = @At("TAIL"))
	private void ultracraft$terrainChanged(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
		if (oldState != newState) WorldMesh.blockChanged(pos);
	}
}
