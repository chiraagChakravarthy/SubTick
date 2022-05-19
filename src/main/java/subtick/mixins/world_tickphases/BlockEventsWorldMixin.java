package subtick.mixins.world_tickphases;

import carpet.helpers.TickSpeed;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.TickProgress;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static subtick.TickProgress.*;

@Mixin(value = ServerWorld.class, priority = 999)
public abstract class BlockEventsWorldMixin extends World implements StructureWorldAccess {
    protected BlockEventsWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
    }

    @Inject(method="tick", at=@At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;processSyncedBlockEvents()V"))
    public void preBlockEvents(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        int runStatus = TickProgress.update(BLOCK_EVENTS, this.getRegistryKey());
        TickSpeed.process_entities = runStatus == RUN_COMPLETELY || runStatus == STEP_TO_FINISH;
    }

    @Inject(method = "processSyncedBlockEvents", at=@At("HEAD"), cancellable = true)
    public void onProcessBlockEvents(CallbackInfo ci){
        int runStatus = TickProgress.runStatus();
        if(runStatus == RUN_COMPLETELY || runStatus == STEP_TO_FINISH){
            return;
        }
        ci.cancel();

    }
}
