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

import static subtick.TickProgress.RUN_COMPLETELY;
import static subtick.TickProgress.STEP_FROM_START;

/*
TickSpeed.process_entities will be used to control the critial parts of the game loop

by default it will be
 */
@Mixin(value = ServerWorld.class, priority = 999)
public abstract class PreTileTicksWorldMixin extends World implements StructureWorldAccess {
    protected PreTileTicksWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
    }

    private boolean actuallyProcessEntities;

    @Inject(method = "tick", at=@At(value = "INVOKE",
    target = "Lnet/minecraft/world/border/WorldBorder;tick()V"))
    public void preWorldBorder(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickProgress.update(TickProgress.TILE_TICKS, this.getRegistryKey());
        int runStatus = TickProgress.runStatus();

        actuallyProcessEntities = TickSpeed.process_entities;
        TickSpeed.process_entities = runStatus == RUN_COMPLETELY || runStatus == STEP_FROM_START;
    }

    @Inject(method = "tick", at=@At(value = "INVOKE",
            target = "Lnet/minecraft/world/border/WorldBorder;tick()V",
            shift = At.Shift.AFTER))
    public void postWorldBorder(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickSpeed.process_entities = actuallyProcessEntities;
    }

    @Inject(method = "tick", at=@At(value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;tickWeather()V"))
    public void preWeather(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        int runStatus = TickProgress.runStatus();

        actuallyProcessEntities = TickSpeed.process_entities;
        TickSpeed.process_entities = runStatus == RUN_COMPLETELY || runStatus == STEP_FROM_START;
    }

    @Inject(method = "tick", at=@At(value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;tickWeather()V",
            shift = At.Shift.AFTER))
    public void postWeather(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickSpeed.process_entities = actuallyProcessEntities;
    }

    @Inject(method = "tick", at=@At(value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;tickTime()V"))
    public void preTickTime(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        int runStatus = TickProgress.runStatus();
        actuallyProcessEntities = TickSpeed.process_entities;
        TickSpeed.process_entities = runStatus == RUN_COMPLETELY || runStatus == STEP_FROM_START;
    }

    @Inject(method = "tick", at=@At(value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;tickTime()V",
            shift = At.Shift.AFTER))
    public void postTickTime(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickSpeed.process_entities = actuallyProcessEntities;
    }
}
