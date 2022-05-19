package subtick.mixins;

import carpet.helpers.TickSpeed;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.tick.Tick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.TickProgress;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin extends ChunkManager {

    @Shadow @Final private ServerWorld world;

    private boolean actuallyProcessEntities;

    @Inject(method = "tickChunks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;isDebugWorld()Z",
            shift = At.Shift.BEFORE))
    public void preTickChunks(CallbackInfo ci){
        actuallyProcessEntities = TickSpeed.process_entities;

        int dimension = TickProgress.dim(this.world.getRegistryKey());

        TickSpeed.process_entities = TickProgress.currentProgress == TickProgress.progressOf(TickProgress.RAIDS, dimension)
                && TickProgress.runStatus() == TickProgress.RUN_COMPLETELY || TickProgress.runStatus() == TickProgress.STEP_TO_FINISH;
    }

    @Inject(method = "tickChunks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;isDebugWorld()Z",
            shift = At.Shift.AFTER))
    public void postTickChunks(CallbackInfo ci){
        TickSpeed.process_entities = actuallyProcessEntities;
    }
}
