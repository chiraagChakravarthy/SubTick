package subtick.mixins.world_tickphases;

import carpet.helpers.TickSpeed;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.tick.WorldTickScheduler;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.TickProgress;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static subtick.TickProgress.*;

@Mixin(value = ServerWorld.class, priority = 999)
public abstract class TileTicksWorldMixin extends World implements StructureWorldAccess {

    protected TileTicksWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
    }

    private boolean actuallyProcessEntities;

    @Inject(method = "tick", at=@At(value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;isDebugWorld()Z"))
    public void preDebug(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        actuallyProcessEntities = TickSpeed.process_entities;
        TickSpeed.process_entities = true;
    }

    @Inject(method = "tick", at=@At(value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;isDebugWorld()Z",
            shift = At.Shift.AFTER))
    public void postDebug(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickSpeed.process_entities = actuallyProcessEntities;
    }

    @Redirect(method = "tick", at=@At(value = "INVOKE",
    target = "Lnet/minecraft/world/tick/WorldTickScheduler;tick(JILjava/util/function/BiConsumer;)V",
    ordinal = 0))
    public void preTileTicks(WorldTickScheduler<Block> instance, long time, int maxTicks, BiConsumer<BlockPos, Block> ticker){
        int runStatus = TickProgress.runStatus();
        if(runStatus == NO_RUN){
            return;
        }
        instance.tick(time, maxTicks, ticker);//tickscheduler mixins will handle run types, just need to know they are the ones currently being run
    }

    @Redirect(method = "tick", at=@At(value = "INVOKE",
            target = "Lnet/minecraft/world/tick/WorldTickScheduler;tick(JILjava/util/function/BiConsumer;)V",
            ordinal = 1))
    public void preLiquidTicks(WorldTickScheduler<Block> instance, long time, int maxTicks, BiConsumer<BlockPos, Block> ticker){
        TickProgress.update(LIQUID_TICKS, this.getRegistryKey());

        int runStatus = TickProgress.runStatus();
        if(runStatus == NO_RUN){
            return;
        }
        instance.tick(time, maxTicks, ticker);//tickscheduler mixins will handle run types, just need to know they are the ones currently being run
    }
}
