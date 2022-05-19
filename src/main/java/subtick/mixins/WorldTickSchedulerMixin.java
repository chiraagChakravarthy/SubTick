package subtick.mixins;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.tick.QueryableTickScheduler;
import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.progress.TickProgress;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static subtick.progress.TickProgress.*;

@Mixin(value = WorldTickScheduler.class, priority = 1001)
public abstract class WorldTickSchedulerMixin<T> implements QueryableTickScheduler<T> {
    @Shadow protected abstract void collectTickableTicks(long time, int maxTicks, Profiler profiler);

    @Shadow @Final private Supplier<Profiler> profilerGetter;

    @Inject(method = "tick(JILjava/util/function/BiConsumer;)V", at = @At("HEAD"))
    public void blockTickStep(long time, int maxTicks, BiConsumer<BlockPos, T> ticker, CallbackInfo ci){
        int runStatus = TickProgress.runStatus();
        if(runStatus == RUN_COMPLETELY){
            return;
        }
        ci.cancel();

        if(runStatus == STEP_FROM_START){
            collectTickableTicks(time, maxTicks, this.profilerGetter.get());
        }

        if(runStatus == STEP_FROM_START || runStatus == STEP){

        }
    }
}
