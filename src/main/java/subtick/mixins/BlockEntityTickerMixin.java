package subtick.mixins;

import carpet.helpers.TickSpeed;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.progress.TickProgress;

import static subtick.progress.TickProgress.RUN_COMPLETELY;
import static subtick.progress.TickProgress.STEP_TO_FINISH;

@Mixin(targets = {"net.minecraft.world.chunk.WorldChunk$DirectBlockEntityTickInvoker"}, priority = 999)
public abstract class BlockEntityTickerMixin implements BlockEntityTickInvoker {

    @Shadow @Final private BlockEntity blockEntity;

    @Inject(method = "tick", at=@At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BlockEntity;)V"))
    public void preTileEntity(CallbackInfo ci){
        if(blockEntity.getWorld() != null && blockEntity.getWorld().isClient){
            return;
        }
        int runStatus = TickProgress.runStatus();
        TickSpeed.process_entities = runStatus == RUN_COMPLETELY || runStatus == STEP_TO_FINISH;
    }

}
