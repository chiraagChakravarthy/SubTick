package subtick.mixins;

import carpet.utils.Messenger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.QueryableTickScheduler;
import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.Highlights;
import subtick.progress.TickActions;
import subtick.progress.TickProgress;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static subtick.progress.TickProgress.*;

@Mixin(value = WorldTickScheduler.class, priority = 1001)
public abstract class WorldTickSchedulerMixin<T> implements QueryableTickScheduler<T> {
    @Shadow protected abstract void collectTickableTicks(long time, int maxTicks, Profiler profiler);

    @Shadow @Final private Supplier<Profiler> profilerGetter;

    @Shadow protected abstract void clear();

    @Shadow protected abstract void tick(BiConsumer<BlockPos, T> ticker);

    @Shadow @Final public Queue<OrderedTick<T>> tickableTicks;

    @Shadow @Final private Set<OrderedTick<?>> copiedTickableTicksList;

    @Shadow @Final private List<OrderedTick<T>> tickedTicks;

    @Inject(method = "tick(JILjava/util/function/BiConsumer;)V", at = @At("HEAD"), cancellable = true)
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
            int total = 0;
            for (int i = 0; i < TickActions.numActionsStep; i++) {
                if(tickableTicks.isEmpty()){
                    Messenger.m(TickActions.actor, "gi No more Tile Ticks in this dimension");
                    TickActions.tickCount = TickActions.ticksPerStep * TickActions.numActionsPlay + 1;
                    break;
                }
                total++;
                OrderedTick<T> orderedTick = this.tickableTicks.poll();
                if (!this.copiedTickableTicksList.isEmpty()) {
                    this.copiedTickableTicksList.remove(orderedTick);
                }

                this.tickedTicks.add(orderedTick);
                ticker.accept(orderedTick.pos(), orderedTick.type());
                if(TickActions.ttSuccess){
                    Highlights.executedHighlight(orderedTick.pos(), TickActions.tempWorld);
                } else {
                    i--;
                    total--;
                }
            }

            if(TickActions.numActionsStep != 0 && !TickActions.stillPlaying()){
                int totalStepped;
                if(tickableTicks.isEmpty()){
                    if(TickActions.ticksPerStep == 0){
                        totalStepped = total;
                    } else {
                        totalStepped = (TickActions.tickCount-1)/TickActions.ticksPerStep + total;
                    }
                } else {
                    totalStepped = TickActions.numActionsPlay;
                }
                if(totalStepped != 0)
                    Messenger.m(TickActions.actor, "wi Stepped " + totalStepped + " tile tick" + (totalStepped==1?"":"s"));
            }
        }

        if(runStatus == STEP_TO_FINISH){
            tick(ticker);
            clear();
        }
    }
}
