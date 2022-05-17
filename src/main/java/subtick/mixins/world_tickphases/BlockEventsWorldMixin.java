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

    private boolean actuallyProcessEntities;

    @Inject(method="tick", at=@At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;processSyncedBlockEvents()V"))
    public void preBlockEvents(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickProgress.update(BLOCK_EVENTS, this.getRegistryKey());
        int runStatus = TickProgress.runStatus();
        actuallyProcessEntities = TickSpeed.process_entities;
        TickSpeed.process_entities = runStatus != NO_RUN;
    }

    @Inject(method="tick", at=@At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;processSyncedBlockEvents()V", shift = At.Shift.AFTER))
    public void postBlockEvents(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickSpeed.process_entities = actuallyProcessEntities;
    }

    @Inject(method = "processSyncedBlockEvents", at=@At("HEAD"), cancellable = true)
    public void customProcessBlockEvents(CallbackInfo ci){
        if(TickProgress.runStatus() == RUN_COMPLETELY){
            return;
        }
        ci.cancel();

    }
}
